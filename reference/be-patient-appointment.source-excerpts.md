# patient-appointment 백엔드 — 신입 교육용 소스 모음

대상 모듈: `x-patient-appointment/backend/patient-appointment`
(Kotlin · Spring Boot 4 · CQRS · QueryDSL · Liquibase · common-eventbus-kt)

경로 표기 약어 — 아래에서 **`$B`** = `x-patient-appointment/backend/patient-appointment/src/main/kotlin/com/projectx/patientappointment`,
**`$R`** = `x-patient-appointment/backend/patient-appointment/src/main/resources`,
**`$T`** = `x-patient-appointment/backend/patient-appointment/src/test/kotlin/com/projectx/patientappointment`.
인용은 전부 실제 소스 그대로이며 `경로:시작줄-끝줄` 을 붙였다.

---

## 1. 패키지 트리

`find src/main/kotlin -name '*.kt'` 결과 **218개** 파일(레이어별: presentation 18 · application 91 · domain 49 · infrastructure 59 · 루트 1). 총 약 35,350 줄.

### 1.1 루트
```
patient_appointment_application.kt            ← @SpringBootApplication (1)
```

### 1.2 presentation (18)
```
presentation/
  appointments_controller.kt              ← 슬롯 검색 · 예약 등록/이동/취소/묶음/회차 · 내부(접수부착/시행/노쇼)
  booking_closures_controller.kt          ← 마감/마감취소
  chemotherapy_registrations_controller.kt← 항암 대상 환자
  clinic_hours_controller.kt              ← 진료시간 기준관리 + 전역 휴무 · 공휴일
  inbox_messages_controller.kt            ← 수신 inbox 운영 API
  medication_schedules_controller.kt      ← 투약 일정(외부 포트 중계)
  mobile_appointments_controller.kt       ← 모바일 예약
  overbook_allowances_controller.kt       ← 오버부킹 가능 범위
  patient_schedules_controller.kt         ← 환자별 예약 스케줄 · 메모
  patterns_controller.kt                  ← 패턴 · 생성 규칙
  period_comments_controller.kt           ← 기간 비고
  schedule_operations_controller.kt       ← 반영 · 휴무 · 일자조정 · 재배정 · 각인 · 운영 일괄
  schedules_controller.kt                 ← 스케줄 · 현황 · 현황판 · 진료과/의사 목록
  common_requests.kt · request_dates.kt · request_ids.kt · token_principal_scope.kt · inbox_error_advice.kt
```

### 1.3 application (91)
```
application/
  commands/ (2)  command.kt · command_bus.kt
  commands/appointment/ (7)   appointment_commands · appointment_command_handlers · appointment_requests_commands ·
                               encounter_booking_commands · encounter_reply_commands · mobile_appointment_command_handler · order_receive_commands
  commands/chemotherapyregistration/ (1) · clinichours/ (3) · employeeschedule/ (1) · patientschedulenote/ (1) · reassignment/ (2)
  commands/schedule/ (10)     booking_closure · day_override · operation · overbook_allowance · pattern · period_comment ·
                               schedule_command_handlers · schedule_commands · schedule_comment · schedule_operation
  integrationevents/ (1)      integration_event.kt
  integrationevents/eventhandling/ (6)  appointment_request_handler · erp_employee_schedule_handler · event_tenant_resolver ·
                               idempotent_integration_event_handler · inbound_ids · order_based_appointment_handler
  integrationevents/events/ (5)  appointment_integration_event · encounter_request_integration_events ·
                               erp_employee_schedule_integration_event · order_based_appointment_integration_events · outbound_integration_events
  ports/ (2)                  i_deployment_tenant_port · i_inbox_operation_port
  queries/ (17)               *_queries.kt(Query IF) + *_view_models.kt(view_model)
  support/ (32)               appointment_editor · appointment_drafter · planned_encounter_service · slot_target_service ·
                               schedule_dashboard_service · schedule_overview_service · available_time_service · recommendation_service …
  validations/ (1)            command_validator.kt
```

### 1.4 domain (49)
```
domain/
  aggregatesmodel/appointment/ (4)   appointment · appointment_change · appointment_phase · appointment_snapshots
  aggregatesmodel/schedule/ (14)     schedule · slot · schedule_pattern · pattern_detail · pattern_generation_rule · pattern_assignment ·
                                     unavailability_rule · day_override · schedule_comment · period_comment(+revision) · overbook_allowance(+revision) · eligibility
  aggregatesmodel/clinichours/ (3)   clinic_hours_policy · clinic_hours_values · weekday_hours
  aggregatesmodel/chemotherapyregistration/ (2) · patientschedulenote/ (2) · employeeschedule/ (1) · reassignment/ (1)
  aggregatesmodel/appointment_repositories.kt   ← Repository IF 전부(1)
  common/ (2)        appointment_enums.kt(enum 60여 개 + clinic_zone) · clinic_time.kt
  events/ (1)        appointment_domain_events.kt
  exceptions/ (1)    appointment_exceptions.kt
  ports/ (1)         external_ports.kt
  seedwork/ (7)      entity · seedwork · strings · tenant_scope · uuid7 · uuid_format · value_object
  services/ (9)      appointment_planner · chemotherapy_cycle_plan · encounter_grouping_policy · move_together_policy ·
                     patient_schedule_policy · planning_model · reassignment_policy · schedule_expansion_policy · slot_occupation_service
```

### 1.5 infrastructure (59)
```
infrastructure/
  appointment_context.kt · domain_event_dispatcher.kt (2)
  config/ (6)        appointment_configs · db_schema_initializer · deployment_tenant_adapter · dotenv_environment_post_processor ·
                     global_exception_handler · root_swagger_redirect_config
  eventbus/ (6)      domain_event_integration_publisher · encounter_request_event_adapter · event_actor ·
                     event_bus_subscription_initializer · received_event · topic_registry
  inbox/ (15)        inbox_receiver · inbox_message · inbox_dispatcher · inbox_processor · inbox_tx_service · inbox_sweeper · … 
  persistence/ (1)   audit_entity_listener
  ports/ (6)         actor_availability_{http,stub}_adapter · directory_http_adapters · external_port_adapters · holiday_calendar_adapters · medication_schedule_http_adapter
  queries/ (19)      *_query_dsl.kt(QueryDSL 구현) · tenant_predicates · schedule_source_loader · inbox_message_queries
  repositories/ (1)  appointment_repositories.kt(JPA 어댑터 전부)
  shared/ (3)        shared_patient · shared_encounter · shared_user_account
```

### 1.6 테스트 트리(`src/test`, 74 파일)
```
architecture/layer_isolation_test.kt                       ← ArchUnit
domain/ (24)        slot_and_appointment_test · schedule_expansion_test · clinic_hours_policy_test · overbook_allowance_test …
application/ (25)   encounter_booking_test · order_receive_test · slot_target_service_*_test · schedule_dashboard_service_test …
infrastructure/ (9) slot_persistence_test · appointment_search_query_test · directory_http_adapters_test …
persistence/ (6)    schema_mapping_test · shared_join_query_test · audit_and_tenant_columns_test · persistence_test_support
inbox/ (7)          inbox_flow_test · inbox_dispatcher_test · inbox_receiver_test …
integrationevents/ (3) · presentation/inbox_error_advice_test.kt · test_ids.kt
```

### 1.7 리소스
```
$R/application.yml · application-dev.yml
$R/META-INF/orm.xml · spring.factories
$R/common-libs/common-masking.yaml · common-observability.yaml
$R/db/changelog/db.changelog-master.yaml + changes/001~020-*.xml
$R/db/manual/*.sql   ← master include 금지(사람이 실행)
```

---

## 2. API 추적표

공통: 모든 컨트롤러가 `@RequestMapping("/api/v1/patient-appointment")` + `@RequireAuth`(inbox 제외 전부 `@TokenPrincipal principal` → `principal.to_tenant_scope()`).
쓰기는 `bus.send(scope, command)` 한 번(= Command 1개), 조회는 `i_*_queries`(QueryDSL) 또는 `application/support/*_service`.
**트랜잭션 경계 = 핸들러의 `@Transactional`**(쓰기) / `@Transactional(readOnly = true)`(QueryDSL 구현 · 일부 support 서비스). 컨트롤러에는 `@Transactional` 이 없다.
테이블명 = Entity 클래스명(스키마 `patient_appointment`). `schedule` 애그리게잇의 자식 테이블 = `schedule_pattern · pattern_detail · pattern_generation_rule · pattern_assignment · unavailability_rule · day_override · schedule_comment · period_comment(_revision) · overbook_allowance(_revision)`(+ `@ElementCollection` 컬렉션 테이블 `schedule_allowed_item · day_override_block`). 근거: `$T/persistence/persistence_test_support.kt:44-56`.

### 2.1 appointments_controller (`$B/presentation/appointments_controller.kt`)

| HTTP | 경로 | Controller.메서드(줄) | Command/Query | Handler/Query 구현 | Repository / Q타입 | 테이블 | 응답 view_model |
|---|---|---|---|---|---|---|---|
| GET | /slots | slots (106) | `slot_filter` → `i_slot_queries.find` | `slot_query_dsl.find` (@Tx ro) | `Qslot · Qschedule · Qpattern_detail · Qpattern_generation_rule` | slot · schedule · pattern_detail · pattern_generation_rule | `items_view_model<slot_view_model>` |
| GET | /slots/{slotId} | slot (141) | `i_slot_queries.find_by_id` | `slot_query_dsl.find_by_id` | Qslot · Qschedule | slot · schedule | `slot_view_model` |
| GET | /slots/find | find_slot_days (146) | `i_slot_queries.find_days` | `slot_query_dsl.find_days` | Qslot | slot | `slot_find_view_model` |
| GET | /slots/available-times | available_times (182) | `available_time_request` → `available_time_service.find` | support/available_time_service → `i_slot_queries` | Qslot | slot | `available_times_view_model` |
| GET | /appointments | appointments (220) | `appointment_filter` → `i_appointment_queries.find` | `appointment_query_dsl.find` (@Tx ro) | `Qappointment · Qshared_patient`(서브쿼리) + `i_shared_directory_queries.patients` | appointment · appointment_phase(EAGER) · **shared.patient** | `items_view_model<appointment_view_model>` |
| GET | /appointments/{appointmentId} | appointment (265) | `i_appointment_queries.find_by_id` | `appointment_query_dsl.find_by_id` | Qappointment | appointment · shared.patient | `appointment_view_model` |
| GET | /appointment-patients | appointment_patients (274) | `appointment_patient_filter` → `find_patient_list` | `appointment_query_dsl.find_patient_list` | Qappointment + shared_directory | appointment · shared.patient | `items_view_model<appointment_patient_view_model>` |
| POST | /appointments (201) | create_appointment (304) | `create_appointment_command` | `create_appointment_command_handler.handle` @Tx | `i_appointment_repository` (+appointment_drafter) | appointment | `appointment_view_model` |
| POST | /appointments/draft | draft (316) | `create_appointment_command` → `appointment_draft_service.draft` | support(저장 없음, @Tx ro) | i_appointment_repository(읽기) | appointment | `appointment_view_model` |
| POST | /appointments/find-optimal-dates | find_optimal_dates (324) | `recommendation_query` → `recommendation_service.recommend` | support(@Tx ro) · `appointment_planner` | i_appointment_repository · i_schedule_repository · i_slot_repository · i_slot_queries | appointment · schedule · slot | `recommendation_response_view_model` |
| POST | /appointments/{id}/book | book (334) | `book_appointment_command` | `book_appointment_command_handler.handle` @Tx | `i_appointment_repository · i_slot_repository`(+ appointment_editor · slot_occupation_service · planned_encounter_service) | appointment · slot (+schedule 읽기) | `book_result_view_model` |
| PATCH | /appointments/{id}/reschedule | reschedule (355) | `reschedule_appointment_command`(If-Match → expected_version) | `reschedule_appointment_command_handler` @Tx | 위 + `overbook_slot_locator` | appointment · appointment_change · slot | `reschedule_result_view_model` |
| GET | /appointments/{id}/changes | changes (380) | `i_appointment_queries.find_changes` | `appointment_query_dsl.find_changes` | `Qappointment_change` | appointment_change | `items_view_model<appointment_change_view_model>` |
| GET | /appointment-series/{seriesGroupId}/changes | series_changes (385) | `find_series_changes` | `appointment_query_dsl.find_series_changes` | Qappointment · Qappointment_change | appointment · appointment_change | `items_view_model<series_change_view_model>` |
| GET | /appointments/{id}/move-partners | move_partners (398) | `move_partner_service.partners` | support → i_appointment_queries · i_schedule_queries · i_slot_queries · `move_together_policy` | Qappointment · Qschedule · Qslot | appointment · schedule · slot | `move_partners_view_model` |
| POST | /appointments/{id}/cancel | cancel (412) | `cancel_appointment_command` | `cancel_appointment_command_handler` @Tx | i_appointment_repository · i_slot_repository · `i_reassignment_task_repository` | appointment · slot · reassignment_task | `cancel_result_view_model` |
| POST | /appointments/book-bundle | book_bundle (431) | `book_bundle_command` | `book_bundle_command_handler` @Tx (전체 성공/실패) | 위 + appointment_drafter · overbook_slot_locator | appointment · appointment_phase · slot · reassignment_task | `bundle_result_view_model` |
| POST | /appointments/book-series | book_series (469) | `book_series_command` | `book_series_command_handler` @Tx (부분 성공 허용) | i_appointment_repository · i_slot_repository | appointment · slot | `series_result_view_model` |
| POST | /internal/appointments/{id}/attach-encounter | attach_encounter (494) | `attach_encounter_command` | `attach_encounter_command_handler` @Tx | i_appointment_repository | appointment | `encounter_attach_result_view_model` |
| POST | /internal/appointments/{id}/fulfill | fulfill (503) | `fulfill_appointment_command` | `fulfill_appointment_command_handler` @Tx | i_appointment_repository | appointment | `appointment_view_model` |
| POST | /internal/appointments/{id}/no-show | no_show (508) | `no_show_appointment_command` | `no_show_appointment_command_handler` @Tx | i_appointment_repository | appointment | `appointment_view_model` |

핸들러 위치: `$B/application/commands/appointment/appointment_command_handlers.kt:52-724`(create 52 · book 86 · reschedule 133 · cancel 241 · book_bundle 298 · book_series 558 · attach 635 · fulfill 651 · no_show 667 · void 687). `@Transactional` 10회(핸들러 메서드마다).

### 2.2 mobile_appointments_controller (`$B/presentation/mobile_appointments_controller.kt`)

| HTTP | 경로 | 메서드(줄) | Command/Query | 구현 | Repository | 테이블 | 응답 |
|---|---|---|---|---|---|---|---|
| GET | /mobile/available-times | available_times (46) | `available_time_request`(category=OUTPATIENT_CLINIC · route=mobile · role=PATIENT_APP 고정) → `available_time_service.find` | support → i_slot_queries | Qslot | slot | `available_times_view_model` |
| POST | /mobile/appointments (201) | book (78) | `book_mobile_appointment_command` | `book_mobile_appointment_command_handler` @Tx (`mobile_appointment_command_handler.kt:39`) | i_appointment_repository · i_slot_repository · appointment_editor · slot_occupation_service · planned_encounter_service | appointment · slot | `mobile_booking_result_view_model` |

### 2.3 schedules_controller (`$B/presentation/schedules_controller.kt`)

| HTTP | 경로 | 메서드(줄) | Command/Query | 구현 | Repository/Q타입 | 테이블 | 응답 |
|---|---|---|---|---|---|---|---|
| GET | /value-sets/{valueSetId}/expand | expand_value_set (98) | `value_set_service.expand` | support(메모리 enum) | — | — | `value_set_view_model` |
| GET | /schedules | schedules (114) | `schedule_filter` → `i_schedule_queries.find` | `schedule_query_dsl.find` | Qschedule | schedule | `items_view_model<schedule_view_model>` |
| GET | /slot-targets | slot_targets (144) | `slot_target_service.of` | support → i_room/therapist/department/physician_directory_port(HTTP) + i_schedule_queries | Qschedule | schedule + 외부 원천 | `items_view_model<slot_target_view_model>` |
| GET | /schedules/{scheduleId} | schedule (152) | `i_schedule_queries.find_by_id` | `schedule_query_dsl.find_by_id` | Qschedule | schedule | `schedule_view_model` |
| POST | /schedules (201) | create_schedule (157) | `create_schedule_command` | `create_schedule_command_handler` @Tx (`schedule_command_handlers.kt:31`) | i_schedule_repository | schedule · schedule_allowed_item | `schedule_view_model` |
| PATCH | /schedules/{scheduleId} | patch_schedule (165) | `change_schedule_policy_command`(If-Match) | `change_schedule_policy_command_handler` @Tx | i_schedule_repository · schedule_editor | schedule | `schedule_command_result` |
| PUT | /schedules/{scheduleId}/comment | put_comment (178) | `change_schedule_comment_command` | `change_schedule_comment_command_handler` @Tx | i_schedule_repository | schedule · schedule_comment | `schedule_comment_result` |
| GET | /schedules/{scheduleId}/comments | comments (187) | `i_schedule_comment_queries.find` | `schedule_comment_query_dsl` | Qschedule_comment · Qshared_user_account | schedule_comment · shared.user_account | `items_view_model<schedule_comment_view_model>` |
| GET | /schedule-overview | schedule_overview (196) | `schedule_overview_filter` → `schedule_overview_service.overview` | support → `i_schedule_overview_queries`(`schedule_overview_query_dsl`) + i_physician_directory_port | Qschedule · (전개 `schedule_expansion_policy`) | schedule + 자식 | `schedule_overview_view_model` |
| GET | /schedule-dashboard | schedule_dashboard (229) | `schedule_dashboard_filter` → `schedule_dashboard_service.dashboard`(@Tx ro) | `i_schedule_dashboard_queries`(`schedule_dashboard_query_dsl`) + `actor_not_available_reader` | Qschedule + 전개 | schedule + 자식 | `schedule_dashboard_view_model` |
| GET | /schedule-daily-status | schedule_daily_status (250) | `schedule_daily_filter` → `schedule_daily_status_service.daily` | `i_schedule_daily_queries`(`schedule_daily_query_dsl`) + i_period_comment_queries | Qslot · Qschedule · Qperiod_comment | slot · schedule · period_comment | `schedule_daily_status_view_model` |
| GET | /schedules/{scheduleId}/month-overview | month_overview (274) | `month_overview_service.of_schedule` | support → i_schedule_repository · i_slot_repository · i_reassignment_task_queries | — (JPA 로딩) | schedule · slot · reassignment_task | `month_overview_view_model` |
| GET | /departments/{departmentId}/schedules/summary | department_summary (283) | `i_schedule_queries.summarize_department` | `schedule_query_dsl` | Qschedule | schedule | `items_view_model<schedule_summary_view_model>` |
| GET | /departments | departments (298) | `department_directory_service.of` | support → `i_department_directory_port`(HTTP 조직 BC) | — | 외부 | `items_view_model<department_view_model>` |
| GET | /departments/{departmentId}/physicians | department_physicians (310) | `physician_directory_service.of` | i_physician_directory_port(HTTP) + i_schedule_queries | Qschedule | schedule + 외부 | `items_view_model<physician_directory_view_model>` |
| GET | /departments/{departmentId}/schedules/month-overview | department_month_overview (318) | `month_overview_service.of_department` | 위와 동일 | — | schedule · slot | `department_month_overview_view_model` |

### 2.4 schedule_operations_controller (`$B/presentation/schedule_operations_controller.kt`)

| HTTP | 경로 | 메서드(줄) | Command/Query | 구현(`operation_command_handlers.kt` 등) | Repository | 테이블 | 응답 |
|---|---|---|---|---|---|---|---|
| GET | /schedules/{id}/pattern-assignments | assignments (87) | `schedule_day_service.assignments_of` | support → i_schedule_repository | — | schedule · pattern_assignment | `items_view_model<pattern_assignment_view_model>` |
| POST | /schedules/{id}/pattern-assignments/assign | assign (95) | `assign_pattern_command`(dryRun) | `assign_pattern_command_handler` @Tx (:62) | i_schedule_repository · i_slot_repository · schedule_expansion_policy | pattern_assignment · slot | `pattern_assignment_view_model` |
| PATCH | /pattern-assignments/{assignmentId} | change_assignment (116) | `change_assignment_command` | `change_assignment_command_handler` @Tx (:139) | i_schedule_repository | pattern_assignment | `pattern_assignment_view_model` |
| DELETE | /pattern-assignments/{assignmentId} | unassign (132) | `unassign_pattern_command` | `unassign_pattern_command_handler` @Tx (:156) | i_schedule_repository | pattern_assignment | `assignment_removed_view_model` |
| GET | /schedules/{id}/unavailability-rules | unavailability_rules (143) | `schedule_day_service.unavailability_of` | support → i_schedule_repository | — | unavailability_rule | `items_view_model<unavailability_rule_view_model>` |
| POST | /schedules/{id}/unavailability-rules (201) | create_unavailability (165) | `add_unavailability_command` | `add_unavailability_command_handler` @Tx (:247) + `unavailability_reassignment_support`(i_appointment_repository · i_reassignment_task_repository) | i_schedule_repository · i_slot_repository · i_appointment_queries | unavailability_rule · slot · reassignment_task | `unavailability_rule_view_model` |
| PATCH | /unavailability-rules/{ruleId} | change_unavailability (190) | `change_unavailability_command` | `change_unavailability_command_handler` @Tx (:312) | i_schedule_repository · i_slot_repository | unavailability_rule · slot | `unavailability_rule_view_model` |
| DELETE | /unavailability-rules/{ruleId} | delete_unavailability (207) | `remove_unavailability_command` | `remove_unavailability_command_handler` @Tx (:375) | 위 | unavailability_rule · slot | `unavailability_rule_view_model` |
| GET | /schedules/{id}/schedule-days | schedule_days (218) | `schedule_day_service.expand` | support(저장 없음) | i_schedule_repository · i_slot_repository | schedule · slot | `schedule_days_view_model` |
| GET | /schedules/{id}/day-overrides | day_overrides (232) | `i_day_override_queries.find` | `override_reassignment_query_dsl`(:33) | Qday_override | day_override · day_override_block | `items_view_model<day_override_view_model>` |
| PUT | /schedules/{id}/day-overrides/{date} | put_day_override (243) | `put_day_override_command`(dryRun) | `put_day_override_command_handler` @Tx (`day_override_command_handlers.kt:39`) | i_schedule_repository · i_slot_repository · i_appointment_queries | day_override · slot | `day_override_view_model` |
| GET | /schedules/{id}/reassignment-tasks | schedule_reassignment_tasks (274) | `reassignment_task_filter` → `i_reassignment_task_queries.find` | `reassignment_task_query_dsl`(:68) | Qreassignment_task + shared_directory | reassignment_task · shared.patient | `items_view_model<reassignment_task_view_model>` |
| GET | /reassignment-tasks | reassignment_tasks (294) | 동일 | 동일 | 동일 | 동일 | 동일 |
| POST | /reassignment-tasks/{taskId}/complete | complete_reassignment_task (304) | `complete_reassignment_task_command` | `reassignment_command_handlers.kt:21` @Tx | i_reassignment_task_repository | reassignment_task | `reassignment_task_result_view_model` |
| POST | /reassignment-tasks/{taskId}/cancel | cancel_reassignment_task (313) | `cancel_reassignment_task_command` | `reassignment_command_handlers.kt:39` @Tx | 동일 | 동일 | 동일 |
| POST | /schedules/{id}/activate | activate (328) | `activate_slots_command` | `activate_slots_command_handler` @Tx (:455) | i_schedule_repository · i_slot_repository · i_appointment_repository · i_reassignment_task_repository · schedule_expansion_policy | slot(대량 insert/delete) · reassignment_task | `activation_view_model` |
| POST | /schedules/{id}/operations | operations (350) | `schedule_operation_command` | `schedule_operation_command_handler` @Tx (`schedule_operation_command_handler.kt:36`) — 단건 핸들러들을 **같은 트랜잭션에서 순서대로 호출** | 위 핸들러들의 합 | 위 합 | `schedule_operation_view_model` |

### 2.5 patterns_controller (`$B/presentation/patterns_controller.kt`) — 핸들러는 전부 `pattern_command_handlers.kt` · `schedule_command_handlers.kt`, 의존은 `i_schedule_repository + schedule_editor`, 테이블은 `schedule_pattern · pattern_detail · pattern_generation_rule`.

| HTTP | 경로 | 메서드(줄) | Command/Query | 구현(줄) | 응답 |
|---|---|---|---|---|---|
| GET | /schedules/{id}/patterns | patterns (66) | `i_pattern_queries.find_summaries` | `schedule_query_dsl.kt:172`(pattern_query_dsl) | `items_view_model<pattern_summary_view_model>` |
| POST | /schedules/{id}/patterns (201) | create_pattern (75) | `create_pattern_command` | `create_pattern_command_handler` @Tx (:41) | `pattern_view_model` |
| GET | /patterns/{patternId} | pattern (94) | `i_pattern_queries.find_by_id` | schedule_query_dsl | `pattern_view_model` |
| PUT | /patterns/{patternId} | replace_pattern (99) | `replace_pattern_command`(If-Match) | `replace_pattern_command_handler` @Tx (:180) | `pattern_view_model` |
| PATCH | /patterns/{patternId} | rename_pattern (118) | `rename_pattern_command` | `rename_pattern_command_handler` @Tx (:211) | `pattern_view_model` |
| POST | /patterns/{patternId}/deactivate | deactivate_pattern (134) | `deactivate_pattern_command` | `schedule_command_handlers.kt:141` @Tx | `pattern_view_model` |
| POST | /patterns/{patternId}/restore | restore_pattern (141) | `restore_pattern_command` | `schedule_command_handlers.kt:161` @Tx | `pattern_view_model` |
| PUT | /patterns/{patternId}/details | put_detail (148) | `put_pattern_detail_command` | `put_pattern_detail_command_handler` @Tx (:230) | `pattern_view_model` |
| DELETE | /patterns/{patternId}/details | remove_detail (160) | `remove_pattern_detail_command` | `remove_pattern_detail_command_handler` @Tx (:251) | `pattern_view_model` |
| POST | /patterns/{patternId}/copy-to | copy_pattern (193) | `copy_pattern_command` | `copy_pattern_command_handler` @Tx (:283) | `pattern_copy_view_model` |
| GET | /patterns/{patternId}/generation-rules | rules (214) | `i_pattern_queries.find_by_id` → `.rule` | schedule_query_dsl | `items_view_model<generation_rule_view_model>` |
| POST | /patterns/{patternId}/generation-rules (201) | create_rule (221) | `create_generation_rule_command`(applyNow/adopt) | `create_generation_rule_command_handler` @Tx (:352) | `generation_apply_view_model` |
| PUT | /generation-rules/{ruleId} | update_rule (245) | `update_generation_rule_command`(If-Match) | (:410) @Tx | `generation_apply_view_model` |
| POST | /generation-rules/{ruleId}/apply | apply_rule (266) | `apply_generation_rule_command` | (:444) @Tx | `generation_apply_view_model` |
| DELETE | /generation-rules/{ruleId} | delete_rule (273) | `delete_generation_rule_command` | (:471) @Tx | `generation_rule_deleted_view_model` |
| PUT | /patterns/{patternId}/generation-rules/order | reorder_rules (280) | `reorder_generation_rules_command` | (:492) @Tx | `pattern_view_model` |

### 2.6 clinic_hours_controller (`$B/presentation/clinic_hours_controller.kt`)

| HTTP | 경로 | 메서드(줄) | Command/Query | 구현 | Repository | 테이블 | 응답 |
|---|---|---|---|---|---|---|---|
| GET | /clinic-hours-policies | policies (71) | `i_clinic_hours_queries.units` | `clinic_hours_query_dsl`(:37) | Qclinic_hours_policy · Qweekday_hours · shared_directory | clinic_hours_policy · weekday_hours · shared.user_account | `items_view_model<clinic_hours_policy_view_model>` |
| GET | /clinic-hours-policies/effective | effective (75) | `i_clinic_hours_queries.effective` | 동일 | 동일 | 동일 | `clinic_hours_policy_view_model` |
| GET | /clinic-hours-policies/{policyId} | policy (82) | `i_clinic_hours_queries.find` | 동일 | 동일 | 동일 | 동일 |
| POST | /clinic-hours-policies (201) | create (87) | `create_clinic_hours_policy_command` | `clinic_hours_command_handlers.kt:68` @Tx | `i_clinic_hours_policy_repository` + clinic_hours_editor | clinic_hours_policy · weekday_hours | `clinic_hours_policy_view_model` |
| PUT | /clinic-hours-policies/{policyId} | replace (99) | `replace_clinic_hours_policy_command`(If-Match) | (:100) @Tx | 동일 | 동일 | 동일 |
| DELETE | /clinic-hours-policies/{policyId} | delete (108) | `delete_clinic_hours_policy_command` | (:184) @Tx | 동일 | 동일 | 동일 |
| GET | /unavailability-rules | global_rules (117) | `i_clinic_hours_queries.global_rules` | clinic_hours_query_dsl | Qunavailability_rule(schedule_id IS NULL) | unavailability_rule | `items_view_model<unavailability_rule_view_model>` |
| POST | /unavailability-rules (201) | create_global (129) | `add_global_unavailability_command` | `holiday_command_handlers.kt:41` @Tx | i_schedule_repository(`save_global`) · i_clinic_hours_queries | unavailability_rule | `unavailability_rule_view_model` |
| POST | /unavailability-rules/holidays/sync | sync_holidays (147) | `sync_holidays_command` | `holiday_command_handlers.kt:102` @Tx | i_schedule_repository · i_slot_repository · i_clinic_hours_policy_repository · `i_holiday_calendar_port`(stub/http) | unavailability_rule · slot · clinic_hours_policy | `holiday_sync_view_model` |

### 2.7 booking_closures · overbook_allowances · period_comments · patient_schedules · chemotherapy · medication

| HTTP | 경로 | Controller.메서드(파일:줄) | Command/Query | 구현 | Repository | 테이블 | 응답 |
|---|---|---|---|---|---|---|---|
| POST | /booking-closures (201) | booking_closures_controller.close (:39) | `close_booking_command` | `booking_closure_commands.kt:48` @Tx | i_schedule_repository | unavailability_rule(마감=휴무 규칙) · slot | `booking_closure_view_model` |
| DELETE | /booking-closures/{ruleId} | .reopen (:61) | `reopen_booking_command` | `booking_closure_commands.kt:99` @Tx | schedule_editor | 동일 | 동일 |
| GET | /schedules/{id}/overbook-allowances | overbook_allowances_controller.overbook_allowances (:48) | `i_overbook_allowance_queries.find` | `overbook_allowance_query_dsl`(:25) | Qoverbook_allowance | overbook_allowance | `items_view_model<overbook_allowance_view_model>` |
| POST | /schedules/{id}/overbook-allowances (201) | .add (:56) | `add_overbook_allowance_command` | `overbook_allowance_command_handlers.kt:30` @Tx | i_schedule_repository · i_shared_directory_queries | overbook_allowance · overbook_allowance_revision | `overbook_allowance_result` |
| PUT | /overbook-allowances/{id} | .change (:76) | `change_overbook_allowance_command`(If-Match) | (:60) @Tx | 동일 | 동일 | 동일 |
| POST | /overbook-allowances/{id}/remove | .remove (:97) | `remove_overbook_allowance_command` | (:93) @Tx | 동일 | 동일 | 동일 |
| GET | /schedules/{id}/overbook-allowance-revisions | .revisions (:111) | `i_overbook_allowance_queries.revisions` | overbook_allowance_query_dsl | Qoverbook_allowance_revision | overbook_allowance_revision · shared.user_account | `items_view_model<overbook_allowance_revision_view_model>` |
| GET | /period-comments | period_comments_controller.period_comments (:48) | `i_period_comment_queries.find` | `period_comment_query_dsl`(:28) | Qperiod_comment · Qshared_user_account | period_comment · shared.user_account | `items_view_model<period_comment_view_model>` |
| POST | /schedules/{id}/period-comments (201) | .add (:66) | `add_period_comment_command` | `period_comment_command_handlers.kt:30` @Tx | i_schedule_repository · i_shared_directory_queries | period_comment · period_comment_revision | `period_comment_result` |
| PUT | /period-comments/{id} | .change (:86) | `change_period_comment_command`(If-Match) | (:59) @Tx | 동일 | 동일 | 동일 |
| POST | /period-comments/{id}/remove | .remove (:107) | `remove_period_comment_command` | (:93) @Tx | 동일 | 동일 | 동일 |
| GET | /schedules/{id}/period-comment-revisions | .revisions (:121) | `i_period_comment_queries.revisions` | period_comment_query_dsl | Qperiod_comment_revision | period_comment_revision | `items_view_model<period_comment_revision_view_model>` |
| GET | /patient-schedules/{patientId} | patient_schedules_controller.schedule (:43) | `patient_schedule_filter` → `patient_schedule_service.schedule` | support → i_patient_schedule_note_queries · i_appointment_queries · `patient_schedule_policy` | Qpatient_schedule_note · Qappointment | patient_schedule_note · appointment · shared.patient | `patient_schedule_view_model` |
| GET | /patient-schedule-notes/{patientId}/revisions | .revisions (:59) | `i_patient_schedule_note_queries.revisions` | `patient_schedule_note_query_dsl`(:23) | Qpatient_schedule_note_revision | patient_schedule_note_revision | `items_view_model<…revision_view_model>` |
| PUT | /patient-schedule-notes/{patientId} | .save (:67) | `save_patient_schedule_note_command`(If-Match) | `patient_schedule_note_commands.kt:44` @Tx | i_patient_schedule_note_repository · i_shared_directory_queries | patient_schedule_note(+revision) | `patient_schedule_note_result` |
| GET | /chemotherapy-registrations | chemotherapy_registrations_controller.registrations (:53) | `chemotherapy_registration_filter` → `chemotherapy_registration_service.list` | support → i_chemotherapy_registration_queries(`chemotherapy_registration_query_dsl`:32) | Qchemotherapy_registration · Qappointment | chemotherapy_registration · appointment · shared.patient | `items_view_model<chemotherapy_registration_view_model>` |
| GET | /chemotherapy-registrations/{id} | .registration (:73) | `service.detail` + `chemotherapy_cycle_plan` | 동일 | 동일 | 동일 | `chemotherapy_registration_detail_view_model` |
| GET | /chemotherapy-registrations/{id}/revisions | .revisions (:80) | `queries.revisions` | 동일 | Qchemotherapy_registration_revision | chemotherapy_registration_revision | `items_view_model<…revision_view_model>` |
| POST | /chemotherapy-registrations (201) | .register (:88) | `register_chemotherapy_command` | `chemotherapy_registration_commands.kt:92` @Tx | i_chemotherapy_registration_repository · i_shared_directory_queries | chemotherapy_registration(+revision) | `chemotherapy_registration_result` |
| PUT | /chemotherapy-registrations/{id} | .change (:103) | `change_chemotherapy_registration_command`(If-Match) | (:115) @Tx | 동일 | 동일 | 동일 |
| GET | /medication-schedules | medication_schedules_controller.medication_schedules (:33) | `medication_schedule_service.of` | support → `i_medication_schedule_port`(HTTP, x-ancillary-main 임시) | — | 외부(저장 없음) | `items_view_model<medication_schedule_view_model>` |

