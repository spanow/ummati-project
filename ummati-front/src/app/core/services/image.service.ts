import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponse } from '../models/api.models';
import { EventPhoto } from './event.service';

/**
 * Visuels des ONG et des événements : logo, bannière, couverture, galerie.
 *
 * Le backend valide le type réel du fichier (signature binaire) et impose 5 Mo max ;
 * les contrôles côté client ne sont là que pour éviter un aller-retour inutile.
 */
@Injectable({ providedIn: 'root' })
export class ImageService {
  private readonly apiUrl = environment.apiUrl;

  /** Types acceptés par le backend. */
  static readonly ACCEPTED_TYPES = 'image/jpeg,image/png,image/webp';
  static readonly MAX_SIZE_BYTES = 5 * 1024 * 1024;

  constructor(private http: HttpClient) {}

  /** Message d'erreur si le fichier est refusé d'avance, sinon null. */
  static validate(file: File): string | null {
    if (!ImageService.ACCEPTED_TYPES.split(',').includes(file.type)) {
      return 'Formats acceptés : JPG, PNG et WebP.';
    }
    if (file.size > ImageService.MAX_SIZE_BYTES) {
      return 'L\'image ne doit pas dépasser 5 Mo.';
    }
    return null;
  }

  // --- ONG ---

  uploadOrgLogo(orgId: string, file: File): Observable<ApiResponse<{ logoUrl: string }>> {
    return this.http.post<ApiResponse<{ logoUrl: string }>>(
      `${this.apiUrl}/organizations/${orgId}/logo`, toFormData(file));
  }

  deleteOrgLogo(orgId: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/organizations/${orgId}/logo`);
  }

  uploadOrgBanner(orgId: string, file: File): Observable<ApiResponse<{ bannerUrl: string }>> {
    return this.http.post<ApiResponse<{ bannerUrl: string }>>(
      `${this.apiUrl}/organizations/${orgId}/banner`, toFormData(file));
  }

  deleteOrgBanner(orgId: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/organizations/${orgId}/banner`);
  }

  // --- Événements ---

  uploadEventCover(eventId: string, file: File): Observable<ApiResponse<{ coverUrl: string }>> {
    return this.http.post<ApiResponse<{ coverUrl: string }>>(
      `${this.apiUrl}/events/${eventId}/cover`, toFormData(file));
  }

  deleteEventCover(eventId: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/events/${eventId}/cover`);
  }

  listEventPhotos(eventId: string): Observable<ApiResponse<EventPhoto[]>> {
    return this.http.get<ApiResponse<EventPhoto[]>>(`${this.apiUrl}/events/${eventId}/photos`);
  }

  addEventPhoto(eventId: string, file: File, caption?: string): Observable<ApiResponse<EventPhoto>> {
    const form = toFormData(file);
    if (caption) form.append('caption', caption);
    return this.http.post<ApiResponse<EventPhoto>>(`${this.apiUrl}/events/${eventId}/photos`, form);
  }

  deleteEventPhoto(photoId: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/events/photos/${photoId}`);
  }
}

function toFormData(file: File): FormData {
  const form = new FormData();
  form.append('file', file);
  return form;
}
