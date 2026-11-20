# 2. 조건을 조립하는 조회 패턴

[이전: 조회 실행 시점](01-query-execution.md) · [목차](README.md) · [다음: 결과 바인딩](03-result-binding-patterns.md)

조회 조건을 읽을 때 중요한 것은 **어떤 행을 남기는지, 조건을 만들 때와 실행할 때가 어떻게 다른지**다. 이 장은 검색창·체크박스·기간 같은 입력이 QueryDSL 조건으로 바뀌는 패턴을 설명한다.

예제는 `Item(id, name, categoryId, active, amount, createdAt, tenantId, hospitalCode)`를 사용한다. `q`는 `QItem.item`, `scope`는 인증에서 얻은 필수 조회 범위다. C#은 EF Core의 `IQueryable`, Kotlin은 QueryDSL JPA를 가정한다. **설명용 예제로 Entity·Q클래스 생성·DI·import는 생략**했으며 그대로 실행하는 완성 코드가 아니다.

## 2.1 패턴: 필수 조건에 선택 조건을 더한다

**문제:** 검색어는 생략할 수 있지만 사용자에게 허용된 tenant·hospital 범위는 항상 있어야 한다. 입력이 없다고 모든 조건을 빼면 안 된다.

C#에서 조건을 누적하는 모양은 익숙하다.

```csharp
var query = db.Items.Where(x =>
    x.TenantId == scope.TenantId && x.HospitalCode == scope.HospitalCode);

var text = keyword?.Trim();
if (!string.IsNullOrEmpty(text))
    query = query.Where(x => x.Name.Contains(text));
if (active.HasValue)
    query = query.Where(x => x.Active == active.Value);

var rows = query.ToList();
```

QueryDSL에서는 조건 객체를 누적할 수 있다.

```kotlin
val where = BooleanBuilder(scopeCondition(scope))                  // ①
val text = keyword?.trim()?.takeIf { it.isNotEmpty() }              // ②
text?.let { where.and(q.name.contains(it)) }                        // ③
active?.let { where.and(q.active.eq(it)) }                          // ④
val rows = queryFactory.selectFrom(q).where(where).fetch()          // ⑤
```

| 줄 | 실제 하는 일 | DB 접근 |
|---|---|---|
| ① | 필수 scope 표현식으로 조건 누적기 생성 | 아직 없음 |
| ② | 요청 문자열을 메모리에서 정리 | 없음 |
| ③ | 검색어가 있으면 이름 조건 추가 | 아직 없음 |
| ④ | active가 null이 아니면 true 또는 false 조건 추가 | 아직 없음 |
| ⑤ | 만든 조건으로 조회하고 결과 목록 획득 | 여기서 실행 |

`scopeCondition`은 이 장에서 반복을 줄인 설명용 함수다. 실제 프로젝트에서는 해당 서비스의 `tenantPredicate`·`inScope`를 사용한다.

```kotlin
fun scopeCondition(scope: TenantScope): BooleanExpression =
    q.tenantId.eq(scope.tenantId)
        .and(q.hospitalCode.eq(scope.hospitalCode))
```

입력에 따른 결과를 말로 먼저 확정하면 코드가 쉬워진다.

| 입력 | 최종 조건의 의미 |
|---|---|
| keyword=null, active=null | 내 범위의 모든 Item |
| keyword="연필", active=null | 내 범위에서 이름에 검색어 포함 |
| keyword=null, active=false | 내 범위에서 비활성 Item |
| keyword="   ", active=true | 공백 검색어는 생략하고 활성 Item |

`active?.let`은 false도 실행한다. `if (active == true)`로 바꾸면 “비활성만 찾기” 입력이 조건 없음으로 바뀐다. 문자열 대소문자 비교는 선택한 QueryDSL 연산과 DB 비교 규칙을 확인한다. `contains`가 모든 환경에서 대소문자를 무시한다고 가정하지 않는다.

**선택 기준:** 조건이 여럿이고 분기별로 누적하는 과정이 잘 보여야 할 때 `BooleanBuilder`가 읽기 쉽다. builder는 요청마다 새로 만들고 Spring 빈의 변경 가능한 공용 필드로 두지 않는다.

