# diagnostic-result 백엔드 — 신입 교육용 소스 수집

- 대상: `x-diagnostic-result/backend/diagnostic-result`
- 아래 모든 경로는 이 모듈 루트 기준 상대 경로다. 코드 루트 = `src/main/kotlin/com/projectx/diagnosticresult/` (이하 `K/`), 리소스 = `src/main/resources/` (이하 `R/`), 테스트 = `src/test/kotlin/com/projectx/diagnosticresult/` (이하 `T/`).
- 인용은 전부 실제 소스 그대로이며 `경로:시작줄-끝줄` 을 붙였다.

---

## 1. 패키지 트리

`find src/main/kotlin -name '*.kt'` 결과 — main 160개 · test 35개.

| 레이어 | 파일 수 | 하위 묶음 |
|---|---|---|
| (루트) | 1 | `diagnostic_result_application.kt` |
| presentation | 9 | 컨트롤러 7 · `inbox_error_advice` · `token_principal_scope` |
| application | 59 | analysis 8 · commands 18 · integrationevents 12(events 7 · eventhandling 4 · `integration_event.kt`) · ports 4 · queries 10 · validations 7 |
| domain | 27 | aggregatesmodel 20(observation 12 · viewerconfiguration 4 · analysisreviewtarget 2 · observationanalyzeresult 2) · exceptions 1 · seedwork 6 |
| infrastructure | 64 | inbox 17 · queries 12 · config 9 · proxies 8 · communication 5 · repositories 4 · eventbus 3 · shared 3 · `diagnostic_result_context.kt` · `domain_event_dispatcher.kt` · persistence 1 |

```
K/
├─ diagnostic_result_application.kt
├─ presentation/
│  ├─ ai_chat_controller.kt · favorite_sets_controller.kt · inbox_messages_controller.kt
│  ├─ observation_analyze_results_controller.kt · observations_controller.kt
│  ├─ result_summary_controller.kt · viewer_configurations_controller.kt
│  ├─ inbox_error_advice.kt                 ← inbox 컨트롤러 전용 @RestControllerAdvice
│  └─ token_principal_scope.kt              ← AuthPrincipal → tenant_scope · employee_id
├─ application/
│  ├─ analysis/      analysis_delivery_service · analysis_review_target_listener · analysis_review_target_writer
│  │                 analysis_review_targets · observation_digest · observation_digest_builder
│  │                 observation_results_changed · result_summary_service
│  ├─ commands/      command · command_bus · {analyze_observations_by_patient, assign_favorite_sets,
│  │                 create_favorite_set, delete_favorite_set, register_viewer_configuration,
│  │                 rename_favorite_set, reorder_favorite_sets}_command(+_handler)
│  │                 observation_analyze_result_saver · viewer_configuration_lookup
│  ├─ integrationevents/
│  │  ├─ integration_event.kt
│  │  ├─ events/     observation_result_integration_event(단일 스키마) + 토픽별 6클래스
│  │  │              (diagnostic_imaging_result · functional_diagnostics_result · laboratory_result ·
│  │  │               pathology_result · observation_record_group_snapshot · external_record)
│  │  └─ eventhandling/ integration_event_handler · observation_result_handler ·
│  │                    observation_payload_mapper · event_tenant_resolver
│  ├─ ports/         i_analysis_agent_port · i_deployment_tenant_port · i_inbox_operation_port · i_record_document_port
│  ├─ queries/       i_observation_queries · i_viewer_configuration_queries · i_observation_analyze_result_queries
│  │                 i_record_document_queries · i_inbox_message_queries · *_view_model.kt(4) · observation_date_basis
│  └─ validations/   command_validator · {assign,create,register,rename,reorder}_*_validator · viewer_configuration_input_rules
├─ domain/
│  ├─ aggregatesmodel/
│  │  ├─ observation/            observation(추상 Root, JOINED) · observation_detail(MappedSuperclass)
│  │  │                          specimen_diagnostics · diagnostic_imaging · functional_diagnostics ·
│  │  │                          clinical_record · external_record · numeric_record _observation
│  │  │                          observation_payload(VO) · observation_factory · observation_category_code · i_observation_repository
│  │  ├─ viewerconfiguration/    viewer_configuration(Root) · favorite_set(자식) · favorite_observation(VO) · i_viewer_configuration_repository
│  │  ├─ analysisreviewtarget/   analysis_review_target(Root) · i_analysis_review_target_repository
│  │  └─ observationanalyzeresult/ observation_analyze_result(Root) · i_observation_analyze_result_repository
│  ├─ exceptions/    diagnostic_result_domain_exception(+not_found · validation · conflict)
│  └─ seedwork/      entity · i_aggregate_root · i_repository · domain_event · tenant_scope · uuid_format
└─ infrastructure/
   ├─ diagnostic_result_context.kt          ← Spring Data JPA 리포지토리 IF 모음(쓰기 전용, 전부 @Query JPQL)
   ├─ domain_event_dispatcher.kt
   ├─ config/        db_schema_initializer · security_config · querydsl_config · global_exception_handler
   │                 hospital_time · dotenv_environment_post_processor · deployment_tenant_adapter · open_api_config · web_config
   ├─ eventbus/      topic_registry · event_bus_subscription_initializer · received_result_event
   ├─ inbox/         inbox_message(Entity) · inbox_message_action · inbox_message_jpa_repository · inbox_receiver ·
   │                 inbox_tx_service · inbox_processor · inbox_dispatcher · inbox_drain_trigger · inbox_drain_executor ·
   │                 inbox_sweeper · inbox_retry_policy · inbox_properties · inbox_payload_rules · inbox_operation_service ·
   │                 inbox_failure_alert · inbox_failure_events · inbox_alert_properties
   ├─ persistence/   audit_entity_listener
   ├─ proxies/       ai_gateway_analysis_agent(+_provider · _properties) · analysis_response_normalizer ·
   │                 result_summary_response_normalizer · ai_response_text · record_document_http_adapter(+_properties)
   ├─ queries/       observation_query_dsl · observation_queries · viewer_configuration_query_dsl · viewer_configuration_queries ·
   │                 observation_analyze_result_query_dsl · observation_analyze_result_queries · inbox_message_query_dsl ·
   │                 inbox_message_queries · record_document_queries · read_rows · tenant_predicate · query_conversions
   ├─ repositories/  observation · viewer_configuration · observation_analyze_result · analysis_review_target _repository
   ├─ shared/        shared_patient · shared_department · shared_user_account   ← shared 스키마 읽기 전용
   └─ communication/ communication_inbox_client · _provider · _properties · _models · _token_source
```

리소스(`R/`): `application.yml` · `application-dev.yml` · `META-INF/orm.xml` · `META-INF/spring.factories` · `common-libs/{common-masking,common-observability}.yaml` · `db/changelog/db.changelog-master.yaml` + `changes/001~012-*.xml` · `db/manual/001~004-*.sql`(master 미포함, 사람이 실행).

테스트 트리(`T/`, 35개):

```
architecture/layer_isolation_test.kt                      ← ArchUnit 4규칙
domain/{favorite_set_rules, analysis_review_target_rules}_test.kt
analysis/{analysis_delivery_service, observation_digest_builder, result_summary}_test.kt
integrationevents/{encounter_order_snapshot, event_tenant_resolution, observation_abnormal_rule,
                   observation_delete_version, report_status_normalization, result_event_routing,
                   scheduled_at_and_result_type_default}_test.kt · test_tenant_support.kt
inbox/{inbox_error_advice, inbox_failure_alert, inbox_flow, inbox_operation, inbox_query,
       inbox_receiver, inbox_rules}_test.kt · inbox_test_support.kt
persistence/{analysis_status_query, audit_and_tenant_columns, inbox_operation_query, observation_date_basis_query,
             observation_encounter_query, schema_mapping, shared_patient_join_query}_test.kt   ← DB 없이 HQL 해석 · 매핑↔changelog 검사
communication/{communication_inbox_client, communication_wiring}_test.kt
config/hospital_time_test.kt · proxies/{ai_gateway_analysis_agent, record_document_adapter}_test.kt · test_ids.kt
```

---

## 2. API 추적표

공통: 모든 컨트롤러는 `@RequireAuth`(common-jwt-kt) + `@TokenPrincipal AuthPrincipal` → `K/presentation/token_principal_scope.kt` 로 `tenant_scope` · `employee_id` 를 얻는다. 조회 구현 클래스(`*_queries`)는 클래스 단위 `@Transactional(readOnly = true)`, 쓰기 핸들러는 `handle()` 단위 `@Transactional`. 예외 매핑은 `global_exception_handler`(전역)이고, `inbox_messages_controller` 만 `inbox_error_advice` 가 먼저 받는다.

### 2-1. viewer_configurations_controller (`/api/v1/diagnostic-result/viewer-configurations/me`)

| HTTP | 경로 | Controller.메서드 | Command/Query | Handler/구현.메서드 (TX) | Repository / Q타입 | 테이블 | 응답 |
|---|---|---|---|---|---|---|---|
| GET | `/` | `find_viewer_configuration` | `i_viewer_configuration_queries.find_viewer_configuration` | `viewer_configuration_queries.find_viewer_configuration` (`@Transactional(readOnly)` 클래스) → `viewer_configuration_query_dsl.find_viewer_configuration` | `Qviewer_configuration` · `Qfavorite_set` · `Qfavorite_observation` (3회 projection) | viewer_configuration · favorite_set · favorite_set_item | `viewer_configuration_view` / 404 |
| PUT | `/` | `save` | `register_viewer_configuration_command` (validator: `register_viewer_configuration_command_validator`) | `register_viewer_configuration_command_handler.handle` (`@Transactional`) → `find_or_new_viewer_configuration` → `viewer_configuration.set_last_view_layout` | `i_viewer_configuration_repository` → `viewer_configuration_repository` → `viewer_configuration_jpa_repository.find_first_by_user_id/save` | viewer_configuration | 204 (본문 없음) |

### 2-2. favorite_sets_controller (`/api/v1/diagnostic-result/viewer-configurations/me`)

| HTTP | 경로 | Controller.메서드 | Command (validator) | Handler.메서드 (TX) → Aggregate 메서드 | Repository | 테이블 | 응답 |
|---|---|---|---|---|---|---|---|
| POST | `/favorite-sets` | `create_favorite_set` | `create_favorite_set_command` (`create_favorite_set_command_validator`) | `create_favorite_set_command_handler.handle` (`@Transactional`) → `find_or_new` → `viewer_configuration.add_favorite_set` | `viewer_configuration_repository.create_or_update_viewer_configuration` (JPA save + `domain_event_dispatcher`) | viewer_configuration · favorite_set · favorite_set_item | 201 + `Location: …/favorite-sets/{id}` |
| PATCH | `/favorite-sets/{favorite_set_id}` | `rename_favorite_set` | `rename_favorite_set_command` (`rename_favorite_set_command_validator`) | `rename_favorite_set_command_handler.handle` (`@Transactional`) → `require_viewer_configuration`(없으면 404) → `rename_favorite_set` | 동일 | favorite_set | 204 |
| DELETE | `/favorite-sets/{favorite_set_id}` | `delete_favorite_set` | `delete_favorite_set_command` (validator 없음) | `delete_favorite_set_command_handler.handle` (`@Transactional`) → `require_` → `remove_favorite_set` (orphanRemoval) | 동일 | favorite_set · favorite_set_item | 204 |
| PUT | `/favorite-sets/order` | `reorder_favorite_sets` | `reorder_favorite_sets_command` (`reorder_favorite_sets_command_validator`) | `reorder_favorite_sets_command_handler.handle` (`@Transactional`) → `require_` → `reorder_favorite_sets` | 동일 | favorite_set | 204 |
| PUT | `/favorite-assignments` | `assign_favorite_sets` | `assign_favorite_sets_command` (`assign_favorite_sets_command_validator`) | `assign_favorite_sets_command_handler.handle` (`@Transactional`) → `find_or_new` → `assign_observation_to_sets` | 동일 | favorite_set_item | 204 |

### 2-3. observations_controller (`/api/v1/diagnostic-result/observations`) — 전부 조회

| HTTP | 경로 | Controller.메서드 | Query IF | 구현.메서드 (TX) | QueryDSL Q타입 | 테이블 | 응답 |
|---|---|---|---|---|---|---|---|
| GET | `/?patient_id&observation_type_code&category_code&from_date&to_date&date_basis` | `search_observations_by_category` | `i_observation_queries.search_observations_by_category` | `observation_queries.search_observations_by_category` (`readOnly` 클래스) → `observation_query_dsl.search_observations_by_category` + `find_user_names` | `Qobservation` + 자식 5종 Q(`Qspecimen_diagnostics_observation` · `Qdiagnostic_imaging_observation` · `Qfunctional_diagnostics_observation` · `Qclinical_record_observation` · `Qexternal_record_observation`) LEFT JOIN + `Qshared_department`; 이름은 `Qshared_user_account` 별도 IN 조회 | observation + 자식 5테이블 + shared.department + shared.user_account | `List<observation_category_view>` |
| GET | `/encounters/{encounter_id}` | `search_observations_by_encounter` | `…search_observations_by_encounter` | `observation_query_dsl.search_observations_by_encounter` (`encounter_where`) | 동일 | 동일 | `List<observation_category_view>` |
| GET | `/origin-encounters/{origin_encounter_id}` | `search_observations_by_origin_encounter` | `…search_observations_by_origin_encounter` | `observation_query_dsl.search_observations_by_origin_encounter` | 동일 | 동일 | `List<observation_category_view>` |
| GET | `/exam-dates?patient_id&date_basis` | `search_observation_exam_dates` | `…search_observation_exam_dates` | `observation_query_dsl.search_observation_exam_dates` (Tuple) → `to_exam_date_views`(Kotlin 집계) | `Qobservation` + `Qspecimen_diagnostics_observation` | observation · specimen_diagnostics_observation | `List<observation_exam_date_view>` |
| GET | `/patients?keyword&page&size` | `search_observation_patients` | `…search_observation_patients` | `observation_query_dsl.search_observation_patients` + `count_observation_patients` | `Qobservation` + `Qshared_patient` (group by patient_id) | observation · shared.patient | `page_view<observation_patient_view>` |
| GET | `/{id}` | `find_observation` | `…find_observation` | `observation_query_dsl.find_observation` | 목록과 같은 `category_query()` | 동일 | `observation_category_view` / 404 |
| GET | `/{id}/record-document` | `find_record_document` | `i_record_document_queries.find_record_document` | `record_document_queries.find_record_document` (메서드 TX 없음; `find_reference` 만 `@Transactional(readOnly)`) → `i_record_document_port.fetch_html` → `record_document_http_adapter` (외부 HTTP) | `Qobservation` (`find_record_reference`) | observation | `record_document_view` / 404 · 503 · 502 |

### 2-4. observation_analyze_results_controller (`/api/v1/diagnostic-result/observation-analyze-results`)

| HTTP | 경로 | Controller.메서드 | Command/Query | Handler/구현 (TX) | Repository / Q타입 | 테이블 | 응답 |
|---|---|---|---|---|---|---|---|
| GET | `/{id}` | `find_one` | `i_observation_analyze_result_queries.find_observation_analyze_result` | `observation_analyze_result_queries` (`readOnly` 클래스) → `observation_analyze_result_query_dsl.find_observation_analyze_result`; 파생값은 `analysis_response_normalizer.derive` | `Qobservation_analyze_result` | observation_analyze_result | `observation_analyze_result_view` / 404 |
| GET | `/?patient_id&page&size` | `search_by_patient` | `…search_by_patient` | `observation_analyze_result_query_dsl.search_by_patient` + `count_by_patient` | `Qobservation_analyze_result` | observation_analyze_result | `page_view<observation_analyze_result_view>` |
| GET | `/status?patient_id` | `find_analysis_status` | `…find_analysis_status` | `observation_analyze_result_query_dsl.find_review_target` | `Qanalysis_review_target` | analysis_review_target | `observation_analysis_status_view` |
| POST | `/` | `run_analysis` | `analyze_observations_by_patient_command` (validator 없음) | `analyze_observations_by_patient_command_handler.handle` — **`@Transactional` 없음**. ① `analysis_review_targets.claim` → `analysis_review_target_writer.claim` (`REQUIRES_NEW`) ② `analysis_delivery_service.deliver` (조회는 `observation_queries` readOnly TX, AI Gateway 호출은 TX 밖) ③ `observation_analyze_result_saver.save` (`@Transactional`) | `i_analysis_review_target_repository` · `i_observation_queries` · `i_analysis_agent_port` · `i_observation_analyze_result_repository` | analysis_review_target · observation(+자식·shared 읽기) · observation_analyze_result | 201 + Location / 204 (대상 아님 · 결과 0건) / 503 / 502 |

### 2-5. result_summary_controller · ai_chat_controller (command_bus 미경유)

| HTTP | 경로 | Controller.메서드 | 호출 | TX | 테이블 | 응답 |
|---|---|---|---|---|---|---|
| POST | `/api/v1/diagnostic-result/result-summary` | `summarize` | `result_summary_service.summarize(scope, patient_id)` → `i_observation_queries.search_observations_by_category` + `i_analysis_agent_port.agent().summarize` | 서비스 TX 없음(조회만 readOnly) | observation(+자식·shared) 읽기 | `result_summary_view` (에이전트 실패도 200) |
| POST | `/api/v1/diagnostic-result/ai-chat` | `chat` | `i_analysis_agent_port.agent().chat(message, thread_id)` → `ai_gateway_analysis_agent.chat` | 없음 | 없음 | `ai_chat_view` / 503 |

### 2-6. inbox_messages_controller (`/api/v1/diagnostic-result`) — `inbox_error_advice` 가 오류 본문 담당

| HTTP | 경로 | Controller.메서드 | Query/Port | 구현.메서드 (TX) | Repository / Q타입 | 테이블 | 응답 |
|---|---|---|---|---|---|---|---|
| GET | `/inbox-summary` | `summary` | `i_inbox_message_queries.summary` | `inbox_message_queries.summary` (`readOnly` 클래스) → `inbox_message_query_dsl.summary` | `Qinbox_message` | inbox_message | `inbox_summary_view` |
| GET | `/inbox-messages` | `find_messages` | `…find_messages(inbox_message_criteria)` | `inbox_message_query_dsl.find_rows` + `count_rows` (커서 페이징) | `Qinbox_message` | inbox_message | `inbox_message_page_view` |
| GET | `/inbox-messages/{inbox_message_id}` | `find_message` | `…find_message` | `find_rows_by_ids` + `find_detail_row` + `find_actions` + `find_user_names` | `Qinbox_message` · `Qinbox_message_action` · `Qshared_user_account` | inbox_message · inbox_message_action · shared.user_account | `inbox_message_detail_view` / 404 |
| GET | `/inbox-messages/{id}/sequence` | `find_sequence` | `…find_sequence` | `find_before` · `find_after` · `count_around` | `Qinbox_message` | inbox_message | `inbox_sequence_view` / 404 |
| GET | `/inbox-keys` | `find_keys` | `…find_keys(inbox_key_criteria)` | `find_key_rows` + `find_rows_by_ids` | `Qinbox_message` | inbox_message | `inbox_key_list_view` |
| POST | `/inbox-messages/{id}/retry` | `retry` | `i_inbox_operation_port.retry` | `inbox_operation_service.retry` (`@Transactional`) → `lock_for_operation`(SKIP LOCKED) → `inbox_message.reset_for_retry` → `inbox_message_stored` 발행 | `inbox_message_jpa_repository` · `inbox_message_action_jpa_repository` | inbox_message · inbox_message_action | `inbox_operation_result` |
| POST | `/inbox-message-retries` | `retry_many` | 동일 | 동일 (최대 200건) | 동일 | 동일 | 동일 |
| PUT | `/inbox-messages/{id}/payload` | `edit_payload` | `…edit_payload` | `inbox_operation_service.edit_payload` (`@Transactional`) → `inbox_payload_rules.changed_locked_paths` → `replace_payload` + `reset_for_retry` | 동일 | 동일 | `inbox_operation_result` |
| POST | `/inbox-messages/{id}/resend` | `resend` | `…resend` | `inbox_operation_service.resend` (`@Transactional`) → `inbox_message.resent_from` 새 행 | 동일 | 동일 | `inbox_operation_result` (새 행 id) |
| POST | `/inbox-messages/{id}/terminate` | `terminate` | `…terminate` | `inbox_operation_service.terminate` (`@Transactional`) → `inbox_message.terminate` | 동일 | 동일 | `inbox_operation_result` |
| POST | `/inbox-message-terminations` | `terminate_many` | 동일 | 동일 | 동일 | 동일 | 동일 |
| POST | `/inbox-messages/{id}/alert` | `send_alert` | `…send_alert` | `inbox_operation_service.send_alert` (**TX 없음**) → `inbox_failure_alert.send_now` → `communication_inbox_client.emit` (외부 HTTP) | `inbox_message_jpa_repository.findById` | inbox_message (읽기) | `inbox_alert_result` / 404 · 409 · 503 · 502 |

---

## 3. 쓰기 경로 샘플 — `POST /viewer-configurations/me/favorite-sets` (즐겨찾기 세트 생성)

### 3-1. Controller

`K/presentation/favorite_sets_controller.kt:31-49`
```kotlin
@RestController
@RequireAuth
@RequestMapping("/api/v1/diagnostic-result/viewer-configurations/me")
@Tag(name = "FavoriteSets", description = "검사결과 화면의 즐겨찾기 세트 API (토큰 사용자)")
class favorite_sets_controller(
    private val command_bus: command_bus
) {

    @Operation(summary = "즐겨찾기 세트 생성 (검사를 주면 새 세트에 바로 넣는다)")
    @PostMapping("/favorite-sets")
    fun create_favorite_set(
        @TokenPrincipal principal: AuthPrincipal,
        @RequestBody request: create_favorite_set_request
    ): ResponseEntity<Void> {
        val created_id = command_bus.send(request.to_command(principal.to_tenant_scope(), principal.employee_id()))
        return ResponseEntity
            .created(URI.create("/api/v1/diagnostic-result/viewer-configurations/me/favorite-sets/$created_id"))
            .build()
    }
```

요청 DTO → command 변환 `K/presentation/favorite_sets_controller.kt:97-115`
```kotlin
// 요청 DTO — 컨트롤러 파일 내 동거. user_id · scope 는 토큰이 준다

data class create_favorite_set_request(
    val set_name: String,
    val observation_type_code: String? = null,
    val observation_code: String? = null
) {
    // 검사는 둘 다 왔을 때만 넣는다 — 한쪽만 오면 validator 가 빈 값으로 잡는다
    fun to_command(scope: tenant_scope, user_id: UUID) = create_favorite_set_command(
        scope = scope,
        user_id = user_id,
        set_name = set_name,
        observation = if (observation_type_code == null && observation_code == null) {
            null
        } else {
            favorite_observation_input(observation_type_code.orEmpty(), observation_code.orEmpty())
        }
    )
}
```

토큰 → 테넌트 범위 · 직원 id `K/presentation/token_principal_scope.kt:10-20`
```kotlin
// 토큰 주체 → 테넌트 범위 · 직원 id. HTTP 요청의 tenant_id · hospital_code · user_id 는 이 파일에서만 나온다.
// AuthPrincipal 은 presentation 밖으로 내보내지 않는다 — application 이하에는 tenant_scope 와 UUID 만 넘긴다.

// claim 이 비어 있으면 tenant_scope.of 가 INVALID_DATA 로 채운다(라이브러리가 필수 claim 을 검증하므로 정상 토큰에서는 생기지 않는다)
fun AuthPrincipal.to_tenant_scope(): tenant_scope = tenant_scope.of(tenantId, hospitalCode)

// 토큰 sub = 직원 레코드 uuid(shared 직원 id). UUID(하이픈 포함 36자 표준 형식 · strict_uuid_or_null)가 아니면
// 이 BC 가 쓸 수 없는 토큰이라 401(AUTH_TOKEN_INVALID)로 거절한다
fun AuthPrincipal.employee_id(): UUID =
    strict_uuid_or_null(userId)
        ?: throw JwtAuthenticationException(JwtErrorCode.AUTH_TOKEN_INVALID, "토큰 사용자 ID 가 UUID 형식이 아닙니다")
```

### 3-2. Command 클래스 · 마커 IF

`K/application/commands/create_favorite_set_command.kt:6-21`
```kotlin
// 즐겨찾기 세트 생성 커맨드
// - observation 을 주면 새 세트에 바로 넣는다(별표 선택창의 「새 세트」). 다른 세트의 소속은 건드리지 않는다
// - 사용자 설정이 없으면 함께 만든다
// - scope · user_id 는 컨트롤러가 토큰에서 채운다(요청 본문 · 경로가 아니다)
data class create_favorite_set_command(
    val scope: tenant_scope,
    val user_id: UUID,
    val set_name: String,
    val observation: favorite_observation_input? = null
) : command<String>   // 반환: 새 favorite_set.id

// 즐겨찾기 한 항목 입력 — (검사구분, 검사코드) 쌍
data class favorite_observation_input(
    val observation_type_code: String,
    val observation_code: String
)
```

`K/application/commands/command.kt:1-9`
```kotlin
package com.projectx.diagnosticresult.application.commands

// CQRS command 마커
interface command<R>

// 핸들러 — 1 command : 1 Handler 매핑
interface command_handler<C : command<R>, R> {
    fun handle(command: C): R
}
```

### 3-3. command_bus — 디스패치 방식(Validator 전부 → ApplicationContext 에서 handle 파라미터 타입으로 핸들러 탐색 · 캐시)

`K/application/commands/command_bus.kt:9-48`
```kotlin
// Validation → Handler 파이프라인을 내장한 커맨드 디스패처
// 핸들러가 아니라 인프라 성격의 라우터라 @Component 로 둔다(핸들러 · saver 는 @Service)
@Component
class command_bus(
    private val application_context: ApplicationContext,
    private val validators: List<command_validator<*>>
) {
    private val logger = KotlinLogging.logger {}
    private val handler_cache = ConcurrentHashMap<Class<*>, String>()

    @Suppress("UNCHECKED_CAST")
    fun <C : command<R>, R> send(command: C): R {
        val command_type = command::class.java

        validators
            .filter { it.command_type().java.isAssignableFrom(command_type) }
            .forEach { validator ->
                logger.debug { "커맨드 검증: ${command_type.simpleName}" }
                (validator as command_validator<C>).validate(command)
            }

        // !it.isBridge 필수 — Kotlin 제네릭 타입 소거 시 bridge method 가 모든 핸들러를 매칭하는 문제 방지
        val handler_bean_name = handler_cache.getOrPut(command_type) {
            application_context.getBeanNamesForType(command_handler::class.java)
                .firstOrNull { bean_name ->
                    val bean = application_context.getBean(bean_name) as command_handler<*, *>
                    val handle_method = bean::class.java.methods
                        .firstOrNull { it.name == "handle" && it.parameterCount == 1 && !it.isBridge }
                    handle_method?.parameterTypes?.firstOrNull()?.isAssignableFrom(command_type) == true
                }
                ?: throw IllegalStateException("핸들러를 찾을 수 없습니다: ${command_type.simpleName}")
        }

        val handler = application_context.getBean(handler_bean_name) as command_handler<C, R>
        logger.debug { "커맨드 처리 시작: ${command_type.simpleName}" }
        val result = handler.handle(command)
        logger.debug { "커맨드 처리 완료: ${command_type.simpleName}" }
        return result
    }
}
```

### 3-4. Validator

`K/application/validations/command_validator.kt:7-17`
```kotlin
// 모든 command 별 Validator 가 구현하는 인터페이스
interface command_validator<C : command<*>> {
    fun command_type(): KClass<C>
    fun validate(command: C)
}

// 검증 실패 시 발생 — global_exception_handler 가 400 으로 매핑
class command_validation_exception(
    message: String,
    val errors: List<String>
) : diagnostic_result_domain_exception("command validation failed: $message")
```

`K/application/validations/create_favorite_set_command_validator.kt:7-21`
```kotlin
@Component
class create_favorite_set_command_validator : command_validator<create_favorite_set_command> {

    override fun command_type(): KClass<create_favorite_set_command> = create_favorite_set_command::class

    override fun validate(command: create_favorite_set_command) {
        val errors = mutableListOf<String>()
        validate_set_name(command.set_name, errors)
        command.observation?.let { validate_observation_key(it.observation_type_code, it.observation_code, errors) }

        if (errors.isNotEmpty()) {
            throw command_validation_exception("create_favorite_set_command 검증 실패", errors)
        }
    }
}
```

`K/application/validations/viewer_configuration_input_rules.kt:9-26`
```kotlin
// 세트 이름 — 앞뒤 공백 제거 후 1~30자
internal fun validate_set_name(set_name: String, errors: MutableList<String>) {
    val name = set_name.trim()
    if (name.isEmpty()) errors.add("세트 이름을 입력하세요")
    if (name.length > favorite_set.MAX_SET_NAME_LENGTH) {
        errors.add("세트 이름은 ${favorite_set.MAX_SET_NAME_LENGTH}자 이하여야 합니다")
    }
}

// 검사 식별 — (검사구분, 검사코드) 둘 다 필수
internal fun validate_observation_key(
    observation_type_code: String,
    observation_code: String,
    errors: MutableList<String>
) {
    if (observation_type_code.isBlank()) errors.add("observation_type_code 는 필수입니다")
    if (observation_code.isBlank()) errors.add("observation_code 는 필수입니다")
}
```

### 3-5. Handler (트랜잭션 경계)

`K/application/commands/create_favorite_set_command_handler.kt:8-25`
```kotlin
// POST /api/v1/diagnostic-result/viewer-configurations/me/favorite-sets — 즐겨찾기 세트 생성 (설정이 없으면 함께 생성)
@Service
class create_favorite_set_command_handler(
    private val repository: i_viewer_configuration_repository
) : command_handler<create_favorite_set_command, String> {

    @Transactional
    override fun handle(command: create_favorite_set_command): String {
        val config = repository.find_or_new_viewer_configuration(command.scope, command.user_id)

        val created = config.add_favorite_set(
            set_name = command.set_name,
            first_item = command.observation?.let { favorite_observation(it.observation_type_code, it.observation_code) }
        )
        repository.create_or_update_viewer_configuration(config)
        return created.id
    }
}
```

핸들러 공통 확장함수 `K/application/commands/viewer_configuration_lookup.kt:12-26`
```kotlin
// 사용자 화면 설정 Command 핸들러 공통 — 설정을 (범위, user_id) 로 찾는 두 가지 규칙.
//   require_* : 없으면 404 — 이름변경 · 삭제 · 순서변경(기존 세트를 전제로 한다)
//   find_or_new_* : 없으면 새로 만든다(upsert) — 설정 저장 · 세트 생성 · 검사 배정

// 없으면 404. 응답 메시지에는 식별자를 넣지 않는다 — user_id 는 로그로만 남긴다.
internal fun i_viewer_configuration_repository.require_viewer_configuration(scope: tenant_scope, user_id: UUID): viewer_configuration =
    find_viewer_configuration(scope, user_id)
        ?: run {
            logger.info { "사용자 화면 설정 없음: user_id=$user_id" }
            throw diagnostic_result_not_found_exception("사용자 화면 설정을 찾을 수 없습니다")
        }

// 없으면 새 설정 — 저장은 호출부가 한다(create_or_update)
internal fun i_viewer_configuration_repository.find_or_new_viewer_configuration(scope: tenant_scope, user_id: UUID): viewer_configuration =
    find_viewer_configuration(scope, user_id) ?: viewer_configuration(user_id = user_id, scope = scope)
```

### 3-6. Aggregate Root 메서드

`K/domain/aggregatesmodel/viewerconfiguration/viewer_configuration.kt:58-79`
```kotlin
    constructor(user_id: UUID, scope: tenant_scope) : this() {
        this.id = UUID.randomUUID().toString()
        this.user_id = user_id
        assign_tenant(scope)
    }

    // === 즐겨찾기 세트 ===

    // 세트 추가 — 맨 뒤 순서. 개수 상한·이름 중복 검증
    // first_item 을 주면 새 세트에 바로 넣는다(별표 선택창의 「새 세트」) — 다른 세트의 소속은 건드리지 않는다
    fun add_favorite_set(set_name: String, first_item: favorite_observation? = null): favorite_set {
        if (_favorite_sets.size >= MAX_FAVORITE_SETS) {
            throw diagnostic_result_domain_exception("즐겨찾기 세트는 ${MAX_FAVORITE_SETS}개까지 만들 수 있습니다")
        }
        val name = favorite_set.normalize_set_name(set_name)
        ensure_unique_set_name(name, except_id = null)
        // 자식 세트는 Root 의 테넌트 범위를 그대로 받는다
        val created = favorite_set(name, sort_order = _favorite_sets.size, scope = tenant_scope.of(tenant_id, hospital_code))
        first_item?.let { created.add_item(it) }
        _favorite_sets.add(created)
        return created
    }
```

