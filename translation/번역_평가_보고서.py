"""번역 평가 기록의 Markdown 생성과 최종 변환 입력 검증."""

import argparse
import hashlib
import json
import os
import re
import string
from collections import Counter, defaultdict
from datetime import datetime
from pathlib import Path, PurePosixPath


REPORT_NAME = "번역_평가_결과.md"
STYLE_GUIDE_NAME = "번역_공통_가이드.md"
DEFAULT_CHAPTER_RE = r"^[IVXLCDM]+\.\s+\S"
SCHEMA_VERSION = 1
RUBRIC_VERSION = "1.0"
# 번역_평가_기준.md의 코드와 표기 순서.
CRITERION_NAMES = {
    "A1": "사건·속성",
    "A2": "논리·확실성",
    "A3": "시간·진행",
    "A4": "누락·가필",
    "A5": "지시 대상",
    "B1": "서술자·시점",
    "B2": "함축·다의성",
    "B3": "감정·태도",
    "B4": "화행",
    "C1": "호칭·높임말",
    "C2": "인물의 목소리",
    "C3": "서술 문체",
    "C4": "작품별 문체 적합성",
    "D1": "심상·비유",
    "D2": "말장난·소리",
    "D3": "반복·대비",
    "D4": "호흡·속도",
    "E1": "문화·제도",
    "E2": "시대·사회 맥락",
    "E3": "주석·설명 위치",
    "F1": "문법·표기",
    "F2": "어휘·결합",
    "F3": "연결·읽기",
    "G1": "명칭·용어",
    "G2": "작품 전반의 관계·문체·모티프",
}
CRITERIA = tuple(CRITERION_NAMES)
STATUSES = ("P", "E", "U", "NA", "NO")
SEVERITIES = ("major", "minor", "unresolved")
KINDS = ("common", "fact", "literary")
ROLES = {"translate": "번역", "revise": "수정", "evaluate": "독립 평가", "adjudicate": "최종 재검토"}
DATA_HEADING = "\n## 집계용 평가 기록\n\n"
JSON_FENCE = "\x60" * 3
SHA256 = re.compile(r"[0-9a-f]{64}")
MARKDOWN_PUNCTUATION = re.compile(rf"([{re.escape(string.punctuation)}])")


def _check(condition, message):
    if not condition:
        raise ValueError(message)


def _expect(value, expected_type, field):
    _check(type(value) is expected_type, f"{field}: {expected_type.__name__} 형식이어야 합니다.")
    return value


def _str(record, key, *, allow_blank=False):
    value = _expect(record.get(key), str, key)
    _check(allow_blank or value.strip(), f"{key}: 값이 비어 있습니다.")
    try:
        value.encode("utf-8")
    except UnicodeEncodeError as exc:
        raise ValueError(f"{key}: 잘못된 유니코드 문자입니다.") from exc
    return value


def _choice(record, key, choices):
    value = _str(record, key)
    _check(value in choices, f"{key}: 지원하지 않는 값입니다 ({value}).")
    return value


def _strings(record, key, *, required=False):
    values = _expect(record.get(key), list, key)
    _check(not required or values, f"{key}: 항목이 하나 이상 필요합니다.")
    _check(all(isinstance(value, str) and value.strip() for value in values),
           f"{key}: 비어 있지 않은 문자열 목록이어야 합니다.")
    for value in values:
        _str({key: value}, key)
    return values


def _records(record, key, *, required=False):
    values = _expect(record.get(key), list, key)
    _check(not required or values, f"{key}: 항목이 하나 이상 필요합니다.")
    for value in values:
        _expect(value, dict, key)
    return values


def _unique(values, field):
    result = set(values)
    _check(len(result) == len(values), f"{field}: 중복 값이 있습니다.")
    return result


def _validate_file_record(value, field, *, allow_parent=False):
    record = _expect(value, dict, field)
    name = _str(record, "path")
    path = PurePosixPath(name)
    _check(
        not path.is_absolute() and path.as_posix() == name and name != "."
        and "\\" not in name and ":" not in name and "\x00" not in name
        and (allow_parent or ".." not in path.parts),
        f"{field}.path: 책 폴더 기준 POSIX 상대 경로가 필요합니다 ({name}).",
    )
    _check(SHA256.fullmatch(_str(record, "sha256")), f"{field}.sha256: 소문자 SHA-256 값이 필요합니다.")
    return path


