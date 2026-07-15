import { Injectable, signal, computed } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, tap, BehaviorSubject } from 'rxjs';
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

  refreshToken(): Observable<ApiResponse<{ accessToken: string; expiresIn: number }>> {
    const refreshToken = localStorage.getItem('refreshToken');
    return this.http.post<ApiResponse<{ accessToken: string; expiresIn: number }>>(
      `${this.apiUrl}/refresh`, null,
      { headers: { Authorization: `Bearer ${refreshToken}` } }
    ).pipe(tap(res => {
      if (res.data) {
        localStorage.setItem('accessToken', res.data.accessToken);
      }
    }));
  }

  forgotPassword(email: string): Observable<ApiResponse<void>> {
    return this.http.post<ApiResponse<void>>(`${this.apiUrl}/forgot-password`, { email });
  }

  resetPassword(token: string, newPassword: string): Observable<ApiResponse<void>> {
    return this.http.post<ApiResponse<void>>(`${this.apiUrl}/reset-password`, { token, newPassword });
  }

  logout(): void {
    this.http.post(`${this.apiUrl}/logout`, null).subscribe({ error: () => {} });
    localStorage.removeItem('accessToken');
    localStorage.removeItem('refreshToken');
    localStorage.removeItem('user');
    this.currentUser.set(null);
    this.router.navigate(['/login']);
  }

  getAccessToken(): string | null {
    return localStorage.getItem('accessToken');
  }

  updateOnboardingDone(): void {
    const user = this.currentUser();
    if (user) {
      const updated = { ...user, onboardingDone: true };
      this.currentUser.set(updated);
      localStorage.setItem('user', JSON.stringify(updated));
    }
  }

  private storeTokens(auth: AuthResponse): void {
    localStorage.setItem('accessToken', auth.accessToken);
    localStorage.setItem('refreshToken', auth.refreshToken);
    localStorage.setItem('user', JSON.stringify(auth.user));
  }

  private loadFromStorage(): void {
    const userJson = typeof localStorage !== 'undefined' ? localStorage.getItem('user') : null;
    if (userJson) {
      try {
        this.currentUser.set(JSON.parse(userJson));
      } catch { /* ignore */ }
    }
  }
}

