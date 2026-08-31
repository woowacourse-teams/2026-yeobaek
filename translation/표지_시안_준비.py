# -*- coding: utf-8 -*-
"""책 표지 시안용 ImageGen 프롬프트 준비 및 결과 검증 도구."""

from __future__ import annotations

import argparse
import hashlib
import json
import struct
import sys
import zlib
from pathlib import Path
from typing import Any, Iterable


if hasattr(sys.stdout, "reconfigure"):
    sys.stdout.reconfigure(encoding="utf-8")
    sys.stderr.reconfigure(encoding="utf-8")


VARIANTS = (
    (
        "A",
        "감성적 수채화 일러스트",
        "illustration-story",
        "손으로 그린 듯한 섬세한 수채화로 서정적인 장면이나 상징을 표현한다. "
        "부드러운 파스텔과 안정적인 톤온톤 배색, 종이의 은은한 결, 번지는 안료의 깊이를 살린다. "
        "우아하고 문학적인 세리프 또는 명조 계열 타이포그래피를 사용하고, 넓은 여백의 중앙 정렬이나 "
        "균형 잡힌 비대칭 구도로 따뜻하고 몽환적인 정서를 만든다.",
    ),
    (
        "B",
        "현대적 타이포그래피",
        "stylized-concept",
        "제목 자체를 가장 강력한 이미지로 삼고 도형, 선, 면, 패턴을 결합한다. 초대형 볼드 산세리프 또는 "
        "고딕 계열 글자를 자르고 겹치거나 회전시키는 과감한 편집 구도를 사용하되 제목 전체는 명확히 "
        "판독되어야 한다. 비비드 컬러와 대비색으로 현대적이고 명료하며 에너지 넘치는 인상을 만든다.",
    ),
    (
        "C",
        "직관적인 실사 오브제",
        "photorealistic-natural",
        "책을 대표하는 인물, 장소 또는 시그니처 오브제를 고급 편집 사진처럼 사실적으로 표현한다. 핵심 "
        "소재를 극단적으로 클로즈업하고 얕은 심도, 미니멀한 배경, 깊고 영화적인 색감과 풍부한 명암을 "
        "사용한다. 절제된 산세리프나 가는 현대적 서체를 작게 배치해 몰입감 있고 세련된 서사를 전달한다.",
    ),
    (
        "D",
        "클래식 아카이브",
        "stylized-concept",
        "양장 고서의 빈티지 질감, 박물관 도록, 세밀화와 동판화의 조형 언어에서 영감을 얻되 기존 작품을 "
        "복제하지 않는 독창적인 그래픽을 만든다. 딥 그린, 버건디 또는 네이비를 바탕으로 골드와 크림을 "
        "포인트로 쓰고, 전통적 세리프·명조 또는 수제 활자 느낌과 장식 프레임을 대칭적으로 구성해 지적이고 "
        "고전적인 깊이와 신뢰감을 준다.",
    ),
    (
        "E",
        "키치 팝아트",
        "stylized-concept",
        "굵은 외곽선의 2D 키치 일러스트, 콜라주 또는 장난감 같은 3D 오브제를 활용한다. 네온과 강한 "
        "원색 대비를 조화롭게 통제하고, 볼드 산세리프나 레트로 그래픽 글자를 기울이거나 왜곡·중첩한다. "
        "요소가 화면을 가득 채우는 높은 밀도와 과감한 크기 대비로 트렌디하고 유쾌하며 도발적인 에너지를 만든다.",
    ),
    (
        "F",
        "텍스처 오브제 프레임",
        "photorealistic-natural",
        "책을 상징하는 단 하나의 오브제를 중앙에 극단적으로 클로즈업하고 재질, 표면, 빛과 그림자를 "
        "정교하게 강조한다. 흑백 또는 모노톤에 단 하나의 포인트 컬러만 쓰며, 오브제와 넓은 여백만으로 "
        "긴장감을 만든다. 극소형의 절제된 서체를 여백에 배치하거나 오브제와 섬세하게 겹쳐 세련되고 감각적인 "
        "고요함을 표현한다.",
    ),
    (
        "G",
        "시각적 은유와 그래픽 폼",
        "stylized-concept",
        "책의 핵심 주제를 도형, 네거티브 스페이스, 착시, 게슈탈트 또는 상징적 그래픽으로 은유한다. 의미 있는 "
        "두세 가지 색만 사용하고 글자가 선, 도형, 공간 구조의 일부로 자연스럽게 결합되게 한다. 한 번 더 "
        "보아야 의미가 드러나는 정교한 시각 구조로 철학적이고 개념적이며 해석의 여지가 있는 인상을 만든다.",
    ),
)

