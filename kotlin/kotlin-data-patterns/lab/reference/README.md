# Kotlin 데이터 패턴 실습 — 완성 예제

JDBC 접속 설정, QueryDSL 조회, JPA 저장과 rollback, 외부 HTTP 응답 반영, Kafka 발행·수신을 직접 실행해 보는 작은 앱이다. **파일을 하나씩 만들며 공부하려면 [11장 단계별 실습](../../11-build-your-own-lab.md)부터 진행한다.** 이 폴더는 완성 코드를 비교하는 기준이다. [학습 앱 설계](../DESIGN.md)에 실무 코드와의 차이를 정리했다.

## IntelliJ에서 시작하기

IntelliJ IDEA, JDK 25, Docker Compose를 준비한다. Python과 시스템 Gradle 설치는 필요하지 않다. Gradle Wrapper 9.1.0이 Kotlin 2.3.0·Spring Boot 4.0.4·QueryDSL OpenFeign 6.11과 필요한 의존성을 내려받는다.

직접 만드는 학습은 **New Project → Kotlin → Build system: Gradle → Gradle DSL: Kotlin → JDK 25**로 시작하고 [11장](../../11-build-your-own-lab.md)의 파일을 순서대로 작성한다. compact project structure 옵션이 있다면 끄고 `src/main/kotlin` 구조를 사용한다. 샘플 파일과 Spring 설정을 섞지 않는 방법도 11장에 설명했다.

완성 예제를 먼저 실행하려면 IntelliJ의 **Open**으로 이 `reference` 폴더를 열고 Gradle 프로젝트로 불러온다. Project SDK·Gradle JVM은 25, Build and run using·Run tests using은 Gradle로 맞춘다. Gradle 창에서 `compileKotlin`, `test`를 실행할 수 있다.

`.env.example`을 IDE에서 같은 폴더의 `.env`로 복사하거나 Terminal에서 다음 명령을 실행한다.

```bash
cp .env.example .env
```

앱 시작점은 **KotlinLabMain.kt의 main**이다. main 옆 Run/Debug를 사용하고 Working directory는 `gradlew`·`.env`가 있는 프로젝트 루트로 지정한다. 터미널 실행을 선택하면 아래 `./gradlew bootRun`을 사용한다. 둘을 동시에 실행하지 않는다. Windows는 `.\gradlew.bat`, macOS/Linux의 실행 권한 오류는 `chmod +x gradlew`로 처리한다.

## DB 설정과 실행

`.env`의 `DB_PASSWORD` 빈칸에 **로컬 학습용 암호**를 직접 입력한다. Docker dotenv와 Java properties가 같은 값으로 해석하도록, 이 실습에서는 공백·따옴표 없는 영문·숫자 조합을 사용한다. `export` 접두어나 값 뒤의 인라인 주석은 쓰지 않는다. 실제 서비스의 설정·암호를 복사하지 않는다.

| 설정 | 기본값과 역할 |
|---|---|
| DB_HOST / DB_PORT | `127.0.0.1` / `55432` — 호스트에서 PostgreSQL에 접속 |
| DB_NAME / DB_USER | `kotlin_lab` / `lab_user` — 학습 DB와 계정 |
| DB_PASSWORD | 직접 입력, 저장소에서 제외되는 `.env`에 보관 |
| SERVER_PORT | `58080` — 앱은 `127.0.0.1`에 바인딩 |
| LAB_REMOTE_BASE_URL | `http://127.0.0.1:58080` — 외부 API를 흉내 내는 이 앱의 주소 |
| LAB_KAFKA_ENABLED | `false` — 기본 실습에는 Kafka가 필요하지 않음 |
| KAFKA_BOOTSTRAP_SERVERS | `127.0.0.1:19092` — 선택 단계의 Kafka 주소 |

Spring은 `application.yml`의 `spring.config.import`를 통해 현재 실행 폴더의 `.env`를 properties 형식으로 읽는다. Docker Compose도 같은 파일을 사용한다. 같은 이름의 셸·IDE 환경변수가 있으면 파일값보다 우선할 수 있다. `SERVER_PORT`를 바꾸면 `LAB_REMOTE_BASE_URL`도 맞춘다.

새 프로젝트 루트에서 다음 순서로 실행한다.

```bash
docker compose --env-file .env config --quiet
./gradlew compileKotlin test
docker compose --env-file .env up -d postgres
docker compose --env-file .env logs --tail=30 postgres
```

PostgreSQL 로그의 `database system is ready to accept connections`를 확인한 뒤 실행한다.

```bash
./gradlew bootRun
```

이 터미널은 실행 상태로 두고, 다른 터미널에서 확인한다.

```bash
curl -i http://127.0.0.1:58080/lab/ping
curl 'http://127.0.0.1:58080/lab/items?page=0&size=10'
curl -i -X POST http://127.0.0.1:58080/lab/items \
  -H 'Content-Type: application/json' \
  -d '{"name":"Alpha","active":true,"amount":100}'
curl 'http://127.0.0.1:58080/lab/items?page=0&size=10'
```

