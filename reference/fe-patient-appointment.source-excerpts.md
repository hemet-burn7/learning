# x-patient-appointment 프런트엔드(`frontend/apps/patient-appointment`) 소스 수집 — 신입 교육 자료용

> 기준 경로(이하 `APP`) = `x-patient-appointment/frontend/apps/patient-appointment`
> 모든 인용은 실제 소스 그대로이며 `경로:시작줄-끝줄` 을 붙인다. 경로는 `APP/` 기준 상대 경로.

---

## 1. 폴더 트리 · 설정

### 1.1 FSD 레이어별 파일 수 (`find src -type f | sort` — 총 215 파일)

| 레이어/영역 | 파일 수 | 비고 |
|---|---|---|
| `src/index.ts` · `bootstrap.tsx` · `routes.tsx` · `global.d.ts` | 4 | 진입점 |
| `src/app/` | 1 | `Standalone.tsx`(dev 전용 셸) |
| `src/exposes/` | 3 | MF expose 진입점(대화상자 3개) |
| `src/views/` | 14 | `appointment/`(5) · `clinic-hours/`(1) · `schedule/`(8) |
| `src/widgets/` | 127 | `appointment/`(42) · `chemotherapy-registration/`(3) · `clinic-hours/`(9) · `patient-schedule/`(5) · `schedule/`(68) |
| `src/features/` | **0** | **이 레포에는 `features` 레이어가 없다** — 훅·store 는 `entities/*/model` 에, UI 조합은 `widgets` 에 있다 |
| `src/entities/` | 46 | 전부 `*/model/*.ts` — `appointment/`(18) · `chemotherapy-registration/`(1) · `clinic-hours/`(3) · `patient-schedule/`(1) · `schedule/`(23). **`entities/*/api/` 폴더가 없다** — HTTP 함수는 `shared/api/appointmentApi.ts`(2,949줄) 한 파일에 모여 있다 |
| `src/shared/` | 18 | `api/`(2) · `lib/`(4) · `patient-session/`(3) · `ui/`(9) |
| `src/mocks/` | 1 | `appointment/booking_request_mock.ts` |
| `src/styles/` | 1 | `globals.css` |

전체 파일 목록(요약):

```
src/app/Standalone.tsx
src/bootstrap.tsx · src/index.ts · src/routes.tsx · src/global.d.ts
src/exposes/{AppointmentBookingDialog,BookingClosureDialog,OutpatientAppointmentDialog}.tsx
src/entities/appointment/model/{appointment_search,auto_select_options,booking,booking_auto,booking_contract,booking_data,booking_ext,booking_keys,booking_months,booking_patient,chemotherapy_plan,move_together,outpatient_booking,outpatient_booking_contract,outpatient_booking_data,period_presets,session_orders}.ts
src/entities/chemotherapy-registration/model/chemotherapy_registration.ts
src/entities/clinic-hours/model/{clinic_hours,closure,effective_clinic_hours}.ts
src/entities/patient-schedule/model/patient_schedule.ts
src/entities/schedule/model/{booking_closure,booking_closure_contract,category_departments,create_schedule_for_target,day_operation,eligibility,operation_days,outpatient,overbook_allowance,pattern,period_comment,physician_lines,schedule,schedule_api,schedule_api_catalog,schedule_comment,schedule_daily,schedule_dashboard,schedule_labels,schedule_menu,schedule_overview,schedule_store,slot,substitute}.ts
src/mocks/appointment/booking_request_mock.ts
src/shared/api/{ancillaryApi,appointmentApi}.ts
src/shared/lib/{datetime,errors,month_cells,query_cache}.ts
src/shared/patient-session/{ancillaryPatientSession,session_patient,session_patient_stand_in}.ts
src/shared/ui/{ClickDatePicker,ConfirmDialog,DialogFooterButtons,PanelCard,ScreenShell,TimeRangeFields}.tsx · shared/ui/filter/{KeywordField,PeriodPresetField,StatusCheckGroup}.tsx
src/styles/globals.css
src/views/appointment/{AppointmentBooking,AppointmentSearch,ChemotherapyPlan,OutpatientAppointment,PatientSchedule}.tsx
src/views/clinic-hours/ConfigurationScreen.tsx
src/views/schedule/{BookingClosureHarness,ScheduleApiReferenceScreen,ScheduleDailyScreen,ScheduleDashboardTodayScreen,ScheduleDashboardWeekScreen,ScheduleOverviewScreen,SchedulePatternScreen,ScheduleSlotScreen}.tsx
src/widgets/appointment/*.tsx (17) + auto-select/(3) + calendar/(3) + chemotherapy/(7) + search/(2) + booking_layout.ts
src/widgets/chemotherapy-registration/{ChemotherapyCycleDialog,ChemotherapyRegistrationDialog,ChemotherapyRegistrationPanel}.tsx
src/widgets/clinic-hours/{AddUnitDialog,BookingPolicyPanel,ClosureDialog,ClosurePanel,CommonRulesPanel,PolicyUnitList,SessionDefaultTable,TimeInput,WeekdayHoursTable}.tsx
src/widgets/patient-schedule/{MedicationCountCard,PatientScheduleHeader,PatientScheduleNoteHistoryDialog,PatientVisitTable,UnbookedOrderCard}.tsx
src/widgets/schedule/api-reference/(8 + parts/8) · aside/(4) · closure/(1) · common/(3) · daily/(2) · dashboard/(8) · operation/(23) · overview/(2) · pattern/(14) · shell/(4)
```

주요 큰 파일: `shared/api/appointmentApi.ts`(2,949줄) · `entities/schedule/model/schedule_api_catalog.ts`(2,747줄) · `entities/schedule/model/schedule_store.ts`(1,206줄) · `entities/appointment/model/booking.ts`(1,193줄) · `entities/schedule/model/day_operation.ts`(821줄) · `entities/schedule/model/outpatient.ts`(820줄).

### 1.2 `package.json` (`package.json:1-53`)

```json
{
  "name": "patient-appointment",
  "version": "0.0.1",
  "private": true,
  "scripts": {
    "dev": "rspack serve --mode development",
    "build": "rspack build --mode production",
    "lint": "eslint src",
    "check:shared": "check-ehr-shared",
    "format": "prettier --write .",
    "format:check": "prettier --check ."
  },
  "dependencies": {
    "@x-ehr/api-axios": "~0.0.9",
    "@x-ehr/app-kit": "~0.0.33",
    "@x-ehr/auth-contract": "~0.0.16",
    "@x-ehr/design-system": "0.0.36",
    "@x-ehr/event-bus": "~0.0.22",
    "@x-ehr/i18n": "~0.0.12",
    "@tanstack/react-query": "^5.90.21",
    "axios": "^1.13.6",
    "i18next": "^25.10.10",
    "react": "19.2.3",
    "react-dom": "19.2.3",
    "react-i18next": "^16.6.6",
    "react-router": "^7.14.2",
    "zustand": "^5.0.11"
  },
  "devDependencies": {
    "@x-ehr/dev-mocks": "~0.0.20",
    "@x-ehr/eslint-config": "~0.0.9",
    "@x-ehr/rspack-config": "~0.0.14",
    "@x-ehr/tsconfig": "~0.0.2",
    "@module-federation/enhanced": "^2.6.0",
    "@rspack/cli": "^2.0.8",
    "@rspack/core": "^2.0.8",
    "@rspack/dev-server": "^2.1.0",
    "@rspack/plugin-react-refresh": "^2.0.2",
    "@tailwindcss/postcss": "^4.0.0",
    ...
    "tailwindcss": "^4.0.0",
    "typescript": "^5"
  }
}
```

워크스페이스 루트 `frontend/package.json` 의 스크립트(루트에서 실행):

```json
  "scripts": {
    "dev": "yarn workspace patient-appointment dev",
    "build": "yarn workspace patient-appointment build",
    "lint": "eslint apps",
    "check:screens": "node node_modules/@x-ehr/design-system/scripts/check-screen-usage.mjs apps",
    "typecheck": "yarn workspace patient-appointment exec tsc --noEmit",
    "check:ts-single": "node node_modules/@x-ehr/eslint-config/scripts/check-single-typescript.cjs",
    "i18n:check": "node node_modules/@x-ehr/i18n/scripts/i18n-check.cjs --allow-empty",
    "check:shared": "yarn workspace patient-appointment check:shared",
    ...
  },
  "resolutions": {
    "typescript": "^5",
    "@tanstack/react-query": "5.101.4"
  }
```

`tsconfig.json` 은 `../../tsconfig.base.json`(= `@x-ehr/tsconfig/base.json`) 을 상속하고 `"@/*": ["./src/*"]` 별칭만 추가. `postcss.config.mjs` 는 `@tailwindcss/postcss` 플러그인 하나.

### 1.3 `rspack.config.js` (`rspack.config.js:1-27`, `68-97`)

```js
const { createRemoteRspackConfig } = require('@x-ehr/rspack-config')

// BC 레포(x-patient-appointment) 의 MF remote. MF name(remoteKey)=patient_appointment, 메뉴 path base=/patient-appointment.
//
// remoteKey·port 는 **불변** — host 는 env.js 의 remoteBaseUrl 로
// `${base}/patient_appointment/remoteEntry.js` convention 을 조립하므로 이 값이 바뀌면 host 가 못 찾는다.
// (host 등록 키도 `patient_appointment` 로 맞춰야 한다 — x-ehr-foundation 의 in-repo 사본 apps/appointment 는 별도 lockstep 작업, dev port 3352 유지)
const DEV_PORT = 3352

// 로컬 백엔드 포트 — dev-up 가 다른 포트로 띄우면 BE_PORT 로 오버라이드한다.
const BE_PORT = process.env.BE_PORT || '5352'
// x-ancillary-main 백엔드 — standalone 의 세션 환자 대역이 그 환자리스트에서 실제 환자를 고른다(dev-up 과 같은 변수).
const ANCILLARY_BE_PORT = process.env.BE_ANCILLARY_MAIN_PORT || '5301'

const base = createRemoteRspackConfig({
  dirname: __dirname,
  name: 'patient_appointment',
  port: DEV_PORT,
  // 검사 예약 · 외래 예약 대화상자 · 슬롯 마감 팝업 — 호출 화면(다른 remote)이 `remoteComponent('patient_appointment', '<이름>')` 로
  // 자기 화면 안에 띄운다(classdiagram 부록 P62 · P73 · P113). `./routes` 는 팩토리가 항상 덧붙인다(host seam 계약).
  exposes: {
    './AppointmentBookingDialog': './src/exposes/AppointmentBookingDialog.tsx',
    './OutpatientAppointmentDialog': './src/exposes/OutpatientAppointmentDialog.tsx',
    './BookingClosureDialog': './src/exposes/BookingClosureDialog.tsx',
  },
  shared: ['@x-ehr/app-kit', '@x-ehr/auth-contract', '@x-ehr/design-system'],
})
```

```js
module.exports = (env, argv) => {
  const config = base(env, argv)
  if (config.devServer) {
    // proxy 보다 먼저 잡아야 한다 — /api/v1/iam/** 는 아래 proxy 규칙 대상이 아니다.
    const prevSetup = config.devServer.setupMiddlewares
    config.devServer.setupMiddlewares = (middlewares, server) => {
      const next = prevSetup ? prevSetup(middlewares, server) : middlewares
      next.unshift({
        name: 'standalone-auth-stub',
        middleware: (req, res, nextFn) => {
          if (!authStub(req, res)) nextFn()
        },
      })
      return next
    }
    config.devServer.proxy = [
      {
        context: ['/api/v1/patient-appointment'],
        target: `http://127.0.0.1:${BE_PORT}`,
        changeOrigin: true,
      },
      {
        context: ['/api/v1/ancillary-main'],
        target: `http://127.0.0.1:${ANCILLARY_BE_PORT}`,
        changeOrigin: true,
      },
    ]
  }
  return config
}
```

중간(`rspack.config.js:29-66`)은 standalone 전용 auth 스텁 — `/api/v1/iam/auth/csrf` · `/api/v1/iam/auth/refresh` 를 devServer 미들웨어가 더미 JSON 으로 응답한다(remote 소스에서 토큰을 심는 것은 계약 위반이라 dev 서버 계층에서 처리).

### 1.4 `eslint.config.mjs` — 커스텀 규칙 전체 (`frontend/eslint.config.mjs:16-79`)

```js
const REACT_QUERY_RESTRICTION = {
  name: '@tanstack/react-query',
  importNames: [
    'useQuery',
    'useQueries',
    'useSuspenseQuery',
    'useSuspenseQueries',
    'useInfiniteQuery',
  ],
  message:
    '조회는 늘 DB 최신이다 — @/shared/lib/query_cache 의 useFreshQuery · useFreshQueries 를 쓴다.',
}
// 날짜 칸은 누를 때만 달력을 연다 — 킷 그대로면 포커스만 받아도(대화상자 첫 입력 · 창 복귀) 달력이 떠 있고,
// 입력란이 스크롤로 가려지거나 화면이 숨겨져도 달력만 맨 위에 남는다.
const DATE_PICKER_RESTRICTION = {
  name: '@x-ehr/design-system',
  importNames: ['DatePicker', 'DateRangePicker'],
  message:
    '날짜 칸은 @/shared/ui/ClickDatePicker 의 ClickDatePicker · ClickDateRangePicker 를 쓴다(누를 때만 달력이 열린다).',
}

export default [
  ...base,
  // Remote 계약 강제 — 라우터/QueryClient 자체 생성 금지, auth 는 @x-ehr/auth-contract,
  // HTTP 는 @x-ehr/api-axios, 포맷은 @x-ehr/i18n 만. 커밋 전 `yarn lint` 통과 필수.
  //
  // ⚠️ 계약 룰의 스코프는 `{ts,tsx}` 다 — `.jsx`/`.js` 는 검사되지 않는다.
  //    diagnostic-result 는 아직 .jsx/.js 가 남아 있어 그 파일들은 계약 미검사 상태다(이관 후 ts 전환 과제).
  ...remoteConfig({
    files: REMOTE_DIRS.map((d) => `apps/${d}/**/*.{ts,tsx}`),
    standalone: REMOTE_DIRS.map((d) => `apps/${d}/src/app/Standalone.tsx`),
    routes: REMOTE_DIRS.map((d) => `apps/${d}/src/routes.tsx`),
  }),
  // MF 빌드 계약 — rspack.config.js 의 name/uniqueName · ./routes expose · base URL · shared strictVersion.
  ...mfBuildContract({ files: ['apps/*/rspack.config.js'] }),
  // 조회는 늘 DB 최신 — react-query 조회 훅은 `@/shared/lib/query_cache` 의 useFreshQuery · useFreshQueries 만 쓴다.
  // 직접 쓰면 host 기본 캐시(staleTime 5분)를 따라 다시 연 화면이 이전에 읽은 값을 보여 준다.
  // 날짜 칸은 `@/shared/ui/ClickDatePicker` 만 쓴다(DATE_PICKER_RESTRICTION).
  // (`no-restricted-imports` 는 공용 config 가 이미 쓰고 있어 덮어쓰지 않도록 typescript-eslint 판을 쓴다.)
  {
    files: REMOTE_DIRS.map((d) => `apps/${d}/src/**/*.{ts,tsx}`),
    ignores: REMOTE_DIRS.map((d) => `apps/${d}/src/shared/lib/query_cache.ts`),
    rules: {
      '@typescript-eslint/no-restricted-imports': [
        'error',
        {
          paths: [REACT_QUERY_RESTRICTION, DATE_PICKER_RESTRICTION],
        },
      ],
    },
  },
  // 래퍼 자신은 킷 날짜 입력을 감싼다 — 조회 규칙만 남긴다(같은 룰을 다시 정의하면 위 옵션을 통째로 덮는다).
  {
    files: REMOTE_DIRS.map((d) => `apps/${d}/src/shared/ui/ClickDatePicker.tsx`),
    rules: {
      '@typescript-eslint/no-restricted-imports': [
        'error',
        {
          paths: [REACT_QUERY_RESTRICTION],
        },
      ],
    },
  },
]
```

`REMOTE_DIRS` 는 `apps/<dir>/src/routes.tsx` 가 있는 폴더를 자동 발견한다(`eslint.config.mjs:11-14`).

---

## 2. 진입점

### 2.1 `src/index.ts` (`src/index.ts:1-2`)

```ts
// MF 비동기 경계 — shared 초기화가 끝난 뒤 standalone 부팅한다.
void import('./bootstrap')
```

### 2.2 `src/bootstrap.tsx` (`src/bootstrap.tsx:1-13`)

```tsx
import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import Standalone from './app/Standalone'
import './styles/globals.css'

const container = document.getElementById('root')
if (!container) throw new Error('Root element #root not found')

createRoot(container).render(
  <StrictMode>
    <Standalone />
  </StrictMode>,
)
```

### 2.3 `src/routes.tsx` — RemoteScope 래퍼와 라우트 목록 (`src/routes.tsx:1-101`)

```tsx
import { Outlet, useOutletContext, type RouteObject } from 'react-router'
import AppointmentBooking from './views/appointment/AppointmentBooking'
import AppointmentSearch from './views/appointment/AppointmentSearch'
import ChemotherapyPlan from './views/appointment/ChemotherapyPlan'
import OutpatientAppointment from './views/appointment/OutpatientAppointment'
import PatientSchedule from './views/appointment/PatientSchedule'
import BookingClosureHarness from './views/schedule/BookingClosureHarness'
import ScheduleApiReferenceScreen from './views/schedule/ScheduleApiReferenceScreen'
import ScheduleDailyScreen from './views/schedule/ScheduleDailyScreen'
import ScheduleDashboardTodayScreen from './views/schedule/ScheduleDashboardTodayScreen'
import ScheduleDashboardWeekScreen from './views/schedule/ScheduleDashboardWeekScreen'
import ScheduleOverviewScreen from './views/schedule/ScheduleOverviewScreen'
import SchedulePatternScreen from './views/schedule/SchedulePatternScreen'
import ScheduleSlotScreen from './views/schedule/ScheduleSlotScreen'
import ConfigurationScreen from './views/clinic-hours/ConfigurationScreen'
import { SCHEDULE_MENU_KEYS } from '@/entities/schedule/model/schedule_menu'
// Host nest 시에도 remote 유틸리티 CSS 가 로드되도록 노출 진입점에서 import.
import './styles/globals.css'

// remote CSS 격리 경계 — 전역 리셋이 host 헤더/배경을 덮지 않도록 모든 화면을 .x-remote-root 로 감싼다
// (host/standalone 공통 seam). 부모(host <main>/standalone shell)의 outlet context(auth)를 그대로 하위 화면에 전달한다.
//
// `khds-root` 는 디자인시스템의 소비측 계약이다 — dist CSS 의 base 리셋(box-sizing·폰트)이 이 클래스
// 하위로만 스코프돼 있어, 빠지면 컴포넌트가 브라우저 기본 모양으로 풀린다(README §Micro-Frontend).
// 두 클래스를 같은 엘리먼트에 둔다: 격리 경계와 킷 스코프가 같은 지점이어야 한다.
function RemoteScope() {
  const ctx = useOutletContext()
  return (
    <div className="x-remote-root khds-root">
      <Outlet context={ctx} />
    </div>
  )
}

