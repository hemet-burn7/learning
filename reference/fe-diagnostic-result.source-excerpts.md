# diagnostic-result Frontend 소스 수집 (신입 교육 자료용)

- 대상: `x-diagnostic-result/frontend/apps/diagnostic-result`
- 아래 인용의 경로는 모두 이 앱 루트 기준 상대경로(`src/...`)이며, `경로:시작줄-끝줄` 은 실제 파일 줄 번호다. 인용은 소스를 그대로 옮겼다.
- 사전 확인 사실(추측 아님):
  - **react-query 를 화면 코드가 쓰지 않는다.** `@tanstack/react-query` import 는 `src/app/Standalone.tsx:17` 의 `QueryClientProvider` 한 곳뿐이다(dev 셸). `useQuery` · `useMutation` · `staleTime` · `useFreshQuery` · `shared/lib/query_cache.ts` 는 이 레포에 없다. 조회는 전부 `useEffect` + `useState` 기반 커스텀 훅(`useRangedObservations` · `useAnalysis` 등)이다.
  - **i18n 번역 파일 · `t()` 호출이 없다.** `public/` 에는 `index.html` 하나뿐이고 `public/i18n/` 폴더가 없다. `@x-ehr/i18n` 은 `FormatContextProvider`(병원 시간대) · `instantParts` 두 용도로만 쓴다.
  - **AI 스트림(SSE)은 없다.** `grep stream|EventSource|SSE` 결과 0건. AI 분석은 POST 1회 + 시간 초과 시 폴링이다(§12).

---

## 1. 폴더 트리 · 패키지 · 빌드/린트 설정

### 1.1 `find src -type f | sort` — FSD 레이어별 (총 125파일, `.DS_Store` 제외 124)

| 레이어 | 파일 수 | 파일 |
|---|---|---|
| 루트 | 4 | `src/index.ts` · `src/bootstrap.tsx` · `src/routes.tsx` · `src/global.d.ts` |
| app (dev 전용) | 1 | `src/app/Standalone.tsx` |
| views | 6 | `views/diagnostic-result-view/DiagnosticResultView.tsx` · `views/summary/ReportSummary.tsx` · `views/patient-list/PatientList.tsx` · `views/poc-test/PocTest.tsx` · `views/poc-test/agentCommand.ts` · `views/poc-test/aiAgent.ts` |
| widgets | 55 | `widgets/diagnostic-result/` (DetailDialog.tsx · ResultDetail.tsx · categoryTree.ts · detailStore.ts · periodStore.ts · useAgentCommandBridge.ts · useDiagnosticResultData.ts · useWidenOlder.ts) · `widgets/diagnostic-result/compact/` (CompactMain · DateGroupList · DetailToolbar · RawPanel · ReportPanel · compactStore · useCompactDerive) · `widgets/diagnostic-result/wide/` (AiSection · CategoryTree · CodeFilterBar · DetailPanel · ExternalSourceMark · GraphPanel · ObservationList · ResultChipBar · TrendDetail · WideMain · aiAbnormalItems · externalSourceBrand · listModes · observationListModel · useGridViewport · useWideDerive · wideStore) · `wide/cells/` 12개 · `wide/columns/` 3개(flatColumns · timeColumns · trendColumns) · `wide/report/` (ReportView · reportParts) · `widgets/patient-search/` (PatientsSidebar · usePatientSearch) · `widgets/report-summary/` (KeyResultItem · ReportSummaryWidget · ResultSummaryCard · useReportSummaryData) |
| features | 4 | `features/favorite-set/model/useSetAction.ts` · `features/favorite-set/ui/FavoriteSetManageDialog.tsx` · `FavoriteSetPicker.tsx` · `NewSetRow.tsx` |
| entities | 33 | `entities/analysis/` (api: analysisApi · resultSummaryApi / model: analysis · types · useAnalysis · useResultSummary / ui: AiBrief · AnalysisHistoryDialog · AnalysisWaitingLine · SeverityBadge) · `entities/observation/` (api: observationApi / model: aggregate · constants · dayGroups · flags · format · graphSeries · rangeSteps · reportSections · significance · sort · timeline · types · useRangedObservations / ui: CodeFilterChips · RangeTrack · RecordDocumentFrame · areaIcon · graph-chart · result-visuals) · `entities/viewer-configuration/` (api: viewerConfigurationApi / model: favoriteSets · types) |
| shared | 14 | `shared/api/apiBase.ts` · `shared/api/mockStore.ts` · `shared/lib/events.ts` · `hospitalTime.ts` · `noPatientCopy.ts` · `record.ts` · `useContainerTier.ts` · `useTypedText.ts` · `viewBus.ts` · `shared/patient-session/sessionPatient.ts` · `sessionPatientStandIn.ts` · `shared/store/chartModeStore.ts` · `shared/ui/RemoteScope.tsx` · `shared/ui/code-block.tsx` |
| mocks | 5 | `mocks/patient-results/clinicalSeed.ts` · `observations.json` · `patient.json` · `store.ts` · `viewer-config.json` |
| styles | 2 | `styles/globals.css` · `styles/styles.css` |

### 1.2 `package.json`

```json
// package.json:1-53
{
  "name": "diagnostic-result",
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
    "@x-ehr/event-bus": "~0.0.22",
    "@x-ehr/i18n": "~0.0.12",
    "@x-ehr/design-system": "0.0.34",
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
    "eslint": "^9",
    "prettier": "^3.8.1",
    "@rspack/cli": "^2.0.8",
    "@rspack/core": "^2.0.8",
    "@rspack/dev-server": "^2.1.0",
    "@rspack/plugin-react-refresh": "^2.0.2",
    "@tailwindcss/postcss": "^4.0.0",
    "@types/node": "^24",
    "@types/react": "^19",
    "@types/react-dom": "^19",
    "css-loader": "^7.1.4",
    "postcss": "^8.5.8",
    "postcss-loader": "^8.2.1",
    "react-refresh": "^0.18.0",
    "style-loader": "^4.0.0",
    "tailwindcss": "^4.0.0",
    "typescript": "^5"
  }
}
```

워크스페이스 루트(`frontend/package.json`)의 스크립트:

```json
// ../../package.json:12-23
  "scripts": {
    "dev": "yarn workspace diagnostic-result dev",
    "build": "yarn workspace diagnostic-result build",
    "lint": "eslint apps",
    "typecheck": "yarn workspace diagnostic-result exec tsc --noEmit",
    "check:ts-single": "node node_modules/@x-ehr/eslint-config/scripts/check-single-typescript.cjs",
    "i18n:check": "node node_modules/@x-ehr/i18n/scripts/i18n-check.cjs --allow-empty",
    "check:shared": "yarn workspace diagnostic-result check:shared",
    "format": "prettier --write .",
    "format:check": "prettier --check .",
    "check:screens": "node node_modules/@x-ehr/design-system/scripts/check-screen-usage.mjs apps/diagnostic-result/src"
  },
```

### 1.3 `rspack.config.js`

```js
// rspack.config.js:1-42
const { createRemoteRspackConfig } = require('@x-ehr/rspack-config')

// BC 레포(x-diagnostic-result) 의 MF remote. MF name(remoteKey)=diagnostic_result,
// 메뉴 path base=/diagnostic-result. ./routes 는 factory 가 강제 노출(host seam).
//
// remoteKey·port 는 **불변** — host 는 env.js 의 remoteBaseUrl 로
// `${base}/diagnostic_result/remoteEntry.js` convention 을 조립하므로 이 값이 바뀌면 host 가 못 찾는다.
//
// x-ehr-foundation in-repo 시절의 `alias: ehrSrcAlias()`(워크스페이스 packages/*/src 로 alias)는 제거했다 —
// 분리 레포에선 @x-ehr/* 를 Nexus 배포본(node_modules)에서 해석한다.
const configFn = createRemoteRspackConfig({
  dirname: __dirname,
  name: 'diagnostic_result',
  port: 3351,
  shared: ['@x-ehr/app-kit', '@x-ehr/auth-contract', '@x-ehr/design-system'],
})

// [standalone dev 전용] API 프록시 — 화면은 게이트웨이 계약 경로(/api/v1/diagnostic-result/*)로 호출하지만
// 단독 dev 서버(:3351)엔 백엔드가 없어 SPA fallback(HTML)로 떨어진다("응답 형식이 올바르지 않습니다"의 원인).
// ⚠️ prefix 를 **벗기지 않고 그대로** 백엔드(기본 :5351)로 넘긴다 — 앱 context-path 는 "/" 이고
//    `/api/v1/diagnostic-result` 는 컨트롤러 매핑이 소유하기 때문이다(pathRewrite 를 두면 404).
// (Host(:3000) 경유 실행은 dev.py 의 apiProxy 가 담당 — 이 프록시는 remote standalone 에만 적용.)
// 백엔드 주소는 DR_BACKEND 로 override 가능(기본 http://localhost:5351).
const DR_BACKEND = process.env.DR_BACKEND || 'http://localhost:5351'
const GATEWAY_PREFIX = '/api/v1/diagnostic-result'

// AI 호출은 백엔드 중계(/api/v1/diagnostic-result/ai-chat)라 게이트웨이 프록시가 없다 — URL · API 키는 백엔드 설정에만 있다.

module.exports = (env, argv) => {
  const cfg = configFn(env, argv)
  if (cfg.devServer) {
    cfg.devServer.proxy = [
      ...(Array.isArray(cfg.devServer.proxy) ? cfg.devServer.proxy : []),
      {
        context: [GATEWAY_PREFIX],
        target: DR_BACKEND,
        changeOrigin: true,
      },
    ]
  }
  return cfg
}
```

factory 가 `./routes` 를 강제 노출하는 근거(Nexus 패키지):

```js
// ../../node_modules/@x-ehr/rspack-config/index.js:170-173
          filename: 'remoteEntry.js',
          exposes: { ...exposes, ...(isDev ? devExposes : {}), './routes': './src/routes.tsx' },
          shared: sharedSingletons(shared),
```

### 1.4 ESLint — 워크스페이스 루트 `eslint.config.mjs` (앱 폴더에는 eslint 설정 파일이 없다)

```js
// ../../eslint.config.mjs:1-31
import { existsSync, readdirSync } from 'node:fs'
import { dirname, join } from 'node:path'
import { fileURLToPath } from 'node:url'
import base from '@x-ehr/eslint-config'
import remoteConfig from '@x-ehr/eslint-config/remote'
import mfBuildContract from '@x-ehr/eslint-config/build-contract'
import fsdConfig from '@x-ehr/eslint-config/fsd'

// BC 레포의 frontend 워크스페이스 루트 config.
// remote 앱 자동 발견 — `apps/<dir>/src/routes.tsx`(./routes MF seam)를 가진 앱이 remote.
// (x-ehr-foundation 루트 config 와 동일 규약 — 신규 remote 를 apps/ 에 추가하면 등록 없이 포함된다.)
const APPS_DIR = join(dirname(fileURLToPath(import.meta.url)), 'apps')
const REMOTE_DIRS = readdirSync(APPS_DIR).filter((d) => existsSync(join(APPS_DIR, d, 'src', 'routes.tsx')))

export default [
  ...base,
  // Remote 계약 강제 — 라우터/QueryClient 자체 생성 금지, auth 는 @x-ehr/auth-contract,
  // HTTP 는 @x-ehr/api-axios, 포맷은 @x-ehr/i18n 만. 커밋 전 `yarn lint` 통과 필수.
  //
  // 계약 룰의 스코프는 `{ts,tsx}` 다 — `src/` 아래는 전부 ts 라 사각지대가 없다(.jsx/.js 잔존 0).
  ...remoteConfig({
    files: REMOTE_DIRS.map((d) => `apps/${d}/**/*.{ts,tsx}`),
    standalone: REMOTE_DIRS.map((d) => `apps/${d}/src/app/Standalone.tsx`),
    routes: REMOTE_DIRS.map((d) => `apps/${d}/src/routes.tsx`),
  }),
  // FSD 레이어 경계 — views → widgets → features → entities → shared 하향만, features 슬라이스 간 직접 참조 금지,
  // shared → mocks 금지(mock json 이 운영 번들에 섞인다). foundation 루트 config 와 같은 배선이다.
  ...REMOTE_DIRS.flatMap((d) => fsdConfig({ src: join(APPS_DIR, d, 'src'), files: [`apps/${d}/src/**/*.{ts,tsx}`] })),
  // MF 빌드 계약 — rspack.config.js 의 name/uniqueName · ./routes expose · base URL · shared strictVersion.
  ...mfBuildContract({ files: ['apps/*/rspack.config.js'] }),
]
```

remote 계약 규칙(공유 패키지 `@x-ehr/eslint-config/remote.mjs`)의 핵심 금지 목록 — 자체 axios · 라우터 · QueryClient 생성 금지:

```js
// ../../node_modules/@x-ehr/eslint-config/remote.mjs:33-68
const remoteRestrictedImports = [
  ...baseRestrictedImports,
  {
    name: 'axios',
    // 타입 전용 import 는 인스턴스를 만들지 않아 토큰/single-flight 분열과 무관 → 허용. 값 import(인스턴스 소유)만 차단.
    allowTypeImports: true,
    message:
      'remote 는 자체 axios 인스턴스 금지 — @x-ehr/api-axios 의 api 를 소비하세요. 인스턴스 분열 시 401 refresh·single-flight 가 깨집니다 (ARCHITECTURE.md 금지 패턴).',
  },
  {
    name: 'react-router',
    importNames: [
      'createBrowserRouter',
      'createMemoryRouter',
      'createHashRouter',
      'createStaticRouter',
      'BrowserRouter',
      'MemoryRouter',
      'HashRouter',
      'unstable_HistoryRouter',
      'RouterProvider',
      'StaticRouterProvider',
    ],
    allowTypeImports: true,
    message:
      'remote 는 별도 라우터 생성/마운트 금지 — host 단일 라우터에 ./routes 를 nest 하고 네비게이션은 shared useNavigate/<Link> 를 쓰세요. dev 용 Standalone.tsx 만 예외 (ARCHITECTURE.md 금지 패턴).',
  },
  {
    name: '@tanstack/react-query',
    // 값 import 만 차단 → `new QueryClient()` 와 alias(`QueryClient as QC`) 둘 다 잡힌다. useQuery 등 다른 export 와
    // 타입 주석(useQueryClient 반환 타입 등)은 통과. imperative 접근이 필요하면 향후 escape hatch(ADR).
    importNames: ['QueryClient'],
    allowTypeImports: true,
    message:
      'remote 는 QueryClient 생성 금지 — host 가 주입한 @tanstack/react-query shared singleton context(useQuery 등)를 소비하세요. dev 용 Standalone.tsx 만 예외 (ARCHITECTURE.md 금지 패턴).',
  },
```

```js
// ../../node_modules/@x-ehr/eslint-config/remote.mjs:150
  const noParentRelative = [{ group: ['../**'], message: '상위 경로 상대 import 금지 — @/ 절대경로(@/…) 사용' }];
```

`tsconfig.json` 은 `@/* → ./src/*` 경로 별칭만 추가한다:

```json
// tsconfig.json:1-9
{
  "extends": "../../tsconfig.base.json",
  "compilerOptions": {
    "types": ["node"],
    "paths": {
      "@/*": ["./src/*"]
    }
  },
  "include": ["src/**/*.ts", "src/**/*.tsx"]
}
```

---

## 2. 진입점

### 2.1 `index.ts` → `bootstrap.tsx` (standalone 부팅 전용)

```ts
// src/index.ts:1-2
// MF 비동기 경계 — shared 초기화가 끝난 뒤 standalone 부팅한다.
void import('./bootstrap')
```

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

### 2.2 `routes.tsx` — 유일한 MF expose. 운영 화면 2개(RemoteScope 래퍼 아래)

