# Ummati — Plateforme de bénévolat & gestion d'ONG

Ummati met en relation des bénévoles et des associations : découverte de missions,
inscriptions, gestion des membres, heures validées et attestations.

Ce fichier est le **point d'entrée** : il explique de quoi le projet est fait, ce
qu'il faut installer, et où aller ensuite. Les procédures détaillées vivent dans des
documents dédiés, listés plus bas.

---

## 1. De quoi le projet est fait

Trois livrables, **un seul code applicatif** pour l'interface :

```
                    ┌──────────────────────────┐
   Navigateur ─────▶│  ummati-front (Angular)  │
   iOS / Android ──▶│  + conteneur Capacitor   │
                    └────────────┬─────────────┘
                                 │ /api/v1/**
                    ┌────────────▼─────────────┐
                    │   ummati (Spring Boot)   │
                    └────────────┬─────────────┘
                                 │
                    ┌────────────▼─────────────┐
                    │       PostgreSQL         │
                    └──────────────────────────┘
```

| Dossier | Quoi | Langage |
|---|---|---|
| `ummati/` | API REST, règles métier, base de données | Java 21 / Spring Boot 4 |
| `ummati-front/` | Interface web **et** applications mobiles | Angular 21 / TypeScript |
| `ummati-front/android/`, `ios/` | Conteneurs natifs générés | Java / Swift |
| `deploy/` | Reverse proxy nginx | — |

Les applications mobiles **ne sont pas un second projet** : elles embarquent le même
bundle Angular. Une fonctionnalité ajoutée au front arrive dans les trois supports.

---

## 2. De quoi tu as besoin, selon ce que tu veux faire

C'est la question qui revient le plus souvent. Rien n'est obligatoire partout.

| Service externe | Pour quoi | Requis quand ? | Si absent |
|---|---|---|---|
| **Docker** | PostgreSQL + MailHog en local | Développement | Rien ne démarre |
| **Java 21 + Node 22** | Compiler back et front | Développement | Rien ne compile |
| **Serveur SMTP** | Emails réels (inscription, mot de passe) | Production | MailHog suffit en local |
| **Nom de domaine + HTTPS** | Liens des emails, CORS, liens profonds | Production | Impossible de publier |
| **Clés VAPID** | Notifications push **navigateur** | Optionnel | Notifications in-app + emails seulement |
| **Firebase (FCM)** | Notifications des **apps mobiles** | Optionnel, mobile | Apps fonctionnelles, sans notifications |
| **Android Studio** | Compiler l'app Android | Mobile Android | — |
| **Un Mac + Xcode** | Compiler l'app iOS | Mobile iOS | Aucune alternative sous Windows |

**À retenir :** le back et le front tournent en local avec seulement Docker, Java et
Node. Tout le reste est additionnel.

> ⚠️ Le backend **refuse de démarrer en profil `prod`** tant qu'un secret est resté à
> la valeur d'exemple du dépôt (`SecretsValidator`). C'est voulu : une variable
> oubliée provoque un échec explicite plutôt qu'un démarrage avec une clé publique.

---

## 3. Démarrer en local (première fois)

```powershell
# 1. Base de données  (le nom ummati_db est celui attendu par application-dev.yaml)
docker run --name ummati-postgres `
  -e POSTGRES_DB=ummati_db -e POSTGRES_USER=ummati -e POSTGRES_PASSWORD=ummati `
  -p 5433:5432 -d postgres:15

# 2. Faux serveur mail — aucun email réel n'est envoyé en local
docker run --name ummati-mailhog -p 1025:1025 -p 8025:8025 -d mailhog/mailhog

# 3. Backend  → http://localhost:8080
cd ummati
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=dev"

# 4. Frontend → http://localhost:4200   (dans un autre terminal)
cd ummati-front
npm install
npm start
```

Les conteneurs déjà créés se relancent avec `docker start ummati-postgres
ummati-mailhog`. Les migrations Flyway s'appliquent seules au démarrage du backend :
sur une base vide, tout le schéma est créé et les données de démonstration insérées.

| Adresse | Quoi |
|---|---|
| http://localhost:4200 | L'application |
| http://localhost:8080/swagger-ui.html | Toutes les routes de l'API, testables |
| http://localhost:8025 | Les emails envoyés (MailHog) |

Postgres est sur le port **5433** et non 5432, pour ne pas entrer en conflit avec une
installation Windows locale.

📖 **Détails, pièges et erreurs fréquentes → [`LANCEMENT.md`](LANCEMENT.md)**

---

## 4. Boucle de travail quotidienne

```powershell
# Backend
cd ummati
.\mvnw.cmd test                              # tous les tests
.\mvnw.cmd test "-Dtest=EventServiceTest"    # une seule classe

# Frontend
cd ummati-front
npm test                                     # tests (Vitest)
npm run build                                # build de production

# Mobile — après TOUTE modification du front
npm run sync:mobile                          # rebuild + copie dans les projets natifs
```

**Le projet impose le développement piloté par les tests** (voir `ROADMAP.md`) :
une fonctionnalité backend arrive avec ses tests unitaires (service) et
d'intégration (contrôleur).

---

## 5. Reprendre la main sur le code

Si tu n'as pas relu ce qui a été produit, voici l'ordre qui demande le moins
d'effort pour le plus de certitude. Les quatre étapes sont indépendantes.

