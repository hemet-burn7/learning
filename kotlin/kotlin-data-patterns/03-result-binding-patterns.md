# 3. 조회 결과를 객체에 담는 세 가지 패턴

[이전: 조회 조건](02-filter-patterns.md) · [목차](README.md) · [다음: 조인과 일괄 조회](04-join-and-batch-patterns.md)

조회 코드를 읽을 때 가장 먼저 물을 것은 **“DB에서 무엇을 선택했고, 그 값을 어떤 객체에 넣었는가?”**다. 같은 `fetch()`라도 Entity, DTO, `Tuple` 중 무엇을 선택했는지에 따라 다음 코드가 달라진다. 여기서 바인딩은 그 값을 객체의 필드·생성자 인자에 연결하는 과정이다.

## 3.1 예제의 입력과 공통 조건

아래는 설명용 `Item` 테이블이다. A·B는 표를 짧게 쓴 id 별칭이고 실제 타입은 UUID다. 모든 행은 같은 허용 tenant·hospital 범위라고 가정한다.

| id | name | amount | active |
|---|---|---:|---|
| A | 연필 | 500 | true |
| B | 공책 | NULL | true |
| C | 지우개 | 300 | false |

C#은 EF Core의 `IQueryable`, Kotlin은 QueryDSL JPA를 사용한 **의미 비교용 예제**다. 실제 기능의 복사본이 아니며 import·Entity 매핑·DI 설정은 생략했다. 다음 조건을 모든 예제에서 유지한다.

```csharp
var scoped = db.Items.Where(x =>
    x.TenantId == tenantId && x.HospitalCode == hospitalCode);
```

```kotlin
val i = QItem.item
val scopePredicate = i.tenantId.eq(scope.tenantId)
    .and(i.hospitalCode.eq(scope.hospitalCode))

data class ItemView(val id: UUID, val name: String, val amount: BigDecimal?)
```

`QItem`은 쿼리 구성용 경로이고 `Item` 데이터 객체가 아니다. `ItemView`는 응답용 일반 객체다. Kotlin의 `data class`에 들어온 값을 바꿔 새 객체를 만들어도 DB 저장이 자동 실행되지는 않는다.

## 3.2 패턴 A: Entity를 읽은 뒤 DTO로 변환한다

**문제:** 조회한 객체의 속성·관계를 이용해 응답을 구성해야 한다. 기존 코드가 Entity를 내부 로딩하는 방식이면, Entity가 생성되는 시점과 응답 DTO가 생성되는 시점을 따로 읽어야 한다.

```csharp
var entities = await scoped.Where(x => x.Active).ToListAsync();
var views = entities
    .Select(x => new ItemView(x.Id, x.Name, x.Amount))
    .ToList();
```

```kotlin
val entities: List<Item> = queryFactory
    .selectFrom(i)
    .where(scopePredicate, i.active.isTrue)
    .fetch()

val views: List<ItemView> = entities.map { entity ->
    ItemView(id = entity.id, name = entity.name, amount = entity.amount)
}
```

| 코드 | 하는 일 | 실행 위치 |
|---|---|---|
| `selectFrom(i)` | Entity를 결과 타입으로 선택 | 쿼리 구성 |
| `where(...)` | 허용 범위와 활성 조건 추가 | 쿼리 구성 |
| `fetch()` | 쿼리를 실행하고 Hibernate가 Entity 상태를 로딩 | DB → 영속성 컨텍스트 |
| `entities.map` | 이미 받은 목록을 순회 | 애플리케이션 메모리 |
| `ItemView(...)` | 각 Entity 값을 새 응답 객체에 복사 | 애플리케이션 메모리 |

결과는 `[ItemView(A, "연필", 500), ItemView(B, "공책", null)]`이다. **DB 값이 Entity에 들어가는 과정과 DTO에 복사되는 과정이 두 번 보인다.** 같은 영속성 컨텍스트에 이미 관리되는 Entity가 있으면 해당 인스턴스가 재사용될 수 있다.

이 패턴에서는 전체 Entity 로딩 비용과 관계 접근을 확인한다. `map` 안에서 `entity.details` 같은 LAZY 컬렉션을 읽으면 추가 SQL이 나갈 수 있다. “fetch 뒤이므로 그 아래는 전부 DB 접근 없는 코드”라고 단정하지 않는다. 선택한 값이 단순 프로퍼티인지, 지연 관계인지가 중요하다.