```tsx
// src/routes.tsx:1-25
import { type RouteObject } from 'react-router'
import RemoteScope from './shared/ui/RemoteScope'
import DiagnosticResultView from './views/diagnostic-result-view/DiagnosticResultView'
import ReportSummary from './views/summary/ReportSummary'
// Host nest 시에도 remote 유틸리티 CSS 가 로드되도록 노출 진입점에서 import.
import './styles/globals.css'

// top-level path 는 Host/메뉴 소유 — remote 는 상대 child route 만 정의한다(absolute path 금지).
// 메뉴 leaf path `/diagnostic-result/<child>` 의 base segment(`/diagnostic-result`)에 host 가 이 routes 를 1회 nest 한다.
// 운영 화면만 둔다 — host 메뉴에 없는 dev 전용 화면(환자목록 · 테스트조회 PoC)은 `app/Standalone.tsx` 가 붙인다(운영 번들에 들어가지 않는다).
// 공통 래퍼(`RemoteScope` — x-remote-root · khds-root · ToastProvider)는 `shared/ui/RemoteScope` 다.
const routes: RouteObject[] = [
  {
    element: <RemoteScope />,
    children: [
      // 검사결과조회 — 받은 폭에 따라 통합조회 · 상세 · 요약.
      // 메뉴 URL = /diagnostic-result/diagnostic-result-view (menuId 는 diagnostic_result_patient 그대로).
      { path: 'diagnostic-result-view', element: <DiagnosticResultView /> },
      // 검사결과 요약(읽기전용 검사일 요약 + AI 브리핑).
      { path: 'summary', element: <ReportSummary /> },
    ],
  },
]

export default routes
```

### 2.3 RemoteScope 래퍼

```tsx
// src/shared/ui/RemoteScope.tsx:1-31
import { ToastProvider } from '@x-ehr/design-system'
import { FormatContextProvider } from '@x-ehr/i18n'
import { useContext } from 'react'
import { Outlet, useOutletContext } from 'react-router'

import { syncHospitalTimeZone } from '@/shared/lib/hospitalTime'

/**
 * remote 공통 래퍼 — 두 가지를 한 자리에서 한다(host/standalone 공통 seam). `routes.tsx` 의 layout route 가 쓴다.
 *   · `x-remote-root` : styles.css 의 수제 마크업 리셋을 이 하위로만 가둔다(host 헤더/배경 보호).
 *   · `khds-root`     : 디자인시스템 CSS 의 base 리셋이 닿는 경계.
 *                       ⚠️ 빼면 킷 컴포넌트가 브라우저 기본 모양으로 풀린다.
 *   · `ToastProvider` : 화면들이 useToast 로 띄우는 피드백의 마운트 지점(자체 토스트 DOM 금지).
 *   · 병원 시간대     : host 가 주는 병원 시간대를 화면의 「오늘」 기준(`shared/lib/hospitalTime`)으로 옮긴다.
 *                       하위 화면이 첫 렌더에서 「오늘」을 셀 수 있게 렌더 중에 맞춘다(같은 값 대입이라 반복해도 된다).
 *                       Provider 가 없으면(standalone) 기본값 서울 — `useI18nFormatContext` 는 Provider 밖에서 throw 해 쓰지 않는다.
 * display:contents 라 레이아웃 박스를 만들지 않는다 — 박스를 만들면 화면 높이 체인이 거기서 끊긴다.
 * 부모(host <main>/standalone shell)의 outlet context(auth)는 그대로 하위 화면에 전달한다.
 * `routes.tsx` 에 두지 않는 까닭 — 그 파일은 routes 배열을 export 하므로 컴포넌트를 함께 두면 fast refresh 가 끊긴다.
 */
export default function RemoteScope() {
  const ctx = useOutletContext()
  syncHospitalTimeZone(useContext(FormatContextProvider)?.hospitalTimeZone)
  return (
    <div className="x-remote-root khds-root">
      <ToastProvider>
        <Outlet context={ctx} />
      </ToastProvider>
    </div>
  )
}
```

### 2.4 `app/Standalone.tsx` — dev 전용 host 에뮬레이터 (prod 빌드 제외)

mock auth · 자체 QueryClient · mock store 지연 등록:

```tsx
// src/app/Standalone.tsx:46-52
const mockAuth = createMockAuthContext()
const queryClient = new QueryClient()

// mock 모드(백엔드 미기동, `window.__DIAGNOSTIC_RESULT_API_BASE__ = 'mock'`)의 데이터 출처 — **지연 로드 함수**만 등록한다.
// 동적 import 라 mock json 은 별도 청크가 되고 mock 모드 API 가 처음 부를 때만 로드된다.
// 운영(host nest)은 이 파일을 거치지 않으므로 등록도 로드도 없다.
registerMockStore(() => import('@/mocks/patient-results/store'))
```

dev 메뉴(menuId ↔ child path 1:1):

```tsx
// src/app/Standalone.tsx:74-109
const DEV_MENU: { group: string; items: MenuItem[] }[] = [
  {
    group: '검사결과',
    items: [
      // 환자목록이 맨 앞이다 — standalone 에서 세션 환자를 고르는 dev 전용 화면이고, 나머지 화면이 그 선택을 세션으로 이어받는다.
      // host 에는 이 메뉴가 없다(세션을 올리는 곳은 x-ancillary-main 환자리스트).
      { menuId: 'diagnostic_result_patient_list', path: 'patient-list', label: '환자목록', title: '환자목록(dev)' },
      // 폭(브라우저 창)을 줄이면 같은 화면이 상세 · 요약으로 바뀐다(useContainerTier).
      { menuId: 'diagnostic_result_patient', path: 'diagnostic-result-view', label: '검사결과조회', title: '검사결과조회(폭에 따라 통합조회 · 상세 · 요약)' },
      { menuId: 'diagnostic_result_summary', path: 'summary', label: '검사결과 요약', title: '검사결과 요약(읽기전용 + AI 브리핑)' },
    ],
  },
  {
    // host 메뉴에는 없는 dev 전용 항목 — 검사결과조회는 받은 폭으로 뷰를 고르므로(useContainerTier)
    // host 타일 폭의 틀 안에 같은 화면을 띄워 상세(M) · 요약(S) 뷰를 창 크기와 무관하게 본다.
    group: '검사결과조회 타일 폭 (dev)',
    items: [
      { menuId: 'diagnostic_result_patient', path: 'diagnostic-result-view', frame: 960, label: '1/2 타일 · 상세', title: '검사결과조회 — 1/2 타일(960px) · 상세' },
      { menuId: 'diagnostic_result_patient', path: 'diagnostic-result-view', frame: 480, label: '1/4 타일 · 요약', title: '검사결과조회 — 1/4 타일(480px) · 요약' },
    ],
  },
  {
    // 검사결과 요약이 실제로 받는 폭(420px) — host 에서는 그 폭의 타일 · 보드 칸에 뜬다.
    group: '검사결과 요약 폭 (dev)',
    items: [
      { menuId: 'diagnostic_result_summary', path: 'summary', frame: 420, label: '요약 타일 420', title: '검사결과 요약 — 요약 타일(420px)' },
    ],
  },
  {
    // dev 전용 화면 — 운영 routes 에 없고 아래 DEV_ROUTES 로만 붙는다(백엔드 중계로 AI Gateway deep agent 에 자유 질의하는 시험 화면).
    group: 'PoC (dev)',
    items: [
      { menuId: 'diagnostic_result_poc_test', path: 'poc-test', label: '테스트조회', title: '테스트조회(PoC · AI Agent)' },
    ],
  },
]
```

DevShell — NavigationContext 대역 + 세션 대역:

```tsx
// src/app/Standalone.tsx:163-196
function DevShell() {
  const location = useLocation()
  const navigate = useNavigate()
  const screen = screenOf(location.pathname)
  // 틀 폭(dev 전용) — 메뉴에 등록된 조합일 때만 쓴다(임의 값으로 틀을 만들지 않는다).
  const [searchParams] = useSearchParams()
  const frameKey = screen != null ? keyOf({ path: screen, frame: Number(searchParams.get('frame')) || null }) : null
  const current = frameKey != null && TITLE_BY_KEY.has(frameKey) ? frameKey : screen
  const frame = current != null && current !== screen ? Number(searchParams.get('frame')) : null
  const patient = useSessionPatient()

  // 세션 소유자 대역 — host 에는 x-ancillary-main 이 세션을 올리지만 여기엔 없다. 이 remote 안에서 고른 환자
  // (환자목록 화면 · 테스트조회 — `ehr:patient:selected`)를 세션으로 올려, 화면들이 host 와 같은 경로(세션)로 받게 한다.
  useSubscribe(
    PATIENT_SELECTED,
    (p) => publishSessionPatientStandIn({ patientId: p.patientId, patientName: p.patientName ?? '' }),
    { replayLast: true },
  )

  // host NavigationContext 대역 — menuId 를 이 에뮬레이터의 라우트로 해석한다(미등록 menuId 는 no-op).
  const navApi = useMemo<AppNavApi>(
    () => ({
      navigate: (menuId: string) => {
        const path = PATH_BY_MENU_ID.get(menuId)
        if (path) void navigate(`/${path}`)
      },
      resolveHref: (menuId: string) => {
        const path = PATH_BY_MENU_ID.get(menuId)
        return path ? `/${path}` : undefined
      },
    }),
    [navigate],
  )
```

```tsx
// src/app/Standalone.tsx:197-247
  return (
    // `khds-root` — 이 셸은 remote routes 의 RemoteScope 밖이라 킷 base 리셋을 직접 걸어야 한다.
    <Stack className="khds-root h-screen min-h-0">
      <Stack
        as="header"
        direction="row"
        align="center"
        gap={8}
        className="shrink-0 border-b border-(--color-border-secondary) bg-(--color-surface-subtle) px-(--spacing-12) py-(--spacing-6)"
      >
        <DevMenu current={current} onPick={(path) => void navigate(`/${path}`)} />
        <span className="typo-label-2sm-bold text-(--color-foreground-secondary)">diagnostic-result (standalone)</span>
        {current != null && (
          <span className="typo-body-2sm-regular text-(--color-foreground)">{TITLE_BY_KEY.get(current)}</span>
        )}
        <Stack.Item grow />
        <Button size="sm" variant="secondary" icon={<User />} onClick={() => void navigate(`/${LANDING}`)}>
          {patient ? patient.patientName || '환자' : '환자 선택'}
        </Button>
        {patient && (
          <>
            <Badge size="sm" variant="quiet">
              {patient.patientId}
            </Badge>
            {/* 세션 비움 재현 — host 에서 수진 없이 열기·조회 실패로 세션이 비워질 때 화면이 비는지 본다. */}
            <Button size="sm" variant="ghost" onClick={() => publishSessionPatientStandIn(null)}>
              세션 비우기
            </Button>
          </>
        )}
      </Stack>

      {/* Host 와 동일하게 layout route 의 <Outlet context={authContext}> 로 auth 를 주입한다.
          Remote 페이지는 useAuthContext()(=useOutletContext)로 그 계약만 읽는다 — auth 자체 보유 금지(§5.1). */}
      <Stack.Item grow className="min-h-0">
        <NavigationContext.Provider value={navApi}>
          {frame ? (
            // host 타일 대역 — 가운데 고정 폭 틀. 화면은 이 틀의 폭을 재서 뷰를 고른다.
            <Stack direction="row" justify="center" className="h-full min-h-0 bg-(--color-surface-recessed)">
              <div className="h-full min-h-0 max-w-full border-x border-(--color-border)" style={{ width: frame }}>
                <Outlet context={mockAuth} />
              </div>
            </Stack>
          ) : (
            <Outlet context={mockAuth} />
          )}
        </NavigationContext.Provider>
      </Stack.Item>
    </Stack>
  )
}
```

dev 전용 라우트 덧붙이기 · 루트 Provider:

```tsx
// src/app/Standalone.tsx:254-277
const DEV_ONLY_CHILDREN: RouteObject[] = [
  // 환자목록 — standalone 의 세션 환자 고르기(고른 환자를 위 DevShell 이 세션 대역으로 올린다). host 에는 메뉴가 없다.
  { path: 'patient-list', element: <PatientList /> },
  // 테스트조회(PoC) — 좌 환자목록 + 우 내부 이벤트 발행 · AI 자유 질의(백엔드 중계)
  { path: 'poc-test', element: <PocTest /> },
]
const DEV_ROUTES: RouteObject[] = routes.map((route, i) =>
  // 첫 항목이 RemoteScope 레이아웃 route 다(index route 는 children 을 가질 수 없다)
  i !== 0 || route.index === true ? route : { ...route, children: [...(route.children ?? []), ...DEV_ONLY_CHILDREN] },
)

// 공유 routes 는 leaf-only(허브 없음) — 에뮬레이터만의 landing 으로 index→환자목록 redirect 를 앞에 끼우고,
// 메뉴에 없는 경로(오타·구 링크)도 같은 곳으로 모은다(운영 host 는 ./routes 만 쓰므로 무영향).
const router = createBrowserRouter([
  {
    path: '/',
    element: <DevShell />,
    children: [
      { index: true, element: <Navigate to={LANDING} replace /> },
      ...DEV_ROUTES,
      { path: '*', element: <Navigate to={`/${LANDING}`} replace /> },
    ],
  },
])
```

```tsx
// src/app/Standalone.tsx:308-322
export default function Standalone() {
  useShellChrome()
  // 운영(Host nest)에서는 host 셸이 ToastProvider · MessageBoxProvider 를 마운트한다 — dev 단독 실행일 때만 여기서
  // 같은 Provider 를 세운다(useToast 는 Provider 밖에서 호출하면 throw 하고, useMessageBox 는 no-op 이 되어
  // 즐겨찾기 세트 삭제 확인 같은 흐름이 dev 에서만 조용히 멈춘다).
  return (
    <QueryClientProvider client={queryClient}>
      <ToastProvider aria-label="알림">
        <MessageBoxProvider>
          <RouterProvider router={router} />
        </MessageBoxProvider>
      </ToastProvider>
    </QueryClientProvider>
  )
}
```

---

## 3. 화면 → API 추적표

### 3.1 화면별 추적표 (운영 routes 2 + dev 전용 2)

| 화면(path) | view 컴포넌트 | 사용 widget | feature | entity api 함수 | HTTP 메서드+경로 (`PREFIX=/api/v1/diagnostic-result`) | 호출 훅 | 상태 보관 |
|---|---|---|---|---|---|---|---|
| `diagnostic-result-view` (menuId `diagnostic_result_patient`) | `views/diagnostic-result-view/DiagnosticResultView.tsx` | `widgets/diagnostic-result/wide/WideMain`(L) · `compact/CompactMain`(M·S) · `DetailDialog`(M·S) · `wide/DetailPanel`(L) · `wide/AiSection` · `wide/CategoryTree` · `wide/ResultChipBar` · `wide/ObservationList` · `wide/GraphPanel` · `wide/report/ReportView` | `features/favorite-set/*` (별표 Popover · 세트 관리 Dialog) | `fetchObservations` · `fetchExamDates` · `fetchObservation` · `fetchRecordDocument` · `fetchViewerConfig` · `saveViewerConfig` · `createFavoriteSet` · `assignFavoriteSets` · `renameFavoriteSet` · `deleteFavoriteSet` · `reorderFavoriteSets` · `fetchAnalysesForPatient` · `fetchAnalysisStatus` · `runAnalysis` | GET `/observations` · GET `/observations/exam-dates` · GET `/observations/{id}` · GET `/observations/{id}/record-document` · GET/PUT `/viewer-configurations/me` · POST `/viewer-configurations/me/favorite-sets` · PUT `.../favorite-assignments` · PATCH/DELETE `.../favorite-sets/{id}` · PUT `.../favorite-sets/order` · GET `/observation-analyze-results` · GET `/observation-analyze-results/status` · POST `/observation-analyze-results` | `useDiagnosticResultData`(widget) → `useRangedObservations` · `useAnalysis`(entity) — 전부 useEffect+useState, react-query 미사용 | 데이터: 훅 local state / 뷰·필터: zustand `wideStore` · `compactStore` · `detailStore` · `periodStore` · `shared/store/chartModeStore` / 환자: host `usePatientContextStore`(app-kit) / 브라우저: sessionStorage(`dr.trend.days` 등) · localStorage(`dr.date.basis` · `dr.category.multi`) |
| `summary` (menuId `diagnostic_result_summary`) | `views/summary/ReportSummary.tsx` | `widgets/report-summary/ReportSummaryWidget` → `ResultSummaryCard` · `KeyResultItem` | 없음 | `fetchObservations`(rangeKey 'all') · `fetchExamDates` · `fetchResultSummary` | GET `/observations?patient_id&date_basis=performed` · GET `/observations/exam-dates` · POST `/result-summary` | `useReportSummaryData` → `useRangedObservations` · `useResultSummary` | 훅 local state · `chartModeStore` 공유 · 환자: host 세션 |
| `patient-list` (dev, menuId `diagnostic_result_patient_list`) | `views/patient-list/PatientList.tsx` | `widgets/patient-search/usePatientSearch` | 없음 | `fetchObservationPatients` | GET `/observations/patients?keyword&page&size` | `usePatientSearch` | local state + event-bus `ehr:patient:selected`(replayLast) |
| `poc-test` (dev, menuId `diagnostic_result_poc_test`) | `views/poc-test/PocTest.tsx` (+ `agentCommand.ts` · `aiAgent.ts`) | `widgets/patient-search/PatientsSidebar` | 없음 | `fetchObservationPatients` · `callAiAgent` | GET `/observations/patients` · POST `/ai-chat` | `usePatientSearch` · 직접 호출 | local state · event-bus 발행(`ehr:diagnostic:agent-command`) |

