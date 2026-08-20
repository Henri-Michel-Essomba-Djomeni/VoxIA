# Évaluation terrain — VOXIA

Ce dossier prépare les tests téléphone réel. Les preuves locales générées pendant une session de test vont dans `reports/` et ne sont pas versionnées par défaut.

## Contenu

- `checklists/phone_real_smoke.csv` : 30 cas manuels prioritaires, liés aux gates du plan directeur.
- `reports/.gitkeep` : conserve le dossier de sortie local sans committer les rapports.

## Règle

Les rapports terrain peuvent contenir des informations d'appareil, de logs ou de contexte utilisateur. Ne pas les committer sans anonymisation et validation de gouvernance. Une gate terrain ne peut pas être franchie avec un cas critique `not_run`.
