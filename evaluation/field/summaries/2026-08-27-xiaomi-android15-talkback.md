# Session terrain TalkBack — Xiaomi Android 15 — 2026-08-27

## Build et environnement

- Commit testé : `7689eeb`
- Version : `0.1.1-alpha-internal` (`versionCode` 4)
- APK debug installé : 118 543 935 octets, SHA-256 `C30AB06F283F10E4F374ED961E2280CD4ED676831E528B65525E61D3CE0EB217`
- Appareil : Xiaomi `25100RA69G`, Android 15/API 35, ARM64
- Langue : français
- TalkBack `17.0.1.926549743` actif ; niveau de détail réglé sur `Faible` après le premier parcours
- Identifiant ADB exclu de cette synthèse

## Incident reproduit et correction vérifiée

Avant `7689eeb`, une commande horaire était reconnue et la réponse TTS démarrait, puis l'annonce TalkBack immédiate de l'état `PROCESSING` prenait le focus audio (`AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK`) et arrêtait la réponse.

Le correctif :

- différencie les événements de focus dans une politique testable ;
- conserve l'interruption sur toute concurrence vocale afin d'éviter deux voix simultanées ;
- diffère l'annonce TalkBack de `PROCESSING` de 500 ms et l'annule dès qu'un nouvel état arrive ;
- garantit qu'un callback différé annulé ou remplacé ne peut plus parler.

Après réinstallation, « Quelle heure est-il ? » a produit une seule réponse horaire complète. L'annonce `PROCESSING` a été annulée avant le démarrage de la réponse, sans double parole.

## Résultats

| Cas | Résultat | Preuve synthétique |
|---|---|---|
| FIELD-015 | pass | Parcours réel avec TalkBack : contrôles principaux atteignables, nommés et ordonnés ; hiérarchie Android confirme les boutons standards focalisables et libellés |
| FIELD-029 | pass | Réponse horaire prononcée une seule fois ; bouton Annuler atteint avec TalkBack et arrêt STT confirmé deux fois ; callbacks Android tardifs `ERROR_CLIENT` ignorés sans message vocal |

Résultats de cette session : **2 pass, 0 fail, 0 blocked**. Totaux cumulés de la checklist : **4 pass, 0 fail, 0 blocked, 26 not_run**.

## Limites et décision

- Le comportement reste validé sur un seul appareil et une seule version TalkBack.
- Les appels, notifications, musique et autres interruptions audio restent à tester.
- La police à 200 %, le paysage et les parcours caméra restent à exécuter séparément.
- Aucun parcours de production ne conserve actuellement l'état `PROCESSING` plus de 500 ms : les opérations caméra asynchrones reviennent à `IDLE`. La sémantique de leurs états devra être corrigée et retestée avant de revendiquer un retour TalkBack pour traitement long.
- Cette session ne ferme ni P0-006 ni un gate global ; elle valide seulement FIELD-015 et FIELD-029 sur cette configuration.

Décision : **conserver le correctif et poursuivre la matrice d'interruptions audio et de mise en page**.

Responsabilités de cette session :

- exécution et confirmation perceptive : opérateur du téléphone ;
- validation technique : mainteneur VOXIA assisté par revue de code indépendante ;
- approbation de gate : non demandée, aucun gate n'étant fermé.