// top-level path 는 Host/메뉴 소유 — remote 는 상대 child route 만 정의한다(absolute path 금지).
// 메뉴 leaf path `/patient-appointment/<child>` 의 base segment 에 host 가 이 routes 를 1회 nest 한다.
//
// **슬롯 관리는 진입 화면 두 벌이다** — 경로는 `<면>/<대상 구분>`:
//   면        `schedule-pattern`(정의 — 대상마다 패턴 여러 벌 · `SchedulePatternScreen`)
//             `schedule-slot`(운영 — 대상마다 슬롯 한 벌 · `ScheduleSlotScreen`)
//             `schedule-overview`(현황 — 예약 주체 전체 현황 · `ScheduleOverviewScreen` · 지금은 `physician` 하나)
//             `schedule-dashboard-week` · `schedule-dashboard-today`(현황판 — 대기실 전광판 이번 주 / 오늘 ·
//             `ScheduleDashboardWeekScreen` · `ScheduleDashboardTodayScreen` · 지금은 `physician` 하나)
//             `schedule-daily`(일자별 현황 — 고른 하루의 세션별 슬롯 현황 · 상태 · 마감 · `ScheduleDailyScreen` · 지금은 외래 하나)
//   대상 구분  `examination` · `injection` · `rehabilitation` · `education`(= 서버 `service_category`) · `physician`(외래 —
//             대상이 의사 한 명이라 이 낱말이고, 서버 값은 `outpatient-clinic`). 줄임말(`exam` · `rehab`)을 쓰지 않는다.
// 검사실 · 주사실 · 재활치료사 · 교육장 · 외래 의사는 **별도 화면이 아니다** — `schedule` 한 애그리게잇의 값이라
// 구분마다 다른 것은 메뉴 프리셋(`entities/schedule/model/schedule_menu.ts`)의 열이다.
//
// **리터럴 경로로 등록한다** — `schedule-slot/:category` 로 두면 host 메뉴가 리터럴만 등록해 다른 값에서
// 「접근 권한이 없습니다」가 난다(목록은 `SCHEDULE_MENU_KEYS` 의 면별 메뉴를 펼친다).
// `key` 는 메뉴 전환 시 **리마운트**를 강제한다 — 같은 컴포넌트 타입이라 React 가 인스턴스를 재사용하면
// 이전 메뉴에서 고른 대상·편집 중인 시간표가 남아 엉뚱한 시간표를 고치게 된다.
const routes: RouteObject[] = [
  {
    element: <RemoteScope />,
    children: [
      { path: 'appointment/booking', element: <AppointmentBooking /> },
      { path: 'appointment/outpatient', element: <OutpatientAppointment /> },
      // 예약 내역 조회 — 날짜 · 분류 · 예약 주체 · 검색어로 예약 목록(FHIR Appointment search · classdiagram 1.20).
      { path: 'appointment/search', element: <AppointmentSearch /> },
      // 항암 주사일정 관리 — 항암 약물군 주사 예약의 조회 · 일정 · 장소 변경 · 월간 현황(classdiagram 1.24).
      { path: 'appointment/chemotherapy-plan', element: <ChemotherapyPlan /> },
      // 환자별 예약 스케줄 — 세션 환자 한 명의 내원별 예약 · 미예약 처방 · 메모 · 치료시작일 · 약제 카운트(classdiagram 1.46).
      { path: 'appointment/patient-schedule', element: <PatientSchedule /> },
      ...SCHEDULE_MENU_KEYS['schedule-pattern'].map((key) => ({
        path: key,
        element: <SchedulePatternScreen key={key} menu={key} />,
      })),
      ...SCHEDULE_MENU_KEYS['schedule-slot'].map((key) => ({
        path: key,
        element: <ScheduleSlotScreen key={key} menu={key} />,
      })),
      ...SCHEDULE_MENU_KEYS['schedule-overview'].map((key) => ({
        path: key,
        element: <ScheduleOverviewScreen key={key} menu={key} />,
      })),
      ...SCHEDULE_MENU_KEYS['schedule-dashboard-week'].map((key) => ({
        path: key,
        element: <ScheduleDashboardWeekScreen key={key} menu={key} />,
      })),
      ...SCHEDULE_MENU_KEYS['schedule-dashboard-today'].map((key) => ({
        path: key,
        element: <ScheduleDashboardTodayScreen key={key} menu={key} />,
      })),
      ...SCHEDULE_MENU_KEYS['schedule-daily'].map((key) => ({
        path: key,
        element: <ScheduleDailyScreen key={key} menu={key} />,
      })),
      // 슬롯 마감 — 호출 화면 안 작은 팝업의 호출측 하네스(MF expose `./BookingClosureDialog` · classdiagram 1.42).
      { path: 'schedule/booking-closure', element: <BookingClosureHarness /> },
      // 진료시간 기준관리 — 병원 공통 · 진료과 예외의 요일별 진료시간 · 세션 기준값 · 예약 설정 · 휴진 · 공휴일(classdiagram 1.15).
      // 시안 파일명과 1:1(`_docs/view/configuration.html`).
      { path: 'configuration', element: <ConfigurationScreen /> },
      // 조회 API 안내 — 다른 시스템이 읽어 가는 진료일정 API 의 명세 · 시험 호출(개발자 간 의사소통용).
      { path: 'schedule/api-reference', element: <ScheduleApiReferenceScreen /> },
    ],
  },
]

export default routes
```

### 2.4 MF expose 진입점 — `src/exposes/OutpatientAppointmentDialog.tsx` (`src/exposes/OutpatientAppointmentDialog.tsx:1-21`)

```tsx
// MF expose 진입점(`patient_appointment/OutpatientAppointmentDialog`) — 외래 예약 대화상자. (classdiagram §1.9 · §2.18-b · 부록 P62 · P73)
//
// 호출 화면(환자접수 · 진료 화면 · CTI)이 app-kit `remoteComponent('patient_appointment', 'OutpatientAppointmentDialog')` 로 **자기 화면 안에**
// 띄운다 — 새 창으로 열지 않는다. props 계약은 `entities/appointment/model/outpatient_booking_contract.ts`(호출 화면은 그 파일을 복사한다).
//
// `./routes` 를 거치지 않고 들어오므로 이 remote 의 유틸리티 CSS 를 여기서도 import 하고, 킷 스코프(`khds-root`) ·
// 격리 경계(`x-remote-root`)를 routes 의 RemoteScope 와 같게 단다.
import '@/styles/globals.css'

import type { OutpatientAppointmentDialogProps } from '@/entities/appointment/model/outpatient_booking_contract'
import { OutpatientAppointmentDialog } from '@/widgets/appointment/OutpatientAppointmentDialog'

export type { OutpatientAppointmentDialogProps } from '@/entities/appointment/model/outpatient_booking_contract'

export default function OutpatientAppointmentDialogExpose(props: OutpatientAppointmentDialogProps) {
  return (
    <div className="x-remote-root khds-root">
      <OutpatientAppointmentDialog {...props} />
    </div>
  )
}
```

(`AppointmentBookingDialog.tsx` · `BookingClosureDialog.tsx` 도 같은 21줄 구조.)

### 2.5 `src/app/Standalone.tsx` — dev 전용 셸 (발췌 `src/app/Standalone.tsx:1-46`, `217-253`, `257-267`, `307-318`)

```tsx
import { createMockAuthContext } from '@x-ehr/dev-mocks'
import {
  Dropdown,
  DropdownLabel,
  DropdownRadioGroup,
  DropdownRadioItem,
  DropdownSeparator,
  Stack,
  ToastProvider,
} from '@x-ehr/design-system'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { Fragment, useEffect } from 'react'
import {
  createBrowserRouter,
  Navigate,
  Outlet,
  RouterProvider,
  useLocation,
  useNavigate,
} from 'react-router'
import routes from '@/routes'
import {
  SCHEDULE_MENU_KEYS,
  schedule_menu,
  type ScheduleMenuKey,
} from '@/entities/schedule/model/schedule_menu'
import { useSessionPatient } from '@/shared/patient-session/session_patient'
import {
  load_session_candidates,
  publish_session_patient_stand_in,
} from '@/shared/patient-session/session_patient_stand_in'
import { useFreshQuery } from '@/shared/lib/query_cache'

// Host 없이 단독 개발할 때만 쓰는 에뮬레이터 — mock auth·자체 QueryClient·dev 메뉴를 제공한다(standalone = dev only).
// 운영(Host nest)에선 Host 의 Provider·authContext·단일 QueryClient·단일 i18next 를 쓰고 **메뉴도 host 가 소유**한다
// (이 파일은 prod 빌드에서 제외되므로 remote 산출물에 이 층이 섞이지 않는다).
...
const mockAuth = createMockAuthContext()
const queryClient = new QueryClient()
```

```tsx
function DevShell() {
  const location = useLocation()
  const navigate = useNavigate()
  const current = screen_of(location.pathname)

  return (
    <div style={{ height: '100%', display: 'flex', flexDirection: 'column' }}>
      <Stack
        direction="row"
        gap={8}
        align="center"
        style={{
          flexShrink: 0,
          padding: '6px 12px',
          borderBottom: '1px solid var(--color-border-subtle)',
          background: 'var(--color-surface-panel)',
        }}
      >
        <DevMenu current={current} onPick={(path) => void navigate(`/${path}`)} />
        <span className="typo-caption-emphasis text-(--color-text-secondary)">
          patient-appointment (dev)
        </span>
        {current != null && (
          <span className="typo-caption text-(--color-text-primary)">
            {TITLE_BY_PATH.get(current)}
          </span>
        )}
        <Stack.Item grow />
        {/* 환자 세션 소유자(x-ancillary-main) 대역 — 운영에서는 host 의 환자리스트가 세션을 올린다. */}
        <SessionPatientMenu />
      </Stack>
      <div style={{ flex: '1 1 auto', minHeight: 0 }}>
        <Outlet context={mockAuth} />
      </div>
    </div>
  )
}