def _validate_inputs(inputs):
    _expect(inputs, dict, "inputs")
    _validate_file_record(inputs.get("source"), "inputs.source", allow_parent=True)
    if "source_index" in inputs:
        _check("chapter_pattern" not in inputs, "원문 색인과 장 제목 패턴을 함께 기록할 수 없습니다.")
        _validate_file_record(inputs["source_index"], "inputs.source_index", allow_parent=True)
    else:
        _str(inputs, "chapter_pattern")
    guide = _validate_file_record(inputs.get("style_guide"), "inputs.style_guide")
    _check(guide.as_posix() == STYLE_GUIDE_NAME, f"inputs.style_guide.path: {STYLE_GUIDE_NAME}여야 합니다.")
    translation_paths = [
        _validate_file_record(row, "inputs.translations")
        for row in _records(inputs, "translations", required=True)
    ]
    _check(
        all(path.parts[0] == "번역본" and path.suffix.lower() == ".md" for path in translation_paths),
        "inputs.translations: 번역본/의 Markdown 파일만 지정할 수 있습니다.",
    )
    _unique(translation_paths, "inputs.translations.path")


def _validate_defects(defects):
    codes_by_id = {}
    for defect in defects:
        identifier = _str(defect, "id")
        _check(identifier not in codes_by_id, "defects.id: 중복 값이 있습니다.")
        codes_by_id[identifier] = _choice(defect, "criterion", CRITERIA)
        _choice(defect, "severity", SEVERITIES)
        _str(defect, "description")
        _str(defect, "source_quote")
        _str(defect, "translation_quote", allow_blank=True)
        _unique(_strings(defect, "locations", required=True), "defects.locations")
    return codes_by_id


def _validate_questions(questions, defect_codes, scope_kind):
    ids, common_criteria, kinds = [], [], Counter()
    defects_by_code = defaultdict(set)
    for identifier, code in defect_codes.items():
        defects_by_code[code].add(identifier)
    for question in questions:
        ids.append(_str(question, "id"))
        kind = _choice(question, "kind", KINDS)
        kinds[kind] += 1
        criterion = _choice(question, "criterion", CRITERIA)
        if kind == "common":
            common_criteria.append(criterion)
        status = _choice(question, "status", STATUSES)
        if kind != "common" or "prompt" in question:
            _str(question, "prompt", allow_blank=kind == "common")
        if kind != "common" or status in ("U", "NA", "NO"):
            _str(question, "reason")
        elif "reason" in question:
            _str(question, "reason", allow_blank=True)
        needs_example = kind != "common" and status == "P"
        for key in ("location", "source_quote", "translation_quote"):
            if needs_example or key in question:
                _str(question, key, allow_blank=not needs_example)
        references = set()
        if "defect_ids" in question:
            references = _unique(_strings(question, "defect_ids"), "questions.defect_ids")
        if status == "E":
            _check(references and references <= defect_codes.keys(), "E 질문은 존재하는 결함 ID를 연결해야 합니다.")
        else:
            _check(not references, "결함 ID는 E 질문에만 연결할 수 있습니다.")
        if kind == "common":
            primary_defects = defects_by_code[criterion]
            _check(
                not primary_defects or (status == "E" and primary_defects <= references),
                f"{criterion} 공통 항목은 확정 결함 판정(E)으로 해당 주코드의 결함 ID를 모두 연결해야 합니다.",
            )
            if scope_kind == "sample" and criterion == "G2":
                _check(status == "NO", "발췌 평가의 G2(전권 일관성)는 미평가(NO)로 기록해야 합니다.")
    _unique(ids, "questions.id")
    _check(_unique(common_criteria, "공통 평가 기준") == set(CRITERIA), "공통 평가 기준 25개가 각 한 번씩 필요합니다.")
    _check(kinds["fact"] and kinds["literary"], "작품별 사실 질문과 문학 질문이 각 하나 이상 필요합니다.")