Liquibase가 `public` 스키마에 표를 만들고 Hibernate가 Entity 매핑을 검증한다. 처음 만든 DB의 첫 목록은 비어 있으며, POST 뒤에는 Alpha가 나타나야 한다. POST를 반복하면 다른 UUID의 행이 추가된다. 수정·rollback·외부 API 반영은 [11장](../../11-build-your-own-lab.md)의 실제 id·version을 사용하는 요청 순서를 따른다.

`test`는 DB 없이 실행하는 단위 테스트다. 실제 앱을 켠 뒤에는 Kotlin/JUnit의 `httpSmokeTest`로 DB 조회·commit·rollback·외부 HTTP 반영을 확인한다. 가상 Item 네 개를 추가하므로 실행할 때마다 학습 데이터가 늘어난다.

```bash
./gradlew test
./gradlew httpSmokeTest
```

IntelliJ Gradle 창의 Execute Gradle Task에서 같은 태스크 이름을 입력해 실행할 수 있다. 일반 단위 테스트는 테스트 함수 옆 Run/Debug로 한 개씩 확인한다. `http-smoke` 태그의 테스트는 기본 `test`에서 제외하므로 전용 태스크를 사용한다. 보고서는 `build/reports/tests/`에 생성된다.

## 선택: Kafka 발행·수신 관찰

앱을 IntelliJ Run/Debug 창의 **Stop**으로 종료하고 `.env`의 `LAB_KAFKA_ENABLED`를 `true`로 바꾼다. 터미널에서 bootRun으로 실행했다면 Ctrl+C로 종료한다.

```bash
docker compose --env-file .env --profile events up -d kafka
docker compose --env-file .env logs --tail=30 kafka
./gradlew bootRun
```

Kafka가 준비되고 앱이 기동하면 새 Item을 만들거나 기존 Item의 이름을 변경한다. 잠시 뒤 다음 응답을 확인한다.

```bash
curl http://127.0.0.1:58080/lab/received-events
```

Kotlin/JUnit으로 송수신까지 함께 확인하려면 앱을 켠 상태에서 `./gradlew httpSmokeTest -PlabKafka=true`를 실행한다. 포트를 바꾸었다면 `-PlabBaseUrl=http://127.0.0.1:새포트`도 지정한다. 테스트는 localhost 학습 앱만 대상으로 한다.

`storage: "MEMORY_ONLY"`는 **수신 결과를 앱 메모리에만 보관한다**는 뜻이다. 항목별 최신 원천 버전을 보여 주며 전체 메시지 이력을 저장하지 않는다. 앱 재시작 시 비고, 기존 consumer group에 commit된 offset이 있다면 과거 메시지를 자동으로 전부 다시 받지 않는다. 재시작 뒤 새 변경을 만들어 관찰한다.

`best-effort AFTER_COMMIT`은 **DB commit 뒤 Kafka 발행을 시도한다**는 뜻이다. 발행 실패가 DB 변경을 rollback하지 않으며, outbox 저장·재전송 보장이 없다. 반대로 DB 트랜잭션이 rollback된 변경은 AFTER_COMMIT 발행 대상이 아니다. `MEMORY_ONLY`는 수신 저장 방식, `best-effort`는 발행 보장 범위라는 서로 다른 설명이다.

## 실행 문제와 종료

| 증상 | 확인할 위치 |
|---|---|
| Java·class file 버전 오류 | `java -version`, `./gradlew --version`, IDE Gradle JVM |
| DB_PASSWORD 자리표시자 오류 | `.env` 존재와 입력값, 새 프로젝트 루트에서 실행했는지 |
| DB 연결 거절 | Docker 상태, PostgreSQL 준비 로그, 호스트 포트 55432 |
| 암호를 바꿨는데 인증 실패 | 기존 DB volume의 계정·암호는 `.env` 변경으로 자동 갱신되지 않음 |
| QItem을 찾지 못함 | kapt를 포함한 `compileKotlin` 실행 후 IDE Gradle 재로드 |
| 외부 HTTP만 실패 | SERVER_PORT와 LAB_REMOTE_BASE_URL 일치 여부 |
| Kafka 수신 목록이 비어 있음 | Kafka 활성화 후 앱 재기동, 새 변경 발생, 발행·수신 WARN 로그 |

Compose의 고정 프로젝트명은 `kotlin-data-lab`이다. 복사본을 여러 벌 실행해도 같은 컨테이너·volume을 가리킬 수 있으므로 한 벌씩 실행한다. 기존 DB volume이 있으면 첫 생성 당시의 계정·암호·DB가 유지된다.

앱은 IntelliJ의 **Stop**(터미널 실행이면 Ctrl+C)으로 종료하고 컨테이너는 다음과 같이 내린다. 학습 DB의 named volume은 남는다.

```bash
docker compose --env-file .env --profile events down
```

[단계별 파일 작성과 학습 과제](../../11-build-your-own-lab.md) · [데이터 패턴 교재](../../README.md)
