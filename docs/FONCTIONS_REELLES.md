# VOXIA — fonctions réelles

**Statut :** inventaire factuel autoritaire
**Dernière vérification :** 20 août 2026
**Commit de référence :** `b2fe2d461f13637af378356a8ce0839e0e7c49f0`
**Version source :** `0.1.0-alpha-internal`

Statuts :

- **Fonctionnel code** : implémenté et couvert par au moins une vérification automatisée ou de build.
- **Partiel** : présent mais non fiable, non complet ou non validé sur appareil.
- **Absent** : non livré ou seulement simulé.

« Offline conditionnel » signifie qu'un moteur, pack ou modèle doit avoir été installé/téléchargé avant le passage en mode avion.

## Capacités

| Domaine | Fonction | Statut | Offline | Preuve actuelle | Limite principale |
|---|---|---|---|---|---|
| Voix | Écoute ponctuelle | Partiel | Conditionnel | Android `SpeechRecognizer` branché | fournisseur système possiblement réseau ; résultat vide pouvant bloquer l'état |
| Voix | Synthèse vocale | Partiel | Conditionnel | Android `TextToSpeech` branché | dépend du moteur/pack ; collision TalkBack et erreur TTS non terminale |
| Voix | Wake word | Absent | Non | stub retournant toujours faux | aucun modèle livré |
| Intentions | Classification locale | Partiel | Oui | règles Kotlin + tests unitaires | mots isolés trop permissifs ; harnais Python différent du moteur livré |
| Intentions | Modèle TFLite | Absent | Non | aucun fichier livré | ne pas annoncer de modèle entraîné |
| Vision | Étiquetage générique | Partiel | Oui | ML Kit embarqué | non spécialisé, erreurs et absence d'objet parfois confondues |
| Vision | OCR latin | Partiel | Oui | ML Kit + contrôle qualité post-capture | initialisation fragile ; aucun guidage temps réel |
| Vision | Lecture segmentée | Fonctionnel code | Oui | session, boutons et tests de logique | aucun test appareil/TalkBack, pas de pause/reprise native |
| Vision | Codes-barres | Partiel | Oui | scanner ML Kit branché | catalogue local vide hors en-tête |
| Vision | Produit sourcé | Partiel | Oui | garde-fous catalogue testés | aucune fiche produit terrain |
| Vision | Abstention financière | Fonctionnel code | Oui | règles et tests synthétiques | pas de test caméra réel ; aucune reconnaissance de valeur |
| Vision | Modèle YOLO VOXIA | Absent | Non | test Python échoue sans modèle | 10 tests ignorés ; benchmark simulé |
| Traduction | OCR puis traduction | Partiel | Conditionnel | ML Kit Language ID/Translate | modèle à télécharger ; erreurs trop génériques ; pas de contrôle qualité image |
| Actions | Appel via composeur | Partiel | Oui | `ACTION_DIAL` + confirmation | contact ambigu, confirmation sans expiration |
| Actions | Ouvrir application | Partiel | Oui | launcher Android + confirmation | correspondance approximative |
| Actions | Alarmes/minuteurs | Partiel | Oui | intents Android + confirmation | dépend de l'application système |
| Actions | Volume/date/heure/batterie | Fonctionnel code | Oui | API Android | non validé dans une campagne appareil |
| Actions | Notifications | Partiel | Oui | listener système | 30 éléments en RAM sans TTL/purge ; lecture sensible |
| Export | Copier/partager OCR | Partiel | Oui | avertissement + confirmation/chooser | presse-papiers et application cible hors contrôle |
| Permissions | Explication avant demande | Partiel | Oui | rationales présentes | parcours refus/retour non instrumentés |
| Accessibilité | Contraste et cibles | Fonctionnel statique | Oui | contrastes explicites AAA, boutons 56–72 dp | thème/focus runtime non mesurés |
| Accessibilité | TalkBack et statuts | Partiel | Oui | libellés et live regions présents | double parole probable, 0 test réel |
| Accessibilité | Police 200 % | Partiel | Oui | tailles `sp`, hauteurs adaptatives | grille à trois colonnes non testée |
| Accessibilité | Haptique | Absent | Oui | permission déclarée seulement | aucune utilisation dans le code |

## Architecture et données

- Application Android monolithique locale : `MainActivity` → `VoiceAssistantService` → voix/intents/vision/actions.
- Aucun backend VOXIA, compte, API privée, base de données ou synchronisation.
- Dernier OCR, notifications et confirmations restent en mémoire.
- Aucun historique persistant.
- Aucun secret ou token API détecté dans le dépôt/historique lors de l'audit.
- Les modèles de traduction ML Kit peuvent être téléchargés à la demande.

L'absence de backend réduit la surface d'attaque mais ne prouve pas un fonctionnement entièrement hors ligne : STT, TTS et traduction dépendent de l'état du téléphone et des packs installés.

## Problèmes P0 confirmés

1. Les fonctions caméra et plusieurs boutons peuvent ne rien faire tant que le service vocal n'est pas lié ou si le microphone est refusé.
2. STT peut rester en écoute après un résultat vide.
3. Une erreur TTS peut empêcher le callback attendu et bloquer la séquence.
4. L'initialisation OCR utilise un `future.get()` insuffisamment protégé.
5. L'état de langue peut diverger entre service, STT, TTS et interface.
6. Les confirmations et demandes de contact n'expirent pas.
7. La recherche contact prend le premier résultat partiel sans désambiguïsation.
8. TalkBack et le TTS VOXIA peuvent annoncer simultanément la même réponse.
9. OkHttp/Okio transitifs comportent des avis de sécurité à neutraliser et vérifier.
10. `targetSdk 34` ne satisfait pas l'exigence Play applicable aux nouvelles versions à partir du 31 août 2026.

## Vérifications exécutées

| Vérification | Résultat |
|---|---|
| `gradlew test` | succès, 35 cas distincts, 70 exécutions, 0 échec |
| `gradlew lintDebug` | succès, 0 erreur, 12 avertissements GradleDependency |
| `gradlew assembleDebug` | succès, APK 118 443 615 octets |
| `gradlew bundleRelease` | succès local, AAB 47 558 559 octets |
| Import FLEURS | 2/2 tests réussis |
| Test Vision Python | 14 pass, 10 skipped, 1 fail |
| Évaluateurs Intent/STT/OCR | exécutables, mais seulement sur un exemple factice chacun |
| Tests instrumentés Android | absents |
| Checklist téléphone | 0/30 exécuté |

Les tailles varient selon le build ; le manifest de chaque future release doit contenir commit, hash, signature et taille réels.

## Interdictions de communication

Ne pas annoncer comme validés :

- fonctionnement totalement hors ligne ;
- compatibilité TalkBack ;
- précision STT/OCR/Vision/intentions ;
- identification fiable de produits, monnaie ou médicaments ;
- performance, autonomie ou stabilité ;
- disponibilité publique.

Une capacité passe de « Partiel » à « Fonctionnel validé » seulement lorsque sa gate dans `PLAN_ACTION_VOXIA.md` est franchie et que la preuve est archivée dans `evaluation/` ou un manifest de release.
