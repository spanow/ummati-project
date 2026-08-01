import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { PlatformService } from './services/platform.service';
import { TokenStorageService } from './services/token-storage.service';
import { AuthService } from './services/auth.service';
import { NativePushService } from './services/native-push.service';

/**
 * Mise en route de l'application installée.
 *
 * <p>Exécuté au démarrage, avant l'affichage du premier écran. Sur le web, la
 * fonction sort immédiatement : rien n'est chargé, rien ne change.
 *
 * <p>L'ordre compte. Le stockage natif est asynchrone, donc la session n'est pas
 * encore lisible quand AuthService se construit : on hydrate d'abord, on redemande
 * ensuite au service de relire. Sans cela, l'app s'ouvrirait sur l'écran de connexion
 * à chaque lancement alors que la session est valide.
 */
export async function initNativeShell(): Promise<void> {
  const platform = inject(PlatformService);
  const storage = inject(TokenStorageService);
  const auth = inject(AuthService);
  const push = inject(NativePushService);
  const router = inject(Router);

  if (!platform.isNative) return;

  await platform.loadDeviceInfo();
  await storage.hydrate();
  auth.reloadFromStorage();

  await applyNativeChrome();
  await wireDeepLinks(router);

  // L'autorisation n'est demandée qu'à un utilisateur déjà connecté : la question a
  // alors un sens pour lui, et sur iOS elle ne se repose jamais après un refus.
  if (auth.isLoggedIn()) {
    void push.register();
  }
}

/** Barre d'état et clavier : réglages d'apparence propres au natif. */
async function applyNativeChrome(): Promise<void> {
  try {
    const { StatusBar, Style } = await import('@capacitor/status-bar');
    await StatusBar.setStyle({ style: Style.Default });
  } catch {
    /* Android sans barre configurable, ou greffon absent */
  }

  try {
    const { Keyboard, KeyboardResize } = await import('@capacitor/keyboard');
    // Le clavier redimensionne la vue plutôt que de la recouvrir : sans cela, le
    // champ en cours de saisie passe sous le clavier dans les formulaires longs
    // (création de mission, questionnaire d'adhésion).
    await Keyboard.setResizeMode({ mode: KeyboardResize.Native });
  } catch {
    /* iOS uniquement pour certaines options */
  }

  try {
    const { SplashScreen } = await import('@capacitor/splash-screen');
    await SplashScreen.hide();
  } catch {
    /* rien à masquer */
  }
}

/**
 * Liens profonds : ouvrir une mission depuis un email ou un partage doit mener à
 * l'écran correspondant dans l'app, pas à la page d'accueil.
 */
async function wireDeepLinks(router: Router): Promise<void> {
  try {
    const { App } = await import('@capacitor/app');

    await App.addListener('appUrlOpen', event => {
      try {
        const url = new URL(event.url);
        const path = url.pathname + url.search;
        if (path && path !== '/') void router.navigateByUrl(path);
      } catch {
        /* URL non exploitable : on reste où l'on est */
      }
    });

    // Le bouton retour d'Android doit remonter la navigation de l'app, et ne quitter
    // celle-ci qu'à la racine — sinon la première pression ferme l'application.
    await App.addListener('backButton', ({ canGoBack }) => {
      if (canGoBack) {
        window.history.back();
      } else {
        void App.exitApp();
      }
    });
  } catch {
    /* greffon absent */
  }
}
