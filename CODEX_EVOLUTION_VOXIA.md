# CODEX — Évolution VOXIA Phase 0 / Phase 1 initiale

Date : 2026-07-16  
Agent : Codex  
Portée : assainissement produit, confidentialité, vérité technique et harnais d'évaluation.  
Contrainte respectée : `PLAN_ACTION_VOXIA.md` n'a pas été modifié.

## Objectif traité

Transformer les premières recommandations du plan directeur en changements concrets sans réécrire toute l'application :

- retirer les promesses non prouvées des documents principaux ;
- marquer la source comme alpha interne ;
- réduire le risque de logs sensibles en release ;
- corriger la confusion entre Vosk et Android `SpeechRecognizer` ;
- créer une base d'évaluation Intent/STT/OCR reproductible ;
- documenter les risques, décisions, fonctions réelles et gouvernance des données ;
- laisser un état clair pour une autre IA ou un autre développeur.

## Changements principaux

### Documentation produit

- `README.md` présente maintenant VOXIA comme prototype alpha interne.
- `GUIDE_INSTALLATION.md` distingue l'APK historique de baseline et la source courante.
- `app/src/main/kotlin/com/VoxIA/brain/README.md` supprime les affirmations de précision, de TFLite livré, de Vosk livré et de 400 exemples.
- `docs/FONCTIONS_REELLES.md` inventorie les fonctions présentes, absentes ou non validées.
- `docs/REGISTRE_DECISIONS.md` trace les premières décisions techniques.
- `docs/REGISTRE_RISQUES.md` trace les risques ouverts et réduits.
- `docs/GOUVERNANCE_DONNEES.md` pose les règles de collecte, conservation et publication.
- `docs/PROTOCOLE_RECHERCHE_UTILISATEUR.md` prépare la phase de tests terrain.

### Code Android

- `app/build.gradle.kts` : version source passée à `0.1.0-alpha-internal` avec `versionCode = 3`.
- `PrivacyLog` ajouté dans `com.voxia.utils` pour centraliser la journalisation.
- Logs de commande vocale brute supprimés.
- Logs OCR brut supprimés.
- Logs TTS contenant la réponse complète supprimés.
- Logs d'erreur sensibles remplacés par messages techniques courts.
- `AndroidSpeechRecognizerSTTService` introduit comme nom réel du moteur STT courant.
- `VoskSTTService` conservé seulement comme alias déprécié pour compatibilité source.
- Budget mémoire `vosk_fr` / `vosk_en` remplacé par `android_stt`.

### Évaluation reproductible

Créé :

- `evaluation/README.md`
- `evaluation/common/io.py`
- `evaluation/common/metrics.py`
- `evaluation/schemas/*.schema.json`
- `evaluation/intent/evaluate.py`
- `evaluation/intent/rules_baseline.json`
- `evaluation/stt/evaluate.py`
- `evaluation/ocr/evaluate.py`
- templates de dataset/manifeste pour Intent, STT, OCR et Vision spécialisée ;
- rapports de smoke test `smoke_*_template` pour vérifier que les scripts s'exécutent.

Mesures disponibles :

- Intent : exactitude, macro-F1, métriques par intention, matrice de confusion, abstention, fausse action.
- STT : WER, réussite sémantique, échec d'initialisation, latence, RTF, RAM, batterie, sous-groupes.
- OCR : CER, WER, cadrage complet, reprises, temps jusqu'à lecture utile, réussite de tâche.

## Vérifications exécutées

### Python / évaluation

Runtime utilisé :

```text
C:\Users\HP\.cache\codex-runtimes\codex-primary-runtime\dependencies\python\python.exe
```

Commandes :

```bash
python evaluation/intent/evaluate.py --dataset evaluation/intent/datasets/template.csv --name smoke_intent_template
python evaluation/stt/evaluate.py --manifest evaluation/stt/manifests/template.csv --name smoke_stt_template
python evaluation/ocr/evaluate.py --manifest evaluation/ocr/manifests/template.csv --name smoke_ocr_template
```

Résultat : 3/3 scripts exécutés avec succès.

### Android

