# Session terrain partielle — Xiaomi Android 15 — 2026-08-26

## Build et environnement

- Commit testé : `e03691b`
- Version : `0.1.1-alpha-internal` (`versionCode` 4)
- APK debug installé : 118 588 967 octets, SHA-256 `F57346BE7396A55AAA8CA7336EB7CC8D8CFDDB6959FBDE086DF689F19743062F`
- Appareil : Xiaomi `25100RA69G`, Android 15/API 35, ARM64
- Langue : français
- Bluetooth désactivé pour le contrôle audio
- Identifiant ADB exclu de cette synthèse

## Résultats

| Cas | Résultat | Preuve synthétique |
|---|---|---|
| FIELD-001 | pass | APK installé, paquet `com.voxia.assistant` lancé sans crash, version installée conforme |
| FIELD-017 | pass | « Quelle heure est-il ? » reconnu ; intention horaire exécutée ; une seule réponse vocale correcte ; retour à `IDLE` sans faux « Je n'ai pas compris » |

Totaux de la checklist après cette session : **2 pass, 0 fail, 0 blocked, 28 not_run**.

La trace Android du contrôle FIELD-017 montre la séquence suivante :

1. prompt « Oui ? » et état `SPEAKING` ;
2. état `LISTENING` ;
3. repli du moteur local vers le moteur système après erreur 13 ;
4. états `PROCESSING`, puis `SPEAKING` pour la réponse horaire ;
5. retour à `IDLE`, sans callback d'erreur STT tardif.

## Limites et décision

- Le repli vers le moteur système ne prouve pas le fonctionnement STT hors ligne. FIELD-021 reste obligatoire avec le pack français préinstallé et le mode avion.
- TalkBack était inactif pendant ce contrôle. FIELD-015 et FIELD-029 restent à exécuter.
- Les autres scénarios de permissions, caméra, OCR, actions sensibles, grande police, audio focus et reprise restent à exécuter.
- Cette session ne ferme aucun gate global. Elle valide uniquement FIELD-001 et FIELD-017 sur cette configuration.
- Le rebuild debug ultérieur mesure 118 527 551 octets et n'est pas l'artefact utilisé pendant cette session ; les archives debug ne sont pas considérées reproductibles octet pour octet.

Décision : **poursuivre la validation terrain**, en commençant par TalkBack et l'absence de double parole.

Responsabilités de cette session :

- exécution et confirmation perceptive : opérateur du téléphone ;
- validation technique : mainteneur VOXIA assisté par revue de code indépendante ;
- approbation de gate : non demandée, aucun gate n'étant fermé.