### 2.8 inbox_messages_controller (`$B/presentation/inbox_messages_controller.kt`) — `@TokenPrincipal` 없음(테넌트 무관 운영 API), 오류 본문은 `inbox_error_advice` 가 `{ error, message }` 로 따로 낸다.

| HTTP | 경로 | 메서드(줄) | Query/Port | 구현 | 테이블 | 응답 |
|---|---|---|---|---|---|---|
| GET | /inbox-summary | summary (43) | `i_inbox_message_queries.summary` | `infrastructure/queries/inbox_message_queries.kt:42` | inbox_message | `inbox_summary_view` |
| GET | /inbox-messages | find_messages (50) | `inbox_message_criteria` → `find_messages` | 동일(QueryDSL `inbox_message_query_dsl`) | inbox_message | `inbox_message_page_view` |
| GET | /inbox-messages/{id} | find_message (76) | `find_message` | 동일 | inbox_message · inbox_message_action | `inbox_message_detail_view` |
| GET | /inbox-messages/{id}/sequence | find_sequence (81) | `find_sequence` | 동일 | inbox_message | `inbox_sequence_view` |
| GET | /inbox-keys | find_keys (89) | `inbox_key_criteria` → `find_keys` | 동일 | inbox_message | `inbox_key_list_view` |
| POST | /inbox-messages/{id}/retry | retry (102) | `i_inbox_operation_port.retry` | `infrastructure/inbox/inbox_operation_service.kt` | inbox_message · inbox_message_action | `inbox_operation_result` |
| POST | /inbox-message-retries | retry_many (109) | `.retry(ids)` | 동일 | 동일 | 동일 |
| PUT | /inbox-messages/{id}/payload | edit_payload (114) | `.edit_payload` | 동일 | 동일 | 동일 |
| POST | /inbox-messages/{id}/resend | resend (121) | `.resend` | 동일 | 동일 | 동일 |
| POST | /inbox-messages/{id}/terminate | terminate (128) | `.terminate` | 동일 | 동일 | 동일 |
| POST | /inbox-message-terminations | terminate_many (135) | `.terminate(ids)` | 동일 | 동일 | 동일 |

### 2.9 Kafka 수신 → Command (HTTP 아님, 참고)

| 토픽(이벤트명) | payload | 핸들러 | Command | 핸들러 @Tx |
|---|---|---|---|---|
| `employee-schedule` | `erp_employee_schedule_integration_event` | `erp_employee_schedule_handler` | `apply_employee_schedule_command` | `employee_schedule_commands.kt:63` |
| `order-based-appointment-request` | `order_based_appointment_request_integration_event` | `order_based_appointment_handler` | `receive_order_command` | `order_receive_commands.kt:121-142` |
| `appointment-request` | `appointment_request_integration_event` | `appointment_request_handler` | `apply_appointment_requests_command`(→ `apply_encounter_reply_command` / `apply_encounter_booking_command`) | `appointment_requests_commands.kt:33-48` |


---

## 3. 쓰기 경로 샘플(Command) — `POST /appointments/{appointmentId}/book`

대표 엔드포인트 = **예약 등록**(`proposed → booked` + 슬롯 점유 + 접수 요청 + 커밋 뒤 Kafka 발행).
체인: Controller → `book_appointment_command` → `command_bus` → (validator) → `book_appointment_command_handler` →
`slot_occupation_service`(도메인 서비스) → `slot.occupy()` · `appointment.book()`(도메인 이벤트 적재) → `i_appointment_repository`(domain IF) →
`appointment_repository_adapter`(JPA, 이벤트 꺼내기) → `domain_event_dispatcher` → `domain_event_integration_publisher`(`AFTER_COMMIT`) → `appointment_integration_event`(Kafka payload).

### 3.1 Controller — `$B/presentation/appointments_controller.kt:86-98`, `:330-349`, `:703`

```kotlin
@Tag(name = "appointments", description = "슬롯 검색 · 예약(추천 · 예약 등록 · 이동 · 취소)")
@RestController
@RequireAuth
@RequestMapping("/api/v1/patient-appointment")
class appointments_controller(
    private val bus: command_bus,
    private val slots: i_slot_queries,
    private val appointments: i_appointment_queries,
    private val recommendations: recommendation_service,
    private val drafts: appointment_draft_service,
    private val booking_roles: booking_role_resolver,
    private val available_times: available_time_service,
    private val move_partners: move_partner_service,
) {
```

```kotlin
    @Operation(
        summary = "예약 등록 — `proposed → booked` 한 번에 + 슬롯 점유",
        description = "선점 단계가 없다. 그 사이 다른 사람이 같은 칸을 잡았으면 422 SLOT_FULL 또는 409 OPTIMISTIC_LOCK_CONFLICT 로 전부 되돌아간다",
    )
    @PostMapping("/appointments/{appointmentId}/book")
    fun book(
        @TokenPrincipal principal: AuthPrincipal,
        @RequestHeader(value = "X-Role-Code", required = false) role_code: String?,
        @PathVariable("appointmentId") appointment_id: String,
        @RequestBody request: book_request,
    ): book_result_view_model = bus.send(
        principal.to_tenant_scope(),
        book_appointment_command(
            appointment_id = appointment_id,
            slot_id = request.slotId,
            channel = channel_of(request.ext?.bookingChannel),
            role_code = role_code,
            route = route_of(request.ext?.bookingRoute),
        ),
    )
```

```kotlin
data class book_request(val slotId: String, val ext: booking_ext_request? = null)
```

토큰 → 테넌트 범위 변환은 presentation 한 파일에서만 한다 — `$B/presentation/token_principal_scope.kt:1-12`:

```kotlin
package com.projectx.patientappointment.presentation

import com.projectx.common.jwt.dto.AuthPrincipal
import com.projectx.patientappointment.domain.seedwork.tenant_scope

// 토큰 주체 → 테넌트 범위. HTTP 요청의 tenant_id · hospital_code 는 이 파일에서만 나온다.
// AuthPrincipal 은 presentation 밖으로 내보내지 않는다 — application 이하에는 tenant_scope 만 넘긴다.
// 작업자(직원 id)는 넘기지 않는다 — 감사 컬럼은 audit_entity_listener 가 MDC userId(토큰 sub)로 채우고,
// 비고 이력의 작성자도 그 created_by 다.

// claim 이 비어 있으면 tenant_scope.of 가 INVALID_DATA 로 채운다(라이브러리가 필수 claim 을 검증하므로 정상 토큰에서는 생기지 않는다)
fun AuthPrincipal.to_tenant_scope(): tenant_scope = tenant_scope.of(tenantId, hospitalCode)
```

### 3.2 Command 클래스 — `$B/application/commands/appointment/appointment_commands.kt:122-131`

```kotlin
/** 단건 예약 등록 — `proposed → booked` 한 번에 + 슬롯 점유. */
data class book_appointment_command(
    val appointment_id: String,
    val slot_id: String,
    val channel: appointment_booking_channel,
    val role_code: String?,
    /** 예약 경로(원내 · 콜센터 · 모바일 · 인터넷) — 주면 예약 등록 직전에 예약에 남긴다(classdiagram 1.19 · 불변식 44). */
    val route: booking_route? = null,
) : command
```

### 3.3 command · command_handler 마커 — `$B/application/commands/command.kt:1-20`

```kotlin
package com.projectx.patientappointment.application.commands

import com.projectx.patientappointment.domain.seedwork.tenant_scope

/** 모든 명령의 마커 인터페이스. */
interface command

/**
 * 명령 핸들러.
 *
 * [scope] 는 이 명령이 다루는 **테넌트 범위**다 — HTTP 는 토큰(`AuthPrincipal`)이, 수신 이벤트는 `event_tenant_resolver` 가 정한다.
 * 핸들러는 이 범위 안에서만 읽고(리포지토리 · 조회 전부) 새 루트도 이 범위로 만든다.
 * 작업자(직원 id)는 넘기지 않는다 — 감사 컬럼은 `audit_entity_listener` 가 MDC `userId` 로 채운다.
 *
 * @param TCommand 처리할 명령 타입
 * @param TResult  처리 결과 타입
 */
interface command_handler<TCommand : command, TResult> {
    fun handle(command: TCommand, scope: tenant_scope): TResult
}
```

### 3.4 command_bus(디스패치 방식) — `$B/application/commands/command_bus.kt:1-38`

`List<command_handler<*, *>>` 빈 전부를 주입받아 **제네릭 첫 타입(명령 클래스)으로 색인**한다(`ResolvableType`). 검증기 → 핸들러 순.

```kotlin
package com.projectx.patientappointment.application.commands

import com.projectx.patientappointment.application.validations.command_validator
import com.projectx.patientappointment.domain.seedwork.tenant_scope
import org.springframework.core.ResolvableType
import org.springframework.stereotype.Component

/**
 * 명령 디스패처.
 * 등록된 command_handler / command_validator 빈을 명령 타입 기준으로 색인하고,
 * 검증 → 핸들러 순으로 디스패치한다. 한 엔드포인트는 하나의 Command 만 발행한다.
 */
@Component
class command_bus(
    handlers: List<command_handler<*, *>>,
    validators: List<command_validator<*>>,
) {
    private val handler_index: Map<Class<*>, command_handler<*, *>> =
        handlers.associateBy { resolve_generic(it.javaClass, command_handler::class.java) }

    private val validator_index: Map<Class<*>, List<command_validator<*>>> =
        validators.groupBy { resolve_generic(it.javaClass, command_validator::class.java) }

    /** [scope] 는 명령의 테넌트 범위 — 검증 뒤 핸들러에 그대로 넘긴다(command_handler 참고). */
    @Suppress("UNCHECKED_CAST")
    fun <TResult> send(scope: tenant_scope, command: command): TResult {
        val type = command.javaClass
        validator_index[type]?.forEach { (it as command_validator<command>).validate(command) }
        val handler = handler_index[type]
            ?: throw IllegalStateException("등록된 핸들러가 없습니다: ${type.name}")
        return (handler as command_handler<command, TResult>).handle(command, scope)
    }

    /** 구현 클래스에서 대상 인터페이스의 첫 번째 제네릭(명령 타입)을 해석한다. */
    private fun resolve_generic(impl: Class<*>, target: Class<*>): Class<*> =
        ResolvableType.forClass(impl).`as`(target).getGeneric(0).resolve()
            ?: throw IllegalStateException("명령 타입을 해석할 수 없습니다: ${impl.name}")
}
```

### 3.5 Validator — `$B/application/validations/command_validator.kt:1-11`

인터페이스만 있고 **구현 빈은 현재 0개**다(`grep -rn 'command_validator<' $B` 결과 command_bus 참조 외 없음).
입력 검증은 요청 DTO 의 `guard_shape()`(`appointments_controller.kt:719-724`) 와 도메인 예외가 맡는다.

```kotlin
package com.projectx.patientappointment.application.validations

import com.projectx.patientappointment.application.commands.command

/**
 * 명령별 검증기. command_bus 가 핸들러 호출 직전에 호출한다.
 * 검증 실패는 `appointment_validation_exception`(→ HTTP 400 Validation) 으로 던진다.
 */
interface command_validator<TCommand : command> {
    fun validate(command: TCommand)
}
```

### 3.6 Handler — `$B/application/commands/appointment/appointment_command_handlers.kt:86-125`

```kotlin
@Component
class book_appointment_command_handler(
    private val appointments: i_appointment_repository,
    private val slots: i_slot_repository,
    private val editor: appointment_editor,
    private val occupation: slot_occupation_service,
    private val roles: booking_role_resolver,
    private val planned: planned_encounter_service,
    private val clock: Clock,
) : command_handler<book_appointment_command, book_result_view_model> {

    private val log = LoggerFactory.getLogger(book_appointment_command_handler::class.java)

    @Transactional
    override fun handle(command: book_appointment_command, scope: tenant_scope): book_result_view_model {
        val target = editor.load(scope, command.appointment_id)
        val slot_row = editor.load_slot(scope, command.slot_id)
        val owning = editor.load_schedule(scope, slot_row.schedule_id)
        val now = clock.instant()
        // 경로는 예약 등록 직전에 남긴다 — 원내 · 콜센터는 기록만, 모바일 · 인터넷은 경로 축 판정에도 실린다(불변식 44).
        command.route?.takeIf { target.status == appointment_status.PROPOSED }?.let { target.enter_route(it, now) }
        // 역할 축(불변식 16) — `X-Role-Code` 에서 파생한 booking_role 을 slot.allowed_roles 와 대조한다.
        occupation.book(target, slot_row, owning, command.channel, roles.resolve(command.role_code), now)
        // 접수 요청 id 를 같은 트랜잭션에서 남긴다 — 요청은 커밋 뒤 발행되고 encounter_id 는 원무 회신이 붙인다(불변식 27).
        val issues = planned.attach(scope, listOf(target), now)
        slots.save(slot_row)
        val saved = appointments.save(target)
        log.info("예약을 등록했습니다 — appointment_id={} slot_id={}", saved.appointment_id, command.slot_id)
        return book_result_view_model(
            appointment_id = saved.appointment_id,
            status = saved.status.code,
            slot_id = saved.slot_id,
            start_at = saved.start_at,
            booked_at = saved.booked_at,
            version = saved.version,
            encounter_id = saved.encounter_id?.toString(),
            issue = issues,
        )
    }
}
```

핸들러가 공유하는 로딩 절차(행 락 없음) — `$B/application/support/appointment_editor.kt:29-48`:

```kotlin
/**
 * 예약 쓰기 핸들러가 공유하는 로딩 절차.
 *
 * 슬롯은 보통 조회로 읽는다 — **행 락(`FOR UPDATE`)은 잡지 않는다.** 같은 칸을 둘이 동시에 저장하면
 * `slot.version` 낙관적 잠금이 한쪽을 `409 OPTIMISTIC_LOCK_CONFLICT` 로 돌리고 화면이 다시 읽는다.
 * 락이 없으니 데드락 · 락 대기 · 외부 호출 중 락 보유 같은 오류 자리가 없다.
 */
@Service
class appointment_editor(
    private val appointments: i_appointment_repository,
    private val schedules: i_schedule_repository,
    private val slots: i_slot_repository,
) {

    @Transactional(readOnly = true)
    fun load(scope: tenant_scope, appointment_id: String): appointment =
        appointments.find_by_id(scope, appointment_id) ?: throw appointment_missing_exception(appointment_id)

    /** 점유 대상 슬롯 — 보통 조회. 없으면 404. */
    fun load_slot(scope: tenant_scope, slot_id: String): slot =
        slots.find_all_by_ids(scope, listOf(slot_id)).firstOrNull() ?: throw slot_not_found_exception(slot_id)
```

### 3.7 도메인 서비스(애그리게잇 둘을 함께 전이) — `$B/domain/services/slot_occupation_service.kt:17-74`

Spring 의존 없는 순수 클래스. `@Bean` 등록은 `infrastructure/config/appointment_configs.kt:54-55`.

```kotlin
/**
 * `appointment` 전이와 `slot.occupy/release` 를 **한 트랜잭션**으로 묶는다.
 * (`patient-appointment.classdiagram.md` §3.2 · 불변식 20)
 *
 * **애그리게잇 둘을 함께 건드리는 유일한 곳**이다. 다른 곳에서 `slot.occupy()` 를 부르면
 * 예약 상태와 점유 수가 어긋난 채 커밋될 수 있다.
 *
 * Spring 의존이 없는 도메인 서비스다 — 트랜잭션 경계는 호출하는 애플리케이션 핸들러가 잡고(`@Transactional`),
 * 여기서는 **판정과 전이**만 한다. 행 락은 잡지 않는다 — 동시 점유는 `slot.version` 낙관적 잠금이 409 로 돌린다.
 */
class slot_occupation_service {

    /**
     * 예약 가능여부 검증 — 통과하지 못하면 예외다.
     * 판정의 본체는 `slot.is_bookable()` 한 곳이고(P38 — 자리 · 시각 · 대상 4축 · **역할**),
     * 여기서는 그 밖의 **구분·화이트리스트**를 본다.
     * [role] 은 호출자의 `booking_role`(`X-Role-Code` 파생) — `slot.allowed_roles` 에 없으면 `422 ROLE_NOT_ALLOWED`.
     */
    fun guard_bookable(
        target: appointment,
        slot_row: slot,
        owning_schedule: schedule,
        role: booking_role?,
        at: Instant,
    ) {
        if (target.category != owning_schedule.category) {
            throw service_category_mismatch_domain_exception(target.category.code, owning_schedule.category.code)
        }
        if (!owning_schedule.allows_service_item(target.order.code)) {
            throw service_item_not_allowed_exception(slot_row.slot_id, target.order.code)
        }
        slot_row.unbookable_reason(target.eligibility_of(), role, at)?.let { code ->
            throw slot_not_bookable_domain_exception(code, slot_row.slot_id, message_of(code))
        }
    }

    /** `proposed → booked` 한 번에 + 슬롯 점유 — 단건 · 묶음 · 회차가 모두 이 경로다. */
    fun book(
        target: appointment,
        slot_row: slot,
        owning_schedule: schedule,
        caller_channel: appointment_booking_channel,
        role: booking_role?,
        at: Instant,
    ) {
        guard_bookable(target, slot_row, owning_schedule, role, at)
        slot_row.occupy(at)
        target.book(
            schedule_id = owning_schedule.schedule_id,
            actor = snapshot_of(owning_schedule),
            slot_id = slot_row.slot_id,
            start_at = slot_row.start_at,
            end_at = slot_row.end_at,
            channel = caller_channel,
            at = at,
        )
    }
```

### 3.8 Aggregate Root 메서드 ① `slot` 판정 · 점유 — `$B/domain/aggregatesmodel/schedule/slot.kt:185-221`

```kotlin
    fun is_bookable(patient: eligibility, role: booking_role?, at: Instant): Boolean =
        !overbook &&
            status == slot_status.FREE &&
            booked_count < capacity &&
            target.allows(patient) &&
            allows_role(role) &&
            start_at.isAfter(at)

    /** 호출자 역할이 이 칸을 잡을 수 있는가 — 빈 집합은 전체 허용, 역할 미상도 통과(불변식 16). */
    fun allows_role(role: booking_role?): Boolean =
        allowed_role_codes.isBlank() || role == null || role in allowed_roles

    /** 왜 못 잡는가 — 오류 코드로 내려 화면이 사유를 말하게 한다. `null` 이면 예약 가능. */
    fun unbookable_reason(patient: eligibility, role: booking_role?, at: Instant): String? = when {
        overbook -> "SLOT_OVERBOOK"
        status.is_closed() -> "SLOT_CLOSED"
        !start_at.isAfter(at) -> "SLOT_IN_PAST"
        booked_count >= capacity -> "SLOT_FULL"
        !target.allows(patient) -> "SLOT_NOT_ELIGIBLE"
        !allows_role(role) -> "ROLE_NOT_ALLOWED"
        status != slot_status.FREE -> "SLOT_FULL"
        else -> null
    }

    /** 남은 여유 정원. */
    fun remaining(): Int = (capacity - booked_count).coerceAtLeast(0)

    /** 예약이 걸려 있는지 — `activate` · 휴무 등록이 「지워도 되는 칸」을 가르는 기준. */
    fun is_occupied(): Boolean = booked_count > 0

    /** 점유. 여유가 없으면 예외 — 호출자는 `slot_occupation_service` 하나다. */
    fun occupy(at: Instant) {
        if (status.is_closed()) throw bad_state_domain_exception("닫힌 슬롯은 예약할 수 없습니다", status.code)
        if (booked_count >= capacity) throw slot_full_domain_exception(id, capacity)
        booked_count += 1
        refresh_status(at, changed = true)
    }
```

### 3.9 Aggregate Root 메서드 ② `appointment.book()` — 도메인 이벤트 적재 — `$B/domain/aggregatesmodel/appointment/appointment.kt:286-322`

```kotlin
     * `proposed → booked` **한 번에** — 슬롯 자리와 시각을 채우고 등록한다.
     * `slot.is_bookable()` 판정과 `slot.occupy()` 는 호출자(`slot_occupation_service`)가 이미 끝냈다.
     * 단건 · 묶음 · 회차가 모두 이 경로다 — 임시 점유(pending) 단계는 없다.
     */
    fun book(
        schedule_id: String,
        actor: actor_snapshot,
        slot_id: String,
        start_at: Instant,
        end_at: Instant,
        channel: appointment_booking_channel,
        at: Instant,
        overbooked: Boolean = false,
    ) {
        if (status != appointment_status.PROPOSED) {
            throw bad_state_domain_exception("예약 등록은 proposed 예약만 가능합니다", status.code)
        }
        assign_slot(schedule_id, actor, slot_id, start_at, end_at)
        this.overbooked = overbooked
        this.channel = channel
        this.status = appointment_status.BOOKED
        this.booked_at = at
        touch(at)
        add_domain_event(
            appointment_booked_domain_event(
                appointment_id = id,
                patient_id = patient_id.toString(),
                based_on_type = based_on_type_value?.code,
                based_on_id = based_on_id,
                schedule_id = schedule_id,
                slot_ids = occupied_slot_ids(),
                start_at = start_at,
                occurred_at = at,
                scope = tenant(),
            ),
        )
    }
```

도메인 이벤트 정의 — `$B/domain/events/appointment_domain_events.kt:8-16`, `:74-85`:

```kotlin
/**
 * 통합예약 BC 도메인 이벤트.
 *
 * 애그리게잇이 적재하고 Repository 어댑터가 꺼내 `domain_event_dispatcher` 로 넘긴다.
 * 통합 이벤트(Kafka) 발행은 `@TransactionalEventListener(AFTER_COMMIT)` 가 받아서 한다 —
 * 커밋 전에 내보내면 롤백된 예약이 밖에 알려진다.
 *
 * 계약은 `patient-appointment.api.md` §9 와 1:1 이다.
 */
```

```kotlin
data class appointment_booked_domain_event(
    val appointment_id: String,
    val patient_id: String,
    val based_on_type: String?,
    val based_on_id: String?,
    val schedule_id: String?,
    val slot_ids: List<String>,
    val start_at: Instant?,
    val occurred_at: Instant,
    /** 예약의 테넌트 범위 — 커밋 뒤 발행기가 이 범위로 예약을 다시 읽는다. */
    val scope: tenant_scope = tenant_scope.INVALID,
) : domain_event
```

### 3.10 seedwork 베이스 클래스

`$B/domain/seedwork/seedwork.kt:1-17` — Aggregate Root 마커 · Repository IF · domain_event 마커:

```kotlin
package com.projectx.patientappointment.domain.seedwork

/** Aggregate Root 마커 인터페이스. Aggregate Root 만 구현한다. */
interface i_aggregate_root

/**
 * Aggregate Root 단위 영속성 추상화.
 * 구현체는 infrastructure 레이어에 위치하며 JPA 어댑터로 구현한다.
 * **읽기는 전부 테넌트 범위 안에서다** — id 로 읽어도 범위가 다르면 없는 것으로 본다(`null`).
 */
interface i_repository<T : i_aggregate_root> {
    fun save(aggregate: T): T
    fun find_by_id(scope: tenant_scope, id: String): T?
}

/** 도메인 이벤트 마커 인터페이스. data class 이벤트가 구현한다. */
interface domain_event
```

`$B/domain/seedwork/entity.kt:24-99` — 모든 Entity 베이스(문자열 PK · 테넌트 2컬럼 · 감사 4종 · `@Transient` 도메인 이벤트 목록):

```kotlin
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
    fun is_transient(): Boolean = id.isBlank()

    /** 이 행의 테넌트 범위. */
    fun tenant(): tenant_scope = tenant_scope.of(tenant_id, hospital_code)

    /** 이 행이 그 범위에 속하는가 — 리포지토리 어댑터가 id 로 읽은 뒤 범위를 확인할 때 쓴다. */
    fun belongs_to(scope: tenant_scope): Boolean =
        tenant_id == scope.tenant_id && hospital_code == scope.hospital_code

    /** 식별자 부여 — 팩토리(companion) 에서만 사용한다. */
    protected fun assign_id(new_id: String) {
        id = new_id
    }

    /** 테넌트 범위 지정 — 생성 때 한 번만 부른다(루트 팩토리). */
    protected fun assign_tenant(scope: tenant_scope) {
        this.tenant_id = scope.tenant_id
        this.hospital_code = scope.hospital_code
    }

    /** 자식 Entity 는 루트(소유자)의 테넌트 범위를 그대로 받는다. */
    protected fun inherit_tenant(owner: entity) {
        this.tenant_id = owner.tenant_id
        this.hospital_code = owner.hospital_code
    }
```

`$B/domain/seedwork/entity.kt:101-128` — 감사 기록(리스너 전용) · 동등성:

```kotlin
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
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        other as entity
        // 둘 다 영속 상태이고 id 가 같을 때만 동등.
        if (is_transient() || other.is_transient()) return false
        return id == other.id
    }

    override fun hashCode(): Int =
        if (is_transient()) super.hashCode() else id.hashCode()
}
```

`$B/domain/seedwork/value_object.kt:1-20`:

```kotlin
package com.projectx.patientappointment.domain.seedwork

/**
 * Value Object 베이스.
 * 동일 클래스 + 동등성 구성요소가 모두 같으면 동등하다.
 */
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

`$B/domain/seedwork/tenant_scope.kt:1-27` — (테넌트, 병원) 쌍 VO:

```kotlin
package com.projectx.patientappointment.domain.seedwork

// 테넌트 범위 value_object — (테넌트, 병원) 한 쌍. 모든 Entity 가 생성 때 받고, 모든 조회가 이 범위 안에서만 본다.
// - HTTP 요청은 토큰(AuthPrincipal)이, 수신 이벤트는 event_tenant_resolver 가, 주체 없는 경로(기동 시 시드 · 배치)는 배포값이 만든다.
// - 값을 특정할 수 없으면 INVALID_DATA 로 채운다 — 빈 값이 저장돼 조회 범위가 흐려지지 않게, 나중에 찾아낼 수 있게.
// - 빈 값 → INVALID_DATA 변환은 of() 한 곳이다. 생성자를 직접 부르지 않는다.
data class tenant_scope private constructor(
    val tenant_id: String,
    val hospital_code: String,
) {
    /** 외부 원천 조회에 실을 병원 코드 — 특정하지 못했으면 null(호출자가 설정 기본값으로 물러선다). */
    fun hospital_code_or_null(): String? = hospital_code.takeIf { it != INVALID_DATA }

    companion object {
        // 테넌트 · 병원을 특정하지 못했을 때 넣는 값
        const val INVALID_DATA = "INVALID_DATA"

        // 빈 값(null · 공백)은 INVALID_DATA 로 바꾼다. 앞뒤 공백은 떼고 대소문자는 받은 그대로 둔다
        fun of(tenant_id: String?, hospital_code: String?): tenant_scope =
            tenant_scope(normalize(tenant_id), normalize(hospital_code))

        // 둘 다 특정하지 못한 범위
        val INVALID: tenant_scope = tenant_scope(INVALID_DATA, INVALID_DATA)

        private fun normalize(value: String?): String = value?.trim()?.takeIf { it.isNotEmpty() } ?: INVALID_DATA
    }
}
```

### 3.11 UUID v7 id 발급 — `$B/domain/seedwork/uuid7.kt:1-29`

외부 라이브러리 없이 도메인 안에서 발급. 예약 팩토리가 `assign_id(uuid7.next(at))` 로 쓴다(`appointment.kt:755-756`).

```kotlin
package com.projectx.patientappointment.domain.seedwork

import java.security.SecureRandom
import java.time.Instant
import java.util.UUID

/**
 * 이 BC 가 발급하는 id — **UUID v7**(RFC 9562 · 48bit 밀리초 시각 + 버전 7 + 74bit 난수 · 36자 문자열).
 *
 * 다른 BC 로 나가는 이벤트(접수 요청 · 예약 이벤트 · 자동 예약 결과 회신)에 `slot_id` · `schedule_id` · `appointment_id` 같은 이 BC 의 id 가 실린다 —
 * 받는 쪽이 uuid 로 다루도록 모든 Entity PK 를 UUID 로 발급한다(classdiagram 1.26). 시각이 앞자리라 ULID 처럼 발급 순으로 정렬되고 인덱스 지역성이 좋다.
 * 외부 라이브러리 없이 도메인 안에서 발급한다. 그 전에 만든 행은 ULID(26자) 그대로다 — 컬럼은 VARCHAR(36) 이고 둘이 섞여 있다.
 */
object uuid7 {

    private val random = SecureRandom()

    fun next(): String = next(Instant.now())

    /** [at] 의 밀리초를 시각 자리에 싣는다 — 슬롯처럼 미래 시각을 주면 그 시각 순으로 정렬된다. */
    fun next(at: Instant): String {
        val millis = at.toEpochMilli() and 0xFFFF_FFFF_FFFFL
        val rand_a = random.nextInt(1 shl 12).toLong()
        val rand_b = random.nextLong() and 0x3FFF_FFFF_FFFF_FFFFL
        val most = (millis shl 16) or (0x7L shl 12) or rand_a
        val least = rand_b or Long.MIN_VALUE // variant 10
        return UUID(most, least).toString()
    }
}
```

`$B/domain/seedwork/uuid_format.kt:1-13` — shared uuid 꼴 판정:

```kotlin
package com.projectx.patientappointment.domain.seedwork

import java.util.UUID

// shared 스키마의 id(환자 · 수진 · 직원 · 진료과)는 uuid 다 — 문자열이 그 꼴인지 한 곳에서 판정한다.
// 하이픈 포함 36자 표준 형식만 받는다 — UUID.fromString 은 "1-1-1-1-1" 같은 값도 받아 준다.
private val UUID_FORMAT = Regex("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")

/** 표준 36자 UUID 꼴인가 — 앞뒤 공백은 무시한다. */
fun is_uuid(value: String?): Boolean = value?.trim()?.let { UUID_FORMAT.matches(it) } ?: false

