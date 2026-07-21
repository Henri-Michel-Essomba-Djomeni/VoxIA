from __future__ import annotations

import re
import unicodedata
from statistics import mean
from typing import Iterable, Sequence


def normalize_text(value: str) -> str:
    value = unicodedata.normalize("NFD", value.lower())
    value = "".join(ch for ch in value if unicodedata.category(ch) != "Mn")
    value = re.sub(r"[^a-z0-9:+*/().,\-\s]", " ", value)
    value = re.sub(r"\s+", " ", value).strip()
    return value


def words(value: str) -> list[str]:
    normalized = normalize_text(value)
    return normalized.split() if normalized else []


def edit_distance(left: Sequence[str] | str, right: Sequence[str] | str) -> int:
    previous = list(range(len(right) + 1))
    for i, left_item in enumerate(left, start=1):
        current = [i]
        for j, right_item in enumerate(right, start=1):
            cost = 0 if left_item == right_item else 1
            current.append(
                min(
                    previous[j] + 1,
                    current[j - 1] + 1,
                    previous[j - 1] + cost,
                )
            )
        previous = current
    return previous[-1]


def word_error_rate(reference: str, hypothesis: str) -> float:
    ref = words(reference)
    hyp = words(hypothesis)
    if not ref:
        return 0.0 if not hyp else 1.0
    return edit_distance(ref, hyp) / len(ref)


def character_error_rate(reference: str, hypothesis: str) -> float:
    ref = normalize_text(reference)
    hyp = normalize_text(hypothesis)
    if not ref:
        return 0.0 if not hyp else 1.0
    return edit_distance(ref, hyp) / len(ref)


def safe_mean(values: Iterable[float]) -> float | None:
    clean = [value for value in values if value is not None]
    return mean(clean) if clean else None


def precision_recall_f1(expected: list[str], predicted: list[str]) -> dict:
    labels = sorted(set(expected) | set(predicted))
    per_label = {}
    f1_values = []
    for label in labels:
        tp = sum(1 for exp, pred in zip(expected, predicted) if exp == label and pred == label)
        fp = sum(1 for exp, pred in zip(expected, predicted) if exp != label and pred == label)
        fn = sum(1 for exp, pred in zip(expected, predicted) if exp == label and pred != label)
        precision = tp / (tp + fp) if tp + fp else 0.0
        recall = tp / (tp + fn) if tp + fn else 0.0
        f1 = (2 * precision * recall / (precision + recall)) if precision + recall else 0.0
        per_label[label] = {
            "precision": precision,
            "recall": recall,
            "f1": f1,
            "support": sum(1 for exp in expected if exp == label),
        }
        if per_label[label]["support"] > 0:
            f1_values.append(f1)
    return {
        "macro_f1": mean(f1_values) if f1_values else None,
        "per_label": per_label,
    }


def confusion_matrix(expected: list[str], predicted: list[str]) -> dict[str, dict[str, int]]:
    matrix: dict[str, dict[str, int]] = {}
    for exp, pred in zip(expected, predicted):
        matrix.setdefault(exp, {})
        matrix[exp][pred] = matrix[exp].get(pred, 0) + 1
    return matrix
