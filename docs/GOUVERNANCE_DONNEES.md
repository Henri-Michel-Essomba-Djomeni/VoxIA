# Gouvernance des données — VOXIA

## Principes

- Consentement explicite, compréhensible et révocable.
- Aucune donnée personnelle dans les datasets par défaut.
- Pseudonymisation des participants et séparation des coordonnées.
- Test gelé séparé des données utilisées pour régler règles ou modèles.
- Suppression sur demande.
- Conservation limitée et documentée.
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
