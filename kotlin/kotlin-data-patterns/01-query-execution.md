# 1. 조회식을 만들고 실행하는 패턴

[목차](README.md) · [다음: 검색조건](02-filter-patterns.md)

## 1.1 먼저 세 가지 값의 정체를 구분한다

데이터 조회 코드를 읽을 때 가장 먼저 볼 것은 함수 이름보다 **변수의 타입**이다.

| 변수 예 | 정체 | 실제 조회한 데이터인가? |
|---|---|---|
| `q = QItem.item` | Entity의 필드를 가리키는 조회 경로 | 아니다 |
| `query = queryFactory.selectFrom(q)...` | 실행할 조회식 | 아니다 |
| `rows = query.fetch()` | 실행 결과 `List<Item>` | 그렇다 |

`q.name`은 항목의 이름 문자열이 아니라 **이름 컬럼을 조회식에서 가리키는 표현식**이다. `rows[0].name`은 읽어 온 첫 객체의 실제 값이다. 두 코드에 `name`이 보이지만 존재하는 단계가 다르다.

C#에서 `IQueryable<Item>`과 `List<Item>`을 구분하는 경험을 그대로 가져오면 된다. 다만 QueryDSL은 LINQ 람다 대신 `q.name`, `q.amount` 같은 타입 있는 경로 객체로 조회식을 구성한다.

## 1.2 기본 패턴: 조건을 붙여 목록으로 읽는다

요구는 간단하다. **현재 범위 안에서 사용 중인 항목을 이름 순으로 조회한다.**

설명에 사용할 행:

| id | tenantId | hospitalCode | name | active |
|---|---|---|---|---|
| A | T1 | H1 | Alpha | true |
| B | T1 | H1 | Beta | false |
| C | T1 | H1 | Gamma | true |
| D | T2 | H1 | Delta | true |

현재 범위는 T1/H1이다. 결과는 A, C 두 행이어야 한다.

### C# / EF Core

```csharp
// 설명용: tenantId, hospitalCode는 신뢰할 수 있는 요청 범위다.
IQueryable<Item> query = db.Items
    .Where(x => x.TenantId == tenantId && x.HospitalCode == hospitalCode)
    .Where(x => x.Active)
    .OrderBy(x => x.Name)
    .ThenBy(x => x.Id);

List<Item> rows = query.ToList();
```

`Where`와 `OrderBy`는 쿼리를 구성한다. 이 예제에서는 `ToList()`에서 실행한다. `ToListAsync()`도 실행 경계지만 기다리는 방식이 다르다.

### Kotlin / QueryDSL JPA

```kotlin
// 설명용. 함수 안의 지역 변수로 조회식을 구성한다.
val q = QItem.item
val scopePredicate = q.tenantId.eq(scope.tenantId)
    .and(q.hospitalCode.eq(scope.hospitalCode))

val query = queryFactory.selectFrom(q)
    .where(scopePredicate, q.active.isTrue)
    .orderBy(q.name.asc(), q.id.asc())

val rows: List<Item> = query.fetch()
```

### 한 줄씩 해석하기

| 코드 | 뜻 | DB 실행 여부 |
|---|---|---|
| `QItem.item` | Item 테이블에 대응하는 Entity 경로 선택 | 실행 안 함 |
| `q.tenantId.eq(...)` | 값 비교 조건 생성 | 실행 안 함 |
| `.and(...)` | 조건을 AND로 결합 | 실행 안 함 |
| `selectFrom(q)` | Entity를 결과로 선택하는 조회 구성 | 실행 안 함 |
| `where(a, b)` | 두 조건을 모두 만족하도록 구성 | 실행 안 함 |
| `orderBy(...)` | 정렬 기준 구성 | 실행 안 함 |
| `fetch()` | 쿼리 실행 및 결과 객체 획득 | 실행 |

SQL 의도는 다음과 같다. **이 SQL은 설명용이며 생성되는 정확한 SQL이나 프로젝트에 추가할 문자열 쿼리가 아니다.**

```sql
SELECT ...
FROM item
WHERE tenant_id = :tenantId
  AND hospital_code = :hospitalCode
  AND active = true
ORDER BY name, item_id;
```

`fetch()`는 동기 호출이다. 이 호출이 정상 반환되면 `rows`에는 결과 목록이 있다. SQL 전송·결과 수신·Entity 구성은 그 호출 과정에 포함된다. 앞에 쿼리를 선언한 시점부터 목록이 미리 채워지는 구조가 아니다.

## 1.3 조회식이 반환되는 함수와 데이터가 반환되는 함수

