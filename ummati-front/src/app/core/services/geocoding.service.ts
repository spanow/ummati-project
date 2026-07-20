import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map, catchError, of } from 'rxjs';

/** Résultat de géocodage normalisé (indépendant du fournisseur). */
export interface GeoResult {
  lat: number;
  lng: number;
  /** Libellé complet lisible (display_name Nominatim). */
  label: string;
  street?: string;
  city?: string;
  zip?: string;
  country?: string;
}

interface NominatimItem {
  lat: string;
  lon: string;
  display_name: string;
  address?: {
    house_number?: string;
    road?: string;
    city?: string;
    town?: string;
    village?: string;
    municipality?: string;
    postcode?: string;
    country?: string;
  };
}

/**
 * Géocodage 100 % open-source via Nominatim (OpenStreetMap) — sans clé ni compte.
 *
 * ⚠️ Politique d'usage de l'instance publique : ~1 requête/seconde, attribution requise.
 * Convient au dev / MVP associatif ; pour un fort trafic prod, auto-héberger Nominatim
 * ou passer par un fournisseur (il suffira de changer BASE_URL).
 */
@Injectable({ providedIn: 'root' })
export class GeocodingService {
  private http = inject(HttpClient);
  private static readonly BASE_URL = 'https://nominatim.openstreetmap.org';

  /** Adresse -> coordonnées (autocomplete). Renvoie [] en cas d'erreur (jamais bloquant). */
  search(query: string, limit = 5): Observable<GeoResult[]> {
    const q = query.trim();
    if (q.length < 3) return of([]);
    const url = `${GeocodingService.BASE_URL}/search`
      + `?format=jsonv2&addressdetails=1&limit=${limit}`
      + `&countrycodes=fr,dz&accept-language=fr`
      + `&q=${encodeURIComponent(q)}`;
    return this.http.get<NominatimItem[]>(url).pipe(
      map(items => (items ?? []).map(GeocodingService.normalize)),
      catchError(() => of([])),
    );
  }

  /** Coordonnées -> adresse (géocodage inverse, sur clic/déplacement du marqueur). */
  reverse(lat: number, lng: number): Observable<GeoResult | null> {
    const url = `${GeocodingService.BASE_URL}/reverse`
      + `?format=jsonv2&addressdetails=1&accept-language=fr`
      + `&lat=${lat}&lon=${lng}`;
    return this.http.get<NominatimItem>(url).pipe(
      map(item => (item ? GeocodingService.normalize(item) : null)),
      catchError(() => of(null)),
    );
  }

  private static normalize(item: NominatimItem): GeoResult {
    const a = item.address ?? {};
    const street = [a.house_number, a.road].filter(Boolean).join(' ') || undefined;
    return {
      lat: parseFloat(item.lat),
      lng: parseFloat(item.lon),
      label: item.display_name,
      street,
      city: a.city || a.town || a.village || a.municipality,
      zip: a.postcode,
      country: a.country,
    };
  }
}
