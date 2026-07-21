# Registre des risques — VOXIA

| ID | Risque | Impact | Probabilité | Réduction | État |
|---|---|---:|---:|---|---|
| R-001 | Transcription, OCR ou réponse dans les logs release | Élevé | Moyen | `PrivacyLog`, revue `rg "Log\\."`, tests release | En réduction |
| R-002 | Confusion entre SpeechRecognizer et Vosk | Moyen | Élevé | Classe STT renommée, docs corrigées | Réduit |
| R-003 | Chiffres de précision non traçables | Élevé | Élevé | Docs corrigées, harnais `evaluation/` | En réduction |
| R-004 | Mauvaise action vocale exécutée | Élevé | Moyen | Confirmation orale ajoutée pour appel/alarme/rappel/ouverture d'app (`ConfirmationParser` + `VoiceAssistantService.requestConfirmation`) ; taux de fausse action toujours à mesurer en conditions réelles | En réduction |
| R-005 | OCR inutilisable par mauvais cadrage | Élevé | Élevé | `FrameQualityAnalyzer` filtre les captures trop sombres/claires/floues avant OCR (premier incrément post-capture) ; guidage temps réel et mesure CER/WER réelle restent à faire | En réduction |
| R-006 | STT fragile aux accents/bruits locaux | Élevé | Élevé | Collecte consentie, WER par sous-groupe | Ouvert |
| R-007 | APK trop lourd pour le terrain | Moyen | Élevé | AAB, ABI, modèles à la demande | Ouvert |
| R-008 | Produit présenté comme validé | Élevé | Moyen | Alpha interne, documents de limites | En réduction |
| R-009 | Identification financière erronée | Très élevé | Moyen | Abstention, seuil strict, pas d'authentification | Non démarré |
| R-010 | Accessibilité TalkBack incomplète | Élevé | Moyen | Tests avec utilisateurs cibles et matrice accessibilité | Ouvert |

## Cadence de revue

Revoir ce registre à chaque fin de phase et après tout changement affectant voix, vision, données, permissions ou actions Android.
