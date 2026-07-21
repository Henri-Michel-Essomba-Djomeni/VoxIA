# Registre des décisions — VOXIA

## ADR-0001 — Marquer la source comme alpha interne

- Date : 2026-07-16
- Statut : accepté
- Décision : la version source devient `0.1.0-alpha-internal`.
- Motivation : le dépôt contient un prototype fonctionnel mais pas une validation produit ou IA.
- Conséquence : toute communication externe doit éviter “version 1.0 prête”.

## ADR-0002 — Nommer honnêtement le moteur STT courant

- Date : 2026-07-16
- Statut : accepté
- Décision : introduire `AndroidSpeechRecognizerSTTService` et déprécier l'alias `VoskSTTService`.
- Motivation : le code utilise Android `SpeechRecognizer`, pas Vosk.
- Conséquence : les futurs benchmarks STT peuvent comparer Android, Vosk et Whisper sans confusion.

## ADR-0003 — Centraliser la journalisation sensible

- Date : 2026-07-16
- Statut : accepté
- Décision : ajouter `PrivacyLog` et supprimer les logs de commande brute, OCR brut et texte TTS.
- Motivation : transcriptions, OCR et réponses peuvent contenir des données personnelles.
- Conséquence : les logs release ne doivent contenir que des événements techniques sans contenu utilisateur.

## ADR-0004 — Mesurer avant d'annoncer une précision

- Date : 2026-07-16
- Statut : accepté
- Décision : toute métrique publique doit venir de `evaluation/` avec commit, dataset, paramètres et rapport.
- Motivation : les chiffres historiques n'étaient pas traçables.
- Conséquence : les documents ne promettent plus de précision non prouvée.

## ADR-0005 — Confirmation orale obligatoire avant toute action sensible

- Date : 2026-07-16
- Statut : accepté
- Décision : `CALL_CONTACT`, `SET_ALARM`, `SET_REMINDER` et `OPEN_APP` passent par `VoiceAssistantService.requestConfirmation(...)` avant exécution ; la réponse est résolue par `ConfirmationParser` (oui/non, FR/EN), pas par le classifieur d'intentions. Une confirmation en attente est annulée par `stopAll()` ou `cancelCurrentAction()`.
- Motivation : R-004 était ouvert — aucune action sensible n'avait de garde applicative avant cette décision ; seule la confirmation *système* (composeur, appli Horloge) existait, ce qui ne protège pas contre une mauvaise reconnaissance d'intention ou de contact en amont.
- Conséquence : chaque nouvel intent sensible ajouté à `IntentMapper` doit explicitement décider s'il passe par `requestConfirmation` avant exécution. Le mécanisme est volontairement simple (une seule confirmation en attente, portée service) en attendant la machine à états unique de l'architecture cible (§6 du plan directeur) ; ne pas empiler plusieurs confirmations sans revoir ce point.
- Limite assumée : non mesuré en conditions réelles (taux de fausse action, taux d'abandon dû à la confirmation). À couvrir par le pilote Intent de la Phase 1.

## ADR-0006 — Contrôle qualité heuristique avant OCR, pas encore guidage caméra temps réel

- Date : 2026-07-16
- Statut : accepté (prototype)
- Décision : `FrameQualityAnalyzer` calcule la luminosité moyenne et une variance de Laplacien (proxy de netteté) sur l'image capturée ; `OCRModule` rejette l'appel ML Kit et renvoie `OCRResult.PoorQuality` avec une consigne vocale si l'image est trop sombre, trop claire ou floue. Le constant `MIN_CONFIDENCE` de `OCRModule`, déclarée mais jamais utilisée (ML Kit Text Recognition ne fournit pas de score de confiance par bloc dans l'API stable utilisée), a été supprimée plutôt que branchée sur une donnée qui n'existe pas.
- Motivation : R-005 était ouvert — aucun filtre de qualité n'existait avant OCR, donc une capture inexploitable partait quand même en reconnaissance ML Kit et produisait un résultat vide ou incompréhensible sans expliquer pourquoi.
- Conséquence : c'est un filtre **post-capture**, pas le guidage temps réel (`ImageAnalysis` continu + retour audio/haptique avant capture) décrit en §6 et §7.4 du plan directeur — ne pas communiquer ceci comme la fonction de guidage caméra de la Phase 2. Les seuils (`DARK_MEAN_LUMA`, `BRIGHT_MEAN_LUMA`, `BLUR_VARIANCE_FLOOR`) sont des valeurs de départ non calibrées et doivent être révisés dès que `evaluation/ocr/` contient des captures réelles avec vérité terrain.
- Limite assumée : ne couvre ni la perspective/inclinaison, ni le cadrage (document partiellement hors champ), ni les reflets localisés (seule la luminosité globale est mesurée).
- **Vérifié sur émulateur** le 2026-07-16 (AVD `Pixel_7_Pro`, caméra arrière `virtualscene`) : le flux complet `MainActivity` → `captureAndRead()` → `OCRModule` → `FrameQualityAnalyzer` → ML Kit → TTS a été exécuté avec une vraie capture caméra. La scène virtuelle (pièce bien éclairée et texturée) a été correctement classée `Acceptable` (pas de rejet à tort), l'OCR ML Kit a tourné et renvoyé « Aucun texte détecté dans l'image. », restitué à l'oral sans erreur. Cela confirme le branchement réel de bout en bout, mais **seule la branche `Acceptable` a été exercée en conditions réelles** : les branches `TooDark`/`TooBright`/`TooBlurry` restent vérifiées uniquement par `FrameQualityAnalyzerTest` (données synthétiques), pas encore par une vraie capture défavorable.

