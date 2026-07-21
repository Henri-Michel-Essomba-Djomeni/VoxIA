# Inventaire des fonctions réelles — VOXIA alpha interne

Date : 2026-07-16 (assainissement Phase 0, Codex) — mises à jour 2026-07-16 (Phase 1, Claude) et 2026-07-21 (reprises Phase 1, Codex)  
Portée : état du dépôt au moment de l'assainissement Phase 0, puis premiers incréments Phase 1.

## Fonctions présentes

| Domaine | Fonction | Implémentation | Limite principale |
|---|---|---|---|
| Voix | Écoute ponctuelle | Android `SpeechRecognizer` | Dépend du moteur installé et peut utiliser le réseau |
| Voix | Synthèse vocale | Android `TextToSpeech` | Qualité variable selon téléphone et packs vocaux |
| Intentions | Classification | Règles Kotlin dans `IntentClassifierEngine` | Couverture limitée des formulations naturelles |
| Vision | Étiquetage générique | ML Kit Image Labeling | Non spécialisé VOXIA |
| Vision | OCR latin | ML Kit Text Recognition | Sensible au cadrage, flou, lumière et reflets |
| Vision | Codes-barres | ML Kit Barcode Scanning | Ne fournit pas encore un catalogue produit complet |
| Traduction | Texte visible | ML Kit Language ID + Translate | Modèles possiblement téléchargés au premier usage |
| Actions | Appel | `Intent.ACTION_DIAL` + confirmation orale VOXIA | L'utilisateur confirme deux fois : à VOXIA puis dans le composeur |
| Actions | Ouverture application | Launcher Android + confirmation orale VOXIA | Recherche approximative par nom |
| Actions | Alarmes/minuteurs | `AlarmClock` + confirmation orale VOXIA | Délègue la saisie finale à l'application système |
| Actions | Notifications | `NotificationListenerService` | Autorisation manuelle obligatoire |
| Actions | Volume/date/heure/batterie | API Android | Fonctions utilitaires simples, jugées non sensibles donc sans confirmation |
| Permissions | Explication avant demande système | `MainActivity.showPermissionRationale` (RECORD_AUDIO, CAMERA, READ_CONTACTS, POST_NOTIFICATIONS Android 13+) | RECORD_AUDIO et CAMERA vérifiés sur émulateur (voir ADR-0007) ; POST_NOTIFICATIONS est séquencé après le microphone et limité à une demande par session ; READ_CONTACTS, POST_NOTIFICATIONS et chemins de refus restent à vérifier visuellement |
| Permissions | Purge des actions différées refusées | `VoiceAssistantService.clearPendingPermissionAction()` appelé par les refus caméra/contacts | Protège contre une action vocale ancienne relancée après refus ; vérifié par compilation/lint, pas encore par test UI instrumenté |

## Fonctions partiellement implémentées (prototypes non calibrés, Phase 1)

Ces fonctions existent en code et sont couvertes par des tests unitaires, mais reposent sur des seuils heuristiques choisis sans données réelles. Elles ne doivent pas être présentées comme validées.

| Domaine | Fonction | Implémentation | Ce qui manque encore |
|---|---|---|---|
| Actions | Confirmation avant action sensible | `VoiceAssistantService.requestConfirmation` + `ConfirmationParser` (`app/src/main/kotlin/com/VoxIA/utils/ConfirmationParser.kt`) gate `CALL_CONTACT`, `SET_ALARM`, `SET_REMINDER`, `OPEN_APP` | Une seule confirmation en attente à la fois (pas de pile) ; pas encore remonté au niveau `IntentMapper`/machine à états unique visée par l'architecture cible ; pas mesuré en conditions réelles (taux de fausse action) |
| Vision/OCR | Contrôle qualité pré-capture | `FrameQualityAnalyzer` (`app/src/main/kotlin/com/VoxIA/vision/FrameQualityAnalyzer.kt`) rejette les images trop sombres, trop claires ou floues avant d'appeler ML Kit OCR, avec consigne vocale (`OCRResult.PoorQuality`) ; branché et vérifié en conditions réelles sur émulateur pour la branche `Acceptable` (voir ADR-0006) | Ce n'est PAS le guidage caméra temps réel du plan directeur (§7.4/Phase 2) : contrôle fait après la capture, pas de flux `ImageAnalysis` continu, pas de détection de cadrage/perspective/reflet localisé, seuils non calibrés sur le pilote OCR ; les branches de rejet (`TooDark`/`TooBright`/`TooBlurry`) ne sont vérifiées que par tests unitaires synthétiques, pas encore sur une vraie capture défavorable |

## Fonctions absentes ou non validées

- Modèle Vosk embarqué.
- Modèle Whisper embarqué.
- Modèle YOLO spécialisé VOXIA embarqué.
- `intent_classifier.tflite` livré dans l'application.
- Reconnaissance fiable de billets, médicaments ou produits locaux.
- Mesure WER/STT sur accents et bruit.
- Mesure CER/WER OCR sur captures réelles.
- Mesure macro-F1 intents sur jeu de test gelé.
- Guidage caméra temps réel avec capture automatique (seul un contrôle qualité post-capture existe, voir ci-dessus).
- Validation TalkBack complète.
- Crash reporting respectueux de la vie privée.
- Tests instrumentés Android couvrant permissions, confirmations et annulation réelle sur appareil.

## Règle de mise à jour

Chaque nouvelle fonction doit indiquer : état réel, dépendance technique, données nécessaires, risque utilisateur, test minimal et métrique de sortie.
