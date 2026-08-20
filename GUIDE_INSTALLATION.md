# VOXIA — build, installation et validation

**Dernière revue :** 20 août 2026
**Source courante :** `0.1.1-alpha-internal`
**Version cible :** `0.2.0-alpha-offline`

Ce guide distingue trois objets qui ne doivent jamais être confondus :

1. l'APK historique `VOXIA-1.0.0-release.apk` ;
2. les artefacts reconstruits depuis la source courante ;
3. la future release terrain hors ligne, qui n'existe pas encore.

## APK de baseline

- Fichier : `VOXIA-1.0.0-release.apk`
- Application : `com.voxia.assistant`
- Version embarquée dans cet APK : `1.0.0` (`versionCode 2`)
- Version source courante : `0.1.1-alpha-internal`
- Compatibilité visée : Android 10/API 29 ou supérieur
- Architectures : `arm64-v8a` et `x86_64`
- Taille : 104 310 991 octets, environ 99,5 Mio
- SHA-256 : `734382429F1055D033B976A96BD3A3D4FE45E6D7B0A8A39FD2DB263466298DBD`
- Signature : APK Signature Scheme v2, certificat VOXIA Release RSA 4096 bits

## Installation

1. Copier `VOXIA-1.0.0-release.apk` sur le téléphone de test.
2. Ouvrir le fichier avec le gestionnaire de fichiers Android.
3. Si Android le demande, autoriser temporairement l'installation depuis cette source.
4. Installer puis ouvrir l'application.
5. Accorder le microphone pour la commande vocale.
6. Accorder la caméra uniquement au moment d'utiliser Lire, Identifier, Scanner produit ou Traduire.
7. Accorder les contacts ou l'accès aux notifications uniquement pour les fonctions correspondantes.
8. Retirer l'autorisation d'installation depuis source inconnue après le test.

Android peut afficher un avertissement Play Protect, car l'APK est distribué hors Play Store. Vérifier l'empreinte SHA-256 avant installation.

## Fonctions à tester

- Bouton **PARLER À VOXIA** : reconnaissance vocale Android, avec repli possible vers le moteur système.
- **IDENTIFIER** : ML Kit Image Labeling, OCR et codes-barres.
- **SCANNER PRODUIT** : code-barres, recherche dans le catalogue local si une fiche sourcée existe, texte visible et catégorie probable ; sinon réponse explicite “produit inconnu”.
- **LIRE TEXTE** : capture photo puis lecture OCR segmentée ; après une lecture réussie, utiliser les boutons Précédent/Répéter/Suite/Plus lent/Normal/Plus vite/Copier/Partager ou dire “lis la suite”, “segment suivant”, “segment précédent”, “répète”, “lis plus vite”, “parle plus lentement”, “vitesse normale”, “copie le texte” ou “partage le texte”.
- **TRADUIRE** : OCR, identification de langue et traduction ML Kit si le modèle requis est disponible.
- Actions : ouvrir une application, préparer un appel dans le composeur, régler alarme/minuteur, lire date/heure/batterie, contrôler le volume.

## Limites connues

- Pas de modèle Vosk embarqué.
- Pas de YOLO VOXIA entraîné ni intégré dans l'application.
- Pas de `intent_classifier.tflite` livré.
- Pas de précision produit, OCR, STT ou Vision démontrée sur données réelles.
- La reconnaissance produit par apparence seule est générique et ne doit pas promettre marque, prix, composition ou allergènes.
- La reconnaissance d'argent ou de billets doit rester une abstention : VOXIA ne doit annoncer ni valeur de coupure ni authenticité.
- Le catalogue produit local est vide par défaut hors en-tête ; une fiche produit doit avoir une source et une date avant d'être annoncée.
- La traduction peut télécharger des modèles ML Kit lors du premier usage.
- Copier le texte OCR place le contenu dans le presse-papiers Android ; éviter cette action sur un document sensible pendant les tests partagés.
- Le cadrage caméra n'est pas encore guidé par une boucle qualité robuste.
- L'APK de baseline historique est lourd pour une distribution pilote ; la source courante génère désormais un AAB release en CI, mais la mesure de taille terrain et les modules à la demande restent à traiter.