## ADR-0007 — Expliquer chaque permission avant la demande système

- Date : 2026-07-16
- Statut : accepté
- Décision : `MainActivity.showPermissionRationale(...)` affiche un `AlertDialog` (titre + message + boutons « Continuer »/« Pas maintenant ») avant tout appel à `ActivityResultContracts.RequestPermission().launch(...)`, pour RECORD_AUDIO (premier lancement), CAMERA (bouton UI et déclenchement vocal via `VoiceAssistantService.ensurePermission`), READ_CONTACTS (déclenchement vocal) et POST_NOTIFICATIONS (Android 13+). Un refus (« Pas maintenant ») réinitialise explicitement l'état en attente côté UI (`pendingCameraAction`) pour éviter qu'une action différée ne se déclenche par erreur plus tard.
- Motivation : c'était un item P0 du backlog (`PLAN_ACTION_VOXIA.md` §10) resté non traité après la Phase 0 — avant cette décision, RECORD_AUDIO et CAMERA (chemin bouton UI) déclenchaient le dialogue système Android sans aucune explication VOXIA, ce qui est particulièrement problématique pour un public malvoyant/aveugle qui doit comprendre pourquoi une permission est demandée avant de décider.
- Conséquence : toute nouvelle permission ajoutée à l'application doit passer par `showPermissionRationale` avant sa demande système, dans `MainActivity` comme dans tout futur écran.
- Limite assumée : le dialogue s'affiche à chaque demande tant que la permission n'est pas accordée (pas de logique « ne plus jamais montrer »), ce qui est un choix de transparence assumé plutôt qu'un défaut ; à revoir si le pilote utilisateur remonte de la lassitude.
- **Vérifié sur émulateur** (AVD `Pixel_7_Pro`, Android 17/API 37, `com.voxia.assistant` installé propre) le 2026-07-16 : le dialogue « Autoriser le microphone » s'affiche bien au tout premier lancement, avant le dialogue système ; « Continuer » enchaîne correctement sur le dialogue système Android (« Allow VOXIA to record audio? ») ; même vérification pour la caméra via le bouton « Lire texte » (dialogue « Autoriser la caméra » → dialogue système « Allow VOXIA to take pictures and record video? »). Aucun crash, `logcat` sans exception sur toute la session de test. Le chemin READ_CONTACTS (déclenché uniquement par la voix, pas de bouton dédié) partage le même code (`showPermissionRationale`) mais n'a pas été testé visuellement faute d'entrée micro simulable dans cet environnement. POST_NOTIFICATIONS partage désormais le même mécanisme depuis la reprise Codex du 2026-07-21 ; compilation/tests/lint validés, mais le dialogue n'a pas encore été revérifié visuellement sur émulateur après cette extension.

## ADR-0008 — Séquencer les permissions et purger les actions différées refusées

