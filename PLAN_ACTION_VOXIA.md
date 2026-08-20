# VOXIA — plan directeur CTO/COO

**Statut :** source unique de pilotage
**Version du plan :** 2.0
**Date de référence :** 20 août 2026
**Propriétaires :** COO/Product Owner + CTO/Android Lead
**Version cible :** `0.2.0-alpha-offline`
**Décision actuelle :** stabiliser et livrer la version hors ligne avant toute couche cloud

## 1. Mission et résultat attendu

VOXIA doit permettre à une personne aveugle ou malvoyante francophone de réaliser de façon autonome trois tâches quotidiennes :

1. lire un document ou une étiquette ;
2. reconnaître une information visuelle limitée et utile ;
3. agir sur son téléphone avec une interaction vocale sûre.

La prochaine version livrable est une **alpha hors ligne de terrain**. Elle ne doit dépendre ni d'un compte, ni d'une clé privée, ni d'un modèle absent, ni d'un cloud obligatoire.

La future version en ligne sera optionnelle. Elle ne commencera qu'après validation de la version hors ligne et devra préciser ce qui quitte le téléphone, pourquoi, pendant combien de temps et avec quel repli local.

## 2. Situation de départ vérifiée

| Axe | État | Décision |
|---|---|---|
| Build Android | Tests, lint, APK debug et AAB release réussissent localement | Base exploitable |
| Tests unitaires | 58 cas distincts, 116 exécutions debug/release, 0 échec | Insuffisant sans tests appareil |
| Terrain | 30/30 scénarios non exécutés | Aucun parcours utilisateur déclaré validé |
| Voix | SpeechRecognizer/TTS Android | Corriger les états bloquants et clarifier le réseau |
| Intentions | Règles locales trop permissives | Ajouter tests négatifs, seuils et timeouts |
| Vision | ML Kit générique, capture unique | Construire le guidage caméra temps réel |
| Produit | Catalogue TSV vide | Ne promettre aucune fiche produit avant données sourcées |
| Accessibilité | Bons contrastes/cibles, TalkBack non validé | Reconcevoir le parcours et tester avec utilisateurs |
| Sécurité | Bon cloisonnement local, supply chain incomplète | Corriger dépendances, modèles, CI et disclosures |
| Distribution | `targetSdk 34` | Migrer vers API 36 avant publication Play |

Score d'audit initial : **43/100**. Préparation estimée au pilote : **28 %**. Ces scores servent uniquement de baseline interne et seront remplacés par des métriques terrain.

## 3. Périmètre des versions

### Version hors ligne — engagement actuel

Inclus :

- bouton Parler utilisable sans wake word ;
- STT/TTS système avec indication claire du moteur et mode local strict lorsqu'il est disponible ;
- OCR, codes-barres et étiquetage générique embarqués ;
- guidage caméra audio/haptique ;
- lecture segmentée, répétition, vitesse, pause/annulation, copie et partage explicites ;
- appels via composeur, alarmes/minuteurs, ouverture d'app et utilitaires ;
- confirmation, expiration et reçu des actions sensibles ;
- catalogue local sourcé ; produit inconnu sinon ;
- français complet, anglais uniquement si toutes les ressources et états sont cohérents ;
- aucune donnée utilisateur persistée ou journalisée par défaut.

Exclus :

- conversation généraliste ;
- navigation ou détection garantie d'obstacles ;
- authentification de monnaie ;
- diagnostic, médicament ou dosage ;
- reconnaissance universelle de produits par apparence ;
- écoute permanente par défaut ;
- prix, allergènes ou composition sans source datée.

### Version en ligne — après gate hors ligne

Candidats, non engagés : description visuelle conversationnelle, catalogue distant, synchronisation chiffrée, amélioration STT et assistance humaine. Chaque fonction exigera consentement, minimisation, coût d'exploitation, SLA, politique de suppression et mode dégradé.

## 4. Règles de fonctionnement CTO/COO

1. Une seule source de vérité par sujet ; aucun nouveau document de planification sans décision explicite.
2. Une fonctionnalité n'est « faite » que si code, tests, accessibilité, sécurité et preuve sont livrés ensemble.
3. Aucun modèle ou fournisseur n'est choisi sans benchmark sur les appareils et utilisateurs cibles.
4. Une réponse incertaine doit s'abstenir ; une action sensible doit confirmer puis expirer.
5. Toute modification de périmètre, seuil ou risque est datée dans le registre approprié.
6. Chaque incrément doit laisser `main` compilable, testé et documenté.
7. Les données terrain ne sont jamais ajoutées au dépôt sans consentement, licence et anonymisation.

### Cadence

- Début de cycle : sélectionner au plus trois résultats mesurables.
- Quotidien : code et tests ; pas de journal narratif obligatoire.
- Fin d'incrément : tests, lint, artefact, revue risques/décisions et une entrée concise dans le journal.
- Fin d'étape : démonstration sur téléphone, rapport de gate et décision `GO`, `GO sous conditions` ou `NO-GO`.

### Responsabilités

