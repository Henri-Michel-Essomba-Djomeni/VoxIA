# VOXIA alpha interne

VOXIA est actuellement un prototype Android vocal et visuel francophone. Le dépôt doit être lu comme une base de travail interne, pas comme une version publique validée.

## État réel

- Application Android `com.voxia.assistant`, compatible Android 10/API 29 ou supérieur.
- Interaction principale par bouton vocal, basée sur Android `SpeechRecognizer`.
- Synthèse vocale Android TTS.
- Vision générique via ML Kit Image Labeling, OCR latin et lecture de codes-barres.
- Actions système limitées : composeur d'appel, ouverture d'application, alarmes/minuteurs, volume, date, heure, batterie et notifications après autorisation.
- Classification d'intentions par règles locales, sans modèle `intent_classifier.tflite` livré.
- Aucun modèle Vosk, YOLO ou classifieur VOXIA entraîné n'est embarqué dans l'application.

## Artefact de baseline

L'APK `VOXIA-1.0.0-release.apk` présent dans le dépôt est conservé comme artefact de baseline historique. Il ne doit pas être présenté comme une version 1.0 publique ni comme une validation produit.

La version source courante est marquée `0.1.0-alpha-internal`.

## Documents importants

- Plan directeur : [PLAN_ACTION_VOXIA.md](PLAN_ACTION_VOXIA.md)
- Journal de reprise Codex : [CODEX_EVOLUTION_VOXIA.md](CODEX_EVOLUTION_VOXIA.md)
- Installation et limites de la baseline : [GUIDE_INSTALLATION.md](GUIDE_INSTALLATION.md)
- Inventaire fonctionnel : [docs/FONCTIONS_REELLES.md](docs/FONCTIONS_REELLES.md)
- Registre des risques : [docs/REGISTRE_RISQUES.md](docs/REGISTRE_RISQUES.md)
- Registre des décisions : [docs/REGISTRE_DECISIONS.md](docs/REGISTRE_DECISIONS.md)
- Gouvernance des données : [docs/GOUVERNANCE_DONNEES.md](docs/GOUVERNANCE_DONNEES.md)
- Évaluation reproductible : [evaluation/README.md](evaluation/README.md)

## Règle de communication

Aucun chiffre de précision, WER, CER, F1, taux de réussite ou disponibilité publique ne doit être annoncé sans protocole, jeu de test gelé, commit Git et rapport reproductible dans `evaluation/`.
