# 1. C#에서 Kotlin으로: 소스를 읽는 데 필요한 문법

[목차](README.md) · [다음: 프로젝트 구조](02-architecture-and-navigation.md)

## 1.1 언어·프레임워크·프로젝트 코드를 구분한다

Kotlin은 JVM에서 실행되고 웹·트랜잭션·DI는 Spring이 담당한다. Kotlin 문법이 데이터를 자동 저장하거나 Kafka로 전송하지는 않는다.

| C# 경험 | 이 소스의 대응 개념 | 주의할 차이 |
|---|---|---|
| 솔루션·`.csproj` | `settings.gradle.kts`·`build.gradle.kts` | 레이어가 별도 프로젝트가 아니라 서비스 내부 패키지다 |
| NuGet | Gradle 의존성·Maven 저장소 | 사내 라이브러리 해석에 Nexus 접근 필요 |
| ASP.NET Core Controller | Spring `@RestController` | 입력 위치를 `@RequestBody`·`@RequestParam`·`@PathVariable`로 읽음 |
| 생성자 DI | 생성자 인자 + Spring Bean | Spring이 관리하는 객체여야 자동 주입됨 |
| Attribute | Annotation `@...` | 프록시·직렬화·ORM 등 해석 주체가 각각 다름 |
| `record` / DTO | `data class` | `copy()`는 얕은 복사 |
| LINQ to Entities | QueryDSL JPA | Kotlin `list.filter`는 메모리 연산일 수 있음 |
| EF Core / DbContext | JPA / Hibernate / EntityManager | JPA는 규격, Hibernate는 구현, Spring Data는 저장소 편의 계층 |
| `Guid` | `UUID` | 외부 식별자가 모두 UUID는 아님 |
| `DateTimeOffset` | `OffsetDateTime`, `Instant` | `LocalDateTime`에는 오프셋 없음 |
| `void` | `Unit` | `Command<Unit>`처럼 제네릭 반환 타입에도 사용 |
| `object` 타입 | `Any` / `Any?` | Kotlin `object` 선언은 싱글턴 |

## 1.2 클래스 선언 읽기

[메모 저장 핸들러](../../../x-diagnostic-result/backend/diagnostic-result/src/main/kotlin/com/projectx/diagnosticresult/application/commands/observation/RegisterObservationMemoCommandHandler.kt)의 선언 발췌:

```kotlin
@Service
class RegisterObservationMemoCommandHandler(
    private val repository: IObservationRepository,
) : CommandHandler<RegisterObservationMemoCommand, Unit> {
    // 메서드 생략
}
```

`@Service`는 Spring 빈 등록 표시다. 클래스 이름 뒤 괄호가 기본 생성자이며 `private val repository`가 인자와 프로퍼티 선언을 겸한다. C# 생성자와 `private readonly` 필드를 한자리에 적은 것으로 읽는다. `:`는 상속·인터페이스 구현이고 마지막 인자 뒤 쉼표도 허용된다.

`val`은 **참조 재대입을 금지**하며 객체 내부 변경까지 막지는 않는다.

```kotlin
val names = mutableListOf("예시")
names.add("교육")       // 같은 목록의 내용 변경은 가능
// names = mutableListOf() // val 참조 재대입은 불가
```

`var`는 재대입 가능하다. Entity의 `var ... protected set`은 외부 대입을 제한해 `book()`, `cancel()` 같은 업무 메서드로 변경하게 한다.

## 1.3 null과 반환식

| 표현 | 읽는 방법 | C#와 비교 |
|---|---|---|
| `Appointment?` | 없을 수 있는 값 | nullable reference |
| `row?.version` | row가 있으면 접근 | `?.` |
| `value ?: fallback` | null이면 오른쪽 | `??` |
| `value ?: throw ...` | null이면 예외 | `?? throw` |
| `value!!` | null이면 실제 NPE 발생 | C# 경고 억제 `!`와 다름 |
| `value as? Type` | 변환 불가면 null | `as` |
| `if (x is Type)` | 타입 검사 후 smart cast | 타입 패턴 검사 |

