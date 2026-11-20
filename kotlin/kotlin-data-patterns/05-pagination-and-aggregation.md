# 5. 단건·목록·페이징·집계를 읽는 패턴

[이전: 조인과 일괄 조회](04-join-and-batch-patterns.md) · [목차](README.md) · [다음: 조회 후 변경·커밋](06-read-change-commit.md)

조회 결과의 **모양과 크기**는 별개다. 같은 `ItemView`를 만들더라도 한 건만 찾는지, 20건을 가져오는지, 그룹당 한 행을 만드는지에 따라 쿼리가 달라진다. 이 장은 “어디서 결과 수를 줄이고, 무엇을 한 건으로 세는가?”를 설명한다.

## 5.1 공통 전제

코드는 설명용 `Item`, `Detail`의 QueryDSL JPA 예제다. C#은 EF Core의 `IQueryable` 비교이며 import·매핑·DI 설정은 생략했다. 조회 조건에는 아래의 필수 범위를 유지한다.

```csharp
var scoped = db.Items.Where(x =>
    x.TenantId == tenantId && x.HospitalCode == hospitalCode);
```

```kotlin
val i = QItem.item
val d = QDetail.detail
val scopePredicate = i.tenantId.eq(scope.tenantId)
    .and(i.hospitalCode.eq(scope.hospitalCode))
```

A·B 등 표의 id는 UUID를 짧게 표시한 별칭이다. `Detail`은 `Item` 소유의 자식 행이고 `itemId`로 연결된다고 가정한다.

## 5.2 패턴 A: 한 건·첫 건·목록·존재 여부를 다르게 읽는다

| 질문 | C#의 의미 비교 | QueryDSL 패턴 | 결과가 없으면 |
|---|---|---|---|
| 조건에 맞는 목록 | `ToListAsync()` | `fetch()` | 빈 목록 |
| 최대 한 건이어야 함 | `SingleOrDefaultAsync()` | `fetchOne()` | Entity 조회라면 null |
| 정렬상 첫 한 건 | `FirstOrDefaultAsync()` | `orderBy(...).fetchFirst()` | null |
| 몇 건인가 | `LongCountAsync()` | `select(i.id.count()).fetchOne()` | 집계값 0 |
| 하나라도 있는가 | `AnyAsync()` | `selectOne()...fetchFirst() != null` | false |

이 표는 의미 대응이며 생성되는 SQL이 똑같다는 뜻은 아니다. `fetchOne()`은 보통 0건·1건을 기대하는 호출이고 여러 결과면 오류다. `fetchFirst()`는 첫 결과만 읽으므로 **중복이 있어서는 안 되는 조건의 오류를 숨길 수 있다.** 유일성이 업무 요구이면 DB의 UNIQUE 제약도 함께 필요하다.

```kotlin
// 설명용: 해당 id의 Entity. 단순 단일 테이블 조회다.
val item: Item? = queryFactory.selectFrom(i)
    .where(scopePredicate, i.id.eq(itemId))
    .fetchOne()

// 설명용: 가장 최근 한 건. 같은 시각이면 id로 순서를 고정한다.
val latest: Item? = queryFactory.selectFrom(i)
    .where(scopePredicate)
    .orderBy(i.createdAt.desc(), i.id.desc())
    .fetchFirst()

// 설명용: 존재 확인에 Entity 전체를 로딩하지 않는다.
val exists: Boolean = queryFactory.selectOne().from(i)
    .where(scopePredicate, i.active.isTrue)
    .fetchFirst() != null
```

`selectOne()`은 숫자 1을 선택하는 쿼리를 구성한다. `fetchFirst()`에서 실행하고, 조건에 맞는 행이 있으면 1, 없으면 null을 받아 Boolean으로 바꾼다. QueryDSL의 서브쿼리 `exists()` 표현식과 이 최상위 조회 패턴은 사용 위치가 다르다.

