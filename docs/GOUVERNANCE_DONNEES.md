# Gouvernance des données — VOXIA

**Owner :** COO / Responsable données
**Dernière revue :** 20 août 2026
**Collecte active autorisée :** aucune collecte utilisateur en production

## Principes

- Consentement explicite, compréhensible et révocable.
- Aucune donnée personnelle dans les datasets par défaut.
- Pseudonymisation des participants et séparation des coordonnées.
- Test gelé séparé des données utilisées pour régler règles ou modèles.
- Suppression sur demande.
- Conservation limitée et documentée.
- Durée, emplacement, responsable et procédure de suppression approuvés avant toute collecte.
- Licence vérifiée pour chaque audio, image, texte, catalogue ou modèle.
- Pas d'utilisation des données de bêta pour l'entraînement sans consentement distinct.

## Données interdites dans les datasets

- Contacts personnels.
- Notifications privées.
- Documents administratifs non anonymisés.
- Images de visages sans nécessité et consentement spécifique.
- Informations médicales ou financières identifiantes.
- Audio contenant des tiers non consentants.

## Champs minimaux d'un dataset pilote

- `sample_id`
- `participant_id` pseudonymisé
- `consent_version`
- `created_at`
- `locale`
- `device_model`
- `os_version`
- `condition` ou `environment`
- vérité terrain
- sortie du système
- notes d'échec sans contenu sensible

## Traçabilité

Chaque rapport d'évaluation doit indiquer commit Git, version du moteur, paramètres, date, taille du dataset, sous-groupes mesurés et limites connues.

## Conservation et suppression

Avant une collecte pilote, produire une fiche approuvée contenant :

- finalité et base de consentement ;
- catégories de données et personnes ayant accès ;
- stockage chiffré et lieu de traitement ;
- durée maximale de conservation ;
- procédure de retrait du consentement et suppression ;
- délai de traitement d'une demande ;
- responsable opérationnel et contact ;
- décision séparée pour toute réutilisation d'entraînement.

Les coordonnées de participants ne doivent jamais être stockées avec les données d'évaluation. Les rapports versionnés doivent être anonymisés et ne contenir aucun audio, document, contact ou notification identifiable.
