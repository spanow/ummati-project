import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponse } from '../models/api.models';

export interface ProfileResponse {
  id: string; email: string; firstName: string; lastName: string;
  phone: string | null; dateOfBirth: string | null; photoUrl: string | null; bio: string | null;
  address: { street: string | null; city: string | null; zip: string | null; country: string | null };
  skills: { id: string; name: string; category: string }[];
  stats: { organizationCount: number; eventsAttended: number; volunteerHours: number };
  onboardingDone: boolean; emailVerified: boolean; createdAt: string;
  profilePublic: boolean;
}

/** Passeport bénévole : la vitrine d'un bénévole (missions, heures, causes). */
export interface VolunteerPassport {
  userId: string;
  firstName: string;
  /** Nom complet sur son propre passeport, initiale seule en vue publique. */
  lastName: string;
  photoUrl: string | null;
  bio: string | null;
  city: string | null;
  memberSince: string;
  missionsCompleted: number;
  hoursTotal: number;
  organizationCount: number;
  skills: { id: string; name: string; category: string }[];
  causes: { domain: string; missionCount: number }[];
  recentMissions: {
    eventId: string; title: string;
    organizationName: string; organizationSlug: string; date: string;
  }[];
  profilePublic: boolean;
}

@Injectable({ providedIn: 'root' })
export class ProfileService {
  private readonly apiUrl = environment.apiUrl + '/profile';

  constructor(private http: HttpClient) {}

  get(): Observable<ApiResponse<ProfileResponse>> {
    return this.http.get<ApiResponse<ProfileResponse>>(this.apiUrl);
  }

  update(data: any): Observable<ApiResponse<ProfileResponse>> {
    return this.http.put<ApiResponse<ProfileResponse>>(this.apiUrl, data);
  }

  uploadPhoto(file: File): Observable<ApiResponse<{ photoUrl: string }>> {
    const form = new FormData();
    form.append('file', file);
    return this.http.post<ApiResponse<{ photoUrl: string }>>(`${this.apiUrl}/photo`, form);
  }

  changePassword(currentPassword: string, newPassword: string): Observable<ApiResponse<void>> {
    return this.http.put<ApiResponse<void>>(`${this.apiUrl}/password`, { currentPassword, newPassword });
  }

  delete(password: string): Observable<void> {
    return this.http.delete<void>(this.apiUrl, { body: { password } });
  }

  // --- Passeport bénévole ---

  getMyPassport(): Observable<ApiResponse<VolunteerPassport>> {
    return this.http.get<ApiResponse<VolunteerPassport>>(`${this.apiUrl}/passport`);
  }

  setPassportVisibility(isPublic: boolean): Observable<ApiResponse<VolunteerPassport>> {
    return this.http.put<ApiResponse<VolunteerPassport>>(
      `${this.apiUrl}/passport/visibility`, { public: isPublic });
  }

  /** Passeport public d'un bénévole — 404 s'il ne l'a pas publié. */
  getPublicPassport(userId: string): Observable<ApiResponse<VolunteerPassport>> {
    return this.http.get<ApiResponse<VolunteerPassport>>(
      `${environment.apiUrl}/public/volunteers/${userId}`);
  }
}

