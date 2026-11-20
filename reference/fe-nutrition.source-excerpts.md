# x-nutrition frontend (`apps/nutrition`) — 신입 교육용 소스 수집

- 레포 루트: `x-nutrition/frontend/apps/nutrition`
- 아래 인용의 경로는 모두 이 루트 기준 상대경로다(`src/...`). `경로:시작줄-끝줄` 은 실제 파일의 줄 번호.
- 수집 기준일: 2026-10-29. 코드는 실제 소스를 그대로 인용했다(각색 없음).

> **먼저 알아둘 것** — 이 remote 는 CLAUDE.md 가 말하는 FSD 5레이어(views→widgets→features→entities→shared) 중
> **`views` 와 `shared` 두 레이어만** 쓴다. `widgets/` · `features/` · `entities/` · `mocks/` 폴더가 없고,
> zustand 스토어 · `@x-ehr/event-bus` · i18n `t()` 호출 · mock 모드 전환도 **소스에 없다**(각 절에 grep 근거).
> 화면(view)이 `shared/api/foodserviceApi.ts` 의 함수를 직접 부르고, 상태는 react-query + 컴포넌트 `useState` 로만 든다.

---

## 1. 폴더 트리 · 설정

### 1.1 `find src -type f | sort` — 레이어별 정리 (총 42 파일)

| 레이어 | 파일 수 | 파일 |
|---|---|---|
| 루트(진입) | 4 | `src/index.ts` · `src/bootstrap.tsx` · `src/routes.tsx` · `src/global.d.ts` |
| `app/` (dev 전용) | 1 | `src/app/Standalone.tsx` |
| `views/` | 25 | 아래 화면별 표 |
| `widgets/` | **0** | 폴더 없음 |
| `features/` | **0** | 폴더 없음 |
| `entities/` | **0** | 폴더 없음 |
| `shared/api` | 1 | `src/shared/api/foodserviceApi.ts` (1298줄 — 타입 + API 함수 전부) |
| `shared/lib` | 3 | `src/shared/lib/dietAttributes.ts` · `src/shared/lib/patient.ts` · `src/shared/lib/query_cache.ts` |
| `shared/ui` | 7 | `src/shared/ui/PanelBar.tsx` · `RestoreDialog.tsx` · `ScreenToolbar.tsx` · `codeLabels.ts` · `csv.ts` · `date.ts` · `preparationParts.tsx` |
| `mocks/` | **0** | 폴더 없음 |
| `styles/` | 1 | `src/styles/globals.css` |

`views/` 25 파일(화면 12종):

| 화면 폴더 | 파일 |
|---|---|
| `aggregation/` | `Aggregation.tsx` · `ServingSheetDialog.tsx` · `ServingSheetDocument.tsx` · `aggregationLabels.ts` |
| `change-requests/` | `ChangeRequests.tsx` · `AddRequestCard.tsx` · `requestDetail.ts` |
| `closing/` | `Closing.tsx` |
| `dashboard/` | `Dashboard.tsx` |
| `diet-meals/` | `DietMeals.tsx` · `DietDefaultsPanel.tsx` |
| `diet-orders/` | `DietOrders.tsx` |
| `meal-codes/` | `MealCodes.tsx` |
| `meal-orders/` | `MealOrders.tsx` · `MealSlotCard.tsx` · `OrderFields.tsx` · `ChangeRequestDialog.tsx` · `mealOrderModel.ts` |
| `not-served/` | `NotServed.tsx` · `ManualNotServedCard.tsx` |
| `preparation/` | `Preparation.tsx` · `NotServedDialog.tsx` |
| `statistics/` | `Statistics.tsx` |
| `ward-diet-orders/` | `WardDietOrders.tsx` (1924줄, 최대) · `DischargeMealDialog.tsx` |

줄 수 상위: `WardDietOrders.tsx` 1924 · `foodserviceApi.ts` 1298 · `DietMeals.tsx` 1132 · `DietOrders.tsx` 870 · `Closing.tsx` 770.

`public/` 에는 `index.html` 하나뿐이다(`public/i18n/` 없음 — §10).

### 1.2 `package.json` — dependencies · scripts

```json
// package.json:5-28
  "scripts": {
    "dev": "rspack serve --mode development",
    "build": "rspack build --mode production",
    "lint": "eslint src",
    "check:shared": "check-ehr-shared",
    "format": "prettier --write .",
    "format:check": "prettier --check ."
  },
  "dependencies": {
    "@tanstack/react-query": "^5.90.21",
    "@x-ehr/api-axios": "~0.0.9",
    "@x-ehr/app-kit": "~0.0.32",
    "@x-ehr/auth-contract": "~0.0.16",
    "@x-ehr/design-system": "0.0.34",
    "@x-ehr/event-bus": "~0.0.21",
    "@x-ehr/i18n": "~0.0.12",
    "axios": "^1.13.6",
    "i18next": "^25.10.10",
    "react": "19.2.3",
    "react-dom": "19.2.3",
    "react-i18next": "^16.6.6",
    "react-router": "^7.14.2",
    "zustand": "^5.0.11"
  },
```

워크스페이스 루트 스크립트(`../../package.json:12-23`): `check:screens`(킷 사용 검사) · `typecheck` · `check:ts-single` · `i18n:check --allow-empty` 가 있고, `resolutions` 로 `@tanstack/react-query` 를 `5.101.4` 에 고정한다(`../../package.json:31-34`).

> `zustand` · `@x-ehr/event-bus` · `i18next` · `react-i18next` · `@x-ehr/i18n` 은 dependencies 에 있지만 **`src/` 에서 import 하는 곳이 없다**(§6 · §7 · §10 의 grep 결과).

### 1.3 `rspack.config.js` — 전체

```js
// rspack.config.js:1-43
const { createRemoteRspackConfig } = require('@x-ehr/rspack-config')

// BC 레포(x-nutrition) 의 MF remote. MF name(remoteKey)=nutrition,
// 메뉴 path base=/nutrition. ./routes 는 factory 가 강제 노출(host seam).
//
// remoteKey·port 는 **불변** — host 는 env.js 의 remoteBaseUrl 로
// `${base}/nutrition/remoteEntry.js` convention 을 조립하므로 이 값이 바뀌면 host 가 못 찾는다.
// (_docs/README.md — remoteKey `nutrition`, dev port 3343)
const DEV_PORT = 3343

// 로컬 백엔드(foodservice) 포트 — dev-up 가 BE_PORT 로 오버라이드하면 그 값을 따라간다.
const BE_PORT = process.env.BE_PORT || '5343'

// 게이트웨이(Istio) 계약 경로. 백엔드 **컨트롤러가 이 prefix 를 그대로 매핑**하므로(context-path 는 "/")
// pathRewrite 가 필요 없다 — 벗기면 404 다.
const GATEWAY_PREFIX = '/api/v1/foodservice'

const configFn = createRemoteRspackConfig({
  dirname: __dirname,
  name: 'nutrition',
  port: DEV_PORT,
  shared: ['@x-ehr/app-kit', '@x-ehr/auth-contract', '@x-ehr/design-system'],
})

// [standalone dev 전용] API 프록시.
// Standalone(:3343) 단독 실행에는 host devServer 프록시가 없어 `/api/v1/foodservice/**` 요청이
// 이 dev 서버로 떨어진다 → historyApiFallback 이 index.html 을 돌려주거나 404 가 난다.
// 로컬 백엔드로 넘겨주는 프록시를 devServer 에 덧붙인다.
// (Host 경유 실행에서는 host 의 프록시가 먼저 처리하므로 이 설정은 영향이 없다.)
module.exports = (env, argv) => {
  const cfg = configFn(env, argv)
  if (cfg.devServer) {
    cfg.devServer.proxy = [
      ...(Array.isArray(cfg.devServer.proxy) ? cfg.devServer.proxy : []),
      {
        context: [GATEWAY_PREFIX],
        target: `http://127.0.0.1:${BE_PORT}`,
        changeOrigin: true,
      },
    ]
  }
  return cfg
}
```

빌드 산출물(`dist/`): `remoteEntry.js` · `mf-manifest.json` · `__federation_expose_routes.*.js/.css`. `mf-manifest.json` 요약 — `name: nutrition`, `exposes: ['./routes']`, shared 싱글턴 = react-router 7.18.2 · @x-ehr/api-axios 0.0.9 · @x-ehr/design-system 0.0.34 · react/react-dom 19.2.3 · @tanstack/react-query 5.101.4 · axios 1.19.0.

### 1.4 `eslint.config.mjs` (워크스페이스 루트) — 커스텀 규칙

```js
// ../../eslint.config.mjs:1-30
import { existsSync, readdirSync } from 'node:fs'
import { dirname, join } from 'node:path'
import { fileURLToPath } from 'node:url'
import base from '@x-ehr/eslint-config'
import remoteConfig from '@x-ehr/eslint-config/remote'
import mfBuildContract from '@x-ehr/eslint-config/build-contract'

// BC 레포의 frontend 워크스페이스 루트 config.
// remote 앱 자동 발견 — `apps/<dir>/src/routes.tsx`(./routes MF seam)를 가진 앱이 remote.
// (x-ehr-foundation 루트 config 와 동일 규약 — 신규 remote 를 apps/ 에 추가하면 등록 없이 포함된다.)
const APPS_DIR = join(dirname(fileURLToPath(import.meta.url)), 'apps')
const REMOTE_DIRS = readdirSync(APPS_DIR).filter((d) =>
  existsSync(join(APPS_DIR, d, 'src', 'routes.tsx')),
)

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
]
```

레포 자체 규칙은 없고 `@x-ehr/eslint-config/remote` 가 막는 것(Nexus 설치본 `node_modules/@x-ehr/eslint-config/remote.mjs:33-124`):
`axios` 값 import · `react-router` 의 `createBrowserRouter`/`RouterProvider` 류 · `@tanstack/react-query` 의 `QueryClient` · `@module-federation/enhanced/runtime` · `@x-ehr/app-kit` 의 `remoteComponent` · `@x-ehr/api-axios` 의 `setAccessToken`/`logout` 등 세션 변경 API · `@x-ehr/auth-contract` 의 `parsePermissions`/`hasPermission` · `../` 상대 import(`remote.mjs:150`) · routes 의 절대 path(`remote.mjs:197`) · `lazy` 속성(`remote.mjs:205`).

---

## 2. 진입점

### 2.1 `index.ts` — MF 비동기 경계

```ts
// src/index.ts:1-2
// MF 비동기 경계 — shared 초기화가 끝난 뒤 standalone 부팅한다.
void import('./bootstrap')
```

### 2.2 `bootstrap.tsx`

```tsx
// src/bootstrap.tsx:1-13
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

### 2.3 `routes.tsx` — 유일한 MF expose (RemoteScope + 라우트 12개)

```tsx
// src/routes.tsx:1-58
import { Outlet, useOutletContext, type RouteObject } from 'react-router'
import Dashboard from './views/dashboard/Dashboard'
import DietOrders from './views/diet-orders/DietOrders'
import MealOrders from './views/meal-orders/MealOrders'
import WardDietOrders from './views/ward-diet-orders/WardDietOrders'
import Closing from './views/closing/Closing'
import Preparation from './views/preparation/Preparation'
import Aggregation from './views/aggregation/Aggregation'
import ChangeRequests from './views/change-requests/ChangeRequests'
import NotServed from './views/not-served/NotServed'
import Statistics from './views/statistics/Statistics'
import DietMeals from './views/diet-meals/DietMeals'
import MealCodes from './views/meal-codes/MealCodes'
// Host nest 시에도 remote 유틸리티 CSS 가 로드되도록 노출 진입점에서 import.
import './styles/globals.css'

// remote CSS 격리 경계 — 전역 리셋이 host 헤더/배경을 덮지 않도록 모든 화면을 .x-remote-root 로 감싼다
// (host/standalone 공통 seam). display:contents 라 레이아웃 영향은 없고,
// 부모(host <main>/standalone shell)의 outlet context(auth)를 그대로 하위 화면에 전달한다.
//
// `khds-root` 는 디자인시스템(@x-ehr/design-system)의 소비측 계약이다 — dist CSS 의 base 리셋(box-sizing·폰트)이
// 이 클래스 하위로만 스코프돼 있어, 빠지면 컴포넌트가 브라우저 기본 모양으로 풀린다.
// 격리 경계와 킷 스코프는 같은 엘리먼트에 둔다.
function RemoteScope() {
  const ctx = useOutletContext()
  return (
    <div className="x-remote-root khds-root">
      <Outlet context={ctx} />
    </div>
  )
}

// top-level path 는 Host/메뉴 소유 — remote 는 상대 child route 만 정의한다(absolute path 금지).
// 메뉴 leaf path `/nutrition/<child>` 의 base segment 에 host 가 이 routes 를 1회 nest 한다.
// 화면 ↔ API 매핑은 _docs/design/foodservice-api-spec.md §2, 시안은 _docs/views/*.html.
const routes: RouteObject[] = [
  {
    element: <RemoteScope />,
    children: [
      { path: 'dashboard', element: <Dashboard /> },
      { path: 'diet-orders', element: <DietOrders /> },
      { path: 'meal-orders', element: <MealOrders /> },
      { path: 'ward-diet-orders', element: <WardDietOrders /> },
      { path: 'closing', element: <Closing /> },
      { path: 'preparation', element: <Preparation /> },
      { path: 'aggregation', element: <Aggregation /> },
      { path: 'change-requests', element: <ChangeRequests /> },
      { path: 'not-served', element: <NotServed /> },
      { path: 'statistics', element: <Statistics /> },
      // 기준 코드 마스터(식이처방·식이 코드) — 영양팀·관리자. 시안 _docs/views/diet-meals.html
      { path: 'diet-meals', element: <DietMeals /> },
      // 식이 코드 목록(식이 코드 기준 + 식이처방 코드 연결, 조회 전용). 시안 _docs/views/meal-codes.html
      { path: 'meal-codes', element: <MealCodes /> },
    ],
  },
]

export default routes
```

### 2.4 `app/Standalone.tsx` — dev 전용 host 에뮬레이터 (핵심부)

```tsx
// src/app/Standalone.tsx:22-53
// Host 없이 단독 개발할 때만 쓰는 에뮬레이터 — mock auth·자체 QueryClient·dev 메뉴를 제공한다(standalone = dev only).
// 운영(Host nest)에선 Host 의 Provider·authContext·단일 QueryClient·단일 i18next 를 쓰고 **메뉴도 host 가 소유**한다
// (이 파일은 prod 빌드에서 제외되므로 remote 산출물에 이 층이 섞이지 않는다).
// 메뉴는 킷 `Dropdown`(라디오 항목 = 현재 화면 1개)으로 조립한다 — 이 레포는 `yarn check:screens` 를 돌리므로
// 네이티브 `<button>`·`text-sm` 류가 위반이다.
const mockAuth = createMockAuthContext()
const queryClient = new QueryClient()

// 경로는 `routes.tsx` 의 child path 와 1:1 로 맞춘다(불일치 시 화면이 안 뜨고 landing 으로 되돌아간다).
const DEV_MENU: Array<{ path: string; label: string }> = [
  { path: 'dashboard', label: '홈' },
  { path: 'diet-orders', label: '식이처방 접수' },
  { path: 'meal-orders', label: '세부오더 관리' },
  { path: 'ward-diet-orders', label: '병동오더 발행' },
  { path: 'closing', label: '마감 관리' },
  { path: 'preparation', label: '조제·배식 현황' },
  { path: 'aggregation', label: '식이처방별 집계' },
  { path: 'change-requests', label: '변경요청 관리' },
  { path: 'not-served', label: '결식 관리' },
  { path: 'statistics', label: '식수 실적' },
  { path: 'diet-meals', label: '식이처방·식이 코드 마스터' },
  { path: 'meal-codes', label: '식이 코드 목록' },
]

const LANDING = 'dashboard'
const LABEL_BY_PATH = new Map(DEV_MENU.map((m) => [m.path, m.label]))

/** 현재 URL → 메뉴 path. 메뉴에 없는 값이면 null. */
function screen_of(pathname: string): string | null {
  const key = pathname.replace(/^\/+/, '').replace(/\/+$/, '')
  return LABEL_BY_PATH.has(key) ? key : null
}
```

```tsx
// src/app/Standalone.tsx:109-159
// 공유 routes 는 leaf-only(허브 없음) — 에뮬레이터만의 landing 으로 index→대시보드 redirect 를 앞에 끼우고,
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

