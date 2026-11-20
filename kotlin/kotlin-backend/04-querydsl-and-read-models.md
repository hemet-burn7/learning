# 4. QueryDSL과 데이터 조회 패턴

[이전: 데이터 바인딩](03-request-and-data-binding.md) · [목차](README.md) · [다음: 저장·트랜잭션](05-commands-and-transactions.md)

## 4.1 CQRS는 읽기와 쓰기의 책임 분리다

담당 레포의 일반 읽기 경로는 `Controller → IXxxQueries → XxxQueries → ViewModel`이다. 쓰기는 `Command → Handler → Aggregate → Repository`다. **읽기/쓰기 DB를 반드시 따로 운영한다는 뜻은 아니다.** 같은 JPA DataSource를 사용하면서 목적과 반환 모델을 나눈다.

쓰기용 Repository도 Aggregate를 수정하기 전에 읽는다. `findById`라는 이름만 보고 조회 API 구현이라고 판단하지 않는다. 반환형이 `Appointment`면 업무 객체를 읽는 경로, `AppointmentViewModel`이면 응답 모델을 만드는 경로다.

## 4.2 LINQ와 대응해서 읽기

```kotlin
// 설명용: 실제 예약 저장소의 찾기 패턴을 축약
queryFactory.selectFrom(a)
    .where(inScope(a._super, scope), a.id.eq(id))
    .fetchOne()
```

| Kotlin/QueryDSL | 뜻 | C#에서 떠올릴 개념 |
|---|---|---|
| `QAppointment.appointment` | 엔티티 필드를 표현하는 생성된 경로 | 타입 있는 쿼리 식 |
| `a.id.eq(id)` | SQL의 id 비교 조건을 구성 | expression tree의 비교 |
| `where(...)` | DB 필터 구성 | `IQueryable.Where` |
| `orderBy(...)` | DB 정렬 구성 | `OrderBy` |
| `fetch()` | 쿼리 실행 후 목록 획득 | `ToList` 경계 |
| `fetchOne()` | 없으면 null, 여러 건이면 오류 | 단건 의미 확인 필요 |
| `fetchFirst()` | 첫 건만 얻음 | `FirstOrDefault`에 가까움 |
| `.fetch().map { ... }` | 실행 뒤 메모리에서 변환 | `ToList().Select` |

`where`에 Kotlin Boolean을 계산해 넣는 것이 아니라 QueryDSL의 표현식을 만든다. `q.id.eq(id)`와 Kotlin `q.id == id`는 같은 의미가 아니다. QueryDSL의 Java 패키지는 `com.querydsl`이어도 빌드 의존성은 OpenFeign fork일 수 있다.

Q클래스는 Entity에서 kapt로 생성된다. `.kt`가 아니라 생성된 `Q...`를 먼저 수정하면 다음 빌드에서 사라진다. Entity 변경 → kapt 생성 → 쿼리 수정 순서로 본다.

## 4.3 실제 예약 조회: Entity를 내부에서 읽고 ViewModel로 옮김

[AppointmentQueries.find](../../../x-patient-appointment/backend/patient-appointment/src/main/kotlin/com/projectx/patientappointment/infrastructure/queries/AppointmentQueries.kt)는 다음 구조다.

```kotlin
// 실제 코드 발췌: 중간의 여러 필터만 생략
val where = BooleanBuilder(tenantPredicate(q.tenantId, q.hospitalCode, scope))
filter.patientId?.let { id -> where.and(q.patientId.eq(id)) }
return factory.selectFrom(q)
    .where(where)
    .orderBy(q.startAt.asc().nullsFirst(), q.createdAt.asc())
    .fetch()
    .let { views(scope, it) }
```

1. `BooleanBuilder`는 조건 누적기다. 첫 조건이 tenant·hospital 범위다.
2. 필터가 있을 때만 조건을 더한다. 빈 집합과 null을 “조건 없음”으로 처리할지 “조회 결과 없음”으로 처리할지는 업무 규칙이다.
3. `fetch()`에서 DB 조회가 실행된다.
4. `views`는 환자 id를 모아 `sharedDirectory.patients`로 이름을 한 번에 읽고 `ViewMappers.toView`로 응답을 만든다.
5. phase 같은 중첩 컬렉션 때문에 이 구현은 Entity를 내부 로딩한다. **Entity 자체를 HTTP로 직렬화하지는 않는다.**

공통 기준의 “가능하면 ViewModel 직접 projection”과 실제 이 경로를 구분한다. 모든 조회가 `Projections.constructor`라고 외우면 실제 소스를 읽다가 막힌다.

## 4.4 실제 검사결과 조회: 컬럼 → Tuple → ViewModel

[ObservationQueries](../../../x-diagnostic-result/backend/diagnostic-result/src/main/kotlin/com/projectx/diagnosticresult/infrastructure/queries/ObservationQueries.kt)는 결과 부모·분류별 자식 컬럼을 골라 `Tuple`로 읽는다.

```kotlin
// 실제 호출 구조 발췌
return toCategoryViews(
    categoryQuery().where(where).orderBy(o.resultDatetime.desc()).fetch()
)
```

`categoryQuery`가 조인·컬럼을 정하고, `toCategoryViews`가 직원 id 묶음을 읽고, 개별 변환에서 어떤 자식 결과값을 표시할지 선택한다. 여기서는 쓰기 Aggregate 전체를 로딩하는 것과 다르다.

조회 필드 하나를 추가한다면 다음 세 자리를 함께 본다.

