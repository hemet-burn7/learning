# 10. 기능 개발과 디버깅 실습

[이전: 검사결과 사례](09-diagnostic-result-walkthrough.md) · [목차](README.md) · [다음: 용어·빠른 찾기](11-glossary-and-cheatsheet.md)

## 10.1 첫 개발을 시작하는 순서

이 장의 변경 사례는 **교육용 과제**다. 실제 제품에 필드를 추가하거나 업무 규칙을 바꾼 결과가 아니다. 운영 기능을 변경할 때는 담당 레포의 설계·요구사항과 맞춘다.

1. **사용자 동작을 한 문장으로 쓴다.** 예: “사용자가 검사결과에 자신의 메모를 저장한다.”
2. **입구를 찾는다.** Controller URL인지 Kafka topic인지, 배치 호출인지 구분한다.
3. **값의 주인을 정한다.** 우리 DB 저장값인지, shared 표시값인지, 외부 HTTP·이벤트 스냅샷인지 확인한다.
4. **기존의 가장 가까운 수직 경로를 읽는다.** Request → Command → Validator → Handler → Aggregate → Repository와 조회를 연결한다.
5. **대상 설계를 먼저 갱신한다.** 담당 레포의 모델·Command·ViewModel·API 변경은 `_docs/design/<ms>.classdiagram.md`를 먼저 맞추고 구현한다.
6. **입출력 계약과 실패 의미를 정한다.** null·빈값·중복·과거 version·외부 미연동 시 동작을 정한다.
7. **필요한 코드와 검증을 함께 바꾼다.** 범위에 맞는 기존 테스트를 먼저 찾고 실제 쿼리·트랜잭션 검증이 필요한 부분을 구분한다.

기반 seedwork·CommandBus·inbox 등 골든 파일을 업무 기능 때문에 각 레포에서 제각각 수정하지 않는다. 기반 변경이 필요하면 [공통 기준](../../../.claude/CLAUDE.md)의 템플릿 관리 절차를 따른다.

## 10.2 과제 A: 조회 응답에 필드 하나 추가하기

예를 들어 **기존 Entity에 있는 필드를 새 응답에 표시**하는 과제라면 Entity나 DB부터 바꿀 필요가 있는지 먼저 따진다.

| 순서 | 수정 후보 | 확인할 질문 |
|---|---|---|
| 1 | 설계·API 계약 | 값의 이름·타입·null·표시 의미는? |
| 2 | `XxxViewModel` | 응답 필드의 Kotlin 타입과 JSON 이름은? |
| 3 | `XxxQueries` select | 해당 컬럼이 실제 조회되는가? |
| 4 | Tuple mapper / ViewMapper | 값을 응답 생성자에 넣는가? |
| 5 | Controller | 기존 반환 타입으로 충분한가? |
| 6 | 관련 조회 테스트 | 값 있음·없음·다른 분류·tenant 경계는? |

실제 시작점은 [ObservationQueries](../../../x-diagnostic-result/backend/diagnostic-result/src/main/kotlin/com/projectx/diagnosticresult/infrastructure/queries/ObservationQueries.kt)의 `categoryColumns`, `categoryQuery`, `toCategoryViews`다. 예약은 [AppointmentQueries](../../../x-patient-appointment/backend/patient-appointment/src/main/kotlin/com/projectx/patientappointment/infrastructure/queries/AppointmentQueries.kt)의 `views`와 [ViewMappers](../../../x-patient-appointment/backend/patient-appointment/src/main/kotlin/com/projectx/patientappointment/application/queries/ViewMappers.kt)를 따라간다.

외부 표시값이라면 새 로컬 컬럼이 필요한지 별도 결정한다. 조회마다 shared에서 읽을 이름과 당시 상황을 보관할 스냅샷은 수명이 다르다. 데이터 소유권을 정하지 않은 채 Entity·테이블에 이름 컬럼부터 늘리지 않는다.