/**
 * 페이지 chrome(높이 체인의 최상단)을 standalone 이 소유한다 — 운영에서는 host 가 `<main>` 에 확정 높이를 주지만
 * standalone 에는 host 가 없어 html/body/#root 가 auto 로 남고 본문이 0 으로 접힌다.
 */
function useShellChrome() {
  useEffect(() => {
    const root = document.getElementById('root')
    const targets = [document.documentElement, document.body, root].filter((el) => el != null)
    const prev = targets.map((el) => ({ el, height: el.style.height, margin: el.style.margin }))
    targets.forEach((el) => {
      el.style.height = '100%'
      el.style.margin = '0'
    })
    return () =>
      prev.forEach(({ el, height, margin }) => {
        el.style.height = height
        el.style.margin = margin
      })
  }, [])
}

export default function Standalone() {
  useShellChrome()
  // 운영(Host nest)에서는 host 셸이 ToastProvider 를 마운트한다 — dev 단독 실행일 때만 여기서 같은 Provider 를 세운다
  // (useToast 는 Provider 밖에서 호출하면 throw 한다).
  // DragProvider 도 host 가 앱 루트에 하나 두는 계약이다(remote 마다 두지 않는다 — design-system dnd.md).
  // 없으면 DataGrid 행 끌기(식이처방·식이 코드 순서)가 no-op 이 되므로 standalone 에서만 같은 자리에 세운다.
  return (
    <QueryClientProvider client={queryClient}>
      <ToastProvider aria-label="알림">
        <DragProvider>
          <RouterProvider router={router} />
        </DragProvider>
      </ToastProvider>
    </QueryClientProvider>
  )
}
```

DevShell(`src/app/Standalone.tsx:63-107`)은 `Dropdown`+`DropdownRadioGroup` 메뉴와 `<Outlet context={mockAuth} />` 로 host 의 auth 주입을 흉내 낸다(`:102-104`).

---

## 3. 화면 → API 추적표

### 3.1 화면별 (routes 의 12 path)

widget/feature/entity 레이어가 없으므로 "사용하는 하위 컴포넌트"와 "직접 호출하는 API 함수"를 적는다. 모든 API 함수는 `src/shared/api/foodserviceApi.ts` 에 있고, 상태 보관은 전 화면 **react-query 캐시 + `useState`** 다(zustand 없음).

| 화면(path) | view 컴포넌트 | 하위 컴포넌트(화면 전용) | 공용 shared/ui · lib | API 함수(entity 상당) | 조회 훅 | 쓰기 훅 | 상태 보관 |
|---|---|---|---|---|---|---|---|
| `dashboard` | `views/dashboard/Dashboard.tsx` | — | ScreenToolbar · PanelBar · codeLabels · date | `findActiveMealTypes` · `findDashboardSummary` · `findClosingStatus` · `findRecentEvents` | **`useQuery`(raw)** + `refetchInterval: 30_000`(`:44,269-279`) | — | react-query · `useRef`(카운트다운 1회 가드 `:288-293`) |
| `diet-orders` | `views/diet-orders/DietOrders.tsx` | — | ScreenToolbar/ToolbarField · PanelBar/PanelFooter · codeLabels · date · dietAttributes · patient | `findWards` · `findClosingStatus` · `aggregateByWard` · `findDietOrders` · `findMealPreparations` · `findMealForms` | `useFreshQuery` | 없음(접수·보류 버튼 비활성 — `:53-54`) | react-query · useState(필터·선택) |
| `meal-orders` | `views/meal-orders/MealOrders.tsx` | `MealSlotCard` · `OrderFields` · `ChangeRequestDialog` · `mealOrderModel.ts` | ScreenToolbar · PanelBar · codeLabels · date · dietAttributes · patient · query_cache | `findWards` · `findMealOrderBoard` · `findClosingBoard` · `findMealPreparations` · `findMealForms` · `findDiets(true)` · `findDietOrders` · `findMealOrders` · `issueMealOrder` · `reviseMealOrder` · `revokeMealOrder` · `createChangeRequest`(Dialog) | `useFreshQuery` | `useMutation` ×2(save · revoke) + Dialog 안 `useMutation`(submit) | react-query · useState(`editing`/`request`/`revokeTarget` 폼 상태 `:108-116`) |
| `ward-diet-orders` | `views/ward-diet-orders/WardDietOrders.tsx` | `DischargeMealDialog` | ScreenToolbar · codeLabels · date · dietAttributes · query_cache | `findWards` · `findMealOrderBoard`(병동 N개) · `findActiveMealTypes` · `findFoodserviceSetting` · `findDietOrders` · `findMealOrders` · `findMealForms` · `findDiets` · `issueMealOrder` · `reviseMealOrder` · `revokeMealOrder` · `setDischargeMeal`(Dialog) | `useFreshQuery` · **`useFreshQueries`**(`:283-288`) | `useMutation` ×1 + Dialog `useMutation` | react-query · useState(끼니 카드 편집본 → 하단 한 번 저장) |
| `closing` | `views/closing/Closing.tsx` | — | ScreenToolbar · PanelBar · codeLabels · date | `findActiveMealTypes` · `findFoodserviceSetting` · `findClosingBoard` · `findClosingHistories` · `closeMeals` · `reopenMeals` · `generatePreparations` | `useQuery`(raw) | `useMutation` ×3 | react-query · useState |
| `preparation` | `views/preparation/Preparation.tsx` | `NotServedDialog` | ScreenToolbar · RestoreDialog · codeLabels · date · preparationParts | `findActiveMealTypes` · `findWards` · `findMealPreparations` · `startPreparation` · `completePreparation` · `markNotServed` · `restorePreparation` | `useQuery`(raw) | `useMutation` ×3 | react-query · useState · `useEffect`(기본 끼니) |
| `aggregation` | `views/aggregation/Aggregation.tsx` | `ServingSheetDialog` · `ServingSheetDocument` · `aggregationLabels.ts` | ScreenToolbar · date · codeLabels | `findActiveMealTypes` · `aggregateByDietType` · `aggregateByWard` · `findServingSheet`(Dialog, `enabled: open`) · `findMealForms`(Dialog) | `useQuery`(raw) | 없음(조회·인쇄) | react-query · useState |
| `change-requests` | `views/change-requests/ChangeRequests.tsx` | `AddRequestCard` · `requestDetail.ts` | ScreenToolbar · codeLabels · date | `findChangeRequests` · `findWards` · `findActiveMealTypes` · `findDiets(false)` · `findMealForms` · `confirmMealChange` · `applyMealChange` · `rejectMealChange` · `findMealOrderBoard`(AddRequestCard) · `createChangeRequest`(AddRequestCard) | `useQuery`(raw, 마스터는 `staleTime: 600_000` `:95-100`) / AddRequestCard 는 `useFreshQuery` | `useMutation` ×1 + 카드 `useMutation` | react-query · useState |
| `not-served` | `views/not-served/NotServed.tsx` | `ManualNotServedCard` | ScreenToolbar · PanelBar · RestoreDialog · codeLabels · date · preparationParts | `findActiveMealTypes` · `findWards` · `findNotServed` · `restorePreparation` · `findMealPreparations`(카드) · `markNotServed`(카드) | `useQuery`(raw) | `useMutation` ×1 + 카드 `useMutation` | react-query · useState |
| `statistics` | `views/statistics/Statistics.tsx` | — | ScreenToolbar · csv · date | `findDailyStatistics` | `useQuery`(raw) | 없음(CSV 내려받기는 `downloadCsvFile`) | react-query · useState(from/to) |
| `diet-meals` | `views/diet-meals/DietMeals.tsx` | `DietDefaultsPanel` | PanelBar · codeLabels · date · query_cache | `findDiets(false)` · `findMealForms` · `createDiet` · `saveDiet` · `saveDietDefaults`(패널) | `useFreshQuery` | **`useMutation` 없음 — `async function saveAll()`에서 직접 await**(`:528-595`) | react-query · useState(`draft` + `dirty: Set` 편집본) |
| `meal-codes` | `views/meal-codes/MealCodes.tsx` | — | PanelBar · codeLabels · csv · date · query_cache | `findDiets(false)` | `useFreshQuery` | 없음(조회 전용) | react-query · useState(정렬·필터) |

> 조회 훅 2종이 섞여 있다 — `useFreshQuery`(staleTime 0 래퍼) 를 쓰는 화면: diet-orders · meal-orders · ward-diet-orders · diet-meals · meal-codes · AddRequestCard. raw `useQuery` 를 쓰는 화면: dashboard · closing · preparation · aggregation · change-requests · not-served · statistics · ManualNotServedCard · ServingSheetDialog. (근거: `grep -rn "from '@tanstack/react-query'" src` 와 `grep -rn "from '@/shared/lib/query_cache'" src`.) §13 참고.

### 3.2 API 함수 전체 — `src/shared/api/foodserviceApi.ts` (BASE = `/api/v1/foodservice`, `:9`)

| 함수(줄) | HTTP | 경로 | 응답 타입 |
|---|---|---|---|
| `findDashboardSummary(date?)` `:894` | GET | `/dashboard/summary?date=` | `DashboardSummaryView` |
| `findClosingStatus(date?)` `:898` | GET | `/dashboard/closing-status?date=` | `ClosingStatusView` |
| `findRecentEvents(limit=20)` `:902` | GET | `/dashboard/recent-events?limit=` | `RecentEventView[]` |
| `findDietOrders(params)` `:908` | GET | `/diet-orders?ward_id&status&keyword&page&size` | `PageView<DietOrderView>` |
| `findMealOrderBoard(wardId, date)` `:920` | GET | `/wards/{wardId}/meal-order-board?date=` | `MealOrderBoardView` |
| `findMealOrders(dietOrderId)` `:924` | GET | `/diet-orders/{id}/meal-orders` | `MealOrderView[]` |
| `issueMealOrder(body)` `:928` | POST | `/meal-orders` | `MealOrderView` |
| `reviseMealOrder(id, body)` `:959` | PUT | `/meal-orders/{id}` | `MealOrderView` |
| `revokeMealOrder(id, reason?)` `:982` | POST | `/meal-orders/{id}/revoke` | `MealOrderView` |
| `setDischargeMeal(dietOrderId, body)` `:990` | PUT | `/diet-orders/{id}/discharge-meal` | `DietOrderView` |
| `findMealPreparations(params)` `:999` | GET | `/meal-preparations?meal_date&meal_type_code&ward_id&status&keyword&page&size` | `WorklistView<MealPreparationView>` |
| `startPreparation(id)` `:1011` | POST | `/meal-preparations/{id}/start` | `MealPreparationView` |
| `completePreparation(id)` `:1015` | POST | `/meal-preparations/{id}/complete` | `MealPreparationView` |
| `markNotServed(id, body)` `:1019` | POST | `/meal-preparations/{id}/not-served` | `MealPreparationView` |
| `restorePreparation(id, reason)` `:1023` | POST | `/meal-preparations/{id}/restore` | `MealPreparationView` |
| `generatePreparations(body)` `:1027` | POST | `/meal-preparations/generate` | `{created, skipped, candidates, meal_preparation_ids…}` |
| `aggregateByDietType(meal_date, meal_type_code)` `:1045` | GET | `/meal-preparations/aggregations/diet-types?meal_date&meal_type_code` | `DietTypeAggregationView` |
| `aggregateByWard(meal_date, meal_type_code)` `:1051` | GET | `/meal-preparations/aggregations/wards?…` | `WardAggregationView` |
| `findServingSheet(meal_date, meal_type_code)` `:1095` | GET | `/meal-preparations/serving-sheet?meal_date&meal_type_code&format=json` | `ServingSheetView` |
| `findNotServed(meal_date, ward_id?)` `:1101` | GET | `/meal-preparations/not-served?meal_date&ward_id` | `NotServedView` |
| `findClosingBoard(meal_date)` `:1107` | GET | `/meal-closings/board?meal_date=` | `ClosingBoardView` |
| `findClosingHistories(params)` `:1111` | GET | `/meal-closings/histories?meal_date&ward_id&page&size` | `PageView<ClosingHistoryView>` |
| `closeMeals(body)` `:1121` | POST | `/meal-closings/close` | `ClosingResultView` |
| `reopenMeals(body)` `:1131` | POST | `/meal-closings/reopen` | `ClosingResultView` |
| `findChangeRequests(params)` `:1142` | GET | `/meal-change-requests?status&meal_date&ward_id&request_type_code&page&size` | `WorklistView<MealChangeRequestView>` |
| `createChangeRequest(body)` `:1170` | POST | `/meal-change-requests` | `MealChangeRequestView` |
| `confirmMealChange(id)` `:1184` | POST | `/meal-change-requests/{id}/confirm` | `MealChangeRequestView` |
| `applyMealChange(id)` `:1188` | POST | `/meal-change-requests/{id}/apply` | `MealChangeRequestView` |
| `rejectMealChange(id, reason)` `:1193` | POST | `/meal-change-requests/{id}/reject` | `MealChangeRequestView` |
| `findDailyStatistics(from, to)` `:1199` | GET | `/meal-statistics/daily?from&to` | `DailyStatisticsView` |
| `findWards()` `:1205` | GET | `/masters/wards` | `WardView[]` |
| `findMealForms()` `:1210` | GET | `/masters/meal-forms` | `MealFormView[]` |
| `findDiets(activeOnly?)` `:1218` | GET | `/masters/diets?active_only=` | `DietView[]` |
| `createDiet(body)` `:1223` | POST | `/masters/diets` | `DietView` |
| `saveDiet(dietId, body)` `:1239` | PUT | `/masters/diets/{id}` | `DietView` |
| `saveDietDefaults(dietId, body)` `:1271` | PUT | `/masters/diets/{id}/defaults` | `DietView` |
| `findFoodserviceSetting()` `:1278` | GET | `/settings` | `FoodserviceSettingView` |
| `findActiveMealTypes()` `:1283` | GET | `/settings/meal-types` | `MealTypeLabelView[]` |
| `saveMealTypeSettings(body)` `:1288` | PUT | `/settings/meal-types` | `FoodserviceSettingView` |
| `saveClosingRoundSettings(body)` `:1293` | PUT | `/settings/closing-rounds` | `FoodserviceSettingView` |

총 40 함수. 그 외 export: `mealTypeLabel()` `:79`(코드→이름 헬퍼) · `DEFAULT_MEAL_TYPES` `:71` · `ApiError` 클래스 `:836`.
`saveMealTypeSettings` · `saveClosingRoundSettings` 는 정의만 있고 **views 에서 호출하는 곳이 없다**(grep 결과 — 급식 운영 설정 화면이 아직 없다).

---

## 4. 데이터 계층 샘플 — 「세부오더 관리」(`meal-orders`) 끝에서 끝까지

체인: `query_cache.ts` → `foodserviceApi.ts`(request/query/ApiError) → `findMealOrderBoard` + `MealOrderBoardView` 타입 → `MealOrders.tsx`(useFreshQuery · 파생값) → `MealSlotCard.tsx`(props) → 렌더.

### 4.1 staleTime 0 래퍼 — `shared/lib/query_cache.ts`

```ts
// src/shared/lib/query_cache.ts:11-42
/**
 * 조회는 **늘 서버의 최신 값을 읽는다** — 화면이 열릴 때마다 다시 조회한다.
 *
 * @remarks
 * QueryClient 는 host 의 것이고 host 기본값이 `staleTime` 5분이다. 그대로 두면 5분 안에 다시 연 화면이
 * 서버에 묻지 않고 처음 읽은 값을 보여 준다. 그래서 받은 값을 최신으로 치지 않고(`staleTime: 0`)
 * 쓰는 화면이 없어지면 곧바로 버린다(`gcTime: 0`).
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

/** `useQueries` 와 같다 — 쿼리마다 캐시 옵션을 [READ_FROM_DB] 로 고정한다. */
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

