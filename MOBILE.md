# Applications mobiles iOS & Android

Ummati est livré en trois formes qui partagent **un seul code applicatif** : le site
Angular, et les applications iOS et Android qui embarquent ce même bundle dans un
conteneur natif (Capacitor).

Il n'existe donc pas d'écrans à réimplémenter : une fonctionnalité ajoutée au front
arrive dans les trois. Ce que le conteneur apporte, ce sont les capacités que le
navigateur refuse — notifications natives, stockage hors de portée du JavaScript,
liens profonds, présence en magasin d'applications.

---

## 1. Ce qui change entre le web et l'app

| | Web | App installée |
|---|---|---|
| Origine de la page | le domaine | `capacitor://localhost` (iOS), `http://localhost` (Android) |
| Adresses d'API | relatives (`/api/v1`) | absolues (`environment.mobile.ts`) |
| Images `/uploads/…` | relatives | préfixées par `MediaUrlPipe` |
| Jetons de session | `localStorage` | stockage natif privé (Preferences) |
| Durée de session | 7 jours, JWT sans état | 90 jours, jeton rotatif révocable |
| Notifications | Web Push (VAPID) | FCM / APNs |
| Rendu serveur | oui | non (bundle statique) |

Aucune de ces différences n'altère le comportement du site : les deux régimes
cohabitent côté backend, et `/auth/refresh` accepte les deux sortes de jetons.

---

## 2. Commandes

```powershell
cd ummati-front

npm run build:mobile    # bundle statique → dist/mobile/browser
npm run sync:mobile     # build + copie dans les projets natifs
npm run open:android    # ouvre Android Studio
npm run open:ios        # ouvre Xcode (macOS uniquement)
```

Les commandes du site sont inchangées : `npm start`, `npm run build`, `npm test`
ciblent explicitement le projet `ummati-front`.

> ⚠️ Après **toute** modification du front, `npm run sync:mobile` est nécessaire pour
> que les applications voient le changement — le bundle est copié dans les projets
> natifs, il n'est pas lu depuis `dist/`.

---

## 3. À configurer avant la première publication

### 3.1 Adresse de l'API — obligatoire

`ummati-front/src/environments/environment.mobile.ts` contient un domaine
d'exemple :

```ts
const API_HOST = 'https://ummati.example.org';
```

Cette valeur est **compilée dans l'application** : la changer impose de republier une
version. À renseigner avant toute compilation destinée aux magasins.

Le même domaine doit être repris dans `android/app/src/main/AndroidManifest.xml`
(`android:host` de l'`intent-filter` des liens profonds).

### 3.2 Notifications natives — Firebase

Les notifications de l'app passent par FCM, qui relaie lui-même vers APNs côté iOS.
Le Web Push ne peut pas s'y substituer : la WKWebView qui héberge l'app iOS ne
l'implémente pas.

1. Créer un projet Firebase, y déclarer les applications `org.takwa.ummati`.
2. **Android** : déposer `google-services.json` dans `ummati-front/android/app/`.
3. **iOS** : déposer `GoogleService-Info.plist` dans `ios/App/App/` via Xcode.
4. **Backend** : générer une clé de compte de service et l'exposer par variable
   d'environnement :

   ```
   FCM_CREDENTIALS_JSON={"type":"service_account", …}
   ```

Sans ces identifiants, le canal natif est simplement inactif : l'application démarre
et fonctionne, seules les notifications natives manquent.

### 3.3 iOS — étapes qui exigent un Mac

Le projet Xcode est généré et versionné, mais sa compilation, sa signature et sa
publication ne se font que sur macOS. Dans Xcode :

- **Signing & Capabilities** → ajouter *Push Notifications*.
- Ajouter *Background Modes* → cocher *Remote notifications*.
- Ajouter *Associated Domains* → `applinks:votre-domaine` pour les liens profonds.
- Renseigner l'équipe de signature et l'identifiant d'application.

Ces réglages modifient le projet Xcode ; ils n'ont pas été appliqués à l'aveugle
depuis Windows, où ils n'auraient pas pu être vérifiés.

### 3.4 Liens profonds — vérification de domaine

Pour qu'un lien s'ouvre directement dans l'app sans boîte de dialogue :

- **Android** : publier `https://votre-domaine/.well-known/assetlinks.json`.
- **iOS** : publier `https://votre-domaine/.well-known/apple-app-site-association`.

### 3.5 Icônes

`public/icon-192.png` et `public/icon-512.png` sont des **icônes d'attente**
(monogramme aux couleurs de la marque), suffisantes pour installer la version web
mais à remplacer par l'identité réelle avant publication. Les icônes natives se
régénèrent ensuite avec `@capacitor/assets`.

---

## 4. Configuration backend

| Variable | Rôle | Défaut |
|---|---|---|
| `FCM_CREDENTIALS_JSON` | Compte de service Firebase (JSON) | vide → canal natif inactif |
| `FCM_CREDENTIALS_PATH` | Variante par fichier | vide |
| `NATIVE_REFRESH_DAYS` | Durée des sessions installées | `90` |
| `APP_CORS_ALLOWED_ORIGINS` | Origines web autorisées | `http://localhost:4200` |

Les origines des webviews natives (`capacitor://localhost`, `ionic://localhost`,
`http://localhost`) sont ajoutées automatiquement par `SecurityConfig` : elles sont
figées par le conteneur natif et ne peuvent pas être revendiquées par un site
distant.

---

## 5. Sessions : ce qui a changé et pourquoi

Un refresh JWT de 7 jours non révocable convient à un navigateur que l'on referme.
Sur un téléphone, il impose une reconnexion hebdomadaire et laisse un appareil perdu
connecté jusqu'à son terme.

Les clients natifs reçoivent donc un **jeton opaque stocké en empreinte**, valable 90
jours et **remplacé à chaque usage**. Si un jeton déjà consommé est présenté, c'est
qu'il a été volé — le client légitime détient le dernier maillon : toute la lignée est
alors révoquée.

Conséquences pratiques :

- La déconnexion révoque réellement la session côté serveur.
- La réinitialisation du mot de passe coupe toutes les sessions natives.
- Le client mobile **doit** enregistrer le `refreshToken` renvoyé par
  `/auth/refresh` ; l'ignorer ferait rejouer un jeton mort et déconnecterait
  l'utilisateur (déjà géré par `AuthService`).

Le web est inchangé : il continue de recevoir un refresh JWT sans état, et
`/auth/refresh` accepte les deux régimes. Aucune session ouverte ne tombe au
déploiement.

---

## 6. Limites connues

- **La compilation iOS exige un Mac.** Le projet est prêt et versionné, mais
  `xcodebuild`, la signature et l'envoi à l'App Store ne fonctionnent pas sous
  Windows.
- **Les notifications natives ne fonctionnent pas tant que Firebase n'est pas
  configuré** (§ 3.2). Le reste de l'application est pleinement opérationnel.
- **`@capacitor/cli` embarque une dépendance `uuid` marquée vulnérable**
  (`GHSA-w5hq-g745-h8pq`). Il s'agit d'un outil de compilation : ce code n'entre pas
  dans les applications publiées.
- **Les jetons sont stockés via `@capacitor/preferences`** (`UserDefaults` /
  `SharedPreferences`), donc dans l'espace privé de l'application et hors de portée du
  JavaScript de la webview — mais pas dans le Keychain / Keystore chiffré. Le
  durcissement passerait par un greffon de stockage sécurisé ; la rotation et la
  révocation des jetons limitent déjà la portée d'une extraction.
