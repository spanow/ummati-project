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
}

