import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponse } from '../models/api.models';

interface VapidPublicKeyResponse {
  publicKey: string;
}

@Injectable({ providedIn: 'root' })
export class PushNotificationService {
  private readonly apiUrl = environment.apiUrl + '/push';

  constructor(private http: HttpClient) {}

  isSupported(): boolean {
    return typeof window !== 'undefined' && 'serviceWorker' in navigator && 'PushManager' in window;
  }

  async getPermissionState(): Promise<NotificationPermission | 'unsupported'> {
    if (!this.isSupported()) return 'unsupported';
    return Notification.permission;
  }

  async isSubscribed(): Promise<boolean> {
    if (!this.isSupported()) return false;
    const registration = await navigator.serviceWorker.getRegistration('/push-sw.js');
    if (!registration) return false;
    const subscription = await registration.pushManager.getSubscription();
    return !!subscription;
  }

  async subscribe(): Promise<void> {
    if (!this.isSupported()) throw new Error('Les notifications push ne sont pas supportées par ce navigateur.');

    const permission = await Notification.requestPermission();
    if (permission !== 'granted') throw new Error('Permission refusée pour les notifications.');

    const registration = await navigator.serviceWorker.register('/push-sw.js');
    await navigator.serviceWorker.ready;

    const { publicKey } = await firstValueFrom(
      this.http.get<ApiResponse<VapidPublicKeyResponse>>(`${this.apiUrl}/vapid-public-key`)
    ).then(res => res.data);

    const subscription = await registration.pushManager.subscribe({
      userVisibleOnly: true,
      applicationServerKey: this.urlBase64ToUint8Array(publicKey) as BufferSource,
    });

    const json = subscription.toJSON();
    await firstValueFrom(this.http.post<void>(`${this.apiUrl}/subscribe`, {
      endpoint: json.endpoint,
      keys: { p256dh: json.keys?.['p256dh'], auth: json.keys?.['auth'] },
    }));
  }

  async unsubscribe(): Promise<void> {
    if (!this.isSupported()) return;
    const registration = await navigator.serviceWorker.getRegistration('/push-sw.js');
    const subscription = await registration?.pushManager.getSubscription();
    if (!subscription) return;

    const endpoint = subscription.endpoint;
    await subscription.unsubscribe();
    await firstValueFrom(this.http.delete<void>(`${this.apiUrl}/subscribe`, { params: { endpoint } }));
  }

  private urlBase64ToUint8Array(base64String: string): Uint8Array {
    const padding = '='.repeat((4 - (base64String.length % 4)) % 4);
    const base64 = (base64String + padding).replace(/-/g, '+').replace(/_/g, '/');
    const rawData = window.atob(base64);
    return Uint8Array.from([...rawData].map(c => c.charCodeAt(0)));
  }
}
