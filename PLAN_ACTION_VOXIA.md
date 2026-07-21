# VOXIA — Plan d’action directeur

**Statut :** document de référence pour la transformation du prototype en produit utilisable  
**Date :** 16 juillet 2026  
**Horizon indicatif :** 4 à 6 mois pour une bêta pilote crédible avec une petite équipe  
**Remplace pour les travaux futurs :** la feuille de route de `STRATEGIE_IMPLEMENTATION.md`, qui décrit surtout la livraison technique du prototype actuel

## 1. Décision exécutive

VOXIA doit être présenté aujourd’hui comme un **prototype Android vocal et visuel**, pas comme un produit IA entraîné ou validé.

La prochaine étape n’est pas d’ajouter davantage de commandes ou de modèles. Elle consiste à :

1. mesurer honnêtement les fonctions actuelles sur des données réelles ;
2. valider un public, une zone géographique et trois besoins prioritaires ;
3. reconstruire l’expérience autour du guidage caméra, d’une voix fiable et d’actions sûres ;
4. choisir les moteurs STT et Vision à partir des résultats, pas d’une préférence technique ;
5. lancer une bêta limitée avant toute communication de précision ou de disponibilité publique.

La proposition produit à tester est :

> **VOXIA aide une personne francophone, notamment aveugle ou malvoyante, à lire ce qui l’entoure, reconnaître certaines informations utiles et agir sur son téléphone avec une interaction vocale simple, sûre et explicite.**

Cette proposition reste une hypothèse jusqu’à la fin de la recherche utilisateur de phase 1.

## 2. Vérité vérifiée sur le dépôt

| Sujet | État vérifié | Conséquence |
|---|---|---|
| Données NLP | `data/nlp/datasets` ne contient qu’un `.gitkeep` | Aucun corpus utilisateur versionné |
| Exemples NLP | Le script contient **125 exemples codés en dur**, pas les 400 annoncés | Le README du module Brain est obsolète et sa métrique n’est pas crédible |
| Modèle d’intentions | Aucun `intent_classifier.tflite` dans l’application | Aucun modèle VOXIA d’intentions n’est livré |
| Vocabulaire | `intent_vocab.json` est embarqué mais jamais lu | Ressource orpheline à supprimer ou réintégrer proprement |
| Intentions actuelles | 26 règles à correspondance exacte ou sous-chaîne | Couverture faible des formulations naturelles |
| Confiance intent | Ratio heuristique entre scores de règles | Ce nombre ne doit pas être présenté comme une probabilité |
| Données vocales | Dossiers français et anglais vides | Aucune mesure sur accents, bruit ou appareils réels |
| STT | `VoskSTTService` utilise en réalité `SpeechRecognizer` Android | Vosk n’est ni embarqué ni évalué ; le nom de classe est trompeur |
| Vision personnalisée | Scripts YOLO présents, mais aucun poids ni modèle Android embarqué | Aucune détection VOXIA entraînée n’est utilisée |
| Vision livrée | ML Kit Image Labeling + OCR + codes-barres | Fonctionnelle, mais générique et non différenciante |
| OCR | `MIN_CONFIDENCE` inutilisé ; stabilisation seulement annoncée en commentaire | Le cadrage et la qualité d’image restent les principaux risques |
| Tests | 12 tests unitaires sur règles, texte et calculatrice ; aucun test UI/instrumenté | Pas de preuve de fiabilité des parcours réels |
| APK | Environ 99,5 Mio et deux ABI, ARM64 et x86_64 | Distribution lourde ; optimisation AAB nécessaire |
| Confidentialité | Transcriptions, OCR et réponses peuvent être écrits dans les logs release | Risque de fuite de données à corriger avant tout pilote |

### Affirmations à retirer immédiatement