**완료 기준:** 응답 필드가 있음을 넘어 올바른 원천·scope·null·날짜 의미를 갖고, 목록 행 수·쿼리 횟수가 의도대로 유지된다.

## 10.3 과제 B: 저장 규칙 추가하기

메모 길이 또는 예약 가능 조건 같은 업무 규칙을 추가한다고 하자.

| 규칙 종류 | 둘 위치 | 이유 |
|---|---|---|
| 요청 필수값·형식·길이 | CommandValidator / 경계 변환 | 잘못된 입력을 업무 처리 전 거절 |
| 한 Aggregate 상태 전이 | Aggregate 메서드 | HTTP·Kafka 등 입구가 달라도 동일 규칙 |
| 여러 업무 객체의 판단 | 도메인 Policy/Service | 규칙을 한곳에 유지 |
| 여러 Handler의 반복 순서 | application Service | 같은 절차 재사용 |
| HTTP 오류 응답 변환 | 대상 레포의 공통 예외 처리 | Controller마다 다른 오류 모양 방지 |

메모의 [Validator](../../../x-diagnostic-result/backend/diagnostic-result/src/main/kotlin/com/projectx/diagnosticresult/application/validations/observation/RegisterObservationMemoCommandValidator.kt)와 [ObservationMemo](../../../x-diagnostic-result/backend/diagnostic-result/src/main/kotlin/com/projectx/diagnosticresult/domain/aggregatesmodel/observation/ObservationMemo.kt)를 비교한다. Validator가 도메인에 정의된 길이·허용 서식 기준을 사용한다. 같은 숫자를 여러 파일에 각각 적어 서로 달라지게 만들지 않는다.

예약 규칙은 화면 추천만 고쳐서는 부족할 수 있다. 실제 저장의 `SlotOccupationService`·Aggregate 판정과 동일한 의미인지 확인한다. 원무 주도 강제 예약 같은 명시적 예외 경로도 8장에서 확인한다.

검증할 것은 “예외가 났는가”뿐 아니다. **예외 전 변경된 다른 Entity가 함께 rollback됐는가, 이벤트가 외부로 나가지 않았는가**도 해당 트랜잭션의 테스트 대상이다. Mock Repository만으로 실제 DB rollback까지 검증했다고 말할 수 없다.

## 10.4 과제 C: 외부 API 응답을 저장하기

처음에 다음 계약을 적는다.

| 결정 | 예시 질문 |
|---|---|
| 원천 | 누가 이 값의 정본을 갖는가? |
| 저장 목적 | 지금 표시할 값인가, 당시 상태를 남길 스냅샷인가? |
| 인증·범위 | 사용자 토큰 전달인지 서비스 인증인지, hospital은 어디서 오는가? |
| 변환 | 외부 snake_case·날짜·enum을 내부 타입으로 어디서 바꾸는가? |
| 실패 | 빈 목록으로 계속해도 되는가, 저장을 거절해야 하는가? |
| 시간 | 동기 호출 시간 동안 DB tx·행 잠금을 잡는가? |
| 재시도 | 원격 성공 후 로컬 실패 시 중복 호출이 허용되는가? |

구현 순서는 **Port 계약 → Adapter·외부 DTO → 매핑 → Handler/Service의 호출·상태 변경 → Repository → 조회 응답**이다. `@JsonProperty`로 기존 wire 이름을 유지하고 전역 naming strategy 변경으로 맞추지 않는다.

비교할 실제 예제는 [예약 의사 원천 조회·저장](08-patient-appointment-walkthrough.md), [검사결과 AI 호출·이력 저장](09-diagnostic-result-walkthrough.md), [ordering 초기값 조회·미저장](07-clinical-care-walkthrough.md)다. 외부 값을 받는 세 업무가 서로 다른 저장·실패 의미를 갖는다는 것을 설명해 본다.

## 10.5 과제 D: 이벤트 필드·수신 타입 변경하기

먼저 발행측 계약과 호환성을 확인한다. 새 필드를 붙인 payload 파일만으로 연동이 끝나지 않는다.

