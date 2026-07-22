# Manifestes source STT

Ce dossier contient des manifestes de sources audio/transcription utilisables pour préparer une évaluation STT. Ce ne sont pas des rapports de qualité VOXIA.

## Manifeste FLEURS

Fichier : `fleurs_fr_dev_sample.csv`

Origine : Google FLEURS `fr_fr`, split `dev`, licence CC-BY-4.0.

Le manifeste référence 36 échantillons sélectionnés depuis 289 lignes sources. Il conserve les références et les chemins audio d'origine, mais laisse `hypothesis_text` vide.

## Utilisation correcte

1. Récupérer les audios depuis la source officielle.
2. Exécuter chaque audio dans VOXIA ou dans un runner contrôlé représentant le moteur STT cible.
3. Copier les hypothèses VOXIA dans un manifeste d'évaluation sous `evaluation/stt/manifests/`.
4. Lancer `evaluation/stt/evaluate.py` uniquement lorsque référence, hypothèse, moteur, appareil et contexte sont renseignés.
5. Archiver le rapport produit sous `evaluation/stt/reports/`.

Ne pas publier de WER depuis ce dossier seul : il ne contient pas encore de sorties VOXIA.
