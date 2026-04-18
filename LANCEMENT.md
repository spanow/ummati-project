# 🚀 Lancer le projet Ummati en local

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

## 3. 🗄️ Initialiser la base de données (première fois uniquement)

Les migrations Flyway ne s'exécutent pas automatiquement en dev (conflit avec DevTools). Il faut les lancer manuellement **une seule fois** après création du container :

```powershell
cd ummati

foreach ($f in (Get-ChildItem "src\main\resources\db\migration\*.sql" | Sort-Object Name)) {
  Write-Host "=== $($f.Name) ==="
  $sql = Get-Content $f.FullName -Raw
  $sql | docker exec -i ummati-postgres psql -U ummati -d ummati_db
}
```

> ✅ Vous devriez voir `CREATE TABLE`, `CREATE INDEX`, `INSERT` pour chaque script V1 à V7.

---

## 4. ⚙️ Lancer le Backend (Spring Boot)

```powershell
cd ummati
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=dev"
```

✅ Le backend démarre sur **http://localhost:8080**

> Les credentials DB sont hardcodés dans `application-dev.yaml` (url: 5433, user: ummati, pass: ummati).
> Flyway est désactivé en dev (`spring.flyway.enabled: false`) — les migrations sont gérées manuellement.
> SMTP pointe vers MailHog (localhost:1025) — aucun vrai email envoyé.

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
Relancer les migrations manuellement (étape 3 ci-dessus).

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
