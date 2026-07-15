import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponse, PageResponse } from '../models/api.models';

export interface AdminStats {
  totalUsers: number; totalOrganizations: number; totalEvents: number;
  pendingOrganizations: number; registrationsThisWeek: number; eventsThisMonth: number;
}
export interface AdminUserSummary {
  id: string; email: string; firstName: string; lastName: string;
  role: string; emailVerified: boolean; enabled: boolean; createdAt: string;
}

@Injectable({ providedIn: 'root' })
export class AdminService {
  private readonly apiUrl = environment.apiUrl + '/admin';
  constructor(private http: HttpClient) {}

  getStats(): Observable<ApiResponse<AdminStats>> {
    return this.http.get<ApiResponse<AdminStats>>(`${this.apiUrl}/stats`);
  }

  listUsers(search = '', enabled?: boolean, page = 0, size = 20): Observable<ApiResponse<PageResponse<AdminUserSummary>>> {
    let params = new HttpParams().set('page', page).set('size', size);
    if (search) params = params.set('search', search);
    if (enabled !== undefined) params = params.set('enabled', enabled);
    return this.http.get<ApiResponse<PageResponse<AdminUserSummary>>>(`${this.apiUrl}/users`, { params });
  }

  changeUserStatus(userId: string, enabled: boolean): Observable<ApiResponse<AdminUserSummary>> {
    return this.http.patch<ApiResponse<AdminUserSummary>>(`${this.apiUrl}/users/${userId}/status`, { enabled });
  }

  listOrganizations(status?: string, page = 0, size = 20): Observable<ApiResponse<any>> {
    let params = new HttpParams().set('page', page).set('size', size);
    if (status) params = params.set('status', status);
    return this.http.get<ApiResponse<any>>(`${this.apiUrl}/organizations`, { params });
  }
}

