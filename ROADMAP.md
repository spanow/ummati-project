# 🕌 Ummati — Roadmap & Tickets de développement

> **Plateforme de Bénévolat & Gestion d'ONG**
> Document généré le 18 avril 2026 — basé sur les spécifications v3.0

IMPORTANT: TEST DRIVEN DEVELOPMENT IS A MUST!!!
---

## 📋 Table des matières

1. [Vue d'ensemble](#vue-densemble)
2. [Légende](#légende)
3. [Phase 1 — MVP (3 mois / 6 sprints)](#phase-1--mvp)
   - [Sprint 1 — Fondations & Setup](#sprint-1--fondations--setup)
   - [Sprint 2 — Authentification](#sprint-2--authentification)
   - [Sprint 3 — Organisations & Memberships](#sprint-3--organisations--memberships)
   - [Sprint 4 — Événements & Inscriptions](#sprint-4--événements--inscriptions)
   - [Sprint 5 — Notifications, Dashboard & Admin](#sprint-5--notifications-dashboard--admin)
   - [Sprint 6 — Frontend complet, Polish & Déploiement](#sprint-6--frontend-complet-polish--déploiement)
4. [Phase 2 — Enrichissement](#phase-2--enrichissement)
5. [Phase 3 — Expansion](#phase-3--expansion)
6. [Suivi d'avancement](#suivi-davancement)

---

## Vue d'ensemble

```
Phase 1 (MVP)       ██████████████████████████████░░░░░░░░░░  3 mois (6 sprints de 2 sem)
Phase 2 (Enrichi)   ░░░░░░░░░░░░░░░░░░░░░░░░░░░░██████████░  3 mois
Phase 3 (Expansion) ░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░████  4 mois
```

| Phase | Durée | Objectif |
|-------|-------|----------|
| **Phase 1 — MVP** | 3 mois | Auth, Profils, ONG, Memberships, Événements, Notifications, Dashboard, Admin |
| **Phase 2 — Enrichissement** | 3 mois | Chat, Push, i18n, Rappels, QR code, Compta, Export PDF, S3, Monitoring |
| **Phase 3 — Expansion** | 4 mois | Mobile, Elasticsearch, Calendriers, Gamification, Matching IA |

---

## Légende

| Tag | Signification |
|-----|---------------|
| `BACK` | Ticket backend (Java / Spring Boot) |
| `FRONT` | Ticket frontend (Angular) |
| `FULL` | Ticket fullstack (back + front) |
| `INFRA` | Infrastructure / CI/CD / Config |
| `DB` | Base de données / Migrations |
| `TEST` | Tests uniquement |
| `🔴 MUST` | Obligatoire pour le MVP |
| `🟡 SHOULD` | Important mais pas bloquant |
| `🟢 COULD` | Bonus si le temps le permet |

**Estimation :** XS (< 2h), S (2-4h), M (4-8h), L (1-2j), XL (2-3j), XXL (3-5j)

> ⚠️ **Directives transversales (voir specs v3.1) :**
> - **Design** : UI épurée, minimaliste, espaces blancs généreux, Material personnalisé, micro-interactions.
> - **TDD pragmatique** : chaque ticket inclut ses tests (unitaires service + intégration controller côté back, tests des services/formulaires côté front). On ne reporte pas les tests.

---

## Phase 1 — MVP

### Sprint 1 — Fondations & Setup
> **Objectif :** Projet opérationnel, CI/CD, BDD prête, structure en place

| # | Ticket | Type | Priorité | Estimation | Description |
|---|--------|------|----------|------------|-------------|
| **T-001** | ✅ Setup projet Spring Boot | `INFRA` | 🔴 MUST | M | Configurer le `pom.xml` avec toutes les dépendances (Spring Security 7, PostgreSQL, Flyway, SpringDoc, Caffeine, Mail, Testcontainers, RestAssured, JaCoCo). Profils `dev`, `test`, `prod`. |
| **T-002** | ✅ Configuration `application.yaml` | `BACK` | 🔴 MUST | S | Config par profil : DB, JWT, Mail, Upload, CORS. Créer `.env.example`. |
| **T-003** | ✅ Setup Angular frontend | `INFRA` | 🔴 MUST | M | Angular Material, environnements, proxy API, routing de base, navbar, pages placeholder. |
| **T-004** | ⏭️ CI/CD GitHub Actions | `INFRA` | 🔴 MUST | L | ~~Reporté~~ — pas de CI/CD pour l'instant. |
| **T-005** | ✅ Migration Flyway V1 — `users` | `DB` | 🔴 MUST | S | Table `users` avec tous les champs, contraintes, index `idx_users_email`, `idx_users_role`. |
| **T-006** | ✅ Migration Flyway V2 — `skills` & `user_skills` | `DB` | 🔴 MUST | S | Tables `skills` et `user_skills`. Index. |
| **T-007** | ✅ Migration Flyway V3 — `organizations` | `DB` | 🔴 MUST | S | Table `organizations` avec tous les champs, contraintes, index. |
| **T-008** | ✅ Migration Flyway V4 — `memberships` | `DB` | 🔴 MUST | S | Table `memberships`, contrainte unique `(user_id, org_id)`, index. |
| **T-009** | ✅ Migration Flyway V5 — `events` & `event_signups` | `DB` | 🔴 MUST | S | Tables `events`, `event_required_skills`, `event_signups`. Index. |
| **T-010** | ✅ Migration Flyway V6-V10 — Tables restantes | `DB` | 🔴 MUST | M | `event_feedbacks`, `documents`, `notifications`, `verification_tokens`, `audit_logs`. |
| **T-011** | ✅ Migration Flyway V11-V12 — Seed data | `DB` | 🔴 MUST | S | Seed skills (~30 compétences). Seed PLATFORM_ADMIN (`admin@ummati.org`). |
| **T-012** | ✅ Entités JPA | `BACK` | 🔴 MUST | XL | Toutes les entités + enums : `User`, `Organization`, `Membership`, `Skill`, `Event`, `EventSignup`, `EventFeedback`, `Document`, `Notification`, `VerificationToken`, `AuditLog`. |
| **T-013** | ✅ Repositories JPA | `BACK` | 🔴 MUST | M | Tous les repositories avec requêtes custom. |
| **T-014** | ✅ DTOs communs & enveloppes réponse | `BACK` | 🔴 MUST | S | `PageResponse<T>`, `ErrorResponse`, `ApiResponse<T>`. Java records. |
| **T-015** | ✅ GlobalExceptionHandler | `BACK` | 🔴 MUST | M | Handler centralisé : 404, 409, 403, 400 avec details. |
| **T-016** | ✅ Utilitaires | `BACK` | 🔴 MUST | S | `SlugUtil`, `FileStorageUtil`. |
| **T-017** | ✅ Config Security de base | `BACK` | 🔴 MUST | M | `SecurityConfig` : endpoints publics vs protégés, CORS, CSRF désactivé, BCrypt 12, headers sécurité. |

---

### Sprint 2 — Authentification
> **Objectif :** Inscription, connexion, vérification email, reset password, JWT

| # | Ticket | Type | Priorité | Estimation | Description |
|---|--------|------|----------|------------|-------------|
| **T-020** | ✅ JwtTokenProvider | `BACK` | 🔴 MUST | L | Génération/validation JWT. Access token 15min, refresh token 7j. Secret via env var. |
| **T-021** | ✅ JwtAuthenticationFilter | `BACK` | 🔴 MUST | M | Filtre Spring Security : extraction Bearer token, validation, injection `SecurityContext`. |
| **T-022** | ✅ `@CurrentUser` annotation | `BACK` | 🔴 MUST | XS | Annotation custom pour injecter l'utilisateur courant dans les controllers. |
| **T-023** | ✅ `POST /api/v1/auth/register` | `BACK` | 🔴 MUST | L | Inscription : validation, bcrypt 12, notif WELCOME, audit log, email vérification. |
| **T-024** | ✅ `POST /api/v1/auth/confirm-email` | `BACK` | 🔴 MUST | M | Confirmation email via token. Token expiré/utilisé gérés. |
| **T-025** | ✅ `POST /api/v1/auth/resend-confirmation` | `BACK` | 🟡 SHOULD | S | Renvoi email. Invalide ancien token. |
| **T-026** | ✅ `POST /api/v1/auth/login` | `BACK` | 🔴 MUST | L | Connexion : vérif email, brute-force (5→lock 30min), access+refresh+user, audit. |
| **T-027** | ✅ `POST /api/v1/auth/refresh` | `BACK` | 🔴 MUST | M | Renouvellement access token via refresh token. |
| **T-028** | ✅ `POST /api/v1/auth/forgot-password` | `BACK` | 🔴 MUST | M | Toujours 200 (pas de leak). Token 1h. |
| **T-029** | ✅ `POST /api/v1/auth/reset-password` | `BACK` | 🔴 MUST | M | Reset mdp via token. Audit log. |
| **T-030** | ✅ `POST /api/v1/auth/logout` | `BACK` | 🔴 MUST | S | Endpoint logout (client-side token removal). |
| **T-031** | ✅ EmailService (Thymeleaf) | `BACK` | 🔴 MUST | L | Service @Async. Templates : WELCOME, VERIFY, PASSWORD_RESET, PASSWORD_CHANGED. |
| **T-032** | ⬜ Tests unitaires Auth | `TEST` | 🔴 MUST | L | AuthService : inscription doublon, brute-force, tokens expirés/utilisés, complexité mdp. |
| **T-033** | ⬜ Tests intégration Auth | `TEST` | 🔴 MUST | L | Tous les endpoints auth avec MockMvc/RestAssured + Testcontainers. |
| **T-034** | ✅ Pages Login & Register (front) | `FRONT` | 🔴 MUST | L | Formulaires Material, validation, gestion erreurs API, redirect post-login. |
| **T-035** | ✅ Pages Forgot/Reset Password (front) | `FRONT` | 🔴 MUST | M | Formulaires forgot + reset avec gestion token. |
| **T-036** | ✅ Service Auth Angular | `FRONT` | 🔴 MUST | L | AuthService, intercepteur JWT (Bearer + refresh auto), stockage token localStorage. |
| **T-037** | ✅ Auth Guard & Redirect | `FRONT` | 🔴 MUST | M | authGuard + guestGuard, redirect /login ou /onboarding. |

---

### Sprint 3 — Organisations & Memberships
> **Objectif :** CRUD ONG, adhésion, gestion membres

| # | Ticket | Type | Priorité | Estimation | Description |
|---|--------|------|----------|------------|-------------|
| **T-040** | ✅ Profil API — `GET/PUT /api/v1/profile` | `BACK` | 🔴 MUST | L | ProfileService + ProfileController : lecture/modification profil, compétences, stats. |
| **T-041** | ✅ `POST /api/v1/profile/photo` | `BACK` | 🔴 MUST | M | Upload photo JPG/PNG max 5Mo. |
| **T-042** | ✅ `DELETE /api/v1/profile` (RGPD) | `BACK` | 🔴 MUST | L | Soft delete : anonymisation, memberships → LEFT, vérif dernier admin. |
| **T-043** | ✅ `PUT /api/v1/profile/password` | `BACK` | 🟡 SHOULD | S | Changement mdp : ancien ≠ nouveau. |
| **T-044** | ✅ `POST /api/v1/profile/onboarding` | `BACK` | 🔴 MUST | M | Onboarding : bio, ville, skills → `onboardingDone=true`. |
| **T-045** | ✅ `GET /api/v1/skills` | `BACK` | 🔴 MUST | S | SkillService + SkillController, filtre catégorie/recherche, cache. |
| **T-046** | ✅ `POST /api/v1/organizations` | `BACK` | 🔴 MUST | XL | Création ONG : slug auto, PENDING, membership ADMIN auto, notifs platform admins, audit. |
| **T-047** | ✅ `GET /api/v1/organizations` | `BACK` | 🔴 MUST | L | Liste ACTIVE, pagination, filtres domaine/ville/recherche, tri. |
| **T-048** | ✅ `GET /api/v1/organizations/{slug}` | `BACK` | 🔴 MUST | M | Détail ONG avec stats (membres, events). |
| **T-049** | ✅ `PUT /api/v1/organizations/{id}` | `BACK` | 🔴 MUST | M | Modification par admin ONG, vérif membership. |
| **T-050** | ✅ `PATCH /api/v1/organizations/{id}/status` | `BACK` | 🔴 MUST | M | Validation/rejet/suspension PLATFORM_ADMIN, motif ≥20 chars, notifs. |
| **T-051** | ✅ `POST /api/v1/organizations/{orgId}/memberships` | `BACK` | 🔴 MUST | L | Demande adhésion : vérifs (active, doublon, cooldown 30j), notif admins. |
| **T-052** | ✅ `PATCH /api/v1/memberships/{id}` (approve/reject) | `BACK` | 🔴 MUST | M | Approbation/rejet par admin ONG, notif bénévole. |
| **T-053** | ✅ `DELETE /api/v1/memberships/{id}` (quitter/exclure) | `BACK` | 🔴 MUST | L | Quitter/exclure, vérif dernier admin, RM-22/RM-23. |
| **T-054** | ✅ `PATCH /api/v1/memberships/{id}/role` | `BACK` | 🔴 MUST | M | Changement rôle, vérif dernier admin. |
| **T-055** | ✅ `GET /api/v1/organizations/{orgId}/memberships` | `BACK` | 🔴 MUST | M | Liste membres paginée, filtre statut. |
| **T-056** | ✅ Tests unitaires Orgs & Memberships | `TEST` | 🔴 MUST | L | 8 tests : RM-10, RM-12, RM-20, RM-21, RM-22, RM-23, slug, duplicat. |
| **T-057** | ⬜ Tests intégration Orgs & Memberships | `TEST` | 🔴 MUST | L | Endpoints orgs + memberships + permissions. |
| **T-058** | ✅ Page liste ONG (front) | `FRONT` | 🔴 MUST | L | Grille responsive, filtres domaine/recherche, pagination, cards épurées avec hover. |
| **T-059** | ✅ Page détail ONG (front) | `FRONT` | 🔴 MUST | L | Banner, onglets Material, stats, bouton Rejoindre. |
| **T-060** | ✅ Formulaire création ONG (front) | `FRONT` | 🔴 MUST | L | Form Material, validation, redirect vers détail après création. |
| **T-061** | ✅ Page gestion membres ONG (front) | `FRONT` | 🔴 MUST | L | Onglets (En attente / Actifs), actions (approuver, refuser, changer rôle, exclure), avatars, badges rôles. |
| **T-062** | ✅ Page profil & édition (front) | `FRONT` | 🔴 MUST | L | Hero avec avatar uploadable, stats, chips compétences, onglets édition infos + changement mdp. |
| **T-063** | ✅ Onboarding guidé (front) | `FRONT` | 🔴 MUST | XL | Stepper 3 étapes Material : Bio/Ville → Compétences → Terminé. |

---

### Sprint 4 — Événements & Inscriptions
> **Objectif :** CRUD événements, inscriptions, waitlist, feedback

| # | Ticket | Type | Priorité | Estimation | Description |
|---|--------|------|----------|------------|-------------|
| **T-070** | ✅ `POST /api/v1/organizations/{orgId}/events` | `BACK` | 🔴 MUST | L | Création événement : validation dates, skills requises, status=DRAFT. Audit log. |
| **T-071** | ✅ `PUT /api/v1/events/{id}` | `BACK` | 🔴 MUST | M | Modification : DRAFT = tout modifiable, PUBLISHED = limité (pas dates/max_participants). |
| **T-072** | ✅ `PATCH /api/v1/events/{id}/status` | `BACK` | 🔴 MUST | L | Publish (DRAFT→PUBLISHED, notif membres), Cancel (motif ≥10 chars, notif inscrits), Complete. |
| **T-073** | ✅ `GET /api/v1/events` | `BACK` | 🔴 MUST | L | Liste PUBLISHED futurs, pagination 10/page, filtres (type, ville, ONG, dates, online, skills), tri. |
| **T-074** | ✅ `GET /api/v1/events/{id}` | `BACK` | 🔴 MUST | M | Détail complet + ONG + compteurs places + feedbacks si passé. |
| **T-075** | ✅ `POST /api/v1/events/{id}/signups` | `BACK` | 🔴 MUST | L | Inscription : vérif deadline, max participants → REGISTERED ou WAITLISTED, vérif min_age, doublon. Notif + email. |
| **T-076** | ✅ `DELETE /api/v1/events/{id}/signups` | `BACK` | 🔴 MUST | L | Désinscription : promotion FIFO waitlist, notif SIGNUP_PROMOTED au promu. |
| **T-077** | ✅ `GET /api/v1/events/{id}/signups` | `BACK` | 🔴 MUST | M | Liste inscrits (admin ONG), compteurs par statut. |
| **T-078** | ✅ `GET /api/v1/events/{id}/signups/export` | `BACK` | 🟡 SHOULD | M | Export CSV des inscrits. |
| **T-079** | ✅ `PATCH /api/v1/events/{id}/signups/attendance` | `BACK` | 🟡 SHOULD | M | Marquage présence : REGISTERED→ATTENDED. Déclenchement notif FEEDBACK_REQUESTED. |
| **T-080** | ✅ `POST /api/v1/events/{id}/feedbacks` | `BACK` | 🟡 SHOULD | M | Feedback : note 1-5, commentaire, anonyme. Vérif ATTENDED, unicité. |
| **T-081** | ✅ `GET /api/v1/events/{id}/feedbacks` | `BACK` | 🟡 SHOULD | S | Liste feedbacks paginée + note moyenne. |
| **T-082** | ✅ EventCompletionJob | `BACK` | 🟡 SHOULD | M | Job planifié 03:00 UTC : PUBLISHED + `end_date < now` → COMPLETED. |
| **T-083** | ✅ Tests unitaires Events & Signups | `TEST` | 🔴 MUST | L | EventService, EventSignupService : max participants, waitlist FIFO, deadline, promotion. |
| **T-084** | ✅ Tests intégration Events | `TEST` | 🔴 MUST | L | Tous les endpoints événements + inscriptions + feedbacks. |
| **T-085** | ✅ Page liste événements (front) | `FRONT` | 🔴 MUST | L | Liste paginée, filtres (type, ville, dates, online), cards avec badge inscrit, indicateur "presque complet". |
| **T-086** | ✅ Page détail événement (front) | `FRONT` | 🔴 MUST | XL | Infos complètes, compteur places + barre progression, carte GPS, boutons inscription/désinscription, feedbacks. |
| **T-087** | ✅ Formulaire création/édition événement (front) | `FRONT` | 🔴 MUST | L | Form multi-champs, sélection skills, toggle online, validation dates. |
| **T-088** | ✅ Page gestion événements ONG (front) | `FRONT` | 🔴 MUST | L | Liste des events de l'ONG (tous statuts), actions (publier, annuler), liste inscrits, marquage présence. |
| **T-089** | ✅ Composant feedback / StarRating (front) | `FRONT` | 🟡 SHOULD | M | Composant étoiles 1-5, formulaire feedback, affichage liste feedbacks. |

---

### Sprint 5 — Notifications, Dashboard & Admin
> **Objectif :** Système de notifications, dashboards par rôle, administration plateforme

| # | Ticket | Type | Priorité | Estimation | Description |
|---|--------|------|----------|------------|-------------|
| **T-100** | ✅ NotificationService | `BACK` | 🔴 MUST | L | Service centralisé : création notif in-app + envoi email asynchrone. Tous les types définis dans la spec. |
| **T-101** | ✅ API Notifications | `BACK` | 🔴 MUST | M | `GET /notifications`, `GET /notifications/unread-count`, `PATCH /{id}/read`, `PATCH /read-all`. |
| **T-102** | ✅ Templates email restants | `BACK` | 🔴 MUST | L | Templates Thymeleaf : ONG (validée/rejetée), Membership (accepted), Event (notification), Signup (confirmed/waitlisted), Feedback request. |
| **T-103** | ✅ AuditService | `BACK` | 🟡 SHOULD | M | Service d'audit : insertion dans `audit_logs` avec actorId, action, entityType, entityId. Intégré dans tous les services. |
| **T-104** | ✅ Dashboard bénévole API | `BACK` | 🔴 MUST | L | Endpoint `/dashboard/volunteer` : prochains events, mes ONG, events suggérés, stats perso. |
| **T-105** | ✅ Dashboard admin ONG API | `BACK` | 🔴 MUST | M | Endpoint `/dashboard/org-admin/{orgId}` : membres actifs, demandes pending, events, note moyenne, derniers membres. |
| **T-106** | ✅ Dashboard PLATFORM_ADMIN API | `BACK` | 🟡 SHOULD | M | `GET /admin/stats` : totalUsers, totalOrgs, totalEvents, pendingOrgs, registrationsThisWeek, eventsThisMonth. |
| **T-107** | ✅ `GET /api/v1/admin/users` | `BACK` | 🔴 MUST | M | Liste users paginée, recherche nom/email, filtre enabled. |
| **T-108** | ✅ `PATCH /api/v1/admin/users/{id}/status` | `BACK` | 🔴 MUST | S | Désactiver/réactiver user. Audit log. |
| **T-109** | ✅ `GET /api/v1/admin/organizations` | `BACK` | 🔴 MUST | S | Liste ONG filtrée par status (PENDING, ACTIVE, SUSPENDED, ARCHIVED). |
| **T-110** | ✅ Documents API | `BACK` | 🟡 SHOULD | L | CRUD documents (upload multipart, download, delete). Whitelist MIME (PDF, JPG, PNG, DOCX). Max 10Mo. |
| **T-111** | ✅ `GET /api/v1/profile/memberships` + `/signups` | `BACK` | 🔴 MUST | M | Mes memberships paginées + mes inscriptions avec filtre status. |
| **T-112** | ✅ Tests intégration Notifications & Admin | `TEST` | 🔴 MUST | L | Endpoints notifs (list, unread, markRead, markAll, access control) + admin (stats, users, orgs, permissions PLATFORM_ADMIN). |
| **T-113** | ✅ Centre de notifications (front) | `FRONT` | 🔴 MUST | L | Page complète paginée, marquer lu/tout marquer lu, badge compteur, icônes par type. |
| **T-114** | ✅ Dashboard bénévole (front) | `FRONT` | 🔴 MUST | XL | Widgets : bienvenue, bannière onboarding, prochains events, mes ONG, events suggérés, stats perso. |
| **T-115** | ✅ Dashboard admin ONG (front) | `FRONT` | 🔴 MUST | L | Widgets : stats ONG, alerte demandes pending, derniers membres, raccourcis gestion. |
| **T-116** | ✅ Dashboard PLATFORM_ADMIN (front) | `FRONT` | 🟡 SHOULD | L | Intégré dans la page Admin : stats globales (5 widgets), onglets utilisateurs + organisations. |
| **T-117** | ✅ Pages admin plateforme (front) | `FRONT` | 🔴 MUST | L | Gestion utilisateurs (liste, recherche, désactiver/réactiver), gestion ONG (liste par statut). |
| **T-118** | ✅ Page "Mes ONG" & "Mes événements" (front) | `FRONT` | 🔴 MUST | M | Page `/my-activities` : onglets mes memberships ACTIVE + mes inscriptions paginées. |

---

### Sprint 6 — Frontend complet, Polish & Déploiement
> **Objectif :** Finalisation UI, pages manquantes, tests E2E, déploiement staging

| # | Ticket | Type | Priorité | Estimation | Description |
|---|--------|------|----------|------------|-------------|
| **T-130** | ✅ Landing page | `FRONT` | 🔴 MUST | L | Page d'accueil attractive : hero, features, stats de la plateforme, CTA inscription. |
| **T-131** | ✅ NavBar responsive | `FRONT` | 🔴 MUST | M | Logo, liens, avatar, notifications (badge), menu burger mobile. |
| **T-132** | ✅ Page paramètres compte (front) | `FRONT` | 🟡 SHOULD | M | Changement mdp, changement email, suppression compte (avec confirmation mdp). |
| **T-133** | ✅ Empty states & illustrations | `FRONT` | 🟡 SHOULD | M | États vides pour toutes les listes : illustrations + CTA (ex: "Aucune ONG trouvée, créez la vôtre !"). |
| **T-134** | ✅ Composants UI communs | `FRONT` | 🔴 MUST | L | Card, Badge, Pagination, Modal, Toast, FormField, Skeleton, Avatar, ProgressBar, Stepper. |
| **T-135** | ✅ Gestion documents ONG (front) | `FRONT` | 🟡 SHOULD | M | Upload, liste, téléchargement, suppression de documents pour une ONG. |
| **T-136** | ✅ Page validation ONG admin (front) | `FRONT` | 🔴 MUST | M | Vue détaillée ONG pending + documents téléchargeables + boutons approuver/rejeter avec motif. |
| **T-137** | ✅ Accessibilité WCAG 2.1 AA | `FRONT` | 🟡 SHOULD | L | Audit accessibilité : navigation clavier, aria-labels, contrastes, focus visible, alt texts. |
| **T-138** | ✅ Tests E2E manuels | `TEST` | 🔴 MUST | XL | Parcours critiques : inscription→login→onboarding, création ONG→validation, inscription event→waitlist→promotion, feedback. |
| **T-139** | ✅ Documentation API OpenAPI | `BACK` | 🔴 MUST | M | Config SpringDoc, annotations `@Operation`, `@ApiResponse` sur tous les controllers. Swagger UI accessible. |
| **T-141** | ✅ AccountPurgeJob | `BACK` | 🟡 SHOULD | M | Job quotidien 04:00 UTC : purge comptes anonymisés > 30j. |
| **T-142** | ✅ Revue sécurité | `BACK` | 🔴 MUST | L | Checklist OWASP Top 10, vérif headers sécurité, validation MIME upload, rate limiting, scan dépendances. |
| **T-143** | ✅ `GET /api/v1/profile/export` (RGPD) | `BACK` | 🟡 SHOULD | M | Export JSON complet des données utilisateur (droit de portabilité). |
| **T-144** | ✅ Performance & optimisation | `BACK` | 🟡 SHOULD | L | Vérification N+1 queries (EntityGraph/JOIN FETCH), cache Caffeine sur skills + stats ONG, pagination optimisée. |

---

## Phase 2 — Enrichissement

> **Durée estimée :** 3 mois — Tickets à découper en sprints quand Phase 1 terminée

| # | Ticket | Type | Priorité | Estimation | Description |
|---|--------|------|----------|------------|-------------|
| **T-200** | Chat temps réel | `FULL` | 🟡 SHOULD | XXL | WebSocket + STOMP, conversations par ONG, messages persistés. |
| **T-201** | Notifications push navigateur | `FULL` | 🟡 SHOULD | XL | Web Push API, service worker, opt-in utilisateur. |
| **T-202** | Multilingue FR/AR | `FULL` | 🟡 SHOULD | XXL | i18n Angular (`@angular/localize`), backend messages traduits, support RTL pour l'arabe. |
| **T-203** | Rappels automatiques J-7 / J-1 | `BACK` | 🟡 SHOULD | L | Spring Scheduler, emails rappel 7j et 1j avant événement. |
| **T-204** | QR code validation présence | `FULL` | 🟡 SHOULD | XL | Génération QR par événement, scan mobile pour valider présence. |
| **T-205** | Module comptable basique | `FULL` | 🟡 SHOULD | XXL | Dépenses/recettes par événement, vue financière ONG (rôle ACCOUNTANT). |
| **T-206** | Export PDF rapports ONG | `BACK` | 🟡 SHOULD | L | Génération PDF : rapport activité ONG (membres, events, stats). |
| **T-207** | Migration stockage → S3 (MinIO) | `BACK` | 🟡 SHOULD | L | Abstraction FileStorage, implémentation S3, migration fichiers existants. |
| **T-208** | Monitoring Prometheus + Grafana | `INFRA` | 🟡 SHOULD | L | Micrometer → Prometheus, dashboards Grafana, alertes. |
| **T-209** | Mode sombre | `FRONT` | 🟢 COULD | L | Toggle dark mode, CSS variables, persistance préférence. |
| **T-210** | InactiveAccountJob | `BACK` | 🟡 SHOULD | M | Job hebdo : avertissement comptes inactifs > 22 mois, suppression > 24 mois. |

---

## Phase 3 — Expansion

> **Durée estimée :** 4 mois

| # | Ticket | Type | Priorité | Estimation | Description |
|---|--------|------|----------|------------|-------------|
| **T-300** | Application mobile Flutter | `FRONT` | 🟢 COULD | XXL | App mobile iOS/Android, même API backend. |
| **T-301** | Recherche avancée Elasticsearch | `FULL` | 🟢 COULD | XXL | Index ONG + événements, recherche full-text avancée, suggestions. |
| **T-302** | Intégration calendriers (iCal, Google) | `FULL` | 🟢 COULD | L | Export iCal par événement/agenda perso, sync Google Calendar. |
| **T-303** | Gamification | `FULL` | 🟢 COULD | XXL | Badges, points, classement bénévoles, récompenses visuelles. |
| **T-304** | Matching IA bénévoles ↔ missions | `BACK` | 🟢 COULD | XXL | Algorithme de recommandation basé sur compétences + localisation + historique. |
| **T-305** | API publique documentée | `BACK` | 🟢 COULD | XL | API ouverte pour intégrateurs tiers, gestion clés API, documentation externe. |

---

## Suivi d'avancement

### Phase 1 — Progression globale

| Sprint | Période | Statut | Progression |
|--------|---------|--------|-------------|
| Sprint 1 — Fondations | Sem 1-2 | ✅ Terminé | ██████████ 100% |
| Sprint 2 — Auth | Sem 3-4 | ✅ Terminé | ██████████ 100% |
| Sprint 3 — Orgs & Memberships | Sem 5-6 | ✅ Terminé | ██████████ 100% |
| Sprint 3 — Orgs & Memberships | Sem 5-6 | ⬜ À faire | ░░░░░░░░░░ 0% |
| Sprint 4 — Événements | Sem 7-8 | ✅ Terminé | ██████████ 100% |
| Sprint 5 — Notifs & Dashboards | Sem 9-10 | ✅ Terminé | ██████████ 100% |
| Sprint 6 — Polish & Déploiement | Sem 11-12 | ✅ Terminé | ██████████ 100% |

### Compteur de tickets Phase 1

| Catégorie | Total | 🔴 MUST | 🟡 SHOULD | 🟢 COULD |
|-----------|-------|---------|-----------|----------|
| Backend | ~45 | ~35 | ~10 | 0 |
| Frontend | ~25 | ~20 | ~5 | 0 |
| Tests | ~10 | ~8 | ~2 | 0 |
| Infra/DB | ~15 | ~15 | 0 | 0 |
| **Total** | **~95** | **~78** | **~17** | **0** |

---

> 📌 **Prochaine étape :** Commencer par le **Sprint 1** — Setup projet, migrations Flyway, entités JPA, config sécurité.


