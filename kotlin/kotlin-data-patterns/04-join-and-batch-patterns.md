# 4. JOIN·존재 확인·일괄 조회로 데이터를 합치는 패턴

[이전: 결과 바인딩](03-result-binding-patterns.md) · [목차](README.md) · [다음: 페이징·집계](05-pagination-and-aggregation.md)

목록에 이름을 붙이거나 하위 목록을 넣을 때 선택하는 패턴이다. 먼저 **부모 한 건당 결과를 한 줄로 유지할지, 자식 수만큼 펼칠지**를 결정한다. JOIN은 데이터 결합 방법이며 항상 가장 빠르거나 항상 한 객체를 반환하는 장치가 아니다.

설명용 모델은 `Item(id, name, categoryId, …)`, `Category(id, name)`, `Detail(id, itemId, label)`이다. `q`, `c`, `d`는 각각 Q클래스의 쿼리 경로다. Item은 `scopeCondition(scope)`로 제한한다. Category는 이 조회에서 사용하도록 허용된 참조 데이터, Detail은 선택된 Item에 속한 자식이라고 가정한다. 실제 다른 서비스 테이블을 임의로 조인하라는 예제가 아니다. 코드의 Entity·DTO·import·DI 선언은 생략했다.

## 4.1 패턴: 다른 테이블의 한 값을 같은 줄에 붙인다

**문제:** Item에는 `categoryId`만 있고 응답에는 `categoryName`도 필요하다. Category의 id가 유일하다면 JOIN으로 한 줄에 붙일 수 있다.

C#의 선택적 결합:

```csharp
var rows = (
    from item in db.Items
    where item.TenantId == scope.TenantId &&
          item.HospitalCode == scope.HospitalCode
    from category in db.Categories
        .Where(x => x.Id == item.CategoryId).DefaultIfEmpty()
    select new {
        item.Id, item.Name,
        CategoryName = category == null ? null : category.Name
    }).ToList();
```

QueryDSL의 같은 형태:

```kotlin
val rows = queryFactory.select(q.id, q.name, c.name)          // ①
    .from(q)                                               // ②
    .leftJoin(c).on(q.categoryId.eq(c.id))                   // ③
    .where(scopeCondition(scope))                          // ④
    .fetch()                                               // ⑤
val views = rows.map { row ->                               // ⑥
    ItemCategoryView(row.get(q.id)!!, row.get(q.name)!!, row.get(c.name))
}
```

| 줄 | 의미 |
|---|---|
| ① | 부모 id·이름과 참조 이름만 선택한다 |
| ② | 결과의 기준 행은 Item이다 |
| ③ | Category가 없어도 부모를 남기는 LEFT JOIN을 만든다 |
| ④ | 허용된 부모 범위를 제한한다 |
| ⑤ | DB가 조인 결과를 만들고 애플리케이션이 Tuple 목록을 받는다 |
| ⑥ | 이미 받은 값을 응답 객체 생성자에 넣는다. 새 SQL이 아니다 |

| Item | 연결 Category | 결과 categoryName |
|---|---|---|
| I1 → C1 | C1: 공통 | 공통 |
| I2 → null | 없음 | null, I2는 남음 |
| I3 → C9 | 대응 행 없음 | null, I3는 남음 |

`join`(INNER JOIN)을 사용하면 대응 Category가 없는 I2·I3는 제외된다. **연결 실패를 허용하는지**가 선택 기준이다. 이 예제의 관련 없는 Entity JOIN+ON은 프로젝트의 QueryDSL/Hibernate 사용 형태를 따른 설명이며 모든 JPA 제공자에서 동일한 지원을 전제하지 않는다.

## 4.2 패턴: LEFT JOIN의 조건을 ON과 WHERE 중 어디에 둘까

**문제:** 이름이 “A”로 시작하는 Category만 붙이되 Category가 없는 Item도 보고 싶다.

```kotlin
// 부모는 남기고 조건에 맞는 참조 이름만 붙임
val rows = queryFactory.select(q.id, c.name).from(q)
    .leftJoin(c).on(q.categoryId.eq(c.id).and(c.name.startsWith("A")))
    .where(scopeCondition(scope))
    .fetch()
```

다음처럼 조건을 WHERE로 옮기면 의미가 달라진다.

