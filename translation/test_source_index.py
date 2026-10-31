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
            (cover / name).write_text('{"schemaVersion": 1}' if name == "시안_목록.json" else "{}")
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


class LogicalVerseIndexTests(unittest.TestCase):
    setUp = SourceIndexTests.setUp
    write_index = SourceIndexTests.write_index
    parse = SourceIndexTests.parse
    cli = SourceIndexTests.cli
    @staticmethod
    def segment(line, start, end):
        return {"line": line, "start": start, "end": end}

    def logical_fixture(self, source='"Sing!" she sang.', translation='"노래!" 하고 불렀다.',
                        kind="mixed", verse_lines=None):
        self.raw = source.replace("\n", "\r\n").encode("utf-8")
        self.original.write_bytes(self.raw)
        if verse_lines is None:
            verse_lines = [{"source": [self.segment(1, 1, 7)],
                            "translation": [self.segment(1, 1, 5)]}]
        paragraph = {"start": 1, "end": len(source.splitlines()), "kind": kind,
                     "verseLines": verse_lines}
        self.data = {"version": 2, "sourceSha256": hashlib.sha256(self.raw).hexdigest(),
                     "chapters": [{"title": "I", "paragraphs": [paragraph]}], "exclusions": []}
        self.write_index()
        self.translation = self.root / "translation.md"
        self.translation.write_text("## I\n\n" + translation + "\n", encoding="utf-8")
        return paragraph

    def verify_logical(self):
        return converter.verify_counts(self.parse(), converter.parse_translation([self.translation]))

    def test_inline_verse_and_prose_share_one_physical_line(self):
        self.logical_fixture()
        before = self.original.read_bytes(), self.translation.read_bytes()
        self.assertTrue(self.verify_logical())
        self.assertEqual(before, (self.original.read_bytes(), self.translation.read_bytes()))

    def test_wrapped_source_has_two_logical_verses_on_one_translation_line(self):
        self.logical_fixture("First wrapped\nverse.\nSecond verse.", "첫째 시행. 둘째 시행.", "verse", [
            {"source": [self.segment(1, 1, 13), self.segment(2, 1, 6)],
             "translation": [self.segment(1, 1, 6)]},
            {"source": [self.segment(3, 1, 13)],
             "translation": [self.segment(1, 8, 13)]}])
        self.assertTrue(self.verify_logical())
        listed = self.cli("--list-chapters", "--original", self.original, "--source-index", self.index)
        self.assertEqual(listed.returncode, 0, listed.stderr)
        partial = self.cli(self.translation, "--original", self.original,
                           "--source-index", self.index, "--partial")
        self.assertEqual(partial.returncode, 0, partial.stdout + partial.stderr)

    def test_source_segment_invalid_coordinates_and_order_fail_closed(self):
        paragraph = self.logical_fixture()
        valid = copy.deepcopy(self.data)
        cases = [None, [], [None], [{"line": 1, "start": 1, "end": 7, "offset": 0}],
                 [self.segment(True, 1, 7)], [self.segment(0, 1, 7)],
                 [self.segment(2, 1, 7)], [self.segment(1, 0, 7)], [self.segment(1, 8, 7)],
                 [self.segment(1, 1, 99)], [self.segment(1, 8, 8)],
                 [self.segment(1, 1, 7), self.segment(1, 7, 9)],
                 [self.segment(1, 5, 7), self.segment(1, 1, 3)]]
        for segments in cases:
            with self.subTest(segments=segments):
                data = copy.deepcopy(valid)
                data["chapters"][0]["paragraphs"][0]["verseLines"][0]["source"] = segments
                self.write_index(data)
                with self.assertRaises(ValueError):
                    self.parse()
        self.data = valid
        paragraph = self.data["chapters"][0]["paragraphs"][0]
        paragraph["verseLines"] *= 2
        self.write_index()
        with self.assertRaises(ValueError):
            self.parse()

    def test_translation_segment_bounds_empty_and_overlap_fail_closed(self):
        self.logical_fixture()
        valid = copy.deepcopy(self.data)
        for segments in (None, [], [None], [{"line": 1, "start": 1, "end": 5, "offset": 0}],
                         [self.segment(1, True, 5)],
                         [self.segment(0, 1, 5)], [self.segment(2, 1, 5)],
                         [self.segment(1, 0, 5)], [self.segment(1, 5, 4)],
                         [self.segment(1, 1, 999)], [self.segment(1, 6, 6)],
                         [self.segment(1, 1, 5), self.segment(1, 5, 7)],
                         [self.segment(1, 4, 5), self.segment(1, 1, 3)]):
            with self.subTest(segments=segments):
                data = copy.deepcopy(valid)
                data["chapters"][0]["paragraphs"][0]["verseLines"][0]["translation"] = segments
                self.write_index(data)
                try:
                    chapters = self.parse()
                except ValueError:
                    continue  # Shape errors may be rejected before translation is read.
                self.assertFalse(converter.verify_counts(chapters, converter.parse_translation([self.translation])))

    def test_pure_verse_requires_full_nonwhitespace_coverage(self):
        self.logical_fixture(kind="verse")
        with self.assertRaises(ValueError):
            self.parse()
        self.logical_fixture('"Sing!"', '"노래!" 산문', "verse")
        self.assertFalse(self.verify_logical())

    def test_mixed_requires_nonwhitespace_residual_on_both_sides(self):
        self.logical_fixture('"Sing!" ', '"노래!" 산문')
        with self.assertRaises(ValueError):
            self.parse()
        self.logical_fixture(translation='"노래!" ')
        self.assertFalse(self.verify_logical())

    def test_unicode_columns_are_codepoints_not_utf8_bytes(self):
        self.logical_fixture('😀노래 산문', '😀노래 서술', verse_lines=[{
            "source": [self.segment(1, 1, 3)], "translation": [self.segment(1, 1, 3)]}])
        self.assertTrue(self.verify_logical())

    def test_new_mapping_requires_v2_valid_kind_and_unambiguous_contract(self):
        self.logical_fixture()
        valid = copy.deepcopy(self.data)
        for change in ({"version": 1}, {"kind": "prose"}, {"verseSpans": []},
                       {"verseLines": []}, {"verseLines": [None]}):
            with self.subTest(change=change):
                data = copy.deepcopy(valid)
                if "version" in change:
                    data.update(change)
                else:
                    data["chapters"][0]["paragraphs"][0].update(change)
                self.write_index(data)
                with self.assertRaises(ValueError):
                    self.parse()

    def test_v2_legacy_paragraphs_keep_existing_validation(self):
        self.data["version"] = 2
        self.write_index()
        translation = self.root / "translation.md"
        translation.write_text("## I\n\n산문.\n\n첫째.\n둘째.\n", encoding="utf-8")
        self.assertTrue(converter.verify_counts(self.parse(), converter.parse_translation([translation])))
        translation.write_text("## I\n\n산문.\n\n합친 시행.\n", encoding="utf-8")
        self.assertFalse(converter.verify_counts(self.parse(), converter.parse_translation([translation])))

    def test_v2_final_cli_requires_current_review_and_preserves_archive_bytes(self):
        # Reuse the existing final-conversion fixtures; no production files or gates are bypassed.
        from test_인제스트_변환 import CreateArchiveCoverTests
        import 표지_시안_준비 as cover

        _, _, meta_path, meta = CreateArchiveCoverTests._write_book_inputs(self.root, "논리 시행 책")
        self.logical_fixture()
        (self.root / "번역본").mkdir()
        self.translation = self.translation.rename(self.root / "번역본/01.md")
        brief = {"genre": "고전 문학", "coreSummary": ["기억의 이야기.", "선택의 이야기."],
                 "keywords": ["기억", "선택", "시간"], "visualMotifs": ["시계"],
                 "targetAudience": "성인 독자", "avoid": []}
        (self.root / "표지_브리프.json").write_text(json.dumps(brief), encoding="utf-8")
        cover_dir = self.root / "표지"
        cover.prepare(meta, brief, cover_dir, "F")
        image, prompt = cover_dir / "F.png", cover_dir / "프롬프트/F.txt"
        CreateArchiveCoverTests._write_png(image, 1)
        (cover_dir / "표지_검수.json").write_text(json.dumps({"schemaVersion": 2, "variants": {"F": {
            "textExact": True, "frontCoverOnly": True, "styleMatch": True,
            "legible": True, "publicationReady": True, "imageSha256": cover._sha256_file(image),
            "promptSha256": cover._sha256_file(prompt)}}, "setChecks": {"noExtraTextAcrossSet": True}}),
            encoding="utf-8")
        review = self.root / "review.json"
        args = (self.translation, "--original", self.original, "--source-index", self.index,
                "--meta", meta_path)
        result = self.cli(*args)
        self.assertNotEqual(result.returncode, 0)
        self.assertFalse((self.root / "ingest.json").exists())
        review_data = {"version": 1, "status": "pass", "reviewer": "Separate fixture reviewer",
                       "evidence": "Explicit logical verse character ranges checked",
                       "sourceSha256": self.data["sourceSha256"],
                       "sourceIndexSha256": hashlib.sha256(self.index.read_bytes()).hexdigest()}
        review.write_text(json.dumps(review_data), encoding="utf-8")
        args += ("--source-index-review", review)
        preserved = {path: path.read_bytes() for path in (self.original, self.translation, self.index, review)}
        result = self.cli(*args)
        self.assertEqual(result.returncode, 0, result.stdout + result.stderr)
        with zipfile.ZipFile(self.root / "논리_시행_책.zip") as archive:
            self.assertIsNone(archive.testzip())
            for name, path in (("원문/original.txt", self.original), ("번역본/01.md", self.translation),
                               ("원문_검증/원문_색인.json", self.index),
                               ("원문_검증/원문_색인_검토.json", review)):
                self.assertEqual(archive.read(name), preserved[path])
        self.assertEqual(preserved, {path: path.read_bytes() for path in preserved})
        self.index.write_bytes(self.index.read_bytes() + b" ")
        stale = self.cli(*args)
        self.assertNotEqual(stale.returncode, 0)
        self.assertIn("SHA256", stale.stderr)
        review_data.update(version=2, sourceIndexSha256=hashlib.sha256(self.index.read_bytes()).hexdigest())
        review.write_text(json.dumps(review_data), encoding="utf-8")
        with self.assertRaises(ValueError):
            converter.validate_source_index_review(self.original, self.index, review)


if __name__ == "__main__":
    unittest.main()
