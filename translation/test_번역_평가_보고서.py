import hashlib
import json
import os
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path

from 번역_평가_보고서 import (
    CRITERIA,
    DEFAULT_CHAPTER_RE,
    REPORT_NAME,
    STYLE_GUIDE_NAME,
    load_report,
    render_report,
    summarize,
)


def create_evaluation(book_dir, original, translations, *, source_language="en", book_title="시험 도서",
                      total_paragraphs=1):
    """최종 변환 통합 테스트에서 재사용하는 평가 기록."""
    def file_record(path):
        return {
            "path": Path(os.path.relpath(path.resolve(), book_dir.resolve())).as_posix(),
            "sha256": hashlib.sha256(path.read_bytes()).hexdigest(),
        }

    def question(identifier, kind, criterion):
        return {
            "id": identifier,
            "kind": kind,
            "criterion": criterion,
            "status": "P",
            "prompt": "원문의 효과를 보존했는가?",
            "reason": "원문과 번역의 대응 표현을 확인했다.",
            "location": "1장 1문단",
            "source_quote": "She paused.",
            "translation_quote": "그녀가 멈췄다.",
            "defect_ids": [],
        }

    def run(identifier, role):
        return {
            "run_id": identifier,
            "role": role,
            "scope": "1장",
            "method": "시험 평가 흐름",
            "model_id": "fixture-model",
            "model_evidence": "테스트용 실행 설정",
            "unknown_reason": None,
        }

    return {
        "schema_version": 1,
        "rubric_version": "1.0",
        "evaluation_id": "시험 평가 1",
        "evaluated_at": "2026-11-16T09:30:00+09:00",
        "book_title": book_title,
        "source_language": source_language,
        "source_edition": "시험 판본",
        "translation_method": "영어 원문에서 한국어로 직접 번역",
        "scope": {
            "kind": "full",
            "evaluated_paragraphs": total_paragraphs,
            "total_paragraphs": total_paragraphs,
            "description": "전체 번역 문단을 평가했다.",
        },
        "inputs": {
            "source": file_record(original),
            "translations": [file_record(path) for path in translations],
            "style_guide": file_record(book_dir / STYLE_GUIDE_NAME),
            "chapter_pattern": DEFAULT_CHAPTER_RE,
        },
        "question_set_id": "시험 문항 세트",
        "questions": [
            *({"id": code, "kind": "common", "criterion": code, "status": "P"} for code in CRITERIA),
            question("F01", "fact", "A1"),
            question("L01", "literary", "D4"),
        ],
        "defects": [],
        "revisions": [],
        "limitations": [],
        "runs": [
            run("translation-1", "translate"),
            run("evaluation-1", "evaluate"),
        ],
    }