1. 발행·수신 payload 타입, `@JsonProperty`, messageType, schemaVersion, partition key를 확인한다.
2. route·구독·dispatcher 등록을 확인한다. 검사결과는 `ReceivedEventsConfig`가 공통 목록이다.
3. DTO→도메인 매퍼, source id·version 처리, 필수/선택 id 변환을 확인한다.
4. 저장 모델·changelog·ViewModel 중 실제로 필요한 곳을 갱신한다.
5. 중복·과거 version·수정·삭제·잘못된 payload의 테스트를 구성한다.
6. 수신 원문 적재와 업무 반영을 각각 확인할 수 있는 관측 지점을 둔다.

같은 message id 중복 방어와 새 message id의 과거 업무 version 방어를 별도로 검증한다. outbox·inbox가 있다고 누락·중복·역전의 모든 경우가 자동 해결되지는 않는다.

## 10.6 DB 모델을 바꾸는 경우

담당 레포는 JPA Entity와 Liquibase changelog를 함께 맞춘다. 부팅 시 Liquibase가 변경을 적용하고 Hibernate `ddl-auto: validate`가 매핑을 검사한다. **앱 기동도 DB 변경을 일으킬 수 있는 작업**이다.

| 변경 | 함께 볼 파일 |
|---|---|
| 영속 필드 | Entity/VO·매핑·새 Liquibase changeSet |
| 필수값 | 기존 행의 값 채우기 계획·nullability·입력 검증 |
| enum | DB 저장 문자열·API·이벤트 계약 |
| 관계 | FK 소유자·cascade·orphanRemoval·로딩 방식 |
| version | 요청 비교·응답 버전·낙관락 처리 |
| 시각 | `Instant`·timestamptz·병원 시간대 변환 |

적용된 changeSet을 고쳐 checksum을 바꾸거나 `ddl-auto=update`로 넘기지 않는다. 파괴적 변경은 담당 레포의 expand/contract·수동 처리 기준에 따른다. shared 원천 스키마는 이 서비스 Liquibase로 변경하지 않는다.

예약의 [로컬 영속성 기준](../../../x-patient-appointment/backend/CLAUDE.md)은 컬렉션의 자연키 UNIQUE와 flush 순서도 설명한다. `clear()` 후 같은 키를 새로 넣으면 INSERT/DELETE 순서 때문에 충돌할 수 있어 `syncDetails`처럼 기존 항목 갱신·삭제·추가를 나누는 패턴을 읽는다.

## 10.7 개발환경과 검증 명령

다음은 **실행 안내이며 이 문서 작성에서 실행한 결과가 아니다.** 작업 디렉터리는 각 레포의 `backend/`다. wrapper와 JDK toolchain, 사내 Nexus 접근을 준비한다. 전체 서비스 기동 없이 컴파일·단위 검증부터 진행한다.

```bash
# x-patient-appointment/backend 에서
./gradlew :patient-appointment:compileKotlin
./gradlew :patient-appointment:test
./gradlew :medication-destination:test

# x-diagnostic-result/backend 에서
./gradlew :diagnostic-result:compileKotlin
./gradlew :diagnostic-result:test

# x-clinical-care/backend 에서
./gradlew :ordering:compileKotlin
./gradlew :ordering:test
# 통합 테스트 환경(Docker 등)이 준비된 경우 별도 실행
./gradlew :ordering:integrationTest
```

Q클래스가 보이지 않으면 Gradle import·kapt 결과를 확인한다. 인증·네트워크 실패로 의존성을 못 받은 것은 소스 검증 통과가 아니다. IDE 빨간 줄을 없애기 위해 공통 라이브러리 버전을 임의로 낮추지 않는다.

담당 레포의 대표 구조 검증은 `LayerIsolationTest`, `CommandWiringTest`, `SchemaMappingTest`, `AuditAndTenantColumnsTest`다. 수신 모듈은 tenant·inbox 테스트도 본다. 이 테스트만으로 실제 PostgreSQL 제약·지연 로딩·Kafka 연동까지 확인됐다고 할 수 없다. 관련 테스트 경로는 [예약 사례](08-patient-appointment-walkthrough.md)와 [검사결과 사례](09-diagnostic-result-walkthrough.md)에 연결했다.

