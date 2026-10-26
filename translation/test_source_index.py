"""Strict opt-in source index regression tests; raw source bytes remain untouched."""
import copy
import hashlib
import importlib.util
import json
from pathlib import Path
import subprocess
import sys
import tempfile
import unittest
import zipfile


SCRIPT = Path(__file__).with_name("인제스트_변환.py")
SPEC = importlib.util.spec_from_file_location("source_index_converter", SCRIPT)
converter = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(converter)


class SourceIndexTests(unittest.TestCase):
    def setUp(self):
        self.directory = tempfile.TemporaryDirectory()
        self.addCleanup(self.directory.cleanup)
        self.root = Path(self.directory.name)
        self.original = self.root / "original.txt"
        # Identical TOC/body heading, multiline title, prose wrap, and poetry.
        self.raw = ("Contents\r\nCHAPTER I\r\n\r\n*** START TEST ***\r\n"
                    "CHAPTER I\r\nA TITLE\r\n\r\nFirst wrapped\r\nparagraph.\r\n"
                    "\r\nVerse one\r\nVerse two\r\n\r\n*** END TEST ***\r\n").encode()
        self.original.write_bytes(self.raw)
        self.index = self.root / "index.json"
        self.data = {
            "version": 1, "sourceSha256": hashlib.sha256(self.raw).hexdigest(),
            "chapters": [{"title": "CHAPTER I — A TITLE", "paragraphs": [
                {"start": 8, "end": 9}, {"start": 11, "end": 12, "kind": "verse"}]}],
            "exclusions": [
                {"start": 1, "end": 2, "category": "toc", "reason": "Duplicate contents listing"},
                {"start": 4, "end": 4, "category": "gutenberg", "reason": "PG start marker"},
                {"start": 5, "end": 6, "category": "heading", "reason": "Two-line chapter title"},
                {"start": 14, "end": 14, "category": "gutenberg", "reason": "PG end marker"},
            ],
        }
        self.write_index()

    def write_index(self, data=None):
        self.index.write_text(json.dumps(data or self.data), encoding="utf-8")

    def parse(self):
        return converter.parse_source_index(self.original, self.index)

    def cli(self, *arguments):
        return subprocess.run([sys.executable, str(SCRIPT), *map(str, arguments)],
                              capture_output=True, text=True, encoding="utf-8", errors="replace")

    def test_metrics_come_from_raw_ranges_without_source_mutation(self):
        chapters = self.parse()
        self.assertEqual(len(chapters), 1)
        self.assertEqual(chapters[0]["count"], 2)
        self.assertEqual(chapters[0]["metrics"]["characters"],
                         len("First wrapped\nparagraph.") + len("Verse one\nVerse two"))
        self.assertEqual(self.original.read_bytes(), self.raw)

    def test_rejects_stale_source_and_stale_review(self):
        self.original.write_bytes(self.raw + b"\n")
        with self.assertRaisesRegex(ValueError, "sourceSha256"):
            self.parse()
        self.original.write_bytes(self.raw)
        review = self.root / "review.json"
        review.write_text(json.dumps({"version": 1, "status": "pass", "reviewer": "Independent reviewer",
                                     "evidence": "Checked each source line classification",
                                     "sourceSha256": self.data["sourceSha256"],
                                     "sourceIndexSha256": hashlib.sha256(self.index.read_bytes()).hexdigest()}))
        converter.validate_source_index_review(self.original, self.index, review)
        self.index.write_bytes(self.index.read_bytes() + b" ")
        with self.assertRaises(ValueError):
            converter.validate_source_index_review(self.original, self.index, review)

    def test_invalid_ranges_and_unclassified_lines_fail_closed(self):
        cases = []
        for start, end in [(True, 9), (0, 9), (9, 8), (8, 999), (8, 10), (8, 8)]:
            item = copy.deepcopy(self.data)
            item["chapters"][0]["paragraphs"][0].update(start=start, end=end)
            cases.append(item)
        item = copy.deepcopy(self.data)
        item["exclusions"].pop(0)
        cases.append(item)
        item = copy.deepcopy(self.data)
        item["exclusions"][0]["end"] = 8
        cases.append(item)
        item = copy.deepcopy(self.data)
        item["chapters"][0]["paragraphs"].reverse()
        cases.append(item)
        item = copy.deepcopy(self.data)
        item["exclusions"].reverse()
        cases.append(item)
        item = copy.deepcopy(self.data)
        item["exclusions"][0]["reason"] = " "
        cases.append(item)
        item = copy.deepcopy(self.data)
        item["exclusions"][0]["category"] = []
        cases.append(item)
        for data in cases:
            with self.subTest(data=data):
                self.write_index(data)
                with self.assertRaises(ValueError):
                    self.parse()

    def test_verse_line_loss_fails_even_when_paragraph_count_matches(self):
        translation = self.root / "translation.md"
        translation.write_text("## I. 제목\n\n첫 문단.\n\n첫째 시행.\n둘째 시행.\n", encoding="utf-8")
        chapters = self.parse()
        self.assertTrue(converter.verify_counts(chapters, converter.parse_translation([translation])))
        translation.write_text("## I. 제목\n\n첫 문단.\n\n두 시행을 한 줄로 합침.\n", encoding="utf-8")
        self.assertFalse(converter.verify_counts(chapters, converter.parse_translation([translation])))

    def test_list_and_partial_cli_use_same_index(self):
        result = self.cli("--list-chapters", "--original", self.original, "--source-index", self.index)
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertIn("장 1개, 문단 총 2개", result.stdout)
        translation = self.root / "translation.md"
        translation.write_text("## I. 제목\n\n첫 문단.\n\n첫째.\n둘째.\n", encoding="utf-8")
        result = self.cli(translation, "--original", self.original, "--source-index", self.index, "--partial")
        self.assertEqual(result.returncode, 0, result.stdout + result.stderr)

    def test_cli_rejects_bypasses_and_requires_review_before_any_output(self):
        output = self.root / "ingest.json"
        for option in ("--force", "--no-verify"):
            result = self.cli("--list-chapters", "--original", self.original,
                              "--source-index", self.index, option, "-o", output)
            self.assertNotEqual(result.returncode, 0)
            self.assertFalse(output.exists())
        result = self.cli("--original", self.original, "--source-index", self.index, "-o", output)
        self.assertNotEqual(result.returncode, 0)
        self.assertIn("source-index-review", result.stderr)
        self.assertFalse(output.exists())

    def test_archive_preserves_exact_source_index_review_bytes(self):
        review = self.root / "review.json"
        review.write_text(json.dumps({"version": 1, "status": "pass", "reviewer": "Separate reviewer",
                                     "evidence": "All source classifications checked",
                                     "sourceSha256": self.data["sourceSha256"],
                                     "sourceIndexSha256": hashlib.sha256(self.index.read_bytes()).hexdigest()}))
        ingest, meta = self.root / "ingest.json", self.root / "meta.json"
        ingest.write_text("{}")
        meta.write_text("{}")
        (self.root / "책_소개.txt").write_text("소개 초안.", encoding="utf-8")
        (self.root / "표지_브리프.json").write_text("{}")
        cover = self.root / "표지"
        (cover / "프롬프트").mkdir(parents=True)
        for name in ("시안_목록.json", "표지_검수.json"):
            (cover / name).write_text("{}")
        for letter in "ABCDEFG":
            (cover / f"{letter}.png").write_bytes(b"fixture")
            (cover / "프롬프트" / f"{letter}.txt").write_text("fixture")
        archive = self.root / "book.zip"
        converter.create_archive(archive, self.root, ingest, meta, self.original, self.index, review)
        with zipfile.ZipFile(archive) as contents:
            self.assertEqual(contents.read("원문/original.txt"), self.raw)
            self.assertEqual(contents.read("원문_검증/원문_색인.json"), self.index.read_bytes())
            self.assertEqual(contents.read("원문_검증/원문_색인_검토.json"), review.read_bytes())
        preserved = {path: path.read_bytes() for path in (self.index, review, ingest, meta, self.original)}
        for protected in (self.index, review):
            with self.subTest(protected=protected), self.assertRaises(ValueError):
                converter.create_archive(protected, self.root, ingest, meta,
                                         self.original, self.index, review)
            for path, original_bytes in preserved.items():
                self.assertEqual(path.read_bytes(), original_bytes)

    def mixed_fixture(self):
        # The first source paragraph has one verse line plus one prose line.
        paragraph = self.data["chapters"][0]["paragraphs"][0]
        paragraph.update(kind="mixed", verseSpans=[
            {"start": 8, "end": 8, "translationStart": 1, "translationEnd": 1}])
        self.write_index()
        return paragraph

    def test_mixed_mapping_preserves_only_explicit_verse_spans(self):
        self.mixed_fixture()
        translation = self.root / "translation.md"
        translation.write_text("## I. 제목\n\n첫째 시행.\n산문은 한 행.\n\n첫째.\n둘째.\n", encoding="utf-8")
        self.assertTrue(converter.verify_counts(self.parse(), converter.parse_translation([translation])))
        translation.write_text("## I. 제목\n\n산문과 시행을 한 행으로 합침.\n\n첫째.\n둘째.\n", encoding="utf-8")
        self.assertFalse(converter.verify_counts(self.parse(), converter.parse_translation([translation])))
        self.data["chapters"][0]["paragraphs"][0]["verseSpans"][0].update(
            translationStart=3, translationEnd=3)
        self.write_index()
        self.assertFalse(converter.verify_counts(self.parse(), converter.parse_translation([translation])))

    def test_mixed_invalid_spans_and_incompatible_fields_fail_closed(self):
        paragraph = self.mixed_fixture()
        valid = copy.deepcopy(self.data)
        bad_spans = [None, [], [None],
                     [{"start": True, "end": 8, "translationStart": 1, "translationEnd": 1}],
                     [{"start": 7, "end": 7, "translationStart": 1, "translationEnd": 1}],
                     [{"start": 8, "end": 10, "translationStart": 1, "translationEnd": 3}],
                     [{"start": 9, "end": 8, "translationStart": 1, "translationEnd": 1}],
                     [{"start": 8, "end": 8, "translationStart": 0, "translationEnd": 0}],
                     [{"start": 8, "end": 8, "translationStart": 1, "translationEnd": 2}],
                     [{"start": 8, "end": 9, "translationStart": 1, "translationEnd": 2}],
                     paragraph["verseSpans"] * 2]
        for spans in bad_spans:
            with self.subTest(spans=spans):
                data = copy.deepcopy(valid)
                data["chapters"][0]["paragraphs"][0]["verseSpans"] = spans
                self.write_index(data)
                with self.assertRaises(ValueError):
                    self.parse()
        for kind in ("prose", "verse"):
            for spans in (None, valid["chapters"][0]["paragraphs"][0]["verseSpans"]):
                data = copy.deepcopy(valid)
                data["chapters"][0]["paragraphs"][0].update(kind=kind, verseSpans=spans)
                self.write_index(data)
                with self.assertRaises(ValueError):
                    self.parse()


if __name__ == "__main__":
    unittest.main()