EXPECTED_IDS = tuple(item[0] for item in VARIANTS)
PNG_SIGNATURE = b"\x89PNG\r\n\x1a\n"
MAX_DECOMPRESSED_PNG_BYTES = 512 * 1024 * 1024


def _write_utf8(path: Path, text: str) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("w", encoding="utf-8", newline="\n") as stream:
        stream.write(text)


def _read_json(path: Path, label: str, errors: list[str]) -> Any:
    try:
        return json.loads(path.read_text(encoding="utf-8-sig"))
    except FileNotFoundError:
        errors.append(f"{label} 파일이 없습니다: {path}")
    except OSError as exc:
        errors.append(f"{label} 파일을 읽을 수 없습니다: {path} ({exc})")
    except json.JSONDecodeError as exc:
        errors.append(f"{label} JSON 형식이 잘못되었습니다: {path} ({exc.msg}, {exc.lineno}행)")
    return None


def _non_empty_string(value: Any) -> bool:
    return isinstance(value, str) and bool(value.strip())


def _validate_string_array(
    value: Any,
    field: str,
    errors: list[str],
    minimum: int | None = None,
    maximum: int | None = None,
    allow_empty: bool = False,
) -> None:
    if not isinstance(value, list):
        errors.append(f"브리프의 {field}는 문자열 배열이어야 합니다.")
        return
    if minimum is not None and len(value) < minimum:
        errors.append(f"브리프의 {field}는 최소 {minimum}개여야 합니다 (현재 {len(value)}개).")
    if maximum is not None and len(value) > maximum:
        errors.append(f"브리프의 {field}는 최대 {maximum}개여야 합니다 (현재 {len(value)}개).")
    for index, item in enumerate(value, 1):
        if not isinstance(item, str) or (not allow_empty and not item.strip()):
            errors.append(f"브리프의 {field}[{index}]는 비어 있지 않은 문자열이어야 합니다.")


def validate_inputs(meta: Any, brief: Any) -> list[str]:
    """메타데이터와 표지 브리프의 오류를 모두 반환한다."""
    errors: list[str] = []
    if not isinstance(meta, dict):
        errors.append("메타데이터 최상위 값은 객체여야 합니다.")
    else:
        if not _non_empty_string(meta.get("title")):
            errors.append("메타데이터의 title은 비어 있지 않은 문자열이어야 합니다.")
        authors = meta.get("authors")
        if not isinstance(authors, list) or not authors:
            errors.append("메타데이터의 authors는 한 명 이상을 담은 배열이어야 합니다.")
        else:
            for index, author in enumerate(authors, 1):
                if not isinstance(author, dict) or not _non_empty_string(author.get("name")):
                    errors.append(
                        f"메타데이터의 authors[{index}].name은 비어 있지 않은 문자열이어야 합니다."
                    )

    if not isinstance(brief, dict):
        errors.append("표지 브리프 최상위 값은 객체여야 합니다.")
    else:
        if not _non_empty_string(brief.get("genre")):
            errors.append("브리프의 genre는 비어 있지 않은 문자열이어야 합니다.")
        _validate_string_array(brief.get("coreSummary"), "coreSummary", errors, 2, 5)
        _validate_string_array(brief.get("keywords"), "keywords", errors, 3, 7)
        _validate_string_array(brief.get("visualMotifs"), "visualMotifs", errors, 1)
        if not _non_empty_string(brief.get("targetAudience")):
            errors.append("브리프의 targetAudience는 비어 있지 않은 문자열이어야 합니다.")
        if "avoid" in brief:
            _validate_string_array(brief["avoid"], "avoid", errors)
    return errors


