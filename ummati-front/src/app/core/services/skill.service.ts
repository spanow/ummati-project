import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponse } from '../models/api.models';

export interface Skill { id: string; name: string; category: string; }
@Injectable({ providedIn: 'root' })
export class SkillService {
  private readonly apiUrl = environment.apiUrl + '/skills';
  constructor(private http: HttpClient) {}

  getAll(): Observable<ApiResponse<Skill[]>> {
    return this.http.get<ApiResponse<Skill[]>>(this.apiUrl);
  }

  getByCategory(category: string): Observable<ApiResponse<Skill[]>> {
    return this.http.get<ApiResponse<Skill[]>>(this.apiUrl, { params: { category } });
  }
}