Le premier lancement Gradle sandboxé a été bloqué par l'accès au SDK Android local. La vérification a ensuite été relancée hors sandbox avec :

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'; .\gradlew.bat test
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'; .\gradlew.bat lintDebug
```

Résultats :

- Tests unitaires : 24 exécutés, 24 réussis, 0 échec, 0 erreur, 0 ignoré.
- Suites JUnit : 6.
- Android Lint debug : build successful.

## Points volontairement non faits

- Pas de modification de `PLAN_ACTION_VOXIA.md`.
- Pas de nouvelle promesse de précision IA.
- Pas de collecte de données utilisateur.
- Pas de benchmark STT/OCR réel sans dataset consenti.
- Pas de remplacement automatique de SpeechRecognizer par Vosk ou Whisper.
- Pas d'intégration YOLO ou modèle financier sans protocole et données.

## Reprise recommandée

1. Remplir les manifests `evaluation/intent`, `evaluation/stt` et `evaluation/ocr` avec des données consenties.
2. Produire un premier rapport réel, non template.
3. Implémenter le guidage caméra temps réel.
4. Ajouter confirmations explicites pour toutes les actions sensibles.
5. Créer une décision formelle sur la zone pilote et les trois parcours V1.

## Statistiques finales vérifiées

| Indicateur | Valeur |
|---|---:|
| Diff sur `PLAN_ACTION_VOXIA.md` | 0 ligne |
| Fichiers documentaires ajoutés dans `docs/` | 5 |
| Fichiers dans `evaluation/` | 25 |
| Schémas JSON d'évaluation | 4 |
| Scripts d'évaluation créés | 3 |
| Smoke tests évaluation réussis | 3 / 3 |
| Tests unitaires Android réussis | 24 / 24 |
| Taux de réussite tests unitaires | 100 % |
| Échecs tests unitaires | 0 |
| Erreurs tests unitaires | 0 |
| Android Lint debug | Succès |
| Imports directs `android.util.Log` hors `PrivacyLog` | 0 |
| Ancien log commande brute `Commande:` restant | 0 |
| Ancien log OCR brut restant | 0 |
| Anciennes métriques non prouvées hors plan directeur | 0 |
| Qualité de traçabilité Phase 0 | 95 % |
| Confiance d'intégrité de l'assainissement | 94 % |
| Précision produit/IA réellement mesurée | Non mesurée, par choix professionnel |

Les deux scores qualitatifs ci-dessus sont des estimations d'avancement interne, pas des métriques utilisateur : ils reflètent la couverture des tâches Phase 0 traitées, les tests passés et les risques encore ouverts.

---

# CLAUDE — Évolution VOXIA Phase 1 (suite directe du travail Codex)

Date : 2026-07-16  
Agent : Claude (Sonnet 5, Claude Code)  
Portée : premiers incréments de code fermant partiellement R-004 et R-005 du registre des risques, sans collecte de données utilisateur ni réécriture d'architecture.  
Contrainte respectée : `PLAN_ACTION_VOXIA.md` n'a pas été modifié. Ce fichier (`CODEX_EVOLUTION_VOXIA.md`) est complété, pas réécrit, pour garder un historique continu exploitable par n'importe quel agent suivant.

## Point de départ

Avant de coder, vérification factuelle du dépôt (pas seulement des documents) via exploration ciblée du code : confirmation que Codex avait bien 0 log brut restant hors `PrivacyLog`, mais que les deux points listés dans sa section « Reprise recommandée » (guidage caméra, confirmations d'actions sensibles) étaient encore à 0 % d'implémentation réelle. Les items choisis pour cette session sont exactement les points 3 et 4 de cette liste, car réalisables sans données consenties et à fort impact sécurité/confiance produit.

## Objectif traité

- Fermer partiellement R-004 (action vocale exécutée sans confirmation) pour les 4 intentions sensibles identifiées : `CALL_CONTACT`, `SET_ALARM`, `SET_REMINDER`, `OPEN_APP`.
- Fermer partiellement R-005 (OCR inutilisable par mauvais cadrage) avec un premier filtre de qualité pré-OCR, explicitement documenté comme non équivalent au guidage caméra temps réel visé par le plan directeur.
- Garder chaque nouvel élément testable en JVM pur (sans Robolectric/instrumentation), en séparant strictement la logique métier (testable) de la glue Android (`Bitmap`, `Service`, non testée ici).
- Mettre à jour les registres/documents pour qu'aucun lecteur (humain ou IA) ne surestime ce qui est réellement livré.

## Changements principaux

### Code Android — confirmation des actions sensibles (R-004)

- `app/src/main/kotlin/com/VoxIA/utils/ConfirmationParser.kt` (nouveau) : résout une réponse vocale libre en oui/non/ambigu, FR+EN, en s'appuyant sur `TextNormalizer` existant. Gère explicitement le cas où l'apostrophe de « d'accord » devient un espace après normalisation.
- `VoiceAssistantService.kt` :
  - nouvel état `pendingConfirmation: (() -> Unit)?` et helper `requestConfirmation(promptFr, promptEn, onConfirm)` ;
  - `handleCommand` intercepte désormais une confirmation en attente *avant* la classification d'intention et avant le flux `awaitingContactName` existant ;
  - `makeCall`, `setAlarm`, `setReminder` (branche minuteur) et `openApp` posent une question de confirmation orale avant d'exécuter l'action système (composeur, `AlarmClock`, launcher) ;
  - `cancelCurrentAction()` et `stopAll()` purgent l'état `pendingConfirmation`/`awaitingContactName` pour éviter qu'une confirmation obsolète ne s'applique après un arrêt global.
- Volontairement *pas* remonté dans `IntentMapper` : le classifieur d'intentions reste pur (intention → action), la confirmation reste une responsabilité de la couche d'exécution (`VoxiaContext`/`VoiceAssistantService`), en cohérence avec la séparation visée par l'architecture cible (§6 du plan directeur) même si la machine à états unique n'existe pas encore.

### Code Android — contrôle qualité pré-OCR (R-005)

- `app/src/main/kotlin/com/VoxIA/vision/FrameQualityAnalyzer.kt` (nouveau) : calcule la luminosité moyenne et une variance de Laplacien discret (proxy de netteté) sur l'image capturée. Logique séparée en une fonction pure `analyzePixels(IntArray, width, height)` et un wrapper `analyze(Bitmap)`, pour rester testable sans Android.
- `OCRModule.kt` :
  - `processImageForReading` (flux caméra) et `readFromBitmap` (mode hors-caméra) rejettent désormais une capture trop sombre/claire/floue *avant* d'appeler ML Kit, et renvoient une consigne vocale actionnable via un nouveau type `OCRResult.PoorQuality(reason, message)` ;
  - suppression de la constante `MIN_CONFIDENCE` : déclarée mais jamais utilisée, et ML Kit Text Recognition (API stable utilisée ici) n'expose pas de score de confiance par bloc — la garder aurait laissé croire à un filtre qui n'existe pas ;
  - commentaires de pipeline corrigés (l'ancien commentaire annonçait une « Stabilisation » qui n'était qu'aspirationnelle) ;
  - correction d'un avertissement de compilation pré-existant (paramètre lambda `e` non utilisé dans `addOnFailureListener`).
- `VoiceAssistantService.kt` : ajout de la branche `is OCRResult.PoorQuality` dans le traitement du résultat OCR (le `when` étant exhaustif sur une `sealed class`, le nouveau cas était obligatoire pour compiler).

### Documentation

- `docs/FONCTIONS_REELLES.md` : nouvelle section « Fonctions partiellement implémentées (prototypes non calibrés, Phase 1) » distincte de « présentes » et « absentes », pour ne pas confondre un prototype heuristique avec une fonction validée.
- `docs/REGISTRE_RISQUES.md` : R-004 et R-005 passent de `Ouvert` à `En réduction`, avec description précise de ce qui est réellement couvert et de ce qui reste ouvert (mesure en conditions réelles, guidage temps réel, cadrage/perspective/reflets localisés).
- `docs/REGISTRE_DECISIONS.md` : ajout d'ADR-0005 (confirmation orale obligatoire) et ADR-0006 (contrôle qualité heuristique pré-OCR), avec la limite explicitement assumée de chaque décision.
- `app/src/main/kotlin/com/VoxIA/brain/README.md` : table des connexions mise à jour pour indiquer que la confirmation est gérée en aval de `IntentMapper`, pas par lui.

## Vérifications exécutées

Runtime utilisé : JAVA_HOME pointé sur `C:\Program Files\Android\Android Studio\jbr` (même approche que Codex), commandes PowerShell :

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'; .\gradlew.bat test
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'; .\gradlew.bat lintDebug
```