### 3.2 entity api 전체 함수 표 (4파일 전수)

| 파일 | 함수 | HTTP | 경로(PREFIX 이후) | 요청 | 응답 변환 |
|---|---|---|---|---|---|
| `entities/observation/api/observationApi.ts:104-119` | `fetchObservationPatients({keyword,page,size})` | GET | `/observations/patients` | query `keyword,page,size` | `Page<PatientRow>` (`toPatientRow`, `total_elements→totalElements`) |
| 〃 `:121-145` | `fetchObservations({patientId,fromDate,toDate,dateBasis})` | GET | `/observations` | query `patient_id,from_date,to_date,date_basis` | `Observation[]` (`toObservation`) |
| 〃 `:151-158` | `fetchExamDates({patientId,dateBasis})` | GET | `/observations/exam-dates` | query `patient_id,date_basis` | `ExamDateRow[]` (`toExamDateRow`) |
| 〃 `:160-164` | `fetchObservation(id)` | GET | `/observations/{id}` | path | `Observation \| null` |
| 〃 `:170-179` | `fetchRecordDocument(observationId)` | GET | `/observations/{id}/record-document` | path | `RecordDocument` (404 서식 없음 · 503 원천 미연결 · 502 원천 실패) |
| `entities/analysis/api/analysisApi.ts:59-72` | `fetchAnalysesForPatient(patientId,{page,size})` | GET | `/observation-analyze-results` | query `patient_id,page,size` | `{data?: Analysis[]; error?}` — page_view 의 `content` 만 |
| 〃 `:78-88` | `fetchAnalysisStatus(patientId)` | GET | `/observation-analyze-results/status` | query `patient_id` | `{data?: {isReviewTarget}}` |
| 〃 `:111-122` (+`runAnalysisOnce` 124-144, `waitForNewAnalysis` 147-160) | `runAnalysis(patientId,{size,onlyIfTarget})` | POST | `/observation-analyze-results` | body `{patient_id, only_if_target?}` · timeout 50s | 201/204 → 이력 재조회. 타임아웃 시 5초 간격 90초 폴링. 환자별 in-flight 공유 |
| `entities/analysis/api/resultSummaryApi.ts:63-72` | `fetchResultSummary(patientId)` | POST | `/result-summary` | body `{patient_id}` · timeout 50s | `{data?: ResultSummary; error?}` (`agent_status` 보정 `toAgentStatus`) |
| `entities/viewer-configuration/api/viewerConfigurationApi.ts:65-73` | `fetchViewerConfig(userId)` | GET | `/viewer-configurations/me` | — (사용자는 토큰) | `ViewerConfig`; 404 → `DEFAULT_VIEWER_CONFIG` |
| 〃 `:76-84` | `saveViewerConfig({userId,lastViewLayoutCode})` | PUT | `/viewer-configurations/me` | body `{last_view_layout_code}` | void(204) |
| 〃 `:89-97` | `createFavoriteSet(userId,setName,observation?)` | POST | `/viewer-configurations/me/favorite-sets` | body `{set_name, observation_type_code?, observation_code?}` | void |
| 〃 `:100-107` | `assignFavoriteSets(userId,observation,favoriteSetIds)` | PUT | `/viewer-configurations/me/favorite-assignments` | body `{observation_type_code, observation_code, favorite_set_ids}` | void |
| 〃 `:109-112` | `renameFavoriteSet(userId,favoriteSetId,setName)` | PATCH | `/viewer-configurations/me/favorite-sets/{id}` | body `{set_name}` | void |
| 〃 `:114-117` | `deleteFavoriteSet(userId,favoriteSetId)` | DELETE | `/viewer-configurations/me/favorite-sets/{id}` | — | void |
| 〃 `:120-123` | `reorderFavoriteSets(userId,favoriteSetIds)` | PUT | `/viewer-configurations/me/favorite-sets/order` | body `{favorite_set_ids}` | void |
| `views/poc-test/aiAgent.ts:59-89` (view 내부, entity 아님) | `callAiAgent(input,{onProgress})` | POST | `/ai-chat` | body `{message, thread_id:null}` | `AgentOutput` (status≠done → throw) |

---

## 4. 데이터 계층 샘플 — 검사결과조회 끝에서 끝까지

> 이 레포에는 `shared/lib/query_cache.ts`(staleTime 0 래퍼)가 **없다**. 대신 「화면이 열릴 때마다 최신 조회」는 (a) `useEffect` 가 환자·요청키마다 `fetch` 를 새로 부르고 (b) 환자 변경 시 `key={patient.patientId}` 로 본문을 remount 하는 방식으로 달성한다(§4.7 참조).

### 4.1 공용 HTTP 베이스 — `@x-ehr/api-axios` 위임 · PREFIX · ApiError

```ts
// src/shared/api/apiBase.ts:15-49
import { api } from '@x-ehr/api-axios'

const PREFIX = '/api/v1/diagnostic-result'

export class ApiError extends Error {
  readonly status: number
  /** 응답을 기다리다 끊었다(api-axios timeout) — 서버는 처리를 계속하고 있을 수 있다. */
  readonly timedOut: boolean

  constructor(message: string, status = 0, timedOut = false) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.timedOut = timedOut
  }
}

/** 요청별 옵션 — 지정하지 않으면 api-axios 기본(10초)이다. 50초를 넘기면 api-axios 가 50초로 자른다. */
interface RequestOptions {
  timeoutMs?: number
}

declare global {
  interface Window {
    __DIAGNOSTIC_RESULT_API_BASE__?: string | null
  }
}

/** 실패 1건의 사용자 문구 — 오류 메시지가 있으면 그것, 없으면 호출부의 폴백 문구. */
export const errorMessage = (e: unknown, fallback: string): string => (e as Error)?.message || fallback

export function isMock(): boolean {
  const v = typeof window !== 'undefined' ? window.__DIAGNOSTIC_RESULT_API_BASE__ : undefined
  return v === null || v === 'mock'
}
```

```ts
// src/shared/api/apiBase.ts:54-103
function toApiError(e: unknown, path: string): ApiError {
  const err = e as {
    response?: { status?: number; data?: { message?: string } }
    message?: string
    code?: string
  }
  const status = err?.response?.status ?? 0
  // 응답 없이 시간 초과 — 브라우저 개발자 도구에는 (canceled) 로 남는다
  if (!err?.response && (err?.code === 'ECONNABORTED' || err?.code === 'ETIMEDOUT')) {
    return new ApiError(`응답 대기 시간을 넘었습니다 — ${path}`, 0, true)
  }
  const message =
    err?.response?.data?.message ||
    (status
      ? `백엔드 요청 실패 (HTTP ${status}) — ${path}`
      : `백엔드에 연결할 수 없습니다 (${err?.message || '네트워크 오류'})`)
  return new ApiError(message, status)
}

function cleanParams(params?: QueryParams): Record<string, string | number | boolean> | undefined {
  if (!params) return undefined
  const out: Record<string, string | number | boolean> = {}
  for (const [k, v] of Object.entries(params)) {
    if (v !== undefined && v !== null && v !== '') out[k] = v
  }
  return out
}

export async function get<T>(path: string, params?: QueryParams): Promise<T> {
  try {
    const res = await api.get<T>(PREFIX + path, { params: cleanParams(params) })
    return res.data
  } catch (e) {
    throw toApiError(e, path)
  }
}

export async function post<T>(path: string, body: unknown, options?: RequestOptions): Promise<T> {
  try {
    const res = await api.post<T>(
      PREFIX + path,
      body,
      options?.timeoutMs ? { timeout: options.timeoutMs } : undefined,
    )
    return res.data
  } catch (e) {
    throw toApiError(e, path)
  }
}
```

api-axios 의 타임아웃 상수(공유 패키지 빌드 산출물):

```js
// ../../node_modules/@x-ehr/api-axios/dist/index.js:107-108
var DEFAULT_TIMEOUT_MS = 1e4;
var MAX_TIMEOUT_MS = 5e4;
```

`asRecord` — 응답(snake_case)을 명시 매핑으로 옮길 때 쓰는 입구:

```ts
// src/shared/lib/record.ts:1-5
/**
 * 알 수 없는 값(API 응답 · 파싱 결과)을 레코드로 본다 — null · undefined 는 빈 레코드.
 * 백엔드 응답(snake_case)을 화면 표기로 옮기는 명시 매핑들이 같은 입구를 쓴다.
 */
export const asRecord = (v: unknown): Record<string, unknown> => (v ?? {}) as Record<string, unknown>
```

### 4.2 entity api — `fetchObservations` + snake→camel 명시 매핑

```ts
// src/entities/observation/api/observationApi.ts:31-67
const toObservation = (raw: unknown): Observation => {
  const o = asRecord(raw)
  return {
    id: o['id'] as string,
    observationTypeCode: o['observation_type_code'] as string,
    observationTypeName: o['observation_type_name'] as string,
    categoryCode: (o['category_code'] as string) ?? '',
    diagnosticServiceSectionCode: (o['diagnostic_service_section_code'] as string) ?? null,
    sourceTypeCode: (o['source_type_code'] as string) ?? null,
    observationCode: o['observation_code'] as string,
    observationName: o['observation_name'] as string,
    encounterClass: (o['encounter_class'] as string) ?? null,
    encounterDate: (o['encounter_date'] as string) ?? null,
    departmentName: (o['department_name'] as string) ?? null,
    doctorName: (o['doctor_name'] as string) ?? null,
    orderingDoctorName: (o['ordering_doctor_name'] as string) ?? null,
    orderedAt: o['ordered_at'] as string,
    orderDate: (o['order_date'] as string) ?? null,
    performedAt: o['performed_at'] as string,
    resultDatetime: o['result_datetime'] as string,
    verifiedAt: o['verified_at'] as string,
    resultSummary: (o['result_summary'] as string) ?? '',
    resultUnit: (o['result_unit'] as string) ?? null,
    resultFlag: (o['result_flag'] as string) ?? null,
    resultGroupCode: (o['result_group_code'] as string) ?? null,
    deltaValue: typeof o['delta_value'] === 'number' ? o['delta_value'] : null,
    referenceLow: typeof o['reference_low'] === 'number' ? o['reference_low'] : null,
    referenceHigh: typeof o['reference_high'] === 'number' ? o['reference_high'] : null,
    reportStatus: (o['report_status'] as string) ?? null,
    isAbnormal: o['is_abnormal'] === true,
    isCritical: o['is_critical'] === true,
    pacsStudyUid: (o['pacs_study_uid'] as string) ?? null,
    slideImageUrl: (o['slide_image_url'] as string) ?? null,
    waveformDataUrl: (o['waveform_data_url'] as string) ?? null,
    recordId: (o['record_id'] as string) ?? null,
  }
}
```

```ts
// src/entities/observation/api/observationApi.ts:121-145
export async function fetchObservations(
  { patientId, fromDate, toDate, dateBasis }: {
    patientId: string
    fromDate?: string | null
    toDate?: string | null
    /** 기간을 거르는 날짜 — 기간을 줄 때 함께 준다. 없으면 서버 기본(결과일). */
    dateBasis?: DateBasis | null
  },
): Promise<Observation[]> {
  if (isMock()) {
    return (await mockStore<ObservationMockStore>()).getObservations({
      fromDate: fromDate ?? null,
      toDate: toDate ?? null,
      dateBasis: dateBasis ?? null,
    })
  }
  const res = await get<unknown[]>('/observations', {
    patient_id: patientId,
    from_date: fromDate,
    to_date: toDate,
    date_basis: dateBasis,
  })
  if (!Array.isArray(res)) throw new ApiError('검사결과 응답 형식이 올바르지 않습니다.')
  return res.map(toObservation)
}
```

### 4.3 entity model 타입

```ts
// src/entities/observation/model/types.ts:12-66
/** 검사분류별 결과 1행 — 백엔드 `observation_category_view`. */
export interface Observation {
  id: string
  /** SPECIMEN_DIAGNOSTICS / DIAGNOSTIC_IMAGING / FUNCTIONAL_DIAGNOSTICS / CLINICAL_RECORD / EXTERNAL_RECORD */
  observationTypeCode: string
  observationTypeName: string
  /** laboratory / imaging / procedure / vital-signs / exam / survey */
  categoryCode: string
  /** 진단 서비스 구분(HL7 v2-0074) — 병리(SP·CP) 판정 키. `isPathology` 가 읽는다. */
  diagnosticServiceSectionCode: string | null
  /** 외부연동기록 출처(PASTA …) — 외부연동기록(EXTERNAL_RECORD)만, 나머지는 null. */
  sourceTypeCode: string | null
  observationCode: string
  observationName: string
  // 수진·처방 스냅샷 — 발행처가 안 실었으면 null(화면은 빈칸)
  /** 입원외래 구분 — O · I · E · HPC. 표시명은 `ENCOUNTER_CLASS_NAME`. */
  encounterClass: string | null
  /** 진료일자 YYYY-MM-DD. */
  encounterDate: string | null
  departmentName: string | null
  /** 진료의. */
  doctorName: string | null
  /** 처방의 — 진료의와 다를 수 있다. */
  orderingDoctorName: string | null
  /** 처방일시 — 처방이 실제로 저장된 시각. */
  orderedAt: string
  /** 오더일자 YYYY-MM-DD — 의사가 검사를 하라고 지시한 날짜(시각 없음). */
  orderDate: string | null
  performedAt: string
  resultDatetime: string
  verifiedAt: string
  resultSummary: string
  resultUnit: string | null
  resultFlag: string | null
  /** 결과 그룹 — 추이 그룹핑 키(검체진단 · 임상관찰 · 외부연동기록). 영상 · 기능은 null. */
  resultGroupCode: string | null
  /** 직전 결과 대비 변화량 — 검체진단만. 그 외 null. */
  deltaValue: number | null
  /** 참고범위 — 결과마다 실려 온 값(같은 검사도 결과일마다 다를 수 있다). 추이 그래프 기준선 · 상세 「참고치」. 영상은 null. */
  referenceLow?: number | null
  referenceHigh?: number | null
  reportStatus: string | null
  /** 비정상 — 적재 때 백엔드 도메인이 계산해 저장한 값. 화면은 다시 판정하지 않는다. */
  isAbnormal: boolean
  /** 위험치 — 검체진단 `is_critical`. 그 외 false. */
  isCritical: boolean
  pacsStudyUid: string | null
  slideImageUrl: string | null
  waveformDataUrl: string | null
  /**
   * 원천 모듈 서식(기록 문서 · HTML) 식별자 — 과내검사(기능) · 병리 · 일부 진단검사만. 있으면 보고서뷰 · 상세 「서식」 탭이
   * `GET /observations/{id}/record-document` 로 HTML 을 연다(`fetchRecordDocument`). 없으면 null.
   */
  recordId: string | null
```

```ts
// src/entities/observation/model/types.ts:118-125
/** 페이지 envelope — 백엔드 `page_view<T>`. */
export interface Page<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}
```

### 4.4 entity model 훅 — `useRangedObservations` (기간 단계 + 더 과거만 덧붙이기)