Kotlin null 안전성은 컴파일러가 확인할 수 있는 범위의 장치다. Java 라이브러리·리플렉션·ORM이 채우는 값까지 전부 보증하지 않는다. [공식 null 안전성 설명](https://kotlinlang.org/docs/null-safety.html).

```kotlin
// 설명용: if도 값을 만드는 식이다.
val display = if (name.isNullOrBlank()) "이름 없음" else name
fun hasValue(value: String?): Boolean = !value.isNullOrBlank()
```

`fun ... = 식`은 그 식을 반환한다. `when`도 `switch`처럼 분기하며 값을 만들 수 있다. enum 상태를 추가하면 어떤 `when` 분기까지 바뀌는지 확인한다.

## 1.4 data class는 데이터를 나르는 상자

[SaveObservationMemoRequest](../../../x-diagnostic-result/backend/diagnostic-result/src/main/kotlin/com/projectx/diagnosticresult/presentation/SaveObservationMemoRequest.kt)의 선언에서 변환 메서드만 생략한 코드:

```kotlin
data class SaveObservationMemoRequest(val memoHtml: String)
```

`data class`는 주 생성자 프로퍼티를 바탕으로 동등성, `toString`, `copy` 등을 제공한다. `copy`는 얕은 복사다. JPA Entity는 영속 객체의 정체성이 중요하므로 무조건 `data class`로 만들지 않고 기존 Entity 베이스를 따른다. [공식 data class 설명](https://kotlinlang.org/docs/data-classes.html).

```kotlin
// 설명용: named argument, 기본값, 복사
data class SearchInput(val keyword: String? = null, val limit: Int = 20)
val first = SearchInput(keyword = "교육용")
val second = first.copy(limit = 10)
```

`copy`는 메모리 객체를 만들 뿐 HTTP 호출·DB 저장·이벤트 발행을 수행하지 않는다.

## 1.5 let / apply / also / run

| 함수 | 대상 접근 | 반환값 | 자주 쓰는 용도 |
|---|---|---|---|
| `let` | `it` 또는 지정 인자명 | 마지막 식 | nullable 값 변환 |
| `apply` | `this` | 원래 객체 | 생성 후 설정 |
| `also` | `it` | 원래 객체 | 로그·추가 동작 |
| `run` | `this` | 마지막 식 | 대상 기반 계산 |

```kotlin
// 설명용: patientId가 있을 때만 조건 추가
filter.patientId?.let { id -> where.and(q.patientId.eq(id)) }
```

블록마다 **대상은 누구인가, 무엇을 반환하는가, 부수효과가 있는가**를 읽는다. `?.let`을 잘못 쓰면 필수 조건이 통째로 빠질 수 있다. [공식 scope functions 설명](https://kotlinlang.org/docs/scope-functions.html).

[DepartmentHttpAdapter](../../../x-patient-appointment/backend/patient-appointment/src/main/kotlin/com/projectx/patientappointment/infrastructure/proxies/DepartmentHttpAdapter.kt)의 `runCatching { ... }.onFailure { ... }.getOrDefault(emptyList())`는 실패를 빈 목록으로 바꾼다. 예외가 발생해도 최종 호출자는 정상 반환을 받는다. 트랜잭션에서 예외를 삼키면 기대한 rollback이 일어나지 않을 수 있다.

## 1.6 컬렉션: LINQ처럼 읽되 실행 위치 확인

| Kotlin | 의도 |
|---|---|
| `map { ... }` | 항목 변환 |
| `mapNotNull { ... }` | 변환 결과 null 제외 |
| `filter { ... }` | 조건에 맞는 항목 유지 |
| `associateBy { it.id }` | id → 항목 Map; 중복 키는 뒤 값이 덮음 |
| `groupBy { ... }` | 키 → 여러 항목 Map |
| `firstOrNull()` | 첫 항목 또는 null |
| `single()` | 정확히 한 항목; 0개·2개 이상이면 예외 |
| `orEmpty()` | nullable 목록/문자열을 빈값으로 변경 |

일반 `List`의 `map`·`filter`는 호출 시 결과를 만드는 메모리 연산이다. C# `Enumerable`의 지연 파이프라인처럼 가정하지 않는다. `List<T>`는 읽기 API 인터페이스이며 완전한 불변 자료구조라는 뜻은 아니다. `Sequence`가 지연하는 것도 메모리 계산이지 SQL 변환이 아니다. DB 필터는 QueryDSL의 `where` 쪽이다.

## 1.7 확장 함수·object·예약어

[TokenPrincipalScope.kt](../../../x-patient-appointment/backend/patient-appointment/src/main/kotlin/com/projectx/patientappointment/presentation/TokenPrincipalScope.kt)의 실제 함수:

```kotlin
fun AuthPrincipal.toTenantScope(): TenantScope = TenantScope.of(tenantId, hospitalCode)
```

원래 `AuthPrincipal`을 수정하지 않고 호출 문법을 확장한다. C# 확장 메서드와 유사하다. 다른 패키지의 확장 함수는 함수의 전체 이름을 import한다(같은 패키지 등은 생략 가능). 객체에 새 상태가 생기지는 않는다.

- `object ErrorCodes`: 싱글턴 선언.
- `companion object`: 클래스에 붙은 동반 객체. `SomeType.create()` 같은 팩터리·상수 진입점.
- `X::class` / `X::class.java`: Kotlin 클래스 참조 / Java Class. 직렬화·로깅·DI에서 사용.
- ``a.id.`in`(ids)``: `in`은 예약어라 백틱으로 감싼 **메서드 이름**이다. 문자열 SQL이 아니다.
- `return@forEach`: 바깥 함수가 아니라 해당 람다에서 반환한다.
- `internal`: Kotlin 모듈 내부 접근. 이 프로젝트에서는 서비스 Gradle 모듈 경계와 연결된다.

## 1.8 애노테이션을 누가 해석하는가

| 애노테이션 | 해석 주체 | 효과 |
|---|---|---|
| `@RestController`·`@RequestBody` | Spring MVC | 라우팅·요청 변환 |
| `@Component`·`@Service`·`@Repository` | Spring | 관리 객체 등록 |
| `@Transactional` | Spring 트랜잭션 프록시 | 호출 경계의 트랜잭션 정책 |
| `@Entity`·`@Table`·`@Column` | JPA/Hibernate | 객체와 테이블·컬럼 매핑 |
| `@Version` | JPA/Hibernate | 동시 수정 충돌 감지 |
| `@JsonProperty` | Jackson | JSON 키와 프로퍼티 대응 |
| `@MessageType` | 사내 EventBus | 이벤트 wire 타입 식별 |

`@field:...`, `@get:...`은 필드·getter 중 적용 대상을 지정한다. 검증 애노테이션을 붙였다고 자동으로 모든 진입점이 검증되지는 않는다. `@Valid`, CommandValidator, 도메인 검증 중 실제 실행 경로를 확인한다.

Kotlin 클래스는 기본 final이다. 이 프로젝트의 Spring/JPA 플러그인은 프록시·Entity 생성 요구를 지원한다. 기존 `plugin.spring`, `plugin.jpa`, kapt 설정을 유지하고 생성된 Q클래스를 직접 편집하지 않는다.

## 1.9 읽기 연습

메모 저장 핸들러를 열고 설명해 본다.

1. 주입되는 의존성은? → `IObservationRepository`.
2. `Unit`의 의미는? → 업무 결과 데이터 없이 처리.
3. 어디서 읽고 변경하는가? → `requireObservation`, `writeMemo`.
4. `repository.save`가 commit인가? → 5장의 프록시·최외곽 트랜잭션 경계를 확인해야 한다.

[목차](README.md) · [다음: 프로젝트 구조](02-architecture-and-navigation.md)