`K/domain/aggregatesmodel/viewerconfiguration/viewer_configuration.kt:133-161`
```kotlin
    // === 내부 ===

    private fun find_favorite_set(favorite_set_id: String): favorite_set =
        _favorite_sets.firstOrNull { it.id == favorite_set_id }
            // 응답으로 나가는 메시지라 세트 id 를 싣지 않는다.
            ?: throw diagnostic_result_not_found_exception("즐겨찾기 세트를 찾을 수 없습니다")

    private fun ensure_unique_set_name(name: String, except_id: String?) {
        if (_favorite_sets.any { it.id != except_id && it.set_name == name }) {
            throw diagnostic_result_domain_exception("이미 있는 세트 이름입니다: $name")
        }
    }

    private fun renumber_favorite_sets() {
        _favorite_sets.forEachIndexed { i, set -> set.move_to(i) }
    }

    companion object {
        // 사용자당 즐겨찾기 세트 상한 — 화면(favoriteSets.ts)과 같은 값
        const val MAX_FAVORITE_SETS = 20

        // 허용된 보기 방식 코드 — 넓은 폭 보기 방식 4종(화면 어휘 그대로)
        val ALLOWED_LAYOUT_CODES: Set<String> = setOf(
            "DETAIL",
            "REPORT",
            "CUMULATIVE",
            "HOURLY"
        )
    }
```

도메인 이벤트 등록: 이 레포의 Aggregate 는 **`add_domain_event` 를 부르는 곳이 main 소스에 없다** (`grep -rn "add_domain_event("` → seedwork `entity.kt` 정의뿐). 근거 주석은 `K/infrastructure/domain_event_dispatcher.kt:7-8` ("본 MS 는 자체 발행 이벤트가 없지만 … 표준 구조 유지"). 프로세스 내 이벤트(`observation_results_changed`)는 `ApplicationEventPublisher` 로 직접 발행한다(§6).

### 3-7. seedwork 베이스

`K/domain/seedwork/entity.kt:10-57`
```kotlin
// 도메인 entity 베이스 — VARCHAR 문자열 ID + 테넌트 범위 + 감사 컬럼 + 도메인 이벤트 보유
//
// 모든 Entity 테이블의 컬럼 순서는 「PK → tenant_id · hospital_code → 업무 컬럼 → 감사 4종」이다(순서는 changelog 가 정한다).
// - PK 컬럼명은 <테이블명>_id 다 — 프로퍼티는 id 그대로 두고 각 Entity 가 @AttributeOverride(JOINED 자식은 @PrimaryKeyJoinColumn)로 바꾼다.
// - tenant_id · hospital_code 는 생성 때 assign_tenant 로 정하고 바꾸지 않는다. 특정하지 못하면 INVALID_DATA 다.
// - 감사 4종은 도메인이 채우지 않는다 — infrastructure 의 audit_entity_listener(JPA 기본 리스너, orm.xml 등록)가
//   @PrePersist · @PreUpdate 에서 record_creation · record_modification 을 부른다. domain 은 시각 · 직원을 모른다.
@MappedSuperclass
abstract class entity {

    @Id
    open var id: String = ""
        protected set

    @Column(name = "tenant_id", nullable = false, updatable = false, length = 64)
    open var tenant_id: String = tenant_scope.INVALID_DATA
        protected set

    @Column(name = "hospital_code", nullable = false, updatable = false, length = 64)
    open var hospital_code: String = tenant_scope.INVALID_DATA
        protected set

    // 생성 일시 — INSERT 때 리스너가 채운다
    @Column(name = "created_at", nullable = false, updatable = false)
    open var created_at: Instant = Instant.EPOCH
        protected set

    // 생성한 직원 ID(shared 직원 uuid) — 인증 주체가 없거나 UUID 가 아니면 null
    @Column(name = "created_by", updatable = false)
    open var created_by: UUID? = null
        protected set

    // 마지막 수정 일시 — INSERT · UPDATE 때 리스너가 채운다
    @Column(name = "updated_at", nullable = false)
    open var updated_at: Instant = Instant.EPOCH
        protected set

    // 마지막으로 수정한 직원 ID(shared 직원 uuid) — 인증 주체가 없거나 UUID 가 아니면 null
    @Column(name = "updated_by")
    open var updated_by: UUID? = null
        protected set

    @Transient
    private val _domain_events: MutableList<domain_event> = mutableListOf()

    val domain_events: List<domain_event>
        get() = _domain_events.toList()
```

`K/domain/seedwork/entity.kt:58-105`
```kotlin
    fun add_domain_event(event: domain_event) {
        _domain_events.add(event)
    }

    fun remove_domain_event(event: domain_event) {
        _domain_events.remove(event)
    }

    fun clear_domain_events() {
        _domain_events.clear()
    }

    fun is_transient(): Boolean = id.isBlank()

    // 테넌트 범위 지정 — 생성 때 한 번만 부른다. JOINED 자식 등 같은 값을 따로 들고 있는 하위 클래스가 override 한다
    protected open fun assign_tenant(scope: tenant_scope) {
        this.tenant_id = scope.tenant_id
        this.hospital_code = scope.hospital_code
    }

    // 생성 감사 기록 — 생성 때는 수정 컬럼도 같은 값으로 채운다.
    // ⚠️ 영속성 리스너(audit_entity_listener) 전용이다 — 도메인 로직에서 부르지 않는다
    open fun record_creation(at: Instant, by: UUID?) {
        this.created_at = at
        this.created_by = by
        this.updated_at = at
        this.updated_by = by
    }

    // 수정 감사 기록 — 생성 컬럼은 건드리지 않는다.
    // ⚠️ 영속성 리스너(audit_entity_listener) 전용이다 — 도메인 로직에서 부르지 않는다
    open fun record_modification(at: Instant, by: UUID?) {
        this.updated_at = at
        this.updated_by = by
    }

    override fun equals(other: Any?): Boolean {
        if (other == null || other !is entity) return false
        if (this === other) return true
        if (this::class != other::class) return false
        if (is_transient() || other.is_transient()) return false
        return id == other.id
    }

    override fun hashCode(): Int {
        return if (!is_transient()) id.hashCode() xor 31 else super.hashCode()
    }
}
```

마커 3종 — `K/domain/seedwork/i_aggregate_root.kt:3-4`, `domain_event.kt:3-4`, `i_repository.kt:3-4`
```kotlin
// Aggregate Root 마커 인터페이스 — Aggregate Root 만 구현
interface i_aggregate_root
```
```kotlin
// 도메인 이벤트 마커 — Aggregate 내부에서 발행되는 이벤트
interface domain_event
```
```kotlin
// Aggregate Repository 의 마커 — 각 Aggregate Root 마다 하나의 Repository 인터페이스를 둔다
interface i_repository<T : i_aggregate_root>
```

`K/domain/seedwork/tenant_scope.kt:3-21`
```kotlin
// 테넌트 범위 값 객체(ValueObject) — (테넌트, 병원) 한 쌍. 모든 Entity 가 생성 때 받고, 모든 조회가 이 범위 안에서만 본다.
// - HTTP 요청은 토큰(AuthPrincipal)이, 수신 이벤트는 event_tenant_resolver 가 만든다.
// - 값을 특정할 수 없으면 INVALID_DATA 로 채운다 — 빈 값이 저장돼 조회 범위가 흐려지지 않게, 나중에 찾아낼 수 있게.
// - 빈 값 → INVALID_DATA 변환은 of() 한 곳이다. 생성자를 직접 부르지 않는다.
data class tenant_scope private constructor(
    val tenant_id: String,
    val hospital_code: String,
) {
    companion object {
        // 테넌트 · 병원을 특정하지 못했을 때 넣는 값
        const val INVALID_DATA = "INVALID_DATA"

        // 빈 값(null · 공백)은 INVALID_DATA 로 바꾼다. 앞뒤 공백은 떼고 대소문자는 받은 그대로 둔다
        fun of(tenant_id: String?, hospital_code: String?): tenant_scope =
            tenant_scope(normalize(tenant_id), normalize(hospital_code))

        private fun normalize(value: String?): String = value?.trim()?.takeIf { it.isNotEmpty() } ?: INVALID_DATA
    }
}
```

### 3-8. Repository IF (domain) → JPA 어댑터 (infrastructure) → dispatcher

`K/domain/aggregatesmodel/viewerconfiguration/i_viewer_configuration_repository.kt:7-12`
```kotlin
// 사용자별 화면 설정 Aggregate Repository
// - 키는 (테넌트 범위, user_id). 신규/수정 모두 create_or_update 로 처리
interface i_viewer_configuration_repository : i_repository<viewer_configuration> {
    fun create_or_update_viewer_configuration(config: viewer_configuration): viewer_configuration
    fun find_viewer_configuration(scope: tenant_scope, user_id: UUID): viewer_configuration?
}
```

`K/infrastructure/repositories/viewer_configuration_repository.kt:11-26`
```kotlin
/** viewer_configuration Aggregate Repository 구현. */
@Repository
class viewer_configuration_repository(
    private val jpa: viewer_configuration_jpa_repository,
    private val dispatcher: domain_event_dispatcher,
) : i_viewer_configuration_repository {

    override fun create_or_update_viewer_configuration(config: viewer_configuration): viewer_configuration {
        val saved = jpa.save(config)
        dispatcher.dispatch(saved)
        return saved
    }

    override fun find_viewer_configuration(scope: tenant_scope, user_id: UUID): viewer_configuration? =
        jpa.find_first_by_user_id(scope.tenant_id, scope.hospital_code, user_id).firstOrNull()
}
```

Spring Data IF 모음 — 파생 쿼리 금지 이유 포함 `K/infrastructure/diagnostic_result_context.kt:12-21, 36-47`
```kotlin
/**
 * JPA 영속성 컨텍스트 — Aggregate 별 Spring Data Repository 인터페이스 모음(쓰기 경로 전용).
 * 조회는 QueryDSL(`infrastructure/queries`)이 담당한다.
 *
 * ⚠️ 프로퍼티명이 snake_case 라 **메서드 이름 기반 파생 쿼리를 쓸 수 없다**
 * (`findFirstBySourceAggregateId` 는 `sourceAggregateId` 프로퍼티를 찾다 부팅에 실패한다).
 * 전부 `@Query` JPQL 로 명시한다.
 *
 * 자연키 조회는 전부 테넌트 범위(tenant_id · hospital_code) 안에서 한다.
 */
```
```kotlin
interface viewer_configuration_jpa_repository : JpaRepository<viewer_configuration, String> {

    @Query(
        "select c from viewer_configuration c where c.tenant_id = :tenant_id and c.hospital_code = :hospital_code " +
            "and c.user_id = :user_id",
    )
    fun find_first_by_user_id(
        @Param("tenant_id") tenant_id: String,
        @Param("hospital_code") hospital_code: String,
        @Param("user_id") user_id: UUID,
    ): List<viewer_configuration>
}
```

`K/infrastructure/domain_event_dispatcher.kt:7-18`
```kotlin
// Aggregate 가 누적한 도메인 이벤트를 Spring ApplicationEventPublisher 로 발행
// (본 MS 는 자체 발행 이벤트가 없지만 미래 확장 / 일관성 유지를 위해 표준 구조 유지)
@Component
class domain_event_dispatcher(
    private val publisher: ApplicationEventPublisher
) {
    fun dispatch(entity: entity) {
        val events = entity.domain_events.toList()
        entity.clear_domain_events()
        events.forEach { publisher.publishEvent(it) }
    }
}
```

감사 컬럼은 영속화 시점에 리스너가 채운다 — `K/infrastructure/persistence/audit_entity_listener.kt:27-44`
```kotlin
class audit_entity_listener {

    @PrePersist
    fun on_pre_persist(target: Any) {
        (target as? entity)?.record_creation(Instant.now(), current_employee_id())
    }

    @PreUpdate
    fun on_pre_update(target: Any) {
        (target as? entity)?.record_modification(Instant.now(), current_employee_id())
    }

    // 비로그인 발행(anonymous)은 null — 형식 검사는 strict_uuid_or_null(하이픈 포함 36자 표준 형식만)
    private fun current_employee_id(): UUID? =
        MDC.get(JwtKeys.USER_ID)
            ?.takeIf { it.trim() != MessageEnvelope.ANONYMOUS_USER_ID }
            ?.let(::strict_uuid_or_null)
}
```

---

## 4. 읽기 경로 샘플 — `GET /observations?patient_id=…` (환자 검사분류별 결과 목록)

### 4-1. Controller

`K/presentation/observations_controller.kt:29-66`
```kotlin
@RestController
@RequireAuth
@RequestMapping("/api/v1/diagnostic-result/observations")
@Tag(name = "Observations", description = "환자 검사결과 통합 조회 API (검체진단/영상진단/기능진단/임상관찰/외부연동기록)")
class observations_controller(
    private val queries: i_observation_queries,
    private val record_document_queries: i_record_document_queries,
) {

    @Operation(summary = "환자ID 기준 검사분류별 검사결과 목록 (결과일자 역순)")
    @GetMapping
    fun search_observations_by_category(
        @TokenPrincipal principal: AuthPrincipal,
        @Parameter(description = "환자 ID(shared 환자 uuid) — 필수", required = true)
        @RequestParam patient_id: UUID,
        @Parameter(
            description = "검사 분류 코드 (SPECIMEN_DIAGNOSTICS / DIAGNOSTIC_IMAGING / " +
                "FUNCTIONAL_DIAGNOSTICS / CLINICAL_RECORD) — 미지정 시 전체",
        )
        @RequestParam(required = false) observation_type_code: String?,
        @Parameter(description = "분류 코드 (laboratory / imaging / procedure / vital-signs / exam / survey) — 미지정 시 전체")
        @RequestParam(required = false) category_code: String?,
        @Parameter(description = "조회 시작일 (yyyy-MM-dd)")
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) from_date: LocalDate?,
        @Parameter(description = "조회 종료일 (yyyy-MM-dd) — 그날 하루 전체를 포함한다")
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) to_date: LocalDate?,
        @Parameter(
            description = "조회기간 날짜기준 — order(오더일) · encounter(수진일) · performed(검사일) · result(결과일, 기본). " +
                "그 날짜가 없으면 오더일은 처방일시, 수진일은 검사일로 거른다",
        )
        @RequestParam(required = false) date_basis: String?,
    ): ResponseEntity<List<observation_category_view>> =
        ResponseEntity.ok(
            queries.search_observations_by_category(
                principal.to_tenant_scope(), patient_id, observation_type_code, category_code, from_date, to_date,
                observation_date_basis.of(date_basis),
            )
        )
```

### 4-2. Query IF (application)

`K/application/queries/i_observation_queries.kt:7-20`
```kotlin
// 검사결과 조회 인터페이스 (Read-side) — 전부 테넌트 범위(scope) 안에서만 본다
interface i_observation_queries {
    // 환자ID 기준 검사분류별 결과 목록 — 분류 미지정 시 전체, 결과일자 역순
    // category_code 는 분류 코드(laboratory / imaging / …) 필터다 — 검사분류와 독립적으로 건다
    // 기간(from_date ~ to_date, 양끝 포함 · 한쪽만도 된다)은 date_basis 의 날짜로 거른다(observation_date_basis)
    fun search_observations_by_category(
        scope: tenant_scope,
        patient_id: UUID,
        observation_type_code: String? = null,
        category_code: String? = null,
        from_date: LocalDate? = null,
        to_date: LocalDate? = null,
        date_basis: observation_date_basis = observation_date_basis.RESULT,
    ): List<observation_category_view>
```

### 4-3. 조회 서비스 (readOnly TX · read row → view_model)

`K/infrastructure/queries/observation_queries.kt:17-47`
```kotlin
/**
 * 검사결과 Read-side 조회 — QueryDSL read row 를 view_model 로 매핑한다.
 * 쿼리는 `observation_query_dsl` 이 갖는다.
 */
@Service
@Transactional(readOnly = true)
class observation_queries(
    private val query_dsl: observation_query_dsl,
    // 저장은 절대 시각(Instant) — API 로는 병원 현지시각으로 내린다
    private val hospital_clock: Clock,
) : i_observation_queries {

    override fun search_observations_by_category(
        scope: tenant_scope,
        patient_id: UUID,
        observation_type_code: String?,
        category_code: String?,
        from_date: LocalDate?,
        to_date: LocalDate?,
        date_basis: observation_date_basis,
    ): List<observation_category_view> =
        query_dsl.search_observations_by_category(
            scope,
            patient_id,
            observation_type_code?.takeIf { it.isNotBlank() },
            category_code?.takeIf { it.isNotBlank() },
            from_date,
            to_date,
            date_basis,
        ).let(::to_category_views)
```

`K/infrastructure/queries/observation_queries.kt:113-171`
```kotlin
    /** 진료의 · 처방의 이름은 행들의 id 를 모아 shared 에서 한 번에 읽는다(조인하지 않는다 — classdiagram §1.6). */
    private fun to_category_views(rows: List<observation_category_read_row>): List<observation_category_view> {
        val names = query_dsl.find_user_names(rows.flatMap { listOfNotNull(it.doctor_id, it.ordering_doctor_id) }.toSet())
        return rows.map { to_category_view(it, names) }
    }

    private fun to_category_view(row: observation_category_read_row, user_names: Map<UUID, String?>): observation_category_view {
        // 자식 5종 중 채워진 하나를 쓴다 — 분류별로 결과 본문이 실리는 컬럼이 다르다.
        val result_flag = row.spec_result_flag ?: row.func_result_flag ?: row.record_result_flag ?: row.ext_result_flag
        return observation_category_view(
            id = row.id,
            observation_type_code = row.observation_type_code,
            observation_type_name = row.observation_type_name,
            category_code = row.category_code ?: "",
            diagnostic_service_section_code = row.diagnostic_service_section_code,
            source_type_code = row.ext_source_type_code,
            observation_code = row.observation_code,
            observation_name = row.observation_name,
            encounter_id = row.encounter_id,
            origin_encounter_id = row.origin_encounter_id,
            order_id = row.order_id,
            encounter_class = row.encounter_class,
            encounter_date = row.encounter_date,
            department_name = row.department_name,
            doctor_name = row.doctor_id?.let { user_names[it] },
            ordering_doctor_name = row.ordering_doctor_id?.let { user_names[it] },
            ordered_at = hospital_clock.to_local(row.ordered_at),
            order_date = row.order_date,
            performed_at = hospital_clock.to_local(row.performed_at),
            result_datetime = hospital_clock.to_local(row.result_datetime),
            verified_at = hospital_clock.to_local(row.verified_at),
            // 검체진단은 결과값(진검)이 비면 진단문(병리)을 쓴다 — 한 자식 안에 둘 다 있다.
            result_summary = row.spec_result_value?.takeIf { it.isNotBlank() }
                ?: row.spec_diagnosis?.takeIf { it.isNotBlank() }
                ?: row.img_conclusion
                ?: row.func_result_value
                ?: row.record_result_value
                ?: row.ext_result_value
                ?: "",
            result_unit = row.spec_result_unit_code ?: row.func_result_unit_code ?: row.record_result_unit_code
                ?: row.ext_result_unit_code,
            result_flag = result_flag,
            result_group_code = row.spec_result_group_code ?: row.record_result_group_code ?: row.ext_result_group_code,
            delta_value = row.spec_delta_value,
            // 참고범위 — 자식 중 채워진 하나. 결과마다 실려 온 값이라 같은 검사도 결과일마다 다를 수 있다
            reference_low = row.spec_reference_low ?: row.func_reference_low ?: row.record_reference_low ?: row.ext_reference_low,
            reference_high = row.spec_reference_high ?: row.func_reference_high ?: row.record_reference_high ?: row.ext_reference_high,
            report_status = row.spec_report_status ?: row.img_report_status ?: row.func_report_status,
            // 비정상 — 적재 때 도메인이 계산해 저장한 값(observation.compute_abnormal). 여기서 다시 판정하지 않는다
            is_abnormal = row.is_abnormal,
            is_critical = row.spec_is_critical == true,
            // 부속 이미지 식별자 — 자식 테이블(영상/검체진단/기능진단) LEFT JOIN 결과
            pacs_study_uid = row.pacs_study_uid,
            slide_image_url = row.slide_image_url,
            waveform_data_url = row.waveform_data_url,
            // 서식 식별자 — 있으면 화면이 보고서뷰 · 상세 「서식」 탭에서 원천 모듈 HTML 을 연다
            record_id = row.record_id,
        )
    }
```

### 4-4. QueryDSL 구현 — `JPAQueryFactory` · projection · 조인

`K/infrastructure/queries/observation_query_dsl.kt:30-68`
```kotlin
/**
 * 검사결과 조회 QueryDSL 구현.
 *
 * - **모든 조회는 테넌트 범위(tenant_id · hospital_code) 안에서만 본다** — `tenant_predicate`(부모 observation 컬럼) 한 곳이 조건을 만든다.
 * - 환자명은 `shared.patient` 가 원천이다 — `patient_id`(uuid) 하나로 left join 한다. observation 은 환자명을 갖지 않는다.
 * - 진료과명은 `shared.department` 가 원천이다 — `department_id` 하나로 left join 한다(`department_join`).
 * - 쓰기 Aggregate 를 하이드레이션하지 않고 read row / view_model 로 **직접 projection** 한다(CQRS 읽기/쓰기 분리).
 * - JOINED 상속이라 부모(observation)에 자식 5종을 `id` 기준 LEFT JOIN 한다(ad-hoc entity join).
 *   진검과 병리는 같은 자식(검체진단)이라 결합 대상이 하나 줄었다 — 구분은 `diagnostic_service_section_code` 다.
 * - 결과 본문·단위·flag·결과그룹의 우선순위 합치기는 SQL 함수 문자열(coalesce) 대신 Kotlin 에서 한다 —
 *   타입세이프 projection 을 유지하기 위해서다(read row 가 자식 컬럼을 그대로 받는다).
 */
@Repository
class observation_query_dsl(
    private val query_factory: JPAQueryFactory,
    // 날짜 기간 → 시각 경계는 병원 시간대로 자른다(시각 컬럼은 timestamptz)
    private val hospital_clock: Clock,
) {
    private val o = Qobservation.observation
    private val spec = Qspecimen_diagnostics_observation.specimen_diagnostics_observation
    private val img = Qdiagnostic_imaging_observation.diagnostic_imaging_observation
    private val func = Qfunctional_diagnostics_observation.functional_diagnostics_observation
    private val record = Qclinical_record_observation.clinical_record_observation
    private val ext = Qexternal_record_observation.external_record_observation
    private val patient = Qshared_patient.shared_patient
    private val department = Qshared_department.shared_department
    private val user_account = Qshared_user_account.shared_user_account

    /**
     * observation ↔ shared.patient 연결 — **id 만으로** 잇는다(테넌트 · 병원은 조건에 넣지 않는다 — UUID 가 이미 유일하다).
     * 둘 다 uuid 라 직접 비교한다(shared 의 PK 인덱스를 쓴다).
     */
    private val patient_join = o.patient_id.eq(patient.patient_id)

    /**
     * observation ↔ shared.department 연결 — **id 만으로** 잇는다(둘 다 uuid — 직접 비교).
     * 진료과 id 가 없거나 짝이 없으면 진료과명이 null 이다.
     */
    private val department_join = o.department_id.eq(department.department_id)
```

생성자 projection(컬럼 순서 = read row 생성자 순서) `K/infrastructure/queries/observation_query_dsl.kt:70-96` (이하 129행까지 자식 컬럼이 이어진다)
```kotlin
    /** 검사분류별 결과 1행 projection — 컬럼 순서 = read row 생성자 순서. */
    private val category_projection = Projections.constructor(
        observation_category_read_row::class.java,
        o.id,
        o.observation_type_code,
        o.observation_type_name,
        o.category_code,
        o.diagnostic_service_section_code,
        o.observation_code,
        o.observation_name,
        o.encounter_id,
        o.origin_encounter_id,
        o.order_id,
        o.encounter_class,
        o.encounter_date,
        department.department_name,
        o.doctor_id,
        o.ordering_doctor_id,
        o.ordered_at,
        o.order_date,
        o.performed_at,
        o.result_datetime,
        o.verified_at,
        o.is_abnormal,
        spec.result_value,
        spec.diagnosis,
        img.conclusion,
```

조회 메서드 · where · 기간 조건 `K/infrastructure/queries/observation_query_dsl.kt:135-167`
```kotlin
    fun search_observations_by_category(
        scope: tenant_scope,
        patient_id: UUID,
        observation_type_code: String?,
        category_code: String?,
        from_date: LocalDate?,
        to_date: LocalDate?,
        date_basis: observation_date_basis,
    ): List<observation_category_read_row> =
        category_query()
            .where(search_where(scope, patient_id, observation_type_code, category_code, from_date, to_date, date_basis))
            .orderBy(o.result_datetime.desc())
            .fetch()

    /** 목록 조건 — 실행 없이 JPQL 해석을 검사하는 테스트도 이 조건을 쓴다. */
    internal fun search_where(
        scope: tenant_scope,
        patient_id: UUID,
        observation_type_code: String?,
        category_code: String?,
        from_date: LocalDate?,
        to_date: LocalDate?,
        date_basis: observation_date_basis,
    ): BooleanBuilder {
        val where = BooleanBuilder()
            .and(tenant_predicate(o.tenant_id, o.hospital_code, scope))
            .and(o.patient_id.eq(patient_id))
            .and(o.is_deleted.isFalse)
        observation_type_code?.let { where.and(o.observation_type_code.eq(it)) }
        category_code?.let { where.and(o.category_code.eq(it)) }
        period_predicate(date_basis, from_date, to_date)?.let { where.and(it) }
        return where
    }
```

`K/infrastructure/queries/observation_query_dsl.kt:176-203`
```kotlin
    private fun basis_columns(basis: observation_date_basis): date_basis_columns = when (basis) {
        observation_date_basis.RESULT -> date_basis_columns(date = null, timestamp = o.result_datetime)
        observation_date_basis.PERFORMED -> date_basis_columns(date = null, timestamp = o.performed_at)
        observation_date_basis.ORDER -> date_basis_columns(date = o.order_date, timestamp = o.ordered_at)
        observation_date_basis.ENCOUNTER -> date_basis_columns(date = o.encounter_date, timestamp = o.performed_at)
    }

    /** 날짜기준의 칸 한 쌍 — 날짜 칸(있으면 우선) + 날짜 칸이 비었거나 없을 때 보는 시각 칸. */
    private class date_basis_columns(val date: DatePath<LocalDate>?, val timestamp: DateTimePath<Instant>)

    /**
     * 조회기간 조건 — 날짜기준의 칸(`basis_columns`)으로 거른다. 날짜 칸이 있으면 그 칸이 기간 안이거나, 비어 있고 시각 칸이 기간 안이면 든다.
     * 시각 칸은 병원 시간대의 [from 00:00, to 다음날 00:00) 반열린 구간으로 본다 — 끝날 하루 전체가 들어간다
     * (= 시각을 병원 시간대 날짜로 자른 값이 [from, to] 안 — 타임라인의 날짜와 같은 기준이다).
     * 양끝이 모두 없으면 null(기간 조건 없음).
     */
    private fun period_predicate(basis: observation_date_basis, from: LocalDate?, to: LocalDate?): BooleanExpression? {
        if (from == null && to == null) return null
        val from_ts = hospital_clock.start_of(from)
        val to_ts_exclusive = hospital_clock.end_of_exclusive(to)
        fun timestamp_in(path: DateTimePath<Instant>): BooleanExpression =
            listOfNotNull(from_ts?.let { path.goe(it) }, to_ts_exclusive?.let { path.lt(it) }).reduce { a, b -> a.and(b) }
        fun date_in(path: DatePath<LocalDate>): BooleanExpression =
            listOfNotNull(from?.let { path.goe(it) }, to?.let { path.loe(it) }).reduce { a, b -> a.and(b) }
        val columns = basis_columns(basis)
        val date = columns.date ?: return timestamp_in(columns.timestamp)
        return date_in(date).or(date.isNull.and(timestamp_in(columns.timestamp)))
    }
```

부모+자식 5종+shared.department LEFT JOIN `K/infrastructure/queries/observation_query_dsl.kt:383-393`
```kotlin
    /** 검사결과 목록 · 단건 공통 쿼리(실행 전) — 실행 없이 JPQL 해석을 검사하는 테스트도 이 쿼리를 쓴다. */
    internal fun category_query(): JPAQuery<observation_category_read_row> =
        query_factory
            .select(category_projection)
            .from(o)
            .leftJoin(spec).on(spec.id.eq(o.id))
            .leftJoin(img).on(img.id.eq(o.id))
            .leftJoin(func).on(func.id.eq(o.id))
            .leftJoin(record).on(record.id.eq(o.id))
            .leftJoin(ext).on(ext.id.eq(o.id))
            .leftJoin(department).on(department_join)
```

shared.user_account 는 조인하지 않고 id 묶음 IN 조회 `K/infrastructure/queries/observation_query_dsl.kt:336-350`
```kotlin
    /**
     * 직원 이름(진료의 · 처방의) — 결과 행의 id 를 모아 **한 번에** 읽는다. 조인하지 않는다 —
     * shared.user_account 에 같은 id 가 두 행 이상인 환경에서도 결과 행이 늘지 않게(같은 id 면 처음 것).
     */
    fun find_user_names(ids: Collection<UUID>): Map<UUID, String?> =
        if (ids.isEmpty()) {
            emptyMap()
        } else {
            query_factory.select(user_account.user_account_id, user_account.user_account_name)
                .from(user_account)
                .where(user_account.user_account_id.`in`(ids))
                .fetch()
                .groupBy({ it.get(user_account.user_account_id)!! }, { it.get(user_account.user_account_name) })
                .mapValues { it.value.firstOrNull() }
        }
```

환자목록(shared.patient 조인 · group by) `K/infrastructure/queries/observation_query_dsl.kt:356-374`
```kotlin
    /** 환자목록 쿼리(실행 전) — 실행 없이 JPQL 해석을 검사하는 테스트도 이 쿼리를 쓴다. */
    internal fun patient_list_query(scope: tenant_scope, keyword: String?): JPAQuery<observation_patient_read_row> =
        query_factory
            .select(
                Projections.constructor(
                    observation_patient_read_row::class.java,
                    o.patient_id,
                    patient.patient_name.max().coalesce(""),
                    o.result_datetime.min(),
                    o.result_datetime.max(),
                    o.count(),
                ),
            )
            .from(o)
            .leftJoin(patient).on(patient_join)
            .where(patient_list_where(scope, keyword))
            .groupBy(o.patient_id)
            .orderBy(o.result_datetime.max().desc())
```

공통 조건 · 변환 — `K/infrastructure/queries/tenant_predicate.kt:7-10`, `query_conversions.kt:10-23`
```kotlin
// 테넌트 범위 조건 — 모든 QueryDSL 조회가 `tenant_id = scope.tenant_id AND hospital_code = scope.hospital_code` 를 거는 한 곳.
// 어느 Q 클래스의 컬럼으로 걸지(부모 observation · 분석 이력 · 화면 설정)는 호출부가 넘긴다.
internal fun tenant_predicate(tenant: StringPath, hospital: StringPath, scope: tenant_scope): BooleanExpression =
    tenant.eq(scope.tenant_id).and(hospital.eq(scope.hospital_code))
```
```kotlin
/** 절대 시각 → 병원 시간대 벽시계(view_model 표기). */
internal fun Clock.to_local(instant: Instant): LocalDateTime = LocalDateTime.ofInstant(instant, zone)

/** 날짜의 시작 — 병원 시간대 그날 00:00. */
internal fun Clock.start_of(date: LocalDate?): Instant? = date?.atStartOfDay(zone)?.toInstant()

/** 날짜의 끝(배타) — 다음 날 00:00. 끝 날짜는 그 날을 포함한다. */
internal fun Clock.end_of_exclusive(date: LocalDate?): Instant? = date?.plusDays(1)?.atStartOfDay(zone)?.toInstant()

/** 페이지 보정 — page<0 은 0, size 는 1~max. */
internal fun safe_page_of(page: Int): Int = page.coerceAtLeast(0)
internal fun safe_size_of(size: Int, max: Int): Int = size.coerceIn(1, max)
internal fun offset_of(page: Int, size: Int): Long = page.toLong() * size
internal fun total_pages_of(total: Long, size: Int): Int = ((total + size - 1) / size).toInt()
```

