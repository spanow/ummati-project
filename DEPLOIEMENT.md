# Déploiement & gestion des secrets

Ce document couvre la mise en production d'Ummati : génération des secrets, démarrage,
rotation, et procédure en cas de fuite.

Pour le développement local, voir [`LANCEMENT.md`](LANCEMENT.md).

---

## 1. Principe : aucun secret dans le dépôt

`application.yaml` contient des valeurs par défaut (clé JWT, mot de passe admin, clés
VAPID). **Elles existent uniquement pour que le projet démarre sans configuration en
développement.** Elles sont publiques — n'importe qui lisant le dépôt peut les utiliser
pour signer un jeton `PLATFORM_ADMIN` et prendre le contrôle de la plateforme.

Le backend **refuse donc de démarrer en profil `prod`** tant qu'un secret est resté à sa
valeur par défaut, absent, ou trop faible :

```
========================================================================
 DÉMARRAGE REFUSÉ — secrets de production non configurés
========================================================================
  - JWT_SECRET est resté à la valeur par défaut du dépôt (publique, donc compromise)
  - DB_PASSWORD n'est pas défini
========================================================================
```

Une variable oubliée provoque un échec explicite, jamais un démarrage silencieux avec une
clé connue de tous. Le contrôle vit dans
`ummati/src/main/java/orga/takwa/ummati/config/SecretsValidator.java`, et il est branché
avant la création du contexte Spring pour que le message ne se perde pas derrière une
erreur de connexion à la base.

---

## 2. Générer les secrets

```bash
cp .env.example .env
```

Puis remplir chaque valeur. Les commandes ci-dessous fonctionnent sous Linux, macOS et
Git Bash sous Windows.

| Variable | Commande | Contrainte |
|---|---|---|
| `JWT_SECRET` | `openssl rand -hex 32` | ≥ 32 octets (HS256 = 256 bits) |
| `DB_PASSWORD` | `openssl rand -base64 24` | — |
| `ADMIN_PASSWORD` | `openssl rand -base64 18` | ≥ 12 caractères |
| `VAPID_PUBLIC_KEY` / `VAPID_PRIVATE_KEY` | `npx web-push generate-vapid-keys` | optionnel |

Tout générer d'un coup :

```bash
cp .env.example .env
{
  echo "DB_PASSWORD=$(openssl rand -base64 24)"
  echo "JWT_SECRET=$(openssl rand -hex 32)"
  echo "ADMIN_PASSWORD=$(openssl rand -base64 18)"
} >> .env
```

Renseigner ensuite manuellement `PUBLIC_URL`, `ADMIN_EMAIL` et les paramètres `MAIL_*`.

> **`.env` n'est jamais committé** — il est dans `.gitignore`, et la CI échoue si un
> `.env` apparaît dans le dépôt.

### Les clés VAPID sont facultatives

Sans elles, seules les **notifications push navigateur** sont désactivées : les
notifications in-app et les emails continuent de fonctionner, et l'application démarre
normalement en le signalant dans les journaux.

---

## 3. Démarrer

```bash
docker compose up -d --build
```

La stack se compose de quatre services :

| Service | Rôle | Exposé |
|---|---|---|
| `proxy` | nginx, point d'entrée unique | oui (`HTTP_PORT`) |
| `frontend` | serveur SSR Angular | non |
| `backend` | API Spring Boot | non |
| `db` | PostgreSQL 16 | non |

Seul `proxy` est joignable de l'extérieur : il route `/api` et `/uploads` vers le
backend, tout le reste vers le rendu serveur. C'est ce qui permet au navigateur
d'appeler `/api/v1/...` en relatif, comme en développement.

Vérifier :

```bash
docker compose ps                       # les 4 services doivent être « healthy »
curl http://localhost/actuator/health   # {"status":"UP"}
```

Les migrations Flyway s'appliquent automatiquement au premier démarrage sur une base
vierge — il n'y a aucune étape d'initialisation manuelle.

### Deux volumes à sauvegarder

| Volume | Contenu | Conséquence d'une perte |
|---|---|---|
| `db-data` | toute la base | perte totale des données |
| `uploads` | logos, bannières, couvertures, galeries, documents | images et pièces jointes perdues |

`uploads` est facile à oublier : sans lui, chaque redéploiement efface les visuels.

---

## 4. Mettre à jour

Les images sont publiées sur GHCR à chaque tag de version :

```bash
git tag v1.0.0 && git push origin v1.0.0
```

Sur le serveur :

```bash
docker compose pull && docker compose up -d
```

Les migrations de schéma s'appliquent au démarrage du nouveau conteneur.

---

## 5. Faire tourner les secrets

### `JWT_SECRET`

