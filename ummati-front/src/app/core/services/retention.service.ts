import { Injectable, signal } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponse, PageResponse } from '../models/api.models';
import { EventSummary } from './event.service';
import { OrganizationSummary } from './organization.service';

export interface MissionAlert {
  id: string;
  label: string;
  city: string | null;
  lat: number | null;
  lng: number | null;
  radiusKm: number | null;
  domains: string[];
  types: string[];
  frequency: 'DAILY' | 'WEEKLY';
  enabled: boolean;
  lastSentAt: string | null;
  createdAt: string;
}

export interface NotificationPreferences {
  emailNewMissions: boolean;
  emailReminders: boolean;
  emailMemberships: boolean;
  emailAnnouncements: boolean;
}

/**
 * Ce qui donne une raison de revenir : missions mises de côté, associations suivies,
 * alertes, et le réglage des emails qui va avec.
 */
@Injectable({ providedIn: 'root' })
export class RetentionService {
  private readonly apiUrl = environment.apiUrl;

  /**
   * Missions en favori connues du client.
   *
   * Un signal partagé plutôt qu'un appel par carte : la liste marque ses cœurs en
   * une requête, et une bascule depuis le détail se reflète immédiatement dans la
   * liste sans rechargement.
   */
  private readonly favoriteIds = signal<ReadonlySet<string>>(new Set());
  readonly favorites = this.favoriteIds.asReadonly();

  constructor(private http: HttpClient) {}

  isFavorite(eventId: string): boolean {
    return this.favoriteIds().has(eventId);
  }

  /** Alimente le cache local à partir d'une page de résultats déjà marquée. */
  primeFavorites(ids: string[]): void {
    this.favoriteIds.set(new Set(ids));
  }

  toggleFavorite(eventId: string, next: boolean): Observable<unknown> {
    // Bascule optimiste : le cœur répond au clic, l'appel suit. En cas d'échec on
    // revient à l'état précédent (cf. composants appelants).
    this.setLocalFavorite(eventId, next);
    const url = `${this.apiUrl}/events/${eventId}/favorite`;
    return next ? this.http.put(url, {}) : this.http.delete(url);
  }

  setLocalFavorite(eventId: string, value: boolean): void {
    const copy = new Set(this.favoriteIds());
    if (value) copy.add(eventId); else copy.delete(eventId);
    this.favoriteIds.set(copy);
  }

  listFavorites(page = 0, size = 10): Observable<ApiResponse<PageResponse<EventSummary>>> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<ApiResponse<PageResponse<EventSummary>>>(
      `${this.apiUrl}/profile/favorites`, { params }
    ).pipe(tap(res => {
      // La page des favoris fait autorité : tout ce qu'elle contient est un favori.
      const ids = new Set(this.favoriteIds());
      res.data.content.forEach(e => ids.add(e.id));
      this.favoriteIds.set(ids);
    }));
  }

  // --- Associations suivies ---

  followState(orgId: string): Observable<ApiResponse<{ following: boolean; followerCount: number }>> {
    return this.http.get<ApiResponse<{ following: boolean; followerCount: number }>>(
      `${this.apiUrl}/organizations/${orgId}/follow/me`);
  }

  toggleFollow(orgId: string, next: boolean): Observable<unknown> {
    const url = `${this.apiUrl}/organizations/${orgId}/follow`;
    return next ? this.http.put(url, {}) : this.http.delete(url);
  }

  listFollowed(page = 0, size = 12): Observable<ApiResponse<PageResponse<OrganizationSummary>>> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<ApiResponse<PageResponse<OrganizationSummary>>>(
      `${this.apiUrl}/profile/following`, { params });
  }

  // --- Alertes ---

  listAlerts(): Observable<ApiResponse<MissionAlert[]>> {
    return this.http.get<ApiResponse<MissionAlert[]>>(`${this.apiUrl}/profile/alerts`);
  }

  createAlert(data: Partial<MissionAlert>): Observable<ApiResponse<MissionAlert>> {
    return this.http.post<ApiResponse<MissionAlert>>(`${this.apiUrl}/profile/alerts`, data);
  }

  updateAlert(id: string, data: Partial<MissionAlert>): Observable<ApiResponse<MissionAlert>> {
    return this.http.put<ApiResponse<MissionAlert>>(`${this.apiUrl}/profile/alerts/${id}`, data);
  }

  deleteAlert(id: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/profile/alerts/${id}`);
  }

  // --- Préférences d'email ---

  getPreferences(): Observable<ApiResponse<NotificationPreferences>> {
    return this.http.get<ApiResponse<NotificationPreferences>>(
      `${this.apiUrl}/profile/notification-preferences`);
  }

  /** @param category NEW_MISSIONS | REMINDERS | MEMBERSHIPS | ANNOUNCEMENTS */
  updatePreference(category: string, value: boolean): Observable<ApiResponse<NotificationPreferences>> {
    return this.http.put<ApiResponse<NotificationPreferences>>(
      `${this.apiUrl}/profile/notification-preferences`, { [category]: value });
  }
}