### 4-5. read row (projection 대상 DTO) — 앞부분

`K/infrastructure/queries/read_rows.kt:8-45`
```kotlin
/**
 * QueryDSL projection 대상 read DTO 모음(JPA entity 아님).
 *
 * **Kotlin 쪽 파생이 필요한 조회에만 둔다.** 파생이 없는 조회(즐겨찾기)는
 * view_model 로 직접 projection 하므로 read row 가 없다. 시각은 저장 그대로(`Instant`) 받고 view_model 로 옮길 때 병원 현지시각으로 바꾼다.
 *
 * 검사분류별 결과는 부모(observation) + 자식 4종을 LEFT JOIN 해 **자식 컬럼을 그대로 받고**
 * 우선순위 합치기(coalesce)를 Kotlin 에서 한다 — SQL 함수 문자열을 쓰지 않고
 * 타입세이프 projection 을 유지하기 위해서다.
 */
data class observation_category_read_row(
    val id: String,
    val observation_type_code: String,
    val observation_type_name: String,
    val category_code: String?,
    val diagnostic_service_section_code: String?,
    val observation_code: String,
    val observation_name: String,
    // 수진 id · 원 수진 id · 처방 id — 원 수진 · 처방은 발행처가 안 실었으면 null
    val encounter_id: UUID,
    val origin_encounter_id: UUID?,
    val order_id: String?,
    // 수진·처방 스냅샷 — 발행처가 안 실었으면 null
    val encounter_class: String?,
    val encounter_date: LocalDate?,
    // 진료과명 — shared.department 에서 읽는다(짝이 없으면 null)
    val department_name: String?,
    // 진료의 · 처방의 id — 이름은 shared.user_account 를 id 묶음으로 따로 읽는다(조인하지 않는다)
    val doctor_id: UUID?,
    val ordering_doctor_id: UUID?,
    // 일시 4종은 컬럼이 NOT NULL 이다(001-create-tables.xml) — 부모 테이블 값이라 조인으로 비지 않는다
    val ordered_at: Instant,
    val order_date: LocalDate?,
    val performed_at: Instant,
    val result_datetime: Instant,
    val verified_at: Instant,
    // 비정상 여부 — 적재 때 도메인이 계산해 저장한 값
    val is_abnormal: Boolean,
```

### 4-6. view_model

`K/application/queries/observation_view_model.kt:9-62`
```kotlin
// 검사분류별 결과 목록 — 검체진단/영상진단/기능진단/임상관찰 통합 표현
// 화면의 병리 탭은 `diagnostic_service_section_code`(SP · CP)에서 파생한다 — 분류 코드가 따로 있지 않다.
data class observation_category_view(
    val id: String,
    val observation_type_code: String,
    val observation_type_name: String,
    // laboratory / imaging / procedure / vital-signs / exam / survey
    val category_code: String,
    // 진단 서비스 구분(HL7 v2-0074) — 화면의 진검/병리 탭 파생 키
    val diagnostic_service_section_code: String?,
    val source_type_code: String?,          // 외부연동기록 출처(PASTA …) — 외부연동기록만
    val observation_code: String,
    val observation_name: String,
    // 수진 id — shared.encounter.encounter_id
    val encounter_id: UUID,
    // 원 수진 id — 처방이 처음 난 수진. 발행처가 안 실었으면 null
    val origin_encounter_id: UUID?,
    // 처방(오더) id — 발행처가 안 실었으면 null
    val order_id: String?,
    // 수진·처방 스냅샷 — 발행처가 안 실었으면 null(화면은 빈칸)
    val encounter_class: String?,          // O / I / E / HPC
    val encounter_date: LocalDate?,        // 진료일자
    val department_name: String?,          // 진료과
    val doctor_name: String?,              // 진료의 — shared.user_account 이름(doctor_id 로 읽는다)
    val ordering_doctor_name: String?,     // 처방의 — shared.user_account 이름(ordering_doctor_id 로 읽는다)
    // 처방일시 — 실제 저장 시각
    val ordered_at: LocalDateTime,
    // 오더일자 — 시행 지시일(시각 없음)
    val order_date: LocalDate?,
    val performed_at: LocalDateTime,
    val result_datetime: LocalDateTime,
    val verified_at: LocalDateTime,
    val result_summary: String,
    val result_unit: String?,
    val result_flag: String?,
    // 결과 그룹 — 추이 그룹핑 키(검체진단 · 임상관찰 · 외부연동기록). 영상 · 기능은 null
    val result_group_code: String?,
    // 직전 결과 대비 변화량 — 검체진단만
    val delta_value: BigDecimal?,
    // 참고범위 — 그 결과에 실려 온 값 그대로(검체진단 · 기능진단 · 임상관찰 · 외부연동기록). 결과마다 다를 수 있다. 영상은 null
    val reference_low: BigDecimal?,
    val reference_high: BigDecimal?,
    val report_status: String?,
    // 비정상 여부 — 적재 때 도메인이 계산해 저장한 값
    @get:JsonProperty("is_abnormal") val is_abnormal: Boolean,
    // 위험치 여부 — 검체진단 is_critical. 그 외 false
    @get:JsonProperty("is_critical") val is_critical: Boolean,
    // 부속 이미지 식별자 — 있으면 화면에서 "이미지 있음" 아이콘/간략 미리보기를 노출(영상/병리/기능)
    val pacs_study_uid: String? = null,
    val slide_image_url: String? = null,
    val waveform_data_url: String? = null,
    // 원천 모듈 서식(기록 문서) 식별자 — 있으면 화면이 GET /observations/{id}/record-document 로 HTML 을 연다(보고서뷰 · 상세 서식 탭)
    val record_id: String? = null,
)
```

### 4-7. shared 스키마 읽기 전용 엔티티

`K/infrastructure/shared/shared_patient.kt:10-39`
```kotlin
/**
 * `shared.patient` 읽기 전용 매핑 — 프로젝트 공유 스키마의 환자(루트 CLAUDE.md §0 DB 스키마의 shared 예외).
 *
 * 이 MS 의 Aggregate 가 아니다 — 소유자가 따로 있고 이 MS 는 **읽기만** 한다(`@Immutable` · 쓰기 경로 없음).
 * 자기 엔티티와 같은 영속성 단위에 두는 이유는 **QueryDSL 한 쿼리에서 observation 과 함께 조인**하기 위해서다.
 * 스키마는 `search_path` 가 아니라 `@Table(schema)` 로만 닿는다(앱의 search_path 는 자기 스키마 하나다).
 *
 * **이 MS 가 쓰는 컬럼만** 매핑한다 — Hibernate `validate` 는 매핑한 컬럼만 기동 때 검사하므로, 소유자가 다른 컬럼을
 * 늘리거나 바꿔도 이 MS 기동이 깨지지 않는다. 컬럼을 더 읽어야 하면 여기에 더한다.
 * ⚠️ 앱 계정에 `shared` USAGE · SELECT 권한이 없으면 validate 에서 기동이 실패한다.
 *
 * observation 과는 **id 만으로** 연결한다 — `observation.patient_id` = `patient_id`(둘 다 uuid).
 */
@Entity
@Immutable
@Table(schema = SHARED_SCHEMA, name = "patient")
class shared_patient protected constructor() {

    @Id
    @Column(name = "patient_id", nullable = false)
    var patient_id: UUID = UUID(0, 0)
        protected set

    @Column(name = "patient_name", nullable = false)
    var patient_name: String = ""
        protected set
}

/** 공유 스키마 이름 — 프로젝트 정책으로 고정된 값이다. */
const val SHARED_SCHEMA = "shared"
```

`K/infrastructure/shared/shared_department.kt:19-32`
```kotlin
@Entity
@Immutable
@Table(schema = SHARED_SCHEMA, name = "department")
class shared_department protected constructor() {

    @Id
    @Column(name = "department_id", nullable = false)
    var department_id: UUID = UUID(0, 0)
        protected set

    @Column(name = "department_name")
    var department_name: String? = null
        protected set
}
```

`K/infrastructure/shared/shared_user_account.kt:17-30`
```kotlin
@Entity
@Immutable
@Table(schema = SHARED_SCHEMA, name = "user_account")
class shared_user_account protected constructor() {

    @Id
    @Column(name = "user_account_id", nullable = false)
    var user_account_id: UUID = UUID(0, 0)
        protected set

    @Column(name = "user_account_name")
    var user_account_name: String? = null
        protected set
}
```

---

## 5. JPA 매핑

이 레포에는 `@Embedded` 사용처가 없다(`grep -rn "@Embedded\b" src/main` → 0건). VO 는 `@Embeddable` + `@ElementCollection` 으로 쓰고, `@Enumerated` 는 domain 이 아니라 infrastructure 의 `inbox_message` 에만 있다(domain 의 코드값은 전부 String 컬럼 + 상수 object).

### 5-1. Aggregate Root 전체 — `viewer_configuration` (`@Entity` · `@Table` · `@AttributeOverride` PK · `@OneToMany` · `@Version` · 테넌트/감사는 `entity` 상속)

`K/domain/aggregatesmodel/viewerconfiguration/viewer_configuration.kt:20-62`
```kotlin
// 검사결과 조회 화면의 사용자별 설정 Aggregate Root
// - 키는 (tenant_id, hospital_code, user_id) — user_id 는 토큰 sub(직원 ID). 즐겨찾기 세트와 마지막 조회 양식/기본 조회 기간을 보관
// - PK 컬럼은 viewer_configuration_id
// - 즐겨찾기 세트(favorite_set)는 자식 Entity — 별표 한 번으로 여러 세트의 소속이 함께 바뀌므로 같은 Aggregate 에 둔다
// - observation Aggregate 와 직접 참조 없음 (라이프사이클 완전 분리, Query 단에서만 활용)
@jakarta.persistence.Entity
@AttributeOverride(name = "id", column = Column(name = "viewer_configuration_id"))
@Table(name = "viewer_configuration")
class viewer_configuration protected constructor() : entity(), i_aggregate_root {

    // 유일성은 (tenant_id, hospital_code, user_id) 복합 UK 가 지킨다(changelog uk_viewer_configuration_tenant_id_hospital_code_user_id)
    @Column(name = "user_id", nullable = false)
    var user_id: UUID = UUID(0, 0)
        protected set

    // 즐겨찾기 세트 목록 — 표시 순서대로. 세트가 지워지면 그 행과 세트 항목도 함께 지워진다(orphanRemoval)
    @OneToMany(cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "viewer_configuration_id", nullable = false)
    @OrderBy("sort_order ASC")
    private val _favorite_sets: MutableList<favorite_set> = mutableListOf()

    val favorite_sets: List<favorite_set>
        get() = _favorite_sets.toList()

    // 마지막 보기 방식(넓은 폭) — 화면 어휘 그대로의 코드 (DETAIL 상세조회 / REPORT 보고서뷰 / CUMULATIVE 누적조회 / HOURLY 시간대별)
    // 정렬 · 기본 조회 기간은 저장하지 않는다 — 옛 default_period_days 컬럼은 매핑하지 않는다(삭제는 db/manual 004 를 사람이 실행)
    @Column(name = "last_view_layout_code")
    var last_view_layout_code: String? = null
        protected set

    // 낙관적 잠금 — 같은 사용자의 동시 저장은 409(global_exception_handler)
    // ⚠️ nullable 로 둔다 — 새 설정을 null 로 만들어야 Spring Data 가 persist 로 보낸다. 0 이면 merge 로 가서
    //    아직 없는 행을 「다른 트랜잭션이 지웠다」로 보고 낙관적 잠금 실패가 난다. 저장하면 Hibernate 가 0 부터 매긴다
    @Version
    @Column(name = "version", nullable = false)
    var version: Long? = null
        protected set

    constructor(user_id: UUID, scope: tenant_scope) : this() {
        this.id = UUID.randomUUID().toString()
        this.user_id = user_id
        assign_tenant(scope)
    }
```

`K/domain/aggregatesmodel/viewerconfiguration/viewer_configuration.kt:81-131`
```kotlin
    // 세트 이름 변경 — 자기 자신을 뺀 나머지와 이름이 겹치면 안 된다
    fun rename_favorite_set(favorite_set_id: String, set_name: String) {
        val target = find_favorite_set(favorite_set_id)
        val name = favorite_set.normalize_set_name(set_name)
        ensure_unique_set_name(name, except_id = target.id)
        target.rename(name)
    }

    // 세트 삭제 — 남은 세트 순서를 0부터 다시 매긴다
    fun remove_favorite_set(favorite_set_id: String) {
        val target = find_favorite_set(favorite_set_id)
        _favorite_sets.remove(target)
        renumber_favorite_sets()
    }

    // 별표 선택창 — 검사가 들어갈 세트를 정확히 favorite_set_ids 로 맞춘다 (멱등)
    // 고른 세트에는 넣고 나머지 세트에서는 뺀다. 빈 집합이면 모든 세트에서 빠진다
    fun assign_observation_to_sets(
        observation_type_code: String,
        observation_code: String,
        favorite_set_ids: Set<String>
    ) {
        val observation = favorite_observation(observation_type_code, observation_code)
        favorite_set_ids.forEach { find_favorite_set(it) } // 모르는 세트 id 가 있으면 아무것도 바꾸기 전에 404
        _favorite_sets.forEach { set ->
            if (set.id in favorite_set_ids) set.add_item(observation) else set.remove_item(observation)
        }
    }

    // 세트 순서 변경 — 세트 전부를 정확히 한 번씩 담아야 한다
    fun reorder_favorite_sets(favorite_set_ids: List<String>) {
        val current = _favorite_sets.map { it.id }.toSet()
        if (favorite_set_ids.size != current.size || favorite_set_ids.toSet() != current) {
            throw diagnostic_result_validation_exception("세트 순서에는 모든 세트를 한 번씩 담아야 합니다")
        }
        val order = favorite_set_ids.withIndex().associate { (i, id) -> id to i }
        _favorite_sets.sortBy { order.getValue(it.id) }
        renumber_favorite_sets()
    }

    // === 화면 설정 ===

    // 마지막 보기 방식 갱신 — 허용 코드 검증
    fun set_last_view_layout(layout_code: String) {
        if (layout_code !in ALLOWED_LAYOUT_CODES) {
            throw diagnostic_result_validation_exception(
                "허용되지 않은 last_view_layout_code 입니다: $layout_code"
            )
        }
        this.last_view_layout_code = layout_code
    }
```
(64-79 `add_favorite_set`, 133-161 내부/companion 은 §3-6 참조.)

### 5-2. 자식 Entity — `favorite_set` (`@ElementCollection` + `@CollectionTable` 로 VO 컬렉션)

`K/domain/aggregatesmodel/viewerconfiguration/favorite_set.kt:15-89`
```kotlin
// 즐겨찾기 세트 — viewer_configuration 의 자식 Entity
// - 사용자가 이름을 붙여 묶은 즐겨찾기 한 벌. 한 검사는 여러 세트에 들 수 있고, 한 세트 안에서는 한 번만 든다
// - 생성·변경은 Root(viewer_configuration) 메서드로만 한다 — 외부에서 직접 만들거나 저장하지 않는다
// - PK 컬럼은 favorite_set_id. 테넌트 범위는 Root 의 값을 그대로 받는다
@jakarta.persistence.Entity
@AttributeOverride(name = "id", column = Column(name = "favorite_set_id"))
@Table(name = "favorite_set")
class favorite_set protected constructor() : entity() {

    @Column(name = "set_name", nullable = false)
    var set_name: String = ""
        protected set

    // 표시 순서 (0부터)
    @Column(name = "sort_order", nullable = false)
    var sort_order: Int = 0
        protected set

    // 세트에 든 검사 — VO 컬렉션 (별도 테이블에 저장). Set 이라 세트 안 중복이 생기지 않는다
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
        name = "favorite_set_item",
        joinColumns = [JoinColumn(name = "favorite_set_id")]
    )
    private val _items: MutableSet<favorite_observation> = mutableSetOf()

    val items: Set<favorite_observation>
        get() = _items.toSet()

    internal constructor(set_name: String, sort_order: Int, scope: tenant_scope) : this() {
        this.id = UUID.randomUUID().toString()
        this.set_name = normalize_set_name(set_name)
        this.sort_order = sort_order
        assign_tenant(scope)
    }

    // === 도메인 메서드 (Root 전용) ===

    // 이름 변경 — 형식 검증(앞뒤 공백 제거 1~30자). 중복 검증은 Root 가 한다
    internal fun rename(set_name: String) {
        this.set_name = normalize_set_name(set_name)
    }

    // 검사 추가 — 이미 있으면 무시 (멱등)
    internal fun add_item(observation: favorite_observation) {
        _items.add(observation)
    }

    // 검사 제거 — 멱등
    internal fun remove_item(observation: favorite_observation) {
        _items.remove(observation)
    }

    // 표시 순서 변경
    internal fun move_to(sort_order: Int) {
        this.sort_order = sort_order
    }

    companion object {
        // 세트 이름 최대 길이 — 화면(favoriteSets.ts)과 같은 값
        const val MAX_SET_NAME_LENGTH = 30

        // 이름 형식 검증 — 앞뒤 공백을 떼고 1~30자
        fun normalize_set_name(raw: String): String {
            val name = raw.trim()
            if (name.isEmpty()) {
                throw diagnostic_result_validation_exception("세트 이름을 입력하세요")
            }
            if (name.length > MAX_SET_NAME_LENGTH) {
                throw diagnostic_result_validation_exception("세트 이름은 ${MAX_SET_NAME_LENGTH}자 이하여야 합니다")
            }
            return name
        }
    }
}
```

### 5-3. VO — `favorite_observation` (`@Embeddable` · equals/hashCode 로 동치)

`K/domain/aggregatesmodel/viewerconfiguration/favorite_observation.kt:6-36`
```kotlin
// 즐겨찾는 검사 값 객체(ValueObject) — (검사구분, 검사코드) 한 쌍
// favorite_set 의 items(ElementCollection) 요소로 임베드됨
@Embeddable
class favorite_observation protected constructor() {

    @Column(name = "observation_type_code", nullable = false)
    var observation_type_code: String = ""
        protected set

    @Column(name = "observation_code", nullable = false)
    var observation_code: String = ""
        protected set

    constructor(observation_type_code: String, observation_code: String) : this() {
        require(observation_type_code.isNotBlank()) { "observation_type_code 는 필수입니다" }
        require(observation_code.isNotBlank()) { "observation_code 는 필수입니다" }
        this.observation_type_code = observation_type_code
        this.observation_code = observation_code
    }

    // (observation_type_code, observation_code) 동치 비교
    override fun equals(other: Any?): Boolean {
        if (other == null || other !is favorite_observation) return false
        if (this === other) return true
        return this.observation_type_code == other.observation_type_code &&
            this.observation_code == other.observation_code
    }

    override fun hashCode(): Int =
        observation_type_code.hashCode() xor observation_code.hashCode()
}
```

### 5-4. JOINED 상속 Root — `observation` 헤더 · 판별자 · 자식 공통 `observation_detail`

`K/domain/aggregatesmodel/observation/observation.kt:17-48`
```kotlin
// 검사결과 통합 Aggregate Root (Abstract)
// - 검사분류(observation_type_code) 에 따라 5종의 자식 entity 중 하나로 구체화됨 (JPA JOINED 상속)
//   자식 이름·분류 코드는 **원천 BC(토픽)** 를 그대로 따른다 —
//   SPECIMEN_DIAGNOSTICS(검체진단) / DIAGNOSTIC_IMAGING(영상진단) / FUNCTIONAL_DIAGNOSTICS(기능진단) / CLINICAL_RECORD(임상관찰)
//   / EXTERNAL_RECORD(외부연동기록 — 병원 밖 외부 모듈, 출처는 자식의 source_type_code)
// - 부모 테이블(observation) 에 공통 필드 + 자식 테이블(<자식 클래스명>, 예 specimen_diagnostics_observation) 에 도메인별 필드
// - 원천 데이터는 외부 BC 이벤트로 적재되며 (Source_Aggregate_Id, Source_Event_Version) 으로 멱등 보장
// - 본 Aggregate 는 조회 전용 성격이 강함 — 자체 발행 도메인 이벤트는 없다
// - PK 컬럼은 observation_id, 테넌트 범위(tenant_id · hospital_code)는 생성 때 정해지고 적재 · 삭제가 바꾸지 않는다
// - 자식 5종은 observation_detail 을 거쳐 상속한다 — 자식 테이블도 테넌트 · 감사 컬럼을 따로 갖는다
@jakarta.persistence.Entity
@AttributeOverride(name = "id", column = Column(name = "observation_id"))
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "observation_type_code")
@Table(name = "observation")
abstract class observation protected constructor() : entity(), i_aggregate_root {

    // 환자 id — shared.patient.patient_id. 환자 정보(환자명 등)는 이 id 로 shared 를 조인해 읽는다.
    @Column(name = "patient_id", nullable = false)
    var patient_id: UUID = UUID(0, 0)
        protected set

    // 수진 id — shared.encounter.encounter_id
    @Column(name = "encounter_id", nullable = false)
    var encounter_id: UUID = UUID(0, 0)
        protected set

    // 원 수진 id — 처방이 처음 난 수진(shared.encounter.encounter_id). 결과가 적재되는 encounter_id 와 다를 수 있다(선택).
    // 컬럼은 changes/009 로 더해 물리 순서상 맨 뒤다.
    @Column(name = "origin_encounter_id")
    var origin_encounter_id: UUID? = null
        protected set
```

`K/domain/aggregatesmodel/observation/observation.kt:114-117, 148-172`
```kotlin
    // 검사분류 — JPA Discriminator 컬럼과 동일 컬럼명 사용 (insertable/updatable = false)
    @Column(name = "observation_type_code", nullable = false, insertable = false, updatable = false)
    var observation_type_code: String = ""
        protected set
```
```kotlin
    // 원천 Aggregate ID — 다른 BC(검체진단 · 영상진단 · 기능진단 · 임상관찰기록) 의 식별자
    @Column(name = "source_aggregate_id", nullable = false)
    var source_aggregate_id: String = ""
        protected set

    // 원천 이벤트 버전 — 멱등 적재용 (현재 버전 이하의 이벤트는 무시)
    @Column(name = "source_event_version", nullable = false)
    var source_event_version: Long = 0
        protected set

    // 원천 모듈 서식(기록 문서 · HTML) 식별자 — 기능진단(과내검사) · 병리 · 일부 진단검사가 싣는다(선택).
    // HTML 은 저장하지 않는다 — 화면이 열 때 GET /observations/{id}/record-document 가 원천 모듈에서 받아 중계한다.
    // 어느 분류든 실을 수 있어 부모가 갖는다. 컬럼은 changes/002 로 더해 물리 순서상 맨 뒤다.
    @Column(name = "record_id", length = 128)
    var record_id: String? = null
        protected set

    @Column(name = "is_deleted", nullable = false)
    var is_deleted: Boolean = false
        protected set

    // 비정상 여부 — 적재할 때 compute_abnormal() 로 계산해 저장한다. 조회 · 화면 · AI 프롬프트가 이 값 하나를 본다
    @Column(name = "is_abnormal", nullable = false)
    var is_abnormal: Boolean = false
        protected set
```

멱등 적재 메서드 `K/domain/aggregatesmodel/observation/observation.kt:176-221`
```kotlin
    // 원천 이벤트 적재 — (source_aggregate_id, source_event_version) 기준 멱등
    // 동일 source_aggregate_id 에서 더 낮거나 같은 버전이 들어오면 무시
    fun apply_source_event(source_aggregate_id: String, source_event_version: Long, payload: observation_payload): Boolean {
        require(source_aggregate_id.isNotBlank()) { "source_aggregate_id 는 필수입니다" }
        require(source_event_version > 0) { "source_event_version 은 1 이상이어야 합니다" }

        // 이미 더 최신 버전이 적재되어 있으면 멱등 무시
        if (this.source_aggregate_id == source_aggregate_id && source_event_version <= this.source_event_version) {
            return false
        }
        this.source_aggregate_id = source_aggregate_id
        this.source_event_version = source_event_version
        this.patient_id = payload.patient_id
        this.encounter_id = payload.encounter_id
        // 원 수진 id — 미동봉(null)이면 기존 값을 덮지 않는다(스냅샷과 같은 규칙)
        payload.origin_encounter_id?.let { this.origin_encounter_id = it }
        // 처방 id — 미동봉(null)이면 기존 값을 덮지 않는다(스냅샷과 같은 규칙)
        payload.order_id?.let { this.order_id = it }
        // 수진·처방 스냅샷 — 미동봉(null)이면 기존 값을 덮지 않는다(환자명과 같은 규칙, 구 발행처 방어)
        payload.encounter_class?.let { this.encounter_class = it }
        payload.encounter_date?.let { this.encounter_date = it }
        payload.department_id?.let { this.department_id = it }
        payload.doctor_id?.let { this.doctor_id = it }
        payload.ordering_doctor_id?.let { this.ordering_doctor_id = it }
        payload.order_date?.let { this.order_date = it }
        this.ordered_at = payload.ordered_at
        this.scheduled_at = payload.scheduled_at
        this.received_at = payload.received_at
        this.performed_at = payload.performed_at
        this.result_datetime = payload.result_datetime
        this.verified_at = payload.verified_at
        this.observation_type_name = payload.observation_type_name
        this.category_code = payload.category_code
        this.diagnostic_service_section_code = payload.diagnostic_service_section_code
        this.observation_code = payload.observation_code
        this.observation_name = payload.observation_name
        this.result_type_code = payload.result_type_code
        // 서식 식별자 — 미동봉(null)이면 기존 값을 덮지 않는다(스냅샷과 같은 규칙)
        payload.record_id?.let { this.record_id = it }
        // 자식 entity 의 detail 필드 반영은 하위 구현에 위임
        apply_detail(payload)
        // 적재 후 결과 flag 재계산 (수치형일 때만 의미 있음) → 그 flag 로 비정상 판정
        recalculate_flag()
        this.is_abnormal = compute_abnormal()
        return true
    }
```

flag · 비정상 규칙 `K/domain/aggregatesmodel/observation/observation.kt:226-274`
```kotlin
    // 원천 삭제 반영 — 적재와 같은 버전 축으로 멱등
    // 적재된 버전보다 높은 삭제만 반영한다 — 늦게 도착한 삭제(같거나 낮은 버전)가 최신 적재를 지우지 못하게.
    // 반영했으면 true (호출부가 저장 여부를 정한다)
    fun mark_deleted(source_event_version: Long): Boolean {
        require(source_event_version > 0) { "source_event_version 은 1 이상이어야 합니다" }
        if (source_event_version <= this.source_event_version) {
            return false
        }
        this.source_event_version = source_event_version
        this.is_deleted = true
        return true
    }

    // 결과 flag 재계산 — 수신 flag 가 비어 있을 때만 자식 entity 가 보유한 수치 결과 / 참조범위 / panic 범위 비교로 채운다
    // (발행측 판정이 이긴다 — 수신 flag 가 있으면 그대로 둔다)
    // 기본 구현은 no-op, 수치 결과를 갖는 자식 entity(검체진단 · 기능진단 · numeric_record_observation — 임상관찰 · 외부연동기록) 가 override 한다
    open fun recalculate_flag() {
        // 자식 entity 에서 override
    }

    // 자식이 가진 결과 flag — flag 를 갖는 자식(검체진단 · 기능진단 · 임상관찰 · 외부연동기록)이 override
    open fun current_result_flag(): String? = null

    // 위험치 여부 — 검체진단만 is_critical 로 override
    open fun has_critical_value(): Boolean = false

    // 비정상 판정 규칙 한 곳 — 위험치이거나 결과 flag 가 정상범위를 벗어났다(H · L · HH · LL · A)
    protected fun compute_abnormal(): Boolean = has_critical_value() || current_result_flag() in ABNORMAL_FLAGS

    // 수치값 → flag 변환 헬퍼 (자식 entity 에서 활용)
    // - 값이 없거나 비교할 기준(참고치 · Panic)이 하나도 없으면 null — 기준 없이 N 으로 단정하지 않는다
    // - panic 범위 벗어나면 HH / LL
    // - 참조 범위 벗어나면 H / L
    // - 그 외 N
    protected fun compute_flag(
        value: BigDecimal?,
        ref_low: BigDecimal?,
        ref_high: BigDecimal?,
        panic_low: BigDecimal?,
        panic_high: BigDecimal?
    ): String? {
        if (value == null) return null
        if (ref_low == null && ref_high == null && panic_low == null && panic_high == null) return null
        if (panic_low != null && value < panic_low) return FLAG_PANIC_LOW
        if (panic_high != null && value > panic_high) return FLAG_PANIC_HIGH
        if (ref_low != null && value < ref_low) return FLAG_LOW
        if (ref_high != null && value > ref_high) return FLAG_HIGH
        return FLAG_NORMAL
    }
```

자식 공통 `@MappedSuperclass` — 자식 테이블도 테넌트·감사 컬럼을 **따로** 갖는다 `K/domain/aggregatesmodel/observation/observation_detail.kt:9-45`
```kotlin
// observation 자식 5종(JOINED) 공통 매핑 — 자식 테이블도 테넌트 · 감사 6컬럼을 **따로** 갖는다.
// - 부모 테이블(observation)의 같은 이름 컬럼은 entity 베이스가 매핑한다. 여기는 자식 테이블 쪽 컬럼이다.
// - 부모 행과 함께 생성 · 수정되므로 값은 언제나 부모와 같다 — 부모 값을 채우는 메서드를 override 해 같은 순간 같은 값을 쓴다.
// - 프로퍼티 이름의 detail_ 접두는 부모 프로퍼티와 이름이 겹치지 않게 하려는 것뿐이다. 컬럼명은 부모와 같다.
// - 테이블이 없는 매핑 클래스다(@MappedSuperclass) — 자식 클래스가 자기 테이블에 이 컬럼들을 갖는다.
@MappedSuperclass
abstract class observation_detail protected constructor() : observation() {

    @Column(name = "tenant_id", nullable = false, updatable = false, length = 64)
    var detail_tenant_id: String = tenant_scope.INVALID_DATA
        protected set

    @Column(name = "hospital_code", nullable = false, updatable = false, length = 64)
    var detail_hospital_code: String = tenant_scope.INVALID_DATA
        protected set

    @Column(name = "created_at", nullable = false, updatable = false)
    var detail_created_at: Instant = Instant.EPOCH
        protected set

    @Column(name = "created_by", updatable = false)
    var detail_created_by: UUID? = null
        protected set

    @Column(name = "updated_at", nullable = false)
    var detail_updated_at: Instant = Instant.EPOCH
        protected set

    @Column(name = "updated_by")
    var detail_updated_by: UUID? = null
        protected set

    override fun assign_tenant(scope: tenant_scope) {
        super.assign_tenant(scope)
        this.detail_tenant_id = scope.tenant_id
        this.detail_hospital_code = scope.hospital_code
    }
```

