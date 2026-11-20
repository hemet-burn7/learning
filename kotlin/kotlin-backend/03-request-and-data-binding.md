# 3. 데이터는 언제 객체에 바인딩되는가

[이전: 프로젝트 구조](02-architecture-and-navigation.md) · [목차](README.md) · [다음: 조회 패턴](04-querydsl-and-read-models.md)

## 3.1 바인딩은 한 번이 아니다

C#의 모델 바인딩·JSON 역직렬화·EF 객체 로딩을 떠올리면 된다. 이 프로젝트에서도 경계마다 다른 도구가 다른 객체를 만든다.

| 경계 | 누가 채우는가 | 만들어지는 것 | 시점 | 저장 여부 |
|---|---|---|---|---|
| HTTP JSON → 요청 | Spring MVC + Jackson | Request DTO | Controller 진입 전 | 저장 안 됨 |
| URL·쿼리·헤더 → 인자 | Spring MVC 변환기 | UUID·문자열·날짜 등 | Controller 진입 전 또는 명시 파싱 | 저장 안 됨 |
| 토큰 → 인증 주체 | 인증 라이브러리·인자 해석기 | AuthPrincipal | 인증 및 메서드 인자 해석 | 저장 안 됨 |
| Request → Command | 개발자가 쓴 `toCommand`·생성자 | Command | Controller 안 | 저장 안 됨 |
| DB → Entity | Hibernate | 관리되는 Entity / 프록시 | Query 실행·지연 로딩 | 읽은 것뿐 |
| DB 컬럼 → ViewModel | QueryDSL + 생성자·매퍼 | 응답 DTO | fetch 및 변환 단계 | 저장 안 됨 |
| 외부 HTTP → Payload | RestClient/WebClient + JSON 변환 | 외부 DTO | body 해석 시 | 저장 안 됨 |
| Payload → 업무 상태 | 매퍼·Handler·Aggregate | 업무 객체 | 명시 호출 시 | 아직 commit 아님 |
| Kafka 원문 → 이벤트 | 담당 레포의 InboxDispatcher + Jackson | MessageEnvelope와 payload | inbox 업무 처리 시 | 원문은 앞선 트랜잭션에서 저장됨 |
| ViewModel → JSON | Spring MVC + Jackson | HTTP 본문 | Controller 결과 직렬화 | 추가 저장 없음 |

**Spring이 JSON을 Request에 채우는 일과 개발자가 Command를 만드는 일, Hibernate가 Entity를 읽는 일은 별개다.** 같은 필드명이 이어져도 객체가 하나로 공유되는 것은 아니다.

## 3.2 실제 메모 저장으로 한 값 추적하기

경로는 `PUT /api/v1/diagnostic-result/observations/{observationId}/memo`다. 다음은 형식 설명용 본문이며 실제 환자 데이터가 아니다.

```json
{ "memoHtml": "<p>교육용 메모</p>" }
```

[Controller.saveObservationMemo](../../../x-diagnostic-result/backend/diagnostic-result/src/main/kotlin/com/projectx/diagnosticresult/presentation/ObservationsController.kt)의 실제 발췌:

```kotlin
fun saveObservationMemo(
    @TokenPrincipal principal: AuthPrincipal,
    @PathVariable observationId: UUID,
    @RequestBody request: SaveObservationMemoRequest,
): ResponseEntity<Void> {
    commandBus.send(principal.toTenantScope(), request.toCommand(principal.employeeId(), observationId))
    return ResponseEntity.noContent().build()
}
```

1. `memoHtml`은 JSON에서 [SaveObservationMemoRequest](../../../x-diagnostic-result/backend/diagnostic-result/src/main/kotlin/com/projectx/diagnosticresult/presentation/SaveObservationMemoRequest.kt)의 생성자 프로퍼티로 들어온다.
2. `observationId`는 경로에서 온다. 본문에 같은 이름을 넣었다고 이 인자가 바뀌는 것이 아니다.
3. `userId`는 `principal.employeeId()`에서 나온다. 본문에서 사용자를 선택하지 않는다.
4. `toCommand`는 이 세 값을 새 [RegisterObservationMemoCommand](../../../x-diagnostic-result/backend/diagnostic-result/src/main/kotlin/com/projectx/diagnosticresult/application/commands/observation/RegisterObservationMemoCommand.kt)에 복사한다.
5. Validator가 형식·길이를 확인하고 Handler가 scope 안의 결과를 읽는다.
6. `Observation.writeMemo`가 메모의 업무 상태를 바꾼다. 저장·커밋은 뒤 단계다.
7. 이 경로는 204라 응답 DTO가 없다. 저장 내용을 읽으려면 별도 조회가 필요하다.

### 디버거에서 보는 값

| 중단점 | 확인할 값 | 아직 일어나지 않은 일 |
|---|---|---|
| Controller 첫 줄 | request, path id, principal | Command 실행 |
| `toCommand` 반환 | 세 입력의 출처가 올바른가 | Entity 변경 |
| Handler 읽기 직후 | 대상 id, tenant, 현재 메모 | 신규 메모 반영 |
| `writeMemo` 직후 | 메모 컬렉션·내용 | DB commit |
| Controller의 `send` 다음 줄 | 이 HTTP 경로에서는 Handler tx 정상 종료 | 클라이언트가 응답을 받았는지는 별개 |

토큰 문자열·메모 원문을 로그에 통째로 출력해서 확인하지 않는다. id·상태·흐름과 마스킹된 개발 데이터를 사용한다.

## 3.3 요청 검증과 업무 검증은 다르다

