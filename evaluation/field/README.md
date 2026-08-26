# Évaluation terrain — VOXIA

Ce dossier prépare les tests téléphone réel. Les preuves locales générées pendant une session de test vont dans `reports/` et ne sont pas versionnées par défaut.

## Contenu

- `checklists/phone_real_smoke.csv` : 30 cas manuels prioritaires, liés aux gates du plan directeur.
- `reports/.gitkeep` : conserve le dossier de sortie local sans committer les rapports.
- `summaries/` : résumés anonymisés et validés, seuls éléments terrain destinés à être versionnés et liés aux gates.

## Règle

Les rapports terrain peuvent contenir des informations d'appareil, de logs ou de contexte utilisateur. Ne pas les committer. Extraire uniquement un résumé anonymisé, le faire valider selon la gouvernance, puis le placer dans `summaries/`. Une gate terrain ne peut pas être franchie avec un cas critique `not_run`.