- « Précision actuelle : 71,25 % ».
- « Entraîné sur 400 exemples ».
- toute affirmation laissant croire que Vosk, YOLO ou un classifieur TFLite VOXIA est livré ;
- toute précision Vision, STT ou OCR sans protocole, jeu de test gelé et rapport reproductible ;
- « version 1.0 prête pour diffusion publique » tant que les portes de sortie de ce document ne sont pas franchies.

## 3. Ce que les deux audits établissent ensemble

Les deux audits convergent sur cinq conclusions :

1. **La fondation Android existe.** L’application compile, l’APK est signé, CameraX, ML Kit, Android TTS et plusieurs actions système sont branchés.
2. **La valeur utilisateur n’est pas encore démontrée.** Les fonctions sont techniquement présentes, mais les parcours ne sont ni guidés, ni mesurés, ni suffisamment différenciés.
3. **Le problème prioritaire est la mesure.** Sans WER, CER, métriques d’intentions, taux de réussite des tâches et tests utilisateurs, aucun choix de moteur n’est défendable.
4. **Le cadrage caméra est plus urgent qu’un nouveau modèle.** Une bonne OCR sur une mauvaise image reste inutilisable.
5. **VOXIA doit choisir une niche.** L’étiquetage générique de scènes ne constitue ni un avantage produit ni une promesse suffisamment utile.

### Nuances apportées à l’audit externe

- Le script NLP ne contient pas 400 exemples codés en dur, mais 125. Le chiffre 400 vient uniquement du README obsolète.
- Un harnais technique peut être créé en quelques jours, mais collecter des données consenties et représentatives demandera plusieurs semaines.
- 200 enregistrements audio, 1 000 énoncés et 100 photos constituent un **pilote de mesure**, pas une validation de production.
- Whisper ou Vosk ne doivent pas remplacer automatiquement `SpeechRecognizer`. Ils doivent gagner un benchmark sur précision, latence, RAM, batterie, taille et compatibilité.
- La reconnaissance de billets CFA est une piste intéressante, mais pas une décision acquise. XAF et XOF, gammes, recto/verso, anciennes séries et conditions réelles doivent être séparés. La première zone pilote doit être choisie avant la collecte.
- Une application de reconnaissance de coupures ne doit jamais prétendre authentifier un billet ou détecter la contrefaçon sans validation et autorisation spécifiques.
- La base Android est exploitable, mais l’architecture actuelle reste trop couplée : un service central conserve des références UI/caméra et porte trop de responsabilités.

## 4. Périmètre produit recommandé

### 4.1 Trois parcours V1

#### A. Lire

- document imprimé ;
- étiquette de produit ;
- courrier, reçu ou panneau court ;
- lecture segmentée avec pause, reprise, répétition, vitesse, copie et partage ;
- guidage audio et haptique avant la capture.

#### B. Reconnaître

- produit via code-barres et catalogue ;
- texte et marque via OCR comme repli ;
- une catégorie spécialisée seulement après validation de phase 1, par exemple coupures monétaires d’une zone précise ;
- abstention explicite lorsque le résultat n’est pas fiable.

#### C. Agir

- appeler un contact via le composeur ;
- ouvrir une application ;
- créer alarme, minuteur ou rappel avec contenu ;
- lire les notifications après autorisation ;
- contrôler volume, date, heure et batterie ;
- toujours afficher un reçu et confirmer les actions sensibles.

### 4.2 Hors périmètre de la première bêta

- assistant conversationnel généraliste ;
- navigation pour personnes aveugles ;
- détection garantie d’obstacles ;
- diagnostic médical ou conseil de dosage ;
- authentification de monnaie ;
- reconnaissance de centaines de produits par apparence seule ;
- écoute permanente activée par défaut ;
- prix produit présenté sans source et date.

## 5. Principes non négociables

