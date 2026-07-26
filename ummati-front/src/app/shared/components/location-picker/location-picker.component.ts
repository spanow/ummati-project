import {
  Component, ElementRef, afterNextRender, effect, inject, input, output,
  signal, viewChild, PLATFORM_ID,
} from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Subject, debounceTime, distinctUntilChanged, switchMap } from 'rxjs';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import type * as LeafletNS from 'leaflet';
import { GeocodingService, GeoResult } from '../../../core/services/geocoding.service';
import { TPipe } from '../../pipes/t.pipe';

/** Centre par défaut : centre approximatif de la France métropolitaine. */
const DEFAULT_CENTER: [number, number] = [46.6, 2.4];
const DEFAULT_ZOOM = 5;
const PIN_ZOOM = 15;

/**
 * Sélecteur de localisation open-source (Leaflet + OpenStreetMap + Nominatim), sans clé ni compte.
 * - Éditable : recherche d'adresse (autocomplete), marqueur déplaçable, clic-pour-placer, autofill inverse.
 * - Lecture seule : affiche la position + lien "Itinéraire".
 * SSR-safe : la carte n'est initialisée que dans le navigateur.
 */
@Component({
  selector: 'app-location-picker',
  standalone: true,
  imports: [FormsModule, MatIconModule, MatProgressSpinnerModule, TPipe],
  template: `
    @if (editable()) {
      <div class="lp-search">
        <mat-icon aria-hidden="true">search</mat-icon>
        <input type="text" [(ngModel)]="query" (ngModelChange)="onQueryChange($event)"
               (focus)="showResults.set(true)"
               [placeholder]="'Rechercher une adresse…' | t"
               [attr.aria-label]="'Rechercher une adresse' | t" autocomplete="off" />
        @if (searching()) { <mat-spinner diameter="18" /> }
        @if (showResults() && results().length > 0) {
          <ul class="lp-results" role="listbox">
            @for (r of results(); track r.label) {
              <li role="option" (mousedown)="selectResult(r)">{{ r.label }}</li>
            }
          </ul>
        }
      </div>
    }

    <div #mapEl class="lp-map" [class.readonly]="!editable()"
         role="application" [attr.aria-label]="'Carte de localisation' | t"></div>

    @if (editable()) {
      <p class="lp-hint">
        <mat-icon aria-hidden="true">touch_app</mat-icon>
        {{ 'Cliquez sur la carte ou déplacez le marqueur pour définir la position.' | t }}
      </p>
    } @else if (hasCoords()) {
      <a class="lp-directions" [href]="directionsUrl()" target="_blank" rel="noopener">
        <mat-icon aria-hidden="true">directions</mat-icon> {{ 'Itinéraire' | t }}
      </a>
    }
  `,
  styles: [`
    :host { display: block; }
    .lp-search { position: relative; display: flex; align-items: center; gap: 8px;
      border: 1px solid var(--brand-border); border-radius: var(--radius-md); padding: 8px 12px;
      background: var(--brand-surface); margin-bottom: 10px; }
    .lp-search mat-icon { color: var(--brand-text-soft); font-size: 20px; width: 20px; height: 20px; }
    .lp-search input { flex: 1; border: none; outline: none; font-size: 0.95rem; background: transparent; color: var(--brand-ink); }
    .lp-results { position: absolute; top: calc(100% + 4px); inset-inline: 0; z-index: 1000;
      list-style: none; margin: 0; padding: 4px; background: var(--brand-surface); border: 1px solid var(--brand-border);
      border-radius: var(--radius-md); box-shadow: var(--brand-shadow-md); max-height: 240px; overflow-y: auto; }
    .lp-results li { padding: 8px 10px; border-radius: var(--radius-xs); cursor: pointer; font-size: 0.88rem; line-height: 1.4; }
    .lp-results li:hover { background: var(--brand-primary-soft); }
    .lp-map { height: 300px; width: 100%; border-radius: var(--radius-md); overflow: hidden; z-index: 0;
      border: 1px solid var(--brand-border); }
    .lp-map.readonly { height: 240px; }
    .lp-hint { display: flex; align-items: center; gap: 6px; color: var(--brand-text-soft); font-size: 0.82rem; margin: 8px 0 0; }
    .lp-hint mat-icon { font-size: 16px; width: 16px; height: 16px; }
    .lp-directions { display: inline-flex; align-items: center; gap: 6px; margin-top: 10px;
      color: var(--brand-primary-dark); font-weight: 600; text-decoration: none; font-size: 0.9rem; }
    .lp-directions mat-icon { font-size: 18px; width: 18px; height: 18px; }
    :host ::ng-deep .lp-pin { display: flex; align-items: center; justify-content: center; }
  `],
})
export class LocationPickerComponent {
  private geocoding = inject(GeocodingService);
  private platformId = inject(PLATFORM_ID);

  // Coordonnées (source de vérité côté parent pour le mode lecture seule)
  readonly lat = input<number | null>(null);
  readonly lng = input<number | null>(null);
  readonly editable = input(true);

  // Sorties : coordonnées choisies + adresse résolue (pour autofill du formulaire)
  readonly coordsChange = output<{ lat: number; lng: number }>();
  readonly addressResolved = output<GeoResult>();

