import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponse } from '../models/api.models';

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


