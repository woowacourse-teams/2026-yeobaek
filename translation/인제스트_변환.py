# -*- coding: utf-8 -*-
"""번역본(마크다운) → 서버 인제스트 JSON 변환 + 원문 문단 수 대조 검증.

번역본 형식(번역 지침.md의 [4. 구조 보존 규칙]):
  - 각 장은 `## 제목` 단독 줄로 시작
  - 문단은 빈 줄로 구분, 원문 문단과 1:1

사용법:
  python 인제스트_변환.py 번역본.md [번역본2.md ...]
      [--meta 메타데이터.json]
      [--original "The Memoirs of Sherlock Holmes.txt"]
      [-o ingest.json]
      [--no-verify]   원문 대조를 건너뜀
      [--force]       검증 실패해도 JSON을 출력

번역본 파일을 여러 개 주면 인자 순서대로 이어 붙여 처리한다(장 단위 분할 번역 지원).
"""
import argparse
import json
import re
import sys
from pathlib import Path

CHAPTER_RE = re.compile(r"^[IVXLCDM]+\.\s+\S")  # 열 0에서 시작하는 로마숫자 장 제목


def parse_original(path):
    """Gutenberg 원문에서 장별 문단 수를 센다. → [{"title", "count"}]"""
    lines = path.read_text(encoding="utf-8-sig").splitlines()

    start, end = 0, len(lines)
    for i, ln in enumerate(lines):
        if ln.startswith("*** START"):
            start = i + 1
        elif ln.startswith("*** END"):
            end = i
            break
    lines = lines[start:end]

    chapters = []
    current = None
    in_block = False
    for ln in lines:
        if CHAPTER_RE.match(ln):
            current = {"title": ln.strip(), "count": 0}
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

    check_len("도서 제목", book["title"])
    check_len("출판사", book["publisher"])
    if not book.get("authors"):
        errors.append("authors가 비어 있음")
    for a in book.get("authors", []):
        if "name" in a:
            check_len("작가명", a["name"])
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


def main():
    ap = argparse.ArgumentParser(description="번역본 → 인제스트 JSON 변환/검증")
    ap.add_argument("translations", nargs="+", type=Path, help="번역본 .md 파일(순서대로 병합)")
    ap.add_argument("--meta", type=Path, default=Path(__file__).parent / "메타데이터.json")
    ap.add_argument("--original", type=Path,
                    default=Path(__file__).parent / "The Memoirs of Sherlock Holmes.txt")
    ap.add_argument("-o", "--output", type=Path, default=Path(__file__).parent / "ingest.json")
    ap.add_argument("--no-verify", action="store_true", help="원문 문단 수 대조를 건너뜀")
    ap.add_argument("--partial", action="store_true",
                    help="번역 완료된 장까지만 대조하고 JSON은 출력하지 않음(진행 중 검증용)")
    ap.add_argument("--force", action="store_true", help="검증 실패해도 JSON을 출력")
    args = ap.parse_args()

    for p in args.translations + [args.meta]:
        if not p.exists():
            sys.exit(f"[오류] 파일 없음: {p}")

    meta = json.loads(args.meta.read_text(encoding="utf-8-sig"))
    chapters = parse_translation(args.translations)
    book = {**meta, "chapters": chapters}

    errors = validate(book)
    if errors:
        print("[규격 검증 실패]")
        for e in errors:
            print(f"  - {e}")

    counts_ok = True
    if args.no_verify:
        print("[경고] 원문 대조를 건너뜀 (--no-verify)")
    elif not args.original.exists():
        sys.exit(f"[오류] 원문 파일 없음: {args.original} (대조 없이 변환하려면 --no-verify)")
    else:
        counts_ok = verify_counts(parse_original(args.original), chapters, partial=args.partial)

    total = sum(len(c["passages"]) for c in chapters)
    print(f"\n장 {len(chapters)}개, 문단 총 {total}개")

    if args.partial:
        if errors or not counts_ok:
            sys.exit("\n[부분 검증 실패] 위 항목을 수정한 뒤 다시 실행하세요.")
        print("[부분 검증 통과] 전체 완료 후 --partial 없이 실행하면 JSON이 생성됩니다.")
        return

    if (errors or not counts_ok) and not args.force:
        sys.exit("\n검증 실패 — JSON을 출력하지 않았습니다. 무시하고 출력하려면 --force")

    args.output.write_text(json.dumps(book, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(f"저장 완료: {args.output}")


if __name__ == "__main__":
    main()