```ts
// src/entities/observation/model/useRangedObservations.ts:63-103
export function useRangedObservations({ patientId, rangeKey, dateBasis, enabled = true }: Options): RangedObservations {
  const [seq, setSeq] = useState(0)
  const key = `${patientId}|${dateBasis}|${seq}`
  const wantFrom = rangeFromOf(rangeKey)

  const [cache, setCache] = useState<Cache | null>(null)
  const [loadError, setLoadError] = useState<Failure | null>(null)
  const [extendError, setExtendError] = useState<Failure | null>(null)
  const [timeline, setTimeline] = useState<{ key: string; rows: ExamDateRow[] } | null>(null)
  const [refreshing, setRefreshing] = useState(false)

  const ready = cache?.key === key
  const pending = enabled && !ready && loadError?.key !== key
  const loading = pending && cache == null
  const reloading = pending && cache != null
  const extendKey = ready ? `${key}|${wantFrom ?? 'all'}` : ''
  const needsMore = enabled && ready && !covers(cache.from, wantFrom)
  const extending = needsMore && extendError?.key !== extendKey

  // 진입 범위를 기억한다 — 환자 · 날짜기준이 바뀔 때만 처음 읽기를 다시 하고, 단계가 바뀌는 것은 아래 넓히기가 맡는다.
  const wantFromRef = useRef(wantFrom)
  useEffect(() => {
    wantFromRef.current = wantFrom
  }, [wantFrom])

  // ── 처음 읽기 — 결과([단계 시작, 오늘]) ──
  useEffect(() => {
    if (!enabled) return
    let alive = true
    const from = wantFromRef.current
    fetchObservations({ patientId, fromDate: from, dateBasis })
      .then((rows) => {
        if (alive) setCache({ key, from, rows })
      })
      .catch((e: unknown) => {
        if (alive) setLoadError({ key, message: errorMessage(e, '검사결과를 조회할 수 없습니다.') })
      })
    return () => {
      alive = false
    }
  }, [enabled, patientId, dateBasis, key])
```

```ts
// src/entities/observation/model/useRangedObservations.ts:122-143
  // ── 넓히기 — 지금 읽어 둔 시작보다 과거가 필요하면 그 구간만 읽어 붙인다 ──
  const loadedFrom = ready ? cache.from : null
  useEffect(() => {
    if (!extending || loadedFrom == null) return
    let alive = true
    const toDate = shiftYmd(loadedFrom, { days: -1 })
    fetchObservations({ patientId, fromDate: wantFrom, toDate, dateBasis })
      .then((older) => {
        if (!alive) return
        setCache((c) => {
          if (!c || c.key !== key || c.from !== loadedFrom) return c
          const seen = new Set(c.rows.map((o) => o.id))
          return { key, from: wantFrom, rows: [...c.rows, ...older.filter((o) => !seen.has(o.id))] }
        })
      })
      .catch((e: unknown) => {
        if (alive) setExtendError({ key: extendKey, message: errorMessage(e, '이전 검사결과를 조회할 수 없습니다.') })
      })
    return () => {
      alive = false
    }
  }, [extending, loadedFrom, wantFrom, patientId, dateBasis, key, extendKey])
```

```ts
// src/entities/observation/model/useRangedObservations.ts:165-186
  const loaded = ready && !refreshing ? cache.rows : EMPTY
  const inRange = useMemo(
    () => (wantFrom ? loaded.filter((o) => dateOfBasis(o, dateBasis) >= wantFrom) : loaded),
    [loaded, wantFrom, dateBasis],
  )

  return {
    inRange,
    loaded,
    loadedFrom: ready ? cache.from : null,
    loading,
    reloading,
    loadError: loadError?.key === key ? loadError.message : null,
    extending,
    extendError: extendError?.key === extendKey ? extendError.message : null,
    timeline: timeline?.key === key ? timeline.rows : EMPTY_TIMELINE,
    timelineLoaded: timeline?.key === key,
    refresh,
    refreshing,
    reload: () => setSeq((s) => s + 1),
  }
}
```

### 4.5 widget 데이터 훅 — `useDiagnosticResultData` (결과 · 설정 · AI 분석을 한 벌로)

```ts
// src/widgets/diagnostic-result/useDiagnosticResultData.ts:118-150
export function useDiagnosticResultData({
  patientId,
  userId,
  scopeToWidePeriod = false,
  viewParams,
}: UseDiagnosticResultDataOptions): DiagnosticResultData {
  // ── 데이터 상태 ─────────────────────────────────────────────────────────
  const [config, setConfig] = useState<ViewerConfig>({ ...DEFAULT_VIEWER_CONFIG, userId: userId ?? '' })
  const [reloadSeq, setReloadSeq] = useState(0)
  const [apiError, setApiError] = useState<string | null>(null)
  /** 화면 설정 로딩 결과 — 요청 키와 함께 담아 **로딩을 파생**시킨다. */
  const [configKey, setConfigKey] = useState<string | null>(null)

  // 자동 분석 — 결과 브리핑이 모든 폭에 있다(L 세부형 · M·S 간략형). 환자당 한 번이라 폭이 바뀌어도 다시 돌지 않는다
  const analysis = useAnalysis(patientId, { auto: true })

  // 결과 — 조회 기간 단계(L · M·S 공유)로 읽고, 넓어지면 더 과거 구간만 추가로 읽어 붙인다(useRangedObservations).
  // 날짜기준은 넓은 폭만 고른다 — 좁은 폭의 날짜 묶음 축은 검사일이다.
  const rangeKey = useRangeKey(patientId)
  const wideDateBasis = useWideStore((s) => s.dateBasis)
  const dateBasis: DateBasis = scopeToWidePeriod ? wideDateBasis : 'performed'
  const ranged = useRangedObservations({ patientId, rangeKey, dateBasis })

  const requestKey = `${userId ?? ''}|${patientId}|${reloadSeq}`
  // 분석 이력도 첫 화면의 일부라 로딩에 포함한다(브리핑이 「분석 없음」으로 한 번 비쳤다가 바뀌지 않게).
  const loading = configKey !== requestKey || ranged.loading || analysis.loading
  const loadError = ranged.loadError

  const pendingDetailCode = useDetailStore((s) => s.pendingDetailCode)
  const observations = ranged.inRange

  // host AI 커맨드 → 스토어. 폭과 무관하게 화면에 붙어 있어야 좁은 뷰에서 온 커맨드도 남는다.
  useAgentCommandBridge(patientId)
```

```ts
// src/widgets/diagnostic-result/useDiagnosticResultData.ts:152-176
  // ── 진입 — 뷰 · 기간 초기화 + 화면설정(분석 이력은 useAnalysis, 결과는 useRangedObservations 가 따로 읽는다) ──
  useEffect(() => {
    let alive = true
    // 환자 변경·재시도 시 뷰/필터를 기본값으로 — 저장된 보기 방식(과 그 모드의 기본 정렬)은 아래에서 덮어쓴다.
    // 조회 기간은 저장값이 없다 — 기간 단계(periodStore)의 고정 기본(3개월)으로 연다.
    // (스토어는 외부 스토어라 effect 본문에서 써도 React 연쇄 렌더를 만들지 않는다.)
    useWideStore.getState().resetForPatient()
    useDetailStore.getState().resetForPatient()
    // 미로그인이면 설정을 읽지 않는다 — 기본 설정으로 연다
    void (userId ? fetchViewerConfig(userId) : Promise.resolve({ ...DEFAULT_VIEWER_CONFIG, userId: '' }))
      .catch(() => ({ ...DEFAULT_VIEWER_CONFIG, userId: userId ?? '' }))
      .then((cfg) => {
        if (!alive) return
        setConfig(cfg)
        const mode = (cfg.lastViewLayoutCode && LIST_MODE_OF[cfg.lastViewLayoutCode]) || FALLBACK_LIST_MODE
        const vs = useWideStore.getState()
        vs.setListMode(mode)
        vs.setSort(DEFAULT_SORT[mode])
        vs.setActiveChip(null)
        setConfigKey(requestKey)
      })
    return () => {
      alive = false
    }
  }, [userId, patientId, requestKey])
```

### 4.6 widget UI — `WideMain` (L 폭) : 데이터 props + 스토어 구독 + 파생

```tsx
// src/widgets/diagnostic-result/wide/WideMain.tsx:33-78
export default function WideMain({ results, noPatient = false }: WideMainProps) {
  const {
    loading,
    loadError,
    reload,
    refresh,
    refreshing,
    observations,
    timeline,
    extending,
    dateBasis,
    patientId,
    favoriteSets,
    analyses,
    analysisHistory,
    analyzing,
    analyzeError,
    reanalyze,
    apiError,
    clearApiError,
    openDetail,
    persistLayout,
  } = results

  // ── 파생 — 필터 값은 넓은 폭 스토어에서 읽는다 ─────────────────────────────
  const codeFilter = useWideStore((s) => s.codeFilter)
  const checked = useWideStore((s) => s.checked)
  const rangeKey = useRangeKey(patientId)
  // 검사일 타임라인 → 슬라이더 눈금 · 「기간 밖에 이전 검사일이 있는지」 — 켜진 결과분류의 영역으로 거른다(세트 · 3단 · 검색 · 상태는 안 거른다)
  const days = useMemo(() => timelineDays(timeline, timelineAreaKeep(checked)), [timeline, checked])
  const {
    groups,
    seriesByCode,
    compareGroups,
    windowStart,
    tree,
    counts,
    isTime,
    listRows,
    totalCount,
    trendDays,
    hasTimeSeries,
    reportSource,
    reportDates,
    reportSections,
  } = useWideDerive(observations, favoriteSets.sets)
```

```tsx
// src/widgets/diagnostic-result/wide/WideMain.tsx:125-191
  return (
    <div className="dr-root">
      <AiSection
        analyses={analyses}
        analysisHistory={analysisHistory}
        observations={observations}
        analyzing={analyzing}
        analyzeError={analyzeError}
        onAnalyze={reanalyze}
        seriesByCode={seriesByCode}
        windowStart={windowStart}
        noPatient={noPatient}
      />

      {/* 부분 실패(상세 조회·설정 저장 등) — 화면을 막지 않고 알린 뒤 닫을 수 있게 한다. */}
      {apiError && (
        <Banner variant="error" description={apiError} onClose={clearApiError} />
      )}

      <ResultChipBar
        counts={counts}
        onSetChip={setChip}
        rangeKey={rangeKey}
        days={days}
        onRefresh={refresh}
        refreshing={refreshing}
        noPatient={noPatient}
      />

      {/* 검사코드 필터 — openView 의 observationCodes 로만 켜진다. */}
      {codeFilter.length > 0 && <CodeFilterBar codes={codeFilter} observations={observations} />}

      <div className="dr-body">
        {/* 골격 태그는 폭 규칙(styles.css `.dr-body > nav`)이 걸리는 자리다 — 지우면 분류가 목록과 폭을 반씩 나눈다. */}
        <nav>
          <CategoryTree tree={tree} favoriteSets={favoriteSets} noPatient={noPatient} />
        </nav>
        <ObservationList
          isTime={isTime}
          trendDays={trendDays}
          resultsVersion={observations}
          hasTimeSeries={hasTimeSeries}
          allGroups={groups}
          rows={listRows}
          reportSource={reportSource}
          reportDates={reportDates}
          reportSections={reportSections}
          totalCount={totalCount}
          seriesByCode={seriesByCode}
          windowStart={windowStart}
          favoriteSets={favoriteSets}
          compareGroups={compareGroups}
          onChangeMode={changeMode}
          onChangeSort={changeSort}
          onOpen={openDetail}
          noPatient={noPatient}
          reloading={refreshing}
          rangeKey={rangeKey}
          days={days}
          extending={extending}
          dateBasis={dateBasis}
        />
        {/* 상세 — 목록 오른쪽에 붙어 표를 민다. 좁은 폭의 Dialog 는 화면(DiagnosticResultView)이 그린다. */}
        <DetailPanel observations={observations} favoriteSets={favoriteSets} />
      </div>
    </div>
  )
}
```

### 4.7 view — `DiagnosticResultView` (세션 환자 · 폭 측정 · remount)

```tsx
// src/views/diagnostic-result-view/DiagnosticResultView.tsx:88-119
export default function DiagnosticResultView() {
  const patient = useSessionPatient()
  const userId = useCurrentUser()?.email || null
  const patientId = patient?.patientId ?? null
  const [received, setReceived] = useState<ReceivedParams | null>(null)
  const { ref, tier } = useContainerTier<HTMLDivElement>()

  // openView 수신 — 화면이 닫혀 있었으면 열리는 순간(마운트) replayLast 로, 열려 있으면 즉시 온다.
  useViewParams(VIEW_KEY, (params) => {
    setReceived({ params, forPatientId: params.patientId ?? patientId })
  })
  // 지금 세션 환자를 가리키는 파라미터만 본문에 넘긴다 — 세션이 다른 환자로 바뀌면 이전 파라미터는 저절로 빠진다.
  const viewParams = received && received.forPatientId === patientId ? received.params : null

  return (
    // 폭 측정 대상 — host 가 준 영역(전체 본문 · 타일) 그 자체다. 높이 체인을 이어받는 골격이기도 하다.
    // typo-body-md-regular — 본문 기준선을 화면에서 한 번 준다(킷: DataGrid·차트 등은 컨테이너를 상속한다).
    <div ref={ref} className="dr-tier-root typo-body-md-regular" data-tier={tier ?? undefined}>
      {tier == null ? null : patient ? (
        <ResultsBody
          key={patient.patientId}
          patientId={patient.patientId}
          userId={userId}
          tier={tier}
          viewParams={viewParams}
        />
      ) : (
        <EmptyBody tier={tier} />
      )}
    </div>
  )
}
```

```tsx
// src/views/diagnostic-result-view/DiagnosticResultView.tsx:167-181
function ResultsBody({ patientId, userId, tier, viewParams }: ResultsBodyProps) {
  // 조회 기간 단계(L · M·S 공유)로 서버가 거른 결과 — 넓은 폭은 고른 날짜기준, 좁은 폭은 검사일로 거른다(바뀌면 훅이 다시 읽는다).
  const results = useDiagnosticResultData({ patientId, userId, viewParams, scopeToWidePeriod: tier === 'L' })

  // 기간 · 좁은 뷰 상태 — 환자가 바뀌면(remount) 기간을 이 환자에 묶고(기본 3개월) 좁은 뷰를 비운 뒤, 파라미터를 반영한다(선언 순서 = 실행 순서).
  // 그리기 전에 맞춰야 이전 환자의 접힘 · 묶음 수가 한 프레임 비치지 않는다.
  useLayoutEffect(() => {
    usePeriodStore.getState().bindPatient(patientId)
    useCompactStore.getState().resetForPatient()
  }, [patientId])
  useLayoutEffect(() => {
    if (!viewParams) return
    if (viewParams.patientId && viewParams.patientId !== patientId) return
    useCompactStore.getState().applyParams(viewParams)
  }, [viewParams, patientId])
```

```tsx
// src/views/diagnostic-result-view/DiagnosticResultView.tsx:201-245
  return (
    <>
      {tier === 'L' ? (
        <div className="drp-root">
          <section className="drp-main">
            <WideMain results={results} />
          </section>
        </div>
      ) : (
        <div className="drp-root">
          <div className="dvc-standalone">
            {/* 스크롤 틀 하나(CompactMain 의 .dr-compact-column) — AI 브리핑을 앞머리로 넘겨 기간 툴바 · 결과와 함께 스크롤한다 */}
            <CompactMain
              observations={results.observations}
              timeline={results.timeline}
              extending={results.extending}
              patientId={patientId}
              loading={results.loading}
              error={results.loadError}
              size={tier === 'M' ? 'detail' : 'summary'}
              onOpen={results.openDetail}
              lead={
                <AiBrief
                  tone="agent"
                  loading={results.loading}
                  error={results.analyzeError}
                  analyzing={results.analyzing}
                  analyses={results.analyses}
                  history={results.analysisHistory}
                  onReanalyze={results.reanalyze}
                />
              }
            />
          </div>
        </div>
      )}

      {/* 상세 — 열림 상태는 한 벌(detailStore)이라 폭이 바뀌어도 닫히지 않고 모양만 바뀐다.
          넓은 폭(L)은 WideMain 이 목록 오른쪽 패널(wide/DetailPanel)로 그리고, 좁은 폭은 여기서 Dialog 로 연다. */}
      {tier !== 'L' && (
        <DetailDialog observations={results.observations} favoriteSets={results.favoriteSets} />
      )}
    </>
  )
}
```

폭 → tier 판정 훅:

```ts
// src/shared/lib/useContainerTier.ts:10-15
export type ContainerTier = 'S' | 'M' | 'L'

const TIER_MIN_WIDTH = { M: 640, L: 1200 } as const

const tierOf = (width: number): ContainerTier =>
  width >= TIER_MIN_WIDTH.L ? 'L' : width >= TIER_MIN_WIDTH.M ? 'M' : 'S'
```

---

## 5. 쓰기(mutation) 샘플 — 즐겨찾기 세트 체크(PUT favorite-assignments)

