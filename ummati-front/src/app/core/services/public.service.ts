import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponse } from '../models/api.models';

export interface PublicStats {
  totalVolunteers: number;
  totalOrganizations: number;
  totalEvents: number;
  totalParticipations: number;
}

@Injectable({ providedIn: 'root' })
export class PublicService {
  private readonly apiUrl = environment.apiUrl + '/public';

  constructor(private http: HttpClient) {}

  getStats(): Observable<ApiResponse<PublicStats>> {
    return this.http.get<ApiResponse<PublicStats>>(`${this.apiUrl}/stats`);
  }
}
