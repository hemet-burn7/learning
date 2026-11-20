# x-nutrition / backend / foodservice — 신입 교육용 소스 수집

- 모듈 루트: `x-nutrition/backend/foodservice`
- 코드 루트: `src/main/kotlin/com/projectx/foodservice/` (이하 경로는 모듈 루트 기준 상대경로)
- 스택: Kotlin · Spring Boot · JPA(Command) + QueryDSL(Query) · Liquibase · common-eventbus-kt(Kafka)
- 본문 코드 인용은 전부 실제 소스 그대로이며, 인용마다 `경로:시작줄-끝줄` 을 붙였다.

---

## 1. 패키지 트리

### 1.1 main (99 파일 · 16,263 줄)

**presentation (9 파일)** — Controller + 요청 id 생성 + 토큰 principal

```
presentation/
├─ diet_masters_controller.kt            (279)  식단·끼니·끼니속성 마스터 API
├─ foodservice_dashboard_controller.kt   ( 96)  대시보드 조회 API
├─ foodservice_settings_controller.kt    (173)  영양팀 설정(끼니 유형·마감) API
├─ meal_change_requests_controller.kt    (201)  세부오더 변경요청 API
├─ meal_closings_controller.kt           (145)  마감 회차 API
├─ meal_orders_controller.kt             (318)  세부오더(식사오더) API
├─ meal_preparations_controller.kt       (260)  배식준비(식수) API
├─ request_ids.kt                        ( 15)  요청 id 생성 헬퍼
└─ token_principal_scope.kt              ( 19)  JWT principal 에서 tenant/user 추출
```

**application (29 파일)** — Command/Handler/Validator · Query IF · view_model · 통합이벤트 핸들러

```
application/
├─ commands/
│  ├─ command.kt                              ( 13)  i_command 마커
│  ├─ command_bus.kt                          ( 36)  command_bus (디스패치)
│  ├─ foodservice_commands.kt                 (498)  모든 command data class
│  ├─ diet/diet_command_handlers.kt           (264)
│  ├─ dietorder/diet_order_command_handlers.kt(410)
│  ├─ mealchangerequest/meal_change_request_command_handlers.kt (441)
│  ├─ mealclosing/meal_closing_command_handlers.kt (206)
│  ├─ mealorder/meal_order_auto_setting.kt    (184)  처방→세부오더 자동 설정 도출
│  ├─ mealorder/meal_order_command_handlers.kt(352)
│  ├─ mealpreparation/meal_preparation_command_handlers.kt (227)
│  └─ setting/foodservice_setting_command_handlers.kt (154)
├─ integrationevents/
│  ├─ integration_event.kt                    ( 11)  i_integration_event 마커
│  ├─ events/diet_integration_event.kt        (110)  수신 payload: diet-order (@MessageType …)
│  ├─ events/inbound_integration_events.kt    (160)  수신 payload: discharge-order 등
│  ├─ events/outbound_integration_events.kt   (293)  발행 payload: meal-order 등
│  └─ eventhandling/
│     ├─ idempotent_integration_event_handler.kt ( 47)  핸들러 베이스(멱등)
│     ├─ diet_order_event_handlers.kt         (290)  식단 처방 수신 핸들러
│     ├─ discharge_order_event_handlers.kt    ( 68)  퇴원지시 수신 핸들러
│     ├─ event_identity_resolver.kt           ( 57)  봉투 → 행위자 id
│     ├─ event_tenant_resolver.kt             ( 59)  봉투 → tenant
│     └─ not_served_applier.kt                ( 84)  퇴식(not served) 적용
├─ ports/
│  ├─ i_deployment_tenant_port.kt             ( 14)
│  └─ i_shared_encounter_reader.kt            ( 15)  shared.encounter 읽기 포트
├─ queries/
│  ├─ foodservice_queries.kt                  (153)  Query IF 전부
│  └─ foodservice_view_models.kt              (824)  view_model 전부
├─ support/
│  ├─ allowed_actions.kt                      ( 89)  화면 버튼 허용 여부 계산
│  ├─ foodservice_support.kt                  ( 22)
│  └─ meal_selection.kt                       (126)  끼니 선택 규칙
└─ validations/
   ├─ command_validator.kt                    ( 11)  i_command_validator
   └─ foodservice_command_validators.kt       (389)  command 별 validator
```

**domain (34 파일)** — Aggregate Root · Entity · VO · enum · Repository IF · 도메인 이벤트/예외 · seedwork

```
domain/
├─ aggregatesmodel/
│  ├─ diet/            diet.kt(450) meal.kt(148) meal_attribute.kt(389) diet_defaults.kt(43) i_diet_repository.kt(11)
│  ├─ dietorder/       diet_order.kt(392) order_value_objects.kt(111) i_diet_order_repository.kt(18)
│  ├─ master/          foodservice_masters.kt(52) i_master_readers.kt(184)
│  ├─ mealchangerequest/ meal_change_request.kt(285) i_meal_change_request_repository.kt(15)
│  ├─ mealorder/       meal_order.kt(384) meal_order_revision.kt(295) i_meal_order_repository.kt(36)
│  ├─ mealpreparation/ meal_preparation.kt(517) meal_closing.kt(106) i_meal_preparation_repository.kt(36)
│  └─ setting/         foodservice_setting.kt(278) meal_closing_setting.kt(149) meal_type_setting.kt(146) i_foodservice_setting_repository.kt(11)
├─ common/             foodservice_enums.kt(342) hospital_zone.kt(6)
├─ events/             foodservice_domain_events.kt(204)
├─ exceptions/         foodservice_exceptions.kt(353)
├─ seedwork/           entity.kt(130) seedwork.kt(20) value_object.kt(20) tenant_scope.kt(26) uuid_format.kt(9) string_list_converter.kt(20)
└─ services/           meal_closing_deadline_policy.kt(135)
```

**infrastructure (26 파일)** — Spring 설정 · Repository 어댑터 · QueryDSL 구현 · EventBus 배선 · shared 스키마 엔티티

```
infrastructure/
├─ foodservice_context.kt                 (440)  JPA 엔티티 등록/빈 배선
├─ domain_event_dispatcher.kt             ( 19)  도메인 이벤트 → Spring 이벤트
├─ config/
│  ├─ db_schema_initializer.kt            ( 75)  CREATE SCHEMA IF NOT EXISTS (Liquibase 앞)
│  ├─ deployment_tenant_adapter.kt        ( 47)
│  ├─ dotenv_environment_post_processor.kt( 25)  backend/.env 로딩
│  ├─ foodservice_configs.kt              ( 83)  JPAQueryFactory 등
│  ├─ global_exception_handler.kt         (127)  @RestControllerAdvice
│  └─ root_swagger_redirect_config.kt     ( 42)
├─ eventbus/
│  ├─ domain_event_integration_publisher.kt ( 56)  AFTER_COMMIT → Kafka 발행
│  └─ topic_registry.kt                   ( 67)  토픽 레지스트리
├─ persistence/audit_entity_listener.kt   ( 43)  감사 컬럼 자동 채움
├─ proxies/ward_master_proxy.kt           ( 69)
├─ queries/
│  ├─ dashboard_query_dsl.kt              (259)
│  ├─ diet_order_query_dsl.kt             (261)
│  ├─ meal_change_request_query_dsl.kt    (143)
│  ├─ meal_closing_query_dsl.kt           (473)
│  ├─ meal_order_query_dsl.kt             (304)
│  ├─ meal_preparation_query_dsl.kt       (486)
│  ├─ query_support.kt                    ( 85)
│  └─ shared_directory.kt                 (152)  shared 환자·수진 조회 헬퍼
├─ repositories/foodservice_repositories.kt (372)  JPA 어댑터 전부
└─ shared/
   ├─ shared_patient.kt (54) shared_encounter.kt (41) shared_department.kt (28) shared_user_account.kt (35)
   └─ shared_encounter_reader.kt (23)
```

**루트** — `foodservice_application.kt` (20) 메인 클래스

**resources**

```
src/main/resources/
├─ application.yml · application-dev.yml
├─ META-INF/orm.xml · META-INF/spring.factories
├─ common-libs/common-masking.yaml · common-observability.yaml
└─ db/
   ├─ changelog/db.changelog-master.yaml
   ├─ changelog/changes/001-create-tables.xml · 002-seed-operational-master.xml · 003-diet-category-meal-form.xml
   │                    900-seed-master.xml · 901-seed-diet-category.xml
   └─ manual/014-reset-schema.sql · 015-relax-meal-form-salt-not-null.sql · 016-drop-diet-flag-columns.sql   (master include 금지 · 사람 실행)
```

### 1.2 test (18 파일, `src/test/resources` 없음)

```
src/test/kotlin/com/projectx/foodservice/
├─ architecture/layer_isolation_test.kt             ArchUnit 레이어 가드
├─ application/meal_order_auto_setting_test.kt
├─ domain/diet_category_test.kt · diet_order_test.kt · diet_test.kt · foodservice_setting_test.kt
│         meal_change_request_test.kt · meal_order_test.kt · meal_preparation_test.kt
├─ integrationevents/diet_integration_event_contract_test.kt · diet_order_identity_test.kt
│                    event_tenant_resolution_test.kt · test_tenant_support.kt
├─ persistence/audit_and_tenant_columns_test.kt · schema_mapping_test.kt · shared_join_query_test.kt · query_test_support.kt
└─ test_ids.kt
```

---

## 2. API 추적표

### 2.0 공통 사항 (모든 컨트롤러)

- 모든 컨트롤러가 `@RestController` + `@RequireAuth`(JWT) + 클래스 레벨 `@RequestMapping("/api/v1/foodservice…")`. **prefix 를 컨트롤러가 소유**한다(앱 context-path 없음).
- 테넌트 범위 · 작업자 id 는 **토큰(`@TokenPrincipal`)에서만** 꺼낸다 (`presentation/token_principal_scope.kt:14-19`). 본문·경로로 받지 않는다.
- 디스패치: Controller → `command_bus.send<TResult>(command)` → (validator 들) → handler. **한 엔드포인트 = 한 Command** (`application/commands/command_bus.kt:24-30`).
- **트랜잭션 경계 = 핸들러 `handle()` 메서드의 `@Transactional`** (예: `application/commands/mealorder/meal_order_command_handlers.kt:77`). Controller 에는 `@Transactional` 이 없다. Command 응답은 **같은 요청 안에서 Query 를 다시 불러** view_model 을 돌려준다(두 개의 트랜잭션: 쓰기 1 + 읽기 1).
- 조회 구현은 전부 `@Repository` 클래스의 메서드 단위 `@Transactional(readOnly = true)` (예: `infrastructure/queries/meal_order_query_dsl.kt:45`).
- 조회 구현 클래스 ↔ Query IF 매핑:
  - `diet_order_query_dsl` → `i_diet_order_queries` (`infrastructure/queries/diet_order_query_dsl.kt:34-44`)
  - `meal_order_query_dsl` → `i_meal_order_queries` (`meal_order_query_dsl.kt:33-43`)
  - `meal_preparation_query_dsl` → `i_meal_preparation_queries` (`meal_preparation_query_dsl.kt:44-55`)
  - `meal_closing_query_dsl` → **`i_meal_closing_queries` + `i_foodservice_master_queries` + `i_foodservice_setting_queries` 세 IF 를 한 클래스가 구현** (`meal_closing_query_dsl.kt:61-80`)
  - `meal_change_request_query_dsl` → `i_meal_change_request_queries` (`meal_change_request_query_dsl.kt:23-32`)
  - `dashboard_query_dsl` → `i_dashboard_queries` (`dashboard_query_dsl.kt:36-51`)
- 테이블 ↔ 엔티티: `diet`·`meal`·`meal_attribute`(Diet Aggregate) / `diet_order` / `meal_order`·`meal_order_revision` / `meal_preparation`·`meal_closing` / `meal_change_request` / `foodservice_setting`·`meal_type_setting`·`meal_closing_setting` / `meal_form`(마스터) / shared 스키마 읽기 전용: `shared.patient`·`shared.encounter`·`shared.department`·`shared.user_account`.

### 2.1 `meal_orders_controller` — `/api/v1/foodservice` (`presentation/meal_orders_controller.kt`)

| HTTP | 경로 | Controller.메서드 | Command/Query 클래스 | Handler / Query 구현.메서드 | 접근 Repository / Q타입 | 테이블 | 응답 view_model |
|---|---|---|---|---|---|---|---|
| GET | `/diet-orders` | `search_diet_orders` (:65-91) | `diet_order_search_filter` → `i_diet_order_queries.search` | `diet_order_query_dsl.search` (:87-) RO-Tx | `Qdiet_order`, `Qmeal_order`, `shared_directory`(Qshared_patient/encounter/department/user_account) | diet_order, meal_order, shared.patient, shared.encounter | `page_view_model<diet_order_view_model>` |
| GET | `/diet-orders/{doid}` | `find_diet_order` (:93-96) | `i_diet_order_queries.find_by_id` | `diet_order_query_dsl.find_by_id` (:76-) RO-Tx | 동상 + `i_meal_order_queries.find_by_diet_order_id` | diet_order, meal_order, meal_preparation, shared.* | `diet_order_view_model` (없으면 `diet_order_not_found_exception` 404) |
| GET | `/diet-orders/{doid}/meal-orders` | `find_meal_orders` (:98-101) | `i_meal_order_queries.find_by_diet_order_id` | `meal_order_query_dsl.find_by_diet_order_id` (:52-) RO-Tx | `Qmeal_order`, `Qdiet_order`, `Qmeal_preparation` | meal_order, diet_order, meal_preparation | `List<meal_order_view_model>` |
| PUT | `/diet-orders/{doid}/discharge-meal` | `set_discharge_meal` (:103-117) | `set_discharge_meal_command` (commands:116-121) | `set_discharge_meal_command_handler.handle` (diet_order_command_handlers.kt:350-361) `@Transactional` | `i_diet_order_repository`, `i_meal_order_repository`, `i_meal_preparation_repository`, `i_foodservice_setting_reader` | diet_order, meal_order, meal_preparation(+meal_closing), foodservice_setting/meal_type_setting(읽기) | `diet_order_view_model` (재조회) |
| GET | `/wards/{ward_id}/meal-order-board` | `find_board` (:121-130) | `i_meal_order_queries.find_board` | `meal_order_query_dsl.find_board` (:73-) RO-Tx | `Qdiet_order`, `Qmeal_order`, `Qmeal_preparation`, `meal_type_resolver`, `shared_directory` | diet_order, meal_order, meal_preparation, meal_type_setting, shared.patient/encounter | `meal_order_board_view_model` |
| POST | `/meal-orders` | `issue` (:134-146) 201 Created | `issue_meal_order_command` (commands:140-166) + validator `issue_meal_order_command_validator` (validators:36-39) | `issue_meal_order_command_handler.handle` (meal_order_command_handlers.kt:65-77) `@Transactional` | `i_diet_order_repository`, `i_meal_order_repository`, `i_diet_type_reader`, `i_meal_form_reader`, `i_foodservice_setting_reader` | diet_order(읽기), **meal_order + meal_order_revision(INSERT)**, diet/meal/meal_attribute·meal_form·foodservice_setting(읽기) | `meal_order_view_model` (재조회) |
| PUT | `/meal-orders/{moid}` | `revise` (:148-182) | `revise_meal_order_command` (commands:174-195) + `revise_meal_order_command_validator` (validators:61-64) | `revise_meal_order_command_handler.handle` (:188-198) `@Transactional` | `i_meal_order_repository`, `i_meal_preparation_repository`, `i_diet_type_reader` | meal_order, meal_order_revision, meal_preparation(마감 여부 확인 → 409 FS-4091) | `meal_order_view_model` |
| POST | `/meal-orders/{moid}/revoke` | `revoke` (:184-194) | `revoke_meal_order_command` (commands:198-202) | `revoke_meal_order_command_handler.handle` (:280-289) `@Transactional` | `i_meal_order_repository`, `i_meal_preparation_repository` | meal_order, meal_preparation(마감 전 접수자료 취소) | `meal_order_view_model` |
| GET | `/meal-orders/{moid}` | `find` (:196-198) | `i_meal_order_queries.find_by_id` | `meal_order_query_dsl.find_by_id` (:45-50) RO-Tx | `Qmeal_order`, `Qmeal_preparation` | meal_order, meal_preparation | `meal_order_view_model` |

### 2.2 `diet_masters_controller` — `/api/v1/foodservice/masters/diets` (`presentation/diet_masters_controller.kt`)

| HTTP | 경로 | Controller.메서드 | Command/Query | Handler / Query 구현 | Repository / Q타입 | 테이블 | 응답 |
|---|---|---|---|---|---|---|---|
| GET | `` (목록) | `find_diets` (:55-63) | `i_foodservice_master_queries.find_diets(active_only)` | `meal_closing_query_dsl.find_diets` (:295-) RO-Tx | `Qdiet`, `Qmeal`, `Qmeal_attribute` | diet, meal, meal_attribute | `List<diet_view_model>` |
| GET | `/{diet_id}` | `find_diet` (:65-67) | `find_diet` | `meal_closing_query_dsl.find_diet` (:304-) RO-Tx | 동상 | 동상 | `diet_view_model` |
| POST | `` | `create` (:69-80) 201 | `create_diet_command` (commands:351-359) + `create_diet_command_validator` (validators:200-201) | `create_diet_command_handler.handle` (diet_command_handlers.kt:33-41) `@Transactional` | `i_diet_repository` | diet | `diet_view_model` |
| PUT | `/{diet_id}` | `save` (:82-98) | `save_diet_command` (commands:405-417) + `save_diet_command_validator` (validators:213-214) | `save_diet_command_handler.handle` (:70-79) `@Transactional` | `i_diet_repository`, `i_meal_form_reader` | diet, meal(upsert), meal_attribute(전체 교체), meal_form(읽기) | `diet_view_model` (version 불일치 409 FS-4094) |
| PUT | `/{diet_id}/defaults` | `save_defaults` (:100-114) | `save_diet_defaults_command` (commands:423-437) | `save_diet_defaults_command_handler.handle` (:234-242) `@Transactional` | `i_diet_repository` | diet, meal_attribute(default_value) | `diet_view_model` |

### 2.3 `foodservice_dashboard_controller` — `/api/v1/foodservice` (`presentation/foodservice_dashboard_controller.kt`)

| HTTP | 경로 | Controller.메서드 | Query | 구현 | Q타입 | 테이블 | 응답 |
|---|---|---|---|---|---|---|---|
| GET | `/dashboard/summary` | `summary` (:46-49) | `i_dashboard_queries.find_summary` | `dashboard_query_dsl.find_summary` (:53-54) RO-Tx | `Qmeal_preparation`, `Qmeal_change_request`, `Qdiet_order` | meal_preparation, meal_change_request, diet_order | `dashboard_summary_view_model` |
| GET | `/dashboard/closing-status` | `closing_status` (:51-54) | `find_closing_status` | `dashboard_query_dsl.find_closing_status` (:116-117) → 내부에서 `i_meal_closing_queries.find_closing_status` + `meal_closing_deadline_policy` | `Qmeal_preparation`, `Qmeal_closing`, 설정 reader | meal_preparation, meal_closing, foodservice_setting/meal_closing_setting | `closing_status_view_model` |
| GET | `/dashboard/recent-events` | `recent_events` (:56-62) | `find_recent_events` | `dashboard_query_dsl.find_recent_events` (:147-148) RO-Tx | `Qdiet_order`, `Qmeal_preparation`, `Qmeal_change_request`, `Qmeal_closing`, shared | 4 테이블 합성 + shared.patient | `List<recent_event_view_model>` |
| GET | `/meal-statistics/daily` | `daily_statistics` (:66-72) | `i_meal_preparation_queries.find_daily_statistics` | `meal_preparation_query_dsl.find_daily_statistics` (:338-339) RO-Tx | `Qmeal_preparation` | meal_preparation | `daily_statistics_view_model` |
| GET | `/masters/diet-types` | `diet_types` (:76-82) | `i_foodservice_master_queries.find_diet_types` | `meal_closing_query_dsl.find_diet_types` (:271-272) RO-Tx | `Qdiet` | diet | `List<diet_type_view_model>` |
| GET | `/masters/meal-forms` | `meal_forms` (:84-89) | `find_meal_forms` | `meal_closing_query_dsl.find_meal_forms` (:381-382) RO-Tx | `Qmeal_form` | meal_form | `List<meal_form_view_model>` |
| GET | `/masters/wards` | `wards` (:91-93) — **principal 없음, 테넌트 범위 없음** | `find_wards` | `meal_closing_query_dsl.find_wards` (:394) → `i_ward_reader`(`infrastructure/proxies/ward_master_proxy.kt`, x-ancillary-main HTTP) | DB 아님 | — | `List<ward_view_model>` |

### 2.4 `foodservice_settings_controller` — `/api/v1/foodservice/settings` (`presentation/foodservice_settings_controller.kt`)

| HTTP | 경로 | Controller.메서드 | Command/Query | Handler / 구현 | Repository / Q타입 | 테이블 | 응답 |
|---|---|---|---|---|---|---|---|
| GET | `` | `find_setting` (:46-51) | `i_foodservice_setting_queries.find_setting` | `meal_closing_query_dsl.find_setting` (:400-401) RO-Tx | `Qfoodservice_setting`, `Qmeal_type_setting`, `Qmeal_closing_setting` | foodservice_setting, meal_type_setting, meal_closing_setting | `foodservice_setting_view_model` |
| GET | `/meal-types` | `active_meal_types` (:53-55) | `find_active_meal_types` | `meal_closing_query_dsl.find_active_meal_types` (:458) → `meal_type_resolver.labels` | `Qmeal_type_setting` | meal_type_setting | `List<meal_type_label_view_model>` |
| PUT | `/meal-types` | `save_meal_types` (:57-67) | `save_meal_type_settings_command` (commands:465-470) + `save_meal_type_settings_command_validator` (validators:292-293) | `save_meal_type_settings_command_handler.handle` (foodservice_setting_command_handlers.kt:29-38) `@Transactional` | `i_foodservice_setting_repository`, `i_meal_order_repository`(활성 세부오더 존재 → 422 FS-4008) | foodservice_setting, meal_type_setting(upsert), meal_order(읽기) | `foodservice_setting_view_model` |
| PUT | `/closing-rounds` | `save_closing_rounds` (:69-79) | `save_closing_round_settings_command` (commands:492-496) + validator (validators:318-319) | `save_closing_round_settings_command_handler.handle` (:83-91) `@Transactional` | `i_foodservice_setting_repository` | foodservice_setting, meal_closing_setting(전체 교체) | `foodservice_setting_view_model` |

### 2.5 `meal_change_requests_controller` — `/api/v1/foodservice/meal-change-requests` (`presentation/meal_change_requests_controller.kt`)

| HTTP | 경로 | Controller.메서드 | Command/Query | Handler / 구현 | Repository / Q타입 | 테이블 | 응답 |
|---|---|---|---|---|---|---|---|
| GET | `` | `search` (:57-77) | `meal_change_request_search_filter` → `i_meal_change_request_queries.search` | `meal_change_request_query_dsl.search` (:39-40) RO-Tx | `Qmeal_change_request`, shared | meal_change_request, shared.patient/user_account | `worklist_view_model<meal_change_request_view_model>` |
| GET | `/{mcrid}` | `find` (:79-81) | `find_by_id` | `meal_change_request_query_dsl.find_by_id` (:34-35) RO-Tx | 동상 | 동상 | `meal_change_request_view_model` |
| POST | `` | `request` (:85-96) 201 | `request_meal_change_command` (commands:303-315) + `request_meal_change_command_validator` (validators:133-136) | `request_meal_change_command_handler.handle` (meal_change_request_command_handlers.kt:57-70) `@Transactional` | `i_meal_change_request_repository`, `i_meal_preparation_repository`, `i_meal_order_repository`, `i_diet_order_repository`, `i_diet_type_reader`, `i_foodservice_setting_reader` | **meal_change_request(INSERT)**, meal_preparation/meal_order/diet_order/diet(읽기) | `meal_change_request_view_model` |
| POST | `/{mcrid}/confirm` | `confirm` (:98-107) | `confirm_meal_change_command` (commands:318-323) | `confirm_meal_change_command_handler.handle` (:214-228) `@Transactional` (+`change_request_applier` :364-367, `foodservice_policy`) | request·preparation·meal_order·diet_order repo, `i_diet_type_reader` | meal_change_request, meal_preparation(ADD 는 INSERT · 즉시반영이면 스냅샷 갱신/취소) | `meal_change_request_view_model` |
| POST | `/{mcrid}/apply` | `apply` (:109-118) | `apply_meal_change_command` (commands:326-329) | `apply_meal_change_command_handler.handle` (:317-326) `@Transactional` | `i_meal_change_request_repository` + `change_request_applier` | meal_change_request, meal_preparation | `meal_change_request_view_model` |
| POST | `/{mcrid}/reject` | `reject` (:120-132) | `reject_meal_change_command` (commands:332-338) | `reject_meal_change_command_handler.handle` (:343-349) `@Transactional` | `i_meal_change_request_repository` | meal_change_request | `meal_change_request_view_model` |

### 2.6 `meal_closings_controller` — `/api/v1/foodservice/meal-closings` (`presentation/meal_closings_controller.kt`)

| HTTP | 경로 | Controller.메서드 | Command/Query | Handler / 구현 | Repository / Q타입 | 테이블 | 응답 |
|---|---|---|---|---|---|---|---|
| GET | `/board` | `board` (:48-54) | `i_meal_closing_queries.find_board` | `meal_closing_query_dsl.find_board` (:83-84) RO-Tx | `Qmeal_preparation`, `Qmeal_closing`, `Qmeal_closing_setting`, `i_ward_reader` | meal_preparation, meal_closing, meal_closing_setting, meal_type_setting | `closing_board_view_model` |
| GET | `/histories` | `histories` (:56-65) | `find_histories` | `meal_closing_query_dsl.find_histories` (:134-135) RO-Tx | `Qmeal_closing`, `Qmeal_preparation`, shared(user_account) | meal_closing, meal_preparation, shared.user_account | `page_view_model<closing_history_view_model>` |
| POST | `/close` | `close` (:69-88) | `close_meals_command` (commands:261-269) + `close_meals_command_validator` (validators:110-111) | `close_meals_command_handler.handle` (meal_closing_command_handlers.kt:46-60) `@Transactional` | `i_meal_preparation_repository`, `i_meal_change_request_repository`, `i_foodservice_setting_reader`, `i_diet_type_reader`, `meal_closing_deadline_policy`, **`domain_event_dispatcher` 직접 주입** | meal_preparation(status/closed_round), **meal_closing(INSERT)**, meal_change_request(2차 이상에서 확인 건 반영) | `closing_result_view_model` (컨트롤러가 `closing_command_result` 를 직접 변환 :106-119 — 재조회 없음) |
| POST | `/reopen` | `reopen` (:90-104) | `reopen_meals_command` (commands:272-280) + validator (:121-122) | `reopen_meals_command_handler.handle` (:138-147) `@Transactional` | `i_meal_preparation_repository`, `domain_event_dispatcher` | meal_preparation, meal_closing(REOPEN 이력) | `closing_result_view_model` |

