# Changelog VOXIA

Ce fichier suit les changements produit et techniques par version. Il ne remplace ni le plan directeur, ni les ADR, ni les rapports de test.

Format : Added, Changed, Fixed, Security, Verification et Known limitations.

## [Unreleased] — cible 0.2.0-alpha-offline

### Changed

- Gouvernance documentaire consolidée autour d'un plan directeur unique.
- Périmètre séparé entre version hors ligne engagée et fonctions en ligne futures.
- Gates, responsabilités, Definition of Done et indicateurs de release définis.
- Version source incrémentée à `0.1.1-alpha-internal` (`versionCode 4`) pour identifier le premier lot technique de l'étape 0 sans le présenter comme une release offline validée.

### Fixed

- Les actions tactiles et caméra sont liées au service même lorsque le microphone est refusé ; elles sont mises en file pendant l'initialisation avec un retour visible au lieu d'un silence.
- Un résultat STT final vide produit une erreur terminale au lieu de laisser l'état en écoute.
- Les erreurs et refus immédiats du moteur TTS exécutent le callback terminal une seule fois.
- Les exceptions d'initialisation CameraX/OCR sont converties en échec terminal contrôlé.
- Les confirmations sensibles expirent après 20 secondes, sont liées à un jeton unique et ne peuvent plus être rejouées par une commande tardive.
- Les demandes et choix de contact expirent ; un résultat ambigu impose un choix numéroté puis une confirmation liée au numéro exact avant `ACTION_DIAL`.
- La V0 hors ligne utilise une politique FR-only centrale : UI, classification, STT et TTS restent en français ; une demande d'anglais explique la limite sans modifier un sous-système isolé.
- Les réponses VOXIA ont désormais un propriétaire vocal unique : le TTS parle, tandis que TalkBack annonce uniquement les états utiles et les retours UI non vocalisés.
- Le TTS acquiert et libère un focus audio transitoire, s'interrompt proprement lors d'une perte de focus et ignore les callbacks devenus obsolètes après arrêt.
- Un refus de focus ou une erreur moteur ne déclenche plus le callback de succès ; le prompt vocal revient à l'état prêt sans ouvrir le microphone et les réponses échouées sont rendues à TalkBack.

### Verification

- Baseline documentaire publiée sur `main` au commit `9ef1ac549099ef1a03f97d7606e3dc98531b52a2` ; lots techniques vérifiés aux commits `ff300c1`, `6047ea1`, `84d2389` et `f4777f0`.
- 76 tests unitaires distincts réussis sur debug et release, soit 152 exécutions, sans échec ni test ignoré.
- Android Lint : 0 erreur, 12 avertissements de versions.
- APK debug courant construit localement (118 527 551 octets) ; la preuve release signée reste celle du lot précédent.
- APK release `0.1.1-alpha-internal` : 103 394 179 octets, SHA-256 `9CBF21956FA139132089347D172F501613BA0917CA6BA73E70A48FC12330F07B`.
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
