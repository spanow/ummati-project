import { Injectable, inject, PLATFORM_ID } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';

export type DevicePlatform = 'IOS' | 'ANDROID' | 'WEB';

/**
 * Pont installé par le conteneur natif avant le chargement de l'application. Il est
 * lu directement plutôt qu'à travers `@capacitor/core` : importer ce paquet ici
 * l'ajouterait au bundle du site, qui ne s'en sert jamais.
 */
interface CapacitorBridge {
  isNativePlatform?: () => boolean;
  getPlatform?: () => string;
}

function bridge(): CapacitorBridge | undefined {
  return typeof window !== 'undefined'
    ? (window as unknown as { Capacitor?: CapacitorBridge }).Capacitor
    : undefined;
}

/**
 * Distingue le site de l'application installée.
 *
 * <p>Un seul code source alimente les deux, mais quelques comportements doivent
 * différer : le stockage des jetons, le canal de notifications, et la durée de
 * session accordée par le backend.
 *
 * <p>Les greffons Capacitor ne sont jamais importés statiquement — seulement chargés
 * à la demande dans les branches natives. Le bundle du site n'en embarque donc aucun,
 * et le rendu serveur, qui s'exécute dans Node, ne risque pas de les évaluer.
 */
@Injectable({ providedIn: 'root' })
export class PlatformService {
  private readonly isBrowser = isPlatformBrowser(inject(PLATFORM_ID));

  /** Vrai uniquement dans le conteneur natif iOS ou Android. */
  readonly isNative = this.isBrowser && bridge()?.isNativePlatform?.() === true;

  readonly platform: DevicePlatform = !this.isNative
    ? 'WEB'
    : bridge()?.getPlatform?.() === 'ios'
      ? 'IOS'
      : 'ANDROID';

  /**
   * Identifiant d'installation et nom d'appareil, renseignés au démarrage de l'app.
   * Ils ne servent qu'à décrire une session dans la liste des appareils connectés.
   */
  private deviceId: string | null = null;
  private deviceName: string | null = null;
  private appVersion: string | null = null;

  async loadDeviceInfo(): Promise<void> {
    if (!this.isNative) return;
    try {
      const { Device } = await import('@capacitor/device');
      const [id, info] = await Promise.all([Device.getId(), Device.getInfo()]);
      this.deviceId = id.identifier;
      this.deviceName = [info.manufacturer, info.model].filter(Boolean).join(' ') || info.model;

      const { App } = await import('@capacitor/app');
      const appInfo = await App.getInfo();
      this.appVersion = appInfo.version;
    } catch {
      // Simples métadonnées d'affichage : leur absence ne doit pas empêcher de se
      // connecter, le backend les accepte nulles.
    }
  }

  getDeviceId(): string | null {
    return this.deviceId;
  }

  /**
   * En-têtes lus par le backend pour décider du régime de session. Vides sur le web,
   * où le comportement historique est conservé tel quel.
   */
  deviceHeaders(): Record<string, string> {
    if (!this.isNative) return {};
    const headers: Record<string, string> = { 'X-Device-Platform': this.platform };
    if (this.deviceId) headers['X-Device-Id'] = this.deviceId;
    if (this.deviceName) headers['X-Device-Name'] = this.deviceName;
    if (this.appVersion) headers['X-App-Version'] = this.appVersion;
    return headers;
  }
}