### 2.7 `meal_preparations_controller` — `/api/v1/foodservice/meal-preparations` (`presentation/meal_preparations_controller.kt`)

| HTTP | 경로 | Controller.메서드 | Command/Query | Handler / 구현 | Repository / Q타입 | 테이블 | 응답 |
|---|---|---|---|---|---|---|---|
| GET | `` | `search` (:63-96) — **조회 전에 `generate_meal_preparations_command` 를 먼저 send** (:80-82, meal_type_code 가 있을 때만) | `generate_meal_preparations_command` + `meal_preparation_search_filter` → `i_meal_preparation_queries.search` | `generate_meal_preparations_command_handler.handle` (meal_preparation_command_handlers.kt:121-127) `@Transactional` → `meal_preparation_generator` (:153-158) / 이어서 `meal_preparation_query_dsl.search` (:71-72) RO-Tx | generator: `i_meal_order_repository`, `i_diet_order_repository`, `i_meal_preparation_repository`, `i_foodservice_setting_reader` / query: `Qmeal_preparation`, shared, `i_ward_reader` | **meal_preparation(INSERT)** 후 meal_preparation + shared.patient 읽기 | `worklist_view_model<meal_preparation_view_model>` |
| GET | `/aggregations/diet-types` | `aggregate_by_diet_type` (:98-104) | `aggregate_by_diet_type` | `meal_preparation_query_dsl.aggregate_by_diet_type` (:110-111) RO-Tx | `Qmeal_preparation`(스냅샷 컬럼만) | meal_preparation | `diet_type_aggregation_view_model` |
| GET | `/aggregations/wards` | `aggregate_by_ward` (:106-112) | `aggregate_by_ward` | `…aggregate_by_ward` (:188-189) RO-Tx | 동상 + `i_ward_reader` | meal_preparation | `ward_aggregation_view_model` |
| GET | `/serving-sheet` | `serving_sheet` (:119-134) (`format!=json` → 400 FS-4001) | `find_serving_sheet` | `…find_serving_sheet` (:250-251) RO-Tx | 동상 + shared.patient | meal_preparation, shared.patient | `serving_sheet_view_model` |
| GET | `/not-served` | `not_served` (:136-142) | `find_not_served` | `…find_not_served` (:307-308) RO-Tx | `Qmeal_preparation` | meal_preparation | `not_served_view_model` |
| GET | `/{mpid}` | `find` (:144-146) | `find_by_id` | `…find_by_id` (:65-66) RO-Tx | 동상 | meal_preparation(+meal_closing) | `meal_preparation_view_model` |
| POST | `/{mpid}/start` | `start` (:150-156) | `start_meal_preparation_command` (commands:214) | `start_meal_preparation_command_handler.handle` (:34-40) `@Transactional` | `i_meal_preparation_repository` | meal_preparation | `meal_preparation_view_model` |
| POST | `/{mpid}/complete` | `complete` (:158-164) | `complete_meal_preparation_command` (:217) | `complete_meal_preparation_command_handler.handle` (:50-58) `@Transactional` | 동상 | meal_preparation | 동상 |
| POST | `/{mpid}/not-served` | `mark_not_served` (:166-185) (`automatic=false` 고정) | `mark_meal_not_served_command` (:220-227) | `mark_meal_not_served_command_handler.handle` (:72-80) `@Transactional` | 동상 | meal_preparation | 동상 |
| POST | `/{mpid}/restore` | `restore` (:187-199) | `restore_meal_preparation_command` (:230-234) | `restore_meal_preparation_command_handler.handle` (:102-108) `@Transactional` | 동상 | meal_preparation, meal_closing(이력 유무로 복원 상태 결정) | 동상 |
| POST | `/generate` | `generate` (:201-226) 201/200 | `generate_meal_preparations_command` (:240-245) + `generate_meal_preparations_command_validator` (validators:102-103) | 위와 동일 handler | 위와 동일 | meal_preparation(INSERT) | `generate_response` (컨트롤러 로컬 DTO :252-260) |

### 2.8 HTTP 가 아닌 Command 진입(이벤트 컨슈머)

| 진입 | Command | Handler | 비고 |
|---|---|---|---|
| Kafka `diet-order` 수신 | `receive_diet_order_command` (commands:60-83) · `revoke_diet_order_command` (:86-90) | `receive_diet_order_command_handler` (diet_order_command_handlers.kt:50-61) · `revoke_diet_order_command_handler` (:163-173) | §6 참조 |
| Kafka `discharge-order` 수신 | `receive_discharge_order_command` (:96-102) · `revoke_discharge_order_command` (:105-110) | `receive_discharge_order_command_handler` (:226-236) · `revoke_discharge_order_command_handler` (:300-309) | §6 참조 |

---

## 3. 쓰기 경로 샘플(Command) — `POST /api/v1/foodservice/meal-orders` 세부오더 발행

체인: Controller → 요청 DTO `to_command` → `command_bus.send` → validator → handler(`@Transactional`) → Aggregate Root 팩토리(도메인 이벤트 적재) → Repository IF(domain) → JPA 어댑터(infrastructure, 이벤트 발행) → `domain_event_dispatcher` → (AFTER_COMMIT 리스너).

### 3.1 Controller 메서드 — `presentation/meal_orders_controller.kt:134-146`

```kotlin
    @PostMapping("/meal-orders")
    @Operation(
        summary = "세부오더 발행",
        description = "DietOrder=active 검증, 금식 처방은 422. 같은 처방·끼니 중복은 409 FS-4093. " +
            "식이 코드가 처방의 식이처방 코드 소속이 아니면 400 FS-4003. 비운 속성은 처방 값·기본값으로 채운다.",
    )
    fun issue(@TokenPrincipal principal: AuthPrincipal, @Valid @RequestBody request: issue_meal_order_request): ResponseEntity<meal_order_view_model> {
        val scope = principal.to_tenant_scope()
        val result: meal_order_command_result = command_bus.send(request.to_command(scope))
        return ResponseEntity
            .created(URI.create("/meal-orders/${result.meal_order_id}"))
            .body(load(scope, result.meal_order_id))
    }
```

요청 DTO → command 변환 — `presentation/meal_orders_controller.kt:255-274`

```kotlin
    /** 발행자는 본문으로 받지 않는다 — 감사 컬럼(created_by)이 토큰 직원 id 로 남긴다. */
    fun to_command(scope: tenant_scope) = issue_meal_order_command(
        scope = scope,
        diet_order_id = diet_order_id.to_request_uuid("diet_order_id")!!,
        meal_type_code = meal_type_code!!,
        meal_code = meal_code,
        unit_qty = unit_qty,
        meal_form_code = meal_form_code,
        portion_code = portion_code,
        salt_level_code = salt_level_code,
        kcal = kcal,
        volume_ml = volume_ml,
        is_isolation_meal = is_isolation_meal,
        is_guardian_meal = is_guardian_meal,
        guardian_portion_code = guardian_portion_code,
        guardian_salt_level_code = guardian_salt_level_code,
        is_guardian_isolation_meal = is_guardian_isolation_meal,
        effective_start_date = effective_start_date!!,
        effective_end_date = effective_end_date,
    )
```

토큰 → tenant_scope — `presentation/token_principal_scope.kt:10-19`

```kotlin
// 토큰 주체 → 테넌트 범위 · 직원 id. HTTP 요청의 tenant_id · hospital_code · 작업자 id 는 이 파일에서만 나온다.
// AuthPrincipal 은 presentation 밖으로 내보내지 않는다 — application 이하에는 tenant_scope 와 UUID 만 넘긴다.

// claim 이 비어 있으면 tenant_scope.of 가 INVALID_DATA 로 채운다(라이브러리가 필수 claim 을 검증하므로 정상 토큰에서는 생기지 않는다)
fun AuthPrincipal.to_tenant_scope(): tenant_scope = tenant_scope.of(tenantId, hospitalCode)

// 토큰 sub = 직원 레코드 uuid(shared 직원 id). UUID 가 아니면 이 BC 가 쓸 수 없는 토큰이라 401(AUTH_TOKEN_INVALID)로 거절한다
fun AuthPrincipal.employee_id(): UUID =
    userId.trim().takeIf { UUID_FORMAT.matches(it) }?.let(UUID::fromString)
        ?: throw JwtAuthenticationException(JwtErrorCode.AUTH_TOKEN_INVALID, "토큰 사용자 ID 가 UUID 형식이 아닙니다")
```

### 3.2 Command 클래스 — `application/commands/foodservice_commands.kt:140-166` (+ 마커 `command.kt:1-13`)

```kotlin
/** 모든 명령의 마커 인터페이스. */
interface command

/**
 * 명령 핸들러.
 * @param TCommand 처리할 명령 타입
 * @param TResult  처리 결과 타입
 */
interface command_handler<TCommand : command, TResult> {
    fun handle(command: TCommand): TResult
}
```

```kotlin
data class issue_meal_order_command(
    val scope: tenant_scope,
    val diet_order_id: UUID,
    /** 끼니 — `MealTypeSetting.meal_type_code`. */
    val meal_type_code: String,
    /** 식이 코드 — 처방의 식이처방 코드의 활성 식이 코드(FS-4003). null 이면 `Meal.is_default`. */
    val meal_code: String? = null,
    /** 개수 — null 이면 처방 값 → 식종 기본값(완제품은 열량 ÷ 단위당 kcal). 식이 코드 단위가 UNIT 가 아니면 버린다. */
    val unit_qty: Int? = null,
    /** 식사 형태 — 밥/죽/미음 …. 식종과 다른 축이다. 코드 집합은 `meal_form` 마스터가 정한다. 완제품 · 금식 식종은 null. */
    val meal_form_code: String? = null,
    val portion_code: String? = null,
    /** 간 — 식이처방의 저염식과 다른 축이다(그 끼니의 조리 지시). */
    val salt_level_code: String? = null,
    /** 열량 · 용량 — 축이 세부오더에 허용될 때만. null 이면 처방 값 → 축 기본값. */
    val kcal: Int? = null,
    val volume_ml: Int? = null,
    /** 격리 배식 — 식기·배선을 따로 쓴다. */
    val is_isolation_meal: Boolean? = null,
    val is_guardian_meal: Boolean = false,
    // 보호자식 옵션 — 환자와 다른 사람의 식사라 따로 받는다(is_guardian_meal=false 면 쓰이지 않는다).
    val guardian_portion_code: String = com.projectx.foodservice.domain.common.portion_code.NORMAL,
    val guardian_salt_level_code: String = com.projectx.foodservice.domain.common.salt_level_code.NORMAL,
    val is_guardian_isolation_meal: Boolean = false,
    val effective_start_date: LocalDate,
    val effective_end_date: LocalDate? = null,
) : command
```

결과 — `foodservice_commands.kt:204-207`

```kotlin
data class meal_order_command_result(
    val meal_order_id: UUID,
    val affected: Map<String, Int> = emptyMap(),
)
```

### 3.3 command_bus — 디스패치 방식 — `application/commands/command_bus.kt:1-36`

핸들러·검증기 빈을 **제네릭 타입(`ResolvableType`)으로 색인**해 명령 클래스 → 핸들러를 찾는다. 검증기 → 핸들러 순.

```kotlin
@Component
class command_bus(
    handlers: List<command_handler<*, *>>,
    validators: List<command_validator<*>>,
) {
    private val handler_index: Map<Class<*>, command_handler<*, *>> =
        handlers.associateBy { resolve_generic(it.javaClass, command_handler::class.java) }

    private val validator_index: Map<Class<*>, List<command_validator<*>>> =
        validators.groupBy { resolve_generic(it.javaClass, command_validator::class.java) }

    @Suppress("UNCHECKED_CAST")
    fun <TResult> send(command: command): TResult {
        val type = command.javaClass
        validator_index[type]?.forEach { (it as command_validator<command>).validate(command) }
        val handler = handler_index[type]
            ?: throw IllegalStateException("등록된 핸들러가 없습니다: ${type.name}")
        return (handler as command_handler<command, TResult>).handle(command)
    }

    /** 구현 클래스에서 대상 인터페이스의 첫 번째 제네릭(명령 타입)을 해석한다. */
    private fun resolve_generic(impl: Class<*>, target: Class<*>): Class<*> =
        ResolvableType.forClass(impl).`as`(target).getGeneric(0).resolve()
            ?: throw IllegalStateException("명령 타입을 해석할 수 없습니다: ${impl.name}")
}
```

### 3.4 Validator — `application/validations/foodservice_command_validators.kt:30-60` (IF: `command_validator.kt:5-11`)

```kotlin
/**
 * 커맨드 검증기 — 입력 형식/값집합 검증(400 Validation).
 * 불변식(422)은 애그리거트/핸들러가, 중복 키·마감 게이트(409)는 핸들러/Repository 가 담당한다.
 * 끼니(FS-4006)·조절값(FS-4005)의 허용 집합은 설정·마스터가 정하므로 핸들러가 스냅샷으로 검증한다 — 여기서는 형식만 본다.
 */

@Component
class issue_meal_order_command_validator(
    private val meal_forms: i_meal_form_reader,
) : command_validator<issue_meal_order_command> {

    override fun validate(command: issue_meal_order_command) {
        require_code(command.meal_type_code, "meal_type_code")
        // 비운 값은 서버가 처방 값·기본값으로 채운다 — 온 값만 검사한다.
        command.portion_code?.let { require_code(it, "portion_code") }
        command.salt_level_code?.let { require_code(it, "salt_level_code") }
        command.meal_form_code?.let { require_meal_form(it, command.scope, meal_forms) }
        command.meal_code?.let { require_code(it, "meal_code") }
        command.unit_qty?.let { require_non_negative(it, "unit_qty") }
        command.kcal?.let { require_non_negative(it, "kcal") }
        command.volume_ml?.let { require_non_negative(it, "volume_ml") }
        val end = command.effective_end_date
        if (end != null && end.isBefore(command.effective_start_date)) {
            throw foodservice_validation_exception(
                "적용 종료일이 시작일보다 빠릅니다.",
                mapOf("field" to "effective_end_date"),
            )
        }
    }
}
```

### 3.5 Handler — `application/commands/mealorder/meal_order_command_handlers.kt:65-160`

트랜잭션 경계 = `handle()` 의 `@Transactional`. 다른 Aggregate(DietOrder · 설정 · 마스터)는 **스냅샷 reader** 로 읽어 검증하고, 중복은 Repository 선검사 + DB partial unique 가 막는다.

```kotlin
@Service
class issue_meal_order_command_handler(
    private val diet_order_repository: i_diet_order_repository,
    private val meal_order_repository: i_meal_order_repository,
    private val diet_types: i_diet_type_reader,
    private val meal_forms: i_meal_form_reader,
    private val settings: i_foodservice_setting_reader,
    private val clock: Clock,
) : command_handler<issue_meal_order_command, meal_order_command_result> {

    private val log = LoggerFactory.getLogger(issue_meal_order_command_handler::class.java)

    @Transactional
    override fun handle(command: issue_meal_order_command): meal_order_command_result {
        val at = clock.instant()
        val diet_order = diet_order_repository.find_by_id(command.scope, command.diet_order_id)
            ?: throw diet_order_not_found_exception(command.diet_order_id)

        // 금식은 낼 끼니가 없는 처방이다 — 화면은 ISSUE_MEAL_ORDER 를 숨기고, 여기서 한 번 더 막는다.
        val diet = diet_types.find(command.scope, diet_order.diet_type_code)
        if (diet?.is_npo() == true) {
            throw npo_diet_order_issue_domain_exception(diet_order.diet_order_id, diet_order.diet_type_code)
        }
        // 끼니는 활성 설정이고 그 식이처방에 허용돼야 한다(FS-4006).
        require_meal_type_allowed(settings, command.scope, command.meal_type_code, diet_order.diet_type_code)

        // 같은 처방·끼니의 활성 세부오더 중복 방어 — partial unique(ux_fs_meal_order_slot) 의 선검사.
        meal_order_repository
            .find_active_by_diet_order_id_and_meal_type(command.scope, command.diet_order_id, command.meal_type_code)
            ?.let { throw duplicate_meal_order_conflict_exception(command.diet_order_id, command.meal_type_code) }

        // 퇴원지시 — 퇴원으로 먹지 않는 식사부터는 발행하지 않고, 적용 종료일은 퇴원 예정일에서 자른다.
        if (!diet_order.serves(command.effective_start_date, command.meal_type_code, serving_order(settings, command.scope))) {
            throw bad_state_domain_exception(
                "퇴원 예정일 이후 식사는 세부오더를 발행할 수 없습니다.",
                diet_order.status.name,
                mapOf(
                    "diet_order_id" to diet_order.diet_order_id,
                    "discharge_planned_date" to diet_order.discharge_planned_date?.toString(),
                    "effective_start_date" to command.effective_start_date.toString(),
                ),
            )
        }
        val effective_end_date = diet_order.discharge_planned_date
            ?.let { planned -> command.effective_end_date?.let { minOf(it, planned) } ?: planned }
            ?: command.effective_end_date

        val attributes = initial_attributes(command, diet_order, diet)
        // 조절값 — 반영 뒤 값을 검증한다(FS-4005). 요청이 주지 않은 항목(처방 복사 · 기본값)은 다시 거부하지 않도록
        // 기준값에서 요청이 준 항목만 비워 넘긴다(필수 축은 전부 본다).
        require_attribute_values(
            diet, diet_order.diet_type_code, attributes.meal_code, attribute_applies_to_code.MEAL_ORDER,
            values = attributes,
            base = attributes.copy(
                kcal = attributes.kcal.takeIf { command.kcal == null },
                volume_ml = attributes.volume_ml.takeIf { command.volume_ml == null },
                unit_qty = attributes.unit_qty.takeIf { command.unit_qty == null },
                meal_form_code = if (command.meal_form_code == null) attributes.meal_form_code else "",
                portion_code = if (command.portion_code == null) attributes.portion_code else "",
                salt_level_code = if (command.salt_level_code == null) attributes.salt_level_code else "",
            ),
        )

        val order = meal_order.issue(
            scope = command.scope,
            diet_order_id = diet_order.diet_order_id,
            diet_order_status = diet_order.status,
            diet_type_code = diet_order.diet_type_code,
            patient_id = diet_order.patient_id,
            encounter_id = diet_order.encounter_id,
            ward_id = diet_order.ward_id,
            meal_type_code = command.meal_type_code,
            attributes = attributes,
            effective_start_date = command.effective_start_date,
            effective_end_date = effective_end_date,
            at = at,
        )
        meal_order_repository.save(order)
        val first = order.latest_revision()
        log.info(
            "세부오더 발행: meal_order_id={} diet_order_id={} 끼니={} 식이코드={} 형태={} 양={} 간={} 열량={} 격리={} 보호자식={}",
            order.meal_order_id, command.diet_order_id, command.meal_type_code, first.meal_code, first.meal_form_code,
            first.portion_code, first.salt_level_code, first.kcal, first.is_isolation_meal, first.is_guardian_meal,
        )
        return meal_order_command_result(order.meal_order_id)
    }
```

### 3.6 Aggregate Root 팩토리 — 도메인 이벤트 적재 — `domain/aggregatesmodel/mealorder/meal_order.kt:329-383`

```kotlin
    companion object {

        /**
         * 세부오더 발행 — DietOrder 가 active 인지 검증한 뒤 생성한다. (C-20)
         * 최초 리비전 1건을 `effective_start_date` 부터 함께 연다.
         * 같은 처방·끼니의 활성 세부오더 중복 방어는 핸들러(+partial unique)가 담당한다.
         */
        fun issue(
            scope: tenant_scope,
            diet_order_id: UUID,
            diet_order_status: com.projectx.foodservice.domain.common.diet_order_status,
            diet_type_code: String,
            patient_id: UUID,
            encounter_id: UUID,
            ward_id: String?,
            meal_type_code: String,
            attributes: meal_attributes,
            effective_start_date: LocalDate,
            effective_end_date: LocalDate?,
            at: Instant,
        ): meal_order {
            if (!diet_order_status.is_active()) {
                throw diet_order_not_active_domain_exception(diet_order_id, diet_order_status.name)
            }
            attributes.validate()
            if (effective_end_date != null && effective_end_date.isBefore(effective_start_date)) {
                throw bad_state_domain_exception("적용 종료일이 시작일보다 빠릅니다.", meal_order_status.ACTIVE.name)
            }
            return meal_order().apply {
                assign_id()
                assign_tenant(scope)
                this.diet_order_id = diet_order_id
                this.diet_type_code = diet_type_code
                this.patient_id = patient_id
                this.encounter_id = encounter_id
                this.ward_id = ward_id
                this.meal_type_code = meal_type_code
                this.effective_start_date = effective_start_date
                this.effective_end_date = effective_end_date
                this.status = meal_order_status.ACTIVE
                this.revision_history.add(
                    meal_order_revision.open(
                        scope = scope,
                        effective_from = effective_start_date,
                        effective_to = effective_end_date,
                        values = attributes,
                        at = at,
                    ),
                )
                add_domain_event(
                    meal_order_issued_domain_event(scope, id, diet_order_id, patient_id, meal_type_code, at),
                )
            }
        }
    }
```

도메인 이벤트 — `domain/events/foodservice_domain_events.kt:63-70`

```kotlin
data class meal_order_issued_domain_event(
    val scope: tenant_scope,
    val meal_order_id: UUID,
    val diet_order_id: UUID,
    val patient_id: UUID,
    val meal_type_code: String,
    val occurred_at: Instant,
) : domain_event
```

### 3.7 seedwork 베이스 클래스

`domain/seedwork/seedwork.kt:1-20`

```kotlin
/** Aggregate Root 마커 인터페이스. Aggregate Root 만 구현한다. */
interface i_aggregate_root

/**
 * Aggregate Root 단위 영속성 추상화.
 * 구현체는 infrastructure 레이어에 위치하며 JPA 어댑터로 구현한다.
 */
interface i_repository<T : i_aggregate_root> {
    fun save(aggregate: T): T

    /** 테넌트 범위 안에서 id 로 찾는다 — 다른 범위의 같은 id 는 없는 것으로 본다. */
    fun find_by_id(scope: tenant_scope, id: UUID): T?
}

/** 도메인 이벤트 마커 인터페이스. data class 이벤트가 구현한다. */
interface domain_event
```

`domain/seedwork/entity.kt:10-95` (UUID id · 테넌트 · 감사 4종 · 도메인 이벤트 보유)

```kotlin
/**
 * 모든 엔티티의 공통 베이스 — UUID ID + 테넌트 범위 + 감사 컬럼 + 도메인 이벤트 보유.
 *
 * 모든 Entity 테이블의 컬럼 순서는 「PK → tenant_id · hospital_code → 업무 컬럼 → 감사 4종」이다(순서는 changelog 가 정한다).
 * - id 는 항상 UUID 다 — 팩토리(companion)가 `UUID.randomUUID()` 로 발급한다. 실제 PK 컬럼명은 `<테이블명>_id` 이고
 *   하위 클래스가 `@AttributeOverride(name = "id", column = @Column(name = "<테이블명>_id"))` 로 지정한다.
 * - tenant_id · hospital_code 는 생성 때 `assign_tenant` 로 정하고 바꾸지 않는다. 특정하지 못하면 INVALID_DATA 다.
 * - 감사 4종은 도메인이 채우지 않는다 — infrastructure 의 audit_entity_listener(JPA 기본 리스너, orm.xml 등록)가
 *   @PrePersist · @PreUpdate 에서 record_creation · record_modification 을 부른다. domain 은 시각 · 직원을 모른다.
 * - 도메인 이벤트를 적재하고, 영속 직후 Repository 어댑터가 꺼내 발행한다.
 */
@MappedSuperclass
abstract class entity {

    @Id
    open var id: UUID = NIL_ID
        protected set

    @Column(name = "tenant_id", nullable = false, updatable = false, length = 64)
    open var tenant_id: String = tenant_scope.INVALID_DATA
        protected set

    @Column(name = "hospital_code", nullable = false, updatable = false, length = 64)
    open var hospital_code: String = tenant_scope.INVALID_DATA
        protected set

    /** 생성 일시 — INSERT 때 리스너가 채운다. */
    @Column(name = "created_at", nullable = false, updatable = false)
    open var created_at: Instant = Instant.EPOCH
        protected set

    /** 생성한 직원 ID(shared 직원 uuid) — 인증 주체가 없거나 UUID 가 아니면 null. */
    @Column(name = "created_by", updatable = false)
    open var created_by: UUID? = null
        protected set

    /** 마지막 수정 일시 — INSERT · UPDATE 때 리스너가 채운다. */
    @Column(name = "updated_at", nullable = false)
    open var updated_at: Instant = Instant.EPOCH
        protected set

    /** 마지막으로 수정한 직원 ID(shared 직원 uuid) — 인증 주체가 없거나 UUID 가 아니면 null. */
    @Column(name = "updated_by")
    open var updated_by: UUID? = null
        protected set

    @Transient
    private val _domain_events: MutableList<domain_event> = mutableListOf()

    /** 적재된 도메인 이벤트의 불변 스냅샷. */
    val domain_events: List<domain_event>
        @Transient get() = _domain_events.toList()

    fun add_domain_event(event: domain_event) {
        _domain_events.add(event)
    }

    fun clear_domain_events() {
        _domain_events.clear()
    }

    /** 아직 식별자가 부여되지 않은(미영속) 상태인지 여부. */
    fun is_transient(): Boolean = id == NIL_ID

    /** 이 엔티티의 테넌트 범위 — 다른 Aggregate 를 같은 범위에서 찾을 때 쓴다. */
    fun scope(): tenant_scope = tenant_scope.of(tenant_id, hospital_code)

    /** 식별자 부여 — 팩토리(companion) 에서만 사용한다. 인자를 생략하면 새 UUID 를 발급한다. */
    protected fun assign_id(new_id: UUID = UUID.randomUUID()) {
        id = new_id
    }

    /** 테넌트 범위 지정 — 생성 때 한 번만 부른다. 하위 Entity 는 Root 의 범위를 그대로 받는다. */
    protected fun assign_tenant(scope: tenant_scope) {
        this.tenant_id = scope.tenant_id
        this.hospital_code = scope.hospital_code
    }

    /**
     * Root 를 수정 상태로 표시한다 — 하위 Entity 만 바뀐 저장에서도 Root 의 `updated_*` 와 `@Version` 이 오르게 한다.
     * 넣은 시각은 표시용일 뿐이고, 실제 수정 시각 · 직원은 저장 때 리스너(record_modification)가 덮어쓴다.
     */
    protected fun touch_root(at: Instant) {
        this.updated_at = at
    }
```