// 공유 routes 는 leaf-only(허브 없음) — 에뮬레이터만의 landing 으로 index→첫 화면 redirect 를 앞에 끼우고,
// 메뉴에 없는 경로(오타·구 링크)도 같은 곳으로 모은다.
const router = createBrowserRouter([
  {
    path: '/',
    element: <DevShell />,
    children: [
      { index: true, element: <Navigate to={LANDING} replace /> },
      ...routes,
      { path: '*', element: <Navigate to={`/${LANDING}`} replace /> },
    ],
  },
])
```

```tsx
export default function Standalone() {
  useShellChrome()
  // 운영(Host nest)에서는 host 셸이 ToastProvider 를 마운트한다 — dev 단독 실행일 때만 여기서 같은 Provider 를 세운다.
  // (useToast 는 Provider 밖에서 호출하면 throw 한다.)
  return (
    <QueryClientProvider client={queryClient}>
      <ToastProvider aria-label="알림">
        <RouterProvider router={router} />
      </ToastProvider>
    </QueryClientProvider>
  )
}
```

`Standalone.tsx:160-200` 의 `SessionPatientMenu` 는 x-ancillary-main 환자리스트에서 실제 환자를 골라 `publish_session_patient_stand_in` 으로 세션에 올린다(§7 참고). `LANDING = 'schedule-slot/examination'`(`Standalone.tsx:114`).


---

## 3. 화면 → API 추적표

> 전제: 이 레포에는 `features/` 레이어와 `entities/*/api/` 가 없다. HTTP 함수는 전부 `src/shared/api/appointmentApi.ts`(자기 BC) · `src/shared/api/ancillaryApi.ts`(x-ancillary-main) 두 파일에 있고, 조회 훅은 `entities/*/model/*.ts` 또는 widget/view 안에서 `useFreshQuery`/`useFreshQueries`(`shared/lib/query_cache.ts`)로, 쓰기는 `useMutation`(react-query) 또는 `async` 함수 직접 호출로 한다. 슬롯 정의·운영 화면의 서버 상태는 `entities/schedule/model/schedule_store.ts`(`useSyncExternalStore` 기반 모듈 store — zustand 아님)에 모인다.

### 3.1 라우트별 추적표

| 화면(path) | view 컴포넌트 | 사용하는 widget | (feature 대신) entity model 훅/함수 | entity api 함수(→ §3.2) | 호출 훅 | 상태 보관 |
|---|---|---|---|---|---|---|
| `appointment/booking` | `views/appointment/AppointmentBooking.tsx`(검사 예약 **호출측 하네스**) | `widgets/appointment/AppointmentBookingDialog.tsx` → `BookingOrderPanel` · `BookingTargetFilter` · `BookingCalendar` · `BookingTimetable` · `AutoSelectDialog` · `BookingConfirmDialog` · `MoveTogetherDialog` · `SaveReasonDialog` · `ScheduleRemarks` | `entities/appointment/model/booking_data.ts`: `useBookingSetup` · `useMonthDays` · `useDaySlots` · `useOverbookLimits` · `useAutoRecommendations` · `useRescheduleOrder` · `useInvalidateBooking`; 순수 함수 `booking.ts`(`to_targets` · `bundle_plan` · `assign_error` …) | `findAppointments` · `findSchedules` · `findAppointment` · `draftAppointment` · `findSlotDays` · `findSlots` · `findOverbookAllowances` · `findOptimalDates` · `findMovePartners` · `bookBundle` | `useFreshQuery` · `useFreshQueries`; 저장은 widget 안 `async` 직접 호출 + `useQueryClient().invalidateQueries` | 대화상자 로컬 `useState`(picks Map · filter) + react-query 캐시(`gcTime 0`); 하네스는 `mocks/appointment/booking_request_mock.ts` 의 `MOCK_BOOKING_ORDERS` 로 요청을 만든다 |
| `appointment/outpatient` | `views/appointment/OutpatientAppointment.tsx`(외래 예약 **호출측 하네스**) | `widgets/appointment/OutpatientAppointmentDialog.tsx` → `OutpatientFilterBar` · `OutpatientCalendar` · `OutpatientTimeChips` · `OutpatientRemarkNote` · `OutpatientAutoSelectDialog` · `SaveReasonDialog` · `ScheduleRemarks` | `entities/appointment/model/outpatient_booking_data.ts`: `useOutpatientSetup` · `useDepartmentSummaries` · `useMedicationSchedules` · `useMonthSlots` · `useRecommendations` · `useInvalidateOutpatient`; 순수 함수 `outpatient_booking.ts` | view: `findSchedules`; 훅: `findAppointment` · `findAppointments` · `findSchedules` · `draftAppointment` · `findDepartmentSummaries` · `findMedicationSchedules` · `findSlots` · `findOptimalDates`; widget: `bookBundle` | `useFreshQuery` · `useFreshQueries`; 저장은 `async` 직접 호출 | 대화상자 로컬 `useState` |
| `appointment/search` | `views/appointment/AppointmentSearch.tsx` | `widgets/appointment/search/AppointmentSearchFilterBar.tsx` · `AppointmentSearchTable.tsx`; 행 클릭 → `AppointmentBookingDialog` / `OutpatientAppointmentDialog` 재사용 | `entities/appointment/model/appointment_search.ts`(필터·`dialog_of`) · `booking_patient_of` | `findAppointments` · `findSchedules` | `useFreshQuery`(view 직접) + `useQueryClient` | view 로컬 `useState`(filter · 열린 대화상자) |
| `appointment/chemotherapy-plan` | `views/appointment/ChemotherapyPlan.tsx` | `widgets/appointment/chemotherapy/{ChemotherapyFilterBar,ChemotherapyTable,ChemotherapyMonthView,ChemotherapyMoveDialog,ChemotherapyCancelDialog,ChemotherapySeriesHistoryDialog,ChemotherapyChangeHistoryDialog}.tsx` · `widgets/chemotherapy-registration/{ChemotherapyRegistrationPanel,ChemotherapyRegistrationDialog,ChemotherapyCycleDialog}.tsx` | `entities/appointment/model/chemotherapy_plan.ts` · `appointment_search.ts` · `entities/chemotherapy-registration/model/chemotherapy_registration.ts` | view: `findAppointments` · `findSchedules`; MoveDialog: `findSlots` · `findMovePartners` · `rescheduleAppointment` · `bookBundle`; CancelDialog: `cancelAppointment`; SeriesHistory: `findSeriesChanges`; ChangeHistory: `findAppointmentChanges`; RegistrationPanel: `findChemotherapyRegistrations` · `expandValueSet`; RegistrationDialog: `registerChemotherapy` · `changeChemotherapyRegistration` · `findDepartments` · `findDepartmentPhysicians`; CycleDialog: `findChemotherapyRegistration` · `findChemotherapyRegistrationRevisions` | `useFreshQuery`; 쓰기 `useMutation` | view 로컬 `useState` |
| `appointment/patient-schedule` | `views/appointment/PatientSchedule.tsx` | `widgets/patient-schedule/{PatientScheduleHeader,PatientVisitTable,UnbookedOrderCard,MedicationCountCard,PatientScheduleNoteHistoryDialog}.tsx` + 두 예약 대화상자 | `entities/patient-schedule/model/patient_schedule.ts` · `entities/appointment/model/session_orders.ts`(`useSessionOrders` · `to_booking_order`) · `useSessionPatient` | view: `findPatientSchedule` · `findChemotherapyRegistrations`; `useSessionOrders` → **ancillary** `findIssuedOrders`; Header: `savePatientScheduleNote`(useMutation); NoteHistory: `findPatientScheduleNoteRevisions` | `useFreshQuery` · `useMutation` | 세션 환자는 event-bus(§7) · 나머지 view 로컬 |
| `schedule-pattern/{examination,injection,dialysis,rehabilitation,education,outpatient-clinic}` (6) | `views/schedule/SchedulePatternScreen.tsx`(`menu` prop = 라우트 키) | `widgets/schedule/shell/{useScheduleShell,ShellTargetList,NarrowPicker,PatternDeleteDialog}` · `aside/PatternListRows` · `pattern/{RulePatternPane,OutpatientPatternPane,PatternEditorFrame,PatternGrid,OutpatientPatternGrid,GenerationRulePanel,GenerationRuleRow,PatternCopyDialog,PatternMoreMenu,RuleDeleteDialog,SortableLine,useEditedPattern}` | `entities/schedule/model/schedule_store.ts`(`useScheduleData` · `schedule_actions` · `load_schedules` · `load_patterns` · `useScheduleStoreLifetime`) · `pattern.ts` · `schedule_api.ts`(어댑터) · `schedule_menu.ts` · `substitute.ts`(`useSubstituteCandidates`) | `useScheduleShell`: `findSlotTargets`; store: `findSchedules` · `findPatterns` · `findPattern` · `createPattern` · `replacePattern` · `deactivatePattern` · `restorePattern` · `copyPatternTo` · `createGenerationRule` · `updateGenerationRule` · `deleteGenerationRule` · `reorderGenerationRules`; `create_schedule_for_target`: `findSchedules` · `createSchedule` | store 는 `useSyncExternalStore`; shell 은 `useFreshQuery` | **모듈 store**(`schedule_store.ts`) — 패턴 버퍼 `draft`/`pending`/`versions`(If-Match); 화면 닫힐 때 `useScheduleStoreLifetime` 이 `reset_schedule_store()` |
| `schedule-slot/{examination,injection,dialysis,rehabilitation,education,outpatient-clinic}` (6) | `views/schedule/ScheduleSlotScreen.tsx` | `widgets/schedule/operation/{OperationWorkspace,OperationMonthCalendar,OperationBulkPanel,YearStrip,SlotModeControls,SlotTimeRow,TimeEditDialog,DaySlotDeleteDialog,BookingCloseDialog,SelectionUnavailabilityDialog,SubstituteDialog,SubstitutePicker,ReassignmentDialog,ScheduleCommentDialog,PeriodCommentPanel,OverbookAllowanceDialog,OutpatientSessionEditor,OutpatientSessionRow,TabScrollGuard,block_editor,day_selection,operation_terms}` · `aside/{PhysicianSummaryTable,TargetAppliedSummary}` · `common/NotAvailableBadge` · `shell/*` | `schedule_store.ts` · `day_operation.ts`(`run_day_overrides` · `run_create` · `run_block` · `run_booking_close` …) · `operation_days.ts`(`useOperationDays`) · `period_comment.ts`(`usePeriodComments`) · `overbook_allowance.ts` · `physician_lines.ts` · `category_departments.ts`(`useCategoryDepartments`) · `create_schedule_for_target.ts` | view: `findDepartmentPhysicians` · `findDepartmentSummaries` · `findDepartments`(via 훅) · `createSchedule`; store: `findPatternAssignments` · `assignPattern` · `unassignPattern` · `findUnavailabilityRules` · `createUnavailabilityRule` · `patchUnavailabilityRule` · `deleteUnavailabilityRule` · `findDayOverrides` · `putDayOverride`; `useOperationDays`: `findScheduleDays`; `day_operation`: `runScheduleOperation` · `findDayOverrides`; OperationWorkspace: `expandValueSet` · `findReassignmentTasks`; SlotModeControls: `findSchedule` · `patchSchedule`; ScheduleCommentDialog: `findSchedule` · `findScheduleComments` · `putScheduleComment`; PeriodCommentPanel: `findPeriodComments` · `addPeriodComment` · `putPeriodComment` · `removePeriodComment` · `findPeriodCommentRevisions`; OverbookAllowanceDialog: `findOverbookAllowances` · `addOverbookAllowance` · `putOverbookAllowance` · `removeOverbookAllowance` · `findOverbookAllowanceRevisions`; ReassignmentDialog: `findReassignmentTasks` · `completeReassignmentTask` · `cancelReassignmentTask` · `findAppointment`; SubstitutePicker: `findSlotTargets` | `useSyncExternalStore` + `useFreshQuery`/`useFreshQueries` + `useMutation`(대화상자) + `async` 직접(day_operation) | 모듈 store + widget 로컬 `useState`(선택된 날짜 집합 `day_selection.ts`) |
| `schedule-overview/outpatient-clinic` | `views/schedule/ScheduleOverviewScreen.tsx` | `widgets/schedule/overview/{OverviewFilterBar,OverviewTable}.tsx` | `schedule_overview.ts` · `category_departments.ts` · `schedule_store.ts`(일부) | `findScheduleOverview` · `findDepartmentPhysicians` · `findDepartments` | `useFreshQuery` · `useFreshQueries` | view 로컬 |
| `schedule-dashboard-week/outpatient-clinic` · `schedule-dashboard-today/outpatient-clinic` | `views/schedule/ScheduleDashboardWeekScreen.tsx` · `ScheduleDashboardTodayScreen.tsx` | `widgets/schedule/dashboard/{DashboardScreenFrame,DashboardStage,WeekBoard,TodayBoard,board_layout,useBoardScale,useBoardTimers,dashboard.css}` | `schedule_dashboard.ts`(`week_range` · `weekday_of` · `month_day`) | `findScheduleDashboard`(DashboardScreenFrame) | `useFreshQuery`(주기 재조회는 `useBoardTimers`) | 없음(전광판) |
| `schedule-daily/outpatient-clinic` | `views/schedule/ScheduleDailyScreen.tsx` | `widgets/schedule/daily/{DailyFilterBar,DailyTable}.tsx` · `widgets/schedule/closure/BookingClosureDialog.tsx` | `schedule_daily.ts`(`useDailyStatus` · `booking_closure_api`) · `category_departments.ts` · `booking_closure_contract.ts` | `findScheduleDailyStatus` · `closeBooking` · `reopenBooking` · `findDepartmentPhysicians` · `findDepartments` | `useFreshQueries` · `useMutation`(BookingClosureDialog) | view 로컬 |
| `schedule/booking-closure` | `views/schedule/BookingClosureHarness.tsx`(슬롯 마감 **호출측 하네스**) | `widgets/schedule/closure/BookingClosureDialog.tsx` | `booking_closure.ts` · `booking_closure_contract.ts` · `schedule_daily.ts`(`booking_closure_api`) | `findSchedules`(하네스) · `closeBooking` · `reopenBooking` | `useFreshQuery` · `useMutation` | 로컬 |
| `configuration` | `views/clinic-hours/ConfigurationScreen.tsx` | `widgets/clinic-hours/{CommonRulesPanel,WeekdayHoursTable,SessionDefaultTable,BookingPolicyPanel,PolicyUnitList,AddUnitDialog,ClosurePanel,ClosureDialog,TimeInput}.tsx` | `entities/clinic-hours/model/{clinic_hours,closure,effective_clinic_hours}.ts` | `findClinicHoursPolicies` · `createClinicHoursPolicy` · `replaceClinicHoursPolicy` · `deleteClinicHoursPolicy` · `findGlobalUnavailabilityRules` · `createGlobalUnavailabilityRule` · `deleteUnavailabilityRule` · `syncHolidays` · `findDepartments` | `useFreshQuery` · `useMutation`(view 직접) + `is_version_conflict` 로 409 처리 | view 로컬 `useState`(편집본) |
| `schedule/api-reference` | `views/schedule/ScheduleApiReferenceScreen.tsx` | `widgets/schedule/api-reference/{ApiDocPanel,ApiSpecTables,ApiTryPanel,EventTryPanel,ExamBookingTryPanel,ComponentTryPanel,OutpatientDeferredTryPanel,ClosureTryPanel}` + `parts/*` | `schedule_api_catalog.ts`(2,747줄 — 명세 데이터) · `session_orders.ts` | `probeReadApi` · `probeWriteApi` · **ancillary** `publishOutpatientBooking` · `findIssuedOrders` | `useMutation`(시험 호출) · `useFreshQuery` | 로컬 |

MF expose(라우트 아님): `./AppointmentBookingDialog` · `./OutpatientAppointmentDialog` · `./BookingClosureDialog` — 각각 위 표의 widget 을 `x-remote-root khds-root` 로 감싸 내보낸다(`src/exposes/*.tsx`).

### 3.2 API 함수 전체 표 — `src/shared/api/appointmentApi.ts` (`BASE = '/api/v1/patient-appointment'`, `appointmentApi.ts:19`)

| 함수 | 줄 | HTTP | 경로(BASE 생략) | 비고 |
|---|---|---|---|---|
| `expandValueSet(valueSetId, actorType?)` | 295 | GET | `/value-sets/{valueSetId}/expand?actorType` | |
| `findSchedules(params)` | 361 | GET | `/schedules?…`(`csv()` 로 배열 파라미터) | |
| `findSchedule(scheduleId)` | 377 | GET | `/schedules/{id}` | |
| `createSchedule(body)` | 415 | POST | `/schedules` | |
| `patchSchedule(scheduleId, ext, version?)` | 428 | PATCH | `/schedules/{id}` | `If-Match` |
| `putScheduleComment(scheduleId, body, version?)` | 456 | PUT | `/schedules/{id}/comment` | `If-Match` |
| `findScheduleComments(scheduleId)` | 466 | GET | `/schedules/{id}/comments` | |
| `findPeriodComments(scheduleIds, from?, to?)` | 521 | GET | `/period-comments?…` | 기간 비고 |
| `addPeriodComment(scheduleId, body)` | 529 | POST | `/schedules/{id}/period-comments` | |
| `putPeriodComment(periodCommentId, body, version?)` | 536 | PUT | `/period-comments/{id}` | `If-Match` |
| `removePeriodComment(periodCommentId, version?)` | 547 | POST | `/period-comments/{id}/remove` | 삭제도 POST 동작 조각 |
| `findPeriodCommentRevisions(scheduleId)` | 553 | GET | `/schedules/{id}/period-comment-revisions` | |
| `findOverbookAllowances(scheduleId)` | 606 | GET | `/schedules/{id}/overbook-allowances` | |
| `addOverbookAllowance(scheduleId, body)` | 612 | POST | `/schedules/{id}/overbook-allowances` | |
| `putOverbookAllowance(id, body, version?)` | 619 | PUT | `/overbook-allowances/{id}` | `If-Match` |
| `removeOverbookAllowance(id, version?)` | 630 | POST | `/overbook-allowances/{id}/remove` | |
| `findOverbookAllowanceRevisions(scheduleId)` | 636 | GET | `/schedules/{id}/overbook-allowance-revisions` | |
| `findDepartmentSummaries(departmentId, asOf?)` | 666 | GET | `/departments/{id}/schedules/summary?asOf` | |
| `findMedicationSchedules(patient)` | 685 | GET | `/medication-schedules?patient` | |
| `findScheduleOverview(params)` | 770 | GET | `/schedule-overview?…` | |
| `findScheduleDashboard(params)` | 843 | GET | `/schedule-dashboard?…` | 현황판 |
| `findScheduleDailyStatus(params)` | 934 | GET | `/schedule-daily-status?…` | |
| `closeBooking(body)` | 956 | POST | `/booking-closures` | 슬롯 마감 |
| `reopenBooking(rule_id)` | 961 | DELETE | `/booking-closures/{rule_id}` | |
| `probeReadApi(path, params)` | 979 | GET | `{path}`(임의) | API 안내 시험 호출 |
| `probeWriteApi(path, body)` | 1002 | POST | `{path}`(임의) | |
| `findDepartments(serviceCategory)` | 1029 | GET | `/departments?serviceCategory` | |
| `findDepartmentPhysicians(departmentId)` | 1054 | GET | `/departments/{id}/physicians` | |
| `findSlotTargets(serviceCategory)` | 1085 | GET | `/slot-targets?serviceCategory` | |
| `findClinicHoursPolicies()` | 1281 | GET | `/clinic-hours-policies` | |
| `findEffectiveClinicHours(departmentId?)` | 1286 | GET | `/clinic-hours-policies/effective?…` | |
| `createClinicHoursPolicy(body)` | 1295 | POST | `/clinic-hours-policies` | |
| `replaceClinicHoursPolicy(policyId, body, version?)` | 1304 | PUT | `/clinic-hours-policies/{id}` | `If-Match` |
| `deleteClinicHoursPolicy(policyId)` | 1315 | DELETE | `/clinic-hours-policies/{id}` | |
| `findGlobalUnavailabilityRules(params)` | 1322 | GET | `/unavailability-rules?from&to` | 전역 휴무 |
| `createGlobalUnavailabilityRule(body)` | 1329 | POST | `/unavailability-rules` | |
| `syncHolidays(params)` | 1334 | POST | `/unavailability-rules/holidays/sync?from&to` | |
| `findPatterns(scheduleId, includeInactive)` | 1476 | GET | `/schedules/{id}/patterns?includeInactive` | |
| `findPattern(patternId)` | 1482 | GET | `/patterns/{id}` | |
| `createPattern(scheduleId, body, from?)` | 1486 | POST | `/schedules/{id}/patterns?from` | |
| `replacePattern(patternId, body, version?)` | 1496 | PUT | `/patterns/{id}` | `If-Match` |
| `deactivatePattern(patternId)` | 1511 | POST | `/patterns/{id}/deactivate` | |
| `restorePattern(patternId)` | 1515 | POST | `/patterns/{id}/restore` | |
| `copyPatternTo(patternId, body)` | 1519 | POST | `/patterns/{id}/copy-to` | |
| `createGenerationRule(patternId, body, …)` | 1534 | POST | `/patterns/{id}/generation-rules` | |
| `updateGenerationRule(ruleId, body, version?)` | 1548 | PUT | `/generation-rules/{id}` | `If-Match` |
| `deleteGenerationRule(ruleId)` | 1559 | DELETE | `/generation-rules/{id}` | |
| `reorderGenerationRules(patternId, ruleIds)` | 1564 | PUT | `/patterns/{id}/generation-rules/order` | |
| `findPatternAssignments(scheduleId)` | 1613 | GET | `/schedules/{id}/pattern-assignments` | |
| `assignPattern(scheduleId, body, dryRun)` | 1620 | POST | `/schedules/{id}/pattern-assignments/assign?dryRun` | |
| `unassignPattern(assignmentId)` | 1628 | DELETE | `/pattern-assignments/{id}` | |
| `findUnavailabilityRules(scheduleId, params)` | 1700 | GET | `/schedules/{id}/unavailability-rules?…` | |
| `createUnavailabilityRule(scheduleId, body, dryRun)` | 1713 | POST | `/schedules/{id}/unavailability-rules?dryRun` | |
| `patchUnavailabilityRule(ruleId, body)` | 1723 | PATCH | `/unavailability-rules/{id}` | |
| `deleteUnavailabilityRule(ruleId)` | 1732 | DELETE | `/unavailability-rules/{id}` | |
| `findDayOverrides(scheduleId, from, to)` | 1785 | GET | `/schedules/{id}/day-overrides?from&to` | |
| `putDayOverride(scheduleId, date, body, dryRun)` | 1792 | PUT | `/schedules/{id}/day-overrides/{date}?dryRun` | |
| `findScheduleDays(scheduleId, from, to)` | 1845 | GET | `/schedules/{id}/schedule-days?from&to` | 달력 전개 정본 |
| `runScheduleOperation(scheduleId, body)` | 1935 | POST | `/schedules/{id}/operations` | 일괄 운영 |
| `findReassignmentTasks(scheduleId, params)` | 1983 | GET | `/schedules/{id}/reassignment-tasks?…` | |
| `completeReassignmentTask(taskId, resolvedSlotId)` | 1992 | POST | `/reassignment-tasks/{id}/complete` | |
| `cancelReassignmentTask(taskId, reason)` | 1998 | POST | `/reassignment-tasks/{id}/cancel` | |
| `findSlots(params)` | 2075 | GET | `/slots?…` | |
| `findSlotDays(params)` | 2095 | GET | `/slots/find?…` | 월 단위 날짜 상태 |
| `findAppointments(params)` | 2359 | GET | `/appointments?…` | |
| `findAppointment(appointmentId)` | 2382 | GET | `/appointments/{id}` | |
| `draftAppointment(body)` | 2446 | POST | `/appointments/draft` | `proposed` 초안 |
| `findOptimalDates(body)` | 2503 | POST | `/appointments/find-optimal-dates` | 자동 추천 |
| `rescheduleAppointment(id, target, version?, change?)` | 2516 | PATCH | `/appointments/{id}/reschedule` | `If-Match` |
| `cancelAppointment(id, reason, note, version?)` | 2539 | POST | `/appointments/{id}/cancel` | `If-Match` |
| `findSeriesChanges(seriesGroupId)` | 2573 | GET | `/appointment-series/{id}/changes` | |
| `findMovePartners(appointmentId, date?)` | 2647 | GET | `/appointments/{id}/move-partners?date` | 함께 옮기기 |
| `findAppointmentChanges(appointmentId)` | 2671 | GET | `/appointments/{id}/changes` | |
| `bookBundle(plan, ext?, cancel, change?)` | 2678 | POST | `/appointments/book-bundle` | **예약 등록 본체**(검사·외래 공통) |
| `findChemotherapyRegistrations(params)` | 2803 | GET | `/chemotherapy-registrations?…` | |
| `findChemotherapyRegistration(id)` | 2821 | GET | `/chemotherapy-registrations/{id}` | |
| `findChemotherapyRegistrationRevisions(id)` | 2827 | GET | `/chemotherapy-registrations/{id}/revisions` | |
| `registerChemotherapy(body)` | 2834 | POST | `/chemotherapy-registrations` | |
| `changeChemotherapyRegistration(id, body, version?)` | 2841 | PUT | `/chemotherapy-registrations/{id}` | `If-Match` |
| `findPatientSchedule(patientId, …)` | 2923 | GET | `/patient-schedules/{patientId}?…` | |
| `findPatientScheduleNoteRevisions(patientId)` | 2934 | GET | `/patient-schedule-notes/{patientId}/revisions` | |
| `savePatientScheduleNote(patientId, body, version?)` | 2941 | PUT | `/patient-schedule-notes/{patientId}` | `If-Match` |

`src/shared/api/ancillaryApi.ts` (`BASE = '/api/v1/ancillary-main'`, `ancillaryApi.ts:15`) — 다른 BC 에 직접 묻는 길은 이 다섯뿐:

| 함수 | 줄 | HTTP | 경로 | 쓰는 곳 |
|---|---|---|---|---|
| `findIssuedOrders(patient_no)` | 61 | GET | `/orders?patient_no&status=ISSUED&limit=500` | `entities/appointment/model/session_orders.ts` |
| `findResidentPatients(size)` | 70 | GET | `/patient-list/residents?size` | `shared/patient-session/session_patient_stand_in.ts`(dev) |
| `findOutpatientPatients(size)` | 76 | GET | `/patient-list/outpatients?size` | 같음 |
| `findPatientContext(encounter_id)` | 82 | GET | `/patient-context?encounter_id` | 같음 |
| `publishOutpatientBooking(body)` | 105 | POST | `/appointment-link/outpatient-bookings`(timeout 50초) | `widgets/schedule/api-reference/OutpatientDeferredTryPanel.tsx` |


---

## 4. 데이터 계층 샘플 — 슬롯 운영 작업면(`schedule-slot/*`)의 달력 한 장, 끝에서 끝까지

체인: `shared/lib/query_cache.ts` → `shared/api/appointmentApi.ts`(`request` · `ApiError` · `findScheduleDays`) → `entities/schedule/model/day_operation.ts`(`OperationDay` 타입) → `entities/schedule/model/operation_days.ts`(`useOperationDays` 훅 = feature model 역할) → `widgets/schedule/operation/OperationWorkspace.tsx` → `widgets/schedule/operation/OperationMonthCalendar.tsx` → `views/schedule/ScheduleSlotScreen.tsx`.

### 4.1 `useFreshQuery` (`src/shared/lib/query_cache.ts:11-48`)

```ts
/**
 * 조회는 **늘 DB 의 최신 값을 읽는다** — 이 remote 의 모든 react-query 조회가 이 파일의 [useFreshQuery] · [useFreshQueries] 를 거친다
 * (`@tanstack/react-query` 의 `useQuery` · `useQueries` 를 직접 쓰지 않는다 — `eslint.config.mjs` 가 막는다).
 *
 * @remarks
 * QueryClient 는 호출 화면(host)의 것이고 host 기본값이 `staleTime` 5분 · `gcTime` 10분이다(x-ehr-foundation `QueryProvider`).
 * 그대로 두면 5분 안에 다시 연 화면이 서버에 묻지 않고 처음 읽은 값을 보여 주고, 화면 밖에서 바뀐 값(다른 사용자 · 이벤트로 생긴 예약)이 안 보인다.
 * 그래서 받은 값을 최신으로 치지 않고(`staleTime: 0`) 쓰는 화면이 없어지면 곧바로 버린다(`gcTime: 0`) — 다시 열면 캐시 없이 서버에서 새로 읽고
 * 이전 값을 먼저 보여 주지 않는다. 열려 있는 동안 창에 다시 들어와도 새로 읽는다.
 * 화면이 열려 있는 동안 조건만 바꿀 때 이전 결과를 잠깐 들고 있는 것(`placeholderData`)은 이 규칙과 별개다 — 다시 연 화면에는 남지 않는다.
 */
export const READ_FROM_DB = { staleTime: 0, gcTime: 0 } as const

/** `useQuery` 와 같다 — 캐시 옵션만 [READ_FROM_DB] 로 고정한다(호출이 준 `staleTime` · `gcTime` 은 무시된다). */
export function useFreshQuery<
  TQueryFnData = unknown,
  TError = DefaultError,
  TData = TQueryFnData,
  TQueryKey extends QueryKey = QueryKey,
