# VOXIA — Stratégie de transformation en application utilisable

## 1. Objectif de livraison

Transformer le prototype actuel en une application Android utilisable sur un téléphone Android 10 ou supérieur, avec un parcours principal qui ne dépend pas d'une clé privée, d'un modèle absent ou d'un service cloud obligatoire.

La livraison doit fournir :

- un APK signé et vérifié ;
- un bouton de commande vocale toujours utilisable, même sans mot d'activation ;
- une reconnaissance vocale sur l'appareil lorsqu'Android la fournit, avec repli vers le moteur système ;
- une synthèse vocale français/anglais ;
- une vision générique embarquée, un OCR embarqué et la lecture de codes-barres ;
- des commandes système réellement exécutées ;
- une interface accessible et un état visible ;
- des tests automatiques et un rapport de vérification reproductible.

Une garantie absolue sur tous les téléphones n'est pas techniquement possible sans campagne matérielle. Les critères de sortie sont donc observables et testables, et chaque dépendance externe dispose d'un repli local ou d'un message explicite.

## 2. Principes d'architecture

### 2.1 Parcours sans dépendance secrète

Le mot d'activation Picovoice reste optionnel. S'il manque une AccessKey ou un fichier `.ppn`, aucune simulation automatique ne doit se produire. Le bouton principal « Parler » devient le parcours garanti.

### 2.2 Audio sérialisé

Le microphone ne peut appartenir qu'à un composant à la fois :

1. suspendre le wake word ;
2. prononcer l'invite ;
3. attendre la fin du TTS ;
4. démarrer le STT ;
5. traiter la commande ;
6. prononcer la réponse ;
7. reprendre le wake word.

### 2.3 Vision en cascade

Le pipeline de vision est composé de briques indépendantes :

1. capture CameraX avec rotation correcte ;
2. étiquetage d'image ML Kit embarqué (400+ concepts génériques) ;
3. OCR latin embarqué ;
4. code-barres embarqué ;
5. fusion des informations et description vocale ;
6. point d'extension pour un futur modèle produit TFLite personnalisé.

Ce pipeline privilégie un résultat utile immédiat. Un code-barres ou le texte d'une étiquette fournit souvent une identification produit plus précise qu'une classe visuelle générique.

### 2.4 Compréhension déterministe et testable

Le classifieur doit :

- normaliser accents, apostrophes, ponctuation et casse ;
- reconnaître des phrases complètes avant les mots isolés ;
- éviter les correspondances par sous-chaîne ;
- extraire les entités : contact, application, expression, heure, durée et langue ;
- retourner une justification et une confiance cohérente ;
- conserver un fallback explicite.

Le vocabulaire devient une ressource réellement chargée ou une taxonomie déclarative, et non un fichier mort.

## 3. Fonctions livrées

### Voix et interface

- parler par bouton ;
- mot d'activation optionnel correctement configuré ;
- français et anglais ;
- affichage de l'état, transcription et réponse ;
- annulation et répétition ;
- retours haptiques et messages d'erreur exploitables.

### Assistant

- heure, date et batterie ;
- volume plus/moins ;
- appel sécurisé via le composeur avec confirmation utilisateur ;
- ouverture d'une application installée ;
- calcul arithmétique local ;
- alarmes et minuteurs via les intents Android ;
- rappels via calendrier ou alarme de repli ;
- lecture des notifications après autorisation explicite ;
- aide, histoire, blague, motivation et identité.

### Vision

- identifier l'image avec plusieurs libellés et scores ;
- décrire une scène ;
- lire un document ;
- traduire un texte reconnu ;
- lire code-barres/QR et présenter la valeur ;
- fusionner texte, code et libellés pour un produit.

## 4. Taxonomie produits cible

La taxonomie est hiérarchique et extensible :

- alimentation et boissons ;
- fruits, légumes, céréales et produits locaux ;
- hygiène et cosmétiques ;
- entretien ménager ;
- santé et dispositifs médicaux, sans diagnostic ;
- vêtements et accessoires ;
- téléphones, électronique et énergie ;
- argent, documents, cartes et moyens de paiement ;
- outils, agriculture et bricolage ;
- mobilité, signalisation et obstacles ;
- mobilier et objets domestiques.

Chaque produit de catalogue devra pouvoir stocker : identifiant, GTIN/code-barres, catégorie, sous-catégorie, marque, nom, variante, quantité, unité, synonymes FR/EN, mots locaux, allergènes et avertissements. Les prix ne doivent pas être embarqués comme vérité permanente : ils nécessitent une source datée.

## 5. Données et modèle personnalisé futur

Le modèle générique embarqué est immédiatement fonctionnel mais ne promet pas la reconnaissance de chaque SKU. Pour un modèle VOXIA local :

- MVP 50 classes : 25 000 à 50 000 images annotées ;
- version robuste 150–250 classes : 120 000 à 500 000 instances ;
- séparation train/validation/test par lieu, sujet et téléphone ;
- diversité de lumière, occlusion, arrière-plan, angle et emballage ;
- métriques obligatoires : mAP50-95, précision, rappel, F1 et latence par appareil ;
- aucun seuil marketing ne doit être publié sans jeu de test gelé.

Pour les références emballées, le code-barres et le catalogue sont prioritaires afin de passer de centaines de classes visuelles à des milliers de produits.

## 6. Sécurité et confidentialité

- permissions demandées au moment de l'usage ;
- `ACTION_DIAL` préféré à l'appel direct ;
- confirmation des contacts ambigus ;
- aucune clé présentée comme secrète ne doit être supposée protégée dans l'APK ;
- sauvegarde Android désactivée pour les données sensibles ;
- notification permanente claire pendant l'écoute ;
- aucune journalisation du contenu complet OCR, contact ou notification en release ;
- écran expliquant caméra, microphone, contacts et notifications.

## 7. Tests et critères de sortie

### Tests automatiques

- tests unitaires du normaliseur, classifieur, extraction d'entités et calculatrice ;
- tests des messages et descriptions vision ;
- tests d'intégration du routage des intentions ;
- test Lint sans erreur ;
- compilation debug et release ;
- vérification de signature et contenu de l'APK.

### Tests matériels à exécuter avant diffusion publique

- Android 10, 12, 14 et une version récente ;
- téléphone avec et sans reconnaissance vocale hors ligne ;
- caméra en portrait/paysage ;
- faible luminosité et bruit ambiant ;
- autorisations acceptées, refusées puis réactivées ;
- redémarrage, arrière-plan et économie de batterie ;
- TalkBack et grandes tailles de police.

### Critères bloquants

- aucun crash au démarrage ;
- parcours bouton Parler utilisable sans Picovoice ;
- aucune activation simulée ;
- résultat ou erreur parlée pour chaque commande ;
- aucune fonction annoncée comme disponible si elle est un stub ;
- APK signé, installable et analysé par Lint ;
- limites du modèle et dépendances externes documentées.

## 8. Ordre d'implémentation

1. Manifest, permissions et service.
2. Orchestration audio et bouton push-to-talk.
3. Classifieur, entités et commandes système.
4. CameraX, image labeling, OCR, traduction et code-barres.
5. Interface accessible et liaison aux états.
6. Tests, Lint, optimisation et dépendances.
7. Build, signature, vérification APK et guide d'installation.