`domain/seedwork/tenant_scope.kt:9-26` — (테넌트, 병원) VO, 빈 값은 `INVALID_DATA`

```kotlin
data class tenant_scope private constructor(
    val tenant_id: String,
    val hospital_code: String,
) {
    companion object {
        /** 테넌트 · 병원을 특정하지 못했을 때 넣는 값. */
        const val INVALID_DATA = "INVALID_DATA"

        /** 빈 값(null · 공백)은 INVALID_DATA 로 바꾼다. 앞뒤 공백은 떼고 대소문자는 받은 그대로 둔다. */
        fun of(tenant_id: String?, hospital_code: String?): tenant_scope =
            tenant_scope(normalize(tenant_id), normalize(hospital_code))

        /** 둘 다 특정하지 못한 범위. */
        val INVALID: tenant_scope = tenant_scope(INVALID_DATA, INVALID_DATA)

        private fun normalize(value: String?): String = value?.trim()?.takeIf { it.isNotEmpty() } ?: INVALID_DATA
    }
}
```

`domain/seedwork/value_object.kt:7-20`

```kotlin
abstract class value_object {

    protected abstract fun get_equality_components(): List<Any?>

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        other as value_object
        return get_equality_components() == other.get_equality_components()
    }

    override fun hashCode(): Int =
        get_equality_components().fold(0) { acc, component -> acc xor (component?.hashCode() ?: 0) }
}
```

### 3.8 Repository IF(domain) — `domain/aggregatesmodel/mealorder/i_meal_order_repository.kt:8-36`

```kotlin
/** 세부오더 Aggregate 영속성 인터페이스. 구현은 infrastructure 레이어. 모든 조회는 테넌트 범위 안에서만 한다. */
interface i_meal_order_repository : i_repository<meal_order> {

    fun save_all(aggregates: List<meal_order>): List<meal_order>

    /** 처방 기준 전체 세부오더 — 처방 취소 전파 대상. */
    fun find_by_diet_order_id(scope: tenant_scope, diet_order_id: UUID): List<meal_order>

    /** 처방 기준 활성 세부오더. */
    fun find_active_by_diet_order_id(scope: tenant_scope, diet_order_id: UUID): List<meal_order>

    /** 같은 처방·끼니의 활성 세부오더 — 중복 발행 방어(partial unique 의 선검사). */
    fun find_active_by_diet_order_id_and_meal_type(scope: tenant_scope, diet_order_id: UUID, meal_type_code: String): meal_order?

    /**
     * 접수자료 생성 대상(접수 목록 조회 시 생성) — 해당 식사일자·끼니를 커버하는 활성 세부오더.
     * `ward_id` 가 주어지면 병동 단위로 좁힌다.
     */
    fun find_active_for_generation(scope: tenant_scope, meal_date: LocalDate, meal_type_code: String, ward_id: String?): List<meal_order>

    /** 환자 단위 활성 세부오더 — 금식 처방의 결식 처리 대상 탐색. */
    fun find_active_by_patient_id(scope: tenant_scope, patient_id: UUID): List<meal_order>

    /** 수진 단위 활성 세부오더 — 퇴원/전동/외출 이벤트 처리. */
    fun find_active_by_encounter_id(scope: tenant_scope, encounter_id: UUID): List<meal_order>

    /** 끼니 코드별 활성 세부오더 건수 — 끼니 사용 중지(FS-4008) 판정. */
    fun count_active_by_meal_type(scope: tenant_scope, meal_type_code: String): Long
}
```

### 3.9 JPA 어댑터(infrastructure) — `infrastructure/repositories/foodservice_repositories.kt:38-100`

저장 직후 **인자 인스턴스**에서 도메인 이벤트를 꺼내 발행한다(merge 가 다른 인스턴스를 돌려주므로).

```kotlin
/**
 * 도메인 Repository 인터페이스의 JPA 어댑터.
 * 저장 직후 애그리거트에 적재된 도메인 이벤트를 발행하고 비운다.
 * 모든 조회는 테넌트 범위(`scope`) 안에서만 한다 — id 로 찾는 것도 같다(class-diagram §3.8).
 *
 * ⚠ 신규 애그리거트는 Spring Data 가 merge 로 저장해 **다른 인스턴스**를 돌려준다
 *   (`@Version` 이 있어 isNew=false 로 판정). 반환본에서 이벤트를 읽으면 유실되므로
 *   인자 인스턴스에서 먼저 꺼낸다.
 */
…
@Repository
class meal_order_repository(
    private val jpa: meal_order_jpa_repository,
    private val dispatcher: domain_event_dispatcher,
) : i_meal_order_repository {

    override fun save(aggregate: meal_order): meal_order {
        val events = aggregate.domain_events
        val saved = jpa.save(aggregate)
        aggregate.clear_domain_events()
        events.forEach { dispatcher.publish(it) }
        return saved
    }

    override fun save_all(aggregates: List<meal_order>): List<meal_order> {
        if (aggregates.isEmpty()) return emptyList()
        val events = aggregates.flatMap { it.domain_events }
        val saved = jpa.saveAll(aggregates)
        aggregates.forEach { it.clear_domain_events() }
        events.forEach { dispatcher.publish(it) }
        return saved
    }

    override fun find_by_id(scope: tenant_scope, id: UUID): meal_order? =
        jpa.find_in_scope(scope.tenant_id, scope.hospital_code, id)
    …
    override fun find_active_by_diet_order_id_and_meal_type(
        scope: tenant_scope,
        diet_order_id: UUID,
        meal_type_code: String,
    ): meal_order? = jpa.find_active_by_diet_order_id_and_meal_type(scope.tenant_id, scope.hospital_code, diet_order_id, meal_type_code)
```

Spring Data JPA 인터페이스(쓰기 경로 전용, JPQL `@Query` 명시) — `infrastructure/foodservice_context.kt:16-23, 78-92, 125-146`

```kotlin
/**
 * Write 경로의 Spring Data JpaRepository 인터페이스 모음.
 * (메서드명은 Spring Data 파생 규칙을 따르므로 framework 예외로 camelCase 파라미터를 쓴다.
 *  프로퍼티명에 underscore 가 있어 파생 쿼리 대신 JPQL 로 명시한다.)
 *
 * **모든 조회는 테넌트 범위(`tenant_id` · `hospital_code`) 안에서만 한다**(class-diagram §3.8) — id 로 찾는 것도 같다.
 * ⚠️ HQL 은 실제 속성(`id`)을 쓴다 — `<name>_id` 별칭 getter 는 JPA 속성이 아니다.
 */
interface meal_order_jpa_repository : JpaRepository<meal_order, UUID> {

    @Query("select m from meal_order m where m.id = :id and m.tenant_id = :tenantId and m.hospital_code = :hospitalCode")
    fun find_in_scope(
        @Param("tenantId") tenant_id: String,
        @Param("hospitalCode") hospital_code: String,
        @Param("id") id: UUID,
    ): meal_order?
    …
    @Query(
        """
        select m from meal_order m
        where m.tenant_id = :tenantId and m.hospital_code = :hospitalCode
          and m.diet_order_id = :dietOrderId
          and m.meal_type_code = :mealTypeCode
          and m.status = com.projectx.foodservice.domain.common.meal_order_status.ACTIVE
        """,
    )
    fun find_active_by_diet_order_id_and_meal_type(
        @Param("tenantId") tenant_id: String,
        @Param("hospitalCode") hospital_code: String,
        @Param("dietOrderId") diet_order_id: UUID,
        @Param("mealTypeCode") meal_type_code: String,
    ): meal_order?
```

### 3.10 domain_event_dispatcher — `infrastructure/domain_event_dispatcher.kt:1-19`

```kotlin
/**
 * 도메인 이벤트 디스패처.
 * ApplicationEventPublisher 를 래핑한다. 커밋 후 외부(Kafka) 발행이 필요한 구독자는
 * `@TransactionalEventListener(phase = AFTER_COMMIT)` 으로 수신한다.
 */
@Component
class domain_event_dispatcher(
    private val publisher: ApplicationEventPublisher,
) {
    fun publish(event: domain_event) {
        publisher.publishEvent(event)
    }
}
```

AFTER_COMMIT 구독자는 현재 **`diet_changed_domain_event` 하나만** 있다(`infrastructure/eventbus/domain_event_integration_publisher.kt:35-36`). `meal_order_issued_domain_event` 는 Spring 이벤트로 발행되지만 **구독자가 없다**(grep `@TransactionalEventListener|@EventListener` 결과 1건). §6 에 전체 인용.

---

## 4. 읽기 경로 샘플(Query)

### 4.1 샘플 A — 세부오더 보드 `GET /wards/{ward_id}/meal-order-board?date=`

체인: Controller → Query IF(application) → QueryDSL 구현(infrastructure) → view_model. 끼니 열은 설정(`meal_type_resolver`), 환자·진료과·의사·입퇴원일은 shared(`shared_directory`)에서 **id 묶음 한 번에** 읽는다.

Controller — `presentation/meal_orders_controller.kt:121-130`

```kotlin
    @GetMapping("/wards/{ward_id}/meal-order-board")
    @Operation(
        summary = "병동 끼니 발행 현황 보드",
        description = "행 = 그 병동의 활성 처방(환자별 마지막 1건), 열 = 끼니. 각 칸에 세부오더 + 해당 날짜 접수자료 상태를 담는다.",
    )
    fun find_board(
        @TokenPrincipal principal: AuthPrincipal,
        @PathVariable("ward_id") ward_id: String,
        @RequestParam date: LocalDate,
    ): meal_order_board_view_model = meal_order_queries.find_board(principal.to_tenant_scope(), ward_id, date)
```

Query IF — `application/queries/foodservice_queries.kt:65-72`

```kotlin
/** 세부오더 조회 (Q-20 · Q-21). */
interface i_meal_order_queries {
    fun find_by_id(scope: tenant_scope, meal_order_id: UUID): meal_order_view_model?
    fun find_by_diet_order_id(scope: tenant_scope, diet_order_id: UUID): List<meal_order_view_model>

    /** 병동 재원환자별 끼니 발행 현황 보드. */
    fun find_board(scope: tenant_scope, ward_id: String, meal_date: LocalDate): meal_order_board_view_model
}
```

QueryDSL 구현 — `infrastructure/queries/meal_order_query_dsl.kt:33-43, 73-128` (`JPAQueryFactory` · `in_scope` 테넌트 조건 · 날짜 범위 동적 where · Tuple 조회)

```kotlin
@Repository
class meal_order_query_dsl(
    private val query_factory: JPAQueryFactory,
    private val diet_types: diet_type_resolver,
    private val meal_types: meal_type_resolver,
    private val shared: shared_directory,
) : i_meal_order_queries {

    private val m = Qmeal_order.meal_order
    private val o = Qdiet_order.diet_order
    private val p = Qmeal_preparation.meal_preparation
    …
    @Transactional(readOnly = true)
    override fun find_board(scope: tenant_scope, ward_id: String, meal_date: LocalDate): meal_order_board_view_model {
        val active_types = meal_types.active(scope)
        val slots = active_types.map { it.meal_type_code }
        val labels = meal_types.labels(scope)

        // 그 병동의 활성 처방을 읽어 환자별 마지막 1건만 남긴다.
        val diet_orders = board_query(scope, ward_id).fetch()
            .groupBy { it.patient_id }
            .map { (_, orders) -> orders.maxBy { it.ordered_at } }
        if (diet_orders.isEmpty()) {
            return meal_order_board_view_model(meal_date, ward_id, slots, labels, emptyList())
        }

        val diet_order_ids = diet_orders.map { it.diet_order_id }
        val meal_orders = query_factory.selectFrom(m)
            .where(
                in_scope(scope)
                    .and(m.diet_order_id.`in`(diet_order_ids))
                    .and(m.status.eq(meal_order_status.ACTIVE))
                    // ⚠ **그 날짜의 적용기간 안**인 것만 발행으로 본다. 기간을 보지 않으면 시작일이
                    //   미래인 오더가 오늘 칸에 "발행" 으로 뜨고, 같은 기간을 보는 달력(Q-21)과 어긋난다.
                    .and(m.effective_start_date.loe(meal_date))
                    .and(m.effective_end_date.isNull.or(m.effective_end_date.goe(meal_date))),
            )
            .fetch()
            .groupBy { it.diet_order_id }

        // 해당 날짜의 접수자료 상태 + 마감 차수를 (세부오더 id) 키로 모아 둔다.
        val preparations = query_factory.select(p.meal_order_id, p.status, p.closed_round)
            .from(p)
            .where(
                p.tenant_id.eq(scope.tenant_id).and(p.hospital_code.eq(scope.hospital_code))
                    .and(p.meal_date.eq(meal_date)),
            )
            .fetch()
            .associate { tuple ->
                tuple.get(p.meal_order_id)!! to Pair(tuple.get(p.status)?.name, tuple.get(p.closed_round))
            }

        // shared 값은 행 묶음 한 번에 읽는다.
        val patients = shared.patients(diet_orders.map { it.patient_id })
        val departments = shared.department_names(diet_orders.map { it.department_id })
        // 의사 이름은 처방의(orderer_id) 이름이다 — 주치의 id 는 보관하지 않는다(수진 스냅샷 없음).
        val doctors = shared.staff_names(diet_orders.map { it.orderer_id })
        val periods = shared.encounters(diet_orders.map { it.encounter_id })

        val master = diet_types.load(scope)
        val content = diet_orders
            .map { diet_order -> diet_order to (patients[diet_order.patient_id] ?: patient_placeholder(diet_order.patient_id)) }
            .sortedWith(
                compareBy(
                    { it.first.room_id.orEmpty() },
                    { it.first.bed_no.orEmpty() },
                    { it.second.patient_name.orEmpty() },
                ),
            )
            .map { (diet_order, patient) ->
                val by_type = meal_orders[diet_order.diet_order_id].orEmpty().associateBy { it.meal_type_code }
                val diet_type = master[diet_order.diet_type_code]
                val period = periods[diet_order.encounter_id]
                meal_order_board_row_view_model(
                    encounter_id = diet_order.encounter_id,
                    diet_order_id = diet_order.diet_order_id,
                    patient = patient,
                    …
                    admission_date = period?.start_at?.let { LocalDate.ofInstant(it, KST) },
                    discharge_date = period?.end_at?.let { LocalDate.ofInstant(it, KST) },
```

칸(slot) 조립 — 그 날짜의 리비전을 쓰고 `allowed_actions` 를 계산한다 — `meal_order_query_dsl.kt:149-188`

```kotlin
                    slots = slots.associateWith { type ->
                        by_type[type]?.let { order ->
                            val preparation = preparations[order.meal_order_id]
                            val closed_round = preparation?.second
                            val closed = closed_round != null
                            // ⚠ 칸의 값은 **그 날짜의 리비전**이다 — 오더 한 벌의 값을 그대로 실으면
                            //   오늘 바꾼 값이 지난 날짜 칸에도 뜬다(수정이 과거를 덮는 것처럼 보인다).
                            val revision = order.revision_at(meal_date) ?: order.latest_revision()
                            val missing = diet_type?.missing_required(revision.meal_code, revision.attributes()).orEmpty()
                            meal_order_slot_view_model(
                                meal_order_id = order.meal_order_id,
                                meal_code = revision.meal_code,
                                meal_name = master[order.diet_type_code]?.meal_of(revision.meal_code)?.meal_name,
                                …
                                status = order.status.name,
                                preparation_status = preparation?.first,
                                closed = closed,
                                closed_round = closed_round,
                                allowed_actions = allowed_actions.of_meal_order(order.status, closed),
                                needs_review = missing.isNotEmpty(),
                                missing_attribute_codes = missing,
                            )
                        }
                    },
                )
            }
        return meal_order_board_view_model(meal_date, ward_id, slots, labels, content)
    }

    /** 병동 보드의 처방 쿼리(실행 전) — 그 병동의 활성(ACTIVE · ON_HOLD) 처방. 실행 없이 HQL 해석을 검사하는 테스트도 쓴다. */
    internal fun board_query(scope: tenant_scope, ward_id: String): JPAQuery<diet_order> =
        query_factory.selectFrom(o)
            .where(
                o.tenant_id.eq(scope.tenant_id).and(o.hospital_code.eq(scope.hospital_code))
                    .and(o.ward_id.eq(ward_id))
                    .and(o.status.`in`(diet_order_status.ACTIVE, diet_order_status.ON_HOLD)),
            )
```

테넌트 조건 헬퍼 — `meal_order_query_dsl.kt:302-304`

```kotlin
    /** 테넌트 범위 조건. */
    private fun in_scope(scope: tenant_scope) =
        m.tenant_id.eq(scope.tenant_id).and(m.hospital_code.eq(scope.hospital_code))
```

view_model — `application/queries/foodservice_view_models.kt:9-12, 371-377, 335-370(일부)`

```kotlin
/**
 * 응답 view_model. Controller 는 Entity / Aggregate Root 를 직렬화하지 않고 이것만 내려준다.
 * JSON 필드명은 **snake_case**(REST 계약 = 백엔드 컨벤션과 1:1). 프런트는 변환하지 않는다.
 */
…
data class meal_order_board_view_model(
    val meal_date: LocalDate,
    val ward_id: String,
    /** 활성 끼니 코드 — 열 순서. 설정이 원천이다(§3.6.1). */
    val slots: List<String>,
    val meal_types: List<meal_type_label_view_model>,
    val content: List<meal_order_board_row_view_model>,
)
…
data class meal_order_slot_view_model(
    val meal_order_id: UUID,
    /** 이 칸의 값은 **조회한 날짜의 리비전** 값이다 — 날짜를 바꾸면 그 날짜의 값이 온다. */
    val meal_code: String?,
    val meal_name: String?,
    val unit_qty: Int?,
    …
    /** 해당 날짜·끼니의 접수자료 상태(있으면) — 마감/배식 진행을 병동에서 함께 본다. */
    val preparation_status: String?,
    val closed: Boolean,
    /** 접수자료의 현재 마감 차수(있으면). */
    val closed_round: Int?,
    val allowed_actions: List<String> = emptyList(),
    /** 확인 필요 — 이 칸의 값에서 필수 축 값이 비어 있다(자동 설정이 필수값 없이 만든 경우). */
    val needs_review: Boolean = false,
    /** 비어 있는 필수 축 코드 — `needs_review` 의 근거. */
    val missing_attribute_codes: List<String> = emptyList(),
)
```

끼니 설정 캐시(열 순서의 원천) — `infrastructure/queries/query_support.kt:34-58`

```kotlin
@Component
class meal_type_resolver(
    private val reader: i_foodservice_setting_reader,
) {
    /** 활성 끼니, 정렬 순서. */
    fun active(scope: tenant_scope): List<meal_type_snapshot> =
        reader.meal_types(scope).filter { it.is_active }.sortedWith(compareBy({ it.sort_order }, { it.serving_time }))

    fun labels(scope: tenant_scope): List<meal_type_label_view_model> = active(scope).map {
        meal_type_label_view_model(it.meal_type_code, it.meal_type_name, it.meal_type_category_code, it.serving_time)
    }
    …
}
```

> 참고: `find_board` 는 `selectFrom(m)` 으로 **엔티티를 그대로 읽어** view_model 로 변환한다(`revision_at` 같은 도메인 조회 술어를 쓰기 위해). "projection" 의 정석은 아래 샘플 B(`Projections.constructor`)다.

### 4.2 샘플 B — 처방 목록 `GET /diet-orders` (`Projections.constructor` + `BooleanBuilder` 동적 where + shared 상관 서브쿼리)

`infrastructure/queries/diet_order_query_dsl.kt:46-74, 87-121, 229-261`

```kotlin
    private val projection = Projections.constructor(
        diet_order_row::class.java,
        o.id,
        o.order_number,
        o.patient_id,
        o.encounter_id,
        o.department_id,
        o.ward_id,
        o.room_id,
        o.bed_no,
        o.orderer_id,
        o.diet_type_code,
        o.meal_form_code,
        o.portion_code,
        o.salt_level_code,
        o.is_isolation_meal,
        o.kcal,
        o.volume_ml,
        o.unit_qty,
        o.instruction_text,
        o.status.stringValue(),
        o.ordered_at,
        o.received_at,
        o.discharge_planned_date,
        o.discharge_last_meal_type_code,
        o.discharge_meal_set_by_code,
        o.discharge_meal_set_at,
        o.version,
    )
    …
    @Transactional(readOnly = true)
    override fun search(scope: tenant_scope, filter: diet_order_search_filter): page_view_model<diet_order_view_model> {
        val predicate = search_predicate(scope, filter)
        val size = safe_size(filter.size)
        val total = query_factory.select(o.count()).from(o).where(predicate).fetchOne() ?: 0L
        val rows = search_query(predicate)
            .orderBy(*order_specifiers(filter.sort))
            .offset(filter.page.toLong() * size)
            .limit(size.toLong())
            .fetch()

        // 목록의 "발행 현황" 열 — 처방별 활성 세부오더 끼니를 한 번에 모아 붙인다(N+1 회피).
        val issued = issued_slots_of(scope, rows.map { it.diet_order_id })
        // 식이처방 마스터 · shared 이름은 요청당 한 번 읽는다(행마다 읽으면 행 수만큼 조회가 나간다).
        val master = diet_types.load(scope)
        val names = shared_names_of(rows)
        return page_view_model(
            content = rows.map { to_view_model(it, issued[it.diet_order_id].orEmpty(), master, names) },
            page = page_of(filter.page, size, total),
        )
    }

    /** 목록 조건 — 검색어는 환자번호 · 환자명(shared) · 처방번호를 함께 훑는다(화면 단일 입력창). */
    internal fun search_predicate(scope: tenant_scope, filter: diet_order_search_filter): BooleanBuilder {
        val predicate = BooleanBuilder(in_scope(scope))
        filter.ward_id?.let { predicate.and(o.ward_id.eq(it)) }
        if (filter.status.isNotEmpty()) predicate.and(o.status.stringValue().`in`(filter.status))
        filter.diet_type_code?.let { predicate.and(o.diet_type_code.eq(it)) }
        filter.received_from?.let { predicate.and(o.received_at.goe(start_of_day(it))) }
        filter.received_to?.let { predicate.and(o.received_at.lt(start_of_day(it.plusDays(1)))) }
        filter.keyword?.takeIf { it.isNotBlank() }?.let { keyword ->
            predicate.and(shared.patient_matches(o.patient_id, keyword).or(o.order_number.containsIgnoreCase(keyword)))
        }
        return predicate
    }

    /** 목록 쿼리(실행 전) — 실행 없이 HQL 해석을 검사하는 테스트도 쓴다. */
    internal fun search_query(predicate: BooleanBuilder): JPAQuery<diet_order_row> =
        query_factory.select(projection).from(o).where(predicate)
```

```kotlin
/**
 * QueryDSL projection 중간 행.
 * ⚠ `ConstructorExpression` 이 리플렉션으로 생성하므로 **중첩 private 클래스로 두면 안 된다**
 *   (런타임 IllegalAccessException). top-level `internal` 로 둔다.
 */
internal data class diet_order_row(
    val diet_order_id: UUID,
    val order_number: String,
    val patient_id: UUID,
    val encounter_id: UUID,
    …
    val version: Long,
)
```

### 4.3 shared 스키마 읽기 전용 엔티티 + 조인 코드

`infrastructure/shared/shared_patient.kt:11-54` — `@Immutable` · `@Table(schema = "shared")` · 쓰는 컬럼만

```kotlin
/**
 * `shared.patient` 읽기 전용 매핑 — 프로젝트 공유 스키마의 환자(루트 CLAUDE.md §0 DB 스키마의 shared 예외).
 *
 * 이 MS 의 Aggregate 가 아니다 — 소유자가 따로 있고 이 MS 는 **읽기만** 한다(`@Immutable` · 쓰기 경로 없음).
 * 자기 엔티티와 같은 영속성 단위에 두는 이유는 **QueryDSL 로 자기 테이블과 함께 읽기** 위해서다(class-diagram §3.9).
 * 스키마는 `search_path` 가 아니라 `@Table(schema)` 로만 닿는다(앱의 search_path 는 `food_service` 하나다).
 *
 * **이 MS 가 쓰는 컬럼만** 매핑한다 — Hibernate `validate` 는 매핑한 컬럼만 기동 때 검사하므로, 소유자가 다른 컬럼을
 * 늘리거나 바꿔도 이 MS 기동이 깨지지 않는다. 컬럼을 더 읽어야 하면 여기에 더한다(schema_mapping_test 목록도 함께).
 * ⚠️ 앱 계정에 `shared` USAGE · SELECT 권한이 없으면 validate 에서 기동이 실패한다.
 *
 * 처방 · 접수자료 · 변경요청과는 **id 만으로** 연결한다 — `patient_id` = `patient_id`(둘 다 uuid).
 */
@Entity
@Immutable
@Table(schema = SHARED_SCHEMA, name = "patient")
class shared_patient protected constructor() {

    @Id
    @Column(name = "patient_id", nullable = false)
    var patient_id: UUID = UUID(0, 0)
        protected set

    /** 환자번호(차트번호) — 비어 있을 수 있다. 화면의 「등록번호」. */
    @Column(name = "patient_number")
    var patient_number: String? = null
        protected set

    @Column(name = "patient_name", nullable = false)
    var patient_name: String = ""
        protected set

    @Column(name = "birth_date")
    var birth_date: LocalDate? = null
        protected set

    /** 성별 코드 — 값 체계가 고정되지 않았다(조회 매핑에서 M · F · U 로 맞춘다). */
    @Column(name = "gender_code")
    var gender_code: String? = null
        protected set
}

/** 공유 스키마 이름 — 프로젝트 정책으로 고정된 값이다. */
const val SHARED_SCHEMA = "shared"
```

> 주석의 `food_service` 는 실제 `application.yml` 의 search_path(`nutrition`)와 다르다 — §8 참조.

`infrastructure/shared/shared_encounter.kt:11-41`

```kotlin
/**
 * `shared.encounter` 읽기 전용 매핑 — 프로젝트 공유 스키마의 수진(내원).
 *
 * 쓰는 곳은 둘이다 — ① 수신 이벤트의 환자 id 찾기(`shared_encounter_reader`: 수진 → 환자)
 * ② 입원일 · 퇴원일 표시(`start_at` · `end_at` — 처방 상세의 입원일시, 병동 보드의 급식 대상 구간).
 * **쓰는 컬럼만** 매핑한다(shared_patient 와 같은 이유).
 */
@Entity
@Immutable
@Table(schema = SHARED_SCHEMA, name = "encounter")
class shared_encounter protected constructor() {

    @Id
    @Column(name = "encounter_id", nullable = false)
    var encounter_id: UUID = UUID(0, 0)
        protected set

    @Column(name = "patient_id", nullable = false)
    var patient_id: UUID = UUID(0, 0)
        protected set

    /** 진료(입원)일시 — 입원일의 원천(KST 일자로 바꿔 쓴다). */
    @Column(name = "start_at", nullable = false)
    var start_at: Instant = Instant.EPOCH
        protected set

    /** 종료(퇴원)일시 — 비어 있으면 아직 끝나지 않았다. */
    @Column(name = "end_at")
    var end_at: Instant? = null
        protected set
}
```