def _validate_runs(runs):
    ids, roles = [], Counter()
    for run in runs:
        ids.append(_str(run, "run_id"))
        roles[_choice(run, "role", ROLES)] += 1
        _str(run, "scope")
        _str(run, "method")
        _check("model_id" in run and "unknown_reason" in run, "실행 기록에 model_id와 unknown_reason이 필요합니다.")
        if run["model_id"] is None:
            _str(run, "unknown_reason")
            _str(run, "model_evidence", allow_blank=True)
        else:
            model = _str(run, "model_id").strip().casefold()
            _check(model not in {"unknown", "unavailable", "none", "null", "n/a", "na", "미확인", "확인 불가"},
                   "모델을 확인할 수 없으면 model_id는 null로 두고 unknown_reason을 기록해야 합니다.")
            _str(run, "model_evidence")
            _check(run["unknown_reason"] is None, "모델 확인 시 unknown_reason은 null이어야 합니다.")
    _unique(ids, "runs.run_id")
    _check(roles["translate"] >= 1 and roles["evaluate"] >= 1,
           "번역과 독립 평가의 실행 기록이 각 1회 이상 필요합니다.")


def _validate_record(data):
    _expect(data, dict, "평가 기록")
    _check(_expect(data.get("schema_version"), int, "schema_version") == SCHEMA_VERSION,
           f"schema_version: {SCHEMA_VERSION}만 지원합니다.")
    _check(_str(data, "rubric_version") == RUBRIC_VERSION, f"rubric_version: {RUBRIC_VERSION}만 지원합니다.")
    for key in ("evaluation_id", "book_title", "source_language", "source_edition", "translation_method", "question_set_id"):
        _str(data, key)
    date_text = _str(data, "evaluated_at")
    try:
        evaluated_at = datetime.fromisoformat(date_text[:-1] + "+00:00" if date_text.endswith("Z") else date_text)
    except ValueError as exc:
        raise ValueError("evaluated_at: ISO 8601 평가 시각이 필요합니다.") from exc
    _check(evaluated_at.utcoffset() is not None, "evaluated_at: 시간대가 필요합니다.")

    scope = _expect(data.get("scope"), dict, "scope")
    kind = _choice(scope, "kind", ("full", "sample"))
    evaluated = _expect(scope.get("evaluated_paragraphs"), int, "scope.evaluated_paragraphs")
    total = _expect(scope.get("total_paragraphs"), int, "scope.total_paragraphs")
    _check(0 < evaluated <= total, "평가 문단 수는 1 이상, 전체 문단 수 이하여야 합니다.")
    _check(kind != "full" or evaluated == total, "전권 평가는 평가 문단 수와 전체 문단 수가 같아야 합니다.")
    _str(scope, "description")

    _validate_inputs(data.get("inputs"))
    defect_codes = _validate_defects(_records(data, "defects"))
    _validate_questions(_records(data, "questions", required=True), defect_codes, kind)
    _validate_runs(_records(data, "runs", required=True))
    _strings(data, "revisions")
    _strings(data, "limitations")


def _summary(data):
    counts = {kind: dict.fromkeys(STATUSES, 0) for kind in KINDS}
    for question in data["questions"]:
        counts[question["kind"]][question["status"]] += 1
    source = {status: counts["fact"][status] + counts["literary"][status] for status in STATUSES}
    applicable = sum(source[status] for status in ("P", "E", "U", "NO"))
    can_compare = applicable > 0 and source["NO"] == 0
    severities = Counter(defect["severity"] for defect in data["defects"])
    return {
        "common_counts": counts["common"],
        "source_counts": source,
        "fact_counts": counts["fact"],
        "literary_counts": counts["literary"],
        "defect_counts": {severity: severities[severity] for severity in SEVERITIES},
        "applicable": applicable,
        "lower": source["E"] / applicable if can_compare else None,
        "upper": (source["E"] + source["U"]) / applicable if can_compare else None,
    }


def summarize(data):
    """고정 질문 상태와 고유 결함을 각각 집계한다."""
    _validate_record(data)
    return _summary(data)


def _md(value):
    return MARKDOWN_PUNCTUATION.sub(r"\\\1", " ".join(value.splitlines()))


def _append_items(lines, heading, values):
    lines.extend(["", f"## {heading}", ""])
    lines.extend([f"- {_md(value)}" for value in values] or ["- 기록 없음"])