  query = '';
  readonly results = signal<GeoResult[]>([]);
  readonly searching = signal(false);
  readonly showResults = signal(false);

  private mapEl = viewChild.required<ElementRef<HTMLDivElement>>('mapEl');
  private L: typeof LeafletNS | null = null;
  private map: LeafletNS.Map | null = null;
  private marker: LeafletNS.Marker | null = null;
  private search$ = new Subject<string>();

  hasCoords = signal(false);

  constructor() {
    // Autocomplete débounced (respecte la limite ~1 req/s de Nominatim)
    this.search$.pipe(
      debounceTime(400),
      distinctUntilChanged(),
      switchMap(q => { this.searching.set(true); return this.geocoding.search(q); }),
    ).subscribe(res => {
      this.searching.set(false);
      this.results.set(res);
    });

    // Init carte (navigateur uniquement — SSR-safe)
    afterNextRender(() => this.initMap());

    // Réagit aux coordonnées poussées par le parent (mode lecture seule notamment)
    effect(() => {
      const la = this.lat();
      const ln = this.lng();
      this.hasCoords.set(la != null && ln != null);
      if (this.map && la != null && ln != null) {
        // Ne zoome qu'au premier placement — sinon un drag réinitialiserait le zoom
        this.placeMarker(la, ln, this.marker ? undefined : PIN_ZOOM, false);
      }
    });
  }

  private async initMap() {
    if (!isPlatformBrowser(this.platformId)) return;
    const mod = await import('leaflet');
    const L = ((mod as any).default ?? mod) as typeof LeafletNS;
    this.L = L;

    const la = this.lat();
    const ln = this.lng();
    const hasInitial = la != null && ln != null;

    this.map = L.map(this.mapEl().nativeElement, {
      center: hasInitial ? [la!, ln!] : DEFAULT_CENTER,
      zoom: hasInitial ? PIN_ZOOM : DEFAULT_ZOOM,
      scrollWheelZoom: this.editable(),
    });

    L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {
      attribution: '© <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors',
      maxZoom: 19,
    }).addTo(this.map);

    if (hasInitial) this.placeMarker(la!, ln!, PIN_ZOOM, false);

    if (this.editable()) {
      this.map.on('click', (e: LeafletNS.LeafletMouseEvent) => {
        this.placeMarker(e.latlng.lat, e.latlng.lng, undefined, true);
      });
    }

    // Leaflet peut mal calculer sa taille si le conteneur était masqué (onglet, etc.)
    setTimeout(() => this.map?.invalidateSize(), 0);
  }

  private divIcon() {
    return this.L!.divIcon({
      className: 'lp-pin',
      html: `<svg width="32" height="40" viewBox="0 0 32 40" xmlns="http://www.w3.org/2000/svg">
        <path d="M16 0C7.2 0 0 7.2 0 16c0 11 16 24 16 24s16-13 16-24C32 7.2 24.8 0 16 0z" fill="#e53935"/>
        <circle cx="16" cy="16" r="6" fill="#fff"/></svg>`,
      iconSize: [32, 40],
      iconAnchor: [16, 40],
    });
  }

  private placeMarker(lat: number, lng: number, zoom: number | undefined, emit: boolean) {
    if (!this.map || !this.L) return;
    if (!this.marker) {
      this.marker = this.L.marker([lat, lng], {
        icon: this.divIcon(),
        draggable: this.editable(),
      }).addTo(this.map);
      if (this.editable()) {
        this.marker.on('dragend', () => {
          const p = this.marker!.getLatLng();
          this.emitCoords(p.lat, p.lng);
          this.reverseFill(p.lat, p.lng);
        });
      }
    } else {
      this.marker.setLatLng([lat, lng]);
    }
    this.map.setView([lat, lng], zoom ?? this.map.getZoom());
    this.hasCoords.set(true);
    if (emit) {
      this.emitCoords(lat, lng);
      this.reverseFill(lat, lng);
    }
  }

  private emitCoords(lat: number, lng: number) {
    this.coordsChange.emit({ lat: round(lat), lng: round(lng) });
  }

  private reverseFill(lat: number, lng: number) {
    this.geocoding.reverse(lat, lng).subscribe(r => { if (r) this.addressResolved.emit(r); });
  }

  onQueryChange(q: string) {
    this.showResults.set(true);
    this.search$.next(q);
  }

  selectResult(r: GeoResult) {
    this.query = r.label;
    this.showResults.set(false);
    this.results.set([]);
    this.addressResolved.emit(r);
    this.placeMarker(r.lat, r.lng, PIN_ZOOM, false);
    this.emitCoords(r.lat, r.lng);
  }

  directionsUrl(): string {
    const la = this.lat(), ln = this.lng();
    return `https://www.openstreetmap.org/?mlat=${la}&mlon=${ln}#map=16/${la}/${ln}`;
  }
}

/** Arrondi à 6 décimales (~0,11 m) — suffisant et compatible avec la précision en base. */
function round(n: number): number {
  return Math.round(n * 1e6) / 1e6;
}