## 2.2 패턴: 선택 조건을 nullable 함수로 분리한다

**문제:** 같은 검색 조건을 여러 조회에서 쓰는데 if문을 반복하고 싶지 않다. 조건을 반환하는 작은 함수를 만들 수 있다.

```kotlin
fun nameContains(keyword: String?): BooleanExpression? =
    keyword?.trim()?.takeIf { it.isNotEmpty() }
        ?.let { q.name.contains(it) }

fun activeEquals(active: Boolean?): BooleanExpression? =
    active?.let { q.active.eq(it) }

val rows = queryFactory.selectFrom(q)
    .where(scopeCondition(scope), nameContains(keyword), activeEquals(active))
    .fetch()
```

QueryDSL의 `where`는 null predicate를 생략하고, 나머지 인자는 AND로 결합한다. 여기서 null은 **조건 없음**이지 `WHERE name IS NULL`이 아니다.

C#에서 조건에 따라 `.Where(...)` 호출을 건너뛰던 일을 함수 반환값으로 표현한 것이다. `nameContains`가 반환하는 것은 Boolean 계산 결과가 아니라 나중에 쿼리로 해석할 표현식이다. SQL 실행은 마지막 `fetch()`다.

**선택 기준:** 간단한 선택 조건을 이름으로 설명하고 재사용할 때 쓴다. 필수 scope까지 nullable 함수로 바꿔 생략 가능하게 만들지 않는다. `BooleanBuilder`와 이 방식은 SQL의 종류가 다른 것이 아니라 조건을 구성하는 코드 모양이 다르다.

## 2.3 패턴: AND와 OR는 괄호를 먼저 정한다

**문제:** “내 범위 안에서 활성 또는 금액 100 이상”을 찾는다. 범위 밖 행이 OR로 빠져나오면 데이터 접근 오류가 된다.

```csharp
var rows = db.Items
    .Where(x => x.TenantId == scope.TenantId &&
                x.HospitalCode == scope.HospitalCode &&
                (x.Active || x.Amount >= 100m))
    .ToList();
```

```kotlin
val businessCondition = q.active.isTrue.or(q.amount.goe(BigDecimal("100")))
val rows = queryFactory.selectFrom(q)
    .where(scopeCondition(scope), businessCondition)
    .fetch()
```

`businessCondition`을 먼저 만들어 **scope AND (A OR B)**로 묶었다. 다음 두 식은 다르다.

```text
원하는 식: scope AND (active = true OR amount >= 100)
다른 식:   (scope AND active = true) OR amount >= 100
```

두 번째 식은 다른 tenant라도 금액이 100 이상이면 통과시킨다. `.and(...).or(...)`를 이어 쓸 때는 코드의 들여쓰기보다 최종 괄호를 읽는다. 검색어가 이름·코드 등 여러 열에 적용될 때도 “여러 열의 OR 묶음”을 만든 다음 필수 범위와 AND로 합친다.

## 2.4 패턴: null인 열을 포함할지 명시한다

**문제:** “금액이 10이 아닌 것”에 금액 미입력 행도 포함하는가? SQL은 true·false 외에 UNKNOWN을 사용하며 WHERE는 true인 행만 남긴다.

| amount | `amount <> 10` | WHERE 결과 |
|---|---|---|
| 10 | false | 제외 |
| 20 | true | 포함 |
| NULL | UNKNOWN | 제외 |

C#의 nullable 비교 경험을 QueryDSL에 그대로 옮기지 않는다. EF Core는 C# 의미를 맞추기 위해 일부 비교에 null 조건을 추가할 수 있다. QueryDSL 조건에서는 원하는 포함 규칙을 명시해서 읽는다. [EF Core의 null 비교 설명](https://learn.microsoft.com/ef/core/querying/null-comparisons).

```csharp
// 미입력도 포함하려는 의도를 명시
var rows = db.Items.Where(x =>
    x.TenantId == scope.TenantId && x.HospitalCode == scope.HospitalCode &&
    (x.Amount == null || x.Amount != 10m)).ToList();
```