shared 조회 헬퍼(**JOIN 이 아니라 id 묶음 `in` 조회 + 상관 `exists` 서브쿼리**) — `infrastructure/queries/shared_directory.kt:18-59, 81-108`

```kotlin
/**
 * shared 원천 조회 — 환자 · 수진 · 직원 · 진료과(class-diagram §3.9).
 *
 * 이 MS 의 테이블은 **id(uuid)만** 갖고, 화면에 필요한 이름 · 번호 · 생년 · 성별은 여기서 읽는다.
 * - 이름은 화면 한 번에 필요한 id 를 모아 `in (...)` 한 번으로 읽는다(행마다 읽지 않는다).
 * - 검색어(환자명 · 환자번호)는 `patient_matches` 의 `exists` 상관 서브쿼리로 건다 — 목록 · 건수 쿼리의 모양을 바꾸지 않는다.
 * - 연결은 **id 만**이다 — 테넌트 컬럼을 조건에 넣지 않는다(uuid 가 이미 유일하다). 조회 범위는 이 MS 쪽 테이블에 건다.
 *
 * shared 에 짝이 없으면 그 id 는 결과 맵에 없다 — 호출 측이 이름을 null 로 둔다(id 는 그대로 보인다).
 */
@Component
class shared_directory(
    private val query_factory: JPAQueryFactory,
) {
    private val patient = Qshared_patient.shared_patient
    private val encounter = Qshared_encounter.shared_encounter
    private val user_account = Qshared_user_account.shared_user_account
    private val department = Qshared_department.shared_department

    /** 환자 id → 환자 view_model(shared 값). */
    fun patients(ids: Collection<UUID>): Map<UUID, patient_view_model> =
        chunked(ids) { chunk -> patient_query(chunk).fetch() }
            .associate { row ->
                val id = row.get(patient.patient_id)!!
                id to patient_view_model(
                    patient_id = id,
                    patient_no = row.get(patient.patient_number),
                    patient_name = row.get(patient.patient_name),
                    birth_date = row.get(patient.birth_date),
                    gender_code = normalize_gender(row.get(patient.gender_code)),
                )
            }

    /** 수진 id → 진료(입원) · 종료(퇴원) 일시. */
    fun encounters(ids: Collection<UUID>): Map<UUID, encounter_period> =
        chunked(ids) { chunk -> encounter_query(chunk).fetch() }
            .associate { row ->
                row.get(encounter.encounter_id)!! to encounter_period(
                    start_at = row.get(encounter.start_at)!!,
                    end_at = row.get(encounter.end_at),
                )
            }
    …
    /**
     * 검색어 조건 — 그 환자의 shared 이름 · 환자번호에 검색어가 들어 있는지(`exists` 상관 서브쿼리).
     * @param patient_id 이 MS 테이블의 환자 id 경로(uuid)
     */
    fun patient_matches(patient_id: ComparablePath<UUID>, keyword: String): BooleanExpression {
        val matched = Qshared_patient("matched_patient")
        return JPAExpressions.selectOne()
            .from(matched)
            .where(
                matched.patient_id.eq(patient_id)
                    .and(matched.patient_name.containsIgnoreCase(keyword).or(matched.patient_number.containsIgnoreCase(keyword))),
            )
            .exists()
    }

    // ----- 실행 전 쿼리 — 실행 없이 HQL 해석을 검사하는 테스트(shared_join_query_test)도 쓴다 -----

    internal fun patient_query(ids: Collection<UUID>): JPAQuery<Tuple> =
        query_factory
            .select(patient.patient_id, patient.patient_number, patient.patient_name, patient.birth_date, patient.gender_code)
            .from(patient)
            .where(patient.patient_id.`in`(ids))

    internal fun encounter_query(ids: Collection<UUID>): JPAQuery<Tuple> =
        query_factory
            .select(encounter.encounter_id, encounter.start_at, encounter.end_at)
            .from(encounter)
            .where(encounter.encounter_id.`in`(ids))
```

수신 이벤트용 shared 포트/어댑터 — `application/ports/i_shared_encounter_reader.kt:5-15` · `infrastructure/shared/shared_encounter_reader.kt:9-23`

```kotlin
/**
 * shared 수진 조회 포트 — 수신 이벤트의 환자 id 를 정하는 데 쓴다(class-diagram §11.1 id 규칙).
 *
 * 발행처(x-ancillary-main)는 `patientId` 자리에 환자번호를 싣고 `encounterId` 에 shared 수진 uuid 를 싣는다 —
 * 그래서 payload 의 환자 id 가 UUID 가 아니면 수진 uuid 로 `shared.encounter.patient_id` 를 찾는다.
 * 구현은 infrastructure/shared 의 shared_encounter_reader(QueryDSL, 읽기 전용 매핑)다.
 */
interface i_shared_encounter_reader {
    /** 그 수진의 환자 id — shared 에 수진이 없으면 null. */
    fun patient_id_of(encounter_id: UUID): UUID?
}
```

```kotlin
@Repository
class shared_encounter_reader(
    private val query_factory: JPAQueryFactory,
) : i_shared_encounter_reader {

    private val e = Qshared_encounter.shared_encounter

    @Transactional(readOnly = true)
    override fun patient_id_of(encounter_id: UUID): UUID? =
        query_factory.select(e.patient_id).from(e).where(e.encounter_id.eq(encounter_id)).fetchFirst()
}
```

---

## 5. JPA 매핑

> 이 레포에는 **`@Embedded` / `@Embeddable` 이 없다**(grep 결과 0건). VO(`diet_prescription` · `meal_attributes` · `tenant_scope`)는 **영속 타입이 아닌 Kotlin data class** 이고, 엔티티가 컬럼을 하나씩 따로 매핑한 뒤 `prescription()` / `attributes()` 같은 메서드로 묶어 내준다. seedwork 의 `value_object` 추상 클래스(`domain/seedwork/value_object.kt`)는 **상속하는 클래스가 없다**(grep `: value_object()` 0건).
> 감사·테넌트 컬럼은 베이스 `entity`(§3.7 인용)가 갖고, 값은 `audit_entity_listener` 가 채운다.

### 5.1 Aggregate Root — `diet_order` — `domain/aggregatesmodel/dietorder/diet_order.kt:24-187`

`@Entity` · `@Table` · `@AttributeOverride(id→diet_order_id)` · `@Enumerated` · `@Version` · 다른 AR 는 UUID 논리참조(JPA 연관 없음).

```kotlin
/**
 * 의사 식이처방 한 건의 Aggregate Root. (class-diagram.md §3.1)
 *
 * FHIR 리소스: **NutritionOrder** (`intent = order`).
 * 읽기 원본은 처방 BC 이고 본 서비스는 Kafka 수신 사본을 보관한다 — 그래서 이 애그리거트는
 * 사용자 액션으로 **퇴식끼니 설정** 하나만 노출하고, 나머지는 **이벤트 추종**(receive · apply_from_event · revoke ·
 * receive_discharge · cancel_discharge)이다.
 *
 * 멱등은 `order_number`(원본 처방번호) 자연키 + `source_event_version` 이 담당한다
 * (consumed_message 테이블을 두지 않는다). 자연키는 테넌트 범위 안에서 유일하다.
 *
 * **수진 스냅샷을 두지 않는다**(class-diagram §3.9) — 환자 · 수진 · 진료과 · 처방의는 shared 의 uuid 로만 가리키고
 * 이름 · 번호 · 입원일은 조회 때 shared 에서 읽는다. 병동 · 병실 · 병상은 처방 이벤트가 실어 온 값을 **참고용으로**
 * 보관할 뿐 전동을 따라가지 않는다(전동 토픽을 받지 않는다).
 */
@Entity
@Table(name = "diet_order")
@AttributeOverride(name = "id", column = Column(name = "diet_order_id"))
class diet_order protected constructor() : entity(), i_aggregate_root {

    /** AR 식별자 별칭 — 화면/이벤트 표기는 diet_order_id 다. ⚠ HQL 에는 쓰지 않는다(실제 속성은 id). */
    val diet_order_id: UUID get() = id

    /** 처방 BC 원본 처방번호 — NutritionOrder.identifier. 테넌트 범위 안에서 유니크. */
    @Column(name = "order_number", nullable = false, length = 64)
    var order_number: String = ""
        protected set

    /** 환자 — NutritionOrder.subject. `shared.patient.patient_id`. */
    @Column(name = "patient_id", nullable = false)
    var patient_id: UUID = UUID(0, 0)
        protected set

    /** 수진 — NutritionOrder.encounter. `shared.encounter.encounter_id`. 퇴원지시 연결 키다. */
    @Column(name = "encounter_id", nullable = false)
    var encounter_id: UUID = UUID(0, 0)
        protected set

    /** 진료과 — `shared.department.department_id`. 발행처 값이 UUID 가 아니면 null. */
    @Column(name = "department_id")
    var department_id: UUID? = null
        protected set

    /**
     * 처방의 — NutritionOrder.orderer. `shared.user_account.user_account_id`.
     * 발행처가 싣지 않았거나 UUID 가 아니면 null 이다(수진의 진료의로 대체하지 않는다 — 대진·협진이면 틀린 값이 된다).
     */
    @Column(name = "orderer_id")
    var orderer_id: UUID? = null
        protected set

    /**
     * 식이처방 코드 — NutritionOrder.oralDiet.type. **Diet 마스터의 `diet_code`** 다(class-diagram §3.5.1).
     * 코드 값 논리 참조라 마스터 행이 바뀌어도 이 처방은 그대로다. 금식도 이 자리로 온다(`Diet.diet_category_code = NPO`).
     */
    @Column(name = "diet_type_code", nullable = false, length = 64)
    var diet_type_code: String = ""
        protected set

    // ----- 병동 · 병실 · 병상 — 처방 이벤트가 실어 온 값(참고 · 조회용). 갱신하지 않는다 -----

    @Column(name = "ward_id", length = 64)
    var ward_id: String? = null
        protected set
    …
    // ----- 처방 속성 (전부 선택) — 비운 값은 null 그대로 두고 세부식사 자동 설정이 식종 기본값으로 채운다 -----

    /** 형태(밥 · 죽 · 미음 · 맑은미음 · 유동식) — oralDiet.texture. 완제품 · 금식 식종은 null. */
    @Column(name = "meal_form_code", length = 32)
    var meal_form_code: String? = null
        protected set
    …
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    var status: diet_order_status = diet_order_status.ACTIVE
        protected set

    @Column(name = "revoke_reason", length = 500)
    var revoke_reason: String? = null
        protected set

    /** 처방일시 — NutritionOrder.dateTime. */
    @Column(name = "ordered_at", nullable = false)
    var ordered_at: Instant = Instant.EPOCH
        protected set

    /** Kafka 수신 일시 (Custom). */
    @Column(name = "received_at", nullable = false)
    var received_at: Instant = Instant.EPOCH
        protected set

    /** 처방 이벤트 순서 역전 방지(멱등) — 동일/이전 버전 재수신은 no-op. */
    @Column(name = "source_event_version", nullable = false)
    var source_event_version: Long = 0
        protected set

    // ----- 퇴원 예정일 · 퇴식끼니 (class-diagram §3.1) -----

    /** 퇴원 예정일 — 퇴원지시(DischargeOrdered) 수신 값. null 이면 퇴원지시가 없다. 이 날짜 다음날부터의 식사는 없다. */
    @Column(name = "discharge_planned_date")
    var discharge_planned_date: LocalDate? = null
        protected set

    /** 퇴식끼니 — 퇴원일에 마지막으로 먹는 끼니(`MealTypeSetting.meal_type_code`). null 이면 미설정. */
    @Column(name = "discharge_last_meal_type_code", length = 16)
    var discharge_last_meal_type_code: String? = null
        protected set

    /** 퇴식끼니를 정한 주체 — NURSE(간호 상담 입력) / PATIENT(환자 직접 입력). */
    @Column(name = "discharge_meal_set_by_code", length = 10)
    var discharge_meal_set_by_code: String? = null
        protected set

    @Column(name = "discharge_meal_set_at")
    var discharge_meal_set_at: Instant? = null
        protected set

    @Version
    @Column(name = "version", nullable = false)
    var version: Long = 0
        protected set
```

`@OneToMany` 자식 보유 패턴은 `meal_order` 에서(단방향 + `@JoinColumn`, 자식은 `@ManyToOne` 없음) — `domain/aggregatesmodel/mealorder/meal_order.kt:104-115`

```kotlin
    /**
     * 적용일자별 속성 이력 — Aggregate 내부 하위 엔티티.
     * 단방향 `@OneToMany` + `@JoinColumn` 이라 하위가 다른 애그리거트를 `@ManyToOne` 으로 참조하지 않는다.
     */
    @OneToMany(cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "meal_order_id", nullable = false)
    @OrderBy("effective_from asc")
    protected var revision_history: MutableList<meal_order_revision> = mutableListOf()

    /** 적용일자 오름차순 이력의 불변 스냅샷. */
    val revisions: List<meal_order_revision>
        get() = revision_history.sortedBy { it.effective_from }
```

같은 패턴: `diet` → `meal` · `meal_attribute` (`diet.kt:90-96`), `foodservice_setting` → `meal_type_setting` · `meal_closing_setting` (`foodservice_setting.kt:48-53`), `meal_preparation` → `meal_closing` (`meal_preparation.kt:230`).

### 5.2 자식 Entity — `meal_order_revision` — `domain/aggregatesmodel/mealorder/meal_order_revision.kt:15-60, 177-212`

`entity()` 만 상속(`i_aggregate_root` 없음). 부모 FK 컬럼은 `insertable=false, updatable=false` 읽기 사본. 생성은 `internal` 팩토리로 Root 경유만.

```kotlin
/**
 * 세부오더의 **적용일자별 속성 이력** — MealOrder Aggregate 내부 하위 엔티티. (class-diagram.md §3.2.1)
 *
 * 세부오더는 여러 날에 걸쳐 적용된다. 속성을 오더 한 행에 담으면 오늘 양을 줄인 것이 **지난주 배식
 * 기록까지** 바꾼다. 그래서 값은 항상 "언제부터" 와 함께 보관하고, 수정은 직전 구간을 닫고 새 구간을
 * 여는 것으로 표현한다.
 *
 * 구간(`effective_from ~ effective_to`)은 서로 겹치지 않고 빈틈이 없으며, 마지막 리비전만
 * `effective_to = null`(이후 계속)이다. 생성·변경은 Root(`meal_order`) 를 거친다.
 * 양·간·열량·용량·병수의 **허용 범위는 `MealAttribute`** 가 정한다 — 검증은 핸들러가 Diet 로 한다(FS-4005).
 */
@Entity
@Table(name = "meal_order_revision")
@AttributeOverride(name = "id", column = Column(name = "meal_order_revision_id"))
class meal_order_revision protected constructor() : entity() {

    val meal_order_revision_id: UUID get() = id

    /** 상위 세부오더 — 단방향 `@JoinColumn` 이라 이 값은 조회 편의용 사본이다. */
    @Column(name = "meal_order_id", nullable = false, insertable = false, updatable = false)
    var meal_order_id: UUID = NIL_ID
        protected set

    /** 이 값들이 적용되는 첫 식사일자. */
    @Column(name = "effective_from", nullable = false)
    var effective_from: LocalDate = LocalDate.EPOCH
        protected set

    /** 마지막 식사일자 — null 이면 이후 계속. */
    @Column(name = "effective_to")
    var effective_to: LocalDate? = null
        protected set

    /**
     * 식이 코드 — `Meal.meal_code`(class-diagram §3.5.2). 같은 식이처방을 **어떤 상품으로** 낼지다
     * (일반식 → 일반/외국인 특별식/서양식, 경관 → 제품, 분유 → 일반/특수).
     * 처방의 식이처방 코드의 식이 코드여야 한다(FS-4003 — 검증은 핸들러). 식이 코드가 없는 식이처방이면 null 이다.
     */
    @Column(name = "meal_code", length = 64)
    var meal_code: String? = null
        protected set

    /** 경관 완제품 병수 — 식이 코드 단위가 UNIT(병)일 때만 뜻이 있다. */
    @Column(name = "unit_qty")
    var unit_qty: Int? = null
        protected set
    …
    companion object {

        /** 새 구간 열기 — Root 경유로만 호출한다. */
        internal fun open(
            scope: tenant_scope,
            effective_from: LocalDate,
            effective_to: LocalDate?,
            values: meal_attributes,
            at: Instant,
        ): meal_order_revision {
            values.validate()
            if (effective_to != null && effective_to.isBefore(effective_from)) {
                throw bad_state_domain_exception("리비전 종료일이 시작일보다 빠릅니다.", "ACTIVE")
            }
            return meal_order_revision().apply {
                assign_id()
                assign_tenant(scope)
                this.effective_from = effective_from
                this.effective_to = effective_to
                this.meal_code = values.meal_code
                …
                this.revised_at = at
            }
        }
    }
}
```

### 5.3 VO — `diet_prescription` — `domain/aggregatesmodel/dietorder/order_value_objects.kt:8-46`

```kotlin
/**
 * 식이처방 애그리거트의 Value Object. (class-diagram §3.1)
 *
 * 둘 다 영속 타입이 아니라 값 묶음이다 — `diet_order` 가 컬럼을 따로 매핑하고 이 값으로 읽고 건넨다.
 * 수진 스냅샷은 두지 않는다 — 수진 · 진료과 · 처방의는 id 만 보관하고 shared 를 조인한다(class-diagram §3.9).
 */

/**
 * 의사 처방의 속성 한 벌 — 형태 · 염도 · 열량 · 양 · 격리 · 용량 · 개수. **전부 선택값이다.** (class-diagram §3.1)
 *
 * 처방은 식종만 있어도 성립한다 — 비운 값은 처방에 null 그대로 두고, 세부식사를 자동 설정할 때 식종 기본값이 채운다(`resolve`).
 * 영속 타입이 아니라 값 묶음이고, `diet_order` 가 컬럼으로 따로 매핑한다.
 */
data class diet_prescription(
    /** 형태(밥 · 죽 · 미음 · 맑은미음 · 유동식) — oralDiet.texture. 완제품 · 금식 식종은 null. */
    val meal_form_code: String? = null,
    /** 양 — 많이 · 보통 · 적게. */
    val portion_code: String? = null,
    /** 염도 — oralDiet.nutrient(sodium). 완제품 · 금식 식종은 null. */
    val salt_level_code: String? = null,
    /** 격리 배식 — 비우면 false(식종 기본값이 없다 — 환자 상태 값이다). */
    val is_isolation_meal: Boolean? = null,
    /** 1일 목표 열량 — oralDiet.nutrient(energy).amount. 당뇨식 1800 등. */
    val kcal: Int? = null,
    /** 1일 용량 ml — enteralFormula.administration.quantity. */
    val volume_ml: Int? = null,
    /** 개수 — 완제품 캔 · 팩 · 병 수. */
    val unit_qty: Int? = null,
) {
    /** null 인 항목은 "바꾸지 않는다" — 변경 이벤트는 변경분만 싣는다. */
    fun merge_onto(base: diet_prescription): diet_prescription = diet_prescription(
        meal_form_code = meal_form_code ?: base.meal_form_code,
        portion_code = portion_code ?: base.portion_code,
        salt_level_code = salt_level_code ?: base.salt_level_code,
        is_isolation_meal = is_isolation_meal ?: base.is_isolation_meal,
        kcal = kcal ?: base.kcal,
        volume_ml = volume_ml ?: base.volume_ml,
        unit_qty = unit_qty ?: base.unit_qty,
    )
```

VO 2 — `meal_attributes` (`meal_order_revision.kt:214-239`) — 리비전이 보유하고 접수자료가 복사해 가는 값 묶음.

```kotlin
/**
 * 세부오더의 조리 속성 한 벌 — 리비전이 보유하고 접수자료가 복사해 가는 값이다.
 *
 * 영속 타입이 아니라 **값 묶음**이다(리비전·접수자료의 컬럼은 각자 따로 매핑한다) — 임베디드로 두면
 * 접수자료의 스냅샷 컬럼까지 한 이름에 묶여 마감 이후 구조 변경이 서로를 끌어당긴다.
 */
data class meal_attributes(
    /** 식이 코드 — 식이 코드가 없는 식이처방이면 null. */
    val meal_code: String? = null,
    /** 경관 완제품 병수 — 식이 코드 단위가 UNIT 일 때만. */
    val unit_qty: Int? = null,
    /** 형태 — 완제품 · 금식 식종은 null. */
    val meal_form_code: String? = com.projectx.foodservice.domain.common.meal_form_code.REGULAR,
    val portion_code: String = com.projectx.foodservice.domain.common.portion_code.NORMAL,
    /** 염도 — 완제품 · 금식 식종은 null. */
    val salt_level_code: String? = com.projectx.foodservice.domain.common.salt_level_code.NORMAL,
    /** 열량 — 축이 허용할 때만 값이 있다. */
    val kcal: Int? = null,
    /** 용량 ml — 경관 식이처방에서만. */
    val volume_ml: Int? = null,
    val is_isolation_meal: Boolean = false,
    val is_guardian_meal: Boolean = false,
    val guardian_portion_code: String = com.projectx.foodservice.domain.common.portion_code.NORMAL,
    val guardian_salt_level_code: String = com.projectx.foodservice.domain.common.salt_level_code.NORMAL,
    val is_guardian_isolation_meal: Boolean = false,
)
```

### 5.4 enum — `domain/common/foodservice_enums.kt:12-57`

```kotlin
enum class diet_order_status {
    DRAFT,
    ACTIVE,
    ON_HOLD,
    REVOKED,
    COMPLETED,
    ;

    /** 세부오더 발행 가능 여부 — DietOrder.isActive(). */
    fun is_active(): Boolean = this == ACTIVE

    fun is_terminal(): Boolean = this == REVOKED || this == COMPLETED
}

/** 세부오더 상태. ACTIVE → REVOKED / COMPLETED */
enum class meal_order_status {
    ACTIVE,
    REVOKED,
    COMPLETED,
    ;

    fun is_terminal(): Boolean = this == REVOKED || this == COMPLETED
}
…
enum class meal_preparation_status {
    REQUESTED,
    ACCEPTED,
    IN_PROGRESS,
    COMPLETED,
    CANCELLED,
    NOT_SERVED,
    ;

    /** 종료 상태(더 이상 상태 변경 불가). */
    fun is_terminal(): Boolean = this == COMPLETED || this == CANCELLED

    /** 조리량 집계(식이처방별 집계·배식표)에 포함되는지 — 결식/취소는 제외한다. */
    fun counts_for_aggregation(): Boolean =
        this == REQUESTED || this == ACCEPTED || this == IN_PROGRESS || this == COMPLETED
}
```

> 같은 파일에 `portion_code` · `salt_level_code` · `meal_form_code` · `meal_type_code` · `diet_group_code` 등은 enum 이 아니라 **`object` 상수 묶음(String 코드)** 이다(`foodservice_enums.kt:122-284`) — 값 집합을 DB 마스터/설정이 정하기 때문. 시간대 상수 `KST` 는 `domain/common/hospital_zone.kt:6`.

### 5.5 감사·테넌트 컬럼 채우기 — `infrastructure/persistence/audit_entity_listener.kt:13-43` + `META-INF/orm.xml:1-19`

```kotlin
/**
 * 감사 컬럼(created_at · created_by · updated_at · updated_by) 채우기 — 모든 Entity 의 JPA 기본 리스너.
 *
 * - 등록은 `META-INF/orm.xml` 의 persistence-unit-defaults 다 — domain 이 infrastructure 를 참조하지 않게
 *   `@EntityListeners` 를 Entity 에 달지 않는다(application.yml 의 `spring.jpa.mapping-resources` 가 orm.xml 을 읽는다).
 * - 직원 ID 는 MDC `userId` 에서 읽는다. 두 진입 경로가 모두 **커밋이 끝날 때까지** 이 값을 넣어 둔다:
 *   HTTP 는 common-jwt-kt 의 JwtAuthenticationFilter(토큰 sub = 직원 ID), 수신 이벤트는 common-eventbus-kt 가
 *   봉투의 userId 로 채운다. 그래서 flush 가 늦게 일어나는 @PreUpdate 에서도 같은 값을 본다.
 *   직원 ID 는 shared 직원 uuid 다 — 비로그인 발행(`anonymous`) · 값 없음 · UUID 형식이 아니면 null 로 남긴다.
 * - 부모 · 자식처럼 함께 저장되는 행도 Entity 마다 콜백이 따로 불려 테이블마다 따로 채워진다.
 *
 * Hibernate 가 기본 생성자로 만든다 — Spring 빈이 아니고 상태를 갖지 않는다.
 */
class audit_entity_listener {

    @PrePersist
    fun on_pre_persist(target: Any) {
        (target as? entity)?.record_creation(Instant.now(), current_employee_id())
    }

    @PreUpdate
    fun on_pre_update(target: Any) {
        (target as? entity)?.record_modification(Instant.now(), current_employee_id())
    }

    private fun current_employee_id(): UUID? =
        MDC.get(JwtKeys.USER_ID)
            ?.trim()
            ?.takeIf { it.isNotEmpty() && it != MessageEnvelope.ANONYMOUS_USER_ID && UUID_FORMAT.matches(it) }
            ?.let(UUID::fromString)
}
```

```xml
<!--
  JPA 기본 엔티티 리스너 등록 — 모든 Entity 의 감사 컬럼(created_* · updated_*)을 audit_entity_listener 가 채운다.
  Entity 클래스에 @EntityListeners 를 달면 domain 이 infrastructure 를 참조하게 되므로(ArchUnit 위반) 여기서 등록한다.
  application.yml 의 spring.jpa.mapping-resources 가 이 파일을 읽는다 — 그 설정이 빠지면 감사 컬럼이 EPOCH · null 로 저장된다.
  ⚠️ 리스너 FQCN 은 문자열이라 컴파일러가 못 잡는다 — 클래스를 옮기거나 이름을 바꾸면 여기도 고친다.
-->
<entity-mappings xmlns="https://jakarta.ee/xml/ns/persistence/orm" … version="3.1">
    <persistence-unit-metadata>
        <persistence-unit-defaults>
            <entity-listeners>
                <entity-listener class="com.projectx.foodservice.infrastructure.persistence.audit_entity_listener"/>
            </entity-listeners>
        </persistence-unit-defaults>
    </persistence-unit-metadata>
</entity-mappings>
```

### 5.6 기타 매핑 — `@Convert` 목록 컬럼 · `@JdbcTypeCode(JSON)`

`domain/seedwork/string_list_converter.kt:6-20` (`meal_attribute.allowed_level_codes` · `meal_type_setting.applicable_diet_codes` 에 `@Convert`)

