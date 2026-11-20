# C# 개발자를 위한 Kotlin 데이터 조회·처리 패턴

**나중에 버그를 고치거나 기능을 바꿀 때, 스스로 수정할 위치와 이유를 판단하기 위한 Kotlin 학습 문서**다. C#의 LINQ·EF Core·DTO 경험을 바탕으로 데이터가 읽히고, 객체에 담기고, 변경되어 저장되는 과정을 익힌다.

읽고 나면 다음을 직접 할 수 있는 것이 목표다. 조회 결과가 틀렸을 때 입력·조건·결과 변환 중 어디에서 달라졌는지 찾고, 검색 조건이나 응답 필드를 추가할 때 연결해서 바꿀 코드를 정하며, 수정한 값이 DB와 다른 서비스에 반영되는 시점을 설명한다.

첫 다섯 장은 조회에 집중한다. 조회식 구성, 선택 조건, 조회 결과 바인딩, 조인·일괄 조회, 페이징·집계를 각각 비교한다. 이어 조회한 값의 변경·커밋, 외부 API, 이벤트를 통한 데이터 반영을 다룬다.

**IntelliJ에서 새 Kotlin 프로젝트를 직접 만들려면 [11장: 한 파일씩 넣어 실행하기](11-build-your-own-lab.md)부터 시작한다.** New Project에서 Kotlin·Gradle을 선택한 뒤 DB 연결, QueryDSL 조회, JPA 저장, 외부 HTTP와 Kafka까지 작성한다. 빌드·디버깅·Kotlin/JUnit 점검을 IDE와 Gradle로 수행하며 Python은 필요하지 않다. [완성 예제](lab/reference/README.md)는 내 코드와 비교할 때 사용한다.

## 먼저 이 차이부터 읽는다

```kotlin
// 설명용. q는 QItem, scopePredicate는 필수 테넌트·병원 조건이다.
val query = queryFactory.selectFrom(q)
    .where(scopePredicate, q.active.isTrue)  // 조회식 구성

val rows = query.fetch()                   // DB 실행 → Entity 목록
val names = rows.map { it.name }            // 이미 읽은 목록을 메모리에서 변환
```

`where`는 DB에서 실행할 조건을 만든다. `fetch`가 실행 경계다. 그 뒤 `map`은 DB 쿼리의 select 목록을 바꾸지 않고 이미 읽은 객체에서 값을 꺼낸다. 같은 `map`이라도 **왼쪽 값의 타입**을 알아야 데이터가 어디에 있는지 판단할 수 있다.

C#으로는 `IQueryable`에 `Where`를 붙이고 `ToList()`로 실행한 다음, 그 `List`에 `Select(...).ToList()`를 하는 단계와 비교한다. 이처럼 각 패턴의 코드와 실행 시점을 먼저 설명하고 실제 프로젝트 코드는 확인용 근거로 연결한다.

## 학습 순서

| 장 | 설명하는 패턴 | 직접 수정해 보는 문제 |
|---|---|---|
| [1. 조회식을 만들고 실행하기](01-query-execution.md) | QueryDSL 구성 → fetch → 메모리 처리 | 조회 실행이 불필요하게 두 번 일어남 |
| [2. 검색조건 조립하기](02-filter-patterns.md) | 고정·선택 조건, AND/OR, null, IN, 날짜 | false 조건을 보냈는데 전체가 조회됨 |
| [3. 결과를 객체에 담기](03-result-binding-patterns.md) | Entity→DTO, 생성자 projection, Tuple→DTO | 응답 DTO에 추가한 필드가 계속 null임 |
| [4. 여러 데이터를 합쳐 조회하기](04-join-and-batch-patterns.md) | JOIN, EXISTS, IN 일괄 조회, 자식 목록, N+1 | 자식 조건을 추가하니 부모 행이 중복됨 |
| [5. 단건·페이징·집계](05-pagination-and-aggregation.md) | 단건 의미, 정렬, count, offset/limit, groupBy | 목록은 맞는데 전체 건수가 틀림 |
| [6. 조회한 값을 바꾸고 커밋하기](06-read-change-commit.md) | 관리 객체, DTO, dirty checking, save/flush/commit | 객체의 값은 바뀌었는데 다시 조회하면 원래 값임 |
| [7. 외부 API에서 읽어 조립·저장하기](07-external-data-patterns.md) | HTTP 역직렬화, 데이터 보강, 스냅샷 저장 | 외부 응답의 새 필드가 로컬 조회에는 반영되지 않음 |
| [8. 이벤트로 데이터를 반영하기](08-event-data-patterns.md) | 커밋 후 발행, outbox, inbox, 멱등 | 늦게 도착한 이벤트가 최신값을 덮어씀 |
| [9. 직접 수정하기 위한 학습법](09-studying-to-change-code.md) | 입력·필드 추적 → 수정 범위 → 변경 → 확인 | 새 검색 조건을 끝까지 연결해 추가함 |
| [10. 실습장](10-practice-workbook.md) | 문제 → 답안 작성 → 수정 → 결과 확인 | 조회·저장·API·Kafka 9문제와 실제 소스 적용 과제 |
| [11. 새 프로젝트부터 실행하는 실습](11-build-your-own-lab.md) | 파일 추가 → 컴파일 → DB·HTTP·Kafka 확인 | 내 프로젝트에서 연결·조회·저장을 직접 구현 |