응답으로 Entity를 그대로 보내지는 않는다. 이 프로젝트의 조회 계약은 ViewModel이며, 읽기 경로에서 Entity의 업무 변경 메서드를 호출하지 않는다. Entity를 읽었다는 이유만으로 이 패턴을 모든 새 조회의 기본값으로 삼을 필요도 없다.

## 3.3 패턴 B: 필요한 컬럼을 골라 DTO 생성자에 바로 넣는다

**문제:** 목록에는 id·이름·금액 세 값만 필요하다. Entity 전체와 변경 추적 상태가 필요하지 않다. 이때 필요한 컬럼만 선택하고 반환 DTO를 만드는 *생성자 projection*이 적합하다.

```csharp
var views = await scoped
    .Where(x => x.Active)
    .Select(x => new ItemView(x.Id, x.Name, x.Amount))
    .ToListAsync();
```

```kotlin
val views: List<ItemView> = queryFactory
    .select(
        Projections.constructor(
            ItemView::class.java,
            i.id,
            i.name,
            i.amount,
        )
    )
    .from(i)
    .where(scopePredicate, i.active.isTrue)
    .fetch()
```

1. `ItemView::class.java`는 **만들 결과 클래스**를 지정한다. Kotlin 프로퍼티 하나를 조회하는 표현이 아니다.
2. `i.id`, `i.name`, `i.amount`는 DB에서 선택할 값과 **생성자 인자 순서**를 지정한다.
3. `fetch()`에서 쿼리가 실행되고, 반환된 컬럼 값으로 각 행의 `ItemView`가 구성된다.
4. 반환값이 이미 `List<ItemView>`이므로 별도의 `entities.map`이 필요 없다.

