from __future__ import annotations

import argparse
import csv
import json
import sys
from collections import Counter, defaultdict, deque
from dataclasses import dataclass
from datetime import datetime, timezone
from pathlib import Path
from urllib.request import Request, urlopen


DEFAULT_TSV_URL = "https://huggingface.co/datasets/google/fleurs/resolve/main/data/fr_fr/dev.tsv"
DEFAULT_AUDIO_ARCHIVE_URL = (
    "https://huggingface.co/datasets/google/fleurs/resolve/main/data/fr_fr/audio/dev.tar.gz"
)
DEFAULT_USER_AGENT = "VoxIADataBootstrap/0.1 (local reproducible data sourcing)"
SOURCE_DATASET = "google/fleurs fr_fr dev"
SOURCE_LICENSE = "CC-BY-4.0"


@dataclass(frozen=True)
class FleursRow:
    sentence_id: str
    audio_path: str
    reference_text: str
    normalized_reference_text: str
    num_samples: int
    gender: str

    @property
    def sample_id(self) -> str:
        stem = Path(self.audio_path).stem
        return f"fleurs-fr-dev-{self.sentence_id}-{stem}"

    @property
    def word_count(self) -> int:
        return len(self.normalized_reference_text.split())

    @property
    def length_bucket(self) -> str:
        if self.word_count <= 12:
            return "short"
        if self.word_count <= 25:
            return "medium"
        return "long"


def fetch_text(url: str, user_agent: str) -> str:
    request = Request(url, headers={"User-Agent": user_agent})
    with urlopen(request, timeout=60) as response:
        return response.read().decode("utf-8")


def parse_tsv(content: str) -> list[FleursRow]:
    rows: list[FleursRow] = []
    reader = csv.reader(content.splitlines(), delimiter="\t")
    for line_number, parts in enumerate(reader, start=1):
        if len(parts) < 7:
            raise ValueError(f"Invalid FLEURS TSV line {line_number}: expected >= 7 columns")
        sentence_id, audio_path, reference, normalized, _characters, num_samples, gender = parts[:7]
        rows.append(
            FleursRow(
                sentence_id=sentence_id.strip(),
                audio_path=audio_path.strip(),
                reference_text=reference.strip(),
                normalized_reference_text=normalized.strip(),
                num_samples=int(num_samples),
                gender=gender.strip().lower() or "unknown",
            )
        )
    return rows


def select_balanced(rows: list[FleursRow], limit: int) -> list[FleursRow]:
    buckets: dict[tuple[str, str], deque[FleursRow]] = defaultdict(deque)
    seen_texts: set[str] = set()
    for row in rows:
        if row.normalized_reference_text in seen_texts:
            continue
        seen_texts.add(row.normalized_reference_text)
        buckets[(row.gender, row.length_bucket)].append(row)

    selected: list[FleursRow] = []
    keys = sorted(buckets.keys())
    while len(selected) < limit and any(buckets[key] for key in keys):
        for key in keys:
            if buckets[key] and len(selected) < limit:
                selected.append(buckets[key].popleft())
    return selected


def write_manifest(
    rows: list[FleursRow],
    output: Path,
    source_url: str,
    audio_archive_url: str,
) -> None:
    output.parent.mkdir(parents=True, exist_ok=True)
    fieldnames = [
        "sample_id",
        "source_dataset",
        "source_split",
        "source_license",
        "source_url",
        "audio_archive_url",
        "audio_path",
        "gender",
        "word_count",
        "length_bucket",
        "reference_text",
        "normalized_reference_text",
        "hypothesis_text",
        "semantic_success",
        "notes",
    ]
    with output.open("w", encoding="utf-8", newline="") as handle:
        writer = csv.DictWriter(handle, fieldnames=fieldnames)
        writer.writeheader()
        for row in rows:
            writer.writerow(
                {
                    "sample_id": row.sample_id,
                    "source_dataset": SOURCE_DATASET,
                    "source_split": "dev",
                    "source_license": SOURCE_LICENSE,
                    "source_url": source_url,
                    "audio_archive_url": audio_archive_url,
                    "audio_path": row.audio_path,
                    "gender": row.gender,
                    "word_count": row.word_count,
                    "length_bucket": row.length_bucket,
                    "reference_text": row.reference_text,
                    "normalized_reference_text": row.normalized_reference_text,
                    "hypothesis_text": "",
                    "semantic_success": "",
                    "notes": "Source metadata only. Fill hypothesis_text after running VOXIA STT on the audio.",
                }
            )


def build_report(all_rows: list[FleursRow], selected: list[FleursRow], source_url: str) -> dict:
    return {
        "created_at": datetime.now(timezone.utc).isoformat(),
        "source_dataset": SOURCE_DATASET,
        "source_license": SOURCE_LICENSE,
        "source_url": source_url,
        "warning": "This is a source manifest, not a STT quality report. Do not publish WER until VOXIA hypotheses are collected on real audio.",
        "all_rows": len(all_rows),
        "selected_rows": len(selected),
        "gender_distribution": dict(Counter(row.gender for row in selected)),
        "length_bucket_distribution": dict(Counter(row.length_bucket for row in selected)),
        "word_count": {
            "min": min((row.word_count for row in selected), default=0),
            "max": max((row.word_count for row in selected), default=0),
            "avg": (
                round(sum(row.word_count for row in selected) / len(selected), 2)
                if selected
                else 0
            ),
        },
    }


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description="Build a traceable FLEURS FR STT source manifest.")
    parser.add_argument("--url", default=DEFAULT_TSV_URL)
    parser.add_argument("--audio-archive-url", default=DEFAULT_AUDIO_ARCHIVE_URL)
    parser.add_argument("--limit", type=int, default=36)
    parser.add_argument(
        "--output",
        type=Path,
        default=Path("evaluation/stt/source_manifests/fleurs_fr_dev_sample.csv"),
    )
    parser.add_argument(
        "--report",
        type=Path,
        default=Path("evaluation/stt/source_manifests/fleurs_fr_dev_sample.report.json"),
    )
    parser.add_argument("--user-agent", default=DEFAULT_USER_AGENT)
    args = parser.parse_args(argv)

    content = fetch_text(args.url, args.user_agent)
    rows = parse_tsv(content)
    selected = select_balanced(rows, args.limit)
    write_manifest(selected, args.output, args.url, args.audio_archive_url)

    args.report.parent.mkdir(parents=True, exist_ok=True)
    args.report.write_text(
        json.dumps(build_report(rows, selected, args.url), ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8",
    )

    print(f"Wrote {args.output}")
    print(f"Wrote {args.report}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
