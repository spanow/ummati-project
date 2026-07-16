import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponse } from '../models/api.models';

export interface OrgAnnouncementResponse {
  id: string;
  orgId: string;
  authorId: string;
  authorFirstName: string;
  authorLastName: string;
  title: string;
  content: string;
  pinned: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface CreateOrgAnnouncementRequest {
  title: string;
  content: string;
  pinned: boolean;
}

@Injectable({ providedIn: 'root' })
export class OrgAnnouncementService {
  private readonly apiUrl = environment.apiUrl;

  constructor(private http: HttpClient) {}

  list(orgId: string): Observable<ApiResponse<OrgAnnouncementResponse[]>> {
    return this.http.get<ApiResponse<OrgAnnouncementResponse[]>>(
      `${this.apiUrl}/organizations/${orgId}/announcements`);
  }

  create(orgId: string, request: CreateOrgAnnouncementRequest): Observable<ApiResponse<OrgAnnouncementResponse>> {
    return this.http.post<ApiResponse<OrgAnnouncementResponse>>(
      `${this.apiUrl}/organizations/${orgId}/announcements`, request);
  }

  delete(orgId: string, announcementId: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/organizations/${orgId}/announcements/${announcementId}`);
  }
}
