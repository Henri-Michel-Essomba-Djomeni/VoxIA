# Sources web et données de travail — VOXIA

Date : 2026-07-22
Agent : Codex
Portée : sources web analysées pour préparer des données de travail sans modifier `PLAN_ACTION_VOXIA.md`.

## Règle appliquée

Les données récupérées doivent rester traçables, licenciées et limitées à un usage de préparation. Aucun chiffre de qualité STT, OCR ou produit ne doit être annoncé tant que VOXIA n'a pas produit ses propres hypothèses sur un protocole vérifié.

## Sources retenues ou analysées

| Source | Usage VOXIA | Licence / conditions | État | Limites professionnelles |
|---|---|---|---|---|
| Google FLEURS `fr_fr` | Manifeste source STT français, reproductible, pour future mesure WER | CC-BY-4.0 | Import réussi du split `dev.tsv` : 289 lignes lues, 36 échantillons sélectionnés | Ce n'est pas un rapport STT. Les audios doivent encore être exécutés dans VOXIA et `hypothesis_text` doit rester vide jusque-là. |
| Mozilla Common Voice Français 26.0 | Candidat futur pour accents, variantes et robustesse STT | CC0-1.0, avec interdiction de ré-identification et de redistribution brute | Analyse web faite, pas de téléchargement local | Les variantes françaises africaines existent mais restent peu couvertes ; ne pas présenter comme baseline terrain Afrique sans collecte locale consentie. |
| Open Food Facts | Candidat futur pour enrichir le catalogue produit local source/date | Base ouverte ODbL, API avec User-Agent et usage raisonnable | API publique temporairement indisponible lors de l'essai local du 2026-07-22 | Ne pas forcer le scrapping. Ne pas inventer prix, allergènes, composition ou disponibilité ; utiliser bulk dumps ou accès stabilisé si besoin. |

## Données générées

- `evaluation/stt/source_manifests/fleurs_fr_dev_sample.csv`
- `evaluation/stt/source_manifests/fleurs_fr_dev_sample.report.json`

Analyse du manifeste FLEURS généré :

- lignes sources lues : 289 ;
- échantillons retenus : 36 ;
- répartition genre source : 15 `female`, 21 `male` ;
- répartition longueur : 8 `short`, 14 `medium`, 14 `long` ;
- mots par référence : minimum 9, maximum 46, moyenne 23,11.

## Reproduction

```powershell
$env:PYTHONDONTWRITEBYTECODE='1'
& 'C:\Users\HP\.cache\codex-runtimes\codex-primary-runtime\dependencies\python\python.exe' tools\data\import_fleurs_stt_sample.py --limit 36
```

Le script ne télécharge pas l'archive audio par défaut. Il produit un manifeste source avec `hypothesis_text` vide afin d'éviter toute confusion entre source de test et mesure de qualité.

## Références

- FLEURS sur Hugging Face : https://huggingface.co/datasets/google/fleurs/tree/main/data/fr_fr
- Publication Google Research FLEURS : https://research.google/pubs/fleurs-few-shot-learning-evaluation-of-universal-representations-of-speech/
- Mozilla Common Voice French 26.0 : https://mozilladatacollective.com/datasets/cmqim41b000tanr07q9btypkc
- Documentation API Open Food Facts : https://openfoodfacts.github.io/openfoodfacts-server/api/
- Conditions API Open Food Facts : https://support.openfoodfacts.org/help/en-gb/12-api-data-reuse/94-are-there-conditions-to-use-the-api
