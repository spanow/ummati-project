import type { CapacitorConfig } from '@capacitor/cli';

/**
 * Conteneur natif des applications iOS et Android.
 *
 * <p>L'app embarque le même bundle Angular que le site : il n'existe pas de seconde
 * implémentation des écrans à maintenir. Ce qui diffère tient aux capacités que le
 * navigateur ne donne pas — notifications natives, stockage hors de portée du
 * JavaScript, liens profonds.
 *
 * <p>webDir pointe sur la sortie de la configuration « mobile » (voir angular.json),
 * qui produit un bundle purement statique : le rendu serveur du site n'a pas de sens
 * ici, l'app étant servie depuis le téléphone lui-même.
 */
const config: CapacitorConfig = {
  appId: 'org.takwa.ummati',
  appName: 'Ummati',
  webDir: 'dist/mobile/browser',

  android: {
    // Le trafic en clair reste interdit : l'API est jointe en HTTPS, y compris
    // depuis l'émulateur.
    allowMixedContent: false,
  },

  ios: {
    // La barre d'état reste lisible quel que soit le thème choisi dans l'app.
    contentInset: 'automatic',
  },

  plugins: {
    SplashScreen: {
      launchShowDuration: 1500,
      backgroundColor: '#ffffff',
      showSpinner: false,
    },
    PushNotifications: {
      // Le son et le badge sont demandés dès l'inscription : sur iOS, obtenir ces
      // autorisations après coup impose un second passage par les réglages système.
      presentationOptions: ['badge', 'sound', 'alert'],
    },
  },
};

export default config;
