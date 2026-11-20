# 7. 외부 API에서 읽어 조립하거나 저장하는 패턴

[이전: 조회 후 변경·커밋](06-read-change-commit.md) · [목차](README.md) · [다음: 이벤트로 데이터 반영](08-event-data-patterns.md)

## 7.1 DB 조회와 외부 API 조회의 공통점

외부 API에서도 **요청 구성 → 실행 → 객체 바인딩 → 필요한 모양으로 변환**을 구분한다.

| DB 조회 | 외부 API 조회 |
|---|---|
| `select`, `where` | HTTP 메서드·URL·쿼리 파라미터 |
| `fetch` | `body` 같은 최종 호출 |
| DB 결과 → Entity/Tuple/DTO | 응답 JSON → 외부 Payload DTO |
| `map`, 생성자 호출 | `map`, `toView`, 내부 DTO 생성 |

외부 응답 DTO는 JPA가 관리하는 Entity가 아니다. 그 객체에 값을 넣거나 `copy`했다고 로컬 DB에 저장되지 않는다.

## 7.2 패턴 A: 읽어 온 값을 응답용으로만 변환한다

교육용 외부 JSON:

```json
[
  { "item_id": "A", "item_name": "Alpha" },
  { "item_id": "B", "item_name": null }
]
```

외부 식별자는 이 예제에서 문자열이다. 로컬 Entity UUID와 동일한 타입이라고 가정하지 않는다.

### C#

```csharp
// 설명용: 인증·base URL이 구성된 HttpClient.
public sealed record RemoteItem(
    [property: JsonPropertyName("item_id")] string Id,
    [property: JsonPropertyName("item_name")] string? Name);
public sealed record ItemOption(string Id, string Label);

var payload = await client.GetFromJsonAsync<RemoteItem[]>(requestUri);
var options = (payload ?? Array.Empty<RemoteItem>())
    .Select(x => new ItemOption(x.Id, x.Name ?? "이름 없음"))
    .ToList();
```

`requestUri`는 현재 호출 범위가 적용된 URL로 미리 구성했다고 가정한다.

### Kotlin

```kotlin
// 설명용: import·클라이언트 설정은 생략한다.
data class RemoteItem(
    @JsonProperty("item_id") val id: String,
    @JsonProperty("item_name") val name: String?,
)
data class ItemOption(val id: String, val label: String)

val payload = client.get()
    .uri { builder ->
        builder.path("/items")
            .queryParam("hospital_code", scope.hospitalCode)
            .build()
    }
    .retrieve()
    .body(Array<RemoteItem>::class.java)

val options = payload.orEmpty()
    .map { row -> ItemOption(row.id, row.name ?: "이름 없음") }
```

`client`는 인증·base URL·timeout이 구성된 Spring `RestClient`다. 동기 호출이며 C# 예제의 `await`와 기다리는 방식은 다르다. 이 비교는 요청·응답 변환의 의미를 대응시킨다.

### 데이터가 들어오는 시점

| 단계 | 변수·객체 상태 |
|---|---|
| `get`, `uri` | 요청 설정 중. payload 없음 |
| `retrieve` | 응답 처리 구성. 이것만으로 요청을 완료하지 않음 |
| `body(Array<RemoteItem>::class.java)` | 요청 실행·응답 수신·JSON 변환 후 DTO 배열 반환 |
| `payload.orEmpty()` | 배열이 없으면 빈 배열 |
| `map` | DTO에서 값을 읽어 새 응답 객체 구성 |

