# 11. 용어와 빠른 찾기

[이전: 기능 개발·디버깅](10-feature-development-and-debugging.md) · [목차](README.md)

## 용어 사전

| 용어 | 이 프로젝트에서 읽는 뜻 | 다시 읽기 |
|---|---|---|
| BC / Bounded Context | 자기 업무 언어와 모델을 소유하는 경계 | [2장](02-architecture-and-navigation.md) |
| MS / Microservice | 독립 배포하는 서비스·Gradle 모듈 | [2장](02-architecture-and-navigation.md) |
| Bean | Spring이 생성·주입·프록시 관리하는 객체 | [1장](01-csharp-to-kotlin.md) |
| DI | 구현 객체를 생성자 등에 주입하는 연결 | [3장](03-request-and-data-binding.md) |
| DTO / Request / ViewModel | 경계에서 값을 주고받는 객체 | [3장](03-request-and-data-binding.md) |
| Command | 수행할 업무 입력 | [5장](05-commands-and-transactions.md) |
| Validator | 입력 형식·필수 조건 검사기 | [3장](03-request-and-data-binding.md) |
| Handler | 업무를 수행하는 호출 순서 | [5장](05-commands-and-transactions.md) |
| Aggregate / Root | 일관된 업무 변경의 경계와 진입점 | [2장](02-architecture-and-navigation.md) |
| VO / Value Object | 의미 있는 값의 묶음 | [2장](02-architecture-and-navigation.md) |
| Repository | 쓰기 업무의 Aggregate 로딩·저장 계약 | [5장](05-commands-and-transactions.md) |
| JPA / Hibernate | ORM 규격 / 그 구현 | [5장](05-commands-and-transactions.md) |
| EntityManager | Entity 영속성 컨텍스트를 다루는 API | [5장](05-commands-and-transactions.md) |
| managed / detached | 현재 컨텍스트가 추적함 / 떨어져 있음 | [5장](05-commands-and-transactions.md) |
| dirty checking | 관리 객체 변경을 찾아 SQL 반영 | [5장](05-commands-and-transactions.md) |
| persist / merge | 새 객체 관리 / 상태를 관리 객체에 병합 | [5장](05-commands-and-transactions.md) |
| flush / commit | SQL 동기화 / DB 트랜잭션 확정 | [5장](05-commands-and-transactions.md) |
| optimistic lock | version으로 동시 수정 충돌 감지 | [5장](05-commands-and-transactions.md) |
| QueryDSL / Q클래스 | 타입으로 쿼리 구성 / 생성된 Entity 경로 | [4장](04-querydsl-and-read-models.md) |
| projection / Tuple | 필요한 값을 선택 / 여러 선택값 묶음 | [4장](04-querydsl-and-read-models.md) |
| materialize / hydrate | 조회 결과를 객체로 구성 | [3장](03-request-and-data-binding.md) |
| lazy loading | 관계 접근 시 필요한 데이터를 나중에 읽음 | [4장](04-querydsl-and-read-models.md) |
| Port / Adapter | 외부 기능 계약 / HTTP 등 연결 구현 | [5장](05-commands-and-transactions.md) |
| snapshot | 당시의 외부 값을 복사해 보관 | [5장](05-commands-and-transactions.md) |
| CDC / shared | 원천 변경을 반영한 공유 읽기 데이터 | [4장](04-querydsl-and-read-models.md) |
| DomainEvent | 내부 업무 변화 알림 | [6장](06-kafka-and-inbox.md) |
| IntegrationEvent | 서비스 사이의 이벤트 계약 | [6장](06-kafka-and-inbox.md) |
| Envelope / payload | 전송 메타데이터 / 업무 내용 | [6장](06-kafka-and-inbox.md) |
| Outbox / Inbox | 발행할 데이터 보관 / 받은 데이터·처리 상태 보관 | [6장](06-kafka-and-inbox.md) |
| idempotency / 멱등 | 같은 업무 입력 반복 시 중복 효과를 만들지 않음 | [6장](06-kafka-and-inbox.md) |
| eventual consistency | 비동기 변경이 전달·반영되면서 최종적으로 수렴하는 모델. 현재 best-effort 발행만으로 수렴이 보장되지는 않음 | [6장](06-kafka-and-inbox.md) |
| Liquibase | 변경 파일로 DB 형상을 관리 | [10장](10-feature-development-and-debugging.md) |
| kapt | Kotlin 빌드에서 annotation processor 실행, Q클래스 생성에 사용 | [4장](04-querydsl-and-read-models.md) |