def render_report(data):
    """검증한 평가 기록으로 UTF-8 Markdown의 내용을 만든다."""
    summary = summarize(data)
    scope = data["scope"]
    counts = summary["defect_counts"]
    scope_label = "전권" if scope["kind"] == "full" else "발췌"
    lines = [
        f"# 번역 평가 결과 — {_md(data['book_title'])}",
        "",
        f"- 평가 ID **{_md(data['evaluation_id'])}** · 기준 {RUBRIC_VERSION} · 양식 {SCHEMA_VERSION}",
        f"- 평가 시각 {_md(data['evaluated_at'])}",
        f"- 원문 {_md(data['source_language'])} · {_md(data['source_edition'])}",
        f"- 번역 방식 {_md(data['translation_method'])}",
        f"- 범위 {scope_label} · {scope['evaluated_paragraphs']}/{scope['total_paragraphs']}문단 · {_md(scope['description'])}",
        f"- 작품별 질문 세트 {_md(data['question_set_id'])}",
    ]
    source_index = data["inputs"].get("source_index")
    if source_index:
        lines.append(f"- 원문 색인 {_md(source_index['path'])} · SHA-256 {source_index['sha256']}")
    else:
        lines.append(f"- 원문 장 제목 패턴 {_md(data['inputs']['chapter_pattern'])}")
    lines.extend([
        "",
        "## 최종 측정",
        "",
        "| 질문 | 보존 P | 확정 결함 E | 유보 U | 비해당 NA | 미평가 NO |",
        "| --- | ---: | ---: | ---: | ---: | ---: |",
    ])
    for label, key in (("공통 항목", "common_counts"), ("작품별 전체", "source_counts"),
                       ("작품별 사실", "fact_counts"), ("작품별 문학", "literary_counts")):
        values = " | ".join(str(summary[key][status]) for status in STATUSES)
        lines.append(f"| {label} | {values} |")
    lines.extend([
        "",
        f"- 고유 결함 **중대 {counts['major']} · 경미 {counts['minor']} · 심각도 유보 {counts['unresolved']}**",
        f"- 적용 대상 작품별 질문 {summary['applicable']}개 (P+E+U+NO)",
    ])
    if summary["lower"] is None:
        lines.append("- 미평가 질문이 있거나 적용 질문이 없어 질문 위반 범위를 산출하지 않았습니다.")
    else:
        lines.append(f"- 작품별 질문 위반 범위 **{summary['lower']:.1%}–{summary['upper']:.1%}**")
    lines.extend([
        "",
        "질문 위반 범위는 고정 질문에서 확정 결함만 센 값과 유보까지 포함한 값의 범위입니다. 전체 번역 오류율이나 AI 정확도를 나타내지 않습니다.",
        "",
        "## 실행 모델",
        "",
        "| 실행 ID | 역할 | 담당 범위 | 방식 | 모델 ID | 확인 근거 또는 미확인 사유 |",
        "| --- | --- | --- | --- | --- | --- |",
    ])
    for run in data["runs"]:
        model = run["model_id"] or "미확인"
        evidence = " · ".join(filter(None, (run["model_evidence"], run["unknown_reason"])))
        cells = (run["run_id"], ROLES[run["role"]], run["scope"], run["method"], model, evidence)
        lines.append("| " + " | ".join(map(_md, cells)) + " |")

    lines.extend(["", "## 확정 결함", ""])
    if not data["defects"]:
        lines.append("- 평가 범위 내 확정 결함 없음")
    for defect in data["defects"]:
        label = {"major": "중대", "minor": "경미", "unresolved": "심각도 유보"}[defect["severity"]]
        locations = ", ".join(map(_md, defect["locations"]))
        lines.extend([
            f"- **{_md(defect['id'])} · {defect['criterion']} · {label}** ({locations})",
            f"  - {_md(defect['description'])}",
            f"  - 원문 「{_md(defect['source_quote'])}」",
            f"  - 번역 「{_md(defect['translation_quote'])}」" if defect["translation_quote"].strip() else "  - 대응 번역 구간 없음",
        ])
    _append_items(lines, "수정 내역", data["revisions"])
    _append_items(lines, "평가 한계", data["limitations"])
    lines.extend(["", "## 질문별 판정", ""])
    for question in data["questions"]:
        criterion = question["criterion"]
        label_parts = [criterion]
        if question["id"] != criterion:
            label_parts.insert(0, _md(question["id"]))
        if question["kind"] == "common":
            label_parts.append(CRITERION_NAMES[criterion])
        label_parts.append(question["status"])
        label = "- **" + " · ".join(label_parts) + "**"
        if question.get("prompt"):
            label += f" {_md(question['prompt'])}"
        lines.append(label)
        if question.get("reason", "").strip():
            lines.append(f"  - {_md(question['reason'])}")
        if question["status"] != "E":
            if question.get("location", "").strip():
                lines.append(f"  - 위치 {_md(question['location'])}")
            if question.get("source_quote", "").strip():
                lines.append(f"  - 원문 「{_md(question['source_quote'])}」")
            if question.get("translation_quote", "").strip():
                lines.append(f"  - 번역 「{_md(question['translation_quote'])}」")
        if question.get("defect_ids"):
            lines.append("  - 결함 " + ", ".join(map(_md, question["defect_ids"])))

    data_json = json.dumps(data, ensure_ascii=False, indent=2, sort_keys=True, allow_nan=False)
    return "\n".join(lines) + "\n" + DATA_HEADING + f"{JSON_FENCE}json\n{data_json}\n{JSON_FENCE}\n"


