import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponse, PageResponse } from './organization.service';

export interface EventSummary {
  id: string; title: string; type: string; locationCity: string;
  online: boolean; startDate: string; endDate: string;
  maxParticipants: number | null; registeredCount: number;
  status: string; organizationName: string; organizationSlug: string;
}

export interface EventDetail {
  id: string; title: string; description: string; objectives: string;
  type: string; locationName: string; locationAddress: string;
  locationCity: string; locationZip: string; locationLat: number; locationLng: number;
  online: boolean; onlineLink: string;
  startDate: string; endDate: string; registrationDeadline: string;
  maxParticipants: number | null; minAge: number | null;
  status: string; cancellationReason: string;
  organizationId: string; organizationName: string; organizationSlug: string;
  registeredCount: number; waitlistedCount: number; availableSpots: number | null;
  requiredSkills: { id: string; name: string; category: string }[];
  feedbackAvgRating: number | null;
  createdAt: string;
  currentUserSignupStatus: string | null;
}

export interface SignupResponse {
  id: string; eventId: string; userId: string;
  userFirstName: string; userLastName: string; userEmail: string;
  status: string; registeredAt: string; attendedAt: string | null;
}

export interface FeedbackResponse {
  id: string; eventId: string; rating: number; comment: string;
  anonymous: boolean; userFirstName: string | null; userLastName: string | null;
  createdAt: string;
}

export interface FeedbackListResponse {
  feedbacks: PageResponse<FeedbackResponse>;
  averageRating: number | null;
}

@Injectable({ providedIn: 'root' })
export class EventService {
  private readonly apiUrl = environment.apiUrl;

  constructor(private http: HttpClient) {}

  listEvents(params: {
    page?: number; size?: number; type?: string; city?: string;
    orgId?: string; online?: boolean; from?: string; to?: string; skillId?: string;
  } = {}): Observable<ApiResponse<PageResponse<EventSummary>>> {
    let httpParams = new HttpParams();
    if (params.page != null) httpParams = httpParams.set('page', params.page);
    if (params.size != null) httpParams = httpParams.set('size', params.size);
    if (params.type) httpParams = httpParams.set('type', params.type);
    if (params.city) httpParams = httpParams.set('city', params.city);
    if (params.orgId) httpParams = httpParams.set('orgId', params.orgId);
    if (params.online != null) httpParams = httpParams.set('online', params.online);
    if (params.from) httpParams = httpParams.set('from', params.from);
    if (params.to) httpParams = httpParams.set('to', params.to);
    if (params.skillId) httpParams = httpParams.set('skillId', params.skillId);
    return this.http.get<ApiResponse<PageResponse<EventSummary>>>(`${this.apiUrl}/events`, { params: httpParams });
  }

  getEvent(id: string): Observable<ApiResponse<EventDetail>> {
    return this.http.get<ApiResponse<EventDetail>>(`${this.apiUrl}/events/${id}`);
  }

  listOrgEvents(orgId: string, page = 0, size = 50): Observable<ApiResponse<PageResponse<EventSummary>>> {
    return this.http.get<ApiResponse<PageResponse<EventSummary>>>(
      `${this.apiUrl}/organizations/${orgId}/events`,
      { params: new HttpParams().set('page', page).set('size', size) }
    );
  }

  createEvent(orgId: string, data: any): Observable<ApiResponse<EventDetail>> {
    return this.http.post<ApiResponse<EventDetail>>(`${this.apiUrl}/organizations/${orgId}/events`, data);
  }

  updateEvent(id: string, data: any): Observable<ApiResponse<EventDetail>> {
    return this.http.put<ApiResponse<EventDetail>>(`${this.apiUrl}/events/${id}`, data);
  }

  changeStatus(id: string, data: { status: string; reason?: string }): Observable<ApiResponse<EventDetail>> {
    return this.http.patch<ApiResponse<EventDetail>>(`${this.apiUrl}/events/${id}/status`, data);
  }

  signup(eventId: string): Observable<ApiResponse<SignupResponse>> {
    return this.http.post<ApiResponse<SignupResponse>>(`${this.apiUrl}/events/${eventId}/signups`, {});
  }

  cancelSignup(eventId: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/events/${eventId}/signups`);
  }

  listSignups(eventId: string, page = 0, size = 20): Observable<ApiResponse<PageResponse<SignupResponse>>> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<ApiResponse<PageResponse<SignupResponse>>>(`${this.apiUrl}/events/${eventId}/signups`, { params });
  }

  exportSignupsCsv(eventId: string): Observable<Blob> {
    return this.http.get(`${this.apiUrl}/events/${eventId}/signups/export`, { responseType: 'blob' });
  }

  markAttendance(eventId: string, userIds: string[]): Observable<void> {
    return this.http.patch<void>(`${this.apiUrl}/events/${eventId}/signups/attendance`, { userIds });
  }

  createFeedback(eventId: string, data: { rating: number; comment?: string; anonymous: boolean }): Observable<ApiResponse<FeedbackResponse>> {
    return this.http.post<ApiResponse<FeedbackResponse>>(`${this.apiUrl}/events/${eventId}/feedbacks`, data);
  }

  listFeedbacks(eventId: string, page = 0, size = 10): Observable<ApiResponse<FeedbackListResponse>> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<ApiResponse<FeedbackListResponse>>(`${this.apiUrl}/events/${eventId}/feedbacks`, { params });
  }
}

