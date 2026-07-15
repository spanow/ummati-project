import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponse, PageResponse } from '../models/api.models';

export interface CommentResponse {
  id: string;
  eventId: string;
  authorId: string;
  authorFirstName: string;
  authorLastName: string;
  authorPhotoUrl: string | null;
  content: string;
  createdAt: string;
  updatedAt: string;
}

@Injectable({ providedIn: 'root' })
export class EventCommentService {
  private readonly apiUrl = environment.apiUrl;

  constructor(private http: HttpClient) {}

  list(eventId: string, page = 0, size = 20): Observable<ApiResponse<PageResponse<CommentResponse>>> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<ApiResponse<PageResponse<CommentResponse>>>(
      `${this.apiUrl}/events/${eventId}/comments`, { params });
  }

  create(eventId: string, content: string): Observable<ApiResponse<CommentResponse>> {
    return this.http.post<ApiResponse<CommentResponse>>(
      `${this.apiUrl}/events/${eventId}/comments`, { content });
  }

  delete(eventId: string, commentId: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/events/${eventId}/comments/${commentId}`);
  }
}