**Effet immédiat : toutes les sessions actives sont invalidées**, les jetons signés avec
l'ancienne clé deviennent invalides et les utilisateurs sont déconnectés. À faire hors
heures de pointe.

```bash
sed -i "s|^JWT_SECRET=.*|JWT_SECRET=$(openssl rand -hex 32)|" .env
docker compose up -d backend
```

### `DB_PASSWORD`

Changer le mot de passe **dans PostgreSQL d'abord**, puis dans `.env` :

```bash
docker compose exec db psql -U ummati -c "ALTER USER ummati WITH PASSWORD 'nouveau';"
sed -i "s|^DB_PASSWORD=.*|DB_PASSWORD=nouveau|" .env
docker compose up -d backend
```

Modifier `.env` sans changer le mot de passe côté base empêche le backend de démarrer.

### `ADMIN_PASSWORD`

Ne s'applique qu'à la création initiale du compte administrateur. Sur une instance déjà
en service, changer le mot de passe depuis l'interface (Paramètres → Mot de passe).

### Clés VAPID

Les faire tourner invalide les abonnements push existants : les utilisateurs devront
réautoriser les notifications dans leur navigateur.

---

## 6. En cas de fuite d'un secret

1. **Faire tourner le secret immédiatement** (section 5).
2. **`JWT_SECRET` fuité** : le changer déconnecte tout le monde et invalide les jetons
   forgés. Vérifier ensuite `audit_logs` à la recherche d'actions administratives
   inattendues.
3. **`DB_PASSWORD` fuité** : changer le mot de passe, puis vérifier que la base n'a
   jamais été exposée hors du réseau Docker (le `docker-compose.yml` fourni ne publie
   aucun port pour `db`).
4. **Si le secret a été committé** : le faire tourner ne suffit pas, il reste dans
   l'historique Git. Purger avec `git filter-repo`, forcer la réécriture, et considérer
   l'ancien secret comme définitivement compromis.

---

## 7. Reverse proxy et rate limiting

Le quota de connexion (20 requêtes/minute sur `/api/v1/auth/**`) identifie les clients
par leur IP. Le réglage `TRUST_FORWARDED_FOR` détermine la source de cette IP :

| Valeur | Source de l'IP | Quand l'utiliser |
|---|---|---|
| `false` (défaut) | adresse du pair TCP | backend exposé directement |
| `true` | premier élément de `X-Forwarded-For` | derrière un reverse proxy de confiance |

Le `docker-compose.yml` fourni met `true`, car nginx est en amont **et réécrit**
l'en-tête (`proxy_set_header X-Forwarded-For $remote_addr`) au lieu de l'enrichir.

⚠️ Cette réécriture est essentielle. Avec la directive `$proxy_add_x_forwarded_for`
usuelle, la valeur envoyée par le client resterait en tête de liste : il suffirait
d'envoyer une IP différente à chaque requête pour échapper entièrement au quota.

À l'inverse, laisser `false` derrière un proxy fait partager un unique compteur à tous
les utilisateurs — l'IP du proxy — et bloque les connexions légitimes.

### HTTPS

Le proxy fourni écoute en HTTP. En production, terminer TLS en amont (Caddy, Traefik,
ou un load balancer managé) et faire pointer `PUBLIC_URL` sur l'URL `https://`.

---

## 8. Checklist de mise en production

- [ ] `.env` créé, tous les secrets générés (aucune valeur du dépôt)
- [ ] `PUBLIC_URL` pointe sur l'URL publique réelle (liens des emails et CORS)
- [ ] SMTP réel configuré et testé (inscription → email de vérification reçu)
- [ ] TLS terminé en amont, `PUBLIC_URL` en `https://`
- [ ] Sauvegarde planifiée des volumes `db-data` **et** `uploads`
- [ ] `docker compose ps` : les 4 services « healthy »
- [ ] Compte admin initial testé, puis mot de passe changé depuis l'interface
- [ ] `.env` sauvegardé hors du dépôt (gestionnaire de secrets)

---

## 9. Intégration continue

| Workflow | Déclencheur | Rôle |
|---|---|---|
| `ci.yml` | push sur `main`, pull request | tests backend et frontend, démarrage complet depuis une base vierge, absence de `.env` versionné |
| `release.yml` | tag `v*` | construction et publication des images sur GHCR |

Le job **« Déploiement depuis zéro »** monte la stack entière sur une base vide et
exerce les parcours essentiels. C'est lui qui attrape les défauts invisibles aux tests
unitaires : une migration qui ne passe que sur une base déjà migrée, une
auto-configuration absente, un healthcheck pointant vers une URL protégée — trois
défauts réellement présents avant sa mise en place.

Aucun secret n'est nécessaire pour la CI : elle génère les siens à la volée.
`release.yml` utilise le `GITHUB_TOKEN` fourni automatiquement.
