# VOXIA — alpha interne

VOXIA est un assistant Android vocal et visuel, conçu en priorité pour aider une personne aveugle ou malvoyante francophone à lire, reconnaître des informations utiles et exécuter des actions simples sur son téléphone.

Le dépôt contient aujourd'hui un **prototype interne**, pas une version publique validée. L'objectif actif est de livrer une première version **hors ligne, sûre, accessible et vérifiée sur téléphone réel** avant d'ajouter des fonctions cloud.

## État au 26 août 2026

- Source : `0.1.1-alpha-internal`, Android 10/API 29 minimum, `targetSdk 34`.
- Voix : Android `SpeechRecognizer` et `TextToSpeech` ; aucun modèle Vosk ou Whisper embarqué.
- Vision : OCR latin, étiquetage générique et codes-barres ML Kit ; aucun modèle YOLO VOXIA embarqué.
- Intentions : règles Kotlin locales ; aucun classifieur TFLite livré.
- Produit : pipeline code-barres présent, mais catalogue local vide hors en-tête.
- Vérification locale : 76 tests unitaires distincts réussis sur debug et release, lint sans erreur et APK debug construit depuis la source courante.
- Étape 0 en cours : P0-002, P0-004, P0-005 et P0-006 sont implémentés côté code ; P0-003 reste en validation avec des timeouts globaux incomplets. Les preuves appareil applicables restent obligatoires.
- Validation terrain : 0 scénario sur 30 exécuté ; TalkBack, caméra, microphone, permissions, mode avion et performances restent à valider sur téléphone réel.
- Décision de diffusion : **NO-GO public** jusqu'au franchissement des gates du plan directeur.

## Objectif de la version hors ligne

La première livraison exploitable doit garantir trois parcours sans compte ni clé privée :

1. **Lire** un document avec guidage caméra, contrôle de la lecture et erreurs compréhensibles.
2. **Reconnaître** un code-barres ou une information visuelle limitée, avec source ou abstention.
3. **Agir** sur le téléphone avec confirmation des actions sensibles et annulation fiable.

Les fonctions en ligne seront une couche optionnelle ultérieure, avec consentement, politique de confidentialité et repli local explicite.

## Source de vérité documentaire

| Besoin | Document autoritaire |
|---|---|
| Où va le produit, dans quel ordre et avec quelles gates | [PLAN_ACTION_VOXIA.md](PLAN_ACTION_VOXIA.md) |
| Ce qui existe réellement dans le code | [docs/FONCTIONS_REELLES.md](docs/FONCTIONS_REELLES.md) |
| Ce qui a changé par version et ce qui a été vérifié | [CHANGELOG.md](CHANGELOG.md) |
| Décisions d'architecture et de produit | [docs/REGISTRE_DECISIONS.md](docs/REGISTRE_DECISIONS.md) |
| Risques ouverts, responsables et réduction | [docs/REGISTRE_RISQUES.md](docs/REGISTRE_RISQUES.md) |
| Installation, build et validation d'un artefact | [GUIDE_INSTALLATION.md](GUIDE_INSTALLATION.md) |
| Données et consentement | [docs/GOUVERNANCE_DONNEES.md](docs/GOUVERNANCE_DONNEES.md) |
| Mesures reproductibles | [evaluation/README.md](evaluation/README.md) |
| Validation sur téléphone | [docs/PROTOCOLE_TESTS_TERRAIN_TELEPHONE.md](docs/PROTOCOLE_TESTS_TERRAIN_TELEPHONE.md) |

`CODEX_EVOLUTION_VOXIA.md` et `STRATEGIE_IMPLEMENTATION.md` sont conservés comme archives/pointeurs historiques. Ils ne doivent plus recevoir de nouvelles décisions.

## Commandes de contrôle

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat test
.\gradlew.bat lintDebug
.\gradlew.bat assembleDebug
.\gradlew.bat bundleRelease
```

L'APK debug courant est généré dans `app/build/outputs/apk/debug/app-debug.apk`. L'APK `VOXIA-1.0.0-release.apk` à la racine est une baseline historique et ne représente pas la source actuelle.

## Règle de publication

Aucun chiffre de précision, WER, CER, F1, taux de réussite ou disponibilité publique ne doit être annoncé sans protocole, dataset gelé, commit Git et rapport reproductible. Aucune version ne doit être dite accessible ou prête tant que les tests téléphone/TalkBack correspondants ne sont pas exécutés.
