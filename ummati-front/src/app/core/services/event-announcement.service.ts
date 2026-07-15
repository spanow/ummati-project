import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponse } from '../models/api.models';

export interface AnnouncementResponse {
  id: string;
  eventId: string;
  authorId: string;
  authorFirstName: string;
  authorLastName: string;
  content: string;
  pinned: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface CreateAnnouncementRequest {
  content: string;
  pinned: boolean;
}

@Injectable({ providedIn: 'root' })
export class EventAnnouncementService {
  private readonly apiUrl = environment.apiUrl;

  constructor(private http: HttpClient) {}

  list(eventId: string): Observable<ApiResponse<AnnouncementResponse[]>> {
    return this.http.get<ApiResponse<AnnouncementResponse[]>>(`${this.apiUrl}/events/${eventId}/announcements`);
  }

  create(eventId: string, request: CreateAnnouncementRequest): Observable<ApiResponse<AnnouncementResponse>> {
    return this.http.post<ApiResponse<AnnouncementResponse>>(`${this.apiUrl}/events/${eventId}/announcements`, request);
  }

  update(eventId: string, id: string, request: CreateAnnouncementRequest): Observable<ApiResponse<AnnouncementResponse>> {
    return this.http.put<ApiResponse<AnnouncementResponse>>(`${this.apiUrl}/events/${eventId}/announcements/${id}`, request);
  }

  delete(eventId: string, id: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/events/${eventId}/announcements/${id}`);
  }
}