1. **Offline-first, pas offline-only.** Les fonctions essentielles doivent avoir un repli local ; les fonctions cloud doivent être optionnelles et consenties.
2. **Mesurer avant de remplacer.** Aucun moteur STT, NLU ou Vision n’entre en production sans rapport comparatif.
3. **L’abstention est une fonction.** Une incertitude bien exprimée est préférable à une réponse fausse et assurée.
4. **L’utilisateur garde le contrôle.** Corriger, répéter, annuler et confirmer doivent être disponibles à chaque étape.
5. **Aucune donnée sensible dans les logs release.** Transcriptions, contacts, notifications, images et OCR sont privés par défaut.
6. **Le handicap est co-conçu, pas simulé.** Les personnes aveugles ou malvoyantes participent à la définition, aux tests et à la validation.
7. **Une métrique marketing doit être traçable.** Chaque chiffre public renvoie à une version, un protocole et un jeu de test gelé.

## 6. Architecture cible

```text
UI Compose + design system accessible
              |
       ViewModels / UiState
              |
        Cas d’usage métier
   ___________|____________
  |           |            |
Voix        Vision       Actions Android
  |           |            |
Adapters    Capture       Passerelles sûres
STT/TTS     guidée        + confirmations
  |           |
Android /   OCR / code-barres /
Vosk /      modèles spécialisés
Whisper
              |
   Données, historique, consentement
              |
      Évaluation reproductible
```

### Règles d’architecture

- une machine à états unique : `Idle`, `Listening`, `Processing`, `NeedsInput`, `NeedsConfirmation`, `Speaking`, `Error` ;
- `StateFlow` et flux unidirectionnel pour l’état UI ;
- un `ViewModel` par écran ou parcours ;
- aucune référence d’`Activity`, de `PreviewView` ou de lifecycle UI conservée par un service longue durée ;
- service foreground uniquement pendant une fonction qui le justifie ;
- moteurs STT interchangeables derrière la même interface ;
- pipeline Vision interchangeable : capture, contrôle qualité, extraction, interprétation, résultat ;
- moteur d’actions séparé du classifieur d’intentions ;
- annulation structurée par coroutine et propagation à toutes les tâches ;
- historique local chiffrable, désactivable et effaçable ;
- événements techniques sans contenu utilisateur par défaut.

## 7. Programme d’évaluation

### 7.1 Structure à créer

```text
evaluation/
  README.md
  schemas/
  intent/
    datasets/
    evaluate.py
    reports/
  stt/
    manifests/
    evaluate.py
    reports/
  ocr/
    manifests/
    evaluate.py
    reports/
  vision_specialized/
    manifests/
    evaluate.py
    reports/
```

Chaque rapport doit inclure : commit Git, version du modèle/moteur, appareil, OS, paramètres, date, jeu de données, métriques globales et métriques par sous-groupe.

### 7.2 Intentions

#### Pilote

- 300 énoncés réels issus d’entretiens et de tests ;
- au moins 10 personnes, avec variations d’âge, aisance numérique et formulation ;
- inclure formulations positives, ambiguës, hors périmètre et dangereuses.

#### Jeu de référence

- objectif initial : 1 000 énoncés consentis ;
- séparation par participant entre entraînement et test ;
- jeu de test gelé et jamais utilisé pour régler les règles ;
- métriques : exactitude, macro-F1, rappel par intention, matrice de confusion, taux d’abstention, taux de fausse action.

#### Baselines à comparer

1. règles actuelles ;
2. règles enrichies avec normalisation, synonymes, similarité de tokens et extraction de slots ;
3. petit classifieur local seulement si la baseline 2 ne suffit pas.

### 7.3 Reconnaissance vocale

#### Pilote

- 200 à 300 commandes enregistrées ;
- participants de la zone réellement ciblée ;
- environnements calme, rue, ventilateur, télévision et conversation proche ;
- téléphones entrée, milieu et haut de gamme ;
- consentement explicite, transcription de vérité et identifiants pseudonymisés.

#### Moteurs à comparer