### 4.2 공용 HTTP 베이스 — api-axios 사용부 · 경로 조립 · 에러 변환

```ts
// src/shared/api/foodserviceApi.ts:1-9
// 영양(nutrition) BC / 급식(foodservice) MS API 클라이언트.
// 백엔드 view_model / command 와 **snake_case 그대로** 1:1 정합한다(변환하지 않는다).
// Base URL 은 게이트웨이(Istio) / dev 프록시가 /api/v1/foodservice 로 라우팅한다.
//
// 호출은 foundation 표준 @x-ehr/api-axios(`api`)에 위임한다 — Remote 자체 fetch/토큰 보유 금지
// (인스턴스 분열 시 401 refresh·토큰이 깨진다).
import { api } from '@x-ehr/api-axios'

const BASE = '/api/v1/foodservice'
```

```ts
// src/shared/api/foodserviceApi.ts:836-890
export class ApiError extends Error {
  constructor(
    readonly status: number,
    readonly code: string,
    message: string,
    readonly context: Record<string, unknown> = {},
  ) {
    super(message)
    this.name = 'ApiError'
  }

  /** 마감된 끼니의 직접 수정 시도인지 — 화면은 변경요청 다이얼로그로 유도한다. */
  get isMealClosed(): boolean {
    return this.code === 'FS-4091'
  }

  /** 낙관적 잠금 충돌(409 FS-4094) — 다른 사용자가 먼저 저장했다. 다시 조회한 뒤 저장해야 한다. */
  get isVersionConflict(): boolean {
    return this.code === 'FS-4094'
  }

  /** 조절값이 축의 허용 범위 밖(400 FS-4005) — `context.violations` 에 항목별 사유가 있다. */
  get isAttributeViolation(): boolean {
    return this.code === 'FS-4005'
  }
}

async function request<T>(path: string, method = 'GET', body?: unknown): Promise<T> {
  try {
    const res = await api.request<T>({ url: `${BASE}${path}`, method, data: body })
    return res.data
  } catch (e: unknown) {
    const err = e as {
      response?: {
        status?: number
        data?: { code?: string; message?: string; context?: Record<string, unknown> }
      }
    }
    const status = err.response?.status ?? 0
    throw new ApiError(
      status,
      err.response?.data?.code ?? 'UNKNOWN',
      err.response?.data?.message ?? `요청에 실패했습니다. (HTTP ${status})`,
      err.response?.data?.context ?? {},
    )
  }
}

function query(params: Record<string, string | number | boolean | undefined | null>): string {
  const qs = Object.entries(params)
    .filter(([, v]) => v != null && v !== '')
    .map(([k, v]) => `${encodeURIComponent(k)}=${encodeURIComponent(String(v))}`)
    .join('&')
  return qs ? `?${qs}` : ''
}
```

