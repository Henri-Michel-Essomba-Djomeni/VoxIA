# VOXIA — fonctions réelles

**Statut :** inventaire factuel autoritaire
**Dernière vérification :** 26 août 2026
**Commit applicatif de référence :** `f4777f0`
**Version source :** `0.1.1-alpha-internal`

Statuts :

- **Fonctionnel code** : implémenté et couvert par au moins une vérification automatisée ou de build.
- **Partiel** : présent mais non fiable, non complet ou non validé sur appareil.
- **Absent** : non livré ou seulement simulé.

« Offline conditionnel » signifie qu'un moteur, pack ou modèle doit avoir été installé/téléchargé avant le passage en mode avion.

## Capacités

| Domaine | Fonction | Statut | Offline | Preuve actuelle | Limite principale |
|---|---|---|---|---|---|
| Voix | Écoute ponctuelle FR | Partiel | Conditionnel | politique FR-only + Android `SpeechRecognizer` + tests | fournisseur système possiblement réseau ; aucun test appareil |
| Voix | Synthèse vocale FR | Partiel | Conditionnel | politique FR-only + focus audio + tests de politiques | dépend du moteur/pack ; validation appareil requise |
| Voix | Anglais | Absent V0 | Non | demande explicitement refusée sans mutation | localisation complète reportée après V0 offline |
| Voix | Wake word | Absent | Non | stub retournant toujours faux | aucun modèle livré |
| Intentions | Classification locale | Partiel | Oui | règles Kotlin + tests unitaires | mots isolés trop permissifs ; harnais Python différent du moteur livré |
| Intentions | Modèle TFLite | Absent | Non | aucun fichier livré | ne pas annoncer de modèle entraîné |
| Vision | Étiquetage générique | Partiel | Oui | ML Kit embarqué | non spécialisé, erreurs et absence d'objet parfois confondues |
| Vision | OCR latin | Partiel | Oui | ML Kit + contrôle qualité + test exception d'initialisation | aucun guidage temps réel ni test caméra réel |
| Vision | Lecture segmentée | Fonctionnel code | Oui | session, boutons et tests de logique | aucun test appareil/TalkBack, pas de pause/reprise native |
| Vision | Codes-barres | Partiel | Oui | scanner ML Kit branché | catalogue local vide hors en-tête |
| Vision | Produit sourcé | Partiel | Oui | garde-fous catalogue testés | aucune fiche produit terrain |
| Vision | Abstention financière | Fonctionnel code | Oui | règles et tests synthétiques | pas de test caméra réel ; aucune reconnaissance de valeur |
| Vision | Modèle YOLO VOXIA | Absent | Non | test Python échoue sans modèle | 10 tests ignorés ; benchmark simulé |
| Traduction | OCR puis traduction | Partiel | Conditionnel | ML Kit Language ID/Translate | modèle à télécharger ; erreurs trop génériques ; pas de contrôle qualité image |
| Actions | Appel via composeur | Partiel | Oui | choix explicite + confirmation expirante + 14 tests | validation contacts/appareil réelle absente |
| Actions | Ouvrir application | Partiel | Oui | launcher Android + confirmation | correspondance approximative |
| Actions | Alarmes/minuteurs | Partiel | Oui | intents Android + confirmation | dépend de l'application système |
| Actions | Volume/date/heure/batterie | Fonctionnel code | Oui | API Android | non validé dans une campagne appareil |
| Actions | Notifications | Partiel | Oui | listener système | 30 éléments en RAM sans TTL/purge ; lecture sensible |
| Export | Copier/partager OCR | Partiel | Oui | avertissement + confirmation/chooser | presse-papiers et application cible hors contrôle |
| Permissions | Explication avant demande | Partiel | Oui | rationales + file d'actions testée | parcours refus/retour non instrumentés |
| Accessibilité | Contraste et cibles | Fonctionnel statique | Oui | contrastes explicites AAA, boutons 56–72 dp | thème/focus runtime non mesurés |
| Accessibilité | TalkBack et statuts | Partiel | Oui | propriétaire d'annonce explicite + fallback TTS + tests | absence de double parole non encore prouvée sur téléphone réel |
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

1. Le découplage caméra/microphone et la file d'actions sont implémentés, mais restent à valider sur appareil, refus de permission et rotation.
2. Les sorties terminales STT vide, TTS en erreur et OCR à l'initialisation sont corrigées en JVM ; les timeouts globaux restent incomplets.
3. La politique FR-only élimine la divergence connue, mais le comportement moteur doit encore être validé sur appareil.
4. Confirmations et contacts disposent maintenant de jetons, délais et désambiguïsation, mais le parcours doit être validé sur appareil avec carnet réel.
5. La duplication connue TalkBack/TTS est supprimée dans le code ; le scénario FIELD-029 et les interruptions audio restent à valider sur téléphone réel.
6. OkHttp/Okio transitifs comportent des avis de sécurité à neutraliser et vérifier.
7. `targetSdk 34` ne satisfait pas l'exigence Play applicable aux nouvelles versions à partir du 31 août 2026.

## Vérifications exécutées

| Vérification | Résultat |
|---|---|
| `gradlew test` | succès, 76 cas distincts, 152 exécutions, 0 échec, 0 ignoré |
| `gradlew lintDebug` | succès, 0 erreur, 12 avertissements GradleDependency |
| `gradlew assembleDebug` | succès, APK 118 527 551 octets |
| `gradlew assembleRelease` | lot précédent uniquement : APK signé v2 de 103 394 179 octets, non reconstruit depuis `f4777f0` |
| `gradlew bundleRelease` | lot précédent uniquement : AAB 47 558 559 octets, non reconstruit depuis `f4777f0` |
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