```kotlin
val amountCondition = q.amount.isNull.or(q.amount.ne(BigDecimal("10")))
val rows = queryFactory.selectFrom(q)
    .where(scopeCondition(scope), amountCondition)
    .fetch()
```

`isNull`은 DB 컬럼의 null 검사다. Kotlin의 `amount == null`은 지금 JVM 변수의 null 검사다. 같은 단어라도 검사 장소가 다르다. ``q.amount.`in`(values)`` 또는 `notIn(values)`에서도 null 행을 포함할지 따로 판단하고, 입력 값 목록에 null이 섞이지 않도록 계약을 정한다.

**선택 기준:** 제외 조건·미입력 포함·선택 참조가 있는 조회라면 정상 값 예제뿐 아니라 NULL 행을 표에 넣어 결과를 확인한다.

## 2.5 패턴: 빈 ID 목록은 업무 의미를 먼저 정한다

**문제:** `ids=[]`가 “필터를 고르지 않았음”인지 “허용된 대상이 하나도 없음”인지에 따라 결과가 반대다.

| 빈 목록의 의미 | 올바른 처리 | 결과 |
|---|---|---|
| 선택 필터 미지정 | 그 조건만 생략, scope는 유지 | 범위 안에서 조회 |
| 조회 가능한 후보가 없음 | 즉시 빈 목록 반환 | DB 조회 없이 0건 |
| 필수 입력 누락 | 경계에서 입력 오류 | 조회 진행 안 함 |

후보가 없으면 0건인 패턴:

```kotlin
// 조회 함수 내부 발췌 형태
if (ids.isEmpty()) return emptyList()
return queryFactory.selectFrom(q)
    .where(scopeCondition(scope), q.id.`in`(ids))
    .fetch()
```

C#의 `ids.Contains(x.Id)`를 쓰기 전에 빈 후보를 처리하는 것과 같은 의도다. 이때 빈 목록이라고 `in` 조건을 생략하면 “후보 없음”이 “전체 조회”로 바뀐다. 빈 `IN`을 ORM이 어떻게 번역하는지에 업무 의미를 맡기지 않는다.

nullable 목록을 사용한다면 `null=필터 미지정`, `empty=후보 없음` 같은 계약도 가능하다. 이 구분을 `orEmpty()` 한 줄로 없애지 않는다.

## 2.6 패턴: 날짜 입력을 시각 범위로 바꾼다

**문제:** 사용자는 시작일·종료일을 날짜로 고르지만 DB에는 `Instant`가 있다. 종료일의 00:00까지만 검색하면 그날 대부분을 놓친다.

C#에서 `startUtc`, `endExclusiveUtc`를 계산해 `CreatedAt >= startUtc && CreatedAt < endExclusiveUtc`로 비교하는 패턴과 같다. Kotlin에서는 병원 시간대를 기준으로 날짜 경계를 시각으로 바꾼다.

```kotlin
// fromDate와 toDate는 포함할 시작일·종료일(LocalDate)
val start = fromDate.atStartOfDay(hospitalZone).toInstant()
val endExclusive = toDate.plusDays(1).atStartOfDay(hospitalZone).toInstant()
val rows = queryFactory.selectFrom(q)
    .where(scopeCondition(scope), q.createdAt.goe(start), q.createdAt.lt(endExclusive))
    .fetch()
```

| 입력·값 | 범위 포함 여부 |
|---|---|
| 날짜 입력 2026-11-01 ~ 2026-11-02 | 병원 시간대 1일 00:00 이상, 3일 00:00 미만 |
| 2일 23:59:59.999… | 포함 |
| 3일 00:00 정확히 | 제외 |

1. `atStartOfDay`·`toInstant`는 JVM에서 경계값을 계산한다.
2. `goe`·`lt`는 DB 비교식을 만든다.
3. `fetch`에서 그 범위로 조회한다.