| 단계 | 예 | 찾을 위치 |
|---|---|---|
| 바인딩 | UUID 문자열 형식 오류, JSON 파싱 실패 | Controller에 들어오기 전, 공통 예외 처리 |
| Command 검증 | 메모 길이·허용 HTML·빈 내용 | [RegisterObservationMemoCommandValidator](../../../x-diagnostic-result/backend/diagnostic-result/src/main/kotlin/com/projectx/diagnosticresult/application/validations/observation/RegisterObservationMemoCommandValidator.kt) |
| 업무 조회 | 해당 scope의 결과가 없음 | `requireObservation` |
| 도메인 규칙 | 원천 삭제된 결과의 메모 쓰기 거절 | `Observation.writeMemo` |
| DB 정합성 | 낙관락·유일 제약 충돌 | flush / commit |

non-null `String`은 모든 업무 검증을 대신하지 않는다. 빈 문자열·허용 길이·HTML 정책은 별도로 검사한다. 기본값이 있는 nullable 필드는 누락되어도 객체가 만들어질 수 있으므로 DTO의 타입과 기본값을 함께 읽는다.

## 3.4 외부 API 응답은 명시적으로 다른 객체로 옮긴다

[DepartmentHttpAdapter.findOutpatientDepartments](../../../x-patient-appointment/backend/patient-appointment/src/main/kotlin/com/projectx/patientappointment/infrastructure/proxies/DepartmentHttpAdapter.kt)의 핵심 발췌:

```kotlin
// client.get().uri(...) 다음 부분
.retrieve()
.body(Array<DepartmentPayload>::class.java)
.orEmpty()
.mapNotNull { it.toView() }
```

네트워크 응답을 JSON 변환기가 `DepartmentPayload[]`로 만든다. 다음 `mapNotNull`은 개발자가 작성한 `toView`를 실행한다. 이 단계는 DB와 무관하다.

[DepartmentPayload](../../../x-patient-appointment/backend/patient-appointment/src/main/kotlin/com/projectx/patientappointment/infrastructure/proxies/DepartmentPayload.kt)의 실제 선언 일부:

```kotlin
@JsonProperty("department_id") val departmentId: String? = null
@JsonProperty("department_name") val departmentName: String? = null
```

외부 JSON은 `department_id`, Kotlin은 `departmentId`다. `@JsonProperty`는 **JSON 경계**를 정하며 DB 컬럼명과는 무관하다. `toView`는 id가 빈 항목을 버리고 표시 이름의 대체값을 정한다. 이 목록 조회는 저장하지 않는다. 외부 값을 업무 객체에 복사해 저장하는 사례는 5장과 8장에 있다.

## 3.5 Entity 바인딩과 ViewModel 바인딩

`queryFactory.selectFrom(q).fetch()`가 Entity를 선택하면 Hibernate가 컬럼을 Entity에 채우고 현재 영속성 컨텍스트가 이를 관리할 수 있다. 이미 같은 id의 객체가 관리 중이면 그 객체가 반환될 수도 있다. 따라서 **함수 호출마다 반드시 새 객체·최신 스냅샷을 얻는다는 뜻이 아니다.**

반면 컬럼을 골라 `Tuple`·ViewModel로 받으면 응답 데이터를 만든다. ViewModel의 `name`을 바꾸는 것은 DB Entity 변경이 아니다. `@Table`, `@Column`, `@Embedded`는 ORM 매핑이고 Request의 JSON 바인딩을 정하지 않는다.

예약의 [Appointment](../../../x-patient-appointment/backend/patient-appointment/src/main/kotlin/com/projectx/patientappointment/domain/aggregatesmodel/appointment/Appointment.kt)는 임베드 스냅샷에 nullable 백킹 필드를 사용한다. 모든 임베드 컬럼이 NULL이면 Hibernate가 그 필드를 null로 로딩할 수 있기 때문이다. 새 객체를 생성한 단위 테스트만으로 DB 로딩 상태를 전부 검증할 수는 없다.

## 3.6 Kafka에서는 원문 저장과 업무 바인딩이 분리된다

담당 레포의 현재 구독은 `subscribeRaw`로 원문을 inbox에 저장한다. 이후 [검사결과 InboxDispatcher](../../../x-diagnostic-result/backend/diagnostic-result/src/main/kotlin/com/projectx/diagnosticresult/infrastructure/inbox/InboxDispatcher.kt)가 등록된 이벤트 타입으로 `MessageEnvelope<T>`를 만들도록 Jackson에 지시한다. payload의 문자열 id를 업무 UUID·날짜·enum으로 바꾸는 매퍼는 그 다음이다.

따라서 “Kafka 수신 로그 있음”·“inbox 행 있음”·“DTO 변환 성공”·“Observation 저장 성공”은 각각 다른 상태다. malformed payload는 inbox에 존재해도 업무 테이블에는 반영되지 않을 수 있다. 6장과 9장에서 실패 상태를 따라간다.

## 3.7 DI도 일종의 연결이지만 업무 데이터 바인딩과 다르다

앱 시작 시 Spring이 `@Service`, `@Repository`, `@Bean` 등을 등록하고 인터페이스 구현을 생성자에 주입한다. `CommandBus`는 Handler·Validator 목록을 받아 타입별로 연결한다. 이것은 **어떤 코드가 호출될지 연결하는 작업**이다.

JSON 역직렬화는 요청마다 입력 값을 채운다. JPA 로딩은 조회할 때 업무 값을 채운다. DI가 잘됐다고 DTO 필드·DB 컬럼 매핑이 맞는 것은 아니다.

[이전: 프로젝트 구조](02-architecture-and-navigation.md) · [목차](README.md) · [다음: 조회 패턴](04-querydsl-and-read-models.md)
