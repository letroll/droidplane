# Directives de gestion des dépendances Android

## Règle de gestion des bibliothèques (Gradle Version Catalog)

1. **Utilisation obligatoire du Catalogue** :
    - Toutes les dépendances Gradle doivent être déclarées et consommées à travers le catalogue de versions (`gradle/libs.versions.toml`).
    - Ne JAMAIS écrire de dépendances en dur avec chaîne de caractères (ex: `implementation("com.example:lib:1.0")`) dans les fichiers `build.gradle.kts`.

2. **Ajout de nouvelles dépendances** :
    - Si une nouvelle bibliothèque est nécessaire pour implémenter une fonctionnalité et qu'elle n'est pas encore présente dans `libs.versions.toml` :
        1. Déclarer la version sous la section `[versions]`.
        2. Déclarer la bibliothèque sous la section `[libraries]`.
        3. Importer la référence générée dans le fichier `build.gradle.kts` correspondant (ex: `implementation(libs.nom.de.la.lib)`).

3. **Format du catalogue** :
    - Respecter le nommage camelCase pour les clés d'alias dans `libs.versions.toml`.