```kotlin
/**
 * `List<String>` ↔ 쉼표 구분 VARCHAR.
 *
 * 허용 단계 · 대상 식이처방처럼 **짧은 코드 목록**을 한 컬럼에 둘 때 쓴다 — Postgres 배열 타입은
 * `ddl-auto: validate` 와 Liquibase 타입 표기가 어긋나기 쉬워 피한다. 빈 목록은 null 로 저장한다.
 */
@Converter
class string_list_converter : AttributeConverter<List<String>, String?> {

    override fun convertToDatabaseColumn(attribute: List<String>?): String? =
        attribute?.map { it.trim() }?.filter { it.isNotEmpty() }?.takeIf { it.isNotEmpty() }?.joinToString(",")

    override fun convertToEntityAttribute(dbData: String?): List<String> =
        dbData?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() } ?: emptyList()
}
```

`meal_change_request.request_detail` 은 `@JdbcTypeCode(SqlTypes.JSON)` jsonb (`domain/aggregatesmodel/mealchangerequest/meal_change_request.kt:76`).

---

## 6. 이벤트 수신/발행 (common-eventbus-kt)

### 6.0 현재 wiring 요약 (소스 근거)

- **라우팅 등록은 발행 `diet-definition` 토픽 1개뿐**이다 — `infrastructure/eventbus/topic_registry.kt:17-18` 주석 "지금 등록한 route 는 발행 `diet-definition` 하나다 … 수신 구독은 없고".
- 수신 핸들러(`diet_order_*_handler` · `discharge_*_handler`)는 `@Component` 로 존재하고 `handle(envelope)` 를 열어 두었지만, **`topic_registry` 에 수신 route 가 없어 실제 구독은 걸리지 않는다**(`idempotent_integration_event_handler.kt:19` 주석 "구독 토픽은 미정이라 현재 등록된 구독이 없다"). `grep -rn "subscribe|inbox"` → 0건. **inbox(수신 메시지) 테이블도 없다** — 멱등은 자연키 `order_number` + `source_event_version` 이 담당한다(`diet_order.kt:32-33`).
- Raw `@KafkaListener` 없음(grep 0건) — 라이브러리 `EventBus` 만 쓴다.

### 6.1 토픽 레지스트리 — `infrastructure/eventbus/topic_registry.kt:11-67`

```kotlin
/**
 * 이벤트 ↔ 토픽 라우팅.
 *
 * **토픽은 `{환경}.{테넌트id}.{이벤트명}` 한 형태다** — 앞의 두 마디(`local.chabd` · `dev.chabd`)는
 * `common.event-bus.namespace` 가 붙이므로 `kafkaTopic` 에는 **이벤트명만** 적는다(값은 이벤트 클래스의 `TOPIC` 상수).
 *
 * 지금 등록한 route 는 **발행 `diet-definition` 하나**다(class-diagram §11 머리의 현재 wiring 상태) — 수신 구독은 없고,
 * 나머지 토픽은 개발하며 필요한 것부터 붙인다.
 * `partitionKeyField` 는 라이브러리(`MapEventRouter`)가 부팅 때 그 이름의 속성이 이벤트 클래스에 있는지 검사한다.
 * 발행 토픽에는 `kafkaPartitions` 를 준다 — 이 MS 가 토픽의 주인이고, 없으면 브로커 auto-create 가 꺼진 환경에서
 * 첫 발행이 `Topic ... not present in metadata` 로 실패한다. Rabbit 값은 provider 전환 대비 자리만 채운다(provider = kafka).
 */
@Configuration
class topic_registry {

    @Bean
    fun event_router(): EventRouter = MapEventRouter(
        buildMap<KClass<*>, EventRoute> {
            // 발행 — 식이처방 코드 정의. 추가 · 수정 · 사용 중지가 한 messageType · 한 토픽이고, 같은 식이처방의 순서를 dietCode 로 묶는다.
            put_route(
                diet_integration_event.TOPIC,
                diet_integration_event.PARTITION_KEY_FIELD,
                diet_integration_event::class,
                partitions = OWNED_PARTITIONS,
            )
        },
    )

    /** 이벤트 클래스 1개 = route 1개 — 같은 토픽의 route 는 토픽 · group · partition key 가 같아야 한다. */
    private fun MutableMap<KClass<*>, EventRoute>.put_route(
        topic: String,
        partition_key_field: String,
        event_type: KClass<*>,
        partitions: Int? = null,
    ) {
        put(
            event_type,
            EventRoute(
                rabbitExchange = "$topic-exchange",
                rabbitRoutingKey = topic,
                rabbitQueue = "$CONSUMER_PREFIX.$topic",
                kafkaTopic = topic,
                partitionKeyField = partition_key_field,
                consumerGroupId = "$CONSUMER_PREFIX.$topic",
                kafkaPartitions = partitions,
            ),
        )
    }

    private companion object {
        /** consumer group 접두 — 앞에 namespace 가 붙어 `dev.chabd.foodservice.<이벤트명>` 이 된다. */
        const val CONSUMER_PREFIX = "foodservice"

        /** 이 MS 가 주인인 토픽의 파티션 수. 순서는 partitionKey 가 보장하므로 개수는 처리량 문제다. */
        const val OWNED_PARTITIONS = 3
    }
}
```

### 6.2 `application.yml` kafka / event-bus 부분 — `src/main/resources/application.yml:66-100`

```yaml
  kafka:
    bootstrap-servers: ${SPRING_KAFKA_BOOTSTRAP_SERVERS:localhost:9092}
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.apache.kafka.common.serialization.StringSerializer
      # 로컬 컨테이너 환경이므로 실패 시 더 빨리 응답을 끊도록 타임아웃을 짧게 조정.
      properties:
        max.block.ms: 5000
        request.timeout.ms: 5000
        delivery.timeout.ms: 10000
        linger.ms: 0
        retries: 1
    # ⚠️ **토픽 생성(KafkaAdmin)이 기동을 붙잡지 않게 한다.**
    admin:
      fail-fast: false
      operation-timeout: 5s
      properties:
        request.timeout.ms: 5000
        default.api.timeout.ms: 5000
        retries: 1
    consumer:
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      auto-offset-reset: earliest
    listener:
      missing-topics-fatal: false
kafka_namespace: ${KAFKA_NAMESPACE:local.chabd}

common:
  event-bus:
    namespace: ${kafka_namespace}
    # ⚠️ common-eventbus-kt 0.1.18 은 기본으로 namespace 앞에 환경(profile)을 또 붙인다 — namespace 가 이미
    #    `{환경}.{테넌트id}` 라 켜 두면 토픽이 `dev.dev.chabd.…` 가 되어 발행측과 갈라진다. 끈다.
    env-prefix-enabled: false
    provider: ${PROJECTX_EVENT_BUS_PROVIDER:kafka}
```

→ 최종 토픽명: `local.chabd.diet-definition` (namespace + `diet_integration_event.TOPIC`). 수신 예정 토픽명은 payload 클래스 주석에 `{환경}.{테넌트id}.diet-order` · `…discharge-order` 로 적혀 있다(`inbound_integration_events.kt:11-12`, `discharge_order_event_handlers.kt:14`).

### 6.3 통합 이벤트 베이스 + 수신 payload 클래스 — `@MessageType` · `@SchemaVersion` · `@Aggregate` · camelCase

`application/integrationevents/integration_event.kt:3-11`

```kotlin
/**
 * 통합(integration) 이벤트 베이스.
 * common-eventbus-kt 0.1.6 부터 이벤트는 평범한 payload 클래스이며, 식별자/시각/테넌트 등
 * 메타데이터는 봉투(com.projectx.common.eventbus.dto.MessageEnvelope)가 보유한다.
 * 메시지 타입/스키마/aggregate 는 각 이벤트의 @MessageType/@SchemaVersion/@Aggregate 로 선언한다
 * (누락 시 EnvelopeFactory 가 봉투를 못 채워 발행이 500 으로 실패한다).
 * 필드는 **camelCase** 다(프로퍼티명이 그대로 JSON 키) — 다른 BC 와 봉투(`MessageEnvelope`)가 쓰는 표기에 맞춘다. 전 영역 snake_case 규칙의 예외다.
 */
abstract class integration_event
```

수신 — 식단 처방(diet-order) — `application/integrationevents/events/inbound_integration_events.kt:8-57, 98-128`

```kotlin
/**
 * 인바운드 통합 이벤트. (class-diagram.md §11 / api-spec §6)
 *
 * **토픽은 `{환경}.{테넌트id}.{이벤트명}` 한 형태다** — 식이처방은 `dev.chabd.diet-order` 하나로 생성·변경·취소가
 * 모두 흐르고, 행위는 봉투의 `@MessageType` 이 구분한다(행위는 토픽에 넣지 않는다). 라우팅은 `topic_registry`.
 *
 * **필드 어휘는 x-ancillary-main 의 발행 규칙을 따른다.** 처방 BC 정본이 개발되기 전까지 그 레포(케이스
 * 생성기)가 이 토픽의 발행처이고, 한 벌의 처방 view_model 로 여러 수신 BC 를 먹인다 — 수신측이 어휘를
 * 따로 쓰면 발행처에 BC 별 번역이 쌓인다. 계약의 단일 기준은 `_docs/design/foodservice.classdiagram.md` §11.
 *
 * 수신 전용이라 봉투는 발행 측이 만들지만, 타입 정합/문서화를 위해 애노테이션을 함께 선언한다.
 * 필드는 cross-BC JSON 정합을 위해 **camelCase**(snake_case 규칙의 유일한 예외).
 */

/**
 * 식이처방 생성 — 로컬 영속화(멱등: `orderId`).
 * …
 */
@MessageType("DietOrderCreated")
@SchemaVersion("2")
@Aggregate(type = "DietOrder")
class diet_order_created_integration_event(
    /** 처방번호 — 멱등키이자 partitionKey. */
    val orderId: String,
    /** 식이처방 코드 — 발행처의 `<도메인>DefinitionId` 자리. */
    val dietTypeCode: String,
    /** 식이처방명 — 마스터 미등재 코드의 화면 표기 폴백(로컬 마스터가 이기면 무시). */
    val dietTypeName: String? = null,
    // ----- 처방 조리 지시(전부 선택) — 세부오더 최초 리비전의 기본값 -----
    /** 형태 — 밥 · 죽 · 미음 · 맑은미음 · 유동식(`meal_form` 마스터 코드). 선택 — 비우면 식종 기본값. */
    val mealFormCode: String? = null,
    /** 양 — LARGE / NORMAL / SMALL. */
    val portionCode: String? = null,
    /** 간 — NORMAL / LOW. */
    val saltLevelCode: String? = null,
    /** 격리 배식. */
    val isolationMeal: Boolean? = null,
    /** 열량 kcal(당뇨식 1800 등). */
    val kcal: Int? = null,
    /** 1일 용량 ml — 선택. 비우면 식종 기본값(완제품은 개수 × 단위당 ml). */
    val volumeMl: Int? = null,
    /** 개수 — 완제품 캔 · 팩 · 병 수. 선택. 비우면 식종 기본값(완제품은 열량 ÷ 단위당 kcal 올림). */
    val unitQty: Int? = null,
    val patientId: String,
    …
    val encounterId: String,
    …
    val sourceEventVersion: Long = 1,
) : integration_event()

/** 식이처방 변경 — 식이처방/조리 지시/코멘트/상태 갱신. 스냅샷은 기존 값을 유지한다(변경분만 싣는다). */
@MessageType("DietOrderUpdated")
@SchemaVersion("2")
@Aggregate(type = "DietOrder")
class diet_order_updated_integration_event(
    val orderId: String,
    val dietTypeCode: String? = null,
    …
    val sourceEventVersion: Long = 1,
) : integration_event()

/** 식이처방 취소 — 하위 세부오더 무효화 + 마감 전 접수자료 취소. */
@MessageType("DietOrderRevoked")
@SchemaVersion("2")
@Aggregate(type = "DietOrder")
class diet_order_revoked_integration_event(
    val orderId: String,
    val reason: String? = null,
    val sourceEventVersion: Long = 1,
) : integration_event()
```

수신 — 퇴원지시 — `inbound_integration_events.kt:130-160`

```kotlin
@MessageType("DischargeOrdered")
@SchemaVersion("1")
@Aggregate(type = "DischargeOrder")
class discharge_ordered_integration_event(
    val dischargeOrderId: String,
    val patientId: String? = null,
    /** shared 수진 uuid — 필수. UUID 가 아니면 거절(DLT). partitionKey. */
    val encounterId: String,
    /** 퇴원 예정일 `yyyy-MM-dd` — 필수. */
    val plannedDischargeDate: String,
    val doctorId: String? = null,
    /** ISO-8601 오프셋 포함. */
    val orderedAt: String? = null,
    val sourceEventVersion: Long = 1,
) : integration_event()

/** 퇴원지시 취소 — 퇴원 예정일 · 퇴식끼니를 비운다. 식사는 접수 목록 조회 시 다시 생성된다. */
@MessageType("DischargeOrderRevoked")
@SchemaVersion("1")
@Aggregate(type = "DischargeOrder")
class discharge_order_revoked_integration_event(
    val dischargeOrderId: String,
    val encounterId: String,
    val reason: String? = null,
    val sourceEventVersion: Long = 1,
) : integration_event()
```

### 6.4 핸들러 베이스 — `application/integrationevents/eventhandling/idempotent_integration_event_handler.kt:10-47`

```kotlin
/**
 * 통합 이벤트 핸들러 베이스 — 봉투를 풀어 payload 와 테넌트 범위를 넘기고, 진입/실패만 로깅한다.
 *
 * 테넌트 범위는 여기서 한 번 정한다(`event_tenant_resolver` — 봉투 vs 배포값, class-diagram §3.8).
 * 하위 핸들러는 받은 범위로만 찾고 만든다. 감사 컬럼의 직원 ID 는 eventbus 가 처리 중 MDC `userId` 에 넣어 둔
 * 봉투 값을 영속성 리스너가 읽는다 — 여기서 넘기지 않는다.
 *
 * **멱등성은 도메인/Repository 에 위임**한다(자연키 · source_event_version) — `consumed_message` 테이블은 두지 않는다.
 *
 * **트랜잭션 경계는 `handle()` 이다** — 구독이 부르는 public 메서드라(구독 토픽은 미정이라 현재 등록된 구독이 없다)
 * 프록시를 탄다. `handle_internal` 에 `@Transactional` 을 붙이면 같은 빈 안의 호출(self-invocation)이라
 * 프록시를 거치지 않아 트랜잭션이 걸리지 않는다. 예외가 나면 여기서 롤백되고 같은 예외가 라이브러리로 올라가
 * 재시도 · DLT 로 간다.
 */
abstract class idempotent_integration_event_handler<T : integration_event>(
    private val tenant_resolver: event_tenant_resolver,
) {

    protected val logger: Logger = LoggerFactory.getLogger(this::class.java)

    /** open — `@Transactional` 빈은 CGLIB 프록시 대상이라 final 메서드면 프록시가 가로채지 못한다. */
    @Transactional
    open fun handle(envelope: MessageEnvelope<out T>) {
        val event = envelope.payload
        val event_type_name = event::class.simpleName
        logger.info("통합 이벤트 수신: type={} messageId={}", event_type_name, envelope.messageId)
        try {
            val scope = tenant_resolver.resolve(envelope.tenantId, envelope.hospitalCode, envelope.messageId.toString())
            handle_internal(event, scope)
        } catch (ex: Exception) {
            logger.error("통합 이벤트 처리 실패: type={} messageId={} 사유={}", event_type_name, envelope.messageId, ex.message, ex)
            throw ex
        }
    }

    /** 하위 핸들러의 실제 처리 — `handle()` 의 트랜잭션 안에서 돈다. 여기에 `@Transactional` 을 붙이지 않는다. */
    protected abstract fun handle_internal(event: T, scope: tenant_scope)
}
```

### 6.5 구체 핸들러 + 매퍼 — `DietOrderCreated` — `eventhandling/diet_order_event_handlers.kt:28-103, 185-191, 214-241`

핸들러는 payload(x-ancillary-main 어휘) → `receive_diet_order_command` 로 **번역만** 하고 `command_bus` 로 넘긴다. 환자·수진 id 는 `event_identity_resolver` 가 shared uuid 로 정한다.

```kotlin
/**
 * 식이처방 인바운드 이벤트 핸들러 — 토픽 `{환경}.{테넌트id}.diet-order` 한 벌에서 생성·변경·취소가 온다.
 *
 * 페이로드 어휘는 **x-ancillary-main 발행 규칙**이다(`orderId` · `wardCode` …) — 로컬
 * 도메인 어휘(order_number · shared uuid)로의 번역은 **이 파일 한 곳**에서만 한다.
 * 환자 · 수진 id 는 shared uuid 로 정한다(`event_identity_resolver` — class-diagram §11.1 id 규칙).
 * 환자 이름 · 번호 · 생년 · 성별 · 진료과명 · 처방의명 · 수진 구분 · 입원일은 받지만 보관하지 않는다
 * (shared 에서 읽는다 — 수진 스냅샷을 만들지 않는다, class-diagram §3.9).
 * 멱등은 `order_number` 자연키 + `source_event_version` 이 담당한다(consumed_message 테이블 없음).
 */

/**
 * DietOrderCreated → 로컬 영속화.
 *
 * **금식(NPO)도 이 경로로 들어온다** — 금식은 별도 오더가 아니라 식이처방이다(class-diagram §3.1).
 * 식이처방 코드 마스터의 식종 분류가 NPO 면 저장 뒤 그 환자의 잔여 접수자료를 결식(NPO)으로 내린다.
 * 금식이 아닌 ACTIVE 처방은 저장과 같은 트랜잭션에서 세부식사가 자동 설정된다(수신 커맨드 핸들러).
 */
@Component
class diet_order_created_handler(
    private val command_bus: command_bus,
    private val diet_type_reader: i_diet_type_reader,
    private val not_served: not_served_applier,
    private val identities: event_identity_resolver,
    private val clock: Clock,
    tenant_resolver: event_tenant_resolver,
) : idempotent_integration_event_handler<diet_order_created_integration_event>(tenant_resolver) {

    override fun handle_internal(event: diet_order_created_integration_event, scope: tenant_scope) {
        val context = "order_id=${event.orderId}"
        val encounter_id = identities.required_encounter_id(event.encounterId, context)
        val patient_id = identities.resolve_patient_id(event.patientId, encounter_id, context)
        val doctor_id = identities.optional_uuid(event.doctorId, "doctorId", context)
        val result: diet_order_command_result = command_bus.send(
            receive_diet_order_command(
                scope = scope,
                order_number = event.orderId,
                patient_id = patient_id,
                encounter_id = encounter_id,
                department_id = identities.optional_uuid(event.departmentCode, "departmentCode", context),
                // 처방의 — 발행처가 안 실으면 수진의 진료의로 대체하지 않는다(대진·협진이면 틀린 값이 된다).
                orderer_id = doctor_id,
                // 병동 · 병실 · 병상 — 참고용 보관. 전동 토픽을 받지 않으므로 이후 갱신하지 않는다.
                ward_id = event.wardCode,
                room_id = event.wardRoomNo,
                bed_no = event.bedNo,
                diet_type_code = event.dietTypeCode,
                prescription = to_prescription(
                    scope = scope,
                    order_id = event.orderId,
                    diet_type_code = event.dietTypeCode,
                    diet_type_reader = diet_type_reader,
                    raw = diet_prescription(
                        meal_form_code = event.mealFormCode,
                        portion_code = event.portionCode,
                        salt_level_code = event.saltLevelCode,
                        is_isolation_meal = event.isolationMeal,
                        kcal = event.kcal,
                        volume_ml = event.volumeMl,
                        unit_qty = event.unitQty,
                    ),
                ),
                instruction_text = event.instructionText,
                status = parse_diet_order_status(event.status),
                // 처방일시 미탑재 시 수신 시각으로 둔다 — 마감 기준시각 계산이 null 을 못 받는다.
                ordered_at = event.orderedAt?.let { parse_instant(it) } ?: Instant.now(),
                source_event_version = event.sourceEventVersion,
            ),
        )
        logger.info(
            "식이처방 수신 반영: order_id={} diet_order_id={} 멱등={}",
            event.orderId, result.diet_order_id, result.idempotent,
        )
        apply_npo_if_fasting_diet(scope, event, patient_id, diet_type_reader, not_served, clock, logger)
    }
}
```

파서(매퍼) — `diet_order_event_handlers.kt:184-206`

```kotlin
/**
 * ISO-8601 문자열 → Instant.
 * 오프셋이 없는 `2026-08-06T16:12:00` 형태도 허용하고 KST 로 해석한다(발행처가 혼용해 보내온다 —
 * x-ancillary-main 은 영상 계약에 `ISO_LOCAL_DATE_TIME`, 과내기능검사 계약에 `Instant` 를 싣는다).
 */
internal fun parse_instant(value: String): Instant =
    runCatching { Instant.parse(value) }
        .getOrElse { LocalDateTime.parse(value).atZone(KST).toInstant() }

/** FHIR NutritionOrder.status(소문자 하이픈) → 도메인 열거형. */
internal fun parse_diet_order_status(value: String): diet_order_status =
    when (value.trim().lowercase()) {
        "draft" -> diet_order_status.DRAFT
        "active" -> diet_order_status.ACTIVE
        "on-hold", "on_hold", "onhold" -> diet_order_status.ON_HOLD
        "revoked", "cancelled" -> diet_order_status.REVOKED
        "completed" -> diet_order_status.COMPLETED
        else -> runCatching { diet_order_status.valueOf(value.trim().uppercase().replace('-', '_')) }
            .getOrElse {
                parser_logger.warn("처방상태 미해석 — active 로 처리: value={}", value)
                diet_order_status.ACTIVE
            }
    }
```

id 해석기 — `eventhandling/event_identity_resolver.kt:9-48`

```kotlin
/**
 * 수신 이벤트의 환자 · 수진 id 를 shared uuid 로 정한다(class-diagram §11.1 id 규칙).
 *
 * - 수진(`encounterId`)은 shared 수진 uuid 여야 한다 — 형식이 틀리면 **거절**(예외 → 재시도 → DLT).
 * - 환자(`patientId`)는 UUID 면 그 값, 아니면(발행처가 환자번호를 싣는 경우) `shared.encounter` 에서 그 수진의 환자 id 를
 *   찾는다. 그래도 못 정하면 **거절**한다.
 * - 진료과 · 처방의 같은 부가 id 는 UUID 가 아니면 경고만 남기고 null 로 적재한다(처방 자체는 받는다).
 *
 * 예외 메시지 · 로그에는 받은 값을 싣지 않는다 — 환자 식별자일 수 있다(필드 이름과 처방번호만 남긴다).
 */
@Component
class event_identity_resolver(
    private val encounters: i_shared_encounter_reader,
) {
    …
    fun required_encounter_id(value: String?, context: String): UUID =
        uuid_or_null(value) ?: run {
            logger.error("수신 거절 — 수진 id 가 shared 수진 uuid 가 아니다: {}", context)
            throw IllegalArgumentException("수신 payload id 형식 오류(UUID 아님): encounterId")
        }

    fun resolve_patient_id(value: String?, encounter_id: UUID?, context: String): UUID {
        uuid_or_null(value)?.let { return it }
        val from_shared = encounter_id?.let { encounters.patient_id_of(it) }
        if (from_shared != null) {
            logger.info("환자 id 를 shared 수진에서 찾았다(payload 값이 uuid 가 아님): {}", context)
            return from_shared
        }
        …
        throw IllegalArgumentException("수신 payload 환자 id 를 정하지 못했습니다: patientId")
    }
```

테넌트 해석기 — `eventhandling/event_tenant_resolver.kt:8-34`

```kotlin
/**
 * 수신 이벤트의 테넌트 범위를 정한다 — 봉투(MessageEnvelope)의 `tenantId` · `hospitalCode` 와 배포값을 맞춰 본다.
 *
 * 규칙(테넌트 · 병원 각각):
 * - 봉투 값이 배포값과 맞으면(대소문자 무시) 배포값을 쓴다 — 표기를 배포값 하나로 맞춘다.
 * - **다르면 경고 로그만 남기고 배포값으로 적재한다** — DLT 로 보내지 않는다. 토픽 자체가 병원별(`{환경}.{테넌트id}.…`)이라
 *   이 배포가 받은 이벤트는 이 배포의 것으로 본다.
 * - 봉투에 없으면 배포값이다(발행측이 JWT 없이 발행하면 봉투가 비어 온다).
 * - 배포값이 설정되지 않았으면 봉투 값을 쓴다. 둘 다 없으면 `INVALID_DATA`(tenant_scope.of).
 *
 * 병원은 봉투 값이 `HOSPITAL_CODES` 중 하나면 맞는 것으로 보고, 배포값으로 대신할 때는 첫 값을 쓴다.
 */
@Component
class event_tenant_resolver(
    private val deployment: i_deployment_tenant_port,
) {
    …
    fun resolve(envelope_tenant_id: String?, envelope_hospital_code: String?, message_id: String): tenant_scope {
        val tenant_id = resolve_tenant(envelope_tenant_id?.trim()?.takeIf { it.isNotEmpty() }, message_id)
        val hospital_code = resolve_hospital(envelope_hospital_code?.trim()?.takeIf { it.isNotEmpty() }, message_id)
        return tenant_scope.of(tenant_id, hospital_code)
    }

    /** 주체 없는 경로(배치 · 스케줄러)의 범위 — 배포 테넌트 + 첫 병원. 설정이 없으면 INVALID_DATA. */
    fun deployment_scope(): tenant_scope =
        tenant_scope.of(deployment.tenant_id(), deployment.hospital_codes().firstOrNull())
```

퇴원지시 핸들러 — `eventhandling/discharge_order_event_handlers.kt:20-45`

```kotlin
/** DischargeOrdered → 퇴원 예정일 기록 + 예정일 다음날부터 세부오더 종료 · 접수자료 취소(DISCHARGE). */
@Component
class discharge_ordered_handler(
    private val command_bus: command_bus,
    private val identities: event_identity_resolver,
    tenant_resolver: event_tenant_resolver,
) : idempotent_integration_event_handler<discharge_ordered_integration_event>(tenant_resolver) {

    override fun handle_internal(event: discharge_ordered_integration_event, scope: tenant_scope) {
        val context = "discharge_order_id=${event.dischargeOrderId}"
        val encounter_id = identities.required_encounter_id(event.encounterId, context)
        val planned_date = runCatching { LocalDate.parse(event.plannedDischargeDate.trim()) }.getOrElse {
            logger.error("수신 거절 — 퇴원 예정일 형식 오류(yyyy-MM-dd 아님): {}", context)
            throw IllegalArgumentException("수신 payload 형식 오류: plannedDischargeDate")
        }
        val result: diet_order_command_result = command_bus.send(
            receive_discharge_order_command(
                scope = scope,
                encounter_id = encounter_id,
                planned_discharge_date = planned_date,
                source_event_version = event.sourceEventVersion,
            ),
        )
        logger.info("퇴원지시 반영: {} 퇴원예정일={} 영향={}", context, planned_date, result.affected)
    }
}
```