### 4.3 entity api 함수 1개 — `findMealOrderBoard`

```ts
// src/shared/api/foodserviceApi.ts:920-922
export function findMealOrderBoard(wardId: string, date: string) {
  return request<MealOrderBoardView>(`/wards/${wardId}/meal-order-board${query({ date })}`)
}
```

### 4.4 entity model 타입 — `MealOrderBoardView` · 행 · 칸

```ts
// src/shared/api/foodserviceApi.ts:329-361
export interface MealOrderSlotView {
  meal_order_id: string
  /** 이 칸의 값은 **조회한 날짜의 리비전** 값이다 — 날짜를 바꾸면 그 날짜의 값이 온다. */
  meal_code: string | null
  meal_name: string | null
  unit_qty: number | null
  /** 형태 · 염도 — 완제품 · 금식 식종은 null. */
  meal_form_code: MealFormCode | null
  portion_code: PortionCode
  salt_level_code: SaltLevelCode | null
  kcal: number | null
  volume_ml: number | null
  /** 격리 배식 — 식기·배선을 따로 쓴다. */
  is_isolation_meal: boolean
  is_guardian_meal: boolean
  /** 보호자식 옵션 — 환자와 다른 사람의 식사라 같은 축을 따로 갖는다. */
  guardian_portion_code: PortionCode
  guardian_salt_level_code: SaltLevelCode
  is_guardian_isolation_meal: boolean
  status: MealOrderStatus
  /** 이 값들이 적용되기 시작한 날짜 — 화면이 "언제부터 적용된 값인지" 를 적는다. */
  revision_effective_from: string
  revision_effective_to: string | null
  preparation_status: PreparationStatus | null
  closed: boolean
  /** 접수자료의 현재 마감 차수(있으면). */
  closed_round: number | null
  /** 필수 축(`is_required`) 값이 비어 있다 — 자동 발행이 기본값 없이 만든 세부식사라 간호 확인이 필요하다. */
  needs_review: boolean
  /** 값이 빈 필수 축 코드(`MealAttributeCode`). */
  missing_attribute_codes: string[]
  allowed_actions: string[]
}
```

```ts
// src/shared/api/foodserviceApi.ts:368-411
export interface MealOrderBoardRowView {
  /** 행 식별자 — 처방의 수진 1건(shared 수진 uuid). */
  encounter_id: string
  diet_order_id: string | null
  patient: PatientView
  ward_id: string | null
  room_id: string | null
  bed_no: string | null
  department_name: string | null
  doctor_name: string | null
  /**
   * 급식 대상 구간 — **입원 ~ 퇴원**. 달력이 이 구간 밖을 "대상 아님" 으로 흐리게 그린다.
   * 입원일 · 퇴원일은 shared 수진의 일자다 — 퇴원 전이면 `discharge_date` 는 null.
   */
  admission_date: string | null
  discharge_date: string | null
  /** 퇴원 예정일 · 퇴식끼니 — 처방의 값. 퇴원 예정일이 있고 퇴식끼니가 없으면 화면이 「퇴식끼니 미설정」을 보인다. */
  discharge_planned_date?: string | null
  discharge_last_meal_type_code?: MealTypeCode | null
  diet_type_code: string | null
  diet_type_name: string | null
  /** 식수 집계 열 GENERAL / THERAPEUTIC / SOFT(마스터 미등재면 null). */
  diet_type_category_code: DietTypeCategory | null
  /** 식종 분류 — Diet 마스터 값(마스터 미등재면 null). */
  diet_category_code: DietCategoryCode | null
  /** 금식(NPO) 식종 — 이 행에는 발행할 끼니가 없다. */
  diet_type_npo: boolean
  /** 완제품(PRODUCT) 식종 — 끼니 편집이 형태 · 염도 대신 규격 · 개수를 받는다. */
  diet_type_product: boolean
  instruction_text: string | null
  diet_order_status: DietOrderStatus | null
  /** 이 식이처방에 허용되는 끼니 코드 — 설정의 대상 식이처방으로 거른 값. 처방이 없으면 전 끼니. */
  applicable_meal_types: MealTypeCode[]
  slots: Record<string, MealOrderSlotView | null>
}

export interface MealOrderBoardView {
  meal_date: string
  ward_id: string
  /** 활성 끼니 코드 — 열 순서. 설정이 원천이다. */
  slots: MealTypeCode[]
  meal_types: MealTypeLabelView[]
  content: MealOrderBoardRowView[]
}
```

### 4.5 feature model 상당 — view 안의 조회 훅 묶음과 파생값 (`MealOrders.tsx`)

```tsx
// src/views/meal-orders/MealOrders.tsx:105-179
export default function MealOrders() {
  const { toast } = useToast()
  const client = useQueryClient()
  const [date, setDate] = useState(today())
  const [wardId, setWardId] = useState('')
  const [keyword, setKeyword] = useState('')
  const [encounterId, setEncounterId] = useState('')
  /** 펼친 끼니 하나(시안 L7) — 환자·날짜·끼니를 키로 들고 있어 선택이 바뀌면 저절로 접힌다. */
  const [editing, setEditing] = useState<{ key: string; slot: MealTypeCode; form: OrderForm } | null>(null)
  const [request, setRequest] = useState<{ key: string; draft: RequestDraft } | null>(null)
  const [revokeTarget, setRevokeTarget] = useState<{ slot: MealTypeCode; meal_order_id: string } | null>(null)
  const [revokeReason, setRevokeReason] = useState('')

  // Q-23 — 병동 마스터(원천은 x-ancillary-main). 못 읽으면 서버가 빈 배열을 준다.
  const wards = useFreshQuery({ queryKey: ['foodservice', 'wards'], queryFn: findWards })
  const ward = wardId || wards.data?.[0]?.ward_id || ''
  const wardMasterEmpty = !wards.isPending && (wards.data?.length ?? 0) === 0

  // Q-20 — 행 = 재원환자, 칸 = 끼니별 세부오더(그 날짜의 리비전 값).
  const board = useFreshQuery({
    queryKey: ['foodservice', 'meal-order-board', ward, date],
    queryFn: () => findMealOrderBoard(ward, date),
    enabled: !!ward,
  })
  // Q-40 — 병동 × 끼니 마감. 미발행 끼니·접수자료 없는 끼니의 「추가요청」 판단에만 쓴다.
  const closingBoard = useFreshQuery({
    queryKey: ['foodservice', 'closing-board', date],
    queryFn: () => findClosingBoard(date),
  })
  // Q-30 — 변경·취소요청의 대상 접수자료 id(Q-20 칸에 없다).
  const preparations = useFreshQuery({
    queryKey: ['foodservice', 'preparations', date, 'ALL', ward],
    queryFn: () => findMealPreparations({ meal_date: date, ward_id: ward, size: 500 }),
    enabled: !!ward,
  })
  // Q-22 · Q-24 — 형태 선택지, 식이 코드 · 조절 축(화면이 코드를 박지 않는다).
  const mealForms = useFreshQuery({ queryKey: ['foodservice', 'meal-forms'], queryFn: findMealForms })
  const diets = useFreshQuery({
    queryKey: ['foodservice', 'masters', 'diets', 'active'],
    queryFn: () => findDiets(true),
  })

  const slots: MealTypeCode[] = board.data?.slots ?? []
  const labelOf = (code: MealTypeCode) => mealTypeLabel(code, board.data?.meal_types)
  const servingOf = (code: MealTypeCode) =>
    board.data?.meal_types.find((t) => t.meal_type_code === code)?.serving_time.slice(0, 5) ?? ''
  const formOptions = (mealForms.data ?? []).filter((f) => f.active)
  const formNameOf = (code: string | null) => formNameFrom(mealForms.data, code)

  const kw = keyword.trim()
  const rows = useMemo(
    () =>
      (board.data?.content ?? []).filter(
        (r) => !kw || (r.patient.patient_name ?? '').includes(kw) || (r.patient.patient_no ?? '').includes(kw),
      ),
    [board.data, kw],
  )
  const row: MealOrderBoardRowView | undefined = rows.find((r) => r.encounter_id === encounterId) ?? rows[0]
  const diet = diets.data?.find((d) => d.diet_code === row?.diet_type_code)

  // Q-10 — 선택 환자의 처방(의사 처방 값 7개). 새 끼니의 처음 값이 이 값부터 채워진다(서버 C-20 과 같은 우선순위).
  const patientNo = row?.patient.patient_no ?? ''
  const dietOrders = useFreshQuery({
    queryKey: ['foodservice', 'diet-orders', ward, patientNo],
    queryFn: () => findDietOrders({ ward_id: ward || undefined, keyword: patientNo, size: 20 }),
    enabled: !!patientNo && !!row?.diet_order_id,
  })
  const rx = (dietOrders.data?.content ?? []).find((o) => o.diet_order_id === row?.diet_order_id) ?? null

  // Q-21 — 선택 환자 처방의 세부오더 리비전(카드의 「적용 이력」).
  const mealOrders = useFreshQuery({
    queryKey: ['foodservice', 'meal-orders', row?.diet_order_id],
    queryFn: () => findMealOrders(row?.diet_order_id ?? ''),
    enabled: !!row?.diet_order_id,
  })
```

### 4.6 widget 상당 — 끼니 카드로 내려가는 props (`MealOrders.tsx` → `MealSlotCard`)

```tsx
// src/views/meal-orders/MealOrders.tsx:540-587
            {visibleSlots.length ? (
              <Stack.Item grow className="min-h-0">
                <Grid
                  columns={Math.min(visibleSlots.length, 4)}
                  rows="minmax(0,1fr)"
                  gap={8}
                  className="h-full min-h-0"
                >
                  {visibleSlots.map((slot) => {
                    const cell = row?.slots[slot] ?? null
                    const isEditing = editingSlot?.slot === slot
                    const order = cell ? mealOrders.data?.find((m) => m.meal_order_id === cell.meal_order_id) : undefined
                    return (
                      <MealSlotCard
                        key={slot}
                        label={labelOf(slot)}
                        servingTime={servingOf(slot)}
                        cell={cell}
                        wardClosing={wardClosingOf(slot)}
                        canAdd={baseAvailable}
                        editing={isEditing}
                        form={isEditing ? editingSlot.form : null}
                        saving={save.isPending && isEditing}
                        busy={busy}
                        diet={diet}
                        formOptions={formOptions}
                        formNameOf={formNameOf}
                        revisions={order ? order.revisions : mealOrders.isPending ? null : []}
                        onFormChange={(patch) =>
                          setEditing((e) => (e ? { ...e, form: { ...e.form, ...patch } } : e))
                        }
                        onEdit={() => setEditing({ key: keyOf(slot), slot, form: formOf(cell, diet, date, rx) })}
                        onCancelEdit={() => setEditing(null)}
                        onSave={() => {
                          if (editingSlot) save.mutate({ slot, form: editingSlot.form })
                        }}
                        onRevoke={() => {
                          if (!cell) return
                          setRevokeReason('')
                          setRevokeTarget({ slot, meal_order_id: cell.meal_order_id })
                        }}
                        onRequest={(type) => openRequest(type, slot)}
                      />
                    )
                  })}
                </Grid>
              </Stack.Item>
            ) : null}
```

### 4.7 카드(읽기 상태) 렌더 — `allowed_actions` 만 보고 버튼 노출

