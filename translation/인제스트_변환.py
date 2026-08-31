# -*- coding: utf-8 -*-
"""번역본(마크다운) → 서버 인제스트 JSON 변환 + 원문 문단 수 대조 검증.

최종 보관용 ZIP에는 표지 브리프와 검수를 통과한 표지 산출물도 함께 보관한다.
표지 생성·검증 자체는 `표지_하네스.md`와 `표지_시안_준비.py`가 담당한다.

번역본 형식(번역_지침.md의 [4. 구조 보존 규칙]):
  - 각 장은 `## 제목` 단독 줄로 시작
  - 문단은 빈 줄로 구분, 원문 문단과 1:1

사용법:
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
      원문의 장 제목·라인 범위·문단 수를 출력한다 (번역 시작 전 장 정보 파악용).

번역본 파일을 여러 개 주면 인자 순서대로 이어 붙여 처리한다(장 단위 분할 번역 지원).
"""
import argparse
import json
import re
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


def parse_original(path, chapter_re):
    """원문에서 장별 문단 수를 센다. → [{"title", "count", "start", "end"}]

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
    in_block = False
    for i in range(start, end):
        ln = lines[i]
        if chapter_re.match(ln):
            if current is not None:
                current["end"] = i  # 직전 줄까지 (1-based로 i)
            current = {"title": ln.strip(), "count": 0, "start": i + 1, "end": end}
            chapters.append(current)
            in_block = False
            continue
        if ln.strip():
            if current is not None and not in_block:
                current["count"] += 1
            in_block = True
        else:
            in_block = False
    return chapters


def parse_translation(paths):
    """번역본에서 chapters 배열을 만든다. → [{"title", "passages": [{"content"}]}]"""
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
                block.append(ln.rstrip())
            elif block:
                if current is None:
                    sys.exit(f"[오류] {path.name}:{lineno} — 장 제목(## ...) 없이 본문이 시작됩니다: "
                             f"{block[0][:40]}...")
                current["passages"].append({"content": "\n".join(block)})
                block = []
    return chapters


def validate(book):
    """인제스트 규격(인제스트_지침.md 2장) 검증. → 오류 메시지 목록"""
    errors = []

    def check_len(label, value):
        if not (1 <= len(value) <= 100):
            errors.append(f"{label}이(가) 1~100자 범위를 벗어남 ({len(value)}자): {value[:50]}")

    check_len("도서 제목", book.get("title", ""))
    check_len("출판사", book.get("publisher", ""))
    if not book.get("authors"):
        errors.append("authors가 비어 있음")
    for i, a in enumerate(book.get("authors", []), 1):
        if not isinstance(a, dict):
            errors.append(f"{i}번째 작가 정보가 객체가 아님")
            continue
        check_len(f"{i}번째 작가명", a.get("name", ""))
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
    if not book.get("chapters"):
        errors.append("chapters가 비어 있음")
    for i, ch in enumerate(book.get("chapters", []), 1):
        check_len(f"{i}번째 목차 제목", ch["title"])
        if not ch["passages"]:
            errors.append(f"{i}번째 목차 '{ch['title']}'에 본문이 없음")
        for j, p in enumerate(ch["passages"], 1):
            if not p["content"].strip():
                errors.append(f"{i}번째 목차 {j}번째 문단이 공백임")
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


def list_chapters(original, chapter_re):
    """원문 장 목록(제목·라인 범위·문단 수)을 출력한다."""
    chapters = parse_original(original, chapter_re)
    if not chapters:
        sys.exit("[오류] 장 제목을 하나도 찾지 못했습니다. "
                 "--chapter-re로 이 책의 장 제목 패턴을 지정하세요.")
    print(f"{'':>3} {'장 제목':<50} {'라인 범위':>13} {'문단':>5}")
    print("-" * 78)
    for i, ch in enumerate(chapters, 1):
        print(f"{i:>3} {ch['title']:<50} {ch['start']:>6}–{ch['end']:<6} {ch['count']:>5}")
    total = sum(ch["count"] for ch in chapters)
    print(f"\n장 {len(chapters)}개, 문단 총 {total}개")


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
    ap.add_argument("--meta", type=Path, help="메타데이터.json 경로 (--partial이면 생략 가능)")
    ap.add_argument("--original", type=Path, help="영문 원문 .txt 경로")
    ap.add_argument("--chapter-re", default=DEFAULT_CHAPTER_RE,
                    help="원문 장 제목 정규식 (기본: 로마숫자 'I. 제목' 형태)")
    ap.add_argument("-o", "--output", type=Path,
                    help="출력 경로 (기본: 메타데이터.json과 같은 폴더의 ingest.json)")
    ap.add_argument("--zip-output", type=Path,
                    help="최종 변환 성공 후 보관용 ZIP을 생성할 경로 "
                         "(생략 시 메타데이터 title 기반 책이름.zip)")
    ap.add_argument("--list-chapters", action="store_true",
                    help="원문의 장 제목·라인 범위·문단 수만 출력하고 종료")
    ap.add_argument("--no-verify", action="store_true", help="원문 문단 수 대조를 건너뜀")
    ap.add_argument("--partial", action="store_true",
                    help="번역 완료된 장까지만 대조하고 JSON은 출력하지 않음(진행 중 검증용)")
    ap.add_argument("--force", action="store_true", help="검증 실패해도 JSON을 출력")
    args = ap.parse_args()

    chapter_re = re.compile(args.chapter_re)

    if args.list_chapters:
        if not args.original or not args.original.exists():
            sys.exit(f"[오류] --list-chapters에는 --original 원문 파일이 필요합니다: {args.original}")
        list_chapters(args.original, chapter_re)
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
        # 메타데이터 없이 진행 중 검증: 구조(장·문단)만 확인한다
        errors = []
        for i, ch in enumerate(chapters, 1):
            if not ch["passages"]:
                errors.append(f"{i}번째 목차 '{ch['title']}'에 본문이 없음")
    else:
        errors = validate(book)
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
    print(f"\n장 {len(chapters)}개, 문단 총 {total}개")

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
