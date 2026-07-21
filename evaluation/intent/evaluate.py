from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path
from statistics import mean

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT))

from common.io import git_commit, now_iso, read_records, write_json_report
from common.metrics import confusion_matrix, normalize_text, precision_recall_f1


def phrase_score(text: str, raw_phrase: str) -> int:
    phrase = normalize_text(raw_phrase)
    if text == phrase:
        return 100
    if f" {phrase} " in f" {text} ":
        return 10 + len(phrase.split()) * 3
    return 0


def predict(utterance: str, rules: dict[str, list[str]]) -> tuple[str, float]:
    normalized = normalize_text(utterance)
    scored: list[tuple[str, int]] = []
    for intent, phrases in rules.items():
        best = max((phrase_score(normalized, phrase) for phrase in phrases), default=0)
        if best > 0:
            scored.append((intent, best))
    scored.sort(key=lambda item: item[1], reverse=True)
    if not scored:
        return "FALLBACK", 0.0
    best_intent, best_score = scored[0]
    second_score = scored[1][1] if len(scored) > 1 else 0
    if best_score >= 100:
        confidence = 0.99
    elif second_score == 0:
        confidence = 0.90
    else:
        confidence = max(0.5, min(0.95, best_score / (best_score + second_score)))
    return best_intent, confidence


def normalize_intent(value: str) -> str:
    return normalize_text(value).upper().replace(" ", "_")


def evaluate(records: list[dict], rules: dict[str, list[str]], group_field: str) -> dict:
    rows = []
    for record in records:
        expected = normalize_intent(record.get("expected_intent", "FALLBACK"))
        utterance = record.get("utterance", "")
        predicted, confidence = predict(utterance, rules)
        rows.append(
            {
                "sample_id": record.get("sample_id", ""),
                "expected": expected,
                "predicted": predicted,
                "confidence": confidence,
                "group": record.get(group_field, "") or "ungrouped",
            }
        )

    total = len(rows)
    expected_values = [row["expected"] for row in rows]
    predicted_values = [row["predicted"] for row in rows]
    correct = sum(1 for row in rows if row["expected"] == row["predicted"])
    abstentions = sum(1 for row in rows if row["predicted"] == "FALLBACK")
    false_actions = sum(
        1
        for row in rows
        if row["predicted"] != "FALLBACK" and row["predicted"] != row["expected"]
    )

    by_group = {}
    for group in sorted({row["group"] for row in rows}):
        group_rows = [row for row in rows if row["group"] == group]
        group_total = len(group_rows)
        by_group[group] = {
            "samples": group_total,
            "accuracy": (
                sum(1 for row in group_rows if row["expected"] == row["predicted"]) / group_total
                if group_total
                else None
            ),
            "abstention_rate": (
                sum(1 for row in group_rows if row["predicted"] == "FALLBACK") / group_total
                if group_total
                else None
            ),
        }

    prf = precision_recall_f1(expected_values, predicted_values) if rows else {"macro_f1": None, "per_label": {}}
    return {
        "metadata": {
            "created_at": now_iso(),
            "git_commit": git_commit(),
            "evaluator": "intent_rules_baseline",
            "samples": total,
            "warning": "Do not publish metrics from templates or non-frozen datasets.",
        },
        "metrics": {
            "accuracy": correct / total if total else None,
            "macro_f1": prf["macro_f1"],
            "abstention_rate": abstentions / total if total else None,
            "false_action_rate": false_actions / total if total else None,
            "mean_confidence": mean([row["confidence"] for row in rows]) if rows else None,
        },
        "per_intent": prf["per_label"],
        "confusion_matrix": confusion_matrix(expected_values, predicted_values) if rows else {},
        "by_group": by_group,
        "predictions": rows,
    }


def markdown(report: dict) -> str:
    metrics = report["metrics"]
    lines = [
        "# Intent Evaluation Report",
        "",
        f"- Commit: `{report['metadata']['git_commit']}`",
        f"- Samples: {report['metadata']['samples']}",
        f"- Accuracy: {metrics['accuracy']}",
        f"- Macro-F1: {metrics['macro_f1']}",
        f"- Abstention rate: {metrics['abstention_rate']}",
        f"- False action rate: {metrics['false_action_rate']}",
        "",
        "Warning: do not publish these metrics unless the dataset is consented, frozen and documented.",
        "",
    ]
    return "\n".join(lines)


def main() -> int:
    parser = argparse.ArgumentParser(description="Evaluate the VOXIA intent rules baseline.")
    parser.add_argument("--dataset", required=True, type=Path, help="CSV, JSON or JSONL intent dataset.")
    parser.add_argument("--rules", type=Path, default=Path(__file__).with_name("rules_baseline.json"))
    parser.add_argument("--group-field", default="subgroup")
    parser.add_argument("--output-dir", type=Path, default=Path(__file__).with_name("reports"))
    parser.add_argument("--name", default="intent_report")
    args = parser.parse_args()

    rules = json.loads(args.rules.read_text(encoding="utf-8"))
    records = read_records(args.dataset)
    report = evaluate(records, rules, args.group_field)
    json_path = write_json_report(report, args.output_dir, args.name)
    md_path = args.output_dir / f"{args.name}.md"
    md_path.write_text(markdown(report), encoding="utf-8")
    print(f"Wrote {json_path}")
    print(f"Wrote {md_path}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