각 패턴은 **문제 → 코드 비교 → 줄별 의미 → 실행·바인딩 시점 → 결과 예시 → 선택 기준·주의점** 순서로 읽는다. 처음에는 1~3장으로 한 번의 조회를 이해하고, 4~5장에서 조회 결과가 커지고 복잡해질 때의 선택을 익힌다.

이어서 각 장의 **직접 수정해 보기**에서 원인을 예상하고, 중단점을 정하고, 수정 전·후 코드를 비교한다. 결과표에는 수정한 사례뿐 아니라 기존 동작이 유지되는지도 확인할 입력을 적었다. 예제의 결함은 학습용으로 만든 것이며 실제 레포의 버그 판정이 아니다.

처음 공부할 때 [9장의 학습 순서](09-studying-to-change-code.md#91-한-패턴을-익히는-순서)를 먼저 읽어도 좋다. 각 장을 읽은 뒤 실제 소스에서 같은 패턴 하나를 찾아 **어느 줄을 바꿀지, 왜 그 줄인지, 무엇으로 확인할지**를 말할 수 있을 때 다음 패턴으로 넘어간다.

직접 답을 작성하며 연습할 때는 [실습장](10-practice-workbook.md)을 사용한다. 공통 예제 데이터, 수정할 코드, 답안 칸, 결과표와 접힌 해설을 한 페이지에 모았다.

## 예제 데이터와 표기

업무 지식 없이 코드를 읽을 수 있도록 설명용 `Item`을 공통으로 사용한다. 다음 타입은 교재용 모델이며 실제 레포에 이 Entity를 추가한 것이 아니다.

| 이름 | Kotlin 타입 | 뜻 |
|---|---|---|
| `Item.id` | `UUID` | 항목 식별자 |
| `Item.name` | `String` | 표시 이름 |
| `Item.categoryId` | `UUID?` | 선택적인 분류 참조 |
| `Item.active` | `Boolean` | 사용 여부 |
| `Item.amount` | `BigDecimal?` | 값이 없을 수 있는 금액 |
| `Item.createdAt` | `Instant` | 생성 시각 |
| `Item.tenantId`, `hospitalCode` | `String` | 조회할 수 있는 범위 |
| `Item.version` | `Long?` | 변경 충돌 확인용 버전 |
| `Category` | `id`, `name` | 조회가 허용된 참조 데이터 |
| `Detail` | `id`, `itemId`, `label` | Item에 속한 자식 데이터 |

- C#은 EF Core의 `db.Items`가 `IQueryable<Item>`이라는 전제다. 프로퍼티는 `Name`, `Active`처럼 PascalCase로 쓴다.
- Kotlin은 JPA의 `EntityManager`에 연결된 `JPAQueryFactory`를 사용한다. `QItem`은 해당 Entity에서 생성된 조회 경로다.
- `q`, `i`, `item`은 장마다 선언한 Q클래스 별칭이다. `scopePredicate`·`scopeCondition(scope)`는 필수 범위 조건을 나타낸다. 실제 구현은 각 장에서 설명한다.
- `A`, `B`, `C1` 같은 표의 id는 가독성을 위한 UUID의 축약 표시다. 실제 UUID 입력값은 아니다.
- 코드 블록은 **패턴을 설명하는 축약 예제**다. import·Entity 매핑·DI 설정 등은 생략하며 그대로 복사해 실행하는 완성 앱이 아니다. C#과 Kotlin은 같은 조회 의도를 비교하며 ORM의 모든 동작이 동일하다는 뜻은 아니다.
- 소스를 열지 않아도 본문의 작은 예제와 결과표로 패턴을 이해할 수 있게 구성했다. 프로젝트 파일은 각 장 끝에서 같은 패턴을 찾아보는 용도로 사용한다.

## 프로젝트에서의 적용 범위

실제 근거는 `x-clinical-care`, `x-patient-appointment`, `x-diagnostic-result`의 코드다. 프로젝트에는 **Entity를 읽고 DTO로 옮기는 코드, 컬럼을 Tuple로 읽는 코드, 생성자 projection**이 모두 있다. 저장·발행 경계도 서로 다르므로 패턴의 공통 의미와 실제 호출자의 경계를 구분한다.

이 교재는 소스와 공식 기술 문서를 바탕으로 작성했다. 1~10장의 개념 예제와 실제 제품 서비스의 실행 검증을 구분한다. 새 독립 학습 앱의 실행 확인 범위는 [11장](11-build-your-own-lab.md)에 기록한다. Markdown을 HTML로 옮길 수 있도록 상대링크, 코드 블록, 표, Mermaid를 사용했다.
