#!/usr/bin/env python3
"""실습 단계의 명시된 파일만 새 디렉터리에 복사한다. 서비스는 실행하지 않는다."""

from __future__ import annotations

import argparse
import json
import shutil
import sys
from pathlib import Path, PurePosixPath


LAB_DIR = Path(__file__).resolve().parent
REFERENCE_DIR = LAB_DIR / "reference"
MANIFEST_PATH = LAB_DIR / "steps.json"
ROOT_FILES = {
    "settings.gradle.kts",
    "build.gradle.kts",
    "gradlew",
    "gradlew.bat",
    "gradle/wrapper/gradle-wrapper.properties",
    "gradle/wrapper/gradle-wrapper.jar",
    ".gitignore",
    "compose.yaml",
    ".env.example",
}
FORBIDDEN_PARTS = {"build", ".gradle", ".git", ".idea", "__pycache__"}


def validate_relative_path(value: object) -> PurePosixPath:
    if not isinstance(value, str) or not value or "\\" in value:
        raise ValueError("manifest 파일 경로는 /로 구분한 상대경로여야 합니다.")
    path = PurePosixPath(value)
    if path.is_absolute() or ".." in path.parts or path.as_posix() != value:
        raise ValueError(f"허용하지 않는 파일 경로: {value}")
    if FORBIDDEN_PARTS.intersection(path.parts):
        raise ValueError(f"빌드 결과·도구 상태는 복사하지 않습니다: {value}")
    if any(part.startswith(".env") for part in path.parts) and value != ".env.example":
        raise ValueError("실제 .env와 환경별 비밀 파일은 복사하지 않습니다.")

    allowed = value in ROOT_FILES
    allowed = allowed or (
        path.parts[:3] in (("src", "main", "kotlin"), ("src", "test", "kotlin"))
        and path.suffix == ".kt"
    )
    allowed = allowed or (
        path.parts[:3] == ("src", "main", "resources")
        and path.suffix in {".yml", ".yaml", ".xml"}
    )
    if not allowed:
        raise ValueError(f"실습 원본 파일의 허용 범위 밖입니다: {value}")
    return path


def load_files() -> list[tuple[int, PurePosixPath, Path]]:
    """선택하지 않은 단계까지 포함해 모든 입력을 쓰기 전에 검사한다."""
    if REFERENCE_DIR.is_symlink() or not REFERENCE_DIR.is_dir():
        raise ValueError("reference는 실제 원본 디렉터리여야 합니다.")
    reference = REFERENCE_DIR.resolve(strict=True)
    manifest = json.loads(MANIFEST_PATH.read_text(encoding="utf-8"))
    if (
        not isinstance(manifest, dict)
        or type(manifest.get("schemaVersion")) is not int
        or manifest["schemaVersion"] != 1
    ):
        raise ValueError("지원하지 않는 manifest 형식입니다.")
    steps = manifest.get("steps")
    if not isinstance(steps, list) or len(steps) != 8:
        raise ValueError("manifest에는 0~7단계가 순서대로 있어야 합니다.")

    seen: set[str] = set()
    result: list[tuple[int, PurePosixPath, Path]] = []
    for expected, step in enumerate(steps):
        if not isinstance(step, dict) or type(step.get("number")) is not int:
            raise ValueError("각 단계에는 정수 number가 필요합니다.")
        if step["number"] != expected:
            raise ValueError("manifest의 단계 번호는 0~7 순서여야 합니다.")
        if not isinstance(step.get("title"), str) or not step["title"].strip():
            raise ValueError(f"{expected}단계에 제목이 없습니다.")
        files = step.get("files")
        if not isinstance(files, list) or not files:
            raise ValueError(f"{expected}단계에 파일 목록이 없습니다.")
        for value in files:
            relative = validate_relative_path(value)
            if value in seen:
                raise ValueError(f"manifest에 중복 파일이 있습니다: {value}")
            seen.add(value)
            source = reference.joinpath(*relative.parts)
            cursor = reference
            for part in relative.parts:
                cursor = cursor / part
                if cursor.is_symlink():
                    raise ValueError(f"심볼릭 링크는 복사하지 않습니다: {value}")
            resolved = source.resolve(strict=True)
            try:
                resolved.relative_to(reference)
            except ValueError as exception:
                raise ValueError(f"reference 밖의 파일입니다: {value}") from exception
            if not resolved.is_file():
                raise ValueError(f"일반 파일이 아닙니다: {value}")
            result.append((expected, relative, resolved))

    # 새 소스를 추가하고 manifest 갱신을 빠뜨렸을 때 불완전한 프로젝트를 만들지 않는다.
    source_roots = ("src/main/kotlin", "src/main/resources", "src/test/kotlin")
    for source_root in source_roots:
        for source in sorted((reference / source_root).rglob("*")):
            if source.is_file() and source.relative_to(reference).as_posix() not in seen:
                raise ValueError(
                    "manifest에서 누락한 원본 파일: " + source.relative_to(reference).as_posix()
                )
    return result


def main() -> int:
    parser = argparse.ArgumentParser(
        description="학습용 Gradle 프로젝트를 새 폴더에 만듭니다. 기존 폴더는 변경하지 않습니다."
    )
    parser.add_argument("destination", type=Path, metavar="DEST", help="아직 존재하지 않는 새 폴더")
    parser.add_argument(
        "--through",
        type=int,
        choices=range(8),
        default=0,
        metavar="0..7",
        help="이 단계까지 누적 복사합니다. 기본값 0은 Gradle 뼈대만 만듭니다.",
    )
    args = parser.parse_args()
    destination = args.destination.expanduser().absolute()
    created = False
    try:
        if destination.exists() or destination.is_symlink():
            raise ValueError(f"대상이 이미 존재합니다. 빈 폴더도 덮어쓰지 않습니다: {destination}")
        inputs = load_files()
        selected = [(relative, source) for number, relative, source in inputs if number <= args.through]

        # manifest의 모든 입력 검사가 끝난 뒤에만 새 디렉터리를 만든다.
        destination.mkdir(parents=True, exist_ok=False)
        created = True
        for relative, source in selected:
            target = destination.joinpath(*relative.parts)
            target.parent.mkdir(parents=True, exist_ok=True)
            if target.exists() or target.is_symlink():
                raise ValueError(f"복사 대상이 이미 존재하여 중단합니다: {target}")
            # wrapper 실행 권한을 포함한 원본 파일 모드를 보존한다.
            shutil.copy2(source, target)
    except (OSError, ValueError) as exception:
        print(f"오류: {exception}", file=sys.stderr)
        if created:
            print("새 폴더에 일부 파일이 남아 있을 수 있습니다. 자동 삭제하지 않습니다.", file=sys.stderr)
        return 1

    print(f"생성 완료: {destination}")
    print(f"0~{args.through}단계, {len(selected)}개 파일을 복사했습니다.")
    print(".env는 생성하지 않았으며 DB·Docker·Gradle·앱을 실행하지 않았습니다.")
    print(f"다음 학습 안내: {LAB_DIR.parent / '11-build-your-own-lab.md'}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