JOINED 자식 1개 — `specimen_diagnostics_observation` 헤더 · 생성자 · override `K/domain/aggregatesmodel/observation/specimen_diagnostics_observation.kt:11-29, 167-170, 208-221`
```kotlin
// 검체진단 결과 entity — observation 의 자식 (JOINED 상속)
// - 검체(Specimen) 기반 결과 한 벌이다. 진단검사와 병리가 **한 Entity** 인 이유는 원천 BC 가
//   검체진단 하나이기 때문이다(토픽은 `laboratory-result` · `pathology-result` 둘) — 자식 Entity 는 원천 BC 와 1:1 로 맞춘다.
// - 진검/병리 구분은 부모의 `diagnostic_service_section_code` 가 한다(SP · CP = 병리).
//   화면의 병리 탭도 이 값에서 파생한다 — 분류 코드를 따로 쪼개지 않는다.
// - PK 컬럼은 specimen_diagnostics_observation_id — 부모(observation.observation_id) 와 같은 값을 join key 로 공유
@jakarta.persistence.Entity
@DiscriminatorValue(specimen_diagnostics_observation.OBSERVATION_TYPE_CODE)
@PrimaryKeyJoinColumn(name = "specimen_diagnostics_observation_id", referencedColumnName = "observation_id")
@Table(name = "specimen_diagnostics_observation")
class specimen_diagnostics_observation protected constructor() : observation_detail() {

    // ── 수치·문자 결과 (진단검사) ────────────────────────────────────────────
    // 병리는 diagnosis 가 본문이라 비어 있다 — 컬럼이 NOT NULL 이라 null 이 아니라 빈 문자열이다
    // (NOT NULL 해제는 파괴적 change 로 막혀 있어 자동 적용 경로에 두지 않는다)
    @Column(name = "result_value", nullable = false, columnDefinition = "TEXT")
    var result_value: String = ""
        protected set

    @Column(name = "result_numeric_value", precision = 19, scale = 4)
    var result_numeric_value: BigDecimal? = null
        protected set
```
```kotlin
    constructor(id: String, scope: tenant_scope) : this() {
        initialize_identity(id, OBSERVATION_TYPE_CODE, scope)
    }
```
```kotlin
    override fun current_result_flag(): String? = result_flag

    override fun has_critical_value(): Boolean = is_critical

    // 수신 flag 가 비어 있을 때만 참조/Panic 범위 비교로 flag 를 채운다 (발행측 판정 우선 · 병리는 수치가 없어 no-op 가 된다)
    override fun recalculate_flag() {
        if (!result_flag.isNullOrBlank()) return
        this.result_flag = compute_flag(result_numeric_value, reference_low, reference_high, panic_low, panic_high)
    }

    companion object {
        const val OBSERVATION_TYPE_CODE: String = "SPECIMEN_DIAGNOSTICS"
    }
}
```

분류별 분기 한 곳 `K/domain/aggregatesmodel/observation/observation_factory.kt:14-22`
```kotlin
fun new_observation(observation_type_code: String, id: String, scope: tenant_scope): observation =
    when (observation_type_code) {
        specimen_diagnostics_observation.OBSERVATION_TYPE_CODE -> specimen_diagnostics_observation(id, scope)
        diagnostic_imaging_observation.OBSERVATION_TYPE_CODE -> diagnostic_imaging_observation(id, scope)
        functional_diagnostics_observation.OBSERVATION_TYPE_CODE -> functional_diagnostics_observation(id, scope)
        clinical_record_observation.OBSERVATION_TYPE_CODE -> clinical_record_observation(id, scope)
        external_record_observation.OBSERVATION_TYPE_CODE -> external_record_observation(id, scope)
        else -> error("알 수 없는 검사분류입니다: $observation_type_code")
    }
```

### 5-5. enum — `inbox_status` + `@Enumerated(EnumType.STRING)` 사용처, 그리고 API 파라미터 enum `observation_date_basis`

`K/infrastructure/inbox/inbox_message.kt:15-37`
```kotlin
/** inbox 행 상태 — HOLD 는 상태가 아니라 집기 조건(앞선 행이 끝나지(PROCESSED · TERMINATED) 않음)이다. */
enum class inbox_status {
    /** 처리 대기 — `next_retry_at` 이 지나면 집힌다(재시도 대기 포함). */
    PENDING,

    /** 한 파드가 집어 처리 중 — `locked_until` 이 지나면 sweeper 가 PENDING 으로 되돌린다. */
    PROCESSING,

    /** 처리 완료 — 같은 키의 다음 행이 풀린다. */
    PROCESSED,

    /** 처리 포기(영구 실패 · 재시도 상한) — 같은 키의 뒤 행은 멈춘 채 남는다. */
    FAILED,

    /** 강제 종료 — 사람이 반영 없이 처리를 끝냈다(실패 · 대기 행 적체 방지). 끝난 행이라 같은 키의 뒤 행이 풀린다. */
    TERMINATED,
    ;

    companion object {
        /** 끝난 상태 — 같은 키의 뒤 행을 막지 않는다(집기 조건 · 순서 대기 판정 · 키 상태가 같은 목록을 쓴다). */
        val FINISHED: List<inbox_status> = listOf(PROCESSED, TERMINATED)
    }
}
```

`K/infrastructure/inbox/inbox_message.kt:54-62, 101-109`
```kotlin
@Entity
@Table(name = "inbox_message")
class inbox_message protected constructor() {

    /** 도착 순번(seq) — DB IDENTITY. 저장 전에는 null. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "inbox_message_id", nullable = false)
    var inbox_message_id: Long? = null
```
```kotlin
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "headers", columnDefinition = "jsonb")
    var headers: String? = null
        protected set

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    var status: inbox_status = inbox_status.PENDING
        protected set
```

`K/application/queries/observation_date_basis.kt:3-24`
```kotlin
// 조회기간 날짜기준 — 검사결과 목록의 기간을 결과의 어느 날짜로 거르는지. 화면 「날짜기준」 Select 와 같은 네 값이다.
// API 로 오가는 값은 code(소문자)다. 그 날짜가 비어 있을 때 대신 보는 칸은 조회 구현(observation_query_dsl)이 갖는다:
//   ORDER     오더일 — order_date(날짜), 비어 있으면 ordered_at(처방일시)
//   ENCOUNTER 수진일 — encounter_date(날짜), 비어 있으면 performed_at(검사일)
//   PERFORMED 검사일 — performed_at
//   RESULT    결과일 — result_datetime (API 기본값)
enum class observation_date_basis(val code: String) {
    ORDER("order"),
    ENCOUNTER("encounter"),
    PERFORMED("performed"),
    RESULT("result");

    companion object {
        // 요청 값 → 날짜기준. 비어 있으면 기본(RESULT), 모르는 값은 IllegalArgumentException(400)
        fun of(code: String?): observation_date_basis {
            val c = code?.trim()?.lowercase().orEmpty()
            if (c.isEmpty()) return RESULT
            return entries.firstOrNull { it.code == c }
                ?: throw IllegalArgumentException("date_basis 는 order · encounter · performed · result 중 하나여야 합니다")
        }
    }
}
```

domain 의 코드값은 enum 이 아니라 상수 object 다 `K/domain/aggregatesmodel/observation/observation_category_code.kt:3-11`
```kotlin
// 분류 코드(`observation.category_code`) 기본값 — 발행측이 `categoryCode` 를 싣지 않을 때 검사분류에서 파생하는 네 값.
// 값 체계는 FHIR observation-category 를 차용한다. 열린 값이라 enum 이 아니다 — 외부연동기록의 구분코드(`pasta-glucose` …)는
// 발행측이 정하고 여기 없다.
object observation_category_code {
    const val LABORATORY: String = "laboratory"
    const val IMAGING: String = "imaging"
    const val PROCEDURE: String = "procedure"
    const val VITAL_SIGNS: String = "vital-signs"
}
```

---

## 6. 이벤트 수신/발행 (common-eventbus · 수신 inbox)

이 MS 는 **수신만** 한다 — 발행 route 가 없다(`topic_registry` 주석). 흐름은 다음 한 줄이다.

```
Kafka → EventBus.subscribeRaw(토픽별) → inbox_receiver.receive() → inbox_message 적재(T0 커밋)
  → [AFTER_COMMIT] inbox_drain_trigger → inbox_drain_executor(별도 스레드) → inbox_processor.drain(key)
  → T1 claim_next → T2 process(inbox_dispatcher.dispatch → observation_result_handler.handle) → PROCESSED
  → (실패) T3 mark_retry(REQUIRES_NEW) → PENDING(재시도) 또는 FAILED → [AFTER_COMMIT] inbox_failure_alert
  → (결과 반영 시) observation_results_changed → [AFTER_COMMIT] analysis_review_target_listener
```

### 6.1 구독 등록 — `@PostConstruct` 에서 토픽마다 `subscribeRaw`

`K/infrastructure/eventbus/event_bus_subscription_initializer.kt:1-40`
```kotlin
package com.projectx.diagnosticresult.infrastructure.eventbus

import com.projectx.common.eventbus.core.EventBus
import com.projectx.diagnosticresult.infrastructure.eventbus.received_result_event.Companion.RECEIVED_RESULT_EVENTS
import com.projectx.diagnosticresult.infrastructure.inbox.inbox_receiver
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.annotation.PostConstruct
import org.springframework.stereotype.Component

private val logger = KotlinLogging.logger {}

/**
 * 부팅 시 EventBus 에 수신 등록한다. `topic_registry` 와 짝이다 — 둘 중 하나만 있으면 수신되지 않는다.
 *
 * **수신은 inbox 에 적재만 한다**(classdiagram §1.8) — 토픽마다 `subscribeRaw`(catch-all · 바인딩 없음)로 원문을 받아
 * `inbox_receiver` 가 `inbox_message` 에 넣는다. 핸들러 실행 · 재시도는 inbox 처리(`inbox_processor`)가 따로 한다.
 * 그래서 본문이 깨졌거나 처리가 실패한 메시지도 DLT 로 가지 않고 inbox 에 남는다.
 */
@Component
class event_bus_subscription_initializer(
    private val event_bus: EventBus,
    private val receiver: inbox_receiver,
) {

    @PostConstruct
    fun subscribe() {
        RECEIVED_RESULT_EVENTS.forEach { subscribe(it) }
    }

    /**
     * 구독 1건 등록. **실패해도 부팅을 막지 않는다** — 브로커 주소 오류·DNS 실패로 컨슈머 생성이 깨지면
     * 예외가 `@PostConstruct` 를 타고 올라가 컨텍스트가 죽고, 검사결과 조회 API 까지 전부 사라진다
     * (Pod NotReady → 라우팅이 upstream connect error 111). 수신은 부가기능이므로 ERROR 로그만 남기고 진행한다.
     * ⚠️ 그래서 "이벤트가 안 들어온다" 는 이 ERROR 로그로만 드러난다 — 기동 로그를 확인한다.
     */
    private fun subscribe(event: received_result_event) {
        runCatching { event_bus.subscribeRaw(setOf(event.event_type)) { raw -> receiver.receive(event.topic, raw) } }
            .onFailure { logger.error(it) { "이벤트 구독 등록 실패 — 이 토픽은 수신되지 않는다: ${event.topic}" } }
    }
}
```

### 6.2 수신 토픽 목록 — 단일 정본 `RECEIVED_RESULT_EVENTS`

route 등록 · 구독 · inbox 분배가 모두 이 한 목록을 읽는다. 토픽을 늘리거나 줄일 때 **여기만** 고친다.

`K/infrastructure/eventbus/received_result_event.kt:12-63`
```kotlin
/**
 * 수신 토픽 1건 — 토픽(namespace 뒤 이벤트명) · 파티션 키 필드 · 이벤트 클래스.
 *
 * 이벤트 1종 = 토픽 1개 = 이벤트 클래스 1개다. route 등록(`topic_registry`) · 구독(`event_bus_subscription_initializer`) ·
 * inbox 분배(`inbox_dispatcher`)가 모두 [RECEIVED_RESULT_EVENTS] 한 목록을 읽는다 — 토픽을 더하거나 뺄 때 여기만 고친다.
 */
data class received_result_event(
    val topic: String,
    val partition_key_field: String,
    val event_type: KClass<out observation_result_integration_event>,
) {
    companion object {
        val RECEIVED_RESULT_EVENTS: List<received_result_event> = listOf(
            // 영상진단 — 영상검사 결과 저장
            received_result_event(
                diagnostic_imaging_result_integration_event.TOPIC,
                diagnostic_imaging_result_integration_event.PARTITION_KEY_FIELD,
                diagnostic_imaging_result_integration_event::class,
            ),
            // 기능진단 — 과내기능검사 결과 저장
            received_result_event(
                functional_diagnostics_result_integration_event.TOPIC,
                functional_diagnostics_result_integration_event.PARTITION_KEY_FIELD,
                functional_diagnostics_result_integration_event::class,
            ),
            // 검체진단 — 진검 결과 저장
            received_result_event(
                laboratory_result_integration_event.TOPIC,
                laboratory_result_integration_event.PARTITION_KEY_FIELD,
                laboratory_result_integration_event::class,
            ),
            // 검체진단 — 병리 결과 저장
            received_result_event(
                pathology_result_integration_event.TOPIC,
                pathology_result_integration_event.PARTITION_KEY_FIELD,
                pathology_result_integration_event::class,
            ),
            // 간호기록 — 관찰기록 그룹 스냅샷(키 · 몸무게 · 혈압 등)
            received_result_event(
                observation_record_group_snapshot_integration_event.TOPIC,
                observation_record_group_snapshot_integration_event.PARTITION_KEY_FIELD,
                observation_record_group_snapshot_integration_event::class,
            ),
            // 외부 연동 모듈 — 외부연동기록(PASTA 등, 출처 sourceTypeCode)
            received_result_event(
                external_record_integration_event.TOPIC,
                external_record_integration_event.PARTITION_KEY_FIELD,
                external_record_integration_event::class,
            ),
        )
    }
}
```

### 6.3 토픽 설정 ① — `topic_registry` (`@Configuration` + `EventRouter` `@Bean`)

`K/infrastructure/eventbus/topic_registry.kt:10-57`
```kotlin
/**
 * 이벤트 ↔ 토픽 라우팅.
 *
 * **토픽은 `{환경}.{테넌트id}.{이벤트명}` 이다.** 앞의 두 마디(`local.chabd` · `dev.chabd`)는
 * `common.event-bus.namespace` 가 붙이므로 `kafkaTopic` 에는 **이벤트명** 만 적는다(토픽명에 발행모듈명을 넣지 않는다):
 *   - `dev.chabd.laboratory-result`
 *
 * ⚠️ **행위(생성 · 수정 · 삭제)는 토픽·messageType 이 아니라 payload 의 `eventType` 이 구분한다** —
 * 이벤트 1종당 토픽 1개 · 이벤트 클래스 1개다. 한 이벤트의 세 행위가 같은 토픽으로
 * 흘러야 `partitionKeyField` 기준 순서가 보장된다. 행위별로 쪼개면 같은 검사건의 삭제가 생성보다 먼저
 * 도착할 수 있다.
 *
 * `partitionKeyField` 는 토픽마다 발행측 식별자(`…ProcessId` · `…ResultId`)다 — 라이브러리(`MapEventRouter`)가
 * 부팅 때 그 이름의 속성이 이벤트 클래스에 있는지 검사하므로 값은 각 클래스의 `PARTITION_KEY_FIELD` 에서 읽는다.
 *
 * 수신 토픽 목록은 [received_result_event.RECEIVED_RESULT_EVENTS] 한 곳이다 — 구독 · inbox 분배와 같은 목록을 읽는다.
 *
 * 이 MS 는 **수신만** 한다 — 발행 route 가 없다.
 * Rabbit 값은 provider 전환 대비 자리만 채운다(현재 provider=kafka).
 */
@Configuration
class topic_registry {

    @Bean
    fun event_router(): EventRouter = MapEventRouter(
        RECEIVED_RESULT_EVENTS.associate { it.event_type to result_route(it.topic, it.partition_key_field) },
    )

    /**
     * 한 이벤트의 route — 이벤트 1종 = 토픽 1개 = 이벤트 클래스 1개다.
     * 행위별로 토픽·group·partition key 가 어긋날 자리를 애초에 만들지 않는다.
     */
    private fun result_route(topic: String, partition_key_field: String): EventRoute =
        EventRoute(
            rabbitExchange = "$topic-exchange",
            rabbitRoutingKey = topic,
            rabbitQueue = "$CONSUMER_PREFIX.$topic",
            kafkaTopic = topic,
            // 같은 검사건(발행측 식별자)의 생성→수정→삭제 순서를 한 파티션에 묶는다.
            partitionKeyField = partition_key_field,
            consumerGroupId = "$CONSUMER_PREFIX.$topic",
        )

    private companion object {
        /** consumer group 접두 — 뒤에 namespace 가 다시 붙어 `dev.chabd.diagnostic-result.<이벤트명>` 이 된다. */
        const val CONSUMER_PREFIX = "diagnostic-result"
    }
}
```

### 6.4 토픽 설정 ② — `application.yml` 의 kafka / event-bus

`R/application.yml:58-98`
```yaml
  # Kafka — 외부 BC(검체진단 · 영상검사 · 과내기능검사 · 임상관찰기록) integration event 수신용.
  #   로컬 기본값은 **로컬 브로커**(워크스페이스 루트 docker-compose.yml · PLAINTEXT :9092)다 — 발행측
  #   x-ancillary-main 과 같은 곳을 봐야 이벤트가 이어진다. 준비는 루트에서 `docker compose up -d`.
  #   dev(k8s)는 application-dev.yml 이 GCP Managed Kafka(${KAFKA_BOOTSTRAP_SERVERS} + SASL)로 따로 준다.
  kafka:
    bootstrap-servers: ${SPRING_KAFKA_BOOTSTRAP_SERVERS:localhost:9092}
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.apache.kafka.common.serialization.StringSerializer
    consumer:
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      auto-offset-reset: earliest
    listener:
      # 브로커/토픽이 없는 상태로 부팅해도 fail-fast 하지 않는다(수신은 부가기능 — 조회 API 를 막지 않는다).
      missing-topics-fatal: false
    # ⚠️ KafkaAdmin 이 브로커에 못 닿으면 **기동을 붙잡는다**(실측 12초 → 49초).
    #   수신 전용이라 토픽을 만들 일이 없으므로 짧게 묶어 브로커 부재가 기동을 지연시키지 않게 한다.
    admin:
      fail-fast: false
      operation-timeout: 3s
      properties:
        request.timeout.ms: 3000
        default.api.timeout.ms: 3000

# 이벤트버스 네임스페이스 — 토픽 이름의 앞 두 마디다.
#   **토픽 = `{환경}.{테넌트id}.{이벤트명}`** (예 `local.chabd.laboratory-result`).
#   여기에는 `{환경}.{테넌트id}` 까지만 두고, 이벤트명은 topic_registry 의 kafkaTopic 이 갖는다.
#   consumer group 에도 같은 prefix 가 붙는다(환경·병원 격리축).
#   기본값은 로컬용이고, dev 값은 application-dev.yml 의 kafka_namespace 가 덮는다.
#   ⚠️ **전 BC 가 같은 값이어야 한다** — 한 BC 만 다르면 브로커·앱이 전부 정상인데
#      발행/구독이 서로 다른 토픽을 보게 돼 이벤트만 조용히 끊긴다.
kafka_namespace: ${KAFKA_NAMESPACE:local.chabd}

common:
  event-bus:
    namespace: ${kafka_namespace}
    # ⚠️ common-eventbus-kt 0.1.18 은 기본으로 namespace 앞에 환경(profile)을 또 붙인다 — namespace 가 이미
    #    `{환경}.{병원}` 이라 켜 두면 토픽이 `dev.dev.chabd.…` 가 되어 발행측과 갈라진다. 끈다.
    env-prefix-enabled: false
    provider: ${PROJECTX_EVENT_BUS_PROVIDER:kafka}
```

dev 프로파일은 namespace 와 브로커 주소만 바꾼다 — `R/application-dev.yml:7-16`, `:33-47`
```yaml
# 이벤트버스 네임스페이스 — 토픽 `{환경}.{테넌트id}.{이벤트명}` 의 앞 두 마디
# (예: dev.chabd.laboratory-result).
# ⚠️ **전 BC(pharmacy 제외)가 같은 값이어야 한다** — 한 BC 만 다르면 브로커·앱이 전부 정상인데
#    발행/구독이 서로 다른 토픽을 보게 돼 이벤트만 조용히 끊긴다. 값을 바꿀 때는 전 BC 를 함께 바꾼다.
kafka_namespace: ${KAFKA_NAMESPACE:dev.chabd}

common:
  event-bus:
    # ⚠️ namespace 가 이미 `{환경}.{병원}` 이다 — 라이브러리(0.1.18)의 환경 접두를 끄지 않으면 토픽이 `dev.dev.chabd.…` 가 된다.
    env-prefix-enabled: false
```
```yaml
  kafka:
    # 개발기 브로커 = **GCP Managed Kafka**(dev-ehr). 옛 사내 공용 kafka(dev-sre-test-app-kafka·10.250.1.22x)는 폐용이다.
    # ⚠️ 관리형 Kafka 는 SASL_SSL + OAUTHBEARER 만 허용한다 — 인증 4종(security.protocol · sasl.mechanism ·
    #    sasl.jaas.config · sasl.login.callback.handler.class)은 플랫폼 ConfigMap `x-ehr-foundation-be-kafka` 가
    #    env 로 넣어 준다(배포 values 의 `env: - configMapRef`). **여기에 적지 않는다.**
    #    토큰은 Workload Identity 로 받는다 — values 의 serviceAccount·gsaEmail + terraform KSA 등록이 선행돼야 한다.
# dev Kafka — GCP Managed Service for Apache Kafka(SASL_SSL + OAUTHBEARER/GCP IAM).
#   주소와 SASL 4종은 배포레포의 공용 configMap `x-ehr-foundation-be-kafka` 가 준다
#   (SPRING_KAFKA_SECURITY_PROTOCOL · SPRING_KAFKA_PROPERTIES_SASL_* 는 relaxed binding 으로 그대로 흡수된다 —
#    주소만 키 이름이 달라 여기서 이어 준다). 로컬 기본값은 application.yml 이 갖는다.
#   ⚠️ **기본값을 두지 않는다** — 값이 안 오면 즉시 부팅 실패로 드러난다. 기본값을 두면 dev 파드가
#      조용히 다른 브로커(폐용 사내 클러스터)를 보고, 브로커·앱이 전부 정상인데 이벤트만 끊긴다.
#   ⚠️ 인증 핸들러 의존성(managed-kafka-auth-login-handler)과 KSA↔GCP SA Workload Identity 바인딩이
#      함께 있어야 한다 — 셋 중 하나만 빠지면 기동 또는 발행/구독이 깨진다.
    bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS}
```

### 6.5 수신 payload 클래스 — 상속 계층

```
integration_event (abstract 표식)                              K/application/integrationevents/integration_event.kt
└─ observation_result_integration_event (abstract · 단일 스키마 · 90여 var)   …/events/observation_result_integration_event.kt
   ├─ laboratory_result_integration_event        @MessageType("LaboratoryResult")                 TOPIC=laboratory-result
   ├─ pathology_result_integration_event         @MessageType("PathologyResult")                  TOPIC=pathology-result   (topic_section_code="SP")
   ├─ diagnostic_imaging_result_integration_event @MessageType("DiagnosticImagingResult")         TOPIC=diagnostic-imaging-result
   ├─ functional_diagnostics_result_integration_event                                            TOPIC=functional-diagnostics-result
   ├─ observation_record_group_snapshot_integration_event @MessageType("ObservationRecordGroupSnapshotEvent") TOPIC=observation-record (groupVersion 으로 버전 덮음)
   └─ external_record_integration_event                                                         (PASTA 등 외부연동기록)
```

베이스 표식 `K/application/integrationevents/integration_event.kt:1-11`
```kotlin
package com.projectx.diagnosticresult.application.integrationevents

/**
 * 통합 이벤트(integration event) payload 의 공통 표식.
 *
 * 메타(messageId · messageType · schemaVersion · occurredAt …)는 봉투(MessageEnvelope)가 갖고,
 * 이 계층의 클래스는 **payload 스키마만** 표현한다.
 *
 * payload 필드는 cross-BC JSON 정합을 위해 **camelCase** 다(전 영역 snake_case 규칙의 유일한 예외).
 */
abstract class integration_event
```

단일 스키마 베이스(머리 부분 + 공통 필드) `K/application/integrationevents/events/observation_result_integration_event.kt:11-115`
```kotlin
/**
 * 외부 BC 가 발행하는 **검사결과 수신 payload 의 단일 스키마**.
 *
 * 이 BC 는 검체진단 · 영상진단 · 기능진단 · 임상관찰기록 · 외부연동기록의 결과를 받아 하나의 `observation` 트리에
 * 적재한다. 원천마다 채우는 필드 묶음만 다를 뿐 **필드 이름은 전부 서로 다르고 의미가 1:1** 이라
 * (`resultValue`(검체) · `imgFindings`(영상) · `vitalResultValue`(임상관찰) …), 스키마를 원천별로
 * 복제하지 않고 여기 한 벌로 둔다. 해당 원천이 안 싣는 필드는 그대로 null 로 남는다.
 *
 * ⚠️ **payload 필드 이름은 cross-BC 계약이라 분류 명칭과 별개다** — 분류는 `observationTypeCode` 가 **싣고 오는 값**이 정한다.
 * 필드명을 이쪽만 바꾸면 발행측과 어긋나 수신분이 통째로 FAILED 로 남는다(`vitalResultValue` 가 임상관찰 묶음인 이유).
 *
 * 그래서 토픽별 클래스(아래 서브클래스들)는 **애노테이션만 갖는 한 줄짜리**가 되고,
 * payload → 도메인 변환도 `observation_payload_mapper` **한 벌**로 끝난다.
 *
 * **행위(생성 · 수정 · 삭제)는 이 스키마의 [eventType] 필드가 구분한다** — 토픽당 이벤트 클래스도
 * `@MessageType` 도 **하나**다. 토픽 · `partitionKeyField` · consumer group 이 하나인 것과 같은 이유로,
 * 한 검사건의 세 행위가 갈라질 자리를 아예 만들지 않는다.
 *
 * 수신 전용이라 봉투는 발행측이 만든다 — 수신은 토픽 단위 inbox(classdiagram §1.8)라 **토픽이 이 스키마의 서브클래스를
 * 고르고**, 서브클래스의 `@MessageType` 은 발행측 messageType 과 맞춰 두는 계약 표기다(라이브러리 route 등록에 필요해 뗄 수 없다).
 *
 * setter 바인딩(무인자 생성자 + var)을 쓰는 이유: 서브클래스가 90여 개 생성자 인자를 다시 나열하지 않게 하려고.
 * 필수값 검증은 생성자가 아니라 `observation_payload_mapper` 가 한 곳에서 한다.
 *
 * **모르는 필드는 무시한다** — 계약에서 뺀 필드(`departmentName` · `doctorName` · `orderingDoctorName` · `reporter` · `diagnosticServiceSectionCode`)나
 * 다른 토픽 전용 필드를 발행측이 계속 실어도 메시지를 실패로 버리지 않는다(PL + Conformist 수신측의 관용).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
abstract class observation_result_integration_event : integration_event() {

    /** 행위 상수 — 문자열 비교를 한 곳에 모은다. */
    companion object {
        const val CREATED = "created"
        const val MODIFIED = "modified"
        const val DELETED = "deleted"
    }


    /**
     * 이 토픽이 실어 나를 수 있는 검사분류 코드. 계약 위반(다른 분류가 흘러옴)을 수신측에서 잡는다.
     * 봉투 payload 가 아니므로 직렬화 대상에서 제외한다.
     */
    @get:JsonIgnore
    abstract val allowed_observation_type_codes: Set<String>

    /**
     * 진단 서비스 구분(HL7 v2-0074) — payload 로 받지 않고 **토픽이 정한다**. 토픽이 구분을 말해 주는 경우(병리)에만 준다.
     * 진검 세부 구분(CH · HM · MB)은 지금 쓰지 않는다 — 필요해지면 발행측에 요청해 다시 받는다.
     * 봉투 payload 가 아니므로 직렬화 대상에서 제외한다.
     */
    @get:JsonIgnore
    open val topic_section_code: String? = null

    /**
     * 적재 멱등 비교에 쓰는 버전 — 기본은 `sourceEventVersion` 이다. 원천이 다른 버전 축을 갖는 토픽
     * (간호기록 그룹 스냅샷의 `groupVersion`)만 덮는다. 게터가 아니라 함수라 직렬화 대상이 아니다.
     */
    open fun resolved_event_version(): Long = sourceEventVersion

    // ── 행위 ────────────────────────────────────────────────────────────────
    /**
     * `created` · `modified` · `deleted` — 이 메시지가 뜻하는 행위. 대소문자는 구분하지 않는다.
     *
     * **값이 없거나 모르는 값이면 적재(upsert)로 본다.** 삭제는 `deleted` 가 명시됐을 때만 한다 —
     * 폴백을 적재 쪽에 두면 발행측 미반영이 "안 지워짐"으로 끝나지만, 삭제 쪽에 두면 멀쩡한 결과가
     * 사라진다. 되돌릴 수 없는 쪽으로 기울이지 않는다.
     */
    var eventType: String? = null

    // ── 멱등 키 ──────────────────────────────────────────────────────────────
    /**
     * 원천 BC 의 Aggregate 식별자. (sourceAggregateId, sourceEventVersion) 이 적재 멱등 키다.
     * 파티션 키는 이것이 아니라 **토픽별 식별자**(서브클래스의 `…ProcessId` · `…ResultId`)다 — 발행측은 둘 다 싣는다.
     */
    var sourceAggregateId: String? = null
    var sourceEventVersion: Long = 0

    // ── 공통 ────────────────────────────────────────────────────────────────
    /**
     * SPECIMEN_DIAGNOSTICS / DIAGNOSTIC_IMAGING / FUNCTIONAL_DIAGNOSTICS / CLINICAL_RECORD / EXTERNAL_RECORD.
     * 자식 entity 판별 키이며 값은 **원천 BC(토픽) 이름**과 같다.
     */
    var observationTypeCode: String? = null
    var observationTypeName: String? = null
    /**
     * 분류 코드 — laboratory / imaging / procedure / vital-signs / exam / survey.
     * 미동봉이면 분류 코드에서 기본값을 파생한다(`observation_payload_mapper`).
     */
    var categoryCode: String? = null
    var patientId: String? = null
    /** 환자명 — 계약이라 받지만 **적재하지 않는다**. 환자명은 shared.patient 를 patientId 로 조인해 읽는다. */
    var patientName: String = ""
    var encounterId: String? = null
    /**
     * 원 수진 id — 선택. 처방이 처음 난 수진이다(결과가 적재되는 `encounterId` 와 다를 수 있다). shared 수진 uuid 문자열.
     * UUID 가 아니면 경고 후 null, 비어 있으면 미동봉으로 보고 기존 값을 덮지 않는다. snake_case 표기로 실어 와도 받는다.
     */
    @JsonAlias("origin_encounter_id")
    var originEncounterId: String? = null
    /**
     * 처방(오더) id — 선택. 결과를 낳은 처방의 원천 식별자다(이름은 처방 이벤트 `DiagnosticImagingOrderCreated` 등의 `orderId` 와 같다).
     * 문자열 그대로 적재한다 — 빈 문자열은 미동봉으로 보고 기존 값을 덮지 않는다. snake_case 표기로 실어 와도 받는다.
     */
    @JsonAlias("order_id")
    var orderId: String? = null
```
(이어서 `:116-245` 는 수진·처방 스냅샷 · 시각(`OffsetDateTime`) · 검체진단 · 병리 · 영상진단 · 기능진단 · 임상관찰/외부연동 필드 묶음. 필드명은 전부 camelCase 계약.)

토픽별 서브클래스는 **애노테이션 3종 + 파티션 키 필드 + TOPIC 상수**만 갖는다 — `K/application/integrationevents/events/laboratory_result_integration_event.kt:21-38`
```kotlin
@MessageType("LaboratoryResult")
@SchemaVersion("1")
@Aggregate(type = "LaboratoryResult")
class laboratory_result_integration_event : observation_result_integration_event() {
    override val allowed_observation_type_codes =
        setOf(specimen_diagnostics_observation.OBSERVATION_TYPE_CODE)

    /** 발행측 진검결과 식별자 — **파티션 키**다(적재 멱등 키는 공통 `sourceAggregateId`). */
    var laboratoryResultId: String? = null

    companion object {
        /** 토픽 `{환경}.{테넌트id}.laboratory-result` 의 **namespace 뒤 마디**(이벤트명). */
        const val TOPIC = "laboratory-result"

        /** [laboratoryResultId] 의 payload 필드명 — `topic_registry` 의 partitionKeyField. */
        const val PARTITION_KEY_FIELD = "laboratoryResultId"
    }
}
```