- Date : 2026-07-21
- Statut : accepté
- Décision : `MainActivity` ne demande plus `POST_NOTIFICATIONS` en parallèle du microphone au premier lancement ; la demande notification est déclenchée seulement après disponibilité de `RECORD_AUDIO` et limitée à une tentative par session d'activité. Le callback caméra distingue maintenant explicitement une action lancée par bouton UI d'une action différée par `VoiceAssistantService.ensurePermission(...)`. En cas de refus caméra ou contacts, `VoiceAssistantService.clearPendingPermissionAction()` purge l'action vocale en attente.
- Motivation : l'implémentation précédente pouvait empiler deux dialogues au démarrage et, dans certains chemins, conserver une action vocale différée après refus de permission. Pour une application vocale accessible, l'ordre des demandes et l'absence d'action fantôme sont aussi importants que la permission elle-même.
- Conséquence : les demandes de permissions restent explicites, mais elles sont plus prévisibles. Toute future permission déclenchée depuis le service doit prévoir à la fois un chemin `retryPendingPermissionAction()` et un chemin de purge en cas de refus.
- Vérification : `gradlew test`, `gradlew lintDebug`, `gradlew assembleDebug` et les trois smoke tests `evaluation/` réussissent le 2026-07-21. Vérification visuelle appareil encore à refaire pour `POST_NOTIFICATIONS`, `READ_CONTACTS` et les refus système.

## ADR-0009 — Scanner produit sans inventer les données catalogue

- Date : 2026-07-21
- Statut : accepté (incrément P1)
- Décision : `scanProduct()` utilise désormais `ProductCatalog`, chargé depuis `app/src/main/assets/product_catalog.tsv`, lorsque ML Kit détecte un code-barres. Le fichier catalogue exige au minimum `barcode`, `name_fr`, `source` et `source_date` pour accepter une fiche. Si le code n'existe pas dans le catalogue local, VOXIA annonce explicitement que le produit est inconnu et peut seulement fournir une catégorie probable issue de l'image/OCR.
- Motivation : le plan directeur demande une recherche produit par code-barres avec source et date, et interdit d'inventer prix, allergènes ou composition. L'ancien comportement répétait le code détecté sans distinguer produit connu et inconnu.
- Conséquence : VOXIA peut être branché sur un catalogue local ou terrain plus tard sans changer le pipeline caméra. Tant qu'aucun catalogue validé n'est fourni, l'application reste honnête : elle reconnaît le code, mais ne prétend pas connaître le produit.
- Vérification : `ProductCatalogTest` couvre le parsing TSV, le rejet des fiches sans source/date, la normalisation du code et les messages produit connu/inconnu.

## ADR-0010 — Préparer la lecture OCR contrôlable par segments

- Date : 2026-07-21
- Statut : accepté (incrément P1)
- Décision : `OCRResult.Success` expose désormais des segments de lecture construits par `DocumentTextSegmenter`. `VoiceAssistantService` conserve une `DocumentReadingSession` après OCR réussi, lit le premier segment avec position, et route les nouvelles intentions `READ_NEXT_SEGMENT` / `READ_PREVIOUS_SEGMENT` pour avancer ou revenir. `repeatLastResponse()` répète le segment courant lorsqu'une session existe.
- Motivation : le plan directeur demande une lecture OCR segmentée et contrôlable. L'ancien comportement envoyait tout le texte reconnu au TTS, ce qui rendait les documents longs difficiles à corriger, répéter ou interrompre.
- Conséquence : la base métier de navigation de lecture existe sans refonte UI. Les commandes vocales "lis la suite", "segment suivant", "segment précédent" et équivalents anglais sont reconnues par les règles locales.
- Limite assumée : ce n'est pas encore une expérience complète avec surlignage, pause/reprise TTS native ou persistance d'historique. Ces éléments restent liés à la reconstruction UI/état de Phase 2.
- Vérification : `DocumentReadingSessionTest` couvre navigation et segmentation ; `IntentClassifierEngineTest` couvre les nouvelles intentions.

## ADR-0011 — Rendre les zones dynamiques plus lisibles par TalkBack

- Date : 2026-07-21
- Statut : accepté (incrément P1)
- Décision : `MainActivity` centralise les mises à jour de transcription et réponse dans `updateTranscript()` / `updateResponse()`, qui synchronisent le texte affiché et la `contentDescription`. Les boutons principaux passent d'une hauteur fixe stricte à `wrap_content` avec `minHeight` et padding vertical pour mieux supporter les grandes tailles de police.
- Motivation : le plan directeur cible TalkBack et texte jusqu'à 200 %. Les zones dynamiques étaient visuellement mises à jour, mais leur libellé accessible n'indiquait pas clairement "transcription" ou "réponse VOXIA", et plusieurs boutons risquaient de couper le texte à grande taille.
- Conséquence : le comportement est plus robuste pour TalkBack et grandes polices sans attendre la refonte Compose. Toute future zone dynamique doit suivre le même modèle : texte visible et description accessible mis à jour ensemble.
- Limite assumée : non vérifié sur appareil réel avec TalkBack, Switch Access, paysage et police 200 %. Cela reste une réduction de risque, pas une certification accessibilité.

