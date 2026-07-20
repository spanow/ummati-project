import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponse, PageResponse } from '../models/api.models';

export interface MembershipResponse {
  id: string; userId: string; organizationId: string; organizationName: string;
  organizationSlug: string;
  firstName: string; lastName: string; photoUrl: string | null;
  role: string; status: string; motivation: string | null;
  joinedAt: string | null; createdAt: string;
}

export interface MembershipQuestion {
  id: string; label: string; type: string; options: string[]; required: boolean; position: number;
}

export interface MembershipAnswer {
  questionId: string; questionLabel: string; type: string; value: string;
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

  getMyMembership(orgId: string): Observable<ApiResponse<MembershipResponse>> {
    return this.http.get<ApiResponse<MembershipResponse>>(
      `${this.apiUrl}/organizations/${orgId}/memberships/me`);
  }

  requestMembership(orgId: string, motivation?: string,
                    answers?: { questionId: string; value: string }[]): Observable<ApiResponse<MembershipResponse>> {
    return this.http.post<ApiResponse<MembershipResponse>>(
      `${this.apiUrl}/organizations/${orgId}/memberships`, { motivation, answers });
  }

  // Questionnaire d'adhésion configurable par l'ONG
  listQuestions(orgId: string): Observable<ApiResponse<MembershipQuestion[]>> {
    return this.http.get<ApiResponse<MembershipQuestion[]>>(
      `${this.apiUrl}/organizations/${orgId}/membership-questions`);
  }

  createQuestion(orgId: string, data: { label: string; type: string; options?: string[]; required: boolean }): Observable<ApiResponse<MembershipQuestion>> {
    return this.http.post<ApiResponse<MembershipQuestion>>(
      `${this.apiUrl}/organizations/${orgId}/membership-questions`, data);
  }

  deleteQuestion(questionId: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/membership-questions/${questionId}`);
  }

  getAnswers(membershipId: string): Observable<ApiResponse<MembershipAnswer[]>> {
    return this.http.get<ApiResponse<MembershipAnswer[]>>(`${this.apiUrl}/memberships/${membershipId}/answers`);
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

  listByUser(status = 'ACTIVE', page = 0, size = 10): Observable<ApiResponse<PageResponse<MembershipResponse>>> {
    return this.http.get<ApiResponse<PageResponse<MembershipResponse>>>(
      `${this.apiUrl}/profile/memberships`, { params: { status, page, size } });
  }
}

