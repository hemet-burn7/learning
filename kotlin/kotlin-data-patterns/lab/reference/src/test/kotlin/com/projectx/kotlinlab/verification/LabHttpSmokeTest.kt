package com.projectx.kotlinlab.verification

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import tools.jackson.databind.JsonNode
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.KotlinModule
import java.io.IOException
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets.UTF_8
import java.time.Duration
import java.util.UUID

// 실행 중인 학습 앱을 HTTP로 검사한다. 앱이나 DB를 직접 시작하지 않으며 가상 항목 네 개를 남긴다.
@Tag("http-smoke")
class LabHttpSmokeTest {
    private val mapper = JsonMapper.builder().addModule(KotlinModule.Builder().build()).build()
    private val baseUri = loopbackUri(System.getProperty("lab.baseUrl", "http://127.0.0.1:58080"))
    private val client = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(5))
        .followRedirects(HttpClient.Redirect.NEVER)
        .build()
    private val passed = mutableListOf<String>()

    @AfterEach
    fun closeClient() = client.close()

    @Test
    fun `실행 중인 앱의 조회 저장 외부 호출과 선택 Kafka 흐름을 확인한다`() {
        val kafkaCheck = System.getProperty("lab.kafka.check", "false").toBooleanStrictOrNull()
            ?: throw AssertionError("lab.kafka.check는 true 또는 false여야 합니다.")
        verify("앱 응답") {
            assertEquals(mapper.readTree("""{"status":"ok"}"""), request("GET", "/lab/ping"))
        }

        val prefix = "labcheck" + UUID.randomUUID().toString().replace("-", "").take(10)
        val rows = listOf(
            Triple("A", true, 100), Triple("B", false, 500),
            Triple("C", true, null), Triple("D", true, 700),
        ).map { (suffix, active, amount) ->
            request("POST", "/lab/items", mapOf("name" to prefix + suffix, "active" to active, "amount" to amount), 201)
        }
        verify("생성 후 버전 바인딩") {
            rows.forEach { assertTrue(number(it, "version") >= 0, "생성 응답에 0 이상의 버전이 필요합니다.") }
        }
        verify("목록·정렬·total") {
            val result = page(prefix)
            assertEquals(rows.map { text(it, "id") }, itemIds(result))
            assertEquals(4L, number(result, "total"))
        }
        verify("false 선택 조건") {
            val result = page(prefix, "active" to "false")
            assertEquals(listOf(text(rows[1], "id")), itemIds(result))
            assertEquals(1L, number(result, "total"))
        }
        listOf(rows[1], rows[3]).forEachIndexed { index, expected ->
            verify("금액 조건·페이지 $index") {
                val result = page(prefix, "minimum" to "400", "size" to "1", "page" to index.toString())
                assertEquals(listOf(text(expected, "id")), itemIds(result))
                assertEquals(2L, number(result, "total"))
            }
        }
        verify("마지막 페이지 이후에도 total 유지") {
            val result = page(prefix, "minimum" to "400", "size" to "1", "page" to "2")
            assertEquals(emptyList<String>(), itemIds(result))
            assertEquals(2L, number(result, "total"))
        }
        verify("nullable 금액 바인딩") {
            val result = request("GET", "/lab/items/" + text(rows[2], "id"))
            assertTrue(field(result, "amount").isNull, "null 금액을 0으로 바꾸면 안 됩니다.")
        }
        verify("입력 검증") {
            request("GET", "/lab/items?size=0", expectedStatus = 400)
            request("POST", "/lab/items", mapOf("name" to "   ", "active" to true), 400)
        }

        val item = rows.first()
        val itemId = text(item, "id")
        val path = "/lab/items/$itemId"
        verify("수정 요청의 필수 버전") {
            request("PATCH", "$path/name", mapOf("name" to "Missing version"), 400)
            request("PATCH", "$path/name", mapOf("name" to "Null version", "expectedVersion" to null), 400)
        }
        val changed = request("PATCH", "$path/name", mapOf("name" to prefix + "Updated", "expectedVersion" to number(item, "version")))
        val after = request("GET", path)
        verify("별도 GET에서 commit·버전 확인") {
            assertEquals(changed, after)
            assertTrue(number(after, "version") > number(item, "version"), "변경 후 버전이 증가해야 합니다.")
        }
        verify("flush 뒤 오류의 rollback") {
            request("PATCH", "$path/name", mapOf("name" to "Stale", "expectedVersion" to number(item, "version")), 409)
            request("PATCH", "$path/name", mapOf("name" to "Rejected", "expectedVersion" to number(after, "version"), "failAfterFlush" to true), 422)
            assertEquals(after, request("GET", path), "충돌·rollback 뒤에는 직전 commit 값이 유지되어야 합니다.")
        }

        verify("원격 HTTP 계약") {
            assertEquals(mapper.readTree("""{"item_name":"Alice"}"""), request("GET", "/lab/remote/items/alice"))
        }
        var refreshed = after
        verify("HTTP 값의 로컬 저장") {
            request("POST", "$path/refresh-name", mapOf("externalId" to "alice"), 400)
            request("POST", "$path/refresh-name", mapOf("externalId" to "alice", "expectedVersion" to null), 400)
            refreshed = request("POST", "$path/refresh-name", mapOf("externalId" to "alice", "expectedVersion" to number(after, "version")))
            assertEquals("Alice", text(refreshed, "name"))
            assertEquals(refreshed, request("GET", path))
        }
        verify("원격 null은 저장 없이 유지") {
            val unchanged = request("POST", "$path/refresh-name", mapOf("externalId" to "unchanged", "expectedVersion" to number(refreshed, "version")))
            assertEquals(refreshed, unchanged)
            assertEquals(refreshed, request("GET", path))
        }
        verify("원격 빈 이름 거절 후 유지") {
            request("POST", "$path/refresh-name", mapOf("externalId" to "blank", "expectedVersion" to number(refreshed, "version")), 400)
            assertEquals(refreshed, request("GET", path))
        }

        if (kafkaCheck) verifyKafka(itemId, path, refreshed)
        assertEquals(if (kafkaCheck) 19 else 16, passed.size)
        println("HTTP 실습 검증 ${passed.size}개 통과: 가상 Item 4개 생성, 접두어=$prefix")
        passed.forEachIndexed { index, label -> println("${index + 1}. $label") }
    }

    private fun verifyKafka(itemId: String, path: String, committed: JsonNode) {
        var observation: Pair<JsonNode, JsonNode>? = null
        verify("Kafka 실제 발행·수신") {
            observation = awaitReceived(itemId, number(committed, "version"))
            assertEquals(text(committed, "name"), text(observation!!.second, "name"))
        }
        val (response, observed) = checkNotNull(observation)
        verify("수신 저장소 계약") {
            assertEquals("MEMORY_ONLY", text(response, "storage"))
            assertEquals(mapper.valueToTree<JsonNode>(false), field(response, "survivesRestart"))
        }
        verify("rollback 뒤 DB·관측된 이벤트 상태 유지") {
            request("PATCH", "$path/name", mapOf("name" to "Kafka rejected", "expectedVersion" to number(committed, "version"), "failAfterFlush" to true), 422)
            // 1초 관찰 구간의 결과다. 미래의 이벤트 미발행까지 증명하는 테스트는 아니다.
            val deadline = System.nanoTime() + Duration.ofSeconds(1).toNanos()
            do {
                val latest = receivedItem(request("GET", "/lab/received-events"), itemId)
                assertEquals(observed, latest, "rollback한 버전이 수신 상태에 나타나면 안 됩니다.")
                Thread.sleep(100)
            } while (System.nanoTime() < deadline)
            assertEquals(committed, request("GET", path))
        }
    }

    private fun awaitReceived(itemId: String, expectedVersion: Long): Pair<JsonNode, JsonNode> {
        val deadline = System.nanoTime() + Duration.ofSeconds(20).toNanos()
        do {
            val response = request("GET", "/lab/received-events")
            assertEquals(mapper.valueToTree<JsonNode>(true), field(response, "kafkaEnabled"), "Kafka 검증에는 LAB_KAFKA_ENABLED=true로 실행한 앱이 필요합니다.")
            val observed = receivedItem(response, itemId)
            if (observed != null && number(observed, "sourceVersion") == expectedVersion) return response to observed
            Thread.sleep(250)
        } while (System.nanoTime() < deadline)
        throw AssertionError("20초 안에 항목 $itemId 의 Kafka 버전 $expectedVersion 을 수신하지 못했습니다. 브로커와 앱 발행 로그를 확인하세요.")
    }

    private fun receivedItem(response: JsonNode, itemId: String): JsonNode? =
        items(response).firstOrNull { text(it, "itemId") == itemId }

    private fun page(prefix: String, vararg filters: Pair<String, String>): JsonNode {
        val parameters = linkedMapOf("keyword" to prefix, "size" to "10").apply { putAll(filters) }
        val query = parameters.entries.joinToString("&") { (key, value) ->
            URLEncoder.encode(key, UTF_8) + "=" + URLEncoder.encode(value, UTF_8)
        }
        return request("GET", "/lab/items?$query")
    }

    private fun request(method: String, path: String, body: Any? = null, expectedStatus: Int = 200): JsonNode {
        val content = if (body == null) HttpRequest.BodyPublishers.noBody()
            else HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body), UTF_8)
        val request = HttpRequest.newBuilder(baseUri.resolve(path))
            .timeout(Duration.ofSeconds(20))
            .header("Accept", "application/json")
            .header("Content-Type", "application/json")
            .method(method, content)
            .build()
        val response = try {
            client.send(request, HttpResponse.BodyHandlers.ofString(UTF_8))
        } catch (failure: IOException) {
            throw AssertionError("$method $path: 학습 앱 $baseUri 에 HTTP로 연결하지 못했습니다. IntelliJ에서 앱을 먼저 실행하고 주소·포트를 확인하세요.", failure)
        } catch (failure: InterruptedException) {
            Thread.currentThread().interrupt()
            throw AssertionError("$method $path: HTTP 검증이 중단됐습니다.", failure)
        }
        assertEquals(expectedStatus, response.statusCode(), "$method $path: 예상 HTTP 상태와 실제 상태가 다릅니다.")
        return try {
            mapper.readTree(response.body()) ?: throw AssertionError("$method $path: JSON 응답 본문이 없습니다.")
        } catch (failure: RuntimeException) {
            throw AssertionError("$method $path: 응답 본문을 JSON으로 읽지 못했습니다.", failure)
        }
    }

    private fun verify(label: String, action: () -> Unit) {
        try {
            action()
            passed += label
        } catch (failure: AssertionError) {
            throw AssertionError("검증 실패 [$label]: ${failure.message}", failure)
        }
    }

    private fun field(node: JsonNode, name: String): JsonNode = node.get(name)
        ?: throw AssertionError("JSON 응답에 필수 필드 '$name'이 없습니다.")

    private fun text(node: JsonNode, name: String): String {
        val value = field(node, name)
        assertTrue(value.isString, "'$name'은 문자열이어야 합니다.")
        return value.stringValue()
    }

    private fun number(node: JsonNode, name: String): Long {
        val value = field(node, name)
        assertTrue(value.isIntegralNumber, "'$name'은 null이 아닌 정수여야 합니다.")
        return value.longValue()
    }

    private fun items(node: JsonNode): List<JsonNode> {
        val value = field(node, "items")
        assertTrue(value.isArray, "'items'는 배열이어야 합니다.")
        return value.toList()
    }

    private fun itemIds(node: JsonNode): List<String> = items(node).map { text(it, "id") }

    private fun loopbackUri(value: String): URI {
        val uri = try {
            URI.create(value)
        } catch (failure: IllegalArgumentException) {
            throw AssertionError("lab.baseUrl이 올바른 HTTP 주소가 아닙니다.", failure)
        }
        val host = uri.host?.removeSurrounding("[", "]")?.lowercase()
        assertTrue(
            uri.scheme == "http" && host in setOf("127.0.0.1", "localhost", "::1") &&
                uri.rawPath in setOf("", "/") && uri.rawQuery == null && uri.rawFragment == null && uri.rawUserInfo == null,
            "lab.baseUrl에는 경로·인증정보가 없는 loopback HTTP 주소만 지정할 수 있습니다. 예: http://127.0.0.1:58080",
        )
        return uri
    }
}