- `SpeechRecognizer` Android local lorsqu’il existe ;
- moteur système avec réseau ;
- Vosk français mobile ;
- whisper.cpp avec au moins deux tailles quantifiées compatibles avec Android.

#### Mesures

- WER global et par accent/bruit/appareil ;
- réussite sémantique de la commande ;
- temps jusqu’au premier texte et au texte final ;
- real-time factor ;
- RAM maximale, batterie sur une session standard, taille téléchargée ;
- disponibilité hors ligne et taux d’échec d’initialisation.

#### Porte de décision

Choisir le moteur ou la cascade qui maximise la réussite sémantique sans dépasser les budgets de latence, mémoire, batterie et taille. Conserver un repli explicite lorsqu’aucun moteur ne satisfait le seuil.

### 7.4 OCR et cadrage

#### Pilote

- au moins 100 captures réalisées par les utilisateurs cibles ;
- documents, étiquettes, reçus et panneaux ;
- faible lumière, reflets, inclinaison, flou, texte partiellement coupé ;
- vérité terrain textuelle et annotation de la cause d’échec.

#### Comparaisons

1. capture immédiate actuelle ;
2. contrôle de netteté, luminosité, reflets et couverture du cadre ;
3. guidage vocal/haptique + capture automatique ;
4. recadrage et correction de perspective.

#### Mesures

- CER et WER OCR ;
- taux de document entièrement cadré ;
- nombre de reprises ;
- temps jusqu’à une lecture utile ;
- taux de tâche réussie sans aide d’un tiers ;
- compréhension de la lecture par l’utilisateur.

### 7.5 Vision spécialisée

Ne démarrer la collecte qu’après validation du besoin.

Pour chaque domaine candidat, documenter : fréquence du problème, gravité, alternatives existantes, disponibilité/licence des données, difficulté, risque d’une erreur, coût de maintenance et valeur locale.

Domaines candidats à comparer :

- coupures monétaires d’une zone précise ;
- médicaments et emballages locaux, sans conseil médical ;
- produits alimentaires locaux via code-barres/OCR ;
- documents administratifs fréquents ;
- objets domestiques limités définis par les utilisateurs.

## 8. Feuille de route par phases

### Phase 0 — Assainissement et vérité produit

**Durée :** 3 à 5 jours  
**Objectif :** rendre le dépôt et la communication honnêtes avant tout nouveau développement.

Actions :

- remplacer les affirmations non prouvées dans README, guide et documentation Brain ;
- renommer la version actuelle en alpha interne ;
- inventorier fonctions réelles, fonctions partielles et fonctions absentes ;
- retirer les logs contenant transcription, OCR et réponse en release ;
- documenter les dépendances cloud, modèles téléchargés et limites hors ligne ;
- créer un registre des décisions et un registre des risques ;
- geler une baseline APK et son rapport de comportement.

Sortie obligatoire :

- aucun chiffre de précision non traçable ;
- documentation cohérente avec l’APK ;
- baseline installable et archivée ;
- liste publique de limites.

### Phase 1 — Recherche utilisateur et harnais d’évaluation

**Durée :** 2 à 4 semaines  
**Objectif :** savoir pour qui VOXIA est construit et mesurer l’existant.

Actions :

- recruter 10 à 15 participants pour la découverte ;
- réaliser entretiens contextuels et tests de l’APK actuelle ;
- choisir une première zone géographique ;
- classer les problèmes par fréquence, gravité et valeur ;
- construire les évaluateurs Intent, STT et OCR ;
- collecter les jeux pilotes avec consentement ;
- produire le premier rapport comparatif ;
- choisir les trois parcours de la bêta et le domaine Vision candidat.

Porte de sortie :

- audience et zone pilote écrites ;
- trois tâches prioritaires validées par les participants ;
- baselines Intent/STT/OCR mesurées ;
- décision documentée sur le prochain moteur STT à prototyper ;
- décision `go/no-go` sur la reconnaissance de billets ou un autre domaine spécialisé.

