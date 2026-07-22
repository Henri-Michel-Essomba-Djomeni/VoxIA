# Évaluation reproductible — VOXIA

Ce dossier contient les premiers harnais de mesure pour passer d'un prototype à une baseline vérifiable. Les templates fournis sont factices et ne doivent jamais servir de chiffres publics.

## Structure

```text
evaluation/
  README.md
  common/
  schemas/
  intent/
    datasets/
    evaluate.py
    reports/
    rules_baseline.json
  stt/
    manifests/
    evaluate.py
    reports/
    source_manifests/
  ocr/
    manifests/
    evaluate.py
    reports/
  field/
    checklists/
    reports/
  vision_specialized/
    manifests/
    reports/
```

## Intentions

Entrée minimale : CSV, JSON ou JSONL avec `sample_id`, `utterance`, `expected_intent`.

Commande :

```bash
python evaluation/intent/evaluate.py --dataset evaluation/intent/datasets/template.csv
```

Métriques produites :

- exactitude ;
- macro-F1 ;
- rappel/précision/F1 par intention ;
- matrice de confusion ;
- taux d'abstention ;
- taux de fausse action ;
- ventilation par sous-groupe.

## STT

Entrée minimale : `sample_id`, `reference_text`, `hypothesis_text`.

Les fichiers dans `stt/source_manifests/` sont des sources de travail reproductibles. Ils ne doivent pas être confondus avec des manifestes d'évaluation tant que `hypothesis_text` n'a pas été produit par VOXIA sur les audios correspondants.

Commande :

```bash
python evaluation/stt/evaluate.py --manifest evaluation/stt/manifests/template.csv
```

Métriques produites :

- WER global ;
- réussite sémantique si `semantic_success` est renseigné ;
- taux d'échec d'initialisation ;
- latence moyenne jusqu'au premier/final texte ;
- real-time factor, RAM et batterie si disponibles ;
- ventilation par moteur, accent, bruit et appareil.

## OCR

Entrée minimale : `sample_id`, `reference_text`, `hypothesis_text`.

Commande :

```bash
python evaluation/ocr/evaluate.py --manifest evaluation/ocr/manifests/template.csv
```

Métriques produites :

- CER ;
- WER ;
- taux de document entièrement cadré ;
- nombre moyen de reprises ;
- temps moyen jusqu'à lecture utile ;
- taux de tâche réussie ;
- ventilation par type de document, lumière et appareil.

## Règle de publication

Un rapport n'est publiable que s'il indique :

- commit Git ;
- version du moteur ou modèle ;
- appareil et OS ;
- paramètres ;
- date ;
- dataset consenti et gelé ;
- limites connues.

Les scripts génèrent du JSON et du Markdown dans `reports/`. Les résultats issus des templates sont des tests de fumée, pas des preuves de qualité.

## Terrain téléphone réel

Les tests téléphone réel utilisent `evaluation/field/checklists/phone_real_smoke.csv` et le protocole `docs/PROTOCOLE_TESTS_TERRAIN_TELEPHONE.md`. Les preuves locales restent dans `evaluation/field/reports/` et ne sont pas versionnées sans anonymisation.
