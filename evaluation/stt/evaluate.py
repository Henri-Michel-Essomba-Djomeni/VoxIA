from __future__ import annotations

import argparse
import sys
from pathlib import Path
from statistics import mean

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT))

from common.io import bool_value, float_value, git_commit, now_iso, read_records, write_json_report
from common.metrics import safe_mean, word_error_rate


GROUP_FIELDS = ["engine", "accent", "noise", "device_model"]


def summarize(rows: list[dict]) -> dict:
    total = len(rows)
    wers = [word_error_rate(row.get("reference_text", ""), row.get("hypothesis_text", "")) for row in rows]
    semantic_values = [bool_value(row.get("semantic_success")) for row in rows]
    init_failures = [bool_value(row.get("initialization_failed")) for row in rows]

    def avg_field(name: str) -> float | None:
        return safe_mean(float_value(row.get(name)) for row in rows)

    return {
        "samples": total,
        "wer": mean(wers) if wers else None,
        "semantic_success_rate": (
            sum(1 for value in semantic_values if value is True)
            / sum(1 for value in semantic_values if value is not None)
            if any(value is not None for value in semantic_values)
            else None
        ),
        "initialization_failure_rate": (
            sum(1 for value in init_failures if value is True)
            / sum(1 for value in init_failures if value is not None)
            if any(value is not None for value in init_failures)
            else None
        ),
        "first_text_ms_avg": avg_field("first_text_ms"),
        "final_text_ms_avg": avg_field("final_text_ms"),
        "real_time_factor_avg": avg_field("real_time_factor"),
        "ram_peak_mb_avg": avg_field("ram_peak_mb"),
        "battery_delta_pct_avg": avg_field("battery_delta_pct"),
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
            "evaluator": "stt_manifest",
            "samples": len(records),
            "warning": "WER is meaningful only with human-verified reference transcripts.",
        },
        "metrics": summarize(records),
        "by_group": by_group,
    }


def markdown(report: dict) -> str:
    metrics = report["metrics"]
    return "\n".join(
        [
            "# STT Evaluation Report",
            "",
            f"- Commit: `{report['metadata']['git_commit']}`",
            f"- Samples: {report['metadata']['samples']}",
            f"- WER: {metrics['wer']}",
            f"- Semantic success rate: {metrics['semantic_success_rate']}",
            f"- Initialization failure rate: {metrics['initialization_failure_rate']}",
            f"- First text avg ms: {metrics['first_text_ms_avg']}",
            f"- Final text avg ms: {metrics['final_text_ms_avg']}",
            "",
            "Warning: results require consented audio, verified transcripts and documented devices.",
            "",
        ]
    )


def main() -> int:
    parser = argparse.ArgumentParser(description="Evaluate VOXIA STT manifests.")
    parser.add_argument("--manifest", required=True, type=Path, help="CSV, JSON or JSONL STT manifest.")
    parser.add_argument("--output-dir", type=Path, default=Path(__file__).with_name("reports"))
    parser.add_argument("--name", default="stt_report")
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