def _brief_digest(brief: dict[str, Any]) -> str:
    canonical = json.dumps(brief, ensure_ascii=False, sort_keys=True, separators=(",", ":"))
    return hashlib.sha256(canonical.encode("utf-8")).hexdigest()


def _quoted(value: str) -> str:
    return json.dumps(value, ensure_ascii=False)


def build_prompt(meta: dict[str, Any], brief: dict[str, Any], variant: tuple[str, str, str, str]) -> str:
    variant_id, label, taxonomy, style = variant
    title = meta["title"]
    author_text = " · ".join(author["name"] for author in meta["authors"])
    avoid = brief.get("avoid", [])
    avoid_text = ", ".join(avoid) if avoid else "별도 지정 없음"
    return (
        f"Use case: {taxonomy}\n"
        "Asset type: 실제 출판용 책 앞표지\n"
        f"{variant_id}안 — {label}\n\n"
        "세로 2:3 비율(1024×1536 이상)의 실제 출판용 앞표지 한 장을 완성 이미지로 제작한다. "
        "책 목업이 아니라 재단된 앞표지 평면 디자인만 화면 전체에 보여 준다.\n\n"
        "도서 해석:\n"
        f"- 장르: {brief['genre']}\n"
        f"- 핵심 내용: {' '.join(brief['coreSummary'])}\n"
        f"- 핵심 키워드: {', '.join(brief['keywords'])}\n"
        f"- 주요 인물·장소·오브제: {', '.join(brief['visualMotifs'])}\n"
        f"- 주요 독자층: {brief['targetAudience']}\n"
        f"- 추가로 피할 요소: {avoid_text}\n\n"
        f"스타일 지시: {style}\n"
        "내용과 정서를 분석해 핵심 장면, 상징, 인물 또는 시그니처 오브제를 이 스타일에 맞게 새롭게 "
        "해석한다. 기존 출간 표지나 특정 작가의 작품을 모방하지 말고 독창적인 디자인으로 제작한다.\n\n"
        "표지에 인쇄할 텍스트는 다음 두 항목뿐이며, 따옴표 안 문자열을 한 글자도 바꾸지 말고 정확히 쓴다:\n"
        f"- 제목: {_quoted(title)}\n"
        f"- 작가명: {_quoted(author_text)}\n"
        "제목과 작가명을 번역·축약·변형하지 않는다. 다른 문구, 부제, 추천사, 의미 없는 문자, 가짜 로고를 "
        "추가하지 않는다. 두 텍스트가 주요 비주얼에 가려지지 않도록 충분한 대비와 가독성을 확보한다.\n\n"
        "금지: 책 목업, 펼쳐진 책, 책등, 뒷표지, 바코드, 가격표, 출판사 로고, 손, 표지 바깥의 배경 공간, "
        "프레임 밖 소품, 워터마크, 추가 텍스트. 전문 북 디자이너가 제작한 인쇄물처럼 균형 잡힌 정보 계층, "
        "선명한 디테일, 정교한 마감과 출판 가능한 완성도를 적용한다.\n"
    )


def expected_outputs(meta: dict[str, Any], brief: dict[str, Any]) -> tuple[dict[str, str], dict[str, Any]]:
    prompts = {variant[0]: build_prompt(meta, brief, variant) for variant in VARIANTS}
    author_text = " · ".join(author["name"] for author in meta["authors"])
    manifest = {
        "schemaVersion": 1,
        "title": meta["title"],
        "authorText": author_text,
        "briefDigest": _brief_digest(brief),
        "variants": [
            {
                "id": variant_id,
                "label": label,
                "prompt": f"프롬프트/{variant_id}.txt",
                "output": f"{variant_id}.png",
            }
            for variant_id, label, _taxonomy, _style in VARIANTS
        ],
        "requirements": {
            "width": 1024,
            "height": 1536,
            "ratio": "2:3",
            "exactCount": 7,
        },
    }
    return prompts, manifest


