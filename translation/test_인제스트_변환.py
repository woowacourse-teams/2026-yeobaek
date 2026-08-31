# -*- coding: utf-8 -*-
import json
import re
import struct
import subprocess
import sys
import tempfile
import unittest
import zipfile
import zlib
from pathlib import Path

import 표지_시안_준비 as cover
from 인제스트_변환 import (
    MAX_SENTENCE_CONTENT_BYTES,
    create_archive,
    estimate_tokens,
    parse_original,
    parse_translation,
    plan_chunks,
    plan_work_assignments,
    split_sentences,
    validate,
    validate_chapters,
    validate_translation_contract,
)


class ChapterWorkloadTests(unittest.TestCase):
    def test_token_estimate_uses_language_independent_conservative_heuristic(self):
        self.assertEqual(estimate_tokens("abcd efgh"), 2)
        self.assertEqual(estimate_tokens("abcde"), 2)
        self.assertEqual(estimate_tokens("a a a a"), 4)
        self.assertEqual(estimate_tokens("가 나"), 2)
        self.assertEqual(estimate_tokens("abcd 가나다"), 4)

    def test_chunk_plan_keeps_exact_boundaries_together(self):
        metrics = [
            {"characters": 10, "estimatedTokens": 3},
            {"characters": 10, "estimatedTokens": 3},
            {"characters": 10, "estimatedTokens": 3},
        ]
        chunks = plan_chunks(metrics, max_paragraphs=2, max_characters=20, max_tokens=6)
        self.assertEqual([(c["paragraphStart"], c["paragraphEnd"]) for c in chunks],
                         [(1, 2), (3, 3)])

    def test_character_and_token_limits_each_trigger_a_split(self):
        metrics = [
            {"characters": 6, "estimatedTokens": 2},
            {"characters": 5, "estimatedTokens": 2},
        ]
        self.assertEqual(
            len(plan_chunks(metrics, max_paragraphs=99, max_characters=10, max_tokens=99)),
            2,
        )
        token_metrics = [
            {"characters": 1, "estimatedTokens": 3},
            {"characters": 1, "estimatedTokens": 3},
        ]
        self.assertEqual(
            len(plan_chunks(token_metrics, max_paragraphs=99, max_characters=99,
                            max_tokens=5)),
            2,
        )

    def test_single_oversized_paragraph_is_a_standalone_chunk(self):
        metrics = [
            {"characters": 100, "estimatedTokens": 100},
            {"characters": 1, "estimatedTokens": 1},
        ]
        chunks = plan_chunks(metrics, max_paragraphs=2, max_characters=10, max_tokens=10)
        self.assertEqual([(c["paragraphStart"], c["paragraphEnd"]) for c in chunks],
                         [(1, 1), (2, 2)])
        self.assertEqual(chunks[0]["characters"], 100)

    def test_chunk_totals_equal_source_metrics(self):
        metrics = [
            {"characters": 7, "estimatedTokens": 2},
            {"characters": 11, "estimatedTokens": 4},
            {"characters": 13, "estimatedTokens": 5},
        ]
        chunks = plan_chunks(metrics, max_paragraphs=2, max_characters=20, max_tokens=7)
        self.assertEqual(sum(c["paragraphs"] for c in chunks), len(metrics))
        self.assertEqual(sum(c["characters"] for c in chunks),
                         sum(m["characters"] for m in metrics))
        self.assertEqual(sum(c["estimatedTokens"] for c in chunks),
                         sum(m["estimatedTokens"] for m in metrics))

    def test_work_assignments_pack_short_chapters_without_exceeding_limits(self):
        chapters = [
            {
                "title": f"CHAPTER {index}",
                "metrics": {
                    "paragraphs": [
                        {"characters": 6, "estimatedTokens": 2},
                        {"characters": 4, "estimatedTokens": 2},
                    ]
                },
            }
            for index in range(1, 4)
        ]

        assignments = plan_work_assignments(
            chapters,
            max_paragraphs=4,
            max_characters=20,
            max_tokens=8,
        )

        self.assertEqual(len(assignments), 2)
        self.assertEqual(
            [[item["chapterIndex"] for item in assignment["items"]]
             for assignment in assignments],
            [[1, 2], [3]],
        )
        self.assertTrue(all(assignment["paragraphs"] <= 4 for assignment in assignments))
        self.assertTrue(all(assignment["characters"] <= 20 for assignment in assignments))
        self.assertTrue(all(assignment["estimatedTokens"] <= 8
                            for assignment in assignments))

    def test_parse_original_keeps_legacy_fields_and_custom_chapter_regex(self):
        with tempfile.TemporaryDirectory() as tmp:
            original = Path(tmp) / "원문.txt"
            original.write_text(
                "*** START OF TEST ***\n"
                "CHAPITRE UN\n\nBonjour monde.\n\nDeuxième paragraphe.\n"
                "CHAPITRE DEUX\n\nFin.\n"
                "*** END OF TEST ***\n",
                encoding="utf-8",
            )
            chapters = parse_original(original, re.compile(r"^CHAPITRE "))
        self.assertEqual([chapter["count"] for chapter in chapters], [2, 1])
        self.assertTrue({"title", "count", "start", "end"}.issubset(chapters[0]))
        self.assertEqual(chapters[0]["metrics"]["characters"],
                         sum(p["characters"] for p in chapters[0]["metrics"]["paragraphs"]))
        self.assertEqual(chapters[0]["metrics"]["estimatedTokens"],
                         sum(p["estimatedTokens"]
                             for p in chapters[0]["metrics"]["paragraphs"]))

    def test_list_chapters_cli_accepts_custom_limits_and_regex(self):
        with tempfile.TemporaryDirectory() as tmp:
            original = Path(tmp) / "original.txt"
            original.write_text("SECTION A\n\nOne.\n\nTwo.\n", encoding="utf-8")
            completed = subprocess.run(
                [
                    sys.executable,
                    str(Path(__file__).with_name("인제스트_변환.py")),
                    "--list-chapters",
                    "--original", str(original),
                    "--chapter-re", r"^SECTION ",
                    "--max-chunk-paragraphs", "1",
                    "--max-chunk-characters", "100",
                    "--max-chunk-estimated-tokens", "100",
                ],
                capture_output=True, text=True, encoding="utf-8", errors="replace",
                check=False,
            )
        self.assertEqual(completed.returncode, 0, completed.stdout + completed.stderr)
        self.assertIn("권장 청크: 1–1문단", completed.stdout)
        self.assertIn("2–2문단", completed.stdout)
        self.assertIn("권장 작업 묶음", completed.stdout)
        self.assertIn("ASCII 연속 구간별 4자≈1토큰", completed.stdout)


