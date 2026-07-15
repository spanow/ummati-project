import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponse, PageResponse } from '../models/api.models';

export interface OrganizationSummary {
  id: string; name: string; slug: string; domain: string;
  logoUrl: string; city: string; memberCount: number; descriptionExcerpt: string; status?: string;
}

export interface OrganizationDetail {
  id: string; name: string; slug: string; description: string; mission: string;
  domain: string; logoUrl: string; bannerUrl: string;
  addressStreet: string; addressCity: string; addressZip: string; addressCountry: string;
  phone: string; email: string; website: string;
  status: string; rejectionReason: string;
  stats: { memberCount: number; eventCount: number; averageRating: number | null };
  createdAt: string;
}

@Injectable({ providedIn: 'root' })
export class OrganizationService {
  private readonly apiUrl = environment.apiUrl + '/organizations';

  constructor(private http: HttpClient) {}

  list(params: { page?: number; size?: number; domain?: string; city?: string; search?: string } = {}): Observable<ApiResponse<PageResponse<OrganizationSummary>>> {
    let httpParams = new HttpParams();
    if (params.page != null) httpParams = httpParams.set('page', params.page);
    if (params.size != null) httpParams = httpParams.set('size', params.size);
    if (params.domain) httpParams = httpParams.set('domain', params.domain);
    if (params.city) httpParams = httpParams.set('city', params.city);
    if (params.search) httpParams = httpParams.set('search', params.search);
    return this.http.get<ApiResponse<PageResponse<OrganizationSummary>>>(this.apiUrl, { params: httpParams });
  }

  getBySlug(slug: string): Observable<ApiResponse<OrganizationDetail>> {
    return this.http.get<ApiResponse<OrganizationDetail>>(`${this.apiUrl}/${slug}`);
  }

  create(data: any): Observable<ApiResponse<OrganizationDetail>> {
    return this.http.post<ApiResponse<OrganizationDetail>>(this.apiUrl, data);
  }

  update(id: string, data: any): Observable<ApiResponse<OrganizationDetail>> {
    return this.http.put<ApiResponse<OrganizationDetail>>(`${this.apiUrl}/${id}`, data);
  }

  changeStatus(id: string, status: string, reason: string): Observable<ApiResponse<OrganizationDetail>> {
    return this.http.patch<ApiResponse<OrganizationDetail>>(`${this.apiUrl}/${id}/status`, { status, reason });
  }
}