```kotlin
// 조인 결과 자체를 거르므로 Category가 없는 부모는 제외됨
val rows = queryFactory.select(q.id, c.name).from(q)
    .leftJoin(c).on(q.categoryId.eq(c.id))
    .where(scopeCondition(scope), c.name.startsWith("A"))
    .fetch()
```

| Item의 Category 이름 | ON에 조건 | WHERE에 조건 |
|---|---|---|
| Alpha | 부모 포함, Alpha 표시 | 부모 포함, Alpha 표시 |
| Beta | 부모 포함, 이름 null | 부모 제외 |
| Category 없음 | 부모 포함, 이름 null | 부모 제외 |

C#에서도 `DefaultIfEmpty()` 이전의 참조 후보 조건과 조인 후 `where category.Name...` 조건은 목적이 다르다. **ON은 어떤 참조 행을 붙일지, WHERE는 만들어진 결과 행을 남길지**를 읽는 출발점으로 삼는다.

## 4.3 패턴: 참조값이 필요 없고 존재 여부만 확인한다

**문제:** label이 “필수”인 Detail이 하나라도 있는 Item만 조회한다. 자식 컬럼을 응답에 넣지는 않는다. 이때는 EXISTS가 결과 의도를 잘 표현한다.

```csharp
var rows = db.Items.Where(item =>
    item.TenantId == scope.TenantId && item.HospitalCode == scope.HospitalCode &&
    db.Details.Any(detail => detail.ItemId == item.Id && detail.Label == "필수"))
    .ToList();
```

```kotlin
val hasRequiredDetail = JPAExpressions.selectOne().from(d)
    .where(d.itemId.eq(q.id), d.label.eq("필수"))
    .exists()
val rows = queryFactory.selectFrom(q)
    .where(scopeCondition(scope), hasRequiredDetail)
    .fetch()
```

`d.itemId.eq(q.id)`가 바깥 부모와 안쪽 자식을 연결한다. Detail이 1개든 5개든 조건은 “존재함” 하나이므로 **자식 수 때문에 부모 행이 늘어나지 않는다.** 두 쿼리 객체가 보이지만 별도의 두 번 조회가 아니라 최종 쿼리 안의 서브쿼리다. 실행은 바깥 `fetch()`에서 일어난다.

동일 목적을 ``q.id.`in`(JPAExpressions.select(d.itemId)...)`` 형태의 서브쿼리로 표현할 수도 있다. 특정 형태가 언제나 빠르다고 단정하지 말고 **존재 조건인지, 연결값이 필요한지**를 먼저 판단한 뒤 데이터량·인덱스·실행계획으로 확인한다.

## 4.4 패턴: 1:N 조인 결과를 부모 한 건으로 묶는다

**문제:** Item에 Detail 목록을 붙이면 SQL 결과는 평평한 여러 행이고 응답은 중첩된 한 객체여야 한다.

```text
DB 조회 결과:                     원하는 응답:
I1, 항목1, D1, 빨강              I1 → [D1 빨강, D2 파랑]
I1, 항목1, D2, 파랑              I2 → []
I2, 항목2, null, null
```

C#에서는 `ToList()` 뒤 `GroupBy(x => x.ItemId)`로 묶던 상황이다. Kotlin에서는 다음처럼 읽는다.

```kotlin
val rows = queryFactory.select(q.id, q.name, d.id, d.label).from(q)
    .leftJoin(d).on(d.itemId.eq(q.id))
    .where(scopeCondition(scope))
    .orderBy(q.id.asc(), d.id.asc())
    .fetch()                                                     // DB의 평평한 행
val views = rows.groupBy { it.get(q.id)!! }.map { (itemId, group) ->
    val details = group.mapNotNull { row ->
        row.get(d.id)?.let { detailId ->
            DetailView(detailId, row.get(d.label)!!)
        }
    }
    ItemDetailsView(itemId, group.first().get(q.name)!!, details)
}
```

- `fetch`까지가 DB 조회다. 그 뒤 `groupBy`, `mapNotNull`, DTO 생성은 메모리 연산이다.
- `mapNotNull`은 LEFT JOIN의 “자식 없음” 행을 빈 자식 목록으로 바꾸는 역할이다.
- SQL `GROUP BY`와 Kotlin `groupBy`는 다르다. 전자는 DB 집계, 후자는 이미 가져온 행의 그룹화다.
- QueryDSL의 `transform(GroupBy.groupBy(...))`라는 결과 그룹화 도구도 있다. 이것을 SQL 집계나 전송량 감소로 오해하지 않는다.

