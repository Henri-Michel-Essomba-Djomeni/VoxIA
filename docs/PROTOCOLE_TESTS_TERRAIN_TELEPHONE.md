# Protocole de tests terrain téléphone réel — VOXIA

Date : 2026-07-22
Portée : validation manuelle et semi-automatisée d'une build VOXIA sur appareil Android réel, sans modifier `PLAN_ACTION_VOXIA.md`.

## Décision courte

Un émulateur suffit pour préparer et déboguer une partie du parcours, mais il ne suffit pas pour valider VOXIA en conditions terrain. Pour une décision pilote sérieuse, il faut au minimum un vrai téléphone Android ARM64.

## Pourquoi un vrai téléphone est nécessaire

Les risques centraux de VOXIA dépendent du matériel et de l'usage réel :

- microphone, accent, bruit ambiant et moteur `SpeechRecognizer` réellement disponible ;
- caméra, autofocus, faible lumière, reflets, petits caractères et mouvements de main ;
- haut-parleur, volume, latence TTS et compréhension audio ;
- TalkBack, grandes polices, gestes tactiles et ergonomie réelle ;
- permissions Android selon constructeur, version OS et surcouches ;
- contacts, notifications, chooser Android, presse-papiers et applications réellement installées ;
- performance, chauffe, mémoire, batterie et taille installée sur appareil ARM64.

## Ce que l'émulateur couvre correctement

- compilation, installation et lancement de base ;
- rendu UI général et régressions évidentes ;
- logique d'intentions, confirmations, navigation de lecture OCR et messages d'abstention ;
- scénarios de permissions simples ;
- tests rapides avant d'utiliser un téléphone.

L'émulateur reste donc utile en développement, mais il ne remplace pas le test final sur téléphone.

## Matrice minimale recommandée

Pour une toute première passe :

- 1 téléphone Android réel ARM64, Android 10/API 29 ou supérieur ;
- TalkBack disponible ;
- caméra arrière fonctionnelle ;
- microphone fonctionnel ;
- espace libre suffisant pour installer l'APK debug ou release.

Pour une passe professionnelle avant pilote :

- 1 téléphone bas ou milieu de gamme Android 10/11 ;
- 1 téléphone Android 14 ;
- 1 téléphone Android 16 pour la cible API 36 et les comportements récents ;
- 1 téléphone d'une surcouche constructeur courante ;
- tests en français avec au moins deux accents et deux environnements sonores.

## Préparation du téléphone

1. Utiliser un téléphone de test, pas un téléphone personnel contenant des données sensibles.
2. Activer les options développeur et le débogage USB.
3. Brancher le téléphone et accepter l'empreinte RSA de l'ordinateur.
4. Créer un ou deux contacts de test non personnels si les scénarios contacts sont vérifiés.
5. Préparer des documents non sensibles : texte imprimé, ticket fictif, boîte produit de test, page avec petits caractères.
6. Éviter les billets, cartes bancaires, documents médicaux ou administratifs réels.
7. Noter modèle, Android, langue système, TalkBack actif/inactif, taille de police et environnement sonore.

## Smoke test assisté par script

Construire l'APK debug :

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat assembleDebug
```

Lister les appareils branchés :

```powershell
.\scripts\run_phone_smoke.ps1 -ListOnly
```

Si PowerShell bloque l'exécution des scripts :

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\scripts\run_phone_smoke.ps1 -ListOnly
```

Installer, lancer et capturer le contexte :

```powershell
.\scripts\run_phone_smoke.ps1 -InstallApk -Launch -CollectLogcat
```

Les preuves locales sont écrites dans `evaluation/field/reports/`. Ce dossier est ignoré par Git, car il peut contenir des informations d'appareil ou de logs.

## Checklist manuelle

Utiliser `evaluation/field/checklists/phone_real_smoke.csv` comme grille d'exécution. Chaque ligne doit recevoir :

- `status` : `pass`, `fail`, `blocked` ou `not_run` ;
- `evidence` : chemin vers capture, log local ou note courte ;
- `notes` : observation utile, sans donnée personnelle.

## Critères de sortie

Une passe téléphone est acceptable seulement si :

- l'application s'installe et se lance sans crash ;
- les permissions sont expliquées avant demande système ;
- un refus de permission ne déclenche pas d'action différée ;
- les actions sensibles demandent confirmation ;
- l'OCR long est navigable par segment ;
- copie/partage affichent un avertissement ou demandent confirmation ;
- l'argent, les billets et objets financiers provoquent une abstention ;
- un produit non présent dans le catalogue reste annoncé comme inconnu ;
- TalkBack peut atteindre les contrôles principaux ;
- aucune transcription brute, OCR brut ou réponse utilisateur sensible n'apparaît dans les logs release.

Ces critères couvrent les gates appareil, accessibilité et release du plan directeur. Ajouter obligatoirement :

- démarrage à froid et à chaud en mode avion ;
- disponibilité réelle des moteurs STT/TTS locaux et absence de fallback réseau silencieux ;
- action sur chaque bouton avant puis après connexion du service ;
- erreur STT vide, erreur TTS et erreur d'initialisation OCR avec retour à un état utilisable ;
- timeout d'une confirmation et annulation d'une action différée ;
- désambiguïsation de deux contacts proches ;
- changement de langue sans divergence UI/STT/TTS ;
- TalkBack actif pendant les réponses VOXIA, sans double parole ;
- audio focus avec appel/notification/musique ;
- police 200 %, taille d'affichage maximale, portrait et paysage.

Une release terrain exige une checklist entièrement renseignée : aucun cas critique ne peut rester `not_run`.

## Sortie attendue

Après test réel, conserver les preuves brutes locales sous `evaluation/field/reports/`. Produire ensuite un résumé anonymisé et validé sous `evaluation/field/summaries/`, puis le lier depuis le changelog et les registres :

- appareil et OS ;
- build testée et commit ;
- nombre de cas pass/fail/blocked ;
- incidents critiques ;
- risques mis à jour ;
- décision : continuer, corriger avant pilote, ou bloquer.