>(options: UseQueryOptions<TQueryFnData, TError, TData, TQueryKey>): UseQueryResult<TData, TError> {
  return useQuery({ ...options, ...READ_FROM_DB })
}

/**
 * `useQueries` 와 같다 — 쿼리마다 캐시 옵션을 [READ_FROM_DB] 로 고정한다.
 * 타입은 `useQueries` 의 시그니처를 그대로 받는다(쿼리 목록에서 결과 타입을 추론한다).
 */
export function useFreshQueries<T extends unknown[], TCombinedResult = QueriesResults<T>>(
  options: Parameters<typeof useQueries<T, TCombinedResult>>[0],
): TCombinedResult {
  return useQueries<T, TCombinedResult>({
    ...options,
    queries: (options.queries as readonly object[]).map((query) => ({
      ...query,
      ...READ_FROM_DB,
    })) as unknown as typeof options.queries,
  })
}
```

### 4.2 공용 HTTP 베이스 — api-axios 사용 · 경로 조립 · 에러 변환 (`src/shared/api/appointmentApi.ts:1-19`, `243-289`)

```ts
// 통합예약(patient-appointment) BC API 클라이언트.
//
// 화면이 쓰는 호출만 둔다(계약 정본은 `_docs/design/patient-appointment.api.md`). 애그리게잇이 둘(`schedule` · `appointment`)이라
// 이 파일도 그 두 덩어리로 나뉜다.
//
// **표기 규약이 방향마다 다르다** — 이 경계가 그것을 흡수하는 유일한 지점이다:
//   · 응답(view_model)은 백엔드와 **snake_case 그대로** 1:1 정합한다.
//   · 요청 본문은 **FHIR camelCase**(`slotDuration` · `capacityPerSlot` · `cycleType`)를 쓰고,
//     로컬 확장은 `ext` 객체에 담는다.
// 다른 파일은 이 규약을 신경 쓸 필요가 없다.
//
// 상태·유형 값은 **FHIR 코드**(`busy-unavailable` · `central-open` 처럼 소문자-하이픈)다 —
// 화면이 대문자 enum 을 기대하고 비교하면 조용히 어긋난다.
//
// 호출은 foundation 표준 @x-ehr/api-axios(`api`)에 위임한다 — Remote 자체 fetch/토큰 보유 금지
// (인스턴스 분열 시 401 refresh·토큰이 깨진다).
import { api } from '@x-ehr/api-axios'

const BASE = '/api/v1/patient-appointment'
```

```ts
export class ApiError extends Error {
  constructor(
    readonly status: number,
    readonly code: ApiErrorCode,
    message: string,
    readonly issues: IssueView[] = [],
    readonly details?: Record<string, unknown>,
  ) {
    super(message)
    this.name = 'ApiError'
  }
}

interface ProblemBody {
  title?: string
  detail?: string
  code?: string
  issue?: IssueView[]
  details?: Record<string, unknown>
}

function to_api_error(error: unknown): ApiError {
  const response = (error as { response?: { status?: number; data?: ProblemBody } }).response
  const body = response?.data
  const first = body?.issue?.[0]
  return new ApiError(
    response?.status ?? 0,
    body?.code ?? issue_code(first) ?? 'UNKNOWN',
    body?.detail ?? body?.title ?? (error as Error).message ?? '요청을 처리하지 못했습니다',
    body?.issue ?? [],
    body?.details,
  )
}

async function request<T>(run: () => Promise<{ data: T }>): Promise<T> {
  try {
    const response = await run()
    return response.data
  } catch (error) {
    throw to_api_error(error)
  }
}

/** `If-Match` — 값이 없으면 헤더를 아예 보내지 않는다(서버가 검사하지 않는다). */
function if_match(version: number | null | undefined) {
  return version == null ? undefined : { headers: { 'If-Match': String(version) } }
}
```

오류 → 문구는 `src/shared/lib/errors.ts:8-15` 의 `error_message(error, fallback)` · `is_version_conflict(error)`(`ApiError.status === 409`) 한 곳에서 한다.

### 4.3 entity api 함수 1개 + 요청/응답 타입 — `findScheduleDays` (`src/shared/api/appointmentApi.ts:1805-1849`)

```ts
export interface ScheduleDaySlotView {
  slot_id: string | null
  start_at: string | null
  end_at: string | null
  start_time: string
  end_time: string
  capacity: number
  booked_count: number
  status: SlotStatus
  eligibility: EligibilityView
}

export interface ScheduleDayView {
  date: string
  closed_reason: DayClosedReasonCode
  assignment_id: string | null
  pattern_id: string | null
  pattern_name: string | null
  day_index: number | null
  unavailability_rule_id: string | null
  unavailability_reason: string | null
  /** 있으면 그 날은 **조정 결과**이고 휴무 규칙을 보지 않은 것이다(불변식 14). */
  override_id?: string | null
  override_kind?: DayOverrideKind | null
  planned_slot_count: number
  planned_capacity: number
  activated_slot_count: number
  booked_count: number
  unavailable_count: number
  slots: ScheduleDaySlotView[]
}

interface ScheduleDaysView {
  schedule_id: string
  actor_id: string
  from: string
  to: string
  days: ScheduleDayView[]
}

export function findScheduleDays(scheduleId: string, from: string, to: string) {
  return request<ScheduleDaysView>(() =>
    api.get(`${BASE}/schedules/${scheduleId}/schedule-days`, { params: { from, to } }),
  )
}
```

### 4.4 entity model 타입 — `OperationDay` (`src/entities/schedule/model/day_operation.ts:59-75`)

```ts
export interface OperationDay {
  date: IsoDate
  /** 0 = 일 … 6 = 토(JS). */
  weekday: number
  /** 각인된 슬롯이 있다 — 「생성됨」. */
  generated: boolean
  /** 계획(반영)은 있으나 아직 각인되지 않았다 — 「미생성」. */
  planned: boolean
  slot_count: number
  capacity: number
  booked: number
  /** 휴무 규칙이 그 날 전체를 닫았다 — `common` 은 병원 공용(읽기 전용). */
  blocked: { reason: string; common: boolean } | null
  /** 그 날 열린 세션 — 종일 · 오전만 · 오후만. 슬롯이 없으면 null. */
  session: SessionPick | null
  plan: DayPlan
}
```

### 4.5 (feature 역할) entity model 훅 — `useOperationDays` (`src/entities/schedule/model/operation_days.ts:1-71`)

```ts
import { keepPreviousData } from '@tanstack/react-query'
import { useMemo } from 'react'
import { findScheduleDays } from '@/shared/api/appointmentApi'
import {
  to_operation_days,
  type OperationDay,
} from '@/entities/schedule/model/day_operation'
import { to_month_days } from '@/entities/schedule/model/schedule_api'
import { is_stand_in_schedule_id } from '@/entities/schedule/model/schedule'
import { useScheduleData } from '@/entities/schedule/model/schedule_store'
import type { IsoDate } from '@/shared/lib/datetime'
import { useFreshQuery } from '@/shared/lib/query_cache'

const ISO_DATE = /^\d{4}-\d{2}-\d{2}$/

const EMPTY: ReadonlyMap<IsoDate, OperationDay> = new Map()

/**
 * 그 기간의 **운영 하루**(슬롯 운영 — 외래 진료일정 · 자원 슬롯 메뉴 공통) — 서버 전개(`schedule-days`) 한 번 + 반영 · 패턴 · 저장된 조정 · 휴무 규칙.
 * 연간 스트립(12/24개월)과 일괄 작업 미리보기(패널 기간)가 같은 훅을 쓴다 — 키가 같은 줄기
 * (`['schedule-days', id, …]`)라 명령 뒤 무효화 한 번으로 둘 다 새로 읽는다.
 * 기간을 고치는 동안 이전 값을 들고 있는다(`keepPreviousData`) — 미리보기가 타자마다 0 으로 깜빡이지 않게.
 */
