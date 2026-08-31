# -*- coding: utf-8 -*-
import json
import struct
import subprocess
import sys
import tempfile
import unittest
import zipfile
import zlib
from pathlib import Path

import 표지_시안_준비 as cover
from 인제스트_변환 import create_archive


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
            self.assertTrue((book_dir / "ingest.json").is_file())
            zip_path = book_dir / "표지_있는_책.zip"
            self.assertTrue(zip_path.is_file())
            with zipfile.ZipFile(zip_path) as archive:
                names = set(archive.namelist())
            self.assertIn("표지/A.png", names)
            self.assertIn("표지/프롬프트/G.txt", names)
            self.assertIn("표지/표지_검수.json", names)


if __name__ == "__main__":
    unittest.main()