`body`에서 DTO 프로퍼티가 채워지고 `map`에서 다시 `ItemOption` 생성자 인자가 채워진다. 둘 다 메모리 객체를 만드는 단계다. `@JsonProperty`는 JSON 키를 연결하고 `@Column`은 DB 매핑을 연결하므로 역할이 다르다. [Spring RestClient 공식 설명](https://docs.spring.io/spring-framework/reference/integration/rest-clients.html).

결과는 `(A, Alpha)`, `(B, 이름 없음)`이다. `orEmpty()`는 null을 빈값으로 바꿀 뿐 HTTP 실패 예외를 잡지는 않는다. `runCatching { ... }.getOrDefault(emptyList())`가 있으면 그때는 실패까지 빈 목록으로 바꾸는 정책이다. “원천에 0건”과 “호출 실패”를 구분해야 하는 조회에서는 같은 정책을 무조건 복사하지 않는다.

## 7.3 패턴 B: DB 목록에 외부 표시값을 붙인다

로컬 Item에는 categoryId만 있고 이름은 외부에 있다고 하자. 100행을 읽으면서 각 행마다 API를 호출하면 HTTP N+1이 된다. 외부가 묶음 조회를 지원한다면 id를 모아서 읽는다.

### C#

```csharp
// 설명용: rows는 필수 범위 조건을 적용해 이미 읽은 목록.
var ids = rows.Where(x => x.CategoryId != null)
    .Select(x => x.CategoryId!.Value).Distinct().ToList();
var remoteRows = ids.Count == 0
    ? new List<CategoryDto>()
    : await remote.FindCategoriesAsync(scope, ids);
var byId = remoteRows.ToDictionary(x => x.Id);
var views = rows.Select(row => new ItemView(
    row.Id, row.Name,
    row.CategoryId is Guid id && byId.TryGetValue(id, out var c) ? c.Name : null
)).ToList();
```

### Kotlin

```kotlin
// 설명용: 외부가 UUID 분류 id의 묶음 조회를 지원한다고 가정한다.
val ids = rows.mapNotNull { it.categoryId }.distinct()
val remoteRows = if (ids.isEmpty()) emptyList()
    else remote.findCategories(scope, ids)
val byId = remoteRows.associateBy { it.id }
val views = rows.map { row ->
    ItemView(
        id = row.id,
        name = row.name,
        categoryName = row.categoryId?.let { byId[it]?.name },
    )
}
```

- `mapNotNull`은 없는 참조 id를 제외하고 `distinct`는 중복 id를 줄인다.
- 네트워크 실행은 `remote.findCategories` 내부다. 뒤의 `associateBy`·`map`은 메모리 작업이다.
- 분류가 없어도 Item 행은 유지하고 이름만 null로 둔다. 원래 행까지 버릴지는 별도 요구사항이다.
- Kotlin `associateBy`는 중복 키의 뒤 값을 남긴다. C# `ToDictionary`는 중복 키에서 예외가 난다. 두 예제는 외부 id가 유일하다고 가정했다.

외부 API가 묶음 조회를 제공하지 않으면 그대로 적용할 수 없다. 계약에 따라 목록을 한 번 읽는 방법, 제한된 병렬 호출, 허용된 캐시 등을 설계한다. 무한 병렬 호출은 일괄 조회와 다르다.

## 7.4 패턴 C: 외부 값을 로컬 Entity에 복사해 저장한다

지금 표시할 값인지 **이번에 확인한 원천 값을 보관**할 것인지에 따라 저장이 달라진다.

```kotlin
// 설명용 순서. 실제 프로젝트의 Port·명령·저장 경계를 사용한다.
val source = remote.findItem(scope, externalId)  // HTTP → 일반 DTO
val name = source.name                          // 아직 메모리 값
writer.changeName(scope, localId, name)          // 쓰기 트랜잭션 호출
```

다른 Spring 빈의 쓰기 경계:

```kotlin
@Transactional
fun changeName(scope: TenantScope, localId: UUID, name: String) {
    val item = repository.findById(scope, localId) ?: throw itemNotFound()
    item.changeName(name)             // 관리 Entity 변경
    repository.save(item)             // 프로젝트 저장·이벤트 처리 경로
}                                     // 최외곽 tx라면 정상 종료 시 commit
```

C#에서도 외부 DTO와 EF 추적 Entity를 나눈다.

```csharp
// 설명용: 이 함수의 외곽에 DB 트랜잭션이 없다고 가정한다.
var source = await remote.FindItemAsync(scope, externalId);
await using var tx = await db.Database.BeginTransactionAsync();
var item = await db.Items.SingleAsync(x =>
    x.Id == localId && x.TenantId == tenantId && x.HospitalCode == hospitalCode);
item.ChangeName(source.Name);
await db.SaveChangesAsync();
await tx.CommitAsync();
```

이 패턴은 필수 이름이 검증된 별도의 원천 응답을 가정한다. 앞의 nullable 표시용 DTO와 동일한 계약이라고 보지 않는다.

외부 호출을 DB 트랜잭션 밖에 두려면 **호출자에 이미 열린 tx가 없는지** 확인해야 한다. 쓰기 메서드를 분리한 것만으로 충분하지 않다. 필요한 경로는 `NOT_SUPPORTED` 등 정책과 프록시 호출을 확인한다.

저장된 이름은 복사한 시점의 스냅샷이다. 외부 원천 이름이 바뀌어도 연결된 객체처럼 바뀌지 않는다. 새로 가져와 덮을지, 조회 때 보강할지, 이벤트로 갱신할지 선택한다.

## 7.5 원천 조회와 로컬 조회를 섞을 때

| 상황 | 가능한 결과 | 확인할 점 |
|---|---|---|
| 로컬 목록 조회 후 외부 이름 조회 | 사이에 외부 값 변경 | 같은 시점의 snapshot이 필요한가? |
| shared CDC에서 이름 조회 | 원천보다 늦은 복제값 | 복제 지연을 허용하는가? |
| 긴 HTTP 호출 중 DB tx 유지 | 커넥션·잠금 장기 점유 가능 | 외부 호출과 저장을 분리할 수 있는가? |
| 외부 성공 후 로컬 commit 실패 | 원격 성공·로컬 미저장 | 재시도·중복 기준은? |
| 외부 타임아웃 | 원격 성공 여부 불명확 가능 | 읽기 재시도인가, 쓰기 재시도인가? |

로컬 rollback은 이미 성공한 외부 HTTP 작업을 취소하지 않는다. 긴 외부 작업을 자료 조회·호출·결과 저장으로 나누면 그 사이 데이터가 바뀔 수 있음도 고려한다.

## 7.6 선택 기준

| 필요한 것 | 패턴 |
|---|---|
| 외부 목록을 현재 응답에서만 표시 | A: HTTP DTO → 응답 DTO |
| 로컬 목록에 외부 이름 보강 | B: id 수집 → 일괄 읽기 → Map 결합 |
| 당시 외부 값을 기록·재사용 | C: DTO → Entity에 복사 → 저장 |
| 외부 변경을 계속 받아 로컬에서 조회 | 다음 장의 이벤트 수신·적재 |

어떤 패턴이든 **네트워크 실행 줄, DTO 생성 줄, Entity 변경 줄, commit 경계**를 따로 표시할 수 있어야 한다.

## 7.7 직접 수정해 보기: API의 새 필드가 로컬 조회에는 나타나지 않는다

**아래는 수정 연습용 가상 결함이며 실제 서비스의 결함 보고가 아니다.** 외부 응답에 `item_label`이 추가되었다. 요구사항은 “동기화하면 이 표시값을 로컬에 보관해 이후 GET에 반환한다”다. 연습에서는 로컬 `displayLabel` 컬럼·Entity 프로퍼티·조회 select가 이미 있다고 가정한다. 실제로 없다면 저장 모델·마이그레이션·조회 변경도 별도 수정 범위다.

외부 JSON은 `{ "item_id": "A", "item_name": "Alpha", "item_label": "추천" }`이다. 로컬에는 기존 표시값 `기본`이 저장되어 있다.

### 값이 사라진 경계를 찾는다

| 중단점 | 기대 | 가상 결함에서 관측한 것 |
|---|---|---|
| HTTP `body(...)` 반환 | `RemoteRefreshItem.label = 추천` | `추천`으로 정상 바인딩 |
| 외부 DTO → 쓰기 입력 | `displayLabel = 추천` | 입력 타입에 필드 없음 |
| Entity 변경 직후 | `displayLabel = 추천` | 기존 `기본` 유지 |
| commit 뒤 별도 GET | `추천` | `기본` |

`body(...)` 직후에도 null이라면 먼저 `@JsonProperty("item_label")`와 외부 계약을 확인한다. 이 사례는 DTO 바인딩은 성공했으므로 **그 다음 명시적 복사 경계**를 고친다. 응답 JSON 키나 조회 select를 임의로 바꾸는 작업이 아니다.

### 수정 전: 새 값이 전달 입력에서 빠진다

```kotlin
// 이 실습의 별도 원격 계약: name은 필수, label은 선택이다.
data class RemoteRefreshItem(
    @JsonProperty("item_id") val id: String,
    @JsonProperty("item_name") val name: String,
    @JsonProperty("item_label") val label: String? = null,
)
data class RefreshItemInput(val name: String)
fun RemoteRefreshItem.toInput() = RefreshItemInput(name = name)
// 기존 writer의 tx 안
item.changeName(input.name)
repository.save(item)
```

### 수정 후: 입력과 도메인 대입까지 값을 연결한다

```kotlin
// 설명용: 이 연습의 필드 정책은 null=미제공(유지), 빈 문자열=빈값으로 교체다.
data class RefreshItemInput(val name: String, val displayLabel: String?)
fun RemoteRefreshItem.toInput() = RefreshItemInput(name, label)

// 기존 writer의 tx 안에서, scope로 읽은 managed Entity를 변경한다.
item.updateFromSource(input.name, input.displayLabel)
repository.save(item)

// Item 내부의 업무 메서드: 필드 규칙은 한곳에 둔다.
fun updateFromSource(name: String, displayLabel: String?) {
    changeName(name)
    if (displayLabel != null) this.displayLabel = displayLabel
}
```

외부 wire 이름은 `RemoteRefreshItem`, 내부 입력은 `RefreshItemInput`, 영속 상태 규칙은 `Item`이 맡는다. Controller에서 Entity 필드를 바로 대입하거나 전역 JSON naming 설정을 바꾸지 않는다. HTTP 호출·writer의 tx 배치는 유지한다. 여기서 정한 null 정책은 교육용 요구사항이며 실제 계약이 “null로 삭제”라면 그에 맞춰 달라져야 한다.

### 입력과 기대값으로 회귀 확인하기

| 외부 입력 / 선행 상태 | 동기화 후 새 GET의 기대값 |
|---|---|
| `item_label=추천`, 기존 `기본` | `추천` |
| `item_label=null`, 기존 `기본` | `기본` 유지 |
| `item_label=""`, 기존 `기본` | 빈 문자열 |
| 정상 payload지만 로컬 저장 tx 실패 | 기존 로컬 값 유지 |
| 다른 tenant 범위의 id | 범위 밖 데이터 변경 없음 |

검증은 DTO 역직렬화 → `toInput` → 도메인 필드 규칙 → commit 뒤 조회로 나눈다. 원격 호출을 mock한 테스트에서 새 필드를 응답 fixture에 넣고도 mapper를 빠뜨렸을 때 실패하는지 본다. 조회 응답에 값을 하드코딩하면 이 회귀를 잡지 못한다. 위 표는 실행 계획이다.

### 다른 호출에 미치는 영향

같은 입력·도메인 메서드를 사용하는 수동 입력·이벤트 처리·재시도 경로를 찾는다. 원격 필드가 없는 기존 응답의 처리, 사용자 입력값을 원천값이 덮어써도 되는지, 누락과 null을 구별해야 하는지는 계약 질문이다. 이번 예제의 정책을 다른 경로에 자동 확대하지 않는다.

수정 범위를 정하는 절차는 [직접 수정하기 위한 공부 방법](09-studying-to-change-code.md)에서 이어간다.

## 실제 소스에서 같은 패턴 찾기

| 패턴 | 근거 파일·심볼 |
|---|---|
| HTTP JSON → Payload → 표시 DTO | [DepartmentHttpAdapter.findOutpatientDepartments](../../../x-patient-appointment/backend/patient-appointment/src/main/kotlin/com/projectx/patientappointment/infrastructure/proxies/DepartmentHttpAdapter.kt), [DepartmentPayload.toView](../../../x-patient-appointment/backend/patient-appointment/src/main/kotlin/com/projectx/patientappointment/infrastructure/proxies/DepartmentPayload.kt) |
| 외부 초기값을 받아 DTO 조립 | [SupplierInitializationClient](../../../x-clinical-care/backend/ordering/src/main/kotlin/com/projectx/ordering/application/initialization/SupplierInitializationClient.kt) |
| 외부 값을 생성 입력에 복사 | [OutpatientScheduleService.create](../../../x-patient-appointment/backend/patient-appointment/src/main/kotlin/com/projectx/patientappointment/application/services/OutpatientScheduleService.kt) |
| 외부 응답 뒤 짧은 저장 tx | [AnalyzeObservationsByPatientCommandHandler.handle](../../../x-diagnostic-result/backend/diagnostic-result/src/main/kotlin/com/projectx/diagnosticresult/application/commands/observationanalyzeresult/AnalyzeObservationsByPatientCommandHandler.kt), [ObservationAnalyzeResultSaveService](../../../x-diagnostic-result/backend/diagnostic-result/src/main/kotlin/com/projectx/diagnosticresult/application/services/ObservationAnalyzeResultSaveService.kt) |

B의 HTTP 묶음 조회는 계약이 지원할 때의 설명용 패턴이다. 로컬 DB의 id 묶음 보강은 [ObservationQueries.findUserNames](../../../x-diagnostic-result/backend/diagnostic-result/src/main/kotlin/com/projectx/diagnosticresult/infrastructure/queries/ObservationQueries.kt)에서 확인한다. 수단이 달라도 id 수집→Map→응답 조립의 형태는 비교할 수 있다.

[이전: 조회 후 변경·커밋](06-read-change-commit.md) · [목차](README.md) · [다음: 이벤트로 데이터 반영](08-event-data-patterns.md)