부모·자식 전체 컬럼에 `distinct`를 붙여도 D1과 D2는 다른 행이라 하나가 되지 않는다. 자식 컬렉션 두 개를 함께 조인하면 자식 A 2개 × 자식 B 3개로 6행이 되는 곱셈도 생긴다. 단순 중복 제거를 붙이기 전에 어떤 결합이 필요한지 다시 본다.

**선택 기준:** 작은 중첩 결과를 한 조회로 구성할 때 이해하기 쉽다. 부모 페이징에 곧바로 `limit`를 붙이면 “부모 수”가 아니라 “펼친 행 수”가 제한될 수 있으므로 다음의 두 번 조회 패턴을 검토한다.

## 4.5 패턴: 필요한 ID를 모아 한 번에 읽고 Map으로 붙인다

**문제:** 부모 페이지는 20건인데 표시 이름이나 자식 데이터를 붙이려고 각 행마다 조회하면 1+20회 호출이 생긴다. 부모를 먼저 읽고 필요한 id들을 모아 한 번에 조회한다.

C#의 `Select(...).Distinct() → Contains로 일괄 조회 → ToDictionary → 응답 조립`에 해당한다.

```kotlin
val items = queryFactory.selectFrom(q)
    .where(scopeCondition(scope))
    .orderBy(q.id.asc()).limit(20).fetch()                         // 조회 1
val categoryIds = items.mapNotNull { it.categoryId }.toSet()       // 메모리
val categories = if (categoryIds.isEmpty()) emptyList() else
    queryFactory.selectFrom(c).where(c.id.`in`(categoryIds)).fetch() // 조회 2
val categoryById = categories.associateBy { it.id }                // 메모리
val views = items.map { item ->
    val category = item.categoryId?.let { categoryById[it] }
    ItemCategoryView(item.id, item.name, category?.name)
}
```

| 단계 | 데이터가 어떻게 바뀌는가 |
|---|---|
| 부모 조회 | I1→C1, I2→C1, I3→C2, I4→null |
| id 수집 | `[C1, C1, C2]` → `{C1, C2}` |
| 참조 일괄 조회 | C1·C2 두 행을 한 번에 읽음 |
| Map 구성 | C1→공통, C2→특수 |
| 최종 조립 | 부모 순서를 유지하며 이름을 붙임. I4는 null |

각 부모의 `categoryById[...]`는 DB가 아니라 메모리 Map 접근이다. 이 예제에서 참조 id가 하나도 없으면 두 번째 DB 조회도 생략한다. 실제 쿼리 수는 Entity의 추가 지연 관계 접근 여부까지 함께 확인한다.

**중복 키 주의:** Kotlin `associateBy`는 같은 키가 여러 번 나오면 뒤 값을 남긴다. C# `ToDictionary`의 중복 키 예외와 다르다. 참조 id가 유일하다는 계약이 없으면 임의의 이름을 고르게 될 수 있다. `groupBy`로 모아 유일성을 검사하거나 “유효일이 가장 최신인 값” 같은 선택 정책을 명시한다. 정렬 없이 “첫 값”·“마지막 값”을 택하면 결과가 안정적이지 않을 수 있다.

자식 목록을 붙일 때는 같은 방식으로 부모 id 묶음을 조회 조건에 넣고 `details.groupBy { it.itemId }`를 만든다. 응답에는 `detailsByItem[item.id].orEmpty()`로 붙인다. 이때 부모의 권한 범위를 통과한 id만 자식 조회에 사용해야 한다.

## 4.6 N+1과 지연 로딩을 읽는 법

다음은 설명용이며 앞의 Item에 Detail 관계가 매핑되어 있다고 가정한 예다.

```kotlin
val items = queryFactory.selectFrom(q).where(scopeCondition(scope)).fetch()
val views = items.map { item ->
    ItemDetailsView(item.id, item.name, item.details.map { DetailView(it.id, it.label) })
}
```

겉으로는 `fetch()`가 한 번이지만 `item.details` 접근이 지연 로딩을 일으키면 부모 수만큼 추가 SQL이 나갈 수 있다. `map` 자체가 DB 함수인 것이 아니라 **그 안에서 읽는 프로퍼티가 ORM 프록시일 수 있다.** 이미 로딩된 상태·배치 로딩 설정에 따라 실제 횟수는 달라지므로 실행 로그로 확인한다.