### Phase 2 — Reconstruction de l’expérience centrale

**Durée :** 4 à 6 semaines  
**Objectif :** rendre Lire, Reconnaître et Agir compréhensibles et utilisables.

Actions UI :

- reconstruire l’interface avec Compose, Material 3 et une identité VOXIA accessible ;
- écrans : Accueil, Lire, Reconnaître, Actions, Historique/Réglages ;
- onboarding avant permissions ;
- bouton vocal central avec états, son et haptique ;
- taille de texte jusqu’à 200 %, TalkBack, contraste, commandes externes et mode paysage ;
- résultats sous forme de cartes avec source, confiance compréhensible et actions suivantes.

Actions Vision/OCR :

- flux `ImageAnalysis` pour évaluer qualité et cadrage ;
- guidage « plus haut », « rapprochez », « trop sombre », « maintenez stable » ;
- capture automatique lorsque la qualité est suffisante ;
- reprise et recadrage ;
- segmentation TTS, pause, reprise, répétition, vitesse, copie et partage ;
- ne plus lire les pourcentages bruts à voix haute.

Actions produit :

- code-barres vers un catalogue avec source et date ;
- cache local ;
- repli OCR marque/nom ;
- résultat « produit inconnu » exploitable ;
- ne jamais inventer prix, allergènes ou composition.

Porte de sortie :

- les trois parcours sont terminables avec TalkBack ;
- les permissions refusées puis accordées sont gérées ;
- Annuler stoppe voix, caméra, OCR, traduction et action en attente ;
- le guidage améliore significativement le taux de capture exploitable face à la baseline.

### Phase 3 — Compréhension et actions fiables

**Durée :** 3 à 5 semaines  
**Objectif :** comprendre des formulations réelles et empêcher les mauvaises actions.

Actions :

- schéma déclaratif des intentions, exemples, synonymes, slots et niveau de risque ;
- similarité de tokens et correspondance floue explicable ;
- extraction robuste de contact, application, heure, durée, date, contenu et langue ;
- gestion multi-tour pour toute information manquante ;
- annulation et correction du tour courant ;
- désambiguïsation des contacts et applications ;
- confirmation des appels, rappels et actions à risque ;
- reçu visuel/vocal et possibilité d’annuler ;
- historique local des actions sans contenu sensible par défaut ;
- comparaison en CI sur le jeu gelé.

Porte de sortie :

- macro-F1 au moins 0,90 sur le domaine retenu ou objectif revu explicitement ;
- réussite de tâche vocale au moins 90 % en environnement calme sur le pilote ;
- fausse action inférieure à 1 % ;
- zéro action sensible exécutée sans confirmation ;
- les commandes hors périmètre produisent une abstention utile.

### Phase 4 — Moteur vocal et Vision spécialisée

**Durée :** 4 à 8 semaines, dépendante des données  
**Objectif :** ajouter uniquement les modèles qui ont gagné les benchmarks.

Voix :

- intégrer l’adapter du moteur gagnant ;
- packs/modèles téléchargeables plutôt que tous embarqués ;
- progression et reprise du téléchargement ;
- cache et versionnement ;
- repli sur le moteur Android ;
- mesure continue par appareil sans enregistrer le contenu utilisateur en production.

Vision spécialisée :

- protocole de collecte et licence validés ;
- classes limitées au besoin retenu ;
- séparation train/validation/test par personne, appareil et lieu ;
- quantification et benchmark sur téléphones réels ;
- test hors distribution ;
- seuils d’abstention calibrés ;
- carte de modèle précisant données, limites et usages interdits.

Si les billets BEAC sont retenus pour un pilote CEMAC :

- commencer par la gamme et les cinq coupures officiellement ciblées ;
- traiter recto, verso, rotation, pli, usure, occultation et éclairage ;
- inclure des objets ressemblants et d’autres monnaies dans les négatifs ;
- annoncer uniquement la valeur probable ;
- afficher « je ne peux pas vérifier l’authenticité » ;
- obtenir un avis sur le droit d’utilisation et de reproduction des images.

