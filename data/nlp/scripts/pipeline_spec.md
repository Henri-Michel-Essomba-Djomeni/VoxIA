# Spécification Pipeline Brain - VOXIA

## API principale exposée aux autres devs

fun predict(text: String): Pair<Intent, Language>

## Intentions supportées
- identify_object → déclenche module Vision (Dev1)
- read_document → déclenche module OCR (Dev3)
- call_contact → déclenche appel Android (Dev4)
- switch_to_english → change modèle STT (Dev2)
- switch_to_french → change modèle STT (Dev2)
- set_reminder → alarme Android
- set_alarm → réveil Android
- tell_story → réponse vocale TTS
- tell_joke → réponse vocale TTS
- describe_surroundings → déclenche module Vision (Dev1)
- read_notification → lecture notifications Android
- open_app → ouvre application Android
- calculate → calcul simple
- what_time → heure système Android
- what_date → date système Android
- battery_status → batterie système Android
- volume_up → volume Android
- volume_down → volume Android
- greeting → réponse vocale TTS
- repeat → répète dernière réponse
- stop → arrête VOXIA
- help → liste les fonctions
- tell_motivational → réponse vocale TTS
- who_are_you → réponse vocale TTS
- fallback → TTS "Pouvez-vous répéter ?"

## Langues détectées
- FR → charge vosk-model-small-fr
- EN → charge vosk-model-small-en

## Seuil de confiance
- Plus de 70% → exécute l'intention
- Moins de 70% → fallback vocal

## Règle mémoire
- Un seul modèle IA lourd chargé à la fois
- YOLOv8n chargé uniquement pour identify_object et describe_surroundings
- ML Kit OCR chargé uniquement pour read_document
- Libérer le module précédent avant d'en charger un nouveau