class SentenceIngestTests(unittest.TestCase):
    @staticmethod
    def _valid_book(passages):
        return {
            "title": "테스트 책",
            "publisher": "테스트 출판사",
            "authors": [{"name": "테스트 작가", "isni": "0000000121441305"}],
            "chapters": [{"title": "첫째", "passages": passages}],
        }

    def test_parse_translation_splits_sentences_into_new_json_structure(self):
        with tempfile.TemporaryDirectory() as tmp:
            translation = Path(tmp) / "번역.md"
            translation.write_text("## 첫째\n\n첫 문장이다. 둘째인가? 맞다!\n", encoding="utf-8")

            chapters = parse_translation([translation])

            passage = chapters[0]["passages"][0]
            self.assertNotIn("content", passage)
            self.assertEqual(
                passage["sentences"],
                [
                    {"content": "첫 문장이다. "},
                    {"content": "둘째인가? "},
                    {"content": "맞다!"},
                ],
            )

    def test_closing_quote_and_trailing_whitespace_stay_with_previous_sentence(self):
        paragraph = '그가 말했다.\"  다음이다!\u201d\n\t끝이다.'
        sentences = split_sentences(paragraph)
        self.assertEqual(sentences, ['그가 말했다.\"  ', '다음이다!\u201d\n\t', '끝이다.'])
        self.assertEqual("".join(sentences), paragraph)

    def test_decimal_abbreviation_and_initial_are_not_split(self):
        paragraph = "Dr. Kim은 3.14를 썼다. A. Smith도 i.e. 예시를 들었다. 끝."
        sentences = split_sentences(paragraph)
        self.assertEqual(
            sentences,
            ["Dr. Kim은 3.14를 썼다. ", "A. Smith도 i.e. 예시를 들었다. ", "끝."],
        )
        self.assertEqual("".join(sentences), paragraph)

    def test_contextual_abbreviations_initials_dates_and_korean_boundaries(self):
        paragraph = (
            "No. 5는 J. R. R. Tolkien의 책이다. "
            "날짜는 2026. 8. 31. 다음 문장이다. "
            "etc. 다음 문장.둘째 문장."
        )
        sentences = split_sentences(paragraph)
        self.assertEqual(
            sentences,
            [
                "No. 5는 J. R. R. Tolkien의 책이다. ",
                "날짜는 2026. 8. 31. ",
                "다음 문장이다. ",
                "etc. ",
                "다음 문장.",
                "둘째 문장.",
            ],
        )
        self.assertEqual(split_sentences("A. 김을 만났다."), ["A. 김을 만났다."])
        self.assertEqual(split_sentences("First.Second."), ["First.", "Second."])
        self.assertEqual(
            split_sentences("example.com을 봤다. 끝."),
            ["example.com을 봤다. ", "끝."],
        )
        self.assertEqual(
            split_sentences("example.info를 봤다. 끝."),
            ["example.info를 봤다. ", "끝."],
        )
        self.assertEqual("".join(sentences), paragraph)

    def test_ambiguous_abbreviations_split_before_korean_new_sentence(self):
        cases = {
            "5 p.m. 그는 떠났다.": ["5 p.m. ", "그는 떠났다."],
            "약칭은 U.S. 다음 문장이다.": ["약칭은 U.S. ", "다음 문장이다."],
            "그는 John Jr. 다음 문장이다.": ["그는 John Jr. ", "다음 문장이다."],
        }
        for paragraph, expected in cases.items():
            with self.subTest(paragraph=paragraph):
                sentences = split_sentences(paragraph)
                self.assertEqual(sentences, expected)
                self.assertEqual("".join(sentences), paragraph)

    def test_guillemets_and_east_asian_closers_stay_with_previous_sentence(self):
        paragraph = "«첫 문장.» 다음 문장.› 셋째 문장.】 넷째.） 끝."
        sentences = split_sentences(paragraph)
        self.assertEqual(
            sentences,
            [
                "«첫 문장.» ",
                "다음 문장.› ",
                "셋째 문장.】 ",
                "넷째.） ",
                "끝.",
            ],
        )
        self.assertEqual("".join(sentences), paragraph)

    def test_newlines_are_preserved_when_sentences_are_rejoined(self):
        paragraph = "첫 문장이다.\n둘째 문장이다.\n종결부호 없는 나머지"
        sentences = split_sentences(paragraph)
        self.assertEqual(sentences[0], "첫 문장이다.\n")
        self.assertEqual("".join(sentences), paragraph)

    def test_parse_translation_preserves_internal_line_end_spaces(self):
        with tempfile.TemporaryDirectory() as tmp:
            translation = Path(tmp) / "번역.md"
            translation.write_text(
                "## 첫째\n\n첫 문장.  \n둘째 문장.\t\n",
                encoding="utf-8",
            )

            chapters = parse_translation([translation])

            contents = [
                sentence["content"]
                for sentence in chapters[0]["passages"][0]["sentences"]
            ]
            self.assertEqual("".join(contents), "첫 문장.  \n둘째 문장.\t")

    def test_validate_rejects_empty_and_oversized_sentences(self):
        passages = [
            {"sentences": []},
            {"sentences": [{"content": "   "}]},
            {"sentences": [{"content": "a" * (MAX_SENTENCE_CONTENT_BYTES + 1)}]},
        ]
        errors = validate(self._valid_book(passages))
        self.assertTrue(any("sentences가 비어 있음" in error for error in errors))
        self.assertTrue(any("문장이 공백임" in error for error in errors))
        self.assertTrue(any("65,535바이트를 초과" in error for error in errors))

    def test_shared_chapter_validation_checks_partial_structure_contract(self):
        self.assertIn("chapters가 비어 있음", validate_chapters([]))
        errors = validate_chapters([
            {"title": "", "passages": []},
            {"title": "가" * 101, "passages": [{"sentences": []}]},
        ])
        self.assertTrue(any("1번째 목차 제목" in error for error in errors))
        self.assertTrue(any("본문이 없음" in error for error in errors))
        self.assertTrue(any("2번째 목차 제목" in error for error in errors))
        self.assertTrue(any("sentences가 비어 있음" in error for error in errors))

    def test_partial_cli_rejects_empty_translation_and_invalid_titles(self):
        cases = {
            "빈 번역": "",
            "빈 제목": "## \n\n본문.\n",
            "101자 제목": f"## {'가' * 101}\n\n본문.\n",
        }
        with tempfile.TemporaryDirectory() as tmp:
            for name, source in cases.items():
                with self.subTest(name=name):
                    translation = Path(tmp) / f"{name}.md"
                    translation.write_text(source, encoding="utf-8")
                    completed = subprocess.run(
                        [
                            sys.executable,
                            str(Path(__file__).with_name("인제스트_변환.py")),
                            str(translation),
                            "--partial",
                            "--no-verify",
                        ],
                        capture_output=True,
                        text=True,
                        encoding="utf-8",
                        errors="replace",
                        check=False,
                    )
                    combined_output = completed.stdout + completed.stderr
                    self.assertNotEqual(completed.returncode, 0, combined_output)
                    self.assertIn("부분 검증 실패", combined_output)

    def test_parse_translation_blocks_ambiguous_sentence_end_abbreviations(self):
        ambiguous_sources = [
            "## 첫째\n\nU.S. Army를 다뤘다.\n",
            "## 첫째\n\n회의는 p.m. Monday에 열린다.\n",
        ]
        with tempfile.TemporaryDirectory() as tmp:
            for index, source in enumerate(ambiguous_sources):
                with self.subTest(source=source):
                    translation = Path(tmp) / f"모호-{index}.md"
                    translation.write_text(source, encoding="utf-8")
                    with self.assertRaises(SystemExit) as raised:
                        parse_translation([translation])
                    message = str(raised.exception)
                    self.assertIn("문장 경계가 모호", message)
                    self.assertIn("약어를 풀어 쓰거나 문장을 다시 표현", message)

    def test_parse_translation_allows_certain_internal_abbreviations(self):
        paragraph = "Dr. Kim은 No. 5와 e.g. 예시, i.e. 설명, J. R. R. 책을 봤다."
        with tempfile.TemporaryDirectory() as tmp:
            translation = Path(tmp) / "확실.md"
            translation.write_text(f"## 첫째\n\n{paragraph}\n", encoding="utf-8")
            chapters = parse_translation([translation])
            contents = [
                sentence["content"]
                for sentence in chapters[0]["passages"][0]["sentences"]
            ]
            self.assertEqual("".join(contents), paragraph)