Porte de sortie Vision financière :

- exactitude au moins 98 % sur images acceptées ;
- taux de réponse fausse avec forte confiance inférieur à 0,1 % ;
- abstention obligatoire sur image insuffisante ou hors distribution ;
- validation avec utilisateurs cibles sur téléphones réels ;
- aucune promesse d’authentification.

### Phase 5 — Durcissement bêta

**Durée :** 3 à 4 semaines  
**Objectif :** préparer une bêta fermée sûre et observable.

Actions :

- tests unitaires, intégration, UI, permissions, service, rotation et reprise ;
- tests Android 10, 12, 14 et version courante ;
- appareils sans Google Play Services ou sans pack vocal local ;
- faible RAM, économie de batterie, faible stockage et réseau instable ;
- TalkBack, Switch Access, tailles de police et contraste ;
- profilage démarrage, caméra, mémoire, batterie et ANR ;
- AAB, livraison par ABI et modules à la demande ;
- politique de confidentialité, consentement, suppression et export des données ;
- canal de retour accessible ;
- crash reporting sans contenu sensible ;
- revue de sécurité et revue des textes de sûreté.

Porte de sortie :

- plus de 99,5 % de sessions sans crash sur le pilote ;
- taux d’ANR inférieur à 0,3 % ;
- zéro donnée utilisateur dans les logs release ;
- première action utile en moins de 60 secondes pour un nouvel utilisateur ;
- taille servie cible inférieure à 60 Mio par appareil ;
- aucun bloqueur TalkBack ;
- chaque erreur explique la cause et l’action suivante.

### Phase 6 — Pilote terrain et décision de lancement

**Durée :** 4 semaines minimum  
**Objectif :** démontrer un impact réel avant une diffusion large.

Actions :

- bêta fermée de 30 à 50 utilisateurs ;
- suivi hebdomadaire des tâches réussies, abandons, erreurs et retours ;
- entretiens à J1, J7 et J30 sur un sous-échantillon ;
- comparaison avec la méthode actuelle de l’utilisateur ;
- correction des trois principales causes d’échec ;
- rapport final technique, accessibilité et impact ;
- décision : publier, prolonger le pilote, réduire le périmètre ou arrêter une fonction.

## 9. Plan des 30 premiers jours

### Semaine 1 — Assainir

- corriger les affirmations documentaires ;
- créer baseline, registre des risques et métriques ;
- supprimer les logs sensibles release ;
- établir la liste exacte des fonctions ;
- écrire protocole de consentement et schémas de données.

### Semaine 2 — Observer

- recruter les premiers participants ;
- tester l’APK actuelle sans les guider ;
- noter tâches, formulations, erreurs de cadrage et réactions ;
- implémenter les scripts d’évaluation Intent/STT/OCR ;
- préparer trois appareils de référence.

### Semaine 3 — Mesurer et prototyper

- collecter le premier lot audio, texte et images ;
- produire les baselines ;
- construire un prototype de guidage caméra ;
- enrichir les règles d’intentions sans ML ;
- tester un catalogue code-barres.

### Semaine 4 — Décider

- comparer capture actuelle et capture guidée ;
- comparer les premiers moteurs STT ;
- choisir zone, public et trois parcours ;
- décider si les billets, médicaments ou produits locaux justifient une étude spécialisée ;
- publier le rapport de phase 1 et le backlog de phase 2.

## 10. Backlog priorisé

### P0 — Bloquant

- [ ] Retirer les métriques non prouvées.
- [ ] Marquer la version actuelle comme alpha interne.
- [ ] Éliminer les logs sensibles release.
- [ ] Construire les harnais Intent, STT et OCR.
- [ ] Écrire consentement, gouvernance et licences des données.
- [ ] Tester avec des utilisateurs cibles.
- [ ] Corriger l’annulation globale et les états incohérents.
- [ ] Expliquer les permissions avant de les demander.