```tsx
// src/views/meal-orders/MealSlotCard.tsx:235-284
  return (
    <Card
      title={head}
      action={stateBadge}
      divider
      bodyOverflow="auto"
      className={`h-full min-h-0 ${closed ? 'bg-(--color-surface-subtle)' : ''}`}
      actionArea={
        <Stack direction="row" gap={6} align="center" justify="end" className="w-full">
          {/* 서버가 준 allowed_actions 만 보고 노출한다(상태 규칙을 화면에 중복 구현하지 않는다). */}
          {actions.includes('REVISE') ? (
            <Button variant="gray" size="sm" icon={<Pencil aria-hidden="true" />} disabled={busy} onClick={onEdit}>
              수정
            </Button>
          ) : null}
          {actions.includes('REVOKE') ? (
            <Button variant="error-subtle" size="sm" icon={<X aria-hidden="true" />} disabled={busy} onClick={onRevoke}>
              취소
            </Button>
          ) : null}
          {addable ? (
            <Button
              variant="primary"
              size="sm"
              icon={<Send aria-hidden="true" />}
              disabled={busy}
              onClick={() => onRequest('ADD')}
            >
              추가요청
            </Button>
          ) : null}
          {actions.includes('REQUEST_CHANGE') ? (
            <Button
              variant="primary"
              size="sm"
              icon={<Send aria-hidden="true" />}
              disabled={busy}
              onClick={() => onRequest('CHANGE')}
            >
              변경요청
            </Button>
          ) : null}
          {actions.includes('REQUEST_CANCEL') ? (
            <Button variant="gray" size="sm" icon={<X aria-hidden="true" />} disabled={busy} onClick={() => onRequest('CANCEL')}>
              취소요청
            </Button>
          ) : null}
        </Stack>
      }
    >
```

### 4.8 다른 조회 스타일 — raw `useQuery` + 주기 갱신 (`Dashboard.tsx`) · `useFreshQueries` (`WardDietOrders.tsx`)

```tsx
// src/views/dashboard/Dashboard.tsx:257-280
export default function Dashboard() {
  const date = today()
  const client = useQueryClient()

  // 끼니 목록은 급식 운영 설정이 정한다 — 배식 완료 KPI 부제의 끼니 순서·이름을 여기서 읽는다.
  const mealTypes = useQuery({
    queryKey: ['foodservice', 'meal-types'],
    queryFn: findActiveMealTypes,
  })
  const summary = useQuery({
    queryKey: ['foodservice', 'dashboard-summary', date],
    queryFn: () => findDashboardSummary(date),
    refetchInterval: REFRESH_MS,
  })
  const closing = useQuery({
    queryKey: ['foodservice', 'closing-status', date],
    queryFn: () => findClosingStatus(date),
    refetchInterval: REFRESH_MS,
  })
  const events = useQuery({
    queryKey: ['foodservice', 'recent-events'],
    queryFn: () => findRecentEvents(20),
    refetchInterval: REFRESH_MS,
  })
```
(`REFRESH_MS = 30_000` — `src/views/dashboard/Dashboard.tsx:44`)

```tsx
// src/views/ward-diet-orders/WardDietOrders.tsx:283-290
  const boards = useFreshQueries({
    queries: targetWards.map((w) => ({
      queryKey: ['foodservice', 'meal-order-board', w, baseDate],
      queryFn: () => findMealOrderBoard(w, baseDate),
    })),
  })
  const boardLoading = boards.some((b) => b.isLoading)
  const boardError = boards.find((b) => b.error)?.error ?? null
```

---

## 5. 쓰기(mutation) 샘플 — 세부오더 발행/수정 (POST `/meal-orders` · PUT `/meal-orders/{id}`)

체인: 카드 [저장/발행] 버튼 → `onSave` → `MealOrders.save.mutate({slot, form})` → `useMutation.mutationFn` 이 `reviseMealOrder`/`issueMealOrder` 로 분기 → 성공 시 toast + `setEditing(null)` + `invalidate()` / 409 FS-4091 이면 변경요청 Dialog 로 이어감.

### 5.1 버튼 onClick — 카드의 편집 상태

```tsx
// src/views/meal-orders/MealSlotCard.tsx:121-156
  if (editing && form) {
    return (
      <Card
        title={head}
        action={stateBadge}
        divider
        bodyOverflow="auto"
        className="h-full min-h-0"
        actionArea={
          <Stack direction="row" gap={6} align="center" justify="end" className="w-full">
            <Button variant="gray" size="sm" disabled={saving} onClick={onCancelEdit}>
              닫기
            </Button>
            <Button
              variant="primary"
              size="sm"
              icon={<Check aria-hidden="true" />}
              loading={saving}
              onClick={onSave}
            >
              {cell ? '저장' : '발행'}
            </Button>
          </Stack>
        }
      >
        <OrderFields
          value={form}
          onChange={onFormChange}
          diet={diet}
          formOptions={formOptions}
          mode="order"
          disabled={saving}
        />
      </Card>
    )
  }
```

### 5.2 폼 상태 — `OrderForm` 타입과 초기값 계산 (`mealOrderModel.ts`)

```ts
// src/views/meal-orders/mealOrderModel.ts:92-112
/** 카드 안 편집값 한 벌 — 발행(C-20)과 수정(C-21)이 같은 모양을 쓴다. */
export interface OrderForm {
  mealCode: string | null
  /** 형태 · 염도 — 완제품 · 금식은 null, 자동 발행이 비워 둔 칸도 null 일 수 있다. */
  form: MealFormCode | null
  portion: PortionCode
  salt: SaltLevelCode | null
  isolation: boolean
  guardian: boolean
  guardianPortion: PortionCode
  guardianSalt: SaltLevelCode
  guardianIsolation: boolean
  kcal: number | null
  volume: number | null
  unitQty: number | null
  /** 적용 시작일 — 발행은 `effective_start_date`, 수정은 `effective_from` 으로 나간다. */
  from: string
}

/** 적용 시작일 기본값 — 조회일. **지난 날짜면 오늘**로 보정한다(서버는 과거를 FS-4002 로 거부한다). */
export const applyFromOf = (date: string) => (date < today() ? today() : date)
```

폼 상태 자체는 view 의 `useState`(`MealOrders.tsx:113`) 에 있고, `onFormChange` 가 패치를 머지한다(`:568-570`). 폼 컴포넌트 `OrderFields` 는 제어 컴포넌트로 `value`/`onChange(patch)` 만 받는다:

```tsx
// src/views/meal-orders/OrderFields.tsx:40-62
export function OrderFields({
  value,
  onChange,
  diet,
  formOptions,
  mode,
  disabled = false,
}: {
  value: OrderForm
  onChange: (patch: Partial<OrderForm>) => void
  diet: DietView | undefined
  formOptions: MealFormView[]
  mode: 'order' | 'request'
  disabled?: boolean
}) {
  const meals = activeMeals(diet)
  const axes = axesOf(diet, value.mealCode, formOptions)
  const product = axes?.product ?? false
  const portions = portionOptions(axes)
  const salts = saltOptions(axes)
  // 마스터를 못 읽었으면(축 없음) 형태 선택지는 활성 형태 전부다.
  const forms = axes ? axes.forms : formOptions.filter((f) => f.active)
```

### 5.3 훅(useMutation) — 분기 · 성공 후 재조회 · 409 분기

```tsx
// src/views/meal-orders/MealOrders.tsx:191-195
  function invalidate() {
    for (const key of ['meal-order-board', 'meal-orders', 'preparations', 'closing-board', 'change-requests']) {
      void client.invalidateQueries({ queryKey: ['foodservice', key] })
    }
  }
```

```tsx
// src/views/meal-orders/MealOrders.tsx:222-272
  const save = useMutation({
    mutationFn: async (input: { slot: MealTypeCode; form: OrderForm }) => {
      if (!row?.diet_order_id) throw new Error('식이처방이 없는 환자입니다.')
      const f = input.form
      const axes = axesOf(diet, f.mealCode)
      const cell = row.slots[input.slot] ?? null
      // 축이 없는 값은 보내지 않는다(FS-4005) — 열량·용량·개수는 축이 세부오더에 허용할 때만.
      // 완제품은 형태 · 염도가 없다 — 보내지 않는다.
      const product = axes?.product ?? false
      const attrs = {
        meal_code: f.mealCode ?? undefined,
        meal_form_code: product ? undefined : (f.form ?? undefined),
        portion_code: f.portion,
        salt_level_code: product ? undefined : (f.salt ?? undefined),
        is_isolation_meal: f.isolation,
        is_guardian_meal: f.guardian,
        guardian_portion_code: f.guardianPortion,
        guardian_salt_level_code: f.guardianSalt,
        is_guardian_isolation_meal: f.guardianIsolation,
        kcal: axes?.kcal ? (f.kcal ?? undefined) : undefined,
        volume_ml: axes?.volume ? (f.volume ?? undefined) : undefined,
        unit_qty: axes?.unitQty ? (f.unitQty ?? undefined) : undefined,
      }
      return cell
        ? reviseMealOrder(cell.meal_order_id, { ...attrs, effective_from: f.from })
        : issueMealOrder({
            ...attrs,
            diet_order_id: row.diet_order_id,
            meal_type_code: input.slot,
            effective_start_date: f.from,
            // 발행자 · 수정자는 보내지 않는다 — 서버가 토큰 직원 id 로 기록한다.
          })
    },
    onSuccess: (_r, input) => {
      const issued = !row?.slots[input.slot]
      toast(`${labelOf(input.slot)} 세부오더를 ${issued ? '발행' : '수정'}했습니다 — ${toMonthDay(input.form.from)}부터 적용.`, {
        variant: 'success',
      })
      setEditing(null)
      invalidate()
    },
    onError: (e, input) => {
      toast(describe(e), { variant: 'error' })
      // 마감과 겹쳤다(409 FS-4091) — 같은 값으로 변경요청을 이어서 받는다(응답의 대상 접수자료로).
      const prepId = closedPreparationIdOf(e)
      if (prepId) {
        setEditing(null)
        openRequest('CHANGE', input.slot, { preparationId: prepId, initial: input.form })
      }
    },
  })
```

### 5.4 entity api — `reviseMealOrder` (PUT)

```ts
// src/shared/api/foodserviceApi.ts:954-980
/**
 * 세부오더 수정 — 값은 **`effective_from` 날짜부터** 적용된다(지난 날짜는 그대로 남는다).
 * 과거 날짜는 400 FS-4002, 그 날짜 이후에 마감된 끼니가 있으면 409 FS-4091 이다
 * (호출부는 ApiError.isMealClosed 로 변경요청 경로를 안내한다).
 */
export function reviseMealOrder(
  mealOrderId: string,
  body: {
    effective_from: string
    /** 처방의 식이처방 코드의 활성 식이 코드여야 한다(FS-4003). 생략 = 직전 리비전 값을 잇는다. */
    meal_code?: string | undefined
    unit_qty?: number | undefined
    meal_form_code?: MealFormCode | undefined
    portion_code?: PortionCode | undefined
    salt_level_code?: SaltLevelCode | undefined
    kcal?: number | undefined
    volume_ml?: number | undefined
    is_isolation_meal?: boolean | undefined
    is_guardian_meal?: boolean | undefined
    guardian_portion_code?: PortionCode | undefined
    guardian_salt_level_code?: SaltLevelCode | undefined
    is_guardian_isolation_meal?: boolean | undefined
  },
) {
  // 수정자는 본문으로 보내지 않는다 — 서버가 토큰 직원 id 로 기록한다.
  return request<MealOrderView>(`/meal-orders/${mealOrderId}`, 'PUT', body)
}
```

### 5.5 에러 → 다음 경로 (`closedPreparationIdOf` · `describe`)

