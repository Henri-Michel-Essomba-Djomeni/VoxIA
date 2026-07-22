# Protocole de recherche utilisateur — VOXIA pilote

## Objectif

Comprendre si VOXIA répond à trois tâches prioritaires pour des personnes francophones, notamment aveugles ou malvoyantes, avant d'ajouter de nouveaux modèles.

## Participants

- Phase découverte : 10 à 15 participants.
- Inclure différents âges, niveaux d'aisance numérique, accents et téléphones.
- Définir une première zone géographique avant collecte large.

## Déroulé recommandé

1. Présenter le prototype comme alpha interne.
2. Expliquer les données collectées et obtenir le consentement.
3. Observer l'utilisateur sur Lire, Reconnaître et Agir sans le guider au début.
4. Noter formulations vocales, hésitations, erreurs de cadrage et abandons.
5. Demander ce qui a été utile, dangereux, incompréhensible ou absent.
6. Classer chaque problème par fréquence, gravité et valeur utilisateur.

## Mesures terrain

- Tâche réussie sans aide.
- Temps jusqu'à résultat utile.
- Nombre de reprises caméra.
- Besoin de correction ou répétition.
- Commande comprise ou non.
- Erreur qui aurait pu déclencher une mauvaise action.
- Compréhension de la réponse vocale.

## Sorties attendues

- Zone pilote écrite.
- Trois tâches prioritaires validées.
- Backlog trié par valeur et risque.
- Dataset pilote prêt pour `evaluation/`.
- Décision documentée sur le moteur STT à prototyper ensuite.

## Complément technique

La vérification appareil, permissions, TalkBack, OCR long, TTS, presse-papiers, partage et logs est détaillée dans `docs/PROTOCOLE_TESTS_TERRAIN_TELEPHONE.md`. Le protocole utilisateur observe les usages ; le protocole téléphone réel vérifie que la build se comporte correctement sur matériel Android.
