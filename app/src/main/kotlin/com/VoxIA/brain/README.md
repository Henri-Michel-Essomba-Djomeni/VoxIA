# Module Brain — VOXIA alpha interne

## Rôle

Le module Brain reçoit le texte fourni par le moteur vocal Android, estime une intention avec des règles locales, extrait quelques paramètres simples, puis appelle le contexte applicatif via `VoxiaContext`.

## État actuel vérifié

- `IntentClassifierEngine` utilise des règles déclarées en Kotlin.
- `Intent` expose 33 intentions métier plus `FALLBACK`.
- Aucun `intent_classifier.tflite` n'est livré dans `app/src/main/assets`.
- Le fichier `intent_vocab.json` existe mais n'est pas utilisé par le pipeline courant.
- La confiance retournée par `PredictionResult.confidence` est une heuristique de score relatif, pas une probabilité statistique.
- Le STT appelé par l'application est Android `SpeechRecognizer`, via `AndroidSpeechRecognizerSTTService`.

## Fichiers

- `IntentClassifier.kt` : types `Intent`, `Language` et `PredictionResult`.
- `IntentClassifierEngine.kt` : règles, scoring et extraction simple de slots.
- `IntentMapper.kt` : routage de l'intention vers `VoxiaContext`.
- `VoxiaContext.kt` : contrat entre Brain, voix, vision et actions Android.
- `VoxiaResponses.kt` : réponses vocales FR/EN.

## Seuil d'exécution

`IntentMapper` demande une clarification lorsque `confidence < 0.70f`. Ce seuil protège seulement contre les scores faibles du moteur de règles actuel ; il devra être recalibré sur un jeu de test gelé avant toute métrique publique.

## Connexions actuelles

| Intention | Module appelé | État |
|---|---|---|
| `IDENTIFY_OBJECT` | ML Kit Image Labeling + OCR + codes-barres | Générique |
| `SCAN_PRODUCT` | Code-barres/OCR/catégorie probable/catalogue local sourcé | Sans catalogue produit distant |
| `READ_DOCUMENT` | ML Kit OCR latin + session segmentée | Capture unique, navigation segment suivant/précédent |
| `READ_NEXT_SEGMENT`, `READ_PREVIOUS_SEGMENT` | `DocumentReadingSession` | Disponible après une lecture OCR réussie |
| `READING_SPEED_UP`, `READING_SPEED_DOWN`, `READING_SPEED_NORMAL` | TTS Android via `SpeechManager` | Vitesse bornée entre 70 % et 140 %, applicable aux lectures suivantes |
| `COPY_READING_TEXT`, `SHARE_READING_TEXT` | Presse-papiers Android / chooser Android | Confirmation orale VOXIA avant export vocal ; aucun envoi silencieux |
| `TRANSLATE_TEXT` | ML Kit OCR + Translate | Modèles de traduction téléchargeables |
| `CALL_CONTACT` | `Intent.ACTION_DIAL` | Confirmation orale VOXIA puis composeur, pas d'appel silencieux |
| `OPEN_APP` | Launcher Android | Confirmation orale VOXIA, recherche par libellé |
| `SET_ALARM`, `SET_REMINDER` | `AlarmClock` | Confirmation orale VOXIA puis UI externe |

`IntentMapper` lui-même ne gère pas la confirmation : elle est implémentée plus bas, dans `VoiceAssistantService` (`requestConfirmation` + `com.voxia.utils.ConfirmationParser`), pour les intentions sensibles listées ci-dessus. Voir ADR-0005 et ADR-0012 dans `docs/REGISTRE_DECISIONS.md`. Si une future intention modifie l'état du téléphone, expose une donnée privée ou engage l'utilisateur (nouvel achat, envoi de message, etc.), elle doit passer par ce même mécanisme.

## Évaluation attendue

La mesure doit passer par `evaluation/intent/evaluate.py` avec un dataset consenti et versionné. Les métriques minimales sont exactitude, macro-F1, rappel par intention, matrice de confusion, taux d'abstention et taux de fausse action.