SQL 정렬을 지정하지 않은 “첫 건”은 가장 오래된 행·최신 행·작은 id를 의미하지 않는다. 우연히 지금 그렇게 보이더라도 순서 계약은 `orderBy`에 써야 한다.

## 5.3 패턴 B: DB에서 정렬하고 페이지 크기만 가져온다

**문제:** 목록 전체가 10만 건이지만 요청은 두 번째 페이지 20건이다. 전체를 읽어 메모리에서 자르면 10만 건의 전송·객체 생성 비용이 먼저 발생한다.

이 예제의 `pageIndex`는 **0-based**, `pageSize`는 양수이며 입력 범위·최대 크기 검증이 끝났다고 가정한다. 외부 API가 1-based라면 경계에서 변환한다.

```csharp
var page = await scoped.Where(x => x.Active)
    .OrderByDescending(x => x.CreatedAt).ThenByDescending(x => x.Id)
    .Skip(checked(pageIndex * pageSize)).Take(pageSize)
    .Select(x => new ItemView(x.Id, x.Name, x.Amount))
    .ToListAsync();
```

```kotlin
val offset = pageIndex.toLong() * pageSize
val page: List<ItemView> = queryFactory
    .select(Projections.constructor(ItemView::class.java, i.id, i.name, i.amount))
    .from(i)
    .where(scopePredicate, i.active.isTrue)
    .orderBy(i.createdAt.desc(), i.id.desc())
    .offset(offset)
    .limit(pageSize.toLong())
    .fetch()
```

| 부분 | 의미 |
|---|---|
| `where` | 페이지를 자를 전체 후보 집합 |
| `orderBy` | 전체 후보의 순서 |
| `offset` | 앞에서 건너뛸 행 수 |
| `limit` | 최대 반환 행 수 |
| `fetch` | 위 조건으로 DB에서 실행 |

예를 들어 정렬 결과가 `[A, B, C, D, E]`, `pageIndex=1`, `pageSize=2`이면 `offset=2`이므로 `[C, D]`를 받는다. offset 계산 전에 `toLong()`으로 바꾸어 Int 곱셈의 오버플로를 피한다.