다음 두 함수를 비교한다.

```kotlin
// 설명용: JPAQuery는 쿼리 구성 객체다.
fun activeQuery(scope: TenantScope): JPAQuery<Item> =
    queryFactory.selectFrom(q)
        .where(scopeCondition(scope), q.active.isTrue)

// 설명용: 이 함수는 실제 조회한다.
fun findActive(scope: TenantScope): List<Item> =
    activeQuery(scope).fetch()
```

첫 함수는 `JPAQuery<Item>`, 둘째는 `List<Item>`을 반환한다. `activeQuery(scope)`를 호출한 것만으로 데이터가 오지 않는다. 반대로 `findActive(scope)`가 반환되었다면 조회가 끝난 목록이다.

`scopeCondition`은 1.2의 tenant·hospital 조건을 함수로 묶은 이름이고 `q`는 같은 QItem이다. 실제 코드에서 `categoryQuery`, `searchWhere`, `find...`를 읽을 때도 **반환형과 terminal 호출을 함께 찾는다.** 함수 이름만으로 실행 여부를 단정하지 않는다.

QueryDSL의 조회 객체는 조건을 누적하는 가변 객체다. 웹 요청마다 지역 변수로 만들고, 요청별 조건이 들어간 query를 싱글턴 Service의 필드에 보관하지 않는다. 한 요청의 조건이 다음 요청에 섞이면 안 된다.

## 1.4 DB 필터와 메모리 필터의 차이

같은 두 행을 얻더라도 데이터가 이동한 양은 다를 수 있다.

### A. DB에서 먼저 거른다

```kotlin
val rows = queryFactory.selectFrom(q)
    .where(scopePredicate, q.active.isTrue)
    .fetch()
```

DB가 사용 중인 행을 골라서 반환한다. 예제에서는 A·C 두 행만 가져온다.

### B. 범위의 전체 행을 가져와 Kotlin에서 거른다

```kotlin
val allRows = queryFactory.selectFrom(q)
    .where(scopePredicate)
    .fetch()
val rows = allRows.filter { it.active }
```

DB에서 A·B·C 세 행을 가져온 다음 B를 버린다. 화면 결과가 같아도 네트워크 전송·객체 생성·메모리 비용이 다르다. 실제 범위가 수십만 행이면 차이가 커진다.

| 단계 | A | B |
|---|---|---|
| DB 결과 | A, C | A, B, C |
| 조회 결과 Item 수 | 2개 | 3개 |
| 추가 Kotlin 필터 | 없음 | 필요 |
| 최종 응답 후보 | A, C | A, C |

C#에서는 `db.Items.Where(...).ToList()`와 `db.Items.ToList().Where(...).ToList()`의 차이에 해당한다. DB에서 표현할 수 있는 기본 필터는 보통 조회식에 넣는다. 이미 작은 목록을 읽었고 DB에서 표현하기 어려운 표시 규칙이라면 메모리 계산이 적절할 수 있다.

## 1.5 select와 map도 위치가 다르다

“이름만 필요하다”면 다음 선택이 생긴다.

```kotlin
// A: 이름 컬럼만 읽는다.
val names: List<String> = queryFactory.select(q.name)
    .from(q)
    .where(scopePredicate)
    .fetch()

// B: Entity를 읽은 뒤 이름을 꺼낸다.
val entities: List<Item> = queryFactory.selectFrom(q)
    .where(scopePredicate)
    .fetch()
val namesAfterLoad: List<String> = entities.map { it.name }
```

A는 scalar 조회다. B는 Entity를 만들고 그 객체에서 이름을 읽는다. 최종 `List<String>` 모양이 같아도 중간 과정은 다르다. **반환 모양이 아니라 `fetch` 앞의 select 대상**이 DB에서 어떤 값을 가져오는지 결정한다.

C#의 대응:

```csharp
var names = scoped.Select(x => x.Name).ToList();
var namesAfterLoad = scoped.ToList().Select(x => x.Name).ToList();
```

여기서 `scoped`는 1.2와 같은 필수 범위를 포함한 IQueryable이다. Entity가 필요한 쓰기 업무인지, 표시할 일부 값만 필요한 조회인지에 따라 선택한다. DTO로 직접 받는 패턴은 3장에서 비교한다.

## 1.6 Kotlin List는 C# Enumerable과 똑같이 지연되지 않는다

```kotlin
val rows = query.fetch()                       // DB 결과 목록 획득
val names = rows.filter { it.active }          // 즉시 목록 가공
    .map { it.name }                           // 즉시 다른 목록 생성
```