체인: 별표 버튼 → Popover 체크박스 → `useSetAction.run` → `FavoriteSetActions.toggleSet`(widget 훅) → `mutateSets`(낙관적 반영 + API + 재조회) → entity api `assignFavoriteSets` → `reloadSets`(GET /viewer-configurations/me).

### 5.1 버튼(별표) — 표 셀

```tsx
// src/widgets/diagnostic-result/wide/cells/FavoriteButton.tsx:12-35
export default function FavoriteButton({
  row,
  favoriteSets,
  open,
  onOpenChange,
}: {
  row: Observation
  favoriteSets: FavoriteSetActions
  open: boolean
  onOpenChange: (open: boolean) => void
}) {
  const on = isFav(row, favoriteSets.sets)
  return (
    <FavoriteSetPicker observation={row} actions={favoriteSets} open={open} onOpenChange={onOpenChange}>
      <Button
        size="sm"
        variant="ghost"
        icon={<Star />}
        label={on ? '즐겨찾기 세트 변경' : '즐겨찾기 세트에 추가'}
        className={on ? 'text-(--color-warning-subtle-foreground)' : 'text-(--color-foreground-tertiary)'}
      />
    </FavoriteSetPicker>
  )
}
```

### 5.2 feature ui — 체크 = 바로 저장

```tsx
// src/features/favorite-set/ui/FavoriteSetPicker.tsx:42-85
function PickerBody({ observation, actions }: Pick<FavoriteSetPickerProps, 'observation' | 'actions'>) {
  // 조작은 한 번에 하나씩(`useSetAction`) — 응답 전에 다음 체크가 나가 순서가 뒤집히지 않게 한다.
  const { busy, error, run } = useSetAction()
  const sets = displaySets(actions.sets)
  const addable = canAddSet(actions.sets)

  const toggle = (set: DisplaySet, on: boolean) => void run(() => actions.toggleSet(observation, set, on))

  return (
    <Stack gap={6} className="w-[272px]">
      <Stack direction="row" align="center" gap={6}>
        <span className="typo-label-2sm-bold text-nowrap">즐겨찾기 세트</span>
        <Stack.Item grow className="min-w-0 text-end">
          <span className="typo-caption-sm-regular block truncate text-(--color-foreground-tertiary)">
            {observation.observationName}
          </span>
        </Stack.Item>
      </Stack>

      <Stack gap={4}>
        {sets.map((set) => (
          <Stack key={set.id || 'unsaved-default'} direction="row" align="center" gap={6}>
            <Stack.Item grow className="min-w-0">
              <Checkbox checked={inSet(observation, set)} disabled={busy} onCheckedChange={(on) => toggle(set, on)}>
                {set.setName}
              </Checkbox>
            </Stack.Item>
            <span className="typo-caption-sm-regular text-(--color-foreground-tertiary)">{set.items.length}</span>
          </Stack>
        ))}
      </Stack>

      <Divider />

      {/* 새 세트 = 만들면서 이 검사를 바로 넣는다 */}
      <NewSetRow addable={addable} busy={busy} onCreate={(name) => run(() => actions.createSet(name, observation))} />
      {error && (
        <span role="alert" className="typo-caption-sm-regular text-(--color-error)">
          {error}
        </span>
      )}
    </Stack>
  )
}
```

### 5.3 feature model — 조작 1건의 실행 상태

```ts
// src/features/favorite-set/model/useSetAction.ts:8-24
export function useSetAction(): {
  busy: boolean
  error: string | null
  clearError: () => void
  run: (job: () => Promise<string | null>) => Promise<boolean>
} {
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const run = async (job: () => Promise<string | null>): Promise<boolean> => {
    setBusy(true)
    const message = await job()
    setBusy(false)
    setError(message)
    return message === null
  }
  return { busy, error, clearError: () => setError(null), run }
}
```

### 5.4 widget 훅 — 낙관적 반영 → API → 재조회(invalidate 대신 `reloadSets`)

```ts
// src/widgets/diagnostic-result/useDiagnosticResultData.ts:268-333
  // ── 즐겨찾기 세트 — 조작마다 바로 저장하고, 끝나면 설정을 다시 읽어 서버 값(새 세트 id · 순서)으로 맞춘다 ──
  //   실패하면 다시 읽어 되돌리고 서버 문구를 돌려준다 — 선택창·관리 Dialog 가 그 자리에 보인다.
  const favoriteSetList = config.favoriteSets
  const setsRef = useRef(favoriteSetList)
  useEffect(() => {
    setsRef.current = favoriteSetList
  }, [favoriteSetList])

  const reloadSets = useCallback(async () => {
    if (!userId) return
    try {
      const cfg = await fetchViewerConfig(userId)
      setConfig((prev) => ({ ...prev, favoriteSets: cfg.favoriteSets }))
    } catch {
      // 다시 읽기 실패 — 화면 값을 그대로 두고 다음 조작·재접속 때 맞춘다
    }
  }, [userId])

  const mutateSets = useCallback(
    async (job: () => Promise<void>, optimistic?: (sets: FavoriteSet[]) => FavoriteSet[]): Promise<string | null> => {
      if (!userId) return NO_USER_MESSAGE
      if (optimistic) setConfig((prev) => ({ ...prev, favoriteSets: optimistic(prev.favoriteSets) }))
      try {
        await job()
        await reloadSets()
        return null
      } catch (e) {
        await reloadSets()
        return errorMessage(e, '즐겨찾기 세트를 저장하지 못했습니다.')
      }
    },
    [reloadSets, userId],
  )

  // 세트 API 는 사용자 키가 필수다 — 미로그인이면 mutateSets 가 먼저 막으므로 여기서는 빈 키가 나가지 않는다.
  const uid = userId ?? ''
  const favoriteSets = useMemo<FavoriteSetActions>(
    () => ({
      sets: favoriteSetList,
      toggleSet: (o, set, on) => {
        // 저장 안 된 「기본」 — 처음 체크할 때 이 검사를 넣으며 만든다(끄는 경우는 없다)
        if (set.unsaved) return on ? mutateSets(() => createFavoriteSet(uid, set.setName, keyOf(o))) : Promise.resolve(null)
        const ids = setIdsOf(o, setsRef.current)
        const next = on ? [...new Set([...ids, set.id])] : ids.filter((id) => id !== set.id)
        return mutateSets(
          () => assignFavoriteSets(uid, keyOf(o), next),
          (sets) => withAssignment(sets, o, set.id, on),
        )
      },
      createSet: (setName, o) => mutateSets(() => createFavoriteSet(uid, setName, o && keyOf(o))),
      renameSet: (set, setName) =>
        mutateSets(() =>
          set.unsaved ? createFavoriteSet(uid, setName) : renameFavoriteSet(uid, set.id, setName),
        ),
      deleteSet: (set) => mutateSets(() => deleteFavoriteSet(uid, set.id)),
      moveSet: (set, by) => {
        const ids = setsRef.current.map((s) => s.id)
        const i = ids.indexOf(set.id)
        const j = i + by
        if (i < 0 || j < 0 || j >= ids.length) return Promise.resolve(null)
        ;[ids[i], ids[j]] = [ids[j] as string, ids[i] as string]
        return mutateSets(() => reorderFavoriteSets(uid, ids))
      },
    }),
    [favoriteSetList, mutateSets, uid],
  )
```

### 5.5 entity api — PUT

```ts
// src/entities/viewer-configuration/api/viewerConfigurationApi.ts:99-107
/** 검사 1건이 들어갈 세트를 정확히 favoriteSetIds 로 맞춘다 — 빈 목록이면 모든 세트에서 빠진다. */
export async function assignFavoriteSets(userId: string, observation: Favorite, favoriteSetIds: string[]): Promise<void> {
  if (isMock()) return (await mock()).assignFavoriteSets(observation, favoriteSetIds)
  await put(`${configPath(userId)}/favorite-assignments`, {
    observation_type_code: observation.observationTypeCode,
    observation_code: observation.observationCode,
    favorite_set_ids: favoriteSetIds,
  })
}
```

### 5.6 세트 관리 Dialog — 삭제 확인(MessageBox) → `deleteSet`

```tsx
// src/features/favorite-set/ui/FavoriteSetManageDialog.tsx:45-62
function ManageBody({ actions, onDeleted }: Pick<FavoriteSetManageDialogProps, 'actions' | 'onDeleted'>) {
  const messageBox = useMessageBox()
  const { busy, error, clearError, run } = useSetAction()
  const sets = displaySets(actions.sets)
  const addable = canAddSet(actions.sets)

  const remove = async (set: DisplaySet) => {
    const answer = await messageBox({
      title: '즐겨찾기 세트 삭제',
      message: `「${set.setName}」 세트를 지울까요? 이 세트에 든 검사 ${set.items.length}건이 세트에서 빠집니다. 다른 세트의 즐겨찾기는 그대로 남습니다.`,
      buttons: 'yesNo',
      tone: 'destructive',
      labels: { yes: '삭제', no: '취소' },
      defaultButton: 'no',
    })
    if (answer !== 'yes') return
    if (await run(() => actions.deleteSet(set))) onDeleted?.(set.id)
  }
```

---

## 6. 전역 상태 (zustand)

스토어 5개: `shared/store/chartModeStore` · `widgets/diagnostic-result/periodStore` · `detailStore` · `wide/wideStore` · `compact/compactStore`.

### 6.1 스토어 정의 전체 — `periodStore`

```ts
// src/widgets/diagnostic-result/periodStore.ts:1-52
import { create } from 'zustand'

import { coveringRangeOf, DEFAULT_RANGE, nextRangeOf, widerRange, type RangeKey } from '@/entities/observation/model/rangeSteps'

/**
 * 검사결과조회 조회 기간 단계 — 넓은 폭(L)과 좁은 폭(M·S)이 **같은 값**을 쓴다(폭이 바뀌어도 기간이 이어진다).
 *   항상 오늘까지다(종료일이 없다). 결과는 `useDiagnosticResultData` → `useRangedObservations` 가 이 단계로 읽고,
 *   넓어지면 더 과거 구간만 추가로 읽어 붙인다.
 *   값은 **환자에 묶인다** — 다른 환자의 단계는 읽는 쪽(`useRangeKey`)에서 기본(3개월)으로 보인다. 그래서 새 환자의 첫 조회가
 *   앞 환자가 넓혀 둔 범위로 나가지 않는다. 화면 본문이 환자를 열 때 `bindPatient` 로 묶는다.
 */
interface PeriodState {
  patientId: string | null
  rangeKey: RangeKey
  /** 환자를 연다 — 다른 환자면 기본 단계로. */
  bindPatient: (patientId: string) => void
  setRangeKey: (key: RangeKey) => void
  /** 다음(더 넓은) 단계로 — 스크롤 넓히기. 이미 전체면 그대로. */
  widen: () => void
  /** 날짜가 들어가는 단계까지 넓힌다(좁히지 않는다) — openView examDate. */
  widenToInclude: (date: string | null | undefined) => void
  /** 날짜가 들어가는 가장 좁은 단계로 맞춘다 — openView fromDate · AI 기간. */
  coverFrom: (date: string | null | undefined) => void
  /** 기본(3개월)으로 — RESET. */
  reset: () => void
}

export const usePeriodStore = create<PeriodState>((set, get) => ({
  patientId: null,
  rangeKey: DEFAULT_RANGE,
  bindPatient: (patientId) => {
    if (get().patientId !== patientId) set({ patientId, rangeKey: DEFAULT_RANGE })
  },
  setRangeKey: (rangeKey) => set({ rangeKey }),
  widen: () => {
    const next = nextRangeOf(get().rangeKey)
    if (next) set({ rangeKey: next.key })
  },
  widenToInclude: (date) => {
    const need = coveringRangeOf(date)
    if (need) set({ rangeKey: widerRange(get().rangeKey, need) })
  },
  coverFrom: (date) => {
    const need = coveringRangeOf(date)
    if (need) set({ rangeKey: need })
  },
  reset: () => set({ rangeKey: DEFAULT_RANGE }),
}))

/** 이 환자의 조회 기간 단계 — 아직 묶이지 않았거나 다른 환자에 묶여 있으면 기본(3개월). */
export const useRangeKey = (patientId: string | null): RangeKey =>
  usePeriodStore((s) => (patientId && s.patientId === patientId ? s.rangeKey : DEFAULT_RANGE))
```

### 6.2 셀렉터 구독 (컴포넌트) vs `getState()` (이벤트 핸들러 · effect)

셀렉터 구독:

```tsx
// src/widgets/diagnostic-result/wide/ResultChipBar.tsx:57-62
  const activeChip = useWideStore((s) => s.activeChip)
  const listMode = useWideStore((s) => s.listMode)
  // 마커 — 상세조회에서 목록이 알린 「지금 보는 곳」(누적조회 · 보고서뷰 · 시간대별은 세로 날짜축이 없어 그리지 않는다)
  const positionDate = useWideStore((s) => s.positionDate)
  const dateBasis = useWideStore((s) => s.dateBasis)
  const setDateBasis = useWideStore((s) => s.setDateBasis)
```

파생 셀렉터 훅:

```ts
// src/widgets/diagnostic-result/detailStore.ts:49-54
/** 열린 상세 — 넓은 폭 패널 · 좁은 폭 Dialog 가 같은 값을 읽는다(닫혀 있으면 null). */
export function useOpenDetail(): Observation | null {
  const drawerOpen = useDetailStore((s) => s.drawerOpen)
  const detail = useDetailStore((s) => s.detail)
  return drawerOpen ? detail : null
}
```

`getState()` 사용 — 핸들러:

```tsx
// src/widgets/diagnostic-result/wide/WideMain.tsx:80-98
  const changeMode = useCallback(
    (m: ListMode) => {
      const s = DEFAULT_SORT[m]
      const vs = useWideStore.getState()
      vs.setListMode(m)
      vs.setSort(s)
      // 보기 방식 4종 모두 저장한다(DETAIL · REPORT · CUMULATIVE · HOURLY) — 다음 진입은 이 방식의 기본 정렬로 연다.
      persistLayout(m)
    },
    [persistLayout],
  )

  // 정렬은 저장하지 않는다 — 보기 방식만 저장한다(다음 진입은 그 방식의 기본 정렬)
  const changeSort = useCallback((s: string) => useWideStore.getState().setSort(s), [])

  const setChip = useCallback((k: ChipKey | null) => {
    const next = useWideStore.getState().activeChip === k ? null : k
    useWideStore.getState().setActiveChip(next)
  }, [])
```

`setState()` 직접 호출(상세 보강):

```ts
// src/widgets/diagnostic-result/useDiagnosticResultData.ts:228-236
  const openDetail = useCallback((o: Observation) => {
    useDetailStore.getState().openDrawer(o)
    fetchObservation(o.id)
      .then((d) => {
        if (!d) return
        useDetailStore.setState((st) => (st.detail?.id === o.id ? { detail: { ...st.detail, ...d } } : {}))
      })
      .catch((e: unknown) => setApiError(`상세 조회 실패: ${errorMessage(e, '알 수 없는 오류')}`))
  }, [])
```

### 6.3 화면이 비워질 때(환자 없음) · 환자 바뀔 때 스토어 비우기

```tsx
// src/views/diagnostic-result-view/DiagnosticResultView.tsx:126-132
function EmptyBody({ tier }: { tier: ContainerTier }) {
  // 전역 뷰 스토어에 앞서 본 환자의 필터·열린 상세가 남아 있으면 빈 틀에 비친다 — 그리기 전에 비운다.
  useLayoutEffect(() => {
    useWideStore.getState().resetForPatient()
    useDetailStore.getState().resetForPatient()
    useCompactStore.getState().resetForPatient()
  }, [])
```

`wideStore` 의 환자 수명 상태 vs 표시 설정(수명이 다르다):

```ts
// src/widgets/diagnostic-result/wide/wideStore.ts:148-164
/** 뷰 초기값 — 환자 변경(remount) 시 `resetForPatient` 로 이 값으로 되돌린다. */
const initialWide = (): WideState => ({
  listMode: 'flat',
  sort: DEFAULT_SORT.flat,
  activeChip: null,
  checked: [],
  search: '',
  compareOpen: false,
  compareCodes: [],
  codeFilter: [],
  compareSpan: '7d',
  expandedTrend: [],
  reportCollapsed: [],
  historyOpen: false,
  focus: null,
  positionDate: null,
})
```