def _unique_json_object(pairs):
    result = dict(pairs)
    _check(len(result) == len(pairs), "평가 기록 JSON에 중복 키가 있습니다.")
    return result


def _read_record(raw):
    return json.loads(raw, object_pairs_hook=_unique_json_object)


def _resolve_translation_files(book_dir, translation_paths):
    translation_paths = [path.resolve() for path in translation_paths]
    _unique(translation_paths, "번역 파일 경로")
    archived_translations = {
        path.resolve()
        for path in (book_dir / "번역본").rglob("*")
        if path.is_file() and path.suffix.lower() == ".md"
    }
    _check(set(translation_paths) == archived_translations, "번역본/의 Markdown 파일 목록과 지정한 파일이 다릅니다.")
    translation_dir = book_dir / "번역본"
    _check(all(path.is_relative_to(translation_dir) for path in translation_paths),
           "번역 파일은 책 폴더의 번역본/ 안에 있어야 합니다.")
    return translation_paths


def _file_sha256(path):
    _check(path.is_file(), f"평가 입력 파일을 찾을 수 없습니다: {path}")
    return hashlib.sha256(path.read_bytes()).hexdigest()


def _validate_input_files(data, book_dir, original_path, translation_paths):
    inputs = data["inputs"]
    originals = [(inputs["source"], original_path.resolve()),
                 (inputs["style_guide"], (book_dir / STYLE_GUIDE_NAME).resolve())]
    translations = inputs["translations"]
    translation_paths = _resolve_translation_files(book_dir, translation_paths)
    _check([path.relative_to(book_dir).as_posix() for path in translation_paths] == [
        record["path"] for record in translations
    ], "평가 기록의 번역 경로와 최종 변환의 경로·순서가 다릅니다.")
    for record, path in originals + list(zip(translations, translation_paths)):
        name = record["path"]
        _check((book_dir / name).resolve() == path, f"평가 입력 경로 또는 번역 파일 순서가 다릅니다: {name}")
        _check(_file_sha256(path) == record["sha256"], f"평가 후 입력이 변경되었습니다: {name}. 최종본을 다시 평가하세요.")


def _capture_inputs(args):
    _check(args.original is not None and args.translations,
           "입력 수집에는 --original과 --translations가 필요합니다.")
    _check(args.output is None, "입력 수집 결과의 경로는 평가 기록 JSON 위치에 지정합니다.")
    book_dir = (args.book_dir or Path.cwd()).resolve()
    record_path = args.record
    _check(not record_path.resolve().is_relative_to((book_dir / "번역본").resolve()),
           "평가 기록은 번역본/ 밖에 저장하세요.")
    _check(not record_path.exists() and not record_path.is_symlink(),
           "평가 기록이 이미 있습니다. 다시 평가할 때는 새 기록 경로를 지정하세요.")
    translations = _resolve_translation_files(book_dir, [book_dir / path for path in args.translations])

    def file_record(path):
        return {"path": Path(os.path.relpath(path, book_dir)).as_posix(), "sha256": _file_sha256(path)}

    inputs = {
        "source": file_record(book_dir / args.original),
        "translations": [file_record(path) for path in translations],
        "style_guide": file_record(book_dir / STYLE_GUIDE_NAME),
    }
    if args.source_index is not None:
        inputs["source_index"] = file_record(book_dir / args.source_index)
    else:
        inputs["chapter_pattern"] = DEFAULT_CHAPTER_RE if args.chapter_re is None else args.chapter_re
    _validate_inputs(inputs)
    record_path.parent.mkdir(parents=True, exist_ok=True)
    with record_path.open("x", encoding="utf-8", newline="\n") as record_file:
        json.dump({"inputs": inputs}, record_file, ensure_ascii=False, indent=2)
        record_file.write("\n")


