# Registre des risques — VOXIA

| ID | Risque | Impact | Probabilité | Réduction | État |
|---|---|---:|---:|---|---|
| R-001 | Transcription, OCR ou réponse dans les logs release | Élevé | Moyen | `PrivacyLog`, revue `rg "Log\\."`, tests release | En réduction |
| R-002 | Confusion entre SpeechRecognizer et Vosk | Moyen | Élevé | Classe STT renommée, docs corrigées | Réduit |
| R-003 | Chiffres de précision non traçables | Élevé | Élevé | Docs corrigées, harnais `evaluation/` | En réduction |
| R-004 | Mauvaise action vocale exécutée | Élevé | Moyen | Confirmation orale ajoutée pour appel/alarme/rappel/ouverture d'app (`ConfirmationParser` + `VoiceAssistantService.requestConfirmation`) ; actions différées par permission purgées en cas de refus ; taux de fausse action toujours à mesurer en conditions réelles | En réduction |
| R-005 | OCR inutilisable par mauvais cadrage | Élevé | Élevé | `FrameQualityAnalyzer` filtre les captures trop sombres/claires/floues avant OCR (premier incrément post-capture) ; guidage temps réel et mesure CER/WER réelle restent à faire | En réduction |
| R-006 | STT fragile aux accents/bruits locaux | Élevé | Élevé | Collecte consentie, WER par sous-groupe | Ouvert |
| R-007 | APK trop lourd pour le terrain | Moyen | Élevé | ABI filtrées `arm64-v8a`/`x86_64`, AAB release généré en CI ; mesure de taille terrain et modules à la demande restent à faire | En réduction |
| R-008 | Produit présenté comme validé | Élevé | Moyen | Alpha interne, documents de limites | En réduction |
| R-009 | Identification financière erronée | Très élevé | Moyen | `FinancialSafety` force une abstention sur argent/monnaie/coupure/CFA/XAF/XOF et labels associés ; aucune valeur ni authenticité annoncée | En réduction |
| R-010 | Accessibilité TalkBack incomplète | Élevé | Moyen | Descriptions accessibles synchronisées pour transcription/réponse, boutons moins fragiles à grande police ; tests TalkBack terrain toujours requis | En réduction |
| R-011 | Dialogue de permission ou action différée incohérente | Moyen | Moyen | Rationale avant demande système, `POST_NOTIFICATIONS` séquencé après audio, purge des actions caméra/contacts refusées | En réduction |
| R-012 | Produit identifié avec des informations inventées ou non sourcées | Élevé | Moyen | Catalogue local exigeant source/date, réponse explicite "produit inconnu", interdiction de prix/allergènes/composition sans source | En réduction |
| R-013 | Lecture OCR longue impossible à contrôler | Moyen | Élevé | Segmentation OCR, session de lecture, commandes et boutons suivant/précédent/répéter, vitesse réglable, copie/partage explicites | En réduction |
| R-014 | Texte OCR sensible exposé via presse-papiers ou partage | Élevé | Moyen | Actions copie/partage explicites, avertissement UI avant bouton, confirmation orale avant export vocal, partage via chooser Android | En réduction |

## Cadence de revue

Revoir ce registre à chaque fin de phase et après tout changement affectant voix, vision, données, permissions ou actions Android.