def prepare(meta: dict[str, Any], brief: dict[str, Any], output_dir: Path) -> None:
    prompts, manifest = expected_outputs(meta, brief)
    prompt_dir = output_dir / "프롬프트"
    prompt_dir.mkdir(parents=True, exist_ok=True)
    for variant_id, prompt in prompts.items():
        _write_utf8(prompt_dir / f"{variant_id}.txt", prompt)
    _write_utf8(
        output_dir / "시안_목록.json",
        json.dumps(manifest, ensure_ascii=False, indent=2) + "\n",
    )


def _validate_prepared_files(
    output_dir: Path, prompts: dict[str, str], expected_manifest: dict[str, Any]
) -> list[str]:
    errors: list[str] = []
    prompt_dir = output_dir / "프롬프트"
    expected_prompt_names = {f"{variant_id}.txt" for variant_id in EXPECTED_IDS}
    try:
        prompt_entries = list(prompt_dir.iterdir())
    except FileNotFoundError:
        prompt_entries = []
    except OSError as exc:
        prompt_entries = []
        errors.append(f"프롬프트 폴더를 읽을 수 없습니다: {prompt_dir} ({exc})")
    for path in sorted(prompt_entries, key=lambda item: item.name):
        if not path.is_file() or path.name not in expected_prompt_names:
            errors.append(f"허용되지 않은 프롬프트 폴더 항목입니다: {path}")

    for variant_id, expected in prompts.items():
        path = prompt_dir / f"{variant_id}.txt"
        try:
            actual = path.read_text(encoding="utf-8-sig")
        except FileNotFoundError:
            errors.append(f"프롬프트 파일이 없습니다: {path}")
        except OSError as exc:
            errors.append(f"프롬프트 파일을 읽을 수 없습니다: {path} ({exc})")
        else:
            if actual != expected:
                errors.append(f"현재 메타데이터/브리프와 프롬프트가 일치하지 않습니다: {path}")

    manifest_errors: list[str] = []
    manifest = _read_json(output_dir / "시안_목록.json", "시안 목록", manifest_errors)
    errors.extend(manifest_errors)
    if manifest is not None and manifest != expected_manifest:
        errors.append("시안_목록.json이 현재 메타데이터/브리프의 기대값과 일치하지 않습니다.")
    return errors


def _scanline_passes(
    width: int, height: int, bits_per_pixel: int, interlace: int
) -> Iterable[tuple[int, int]]:
    if interlace == 0:
        row_bytes = (width * bits_per_pixel + 7) // 8
        yield row_bytes, height
        return

    # Adam7: x 시작, y 시작, x 간격, y 간격
    for x_start, y_start, x_step, y_step in (
        (0, 0, 8, 8),
        (4, 0, 8, 8),
        (0, 4, 4, 8),
        (2, 0, 4, 4),
        (0, 2, 2, 4),
        (1, 0, 2, 2),
        (0, 1, 1, 2),
    ):
        pass_width = 0 if width <= x_start else (width - x_start + x_step - 1) // x_step
        pass_height = 0 if height <= y_start else (height - y_start + y_step - 1) // y_step
        if pass_width == 0 or pass_height == 0:
            continue
        row_bytes = (pass_width * bits_per_pixel + 7) // 8
        yield row_bytes, pass_height


def _scanline_data_lengths(
    width: int, height: int, bits_per_pixel: int, interlace: int
) -> Iterable[int]:
    for row_bytes, row_count in _scanline_passes(width, height, bits_per_pixel, interlace):
        for _ in range(row_count):
            yield row_bytes