```ts
// src/widgets/diagnostic-result/wide/wideStore.ts:205-212
export const useWideStore = create<WideState & WideDisplayState & WideActions>((set, get) => ({
  ...initialWide(),
  trendDays: readTrendDays(),
  dateBasis: readDateBasis(),
  multiSelect: readLocal(MULTI_SELECT_KEY) === '1',
  graphHourly: readGraphHourly(),
  reportLayout: readReportLayout(),
```

```ts
// src/widgets/diagnostic-result/wide/wideStore.ts:311-312
  // 환자 변경(remount) 시 뷰 초기화 — config 기반 listMode/sort/chip 은 데이터 훅의 load effect 가 덮어쓴다.
  resetForPatient: () => set(initialWide()),
```

### 6.4 화면 간 공유 스토어(sessionStorage 백업)

```ts
// src/shared/store/chartModeStore.ts:1-34
import { create } from 'zustand'

/**
 * 수치형 추이 그래프 표시 — `line`=추세(값 사이 기울기) · `spike`=측정 시점(바닥에서 세운 세로 스템).
 * 같은 데이터를 두 시각으로 본다. 검사결과조회(모든 폭) · 요약이 **한 값을 공유**한다 — 그래서 화면 밖(shared)에 둔다.
 */
export type ChartMode = 'line' | 'spike'

/** 추이 표시 모드 세션 키 — 화면(검사결과조회↔요약)과 폭을 옮겨도 고른 표시가 유지된다. */
const CHART_MODE_KEY = 'dr.chart.mode'
const readChartMode = (): ChartMode => {
  try {
    return sessionStorage.getItem(CHART_MODE_KEY) === 'spike' ? 'spike' : 'line'
  } catch {
    return 'line' // 세션 저장이 막힌 환경(시크릿 등) — 기본값으로 동작만 한다
  }
}

interface ChartModeState {
  chartMode: ChartMode
  setChartMode: (m: ChartMode) => void
}

export const useChartModeStore = create<ChartModeState>((set) => ({
  chartMode: readChartMode(),
  setChartMode: (m) => {
    set({ chartMode: m })
    try {
      sessionStorage.setItem(CHART_MODE_KEY, m)
    } catch {
      /* 세션 저장이 막힌 환경 — 이번 화면에서만 유지된다 */
    }
  },
}))
```

---

## 7. event-bus

### 7.1 계약 선언 (module augmentation)

```ts
// src/shared/lib/events.ts:8-26
import '@x-ehr/event-bus'

/**
 * 이 remote 안에서 고른 환자 — 환자목록 화면·테스트조회(PoC)가 발행한다(replayLast).
 * 검사결과조회·요약은 이것이 아니라 **환자 세션**(`shared/patient-session/sessionPatient`)을 본다.
 * standalone 에서만 `Standalone.tsx` 의 소유자 대역이 이 선택을 세션으로 올린다.
 */
export const PATIENT_SELECTED = 'ehr:patient:selected'
/** 메뉴 열기 파라미터 채널 — viewKey(=host menuId) 단위로 replayLast 보관된다. */
export const VIEW_PARAMS_EVENT = 'ehr:app:view-params'
/** host AI 가 해석한 뷰 커맨드 — 검사결과조회 화면의 필터에 반영된다. */
export const AGENT_COMMAND_EVENT = 'ehr:diagnostic:agent-command'

/** 활성 환자 payload — 환자 id · 이름 · 활성 검사건수(환자목록 행 값). */
export interface PatientSelectedEvent {
  patientId: string
  patientName?: string | undefined
  observationCount?: number | undefined
}
```

```ts
// src/shared/lib/events.ts:83-89
declare module '@x-ehr/event-bus' {
  interface EhrEventMap {
    'ehr:patient:selected': PatientSelectedEvent
    'ehr:app:view-params': ViewParamsEvent
    'ehr:diagnostic:agent-command': AgentCommand
  }
}
```

### 7.2 `openView` / `useViewParams` — publish(replayLast) + navigate / subscribe(replayLast) + URL 반영

```ts
// src/shared/lib/viewBus.ts:63-74
export function useOpenView(): (menuId: string, params?: ViewParams) => void {
  const { navigate } = useAppLink()
  return useCallback(
    (menuId: string, params: ViewParams = {}) => {
      // 1) 파라미터를 뷰 채널에 저장(replayLast) — 화면이 나중에 열려도 구독 즉시 수신.
      publish(VIEW_PARAMS_EVENT, { viewKey: menuId, params, requestId: nextRequestId() }, { replayLast: true })
      // 2) 열기(또는 포커스). Host 미주입(standalone)이면 no-op.
      navigate(menuId)
    },
    [navigate],
  )
}
```

```ts
// src/shared/lib/viewBus.ts:76-114
/** 화면이 자기 viewKey 의 파라미터를 수신하는 훅. */
export function useViewParams(
  viewKey: string,
  handler: (params: ViewParams, meta: { viewKey: string; fromUrl?: boolean; requestId?: string }) => void,
): void {
  // 최신 handler 를 ref 에 담아 구독을 재생성하지 않는다 — 렌더 중이 아니라 effect 에서 쓴다.
  const handlerRef = useRef(handler)
  useEffect(() => {
    handlerRef.current = handler
  })
  const lastReqId = useRef<string | null>(null)

  useEffect(() => {
    let delivered = false
    const unsub = subscribe(
      VIEW_PARAMS_EVENT,
      (ev: ViewParamsEvent) => {
        if (!ev || ev.viewKey !== viewKey) return
        if (ev.requestId && ev.requestId === lastReqId.current) return
        lastReqId.current = ev.requestId || null
        delivered = true
        const params = ev.params || {}
        reflectToUrl(params) // URL 동기 → 새로고침/북마크에서도 재현
        handlerRef.current(params, { viewKey, requestId: ev.requestId })
      },
      { replayLast: true },
    )
    // 버스에 저장값이 없던 경우(하드 리로드/딥링크 진입): URL 쿼리를 소스로 적용.
    if (!delivered) {
      const fromUrl = paramsFromUrl()
      if (fromUrl) handlerRef.current(fromUrl, { viewKey, fromUrl: true })
    }
    // 구독 해제 시 dedup 리셋 — 재오픈/환자변경/StrictMode 재실행마다 replayLast 를 다시 적용한다.
    return () => {
      unsub()
      lastReqId.current = null
    }
  }, [viewKey])
}
```

사용부 — 요약 화면의 「검사결과조회로 이어 보기」:

```tsx
// src/widgets/report-summary/ReportSummaryWidget.tsx:28-33
  const openView = useOpenView()
  const d = useReportSummaryData(patientId)
  const ai = useResultSummary(patientId)

  /** 주요 결과 「검사결과조회」 — 같은 환자 · 그 결과의 최근 검사일로 연다(받는 쪽이 그 날짜 묶음으로 스크롤 · 강조). */
  const openResults = (date: string) => openView(RESULTS_MENU_ID, { patientId, action: 'query', examDate: date })
```

### 7.3 host AI 커맨드 구독 (`subscribe` + requestId dedup)

```ts
// src/widgets/diagnostic-result/useAgentCommandBridge.ts:28-50
export function useAgentCommandBridge(patientId: string | null | undefined): void {
  const lastReqId = useRef<string | null>(null)
  useEffect(() => {
    const unsub = subscribe(
      AGENT_COMMAND_EVENT,
      (cmd) => {
        if (!cmd || typeof cmd !== 'object') return
        // patientId 미지정 커맨드는 현재 환자에 적용, 지정됐는데 다르면 무시
        if (cmd.patientId && patientId && cmd.patientId !== patientId) return
        if (cmd.requestId && cmd.requestId === lastReqId.current) return
        lastReqId.current = cmd.requestId || null
        applyAgentCommand(cmd)
      },
      { replayLast: true },
    )
    // 구독 해제 시 dedup 리셋 — 재구독(환자 변경·StrictMode·화면 재오픈) 뒤 replayLast 로 다시 온
    // 커맨드를 재적용해, 초기화가 커맨드를 덮어쓴 채 남지 않게 한다.
    return () => {
      unsub()
      lastReqId.current = null
    }
  }, [patientId])
}
```

### 7.4 환자 세션 수신 — host store(`@x-ehr/app-kit` `usePatientContextStore`) → `SessionPatient`

```ts
// src/shared/patient-session/sessionPatient.ts:20-45
/** 화면이 쓰는 세션 환자 — 세션 응답에서 이 remote 가 필요한 값만 옮긴다. */
export interface SessionPatient {
  patientId: string
  /** 원무에 이름이 없으면 빈 문자열이다 — ID 로 다시 조회하지 않는다. */
  patientName: string
}

/** 세션 응답 → 세션 환자. **응답 모양이 바뀌면 여기만 고친다.** */
function toSessionPatient(context: PatientContext | null): SessionPatient | null {
  if (!context) return null
  return {
    patientId: context.patient.id,
    patientName: context.patient.name ?? '',
  }
}

/**
 * 현재 세션 환자. 아무도 환자를 올리지 않았거나 세션이 비워졌으면 `null`.
 *
 * 같은 환자의 내원만 바뀌어도 새 객체가 온다 — 환자 단위로 다시 그릴 화면은 `patientId` 를 key 로 쓴다.
 * 다시 가져오기(`refresh`)로 바뀐 것이 없으면 store 가 같은 값을 두므로 다시 그리지 않는다.
 */
export function useSessionPatient(): SessionPatient | null {
  const context = usePatientContextStore((state) => state.data)
  return useMemo(() => toSessionPatient(context), [context])
}
```

standalone 전용 세션 대역(발행측):

```ts
// src/shared/patient-session/sessionPatientStandIn.ts:17-36
export function publishSessionPatientStandIn(patient: SessionPatient | null): void {
  const store = usePatientContextStore
  if (!patient) {
    store.getState().clear()
    return
  }
  store.setState({
    data: {
      contextKey: `standalone:${patient.patientId}`,
      contextType: PATIENT_CONTEXT_TYPE.PATIENT_ONLY,
      state: PATIENT_CONTEXT_STATE.ACTIVE,
      readOnly: false,
      openedAt: new Date().toISOString(),
      patient: { id: patient.patientId, name: patient.patientName, birthDate: null, age: null, gender: null },
    },
    status: 'ready',
    error: null,
    refreshError: null,
  })
}
```

### 7.5 `publish` 사용부(환자목록 dev 화면)

```tsx
// src/views/patient-list/PatientList.tsx:84-92
  // 환자 선택 — 화면 state 갱신 + 버스 발행(전역 활성환자 → 조회/요약/host 상단바 동기).
  const selectPatient = useCallback((row: PatientRow) => {
    setPatient(row)
    publish(
      PATIENT_SELECTED,
      { patientId: row.patientId, patientName: row.patientName, observationCount: row.observationCount },
      { replayLast: true },
    )
  }, [])
```

---

## 8. mock

### 8.1 전환 방식 — `window.__DIAGNOSTIC_RESULT_API_BASE__ = 'mock'` + standalone 의 지연 등록

```ts
// src/shared/api/apiBase.ts:46-49
export function isMock(): boolean {
  const v = typeof window !== 'undefined' ? window.__DIAGNOSTIC_RESULT_API_BASE__ : undefined
  return v === null || v === 'mock'
}
```

```ts
// src/shared/api/mockStore.ts:1-20
// mock 모드의 데이터 출처 — standalone 이 mock store 를 **지연 로드 함수**로 등록한다.
//
// 이 파일은 mock 을 import 하지 않는다 — shared 가 mocks 를 읽으면 mock json 이 운영 번들에 섞이고
// shared → mocks → shared 순환이 생긴다. 등록(`registerMockStore`)은 `app/Standalone.tsx` 가 `isMock()` 일 때만 하고,
// 그때 mock 모듈은 별도 청크로 분리돼 운영(host nest)에서는 로드되지 않는다.
// 각 entity API 가 자기에게 필요한 모양(`mockStore<T>()`)으로 꺼내 쓴다.
import { ApiError } from '@/shared/api/apiBase'

let loader: (() => Promise<unknown>) | null = null

/** mock store 지연 로드 함수 등록 — standalone 전용. */
export function registerMockStore(load: () => Promise<unknown>): void {
  loader = load
}

/** 등록된 mock store. mock 모드인데 등록이 없으면 배선 누락이라 바로 알린다. */
export async function mockStore<T>(): Promise<T> {
  if (!loader) throw new ApiError('mock 모드인데 mock 데이터가 등록되지 않았습니다 (app/Standalone.tsx 의 registerMockStore).')
  return (await loader()) as T
}
```

entity api 에서 분기(각 API 가 자기 모양을 인터페이스로 선언):

```ts
// src/entities/observation/api/observationApi.ts:18-29
/** mock 모드에서 이 API 가 쓰는 mock store 의 모양. */
interface ObservationMockStore {
  getObservationPatients(q: { keyword?: string; page?: number; size?: number }): Page<PatientRow>
  getObservations(q: {
    fromDate: string | null
    toDate: string | null
    dateBasis: DateBasis | null
  }): Observation[]
  getObservation(id: string): Observation | null
  getExamDates(q: { dateBasis: DateBasis | null }): ExamDateRow[]
  getRecordDocument(id: string): RecordDocument | null
}
```

### 8.2 mock 파일 발췌 — `mocks/patient-results/store.ts`

```ts
// src/mocks/patient-results/store.ts:1-32
/**
 * diagnostic-result mock 저장소 — `isMock()` 일 때만 쓰인다(백엔드 미기동 로컬 개발).
 * 백엔드 `observation_category_view` · `viewer_configuration_view` 응답 형태를 화면 표기(camelCase)로 흉내낸다.
 */
import { areaOf, NARRATIVE_AREAS, type DateBasis } from '@/entities/observation/model/constants'
import { isCritical, sev } from '@/entities/observation/model/flags'
import { dateOfBasis, performedDate } from '@/entities/observation/model/format'
import { fmtDate, fmtDateTime } from '@/entities/observation/model/format'
import type { ExamDateRow, Observation, Page, PatientRow, RecordDocument } from '@/entities/observation/model/types'
import { FAVORITE_SET_LIMIT, FAVORITE_SET_NAME_MAX, inSet } from '@/entities/viewer-configuration/model/favoriteSets'
import type { Favorite, ViewerConfig } from '@/entities/viewer-configuration/model/types'

import { clinicalObservations } from './clinicalSeed'
import observationSeed from './observations.json'
import patientSeed from './patient.json'
import viewerConfigSeed from './viewer-config.json'

/** seed 는 화면이 쓰지 않는 필드(참고치·삭제표시 등)도 갖고 있어 확장 형태로 받는다. */
type MockObservation = Observation & { isDeleted?: boolean }

const labSeed = clone(observationSeed as unknown as MockObservation[])
/** 임상정보(활력징후 · 연속혈당 · I/O · 투약) — 검사 seed 마지막 검사일까지 4일(입원 구간). 원천 BC 연동 전이라 mock 에만 있다. */
const lastLabDate = labSeed.map((o) => (o.resultDatetime || o.orderedAt || '').slice(0, 10)).sort().at(-1) ?? '2026-05-25'
const observations: MockObservation[] = [...labSeed, ...(clinicalObservations(lastLabDate) as MockObservation[])]
let viewerConfig = clone(viewerConfigSeed as unknown as ViewerConfig) // upsert 시 갱신

/** 결과시간 우선, 없으면 처방시간(판독 대기 항목 정렬용). */
const sortKey = (o: MockObservation): string => o.resultDatetime || o.orderedAt || ''

function clone<T>(x: T): T {
  return x == null ? x : (JSON.parse(JSON.stringify(x)) as T)
}
```

```ts
// src/mocks/patient-results/store.ts:57-71
/** GET /observations — 기간 필터(결과시간 desc). */
export function getObservations(
  { fromDate, toDate, dateBasis }: {
    fromDate?: string | null
    toDate?: string | null
    /** 백엔드 date_basis 와 같다 — 없으면 결과일(서버 기본). 비어 있는 날짜의 대신 칸은 dateOfBasis 규칙. */
    dateBasis?: DateBasis | null
  } = {},
): Observation[] {
  let rows = observations.filter((o) => !o.isDeleted)
  const basis = dateBasis ?? 'result'
  if (fromDate) rows = rows.filter((o) => dateOfBasis(o, basis) >= fromDate)
  if (toDate) rows = rows.filter((o) => dateOfBasis(o, basis) <= toDate)
  return clone(rows.sort((a, b) => sortKey(b).localeCompare(sortKey(a))))
}
```