`createdAt`이 같은 행이 여러 개면 이것만으로 순서가 완전히 결정되지 않는다. 유일 키인 id를 마지막 정렬 기준으로 붙인다. EF Core 공식 문서도 페이징에서 완전히 유일한 정렬을 요구한다. [페이징과 정렬 기준](https://learn.microsoft.com/en-us/ef/core/querying/pagination).

안정적인 정렬을 넣어도 **페이지 요청 사이의 삽입·삭제까지 고정되는 것은 아니다.** offset 방식은 앞쪽 행이 달라지면 중복·건너뜀을 보일 수 있고 깊은 페이지에서는 앞 행을 건너뛰는 비용이 커진다. 다음 항목을 계속 읽는 용도라면 마지막 정렬값을 조건으로 넘기는 커서 패턴도 검토한다. 프로젝트 소스에 offset과 cursor 방식이 모두 있으므로 호출 계약을 먼저 읽는다.

### 정렬 필드가 입력값일 때

사용자가 `sort=name` 또는 `sort=createdAt`을 보낸다면 **허용한 입력을 Q필드에 대응**시킨다. 다음은 오름차순만 다룬 설명용 코드다.

```kotlin
val primaryOrder = when (sortKey) {
    "name" -> i.name.asc()
    "createdAt" -> i.createdAt.asc()
    else -> throw invalidSortField()
}
val rows = queryFactory.selectFrom(i)
    .where(scopePredicate)
    .orderBy(primaryOrder, i.id.asc())
    .offset(offset)
    .limit(pageSize.toLong())
    .fetch()
```

C#도 `sortKey`를 `switch`로 나눠 `OrderBy(x => x.Name)` 또는 `OrderBy(x => x.CreatedAt)`을 선택하는 의도다. `offset`은 앞 예제의 계산값이고 `invalidSortField()`는 입력 오류를 만드는 설명용 함수다. 이 예제의 기본 정렬은 요청 경계에서 정했다고 가정한다. 필드명은 SQL의 값 파라미터로 대신할 수 없으므로 사용자 문자열을 쿼리 조각으로 붙이지 않는다. 실제 적용 시에는 프로젝트의 정렬 파서·오류 계약을 따른다.

## 5.4 패턴 C: 페이지 데이터와 전체 건수를 따로 조회한다

페이지 20건을 가져온 뒤 `page.size`를 세면 **현재 페이지의 건수**다. 전체 후보가 몇 건인지는 다른 질문이다.

```kotlin
val filter = scopePredicate.and(i.active.isTrue)

// 본문 조회도 같은 filter를 사용한다. 페이지 쿼리는 앞 절과 같다.
val total: Long = queryFactory.select(i.id.count())
    .from(i)
    .where(filter)
    .fetchOne() ?: 0L
```

C#에서는 `await scoped.Where(x => x.Active).LongCountAsync()`에 해당한다. 전체 건수 쿼리에는 페이지용 offset·limit을 붙이지 않는다. 본문은 active 조건을 넣고 count에는 빼면 “마지막 페이지인데 목록이 비어 있음” 같은 불일치가 생긴다.

| 전체 후보 | 현재 페이지 항목 | `page.size` | `total` |
|---:|---|---:|---:|
| 5 | C, D | 2 | 5 |
| 5 | E | 1 | 5 |
| 0 | 없음 | 0 | 0 |

두 조회 사이에 데이터가 바뀔 수 있다. 같은 `readOnly` 트랜잭션이라고 반드시 같은 시점의 스냅샷을 보는 것도 아니다. 정확히 같은 시점의 본문·건수가 필요하면 실제 DB 격리 수준과 요구를 함께 검토한다.

### 1:N 조인에서는 무엇을 세는지 먼저 정한다

| 조인 결과 | item id | detail id |
|---|---|---|
| 1행 | A | D1 |
| 2행 | A | D2 |
| 3행 | B | D3 |

이 결과에서 행 수는 3이지만 Item 수는 2다.

```kotlin
// 설명용: Detail이 있는 Item 수를 센다.
val totalItems = queryFactory.select(i.id.countDistinct())
    .from(i)
    .join(d).on(d.itemId.eq(i.id))
    .where(scopePredicate)
    .fetchOne() ?: 0L
```

C#의 `Join(...).Select(x => x.ItemId).Distinct().LongCountAsync()`와 같은 목적이다. 위 코드는 자식이 있는 Item만 세며, 자식이 없는 Item까지 포함할 전체 건수와는 다르다. **본문과 count의 조인·필터·세는 단위가 같아야 한다.** 모든 count에 습관적으로 distinct를 붙이는 것이 해결책은 아니다.

## 5.5 패턴 D: 부모 페이지를 먼저 정하고 자식을 묶어서 읽는다

**문제:** “Item 2개와 각 Item의 모든 Detail”이 필요하다. 1:N 조인한 행에 `limit(2)`를 주면 Item 두 개가 아니라 조인 행 두 개를 자를 수 있다. A의 자식 두 개만으로 페이지가 채워지거나 한 Item의 자식이 중간에 잘릴 수 있다.

```text
1차 쿼리: Item에 조건·정렬·offset·limit 적용 → 부모 페이지 [A, B]
2차 쿼리: Detail where itemId IN [A, B] → 자식 전부
메모리: 자식을 itemId로 groupBy → 부모 페이지 순서대로 DTO 조립
```

```kotlin
// parentPage는 이미 DB에서 페이지를 자른 ItemView 목록이다.
val ids = parentPage.map { it.id }
val details = if (ids.isEmpty()) emptyList() else queryFactory
    .selectFrom(d)
    .where(d.itemId.`in`(ids))
    .orderBy(d.itemId.asc(), d.id.asc())
    .fetch()
val detailsByItem = details.groupBy { it.itemId }
val result = parentPage.map { parent ->
    ItemWithDetailsView(parent, detailsByItem[parent.id].orEmpty().map { it.label })
}
```

`ItemWithDetailsView`는 `item: ItemView`, `labels: List<String>`를 가진 설명용 DTO다. C#에서도 부모 페이지의 id를 구하고 `ids.Contains(detail.ItemId)`로 자식을 한 번에 읽은 뒤 `GroupBy`·사전을 구성하는 목적은 같다.

`IN` 결과의 순서가 ids 입력 순서와 같다고 기대하지 않는다. 최종 결과는 **이미 정렬된 parentPage를 기준으로** 만든다. 이 예제의 자식 쿼리는 앞서 scope 검증한 부모 id 목록으로 제한하며, 독립적인 외부 id 목록을 무검증으로 받는 조회와 구분한다. 여러 쿼리를 쓴 만큼 동시 변경 시점의 일관성 요구도 확인한다.

## 5.6 패턴 E: DB 집계와 메모리 그룹화를 구분한다

**문제:** 분류별 건수·합계만 필요한데 모든 Item을 읽어 애플리케이션에서 합산하면 비용이 크다. DB에서 집계하면 그룹당 결과 한 행만 가져올 수 있다.

```csharp
var summaries = await scoped.GroupBy(x => x.CategoryId)
    .Select(g => new { CategoryId = g.Key, Count = g.LongCount(), Total = g.Sum(x => x.Amount) })
    .ToListAsync();
```

```kotlin
val itemCount = i.id.count()
val totalAmount = i.amount.sum()
val rows: List<Tuple> = queryFactory
    .select(i.categoryId, itemCount, totalAmount)
    .from(i)
    .where(scopePredicate)
    .groupBy(i.categoryId)
    .fetch()
```

| 원본 입력 | DB 집계 결과 |
|---|---|
| K1: 500, NULL / K2: 300 | K1: count=2, sum=500 / K2: count=1, sum=300 |

`count(i.id)`는 NULL이 아닌 id의 수이고 `sum(amount)`는 NULL을 제외해 합산한다. 어떤 그룹의 금액이 모두 NULL이면 SQL SUM 결과는 NULL일 수 있다. EF provider가 만들어 주는 null 보정과 무조건 같다고 가정하지 말고, 응답의 0/null 정책을 직접 정한다. 집계값은 `row.get(itemCount)`, `row.get(totalAmount)`로 꺼낸다.

다음 코드는 이름이 비슷하지만 **DB 집계가 아니다.**

```kotlin
val items = queryFactory.selectFrom(i).where(scopePredicate).fetch()
val groups: Map<UUID?, List<Item>> = items.groupBy { it.categoryId }
```

여기서는 모든 Item을 이미 전송·로딩한 뒤 Kotlin이 목록을 나눈다. 그룹별 세부 항목 목록이 필요하거나 이미 제한된 소량 데이터를 가공할 때 유용하다. SQL의 `groupBy(...)`, Kotlin의 `groupBy { ... }`, QueryDSL의 `transform(GroupBy.groupBy(...))`는 결과 형태와 처리 위치를 구분해서 읽는다. 마지막 방식은 조회 행을 메모리에서 묶는 결과 변환 패턴이다.

## 5.7 fetch 앞뒤를 바꾸면 결과와 비용이 바뀐다

```kotlin
// A: DB가 필터링한 결과만 가져온다.
val active = queryFactory.selectFrom(i)
    .where(scopePredicate, i.active.isTrue).fetch()

// B: scope의 모든 행을 먼저 가져오고 메모리에서 버린다.
val activeInMemory = queryFactory.selectFrom(i)
    .where(scopePredicate).fetch().filter { it.active }
```

A·B가 같은 항목을 만들 수 있어도 B는 더 많은 전송·객체 생성 비용을 쓴다. 특히 **먼저 pageSize만 fetch한 후 filter하면**, “조건에 맞는 항목 pageSize개”가 아니라 “원래 페이지 중 조건에 맞는 나머지”가 된다.

C#에서도 `IQueryable.Where`와 `ToListAsync` 뒤의 `Enumerable.Where`를 구분한다. DB provider가 번역하는 범위와 클라이언트 계산의 경계는 [EF Core 서버·클라이언트 평가 설명](https://learn.microsoft.com/en-us/ef/core/querying/client-eval)을 참고한다. Kotlin에서는 일반 `List.filter`가 SQL 조건으로 되돌아가지 않는다.

## 5.8 직접 수정해 보기: 목록은 줄었는데 전체 건수는 그대로다

다음은 **교육용으로 만든 결함**이며 실제 레포의 버그를 지적하는 코드가 아니다. 새 요구 “활성·비활성·전체 선택”을 목록에 적용했더니, 활성 결과는 세 건인데 화면에는 총 다섯 건·세 페이지라고 나온다. `pageSize=2`, `pageIndex=0`부터 시작한다.

### 먼저 원인을 예상하고 관측한다

내 scope의 id 정렬 순서가 A1·A2·A3·B1·B2이고 A는 활성, B는 비활성이라고 하자. 다른 scope에는 활성 X1도 있다. 다음 코드를 읽고 `active=true`일 때 `rows`, `total`, 페이지 수를 각각 적어 본다.

```kotlin
// 잘못된 설명용 코드: 새 조건이 rows에만 추가되었다.
val rows = queryFactory.select(i.id, i.name).from(i)
    .where(scopePredicate, active?.let { i.active.eq(it) })
    .orderBy(i.id.asc())
    .offset(pageIndex.toLong() * pageSize).limit(pageSize.toLong())
    .fetch()
val total = queryFactory.select(i.id.count()).from(i)
    .where(scopePredicate)
    .fetchOne() ?: 0L
```

`rows`의 fetch 직후와 `total`의 fetchOne 직후에 중단점을 둔다. 첫 페이지의 id는 `[A1, A2]`이고 total은 5다. 페이지 크기 2로 계산하면 3페이지지만 활성 조건의 마지막 페이지에는 데이터가 없다. 응답의 total이 count 값 그대로라면, 페이지 수 계산식보다 **본문과 count가 같은 후보 집합을 보고 있는지**를 먼저 비교한다.

안전한 개발 데이터로 실행할 때 두 쿼리의 조건·바인딩 값을 확인한다. rows에는 active 조건이 있고 count에는 없는 것을 찾아야 한다. 관측 없이 “페이지 계산이 하나 많다”며 totalPages만 줄이면 다른 필터에서 다시 틀린다.

### 공통 조건 함수로 고친다

이 예제에서 `null=전체`, `true=활성`, `false=비활성`이다. Queries 클래스의 `i = QItem.item`을 사용하는 설명용 함수를 만든다. 함수는 호출마다 표현식을 반환하며 변경 가능한 공용 builder를 보관하지 않는다.

```kotlin
private fun itemFilter(scope: TenantScope, active: Boolean?): BooleanExpression {
    val required = i.tenantId.eq(scope.tenantId)
        .and(i.hospitalCode.eq(scope.hospitalCode))
    return active?.let { required.and(i.active.eq(it)) } ?: required
}
```

그다음 두 조회의 WHERE를 같은 함수에서 만든 조건으로 연결한다.

```diff
+val filter = itemFilter(scope, active)
 val rows = queryFactory.select(i.id, i.name).from(i)
-    .where(scopePredicate, active?.let { i.active.eq(it) })
+    .where(filter)
     .orderBy(i.id.asc())
     .offset(pageIndex.toLong() * pageSize).limit(pageSize.toLong())
     .fetch()
 val total = queryFactory.select(i.id.count()).from(i)
-    .where(scopePredicate)
+    .where(filter)
     .fetchOne() ?: 0L
```

고치는 위치는 응답 DTO가 아니라 **두 쿼리의 공통 필터**다. count에 offset·limit을 복사하지 않는다. 공유하는 것은 조건 표현식이며, 페이지 설정으로 변경된 쿼리 객체 자체를 count에 재사용하지 않는다. C#에서 공통 `IQueryable` 후보에 각각 `Skip/Take`와 `LongCount`를 적용하던 의도를 떠올리면 된다.

### 회귀 입력과 기대 결과

| 입력 | rows의 id | total | 페이지 수(size=2) |
|---|---|---:|---:|
| active=true, page=0 | A1, A2 | 3 | 2 |
| active=true, page=1 | A3 | 3 | 2 |
| active=false, page=0 | B1, B2 | 2 | 1 |
| active=null, page=0 | A1, A2 | 5 | 3 |
| 별도 fixture: 내 scope가 모두 비활성, active=true | 없음 | 0 | 0 |
| 원본에 다른 scope의 활성 행 추가, active=true | A1, A2 | 3 | 2 |

표의 페이지 수 0은 전체 건수 0일 때의 이 예제 계약이다. 실제 API 계약이 다르면 맞춘다. 특히 false 입력을 확인해야 `if (active == true)`로 바꿔 비활성 검색을 놓치는 회귀를 잡을 수 있다. 데이터 변경이 없는 fixture에서 결과를 대조하고, 실제 운영의 두 조회 사이 동시 변경 문제와 구분한다.

### 다른 호출과 새 조건을 함께 점검한다

`itemFilter`를 쓰는 함수의 사용처를 찾는다. 다운로드·건수 API·선택 목록이 같은 조건을 원하면 공유할 수 있지만, “비활성도 항상 포함”하는 별도 호출에 새 기본값을 강제하면 기능이 바뀐다. 현재 인자의 null 의미와 각 호출자가 넘기는 값을 확인한다. 이후 검색어·분류 조건을 추가할 때는 **공통 조건과 rows/count의 회귀 입력을 함께** 확장한다.

**추가 연습:** 다음 요구가 “특정 Detail label이 있는 Item만”이면 count의 `countDistinct`만 바꾸면 충분할까?

<details>
<summary>생각한 뒤 비교할 답</summary>

충분하지 않다. 먼저 부모를 고르는 조건을 rows와 count에 똑같이 넣어야 한다. EXISTS를 공통 predicate에 넣으면 부모 기준 집합을 유지할 수 있다. JOIN이 필요한 다른 이유가 있다면 count가 조인 행 수인지 부모 수인지 별도 검토한다. distinct는 누락된 필터를 대신하지 않는다.

</details>

## 실제 코드에서 이 패턴 찾기

| 패턴 | 소스와 찾을 부분 |
|---|---|
| 안정 정렬·offset·별도 count | [ObservationAnalyzeResultQueries](../../../x-diagnostic-result/backend/diagnostic-result/src/main/kotlin/com/projectx/diagnosticresult/infrastructure/queries/ObservationAnalyzeResultQueries.kt)의 `searchByPatient`, `patientHistoryQuery`, `patientHistoryCountQuery` |
| 그룹 목록과 countDistinct | [ObservationQueries](../../../x-diagnostic-result/backend/diagnostic-result/src/main/kotlin/com/projectx/diagnosticresult/infrastructure/queries/ObservationQueries.kt)의 `patientListQuery`, `patientCountQuery`: 세는 단위가 환자 id |
| DB groupBy·cursor | [InboxMessageQueries](../../../x-diagnostic-result/backend/diagnostic-result/src/main/kotlin/com/projectx/diagnosticresult/infrastructure/queries/InboxMessageQueries.kt)의 `summaryQuery`, `rowsQuery` |
| 부모 목록 후 자식 일괄 조회 | [ViewerConfigurationQueries](../../../x-diagnostic-result/backend/diagnostic-result/src/main/kotlin/com/projectx/diagnosticresult/infrastructure/queries/ViewerConfigurationQueries.kt)의 `sets` → `setIds` → `itemsBySet`: 실제 예는 페이징 없는 목록이며 id 일괄 조회 원리가 같음 |

[이전: 조인과 일괄 조회](04-join-and-batch-patterns.md) · [목차](README.md) · [다음: 조회 후 변경·커밋](06-read-change-commit.md)