class _ScanlineValidator:
    def __init__(self, width: int, height: int, bits_per_pixel: int, interlace: int) -> None:
        self._rows = iter(_scanline_data_lengths(width, height, bits_per_pixel, interlace))
        self.expected_size = sum(
            (row_bytes + 1) * row_count
            for row_bytes, row_count in _scanline_passes(width, height, bits_per_pixel, interlace)
        )
        if self.expected_size > MAX_DECOMPRESSED_PNG_BYTES:
            raise ValueError(
                "IHDR 기준 압축 해제 예상 크기가 안전 상한을 넘습니다: "
                f"{self.expected_size}바이트"
            )
        self.produced = 0
        self._row_remaining = 0
        self._needs_filter = False
        self._done = False
        self._advance_row()

    def _advance_row(self) -> None:
        try:
            self._row_remaining = next(self._rows)
            self._needs_filter = True
        except StopIteration:
            self._done = True
            self._row_remaining = 0
            self._needs_filter = False

    def consume(self, data: bytes) -> None:
        offset = 0
        while offset < len(data):
            if self._done:
                raise ValueError("IDAT 압축 해제 결과가 IHDR 예상 크기를 초과합니다")
            if self._needs_filter:
                filter_type = data[offset]
                if filter_type > 4:
                    raise ValueError(f"PNG 스캔라인 필터 값이 유효하지 않습니다: {filter_type}")
                offset += 1
                self.produced += 1
                self._needs_filter = False
            available = len(data) - offset
            consumed = min(available, self._row_remaining)
            offset += consumed
            self.produced += consumed
            self._row_remaining -= consumed
            if self._row_remaining == 0:
                self._advance_row()

    def finish(self) -> None:
        if not self._done or self.produced != self.expected_size:
            raise ValueError(
                "IDAT 압축 해제 결과의 스캔라인 길이가 IHDR 예상값과 일치하지 않습니다: "
                f"{self.produced}/{self.expected_size}바이트"
            )


def _feed_idat(
    decompressor: zlib.Decompress, validator: _ScanlineValidator, compressed: bytes
) -> None:
    if decompressor.eof and compressed:
        raise ValueError("IDAT zlib 스트림 뒤에 추가 압축 데이터가 있습니다")
    pending = compressed
    while pending:
        allowance = validator.expected_size - validator.produced
        output_limit = min(64 * 1024, allowance + 1)
        previous_length = len(pending)
        try:
            output = decompressor.decompress(pending, output_limit)
        except zlib.error as exc:
            raise ValueError(f"IDAT zlib 압축 스트림이 손상되었습니다: {exc}") from exc
        pending = decompressor.unconsumed_tail
        if decompressor.unused_data:
            raise ValueError("IDAT zlib 스트림 뒤에 trailing compressed data가 있습니다")
        validator.consume(output)
        if decompressor.eof:
            if pending:
                raise ValueError("IDAT zlib 스트림 뒤에 추가 압축 데이터가 있습니다")
            break
        if pending and len(pending) == previous_length and not output:
            raise ValueError("IDAT zlib 압축 스트림을 더 이상 해제할 수 없습니다")


