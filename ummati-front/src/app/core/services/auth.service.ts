import { Injectable, signal, computed, inject, PLATFORM_ID } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, tap, shareReplay, finalize } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponse } from '../models/api.models';
import { TokenStorageService } from './token-storage.service';
import { PlatformService } from './platform.service';
import { NativePushService } from './native-push.service';

export interface UserSummary {
  id: string;
  email: string;
  firstName: string;
  lastName: string;
  role: string;
  onboardingDone: boolean;
}

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  expiresIn: number;
  tokenType: string;
  user: UserSummary;
}

/**
 * Réponse de /auth/refresh.
 *
 * <p>refreshToken n'est renseigné que pour les sessions natives, qui tournent à
 * chaque usage : il faut alors remplacer celui que l'on détient. Le web reçoit un
 * champ absent et conserve son jeton jusqu'à son terme.
 */
export interface TokenRefreshResponse {
  accessToken: string;
  expiresIn: number;
  refreshToken?: string | null;
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly apiUrl = environment.apiUrl + '/auth';
  private currentUser = signal<UserSummary | null>(null);

  readonly user = this.currentUser.asReadonly();
  readonly isLoggedIn = computed(() => this.currentUser() !== null);
  readonly isAdmin = computed(() => this.currentUser()?.role === 'PLATFORM_ADMIN');

  private readonly isBrowser = isPlatformBrowser(inject(PLATFORM_ID));

  /**
   * Rafraîchissement en cours, partagé entre tous les appelants.
   *
   * Sans ce partage, N requêtes recevant un 401 en même temps déclenchaient N appels à
   * /auth/refresh : le rate limiter (20 req/min sur /auth/**) répondait 429, l'intercepteur
   * traitait cet échec comme un refresh invalide et déconnectait l'utilisateur.
   */
  private refresh$: Observable<ApiResponse<TokenRefreshResponse>> | null = null;

  private readonly storage = inject(TokenStorageService);
  private readonly platform = inject(PlatformService);
  // Ce service ne charge les greffons Capacitor que dynamiquement : l'injecter ici
  // ne fait pas entrer le code natif dans le bundle du site.
  private readonly nativePush = inject(NativePushService);

  constructor(private http: HttpClient, private router: Router) {
    this.loadFromStorage();
  }

  /**
   * Relit la session après hydratation du stockage natif, qui est asynchrone et n'est
   * donc pas encore disponible à la construction du service (cf. initNativeShell).
   */
  reloadFromStorage(): void {
    this.loadFromStorage();
  }

  register(data: { email: string; password: string; firstName: string; lastName: string }): Observable<ApiResponse<any>> {
    return this.http.post<ApiResponse<any>>(`${this.apiUrl}/register`, data);
  }

  login(email: string, password: string): Observable<ApiResponse<AuthResponse>> {
    // Les en-têtes d'appareil sont vides sur le web : la requête y est identique à
    // celle d'avant. Sur mobile, elles valent au client une session longue.
    return this.http.post<ApiResponse<AuthResponse>>(`${this.apiUrl}/login`, { email, password },
      { headers: this.platform.deviceHeaders() })
      .pipe(tap(res => {
        if (res.data) {
          this.storeTokens(res.data);
          this.currentUser.set(res.data.user);
          // Le bon moment pour demander l'autorisation : la personne vient de se
          // connecter, la question a un sens. Sans effet sur le web.
          void this.nativePush.register();
        }
      }));
  }

  /** Un seul appel réseau, quel que soit le nombre de requêtes en attente. */
  refreshToken(): Observable<ApiResponse<TokenRefreshResponse>> {
    if (this.refresh$) {
      return this.refresh$;
    }

    const refreshToken = this.readStorage('refreshToken');
    this.refresh$ = this.http.post<ApiResponse<TokenRefreshResponse>>(
      `${this.apiUrl}/refresh`, null,
      { headers: { ...this.platform.deviceHeaders(), Authorization: `Bearer ${refreshToken}` } }
    ).pipe(
      tap(res => {
        if (res.data) {
          this.writeStorage('accessToken', res.data.accessToken);
          // Session native : le jeton vient d'être consommé et remplacé. Ne pas
          // enregistrer son successeur ferait rejouer un jeton mort au refresh
          // suivant — le backend y voit une session volée et révoque tout, ce qui
          // déconnecterait l'utilisateur sans raison apparente.
          if (res.data.refreshToken) {
            this.writeStorage('refreshToken', res.data.refreshToken);
          }
        }
      }),
      // Libère le verrou pour qu'un 401 ultérieur puisse déclencher un nouveau refresh.
      finalize(() => { this.refresh$ = null; }),
      shareReplay({ bufferSize: 1, refCount: false }),
    );

    return this.refresh$;
  }

  forgotPassword(email: string): Observable<ApiResponse<void>> {
    return this.http.post<ApiResponse<void>>(`${this.apiUrl}/forgot-password`, { email });
  }

  resetPassword(token: string, newPassword: string): Observable<ApiResponse<void>> {
    return this.http.post<ApiResponse<void>>(`${this.apiUrl}/reset-password`, { token, newPassword });
  }

  logout(): void {
    // Le refresh token accompagne la déconnexion pour que le serveur révoque
    // réellement la session : sur mobile elle survivrait sinon 90 jours.
    const refreshToken = this.readStorage('refreshToken');
    const headers: Record<string, string> = refreshToken ? { 'X-Refresh-Token': refreshToken } : {};
    this.http.post(`${this.apiUrl}/logout`, null, { headers }).subscribe({ error: () => {} });

    // Détache l'appareil des notifications : sans cela, le téléphone continuerait de
    // recevoir celles du compte quitté — cas courant d'un appareil partagé.
    void this.nativePush.unregister();

    this.removeStorage('accessToken');
    this.removeStorage('refreshToken');
    this.removeStorage('user');
    this.refresh$ = null;
    this.currentUser.set(null);
    this.router.navigate(['/login']);
  }

  getAccessToken(): string | null {
    return this.readStorage('accessToken');
  }

  updateOnboardingDone(): void {
    const user = this.currentUser();
    if (user) {
      const updated = { ...user, onboardingDone: true };
      this.currentUser.set(updated);
      this.writeStorage('user', JSON.stringify(updated));
    }
  }

  private storeTokens(auth: AuthResponse): void {
    this.writeStorage('accessToken', auth.accessToken);
    this.writeStorage('refreshToken', auth.refreshToken);
    this.writeStorage('user', JSON.stringify(auth.user));
  }

  private loadFromStorage(): void {
    const userJson = this.readStorage('user');
    if (userJson) {
      try {
        this.currentUser.set(JSON.parse(userJson));
      } catch { /* ignore */ }
    }
  }

  // localStorage n'existe pas pendant le rendu serveur : l'intercepteur appelle
  // getAccessToken() sur chaque requête, y compris côté SSR, où un accès direct
  // lèverait une ReferenceError et ferait échouer le rendu de la page.

  private readStorage(key: string): string | null {
    return this.isBrowser ? this.storage.read(key) : null;
  }

  private writeStorage(key: string, value: string): void {
    if (this.isBrowser) this.storage.write(key, value);
  }

  private removeStorage(key: string): void {
    if (this.isBrowser) this.storage.remove(key);
  }

}

