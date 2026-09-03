# -*- coding: utf-8 -*-
"""번역본(마크다운) → 문장 단위 서버 인제스트 JSON 변환 + 원문 문단 수 대조 검증.

최종 보관용 ZIP에는 표지 브리프와 검수를 통과한 표지 산출물도 함께 보관한다.
표지 생성·검증 자체는 `표지_하네스.md`와 `표지_시안_준비.py`가 담당한다.

번역본 형식(번역_지침.md의 [4. 구조 보존 규칙]):
  - 각 장은 `## 제목` 단독 줄로 시작
  - 문단은 빈 줄로 구분, 원문 문단과 1:1
  - 각 문단은 공백·개행을 보존한 sentences 배열로 분리하고 재결합을 검증

사용법:
  python 인제스트_변환.py --normalize-legacy-ingest 구형_ingest.json
      [--meta 메타데이터.json] [-o 현재_ingest.json] [--backup-output 원본_백업.json]
      구형 passages[].content를 현재 passages[].sentences[].content로 변환한다.
      -o를 생략하면 원본 옆에 .bak 백업을 만든 뒤 제자리에서 원자적으로 교체한다.

  python 인제스트_변환.py 번역본.md [번역본2.md ...]
      --original 원문.txt
      [--meta 메타데이터.json]
      [--chapter-re 정규식]   원문 장 제목 패턴 (기본: 로마숫자 "I. 제목")
      [-o ingest.json]
      [--zip-output 책이름.zip]   보관용 ZIP 경로 (생략 시 메타데이터 title 기반)
      [--partial]     완료된 장까지만 대조 (진행 중 검증용)
      [--no-verify]   원문 대조를 건너뜀
      [--force]       검증 실패해도 JSON을 출력

  python 인제스트_변환.py --list-chapters --original 원문.txt [--chapter-re 정규식]
      [--max-chunk-paragraphs 150] [--max-chunk-characters 24000]
      [--max-chunk-estimated-tokens 8000]
      원문의 장 제목·라인 범위·문단·문자·보수적 추정 토큰 수와 권장 청크를
      출력한다 (번역 시작 전 작업량 파악·분할용).

번역본 파일을 여러 개 주면 인자 순서대로 이어 붙여 처리한다(장 단위 분할 번역 지원).
"""
import argparse
import copy
import json
import os
import re
import tempfile
import subprocess
import sys
import zipfile
from pathlib import Path

if hasattr(sys.stdout, "reconfigure"):
    sys.stdout.reconfigure(encoding="utf-8")
    sys.stderr.reconfigure(encoding="utf-8")

# 기본값: 열 0에서 시작하는 로마숫자 장 제목 ("I. Silver Blaze" 형태)
# 다른 형식의 책은 --chapter-re로 교체한다. 예: "^CHAPTER [IVXLCDM]+" , "^Chapter \d+"
DEFAULT_CHAPTER_RE = r"^[IVXLCDM]+\.\s+\S"

SENTENCE_TERMINATORS = ".?!。？！…"
SENTENCE_CLOSERS = "\"'’”)]}〉》」』】〕］｝）〗〙〛｠»›"
MAX_SENTENCE_CONTENT_BYTES = 65_535
DEFAULT_CHUNK_MAX_PARAGRAPHS = 150
DEFAULT_CHUNK_MAX_CHARACTERS = 24_000
DEFAULT_CHUNK_MAX_TOKENS = 8_000
TITLE_PREFIX_ABBREVIATIONS = {
    "mr.", "mrs.", "ms.", "dr.", "prof.", "rev.", "hon.",
}
CERTAIN_INLINE_ABBREVIATIONS = {"e.g.", "i.e."}
AMBIGUOUS_ABBREVIATIONS = {
    "a.m.", "p.m.", "u.s.", "u.k.", "jr.", "sr.", "st.",
}


class LegacyNormalizationError(ValueError):
    """구형 ingest JSON을 안전하게 정규화할 수 없을 때 발생한다."""


def _looks_like_lowercase_hostname(value):
    """고정 TLD 목록 없이 소문자 ASCII 호스트명 형태만 인정한다."""
    labels = value.split(".")
    if len(labels) < 2 or len(labels[-1]) < 2:
        return False
    return all(
        label
        and label[0].isalnum()
        and label[-1].isalnum()
        and all(char.isascii() and (char.islower() or char.isdigit() or char == "-")
                for char in label)
        for label in labels
    )


def _period_is_internal(text, index):
    """마침표가 소수점·약어·이니셜 내부인지 보수적으로 판정한다."""
    previous = text[index - 1] if index else ""
    following = text[index + 1] if index + 1 < len(text) else ""
    if previous.isdigit() and following.isdigit():
        return True
    if previous.isdigit():
        next_nonspace = text[index + 1:].lstrip()
        if next_nonspace and next_nonspace[0].isdigit():
            return True
        if following and following.isalpha():
            return True
    if (previous.isascii() and previous.isalpha()
            and following.isascii() and following.isalpha()):
        if index + 2 < len(text) and text[index + 2] == ".":
            return True
        domain_start = index
        while domain_start > 0 and (
            (text[domain_start - 1].isascii() and text[domain_start - 1].isalnum())
            or text[domain_start - 1] in ".-"
        ):
            domain_start -= 1
        domain_end = index + 1
        while domain_end < len(text) and (
            (text[domain_end].isascii() and text[domain_end].isalnum())
            or text[domain_end] in ".-"
        ):
            domain_end += 1
        domain = text[domain_start:domain_end].strip(".").lower()
        raw_domain = text[domain_start:domain_end].strip(".")
        if raw_domain == domain and _looks_like_lowercase_hostname(domain):
            return True

    token_start = index
    while token_start > 0 and (text[token_start - 1].isalpha() or text[token_start - 1] == "."):
        token_start -= 1
    token = text[token_start:index + 1]
    lowered = token.lower()
    next_nonspace = text[index + 1:].lstrip(SENTENCE_CLOSERS).lstrip()
    if lowered in TITLE_PREFIX_ABBREVIATIONS:
        return bool(next_nonspace)
    if lowered == "no.":
        return bool(next_nonspace and next_nonspace[0].isdigit())
    if lowered in CERTAIN_INLINE_ABBREVIATIONS:
        return True
    if lowered in AMBIGUOUS_ABBREVIATIONS:
        return bool(next_nonspace and next_nonspace[0].isascii()
                    and next_nonspace[0].isupper())
    if lowered in {"etc.", "vs."}:
        return bool(next_nonspace and next_nonspace[0].isascii()
                    and next_nonspace[0].islower())
    if re.fullmatch(r"(?:[A-Za-z]\.){2,}", token):
        return bool(next_nonspace)
    if re.fullmatch(r"[A-Z]\.", token):
        return bool(next_nonspace and next_nonspace[0].isalpha())
    return False


