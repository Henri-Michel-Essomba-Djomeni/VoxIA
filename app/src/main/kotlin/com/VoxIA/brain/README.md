# Module Brain - VOXIA

## Responsable
Dev3 - Intent Classifier, Pipeline Dialogue, Connexion des modules

## Description
Le module Brain est le cerveau de VOXIA. Il reçoit le texte transcrit 
par Vosk (Dev2), détecte l'intention et la langue, puis déclenche 
l'action correspondante.

## Fichiers

### IntentClassifier.kt
Contient les enums de base :
- Intent : toutes les 25 intentions supportées
- Language : FRENCH, ENGLISH, UNKNOWN
- PredictionResult : résultat retourné (intention + langue + confiance)

### IntentMapper.kt
Reçoit un PredictionResult et exécute l'action correspondante.
Règle : si confiance est inférieure à 70% → demande clarification vocale.

### VoxiaContext.kt
Interface que DEV4 DOIT IMPLÉMENTER dans VoiceAssistantService.
Contient tous les contrats entre Brain et le reste de l'app.

---

## Comment utiliser (pour Dev4)

### Étape 1 - Implémenter VoxiaContext
Dans ton VoiceAssistantService, implémente l'interface :

class VoiceAssistantService : Service(), VoxiaContext {

    override fun speak(fr: String, en: String) {
        // Utiliser Android TTS ici
    }

    override fun loadVisionModule() {
        // Charger YOLOv8n ici (Dev1)
    }

    override fun loadOcrModule() {
        // Charger ML Kit OCR ici
    }

    override fun makeCall(contactName: String?) {
        // Intent.ACTION_CALL ici
    }

    // ... implémenter toutes les autres fonctions
}

### Étape 2 - Charger le modèle TFLite
Le modèle est dans :
app/src/main/assets/intent_classifier.tflite

### Étape 3 - Appeler le pipeline
Quand Vosk retourne un texte transcrit :

val result = PredictionResult(
    intent = Intent.IDENTIFY_OBJECT,
    language = Language.FRENCH,
    confidence = 0.85f
)
IntentMapper.execute(result, this)

---

## Modèle TFLite

- Fichier : intent_classifier.tflite
- Précision actuelle : 71.25%
- Intentions : 25
- Langues : FR + EN
- Entraîné sur : 400 exemples (200 FR + 200 EN)

---

## Connexions avec les autres modules

| Intention | Module appelé | Dev responsable |
|---|---|---|
| identify_object | Vision YOLOv8n | Dev1 |
| read_document | ML Kit OCR | Dev3 |
| call_contact | Intent Android | Dev4 |
| switch_to_english | Vosk STT EN | Dev2 |
| switch_to_french | Vosk STT FR | Dev2 |

---

## Seuil de confiance
- Supérieur à 70% → exécute l'intention
- Inférieur à 70% → TTS "Pouvez-vous répéter ?"

---

## Prochaines étapes
- Semaine 3 : fallback langue + hot-switch
- Semaine 4 : intégration TFLite avec Dev4
- Semaine 6 : ML Kit OCR
- Semaine 7 : extraction contact