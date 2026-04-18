import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponse, PageResponse } from './organization.service';

export interface NotificationItem {
  id: string; type: string; title: string; message: string;
  read: boolean; link: string | null; createdAt: string;
}

@Injectable({ providedIn: 'root' })
export class NotificationService {
  private readonly apiUrl = environment.apiUrl + '/notifications';
  constructor(private http: HttpClient) {}

  list(page = 0, size = 20): Observable<ApiResponse<PageResponse<NotificationItem>>> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<ApiResponse<PageResponse<NotificationItem>>>(this.apiUrl, { params });
  }

  unreadCount(): Observable<ApiResponse<{ count: number }>> {
    return this.http.get<ApiResponse<{ count: number }>>(`${this.apiUrl}/unread-count`);
  }

  markAsRead(id: string): Observable<void> {
    return this.http.patch<void>(`${this.apiUrl}/${id}/read`, {});
  }

  markAllAsRead(): Observable<void> {
    return this.http.patch<void>(`${this.apiUrl}/read-all`, {});
  }
}

