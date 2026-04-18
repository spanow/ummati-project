import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponse, PageResponse } from './organization.service';

export interface VolunteerDashboard {
  firstName: string; onboardingDone: boolean;
  upcomingEvents: any[]; myOrganizations: any[];
  suggestedEvents: any[]; stats: { eventsAttended: number; organizationsJoined: number };
}

export interface OrgAdminDashboard {
  organizationName: string; activeMembers: number; pendingRequests: number;
  eventsThisMonth: number; totalEvents: number; averageFeedbackRating: number | null;
  recentMembers: { firstName: string; lastName: string; role: string; joinedAt: string }[];
}

export interface AdminStats {
  totalUsers: number; totalOrganizations: number; totalEvents: number;
  pendingOrganizations: number; registrationsThisWeek: number; eventsThisMonth: number;
}

export interface UserSummary {
  id: string; email: string; firstName: string; lastName: string;
  role: string; emailVerified: boolean; enabled: boolean; createdAt: string;
}

@Injectable({ providedIn: 'root' })
export class DashboardService {
  private readonly apiUrl = environment.apiUrl;
  constructor(private http: HttpClient) {}

  getVolunteerDashboard(): Observable<ApiResponse<VolunteerDashboard>> {
    return this.http.get<ApiResponse<VolunteerDashboard>>(`${this.apiUrl}/dashboard/volunteer`);
  }

  getOrgAdminDashboard(orgId: string): Observable<ApiResponse<OrgAdminDashboard>> {
    return this.http.get<ApiResponse<OrgAdminDashboard>>(`${this.apiUrl}/dashboard/org-admin/${orgId}`);
  }
}

@Injectable({ providedIn: 'root' })
export class AdminApiService {
  private readonly apiUrl = environment.apiUrl + '/admin';
  constructor(private http: HttpClient) {}

  getStats(): Observable<ApiResponse<AdminStats>> {
    return this.http.get<ApiResponse<AdminStats>>(`${this.apiUrl}/stats`);
  }

  listUsers(params: { page?: number; size?: number; search?: string; enabled?: boolean } = {}): Observable<ApiResponse<PageResponse<UserSummary>>> {
    let httpParams = new HttpParams();
    if (params.page != null) httpParams = httpParams.set('page', params.page);
    if (params.size != null) httpParams = httpParams.set('size', params.size);
    if (params.search) httpParams = httpParams.set('search', params.search);
    if (params.enabled != null) httpParams = httpParams.set('enabled', params.enabled);
    return this.http.get<ApiResponse<PageResponse<UserSummary>>>(`${this.apiUrl}/users`, { params: httpParams });
  }

  changeUserStatus(id: string, enabled: boolean): Observable<ApiResponse<UserSummary>> {
    return this.http.patch<ApiResponse<UserSummary>>(`${this.apiUrl}/users/${id}/status`, { enabled });
  }

  listOrganizations(params: { page?: number; status?: string } = {}): Observable<ApiResponse<PageResponse<any>>> {
    let httpParams = new HttpParams();
    if (params.page != null) httpParams = httpParams.set('page', params.page);
    if (params.status) httpParams = httpParams.set('status', params.status);
    return this.http.get<ApiResponse<PageResponse<any>>>(`${this.apiUrl}/organizations`, { params: httpParams });
  }
}

