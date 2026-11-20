# x-diagnostic-result 로 배우는 Kotlin · React — 패턴 · 문법 · 호출관계 해설서

> 대상 레포: `x-diagnostic-result` (검사결과 Bounded Context 한 벌 — backend Kotlin/Spring + frontend React/TypeScript)
> 이 문서는 실제 소스 코드를 근거로 **언어 문법 → 구조 → 호출관계** 순서로 설명한다.
> 코드 인용은 모두 이 레포의 실제 코드이며, 파일 경로는 레포 루트 기준이다.
> 백엔드 경로 접두 `BE:` = `backend/diagnostic-result/src/main/kotlin/com/projectx/diagnosticresult/`
> 프론트 경로 접두 `FE:` = `frontend/apps/diagnostic-result/src/`

---

## 목차

- 0장. 이 문서 사용법
- 1장. 전체 그림 — 레포 구조와 요청 한 건의 여정
- 2부. 백엔드 (Kotlin · Spring Boot)
  - 2-1. 빌드 — Gradle Kotlin DSL
  - 2-2. 레이어 구조와 의존 방향
  - 2-3. Kotlin 문법 — 이 코드에 실제로 나오는 것들
  - 2-4. DDD 빌딩 블록 — seedwork 와 Aggregate 3개
  - 2-5. JPA 매핑 패턴
  - 2-6. 쓰기 경로(Command) 호출관계
  - 2-7. 읽기 경로(Query) 호출관계 — QueryDSL
  - 2-8. 이벤트 수신 경로(Kafka) 호출관계
  - 2-9. 외부 호출 — Port & Adapter (AI Gateway)
  - 2-10. 예외 → HTTP 상태 매핑
  - 2-11. 설정 · 부팅 · DB 마이그레이션
  - 2-12. 테스트
  - 2-13. 백엔드 디자인 패턴 요약표
- 3부. 프론트엔드 (React · TypeScript)
  - 3-1. 빌드 · 런타임 — Module Federation remote
  - 3-2. 폴더 구조 — FSD 레이어
  - 3-3. TypeScript 문법 — 이 코드에 실제로 나오는 것들
  - 3-4. React 기본 — 컴포넌트 · JSX · props
  - 3-5. React 훅 — 이 코드의 사용 패턴
  - 3-6. 전역 상태 — zustand 스토어
  - 3-7. 데이터 계층 — API 호출 · 표기 변환 · mock
  - 3-8. 이벤트 버스 — remote 간 통신
  - 3-9. 라우팅 — routes.tsx 와 Standalone
  - 3-10. 디자인 시스템 컴포넌트 사용법
  - 3-11. 화면 컴포넌트 트리 (호출관계)
  - 3-12. 프론트엔드 디자인 패턴 요약표
- 4부. 백엔드 ↔ 프론트엔드 연결
  - 4-1. API 계약표
  - 4-2. 끝에서 끝까지 — 시나리오 3개 전체 호출 체인
  - 4-3. 양쪽이 같은 값을 갖는 자리
- 5부. 학습 경로 · 연습 과제 · NotebookLM 질문 예시
- 부록. 용어집

---

# 0장. 이 문서 사용법

## 0-1. 읽는 순서 제안

1. **1장**으로 전체 그림을 먼저 잡는다(무엇이 무엇을 부르는지).
2. Kotlin 이 처음이면 **2-3(문법)** 을 먼저, Spring 이 처음이면 **2-6 · 2-7(호출관계)** 을 먼저 읽는다.
3. React 가 처음이면 **3-3 → 3-4 → 3-5** 순서로 읽고, 그다음 **3-11(컴포넌트 트리)** 로 실제 화면을 따라간다.
4. **4-2(끝에서 끝까지)** 는 버튼 클릭 하나가 브라우저에서 DB 까지 어떻게 내려가고 돌아오는지를 보여준다. 이 절을 이해하면 레포 전체를 이해한 것이다.

## 0-2. 이 코드베이스만의 특이점 (먼저 알아두기)

- **백엔드는 전 영역 snake_case** 다 — 클래스 이름도 `viewer_configuration`, 함수도 `add_favorite_set`. 일반적인 Kotlin 관례(클래스 PascalCase, 함수 camelCase)와 다르다. 문법은 같고 이름 규칙만 다르다고 보면 된다.
  - 예외: Kafka 로 받는 integration event payload 필드는 **camelCase**(`patientId`) — 다른 BC 와의 JSON 계약이기 때문.
- **프론트엔드는 일반적인 TypeScript 관례**(camelCase 변수, PascalCase 컴포넌트)를 쓴다. 백엔드 JSON(snake_case) → 화면 타입(camelCase) 변환은 API 파일에서 **손으로 명시 매핑**한다.
- 백엔드는 **DDD + CQRS** 구조다 — 쓰기(Command)는 JPA Aggregate, 읽기(Query)는 QueryDSL projection 으로 길이 완전히 갈린다.
- 프론트엔드는 **Micro Frontend 의 remote** 하나다 — 단독 앱이 아니라 host(`x-ehr-foundation`) 안에 끼워져 동작한다.

---

# 1장. 전체 그림 — 레포 구조와 요청 한 건의 여정

## 1-1. 레포 구조

```
x-diagnostic-result/                  ← Bounded Context(검사결과) 한 벌
├─ _docs/                             ← 설계문서
├─ backend/                           ← Kotlin / Spring Boot 4 / JDK 25
│  ├─ settings.gradle.kts             ← include("diagnostic-result") — 모듈 1개
│  ├─ build.gradle.kts                ← 플러그인 버전 선언 + 공통 설정
│  └─ diagnostic-result/              ← MicroService 1개 = 배포단위 1개 (포트 5351)
│     └─ src/main/kotlin/com/projectx/diagnosticresult/
│        ├─ presentation/             ← REST Controller (HTTP 입구)
│        ├─ application/              ← Command · Handler · Validator · Query 계약 · 통합이벤트 처리 · Port
│        ├─ domain/                   ← Aggregate · Entity · VO · Repository 인터페이스 (Spring 의존 금지)
│        └─ infrastructure/           ← Repository 구현 · QueryDSL 조회 · EventBus 배선 · 외부 API 어댑터 · config
└─ frontend/                          ← React 19 / TypeScript / rspack
   └─ apps/diagnostic-result/         ← MF remote (remoteKey = diagnostic_result, 포트 3351)
      └─ src/
         ├─ routes.tsx                ← host 에 노출하는 유일한 진입점
         ├─ app/Standalone.tsx        ← 단독 개발 실행용
         ├─ views/                    ← 화면(페이지)
         ├─ widgets/                  ← 화면 블록(여러 feature/entity 조합)
         ├─ features/                 ← 사용자 행위 단위 UI (즐겨찾기 세트 선택 등)
         ├─ entities/                 ← 도메인 타입 · API 호출 · 순수 판정 함수
         └─ shared/                   ← 공용 유틸 · API 기반 · 이벤트 계약 · 스토어
```

## 1-2. 이 BC 가 하는 일 (도메인)

- **수신**: 다른 BC(검체진단 · 영상진단 · 기능진단 · 간호기록 · 외부연동)가 Kafka 로 발행한 "검사결과 저장" 이벤트를 받아 `observation` 테이블 트리에 쌓는다.
- **조회**: 환자별 통합 검사결과 목록 · 환자목록 · 단건 상세 API 를 제공한다.
- **사용자 설정**: 로그인 사용자별 화면 설정(조회 양식 · 기본 기간)과 즐겨찾기 세트를 저장한다.
- **AI 분석**: 환자 1명을 AI Gateway 에이전트로 분석하고 결과를 이력으로 저장한다.

## 1-3. 세 가지 흐름 한눈에

```
[흐름 A: 쓰기]  브라우저 클릭
   → FE api 함수(put/post) → HTTP
   → Controller → command_bus.send(command)
   → Validator.validate → Handler.handle (@Transactional)
   → Aggregate 도메인 메서드 (규칙 검사 · 상태 변경)
   → Repository 인터페이스 → JPA 구현 → DB

[흐름 B: 읽기]  화면 진입
   → FE fetch 함수(get) → HTTP
   → Controller → i_*_queries 인터페이스
   → *_queries 구현 (@Transactional(readOnly)) → *_query_dsl (QueryDSL)
   → DB → read_row → view_model(data class) → JSON
   → FE toXxx 매핑(snake→camel) → state → 렌더

[흐름 C: 수신]  다른 BC 가 Kafka 에 발행
   → common-eventbus 라이브러리 (topic_registry 로 토픽↔클래스 라우팅)
   → event_bus_subscription_initializer 가 등록한 람다
   → integration_event_handler.handle (@Transactional) → observation_result_handler.handle_internal
   → payload 매퍼 → 팩토리(new_observation) → observation.apply_source_event (멱등)
   → Repository → DB
```

---

# 2부. 백엔드 (Kotlin · Spring Boot)

## 2-1. 빌드 — Gradle Kotlin DSL

빌드 스크립트도 Kotlin 문법이다(`.gradle.kts`). 세 파일이 역할을 나눈다.

### settings.gradle.kts — 어떤 모듈이 있는가

```kotlin
plugins {
    // JDK 25 toolchain 자동 프로비저닝. 설치된 JDK 25 가 없으면 자동 다운로드.
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
rootProject.name = "diagnostic-result"
include("diagnostic-result")
```

- `include(...)` — 하위 모듈 등록. 이 레포는 모듈 1개(MS 1개 = 배포단위 1개).

### backend/build.gradle.kts — 플러그인 버전만 선언, 적용은 하위에서

```kotlin
plugins {
    kotlin("jvm") version "2.3.0" apply false
    kotlin("kapt") version "2.3.0" apply false
    kotlin("plugin.spring") version "2.3.0" apply false
    kotlin("plugin.jpa") version "2.3.0" apply false
    id("org.springframework.boot") version "4.0.4" apply false
    id("io.spring.dependency-management") version "1.1.7" apply false
}

allprojects {
    group = "com.projectx"
    version = "1.0.0"
    repositories { mavenCentral(); maven { name = "nexus"; url = uri("https://…/maven-releases") } }
}

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")
    configure<JavaPluginExtension> { toolchain { languageVersion = JavaLanguageVersion.of(25) } }
    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        compilerOptions {
            freeCompilerArgs.addAll("-Xjsr305=strict")   // Java @Nullable 애노테이션을 Kotlin null 타입에 엄격 반영
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_25)
        }
    }
    tasks.withType<Test> { useJUnitPlatform() }
}
```

문법 포인트:
- `apply false` — 버전만 고정하고 루트에는 적용하지 않는다. 하위 모듈이 버전 없이 `kotlin("jvm")` 만 쓰면 이 버전을 따른다.
- `configure<T> { }` · `tasks.withType<T> { }` — **제네릭 + 람다 리시버(DSL)**. 중괄호 안에서 `this` 가 그 타입이 된다(그래서 `toolchain { … }` 을 바로 부를 수 있다).
- `val liquibase_guard by tasks.registering(Exec::class) { … }` — **프로퍼티 위임(`by`)**. 변수 이름이 곧 태스크 이름이 된다. 이 태스크는 Python 스크립트로 changelog 의 파괴적 SQL(DROP 등)을 빌드에서 막는다.

### diagnostic-result/build.gradle.kts — 실제 모듈

```kotlin
plugins {
    kotlin("jvm")
    kotlin("plugin.spring")   // Spring 애노테이션 클래스를 open 으로 (Kotlin 클래스는 기본 final)
    kotlin("plugin.jpa")      // JPA 엔티티용 컴파일러 플러그인
    kotlin("kapt")            // 애노테이션 프로세서 — QueryDSL Q클래스 생성
    id("org.springframework.boot")
    id("io.spring.dependency-management")
}
val querydslVersion: String by project      // gradle.properties 값을 위임으로 읽기

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")      // 쓰기: JPA
    implementation("io.github.openfeign.querydsl:querydsl-jpa:$querydslVersion") // 읽기: QueryDSL
    kapt("io.github.openfeign.querydsl:querydsl-apt:$querydslVersion:jakarta")   // Q클래스 생성기
    implementation("org.springframework.boot:spring-boot-liquibase")             // DB 마이그레이션
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("com.projectx.common:common-eventbus-kt:0.1.10")              // 사내 Kafka 공통 라이브러리
    implementation("tools.jackson.module:jackson-module-kotlin")                 // Jackson 3 (Boot 4)
    implementation("io.github.oshai:kotlin-logging-jvm:7.0.13")                  // KotlinLogging
    testImplementation("com.tngtech.archunit:archunit-junit5:1.4.1")             // 레이어 격리 테스트
    // …
}
```

왜 Kotlin 에 컴파일러 플러그인이 필요한가:
- **Kotlin 클래스는 기본이 `final`** (상속 불가)이다. Spring 은 `@Transactional` 같은 기능을 **클래스를 상속한 프록시(CGLIB)** 로 구현하므로 클래스가 열려 있어야 한다. `plugin.spring` 은 `@Component`·`@Service`·`@Transactional` 등이 붙은 클래스를 자동으로 `open` 으로 컴파일한다.
- `plugin.jpa` 는 JPA 엔티티용 플러그인이다. 프로젝트 주석은 이 플러그인을 빠뜨리면 Hibernate 프록시 생성이 실패(HHH000305)한다고 경고한다. 이 코드는 별도로 각 엔티티에 `protected constructor()` 를 직접 둔다(JPA 가 리플렉션으로 객체를 만들 때 쓰는 무인자 생성자).
- `kapt` 는 컴파일 시 `@Entity` 클래스를 읽어 `Qobservation` 같은 **QueryDSL 메타 클래스**를 생성한다. 2-7 에서 쓰는 `Qobservation.observation` 이 이것이다.

## 2-2. 레이어 구조와 의존 방향

```
presentation ──→ application ──→ domain
      │                ▲            ▲
      │                │ (구현)     │ (구현)
      └──✗──→   infrastructure ─────┘
```

- `domain` 은 아무것도 모른다(Spring 도 모른다). 순수 Kotlin + JPA 애노테이션만.
- `application` 은 `domain` 만 안다. 외부 기술이 필요한 곳은 **인터페이스(계약)** 만 둔다 — `i_observation_queries`, `i_analysis_agent_port`.
- `infrastructure` 가 그 인터페이스들을 **구현**한다(의존성 역전, DIP).
- `presentation` 은 `application` 의 `command_bus` 와 `i_*_queries` 만 본다.

단일 gradle 모듈이라 컴파일러가 이 방향을 강제하지 못한다. 그래서 **ArchUnit 테스트**가 강제한다:

```kotlin
// backend/diagnostic-result/src/test/kotlin/…/architecture/layer_isolation_test.kt
@AnalyzeClasses(packages = ["com.projectx.diagnosticresult"])
class layer_isolation_test {

    @ArchTest
    val domain_must_not_depend_on_spring: ArchRule =
        noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAnyPackage("org.springframework..")

    @ArchTest
    val application_must_not_depend_on_infrastructure: ArchRule =
        noClasses()
            .that().resideInAPackage("..application..")
            .should().dependOnClassesThat().resideInAPackage("..infrastructure..")
    // presentation → infrastructure 금지, domain → 바깥 레이어 금지 도 같은 모양
}
```

- `["com.projectx.diagnosticresult"]` — Kotlin 애노테이션 인자에서 배열은 `[ ]` 로 쓴다.
- `..domain..` — ArchUnit 패키지 패턴(앞뒤 임의 패키지).

## 2-3. Kotlin 문법 — 이 코드에 실제로 나오는 것들

### (1) 클래스 · 주 생성자 · 의존성 주입

```kotlin
// BE:application/commands/create_favorite_set_command_handler.kt
@Service
class create_favorite_set_command_handler(
    private val repository: i_viewer_configuration_repository
) : command_handler<create_favorite_set_command, String> {
    @Transactional
    override fun handle(command: create_favorite_set_command): String { … }
}
```

- 클래스 이름 뒤 괄호 = **주 생성자**. `private val repository: …` 라고 쓰면 생성자 인자이면서 동시에 **필드**가 된다(Java 의 필드 선언 + 생성자 대입 + getter 를 한 줄로).
- Spring 은 생성자가 하나뿐이면 그 생성자로 의존성을 주입한다 → `@Autowired` 가 필요 없다. 이 코드베이스의 모든 빈이 이 방식(**생성자 주입**)이다.
- `: command_handler<…>` — 인터페이스 구현(상속도 같은 `:` 문법). `override` 는 필수 키워드.

### (2) 보조 생성자 · protected 생성자 · 생성 규칙 강제

```kotlin
// BE:domain/aggregatesmodel/observation/specimen_diagnostics_observation.kt
class specimen_diagnostics_observation protected constructor() : observation() {
    // … 필드들 …
    constructor(id: String) : this() {
        initialize_identity(id, OBSERVATION_TYPE_CODE)
    }
}
```

- `protected constructor()` — 주 생성자를 **외부에서 못 부르게** 막는다. JPA(리플렉션)만 쓴다.
- `constructor(id: String) : this()` — **보조 생성자**. 반드시 주 생성자(`this()`)를 먼저 부른 뒤 본문을 실행한다. 업무 코드는 이 생성자로만 객체를 만든다 → "id 없는 엔티티"가 생길 수 없다.
- `favorite_set` 은 보조 생성자를 `internal constructor(set_name: String, sort_order: Int)` 로 둔다 — **같은 모듈 안에서만** 생성 가능. 의도는 "세트는 Root(`viewer_configuration`)를 통해서만 만든다".

### (3) 프로퍼티 · 접근자 · 캡슐화

```kotlin
// BE:domain/aggregatesmodel/observation/observation.kt
@Column(name = "patient_id", nullable = false)
var patient_id: String = ""
    protected set
```

- Kotlin 의 `var` 는 필드 + getter + setter 다. `protected set` 은 **setter 만** 하위 클래스로 제한한다. 읽기는 누구나, 쓰기는 도메인 메서드만. 이 코드에 137번 나온다 — Aggregate 상태를 밖에서 직접 바꾸지 못하게 하는 핵심 장치다.

```kotlin
// BE:domain/aggregatesmodel/viewerconfiguration/viewer_configuration.kt
private val _favorite_sets: MutableList<favorite_set> = mutableListOf()

val favorite_sets: List<favorite_set>
    get() = _favorite_sets.toList()
```

- **backing property 패턴**: 내부는 변경 가능한 `MutableList`, 외부에는 읽기 전용 `List` 를, 그것도 `toList()` **복사본**으로 준다. 바깥에서 리스트를 수정해도 Aggregate 에 영향이 없다.
- `get() = …` — **커스텀 getter**. 읽을 때마다 계산된다.
- `List` vs `MutableList` — Kotlin 은 컬렉션 인터페이스가 읽기용/변경용으로 나뉜다.

### (4) Null 안전성 — `?` `?.` `?:` `let` `takeIf`

Kotlin 은 타입에 null 가능 여부가 들어간다. `String` 에는 null 을 넣을 수 없고, `String?` 만 null 을 허용한다.

```kotlin
// BE:domain/aggregatesmodel/observation/observation.kt — apply_source_event 안
if (payload.patient_name.isNotBlank()) this.patient_name = payload.patient_name
payload.encounter_class?.let { this.encounter_class = it }
payload.department_code?.let { this.department_code = it }
```

- `x?.let { … }` — x 가 null 이 아니면 블록 실행, 블록 안에서 `it` = x. 여기서는 "**미동봉(null)이면 기존 값을 덮지 않는다**" 는 업무 규칙을 한 줄로 표현한다(22번 사용).

```kotlin
// BE:application/commands/create_favorite_set_command_handler.kt
val config = repository.find_viewer_configuration(command.user_id)
    ?: viewer_configuration(user_id = command.user_id)
```

- `a ?: b` — **엘비스 연산자**. a 가 null 이면 b. "있으면 찾고 없으면 새로 만든다(upsert)" 를 표현하는 이 코드의 단골 문법(59번).

```kotlin
// BE:domain/aggregatesmodel/viewerconfiguration/viewer_configuration.kt
private fun find_favorite_set(favorite_set_id: String): favorite_set =
    _favorite_sets.firstOrNull { it.id == favorite_set_id }
        ?: throw diagnostic_result_not_found_exception("즐겨찾기 세트를 찾을 수 없습니다")
```

- `throw` 도 **식(expression)** 이라 `?:` 오른쪽에 올 수 있다. "없으면 예외" 를 한 줄로.

```kotlin
// BE:infrastructure/queries/observation_queries.kt
observation_type_code?.takeIf { it.isNotBlank() }
```

- `takeIf { 조건 }` — 조건이 참이면 자신, 거짓이면 null. "빈 문자열을 null 로" 정규화에 쓴다.

```kotlin
// BE:infrastructure/queries/observation_queries.kt — 여러 후보 중 첫 non-null
val result_flag = row.spec_result_flag ?: row.func_result_flag ?: row.record_result_flag ?: row.ext_result_flag
```

- 엘비스를 이어 붙이면 SQL 의 `COALESCE` 와 같다.