| Responsabilité | Redevable | Livrable |
|---|---|---|
| Produit, périmètre, priorités | COO/Product Owner | objectifs, utilisateurs, décisions de scope |
| Architecture, sécurité, release | CTO/Android Lead | code, ADR, CI, APK/AAB |
| Accessibilité | Design/QA avec utilisateurs cibles | parcours, audits TalkBack, preuves terrain |
| IA et données | ML/Data Lead | datasets, benchmarks, cartes de modèles |
| Qualité | QA/Release Owner | matrice appareils, rapports, décision de gate |

Si une seule personne cumule les rôles, l'ordre des gates reste obligatoire.

## 5. Definition of Done commune

Un item est terminé seulement si :

- comportement nominal, refus, erreur, timeout et annulation sont traités ;
- tests unitaires et, si applicable, tests instrumentés couvrent le risque ;
- aucun contenu utilisateur n'apparaît dans les logs release ;
- TalkBack, police 200 %, orientation et retour audio/haptique ont été vérifiés pour toute UI touchée ;
- dépendances, permissions et collecte de données sont minimales ;
- documentation factuelle et registre de risques sont mis à jour ;
- `test`, `lintDebug`, `assembleDebug` et la tâche release attendue réussissent ;
- l'artefact est identifiable par version, commit, hash et rapport.

## 6. Étapes et gates

### Étape 0 — Baseline propre et fiable

**Objectif :** éliminer les blocages techniques et rendre la construction hors ligne reproductible avant toute nouvelle fonctionnalité.

Backlog bloquant :

- [ ] corriger les états STT/TTS bloqués et protéger l'initialisation OCR ;
- [ ] découpler les parcours caméra de l'autorisation microphone et supprimer les actions silencieuses avant binding ;
- [ ] synchroniser langue service/STT/TTS/UI ou désactiver temporairement l'anglais incomplet ;
- [ ] ajouter timeout et identité de transaction aux confirmations ;
- [ ] renforcer le classifieur contre les mots isolés et ajouter un corpus négatif ;
- [ ] désambiguïser les contacts avant ouverture du composeur ;
- [ ] neutraliser les versions OkHttp/Okio vulnérables et vérifier le graphe final ;
- [ ] migrer `compileSdk`/`targetSdk` vers API 36 et tester les changements Android 15/16 ;
- [ ] sécuriser les téléchargements de modèles et verrouiller les dépendances Python ;
- [ ] clarifier STT/TTS/ML Kit, mode local et confidentialité dans l'interface ;
- [ ] durcir CI, signature, checksum wrapper, SBOM, scans et licences ;
- [ ] ajouter une licence projet et les notices tierces requises ;
- [ ] figer une baseline APK hors ligne avec hash et rapport.

Gate 0 :

- 0 test en échec, 0 erreur lint ;
- 100 scénarios automatisés voix/intentions sans état bloqué ;
- 0 action silencieuse depuis l'UI ;
- 0 CVE High connue dans le graphe livré ou exception signée ;
- build reproductible depuis un checkout propre ;
- APK installable, versionnée et accompagnée de son hash ;
- registre des risques à jour.

### Étape 1 — Expérience accessible hors ligne

**Objectif :** rendre Lire, Reconnaître et Agir terminables sans assistance visuelle.

- écran organisé par trois modes, commandes contextuelles et Annuler toujours accessible ;
- coordination TalkBack/TTS et audio focus ;
- feedback immédiat sonore et haptique ;
- guidage caméra temps réel, stabilité, lumière, cadrage, torche et auto-capture ;
- français complet, libellés explicites, headings et mise en page 200 % ;
- paramètres de voix, vitesse, haptique, contraste et verbosité.

Gate 1 : 0 erreur AccessibilityChecks, 0 double annonce sur 30 scénarios, 0 troncature à 200 %, feedback initial inférieur à 300 ms et trois parcours terminables avec TalkBack.

### Étape 2 — Évaluation et données réelles

**Objectif :** remplacer les templates factices par des baselines fiables.

- harnais Intent identique au moteur Kotlin livré ;
- jeux gelés : intentions, STT, OCR et produit ;
- collecte consentie par accent, bruit, lumière et appareil ;
- Macrobenchmark release sur téléphone réel ;
- catalogue initial sourcé et daté.

Gate 2 : au moins 1 000 énoncés, 200 commandes audio, 300 captures OCR, rapports reproductibles et aucune fuite entre réglage et test.

### Étape 3 — Pilote fermé

**Objectif :** démontrer l'utilité et la sûreté avec les utilisateurs cibles.

- 10 à 15 participants de co-conception, puis 30 à 50 utilisateurs pilote ;
- Android 10/11, 14 et 16, appareils bas et milieu de gamme ;
- TalkBack, 200 %, faible lumière, bruit et refus de permissions ;
- suivi des échecs sans enregistrer le contenu utilisateur.

Gate 3 : réussite sans aide supérieure ou égale à 90 % sur les trois tâches, 0 erreur dangereuse, capture exploitable au premier essai supérieure ou égale à 85 %, sessions sans crash supérieures à 99,5 %.

