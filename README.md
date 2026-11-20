# 학습 자료 — Kotlin 백엔드 · React 프론트엔드 (세 BC 기준)

> 대상 레포: `x-diagnostic-result` · `x-patient-appointment` · `x-nutrition`
> 대상 독자: 이 코드베이스를 처음 보는 개발자(신입 교육 · 소스 분석 훈련)
> 모든 코드 인용은 실제 소스이고, 인용 위에 `레포/경로:시작줄-끝줄` 을 붙였다. 코드가 바뀌면 줄 번호는 어긋날 수 있다.

---

## 1. 문서 지도

| 순서 | 문서 | 무엇을 배우나 |
|---|---|---|
| 1 | [backend/01-kotlin-backend-patterns.md](backend/01-kotlin-backend-patterns.md) | 레이어 구조, 요청 한 건이 Controller 에서 DB 까지 가는 길, CQRS · Aggregate · QueryDSL · Kafka inbox 패턴, Kotlin 문법, 새 엔드포인트 추가 샘플 |
| 2 | [frontend/01-react-frontend-patterns.md](frontend/01-react-frontend-patterns.md) | Host + Remote(Module Federation), FSD 폴더 구조, 화면이 데이터를 가져오는 길, useFreshQuery · useMutation · 상태 관리 · 디자인킷, 새 화면 추가 샘플 |
| 3 | [trace/diagnostic-result-api-trace.md](trace/diagnostic-result-api-trace.md) | 검사결과 BC 의 모든 API — 어느 메서드가 불리고 어느 테이블에서 데이터를 가져오는지 |
| 4 | [trace/patient-appointment-api-trace.md](trace/patient-appointment-api-trace.md) | 환자예약 BC 의 모든 API 추적 |
| 5 | [trace/nutrition-api-trace.md](trace/nutrition-api-trace.md) | 영양(급식) BC 의 모든 API 추적 |
| 부록 | [reference/](reference/) | 세 BC 의 BE · FE 원문 발췌집 6편. 위 문서들의 근거 자료이고, 더 깊이 볼 때 쓴다 |
| 더 읽기 | [x-diagnostic-result-kotlin-react-guide.md](x-diagnostic-result-kotlin-react-guide.md) | 검사결과 BC 한 벌을 문법부터 끝까지 해설한 단일 레포 해설서 |

원문 발췌집 6편:

- [reference/be-diagnostic-result.source-excerpts.md](reference/be-diagnostic-result.source-excerpts.md)
- [reference/be-patient-appointment.source-excerpts.md](reference/be-patient-appointment.source-excerpts.md)
- [reference/be-nutrition.source-excerpts.md](reference/be-nutrition.source-excerpts.md)
- [reference/fe-diagnostic-result.source-excerpts.md](reference/fe-diagnostic-result.source-excerpts.md)
- [reference/fe-patient-appointment.source-excerpts.md](reference/fe-patient-appointment.source-excerpts.md)
- [reference/fe-nutrition.source-excerpts.md](reference/fe-nutrition.source-excerpts.md)

---

## 2. 세 BC 한눈에

| BC 레포 | 백엔드 MS(모듈) | 프론트 remote | 성격 |
|---|---|---|---|
| `x-diagnostic-result` | `diagnostic-result` | `diagnostic-result` | 다른 BC 가 Kafka 로 보낸 검사결과를 받아 쌓고 보여준다. 수신 중심, AI 분석 외부 호출이 있다 |
| `x-patient-appointment` | `patient-appointment` | `patient-appointment` | 슬롯을 정의 · 운영하고 예약을 등록 · 취소한다. 쓰기와 이벤트 발행이 가장 많다 |
| `x-nutrition` | `foodservice` | `nutrition` | 식이 처방을 받아 끼니별 세부오더 · 변경요청 · 마감을 관리한다. 오류 코드와 `allowed_actions` 로 화면 버튼을 정한다 |

세 BC 는 같은 규칙(레이어 4개 · CQRS · QueryDSL · Liquibase · common-eventbus · FSD · 디자인킷)을 따르지만 구현 세부가 조금씩 다르다. 패턴편의 「세 BC 비교표」가 그 차이를 모아 둔다.

---

## 3. 신입 학습 경로 (권장 2주)

| 일차 | 할 일 | 읽을 곳 |
|---|---|---|
| 1 | 레포 구조와 규칙 읽기. 레이어 · CQRS · snake_case 규칙을 이해한다 | 루트 `CLAUDE.md` §0 · §1, 백엔드 패턴편 1장 |
| 2 | 요청 한 건의 여정을 코드로 따라간다. IDE 에서 Controller 부터 Repository 까지 직접 점프한다 | 백엔드 패턴편 2장 |
| 3–4 | 패턴별 해설과 Kotlin 문법 | 백엔드 패턴편 3 · 4장 |
| 5 | 추적서 한 편을 골라 엔드포인트 3개를 직접 추적하고, 추적서 내용과 맞는지 확인한다 | trace 문서 1편 |
| 6 | 프론트 큰 그림과 FSD | 프론트 패턴편 1 · 2장 |
| 7 | 화면이 데이터를 가져오는 길을 브라우저 개발자 도구 Network 탭과 함께 따라간다 | 프론트 패턴편 3장 |
| 8–9 | 패턴별 해설 · 문법 · 디자인킷 | 프론트 패턴편 4 · 5장 |
| 10 | 화면 하나를 골라 클릭에서 DB 까지 끝에서 끝으로 추적한다 | 추적서의 「화면 → API 역추적표」 |
| 11–12 | 따라 만들기 샘플을 로컬 브랜치에서 실제로 만들어 본다(커밋 · 푸시는 하지 않는다) | 두 패턴편의 「따라 만들기 샘플」 |
| 13–14 | 연습 과제와 스스로 확인 문제 | 각 문서 끝 |

### 매일 쓰는 추적 요령

1. **백엔드**: 경로 문자열(예 `/appointments`)로 `@RequestMapping` 을 검색해 Controller 를 찾는다. Controller 메서드가 만드는 command 나 부르는 Query 인터페이스를 따라간다. 쓰기는 `*_handler` → Aggregate 메서드 → Repository, 읽기는 `*_query_dsl` 구현 클래스에서 끝난다.
2. **프론트**: 화면 path 로 `routes.tsx` 에서 view 컴포넌트를 찾는다. view 아래의 훅(`useFreshQuery` · `useMutation`)이 부르는 api 함수에서 HTTP 경로를 확인하고, 그 경로로 1번을 이어 간다.
3. **데이터**: 조회 결과가 이상하면 QueryDSL 구현의 `where` 조건과 projection 필드를 먼저 본다. DB 는 에이전트나 도구로 직접 붙지 않고, 앱 기동 로그와 HTTP 응답으로 확인한다.

---

## 4. 이 자료를 고칠 때

- 코드가 바뀌어 인용이 어긋나면 해당 문서의 인용과 `경로:줄` 을 함께 고친다.
- 규칙 문서(`CLAUDE.md` · `.claude/docs/rules/*`)와 이 자료가 다르면 규칙 문서가 기준이다.
- 따라 만들기 샘플은 학습용 가상 기능이다. 실제 레포에 그대로 넣지 않는다.