## ADR-0012 — Exposer les contrôles OCR et l'export texte comme actions explicites

- Date : 2026-07-21
- Statut : accepté (incrément P1)
- Décision : l'écran principal ajoute des boutons `Précédent`, `Répéter`, `Suite`, `Copier` et `Partager` pour le dernier texte OCR. Les boutons `Copier` et `Partager` affichent un avertissement confidentialité avant d'appeler le service. Les commandes vocales `COPY_READING_TEXT` et `SHARE_READING_TEXT` complètent les boutons et demandent une confirmation orale avant export. Le partage utilise le chooser Android ; la copie utilise le presse-papiers système.
- Motivation : le plan directeur demande lecture segmentée avec répétition, copie et partage. Les commandes vocales seules ne suffisent pas pour un parcours accessible : les actions doivent aussi être visibles et activables à l'écran.
- Conséquence : aucune donnée OCR n'est envoyée automatiquement. L'utilisateur déclenche explicitement copie ou partage, puis confirme à l'oral lorsque l'action vient d'une commande vocale ; VOXIA annonce lorsqu'aucun texte OCR récent n'est disponible.
- Limite assumée : le presse-papiers Android peut être lu par d'autres surfaces système selon version et contexte. Pour une bêta, il faudra évaluer une option de nettoyage automatique du presse-papiers.

## ADR-0013 — Ajouter un réglage borné de vitesse TTS

- Date : 2026-07-21
- Statut : accepté (incrément P1)
- Décision : `TTSService` conserve un multiplicateur global de vitesse borné entre 70 % et 140 %. `SpeechManager` expose les commandes augmenter, diminuer et réinitialiser ; `VoiceAssistantService`, `IntentMapper` et l'écran principal les rendent accessibles par voix et boutons.
- Motivation : le plan directeur demande une lecture OCR contrôlable avec vitesse réglable. Les segments précédent/répéter/suite existaient déjà ; la vitesse restait une limite fonctionnelle directe.
- Conséquence : les lectures suivantes utilisent la vitesse choisie, y compris les réponses VOXIA. Le réglage reste local et ne modifie pas le volume système.
- Limite assumée : Android `TextToSpeech` ne fournit pas une vraie pause/reprise native du flux courant. `Annuler` stoppe toujours la voix ; pause/reprise fine reste à concevoir dans la refonte lecture.

## ADR-0014 — Produire un AAB release dans la CI

- Date : 2026-07-21
- Statut : accepté (incrément P1)
- Décision : le workflow GitHub Actions construit désormais `bundleRelease` en plus de `test`, `lintDebug` et `assembleDebug`, puis publie `app/build/outputs/bundle/release/app-release.aab` comme artefact `voxia-release-aab`.
- Motivation : R-007 signale le risque d'un APK trop lourd pour le terrain. L'AAB est le format de distribution Android adapté aux splits et à une livraison plus fine que l'APK debug monolithique.
- Conséquence : chaque push/PR vérifie que la source courante peut produire un AAB release. Le dépôt garde aussi l'APK debug pour les contrôles techniques rapides.
- Limite assumée : cette décision ne mesure pas encore la taille réelle livrée par appareil et n'introduit pas de modules dynamiques ou modèles à la demande.

## ADR-0015 — Abstention explicite pour objets financiers

- Date : 2026-07-21
- Statut : accepté (incrément P1)
- Décision : `VisionModule` appelle `FinancialSafety` avant de formater une description générique. Si les labels ou le texte visible évoquent argent, monnaie, coupure, CFA/XAF/XOF, BEAC/BCEAO ou équivalent anglais, VOXIA annonce qu'il ne peut ni identifier la valeur ni vérifier l'authenticité.
- Motivation : le plan directeur interdit l'authentification de monnaie et exige abstention, seuil strict et absence de promesse financière sans protocole spécialisé.
- Conséquence : une détection générique de type `money`, `banknote`, `bill` ou `coin` ne devient plus une réponse de reconnaissance financière. Le libellé `ticket` reste un ticket, pas un billet bancaire.
- Limite assumée : ce n'est pas un classifieur de billets et cela ne remplace pas une future décision go/no-go sur un domaine spécialisé. La règle réduit le risque de mauvaise promesse, pas le risque de non-détection.