def find_ambiguous_abbreviation(text):
    """영문 대문자 앞에서 내부/문장끝을 확정할 수 없는 약어를 찾는다."""
    alternatives = "|".join(
        re.escape(abbreviation)
        for abbreviation in sorted(AMBIGUOUS_ABBREVIATIONS, key=len, reverse=True)
    )
    pattern = re.compile(rf"(?i)(?<![A-Za-z])(?P<abbr>{alternatives})")
    for match in pattern.finditer(text):
        boundary = match.end()
        while boundary < len(text) and text[boundary] in SENTENCE_CLOSERS:
            boundary += 1
        whitespace_start = boundary
        while boundary < len(text) and text[boundary].isspace():
            boundary += 1
        if (boundary > whitespace_start and boundary < len(text)
                and text[boundary].isascii() and text[boundary].isupper()):
            return match.group("abbr")
    return None


def split_sentences(text):
    """문단을 문장으로 나누되 모든 문자를 정확히 한 문장에 보존한다.

    종결부호 뒤의 닫는 따옴표/괄호와 다음 문장 전 공백·개행은 앞 문장에
    포함한다. 확실한 경계가 아니면 나머지 전체를 마지막 문장으로 둔다.
    """
    if not text:
        return [text]

    sentences = []
    start = 0
    index = 0
    while index < len(text):
        if text[index] not in SENTENCE_TERMINATORS:
            index += 1
            continue
        if text[index] == "." and _period_is_internal(text, index):
            index += 1
            continue

        boundary = index + 1
        while boundary < len(text) and text[boundary] in SENTENCE_TERMINATORS:
            boundary += 1
        while boundary < len(text) and text[boundary] in SENTENCE_CLOSERS:
            boundary += 1
        while boundary < len(text) and text[boundary].isspace():
            boundary += 1

        if boundary < len(text):
            sentences.append(text[start:boundary])
            start = boundary
        index = boundary

    if start < len(text) or not sentences:
        sentences.append(text[start:])
    return sentences


def normalize_legacy_book(book):
    """구형 passages[].content를 현재 sentences 배열로 정규화한다.

    문단 문자열은 변경하지 않고 분할된 문장을 이었을 때 UTF-8 바이트까지
    원래 값과 같은지 확인한다. 이미 현재 구조이거나 구/신 구조가 섞인 입력은
    실수로 재처리하지 않도록 거부한다.
    """
    if not isinstance(book, dict):
        raise LegacyNormalizationError("최상위 JSON 값이 객체가 아닙니다.")
    _validate_legacy_metadata(book, "구형 ingest JSON")
    chapters = book.get("chapters")
    if not isinstance(chapters, list) or not chapters:
        raise LegacyNormalizationError("chapters가 비어 있거나 배열이 아닙니다.")

    paragraph_count = 0
    sentence_count = 0
    for chapter_index, chapter in enumerate(chapters, 1):
        if not isinstance(chapter, dict):
            raise LegacyNormalizationError(
                f"{chapter_index}번째 목차가 객체가 아닙니다."
            )
        passages = chapter.get("passages")
        if not isinstance(passages, list) or not passages:
            raise LegacyNormalizationError(
                f"{chapter_index}번째 목차의 passages가 비어 있거나 배열이 아닙니다."
            )
        for passage_index, passage in enumerate(passages, 1):
            label = f"{chapter_index}번째 목차 {passage_index}번째 문단"
            if not isinstance(passage, dict):
                raise LegacyNormalizationError(f"{label}이 객체가 아닙니다.")
            has_content = "content" in passage
            has_sentences = "sentences" in passage
            if has_content and has_sentences:
                raise LegacyNormalizationError(
                    f"{label}에 content와 sentences가 함께 있습니다(혼합 구조)."
                )
            if has_sentences:
                raise LegacyNormalizationError(
                    f"{label}이 이미 현재 sentences 구조입니다. 구형 입력만 허용합니다."
                )
            if not has_content:
                raise LegacyNormalizationError(f"{label}에 content가 없습니다.")
            content = passage["content"]
            if not isinstance(content, str):
                raise LegacyNormalizationError(f"{label}의 content가 문자열이 아닙니다.")
            if not content.strip():
                raise LegacyNormalizationError(f"{label}의 content가 비어 있습니다.")

            sentence_contents = split_sentences(content)
            rejoined = "".join(sentence_contents)
            if rejoined != content or rejoined.encode("utf-8") != content.encode("utf-8"):
                raise AssertionError(f"{label}의 문장 재결합 결과가 원래 문단과 다릅니다.")
            passage.pop("content")
            passage["sentences"] = [
                {"content": sentence} for sentence in sentence_contents
            ]
            paragraph_count += 1
            sentence_count += len(sentence_contents)

    errors = validate(book)
    if errors:
        formatted = "\n".join(f"  - {error}" for error in errors)
        raise LegacyNormalizationError(f"현재 인제스트 규격 검증 실패:\n{formatted}")
    return paragraph_count, sentence_count