운영·개발 DB에 직접 붙는 대신 프로젝트 기준에 맞는 앱 로그·HTTP·준비된 테스트 환경으로 확인한다. 기동은 사용자가 준비한 설정과 환경에서 수행하며 문서에 비밀값이나 환자 정보를 복사하지 않는다. 계약 baseline 재생성 스위치는 검증 성공을 만들기 위한 수단으로 켜지 않는다.

## 10.8 중단점으로 원인을 좁히는 순서

| 증상 | 중단점·파일 | 확인할 것 |
|---|---|---|
| Controller 진입 전 400 | Request 타입·전역 예외 처리 | 필수 JSON·UUID·enum·날짜 형식 |
| 요청 값이 Command에 없음 | `toCommand`, Controller 생성자 호출 | 바인딩 문제인지 명시 복사 누락인지 |
| Command가 실행되지 않음 | `CommandBus.send` | validator 오류·handler 등록·제네릭 매핑 |
| DB 조회는 되는데 업무 변경 없음 | Handler→Aggregate 메서드 | 현재 상태·멱등 분기·scope |
| save 호출 뒤 전체 취소 | 외곽 트랜잭션 이후 예외 | flush 성공과 commit 성공 구분 |
| 다음 저장이 409 | 요청 expectedVersion·saved.version·flush | 오래된 화면인지 실제 동시 경합인지 |
| 외부 목록만 비어 있음 | Adapter `body`·fallback | 미설정·호출 실패·빈 응답·DTO 키 불일치 |
| 저장됐는데 원무 미반영 | Publisher·예약 상태·회신 inbox | REQUESTED, 실제 publish, 회신 지연/거절 |
| Kafka는 왔는데 결과 없음 | InboxDispatcher·매퍼·handler | 바인딩·UUID·버전·FAILED 원인 |
| AI 성공했는데 이력 없음 | 응답 정규화·SaveService | 외부 호출 성공과 로컬 저장 실패 구분 |

`@Transactional` 메서드 안 마지막 줄의 중단점에서는 아직 commit 전일 수 있다. 프록시 호출이 반환된 지점과 외곽 호출자를 함께 본다. SQL 로그에 UPDATE가 보이는 것만으로 최종 저장을 확정하지 않는다.

## 10.9 신규 입사자에게 설명해 보게 할 질문

1. Request, Command, Entity, ViewModel을 같은 객체로 만들면 무엇이 섞이는가?
2. `fetch().filter`와 `where(...).fetch()`의 비용은 어떻게 다른가?
3. 예약의 `saveAndFlush` 뒤 예외가 나면 앞서 실행된 SQL은 어떻게 되는가?
4. 외부 API DTO에 값을 넣었는데 저장되지 않는 이유는 무엇인가?
5. Kafka 원문이 inbox에 있는데 결과 조회가 비어 있을 수 있는가?
6. 주문 outbox, 예약 AFTER_COMMIT 발행, 결과 inbox는 각각 어느 실패 구간을 다루는가?
7. 검사결과 AI 분석·요약 핸들러가 `NOT_SUPPORTED`인 이유는 무엇인가?
8. HTTP에서 받은 tenant와 Kafka에서 해석한 tenant는 어느 코드가 정하는가?

정답을 외우기보다 **실제 소스 링크를 열어 호출 순서와 commit 경계를 표시**하게 한다. 마지막으로 본인이 맡은 기능에 대해 “입력·조회·상태 변경·저장·외부 효과·재조회”를 한 장으로 그려 설명하면 첫 실무 작업을 시작할 준비가 된다.

[이전: 검사결과 사례](09-diagnostic-result-walkthrough.md) · [목차](README.md) · [다음: 용어·빠른 찾기](11-glossary-and-cheatsheet.md)
