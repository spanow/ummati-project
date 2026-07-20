import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponse } from '../models/api.models';

export interface DocumentItem {
  id: string; name: string; fileType: string; fileSize: number;
  uploadedBy: string; createdAt: string; downloadUrl: string;
}

@Injectable({ providedIn: 'root' })
export class DocumentService {
  private base = environment.apiUrl;
  constructor(private http: HttpClient) {}

  listByOrg(orgId: string): Observable<ApiResponse<DocumentItem[]>> {
    return this.http.get<ApiResponse<DocumentItem[]>>(`${this.base}/organizations/${orgId}/documents`);
  }

  upload(orgId: string, file: File): Observable<ApiResponse<DocumentItem>> {
    const form = new FormData();
    form.append('file', file);
    return this.http.post<ApiResponse<DocumentItem>>(`${this.base}/organizations/${orgId}/documents`, form);
  }

  listByEvent(eventId: string): Observable<ApiResponse<DocumentItem[]>> {
    return this.http.get<ApiResponse<DocumentItem[]>>(`${this.base}/events/${eventId}/documents`);
  }

  uploadToEvent(eventId: string, file: File): Observable<ApiResponse<DocumentItem>> {
    const form = new FormData();
    form.append('file', file);
    return this.http.post<ApiResponse<DocumentItem>>(`${this.base}/events/${eventId}/documents`, form);
  }

  /** Suppression : endpoint générique par id de document (org & event). */
  delete(docId: string): Observable<void> {
    return this.http.delete<void>(`${this.base}/documents/${docId}`);
  }

  downloadUrl(docId: string): string {
    return `${this.base}/documents/${docId}/download`;
  }
}