def _fsync_parent_directory(path):
    """지원하는 플랫폼에서는 디렉터리 엔트리 변경도 디스크에 반영한다."""
    flags = os.O_RDONLY | getattr(os, "O_DIRECTORY", 0)
    try:
        directory_fd = os.open(path.parent, flags)
    except OSError:
        return
    try:
        os.fsync(directory_fd)
    except OSError:
        pass
    finally:
        os.close(directory_fd)


def _atomic_publish_backup(path, data):
    """동일 디렉터리 임시 파일을 완전히 기록한 뒤 백업을 배타적으로 공개한다."""
    temporary_path = None
    try:
        with tempfile.NamedTemporaryFile(
            mode="wb",
            dir=path.parent,
            prefix=f".{path.name}.",
            suffix=".tmp",
            delete=False,
        ) as temporary_file:
            temporary_path = Path(temporary_file.name)
            temporary_file.write(data)
            temporary_file.flush()
            os.fsync(temporary_file.fileno())

        # 같은 파일시스템의 hard link 생성은 원자적이며 기존 path를 덮어쓰지
        # 않는다. os.link는 Windows에서도 동일한 배타적 생성 의미를 제공한다.
        os.link(temporary_path, path)
        _fsync_parent_directory(path)
    finally:
        if temporary_path is not None and temporary_path.exists():
            temporary_path.unlink()


def _atomic_write_text(path, text):
    """대상과 같은 디렉터리에 임시 파일을 쓴 뒤 원자적으로 교체한다."""
    path = path.resolve()
    if not path.parent.is_dir():
        raise LegacyNormalizationError(f"출력 디렉터리가 없습니다: {path.parent}")
    temporary_path = None
    try:
        with tempfile.NamedTemporaryFile(
            mode="w",
            encoding="utf-8",
            newline="",
            dir=path.parent,
            prefix=f".{path.name}.",
            suffix=".tmp",
            delete=False,
        ) as temporary_file:
            temporary_path = Path(temporary_file.name)
            temporary_file.write(text)
            temporary_file.flush()
            os.fsync(temporary_file.fileno())
        os.replace(temporary_path, path)
    finally:
        if temporary_path is not None and temporary_path.exists():
            temporary_path.unlink()


def apply_legacy_metadata(book, metadata):
    """검증된 메타데이터의 서지 필드만 적용하고 본문은 건드리지 않는다."""
    if not isinstance(book, dict):
        raise LegacyNormalizationError("구형 ingest JSON의 최상위 값이 객체가 아닙니다.")
    if not isinstance(metadata, dict):
        raise LegacyNormalizationError("메타데이터 JSON의 최상위 값이 객체가 아닙니다.")
    _validate_legacy_metadata(metadata, "메타데이터")
    required_fields = ("title", "publisher", "publishedYear", "authors")
    for field in required_fields:
        book[field] = copy.deepcopy(metadata[field])


def _validate_legacy_metadata(book, label):
    """정규화 전에 최상위 서지 필드의 존재와 타입을 안전하게 확인한다."""
    required_fields = ("title", "publisher", "publishedYear", "authors")
    missing = [field for field in required_fields if field not in book]
    if missing:
        raise LegacyNormalizationError(
            f"{label}에 필수 필드가 없습니다: " + ", ".join(missing)
        )

    for field in ("title", "publisher"):
        value = book[field]
        if not isinstance(value, str):
            raise LegacyNormalizationError(f"{label}의 {field}이 문자열이 아닙니다.")
    if not isinstance(book["publishedYear"], int) or isinstance(book["publishedYear"], bool):
        raise LegacyNormalizationError(f"{label}의 publishedYear가 정수가 아닙니다.")

    authors = book["authors"]
    if not isinstance(authors, list):
        raise LegacyNormalizationError(f"{label}의 authors가 배열이 아닙니다.")
    for author_index, author in enumerate(authors, 1):
        if not isinstance(author, dict):
            raise LegacyNormalizationError(
                f"{label}의 {author_index}번째 작가 정보가 객체가 아닙니다."
            )
        for field in ("name", "isni"):
            if field in author and not isinstance(author[field], str):
                raise LegacyNormalizationError(
                    f"{label}의 {author_index}번째 작가 {field}가 문자열이 아닙니다."
                )


def _load_json_file(path, label):
    try:
        return json.loads(path.read_bytes().decode("utf-8-sig"))
    except (UnicodeDecodeError, json.JSONDecodeError) as error:
        raise LegacyNormalizationError(f"{label} UTF-8 JSON을 읽을 수 없습니다: {error}") from error


def normalize_legacy_ingest_file(source, output=None, backup=None, metadata_path=None):
    """구형 ingest 파일을 검증 후 저장하며 제자리 변환 시 원본을 백업한다."""
    source = source.resolve()
    output = (output or source).resolve()
    if not source.is_file():
        raise LegacyNormalizationError(f"구형 ingest JSON 파일이 없습니다: {source}")
    in_place = output == source
    if backup is not None and not in_place:
        raise LegacyNormalizationError("--backup-output은 제자리 변환에서만 사용할 수 있습니다.")
    backup = (backup or source.with_name(f"{source.name}.bak")).resolve() if in_place else None
    if backup == source:
        raise LegacyNormalizationError("백업 경로는 입력 파일과 달라야 합니다.")
    if backup is not None and not backup.parent.is_dir():
        raise LegacyNormalizationError(f"백업 디렉터리가 없습니다: {backup.parent}")
    if backup is not None and backup.exists():
        raise LegacyNormalizationError(f"기존 백업을 덮어쓰지 않습니다: {backup}")

    source_bytes = source.read_bytes()
    try:
        book = json.loads(source_bytes.decode("utf-8-sig"))
    except (UnicodeDecodeError, json.JSONDecodeError) as error:
        raise LegacyNormalizationError(f"구형 ingest UTF-8 JSON을 읽을 수 없습니다: {error}") from error
    if metadata_path is not None:
        metadata_path = metadata_path.resolve()
        if not metadata_path.is_file():
            raise LegacyNormalizationError(f"메타데이터 파일이 없습니다: {metadata_path}")
        apply_legacy_metadata(book, _load_json_file(metadata_path, "메타데이터"))

    paragraph_count, sentence_count = normalize_legacy_book(book)
    serialized = json.dumps(book, ensure_ascii=False, indent=2) + "\n"
    if backup is not None:
        try:
            _atomic_publish_backup(backup, source_bytes)
        except FileExistsError as error:
            raise LegacyNormalizationError(f"기존 백업을 덮어쓰지 않습니다: {backup}") from error
        except OSError as error:
            raise LegacyNormalizationError(f"원본 백업을 안전하게 만들 수 없습니다: {error}") from error
    try:
        _atomic_write_text(output, serialized)
    except Exception:
        if backup is not None and backup.exists() and not output.exists():
            os.replace(backup, output)
        raise
    return output, backup, paragraph_count, sentence_count