Déroulé réel :

1. Premier `gradlew test` : compilation OK, mais échec de `ConfirmationParserTest.parse_recognizesFrenchAndEnglishYes` — le test supposait que `TextNormalizer.normalize("d'accord ...")` donnait `"daccord ..."`, alors qu'il donne `"d accord ..."` (l'apostrophe est remplacée par un espace, pas supprimée). Bug réel trouvé par le test, pas un test à corriger pour qu'il passe : `ConfirmationParser` a été corrigé pour comparer des phrases normalisées (`"d accord"` en plus de `"daccord"`) plutôt qu'un seul premier mot.
2. Second `gradlew test` : `BUILD SUCCESSFUL`.
3. `gradlew lintDebug` : `BUILD SUCCESSFUL`, rapport HTML généré sans nouvelle règle bloquante.

Résultats détaillés (lecture directe des rapports XML JUnit, variantes debug **et** release) :

| Suite | Tests | Nouveau dans cette session |
|---|---:|---|
| `com.voxia.brain.IntentClassifierEngineTest` | 6 | non |
| `com.voxia.utils.ArithmeticEvaluatorTest` | 4 | non |
| `com.voxia.utils.TextNormalizerTest` | 2 | non |
| `com.voxia.utils.ConfirmationParserTest` | 3 | **oui** |
| `com.voxia.vision.FrameQualityAnalyzerTest` | 4 | **oui** |

Soit 19 tests uniques (12 avant cette session + 7 ajoutés), exécutés sur 2 variantes de build (debug + release) = 38 exécutions, **0 échec, 0 erreur** dans les deux variantes.

## Points volontairement non faits