선택지는 필요한 컬럼 JOIN, 적절한 관계 fetch join, ID 묶음 일괄 조회다. fetch join은 모든 관계에 무조건 붙이는 해결책이 아니며 컬렉션 조인·페이징·행 증식을 함께 고려한다. 여러 쿼리로 나눠 읽으면 첫 조회와 다음 조회 사이 데이터 변경 가능성도 있으므로 필요한 일관성과 트랜잭션 격리 수준을 확인한다.

## 패턴 선택표

| 필요한 결과 | 먼저 고려할 형태 |
|---|---|
| 참조의 한 값을 같은 줄에 표시 | JOIN + 필요한 컬럼 선택 |
| 참조가 없어도 부모 유지 | LEFT JOIN, ON/WHERE 위치 확인 |
| 자식이 있는 부모만 | EXISTS 또는 IN 서브쿼리 |
| 부모마다 작은 자식 목록 | 조인 결과 그룹화 또는 ID 일괄 조회 |
| 부모 페이지를 유지하며 여러 부가 데이터 연결 | 부모 조회 → ID 묶음 조회 → Map/그룹으로 조립 |
| 조회 한 번처럼 보이는데 느림 | fetch 뒤 관계 접근·반복 HTTP·DB 호출 확인 |

## 4.7 직접 수정해 보기: 같은 부모가 두 줄씩 나온다

이 절은 **중립 예제의 의도적인 결함을 고치는 학습 과제**다. 실제 레포에서 발견한 버그라는 뜻이 아니다. 요구는 “필수 Detail이 하나 이상 있는 Item을 id 순서로 두 개 표시”이며, 응답에는 Item의 id·이름만 필요하다.

### 고치기 전에 결과를 예상한다

테스트용 id의 정렬 순서는 I1 → I2 → I3 → I4라고 가정한다.

| Item | 범위 | 연결된 Detail |
|---|---|---|
| I1 | 내 scope | D1: 필수, D2: 필수 |
| I2 | 내 scope | D3: 필수 |
| I3 | 내 scope | D4: 선택 |
| I4 | 내 scope | 없음 |
| X1 | 다른 scope | D5: 필수 |

아래의 **잘못된 설명용 코드**에서 `rows`의 길이, Item id 목록, `distinct()`를 덧붙였을 때의 결과를 먼저 적어 본다. `ItemNameView`는 `id: UUID`, `name: String`을 갖는 DTO다.

```kotlin
val rows = queryFactory.select(q.id, q.name, d.id).from(q)
    .join(d).on(d.itemId.eq(q.id))
    .where(scopeCondition(scope), d.label.eq("필수"))
    .orderBy(q.id.asc(), d.id.asc())
    .limit(2)
    .fetch()
val views = rows.map { row ->
    ItemNameView(row.get(q.id)!!, row.get(q.name)!!)
}
```

`fetch()` 직후에 중단점을 두고 `rows.map { it.get(q.id) to it.get(d.id) }`를 확인한다. 기대 관측값은 `[(I1, D1), (I1, D2)]`다. 다음 DTO 변환 뒤에는 `[I1, I1]`이 된다. **중복이 DB 조인에서 이미 생겼는지, 응답 조립에서 생겼는지**를 이 두 지점으로 나눈다.

선택값에 d.id가 있어 SQL `distinct`를 넣어도 두 행은 서로 다르다. `views.distinctBy { it.id }`로 나중에 지우면 한 개만 남고 I2는 이미 DB의 limit 밖으로 밀렸다. 결과 중복과 페이지 부족을 함께 고쳐야 한다.

### 바꿀 위치를 결정한다

자식 값이 응답에 필요하지 않으므로 자식은 **결합 대상이 아니라 부모를 고르는 조건**이다. DTO나 화면의 중복 제거 코드를 고치기보다 쿼리의 JOIN을 EXISTS로 바꾼다. C#에서 `Join` 결과를 `Distinct`로 줄이기 전에 `Any`로 목적을 표현하는 것과 같다.

```diff
-val rows = queryFactory.select(q.id, q.name, d.id).from(q)
-    .join(d).on(d.itemId.eq(q.id))
-    .where(scopeCondition(scope), d.label.eq("필수"))
-    .orderBy(q.id.asc(), d.id.asc())
+val hasRequiredDetail = JPAExpressions.selectOne().from(d)
+    .where(d.itemId.eq(q.id), d.label.eq("필수"))
+    .exists()
+val rows = queryFactory.select(q.id, q.name).from(q)
+    .where(scopeCondition(scope), hasRequiredDetail)
+    .orderBy(q.id.asc())
     .limit(2)
     .fetch()
```