이 코드에는 `!!`(강제 non-null 단언)가 없다. 대신 `requireNotNull` 로 이유를 남긴다:

```kotlin
// BE:application/integrationevents/eventhandling/observation_result_handler.kt
val source_aggregate_id = requireNotNull(event.sourceAggregateId) { "수신 payload 필수 필드 누락: sourceAggregateId" }
```

- `requireNotNull(x) { 메시지 }` — null 이면 `IllegalArgumentException`, 아니면 non-null 타입으로 **스마트 캐스트**된 값을 돌려준다.

### (5) 사전조건 — `require` · `error`

```kotlin
// BE:domain/aggregatesmodel/observation/observation.kt
fun apply_source_event(source_aggregate_id: String, source_event_version: Long, payload: observation_payload): Boolean {
    require(source_aggregate_id.isNotBlank()) { "source_aggregate_id 는 필수입니다" }
    require(source_event_version > 0) { "source_event_version 은 1 이상이어야 합니다" }
    …
}
```

- `require(조건) { 메시지 }` — 거짓이면 `IllegalArgumentException`. 메시지 람다는 실패할 때만 실행된다(지연 평가).
- `error("…")` — `IllegalStateException` 을 던지는 표준 함수. 반환 타입이 `Nothing` 이라 `when` 의 `else` 가지에 쓸 수 있다(아래 when 예시).

### (6) `when` 식 — 분기

```kotlin
// BE:domain/aggregatesmodel/observation/observation_factory.kt
fun new_observation(observation_type_code: String, id: String): observation =
    when (observation_type_code) {
        specimen_diagnostics_observation.OBSERVATION_TYPE_CODE -> specimen_diagnostics_observation(id)
        diagnostic_imaging_observation.OBSERVATION_TYPE_CODE -> diagnostic_imaging_observation(id)
        functional_diagnostics_observation.OBSERVATION_TYPE_CODE -> functional_diagnostics_observation(id)
        clinical_record_observation.OBSERVATION_TYPE_CODE -> clinical_record_observation(id)
        external_record_observation.OBSERVATION_TYPE_CODE -> external_record_observation(id)
        else -> error("알 수 없는 검사분류입니다: $observation_type_code")
    }
```

- `when` 은 Java `switch` 의 확장판이자 **값을 돌려주는 식**이다. 함수 본문이 식 하나면 `= when …` 처럼 **식 본문 함수**로 쓴다.
- 이 함수는 클래스 밖에 있는 **최상위(top-level) 함수**다. Kotlin 은 함수가 꼭 클래스 안에 있을 필요가 없다.
- 디자인 패턴으로는 **Factory** — 분류별 분기를 이 한 곳에 모은다.

조건만 있는 `when { }` 도 있다:

```kotlin
// BE:infrastructure/proxies/ai_gateway_analysis_agent_provider.kt
private val agent: analysis_agent? = when {
    props.api_key.isBlank() -> { logger.warn { "…" }; null }
    props.agent_id.isBlank() -> { logger.warn { "…" }; null }
    else -> ai_gateway_agent_analysis_agent(…).also { logger.info { "AI 분석 에이전트 준비 완료: agent=${it.name}" } }
}
```

여러 값을 한 가지로 묶을 수도 있다:

```kotlin
// BE:application/integrationevents/eventhandling/observation_result_handler.kt
return when (action) {
    observation_result_integration_event.DELETED -> true
    null, "",
    observation_result_integration_event.CREATED,
    observation_result_integration_event.MODIFIED,
    -> false
    else -> { logger.warn { "모르는 행위 — 적재로 처리한다: …" }; false }
}
```

### (7) 문자열 템플릿

```kotlin
"세트 이름은 ${MAX_SET_NAME_LENGTH}자 이하여야 합니다"
"알 수 없는 검사분류입니다: $observation_type_code"
```

- `$변수` 또는 `${식}`. 뒤에 글자가 붙으면 `${}` 로 감싼다.

### (8) `data class` — 값 묶음

```kotlin
// BE:application/commands/create_favorite_set_command.kt
data class create_favorite_set_command(
    val user_id: String,
    val set_name: String,
    val observation: favorite_observation_input? = null
) : command<String>
```

- `data class` 는 `equals` · `hashCode` · `toString` · `copy` · 구조분해(`component1()`)를 자동 생성한다. Command, view_model, 요청 DTO, read row 가 전부 data class 다(33개).
- `= null` — **기본값 인자**. 호출할 때 생략 가능.
- 호출은 **이름 있는 인자**로 하는 경우가 많다: `viewer_configuration(user_id = command.user_id)`, `add_favorite_set(set_name = …, first_item = …)`. 인자가 많은 `observation_payload(…)` 같은 곳에서 가독성과 실수 방지에 중요하다.

### (9) `companion object` · `const val` — 클래스 수준 상수

```kotlin
// BE:domain/aggregatesmodel/viewerconfiguration/viewer_configuration.kt
companion object {
    const val MAX_FAVORITE_SETS = 20
    val ALLOWED_LAYOUT_CODES: Set<String> = setOf("BY_CATEGORY", "BY_RESULT_DATETIME", "BY_ORDER_DATETIME")
}
```

- Kotlin 에는 `static` 이 없다. 대신 클래스마다 하나씩 둘 수 있는 `companion object` 안에 둔다. 사용은 `viewer_configuration.MAX_FAVORITE_SETS`.
- `const val` — 컴파일 타임 상수(원시 타입 · String 만). 애노테이션 인자에도 쓸 수 있다. `setOf(...)` 같은 객체는 `val`.
- `private companion object` 도 보인다(`observation_queries`의 `MAX_PAGE_SIZE`) — 클래스 안에서만 쓰는 상수.

### (10) 확장 함수 — 남의 타입에 함수 붙이기

```kotlin
// BE:application/integrationevents/eventhandling/observation_payload_mapper.kt
internal fun observation_result_integration_event.to_observation_payload(): observation_payload {
    val type_code = observationTypeCode.required("observationTypeCode")
    …
}

private fun String?.blank_to_null(): String? = this?.takeIf { it.isNotBlank() }

private fun <T : Any> T?.required(field_name: String): T =
    requireNotNull(this) { "수신 payload 필수 필드 누락: $field_name" }
        .also { if (it is String) require(it.isNotBlank()) { "수신 payload 필수 필드 공백: $field_name" } }
```

- `수신타입.함수이름()` 형태로 선언하면 그 타입의 메서드처럼 호출된다: `event.to_observation_payload()`, `departmentCode.blank_to_null()`.
- **nullable 타입에도 확장**할 수 있다(`String?.blank_to_null`). 안에서 `this` 가 null 일 수 있다.
- `<T : Any> T?.required(...)` — **제네릭 확장 함수**. `T : Any` 는 "T 는 non-null 타입" 이라는 상한(upper bound). 결과는 non-null `T`.
- `.also { }` — 값을 그대로 돌려주면서 부수 작업(여기서는 추가 검증)을 끼워 넣는다.
- `it is String` — 타입 검사(Java `instanceof`). 참이면 블록 안에서 `it` 이 String 으로 스마트 캐스트된다.

다른 확장 함수 예:

```kotlin
// BE:infrastructure/eventbus/topic_registry.kt — 수신 객체가 MutableMap 인 확장 함수
private fun MutableMap<KClass<*>, EventRoute>.put_result_route(topic: String, partition_key_field: String, event_type: KClass<*>) {
    put(event_type, EventRoute(…))
}

// BE:infrastructure/proxies/ai_gateway_agent_analysis_agent.kt — 라이브러리 타입(Jackson JsonNode)에 붙이기
private fun JsonNode.firstOrNull(): JsonNode? = if (isArray && size() > 0) get(0) else null
```

### (11) 스코프 함수 — `let` `also` `apply` `use` `runCatching`

| 함수 | 블록 안 객체 | 반환 | 이 코드의 용도 |
|---|---|---|---|
| `x?.let { it … }` | `it` | 블록 결과 | null 이 아닐 때만 실행 |
| `x.also { it … }` | `it` | x 자신 | 로그 · 추가 검증 끼워 넣기 |
| `x.apply { … }` | `this` | x 자신 | 객체 설정 (`SimpleClientHttpRequestFactory().apply { setConnectTimeout(…) }`) |
| `x.use { … }` | `it` | 블록 결과 | 자원 자동 close (Java try-with-resources) |
| `runCatching { }` | — | `Result<T>` | 예외를 값으로 바꿔 처리 |

```kotlin
// BE:infrastructure/config/db_schema_initializer.kt — use: Connection · Statement 자동 close
data_source_provider.getObject().connection.use { connection ->
    connection.createStatement().use { statement ->
        statement.execute("CREATE SCHEMA IF NOT EXISTS $schema_name")
    }
}

// BE:infrastructure/eventbus/event_bus_subscription_initializer.kt — runCatching: 실패해도 부팅을 막지 않기
runCatching { event_bus.subscribeEnvelope(event_type, handler) }
    .onFailure { logger.error(it) { "이벤트 구독 등록 실패 — 이 이벤트는 수신되지 않는다: ${event_type.simpleName}" } }

// BE:infrastructure/proxies/analysis_response_normalizer.kt — 파싱 실패를 null 로
val node = runCatching { object_mapper.readTree(detail) }.getOrNull() ?: return derived_fields()
```

- 마지막 예: `?: return …` — **조기 반환**도 식이라 엘비스 오른쪽에 둘 수 있다.

### (12) 람다 · 함수 타입 · 후행 람다

```kotlin
// BE:infrastructure/eventbus/event_bus_subscription_initializer.kt
private fun <T : Any> subscribe(event_type: KClass<T>, handler: (MessageEnvelope<T>) -> Unit) { … }

listOf(
    diagnostic_imaging_result_integration_event::class,
    functional_diagnostics_result_integration_event::class,
    // …
).forEach { subscribe(it) { envelope -> result_handler.handle(envelope) } }
```

- `(MessageEnvelope<T>) -> Unit` — **함수 타입**. "MessageEnvelope 를 받아 아무것도 반환하지 않는 함수". `Unit` = Java `void`.
- **후행 람다**: 마지막 인자가 함수면 괄호 밖에 `{ }` 로 쓴다. `subscribe(it) { envelope -> … }` 는 `subscribe(it, { envelope -> … })` 와 같다.
- 인자가 하나인 람다는 이름을 생략하고 `it` 으로 부른다.

### (13) 컬렉션 연산 — 함수형 스타일

```kotlin
// BE:domain/aggregatesmodel/viewerconfiguration/viewer_configuration.kt
fun reorder_favorite_sets(favorite_set_ids: List<String>) {
    val current = _favorite_sets.map { it.id }.toSet()
    if (favorite_set_ids.size != current.size || favorite_set_ids.toSet() != current) {
        throw diagnostic_result_validation_exception("세트 순서에는 모든 세트를 한 번씩 담아야 합니다")
    }
    val order = favorite_set_ids.withIndex().associate { (i, id) -> id to i }
    _favorite_sets.sortBy { order.getValue(it.id) }
    renumber_favorite_sets()
}

private fun renumber_favorite_sets() {
    _favorite_sets.forEachIndexed { i, set -> set.move_to(i) }
}
```

- `map` · `toSet` · `withIndex` · `associate` · `sortBy` · `forEachIndexed` — 컬렉션 표준 함수.
- `(i, id) -> …` — 람다 인자 **구조분해**(`IndexedValue` 를 두 변수로).
- `id to i` — `to` 는 **중위 함수(infix)** 로 `Pair(id, i)` 를 만든다. `mapOf("role" to "user")` 도 같은 문법.
- `set.id in favorite_set_ids` — `in` 연산자 = `contains`. `layout_code !in ALLOWED_LAYOUT_CODES` 처럼 부정도 된다.

```kotlin
// BE:infrastructure/queries/viewer_configuration_query_dsl.kt — groupBy (키, 값 변환)
.groupBy({ it.favorite_set_id }, { favorite_observation_view(it.observation_type_code, it.observation_code) })
// …
items = items_by_set[it.id].orEmpty()   // null 이면 빈 리스트
```

```kotlin
// BE:infrastructure/eventbus/topic_registry.kt — buildMap: 빌더 람다 안에서 put 한 뒤 읽기전용 Map 으로
MapEventRouter(
    buildMap<KClass<*>, EventRoute> {
        put_result_route(…)
        put_result_route(…)
    },
)
```

### (14) 제네릭 · 공변성 · `KClass`

```kotlin
// BE:application/commands/command.kt
interface command<R>                                  // R = 이 커맨드의 반환 타입
interface command_handler<C : command<R>, R> {        // C 는 command<R> 의 하위 타입이어야 한다
    fun handle(command: C): R
}
```

- `create_favorite_set_command : command<String>` 이면 핸들러 반환이 `String`(새 세트 id), `assign_favorite_sets_command : command<Unit>` 이면 반환 없음. **타입으로 커맨드와 반환을 묶는다.**

```kotlin
// BE:application/integrationevents/eventhandling/integration_event_handler.kt
abstract class integration_event_handler<T : integration_event> {
    open fun handle(envelope: MessageEnvelope<out T>) { … }
}
```

- `out T` — **사용처 공변성(use-site variance)**. `MessageEnvelope<laboratory_result_integration_event>` 를 `MessageEnvelope<out observation_result_integration_event>` 자리에 넘길 수 있게 한다(Java 의 `? extends T`). 한 핸들러가 6개 토픽 클래스를 다 받는 근거다.

```kotlin
// BE:application/validations/command_validator.kt
interface command_validator<C : command<*>> {
    fun command_type(): KClass<C>
    fun validate(command: C)
}
```

- `command<*>` — **스타 프로젝션**. "반환 타입은 뭐든 상관없는 command".
- `KClass<C>` — Kotlin 의 클래스 객체(Java `Class` 대응). `create_favorite_set_command::class` 로 얻는다. Java Class 가 필요하면 `::class.java`.

### (15) 상속 · `abstract` · `open` · `override val`

```kotlin
// BE:domain/aggregatesmodel/observation/observation.kt
abstract class observation protected constructor() : entity(), i_aggregate_root {
    protected abstract fun apply_detail(payload: observation_payload)   // 자식이 반드시 구현
    open fun recalculate_flag() { }                                     // 자식이 원하면 재정의
    open fun current_result_flag(): String? = null
    open fun has_critical_value(): Boolean = false
    protected fun compute_abnormal(): Boolean = has_critical_value() || current_result_flag() in ABNORMAL_FLAGS
}
```

- `abstract` — 구현 없음, 자식이 반드시 구현. `open` — 기본 구현 있음, 재정의 허용. 둘 다 없으면 재정의 불가(final).
- `: entity(), i_aggregate_root` — 클래스 상속(괄호로 부모 생성자 호출) 1개 + 인터페이스 여러 개.
- 이것은 **템플릿 메서드 패턴**이다. `apply_source_event` 가 공통 순서(검증 → 공통 필드 → `apply_detail` → `recalculate_flag` → `compute_abnormal`)를 정하고, 자식이 빈 칸을 채운다.

프로퍼티도 재정의된다:

```kotlin
// BE:application/integrationevents/events/observation_result_integration_event.kt (부모)
@get:JsonIgnore abstract val allowed_observation_type_codes: Set<String>
@get:JsonIgnore open val default_section_code: String? = null
open fun resolved_event_version(): Long = sourceEventVersion

// BE:application/integrationevents/events/pathology_result_integration_event.kt (자식)
override val allowed_observation_type_codes = setOf(specimen_diagnostics_observation.OBSERVATION_TYPE_CODE)
@get:JsonIgnore override val default_section_code: String = PATHOLOGY_SECTION_CODE

// BE:application/integrationevents/events/observation_record_group_snapshot_integration_event.kt (자식)
override fun resolved_event_version(): Long = groupVersion ?: sourceEventVersion
```

- 부모가 `String?` 이었는데 자식이 `String` 으로 좁혀 재정의할 수 있다(non-null 은 nullable 의 하위 타입).
- `@get:JsonIgnore` — **애노테이션 사용 위치 지정(use-site target)**. Kotlin 프로퍼티는 필드 · getter · setter 로 컴파일되므로 "getter 에 붙여라" 를 명시한다. view_model 의 `@get:JsonProperty("is_abnormal")` 도 같은 문법 — `is_` 로 시작하는 Boolean 이름을 Jackson 이 다르게 해석해 JSON 키가 사라지는 문제를 막으려 getter 에 이름을 고정한다.

### (16) 가시성 — `private` `protected` `internal` `public`

- 기본은 `public`.
- `internal` — 같은 **모듈**(gradle 모듈) 안에서만. `favorite_set` 의 생성자와 `rename`/`add_item` 등, 매퍼 확장 함수, 입력 검증 함수(`validate_user_id`)에 쓴다.
- 최상위 `private` — **같은 파일** 안에서만. 파일 맨 위 `private val logger = KotlinLogging.logger {}` 가 그 예.

### (17) 함수 참조 · 클래스 참조

```kotlin
// BE:infrastructure/queries/observation_queries.kt
query_dsl.search_observations_by_category(…).map(::to_category_view)
query_dsl.find_observation(id)?.let(::to_category_view)
```

- `::함수이름` — 함수를 값으로 넘긴다. `map { to_category_view(it) }` 와 같다.
- `observation_category_read_row::class.java` — QueryDSL `Projections.constructor` 에 Java `Class` 가 필요해서.

### (18) 익명 객체 · `object :`

```kotlin
// BE:infrastructure/proxies/ai_gateway_analysis_agent_provider.kt
val dns_resolver = object : SystemDefaultDnsResolver() {
    override fun resolve(host: String): Array<InetAddress> =
        if (host.equals(resolve_host, ignoreCase = true)) arrayOf(InetAddress.getByName(resolve_ip))
        else super.resolve(host)
}
```

- Java 의 익명 클래스. 특정 호스트만 고정 IP 로 해석하도록 메서드 하나를 재정의한다.
- `if … else …` 도 **식**이라 값으로 쓴다(Kotlin 에는 삼항연산자가 없고 if 식을 쓴다).

### (19) 인라인 람다 안의 `return` — `repeat`

```kotlin
// BE:infrastructure/proxies/ai_gateway_agent_analysis_agent.kt
var last_error: Exception? = null
repeat(max_retries + 1) { attempt ->
    try {
        val response_body = rest_client.post() … .body(String::class.java)
            ?: throw IllegalStateException("AI Gateway 응답이 비어있습니다 (graph_id=$graph_id)")
        return parse_reply(response_body)        // ← 람다가 아니라 call_with_retry 함수 자체에서 반환
    } catch (e: Exception) {
        last_error = e
        logger.warn { "… 호출 실패(시도 ${attempt + 1}/${max_retries + 1}) …" }
    }
}
throw last_error ?: IllegalStateException("AI Gateway 호출 실패 (graph_id=$graph_id)")
```

- `repeat(n) { }` 는 `inline` 함수라 람다 안의 `return` 이 **바깥 함수에서 반환(non-local return)** 한다. 성공하면 즉시 빠져나가고, 전부 실패하면 마지막 예외를 던진다 — **재시도 패턴**.
- `try` 도 식이다: `val root: JsonNode = try { object_mapper.readTree(body) } catch (e: Exception) { return gateway_reply(body, null) }`.

### (20) 그 밖의 문법 조각

- **백틱 이름**: 테스트 함수 이름에 한글 · 공백을 쓴다 — ``fun `검사 배정은 고른 세트에만 넣고 나머지 세트에서는 뺀다`()``. 또 Kotlin 예약어와 겹치는 Java 메서드를 부를 때: QueryDSL 의 `s.id.`in`(sets.map { it.id })`.
- **스프레드 연산자**: `runApplication<diagnostic_result_application>(*args)` · `.allowedOrigins(*origins)` — 배열을 가변 인자로 펼친다.
- **reified 제네릭 호출**: `runApplication<T>(…)` 처럼 타입 인자만으로 클래스를 넘기는 Spring Kotlin 확장 함수.
- **`coerceIn` · `coerceAtLeast`**: `size.coerceIn(1, MAX_PAGE_SIZE)` — 범위 강제(페이지 크기 방어).
- **`ifBlank`**: `props.graph_id.ifBlank { "auto_${props.agent_id}" }` — 비어 있으면 대체값.
- **`Regex`**: `schema_name.matches(IDENTIFIER)` (`Regex("^[a-z_][a-z0-9_]{0,62}$")`) — SQL 주입 방지용 화이트리스트.
- **로깅 람다**: `logger.info { "…" }` — KotlinLogging 은 메시지를 람다로 받아, 로그 레벨이 꺼져 있으면 문자열을 만들지 않는다.
- **`@Suppress("UNCHECKED_CAST")`**: 제네릭 강제 캐스트 경고 억제(`command_bus`).

## 2-4. DDD 빌딩 블록 — seedwork 와 Aggregate 3개

### seedwork (모든 도메인 클래스의 바탕)