## C# 개발자가 특히 혼동하기 쉬운 표현

| 보이는 코드 | 이렇게 해석한다 |
|---|---|
| `val entity = ...` | 참조 재대입 금지. Entity 내부 불변을 뜻하지 않음 |
| `data.copy(...)` | 얕은 객체 복사. DB 저장 아님 |
| `value!!` | null이면 런타임 예외. C# `!` 경고 억제와 다름 |
| `list.map { ... }` | 일반 컬렉션에서는 즉시 메모리 변환 |
| `@Transactional` | 실제 프록시 호출·전파·외곽 경계를 확인 |
| `save(entity)` | 구현 확인. commit과 동일시하지 않음 |
| `saveAndFlush(entity)` | SQL 동기화까지. 최외곽 tx는 계속될 수 있음 |
| `publishEvent(event)` | Spring 내부 이벤트일 수 있음 |
| `eventBus.publish(event)` | 전송 API 호출. 상대 업무 처리 완료 아님 |
| inbox `PROCESSED` | 메시지 처리 성공. 업무 승인·접수 수락과 별개 |
| `sourceEventVersion` | 발행측 업무 버전. JPA version과 별개 |

## 원하는 작업에서 바로 시작하기

| 하려는 일 | 출발점 |
|---|---|
| JSON 값이 어디 들어왔는지 찾기 | [3장 메모 저장 추적](03-request-and-data-binding.md) |
| 조회 조건·응답 컬럼 수정 | [4장 조회 형태](04-querydsl-and-read-models.md), [10장 과제 A](10-feature-development-and-debugging.md) |
| 여러 테이블을 함께 저장 | [5장 최외곽 트랜잭션](05-commands-and-transactions.md), [8장 묶음 예약](08-patient-appointment-walkthrough.md) |
| 외부 API 값 조회·세팅·저장 | [5장 세 패턴](05-commands-and-transactions.md), [9장 AI](09-diagnostic-result-walkthrough.md) |
| Kafka 발행 위치 찾기 | [6장 발행 전략](06-kafka-and-inbox.md), [7장 outbox](07-clinical-care-walkthrough.md) |
| 받은 결과가 안 보임 | [9장 inbox→Observation](09-diagnostic-result-walkthrough.md) |
| 새 직원에게 전체 흐름 설명 | [README 학습 순서](README.md), [10장 설명 질문](10-feature-development-and-debugging.md) |

## 한 기능을 읽을 때 적는 메모 양식

```text
업무 동작:
입구(URL / topic / 실행 함수):
입력값의 출처(Request / 토큰 / 외부 payload):
조회 구현·필수 scope:
업무 규칙의 클래스·메서드:
상태를 바꾸는 객체:
Repository와 flush 위치:
최외곽 트랜잭션의 시작·종료:
외부 HTTP / Kafka 효과:
실패·중복·과거 version 처리:
사용자가 결과를 다시 읽는 API:
관련 설계·테스트:
```

이 메모에서 “값 세팅”, “저장”, “commit”, “발행”, “상대 처리”를 서로 다른 줄에 쓸 수 있으면 그 기능의 중요한 경계를 이해한 것이다.

[이전: 기능 개발·디버깅](10-feature-development-and-debugging.md) · [목차](README.md)