개념적으로 DB에서 필요한 세 컬럼을 가져온 뒤 애플리케이션이 DTO 생성자를 호출한다. **DB가 Kotlin 객체를 만들어 보내는 것은 아니다.** 실제 SQL의 추가 컬럼·정렬 처리 등은 JPA provider가 결정한다. QueryDSL의 행 단위 결과 변환과 생성자 projection은 [공식 결과 처리 설명](https://querydsl.com/static/querydsl/latest/reference/html/ch03s02.html)에서 확인할 수 있다.

패턴 A와 반환 데이터는 같을 수 있지만 중간 결과가 다르다.

| 구분 | 패턴 A | 패턴 B |
|---|---|---|
| select 대상 | Entity | 필요한 컬럼 |
| fetch의 결과 | `List<Item>` | `List<ItemView>` |
| DTO 생성 코드 | fetch 다음 `map` | projection 정의 |
| 영속 Entity 상태 | 내부에 존재 | 이 예제는 scalar DTO만 반환 |
| 유용한 경우 | 기존 Entity 기반 조립을 읽을 때 | 단순 목록·단건 응답 |

### 이름이 아니라 순서와 타입을 맞춘다

`ItemView(id, name, amount)`인데 select에 `id, amount, name`을 넘기면 올바른 생성자를 찾지 못하거나 변환 오류가 날 수 있다. 일반 `Projections.constructor`는 DTO 인자 순서까지 컴파일러가 완전히 검사해 주는 방식이 아니다.

특히 `(name: String, categoryName: String)`처럼 같은 타입이 연속되면 순서를 바꾸어도 다른 값이 들어간 채 실행될 수 있다. DTO 프로퍼티명과 컬럼명이 같다는 사실은 자동 이름 매핑을 의미하지 않는다. C# 코드의 명시적 생성자 인자 순서를 읽듯 대조한다.

## 3.4 패턴 C: 여러 컬럼을 Tuple로 받고 Kotlin에서 조립한다

**문제:** 응답 필드가 단순 복사가 아니라, 여러 선택값 중 우선순위를 적용하거나 null 정책을 설명해야 하는 값이다. 필요한 컬럼을 읽고 조립 코드를 명확히 드러내려면 `Tuple`을 사용할 수 있다.

C#에서는 먼저 익명 타입으로 필요한 값을 받고 메모리에서 응답을 만드는 코드와 비교한다.

```csharp
var rows = await scoped.Where(x => x.Active)
    .Select(x => new { x.Id, x.Name, x.Amount }).ToListAsync();
var views = rows.Select(x => new ItemDisplayView(
    x.Id, x.Name, x.Amount == null ? "미입력" : x.Amount.ToString()
)).ToList();
```

```kotlin
data class ItemDisplayView(val id: UUID, val name: String, val amountText: String)

val rows: List<Tuple> = queryFactory
    .select(i.id, i.name, i.amount)
    .from(i)
    .where(scopePredicate, i.active.isTrue)
    .fetch()

val views = rows.map { row ->
    ItemDisplayView(
        id = requireNotNull(row.get(i.id)),
        name = requireNotNull(row.get(i.name)),
        amountText = row.get(i.amount)?.toPlainString() ?: "미입력",
    )
}
```

금액 문자열은 메모리 변환을 보이기 위한 예시이며 통화 표시·로캘 정책을 구현한 것은 아니다.

| 입력 행 | `row.get(i.amount)` | 출력 `amountText` |
|---|---|---|
| A | `BigDecimal(500)` | `"500"` |
| B | `null` | `"미입력"` |

`Tuple`은 여러 선택값을 담는 행 객체다. `row.get(i.amount)`는 이 행의 선택 결과를 읽는다. **DB에 amount를 다시 조회하는 함수가 아니다.** `fetch()`와 `map` 사이에서 DB 실행과 메모리 조립이 갈린다.

select에 `i.amount.coalesce(BigDecimal.ZERO)` 같은 **다른 표현식**을 사용했다면, Tuple을 읽을 때도 그 표현식을 변수에 보관해 사용한다. 선택하지 않은 원래 `i.amount`로 읽고 자동 대응을 기대하지 않는다. 여러 같은 타입의 컬럼을 위치 번호로 읽는 코드는 순서 변경에 특히 주의한다.

## 3.5 null과 기본값은 어느 단계에서 정하는가

| 코드 | 적용 위치 | 의미 |
|---|---|---|
| select의 `i.amount.coalesce(BigDecimal.ZERO)` | DB 쿼리 | NULL을 0으로 선택 |
| `row.get(i.amount) ?: BigDecimal.ZERO` | Kotlin 메모리 | 가져온 null을 0으로 변환 |
| `val amount: BigDecimal?` | Kotlin 타입 | 결과가 null일 수 있음을 허용 |
| `val amount: BigDecimal = ZERO` | 생성자 기본 인자 | 인자를 생략한 Kotlin 호출의 기본값 |

생성자 기본값은 **DB의 null을 자동으로 대체하는 정책이 아니다.** projection이 인자로 null을 넘기는 상황과 생성자 인자를 생략한 호출은 다르다. LEFT JOIN으로 연결된 값은 원래 테이블의 컬럼이 NOT NULL이어도 연결 행이 없으면 조회 결과가 null일 수 있다.

`!!`로 오류를 없애려 하지 않는다. 정말 필수인 id·이름이 누락되면 매핑 결함으로 드러내고, 선택값은 nullable로 두거나 업무상 합의된 대체값을 사용한다. “금액 미입력”과 “금액 0”을 구분해야 하는 조회에서 무조건 0을 넣으면 의미가 사라진다.

## 3.6 응답 JSON으로 바뀌는 것은 그다음이다

```text
select/where 구성
  → fetch: DB 실행
  → Entity / DTO / Tuple에 값 로딩
  → 필요한 경우 map으로 최종 ViewModel 구성
  → Controller 반환
  → Jackson이 ViewModel을 JSON으로 직렬화
```

`@Column(name = "item_name")`은 Entity와 DB 컬럼의 연결이고 `@JsonProperty("itemName")`은 JSON 키의 연결이다. 둘은 서로 다른 경계다. DTO의 프로퍼티를 추가한다고 select가 자동으로 늘어나지 않으며, JSON 키를 바꾼다고 DB 컬럼이 바뀌지 않는다.

값이 응답에서 빠졌다면 **select에 포함 → Tuple/Entity에 로딩 → DTO 생성자 대입 → 직렬화 정책** 순서로 확인한다. DTO를 만드는 줄에서 값이 이미 없으면 JSON 바인딩을 먼저 고칠 일이 아니다.

## 3.7 어떤 패턴을 고를까

- 단순한 필드 응답은 필요한 컬럼을 선택하는 생성자 projection부터 검토한다.
- 여러 컬럼의 결합·null·표시 규칙을 명시해야 하면 Tuple 후 변환을 검토한다.
- 이미 Entity·관계 기반으로 조립하는 조회를 읽을 때는 Entity→DTO 변환과 지연 로딩을 함께 확인한다.
- DTO 안에 자식 `List`가 있다고 단일 행 생성자 projection이 자동으로 중첩 목록을 만들어 주지는 않는다. 여러 행을 묶는 패턴은 [4장](04-join-and-batch-patterns.md)에서 다룬다.

## 3.8 직접 수정해 보기: 응답 필드를 추가했는데 항상 null이다

다음은 **의도적으로 만든 학습용 사례**이며 실제 소스의 결함 보고가 아니다. 새 요구는 “응답에 저장된 `categoryId`도 포함한다”이다. Item 테이블의 기존 컬럼을 읽는 것이므로 여기서는 새 DB 컬럼을 추가하지 않는다.

### 필드 선언만으로 조회값이 연결되는가

이 장의 DTO에 다음 필드를 추가했다고 하자.

```kotlin
// 설명용 변경: 기본값 때문에 기존 생성자 호출도 계속 컴파일될 수 있다.
data class ItemView(
    val id: UUID,
    val name: String,
    val amount: BigDecimal?,
    val categoryId: UUID? = null,
)
```

DB에는 A의 categoryId가 C1인데 응답에는 null이 나온다. 조회 구현은 여전히 다음과 같다.

```kotlin
// 잘못된 설명용 코드: 새 필드의 select와 대입을 둘 다 빠뜨렸다.
val rows = queryFactory.select(i.id, i.name, i.amount)
    .from(i).where(scopePredicate).fetch()
val views = rows.map { row ->
    ItemView(
        id = requireNotNull(row.get(i.id)),
        name = requireNotNull(row.get(i.name)),
        amount = row.get(i.amount),
    )
}
```

스스로 먼저 판단한다. “DB에 실제 값이 없는가, 조회에 포함되지 않았는가, 조회했지만 DTO에 넣지 않았는가, DTO에는 있는데 직렬화에서 빠졌는가?” **API에 null이 보인다는 증상만으로 Jackson 설정을 고르지 않는다.**

### 중단점을 걸어 값이 끊긴 경계를 찾는다

| 관측 위치 | 확인할 내용 | 이 예제에서 발견할 것 |
|---|---|---|
| select 구성 | `i.categoryId`를 선택했는가 | 선택 목록에 없음 |
| fetch 직후 Tuple | 선택한 표현식과 실제 행 값 | categoryId의 선택 결과를 확보하지 않음 |
| `ItemView(...)` 생성 | categoryId 인자를 넘기는가 | 생략되어 기본값 null 사용 |
| mapper 반환 직후 | DTO의 categoryId | 이미 null이므로 직렬화 이전 문제 |
| Controller 반환 경계 | DTO 값과 응답 계약 | DTO 값이 정상일 때 직렬화 정책 확인 |

첫 수정 후에도 null이면 관측을 반복한다. select에 값을 추가했어도 생성자에서 인자를 생략하면 기본값을 계속 쓴다. Tuple의 열 선택과 DTO의 프로퍼티 추가는 서로 자동 연결되지 않는다.

### select와 명시 대입을 함께 고친다

```diff
-val rows = queryFactory.select(i.id, i.name, i.amount)
+val rows = queryFactory.select(i.id, i.name, i.amount, i.categoryId)
     .from(i).where(scopePredicate).fetch()
 val views = rows.map { row ->
     ItemView(
         id = requireNotNull(row.get(i.id)),
         name = requireNotNull(row.get(i.name)),
         amount = row.get(i.amount),
+        categoryId = row.get(i.categoryId),
     )
 }
```

DB에서 기존 값을 읽는 select와 읽은 값을 응답에 복사하는 mapper를 고쳤다. 값이 선택사항인 계약이므로 `!!`나 임의 UUID 기본값은 넣지 않는다. 이 조회 변경을 위해 `save()`를 호출할 이유도 없다.

생성자 projection을 쓰는 다른 조회가 같은 DTO를 반환한다면 그 생성자 인자 목록도 확인한다. 일반 `Projections.constructor`가 Kotlin 기본 인자를 알아서 생략해 호출한다고 가정하지 않는다. 특히 `name`·`categoryName`처럼 같은 String 타입을 추가했다면 **인자 위치가 바뀌어도 컴파일 또는 실행이 성공할 수 있으므로 값 자체를 검사**한다.

### 회귀 입력과 기대 결과를 적는다

| 준비된 데이터·상황 | 기대 응답 | 확인하는 이유 |
|---|---|---|
| A.categoryId=C1 | A의 categoryId=C1 | 새 필드가 실제 선택·대입됨 |
| B.categoryId=null | B의 categoryId=null | 미입력을 임의 값으로 바꾸지 않음 |
| A.amount=500, name=연필 | 기존 금액·이름 유지 | 다른 필드 대입을 흔들지 않음 |
| 다른 scope의 D.categoryId=C1 | D는 응답에서 제외 | 표시 필드 추가가 범위를 넓히지 않음 |
| 결과 0건 | 기존 계약의 빈 목록 | 새 필드 때문에 빈 결과가 예외로 바뀌지 않음 |
| 같은 DTO를 반환하는 다른 조회 | 해당 조회의 필드 계약대로 출력 | 공용 DTO 변경의 호출 영향 확인 |

수정한 쿼리가 쓰이는 목록·단건·export 등 호출을 사용처 검색으로 확인한다. 모든 경로에 같은 필드가 필요한지, 어떤 경로는 의도적으로 생략하는지 구분하고 검증한다. 새 필드가 항상 필수라면 nullable 기본값으로 누락을 숨기기보다 그 계약에 맞는 타입·생성자 사용을 정한다.

**추가 문제:** Tuple에서 categoryId=C1이고 DTO에서도 C1인데 HTTP JSON에 키 자체가 없다면 지금 diff를 다시 바꿔야 할까?

<details>
<summary>판단 확인</summary>

조회·대입 경계는 통과했다. 실제 반환 타입, getter·직렬화 애노테이션, 응답 필드 제외 정책을 확인한다. mapper 뒤에 다른 DTO로 한 번 더 옮기는 코드가 있는지도 본다. 값이 사라지는 다음 경계를 찾아야 한다.

</details>

관측한 값으로 수정 위치를 좁히는 방법은 [직접 수정하기 위한 학습법](09-studying-to-change-code.md)에서도 이어진다.

## 실제 코드에서 이 패턴 찾기

아래 파일은 패턴을 확인할 때만 연다. 업무 전체를 따라갈 필요 없이 지정 메서드의 select·fetch·변환 부분을 비교한다.

| 패턴 | 소스와 찾을 부분 |
|---|---|
| Entity → DTO | [AppointmentQueries](../../../x-patient-appointment/backend/patient-appointment/src/main/kotlin/com/projectx/patientappointment/infrastructure/queries/AppointmentQueries.kt)의 `find`, `views`: `selectFrom` → `fetch` → 매퍼 |
| 생성자 projection | [ViewerConfigurationQueries](../../../x-diagnostic-result/backend/diagnostic-result/src/main/kotlin/com/projectx/diagnosticresult/infrastructure/queries/ViewerConfigurationQueries.kt)의 `findViewerConfiguration`: `Projections.constructor(FavoriteObservationViewModel...)` |
| Tuple → DTO | [ObservationQueries](../../../x-diagnostic-result/backend/diagnostic-result/src/main/kotlin/com/projectx/diagnosticresult/infrastructure/queries/ObservationQueries.kt)의 `categoryQuery`, `toCategoryViews`, `toCategoryView` |
| Entity → View | [OrderingQueries](../../../x-clinical-care/backend/ordering/src/main/kotlin/com/projectx/ordering/application/queries/OrderingQueries.kt)의 `getOrderSheet`, `toOrderSheetView` |

[이전: 조회 조건](02-filter-patterns.md) · [목차](README.md) · [다음: 조인과 일괄 조회](04-join-and-batch-patterns.md)
