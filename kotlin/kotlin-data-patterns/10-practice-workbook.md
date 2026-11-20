# 10. Kotlin 백엔드 데이터 패턴 실습장

[학습 목차](README.md) · [학습 방법](09-studying-to-change-code.md)

**새 프로젝트를 만들고 코드를 넣어 실행할 때는 [11장 단계별 실습](11-build-your-own-lab.md)을 먼저 진행한다.** 이 페이지는 실행한 코드의 결과를 예상하고 수정 이유를 적는 연습장으로 함께 사용한다.

**코드의 결과를 예상하고, 문제가 생긴 경계를 찾아 직접 수정하는 연습 페이지**다. C# 개발 경험을 바탕으로 QueryDSL·Kotlin·JPA의 실행과 데이터 전달 방식을 익힌다. 정답을 보기 전에 각 문제의 답안을 작성한다.

## 실습 방법

1. 입력과 요구사항을 읽고 **수정 전 결과**를 예상한다.
2. 확인할 변수와 중단점을 고르고, **바꿀 위치와 이유**를 적는다.
3. 답안의 코드 칸에 수정 코드를 작성한다.
4. 결과표를 채운 다음 접힌 해설과 비교한다.
5. 실행했다면 실제 결과를 기록하고, 실행하지 않았다면 예상 결과로 남긴다.

답안은 이 Markdown 파일을 편집해 작성한다. 각 문제는 독립적이며 앞 문제의 변경을 자동으로 이어받지 않는다. 코드는 import·DI·Entity 매핑 등을 생략한 **수정 연습용 조각**이다. 이 파일을 여는 것만으로 Kotlin이나 DB가 실행되지는 않는다. 실행 확인은 프로젝트의 기존 테스트 방식과 준비된 개발 환경에서 한다.

가상의 결함을 넣은 문제이며 실제 레포의 버그 보고가 아니다. 설명이 막히면 연결한 학습 장을 읽고 돌아온다.