```ts
// src/views/meal-orders/mealOrderModel.ts:220-233
/** 마감 후 수정 시도(FS-4091)면 응답이 막힌 접수자료 id 를 준다 — 변경요청 대상으로 그대로 쓴다. */
export function closedPreparationIdOf(error: unknown): string | null {
  if (!(error instanceof ApiError) || !error.isMealClosed) return null
  const id = error.context['meal_preparation_id']
  return typeof id === 'string' ? id : null
}

/** 에러 → 토스트 문구. 마감(FS-4091)은 변경요청 경로를 함께 안내한다. */
export function describe(error: unknown): string {
  if (error instanceof ApiError && error.isMealClosed) {
    return `${error.message} 변경요청으로 등록하세요.`
  }
  return error instanceof Error ? error.message : '처리에 실패했습니다.'
}
```

### 5.6 폼 검증 방식 — 세 가지가 공존한다

**(a) 선택지를 축으로 미리 좁힌다(서버 FS-4005 를 피함)** — `OrderFields.tsx:31-38` 주석 + `:83-91`:
```tsx
// src/views/meal-orders/OrderFields.tsx:82-103
        {product ? null : (
          <Select
            label="형태"
            size="md"
            placeholder="—"
            options={forms.map((f) => ({ value: f.meal_form_code, label: f.meal_form_name }))}
            value={value.form ?? ''}
            onValueChange={(v) => onChange({ form: v })}
            disabled={disabled || forms.length === 0 || (forms.length < 2 && value.form != null)}
          />
        )}
        {axes?.unitQty ? (
          <NumberInput
            label="개수"
            size="md"
            value={value.unitQty}
            onChange={(v) => onChange({ unitQty: v })}
            {...rangeOf(axes.unitQty)}
            suffix="개"
            disabled={disabled}
          />
        ) : null}
```

**(b) 제출 버튼 `disabled` 로 막는다** — `ChangeRequestDialog.tsx:99-102, 137-143`:
```tsx
// src/views/meal-orders/ChangeRequestDialog.tsx:99-102
  const detail = draft.type === 'CANCEL' ? {} : detailOf(draft.base, form, diet)
  const noTarget = draft.type === 'ADD' ? !draft.baseOrderId : !draft.preparationId
  // 변경요청은 바꿀 값이 있어야 뜻이 있다 — 비운 변경요청은 영양팀이 확인할 것이 없다.
  const noChange = draft.type === 'CHANGE' && Object.keys(detail).length === 0
```
```tsx
// src/views/meal-orders/ChangeRequestDialog.tsx:137-143
          <Button
            variant="primary"
            icon={<Send aria-hidden="true" />}
            loading={submit.isPending}
            disabled={noTarget || noChange}
            onClick={() => submit.mutate()}
          >
```
같은 방식: `RestoreDialog.tsx:44-47` (`disabled={!trimmed}`).

**(c) 제출 직전 함수 검증 → toast** — `DietMeals.tsx:152-172` 의 `validate()` 를 `saveAll()` 이 먼저 부른다(`:530-535`):
```ts
// src/views/diet-meals/DietMeals.tsx:152-166
function validate(targets: DietDraft[], all: DietDraft[]): string | null {
  for (const d of targets) {
    const label = d.diet_name.trim() || d.diet_code
    if (d.diet_id == null) {
      if (!CODE_PATTERN.test(d.diet_code)) {
        return `식이처방 코드 「${d.diet_code}」 — 대문자·숫자·_ 64자 이내로 입력하세요.`
      }
      if (all.some((o) => o !== d && o.diet_code === d.diet_code)) {
        return `식이처방 코드 「${d.diet_code}」 가 이미 있습니다.`
      }
    }
    if (!d.diet_name.trim()) return `식이처방 「${d.diet_code}」 의 식이처방명을 입력하세요.`
    if (d.diet_category_code === 'NPO' && d.meals.length > 0) {
      return `「${label}」 — 금식 식이처방에는 식이 코드를 둘 수 없습니다.`
    }
```

### 5.7 useMutation 없이 쓰는 변형 — `DietMeals.saveAll()` (낙관락 409 처리 포함)

```ts
// src/views/diet-meals/DietMeals.tsx:528-536
  async function saveAll() {
    if (!draft) return
    const targets = draft.diets.filter((d) => draft.dirty.has(d.key))
    const problem = validate(targets, draft.diets)
    if (problem) {
      toast(problem, { variant: 'error' })
      return
    }
    setSaving(true)
```
```ts
// src/views/diet-meals/DietMeals.tsx:568-595
        const saved = await saveDiet(dietId, {
          version,
          diet_name: d.diet_name.trim(),
          diet_category_code: d.diet_category_code,
          sort_order: d.sort_order,
          is_active: d.is_active,
          meals: d.meals.map(toMealBody),
        })
        settle(d.key, { ...toDietDraft(saved), key: d.key }, false)
      }
      await dietsQuery.refetch()
      setDraft(null)
      const createdSelected = selected ? createdIds.get(selected.key) : undefined
      if (createdSelected) setSelectedKey(createdSelected)
      toast(`식이처방 ${targets.length}건을 저장했습니다.`, { variant: 'success' })
    } catch (e) {
      if (e instanceof ApiError && e.isVersionConflict) {
        toast('다른 사용자가 먼저 변경했습니다 — 다시 불러왔습니다', { variant: 'error' })
        await dietsQuery.refetch()
        setDraft(null)
      } else {
        // FS-4001(입력 규칙) · FS-4004(금식 식이처방 식이 코드) · FS-4093(코드 중복) 등 — 서버 문구를 그대로 보인다.
        toast(describe(e), { variant: 'error' })
      }
    } finally {
      setSaving(false)
    }
  }
```

---

## 6. 전역 상태 — zustand 스토어: **없음**

근거:
```
$ grep -rn "zustand\|create(\|getState\|useStore" src
(출력 없음)
```
`zustand` 는 `package.json:27` 에만 있다. 이 remote 에 전역 스토어 · 셀렉터 구독 · `getState()` · 화면 닫힐 때 비우는 코드가 **존재하지 않는다**.

대신 상태는 두 곳에만 있다:
1. **서버 상태 = react-query 캐시**. 키 접두 `['foodservice', …]` 로 통일(§3 표). 새로고침 버튼은 접두 전체 무효화:
   ```tsx
   // src/views/meal-orders/MealOrders.tsx:329-336
           <Button
             variant="gray"
             size="md"
             icon={<RefreshCw aria-hidden="true" />}
             onClick={() => void client.invalidateQueries({ queryKey: ['foodservice'] })}
           >
             새로고침
           </Button>
   ```
   화면이 닫힐 때 비우는 것은 `useFreshQuery` 의 `gcTime: 0`(`src/shared/lib/query_cache.ts:19`) 이 맡는다 — 단, raw `useQuery` 를 쓰는 화면은 host 기본 gcTime 을 따른다.
2. **화면 상태 = 컴포넌트 `useState`**. 예: 편집본 `editing`/`request`/`revokeTarget`(`MealOrders.tsx:113-116`), 마스터 편집본 `draft` + `dirty: Set`(`DietMeals.tsx:530, 538-544`). 화면을 떠나면 컴포넌트 언마운트로 사라진다.

---

## 7. event-bus — 사용처 **없음**

근거:
```
$ grep -rn "event-bus\|publish\|subscribe\|replayLast\|openView\|useViewParams\|SessionPatient\|app-kit\|auth-contract" src
src/routes.tsx:1:import { Outlet, useOutletContext, type RouteObject } from 'react-router'
src/routes.tsx:25:  const ctx = useOutletContext()
src/styles/globals.css:7:/* 공유 UI 패키지(@x-ehr/app-kit — ToastProvider 등)의 클래스도 스캔 대상에 포함.
src/styles/globals.css:9:@source "../../../../node_modules/@x-ehr/app-kit";
```
- `@x-ehr/event-bus` 의 module augmentation · `publish`/`subscribe`/`replayLast` · 환자 세션(host store · `SessionPatient`) 수신 · `openView`/`useViewParams` 를 **이 remote 는 쓰지 않는다**(`@x-ehr/event-bus` · `@x-ehr/app-kit` 은 package.json 에만 있고 import 0건).
- host 와의 접점은 `routes.tsx:24-31` 의 `useOutletContext()` → `<Outlet context={ctx}>` 전달뿐이며, 그 ctx(auth)를 읽는 view 도 없다(`grep -rn "usePermission\|useAuthContext\|useOutletContext" src | grep -v routes.tsx` → 출력 없음).
- 환자 선택은 host 환자 세션이 아니라 **화면 안 목록**에서 한다 — `MealOrders.tsx:383-392` 의 킷 `List selectionMode="single"` + `encounterId` 상태(`:111, 162`).
- 화면 간 이동도 코드에 없다(`useNavigate` 는 `Standalone.tsx` 만). 메뉴는 host 소유(`routes.tsx:33-34`).

---

## 8. mock — mock 모드 전환 **없음**

근거:
```
$ grep -rn "mock\|dev-mocks\|MOCK" src
src/app/Standalone.tsx:1:import { createMockAuthContext } from '@x-ehr/dev-mocks'
src/app/Standalone.tsx:22:// Host 없이 단독 개발할 때만 쓰는 에뮬레이터 — mock auth·자체 QueryClient·dev 메뉴를 제공한다(standalone = dev only).
src/app/Standalone.tsx:27:const mockAuth = createMockAuthContext()
src/app/Standalone.tsx:103:        <Outlet context={mockAuth} />
```
- `src/mocks/` 폴더가 없고 API mock 데이터도 없다. mock 은 **auth 컨텍스트 하나**(`@x-ehr/dev-mocks` 의 `createMockAuthContext()`)뿐이며 standalone 에서만 쓰인다.
- 화면 소스가 이를 명시한다:
  ```ts
  // src/views/ward-diet-orders/WardDietOrders.tsx:91-93
  // 병동별 식이오더 발행 (시안 `_docs/views/ward-diet-orders.html`).
  // **실 API 전용** — 목업 데이터가 없다. 처방은 Kafka `{환경}.{테넌트id}.diet-order` 수신분이고,
  // 이 화면은 그 처방을 끼니로 세분화(발행·수정·취소)한다.
  ```
- 그래서 standalone 은 API 를 흉내 내지 않고 **로컬 백엔드로 프록시**한다(`rspack.config.js:25-43`, `BE_PORT` 기본 5343). 백엔드 없이 띄우면 조회가 전부 실패해 각 화면의 `Banner variant="error"` 가 뜬다.

---

## 9. 디자인 시스템 (`@x-ehr/design-system` 0.0.34)

### 9.1 `DataGrid` — 컬럼 정의 + 렌더 (`Preparation.tsx`)

```tsx
// src/views/preparation/Preparation.tsx:263-287
  const columns = useMemo<ColumnDef<MealPreparationView>[]>(
    () => [
      { id: 'ward_id', header: '병동', width: 90, cell: ({ row }) => wardNameOf(row.ward_id) },
      {
        id: 'room_id',
        header: '병실',
        width: 80,
        cellClassName: () => 'nut-nums',
        cell: ({ row }) => roomText(row),
      },
      {
        id: 'patient_name',
        header: '환자',
        width: 100,
        cell: ({ row }) => (
          <span className="typo-label-md-bold">{row.patient.patient_name ?? '—'}</span>
        ),
      },
      {
        id: 'patient_no',
        header: '등록번호',
        width: 100,
        cellClassName: () => 'nut-nums',
        cell: ({ row }) => row.patient.patient_no ?? '—',
      },
```
```tsx
// src/views/preparation/Preparation.tsx:609-628
            <DataGrid
              rows={rows}
              columns={columns}
              getRowId={(r) => r.meal_preparation_id}
              height="100%"
              columnDividers={false}
              selectionMode={false}
              locale="ko-KR"
              aria-label="접수자료 목록"
              emptyVariant="keep-header"
              empty={
                <EmptyState
                  size="sm"
                  title={
                    loadError ??
                    (list.isLoading ? '불러오는 중…' : '조건에 맞는 접수자료가 없습니다.')
                  }
                />
              }
            />
```

