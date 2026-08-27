# VOXIA — registre des risques

**Propriétaires :** COO + CTO
**Dernière revue :** 27 août 2026
**Cadence :** hebdomadaire et à chaque gate
**Score :** probabilité × impact, de 1 à 25

Un risque n'est « réduit » que si une preuve est liée. Un score de 15 à 25 exige une décision conjointe COO/CTO avant release.

| ID | Risque | Score | Owner | Gate | Réduction / preuve attendue | État |
|---|---|---:|---|---|---|---|
| R-001 | Contenu utilisateur dans les logs release | 12 | CTO | 0 | API de logs structurée + test logcat release | En réduction |
| R-002 | Confusion SpeechRecognizer/Vosk | 6 | CTO | 0 | noms/docs honnêtes, aucun modèle Vosk annoncé | Réduit |
| R-003 | Métriques non traçables | 12 | COO | 2 | rapport, commit, dataset gelé, protocole | En réduction |
| R-004 | Fausse action vocale | 20 | CTO | 0/3 | tests adversariaux, confirmation, expiration, taux terrain | Ouvert |
| R-005 | OCR inutilisable par mauvais cadrage | 20 | Accessibilité | 1/3 | guidage temps réel + comparaison terrain | Ouvert |
| R-006 | STT fragile aux accents/bruits | 16 | ML/Data | 2/3 | collecte consentie et WER par sous-groupe | Ouvert |
| R-007 | Artefact trop lourd | 12 | CTO | 4 | AAB par appareil, modules/modèles optimisés | En réduction |
| R-008 | Prototype présenté comme produit validé | 12 | COO | Toutes | vocabulaire alpha + gates et preuves | En réduction |
| R-009 | Identification financière erronée | 20 | COO | Toutes | abstention, aucune valeur/authenticité | En réduction |
| R-010 | TalkBack incomplet | 20 | Accessibilité | 1/3 | tests TalkBack utilisateurs, 0 bloqueur | Ouvert |
| R-011 | Permissions/actions différées incohérentes | 16 | CTO | 0/3 | tests instrumentés refus/retour/rotation | Ouvert |
| R-012 | Données produit inventées/non sourcées | 16 | COO | 2 | catalogue avec source/date, inconnu sinon | En réduction |
| R-013 | Lecture OCR longue incontrôlable | 12 | CTO | 1/3 | navigation, pause/annulation, test terrain | En réduction |
| R-014 | OCR sensible exposé par export | 12 | COO | 3 | avertissement, confirmation, test presse-papiers/chooser | En réduction |
| R-015 | Boutons/caméra inopérants sans service ou micro | 20 | CTO | 0 | file/bind et feedback testés dans `ff300c1` ; appareil requis | En validation P0 |
| R-016 | États STT/TTS/OCR bloqués | 20 | CTO | 0 | terminaisons ciblées testées dans `ff300c1` ; timeouts restants | En réduction P0 |
| R-017 | Langues UI/service/STT/TTS divergentes | 12 | CTO | 0 | politique FR-only testée dans `84d2389` ; appareil requis | En validation P0 |
| R-018 | Confirmation sans expiration/contact ambigu | 20 | CTO | 0 | jetons, délais et choix testés dans `6047ea1` ; appareil requis | En validation P0 |
| R-019 | Double parole TalkBack/TTS | 20 | Accessibilité | 1 | propriétaire unique, 91 tests et FIELD-029 passés dans `7689eeb` sur Xiaomi Android 15 ; appels, notifications, musique et autres appareils restent à valider | En validation P0 |
| R-020 | CVE transitives OkHttp/Okio | 16 | CTO/Sécurité | 0 | contraintes sûres + scan graphe final | Ouvert P0 |
| R-021 | AAB CI potentiellement non signé | 12 | CTO | 0/4 | Play App Signing/secret manager + vérification | Ouvert P0 |
| R-022 | `targetSdk 34` incompatible publication Play | 20 | CTO | 0 | migration API 36 + matrice Android 15/16 | Ouvert P0 |
| R-023 | Revendication « hors ligne » fausse | 20 | COO | 0/3 | contrat, préflight, mode avion cold/warm | Ouvert P0 |
| R-024 | Absence de tests instrumentés et terrain | 20 | QA | 1/3 | androidTest + 30/30 checklist + appareils | Ouvert |
| R-025 | Téléchargements/modèles sans hash | 16 | ML/Data | 0 | manifest provenance, SHA-256, extraction sûre | Ouvert P0 |
| R-026 | Notifications privées en RAM sans purge/TTL | 16 | COO/CTO | 0/3 | TTL, purge, exclusions et confirmation | Ouvert |

## Top 5 du cycle actif

1. R-019 — collision TalkBack/TTS et audio focus.
2. R-016 — timeouts vocaux/OCR restant à couvrir.
3. R-018 — preuve appareil des confirmations et contacts.
4. R-020 — dépendances runtime vulnérables.
5. R-022 — migration API 36.

## Acceptation et fermeture

Chaque fermeture doit indiquer dans le commit ou le journal :

- test ou rapport prouvant la réduction ;
- risque résiduel ;
- date et commit ;
- approbation du propriétaire ;
- décision COO+CTO si le score initial était supérieur ou égal à 15.