| 완료 | 실습 | 익힐 판단 | 참고 |
|---|---|---|---|
| ☐ | [1. 조회 조건과 실행 순서](#실습-1-조회-조건과-실행-순서) | DB 처리와 메모리 처리 구분 | [1장](01-query-execution.md) |
| ☐ | [2. false와 빈 후보 목록](#실습-2-false와-빈-후보-목록) | 입력의 의미를 조건으로 표현 | [2장](02-filter-patterns.md) |
| ☐ | [3. 응답 필드 채우기](#실습-3-응답-필드-채우기) | select와 DTO 대입 연결 | [3장](03-result-binding-patterns.md) |
| ☐ | [4. LEFT JOIN 조건 위치](#실습-4-left-join-조건-위치) | 부모 행을 남기는 조건 | [4장](04-join-and-batch-patterns.md) |
| ☐ | [5. 페이지와 전체 건수](#실습-5-페이지와-전체-건수) | 같은 후보 집합으로 조회 | [5장](05-pagination-and-aggregation.md) |
| ☐ | [6. 저장 실패와 롤백](#실습-6-저장-실패와-롤백) | save·flush·commit 구분 | [6장](06-read-change-commit.md) |
| ☐ | [7. 외부 값을 로컬에 반영](#실습-7-외부-값을-로컬에-반영) | JSON·DTO·Entity 경계 연결 | [7장](07-external-data-patterns.md) |
| ☐ | [8. DB 커밋과 Kafka 발행](#실습-8-db-커밋과-kafka-발행) | 실패 시 남는 데이터 판단 | [8장](08-event-data-patterns.md) |
| ☐ | [9. 중복과 역순 이벤트](#실습-9-중복과-역순-이벤트) | 메시지와 원천 버전 구분 | [8장](08-event-data-patterns.md) |
| ☐ | [10. 실제 소스에 적용](#실습-10-실제-소스에-적용) | 필드 하나의 수정 범위 결정 | [9장](09-studying-to-change-code.md) |

## 공통 데이터와 약속

1~5번은 아래 데이터를 사용한다. 현재 조회 범위는 T1/H1이다. A·B·C·D·X와 C1·C2는 UUID의 축약 표기이며 실제 UUID 문자열이 아니다.

| Item id | tenantId | hospitalCode | name | active | amount | categoryId |
|---|---|---|---|---|---:|---|
| A | T1 | H1 | Alpha | true | 100 | C1 |
| B | T1 | H1 | Beta | false | 500 | null |
| C | T1 | H1 | Gamma | true | null | C1 |
| D | T1 | H1 | Omega | true | 700 | C2 |
| X | T2 | H1 | Zeta | true | 900 | C2 |

| Category id | name |
|---|---|
| C1 | 일반 |
| C2 | 특별 |

`Item`의 id는 `UUID`, amount는 `BigDecimal?`, categoryId는 `UUID?`다. 다른 프로퍼티는 표에 대응하는 비nullable 타입이다. Category는 조회가 허용된 참조 데이터이며 id가 유일하다. `q = QItem.item`, `c = QCategory.category`다.

```kotlin
// 문제에서 사용하는 필수 조회 범위
fun scopeCondition(scope: TenantScope): BooleanExpression =
    q.tenantId.eq(scope.tenantId)
        .and(q.hospitalCode.eq(scope.hospitalCode))
```

`queryFactory`는 `JPAQueryFactory`다. `scopePredicate`는 `scopeCondition(scope)`의 결과다. 목록은 달리 적지 않으면 `q.name.asc(), q.id.asc()`로 정렬한다. 예제 이름은 정렬상 Alpha → Beta → Gamma → Omega → Zeta 순서라고 가정한다. 요청 범위는 기존 인증 경로에서 얻는다.

## 실습 1. 조회 조건과 실행 순서

**요구:** 현재 범위의 활성 Item 중 이름 순으로 앞의 두 개를 반환한다.

```kotlin
val rows = queryFactory.selectFrom(q)
    .where(scopePredicate)
    .orderBy(q.name.asc(), q.id.asc())
    .limit(2)
    .fetch()
val result = rows.filter { it.active }.map { it.name }
```

**할 일:** DB가 반환한 id와 최종 이름 목록을 구분해 적는다. 항상 조건에 맞는 앞의 두 개를 가져오도록 수정한다. `asSequence()`를 넣는 것만으로 해결되는지도 설명한다.

```text
DB 실행 줄:
수정 전 rows의 id / result의 값:
고칠 위치와 이유:
fetch 직후 확인할 값:
```

```kotlin
// 내 수정 코드:
```

| 확인 조건 | 내가 예상한 수정 후 결과 | 실제 결과 또는 미실행 |
|---|---|---|
| 공통 데이터 | 작성 | 작성 |
| 내 범위가 모두 비활성 | 작성 | 작성 |
| 다른 범위의 활성 X가 존재 | 작성 | 작성 |

<details>
<summary>해설과 수정 예시</summary>

수정 전 DB 결과는 A·B다. 메모리에서 B를 제거해 result는 `[Alpha]`가 된다. 요구하는 결과는 `[Alpha, Gamma]`다.

```kotlin
val result = queryFactory.select(q.name).from(q)
    .where(scopePredicate, q.active.isTrue)
    .orderBy(q.name.asc(), q.id.asc())
    .limit(2)
    .fetch()
```

조건을 `fetch` 이전의 DB 조회식에 넣었다. 결과는 이름만 필요하므로 scalar select를 사용했다. Entity를 선택한 뒤 이름을 매핑해도 선택·페이지 의미가 같으면 정답이다.

내 범위가 모두 비활성이면 빈 목록이고 X는 제외된다. `asSequence()`는 이미 가져온 메모리 목록을 처리하는 방식만 바꾸므로 빠진 C를 가져오지 못한다. C#의 `ToList()` 앞뒤에서 `Where` 위치를 바꾸는 차이와 연결해 설명한다.

</details>

- [ ] `fetch` 앞뒤의 처리 위치와 반환 타입을 설명했다.
- [ ] 결과 행과 페이지 크기를 함께 확인할 방법을 적었다.

## 실습 2. false와 빈 후보 목록

**계약:** `candidateIds`는 별도 규칙으로 계산한 **조회 가능한 후보**다. 빈 목록은 후보 없음이다. `active`는 null이면 전체, true면 활성, false면 비활성이다.

```kotlin
val where = BooleanBuilder(scopePredicate)
if (candidateIds.isNotEmpty()) where.and(q.id.`in`(candidateIds))
if (active == true) where.and(q.active.isTrue)
return queryFactory.selectFrom(q).where(where)
    .orderBy(q.name.asc(), q.id.asc()).fetch()
```

**할 일:** 두 입력의 의미가 사라지는 분기를 찾고 수정한다. “사용자가 검색 필터를 선택하지 않은 빈 목록”에도 같은 수정이 맞는지 별도로 판단한다.

```text
발견한 두 원인:
중단점에서 확인할 입력과 분기:
다른 호출자의 candidateIds 계약을 확인할 위치:
```

```kotlin
// 내 수정 코드:
```

| candidateIds | active | 수정 전 id | 수정 후 기대 id |
|---|---|---|---|
| 빈 목록 | null | 작성 | 작성 |
| A, B | false | 작성 | 작성 |
| A, B | true | 작성 | 작성 |
| A, X | null | 작성 | 작성 |

<details>
<summary>해설과 수정 예시</summary>

```kotlin
if (candidateIds.isEmpty()) return emptyList()
val where = BooleanBuilder(scopePredicate)
    .and(q.id.`in`(candidateIds))
active?.let { where.and(q.active.eq(it)) }
return queryFactory.selectFrom(q).where(where)
    .orderBy(q.name.asc(), q.id.asc()).fetch()
```

| 입력 순서 | 수정 전 id | 수정 후 id |
|---|---|---|
| 1 | A, B, C, D | 없음 |
| 2 | A, B | B |
| 3 | A | A |
| 4 | A | A |

false는 값이 있는 Boolean이므로 `?.let` 안에 들어온다. 빈 후보는 조회식을 만들기 전에 빈 결과로 반환한다. 반면 “선택하지 않으면 전체”인 검색 필터는 빈 목록의 뜻이 다르므로 이 조기 반환을 그대로 복사하면 안 된다. 조건 함수의 사용처와 계약을 먼저 확인한다.

</details>

- [ ] null·false·빈 목록의 계약을 구분했다.
- [ ] X가 응답에 섞이지 않는 이유를 설명했다.

## 실습 3. 응답 필드 채우기

**요구:** 기존 응답에 categoryId를 추가한다. 참조가 없는 경우 null을 유지한다.

```kotlin
data class ItemView(val id: UUID, val name: String, val categoryId: UUID? = null)

val rows = queryFactory.select(q.id, q.name).from(q)
    .where(scopePredicate).orderBy(q.name.asc(), q.id.asc()).fetch()
val result = rows.map { row ->
    ItemView(id = requireNotNull(row.get(q.id)), name = requireNotNull(row.get(q.name)))
}
```

**할 일:** DTO에 필드는 있지만 값이 채워지지 않는 이유 두 가지를 찾는다. 수정 후 A·B의 값을 적고, `@JsonProperty`를 붙이기 전에 어느 객체를 확인할지 정한다.

```text
값이 빠진 두 위치:
조회 직후 확인할 표현식 / 매핑 직후 확인할 프로퍼티:
같은 DTO를 만드는 다른 코드의 영향:
```

```kotlin
// 내 수정 코드:
```

<details>
<summary>해설과 수정 예시</summary>

```kotlin
val rows = queryFactory.select(q.id, q.name, q.categoryId).from(q)
    .where(scopePredicate).orderBy(q.name.asc(), q.id.asc()).fetch()
val result = rows.map { row ->
    ItemView(
        id = requireNotNull(row.get(q.id)),
        name = requireNotNull(row.get(q.name)),
        categoryId = row.get(q.categoryId),
    )
}
```

A는 C1, B는 null이다. 선택 컬럼과 생성자 인자를 모두 연결해야 한다. DTO의 기본 인자가 null이면 대입을 빠뜨려도 컴파일될 수 있다. `!!`나 임의 식별자로 빈 참조를 감추지 않는다.

DTO까지 값이 맞는데 JSON만 다를 때 직렬화 설정과 다음 응답 변환을 확인한다. 같은 DTO의 다른 생성 경로와 생성자 projection도 사용처 검색으로 확인한다. 조회 필드 추가이므로 save는 필요하지 않다.

</details>

- [ ] select → Tuple → DTO의 대입을 직접 작성했다.
- [ ] A·B뿐 아니라 기존 name·id와 다른 조회의 응답도 확인 대상으로 적었다.

## 실습 4. LEFT JOIN 조건 위치

**요구:** 내 범위 Item을 모두 표시한다. 분류 이름이 “특”으로 시작할 때만 categoryName을 붙이고, 나머지는 null로 표시한다. 분류가 없는 Item도 남아야 한다.

```kotlin
val rows = queryFactory.select(q.id, c.name).from(q)
    .leftJoin(c).on(q.categoryId.eq(c.id))
    .where(scopePredicate, c.name.startsWith("특"))
    .orderBy(q.name.asc(), q.id.asc()).fetch()
```

**할 일:** 수정 전 반환 id를 예상하고 조건 하나의 위치를 바꾼다. `distinct()`로 해결할 수 있는지도 설명한다. 조인 결과는 Tuple이며 `row.get(c.name)`은 nullable로 받는다.

```text
수정 전 id:
ON이 제한하는 대상 / WHERE가 제한하는 대상:
```

```kotlin
// 내 수정 코드:
```

| 수정 후 Item | categoryName 예상값 | 실제 결과 또는 미실행 |
|---|---|---|
| A | 작성 | 작성 |
| B | 작성 | 작성 |
| C | 작성 | 작성 |
| D | 작성 | 작성 |

<details>
<summary>해설과 수정 예시</summary>

```kotlin
val rows = queryFactory.select(q.id, c.name).from(q)
    .leftJoin(c).on(q.categoryId.eq(c.id).and(c.name.startsWith("특")))
    .where(scopePredicate)
    .orderBy(q.name.asc(), q.id.asc()).fetch()
```

수정 전에는 D만 남는다. 수정 후에는 A·B·C·D가 남고 categoryName은 각각 null·null·null·특별이다. ON에서 분류의 결합 여부를 정하고, WHERE에서 Item의 범위만 제한했다. X는 제외된다.

이 문제는 행이 빠진 것이므로 distinct로 복원할 수 없다. 요구가 “특별 분류 Item만 표시”였다면 원래 WHERE 조건이 맞을 수 있다. 관련 없는 Entity의 JOIN+ON은 이 프로젝트의 Hibernate·QueryDSL 사용 형태를 전제로 한다.

</details>

- [ ] 참조가 없는 행과 참조는 있지만 조건이 다른 행을 모두 확인했다.
- [ ] 부모를 남길지 제외할지 요구사항으로 결정했다.

## 실습 5. 페이지와 전체 건수

**요구:** 최소 금액 이상인 Item을 이름 순으로 한 페이지에 1개 반환한다. total은 같은 조건의 전체 건수다. 최소 금액이 null이면 금액 제한을 생략한다.

입력은 `minimum = BigDecimal("400")`, `pageIndex = 0`, `pageSize = 1`이다.

```kotlin
val rows = queryFactory.select(q.id).from(q)
    .where(scopePredicate, minimum?.let { q.amount.goe(it) })
    .orderBy(q.name.asc(), q.id.asc())
    .offset(pageIndex.toLong() * pageSize).limit(pageSize.toLong()).fetch()
val total = queryFactory.select(q.id.count()).from(q)
    .where(scopePredicate).fetchOne() ?: 0L
```

**할 일:** 목록과 count가 공유할 조건을 만들고 두 쿼리에 적용한다. total을 `rows.size`로 바꾸지 않고 해결한다.

```text
수정 전 rows / total:
공유할 조건 / 공유하면 안 되는 페이지 설정:
```

```kotlin
// 내 수정 코드:
```

| minimum | pageIndex | 수정 후 rows | 수정 후 total |
|---|---:|---|---:|
| 400 | 0 | 작성 | 작성 |
| 400 | 1 | 작성 | 작성 |
| 400 | 2 | 작성 | 작성 |
| null | 0 | 작성 | 작성 |
| 701 | 0 | 작성 | 작성 |

<details>
<summary>해설과 수정 예시</summary>

```kotlin
val filter = BooleanBuilder(scopePredicate)
minimum?.let { filter.and(q.amount.goe(it)) }
val rows = queryFactory.select(q.id).from(q)
    .where(filter).orderBy(q.name.asc(), q.id.asc())
    .offset(pageIndex.toLong() * pageSize).limit(pageSize.toLong()).fetch()
val total = queryFactory.select(q.id.count()).from(q)
    .where(filter).fetchOne() ?: 0L
```

filter는 이 호출의 지역 변수다. 두 쿼리에 전달한 뒤 추가 변경하지 않는다. 공통 함수를 통해 조건을 만드는 형태도 가능하다.

수정 전 첫 페이지는 B이고 total은 4다. 수정 후 결과는 순서대로 **B/2, D/2, 빈 목록/2, A/4, 빈 목록/0**이다. amount=null인 C는 금액 조건이 있을 때 제외된다. X는 금액이 커도 scope 밖이다.

`rows.size`는 현재 페이지 크기다. count에 offset·limit을 넣으면 전체 건수의 의미가 깨진다. 이 기대값은 조회 사이에 데이터가 바뀌지 않는 고정 fixture를 전제로 한다.

</details>

- [ ] 첫 페이지·마지막 페이지·범위 밖 페이지·빈 결과를 구분했다.
- [ ] 새 조건을 추가할 때 목록과 count를 함께 확인할 수 있다.

## 실습 6. 저장 실패와 롤백

**요구:** 변경을 거절하면 DB 이름도 이전 값으로 남아야 한다. 초기 이름은 Alpha, 요청 이름은 Beta다.

아래 메서드는 다른 객체가 Spring 프록시를 통해 호출하며 외곽 트랜잭션은 없다. Repository는 같은 DB 트랜잭션에 참여한다. 이 실습에는 외부 호출·발행이 없고 트랜잭션을 별도로 rollback-only로 표시하는 코드도 없다.

```kotlin
@Transactional
fun handle(scope: TenantScope, id: UUID, name: String, rejectForExercise: Boolean): Boolean {
    val item = repository.findById(scope, id) ?: throw itemNotFound()
    item.changeName(name)
    repository.saveAndFlush(item)
    if (rejectForExercise) return false
    return true
}
```

**할 일:** false를 반환하면 자동 rollback되는지 판단한다. 거절을 업무 오류로 응답하는 계약으로 수정하되 실제 DB 변경도 남지 않게 한다. `changeRejected()`는 프로젝트 방식으로 만드는 **RuntimeException 계열 업무 오류**를 뜻한다.

```text
reject=true의 기존 반환값 / 메서드 종료 후 DB 이름:
saveAndFlush 직후 확정된 것 / 아직 확정되지 않은 것:
수정할 위치와 오류가 전파되어야 할 경계:
```

```kotlin
// 내 수정 코드:
```

<details>
<summary>해설과 수정 예시</summary>

원래 코드는 false로 정상 반환하므로 rollback 사유가 없으면 commit되고 이름이 Beta로 남는다. 반환 Boolean은 트랜잭션 제어 명령이 아니다.

```kotlin
@Transactional
fun handle(scope: TenantScope, id: UUID, name: String, rejectForExercise: Boolean): Boolean {
    val item = repository.findById(scope, id) ?: throw itemNotFound()
    if (rejectForExercise) throw changeRejected()
    item.changeName(name)
    repository.save(item)
    return true
}
```

거절을 변경 전에 알 수 있어 먼저 판단했다. 오류는 트랜잭션 프록시 밖까지 전파되고 그 뒤 API의 기존 오류 변환에서 응답으로 만든다. 같은 메서드 안에서 오류를 잡아 false로 반환하는 방식은 이 의도를 다시 깨뜨릴 수 있다.

saveAndFlush는 SQL 동기화까지 수행하지만 commit은 아니다. 변경 후에야 거절을 판단할 수 있더라도 RuntimeException이 이 트랜잭션 경계 밖으로 전파되면 이미 flush한 SQL도 rollback 대상이다. 다른 트랜잭션에서 이미 commit한 변경이나 외부 시스템의 작업은 이 설명의 대상이 아니다.

각 입력 전에 Alpha로 초기화한다. 거절하면 업무 오류와 Alpha 유지, 허용하면 true와 commit 뒤 Beta를 확인한다. 별도 조회에서 값을 확인하며 동일 관리 객체의 필드만 보고 저장 완료로 판단하지 않는다. 실제 동시성·expectedVersion 정책은 기존 쓰기 경로를 유지한다.

</details>

- [ ] save·flush·commit의 차이를 이번 코드의 줄과 연결했다.
- [ ] 거절과 허용 두 경로의 DB 기대값을 적었다.

## 실습 7. 외부 값을 로컬에 반영

**요구:** 외부 표시 이름이 있으면 로컬 이름을 갱신한다. 외부 이름이 null이면 기존 이름을 유지하고, 빈 문자열이면 업무 오류다. 외부 식별자는 String, 로컬 id는 UUID로 서로 다르며 연결 관계는 이미 확인됐다.

```kotlin
data class RemoteItem(
    @JsonProperty("item_name") val name: String?,
)
data class RefreshInput(val name: String?)

// 호출자에는 DB 트랜잭션이 없다. requestUri는 인증·범위가 설정된 원격 GET 주소다.
val payload = client.get().uri(requestUri).retrieve()
    .body(RemoteItem::class.java) ?: throw emptyRemoteBody()
val input = RefreshInput(name = null)
writer.refresh(scope, localId, input)
```

`writer`는 별도 Spring Bean이다. 아래 메서드가 쓰기 트랜잭션이며 `item.changeName`은 빈 문자열을 RuntimeException 계열 업무 오류로 거절한다. HTTP 응답의 `item_name`은 `New name`인데 commit 뒤 로컬 GET은 계속 `Old name`을 반환한다.

```kotlin
@Transactional
fun refresh(scope: TenantScope, localId: UUID, input: RefreshInput) {
    val item = repository.findById(scope, localId) ?: throw itemNotFound()
    repository.save(item)
}
```

**할 일:** 원격 DTO → 입력 → Entity의 두 누락을 채운다. `.body(...)`, 입력 생성, 도메인 변경, commit을 각각 표시한다. null을 빈 문자열로 대체해도 되는지 판단한다.

```text
payload.name / input.name / 변경 직후 item.name의 예상값:
두 수정 위치:
원격 조회 성공 후 로컬 저장이 실패하면 남는 값:
```

```kotlin
// 내 수정 코드:
```

<details>
<summary>해설과 수정 예시</summary>

```kotlin
val input = RefreshInput(name = payload.name)
writer.refresh(scope, localId, input)

// writer 메서드 내부: scope로 Entity를 읽은 다음
input.name?.let { item.changeName(it) }
repository.save(item)
```

각 사례의 초기 로컬 이름을 Old name으로 둔다. 원격 `New name`이면 새 GET도 New name, null이면 Old name 유지, 빈 문자열이면 업무 오류와 Old name 유지다. Kotlin `?.let`은 빈 문자열도 실행하므로 도메인 규칙이 거절한다.

`payload.name ?: ""`로 바꾸면 null이 오류 경로가 되어 요구를 바꾼다. JSON 역직렬화 성공만으로 로컬 Entity가 갱신되지 않는다. 먼저 payload, 다음 input, 마지막 Entity 값에 중단점을 두면 누락 위치를 구분할 수 있다.

이 코드에서는 HTTP 호출이 끝난 뒤 writer의 새 DB 트랜잭션이 열린다. 로컬 저장이 실패하면 기존 로컬 값은 유지된다. 원격 DTO를 읽었다는 사실은 로컬 commit 성공이 아니다. 새 메서드를 기존 트랜잭션 안에서 호출하게 바꾼다면 이 시간 순서도 다시 확인한다.

</details>

- [ ] null과 빈 문자열을 서로 다른 입력으로 검증했다.
- [ ] 네트워크 응답 바인딩과 영속 상태 대입을 구분했다.

## 실습 8. DB 커밋과 Kafka 발행

**요구:** DB 변경이 rollback되면 그 변경의 메시지가 발행되지 않아야 한다. DB가 commit된 뒤 프로세스가 중단돼도 재전송할 근거가 남아야 한다.

```kotlin
@Transactional
fun change(scope: TenantScope, id: UUID, name: String) {
    val item = repository.findById(scope, id) ?: throw itemNotFound()
    item.changeName(name)
    repository.save(item)
    eventBus.publish(snapshotOf(item))
    // 이후 DB commit은 실패할 수도 있다.
}
```

이 문제의 `eventBus.publish`는 외부 Kafka 발행이며 **DB 트랜잭션에 참여하지 않는다**. Kafka에 먼저 전달됐는데 DB commit이 실패하는 경우를 생각한다. `snapshotOf`는 발행할 값을 복사하는 설명용 함수다.

**할 일:** 업무 Entity와 전송할 payload를 같은 DB·같은 트랜잭션에 저장하는 outbox 형태로 바꾼다. 실제 송신은 별도 전송기가 맡도록 두 단계로 적는다. AFTER_COMMIT으로 옮기는 것만으로 위 요구를 모두 만족하는지, Kafka 발행 성공만으로 상대 서비스의 조회값까지 바뀌었다고 판단할 수 있는지도 답한다.

```text
기존 코드에서 DB 실패 후에도 외부에 남을 수 있는 것:
업무 트랜잭션에 넣을 작업:
별도 전송기에 넣을 작업:
발행 성공 후 전송 상태 기록 전에 중단되면:
```

```kotlin
// 내 업무 트랜잭션 수정 코드:
```

<details>
<summary>해설과 수정 예시</summary>

```kotlin
@Transactional
fun change(scope: TenantScope, id: UUID, name: String) {
    val item = repository.findById(scope, id) ?: throw itemNotFound()
    item.changeName(name)
    repository.save(item)
    outbox.store(snapshotOf(item))
}
```

`outbox.store`는 같은 DB·같은 트랜잭션의 저장이다. 별도 트랜잭션으로 먼저 commit하거나 내부에서 Kafka를 바로 호출하면 이 해설이 성립하지 않는다. 기존 프로젝트에 있는 전송 정책과 구현을 사용한다.

```text
업무 단계: Entity 변경 + outbox 저장 → 함께 commit
전송 단계: commit된 미전송 outbox 선택 → Kafka 발행 → 전송 결과 기록
```

| 실패 위치 | 남는 상태와 확인할 동작 |
|---|---|
| 업무 commit 전 실패 | Entity 변경·새 outbox가 함께 rollback, 이 작업의 발행 없음 |
| 업무 commit 후, 발행 전 중단 | 미전송 outbox가 남아 전송기가 재시도 가능 |
| 발행 성공 후, 완료 기록 전 중단 | 재발행될 수 있으므로 수신 측 중복 처리 필요 |

AFTER_COMMIT 직접 발행은 rollback된 변경을 보내는 문제를 줄이지만, commit 후 발행 전에 프로세스가 중단되었을 때 재전송할 기록까지 자동으로 만들지는 않는다. outbox도 DB·Kafka를 하나의 원자적 commit으로 묶거나 중복을 없애는 장치는 아니다.

Kafka 발행 성공과 상대 서비스의 업무 반영 완료도 다르다. 상대가 inbox 원문만 저장하고 아직 처리하지 않았다면 상대 조회는 이전 값을 반환할 수 있다. 상대 업무 트랜잭션의 commit까지 구분해야 하며 다음 실습에서 그 적용 조건을 다룬다.

이 실습은 패턴 판단이다. 실제 레포의 발행 경로가 다를 수 있으므로 모든 서비스에 새 outbox를 추가하라는 뜻으로 적용하지 않는다.

</details>

- [ ] rollback·발행 전 중단·발행 후 중단을 각각 설명했다.
- [ ] DB 저장 성공과 Kafka 발행 성공을 별도로 확인할 수 있다.

## 실습 9. 중복과 역순 이벤트

**계약:** 원천은 항목별로 증가하는 필수 `sourceVersion`과 전체 상태를 보낸다. 기존 로컬 항목의 초기 상태는 `sourceVersion=12`, name=Beta다. 로컬 JPA `@Version` 필드는 따로 있다. 아래 메시지를 순서대로 처리한다.

| 순서 | messageId | sourceVersion | name |
|---:|---|---:|---|
| 1 | M1 | 11 | Alpha |
| 2 | M2 | 12 | Beta |
| 3 | M3 | 13 | Gamma |
| 4 | M4 | 12 | Beta |

처리 대상 inbox 메시지와 기존 Entity는 같은 업무 트랜잭션의 관리 객체다. 메시지 scope와 원천 id 검증은 끝났다. 과거·동일 버전은 정상 no-op로 완료 처리하는 계약이다.

```kotlin
// 변경 전: 버전 비교 없이 적용
existing.applySource(payload)
repository.save(existing)
message.markProcessed()
```

**할 일:** `applySource` 전에 비교 조건을 넣는다. 메시지마다 처리 후 이름과 원천 버전을 적는다. messageId가 모두 다르다는 이유로 네 메시지를 전부 적용해도 되는지 답한다.

```kotlin
// 내 수정 코드:
```

| 처리 후 | name | sourceVersion | inbox의 기대 상태 |
|---|---|---:|---|
| M1 | 작성 | 작성 | 작성 |
| M2 | 작성 | 작성 | 작성 |
| M3 | 작성 | 작성 | 작성 |
| M4 | 작성 | 작성 | 작성 |

<details>
<summary>해설과 수정 예시</summary>

```kotlin
if (payload.sourceVersion > existing.sourceVersion) {
    existing.applySource(payload) // 이름과 sourceVersion을 함께 대입
    repository.save(existing)
}
message.markProcessed()
```

결과는 순서대로 **Beta/12, Beta/12, Gamma/13, Gamma/13**이다. 모두 정상 처리 완료다. 업무 변경과 PROCESSED 상태는 함께 commit되어야 한다.

messageId는 메시지를 구분하고 sourceVersion은 원천 상태의 신구를 구분한다. 서로 다른 메시지가 같은 버전이나 과거 버전을 전달할 수 있다. JPA version은 로컬 DB 동시 변경 충돌을 감지하는 별도 값이다.

이 문제는 직렬 처리되는 기존 항목의 갱신이다. 동시 처리에서는 두 작업이 같은 이전 상태를 읽을 수 있으므로 기존 잠금·낙관적 충돌·재시도에서 새 상태를 읽는 경로도 확인한다. 최초 생성, 삭제, 버전 없는 payload, 증분 이벤트에는 별도 계약이 필요하다.

</details>

- [ ] 세 식별자·버전의 역할을 구분했다.
- [ ] no-op와 업무 실패를 같은 것으로 처리하지 않았다.

## 실습 10. 실제 소스에 적용

이제 작은 예제에서 사용한 이름을 실제 코드의 이름에 대응시킨다. 아래에서 **한 가지 패턴**을 골라 진행한다. 처음에는 읽기와 수정안 작성으로 끝내도 된다. 제품 코드를 바꿔 실행했다면 어떤 범위를 확인했는지 별도로 적는다.

| 선택 | 시작할 소스 | 과제 |
|---|---|---|
| A | [AppointmentQueries](../../../x-patient-appointment/backend/patient-appointment/src/main/kotlin/com/projectx/patientappointment/infrastructure/queries/AppointmentQueries.kt)의 `find`, `views` | 기존 선택 조건 하나와 응답 필드 하나의 전달 경로를 각각 추적 |
| B | [ObservationQueries](../../../x-diagnostic-result/backend/diagnostic-result/src/main/kotlin/com/projectx/diagnosticresult/infrastructure/queries/ObservationQueries.kt)의 `categoryQuery`와 호출부 | 선택한 표현식이 Tuple에서 꺼내져 응답에 들어가는 지점을 찾기 |
| C | [OrderingQueries](../../../x-clinical-care/backend/ordering/src/main/kotlin/com/projectx/ordering/application/queries/OrderingQueries.kt)의 `getOrderSheet` | 단건 실행, null 처리, 결과 변환을 구분하고 담당 소스와 비교 |

**과제:** 기존 필드 하나를 선택하고 “이 필드에 조회 조건을 추가하거나 응답에서 값을 바꿔야 한다면”을 가정한다. 먼저 실제 입력·타입·조건·매핑을 확인한 다음 수정안을 적는다. 요구사항이 확정되지 않은 동작은 가정이라고 표시한다.

```text
선택한 패턴 / 실제 파일과 함수:
추적할 필드와 Kotlin 타입:
입력값을 처음 받는 곳:
Filter나 Command로 복사하는 곳:
DB 또는 HTTP가 실행되는 줄:
Entity / Tuple / DTO 중 실제 반환 타입:
응답 필드를 채우는 곳:

가정한 변경 요구:
현재 입력 → 현재 결과 → 원하는 결과:
바꿀 코드와 이유:
함께 확인할 select / mapper / count / API 계약:
사용처 검색으로 찾은 다른 영향:
```

```diff
 내 변경안:
```

| 확인할 입력·사전 상태 | 수정 전 예상 | 수정 후 기대 | 실제 결과 또는 미실행 |
|---|---|---|---|
| 정상 사례 | 작성 | 작성 | 작성 |
| null·빈 목록 등 해당 필드의 경계 사례 | 작성 | 작성 | 작성 |
| 기존 호출자가 입력을 생략한 사례 | 작성 | 작성 | 작성 |
| 범위 밖 데이터가 함께 존재하는 사례 | 작성 | 작성 | 작성 |
| 같은 매퍼·조건을 쓰는 다른 호출 | 작성 | 작성 | 작성 |

<details>
<summary>스스로 검토할 기준</summary>

파일 이름을 찾은 것에서 끝내지 않고 **한 값이 어떤 객체를 거쳐 어디에서 읽히고 복사되는지**를 설명했는지 본다. “이 줄에서 왜 고치는가”가 증상과 연결되어야 한다.

- 조건을 추가한다면 요청·Filter·목록·count의 전달을 확인한다.
- 응답 필드를 추가한다면 실제 조회 패턴에 따라 select·생성자 projection·매퍼를 확인한다.
- 저장값을 바꾼다면 Entity 변경·Repository 경로·트랜잭션·동시성 정책을 확인한다.
- 외부 데이터라면 원격 응답이나 수신 payload의 값이 로컬 상태에 대입되는 위치를 확인한다.
- 테스트가 mock 결과만 비교하는지, 바꾼 조회·바인딩·저장 경계도 실행하는지 구분한다.

실제 기능 변경에 들어갈 때는 대상 프로젝트의 설계·API와 로컬 지침을 확인한다. 모델·계약 변경은 프로젝트의 설계 우선 절차에 맞춰 문서와 코드를 연결한다. 이 실습에 정해진 하나의 파일 목록이나 정답 diff는 없다. 선택한 필드와 호출 경로로 근거를 제시하는 것이 완료 기준이다.

</details>

- [ ] 값이 지나는 경로와 타입을 소스에서 확인했다.
- [ ] 최소 수정안과 다른 호출의 영향을 설명했다.
- [ ] 실제 실행 결과와 아직 예상만 한 결과를 구분했다.

## 실습을 마친 뒤 남길 메모

```text
이제 스스로 고칠 수 있는 유형:
아직 혼동하는 타입·실행·저장 경계:
다시 읽을 학습 장:
다음에 담당 소스에서 추적할 필드:
```

[학습 목차](README.md) · [학습 방법](09-studying-to-change-code.md)