`d.itemId.eq(q.id)`는 현재 부모의 자식인지 연결하는 조건이므로 반드시 남긴다. 이를 빼면 “어떤 Item에든 필수 자식이 존재하면 모든 부모 통과”가 된다. DTO 변환은 그대로 두며 수정 뒤 `rows`와 `views`의 Item id는 모두 `[I1, I2]`여야 한다.

### 회귀 입력과 기대 결과

| 입력 변경 | 기대 결과 | 확인하는 의미 |
|---|---|---|
| 위 원본 데이터, limit=2 | I1, I2 각 한 번 | 자식 수와 부모 페이지 수 분리 |
| I1에 필수 자식 10개 추가 | 여전히 I1, I2 | 자식 수 증가가 부모 수를 늘리지 않음 |
| I1의 필수 자식을 전부 제거 | I2만 | 존재 조건 유지 |
| 모든 부모에서 필수 자식 제거 | 빈 목록 | EXISTS 연결·label 조건 유지 |
| X1에 필수 자식 10개 추가 | 원본의 I1, I2 유지 | 다른 scope가 섞이지 않음 |
| I3에 필수 자식 추가, limit=3 | I1, I2, I3 | 부모 정렬·제한 정상 |

검증 때는 화면 행 수만 보지 말고 DB 조회 결과의 부모 id 수와 응답의 id 수를 함께 비교한다. DTO 매핑만 확인하는 단위 테스트는 실제 JOIN·limit 조합을 검증하지 못하므로, 준비된 조회 테스트 환경에서 이 입력의 결과를 확인해야 한다.

### 다른 호출에 미치는 영향도 읽는다

IDE의 사용처 찾기로 이 쿼리 함수가 **자식 id·label을 실제로 사용하는 다른 호출**에도 쓰이는지 본다. 그런 호출까지 EXISTS로 바꾸면 필요한 값이 사라진다. 부모만 고르는 조회와 자식 상세 조회의 책임을 나누고, 연결된 total count도 “부모 수”를 세는지 확인한다. EXISTS가 항상 더 빠르다는 결론은 별도 실행 근거 없이 내리지 않는다.

**추가 연습:** 요구가 “필수 자식이 있는 Item을 고르되, 그 Item의 모든 자식 label도 보여 달라”로 바뀌면 어디를 바꿀까?

<details>
<summary>생각한 뒤 비교할 답</summary>

EXISTS로 부모 후보·페이지를 정하는 부분은 유지한다. 선택한 부모 id들로 Detail을 일괄 조회한 뒤 `groupBy(itemId)`로 붙인다. 자식 조회에도 `label = "필수"`를 걸면 선택 자식까지 보여야 하는 새 요구를 어기므로, **부모 선정 조건과 응답에 담을 자식 범위**를 분리한다.

</details>

## 실제 소스에서 같은 패턴 확인하기

| 확인할 패턴 | 소스와 찾을 메서드 |
|---|---|
| 부모 목록 → id 수집 → 부가 정보 조립 | [AppointmentQueries](../../../x-patient-appointment/backend/patient-appointment/src/main/kotlin/com/projectx/patientappointment/infrastructure/queries/AppointmentQueries.kt)의 `views` |
| 참조 검색 조건을 IN 서브쿼리로 적용 | 같은 파일의 `keywordMatches` |
| 컬럼 JOIN과 Tuple 결과 | [ObservationQueries](../../../x-diagnostic-result/backend/diagnostic-result/src/main/kotlin/com/projectx/diagnosticresult/infrastructure/queries/ObservationQueries.kt)의 `categoryQuery`·`toCategoryView` |
| id 중복 때문에 JOIN 대신 별도 일괄 조회 | 같은 파일의 `findUserNames`·`toCategoryViews` |
| 부모·자식 묶음을 나눠 읽고 중첩 응답 구성 | [ViewerConfigurationQueries](../../../x-diagnostic-result/backend/diagnostic-result/src/main/kotlin/com/projectx/diagnosticresult/infrastructure/queries/ViewerConfigurationQueries.kt)의 `findViewerConfiguration` |

[이전: 결과 바인딩](03-result-binding-patterns.md) · [목차](README.md) · [다음: 페이징·집계](05-pagination-and-aggregation.md)
