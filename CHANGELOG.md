# Changelog VOXIA

Ce fichier suit les changements produit et techniques par version. Il ne remplace ni le plan directeur, ni les ADR, ni les rapports de test.

Format : Added, Changed, Fixed, Security, Verification et Known limitations.

## [Unreleased] — cible 0.2.0-alpha-offline

### Changed

- Gouvernance documentaire consolidée autour d'un plan directeur unique.
- Périmètre séparé entre version hors ligne engagée et fonctions en ligne futures.
- Gates, responsabilités, Definition of Done et indicateurs de release définis.

### Verification

- Baseline documentaire fondée sur le commit `b2fe2d461f13637af378356a8ce0839e0e7c49f0`.
- 35 tests unitaires distincts réussis sur debug et release, soit 70 exécutions.
- Android Lint : 0 erreur, 12 avertissements de versions.
- APK debug et AAB release construits localement.
- Test Vision Python : 14 réussites, 10 tests ignorés et 1 échec faute de modèle YOLO.

### Known limitations

- Aucun test Android instrumenté.
- 0 scénario terrain exécuté sur 30.
- TalkBack, police 200 %, mode avion et performances non validés sur téléphone.
- Plusieurs problèmes P0 de cycle de vie, accessibilité, dépendances et release restent ouverts dans le registre des risques.

## [0.1.0-alpha-internal] — 2026-07-22

### Added

- Interaction vocale Android, TTS, OCR, étiquetage d'image et codes-barres ML Kit.
- Lecture OCR segmentée avec navigation, vitesse, copie et partage explicites.
- Confirmations pour plusieurs actions sensibles.
- Catalogue produit local sourcé avec réponse honnête « produit inconnu ».
- Abstention financière, registres de risques/décisions et harnais d'évaluation.
- CI de tests, lint, APK debug et AAB release.

### Security

- Journalisation du contenu vocal/OCR brut supprimée en release.
- Sauvegarde Android et trafic HTTP en clair désactivés.

### Known limitations

- L'APK `VOXIA-1.0.0-release.apk` est une baseline historique et ne correspond pas à cette version source.
- Aucun modèle Vosk, Whisper, YOLO ou classifieur TFLite VOXIA n'est livré.