금식(NPO) 결식 일괄 — `eventhandling/not_served_applier.kt:15-60`

```kotlin
/**
 * 결식 일괄 적용 — "환자의 활성 세부오더에 딸린 기간 × 끼니의 미종료 접수자료를 결식으로 내린다".
 *
 * 호출처는 금식 식이처방의 수신(`diet_order_event_handlers`)이다 — 금식은 별도 오더가 아니라 식이처방이다(class-diagram §3.1).
 * 화면 수동 결식과 같은 경로(커맨드)를 타도록 핸들러 밖에 둔다. 퇴원은 결식이 아니라 취소(DISCHARGE)라 이 경로를 쓰지 않는다.
 *
 * 대상 세부오더는 **수진 id 로 먼저** 찾고, 수진 id 가 없으면 환자 id 로 찾는다(둘 다 shared uuid — §11.1).
 */
@Component
class not_served_applier(
    private val command_bus: command_bus,
    private val meal_order_repository: i_meal_order_repository,
    private val preparation_repository: i_meal_preparation_repository,
) {
    …
    fun mark_period_not_served(
        scope: tenant_scope, patient_id: UUID?, encounter_id: UUID?,
        from_date: LocalDate, to_date: LocalDate, slots: List<String>,
        reason_code: not_served_reason_code, note: String?,
    ): Int {
        val meal_orders = when {
            encounter_id != null -> meal_order_repository.find_active_by_encounter_id(scope, encounter_id)
            patient_id != null -> meal_order_repository.find_active_by_patient_id(scope, patient_id)
            else -> emptyList()
        }
        …
        val targets = preparation_repository.find_open_by_meal_order_ids_in_period(
            scope = scope,
            meal_order_ids = meal_orders.map { it.meal_order_id },
            from_date = from_date,
            to_date = to_date,
            meal_type_codes = slots,
        )
```

### 6.6 발행 측 (전체)

발행 payload — `application/integrationevents/events/diet_integration_event.kt:13-60` (§6.3 베이스와 같은 애노테이션, `@get:JsonProperty` 로 `is*` 키 고정)

```kotlin
/**
 * 식이처방 코드 정의 — **식이오더코드 추가·수정 이벤트**(class-diagram §11.4). 토픽 `{namespace}.diet-definition`,
 * partitionKey `dietCode`.
 *
 * 급식이 식이처방 코드 마스터의 주인이라 이 페이로드가 공개 어휘(PL)다 — 수신 BC(처방, 임시 = x-ancillary-main)는
 * ACL 로 자기 카탈로그 어휘로 번역해 받는다. 바뀐 필드가 아니라 **그 식이처방의 현재 정의 전체**를 싣는다
 * (수신 측은 `dietCode` 로 덮어쓴다). 추가 · 수정 · 사용 중지가 전부 이 messageType 하나이고 [changeType] 이 가른다.
 * 식이 코드는 사용 중지분까지 전부 싣고, 조절 축은 싣지 않는다.
 *
 * **payload 필드는 camelCase** 다 — 프로퍼티명이 그대로 JSON 키다(다른 BC · 봉투와 같은 표기).
 * ⚠ `is` 로 시작하는 Boolean 은 직렬화기에 따라 키가 `active` 처럼 접두가 떨어진다 — wire 이름을 `@get:JsonProperty` 로 못박는다.
 * ⚠ messageType · 토픽 · 필드명은 수신 측과 맞춘 contract 다 — 바꾸면 수신이 조용히 멈춘다.
 */
@MessageType("DietIntegrationEvent")
@SchemaVersion("1")
@Aggregate(type = "Diet")
class diet_integration_event(
    /** CREATED(추가, C-70) / MODIFIED(수정, C-71 · C-72) — `diet_change_type_code`. */
    val changeType: String,
    /** 식이처방 식별자(uuid) — 참고용. 수신 측 키는 dietCode 다. */
    val dietId: String,
    /** 식이처방 코드 — partitionKey. `DietOrder.dietTypeCode` 가 이 값이다. */
    val dietCode: String,
    val dietName: String,
    /** 식종 분류 — GENERAL / THERAPEUTIC / TARGET / PRODUCT / NPO(meals 가 빈 목록). PRODUCT 면 처방에서 형태 · 염도를 고르지 않는다. */
    val dietCategoryCode: String,
    val sortOrder: Int,
    /** 사용 여부 — false 면 새 처방에서 고르지 않는다(행은 지우지 않는다). */
    @get:JsonProperty("isActive")
    val isActive: Boolean,
    /** 낙관적 잠금 버전 — 같은 dietCode 의 최신 판정(받은 값 이하면 버려도 된다). */
    val version: Long,
    /** 식이 코드 전부(사용 중지 포함), 정렬 순서대로. */
    val meals: List<diet_meal_payload> = emptyList(),
    /** 마지막 변경 시각(추가면 추가 시각) — ISO-8601 오프셋 포함(병원 시간대). */
    val modifiedAt: String,
) : integration_event() {
    …
    companion object {
        /** 토픽 `{환경}.{테넌트id}.diet-definition` 의 namespace 뒤 마디(이벤트명). */
        const val TOPIC = "diet-definition"

        /** [dietCode] 의 payload 필드명 — `topic_registry` 의 partitionKeyField. */
        const val PARTITION_KEY_FIELD = "dietCode"
```

발행기(AFTER_COMMIT) — `infrastructure/eventbus/domain_event_integration_publisher.kt:14-56` (전문)

```kotlin
/**
 * 도메인 이벤트 → 통합 이벤트 전환 · 발행기. (class-diagram §11)
 *
 * - 지금 보내는 것은 **식이처방 코드 정의(`DietIntegrationEvent` · §11.4) 하나**다 — route 도 그것만 등록돼 있다
 *   (`topic_registry`). 나머지 발행 이벤트는 토픽을 붙일 때 여기에 더한다.
 * - **트랜잭션 커밋 후(AFTER_COMMIT)** 받아, 도메인 이벤트가 실은 테넌트 범위(`scope`)로 그 식이처방을 다시 읽어
 *   현재 정의 전체를 싣는다 — 한 저장이 여러 번 바꿔도 마지막 상태 1건이 나간다.
 * - 봉투의 `userId` 는 발행 순간 MDC 의 로그인 사용자다(HTTP 요청 = 토큰). 없으면 대체값을 넣지 않고 WARN 으로 드러낸다.
 * - 발행 · 조립 실패가 **커밋된 쓰기를 되돌리지 않도록** 예외는 로그만 남긴다(best-effort · outbox 없음 · 다시 보내지 않는다).
 */
@Component
class domain_event_integration_publisher(
    private val event_bus: EventBus,
    private val master_queries: i_foodservice_master_queries,
) {

    private val log = LoggerFactory.getLogger(domain_event_integration_publisher::class.java)

    // ----- 기준 코드 마스터 -----

    /** 식이처방 추가 · 수정(C-70 · C-71 · C-72) — 추가 · 수정 · 사용 중지 모두 이 한 messageType 이고 changeType 이 가른다. */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun on_diet_changed(event: diet_changed_domain_event) {
        runCatching {
            val view = master_queries.find_diet(event.scope, event.diet_id)
                ?: return log.warn(
                    "발행할 식이처방을 찾지 못했습니다 — diet_id={} diet_code={} change_type={}",
                    event.diet_id, event.diet_code, event.change_type,
                )
            val payload = diet_integration_event.of(event.change_type, view, event.occurred_at)
            if (MDC.get(JwtKeys.USER_ID).isNullOrBlank()) {
                log.warn("발행 봉투에 로그인 사용자 id 가 없습니다 — messageType=DietIntegrationEvent dietCode={}", payload.dietCode)
            }
            event_bus.publish(payload, aggregateId = payload.dietId)
            log.info("식이처방 코드 정의를 발행했습니다 — {}", payload)
        }.onFailure {
            log.error(
                "식이처방 코드 정의 발행에 실패했습니다(저장은 됐다 · 다시 보내지 않는다) — diet_id={} diet_code={} change_type={} 사유={}",
                event.diet_id, event.diet_code, event.change_type, it.message, it,
            )
        }
    }
}
```

아웃바운드 payload 카탈로그(정의만 있고 **발행 route 미등록**) — `events/outbound_integration_events.kt:8-48` + `@MessageType` 목록: `MealOrderIssued`(:25) · `MealOrderRevised`(:51) · `MealOrderRevoked`(:76) · `DischargeMealSet`(:90) · `MealPreparationClosed`(:110) · `MealPreparationReopened`(:125) · `MealChangeRequested`(:139) · `MealChangeConfirmed`(:153) · `MealChangeApplied`(:166) · `MealChangeRejected`(:179) · `MealNotServed`(:190) · `MealNotServedCancelled`(:205) · `MealPreparationStarted`(:225) · `MealServed`(:261). 전부 `@SchemaVersion("1")`.

```kotlin
/**
 * 아웃바운드 통합 이벤트. (class-diagram.md §11 Outbound / api-spec §6)
 *
 * `@TransactionalEventListener(AFTER_COMMIT)` 에서 도메인이벤트를 받아 구성·발행한다.
 * partitionKey 는 `patientId` — 동일 환자 이벤트의 처리 순서를 보장한다.
 * 마감/해제만 배치 단위라 partitionKey 를 `mealDate` 로 둔다(환자가 여러 명).
 * …
 * ⚠ 이벤트명·필드명을 바꾸면 상대 BC 와 lockstep 이 필요하다(한쪽만 바꾸면 조용히 미수신).
 */

/** 세부오더 발행 — 내부 배치(접수자료 생성), 병동 화면. */
@MessageType("MealOrderIssued")
@SchemaVersion("1")
@Aggregate(type = "MealOrder")
class meal_order_issued_integration_event(
    val patientId: String,
    val dietOrderId: String,
    val mealOrderId: String,
    val mealTypeCode: String,
    val dietTypeCode: String,
    …
) : integration_event()
```

---

## 7. 예외 → HTTP

### 7.1 `@RestControllerAdvice` — `infrastructure/config/global_exception_handler.kt:19-127`

```kotlin
/**
 * 표준 에러 응답.
 *
 * api-spec §1 은 `{ code, message, traceId }` 를 규정한다 — 여기서는 그 상위집합으로
 * `error`(카테고리) 와 `context`(대안 경로 안내 등)를 함께 내려준다.
 * `context.hint` 는 마감 후 수정 시도(FS-4091)에서 변경요청 API 를 안내하는 자리다.
 * 내부 stacktrace · PII 는 노출하지 않는다.
 */
data class error_response(
    val error: String,
    val code: String,
    val message: String,
    val trace_id: String?,
    val context: Map<String, Any?> = emptyMap(),
)

/**
 * 전역 예외 → HTTP 매핑.
 * - `*_not_found_exception`      → 404 NotFound             (FS-4041)
 * - validation / IllegalArgument → 400 Validation           (FS-4001)
 * - `*_domain_exception`(불변식) → **422** DomainRuleViolation (FS-4092)
 * - `*_conflict_exception`       → 409 Conflict             (FS-4091 / FS-4093)
 * - `*_forbidden_exception`      → 403 Forbidden            (FS-4031)
 */
@RestControllerAdvice
class global_exception_handler {

    /**
     * 401 · 403 — 인증 실패(@RequireAuth · @TokenPrincipal). 상태 · 코드는 라이브러리 에러코드가 정한다.
     * `code` 에 wire 코드(AUTH_TOKEN_MISSING · AUTH_TOKEN_EXPIRED …)를 실어 화면이 `AUTH_` 접두로 분기할 수 있게 한다.
     */
    @ExceptionHandler(JwtAuthenticationException::class)
    fun handle_authentication(ex: JwtAuthenticationException): ResponseEntity<error_response> =
        ResponseEntity.status(ex.errorCode.status)
            .body(error_response("Unauthorized", ex.errorCode.name, ex.errorCode.defaultMessage, trace_id()))

    @ExceptionHandler(foodservice_not_found_exception::class)
    fun handle_not_found(ex: foodservice_not_found_exception): ResponseEntity<error_response> =
        ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(error_response("NotFound", ex.code, ex.message ?: "리소스를 찾을 수 없습니다.", trace_id(), ex.context))

    @ExceptionHandler(foodservice_domain_exception::class)
    fun handle_domain_rule(ex: foodservice_domain_exception): ResponseEntity<error_response> =
        ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
            .body(error_response("DomainRuleViolation", ex.code, ex.message ?: "도메인 규칙을 위반했습니다.", trace_id(), ex.context))

    @ExceptionHandler(foodservice_conflict_exception::class)
    fun handle_domain_conflict(ex: foodservice_conflict_exception): ResponseEntity<error_response> =
        ResponseEntity.status(HttpStatus.CONFLICT)
            .body(error_response("Conflict", ex.code, ex.message ?: "리소스 충돌이 발생했습니다.", trace_id(), ex.context))

    @ExceptionHandler(foodservice_forbidden_exception::class)
    fun handle_forbidden(ex: foodservice_forbidden_exception): ResponseEntity<error_response> =
        ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(error_response("Forbidden", ex.code, ex.message ?: "권한이 없습니다.", trace_id(), ex.context))

    @ExceptionHandler(OptimisticLockingFailureException::class)
    fun handle_optimistic_lock(ex: OptimisticLockingFailureException): ResponseEntity<error_response> =
        ResponseEntity.status(HttpStatus.CONFLICT)
            .body(error_response("Conflict", "FS-4094", "다른 사용자가 먼저 변경했습니다. 다시 조회 후 시도하세요.", trace_id()))

    /**
     * 중복 키 위반 — 애플리케이션 선검사를 우회한 동시 요청의 2차 방어.
     * 유니크 인덱스명을 도메인 에러 코드로 번역한다.
     */
    @ExceptionHandler(DataIntegrityViolationException::class)
    fun handle_integrity(ex: DataIntegrityViolationException): ResponseEntity<error_response> {
        val detail = ex.mostSpecificCause.message.orEmpty()
        return when {
            detail.contains("uk_meal_preparation_meal_order_id_meal_date_meal_type_code") -> conflict("FS-4093", "이미 생성된 접수자료입니다.")
            detail.contains("uk_meal_order_diet_order_id_meal_type_code") -> conflict("FS-4093", "해당 처방의 같은 끼니에 이미 활성 세부오더가 있습니다.")
            detail.contains("uk_diet_order_tenant_id_hospital_code_order_number") -> conflict("FS-4093", "이미 수신된 처방번호입니다.")
            detail.contains("uk_diet_tenant_id_hospital_code_diet_code") -> conflict("FS-4093", "이미 있는 식이처방 코드입니다.")
            detail.contains("uk_meal_diet_id_meal_code") -> conflict("FS-4093", "같은 식이처방에 이미 있는 식이 코드입니다.")
            else -> conflict("FS-4093", "데이터 제약 조건을 위반했습니다.")
        }
    }

    @ExceptionHandler(foodservice_validation_exception::class)
    fun handle_validation(ex: foodservice_validation_exception): ResponseEntity<error_response> =
        ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(error_response("Validation", ex.code, ex.message ?: "요청 값이 유효하지 않습니다.", trace_id(), ex.context))

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handle_bean_validation(ex: MethodArgumentNotValidException): ResponseEntity<error_response> {
        val fields = ex.bindingResult.fieldErrors.associate { it.field to (it.defaultMessage ?: "유효하지 않은 값") }
        val message = fields.entries.joinToString(", ") { "${it.key}: ${it.value}" }
            .ifBlank { "요청 값이 유효하지 않습니다." }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(error_response("Validation", "FS-4001", message, trace_id(), mapOf("fields" to fields)))
    }

    /** 경로 · 쿼리 변수 형식 오류(예: id 가 UUID 가 아님) — 400 FS-4001. 받은 값은 싣지 않는다(식별자일 수 있다). */
    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
    fun handle_type_mismatch(ex: MethodArgumentTypeMismatchException): ResponseEntity<error_response> =
        ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(error_response("Validation", "FS-4001", "요청 값의 형식이 올바르지 않습니다: ${ex.name}", trace_id(), mapOf("field" to ex.name)))

    @ExceptionHandler(IllegalArgumentException::class)
    fun handle_illegal_argument(ex: IllegalArgumentException): ResponseEntity<error_response> =
        ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(error_response("Validation", "FS-4001", ex.message ?: "요청 값이 유효하지 않습니다.", trace_id()))

    private fun conflict(code: String, message: String): ResponseEntity<error_response> =
        ResponseEntity.status(HttpStatus.CONFLICT).body(error_response("Conflict", code, message, trace_id()))

    /** trace_id 는 게이트웨이/observability 가 MDC 에 넣어 준다. */
    private fun trace_id(): String? = MDC.get("traceId") ?: MDC.get("trace_id")
}
```

### 7.2 도메인 예외 체계(FS-40xx) — `domain/exceptions/foodservice_exceptions.kt:5-59`

```kotlin
/**
 * 급식 BC 예외 체계 — `_docs/design/foodservice-api-spec.md` §1 의 에러 코드(FS-40xx)와 1:1 이다.
 *
 * | 예외 베이스                       | HTTP | error               | 대표 code |
 * |-----------------------------------|------|---------------------|-----------|
 * | `foodservice_not_found_exception` | 404  | NotFound            | FS-4041   |
 * | `foodservice_validation_exception`| 400  | Validation          | FS-4001   |
 * | `foodservice_domain_exception`    | 422  | DomainRuleViolation | FS-4092   |
 * | `foodservice_conflict_exception`  | 409  | Conflict            | FS-4091 / FS-4093 / FS-4094 |
 * | (validation)                      | 400  | Validation          | FS-4002 / FS-4003 / FS-4005 / FS-4006 |
 * | (domain)                          | 422  | DomainRuleViolation | FS-4004 / FS-4007 / FS-4008 |
 * | `foodservice_forbidden_exception` | 403  | Forbidden           | FS-4031   |
 *
 * `context` 는 화면이 사유를 설명하고 대안 경로(변경요청 API 등)를 안내할 수 있도록 담는 부가 정보다 —
 * PII / 내부 스택은 담지 않는다.
 */

/** 불변식 위반 (HTTP 422 / DomainRuleViolation). */
abstract class foodservice_domain_exception(
    val code: String,
    message: String,
    val context: Map<String, Any?> = emptyMap(),
) : RuntimeException(message)

/** 리소스 없음 (HTTP 404 / NotFound). */
class foodservice_not_found_exception(
    val code: String,
    message: String,
    val context: Map<String, Any?> = emptyMap(),
) : RuntimeException(message)

/** 동시성 / 중복 키 / 마감 후 직접 수정 (HTTP 409 / Conflict). */
class foodservice_conflict_exception(
    val code: String,
    message: String,
    val context: Map<String, Any?> = emptyMap(),
) : RuntimeException(message)

/**
 * 입력 검증 실패 (HTTP 400 / Validation).
 * 기본 코드는 FS-4001 이고, 화면이 따로 다뤄야 하는 검증만 자기 코드를 갖는다(예: FS-4002 과거 적용일).
 */
class foodservice_validation_exception(
    message: String,
    val context: Map<String, Any?> = emptyMap(),
    val code: String = "FS-4001",
) : RuntimeException(message)

/** 역할 권한 없음 (HTTP 403 / Forbidden) — FS-4031. */
class foodservice_forbidden_exception(
    message: String,
    val context: Map<String, Any?> = emptyMap(),
) : RuntimeException(message) {
    val code: String = "FS-4031"
}
```

예외는 **클래스가 아니라 팩토리 함수**가 대부분이다(`foodservice_not_found_exception` 등 4개 베이스 클래스 + 팩토리 함수). 404 — `foodservice_exceptions.kt:65-70`

```kotlin
fun diet_order_not_found_exception(diet_order_id: UUID) =
    foodservice_not_found_exception(
        code = "FS-4041",
        message = "식이처방을 찾을 수 없습니다.",
        context = mapOf("diet_order_id" to diet_order_id),
    )
```

409 마감 후 직접 수정(변경요청 경로 hint 포함) — `foodservice_exceptions.kt:152-168`

```kotlin
/**
 * 마감된 끼니에 대한 직접 수정 시도 — FS-4091.
 * 화면이 변경요청 경로로 유도할 수 있도록 `context.hint` 에 대체 API 를 담는다(api-spec §5).
 */
fun meal_closed_conflict_exception(
    meal_date: String,
    meal_type_code: String,
    context_extra: Map<String, Any?> = emptyMap(),
) = foodservice_conflict_exception(
    code = "FS-4091",
    message = "$meal_date $meal_type_code 식사는 마감되어 직접 수정할 수 없습니다. 변경요청을 등록하세요.",
    context = mapOf(
        "meal_date" to meal_date,
        "meal_type_code" to meal_type_code,
        "hint" to mapOf("change_request_api" to "POST /meal-change-requests"),
    ) + context_extra,
)
```

409 낙관락(수동 버전 비교) — `foodservice_exceptions.kt:201-210` / 422 상태전이 — `:216-225`

```kotlin
fun stale_version_conflict_exception(resource: String, id: Any, expected: Long, actual: Long) =
    foodservice_conflict_exception(
        code = "FS-4094",
        message = "다른 사용자가 먼저 변경했습니다. 다시 조회 후 시도하세요.",
        context = mapOf("resource" to resource, "id" to id, "expected_version" to expected, "current_version" to actual),
    )
…
/** 허용되지 않은 상태 전이 — FS-4092. */
class bad_state_domain_exception(
    message: String,
    current_status: String,
    context_extra: Map<String, Any?> = emptyMap(),
) : foodservice_domain_exception(
    code = "FS-4092",
    message = message,
    context = mapOf("current_status" to current_status) + context_extra,
)
```

FS 코드 전체 목록(파일 내 등장 줄): FS-4001(기본 검증) · FS-4002(과거 적용일 :101-109) · FS-4003(식이 코드 소속 :112-120) · FS-4005(축 범위 :123-134) · FS-4006(끼니 설정 :138-145) · FS-4091(마감 후 수정 :153-161) · FS-4093(중복 :170-196) · FS-4094(버전 :202-207) · FS-4092(상태전이 계열 :216-293, 350) · FS-4004(식종 분류 :298-312) · FS-4007(마감 차수 :318-334) · FS-4008(끼니 사용 중지 :339-342) · FS-4031(403) · FS-4041(404).

---

## 8. 설정 · 부팅

### 8.0 스키마명 — 실제 값 확인

> 요청 가정("스키마명이 모듈명과 다른 `nutrition`")과 **소스가 다르다.** 현재 소스는 전부 **`${DB_SCHEMA:food_service}`** 다.
> - `application.yml:1-4` 주석: "MS 전용 스키마 `food_service` … (구 `nutrition` 스키마는 BC 단위였다 — 두 MS 가 공유하면 DATABASECHANGELOG·Liquibase LOCK 이 얽힌다.)"
> - `application.yml:32` `connection-init-sql: SET search_path TO ${DB_SCHEMA:food_service}` · `:56-57` liquibase `default-schema` / `liquibase-schema` 동일 변수.
> - `db_schema_initializer.kt:32` `@Value("\${DB_SCHEMA:food_service}")`.
> - `nutrition` 은 **gradle rootProject 이름**(`backend/settings.gradle.kts:10 rootProject.name = "nutrition"`)과 OpenAPI 설명(`foodservice_configs.kt:56`)에만 남아 있다.
> - 모듈명 `foodservice` ↔ 스키마 `food_service` (언더스코어 유무) — CLAUDE.md 규칙 "스키마명 = 모듈명 snake_case" 와는 `foodservice` 가 아니라 `food_service` 로 다르다. `shared_patient.kt:16` 주석도 `food_service`.
> - 실제 로컬/dev 환경값은 `backend/.env`(gitignore) · 배포 `values.yaml` 의 `DB_SCHEMA` 가 덮어쓸 수 있으므로, 런타임 스키마는 **기동 로그 `DB 스키마 확인/생성 완료 — db=…, schema=…`**(`db_schema_initializer.kt:55-59`)로 확인한다.

### 8.1 메인 클래스 — `src/main/kotlin/com/projectx/foodservice/foodservice_application.kt:1-20`

```kotlin
/**
 * Foodservice 서비스 부트스트랩.
 * - 앱 클래스가 루트 패키지(com.projectx.foodservice)에 있어 component / entity 스캔이
 *   presentation · application · domain · infrastructure 전체를 커버한다.
 * - JPA Repository(write) 는 infrastructure 패키지에서 스캔한다.
 * - 조회(read)는 QueryDSL(JPAQueryFactory)로 view_model 을 projection 한다(MyBatis 미사용).
 */
@SpringBootApplication
@EnableJpaRepositories(basePackages = ["com.projectx.foodservice.infrastructure"])
class foodservice_application

fun main(args: Array<String>) {
    runApplication<foodservice_application>(*args)
}
```

### 8.2 `application.yml` — DB · search_path · liquibase — `src/main/resources/application.yml:1-64`