class CreateArchiveCoverTests(unittest.TestCase):
    @staticmethod
    def _png_chunk(chunk_type, data):
        payload = chunk_type + data
        return (
            struct.pack(">I", len(data))
            + payload
            + struct.pack(">I", zlib.crc32(payload) & 0xFFFFFFFF)
        )

    @classmethod
    def _write_png(cls, path, seed):
        width, height = 1024, 1536
        pixel = bytes((seed, seed * 3, seed * 7))
        raw = (b"\x00" + pixel * width) * height
        ihdr = struct.pack(">IIBBBBB", width, height, 8, 2, 0, 0, 0)
        path.write_bytes(
            cover.PNG_SIGNATURE
            + cls._png_chunk(b"IHDR", ihdr)
            + cls._png_chunk(b"IDAT", zlib.compress(raw, 9))
            + cls._png_chunk(b"IEND", b"")
        )

    @staticmethod
    def _write_book_inputs(book_dir, title):
        original = book_dir / "원문.txt"
        translation = book_dir / "01_첫째.md"
        meta_path = book_dir / "메타데이터.json"
        meta = {
            "title": title,
            "publisher": "테스트 출판사",
            "publishedYear": 2026,
            "authors": [{"name": "테스트 작가", "isni": "0000000121441305"}],
        }
        original.write_text(
            "*** START OF THE PROJECT GUTENBERG EBOOK TEST ***\n\n"
            "I. First\n\nFirst paragraph.\n\n"
            "*** END OF THE PROJECT GUTENBERG EBOOK TEST ***\n",
            encoding="utf-8",
        )
        translation.write_text("## I. 첫째\n\n첫 문단.\n", encoding="utf-8")
        meta_path.write_text(json.dumps(meta, ensure_ascii=False), encoding="utf-8")
        (book_dir / "장_정보.md").write_text(
            "# 장 정보\n\n"
            "원문 언어: en — 영어\n"
            "언어 확인: 출처 선언 en / 실제 텍스트 식별 en / 일치\n",
            encoding="utf-8",
        )
        (book_dir / "번역_공통_가이드.md").write_text(
            "# 번역 공통 가이드\n\n"
            "원문 언어: en — 영어\n"
            "번역 경로: en 원문 → 한국어 직접 번역\n\n"
            "## 공통 코어\n\n공통 원칙.\n\n"
            "## 원문 언어별 프로필\n\n영어 프로필.\n",
            encoding="utf-8",
        )
        return original, translation, meta_path, meta

    @staticmethod
    def _run_conversion(book_dir, original, translation, meta_path):
        return subprocess.run(
            [
                sys.executable,
                str(Path(__file__).with_name("인제스트_변환.py")),
                str(translation),
                "--original",
                str(original),
                "--meta",
                str(meta_path),
            ],
            cwd=book_dir,
            capture_output=True,
            text=True,
            encoding="utf-8",
            errors="replace",
            check=False,
        )

    def test_archive_includes_final_cover_artifacts_and_excludes_cover_work(self):
        with tempfile.TemporaryDirectory() as tmp:
            book_dir = Path(tmp) / "테스트책"
            cover_dir = book_dir / "표지"
            prompt_dir = cover_dir / "프롬프트"
            work_dir = cover_dir / "작업"
            prompt_dir.mkdir(parents=True)
            work_dir.mkdir(parents=True)

            ingest = book_dir / "ingest.json"
            meta = book_dir / "메타데이터.json"
            brief = book_dir / "표지_브리프.json"
            discarded = work_dir / "A_실패.png"
            extra = cover_dir / "H.jpg"

            ingest.write_text("{}", encoding="utf-8")
            meta.write_text("{}", encoding="utf-8")
            brief.write_text("{}", encoding="utf-8")
            (cover_dir / "시안_목록.json").write_text("{}", encoding="utf-8")
            (cover_dir / "표지_검수.json").write_text("{}", encoding="utf-8")
            for variant_id in "ABCDEFG":
                (cover_dir / f"{variant_id}.png").write_bytes(
                    f"final-cover-{variant_id}".encode("ascii")
                )
                (prompt_dir / f"{variant_id}.txt").write_text(
                    f"prompt-{variant_id}", encoding="utf-8"
                )
            discarded.write_bytes(b"discarded-cover")
            extra.write_bytes(b"unlisted-cover")

            zip_path = book_dir / "테스트책.zip"
            create_archive(zip_path, book_dir, ingest, meta)

            with zipfile.ZipFile(zip_path) as archive:
                names = set(archive.namelist())

            self.assertIn("표지_브리프.json", names)
            self.assertIn("표지/A.png", names)
            self.assertIn("표지/프롬프트/A.txt", names)
            self.assertNotIn("표지/작업/A_실패.png", names)
            self.assertNotIn("표지/H.jpg", names)

    def test_final_conversion_rejects_missing_covers_before_writing_outputs(self):
        with tempfile.TemporaryDirectory() as tmp:
            book_dir = Path(tmp) / "표지없음"
            book_dir.mkdir()
            original, translation, meta_path, _meta = self._write_book_inputs(
                book_dir, "표지 없는 책"
            )
            completed = self._run_conversion(book_dir, original, translation, meta_path)

            combined_output = completed.stdout + completed.stderr
            self.assertNotEqual(completed.returncode, 0, combined_output)
            self.assertIn("표지 시안 검증 실패", combined_output)
            self.assertFalse((book_dir / "ingest.json").exists())
            self.assertFalse((book_dir / "표지_없는_책.zip").exists())

    def test_final_contract_rejects_missing_language_profile(self):
        with tempfile.TemporaryDirectory() as tmp:
            book_dir = Path(tmp) / "언어계약없음"
            book_dir.mkdir()
            _original, _translation, _meta_path, _meta = self._write_book_inputs(
                book_dir, "언어 계약 없는 책"
            )
            (book_dir / "번역_공통_가이드.md").write_text(
                "원문 언어: fr — 프랑스어\n",
                encoding="utf-8",
            )

            errors = validate_translation_contract(book_dir)

            self.assertTrue(any("원문 언어 태그가 일치하지 않음" in error for error in errors))
            self.assertTrue(any("## 공통 코어" in error for error in errors))
            self.assertTrue(any("## 원문 언어별 프로필" in error for error in errors))
            self.assertTrue(any("번역 경로" in error for error in errors))

    def test_final_contract_rejects_pivot_language_mismatch(self):
        with tempfile.TemporaryDirectory() as tmp:
            book_dir = Path(tmp) / "경로불일치"
            book_dir.mkdir()
            self._write_book_inputs(book_dir, "경로 불일치 책")
            guide_path = book_dir / "번역_공통_가이드.md"
            guide_path.write_text(
                guide_path.read_text(encoding="utf-8").replace(
                    "번역 경로: en 원문",
                    "번역 경로: fr 원문",
                ),
                encoding="utf-8",
            )

            errors = validate_translation_contract(book_dir)

            self.assertTrue(any("번역 경로의 원문 언어 태그" in error for error in errors))

    def test_final_contract_accepts_extended_and_private_use_bcp47_tags(self):
        for language_tag in ("en-US-u-va-posix", "x-klingon", "qaa-Latn-x-classic"):
            with self.subTest(language_tag=language_tag), tempfile.TemporaryDirectory() as tmp:
                book_dir = Path(tmp) / "확장태그"
                book_dir.mkdir()
                self._write_book_inputs(book_dir, "확장 태그 책")
                chapter_path = book_dir / "장_정보.md"
                chapter_path.write_text(
                    chapter_path.read_text(encoding="utf-8").replace(
                        "원문 언어: en — 영어",
                        f"원문 언어: {language_tag} — 시험 언어",
                    ),
                    encoding="utf-8",
                )
                guide_path = book_dir / "번역_공통_가이드.md"
                guide_path.write_text(
                    guide_path.read_text(encoding="utf-8")
                    .replace("원문 언어: en — 영어", f"원문 언어: {language_tag} — 시험 언어")
                    .replace("번역 경로: en 원문", f"번역 경로: {language_tag} 원문"),
                    encoding="utf-8",
                )

                self.assertEqual(validate_translation_contract(book_dir), [])

    def test_final_conversion_validates_covers_and_archives_only_canonical_outputs(self):
        with tempfile.TemporaryDirectory() as tmp:
            book_dir = Path(tmp) / "표지있음"
            book_dir.mkdir()
            original, translation, meta_path, meta = self._write_book_inputs(
                book_dir, "표지 있는 책"
            )
            brief = {
                "genre": "고전 문학",
                "coreSummary": ["첫 번째 문장.", "두 번째 문장."],
                "keywords": ["기억", "선택", "여행"],
                "visualMotifs": ["낡은 시계"],
                "targetAudience": "성인 독자",
                "avoid": [],
            }
            brief_path = book_dir / "표지_브리프.json"
            brief_path.write_text(json.dumps(brief, ensure_ascii=False), encoding="utf-8")
            cover_dir = book_dir / "표지"
            cover.prepare(meta, brief, cover_dir)
            review_variants = {}
            for seed, variant_id in enumerate("ABCDEFG", 1):
                image_path = cover_dir / f"{variant_id}.png"
                prompt_path = cover_dir / "프롬프트" / f"{variant_id}.txt"
                self._write_png(image_path, seed)
                review_variants[variant_id] = {
                    "textExact": True,
                    "frontCoverOnly": True,
                    "styleMatch": True,
                    "legible": True,
                    "publicationReady": True,
                    "imageSha256": cover._sha256_file(image_path),
                    "promptSha256": cover._sha256_file(prompt_path),
                    "notes": "",
                }
            (cover_dir / "표지_검수.json").write_text(
                json.dumps(
                    {
                        "schemaVersion": 1,
                        "variants": review_variants,
                        "setChecks": {
                            "visuallyDistinct": True,
                            "noExtraTextAcrossSet": True,
                        },
                    },
                    ensure_ascii=False,
                ),
                encoding="utf-8",
            )

            completed = self._run_conversion(book_dir, original, translation, meta_path)

            combined_output = completed.stdout + completed.stderr
            self.assertEqual(completed.returncode, 0, combined_output)
            ingest_path = book_dir / "ingest.json"
            self.assertTrue(ingest_path.is_file())
            ingest = json.loads(ingest_path.read_text(encoding="utf-8"))
            passage = ingest["chapters"][0]["passages"][0]
            self.assertEqual(passage, {"sentences": [{"content": "첫 문단."}]})
            self.assertIn("문장 총 1개", completed.stdout)
            self.assertIn("문장 재결합 검증 통과", completed.stdout)
            zip_path = book_dir / "표지_있는_책.zip"
            self.assertTrue(zip_path.is_file())
            with zipfile.ZipFile(zip_path) as archive:
                names = set(archive.namelist())
            self.assertIn("표지/A.png", names)
            self.assertIn("표지/프롬프트/G.txt", names)
            self.assertIn("표지/표지_검수.json", names)


if __name__ == "__main__":
    unittest.main()
