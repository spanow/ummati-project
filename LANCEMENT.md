# 🚀 Lancer le projet Ummati en local

> Ce document couvre le **développement local**. Pour la mise en production
> (conteneurs, secrets, rotation, CI), voir [`DEPLOIEMENT.md`](DEPLOIEMENT.md).
>
> Raccourci : `docker compose up -d --build` monte la stack complète (base, backend,
> frontend, reverse proxy) sans rien installer — un `.env` renseigné suffit.

## Prérequis

- Java 21+
- Node.js 18+ & npm 11+
- Docker (pour PostgreSQL et MailHog)

---

## 1. 🐘 Lancer PostgreSQL avec Docker

```powershell
docker run --name ummati-postgres `
  -e POSTGRES_DB=ummati_db `
  -e POSTGRES_USER=ummati `
  -e POSTGRES_PASSWORD=ummati `
  -p 5433:5432 `
  -d postgres:15
```

> ⚠️ Le port **5433** est utilisé (et non 5432) car PostgreSQL est installé localement sur Windows et occupe déjà le 5432.

Si le container existe déjà :
```powershell
docker start ummati-postgres
```

---

## 2. 📧 Lancer MailHog (faux serveur SMTP pour le dev)

MailHog intercepte tous les emails et les affiche dans une interface web. Aucun email réel n'est envoyé.

```powershell
docker run --name ummati-mailhog -p 1025:1025 -p 8025:8025 -d mailhog/mailhog
```

Si le container existe déjà :
```powershell
docker start ummati-mailhog
```

✅ Interface web MailHog : **http://localhost:8025**
(tous les emails de vérification et notifications y apparaissent)

---

## 3. 🗄️ Base de données — migrations automatiques

**Rien à faire.** Depuis l'activation de Flyway en dev, toutes les migrations
`src/main/resources/db/migration/V*.sql` sont appliquées **automatiquement au démarrage
du backend** (étape 4). Sur une base vide, Flyway crée tout le schéma depuis `V1` et
insère les données de démo (`V7__seed_data.sql`).

### Vous aviez déjà une base migrée à la main ?

`application-dev.yaml` pose `baseline-on-migrate: true` avec `baseline-version: 17` :
Flyway considère qu'une base déjà peuplée mais sans historique est à jour jusqu'à `V17`,
enregistre cette ligne de base et n'exécute que `V18` et suivantes.

⚠️ Ce réglage suppose que votre base est bien à jour jusqu'à `V17`. Si vous n'aviez
appliqué qu'une partie des scripts, les migrations manquantes seraient **silencieusement
sautées**. Dans le doute, repartez d'une base propre — les données de dev sont jetables :

```powershell
docker exec -i ummati-postgres psql -U ummati -d postgres -c "DROP DATABASE ummati_db;"
docker exec -i ummati-postgres psql -U ummati -d postgres -c "CREATE DATABASE ummati_db OWNER ummati;"
```

Puis relancez le backend : Flyway rejouera l'intégralité des scripts depuis `V1`.

### Vérifier ce que Flyway a appliqué

```powershell
docker exec -i ummati-postgres psql -U ummati -d ummati_db -c "SELECT version, description, success FROM flyway_schema_history ORDER BY installed_rank;"
```

---

## 4. ⚙️ Lancer le Backend (Spring Boot)

```powershell
cd ummati
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=dev"
```

✅ Le backend démarre sur **http://localhost:8080**

> Les credentials DB sont hardcodés dans `application-dev.yaml` (url: 5433, user: ummati, pass: ummati).
> Flyway applique les migrations au démarrage (cf. étape 3) — plus rien à lancer à la main.
> SMTP pointe vers MailHog (localhost:1025) — aucun vrai email envoyé.
> Les images uploadées sont écrites sous `ummati/uploads/images/` et servies sur `/uploads/images/**`.

---

## 5. 🌐 Lancer le Frontend (Angular)

```powershell
cd ummati-front
npm install        # une seule fois
npm start
```

✅ Le frontend démarre sur **http://localhost:4200**

> Le proxy `proxy.conf.json` redirige `/api/*` → `http://localhost:8080`.

---

## 6. 🧪 Lancer les tests

### Tests Backend
```powershell
cd ummati
.\mvnw.cmd test
```

### Tests Frontend
```powershell
cd ummati-front
npm test
```

---

## 📋 Résumé des ports

| Service      | URL                                             |
|--------------|-------------------------------------------------|
| Frontend     | http://localhost:4200                           |
| Backend      | http://localhost:8080                           |
| Swagger UI   | http://localhost:8080/swagger-ui.html           |
| MailHog UI   | http://localhost:8025  (emails de dev)          |
| PostgreSQL   | localhost:**5433** (Docker) / 5432 (local Win)  |

---

## ❗ Problèmes fréquents

### `MailAuthenticationException` lors de l'inscription
MailHog n'est pas démarré. Lancez : `docker start ummati-mailhog`

### PostgreSQL : "password authentication failed"
Il y a un PostgreSQL local Windows sur le port 5432 qui interfère. Le projet est configuré sur le **port 5433** (Docker).
Vérifiez que le container tourne : `docker ps`

### La base est vide / tables manquantes
Flyway applique les migrations au démarrage du backend. Vérifiez l'historique
(`flyway_schema_history`, étape 3) : si des versions manquent, repartez d'une base propre
avec le `DROP DATABASE` / `CREATE DATABASE` de l'étape 3, puis relancez le backend.

### `FlywayValidateException` : migration checksum mismatch
Un fichier `V*.sql` déjà appliqué a été modifié. Les migrations sont **immuables** : rétablissez
le fichier d'origine et ajoutez plutôt un nouveau `V{n}__description.sql`.

### Le port 8080 est déjà utilisé
```powershell
netstat -ano | findstr :8080
taskkill /PID <PID> /F
```

### Le port 4200 est déjà utilisé
```powershell
# Dans ummati-front/
npx ng serve --port 4201
```