def load_report(path, *, book_dir, original_path, translation_paths):
    """생성된 보고서와 실제 최종 변환 입력의 경로·순서·해시를 대조한다."""
    try:
        raw = path.read_bytes().decode("utf-8")
        _, separator, section = raw.rpartition(DATA_HEADING)
        opening, closing = f"{JSON_FENCE}json\n", f"\n{JSON_FENCE}\n"
        _check(separator and section.startswith(opening) and section.endswith(closing),
               "집계용 JSON 블록이 없습니다. 평가 기록으로 Markdown을 생성하세요.")
        data = _read_record(section[len(opening):-len(closing)])
        canonical = render_report(data)
        _check(raw == canonical, "보고서 본문과 집계용 기록이 일치하지 않습니다. Markdown을 다시 생성하세요.")
        _validate_input_files(data, book_dir.resolve(), original_path, translation_paths)
    except (OSError, ValueError, RecursionError) as exc:
        raise ValueError(f"번역 평가 보고서 오류 ({path}): {exc}") from exc
    return data


def _protect_report_output(output, record_path, inputs):
    _check(not output.is_symlink(), "보고서 출력에는 심볼릭 링크를 사용할 수 없습니다.")
    book_dir = output.parent.resolve()
    output = output.resolve()
    input_records = [inputs["source"], inputs["style_guide"], *inputs["translations"]]
    if "source_index" in inputs:
        input_records.append(inputs["source_index"])
    protected = [record_path, *(book_dir / entry["path"] for entry in input_records)]
    for path in protected:
        same_file = output.exists() and path.is_file() and output.samefile(path)
        _check(output != path.resolve() and not same_file, f"보고서 출력과 평가 입력 경로가 충돌합니다: {path}")


def main(argv=None):
    parser = argparse.ArgumentParser(description="평가 입력을 기록하거나 번역 평가 Markdown 보고서를 생성합니다.")
    parser.add_argument("record", type=Path, help="평가 기록 JSON (입력 수집 시 새 파일)")
    parser.add_argument("--output", type=Path, help=f"출력 Markdown (기본값: {REPORT_NAME})")
    parser.add_argument("--capture-inputs", action="store_true", help="평가 시작 시 실제 입력을 새 JSON에 기록")
    parser.add_argument("--book-dir", type=Path, help="입력 수집의 기준 폴더 (기본값: 현재 폴더)")
    parser.add_argument("--original", type=Path, help="입력 수집에 사용할 원문")
    parser.add_argument("--translations", type=Path, nargs="+", help="입력 수집에 사용할 번역 파일 (최종 변환 순서)")
    structure = parser.add_mutually_exclusive_group()
    structure.add_argument("--source-index", type=Path, help="입력 수집에 사용할 원문 색인")
    structure.add_argument("--chapter-re", help="입력 수집에 사용할 장 제목 정규식")
    args = parser.parse_args(argv)
    try:
        if args.capture_inputs:
            _capture_inputs(args)
            return
        _check(all(option is None for option in (
            args.original, args.translations, args.book_dir, args.source_index, args.chapter_re,
        )), "원문·번역·장 구분 옵션은 --capture-inputs와 함께 사용하세요.")
        if args.output is None:
            args.output = Path(REPORT_NAME)
        _check(args.output.name == REPORT_NAME, f"보고서 출력 파일명은 {REPORT_NAME}이어야 합니다.")
        data = _read_record(args.record.read_text(encoding="utf-8-sig"))
        report = render_report(data)
        _protect_report_output(args.output, args.record, data["inputs"])
        args.output.write_bytes(report.encode("utf-8"))
    except (OSError, ValueError, RecursionError) as exc:
        parser.error(str(exc))


if __name__ == "__main__":
    main()