일반 Kotlin 컬렉션의 `filter`·`map`은 각 단계에서 결과를 만든다. C# `IEnumerable`의 LINQ처럼 마지막 열거 때까지 전부 미뤄지는 것으로 읽으면 안 된다.

```kotlin
val names = rows.asSequence()
    .filter { it.active }
    .map { it.name }
    .toList()
```

이 코드는 메모리 목록을 지연 처리한다. **이미 끝난 `query.fetch()`를 SQL 쿼리로 되돌리지 않는다.** `Sequence`는 DB 쿼리를 나타내는 `IQueryable`의 대체물이 아니다. 작은 목록은 일반 컬렉션이 더 단순할 수 있다. [Kotlin 공식 Sequence 설명](https://kotlinlang.org/docs/sequences.html).

## 1.7 fetch를 두 번 호출하면 결과 목록을 재사용하는가

```kotlin
val query = queryFactory.selectFrom(q).where(scopePredicate)
val first = query.fetch()
val second = query.fetch()
```

여기에는 실행 요청이 두 번 있다. query는 자동으로 결과 목록을 보관하는 캐시가 아니다. 구성된 ORM 캐시가 실제 SQL 횟수에 영향을 줄 수는 있지만, 첫 `List`를 단순 재사용한 코드로 읽으면 안 된다.

```kotlin
val rows = query.fetch()
val names = rows.map { it.name }
val ids = rows.map { it.id }
```

이 코드는 결과 목록을 재사용한다. 다만 `map` 안에서 아직 로딩하지 않은 관계를 접근하면 추가 조회가 발생할 수 있다. `map`이라는 문법 자체가 쿼리를 실행하는 것은 아니며, **람다 안에서 무엇에 접근하는지**가 중요하다. 4장에서 지연 로딩을 설명한다.

동일 영속성 컨텍스트에서 Entity를 다시 조회하면 기존 관리 객체가 재사용될 수 있다. `fetch`를 다시 했다는 사실만으로 모든 객체 필드가 최신 DB 값으로 덮어써졌다고 가정하지 않는다. 이 문제는 6장에서 다룬다.

## 1.8 이 장에서 필요한 Kotlin 문법만 읽기

| 코드 | 의미 |
|---|---|
| `val rows = ...` | 참조를 재대입하지 않음. SQL 실행 여부와 무관 |
| `List<Item>` | Item 객체 목록 |
| `rows.map { it.name }` | 항목마다 이름을 꺼낸 새 목록. `it`은 현재 항목 |
| `rows.map { row -> row.name }` | 위 코드와 같고 인자 이름만 명시 |
| `query.fetch().let { rows -> toViews(rows) }` | 실행한 목록을 `toViews`에 넘김 |
| `q.id.eq(id)` | DB 비교를 나타내는 표현식 생성 |
| `q.id == id` | Kotlin의 객체 동등성 비교. QueryDSL의 where 조건과 다름 |
| `Item?` | 단건 결과가 없을 수 있음 |

## 1.9 코드를 읽는 연습

```kotlin
val query = queryFactory.select(q.id, q.name)
    .from(q)
    .where(scopePredicate, q.active.isTrue)
val rows = query.fetch()
val result = rows.map { row -> row.get(q.name) }
```

1. DB 실행은? → `query.fetch()`.
2. Entity 전체를 읽는가? → 아니다. id·name을 담은 Tuple 목록을 읽는다.
3. `map`이 SQL SELECT를 만드는가? → 아니다. 가져온 Tuple에서 값을 꺼낸다.
4. result의 값을 바꾸면 DB가 갱신되는가? → 아니다. 새 목록의 표시 데이터다.

## 1.10 직접 수정해 보기: 한 요청에서 같은 조회를 두 번 실행한다

아래는 실행 시점을 익히기 위해 만든 가상 결함이다. 이 메서드는 **페이지 제한 없는 활성 목록과 그 목록의 건수**를 함께 반환한다. `ItemListResult`는 `count: Int`, `items: List<ItemView>`를 가진 설명용 DTO이고, `toView`는 이미 읽은 기본 필드만 복사한다.

```kotlin
// 수정 전: 같은 조회식을 두 번 실행한다.
val query = queryFactory.selectFrom(q)
    .where(scopePredicate, q.active.isTrue)
    .orderBy(q.name.asc(), q.id.asc())

val count = query.fetch().size
val items = query.fetch().map { toView(it) }
return ItemListResult(count = count, items = items)
```

처음 읽을 때 두 번째 `fetch()`가 첫 목록을 꺼내 쓰는 것으로 보였다면 1.1·1.7을 다시 읽는다. `query`에 담긴 것은 실행식이다. 첫 `fetch` 결과는 `size`를 계산한 뒤 보관하지 않았다.

### 먼저 관측하고 수정 위치를 고른다

두 `fetch`에 중단점을 걸고 같은 요청에서 둘 다 지나는지 확인한다. 준비된 테스트 환경의 쿼리 기록으로 실행 횟수를 확인하되, 디버거의 값 평가에서 `fetch()`를 직접 호출하지 않는다. 평가식도 코드를 실행하므로 관측 때문에 조회가 추가될 수 있다.

이 예제는 같은 결과 목록을 두 번 쓰려는 의도다. Controller의 응답 형식이나 Entity를 고치는 대신 **실행 결과를 지역 변수에 보관**한다.

```diff
-val count = query.fetch().size
-val items = query.fetch().map { toView(it) }
+val rows = query.fetch()
+val count = rows.size
+val items = rows.map { toView(it) }
 return ItemListResult(count = count, items = items)
```

`rows`는 `List<Item>`이다. `size`와 기본 필드의 `map`은 이 목록을 읽는다. 매퍼에서 지연 관계나 다른 조회 함수를 호출한다면 그 추가 실행은 별도로 추적해야 한다.

### 동작과 실행 횟수를 함께 확인한다

| 준비 데이터·조건 | 기대 결과 | 확인할 변화 |
|---|---|---|
| 내 범위 활성 A·C, 비활성 B | count=2, items=A·C | 조건·순서는 이전과 같음 |
| 일치 항목 없음 | count=0, items는 빈 목록 | 빈 결과도 정상 반환 |
| 다른 범위 활성 D 추가 | D는 제외 | 필수 범위 유지 |
| 같은 입력으로 한 번 호출 | 이 목록의 `fetch` 호출은 1회 | 중복 실행 제거 |

캐시를 끈 조회 테스트 등 실행 조건이 통제된 곳에서 SQL 횟수도 확인한다. 목록 값이 맞는 것만으로 중복 실행 제거를 검증한 것은 아니다. 반대로 횟수만 줄고 필터나 정렬이 바뀌어도 잘못된 수정이다.

### 같은 수정을 적용하면 안 되는 경우도 판단한다

함수의 사용처에서 `count`가 “이번 목록 크기”인지 “모든 페이지를 합친 전체 건수”인지 확인한다. `limit(20)`이 붙고 전체 대상이 53개라면 `rows.size`는 최대 20이다. 전체 건수 53을 요구하는 API라면 별도 count 조회가 필요하다([5장](05-pagination-and-aggregation.md)).

**추가 문제:** `val query`를 `val rows`로 이름만 바꾸면 실행 결과가 캐시되는가?

<details>
<summary>판단 확인</summary>

아니다. 변수 이름과 `val` 여부가 아니라 오른쪽 식과 반환 타입을 본다. `selectFrom(...).where(...)`는 조회식이고 `fetch()`가 반환한 것이 목록이다. C#에서도 `IQueryable` 변수 이름을 `list`로 바꾼다고 `List`가 되지 않는다.

</details>

실제 소스에 이 판단을 적용하는 절차는 [9장](09-studying-to-change-code.md)을 따른다.

## 실제 소스에서 같은 패턴 찾기

| 확인할 패턴 | 근거 파일·심볼 |
|---|---|
| 조회 구성 후 `fetch`, Entity 목록을 View로 변환 | [AppointmentQueries.find](../../../x-patient-appointment/backend/patient-appointment/src/main/kotlin/com/projectx/patientappointment/infrastructure/queries/AppointmentQueries.kt) |
| 쿼리 구성 함수가 `JPAQuery<Tuple>` 반환 | [ObservationQueries.categoryQuery](../../../x-diagnostic-result/backend/diagnostic-result/src/main/kotlin/com/projectx/diagnosticresult/infrastructure/queries/ObservationQueries.kt) |
| `fetchOne` 뒤 Entity→View 변환 | [OrderingQueries.getOrderSheet](../../../x-clinical-care/backend/ordering/src/main/kotlin/com/projectx/ordering/application/queries/OrderingQueries.kt) |

소스에서 찾을 것은 업무 동작이 아니라 `select 대상 → 조건 → 실행 → 결과 변환`의 네 부분이다.

[목차](README.md) · [다음: 검색조건](02-filter-patterns.md)