class QualityReportTests(unittest.TestCase):
    def setUp(self):
        folder = tempfile.TemporaryDirectory()
        self.addCleanup(folder.cleanup)
        root = Path(folder.name)
        self.book_dir = root / "시험 도서"
        self.book_dir.mkdir()
        self.original = root / "원문.txt"
        self.original.write_bytes(b"I. Chapter\r\n\r\nShe paused.\r\n")
        (self.book_dir / STYLE_GUIDE_NAME).write_bytes("## 공통 코어\n".encode("utf-8"))
        translation_dir = self.book_dir / "번역본"
        translation_dir.mkdir()
        self.translations = [translation_dir / f"{index:02}_번역.md" for index in (1, 2)]
        for path in self.translations:
            path.write_bytes("## 장 제목\n\n그녀가 멈췄다.\n".encode("utf-8"))
        self.report = self.book_dir / REPORT_NAME
        self.record = create_evaluation(self.book_dir, self.original, self.translations)

    def write_report(self):
        self.report.write_bytes(render_report(self.record).encode("utf-8"))

    def read_report(self, translations=None):
        return load_report(
            self.report,
            book_dir=self.book_dir,
            original_path=self.original,
            translation_paths=self.translations if translations is None else translations,
        )

    def run_report_cli(self, output):
        record_path = self.book_dir / "평가.json"
        record_path.write_text(json.dumps(self.record, ensure_ascii=False), encoding="utf-8")
        script = Path(__file__).with_name("번역_평가_보고서.py")
        return subprocess.run(
            [sys.executable, str(script), str(record_path), "--output", str(output)],
            capture_output=True,
        )

    def run_capture_cli(self, record_path, *options, cwd=None, translations=None):
        script = Path(__file__).with_name("번역_평가_보고서.py")
        if translations is None:
            translations = [path.relative_to(self.book_dir) for path in self.translations]
        return subprocess.run(
            [sys.executable, str(script), str(record_path), "--capture-inputs",
             "--original", str(Path("..") / self.original.name),
             "--translations", *map(str, translations), *map(str, options)],
            cwd=cwd or self.book_dir,
            env={**os.environ, "PYTHONIOENCODING": "utf-8"},
            capture_output=True,
        )

    def test_cli_round_trip_keeps_one_json_block_and_escapes_markdown(self):
        self.record["book_title"] = "시험 [제목]"
        self.record["runs"][0]["method"] = "모델 \x60\x60\x60 | 분리\n다음 줄"
        result = self.run_report_cli(self.report)

        self.assertEqual(result.returncode, 0, result.stderr.decode("utf-8"))
        self.assertEqual(self.read_report(), self.record)
        report = self.report.read_bytes().decode("utf-8")
        self.assertNotIn("\r", report)
        self.assertTrue(report.startswith("# 번역 평가 결과 — 시험 \\[제목\\]\n"))
        self.assertIn("**A1 · 사건·속성 · P**", report)
        self.assertIn(r"\| 분리 다음 줄", report)
        self.assertEqual(report.count("\n" + "\x60" * 3 + "json\n"), 1)

    def test_cli_preserves_input_bytes_when_output_collides(self):
        translation = self.translations[0]
        translation_bytes = translation.read_bytes()
        result = self.run_report_cli(translation)
        self.assertNotEqual(result.returncode, 0)
        self.assertEqual(translation.read_bytes(), translation_bytes)

        source_bytes = self.original.read_bytes()
        self.report.write_bytes(source_bytes)
        self.record["inputs"]["source"]["path"] = REPORT_NAME
        result = self.run_report_cli(self.report)
        self.assertNotEqual(result.returncode, 0)
        self.assertEqual(self.report.read_bytes(), source_bytes)

    def test_summary_counts_linked_defect_once_and_keeps_uncertainty(self):
        self.record["defects"] = [{
            "id": "X01", "criterion": "A1", "severity": "minor", "description": "수치 범위가 바뀌었다.",
            "source_quote": "fourteen or younger", "translation_quote": "열네 살", "locations": ["1장 1문단"],
        }]
        common = next(question for question in self.record["questions"] if question["id"] == "A1")
        fact, literary = self.record["questions"][-2:]
        for question in (common, fact):
            question.update(status="E", defect_ids=["X01"])
        for key in ("location", "source_quote", "translation_quote"):
            del fact[key]
        literary.update(status="U", reason="가려진 표현의 강도를 판단할 판본 근거가 부족하다.")

        summary = summarize(self.record)
        self.assertEqual(summary["defect_counts"], {"major": 0, "minor": 1, "unresolved": 0})
        self.assertEqual(summary["common_counts"]["E"], 1)
        self.assertEqual(summary["source_counts"], {"P": 0, "E": 1, "U": 1, "NA": 0, "NO": 0})
        self.assertEqual((summary["applicable"], summary["lower"], summary["upper"]), (2, 0.5, 1))
        self.write_report()
        self.assertEqual(self.read_report(), self.record)
        report_body = self.report.read_text(encoding="utf-8").split("## 집계용 평가 기록", 1)[0]
        self.assertEqual(report_body.count("원문 「fourteen or younger」"), 1)

    def test_compact_questions_require_reasons_for_nonjudgments_and_evidence_for_source_preservation(self):
        common, fact = self.record["questions"][0], self.record["questions"][-2]
        for status in ("U", "NA", "NO"):
            with self.subTest(status=status):
                common["status"] = status
                with self.assertRaisesRegex(ValueError, "reason"):
                    render_report(self.record)
        common.update(reason="확인할 대응 문단이 평가에서 제외되었다.")
        fact.pop("source_quote")
        with self.assertRaisesRegex(ValueError, "source_quote"):
            render_report(self.record)

    def test_summary_leaves_bounds_empty_for_unevaluated_or_inapplicable_questions(self):
        fact, literary = self.record["questions"][-2:]
        for question in (fact, literary):
            question.update(status="NA", location="", source_quote="", translation_quote="")
        summary = summarize(self.record)
        self.assertEqual((summary["applicable"], summary["lower"], summary["upper"]), (0, None, None))
        fact.update(status="NO", reason="해당 질문의 대응 문단을 아직 읽지 않았다.")
        summary = summarize(self.record)
        self.assertEqual((summary["applicable"], summary["lower"], summary["upper"]), (1, None, None))
        self.assertEqual(summary["source_counts"]["NO"], 1)

    def test_requires_known_model_evidence_or_unknown_reason(self):
        run = self.record["runs"][0]
        run.update(model_id=" UNKNOWN ", model_evidence="임의 표기")
        with self.assertRaisesRegex(ValueError, "model_id"):
            render_report(self.record)
        run.update(model_id=None, model_evidence="")
        with self.assertRaisesRegex(ValueError, "unknown_reason"):
            render_report(self.record)
        run.update(unknown_reason="변환 이전의 실행 기록을 찾을 수 없다.")
        self.write_report()
        self.assertEqual(self.read_report()["runs"][0], run)
        self.assertIn("변환 이전의 실행 기록을 찾을 수 없다.", self.report.read_text(encoding="utf-8"))

    def test_rejects_duplicate_question_and_dangling_defect(self):
        fact = self.record["questions"][-2]
        identifier = fact["id"]
        fact["id"] = "A1"
        with self.assertRaisesRegex(ValueError, "questions.id"):
            render_report(self.record)
        fact.update(id=identifier, status="E", defect_ids=["X01"])
        with self.assertRaisesRegex(ValueError, "결함 ID"):
            render_report(self.record)

    def test_common_primary_criterion_must_link_all_final_defects(self):
        defect = {
            "id": "X01", "criterion": "A1", "severity": "minor", "description": "수치 범위가 바뀌었다.",
            "source_quote": "fourteen or younger", "translation_quote": "열네 살", "locations": ["1장 1문단"],
        }
        self.record["defects"] = [defect, {
            **defect, "id": "X02", "description": "장소의 속성이 바뀌었다.", "locations": ["1장 2문단"],
        }]
        common = self.record["questions"][0]
        common["reason"] = "공통 항목의 결함 상태 검증용 사유"
        for status in ("P", "U", "NA", "NO"):
            with self.subTest(common_status=status):
                common["status"] = status
                with self.assertRaisesRegex(ValueError, "A1 공통 항목"):
                    render_report(self.record)
        common.update(status="E", defect_ids=["X01"])
        with self.assertRaisesRegex(ValueError, "결함 ID를 모두"):
            render_report(self.record)

        common["defect_ids"].append("X02")
        secondary = self.record["questions"][1]
        secondary.update(status="E", defect_ids=["X01"])
        self.write_report()
        self.assertEqual(self.read_report()["questions"][-2]["status"], "P")
        self.assertEqual(summarize(self.record)["defect_counts"]["minor"], 2)

    def test_sample_requires_whole_book_consistency_not_evaluated(self):
        self.record["scope"].update(kind="sample", total_paragraphs=4)
        with self.assertRaisesRegex(ValueError, "발췌 평가의 G2"):
            render_report(self.record)
        question = next(q for q in self.record["questions"] if q["criterion"] == "G2")
        question.update(status="NO", reason="발췌만 읽어 전권 일관성을 판단할 수 없다.",
                        location="", source_quote="", translation_quote="")
        self.write_report()
        self.assertEqual(self.read_report()["scope"]["kind"], "sample")

    def test_requires_exactly_one_source_structure_input(self):
        inputs = self.record["inputs"]
        chapter_pattern = inputs.pop("chapter_pattern")
        with self.assertRaisesRegex(ValueError, "chapter_pattern"):
            render_report(self.record)
        inputs["source_index"] = {"path": "원문_색인.json", "sha256": "a" * 64}
        self.assertIn("원문 색인", render_report(self.record))
        inputs["chapter_pattern"] = chapter_pattern
        with self.assertRaisesRegex(ValueError, "함께 기록"):
            render_report(self.record)

    def test_requires_at_least_one_independent_run_and_integer_scope(self):
        self.record["scope"]["total_paragraphs"] = True
        with self.assertRaisesRegex(ValueError, "total_paragraphs"):
            render_report(self.record)
        self.record["scope"]["total_paragraphs"] = 1
        self.write_report()
        self.assertEqual([run["role"] for run in self.read_report()["runs"]], ["translate", "evaluate"])
        self.record["runs"].pop()
        with self.assertRaisesRegex(ValueError, "독립 평가"):
            render_report(self.record)

    def test_capture_tracks_actual_input_order_and_preserves_existing_record(self):
        relative_record_path = Path("작업") / "평가.json"
        record_path = self.book_dir / relative_record_path
        ordered_translations = list(reversed(self.translations))
        result = self.run_capture_cli(
            relative_record_path, translations=[path.relative_to(self.book_dir) for path in ordered_translations],
        )
        self.assertEqual(result.returncode, 0, result.stderr.decode("utf-8"))
        expected = create_evaluation(self.book_dir, self.original, ordered_translations)["inputs"]
        original_record = record_path.read_bytes()
        self.assertEqual(json.loads(original_record), {"inputs": expected})

        self.record["inputs"] = json.loads(original_record)["inputs"]
        self.write_report()
        self.assertEqual(self.read_report(ordered_translations), self.record)
        ordered_translations[0].write_bytes(b"updated translation")
        rejected = self.run_capture_cli(relative_record_path)
        self.assertNotEqual(rejected.returncode, 0)
        self.assertEqual(record_path.read_bytes(), original_record)
        with self.assertRaisesRegex(ValueError, "평가 후 입력이 변경"):
            self.read_report(ordered_translations)

    def test_capture_supports_book_directory_and_source_index(self):
        work_dir = self.book_dir.parent
        record_path = work_dir / "평가.json"
        index = work_dir / "색인.json"
        index.write_bytes(b"{\"source_index\": true}\n")
        result = self.run_capture_cli(
            record_path.name, "--book-dir", self.book_dir.name, "--source-index", f"../{index.name}",
            cwd=work_dir,
        )
        self.assertEqual(result.returncode, 0, result.stderr.decode("utf-8"))
        inputs = json.loads(record_path.read_bytes())["inputs"]
        expected = {key: value for key, value in self.record["inputs"].items() if key != "chapter_pattern"}
        self.assertEqual(inputs, {
            **expected, "source_index": {"path": f"../{index.name}", "sha256": hashlib.sha256(index.read_bytes()).hexdigest()},
        })

    def test_capture_rejects_incomplete_translation_set_without_creating_record(self):
        record_path = self.book_dir / "평가.json"
        result = self.run_capture_cli(record_path, translations=[self.translations[0]])
        self.assertNotEqual(result.returncode, 0)
        self.assertIn("파일 목록", result.stderr.decode("utf-8"))
        self.assertFalse(record_path.exists())

    def test_capture_cannot_create_files_inside_translation_folder(self):
        record_path = self.translations[0].parent / "평가.md"
        result = self.run_capture_cli(record_path)
        self.assertNotEqual(result.returncode, 0)
        self.assertIn("번역본/ 밖", result.stderr.decode("utf-8"))
        self.assertFalse(record_path.exists())

    def test_rejects_changed_input_bytes(self):
        self.write_report()
        for path in (self.original, *self.translations, self.book_dir / STYLE_GUIDE_NAME):
            with self.subTest(path=path.name):
                previous = path.read_bytes()
                path.write_bytes(previous + b"\n")
                try:
                    with self.assertRaisesRegex(ValueError, "평가 후 입력이 변경"):
                        self.read_report()
                finally:
                    path.write_bytes(previous)

    def test_rejects_changed_order_and_unlisted_translation_file(self):
        self.write_report()
        with self.assertRaisesRegex(ValueError, "순서"):
            self.read_report(list(reversed(self.translations)))
        (self.translations[0].parent / "03_미평가.md").write_bytes(b"unevaluated")
        with self.assertRaisesRegex(ValueError, "파일 목록"):
            self.read_report()

    def test_rejects_hand_edited_summary_and_malformed_record(self):
        self.write_report()
        raw = self.report.read_bytes()
        self.report.write_bytes(raw.replace("경미 0".encode("utf-8"), "경미 2".encode("utf-8"), 1))
        with self.assertRaisesRegex(ValueError, "본문과 집계용 기록"):
            self.read_report()
        self.report.write_bytes(raw.replace(b'"schema_version": 1,', b'"schema_version": 1,\n  "schema_version": 1,', 1))
        with self.assertRaisesRegex(ValueError, "중복 키"):
            self.read_report()
        self.report.write_bytes(b"\xff")
        with self.assertRaisesRegex(ValueError, "번역 평가 보고서 오류"):
            self.read_report()


if __name__ == "__main__":
    unittest.main()