```yaml
# 기본 프로파일: 로컬 PostgreSQL (`sup` DB / **MS 전용 스키마 `food_service`**).
# 스키마는 MicroService 단위다(루트 CLAUDE.md §0 DB 스키마) — 이 MS(foodservice)는 `food_service` 만 본다.
# ⚠️ BC 스키마가 아니다 — 이 레포에 nutrition_consult MS 가 추가되면 그 MS 는 **자기 스키마**를 따로 갖는다.
#    (구 `nutrition` 스키마는 BC 단위였다 — 두 MS 가 공유하면 DATABASECHANGELOG·Liquibase LOCK 이 얽힌다.)
# 로컬 접속값(호스트·DB·계정/비번)은 **`backend/.env`** 로 준다 — `.env.example` 복사 후 값 채움(spring-dotenv).
# 스키마는 **앱이 부팅하며 직접 만든다**(infrastructure/config/db_schema_initializer, 멱등 CREATE SCHEMA).
# 그 안의 테이블/시드는 Liquibase 가 적용하고 Hibernate 는 검증만 한다(ddl-auto=validate).
server:
  port: ${SERVER_PORT:5343}
  # ⚠️ context-path 를 두지 않는다(= "/"). API prefix `/api/v1/foodservice` 는 **컨트롤러가 소유**한다
  #    — BC 표준(정본 x-diagnostic-imaging). 그래야 문서가 prefix 밖의 `/spec/foodservice/**` 로 노출되고
  #    actuator 도 `/actuator/health/**`(루트) 로 남아 k8s probe 경로가 단순해진다.

spring:
  # 공통 라이브러리 default 는 common-libs/*.yaml 로 분리 — classpath 에 포장되어 부팅 시 자동 흡수.
  config:
    import:
      - classpath:common-libs/common-masking.yaml
      - classpath:common-libs/common-observability.yaml
  application:
    name: foodservice
  # **계정/비밀번호는 기본값을 두지 않는다** — 자격증명을 소스에 남기지 않고, 누락 시 부팅이 즉시 실패해
  #   ".env 를 안 채웠다" 가 바로 드러난다. 로컬은 `backend/.env`, k8s 는 configData + vault Secret.
  datasource:
    url: jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5432}/${DB_NAME:sup}
    username: ${DB_USER}
    password: ${DB_PASSWORD}
    driver-class-name: org.postgresql.Driver
    hikari:
      # 커넥션 오픈 시마다 실행된다 — search_path 고정만 한다(이 MS 는 자기 스키마만 본다).
      # ⚠️ 여기에 CREATE SCHEMA 를 넣지 않는다(커넥션마다 돌아 pg_namespace 충돌) — 생성은 부팅 시 1회.
      connection-init-sql: SET search_path TO ${DB_SCHEMA:food_service}
  jpa:
    hibernate:
      # DDL 은 Liquibase 가 부팅 시 적용한다. Hibernate 는 검증만.
      ddl-auto: validate
    open-in-view: false
    # 감사 컬럼 리스너(audit_entity_listener)를 기본 엔티티 리스너로 등록하는 orm.xml.
    #   ⚠️ 빠지면 감사 컬럼이 EPOCH · null 로 저장된다(부팅 · 빌드 모두 통과한다).
    mapping-resources:
      - META-INF/orm.xml
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
        format_sql: true
        # 하위 컬렉션(세부오더 리비전 · 마감 이력)을 행마다 따로 읽지 않게 묶어 온다 —
        # 병동 보드는 한 화면에서 수십 건의 세부오더를 펼치므로 N+1 이 그대로 지연이 된다.
        default_batch_fetch_size: 100

  # Liquibase — 스키마 마이그레이션(부팅 시 자동 적용).
  #   contexts=seed 면 마스터 샘플 데이터까지 적재(로컬/dev). 운영은 LIQUIBASE_CONTEXTS 를 빈 값으로 오버라이드.
  liquibase:
    change-log: classpath:db/changelog/db.changelog-master.yaml
    # 스키마명은 이 한 값(DB_SCHEMA)에서 나온다 — hikari search_path 와 같은 출처.
    #   하드코딩하면 배포 values 의 configData.DB_SCHEMA 가 조용히 무시된다.
    default-schema: ${DB_SCHEMA:food_service}
    liquibase-schema: ${DB_SCHEMA:food_service}
    contexts: ${LIQUIBASE_CONTEXTS:seed}
    # 시드 · 운영 기본 마스터(002 · 900)의 테넌트 값 — 앱이 읽는 배포 테넌트와 같은 출처다.
    #   HOSPITAL_CODES 가 여러 병원(콤마 구분)이면 SEED_HOSPITAL_CODE 로 적재할 병원 하나를 따로 준다.
    #   값이 없으면 INVALID_DATA 로 적재된다 — 그 행은 실제 토큰 범위(테넌트 · 병원)의 조회에 나오지 않는다.
    parameters:
      seed_tenant_id: ${TENANT_ID:INVALID_DATA}
      seed_hospital_code: ${SEED_HOSPITAL_CODE:${HOSPITAL_CODES:INVALID_DATA}}
```

(eventbus 부분은 §6.2. JWT · 테넌트 · springdoc 은 `application.yml:102-146`.) `application.yml:105-121`:

```yaml
jwt:
  public-key: ${X_JWT_PUBLIC_KEY}
  claim-aes-secret-key: ${X_JWT_CLAIM_AES_SECRET_KEY}

projectx:
  foodservice:
    tenant:
      tenant-id: ${TENANT_ID:INVALID_DATA}
      hospital-codes: ${HOSPITAL_CODES:INVALID_DATA}
    # 🚩 논의사항(api.md §7) — 변경요청 confirm 즉시 apply 여부.
    #   true(기본) = confirm 트랜잭션에서 곧바로 스냅샷 반영까지 수행(C-52 는 멱등 no-op).
    #   false      = confirm 은 상태만 바꾸고 C-52 apply 를 별도 호출/배치가 수행.
    confirm-applies-immediately: ${FOODSERVICE_CONFIRM_APPLIES:true}
```

### 8.3 `application-dev.yml` — `src/main/resources/application-dev.yml:1-33, 45-48`

dev 는 **상속에 기대지 않고 같은 값을 전부 다시 명시**한다(메모리 규칙 "프로파일 yml 상속 금지").

```yaml
# dev 프로파일 — k8s(core 네임스페이스) 배포 전용 오버라이드.
spring:
  datasource:
    # configData(DB_HOST/DB_PORT/DB_NAME/DB_SCHEMA) + vault Secret(DB_USER/DB_PASSWORD) 주입.
    # 카탈로그명은 로컬·dev 모두 `sup`, 스키마는 MS 단위(= food_service).
    url: jdbc:postgresql://${DB_HOST}:${DB_PORT}/${DB_NAME:sup}
    username: ${DB_USER}
    password: ${DB_PASSWORD}
    driver-class-name: org.postgresql.Driver
    hikari:
      connection-init-sql: SET search_path TO ${DB_SCHEMA:food_service}
  jpa:
    hibernate:
      ddl-auto: validate
    open-in-view: false
    mapping-resources:
      - META-INF/orm.xml
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
        default_batch_fetch_size: 100
  liquibase:
    contexts: ${LIQUIBASE_CONTEXTS:seed}
    parameters:
      seed_tenant_id: ${TENANT_ID:INVALID_DATA}
      seed_hospital_code: ${SEED_HOSPITAL_CODE:${HOSPITAL_CODES:INVALID_DATA}}
…
kafka_namespace: ${KAFKA_NAMESPACE:dev.chabd}
```

### 8.4 `db_schema_initializer` — `infrastructure/config/db_schema_initializer.kt:11-75`

```kotlin
/**
 * 이 MS 전용 DB 스키마를 **앱이 부팅하며 직접 생성**한다(멱등 `CREATE SCHEMA IF NOT EXISTS`).
 *
 * 스키마는 MicroService 단위다(루트 CLAUDE.md §0 DB 스키마) — 이 MS(foodservice)는 `food_service` 하나만 본다.
 * 같은 레포에 들어올 nutrition_consult MS 는 자기 스키마를 따로 갖는다(BC 단위로 공유하지 않는다).
 * 스키마명은 `DB_SCHEMA` 로 오버라이드한다(로컬은 `backend/.env`, k8s 는 values 의 configData).
 *
 * 왜 BeanPostProcessor 인가: **Liquibase 보다 먼저** 돌아야 한다. Liquibase 는 `default-schema` 가
 * 없으면 changelog 적용에 실패하므로, `SpringLiquibase` 빈이 초기화되기 직전에 스키마를 만든다.
 * 빈 이름에 의존하는 `@DependsOn("liquibase")` 대신 **타입**으로 잡아 Boot 버전 변화에 흔들리지 않게 한다.
 *
 * - `DataSource` 는 `ObjectProvider` 로 늦게 받는다 — BeanPostProcessor 가 생성자에서 직접 주입받으면
 *   그 DataSource 자신이 후처리 대상에서 빠진다(Spring 경고 `is not eligible for getting processed by all BPPs`).
 * - hikari `connection-init-sql`(`SET search_path`) 에는 CREATE SCHEMA 를 넣지 않는다 — 커넥션마다 돌아
 *   동시 오픈 시 `pg_namespace` 유니크 충돌로 실패할 수 있다. 여기서 **부팅 시 1회**만 만든다.
 * - 계정에 CREATE 권한이 없으면 경고만 남기고 넘어간다 — 스키마가 이미 있는 정상 케이스를 막지 않기 위함이며,
 *   실제로 없으면 바로 뒤의 Liquibase 가 명확한 오류로 기동을 세운다.
 */
@Configuration
class db_schema_initializer(
    private val data_source_provider: ObjectProvider<DataSource>,
    @Value("\${DB_SCHEMA:food_service}") private val schema_name: String,
) : BeanPostProcessor {

    private val logger = LoggerFactory.getLogger(db_schema_initializer::class.java)

    override fun postProcessBeforeInitialization(bean: Any, bean_name: String): Any {
        if (bean is SpringLiquibase) {
            create_schema_if_absent()
        }
        return bean
    }

    private fun create_schema_if_absent() {
        // 스키마명은 식별자라 파라미터 바인딩이 불가능하다 — 화이트리스트로 검증해 SQL 주입을 막는다.
        require(schema_name.matches(IDENTIFIER)) {
            "DB_SCHEMA 값이 스키마명 규칙에 맞지 않는다: '$schema_name' (소문자·숫자·밑줄, 63자 이내)"
        }
        try {
            data_source_provider.getObject().connection.use { connection ->
                connection.createStatement().use { statement ->
                    statement.execute("CREATE SCHEMA IF NOT EXISTS $schema_name")
                }
                // 어느 DB(카탈로그)에 만들었는지까지 남긴다 — DB_NAME 오설정을 로그 한 줄로 잡기 위함.
                logger.info(
                    "DB 스키마 확인/생성 완료 — db={}, schema={}",
                    connection.catalog,
                    schema_name,
                )
            }
        } catch (exception: Exception) {
            logger.warn(
                "DB 스키마 생성 실패 — schema={}, 원인={}. 계정에 CREATE 권한이 있는지 확인하라"
                    + "(DBA 선행 생성 경로: _docs/db-bootstrap/bootstrap-account-schema.simple.sql)",
                schema_name,
                exception.message,
            )
        }
    }

    private companion object {
        /** PostgreSQL 비인용 식별자 규칙 — 소문자/밑줄로 시작, 소문자·숫자·밑줄, 63자 이내. */
        private val IDENTIFIER = Regex("^[a-z_][a-z0-9_]{0,62}$")
    }
}
```

### 8.5 기타 부팅 배선

`.env` 로더 — `infrastructure/config/dotenv_environment_post_processor.kt:8-25` + `META-INF/spring.factories:1-4`

```kotlin
/**
 * `.env` 로더 연결 (spring-dotenv).
 * …
 * - spring-dotenv 4.0.0 의 자동 등록(SpringApplicationRunListener)은 Boot 4 에서 동작하지 않아
 *   (RunListener 시그니처 변경) `.env` 가 로드되지 않는다. 로더만 재사용하고 등록은 Boot 4 의
 *   `EnvironmentPostProcessor` 계약으로 직접 한다(META-INF/spring.factories).
 * …
 */
class dotenv_environment_post_processor : EnvironmentPostProcessor {
    override fun postProcessEnvironment(environment: ConfigurableEnvironment, application: SpringApplication) {
        DotenvPropertySource.addToEnvironment(environment)
    }
}
```

```properties
org.springframework.boot.EnvironmentPostProcessor=\
  com.projectx.foodservice.infrastructure.config.dotenv_environment_post_processor
```

QueryDSL · Clock(KST) · 도메인 서비스 · 시큐리티 — `infrastructure/config/foodservice_configs.kt:20-45, 61-83`

```kotlin
/** QueryDSL 배선 — 조회는 JPAQueryFactory 타입세이프 쿼리로 view_model 을 projection 한다. */
@Configuration
class querydsl_config {

    @Bean
    fun jpa_query_factory(entity_manager: EntityManager): JPAQueryFactory = JPAQueryFactory(entity_manager)
}

/**
 * 도메인 서비스 빈 등록.
 * domain 패키지는 Spring 의존이 금지(ArchUnit 강제)라 `@Component` 를 붙일 수 없다 —
 * 여기서 팩토리 메서드로 등록한다.
 */
@Configuration
class domain_service_config {

    /** 급식 운영은 전부 KST 기준(식사일자 · 마감 기준시각)이라 시스템 존을 고정한다. */
    @Bean
    fun clock(): Clock = Clock.system(KST)

    // ⚠ 빈 메서드명을 타입명과 같게 두면 본문의 생성자 호출이 자기 자신(빈 메서드)으로 해석돼
    //    StackOverflowError 가 난다 — 접두를 붙여 구분한다.
    @Bean
    fun foodservice_closing_deadline_policy(): meal_closing_deadline_policy =
        default_meal_closing_deadline_policy()
}
…
/**
 * 인증 — common-jwt-kt 소비자.
 * - 필터 체인은 토큰을 **해석만** 한다(JwtAuthenticationFilter → SecurityContext · MDC userId/tenantId/hospitalCode).
 *   막는 일은 컨트롤러의 @RequireAuth(aspect)가 한다 — 토큰이 없거나 틀리면 401, global_exception_handler 가 응답을 만든다.
 *   그래서 체인은 permitAll 이다(actuator probe · swagger 는 토큰 없이 열려 있어야 한다).
 * - ⚠️ 이 빈을 직접 정의하면 라이브러리 기본 체인이 꺼진다 — JwtAuthenticationFilter 를 여기서 꼭 등록한다.
 *   빠지면 @RequireAuth 가 전부 401 이 된다.
 * - 역할 인가(NURSE / DIETITIAN / KITCHEN / ADMIN)는 아직 없다 — FS-4031 은 예외 체계만 준비돼 있다.
 */
@Configuration
@EnableWebSecurity
class security_config {