### 9.2 `Dialog` + `DialogClose` + `ButtonGroup` + `TextField` — 취소 확인창 (`MealOrders.tsx`)

```tsx
// src/views/meal-orders/MealOrders.tsx:610-640
      {/* 세부오더 취소(C-22) — 되돌릴 수 없어 확인을 받는다(시안 L9). 사유는 선택. */}
      <Dialog
        open={!!revokeTarget}
        onClose={() => setRevokeTarget(null)}
        title={`${revokeTarget ? labelOf(revokeTarget.slot) : ''} 세부오더 취소`}
        description="이 끼니의 세부오더와 마감 전 접수자료가 함께 취소됩니다. 되돌릴 수 없습니다."
        footer={
          <ButtonGroup>
            <DialogClose>
              <Button variant="gray">닫기</Button>
            </DialogClose>
            <Button
              variant="error"
              icon={<X aria-hidden="true" />}
              loading={revoke.isPending}
              onClick={() => revokeTarget && revoke.mutate({ ...revokeTarget, reason: revokeReason.trim() })}
            >
              세부오더 취소
            </Button>
          </ButtonGroup>
        }
      >
        <TextField
          label="취소 사유 (선택)"
          size="md"
          placeholder="예: 식사 불필요"
          value={revokeReason}
          onChange={(e) => setRevokeReason(e.target.value)}
          maxLength={500}
        />
      </Dialog>
```

### 9.3 `Button` — `MealSlotCard.tsx:245-249` (§4.7 인용) · 아이콘은 `@x-ehr/design-system/icons` 만(`MealSlotCard.tsx:13`).

### 9.4 `Select` · `DatePicker` (툴바)

```tsx
// src/views/meal-orders/MealOrders.tsx:301-328
      <ScreenToolbar>
        <div className="w-[216px]">
          <DatePicker
            label="조회일"
            labelPlacement="start"
            size="md"
            clearable={false}
            value={toPickerDate(date)}
            onValueChange={(v) => {
              if (v) setDate(fromPickerDate(v))
            }}
          />
        </div>
        <div className="w-[220px]">
          <Select
            label="병동"
            labelPlacement="start"
            size="md"
            disabled={wardMasterEmpty}
            placeholder="병동 없음"
            options={(wards.data ?? []).map((w) => ({ value: w.ward_id, label: w.ward_name }))}
            value={ward}
            onValueChange={(v) => {
              setWardId(v)
              setEncounterId('')
            }}
          />
        </div>
```

### 9.5 `Stack` / `Grid` 프리미티브 + semantic 토큰 class

```tsx
// src/views/meal-orders/MealOrders.tsx:298-300
  return (
    <Stack gap={8} className="h-full min-h-0 p-(--spacing-8) typo-body-md-regular text-(--color-foreground)">
      {/* ① 조회 툴바 */}
```
```tsx
// src/views/meal-orders/MealOrders.tsx:339-340
      <Stack.Item grow className="min-h-0">
        <Grid columns={['300px', 'minmax(0,1fr)']} rows="minmax(0,1fr)" gap={8} className="h-full min-h-0">
```
```tsx
// src/shared/ui/ScreenToolbar.tsx:21-32
  return (
    <Stack
      direction="row"
      gap={12}
      align="center"
      wrap
      bordered
      radius="lg"
      role="group"
      aria-label={ariaLabel}
      className="shrink-0 bg-(--color-surface) shadow-(--shadow-sm) px-(--spacing-12) py-(--spacing-6)"
    >
```
토큰 문법 예: `px-(--spacing-12)` · `rounded-(--radius-full)` · `text-(--color-foreground-secondary)` · `size-(--size-dot-sm)` · `typo-caption-sm-regular`(`MealOrders.tsx:98-100, 416-420`). 간격 토큰은 `--spacing-{0,2,4,6,8,10,12,16,20,24,32,40,48}` 만 있고 `gap={3}` 같은 값은 **에러 없이 0** 이 된다(`../../CLAUDE.md:32-34`).

### 9.6 킷에 없는 것 — 네이티브 `<button>` 예외와 기록

```tsx
// src/views/ward-diet-orders/WardDietOrders.tsx:1604-1609
  return (
    // uikit-check: ignore-next-line
    <button type="button" className="wdo-stat" aria-pressed={active} onClick={onClick}>
      <Statistic label={label} value={value} suffix="명" size="md" />
    </button>
  )
```
사유·대체는 `_docs/design/design-system-gaps.md`(22개 절) 에 기록돼 있고(`globals.css:78-79` 주석 §5), `yarn check:screens` 는 `uikit-check: ignore-next-line` 로 그 줄만 예외 처리한다.

### 9.7 `styles/globals.css`

```css
/* src/styles/globals.css:1-28 */
/* Remote — 디자인 SOT = @x-ehr/design-system. 디자인시스템 CSS(토큰+컴포넌트) 위에 @theme inline 브리지로
 * 기존 @x-ehr 유틸 이름을 디자인시스템 전역 변수에 연결한다(값 SOT=디자인시스템). */
@import 'tailwindcss';
@import '@x-ehr/design-system/styles';

@source "../";
/* 공유 UI 패키지(@x-ehr/app-kit — ToastProvider 등)의 클래스도 스캔 대상에 포함.
 * 분리 레포에선 워크스페이스 packages/ 가 없으므로 Nexus 설치본(node_modules)을 스캔한다. */
@source "../../../../node_modules/@x-ehr/app-kit";

/* class 기반 다크모드 */
@custom-variant dark (&:where(.dark, .dark *));

/* @theme inline — 유틸에 디자인시스템 var 를 인라인(순환참조 방지). @x-ehr 유틸 이름 → 디자인시스템 변수 매핑. */
@theme inline {
  --color-primary: var(--color-primary);
  --color-surface: var(--color-surface);
  --color-error: var(--color-error);
  --color-success: var(--color-success);
  --color-warning: var(--color-warning);
  --color-text-main: var(--color-foreground);
  --color-text-muted: var(--color-foreground-secondary);
  --color-background-main: var(--color-background);
  --color-border-color: var(--color-border);
  --color-danger: var(--color-error);
  --color-skeleton: var(--color-surface-muted);
  --color-skeleton-highlight: var(--color-surface);
}
```
```css
/* src/styles/globals.css:52-76 */
/* remote 높이 체인 — routes 의 <RemoteScope> 래퍼(div.x-remote-root.khds-root)에 붙는다.
 * `display: contents` 로 **박스를 만들지 않아야** 화면의 `h-full` 이 래퍼(auto)가 아니라
 * 그 위(host <main> / standalone pane)의 확정 높이를 잡는다. 박스를 만들면 높이가 auto 로 끊겨
 * 카드 내부 스크롤 대신 화면이 아래로 넘친다(host·standalone 공통).
 * 디자인시스템의 base 리셋은 `.khds-root *` 후손 선택자라 display:contents 여도 그대로 적용된다. */
.x-remote-root {
  display: contents;
  /* 상태 점 크기 — 킷 `theme.css` 에는 있지만 `/styles`(khds.css) 가 방출하지 않는 토큰이라, 킷 정의값(6px) 그대로
   * remote 범위에 둔다. 없으면 `size-(--size-dot-sm)` 점이 에러 없이 0px 로 사라진다(`_docs/design/design-system-gaps.md` §22).
   * custom property 는 display:contents 래퍼에서도 후손에게 상속된다. */
  --size-dot-sm: 6px;
}

/* ── 화면 골격 — 킷 컴포넌트로 표현되지 않는 것만 남긴다(컨트롤 스타일은 전부 킷) ──────────── */

/* 표를 담는 카드 — 킷 `Card`(divider) 의 본문 여백을 걷어 표가 카드 가장자리까지 닿게 한다(영양 BC 공통 화면 틀 F3).
 * card.md §8 치수 변수 `--card-body-pad` 를 화면군 한 곳에서 선언하는 용도 그대로다. 카드 안 필터 줄은 `PanelBar`. */
.nut-flush {
  --card-body-pad: 0px;
}

/* 숫자는 표 정렬을 위해 고정폭 숫자로 — typography.md "테이블/목록 내 숫자 + tabular-nums" */
.nut-nums {
  font-variant-numeric: tabular-nums;
}
```
나머지(`:78-276`)는 병동오더 발행 화면 전용 `.wdo-*` 골격 CSS(4주 달력 칸 · 현황 버튼 · 끼니 줄)다.

### 9.8 공용 화면 틀 컴포넌트 — `PanelBar` / `PanelFooter`

```tsx
// src/shared/ui/PanelBar.tsx:8-20
export function PanelBar({ children, className }: { children: ReactNode; className?: string }) {
  return (
    <Stack
      direction="row"
      gap={6}
      align="center"
      wrap
      className={`shrink-0 border-b border-(--color-border-secondary) px-(--spacing-12) py-(--spacing-8) ${className ?? ''}`}
    >
      {children}
    </Stack>
  )
}
```

---

## 10. i18n — 번역 로딩 · `t()` 사용: **없음**

근거:
```
$ grep -rn "i18n\|useTranslation\|\bt(" src
src/app/Standalone.tsx:23:// 운영(Host nest)에선 Host 의 Provider·authContext·단일 QueryClient·단일 i18next 를 쓰고 **메뉴도 host 가 소유**한다
src/shared/ui/date.ts:4: * 사용자 표시 포맷이 필요해지면 `@x-ehr/i18n` 을 쓴다(직접 Intl/toLocale 금지).
src/shared/ui/date.ts:21: * `@x-ehr/i18n` 의 `useFormat` 은 host 포맷 컨텍스트가 없으면 throw 해 standalone 에서 쓸 수 없어 여기서 푼다.
$ find frontend -maxdepth 5 -type d -name "i18n" -not -path "*/node_modules/*"
(출력 없음)
```
- `public/i18n/v1/ko-KR` 디렉터리가 **없다**. 모든 문구는 소스에 한국어 리터럴로 박혀 있다(예: `codeLabels.ts` 의 코드→한글 표, Dialog 제목·설명).
- `i18next` · `react-i18next` · `@x-ehr/i18n` 은 package.json 에만 있고 import 0건. 루트 스크립트 `i18n:check` 는 `--allow-empty` 로 돈다(`../../package.json:19`).
- 날짜 포맷은 `@x-ehr/i18n` 대신 자체 헬퍼로 푼다(사유 주석 포함):
  ```ts
  // src/shared/ui/date.ts:19-29
  /**
   * ISO 시각 → `YYYY-MM-DD HH:mm`(브라우저 로컬 시각). 값을 바꾸지 않고 표기만 한다 — UTC(`Z`)로 와도 로컬로 읽는다.
   * `@x-ehr/i18n` 의 `useFormat` 은 host 포맷 컨텍스트가 없으면 throw 해 standalone 에서 쓸 수 없어 여기서 푼다.
   */
  export function formatInstant(iso: string | null | undefined): string | null {
    if (!iso) return null
    const d = new Date(iso)
    if (Number.isNaN(d.getTime())) return iso
    const offset = d.getTimezoneOffset() * 60_000
    return new Date(d.getTime() - offset).toISOString().slice(0, 16).replace('T', ' ')
  }
  ```
- 코드 → 표시명 표(번역 파일 대용):
  ```ts
  // src/shared/ui/codeLabels.ts:10-20
  // 서버 코드 → 화면 표시 이름. 화면은 코드를 그대로 찍지 않는다(시안의 한글 칩과 같게).
  // 모르는 코드가 오면 코드를 그대로 보인다 — 새 값이 조용히 사라지지 않게.

  export const PREPARATION_STATUS_LABEL: Record<PreparationStatus, string> = {
    REQUESTED: '접수',
    ACCEPTED: '마감확정',
    IN_PROGRESS: '조제중',
    COMPLETED: '배식완료',
    CANCELLED: '취소',
    NOT_SERVED: '결식',
  }
  ```