버전 축을 덮는 변형 — `K/application/integrationevents/events/observation_record_group_snapshot_integration_event.kt:22-44`
```kotlin
@MessageType("ObservationRecordGroupSnapshotEvent")
@SchemaVersion("1")
@Aggregate(type = "ObservationRecordGroup")
class observation_record_group_snapshot_integration_event : observation_result_integration_event() {
    override val allowed_observation_type_codes = setOf(clinical_record_observation.OBSERVATION_TYPE_CODE)

    /** 기록 그룹 식별자 — **파티션 키**다(적재 멱등 키는 항목별 `sourceAggregateId`). */
    var recordingGroupId: String? = null

    /** 같은 그룹 안에서 단조 증가하는 스냅샷 버전. 없으면 `sourceEventVersion` 을 쓴다. */
    var groupVersion: Long? = null

    /** 그룹 스냅샷은 `groupVersion` 이 순서의 기준이다 — 뒤의 스냅샷이 앞의 것을 덮는다. */
    override fun resolved_event_version(): Long = groupVersion ?: sourceEventVersion

    companion object {
        /** 토픽 `{환경}.{테넌트id}.observation-record` 의 **namespace 뒤 마디**(이벤트명). */
        const val TOPIC = "observation-record"

        /** [recordingGroupId] 의 payload 필드명 — `topic_registry` 의 partitionKeyField. */
        const val PARTITION_KEY_FIELD = "recordingGroupId"
    }
}
```

토픽이 값을 정하는 변형(병리 → `SP`) — `K/application/integrationevents/events/pathology_result_integration_event.kt:23-46`
```kotlin
@MessageType("PathologyResult")
@SchemaVersion("1")
@Aggregate(type = "PathologyResult")
class pathology_result_integration_event : observation_result_integration_event() {
    override val allowed_observation_type_codes =
        setOf(specimen_diagnostics_observation.OBSERVATION_TYPE_CODE)

    /** 병리 토픽이라 진단 서비스 구분을 병리(HL7 v2-0074 `SP` — 외과병리)로 적재한다. */
    @get:JsonIgnore
    override val topic_section_code: String = PATHOLOGY_SECTION_CODE

    /** 발행측 병리결과 식별자 — **파티션 키**다(적재 멱등 키는 공통 `sourceAggregateId`). */
    var pathologyResultId: String? = null

    companion object {
        /** 토픽 `{환경}.{테넌트id}.pathology-result` 의 **namespace 뒤 마디**(이벤트명). */
        const val TOPIC = "pathology-result"

        /** [pathologyResultId] 의 payload 필드명 — `topic_registry` 의 partitionKeyField. */
        const val PARTITION_KEY_FIELD = "pathologyResultId"

        const val PATHOLOGY_SECTION_CODE = "SP"
    }
}
```

### 6.6 핸들러 베이스 — 템플릿 메서드 `handle()` → `handle_internal()`

`K/application/integrationevents/eventhandling/integration_event_handler.kt:11-50`
```kotlin
/**
 * 통합 이벤트 핸들러 베이스 — 봉투를 풀어 payload 와 테넌트 범위를 넘기고, 진입/실패만 로깅한다.
 *
 * 테넌트 범위는 여기서 한 번 정한다(`event_tenant_resolver` — 봉투 vs 배포값). 하위 핸들러는 받은 범위로만 찾고 만든다.
 * 감사 컬럼의 직원 ID 는 inbox 처리(`inbox_dispatcher`)가 처리 중 MDC `userId` 에 넣어 둔 봉투 값을 영속성 리스너가 읽는다 — 여기서 넘기지 않는다.
 *
 * 멱등은 여기서 하지 않는다. 이 BC 의 모든 상태 변경은 `observation.apply_source_event` 가
 * (source_aggregate_id, source_event_version) 기준으로 자체 멱등을 보장한다. 재시도 · 실패 확정(FAILED)은 수신 inbox 몫이다(classdiagram §1.8).
 *
 * `out T` — 한 핸들러가 같은 스키마의 여러 messageType(생성·수정)을 함께 받는다.
 *
 * **트랜잭션 경계는 `handle()` 이다** — inbox 처리(`inbox_dispatcher`)가 부르는 public 메서드라 프록시를 탄다.
 * inbox 의 처리 트랜잭션(T2) 안에서 불리므로 그 트랜잭션에 합류한다 — 결과 반영과 inbox 행의 PROCESSED 가 함께 커밋된다.
 * `handle_internal` 에 `@Transactional` 을 붙이면 같은 빈 안의 호출(self-invocation)이라
 * 프록시를 거치지 않아 **트랜잭션이 걸리지 않는다** — 조회 → 저장이 원자적이지 않게 된다.
 * 예외가 나면 T2 가 롤백되고 같은 예외가 inbox 로 올라가 재시도 대기 또는 FAILED 로 기록된다.
 */
abstract class integration_event_handler<T : integration_event>(
    private val tenant_resolver: event_tenant_resolver,
) {

    /** open — `@Transactional` 빈은 CGLIB 프록시 대상이라 final 메서드면 프록시가 가로채지 못한다. */
    @Transactional
    open fun handle(envelope: MessageEnvelope<out T>) {
        val event = envelope.payload
        logger.info { "통합 이벤트 수신: messageType=${envelope.messageType} messageId=${envelope.messageId}" }
        try {
            val scope = tenant_resolver.resolve(envelope.tenantId, envelope.hospitalCode, envelope.messageId.toString())
            handle_internal(event, scope)
        } catch (ex: Exception) {
            logger.error(ex) {
                "통합 이벤트 처리 실패: messageType=${envelope.messageType} messageId=${envelope.messageId} 사유=${ex.message}"
            }
            throw ex
        }
    }

    /** 하위 핸들러의 실제 처리 — `handle()` 의 트랜잭션 안에서 돈다. 여기에 `@Transactional` 을 붙이지 않는다. */
    protected abstract fun handle_internal(event: T, scope: tenant_scope)
}
```

테넌트 범위 결정(봉투 vs 배포값) — `K/application/integrationevents/eventhandling/event_tenant_resolver.kt:10-31`
```kotlin
/**
 * 수신 이벤트의 테넌트 범위를 정한다 — 봉투(MessageEnvelope)의 `tenantId` · `hospitalCode` 와 배포값을 맞춰 본다.
 *
 * 규칙(테넌트 · 병원 각각):
 * - 봉투 값이 배포값과 맞으면(대소문자 무시) 배포값을 쓴다 — 표기를 배포값 하나로 맞춘다.
 * - **다르면 경고 로그만 남기고 배포값으로 적재한다** — DLT 로 보내지 않는다. 토픽 자체가 병원별(`{환경}.{병원}.…`)이라
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

    fun resolve(envelope_tenant_id: String?, envelope_hospital_code: String?, message_id: String): tenant_scope {
        val tenant_id = resolve_tenant(envelope_tenant_id?.trim()?.takeIf { it.isNotEmpty() }, message_id)
        val hospital_code = resolve_hospital(envelope_hospital_code?.trim()?.takeIf { it.isNotEmpty() }, message_id)
        return tenant_scope.of(tenant_id, hospital_code)
    }
```

### 6.7 구체 핸들러 — 5원천 · 생성/수정/삭제 공용 한 벌

`K/application/integrationevents/eventhandling/observation_result_handler.kt:34-98`
```kotlin
@Component
class observation_result_handler(
    private val repository: i_observation_repository,
    private val event_publisher: ApplicationEventPublisher,
    tenant_resolver: event_tenant_resolver,
) : integration_event_handler<observation_result_integration_event>(tenant_resolver) {

    // 트랜잭션은 베이스의 handle() 이 연다(self-invocation 회피).
    override fun handle_internal(event: observation_result_integration_event, scope: tenant_scope) {
        val source_aggregate_id = requireNotNull(event.sourceAggregateId) { "수신 payload 필수 필드 누락: sourceAggregateId" }
        if (is_deleted(event)) {
            delete(event, scope, source_aggregate_id)
            return
        }
        upsert(event, scope, source_aggregate_id)
    }

    /**
     * 원천 삭제 — 버전 비교는 `mark_deleted` 가 한다(같거나 낮은 버전이면 무시).
     * 아직 적재되지 않은 건은 지울 것이 없다 — 정상 무시, 로그만 남긴다.
     */
    private fun delete(event: observation_result_integration_event, scope: tenant_scope, source_aggregate_id: String) {
        val target = repository.find_observation_by_source(scope, source_aggregate_id)
        if (target == null) {
            logger.info { "삭제 대상 없음 — 적재된 적 없는 원천: sourceAggregateId=$source_aggregate_id" }
            return
        }
        val version = event.resolved_event_version()
        if (target.mark_deleted(version)) {
            repository.create_or_update_observation(target)
            event_publisher.publishEvent(observation_results_changed(scope, target.patient_id))
        } else {
            logger.info { "늦게 온 삭제 무시: sourceAggregateId=$source_aggregate_id 삭제버전=$version 적재버전=${target.source_event_version}" }
        }
    }

    private fun upsert(event: observation_result_integration_event, scope: tenant_scope, source_aggregate_id: String) {
        val payload = event.to_observation_payload()
        val target = repository.find_observation_by_source(scope, source_aggregate_id)
            ?: new_observation(payload.observation_type_code, UUID.randomUUID().toString(), scope)
        if (target.apply_source_event(source_aggregate_id, event.resolved_event_version(), payload)) {
            repository.create_or_update_observation(target)
            event_publisher.publishEvent(observation_results_changed(scope, target.patient_id))
        }
    }

    /**
     * 삭제 판정 — 대소문자는 구분하지 않는다. 모르는 값은 **적재**로 보고 경고만 남긴다.
     * 발행측이 행위를 안 실으면 "안 지워짐"으로 끝나야 한다(반대로 기울면 멀쩡한 결과가 사라진다).
     */
    private fun is_deleted(event: observation_result_integration_event): Boolean {
        val action = event.eventType?.trim()?.lowercase()
        return when (action) {
            observation_result_integration_event.DELETED -> true
            null, "",
            observation_result_integration_event.CREATED,
            observation_result_integration_event.MODIFIED,
            -> false
            else -> {
                logger.warn { "모르는 행위 — 적재로 처리한다: eventType=${event.eventType} sourceAggregateId=${event.sourceAggregateId}" }
                false
            }
        }
    }
}
```

멱등의 실체는 Aggregate 메서드다 — `K/domain/aggregatesmodel/observation/observation.kt:176-187`, `:226-237`
```kotlin
    // 원천 이벤트 적재 — (source_aggregate_id, source_event_version) 기준 멱등
    // 동일 source_aggregate_id 에서 더 낮거나 같은 버전이 들어오면 무시
    fun apply_source_event(source_aggregate_id: String, source_event_version: Long, payload: observation_payload): Boolean {
        require(source_aggregate_id.isNotBlank()) { "source_aggregate_id 는 필수입니다" }
        require(source_event_version > 0) { "source_event_version 은 1 이상이어야 합니다" }

        // 이미 더 최신 버전이 적재되어 있으면 멱등 무시
        if (this.source_aggregate_id == source_aggregate_id && source_event_version <= this.source_event_version) {
            return false
        }
        this.source_aggregate_id = source_aggregate_id
        this.source_event_version = source_event_version
```
```kotlin
    // 원천 삭제 반영 — 적재와 같은 버전 축으로 멱등
    // 적재된 버전보다 높은 삭제만 반영한다 — 늦게 도착한 삭제(같거나 낮은 버전)가 최신 적재를 지우지 못하게.
    // 반영했으면 true (호출부가 저장 여부를 정한다)
    fun mark_deleted(source_event_version: Long): Boolean {
        require(source_event_version > 0) { "source_event_version 은 1 이상이어야 합니다" }
        if (source_event_version <= this.source_event_version) {
            return false
        }
        this.source_event_version = source_event_version
        this.is_deleted = true
        return true
    }
```

### 6.8 매퍼 — 계약(camelCase payload) → 도메인 VO(`observation_payload`)

확장 함수 하나가 원천 5종을 다 처리하고, 필수값 검증도 여기서 한다(실패 = `IllegalArgumentException` = 영구 실패 → FAILED).

`K/application/integrationevents/eventhandling/observation_payload_mapper.kt:17-74`
```kotlin
/**
 * 수신 payload → 도메인 적재 VO 변환. **원천(검체진단 · 영상진단 · 기능진단 · 임상관찰) 공용 한 벌**이다 —
 * 이벤트 스키마가 `observation_result_integration_event` 한 벌이라 원천별 매퍼를 둘 이유가 없다.
 *
 * 필수값은 여기서 한 번에 검증한다(이벤트가 setter 바인딩이라 생성자 검증이 없다).
 * 누락은 `IllegalArgumentException` → 핸들러가 로깅 후 재던짐 → 영구 실패라 inbox 행이 바로 FAILED(classdiagram §1.8).
 *
 * **적재(생성 · 수정) 경로에서만 호출된다** — 삭제는 `sourceAggregateId` 하나면 충분해서 이 변환을
 * 타지 않는다. 그래서 여기 필수값 목록을 삭제 메시지가 만족할 필요가 없다.
 *
 * payload 필드 이름(camelCase)은 cross-BC 계약이라 분류 명칭 개정과 무관하게 옛 이름을 그대로 읽는다 —
 * 도메인 쪽 이름으로 바꿔 담는 자리가 여기다(`vitalResultValue` → `record_result_value` 등).
 */
internal fun observation_result_integration_event.to_observation_payload(): observation_payload {
    val type_code = observationTypeCode.required("observationTypeCode")
    require(type_code in allowed_observation_type_codes) {
        "이 토픽이 실어 나를 수 없는 검사분류입니다: $type_code (허용 ${allowed_observation_type_codes.joinToString("/")})"
    }
    return observation_payload(
        // 환자명(patientName)은 옮기지 않는다 — 환자 정보는 shared.patient 를 patient_id 로 조인해 읽는다
        // id 는 shared 의 uuid 다 — 환자 · 수진은 UUID 가 아니면 거절(FAILED), 진료과 · 진료의는 경고 후 null
        patient_id = patientId.required_uuid("patientId"),
        encounter_id = encounterId.required_uuid("encounterId"),
        // 원 수진 id — 선택. UUID 가 아니면 경고 후 null, 비면 미동봉(기존 값을 덮지 않게)
        origin_encounter_id = originEncounterId.optional_uuid("originEncounterId", sourceAggregateId),
        // 처방 id — 선택. 빈 문자열은 미동봉(기존 값을 덮지 않게)
        order_id = orderId?.trim().blank_to_null(),
        // 수진·처방 스냅샷 — 선택. 빈 문자열은 미동봉으로 본다(기존 값을 덮지 않게)
        encounter_class = encounterClass.blank_to_null(),
        encounter_date = encounterDate,
        department_id = departmentCode.optional_uuid("departmentCode", sourceAggregateId),
        // 진료과 · 진료의 · 처방의 이름은 계약에 없다 — 조회 때 shared 에서 id 로 읽는다
        doctor_id = doctorId.optional_uuid("doctorId", sourceAggregateId),
        ordering_doctor_id = orderingDoctorId.optional_uuid("orderingDoctorId", sourceAggregateId),
        order_date = orderDate,
        ordered_at = orderedAt.required("orderedAt").toInstant(),
        // 예약일시 — 미동봉이면 시행일시로 채운다(예약 없이 바로 시행한 건은 발행측이 비워 보낸다)
        scheduled_at = (scheduledAt ?: performedAt.required("performedAt")).toInstant(),
        received_at = receivedAt.required("receivedAt").toInstant(),
        performed_at = performedAt.required("performedAt").toInstant(),
        result_datetime = resultDatetime.required("resultDatetime").toInstant(),
        verified_at = verifiedAt.required("verifiedAt").toInstant(),
        observation_type_code = type_code,
        observation_type_name = observationTypeName.required("observationTypeName"),
        // 분류 코드는 NOT NULL 이다 — 발행측이 아직 안 싣는 동안은 검사분류에서 파생한다.
        // 외부연동기록은 구분코드(pasta-glucose …)가 화면 갈래라 **필수**다 — 없으면 둘 곳이 없어 계약 위반으로 본다(FAILED).
        category_code = if (type_code == external_record_observation.OBSERVATION_TYPE_CODE) {
            categoryCode.required("categoryCode").lowercase()
        } else {
            categoryCode?.takeIf { it.isNotBlank() } ?: default_category_code(type_code)
        },
        // 진단 서비스 구분은 토픽이 정한다(병리 → SP) — payload 로 받지 않는다
        diagnostic_service_section_code = topic_section_code,
        observation_code = observationCode.required("observationCode"),
        observation_name = observationName.required("observationName"),
        result_type_code = resultTypeCode.result_type_code_or_default(sourceAggregateId),
        // 서식 식별자 — 선택. 빈 문자열은 미동봉(기존 값을 덮지 않게)
        record_id = recordId.blank_to_null(),
```

매퍼 보조 함수(필수/선택 id · 기본값) — `K/application/integrationevents/eventhandling/observation_payload_mapper.kt:171-213`
```kotlin
private fun default_category_code(observation_type_code: String): String =
    when (observation_type_code) {
        specimen_diagnostics_observation.OBSERVATION_TYPE_CODE -> observation_category_code.LABORATORY
        diagnostic_imaging_observation.OBSERVATION_TYPE_CODE -> observation_category_code.IMAGING
        functional_diagnostics_observation.OBSERVATION_TYPE_CODE -> observation_category_code.PROCEDURE
        clinical_record_observation.OBSERVATION_TYPE_CODE -> observation_category_code.VITAL_SIGNS
        else -> error("알 수 없는 검사분류입니다: $observation_type_code")
    }

/** 수신 payload 의 필수 필드 언랩 — 누락 시 어느 필드인지 이름을 남긴다. */
private fun String?.blank_to_null(): String? = this?.takeIf { it.isNotBlank() }

// 결과값 구분 — 정해진 값 체계(대소문자 무관)로 맞춘다. 없거나 모르는 값이면 문자열(shortString)로 본다 —
// 결과값은 어느 구분이든 문자열로는 보일 수 있어서, 구분 하나 때문에 결과를 FAILED 로 버리지 않는다
private val RESULT_TYPE_CODES = listOf("shortString", "numeric", "longText", "link", "image")
private const val DEFAULT_RESULT_TYPE_CODE = "shortString"

private fun String?.result_type_code_or_default(source_aggregate_id: String?): String {
    val value = this?.trim()?.takeIf { it.isNotEmpty() } ?: return DEFAULT_RESULT_TYPE_CODE
    RESULT_TYPE_CODES.firstOrNull { it.equals(value, ignoreCase = true) }?.let { return it }
    logger.warn { "모르는 결과값 구분이다 — $DEFAULT_RESULT_TYPE_CODE 로 적재한다: resultTypeCode=$value sourceAggregateId=$source_aggregate_id" }
    return DEFAULT_RESULT_TYPE_CODE
}

// 판독 상태 — 발행측마다 대소문자가 달라(FINAL · final) 소문자 하나로 맞춰 적재한다. 공백만 있으면 미동봉
private fun String?.report_status_or_null(): String? = this?.trim()?.takeIf { it.isNotEmpty() }?.lowercase()

// 필수 id — 없거나 UUID(하이픈 포함 36자 표준 형식 · strict_uuid_or_null)가 아니면 거절한다(예외 → 영구 실패 → inbox 행 FAILED).
// 값은 메시지에 싣지 않는다(환자 식별자)
private fun String?.required_uuid(field_name: String): UUID =
    requireNotNull(strict_uuid_or_null(required(field_name))) { "수신 payload id 형식 오류(UUID 아님): $field_name" }

// 선택 id — 비면 미동봉(null), UUID 가 아니면 경고만 남기고 null 로 적재한다(결과 자체는 남긴다)
private fun String?.optional_uuid(field_name: String, source_aggregate_id: String?): UUID? {
    val value = this?.trim()?.takeIf { it.isNotEmpty() } ?: return null
    strict_uuid_or_null(value)?.let { return it }
    logger.warn { "수신 id 가 UUID 가 아니다 — null 로 적재한다: field=$field_name sourceAggregateId=$source_aggregate_id" }
    return null
}

private fun <T : Any> T?.required(field_name: String): T =
    requireNotNull(this) { "수신 payload 필수 필드 누락: $field_name" }
        .also { if (it is String) require(it.isNotBlank()) { "수신 payload 필수 필드 공백: $field_name" } }
```

### 6.9 inbox 적재 — 수신은 "원문을 DB 에 넣기"만

`K/infrastructure/inbox/inbox_receiver.kt:15-64`
```kotlin
/**
 * 수신 — 원문을 inbox 에 **적재만** 한다(비즈니스 로직 없음). 적재가 커밋되고 여기서 돌아가면 Kafka 가 ack 한다.
 *
 * 여기가 실패하는 것은 DB 에 쓰지 못할 때뿐이다 — 그때만 라이브러리 재시도 · DLT 가 돈다. 본문이 깨져도 적재는 된다
 * (메타를 못 읽으면 대체값으로 채우고, 처리 단계에서 영구 실패(FAILED)로 남는다).
 *
 * 트랜잭션은 `inbox_tx_service.store` 가 연다 — 이 클래스는 트랜잭션 밖이다. 동시 재전달로 `message_id` UK 가 부딪히면
 * 그 예외를 **트랜잭션 밖인 여기서** 잡는다(트랜잭션 안에서 잡으면 rollback-only 로 커밋이 실패한다).
 */
@Component
class inbox_receiver(
    private val tx: inbox_tx_service,
    private val object_mapper: ObjectMapper,
) {

    /** [topic] 은 namespace 를 뺀 토픽(이벤트명) — 구독 등록 때 정해진다. */
    fun receive(topic: String, raw: RawMessage) {
        val message = to_inbox_message(topic, raw, Instant.now())
        try {
            if (tx.store(message)) {
                logger.info { "inbox 적재: topic=$topic key=${message.partition_key} messageId=${message.message_id} messageType=${message.event_type}" }
            } else {
                logger.info { "중복 수신 — 이미 적재된 메시지라 건너뛴다: topic=$topic messageId=${message.message_id}" }
            }
        } catch (ex: DataIntegrityViolationException) {
            // 다른 무결성 위반(길이 초과 등)은 삼키지 않는다 — 중복이 확인될 때만 성공으로 본다.
            if (!tx.exists(message.message_id)) throw ex
            logger.info { "중복 수신(동시 재전달) — 건너뛴다: topic=$topic messageId=${message.message_id}" }
        }
    }

    /**
     * 원문 → inbox 행. 봉투 메타(`messageId` · `messageType`)는 본문에서 읽고, 못 읽으면 대체한다:
     * `message_id` → 원문 SHA-256 hex(같은 원문의 재전달은 같은 값이라 중복이 걸러진다),
     * `event_type` → 헤더의 messageType → `UNKNOWN`, `partition_key` → Kafka 키가 없으면 `message_id`(혼자 처리된다).
     */
    internal fun to_inbox_message(topic: String, raw: RawMessage, at: Instant): inbox_message {
        val envelope = read_tree(raw.payload)
        val message_id = envelope.text("messageId")?.takeIf { it.length <= MAX_MESSAGE_ID_LENGTH } ?: sha256_hex(raw.payload)
        return inbox_message.received(
            message_id = message_id,
            topic = topic,
            partition_key = raw.key?.takeIf { it.isNotBlank() } ?: message_id,
            event_type = raw.messageType?.takeIf { it.isNotBlank() } ?: envelope.text("messageType") ?: UNKNOWN_EVENT_TYPE,
            payload = raw.payload,
            headers = raw.headers.takeIf { it.isNotEmpty() }?.let { object_mapper.writeValueAsString(it) },
            at = at,
            source_service = envelope.text("sourceService"),
        )
    }
```

inbox 행 엔티티(상태 enum · 키) — `K/infrastructure/inbox/inbox_message.kt:15-56`
```kotlin
/** inbox 행 상태 — HOLD 는 상태가 아니라 집기 조건(앞선 행이 끝나지(PROCESSED · TERMINATED) 않음)이다. */
enum class inbox_status {
    /** 처리 대기 — `next_retry_at` 이 지나면 집힌다(재시도 대기 포함). */
    PENDING,

    /** 한 파드가 집어 처리 중 — `locked_until` 이 지나면 sweeper 가 PENDING 으로 되돌린다. */
    PROCESSING,

    /** 처리 완료 — 같은 키의 다음 행이 풀린다. */
    PROCESSED,

    /** 처리 포기(영구 실패 · 재시도 상한) — 같은 키의 뒤 행은 멈춘 채 남는다. */
    FAILED,

    /** 강제 종료 — 사람이 반영 없이 처리를 끝냈다(실패 · 대기 행 적체 방지). 끝난 행이라 같은 키의 뒤 행이 풀린다. */
    TERMINATED,
    ;

    companion object {
        /** 끝난 상태 — 같은 키의 뒤 행을 막지 않는다(집기 조건 · 순서 대기 판정 · 키 상태가 같은 목록을 쓴다). */
        val FINISHED: List<inbox_status> = listOf(PROCESSED, TERMINATED)
    }
}

/** 행이 생긴 길 — Kafka 수신 · 사람의 재전송. 조회가 application 의 같은 이름 값으로 옮겨 내린다. */
enum class inbox_origin { KAFKA, RESEND }

/** hold · 처리 순서의 단위 — 같은 키는 직렬, 다른 키는 병렬. 토픽마다 키의 의미가 달라 토픽까지 묶는다. */
data class inbox_key(val topic: String, val partition_key: String)

/**
 * 수신 원문 1건 — 통합 이벤트가 핸들러에 닿기 전에 머무는 자리(classdiagram §1.8).
 *
 * 도메인 Entity 가 아니라 수신 장치라 테넌트 · 감사 공통 컬럼(§1.5)을 두지 않는다 — 테넌트는 처리 때 봉투로 정한다.
 * PK(`inbox_message_id`)는 **INSERT 순서의 IDENTITY** 이고 그 값이 도착 순번(seq)이다 — 같은 키 안의 처리 순서 · hold 비교의 기준.
 * 배치 할당 시퀀스(allocationSize)는 도착 순서를 깨므로 쓰지 않는다.
 *
 * 상태는 아래 메서드로만 바꾼다(서비스에서 필드를 직접 고치지 않는다).
 */
@Entity
@Table(name = "inbox_message")
class inbox_message protected constructor() {
```

### 6.10 inbox 처리 — 트랜잭션 경계 T1/T2/T3 와 처리 루프

`K/infrastructure/inbox/inbox_tx_service.kt:13-69`
```kotlin
/**
 * inbox 트랜잭션 경계 모음 — 적재 · 집기(T1) · 처리(T2) · 실패 기록(T3) · 회수.
 *
 * ⚠️ **이 메서드들은 반드시 다른 빈(`inbox_processor` · `inbox_receiver` · `inbox_sweeper`)이 부른다.**
 * 같은 빈 안에서 부르면(self-invocation) 프록시를 타지 않아 `REQUIRES_NEW` 가 무시되고, T3 의 실패 기록이 T2 와 함께 롤백된다.
 * 부르는 쪽에는 트랜잭션이 없어야 한다.
 *
 * 엔티티를 트랜잭션 사이로 넘기지 않는다 — id(Long)만 넘기고 각 트랜잭션이 다시 읽는다(T2 가 롤백되면 그 엔티티는 detached 다).
 */
@Service
class inbox_tx_service(
    private val repository: inbox_message_jpa_repository,
    private val dispatcher: inbox_dispatcher,
    private val retry_policy: inbox_retry_policy,
    private val properties: inbox_properties,
    private val event_publisher: ApplicationEventPublisher,
) {

    /**
     * 수신 1건 적재. 같은 `message_id` 가 이미 있으면 `false`(중복 — 적재하지 않는다).
     * 적재했으면 커밋 뒤 그 키를 바로 처리하도록 `inbox_message_stored` 를 발행한다(`inbox_drain_trigger`).
     * 동시 재전달로 UK 위반이 나면 예외가 그대로 나간다 — 받는 쪽(`inbox_receiver`)이 트랜잭션 밖에서 판정한다.
     */
    @Transactional
    fun store(message: inbox_message): Boolean {
        if (repository.exists_by_message_id(message.message_id)) return false
        repository.save(message)
        event_publisher.publishEvent(inbox_message_stored(message.key()))
        return true
    }

    @Transactional(readOnly = true)
    fun exists(message_id: String): Boolean = repository.exists_by_message_id(message_id)

    /** T1 — 키의 맨 앞 처리 가능 행을 집어 PROCESSING + 임대. 집을 것이 없으면 null(hold 포함). */
    @Transactional
    fun claim_next(key: inbox_key): Long? {
        val now = Instant.now()
        val message = repository.pick_next(
            key.topic, key.partition_key, now, inbox_status.PENDING, inbox_status.FINISHED, FIRST_ONE,
        ).firstOrNull() ?: return null
        message.mark_processing(worker_id(), now.plusSeconds(properties.lease_seconds))
        return message.inbox_message_id
    }

    /**
     * T2 — 비즈니스 처리와 PROCESSED 를 **한 트랜잭션**으로 커밋한다. 핸들러의 `@Transactional` 은 여기에 합류한다.
     * 예외는 잡지 않고 그대로 던진다 — 여기서 잡으면 rollback-only 인 채 커밋을 시도해 `UnexpectedRollbackException` 이 난다.
     * 시간 제한은 임대(lease)보다 짧아야 한다 — 길면 처리 중에 sweeper 가 회수해 다른 파드가 같은 행을 또 처리한다.
     */
    @Transactional(timeoutString = "\${diagnosticresult.inbox.process-timeout-seconds:60}")
    fun process(inbox_message_id: Long) {
        val message = load(inbox_message_id)
        if (message.status in inbox_status.FINISHED) return // 임대 만료 회수 뒤 다른 파드가 먼저 끝냈다(또는 강제 종료됐다)
        dispatcher.dispatch(message)
        message.mark_processed(Instant.now())
    }
```

T3 실패 기록(`REQUIRES_NEW`) — `K/infrastructure/inbox/inbox_tx_service.kt:71-87`
```kotlin
    /**
     * T3 — 실패 기록. T2 와 별개로 커밋되도록 `REQUIRES_NEW` 다. 롤백된 T2 의 엔티티를 쓰지 않고 다시 읽는다.
     * FAILED 로 확정됐으면 `true` — 같은 키의 뒤 행이 멈췄다는 뜻이라 ERROR 로 남기고 `inbox_message_failed` 를 낸다
     * (T3 커밋 뒤 `inbox_failure_alert` 가 알림 센터로 보낸다 — classdiagram §1.10).
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun mark_retry(inbox_message_id: Long, error: Throwable): Boolean {
        val message = load(inbox_message_id)
        val failures = message.retry_count + 1
        val now = Instant.now()
        val failed = message.record_failure(
            error = describe(error),
            at = now,
            retry_at = now.plus(retry_policy.delay_after(failures)),
            give_up = retry_policy.gives_up(failures, error),
        )
        if (failed) {
```

처리 루프(트랜잭션 없음) — `K/infrastructure/inbox/inbox_processor.kt:8-34`
```kotlin
/**
 * 키 하나를 차례로 흘린다 — **트랜잭션이 없다**(순서만 잡고 경계는 전부 `inbox_tx_service` 가 갖는다).
 *
 * 루프: T1 집기 → T2 처리 → 성공이면 같은 키의 다음 행을 바로 이어서 집는다(체인 — 앞 행이 PROCESSED 가 되면 뒤 행이 풀린다).
 * 실패하면 T3 로 기록하고 멈춘다 — 뒤 행은 hold 조건으로 자연히 기다리고, 재시도 시각이 되면 sweeper 가 다시 부른다.
 * 집을 것이 없으면(비었거나 · 앞 행이 처리 중 · 재시도 대기 · FAILED) 바로 끝난다.
 *
 * 적재 커밋 직후(`inbox_drain_trigger`)와 sweeper 가 같은 이 함수 하나를 부른다.
 */
@Component
class inbox_processor(
    private val tx: inbox_tx_service,
) {

    fun drain(key: inbox_key) {
        while (true) {
            val inbox_message_id = tx.claim_next(key) ?: return
            val failure = runCatching { tx.process(inbox_message_id) }.exceptionOrNull() ?: continue
            runCatching { tx.mark_retry(inbox_message_id, failure) }
                .onFailure {
                    // 실패 기록마저 못 했다(DB 장애 등) — 행은 PROCESSING 으로 남고 임대가 끝나면 sweeper 가 회수한다.
                    logger.error(it) { "inbox 실패 기록 실패 — 임대 만료 뒤 다시 처리된다: id=$inbox_message_id key=$key" }
                }
            return
        }
    }
}
```