| 파일 | 역할 |
|---|---|
| `BE:domain/seedwork/entity.kt` | 모든 엔티티의 추상 부모. 문자열 `id`, `created_at`/`updated_at` 자동 기록, 도메인 이벤트 목록, id 기반 `equals`/`hashCode` |
| `BE:domain/seedwork/value_object.kt` | 값 객체 부모. 구성요소 동치로 `equals` |
| `BE:domain/seedwork/i_aggregate_root.kt` | Aggregate Root 표식 인터페이스(멤버 없음) |
| `BE:domain/seedwork/i_repository.kt` | `interface i_repository<T : i_aggregate_root>` — Root 만 Repository 를 가질 수 있음을 **타입으로** 강제 |
| `BE:domain/seedwork/domain_event.kt` | 도메인 이벤트 표식 |

`entity` 의 핵심:

```kotlin
@MappedSuperclass
abstract class entity {
    @Id open var id: String = ""
        protected set

    @Transient private val _domain_events: MutableList<domain_event> = mutableListOf()
    val domain_events: List<domain_event> get() = _domain_events.toList()
    fun add_domain_event(event: domain_event) { _domain_events.add(event) }
    fun clear_domain_events() { _domain_events.clear() }

    fun is_transient(): Boolean = id.isBlank()

    @PrePersist protected fun on_pre_persist() { val now = LocalDateTime.now(); created_at = now; updated_at = now }
    @PreUpdate  protected fun on_pre_update() { updated_at = LocalDateTime.now() }

    override fun equals(other: Any?): Boolean {
        if (other == null || other !is entity) return false
        if (this === other) return true                 // === 는 참조 동일성, == 는 equals
        if (this::class != other::class) return false
        if (is_transient() || other.is_transient()) return false
        return id == other.id
    }
}
```

- `@Transient` — DB 컬럼이 아님. 도메인 이벤트는 메모리에만 쌓였다가 저장 직후 발행된다(2-6 의 `domain_event_dispatcher`).
- `==` 는 `equals` 호출, `===` 는 같은 객체인지.

### Aggregate 3개

| Aggregate Root | 자식 | 핵심 도메인 메서드 | 불변식(규칙) |
|---|---|---|---|
| `observation` (추상, 자식 5종 JOINED 상속) | `specimen_diagnostics_observation` · `diagnostic_imaging_observation` · `functional_diagnostics_observation` · `clinical_record_observation` · `external_record_observation` | `apply_source_event` · `mark_deleted` · `recalculate_flag` | (원천 id, 원천 버전) 기준 **멱등** — 같거나 낮은 버전은 무시. 비정상 판정은 `compute_abnormal` 한 곳 |
| `viewer_configuration` | `favorite_set`(Entity) → `favorite_observation`(VO, `@Embeddable`) | `add_favorite_set` · `rename_favorite_set` · `remove_favorite_set` · `assign_observation_to_sets` · `reorder_favorite_sets` · `set_last_view_layout` · `set_default_period` | 세트 최대 20개, 이름 1~30자 · 중복 금지, 순서는 0부터 연속 |
| `observation_analyze_result` | 없음 | 생성자뿐 | 호출마다 새 행(이력 누적) |

**멱등 적재** (이 BC 의 가장 중요한 규칙):

```kotlin
// BE:domain/aggregatesmodel/observation/observation.kt
fun apply_source_event(source_aggregate_id: String, source_event_version: Long, payload: observation_payload): Boolean {
    require(…)
    // 이미 더 최신 버전이 적재되어 있으면 멱등 무시
    if (this.source_aggregate_id == source_aggregate_id && source_event_version <= this.source_event_version) {
        return false
    }
    this.source_aggregate_id = source_aggregate_id
    this.source_event_version = source_event_version
    … 공통 필드 대입 …
    apply_detail(payload)          // 자식별 필드 (템플릿 메서드의 빈 칸)
    recalculate_flag()             // 참조범위 비교로 H/L/HH/LL 재계산
    this.is_abnormal = compute_abnormal()
    return true                    // 반영했으면 true → 호출부가 저장
}
```

- Kafka 는 같은 메시지를 두 번 줄 수도, 순서를 뒤집어 줄 수도 있다. 이 메서드는 버전 비교만으로 그 둘에 안전하다.

**Root 를 통해서만 자식을 바꾼다**:

```kotlin
// BE:domain/aggregatesmodel/viewerconfiguration/viewer_configuration.kt
fun assign_observation_to_sets(observation_type_code: String, observation_code: String, favorite_set_ids: Set<String>) {
    val observation = favorite_observation(observation_type_code, observation_code)
    favorite_set_ids.forEach { find_favorite_set(it) }   // 모르는 id 가 있으면 아무것도 바꾸기 전에 404
    _favorite_sets.forEach { set ->
        if (set.id in favorite_set_ids) set.add_item(observation) else set.remove_item(observation)
    }
}
```

- `favorite_set.add_item` 은 `internal` 이라 바깥 모듈에서 부를 수 없고, 같은 모듈의 handler 도 관례상 Root 메서드만 부른다.
- "먼저 전부 검증 → 그다음 변경" 순서로 **부분 반영**을 막는다.

**값 객체(VO)** `favorite_observation` 은 `(observation_type_code, observation_code)` 동치로 `equals`/`hashCode` 를 재정의한다. 그래서 `MutableSet<favorite_observation>` 에 넣으면 같은 검사가 두 번 들어가지 않는다(세트 안 중복 방지가 자료구조로 해결된다).

**도메인 예외** — 상속으로 HTTP 상태를 구분한다(2-10):

```kotlin
// BE:domain/exceptions/diagnostic_result_domain_exception.kt
open class diagnostic_result_domain_exception(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
class diagnostic_result_not_found_exception(message: String) : diagnostic_result_domain_exception(message)   // 404
class diagnostic_result_validation_exception(message: String) : diagnostic_result_domain_exception(message)  // 400
```

## 2-5. JPA 매핑 패턴

| 애노테이션 | 어디 | 의미 |
|---|---|---|
| `@MappedSuperclass` | `entity` | 테이블은 없고 필드만 자식 테이블에 상속 |
| `@jakarta.persistence.Entity` + `@Table(name = "observation")` | 각 Aggregate | 테이블 매핑. 테이블명 = 클래스명 |
| `@Inheritance(strategy = InheritanceType.JOINED)` + `@DiscriminatorColumn(name = "observation_type_code")` | `observation` | 부모 테이블 + 자식 테이블을 id 로 조인. 구분 컬럼 값으로 자식 타입 결정 |
| `@DiscriminatorValue("SPECIMEN_DIAGNOSTICS")` + `@PrimaryKeyJoinColumn(name = "id")` | 자식 5종 | 이 자식의 구분값, 부모와 같은 PK 를 조인 키로 공유 |
| `@Column(…, insertable = false, updatable = false)` | `observation.observation_type_code` | 구분 컬럼은 JPA 가 쓰므로 필드로는 읽기만 |
| `@OneToMany(cascade = [CascadeType.ALL], orphanRemoval = true)` + `@JoinColumn` + `@OrderBy("sort_order ASC")` | `viewer_configuration._favorite_sets` | Root 저장 시 자식도 함께 저장 · 리스트에서 빠진 자식은 DELETE |
| `@ElementCollection` + `@CollectionTable(name = "favorite_set_item")` | `favorite_set._items` | VO 컬렉션을 별도 테이블에 |
| `@Embeddable` | `favorite_observation` | 독립 id 없는 값 타입 |
| `@Version` | `viewer_configuration.version` | **낙관적 잠금**. 동시 저장 충돌 → `OptimisticLockingFailureException` → 409 |
| `@PrePersist` / `@PreUpdate` | `entity` | 저장 직전 콜백 |

`@Version` 이 `Long?` 인 이유(코드 주석 요약): Spring Data 의 `save()` 는 version 이 null 이면 "새 엔티티"로 보고 `persist`, 값이 있으면 `merge` 한다. 0 으로 초기화하면 아직 없는 행을 merge 하려다 낙관적 잠금 실패가 난다.

`Long` vs `Long?` · `String = ""` 기본값: Kotlin non-null 프로퍼티는 초기값이 있어야 하므로 `""`·`0`·`LocalDateTime.MIN` 으로 채우고, 실제 값은 생성자나 도메인 메서드가 넣는다.

## 2-6. 쓰기 경로(Command) 호출관계

### 전체 체인 (즐겨찾기 세트 생성 예)

```
HTTP POST /api/v1/diagnostic-result/viewer-configurations/{user_id}/favorite-sets
 └ favorite_sets_controller.create_favorite_set(user_id, request)        [presentation]
    └ request.to_command(user_id) → create_favorite_set_command          (요청 DTO → 커맨드 변환)
    └ command_bus.send(command)                                          [application]
       ├ create_favorite_set_command_validator.validate(command)         (형식 검증 · 실패 시 400)
       └ create_favorite_set_command_handler.handle(command)  @Transactional
          ├ repository.find_viewer_configuration(user_id)                (i_viewer_configuration_repository — domain 계약)
          │   └ viewer_configuration_repository (infrastructure 구현)
          │       └ viewer_configuration_jpa_repository.find_first_by_user_id (@Query JPQL)
          ├ ?: viewer_configuration(user_id)                             (없으면 생성)
          ├ config.add_favorite_set(set_name, first_item)                [domain — 규칙 검사 · 상태 변경]
          └ repository.create_or_update_viewer_configuration(config)
              └ jpa.save(config) → domain_event_dispatcher.dispatch(saved)
    ← 반환값(새 세트 id) → ResponseEntity.created(URI) → 201 + Location
트랜잭션 커밋 시점에 INSERT/UPDATE SQL 이 실제로 나간다.
```

### Controller — 얇게, 변환과 위임만

```kotlin
// BE:presentation/favorite_sets_controller.kt
@RestController
@RequestMapping("/api/v1/diagnostic-result/viewer-configurations/{user_id}")
class favorite_sets_controller(private val command_bus: command_bus) {

    @PostMapping("/favorite-sets")
    fun create_favorite_set(@PathVariable user_id: String, @RequestBody request: create_favorite_set_request): ResponseEntity<Void> {
        val created_id = command_bus.send(request.to_command(user_id))
        return ResponseEntity
            .created(URI.create("/api/v1/diagnostic-result/viewer-configurations/$user_id/favorite-sets/$created_id"))
            .build()
    }

    @PatchMapping("/favorite-sets/{favorite_set_id}")
    fun rename_favorite_set(@PathVariable user_id: String, @PathVariable favorite_set_id: String,
                            @RequestBody request: rename_favorite_set_request): ResponseEntity<Void> {
        command_bus.send(rename_favorite_set_command(user_id, favorite_set_id, request.set_name))
        return ResponseEntity.noContent().build()      // 204
    }
}

// 요청 DTO 는 컨트롤러 파일에 함께 둔다 — 변환 메서드(to_command)를 갖는다
data class create_favorite_set_request(val set_name: String, val observation_type_code: String? = null, val observation_code: String? = null) {
    fun to_command(user_id: String) = create_favorite_set_command(
        user_id = user_id,
        set_name = set_name,
        observation = if (observation_type_code == null && observation_code == null) null
                      else favorite_observation_input(observation_type_code.orEmpty(), observation_code.orEmpty())
    )
}
```

- HTTP 동사와 응답: 생성 POST → 201 + Location, 수정 PATCH/PUT · 삭제 DELETE → 204. "한 엔드포인트 = 커맨드 하나".
- `ResponseEntity<Void>` — 본문 없는 응답 타입.
- `orEmpty()` — `String?` 을 null 이면 `""` 로.

### command_bus — 리플렉션으로 핸들러 찾기 (Mediator 패턴)

```kotlin
// BE:application/commands/command_bus.kt
@Component
class command_bus(
    private val application_context: ApplicationContext,
    private val validators: List<command_validator<*>>      // ← 이 타입의 빈 전부를 리스트로 주입받는다
) {
    private val handler_cache = ConcurrentHashMap<Class<*>, String>()

    @Suppress("UNCHECKED_CAST")
    fun <C : command<R>, R> send(command: C): R {
        val command_type = command::class.java

        // 1) 이 커맨드 타입에 맞는 validator 전부 실행
        validators
            .filter { it.command_type().java.isAssignableFrom(command_type) }
            .forEach { validator -> (validator as command_validator<C>).validate(command) }

        // 2) handle(파라미터 1개) 의 파라미터 타입이 이 커맨드인 핸들러 빈을 찾아 캐시
        val handler_bean_name = handler_cache.getOrPut(command_type) {
            application_context.getBeanNamesForType(command_handler::class.java)
                .firstOrNull { bean_name ->
                    val bean = application_context.getBean(bean_name) as command_handler<*, *>
                    val handle_method = bean::class.java.methods
                        .firstOrNull { it.name == "handle" && it.parameterCount == 1 && !it.isBridge }
                    handle_method?.parameterTypes?.firstOrNull()?.isAssignableFrom(command_type) == true
                }
                ?: throw IllegalStateException("핸들러를 찾을 수 없습니다: ${command_type.simpleName}")
        }

        // 3) 핸들러 실행
        val handler = application_context.getBean(handler_bean_name) as command_handler<C, R>
        return handler.handle(command)
    }
}
```

배울 점:
- **컬렉션 주입**: 생성자 인자를 `List<인터페이스>` 로 두면 Spring 이 그 인터페이스를 구현한 빈을 전부 모아 준다. validator 를 새로 만들면 `@Component` 만 붙이면 자동 등록된다.
- **`!it.isBridge`**: 제네릭 인터페이스를 구현하면 JVM 이 타입 소거 때문에 `handle(Object)` 브리지 메서드를 추가로 만든다. 이것을 걸러야 "모든 핸들러가 모든 커맨드에 매칭"되는 오류를 피한다.
- `getOrPut` — 맵에 없으면 계산해서 넣고 돌려준다(메모이제이션).
- 컨트롤러는 어떤 핸들러가 있는지 모른다 → 결합도가 낮다.

### Validator — 형식 검증, 오류 목록 모으기

```kotlin
// BE:application/validations/create_favorite_set_command_validator.kt
@Component
class create_favorite_set_command_validator : command_validator<create_favorite_set_command> {
    override fun command_type(): KClass<create_favorite_set_command> = create_favorite_set_command::class
    override fun validate(command: create_favorite_set_command) {
        val errors = mutableListOf<String>()
        validate_user_id(command.user_id, errors)
        validate_set_name(command.set_name, errors)
        command.observation?.let { validate_observation_key(it.observation_type_code, it.observation_code, errors) }
        if (errors.isNotEmpty()) throw command_validation_exception("create_favorite_set_command 검증 실패", errors)
    }
}
```

- 역할 분담: **Validator = 입력 형식**(빈 값 · 길이), **Domain = 업무 규칙**(개수 상한 · 이름 중복). 공용 형식 검사 함수는 `viewer_configuration_input_rules.kt` 에 `internal fun` 으로 모았다.

### Handler — 트랜잭션 경계

```kotlin
// BE:application/commands/register_viewer_configuration_command_handler.kt
@Service
class register_viewer_configuration_command_handler(
    private val repository: i_viewer_configuration_repository
) : command_handler<register_viewer_configuration_command, String> {

    @Transactional
    override fun handle(command: register_viewer_configuration_command): String {
        val existing = repository.find_viewer_configuration(command.user_id)
        val config = existing ?: viewer_configuration(user_id = command.user_id)
        command.last_view_layout_code?.let { config.set_last_view_layout(it) }
        command.default_period_days?.let { config.set_default_period(it) }
        return repository.create_or_update_viewer_configuration(config).id
    }
}
```

- `@Transactional` 이 붙은 메서드가 끝날 때 커밋, 예외가 나면 롤백. 코드 안에 commit/rollback 을 쓰지 않는다(선언적 트랜잭션).
- 핸들러는 "조회 → 도메인 메서드 호출 → 저장" 만 한다. 규칙은 도메인에 있다.

### 트랜잭션을 일부러 쪼개는 경우 — self-invocation 함정

```kotlin
// BE:application/commands/analyze_observations_by_patient_command_handler.kt
@Service
class analyze_observations_by_patient_command_handler(
    private val agent_port: i_analysis_agent_port,
    private val result_saver: observation_analyze_result_saver,
) : command_handler<analyze_observations_by_patient_command, String> {

    override fun handle(command: analyze_observations_by_patient_command): String {   // @Transactional 없음
        val agent = agent_port.agent()
        val response = agent.analyze("${command.patient_id} ${LocalDate.now().format(REFERENCE_DATE)}기준")  // 수십 초 걸릴 수 있음
        val result = observation_analyze_result(patient_id = command.patient_id, …)
        return result_saver.save(result)           // 저장만 짧은 트랜잭션 (다른 빈)
    }
}

// BE:application/commands/observation_analyze_result_saver.kt
@Service
class observation_analyze_result_saver(private val repository: i_observation_analyze_result_repository) {
    @Transactional
    fun save(result: observation_analyze_result): String = repository.save_observation_analyze_result(result).id
}
```

- AI 호출 동안 DB 커넥션을 잡고 있지 않으려고 트랜잭션을 저장에만 건다.
- **왜 다른 빈인가**: Spring 의 `@Transactional` 은 **프록시 객체**를 거쳐 호출될 때만 동작한다. 같은 클래스 안에서 `this.save()` 를 부르면 프록시를 거치지 않아 트랜잭션이 안 걸린다(self-invocation). 그래서 별도 빈으로 뺐다. 같은 원리가 `integration_event_handler` 에도 적용된다(2-8).

### Repository — 도메인 계약과 JPA 구현의 분리

```kotlin
// 계약 — BE:domain/aggregatesmodel/viewerconfiguration/i_viewer_configuration_repository.kt
interface i_viewer_configuration_repository : i_repository<viewer_configuration> {
    fun create_or_update_viewer_configuration(config: viewer_configuration): viewer_configuration
    fun find_viewer_configuration(user_id: String): viewer_configuration?
}

// 구현(어댑터) — BE:infrastructure/repositories/viewer_configuration_repository.kt
@Repository
class viewer_configuration_repository(
    private val jpa: viewer_configuration_jpa_repository,
    private val dispatcher: domain_event_dispatcher,
) : i_viewer_configuration_repository {
    override fun create_or_update_viewer_configuration(config: viewer_configuration): viewer_configuration {
        val saved = jpa.save(config)
        dispatcher.dispatch(saved)        // 쌓인 도메인 이벤트를 Spring 이벤트로 발행
        return saved
    }
    override fun find_viewer_configuration(user_id: String): viewer_configuration? =
        jpa.find_first_by_user_id(user_id).firstOrNull()
}

// Spring Data 인터페이스 — BE:infrastructure/diagnostic_result_context.kt
interface viewer_configuration_jpa_repository : JpaRepository<viewer_configuration, String> {
    @Query("select c from viewer_configuration c where c.user_id = :user_id")
    fun find_first_by_user_id(@Param("user_id") user_id: String): List<viewer_configuration>
}
```

- `JpaRepository<엔티티, ID타입>` 을 상속한 **인터페이스만** 선언하면 Spring Data 가 런타임에 구현체를 만들어 준다(`save`, `findById` 등 기본 제공).
- snake_case 프로퍼티 때문에 메서드 이름 기반 쿼리(`findByUserId`)를 쓸 수 없어 `@Query` JPQL 로 명시한다.
- `findById(id).orElse(null)` — Java `Optional` 을 Kotlin nullable 로 바꾸는 관용구.
- `@EnableJpaRepositories(basePackages = ["com.projectx.diagnosticresult.infrastructure"])` (메인 클래스) 가 이 인터페이스들을 찾을 위치를 지정한다.

### domain_event_dispatcher

```kotlin
// BE:infrastructure/domain_event_dispatcher.kt
@Component
class domain_event_dispatcher(private val publisher: ApplicationEventPublisher) {
    fun dispatch(entity: entity) {
        val events = entity.domain_events.toList()
        entity.clear_domain_events()
        events.forEach { publisher.publishEvent(it) }
    }
}
```

- 현재 이 MS 는 자체 도메인 이벤트를 발행하지 않지만 구조는 표준대로 둔다. 쓰는 쪽은 `@TransactionalEventListener(AFTER_COMMIT)` 로 커밋 후 받는다.

## 2-7. 읽기 경로(Query) 호출관계 — QueryDSL

### 전체 체인 (환자 검사결과 목록)

```
HTTP GET /api/v1/diagnostic-result/observations?patient_id=…&from_date=…
 └ observations_controller.search_observations_by_category(…)           [presentation]
    └ i_observation_queries.search_observations_by_category(…)           [application — 계약]
       └ observation_queries (구현, @Transactional(readOnly = true))      [infrastructure]
          ├ 입력 정규화 (빈 문자열 → null, LocalDate → 하루 시작/끝)
          └ observation_query_dsl.search_observations_by_category(…)
             └ JPAQueryFactory: select(projection).from(o).leftJoin(자식 5종)…where…orderBy…fetch()
          ← List<observation_category_read_row>
          └ .map(::to_category_view)  (자식 컬럼 중 채워진 것 고르기)
       ← List<observation_category_view>
 ← ResponseEntity.ok(list) → Jackson 이 JSON(snake_case 키)으로 직렬화
```