export function useOperationDays(schedule_id: string, from: IsoDate, to: IsoDate) {
  const { assignments, patterns, actor_holiday_rules, global_holiday_rules, saved_overrides } =
    useScheduleData()
  // 대역 id(아직 없는 예약 기준)는 읽지 않는다 — 빈 날들(반영 없음)로 본다.
  const valid =
    schedule_id !== '' &&
    !is_stand_in_schedule_id(schedule_id) &&
    ISO_DATE.test(from) &&
    ISO_DATE.test(to) &&
    from <= to
  const query = useFreshQuery({
    queryKey: ['schedule-days', schedule_id, from, to],
    queryFn: () => findScheduleDays(schedule_id, from, to),
    enabled: valid,
    placeholderData: keepPreviousData,
  })

  const by_date = useMemo(() => {
    const server_days = query.data?.days ?? []
    if (server_days.length === 0) return EMPTY
    const booked = new Map(server_days.map((day) => [day.date, day.booked_count]))
    const month_days = to_month_days({
      days: server_days,
      assignments: assignments[schedule_id] ?? [],
      patterns: patterns[schedule_id] ?? [],
      saved_overrides: saved_overrides[schedule_id] ?? {},
      overrides: {},
      booked_of: (date) => booked.get(date) ?? 0,
      tasks_of: () => 0,
    })
    const days = to_operation_days({
      month_days,
      server_days,
      rules: [...(actor_holiday_rules[schedule_id] ?? []), ...global_holiday_rules],
    })
    return new Map(days.map((day) => [day.date, day]))
  }, [
    query.data,
    assignments,
    patterns,
    saved_overrides,
    actor_holiday_rules,
    global_holiday_rules,
    schedule_id,
  ])

  return { by_date, loading: query.isFetching }
}
```

### 4.6 widget — `OperationWorkspace` 가 훅을 부르고 달력에 내려준다 (`src/widgets/schedule/operation/OperationWorkspace.tsx:224-260`, `368-375`, `831-866`)

```tsx
export function OperationWorkspace({
  target,
  service_category,
  terms,
  editor,
  missing_pattern_hint,
  toolbar_start,
  toolbar_end,
  on_changed,
  cell_detail = 'slots_capacity',
  pending = false,
  ensure_schedule_id,
}: {
  target: Schedule
  /** 메뉴의 예약 구분 — 진료불가 대진 후보의 원천(`GET /slot-targets?serviceCategory=` · classdiagram 1.40). */
  service_category: ServiceCategory
  terms: OperationTerms
  editor: BlockEditor
  /** 걸린 시간표가 없을 때 툴바에 적는 한 줄 — 어디서 만드는지. */
  missing_pattern_hint: string
  /** 툴바 왼쪽 — 좌측 목록이 접혔을 때의 대상 콤보 등. */
  toolbar_start?: ReactNode
  /** 툴바 오른쪽 — 보기 전환 등. */
  toolbar_end?: ReactNode
  /** 명령이 끝난 뒤 화면이 따로 새로 읽을 것(좌측 비교표 등). */
  on_changed?: () => void
  /** 칸의 형상 표기 — 기본 「N슬롯 · N명」, 외래는 「N슬롯」. */
  cell_detail?: DayCellDetail
  /** 예약 기준이 아직 없다 — `target` 은 대역(`stand_in_schedule`)이고 그 id 로는 요청을 보내지 않는다. */
  pending?: boolean
  /** 첫 쓰기 직전에 실제 예약 기준을 만들고 그 id 를 돌려준다(`pending` 일 때만 부른다 · 진행 중이면 같은 약속). */
  ensure_schedule_id?: () => Promise<string>
}) {
  const { toast } = useToast()
  const query_client = useQueryClient()
  const { patterns, assignments, actor_holiday_rules } = useScheduleData()
  const target_id = target.id
```

```tsx
  /**
   * 일자 데이터는 **스트립과 상세 2개월을 따로 읽는다** — 스트립을 옮겨도 아래 달력은 마지막에 고른 달을 그대로 보이고
   * 데이터도 그대로다(스트립 범위 하나로만 읽으면 스트립이 상세 달을 벗어나는 순간 달력이 비거나 다시 읽혀 깜빡인다).
   * 둘이 겹치면 상세 쪽 값이 이긴다 — 같은 서버 전개라 값은 같다.
   */
  const { by_date: strip_days } = useOperationDays(target_id, strip_from, strip_to)
  const { by_date: detail_days } = useOperationDays(target_id, detail_from, detail_to)
  const days = useMemo(() => new Map([...strip_days, ...detail_days]), [strip_days, detail_days])
```

```tsx
            <YearStrip
              months={strip_months}
              on_shift={(delta) => set_strip_start((current) => shift_month(current, delta))}
              days={strip_days}
              detail_months={detail_months}
              selected={selected}
              on_pick_month={show_detail}
              block_label={terms.block}
            />

            <Stack.Item
              grow
              className="op-months min-h-0 overflow-auto px-(--spacing-12) py-(--spacing-8)"
            >
              {/* 래퍼는 **포인터 전용 보강**이다(칸 누르기 · 끌기 · Shift · Ctrl — `useRangeSelection`). 키보드는
                  칸 안의 날짜 버튼 · 요일 머리글 버튼으로 들어온다(역할·tabindex 를 덧붙이면 두 번 읽힌다). */}
              {/* eslint-disable-next-line jsx-a11y/no-static-element-interactions */}
              <div
                className={`op-two ${selection.area_props.className}`}
                onMouseDown={selection.area_props.onMouseDown}
                onMouseMove={selection.area_props.onMouseMove}
              >
                {detail_months.map((month, index) => (
                  <OperationMonthCalendar
                    key={month}
                    month={month}
                    days={days}
                    selected={selected}
                    today={today}
                    terms={terms}
                    cell_detail={cell_detail}
                    on_weekday={(weekday) => pick_weekday(month, weekday)}
                    on_day_click={selection.on_day_click}
                    comments_of={(date) => comments_of_day.get(date) ?? NO_COMMENTS}
                    closure_of={closure_of_day}
                    on_comments={() => open_comment({ tab: 'period', range: null })}
```

### 4.7 widget 말단 — `OperationMonthCalendar` (`src/widgets/schedule/operation/OperationMonthCalendar.tsx:33-130`)

```tsx
/**
 * 슬롯 운영 **상세 달력 한 장** — 외래 진료일정 · 자원 슬롯 메뉴 공통. (시안 `schedule-slot-*.html` `renderMonths`)
 *
 * 킷 `MonthView` 를 쓰지 않는다 — **요일 머리글을 눌러 그 요일 전부를 고르는 것**이 이 화면의 핵심 조작인데
 * `MonthView` 는 머리글을 열어 두지 않았다(`design-system-gaps.md` §3-2). 그래서 격자는 CSS(`.op-*`)로 짜고,
 * 조작은 전부 킷 `Button`(머리글 · 날짜 숫자 — 키보드가 여기로 들어온다)과 래퍼의 포인터(`useRangeSelection`)로 받는다.
 * ...
 */
export function OperationMonthCalendar({
  month,
  days,
  selected,
  today,
  head_start,
  head_end,
  on_weekday,
  on_day_click,
  terms,
  cell_detail,
  comments_of,
  on_comments,
  closure_of,
}: {
  month: MonthKey
  days: ReadonlyMap<IsoDate, OperationDay>
  selected: ReadonlySet<IsoDate>
  today: IsoDate
  /** 머리 왼쪽 슬롯(이전 달). */
  head_start?: ReactNode
  /** 머리 오른쪽 슬롯(이번 달로 · 다음 달). */
  head_end?: ReactNode
  /** 요일 머리글 — 그 달의 그 요일 전체를 켜고 끈다. */
  on_weekday: (weekday: number) => void
  on_day_click: (date: IsoDate) => void
  /** 메뉴의 업무 말 — 「진료 12일」 · 「운영 12일」, 「진료불가 3」 · 「휴무 3」. */
  terms: OperationTerms
  cell_detail: DayCellDetail
  /** 그 날에 걸린 기간 비고 — 없으면 표시를 그리지 않는다. */
  comments_of?: (date: IsoDate) => readonly PeriodCommentLike[]
  /** 비고 표시를 누르면 — 비고 팝업을 「기간 비고」 탭으로 연다. */
  on_comments?: () => void
  /** 그 날의 마감 — 덮인 세션 · 비고. 없으면 null(표지를 그리지 않는다). */
  closure_of?: (date: IsoDate) => { session: ClosureSession; note: string } | null
}) {
  const [year, index] = month.split('-').map(Number)
  const first = new Date(year ?? 1970, (index ?? 1) - 1, 1)
  const in_month = dates_of_month(month)
  const offset = first.getDay()
  const rows = Math.ceil((offset + in_month.length) / 7)

  let open_days = 0
  let slots = 0
  let booked = 0
  let blocked = 0
  const weekday_open: number[] = [0, 0, 0, 0, 0, 0, 0]
  for (const date of in_month) {
    const day = days.get(date)
    if (!day) continue
    if (day.blocked) blocked += 1
    if (day.generated) {
      open_days += 1
      slots += day.slot_count
      booked += day.booked
      weekday_open[day.weekday] = (weekday_open[day.weekday] ?? 0) + 1
    }
  }
  ...
  return (
    <div className="op-month">
      <Stack
        direction="row"
        gap={6}
        align="center"
        wrap
        className="shrink-0 border-b border-(--color-border-default) bg-(--color-surface-default) px-(--spacing-8) py-(--spacing-6)"
      >
        {head_start}
        <span className="typo-caption-emphasis text-(--color-text-primary)">{month_label(month)}</span>
        {open_days > 0 ? (
          <>
            <Badge variant="soft" color="gray" size="sm">{`${terms.open_day} ${open_days}일`}</Badge>
            <Badge variant="soft" color="gray" size="sm">{`${slots}슬롯`}</Badge>
```

### 4.8 view — `ScheduleSlotScreen` (`src/views/schedule/ScheduleSlotScreen.tsx:63-72`, `78-145`)

```tsx
export default function ScheduleSlotScreen({ menu: menu_key }: { menu: ScheduleMenuKey }) {
  // 화면이 닫히면 store 를 비운다 — 다시 열면 서버에서 새로 읽는다(이전에 읽은 값을 쓰지 않는다).
  useScheduleStoreLifetime()
  const menu = schedule_menu(menu_key)
  return menu.category.slot_aside === 'physician-table' ? (
    <PhysicianSlotBoard menu={menu} />
  ) : (
    <TargetSlotBoard menu={menu} />
  )
}

/**
 * 좌측이 **대상 목록**인 운영 면 — 검사실 · 주사실 · 투석실 · 재활치료사 · 교육장. 목록 줄은 반영된 형상 한 줄이다.
 * 제목 줄 오른쪽에 「슬롯 관리」 스위치와 [오버부킹](켜진 방만)이 선다(`SlotModeControls` · classdiagram 1.33).
 */
function TargetSlotBoard({ menu }: { menu: ScheduleMenu }) {
  const category = menu.category
  const shell = useScheduleShell(menu)
  const profile = useMemo(() => operation_profile(category), [category])
  const target = shell.target
  const editor = useMemo(() => (target ? profile.block_editor(target) : null), [profile, target])
  const today = to_iso_date(new Date())

  // 대상 · 진료과가 없어도 화면은 띄운다 — 원천 조회 실패는 셸이 토스트로 알린다(정의 면과 같다).

  return (
    // 제목 줄을 두지 않는다 — host breadcrumb·메뉴가 이미 화면 이름을 가리킨다.
    <ScreenShell density="compact">
      <div className="schedule-split slot-screen">
        <div className="schedule-grid">
          <div className="schedule-aside">
            <ShellTargetList
              menu={menu}
              shell={shell}
              // 대상이 **지금 무엇으로 돌고 있는가** 한 줄(치료사는 직종 · 사번이 먼저).
              render_target_detail={(row) => (
                <TargetAppliedSummary
                  target_id={row.id}
                  today={today}
                  prefix={shell.subtitle_of.get(row.id)}
                />
              )}
              // 외부 가용 상태(장비 고장 · 휴가 …) — 원천 밖 정보라 보이기만 한다(classdiagram 1.11).
              render_target_status={(key, name) => (
                <NotAvailableBadge
                  items={shell.not_available_of.get(key) ?? []}
                  target_name={name}
                />
              )}
            />
          </div>

          <Stack direction="column" gap={8} className="min-h-0 min-w-0">
            <NarrowPicker menu={menu} shell={shell} />
            <Stack.Item grow className="min-h-0 min-w-0">
              {target == null || editor == null ? (
                <EmptyState
                  size="md"
                  title="예약 기준이 있는 대상이 없습니다"
                  description={`[${category.pattern_title}]에서 패턴을 만들면 여기서 기간에 걸어 슬롯을 풉니다.`}
                />
              ) : (
                // `key` 는 대상·메뉴가 바뀔 때 **리마운트**를 강제한다 — 이전 대상의 선택 · 이력이 남지 않게.
                <OperationWorkspace
                  key={`${menu.key}:${target.id}`}
                  target={target}
                  service_category={category.service_category}
                  terms={profile.terms}
                  editor={editor}
                  cell_detail={profile.cell_detail}
                  missing_pattern_hint={profile.missing_pattern_hint}
                  // 슬롯 관리 · 오버부킹(classdiagram 1.33) — 구분과 상관없이 대상 목록 운영 면이 모두 같다.
                  toolbar_end={
                    <SlotModeControls schedule_id={target.id} target_name={target.name} />
                  }
                />
              )}
            </Stack.Item>
          </Stack>
        </div>
      </div>
    </ScreenShell>
  )
```


---

## 5. 쓰기(mutation) 샘플 — 외래 예약 대화상자의 예약 등록 한 건

> 주의: 이 레포의 예약 등록은 `POST …/{id}/book` 이 아니라 **`POST /api/v1/patient-appointment/appointments/book-bundle`**(`bookBundle`) 한 번이다(열 때 `POST /appointments/draft` 로 초안(`draft:…` id)을 받고, 저장 때 그 초안 줄을 `book-bundle` 에 싣는다). 쓰기는 `useMutation` 이 아니라 **widget 안의 `async` 함수가 api 를 직접 await** 하고, `finally` 에서 키 줄기 전체를 invalidate 한다.

체인: `[저장]` 버튼 `onClick={request_save}` → 수정 모드면 `SaveReasonDialog`(사유 폼) → `save()` / `save_edit(change)` → `bookBundle(plan, ext, cancel, change)` → `on_result(to_outpatient_result(...))` + `toast` → `finally { invalidate() }`(`useInvalidateOutpatient` → `useInvalidateRoot('outpatient-booking')`). 대화상자는 스스로 닫지 않는다 — 호출 화면이 `open` 을 내린다(계약 `on_result` 주석).

### 5.1 props 계약 (`src/entities/appointment/model/outpatient_booking_contract.ts:153-161`, 진입 `51-90` 요약)

```ts
/** MF 노출 컴포넌트(`patient_appointment/OutpatientAppointmentDialog`)의 props. */
export interface OutpatientAppointmentDialogProps {
  open: boolean
  request: OutpatientBookingRequest
  /** 저장이 끝나면(저장여부 `N` 은 [선택]하면) 한 번 부른다. 대화상자는 닫지 않는다 — 호출 화면이 `open` 을 내린다. */
  on_result: (result: OutpatientBookingResult) => void
  /** 저장 없이 닫을 때. */
  on_close: () => void
}
```

`OutpatientBookingRequest`(`:51-90`)의 핵심 필드: `patient_id` · `patient`(환자 표시값 — 세션을 읽지 않는다) · `caller` · `department_id?` · `physician_id?` · `date?` · `timetable_view?` · `appointment_id?`(수정 모드) · `encounter_id?` · `booking_route?` · `save?: 'Y' | 'N'`.

### 5.2 열 때마다 새 상태 — `key` 로 리마운트 (`src/widgets/appointment/OutpatientAppointmentDialog.tsx:91-121`)

```tsx
export function OutpatientAppointmentDialog(props: OutpatientAppointmentDialogProps) {
  // 열 때마다 새 상태 — 지난 열림의 선택을 끌고 오지 않는다(닫히면 상태째 내린다).
  if (!props.open) return null
  return <OutpatientDialog key={request_key(props.request)} {...props} />
}

const request_key = (request: OutpatientBookingRequest) =>
  [
    request.patient_id,
    request.department_id,
    request.physician_id,
    request.date,
    request.timetable_view,
    request.appointment_id,
    request.encounter_id,
    request.save,
  ].join('|')

function OutpatientDialog({ request, on_result, on_close }: OutpatientAppointmentDialogProps) {
  const { toast } = useToast()
  /** 요청에 실린 환자 — 없으면 본문 대신 오류 띠(MF 호출 화면은 타입 검사를 거치지 않을 수 있다). */
  const patient = has_outpatient_patient(request) ? request.patient : null
  const setup = useOutpatientSetup(request)
  const invalidate = useInvalidateOutpatient()
  // 투약 종료(처방 BC) — 예약 참고 줄. 칸 · 추천에는 쓰지 않는다(읽어서 보이기만).
  const medications = useMedicationSchedules(request)
  const today = to_iso_date(new Date())
  const view = request.timetable_view ?? 'inline'
  /** 저장여부 N — 저장하지 않고 고른 결과만 돌려준다(원무가 접수를 만든 뒤 예약에 알린다). */
  const deferred = request.save === 'N'
```

### 5.3 폼 상태 — 「요청 초기값 + 바꾼 것(`undefined` = 초기값)」 (`OutpatientAppointmentDialog.tsx:139-152`, 설계 주석 `:83-84`)

```tsx
  const [dept_edit, set_dept_edit] = useState<string | null | undefined>(undefined)
  const [chosen_edit, set_chosen_edit] = useState<ReadonlySet<string> | undefined>(undefined)
  const [wish_edit, set_wish_edit] = useState<IsoDate | null | undefined>(undefined)
  const [selected_edit, set_selected_edit] = useState<IsoDate | null | undefined>(undefined)
  ...
  const [pick_edit, set_pick_edit] = useState<OutpatientPick | null | undefined>(undefined)
  const [session, set_session] = useState<ChipSession>('all')
  const [shown, set_shown] = useState<ReadonlySet<EventKind>>(() => new Set(EVENT_KINDS))
  ...
  const [reason_open, set_reason_open] = useState(false)
  const [auto_open, set_auto_open] = useState(false)
  const [saving, set_saving] = useState(false)
```

> `:83-84` 주석: "화면 상태는 「요청 초기값 + 사용자가 바꾼 것」이다 — 바꾼 값만 상태로 들고(`undefined` = 초기값) 나머지는 요청과 서버 목록에서 파생한다. 그래서 목록이 늦게 와도 초기값을 다시 잡는 effect 가 없고, [초기화]는 바꾼 값을 비우는 것이다."

### 5.4 폼 검증 — 파생 불리언으로 버튼을 잠근다 (`OutpatientAppointmentDialog.tsx:223`, `258-271`)

```tsx
  const ready = setup.enabled && !setup.pending && setup.error == null && setup.appointment != null
  ...
  const es = edit_state(original, pick)
  const locked = edit_target != null && is_locked(edit_target)
  const changed =
    dept_edit !== undefined ||
  ...
  const can_save =
    ready &&
    !saving &&
    (es == null
      ? pick != null && pick_doctor != null
      : !locked && es !== 'same' && (es === 'cancel' || pick_doctor != null))
```

칸 선택 시 시각 겹침 검사는 토스트(`:293-307`):

```tsx
  const choose_slot = (slot: SlotView) => {
    if (pick?.slot_id === slot.slot_id) {
      set_pick_edit(null)
      return
    }
    const clash = clash_of(other_events, slot)
    if (clash) {
      toast(
        `${short_date_label(slot.slot_date)} ${time_of_instant(slot.start_at)} 는 ${clash.name}(${clash.time}) 와 시각이 겹칩니다 — 다른 칸을 고르세요`,
        { variant: 'warning' },
      )
      return
    }
    set_pick_edit(to_outpatient_pick(slot))
  }
```

### 5.5 버튼 onClick → `request_save` (`OutpatientAppointmentDialog.tsx:338-343`, 푸터 `696-716`)

```tsx
  // 수정 모드 — 변경 · 취소는 사유를 먼저 받는다(검사 예약과 같은 사유 창).
  const request_save = () => {
    if (!can_save) return
    if (es != null) set_reason_open(true)
    else void save()
  }
```

```tsx
          <Button variant="gray" size="md" disabled={!changed} onClick={reset}>
            초기화
          </Button>
          <Button
            variant="secondary"
            size="md"
            disabled={!ready || active.length === 0}
            onClick={() => set_auto_open(true)}
          >
            자동선택
          </Button>
          <Button
            variant="primary"
            size="md"
            disabled={!can_save}
            loading={saving}
            onClick={request_save}
          >
            {deferred ? '선택' : '저장'}
          </Button>
```

### 5.6 `save()` — 신규 등록: `bookBundle` 호출 → 결과 → 토스트 → `on_result` → invalidate (`OutpatientAppointmentDialog.tsx:429-491`)

```tsx
  const save = async () => {
    if (!can_save || !pick || !pick_doctor || !setup.appointment) return
    const appointment_id = setup.appointment.appointment_id
    if (deferred) {
      // 저장하지 않는다 — 예약 행이 없어 appointment_id 는 null. 원무가 접수를 만든 뒤 encounter_id 를 더해 BOOK 으로 보낸다(classdiagram 1.28).
      toast(
        `외래 예약 선택 — ${short_date_label(pick.date)} ${time_of_instant(pick.start_at)} ${pick_doctor.name}`,
        { variant: 'success' },
      )
      on_result(
        to_outpatient_result(
          { appointment_id: null, status: 'proposed', start_at: pick.start_at, slot_id: pick.slot_id, encounter_id: null },
          pick,
          pick_doctor,
          'booked',
          {
            event_type: 'BOOK',
            patient_id: request.patient_id,
            // 예약 스냅샷의 원천 — 원무가 그대로 되돌려 보내고 서버가 그 값으로 예약을 만든다(shared 를 읽지 않는다).
            ...encounter_booking_patient(patient),
            appointment_ids: [],
            slot_id: pick.slot_id,
            booking_route: request.booking_route ?? null,
            preferred_date: request.date ?? null,
            visit_type: patient?.visit_type ?? null,
            change_reason: null,
            change_note: null,
          },
        ),
      )
      return
    }
    set_saving(true)
    try {
      // 예약 등록 — 초안 줄 하나를 `book-bundle` 로 보낸다. 서버가 같은 트랜잭션에서 예약 행을 만들고 등록한다(classdiagram 1.21).
      const saved = await bookBundle(
        [
          {
            appointmentId: appointment_id,
            slotId: pick.slot_id,
            ...(setup.draft_input ? { draft: setup.draft_input } : {}),
          },
        ],
        link_booking_ext(request.booking_route),
      )
      const entry = saved.booked.find(
        (row) => (row.draft_id ?? row.appointment_id) === appointment_id,
      )
      if (!entry) throw new Error('예약 등록 결과를 받지 못했습니다')
      const result = { ...entry, encounter_id: entry.encounter_id ?? null }
      toast(
        `외래 예약 — ${short_date_label(pick.date)} ${time_of_instant(pick.start_at)} ${pick_doctor.name}`,
        { variant: 'success' },
      )
      on_result(to_outpatient_result(result, pick, pick_doctor))
    } catch (error) {
      // 그 사이 칸이 찼거나(422 SLOT_FULL) 같은 순간 저장(409)이면 아무 일도 없다 — 그 날을 다시 읽고 다시 고른다.
      toast((error as Error).message, { variant: 'error' })
    } finally {
      set_saving(false)
      void invalidate()
    }
  }
```

수정 모드(`save_edit(change)` `:345-427`)는 `bookBundle(es === 'move' ? [{ appointmentId, slotId }] : [], link_booking_ext(null), es === 'cancel' ? [{ appointmentId }] : [], change_input(change))` 로 이동·취소를 같은 엔드포인트에 싣는다.

### 5.7 사유 폼 — `SaveReasonDialog` 의 검증(폼 안 오류 문구) (`src/widgets/appointment/SaveReasonDialog.tsx:63-79`, `114-120`)

```tsx
  const [reason, set_reason] = useState<CancellationReasonCode>('pat')
  const [note, set_note] = useState('')
  const [tried, set_tried] = useState(false)
  // 입력 오류는 폼 안에 적는다(토스트 아님).
  const note_missing = reason === 'oth' && note.trim() === ''
  ...
  const confirm = () => {
    set_tried(true)
    if (note_missing) return
    on_confirm({ reason, note: note.trim() })
  }
```

```tsx
        <TextArea
          label="메모"
          value={note}
          maxLength={500}
          onChange={(event) => set_note(event.target.value)}
          {...(tried && note_missing ? { errorText: '기타는 메모를 적어 주세요' } : {})}
        />
```

### 5.8 요청 본문 조립 헬퍼 (`src/entities/appointment/model/booking_ext.ts:1-20`)

```ts
import type { BookingExt, CancellationReasonCode } from '@/shared/api/appointmentApi'

/**
 * 대화상자 저장(`book-bundle`)의 `ext` — 채널은 늘 `link`(호출 화면 안 대화상자)이고, 예약 경로는 요청이 넘긴 것만 싣는다
 * (변경 · 취소만 있는 저장은 경로 없이 `link` 하나).
 */
export function link_booking_ext(booking_route: BookingExt['bookingRoute'] | null): BookingExt {
  return {
    bookingChannel: 'link',
    ...(booking_route ? { bookingRoute: booking_route } : {}),
  }
}

/** 저장 사유 창의 값 → `book-bundle` 의 `change` — 메모는 적었을 때만 싣는다. */
export function change_input(change: { reason: CancellationReasonCode; note: string }): {
  changeReason: CancellationReasonCode
  changeNote?: string
} {
  return { changeReason: change.reason, ...(change.note ? { changeNote: change.note } : {}) }
}
```

### 5.9 api 함수 — `bookBundle` (`src/shared/api/appointmentApi.ts:2678-2690`)

```ts
export function bookBundle(
  plan: BundlePlanEntry[],
  /** `bookingRoute` 는 이 저장에서 **새로 예약 등록하는 줄**에만 남는다(이동 · 취소 줄은 등록 때 값 유지). */
  ext?: BookingExt,
  /** 같은 저장에서 취소할 예약 — [예약 초기화]로 비운 채 둔 예약된 처방(전체 성공 or 전체 실패에 함께 묶인다). */
  cancel: Array<{ appointmentId: string }> = [],
  /** 변경 · 취소 사유 — 한 저장에 하나. 취소가 있으면 필수, `oth` 면 메모 필수(api §6.6). */
  change?: { changeReason: CancellationReasonCode; changeNote?: string },
) {
  return request<BundleResultView>(() =>
    api.post(`${BASE}/appointments/book-bundle`, { plan, ext, cancel, ...change }),
  )
}
```

### 5.10 재조회(invalidate) (`src/entities/appointment/model/outpatient_booking_data.ts:315-320`, `src/entities/appointment/model/booking_keys.ts:1-17`)

```ts
/**
 * 저장 뒤 — 달력 · 칩 · 추천 · 환자 일정 · 초안을 새로 읽는다(초안은 부수효과가 없다).
 */
export function useInvalidateOutpatient() {
  return useInvalidateRoot(ROOT)
}
```

```ts
import { useQueryClient } from '@tanstack/react-query'
import { useCallback } from 'react'

/**
 * 조회 키 줄기(`[root, …]`) 하나를 통째로 무효화하는 훅 — 검사 예약(`'booking'`) · 외래 예약(`'outpatient-booking'`) 대화상자가
 * 저장 뒤 달력 · 칩 · 환자 일정 · 초안을 함께 새로 읽을 때 쓴다. 초안도 다시 읽는다 — 부수효과가 없어 다시 읽어도 행이 생기지 않는다.
 */
export function useInvalidateRoot(root: string) {
  const client = useQueryClient()
  return useCallback(
    () =>
      client.invalidateQueries({
        predicate: (query) => query.queryKey[0] === root,
      }),
    [client, root],
  )
}
```

(`useMutation` 을 쓰는 쓰기 예는 `views/clinic-hours/ConfigurationScreen.tsx` · `widgets/schedule/operation/PeriodCommentPanel.tsx` 등 16개 파일 — §3 표 참조.)


---

## 6. 전역 상태

> **사실 확인**: `package.json` 에 `zustand ^5.0.11` 이 있지만 **`src/` 어디에서도 `from 'zustand'` 를 import 하지 않는다**(`grep -rn "from 'zustand" src` 결과 0건, `getState()` 사용 0건). 이 레포의 라우트 밖 전역 상태는 두 가지다 — ① 슬롯 정의·운영·현황 화면이 공유하는 **모듈 스코프 store + `useSyncExternalStore`**(`entities/schedule/model/schedule_store.ts`, 1,206줄), ② **event-bus `replayLast` 보관값**인 환자 세션(§7). 그 밖의 화면 상태는 전부 `useState` 와 react-query 캐시(`gcTime 0`)다.

### 6.1 store 정의 — 스냅샷 · 구독 · 커밋 · 훅 (`src/entities/schedule/model/schedule_store.ts:1-20`, `88-132` 발췌, `151-211`)

```ts
/**
 * 슬롯 자원 · 패턴 **서버 상태** — 패턴 메뉴와 운영 메뉴가 같은 데이터를 본다.
 *
 * 두 메뉴가 같은 자원·패턴을 보므로 화면마다 `useState` 로 들고 있으면 메뉴를 옮기는 순간 서로 다른
 * 세계가 된다. 그래서 라우트 밖에 사는 store 를 둔다. **목업은 없다** — 자원과 패턴은 전부
 * `/schedules` · `/patterns` 에서 온다.
 * ...
 * ⚠ 훅 이름만 camelCase 다(`useScheduleData`) — dev 의 `react-refresh` 는 `use[A-Z]` 로 시작하는
 *   이름만 훅으로 인식한다. snake_case 로 두면 컴포넌트의 refresh 서명에서 빠져 훅 순서가 어긋나고
 *   `Should have a queue.` 로 화면이 죽는다.
 */
import { useEffect, useSyncExternalStore } from 'react'
```

```ts
interface ScheduleData {
  /** 지금 읽어 둔 목록의 예약 구분 — 같은 구분을 다시 열면 다시 읽지 않는다. */
  service_category: ServiceCategory | null
  status: 'idle' | 'loading' | 'ready' | 'error'
  error: string | null
  directory: ScheduleDirectory
  /** 자원 id → 그 자원의 패턴 목록(서버에서 읽은 것). */
  patterns: Record<string, SchedulePattern[]>
  /** 패턴 id → ETag. 저장은 `If-Match` 로 이 값을 실어 보낸다(불변식 5). */
  versions: Record<string, number>
  /** 패턴을 이미 읽은 자원 — 다시 눌러도 재조회하지 않는다. */
  loaded: Record<string, boolean>
  /** 좌측 목록에 **비활성 패턴까지** 보일 것인가. 기본은 활성만이다. */
  include_inactive: boolean
  /**
   * 아직 서버에 없는 패턴 — **[+] 는 여기 담기만 한다**(actor_id → 목록).
   * 하단 [저장]이 `POST /schedules/{id}/patterns` 로 만들고 나서야 `patterns` 로 옮겨간다.
   * 서버 목록을 다시 읽어도 **지워지지 않는다** — 편집 중인 것을 새로고침이 삼키면 안 된다.
   */
  pending: Record<string, SchedulePattern[]>
  draft: Record<string, SchedulePattern>
  /** 저장 중인 패턴 id — 버튼을 잠근다. */
  saving: Record<string, boolean>
  // 운영 — 반영·휴무·일자 조정 전부 서버에서 온다. 일자 조정은 저장본과 버퍼가 갈린다.
  /** 자원 id → 그 자원에 걸린 반영 구간. */
  assignments: Record<string, PatternAssignment[]>
  /** 자원 id → 그 자원 **전용** 휴무 규칙(전역은 [global_holiday_rules]). */
  actor_holiday_rules: Record<string, UnavailabilityRule[]>
  /** 전역(병원 공용) 휴무 — 서버가 마스터를 갖기 전까지는 빈 목록이다(api-gaps.md E3). */
  global_holiday_rules: UnavailabilityRule[]
  /** 운영 상태를 이미 읽은 자원 — 화면을 옮겨도 다시 읽지 않는다. */
  operation_loaded: Record<string, boolean>
  /**
   * **저장 안 된** 일자 조정 — 자원 id → 날짜 → 조정. 하단 [저장]이 날짜마다 `PUT` 으로 보내는 유일한 출구다.
   * 달력은 이 항목을 「조정 · 저장 안 됨」으로 적는다. 저장되면 [saved_overrides] 로 옮겨간다.
   */
  overrides: Record<string, Record<IsoDate, DayOverride>>
  /** 서버에 **저장된** 일자 조정 — 보고 있는 구간만큼 읽어 둔다(`GET …/day-overrides?from&to`). */
  saved_overrides: Record<string, Record<IsoDate, DayOverride>>
}
```

```ts
const EMPTY: ScheduleData = {
  service_category: null,
  status: 'idle',
  error: null,
  directory: EMPTY_DIRECTORY,
  patterns: {},
  versions: {},
  loaded: {},
  include_inactive: false,
  pending: {},
  draft: {},
  saving: {},
  assignments: {},
  actor_holiday_rules: {},
  global_holiday_rules: [],
  operation_loaded: {},
  overrides: {},
  saved_overrides: {},
}

let snapshot: ScheduleData = EMPTY
const listeners = new Set<() => void>()

function subscribe(listener: () => void): () => void {
  listeners.add(listener)
  return () => {
    listeners.delete(listener)
  }
}

/** 스냅샷은 **불변 교체**한다 — 제자리에서 고치면 `useSyncExternalStore` 가 변경을 못 본다. */
function commit(next: ScheduleData): void {
  snapshot = next
  for (const listener of listeners) listener()
}

const read = (): ScheduleData => snapshot

export function useScheduleData(): ScheduleData {
  return useSyncExternalStore(subscribe, read, read)
}

/**
 * store 를 비운 횟수 — 비우기 전에 떠난 조회의 응답이 새 화면의 store 를 덮지 않게, 적재는 시작할 때의 값과 같을 때만 반영한다.
 */
let generation = 0

/**
 * store 를 비운다 — **화면이 닫힐 때** 부른다([useScheduleStoreLifetime]). 다음에 화면을 열면 빈 상태에서 서버를 새로 읽는다 —
 * 한 번 읽은 목록 · 패턴 · 운영 상태를 다시 연 화면이 그대로 쓰지 않는다(조회는 늘 DB 최신 · `shared/lib/query_cache`).
 * 저장 안 한 편집본도 함께 버린다(옛 버전 위의 편집을 새로 읽은 버전으로 저장하면 낙관적 잠금이 무력해진다).
 */
export function reset_schedule_store(): void {
  generation += 1
  commit(EMPTY)
}

/** 이 store 를 쓰는 화면(정의 · 운영 · 현황)이 부른다 — 화면이 닫히면(언마운트) store 를 비운다. */
export function useScheduleStoreLifetime(): void {
  useEffect(() => reset_schedule_store, [])
}
```

### 6.2 적재 액션 — 세대(generation) 가드로 늦게 온 응답을 버린다 (`schedule_store.ts:232-261`)

```ts
export async function load_schedules(
  service_category: ServiceCategory,
  force = false,
): Promise<void> {
  if (snapshot.status === 'loading') return
  // `force` — 예약 기준을 방금 만든 뒤처럼 같은 구분을 다시 읽어야 할 때.
  if (!force && snapshot.service_category === service_category && snapshot.status === 'ready')
    return
  commit({ ...snapshot, service_category, status: 'loading', error: null })
  const started = generation
  try {
    const response = await findSchedules({ serviceCategory: service_category, active: true })
    if (started !== generation) return
    commit({
      ...snapshot,
      service_category,
      status: 'ready',
      error: null,
      directory: build_directory(response.items),
    })
  } catch (error: unknown) {
    if (started !== generation) return
    commit({
      ...snapshot,
      service_category,
      status: 'error',
      error: error_message(error, '요청에 실패했습니다'),
    })
  }
}
```

### 6.3 액션 묶음 — `schedule_actions`(zustand 의 액션 대신 모듈 객체) (`schedule_store.ts:535-570`)

```ts
export const schedule_actions = {
  // ── 패턴 메뉴 ────────────────────────────────────────────────────────────
  /**
   * 패턴 추가 — **서버에 만들지 않는다**. 화면 버퍼(`pending`)에 빈 패턴을 담고 [저장]에서 한 번에 만든다.
   * ...
   */
  add_pattern: (actor_id: string, target_name: string): SchedulePattern => {
    const taken = [
      ...(snapshot.patterns[actor_id] ?? []),
      ...(snapshot.pending[actor_id] ?? []),
    ].map((item) => item.name)
    const pattern: SchedulePattern = {
      id: `${PENDING_PREFIX}${(pending_seq += 1)}`,
      schedule_actor_id: actor_id,
      name: next_pattern_name(target_name, taken),
      active: true,
      cycle_type: 'weekly',
      cycle_length: 7,
      detail: [],
      rule: [],
      over_capacity: [],
    }
    commit({
      ...snapshot,
      pending: {
        ...snapshot.pending,
        [actor_id]: [...(snapshot.pending[actor_id] ?? []), pattern],
      },
      // 버퍼에 담자마자 「저장 안 됨」이다 — 편집과 같은 출구를 쓴다.
      draft: { ...snapshot.draft, [pattern.id]: pattern },
    })
```

### 6.4 구독(셀렉터 대신 구조 분해)과 `getState()` 대응물(모듈 변수 `snapshot` 직접 참조) — 사용부

- 구독: `src/widgets/schedule/shell/useScheduleShell.ts:46-64`

```ts
  const {
    status,
    error,
    directory: schedule_directory,
    patterns,
    pending,
    draft,
    include_inactive,
  } = useScheduleData()

  /** 고른 진료과들 — `null` 이면 아직 고르지 않은 것이라 목록의 첫 과를 편다. 단일 모드는 한 칸만 쓴다. */
  const [department_codes, set_department_codes] = useState<string[] | null>(null)
  const [target_id, set_target_id] = useState<string | null>(null)
  const [pattern_id, set_pattern_id] = useState<string | null>(null)
  const [creating, set_creating] = useState<string | null>(null)

  useEffect(() => {
    void load_schedules(service_category)
  }, [service_category])
```

- 액션 호출: `src/views/schedule/SchedulePatternScreen.tsx:60-65`, `:136` — `schedule_actions.discard_pattern(victim.id)` · `void schedule_actions.deactivate_pattern(...)` · `void schedule_actions.set_include_inactive(next === true)`.
- 비-React 코드에서 현재 값 읽기(`getState()` 역할): 액션 내부가 모듈 변수 `snapshot` 을 직접 읽는다(`schedule_store.ts:547-550` `snapshot.patterns[actor_id]`).

### 6.5 화면 닫힐 때 비우기 — 모든 슬롯 화면의 첫 줄 (`src/views/schedule/ScheduleSlotScreen.tsx:63-66`, `SchedulePatternScreen.tsx:43`)

```tsx
export default function ScheduleSlotScreen({ menu: menu_key }: { menu: ScheduleMenuKey }) {
  // 화면이 닫히면 store 를 비운다 — 다시 열면 서버에서 새로 읽는다(이전에 읽은 값을 쓰지 않는다).
  useScheduleStoreLifetime()
  const menu = schedule_menu(menu_key)
```

(`useScheduleStoreLifetime` = `useEffect(() => reset_schedule_store, [])` — 언마운트 시 `generation += 1; commit(EMPTY)`.)

---

## 7. event-bus

### 7.1 계약 선언(module augmentation) + 환자 세션 수신/발행 — `src/shared/patient-session/ancillaryPatientSession.ts`(x-ancillary-main 정본의 **복사본**, `:1-30`, `:68-77`, `:107-178`)

```ts
import { PATIENT_EVENT, publish, subscribe, useSubscribe, type Unsubscribe } from '@x-ehr/event-bus'
import { useState } from 'react'

/**
 * 환자 세션 계약 — **이 파일 하나가 전부다.**
 *
 * @remarks
 * host(`x-ehr-foundation`)에는 환자 세션 저장소가 없다. 플랫폼이 가진 수단은 **요청 이벤트 하나**
 * (`ehr:patient:switch-requested`)뿐이고 그마저 id 둘만 싣는다 — "지금 환자가 누구인가" 라는 **값**을
 * 들고 있는 주체가 없다. 그 자리를 이 remote 가 임시로 맡는다. 접두 `ancillary-` 가 그 임시라는 표시다.
 *
 * **다른 remote 에 붙이는 법** — 런타임은 이미 이어져 있다(`@x-ehr/event-bus` 가 shared singleton 이라
 * 모든 remote 가 같은 버스를 본다). 필요한 것은 타입 선언뿐인데 MF 빌드 격리상 이쪽 `declare module` 을
 * 저쪽이 못 본다 — **이 파일을 복사**해 그 remote 의 `shared/` 에 두고 거기서 import 한다.
 * ...
 * 정본: `x-ancillary-main/frontend/apps/ancillary-main/src/shared/patient-session/ancillaryPatientSession.ts`
 * — 이 파일은 그 복사본이다. 정본이 바뀌면 통째로 다시 복사한다(여기서 따로 고치지 않는다).
 */

/** 세션 값 브로드캐스트 — **소유자만 발행**한다. `replayLast` 로 보관돼 늦게 뜬 화면도 즉시 받는다. */
const ANCILLARY_PATIENT_SESSION = 'ehr:ancillary-patient:session'
```

```ts
/** 세션 이벤트 payload — 봉투로 감싼다. 값 자체를 payload 로 쓰면 "비움" 을 실을 수 없다. */
export interface AncillaryPatientSessionEvent {
  session: AncillaryPatientSession | null
}

declare module '@x-ehr/event-bus' {
  interface EhrEventMap {
    [ANCILLARY_PATIENT_SESSION]: AncillaryPatientSessionEvent
  }
}
```

```ts
export function requestAncillaryPatientSession(
  target: AncillaryPatientTarget,
  options: AncillaryPatientRequestOptions = {},
): Promise<boolean> {
  return publish(
    PATIENT_EVENT.SWITCH_REQUESTED,
    { patientId: target.patient_id, encounterId: target.encounter_id },
    { replayLast: true, action: () => options.onSwitched?.() },
  )
}

/**
 * ③ **세션을 확정해 내보낸다** — 소유자 전용(`ancillaryPatientSessionOwner`)이다.
 */
export function publishAncillaryPatientSession(session: AncillaryPatientSession | null): void {
  publish(ANCILLARY_PATIENT_SESSION, { session }, { replayLast: true })
}

export function subscribeAncillaryPatientSessionRequests(
  handler: (target: AncillaryPatientTarget) => void,
): Unsubscribe {
  return subscribe(
    PATIENT_EVENT.SWITCH_REQUESTED,
    (payload) => handler({ patient_id: payload.patientId, encounter_id: payload.encounterId }),
    { replayLast: true },
  )
}

/**
 * ① **세션을 읽는다** — 아무 remote 나 호출한다. 아직 아무도 환자를 올리지 않았으면 `null`.
 *
 * `replayLast` 라 나중에 마운트해도 현재 환자를 즉시 받는다. 보관값은 **메모리라 새로고침을 넘기지
 * 못한다** — 넘겨야 하는 화면은 자기 sessionStorage 에 따로 둔다.
 *
 * 첫 렌더부터 보관값을 쓴다 — `useSubscribe` 의 replay 는 마운트 effect 에서야 오므로, 초기값을 `null` 로 두면
 * 이미 환자가 있는데도 한 커밋 동안 "환자 없음" 이 그려지고 이어서 값이 바뀐다. ...
 */
export function useAncillaryPatientSession(): AncillaryPatientSession | null {
  const [session, setSession] = useState(readRetainedSession)
  useSubscribe(ANCILLARY_PATIENT_SESSION, (payload) => setSession(payload.session), {
    replayLast: true,
  })
  return session
}

/**
 * 보관된 세션을 **동기로** 읽는다 — 구독해서 replay 한 번만 받고 바로 해제한다.
 *
 * event-bus 에 보관값 조회 API 가 없어 이 모양이다. replay 는 `subscribe` 호출 스택에서 즉시 전달되므로
 * 해제 전에 값이 채워진다. 서버에서는 `subscribe` 가 no-op 이라 `null` 이다.
 */
function readRetainedSession(): AncillaryPatientSession | null {
  let current: AncillaryPatientSession | null = null
  const off = subscribe(
    ANCILLARY_PATIENT_SESSION,
    (payload) => {
      current = payload.session
    },
    { replayLast: true },
  )
  off()
  return current
}
```

### 7.2 세션 환자 어댑터 — 화면이 환자를 얻는 유일한 입구 `useSessionPatient` (`src/shared/patient-session/session_patient.ts:24-45`, `60-88`)

```ts
/** 화면이 쓰는 세션 환자 — 발행처 전문에서 이 remote 가 필요한 값만 옮긴다. */
export interface SessionPatient {
  patient_id: string
  patient_no: string
  patient_name: string
  /** M / F. */
  sex: string
  /** 만 나이 — 발행처가 확정한 값. */
  age: number
  /** 생년월일 `YYYY-MM-DD` — 모르면 null(항암 대상 환자 등록의 신원 스냅샷 · classdiagram 1.45). */
  birth_date: string | null
  encounter_id: string | null
  /** 슬롯 대상 축의 진료구분 — 발행처 수진구분에서 옮긴다. 모르면 null. */
  encounter_class: EncounterClassCode | null
  /** 슬롯 대상 축의 초재진 — 외래 초진 = `new`, 외래 재진 = `followup`, 그 밖은 null. */
  visit_type: VisitTypeCode | null
  /** 화면 표기 — 외래 / 입원. */
  class_label: string
  department_name: string | null
  physician_name: string | null
  diagnosis: string | null
}
```

```ts
/** 발행처 전문 → 세션 환자. **발행처가 바뀌면 여기만 고친다.** */
function to_session_patient(session: AncillaryPatientSession | null): SessionPatient | null {
  if (!session || !session.patient_id) return null
  return {
    patient_id: session.patient_id,
    patient_no: session.patient_no,
    patient_name: session.patient_name,
    sex: session.patient_sex,
    age: session.patient_age,
    birth_date: session.patient_birthdate || null,
    encounter_id: session.encounter_id || null,
    encounter_class: ENCOUNTER_CLASS_OF[session.encounter_class_code] ?? null,
    visit_type: VISIT_TYPE_OF[session.encounter_type] ?? null,
    class_label: session.encounter_class_name,
    department_name: session.encounter_department_name || null,
    physician_name: session.encounter_physician_name || null,
    diagnosis: session.encounter_diagnosis_name,
  }
}

/**
 * 현재 세션 환자. 아무도 환자를 올리지 않았거나 세션이 비워졌으면 `null`.
 *
 * 같은 환자의 수진만 바뀌어도 새 객체가 온다 — 환자 단위로 다시 그릴 화면은 `patient_id` 를 key 로 쓴다.
 */
export function useSessionPatient(): SessionPatient | null {
  const session = useAncillaryPatientSession()
  return useMemo(() => to_session_patient(session), [session])
}
```

### 7.3 dev 전용 세션 발행 대역 (`src/shared/patient-session/session_patient_stand_in.ts:54-61`)

```ts
/** 고른 수진의 세션 값을 소유자와 같은 경로로 읽어 올린다. `null` 이면 세션을 비운다. */
export async function publish_session_patient_stand_in(encounter_id: string | null): Promise<void> {
  if (encounter_id == null) {
    publishAncillaryPatientSession(null)
    return
  }
  publishAncillaryPatientSession(await findPatientContext(encounter_id))
}
```

`Standalone.tsx:160-200` 의 `SessionPatientMenu` 가 x-ancillary-main 환자리스트(`load_session_candidates`)에서 고른 수진으로 이 함수를 부른다.

### 7.4 `openView` / `useViewParams` — **이 레포에는 없다.** app-kit 사용은 `useAppLink().navigate`(host 메뉴 이동) 한 곳뿐 (`src/widgets/schedule/common/NotAvailableBadge.tsx:3`, `:15-41`)

```tsx
import { useAppLink } from '@x-ehr/app-kit'
...
/**
 * 좌측 대상 줄의 **외부 가용 상태** — 아이콘 + 짧은 글(「장비 고장 10/2~10/3」), 누르면 팝업.
 *
 * 원천(장비 관리 · 인사 근태)은 이 BC 밖이라 **보여 주기만** 한다 — 슬롯을 닫지 않는다. 팝업의 이동 버튼은 원천 화면의
 * host 메뉴(`detail_menu_id`)를 `useAppLink().navigate` 로 연다(standalone 은 대역이 없으면 아무 일도 없다).
 * ...
 */
export function NotAvailableBadge({ items, target_name, label: label_override, icon = true }: {...}) {
  const [open, set_open] = useState(false)
  const { navigate } = useAppLink()
```

### 7.5 cross-remote 대화상자(`remoteComponent`) 패턴 — 이 레포는 **제공자**다

이 remote 가 `remoteComponent` 를 **소비하는** 코드는 없다(`grep` 결과 `schedule_api_catalog.ts` 의 **문서용 샘플 문자열**뿐). 제공 측 구성:
- `rspack.config.js:21-25` `exposes` 3개 → `src/exposes/*.tsx`(§2.4) 가 `x-remote-root khds-root` 래퍼로 widget 을 내보낸다.
- 계약 파일(`entities/appointment/model/booking_contract.ts` · `outpatient_booking_contract.ts` · `entities/schedule/model/booking_closure_contract.ts`)은 **import 없는 단독 파일**이라 호출 레포가 통째로 복사한다.

호출 측 코드는 「조회 API 안내」 화면의 샘플로 레포 안에 문자열로 들어 있다 (`src/entities/schedule/model/schedule_api_catalog.ts:364-400`):

```tsx
// eslint-disable-next-line no-restricted-imports -- 외래 예약은 호출 화면 안의 팝업이다(patient-appointment 부록 P73)
import { remoteComponent } from '@x-ehr/app-kit'
import { Button } from '@x-ehr/design-system'
import { Suspense, useState } from 'react'
import type {
  OutpatientAppointmentDialogProps,
  OutpatientBookingPatient,
} from '@/shared/patient-appointment/outpatientBookingContract'

const OutpatientDialog = remoteComponent<OutpatientAppointmentDialogProps>(
  'patient_appointment',
  'OutpatientAppointmentDialog',
)

export function ReceptionBooking({
  patient_id,
  patient,
}: {
  patient_id: string
  patient: OutpatientBookingPatient
}) {
  const [open, set_open] = useState(false)
  const [appointment_id, set_appointment_id] = useState<string | null>(null)
  return (
    <>
      <Button onClick={() => set_open(true)}>{appointment_id ? '외래 예약 수정' : '외래 예약'}</Button>
      {open && (
        <Suspense fallback={null}>
          <OutpatientDialog
            open
            request={{
              patient_id,
              patient,
              caller: 'patient-visit/reception',
              booking_route: 'in-house',
              appointment_id, // 있으면 수정 모드
            }}
```


---

## 8. mock

### 8.1 mock 모드 전환 방식 — **런타임 토글이 없다.** `grep -rln "MOCK\|mock" src` 결과는 3개 파일뿐

| 파일 | 역할 |
|---|---|
| `src/app/Standalone.tsx:1`, `:45` | `createMockAuthContext()`(`@x-ehr/dev-mocks`) — standalone 전용 auth 컨텍스트. prod 빌드에서 이 파일 자체가 빠진다 |
| `src/mocks/appointment/booking_request_mock.ts` | 검사 예약 **호출측 하네스**(`appointment/booking`)가 대화상자에 넘길 처방 목록(`MOCK_BOOKING_ORDERS`) — API 응답 mock 이 아니라 **호출 화면이 넘겨 줄 입력값의 대역** |
| `src/views/appointment/AppointmentBooking.tsx:9`, `:26-33` | 위 상수를 import 해 체크박스 목록을 만든다 |

데이터(슬롯 · 패턴 · 예약)는 전부 실제 백엔드(`rspack.config.js` devServer proxy → `:5352` / `:5301`)에서 온다 — `schedule_store.ts:5-6` "**목업은 없다** — 자원과 패턴은 전부 `/schedules` · `/patterns` 에서 온다". 환자 세션도 지어낸 값이 아니라 x-ancillary-main 환자리스트의 실제 환자다(`session_patient_stand_in.ts:16-19`).

### 8.2 mock 파일 발췌 (`src/mocks/appointment/booking_request_mock.ts:1-45`)

```ts
import type { BookingOrder } from '@/entities/appointment/model/booking_contract'

/**
 * 호출측 하네스(`appointment/booking`)가 대화상자에 넘기는 **처방 여럿** — 시안 v5 의 세 처방과 같은 구성.
 *
 * 실제 호출 화면(영상검사 처방 관리 · x-ancillary-main 「검사 예약」 메뉴)은 자기 처방을 넘긴다. 여기 값은
 * standalone 에서 대화상자를 여는 대역이다 — 검사의 가능 검사실은 처방의 `candidate_actor_ids`(= x-ancillary-main 검사실 id = `schedule.actor_id`)뿐이다.
 * 그 검사실의 예약 기준이 없거나 목록이 비면 「가능 검사실 없음」으로 선다(부록 P64 확인용 — 유방촬영) — 검사실 예약 기준은 정의 면의 [+]로 먼저 만든다.
 * 주사 처방(항암주사)은 방을 넘기지 않는다 — 주사실 예약 기준 전체가 가능 대상이다(classdiagram 1.32 · 시안 N20 확인용).
 */
/**
 * 처방 맥락 대역 — 처방일 · 처방 진료과 id · 처방의 id 는 필수다(없으면 서버가 422 로 저장하지 않는다 · classdiagram 부록 P105).
 * uuid 꼴의 대역 값이라 접수 요청까지 나간다(실제 진료과 · 직원이 아니다).
 */
const MOCK_ORDER_CONTEXT = {
  ordered_date: '2026-10-01',
  department_id: '00000000-0000-4000-8000-0000000000d1',
  requester_id: '00000000-0000-4000-8000-0000000000a1',
}

export const MOCK_BOOKING_ORDERS: BookingOrder[] = [
  {
    based_on_type: 'diagnostic-imaging',
    ...MOCK_ORDER_CONTEXT,
    based_on_id: 'img-sr-7790',
    code: 'RCT003',
    display: '복부 CT (조영)',
    candidate_actor_ids: ['sa-ct1', 'sa-ct2'],
    duration_minutes: 20,
    preparation_notes: ['금식 6시간', 'Cr 확인 (조영제)'],
    // 전처치 코드(classdiagram 1.38) — 카드 「금식」 · [자동선택]이 오전 칸 · 같은 날 먼저 놓는다. 처방 희망일(1.37)은 처방마다 준다.
    preparation_codes: ['fasting'],
    preferred_date: '2026-11-04',
    priority: 'routine',
  },
  {
    based_on_type: 'diagnostic-imaging',
    ...MOCK_ORDER_CONTEXT,
    based_on_id: 'img-sr-7793',
    code: 'RCT010',
    display: '흉부 CT (비조영)',
    candidate_actor_ids: ['sa-ct1', 'sa-ct2'],
    duration_minutes: 15,
    priority: 'routine',
  },
```

### 8.3 하네스에서 쓰는 곳 (`src/views/appointment/AppointmentBooking.tsx:24-48`)

```tsx
export default function AppointmentBooking() {
  const patient = useSessionPatient()
  const [checked, set_checked] = useState<ReadonlySet<string>>(
    () => new Set(MOCK_BOOKING_ORDERS.map(order_key)),
  )
  const [request, set_request] = useState<BookingRequest | null>(null)
  const [result, set_result] = useState<BookingResult | null>(null)

  const orders = MOCK_BOOKING_ORDERS.filter((order) => checked.has(order_key(order)))
  const unbooked_orders = MOCK_BOOKING_ORDERS.filter((order) => !checked.has(order_key(order)))

  const open = () => {
    if (!patient || orders.length === 0) return
    set_result(null)
    set_request({
      patient_id: patient.patient_id,
      patient: booking_patient_of(patient),
      mode: 'book',
      caller: 'patient-appointment(하네스)',
      orders,
      unbooked_orders,
      // 원내 처방 화면을 흉내 낸다 — 새로 예약 등록하는 처방에 경로 「원내」가 남는다(classdiagram 1.19).
      booking_route: 'in-house',
    })
  }
```

---

## 9. 디자인 시스템 (`@x-ehr/design-system` 0.0.36)

사용 파일 수(`grep -rlE "\b<이름>\b" src`): `Button` 78 · `Badge` 60 · `Select` 34 · `Dialog` 28 · `EmptyState` 26 · `DataGrid` 17 · `Tooltip` 16 · `Checkbox` 15 · `Grid` 13 · `Banner` 9 · `Switch` 8 · `Tabs` 7 · `Card` 7 · `MonthView` 6 · `TextArea` 5 · `DatePicker` 5(래퍼 경유). `Stack` 은 거의 전 파일.

### 9.1 `DataGrid` — 예약 내역 표 (`src/widgets/appointment/search/AppointmentSearchTable.tsx:1`, `:209-229`)

```tsx
import { Badge, DataGrid, EmptyState, Tooltip, type ColumnDef } from '@x-ehr/design-system'
```

```tsx
  return (
    <DataGrid
      rows={rows}
      columns={columns}
      getRowId={(row) => row.appointment_id}
      rowHeight={40}
      className="typo-body"
      aria-label="예약 내역"
      onRowDoubleClick={({ row }) => on_open(row)}
      empty={
        loading ? null : (
          <EmptyState
            size="sm"
            title="조건에 맞는 예약이 없습니다"
            description="기간 · 분류 · 검색어를 바꿔 보세요."
          />
        )
      }
    />
  )
}
```

### 9.2 `Dialog` + 푸터 `Button` + `RadioGroup` + `TextArea` — 사유 창 (`src/widgets/appointment/SaveReasonDialog.tsx:82-122`, §5.7 에 전체 인용)

```tsx
    <Dialog
      open
      onClose={on_close}
      title="변경 · 취소 사유"
      description={`${summary}의 사유를 고르세요.`}
      style={{ maxWidth: 'min(440px, 92vw)', width: '92vw' }}
      footer={
        <Stack direction="row" gap={8} justify="end" className="w-full">
          <Button variant="gray" size="md" onClick={on_close} disabled={saving}>
            취소
          </Button>
          <Button variant="primary" size="md" onClick={confirm} loading={saving} disabled={saving}>
            저장
          </Button>
        </Stack>
      }
    >
```

### 9.3 `Select` + `TextField` (`src/views/schedule/BookingClosureHarness.tsx:43-59`)

```tsx
          <Select
            size="md"
            aria-label="의사"
            className="w-[260px]"
            placeholder="외래 예약 기준이 있는 의사"
            options={doctors}
            value={picked ?? doctors[0]?.value ?? ''}
            onValueChange={(value) => set_picked(String(value))}
          />
          <TextField
            size="md"
            aria-label="actor_id"
            placeholder="actor_id 직접 입력"
            className="w-[300px] shrink-0"
            value={typed}
            onChange={(event) => set_typed(event.target.value)}
          />
```

### 9.4 `DatePicker` — 직접 쓰지 않고 `ClickDatePicker` 래퍼만(lint 강제, §1.4) (`src/shared/ui/ClickDatePicker.tsx:10-32`, `35-57`)

```tsx
/**
 * **누를 때만 여는 날짜 입력**(킷 `DatePicker` · `DateRangePicker` 그대로 + 달력 열림만 이 remote 가 쥔다) — 날짜 칸은 전부 이것을 쓴다.
 * 킷 그대로면 생기는 일 둘을 막는다:
 * ① 킷은 **포커스만 받아도** 달력을 연다 — 킷 `Dialog` 가 열리며 첫 `input` 에 포커스를 주고, 다른 대화상자가 닫히며 포커스를 되돌리고,
 *    브라우저 창으로 돌아올 때도 포커스가 다시 와서 화면이 열리자마자 달력이 떠 있다. 포커스로 온 열기 요청만 무시한다(클릭 · ↓ 키 · 달력 버튼은 그대로 연다).
 * ② 달력은 `body` 에 `fixed` · `z-50` 으로 붙고 바깥 mousedown 에만 닫힌다 — 입력란이 스크롤로 가려지거나 화면이 숨겨져도(host 탭 전환)
 *    달력만 맨 위에 남는다. 연 입력란이 화면에서 완전히 사라지면 닫는다(`IntersectionObserver` — 조상 스크롤 박스의 잘림 · `display: none` 을 함께 본다).
 * `open` · `defaultOpen` 은 받지 않는다(열림은 이 래퍼가 쥔다) — `onOpenChange` 는 받아들인 변경만 그대로 알린다.
 */
export function ClickDatePicker({
  onOpenChange,
  ...props
}: Omit<DatePickerProps, 'open' | 'defaultOpen'>) {
  return <DatePicker {...props} {...useClickOpen(onOpenChange)} />
}

/** 기간 입력판 — 규칙은 {@link ClickDatePicker} 와 같다. */
export function ClickDateRangePicker({
  onOpenChange,
  ...props
}: Omit<DateRangePickerProps, 'open' | 'defaultOpen'>) {
  return <DateRangePicker {...props} {...useClickOpen(onOpenChange)} />
}

/** 두 킷 입력의 열림 이벤트는 같은 구성이다(`DateRangePickerOpenChangeEvent = DatePickerOpenChangeEvent`). */
function useClickOpen(on_open_change?: (open: boolean, event: DatePickerOpenChangeEvent) => void) {
  const [open, set_open] = useState(false)
  const [anchor, set_anchor] = useState<Element | null>(null)

  useEffect(() => {
    if (!open || !anchor || typeof IntersectionObserver === 'undefined') return
    const observer = new IntersectionObserver((entries) => {
      if (entries[entries.length - 1]?.isIntersecting === false) set_open(false)
    })
    observer.observe(anchor)
    return () => observer.disconnect()
  }, [open, anchor])

  return {
    open,
    onOpenChange: (next: boolean, event: DatePickerOpenChangeEvent) => {
      if (next && event?.type === 'focus') return
      if (next) set_anchor(anchor_of(event))
      set_open(next)
      on_open_change?.(next, event)
    },
  }
}
```

### 9.5 `Stack` / `Grid` 프리미티브 + semantic 토큰 class — 화면 셸과 요일 진료시간 표

`src/shared/ui/ScreenShell.tsx:15-60`:

```tsx
export function ScreenShell({
  title,
  description,
  actions,
  notice,
  density = 'default',
  children,
}: {...}) {
  const compact = density === 'compact'

  return (
    <Stack
      direction="column"
      gap={compact ? 8 : 12}
      className={`h-full min-h-0 ${compact ? 'p-(--spacing-8)' : 'p-(--spacing-16)'}`}
    >
      {title != null ? (
        <Header level="page" title={title} description={description} action={actions} />
      ) : (
        // 제목 없이 액션만 있는 화면 — `Header` 는 `title` 이 필수라 액션 줄만 따로 그린다.
        actions != null && (
          <Stack direction="row" gap={6} align="center" justify="end" wrap className="shrink-0">
            {actions}
          </Stack>
        )
      )}
      {notice}
      <Stack.Item grow className="min-h-0">
        {children}
      </Stack.Item>
    </Stack>
  )
}
```

`src/widgets/clinic-hours/WeekdayHoursTable.tsx:61-89`:

```tsx
      <Stack bordered radius={8} overflow="auto">
        <Stack direction="column" className="ch-hours min-w-[1080px]">
          <Grid columns={COLUMNS} columnGap={8} align="center" className="ch-hours-head flex-none">
            <span>요일</span>
            <span>휴진</span>
            <span className="ch-head-am">오전 진료</span>
            <span className="ch-head-lunch">점심</span>
            <span className="ch-head-pm">오후 진료</span>
            <span className="ch-head-night">야간 진료</span>
            ...
          </Grid>
          {hours.map((day, index) => (
            <Grid
              key={day.day_of_week}
              columns={COLUMNS}
              columnGap={8}
              align="center"
              className="ch-hours-row flex-none"
              data-closed={day.closed ? 'true' : undefined}
              data-day={day.day_of_week}
            >
              <span className="ch-day typo-subheading">{DAY_LABEL[day.day_of_week]}</span>
```

semantic 토큰 class 예(§4.7 `OperationMonthCalendar.tsx:117-125`): `className="shrink-0 border-b border-(--color-border-default) bg-(--color-surface-default) px-(--spacing-8) py-(--spacing-6)"` · `<span className="typo-caption-emphasis text-(--color-text-primary)">`. `Standalone.tsx:236` `typo-caption-emphasis text-(--color-text-secondary)`.

### 9.6 `styles/globals.css` — 머리 + 핵심 규칙 (`src/styles/globals.css:1-25`, 795줄)

```css
/* Remote — 디자인 SOT = @x-ehr/design-system. foundation `templates/remote` 형태다.
 * preflight(전역 리셋)는 뺀다 — host 가 소유하고, `@x-ehr/design-system/styles` 의 `.khds-root` 스코프 리셋이 대신 커버한다.
 * `/theme` 은 킷 토큰 + 네임스페이스 리셋이라 Tailwind 기본 팔레트 · 기본 스케일(`bg-blue-500` · `text-sm` · `p-4`)이 생기지 않는다 —
 * 값은 토큰 문법(`p-(--spacing-12)` · `text-(--color-text-primary)` · `typo-*`)으로만 쓴다. */
@layer theme, base, components, utilities;
@import "tailwindcss/theme.css" layer(theme);
@import "tailwindcss/utilities.css" layer(utilities);
@import "@x-ehr/design-system/theme";
@import "@x-ehr/design-system/styles";

@source "../";
/* 공유 UI 패키지(@x-ehr/app-kit — ToastProvider 등)의 클래스도 스캔 대상에 포함.
 * 분리 레포에선 워크스페이스 packages/ 가 없으므로 Nexus 설치본(node_modules)을 스캔한다
 * (foundation in-repo 시절 경로: ../../../../packages/app-kit/src). */
@source "../../../../node_modules/@x-ehr/app-kit";

/* remote 높이 체인 — routes 의 <RemoteScope> 래퍼(div.x-remote-root.khds-root)에 붙는다.
 * `display: contents` 로 **박스를 만들지 않아야** 화면 셸의 `h-full` 이 래퍼(auto)가 아니라
 * 그 위(host <main> / standalone #root)의 확정 높이를 잡는다. 박스를 만들면 높이가 auto 로 끊겨
 * 셸이 헤더 높이만큼만 남고 본문이 0 으로 접힌다.
 * 디자인시스템의 base 리셋은 `.khds-root *` 후손 선택자라 display:contents 여도 그대로 적용된다. */
.x-remote-root { display: contents; }

/* class 기반 다크모드 */
@custom-variant dark (&:where(.dark, .dark *));
```

이 파일의 나머지는 **킷에 없는 격자**만 CSS 로 짠 것이다 — `[검사실 세션 관리]` 격자 셀(`.exam-grid-cell` — 네이티브 button 리셋, `:33-57`), `[외래 진료세션 관리]`(`:135~`), 슬롯 운영 달력 `.op-*`, 검사 예약 대화상자 `.bk-*`(`:363~`), 외래 예약 `.ob-*`, 진료시간 `.ch-*` 등.

### 9.7 커스텀 달력(월 격자) — 킷 `MonthView` 를 쓰지 않는 이유와 CSS (`src/widgets/schedule/operation/OperationMonthCalendar.tsx:33-38` + `src/styles/globals.css:309-335`)

```
 * 킷 `MonthView` 를 쓰지 않는다 — **요일 머리글을 눌러 그 요일 전부를 고르는 것**이 이 화면의 핵심 조작인데
 * `MonthView` 는 머리글을 열어 두지 않았다(`design-system-gaps.md` §3-2). 그래서 격자는 CSS(`.op-*`)로 짜고,
 * 조작은 전부 킷 `Button`(머리글 · 날짜 숫자 — 키보드가 여기로 들어온다)과 래퍼의 포인터(`useRangeSelection`)로 받는다.
```

```css
/* 상세 2개월 — 달 두 장이 남는 높이를 나눠 갖는다. 2/4 단계부터 위아래로 놓는다(위 「화면 폭 단계」). */
.op-months { min-height: 0; }
.op-two { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: var(--spacing-12); height: 100%; min-height: 0; }
.op-month { display: flex; flex-direction: column; min-height: 0; min-width: 0; border: 1px solid var(--color-border-default); border-radius: var(--radius-8); overflow: hidden; }
.op-wdh { display: grid; grid-template-columns: repeat(7, minmax(0, 1fr)); border-bottom: 1px solid var(--color-border-subtle); background: var(--color-surface-subtle); }
.op-wdh > * { width: 100%; border-radius: 0; }
.op-wdh > * + * { border-inline-start: 1px solid var(--color-border-subtle); }
.op-weeks { display: grid; grid-auto-rows: minmax(0, 1fr); flex: 1; min-height: 0; }
.op-week { display: grid; grid-template-columns: repeat(7, minmax(0, 1fr)); border-top: 1px solid var(--color-border-subtle); min-height: 78px; }
.op-week:first-child { border-top: 0; }
.op-day {
  position: relative; display: flex; flex-direction: column; gap: 3px; min-width: 0; cursor: pointer;
  padding: var(--spacing-4); border-inline-start: 1px solid var(--color-border-subtle);
}
.op-day:first-child { border-inline-start: 0; }
.op-day-other { background: var(--color-surface-subtle); cursor: default; }
/* 닫힌 날은 회색 음영(`surface-disabled`) — 경고가 아니다. 붉은색이면 고장·오류로 읽힌다 — 닫힘은 「운영하지 않는 날」이다. */
.op-day-closed { background: var(--color-surface-disabled); }
.op-day-sel { background: var(--color-action-primary-selected); box-shadow: inset 0 0 0 1.5px var(--color-action-primary); }
.op-day-sel.op-day-blocked { background: var(--color-error-subtle); }
.op-day-sun .op-day-num, .op-day-blocked .op-day-num { color: var(--color-text-error); }
.op-day-today .op-day-num { background: var(--color-inverse-surface); color: var(--color-text-inverse); }
/* 날짜 숫자 — 킷 Button(ghost · sm) 의 높이만 공식 변수 `--button-h` 로 22px 에 맞춘다(시안 `.day .num` 22 · 모서리는 킷 sm 2). */
.op-day-num { align-self: flex-start; min-width: 22px; padding-inline: 2px; --button-h: 22px; }
.op-day-foot { position: absolute; inset-inline-end: 6px; bottom: 4px; }
.op-pm-bar { height: 3px; border-radius: var(--radius-1); margin-top: auto; background: var(--color-badge-blue-subtle); }
.op-pm-bar-full { background: var(--color-action-primary); }
```

폭 단계는 `@container slot-screen (max-width: 1179px)` · `(max-width: 719px)` 컨테이너 쿼리(`globals.css:337-361`)로 2열→1열 전환.

---

## 10. i18n

**사실 확인**: `package.json` 에 `@x-ehr/i18n` · `i18next` · `react-i18next` 가 있지만 **`src/` 에서 `useTranslation` · `t()` · `i18next` 를 import 하는 코드가 없다**(`grep -rln "useTranslation\|i18next\|react-i18next" src` → `Standalone.tsx` 1건, 그것도 주석 "단일 i18next 를 쓰고" 뿐). **`public/i18n/` 폴더도 없다**(`public/` 에는 `index.html` 만). 화면 문구는 전부 한국어 리터럴이다(예: `SaveReasonDialog.tsx:6-10` `REASONS` 라벨, `AppointmentSearchTable.tsx:221-223`). 루트 `package.json` 의 `"i18n:check": "... i18n-check.cjs --allow-empty"` 가 **빈 번역을 허용**한 상태.

날짜·시각 표기는 `shared/lib/datetime.ts` 가 ISO 문자열 자리 자르기로 처리하고, 로케일 포맷은 host 의 `@x-ehr/i18n` 으로 옮길 예정임을 주석으로 남겼다 (`src/shared/lib/datetime.ts:1-10`, `63-70`):

```ts
/**
 * 달력 좌표(월·날짜)와 서버 instant 사이의 변환만 담는 순수 유틸.
 *
 * 표기 규약은 디자인시스템 `MonthView` 계약과 같다 — **월은 `"2026-08"`, 날짜는 `"2026-08-13"`**(타임존 없음)이고,
 * 이벤트 시각만 `Date`(특정 순간)로 다룬다. `Date` 는 반드시 **로컬 생성자**로 만든다
 * (`new Date("2026-08-10")` 은 UTC 자정으로 파싱돼 UTC 서쪽 타임존에서 하루 어긋난 칸에 배치된다).
 *
 * 표시 포맷(날짜·시각 문자열)은 host 의 `@x-ehr/i18n` 이 들어오면 그쪽으로 옮긴다 —
 * 여기서는 서버가 준 ISO 문자열의 자리 자르기만 한다(로케일 규칙을 프런트가 만들지 않는다).
 */
```

```ts
/**
 * 서버 instant 의 **로컬 `HH:mm`**.
 *
 * ⚠ 같은 이유로 `slice(11, 16)` 금지 — UTC 시:분이 그대로 찍혀 09시가 00시로 보인다.
 * (표시 포맷 규칙이 필요해지면 host 의 `@x-ehr/i18n` 으로 옮긴다.)
 */
export function time_of_instant(instant: string): string {
  const at = new Date(instant)
```


---

## 11. 컴포넌트 트리 — 외래 예약 대화상자 (`widgets/appointment/OutpatientAppointmentDialog.tsx`, 913줄)

```
<OutpatientAppointmentDialogExpose>                       src/exposes/OutpatientAppointmentDialog.tsx:15   (MF expose 진입 · 호출 remote 가 remoteComponent 로 띄움)
└─ <div className="x-remote-root khds-root">
   └─ <OutpatientAppointmentDialog open request on_result on_close>   :91   — open 이 false 면 null
      └─ <OutpatientDialog key={request_key(request)} {...props}>     :109  — 열 때마다 새 상태(key 리마운트)
         │  훅: useToast · useOutpatientSetup(request) · useInvalidateOutpatient() · useMedicationSchedules(request)
         │      · useCategoryDepartments('outpatient-clinic', scheduled) · useMonthSlots(...) · usePeriodComments(...)
         │  상태: dept_edit · chosen_edit · wish_edit · selected_edit · pick_edit(undefined=초기값) · session('all')
         │       · shown(EventKind 집합) · focus · reason_open · auto_open · saving
         └─ <Dialog open onClose closeOnOverlayClick={false} className="ob-dialog"
                   style={{ width: 'min(800px, 96vw)', height: 'min(680px, 94vh)' }}>          :667-684
            ├─ title = <HeaderLine editing patient request departments schedules_doctor_name next />   :672-681
            ├─ body (:531-664) — 준비 상태에 따라 분기
            │   ├─ !patient → <Banner variant="error" …>                                      :533
            │   ├─ setup.pending → <Spinner size="md" aria-label="예약을 준비하는 중" />           :539
            │   ├─ setup.error → <Banner …>                                                   :542
            │   ├─ setup.blocked → <Banner variant="error" description={setup.blocked} />        :548
            │   └─ 정상:
            │       ├─ (locked) <Banner description="접수된 예약이라 바꾸거나 취소할 수 없습니다." />  :557-561
            │       ├─ <OutpatientFilterBar departments department_id doctors chosen summaries
            │       │        previous_doctor_id wish on_department on_chosen on_wish />        :563-580
            │       ├─ <div className="ob-main [ob-main-tt-open]">                               :581
            │       │   ├─ <OutpatientCalendar months max_month today selected wish pick_date
            │       │   │        summary_of events shown session on_session focus
            │       │   │        expansion={view==='inline' && chips ? <Card className="ob-tt-card">{chips}</Card> : null}
            │       │   │        head={<OutpatientRemarkNote patient_id medications target today department_id
            │       │   │                     department_name departments doctors chosen previous_doctor_id events … />} />   :584-650
            │       │   │   └─ chips = <OutpatientTimeChips view date summary doctors slots bookable_ids mine_ids pick
            │       │   │                     original_slot_id previous_doctor_id same_day session remarks_of loading
            │       │   │                     on_pick={choose_slot} on_close={close_timetable} />          :501-527
            │       │   └─ (view !== 'inline') <div className="ob-tt-dimm" onClick={close_timetable} /> + drawer/dialog 시간표   :641
            │       └─ <OutpatientAutoSelectDialog open={auto_open} patient_id target doctors previous_doctor_id
            │                wish today on_apply={apply_entry} on_close />                      :652-662
            ├─ footer = <Stack direction="row" gap={8} align="center" className="ob-foot">        :685-717
            │   ├─ <PickLine pick doctor dept state={es} original original_doctor on_clear />      :687-695
            │   ├─ <Button variant="gray" disabled={!changed} onClick={reset}>초기화</Button>       :696
            │   ├─ <Button variant="secondary" disabled={!ready || active.length===0} onClick={()=>set_auto_open(true)}>자동선택</Button>  :699
            │   └─ <Button variant="primary" disabled={!can_save} loading={saving} onClick={request_save}>{deferred ? '선택' : '저장'}</Button>  :707-715
            └─ <SaveReasonDialog open={reason_open} moved_count cancel_count saving
                     on_confirm={(change)=>void save_edit(change)} on_close />                :720-727
               └─ <Dialog title="변경 · 취소 사유"> <RadioGroup>(pat/prov/oth) + <TextArea label="메모" errorText?>   SaveReasonDialog.tsx:82-122
```

핵심 props 값: `request.save === 'N'` → `deferred`(버튼 글자 「선택」, 저장 안 함, `on_result.encounter_booking` 반환) · `request.appointment_id`/`encounter_id` → `setup.edit_mode`(수정 모드: 변경/취소 + 사유 창) · `timetable_view`(`inline`|`drawer`|`dialog`) → 시간표 위치.

---

## 12. 이 레포만의 특징 패턴

### 12.1 슬롯 정의 / 운영 2벌 화면 × 대상 구분 = 메뉴 프리셋
- 라우트는 `<면>/<대상 구분>` 리터럴 — 정의 6 · 운영 6 · 현황 1 · 현황판 2 · 일자별 1 (`routes.tsx:66-89`, `schedule_menu.ts:252-264`).
- 면(`schedule-pattern`/`schedule-slot`/…)과 구분(`examination`…`outpatient-clinic`)은 서버 `service_category` 어휘 그대로, 줄임말 금지 (`schedule_menu.ts:9-12`).
- 구분마다 다른 것(좌측 목록 vs 의사 비교표 · 편집기 종류 · 업무 말 · 기본 사유)은 **코드 분기가 아니라 프리셋 표의 열** — `SCHEDULE_CATEGORIES` (`schedule_menu.ts:89-216`, 예: 외래 `:195-215` `slot_aside: 'physician-table'` · `pattern_editor: 'outpatient'` · `block_label: '진료불가'`).
- 화면 컴포넌트는 `menu` prop 하나로 한 벌 (`ScheduleSlotScreen.tsx:63-72` — `slot_aside` 로 `PhysicianSlotBoard`/`TargetSlotBoard` 분기), `key={key}` 로 메뉴 전환 시 리마운트 (`routes.tsx:52-53`).
- 정의 · 운영 · 현황이 공유하는 서버 상태는 `schedule_store.ts` 모듈 store(§6), 언마운트에 비움.

### 12.2 세션 오전/오후(시간대별 · 하루) 표기
- 서버 `session_unit`(`'session' | 'day'`) · `session_band`(`'am' | 'pm' | 'night' | 'day'`) 코드 → 화면 글자는 `schedule_labels.ts` 한 곳 (`:63-72` `SESSION_BAND_LABEL` 오전/오후/야간/종일 · `SESSION_UNIT_LABEL` 시간대별/하루).
- 세션 한 칸 칸은 시각 대신 밴드 글자 — `slot_time_label(start_at, session_band)` (`schedule_labels.ts:78-83`); "시작 시각으로 다시 가르지 않는다".
- 운영 면 제목 줄 `SlotModeControls` 의 「시간대별 · 하루」 `SegmentedControl` 이 `PATCH /schedules/{id}`(`ext.sessionUnit`)로 즉시 저장 (`SlotModeControls.tsx:15-16`).
- 운영일 세션(종일/오전만/오후만)은 `day_operation.ts:34-41` `day_session` 이 블록 시작 시각(정오 경계)으로 판정 — 서버 `session_code.of` 와 같다는 주석(`:28`).
- 마감 세션(`booking_closure.ts:16-25`)도 `all/am/pm` → 종일/오전/오후.

### 12.3 외래 진료 현황판(전광판)
- 두 라우트(`schedule-dashboard-week/outpatient-clinic` · `schedule-dashboard-today/outpatient-clinic`)가 `DashboardScreenFrame` 하나를 공유 (`DashboardScreenFrame.tsx:1-3`).
- `GET /schedule-dashboard` 를 `refetchInterval: 5분` · `placeholderData: previous` 로 읽는다 (`DashboardScreenFrame.tsx:17-18`, `41-47`) — 사람이 새로고침하지 않는 판.
- 본문 높이를 재서 단 나누기 · 쪽 넘김(`useBoardPaging` 20초, `useToday` 30초 자정 감지 — `useBoardTimers.ts:8-32`).
- 스타일은 별도 `widgets/schedule/dashboard/dashboard.css`, 축척은 `useBoardScale`.

### 12.4 기간 비고(period comment)
- 슬롯 운영 달력 칸에 날짜 범위 비고를 겹쳐 보인다 — `usePeriodComments(ids, from, to)` → `GET /period-comments` (`period_comment.ts:82-103`), 달력은 `comments_of(date)` 로 받는다 (`OperationWorkspace.tsx:377-387`, `OperationMonthCalendar.tsx:74-77`).
- 추가/수정/삭제/이력은 `PeriodCommentPanel`(`useMutation` + `RichTextEditor` + `ClickDateRangePicker`, `PeriodCommentPanel.tsx:1-40`); 삭제는 `POST …/remove` 동작 조각(`appointmentApi.ts:547-550`), 수정은 `If-Match` 로 409 충돌 처리(`is_version_conflict`).
- 예약 대화상자 시간표 칩에도 같은 비고가 `remarks_of(schedule_id)` 로 들어간다 (`OutpatientAppointmentDialog.tsx:516-523`).

### 12.5 미예약 처방 카드(환자별 예약 스케줄)
- 다른 BC(x-ancillary-main)의 발행 처방을 `findIssuedOrders(patient_no)` 로 읽고(`useSessionOrders`, `session_orders.ts:59-61`) 이미 예약·시행된 것을 뺀 목록 (`UnbookedOrderCard.tsx:14-18`).
- 체크한 처방 = `orders`, 나머지 = `unbooked_orders` 로 `BookingRequest` 를 만들어 검사 예약 대화상자를 연다 (`PatientSchedule.tsx:7`, `:21`).
- 카드 골격은 `shared/ui/PanelCard`(킷 `Stack` 조립) + 킷 `Checkbox`·`Badge`·`Button`.

### 12.6 개인설정(preference)
- **이 레포에는 사용자 개인설정 SDK/저장이 없다.** `grep "preference"` 는 `booking_auto.ts:27` `AutoDayPreference = 'any' | 'avoid' | 'prefer'`(자동선택 옵션의 공휴일 전날 선호)뿐이다. 화면 상태는 리마운트마다 초기화된다(§5.2 `key` 리마운트, §6.5 store 비움).

### 12.7 그 밖의 반복 패턴
- **호출측 하네스 라우트**: 대화상자 3개(검사 예약 · 외래 예약 · 슬롯 마감)는 메뉴 화면이 아니라 MF expose 라서, standalone 확인용 「호출측 하네스」 라우트를 따로 둔다 (`AppointmentBooking.tsx:12-23`, `Standalone.tsx:69-74`).
- **계약 파일은 import 없는 단독 파일** — 호출 레포가 통째로 복사 (`outpatient_booking_contract.ts:5-6`).
- **If-Match 낙관 락**: 수정 API 대부분이 `version` 을 `If-Match` 로 싣고(`appointmentApi.ts:287-289`), 화면은 `is_version_conflict` 로 409 를 「다시 읽기」로 처리.
- **초안(draft) · 대역(stand-in) id 접두**: `draft:`(`appointmentApi.ts:2436`) · `pending:`(`schedule.ts:101`) — 서버에 없는 객체를 같은 타입으로 다루되 요청을 막는다(`operation_days.ts:27-30`).
- **「조회 API 안내」 화면**: 다른 시스템 개발자용 명세 + 시험 호출을 레포 안 데이터(`schedule_api_catalog.ts`, 2,747줄)로 둔다.

---

## 13. 신입이 헷갈릴 포인트 10개

1. **`features/` 레이어가 없다** — CLAUDE.md 의 FSD 5계층 중 `features` 가 비어 있고, 훅·store 는 `entities/*/model`(`booking_data.ts` · `schedule_store.ts`), UI 조합은 `widgets` 에 있다. `entities/*/api/` 도 없고 HTTP 는 `shared/api/appointmentApi.ts` 한 파일(2,949줄)이다 (§1.1, `find src -type f`).
2. **`zustand` 는 설치돼 있지만 쓰지 않는다** — 전역 store 는 `useSyncExternalStore` + 모듈 변수 `snapshot` (`schedule_store.ts:171-191`). `getState()` 대신 액션이 `snapshot` 을 직접 읽는다.
3. **`useQuery` 를 직접 import 하면 lint 에러** — `useFreshQuery`/`useFreshQueries` 만 허용(`frontend/eslint.config.mjs:16-27`, `55-66`); host QueryClient 의 `staleTime 5분` 을 `staleTime 0 · gcTime 0` 으로 덮는다 (`query_cache.ts:22`). 마찬가지로 `DatePicker` 직접 사용 금지 → `ClickDatePicker` (`eslint.config.mjs:30-36`).
4. **표기 규약이 방향마다 다르다** — 응답은 snake_case 그대로, 요청 본문은 FHIR camelCase(`slotId` · `cycleType`), 코드값은 소문자-하이픈(`busy-unavailable` · `central-open`) — 대문자 enum 과 비교하면 조용히 어긋난다 (`appointmentApi.ts:6-13`).
5. **훅 이름만 camelCase** — 전 영역 snake_case 지만 훅은 `use[A-Z]` 여야 react-refresh 가 훅으로 인식한다; snake 로 두면 `Should have a queue.` 로 죽는다 (`schedule_store.ts:16-18`).
6. **예약 등록은 `POST /appointments/book-bundle` 한 번** — 열 때 `POST /appointments/draft` 로 `draft:…` 초안을 받고, 저장 때 초안 줄을 `plan` 에 실어 보낸다. 이동·취소도 같은 엔드포인트의 `plan`/`cancel`/`change` 조각이다 (`OutpatientAppointmentDialog.tsx:464-473`, `:392-397`; `is_draft_id` `appointmentApi.ts:2436-2440`).
7. **대화상자는 스스로 닫지 않는다** — `on_result` 를 부를 뿐, `open` 을 내리는 것은 호출 화면이다 (`outpatient_booking_contract.ts:157`). 또 `request.save === 'N'` 이면 저장하지 않고 `encounter_booking` 만 돌려준다 (`OutpatientAppointmentDialog.tsx:432-460`).
8. **`:param` 라우트 금지, 리터럴 경로 + `key` 리마운트** — `schedule-slot/:category` 로 두면 host 메뉴가 리터럴만 등록해 「접근 권한이 없습니다」가 나고, `key` 를 빼면 메뉴 전환 시 이전 대상의 편집 상태가 남는다 (`routes.tsx:50-53`).
9. **`.x-remote-root` 는 `display: contents`** — 박스를 만들지 않아야 `ScreenShell` 의 `h-full` 이 host `<main>` 높이를 잡는다; standalone 은 `DevShell` 의 pane(`flex: 1 1 auto; minHeight: 0`)이 그 높이를 세운다 (`globals.css:17-22`, `Standalone.tsx:208-212`, `:248`). 그리고 `khds-root` 를 빼면 킷 리셋이 안 닿아 컴포넌트가 브라우저 기본 모양으로 풀린다 (`routes.tsx:23-25`).
10. **Tailwind 기본 스케일이 없다** — `p-4` · `text-sm` 은 조용히 무시되고 `p-(--spacing-12)` · `typo-*` 토큰 문법만 먹는다 (`globals.css:3-4`). `font: inherit` 단축속성도 `typo-*` 를 무력화한다 (`globals.css:35-36`).

(보너스) **환자 세션은 event-bus `replayLast` 보관값이고 메모리라 새로고침을 넘기지 못한다** (`ancillaryPatientSession.ts:146-147`); 외래·검사 예약 대화상자는 세션을 읽지 않고 **계약 파라미터(`request.patient_id` + `request.patient`)로만** 환자를 받는다 (`OutpatientAppointmentDialog.tsx:88-89`). **standalone 의 API 는 401 이다** — devServer auth 스텁은 더미 토큰이라 데이터까지 보려면 host 경유(`./dev-up`)로 띄운다 (`rspack.config.js:55-56`). **i18n 은 배선만 있고 번역 파일·`t()` 사용이 없다** (§10).