def _png_dimensions(path: Path) -> tuple[int, int]:
    """PNG 전체 청크 스트림을 검증하고 IHDR 크기를 반환한다."""
    with path.open("rb") as stream:
        if stream.read(8) != PNG_SIGNATURE:
            raise ValueError("PNG 시그니처가 올바르지 않습니다")

        chunk_index = 0
        ihdr_count = 0
        idat_count = 0
        iend_count = 0
        width = height = 0
        scanlines: _ScanlineValidator | None = None
        decompressor: zlib.Decompress | None = None
        idat_ended = False
        while True:
            chunk_header = stream.read(8)
            if not chunk_header:
                raise ValueError("IEND 청크 전에 파일이 끝났습니다")
            if len(chunk_header) != 8:
                raise ValueError("PNG 청크 헤더가 잘렸습니다")
            length, chunk_type = struct.unpack(">I4s", chunk_header)
            if not all(65 <= byte <= 90 or 97 <= byte <= 122 for byte in chunk_type):
                raise ValueError("PNG 청크 타입이 유효하지 않습니다")
            data = stream.read(length)
            crc_bytes = stream.read(4)
            if len(data) != length or len(crc_bytes) != 4:
                raise ValueError(f"{chunk_type.decode('ascii')} 청크가 잘렸습니다")
            expected_crc = struct.unpack(">I", crc_bytes)[0]
            actual_crc = zlib.crc32(chunk_type)
            actual_crc = zlib.crc32(data, actual_crc) & 0xFFFFFFFF
            if actual_crc != expected_crc:
                raise ValueError(f"{chunk_type.decode('ascii')} 청크 CRC가 올바르지 않습니다")

            chunk_index += 1
            if chunk_type == b"IHDR":
                ihdr_count += 1
                if chunk_index != 1:
                    raise ValueError("IHDR은 첫 번째 청크여야 합니다")
                if ihdr_count != 1:
                    raise ValueError("IHDR 청크가 중복되었습니다")
                if length != 13:
                    raise ValueError("IHDR 청크 길이는 13이어야 합니다")
                width, height, bit_depth, color_type, compression, filter_method, interlace = (
                    struct.unpack(">IIBBBBB", data)
                )
                if width == 0 or height == 0:
                    raise ValueError("IHDR 너비와 높이는 양수여야 합니다")
                valid_depths = {
                    0: {1, 2, 4, 8, 16},
                    2: {8, 16},
                    3: {1, 2, 4, 8},
                    4: {8, 16},
                    6: {8, 16},
                }
                if color_type not in valid_depths or bit_depth not in valid_depths[color_type]:
                    raise ValueError(
                        f"IHDR 색상 유형/비트 깊이 조합이 유효하지 않습니다: {color_type}/{bit_depth}"
                    )
                if compression != 0 or filter_method != 0 or interlace not in (0, 1):
                    raise ValueError("IHDR 압축·필터·인터레이스 값이 유효하지 않습니다")
                channels = {0: 1, 2: 3, 3: 1, 4: 2, 6: 4}[color_type]
                scanlines = _ScanlineValidator(
                    width, height, bit_depth * channels, interlace
                )
                decompressor = zlib.decompressobj()
            elif chunk_type == b"IDAT":
                if scanlines is None or decompressor is None:
                    raise ValueError("IDAT보다 먼저 유효한 IHDR이 있어야 합니다")
                if idat_ended:
                    raise ValueError("IDAT 청크는 중간에 다른 청크 없이 연속되어야 합니다")
                idat_count += 1
                _feed_idat(decompressor, scanlines, data)
            elif chunk_type == b"IEND":
                iend_count += 1
                if length != 0:
                    raise ValueError("IEND 청크 길이는 0이어야 합니다")
                if iend_count != 1:
                    raise ValueError("IEND 청크가 중복되었습니다")
                if stream.read(1):
                    raise ValueError("IEND 뒤에 불필요한 바이트가 있습니다")
                break
            elif chunk_type[:1].isupper() and chunk_type not in {b"PLTE"}:
                raise ValueError(f"알 수 없는 필수 PNG 청크입니다: {chunk_type.decode('ascii')}")
            elif idat_count:
                idat_ended = True

        if ihdr_count != 1:
            raise ValueError("IHDR 청크가 정확히 하나 있어야 합니다")
        if idat_count < 1:
            raise ValueError("IDAT 청크가 하나 이상 있어야 합니다")
        if decompressor is None or not decompressor.eof:
            raise ValueError("IDAT zlib 압축 스트림이 끝까지 완료되지 않았습니다")
        if scanlines is None:
            raise ValueError("PNG 스캔라인 정보를 만들 수 없습니다")
        scanlines.finish()
        if iend_count != 1:
            raise ValueError("IEND 청크가 정확히 하나 있어야 합니다")
        return width, height