- 쓰기 경로와 달리 **Aggregate 를 만들지 않는다**. DB 에서 바로 화면용 DTO 로 projection 한다(CQRS 의 Q).

### Controller

```kotlin
// BE:presentation/observations_controller.kt
@GetMapping
fun search_observations_by_category(
    @RequestParam patient_id: String,
    @RequestParam(required = false) observation_type_code: String?,
    @RequestParam(required = false) category_code: String?,
    @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) from_date: LocalDate?,
    @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) to_date: LocalDate?
): ResponseEntity<List<observation_category_view>> =
    ResponseEntity.ok(queries.search_observations_by_category(patient_id, observation_type_code, category_code, from_date, to_date))

@GetMapping("/{id}")
fun find_observation(@PathVariable id: String): ResponseEntity<observation_category_view> {
    val view = queries.find_observation(id) ?: return ResponseEntity.notFound().build()
    return ResponseEntity.ok(view)
}
```

- 선택 쿼리 파라미터는 `required = false` + nullable 타입. `@DateTimeFormat` 으로 `yyyy-MM-dd` 문자열 → `LocalDate` 자동 변환.
- 단건 없음 → `?: return ResponseEntity.notFound().build()` (404) — 엘비스 + 조기 반환.

### Query 구현 — 정규화와 매핑

```kotlin
// BE:infrastructure/queries/observation_queries.kt
@Service
@Transactional(readOnly = true)              // 클래스에 붙이면 모든 public 메서드에 적용
class observation_queries(private val query_dsl: observation_query_dsl) : i_observation_queries {

    override fun search_observation_patients(keyword: String?, page: Int, size: Int): page_view<observation_patient_view> {
        val safe_size = size.coerceIn(1, MAX_PAGE_SIZE)
        val safe_page = page.coerceAtLeast(0)
        val offset = safe_page.toLong() * safe_size
        val like = keyword?.trim()?.takeIf { it.isNotEmpty() }
        val content = query_dsl.search_observation_patients(like, safe_size, offset)
        val total = query_dsl.count_observation_patients(like)
        val total_pages = ((total + safe_size - 1) / safe_size).toInt()   // 올림 나눗셈
        return page_view(content, safe_page, safe_size, total, total_pages)
    }
}
```

- 구현 파일이 둘로 나뉜다: `*_queries`(트랜잭션 · 입력 보정 · 매핑) 와 `*_query_dsl`(쿼리 자체).

### QueryDSL — 타입세이프 쿼리

```kotlin
// BE:infrastructure/queries/observation_query_dsl.kt
@Repository
class observation_query_dsl(private val query_factory: JPAQueryFactory) {
    private val o = Qobservation.observation                                   // kapt 가 만든 Q클래스
    private val spec = Qspecimen_diagnostics_observation.specimen_diagnostics_observation
    private val img = Qdiagnostic_imaging_observation.diagnostic_imaging_observation
    // func · record · ext …

    private val category_projection = Projections.constructor(
        observation_category_read_row::class.java,     // 이 클래스 생성자에 아래 순서대로 값을 넣는다
        o.id, o.observation_type_code, …, spec.result_value, spec.diagnosis, img.conclusion, …
    )

    fun search_observations_by_category(patient_id: String, observation_type_code: String?, category_code: String?,
                                        from_ts: LocalDateTime?, to_ts: LocalDateTime?): List<observation_category_read_row> {
        val where = BooleanBuilder()
            .and(o.patient_id.eq(patient_id))
            .and(o.is_deleted.isFalse)
        observation_type_code?.let { where.and(o.observation_type_code.eq(it)) }     // 값이 있을 때만 조건 추가
        category_code?.let { where.and(o.category_code.eq(it)) }
        if (from_ts != null && to_ts != null) where.and(o.result_datetime.between(from_ts, to_ts))
        return category_query().where(where).orderBy(o.result_datetime.desc()).fetch()
    }

    private fun category_query() =
        query_factory
            .select(category_projection)
            .from(o)
            .leftJoin(spec).on(spec.id.eq(o.id))
            .leftJoin(img).on(img.id.eq(o.id))
            // … 자식 5종 LEFT JOIN

    fun search_observation_patients(keyword: String?, size: Int, offset: Long): List<observation_patient_view> =
        query_factory
            .select(Projections.constructor(observation_patient_view::class.java,
                o.patient_id, o.patient_name.max().coalesce(""), o.result_datetime.min(), o.result_datetime.max(), o.count()))
            .from(o)
            .where(patient_list_where(keyword))
            .groupBy(o.patient_id)
            .orderBy(o.result_datetime.max().desc())
            .limit(size.toLong()).offset(offset)
            .fetch()
}
```

배울 점:
- **Q클래스**: `o.patient_id` 는 문자열이 아니라 타입 있는 경로 객체라, 컬럼 이름 오타가 **컴파일 오류**가 된다(JPQL 문자열과의 차이).
- **동적 조건**: `BooleanBuilder` + `?.let` 조합으로 "값이 있을 때만 where 추가".
- **Projection**: `Projections.constructor(클래스, 식…)` — SQL 결과를 data class 생성자로 바로 넣는다. 인자 순서가 생성자 순서와 같아야 한다.
- **집계**: `max()` · `min()` · `count()` · `countDistinct()` · `groupBy` · `coalesce("")`.
- **페이징**: `limit` + `offset`.
- `JPAQueryFactory` 빈은 `BE:infrastructure/config/querydsl_config.kt` 의 `@Bean fun jpa_query_factory(entity_manager: EntityManager) = JPAQueryFactory(entity_manager)` 가 만든다.

컬렉션(세트 → 항목)은 생성자 projection 한 번으로 만들 수 없어 **3단계로 나눠 읽고 Kotlin 에서 조립**한다:

```kotlin
// BE:infrastructure/queries/viewer_configuration_query_dsl.kt (요약)
val head = query_factory.select(Projections.constructor(viewer_configuration_head::class.java, c.id, c.user_id, …))
    .from(c).where(c.user_id.eq(user_id)).fetchFirst() ?: return null          // 1) 본체 (없으면 바로 null)
val sets = query_factory.select(…favorite_set_head…).from(c).join(c._favorite_sets, s)
    .where(c.id.eq(head.id)).orderBy(s.sort_order.asc()).fetch()               // 2) 세트 목록
val items_by_set = if (sets.isEmpty()) emptyMap() else query_factory.select(…)
    .from(s).join(s._items, i).where(s.id.`in`(sets.map { it.id })).fetch()
    .groupBy({ it.favorite_set_id }, { favorite_observation_view(…) })        // 3) 항목을 한 번에 → N+1 회피
return viewer_configuration_view(user_id = head.user_id, favorite_sets = sets.map { favorite_set_view(…, items = items_by_set[it.id].orEmpty()) }, …)
```

- `internal data class viewer_configuration_head(…)` 처럼 **조회 내부 전용 DTO** 를 클래스 안에 중첩 선언한다.

### view_model — 응답 계약

```kotlin
// BE:application/queries/observation_view_model.kt
data class page_view<T>(val content: List<T>, val page: Int, val size: Int, val total_elements: Long, val total_pages: Int)
```

- **제네릭 data class**. Controller 는 Entity 를 절대 직렬화하지 않고 view_model 만 내보낸다.

## 2-8. 이벤트 수신 경로(Kafka) 호출관계

### 전체 체인

```
다른 BC 가 Kafka 토픽 `dev.chabd.specimen-diagnostics.laboratory-result` 에 봉투(JSON) 발행
 └ common-eventbus-kt (사내 라이브러리) 컨슈머가 수신
    ├ topic_registry.event_router()  : 토픽 ↔ 이벤트 클래스 ↔ partitionKey ↔ consumer group
    └ 봉투의 @MessageType("LaboratoryResult") 로 laboratory_result_integration_event 로 역직렬화
 └ event_bus_subscription_initializer 가 부팅 때 등록한 람다
    └ result_handler.handle(envelope)                           (프록시 경유 → @Transactional 시작)
       └ integration_event_handler.handle  [템플릿]               (로그 · 예외 재던짐)
          └ observation_result_handler.handle_internal(event)
             ├ is_deleted(event)? → delete: find_observation_by_source → mark_deleted(version) → save
             └ upsert:
                ├ event.to_observation_payload()                (확장 함수 매퍼 — 필수값 검증 · 이름 변환)
                ├ repository.find_observation_by_source(id) ?: new_observation(type_code, UUID)   (팩토리)
                ├ target.apply_source_event(id, event.resolved_event_version(), payload)   (멱등)
                └ true 면 repository.create_or_update_observation(target)
    예외 → 롤백 → 라이브러리가 재시도 → 소진되면 DLT(격리 토픽)
```

### 부팅 시 구독 등록 — `@PostConstruct`

```kotlin
// BE:infrastructure/eventbus/event_bus_subscription_initializer.kt
@Component
class event_bus_subscription_initializer(
    private val event_bus: EventBus,
    private val result_handler: observation_result_handler,
) {
    @PostConstruct                      // 빈 생성 + 주입이 끝난 직후 1회 실행
    fun subscribe() {
        listOf(
            diagnostic_imaging_result_integration_event::class,
            functional_diagnostics_result_integration_event::class,
            laboratory_result_integration_event::class,
            pathology_result_integration_event::class,
            observation_record_group_snapshot_integration_event::class,
            external_record_integration_event::class,
        ).forEach { subscribe(it) { envelope -> result_handler.handle(envelope) } }
    }
}
```

### 토픽 라우팅 — `@Configuration` + `@Bean`

```kotlin
// BE:infrastructure/eventbus/topic_registry.kt
@Configuration
class topic_registry {
    @Bean
    fun event_router(): EventRouter = MapEventRouter(
        buildMap<KClass<*>, EventRoute> {
            put_result_route(laboratory_result_integration_event.TOPIC,
                             laboratory_result_integration_event.PARTITION_KEY_FIELD,
                             laboratory_result_integration_event::class)
            // … 6개
        },
    )
}
```

- `@Configuration` 클래스 안의 `@Bean` 함수 반환값이 스프링 빈이 된다(직접 만든 객체를 빈으로 등록하는 방법).
- 토픽 문자열은 각 이벤트 클래스의 `companion object { const val TOPIC = "specimen-diagnostics.laboratory-result" }` 에 두고 여기서 읽는다 → 문자열이 한 곳에만 있다.

### 이벤트 클래스 계층 — 상속 + 애노테이션만 다른 서브클래스

```kotlin
// 부모: 필드 90여 개를 가진 단일 스키마 (var + 기본값 → setter 바인딩)
abstract class observation_result_integration_event : integration_event() {
    var eventType: String? = null
    var sourceAggregateId: String? = null
    var sourceEventVersion: Long = 0
    var observationTypeCode: String? = null
    var patientId: String? = null
    var resultValue: String? = null
    // …
}

// 자식: 애노테이션(라이브러리 라우팅 키) + 파티션 키 필드 + 토픽 상수
@MessageType("LaboratoryResult")
@SchemaVersion("1")
@Aggregate(type = "LaboratoryResult")
class laboratory_result_integration_event : observation_result_integration_event() {
    override val allowed_observation_type_codes = setOf(specimen_diagnostics_observation.OBSERVATION_TYPE_CODE)
    var laboratoryResultId: String? = null
    companion object {
        const val TOPIC = "specimen-diagnostics.laboratory-result"
        const val PARTITION_KEY_FIELD = "laboratoryResultId"
    }
}
```

- 필드가 너무 많아 생성자 인자 대신 **무인자 생성자 + `var`** 로 두고 Jackson 이 setter 로 채운다. 그래서 필수값 검증은 매퍼(`required(...)`)가 한다.
- payload 필드만 camelCase 다(BC 간 JSON 계약).

### 템플릿 메서드 + 트랜잭션 프록시

```kotlin
// BE:application/integrationevents/eventhandling/integration_event_handler.kt
abstract class integration_event_handler<T : integration_event> {
    @Transactional
    open fun handle(envelope: MessageEnvelope<out T>) {        // open: CGLIB 프록시가 가로챌 수 있게
        val event = envelope.payload
        logger.info { "통합 이벤트 수신: messageType=${envelope.messageType} messageId=${envelope.messageId}" }
        try {
            handle_internal(event)
        } catch (ex: Exception) {
            logger.error(ex) { "통합 이벤트 처리 실패: …" }
            throw ex                                          // 다시 던져야 롤백 + 라이브러리 재시도
        }
    }
    protected abstract fun handle_internal(event: T)          // 여기엔 @Transactional 을 붙이지 않는다
}
```

- 부모 클래스 자체엔 `@Component` 가 없어 `plugin.spring` 이 열어 주지 않는다 → `open` 을 직접 써야 프록시가 이 메서드를 재정의할 수 있다.
- 트랜잭션은 바깥(프록시를 타는) `handle` 에, 실제 일은 안쪽 `handle_internal` 에. `handle_internal` 에 붙이면 self-invocation 이라 무시된다.

### 매퍼 — 계약 이름 → 도메인 이름

```kotlin
// BE:application/integrationevents/eventhandling/observation_payload_mapper.kt (발췌)
internal fun observation_result_integration_event.to_observation_payload(): observation_payload {
    val type_code = observationTypeCode.required("observationTypeCode")
    require(type_code in allowed_observation_type_codes) { "이 토픽이 실어 나를 수 없는 검사분류입니다: $type_code …" }
    return observation_payload(
        patient_id = patientId.required("patientId"),
        encounter_class = encounterClass.blank_to_null(),
        category_code = if (type_code == external_record_observation.OBSERVATION_TYPE_CODE) {
            categoryCode.required("categoryCode").lowercase()
        } else {
            categoryCode?.takeIf { it.isNotBlank() } ?: default_category_code(type_code)
        },
        diagnostic_service_section_code = diagnosticServiceSectionCode?.takeIf { it.isNotBlank() } ?: default_section_code,
        specimen_type_code = specimenTypeCode ?: pathSpecimenTypeCode,
        record_result_value = vitalResultValue,          // 계약 필드명(vital…) → 도메인 필드명(record…)
        // …
    )
}
```

- **Anti-Corruption Layer** 역할: 외부 BC 의 이름 · 결측 · 기본값 규칙을 여기서 흡수하고 도메인은 깨끗한 `observation_payload`(data class VO)만 받는다.
- `default_section_code` 는 자식 클래스가 재정의한 값을 다형적으로 읽는다(병리 토픽은 `"SP"`).

## 2-9. 외부 호출 — Port & Adapter (AI Gateway)

```
analyze_observations_by_patient_command_handler
 └ i_analysis_agent_port.agent()                   [application/ports — 인터페이스]
     └ ai_gateway_analysis_agent_provider          [infrastructure/proxies — @Component 구현]
         └ (설정이 있으면) ai_gateway_agent_analysis_agent 1개를 만들어 보관
 └ analysis_agent.analyze(patient_reference)
     └ ai_gateway_agent_analysis_agent.call_with_retry → RestClient POST /khc/v1/chat/completions
     └ parse_reply (Jackson JsonNode 트리 탐색) → analysis_response_normalizer.normalize
 ← analysis_agent_response(summary, detail, details, model)
```

### Port — application 이 정하는 계약

```kotlin
// BE:application/ports/i_analysis_agent_port.kt
interface i_analysis_agent_port { fun agent(): analysis_agent }

interface analysis_agent {
    val name: String            // 인터페이스에 프로퍼티 선언 가능
    val version: String
    val provider: String
    fun analyze(patient_reference: String): analysis_agent_response
}

data class analysis_agent_response(val summary: String, val detail: String, val details: String, val model: String? = null)
class analysis_agent_unavailable_exception(message: String) : RuntimeException(message)   // → 503
```

### Adapter — 설정이 없으면 degrade (부팅은 막지 않음)

```kotlin
// BE:infrastructure/proxies/ai_gateway_analysis_agent_provider.kt (요약)
@Component
class ai_gateway_analysis_agent_provider(props: ai_gateway_agent_properties, object_mapper: ObjectMapper,
                                         normalizer: analysis_response_normalizer) : i_analysis_agent_port {
    private val agent: analysis_agent? = when {
        props.api_key.isBlank() -> { logger.warn { "…" }; null }
        props.agent_id.isBlank() -> { logger.warn { "…" }; null }
        else -> ai_gateway_agent_analysis_agent(…, rest_client = build_rest_client(props), …)
    }
    override fun agent(): analysis_agent =
        agent ?: throw analysis_agent_unavailable_exception("사용할 수 있는 AI 분석 에이전트가 없습니다")

    private fun build_rest_client(props: ai_gateway_agent_properties): RestClient =
        RestClient.builder()
            .requestFactory(build_request_factory(…))
            .baseUrl(props.base_url)
            .defaultHeader("X-API-Key", props.api_key)
            .build()
}
```

- 생성자 인자에 `val` 이 없다(`props: …`) → 필드가 아니라 **초기화 중에만 쓰는 인자**. 프로퍼티 초기화식(`private val agent = when {…}`)에서 쓸 수 있다.
- `ai_gateway_agent_analysis_agent` 는 `@Component` 가 아닌 일반 클래스 — provider 가 `new` 해서 들고 있다(Spring 이 아닌 코드가 객체를 만드는 경우).
- **빌더 패턴**: `RestClient.builder()…build()`, `HttpClients.custom()…build()`.
- 설정 클래스:

```kotlin
// BE:infrastructure/proxies/ai_gateway_agent_properties.kt
@Component
@ConfigurationProperties(prefix = "diagnosticresult.analysis.ai-gateway")
class ai_gateway_agent_properties {
    var base_url: String = "https://dev-ai-gateway-api.kakaohealthcare.com"
    var api_key: String = ""
    var agent_id: String = ""
    var read_timeout_ms: Long = 60000
    var max_retries: Int = 1
    // …
}
```

- yml 의 `diagnosticresult.analysis.ai-gateway.api-key` 가 `api_key` 에 바인딩된다(Spring 의 느슨한 이름 바인딩: kebab-case ↔ snake/camel).

## 2-10. 예외 → HTTP 상태 매핑

```kotlin
// BE:infrastructure/config/global_exception_handler.kt
@RestControllerAdvice                                   // 모든 컨트롤러의 예외를 가로챈다
class global_exception_handler {
    @ExceptionHandler(diagnostic_result_not_found_exception::class)
    fun handle_not_found(ex: diagnostic_result_not_found_exception): ResponseEntity<error_response> =
        ResponseEntity.status(HttpStatus.NOT_FOUND).body(error_response(error = "NotFound", message = ex.message ?: "…"))

    @ExceptionHandler(diagnostic_result_validation_exception::class, command_validation_exception::class, IllegalArgumentException::class)
    fun handle_validation(ex: Exception): ResponseEntity<error_response> {
        val details: List<String> = if (ex is command_validation_exception) ex.errors else emptyList()
        …
    }
    // …
}
data class error_response(val error: String, val message: String, val details: List<String> = emptyList())
```

| 예외 | HTTP | 언제 |
|---|---|---|
| `diagnostic_result_not_found_exception` | 404 | 세트 id 없음 등 |
| `diagnostic_result_validation_exception` · `command_validation_exception` · `IllegalArgumentException`(`require` 실패) | 400 | 입력 형식 오류 |
| `diagnostic_result_domain_exception`(위 둘 외의 하위 포함) | 422 | 업무 규칙 위반(세트 20개 초과, 이름 중복) |
| `OptimisticLockingFailureException` | 409 | 동시 저장 충돌(`@Version`) |
| `analysis_agent_unavailable_exception` | 503 | AI 설정 없음 |
| 그 외 `Exception` | 500 | 고정 문구만 응답, 원인은 로그로만 |

- Spring 은 **가장 구체적인 예외 타입**의 핸들러를 고른다. not_found 가 domain_exception 의 자식이어도 404 핸들러가 먼저 잡힌다 — 상속 계층이 곧 상태 코드 정책이다.

## 2-11. 설정 · 부팅 · DB 마이그레이션

### 메인 클래스

```kotlin
// BE:diagnostic_result_application.kt
@SpringBootApplication                     // 컴포넌트 스캔 + 자동 설정의 시작점
@EnableJpaRepositories(basePackages = ["com.projectx.diagnosticresult.infrastructure"])
@EnableScheduling
class diagnostic_result_application

fun main(args: Array<String>) {
    runApplication<diagnostic_result_application>(*args)
}
```

- 본문 없는 클래스는 `{}` 를 생략할 수 있다. `main` 은 최상위 함수.

### application.yml — 환경변수 + 기본값

```yaml
server:
  port: ${SERVER_PORT:5351}               # ${환경변수:기본값}
spring:
  datasource:
    url: jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5432}/${DB_NAME:sup}
    username: ${DB_USER}                  # 기본값 없음 → 없으면 부팅 실패 (자격증명을 소스에 두지 않는다)
    hikari:
      connection-init-sql: SET search_path TO ${DB_SCHEMA:diagnostic_result}
  jpa:
    hibernate:
      ddl-auto: ${SPRING_JPA_DDL_AUTO:validate}     # 스키마는 Liquibase 가, Hibernate 는 검증만
    open-in-view: false
  liquibase:
    change-log: classpath:db/changelog/db.changelog-master.yaml
    default-schema: ${DB_SCHEMA:diagnostic_result}
```

