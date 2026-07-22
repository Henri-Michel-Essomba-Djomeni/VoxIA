from __future__ import annotations

import unittest
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from import_fleurs_stt_sample import parse_tsv, select_balanced


class FleursImportTest(unittest.TestCase):
    def test_parse_tsv_reads_expected_columns(self) -> None:
        content = (
            "1\ta.wav\tBonjour VOXIA.\tbonjour voxia\tb o n j o u r | v o x i a |\t32000\tMALE\n"
        )

        rows = parse_tsv(content)

        self.assertEqual(1, len(rows))
        self.assertEqual("fleurs-fr-dev-1-a", rows[0].sample_id)
        self.assertEqual("male", rows[0].gender)
        self.assertEqual(2, rows[0].word_count)
        self.assertEqual("short", rows[0].length_bucket)

    def test_select_balanced_deduplicates_reference_text(self) -> None:
        content = "\n".join(
            [
                "1\ta.wav\tTexte un.\ttexte un\tx\t32000\tMALE",
                "1\tb.wav\tTexte un.\ttexte un\tx\t33000\tFEMALE",
                "2\tc.wav\tTexte deux assez long pour le test.\ttexte deux assez long pour le test\tx\t34000\tFEMALE",
            ]
        )

        selected = select_balanced(parse_tsv(content), limit=10)

        self.assertEqual(2, len(selected))
        self.assertEqual({"texte un", "texte deux assez long pour le test"}, {row.normalized_reference_text for row in selected})


if __name__ == "__main__":
    unittest.main()
