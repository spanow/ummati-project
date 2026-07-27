import { Injectable, signal, computed, inject, PLATFORM_ID } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, tap, shareReplay, finalize } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponse } from '../models/api.models';

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
  private refresh$: Observable<ApiResponse<{ accessToken: string; expiresIn: number }>> | null = null;

  constructor(private http: HttpClient, private router: Router) {
    this.loadFromStorage();
  }

  register(data: { email: string; password: string; firstName: string; lastName: string }): Observable<ApiResponse<any>> {
    return this.http.post<ApiResponse<any>>(`${this.apiUrl}/register`, data);
  }

  login(email: string, password: string): Observable<ApiResponse<AuthResponse>> {
    return this.http.post<ApiResponse<AuthResponse>>(`${this.apiUrl}/login`, { email, password })
      .pipe(tap(res => {
        if (res.data) {
          this.storeTokens(res.data);
          this.currentUser.set(res.data.user);
        }
      }));
  }

  /** Un seul appel réseau, quel que soit le nombre de requêtes en attente. */
  refreshToken(): Observable<ApiResponse<{ accessToken: string; expiresIn: number }>> {
    if (this.refresh$) {
      return this.refresh$;
    }

    const refreshToken = this.readStorage('refreshToken');
    this.refresh$ = this.http.post<ApiResponse<{ accessToken: string; expiresIn: number }>>(
      `${this.apiUrl}/refresh`, null,
      { headers: { Authorization: `Bearer ${refreshToken}` } }
    ).pipe(
      tap(res => {
        if (res.data) {
          this.writeStorage('accessToken', res.data.accessToken);
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
    this.http.post(`${this.apiUrl}/logout`, null).subscribe({ error: () => {} });
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
    return this.isBrowser ? localStorage.getItem(key) : null;
  }

  private writeStorage(key: string, value: string): void {
    if (this.isBrowser) localStorage.setItem(key, value);
  }

  private removeStorage(key: string): void {
    if (this.isBrowser) localStorage.removeItem(key);
  }
}