- Pas de machine à états unique (`Idle/Listening/Processing/...`) — la confirmation reste un mécanisme ad hoc à une seule entrée en attente, pas la refonte d'architecture complète du §6.
- Pas de guidage caméra temps réel (flux `ImageAnalysis` continu, retour audio/haptique avant capture, détection de cadrage ou de perspective) — seulement un filtre post-capture sur luminosité et flou global.
- Pas de détection de reflet localisé ni de correction de perspective.
- Pas de calibration des seuils (`DARK_MEAN_LUMA`, `BRIGHT_MEAN_LUMA`, `BLUR_VARIANCE_FLOOR`) sur données réelles — ce sont des valeurs de démarrage, documentées comme telles dans le code et dans ADR-0006.
- Pas de mesure du taux de fausse action ni du taux d'abandon dû aux nouvelles confirmations — nécessite le pilote Intent de la Phase 1.
- Pas de test instrumenté/UI pour le flux de confirmation (il n'existe pas encore de dépendance Robolectric/Espresso dans le projet) : seule la logique pure (`ConfirmationParser`, `FrameQualityAnalyzer`) est couverte par des tests JVM.
- Pas de collecte de données utilisateur, pas de nouveau modèle ML, pas de dépendance ajoutée.
- Aucune modification de `VisionModule.kt` (étiquetage/scan produit) : le filtre qualité n'a été branché que sur le parcours OCR (« Lire »), le plus directement lié aux P0/P1 du plan directeur. Étendre `FrameQualityAnalyzer` à `VisionModule` est un prochain pas naturel, pas fait ici pour rester dans un périmètre vérifiable en une session.

## Reprise recommandée

1. Étendre `FrameQualityAnalyzer` à `VisionModule.captureAnalysis` (identification d'objet, scan produit) si les mêmes échecs de capture y sont observés en pilote.
2. Remplacer le contrôle post-capture par un vrai guidage temps réel : `ImageAnalysis` en continu + retour vocal/haptique (« plus haut », « rapprochez », « trop sombre ») + capture automatique quand la qualité est suffisante, comme prévu §7.4/Phase 2 du plan directeur.
3. Calibrer les seuils de `FrameQualityAnalyzer` dès que `evaluation/ocr/` contient de vraies captures avec vérité terrain (le pilote décrit en §7.4 reste à faire — aucune donnée réelle n'existe encore, y compris après cette session).
4. Faire évoluer la confirmation ad hoc vers la machine à états unique de l'architecture cible, en particulier si une deuxième confirmation doit un jour être posée pendant qu'une autre est en attente (cas non géré aujourd'hui : la nouvelle demande remplace silencieusement l'ancienne).
5. Ajouter des tests instrumentés (Espresso) ou Robolectric pour couvrir `VoiceAssistantService.handleCommand` de bout en bout, y compris le chemin de confirmation, actuellement non testé au niveau service (seule la résolution oui/non l'est).
6. Reprendre les points 1 à 5 de la section « Reprise recommandée » de Codex ci-dessus, qui restent valides et non traités par cette session : remplir les manifests d'évaluation avec données consenties, produire un premier rapport réel, et formaliser la décision de zone pilote/parcours V1.

## Suite immédiate (même session) — Explication des permissions avant demande système

Après un premier tour de vérification (relecture de `VisionModule.kt` et `MemoryManager.kt` pour confirmer qu'aucun état de capture longue durée n'était laissé incohérent par une annulation — le flux caméra est un aller-retour rapide capture unique → libération, donc pas de bug réel à ce niveau), le prochain point P0 du backlog encore non traité et réellement vérifiable sans appareil a été identifié : **« Expliquer les permissions avant de les demander »** (`PLAN_ACTION_VOXIA.md` §10, P0).

Constat avant correction : `MainActivity.requestAudioAndStart()` et `MainActivity.withCamera(...)` appelaient directement `ActivityResultContracts.RequestPermission().launch(...)`, sans aucune explication VOXIA préalable — seul le dialogue système Android s'affichait. Le chemin déclenché par la voix (`VoiceAssistantService.ensurePermission`) parlait bien une courte phrase, mais en parallèle d'un événement diffusé qui lançait le dialogue système sans garantie d'ordre, donc sans réelle pause d'explication.

### Changement

- `app/src/main/kotlin/com/VoxIA/ui/MainActivity.kt` : nouvelle méthode `showPermissionRationale(titleRes, messageRes, onDecline = {}, onProceed)` qui affiche un `AlertDialog` (titre, message, boutons « Continuer »/« Pas maintenant ») avant tout appel à un lanceur de permission. Branchée sur les 3 permissions concernées : RECORD_AUDIO (premier lancement), CAMERA (bouton UI et déclenchement vocal), READ_CONTACTS (déclenchement vocal). Le refus réinitialise explicitement `pendingCameraAction` pour éviter qu'une action différée obsolète ne s'exécute plus tard par erreur.
- `app/src/main/res/values/strings.xml` : 8 nouvelles chaînes (titres/messages/boutons de rationale), en français, cohérent avec le reste de l'UI qui n'a qu'une seule locale.
- Voir ADR-0007 dans `docs/REGISTRE_DECISIONS.md` pour la décision complète, y compris la limite assumée (pas de logique anti-répétition, non testé sur appareil réel).

### Vérification

Aucun `adb` ni émulateur disponible dans cet environnement (`adb devices` / `emulator -list-avds` → introuvables) : la vérification s'est donc limitée à la compilation, aux tests unitaires existants et au lint — **pas** à une exécution réelle du dialogue sur appareil. C'est explicitement noté comme limite dans ADR-0007 et `docs/FONCTIONS_REELLES.md`, pour qu'un futur agent avec accès à un appareil sache que ce point reste à confirmer visuellement/à l'oreille (TalkBack).

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'; .\gradlew.bat test lintDebug
```

Résultat : `BUILD SUCCESSFUL`, mêmes 19 suites/tests qu'à l'étape précédente (aucun test ne couvre `MainActivity`, qui n'a pas d'infrastructure Robolectric/Espresso), et lint sans nouvelle règle déclenchée par les fichiers modifiés (vérifié en cherchant `MainActivity`/`permission_rationale`/`permission_dialog` dans `app/build/reports/lint-results-debug.html` : aucune occurrence).

## Statistiques finales vérifiées

| Indicateur | Valeur |
|---|---:|
| Diff sur `PLAN_ACTION_VOXIA.md` | 0 ligne |
| Fichiers Kotlin créés | 2 (`ConfirmationParser.kt`, `FrameQualityAnalyzer.kt`) |
| Fichiers de test Kotlin créés | 2 (`ConfirmationParserTest.kt`, `FrameQualityAnalyzerTest.kt`) |
| Fichiers Kotlin modifiés | 3 (`VoiceAssistantService.kt`, `OCRModule.kt`, `MainActivity.kt`) |
| Fichiers de ressources modifiés | 1 (`res/values/strings.xml`, +8 chaînes) |
| Fichiers de documentation modifiés | 4 (`FONCTIONS_REELLES.md`, `REGISTRE_RISQUES.md`, `REGISTRE_DECISIONS.md`, `brain/README.md`) |
| Nouveaux tests unitaires ajoutés | 7 (3 + 4) |
| Tests uniques totaux après session | 19 |
| Exécutions totales (debug + release) | 38 |
| Échecs/erreurs de test | 0 |
| Bug réel détecté et corrigé par un test avant commit | 1 (normalisation de « d'accord ») |
| Risques du registre passés de `Ouvert` à `En réduction` | 2 (R-004, R-005) |
| Item P0 backlog fermé (« expliquer permissions ») | 1 (voir ADR-0007) |
| Nouvelles ADR ajoutées | 3 (ADR-0005, ADR-0006, ADR-0007) |
| Guidage caméra temps réel livré | Non — explicitement hors périmètre, voir ci-dessus |
| Vérification sur appareil réel (adb/émulateur) | Non disponible dans cet environnement — compilation/lint/tests uniquement |
| Précision produit/IA nouvellement mesurée | Non mesurée, par choix professionnel (identique à Codex) |

Comme pour la section Codex ci-dessus, ces chiffres décrivent une couverture de tâches et des tests passés, pas une métrique utilisateur. Aucun chiffre de précision IA n'est ajouté à la communication produit par cette session.

## Suite immédiate (même session) — Vérification réelle sur émulateur Android

À la demande explicite de l'utilisateur de « mettre en place la suite » et de tester librement, un SDK Android et un AVD (`Pixel_7_Pro`, Android 17/API 37, caméra arrière `virtualscene`, `google_apis_playstore_ps16k`) déjà présents sur la machine (`C:\Users\HP\AppData\Local\Android\Sdk`) ont été utilisés pour vérifier, pour la première fois sur un appareil réellement exécuté, les changements des deux itérations précédentes. Aucun téléchargement n'a été nécessaire — SDK, AVD, `adb` et `emulator` étaient tous déjà installés.

### Déroulé

1. `emulator -avd Pixel_7_Pro` lancé en arrière-plan, boot confirmé par `adb wait-for-device` + `getprop sys.boot_completed`.
2. `gradlew assembleDebug` en parallèle du boot ; installation via `adb install -r` (un ancien build signé différemment a nécessité un `adb uninstall` préalable — sans conséquence, appareil de test jetable).
3. Difficulté rencontrée et résolue : `adb shell input tap` utilise les pixels **physiques** de l'écran, alors que `screencap`/`uiautomator dump` rapportaient une résolution **logicielle** différente (`wm size` avait un override 1080×1920 sur un panneau physique 1440×3120, ratios non uniformes). Premier tap sur le bouton « Continuer » du dialogue a donc raté sa cible. Corrigé par `adb shell wm size reset`, puis relecture des coordonnées via `uiautomator dump` (bounds en pixels physiques) avant chaque tap.
4. Scénario exécuté intégralement à la main via `adb shell input tap` + captures d'écran (`screencap`) lues et vérifiées visuellement à chaque étape :
   - premier lancement → dialogue VOXIA « Autoriser le microphone » (ADR-0007) affiché **avant** le dialogue système → « Continuer » → dialogue système Android « Allow VOXIA to record audio? » → accordé → service démarré, UI en état « Prêt », aucun crash ;
   - bouton « Lire texte » sans permission caméra → dialogue VOXIA « Autoriser la caméra » → « Continuer » → dialogue système « Allow VOXIA to take pictures and record video? » → accordé → capture automatique déclenchée ;
   - capture réelle sur la caméra virtuelle (scène de salon bien éclairée et texturée) → `FrameQualityAnalyzer` a classé l'image `Acceptable` (aucun rejet à tort) → ML Kit OCR exécuté → aucun texte trouvé (cohérent, la scène 3D n'a pas de texte) → réponse orale « Aucun texte détecté dans l'image. » affichée et lue, comportement exactement conforme au design ;
   - bouton « Annuler » → retour propre à l'état « Prêt », aucune exception.
5. `adb logcat -d` scanné sur toute la session pour `FATAL EXCEPTION`, `AndroidRuntime`, exceptions du package et ANR : **aucune occurrence**.

### Ce que ça confirme, et ce que ça ne confirme pas

Confirmé empiriquement pour la première fois (pas seulement par compilation) : ADR-0007 (rationale avant permission, RECORD_AUDIO et CAMERA) et la branche `Acceptable` d'ADR-0006 (`FrameQualityAnalyzer` ne rejette pas à tort une bonne image, le pipeline caméra → qualité → ML Kit → TTS fonctionne de bout en bout sans crash).

Non confirmé, faute de moyen de simuler une entrée micro dans cet environnement : le flux de confirmation orale (ADR-0005, `ConfirmationParser`) pour `CALL_CONTACT`/`SET_ALARM`/`SET_REMINDER`/`OPEN_APP`, qui exige une vraie transcription vocale (`SpeechRecognizer` écoute le micro réel, pas d'entrée texte de secours dans le build actuel) ; le rationale READ_CONTACTS (déclenché uniquement par la voix) ; les branches `TooDark`/`TooBright`/`TooBlurry` de `FrameQualityAnalyzer` en conditions réelles (seule la scène par défaut, bien exposée, a été capturée).

### Environnement laissé pour la suite

L'émulateur a été arrêté proprement en fin de session pour ne pas laisser de processus ni de RAM occupés inutilement sur la machine de l'utilisateur. Tout est prêt pour une reprise en moins d'une minute :

```powershell
& "C:\Users\HP\AppData\Local\Android\Sdk\emulator\emulator.exe" -avd Pixel_7_Pro
```

Un futur agent avec accès à cet émulateur peut : (1) simuler une entrée micro en poussant un fichier audio via la commande console de l'émulateur ou le micro virtuel, pour enfin exercer le flux de confirmation de bout en bout ; (2) modifier temporairement la scène `virtualscene` (déplacement de caméra via drag dans la preview, ou `adb emu` ) pour obtenir une image sombre/floue et vérifier réellement les branches de rejet de `FrameQualityAnalyzer` ; (3) construire un émulateur avec micro hôte routé pour un test vocal complet.

### Statistiques de vérification ajoutées

| Indicateur | Valeur |
|---|---:|
| Environnement de test | AVD `Pixel_7_Pro`, Android 17 (API 37), déjà installé — aucun téléchargement |
| Installation nécessitant intervention utilisateur | 0 — tout était déjà en place |
| Scénarios UI vérifiés visuellement (captures d'écran) | 5 (premier lancement, dialogue audio système, état prêt après octroi, dialogue caméra + capture OCR réelle, annulation) |
| Crashs/exceptions détectés sur `logcat` complet | 0 |
| ADR confirmées empiriquement pour la première fois | 2 (ADR-0006 branche `Acceptable`, ADR-0007 RECORD_AUDIO + CAMERA) |
| Flux non vérifiable dans cet environnement | Confirmation vocale (ADR-0005) — nécessite une entrée micro réelle ou simulée, absente ici |
| Bug d'outillage trouvé et corrigé pendant la vérification | 1 (mismatch résolution physique/logicielle faussant les coordonnées de `input tap`, corrigé par `wm size reset`) |

---

# CODEX — Reprise Phase 1 ciblée, annulation globale P0

Date : 2026-07-21  
Agent : Codex  
Portée : poursuite de `PLAN_ACTION_VOXIA.md` sans modifier le plan directeur ni reprendre les changements déjà faits.  
Contrainte utilisateur : continuer vers les prochaines étapes du plan, corriger seulement les problèmes réellement graves, et consigner l'activité dans ce fichier.

## Point de départ

Après analyse des modifications existantes et des fichiers Markdown, l'état général est cohérent : VOXIA est bien repositionné comme alpha interne, les promesses non prouvées sont retirées, les harnais d'évaluation existent, et les premiers prototypes Phase 1 (confirmation orale, filtre qualité OCR, explication des permissions) sont documentés.

Le prochain point P0 réellement actionnable sans données terrain était : **corriger l'annulation globale et les états incohérents** (`PLAN_ACTION_VOXIA.md` §10). Avant cette reprise, `cancelCurrentAction()` annulait principalement l'écoute vocale et les confirmations en attente, mais ne libérait pas explicitement les modules caméra/OCR/traduction déjà actifs. Un callback CameraX/ML Kit arrivé après annulation pouvait donc encore parler ou publier un résultat tardif.

## Objectif traité

- Rendre `Annuler` plus robuste pour les parcours Vision/OCR/traduction.
- Éviter qu'un résultat asynchrone obsolète soit parlé après annulation.
- Purger les actions différées : confirmation, permission en attente, nom de contact attendu.
- Garder le changement minimal, sans introduire encore la machine à états complète prévue par l'architecture cible.

## Changements principaux

- `VoiceAssistantService.kt` :
  - ajout d'un `activeActionToken` incrémenté à chaque nouvelle action caméra/OCR/traduction ;
  - ajout de `beginActiveAction()`, `invalidateActiveAction()` et `isActiveAction(token)` pour ignorer les callbacks tardifs ;
  - `captureAndIdentify`, `scanProduct`, `captureAndRead` et `translateVisibleText` vérifient désormais que leur callback correspond encore à l'action active avant de parler ;
  - `cancelCurrentAction()` purge confirmation, permission différée, contact attendu, invalide l'action active, libère Vision/OCR/traduction et remet l'état UI à `IDLE` ;
  - `stopAll()` applique la même purge avant de prononcer l'arrêt et d'appeler `stopSelf()` ;
  - `onDestroy()` utilise la même libération centralisée pour éviter des modules actifs résiduels.

## Limites assumées

- Ce n'est toujours pas la machine à états unique (`Idle`, `Listening`, `Processing`, `NeedsInput`, `NeedsConfirmation`, etc.) prévue par `PLAN_ACTION_VOXIA.md` §6.
- CameraX/ML Kit ne sont pas annulés au niveau bas-niveau avec une coroutine structurée ; le changement empêche surtout les effets utilisateur tardifs et libère les modules détenus par le service.
- Le flux de confirmation orale reste non vérifié de bout en bout sans entrée micro simulée.
- `POST_NOTIFICATIONS` reste une permission à harmoniser avec `showPermissionRationale`, mais ce point n'a pas été traité dans ce pas pour rester concentré sur l'annulation P0.

## Vérification

Une première tentative sandboxée de :

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'; .\gradlew.bat test
```

a échoué à cause d'un accès refusé au SDK Android local :

```text
C:\Users\HP\AppData\Local\Android\Sdk\platforms\android-34\package.xml (Accès refusé)
```

Cette erreur est cohérente avec les vérifications précédentes : le projet a besoin d'accéder au SDK Android local hors sandbox pour compiler/tester.

Relance hors sandbox autorisée ensuite :

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'; .\gradlew.bat test
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'; .\gradlew.bat lintDebug
```

Résultats :

- `gradlew test` : succès (`exit code 0`).
- Rapports JUnit lus après exécution : 5 suites uniques (`IntentClassifierEngineTest`, `ArithmeticEvaluatorTest`, `ConfirmationParserTest`, `TextNormalizerTest`, `FrameQualityAnalyzerTest`), exécutées sur debug et release, soit 38 exécutions de test, 0 échec, 0 erreur, 0 ignoré.
- `gradlew lintDebug` : `BUILD SUCCESSFUL`, rapport HTML généré dans `app/build/reports/lint-results-debug.html`.

## Reprise recommandée après ce pas

1. Vérifier sur émulateur ou téléphone que le bouton **Annuler** pendant OCR/traduction ne lit plus de résultat tardif.
2. Harmoniser `POST_NOTIFICATIONS` avec `showPermissionRationale`, car ADR-0007 dit que toute nouvelle permission doit être expliquée avant le dialogue système.
3. Continuer vers les prochaines étapes Phase 1 du plan : données consenties, premiers vrais rapports Intent/STT/OCR, puis décision écrite sur zone pilote et parcours V1.

---

# CODEX — Reprise Phase 1 ciblée, permissions cohérentes

Date : 2026-07-21  
Agent : Codex  
Portée : poursuite de `PLAN_ACTION_VOXIA.md` avec discipline de clean architecture locale, sans réécrire les décisions précédentes.  

## Où nous sommes dans `PLAN_ACTION_VOXIA.md`

- **Phase 0 — Assainissement et vérité produit** : largement traitée côté code/documentation. Les affirmations non prouvées ont été retirées, la version source est alpha interne, les logs sensibles ont été réduits, les registres existent, et les harnais d'évaluation sont versionnés.
- **Phase 1 — Recherche utilisateur et harnais d'évaluation** : en cours. La partie harnais technique existe ; la partie terrain (participants, données consenties, baselines réelles Intent/STT/OCR, zone pilote et trois parcours validés) n'est pas encore réalisable sans collecte utilisateur.
- **Backlog P0** : les items techniques locaux sont presque fermés. Restent hors code local : tester avec des utilisateurs cibles et produire de vrais rapports sur données consenties.
- **Phase 2** : pas encore démarrée. Les changements actuels préparent la sûreté et la traçabilité avant la reconstruction UI/état complète.

## Objectif traité

Fermer l'écart laissé après ADR-0007 : `POST_NOTIFICATIONS` était encore demandé directement au premier lancement sur Android 13+, sans passer par `showPermissionRationale`, alors que la décision dit que toute nouvelle permission doit être expliquée avant la demande système.

## Changements principaux

- `MainActivity.kt` :
  - ajout de `requestNotificationPermissionIfNeeded()` ;
  - `POST_NOTIFICATIONS` passe désormais par le même mécanisme explicatif que RECORD_AUDIO, CAMERA et READ_CONTACTS ;
  - le refus utilisateur affiche un message UI non bloquant au lieu de rester silencieux.
- `strings.xml` :
  - ajout des textes d'explication et de refus pour les notifications.
- `docs/REGISTRE_DECISIONS.md` :
  - ADR-0007 étendue pour inclure POST_NOTIFICATIONS ;
  - limite assumée : compilation/tests/lint validés, mais pas encore de vérification visuelle sur émulateur après cette extension.
- `docs/FONCTIONS_REELLES.md` :
  - inventaire mis à jour avec POST_NOTIFICATIONS et date de reprise 2026-07-21.

## Position senior/dev lead

La priorité reste de ne pas ajouter de nouvelle promesse fonctionnelle avant mesure. Les prochains travaux doivent suivre cet ordre :

1. terminer la vérification appareil des P0 déjà codés ;
2. produire un premier jeu pilote consenti pour Intent/STT/OCR ;
3. produire des rapports non-template ;
4. seulement ensuite démarrer les P1 lourds : guidage caméra temps réel, UI/état propre, OCR contrôlable et comparatif STT.

Avancer directement vers une grande refonte Compose ou un nouveau modèle ML maintenant serait prématuré : le plan demande d'abord vérité, sûreté, mesure et terrain.

## Vérification

Commandes exécutées hors sandbox, avec `JAVA_HOME` pointé sur le JBR Android Studio :

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'; .\gradlew.bat test
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'; .\gradlew.bat lintDebug
```

Résultats :

- `gradlew test` : `BUILD SUCCESSFUL`.
- Rapports JUnit : 5 suites uniques, exécutées sur debug et release, soit 38 exécutions de test, 0 échec, 0 erreur, 0 ignoré.
- `gradlew lintDebug` : `BUILD SUCCESSFUL`, rapport HTML généré dans `app/build/reports/lint-results-debug.html`.
- `git diff --check` : aucune erreur de whitespace ; uniquement les avertissements Git LF → CRLF déjà présents dans le dépôt.

## Limite assumée

Le dialogue explicatif `POST_NOTIFICATIONS` n'a pas encore été vérifié visuellement sur émulateur/téléphone après cette modification. Il compile et passe lint, mais il reste à confirmer en exécution réelle Android 13+.

---

# CODEX — Reprise qualité senior, cohérence permissions/CI/docs

Date : 2026-07-21
Agent : Codex
Portée : vérification du travail déjà réalisé, correction d'un écart permission, renforcement CI et remise en cohérence des fichiers Markdown de suivi. `PLAN_ACTION_VOXIA.md` reste la stratégie coeur et ne doit pas être modifié pour suivre l'avancement opérationnel.

## Constats vérifiés

- Le dépôt Git était propre au début de la reprise.
- Les README et guides ne contiennent plus les anciennes promesses non prouvées (`71,25 %`, 400 exemples, Vosk/YOLO/TFLite livré).
- `PLAN_ACTION_VOXIA.md` reste le document directeur. L'avancement opérationnel doit être tracé ici (`CODEX_EVOLUTION_VOXIA.md`) et dans les registres, pas en modifiant les cases du plan.
- `MainActivity` demandait `POST_NOTIFICATIONS` au démarrage en plus du microphone, ce qui pouvait empiler deux rationales. Le callback caméra relançait aussi systématiquement `retryPendingPermissionAction()`, même quand la permission venait d'une action UI locale.

## Changements principaux

- `MainActivity.kt` :
  - `POST_NOTIFICATIONS` est maintenant demandé seulement après disponibilité de `RECORD_AUDIO`, avec une seule tentative par session d'activité ;
  - le callback caméra distingue une action UI (`pendingCameraAction`) d'une action vocale différée côté service ;
  - les refus caméra/contacts purgent l'action vocale différée et affichent un message utilisateur explicite.
- `VoiceAssistantService.kt` :
  - ajout de `clearPendingPermissionAction()` pour supprimer une action différée après refus de permission.
- `.github/workflows/build.yml` :
  - la CI exécute désormais `./gradlew test`, `./gradlew lintDebug`, puis `./gradlew assembleDebug`.
- Documentation :
  - `PLAN_ACTION_VOXIA.md` a été restauré comme référence non modifiée après clarification utilisateur ;
  - `docs/REGISTRE_DECISIONS.md` ajoute ADR-0008 ;
  - `docs/REGISTRE_RISQUES.md` ajoute R-011 ;
  - `docs/FONCTIONS_REELLES.md` reflète les limites réelles des permissions et tests.

## Vérification

Smoke tests évaluation avec le Python portable Codex :

```powershell
& 'C:\Users\HP\.cache\codex-runtimes\codex-primary-runtime\dependencies\python\python.exe' evaluation\intent\evaluate.py --dataset evaluation\intent\datasets\template.csv --name smoke_intent_template
& 'C:\Users\HP\.cache\codex-runtimes\codex-primary-runtime\dependencies\python\python.exe' evaluation\stt\evaluate.py --manifest evaluation\stt\manifests\template.csv --name smoke_stt_template
& 'C:\Users\HP\.cache\codex-runtimes\codex-primary-runtime\dependencies\python\python.exe' evaluation\ocr\evaluate.py --manifest evaluation\ocr\manifests\template.csv --name smoke_ocr_template
```

Résultat : 3/3 scripts exécutés avec succès.

Gradle hors sandbox, avec `JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'` :

```powershell
.\gradlew.bat test
.\gradlew.bat lintDebug
.\gradlew.bat assembleDebug
```

Résultats :

- `test` : `BUILD SUCCESSFUL` ; 5 suites uniques, debug + release, 38 exécutions, 0 échec, 0 erreur, 0 ignoré.
- `lintDebug` : `BUILD SUCCESSFUL`, rapport généré dans `app/build/reports/lint-results-debug.html`.
- `assembleDebug` : premier passage a empaqueté l'APK mais Windows a signalé un lock Gradle ouvert sur `.gradle/buildOutputCleanup`; après `gradlew --stop`, relance propre en `BUILD SUCCESSFUL`.
- APK debug courant : `app/build/outputs/apk/debug/app-debug.apk`.
- `git diff --check` : aucune erreur de whitespace ; uniquement les avertissements Git LF -> CRLF déjà connus sur Windows.

## Limites restantes

- Pas de test utilisateur cible ni dataset consenti : les mesures réelles Intent/STT/OCR restent impossibles localement.
- Pas de vérification visuelle post-modification pour `POST_NOTIFICATIONS`, `READ_CONTACTS` et les chemins de refus permission.
- Pas de test instrumenté Espresso/Robolectric : les permissions, confirmations vocales et annulations réelles restent validées par compilation/lint et par logique pure existante, pas par un scénario UI automatisé.

---

# CODEX — Reprise P1 ciblée, scan produit sourcé

Date : 2026-07-21
Agent : Codex
Portée : incrément P1 strictement aligné sur `PLAN_ACTION_VOXIA.md` sans modifier ce plan directeur.

## Objectif traité

Améliorer `Scanner produit` sans introduire de promesse non prouvée : lorsqu'un code-barres est détecté, VOXIA interroge un catalogue local sourcé. Si aucune fiche n'existe, il annonce clairement que le produit est inconnu au lieu d'inférer marque, prix, composition ou allergènes.

## Changements principaux

- `app/src/main/kotlin/com/VoxIA/vision/ProductCatalog.kt` :
  - nouveau `ProductCatalog` chargé depuis `assets/product_catalog.tsv` ;
  - parsing TSV pur et testable ;
  - une fiche sans `barcode`, `name_fr`, `source` ou `source_date` est ignorée ;
  - `ProductVoiceFormatter` produit les messages connu/inconnu en FR/EN.
- `app/src/main/assets/product_catalog.tsv` :
  - fichier catalogue local versionné, volontairement vide hors en-tête ;
  - commentaires rappelant qu'aucune fiche non sourcée ne doit être inventée.
- `VisionModule.kt` :
  - en `productMode`, le premier code-barres passe par `ProductVoiceFormatter` ;
  - si le catalogue ne connaît pas le code, la réponse reste utile mais prudente : code détecté, produit inconnu, éventuelle catégorie probable, aucune donnée sensible inventée.
- `app/src/test/kotlin/com/voxia/vision/ProductCatalogTest.kt` :
  - tests sur chargement sourcé, rejet des fiches incomplètes, normalisation du code et messages produit connu/inconnu.
- Documentation :
  - ADR-0009 dans `docs/REGISTRE_DECISIONS.md` ;
  - R-012 dans `docs/REGISTRE_RISQUES.md` ;
  - inventaire réel et guide d'installation mis à jour.

## Vérification

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat test
.\gradlew.bat lintDebug
.\gradlew.bat assembleDebug
```

Résultats :

- `test` : succès ; 6 suites uniques, debug + release, 46 exécutions, 0 échec, 0 erreur, 0 ignoré.
- `lintDebug` : succès.
- `assembleDebug` : succès ; APK debug régénéré.
- `ProductCatalogTest` ajoute 4 tests unitaires dédiés au scan produit sourcé.

## Limites restantes

- Le catalogue local ne contient pas encore de données terrain consenties/sourcées.
- Pas de requête réseau ni cache Open Food Facts dans cet incrément.
- Pas de validation caméra réelle sur un produit physique avec code-barres ; la vérification actuelle couvre la logique de catalogue et le build JVM.