/** UUID 꼴이면 UUID, 아니면 null — 예약 주체 id 는 의사(uuid) · 방 · 치료사 코드가 섞여 있다. */
fun uuid_or_null(value: String?): UUID? = value?.trim()?.takeIf { UUID_FORMAT.matches(it) }?.let(UUID::fromString)
```

### 3.12 Repository IF(domain) — `$B/domain/aggregatesmodel/appointment_repositories.kt:19-31`, `:116-134`

```kotlin
/**
 * 도메인 Repository 포트. 구현(JPA 어댑터)은 infrastructure 레이어에 둔다.
 * 쓰기(Command) 경로만 통과하며, 조회(Query)는 QueryDSL projection 이 별도로 담당한다.
 *
 * ⚠ `schedule` 과 `slot` 은 **같은 애그리게잇인데 리포지토리가 둘**이다(P37) —
 * 슬롯은 수천 건이라 루트로 로딩하면 점유마다 충돌한다.
 *
 * **모든 조회는 테넌트 범위(`tenant_scope`) 안에서다** — 같은 자연키(actor_id · 처방 id …)라도 범위가 다르면 다른 행이다.
 *
 * ⚠ **행 락(`SELECT … FOR UPDATE`)은 없다.** 동시 변경은 `@Version` 낙관적 잠금 하나로 막는다 —
 * 화면이 열었을 때 받은 버전과 저장할 때의 버전이 다르면 409 로 돌려 다시 읽게 한다.
 */
```

```kotlin
interface i_appointment_repository : i_repository<appointment> {

    fun save_all(aggregates: List<appointment>): List<appointment>

    fun find_all_by_ids(scope: tenant_scope, appointment_ids: Collection<String>): List<appointment>

    fun find_by_slot_ids(scope: tenant_scope, slot_ids: Collection<String>): List<appointment>

    fun find_by_series_group(scope: tenant_scope, series_group_id: String): List<appointment>

    fun find_by_encounter(scope: tenant_scope, encounter_id: java.util.UUID): List<appointment>

    /** 같은 처방으로 이미 선 예약 — 링크 진입이 `proposed` 를 중복 생성하지 않게 한다. */
    fun find_by_based_on(scope: tenant_scope, based_on_type: String, based_on_id: String): List<appointment>

    /** 슬롯이 닫힐 때 「예약이 걸려 있다」를 보고하기 위한 조회. */
    fun find_active_by_slot_ids(scope: tenant_scope, slot_ids: Collection<String>, statuses: Collection<appointment_status>): List<appointment>
}
```

### 3.13 JPA 어댑터(infrastructure) — `$B/infrastructure/repositories/appointment_repositories.kt:44-83`, `:232-244`

핵심: **저장 전에 인자 인스턴스에서 이벤트를 먼저 꺼낸다**(merge 가 다른 인스턴스를 돌려주면 `@Transient` 이벤트가 유실), 단건은 `saveAndFlush`(ETag 용 `version` 을 즉시 올린다).

```kotlin
/**
 * 도메인 Repository 포트의 JPA 어댑터.
 *
 * ⚠ 신규 애그리게잇은 `@Version` 때문에 Spring Data 가 isNew=false 로 판정해 **merge** 로 저장하고
 *   **다른 인스턴스**를 돌려준다. 반환본에서 `@Transient` 도메인 이벤트를 읽으면 유실되므로
 *   **인자 인스턴스에서 먼저 꺼낸다** — 아래 [save_with_events] 가 그 규칙을 한 곳에 모은다.
 *
 * **읽기는 전부 테넌트 범위 안에서다** — JPQL 이 `tenant_id` · `hospital_code` 로 거르고, id 하나로 읽는 `findById` 는
 *   읽은 뒤 [in_scope] 로 확인한다(범위가 다르면 없는 것으로 본다).
 *
 * ⚠ 단건 저장은 `saveAndFlush` 다 — `save` 만 하면 `@Version` 이 **커밋 시점에** 올라가서
 *   핸들러가 읽는 `saved.version` 이 한 박자 뒤진 값이 된다. 그 값을 ETag 로 돌려주면 화면이
 *   그대로 다음 `If-Match` 에 실어 보내 **매번 409** 가 난다.
 */
/** id 로 읽은 행이 그 범위에 속할 때만 돌려준다. */
private fun <T : entity> T?.in_scope(scope: tenant_scope): T? = this?.takeIf { it.belongs_to(scope) }

private inline fun <T : entity> save_with_events(
    aggregate: T,
    dispatcher: domain_event_dispatcher,
    persist: (T) -> T,
): T {
    val events: List<domain_event> = aggregate.domain_events
    val saved = persist(aggregate)
    aggregate.clear_domain_events()
    events.forEach { dispatcher.publish(it) }
    return saved
}

private inline fun <T : entity> save_all_with_events(
    aggregates: List<T>,
    dispatcher: domain_event_dispatcher,
    persist: (List<T>) -> List<T>,
): List<T> {
    if (aggregates.isEmpty()) return emptyList()
    val events: List<domain_event> = aggregates.flatMap { it.domain_events }
    val saved = persist(aggregates)
    aggregates.forEach { it.clear_domain_events() }
    events.forEach { dispatcher.publish(it) }
    return saved
}
```

```kotlin
@Repository
class appointment_repository_adapter(
    private val jpa: appointment_jpa_repository,
    private val dispatcher: domain_event_dispatcher,
) : i_appointment_repository {

    override fun save(aggregate: appointment): appointment =
        save_with_events(aggregate, dispatcher) { jpa.saveAndFlush(it) }

    override fun save_all(aggregates: List<appointment>): List<appointment> =
        save_all_with_events(aggregates, dispatcher) { jpa.saveAllAndFlush(it) }

    override fun find_by_id(scope: tenant_scope, id: String): appointment? = jpa.findById(id).orElse(null).in_scope(scope)
```

Spring Data `JpaRepository` 인터페이스 모음의 규약 — `$B/infrastructure/appointment_context.kt:23-31`:

```kotlin
/**
 * Write 경로의 Spring Data JpaRepository 인터페이스 모음.
 *
 * (메서드 파라미터는 Spring Data 규약이라 camelCase 를 쓴다 — framework 예외.
 *  프로퍼티명에 underscore 가 있어 파생 쿼리 대신 JPQL 로 명시한다.)
 *
 * **모든 조회는 테넌트 범위(tenant_id · hospital_code) 안에서 한다** — 파라미터 `tenantId` · `hospitalCode` 는 어댑터가
 * `tenant_scope` 에서 꺼내 넘긴다. id 하나로 읽는 `findById` 는 어댑터가 읽은 뒤 범위를 확인한다(`entity.belongs_to`).
 */
```

### 3.14 domain_event_dispatcher — `$B/infrastructure/domain_event_dispatcher.kt:1-19`

```kotlin
package com.projectx.patientappointment.infrastructure

import com.projectx.patientappointment.domain.seedwork.domain_event
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component

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

### 3.15 `@TransactionalEventListener(AFTER_COMMIT)` 통합 이벤트 발행 — `$B/infrastructure/eventbus/domain_event_integration_publisher.kt:34-89`

```kotlin
/**
 * 도메인 이벤트 → 통합 이벤트 전환·발행기.
 *
 * - **트랜잭션 커밋 후(AFTER_COMMIT)** 수신해 페이로드를 조립한다 — 커밋 전에 내보내면
 *   롤백된 예약이 밖에 알려진다.
 * - **예약 등록 · 이동 · 취소는 실제로 발행한다**(`Appointment` · classdiagram 1.23) — 커밋 뒤 예약을 다시 읽어 payload 를 채우고
 *   `projectx.patient-appointment.event-bus.enabled = true` 일 때만 `EventBus.publish` 한다(꺼져 있으면 로그만).
 * - **접수 요청도 실제로 발행한다**(`EncounterRequest` · classdiagram 1.28) — 환자마다 1건(묶음마다의 요청은 배열 항목), 요청 값은 도메인 이벤트에 다 실려 있어 다시 읽지 않는다.
 * - **자동 예약 생성 결과 회신도 실제로 발행한다**(`OrderBasedAppointmentBooked` · classdiagram 1.22) — 처방마다 1건, 커밋 뒤 그 처방의 예약 전부를 다시 읽는다.
 *   그 밖의 이벤트(슬롯 · 휴무 · 재배정 · 창구 접수 부착)는 아직 조립한 페이로드를 로그로만 남긴다.
 * - 봉투의 `userId` 는 로그인한 사람이다(토큰 · 받은 봉투) — 발행 지점을 `event_actor.publish` 로 감싸 없으면 WARN 으로 드러낸다(대체값 없음 · classdiagram 1.26).
 * - 발행 · 조립 실패가 **커밋된 쓰기를 되돌리지 않도록** 예외는 로그만 남긴다(best-effort · outbox 없음 · 다시 보내지 않는다).
 */
@Component
class domain_event_integration_publisher(
    private val appointment_queries: i_appointment_queries,
    private val event_bus: ObjectProvider<EventBus>,
    @Value("\${projectx.patient-appointment.event-bus.enabled:true}") private val enabled: Boolean,
) {

    private val log = LoggerFactory.getLogger(domain_event_integration_publisher::class.java)

    // ----- appointment — 등록 · 이동 · 취소는 `Appointment` 한 타입 · 한 토픽(classdiagram 1.23) -----

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun on_booked(event: appointment_booked_domain_event) =
        publish_appointment(appointment_event_type.BOOKED, event.scope, event.appointment_id)

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun on_rescheduled(event: appointment_rescheduled_domain_event) =
        publish_appointment(appointment_event_type.RESCHEDULED, event.scope, event.appointment_id)

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun on_cancelled(event: appointment_cancelled_domain_event) =
        publish_appointment(appointment_event_type.CANCELLED, event.scope, event.appointment_id)

    /**
     * 예약 1건 발행 — 커밋 뒤 예약을 다시 읽어(같은 트랜잭션에서 붙인 접수까지) payload 를 만든다.
     * 꺼져 있으면 로그만 남긴다. 실패는 WARN 만 — 커밋된 예약을 되돌리지 않는다.
     */
    private fun publish_appointment(type: appointment_event_type, scope: tenant_scope, appointment_id: String) {
        runCatching {
            val view = appointment_queries.find_by_id(scope, appointment_id)
                ?: return log.warn("발행할 예약을 찾지 못했습니다 — appointment_id={} event_type={}", appointment_id, type)
            val payload = appointment_integration_event.of(type, view)
            if (!enabled) {
                log.info("예약 이벤트 조립 완료(발행 꺼짐) — {}", payload)
                return
            }
            event_actor.publish("Appointment") { event_bus.getObject().publish(payload, aggregateId = payload.appointment_id) }
            log.info("예약 이벤트를 발행했습니다 — {}", payload)
        }.onFailure {
            log.warn("예약 이벤트 발행에 실패했습니다(예약은 저장됨 · 다시 보내지 않는다) — appointment_id={} event_type={} 사유={}", appointment_id, type, it.message)
        }
    }
```

발행 봉투의 `userId` 보호막 — `$B/infrastructure/eventbus/event_actor.kt:8-29`:

```kotlin
/**
 * 발행 봉투의 `userId` — 이 BC 가 내보내는 메시지의 행위자는 **로그인한 사람의 id** 다(classdiagram 1.26).
 *
 * eventbus 는 발행 순간의 MDC `userId` 를 봉투에 싣는다. 그 값은 HTTP 요청이면 토큰의 사용자(`@RequireAuth` 라 늘 있다),
 * 받은 메시지의 처리면 받은 봉투의 `userId`(그 메시지를 낳은 로그인 사용자 — `inbox_dispatcher` 가 넣고 커밋 뒤까지 둔다)다.
 * **대체값(시스템 id 등)을 만들지 않는다** — 받은 봉투에 로그인 id 가 없으면 보낸 쪽의 계약 위반이라 WARN 으로 드러낸다.
 */
object event_actor {

    private val log = LoggerFactory.getLogger(event_actor::class.java)

    /** 지금 스레드의 로그인 사용자 id — uuid 가 아니면(없음 · `anonymous`) null. */
    fun current(): String? = MDC.get(JwtKeys.USER_ID)?.trim()?.takeIf { uuid_or_null(it) != null }

    /** 발행 지점을 감싼다 — 로그인 id 가 없으면 WARN 을 남기고(값은 바꾸지 않는다) 그대로 발행한다. */
    fun <T> publish(message_type: String, block: () -> T): T {
        if (current() == null) {
            log.warn("발행 봉투에 로그인 사용자 id 가 없습니다 — messageType={} userId={} (받은 메시지의 봉투 userId 를 보낸 쪽이 채워야 한다)", message_type, MDC.get(JwtKeys.USER_ID))
        }
        return block()
    }
}
```

### 3.16 Kafka 발행 payload 클래스 — `$B/application/integrationevents/events/appointment_integration_event.kt:12-54`, `:60-97`

```kotlin
/** 예약 이벤트의 행위 — 같은 토픽에 함께 흐르고 payload 의 `event_type` 이 가른다(classdiagram 1.23). */
enum class appointment_event_type { BOOKED, RESCHEDULED, CANCELLED }

/**
 * 예약 이벤트 발행 — 등록 · 이동 · 취소를 커밋 뒤 예약 1건마다 알린다. (`patient-appointment.api.md` §9-d · classdiagram 1.23)
 *
 * 토픽 `{환경}.{병원코드}.appointment` 하나에 셋이 함께 흐르고 [event_type] 이 가른다.
 * **payload 필드는 snake_case** 다 — 프로퍼티명이 그대로 JSON 키다.
 * 접수(진료)의 진료과 · 진료의는 처방이 있으면 처방 진료, 처방 없는 외래는 잡은 칸의 진료의다. 모르는 값은 `null` 이다.
 * 환자 이름 · 환자번호 · 진료과/진료의 이름은 예약 스냅샷 값이다(예약을 만든 요청이 넘겼다).
 */
@MessageType("Appointment")
@SchemaVersion("1")
@Aggregate(type = "Appointment")
class appointment_integration_event(
    /** `BOOKED` · `RESCHEDULED` · `CANCELLED`. */
    val event_type: String,
    /** 파티션 키. */
    val appointment_id: String,
    val status: String,
    val service_category: String,
    /** 처방이 있으면 `based_on_type`, 처방 없는 외래는 `patient`. */
    val base_type: String,
    /** 처방이 있으면 `based_on_id`(과내 검사 = process id · 처방 수신 = 처방 id), 처방 없는 외래는 `patient_id`. */
    val base_id: String,
    val patient_id: String,
    val patient_name: String?,
    val patient_number: String?,
    val encounter_id: String?,
    /** 예약일 `YYYY-MM-DD`. */
    val encounter_date: String?,
    /** 예약 시작 — 오프셋 포함 ISO-8601(병원 시간대 · `YYYY-MM-DDTHH:MM:SS+09:00`). */
    val encounter_datetime: String?,
    val encounter_department_id: String?,
    val encounter_department_name: String?,
    val encounter_department_code: String?,
    val encounter_physician_id: String?,
    val encounter_physician_name: String?,
    /** 진료의 직원번호(사번) — 값의 원천이 없어 늘 `null` 이다(필드는 계약이라 남긴다). */
    val encounter_physician_number: String?,
    /** 취소 사유 코드 — 취소가 아니면 `null`. */
    val cancellation_reason: String?,
) : integration_event() {
```

```kotlin
    companion object {
        /** 토픽 `{환경}.{병원코드}.appointment` 의 namespace 뒤 마디(이벤트명). */
        const val TOPIC = "appointment"

        /** [appointment_id] 의 payload 필드명 — `topic_registry` 의 partitionKeyField. */
        const val PARTITION_KEY_FIELD = "appointment_id"

        /** 커밋 뒤 다시 읽은 예약 → payload. 근거가 처방이면 처방 진료, 외래 근거면 잡은 칸의 진료의다. */
        fun of(event_type: appointment_event_type, view: appointment_view_model): appointment_integration_event {
            val order = view.order_snapshot
            val actor = view.actor_snapshot
            val basis = view.based_on_type ?: based_on_type.OUTPATIENT_CLINIC.code
            val has_order = based_on_type.from_code(basis).is_order() && view.based_on_id != null
            return appointment_integration_event(
                event_type = event_type.name,
                appointment_id = view.appointment_id,
                status = view.status,
                service_category = view.service_category,
                // 예약 근거 그대로 — 외래는 outpatient-clinic · 환자 id(옛 행은 근거가 비어 있어도 같은 값으로 낸다).
                base_type = basis,
                base_id = view.based_on_id ?: view.patient_id,
                patient_id = view.patient_id,
                // 환자 이름 · 번호는 shared.patient 값이다(응답 patient · 부록 P124).
                patient_name = view.patient.name,
                patient_number = view.patient.mrn,
                encounter_id = view.encounter_id,
                encounter_date = view.visit_date?.toString(),
                encounter_datetime = view.start_at?.atZone(clinic_zone)?.toOffsetDateTime()?.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
                encounter_department_id = if (has_order) order.department_id else actor.department_id,
                encounter_department_name = if (has_order) order.department_name else actor.department_name,
                encounter_department_code = if (has_order) order.department_code else null,
                encounter_physician_id = if (has_order) order.requester_id else actor.actor_id,
                encounter_physician_name = if (has_order) order.requester_name else actor.actor_name,
                encounter_physician_number = null,
                cancellation_reason = view.cancellation_reason,
            )
        }
    }
}
```

⚠ 이 payload 는 **snake_case** 다(루트 CLAUDE.md 의 「payload 는 camelCase」 규칙과 다름). 베이스 `integration_event.kt:3-9` 가 그 사정을 적어 둔다:

```kotlin
/**
 * 통합(integration) 이벤트 베이스. 이벤트는 평범한 payload 클래스다.
 * 구독하는 것은 ERP 직원 일정 · 자동 예약 생성 요청 · 원무 수진 등록 회신이다(설정으로 켤 때만 · classdiagram 1.17 · 1.22 · 1.28) —
 * 발행은 예약 이벤트 · 접수 요청이고, 나머지는 발행/수신 배선 없이 payload 정의만 유지한다.
 * 필드는 **snake_case** 다(자동 예약 생성 요청 · 접수 요청 · 회신 · 예약 이벤트). 먼저 정한 camelCase 계약(ERP 직원 일정 · 옛 payload)은 상대와 함께 바꿀 때까지 그대로 둔다.
 */
abstract class integration_event
```

### 3.17 감사 컬럼 채우기(같은 트랜잭션의 flush 시점) — `$B/infrastructure/persistence/audit_entity_listener.kt:25-39` + `$R/META-INF/orm.xml:12-18`

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

    /** MDC 의 사용자 id — 익명(이벤트 수신) 이거나 UUID 꼴이 아니면 null. */
    private fun current_employee_id(): UUID? = event_actor.current()?.let(UUID::fromString)
}
```

```xml
    <persistence-unit-metadata>
        <persistence-unit-defaults>
            <entity-listeners>
                <entity-listener class="com.projectx.patientappointment.infrastructure.persistence.audit_entity_listener"/>
            </entity-listeners>
        </persistence-unit-defaults>
    </persistence-unit-metadata>
```

---

## 4. 읽기 경로 샘플(Query)

조회는 **Controller → Query IF(application) → QueryDSL 구현(infrastructure) → view_model** 이고 Repository 를 거치지 않는다.
`JPAQueryFactory` 빈은 `$B/infrastructure/config/appointment_configs.kt:26-32`:

```kotlin
/** QueryDSL 배선 — 조회는 JPAQueryFactory 타입세이프 쿼리로 view_model 을 projection 한다. */
@Configuration
class querydsl_config {

    @Bean
    fun jpa_query_factory(entity_manager: EntityManager): JPAQueryFactory = JPAQueryFactory(entity_manager)
}
```

모든 QueryDSL 조회가 테넌트 범위를 거는 한 곳 — `$B/infrastructure/queries/tenant_predicates.kt:7-12`:

```kotlin
/**
 * 테넌트 범위 조건 — **모든 QueryDSL 조회가 이 한 곳으로 범위를 건다**(tenant_id · hospital_code).
 * shared 조인에는 걸지 않는다(id 만으로 잇는다) — 범위는 자기 테이블 쪽에 건다.
 */
internal fun in_scope(tenant_id: StringPath, hospital_code: StringPath, scope: tenant_scope): BooleanExpression =
    tenant_id.eq(scope.tenant_id).and(hospital_code.eq(scope.hospital_code))
```

### 4.1 ① 슬롯 검색 · 달력(`GET /slots` · `GET /slots/find`) — 동적 where + 메모리 판정

Controller — `$B/presentation/appointments_controller.kt:106-139`:

```kotlin
    @GetMapping("/slots")
    fun slots(
        @TokenPrincipal principal: AuthPrincipal,
        @RequestHeader(value = "X-Role-Code", required = false) role_code: String?,
        @RequestParam(required = false) role: String?,
        @RequestParam(required = false) schedule: String?,
        @RequestParam(required = false) actor: String?,
        @RequestParam(required = false) serviceCategory: String?,
        @RequestParam(required = false) from: String?,
        @RequestParam(required = false) to: String?,
        @RequestParam(required = false) status: String?,
        @RequestParam(required = false) ageGroup: String?,
        @RequestParam(required = false) encounterClass: String?,
        @RequestParam(required = false) visitType: String?,
        @RequestParam(required = false) bookable: Boolean?,
    ): items_view_model<slot_view_model> = items_view_model(
        slots.find(
            principal.to_tenant_scope(),
            slot_filter(
                schedule_ids = csv_param(schedule).toSet(),
                actor_ids = csv_param(actor).toSet(),
                categories = csv_param(serviceCategory).mapTo(mutableSetOf()) { service_category.from_code(it) },
                statuses = csv_param(status).mapTo(mutableSetOf()) { slot_status.from_code(it) },
                from = date_param_or_null("from", from),
                to = date_param_or_null("to", to),
                age_group = ageGroup,
                encounter_class = encounterClass,
                visit_type = visitType,
                role = booking_roles.resolve(role, role_code),
                bookable = bookable,
            ),
        ),
    )
```

Query IF + filter(application) — `$B/application/queries/appointment_queries.kt:51-91`:

```kotlin
data class slot_filter(
    val schedule_ids: Set<String> = emptySet(),
    val actor_ids: Set<String> = emptySet(),
    /** 진료과 · 부서(`slot.department_id`) — 가용 시간 조회(§5.1)가 진료과로 찾는다. */
    val department_ids: Set<String> = emptySet(),
    val categories: Set<service_category> = emptySet(),
    val statuses: Set<slot_status> = emptySet(),
    val from: LocalDate? = null,
    val to: LocalDate? = null,
    val age_group: String? = null,
    val encounter_class: String? = null,
    val visit_type: String? = null,
    /** 환자 직접예약 경로(`mobile` · `internet`) — 비면 원내 예약이라 경로 축을 통과한다. */
    val booking_route: String? = null,
    /** 호출자 역할(§5) — `null` 은 역할 미상이라 역할 축을 보지 않는다. */
    val role: booking_role? = null,
    /**
     * `true` 면 `slot.is_bookable()` 을 통과한 칸만.
     *
     * ⚠ **기본값은 null 이다** — 기본을 `free` 로 좁혀 두면 파라미터를 안 준 호출이 빈 목록을 받고
     * 원인을 찾기 어렵다(루트 CLAUDE.md: 조회 기본 필터를 몰래 넣지 않는다).
     */
    val bookable: Boolean? = null,
    val as_of: Instant? = null,
)

interface i_slot_queries {

    fun find(scope: tenant_scope, filter: slot_filter): List<slot_view_model>

    fun find_by_id(scope: tenant_scope, slot_id: String): slot_view_model?

    /**
     * 세션 한 칸 칸 → 시간대(`am` · `pm` · `night` · `day`) — 출처 규칙이 「세션 한 칸」이고 같은 패턴 · 일차의 같은 시간대(하루 단위면 그 날)에
     * 그 행 하나뿐인 칸만 싣는다(classdiagram 1.48 · 부록 P127). 예약 응답의 `whole_session` · `session_band` 를 묶음 한 번으로 채운다(행마다 묻지 않는다).
     */
    fun whole_session_bands(scope: tenant_scope, slot_ids: Collection<String>): Map<String, String>

    /** 달력 — 일자별 가용 요약. */
    fun find_days(scope: tenant_scope, filter: slot_filter): slot_find_view_model
}
```

QueryDSL 구현 — `$B/infrastructure/queries/slot_query_dsl.kt:32-67`(클래스 · `find` · `find_by_id`):

```kotlin
/**
 * 슬롯 조회 — **QueryDSL**.
 *
 * 3축 판정은 SQL 로 흉내내지 않는다 — `eligibility.allows()` 와 `slot.is_bookable()` 한 곳이
 * 판정의 정본이므로(P38) 후보를 좁힌 뒤 **메모리에서** 거른다. 날짜·스케줄로 이미 충분히 좁혀진다.
 */
