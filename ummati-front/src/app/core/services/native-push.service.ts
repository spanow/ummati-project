import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { environment } from '../../../environments/environment';
import { PlatformService } from './platform.service';

/**
 * Notifications de l'application installée.
 *
 * <p>Pendant natif de {@link PushNotificationService}, qui reste en charge du Web
 * Push côté navigateur. Les deux ne sont pas interchangeables : la WKWebView qui
 * héberge l'app iOS n'implémente pas le Web Push, l'app passe donc obligatoirement
 * par FCM (relayé vers APNs par Firebase).
 *
 * <p>Le jeton obtenu est envoyé au backend, qui s'en sert comme adresse de livraison.
 */
@Injectable({ providedIn: 'root' })
export class NativePushService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);
  private readonly platform = inject(PlatformService);
  private readonly apiUrl = environment.apiUrl + '/push';

  /** Dernier jeton enregistré, conservé pour pouvoir le retirer à la déconnexion. */
  private currentToken: string | null = null;

  /**
   * Demande l'autorisation puis enregistre l'appareil.
   *
   * <p>N'est appelé qu'une fois l'utilisateur connecté : demander l'autorisation dès
   * le premier lancement, avant que la personne ait compris ce que l'app lui apporte,
   * est le meilleur moyen de se faire refuser définitivement — iOS ne repose jamais
   * la question.
   */
  async register(): Promise<void> {
    if (!this.platform.isNative) return;

    try {
      const { PushNotifications } = await import('@capacitor/push-notifications');

      let permission = await PushNotifications.checkPermissions();
      if (permission.receive === 'prompt' || permission.receive === 'prompt-with-rationale') {
        permission = await PushNotifications.requestPermissions();
      }
      if (permission.receive !== 'granted') return;

      // Les écouteurs sont posés avant l'inscription : le jeton est émis de façon
      // asynchrone et arriverait dans le vide si l'on s'abonnait après.
      await PushNotifications.removeAllListeners();

      await PushNotifications.addListener('registration', token => {
        void this.sendTokenToBackend(token.value);
      });

      await PushNotifications.addListener('registrationError', err => {
        console.warn('Enregistrement push impossible', err);
      });

      // Tap sur une notification : le backend a placé la destination dans « url ».
      await PushNotifications.addListener('pushNotificationActionPerformed', action => {
        const url = action.notification.data?.['url'];
        if (typeof url === 'string' && url.startsWith('/')) {
          void this.router.navigateByUrl(url);
        }
      });

      await PushNotifications.register();
    } catch (err) {
      // Les notifications sont un confort : leur échec ne doit pas dégrader l'app.
      console.warn('Notifications natives indisponibles', err);
    }
  }

  private async sendTokenToBackend(token: string): Promise<void> {
    this.currentToken = token;
    try {
      await firstValueFrom(this.http.post<void>(`${this.apiUrl}/device`, {
        token,
        platform: this.platform.platform,
        deviceId: this.platform.getDeviceId(),
      }));
    } catch (err) {
      console.warn('Enregistrement du jeton push refusé par le serveur', err);
    }
  }

  /** Retire l'appareil du compte qui se déconnecte. */
  async unregister(): Promise<void> {
    const token = this.currentToken;
    if (!token) return;
    this.currentToken = null;

    try {
      await firstValueFrom(this.http.delete<void>(
        `${this.apiUrl}/device?token=${encodeURIComponent(token)}`));
    } catch {
      /* le compte est de toute façon déconnecté localement */
    }

    try {
      const { PushNotifications } = await import('@capacitor/push-notifications');
      await PushNotifications.removeAllListeners();
    } catch {
      /* greffon absent */
    }
  }
}
