#!/usr/bin/env python3
"""실행 중인 로컬 학습 앱을 HTTP로 확인한다. 가상 Item 네 개를 새로 만든다."""

import argparse
import json
import time
import uuid
from urllib.error import HTTPError
from urllib.parse import urlencode, urlparse
from urllib.request import Request, urlopen


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--base-url", default="http://127.0.0.1:58080")
    parser.add_argument("--phase", type=int, choices=(4, 5, 6), default=5)
    args = parser.parse_args()
    parsed = urlparse(args.base_url)
    if parsed.scheme != "http" or parsed.hostname not in ("127.0.0.1", "localhost") or parsed.path not in ("", "/"):
        parser.error("학습 앱의 localhost HTTP 주소만 지정할 수 있습니다.")
    base = args.base_url.rstrip("/")
    checks = []

    def request(method, path, body=None, expected=200):
        data = None if body is None else json.dumps(body).encode()
        req = Request(base + path, data=data, method=method, headers={"Content-Type": "application/json"})
        try:
            with urlopen(req, timeout=20) as response:
                status, raw = response.status, response.read()
        except HTTPError as error:
            status, raw = error.code, error.read()
        if status != expected:
            raise AssertionError(f"{method} {path}: HTTP {expected} 예상, 실제 {status}")
        return json.loads(raw) if raw else None

    def check(condition, label):
        if not condition:
            raise AssertionError(label)
        checks.append(label)

    check(request("GET", "/lab/ping") == {"status": "ok"}, "앱 응답")
    prefix = "labcheck" + uuid.uuid4().hex[:10]
    rows = []
    for suffix, active, amount in (("A", True, 100), ("B", False, 500), ("C", True, None), ("D", True, 700)):
        rows.append(request("POST", "/lab/items", {"name": prefix + suffix, "active": active, "amount": amount}, 201))
    check(all(row["version"] is not None for row in rows), "생성 후 버전 바인딩")

    def page(**filters):
        return request("GET", "/lab/items?" + urlencode({"keyword": prefix, "size": 10, **filters}))

    result = page()
    check([x["id"] for x in result["items"]] == [x["id"] for x in rows] and result["total"] == 4, "목록·정렬·total")
    result = page(active="false")
    check([x["id"] for x in result["items"]] == [rows[1]["id"]] and result["total"] == 1, "false 선택 조건")
    for index, expected_item in enumerate((rows[1], rows[3])):
        result = page(minimum=400, size=1, page=index)
        check(result["total"] == 2 and [x["id"] for x in result["items"]] == [expected_item["id"]], f"금액 조건·페이지 {index}")
    result = page(minimum=400, size=1, page=2)
    check(result["items"] == [] and result["total"] == 2, "마지막 페이지 이후에도 total 유지")
    check(request("GET", "/lab/items/" + rows[2]["id"])["amount"] is None, "nullable 금액 바인딩")
    request("GET", "/lab/items?size=0", expected=400)
    request("POST", "/lab/items", {"name": "   ", "active": True}, expected=400)
    checks.append("입력 검증")

    item = rows[0]
    path = "/lab/items/" + item["id"]
    request("PATCH", path + "/name", {"name": "Missing version"}, expected=400)
    request("PATCH", path + "/name", {"name": "Null version", "expectedVersion": None}, expected=400)
    checks.append("수정 요청의 필수 버전")
    changed = request("PATCH", path + "/name", {"name": prefix + "Updated", "expectedVersion": item["version"]})
    after = request("GET", path)
    check(after == changed and after["version"] > item["version"], "별도 GET에서 commit·버전 확인")
    request("PATCH", path + "/name", {"name": "Stale", "expectedVersion": item["version"]}, expected=409)
    request("PATCH", path + "/name", {"name": "Rejected", "expectedVersion": after["version"], "failAfterFlush": True}, expected=422)
    check(request("GET", path) == after, "flush 뒤 오류의 rollback")

    if args.phase >= 5:
        check(request("GET", "/lab/remote/items/alice") == {"item_name": "Alice"}, "원격 HTTP 계약")
        request("POST", path + "/refresh-name", {"externalId": "alice"}, expected=400)
        request("POST", path + "/refresh-name", {"externalId": "alice", "expectedVersion": None}, expected=400)
        refreshed = request("POST", path + "/refresh-name", {"externalId": "alice", "expectedVersion": after["version"]})
        check(refreshed["name"] == "Alice" and request("GET", path) == refreshed, "HTTP 값의 로컬 저장")
        unchanged = request("POST", path + "/refresh-name", {"externalId": "unchanged", "expectedVersion": refreshed["version"]})
        check(unchanged == refreshed, "원격 null은 저장 없이 유지")
        request("POST", path + "/refresh-name", {"externalId": "blank", "expectedVersion": refreshed["version"]}, expected=400)
        check(request("GET", path) == refreshed, "원격 빈 이름 거절 후 유지")
        after = refreshed

    if args.phase >= 6:
        observed = None
        deadline = time.monotonic() + 20
        while time.monotonic() < deadline:
            response = request("GET", "/lab/received-events")
            if not response["kafkaEnabled"]:
                raise AssertionError("6단계에는 LAB_KAFKA_ENABLED=true가 필요합니다.")
            observed = next((x for x in response["items"] if x["itemId"] == item["id"]), None)
            if observed and observed["sourceVersion"] == after["version"]:
                break
            time.sleep(0.25)
        check(observed is not None and observed["name"] == after["name"] and observed["sourceVersion"] == after["version"], "Kafka 실제 발행·수신")
        check(response["storage"] == "MEMORY_ONLY" and response["survivesRestart"] is False, "수신 저장소 계약")
        request("PATCH", path + "/name", {"name": "Kafka rejected", "expectedVersion": after["version"], "failAfterFlush": True}, expected=422)
        time.sleep(1)
        response = request("GET", "/lab/received-events")
        last = next(x for x in response["items"] if x["itemId"] == item["id"])
        check(last == observed and request("GET", path) == after, "rollback 뒤 DB·관측된 이벤트 상태 유지")

    print(json.dumps({"phase": args.phase, "passed": len(checks), "checks": checks, "createdItems": 4}, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