### Étape 4 — Release hors ligne

**Objectif :** publier une version stable, signée, traçable et supportable.

- AAB Play et APK de distribution contrôlée ;
- taille servie cible inférieure à 60 Mio par appareil ;
- politique de confidentialité, Data Safety, support et rollback ;
- monitoring technique sans contenu sensible ;
- audit sécurité/accessibilité final.

Gate 4 : tous les critères release réussis, aucun risque critique ouvert et décision de publication signée.

### Étape 5 — Fonctions en ligne optionnelles

**Objectif :** augmenter la valeur sans dégrader autonomie, coût ou confidentialité.

Chaque fonction cloud passe séparément par : étude utilisateur, architecture de données, menace, consentement, coût unitaire, benchmark, repli hors ligne et kill switch.

## 7. Indicateurs de pilotage

| Axe | Indicateur | Seuil avant release |
|---|---|---:|
| Utilité | Réussite sans aide sur les trois tâches | ≥ 90 % |
| Sûreté | Action sensible sans confirmation | 0 |
| Intentions | Macro-F1 sur jeu gelé | ≥ 0,90 |
| STT calme | WER domaine ciblé | ≤ 15 % |
| STT bruit modéré | WER domaine ciblé | ≤ 25 % |
| OCR contrôlé | CER texte imprimé | ≤ 5 % |
| Capture | Exploitable au premier essai | ≥ 85 % |
| Réactivité | Feedback initial | ≤ 300 ms |
| Accessibilité | Bloqueur TalkBack / troncature 200 % | 0 |
| Stabilité | Sessions sans crash | ≥ 99,5 % |
| ANR | Sessions avec ANR | < 0,1 % |
| Confidentialité | Contenu utilisateur dans logs release | 0 |
| Distribution | Taille servie par appareil | < 60 Mio |

Les seuils peuvent évoluer après une baseline réelle, mais uniquement par décision datée.

## 8. Backlog actif

Le backlog actif est limité à l'étape en cours. Au 20 août 2026, seule **l'étape 0** est autorisée. Toute fonctionnalité non liée à sa gate reste en attente.

| ID | Résultat attendu | Owner | Statut | Preuve de sortie |
|---|---|---|---|---|
| P0-001 | Gouvernance finale et baseline fusionnées sur `main` | COO+CTO | Terminé | `9ef1ac5`, liens/docs et build contrôlés |
| P0-002 | Caméra et actions UI indépendantes du micro/binding | CTO | Implémenté, appareil requis | `ff300c1`, file testée ; preuve instrumentée/terrain restante |
| P0-003 | États STT/TTS/OCR toujours terminaux | CTO | En validation | `ff300c1`, 9 tests ajoutés au lot ; timeouts globaux restant à traiter |
| P0-004 | Confirmations expirantes et contacts désambiguïsés | CTO | Implémenté, appareil requis | `6047ea1`, 14 tests transaction/expiration/choix |
| P0-005 | Langue unique et cohérente | CTO | Prochain WIP | tests UI/service/STT/TTS |
| P0-006 | TalkBack/TTS et audio focus coordonnés | Accessibilité+CTO | À faire | 30 scénarios sans double parole |
| P0-007 | Contrat offline prouvé en mode avion | QA | À faire | rapport cold/warm + moteurs |
| P0-008 | CVE, API 36, CI, signature, licences et modèles sécurisés | CTO/Sécurité | À faire | scan, SBOM, build propre |
| P0-009 | Tests instrumentés et matrice appareil | QA | À faire | androidTest + checklist 30/30 |
| P0-010 | APK release terrain signée et manifestée | CTO+COO | À faire | APK, SHA-256, rapport, double GO |

WIP maximal : un item principal à la fois. Ordre permanent : sécurité utilisateur → fonction centrale bloquée → accessibilité → vérité offline/confidentialité → stabilité → performance/taille → nouvelles fonctions.

## 9. Gouvernance documentaire

- `PLAN_ACTION_VOXIA.md` : direction, ordre, gates, métriques et backlog actif.
- `docs/FONCTIONS_REELLES.md` : inventaire factuel du produit au dernier commit vérifié.
- `CHANGELOG.md` : évolution des versions livrées ; entrées courtes avec changements, sécurité, preuves et limites.
- `CODEX_EVOLUTION_VOXIA.md` : archive historique figée des sessions de juillet 2026.
- `docs/REGISTRE_DECISIONS.md` : décisions irréversibles ou structurantes.
- `docs/REGISTRE_RISQUES.md` : risques ouverts et réduction.
- `evaluation/` et protocoles : preuves et méthodes spécialisées.

Une information présente dans plusieurs documents doit être détaillée dans un seul et seulement référencée ailleurs.

## 10. Prochaine action

Traiter P0-005 : créer une source de vérité unique pour la langue et synchroniser service, STT, TTS et interface. Ne conserver l'anglais que si l'état, les ressources et les parcours testés restent cohérents ; sinon le désactiver explicitement pour cette version.