### P1 — Valeur centrale

- [ ] Guidage caméra temps réel.
- [ ] Lecture OCR segmentée et contrôlable.
- [ ] Recherche produit par code-barres.
- [ ] Nouvelle architecture UI/état.
- [ ] Dialogue multi-tour et confirmations.
- [ ] Comparatif STT sur accents et bruit.
- [ ] Accessibilité TalkBack et texte 200 %.
- [ ] Historique et réglages de confidentialité.

### P2 — Différenciation mesurée

- [ ] Choisir un domaine Vision spécialisé.
- [ ] Collecter et documenter les données.
- [ ] Entraîner, quantifier et calibrer l’abstention.
- [ ] Intégrer le moteur STT gagnant.
- [ ] Ajouter feedback « correct / incorrect » consentant.
- [ ] Produire cartes de modèles et rapports de biais.

### P3 — Industrialisation

- [ ] Bêta fermée et crash reporting respectueux de la vie privée.
- [ ] AAB, modules à la demande et optimisation de taille.
- [ ] CI complète et matrice d’appareils.
- [ ] Documentation d’exploitation et procédure de rollback.
- [ ] Dossier de financement basé sur données, résultats et témoignages.

## 11. Critères de réussite par axe

| Axe | Mesure principale | Cible bêta |
|---|---|---|
| Activation | Temps jusqu’à la première tâche utile | < 60 secondes |
| Intentions | Macro-F1 sur jeu gelé | ≥ 0,90 ou seuil justifié |
| Sûreté actions | Action sensible sans confirmation | 0 |
| Erreur actions | Fausse action exécutée | < 1 % |
| STT | Réussite sémantique calme | ≥ 90 % |
| STT bruité | Réussite sémantique pilote | ≥ 80 % ou repli clair |
| OCR | Tâche réussie avec capture utilisateur | ≥ 85 % |
| Cadrage | Amélioration face à la baseline | significative et documentée |
| Vision spécialisée | Réponse fausse très confiante | < 0,1 % pour domaine financier |
| Accessibilité | Bloqueur TalkBack | 0 |
| Stabilité | Sessions sans crash | > 99,5 % |
| ANR | Sessions avec ANR | < 0,3 % |
| Confidentialité | Contenu utilisateur dans logs release | 0 |
| Distribution | Taille servie par appareil | < 60 Mio |

Les cibles peuvent être révisées après la baseline, mais jamais silencieusement : toute modification doit être datée et justifiée.

## 12. Gouvernance des données

- consentement compréhensible et révocable ;
- rémunération ou compensation des participants lorsque possible ;
- identifiants pseudonymisés ;
- séparation des coordonnées de contact et des données de recherche ;
- durée de conservation définie ;
- suppression sur demande ;
- aucun contact, notification ou document personnel dans les datasets ;
- licences vérifiées pour chaque image, audio, catalogue et modèle ;
- fiche de dataset avec provenance, population, appareils, conditions et limites ;
- test gelé inaccessible au réglage quotidien ;
- suivi des performances par accent, bruit, appareil, lumière et groupe utilisateur ;
- chiffrement des données sensibles au repos et en transit ;
- interdiction d’utiliser les données de bêta pour l’entraînement sans consentement distinct.

## 13. Registre des risques