- 로컬은 `backend/.env` 를 `spring-dotenv` 로 읽는다. Boot 4 호환을 위해 `dotenv_environment_post_processor`(`EnvironmentPostProcessor` 구현)를 `META-INF/spring.factories` 로 등록한다 — 빈 생성 **이전**에 환경값을 넣는 확장 지점.

### 부팅 순서에 끼어들기 — BeanPostProcessor

```kotlin
// BE:infrastructure/config/db_schema_initializer.kt
@Configuration
class db_schema_initializer(
    private val data_source_provider: ObjectProvider<DataSource>,               // 늦게 꺼내 쓰기
    @Value("\${DB_SCHEMA:diagnostic_result}") private val schema_name: String,    // \$ 는 Kotlin 템플릿 이스케이프
) : BeanPostProcessor {
    override fun postProcessBeforeInitialization(bean: Any, bean_name: String): Any {
        if (bean is SpringLiquibase) create_schema_if_absent()                   // Liquibase 빈 초기화 직전에 스키마 생성
        return bean
    }
}
```

- `BeanPostProcessor` 는 **모든 빈**이 초기화될 때마다 호출되는 훅이다. 타입(`is SpringLiquibase`)으로 골라 "Liquibase 가 돌기 직전" 시점을 잡는다.
- `@Value("\${…}")` — Kotlin 문자열에서 `$` 는 템플릿이라 `\$` 로 이스케이프해야 Spring 플레이스홀더로 전달된다.
- `ObjectProvider<T>` — 의존 빈을 생성 시점이 아니라 사용 시점에 꺼낸다(순환·초기화 순서 문제 회피).

### Liquibase — 스키마 변경 이력

```yaml
# db/changelog/db.changelog-master.yaml
databaseChangeLog:
  - include: { file: changes/001-create-tables.xml, relativeToChangelogFile: true }
  - include: { file: changes/004-add-observation-is-abnormal.xml, relativeToChangelogFile: true }
```

```xml
<!-- changes/004-add-observation-is-abnormal.xml -->
<changeSet id="diagnostic-result-004-add-observation-is-abnormal" author="diagnostic-result">
    <addColumn tableName="observation">
        <column name="is_abnormal" type="BOOLEAN" defaultValueBoolean="false">
            <constraints nullable="false"/>
        </column>
    </addColumn>
    <rollback/>
</changeSet>
```

- 앱이 뜰 때 아직 적용 안 된 changeSet 을 순서대로 실행한다. 적용된 changeSet 은 체크섬이 기록되므로 **고치지 않고 새 파일을 추가**한다. DROP 같은 파괴적 변경은 빌드 가드가 막는다.

### 기타 설정 빈

- `security_config` — `SecurityFilterChain` 빈을 람다 DSL 로 구성: `http.csrf { it.disable() }.authorizeHttpRequests { auth -> auth.anyRequest().permitAll() }`.
- `web_config : WebMvcConfigurer` — 인터페이스의 일부 메서드만 `override` 해서 CORS · 루트 리다이렉트 추가(기본 메서드가 있는 인터페이스).

## 2-12. 테스트

```kotlin
// backend/diagnostic-result/src/test/kotlin/…/domain/favorite_set_rules_test.kt
class favorite_set_rules_test {
    private val glu = favorite_observation("SPECIMEN_DIAGNOSTICS", "GLU")

    @Test
    fun `검사 배정은 고른 세트에만 넣고 나머지 세트에서는 뺀다`() {
        val config = viewer_configuration("u1")
        val a = config.add_favorite_set("기본", first_item = glu)
        val b = config.add_favorite_set("신장")
        val c = config.add_favorite_set("외래")

        config.assign_observation_to_sets("SPECIMEN_DIAGNOSTICS", "GLU", setOf(b.id, c.id))

        assertThat(a.items).doesNotContain(glu)
        assertThat(b.items).containsExactly(glu)
    }

    @Test
    fun `모르는 세트 id 로 배정하면 아무것도 바꾸지 않고 404`() {
        val config = viewer_configuration("u1")
        val a = config.add_favorite_set("기본", first_item = glu)
        assertThatThrownBy { config.assign_observation_to_sets("SPECIMEN_DIAGNOSTICS", "GLU", setOf("없는-세트")) }
            .isInstanceOf(diagnostic_result_not_found_exception::class.java)
        assertThat(a.items).containsExactly(glu)
    }
}
```

- 도메인이 Spring 에 의존하지 않으므로 **스프링 컨텍스트 없이 `new` 만으로** 규칙을 테스트한다(빠르고 단순). DDD 레이어 분리의 실익.
- JUnit5 `@Test` + AssertJ `assertThat`/`assertThatThrownBy`.
- `result_event_routing_test` 는 `topic_registry().event_router()` 를 직접 만들어 토픽 문자열 · 파티션 키(오타가 나도 컴파일되는 계약)를 검증한다.

## 2-13. 백엔드 디자인 패턴 요약표

| 패턴 | 위치 | 한 줄 설명 |
|---|---|---|
| Layered / Hexagonal | 패키지 4개 + ArchUnit | 안쪽(domain)은 바깥을 모른다 |
| CQRS | `application/commands` vs `application/queries` | 쓰기 = Aggregate, 읽기 = projection |
| Mediator | `command_bus` | 컨트롤러는 핸들러를 모르고 버스에 보낸다 |
| Command / Handler | `*_command` + `*_command_handler` | 커맨드 1 : 핸들러 1 |
| Aggregate Root | `observation` · `viewer_configuration` · `observation_analyze_result` | 일관성 경계, 자식은 Root 경유 |
| Value Object | `favorite_observation` · `observation_payload` | 값으로 동치 |
| Repository | `i_*_repository` + 구현 | 컬렉션처럼 보이는 저장소 |
| Factory | `new_observation` | 분류 코드 → 자식 타입 |
| Template Method | `observation.apply_source_event` · `integration_event_handler.handle` | 순서는 부모, 빈 칸은 자식 |
| Idempotent Consumer | `apply_source_event` · `mark_deleted` | 버전 비교로 중복 · 역순 무해화 |
| Anti-Corruption Layer | `observation_payload_mapper` | 외부 계약 → 도메인 언어 |
| Port & Adapter | `i_analysis_agent_port` ↔ `ai_gateway_*` | 외부 시스템을 인터페이스 뒤로 |
| Retry | `call_with_retry` | 실패 시 N회 재시도 |
| Graceful Degradation | agent provider · 구독 초기화 `runCatching` | 부가 기능 실패가 부팅을 막지 않음 |
| Optimistic Locking | `@Version` | 충돌 시 409 |

---

# 3부. 프론트엔드 (React · TypeScript)

## 3-1. 빌드 · 런타임 — Module Federation remote

### 패키지 구조 (yarn workspaces)

```json
// frontend/package.json
{
  "packageManager": "yarn@1.22.22",
  "workspaces": ["apps/*"],
  "scripts": {
    "dev": "yarn workspace diagnostic-result dev",
    "lint": "eslint apps",
    "typecheck": "yarn workspace diagnostic-result exec tsc --noEmit",
    "check:screens": "node node_modules/@x-ehr/design-system/scripts/check-screen-usage.mjs apps/diagnostic-result/src"
  }
}
```

주요 의존성(`apps/diagnostic-result/package.json`): `react 19.2.3` · `react-router ^7` · `zustand ^5`(전역 상태) · `axios` · `@x-ehr/api-axios`(공용 HTTP) · `@x-ehr/event-bus`(remote 간 통신) · `@x-ehr/design-system`(UI 킷) · `@x-ehr/auth-contract`(로그인 사용자) · `@x-ehr/app-kit`(네비게이션) · 빌드는 `@rspack/*`(webpack 호환 Rust 번들러) + `@module-federation/enhanced` + `tailwindcss 4`.

### rspack.config.js — remote 선언

```js
const { createRemoteRspackConfig } = require('@x-ehr/rspack-config')
const configFn = createRemoteRspackConfig({
  dirname: __dirname,
  name: 'diagnostic_result',       // remoteKey — host 가 `${base}/diagnostic_result/remoteEntry.js` 로 찾는다
  port: 3351,
  shared: ['@x-ehr/app-kit', '@x-ehr/auth-contract', '@x-ehr/design-system'],   // host 와 싱글턴 공유
})
module.exports = (env, argv) => {
  const cfg = configFn(env, argv)
  if (cfg.devServer) {
    cfg.devServer.proxy = [
      ...(Array.isArray(cfg.devServer.proxy) ? cfg.devServer.proxy : []),
      { context: ['/api/v1/diagnostic-result'], target: 'http://localhost:5351', changeOrigin: true },   // 단독 dev 시 백엔드로
    ]
  }
  return cfg
}
```

- **Module Federation**: host 앱이 실행 중에 이 remote 의 `remoteEntry.js` 를 내려받아 `./routes` 를 꺼내 자기 라우터에 끼운다. remote 는 코드가 아니라 **규약(이름 · 경로)** 으로 붙는다.
- `shared` — React · 킷 등은 host 와 **한 벌**을 공유해야 한다(두 벌이면 Context · 훅이 깨진다).

### 진입점 3개

```ts
// FE:index.ts — MF 비동기 경계
void import('./bootstrap')
```

- 동적 `import()` 로 한 번 끊어야 MF 가 shared 모듈 협상을 마친 뒤 React 를 로드한다. `void` 는 "Promise 결과를 일부러 버린다" 는 표시(린트의 floating promise 경고 회피).

```tsx
// FE:bootstrap.tsx — 단독 실행일 때만 쓰는 루트
import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import Standalone from './app/Standalone'
import './styles/globals.css'

const container = document.getElementById('root')
if (!container) throw new Error('Root element #root not found')
createRoot(container).render(
  <StrictMode>
    <Standalone />
  </StrictMode>,
)
```

- `createRoot(…).render(<App/>)` — React 18+ 루트 API.
- `StrictMode` — 개발 모드에서 effect 를 일부러 두 번 실행해 정리(cleanup) 누락을 드러낸다. 이 코드의 effect 들이 cleanup 에서 상태를 리셋하는 이유 중 하나.
- `if (!container) throw …` 뒤로 `container` 는 `HTMLElement`(null 아님)로 **좁혀진다**(TS 제어 흐름 분석).

```tsx
// FE:routes.tsx — host 에 노출되는 유일한 모듈 (3-9 에서 상세)
export default routes
```

## 3-2. 폴더 구조 — FSD 레이어

```
views → widgets → features → entities → shared     (위에서 아래로만 import, 역방향 금지)
```

| 레이어 | 이 레포의 예 | 무엇을 두나 |
|---|---|---|
| `views/` | `diagnostic-result-view/DiagnosticResultView.tsx` · `patient-list/PatientList.tsx` · `summary/ReportSummary.tsx` | 라우트 1개 = 화면 1개. 조립만 |
| `widgets/` | `diagnostic-result/wide/WideMain.tsx` · `ObservationList.tsx` · `compact/CompactMain.tsx` · `useDiagnosticResultData.ts` · 스토어 | 화면 블록 + 그 블록의 상태/파생 |
| `features/` | `favorite-set/ui/FavoriteSetPicker.tsx` · `FavoriteSetManageDialog.tsx` | 사용자 행위 하나 |
| `entities/` | `observation/{api,model,ui}` · `viewer-configuration/{api,model}` · `analysis/{api,model,ui}` | 도메인 타입 · API · 순수 함수 · 작은 표시 부품 |
| `shared/` | `api/apiBase.ts` · `lib/events.ts` · `lib/viewBus.ts` · `lib/useContainerTier.ts` · `store/chartModeStore.ts` · `patient-session/*` | 도메인 무관 공용 |
| `mocks/` | `patient-results/store.ts` | 단독 개발용 가짜 데이터 |

규칙(eslint 로 강제):
- import 는 `@/…` **절대경로만**(`../` 금지). `tsconfig.json` 의 `"paths": { "@/*": ["./src/*"] }`.
- `shared/ui` 외에는 `index.ts` 배럴 금지 — 파일을 직접 import 한다.
- `features` 끼리 직접 참조 금지.

## 3-3. TypeScript 문법 — 이 코드에 실제로 나오는 것들

### (1) `interface` 와 `type`

```ts
// FE:entities/observation/model/types.ts
export interface Observation {
  id: string
  observationTypeCode: string
  diagnosticServiceSectionCode: string | null      // 유니언: string 또는 null
  deltaValue: number | null
  isAbnormal: boolean
  observationCategory?: string                      // ? = 있을 수도 없을 수도 (undefined 허용)
  aggregationMethod?: AggregationMethod | null
}

export type AggregationMethod = 'EACH' | 'RANGE' | 'SUM' | 'DOSE'   // 문자열 리터럴 유니언
export interface Page<T> { content: T[]; page: number; size: number; totalElements: number; totalPages: number }
```

- `interface` — 객체 모양. 확장(`extends`) · 병합(declaration merging)이 된다.
- `type` — 별칭. 유니언 · 튜플 · 함수 타입 등 무엇이든.
- `| null` 과 `?` 의 차이: `| null` 은 "키는 있고 값이 null 일 수 있음"(백엔드가 null 을 보냄), `?` 는 "키 자체가 없을 수 있음".
- **문자열 리터럴 유니언**은 Kotlin enum 처럼 쓰인다: `type ListMode = 'flat' | 'date' | 'agg' | 'time'`, `type ContainerTier = 'S' | 'M' | 'L'`. 오타는 컴파일 오류.

### (2) 제네릭

```ts
// FE:shared/api/apiBase.ts
export async function get<T>(path: string, params?: QueryParams): Promise<T> {
  const res = await api.get<T>(PREFIX + path, { params: cleanParams(params) })
  return res.data
}
// 사용
const res = await get<Record<string, unknown>>('/observations/patients', { keyword, page, size })
```

```tsx
// 컴포넌트에도 타입 인자를 준다
<DataGrid<Observation> rows={rows as Observation[]} columns={flatColumns} getRowId={(o) => o.id} … />

// 훅 제네릭 + 기본 타입 + 상한
export function useContainerTier<T extends HTMLElement = HTMLDivElement>(): { ref: (el: T | null) => (() => void) | undefined; tier: ContainerTier | null }
```

### (3) 유틸리티 타입 — `Record` `Pick` `Omit` `Partial`

```ts
export type QueryParams = Record<string, string | number | boolean | undefined | null>   // 키 string → 값 타입
export const DEFAULT_VIEWER_CONFIG: Omit<ViewerConfig, 'userId'> = { favoriteSets: [], lastViewLayoutCode: 'BY_CATEGORY', defaultPeriodDays: 1095 }
const patch: Partial<WideState> = {}                     // 모든 필드 선택적 — 바꿀 것만 담는 객체
function PickerBody({ observation, actions }: Pick<FavoriteSetPickerProps, 'observation' | 'actions'>) { … }
export function publishSessionPatientStandIn(patient: Pick<SessionPatient, 'patientId' | 'patientName'> | null): void
const LAYOUT_PRESET: Record<string, { listMode: ListMode; sort: string; chip: ChipKey | null }> = { … }
```

### (4) `as const` 와 배열에서 유니언 뽑기

```ts
// FE:entities/observation/model/constants.ts
export const TYPE_ORDER = [
  'SPECIMEN_DIAGNOSTICS', 'DIAGNOSTIC_IMAGING', 'FUNCTIONAL_DIAGNOSTICS', 'CLINICAL_RECORD', 'MEDICATION',
] as const
export type TypeCode = (typeof TYPE_ORDER)[number]
// TypeCode = 'SPECIMEN_DIAGNOSTICS' | 'DIAGNOSTIC_IMAGING' | … (배열 원소 전체의 유니언)

// FE:shared/lib/useContainerTier.ts
export const TIER_MIN_WIDTH = { M: 640, L: 1200 } as const
```

- `as const` — 값을 **가장 좁은 리터럴 타입 + readonly** 로 고정한다. 그 값에서 `typeof X[number]` 로 타입을 파생하면 **값과 타입이 한 곳**에서 나온다.

### (5) `unknown` · 타입 단언 `as` · 안전한 변환

```ts
// FE:entities/observation/api/observationApi.ts
const asRecord = (v: unknown): Record<string, unknown> => (v ?? {}) as Record<string, unknown>

export const toObservation = (raw: unknown): Observation => {
  const o = asRecord(raw)
  return {
    id: o.id as string,
    categoryCode: (o.category_code as string) ?? '',
    deltaValue: typeof o.delta_value === 'number' ? o.delta_value : null,   // 타입 가드로 좁히기
    isAbnormal: o.is_abnormal === true,
    // …
  }
}
```

- 서버 응답은 믿을 수 없으므로 `unknown` 으로 받고, 하나씩 단언 · 검사하며 화면 타입으로 옮긴다.
- `as` 는 "컴파일러에게 이 타입이라고 믿게" 하는 것(런타임 검사 없음). 정말 검사가 필요한 곳은 `typeof … === 'number'`, `Array.isArray(res)`.
- `catch (e: unknown)` 후 `(e as Error)?.message` — catch 변수는 `unknown` 이라 단언해서 쓴다.

### (6) `?.` 옵셔널 체이닝 · `??` 널 병합 (182번 사용)

```ts
const userId = useCurrentUser()?.email || null       // 사용자가 없으면 undefined → || 로 null
const patientId = patient?.patientId ?? null
const width = entry.borderBoxSize?.[0]?.inlineSize ?? entry.contentRect.width   // 배열 인덱스에도 ?.
```

- `??` 는 null/undefined 일 때만 대체, `||` 는 `''` · `0` · `false` 에도 대체. 이 코드는 대부분 `??` 를 쓰고, 빈 문자열도 "없음" 으로 보고 싶을 때만 `||` 를 쓴다(`userId` 가 그 예).

### (7) 구조분해 · 기본값 · 전개(spread)

```ts
// 매개변수 구조분해 + 기본값 + 전체 기본값
export async function fetchObservationPatients(
  { keyword, page = 0, size = 20 }: { keyword?: string; page?: number; size?: number } = {},
): Promise<Page<PatientRow>> { … }

// 객체 전개 — 불변 업데이트
setConfig((prev) => ({ ...prev, favoriteSets: cfg.favoriteSets }))
return on ? (inSet(o, set) ? set : { ...set, items: [...set.items, keyOf(o)] }) : …

// 조건부 키 넣기 — 값이 있을 때만 필드를 만든다
applyAgentCommand({
  intent: viewParams.action === 'reset' ? 'RESET' : 'FILTER',
  ...(viewParams.patientId ? { patientId: viewParams.patientId } : {}),
  params: { ...(viewParams.fromDate ? { dateFrom: viewParams.fromDate } : {}) },
})

// Set 으로 중복 제거 후 배열로
const next = on ? [...new Set([...ids, set.id])] : ids.filter((id) => id !== set.id)

// 배열 구조분해로 두 원소 교환 (앞의 ; 는 ASI 함정 방지 — 앞줄과 붙어 해석되지 않게)
;[ids[i], ids[j]] = [ids[j] as string, ids[i] as string]
```

### (8) `async` / `await` / `Promise.all`

```ts
// FE:widgets/diagnostic-result/useDiagnosticResultData.ts
Promise.all([
  userId ? fetchViewerConfig(userId) : Promise.resolve({ ...DEFAULT_VIEWER_CONFIG, userId: '' }),
  fetchObservations({ patientId }),
])
  .then(([cfg, obs]) => { … })     // 결과를 튜플로 구조분해
  .catch((e: unknown) => { … })
```

- 두 요청을 **병렬**로 보내고 둘 다 끝나면 진행. `then` 의 인자 `[cfg, obs]` 는 각각 정확한 타입으로 추론된다.

```ts
// FE:entities/viewer-configuration/api/viewerConfigurationApi.ts
export async function fetchViewerConfig(userId: string): Promise<ViewerConfig> {
  if (isMock()) return (await mock()).getViewerConfig(userId)
  try {
    return toViewerConfig(await get<unknown>(configPath(userId)))
  } catch (e) {
    if (e instanceof ApiError && e.status === 404) return { ...DEFAULT_VIEWER_CONFIG, userId }   // 404 = 정상(기본값)
    throw e
  }
}
```

### (9) 클래스 — 커스텀 에러

```ts
// FE:shared/api/apiBase.ts
export class ApiError extends Error {
  readonly status: number
  constructor(message: string, status = 0) {
    super(message)
    this.name = 'ApiError'
    this.status = status
  }
}
```

- `instanceof ApiError` 로 구분해 404 분기 등을 한다. 이 코드에서 클래스는 거의 이것뿐이고 나머지는 함수 + 객체다(React 는 함수 중심).

### (10) 전역 · 모듈 타입 확장 — `declare global` · `declare module`

