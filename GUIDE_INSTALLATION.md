# VOXIA alpha interne — Installation de baseline

Ce guide décrit l'installation de l'APK historique conservé dans le dépôt. Il sert aux tests internes et à la mesure de baseline, pas à une diffusion publique.

## APK de baseline

- Fichier : `VOXIA-1.0.0-release.apk`
- Application : `com.voxia.assistant`
- Version embarquée dans cet APK : `1.0.0` (`versionCode 2`)
- Version source courante : `0.1.0-alpha-internal`
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
- **LIRE TEXTE** : capture photo puis lecture OCR.
- **TRADUIRE** : OCR, identification de langue et traduction ML Kit si le modèle requis est disponible.
- Actions : ouvrir une application, préparer un appel dans le composeur, régler alarme/minuteur, lire date/heure/batterie, contrôler le volume.

## Limites connues

- Pas de modèle Vosk embarqué.
- Pas de YOLO VOXIA entraîné ni intégré dans l'application.
- Pas de `intent_classifier.tflite` livré.
- Pas de précision produit, OCR, STT ou Vision démontrée sur données réelles.
- La reconnaissance produit par apparence seule est générique et ne doit pas promettre marque, prix, composition ou allergènes.
- Le catalogue produit local est vide par défaut hors en-tête ; une fiche produit doit avoir une source et une date avant d'être annoncée.
- La traduction peut télécharger des modèles ML Kit lors du premier usage.
- Le cadrage caméra n'est pas encore guidé par une boucle qualité robuste.
- L'APK est lourd pour une distribution pilote ; l'objectif reste AAB, ABI et modules à la demande.

## Vérifications obligatoires avant tout pilote

- `./gradlew test`
- `./gradlew lintDebug`
- `./gradlew assembleDebug` ou build release signé selon le canal de test.
- Installation sur au moins un téléphone ARM64 réel.
- Test microphone en environnement calme et bruité.
- Test caméra : faible lumière, reflets, petits caractères, image inclinée et document partiellement coupé.
- Test permissions refusées puis accordées.
- Test TalkBack et tailles de police élevées.
- Vérification qu'aucun contenu utilisateur n'apparaît dans les logs release.
- Rapport de baseline dans `evaluation/`.

## Règle de publication

Cette baseline ne doit pas être appelée “version 1.0 prête”. Toute communication externe doit parler de prototype alpha interne tant que les critères du plan directeur ne sont pas franchis.

## Build source courant

Pour vérifier l'état source actuel sur cette machine Windows :

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat test
.\gradlew.bat lintDebug
.\gradlew.bat assembleDebug
```

L'APK debug généré se trouve dans `app/build/outputs/apk/debug/app-debug.apk`. Il sert au contrôle technique interne ; il ne remplace pas l'APK de baseline historique ni une release pilote signée.