끝을 `23:59:59.999`로 직접 만들면 DB 시간 정밀도에 따라 빠지는 값이 생길 수 있다. 하루를 무조건 24시간 더하는 대신 병원 날짜의 다음 날 시작을 계산하면 시간대의 일광절약시간 변화도 다룰 수 있다. 반면 DB 열 자체가 `LocalDate`라면 날짜끼리 `>= from`, `<= to`로 비교한다. 시작일이 종료일보다 늦은 입력은 조회 전에 계약대로 거절하거나 처리한다.

## 2.7 패턴: 입력값과 쿼리 구조를 분리한다

`q.name.eq(input)`·`q.amount.goe(minimum)`은 값으로 조건을 구성하고 JPA가 쿼리 매개변수로 처리하는 경로다. 사용자 문자열로 JPQL·SQL 조각을 이어 붙이지 않는다. 따옴표가 들어 있는 검색어도 **검색값**이어야 하며 쿼리 문법이 되면 안 된다.

다만 값 바인딩은 업무 검증·권한 검증을 대신하지 않는다. 너무 긴 검색어·과도한 기간·필수 scope 누락은 별도 문제다. 정렬 컬럼 이름은 값 파라미터가 아니므로 허용된 Q필드 목록에 매핑해야 한다([5장](05-pagination-and-aggregation.md)).

## 2.8 직접 수정해 보기: 비활성 조회에서 전체가 나오는 이유

이 절은 **의도적으로 만든 학습용 버그**다. 실제 레포에 같은 결함이 있다는 뜻은 아니다. 목표는 정답 코드를 외우는 것이 아니라 증상에서 조건 누락을 찾아 수정 위치를 판단하는 것이다.

### 증상과 계약을 먼저 확정한다

조회 API의 계약은 `active=null`이면 전체, `true`면 활성만, `false`면 비활성만이다. 내 범위에 A(활성)·B(비활성)가 있고 다른 범위에 C(비활성)가 있다. 비활성을 선택했는데 A·B가 모두 보인다.

먼저 스스로 묻는다. “false가 서버에 안 왔는가, 서버에는 왔지만 조건으로 사용하지 않았는가, 조건은 맞는데 응답을 조립하다 섞였는가?” 이 셋은 고칠 파일이 다르다.

```kotlin
// 잘못된 설명용 코드: false를 '조건 없음'으로 취급했다.
val where = BooleanBuilder(scopeCondition(scope))
if (active == true) {
    where.and(q.active.isTrue)
}
val rows = queryFactory.selectFrom(q).where(where).fetch()
```

`if`의 실행 결과를 입력별로 손으로 따라가 본다. true일 때만 조건이 붙고, null과 false는 같은 경로다. Kotlin Boolean이 특별한 것이 아니라 **개발자가 두 입력을 같은 의미로 합친 것**이다.

### 중단점을 걸어 가설을 좁힌다

| 관측 위치 | 볼 값·상태 | 다음 판단 |
|---|---|---|
| Controller의 입력 인자 | `active`가 null인가 false인가 | null이면 요청 이름·변환·기본값부터 확인 |
| 조회 메서드 진입 | 전달된 filter의 active | 여기서 달라졌으면 Request→Filter 복사 확인 |
| 조건 분기 직전 | `active == true`의 결과 | false가 들어왔는데 조건이 생략되는지 확인 |
| `fetch()` 직전 | scope와 active 표현식의 구성 | 입력 문제와 조건 조립 문제를 구분 |
| `fetch()` 직후 | 결과 id와 active 값 | DB 결과부터 잘못됐는지, 이후 응답에서 섞였는지 구분 |

디버거의 조건 객체 문자열만으로 실행된 SQL 전체를 검증했다고 보지는 않는다. 준비된 테스트 환경에서는 실제 쿼리 결과도 확인한다. 이 예제에 실환자 정보는 필요하지 않다.

### 조건을 만드는 위치에서 고친다

```diff
 val where = BooleanBuilder(scopeCondition(scope))
-if (active == true) {
-    where.and(q.active.isTrue)
-}
+active?.let { requested ->
+    where.and(q.active.eq(requested))
+}
 val rows = queryFactory.selectFrom(q).where(where).fetch()
```