inbox 행 → 봉투 복원 → 핸들러(MDC 에 봉투 userId 주입) — `K/infrastructure/inbox/inbox_dispatcher.kt:23-59`
```kotlin
@Component
class inbox_dispatcher(
    private val handler: observation_result_handler,
    private val object_mapper: ObjectMapper,
) {

    private val event_types = RECEIVED_RESULT_EVENTS.associate { it.topic to it.event_type }

    fun dispatch(message: inbox_message) {
        val event_type = requireNotNull(event_types[message.topic]) {
            "수신 토픽이 아니다 — 분배할 이벤트 클래스가 없다: topic=${message.topic}"
        }
        val envelope_type = object_mapper.typeFactory.constructParametricType(MessageEnvelope::class.java, event_type.java)
        val envelope: MessageEnvelope<out observation_result_integration_event> = object_mapper.readValue(message.payload, envelope_type)
        with_envelope_mdc(envelope) { handler.handle(envelope) }
    }

    private fun with_envelope_mdc(envelope: MessageEnvelope<*>, block: () -> Unit) {
        val previous = MDC_KEYS.associateWith { MDC.get(it) }
        try {
            put_or_remove(JwtKeys.USER_ID, envelope.userId)
            put_or_remove(JwtKeys.TENANT_ID, envelope.tenantId)
            put_or_remove(JwtKeys.HOSPITAL_CODE, envelope.hospitalCode)
            block()
        } finally {
            previous.forEach { (key, value) -> put_or_remove(key, value) }
        }
    }

    private fun put_or_remove(key: String, value: String?) {
        if (value != null) MDC.put(key, value) else MDC.remove(key)
    }

    private companion object {
        val MDC_KEYS = listOf(JwtKeys.USER_ID, JwtKeys.TENANT_ID, JwtKeys.HOSPITAL_CODE)
    }
}
```

재시도 규칙(영구 실패 판정) — `K/infrastructure/inbox/inbox_retry_policy.kt:20-45`
```kotlin
@Component
class inbox_retry_policy(
    private val properties: inbox_properties,
) {

    val max_retry: Int get() = properties.max_retry

    /** [failures] 번째 실패(1부터) 뒤 다시 집힐 때까지의 대기. */
    fun delay_after(failures: Int): Duration {
        val steps = properties.backoff_seconds.ifEmpty { listOf(DEFAULT_BACKOFF_SECONDS) }
        return Duration.ofSeconds(steps[(failures - 1).coerceIn(0, steps.lastIndex)])
    }

    fun is_permanent(error: Throwable): Boolean =
        generateSequence(error) { it.cause.takeIf { cause -> cause !== it } }
            .take(MAX_CAUSE_DEPTH)
            .any { it is JacksonException || it is IllegalArgumentException || it is diagnostic_result_domain_exception }

    /** 이번 실패로 [failures] 번째가 됐을 때 FAILED 로 둘지. */
    fun gives_up(failures: Int, error: Throwable): Boolean = is_permanent(error) || failures >= max_retry

    private companion object {
        const val DEFAULT_BACKOFF_SECONDS = 60L
        const val MAX_CAUSE_DEPTH = 10
    }
}
```

안전망 스케줄러 — `K/infrastructure/inbox/inbox_sweeper.kt:22-44`
```kotlin
@Component
@EnableScheduling
class inbox_sweeper(
    private val tx: inbox_tx_service,
    private val executor: inbox_drain_executor,
    private val properties: inbox_properties,
) {

    @Scheduled(
        fixedDelayString = "\${diagnosticresult.inbox.sweep-interval-ms:30000}",
        initialDelayString = "\${diagnosticresult.inbox.sweep-interval-ms:30000}",
    )
    fun sweep() {
        try {
            val reclaimed = tx.reclaim_expired()
            if (reclaimed > 0) logger.warn { "inbox 임대 만료 회수 — 다시 처리한다: ${reclaimed}건" }
            tx.find_ready_keys(properties.sweep_batch_size).forEach { executor.drain_later(it) }
        } catch (ex: Exception) {
            // DB 장애 등 — 스케줄러 스레드로 올리지 않고 다음 주기에 다시 돈다.
            logger.error(ex) { "inbox sweeper 실패 — 다음 주기에 다시 돈다" }
        }
    }
}
```

### 6.11 `@TransactionalEventListener(AFTER_COMMIT)` — 프로세스 내부 이벤트 3곳

이 MS 는 Kafka 로 **발행하지 않는다**. `AFTER_COMMIT` 리스너는 전부 **프로세스 안 Spring 이벤트**용이다.

① 적재 커밋 → 즉시 drain — `K/infrastructure/inbox/inbox_drain_trigger.kt:7-25`
```kotlin
/** 적재 커밋 신호 — `inbox_tx_service.store` 가 발행한다. */
data class inbox_message_stored(val key: inbox_key)

/**
 * 적재가 **커밋된 뒤** 그 키를 바로 처리한다(즉시 처리). 롤백된 적재는 처리하지 않는다.
 * 처리는 `inbox_drain_executor` 의 다른 스레드에서 돈다 — 이 콜백 안에서 DB 를 쓰면 커밋되지 않는다.
 *
 * 레이스로 여기서 못 집은 행(앞 행이 아직 처리 중으로 보인 경우 등)은 앞 행의 체인이나 sweeper 가 줍는다.
 */
@Component
class inbox_drain_trigger(
    private val executor: inbox_drain_executor,
) {

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun on_stored(event: inbox_message_stored) {
        executor.drain_later(event.key)
    }
}
```

왜 별도 스레드인가 — `K/infrastructure/inbox/inbox_drain_executor.kt:11-20`
```kotlin
/**
 * inbox 처리 전용 실행기 — `drain(key)` 를 별도 스레드에서 돌린다.
 *
 * 적재 커밋 직후(`AFTER_COMMIT`) 처리를 **새 스레드**로 넘겨야 한다 — 그 콜백 안에서 바로 DB 를 쓰면 원래 트랜잭션 자원에
 * 묶여 커밋되지 않는다(Spring `TransactionalEventListener` 문서). 그래서 `@Async` 대신 이 실행기를 직접 쓴다 —
 * Executor 빈을 하나 더 등록하면 Boot 의 기본 `applicationTaskExecutor` 가 물러날 수 있어 앱 전역에 영향을 주지 않으려는 것이다.
 *
 * 가득 차면(큐 초과 · 종료 중) 버리고 경고만 남긴다 — 행은 DB 에 있으므로 sweeper 가 다음 주기에 줍는다.
 * 처리 루프(`inbox_processor`)에 의존하므로 종료 때 그보다 먼저 닫히고, 진행 중인 처리를 기다린다.
 */
```

② 결과 반영 커밋 → AI 재점검 대상 표시 — `K/application/analysis/observation_results_changed.kt:6-11`, `K/application/analysis/analysis_review_target_listener.kt:7-21`
```kotlin
// 검사결과가 적재 · 수정 · 삭제 반영됐다 — 프로세스 안 application 이벤트(외부로 나가지 않는다).
// observation_result_handler 가 결과 트랜잭션 안에서 발행하고, analysis_review_target_listener 가 커밋 뒤 받아 재점검 대상을 켠다.
data class observation_results_changed(
    val scope: tenant_scope,
    val patient_id: UUID,
)
```
```kotlin
/**
 * 결과 적재 · 변경 → AI 재점검 대상 표시. **결과 트랜잭션이 커밋된 뒤**에 켠다 —
 * 커밋 전에 켜면, 그 사이 시작한 분석이 대상을 가져가면서도 아직 안 보이는 결과를 놓친다.
 * 결과 적재가 롤백되면 표시하지 않는다.
 */
@Component
class analysis_review_target_listener(
    private val review_targets: analysis_review_targets,
) {

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun on_results_changed(event: observation_results_changed) {
        review_targets.mark(event.scope, event.patient_id)
    }
}
```

③ FAILED 확정 커밋 → 알림 센터 — `K/infrastructure/inbox/inbox_failure_alert.kt:80-95`
```kotlin
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun on_failed(event: inbox_message_failed) {
        if (!enabled) return
        submit("FAILED 알림 id=${event.inbox_message_id}") { send_failed(event) }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun on_cleared(event: inbox_failure_cleared) {
        if (!enabled || event.failures.isEmpty()) return
        val token = token_source.request_token() ?: token_source.service_token()
        if (token == null) {
            logger.warn { "inbox 실패 알림을 끝내지 못했다 — 요청 토큰 · 서비스 토큰이 없다: ids=${event.failures.map { it.inbox_message_id }}" }
            return
        }
        submit("알림 끝내기 ids=${event.failures.map { it.inbox_message_id }}") { send_cleared(event.failures, token) }
    }
```

---

## 7. 예외 → HTTP

두 개의 `@RestControllerAdvice` 가 있다. **전역**은 `global_exception_handler`(infrastructure/config), **inbox 운영 API 전용**은 `inbox_error_advice`(presentation, `assignableTypes` + `HIGHEST_PRECEDENCE`) 다. 둘 다 응답 본문은 `{ error, message(, details) }` 이고 `error` 는 카테고리 코드만 싣는다.

### 7.1 도메인 예외 계층

`K/domain/exceptions/diagnostic_result_domain_exception.kt:1-22`
```kotlin
package com.projectx.diagnosticresult.domain.exceptions

// 진단결과 도메인 공통 예외 베이스
open class diagnostic_result_domain_exception(
    message: String,
    cause: Throwable? = null
) : RuntimeException(message, cause)

// 도메인 모델을 찾지 못한 경우 — 404 매핑
class diagnostic_result_not_found_exception(
    message: String
) : diagnostic_result_domain_exception(message)

// 도메인 검증 실패 — 400 매핑
class diagnostic_result_validation_exception(
    message: String
) : diagnostic_result_domain_exception(message)

// 지금 상태와 맞지 않는 요청(다른 곳이 처리 중 · 이미 끝난 행 등) — 409 매핑
class diagnostic_result_conflict_exception(
    message: String
) : diagnostic_result_domain_exception(message)
```

application 레이어의 포트 예외(infrastructure 가 던지고 advice 가 매핑) — `K/application/ports/i_analysis_agent_port.kt:80-84`, `K/application/ports/i_record_document_port.kt:15-19`
```kotlin
// 에이전트가 설정되지 않았다 — API 키 미설정 환경. 503 으로 매핑한다(부팅은 막지 않는다)
class analysis_agent_unavailable_exception(message: String) : RuntimeException(message)

// 게이트웨이가 분석을 완료하지 못했다 — 호출 실패 · status != done · output 없음. 502 로 매핑한다
class analysis_agent_failure_exception(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
```
```kotlin
// 그 원천 모듈의 서식 API 주소가 설정돼 있지 않다 — 503 으로 매핑한다(검사결과 조회는 정상, 서식만 못 연다)
class record_document_unavailable_exception(message: String) : RuntimeException(message)

// 원천 모듈 호출이 실패했다(타임아웃 · 연결 실패 · 2xx 아님 · 빈 본문) — 502 로 매핑한다. 원인 체인은 로그에만 남긴다
class record_document_fetch_exception(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
```

### 7.2 매핑표

| HTTP | `error` | 예외 | 처리기 |
|---|---|---|---|
| 401·403 | 라이브러리 코드(`AUTH_TOKEN_MISSING` …) | `JwtAuthenticationException` | 둘 다 |
| 404 | `NotFound` | `diagnostic_result_not_found_exception` | 둘 다 |
| 400 | `Validation` | `diagnostic_result_validation_exception` · `command_validation_exception`(details 포함) · `IllegalArgumentException` · `MethodArgumentTypeMismatchException` · `HttpMessageNotReadableException` · (inbox 만) `MissingServletRequestParameterException` | 둘 다 |
| 409 | `Conflict` | `diagnostic_result_conflict_exception` · `OptimisticLockingFailureException` | 전역(낙관락은 전역만) |
| 422 | `DomainRuleViolation` | 위에서 안 잡힌 `diagnostic_result_domain_exception` | 전역 |
| 503 | `AgentUnavailable` / `RecordDocumentUnavailable` / (inbox) `AlertUnavailable` | 포트 예외 | 전역 / inbox |
| 502 | `AgentFailure` / `RecordDocumentFetch` / (inbox) `AlertFailure` | 포트 예외 | 전역 / inbox |
| 500 | `InternalError` | `Exception` — 고정 문구, 스택은 로그만 | 둘 다 |

### 7.3 전역 advice 전체

`K/infrastructure/config/global_exception_handler.kt:22-105`
```kotlin
// 도메인 예외 → HTTP 매핑 단일 진입점
// 내부 stacktrace / 메시지를 그대로 노출하지 않고, error 는 카테고리 식별자만 노출
@RestControllerAdvice
class global_exception_handler {

    private val logger = KotlinLogging.logger {}

    // 401 · 403 — 인증 실패(@RequireAuth · @TokenPrincipal). 상태 · 코드는 라이브러리 에러코드가 정한다
    // error 에 wire 코드(AUTH_TOKEN_MISSING · AUTH_TOKEN_EXPIRED …)를 실어 화면이 `AUTH_` 접두로 분기할 수 있게 한다
    @ExceptionHandler(JwtAuthenticationException::class)
    fun handle_authentication(ex: JwtAuthenticationException): ResponseEntity<error_response> {
        logger.info { "Authentication: ${ex.errorCode}" }
        return ResponseEntity.status(ex.errorCode.status).body(
            error_response(error = ex.errorCode.name, message = ex.errorCode.defaultMessage)
        )
    }

    // 404 — 도메인 모델 미존재
    @ExceptionHandler(diagnostic_result_not_found_exception::class)
    fun handle_not_found(ex: diagnostic_result_not_found_exception): ResponseEntity<error_response> {
        logger.info { "NotFound: ${ex.message}" }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
            error_response(error = "NotFound", message = ex.message ?: "리소스를 찾을 수 없습니다")
        )
    }

    // 400 — 도메인/커맨드 검증 실패, IllegalArgumentException 포함
    @ExceptionHandler(
        diagnostic_result_validation_exception::class,
        command_validation_exception::class,
        IllegalArgumentException::class
    )
    fun handle_validation(ex: Exception): ResponseEntity<error_response> {
        logger.info { "Validation: ${ex.message}" }
        val details: List<String> = if (ex is command_validation_exception) ex.errors else emptyList()
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
            error_response(error = "Validation", message = ex.message ?: "요청 검증에 실패했습니다", details = details)
        )
    }

    // 400 — 요청 파라미터 · 본문 형식 오류(예: patient_id 가 UUID 가 아님). 처리기가 없으면 아래 500 으로 떨어진다
    // 응답에는 파라미터 이름만 싣는다 — 받은 값(환자 식별자일 수 있다)은 싣지 않는다
    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
    fun handle_type_mismatch(ex: MethodArgumentTypeMismatchException): ResponseEntity<error_response> {
        logger.info { "Validation: 파라미터 형식 오류 ${ex.name}" }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
            error_response(error = "Validation", message = "요청 파라미터 형식이 올바르지 않습니다: ${ex.name}")
        )
    }

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handle_unreadable(ex: HttpMessageNotReadableException): ResponseEntity<error_response> {
        logger.info { "Validation: 요청 본문 해석 실패" }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
            error_response(error = "Validation", message = "요청 본문 형식이 올바르지 않습니다")
        )
    }

    // 409 — 지금 상태와 맞지 않는 요청(inbox 운영 — 다른 곳이 처리 중 · 재처리할 수 없는 상태)
    @ExceptionHandler(diagnostic_result_conflict_exception::class)
    fun handle_state_conflict(ex: diagnostic_result_conflict_exception): ResponseEntity<error_response> {
        logger.info { "Conflict: ${ex.message}" }
        return ResponseEntity.status(HttpStatus.CONFLICT).body(
            error_response(error = "Conflict", message = ex.message ?: "지금 상태에서 할 수 없는 요청입니다")
        )
    }

    // 422 — 도메인 불변식 위반 (위에서 처리 안 된 모든 *DomainException)
    @ExceptionHandler(diagnostic_result_domain_exception::class)
    fun handle_domain_rule_violation(ex: diagnostic_result_domain_exception): ResponseEntity<error_response> {
        logger.warn { "DomainRuleViolation: ${ex.message}" }
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(
            error_response(error = "DomainRuleViolation", message = ex.message ?: "도메인 규칙을 위반했습니다")
        )
    }

    // 409 — 낙관락 충돌
    @ExceptionHandler(OptimisticLockingFailureException::class)
    fun handle_conflict(ex: OptimisticLockingFailureException): ResponseEntity<error_response> {
        logger.warn { "Conflict: ${ex.message}" }
        return ResponseEntity.status(HttpStatus.CONFLICT).body(
            error_response(error = "Conflict", message = "다른 트랜잭션과 충돌했습니다. 다시 시도해 주세요")
        )
    }
```

`K/infrastructure/config/global_exception_handler.kt:107-160`
```kotlin
    // 503 — 사용할 분석 에이전트가 없다(API 키 미설정). 검사결과 조회는 정상이고 AI 분석만 쓸 수 없다
    @ExceptionHandler(analysis_agent_unavailable_exception::class)
    fun handle_agent_unavailable(ex: analysis_agent_unavailable_exception): ResponseEntity<error_response> {
        logger.warn { "AgentUnavailable: ${ex.message}" }
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(
            error_response(error = "AgentUnavailable", message = ex.message ?: "AI 분석을 사용할 수 없습니다")
        )
    }

    // 502 — AI Gateway 가 분석을 완료하지 못했다(호출 실패 · status != done · output 없음). 원인 체인은 로그에만 남는다
    @ExceptionHandler(analysis_agent_failure_exception::class)
    fun handle_agent_failure(ex: analysis_agent_failure_exception): ResponseEntity<error_response> {
        logger.warn(ex) { "AgentFailure: ${ex.message}" }
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(
            error_response(error = "AgentFailure", message = ex.message ?: "AI 분석에 실패했습니다")
        )
    }

    // 503 — 그 원천 모듈의 서식 API 주소가 설정돼 있지 않다. 검사결과 조회는 정상이고 서식만 못 연다
    @ExceptionHandler(record_document_unavailable_exception::class)
    fun handle_record_document_unavailable(ex: record_document_unavailable_exception): ResponseEntity<error_response> {
        logger.warn { "RecordDocumentUnavailable: ${ex.message}" }
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(
            error_response(error = "RecordDocumentUnavailable", message = ex.message ?: "서식 원천 모듈이 연결되지 않았습니다")
        )
    }

    // 502 — 원천 모듈이 서식을 돌려주지 못했다(호출 실패 · 2xx 아님 · 빈 본문). 원인 체인은 로그에만 남는다
    @ExceptionHandler(record_document_fetch_exception::class)
    fun handle_record_document_fetch(ex: record_document_fetch_exception): ResponseEntity<error_response> {
        logger.warn(ex) { "RecordDocumentFetch: ${ex.message}" }
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(
            error_response(error = "RecordDocumentFetch", message = ex.message ?: "서식을 가져오지 못했습니다")
        )
    }

    // 500 — 미분류 예외.
    // 응답에는 고정 문구만 준다 — 예외 클래스 · 메시지 체인은 SQL · 식별자 · 내부 구조를 드러낼 수 있다.
    // 원인은 로그(스택 포함)로만 남긴다.
    @ExceptionHandler(Exception::class)
    fun handle_all(ex: Exception): ResponseEntity<error_response> {
        logger.error(ex) { "처리되지 않은 예외" }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
            error_response(error = "InternalError", message = "서버 내부 오류가 발생했습니다")
        )
    }
}

// 공통 에러 응답 스키마
data class error_response(
    val error: String,
    val message: String,
    val details: List<String> = emptyList()
)
```

### 7.4 inbox 운영 API 전용 advice (BC 간 공통 계약)

`K/presentation/inbox_error_advice.kt:21-69`
```kotlin
/**
 * 수신 inbox 운영 API(classdiagram §1.9)의 오류 응답 — **inbox 를 쓰는 모든 서비스가 같은 본문 · 같은 코드**를 낸다(x-patient-appointment 와 같은 파일).
 *
 * 본문은 `{ error, message }` — `error` 는 카테고리 코드(`.claude/docs/rules/api-layer.md` 의 예외 → HTTP 매핑), `message` 는 사용자에게 보일 사유.
 * 화면(x-ancillary-main)은 `response.data.message` 로 사유를 읽고 상태 코드로 가른다.
 * `inbox_messages_controller` 에만 걸리고(`assignableTypes`) 이 BC 의 `global_exception_handler` 보다 먼저 고른다(`HIGHEST_PRECEDENCE`) —
 * 이 BC 의 다른 API 오류 모양은 건드리지 않는다.
 *
 * | 상태 | error |
 * |---|---|
 * | 400 | `Validation` — 입력 검증 · 파라미터 형식 · 본문 해석 실패 · 파라미터 누락 |
 * | 401 · 403 | 인증 라이브러리 코드(`AUTH_TOKEN_MISSING` · `AUTH_TOKEN_EXPIRED` …) |
 * | 404 | `NotFound` |
 * | 409 | `Conflict` — 다른 곳에서 처리 중 · 지금 상태에서 할 수 없는 동작 |
 * | 502 · 503 | `AlertFailure` · `AlertUnavailable` — 알림 보내기만(classdiagram §1.10 · 알림을 넣은 서비스만) |
 * | 500 | `InternalError` — 고정 문구(원인은 로그로만) |
 */
@RestControllerAdvice(assignableTypes = [inbox_messages_controller::class])
@Order(Ordered.HIGHEST_PRECEDENCE)
class inbox_error_advice {

    private val log = LoggerFactory.getLogger(inbox_error_advice::class.java)

    @ExceptionHandler(JwtAuthenticationException::class)
    fun handle_authentication(ex: JwtAuthenticationException): ResponseEntity<inbox_error_response> =
        respond(ex.errorCode.status, ex.errorCode.name, ex.errorCode.defaultMessage)

    @ExceptionHandler(diagnostic_result_not_found_exception::class)
    fun handle_not_found(ex: diagnostic_result_not_found_exception): ResponseEntity<inbox_error_response> =
        respond(HttpStatus.NOT_FOUND, NOT_FOUND, ex.message)

    @ExceptionHandler(diagnostic_result_conflict_exception::class)
    fun handle_conflict(ex: diagnostic_result_conflict_exception): ResponseEntity<inbox_error_response> =
        respond(HttpStatus.CONFLICT, CONFLICT, ex.message)

    @ExceptionHandler(inbox_alert_unavailable_exception::class)
    fun handle_alert_unavailable(ex: inbox_alert_unavailable_exception): ResponseEntity<inbox_error_response> =
        respond(HttpStatus.SERVICE_UNAVAILABLE, ALERT_UNAVAILABLE, ex.message)

    // 원인 체인(상대 응답 본문)은 로그로만 — 사유에는 상대 오류 코드까지만 싣는다
    @ExceptionHandler(inbox_alert_failure_exception::class)
    fun handle_alert_failure(ex: inbox_alert_failure_exception): ResponseEntity<inbox_error_response> {
        log.warn("inbox 알림 보내기 실패: {}", ex.message, ex.cause)
        return respond(HttpStatus.BAD_GATEWAY, ALERT_FAILURE, ex.message)
    }

    @ExceptionHandler(diagnostic_result_validation_exception::class, IllegalArgumentException::class)
    fun handle_validation(ex: Exception): ResponseEntity<inbox_error_response> =
        respond(HttpStatus.BAD_REQUEST, VALIDATION, ex.message)
```

`K/presentation/inbox_error_advice.kt:71-111`
```kotlin
    // 받은 값(식별자일 수 있다)은 싣지 않는다 — 파라미터 이름만
    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
    fun handle_type_mismatch(ex: MethodArgumentTypeMismatchException): ResponseEntity<inbox_error_response> =
        respond(HttpStatus.BAD_REQUEST, VALIDATION, "요청 파라미터 형식이 올바르지 않습니다: ${ex.name}")

    @ExceptionHandler(MissingServletRequestParameterException::class)
    fun handle_missing_parameter(ex: MissingServletRequestParameterException): ResponseEntity<inbox_error_response> =
        respond(HttpStatus.BAD_REQUEST, VALIDATION, "요청 파라미터가 없습니다: ${ex.parameterName}")

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handle_unreadable(ex: HttpMessageNotReadableException): ResponseEntity<inbox_error_response> =
        respond(HttpStatus.BAD_REQUEST, VALIDATION, "요청 본문 형식이 올바르지 않습니다")

    // 응답에는 고정 문구만 준다 — 예외 메시지 체인은 SQL · 식별자 · 내부 구조를 드러낼 수 있다
    @ExceptionHandler(Exception::class)
    fun handle_all(ex: Exception): ResponseEntity<inbox_error_response> {
        log.error("inbox 운영 API 처리되지 않은 예외", ex)
        return respond(HttpStatus.INTERNAL_SERVER_ERROR, INTERNAL_ERROR, "서버 내부 오류가 발생했습니다")
    }

    private fun respond(status: HttpStatusCode, error: String, message: String?): ResponseEntity<inbox_error_response> {
        if (status.is4xxClientError) log.info("inbox 운영 API {}: {}", error, message)
        return ResponseEntity.status(status).body(inbox_error_response(error, message ?: DEFAULT_MESSAGE))
    }

    companion object {
        const val VALIDATION = "Validation"
        const val NOT_FOUND = "NotFound"
        const val CONFLICT = "Conflict"
        const val INTERNAL_ERROR = "InternalError"
        const val ALERT_UNAVAILABLE = "AlertUnavailable"
        const val ALERT_FAILURE = "AlertFailure"
        private const val DEFAULT_MESSAGE = "요청을 처리하지 못했습니다"
    }
}

/** 운영 API 오류 본문 — `error` = 카테고리 코드, `message` = 사용자에게 보일 사유. */
data class inbox_error_response(
    val error: String,
    val message: String,
)
```

인증 예외의 출처 — 필터는 해석만, 거절은 `@RequireAuth` aspect — `K/infrastructure/config/security_config.kt:12-30`
```kotlin
// 인증 — common-jwt-kt 소비자.
// - 필터 체인은 토큰을 **해석만** 한다(JwtAuthenticationFilter → SecurityContext · MDC userId/tenantId/hospitalCode).
//   막는 일은 컨트롤러의 @RequireAuth(aspect)가 한다 — 토큰이 없거나 틀리면 401, global_exception_handler 가 응답을 만든다.
//   그래서 체인은 permitAll 이다(actuator probe · swagger 는 토큰 없이 열려 있어야 한다).
// - ⚠️ 이 빈을 직접 정의하면 라이브러리 기본 체인이 꺼진다 — JwtAuthenticationFilter 를 여기서 꼭 등록한다.
//   빠지면 기동 로그에 ERROR("모든 요청이 인증 없이 통과됩니다")가 뜨고 @RequireAuth 가 전부 401 이 된다.
@Configuration
@EnableWebSecurity
class security_config {
    @Bean
    fun security_filter_chain(http: HttpSecurity, jwt_authentication_filter: JwtAuthenticationFilter): SecurityFilterChain {
        http
            .csrf { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .addFilterBefore(jwt_authentication_filter, UsernamePasswordAuthenticationFilter::class.java)
            .authorizeHttpRequests { auth -> auth.anyRequest().permitAll() }
        return http.build()
    }
}
```

토큰 → 테넌트 범위 · 직원 id(401 의 또 다른 출처) — `K/presentation/token_principal_scope.kt:10-20`
```kotlin
// 토큰 주체 → 테넌트 범위 · 직원 id. HTTP 요청의 tenant_id · hospital_code · user_id 는 이 파일에서만 나온다.
// AuthPrincipal 은 presentation 밖으로 내보내지 않는다 — application 이하에는 tenant_scope 와 UUID 만 넘긴다.

// claim 이 비어 있으면 tenant_scope.of 가 INVALID_DATA 로 채운다(라이브러리가 필수 claim 을 검증하므로 정상 토큰에서는 생기지 않는다)
fun AuthPrincipal.to_tenant_scope(): tenant_scope = tenant_scope.of(tenantId, hospitalCode)

// 토큰 sub = 직원 레코드 uuid(shared 직원 id). UUID(하이픈 포함 36자 표준 형식 · strict_uuid_or_null)가 아니면
// 이 BC 가 쓸 수 없는 토큰이라 401(AUTH_TOKEN_INVALID)로 거절한다
fun AuthPrincipal.employee_id(): UUID =
    strict_uuid_or_null(userId)
        ?: throw JwtAuthenticationException(JwtErrorCode.AUTH_TOKEN_INVALID, "토큰 사용자 ID 가 UUID 형식이 아닙니다")
```

---

## 8. 설정·부팅

### 8.1 메인 클래스 — Spring 기동 전에 JVM 시간대를 병원 시간대로

`K/diagnostic_result_application.kt:1-23`
```kotlin
package com.projectx.diagnosticresult

import com.projectx.diagnosticresult.infrastructure.config.hospital_time_zone
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.data.jpa.repository.config.EnableJpaRepositories

/**
 * diagnostic-result MS 부트스트랩.
 * - 외부 BC 의 검사결과 integration event 를 Kafka 로 수신해 `observation` 트리에 적재한다
 *   (검체진단 · 영상검사 · 과내기능검사 · 임상관찰기록).
 * - 환자별 통합 검사결과 조회 API 와 사용자별 화면 설정 API 를 제공한다.
 * - EventBus 는 common-eventbus-kt 의 자동설정이 올린다(라우팅은 infrastructure/eventbus/topic_registry).
 */
@SpringBootApplication
@EnableJpaRepositories(basePackages = ["com.projectx.diagnosticresult.infrastructure"])
class diagnostic_result_application

fun main(args: Array<String>) {
    // 시각은 병원 현지시각 하나로 다룬다 — JVM 기본 시간대(로그 · JDBC 세션)를 Spring 기동 전에 맞춘다
    hospital_time_zone.apply_as_default()
    runApplication<diagnostic_result_application>(*args)
}
```

`K/infrastructure/config/hospital_time.kt:23-56`
```kotlin
/**
 * 병원 시간대 — 이 MS 의 시각은 오프셋이 붙은 절대 시각(`Instant` · timestamptz)이고, 병원 시간대는
 * **날짜 경계 · API 표기 · 오프셋 없는 수신값의 해석**에만 쓴다(classdiagram §공통 「시각」).
 *
 * 값의 출처는 환경변수 [ENV] 하나다(없으면 [DEFAULT]). Spring 설정이 아니라 환경변수인 이유는
 * JVM 기본 시간대를 Spring 이 뜨기 **전에** 맞춰야 해서다 — 로그 · JDBC 세션 시간대가 그 값을 따른다.
 */
object hospital_time_zone {

    const val ENV = "HOSPITAL_TIME_ZONE"
    const val DEFAULT = "Asia/Seoul"

    /** 병원 시간대. 잘못된 값이면 부팅을 멈춘다 — 조용히 UTC 로 떨어지면 모든 시각이 9시간 어긋난다. */
    fun zone(): ZoneId = ZoneId.of(System.getenv(ENV)?.takeIf { it.isNotBlank() } ?: DEFAULT)

    /** JVM 기본 시간대를 병원 시간대로 맞춘다 — `main` 이 Spring 기동 전에 부른다. */
    fun apply_as_default() {
        TimeZone.setDefault(TimeZone.getTimeZone(zone()))
    }
}

@Configuration
class hospital_time_config {

    /** 「지금」의 유일한 출처 — `Instant.now(clock)` · `LocalDate.now(clock)` 로만 읽는다. */
    @Bean
    fun hospital_clock(): Clock = Clock.system(hospital_time_zone.zone())

    /** 공용 ObjectMapper(수신 payload 역직렬화)에 수신 시각 해석을 얹는다 — Boot 가 JacksonModule 빈을 자동 등록한다. */
    @Bean
    fun hospital_offset_date_time_module(hospital_clock: Clock): JacksonModule =
        SimpleModule("hospital-offset-date-time")
            .addDeserializer(OffsetDateTime::class.java, hospital_offset_date_time_deserializer(hospital_clock.zone))
}
```

### 8.2 `application.yml` — DB · search_path · Liquibase · JPA