```ts
// FE:shared/api/apiBase.ts — window 에 필드 추가
declare global {
  interface Window { __DIAGNOSTIC_RESULT_API_BASE__?: string | null }
}

// FE:shared/lib/events.ts — 라이브러리 타입에 이벤트 등록 (모듈 보강 · declaration merging)
declare module '@x-ehr/event-bus' {
  interface EhrEventMap {
    'ehr:patient:selected': PatientSelectedEvent
    'ehr:app:view-params': ViewParamsEvent
    'ehr:diagnostic:agent-command': AgentCommand
  }
}

// FE:global.d.ts — CSS import 를 타입 오류 없이
declare module '*.css'
```

- `interface` 는 같은 이름으로 다시 선언하면 **합쳐진다**. 라이브러리의 `EhrEventMap` 에 이 remote 의 이벤트를 추가하면 `publish('ehr:app:view-params', payload)` 의 payload 타입이 검사된다. 등록 안 한 이벤트 이름은 컴파일 오류.

### (11) 인덱스 시그니처 · 타입 전용 import

```ts
export interface ViewParams {
  patientId?: string
  fromDate?: string
  action?: 'query' | 'save' | 'reset'
  [key: string]: unknown          // 그 밖의 임의 키도 허용
}

import type { Observation } from '@/entities/observation/model/types'          // 타입만 — 번들에서 사라짐
import { useContainerTier, type ContainerTier } from '@/shared/lib/useContainerTier'   // 값과 타입 섞어서
```

### (12) 판별 · 좁히기

```ts
// FE:entities/observation/model/sort.ts
export type ListUnit = Observation | ObservationGroup
export const repOf = (u: ListUnit): Observation => ('latest' in u ? u.latest : u)
```

- `'latest' in u` — 그 키가 있으면 `ObservationGroup` 으로 **좁혀진다**(in 연산자 타입 가드).

## 3-4. React 기본 — 컴포넌트 · JSX · props

### 함수 컴포넌트와 props 타입

```tsx
// FE:widgets/diagnostic-result/wide/cells/FavoriteButton.tsx
export default function FavoriteButton({
  row,
  favoriteSets,
  open,
  onOpenChange,
}: {
  row: Observation
  favoriteSets: FavoriteSetActions
  open: boolean
  onOpenChange: (open: boolean) => void
}) {
  const on = isFav(row, favoriteSets.sets)
  return (
    <FavoriteSetPicker observation={row} actions={favoriteSets} open={open} onOpenChange={onOpenChange}>
      <Button
        size="sm"
        variant="ghost"
        icon={<Star />}
        label={on ? '즐겨찾기 세트 변경' : '즐겨찾기 세트에 추가'}
        className={on ? 'text-(--color-warning-subtle-foreground)' : 'text-(--color-foreground-tertiary)'}
      />
    </FavoriteSetPicker>
  )
}
```

- 컴포넌트 = **props 를 받아 JSX 를 돌려주는 함수**. 이름은 PascalCase.
- props 는 객체 하나 — 매개변수에서 바로 구조분해한다. 타입은 인라인 객체 타입 또는 `interface XxxProps`.
- JSX 안의 `{ }` 는 JavaScript 식. 속성에 식 · 함수 · 다른 JSX(`icon={<Star />}`)를 넘길 수 있다.
- 태그 사이의 내용은 `children` prop 으로 전달된다(위에서 `<Button>` 이 `FavoriteSetPicker` 의 children).

```tsx
// FE:features/favorite-set/ui/FavoriteSetPicker.tsx
export interface FavoriteSetPickerProps {
  observation: FavoriteKey & { observationName?: string }    // 교차 타입 &
  actions: FavoriteSetActions
  open: boolean
  onOpenChange: (open: boolean) => void
  children: ReactNode                                         // 무엇이든 렌더 가능한 것
}
```

### 조건부 렌더링

```tsx
// FE:views/diagnostic-result-view/DiagnosticResultView.tsx
<div ref={ref} className="dr-tier-root typo-body-md-regular" data-tier={tier ?? undefined}>
  {tier == null ? null : patient ? (
    <ResultsBody key={patient.patientId} patientId={patient.patientId} userId={userId} tier={tier} viewParams={viewParams} />
  ) : (
    <EmptyBody tier={tier} />
  )}
</div>

// && — 참일 때만
{apiError && <Banner variant="error" description={apiError} onClose={clearApiError} />}
{tier !== 'L' && <DetailDialog observations={results.observations} favoriteSets={results.favoriteSets} />}

// 조기 return
if (loading) return <Stack align="center" justify="center" className="h-full"><Spinner size="md">불러오는 중…</Spinner></Stack>
if (loadError) return <div className="p-(--spacing-5)"><Banner … /></div>
```

- `null` 을 반환하면 아무것도 그리지 않는다.
- `data-tier={tier ?? undefined}` — undefined 속성은 DOM 에 안 붙는다.

### 리스트 렌더링과 `key`

```tsx
// FE:features/favorite-set/ui/FavoriteSetPicker.tsx
{sets.map((set) => (
  <Stack key={set.id || 'unsaved-default'} direction="row" align="center" gap={2}>
    <Checkbox checked={inSet(observation, set)} disabled={busy} onCheckedChange={(on) => toggle(set, on)}>
      {set.setName}
    </Checkbox>
  </Stack>
))}
```

- 배열 → JSX 배열. 각 원소에 **안정적인 `key`** 가 있어야 React 가 어떤 항목이 바뀌었는지 안다.

### `key` 로 컴포넌트를 통째로 다시 만들기 (remount)

```tsx
<ResultsBody key={patient.patientId} … />
```

- `key` 가 바뀌면 React 는 그 컴포넌트를 **버리고 새로 만든다** → 안의 `useState` · `useRef` 가 전부 초기화된다. "환자가 바뀌면 이전 환자의 상태를 하나도 남기지 않는다" 를 한 줄로 해결하는 이 화면의 핵심 기법.

### Fragment

```tsx
return (
  <>
    {tier === 'L' ? <div className="drp-root">…</div> : <div className="drp-root">…</div>}
    {tier !== 'L' && <DetailDialog … />}
  </>
)
```

- 래퍼 DOM 없이 여러 형제를 반환할 때 `<> </>`.

### 이벤트 · 제어 컴포넌트(controlled input) · 한글 입력

```tsx
// FE:features/favorite-set/ui/FavoriteSetPicker.tsx
const [newName, setNewName] = useState('')

const onNameKeyDown = (e: KeyboardEvent<HTMLInputElement>) => {
  // 한글 조합 중 Enter 는 조합 확정이다 — 여기서 보내면 끝 글자가 두 번 들어간다.
  if (e.key === 'Enter' && !e.nativeEvent.isComposing) void add()
}

<TextField
  value={newName}                                 // 값은 state 가 소유
  onChange={(e) => setNewName(e.target.value)}    // 입력마다 state 갱신 → 재렌더
  onKeyDown={onNameKeyDown}
  maxLength={FAVORITE_SET_NAME_MAX}
  disabled={!addable}
/>

// 이벤트 전파 차단 — 팝오버 안 클릭이 표 행 클릭(상세 열기)으로 번지지 않게
const stop = (e: SyntheticEvent) => e.stopPropagation()
<span onClick={stop} onKeyDown={stop}>…</span>
```

- **제어 컴포넌트**: 입력값의 단일 출처가 React state. `value` + `onChange` 짝.
- `KeyboardEvent<HTMLInputElement>` · `SyntheticEvent` · `FormEvent` — React 이벤트 타입(`import { type KeyboardEvent } from 'react'`).
- 포털(Popover 는 body 에 그려진다) 안의 이벤트도 **React 트리 기준**으로 부모에게 버블링된다 → 그래서 감싸는 span 에서 막는다.

### 렌더 프롭(render prop) — 함수를 children 으로

```tsx
// FE:widgets/diagnostic-result/wide/listModes.tsx
<Tooltip content={TIME_MODE_LOCKED_TIP} placement="top">
  {(anchor) => <span {...anchor}>시간대별</span>}
</Tooltip>
```

- children 자리에 **함수**를 넘기면 킷이 앵커 props(ref · 이벤트 핸들러)를 인자로 주고, 우리는 그것을 원하는 요소에 펼쳐(`{...anchor}`) 붙인다.

## 3-5. React 훅 — 이 코드의 사용 패턴

훅 규칙: 컴포넌트/커스텀 훅의 **최상위에서만**, **항상 같은 순서로** 호출한다(조건문 · 반복문 안 금지). 이름은 `use` 로 시작.

### `useState` — 지역 상태

```ts
const [observations, setObservations] = useState<Observation[]>([])
const [received, setReceived] = useState<ReceivedParams | null>(null)

// 지연 초기화 — 함수를 넘기면 첫 렌더에 한 번만 호출
const [session, setSession] = useState(readRetainedSession)       // FE:shared/patient-session/ancillaryPatientSession.ts

// 함수형 업데이트 — 이전 값 기반으로 안전하게
setReloadSeq((s) => s + 1)
setConfig((prev) => ({ ...prev, favoriteSets: optimistic(prev.favoriteSets) }))
```

- state 는 **불변**으로 다룬다 — 기존 배열/객체를 고치지 않고 새로 만들어 set 한다(그래야 React 가 변경을 감지).

### `useEffect` — 렌더 후 부수효과 (데이터 로딩 · 구독)

```ts
// FE:entities/analysis/model/useAnalysis.ts
useEffect(() => {
  let alive = true
  void fetchAnalysesForPatient(patientId, { skip: 0, take: PAGE_SIZE }).then((res) => {
    if (!alive) return                          // 이미 정리된 effect 의 늦은 응답은 버린다
    setLoaded({ key, list: res.data ?? [], error: res.error ?? null })
  })
  return () => { alive = false }                // cleanup — 의존값이 바뀌거나 언마운트될 때
}, [patientId, key])                            // 의존성 배열 — 이 값이 바뀔 때만 다시 실행
```

- **`alive` 플래그 패턴**: 환자를 A → B 로 빠르게 바꾸면 A 응답이 B 화면에 덮어쓰는 경쟁 상태(race)가 생긴다. cleanup 에서 `alive = false` 로 막는다. 이 코드의 모든 로딩 effect 가 이 모양이다.
- 의존성 배열: `[]` 는 마운트 1회, 생략하면 매 렌더마다.

구독 effect:

```ts
// FE:widgets/diagnostic-result/useAgentCommandBridge.ts
useEffect(() => {
  const unsub = subscribe(AGENT_COMMAND_EVENT, (cmd) => { … applyAgentCommand(cmd) }, { replayLast: true })
  return () => { unsub(); lastReqId.current = null }     // 구독 해제
}, [patientId])
```

### "로딩 상태를 파생시킨다" — requestKey 패턴

```ts
// FE:widgets/patient-search/usePatientSearch.ts
const requestKey = `${query}|${page}|${size}|${retrySeq}`
const loading = loaded?.key !== requestKey        // state 가 아니라 계산값
const data = loaded?.data ?? EMPTY

useEffect(() => {
  let alive = true
  fetchObservationPatients({ keyword: query, page, size })
    .then((res) => { if (!alive) return; setLoaded({ key: requestKey, data: res, error: null }) })
    .catch((e: Error) => { if (!alive) return; setLoaded({ key: requestKey, data: EMPTY, error: e?.message || '…' }) })
  return () => { alive = false }
}, [requestKey])
```

- 흔한 방식은 effect 시작에서 `setLoading(true)` 를 하는 것인데, effect 안의 동기 setState 는 렌더를 한 번 더 일으키고 이 레포의 lint(`react-hooks/set-state-in-effect`)에 걸린다.
- 대신 "**마지막으로 끝난 요청의 키**" 만 저장하고, 현재 요청 키와 다르면 로딩 중이라고 **계산**한다. 재시도는 `retrySeq` 를 올려 키를 바꾸는 것으로 끝난다. `useDiagnosticResultData` · `useAnalysis` 도 같은 패턴.

### `useLayoutEffect` — 그리기 직전

```ts
// FE:views/diagnostic-result-view/DiagnosticResultView.tsx — EmptyBody
useLayoutEffect(() => {
  useWideStore.getState().resetForPatient()
  useDetailStore.getState().resetForPatient()
  useCompactStore.getState().resetForPatient()
}, [])
```

- `useEffect` 는 화면에 그린 **뒤**, `useLayoutEffect` 는 DOM 반영 후 **페인트 전**에 동기 실행된다. 이전 환자의 필터가 한 프레임이라도 비치면 안 되는 곳에 쓴다.
- 이전 값 기억 + 변화 감지:

```ts
const prevTierRef = useRef(tier)
useLayoutEffect(() => {
  const prev = prevTierRef.current
  prevTierRef.current = tier
  if (prev === tier) return
  // 폭 단계가 바뀌었을 때만: 넓은 뷰의 분류 선택 ↔ 좁은 뷰의 분류 탭을 옮겨 준다
}, [tier])
```

### `useRef` — 렌더와 무관한 값 · 최신 콜백

```ts
// FE:shared/lib/viewBus.ts — "최신 핸들러 ref" 패턴
const handlerRef = useRef(handler)
useEffect(() => { handlerRef.current = handler })      // 의존성 배열 없음 = 매 렌더 후 최신으로
useEffect(() => {
  const unsub = subscribe(VIEW_PARAMS_EVENT, (ev) => { …; handlerRef.current(params, { viewKey }) }, { replayLast: true })
  return () => { unsub(); lastReqId.current = null }
}, [viewKey])                                           // handler 가 바뀌어도 재구독하지 않는다
```

- ref 값을 바꿔도 재렌더되지 않는다. 호출부가 인라인 함수를 넘겨도(매 렌더 새 함수) 구독을 다시 만들지 않으려고 쓴다.
- `useRef<HTMLDivElement>(null)` + `ref={gridWrapRef}` — DOM 요소 참조(`ObservationList`).
- `refreshPidRef` — 비동기 응답이 왔을 때 "그사이 환자가 바뀌었나" 를 확인하는 용도.

### 콜백 ref + 정리 함수 (React 19)

```ts
// FE:shared/lib/useContainerTier.ts
export function useContainerTier<T extends HTMLElement = HTMLDivElement>() {
  const [tier, setTier] = useState<ContainerTier | null>(null)
  const ref = useCallback((el: T | null) => {
    if (!el || typeof ResizeObserver === 'undefined') return undefined
    const ro = new ResizeObserver((entries) => {
      const entry = entries[0]
      if (!entry) return
      const width = entry.borderBoxSize?.[0]?.inlineSize ?? entry.contentRect.width
      setTier(tierOf(width))              // 같은 값이면 React 가 재렌더를 건너뛴다 → 단계가 바뀔 때만 그린다
    })
    ro.observe(el)
    return () => ro.disconnect()          // React 19: 콜백 ref 가 반환한 함수는 요소가 빠질 때 호출된다
  }, [])
  return { ref, tier }
}
// 사용
const { ref, tier } = useContainerTier<HTMLDivElement>()
<div ref={ref}>…</div>
```

- 화면이 **자기가 받은 폭**(창 폭이 아니라)을 재서 L/M/S 뷰를 고른다. host 가 화면을 전체 · 1/2 · 1/4 타일 어디에 두든 맞는 뷰가 나온다.

### `useMemo` · `useCallback` — 계산 · 함수 재사용

```ts
// FE:widgets/diagnostic-result/wide/ObservationList.tsx
const listModes = useMemo(() => listModesOf(hasTimeSeries), [hasTimeSeries])
const flatColumns = useMemo<ColumnDef<Observation>[]>(() => buildFlatColumns({ seriesByCode, windowStart, ctx }), [/* 빌더가 읽는 값 */])

const pickerProps = useCallback(
  (o: Observation) => ({
    open: pickerKey === pickerKeyOf(o),
    onOpenChange: (next: boolean) => setPickerKey(next ? pickerKeyOf(o) : null),
  }),
  [pickerKey],
)
```

- `useMemo(계산, 의존성)` — 의존값이 같으면 이전 결과를 재사용. 표 컬럼 정의처럼 **참조가 바뀌면 자식(DataGrid)이 전부 다시 그리는** 값에 필수.
- `useCallback(함수, 의존성)` = `useMemo(() => 함수, 의존성)`.
- `pickerKey` 하나로 "**한 번에 하나의 팝오버만** 열림" 을 표현하고, 열림 상태를 셀 밖(목록)이 갖는다 — 행이 다시 그려져도 팝오버가 닫히지 않는다.

### 커스텀 훅 — 로직 묶음의 재사용 단위

| 훅 | 파일 | 하는 일 |
|---|---|---|
| `useDiagnosticResultData` | `FE:widgets/diagnostic-result/useDiagnosticResultData.ts` | 환자 1명의 결과 · 설정 · 분석 로딩, 즐겨찾기 조작 함수 묶음, 상세 열기 |
| `useAnalysis` | `FE:entities/analysis/model/useAnalysis.ts` | AI 분석 이력 로딩 · 다시 분석 |
| `usePatientSearch` | `FE:widgets/patient-search/usePatientSearch.ts` | 환자목록 검색 · 페이징 · 재시도 |
| `useContainerTier` | `FE:shared/lib/useContainerTier.ts` | 컨테이너 폭 → L/M/S |
| `useSessionPatient` | `FE:shared/patient-session/sessionPatient.ts` | 현재 환자(이벤트 버스 세션) |
| `useViewParams` · `useOpenView` | `FE:shared/lib/viewBus.ts` | 메뉴 열기 파라미터 송수신 |
| `useAgentCommandBridge` | `FE:widgets/diagnostic-result/useAgentCommandBridge.ts` | host AI 커맨드 구독 → 스토어 |
| `useWideDerive` · `useCompactDerive` | `FE:widgets/diagnostic-result/wide|compact/` | 원본 결과 → 화면용 파생값(필터 · 그룹 · 트리 · 건수) |
| `useGridViewport` | `FE:widgets/diagnostic-result/wide/useGridViewport.ts` | DataGrid 가로 스크롤 위치를 CSS 변수로 |
| `useOpenDetail` | `FE:widgets/diagnostic-result/detailStore.ts` | 열린 상세(닫혀 있으면 null) |

커스텀 훅은 **객체를 반환하는 인터페이스**를 명시해 계약으로 쓴다:

```ts
export interface DiagnosticResultData {
  patientId: string
  loading: boolean
  loadError: string | null
  reload: () => void
  refresh: () => void
  observations: Observation[]
  favoriteSets: FavoriteSetActions
  analyses: Analysis[]
  openDetail: (o: Observation) => void
  persistLayout: (patch: { listMode?: ListMode; sort?: string }) => void
  // …
}
export function useDiagnosticResultData({ patientId, userId, viewParams }: UseDiagnosticResultDataOptions): DiagnosticResultData
```

- 환자 미선택 화면은 이 훅을 부르지 않고 같은 모양의 상수 `EMPTY_RESULTS: DiagnosticResultData`(모든 함수가 noop)를 넘긴다 — **Null Object 패턴**. 그래서 `WideMain` 은 데이터가 있든 없든 같은 코드로 그린다.

## 3-6. 전역 상태 — zustand 스토어

컴포넌트 트리 여러 곳이 같은 상태를 읽고 쓰거나, React 밖(이벤트 콜백)에서 상태를 바꿔야 할 때 쓴다. 이 레포의 스토어 4개:

| 스토어 | 파일 | 담는 것 |
|---|---|---|
| `useWideStore` | `FE:widgets/diagnostic-result/wide/wideStore.ts` | 넓은 폭 필터 · 보기 방식 · 정렬 · 트리 노드 · 비교 그래프 |
| `useCompactStore` | `FE:widgets/diagnostic-result/compact/compactStore.ts` | 좁은 폭 모드 · 분류 · 기준일 |
| `useDetailStore` | `FE:widgets/diagnostic-result/detailStore.ts` | 열린 상세(폭 무관) |
| `useChartModeStore` | `FE:shared/store/chartModeStore.ts` | 추이 표시(line/spike) — 화면 간 공유 |

### 스토어 정의

```ts
// FE:widgets/diagnostic-result/detailStore.ts
import { create } from 'zustand'

export interface DetailState { drawerOpen: boolean; detail: Observation | null; tab: string; pendingDetailCode: string | null }
export interface DetailActions {
  openDrawer: (o: Observation) => void
  closeDrawer: () => void
  requestDetail: (code: string) => void
  resetForPatient: () => void
}

const initialDetail = (): DetailState => ({ drawerOpen: false, detail: null, tab: 'result-detail', pendingDetailCode: null })

export const useDetailStore = create<DetailState & DetailActions>((set) => ({
  ...initialDetail(),
  openDrawer: (o) => set({ detail: o, tab: 'result-detail', drawerOpen: true }),
  closeDrawer: () => set({ drawerOpen: false }),
  requestDetail: (code) => set({ pendingDetailCode: code }),
  resetForPatient: () => set(initialDetail()),
}))
```