def estimate_tokens(text):
    """언어 비종속 작업량 추정치(ASCII 연속 구간 4자 + 비ASCII 1자)."""
    ascii_runs = re.findall(r"[\x21-\x7e]+", text)
    ascii_tokens = sum((len(run) + 3) // 4 for run in ascii_runs)
    non_ascii_nonspace = sum(not char.isascii() and not char.isspace() for char in text)
    return ascii_tokens + non_ascii_nonspace


def plan_chunks(paragraph_metrics, max_paragraphs=DEFAULT_CHUNK_MAX_PARAGRAPHS,
                max_characters=DEFAULT_CHUNK_MAX_CHARACTERS,
                max_tokens=DEFAULT_CHUNK_MAX_TOKENS):
    """문단을 쪼개지 않고 어느 한도든 넘기 직전에 끊는 청크 계획을 만든다."""
    limits = (max_paragraphs, max_characters, max_tokens)
    if any(limit <= 0 for limit in limits):
        raise ValueError("청크 한도는 모두 1 이상이어야 합니다.")

    chunks = []
    current = None
    for paragraph_number, metrics in enumerate(paragraph_metrics, 1):
        characters = metrics["characters"]
        tokens = metrics["estimatedTokens"]
        exceeds_if_added = current is not None and (
            current["paragraphs"] + 1 > max_paragraphs
            or current["characters"] + characters > max_characters
            or current["estimatedTokens"] + tokens > max_tokens
        )
        if exceeds_if_added:
            chunks.append(current)
            current = None
        if current is None:
            current = {
                "paragraphStart": paragraph_number,
                "paragraphEnd": paragraph_number,
                "paragraphs": 0,
                "characters": 0,
                "estimatedTokens": 0,
            }
        current["paragraphEnd"] = paragraph_number
        current["paragraphs"] += 1
        current["characters"] += characters
        current["estimatedTokens"] += tokens
    if current is not None:
        chunks.append(current)
    return chunks


def plan_work_assignments(chapters, max_paragraphs=DEFAULT_CHUNK_MAX_PARAGRAPHS,
                          max_characters=DEFAULT_CHUNK_MAX_CHARACTERS,
                          max_tokens=DEFAULT_CHUNK_MAX_TOKENS):
    """장별 청크를 순서대로 묶어 작업자 한 명당 합산 한도를 지킨다."""
    work_items = []
    for chapter_index, chapter in enumerate(chapters, 1):
        for chunk in plan_chunks(
            chapter["metrics"]["paragraphs"],
            max_paragraphs,
            max_characters,
            max_tokens,
        ):
            work_items.append({
                "chapterIndex": chapter_index,
                "chapterTitle": chapter["title"],
                **chunk,
            })

    assignments = []
    current = None
    for item in work_items:
        exceeds_if_added = current is not None and (
            current["paragraphs"] + item["paragraphs"] > max_paragraphs
            or current["characters"] + item["characters"] > max_characters
            or current["estimatedTokens"] + item["estimatedTokens"] > max_tokens
        )
        if exceeds_if_added:
            assignments.append(current)
            current = None
        if current is None:
            current = {
                "items": [],
                "paragraphs": 0,
                "characters": 0,
                "estimatedTokens": 0,
            }
        current["items"].append(item)
        current["paragraphs"] += item["paragraphs"]
        current["characters"] += item["characters"]
        current["estimatedTokens"] += item["estimatedTokens"]
    if current is not None:
        assignments.append(current)
    return assignments


def parse_original(path, chapter_re):
    """원문 장 정보와 호환 가능한 작업량 metrics를 만든다.

    기존 소비자를 위해 title/count/start/end 키를 그대로 유지하고, metrics에
    문자 수·추정 토큰 수·문단별 작업량을 추가한다.

    start/end는 원문 파일 기준 1-based 라인 번호(장 제목 줄 ~ 다음 장 직전).
    Gutenberg 텍스트의 *** START/END 마커 바깥은 무시한다.
    """
    lines = path.read_text(encoding="utf-8-sig").splitlines()

    start, end = 0, len(lines)
    for i, ln in enumerate(lines):
        if ln.startswith("*** START"):
            start = i + 1
        elif ln.startswith("*** END"):
            end = i
            break

    chapters = []
    current = None
    block = []

    def flush_paragraph():
        nonlocal block
        if current is None or not block:
            block = []
            return
        paragraph = "\n".join(block)
        current["count"] += 1
        current["metrics"]["characters"] += len(paragraph)
        tokens = estimate_tokens(paragraph)
        current["metrics"]["estimatedTokens"] += tokens
        current["metrics"]["paragraphs"].append({
            "characters": len(paragraph),
            "estimatedTokens": tokens,
        })
        block = []

    for i in range(start, end):
        ln = lines[i]
        if chapter_re.match(ln):
            flush_paragraph()
            if current is not None:
                current["end"] = i  # 직전 줄까지 (1-based로 i)
            current = {
                "title": ln.strip(),
                "count": 0,
                "start": i + 1,
                "end": end,
                "metrics": {"characters": 0, "estimatedTokens": 0, "paragraphs": []},
            }
            chapters.append(current)
            continue
        if ln.strip():
            if current is not None:
                block.append(ln)
        else:
            flush_paragraph()
    flush_paragraph()
    return chapters


def parse_translation(paths):
    """번역본에서 chapters 배열을 만든다. → passages[].sentences[].content"""
    chapters = []
    current = None
    for path in paths:
        block = []
        # 마지막 문단 flush를 위해 빈 줄 하나를 덧붙여 순회
        for lineno, ln in enumerate(path.read_text(encoding="utf-8-sig").splitlines() + [""], 1):
            if ln.startswith("## "):
                if block:
                    sys.exit(f"[오류] {path.name}:{lineno} — 문단과 장 제목 사이에 빈 줄이 없습니다.")
                current = {"title": ln[3:].strip(), "passages": []}
                chapters.append(current)
            elif ln.strip():
                block.append(ln)
            elif block:
                if current is None:
                    sys.exit(f"[오류] {path.name}:{lineno} — 장 제목(## ...) 없이 본문이 시작됩니다: "
                             f"{block[0][:40]}...")
                paragraph = "\n".join(block)
                ambiguous = find_ambiguous_abbreviation(paragraph)
                if ambiguous:
                    sys.exit(
                        f"[오류] {path.name}:{lineno} — 약어 '{ambiguous}' 뒤의 문장 경계가 "
                        "모호합니다. 약어를 풀어 쓰거나 문장을 다시 표현해 경계를 "
                        "명확히 하세요."
                    )
                sentence_contents = split_sentences(paragraph)
                if "".join(sentence_contents) != paragraph:
                    raise AssertionError("문장 분리 후 재결합한 내용이 원래 문단과 다릅니다.")
                current["passages"].append({
                    "sentences": [{"content": sentence} for sentence in sentence_contents]
                })
                block = []
    return chapters


def _check_len(errors, label, value):
    if not (1 <= len(value) <= 100):
        errors.append(f"{label}이(가) 1~100자 범위를 벗어남 ({len(value)}자): {value[:50]}")


def validate_chapters(chapters):
    """full/partial에서 공통으로 쓰는 장·문단·문장 구조 검증."""
    errors = []
    if not chapters:
        errors.append("chapters가 비어 있음")
    for i, ch in enumerate(chapters, 1):
        _check_len(errors, f"{i}번째 목차 제목", ch.get("title", ""))
        passages = ch.get("passages", [])
        if not passages:
            errors.append(f"{i}번째 목차 '{ch.get('title', '')}'에 본문이 없음")
        for j, passage in enumerate(passages, 1):
            sentences = passage.get("sentences", [])
            if not sentences:
                errors.append(f"{i}번째 목차 {j}번째 문단의 sentences가 비어 있음")
            for k, sentence in enumerate(sentences, 1):
                content = sentence.get("content", "") if isinstance(sentence, dict) else ""
                if not content.strip():
                    errors.append(f"{i}번째 목차 {j}번째 문단 {k}번째 문장이 공백임")
                    continue
                content_bytes = len(content.encode("utf-8"))
                if content_bytes > MAX_SENTENCE_CONTENT_BYTES:
                    errors.append(
                        f"{i}번째 목차 {j}번째 문단 {k}번째 문장이 "
                        f"UTF-8 {MAX_SENTENCE_CONTENT_BYTES:,}바이트를 초과함 "
                        f"({content_bytes}바이트)"
                    )
    return errors


def validate(book):
    """인제스트 규격(docs/인제스트_가이드.md) 검증. → 오류 메시지 목록"""
    errors = []

    _check_len(errors, "도서 제목", book.get("title", ""))
    _check_len(errors, "출판사", book.get("publisher", ""))
    if not book.get("authors"):
        errors.append("authors가 비어 있음")
    for i, a in enumerate(book.get("authors", []), 1):
        if not isinstance(a, dict):
            errors.append(f"{i}번째 작가 정보가 객체가 아님")
            continue
        _check_len(errors, f"{i}번째 작가명", a.get("name", ""))
        isni = str(a.get("isni", "")).strip()
        if not isni:
            errors.append(f"{i}번째 작가 ISNI가 비어 있음: {a.get('name', '')}")
        else:
            normalized_isni = re.sub(r"[\s-]", "", isni).upper()
            if not re.fullmatch(r"\d{15}[\dX]", normalized_isni):
                errors.append(
                    f"{i}번째 작가 ISNI 형식 오류(16자리, 마지막은 숫자 또는 X): {isni}"
                )
            else:
                # ISO/IEC 7064 MOD 11-2: 15자리 데이터와 체크 문자의
                # 가중합이 1 (mod 11)이 되도록 한다.
                weights = (10, 5, 8, 4, 2, 1, 6, 3, 7, 9, 10, 5, 8, 4, 2)
                total = sum(int(digit) * weight
                            for digit, weight in zip(normalized_isni[:15], weights))
                expected = (1 - total) % 11
                actual = 10 if normalized_isni[-1] == "X" else int(normalized_isni[-1])
                if actual != expected:
                    errors.append(f"{i}번째 작가 ISNI 체크디짓 불일치: {isni}")
    errors.extend(validate_chapters(book.get("chapters", [])))
    return errors


BCP47_TAG = (
    r"(?:[A-Za-z]{2,8}(?:-[A-Za-z0-9]{1,8})*"
    r"|[A-Za-z](?:-[A-Za-z0-9]{1,8})+"
    r"|x(?:-[A-Za-z0-9]{1,8})+)"
)
SOURCE_LANGUAGE_LINE_RE = re.compile(
    rf"(?m)^원문 언어:\s*({BCP47_TAG})\s+[—-]\s+\S.*$",
    re.IGNORECASE,
)
TRANSLATION_PATH_RE = re.compile(
    rf"(?m)^번역 경로:\s*({BCP47_TAG})\s+원문\s*→\s*한국어 직접 번역\s*$",
    re.IGNORECASE,
)


def validate_translation_contract(book_dir):
    """최종 변환에 필요한 원문 언어·직접 번역 작업 계약을 검증한다."""
    errors = []
    documents = {
        "장 정보": book_dir / "장_정보.md",
        "번역 공통 가이드": book_dir / "번역_공통_가이드.md",
    }
    contents = {}
    for label, path in documents.items():
        if not path.is_file():
            errors.append(f"{label} 파일이 없음: {path.name}")
            continue
        contents[label] = path.read_text(encoding="utf-8-sig")

    language_tags = {}
    for label, content in contents.items():
        match = SOURCE_LANGUAGE_LINE_RE.search(content)
        if not match:
            errors.append(
                f"{label}에 '원문 언어: <BCP 47 태그> — <언어명>' 표기가 없음"
            )
        else:
            language_tags[label] = match.group(1).lower()

    if len(set(language_tags.values())) > 1:
        errors.append("장_정보.md와 번역_공통_가이드.md의 원문 언어 태그가 일치하지 않음")

    chapter_info = contents.get("장 정보", "")
    if chapter_info and not re.search(r"(?m)^언어 확인:\s*\S", chapter_info):
        errors.append("장_정보.md에 출처 선언과 실제 텍스트를 대조한 '언어 확인:' 기록이 없음")

    guide = contents.get("번역 공통 가이드", "")
    if guide:
        for heading in ("## 공통 코어", "## 원문 언어별 프로필"):
            if heading not in guide:
                errors.append(f"번역_공통_가이드.md에 필수 절이 없음: {heading}")
        path_match = TRANSLATION_PATH_RE.search(guide)
        if not path_match:
            errors.append(
                "번역_공통_가이드.md에 '번역 경로: <태그> 원문 → 한국어 직접 번역' 기록이 없음"
            )
        elif language_tags and path_match.group(1).lower() not in set(language_tags.values()):
            errors.append("번역 경로의 원문 언어 태그가 선언된 원문 언어와 일치하지 않음")
    return errors


def verify_counts(original, translated, partial=False):
    """원문/번역 장별 문단 수 대조. 성공 여부를 반환.

    partial=True면 번역이 완료된 장까지만 대조한다(장 단위 진행 중 사용).
    """
    ok = True
    print(f"\n{'':>3} {'원문 장 제목':<45} {'원문':>5} {'번역':>5}  판정")
    print("-" * 75)
    limit = len(translated) if partial else max(len(original), len(translated))
    for i in range(limit):
        o = original[i] if i < len(original) else None
        t = translated[i] if i < len(translated) else None
        o_title = o["title"] if o else "(없음)"
        o_n = o["count"] if o else "-"
        t_n = len(t["passages"]) if t else "-"
        match = o is not None and t is not None and o["count"] == len(t["passages"])
        if not match:
            ok = False
        mark = "OK" if match else "불일치"
        t_title = f"  ← {t['title']}" if t else ""
        print(f"{i + 1:>3} {o_title:<45} {o_n:>5} {t_n:>5}  {mark}{t_title if not match else ''}")
    if partial:
        if len(translated) > len(original):
            ok = False
            print(f"\n[오류] 번역 장 수({len(translated)})가 원문 장 수({len(original)})보다 많음")
        else:
            print(f"\n[부분 검증] {len(translated)}/{len(original)}장 대조 완료")
    elif len(original) != len(translated):
        ok = False
        print(f"\n[오류] 장 수 불일치: 원문 {len(original)}개 vs 번역 {len(translated)}개")
    return ok


def list_chapters(original, chapter_re, max_paragraphs=DEFAULT_CHUNK_MAX_PARAGRAPHS,
                  max_characters=DEFAULT_CHUNK_MAX_CHARACTERS,
                  max_tokens=DEFAULT_CHUNK_MAX_TOKENS):
    """원문 장 목록과 언어 비종속 작업량·권장 청크를 출력한다."""
    chapters = parse_original(original, chapter_re)
    if not chapters:
        sys.exit("[오류] 장 제목을 하나도 찾지 못했습니다. "
                 "--chapter-re로 이 책의 장 제목 패턴을 지정하세요.")
    print(f"{'':>3} {'장 제목':<42} {'라인 범위':>13} {'문단':>5} {'문자':>9} {'추정 토큰':>10}")
    print("-" * 94)
    for i, ch in enumerate(chapters, 1):
        metrics = ch["metrics"]
        print(f"{i:>3} {ch['title']:<42} {ch['start']:>6}–{ch['end']:<6} "
              f"{ch['count']:>5} {metrics['characters']:>9,} "
              f"{metrics['estimatedTokens']:>10,}")
        chunks = plan_chunks(metrics["paragraphs"], max_paragraphs, max_characters,
                             max_tokens)
        chunk_text = ", ".join(
            f"{chunk['paragraphStart']}–{chunk['paragraphEnd']}문단"
            f"({chunk['characters']:,}자/{chunk['estimatedTokens']:,}토큰)"
            for chunk in chunks
        ) or "본문 없음"
        print(f"    권장 청크: {chunk_text}")
    assignments = plan_work_assignments(
        chapters,
        max_paragraphs,
        max_characters,
        max_tokens,
    )
    print("\n권장 작업 묶음:")
    for index, assignment in enumerate(assignments, 1):
        item_text = ", ".join(
            f"{item['chapterIndex']}장 p{item['paragraphStart']:03}–p{item['paragraphEnd']:03}"
            for item in assignment["items"]
        )
        print(
            f"  {index}. {item_text} — {assignment['paragraphs']}문단 / "
            f"{assignment['characters']:,}자 / "
            f"추정 {assignment['estimatedTokens']:,}토큰"
        )
    total_paragraphs = sum(ch["count"] for ch in chapters)
    total_characters = sum(ch["metrics"]["characters"] for ch in chapters)
    total_tokens = sum(ch["metrics"]["estimatedTokens"] for ch in chapters)
    print(f"\n장 {len(chapters)}개, 문단 총 {total_paragraphs}개, "
          f"문자 총 {total_characters:,}자, 추정 토큰 총 {total_tokens:,}개")
    print("추정식: 공백으로 나뉜 ASCII 연속 구간별 4자≈1토큰(올림) + "
          "비ASCII 비공백 1자≈1토큰")
    print(f"청크 한도: 문단 {max_paragraphs}개 / 문자 {max_characters:,}자 / "
          f"추정 토큰 {max_tokens:,}개")


def create_archive(zip_output, book_dir, ingest_path, meta_path):
    """최종 보관용 ZIP을 만든다. 원문과 표지 작업 파일은 포함하지 않는다."""
    book_dir = book_dir.resolve()
    zip_output = zip_output.resolve()
    cover_dir = book_dir / "표지"
    required_cover_files = [
        book_dir / "표지_브리프.json",
        cover_dir / "시안_목록.json",
        cover_dir / "표지_검수.json",
        *(cover_dir / f"{variant_id}.png" for variant_id in "ABCDEFG"),
        *(cover_dir / "프롬프트" / f"{variant_id}.txt" for variant_id in "ABCDEFG"),
    ]
    missing_cover_files = [path for path in required_cover_files if not path.is_file()]
    if missing_cover_files:
        missing = "\n".join(f"  - {path}" for path in missing_cover_files)
        raise FileNotFoundError(f"필수 표지 산출물이 없어 ZIP을 만들 수 없습니다:\n{missing}")

    fixed_files = [
        ingest_path,
        meta_path,
        book_dir / "장_정보.md",
        book_dir / "번역_공통_가이드.md",
        *required_cover_files,
    ]
    output_dirs = [
        book_dir / "번역본",
        book_dir / "리뷰",
    ]

    zip_output.parent.mkdir(parents=True, exist_ok=True)
    seen = set()
    with zipfile.ZipFile(zip_output, "w", compression=zipfile.ZIP_DEFLATED) as zf:
        for path in fixed_files:
            if not path or not path.exists() or not path.is_file():
                continue
            resolved = path.resolve()
            arcname = resolved.relative_to(book_dir).as_posix()
            zf.write(resolved, arcname)
            seen.add(resolved)

        for directory in output_dirs:
            if not directory.exists() or not directory.is_dir():
                continue
            for path in sorted(directory.rglob("*")):
                if not path.is_file():
                    continue
                resolved = path.resolve()
                if resolved in seen:
                    continue
                arcname = resolved.relative_to(book_dir).as_posix()
                zf.write(resolved, arcname)
                seen.add(resolved)


def default_archive_name(title, fallback):
    """메타데이터 제목을 파일명으로 안전하게 바꾼다."""
    stem = (title or fallback or "book").strip()
    stem = re.sub(r"\s+", "_", stem)
    stem = re.sub(r'[<>:"/\\|?*\x00-\x1f]', "", stem)
    stem = re.sub(r"_+", "_", stem).strip("._ ")
    if not stem:
        stem = "book"
    return f"{stem}.zip"


def validate_cover_artifacts(meta_path):
    """현재 메타데이터에 대응하는 표지 A~G와 시각 검수 기록을 검증한다."""
    validator = Path(__file__).resolve().with_name("표지_시안_준비.py")
    book_dir = meta_path.resolve().parent
    if not validator.is_file():
        print(f"[표지 시안 검증 실패]\n  - 표지 검증 스크립트가 없습니다: {validator}")
        return False

    command = [
        sys.executable,
        str(validator),
        "validate",
        "--meta",
        str(meta_path.resolve()),
        "--brief",
        str(book_dir / "표지_브리프.json"),
        "--output-dir",
        str(book_dir / "표지"),
    ]
    completed = subprocess.run(
        command,
        capture_output=True,
        text=True,
        encoding="utf-8",
        errors="replace",
        check=False,
    )
    if completed.stdout:
        print(completed.stdout, end="" if completed.stdout.endswith("\n") else "\n")
    if completed.stderr:
        print(completed.stderr, file=sys.stderr, end="" if completed.stderr.endswith("\n") else "\n")
    return completed.returncode == 0


def main():
    ap = argparse.ArgumentParser(description="번역본 → 인제스트 JSON 변환/검증")
    ap.add_argument("translations", nargs="*", type=Path, help="번역본 .md 파일(순서대로 병합)")
    ap.add_argument("--normalize-legacy-ingest", type=Path, metavar="INGEST_JSON",
                    help="구형 passages[].content JSON을 현재 문장 배열 구조로 정규화")
    ap.add_argument("--backup-output", type=Path,
                    help="제자리 구형 정규화 시 원본 백업 경로 (기본: 입력파일.bak)")
    ap.add_argument("--meta", type=Path,
                    help="메타데이터.json 경로 (--partial이면 생략 가능; 구형 정규화에서는 "
                         "서지 필드만 적용)")
    ap.add_argument("--original", type=Path, help="원문 .txt 경로 (언어 무관)")
    ap.add_argument("--chapter-re", default=DEFAULT_CHAPTER_RE,
                    help="원문 장 제목 정규식 (기본: 로마숫자 'I. 제목' 형태)")
    ap.add_argument("-o", "--output", type=Path,
                    help="출력 경로 (기본: 메타데이터.json과 같은 폴더의 ingest.json)")
    ap.add_argument("--zip-output", type=Path,
                    help="최종 변환 성공 후 보관용 ZIP을 생성할 경로 "
                         "(생략 시 메타데이터 title 기반 책이름.zip)")
    ap.add_argument("--list-chapters", action="store_true",
                    help="원문의 장별 작업량과 권장 청크를 출력하고 종료")
    ap.add_argument("--max-chunk-paragraphs", type=int,
                    default=DEFAULT_CHUNK_MAX_PARAGRAPHS,
                    help="권장 청크의 최대 문단 수 (기본: 150)")
    ap.add_argument("--max-chunk-characters", type=int,
                    default=DEFAULT_CHUNK_MAX_CHARACTERS,
                    help="권장 청크의 최대 문자 수 (기본: 24000)")
    ap.add_argument("--max-chunk-estimated-tokens", type=int,
                    default=DEFAULT_CHUNK_MAX_TOKENS,
                    help="권장 청크의 최대 보수적 추정 토큰 수 (기본: 8000)")
    ap.add_argument("--no-verify", action="store_true", help="원문 문단 수 대조를 건너뜀")
    ap.add_argument("--partial", action="store_true",
                    help="번역 완료된 장까지만 대조하고 JSON은 출력하지 않음(진행 중 검증용)")
    ap.add_argument("--force", action="store_true", help="검증 실패해도 JSON을 출력")
    args = ap.parse_args()

    if args.backup_output is not None and args.normalize_legacy_ingest is None:
        ap.error("--backup-output은 --normalize-legacy-ingest와 함께 사용해야 합니다.")

    if args.normalize_legacy_ingest:
        incompatible = []
        if args.translations:
            incompatible.append("번역본 위치 인자")
        for option, value in (
            ("--original", args.original),
            ("--zip-output", args.zip_output),
            ("--list-chapters", args.list_chapters),
            ("--no-verify", args.no_verify),
            ("--partial", args.partial),
            ("--force", args.force),
        ):
            if value:
                incompatible.append(option)
        if incompatible:
            ap.error(
                "--normalize-legacy-ingest와 함께 사용할 수 없습니다: "
                + ", ".join(incompatible)
            )
        try:
            output, backup, paragraph_count, sentence_count = normalize_legacy_ingest_file(
                args.normalize_legacy_ingest,
                output=args.output,
                backup=args.backup_output,
                metadata_path=args.meta,
            )
        except LegacyNormalizationError as error:
            sys.exit(f"[구형 인제스트 정규화 실패] {error}")
        print(f"구형 인제스트 정규화 완료: {output}")
        if backup is not None:
            print(f"원본 백업: {backup}")
        print(f"문단 {paragraph_count}개, 문장 {sentence_count}개")
        print(f"문장 재결합 및 UTF-8 바이트 보존 검증 통과: 문단 {paragraph_count}개")
        return

    chapter_re = re.compile(args.chapter_re)

    if args.list_chapters:
        if not args.original or not args.original.exists():
            sys.exit(f"[오류] --list-chapters에는 --original 원문 파일이 필요합니다: {args.original}")
        try:
            list_chapters(
                args.original,
                chapter_re,
                args.max_chunk_paragraphs,
                args.max_chunk_characters,
                args.max_chunk_estimated_tokens,
            )
        except ValueError as error:
            ap.error(str(error))
        return

    if not args.translations:
        ap.error("번역본 .md 파일을 하나 이상 지정하세요 (--list-chapters 모드가 아닌 경우)")

    for p in args.translations:
        if not p.exists():
            sys.exit(f"[오류] 파일 없음: {p}")

    meta = {}
    if args.meta:
        if not args.meta.exists():
            sys.exit(f"[오류] 메타데이터 파일 없음: {args.meta}")
        meta = json.loads(args.meta.read_text(encoding="utf-8-sig"))
    elif not args.partial:
        sys.exit("[오류] 최종 변환에는 --meta 메타데이터.json이 필요합니다.")

    chapters = parse_translation(args.translations)
    book = {**meta, "chapters": chapters}

    if args.partial and not args.meta:
        # 메타데이터 없이 진행 중 검증: 메타데이터만 생략하고 본문 구조는 모두 검사한다.
        errors = validate_chapters(chapters)
    else:
        errors = validate(book)
    if not args.partial and args.meta:
        errors.extend(validate_translation_contract(args.meta.resolve().parent))
    if errors:
        print("[규격 검증 실패]")
        for e in errors:
            print(f"  - {e}")

    counts_ok = True
    if args.no_verify:
        print("[경고] 원문 대조를 건너뜀 (--no-verify)")
    elif not args.original or not args.original.exists():
        sys.exit(f"[오류] 원문 파일 없음: {args.original} (대조 없이 변환하려면 --no-verify)")
    else:
        original = parse_original(args.original, chapter_re)
        if not original:
            sys.exit("[오류] 원문에서 장 제목을 하나도 찾지 못했습니다. "
                     "--chapter-re로 이 책의 장 제목 패턴을 지정하세요.")
        counts_ok = verify_counts(original, chapters, partial=args.partial)

    total = sum(len(c["passages"]) for c in chapters)
    sentence_total = sum(
        len(p["sentences"])
        for chapter in chapters
        for p in chapter["passages"]
    )
    print(f"\n장 {len(chapters)}개, 문단 총 {total}개, 문장 총 {sentence_total}개")
    print(f"문장 재결합 검증 통과: 문단 {total}개")

    if args.partial:
        if errors or not counts_ok:
            sys.exit("\n[부분 검증 실패] 위 항목을 수정한 뒤 다시 실행하세요.")
        print("[부분 검증 통과] 전체 완료 후 --partial 없이 실행하면 JSON이 생성됩니다.")
        return

    if (errors or not counts_ok) and not args.force:
        sys.exit("\n검증 실패 — JSON을 출력하지 않았습니다. 무시하고 출력하려면 --force")

    if not validate_cover_artifacts(args.meta):
        sys.exit("\n표지 검증 실패 — ingest.json과 ZIP을 출력하지 않았습니다.")

    output = args.output or args.meta.parent / "ingest.json"
    output.write_text(json.dumps(book, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(f"저장 완료: {output}")

    zip_output = args.zip_output or Path(default_archive_name(meta.get("title", ""), output.parent.name))
    if not zip_output.is_absolute():
        zip_output = output.parent / zip_output
    create_archive(zip_output, output.parent, output, args.meta)
    print(f"ZIP 저장 완료: {zip_output}")


if __name__ == "__main__":
    main()