def _sha256_file(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as stream:
        for chunk in iter(lambda: stream.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def _validate_output_layout(output_dir: Path) -> list[str]:
    errors: list[str] = []
    allowed_files = (
        {f"{variant_id}.png" for variant_id in EXPECTED_IDS}
        | {"시안_목록.json", "표지_검수.json"}
    )
    allowed_directories = {"프롬프트", "작업"}
    try:
        entries = list(output_dir.iterdir())
    except FileNotFoundError:
        return [f"표지 출력 폴더가 없습니다: {output_dir}"]
    except OSError as exc:
        return [f"표지 출력 폴더를 읽을 수 없습니다: {output_dir} ({exc})"]
    for path in sorted(entries, key=lambda item: item.name):
        if path.is_file() and path.name in allowed_files:
            continue
        if path.is_dir() and path.name in allowed_directories:
            continue
        errors.append(f"허용되지 않은 표지 출력 항목입니다: {path}")
    return errors


def _validate_images(output_dir: Path) -> list[str]:
    errors: list[str] = []
    expected_names = {f"{variant_id}.png" for variant_id in EXPECTED_IDS}
    try:
        root_pngs = {
            path.name for path in output_dir.iterdir() if path.is_file() and path.suffix.lower() == ".png"
        }
    except FileNotFoundError:
        return [f"표지 출력 폴더가 없습니다: {output_dir}"]
    except OSError as exc:
        return [f"표지 출력 폴더를 읽을 수 없습니다: {output_dir} ({exc})"]

    for name in sorted(expected_names - root_pngs):
        errors.append(f"필수 표지 이미지가 없습니다: {output_dir / name}")
    hashes: dict[str, list[str]] = {}
    for name in sorted(expected_names & root_pngs):
        path = output_dir / name
        try:
            width, height = _png_dimensions(path)
        except (OSError, ValueError, struct.error) as exc:
            errors.append(f"{name} PNG 구조 오류: {exc}")
            continue
        if width < 1024 or height < 1536:
            errors.append(f"{name} 해상도가 최소 1024×1536보다 작습니다: {width}×{height}")
        if width * 3 != height * 2:
            errors.append(f"{name} 비율이 정확한 2:3이 아닙니다: {width}×{height}")
        try:
            file_hash = _sha256_file(path)
        except OSError as exc:
            errors.append(f"{name} 파일 해시를 계산할 수 없습니다: {exc}")
        else:
            hashes.setdefault(file_hash, []).append(name)

    for names in hashes.values():
        if len(names) > 1:
            errors.append(f"표지 이미지 파일 내용이 중복됩니다: {', '.join(names)}")
    return errors


def _all_true_fields(value: Any, fields: Iterable[str], prefix: str, errors: list[str]) -> None:
    if not isinstance(value, dict):
        errors.append(f"{prefix}는 객체여야 합니다.")
        return
    for field in fields:
        if value.get(field) is not True:
            errors.append(f"{prefix}.{field}가 true가 아닙니다.")


def _reject_unexpected_keys(
    value: dict[str, Any], allowed: set[str], prefix: str, errors: list[str]
) -> None:
    for field in sorted(set(value) - allowed):
        errors.append(f"{prefix}에 허용되지 않은 키가 있습니다: {field}")


def _validate_review(output_dir: Path) -> list[str]:
    errors: list[str] = []
    review = _read_json(output_dir / "표지_검수.json", "표지 검수", errors)
    if review is None:
        return errors
    if not isinstance(review, dict):
        errors.append("표지_검수.json 최상위 값은 객체여야 합니다.")
        return errors
    _reject_unexpected_keys(review, {"schemaVersion", "variants", "setChecks"}, "표지_검수.json", errors)
    if review.get("schemaVersion") != 1:
        errors.append("표지_검수.json의 schemaVersion은 1이어야 합니다.")
    variants = review.get("variants")
    if not isinstance(variants, dict):
        errors.append("표지_검수.json의 variants는 A~G 객체를 담은 객체여야 합니다.")
    else:
        expected_ids = set(EXPECTED_IDS)
        actual_ids = set(variants)
        for variant_id in sorted(expected_ids - actual_ids):
            errors.append(f"표지_검수.json에 {variant_id}안 검수 결과가 없습니다.")
        for variant_id in sorted(actual_ids - expected_ids):
            errors.append(f"표지_검수.json에 허용되지 않은 시안 키가 있습니다: {variant_id}")
        for variant_id in EXPECTED_IDS:
            if variant_id in variants:
                value = variants[variant_id]
                if not isinstance(value, dict):
                    errors.append(f"variants.{variant_id}는 객체여야 합니다.")
                    continue
                boolean_fields = (
                    "textExact",
                    "frontCoverOnly",
                    "styleMatch",
                    "legible",
                    "publicationReady",
                )
                hash_fields = {"imageSha256", "promptSha256"}
                _reject_unexpected_keys(
                    value,
                    set(boolean_fields) | hash_fields | {"notes"},
                    f"variants.{variant_id}",
                    errors,
                )
                _all_true_fields(
                    value,
                    boolean_fields,
                    f"variants.{variant_id}",
                    errors,
                )
                if "notes" in value and not isinstance(value["notes"], str):
                    errors.append(f"variants.{variant_id}.notes는 문자열이어야 합니다.")
                for field, path in (
                    ("imageSha256", output_dir / f"{variant_id}.png"),
                    ("promptSha256", output_dir / "프롬프트" / f"{variant_id}.txt"),
                ):
                    try:
                        current_hash = _sha256_file(path)
                    except OSError as exc:
                        errors.append(f"variants.{variant_id}.{field} 비교 대상 파일을 읽을 수 없습니다: {exc}")
                    else:
                        if value.get(field) != current_hash:
                            errors.append(
                                f"variants.{variant_id}.{field}가 현재 파일 SHA-256과 일치하지 않습니다."
                            )
    set_checks = review.get("setChecks")
    if isinstance(set_checks, dict):
        _reject_unexpected_keys(
            set_checks,
            {"visuallyDistinct", "noExtraTextAcrossSet"},
            "setChecks",
            errors,
        )
    _all_true_fields(set_checks, ("visuallyDistinct", "noExtraTextAcrossSet"), "setChecks", errors)
    return errors


def validate_result(
    meta: dict[str, Any], brief: dict[str, Any], output_dir: Path, structural_only: bool
) -> list[str]:
    prompts, manifest = expected_outputs(meta, brief)
    errors = _validate_output_layout(output_dir)
    errors.extend(_validate_prepared_files(output_dir, prompts, manifest))
    errors.extend(_validate_images(output_dir))
    if not structural_only:
        errors.extend(_validate_review(output_dir))
    return errors


def _load_and_validate(meta_path: Path, brief_path: Path) -> tuple[Any, Any, list[str]]:
    errors: list[str] = []
    meta = _read_json(meta_path, "메타데이터", errors)
    brief = _read_json(brief_path, "표지 브리프", errors)
    if meta is not None and brief is not None:
        errors.extend(validate_inputs(meta, brief))
    return meta, brief, errors


def _print_errors(errors: list[str]) -> None:
    print("[표지 시안 검증 실패]")
    for error in errors:
        print(f"  - {error}")


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(description="책 표지 시안 프롬프트 준비 및 결과 검증")
    subparsers = parser.add_subparsers(dest="command", required=True)
    for command, help_text in (
        ("prepare", "A~G 표지 시안 프롬프트와 시안 목록을 생성"),
        ("validate", "프롬프트, 이미지, 수동 검수 결과를 검증"),
    ):
        subparser = subparsers.add_parser(command, help=help_text)
        subparser.add_argument("--meta", required=True, type=Path, help="메타데이터.json 경로")
        subparser.add_argument("--brief", required=True, type=Path, help="표지_브리프.json 경로")
        subparser.add_argument("--output-dir", required=True, type=Path, help="표지 출력 폴더")
        if command == "validate":
            subparser.add_argument(
                "--structural-only",
                action="store_true",
                help="표지_검수.json의 수동 시각 검수 게이트를 생략",
            )
    return parser


def main(argv: list[str] | None = None) -> int:
    args = build_parser().parse_args(argv)
    meta, brief, errors = _load_and_validate(args.meta, args.brief)
    if errors:
        _print_errors(errors)
        return 1

    if args.command == "prepare":
        try:
            prepare(meta, brief, args.output_dir)
        except OSError as exc:
            _print_errors([f"표지 준비 산출물을 저장할 수 없습니다: {exc}"])
            return 1
        print(f"[표지 시안 준비 완료] 프롬프트 7개와 시안 목록을 생성했습니다: {args.output_dir}")
        print("기존 표지 이미지 파일은 변경하지 않았습니다.")
        return 0

    errors = validate_result(meta, brief, args.output_dir, args.structural_only)
    if errors:
        _print_errors(errors)
        return 1
    review_text = "구조 검증" if args.structural_only else "구조 및 수동 검수"
    print(f"[표지 시안 검증 통과] A~G 7개, 2:3, 최소 1024×1536, 중복 없음, {review_text} 통과")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