수정은 null만 생략하고 true·false를 모두 비교식으로 만든다. 필수 scope는 그대로 남는다. DB가 처음부터 올바른 행을 고르도록 **fetch 전의 조건 조립**을 고친 것이다.

응답의 `rows.filter { !it.active }`로 감추면 이 요청의 null·true 의미가 깨지고, 페이지 크기·전체 건수도 맞지 않을 수 있다. Entity 기본값을 false로 바꾸는 것도 기존 데이터 조회 조건 문제를 해결하지 않는다.

### 회귀 입력과 기대 결과를 적는다

A·B는 내 범위, C는 다른 범위라는 위의 데이터로 확인한다.

| 입력 | 기대 id | 함께 확인할 것 |
|---|---|---|
| active 생략 또는 null | A, B | 선택 조건만 생략되고 scope는 유지 |
| active=true | A | 기존 활성 조회 동작 유지 |
| active=false | B | 고친 비활성 조회 동작 |
| active=false, B를 제외하는 다른 필터 | 없음 | 다른 조건과 AND로 결합 |
| active=false, 전체 건수 조회 | 1 | 목록과 count가 같은 조건 사용 |
| active=false, C가 존재 | C 제외 | 다른 범위의 데이터가 섞이지 않음 |

예상 행 수만 보지 말고 **어떤 id가 남았는지** 확인한다. 같은 건수라도 A와 B가 바뀌면 실패다. 순서 검증이 목적이 아니라면 결과 id 집합을 비교하고, 정렬 계약이 있으면 순서도 따로 확인한다.

### 다른 호출에 미치는 영향과 새 조건 추가

이 조회 함수를 호출하는 곳을 검색해 `active=false`를 어떤 뜻으로 보내는지 읽는다. 다른 호출자가 false를 “활성 조건 사용 안 함”으로 해석하고 있었다면 함수 계약이 섞인 것이다. 호출자별 의도를 먼저 정리해야 한다.

새 `categoryId` 조건을 추가할 때도 같은 순서다. **요청 입력 → Filter 전달 → QueryDSL 조건 → 목록과 count**까지 연결하고, 생략·일치·불일치·다른 scope의 결과를 정한다. DTO에 필드만 추가하거나 Controller에서 값을 받는 것만으로 DB 필터가 생기지는 않는다.

**추가 문제:** 입력 이름이 `activeOnly`이고 “true일 때만 활성으로 제한, false·null이면 전체”가 계약이면 위 수정이 여전히 맞는가?

<details>
<summary>판단 확인</summary>

아니다. 그 계약에서는 기존 true 분기가 맞을 수 있다. 코드 모양이 같아도 입력의 세 값에 부여한 의미가 다르다. 버그 수정 전에 계약·호출자의 의도·재현 결과를 맞추는 이유다.

</details>

다른 패턴에서도 같은 방식으로 연습하려면 [직접 수정하기 위한 학습법](09-studying-to-change-code.md)을 따른다.

## 실제 소스에서 같은 패턴 확인하기

| 확인할 패턴 | 소스와 찾을 메서드 |
|---|---|
| 필수 범위 + 선택 조건·null 포함 제외 | [AppointmentQueries](../../../x-patient-appointment/backend/patient-appointment/src/main/kotlin/com/projectx/patientappointment/infrastructure/queries/AppointmentQueries.kt)의 `find` |
| 날짜와 시각 범위의 차이 | 같은 파일의 `dateRange` |
| 여러 열 OR + 필수 범위 AND | [ObservationQueries](../../../x-diagnostic-result/backend/diagnostic-result/src/main/kotlin/com/projectx/diagnosticresult/infrastructure/queries/ObservationQueries.kt)의 `patientListWhere` |
| 빈 후보이면 쿼리 생략 | 같은 파일의 `searchObservationsByCategory`·`searchObservationResultHistory` |
| 프로젝트 조회 기준 | [Persistence 규칙](../../../.claude/docs/rules/persistence.md) |

[이전: 조회 실행 시점](01-query-execution.md) · [목차](README.md) · [다음: 결과 바인딩](03-result-binding-patterns.md)