`R/application.yml:1-56`
```yaml
# 값은 전부 환경변수로 받고 기본값은 로컬 기준이다 — 로컬은 `backend/.env`(spring-dotenv).
#   dev 차이는 두 곳에서만 온다:
#     · application-dev.yml — 전 BC 공통 dev 값(이벤트버스 kafka_namespace)
#     · 배포레포 values.yaml 의 configData + vault Secret — 이 배포만의 값(포트 · DB · 스키마 · 게이트웨이 IP)
#   같은 값을 두 곳에 두지 않는다(어긋나면 조용히 실패한다).
server:
  # 로컬·dev 모두 5351 — **배포값과 같은 포트를 로컬에서도 쓴다**(포트가 갈리면 로컬에서만 되는 배선이 생긴다).
  # ⚠️ 배포 values 의 containerPort · servicePort · 라우팅 VS destination 과 같은 값이어야 한다.
  port: ${SERVER_PORT:5351}
  # ⚠️ context-path 를 두지 않는다(= "/"). API prefix `/api/v1/diagnostic-result` 는 **컨트롤러가 소유**한다 —
  #    그래야 문서가 `/spec/diagnostic-result/**`, actuator 가 `/actuator/**`(k8s probe 경로) 로 남는다.

spring:
  # 공통 라이브러리 default 는 common-libs/*.yaml 로 분리 — classpath 에 포장되어 부팅 시 자동 흡수.
  config:
    import:
      - classpath:common-libs/common-masking.yaml
      - classpath:common-libs/common-observability.yaml
  application:
    name: diagnostic-result

  # DB: 카탈로그 `sup`(로컬·dev 동일) / **MS 전용 스키마 `diagnostic_result`**(= 모듈명 snake_case).
  #   스키마는 앱이 부팅하며 직접 만든다(db_schema_initializer, Liquibase 직전 멱등 CREATE SCHEMA).
  #   **계정/비밀번호는 기본값을 두지 않는다** — 자격증명을 소스에 남기지 않고, 누락 시 부팅이 즉시 실패한다.
  datasource:
    url: jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5432}/${DB_NAME:sup}
    username: ${DB_USER}
    password: ${DB_PASSWORD}
    driver-class-name: org.postgresql.Driver
    hikari:
      # 커넥션 오픈 시마다 실행된다 — search_path 고정만 한다(이 MS 는 자기 스키마만 본다).
      # ⚠️ 여기에 CREATE SCHEMA 를 넣지 않는다 — 커넥션마다 돌아 동시 오픈 시 pg_namespace 충돌로 실패한다.
      connection-init-sql: SET search_path TO ${DB_SCHEMA:diagnostic_result}

  jpa:
    hibernate:
      # DDL 은 Liquibase 가 부팅 시 적용한다. Hibernate 는 검증만.
      ddl-auto: ${SPRING_JPA_DDL_AUTO:validate}
    open-in-view: false
    # 감사 컬럼 리스너(audit_entity_listener)를 기본 엔티티 리스너로 등록하는 orm.xml.
    #   ⚠️ 빠지면 감사 컬럼이 EPOCH · null 로 저장된다(부팅 · 빌드 모두 통과한다).
    mapping-resources:
      - META-INF/orm.xml
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect

  # Liquibase — 부팅 시 자동 적용. 파괴적 DDL 은 수동 expand/contract(루트 CLAUDE.md).
  #   스키마명은 DB_SCHEMA 한 값에서 나온다 — hikari search_path 와 같은 출처. changelog 에는 스키마명을 쓰지 않는다.
  #   ⚠️ **샘플 시드는 두지 않는다** — contexts 기본값을 빈 값으로 고정한다. 시드가 배포본에 남으면
  #      실데이터·수신 이벤트와 섞여 화면 검증이 오염된다(배포 전 제거가 아니라 애초에 안 만든다).
  liquibase:
    change-log: classpath:db/changelog/db.changelog-master.yaml
    default-schema: ${DB_SCHEMA:diagnostic_result}
    liquibase-schema: ${DB_SCHEMA:diagnostic_result}
    contexts: ${LIQUIBASE_CONTEXTS:}
```

이 BC 전용 설정 블록(테넌트 · inbox · AI · 서식) — `R/application.yml:135-156`, `:179-206`
```yaml
diagnosticresult:
  # 이 배포가 맡은 테넌트 · 병원 — 수신 이벤트의 테넌트를 정하는 기준값(event_tenant_resolver).
  #   k8s 는 ConfigMap x-ehr-tenant-config(TENANT_ID · HOSPITAL_CODES, 콤마 구분 · 첫 값 = 기본 병원)가 준다.
  #   값이 없으면 INVALID_DATA — 봉투에도 값이 없는 수신분이 INVALID_DATA 로 적재된다(부팅은 막지 않는다).
  #   HTTP 요청은 이 값이 아니라 토큰의 tenantId · hospitalCode 를 쓴다.
  tenant:
    tenant-id: ${TENANT_ID:INVALID_DATA}
    hospital-codes: ${HOSPITAL_CODES:INVALID_DATA}

  # 수신 inbox(classdiagram §1.8) — 수신은 inbox_message 에 적재만 하고 처리 · 재시도는 inbox 처리기가 한다.
  #   실패하면 backoff-seconds 만큼 기다려 다시 하고(마지막 값 반복), max-retry 에 닿거나 영구 실패면 FAILED 로 남는다
  #   (같은 토픽 · 같은 키의 뒤 행이 멈춘다 — ERROR 로그 「inbox 처리 실패 확정」).
  #   ⚠️ lease-seconds 는 process-timeout-seconds 보다 길어야 한다 — 짧으면 처리 중에 회수돼 다른 파드가 같은 행을 또 처리한다.
  inbox:
    max-retry: ${INBOX_MAX_RETRY:10}
    backoff-seconds: ${INBOX_BACKOFF_SECONDS:60,300,1800,3600}
    lease-seconds: ${INBOX_LEASE_SECONDS:120}
    process-timeout-seconds: ${INBOX_PROCESS_TIMEOUT_SECONDS:60}
    sweep-interval-ms: ${INBOX_SWEEP_INTERVAL_MS:30000}
    sweep-batch-size: ${INBOX_SWEEP_BATCH_SIZE:100}
    executor-pool-size: ${INBOX_EXECUTOR_POOL_SIZE:4}
    executor-queue-capacity: ${INBOX_EXECUTOR_QUEUE_CAPACITY:1000}
```
```yaml
  # Observation_Analyze_Result · 테스트조회 자유 질의 — AI Gateway 호출.
  #   키가 비어 있으면 에이전트를 만들지 않고 분석 · 질의만 503 이다 — AI 는 부가기능이라 부팅을 막지 않는다.
  analysis:
    # Kakao AI Gateway `POST /v2/chat/complete`(deep agent, X-API-Key) — 이 BC 의 유일한 AI 경로.
    #   에이전트 · 모델 · 경로를 이 BC 가 고르지 않는다 — 메시지를 보내면 게이트웨이의 deep agent 가 처리할 에이전트를 정한다.
    #   분석은 output_schema 로 구조화 출력을 받아 그 JSON 을 보고서로 저장하고, 실제로 쓰인 에이전트(agents)는 이력(agent_version)에 남긴다.
    #   URL 과 API 키는 여기(application.yml)에 둔다 — 환경변수(AI_GATEWAY_BASE_URL · AI_GATEWAY_API_KEY)가 있으면 그 값이 우선한다.
    ai-gateway:
      base-url: ${AI_GATEWAY_BASE_URL:https://dev-ai-gateway-api.kakaohealthcare.com}
      api-key: ${AI_GATEWAY_API_KEY:wak_…(교육자료에서는 가림 — 소스에는 평문 키가 기본값으로 들어 있다)}
      connect-timeout-ms: ${AI_GATEWAY_CONNECT_TIMEOUT_MS:60000}
      read-timeout-ms: ${AI_GATEWAY_READ_TIMEOUT_MS:60000}
      max-retries: ${AI_GATEWAY_MAX_RETRIES:1}
      # [DNS 오버라이드] 클러스터 안 hairpin 우회 — dev 값은 application-dev.yml 이 갖는다(configData AI_GATEWAY_RESOLVE_HOST/IP 로 덮어쓸 수 있다).
      resolve-host: ${AI_GATEWAY_RESOLVE_HOST:}
      resolve-ip: ${AI_GATEWAY_RESOLVE_IP:}

  # 서식(기록 문서) 원천 모듈 — observation.record_id 로 HTML 을 받아 중계한다(GET /observations/{id}/record-document).
  #   키 = 원천 모듈 코드(분류 코드의 kebab-case), 값 = `{record_id}` 자리를 가진 URL 템플릿.
  #   비어 있는 모듈의 서식은 503(RecordDocumentUnavailable) — 서식 보기는 부가기능이라 부팅을 막지 않는다.
  #   원천 모듈의 서식 API 는 상대 BC 가 정한다 — 계약 확정 전까지 기본값은 빈 값이다(상대 BC 계약 대기).
  record-document:
    connect-timeout-ms: ${RECORD_DOCUMENT_CONNECT_TIMEOUT_MS:3000}
    read-timeout-ms: ${RECORD_DOCUMENT_READ_TIMEOUT_MS:10000}
    sources:
      specimen-diagnostics: ${RECORD_DOCUMENT_SPECIMEN_DIAGNOSTICS_URL:}
      functional-diagnostics: ${RECORD_DOCUMENT_FUNCTIONAL_DIAGNOSTICS_URL:}
      diagnostic-imaging: ${RECORD_DOCUMENT_DIAGNOSTIC_IMAGING_URL:}
```
(⚠ `R/application.yml:188` 의 `api-key` 기본값은 **실제 평문 키**다. 인용에서는 가렸다 — §11 참고.)

`.env` 로더 등록(Boot 4 호환) — `R/META-INF/spring.factories:1-4`
```
# .env 로더(spring-dotenv)를 Boot 4 호환 EnvironmentPostProcessor 로 등록.
# (spring-dotenv 4.0.0 의 SpringApplicationRunListener 자동 등록은 Boot 4 에서 동작하지 않음)
org.springframework.boot.EnvironmentPostProcessor=\
  com.projectx.diagnosticresult.infrastructure.config.dotenv_environment_post_processor
```

감사 리스너 등록(ArchUnit 때문에 orm.xml) — `R/META-INF/orm.xml:1-19`
```xml
<?xml version="1.0" encoding="UTF-8"?>
<!--
  JPA 기본 엔티티 리스너 등록 — 모든 Entity 의 감사 컬럼(created_* · updated_*)을 audit_entity_listener 가 채운다.
  Entity 클래스에 @EntityListeners 를 달면 domain 이 infrastructure 를 참조하게 되므로(ArchUnit 위반) 여기서 등록한다.
  application.yml 의 spring.jpa.mapping-resources 가 이 파일을 읽는다 — 그 설정이 빠지면 감사 컬럼이 EPOCH · null 로 저장된다.
  ⚠️ 리스너 FQCN 은 문자열이라 컴파일러가 못 잡는다 — 클래스를 옮기거나 이름을 바꾸면 여기도 고친다.
-->
<entity-mappings xmlns="https://jakarta.ee/xml/ns/persistence/orm"
                 xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                 xsi:schemaLocation="https://jakarta.ee/xml/ns/persistence/orm https://jakarta.ee/xml/ns/persistence/orm/orm_3_1.xsd"
                 version="3.1">
    <persistence-unit-metadata>
        <persistence-unit-defaults>
            <entity-listeners>
                <entity-listener class="com.projectx.diagnosticresult.infrastructure.persistence.audit_entity_listener"/>
            </entity-listeners>
        </persistence-unit-defaults>
    </persistence-unit-metadata>
</entity-mappings>
```

`K/infrastructure/persistence/audit_entity_listener.kt:27-44`
```kotlin
class audit_entity_listener {

    @PrePersist
    fun on_pre_persist(target: Any) {
        (target as? entity)?.record_creation(Instant.now(), current_employee_id())
    }

    @PreUpdate
    fun on_pre_update(target: Any) {
        (target as? entity)?.record_modification(Instant.now(), current_employee_id())
    }

    // 비로그인 발행(anonymous)은 null — 형식 검사는 strict_uuid_or_null(하이픈 포함 36자 표준 형식만)
    private fun current_employee_id(): UUID? =
        MDC.get(JwtKeys.USER_ID)
            ?.takeIf { it.trim() != MessageEnvelope.ANONYMOUS_USER_ID }
            ?.let(::strict_uuid_or_null)
}
```

### 8.3 `db_schema_initializer` — Liquibase 직전 멱등 `CREATE SCHEMA`

`K/infrastructure/config/db_schema_initializer.kt:11-73`
```kotlin
/**
 * 이 MS 전용 DB 스키마를 **앱이 부팅하며 직접 생성**한다(멱등 `CREATE SCHEMA IF NOT EXISTS`).
 *
 * 스키마는 MicroService 단위다(루트 CLAUDE.md §0 DB 스키마) — 이 MS 는 `diagnostic_result` 하나만 본다.
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
    @Value("\${DB_SCHEMA:diagnostic_result}") private val schema_name: String,
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
                logger.info(
                    "DB 스키마 확인/생성 완료 — db={}, schema={}",
                    connection.catalog,
                    schema_name,
                )
            }
        } catch (exception: Exception) {
            logger.warn(
                "DB 스키마 생성 실패 — schema={}, 원인={}. 계정에 CREATE 권한이 있는지 확인하라"
                    + "(DBA 선행 생성 경로: _docs/db-bootstrap/)",
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

### 8.4 Liquibase master + changeSet 1개

`R/db/changelog/db.changelog-master.yaml:1-33`
```yaml
# diagnostic-result 스키마 마스터 changelog.
# 스키마(diagnostic_result)는 **앱이 부팅하며 직접 만든다**(infrastructure/config/db_schema_initializer, 멱등).
# 스키마는 MicroService 단위다(루트 CLAUDE.md §0 DB 스키마) — MS 1개 = 스키마 1개, DATABASECHANGELOG 도 그 안에 있다.
# 계정·권한은 DBA 가 선행 생성한다(_docs/db-bootstrap/). 앱 계정에 DB CREATE 권한이 필요하다.
# 테이블·제약 이름은 .claude/docs/rules/conventions.md §A-1 을 따른다(테이블명 = 클래스명, 접두 없음).
#
# ⚠️ **적용된 changeSet 은 고치지 않는다** — 내용이 checksum 이라 주석 한 줄만 바꿔도 이미 적용된 DB 에서
#    `ValidationFailedException: changesets check sum` 으로 **부팅이 막힌다**(컴파일·테스트는 전부 통과한다).
#    스키마를 바꿔야 하면 `changes/00N-*.xml` 을 새로 추가한다.
#
# ⚠️ **샘플 시드 changeSet 을 두지 않는다.** 배포본에 남으면 실데이터·수신 이벤트와 섞여 화면 검증이
#    오염된다. 테스트 데이터가 필요하면 시드가 아니라 **발행측(x-ancillary-main 케이스 생성기 등)**이
#    실제 이벤트를 흘려보내 만든다 — 이 MS 는 평소처럼 수신만 한다.
#
# 최초 스키마는 changes/001-create-tables.xml 한 벌이다 — 모든 Entity 테이블이 「PK(<테이블명>_id) → tenant_id · hospital_code
#    → 업무 컬럼 → 감사 4종」 순서를 지키도록 처음부터 만든다. 이전 changelog(001~005)로 만든 DB 는
#    수동 트리(manual)의 001-reset-schema.sql 로 사람이 초기화한 뒤 기동한다.
databaseChangeLog:
  - include:
      file: changes/001-create-tables.xml
      relativeToChangelogFile: true
  # observation.record_id — 원천 모듈 서식(HTML) 식별자. 서식 본문은 저장하지 않는다(화면이 열 때 원천 모듈에서 중계).
  - include:
      file: changes/002-add-observation-record-id.xml
      relativeToChangelogFile: true
  # analysis_review_target — 환자 AI 재점검 대상여부(환자당 1행). 결과 적재 · 변경이 켜고 분석 시작이 끈다 + 기존 데이터 대상 채우기.
  - include:
      file: changes/003-create-analysis-review-target.xml
      relativeToChangelogFile: true
  # inbox_message — 수신 원문 inbox(적재와 처리를 나눈다 · 처리 실패는 DLT 가 아니라 행 상태로 남는다).
  - include:
      file: changes/004-create-inbox-message.xml
      relativeToChangelogFile: true
```
(이어서 `:34-65` 에 005 ~ 012 include. 012 까지 있고 `R/db/manual/001~004-*.sql` 은 master 에 include 되지 않는 수동 DROP 스크립트다.)

001 의 머리 주석(컬럼 순서 규약) — `R/db/changelog/changes/001-create-tables.xml:1-14`
```xml
<?xml version="1.0" encoding="UTF-8"?>
<!--
  diagnostic-result 최초 스키마 — 테이블 · 제약 · 인덱스 전부.
  명명은 .claude/docs/rules/conventions.md §A-1 을 따른다 — 테이블명 = 클래스명(snake_case), 서비스·도메인 접두 없음
  (스키마 diagnostic_result 가 MS 경계라 접두가 필요 없다). 제약은 pk_ · fk_ · uk_ · ix_ 접두.
  스키마는 앱이 부팅하며 db_schema_initializer 로 만든다 — 이 changelog 는 그 스키마 안에서만 돈다.

  모든 Entity 테이블의 컬럼 순서(설계 classdiagram §1.5):
    ① PK `<테이블명>_id`  ② tenant_id · hospital_code  ③ 업무 컬럼  ④ created_at · created_by · updated_at · updated_by
  PostgreSQL 은 이미 있는 테이블의 컬럼 순서를 바꾸지 못한다 — 이 순서를 지키려고 최초 생성 한 벌로 다시 썼다.
  이전 changelog 로 만들어진 DB 는 db/manual/001-reset-schema.sql 로 사람이 초기화한 뒤 기동한다
  (초기화하지 않으면 checksum 검증에서 부팅이 멈춘다 — 테이블은 건드리지 않는다).
  이후 변경은 이 파일을 고치지 않고 changes/00N-*.xml 을 새로 추가한다(checksum 불변).
-->
```

changeSet 1개 — `viewer_configuration` 테이블 생성(§3 쓰기 경로의 Aggregate) `R/db/changelog/changes/001-create-tables.xml:438-473`
```xml
    <changeSet id="diagnostic-result-001-create-viewer-configuration" author="diagnostic-result">
        <createTable tableName="viewer_configuration">
            <column name="viewer_configuration_id" type="VARCHAR(64)">
                <constraints nullable="false" primaryKey="true" primaryKeyName="pk_viewer_configuration"/>
            </column>
            <column name="tenant_id" type="VARCHAR(64)">
                <constraints nullable="false"/>
            </column>
            <column name="hospital_code" type="VARCHAR(64)">
                <constraints nullable="false"/>
            </column>
            <!-- 토큰 sub(직원 레코드 uuid) -->
            <column name="user_id" type="UUID">
                <constraints nullable="false"/>
            </column>
            <!-- BY_CATEGORY / BY_RESULT_DATETIME / BY_ORDER_DATETIME -->
            <column name="last_view_layout_code" type="VARCHAR(32)"/>
            <column name="default_period_days" type="INTEGER"/>
            <!-- 낙관적 잠금(@Version) — 같은 사용자의 동시 저장은 409 -->
            <column name="version" type="BIGINT" defaultValueNumeric="0">
                <constraints nullable="false"/>
            </column>
            <column name="created_at" type="TIMESTAMP WITH TIME ZONE">
                <constraints nullable="false"/>
            </column>
            <column name="created_by" type="UUID"/>
            <column name="updated_at" type="TIMESTAMP WITH TIME ZONE">
                <constraints nullable="false"/>
            </column>
            <column name="updated_by" type="UUID"/>
        </createTable>
        <addUniqueConstraint tableName="viewer_configuration"
                             columnNames="tenant_id, hospital_code, user_id"
                             constraintName="uk_viewer_configuration_tenant_id_hospital_code_user_id"/>
        <rollback/>
    </changeSet>
```
(참고: 이 changeSet 주석의 `BY_CATEGORY / …` 는 옛 코드이고, 012 changeSet 이 `CUMULATIVE · DETAIL` 등으로 옮겼다 — master `:62-65`. `default_period_days` 컬럼도 `R/db/manual/004-*.sql` 로 수동 제거 대기. **적용된 changeSet 은 고치지 않기** 때문에 낡은 주석이 남아 있다.)

### 8.5 `build.gradle.kts` 의존성 블록

`build.gradle.kts:1-15`
```kotlin
// Diagnostic Result — 단일 서비스 모듈 (배포 단위 = bootJar 1개).
// 레이어는 패키지로 표현한다(presentation / application / domain / infrastructure).
// 레이어 의존 방향은 패키지 규약 + ArchUnit(layer_isolation_test)으로 보장한다.
// 구성은 정본(x-diagnostic-imaging/backend/diagnostic-imaging-process)과 동형이다.
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
```

`build.gradle.kts:19-67`
```kotlin
dependencies {
    // domain — JPA API(jakarta.persistence) 는 data-jpa(→ hibernate-core) 가 Boot 관리버전으로 전이 제공한다.
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
    // DB 스키마 마이그레이션(부팅 시 자동 적용) — Boot 4 는 자동설정이 기술별 모듈로 분리됐다.
    //   ⚠ liquibase-core 단독으로는 LiquibaseAutoConfiguration 이 없어 부팅 마이그레이션이 안 돈다
    //     → spring-boot-liquibase 를 써야 한다(liquibase-core 전이 포함).
    implementation("org.springframework.boot:spring-boot-liquibase")
    runtimeOnly("org.postgresql:postgresql")

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-security")
    // k8s liveness/readiness probe 용 actuator health 엔드포인트 제공
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    // 사내 공통 EventBus — 외부 BC integration event 수신 (Nexus).
    //   ⚠ 0.1.10 부터 **한 토픽에 여러 messageType 을 태우고 봉투의 @MessageType 으로 분배**한다.
    //     토픽 규약이 `{환경}.{병원코드}.{리소스}` 로 바뀌면서 생성·수정·삭제가 한 토픽에 모이므로
    //     이 기능이 필수다(구 0.1.6 은 이벤트 타입마다 컨테이너를 따로 만들어 같은 토픽을 나눠 읽는다).
    //   ⚠ 0.1.18 은 봉투의 테넌트 메타가 String(`tenantId` · `hospitalCode`)이다 — 수신 테넌트 결정(event_tenant_resolver)이 읽는다
    //     (0.1.10 은 `tenantId: Long` · `hospitalId: Long` 이라 발행측이 "CHABD" 를 실으면 봉투 역직렬화가 깨진다).
    //   ⚠ 0.1.18 은 토픽 앞에 환경(profile)을 스스로 붙인다 — namespace 가 이미 `{환경}.{병원}` 이라
    //     application*.yml 의 `common.event-bus.env-prefix-enabled: false` 로 꺼서 토픽 이름을 그대로 유지한다.
    implementation("com.projectx.common:common-eventbus-kt:0.1.18")
    // 사내 공통 JWT — 인증 소비자(@RequireAuth · @TokenPrincipal AuthPrincipal). 토큰 발급은 iam 이 한다.
    //   검증 키는 X_JWT_PUBLIC_KEY · X_JWT_CLAIM_AES_SECRET_KEY(k8s Secret x-ehr-common-jwt). 토큰 방식은 헤더(sch)로 자동 판별된다.
    implementation("com.projectx.common:common-jwt-kt:0.1.24")
    // @RequireAuth 는 aspect 다 — AspectJ 위빙 런타임이 있어야 프록시가 만들어진다.
    implementation("org.springframework.boot:spring-boot-starter-aspectj")
    // 사내 공통 Observability — HTTP inbound·Kafka(common-eventbus) 자동 로깅(ECS 포맷).
    //   eventbus 와 같은 계열(com.projectx.common)로 맞춘다 — 구 계열은 봉투 로깅 훅이 안 붙는다.
    //   설정은 common-libs/common-observability.yaml. LLM 아웃바운드는 httpclient5(RestClient)를 쓰므로
    //   http-client 로깅은 미적용.
    implementation("com.projectx.common:common-observability-kt:0.1.18")
    // 민감 필드 마스킹 — body 로깅 시 PII(patient_id/patient_name 등) 자동 마스킹.
    implementation("com.projectx.common:common-masking-kt:0.1.6")
    implementation("org.springframework.kafka:spring-kafka")
```

`build.gradle.kts:68-105`
```kotlin
    // dev(GCP Managed Kafka) 는 SASL_SSL + OAUTHBEARER 다 — 이 핸들러가 없으면 SASL 설정이 주입된 순간
    //   sasl.login.callback.handler.class 를 못 찾아 producer/consumer 생성이 깨진다(기동 후 첫 발행·구독에서).
    //   접속값(주소 + SASL 4종)은 배포레포의 공용 configMap `x-ehr-foundation-be-kafka` 가 준다.
    //   전이로 딸려오는 `io.confluent:*` 는 mavenCentral 에 없어 해석이 실패하므로 제외한다.
    //   로컬은 사내 브로커(PLAINTEXT)라 이 핸들러가 관여하지 않는다.
    implementation("com.google.cloud.hosted.kafka:managed-kafka-auth-login-handler:1.0.6") {
        exclude(group = "io.confluent")
    }
    // common-eventbus 의 Rabbit 빈이 ConnectionFactory 를 요구(provider=kafka 라도 빈 생성). lazy 라 미사용 시 연결 없음.
    implementation("org.springframework.boot:spring-boot-starter-amqp")

    // ⚠ Boot 4(Spring 7)의 런타임 Jackson 은 **Jackson 3(`tools.jackson`)** 이다.
    //   관성으로 `com.fasterxml.jackson.module:jackson-module-kotlin`(Jackson 2)을 넣으면 조용히 무시돼
    //   Kotlin 기본값·nullability 처리가 빠진다(생략된 필드가 500 으로 터진다).
    //   JSR-310 은 Jackson 3 내장이라 jackson-datatype-jsr310 이 필요 없다.
    implementation("tools.jackson.module:jackson-module-kotlin")

    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:$springdocVersion")
    implementation("io.github.oshai:kotlin-logging-jvm:7.0.13")

    // 로컬 접속값을 코드 밖에서 준다 — `backend/.env` 를 Spring Environment 로 로드(spring-dotenv).
    //   Boot 4 호환 등록은 META-INF/spring.factories 의 dotenv_environment_post_processor.
    //   .env 부재 시 무시되므로 k8s 배포(실제 환경변수)에는 영향이 없다.
    implementation("me.paulschwarz:spring-dotenv:4.0.0")

    // LLM Gateway 호출용 HTTP 클라이언트 — k8s 차트(khc-deployment)가 hostAliases 를 지원하지 않아
    //   DnsResolver 로 특정 호스트만 내부 IP 로 해석하기 위해 사용 (버전은 Boot BOM 관리).
    implementation("org.apache.httpcomponents.client5:httpclient5")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    // 레이어 격리 가드 — 단일 모듈이라 컴파일 강제가 없으므로 ArchUnit 으로 domain 격리를 강제한다.
    testImplementation("com.tngtech.archunit:archunit-junit5:1.4.1")
}
```

### 8.6 AI Gateway — Port(application) & Adapter(infrastructure)

Port(계약) `K/application/ports/i_analysis_agent_port.kt:3-47`
```kotlin
// AI 에이전트 port — application 이 소유하는 계약, 구현은 infrastructure/proxies 의 ai_gateway_analysis_agent_provider.
// 핸들러 · 컨트롤러는 이 인터페이스만 본다(infrastructure 를 import 하지 않는다 — ArchUnit 이 강제).
//
// 경로는 하나다 — AI Gateway `POST /v2/chat/complete`(deep agent). 모델 · 에이전트 · 경로를 이 BC 가 고르지 않는다:
// 메시지 하나를 보내면 게이트웨이의 deep agent 가 처리할 에이전트를 정한다.
interface i_analysis_agent_port {
    // AI 에이전트. 설정(API 키)이 없으면 analysis_agent_unavailable_exception(503).
    fun agent(): analysis_agent
}

// 환자 검사결과를 분석하고 자유 질의에 답하는 AI 에이전트. 호출 경로에 종속되지 않도록 이 인터페이스로만 부른다.
interface analysis_agent {
    // agent_name 컬럼에 저장될 경로 식별자 (예: "ai-gateway")
    val name: String

    // 응답에 사용된 에이전트 목록이 비었을 때 agent_version 으로 남길 값 — 게이트웨이 경로(예: "v2/chat/complete")
    val version: String

    // 분석 1회 호출 — 구조화 출력(output_schema)으로 받는다. 입력은 분석 요청 문장 하나다 —
    // 검사결과 요약본을 담은 문장은 application/analysis 의 analysis_delivery_service 가 만든다(이 계약은 내용을 모른다).
    // 모델 · 프롬프트는 게이트웨이에 있다. 응답은 어댑터가 정규화해서 준다(코드펜스 제거 · summary 추출).
    // 게이트웨이가 완료(done)로 답하지 못하면 analysis_agent_failure_exception(502).
    fun analyze(request_message: String): analysis_agent_response

    // 검사결과 요약 1회 — 저장하지 않는다. 분석과 출력 모양이 다르다(맥락 문단 + 고른 결과 코드 · 이유).
    // 요청 문장(전체 요약 프롬프트 + 요약본)은 application/analysis 의 result_summary_service 가 만든다.
    // 게이트웨이가 완료(done)로 답하지 못하면 analysis_agent_failure_exception(502).
    fun summarize(request_message: String): result_summary_agent_response

    // 자유 질의 1회 — 저장하지 않는다. 구조화 없이 메시지를 그대로 보내고 output 을 문자열로 돌려준다.
    // thread_id 를 주면 그 대화에 이어 묻는다(응답의 thread_id 를 다음 호출에 넘긴다). 실패는 status · error 로 돌려준다.
    fun chat(message: String, thread_id: String?): analysis_chat_reply
}

// 정규화된 분석 응답
data class analysis_agent_response(
    // 요약 — detail 의 "summary" 필드(없거나 잘렸으면 최선 추출)
    val summary: String,
    // 본문 — 구조화 output(JSON) 그대로, 문자열로 왔으면 코드펜스를 걷어낸 본문. analyze_report_detail 로 저장된다
    val detail: String,
    // 호출 메타 JSON (run_id · session_id · usage · elapsed_sec · latency_ms …) — agent_details 컬럼으로 저장된다(이름을 컬럼과 같게 둔다)
    val agent_details: String,
    // 게이트웨이가 실제로 쓴 에이전트 이름들 — 이 BC 가 고른 값이 아니다. 없으면 빈 목록
    val agents: List<String> = emptyList(),
)
```

Adapter ① Provider(`i_analysis_agent_port` 구현 · 키 없으면 null 보관 = 부팅 안 막음) `K/infrastructure/proxies/ai_gateway_analysis_agent_provider.kt:21-63`
```kotlin
// i_analysis_agent_port 구현 — AI Gateway 에이전트 **하나**를 설정(base-url · api-key)으로 만들어 돌려준다.
// 에이전트 · 모델 · 경로를 이 BC 가 고르지 않는다 — 선택 · 폴백 없이 언제나 POST /v2/chat/complete 로 부른다.
// api-key 가 비어 있으면 만들지 않고 부팅은 막지 않는다 — 분석 · 질의만 analysis_agent_unavailable_exception(503).
// ⚠️ 여기서 예외를 던지면 안 된다 — 키가 없는 환경에서 부팅이 실패하면 검사결과 조회 API 전체가 뜨지 못해
//    서비스가 라우팅에서 사라진다(Envoy 가 upstream connect error 111 을 반환). AI 분석은 부가기능이라 degrade 로 처리한다.
@Component
class ai_gateway_analysis_agent_provider(
    props: ai_gateway_properties,
    object_mapper: ObjectMapper,
    normalizer: analysis_response_normalizer,
    summary_normalizer: result_summary_response_normalizer,
) : i_analysis_agent_port {
    private val logger = KotlinLogging.logger {}

    private val agent: analysis_agent? =
        if (props.api_key.isBlank()) {
            logger.warn { "AI Gateway api-key 가 비어 있어 AI 에이전트를 만들지 않습니다 — AI 분석 · 질의는 503 으로 응답합니다" }
            null
        } else {
            ai_gateway_analysis_agent(
                rest_client = build_rest_client(props),
                max_retries = props.max_retries,
                object_mapper = object_mapper,
                normalizer = normalizer,
                summary_normalizer = summary_normalizer,
            ).also { logger.info { "AI 에이전트 준비 완료: base_url=${props.base_url}, route=${it.version}" } }
        }

    override fun agent(): analysis_agent =
        agent ?: throw analysis_agent_unavailable_exception("사용할 수 있는 AI 에이전트가 없습니다")

    // 타임아웃이 적용된 AI Gateway 전용 RestClient — X-API-Key 헤더 인증.
    private fun build_rest_client(props: ai_gateway_properties): RestClient {
        return RestClient.builder()
            .requestFactory(
                build_request_factory(
                    "AI Gateway", props.connect_timeout_ms, props.read_timeout_ms, props.resolve_host, props.resolve_ip
                )
            )
            .baseUrl(props.base_url)
            .defaultHeader("X-API-Key", props.api_key)
            .build()
    }
```

Adapter ② 실제 호출(`analysis_agent` 구현 · `@Component` 아님) `K/infrastructure/proxies/ai_gateway_analysis_agent.kt:14-74`
```kotlin
// Kakao AI Gateway `POST /v2/chat/complete`(deep agent) 기반 analysis_agent 구현 — 이 BC 의 유일한 AI 경로.
// - 요청 body = { message, approval: "auto", output_schema, thread_id }. 에이전트 · 모델 · 경로를 싣지 않는다 —
//   어느 에이전트가 처리할지는 게이트웨이의 deep agent 가 메시지를 보고 정한다(approval=auto 라 승인 대기 없이 끝까지 돈다).
// - 응답 봉투 = { run_id, session_id, status(done·approval_required·rejected·error), output, plan, approval, agents,
//   agent_draft_pending, usage{input,output}, elapsed_sec, error{code,message} }.
//   · output_schema 를 주면 output 이 **객체**(요청한 모양), 주지 않으면 문자열이다. 모델이 스키마대로 내지 못하면 객체 대신
//     문자열이 올 수 있어 타입을 보고 처리한다.
// - analyze: 분석 스키마(analysis_response_normalizer.output_schema)로 구조화 출력을 받아 정규화해 돌려준다 →
//   핸들러가 summary · detail 을 그대로 저장한다. 실제로 쓰인 에이전트(agents)는 이력의 agent_version 이 된다.
// - summarize: 요약 스키마(result_summary_response_normalizer.output_schema)로 받아 해석해 돌려준다(검사결과 요약 · 저장 없음).
// - chat: 스키마 없이 메시지를 그대로 보내고 output 을 문자열로 돌려준다(테스트조회 자유 질의 · 저장 없음).
// - @Component 가 아님 — ai_gateway_analysis_agent_provider 가 설정으로 1개 만든다.
class ai_gateway_analysis_agent(
    private val rest_client: RestClient,
    private val max_retries: Int,
    private val object_mapper: ObjectMapper,
    private val normalizer: analysis_response_normalizer,
    private val summary_normalizer: result_summary_response_normalizer,
) : analysis_agent {

    private val logger = KotlinLogging.logger {}

    // agent_name 컬럼 식별자 — 경로가 하나라 고정이다
    override val name: String = AGENT_NAME

    // 응답에 에이전트 목록이 없을 때 agent_version 으로 남길 값 — 경로
    override val version: String = ROUTE

    override fun analyze(request_message: String): analysis_agent_response {
        val started_at = System.currentTimeMillis()
        val envelope = complete(request_message, normalizer.output_schema(), null)
        val raw_text = completed_output_text(envelope, "분석")
        val latency_ms = System.currentTimeMillis() - started_at

        val agent_details = object_mapper.writeValueAsString(
            mapOf(
                "provider" to PROVIDER,
                "route" to ROUTE,
                "run_id" to envelope.run_id,
                "session_id" to envelope.session_id,
                "agents" to envelope.agents,
                "usage" to envelope.usage,
                "elapsed_sec" to envelope.elapsed_sec,
                "structured" to envelope.output_is_object,
                "promptCharCount" to request_message.length,
                "responseCharCount" to raw_text.length,
                "latency_ms" to latency_ms
            )
        )
        logger.info {
            "ai_gateway_analysis_agent 분석 완료: run_id=${envelope.run_id}, agents=${envelope.agents}, " +
                "structured=${envelope.output_is_object}, elapsed_sec=${envelope.elapsed_sec}, latency_ms=$latency_ms"
        }
        val normalized = normalizer.normalize(raw_text)
        return analysis_agent_response(
            summary = normalized.summary,
            detail = normalized.detail,
            agent_details = agent_details,
            agents = envelope.agents
        )
    }
```

HTTP 호출 + 재시도 — `K/infrastructure/proxies/ai_gateway_analysis_agent.kt:120-147`
```kotlin
    private fun complete(message: String, output_schema: Map<String, Any>?, thread_id: String?): gateway_envelope {
        val request_body = mapOf(
            "message" to message,
            "approval" to APPROVAL_AUTO,
            "output_schema" to output_schema,
            "thread_id" to thread_id
        )

        var last_error: Exception? = null
        repeat(max_retries + 1) { attempt ->
            try {
                val response_body = rest_client.post()
                    .uri(COMPLETE_PATH)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(request_body)
                    .retrieve()
                    .body(String::class.java)
                    ?: throw IllegalStateException("AI Gateway 응답이 비어있습니다")
                return parse_envelope(response_body)
            } catch (e: Exception) {
                last_error = e
                logger.warn { "ai_gateway_analysis_agent 호출 실패(시도 ${attempt + 1}/${max_retries + 1}): cause=${e.message}" }
            }
        }
        // 응답 메시지에는 원인 체인을 싣지 않는다(게이트웨이 내부 문구가 그대로 화면에 가지 않게) — 원인은 로그와 cause 에 남는다
        throw analysis_agent_failure_exception("AI Gateway 호출에 실패했습니다", last_error)
    }
```

---

## 9. 테스트

테스트 트리(`T/`)에는 **`@SpringBootTest` · `@DataJpaTest` 가 하나도 없다**(`grep -rn '@SpringBootTest\|@DataJpaTest' src/test` → 0건). 전부 순수 JUnit5 + AssertJ 이고, Spring 컨텍스트를 올리는 유일한 테스트는 `ApplicationContextRunner` 를 쓰는 `communication_wiring_test` 다. DB 가 필요한 검증(SKIP LOCKED · REQUIRES_NEW)은 "앱 기동 · 수신으로 확인한다" 고 테스트 주석이 명시한다.

```
T/
├─ architecture/layer_isolation_test.kt         ← ArchUnit
├─ domain/{favorite_set_rules,analysis_review_target_rules}_test.kt   ← Aggregate 단위
├─ inbox/*_test.kt (+ inbox_test_support.kt)      ← 수신 흐름을 Spring 없이 fixture 로 조립
├─ integrationevents/*_test.kt                   ← 매퍼 · 라우팅 · 테넌트 결정
├─ persistence/*_query_test.kt                   ← QueryDSL 쿼리의 JPQL 모양 검사
├─ proxies/*_test.kt                             ← AI/서식 어댑터(가짜 RestClient)
├─ communication/communication_wiring_test.kt    ← ApplicationContextRunner 배선
└─ analysis/*_test.kt · config/hospital_time_test.kt · test_ids.kt
```

### 9.1 ArchUnit `layer_isolation_test` 전체

`T/architecture/layer_isolation_test.kt:1-48`
```kotlin
package com.projectx.diagnosticresult.architecture

import com.tngtech.archunit.junit.AnalyzeClasses
import com.tngtech.archunit.junit.ArchTest
import com.tngtech.archunit.lang.ArchRule
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses

/**
 * 레이어 격리 가드 — 단일 서비스 모듈(sre-backend 구조)에서 컴파일 타임 모듈 경계가 사라진 대신,
 * domain 이 Spring/바깥 레이어에, application · presentation 이 infrastructure 에 의존하지 않음을 ArchUnit 으로 강제한다.
 *
 * 구 3모듈(-domain/-infrastructure/-api) 시절에는 gradle 모듈 경계가 이를 강제했다 — 1모듈 통합으로
 * 그 강제가 사라졌으므로 이 테스트가 그 역할을 대신한다. diagnostic-imaging-process 와 동형.
 */