---

## 11. 컴포넌트 트리 — 「세부오더 관리」(`meal-orders`)

```
<RemoteScope>  (routes.tsx:24-31, div.x-remote-root.khds-root, Outlet context=auth)
└─ <MealOrders>  (views/meal-orders/MealOrders.tsx:105)
   │  state: date · wardId · keyword · encounterId · editing{key,slot,form} · request{key,draft} · revokeTarget · revokeReason
   │  queries: wards · board(ward,date) · closingBoard(date) · preparations(date,ward) · mealForms · diets(active) · dietOrders(ward,patientNo) · mealOrders(diet_order_id)
   │  mutations: save(issue|revise) · revoke
   ├─ <Stack gap=8 h-full>
   │  ├─ <ScreenToolbar>  (shared/ui/ScreenToolbar.tsx)
   │  │  ├─ <DatePicker value=toPickerDate(date) onValueChange→setDate>
   │  │  ├─ <Select options=wards.data value=ward onValueChange→setWardId+setEncounterId('')>
   │  │  └─ <Button 새로고침 onClick→invalidateQueries(['foodservice'])>
   │  └─ <Stack.Item grow> <Grid columns=['300px','minmax(0,1fr)']>
   │     ├─ <Card title="재원 환자" .nut-flush action={rows.length}명>
   │     │  ├─ <PanelBar> <TextField keyword onChange→setKeyword/>
   │     │  └─ <List selectionMode="single" value=row.encounter_id onValueChange→setEncounterId items=rows>
   │     │     └─ (r,{selected}) → <List.Item> <Stack bordered class=rowCardClass(selected)> 이름·등록번호·병실 / 식종 점·배지(확인 필요 | 처방 없음 | 금식 | 발행 n/m)
   │     └─ <Stack gap=8>
   │        ├─ <Card 선택 환자>  이름 · sexAge(row.patient) · 식종 Badge · 처방상태 Badge
   │        │  └─ <Descriptions columns=4 items=[진료과,주치의,입원일,조회일, 의사 처방=prescriptionSummary(rx)]>
   │        ├─ <Banner warning|error>  (처방 없음 / 금식 / 허용 끼니 없음 — 조건부)
   │        └─ <Grid columns=min(visibleSlots,4)>
   │           └─ visibleSlots.map(slot) → <MealSlotCard  (views/meal-orders/MealSlotCard.tsx:53)
   │                 props: label=labelOf(slot) · servingTime · cell=row.slots[slot] · wardClosing=wardClosingOf(slot) · canAdd
   │                        editing · form=editing.form · saving · busy · diet · formOptions · formNameOf · revisions
   │                        onFormChange(patch)→setEditing merge · onEdit→setEditing(formOf(cell,diet,date,rx))
   │                        onSave→save.mutate({slot,form}) · onRevoke→setRevokeTarget · onRequest(type)→openRequest
   │                 ├─ [editing] <Card actionArea=[닫기][저장|발행]> <OrderFields value=form onChange mode="order"/>
   │                 │     └─ <Select 식이 코드> <Grid 2열: Select 형태 · NumberInput 개수 · Select 양 · Select 염도 · NumberInput 열량/용량>
   │                 │        <Checkbox 격리> <Divider/> <Checkbox 보호자식> [<Grid 보호자식 양/간/격리>] <DatePicker 적용 시작일 minDate=today>
   │                 ├─ [!cell] <Card> <EmptyState 미발행 image=imgSalad/> actionArea=[발행][추가요청(마감 시)]
   │                 └─ [cell]  <Card actionArea= allowed_actions ⊂ {REVISE,REVOKE,REQUEST_CHANGE,REQUEST_CANCEL} + addable>
   │                       <Badge 발행> <Badge 조제상태 PREP_COLOR> [<Badge 확인 필요 title=missingText>] "{revision_effective_from}부터 적용"
   │                       <Descriptions items=[식이 코드|규격, 형태, 양 조절, 염도, 격리, 개수, 열량, 용량, 보호자식]/>
   │                       <Divider/> 적용 이력: revisions.map → <Grid ['96px','1fr']> 기간 · revisionText(prev,cur)
   ├─ [activeRequest] <ChangeRequestDialog key=type|slot|prepId draft row date diet formOptions labelOf onClose onDone→invalidate>
   │     state: form(OrderForm)=draft.initial · reason ; mutation: submit→createChangeRequest
   │     └─ <Dialog footer=[닫기][요청 등록 disabled=noTarget||noChange]> <Descriptions 대상/기준/종류> [<Banner error>] [<OrderFields mode="request">] <TextArea 사유>
   └─ <Dialog 세부오더 취소 open=!!revokeTarget footer=[닫기][세부오더 취소 → revoke.mutate]> <TextField 취소 사유>
```

---

## 12. 이 레포만의 특징 패턴

1. **2레이어 FSD(views + shared)** — widgets/features/entities 없이 view 가 `shared/api` 를 직접 부른다. 화면 전용 계산은 같은 폴더의 `*Model.ts`(`mealOrderModel.ts`) 로 뺀다. 교차 화면 공용 계산은 `shared/lib`(`dietAttributes.ts`).
2. **API 클라이언트 한 파일** — `foodserviceApi.ts` 에 타입 + 40개 함수 + `ApiError` + `request()`/`query()` 가 전부 있다. REST JSON 은 snake_case 그대로(변환 금지, `:1-3`). 행위자(발행자·요청자)는 본문에 싣지 않는다(서버가 토큰에서 — `:950, 978, 1179`).
3. **서버가 준 `allowed_actions` 로만 버튼 노출** — 상태 전이 규칙을 FE 에 중복 구현하지 않는다(`MealSlotCard.tsx:244-281`). 예외 하나(「추가요청」)는 설계미비로 주석에 남긴다(`:48-51`).
4. **에러 코드 → 다음 경로**: `ApiError.isMealClosed`(FS-4091) 이면 응답 `context.meal_preparation_id` 로 변경요청 Dialog 를 이어 연다(`MealOrders.tsx:263-271`); `isVersionConflict`(FS-4094) 면 refetch 후 편집본 폐기(`DietMeals.tsx:584-587`).
5. **react-query 키 접두 `['foodservice', …]` 통일** + 화면별 `invalidate()` 묶음(`MealOrders.tsx:191-195`) + 툴바 새로고침은 접두 전체 무효화.
6. **공통 화면 틀 F1~F4** — 제목 줄 없음, 조회 조건은 `ScreenToolbar` 한 줄, 표 카드는 `.nut-flush`, 카드 안 필터는 `PanelBar`, 하단 요약은 `PanelFooter`(`../../CLAUDE.md:57-60`).
7. **끼니 코드는 하드코딩하지 않는다** — `GET /settings/meal-types` 가 원천이고 `DEFAULT_MEAL_TYPES` 는 폴백(`foodserviceApi.ts:13-17, 68-71`). 조절 축(형태·염도·열량…)도 마스터 `DietView.attributes` 로 선택지를 좁힌다(`OrderFields.tsx:31-38`).
8. **표시 포맷 자체 헬퍼** — `@x-ehr/i18n useFormat` 이 standalone 에서 throw 해 `shared/ui/date.ts` 로 푼다(`:19-22`). CSV 는 BOM 붙여 내려받는다(`csv.ts:8-22`).
9. **standalone 은 프록시 기반** — mock 데이터 없이 `rspack.config.js` 가 `/api/v1/foodservice` 를 로컬 BE 로 넘긴다(pathRewrite 없음).
10. **HTML 시안 우선** — 각 view 머리 주석이 `_docs/views/<화면>.html` 과 시안 라벨(L4, M7 …)을 가리킨다(`MealOrders.tsx:57-66`, `WardDietOrders.tsx:91-112`).

---

## 13. 신입이 헷갈릴 포인트 10개

1. **`useFreshQuery` 와 raw `useQuery` 가 섞여 있다** — CLAUDE.md 는 staleTime 0 래퍼만 쓰라 하지만 dashboard · closing · preparation · aggregation · change-requests · not-served · statistics 는 `@tanstack/react-query` 의 `useQuery` 를 직접 쓴다(`grep -rn "from '@tanstack/react-query'" src`). `ChangeRequests.tsx:95-100` 은 오히려 `staleTime: 600_000` 을 준다. 어느 쪽을 따를지 팀 결정을 확인할 것.
2. **`widgets/`·`features/`·`entities/` 가 없다** — CLAUDE.md §0 의 레이어 그림과 다르다. 새 코드를 어디에 둘지는 기존 관례(`views/<screen>/` + `shared/`)를 따른다(§1.1).
3. **`zustand`·`@x-ehr/event-bus`·`i18next` 가 package.json 에 있지만 안 쓴다** — import 0건(§6 · §7 · §10). 의존성 목록만 보고 전역 스토어/이벤트버스를 찾지 말 것.
4. **standalone 과 host 가 다른 Provider 를 가진다** — `Standalone.tsx:144-159` 의 `QueryClientProvider`/`ToastProvider`/`DragProvider` 는 dev 전용이고, 운영에선 host 가 준다. `useToast` 를 Provider 밖에서 부르면 throw(`:146-147`).
5. **`.x-remote-root` 는 `display: contents`** 라 박스가 없다 — 높이 체인은 `h-full min-h-0` 으로 부모(host `<main>`/standalone pane)의 확정 높이를 잡는다(`globals.css:52-63`, `Standalone.tsx:58-61`). 래퍼에 패딩·배경을 주면 안 된다.
6. **간격 토큰 밖 숫자는 조용히 0** — `gap={3}` · `p-(--spacing-1)` 은 CSS 가 안 생긴다(`../../CLAUDE.md:32-34`). `--size-dot-sm` 처럼 킷이 방출하지 않는 토큰은 remote 가 직접 선언했다(`globals.css:59-62`).
7. **세부오더 수정은 「적용 시작일부터」** — PUT 은 `effective_from` 을 싣고 과거 날짜는 FS-4002(`foodserviceApi.ts:954-958`), 기본값은 `applyFromOf(date)` 가 오늘로 보정한다(`mealOrderModel.ts:111-112`). 칸(`MealOrderSlotView`)의 값은 "조회한 날짜의 리비전"(`:331`)이라 날짜를 바꾸면 값이 달라진다.
8. **409 FS-4091 은 실패가 아니라 분기** — `save.onError` 가 `closedPreparationIdOf(e)` 로 접수자료 id 를 꺼내 변경요청 Dialog 를 연다(`MealOrders.tsx:263-271`). 토스트만 보고 끝내면 안 된다.
9. **마스터 편집(DietMeals)은 `useMutation` 이 아니라 `async saveAll()`** 이다 — 여러 건을 순차 저장하며 성공분을 편집본에 즉시 반영해 재전송 409 를 막는다(`DietMeals.tsx:537-544`). 다른 화면의 `useMutation` 패턴과 다르다.
10. **standalone 은 백엔드가 있어야 화면이 찬다** — mock API 가 없고 `rspack.config.js:25-43` 프록시가 `127.0.0.1:${BE_PORT}`(기본 5343) 로 넘긴다. `BE_PORT` 를 안 맞추면 모든 조회가 `Banner error` 로 뜬다. 또 API 함수 중 `saveMealTypeSettings`·`saveClosingRoundSettings` 는 호출 화면이 없어(설정 화면 미구현) 코드만 보고 기능이 있다고 오해하지 말 것.