- `create<상태 & 액션>((set, get) => ({ …초기값, …액션 }))` — 상태와 그것을 바꾸는 함수를 **한 객체**에 둔다.
- `set({ … })` 은 **얕은 병합**(넘긴 키만 바뀜). `set((st) => ({ … }))` 는 현재 상태 기반.
- `initialXxx()` 를 **함수**로 두는 이유: 매번 새 객체(날짜 기본값 등 포함)를 만들어 리셋하려고.

### 읽기 — 셀렉터 구독 vs `getState()`

```ts
// 컴포넌트 안: 셀렉터로 필요한 조각만 구독 → 그 값이 바뀔 때만 재렌더
const listMode = useWideStore((s) => s.listMode)
const codeFilter = useWideStore((s) => s.codeFilter)

// 이벤트 핸들러 · effect · React 밖: 구독 없이 현재 값 읽기/쓰기
const changeMode = useCallback((m: ListMode) => {
  const vs = useWideStore.getState()
  vs.setListMode(m)
  vs.setSort(DEFAULT_SORT[m])
}, [persistLayout])

// 스토어 밖에서 직접 상태 패치
useDetailStore.setState((st) => (st.detail?.id === o.id ? { detail: { ...st.detail, ...d } } : {}))
```

- 셀렉터 없이 `useWideStore()` 로 통째로 구독하면 아무 필드나 바뀌어도 재렌더되므로 이 코드는 항상 셀렉터를 쓴다.
- `getState()` 는 훅이 아니라 일반 함수라 **어디서든** 부를 수 있다 — 이벤트 버스 콜백(`applyAgentCommand`)이 스토어를 바꾸는 근거.

### 함수형 setter 를 받는 액션 (React `setState` 흉내)

```ts
setCompareOpen: (v) => set({ compareOpen: typeof v === 'function' ? v(get().compareOpen) : v }),
```

- 타입: `setCompareOpen: (v: boolean | ((prev: boolean) => boolean)) => void`.

### 커맨드 → 상태 매핑 어댑터

```ts
// FE:widgets/diagnostic-result/wide/wideStore.ts (발췌)
applyAgentCommand: (cmd) => {
  if (!cmd || typeof cmd !== 'object') return
  const p = cmd.params ?? {}
  const patch: Partial<WideState> = {}
  if (cmd.intent === 'RESET') { set(initialWide()); return }
  if (p.dateFrom) patch.dateFrom = p.dateFrom
  if (!p.dateFrom && !p.dateTo && Number.isFinite(p.periodDays)) {
    patch.dateFrom = daysAgoLocal(p.periodDays as number)
    patch.dateTo = todayLocal()
  }
  if (p.status && CHIP_KEYS.has(p.status)) patch.activeChip = p.status as ChipKey
  if (cmd.intent === 'COMPARE') { patch.listMode = 'agg'; if (p.codes?.length) { patch.compareCodes = p.codes; patch.compareOpen = true } }
  set(patch)                                   // 모아서 한 번에
}
```

- 외부 입력(AI 커맨드)을 **화이트리스트 검사**(`CHIP_KEYS.has`)하고 `Partial` 패치에 모아 한 번에 반영한다.

### sessionStorage 를 안전하게

```ts
// FE:shared/store/chartModeStore.ts
const readChartMode = (): ChartMode => {
  try {
    return sessionStorage.getItem(CHART_MODE_KEY) === 'spike' ? 'spike' : 'line'
  } catch {
    return 'line'           // 시크릿 모드 등 저장소가 막힌 환경 — 기본값
  }
}
```

- `catch { }` — 오류 변수를 쓰지 않으면 생략 가능(ES2019).

## 3-7. 데이터 계층 — API 호출 · 표기 변환 · mock

### 3단 구조

```
화면/훅 (useDiagnosticResultData)
  └ entity API 함수 (fetchObservations · assignFavoriteSets …)     ← snake ↔ camel 명시 매핑 · mock 분기
      └ shared/api/apiBase.ts (get · post · put · patch · del)     ← PREFIX · 쿼리 정리 · ApiError 변환
          └ @x-ehr/api-axios 의 api (host 공용 axios: 토큰 · 401 refresh · CSRF)
```

### apiBase — 공용 HTTP 위임

```ts
// FE:shared/api/apiBase.ts
import { api } from '@x-ehr/api-axios'
const PREFIX = '/api/v1/diagnostic-result'

function cleanParams(params?: QueryParams): Record<string, string | number | boolean> | undefined {
  if (!params) return undefined
  const out: Record<string, string | number | boolean> = {}
  for (const [k, v] of Object.entries(params)) {
    if (v !== undefined && v !== null && v !== '') out[k] = v      // 빈 값은 보내지 않는다
  }
  return out
}

function toApiError(e: unknown, path: string): ApiError {
  const err = e as { response?: { status?: number; data?: { message?: string } }; message?: string }
  const status = err?.response?.status ?? 0
  const message = err?.response?.data?.message || (status ? `백엔드 요청 실패 (HTTP ${status}) — ${path}` : `백엔드에 연결할 수 없습니다 (…)`)
  return new ApiError(message, status)
}

export async function put<T>(path: string, body: unknown): Promise<T> {
  try {
    const res = await api.put<T>(PREFIX + path, body)
    return res.data
  } catch (e) {
    throw toApiError(e, path)
  }
}
```

- remote 는 자기 axios 인스턴스를 만들지 않는다(토큰 · refresh 가 갈라지므로) — lint 로 강제.
- 백엔드 `error_response.message` 를 그대로 화면 문구로 쓴다 → 도메인 예외 메시지("이미 있는 세트 이름입니다: 신장")가 사용자에게 보인다.
- `Object.entries(obj)` + `for…of` + 구조분해 — 객체 순회 관용구.

### entity API — 명시 매핑

```ts
// FE:entities/viewer-configuration/api/viewerConfigurationApi.ts
const toFavoriteSet = (raw: unknown): FavoriteSet => {
  const s = asRecord(raw)
  return {
    id: s.id as string,
    setName: (s.set_name as string) ?? '',
    sortOrder: (s.sort_order as number) ?? 0,
    items: ((s.items as unknown[]) ?? []).map(toFavorite),
  }
}

export async function assignFavoriteSets(userId: string, observation: Favorite, favoriteSetIds: string[]): Promise<void> {
  if (isMock()) return (await mock()).assignFavoriteSets(observation, favoriteSetIds)
  await put(`${configPath(userId)}/favorite-assignments`, {
    observation_type_code: observation.observationTypeCode,     // camel → snake
    observation_code: observation.observationCode,
    favorite_set_ids: favoriteSetIds,
  })
}
const configPath = (userId: string) => `/viewer-configurations/${encodeURIComponent(userId)}`
```

- 자동 camelCase 변환 라이브러리를 쓰지 않는 이유(코드 주석): 값 자체에 밑줄이 있는 데이터(`diagnostic_observation_result_reviewer`)까지 망가진다. 매핑 함수가 곧 **백엔드 view_model 과의 계약 문서**다.
- 템플릿 리터럴 + `encodeURIComponent` — 경로 파라미터 안전하게(이메일의 `@` 등).

### mock 모드 — 동적 import 로 운영 번들에서 분리

```ts
// FE:shared/api/mockStore.ts
let loader: (() => Promise<unknown>) | null = null
export function registerMockStore(load: () => Promise<unknown>): void { loader = load }
export async function mockStore<T>(): Promise<T> {
  if (!loader) throw new ApiError('mock 모드인데 mock 데이터가 등록되지 않았습니다 …')
  return (await loader()) as T
}

// FE:app/Standalone.tsx — 단독 실행에서만 등록
registerMockStore(() => import('@/mocks/patient-results/store'))
```

- **의존성 역전**: `shared` 는 `mocks` 를 import 하지 않는다(그러면 mock 데이터가 운영 번들에 들어간다). 대신 "로더 함수를 등록받는 자리" 만 두고, dev 전용 파일이 등록한다.
- 각 API 파일은 자기가 쓰는 mock 의 모양을 `interface ObservationMockStore { … }` 로 선언하고 `mockStore<ObservationMockStore>()` 로 꺼낸다.

### 낙관적 업데이트 + 서버 재조회

```ts
// FE:widgets/diagnostic-result/useDiagnosticResultData.ts
const mutateSets = useCallback(
  async (job: () => Promise<void>, optimistic?: (sets: FavoriteSet[]) => FavoriteSet[]): Promise<string | null> => {
    if (!userId) return NO_USER_MESSAGE
    if (optimistic) setConfig((prev) => ({ ...prev, favoriteSets: optimistic(prev.favoriteSets) }))   // 1) 화면 먼저
    try {
      await job()             // 2) 서버 저장
      await reloadSets()      // 3) 서버 값으로 맞춤(새 id · 순서)
      return null             //    성공 = null
    } catch (e) {
      await reloadSets()      //    실패하면 서버 값으로 되돌림
      return (e as Error)?.message || '즐겨찾기 세트를 저장하지 못했습니다.'   // 실패 = 사용자 문구
    }
  },
  [reloadSets, userId],
)
```

- 반환 규약 "성공 null / 실패 문구" 로 호출부(`FavoriteSetPicker`)가 오류를 그 자리에 보여 준다.
- 이 레포는 `@tanstack/react-query` 를 의존성에 갖지만(host 와 단독 실행 Provider 용) 화면 데이터는 위처럼 **커스텀 훅 + useEffect** 로 직접 관리한다.

## 3-8. 이벤트 버스 — remote 간 통신

remote 끼리, 또 host ↔ remote 는 서로를 import 하지 않고 `@x-ehr/event-bus` 로만 말한다(Pub/Sub).

### 계약 선언 (3-3 (10) 의 module augmentation)

`FE:shared/lib/events.ts` 에 이벤트 이름 상수 + payload 타입 + `EhrEventMap` 등록을 한 곳에 둔다:

| 이벤트 | payload | 용도 |
|---|---|---|
| `ehr:patient:selected` | `PatientSelectedEvent` | remote 안에서 고른 환자 |
| `ehr:app:view-params` | `ViewParamsEvent { viewKey, requestId, params }` | "메뉴 열면서 파라미터 전달" |
| `ehr:diagnostic:agent-command` | `AgentCommand { intent, params, … }` | host AI 가 해석한 필터 명령 |
| (ancillary 세션 이벤트) | `{ session: AncillaryPatientSession \| null }` | 현재 환자 세션 |

### `publish` / `subscribe` / `replayLast`

```ts
// 발행 — replayLast: 마지막 값을 보관해 나중에 구독한 쪽도 즉시 받게
publish(VIEW_PARAMS_EVENT, { viewKey: menuId, params, requestId: nextRequestId() }, { replayLast: true })

// 구독 — replayLast: 보관된 마지막 값을 구독 즉시 1회 받는다. 반환값은 해제 함수
const unsub = subscribe(VIEW_PARAMS_EVENT, (ev: ViewParamsEvent) => { … }, { replayLast: true })
```

- **replayLast** 가 핵심이다: 화면이 아직 열리지 않았을 때 발행된 파라미터도, 화면이 마운트되며 구독하는 순간 받는다 → "열려 있으면 바로 전달, 닫혀 있으면 열면서 전달" 이 같은 코드 경로.
- 중복 방지: `requestId` 를 ref 에 기억해 같은 요청을 두 번 적용하지 않고, cleanup 에서 리셋해 재마운트 시에는 다시 적용한다.

### 환자 세션 — 화면은 `SessionPatient` 만 안다

```ts
// FE:shared/patient-session/sessionPatient.ts
export interface SessionPatient { patientId: string; patientName: string; sex: string; age: number; diagnosis?: string | undefined }

function toSessionPatient(session: AncillaryPatientSession | null): SessionPatient | null {
  if (!session) return null
  return { patientId: session.patient_id, patientName: session.patient_name, sex: session.patient_sex, age: session.patient_age,
           diagnosis: session.encounter_diagnosis_name ?? undefined }
}

export function useSessionPatient(): SessionPatient | null {
  const session = useAncillaryPatientSession()
  return useMemo(() => toSessionPatient(session), [session])
}
```

- 발행처(다른 BC 의 remote)의 전문 형식은 이 폴더 밖으로 새지 않는다 — **어댑터 경계**. 발행처가 바뀌면 `toSessionPatient` 만 고친다.
- `useAncillaryPatientSession` 은 `useState(readRetainedSession)`(첫 렌더부터 보관값 동기 읽기) + `useSubscribe(…)`(이후 변경) 조합이다. 첫 렌더에 null 이 한 번 비치는 깜빡임을 막는다.

### openView / useViewParams (메뉴 열기 + 파라미터)

```ts
// FE:shared/lib/viewBus.ts
export function useOpenView(): (menuId: string, params?: ViewParams) => void {
  const { navigate } = useAppLink()                  // host 가 주입한 네비게이션
  return useCallback((menuId: string, params: ViewParams = {}) => {
    publish(VIEW_PARAMS_EVENT, { viewKey: menuId, params, requestId: nextRequestId() }, { replayLast: true })
    navigate(menuId)                                 // 열기(이미 열려 있으면 포커스)
  }, [navigate])
}
```

```ts
// FE:views/diagnostic-result-view/DiagnosticResultView.tsx — 받는 쪽
useViewParams(VIEW_KEY, (params) => {
  setReceived({ params, forPatientId: params.patientId ?? patientId })
})
const viewParams = received && received.forPatientId === patientId ? received.params : null
```

- 받은 파라미터를 "어느 환자 대상인지" 와 함께 저장하고, **지금 세션 환자 대상일 때만** 본문에 넘긴다.

## 3-9. 라우팅 — routes.tsx 와 Standalone

```tsx
// FE:routes.tsx
import { ToastProvider } from '@x-ehr/design-system'
import { Outlet, useOutletContext, type RouteObject } from 'react-router'

function RemoteScope() {
  const ctx = useOutletContext()               // 부모(host)가 준 컨텍스트(auth 등)
  return (
    <div className="x-remote-root khds-root">   {/* CSS 스코프 + 디자인킷 base 리셋 경계 */}
      <ToastProvider>
        <Outlet context={ctx} />                 {/* 자식 라우트가 그려질 자리, 컨텍스트를 그대로 전달 */}
      </ToastProvider>
    </div>
  )
}

const routes: RouteObject[] = [
  {
    element: <RemoteScope />,                    // path 없는 레이아웃 라우트
    children: [
      { path: 'patient-list', element: <PatientList /> },
      { path: 'diagnostic-result-view', element: <DiagnosticResultView /> },
      { path: 'summary', element: <ReportSummary /> },
    ],
  },
]
export default routes
```

- **객체 라우트 설정**(`RouteObject[]`): 상대 경로 child 만 정의하고, host 가 `/diagnostic-result` 아래에 끼운다(절대 경로 금지).
- `Outlet` = 중첩 라우트 자리, `useOutletContext` = 부모 → 자식 데이터 전달.
- **Provider 패턴**: `ToastProvider` 로 감싸면 그 아래 어디서든 `useToast()` 로 토스트를 띄운다(React Context).

`FE:app/Standalone.tsx` 는 host 없이 개발할 때 host 역할을 흉내 낸다: `createBrowserRouter` 로 라우터 생성, `createMockAuthContext()` 로 가짜 로그인, `NavigationContext` 로 가짜 네비게이션, `QueryClientProvider`, dev 메뉴 드롭다운, mock store 등록. 운영 빌드에는 포함되지 않는다(lint 계약: remote 는 라우터 · QueryClient 를 직접 만들지 않는다 — Standalone 만 예외).

## 3-10. 디자인 시스템 컴포넌트 사용법

- 모든 컨트롤은 `@x-ehr/design-system` 컴포넌트만 쓴다: `Button` · `Checkbox` · `TextField` · `Popover`/`PopoverTrigger`/`PopoverContent` · `Tooltip` · `Badge` · `Banner` · `Spinner` · `Divider` · `DataGrid` · `SegmentedControl` · `Sparkline` · `Chart` · `Dropdown` · `ToastProvider` · `MessageBoxProvider`.
- 아이콘은 `@x-ehr/design-system/icons`(`Star` · `Plus` · `Calendar` · `Clock` …).
- 레이아웃은 `Stack`(flex 래퍼)/`Grid`/`Stack.Item grow` — `flex`/`grid` 유틸 직접 사용 금지.
- 스타일 값은 **토큰 문법**: `p-(--spacing-5)` · `text-(--color-foreground-tertiary)` · `typo-caption-sm-regular`. Tailwind 기본 스케일(`p-4`)은 킷 테마가 리셋해 조용히 무시된다.

### DataGrid — 선언적 표

```tsx
// FE:widgets/diagnostic-result/wide/columns/flatColumns.tsx
export function buildFlatColumns({ seriesByCode, windowStart, ctx }: {…}): ColumnDef<Observation>[] {
  return [
    {
      id: 'performedAt',
      header: '시행일',
      width: 104,
      cell: ({ row }) =>
        isTimeSeriesType(row) ? (
          <Stack><span>{performedDate(row)}</span><span className="typo-caption-sm-regular …">{timeOf(row)}</span></Stack>
        ) : (
          performedDate(row)
        ),
    },
    { id: 'observationCode', header: '코드', width: 60, cell: ({ row }) => <TruncatedText text={row.observationCode} /> },
    // …
  ]
}

// FE:widgets/diagnostic-result/wide/ObservationList.tsx
<DataGrid<Observation>
  rows={rows as Observation[]}
  columns={flatColumns}                       // useMemo 로 고정
  getRowId={(o) => o.id}
  rowClassName={(o) => withNoTrend(o.id, rowClassName(o))}
  rowHeight={44}
  rowHeightMode="measured"
  onRowClick={(meta) => { if (meta.colId === 'trend') toggleTrendIfAny(meta.row.id); else onOpen(meta.row) }}
  renderDetail={renderFlatTrendDetail}
  expandedRowIds={expandedIds}
  onExpandedChange={onTrendExpandedChange}
  empty={empty}
  aria-label="상세조회 결과"
/>
```

- 컬럼 정의는 **데이터(배열)** 이고, 각 칸의 그리기는 `cell` 함수(렌더 함수)다. 보기 모드마다 빌더 함수를 `columns/*` 파일로 분리했다.
- 페이징하지 않고 전체를 넘기면 킷의 **가상화**가 보이는 행만 그린다.

## 3-11. 화면 컴포넌트 트리 (호출관계)

```
routes.tsx
└ RemoteScope (.x-remote-root .khds-root + ToastProvider + Outlet)
  └ DiagnosticResultView                               views/diagnostic-result-view/
     ├ useSessionPatient()        → 현재 환자
     ├ useCurrentUser()           → userId(email)
     ├ useContainerTier()         → tier L/M/S (ref 로 폭 측정)
     ├ useViewParams(VIEW_KEY)    → openView 파라미터
     ├ [환자 없음] EmptyBody → 스토어 리셋 → WideMain(EMPTY_RESULTS) 또는 CompactMain
     └ [환자 있음] ResultsBody  key={patientId}
        ├ useDiagnosticResultData({patientId, userId, viewParams})
        │   ├ useAnalysis(patientId)                  → GET observation-analyze-results
        │   ├ useAgentCommandBridge(patientId)        → subscribe(agent-command) → wideStore/detailStore
        │   ├ effect: Promise.all[fetchViewerConfig, fetchObservations] → setObservations/setConfig + wideStore 레이아웃
        │   ├ effect: viewParams → applyAgentCommand → wideStore
        │   ├ openDetail(o) → detailStore.openDrawer + fetchObservation(id) 보강
        │   └ favoriteSets(useMemo) = { toggleSet, createSet, renameSet, deleteSet, moveSet } → mutateSets
        ├ [L] WideMain(results)
        │   ├ useWideDerive(observations, sets)       → groups · tree · counts · listRows …
        │   ├ AiSection                               (AI 브리핑 · 다시 분석)
        │   ├ ResultChipBar                           (상태 필터 칩 · 「조회」=refresh)
        │   ├ CodeFilterBar                           (openView 의 검사코드 필터)
        │   ├ nav > CategoryTree                      (분류 트리 · 즐겨찾기 세트 노드)
        │   ├ ObservationList                         (보기 4모드 → DataGrid)
        │   │   ├ columns/{flat,date,trend,time}Columns
        │   │   └ cells/{FavoriteButton → FavoriteSetPicker, TrendCell, TruncatedText, …}
        │   └ DetailPanel                             (detailStore 를 읽어 오른쪽 상세)
        ├ [M·S] CompactMain(observations, size)
        │   └ ReportPanel · RawPanel · TrendCards · NarrativeList · DetailToolbar (compactStore)
        └ [M·S] DetailDialog                          (detailStore 를 읽어 Dialog 상세)
```

읽는 법:
- **데이터는 위에서 한 번**(`useDiagnosticResultData`) 불러 props 로 내려준다.
- **뷰 상태는 스토어**가 갖고, 필요한 컴포넌트가 직접 구독한다(props drilling 회피).
- **파생값은 `useWideDerive`** 가 useMemo 체인으로 계산한다(원본 → 기간 필터 → 그룹 → 트리 → 검색 → 칩 필터 → 정렬).
- 폭이 바뀌면 L ↔ M/S 뷰만 갈아 끼우고, 데이터 훅과 상세 상태는 `ResultsBody` 에 남아 있어 **다시 조회하지 않는다**.