@Repository
class slot_query_dsl(
    private val factory: JPAQueryFactory,
    private val clock: Clock,
) : i_slot_queries {

    private val q = Qslot.slot
    private val s = Qschedule.schedule
    private val d = Qpattern_detail.pattern_detail
    private val r = Qpattern_generation_rule.pattern_generation_rule

    @Transactional(readOnly = true)
    override fun find(scope: tenant_scope, filter: slot_filter): List<slot_view_model> {
        val rows = load(scope, filter)
        val names = actor_names(scope, rows)
        val bands = whole_slot_bands(scope, rows)
        return rows.map { view_mappers.to_view(it, names[it.schedule_id], bands[it.slot_id]) }
    }

    @Transactional(readOnly = true)
    override fun find_by_id(scope: tenant_scope, slot_id: String): slot_view_model? {
        val found = factory.selectFrom(q).where(in_scope(q.tenant_id, q.hospital_code, scope), q.id.eq(slot_id)).fetchFirst()
            ?: return null
        return view_mappers.to_view(
            found,
            actor_names(scope, listOf(found))[found.schedule_id],
            whole_slot_bands(scope, listOf(found))[found.slot_id],
        )
    }
```

동적 where(`BooleanBuilder`) — `$B/infrastructure/queries/slot_query_dsl.kt:107-134`:

```kotlin
    private fun load(scope: tenant_scope, filter: slot_filter): List<slot> {
        val at = filter.as_of ?: clock.instant()
        val where = BooleanBuilder(in_scope(q.tenant_id, q.hospital_code, scope))
        if (filter.schedule_ids.isNotEmpty()) where.and(q.schedule_id.`in`(filter.schedule_ids))
        if (filter.actor_ids.isNotEmpty()) where.and(q.actor_id.`in`(filter.actor_ids))
        if (filter.department_ids.isNotEmpty()) where.and(q.department_id.`in`(filter.department_ids))
        if (filter.categories.isNotEmpty()) where.and(q.category.`in`(filter.categories))
        if (filter.statuses.isNotEmpty()) where.and(q.status.`in`(filter.statuses))
        filter.from?.let { where.and(q.slot_date.goe(it)) }
        filter.to?.let { where.and(q.slot_date.loe(it)) }
        // `bookable=true` 는 SQL 로 크게 좁히고(자리 있음 · 미래) 3축만 메모리에서 본다.
        if (filter.bookable == true) {
            where.and(q.status.eq(slot_status.FREE))
            where.and(q.booked_count.lt(q.capacity))
            where.and(q.start_at.gt(at))
        }

        val rows = factory.selectFrom(q)
            .where(where)
            .orderBy(q.start_at.asc(), q.id.asc())
            .fetch()

        if (filter.bookable != true) return rows
        val patient = patient_of(filter)
        // 역할 축(불변식 16) — `slot.allowed_roles` 가 비어 있지 않고 그 역할이 없으면 예약 불가로 걸러진다.
        return rows.filter { it.is_bookable(patient, filter.role, at) }
    }
```

달력(일자별 집계) — `$B/infrastructure/queries/slot_query_dsl.kt:77-105`:

```kotlin
    @Transactional(readOnly = true)
    override fun find_days(scope: tenant_scope, filter: slot_filter): slot_find_view_model {
        val at = filter.as_of ?: clock.instant()
        val patient = patient_of(filter)
        // 달력은 전 상태를 봐야 한다 — 휴무로 닫힌 날도 「장비 점검」으로 그려야 하기 때문이다.
        val rows = load(scope, filter.copy(bookable = null, statuses = emptySet()))

        val days = rows.groupBy { it.slot_date }.toSortedMap().map { (date, day_rows) ->
            val bookable = day_rows.count { it.is_bookable(patient, filter.role, at) }
            val capacity = day_rows.sumOf { it.capacity }
            val booked = day_rows.sumOf { it.booked_count }
            val closed = day_rows.filter { it.status == slot_status.BUSY_UNAVAILABLE }
            slot_find_day_view_model(
                date = date,
                state = state_of(day_rows, bookable, capacity, booked).code,
                bookable_count = bookable,
                total_capacity = capacity,
                booked_count = booked,
                earliest_start = day_rows.filter { it.is_bookable(patient, filter.role, at) }.minOfOrNull { it.start_at },
                unavailability = closed.firstOrNull()?.let {
                    coding_view_model(
                        code = it.unavailable_by ?: "unavailable",
                        display = it.unavailable_reason ?: "휴무",
                    )
                },
            )
        }
        return slot_find_view_model(days)
    }
```

view_model — `$B/application/queries/schedule_view_models.kt:563-592`:

```kotlin
data class slot_view_model(
    val slot_id: String,
    val schedule_id: String,
    val actor_id: String,
    val actor_name: String?,
    val service_category: String,
    val department_id: String,
    val booking_channel: String,
    val slot_date: LocalDate,
    val start_at: Instant,
    val end_at: Instant,
    /** `am` · `pm` — 모든 구분이 같은 기준으로 시작 시각에서 판정한다(12:00 이전 = 오전). */
    val session: String,
    val status: String,
    val capacity: Int,
    val booked_count: Int,
    val remaining: Int,
    val eligibility: eligibility_view_model,
    /** `schedule.allowed_roles` 의 각인값 — 빈 목록 = 전체 허용. */
    val allowed_roles: List<String>,
    val source_detail_id: String?,
    val source_override_id: String?,
    val unavailable_reason: String?,
    val version: Long,
    /** 오버부킹 슬롯(그 날 00:00) — 예약 가능 칸이 아니다(classdiagram 1.22 · 불변식 45). */
    val overbook: Boolean = false,
    /** 세션 한 칸 칸 — 화면은 시각 대신 [session_band] 글자로 적는다(classdiagram 1.48). */
    val whole_session: Boolean = false,
    /** 세션 한 칸 칸의 시간대 — `am` 「오전」 · `pm` 「오후」 · `night` 「야간」 · `day` 「종일」(예약 기준의 세션 단위가 정한다 · classdiagram 1.48 · 부록 P127). 아니면 null. */
    val session_band: String? = null,
)
```

### 4.2 ② 예약 목록(`GET /appointments`) · 예약 환자 목록(`GET /appointment-patients`) — `shared.patient` 조인

Controller — `$B/presentation/appointments_controller.kt:220-263`:

```kotlin
    @GetMapping("/appointments")
    fun appointments(
        @TokenPrincipal principal: AuthPrincipal,
        @RequestParam(required = false) patient: String?,
        @RequestParam(required = false) schedule: String?,
        @RequestParam(required = false) encounter: String?,
        @RequestParam(required = false) basedOnType: String?,
        @RequestParam(required = false) basedOnId: String?,
        @RequestParam(required = false) seriesGroup: String?,
        @RequestParam(required = false) status: String?,
        @RequestParam(required = false) from: String?,
        @RequestParam(required = false) to: String?,
        // 예약 검색(classdiagram 1.20) — 분류 여럿 · 검색어(환자명 · 환자번호 · 예약 주체 · 진료과 · 처방명).
        @RequestParam(required = false) serviceCategory: String?,
        @RequestParam(required = false) q: String?,
        // 날짜 기준 — from · to 를 어느 날짜에 거는가(visit · last-updated · ordered · requested · 기본 visit).
        @RequestParam(required = false) dateBasis: String?,
        // 처방 항목 분류(`order_snapshot.item_type_code` — 약물군 CHEMOTHERAPY …) 여럿 · classdiagram 1.24.
        @RequestParam(required = false) itemType: String?,
        // 처방 항목 분류가 이 값들이 아니거나 비어 있는 예약 — FHIR token `:not` 수식어(항암 외 주사 · classdiagram 1.44).
        @RequestParam(name = "itemType:not", required = false) item_type_not: String?,
    ): items_view_model<appointment_view_model> = items_view_model(
        appointments.find(
            principal.to_tenant_scope(),
            appointment_filter(
                patient_id = patient,
                schedule_ids = csv_param(schedule).toSet(),
                encounter_id = encounter,
                based_on_type = basedOnType,
                based_on_id = basedOnId,
                series_group_id = seriesGroup,
                statuses = csv_param(status).mapTo(mutableSetOf()) { appointment_status.from_code(it) },
                from = date_param_or_null("from", from),
                to = date_param_or_null("to", to),
                categories = csv_param(serviceCategory).mapTo(mutableSetOf()) { service_category.from_code(it) },
                keyword = q?.trim()?.takeIf { it.isNotEmpty() },
                date_basis = dateBasis?.takeIf { it.isNotBlank() }?.let { appointment_date_basis.from_code(it) }
                    ?: appointment_date_basis.VISIT,
                item_types = csv_param(itemType).toSet(),
                excluded_item_types = csv_param(item_type_not).toSet(),
            ),
        ),
    )
```

Query IF — `$B/application/queries/appointment_queries.kt:125-150`(filter), `:165-182`(IF):

```kotlin
data class appointment_filter(
    val patient_id: String? = null,
    val schedule_ids: Set<String> = emptySet(),
    val encounter_id: String? = null,
    val based_on_type: String? = null,
    val based_on_id: String? = null,
    val series_group_id: String? = null,
    val statuses: Set<appointment_status> = emptySet(),
    /** [from] · [to] 를 거는 날짜 — 예약일(기본) · 예약수정일 · 처방일 · 희망일(classdiagram 1.20 · 부록 P85). */
    val date_basis: appointment_date_basis = appointment_date_basis.VISIT,
    val from: LocalDate? = null,
    val to: LocalDate? = null,
    /** 분류(`service_category`) — 비면 전부(classdiagram 1.20). */
    val categories: Set<service_category> = emptySet(),
    /**
     * 검색어 — 대소문자 무시 포함 검색. 환자명 · 환자번호(`shared.patient`) · 예약 주체 이름 · 예약 주체 진료과 ·
     * 처방 진료과명 · 처방명 · 처방 코드(예약 스냅샷) 중 하나라도 맞으면 나온다(classdiagram 1.20 · 부록 P84 · P124).
     */
    val keyword: String? = null,
    /** 처방 항목 분류(`order_snapshot.item_type_code` — 약물군) 여럿 — 비면 전부(classdiagram 1.24). */
    val item_types: Set<String> = emptySet(),
    /**
     * 처방 항목 분류가 **이 값들이 아니거나 비어 있는** 예약만 — FHIR token `:not`(classdiagram 1.44 · 항암 외 주사).
     * 약물군 없는 예약도 나온다. 비면 조건 없음. [item_types] 와 함께 주면 AND.
     */
    val excluded_item_types: Set<String> = emptySet(),
)
```

```kotlin
interface i_appointment_queries {

    fun find(scope: tenant_scope, filter: appointment_filter): List<appointment_view_model>

    fun find_by_id(scope: tenant_scope, appointment_id: String): appointment_view_model?

    /** 예약 이동 이력 — 최근 것부터(classdiagram §2.27). 없는 예약은 빈 목록. */
    fun find_changes(scope: tenant_scope, appointment_id: String): List<appointment_change_view_model>

    /** 회차 묶음 이력 — 그 묶음 회차들의 이동 이력 + 취소, 최근 것부터(classdiagram 1.44). 묶음이 없으면 빈 목록. */
    fun find_series_changes(scope: tenant_scope, series_group_id: String): List<series_change_view_model>

    /** 슬롯에 걸린 예약 — 휴무·각인이 「예약이 있다」를 보고할 때 쓴다. */
    fun find_occupying(scope: tenant_scope, slot_ids: Collection<String>): List<occupied_appointment_view_model>

    /** 예약 환자 목록 — 활성 예약(booked · fulfilled) 한 건이 한 줄(classdiagram 1.47). 예약일시 → 예약실 이름 → 환자명 순. */
    fun find_patient_list(scope: tenant_scope, filter: appointment_patient_filter): List<appointment_patient_view_model>
}
```

QueryDSL 구현 — `$B/infrastructure/queries/appointment_query_dsl.kt:35-98`(`find` · 환자 신원은 id 묶음으로 따로 읽는다):

```kotlin
/**
 * 예약 조회 — **QueryDSL**. 모든 조회는 테넌트 범위(`in_scope`) 안에서다.
 *
 * 예약은 phase 컬렉션을 함께 내보내므로 `Projections.constructor` 가 아니라 엔티티를 읽어
 * `view_mappers` 로 옮긴다(중첩 컬렉션 projection 은 런타임에만 터진다).
 *
 * **처방의 · 처방 진료과명은 스냅샷 컬럼이다**(예약을 만든 요청이 넘긴 값 · classdiagram 1.27).
 * **환자 신원은 `shared.patient`** 다(부록 P124) — 응답은 목록의 환자 id 묶음을 한 번에 읽어 붙이고([i_shared_directory_queries.patients]),
 * 검색어는 `patient_id in (shared.patient 서브쿼리)` 로 건다. 행 조인을 쓰지 않아 shared 쪽 id 가 겹쳐도 예약 줄이 늘지 않는다.
 */
@Repository
class appointment_query_dsl(
    private val factory: JPAQueryFactory,
    private val shared_directory: i_shared_directory_queries,
    private val slots: i_slot_queries,
    private val clock: Clock,
) : i_appointment_queries {

    private val q = Qappointment.appointment
    private val p = Qshared_patient.shared_patient

    @Transactional(readOnly = true)
    override fun find(scope: tenant_scope, filter: appointment_filter): List<appointment_view_model> {
        val where = BooleanBuilder(in_scope(q.tenant_id, q.hospital_code, scope))
        filter.patient_id?.let { id ->
            // 환자 id 는 shared 의 uuid 다 — 꼴이 아니면 그 환자의 예약은 있을 수 없다(빈 목록).
            val patient_id = uuid_or_null(id) ?: return emptyList()
            where.and(q.patient_id.eq(patient_id))
        }
        if (filter.schedule_ids.isNotEmpty()) where.and(q.schedule_id.`in`(filter.schedule_ids))
        filter.encounter_id?.let { id ->
            val encounter_id = uuid_or_null(id) ?: return emptyList()
            where.and(q.encounter_id.eq(encounter_id))
        }
        filter.based_on_type?.let { where.and(q.based_on_type_value.eq(based_on_type.from_code(it))) }
        filter.based_on_id?.let { where.and(q.based_on_id.eq(it)) }
        filter.series_group_id?.let { where.and(q.series_group_id.eq(it)) }
        if (filter.statuses.isNotEmpty()) where.and(q.status.`in`(filter.statuses))
        where.and(date_range(filter))
        if (filter.categories.isNotEmpty()) where.and(q.category.`in`(filter.categories))
        filter.keyword?.let { where.and(keyword_matches(it)) }
        if (filter.item_types.isNotEmpty()) where.and(q.order_value.item_type_code.`in`(filter.item_types))
        // FHIR `:not` — 값이 다르거나 **없는** 예약(약물군을 싣지 않은 처방의 주사도 「항암 외」에 든다 · classdiagram 1.44).
        if (filter.excluded_item_types.isNotEmpty()) {
            where.and(q.order_value.item_type_code.isNull.or(q.order_value.item_type_code.notIn(filter.excluded_item_types)))
        }

        return factory.selectFrom(q)
            .where(where)
            // 미예약(`proposed`)은 `start_at` 이 없다 — 뒤가 아니라 **앞**에 와야 좌측 목록이 그것부터 보여 준다.
            .orderBy(q.start_at.asc().nullsFirst(), q.created_at.asc())
            .fetch()
            .let { views(scope, it) }
    }

    /** 예약 → 응답 — 환자 신원 · 세션 한 칸 칸 여부(1.48)는 id 묶음 한 번으로 읽는다. */
    private fun views(scope: tenant_scope, rows: List<appointment>): List<appointment_view_model> {
        if (rows.isEmpty()) return emptyList()
        val patients = shared_directory.patients(rows.map { it.patient_id })
        val bands = slots.whole_session_bands(scope, rows.mapNotNull { it.slot_id })
        val today = today()
        return rows.map { view_mappers.to_view(it, patients[it.patient_id], today, it.slot_id?.let(bands::get)) }
    }
```

검색어 — `shared.patient` 서브쿼리(`JPAExpressions`) — `$B/infrastructure/queries/appointment_query_dsl.kt:128-146`:

```kotlin
    /**
     * 검색어 — 대소문자 무시 포함 검색을 일곱 칸에 OR 로 건다(classdiagram 1.20 · 부록 P84).
     * 환자명 · 환자번호는 `shared.patient` 서브쿼리(부록 P124), 예약 주체 이름 · 예약 주체 진료과 · 처방 진료과명 · 처방명 · 처방 코드는 이 예약 행의 스냅샷 칸이다.
     */
    private fun keyword_matches(keyword: String): BooleanBuilder =
        BooleanBuilder()
            .or(
                q.patient_id.`in`(
                    JPAExpressions.select(p.patient_id).from(p)
                        .where(p.patient_name.containsIgnoreCase(keyword).or(p.patient_number.containsIgnoreCase(keyword))),
                ),
            )
            .or(q.actor_value.actor_name.containsIgnoreCase(keyword))
            .or(q.actor_value.department_name.containsIgnoreCase(keyword))
            .or(q.order_value.department_name.containsIgnoreCase(keyword))
            .or(q.order_value.display.containsIgnoreCase(keyword))
            // 처방 코드 — 항암 주사일정이 약제 코드로도 찾는다(classdiagram 1.24).
            .or(q.order_value.code.containsIgnoreCase(keyword))
```

튜플 projection(`factory.select(...)`) + shared 신원 병합 + 메모리 정렬 — `$B/infrastructure/queries/appointment_query_dsl.kt:257-307`:

```kotlin
    /**
     * 예약 환자 목록(classdiagram 1.47) — 활성 예약(`booked` · `fulfilled`)을 예약일 · 구분 · 예약실로 거르고 한 건을 한 줄로 낸다.
     * 예약실은 루트의 예약 주체(`actor_snapshot.actor_id`)만 본다(연속 예약도 루트 한 줄). 신원은 shared.patient id 묶음 한 번이다.
     */
    @Transactional(readOnly = true)
    override fun find_patient_list(scope: tenant_scope, filter: appointment_patient_filter): List<appointment_patient_view_model> {
        val where = BooleanBuilder(in_scope(q.tenant_id, q.hospital_code, scope))
            .and(q.status.`in`(appointment_status.entries.filter { it.is_active() }))
            .and(q.visit_date.goe(filter.from))
            .and(q.visit_date.loe(filter.to))
        if (filter.categories.isNotEmpty()) where.and(q.category.`in`(filter.categories))
        if (filter.actor_ids.isNotEmpty()) where.and(q.actor_value.actor_id.`in`(filter.actor_ids))

        val rows = factory.select(
            q.id, q.category, q.schedule_id, q.actor_value.actor_id, q.actor_value.actor_name, q.start_at, q.visit_date,
            q.patient_id, q.order_value.code, q.order_value.display, q.booked_at,
        )
            .from(q)
            .where(where)
            .fetch()
        val patients = shared_directory.patients(rows.mapNotNull { it.get(q.patient_id) })
        return rows.map { row ->
            val patient_id = row.get(q.patient_id)!!
            val identity = patients[patient_id]
            val category = row.get(q.category)!!
            appointment_patient_view_model(
                appointment_id = row.get(q.id)!!,
                service_category = category.code,
                service_category_display = category.display,
                schedule_id = row.get(q.schedule_id),
                actor_id = row.get(q.actor_value.actor_id),
                actor_name = row.get(q.actor_value.actor_name),
                start_at = row.get(q.start_at),
                visit_date = row.get(q.visit_date),
                patient_id = patient_id.toString(),
                patient_number = identity?.patient_number,
                patient_name = identity?.name,
                sex = identity?.sex,
                birth_date = identity?.birth_date,
                order_code = row.get(q.order_value.code),
                order_display = row.get(q.order_value.display),
                booked_at = row.get(q.booked_at),
            )
        }.sortedWith(
            // 환자명은 shared 값이라 DB 정렬에 넣지 않고 여기서 함께 정렬한다 — 예약일시 → 예약실 이름 → 환자명.
            compareBy<appointment_patient_view_model, java.time.Instant?>(nullsLast()) { it.start_at }
                .thenBy(nullsLast()) { it.actor_name }
                .thenBy(nullsLast()) { it.patient_name },
        )
    }
}
```

### 4.3 `shared` 스키마 읽기 전용 엔티티와 조회

환자 매핑 — `$B/infrastructure/shared/shared_patient.kt:11-46`(`@Immutable` · `@Table(schema = "shared")` · 쓰는 컬럼만):

```kotlin
/**
 * `shared.patient` 읽기 전용 매핑 — 프로젝트 공유 스키마의 환자.
 *
 * 이 MS 가 읽는 것은 **환자 신원**(이름 · 환자번호 · 성별 · 생년월일)뿐이다 — 예약에는 `patient_id` 만 두고
 * 응답 · 검색어 · 예약 환자 목록 · 발행 이벤트가 이 테이블에서 신원을 읽는다(classdiagram 1.27 · 부록 P124).
 * 쓰기 경로는 없다(`@Immutable` · 소유자는 원무). **id 만으로** 잇고 테넌트 조건은 걸지 않는다.
 *
 * **쓰는 컬럼만** 매핑한다(`schema_mapping_test` 가 목록을 지킨다). `gender_code` 의 값 체계는 정해지지 않아 첫 글자로만 읽는다.
 */
@Entity
@Immutable
@Table(schema = SHARED_SCHEMA, name = "patient")
class shared_patient protected constructor() {

    @Id
    @Column(name = "patient_id", nullable = false)
    var patient_id: UUID = UUID(0, 0)
        protected set

    /** 환자번호(FHIR `Patient.identifier` type `MR`). */
    @Column(name = "patient_number")
    var patient_number: String? = null
        protected set

    @Column(name = "patient_name")
    var patient_name: String? = null
        protected set

    @Column(name = "gender_code")
    var gender_code: String? = null
        protected set

    @Column(name = "birth_date")
    var birth_date: LocalDate? = null
        protected set
}
```

상수 정의 — `$B/infrastructure/shared/shared_user_account.kt:39-40`:

```kotlin
/** 공유 스키마 이름 — 프로젝트 정책으로 고정된 값이다. */
const val SHARED_SCHEMA = "shared"
```

application 쪽 IF — `$B/application/queries/shared_directory_queries.kt:50-64`:

```kotlin
/**
 * shared 직원 · 환자를 **id 묶음으로** 찾는다 — 구현은 `infrastructure/queries/shared_directory_query_dsl`(QueryDSL).
 *
 * 직원 — 감사 컬럼(`created_by` · `updated_by`)의 작성자 · 저장자 이름(비고 · 기간 비고 · 진료시간 기준 · 오버부킹 범위)에 쓴다.
 * 환자 — 예약 응답 · 예약 환자 목록 · 발행 이벤트의 신원(이름 · 환자번호 · 성별 · 생년월일)에 쓴다(부록 P124).
 * 처방의 · 처방 진료과 이름은 여기서 찾지 않는다 — 예약 스냅샷 값이다.
 * 조회는 **id 만으로** 한다(테넌트 조건 없음 — uuid 가 이미 유일하다). 행 조인으로 붙이지 않아 shared 쪽 id 가 겹쳐도 예약 줄이 늘지 않는다.
 */
interface i_shared_directory_queries {

    fun staff(ids: Collection<UUID>): Map<UUID, staff_view>

    /** 환자 신원 — 없는 id 는 결과에 없다. */
    fun patients(ids: Collection<UUID>): Map<UUID, patient_identity_view>
}
```

QueryDSL 구현(id 묶음 · chunk) — `$B/infrastructure/queries/shared_directory_query_dsl.kt:17-71`:

```kotlin
/**
 * shared 직원 이름 · 환자 신원 찾기 — **QueryDSL**, id 묶음 한 번에 한 쿼리.
 * 조건은 **id 만**이다(테넌트 조건을 넣지 않는다 — uuid 가 이미 유일하다).
 */
@Repository
class shared_directory_query_dsl(
    private val factory: JPAQueryFactory,
) : i_shared_directory_queries {

    private val user = Qshared_user_account.shared_user_account
    private val patient = Qshared_patient.shared_patient

    @Transactional(readOnly = true)
    override fun staff(ids: Collection<UUID>): Map<UUID, staff_view> {
        val wanted = ids.distinct()
        if (wanted.isEmpty()) return emptyMap()
        return factory.select(user.user_account_id, user.user_account_name, user.employee_number)
            .from(user)
            .where(user.user_account_id.`in`(wanted))
            .fetch()
            .mapNotNull { row ->
                val id = row.get(user.user_account_id) ?: return@mapNotNull null
                id to staff_view(id, row.get(user.user_account_name), row.get(user.employee_number))
            }
            .toMap()
    }

    @Transactional(readOnly = true)
    override fun patients(ids: Collection<UUID>): Map<UUID, patient_identity_view> {
        val wanted = ids.distinct()
        if (wanted.isEmpty()) return emptyMap()
        // in 목록이 너무 길지 않게 나눠 읽는다 — 기간 목록은 환자 수천 명이 될 수 있다.
        return wanted.chunked(PATIENT_CHUNK).flatMap { chunk ->
            factory.select(patient.patient_id, patient.patient_name, patient.patient_number, patient.gender_code, patient.birth_date)
                .from(patient)
                .where(patient.patient_id.`in`(chunk))
                .fetch()
        }.mapNotNull { row ->
            val id = row.get(patient.patient_id) ?: return@mapNotNull null
            id to patient_identity_view(
                patient_id = id,
                name = row.get(patient.patient_name).blank_to_null(),
                patient_number = row.get(patient.patient_number).blank_to_null(),
                sex = patient_identity_view.sex_of(row.get(patient.gender_code)),
                birth_date = row.get(patient.birth_date),
            )
        }.toMap()
    }

    private companion object {
        /** 한 번에 묻는 환자 id 수. */
        const val PATIENT_CHUNK = 1_000
    }
}
```

view_model — `$B/application/queries/appointment_view_models.kt:114-171`(예약) · `:172-199`(예약 환자 목록 한 줄):

```kotlin
data class appointment_view_model(
    val appointment_id: String,
    val status: String,
    val service_category: String,
    val based_on_type: String?,
    val based_on_id: String?,
    val patient_id: String,
    val order_snapshot: order_snapshot_view_model,
    val patient_snapshot: patient_snapshot_view_model,
    /** 환자 신원(shared.patient). */
    val patient: patient_view_model = patient_view_model(),
    val schedule_id: String?,
    val actor_snapshot: actor_snapshot_view_model,
    val slot_id: String?,
    val start_at: Instant?,
    val end_at: Instant?,
    val visit_date: LocalDate?,
    /** 희망일(FHIR `requestedPeriod.start`) — 받지 않은 예약은 null. */
    val preferred_date: LocalDate? = null,
    val priority: String?,
    val encounter_id: String?,
    /** `planned`(예약 등록 시 만든 접수 · 이동 · 취소 허용) · `arrived` 이후(잠금). */
    val encounter_status: String? = null,
    val series_group_id: String?,
    val series_seq: Int?,
    val series_total: Int?,
    val booking_channel: String,
    /** 오버부킹 슬롯에 들어간 예약 — 처방 수신 자동 예약이 칸을 못 찾은 날(classdiagram 1.22). */
    val overbooked: Boolean = false,
    /** 루트 칸(`slot_id`)이 세션 한 칸 칸 — 화면은 시각 대신 [session_band] 글자로 적는다(classdiagram 1.48). */
    val whole_session: Boolean = false,
    /** 세션 한 칸 칸의 시간대 — `am` 「오전」 · `pm` 「오후」 · `night` 「야간」 · `day` 「종일」(예약 기준의 세션 단위가 정한다 · classdiagram 1.48 · 부록 P127). 아니면 null. */
    val session_band: String? = null,
    /** 예약 경로(`in-house` · `call-center` · `mobile` · `internet`) — 받지 않은 예약은 null(classdiagram 1.19). */
    val booking_route: String? = null,
    val cancellation_reason: String?,
    val cancelled_at: Instant?,
    val note: String?,
    /** 마지막 이동의 사유 · 메모 · 시각 — 사유를 실은 이동만 채운다(classdiagram §2.12). */
    val reschedule_reason: String? = null,
    val reschedule_note: String? = null,
    val rescheduled_at: Instant? = null,
    val phases: List<appointment_phase_view_model>,
    val created_at: Instant,
    val booked_at: Instant?,
    /** 예약수정일시(FHIR `meta.lastUpdated`). */
    val updated_at: Instant? = null,
    val version: Long,
    /**
     * 상태 전이 규칙을 **서버가 계산**해 내려준다 — 프런트에 전이 규칙을 중복 구현하지 않기 위한 장치다.
     */
    val allowed_actions: List<String> = emptyList(),
)
```

```kotlin
data class appointment_patient_view_model(
    val appointment_id: String,
    /** 예약구분 코드(`injection` …). */
    val service_category: String,
    /** 예약구분 이름(「주사」 …). */
    val service_category_display: String,
    val schedule_id: String?,
    /** 예약실 — 예약 주체 방 id(`schedule.actor_id`). */
    val actor_id: String?,
    /** 예약실 이름 — 예약 당시 표시값. */
    val actor_name: String?,
    /** 예약일시. */
    val start_at: Instant?,
    /** 예약일(병원 기준). */
    val visit_date: LocalDate?,
    val patient_id: String,
    val patient_number: String?,
    val patient_name: String?,
    /** `M` · `F` · `U`. */
    val sex: String?,
    val birth_date: LocalDate?,
    /** 처방코드. */
    val order_code: String?,
    /** 처방명. */
    val order_display: String?,
    /** 예약등록일시. */
    val booked_at: Instant?,
)
```

---

## 5. JPA 매핑

### 5.1 Aggregate Root 전체 — `appointment` (`$B/domain/aggregatesmodel/appointment/appointment.kt`)

클래스 선언 · 어노테이션 — `:41-60`:

```kotlin
/**
 * FHIR `Appointment` Aggregate Root — **예약이 들어간 것**.
 * (`patient-appointment.classdiagram.md` §2.12)
 *
 * - `basedOn` = 모듈 처방(`based_on_type` + `based_on_id`) — 정본은 밖이고 여기엔 스냅샷만 있다(P46).
 * - **선점(hold · pending · TTL)은 없다** — 예약 등록은 `proposed → booked` 한 번이고, 슬롯 점유는 저장할 때만 일어난다(P45).
 *   동시 점유는 `slot.version` 낙관적 잠금이 409 로 돌린다(행 락 없음).
 * - `encounter_id` 는 접수의 **역참조 캐시**다 — 예약 등록 핸들러가 (처방일 · 진료과 · 진료의 · 예약일) 묶음마다
 *   원무에 접수를 요청하고(`requested` · 불변식 28 · 1.28), 원무 회신이 `planned` 접수를 붙인다. 접수 요청 id 는 두지 않는다(부록 P105).
 *   창구 접수 이벤트는 `arrived` 접수를 붙인다.
 *   FHIR 방향은 `Encounter.appointment[]` 이고 「여러 예약 = 한 접수」가 그대로 성립한다.
 *
 * ⚠ **슬롯 점유는 이 클래스가 직접 하지 않는다.** 상태 전이만 하고 `slot.occupy/release` 는
 * `slot_occupation_service` 가 같은 트랜잭션에서 수행한다(불변식 4) — 애그리게잇 둘을 건드리는 유일한 곳이다.
 */
@Entity
@Table(name = "appointment")
@AttributeOverride(name = "id", column = Column(name = "appointment_id", length = 36))
class appointment protected constructor() : entity(), i_aggregate_root {
```

`@Enumerated` · `@Embedded`(nullable 백킹 필드) · 논리 FK — `:61-129`:

```kotlin
    val appointment_id: String get() = id

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 24)
    var status: appointment_status = appointment_status.PROPOSED
        protected set

    /** `schedule` 에서 복사 — 좌측 목록 배지·탭이 조인 없이 그린다. */
    @Enumerated(EnumType.STRING)
    @Column(name = "service_category", nullable = false, length = 32)
    var category: service_category = service_category.EXAMINATION
        protected set

    /** 처방을 소유한 모듈. 외래 신환은 null. */
    @Enumerated(EnumType.STRING)
    @Column(name = "based_on_type", length = 32)
    var based_on_type_value: based_on_type? = null
        protected set

    /** 그 모듈 안 처방의 PK. `(type, id)` 는 **UNIQUE 가 아니다**(반복처방·재예약). */
    @Column(name = "based_on_id", length = 64)
    var based_on_id: String? = null
        protected set

    /** 환자 — `shared.patient.patient_id`(uuid). 이름 · 환자번호 · 성별 · 생년월일은 두지 않고 이 id 로 shared 에서 읽는다(부록 P124). */
    @Column(name = "patient_id", nullable = false)
    var patient_id: UUID = UUID(0, 0)
        protected set

    /**
     * ⚠ 임베드 VO 는 **nullable 백킹 필드**로 둔다 — Hibernate 는 VO 의 컬럼이 **전부 NULL 이면
     * 필드 자체를 null 로 채운다**(Kotlin 의 non-null 선언은 런타임에 강제되지 않는다).
     * `proposed` 예약은 `actor_*` 가 전부 비어 있어 거의 항상 그 경우이고, 그대로 두면 조회에서 NPE 가 난다.
     * 읽기 접근자가 **빈 VO** 를 보장하므로 바깥 코드는 null 을 몰라도 된다.
     */
    @Embedded
    protected var order_value: order_snapshot? = null

    val order: order_snapshot get() = order_value ?: order_snapshot.empty()

    @Embedded
    protected var patient_value: patient_snapshot? = null

    val patient: patient_snapshot get() = patient_value ?: patient_snapshot.empty()

    @Embedded
    protected var actor_value: actor_snapshot? = null

    val actor: actor_snapshot get() = actor_value ?: actor_snapshot.empty()

    /** 논리 FK — 다른 애그리게잇이라 JPA 연관을 걸지 않는다. */
    @Column(name = "schedule_id", length = 36)
    var schedule_id: String? = null
        protected set

    /** 점유 슬롯. `proposed` 는 null. 다단계는 phase 가 갖는다. */
    @Column(name = "slot_id", length = 36)
    var slot_id: String? = null
        protected set

    @Column(name = "start_at")
    var start_at: Instant? = null
        protected set

    @Column(name = "end_at")
    var end_at: Instant? = null
        protected set
```

날짜 · 접수 캐시 · 회차 — `:130-186`:

```kotlin
    /** `start_at` 의 병원 로컬 일자 — 달력 조회 인덱스. */
    @Column(name = "visit_date")
    var visit_date: LocalDate? = null
        protected set

    /**
     * 희망일 — FHIR `requestedPeriod.start`. 호출 화면이 넘긴 희망일을 `proposed` 를 만들거나 다시 열 때 남긴다
     * ([enter_preferred_date] · classdiagram §2.12 · 1.20). 예약 검색의 날짜 기준 하나다.
     */
    @Column(name = "preferred_date")
    var preferred_date: LocalDate? = null
        protected set

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", length = 16)
    var priority: appointment_priority? = null
        protected set

    /** 접수 역참조 캐시 — `shared.encounter.encounter_id`(uuid). 같은 값을 가진 예약이 여럿일 수 있다(같은 묶음 · 불변식 28). */
    @Column(name = "encounter_id")
    var encounter_id: UUID? = null
        protected set

    /**
     * `Encounter.status` 캐시 — `planned`(예약 등록 시 만든 접수)는 이동 · 취소를 허용하고 `arrived` 이후는 잠근다(불변식 25).
     * `encounter_id` 는 있는데 이 값이 비어 있으면 `arrived` 로 본다(상태를 모르는 접수는 잠그는 쪽이 안전하다).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "encounter_status", length = 16)
    var encounter_status_value: encounter_status? = null
        protected set

    /**
     * 오버부킹 슬롯에 들어간 예약인가 — 처방 수신 자동 예약이 칸을 못 찾아 그 날 00:00 오버부킹 슬롯에 넣었다(classdiagram 1.22 · 부록 P87).
     * 다른 칸으로 옮기면 `false` 로 돌아간다.
     */
    @Column(name = "overbooked", nullable = false)
    var overbooked: Boolean = false
        protected set

    @Column(name = "series_group_id", length = 36)
    var series_group_id: String? = null
        protected set

    @Column(name = "series_seq")
    var series_seq: Int? = null
        protected set

    @Column(name = "series_total")
    var series_total: Int? = null
        protected set

    @Enumerated(EnumType.STRING)
    @Column(name = "booking_channel", nullable = false, length = 16)
    var channel: appointment_booking_channel = appointment_booking_channel.CENTRAL
        protected set
```

`@OneToMany`(EAGER phase · LAZY 이력) · `@Version` — `:187-247`:

```kotlin
    /**
     * 예약 경로(`in-house` · `call-center` · `mobile` · `internet`) — 경로를 받지 않은 예약은 `null`.
     * 예약 등록 명령 · 모바일 예약 API 가 예약 등록 직전에 적고(classdiagram 1.18 · 1.19), `mobile` · `internet` 만 예약 등록 판정의 경로 축으로 쓴다(불변식 44).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "booking_route", length = 16)
    var booking_route: booking_route? = null
        protected set

    @Enumerated(EnumType.STRING)
    @Column(name = "cancellation_reason", length = 32)
    var cancellation_reason_value: cancellation_reason? = null
        protected set

    @Column(name = "cancelled_at")
    var cancelled_at: Instant? = null
        protected set

    @Column(name = "note", length = 500)
    var note: String? = null
        protected set

    /** 마지막 이동의 사유 — `cancellation_reason` 과 같은 ValueSet. 사유를 실은 이동만 채운다(§2.12). */
    @Enumerated(EnumType.STRING)
    @Column(name = "reschedule_reason", length = 32)
    var reschedule_reason_value: cancellation_reason? = null
        protected set

    @Column(name = "reschedule_note", length = 500)
    var reschedule_note: String? = null
        protected set

    @Column(name = "rescheduled_at")
    var rescheduled_at: Instant? = null
        protected set

    @OneToMany(mappedBy = "owner", cascade = [CascadeType.ALL], fetch = FetchType.EAGER, orphanRemoval = true)
    @OrderBy("phase_seq asc")
    protected var phase_values: MutableList<appointment_phase> = mutableListOf()

    val phases: List<appointment_phase> get() = phase_values.sortedBy { it.phase_seq }

    /**
     * 이동 이력 — 이동마다 한 줄(classdiagram §2.27 · 1.24). 읽는 쪽은 조회(`find_changes`)라 지연 로딩이고,
     * 이동할 때만 이 컬렉션을 건드린다.
     */
    @OneToMany(mappedBy = "owner", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    @OrderBy("changed_at asc")
    protected var change_values: MutableList<appointment_change> = mutableListOf()

    val changes: List<appointment_change> get() = change_values.toList()

    @Column(name = "booked_at")
    var booked_at: Instant? = null
        protected set


    @Version
    @Column(name = "version", nullable = false)
    var version: Long = 0
        protected set
```

팩토리(`companion.of`) — `:727-774`(UUID v7 발급 · 테넌트 지정 · 불변식 24 · 46):

```kotlin
    companion object {

        /**
         * `proposed` 생성 — 처방 이벤트 리스너와 링크 진입이 부른다.
         * 예약 근거(`based_on`)는 모든 예약에 필수다 — 외래는 `outpatient-clinic` · 환자 id(불변식 24).
         * 구분은 그 근거의 소속 구분이어야 한다(불변식 46).
         */
        fun of(
            category: service_category,
            based_on_type_value: based_on_type?,
            based_on_id: String?,
            patient_id: UUID,
            order: order_snapshot,
            patient: patient_snapshot,
            priority: appointment_priority?,
            channel: appointment_booking_channel,
            at: Instant,
            scope: tenant_scope,
        ): appointment {
            if (based_on_type_value == null || based_on_id.isNullOrBlank()) {
                throw based_on_required_domain_exception(category.code)
            }
            if (based_on_type_value.category != category) {
                throw based_on_category_mismatch_domain_exception(
                    based_on_type_value.code,
                    based_on_type_value.category.code,
                    category.code,
                )
            }
            return appointment().apply {
                // 예약 id 는 UUID(v7)다 — 이 BC 의 다른 Entity 와 같은 발급기(그 전에 만든 예약은 ULID · UUID v4 그대로).
                assign_id(uuid7.next(at))
                assign_tenant(scope)
                this.status = appointment_status.PROPOSED
                this.category = category
                this.based_on_type_value = based_on_type_value
                this.based_on_id = based_on_id
                this.patient_id = patient_id
                this.order_value = order
                this.patient_value = patient
                this.priority = priority
                this.channel = channel
                this.created_at = at
                this.updated_at = at
            }
        }
    }
}
```

감사 · 테넌트 컬럼(`tenant_id` · `hospital_code` · `created_at/by` · `updated_at/by`)은 베이스 `entity` 가 갖는다(§3.10).

### 5.2 자식 Entity — `appointment_phase` (`$B/domain/aggregatesmodel/appointment/appointment_phase.kt:18-80`)

`@ManyToOne(LAZY)` 로 루트(owner)만 가리키고, 다른 Aggregate(schedule · slot)는 **문자열 id** 로만 참조한다.

```kotlin
/**
 * 다단계 검사의 단계. (`patient-appointment.classdiagram.md` §2.16)
 *
 * 「방사성의약품 주사(주사실) → 업테이크 대기 → 촬영(감마카메라)」처럼 **대상과 시각이 갈리는 검사**에만 생긴다.
 * 단일 검사(CT · 도수치료)는 0건이고 루트 필드만 쓴다.
 *
 * 단계마다 예약 행을 만들지 않는 이유(P19): 「검사 1건」이 목록·취소·접수의 단위다.
 * 나누면 환자에게 예약이 3건으로 보이고 취소가 부분 성공한다.
 */
@Entity
@Table(name = "appointment_phase")
@AttributeOverride(name = "id", column = Column(name = "appointment_phase_id", length = 36))
class appointment_phase protected constructor() : entity() {

    val phase_id: String get() = id

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "appointment_id", nullable = false)
    protected var owner: appointment? = null

    @Column(name = "phase_seq", nullable = false)
    var phase_seq: Int = 1
        protected set

    @Enumerated(EnumType.STRING)
    @Column(name = "phase_code", nullable = false, length = 16)
    var code: phase_code = phase_code.SCAN
        protected set

    @Column(name = "phase_name", nullable = false, length = 100)
    var phase_name: String = ""
        protected set

    /** 이 단계의 스케줄 — `wait` 는 없다(null). */
    @Column(name = "schedule_id", length = 36)
    var schedule_id: String? = null
        protected set

    /** 임베드 VO 는 nullable 백킹 필드다 — 컬럼이 전부 NULL 이면 Hibernate 가 필드를 null 로 채운다. */
    @Embedded
    protected var actor_value: actor_snapshot? = null

    val actor: actor_snapshot get() = actor_value ?: actor_snapshot.empty()

    @Column(name = "slot_id", length = 36)
    var slot_id: String? = null
        protected set

    @Column(name = "start_at")
    var start_at: Instant? = null
        protected set

    @Column(name = "end_at")
    var end_at: Instant? = null
        protected set

    /** 앞 단계 종료 후 최소 간격(업테이크 180분) — 처방 포트가 준 값을 복사한 표시값. */
    @Column(name = "min_gap_minutes")
    var min_gap_minutes: Int? = null
        protected set

    /** 대기 단계는 슬롯을 점유하지 않는다. */
    fun needs_slot(): Boolean = code.needs_slot()
```

### 5.3 VO — `actor_snapshot` (`$B/domain/aggregatesmodel/appointment/appointment_snapshots.kt:292-343`)

`@Embeddable` + `value_object` 상속. `protected constructor` + `companion.of` 팩토리.

```kotlin
/**
 * 대상 스냅샷 — `participant[actor].display`.
 * 스케줄이 바뀌거나 비활성이 돼도 목록·달력은 **예약 당시의 방·치료사 이름**을 그린다.
 */
@Embeddable
class actor_snapshot protected constructor() : value_object() {

    @Enumerated(EnumType.STRING)
    @Column(name = "actor_type", length = 24)
    var actor_type: schedule_actor_type? = null
        protected set

    @Column(name = "actor_id", length = 64)
    var actor_id: String? = null
        protected set

    @Column(name = "actor_name", length = 200)
    var actor_name: String? = null
        protected set

    @Column(name = "actor_department_id", length = 64)
    var department_id: String? = null
        protected set

    @Column(name = "actor_department_name", length = 200)
    var department_name: String? = null
        protected set

    fun is_empty(): Boolean = actor_id == null

    override fun get_equality_components(): List<Any?> =
        listOf(actor_type, actor_id, actor_name, department_id, department_name)

    companion object {

        fun empty(): actor_snapshot = actor_snapshot()

        fun of(
            actor_type: schedule_actor_type?,
            actor_id: String?,
            actor_name: String?,
            department_id: String?,
            department_name: String?,
        ): actor_snapshot = actor_snapshot().apply {
            this.actor_type = actor_type
            this.actor_id = actor_id
            this.actor_name = actor_name
            this.department_id = department_id
            this.department_name = department_name
        }
    }
}
```

### 5.4 enum — `appointment_status` (`$B/domain/common/appointment_enums.kt:14-16`, `:618-645`)

enum 은 전부 `coded` 를 구현해 **`code`(FHIR 코드)와 `name`(DB 저장값 — `EnumType.STRING`)** 을 분리한다.

```kotlin
interface coded {
    val code: String
}
```

```kotlin
/**
 * FHIR `appointmentstatus` 부분집합.
 * `pending`(임시 점유 · hold)은 쓰지 않는다 — 예약 등록은 `PROPOSED → BOOKED` 한 번이다(P45).
 */
enum class appointment_status(override val code: String) : coded {
    PROPOSED("proposed"),
    BOOKED("booked"),
    FULFILLED("fulfilled"),
    CANCELLED("cancelled"),
    NOSHOW("noshow"),
    ENTERED_IN_ERROR("entered-in-error"),
    ;

    /** 더 이상 변경 불가한 종료 상태인지. */
    fun is_terminal(): Boolean = this == FULFILLED || this == CANCELLED || this == NOSHOW || this == ENTERED_IN_ERROR

    /** 슬롯을 점유 중인 상태인지 — 취소·이동이 슬롯을 반환해야 하는지 판정. */
    fun occupies_slot(): Boolean = this == BOOKED

    /** 접수·현황 집계의 "활성" 예약인지. */
    fun is_active(): Boolean = this == BOOKED || this == FULFILLED

    companion object {
        fun from_code(value: String): appointment_status =
            entries.firstOrNull { it.code.equals(value, ignoreCase = true) || it.name.equals(value, ignoreCase = true) }
                ?: throw IllegalArgumentException("알 수 없는 Appointment.status 입니다: $value")
    }
}
```

---

## 6. 이벤트 수신/발행 (common-eventbus-kt)

구조 한눈에: **수신**은 `subscribeRaw` 로 원문을 받아 `inbox_message` 에 적재만 하고(ack), 별도 처리기가 T1(집기)·T2(처리)·T3(실패 기록) 트랜잭션으로 핸들러를 돌린다.
**발행**은 §3 의 `domain_event_integration_publisher`(AFTER_COMMIT) 가 한다. 토픽 라우팅은 `topic_registry` 한 곳.

### 6.1 토픽 설정 ① `application.yml` — `$R/application.yml:68-108`

```yaml
# Kafka — 수신: ERP 직원 일정(classdiagram 1.17) · 자동 예약 생성 요청(1.22) · 원무 회신(1.28).
#   발행: 예약 이벤트(1.23) · 접수 요청(1.28) · 자동 예약 생성 결과 회신(1.22).
#   ⚠️ 구독 · 발행은 projectx.patient-appointment.event-bus.enabled 하나로 켠다(기본 켜짐 · env EVENT_BUS_ENABLED).
#      브로커가 없는 환경은 EVENT_BUS_ENABLED=false 로 끈다 — 꺼지면 컨슈머를 만들지 않아 브로커에 붙지 않는다.
#   로컬 브로커 = 루트 docker compose(localhost:9092). dev 는 application-dev.yml 이 따로 준다.
  kafka:
    bootstrap-servers: ${SPRING_KAFKA_BOOTSTRAP_SERVERS:localhost:9092}
    consumer:
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      auto-offset-reset: earliest
    listener:
      # 브로커/토픽이 없는 상태로 부팅해도 fail-fast 하지 않는다(수신은 부가기능 — 예약 API 를 막지 않는다).
      missing-topics-fatal: false
    # 발행 토픽(appointment)은 이 BC 가 주인이라 KafkaAdmin 이 선언한다 — **이벤트버스를 켤 때만** 브로커에 붙는다
    #   (꺼진 환경은 부팅이 브로커를 기다리지 않는다). 못 닿아도 3초에 끊고 기동을 막지 않는다.
    admin:
      auto-create: ${EVENT_BUS_ENABLED:true}
      fail-fast: false
      operation-timeout: 3s
      properties:
        request.timeout.ms: 3000
        default.api.timeout.ms: 3000
    # 발행은 요청 스레드에서 동기다(라이브러리가 send().get()) — 브로커가 없을 때 요청을 오래 붙잡지 않게 대기를 짧게 묶는다.
    producer:
      properties:
        max.block.ms: 5000
        request.timeout.ms: 5000
        delivery.timeout.ms: 8000

# 이벤트버스 네임스페이스 — 토픽 이름의 앞 두 마디 `{환경}.{병원코드}`. 뒤 마디(이벤트명)는 topic_registry 가 갖는다.
#   ⚠️ 전 BC 가 같은 값이어야 한다 — 다르면 브로커 · 앱이 전부 정상인데 이벤트만 조용히 끊긴다. dev 는 application-dev.yml.
kafka_namespace: ${KAFKA_NAMESPACE:local.chabd}

common:
  event-bus:
    namespace: ${kafka_namespace}
    # ⚠️ common-eventbus-kt 0.1.18 은 기본으로 namespace 앞에 환경(profile)을 또 붙인다 — namespace 가 이미
    #    `{환경}.{병원}` 이라 켜 두면 토픽이 `local.local.chabd.…` 가 되어 발행측과 갈라진다. 끈다.
    env-prefix-enabled: false
    provider: ${PROJECTX_EVENT_BUS_PROVIDER:kafka}
```

스위치 · inbox 설정 — `$R/application.yml:117-141`:

```yaml
projectx:
  patient-appointment:
    # 이 배포가 맡은 테넌트 · 병원 — 수신 이벤트의 테넌트를 정하는 기준값(event_tenant_resolver)이고, 주체 없는 경로(배치 · 기동 시 작업)의 범위다.
    #   k8s 는 ConfigMap x-ehr-tenant-config(TENANT_ID · HOSPITAL_CODES, 콤마 구분 · 첫 값 = 기본 병원)가 준다.
    #   값이 없으면 INVALID_DATA — 봉투에도 값이 없는 수신분이 INVALID_DATA 로 적재된다(부팅은 막지 않는다).
    #   HTTP 요청은 이 값이 아니라 토큰의 tenantId · hospitalCode 를 쓴다.
    tenant:
      tenant-id: ${TENANT_ID:INVALID_DATA}
      hospital-codes: ${HOSPITAL_CODES:INVALID_DATA}
    # 이벤트 구독 · 발행 켜기 — 기본 켜짐. 켜 두려면 Kafka 접속값이 있어야 한다(브로커가 없는 환경은 EVENT_BUS_ENABLED=false).
    event-bus:
      enabled: ${EVENT_BUS_ENABLED:true}
    # 수신 inbox(classdiagram 1.34) — 받은 메시지는 inbox_message 에 적재만 하고 처리 · 재시도는 inbox 처리기가 한다.
    #   실패하면 backoff-seconds 만큼 기다려 다시 하고(마지막 값 반복), max-retry 에 닿거나 영구 실패면 FAILED 로 남는다
    #   (같은 토픽 · 같은 키 — 대개 같은 환자 — 의 뒤 메시지가 멈춘다 · ERROR 로그 「inbox 처리 실패 확정」).
    #   ⚠ lease-seconds 는 process-timeout-seconds 보다, process-timeout-seconds 는 포트 호출 시간 제한(read-timeout)보다 길어야 한다.
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

dev 프로파일은 값을 상속하지 않고 다시 적는다 — `$R/application-dev.yml:39-64`:

```yaml
  # dev Kafka — GCP Managed Kafka(SASL_SSL + OAUTHBEARER). 주소와 SASL 4종은 배포레포의 공용 configMap
  #   `x-ehr-foundation-be-kafka` 가 준다(SASL 은 relaxed binding 으로 흡수 · 주소만 키 이름이 달라 여기서 잇는다).
  #   ⚠️ 기본값은 비워 둔다 — configMap 없이 켜지면 컨슈머 생성이 실패해 ERROR 로그로 드러난다(폐용 브로커를 조용히 보지 않는다).
  #      configMap · Workload Identity 가 없는 배포는 values 의 configData 로 EVENT_BUS_ENABLED=false 를 준다.
  kafka:
    bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS:}
    # 발행 토픽 선언 · 발행 대기 — application.yml 과 같은 값(프로필 상속에 기대지 않는다).
    admin:
      auto-create: ${EVENT_BUS_ENABLED:true}
      fail-fast: false
      operation-timeout: 3s
      properties:
        request.timeout.ms: 3000
        default.api.timeout.ms: 3000
    producer:
      properties:
        max.block.ms: 5000
        request.timeout.ms: 5000
        delivery.timeout.ms: 8000

# 이벤트버스 네임스페이스 — 전 BC 공통 dev 값.
kafka_namespace: ${KAFKA_NAMESPACE:dev.chabd}

common:
  event-bus:
    # ⚠️ namespace 가 이미 `{환경}.{병원}` 이다 — 라이브러리(0.1.18)의 환경 접두를 끄지 않으면 토픽이 `dev.dev.chabd.…` 가 된다.
    env-prefix-enabled: false
```

### 6.2 토픽 설정 ② `topic_registry`(`@Configuration` · `MapEventRouter`) — `$B/infrastructure/eventbus/topic_registry.kt:14-93`

```kotlin
/**
 * 이벤트 ↔ 토픽 라우팅. (classdiagram 1.17 · 1.22 · 1.23 · 1.28 · api §9)
 *
 * **토픽은 `{환경}.{병원코드}.{이벤트명}` 이다.** 앞의 두 마디(`local.chabd` · `dev.chabd`)는
 * `common.event-bus.namespace` 가 붙이므로 `kafkaTopic` 에는 **이벤트명(kebab-case)** 만 적는다 — 발행 모듈명은 토픽에 넣지 않는다.
 *
 * 이 MS 가 구독하는 것은 **ERP 직원 일정 · 자동 예약 생성 요청(classdiagram 1.22) · 원무 수진 등록 회신(1.28)** 이다 —
 * 발행 route 는 예약 이벤트(`appointment` · 1.23) · 접수 요청(`encounter-request` · 1.28) ·
 * 자동 예약 생성 결과 회신(`order-based-appointment-booked` · 1.22) 셋이다. 자동 예약 생성 요청은 여러 영역이 함께 보내는 공용 토픽이라 보내는 쪽이 주인이다.
 * **토픽의 주인이라 `kafkaPartitions` 를 준다**
 * (KafkaAdmin 이 토픽을 선언한다. 비워 두면 auto-create 가 꺼진 브로커에서 첫 발행이 `not present in metadata` 로 실패한다).
 * 구독 등록은 [event_bus_subscription_initializer] 가 설정으로 켤 때만 한다. 라우터는 늘 서 있어도 브로커에 붙지 않는다.
 * 받는 토픽 목록은 [received_event.RECEIVED_EVENTS] 한 곳이다 — 구독 · inbox 분배와 같은 목록을 읽는다.
 * `partitionKeyField` 는 라이브러리(`MapEventRouter`)가 부팅 때 그 이름의 속성이 이벤트 클래스에 있는지 검사한다.
 * Rabbit 값은 provider 전환 대비 자리만 채운다(provider = kafka).
 */
@Configuration
class topic_registry {

    @Bean
    fun event_router(): EventRouter = MapEventRouter(
        buildMap<KClass<*>, EventRoute> {
            // 수신 — ERP 직원 일정 · 자동 예약 생성 요청 · 원무 메시지(목록은 received_event 한 곳 · inbox 로 받는다 · classdiagram 1.34)
            RECEIVED_EVENTS.forEach { put_route(it.topic, it.partition_key_field, it.event_type) }
            // 발행 — 예약 등록 · 이동 · 취소(같은 예약의 순서를 한 파티션에 묶는다)
            put_route(
                appointment_integration_event.TOPIC,
                appointment_integration_event.PARTITION_KEY_FIELD,
                appointment_integration_event::class,
                partitions = OWNED_PARTITIONS,
            )
            // 발행 — 자동 예약 생성 결과 회신(요청한 쪽에 그 처방의 예약 + 접수 · 환자로 순서를 묶는다)
            put_route(
                order_based_appointment_booked_integration_event.TOPIC,
                order_based_appointment_booked_integration_event.PARTITION_KEY_FIELD,
                order_based_appointment_booked_integration_event::class,
                partitions = OWNED_PARTITIONS,
            )
            // 발행 — 접수 요청(환자로 순서를 묶는다 — 같은 접수의 생성 → 수정 → 취소 포함)
            put_route(
                encounter_request_integration_event.TOPIC,
                encounter_request_integration_event.PARTITION_KEY_FIELD,
                encounter_request_integration_event::class,
                partitions = OWNED_PARTITIONS,
            )
        },
    )

    /**
     * 이벤트 클래스 1개 = route 1개 — 행위별로 토픽 · group · partition key 가 어긋날 자리를 두지 않는다.
     */
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
                // 같은 일정의 생성 → 변경 → 취소 순서를 한 파티션에 묶는다.
                partitionKeyField = partition_key_field,
                consumerGroupId = "$CONSUMER_PREFIX.$topic",
                kafkaPartitions = partitions,
            ),
        )
    }

    private companion object {
        /** consumer group 접두 — 뒤에 namespace 가 다시 붙어 `dev.chabd.patient-appointment.employee-schedule` 이 된다(토픽에는 발행 모듈명이 없어도 group 에는 받는 MS 이름이 남는다). */
        const val CONSUMER_PREFIX = "patient-appointment"

        /** 이 BC 가 주인인 토픽의 파티션 수. */
        const val OWNED_PARTITIONS = 3
    }
}
```

수신 토픽 목록 한 곳 — `$B/infrastructure/eventbus/received_event.kt:9-42`:

```kotlin
/**
 * 받는 토픽 1건 — 토픽(namespace 뒤 `이벤트명`) · 파티션 키 필드 · 이벤트 클래스(classdiagram 1.34).
 *
 * 수신 route 등록(`topic_registry`) · 구독(`event_bus_subscription_initializer`) · inbox 분배(`inbox_dispatcher`)가 모두
 * [RECEIVED_EVENTS] 한 목록을 읽는다 — 받는 토픽을 더하거나 뺄 때 여기와 `inbox_dispatcher` 의 핸들러 표를 함께 고친다.
 */
data class received_event(
    val topic: String,
    val partition_key_field: String,
    val event_type: KClass<out integration_event>,
) {
    companion object {
        val RECEIVED_EVENTS: List<received_event> = listOf(
            // ERP — 직원 일정(학회 · 연차 · classdiagram 1.17)
            received_event(
                erp_employee_schedule_integration_event.TOPIC,
                erp_employee_schedule_integration_event.PARTITION_KEY_FIELD,
                erp_employee_schedule_integration_event::class,
            ),
            // 진단검사 · 약제 · 투석 → 자동 예약 생성 요청(공용 토픽 · 보내는 쪽이 주인 · 환자로 순서를 묶는다 · 1.22)
            received_event(
                order_based_appointment_request_integration_event.TOPIC,
                order_based_appointment_request_integration_event.PARTITION_KEY_FIELD,
                order_based_appointment_request_integration_event::class,
            ),
            // 원무 → 수진 등록 회신 · 원무 주도 외래 예약(환자마다 1건 · 항목 배열 · 환자로 순서를 묶는다 · 1.28)
            received_event(
                appointment_request_integration_event.TOPIC,
                appointment_request_integration_event.PARTITION_KEY_FIELD,
                appointment_request_integration_event::class,
            ),
        )
    }
}
```

| 방향 | 이벤트명(토픽 뒷마디) | `@MessageType` | partitionKeyField | 클래스(파일:줄) |
|---|---|---|---|---|
| 수신 | `employee-schedule` | `ErpEmployeeSchedule` | `employeeScheduleId`(camelCase) | `erp_employee_schedule_integration_event.kt:16-51` |
| 수신 | `order-based-appointment-request` | `OrderBasedAppointmentRequest` | `patient_id` | `order_based_appointment_integration_events.kt:20, 108-111` |
| 수신 | `appointment-request` | `AppointmentRequest` | `patient_id` | `encounter_request_integration_events.kt:136, 152-155` |
| 발행 | `appointment` | `Appointment` | `appointment_id` | `appointment_integration_event.kt:23-25, 62-65` |
| 발행 | `encounter-request` | `EncounterRequest` | `patient_id` | `encounter_request_integration_events.kt:22-24, 37-40` |
| 발행 | `order-based-appointment-booked` | `OrderBasedAppointmentBooked` | `patient_id` | `order_based_appointment_integration_events.kt:148, 169-172` |

### 6.3 구독 등록(부팅) — `$B/infrastructure/eventbus/event_bus_subscription_initializer.kt:11-45`

```kotlin
/**
 * 부팅 시 EventBus 에 수신 등록한다 — [topic_registry] 와 짝이다. (classdiagram 1.17 · 1.34 · 부록 P81)
 *
 * **`projectx.patient-appointment.event-bus.enabled = true` 일 때만 선다**(기본 켜짐 · env `EVENT_BUS_ENABLED`).
 * **수신은 inbox 에 적재만 한다**(classdiagram 1.34) — 토픽마다 `subscribeRaw`(catch-all · 바인딩 없음)로 원문을 받아
 * `inbox_receiver` 가 `inbox_message` 에 넣는다. 핸들러 실행(봉투의 `tenantId` · `hospitalCode` 로 테넌트 범위를 정한다) · 재시도는
 * inbox 처리(`inbox_processor`)가 따로 한다. 그래서 본문이 깨졌거나 처리가 실패한 메시지도 DLT 로 가지 않고 inbox 에 남는다.
 * 꺼져 있으면(`EVENT_BUS_ENABLED=false`) 구독이 없어 브로커에 붙지 않는다 — Kafka 접속값이 없는 환경은 이렇게 끄고 뜬다.
 * 켤 때는 배포 values 에 Kafka 접속값(`x-ehr-foundation-be-kafka` configMap · Workload Identity)이 먼저 있어야 한다.
 */
@Component
@ConditionalOnProperty(prefix = "projectx.patient-appointment.event-bus", name = ["enabled"], havingValue = "true")
class event_bus_subscription_initializer(
    private val event_bus: EventBus,
    private val receiver: inbox_receiver,
) {

    private val log = LoggerFactory.getLogger(event_bus_subscription_initializer::class.java)

    @PostConstruct
    fun subscribe() {
        RECEIVED_EVENTS.forEach { subscribe(it) }
        log.info("이벤트 구독을 등록했습니다(inbox 적재) — {}", RECEIVED_EVENTS.joinToString(" · ") { it.topic })
    }

    /**
     * 구독 1건 등록. **실패해도 부팅을 막지 않는다** — 브로커 주소 오류 · DNS 실패로 컨슈머 생성이 깨지면 예외가
     * `@PostConstruct` 를 타고 올라가 컨텍스트가 죽고 예약 API 까지 전부 사라진다. 수신은 부가기능이므로 ERROR 로그만 남긴다.
     * ⚠ 그래서 「이벤트가 안 들어온다」는 이 ERROR 로그로만 드러난다 — 기동 로그를 확인한다.
     */
    private fun subscribe(event: received_event) {
        runCatching { event_bus.subscribeRaw(setOf(event.event_type)) { raw -> receiver.receive(event.topic, raw) } }
            .onFailure { log.error("이벤트 구독 등록 실패 — 이 토픽은 수신되지 않습니다: {}", event.topic, it) }
    }
}
```

### 6.4 inbox 적재 — `$B/infrastructure/inbox/inbox_receiver.kt:12-64`

```kotlin
/**
 * 수신 — 원문을 inbox 에 **적재만** 한다(비즈니스 로직 없음 · classdiagram 1.34). 적재가 커밋되고 여기서 돌아가면 Kafka 가 ack 한다.
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

    private val log = LoggerFactory.getLogger(inbox_receiver::class.java)

    /** [topic] 은 namespace 를 뺀 토픽(`이벤트명`) — 구독 등록 때 정해진다. */
    fun receive(topic: String, raw: RawMessage) {
        val message = to_inbox_message(topic, raw, Instant.now())
        try {
            if (tx.store(message)) {
                log.info("inbox 적재: topic={} messageId={} messageType={}", topic, message.message_id, message.event_type)
            } else {
                log.info("중복 수신 — 이미 적재된 메시지라 건너뜁니다: topic={} messageId={}", topic, message.message_id)
            }
        } catch (ex: DataIntegrityViolationException) {
            // 다른 무결성 위반(길이 초과 등)은 삼키지 않는다 — 중복이 확인될 때만 성공으로 본다.
            if (!tx.exists(message.message_id)) throw ex
            log.info("중복 수신(동시 재전달) — 건너뜁니다: topic={} messageId={}", topic, message.message_id)
        }
    }

    /**
     * 원문 → inbox 행. 봉투 메타(`messageId` · `messageType`)는 본문에서 읽고, 못 읽으면 대체한다:
     * `message_id` → 원문 SHA-256 hex(같은 원문의 재전달은 같은 값이라 중복이 걸러진다),
     * `event_type` → 헤더의 messageType → `UNKNOWN`, `partition_key` → Kafka 키가 없으면 `message_id`(혼자 처리된다).
     * 키는 로그에 남기지 않는다 — 이 BC 는 대개 환자 id 다.
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

inbox 행(엔티티 · 도메인 Entity 가 아니라 테넌트/감사 컬럼 없음, PK 는 IDENTITY = 도착 순번) — `$B/infrastructure/inbox/inbox_message.kt:15-37`, `:45-73`:

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

```kotlin
/**
 * 수신 원문 1건 — 통합 이벤트가 핸들러에 닿기 전에 머무는 자리(classdiagram 1.34).
 *
 * 도메인 Entity 가 아니라 수신 장치라 테넌트 · 감사 공통 컬럼(1.26)을 두지 않는다 — 테넌트는 처리 때 봉투로 정한다.
 * PK(`inbox_message_id`)는 **INSERT 순서의 IDENTITY** 이고 그 값이 도착 순번(seq)이다 — 같은 키 안의 처리 순서 · hold 비교의 기준.
 * 배치 할당 시퀀스(allocationSize)는 도착 순서를 깨므로 쓰지 않는다.
 *
 * 상태는 아래 메서드로만 바꾼다(서비스에서 필드를 직접 고치지 않는다).
 */
@Entity
@Table(name = "inbox_message")
class inbox_message protected constructor() {

    /** 도착 순번(seq) — DB IDENTITY. 저장 전에는 null. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "inbox_message_id", nullable = false)
    var inbox_message_id: Long? = null
        protected set

    /** 봉투 messageId(발행측 Outbox 이벤트 ID) — 중복 수신을 거르는 키. 본문에서 못 읽으면 원문 SHA-256 hex. */
    @Column(name = "message_id", nullable = false, length = 64)
    var message_id: String = ""
        protected set

    /** namespace 를 뺀 토픽(`이벤트명`) — 이벤트 클래스를 고르는 키. */
    @Column(name = "topic", nullable = false, length = 200)
    var topic: String = ""
        protected set
```

### 6.5 inbox 처리 루프(T1/T2/T3) — `$B/infrastructure/inbox/inbox_processor.kt:7-48`, `inbox_tx_service.kt:58-70`

```kotlin
/**
 * 키 하나를 차례로 흘린다 — **트랜잭션이 없다**(순서만 잡고 경계는 전부 `inbox_tx_service` 가 갖는다 · classdiagram 1.34).
 *
 * 루프: T1 집기 → T2 처리 → 성공이면 같은 키의 다음 행을 바로 이어서 집는다(체인 — 앞 행이 PROCESSED 가 되면 뒤 행이 풀린다).
 * 실패하면 T3 로 기록하고 멈춘다 — 뒤 행은 순서 대기 조건으로 자연히 기다리고, 재시도 시각이 되면 sweeper 가 다시 부른다.
 * 집을 것이 없으면(비었거나 · 앞 행이 처리 중 · 재시도 대기 · FAILED) 바로 끝난다.
 *
 * 적재 커밋 직후(`inbox_drain_trigger`)와 sweeper 가 같은 이 함수 하나를 부른다.
 *
 * T2 는 `inbox_dispatcher` 가 봉투의 행위자 · 테넌트를 MDC 에 넣은 채 커밋한다 — 커밋 flush 의 감사 리스너와 커밋 뒤 발행(봉투 `userId`)이
 * 그 값을 봐야 해서 T2 안에서는 되돌리지 않는다. 그래서 **T2 바깥인 여기서** 앞 값을 저장하고 끝나면 되돌린다(풀 스레드라 다음 행에 새지 않게).
 */
@Component
class inbox_processor(
    private val tx: inbox_tx_service,
) {

    private val log = LoggerFactory.getLogger(inbox_processor::class.java)

    fun drain(key: inbox_key) {
        while (true) {
            val inbox_message_id = tx.claim_next(key) ?: return
            val failure = runCatching { with_restored_mdc { tx.process(inbox_message_id) } }.exceptionOrNull() ?: continue
            runCatching { tx.mark_retry(inbox_message_id, failure) }
                .onFailure {
                    // 실패 기록마저 못 했다(DB 장애 등) — 행은 PROCESSING 으로 남고 임대가 끝나면 sweeper 가 회수한다.
                    log.error("inbox 실패 기록 실패 — 임대 만료 뒤 다시 처리됩니다: id={} topic={}", inbox_message_id, key.topic, it)
                }
            return
        }
    }

    /** T2 가 넣은 봉투 MDC 를 끝난 뒤 앞 값으로 되돌린다. */
    private fun with_restored_mdc(block: () -> Unit) {
        val previous = inbox_dispatcher.MDC_KEYS.associateWith { MDC.get(it) }
        try {
            block()
        } finally {
            previous.forEach { (key, value) -> inbox_dispatcher.put_or_remove(key, value) }
        }
    }
}
```

```kotlin
    /**
     * T2 — 비즈니스 처리와 PROCESSED 를 **한 트랜잭션**으로 커밋한다. 명령 핸들러의 `@Transactional`(REQUIRED)이 여기에 합류한다 —
     * 원무 메시지의 「메시지 하나 = 트랜잭션 하나」(classdiagram 1.28)가 inbox 행의 PROCESSED 와 함께 커밋된다.
     * 예외는 잡지 않고 그대로 던진다 — 여기서 잡으면 rollback-only 인 채 커밋을 시도해 `UnexpectedRollbackException` 이 난다.
     * 시간 제한은 임대(lease)보다 짧고 포트 호출 시간 제한보다 길어야 한다.
     */
    @Transactional(timeoutString = "\${projectx.patient-appointment.inbox.process-timeout-seconds:60}")
    fun process(inbox_message_id: Long) {
        val message = load(inbox_message_id)
        if (message.status in inbox_status.FINISHED) return // 임대 만료 회수 뒤 다른 파드가 먼저 끝냈다(또는 강제 종료됐다)
        dispatcher.dispatch(message)
        message.mark_processed(Instant.now())
    }
```

### 6.6 분배(원문 → 봉투 복원 → 핸들러) — `$B/infrastructure/inbox/inbox_dispatcher.kt:20-72`

```kotlin
/**
 * inbox 행 → 핸들러(classdiagram 1.34). 토픽으로 이벤트 클래스 · 핸들러를 고르고, 원문에서 봉투를 복원해 `handle(envelope)` 을 부른다.
 *
 * - 분배 키는 **토픽**이다 — 토픽 1개 = 수신 이벤트 클래스 1개라서(`RECEIVED_EVENTS`). 봉투 messageType 은 기록만 한다.
 *   받는 토픽마다 핸들러가 있어야 한다 — 빠지면 **부팅이 실패**한다(조용히 못 받는 토픽을 만들지 않는다).
 * - 역직렬화는 라이브러리와 **같은 ObjectMapper 빈**(Boot 의 Jackson 3)으로 한다 — 직접 구독하던 때와 같은 규칙으로 읽는다.
 *   형식이 계약과 다르면 `JacksonException` → 영구 실패(FAILED).
 * - 처리 스레드는 수신 스레드가 아니라 MDC 가 비어 있다 — 봉투의 `userId` · `tenantId` · `hospitalCode` 를 넣는다
 *   (감사 컬럼의 직원 id · 커밋 뒤 발행 봉투의 `userId` 출처 — classdiagram 1.26). 받은 값 그대로 넣는다(대체값 없음).
 *   **여기서 되돌리지 않는다** — 커밋 flush 의 감사 리스너와 커밋 뒤(AFTER_COMMIT) 발행이 T2 가 끝날 때 돌기 때문이다.
 *   되돌리기는 T2 바깥에서 부르는 `inbox_processor` 가 한다([MDC_KEYS] 를 함께 쓴다).
 *
 * `inbox_tx_service.process`(T2) 안에서만 부른다 — 명령 핸들러의 트랜잭션이 T2 에 합류한다.
 */
@Component
class inbox_dispatcher(
    employee_schedule_handler: erp_employee_schedule_handler,
    order_based_handler: order_based_appointment_handler,
    appointment_request_handler: appointment_request_handler,
    private val object_mapper: ObjectMapper,
) {

    private val handlers: Map<KClass<out integration_event>, idempotent_integration_event_handler<*>> = mapOf(
        erp_employee_schedule_integration_event::class to employee_schedule_handler,
        order_based_appointment_request_integration_event::class to order_based_handler,
        appointment_request_integration_event::class to appointment_request_handler,
    )

    private val routes: Map<String, received_event> = RECEIVED_EVENTS.associateBy { it.topic }

    init {
        val missing = RECEIVED_EVENTS.filter { it.event_type !in handlers }.map { it.topic }
        check(missing.isEmpty()) { "받는 토픽에 핸들러가 없습니다 — inbox_dispatcher 에 더해야 합니다: $missing" }
    }

    fun dispatch(message: inbox_message) {
        val route = requireNotNull(routes[message.topic]) {
            "받는 토픽이 아닙니다 — 분배할 이벤트 클래스가 없습니다: topic=${message.topic}"
        }
        val envelope_type = object_mapper.typeFactory.constructParametricType(MessageEnvelope::class.java, route.event_type.java)
        val envelope: MessageEnvelope<integration_event> = object_mapper.readValue(message.payload, envelope_type)
        @Suppress("UNCHECKED_CAST")
        val handler = handlers.getValue(route.event_type) as idempotent_integration_event_handler<integration_event>
        put_envelope_mdc(envelope)
        handler.handle(envelope)
    }

    /** 봉투의 행위자 · 테넌트를 MDC 에 넣는다 — 되돌리지 않는다(위 설명). */
    private fun put_envelope_mdc(envelope: MessageEnvelope<*>) {
        put_or_remove(JwtKeys.USER_ID, envelope.userId)
        put_or_remove(JwtKeys.TENANT_ID, envelope.tenantId)
        put_or_remove(JwtKeys.HOSPITAL_CODE, envelope.hospitalCode)
    }
```

### 6.7 핸들러 베이스 클래스 — `$B/application/integrationevents/eventhandling/idempotent_integration_event_handler.kt:9-44`

```kotlin
/**
 * 통합 이벤트 핸들러 베이스. 봉투를 풀어 payload 와 **테넌트 범위**를 넘기고, 수신 로깅 + 예외 래핑을 제공한다.
 *
 * 테넌트 범위는 여기서 한 번 정한다(`event_tenant_resolver` — 봉투 `tenantId` · `hospitalCode` vs 배포값). 하위 핸들러는 받은 범위로만
 * 찾고 만든다(명령의 `scope`). 감사 컬럼의 직원 ID 는 eventbus 가 처리 중 MDC `userId` 에 넣어 둔 봉투 값을 영속성 리스너가 읽는다 —
 * 여기서 넘기지 않는다.
 *
 * **멱등성은 도메인/Repository 에 위임**한다(자연키 · source_event_version · 날짜 비교) — `consumed_message` 테이블은 두지 않는다.
 * 예외는 그대로 올려 inbox 의 재시도 · FAILED 로 남긴다.
 */
abstract class idempotent_integration_event_handler<T : integration_event>(
    private val tenant_resolver: event_tenant_resolver,
) {

    protected val logger: Logger = LoggerFactory.getLogger(this::class.java)

    /** 구독 진입점 — 봉투에서 테넌트 범위를 정해 [handle] 로 넘긴다. */
    open fun handle(envelope: MessageEnvelope<T>) {
        val scope = tenant_resolver.resolve(envelope.tenantId, envelope.hospitalCode, envelope.messageId.toString())
        handle(envelope.payload, scope)
    }

    // open — @Transactional 빈은 CGLIB 프록시 대상이라 final 메서드는 프록시 경고를 유발한다.
    open fun handle(event: T, scope: tenant_scope) {
        val event_type_name = event::class.simpleName
        logger.info("통합 이벤트 수신: type={} tenant={}/{}", event_type_name, scope.tenant_id, scope.hospital_code)
        try {
            handle_internal(event, scope)
        } catch (ex: Exception) {
            logger.error("통합 이벤트 처리 실패: type={} 사유={}", event_type_name, ex.message, ex)
            throw ex
        }
    }

    protected abstract fun handle_internal(event: T, scope: tenant_scope)
}
```

수신 테넌트 결정 — `$B/application/integrationevents/eventhandling/event_tenant_resolver.kt:8-50`:

```kotlin
/**
 * 수신 이벤트의 테넌트 범위를 정한다 — 봉투(MessageEnvelope)의 `tenantId` · `hospitalCode` 와 배포값을 맞춰 본다.
 *
 * 규칙(테넌트 · 병원 각각):
 * - 봉투 값이 배포값과 맞으면(대소문자 무시) 배포값을 쓴다 — 표기를 배포값 하나로 맞춘다.
 * - **다르면 경고 로그만 남기고 배포값으로 적재한다** — 실패로 보지 않는다. 토픽 자체가 병원별(`{환경}.{병원}.…`)이라
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

    private val log = LoggerFactory.getLogger(event_tenant_resolver::class.java)

    fun resolve(envelope_tenant_id: String?, envelope_hospital_code: String?, message_id: String): tenant_scope {
        val tenant_id = resolve_tenant(envelope_tenant_id?.trim()?.takeIf { it.isNotEmpty() }, message_id)
        val hospital_code = resolve_hospital(envelope_hospital_code?.trim()?.takeIf { it.isNotEmpty() }, message_id)
        return tenant_scope.of(tenant_id, hospital_code)
    }

    private fun resolve_tenant(from_envelope: String?, message_id: String): String? {
        val deployed = deployment.tenant_id() ?: return from_envelope
        if (from_envelope != null && !from_envelope.equals(deployed, ignoreCase = true)) {
            log.warn("수신 이벤트 테넌트가 배포값과 다르다 — 배포값으로 적재한다: messageId={} 봉투={} 배포={}", message_id, from_envelope, deployed)
        }
        return deployed
    }

    private fun resolve_hospital(from_envelope: String?, message_id: String): String? {
        val deployed = deployment.hospital_codes()
        if (deployed.isEmpty()) return from_envelope
        if (from_envelope == null) return deployed.first()
        val matched = deployed.firstOrNull { it.equals(from_envelope, ignoreCase = true) }
        if (matched != null) return matched
        log.warn("수신 이벤트 병원이 배포값에 없다 — 배포 기본 병원으로 적재한다: messageId={} 봉투={} 배포={}", message_id, from_envelope, deployed)
        return deployed.first()
    }
}
```

### 6.8 수신 payload 클래스(`@MessageType` · `@SchemaVersion` · `@Aggregate`) — `$B/application/integrationevents/events/erp_employee_schedule_integration_event.kt:8-52`

```kotlin
/**
 * ERP **직원 일정** 수신 — 학회 · 연차를 외부 가용 상태 태그로 싣는다. (`patient-appointment.api.md` §9-b · classdiagram 1.17)
 *
 * ⚠ **임시 계약이다** — ERP 명세가 없어 이 BC 가 토픽 · `@MessageType` · 필드를 정했다. ERP 명세가 오면 거기에 맞춘다(Conformist).
 * 생성 · 변경 · 취소가 **한 토픽 · 한 messageType · 한 클래스**로 흐르고 셋의 구분은 [eventType] 이 한다.
 *
 * 필드는 cross-BC JSON 정합을 위해 **camelCase** 이고, 역직렬화가 기본 생성자 + 필드 주입으로 돌도록 전부 `var` + 기본값이다.
 */
@MessageType("ErpEmployeeSchedule")
@SchemaVersion("1")
@Aggregate(type = "EmployeeSchedule")
class erp_employee_schedule_integration_event : integration_event() {

    /** ERP 일정 id — 자연키 · **파티션 키**. */
    var employeeScheduleId: String = ""

    /** `CREATED` · `MODIFIED` · `CANCELLED`. */
    var eventType: String = ""

    /** 직원 = 사용자 계정 BC 의 계정 `id`(= `schedule.actor_id`). */
    var employeeId: String = ""

    /** 사번 — 표시 · 대조용. */
    var employeeNumber: String? = null

    /** `CONFERENCE`(학회) · `ANNUAL_LEAVE`(연차) — 그 밖의 값은 받지 않는다. */
    var scheduleTypeCode: String? = null

    /** `YYYY-MM-DD`(양끝 포함). */
    var startDate: String = ""
    var endDate: String = ""

    var description: String? = null

    /** 같은 일정의 변경 순번 — 같거나 낮으면 무시한다. */
    var sourceEventVersion: Long = 0

    companion object {
        /** 토픽 `{환경}.{병원코드}.employee-schedule` 의 **namespace 뒤 마디**(이벤트명). */
        const val TOPIC = "employee-schedule"

        /** [employeeScheduleId] 의 payload 필드명 — `topic_registry` 의 partitionKeyField. */
        const val PARTITION_KEY_FIELD = "employeeScheduleId"
    }
}
```

### 6.9 구체 핸들러 + 매퍼(payload → command) — `$B/application/integrationevents/eventhandling/erp_employee_schedule_handler.kt:11-52`

별도 매퍼 클래스는 없다 — `handle_internal` 안에서 payload 를 명령 1건으로 바꾸고 `command_bus` 로 보낸다(id · 날짜 해석은 `inbound_ids`).

```kotlin
/**
 * ERP 직원 일정 수신 핸들러 — payload → 명령 하나(`apply_employee_schedule`). (classdiagram 1.17 · §3.5)
 *
 * 구독은 `event_bus_subscription_initializer` 가 `projectx.patient-appointment.event-bus.enabled = true` 일 때만 등록한다.
 * 멱등은 도메인이 한다(ERP 일정 id · `sourceEventVersion`). 형식 오류는 예외로 올려 inbox 에 바로 FAILED 로 남긴다(영구 실패 · classdiagram 1.34).
 * **직원 id 가 UUID 가 아니면 경고만 남기고 건너뛴다**(실패로 보지 않는다) — 의사 줄(shared 직원 uuid)과 이을 수 없는 일정이라
 * 다시 받아도 결과가 같다. 실패로 올리면 FAILED 행만 쌓인다. 사번(`employeeNumber`)은 계약이라 받지만 저장하지 않는다.
 * 트랜잭션 경계는 명령 핸들러(`apply_employee_schedule_command_handler`)다 — 여기서 `@Transactional` 을 붙여도
 * 베이스의 `handle` 이 같은 빈 안에서 부르는 호출이라 프록시를 타지 않는다.
 */
@Component
class erp_employee_schedule_handler(
    private val bus: command_bus,
    tenant_resolver: event_tenant_resolver,
) : idempotent_integration_event_handler<erp_employee_schedule_integration_event>(tenant_resolver) {

    override fun handle_internal(event: erp_employee_schedule_integration_event, scope: tenant_scope) {
        require(event.employeeScheduleId.isNotBlank()) { "employeeScheduleId 가 비어 있습니다" }
        require(event.employeeId.isNotBlank()) { "employeeId 가 비어 있습니다" }
        if (!inbound_ids.is_uuid(event.employeeId)) {
            logger.warn(
                "직원 id 가 UUID 가 아니어서 ERP 직원 일정을 건너뜁니다 — employeeScheduleId={}",
                event.employeeScheduleId,
            )
            return
        }
        val result: employee_schedule_apply_result = bus.send(
            scope,
            apply_employee_schedule_command(
                erp_schedule_id = event.employeeScheduleId,
                event_type = employee_schedule_event_type.from(event.eventType),
                employee_id = inbound_ids.required(event.employeeId, "employeeId"),
                schedule_type_code = event.scheduleTypeCode,
                during_start = inbound_ids.date_of(event.startDate, "startDate"),
                during_end = inbound_ids.date_of(event.endDate, "endDate"),
                description = event.description,
                source_event_version = event.sourceEventVersion,
            ),
        )
        logger.info("ERP 직원 일정 이벤트 처리 — employeeScheduleId={} 결과={}", event.employeeScheduleId, result)
    }
}
```

id · 날짜 해석 규칙 — `$B/application/integrationevents/eventhandling/inbound_ids.kt:13-22`:

```kotlin
/**
 * 수신 payload 의 id → shared uuid(classdiagram 1.27 · §Integration Events) · 날짜 문자열 → LocalDate.
 *
 * - **환자 · 수진은 필수**다 — 없거나 UUID 가 아니면 거절한다(예외 → inbox 행 FAILED). 값은 메시지에 싣지 않는다(환자 식별자).
 * - 진료과 · 진료의 같은 **부가 id 는 UUID 가 아니면 경고만 남기고 null** 이다 — 예약 자체는 만든다
 *   (처방의가 없으면 접수 묶음 키가 비어 접수 없이 등록된다).
 * - 날짜는 `YYYY-MM-DD` 다 — 형식이 틀리면 거절한다(FAILED). 자동 예약 생성 요청만 시각이 붙은 값도 받는다([date_of] 의 `allow_datetime`).
 * - payload 필드명 · 토픽은 계약이라 바꾸지 않는다 — 값의 해석만 이 파일이 정한다.
 */
internal object inbound_ids {
```

### 6.10 발행 측 전체 — 접수 요청(`EncounterRequest`)

포트 어댑터(도메인 이벤트로 바꿔 싣는다) — `$B/infrastructure/eventbus/encounter_request_event_adapter.kt:12-37`:

```kotlin
/**
 * 접수 요청 포트 어댑터. (classdiagram 1.28 · 부록 P92)
 *
 * 요청을 **바로 보내지 않는다** — 환자마다 `encounter_requested_domain_event` 1건으로 같은 트랜잭션에 실어 두고,
 * `domain_event_integration_publisher` 가 `@TransactionalEventListener(AFTER_COMMIT)` 로 받아 Kafka 로 보낸다.
 * 그래서 롤백된 예약 등록의 요청은 나가지 않는다.
 * 켜짐 여부는 수신 · 예약 이벤트 발행과 같은 스위치(`projectx.patient-appointment.event-bus.enabled`)다.
 */
@Component
class encounter_request_event_adapter(
    private val dispatcher: domain_event_dispatcher,
    private val clock: Clock,
    @Value("\${projectx.patient-appointment.event-bus.enabled:true}") private val enabled: Boolean,
) : i_encounter_request_port {

    override fun is_enabled(): Boolean = enabled

    /** 요청을 환자마다 묶어 도메인 이벤트 1건씩 싣는다 — 메시지 1건 = 환자 1명(파티션 키)이고 요청은 배열 항목이다. 넘긴 순서를 지킨다. */
    override fun request(scope: tenant_scope, requests: List<encounter_request>) {
        if (requests.isEmpty()) return
        val at = clock.instant()
        requests.groupBy { it.patient_id }.forEach { (patient_id, of_patient) ->
            dispatcher.publish(encounter_requested_domain_event(patient_id, of_patient, scope, at))
        }
    }
}
```

도메인 이벤트 — `$B/domain/events/appointment_domain_events.kt:121-128`:

```kotlin
data class encounter_requested_domain_event(
    /** 환자 — 발행 메시지의 파티션 키. [requests] 전부가 이 환자의 것이다. */
    val patient_id: java.util.UUID,
    /** 그 환자의 접수 요청(묶음마다 1건 · 1건 이상) — 메시지 하나의 배열로 나간다. */
    val requests: List<com.projectx.patientappointment.domain.ports.encounter_request>,
    val scope: tenant_scope,
    val occurred_at: Instant,
) : domain_event
```

통합 이벤트 클래스 — `$B/application/integrationevents/events/encounter_request_integration_events.kt:13-49`:

```kotlin
/**
 * 접수 요청 발행 — 예약 등록 · 이동이 묶음(같은 방문)마다, 예약 취소가 취소한 예약마다 원무(patient-visit)에 접수를 요청한다(부록 P123).
 * (`patient-appointment.api.md` §9-e · classdiagram 1.28 · 부록 P92)
 *
 * ⚠ **이 BC 가 정한 임시 계약이다** — 토픽 `{환경}.{병원코드}.encounter-request` 하나에 생성 · 수정 · 취소가 흐르고
 * 항목의 `event_type` 이 가른다. 한 번의 예약 등록이 묶음을 여럿 만들 수 있어 **환자마다 메시지 1건에 요청을 배열([encounter_requests])로** 싣는다.
 * 파티션 키 = 최상위 [patient_id] 라 같은 환자(같은 접수 포함)의 순서가 뒤집히지 않는다. **payload 필드는 snake_case** 이고 id 값은 전부 uuid 다
 * (예약 id 만 옛 예약이 ULID 일 수 있다). 이 BC 안의 요청 id 는 싣지 않는다 — 원무는 [encounter_request_item.appointments] 의 예약 id 로 같은 접수를 찾는다.
 */
@MessageType("EncounterRequest")
@SchemaVersion("3")
@Aggregate(type = "EncounterRequest")
class encounter_request_integration_event(
    /** 환자 — **파티션 키**. [encounter_requests] 전부가 이 환자의 것이다. */
    val patient_id: String,
    /** 접수 요청 — 묶음(같은 방문)마다 1건 · 1건 이상. 원무는 항목마다 따로 처리하고 회신한다. */
    val encounter_requests: List<encounter_request_item>,
) : integration_event() {

    override fun toString(): String =
        "encounter_request_integration_event(patient_id=$patient_id, encounter_requests=$encounter_requests)"

    companion object {
        /** 토픽 `{환경}.{병원코드}.encounter-request` 의 namespace 뒤 마디. */
        const val TOPIC = "encounter-request"

        /** [patient_id] 의 payload 필드명 — `topic_registry` 의 partitionKeyField. */
        const val PARTITION_KEY_FIELD = "patient_id"

        /** 환자 1명의 요청을 메시지 1건으로 — 다른 환자의 요청이 섞이면 파티션 키가 틀리므로 거절한다. */
        fun of(patient_id: UUID, requests: List<encounter_request>): encounter_request_integration_event {
            require(requests.isNotEmpty()) { "접수 요청이 없습니다 — patient_id=$patient_id" }
            require(requests.all { it.patient_id == patient_id }) { "다른 환자의 접수 요청이 섞여 있습니다 — patient_id=$patient_id" }
            return encounter_request_integration_event(
                patient_id = patient_id.toString(),
                encounter_requests = requests.map { encounter_request_item.of(it) },
            )
        }
    }
}
```

publisher(AFTER_COMMIT) — `$B/infrastructure/eventbus/domain_event_integration_publisher.kt:95-110`:

```kotlin
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun on_encounter_requested(event: encounter_requested_domain_event) {
        runCatching {
            val payload = encounter_request_integration_event.of(event.patient_id, event.requests)
            if (!enabled) {
                log.info("접수 요청 조립 완료(발행 꺼짐) — {}", payload)
                return
            }
            event_actor.publish("EncounterRequest") { event_bus.getObject().publish(payload, aggregateId = payload.patient_id) }
            log.info("접수 요청을 발행했습니다 — {}", payload)
        }.onFailure {
            log.warn(
                "접수 요청 발행에 실패했습니다(예약은 requested 로 남는다 · 다시 보내지 않는다) — patient_id={} appointment_ids={} 사유={}",
                event.patient_id, event.requests.map { request -> request.appointments.map { it.appointment_id } }, it.message,
            )
        }
    }
```

배선 없는 이벤트(로그만) — `$B/application/integrationevents/events/outbound_integration_events.kt:5-19`:

```kotlin
/**
 * 아웃바운드 통합 이벤트. (`patient-appointment.api.md` §9)
 *
 * payload 필드는 cross-BC JSON 정합을 위해 **camelCase**(snake_case 규칙의 유일한 예외).
 *
 * ⚠ 토픽은 **`{환경}.{테넌트id}.{이벤트명}`** — 발행 모듈명은 토픽에 넣지 않는다. 이 파일의 이벤트(슬롯 · 휴무 · 재배정 · 접수 부착)는
 * 아직 배선이 없어 이벤트명을 정하지 않았다(배선할 때 정한다). **행위(Booked/Cancelled)는 토픽에 넣지 않는다** —
 * 한 이벤트의 전이가 같은 토픽으로 흘러야 `partitionKeyField` 기준 순서가 보장된다.
 * 이벤트 구분은 봉투의 `@MessageType`(PascalCase)이 한다.
 *
 * ⚠ 실제 발행에는 payload 클래스에 `@MessageType` / `@SchemaVersion` / `@Aggregate` 가 필요하다 —
 * 누락되면 `EnvelopeFactory` 가 봉투를 채우지 못해 발행이 **500** 으로 실패한다.
 * 이 MS 는 아직 브로커에 붙지 않아 애노테이션을 달지 않았다(→ `domain_event_integration_publisher`).
 */
```

---

## 7. 예외 → HTTP

### 7.1 도메인 예외 체계 — `$B/domain/exceptions/appointment_exceptions.kt:3-62`

베이스 6종이 HTTP 상태 1개씩에 대응한다. 404 · 409 · 400 · 403 · 502 는 **구체 클래스가 아니라 팩토리 함수**(`fun xxx_exception(...) = appointment_not_found_exception(code = ..., ...)`)로 만들고, 422 만 `appointment_domain_exception` 을 상속한 구체 클래스다.

```kotlin
/**
 * 통합예약 BC 예외 체계 — `_docs/design/patient-appointment.api.md` §8 의 code ↔ HTTP 표와 1:1 이다.
 * `code` 는 `OperationOutcome.issue[].details.coding[0].code` 로 나간다.
 *
 * | 예외 베이스                         | HTTP | issue.code    |
 * |-------------------------------------|------|---------------|
 * | `appointment_not_found_exception`   | 404  | not-found     |
 * | `appointment_validation_exception`  | 400  | invalid       |
 * | `appointment_domain_exception`      | 422  | business-rule |
 * | `appointment_conflict_exception`    | 409  | conflict      |
 * | `appointment_forbidden_exception`   | 403  | forbidden     |
 * | `appointment_upstream_exception`    | 502  | exception     |
 *
 * `details` 는 화면이 사유를 설명할 수 있도록 담는 부가 정보다 — **PII / 내부 스택은 담지 않는다**.
 * `expression` 은 FHIR `issue.expression` 으로 나가는 대상 리소스 경로다.
 */

/** 불변식 위반 (HTTP 422). */
abstract class appointment_domain_exception(
    val code: String,
    message: String,
    val details: Map<String, Any?> = emptyMap(),
    val expression: List<String> = emptyList(),
) : RuntimeException(message)

/** 리소스 없음 (HTTP 404). */
class appointment_not_found_exception(
    val code: String,
    message: String,
    val details: Map<String, Any?> = emptyMap(),
) : RuntimeException(message)

/** 동시성 / 중복 (HTTP 409). */
class appointment_conflict_exception(
    val code: String,
    message: String,
    val details: Map<String, Any?> = emptyMap(),
    val expression: List<String> = emptyList(),
) : RuntimeException(message)

/** 입력 검증 실패 (HTTP 400). */
class appointment_validation_exception(
    message: String,
    val details: Map<String, Any?> = emptyMap(),
) : RuntimeException(message)

/** 권한 위반 (HTTP 403). */
class appointment_forbidden_exception(
    val code: String,
    message: String,
    val details: Map<String, Any?> = emptyMap(),
) : RuntimeException(message)

/** 외부 모듈 호출 실패 (HTTP 502). */
class appointment_upstream_exception(
    val code: String,
    message: String,
    val details: Map<String, Any?> = emptyMap(),
) : RuntimeException(message)

// ---------------------------------------------------------------------------
// 404
// ---------------------------------------------------------------------------

fun schedule_not_found_exception(schedule_id: String) =
    appointment_not_found_exception(
        code = "NOT_FOUND",
        message = "스케줄을 찾을 수 없습니다",
        details = mapOf("scheduleId" to schedule_id),
    )
```

§3 체인에서 쓰인 422 예외 — `:444-450`, `:502-507`:

```kotlin
class slot_not_bookable_domain_exception(code: String, slot_id: String, reason: String) :
    appointment_domain_exception(
        code = code,
        message = reason,
        details = mapOf("slotId" to slot_id),
        expression = listOf("Slot/$slot_id"),
    )
```

```kotlin
class bad_state_domain_exception(reason: String, current_status: String) :
    appointment_domain_exception(
        code = "NOT_BOOKED",
        message = reason,
        details = mapOf("status" to current_status),
    )
```

### 7.2 `@RestControllerAdvice` — `$B/infrastructure/config/global_exception_handler.kt`

응답 모양(`application/problem+json` + FHIR `OperationOutcome.issue[]`) — `:20-51`:

```kotlin
/**
 * 표준 에러 응답 — `application/problem+json` + FHIR `OperationOutcome` 호환 `issue[]`.
 * (설계문서 삭제 — 시안 `_docs/view/` 참조)
 *
 * 화면은 `issue[].details.coding[].code` 로 분기한다 — 문구가 아니라 코드로 판단해야
 * 메시지를 다듬을 때 화면이 깨지지 않는다. 내부 stacktrace · PII 는 담지 않는다.
 */
data class error_response(
    val type: String = "about:blank",
    val title: String,
    val status: Int,
    val traceId: String?,
    val issue: List<problem_issue>,
    val details: Map<String, Any?> = emptyMap(),
)

data class problem_issue(
    val severity: String = "error",
    /** FHIR `issue.code`. */
    val code: String,
    val details: problem_coding,
    val diagnostics: String?,
    val expression: List<String> = emptyList(),
)

data class problem_coding(
    val coding: List<Map<String, String>>,
) {
    companion object {
        fun of(code: String): problem_coding = problem_coding(listOf(mapOf("code" to code)))
    }
}
```

매핑 규칙 요약 + 핸들러 — `:53-108`:

```kotlin
/**
 * 전역 예외 → HTTP 매핑.
 * - 401 · 403 인증 실패(`@RequireAuth`) — 코드는 라이브러리 에러코드(`AUTH_TOKEN_MISSING` · `AUTH_TOKEN_EXPIRED` …) (`login` · `forbidden`)
 * - 404 `RESOURCE_NOT_FOUND` (`issue.code = not-found`)
 * - 400 `VALIDATION_ERROR` (`invalid`)
 * - 403 `SERVICE_ITEM_NOT_ALLOWED` (`forbidden`) — 역할 판정 `ROLE_NOT_ALLOWED` 는 422. 메뉴 접근은 플랫폼 RBAC 이 제어한다
 * - 409 `VERSION_CONFLICT`(If-Match 불일치) / `OPTIMISTIC_LOCK_CONFLICT`(@Version 경합) /
 *       `SCHEDULE_ALREADY_EXISTS` / `SLOT_DUPLICATE` … (`conflict`)
 * - 422 `CAPACITY_EXCEEDS_MAX` / `RULE_CYCLE` / `PHASE_CHAIN_INCOMPLETE` … (`business-rule`)
 */
@RestControllerAdvice
class global_exception_handler {

    private val log = org.slf4j.LoggerFactory.getLogger(global_exception_handler::class.java)

    /**
     * 401 · 403 — 인증 실패(`@RequireAuth` · `@TokenPrincipal`). 상태 · 코드는 라이브러리 에러코드가 정한다.
     * `issue[].details.coding[0].code` 에 wire 코드(`AUTH_TOKEN_MISSING` · `AUTH_TOKEN_EXPIRED` …)를 실어 화면이 `AUTH_` 접두로 분기할 수 있게 한다.
     */
    @ExceptionHandler(JwtAuthenticationException::class)
    fun handle_authentication(ex: JwtAuthenticationException): ResponseEntity<error_response> {
        log.info("인증 실패: {}", ex.errorCode)
        val status = ex.errorCode.status
        val issue_code = if (status == HttpStatus.FORBIDDEN) "forbidden" else "login"
        return problem(status, issue_code, ex.errorCode.name, ex.errorCode.defaultMessage)
    }

    @ExceptionHandler(appointment_not_found_exception::class)
    fun handle_not_found(ex: appointment_not_found_exception): ResponseEntity<error_response> =
        problem(HttpStatus.NOT_FOUND, "not-found", ex.code, ex.message ?: "리소스를 찾을 수 없습니다", ex.details)

    @ExceptionHandler(appointment_domain_exception::class)
    fun handle_domain_rule(ex: appointment_domain_exception): ResponseEntity<error_response> =
        problem(
            HttpStatus.UNPROCESSABLE_ENTITY,
            "business-rule",
            ex.code,
            ex.message ?: "도메인 규칙을 위반했습니다",
            ex.details,
            ex.expression,
        )

    @ExceptionHandler(appointment_conflict_exception::class)
    fun handle_conflict(ex: appointment_conflict_exception): ResponseEntity<error_response> =
        problem(
            HttpStatus.CONFLICT,
            "conflict",
            ex.code,
            ex.message ?: "리소스 충돌이 발생했습니다",
            ex.details,
            ex.expression,
        )

    @ExceptionHandler(appointment_forbidden_exception::class)
    fun handle_forbidden(ex: appointment_forbidden_exception): ResponseEntity<error_response> =
        problem(HttpStatus.FORBIDDEN, "forbidden", ex.code, ex.message ?: "허용되지 않은 요청입니다", ex.details)
```

낙관락 · DB 제약명 → 코드 변환 — `:118-160`:

```kotlin
    /**
     * JPA 낙관락(`@Version`) 경합 — 행 락을 쓰지 않으므로 **이것이 유일한 동시성 장치**다.
     * 같은 슬롯 · 같은 스케줄 · 같은 진료시간 기준을 둘이 같은 순간 저장하면 한쪽이 여기로 온다. 화면은 다시 읽고 다시 저장한다.
     */
    @ExceptionHandler(OptimisticLockingFailureException::class)
    fun handle_optimistic_lock(ex: OptimisticLockingFailureException): ResponseEntity<error_response> =
        problem(
            HttpStatus.CONFLICT,
            "conflict",
            "OPTIMISTIC_LOCK_CONFLICT",
            "다른 사용자가 먼저 변경했습니다. 다시 조회 후 시도하세요",
        )

    /**
     * 중복 키 위반 — DB 제약명을 화면이 읽을 코드로 옮긴다.
     *
     * 여기까지 온다는 것은 **애그리게잇 검사를 지나친 동시 경합**이라는 뜻이다(단일 요청의 중복은
     * 도메인이 먼저 막는다). 제약명을 그대로 내보내면 화면이 스키마에 묶이므로 코드로 바꾼다.
     */
    @ExceptionHandler(DataIntegrityViolationException::class)
    fun handle_integrity(ex: DataIntegrityViolationException): ResponseEntity<error_response> {
        val detail = ex.mostSpecificCause.message.orEmpty()
        return when {
            detail.contains("ux_schedule_actor_category") || detail.contains("ux_schedule_actor_id") ->
                problem(
                    HttpStatus.CONFLICT,
                    "conflict",
                    "SCHEDULE_ALREADY_EXISTS",
                    "같은 대상·구분의 스케줄이 이미 있습니다",
                )
            // 같은 시각·같은 대상 3축의 슬롯은 하나뿐이다 — `activate` 동시 실행이 여기 걸린다.
            detail.contains("ux_slot_schedule_start_audience") ->
                problem(HttpStatus.CONFLICT, "conflict", "SLOT_DUPLICATE", "같은 시각·대상의 슬롯이 이미 있습니다")
            // 행 목록을 통째로 갈아끼웠을 때 나는 대표 증상이다(제자리 갱신이면 나지 않는다).
            detail.contains("ux_pattern_detail_natural") ->
                problem(HttpStatus.CONFLICT, "conflict", "DETAIL_DUPLICATE", "같은 일차·시각·대상의 행이 이미 있습니다")
            detail.contains("ux_schedule_pattern_name") ->
                problem(HttpStatus.CONFLICT, "conflict", "PATTERN_NAME_DUPLICATE", "같은 이름의 패턴이 이미 있습니다")
            detail.contains("ux_appointment_phase_seq") ->
                problem(HttpStatus.CONFLICT, "conflict", "PHASE_DUPLICATE", "같은 단계 번호가 이미 있습니다")
            else -> problem(HttpStatus.CONFLICT, "conflict", "CONSTRAINT_VIOLATION", "데이터 제약 조건을 위반했습니다")
        }
    }
```

400 계열과 응답 조립 — `:162-205`:

```kotlin
    @ExceptionHandler(appointment_validation_exception::class)
    fun handle_validation(ex: appointment_validation_exception): ResponseEntity<error_response> =
        problem(HttpStatus.BAD_REQUEST, "invalid", "VALIDATION_ERROR", ex.message ?: "요청 값이 유효하지 않습니다", ex.details)

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handle_bean_validation(ex: MethodArgumentNotValidException): ResponseEntity<error_response> {
        val fields = ex.bindingResult.fieldErrors.associate { it.field to (it.defaultMessage ?: "유효하지 않은 값") }
        val message = fields.entries.joinToString(", ") { "${it.key}: ${it.value}" }
            .ifBlank { "요청 값이 유효하지 않습니다" }
        return problem(HttpStatus.BAD_REQUEST, "invalid", "VALIDATION_ERROR", message, mapOf("fields" to fields))
    }

    /** 코드 파싱 실패(`from_code`)도 여기로 온다 — 알 수 없는 FHIR 코드는 400 이다. */
    @ExceptionHandler(IllegalArgumentException::class)
    fun handle_illegal_argument(ex: IllegalArgumentException): ResponseEntity<error_response> =
        problem(HttpStatus.BAD_REQUEST, "invalid", "VALIDATION_ERROR", ex.message ?: "요청 값이 유효하지 않습니다")

    private fun problem(
        status: HttpStatus,
        issue_code: String,
        code: String,
        message: String,
        details: Map<String, Any?> = emptyMap(),
        expression: List<String> = emptyList(),
    ): ResponseEntity<error_response> = ResponseEntity.status(status)
        .contentType(MediaType.APPLICATION_PROBLEM_JSON)
        .body(
            error_response(
                title = message,
                status = status.value(),
                traceId = trace_id(),
                issue = listOf(
                    problem_issue(
                        code = issue_code,
                        details = problem_coding.of(code),
                        diagnostics = message,
                        expression = expression,
                    ),
                ),
                details = details,
            ),
        )

    /** trace_id 는 게이트웨이/observability 가 MDC 에 넣어 준다. */
    private fun trace_id(): String? = MDC.get("traceId") ?: MDC.get("trace_id")
}
```

### 7.3 inbox 운영 API 전용 advice(`{ error, message }` · 다른 BC 와 같은 본문) — `$B/presentation/inbox_error_advice.kt:19-56`, `:77-95`

```kotlin
/**
 * 수신 inbox 운영 API(classdiagram 1.35)의 오류 응답 — **inbox 를 쓰는 모든 서비스가 같은 본문 · 같은 코드**를 낸다(x-diagnostic-result 와 같은 파일).
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
 * | 500 | `InternalError` — 고정 문구(원인은 로그로만) |
 */
@RestControllerAdvice(assignableTypes = [inbox_messages_controller::class])
@Order(Ordered.HIGHEST_PRECEDENCE)
class inbox_error_advice {

    private val log = LoggerFactory.getLogger(inbox_error_advice::class.java)

    @ExceptionHandler(JwtAuthenticationException::class)
    fun handle_authentication(ex: JwtAuthenticationException): ResponseEntity<inbox_error_response> =
        respond(ex.errorCode.status, ex.errorCode.name, ex.errorCode.defaultMessage)

    @ExceptionHandler(appointment_not_found_exception::class)
    fun handle_not_found(ex: appointment_not_found_exception): ResponseEntity<inbox_error_response> =
        respond(HttpStatus.NOT_FOUND, NOT_FOUND, ex.message)

    @ExceptionHandler(appointment_conflict_exception::class)
    fun handle_conflict(ex: appointment_conflict_exception): ResponseEntity<inbox_error_response> =
        respond(HttpStatus.CONFLICT, CONFLICT, ex.message)

    @ExceptionHandler(appointment_validation_exception::class, IllegalArgumentException::class)
    fun handle_validation(ex: Exception): ResponseEntity<inbox_error_response> =
        respond(HttpStatus.BAD_REQUEST, VALIDATION, ex.message)
```

```kotlin
    private fun respond(status: HttpStatusCode, error: String, message: String?): ResponseEntity<inbox_error_response> {
        if (status.is4xxClientError) log.info("inbox 운영 API {}: {}", error, message)
        return ResponseEntity.status(status).body(inbox_error_response(error, message ?: DEFAULT_MESSAGE))
    }

    companion object {
        const val VALIDATION = "Validation"
        const val NOT_FOUND = "NotFound"
        const val CONFLICT = "Conflict"
        const val INTERNAL_ERROR = "InternalError"
        private const val DEFAULT_MESSAGE = "요청을 처리하지 못했습니다"
    }
}

/** 운영 API 오류 본문 — `error` = 카테고리 코드, `message` = 사용자에게 보일 사유. */
data class inbox_error_response(
    val error: String,
    val message: String,
)
```

---

## 8. 설정 · 부팅

### 8.1 메인 클래스 — `$B/patient_appointment_application.kt:1-20`

```kotlin
package com.projectx.patientappointment

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.data.jpa.repository.config.EnableJpaRepositories

/**
 * 통합예약(patient-appointment) 서비스 부트스트랩.
 * - 앱 클래스가 루트 패키지(com.projectx.patientappointment)에 있어 component / entity 스캔이
 *   presentation · application · domain · infrastructure 전체를 커버한다.
 * - JPA Repository(write) 는 infrastructure 패키지에서 스캔한다.
 * - 조회(read)는 QueryDSL(JPAQueryFactory)로 view_model 을 projection 한다(MyBatis 미사용).
 */
@SpringBootApplication
@EnableJpaRepositories(basePackages = ["com.projectx.patientappointment.infrastructure"])
class patient_appointment_application

fun main(args: Array<String>) {
    runApplication<patient_appointment_application>(*args)
}
```

### 8.2 `application.yml` — DB · search_path · Liquibase — `$R/application.yml:9-66`

스키마명은 `${DB_SCHEMA:patient_appointment}` **한 값**에서 ① `connection-init-sql` ② `liquibase.default-schema` ③ `liquibase-schema` 로 퍼진다.

```yaml
server:
  port: ${SERVER_PORT:5352}
  # ⚠️ context-path 를 두지 않는다(= "/"). API prefix `/api/v1/patient-appointment` 는 **컨트롤러가 소유**한다
  #    — BC 표준(정본 x-diagnostic-imaging). 그래야 문서가 prefix 밖의 `/spec/patient-appointment/**` 로 노출되고
  #    actuator 도 `/actuator/health/**`(루트) 로 남아 k8s probe 경로가 단순해진다.

spring:
  # 공통 라이브러리 default 는 common-libs/*.yaml 로 분리 — classpath 에 포장되어 부팅 시 자동 흡수.
  config:
    import:
      - classpath:common-libs/common-masking.yaml
      - classpath:common-libs/common-observability.yaml
  application:
    name: patient-appointment
  # **계정/비밀번호는 기본값을 두지 않는다** — 자격증명을 소스에 남기지 않고, 누락 시 부팅이 즉시 실패해
  #   ".env 를 안 채웠다" 가 바로 드러난다. 로컬은 `backend/.env`, k8s 는 configData + vault Secret.
  datasource:
    # reWriteBatchedInserts — 배치 INSERT 를 드라이버가 다중 VALUES 한 문장으로 다시 써서 보낸다(PostgreSQL 드라이버 전용).
    url: jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5432}/${DB_NAME:sup}?reWriteBatchedInserts=true
    username: ${DB_USER}
    password: ${DB_PASSWORD}
    driver-class-name: org.postgresql.Driver
    hikari:
      # 커넥션 오픈 시마다 실행된다 — search_path 고정만 한다(이 MS 는 자기 스키마만 본다).
      # ⚠️ 여기에 CREATE SCHEMA 를 넣지 않는다(커넥션마다 돌아 pg_namespace 충돌) — 생성은 부팅 시 1회.
      connection-init-sql: SET search_path TO ${DB_SCHEMA:patient_appointment}
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
        # JDBC 배치 — `activate` 가 한 번에 수천 행을 쓴다. 묶지 않으면 행마다 DB 왕복 1회다.
        #   order_* 는 같은 테이블 문장을 모아 배치가 끊기지 않게 한다. 버전 컬럼 행도 배치된다(Hibernate 기본).
        jdbc:
          batch_size: 500
        order_inserts: true
        order_updates: true

  # Liquibase — 스키마 마이그레이션(부팅 시 자동 적용). 샘플 데이터 changeSet 은 없다(예약 기준은 화면의 [+]가 원천 id 로 만든다).
  liquibase:
    change-log: classpath:db/changelog/db.changelog-master.yaml
    # 스키마명은 이 한 값(DB_SCHEMA)에서 나온다 — hikari search_path 와 같은 출처.
    #   하드코딩하면 배포 values 의 configData.DB_SCHEMA 가 조용히 무시된다.
    default-schema: ${DB_SCHEMA:patient_appointment}
    liquibase-schema: ${DB_SCHEMA:patient_appointment}
    # Liquibase 가 넣는 행(002 병원 공통 진료시간 기준)의 테넌트 · 병원 — 배포 테넌트와 같은 값이다.
    #   ⚠️ HOSPITAL_CODES 가 여러 값(콤마)이면 SEED_HOSPITAL_CODE 로 하나를 준다 — 그대로 두면 콤마가 든 값이 병원 코드로 들어간다.
    parameters:
      seed_tenant_id: ${TENANT_ID:INVALID_DATA}
      seed_hospital_code: ${SEED_HOSPITAL_CODE:${HOSPITAL_CODES:INVALID_DATA}}
```

(eventbus 부분은 §6.1 참고. `.env` 로더 등록은 `$R/META-INF/spring.factories:1-4`:)

```properties
# .env 로더(spring-dotenv)를 Boot 4 호환 EnvironmentPostProcessor 로 등록.
# (spring-dotenv 4.0.0 의 SpringApplicationRunListener 자동 등록은 Boot 4 에서 동작하지 않는다 — RunListener 시그니처 변경)
org.springframework.boot.EnvironmentPostProcessor=\
  com.projectx.patientappointment.infrastructure.config.dotenv_environment_post_processor
```

### 8.3 `db_schema_initializer` — `$B/infrastructure/config/db_schema_initializer.kt:11-74`

`BeanPostProcessor` 로 `SpringLiquibase` 빈 **초기화 직전**에 멱등 `CREATE SCHEMA IF NOT EXISTS` 를 1회 실행한다.

```kotlin
/**
 * 이 MS 전용 DB 스키마를 **앱이 부팅하며 직접 생성**한다(멱등 `CREATE SCHEMA IF NOT EXISTS`).
 *
 * 스키마는 MicroService 단위다(루트 CLAUDE.md §0 DB 스키마) — 이 MS 는 `patient_appointment` 하나만 본다.
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
    @Value("\${DB_SCHEMA:patient_appointment}") private val schema_name: String,
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

### 8.4 Liquibase master — `$R/db/changelog/db.changelog-master.yaml:1-30`

`changes/001-create-tables.xml` ~ `020-schedule-session-unit.xml` 20개 파일(디렉터리 `$R/db/changelog/changes/`).

```yaml
# patient-appointment 마스터 changelog.
#
# 스키마는 MicroService 단위다(루트 CLAUDE.md §0 DB 스키마) — 이 MS 전용 스키마는 `patient_appointment`
# (= 모듈명 snake_case), DATABASECHANGELOG 도 그 안에 있다.
# 스키마는 **앱이 부팅하며 직접 만든다**(infrastructure/config/db_schema_initializer — 있으면 그대로 둔다).
# 계정·권한만 DBA 가 선행 생성한다(_docs/db-bootstrap/bootstrap-account-schema.simple.sql) —
# 앱 계정에 DB 의 스키마 생성 권한이 필요하다(부여 구문은 _docs/db-bootstrap/ 스크립트에 있다).
# 테이블·제약 이름은 .claude/docs/rules/conventions.md §A-1 을 따른다(테이블명 = 클래스명, 접두 없음).
#
# ⚠️ **적용된 changeSet 은 고치지 않는다** — 내용이 checksum 이라 주석 한 줄만 바꿔도 이미 적용된 DB 에서
#    `ValidationFailedException: changesets check sum` 으로 **부팅이 막힌다**(컴파일·테스트는 전부 통과한다).
#    스키마를 바꿔야 하면 `changes/00N-*.xml` 을 새로 추가한다. DB 형상을 없애는 변경은 자동 적용하지 않는다(db/manual).
#
# 최초 스키마는 changes/001-create-tables.xml 한 벌이다 — 모든 Entity 테이블이 「PK(<테이블명>_id) → tenant_id · hospital_code
#    → 업무 컬럼 → 감사 4종」 순서를 지키도록 처음부터 만든다. 이전 changelog(001-schedule ~ 029 · 900)로 만든 DB 는
#    수동 트리(manual)의 007-reset-schema.sql 로 사람이 초기화한 뒤 기동한다.
#
# 테넌트 값이 필요한 행(002 운영 마스터)은 Liquibase 파라미터 `seed_tenant_id` · `seed_hospital_code` 를 쓴다
#    (application*.yml 의 spring.liquibase.parameters — 배포 테넌트 TENANT_ID · HOSPITAL_CODES).
# 샘플 데이터 changeSet 은 두지 않는다 — 예약 기준(schedule)은 화면의 [+]가 원천(조직 · 사용자 계정 BC · x-ancillary-main 마스터)의
#    id 로 만든다. 지어낸 의사 · 방을 시드로 넣으면 원천 목록과 이어지지 않는 행이 남는다.
databaseChangeLog:
  # 최초 스키마 — 테이블 · 제약 · 인덱스 전부
  - include:
      file: changes/001-create-tables.xml
      relativeToChangelogFile: true
  # 운영 마스터 — 배포 테넌트의 진료시간 기준 병원 공통 1행(+ 요일 7행). seed 가 아니다(운영에도 필요)
  - include:
      file: changes/002-clinic-hours-hospital-row.xml
      relativeToChangelogFile: true
```

### 8.5 changeSet 1개 — `appointment` 테이블 생성 — `$R/db/changelog/changes/001-create-tables.xml:760-832`

```xml
    <!-- 14. appointment — 예약(Aggregate Root · FHIR Appointment). 환자 신원 · 처방의 · 처방 진료과 이름은 shared 조인으로 읽는다(스냅샷 없음) -->
    <changeSet id="pa-001-create-appointment" author="patient-appointment">
        <createTable tableName="appointment">
            <column name="appointment_id" type="VARCHAR(26)">
                <constraints nullable="false" primaryKey="true" primaryKeyName="pk_appointment"/>
            </column>
            <column name="tenant_id" type="VARCHAR(64)">
                <constraints nullable="false"/>
            </column>
            <column name="hospital_code" type="VARCHAR(64)">
                <constraints nullable="false"/>
            </column>
            <column name="status" type="VARCHAR(24)" defaultValue="PROPOSED">
                <constraints nullable="false"/>
            </column>
            <column name="service_category" type="VARCHAR(32)">
                <constraints nullable="false"/>
            </column>
            <column name="based_on_type" type="VARCHAR(32)"/>
            <!-- 예약 근거 번호 — 처방 id 또는 외래는 환자 id(uuid 문자열). 근거마다 다른 id 라 문자열이다 -->
            <column name="based_on_id" type="VARCHAR(64)"/>
            <!-- 환자 — shared.patient.patient_id -->
            <column name="patient_id" type="UUID">
                <constraints nullable="false"/>
            </column>
            <column name="order_code" type="VARCHAR(64)"/>
            <column name="order_display" type="VARCHAR(200)"/>
            <!-- 처방의 — shared.user_account.user_account_id -->
            <column name="order_requester_id" type="UUID"/>
            <column name="order_ordered_date" type="DATE"/>
            <!-- 처방 진료과 코드 — shared 에 진료과 코드가 없어 스냅샷(접수 묶음 키) -->
            <column name="order_department_code" type="VARCHAR(64)"/>
            <!-- 처방 진료과 — shared.department.department_id -->
            <column name="order_department_id" type="UUID"/>
            <column name="order_item_type_code" type="VARCHAR(32)"/>
            <column name="order_due_date" type="DATE"/>
            <column name="order_patient_instruction" type="VARCHAR(500)"/>
            <column name="order_duration_minutes" type="INT" defaultValueNumeric="0">
                <constraints nullable="false"/>
            </column>
            <!-- 예약 시점 판정 사실 — 나이 · 나이군 · 내원 구분 · 초재진(슬롯 3축 판정에 쓴 값) -->
            <column name="patient_age" type="INT"/>
            <column name="patient_age_group" type="VARCHAR(16)"/>
            <column name="patient_encounter_class" type="VARCHAR(16)"/>
            <column name="patient_visit_type" type="VARCHAR(16)"/>
            <column name="actor_type" type="VARCHAR(24)"/>
            <column name="actor_id" type="VARCHAR(64)"/>
            <column name="actor_name" type="VARCHAR(200)"/>
            <column name="actor_department_id" type="VARCHAR(64)"/>
            <column name="actor_department_name" type="VARCHAR(200)"/>
            <column name="schedule_id" type="VARCHAR(26)"/>
            <column name="slot_id" type="VARCHAR(26)"/>
            <column name="start_at" type="TIMESTAMP"/>
            <column name="end_at" type="TIMESTAMP"/>
            <column name="visit_date" type="DATE"/>
            <column name="preferred_date" type="DATE"/>
            <column name="priority" type="VARCHAR(16)"/>
            <!-- 접수 역참조 캐시 — shared.encounter.encounter_id -->
            <column name="encounter_id" type="UUID"/>
            <!-- 접수 상태 캐시(planned · arrived …) — 변경 · 취소 잠금 판정 -->
            <column name="encounter_status" type="VARCHAR(16)"/>
            <column name="overbooked" type="BOOLEAN" defaultValueBoolean="false">
                <constraints nullable="false"/>
            </column>
            <column name="series_group_id" type="VARCHAR(26)"/>
            <column name="series_seq" type="INT"/>
            <column name="series_total" type="INT"/>
            <column name="booking_channel" type="VARCHAR(16)" defaultValue="CENTRAL">
                <constraints nullable="false"/>
            </column>
            <column name="booking_route" type="VARCHAR(16)"/>
            <column name="cancellation_reason" type="VARCHAR(32)"/>
            <column name="cancelled_at" type="TIMESTAMP"/>
```

`:833-883` (감사 4종 · `@Version` · 인덱스):

```xml
            <column name="note" type="VARCHAR(500)"/>
            <column name="reschedule_reason" type="VARCHAR(32)"/>
            <column name="reschedule_note" type="VARCHAR(500)"/>
            <column name="rescheduled_at" type="TIMESTAMP"/>
            <column name="booked_at" type="TIMESTAMP"/>
            <!-- 낙관적 잠금(@Version) — 화면의 If-Match 가 이 값이다 -->
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
        <createIndex tableName="appointment" indexName="ix_appointment_patient_start">
            <column name="tenant_id"/>
            <column name="hospital_code"/>
            <column name="patient_id"/>
            <column name="start_at"/>
        </createIndex>
        <createIndex tableName="appointment" indexName="ix_appointment_schedule_start">
            <column name="schedule_id"/>
            <column name="start_at"/>
        </createIndex>
        <createIndex tableName="appointment" indexName="ix_appointment_based_on">
            <column name="tenant_id"/>
            <column name="hospital_code"/>
            <column name="based_on_type"/>
            <column name="based_on_id"/>
        </createIndex>
        <createIndex tableName="appointment" indexName="ix_appointment_encounter">
            <column name="encounter_id"/>
        </createIndex>
        <createIndex tableName="appointment" indexName="ix_appointment_series">
            <column name="series_group_id"/>
        </createIndex>
        <createIndex tableName="appointment" indexName="ix_appointment_slot">
            <column name="slot_id"/>
        </createIndex>
        <createIndex tableName="appointment" indexName="ix_appointment_visit_date">
            <column name="tenant_id"/>
            <column name="hospital_code"/>
            <column name="visit_date"/>
        </createIndex>
        <rollback/>
    </changeSet>
```

⚠ 001 의 `start_at` 등은 `TIMESTAMP`(시간대 없음) · PK 는 `VARCHAR(26)`(ULID) 이고, 뒤 changeSet 이 고친다 — `016-entity-id-uuid-length.xml`(36자) · `017-timestamp-with-time-zone.xml`(`USING ... AT TIME ZONE 'UTC'`). `017` 머리말(`:1-33`):

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!--
  시각 컬럼을 전부 절대 시각(TIMESTAMP WITH TIME ZONE)으로 맞춘다(classdiagram 1.26).
  감사 · 수신 장치 등 이 스키마의 다른 시각 컬럼과 같은 타입이다. 앱은 java.time.Instant 로 읽고 쓰므로 저장값은 UTC 기준의 한 순간이다
  (조회 도구는 세션 시간대로 보여 준다 — SET TIME ZONE 'Asia/Seoul' 이면 병원 시각).
  기존 값은 시간대 없이 UTC 벽시계로 들어 있으므로(배포 파드의 JVM 시간대 = UTC) UTC 로 해석해 옮긴다 — 같은 순간이라 값이 바뀌지 않는다.
  USING 을 명시한다 — 생략하면 세션 시간대로 해석돼 실행한 곳에 따라 9시간 어긋난다.
  이미 바뀐 컬럼이 하나라도 있으면(db/manual/016 을 먼저 실행한 DB) 건너뛴다 — WITH TIME ZONE 컬럼에 같은 USING 을 다시 돌리면
  실행 세션의 시간대로 값이 옮겨진다. 남은 컬럼은 018 이 마저 바꾼다. precondition 은 checksum 에 들지 않는다.
-->
<databaseChangeLog
        xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
            http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-latest.xsd">

    <changeSet id="pa-017-timestamp-with-time-zone" author="patient-appointment">
        <preConditions onFail="MARK_RAN">
            <sqlCheck expectedResult="0">
                SELECT count(*) FROM information_schema.columns
                 WHERE table_schema = current_schema()
                   AND data_type = 'timestamp with time zone'
                   AND (table_name, column_name) IN (
                       ('slot', 'start_at'), ('slot', 'end_at'),
                       ('appointment', 'start_at'), ('appointment', 'end_at'), ('appointment', 'cancelled_at'),
                       ('appointment', 'rescheduled_at'), ('appointment', 'booked_at'),
                       ('appointment_phase', 'start_at'), ('appointment_phase', 'end_at'),
                       ('appointment_change', 'changed_at'), ('appointment_change', 'from_start_at'), ('appointment_change', 'to_start_at'),
                       ('reassignment_task', 'slot_start_at'), ('reassignment_task', 'resolved_at'),
                       ('clinic_hours_policy', 'holidays_synced_at'), ('employee_schedule', 'received_at'),
                       ('schedule_comment', 'time'), ('period_comment_revision', 'time'), ('overbook_allowance_revision', 'time'),
                       ('chemotherapy_registration_revision', 'time'), ('patient_schedule_note_revision', 'time'))
            </sqlCheck>
        </preConditions>
```

### 8.6 `build.gradle.kts` — `x-patient-appointment/backend/patient-appointment/build.gradle.kts:1-41`, `:43-89`

```kotlin
// Patient Appointment(통합예약) — 단일 서비스 모듈 (배포 단위 = bootJar 1개).
// 레이어를 패키지로 표현(presentation / application / domain / infrastructure).
// 레이어 의존 방향은 패키지 규약 + ArchUnit(layer_isolation_test) 으로 보장한다.
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
```

```kotlin
    // 사내 공통 EventBus — ERP 직원 일정(학회 · 연차) 수신 하나(classdiagram 1.17 · api §9-b).
    //   0.1.10 부터 한 토픽에 여러 messageType 을 태우고 봉투의 @MessageType 으로 분배한다(토픽 규약 필수 기능).
    //   ⚠ 구독 등록은 projectx.patient-appointment.event-bus.enabled = true 일 때만 한다(기본 꺼짐) —
    //     꺼져 있으면 브로커에 붙지 않는다. 발행 배선은 없다(domain_event_integration_publisher 는 로그만).
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
    // 사내 공통 Observability — HTTP inbound · Kafka(common-eventbus) 자동 로깅(ECS).
    //   eventbus 와 같은 계열(com.projectx.common)로 맞춘다 — 구 계열은 봉투 로깅 훅이 안 붙는다.
    implementation("com.projectx.common:common-observability-kt:0.1.18")
    // 민감 필드 마스킹 — body 로깅 시 PII(patient_name 등) 자동 마스킹.
    implementation("com.projectx.common:common-masking-kt:0.1.6")
    implementation("org.springframework.kafka:spring-kafka")
    // dev(GCP Managed Kafka) 는 SASL_SSL + OAUTHBEARER 다 — 이 핸들러가 없으면 configMap 이 SASL 설정을 주입한 순간
    //   sasl.login.callback.handler.class 를 못 찾아 consumer 생성이 깨진다. 전이 io.confluent:* 는 mavenCentral 에 없어 제외한다.
    implementation("com.google.cloud.hosted.kafka:managed-kafka-auth-login-handler:1.0.6") {
        exclude(group = "io.confluent")
    }
    // common-eventbus 의 Rabbit 빈이 ConnectionFactory 를 요구할 수 있다(provider=kafka). lazy 라 미사용 시 연결 없음.
    implementation("org.springframework.boot:spring-boot-starter-amqp")

    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")

    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:$springdocVersion")

    // 로컬 접속값을 코드 밖에서 준다 — `backend/.env` 를 Spring Environment 로 로드(spring-dotenv).
    //   Boot 4 호환 등록은 META-INF/spring.factories 의 dotenv_environment_post_processor.
    //   .env 부재 시 무시되므로 k8s 배포(실제 환경변수)에는 영향이 없다.
    implementation("me.paulschwarz:spring-dotenv:4.0.0")

    // 도메인 단위 테스트 + ArchUnit(레이어 격리 가드)
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("com.tngtech.archunit:archunit-junit5:1.4.1")
    // 버전은 Boot BOM 이 관리한다 — 여기서 핀하면 Boot 업그레이드 때 어긋난다.
    testImplementation("org.assertj:assertj-core")
    // 슬롯 저장 경로의 SQL 문장 수 검증용 인메모리 DB — 버전은 Boot BOM 관리.
    testImplementation("com.h2database:h2")
}
```

⚠ 위 주석(「기본 꺼짐」·「발행 배선은 없다」)은 **낡았다** — 실제 기본값은 `enabled: ${EVENT_BUS_ENABLED:true}`(`application.yml:128`) 이고 발행도 한다(§3.15). 신입은 yml 과 코드를 기준으로 본다.

### 8.7 도메인 서비스 `@Bean` 등록 — `$B/infrastructure/config/appointment_configs.kt:34-47`

domain 패키지는 Spring 금지라 `@Component` 를 못 단다. 여기서 수동 등록(`:50-79` 에 `schedule_expansion_policy` · `slot_occupation_service` · `reassignment_policy` · `appointment_planner` · `encounter_grouping_policy` · `move_together_policy` · `chemotherapy_cycle_plan` · `patient_schedule_policy` 8개).

```kotlin
/**
 * 도메인 서비스 빈 등록.
 * domain 패키지는 Spring 의존이 금지(ArchUnit 강제)라 `@Component` 를 붙일 수 없다 — 여기서 등록한다.
 *
 * ⚠ 빈 메서드명을 타입명과 같게 두면 본문의 생성자 호출이 자기 자신(빈 메서드)으로 해석돼
 *   StackOverflowError 가 난다 — 접두를 붙여 구분한다.
 */
@Configuration
@EnableConfigurationProperties(com.projectx.patientappointment.application.support.appointment_properties::class)
class domain_service_config {

    /** 병원 로컬 타임존 고정 — `slot_date` · `visit_date` 파생이 여기 의존한다. */
    @Bean
    fun clock(): Clock = Clock.system(clinic_zone)
```

---

## 9. 테스트

테스트 73개 파일(`$T/`): `architecture/`(1) · `domain/`(22, 순수 단위) · `application/`(25, fake 로 핸들러) · `infrastructure/` · `persistence/`(H2 PostgreSQL 모드 실행) · `inbox/`(7) · `integrationevents/`(3) · `presentation/`(1).

### 9.1 ArchUnit — `$T/architecture/layer_isolation_test.kt:8-44`

```kotlin
/**
 * 레이어 격리 가드 — 단일 서비스 모듈이라 컴파일 타임 모듈 경계가 없는 대신,
 * domain 이 Spring/바깥 레이어에 의존하지 않음을 ArchUnit 으로 강제한다(루트 CLAUDE.md "domain Spring 금지").
 */
@AnalyzeClasses(packages = ["com.projectx.patientappointment"])
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

    /** 조회는 QueryDSL 만 — MyBatis / 네이티브 SQL 매퍼 의존 금지. */
    @ArchTest
    val no_mybatis_anywhere: ArchRule =
        noClasses()
            .that().resideInAPackage("com.projectx.patientappointment..")
            .should().dependOnClassesThat().resideInAnyPackage("org.apache.ibatis..", "org.mybatis..")

    /** presentation 은 도메인 애그리거트를 직렬화하지 않는다 — 응답은 view_model 만. */
    @ArchTest
    val presentation_must_not_depend_on_aggregates: ArchRule =
        noClasses()
            .that().resideInAPackage("..presentation..")
            .should().dependOnClassesThat().resideInAnyPackage("..domain.aggregatesmodel..")
}
```

### 9.2 단위 테스트(도메인, Spring 없음) — `$T/domain/slot_and_appointment_test.kt:36-46`, `:116-150`

팩토리(`schedule.of` · `slot.of` · `appointment.of`)로 객체를 만들고 `slot_occupation_service()` 를 직접 `new` 한다. DB · 컨텍스트 없음.

```kotlin
/**
 * 슬롯 점유와 예약 전이 — `patient-appointment.classdiagram.md` §2.12 · §2.8 ·
 * 불변식 7·10·17·19·20·25.
 */
@DisplayName("slot · appointment — 점유와 전이")
class slot_and_appointment_test {

    private val now = Instant.parse("2026-10-05T00:00:00Z")
    private val slot_start = Instant.parse("2026-10-05T01:00:00Z")
    private val occupation = slot_occupation_service()
```

```kotlin
    @Nested
    @DisplayName("slot.is_bookable — 판정은 여기 한 곳이다(P38)")
    inner class bookability {

        @Test
        fun `자리가 있고 미래이며 3축을 통과하면 예약 가능하다`() {
            val row = a_slot(a_schedule())
            assertThat(row.is_bookable(eligibility.of_patient(age_group.ADULT, null, null), null, now)).isTrue()
        }

        @Test
        fun `꽉 차면 SLOT_FULL 이다`() {
            val row = a_slot(a_schedule(), capacity = 1)
            row.occupy(now)
            assertThat(row.status).isEqualTo(slot_status.BUSY)
            assertThat(row.is_bookable(eligibility.unrestricted(), null, now)).isFalse()
            assertThat(row.unbookable_reason(eligibility.unrestricted(), null, now)).isEqualTo("SLOT_FULL")
        }

        @Test
        fun `3축이 안 맞으면 SLOT_NOT_ELIGIBLE 이다`() {
            val row = a_slot(a_schedule(), target = eligibility.of(setOf(age_group.PEDIATRIC)))
            val adult = eligibility.of_patient(age_group.ADULT, null, null)
            assertThat(row.unbookable_reason(adult, null, now)).isEqualTo("SLOT_NOT_ELIGIBLE")
        }

        @Test
        fun `지난 시각은 SLOT_IN_PAST 다`() {
            val row = a_slot(a_schedule())
            assertThat(row.unbookable_reason(eligibility.unrestricted(), null, slot_start.plusSeconds(60)))
                .isEqualTo("SLOT_IN_PAST")
        }

        @Test
        fun `휴무로 닫히면 SLOT_CLOSED 다`() {
            val row = a_slot(a_schedule())
            row.mark_unavailable("01JRULE", "장비 점검", now)
            assertThat(row.unbookable_reason(eligibility.unrestricted(), null, now)).isEqualTo("SLOT_CLOSED")
        }
```

### 9.3 통합(영속성) 테스트 — H2 PostgreSQL 모드 위에서 QueryDSL 실행

하네스 — `$T/persistence/persistence_test_support.kt:85-100`(Spring 컨텍스트 없이 Hibernate `SessionFactory` 만 띄운다 · `shared` 스키마도 만든다):

```kotlin
/** H2(PostgreSQL 모드) 위의 세션 팩토리 — shared 스키마까지 만든다. 쿼리를 실제로 실행해 볼 때 쓴다. */
internal fun <T> with_h2(block: (SessionFactory) -> T): T {
    val h2 = JdbcDataSource().apply {
        setURL("jdbc:h2:mem:pa-persistence-${System.nanoTime()};MODE=PostgreSQL;DB_CLOSE_DELAY=-1")
        user = "sa"
    }
    val registry = StandardServiceRegistryBuilder()
        .applySetting("hibernate.connection.datasource", h2)
        .applySetting("hibernate.hbm2ddl.auto", "create-drop")
        // shared 스키마도 테스트 DB 에 만든다 — 운영에서는 소유자가 만든 스키마를 읽기만 한다
        .applySetting("hibernate.hbm2ddl.create_namespaces", "true")
        .build()
    try {
        return metadata_of(registry).buildSessionFactory().use(block)
    } finally {
        StandardServiceRegistryBuilder.destroy(registry)
    }
}
```

테스트 — `$T/persistence/shared_join_query_test.kt:30-70`:

```kotlin
/**
 * shared 조인 · 조회가 HQL 로 해석되고 id(uuid = uuid)로 이어지는지 — H2 위에서 실제로 실행한다(classdiagram 1.27).
 *
 * 문자열과 uuid 를 그대로 비교하면 Hibernate 가 쿼리를 만들 때(의미 분석) 거절한다 — 빌드는 통과하고 조회 API 를
 * 처음 부를 때 500 으로만 드러난다. 예약 목록의 조인은 `appointment_search_query_test` 가 실행해 본다.
 */
class shared_join_query_test {

    private val now: Instant = Instant.parse("2026-10-13T00:00:00Z")

    @AfterEach
    fun clear_mdc() = MDC.remove(JwtKeys.USER_ID)

    @Test
    fun `비고 이력 · 기간 비고의 작성자 이름을 shared 직원에서 조인해 읽는다`() {
        with_h2 { session_factory ->
            val owner = a_schedule()
            write(session_factory) { em ->
                insert_staff(em, TEST_EMPLOYEE_ID, "김영상", "A0001")
                MDC.put(JwtKeys.USER_ID, TEST_EMPLOYEE_ID)
                owner.change_comment("<p>목요일 오후는 소아 우선</p>", now)
                owner.add_period_comment(LocalDate.of(2026, 10, 13), LocalDate.of(2026, 10, 17), "<p>조영제 수급 지연</p>", now)
                em.persist(owner)
            }

            read(session_factory) { factory ->
                val comment = schedule_comment_query_dsl(factory).find(TEST_SCOPE, owner.schedule_id)!!.single()
                assertThat(comment.author_id).isEqualTo(TEST_EMPLOYEE_ID)
                assertThat(comment.author_name).isEqualTo("김영상")

                val periods = period_comment_query_dsl(factory)
                assertThat(periods.find(TEST_SCOPE, listOf(owner.schedule_id), null, null).single().author_name).isEqualTo("김영상")
                assertThat(periods.revisions(TEST_SCOPE, owner.schedule_id)!!.single().author_name).isEqualTo("김영상")

                // 다른 테넌트 범위에서는 보이지 않는다
                assertThat(schedule_comment_query_dsl(factory).find(OTHER_SCOPE, owner.schedule_id)).isNull()
                assertThat(periods.find(OTHER_SCOPE, listOf(owner.schedule_id), null, null)).isEmpty()
            }
        }
    }
```

(`@SpringBootTest` 류의 전체 컨텍스트 테스트는 없다 — 핸들러 테스트는 `application/shared_test_fakes.kt` 의 in-memory fake 리포지토리로 돈다.)

---

## 10. 이 레포만의 특징 패턴

각 항목은 KDoc 원문 인용(경로:줄) + 요점.

### 10.1 슬롯 정의(패턴 · 생성 규칙) / 운영(각인 · 반영)

- `schedule`(예약 기준) 밑에 패턴(시간표) → 기간 반영 → 휴무 → 일자 조정 순으로 **전개(expand)** 하고, 전개 결과를 `slot` 행으로 **각인(activate)** 한다. 전개 계산은 Spring 없는 도메인 서비스.
- `slot` 은 개념상 `schedule` 애그리게잇 안이지만 **행 단위로 자기 리포지토리**를 갖는다(수천 건).
- 생성 규칙은 「데이터가 아니라 도구」 — 규칙을 지우면 만든 행도 같이 사라진다.

`$B/domain/services/schedule_expansion_policy.kt:14-28`:

```kotlin
/**
 * 전개와 각인의 계산. (`patient-appointment.classdiagram.md` §3)
 *
 * Spring 의존이 없는 **도메인 서비스**다 — `infrastructure/config/appointment_configs` 가 `@Bean` 으로 등록한다.
 *
 * 이 클래스가 하는 일은 둘이다:
 * 1. [expand] — 기간을 날짜로 펴서 `schedule.expand(date)` 를 모은다(저장 안 함).
 *    순서는 반영 → 행 → 휴무 · 진료시간(외래) → **일자 조정**이고 뒤가 앞을 이긴다(불변식 14).
 *    외래는 `clinic_hours_effective` 를 함께 받아 그 날 요일의 열린 구간 밖 행을 거른다(불변식 48) — 미리보기 · 각인 ·
 *    반영 영향 · 월간 현황 · 진료일정 현황 · 현황판이 같은 값을 넘겨야 보이는 칸 수와 만들어지는 슬롯 수가 같다.
 * 2. [plan_activation] — 계획과 **이미 있는 슬롯**을 대조해 만들 것·유지할 것·지울 것·덮어쓸 것을 가른다.
 *
 * 슬롯은 루트로 로딩하지 않으므로(P37) 기존 슬롯은 **인자로 받는다** — 애플리케이션이
 * `i_slot_repository` 에서 읽어 넘긴다.
 */
```

`$B/domain/aggregatesmodel/schedule/pattern_generation_rule.kt:21-28`:

```kotlin
/**
 * 행을 **만드는 도구**(구간 규칙). (`patient-appointment.classdiagram.md` §2.4)
 *
 * 「월~금 09:00~12:00 · 30분 · 2명」 한 줄이 행 30개를 만든다. 규칙은 데이터가 아니라 도구라서
 * 화면의 [저장] 버튼과 무관하게 즉시 저장되고, **표가 바뀌는 것은 `apply` 뿐**이다.
 *
 * 규칙을 지우면 그 규칙의 행도 함께 지워진다 — 만든 행과 그 자리의 손으로 고친 행(부록 P68 · `schedule_pattern.remove_generation_rule`).
 */
```

각인 저장 최적화(`persist` vs `merge` 를 `is_new()` 로 가른다) — `$B/infrastructure/repositories/appointment_repositories.kt:174-189`:

```kotlin
    /**
     * 일괄 저장 — **신규는 `persist`, 기존은 `merge`** 로 가른다(`slot.is_new()`).
     * `jpa.saveAll` 은 id 가 미리 부여된 신규 슬롯을 기존으로 보고 `merge` 해 행마다 SELECT 를 먼저 보낸다.
     * `persist` 는 SELECT 없이 INSERT 만 쌓고, 쌓인 INSERT 는 `hibernate.jdbc.batch_size` 로 묶여 나간다.
     * 기존 슬롯은 이미 이 영속성 컨텍스트가 관리 중이라 `merge` 가 같은 인스턴스를 돌려준다(SELECT 없음).
     * `@Transactional` 은 `SimpleJpaRepository.saveAll` 과 같은 전파(REQUIRED)다 — 호출자 트랜잭션에 합류한다.
     */
    @Transactional
    override fun save_all(aggregates: List<slot>): List<slot> =
        save_all_with_events(aggregates, dispatcher) { rows ->
            val saved = rows.map { row ->
                if (row.is_new()) row.also { entity_manager.persist(it) } else entity_manager.merge(row)
            }
            entity_manager.flush()
            saved
        }
```

### 10.2 슬롯 관리 여부 · 세션 단위 · 시각 미정 칸(오버부킹 슬롯)

- `schedule.slot_managed`(`schedule.kt:222-223`) 가 `false` 면 칸(정원) 없이 받는다. `session_unit`(`:231-232`) 은 「세션 한 칸」을 오전·오후·야간으로 부를지 종일로 묶을지.
- 칸을 못 고른 예약(처방 자동 예약 · 원무 주도 외래 · 시각 미정 줄)은 그 날 00:00 **오버부킹 슬롯**에 들어간다 — `slot.occupy_overbook()`(`slot.kt:227-232`) 은 정원과 점유를 함께 1 올린다.
- 한도는 `overbook_allowance`(기간 · 하루 최대) — 이력(revision) 을 쌓고 지우면 `REMOVED` 로 내린다.

`$B/domain/aggregatesmodel/schedule/overbook_allowance.kt:26-35`:

```kotlin
/**
 * **오버부킹 가능 범위 — 지금 값.** 슬롯을 관리하는 예약 기준(외래 진료의 · 검사실 등)이 그 기간에 하루 몇 건까지 오버부킹을 받는가.
 * (`patient-appointment.classdiagram.md` §2.28 · 1.31 · 1.33 · 부록 P99 · P102)
 *
 * 칸(`slot_id`) 없이 온 원무 주도 외래 예약 · 검사 예약 대화상자의 시각 미정 줄이 그 날 00:00 오버부킹 칸에 들어갈 때 [daily_limit] 이 한도다 — 덮는 행이 없는 날은 오버부킹 불가다.
 * 처방 수신 자동 예약은 이 한도로 막지 않는다(넣은 건은 센 수에 든다).
 * `active` 행끼리 기간이 겹치지 않는다(불변식 52) — 겹침은 행 사이의 규칙이라 소유 `schedule` 이 판정한다.
 * 바꿀 때마다 [revisions] 에 한 행이 쌓이고, 지우면 행을 없애지 않고 `REMOVED` 로 내린다. 동시 편집은 **이 행의** [version] 이 막는다.
 * 「마지막으로 저장한 사람」은 감사 컬럼 `updated_by` 다 — 저장자 컬럼을 따로 두지 않는다([record] 가 수정 시각을 고쳐 dirty 로 만든다).
 */
```

### 10.3 진료시간 기준관리(clinic_hours_policy)

- 병원 공통 1행 + 진료과 예외 N행. 예외 행의 빈 칸은 병원 공통으로 채운 **merged** 값을 쓴다(저장하지 않는 파생).
- 외래 전개 · 진료표 저장 검사 · `GET /clinic-hours-policies/effective` 가 같은 merged 값을 본다. 공휴일 설정은 병원 공통만.
- 버전 · 개정안 없음 — 저장 즉시 적용, 「최종 수정」은 감사 컬럼. Liquibase 002 가 배포 테넌트의 병원 공통 1행을 넣는다(§8.2 `parameters`).

`$B/domain/aggregatesmodel/clinichours/clinic_hours_policy.kt:99-110`:

```kotlin
/**
 * 진료시간 기준 — **적용 단위 하나 = 한 행**(병원 공통 1행 + 진료과 · 센터 예외 N행). (`patient-appointment.classdiagram.md` §2.22 · 1.15)
 *
 * 화면 `configuration` 의 좌측 줄 하나가 이 행이다. **예약 주체(`schedule`) 밖의 베이스 설정이자 외래 슬롯의 운영 틀**이다 — 스스로 슬롯을 만들지는 않지만
 * 외래 진료표의 기본값이 되고(불변식 49) 저장할 때 검사하며(불변식 39) 전개 · 각인이 외래 행을 그 날 요일의 열린 구간으로 거른다(불변식 48 · 부록 P76).
 * 추천 · 현황 · 현황판은 같은 전개와 각인된 슬롯을 읽으므로 그대로 따라온다.
 *
 * - 유효기간 버전 · 개정안 · 이력 목록이 없다(부록 P75) — 저장 즉시 적용이고 「최종 수정」(감사 컬럼 `updated_at` · `updated_by` — 토큰 직원)만 남는다.
 * - 진료과 예외의 세션 기준값 · 예약 설정은 **열마다 nullable** 이고 비면 병원 공통 값을 따른다([merged]).
 *   요일 표 · 공통 규칙은 단위마다 자기 값이다(예외를 만들 때 병원 공통을 복사한다).
 * - 「대상 아님」(`excluded`)은 요일 표 · 공통 규칙 · 기준값 · 예약 설정이 모두 비어 있다(불변식 38).
 */
```

전역 휴무 · 공휴일 가져오기 — `$B/application/commands/clinichours/holiday_command_handlers.kt:30-35`:

```kotlin
/**
 * 전역 휴진 · 공휴일 가져오기. (`patient-appointment.classdiagram.md` §3.4 · 부록 P77 · api §4.2-c)
 *
 * 전역 휴무(`schedule_id = null`)는 **모든 예약 구분의 전개가 함께 본다**. 여기서는 그 행만 쓰고
 * **이미 만들어진 슬롯은 고치지 않는다** — 그 날의 등록된 예약 수만 알린다. 슬롯은 운영 면의 [생성](`activate`)이 다시 만들 때 닫힌다.
 */
```

### 10.4 외래 진료 현황판(schedule_dashboard)

- 대기실 전광판용 — 날짜 × 오전/오후 칸을 진료과 · 의사로 묶는다. 내부 판정(진료 칸 · 휴무 · 공휴일)은 애그리게잇 계산, 외부 진료불가(ERP 태그 등)는 **진료 칸에만** 덮는다.
- 대진(substitute) 은 휴진과 다르게 알린다. 쿼리 IF 는 `i_schedule_dashboard_queries`(`$B/application/queries/schedule_dashboard_queries.kt`).

`$B/application/support/schedule_dashboard_service.kt:23-29`:

```kotlin
/**
 * **외래 진료 현황판** — 대기실 전광판의 칸(날짜 × 오전·오후)을 진료과 · 의사로 묶는다(classdiagram 1.13 · api §2.5 · 부록 P72).
 *
 * 내부 판정(진료 칸 · 휴무 사유 · 공휴일)은 [i_schedule_dashboard_queries] 가 애그리게잇 계산으로 내고, 이 서비스는
 * 외부 진료불가([actor_not_available_reader])를 **진료 칸에만** 덮어 칸 상태를 정한다 — 이미 내부 사유로 닫힌 칸은 내부 사유를 남긴다.
 * 사유 → 상태는 진료일정 현황(1.12)의 표를 그대로 쓰고, 대진만 따로 뗀다(전광판은 대진을 휴진과 다르게 알린다).
 */
```

### 10.5 ERP 연동(직원 일정 수신 · 외부 원천 HTTP 포트)

- ERP 직원 일정(학회 · 연차)은 Kafka 로 받아 `employee_schedule` 사본으로 두고 **태그만** 단다(슬롯 · 휴무를 바꾸지 않는다). 자연키 = ERP 일정 id(테넌트 범위 안 UK), `source_event_version` 으로 멱등.
- 진료과 · 의사 · 방 마스터 · 투약 일정 · 가용 상태는 **HTTP 포트**(`domain/ports/external_ports.kt` IF ← `infrastructure/ports/*_http_adapter.kt`)로 읽고 저장하지 않는다. 주소 · 타임아웃은 `application.yml:152-187`.

`$B/domain/aggregatesmodel/employeeschedule/employee_schedule.kt:21-32`:

```kotlin
/**
 * **ERP 직원 일정 수신 사본** — 학회 · 연차만 둔다. (`patient-appointment.classdiagram.md` §2.26 · 1.17 · 부록 P81)
 *
 * ERP 가 `ErpEmployeeSchedule` 이벤트로 밀어 준 일정을 받아 외부 가용 상태(1.11)에 「학회」 · 「연차」 태그로 싣는다.
 * **슬롯 · 휴무를 바꾸지 않는다** — 태그만이다(불변식 43). 쓰는 곳은 이벤트 핸들러 하나다.
 *
 * PK 는 이 BC 의 ULID([employee_schedule_id])이고 **자연키는 ERP 일정 id([erp_schedule_id])**다 — 테넌트 범위 안에서 유일하다
 * (UK `(tenant_id, hospital_code, erp_schedule_id)` · 부록 P91). ERP id 를 PK 로 쓰면 다른 병원에서 같은 id 가 올 때 부딪힌다.
 * 같거나 낮은 [source_event_version] 은 반영하지 않는다(재전달 · 순서 뒤바뀜).
 * 직원은 `shared.user_account` 의 uuid([employee_id])다 — 사번 · 이름은 저장하지 않고 조회가 shared 를 조인해 읽는다.
 * 취소는 행을 지우지 않고 `CANCELLED` 로 내린다.
 */
```

### 10.6 접수 Kafka 요청(planned encounter) · 원무 회신 · 원무 주도 외래 예약

- 예약 등록 · 이동 뒤 **같은 트랜잭션에서** (환자 · 처방일 · 진료과 · 진료의 · 예약일) 묶음마다 접수 요청을 만들어 도메인 이벤트로 싣고, 커밋 뒤 `encounter-request` 토픽으로 나간다. 묶음 키가 없으면 예약 자체를 저장하지 않는다(422).
- 원무 회신(`REGISTERED` …) 과 원무 주도 외래 예약(`BOOK` · `RESCHEDULE` · `CANCEL`) 은 같은 `appointment-request` 토픽 · 환자당 메시지 1건 · **메시지 1건 = 묶음 명령 1건 = 트랜잭션 하나**.
- 예약 취소는 구분 무관 예약마다 접수 `CANCEL` 요청.

`$B/application/support/planned_encounter_service.kt:26-46`:

```kotlin
/**
 * 예약 등록 뒤 **접수를 요청한다**. (`patient-appointment.classdiagram.md` §1.9 · 1.28 · §3.2 · 불변식 27 · 28 · 부록 P92 · P105)
 *
 * 예약 등록 핸들러(`book` · `book-bundle` · `book-series` · 모바일 · 처방 기반 자동 예약)와 `reschedule` 이 **커밋 전, 같은 트랜잭션에서** 부른다:
 * 1. `encounter_grouping_policy` 가 이번에 등록 · 이동한 예약을 (처방일 · 진료과 id · 진료의 · 예약일)로 묶는다 — 묶음 1개 = 접수 요청 항목 1건
 * 2. 묶음의 예약 중 **기존 `encounter_id`(planned · 원천에서 쓸 수 있는 접수)를 가진 것이 있으면 `UPDATE`**(그 접수 · 그 접수의 다른 예약도 함께 싣는다),
 *    **그 밖에는 `CREATE`** 다(사용자 결정 · 부록 P105). 다른 저장의 예약에 합류하지 않고 접수 요청 id 도 두지 않는다 — 원무는 요청의 예약 id 로 접수를 찾는다
 * 3. 예약에 `request_encounter` 로 상태(`requested` · `UPDATE` 면 `planned` 그대로)를 남기고, 묶음마다 만든 요청을 모아 `i_encounter_request_port.request()` 에 **한 번에** 싣는다 —
 *    요청은 **커밋 뒤** Kafka 로 환자마다 메시지 1건(요청 배열)으로 나간다(롤백되면 나가지 않는다). `encounter_id` 는 원무 회신(1.28)이 붙인다.
 *
 * **접수 묶음 키가 없으면 예약을 저장하지 않는다**(`422 ENCOUNTER_CONTEXT_REQUIRED` · 트랜잭션 전체를 되돌린다) — 화면에서는 저장됐는데 접수 요청이
 * 빠지면 원무와 어긋나므로, 요청을 건너뛰는 경로를 두지 않는다(부록 P105). 접수 요청 없이 예약 등록하는 경우는 둘뿐이다 —
 * 이벤트버스 꺼짐(환경 설정 · `issue[] ENCOUNTER_REQUEST_DISABLED`)과 `education`(접수를 만들지 않는 구분).
 *
 * **외래 진료 예약(`outpatient-clinic`)은 접수가 그 예약을 따라간다**(부록 P74) — 키는 (환자 · 예약일 · 진료과 · 진료의)이고 처방일이 없다.
 * **예약을 취소하면 구분과 관계없이 예약마다 접수 취소를 요청한다**(`release` → `CANCEL` · 부록 P123) — 남은 예약으로 접수를 둘지는 원무가 정한다.
 *
 * **원무 주도 외래 예약(`BOOK`)** 은 원무가 접수를 먼저 만들었으므로 묶지 않고 `link` 로 그 접수에 예약을 잇는다(`UPDATE` · 부록 P103).
 *
 * 저장은 하지 않는다 — 넘겨받은 예약 인스턴스를 고치고, 저장은 호출한 핸들러가 한다.
 */
```

`$B/application/integrationevents/eventhandling/appointment_request_handler.kt:18-34`:

```kotlin
/**
 * 원무 `AppointmentRequest` 핸들러 — **메시지 1건 = 묶음 명령 1건 = 트랜잭션 하나**(classdiagram 1.28 · api §9-e · 부록 P104).
 *
 * 메시지는 환자마다 1건이고 항목은 `appointment_requests[]` 다. **항목 전부를 먼저 명령으로 바꾼다**(필수값 · uuid · 코드 검사) —
 * 하나라도 틀리면 아무것도 반영하지 않고 거절(예외 → inbox 행 FAILED)이다. 그 뒤 `apply_appointment_requests` 1건을 보내고,
 * 그 핸들러가 항목 순서대로 단건 핸들러를 한 트랜잭션에서 부른다(한 항목이라도 예외면 전부 롤백). 항목은 두 종류이고 `event_type` 이 가른다.
 *
 * - **회신**(`REGISTERED` · `UPDATED` · `CANCELLED` · `REJECTED`) → `apply_encounter_reply` — 이 BC 가 보낸 접수 요청의 결과다.
 *   대상은 `appointment_ids`(비면 거절)이고 환자 · 수진 id 는 UUID 여야 한다 — 아니면 거절. `REJECTED` 만 수진 id 없이 받는다.
 * - **원무 주도 외래 예약**(`BOOK` · `RESCHEDULE` · `CANCEL` · 저장여부 N) → `apply_encounter_booking` — 원무가 접수를 먼저 만들고 보낸 것이다(불변식 50).
 *   수진 · 환자 id 가 필수고 대상 `appointment_ids` 는 0~1건이다(두 건 이상 · 사유 · 경로 코드가 틀리면 거절).
 *   `CANCEL` 의 `appointment_ids` 는 취소할 예약이다(건수 제한 없음 · 그 예약만 · 접수는 그대로) — 비면 원무가 그 접수를 취소한 것이라 그 접수의 예약 전부를 취소한다(부록 P123).
 *   `BOOK` · `RESCHEDULE` 에 `slot_id` 가 없으면 `department_id` · `physician_id` · `encounter_date` 가 필수다 — 그 날 오버부킹 칸에 의사별 한도 안에서 넣는다(1.31 · 없으면 FAILED).
 *
 * 항목의 `patient_id` 가 비면 메시지의 `patient_id`(파티션 키)를 쓰고, 다르면 거절이다. 항목 배열 없는 모양(`@SchemaVersion 1`)은 항목이 비어 거절된다.
 * 같은 메시지를 다시 받아도 결과가 같다(예약이 이미 그 상태면 아무것도 바꾸지 않는다).
 */
```

### 10.7 자동 예약(처방 수신 → `receive_order`)

- 진단검사 · 약제 · 투석이 공용 토픽 `order-based-appointment-request` 로 보낸 처방을 **거르지 않고 전부** 받아 자동으로 예약 등록한다. 칸이 없으면 그 날 오버부킹 슬롯에 넣는다(`overbooked = true`).
- 결과는 커밋 뒤 `order-based-appointment-booked` 로 회신(그 처방의 예약 전부를 다시 읽어 보낸다 · `publisher.kt:117-140`).

`$B/application/integrationevents/eventhandling/order_based_appointment_handler.kt:17-26`:

```kotlin
/**
 * 자동 예약 생성 요청 핸들러 — payload → 명령 하나(`receive_order`). (classdiagram 1.22 · api §9-c)
 *
 * 구독은 `event_bus_subscription_initializer` 가 `projectx.patient-appointment.event-bus.enabled = true` 일 때만 등록한다.
 * **받는 대상 = 보낸 것 전부**다 — 보내는 영역이 예약이 필요한 처방을 골랐으므로 처방 구분 · 내원 구분으로 다시 거르지 않는다.
 * 생성인데 날짜가 없으면 무시한다(로그만). 형식 오류(날짜 · event_type · base_type · 빈 id)와 받지 않는 `base_type`(외래 · 재활 · 교육)은
 * 예외로 올려 inbox 에 바로 FAILED 로 남긴다(영구 실패 · 계약 위반이라 조용히 버리지 않는다). 트랜잭션 경계는 명령 핸들러다.
 * 등록 · 변경은 접수 묶음 키(`ordered_date` · `department_id` · `physician_id` — 둘은 shared uuid)가 필수다 — 없으면 같은 방식으로 거절한다(부록 P105).
 * 처방을 낸 수진(`encounter_id`)은 없어도 받는다 — 투석처럼 진료과 · 진료의만 오면 그 둘이 처방 출처로 접수 요청에 그대로 실린다(부록 P128).
 */
```

### 10.8 기간 비고(period_comment) · 비고 이력 · 저장자 = 감사 컬럼

- 예약 주체의 날짜 범위에 붙는 안내문. 판정에는 쓰지 않는다. 바꿀 때마다 `revision` 행을 쌓고 지우면 `REMOVED`.
- 저장자 컬럼을 따로 두지 않고 **감사 컬럼 `updated_by`/`created_by`** 를 쓴다 — 그래서 `record()` 가 수정 시각을 고쳐 dirty 로 만들어 리스너가 돌게 한다. 이름은 조회가 `shared.user_account` 에서 읽는다(§9.3 테스트).

`$B/domain/aggregatesmodel/schedule/period_comment.kt:27-36`:

```kotlin
/**
 * **기간 비고 — 지금 값.** 예약 주체의 날짜 범위(하루면 시작 = 종료)에 붙는 비고다.
 * (`patient-appointment.classdiagram.md` §2.24 · 1.16 · 부록 P80)
 *
 * `schedule.comment`(예약 주체 전체에 한 벌)와 따로 선다. 슬롯 · 전개 · 예약 판정은 이 값을 읽지 않는다 — 안내문이다.
 * 기간끼리 겹쳐도 된다. 바꿀 때마다 [revisions] 에 한 행이 쌓이고, 지우면 행을 없애지 않고 `REMOVED` 로 내린다(불변식 42).
 * 동시 편집은 **이 행의** [version] 이 막는다 — 다른 기간 비고를 고치는 사람과는 부딪히지 않는다.
 * 「마지막으로 저장한 사람」은 감사 컬럼 `updated_by`(토큰 직원 uuid)다 — 저장자 컬럼을 따로 두지 않는다.
 * 등록 · 변경 · 지우기마다 [record] 가 이 행의 수정 시각을 고쳐 dirty 로 만들므로 리스너가 `updated_by` 를 그 사람으로 채운다.
 */
```

### 10.9 모바일 예약 · 역할 축(`X-Role-Code`) · 마감(booking closure)

- 모바일 예약은 명령 하나가 외래 `proposed` 찾기/만들기 → 경로 기록 → 점유 → 접수 요청을 한 트랜잭션으로 한다. 판정 · 점유는 원내 예약과 **같은 길**(`slot_occupation_service.book`).
- `X-Role-Code` 헤더 → `booking_role`(설정 `booking-roles` 매핑, 없으면 `dept`, 헤더 없으면 역할 미상) → `slot.allowed_roles` 대조.
- 마감은 휴무 규칙(`unavailability_rule`)의 한 종류로 구현된다(`booking_closure_commands.kt:29-42`).

`$B/application/commands/appointment/mobile_appointment_command_handler.kt:23-30`:

```kotlin
/**
 * **모바일 예약 등록** — 환자 앱 전용(classdiagram 1.18 · api §5.2 · 불변식 44 · 부록 P82).
 *
 * 한 트랜잭션에서 ① 그 환자의 처방 없는 외래 `proposed` 를 찾거나 만들고(불변식 34) ② 경로를 적고
 * ③ `slot_occupation_service.book` 으로 점유 · 예약 등록하고(경로 축 · 역할 `patient-app` 판정 포함) ④ 외래 접수를 요청한다(불변식 27 · 28 · 커밋 뒤 발행 · classdiagram 1.28).
 * 판정 · 점유 · 접수는 원내 예약 등록(`book_appointment_command_handler`)과 같은 길이다 — 계산을 두 벌로 두지 않는다.
 * 하나라도 실패하면 전부 되돌아간다(`422 SLOT_FULL` · `SLOT_NOT_ELIGIBLE` · `SERVICE_CATEGORY_MISMATCH` · `409`) — 접수 요청은 커밋 뒤 발행이라 여기서 실패하지 않는다.
 */
```

`$B/application/support/booking_role_resolver.kt:7-14`:

```kotlin
/**
 * `X-Role-Code` → `booking_role` (api §0 · 불변식 16).
 *
 * 슬롯 예약 가능 판정(`slot.is_bookable`)이 대조하는 호출자 역할이다. 매핑은 설정
 * `projectx.patient-appointment.booking-roles`(역할 코드 → `booking_role` 코드)가 갖고,
 * **매핑이 없는 역할은 `dept`** 로 본다. 헤더가 없으면(게이트웨이 뒤 내부 호출·스크립트) 역할 미상(`null`)이고
 * 역할 축을 보지 않는다.
 */
```

---

## 11. 신입이 헷갈릴 포인트 10개

1. **선점(hold) 단계가 없다 — `proposed → booked` 한 번이고 동시성은 `@Version` 낙관락 하나다.** 행 락(`FOR UPDATE`)을 어디서도 잡지 않는다(`appointment_editor.kt:29-34`, `appointment_repositories.kt(domain):29-30`). ⚠ `slot.kt:41` 의 KDoc 「조회 시 행 락(`find_all_for_update`)」 은 낡은 설명이다 — `grep find_all_for_update` 결과 그런 메서드는 없다. 409 `OPTIMISTIC_LOCK_CONFLICT` 가 나면 「다시 읽고 다시 저장」이 정답(`global_exception_handler.kt:118-129`).

2. **`schedule` 과 `slot` 은 같은 애그리게잇인데 Repository 가 둘이고 `slot` 도 `i_aggregate_root` 를 단다.** 수천 건이라 루트로 로딩하지 않는다(`slot.kt:30-36`, `appointment_repositories.kt(domain):23-24`). 「Aggregate Root 가 아닌 클래스에 `i_aggregate_root` 금지」 규칙의 의도적 예외(P37).

3. **애그리게잇 둘(appointment · slot)을 함께 바꾸는 곳은 `slot_occupation_service` 하나다.** 핸들러에서 `slot.occupy()` 를 직접 부르면 예약 상태와 점유 수가 어긋난다(`slot_occupation_service.kt:17-27`). 이 클래스는 Spring 빈이 아니라 `appointment_configs.kt:54-55` 가 `@Bean` 으로 등록한다 — 빈 메서드명을 타입명과 같게 두면 StackOverflow(`:38-39`).

4. **도메인 이벤트는 저장 전에 「인자 인스턴스」에서 꺼내야 한다.** `@Version` 이 있는 신규 엔티티는 Spring Data 가 `merge` 해 **다른 인스턴스**를 돌려주고 `@Transient` 이벤트는 그 사본에 없다(`appointment_repositories.kt(infra):47-50`). 단건 저장이 `saveAndFlush` 인 이유도 같은 파일 `:54-56`(ETag 용 `version` 이 한 박자 늦으면 매번 409).

5. **통합 이벤트 발행은 best-effort · outbox 없음.** `AFTER_COMMIT` 리스너가 예외를 삼키고 WARN 만 남긴다(`domain_event_integration_publisher.kt:46`, `:86-88`). 발행 실패해도 DB 는 커밋된 상태이고 재전송은 없다. 또 `EVENT_BUS_ENABLED=false` 면 「조립 완료(발행 꺼짐)」 INFO 로그만 남는다(`:80-83`).

6. **payload 케이스가 두 벌이다.** ERP 수신(`erp_employee_schedule_integration_event.kt:14`) 과 `outbound_integration_events.kt:8` 은 camelCase, 이 BC 가 정한 계약(예약 이벤트 · 접수 요청 · 자동 예약 요청/회신)은 **snake_case**(`integration_event.kt:7`, `appointment_integration_event.kt:19`). 루트 CLAUDE.md 의 「payload 는 camelCase」 와 다르니 **클래스 KDoc 을 먼저 본다.** `partitionKeyField` 는 그 클래스의 실제 프로퍼티명(`topic_registry.kt:27` — 부팅 때 검사).

7. **Kafka 수신은 핸들러를 바로 부르지 않는다 — inbox 에 적재만 하고 ack 한다.** 처리 · 재시도 · FAILED 는 `inbox_processor`(T1/T2/T3) 가 따로 한다(`event_bus_subscription_initializer.kt:15-17`, `inbox_processor.kt:7-18`). 「메시지를 보냈는데 아무 일도 없다」 는 ① 구독 등록 실패 ERROR 로그(`:37-39`) ② inbox 행 상태(FAILED 면 같은 키의 뒤 메시지가 멈춘다 · `inbox_message.kt:26-27`) ③ `kafka_namespace` 불일치(`application.yml:98-99`) 순으로 본다.

8. **수신 처리의 `@Transactional` 은 명령 핸들러에 있고, 이벤트 핸들러에 붙여도 소용없다.** 베이스 `handle` 이 같은 빈 안에서 `handle_internal` 을 부르므로 프록시를 타지 않는다(`erp_employee_schedule_handler.kt:18-19`). 그리고 그 트랜잭션은 `inbox_tx_service.process`(T2) 에 합류한다(`inbox_tx_service.kt:59-60`).

9. **`@Embedded` VO 는 nullable 백킹 필드 + non-null 읽기 접근자 쌍이다.** 컬럼이 전부 NULL 이면 Hibernate 가 필드를 null 로 채워 Kotlin non-null 선언이 깨진다(`appointment.kt:91-97`, `appointment_phase.kt:56-60`). 새 VO 를 추가할 때 `protected var xxx_value: vo? = null` + `val xxx get() = xxx_value ?: vo.empty()` 패턴을 그대로 따른다.

10. **테넌트 · 감사 · 행위자는 전부 「흘러오는 값」이다 — 직접 넣지 않는다.** ① `tenant_scope` 는 HTTP = 토큰(`token_principal_scope.kt:6-12`), 이벤트 = `event_tenant_resolver`(봉투 vs 배포값, 다르면 WARN 후 배포값). ② 감사 컬럼은 `orm.xml` 로 등록된 `audit_entity_listener` 가 MDC `userId` 로 채운다 — `mapping-resources` 가 빠지면 EPOCH · null 로 조용히 저장된다(`application.yml:40-43`). ③ 발행 봉투 `userId` 도 MDC 다 — inbox 처리는 `inbox_dispatcher` 가 넣고 T2 밖에서 되돌린다(`inbox_dispatcher.kt:27-30`). 시스템 id 같은 대체값은 **만들지 않는다**(`event_actor.kt:13`).

(보너스) **`slot_filter.bookable` 기본값은 `null`** — 「예약 가능한 칸만」을 기본으로 몰래 좁히지 않는다(`appointment_queries.kt:67-73`). 그리고 ID 는 UUID v7(36자) 로 발급하지만 옛 행은 ULID(26자) 가 섞여 있어 `VARCHAR(36)` 컬럼에 두 모양이 공존한다(`uuid7.kt:12`, `016-entity-id-uuid-length.xml`).
