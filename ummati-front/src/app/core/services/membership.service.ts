import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface MembershipResponse {
  id: string; userId: string; organizationId: string;
  firstName: string; lastName: string; photoUrl: string | null;
  role: string; status: string; motivation: string | null;
  joinedAt: string | null; createdAt: string;
}

export interface ApiResponse<T> { success: boolean; message: string | null; data: T; }
export interface PageResponse<T> {
  content: T[]; page: number; size: number;
  totalElements: number; totalPages: number; last: boolean;
}

@Injectable({ providedIn: 'root' })
export class MembershipService {
  private readonly apiUrl = environment.apiUrl;

  constructor(private http: HttpClient) {}

  listMembers(orgId: string, status?: string, page = 0, size = 20): Observable<ApiResponse<PageResponse<MembershipResponse>>> {
    let params: any = { page, size };
    if (status) params['status'] = status;
    return this.http.get<ApiResponse<PageResponse<MembershipResponse>>>(
      `${this.apiUrl}/organizations/${orgId}/memberships`, { params });
  }

  requestMembership(orgId: string, motivation?: string): Observable<ApiResponse<MembershipResponse>> {
    return this.http.post<ApiResponse<MembershipResponse>>(
      `${this.apiUrl}/organizations/${orgId}/memberships`, { motivation });
  }

  approve(membershipId: string): Observable<ApiResponse<MembershipResponse>> {
    return this.http.patch<ApiResponse<MembershipResponse>>(
      `${this.apiUrl}/memberships/${membershipId}`, { action: 'APPROVE' });
  }

  reject(membershipId: string): Observable<ApiResponse<MembershipResponse>> {
    return this.http.patch<ApiResponse<MembershipResponse>>(
      `${this.apiUrl}/memberships/${membershipId}`, { action: 'REJECT' });
  }

  changeRole(membershipId: string, role: string): Observable<ApiResponse<MembershipResponse>> {
    return this.http.patch<ApiResponse<MembershipResponse>>(
      `${this.apiUrl}/memberships/${membershipId}/role`, { role });
  }

  remove(membershipId: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/memberships/${membershipId}`);
  }
}

