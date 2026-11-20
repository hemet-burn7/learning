# 2. 프로젝트 구조와 소스 탐색

[이전: Kotlin 문법](01-csharp-to-kotlin.md) · [목차](README.md) · [다음: 데이터 바인딩](03-request-and-data-binding.md)

## 2.1 솔루션 대신 서비스 모듈부터 찾는다

`project-x`는 여러 레포가 모인 작업공간이다. `backend/settings.gradle.kts`가 포함하는 Gradle 모듈을 결정하고, 각 서비스 모듈의 `build.gradle.kts`가 의존성·실행 산출물을 정한다. **MS 하나가 배포단위이고 4개 레이어는 그 모듈 안의 패키지**다.

```text
x-patient-appointment/
  _docs/                        업무·설계·API 명세
  backend/
    settings.gradle.kts         patient-appointment, medication-destination
    build.gradle.kts            공통 플러그인·버전·검증
    patient-appointment/
      build.gradle.kts
      src/main/kotlin/com/projectx/patientappointment/
        presentation/           HTTP 입출력
        application/            유스케이스·명령·조회 계약·외부 포트
        domain/                 업무 상태·규칙
        infrastructure/         DB·HTTP·Kafka 구현
      src/main/resources/       설정·Liquibase·ORM 등록
      src/test/kotlin/          테스트
    medication-destination/     별도 서비스, 별도 스키마
```