    @Bean
    fun filter_chain(http: HttpSecurity, jwt_authentication_filter: JwtAuthenticationFilter): SecurityFilterChain {
        http
            .csrf { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .addFilterBefore(jwt_authentication_filter, UsernamePasswordAuthenticationFilter::class.java)
            .authorizeHttpRequests { auth -> auth.anyRequest().permitAll() }
        return http.build()
    }
}
```

배포 테넌트 — `infrastructure/config/deployment_tenant_adapter.kt:10-34`

```kotlin
/**
 * 배포 테넌트 · 병원 — `projectx.foodservice.tenant.*` (k8s 는 ConfigMap x-ehr-tenant-config 의 TENANT_ID · HOSPITAL_CODES).
 * 값이 없거나 INVALID_DATA 면 「설정 안 됨」으로 본다 — 수신 이벤트는 봉투 값, 그것도 없으면 INVALID_DATA 로 적재된다.
 * 부팅을 막지 않는다(로컬 · 테스트는 값 없이 뜬다). 대신 기동 로그에 무엇으로 정해졌는지 남긴다.
 */
@Component
class deployment_tenant_adapter(
    @Value("\${projectx.foodservice.tenant.tenant-id:}") private val raw_tenant_id: String,
    @Value("\${projectx.foodservice.tenant.hospital-codes:}") private val raw_hospital_codes: String,
) : i_deployment_tenant_port {
    …
    private val tenant_id: String? = raw_tenant_id.trim().takeIf { it.isNotEmpty() && it != tenant_scope.INVALID_DATA }

    // HOSPITAL_CODES 는 콤마 구분이다 — 순서를 지킨다(첫 값 = 기본 병원)
    private val hospital_codes: List<String> = raw_hospital_codes
        .split(",")
        .map { it.trim() }
        .filter { it.isNotEmpty() && it != tenant_scope.INVALID_DATA }
        .distinct()
```

### 8.6 Liquibase master + changeSet 1개

`src/main/resources/db/changelog/db.changelog-master.yaml:1-38`

```yaml
# foodservice 마스터 changelog (DB = sup).
# 스키마는 MicroService 단위다(루트 CLAUDE.md §0) — 이 MS 전용 스키마는 `food_service`.
# 이 레포에 추가될 nutrition_consult MS 는 자기 스키마·자기 DATABASECHANGELOG 를 따로 갖는다.
# 스키마(food_service)는 **앱이 부팅하며 직접 만든다**(infrastructure/config/db_schema_initializer,
# SpringLiquibase 초기화 직전에 멱등 실행 — 앱 계정에 DB CREATE 권한 필요).
# …
# ⚠️ **적용된 changeSet 은 고치지 않는다** — 내용이 checksum 이라 주석 한 줄만 바꿔도 이미 적용된 DB 에서
#    `ValidationFailedException: changesets check sum` 으로 **부팅이 막힌다**(컴파일·테스트는 전부 통과한다).
#    스키마를 바꿔야 하면 `changes/00N-*.xml` 을 새로 추가한다.
#
# 최초 스키마는 changes/001-create-tables.xml 한 벌이다 — 모든 Entity 테이블이 「PK(<테이블명>_id, UUID) → tenant_id · hospital_code
#    → 업무 컬럼 → 감사 4종」 순서를 지키도록 처음부터 만든다. 이전 001 로 만든 DB(ULID id · 수진 스냅샷 컬럼)는
#    수동 트리(manual)의 014-reset-schema.sql 로 사람이 초기화한 뒤 기동한다.
# 002 는 운영 기본 마스터(식사 형태 — 시드 컨텍스트 아님), 900 은 병원별 예시 시드(context=seed — 급식 운영 설정 · 끼니 · 마감 차수 포함)다.
# 003 은 식종 분류(diet.diet_category_code) · 식사 형태 5종, 901 은 식종 분류마다 대표 1건 시드(context=seed)다.
#    형태 · 염도 NOT NULL 해제와 옛 식종 플래그 컬럼 삭제는 수동 트리(manual)의 015 · 016 을 사람이 적용한다.
databaseChangeLog:
  - include:
      file: changes/001-create-tables.xml
      relativeToChangelogFile: true
  - include:
      file: changes/002-seed-operational-master.xml
      relativeToChangelogFile: true
  - include:
      file: changes/003-diet-category-meal-form.xml
      relativeToChangelogFile: true
  - include:
      file: changes/900-seed-master.xml
      relativeToChangelogFile: true
  - include:
      file: changes/901-seed-diet-category.xml
      relativeToChangelogFile: true
```

changeSet — `meal_order` 테이블 생성 — `changes/001-create-tables.xml:134-205` (partial unique index + CHECK 는 `<sql>` 블록, rollback 은 `SELECT 1` 로 비활성)

```xml
    <changeSet id="foodservice-001-create-meal-order" author="foodservice">
        <createTable tableName="meal_order">
            <column name="meal_order_id" type="UUID">
                <constraints nullable="false" primaryKey="true" primaryKeyName="pk_meal_order"/>
            </column>
            <!-- 테넌트 범위 — 특정하지 못하면 INVALID_DATA(값을 비워 두지 않는다) -->
            <column name="tenant_id" type="VARCHAR(64)">
                <constraints nullable="false"/>
            </column>
            <column name="hospital_code" type="VARCHAR(64)">
                <constraints nullable="false"/>
            </column>
            <!-- NutritionOrder.basedOn — 물리 FK 없음(애그리거트 간 UUID 논리 참조) -->
            <column name="diet_order_id" type="UUID">
                <constraints nullable="false"/>
            </column>
            <!-- 끼니 — meal_type_setting.meal_type_code(값 집합은 설정이 정한다 — CHECK 로 고정하지 않는다) -->
            <column name="meal_type_code" type="VARCHAR(16)">
                <constraints nullable="false"/>
            </column>
            <column name="effective_start_date" type="DATE">
                <constraints nullable="false"/>
            </column>
            <!-- NULL = 계속 적용. 퇴원지시 수신 시 퇴원 예정일로 잘린다 -->
            <column name="effective_end_date" type="DATE"/>
            <!-- ACTIVE | REVOKED | COMPLETED -->
            <column name="status" type="VARCHAR(16)">
                <constraints nullable="false"/>
            </column>
            <column name="revoke_reason" type="VARCHAR(500)"/>
            <!-- 읽기 편의 사본(소유자 = diet_order) -->
            <column name="diet_type_code" type="VARCHAR(64)">
                <constraints nullable="false"/>
            </column>
            <column name="patient_id" type="UUID">
                <constraints nullable="false"/>
            </column>
            <column name="encounter_id" type="UUID">
                <constraints nullable="false"/>
            </column>
            <column name="ward_id" type="VARCHAR(64)"/>
            <column name="version" type="BIGINT" defaultValueNumeric="0">
                <constraints nullable="false"/>
            </column>
            <!-- 감사 — audit_entity_listener 가 채운다. *_by 는 직원 ID(shared 직원 uuid — 토큰 sub · 봉투 userId), 없으면 NULL -->
            <column name="created_at" type="TIMESTAMP WITH TIME ZONE">
                <constraints nullable="false"/>
            </column>
            <column name="created_by" type="UUID"/>
            <column name="updated_at" type="TIMESTAMP WITH TIME ZONE">
                <constraints nullable="false"/>
            </column>
            <column name="updated_by" type="UUID"/>
        </createTable>
        <!-- 같은 처방·끼니에 활성 세부오더는 1건만 — 중복 발행의 2차 방어(FS-4093). 접수자료 생성 · 결식 대상 탐색용 partial index -->
        <sql splitStatements="true" endDelimiter=";">
            ALTER TABLE meal_order
                ADD CONSTRAINT ck_meal_order_effective_period
                    CHECK (effective_end_date IS NULL OR effective_end_date >= effective_start_date);
            CREATE UNIQUE INDEX uk_meal_order_diet_order_id_meal_type_code
                ON meal_order (diet_order_id, meal_type_code) WHERE status = 'ACTIVE';
            CREATE INDEX ix_meal_order_tenant_id_hospital_code_meal_type_code
                ON meal_order (tenant_id, hospital_code, meal_type_code, effective_start_date, effective_end_date, ward_id)
                WHERE status = 'ACTIVE';
            CREATE INDEX ix_meal_order_patient_id ON meal_order (patient_id) WHERE status = 'ACTIVE';
            CREATE INDEX ix_meal_order_encounter_id ON meal_order (encounter_id) WHERE status = 'ACTIVE';
        </sql>
        <rollback>
            <!-- 자동 rollback 금지 — 되돌리기는 사람이 db/manual 로 적용한다(루트 CLAUDE.md · liquibase_guard). -->
            SELECT 1;
        </rollback>
    </changeSet>
```

changeSet 목록(`001-create-tables.xml`): diet_order(:30) · meal_order(:134) · meal_order_revision(:208) · meal_preparation(:288) · meal_closing(:411) · meal_change_request(:470) · diet(:559) · meal(:615) · meal_attribute(:683) · meal_form(:754) · foodservice_setting(:797) · meal_type_setting(:830) · meal_closing_setting(:894).

수동 트리(master 미포함 · 사람 실행) — `db/manual/014-reset-schema.sql:1-4`

```sql
-- food_service 스키마 초기화 — **사람이 적용한다**(master changelog 가 include 하지 않는 트리).
--
-- 무엇을: 스키마 안의 테이블 전부와 Liquibase 이력(databasechangelog · databasechangeloglock)을 지운다.
--         스키마 자체와 그 권한은 남긴다 — DBA 가 선행 생성한 환경에서도 그대로 쓸 수 있다.
```

### 8.7 Gradle

`backend/settings.gradle.kts:1-12`

```kotlin
// nutrition BC 멀티모듈 루트.
// MS(=배포단위) 당 단일 서비스 모듈. 레이어는 모듈 내부 패키지로 표현한다
// (presentation / application / domain / infrastructure). 레이어 의존 방향은 ArchUnit 가드로 강제.
//   - foodservice : Foodservice MS (/api/v1/foodservice)
plugins {
    // JDK 25 toolchain 자동 프로비저닝. 설치된 JDK 25 가 없으면 자동 다운로드.
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "nutrition"

include("foodservice")
```

`backend/build.gradle.kts:4-17` (플러그인 선언만 · Kotlin 2.3.0 · Boot 4.0.4 · JVM 25)

```kotlin
plugins {
    // common-eventbus(JVM 25) 참조를 위해 x-diagnostic-imaging 과 동일 스택으로 정렬.
    kotlin("jvm") version "2.3.0" apply false
    kotlin("plugin.spring") version "2.3.0" apply false
    kotlin("plugin.jpa") version "2.3.0" apply false
    // 조회(QueryDSL) Q-class 생성용 kapt — 하위 모듈에서 apply.
    kotlin("kapt") version "2.3.0" apply false
    id("org.springframework.boot") version "4.0.4" apply false
    id("io.spring.dependency-management") version "1.1.7" apply false
}

allprojects {
    group = "com.projectx"
    version = "1.0.0"
```

`backend/foodservice/build.gradle.kts:4-91` 의존성 블록

```kotlin
plugins {
    kotlin("jvm")
    kotlin("plugin.spring")
    // @Entity / @MappedSuperclass / @Embeddable all-open (누락 시 Hibernate proxy 실패 HHH000305)
    kotlin("plugin.jpa")
    // 조회(QueryDSL) Q-class 생성용 kapt
    kotlin("kapt")
    id("org.springframework.boot")
    id("io.spring.dependency-management")
}

val querydslVersion: String by project
val springdocVersion: String by project

dependencies {
    // domain — JPA API (jakarta.persistence) 는 data-jpa(→ hibernate-core) 가 Boot 관리버전으로 전이 제공한다.
    //   ⚠ 여기서 3.1.0 등으로 명시 핀하면 Hibernate7 이 요구하는 3.2.0 을 다운그레이드시켜
    //     jakarta.persistence.FindOption NoClassDefFound 로 기동 실패한다.

    // Write 경로: Spring Data JPA / Hibernate
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    // Read 경로: QueryDSL (JPAQueryFactory 타입세이프 쿼리 → view_model projection).
    //   OpenFeign fork 사용 — classic QueryDSL 5.x 는 Hibernate7 에서 APT 가 깨진다.
    implementation("io.github.openfeign.querydsl:querydsl-jpa:$querydslVersion")
    kapt("io.github.openfeign.querydsl:querydsl-apt:$querydslVersion:jakarta")
    // kapt(APT) 클래스패스에 필요한 jakarta 애노테이션 API
    kapt("jakarta.persistence:jakarta.persistence-api")
    kapt("jakarta.annotation:jakarta.annotation-api")
    // DB 스키마 마이그레이션(부팅 시 자동 적용) — Boot 4 는 자동설정이 기술별 모듈로 분리됨.
    //   liquibase-core 단독으로는 LiquibaseAutoConfiguration 이 없어 부팅 마이그레이션이 안 돈다
    //   → spring-boot-liquibase 를 써야 한다(liquibase-core 전이 포함).
    implementation("org.springframework.boot:spring-boot-liquibase")
    runtimeOnly("org.postgresql:postgresql")

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    // 사내 공통 EventBus — 외부 BC integration event 수신/발행 (Nexus).
    //   ⚠ 0.1.10 부터 **한 토픽에 여러 messageType 을 태우고 봉투의 @MessageType 으로 분배**한다.
    //   ⚠ 0.1.18 은 토픽 앞에 환경(profile)을 스스로 붙인다 — namespace 가 이미 `{환경}.{테넌트id}` 라
    //     application*.yml 의 `common.event-bus.env-prefix-enabled: false` 로 꺼서 토픽 이름을 그대로 유지한다.
    implementation("com.projectx.common:common-eventbus-kt:0.1.18")
    // 사내 공통 JWT — 인증 소비자(@RequireAuth · @TokenPrincipal AuthPrincipal). 토큰 발급은 iam 이 한다.
    implementation("com.projectx.common:common-jwt-kt:0.1.24")
    // @RequireAuth 는 aspect 다 — AspectJ 위빙 런타임이 있어야 프록시가 만들어진다.
    implementation("org.springframework.boot:spring-boot-starter-aspectj")
    implementation("com.projectx.common:common-observability-kt:0.1.18")
    implementation("com.projectx.common:common-masking-kt:0.1.6")
    implementation("org.springframework.kafka:spring-kafka")
    implementation("com.google.cloud.hosted.kafka:managed-kafka-auth-login-handler:1.0.6") {
        exclude(group = "io.confluent")
    }
    // common-eventbus 의 Rabbit 빈이 ConnectionFactory 를 요구(provider=kafka 라도 빈 생성). lazy 라 미사용 시 연결 없음.
    implementation("org.springframework.boot:spring-boot-starter-amqp")

    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:$springdocVersion")
    implementation("me.paulschwarz:spring-dotenv:4.0.0")

    // 도메인 단위 테스트 + ArchUnit(레이어 격리 가드)
    // 버전은 Boot BOM(JUnit 6) 이 정한다 — jupiter 를 5.x 로 핀하면 launcher(6.x)와 엔진이 어긋나
    // 테스트가 0건 탐지된다(빌드는 "no discovered tests" 로 실패).
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("com.tngtech.archunit:archunit-junit5:1.4.1")
}
```

---

## 9. 테스트

> `src/test/resources` 가 없다 — **DB · Spring 컨텍스트를 띄우는 테스트가 없다.** 영속성 테스트는 Hibernate 메타데이터를 **연결 없이** 만들어(`with_metadata`) 매핑·HQL 해석만 검사한다. 통합(SpringBootTest) 테스트는 0건.

### 9.1 ArchUnit 레이어 가드 — `src/test/kotlin/com/projectx/foodservice/architecture/layer_isolation_test.kt:1-44` (전문)

```kotlin
/**
 * 레이어 격리 가드 — 단일 서비스 모듈이라 컴파일 타임 모듈 경계가 없는 대신,
 * domain 이 Spring/바깥 레이어에 의존하지 않음을 ArchUnit 으로 강제한다(루트 CLAUDE.md "domain Spring 금지").
 */
@AnalyzeClasses(packages = ["com.projectx.foodservice"])
class layer_isolation_test {

    @ArchTest
    val domain_must_not_depend_on_spring: ArchRule =
        noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAnyPackage("org.springframework..")

    @ArchTest
    val domain_must_not_depend_on_outer_layers: ArchRule =
        noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAnyPackage(
                "..presentation..",
                "..application..",
                "..infrastructure..",
            )

    /** 조회는 QueryDSL 만 — MyBatis / 네이티브 SQL 매퍼 의존 금지(설계문서의 MyBatis 표기와 무관하게 팀 표준). */
    @ArchTest
    val no_mybatis_anywhere: ArchRule =
        noClasses()
            .that().resideInAPackage("com.projectx.foodservice..")
            .should().dependOnClassesThat().resideInAnyPackage("org.apache.ibatis..", "org.mybatis..")

    /** Controller 는 Aggregate / Entity 를 직렬화하지 않는다 — view_model 만 내려준다. */
    @ArchTest
    val presentation_must_not_depend_on_aggregates: ArchRule =
        noClasses()
            .that().resideInAPackage("..presentation..")
            .should().dependOnClassesThat().resideInAnyPackage("..domain.aggregatesmodel..")
}
```

### 9.2 단위 테스트(도메인) — `domain/meal_order_test.kt:24-82`

순수 Kotlin 객체 테스트(Spring/DB 없음). 픽스처 id 는 `test_ids.kt:6-20`(`TEST_SCOPE = tenant_scope.of("CHABD","CHABD")`, `tid(label)` = 이름 기반 UUID).

```kotlin
/**
 * 세부오더 애그리거트 테스트 — **속성은 적용일자별 이력이 소유한다**(class-diagram §3.2.1).
 * 이 테스트가 지키는 것 한 줄: **수정은 그 날짜부터고, 지난 날짜는 바뀌지 않는다.**
 */
class meal_order_test {

    private val now: Instant = Instant.parse("2026-09-01T00:30:00Z")
    private val start: LocalDate = LocalDate.of(2026, 9, 1)

    private fun sample(end: LocalDate? = null): meal_order = meal_order.issue(
        scope = TEST_SCOPE,
        diet_order_id = tid("DO-0001"),
        diet_order_status = diet_order_status.ACTIVE,
        diet_type_code = "DIABETIC_1800",
        patient_id = uuid(TEST_PATIENT_ID),
        encounter_id = uuid(TEST_ENCOUNTER_ID),
        ward_id = "51병동",
        meal_type_code = meal_type_code.LUNCH,
        attributes = meal_attributes(
            meal_form_code = meal_form_code.REGULAR,
            portion_code = portion_code.NORMAL,
            salt_level_code = salt_level_code.NORMAL,
        ),
        effective_start_date = start,
        effective_end_date = end,
        at = now,
    )

    @Test
    fun `발행은 적용 시작일부터 여는 리비전 1건을 함께 만든다`() {
        val order = sample()
        assertEquals(1, order.revisions.size)
        assertEquals(start, order.revisions.first().effective_from)
        assertNull(order.revisions.first().effective_to)
        assertEquals(meal_form_code.REGULAR, order.revision_at(start)?.meal_form_code)
    }

    @Test
    fun `수정은 그 날짜부터 적용되고 지난 날짜는 이전 값을 유지한다`() {
        val order = sample()
        val from = LocalDate.of(2026, 9, 10)
        order.revise(
            effective_from = from,
            meal_form = meal_form_code.PORRIDGE,
            portion_code_value = portion_code.SMALL,
            at = now,
        )

        assertEquals(2, order.revisions.size)
        // 직전 구간은 전날로 닫힌다 — 그 앞 날짜의 조리 지시는 그대로다.
        assertEquals(from.minusDays(1), order.revisions.first().effective_to)
        assertEquals(meal_form_code.REGULAR, order.revision_at(LocalDate.of(2026, 9, 9))?.meal_form_code)
        assertEquals(portion_code.NORMAL, order.revision_at(LocalDate.of(2026, 9, 9))?.portion_code)
        // 그 날짜부터는 새 값이다.
        assertEquals(meal_form_code.PORRIDGE, order.revision_at(from)?.meal_form_code)
        assertEquals(portion_code.SMALL, order.revision_at(from)?.portion_code)
        // 주지 않은 값은 직전 구간에서 그대로 이어받는다.
        assertEquals(salt_level_code.NORMAL, order.revision_at(from)?.salt_level_code)
    }
```

### 9.3 영속성 테스트(DB 없는 "통합") — `persistence/shared_join_query_test.kt:14-63` + 헬퍼 `persistence/schema_mapping_test.kt:198-230`

QueryDSL 쿼리를 `createQuery()` 까지만 만들어 **HQL 의미 분석**을 통과하는지 검사한다(uuid ↔ String 비교 같은 오류가 런타임 500 으로만 드러나는 걸 막는다).

```kotlin
/**
 * shared 조회 · 검색 조건이 HQL 로 해석되는지 — DB 없이 잡는다.
 *   환자 · 수진 · 직원 · 진료과 모두 uuid = uuid(직접 비교).
 *
 * 문자열과 uuid 를 그대로 비교하면 Hibernate 가 쿼리를 만들 때(의미 분석) 거절한다 — 빌드 · 단위테스트는 통과하고
 * 조회 API 를 처음 부를 때 500 으로만 드러난다. 실제 조회 쿼리를 `createQuery` 까지(실행 없이) 만들어 본다.
 */
class shared_join_query_test {

    @Test
    fun `shared 이름 조회 · 환자 검색 조건 · 병동 보드 처방 쿼리를 해석한다`() {
        with_metadata { metadata ->
            metadata.buildSessionFactory().use { session_factory ->
                val em = session_factory.createEntityManager()
                try {
                    val factory = JPAQueryFactory(em)
                    val shared = shared_directory(factory)
                    val ids = listOf(uuid(TEST_PATIENT_ID))
                    shared.patient_query(ids).createQuery()
                    shared.encounter_query(ids).createQuery()
                    shared.staff_query(ids).createQuery()
                    shared.department_query(ids).createQuery()

                    val diet_orders = com.projectx.foodservice.infrastructure.queries.diet_order_query_dsl(
                        factory, diet_types = dummy_diet_types(), meal_types = dummy_meal_types(), meal_order_queries = dummy_meal_order_queries(), shared = shared,
                    )
                    diet_orders.search_query(
                        diet_orders.search_predicate(TEST_SCOPE, diet_order_search_filter(keyword = "홍", ward_id = "51병동")),
                    ).createQuery()
                    …
                    val meal_orders = com.projectx.foodservice.infrastructure.queries.meal_order_query_dsl(
                        factory, dummy_diet_types(), dummy_meal_types(), shared,
                    )
                    meal_orders.board_query(TEST_SCOPE, "51병동").createQuery()
                } finally {
                    em.close()
                }
            }
        }
    }
```

```kotlin
/** 이 MS 의 영속성 단위와 같은 엔티티 · orm.xml 로 만든 Hibernate 메타데이터(DB 연결 없음). */
internal fun <T> with_metadata(block: (org.hibernate.boot.Metadata) -> T): T {
    val registry = StandardServiceRegistryBuilder()
        .applySetting("hibernate.dialect", PostgreSQLDialect::class.java.name)
        .applySetting("hibernate.boot.allow_jdbc_metadata_access", "false")
        .applySetting("hibernate.temp.use_jdbc_metadata_defaults", "false")
        .build()
    try {
        val metadata = MetadataSources(registry)
            .addResource("META-INF/orm.xml")
            .addAnnotatedClass(diet_order::class.java)
            .addAnnotatedClass(meal_order::class.java)
            …
            .addAnnotatedClass(shared_patient::class.java)
            .addAnnotatedClass(shared_encounter::class.java)
            .addAnnotatedClass(shared_user_account::class.java)
            .addAnnotatedClass(shared_department::class.java)
            .buildMetadata()
        return block(metadata)
    } finally {
        StandardServiceRegistryBuilder.destroy(registry)
    }
}
```

같은 파일 `schema_mapping_test.kt` 는 **Liquibase XML 을 파싱해 Hibernate 매핑과 대조**한다 — UUID 컬럼 일치(:35-49), shared 는 쓰는 컬럼만(:51-63), 모든 Entity 테이블 앞 3컬럼 = `<table>_id, tenant_id, hospital_code`(:65-71). 엔티티를 새로 추가하면 `with_metadata` 의 `addAnnotatedClass` 목록도 손봐야 한다.

---

## 10. 이 레포만의 특징 패턴

### 10.1 식단 처방 수신(diet-order) = 이벤트 추종 Aggregate + 같은 트랜잭션의 세부오더 자동 설정

- `diet_order` 는 사용자 액션이 **퇴식끼니 설정 하나뿐**이고 나머지는 이벤트 추종(`receive`·`apply_from_event`·`revoke`·`receive_discharge`) — `diet_order.kt:24-37`.
- 멱등은 inbox 테이블이 아니라 **자연키 `order_number` + `source_event_version`** — `diet_order.kt:224 is_stale_event` · 핸들러 `diet_order_command_handlers.kt:95-101` (순서 역전은 `idempotent=true` 로 정상 종료 → 재시도 루프 방지).
- 처음 받은 ACTIVE 처방은 **같은 트랜잭션에서 `meal_order_auto_setting.issue_defaults`** 로 기본 발행 끼니(`is_default_issue`)마다 세부오더를 낸다 — `diet_order_command_handlers.kt:86-92`, `meal_order_auto_setting.kt:24-110`. 인바운드라 **예외를 던지지 않고 WARN** 만(DLT 방지).
- payload 어휘(x-ancillary-main: `orderId`·`wardCode`)→ 도메인 어휘 번역은 `diet_order_event_handlers.kt` 한 곳. 환자 id 가 UUID 가 아니면 `shared.encounter` 로 역추적(`event_identity_resolver.kt:36-48`).
- **현재 Kafka 구독 route 미등록**(`topic_registry.kt:17-18`) — 핸들러 코드는 완성돼 있으나 실제 수신은 토픽을 붙여야 시작된다.

### 10.2 끼니/축 마스터 — enum 이 아니라 DB 행(설정 · Diet Aggregate)

- 끼니(아침·점심·간식…)는 `meal_type_setting` 행 — `meal_type_setting.kt:17-23` "끼니 목록은 enum 이 아니라 이 행이 정한다". 열 순서는 `serving_time`·`sort_order`(`query_support.kt:43-44`, `meal_selection.kt:123-126 serving_order`).
- 끼니 허용 검증(FS-4006)은 `require_meal_type_allowed`(`meal_selection.kt:41-54`) — `applicable_diet_codes`(쉼표 VARCHAR, `string_list_converter`)로 식이처방별 제한.
- 조절 축(형태·염도·열량·용량·양·개수)은 `Diet` Aggregate 의 자식 `meal_attribute` — `meal_attribute.kt:25-34` "값은 축이 아니라 처방·리비전이 가진다 … EAV 를 두지 않는다". 범위 검증(FS-4005)은 `diet_type_snapshot.validate_values`(`i_master_readers.kt:71-75`).
- 비운 값 채우기 우선순위: 요청 > 의사 처방 > 식이 코드 전용 축 기본값 > 식종 전체 축 기본값 > 상수 — `diet_prescription.resolve`(`order_value_objects.kt:66-80`), 완제품은 `diet_defaults.for_product`(열량÷단위kcal 올림, `diet_defaults.kt:25-42`).
- 쓰기 경로는 마스터를 **읽기 스냅샷 reader**(`i_diet_type_reader`·`i_foodservice_setting_reader`·`i_meal_form_reader`, `i_master_readers.kt:20-24`)로만 본다 — 다른 Aggregate 를 직접 로드하지 않는다.

### 10.3 세부오더 변경요청 409 흐름 (FS-4091 → `POST /meal-change-requests`)

- 세부오더(`meal_order`)는 마감 여부를 모른다 — 마감 상태는 접수자료(`meal_preparation.closed_round`)에 있다. 수정 핸들러가 `effective_from` 이후 미종료 접수자료를 조회해 `guard_not_closed` → `meal_closed_conflict_exception`(409 FS-4091, `context.hint.change_request_api = "POST /meal-change-requests"`) — `meal_order_command_handlers.kt:239-241, 337-352`, `foodservice_exceptions.kt:152-168`.
- 화면은 `allowed_actions.of_meal_order(status, closed)` 로 `REVISE/REVOKE` 대신 `REQUEST_CHANGE/REQUEST_CANCEL` 을 노출 — `allowed_actions.kt:37-51`.
- 변경요청 CHANGE/CANCEL 은 **대상 접수자료가 마감 상태여야** 생성 가능(`preparation_not_closed_domain_exception` 422) — `meal_change_request_command_handlers.kt:87-98`. ADD 는 접수자료 없이 세부오더+일자+끼니로.
- confirm → `foodservice_policy.confirm_applies_immediately`(기본 true)면 같은 트랜잭션에서 `change_request_applier.apply` 로 스냅샷 반영 → APPLIED; `POST /apply` 는 멱등 no-op — `meal_change_request_command_handlers.kt:229-253`, `foodservice_support.kt:8-22`. ADD 확인은 접수자료를 새로 만들고 **즉시 1차 마감**(`preparation.close`) — `:309-313`.
- 2차 이상 마감이 그 사이 CONFIRMED 요청을 먼저 반영한다 — `meal_closing_command_handlers.kt:68-80`.

### 10.4 allowed_actions — 상태 전이 규칙의 단일 출처

- `application/support/allowed_actions.kt:8-15` "상태 전이 규칙을 프런트에 중복 구현하지 않게 … 한쪽만 바꾸면 '버튼은 보이는데 422'". 4개 Aggregate 별 `of_diet_order`·`of_meal_order`·`of_meal_preparation`·`of_meal_change_request`.
- 모든 view_model 이 `allowed_actions: List<String>` 을 싣는다(예 `meal_order_slot_view_model.allowed_actions`, `diet_order_view_model` 조립 `diet_order_query_dsl.kt:154-156`).
- 입력 인자는 Query 가 계산한 파생값(`closed`·`issued_slot_count`·`npo`·`discharge_ordered`) — 도메인 가드와 **같은 규칙을 두 번** 표현한다(변경 시 둘 다 고친다).
- `of_meal_change_request` 는 정책값 `confirm_applies_immediately` 를 받아 APPLY 버튼 노출을 결정 — `:76-88`.

### 10.5 마감 회차(N차 마감) — 배치 단위 + 설정 기반 차수

- 마감은 **병동 × 날짜 × 끼니 × 차수 배치**가 접수자료마다 `meal_preparation.close()` 를 부르는 구조 — `meal_closing_command_handlers.kt:28-43`; "트랜잭션당 애그리거트 1개 규칙의 의도적 예외".
- 도메인: 차수는 `closed_round + 1` 이어야 하고(FS-4007 `closing_round_skipped_domain_exception`), 1차 마감에서만 `status=ACCEPTED`, 2·3차는 차수만 오른다 — `meal_preparation.kt` `close()`(§10 인용 `:19-36` 상대줄). 이력은 자식 `meal_closing`(CLOSE/REOPEN, `is_active`) — `meal_closing.kt:16-25`.
- 차수·기준시각은 `meal_closing_setting`(끼니 × 차수 × 식이처방군 × 병동, `day_offset`·`closing_time`) — `meal_closing_setting.kt:20-25`; 적용 묶음 선택·다음 마감 계산은 도메인 서비스 `meal_closing_deadline_policy`(`domain/services/meal_closing_deadline_policy.kt:10-48`, Spring 없음 → `domain_service_config` 에서 빈 등록).
- 배치 결과 도메인 이벤트(`meal_preparation_closed_domain_event`)는 Aggregate 가 아니라 **핸들러가 `dispatcher.publish` 로 직접** 올린다 — `meal_closing_command_handlers.kt:101-115`.
- 해제(`reopen`)는 활성 CLOSE 를 `deactivate` 하고 REOPEN 이력을 남기며 한 차수 내린다; IN_PROGRESS 면 거부 — `meal_preparation.kt reopen()`.

### 10.6 퇴원지시 · 퇴식끼니 — "먹는 대상" 판정 단일 진입점 `serves()`

- 퇴원은 원무 encounter 토픽이 아니라 **처방 BC 의 `discharge-order`** 로 받는다 — `discharge_order_event_handlers.kt:13-18`. 키는 shared 수진 uuid.
- 수신 핸들러: 수진의 활성 처방마다 `receive_discharge(planned)` → 세부오더 `change_effective_period(null, planned)` 로 종료일 자르기 → 예정일 **다음날부터** 미종료 접수자료 `cancel(DISCHARGE)`(**마감된 것도**) — `diet_order_command_handlers.kt:226-283`.
- 퇴식끼니(`PUT /diet-orders/{id}/discharge-meal`)는 퇴원일 당일 **그 끼니 이후** 접수자료만 취소 — `:350-376`; 판정은 VO `discharge_meal.serves(meal_date, meal_type_code, serving_order)`(`order_value_objects.kt:82-111`): 예정일 이후 false · 이전 true · 당일은 퇴식끼니 인덱스 이하만 true(미설정이면 전부 true).
- 접수자료 생성기(`meal_preparation_generator.generate`)와 세부오더 발행(`issue_meal_order_command_handler`)·자동 발행 모두 `diet_order.serves` 를 물어 퇴원 이후 식사를 만들지 않는다 — `meal_preparation_command_handlers.kt:175-180`, `meal_order_command_handlers.kt:94-105`.
- 퇴원지시 취소는 취소된 접수자료를 되살리지 않는다 — 접수 목록 조회가 다시 만든다(`:295-300` 주석).

### 10.7 접수자료(meal_preparation)는 **조회 시 생성**(배치 아님) + 스냅샷 고정

- `GET /meal-preparations?meal_type_code=…` 가 조회 전에 `generate_meal_preparations_command` 를 send — `meal_preparations_controller.kt:59-82` (컨트롤러가 Command 1 + Query 1 을 순서대로 부른다 — "한 엔드포인트 한 Command" 는 지킨다).
- 속성은 **그 식사일자의 리비전** `order.attributes_at(meal_date)` 에서 복사 — `meal_preparation_command_handlers.kt:183-185`. 유니크 `(meal_order_id, meal_date, meal_type_code)` 가 멱등 2차 방어.
- 집계·배식표 Query 는 스냅샷 컬럼만 읽고 원본 세부오더를 조인하지 않는다 — `meal_preparation_query_dsl.kt:34-43`.

### 10.8 세부오더 속성 = 적용일자별 리비전(`meal_order_revision`)

- Root 는 조리 속성을 갖지 않고 `revision_history` 가 보유 — "오늘의 수정이 지난 날짜의 조리 지시까지 바꾸지 않게" (`meal_order.kt:30-43`). `revise(effective_from)` 는 직전 구간을 전날로 닫고 새 구간을 연다; 같은 시작일이면 기존 리비전을 고친다 — `:145-190, 220-267`.
- 과거 `effective_from` 은 400 FS-4002(`meal_order_command_handlers.kt:201-206`), 보드 칸·접수자료 생성·통합이벤트는 전부 `revision_at(meal_date)` 로 날짜와 함께 묻는다.

---

## 11. 신입이 헷갈릴 포인트 10개

1. **스키마명은 `nutrition` 이 아니라 `${DB_SCHEMA:food_service}`** 다 — `application.yml:32,56-57`, `db_schema_initializer.kt:32`. `nutrition` 은 gradle rootProject 이름(`backend/settings.gradle.kts:10`)일 뿐. 모듈명(`foodservice`)과도 언더스코어가 달라 CLAUDE.md 의 "스키마 = 모듈명 snake_case" 와 1:1 이 아니다. 런타임 값은 기동 로그 `DB 스키마 확인/생성 완료 — db=…, schema=…` 로 확인.
2. **Kafka 수신 핸들러는 있으나 구독 route 가 없다** — `topic_registry.kt:17-18` 에 발행 `diet-definition` 1건만 등록. `diet_order_created_handler` 등은 빈으로 뜨지만 아무 토픽도 읽지 않는다(`idempotent_integration_event_handler.kt:19`). 아웃바운드 payload 14종(`outbound_integration_events.kt`)도 route 미등록이라 발행되지 않고, `meal_order_issued_domain_event` 는 AFTER_COMMIT 구독자가 없다(`@TransactionalEventListener` grep 1건 = `on_diet_changed`).
3. **쓰기 경로 Spring Data 인터페이스는 JPQL `@Query` 문자열을 쓴다** — `infrastructure/foodservice_context.kt:16-23` (프로퍼티명 underscore 때문에 파생 쿼리 불가). "조회는 QueryDSL · JPQL 문자열 금지" 규칙은 **Query(view_model) 경로** 얘기이고, Repository 의 Aggregate 로드는 JPQL 이다. HQL 에서는 `diet_order_id` 별칭이 아니라 실제 속성 `id` 를 써야 한다(`diet_order.kt:44`).
4. **감사 컬럼은 도메인이 채우지 않는다** — `orm.xml` 의 default entity-listener + MDC `userId` (`audit_entity_listener.kt:13-43`). `application.yml` 의 `spring.jpa.mapping-resources` 가 빠지면 **부팅·빌드 전부 통과한 채** created_at 이 EPOCH 로 저장된다. `@EntityListeners` 를 엔티티에 달면 ArchUnit(domain→infrastructure) 위반.
5. **Command 응답은 핸들러 결과가 아니라 재조회 view_model** 이다 — 컨트롤러 `load()` (`meal_orders_controller.kt:200-202`). 즉 쓰기 1 트랜잭션 + 읽기 1 트랜잭션(readOnly)이며, 예외로 `meal_closings_controller.to_view_model`(:106-119) 만 `closing_command_result` 를 직접 변환한다.
6. **JPA 어댑터는 `jpa.save()` 반환본이 아니라 인자 인스턴스에서 도메인 이벤트를 꺼낸다** — `@Version` 때문에 신규도 merge 로 저장돼 다른 인스턴스가 돌아온다(`foodservice_repositories.kt:38-46`). 반환본에서 읽으면 이벤트가 조용히 유실된다.
7. **`@Transactional` 은 `handle()` 에만** — 이벤트 핸들러 베이스에서 `handle_internal` 에 붙이면 self-invocation 으로 프록시를 안 타 트랜잭션이 안 걸린다(`idempotent_integration_event_handler.kt:19-22, 30-31`). `open` 키워드도 필요(final 이면 CGLIB 가로채기 실패).
8. **`is*` Boolean 은 Kafka payload 에서 키가 바뀔 수 있다** — `diet_integration_event.kt:23, 41-42` `@get:JsonProperty("isActive")` 로 못박는다. payload 는 camelCase(전 영역 snake_case 의 유일한 예외, `integration_event.kt:9`), REST JSON 은 snake_case(`foodservice_view_models.kt:11`).
9. **마감 상태는 세부오더가 아니라 접수자료에 있다** — 세부오더 수정 409(FS-4091)는 핸들러가 `meal_preparation.closed_round` 를 조회해 세운다(`meal_order_command_handlers.kt:43-49, 337-352`). 보드의 `closed` 플래그도 `meal_preparation` 에서 모은다(`meal_order_query_dsl.kt:101-110`). 세부오더 Aggregate 만 보고 "왜 422/409 가 나지?" 를 찾으면 못 찾는다.
10. **접수자료(`meal_preparation`)는 스케줄러가 아니라 `GET /meal-preparations?meal_type_code=` 조회가 만든다** — `meal_preparations_controller.kt:59-82`. 끼니를 비우고 조회하면 생성되지 않고, 퇴원·퇴식끼니로 취소된 건은 되살리지 않고 다시 생성된다(`diet_order_command_handlers.kt:295-300`). "데이터가 없다" 의 첫 점검은 끼니 파라미터와 `serves()` 판정.

(보너스) **`version` 은 두 경로로 409 가 된다** — JPA `OptimisticLockingFailureException`(`global_exception_handler.kt:75-78`) 와, 화면이 보낸 `version` 을 핸들러가 수동 비교하는 `stale_version_conflict_exception`(`foodservice_exceptions.kt:201-210`, 마스터·설정 저장). 둘 다 FS-4094.
(보너스) **`Clock` 빈은 `Asia/Seoul`** (`foodservice_configs.kt:36-38`) — `LocalDate.now(clock)` 이 병원의 "오늘"이다. 테스트에서 `Clock.systemUTC()` 를 넣으면 날짜 경계가 9시간 어긋난다. `KST` 상수는 `domain/common/hospital_zone.kt:6`.
(보너스) **`@Embedded` 가 없다** — VO(`diet_prescription`·`meal_attributes`)는 data class 이고 엔티티가 컬럼을 하나씩 매핑한다(`meal_order_revision.kt:214-219` 이유 설명). seedwork `value_object` 추상 클래스는 상속 0건.
