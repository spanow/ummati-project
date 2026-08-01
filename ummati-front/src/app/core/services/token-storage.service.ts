import { Injectable, inject } from '@angular/core';
import { PlatformService } from './platform.service';

const KEYS = ['accessToken', 'refreshToken', 'user'] as const;

/**
 * Stockage des jetons de session, adapté au support.
 *
 * <p>Sur le web, l'implémentation reste exactement celle d'avant : un accès direct à
 * localStorage. Dans l'app installée, les jetons migrent vers le stockage natif, hors
 * de portée du JavaScript de la webview — un point d'autant plus important que la
 * session native dure 90 jours là où celle du navigateur en dure 7.
 *
 * <p>L'API reste synchrone. Le stockage natif, lui, est asynchrone : les valeurs sont
 * donc chargées une fois au démarrage dans un cache mémoire qui sert ensuite toutes
 * les lectures, les écritures étant répercutées en arrière-plan. Sans ce cache, il
 * aurait fallu rendre asynchrones {@code getAccessToken()} et tout l'intercepteur
 * HTTP — une réécriture à haut risque de régression pour un besoin propre au mobile.
 */
@Injectable({ providedIn: 'root' })
export class TokenStorageService {
  private readonly platform = inject(PlatformService);
  private readonly cache = new Map<string, string>();

  /**
   * Recharge le cache depuis le stockage natif. Appelé une fois au démarrage, avant
   * que quoi que ce soit ne lise une session.
   */
  async hydrate(): Promise<void> {
    if (!this.platform.isNative) return;
    try {
      const { Preferences } = await import('@capacitor/preferences');
      const entries = await Promise.all(
        KEYS.map(async key => [key, (await Preferences.get({ key })).value] as const),
      );
      for (const [key, value] of entries) {
        if (value !== null) this.cache.set(key, value);
      }
    } catch {
      // Cache vide : l'utilisateur devra se reconnecter, ce qui reste préférable à
      // une app qui refuse de démarrer.
    }
  }

  read(key: string): string | null {
    if (this.platform.isNative) return this.cache.get(key) ?? null;
    // localStorage n'existe pas pendant le rendu serveur : l'intercepteur lit le
    // jeton à chaque requête, y compris côté SSR, où un accès direct lèverait une
    // ReferenceError et ferait échouer le rendu.
    return this.isWebBrowser() ? localStorage.getItem(key) : null;
  }

  write(key: string, value: string): void {
    if (this.platform.isNative) {
      this.cache.set(key, value);
      void this.persist(key, value);
      return;
    }
    if (this.isWebBrowser()) localStorage.setItem(key, value);
  }

  remove(key: string): void {
    if (this.platform.isNative) {
      this.cache.delete(key);
      void this.forget(key);
      return;
    }
    if (this.isWebBrowser()) localStorage.removeItem(key);
  }

  private isWebBrowser(): boolean {
    return typeof localStorage !== 'undefined';
  }

  private async persist(key: string, value: string): Promise<void> {
    try {
      const { Preferences } = await import('@capacitor/preferences');
      await Preferences.set({ key, value });
    } catch {
      // Le cache mémoire garde la session utilisable pour la durée d'exécution.
    }
  }

  private async forget(key: string): Promise<void> {
    try {
      const { Preferences } = await import('@capacitor/preferences');
      await Preferences.remove({ key });
    } catch {
      /* le cache a déjà été vidé */
    }
  }
}