@AnalyzeClasses(packages = ["com.projectx.diagnosticresult"])
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

    // application 은 계약(port · i_*_queries · i_*_repository)만 갖고 구현을 모른다 —
    // 외부 호출 어댑터는 infrastructure/proxies, 조회 구현은 infrastructure/queries 가 그 계약을 구현한다.
    @ArchTest
    val application_must_not_depend_on_infrastructure: ArchRule =
        noClasses()
            .that().resideInAPackage("..application..")
            .should().dependOnClassesThat().resideInAPackage("..infrastructure..")

    // controller 는 command_bus · i_*_queries · port 만 본다
    @ArchTest
    val presentation_must_not_depend_on_infrastructure: ArchRule =
        noClasses()
            .that().resideInAPackage("..presentation..")
            .should().dependOnClassesThat().resideInAPackage("..infrastructure..")
}
```

### 9.2 단위 테스트 — Aggregate 규칙(`favorite_set_rules_test`)

`T/domain/favorite_set_rules_test.kt:15-70`
```kotlin
/**
 * 즐겨찾기 세트 규칙 — 세트 상한 · 이름 형식/중복 · 검사 배정(고른 세트에만) · 순서.
 * 별표 선택창은 요청 한 번에 여러 세트를 함께 바꾼다 — 고르지 않은 세트에서 빠지는지, 새 세트가 다른 세트를
 * 건드리지 않는지가 이 기능의 핵심이다.
 */
class favorite_set_rules_test {

    private val glu = favorite_observation("SPECIMEN_DIAGNOSTICS", "GLU")

    @Test
    fun `검사 배정은 고른 세트에만 넣고 나머지 세트에서는 뺀다`() {
        val config = viewer_configuration(uuid(TEST_EMPLOYEE_ID), tenant_scope.of("CHABD", "CHABD"))
        val a = config.add_favorite_set("기본", first_item = glu)
        val b = config.add_favorite_set("신장")
        val c = config.add_favorite_set("외래")

        config.assign_observation_to_sets("SPECIMEN_DIAGNOSTICS", "GLU", setOf(b.id, c.id))

        assertThat(a.items).doesNotContain(glu)
        assertThat(b.items).containsExactly(glu)
        assertThat(c.items).containsExactly(glu)

        // 빈 집합 = 즐겨찾기 해제
        config.assign_observation_to_sets("SPECIMEN_DIAGNOSTICS", "GLU", emptySet())
        assertThat(config.favorite_sets.flatMap { it.items }).isEmpty()
    }

    @Test
    fun `새 세트에 검사를 넣어도 다른 세트의 소속은 그대로다`() {
        val config = viewer_configuration(uuid(TEST_EMPLOYEE_ID), tenant_scope.of("CHABD", "CHABD"))
        val a = config.add_favorite_set("기본", first_item = glu)
        val b = config.add_favorite_set("신장", first_item = glu)

        assertThat(a.items).containsExactly(glu)
        assertThat(b.items).containsExactly(glu)
    }

    @Test
    fun `모르는 세트 id 로 배정하면 아무것도 바꾸지 않고 404`() {
        val config = viewer_configuration(uuid(TEST_EMPLOYEE_ID), tenant_scope.of("CHABD", "CHABD"))
        val a = config.add_favorite_set("기본", first_item = glu)

        assertThatThrownBy { config.assign_observation_to_sets("SPECIMEN_DIAGNOSTICS", "GLU", setOf("없는-세트")) }
            .isInstanceOf(diagnostic_result_not_found_exception::class.java)
        assertThat(a.items).containsExactly(glu)
    }

    @Test
    fun `세트는 20개까지 만들 수 있다`() {
        val config = viewer_configuration(uuid(TEST_EMPLOYEE_ID), tenant_scope.of("CHABD", "CHABD"))
        repeat(viewer_configuration.MAX_FAVORITE_SETS) { config.add_favorite_set("세트$it") }

        assertThatThrownBy { config.add_favorite_set("하나 더") }
            .isInstanceOf(diagnostic_result_domain_exception::class.java)
            .hasMessageContaining("20개")
    }
```

테스트가 검증하는 도메인 메서드 — `K/domain/aggregatesmodel/viewerconfiguration/viewer_configuration.kt:96-108`
```kotlin
    // 별표 선택창 — 검사가 들어갈 세트를 정확히 favorite_set_ids 로 맞춘다 (멱등)
    // 고른 세트에는 넣고 나머지 세트에서는 뺀다. 빈 집합이면 모든 세트에서 빠진다
    fun assign_observation_to_sets(
        observation_type_code: String,
        observation_code: String,
        favorite_set_ids: Set<String>
    ) {
        val observation = favorite_observation(observation_type_code, observation_code)
        favorite_set_ids.forEach { find_favorite_set(it) } // 모르는 세트 id 가 있으면 아무것도 바꾸기 전에 404
        _favorite_sets.forEach { set ->
            if (set.id in favorite_set_ids) set.add_item(observation) else set.remove_item(observation)
        }
    }
```

### 9.3 통합(흐름) 테스트 — 수신 inbox 흐름(`inbox_flow_test`, Spring·DB 없이 fixture 조립)

`T/inbox/inbox_flow_test.kt:14-68`
```kotlin
/**
 * 수신 inbox 흐름(classdiagram §1.8) — 수신 → 적재 → drain(T1 집기 · T2 처리 · T3 실패 기록) → 실제 결과 핸들러.
 *
 * Spring · DB 없이 조립한다(`inbox_fixture`). 그래서 여기서는 **순서 · hold · 재시도 · 체인 규칙**만 본다 —
 * 트랜잭션 경계(REQUIRES_NEW 가 T2 롤백과 따로 커밋되는지)와 SKIP LOCKED 동시성은 DB 가 있어야 드러나 앱 기동 · 수신으로 확인한다.
 * 집기 SQL 의 모양(SKIP LOCKED · hold 조건)은 `inbox_query_test` 가 본다.
 */
class inbox_flow_test {

    private val key = inbox_key(LAB_TOPIC, "LAB-1")

    @Test
    fun `적재 커밋 신호를 내고 drain 하면 같은 키를 도착 순서대로 끝까지 처리한다`() {
        val f = inbox_fixture()
        (1L..3L).forEach { f.receive_lab("LAB-1", version = it) }

        // 적재마다 즉시 처리 신호(커밋 뒤 drain) — 키는 (토픽, 결과 식별자)
        assertThat(f.stored_events.events).containsOnly(inbox_message_stored(key))

        f.processor.drain(key)

        assertThat(f.inbox.of(key)).allMatch { it.status == inbox_status.PROCESSED && it.processed_at != null && it.locked_by == null }
        assertThat(f.observations.saved_versions).containsExactly(1L, 2L, 3L)
    }

    @Test
    fun `같은 키 3건 중 2번째가 일시 실패하면 3번째는 멈추고, 재시도 시각이 지나면 체인으로 끝까지 흐른다`() {
        val f = inbox_fixture()
        f.observations.failing_find = { it == 2 } // 2번째 처리(버전 2)의 첫 시도만 일시 장애
        (1L..3L).forEach { f.receive_lab("LAB-1", version = it) }

        f.processor.drain(key)

        val (first, second, third) = f.inbox.of(key)
        assertThat(first.status).isEqualTo(inbox_status.PROCESSED)
        assertThat(second.status).isEqualTo(inbox_status.PENDING)
        assertThat(second.retry_count).isEqualTo(1)
        assertThat(second.last_error).contains("IllegalStateException").contains("DB 일시 장애")
        assertThat(second.next_retry_at).isAfter(second.received_at.plusSeconds(59)) // 첫 백오프 60초
        assertThat(third.status).isEqualTo(inbox_status.PENDING)
        assertThat(third.retry_count).isZero()                                       // 집히지도 않았다(hold)

        // 재시도 시각 전에는 다시 drain 해도 아무것도 집히지 않는다 — 2번째는 대기, 3번째는 hold
        f.processor.drain(key)
        assertThat(f.observations.saved_versions).containsExactly(1L)
        assertThat(f.tx.find_ready_keys(10)).isEmpty()

        // 재시도 시각이 지나면 sweeper 가 키를 찾고, 2번째가 성공하면 3번째까지 이어서 처리한다
        f.inbox.time_shift = Duration.ofMinutes(2)
        assertThat(f.tx.find_ready_keys(10)).containsExactly(key)
        f.processor.drain(key)

        assertThat(f.inbox.of(key)).allMatch { it.status == inbox_status.PROCESSED }
        assertThat(f.observations.saved_versions).containsExactly(1L, 2L, 3L)
    }
```

fixture 가 실제 봉투 JSON 을 만드는 방식(발행측 흉내) — `T/inbox/inbox_test_support.kt:39-60`
```kotlin
/** 운영 ObjectMapper(Boot JsonMapper + Kotlin 모듈)와 같은 규칙의 매퍼 — 봉투 직렬화 · 복원에 쓴다. */
internal fun test_json_mapper(): ObjectMapper = jacksonObjectMapper()

internal const val LAB_TOPIC = laboratory_result_integration_event.TOPIC

/** 진검 결과 봉투 원문 — 실제 발행측처럼 메타 + payload 를 한 JSON 으로 만든다. */
internal fun lab_envelope_json(
    mapper: ObjectMapper,
    source_id: String,
    version: Long,
    patient_id: String = TEST_PATIENT_ID,
    message_id: UUID = UUID.randomUUID(),
    user_id: String = "anonymous",
): String {
    val at = OffsetDateTime.of(2026, 10, 22, 9, 0, 0, 0, ZoneOffset.ofHours(9))
    val payload = laboratory_result_integration_event().apply {
        laboratoryResultId = source_id
        sourceAggregateId = source_id
        sourceEventVersion = version
        observationTypeCode = specimen_diagnostics_observation.OBSERVATION_TYPE_CODE
        observationTypeName = "검체진단"
        patientId = patient_id
```

### 9.4 Spring 컨텍스트를 올리는 유일한 테스트 — `ApplicationContextRunner` 배선 검사

`T/communication/communication_wiring_test.kt:17-62`
```kotlin
/**
 * 알림 빈 배선(classdiagram §1.10) — Spring 이 설정 생성자(`@Autowired`)를 고르고, 콤마 목록이 받는 사람으로 묶이고,
 * 주소가 비어도 앱이 뜨는지(부가기능 — 부팅을 막지 않는다).
 */
class communication_wiring_test {

    @Configuration
    @EnableConfigurationProperties
    @Import(
        communication_properties::class,
        communication_inbox_provider::class,
        communication_token_source::class,
        inbox_alert_properties::class,
        inbox_failure_alert::class,
    )
    class wiring

    private val runner = ApplicationContextRunner()
        .withUserConfiguration(wiring::class.java)
        .withBean(ObjectMapper::class.java, { JsonMapper.builder().build() })
        .withPropertyValues("spring.application.name=diagnostic-result")

    @Test
    fun `주소가 있으면 설정으로 클라이언트를 만들고 받는 사람 콤마 목록 · 메시지 칸 키를 묶는다`() {
        runner.withPropertyValues(
            "diagnosticresult.communication.base-url=https://dev-ehr-chabd.kakaohealthcare.com/",
            "diagnosticresult.inbox.alert.code=INBOX_MESSAGE_FAILED",
            "diagnosticresult.inbox.alert.recipients=DEPARTMENT:EHR-OPS,EMPLOYEE:u-1",
            "diagnosticresult.inbox.alert.message-key=MESSAGE",
        ).run { context ->
            assertThat(context).hasNotFailed()
            assertThat(context.getBean(communication_inbox_provider::class.java).client()).isNotNull()
            assertThat(context.getBean(inbox_alert_properties::class.java).recipients).containsExactly("DEPARTMENT:EHR-OPS", "EMPLOYEE:u-1")
            assertThat(context.getBean(inbox_alert_properties::class.java).message_key).isEqualTo("MESSAGE")
        }
    }

    @Test
    fun `아무 설정이 없어도 뜨고 클라이언트는 없다`() {
        runner.run { context ->
            assertThat(context).hasNotFailed()
            assertThat(context.getBean(communication_inbox_provider::class.java).client()).isNull()
            assertThat(context.getBean(inbox_alert_properties::class.java).recipients).isEmpty()
        }
    }
}
```

---

## 10. 이 레포만의 특징 패턴

### 10.1 AI Gateway Port & Adapter (degrade-first)
- application 이 `i_analysis_agent_port` · `analysis_agent` 계약을 소유하고(`K/application/ports/i_analysis_agent_port.kt:8-35`), infrastructure/proxies 의 `ai_gateway_analysis_agent_provider` 가 구현한다. ArchUnit 이 역방향 import 를 막는다.
- 경로는 **하나**(`POST /v2/chat/complete`, deep agent). 에이전트·모델 선택이 없고 게이트웨이가 정한다(`ai_gateway_analysis_agent.kt:14-25`).
- API 키가 비면 에이전트를 `null` 로 두고 **부팅을 막지 않는다** — 호출 시점에만 503 `AgentUnavailable`(`provider.kt:35-50`). 호출 실패는 502 `AgentFailure` 로 원인 체인 없이 응답(`:146`).
- 분석(`analyze`) 은 `output_schema` 로 구조화 출력 → `analysis_response_normalizer` 가 summary/detail 추출 → `observation_analyze_result` 로 저장. 요약(`summarize`) · 자유 질의(`chat`) 는 저장하지 않는다.
- AI 호출 동안 DB 커넥션을 잡지 않도록 `analysis_delivery_service` 는 `@Transactional` 을 걸지 않는다(`K/application/analysis/analysis_delivery_service.kt:32-33`). dev 는 `resolve-host/ip` 로 앱 레벨 DNS 오버라이드(hairpin 우회)를 둘 수 있다.

### 10.2 보고서 뷰 / 서식(record document) 중계
- 결과 HTML 은 **저장하지 않는다** — `observation.record_id` 만 적재하고(payload `recordId`, `observation_result_integration_event.kt:142-147`), 화면이 열 때 `GET /observations/{id}/record-document` 로 원천 모듈에서 가져와 그대로 돌려준다(`K/presentation/observations_controller.kt:157-162`).
- `i_record_document_port.fetch_html(source_module_code, record_id)` 가 계약, `record_document_http_adapter` 가 URL 템플릿(`{record_id}`)으로 GET(`K/infrastructure/proxies/record_document_http_adapter.kt:35-58`).
- 원천 모듈 주소는 `diagnosticresult.record-document.sources.<모듈>` 이고 기본값이 **빈 값**(상대 BC 계약 대기) → 503 `RecordDocumentUnavailable`. 호출 실패·빈 본문은 502.
- 원천 응답 본문은 환자 정보일 수 있어 **로그에도 남기지 않는다**(`:46-48`). 화면은 sandbox iframe 에 격리해 그린다.

### 10.3 즐겨찾기 세트 (viewer_configuration Aggregate)
- 사용자(직원)당 1행 `viewer_configuration`(UK `tenant_id, hospital_code, user_id`) 안에 `favorite_set` 자식 Entity 목록(최대 20 · `MAX_FAVORITE_SETS`) · 그 안에 `favorite_observation` VO 항목.
- 별표 선택창은 **세트 집합을 통째로 맞추는 멱등 연산** `assign_observation_to_sets` — 고른 세트엔 넣고 나머지에선 뺀다(`viewer_configuration.kt:96-108`). 모르는 세트 id 가 하나라도 있으면 아무것도 바꾸기 전에 404.
- 엔드포인트는 `viewer-configurations/me/favorite-sets` (POST · PATCH · DELETE) · `/favorite-sets/order`(PUT) · `/favorite-assignments`(PUT) (`K/presentation/favorite_sets_controller.kt:40-87`). `@Version` 낙관락이라 동시 저장은 409.
- 세트 순서는 "모든 세트를 정확히 한 번씩" 담아야 하고(`reorder_favorite_sets`, `:111-121`), 번호는 `renumber_favorite_sets` 가 다시 매긴다.

### 10.4 누적조회 · 보기 방식 코드
- 환자 결과 목록 `GET /observations?patient_id=…` 가 분류(`observation_type_code`)·분류코드(`category_code`)·기간·날짜기준으로 거르고, 화면 보기 방식은 `viewer_configuration.last_view_layout_code` 에 사용자별로 기억한다(`set_last_view_layout`, `viewer_configuration.kt:124`).
- 보기 방식 코드는 012 changeSet 이 옛 `BY_CATEGORY / BY_RESULT_DATETIME / BY_ORDER_DATETIME` 을 `CUMULATIVE · DETAIL …` 4종으로 옮겼다(master `:62-65`). 001 의 컬럼 주석은 옛 값 그대로 남아 있다(적용된 changeSet 불변).
- 검사일 타임라인 `GET /observations/exam-dates` 는 환자 **전 기간** 결과 1건마다 날짜기준의 날짜·분류·비정상·위험치를 읽고 호출측이 (날짜, 분류, 구분)으로 집계한다(`observation_query_dsl.kt:205-228`).
- 수진 단위 조회 2종(`/encounters/{id}` · `/origin-encounters/{id}`)이 따로 있다 — `order_id` · `origin_encounter_id` 는 008 · 009 changeSet 으로 뒤늦게 추가된 컬럼이다.

### 10.5 조회 기간 · 날짜기준(`date_basis`)
- `observation_date_basis` 4종(ORDER · ENCOUNTER · PERFORMED · RESULT, 기본 RESULT)이 "어느 칸의 날짜로 자를지"를 정한다 — `basis_columns` 가 날짜 칸(`order_date` · `encounter_date`)과 시각 칸(`ordered_at` · `performed_at` · `result_datetime`)을 짝지운다(`observation_query_dsl.kt:176-184`).
- `period_predicate` 는 날짜 칸이 있으면 `[from, to]` 포함, 없으면 시각 칸을 **병원 시간대 [from 00:00, to+1 00:00)** 반열린 구간으로 본다(`:192-203`). 날짜 칸이 null 인 행은 시각 칸으로 폴백(`date.isNull.and(timestamp_in)`).
- 한쪽만 줘도 된다(`from` 만 · `to` 만), 둘 다 없으면 조건 없음.
- 시각 → 병원 날짜 변환은 DB 함수 문자열 없이 쓸 수 없어 **Kotlin 에서** 한다(`:216-220`) — QueryDSL 타입세이프 원칙과 "네이티브 함수 문자열 금지"의 타협점.

### 10.6 수신 inbox (DLT 대신 DB 행)
- Kafka 수신은 `subscribeRaw` 로 **원문을 inbox_message 에 적재만** 하고 ack 한다 — 본문이 깨져도 적재되고 처리 단계에서 FAILED 로 남는다(`inbox_receiver.kt:15-23`).
- 처리는 `(topic, partition_key)` 키 단위로 **직렬**(hold 조건 · SKIP LOCKED), 다른 키는 병렬. T1 집기(임대) → T2 처리+PROCESSED 한 트랜잭션 → 실패 시 T3 `REQUIRES_NEW` 기록(`inbox_tx_service.kt`).
- 즉시 처리(`AFTER_COMMIT` → 전용 스레드풀)와 30초 sweeper(임대 회수 + ready 키 drain)가 같은 `inbox_processor.drain` 을 부른다.
- 영구 실패(Jackson · IllegalArgument · 도메인 예외)는 재시도 없이 바로 FAILED, 그 외는 60/300/1800/3600 초 백오프 최대 10회(`inbox_retry_policy.kt`). FAILED 확정은 알림 센터(communication Inbox)로 카드를 보낸다.
- 운영 API `inbox-messages/*`(retry · payload 수정 · resend · terminate · alert) 가 있고, 오류 본문은 다른 BC(x-patient-appointment)와 **같은 파일** `inbox_error_advice` 로 통일한다.

### 10.7 그 밖에 눈에 띄는 것
- **테넌트 범위 VO** `tenant_scope(tenant_id, hospital_code)` 를 모든 Entity 가 생성 때 받고 모든 조회가 이 범위로 거른다. HTTP 는 토큰, 수신은 `event_tenant_resolver`(봉투 vs 배포값, 불일치는 경고 후 배포값).
- **감사 컬럼은 orm.xml 등록 리스너**가 MDC `userId` 에서 채운다 — HTTP 는 JWT 필터가, 수신은 `inbox_dispatcher` 가 봉투 userId 를 MDC 에 넣는다.
- **병원 시간대 1곳**(`HOSPITAL_TIME_ZONE` 환경변수) — JVM 기본 시간대 · `hospital_clock` 빈 · 오프셋 없는 수신 시각 해석 모두 여기서.
- **AI 재점검 대상** `analysis_review_target`(환자당 1행) — 결과 커밋 뒤 켜고, 분석 시작이 끈다(`analysis_review_targets.kt:28-40`).

---

## 11. 신입이 헷갈릴 포인트 10개

1. **`@MessageType` 은 분배에 쓰이지 않는다** — 수신 분배 키는 토픽이고(`inbox_dispatcher.kt:15`, `event_types[message.topic]`), `@MessageType` 은 라이브러리 route 등록용 계약 표기일 뿐이다. 그래도 빼면 `MapEventRouter` 등록이 깨진다(`laboratory_result_integration_event.kt:15-16`).
2. **생성·수정·삭제는 토픽도 클래스도 하나** — 행위는 payload `eventType` 이 정하고, 값이 없거나 모르면 **적재(upsert)** 로 본다. 삭제는 `deleted` 명시일 때만(`observation_result_handler.kt:84-97`). "왜 삭제 이벤트 클래스가 없지?" 가 첫 질문이 된다.
3. **`handle()` 에만 `@Transactional`, `handle_internal` 엔 붙이지 말 것** — 같은 빈 안 self-invocation 은 프록시를 안 타 트랜잭션이 안 걸린다(`integration_event_handler.kt:22-26`). `inbox_tx_service` 도 같은 이유로 "반드시 다른 빈이 부른다"(`inbox_tx_service.kt:16-18`).
4. **`AFTER_COMMIT` 리스너 안에서 DB 를 쓰면 커밋되지 않는다** — 그래서 `inbox_drain_trigger` 는 전용 실행기 스레드로 넘긴다(`inbox_drain_executor.kt:14-16`). `@Async` 를 안 쓴 이유도 거기 적혀 있다(기본 `applicationTaskExecutor` 가 물러날 수 있음).
5. **payload 필드만 camelCase** — 전 영역 snake_case 인데 `observation_result_integration_event` 의 `var` 들은 camelCase 다(cross-BC 계약, `integration_event.kt:9`). 매퍼가 `vitalResultValue → record_result_value` 처럼 바꿔 담는다. 이름을 "정리" 하면 수신분이 통째로 FAILED(`observation_result_integration_event.kt:19-20`).
6. **Jackson 은 3(`tools.jackson`)** — `tools.jackson.databind.ObjectMapper` · `tools.jackson.module:jackson-module-kotlin` 이다. 관성으로 `com.fasterxml` 모듈을 넣으면 조용히 무시된다(`build.gradle.kts:80-84`). 단, 애노테이션(`@JsonAlias` · `@JsonIgnore`)은 여전히 `com.fasterxml.jackson.annotation` 패키지다(`observation_result_integration_event.kt:3-5`).
7. **감사 컬럼이 EPOCH/null 로 저장되면 orm.xml 을 의심** — 리스너는 `@EntityListeners` 가 아니라 `R/META-INF/orm.xml` + `spring.jpa.mapping-resources` 로 등록된다(ArchUnit 때문). 리스너 FQCN 은 문자열이라 클래스를 옮기면 컴파일은 통과하고 런타임에만 조용히 깨진다(`orm.xml:6`).
8. **적용된 changeSet 은 주석 한 줄도 고치지 않는다** — checksum 이라 이미 적용된 DB 에서 부팅이 멈춘다(`db.changelog-master.yaml:7-9`). 001 의 `BY_CATEGORY …` 낡은 주석과 `default_period_days` 컬럼이 남아 있는 이유. DROP 은 `R/db/manual/*.sql` 로 사람이 실행한다.
9. **"이벤트가 안 들어온다" 는 ERROR 로그 한 줄로만 드러난다** — 구독 등록 실패도, Kafka 브로커 부재도 부팅을 막지 않는다(`event_bus_subscription_initializer.kt:31-38`, `application.yml:71-81`). 같은 이유로 `kafka_namespace` 가 다른 BC 와 다르면 "전부 정상인데 이벤트만 끊긴다"(`application.yml:88-89`). 기동 로그와 `inbox-summary` API 를 먼저 본다.
10. **UUID 는 `strict_uuid_or_null` 로만** — `UUID.fromString` 은 `"1-1-1-1-1"` 도 받는다(`uuid_format.kt:5-6`). 환자·수진 id 가 UUID 가 아니면 FAILED(영구 실패), 진료과·의사·보고자 id 는 경고 후 null(`observation_payload_mapper.kt:199-209`). 토큰 `sub` 가 UUID 가 아니면 401 `AUTH_TOKEN_INVALID`(`token_principal_scope.kt:18-20`).

보너스(보안): `R/application.yml:188` 의 `api-key` 기본값에 **평문 키**가 들어 있다 — 교육자료 인용에서는 가렸다. 같은 파일 주석(`:24`, `:172`)은 "자격증명은 소스에 두지 않는다" 고 하므로 규칙과 어긋나는 지점이다.