## 3-12. 프론트엔드 디자인 패턴 요약표

| 패턴 | 위치 | 한 줄 설명 |
|---|---|---|
| Micro Frontend (remote) | `rspack.config.js` · `routes.tsx` | host 가 런타임에 끼운다 |
| Feature-Sliced Design | 폴더 구조 + eslint | 레이어 하향 의존 |
| Container / Presentational | `ResultsBody`(데이터) → `WideMain` · 셀(표시) | 데이터 로딩과 그리기 분리 |
| Custom Hook | `use*` | 로직 재사용 단위 |
| Null Object | `EMPTY_RESULTS` | 데이터 없음도 같은 모양 |
| Key remount | `key={patientId}` | 상태 일괄 초기화 |
| Derived state | `requestKey` · `loading = …` | 저장 대신 계산 |
| Latest ref | `handlerRef.current = handler` | 재구독 없이 최신 콜백 |
| Stale response guard | `alive` · `pidRef` | 늦은 응답 무시 |
| Global store (Flux 계열) | zustand 4개 | 셀렉터 구독 · getState |
| Optimistic update | `mutateSets` | 화면 먼저, 서버로 정정 |
| Adapter / Mapper | `toObservation` · `toSessionPatient` | 외부 모양 → 화면 타입 |
| Pub/Sub | `@x-ehr/event-bus` | remote 간 무결합 통신 |
| Dependency Inversion | `registerMockStore` | shared 가 mocks 를 모름 |
| Provider | `ToastProvider` · `RemoteScope` | 하위 트리에 컨텍스트 공급 |
| Render prop | `Tooltip` children 함수 | 렌더 위임 |
| Controlled component | `TextField value/onChange` · Popover `open/onOpenChange` | 상태를 부모가 소유 |

---

# 4부. 백엔드 ↔ 프론트엔드 연결

## 4-1. API 계약표

경로 접두 `/api/v1/diagnostic-result` 생략. BE 포트 5351, FE 단독 dev 서버 3351(프록시로 BE 에 전달).

| HTTP | 경로 | BE Controller 메서드 | BE 경로(Command/Query) | FE 함수 |
|---|---|---|---|---|
| GET | `/observations?patient_id&observation_type_code&category_code&from_date&to_date` | `observations_controller.search_observations_by_category` | `i_observation_queries.search_observations_by_category` | `fetchObservations` |
| GET | `/observations/patients?keyword&page&size` | `search_observation_patients` | `search_observation_patients` → `page_view` | `fetchObservationPatients` |
| GET | `/observations/{id}` | `find_observation` | `find_observation` | `fetchObservation` |
| GET | `/viewer-configurations/{user_id}` | `viewer_configurations_controller.find_viewer_configuration` | `i_viewer_configuration_queries` (없으면 404) | `fetchViewerConfig` (404 → 기본값) |
| PUT | `/viewer-configurations/{user_id}` | `save` | `register_viewer_configuration_command` → 204 | `saveViewerConfig` |
| POST | `/viewer-configurations/{user_id}/favorite-sets` | `favorite_sets_controller.create_favorite_set` | `create_favorite_set_command` → 201 | `createFavoriteSet` |
| PATCH | `/viewer-configurations/{user_id}/favorite-sets/{id}` | `rename_favorite_set` | `rename_favorite_set_command` → 204 | `renameFavoriteSet` |
| DELETE | `/viewer-configurations/{user_id}/favorite-sets/{id}` | `delete_favorite_set` | `delete_favorite_set_command` → 204 | `deleteFavoriteSet` |
| PUT | `/viewer-configurations/{user_id}/favorite-sets/order` | `reorder_favorite_sets` | `reorder_favorite_sets_command` → 204 | `reorderFavoriteSets` |
| PUT | `/viewer-configurations/{user_id}/favorite-assignments` | `assign_favorite_sets` | `assign_favorite_sets_command` → 204 | `assignFavoriteSets` |
| GET | `/observation-analyze-results?patient_id&skip&take` | `observation_analyze_results_controller.search_by_patient` | `i_observation_analyze_result_queries` | `fetchAnalysesForPatient` |
| GET | `/observation-analyze-results/{id}` | `find_one` | 〃 | — |
| POST | `/observation-analyze-results` | `run_analysis` | `analyze_observations_by_patient_command` → 201 (설정 없으면 503) | `runAnalysis` (POST 후 목록 재조회) |

이름 규칙 경계:
- BE 응답 JSON 키 = view_model 프로퍼티 이름 = **snake_case**(`result_datetime`, `is_abnormal`).
- FE 타입 = **camelCase**(`resultDatetime`, `isAbnormal`). 변환은 `entities/*/api/*.ts` 의 `toXxx` 함수 한 곳.
- Kafka payload = **camelCase**(`resultDatetime`) — BE 내부에서 매퍼가 snake 도메인 이름으로 바꾼다.

## 4-2. 끝에서 끝까지 — 시나리오 3개 전체 호출 체인

### 시나리오 1. 별표를 눌러 검사를 "신장" 세트에 넣는다

```
[브라우저 — React]
1  ObservationList 의 행 셀: FavoriteButton(row, favoriteSets, open, onOpenChange)
2    별표 Button 클릭 → Popover 열림(pickerKey = 이 행)
3  FavoriteSetPicker > PickerBody: "신장" Checkbox onCheckedChange(true)
4    toggle(set, true) → run(() => actions.toggleSet(observation, set, true))      busy=true
5  useDiagnosticResultData 의 favoriteSets.toggleSet
6    ids = setIdsOf(o, setsRef.current); next = [...new Set([...ids, set.id])]
7    mutateSets(job = () => assignFavoriteSets(uid, keyOf(o), next),
                optimistic = (sets) => withAssignment(sets, o, set.id, true))
8      setConfig(optimistic)  → 재렌더: 체크 표시 · 별표 색이 즉시 바뀜
9      await assignFavoriteSets(...)                     entities/viewer-configuration/api
10       put('/viewer-configurations/{uid}/favorite-assignments',
             { observation_type_code, observation_code, favorite_set_ids })      shared/api/apiBase
11       api.put(PREFIX + path, body)                    @x-ehr/api-axios (토큰 첨부)

[네트워크]  PUT /api/v1/diagnostic-result/viewer-configurations/{uid}/favorite-assignments

[서버 — Kotlin]
12 favorite_sets_controller.assign_favorite_sets(user_id, request)
13   request.to_command(user_id)   (List → Set 변환)
14   command_bus.send(assign_favorite_sets_command)
15     assign_favorite_sets_command_validator.validate  (user_id · 키 형식)
16     assign_favorite_sets_command_handler.handle      @Transactional 시작
17       repository.find_viewer_configuration(user_id) ?: viewer_configuration(user_id)
18       config.assign_observation_to_sets(type, code, ids)
19         ids 전부 find_favorite_set (없으면 not_found → 404)
20         각 세트: id in ids ? add_item : remove_item     (Set 이라 중복 없음)
21       repository.create_or_update_viewer_configuration(config) → jpa.save → dispatcher
22     커밋: favorite_set_item INSERT/DELETE, viewer_configuration.version 증가
23   204 No Content

[브라우저]
24 await reloadSets() → fetchViewerConfig(uid) → GET /viewer-configurations/{uid}
     서버: viewer_configuration_queries → query_dsl 3단 조회 → viewer_configuration_view
     toViewerConfig(snake → camel)
25 setConfig(서버 값) → 재렌더 → isFav(row, sets) → 별표 색 확정
26 run() 이 null 수신 → busy=false, error=null
(실패했다면: 24 에서 서버 값으로 되돌리고 서버 메시지를 Picker 하단 role="alert" 로 표시)
```

### 시나리오 2. 다른 BC 가 진검 결과를 발행 → 화면에 보인다

```
[다른 BC]  Kafka `dev.chabd.specimen-diagnostics.laboratory-result` 에 봉투 발행
           payload: { eventType:"created", sourceAggregateId, sourceEventVersion:3, observationTypeCode:"SPECIMEN_DIAGNOSTICS",
                      patientId, resultNumericValue: 8.2, referenceHigh: 6.0, laboratoryResultId, … }

[서버]
1 common-eventbus 컨슈머 → topic_registry 라우트 → @MessageType("LaboratoryResult") → laboratory_result_integration_event
2 subscription 람다 → observation_result_handler.handle(envelope)   (프록시 → @Transactional)
3 integration_event_handler.handle → handle_internal(event)
4 is_deleted? eventType="created" → false → upsert
5 event.to_observation_payload()
    required(...) 필수값 검증, allowed_observation_type_codes 검사, category_code 기본값 "laboratory"
6 repository.find_observation_by_source(sourceAggregateId) → 없음
    → new_observation("SPECIMEN_DIAGNOSTICS", UUID) → specimen_diagnostics_observation(id)
7 apply_source_event(id, 3, payload)
    버전 비교 통과 → 공통 필드 → apply_detail → recalculate_flag(8.2 > 6.0 → "H") → is_abnormal = true
8 create_or_update_observation → jpa.save → INSERT observation + INSERT specimen_diagnostics_observation (JOINED)
9 커밋. (같은 메시지가 다시 와도 7 에서 버전 3 <= 3 이라 false → 저장 안 함)

[브라우저 — 사용자가 「조회」 클릭]
10 ResultChipBar onRefresh → results.refresh()
11 fetchObservations({ patientId }) → GET /observations?patient_id=…
12 서버: observation_queries → observation_query_dsl (부모 + 자식 5종 LEFT JOIN, is_deleted=false, 결과일 역순)
       → read_row → to_category_view (result_summary = spec_result_value …, result_flag = "H", is_abnormal = true)
13 toObservation → setObservations
14 useWideDerive 재계산 → counts.abnormal +1 → ObservationList 행 배경 bg-(--color-surface-subtle) · FlagBadge "H"(red)
```

### 시나리오 3. 「다시 분석」 버튼

```
[브라우저]
1 AiSection onAnalyze → results.reanalyze() → useAnalysis.reanalyze()
2 setAnalyzing(true) → runAnalysis(patientId) → post('/observation-analyze-results', { patient_id })

[서버]
3 observation_analyze_results_controller.run_analysis → command_bus.send(analyze_observations_by_patient_command)
4 handler.handle (트랜잭션 없음)
5   agent_port.agent()  (설정 없으면 analysis_agent_unavailable_exception → 503)
6   agent.analyze("PT… 20261002기준") → RestClient POST AI Gateway (재시도) → JSON 트리 파싱 → normalizer
7   observation_analyze_result(…) 생성
8   result_saver.save(result)  @Transactional — 짧은 트랜잭션으로 INSERT
9 201 Created + Location

[브라우저]
10 runAnalysis 가 이어서 fetchAnalysesForPatient → GET ?patient_id&skip=0&take=20
11 서버: observation_analyze_result_queries → query_dsl → normalizer.derive(detail JSON) 로 severity 등 파생 → view
12 toAnalysis → setLoaded → analyses(최신 1건) · history → AiSection 재렌더
13 pidRef 로 "응답 사이 환자가 바뀌었나" 확인 — 바뀌었으면 결과 버림
```

## 4-3. 양쪽이 같은 값을 갖는 자리

| 값 | 백엔드 | 프론트엔드 |
|---|---|---|
| 세트 상한 20 | `viewer_configuration.MAX_FAVORITE_SETS` | `FAVORITE_SET_LIMIT` (`entities/viewer-configuration/model/favoriteSets.ts`) |
| 세트 이름 최대 30자 | `favorite_set.MAX_SET_NAME_LENGTH` | `FAVORITE_SET_NAME_MAX` |
| 조회 양식 코드 | `viewer_configuration.ALLOWED_LAYOUT_CODES` | `LAYOUT_PRESET` 키 (`useDiagnosticResultData.ts`) |
| 비정상 판정 | `observation.compute_abnormal` → `is_abnormal` 컬럼 | `isAbnormal = (o) => o.isAbnormal` (다시 판정하지 않음) |
| 분류 코드 | `*_observation.OBSERVATION_TYPE_CODE` | `TYPE_ORDER` (`entities/observation/model/constants.ts`) |
| API 접두 | 컨트롤러 `@RequestMapping("/api/v1/diagnostic-result/…")` | `PREFIX` (`shared/api/apiBase.ts`) |

원칙: **규칙의 판정은 서버 한 곳**에서 하고, 화면은 UX 용(버튼 잠금 등)으로만 같은 상수를 갖는다.

---

# 5부. 학습 경로 · 연습 과제 · NotebookLM 질문 예시

## 5-1. 학습 경로 (권장 순서)

1. **Kotlin 문법 워밍업**: `viewer_configuration.kt` 한 파일을 처음부터 끝까지 읽는다. 생성자 · 프로퍼티 · `protected set` · backing property · 컬렉션 연산 · 예외 · companion object 가 다 나온다.
2. **테스트로 도메인 이해**: `favorite_set_rules_test.kt` 를 읽고 각 테스트가 어떤 규칙을 지키는지 말로 설명해 본다.
3. **쓰기 경로 추적**: `favorite_sets_controller` → `command_bus` → `assign_favorite_sets_command_handler` → `viewer_configuration.assign_observation_to_sets` 를 IDE 의 "정의로 이동"으로 따라간다.
4. **읽기 경로 추적**: `observations_controller` → `observation_queries` → `observation_query_dsl`. Q클래스가 어디서 생기는지(`build/generated/source/kapt`) 확인한다.
5. **이벤트 경로 추적**: `topic_registry` · `event_bus_subscription_initializer` → `observation_result_handler` → `observation.apply_source_event`. 템플릿 메서드 두 개(핸들러 · Aggregate)를 찾는다.
6. **TypeScript 워밍업**: `entities/observation/model/types.ts` · `constants.ts` · `flags.ts` (순수 타입 · 함수, React 없음).
7. **React 기본**: `FavoriteButton.tsx` → `FavoriteSetPicker.tsx` (props · state · 이벤트 · 조건부 렌더 · 리스트).
8. **훅 패턴**: `usePatientSearch.ts`(가장 작은 데이터 훅) → `useAnalysis.ts` → `useDiagnosticResultData.ts`(가장 큰 훅).
9. **상태 관리**: `detailStore.ts`(작음) → `wideStore.ts`(큼). 어디서 `getState()` 를 쓰고 어디서 셀렉터를 쓰는지 구분한다.
10. **화면 조립**: `DiagnosticResultView.tsx` → `WideMain.tsx` → `ObservationList.tsx`.
11. **통합**: 4-2 의 시나리오 1 을 실제 코드로 한 단계씩 확인한다.

## 5-2. 연습 과제 (난이도 순)

1. (Kotlin) `viewer_configuration` 에 "세트 비우기(`clear_favorite_set`)" 도메인 메서드를 추가하고 테스트를 작성한다. 어떤 예외를 던져야 하는지, 왜 `favorite_set` 의 메서드는 `internal` 이어야 하는지 설명한다.
2. (Kotlin) 위 메서드를 HTTP 로 노출한다: command data class · validator · handler · controller 메서드(`DELETE …/favorite-sets/{id}/items`). `command_bus` 에 등록 코드를 따로 쓸 필요가 없는 이유를 설명한다.
3. (Kotlin) `observation_query_dsl` 에 "특정 검사코드의 결과만" 필터를 추가한다(`BooleanBuilder` + `?.let`).
4. (TS) `entities/viewer-configuration/api` 에 위 API 호출 함수를 추가한다(mock 분기 포함).
5. (React) `FavoriteSetManageDialog` 에 "비우기" 버튼을 붙이고 `mutateSets` 경유로 연결한다. 낙관적 업데이트 함수도 작성한다.
6. (React) `useContainerTier` 의 경계값을 바꾸면 어떤 컴포넌트가 다시 그려지는지 트리(3-11)로 설명한다.
7. (사고 실험) 같은 Kafka 메시지가 버전 2 → 3 → 2 순서로 오면 DB 에 무엇이 남는가? 삭제(버전 4)가 적재(버전 3)보다 먼저 오면?

## 5-3. NotebookLM 에 물어볼 질문 예시

- "command_bus 가 핸들러를 찾는 과정을 단계별로 설명하고, isBridge 검사가 왜 필요한지 알려줘."
- "observation 의 apply_source_event 가 멱등한 이유를 버전 비교 관점에서 설명해줘."
- "@Transactional 을 handle_internal 에 붙이면 왜 동작하지 않는지 self-invocation 으로 설명해줘."
- "Kotlin 의 protected set 과 backing property 패턴이 이 코드에서 어떤 캡슐화를 하는지 예시와 함께."
- "QueryDSL Projections.constructor 와 JPA Entity 조회의 차이, 이 레포가 조회에서 Entity 를 쓰지 않는 이유."
- "useDiagnosticResultData 의 loading 이 state 가 아니라 계산값인 이유와 requestKey 패턴."
- "ResultsBody 에 key={patientId} 를 준 효과를 useState 초기화 관점에서 설명해줘."
- "zustand 의 셀렉터 구독과 getState() 를 각각 언제 쓰는지 이 코드의 예로."
- "별표 클릭부터 DB 저장, 화면 갱신까지 호출되는 함수를 순서대로 나열해줘."
- "replayLast 옵션이 없으면 openView 파라미터 전달에서 어떤 문제가 생기는지."
- "Kotlin 의 out T 와 TypeScript 의 제네릭 상한(extends)을 이 코드 예로 비교해줘."

---

# 부록. 용어집

| 용어 | 뜻 |
|---|---|
| BC (Bounded Context) | 하나의 업무 모델이 일관되게 쓰이는 경계. 이 레포 = 검사결과 BC |
| MS (MicroService) | 배포 단위. 이 BC 는 MS 1개(`diagnostic-result`) |
| Aggregate / Aggregate Root | 함께 일관성을 지켜야 하는 객체 묶음 / 그 입구 객체. 바깥은 Root 만 만진다 |
| Entity | id 로 구별되는 객체(`favorite_set`) |
| Value Object (VO) | 값으로 구별되는 불변 객체(`favorite_observation`) |
| Repository | Aggregate 를 저장·조회하는 컬렉션 같은 인터페이스 |
| CQRS | 쓰기 모델(Command)과 읽기 모델(Query)을 분리 |
| Command / Handler | 상태를 바꾸는 요청 객체 / 그것을 처리하는 클래스 |
| view_model | API 응답 전용 data class (Entity 직렬화 금지) |
| read row | QueryDSL 결과를 받는 중간 DTO (Kotlin 파생이 필요할 때) |
| Integration Event | 다른 BC 와 주고받는 이벤트(Kafka) — payload camelCase |
| Domain Event | Aggregate 내부에서 생기는 이벤트(이 MS 는 현재 미사용) |
| 봉투 (MessageEnvelope) | 이벤트 메타(messageId · messageType · schemaVersion)를 담는 래퍼 |
| DLT | Dead Letter Topic — 재시도 끝에 실패한 메시지를 격리하는 토픽 |
| 멱등 (Idempotent) | 같은 입력을 여러 번 적용해도 결과가 같음 |
| Port / Adapter | application 이 정의한 외부 계약 / infrastructure 의 구현 |
| ArchUnit | 패키지 의존 규칙을 테스트로 검사하는 라이브러리 |
| Liquibase changelog / changeSet | DB 스키마 변경 이력 파일 / 그 안의 변경 1건 |
| QueryDSL Q클래스 | kapt 가 Entity 에서 생성하는 타입세이프 쿼리 메타 클래스 |
| kapt | Kotlin 용 애노테이션 프로세서 실행기 |
| CGLIB 프록시 | 클래스를 상속해 메서드를 가로채는 Spring 의 AOP 방식(@Transactional) |
| self-invocation | 같은 객체 내부 호출 — 프록시를 거치지 않아 AOP 가 안 걸림 |
| Module Federation (MF) | 런타임에 다른 빌드의 모듈을 불러오는 webpack/rspack 기능 |
| host / remote | MF 에서 다른 앱을 끼우는 쪽 / 끼워지는 쪽 |
| remoteKey | remote 의 MF 이름(`diagnostic_result`) — CDN 경로와 host 설정에 쓰인다 |
| FSD | Feature-Sliced Design — views/widgets/features/entities/shared 레이어 구조 |
| 훅 (Hook) | `use` 로 시작하는 React 함수 — 상태 · 효과 · 재사용 로직 |
| 셀렉터 | 스토어에서 필요한 조각만 고르는 함수 `(s) => s.listMode` |
| replayLast | 이벤트 버스가 마지막 값을 보관해 늦은 구독자에게도 전달하는 옵션 |
| 낙관적 업데이트 | 서버 응답 전에 화면을 먼저 바꾸고, 실패하면 되돌리는 방식 |
| 디자인 토큰 | `--spacing-4` · `--color-foreground` 같은 CSS 변수 이름의 디자인 값 |
| tier (L/M/S) | 화면이 받은 폭 단계 — 1200 이상 L, 640 이상 M, 그 밖 S |