| Risque | Impact | Réduction |
|---|---|---|
| Mauvaise reconnaissance énoncée avec assurance | Perte de confiance ou préjudice | Abstention, confirmation, seuils calibrés |
| Données d’accents insuffisantes | STT injuste et fragile | Collecte locale, métriques par sous-groupe |
| Cadrage impossible sans aide | OCR inutilisable | Guidage audio/haptique et capture automatique |
| Mauvaise coupure monétaire | Préjudice financier | Domaine limité, seuil strict, abstention, pas d’authentification |
| Médicament mal identifié | Risque de santé | Code-barres/OCR prioritaire, aucune posologie, avertissements |
| Service micro permanent | Batterie, vie privée, restrictions Android | Push-to-talk par défaut, wake word optionnel |
| APK trop lourd | Faible adoption et coût data | AAB, ABI, modèles à la demande |
| Prototype présenté comme validé | Perte de crédibilité financeur | Rapport reproductible et communication honnête |
| Données sensibles dans télémétrie | Atteinte à la vie privée | Minimisation, redaction, revue release |
| Dépendance à un seul fournisseur | Rupture de fonction | Interfaces adapters et replis testés |

## 14. Équipe minimale et responsabilités

Pour tenir l’horizon de 4 à 6 mois :

- **Produit/recherche :** besoins, recrutement, protocole, priorités et impact ;
- **Android :** architecture, UI, CameraX, actions, performance et distribution ;
- **ML/data :** datasets, baselines, STT, modèles Vision et calibration ;
- **Design/accessibilité :** co-conception, TalkBack, guidage, contenus et tests ;
- **QA/sécurité :** CI, appareils, permissions, confidentialité et release.

Avec une seule personne, respecter strictement l’ordre : mesure → guidage/OCR → intentions/actions → moteur STT → modèle Vision → bêta.

## 15. Livrables attendus pour une due diligence

- démonstration de trois tâches centrales sur téléphone réel ;
- protocole et résultats d’étude utilisateur ;
- rapports Intent, STT, OCR et Vision reproductibles ;
- jeux de données documentés et légalement utilisables ;
- matrice appareils/OS ;
- architecture et décisions techniques ;
- politique de confidentialité et flux de données ;
- cartes de modèles et limites ;
- métriques de stabilité et accessibilité ;
- témoignages et mesure d’impact ;
- plan financier pour collecte, maintenance des modèles et support.

## 16. Références techniques de départ

- Android `SpeechRecognizer` : le moteur système peut utiliser un service distant et n’est pas destiné à l’écoute continue — <https://developer.android.com/reference/android/speech/SpeechRecognizer.html>
- Restrictions Android des services foreground microphone — <https://developer.android.com/develop/background-work/services/fgs/restrictions-bg-start>
- Architecture Android et état UI — <https://developer.android.com/topic/architecture/recommendations>
- ML Kit Image Labeling : classification générique de l’image entière et modèles personnalisés — <https://developers.google.com/ml-kit/vision/image-labeling>
- Vosk mobile/offline et adaptation — <https://alphacephei.com/vosk/> et <https://alphacephei.com/vosk/adaptation>
- whisper.cpp — <https://github.com/ggml-org/whisper.cpp>
- Open Food Facts, recherche produit par code-barres — <https://openfoodfacts.github.io/documentation/docs/Product-Opener/v3/products/get-api-v3-product-code/>
- BEAC, gamme 2020 et coupures — <https://www.beac.int/accueil/billets-gamme-2020/>
- Réduction de taille et Android App Bundle — <https://developer.android.com/topic/performance/reduce-apk-size>
- Tests d’accessibilité Android — <https://developer.android.com/guide/topics/ui/accessibility/testing>

## 17. Prochaine action unique

La prochaine action recommandée est de lancer **Phase 0 puis Phase 1**, et non de réécrire immédiatement toute l’application.

Le premier jalon doit produire, dans cet ordre :

1. documentation corrigée ;
2. harnais d’évaluation versionné ;
3. protocole de recherche et consentement ;
4. baseline Intent/STT/OCR ;
5. prototype de guidage caméra ;
6. décision écrite sur le public, la zone pilote, le moteur STT et le domaine Vision.

Tant que ces six éléments ne sont pas disponibles, tout nouveau modèle ou nouvelle fonctionnalité majeure doit rester en attente.
