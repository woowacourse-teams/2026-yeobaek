import re
import sys
from pathlib import Path

import pdfplumber


CHAPTER_RE = re.compile(r"^Chapter\s+([0-9]+)$")
PART_RE = re.compile(r"^PART\s+(ONE|TWO|THREE)$")
APPENDIX_RE = re.compile(r"^APPENDIX\.?$")


def normalize(text: str) -> str:
    text = text.replace("ﬁ", "fi").replace("ﬂ", "fl")
    text = re.sub(r"\b(fi|fl) (?=[a-z])", r"\1", text)
    text = re.sub(r"(?<=[a-z])(fi|fl) (?=[a-z])", r"\1", text)
    text = text.replace("curr ent", "current")
    text = re.sub(r"(?<=[a-z]) {2,}(?=[a-z])", "", text)
    text = re.sub(r"\s+([,.;:?!])", r"\1", text)
    return text.strip()


def page_lines(page):
    lines = page.extract_text_lines(layout=False, strip=False, return_chars=True)
    result = []
    for line in lines:
        if line["top"] > page.height - 70:
            continue
        text = normalize(line["text"])
        if not text:
            continue
        if re.fullmatch(r"\d{1,3}", text) and line["top"] > page.height - 100:
            continue
        result.append((float(line["x0"]), float(line["top"]), text))
    return result


def main(pdf_path: Path, output_path: Path) -> None:
    chapters = []
    current = None
    current_paragraph = []
    active_part = None
    base_x = None

    def flush_paragraph():
        nonlocal current_paragraph
        if current is not None and current_paragraph:
            joined = "".join(current_paragraph)
            joined = re.sub(r"-\s+(?=[a-z])", "", joined)
            joined = re.sub(r"[ \t]+", " ", joined).strip()
            joined = normalize(joined)
            if joined:
                current["paragraphs"].append(joined)
        current_paragraph = []

    with pdfplumber.open(pdf_path) as pdf:
        for page_no, page in enumerate(pdf.pages, 1):
            lines = page_lines(page)
            body_candidates = [
                x0
                for x0, _top, text in lines
                if not PART_RE.fullmatch(text)
                and not CHAPTER_RE.fullmatch(text)
                and not APPENDIX_RE.fullmatch(text)
            ]
            page_base_x = min(body_candidates) if body_candidates else 0.0
            for index, (x0, top, text) in enumerate(lines):
                part_match = PART_RE.fullmatch(text)
                chapter_match = CHAPTER_RE.fullmatch(text)
                appendix_match = APPENDIX_RE.fullmatch(text)
                if part_match:
                    flush_paragraph()
                    active_part = part_match.group(1)
                    continue
                if (
                    page_no == 201
                    and index == 0
                    and current is not None
                    and current["part"] == "TWO"
                    and current["chapter"] == 9
                ):
                    flush_paragraph()
                    current = {
                        "part": "TWO",
                        "chapter": 10,
                        "page": page_no,
                        "paragraphs": [],
                    }
                    chapters.append(current)
                    base_x = None
                if chapter_match and page_no >= 5:
                    flush_paragraph()
                    current = {
                        "part": active_part,
                        "chapter": int(chapter_match.group(1)),
                        "page": page_no,
                        "paragraphs": [],
                    }
                    chapters.append(current)
                    base_x = None
                    continue
                if appendix_match:
                    flush_paragraph()
                    current = {
                        "part": "APPENDIX",
                        "chapter": None,
                        "page": page_no,
                        "paragraphs": [],
                    }
                    chapters.append(current)
                    base_x = None
                    continue
                if current is not None and current["part"] == "APPENDIX" and text == "The Principles of Newspeak":
                    continue
                if current is None:
                    continue

                if base_x is None:
                    base_x = page_base_x

                previous_top = lines[index - 1][1] if index else None
                large_gap = previous_top is not None and top - previous_top > 18
                indented = x0 > page_base_x + 5
                previous_indented = index > 0 and lines[index - 1][0] > page_base_x + 5
                verse_continuation = (
                    current_paragraph
                    and indented
                    and previous_indented
                    and not lines[index - 1][2].rstrip().endswith(("’", "'", "”", '"'))
                    and not large_gap
                )
                if current_paragraph and (large_gap or (indented and not verse_continuation)):
                    flush_paragraph()
                separator = "\n" if verse_continuation else ("" if not current_paragraph else " ")
                current_paragraph.append(separator + text)

    flush_paragraph()

    lines_out = []
    for chapter in chapters:
        if chapter["part"] == "APPENDIX":
            lines_out.append("APPENDIX — The Principles of Newspeak")
        else:
            lines_out.append(f"PART {chapter['part']} — Chapter {chapter['chapter']}")
        lines_out.append("")
        for paragraph in chapter["paragraphs"]:
            lines_out.append(paragraph)
            lines_out.append("")

    output_path.write_text("\n".join(lines_out).rstrip() + "\n", encoding="utf-8")
    print(f"chapters={len(chapters)} paragraphs={sum(len(c['paragraphs']) for c in chapters)}")
    for chapter in chapters:
        label = (
            "APPENDIX"
            if chapter["part"] == "APPENDIX"
            else f"PART {chapter['part']} Chapter {chapter['chapter']}"
        )
        print(f"{label}: page {chapter['page']}, paragraphs {len(chapter['paragraphs'])}")


if __name__ == "__main__":
    if len(sys.argv) != 3:
        raise SystemExit("usage: PDF_원문_추출.py INPUT.pdf OUTPUT.txt")
    main(Path(sys.argv[1]), Path(sys.argv[2]))