### Étape 1 — Laisser la machine vérifier

```powershell
cd ummati        ; .\mvnw.cmd test     # ~290 tests
cd ..\ummati-front ; npm test          # ~77 tests
```

La CI GitHub rejoue cela sur chaque PR, **et monte en plus la stack complète sur une
base vierge** (job « Déploiement depuis zéro »). Ce job existe parce que trois bugs
avaient échappé aux tests unitaires : une migration qui ne passait que sur une base
déjà migrée, une auto-configuration absente, un healthcheck sur une URL protégée.

### Étape 2 — Voir tourner l'application

Le plus rapide pour juger : lance la stack (§ 3) et parcours le parcours réel —
inscription, validation par email dans MailHog, création d'une association, d'une
mission, inscription d'un bénévole.

Il existe aussi un test de bout en bout qui exerce toute l'API :

```powershell
.\test-e2e.ps1     # nécessite le backend lancé
```

### Étape 3 — Lire dans le bon ordre

Le code métier est concentré dans `ummati/src/main/java/orga/takwa/ummati/` :

1. `entity/enums/` — les états possibles (statuts d'événement, rôles, adhésions).
   C'est la source de vérité des règles métier, et ça se lit en 10 minutes.
2. `service/` — toute la logique, un service par domaine.
3. `controller/` — uniquement l'exposition HTTP, très peu de logique.

Côté front, `core/services/` reflète les contrôleurs backend un pour un.

### Étape 4 — Relire l'historique

Chaque fonctionnalité est arrivée par une branche puis une pull request. Les messages
de commit expliquent **pourquoi** un choix a été fait, pas seulement quoi :

```powershell
git log --oneline -20
git log -1 <hash>              # le raisonnement complet d'un changement
```

Les pull requests sont sur
[github.com/spanow/ummati-project/pulls](https://github.com/spanow/ummati-project/pulls)
(chacune résume ce qui a changé et pourquoi).

`ROADMAP.md` contient les tickets d'origine et les règles métier (RM-xx) : c'est là
qu'est l'intention produit quand le code ne suffit pas.

---

## 6. Mettre en production

```powershell
cp .env.example .env      # puis remplir CHAQUE valeur marquée « À GÉNÉRER »
docker compose up -d
```

Le `docker-compose.yml` monte quatre conteneurs : PostgreSQL, le backend, le serveur
de rendu Angular, et **nginx en unique point d'entrée**. C'est nginx qui route
`/api` et `/uploads` vers le backend et tout le reste vers le front — sans lui, le
navigateur appellerait `/api/v1` sur une origine qui ne la connaît pas.

Le front est servi par **Node et non par un nginx statique** : l'application est en
rendu serveur (SSR), un simple serveur de fichiers ne fonctionnerait pas.

⚠️ Le `docker-compose.yml` écoute en **HTTP**. Le certificat HTTPS se met en place
devant (Caddy, Traefik ou un nginx hôte) — voir `DEPLOIEMENT.md § 7`.

📖 **Génération des secrets, rotation, sauvegarde, checklist → [`DEPLOIEMENT.md`](DEPLOIEMENT.md)**

---

## 7. Applications mobiles

```powershell
cd ummati-front
npm run sync:mobile        # build + copie dans les projets natifs
npm run open:android       # Android Studio
npm run open:ios           # Xcode (macOS uniquement)
```

Cinq points sont à configurer avant toute publication — domaine réel, Firebase,
capacités Xcode, vérification de domaine pour les liens profonds, icônes définitives.

📖 **Procédure complète → [`MOBILE.md`](MOBILE.md)**

**Limites à connaître :** la compilation iOS exige un Mac (aucune alternative sous
Windows), et les notifications mobiles restent inactives tant que Firebase n'est pas
configuré — le reste de l'application fonctionne.

---

## 8. Où trouver quoi

| Document | Répond à |
|---|---|
| **`README.md`** (ce fichier) | Par où commencer, de quoi j'ai besoin |
| [`LANCEMENT.md`](LANCEMENT.md) | Lancer en local, résoudre les erreurs de démarrage |
| [`DEPLOIEMENT.md`](DEPLOIEMENT.md) | Mettre en production, gérer et faire tourner les secrets |
| [`MOBILE.md`](MOBILE.md) | Compiler et publier les applications iOS / Android |
| [`ROADMAP.md`](ROADMAP.md) | Tickets, sprints, règles métier (RM-xx), intention produit |
| [`CLAUDE.md`](CLAUDE.md) | Architecture détaillée et conventions de code |

---

## 9. Conventions du projet

- **Branches et PR** : une branche par fonctionnalité, jamais de commit direct sur
  `main`, puis une pull request.
- **Migrations** : les fichiers `db/migration/V*.sql` sont additifs. On ne modifie
  **jamais** une migration déjà appliquée, on en ajoute une nouvelle. Toute
  modification de schéma se recopie dans `src/test/resources/db/h2migration/`.
- **Erreurs** : les services lèvent les exceptions métier
  (`ResourceNotFoundException`, `ConflictException`, `ForbiddenException`,
  `BusinessRuleException`) ; `GlobalExceptionHandler` les traduit en réponses HTTP.
  On ne construit pas de réponse d'erreur à la main dans un contrôleur.
- **Secrets** : rien de sensible dans le dépôt. Tout passe par des variables
  d'environnement, et `.env` est ignoré par git.
