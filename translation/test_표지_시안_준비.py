# -*- coding: utf-8 -*-
"""표지_시안_준비.py 단위 테스트."""

from __future__ import annotations

import contextlib
import io
import json
import struct
import tempfile
import unittest
import zlib
from pathlib import Path

import 표지_시안_준비 as cover


class CoverPreparationTests(unittest.TestCase):
    def setUp(self) -> None:
        self.temporary = tempfile.TemporaryDirectory()
        self.root = Path(self.temporary.name)
        self.meta_path = self.root / "메타데이터.json"
        self.brief_path = self.root / "표지_브리프.json"
        self.output_dir = self.root / "표지"
        self.meta = {
            "title": "여백의 고전",
            "authors": [{"name": "첫 작가"}, {"name": "둘째 작가"}],
        }
        self.brief = {
            "genre": "고전 문학",
            "coreSummary": ["한 인물이 잃어버린 시간을 찾아 나선다.", "기억과 선택의 의미를 묻는다."],
            "keywords": ["기억", "여행", "선택"],
            "visualMotifs": ["낡은 시계", "안개 낀 역"],
            "targetAudience": "문학적 미스터리를 좋아하는 성인 독자",
            "avoid": ["공포물처럼 보이는 연출"],
        }
        self._write_json(self.meta_path, self.meta)
        self._write_json(self.brief_path, self.brief)

    def tearDown(self) -> None:
        self.temporary.cleanup()

    @staticmethod
    def _write_json(path: Path, value: object) -> None:
        path.write_text(json.dumps(value, ensure_ascii=False), encoding="utf-8")

    def _run(self, *arguments: str) -> tuple[int, str]:
        output = io.StringIO()
        with contextlib.redirect_stdout(output):
            code = cover.main(list(arguments))
        return code, output.getvalue()

    def _prepare(self) -> None:
        code, output = self._run(
            "prepare",
            "--meta",
            str(self.meta_path),
            "--brief",
            str(self.brief_path),
            "--output-dir",
            str(self.output_dir),
        )
        self.assertEqual(code, 0, output)

    @staticmethod
    def _chunk(chunk_type: bytes, data: bytes) -> bytes:
        payload = chunk_type + data
        return struct.pack(">I", len(data)) + payload + struct.pack(">I", zlib.crc32(payload) & 0xFFFFFFFF)

    def _write_png(self, path: Path, width: int = 1024, height: int = 1536, seed: int = 0) -> None:
        # 각 행은 filter byte + RGB 픽셀이다. 단색이라 압축 결과가 작고 seed로 파일 해시가 달라진다.
        pixel = bytes((seed % 256, (seed * 31) % 256, (seed * 67) % 256))
        raw = (b"\x00" + pixel * width) * height
        ihdr = struct.pack(">IIBBBBB", width, height, 8, 2, 0, 0, 0)
        png = (
            cover.PNG_SIGNATURE
            + self._chunk(b"IHDR", ihdr)
            + self._chunk(b"IDAT", zlib.compress(raw, 9))
            + self._chunk(b"IEND", b"")
        )
        path.write_bytes(png)

    def _write_png_with_idat(self, path: Path, idat: bytes) -> None:
        ihdr = struct.pack(">IIBBBBB", 1024, 1536, 8, 2, 0, 0, 0)
        path.write_bytes(
            cover.PNG_SIGNATURE
            + self._chunk(b"IHDR", ihdr)
            + self._chunk(b"IDAT", idat)
            + self._chunk(b"IEND", b"")
        )

    def _write_all_images(self) -> None:
        for seed, variant_id in enumerate(cover.EXPECTED_IDS, 1):
            self._write_png(self.output_dir / f"{variant_id}.png", seed=seed)

    def _write_passing_review(self) -> None:
        checks = {
            "textExact": True,
            "frontCoverOnly": True,
            "styleMatch": True,
            "legible": True,
            "publicationReady": True,
        }
        variants = {}
        for variant_id in cover.EXPECTED_IDS:
            variants[variant_id] = {
                **checks,
                "imageSha256": cover._sha256_file(self.output_dir / f"{variant_id}.png"),
                "promptSha256": cover._sha256_file(
                    self.output_dir / "프롬프트" / f"{variant_id}.txt"
                ),
                "notes": "",
            }
        review = {
            "schemaVersion": 1,
            "variants": variants,
            "setChecks": {"visuallyDistinct": True, "noExtraTextAcrossSet": True},
        }
        self._write_json(self.output_dir / "표지_검수.json", review)

    def _validate(self, structural_only: bool = False) -> tuple[int, str]:
        arguments = [
            "validate",
            "--meta",
            str(self.meta_path),
            "--brief",
            str(self.brief_path),
            "--output-dir",
            str(self.output_dir),
        ]
        if structural_only:
            arguments.append("--structural-only")
        return self._run(*arguments)

    def test_prepare_creates_deterministic_prompts_manifest_and_preserves_images(self) -> None:
        self.output_dir.mkdir()
        existing_image = self.output_dir / "A.png"
        existing_image.write_bytes(b"existing image bytes")

        self._prepare()
        first_prompt = (self.output_dir / "프롬프트" / "A.txt").read_text(encoding="utf-8")
        first_manifest = (self.output_dir / "시안_목록.json").read_bytes()
        self._prepare()

        self.assertEqual(existing_image.read_bytes(), b"existing image bytes")
        self.assertEqual(first_manifest, (self.output_dir / "시안_목록.json").read_bytes())
        self.assertTrue(first_prompt.startswith("Use case: illustration-story\n"))
        self.assertIn("Asset type: 실제 출판용 책 앞표지", first_prompt)
        self.assertIn('제목: "여백의 고전"', first_prompt)
        self.assertIn('작가명: "첫 작가 · 둘째 작가"', first_prompt)
        self.assertEqual(
            sorted(path.name for path in (self.output_dir / "프롬프트").glob("*.txt")),
            [f"{variant_id}.txt" for variant_id in cover.EXPECTED_IDS],
        )
        manifest = json.loads(first_manifest)
        self.assertEqual(manifest["authorText"], "첫 작가 · 둘째 작가")
        self.assertEqual(manifest["requirements"]["exactCount"], 7)
        self.assertEqual(len(manifest["briefDigest"]), 64)

        (self.output_dir / "프롬프트" / "H.txt").write_text("extra", encoding="utf-8")
        self._write_all_images()
        code, output = self._validate(structural_only=True)
        self.assertEqual(code, 1)
        self.assertIn("허용되지 않은 프롬프트", output)

    def test_validate_success_with_manual_review(self) -> None:
        self._prepare()
        self._write_all_images()
        self._write_passing_review()

        code, output = self._validate()

        self.assertEqual(code, 0, output)
        self.assertIn("검증 통과", output)

    def test_validate_reports_missing_duplicate_and_wrong_ratio_together(self) -> None:
        self._prepare()
        self._write_all_images()
        (self.output_dir / "G.png").unlink()
        (self.output_dir / "B.png").write_bytes((self.output_dir / "A.png").read_bytes())
        self._write_png(self.output_dir / "C.png", width=1024, height=1600, seed=9)

        code, output = self._validate(structural_only=True)

        self.assertEqual(code, 1)
        self.assertIn("G.png", output)
        self.assertIn("중복", output)
        self.assertIn("정확한 2:3이 아닙니다", output)

    def test_validate_rejects_failed_manual_review(self) -> None:
        self._prepare()
        self._write_all_images()
        self._write_passing_review()
        review_path = self.output_dir / "표지_검수.json"
        review = json.loads(review_path.read_text(encoding="utf-8"))
        review["variants"]["D"]["legible"] = False
        review["setChecks"]["visuallyDistinct"] = False
        self._write_json(review_path, review)

        code, output = self._validate()

        self.assertEqual(code, 1)
        self.assertIn("variants.D.legible", output)
        self.assertIn("setChecks.visuallyDistinct", output)

    def test_review_hashes_expire_after_image_prompt_or_input_change(self) -> None:
        self._prepare()
        self._write_all_images()
        self._write_passing_review()

        self._write_png(self.output_dir / "A.png", seed=99)
        code, output = self._validate()
        self.assertEqual(code, 1)
        self.assertIn("variants.A.imageSha256", output)

        self._write_png(self.output_dir / "A.png", seed=1)
        self._write_passing_review()
        prompt_path = self.output_dir / "프롬프트" / "B.txt"
        prompt_path.write_text(prompt_path.read_text(encoding="utf-8") + "변경", encoding="utf-8")
        code, output = self._validate()
        self.assertEqual(code, 1)
        self.assertIn("variants.B.promptSha256", output)

        self._prepare()
        self._write_passing_review()
        self.brief["keywords"][0] = "변경된 기억"
        self._write_json(self.brief_path, self.brief)
        self._prepare()
        code, output = self._validate()
        self.assertEqual(code, 1)
        self.assertIn("promptSha256", output)

    def test_validate_rejects_truncated_and_bad_crc_png(self) -> None:
        self._prepare()
        self._write_all_images()
        a_path = self.output_dir / "A.png"
        a_path.write_bytes(a_path.read_bytes()[:-3])
        b_path = self.output_dir / "B.png"
        damaged = bytearray(b_path.read_bytes())
        damaged[-1] ^= 0x01
        b_path.write_bytes(damaged)

        code, output = self._validate(structural_only=True)

        self.assertEqual(code, 1)
        self.assertIn("A.png PNG 구조 오류", output)
        self.assertIn("잘렸습니다", output)
        self.assertIn("B.png PNG 구조 오류", output)
        self.assertIn("CRC", output)

    def test_validate_rejects_bad_zlib_scanline_length_and_filter(self) -> None:
        self._prepare()
        self._write_all_images()
        self._write_png_with_idat(self.output_dir / "C.png", b"not-zlib-data")
        self._write_png_with_idat(self.output_dir / "D.png", zlib.compress(b"\x00"))
        pixel = bytes((5, 7, 11))
        bad_filter_scanlines = (b"\x05" + pixel * 1024) + (
            (b"\x00" + pixel * 1024) * 1535
        )
        self._write_png_with_idat(
            self.output_dir / "E.png", zlib.compress(bad_filter_scanlines, 9)
        )
        self._write_png_with_idat(
            self.output_dir / "F.png", zlib.compress(b"\x00") + zlib.compress(b"\x00")
        )
        self._write_png_with_idat(
            self.output_dir / "G.png", zlib.compress(b"\x00")[:-2]
        )

        code, output = self._validate(structural_only=True)

        self.assertEqual(code, 1)
        self.assertIn("C.png PNG 구조 오류", output)
        self.assertIn("zlib 압축 스트림이 손상", output)
        self.assertIn("D.png PNG 구조 오류", output)
        self.assertIn("스캔라인 길이", output)
        self.assertIn("E.png PNG 구조 오류", output)
        self.assertIn("스캔라인 필터", output)
        self.assertIn("F.png PNG 구조 오류", output)
        self.assertIn("trailing compressed data", output)
        self.assertIn("G.png PNG 구조 오류", output)
        self.assertIn("끝까지 완료되지 않았습니다", output)

    def test_validate_rejects_extra_jpg_and_unknown_directory(self) -> None:
        self._prepare()
        self._write_all_images()
        (self.output_dir / "H.jpg").write_bytes(b"jpeg")
        (self.output_dir / "임의폴더").mkdir()

        code, output = self._validate(structural_only=True)

        self.assertEqual(code, 1)
        self.assertIn("H.jpg", output)
        self.assertIn("임의폴더", output)

    def test_brief_validation_collects_multiple_errors(self) -> None:
        self.brief["coreSummary"] = ["하나뿐"]
        self.brief["keywords"] = ["둘", "뿐"]
        self.brief["visualMotifs"] = []
        self.brief["targetAudience"] = " "
        self.brief["avoid"] = [3]
        self._write_json(self.brief_path, self.brief)

        code, output = self._run(
            "prepare",
            "--meta",
            str(self.meta_path),
            "--brief",
            str(self.brief_path),
            "--output-dir",
            str(self.output_dir),
        )

        self.assertEqual(code, 1)
        self.assertIn("coreSummary", output)
        self.assertIn("keywords", output)
        self.assertIn("visualMotifs", output)
        self.assertIn("targetAudience", output)
        self.assertIn("avoid[1]", output)
        self.assertFalse(self.output_dir.exists())


if __name__ == "__main__":
    unittest.main()