[C#의 프로젝트 참조]처럼 폴더만으로 컴파일 의존성이 차단되는 구조가 아니다. 담당 레포는 `LayerIsolationTest` 등의 아키텍처 테스트로 레이어 규칙을 확인한다.

| 레포 | 백엔드 모듈·읽기 출발점 | 역할 |
|---|---|---|
| clinical-care | [settings](../../../x-clinical-care/backend/settings.gradle.kts), [ordering 안내](../../../x-clinical-care/backend/ordering/AGENTS.md) | 처방·처방정의·진료기록 등 여러 서비스. 이 교재의 대표는 ordering |
| patient-appointment | [settings](../../../x-patient-appointment/backend/settings.gradle.kts), [로컬 기준](../../../x-patient-appointment/backend/CLAUDE.md) | 예약 서비스와 약 전달위치 결정 서비스 |
| diagnostic-result | [settings](../../../x-diagnostic-result/backend/settings.gradle.kts), [로컬 기준](../../../x-diagnostic-result/backend/CLAUDE.md) | 결과 통합 조회·사용자 설정·AI 분석 |

`medication-destination`은 동기 HTTP 업무이며 Kafka·inbox가 없다. 레포 이름에 예약이 들어간다고 모든 모듈이 예약 Aggregate를 가지는 것은 아니다.

## 2.2 각 레이어에 던지는 질문

| 레이어 | 질문 | 찾아볼 코드 |
|---|---|---|
| presentation | 어떤 URL·본문·인증 정보를 받는가? | Controller, Request, `TokenPrincipalScope` |
| application | 이 업무를 어떤 순서로 수행하는가? | Command, Validator, Handler, Service |
| domain | 어떤 상태 전이가 허용되는가? | Aggregate 메서드, Entity, VO, enum, Policy |
| infrastructure | 그 값을 DB·외부 시스템과 어떻게 주고받는가? | Repository, Queries, HTTP Adapter, eventbus, inbox |

**Controller만 보고 저장·commit 여부를 단정하지 않는다.** Controller는 요청 변환을 보여 주지만 실제 쓰기 규칙과 저장 경계는 다음 호출에서 확인해야 한다. 반대로 도메인 클래스부터 전부 읽으면 그 규칙을 어떤 요청에서 사용하는지 놓치기 쉽다.

## 2.3 처음에는 한 업무를 수직으로 읽는다

간단한 시작점은 검사결과 메모 저장이다.

```text
ObservationsController.saveObservationMemo
  → SaveObservationMemoRequest.toCommand
  → CommandBus.send
  → RegisterObservationMemoCommandValidator.validate
  → RegisterObservationMemoCommandHandler.handle
  → repository.requireObservation
  → Observation.writeMemo
  → ObservationRepository.save
  → 최외곽 트랜잭션 commit
  → HTTP 204
```

첫 파일은 [ObservationsController](../../../x-diagnostic-result/backend/diagnostic-result/src/main/kotlin/com/projectx/diagnosticresult/presentation/ObservationsController.kt)다. IDE에서 다음 호출의 정의로 이동한다. 인터페이스에서 멈추면 **구현으로 이동**한다. 반환 타입이 DTO인지 Entity인지 확인한다. 3장과 5장에서 같은 사례를 자세히 설명한다.

그다음에는 반대 방향도 읽는다. `GET` 메서드 → `IObservationQueries` → `ObservationQueries` → 조건·select 컬럼 → ViewModel. 쓰기와 읽기의 파일은 같지 않다.

## 2.4 도메인 용어를 업무 코드에 붙이기

- **Aggregate Root:** 관련 상태를 일관되게 변경하는 진입점. `Appointment`, `Schedule`, `Observation`을 떠올린다.
- **Entity:** 식별자를 갖고 시간에 따라 바뀌는 객체. 같은 값이라도 id가 다르면 다른 업무 대상이다.
- **Value Object(VO):** 주소·스냅샷·범위처럼 값의 묶음으로 의미를 갖는 객체. 모든 VO가 별도 테이블인 것은 아니다.
- **Repository:** 쓰기 업무가 Aggregate를 찾고 저장하는 계약. 내부에서 QueryDSL로 찾을 수도 있다.
- **Query/ViewModel:** 사용자가 읽기 좋은 응답을 만드는 계약·데이터. C# MVVM의 변경 알림 ViewModel과 다르며 서버 응답 DTO다.
- **Port/Adapter:** 외부 기능의 인터페이스와 HTTP 등 실제 연결 구현. 도메인이 HTTP JSON을 직접 해석하지 않게 한다.
- **CommandBus:** 어떤 Command를 어떤 Validator·Handler로 보낼지 결정하는 프로젝트 코드. 그 자체가 Kafka는 아니다.

`Schedule`과 `Appointment`가 한 DB 트랜잭션에서 함께 바뀌어도 하나의 클래스가 되는 것은 아니다. Aggregate 경계와 트랜잭션 경계는 다른 질문이다.

## 2.5 기준 사례와 담당 레포의 실제 차이

| 항목 | clinical-care의 ordering | 담당 두 레포의 대표 구현 |
|---|---|---|
| 쓰기 경계 | CommandBus 트랜잭션; 서명은 바깥 `OrderDisplaySigning` 트랜잭션 | 일반 CommandHandler의 `handle`; 수신은 inbox 바깥 경계에 합류 |
| 저장 | EntityManager + UnitOfWork·dirty checking | Repository → Spring Data JPA; 예약 단건은 `saveAndFlush` |
| 도메인 이벤트 | UnitOfWork가 수집·전달, 통합 이벤트 outbox 저장 | Repository dispatcher; 예약은 커밋 후 발행 |
| 조회 구현 위치 | 대표 `application/queries/OrderingQueries` | `infrastructure/queries` |
| HTTP 응답 | common-web 봉투 `{data}`, 페이지 `meta.pagination` | ViewModel 직접 반환·`ItemsViewModel`·`PageViewModel` 등 실제 선언 확인 |
| 오류 | common-web의 `{error:{code,message}}` | `DomainException` + FHIR 형태 `issue[]` |
| Kafka | outbox 재시도 경로 존재 | 예약: best-effort 발행 / 결과: 수신 전용 |
| 소스의 문서 위치 | `docs/`, 서비스 `CONTEXT.md` 등 | 레포 `_docs/` |

이 표는 **교재에서 추적한 대표 경로**의 비교다. 모든 clinical-care 모듈이 ordering과 같다고 단정하지 않는다. 예약의 결과 발행 collector나 검사결과 AI처럼 같은 레포 안에도 트랜잭션 예외가 있다.

빌드도 다르다. 현재 [clinical-care 빌드](../../../x-clinical-care/backend/build.gradle.kts)는 Kotlin 2.4.10 / Spring Boot 4.1.0, [예약](../../../x-patient-appointment/backend/build.gradle.kts)과 [검사결과](../../../x-diagnostic-result/backend/build.gradle.kts)는 Kotlin 2.3.0 / Boot 4.0.4를 선언한다. JDK toolchain은 25다. 이는 **현재 파일의 선언값**이며 최신 권장 버전 안내가 아니다. Jackson·EventBus import와 버전도 대상 빌드에서 확인한다.

## 2.6 검색으로 호출 경로 찾기

다음은 `project-x`에서 실행하는 읽기 명령이다.

```bash
# 어떤 컨트롤러인지
rg -n 'memo|book-bundle' x-diagnostic-result/backend/diagnostic-result/src/main/kotlin x-patient-appointment/backend/patient-appointment/src/main/kotlin

# 인터페이스 구현과 호출
rg -n 'IObservationRepository|RegisterObservationMemoCommand' x-diagnostic-result/backend/diagnostic-result/src

# 트랜잭션, 이벤트, 실제 전송 지점
rg -n 'Transactional|saveAndFlush|publish\(|afterCommit' x-patient-appointment/backend/patient-appointment/src/main/kotlin

# Q클래스 생성 대상인 Entity 찾기
rg -n '@Entity|@Table|@Version' x-diagnostic-result/backend/diagnostic-result/src/main/kotlin/com/projectx/diagnosticresult/domain
```

`.env`·인증 토큰·환자 데이터는 소스 학습에 필요하지 않다. 코드·설계·테스트로 구조를 먼저 이해한다.

[이전: Kotlin 문법](01-csharp-to-kotlin.md) · [목차](README.md) · [다음: 데이터 바인딩](03-request-and-data-binding.md)
