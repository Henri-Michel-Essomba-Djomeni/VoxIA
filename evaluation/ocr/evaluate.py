from __future__ import annotations

import argparse
import sys
from pathlib import Path
from statistics import mean

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT))

from common.io import bool_value, float_value, git_commit, now_iso, read_records, write_json_report
from common.metrics import character_error_rate, safe_mean, word_error_rate


GROUP_FIELDS = ["document_type", "lighting", "device_model"]


def rate(values: list[bool | None]) -> float | None:
    known = [value for value in values if value is not None]
    return sum(1 for value in known if value) / len(known) if known else None


def summarize(rows: list[dict]) -> dict:
    wers = [word_error_rate(row.get("reference_text", ""), row.get("hypothesis_text", "")) for row in rows]
    cers = [character_error_rate(row.get("reference_text", ""), row.get("hypothesis_text", "")) for row in rows]
    return {
        "samples": len(rows),
        "wer": mean(wers) if wers else None,
        "cer": mean(cers) if cers else None,
        "full_frame_rate": rate([bool_value(row.get("document_in_frame")) for row in rows]),
        "task_success_rate": rate([bool_value(row.get("task_success")) for row in rows]),
        "retakes_avg": safe_mean(float_value(row.get("retakes")) for row in rows),
        "seconds_to_useful_reading_avg": safe_mean(
            float_value(row.get("seconds_to_useful_reading")) for row in rows
        ),
    }


def evaluate(records: list[dict]) -> dict:
    by_group: dict[str, dict[str, dict]] = {}
    for field in GROUP_FIELDS:
        groups = sorted({record.get(field, "") or "ungrouped" for record in records})
        by_group[field] = {
            group: summarize([record for record in records if (record.get(field, "") or "ungrouped") == group])
            for group in groups
        }
    return {
        "metadata": {
            "created_at": now_iso(),
            "git_commit": git_commit(),
            "evaluator": "ocr_manifest",
            "samples": len(records),
            "warning": "CER/WER require human-verified ground truth text.",
        },
        "metrics": summarize(records),
        "by_group": by_group,
    }


def markdown(report: dict) -> str:
    metrics = report["metrics"]
    return "\n".join(
        [
            "# OCR Evaluation Report",
            "",
            f"- Commit: `{report['metadata']['git_commit']}`",
            f"- Samples: {report['metadata']['samples']}",
            f"- CER: {metrics['cer']}",
            f"- WER: {metrics['wer']}",
            f"- Full frame rate: {metrics['full_frame_rate']}",
            f"- Task success rate: {metrics['task_success_rate']}",
            f"- Retakes avg: {metrics['retakes_avg']}",
            f"- Seconds to useful reading avg: {metrics['seconds_to_useful_reading_avg']}",
            "",
            "Warning: results require consented captures and verified reference text.",
            "",
        ]
    )


def main() -> int:
    parser = argparse.ArgumentParser(description="Evaluate VOXIA OCR manifests.")
    parser.add_argument("--manifest", required=True, type=Path, help="CSV, JSON or JSONL OCR manifest.")
    parser.add_argument("--output-dir", type=Path, default=Path(__file__).with_name("reports"))
    parser.add_argument("--name", default="ocr_report")
    args = parser.parse_args()

    report = evaluate(read_records(args.manifest))
    json_path = write_json_report(report, args.output_dir, args.name)
    md_path = args.output_dir / f"{args.name}.md"
    md_path.write_text(markdown(report), encoding="utf-8")
    print(f"Wrote {json_path}")
    print(f"Wrote {md_path}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