```json
// src/mocks/patient-results/viewer-config.json:1-15
{
  "userId": "demo",
  "favoriteSets": [
    {
      "id": "mock-set-default",
      "setName": "기본",
      "sortOrder": 0,
      "items": [
        { "observationTypeCode": "SPECIMEN_DIAGNOSTICS", "observationCode": "GLU" },
        { "observationTypeCode": "SPECIMEN_DIAGNOSTICS", "observationCode": "HB" }
      ]
    }
  ],
  "lastViewLayoutCode": "DETAIL"
}
```

주의: mock json 은 camelCase(화면 표기)이고 실제 백엔드 응답은 snake_case 다 — mock 은 `toObservation` 매핑을 거치지 않는다(`observationApi.ts:130-135`).

---

## 9. 디자인 시스템 (`@x-ehr/design-system`)

### 9.1 `DataGrid` — 상세조회 표 (가상화 · measured 행 높이 · master-detail)

```tsx
// src/widgets/diagnostic-result/wide/ObservationList.tsx:509-541
    return (
      <DataGrid<Observation>
        rows={rows as Observation[]}
        columns={flatColumns}
        apiRef={gridApiRef}
        getRowId={(o) => o.id}
        rowClassName={rowClassName}
        // 결과 줄바꿈으로 행마다 높이가 다르다 — 44 는 초기 추정치. measured 는 병합(mergeCells)과 함께 쓸 수 없어
        // 단위 칸 병합을 두지 않는다(함께 주면 고정 높이로 돌아가 줄바꿈한 결과가 잘린다).
        rowHeight={44}
        rowHeightMode="measured"
        selectionMode={false}
        // 추이 칸 = 펼침, 그 밖 = 오른쪽 상세. 한 클릭이 둘 다 일으키지 않는다.
        onRowClick={(meta) => {
          if (meta.colId === 'trend') toggleTrendIfAny(meta.row.id)
          else onOpen(meta.row)
        }}
        renderDetail={renderFlatTrendDetail}
        detailHeight={TREND_DETAIL_HEIGHT}
        expandedRowIds={expandedIds}
        onExpandedChange={onTrendExpandedChange}
        // 날짜 내림차순이면 행 끝 근처에서 기간을 넓힌다 — 사용자가 표를 스크롤한 뒤에만(처음부터 끝이 보이는 짧은 표는 넓히지 않는다)
        {...(flatWidening
          ? {
              onLoadMore: () => {
                if (scrolledRef.current) flatWiden.requestWiden()
              },
            }
          : {})}
        empty={empty}
        aria-label="상세조회 결과"
      />
    )
```

### 9.2 `Dialog` — 좁은 폭 상세

```tsx
// src/widgets/diagnostic-result/DetailDialog.tsx:20-38
export default function DetailDialog({ observations, favoriteSets }: DetailProps) {
  const obs = useOpenDetail()
  const closeDrawer = useDetailStore((s) => s.closeDrawer)
  if (!obs) return null

  return (
    <Dialog
      open
      onOpenChange={(open) => {
        if (!open) closeDrawer()
      }}
      title={<DetailTitle obs={obs} />}
      className="max-w-(--size-dialog-lg)"
      footer={<DetailActions obs={obs} favoriteSets={favoriteSets} />}
    >
      <DetailBody obs={obs} observations={observations} />
    </Dialog>
  )
}
```

### 9.3 `Button` · `Stack` · semantic 토큰 class — 주요 결과 카드

```tsx
// src/widgets/report-summary/KeyResultItem.tsx:36-69
  return (
    <Stack gap={6} bordered radius="md" className="min-w-0 p-(--spacing-12) bg-(--color-surface)" aria-label={item.observationName}>
      <Stack direction="row" align="center" gap={6} className="min-w-0">
        <Button
          size="sm"
          variant="text"
          className="min-w-0 truncate"
          aria-label={`${item.observationName} — ${item.latestDate} 검사결과조회`}
          onClick={() => onOpenResults(item.latestDate)}
        >
          {item.observationName}
        </Button>
        {graph ? (
          <b className="typo-label-2sm-bold whitespace-nowrap tabular-nums">
            {last.resultSummary ?? '—'}
            {unit}
          </b>
        ) : null}
        <FlagBadge flag={last.resultFlag} />
        <Stack.Item grow />
        <span className="typo-caption-sm-regular whitespace-nowrap tabular-nums text-(--color-foreground-tertiary)">
          {item.latestDate}
        </span>
      </Stack>
      {item.reason ? (
        <span className="typo-body-2sm-regular text-(--color-foreground-secondary)">{item.reason}</span>
      ) : null}
      {graph ? (
        <ResultTrendChart series={trend} name={item.observationName} unit={last.resultUnit ?? ''} height={CHART_HEIGHT} />
      ) : (
        <span className="typo-body-2sm-regular line-clamp-4">{obsResultText(last)}</span>
      )}
    </Stack>
  )
```

semantic 토큰 문법 예(위 인용 내): `p-(--spacing-12)` · `bg-(--color-surface)` · `text-(--color-foreground-tertiary)` · `typo-caption-sm-regular` · `typo-label-2sm-bold`. Tailwind 기본 스케일(`p-4` · `text-sm`)은 쓰지 않는다.

### 9.4 `SegmentedControl` · `Select` — 상태 필터 + 날짜기준

```tsx
// src/widgets/diagnostic-result/wide/ResultChipBar.tsx:81-117
  return (
    <Stack direction="row" align="center" gap={8} wrap>
      <SegmentedControl
        size="sm"
        options={options}
        value={activeChip ?? ALL}
        onValueChange={(v) => onSetChip(v === ALL ? null : (v as ChipKey))}
        aria-label="결과 상태 필터"
      />

      {/* 날짜기준 — 조회 기간 · 눈금 · 마커를 이 날짜로 본다. 폭 틀(.dr-field-basis)이 없으면 킷 w-full 로 줄을 다 먹는다. */}
      <div className="dr-field-basis">
        <Select
          size="md"
          options={DATE_BASIS_OPTIONS}
          value={dateBasis}
          onValueChange={(v) => {
            if (isDateBasis(v)) setDateBasis(v)
          }}
          aria-label="날짜기준"
        />
      </div>
      {/* 조회 기간 슬라이더 — 폭 틀(.dr-field-track 24rem) 안에서 트랙이 폭을 다 쓴다. 날짜기준 바로 뒤에 붙는다. */}
      <div className="dr-field-track">
        <RangeTrack
          value={rangeKey}
          onChange={(k) => usePeriodStore.getState().setRangeKey(k)}
          days={days}
          markerDate={markerDate}
          disabled={noPatient}
        />
      </div>
      <Button size="sm" variant="secondary" icon={<RefreshCw />} loading={refreshing} disabled={noPatient} onClick={onRefresh}>
        조회
      </Button>
    </Stack>
  )
```

### 9.5 `Tree` — 결과분류

```tsx
// src/widgets/diagnostic-result/wide/CategoryTree.tsx:214-232
      <Tree<CategoryNode>
        className="dr-result-tree"
        rowHeight={26}
        indent={14}
        items={tree}
        getId={getId}
        getLabel={getLabel}
        getChildren={getChildren}
        renderNode={renderNodeLabel}
        getIcon={getIcon}
        isDisabled={isDisabled}
        icons={NO_DEFAULT_ICONS}
        renderAccessory={renderAccessory}
        expandedIds={expandedIds}
        onExpandedChange={keepExpanded}
        selectedIds={selectedIds}
        onSelectedIdsChange={onSelectedIdsChange}
        aria-label="결과분류"
      />
```

### 9.6 `Card` (side drawer 대체 — gaps §1)

```tsx
// src/widgets/diagnostic-result/wide/DetailPanel.tsx:29-37
  return (
    <Card
      title={<DetailTitle obs={obs} />}
      divider
      bodyOverflow="auto"
      bodyAriaLabel="검사결과 상세 본문"
      // 폭은 Tailwind 로 준다 — 킷 Card 루트의 w-full 을 tailwind-merge 가 이 값으로 바꾼다(styles.css 의 width 는 진다).
      className="dr-detail-panel h-full min-h-0 w-[440px] shrink-0"
      action={
```

### 9.7 `styles/globals.css` (전체) · `styles.css` 골격 일부

```css
/* src/styles/globals.css:1-23 */
/* Remote — 디자인 SOT = @x-ehr/design-system. 디자인시스템 CSS(토큰+컴포넌트) 위에 @theme inline
 * 브리지로 @x-ehr 공유 패키지가 쓰는 유틸 이름을 전역 변수에 연결한다(값 SOT=디자인시스템). */
@import "tailwindcss";
@import "@x-ehr/design-system/styles";

@source "../";
/* 공유 UI 패키지(@x-ehr/app-kit — ToastProvider 등)의 클래스도 스캔 대상에 포함.
 * 분리 레포에선 워크스페이스 packages/ 가 없으므로 Nexus 설치본(node_modules)을 스캔한다
 * (foundation in-repo 시절 경로: ../../../../packages/app-kit/src). */
@source "../../../../node_modules/@x-ehr/app-kit";

/* class 기반 다크모드 */
@custom-variant dark (&:where(.dark, .dark *));

/* @theme inline — 유틸에 디자인시스템 var 를 인라인(순환참조 방지). @x-ehr 유틸 이름 → 토큰 매핑.
 * 화면 코드는 semantic 토큰 문법(`text-(--color-foreground)`)만 쓴다 — 여기 이름은 공유 패키지(@x-ehr/app-kit) 스캔용이다. */
@theme inline {
  --color-primary: var(--color-primary);
  --color-surface: var(--color-surface);
  --color-error: var(--color-error);
  --color-success: var(--color-success);
  --color-warning: var(--color-warning);
}
```

```css
/* src/styles/styles.css:1-41 */
/* ============================================================================
   diagnostic-result remote — 화면 **골격**만 남긴 CSS.
   컨트롤·표·배지·카드는 전부 `@x-ehr/design-system` 컴포넌트가 그린다. 여기 남는 것은
   킷이 다루지 않는 두 가지뿐이다:
     ① remote CSS 격리 경계(`.x-remote-root`)
     ② 화면 높이 체인(host <main> → 화면 → 킷 Card 가 100% 를 이어받게 하는 뼈대)
   기계 검사: `yarn check:screens` (킷 미사용 컨트롤·이모지·숫자 유틸 차단)
   ============================================================================ */

/* remote CSS 격리 경계 — routes 의 <RemoteScope> 래퍼(div.x-remote-root.khds-root)에 붙는다.
   display:contents 라 레이아웃 박스를 만들지 않는다(자식이 host <main>/standalone shell 을 직접
   부모로 본다) — 박스를 만들면 화면 높이 체인이 여기서 끊긴다. 셀렉터 조상으로는 남아
   아래 규칙이 remote 하위로만 적용되고 host 헤더/배경을 덮지 않는다. */
.x-remote-root { display: contents; }
.x-remote-root, .x-remote-root * { box-sizing: border-box; }

/* ── 화면 바깥 골격 — 좌 사이드(있는 화면만) + 우 본문 ──────────────────────
   `.drp-side` 를 쓰는 곳은 환자목록 · 테스트조회(PoC)뿐이다 —
   검사결과조회는 목록 없이 본문만 둔다(활성 환자를 버스로 받는다). */
.drp-root {
  display: flex;
  height: 100%;
  min-height: 0;
  gap: var(--spacing-12);
  padding: var(--spacing-16);
  background: var(--color-background);
}
.drp-side { flex: 0 0 var(--size-aside-sm); min-height: 0; display: flex; }
.drp-side > * { flex: 1; min-height: 0; }
.drp-main { flex: 1; min-width: 0; min-height: 0; display: flex; flex-direction: column; }

/* ── 통합조회 본문(상단 AI · 좌 결과분류 · 중 목록) ─────────────────────────
   결과분류는 3단(구역 · 분류 · 세부)이라 킷 사이드패널 토큰(`--size-sidepanel-sm` 220px)을 그대로 쓴다. */
.dr-root { flex: 1; min-height: 0; display: flex; flex-direction: column; gap: var(--spacing-8); }
.dr-body { --dr-category-width: var(--size-sidepanel-sm); flex: 1; min-height: 0; display: flex; gap: var(--spacing-8); }
.dr-body > nav { flex: 0 0 var(--dr-category-width); min-height: 0; display: flex; }
.dr-body > nav > * { flex: 1; min-height: 0; }
/* 목록 자리 — 태그(section)가 아니라 클래스로 잡는다. 킷 Card 루트도 <section> 이라 태그로 잡으면
   목록 오른쪽 패널(상세 Card)까지 남은 폭을 나눠 갖고, 카드 머리까지 flex:1 로 늘어난다. */
.dr-body > .dr-list { flex: 1; min-width: 0; min-height: 0; display: flex; }
.dr-body > .dr-list > * { flex: 1; min-height: 0; }
```

킷 컴포넌트 사용 집계(`grep from '@x-ehr/design-system'`): Stack 34 · Button 21 · EmptyState 12 · Banner 9 · Card 8 · Badge 8 · Tooltip 9 · Spinner 6 · SegmentedControl 4 · Dialog 3 · DataGrid 3 · Checkbox 4 · List 3 · Grid 3 · Chart 2 · Tree/Sparkline/RangeSparkline/BulletChart/Popover/Dropdown/Select/Pagination/TextField/Image/Chip 각 1~2.

---

## 10. i18n

사실 관계:
- `public/` 에는 `index.html` 뿐이다(`public/i18n/` 폴더 없음, `version.json` 없음). `src` 에 `useTranslation` · `i18next` · `t(` 호출이 없다. 화면 문구는 전부 한국어 리터럴이다.
- `@x-ehr/i18n` 사용처는 2곳 — ① 병원 시간대 Provider 읽기(`RemoteScope.tsx:23`) ② `instantParts`(`hospitalTime.ts:1`).
- 루트에 `i18n:check` 스크립트가 있으나 `--allow-empty` 로 돌아간다(`../../package.json:18`).

```ts
// src/shared/lib/hospitalTime.ts:1-28
import { instantParts } from '@x-ehr/i18n'

/**
 * 병원 시간대 — 화면의 「오늘」 · 조회기간 기본값은 브라우저 시간대가 아니라 이 시간대로 센다.
 * API 의 시각은 전부 병원 시간대의 오프셋 없는 벽시계 값이라 화면은 문자열 그대로 비교 · 표시한다(`T` 앞이 날짜, 뒤가 시각).
 *
 * 값은 host 가 주는 병원 시간대(`@x-ehr/i18n` FormatContext 의 `hospitalTimeZone`)다 — `RemoteScope` 가 렌더마다
 * `syncHospitalTimeZone` 으로 옮긴다. host 가 주지 않으면(standalone · 사용자 정보 복원 전) 기본값 서울이다.
 */

/** 병원 시간대 기본값 — host 가 병원 시간대를 주지 않을 때 쓴다. 백엔드 `HOSPITAL_TIME_ZONE` 기본값과 같다. */
export const DEFAULT_HOSPITAL_TIME_ZONE = 'Asia/Seoul'

let hospitalTimeZone: string = DEFAULT_HOSPITAL_TIME_ZONE

/** host 가 준 병원 시간대로 맞춘다 — 비어 있으면 기본값(서울). 같은 값을 여러 번 넣어도 된다. */
export const syncHospitalTimeZone = (timeZone: string | null | undefined): void => {
  hospitalTimeZone = timeZone || DEFAULT_HOSPITAL_TIME_ZONE
}

/** 병원 기준 오늘 YYYY-MM-DD — 브라우저가 다른 시간대에 있어도 병원 날짜다. */
export const hospitalToday = (now: Date = new Date()): string => instantParts(now.toISOString(), hospitalTimeZone).date

/** YYYY-MM-DD 에서 개월 · 일을 더하고 뺀다 — 날짜만 세므로 시간대와 무관하다(월말 넘침은 Date 규칙 그대로). */
export const shiftYmd = (ymd: string, { months = 0, days = 0 }: { months?: number; days?: number }): string => {
  const [y = 1970, m = 1, d = 1] = ymd.split('-').map(Number)
  return new Date(Date.UTC(y, m - 1 + months, d + days)).toISOString().slice(0, 10)
}
```

날짜 유틸이 병원 시간대를 따르는 지점:

```ts
// src/entities/observation/model/format.ts:78-81
export const todayLocal = (): string => hospitalToday()
export const monthsAgoLocal = (n: number): string => shiftYmd(hospitalToday(), { months: -n })

export const daysAgoLocal = (n: number): string => shiftYmd(hospitalToday(), { days: -n })
```

---

## 11. 컴포넌트 트리 — 검사결과조회(`diagnostic-result-view`) L 폭 기준

```
<RemoteScope>                                   shared/ui/RemoteScope.tsx   (div.x-remote-root.khds-root + ToastProvider, outlet context=auth)
└─ <DiagnosticResultView>                       views/diagnostic-result-view/DiagnosticResultView.tsx
   │  useSessionPatient() → patient / useCurrentUser().email → userId / useContainerTier() → tier / useViewParams(VIEW_KEY)
   └─ <div.dr-tier-root ref data-tier>
      ├─ [patient 없음] <EmptyBody tier>  →  L: <WideMain results={EMPTY_RESULTS} noPatient /> · M·S: <CompactMain ... noPatient />
      └─ [patient 있음] <ResultsBody key={patientId} patientId userId tier viewParams>
         │  results = useDiagnosticResultData({patientId,userId,viewParams,scopeToWidePeriod: tier==='L'})
         │     ├─ useAnalysis(patientId,{auto:true})            entities/analysis/model/useAnalysis.ts
         │     ├─ useRangeKey(patientId) · useWideStore(dateBasis) → useRangedObservations({patientId,rangeKey,dateBasis})
         │     ├─ fetchViewerConfig(userId) → config(favoriteSets · lastViewLayoutCode)
         │     └─ useAgentCommandBridge(patientId)
         │  useLayoutEffect: periodStore.bindPatient · compactStore.resetForPatient · compactStore.applyParams(viewParams)
         ├─ [L] <div.drp-root><section.drp-main>
         │     └─ <WideMain results>                     widgets/diagnostic-result/wide/WideMain.tsx
         │        │  useWideStore(codeFilter, checked) · useWideDerive(observations, favoriteSets.sets)
         │        ├─ <AiSection analyses analysisHistory observations analyzing analyzeError onAnalyze={reanalyze} seriesByCode windowStart>
         │        │     ├─ <SeverityBadge> · <AnalysisWaitingLine> · <ValueSpark>(주의 검사 미니 추이)
         │        │     └─ <AnalysisHistoryDialog>
         │        ├─ <Banner apiError onClose={clearApiError}>
         │        ├─ <ResultChipBar counts onSetChip rangeKey days onRefresh={refresh} refreshing>
         │        │     ├─ <SegmentedControl>(상태) · <Select>(날짜기준 → wideStore.setDateBasis)
         │        │     ├─ <RangeTrack value={rangeKey} onChange→periodStore.setRangeKey days markerDate>   entities/observation/ui/RangeTrack.tsx
         │        │     └─ <Button 조회>
         │        ├─ <CodeFilterBar codes observations>  (openView observationCodes 일 때만)
         │        └─ <div.dr-body>
         │           ├─ <nav><CategoryTree tree favoriteSets>  → <Card><Tree<CategoryNode>> + <FavoriteSetManageDialog actions={favoriteSets}>
         │           ├─ <ObservationList isTime trendDays rows reportSections ... favoriteSets onOpen={openDetail} rangeKey days extending dateBasis>
         │           │     ├─ <section.dr-list><Card>
         │           │     ├─ 툴바: <SegmentedControl>(보기 방식 flat/agg/report/time) · <Select>(정렬) · 검색
         │           │     ├─ <GraphPanel groups allGroups onPickCodes onClear onClose>  → <GraphChart>(킷 Chart) · <ChartModeToggle>
         │           │     ├─ listMode별 표: <DataGrid<Observation>>(flat, columns/flatColumns) · <DataGrid<ObservationGroup>>(agg · time, columns/trendColumns · timeColumns)
         │           │     │     └─ cells/: <FavoriteButton row favoriteSets open onOpenChange> → <FavoriteSetPicker>(features) · <CompareCheck> · <TrendCell> · <AggCell> ...
         │           │     │     └─ renderDetail → <TrendDetail>(RangeSparkline)
         │           │     └─ listMode==='report': <ReportView reportSource reportDates reportSections ...>  → report/reportParts (<DataGrid<ReportRow>> · <BulletChart> · <RecordDocumentFrame>)
         │           └─ <DetailPanel observations favoriteSets>      (detailStore.useOpenDetail() 가 null 이면 렌더 안 함)
         │                 └─ <Card w-[440px]> action=<FavoriteSetPicker>+닫기 · <DetailBody obs observations narrow before={<ViewerButtons>}>
         ├─ [M·S] <div.drp-root><div.dvc-standalone>
         │     └─ <CompactMain observations timeline extending patientId loading error size onOpen={openDetail} lead={<AiBrief tone="agent" ...>}>
         │        │  useCompactStore(...) · useCompactDerive(observations,{category,timeline}) · useWidenOlder(...)
         │        └─ <div.dr-compact-column onScroll onWheel>
         │           ├─ {lead}  (AiBrief → Card · Dropdown(다시 분석 · 이력) · AnalysisHistoryDialog)
         │           └─ <Card.dr-compact-card>
         │              ├─ <DetailToolbar counts category setCategory mode setMode rangeKey onRangeChange days markerDate fold>  (SegmentedControl ×2 + RangeTrack)
         │              └─ mode==='report' ? <ReportPanel dayGroups live shownGroups size focusDate sentinel onOpen>
         │                                     └─ <DateGroup ...> ×N  → <DataGrid<DayItem>>(이상 항목) · <NarrativeBlock>(List)
         │                                 : <RawPanel live rawCodes onClearCodes onOpen sentinel> → <DataGrid<RawRow>>
         └─ [M·S] <DetailDialog observations favoriteSets>   → 킷 <Dialog> · footer <DetailActions>(FavoriteSetPicker + ViewerButtons)
```

props 로 내려가는 핵심 값: `results`(= `DiagnosticResultData` — `observations` · `timeline` · `favoriteSets`(액션 묶음) · `analyses` · `openDetail` · `refresh` · `persistLayout`), `rangeKey`(periodStore 파생), `tier`(폭). 뷰 상태는 props 가 아니라 각 컴포넌트가 zustand 셀렉터로 직접 읽는다.

---

## 12. 이 레포만의 특징 패턴

1. **폭 기반 뷰 전환(tier)** — `useContainerTier`(`shared/lib/useContainerTier.ts:22-42`)가 ResizeObserver 로 *받은 영역*의 폭을 재서 L(≥1200)/M(≥640)/S 로 가르고, 같은 데이터 훅(`useDiagnosticResultData`)을 유지한 채 `WideMain` ↔ `CompactMain` 만 갈아 끼운다(`DiagnosticResultView.tsx:201-236`). 폭이 바뀌면 분류 선택만 두 스토어 사이에서 옮긴다(`:184-199`).
2. **조회 기간 단계 + 더 과거만 덧붙이기** — `rangeSteps.ts:16-21` 의 단계(3m · 6m · 1y · 2y · all, 끝은 항상 오늘). `useRangedObservations` 가 처음엔 `[단계 시작, 오늘]`, 넓힐 땐 `[새 시작, 기존 시작−1일]` 만 읽어 `rows` 뒤에 붙인다(`useRangedObservations.ts:122-143`). 좁힐 땐 재조회 없이 `inRange` 필터(`:165-169`).
3. **기간 슬라이더 `RangeTrack`** — 킷 Slider 로는 시간축 눈금·마커를 못 그려 자체 컴포넌트(`entities/observation/ui/RangeTrack.tsx:14-22`, gaps §29). 단계는 등간격, 구간별 시간 눈금은 piecewise(`rangeSteps.ts:45-74`). 눈금 = `GET /observations/exam-dates` 타임라인, 마커 = 목록이 알린 `positionDate`.
4. **스크롤 넓히기 `useWidenOlder`** — 바닥에서 *사용자가 실제 스크롤·휠을 했을 때만* `periodStore.widenToInclude(기간 밖 가장 최근 검사일)` 로 단계를 올린다(`useWidenOlder.ts:51-89`). 넓혔는데 보이는 행이 안 늘면 이어서 넓힌다(`:79-86`). L 상세조회는 DataGrid `onLoadMore`, M·S 는 `.dr-compact-column` 의 `onScroll/onWheel`(`CompactMain.tsx:163-178`).
5. **누적조회 날짜 묶음 · 검사일별 묶음** — L 누적조회(agg)는 「날짜 1개 = 칸 1개」(`buildTrendDays`), M·S 보고서는 `buildDayGroups`(`dayGroups.ts:43-`)로 검사일별 묶음(수치형 표 + 판독문형 List), 6일씩 페이징(`compactStore.ts:34-35`, `GROUP_PAGE`).
6. **그래프 보기 / 띄워 보는 추이 = RangeSparkline** — 단일 검사 추이는 전부 킷 `RangeSparkline`(`result-visuals.tsx:117-168`, 1회 결과도 점 하나로), 표 칸 미니 추이는 `Sparkline`(`ValueSpark`), 여러 계열 겹치기만 킷 `Chart`(`graph-chart.tsx:94`). 라인/스파이크는 `chartModeStore` 가 화면 간 공유.
7. **AI 분석 — 스트림 없음, POST + 폴링** — `runAnalysis` 는 50초 타임아웃 POST, 넘기면 이력 최신 id 가 바뀔 때까지 5초 간격 90초 폴링(`analysisApi.ts:19-21, 147-160`). 환자별 in-flight Promise 를 공유해 두 화면이 동시에 열려도 1회(`:94-122`). 자동 분석은 `/status` 의 `is_review_target` 일 때만(`useAnalysis.ts:97-114`). 작성중 효과는 `useTypedText`(`shared/lib/useTypedText.ts:15-37`).
8. **보고서 뷰 · 서식 iframe** — L 보기 방식 `report` 는 분류별 보고서(`wide/report/ReportView`), 기록 서식은 `GET /observations/{id}/record-document` HTML 을 완전 sandbox iframe(srcdoc)에만 넣는다(`RecordDocumentFrame.tsx:9-15`).
9. **좁은 폭 간략형 AI 브리핑 · 스크롤 하나** — M·S 는 `AiBrief`(`entities/analysis/ui/AiBrief.tsx`)를 `CompactMain` 의 `lead` 로 넘겨 기간 툴바·결과와 한 스크롤 틀(`.dr-compact-column`, `styles.css:66-70`)에서 함께 스크롤한다. ✦ 아이콘 Dropdown 이 「다시 분석 · 이력」.
10. **즐겨찾기 세트** — 즐겨찾기는 세트 단위(한 검사가 여러 세트), 체크 = 즉시 저장 + 재조회(§5), 세트가 없으면 저장 안 된 「기본」을 보여 주고 첫 체크 때 만든다(`favoriteSets.ts:39-42`). 트리 노드 키 `fav/<id>` 로 필터(`categoryTree.ts:23-27`), 상한 20 · 이름 30자(`favoriteSets.ts:12-13`).
11. **openView 파라미터 계약** — `ViewParams`(`events.ts:32-46`)를 replayLast 로 발행하고 URL 쿼리에도 반영해 새로고침·딥링크를 재현(`viewBus.ts:24-52`). 수신 화면은 세션 환자와 일치할 때만 적용(`DiagnosticResultView.tsx:95-100`). 좁은 폭은 `compactStore.applyParams`(`compactStore.ts:102-141`), 넓은 폭은 `applyAgentCommand` + `focusReportDate`(`useDiagnosticResultData.ts:200-219`).
12. **브라우저 저장 키 — 수명 분리** — sessionStorage: `dr.chart.mode` · `dr.trend.days` · `dr.graph.hourly` · `dr.report.layout` / localStorage: `dr.date.basis` · `dr.category.multi`(`wideStore.ts:41-96`). 서버 저장은 `last_view_layout_code` 하나(정렬·기간은 저장하지 않음).

---

## 13. 신입이 헷갈릴 포인트 10

1. **react-query 를 쓰지 않는다.** `QueryClient` 는 `app/Standalone.tsx:47` 에만 있고 화면 조회는 전부 `useEffect` + 「요청 키로 로딩 파생」 패턴이다(`useRangedObservations.ts:74-80`, `usePatientSearch.ts:59-62`). `useQuery` 를 찾지 말 것.
2. **effect 본문에서 동기 `setState` 금지(lint).** 그래서 `loading` 을 state 로 두지 않고 `loaded?.key !== requestKey` 로 파생한다(`usePatientSearch.ts:59-62`, `useAnalysis.ts:50-53`). 넓히기 오류를 배너로 올릴 땐 `queueMicrotask` 로 우회(`useDiagnosticResultData.ts:180-189`).
3. **환자는 화면이 고르지 않는다 — host 세션(`usePatientContextStore`)만 읽는다.** `useSessionPatient`(`sessionPatient.ts:42-45`)가 유일한 입구. `ehr:patient:selected` 버스 이벤트는 dev 전용 화면이 발행하고 `Standalone.tsx:176-180` 의 대역이 세션으로 올린다. 운영 화면은 그 이벤트를 구독하지 않는다.
4. **환자가 바뀌면 remount(`key={patient.patientId}`)다.** `DiagnosticResultView.tsx:107-108`. 그래서 훅 내부에 「환자 변경 감지」 로직이 거의 없고, 대신 전역 zustand 스토어는 `resetForPatient()` 로 비워야 한다(`useDiagnosticResultData.ts:158-159`, `EmptyBody` `:128-132`).
5. **zustand 스토어가 5개고 수명이 다르다.** `periodStore` 는 환자에 묶이고(`bindPatient`), `wideStore` 는 `WideState`(환자마다 리셋)와 `WideDisplayState`(유지, `wideStore.ts:131-146`)가 한 스토어에 섞여 있다. `resetForPatient` 가 `trendDays` · `dateBasis` 를 건드리지 않는 이유.
6. **snake_case ↔ camelCase 는 수동 매핑이다.** 자동 변환 없음(`observationApi.ts:9-10` 주석 — 값에 밑줄이 든 데이터 때문). 새 필드는 `toObservation` 등 매퍼에 직접 추가해야 화면에 나온다. 요청 파라미터도 `patient_id` 처럼 직접 snake 로 쓴다(`:137-142`).
7. **mock json 은 camelCase, 실서버는 snake_case.** mock 은 매퍼를 거치지 않으므로(`observationApi.ts:130-135`) mock 에서만 보이는 필드(`observationCategory` · `resultNumericValue`, `types.ts:67-77`)가 있다. mock 모드는 `window.__DIAGNOSTIC_RESULT_API_BASE__='mock'` + standalone 에서만 등록된다(`mockStore.ts:16-20`).
8. **`PREFIX` 를 프록시가 벗기지 않는다.** `/api/v1/diagnostic-result` 는 백엔드 컨트롤러 매핑이 소유한다(`rspack.config.js:18-25`). standalone 에서 「응답 형식이 올바르지 않습니다」가 뜨면 백엔드 미기동 → SPA fallback HTML 을 받은 것이다.
9. **`RemoteScope` 의 `x-remote-root khds-root` 두 클래스와 `display: contents`.** 빼면 킷 컴포넌트가 브라우저 기본 모양으로 풀리고(`RemoteScope.tsx:11-12`), 박스를 만들면 높이 체인이 끊긴다(`styles.css:10-15`). `Standalone` 셸은 RemoteScope 밖이라 `khds-root` 를 따로 단다(`Standalone.tsx:198-199`).
10. **AI 분석 POST 가 「실패」처럼 보여도 서버는 계속 돈다.** api-axios 기본 10초/상한 50초(`dist/index.js:107-108`) 때문에 브라우저가 먼저 끊고(개발자 도구 `(canceled)`), 화면은 `timedOut` 을 보고 폴링으로 전환한다(`analysisApi.ts:138-140, 147-160`). `fetchAnalysesForPatient` 는 throw 하지 않고 `{data|error}` 를 돌려준다(`:59-72`) — 다른 API 와 반환 규약이 다르다.
11. (덤) **즐겨찾기 Popover 의 열림은 셀 밖이 든다.** 표 행이 다시 그려지면 셀 state 가 날아가 창이 닫히므로 `openKey` 를 목록이 소유한다(`FavoriteSetPicker.tsx:14`, `FavoriteButton.tsx:9-10`). 그리고 트리거·패널은 `stopPropagation` 으로 행 클릭(=상세 열기)을 막는다(`FavoriteSetPicker.tsx:25-31`).