1. QueryDSL select 목록에 필요한 값이 있는가?
2. Tuple에서 값을 꺼내 ViewModel 생성자에 넣었는가?
3. ViewModel 타입·nullability·시간 변환이 API 의미와 맞는가?

DB에 값이 있는데 응답만 null이면 이 매핑 중간이 빠졌을 수 있다. Entity에 필드가 있다고 모든 응답에 자동으로 나타나지 않는다.

## 4.5 세 가지 조회 형태를 구분한다

| 형태 | 사용 방식 | 이 교재의 실제 사례 |
|---|---|---|
| ViewModel 생성자 projection | select 시 바로 DTO 생성 | 공통 기준의 기본 권장 패턴; 대상 쿼리에서 사용 여부 확인 |
| scalar Tuple 후 조립 | 필요한 컬럼을 읽어 Kotlin에서 응답 구성 | 검사결과 `ObservationQueries` |
| Entity 내부 로딩 후 응답 변환 | 관계·컬렉션을 읽고 View로 변환 | 예약 `AppointmentQueries`, ordering `OrderingQueries` 대표 경로 |

아래는 **패턴 설명용이며 현재 특정 메서드의 복사본이 아니다.**

```kotlin
queryFactory.select(
    Projections.constructor(SimpleView::class.java, q.id, q.name)
).from(q).where(/* 필수 scope + 검색 조건 */).fetch()
```

생성자 projection은 인자 순서·타입·nullability가 맞아야 한다. 이름이 같다고 자동 매핑되는 것이 아니다. 새 쿼리는 대상 레포의 규칙과 가장 가까운 기존 구현을 따른다.

## 4.6 테넌트와 shared 데이터

담당 레포의 업무 테이블은 `tenantId`·`hospitalCode`로 범위를 제한한다. id로 한 건을 찾을 때도 조건을 생략하지 않는다. 인증 성공은 “모든 행 접근 허용”이 아니다.

shared의 환자·직원·부서는 원천 시스템의 CDC 데이터를 읽는 경로다. [예약 백엔드 기준](../../../x-patient-appointment/backend/CLAUDE.md)과 [검사결과 기준](../../../x-diagnostic-result/backend/CLAUDE.md)에 따라 **허용된 읽기 전용 매핑만** 사용한다. shared에 직접 쓰거나 다른 서비스 업무 스키마를 자유롭게 조인하는 패턴으로 확대하지 않는다.

현재 shared 조인은 원천 id 기준이며 tenant 조건은 자기 업무 테이블에 건다. 예약은 환자 목록을 id 묶음으로 읽고 검사결과는 환자·부서 조인과 직원 묶음 조회를 구분한다. 이름을 읽는 경로가 다르므로 “id가 같으니 join 하나 추가”로 결정하지 않는다. 조인 대상에 중복 행이 있으면 결과 수가 늘어날 수 있다.

## 4.7 조회가 느리거나 이상할 때

| 증상 | 먼저 볼 곳 | 이유 |
|---|---|---|
| 조건을 줬는데 전체 조회 | nullable 조건 분기·빈 집합 처리 | `?.let`으로 필수 조건까지 빠졌을 수 있음 |
| N건인데 DB 호출이 N+1회 | fetch 이후 Entity 관계 접근·mapper | 지연 컬렉션을 항목마다 읽을 수 있음 |
| 동일 결과가 중복 | 조인 대상·카디널리티·중첩 컬렉션 | 부모 하나가 자식 수만큼 늘어남 |
| 응답만 null | select→Tuple→ViewModel 매핑 | DB 컬럼이 자동으로 DTO에 복사되지는 않음 |
| 날짜 끝날 데이터 누락 | 날짜→병원 시간대 시각 범위 | inclusive 종료와 다음 날 exclusive 경계 확인 |
| 저장 직후 읽기 값이 예상과 다름 | flush 여부·영속성 컨텍스트·실제 DB 경로 | 같은 트랜잭션 읽기와 새 요청 읽기는 다름 |
| 이름만 없음 | shared 반영 지연·id 형태·외부 API fallback | 업무 행이 없는 경우와 구분 |

조회용 `readOnly = true`는 “쓰기 금지를 DB가 완벽하게 보장”하는 선언이 아니다. 조회 코드에서 Aggregate 변경 메서드를 호출하지 않는 책임 분리가 필요하다.

페이징은 DB의 `offset`·`limit` 적용 지점을 찾는다. 모든 행을 `fetch()`한 뒤 `drop/take`로 자르면 네트워크·메모리 비용이 이미 발생했다. 페이지의 0/1 기반과 응답 봉투는 레포마다 확인한다. ordering의 common-web은 wire `page` 1-based다.

## 4.8 읽기 연습

`AppointmentQueries.find`에서 `fetch()`에 중단점을 두고 조건 객체를 본다. 이어 `views`에서 shared 조회와 ViewModel 변환을 본다. 다음 질문에 답하면 흐름을 이해한 것이다.

- 요청의 `patientId`는 어느 조건으로 들어갔는가?
- DB 실행은 어느 줄인가?
- 환자명은 예약 Entity에 저장된 값인가, 별도 원천에서 붙인 값인가?
- ViewModel을 수정하면 DB도 바뀌는가? → 아니다.

[이전: 데이터 바인딩](03-request-and-data-binding.md) · [목차](README.md) · [다음: 저장·트랜잭션](05-commands-and-transactions.md)