## Vérifications obligatoires avant tout pilote

- `./gradlew test`
- `./gradlew lintDebug`
- `./gradlew assembleDebug`
- `./gradlew bundleRelease` ou build release signé selon le canal de test.
- Installation sur au moins un téléphone ARM64 réel.
- Test microphone en environnement calme et bruité.
- Test caméra : faible lumière, reflets, petits caractères, image inclinée et document partiellement coupé.
- Test permissions refusées puis accordées.
- Test TalkBack et tailles de police élevées.
- Vérification qu'aucun contenu utilisateur n'apparaît dans les logs release.
- Rapport de baseline dans `evaluation/`.

Le protocole détaillé de téléphone réel est dans `docs/PROTOCOLE_TESTS_TERRAIN_TELEPHONE.md`. Le script `scripts/run_phone_smoke.ps1` aide à détecter `adb`, lister un appareil, installer l'APK debug, lancer VOXIA et stocker les preuves locales dans `evaluation/field/reports/`. Si PowerShell bloque le script, utiliser `powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\scripts\run_phone_smoke.ps1 -ListOnly`.

## Règle de publication

Cette baseline ne doit pas être appelée “version 1.0 prête”. Toute communication externe doit parler de prototype alpha interne tant que les critères du plan directeur ne sont pas franchis.

## Build source courant

Pour vérifier l'état source actuel sur cette machine Windows :

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat test
.\gradlew.bat lintDebug
.\gradlew.bat assembleDebug
.\gradlew.bat bundleRelease
```

L'APK debug généré se trouve dans `app/build/outputs/apk/debug/app-debug.apk`. L'AAB release se trouve dans `app/build/outputs/bundle/release/app-release.aab` ; sa signature dépend de la présence d'un `keystore.properties` local. Ces artefacts servent au contrôle technique interne ; ils ne remplacent pas l'APK de baseline historique ni une release pilote signée.

Dernière vérification locale au 20 août 2026, commit applicatif `6047ea1` :

- 58 tests distincts réussis sur debug et release, soit 116 exécutions sans échec ;
- lint : 0 erreur, 12 avertissements de versions ;
- APK debug : 118 511 139 octets ;
- APK release minifié et signé v2 : 103 394 119 octets ;
- SHA-256 APK release : `89EEDF2EA690806A2BD2C1964559831E30618829AA333FCE0BE3E0602B108445` ;
- version embarquée : `0.1.1-alpha-internal` (`versionCode 4`).

Ces tailles ne sont pas des constantes. Le manifest d'une release doit toujours être généré depuis l'artefact exact.

## Préflight hors ligne obligatoire

Avant de qualifier une future APK de « hors ligne » :

1. installer l'APK et préparer explicitement les packs vocaux/modèles autorisés ;
2. noter le moteur STT et TTS actif ;
3. activer le mode avion ;
4. redémarrer l'application à froid puis à chaud ;
5. vérifier Lire, Identifier, Scanner produit, actions locales et messages d'erreur ;
6. vérifier que la traduction est annoncée indisponible si son modèle n'a pas été préchargé ;
7. vérifier qu'aucun fallback réseau silencieux ne se produit ;
8. exécuter TalkBack, police 200 %, refus de permissions et annulation ;
9. enregistrer appareil, OS, commit, version, hash APK et résultat.

La commande vocale offline n'est garantie que si Android fournit réellement un recognizer local compatible. Dans le cas contraire, l'interface tactile accessible doit rester utilisable et expliquer la limite.

## Signature, hash et rollback

Pour chaque release terrain :

- vérifier la signature de l'APK ;
- publier le SHA-256 ;
- conserver le commit et le manifest de tests ;
- tester installation fraîche et mise à jour depuis la version précédente ;
- conserver l'APK précédent signé comme rollback ;
- ne jamais utiliser l'APK historique 1.0.0 comme rollback de la future branche 0.2.0 sans test de compatibilité.
