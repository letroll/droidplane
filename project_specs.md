# Specifications projet de l'application Droidplane 

## Aperçu du projet
Une application de mindmapping qui permet aux utilisateurs de gérer des mindmaps au format du logiciel freeplane.

## Pile technologique

- Kotlin
- Jetpack compose
- Android xml

## Caractéristiques principales

### 1. Visualisation de mindmap

- Visualisation des `nodes` de la mindmap
- Affichage des nodes avec leur personnalisation de style

### 2. Gestion des nodes

- Création, lecture, mise à jour et suppression d'événements (CRUD)

- Propriétés des nodes :
  - description
  - date de création et de modification

- Actions sur les nodes :
  - copie de la description
  - accès aux nodes liés
  

## Structure du projet

### Briques logicielles 

Le projet contient les modules core, data et app. Qui ont respectivement les responsabilitées suivantes:

 - core : le code générique pouvant être ré-employer dans d'autres projets
 - data : le code propre au modèle du projet et à leur gestion
 - app: le code spécifique au projet ne rentrant pas dans les catégories précédentes.

### Composants frontaux

1. **Toolbar principal**

   - Elle comprend :
     - l'icon de l'application suivi du titre du node courrant
     - un icon de loupe pour la recherche 
     - un bouton d'accès aux menus supplémentaires.
   - Un clic sur un des enfants, mets à jour le titre de la barre par la descritpion de cette enfant
   - Un clic sur le bouton de recherche met à jour l'apparence de la bar vers le mode de recherche de l'application, 
     représenté par une flèche de retour permettant le retour au mode d'"affichage classique de la barre. 
     Ce mode inclus aussi un champ texte de recherche ainsi que des boutons de défilement vers les résultats précédents et suivants de la recherche.
     Et finalement le bouton d'accès vers les options supplèmentaires du menu.
   - Un clic sur le bouton de mnenu, affiche les autres option du menu à savoir: Up, Top, Open, Help, Save

2. **Vue de listing des enfants du node courant**

   - une liste de chaque node enfant du node courant
   - un node enfant est représenté par une cardview contenant :
     - les éventuelles icons associés au node enfant
     - la description dont le texte est personnalisé par l'aspect associé lorsque nécessaire (Bold,Italic)
     - un chevron présent si l'enfant à lui même des enfants
   - un clic sur un des nodes enfants met à jour la barre principal et remplace le listing courant par celui des enfants du node précédemment cliqué.