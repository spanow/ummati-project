import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponse, PageResponse } from '../models/api.models';

export type ReportTargetType = 'ORGANIZATION' | 'EVENT' | 'ORG_ANNOUNCEMENT';
export type ReportReason = 'SPAM' | 'INAPPROPRIATE_CONTENT' | 'FRAUD' | 'HARASSMENT' | 'OTHER';
export type ReportStatus = 'PENDING' | 'REVIEWED' | 'DISMISSED' | 'ACTION_TAKEN';

export interface CreateReportRequest {
  targetType: ReportTargetType;
  targetId: string;
  reason: ReportReason;
  description?: string;
}

export interface ResolveReportRequest {
  status: ReportStatus;
  resolutionNote?: string;
}

export interface ReportResponse {
  id: string;
  reporterId: string;
  reporterName: string;
  targetType: ReportTargetType;
  targetId: string;
  targetLabel: string;
  reason: ReportReason;
  description: string | null;
  status: ReportStatus;
  resolutionNote: string | null;
  reviewedByName: string | null;
  reviewedAt: string | null;
  createdAt: string;
}

export const REPORT_REASON_LABELS: Record<ReportReason, string> = {
  SPAM: 'Spam',
  INAPPROPRIATE_CONTENT: 'Contenu inapproprié',
  FRAUD: 'Fraude',
  HARASSMENT: 'Harcèlement',
  OTHER: 'Autre',
};

@Injectable({ providedIn: 'root' })
export class ReportService {
  private readonly apiUrl = environment.apiUrl;

  constructor(private http: HttpClient) {}

  create(request: CreateReportRequest): Observable<ApiResponse<ReportResponse>> {
    return this.http.post<ApiResponse<ReportResponse>>(`${this.apiUrl}/reports`, request);
  }

  list(status?: ReportStatus, page = 0, size = 20): Observable<ApiResponse<PageResponse<ReportResponse>>> {
    let params = new HttpParams().set('page', page).set('size', size);
    if (status) params = params.set('status', status);
    return this.http.get<ApiResponse<PageResponse<ReportResponse>>>(`${this.apiUrl}/admin/reports`, { params });
  }

  resolve(reportId: string, request: ResolveReportRequest): Observable<ApiResponse<ReportResponse>> {
    return this.http.patch<ApiResponse<ReportResponse>>(`${this.apiUrl}/admin/reports/${reportId}`, request);
  }
}
