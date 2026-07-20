import { Component, signal, computed, inject, OnInit, PLATFORM_ID } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatChipsModule } from '@angular/material/chips';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { DatePipe } from '@angular/common';
import { EventService, EventSummary } from '../../../core/services/event.service';
import { EVENT_TYPES } from '../../../core/constants/event-types';
import { TPipe } from '../../../shared/pipes/t.pipe';

@Component({
  selector: 'app-event-list',
  standalone: true,
  imports: [MatCardModule, MatButtonModule, MatIconModule, MatFormFieldModule, MatInputModule,
    MatSelectModule, MatChipsModule, MatPaginatorModule, MatProgressSpinnerModule, MatCheckboxModule,
    RouterLink, FormsModule, DatePipe, TPipe],
  template: `
    <div class="page-container">
      <header class="page-header">
        <div>
          <h1>{{ 'Événements' | t }}</h1>
          <p class="subtitle">{{ 'Trouvez une mission de bénévolat près de chez vous' | t }}</p>
        </div>
      </header>

      <div class="filters">
        <mat-form-field appearance="outline" class="search-field">
          <mat-label>{{ 'Ville' | t }}</mat-label>
          <input matInput [(ngModel)]="cityFilter" (keyup.enter)="loadEvents()"
                 placeholder="Lyon, Paris..." />
          <mat-icon matSuffix>location_on</mat-icon>
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>{{ 'Type' | t }}</mat-label>
          <mat-select [(ngModel)]="typeFilter" (selectionChange)="loadEvents()">
            <mat-option [value]="null">{{ 'Tous' | t }}</mat-option>
            @for (t of eventTypes; track t.value) {
              <mat-option [value]="t.value">{{ t.label }}</mat-option>
            }
          </mat-select>
        </mat-form-field>
        <mat-checkbox [(ngModel)]="onlineOnly" (change)="loadEvents()">{{ 'En ligne uniquement' | t }}</mat-checkbox>
        <button mat-stroked-button type="button" class="near-btn" [class.active]="userLoc()"
                [disabled]="locating()" (click)="toggleNearMe()">
          @if (locating()) { <mat-spinner diameter="18" /> } @else { <mat-icon>my_location</mat-icon> }
          {{ (userLoc() ? 'Trié par distance' : 'Près de chez moi') | t }}
        </button>
      </div>
      @if (geoError()) { <p class="geo-error">{{ geoError() }}</p> }

      @if (loading()) {
        <div class="loading"><mat-spinner diameter="40" /></div>
      } @else if (events().length === 0) {
        <div class="empty-state">
          <mat-icon class="empty-icon">event_busy</mat-icon>
          <h3>{{ 'Aucun événement trouvé' | t }}</h3>
          <p>{{ 'Essayez d\\'ajuster vos filtres ou revenez plus tard !' | t }}</p>
        </div>
      } @else {
        <div class="event-grid">
          @for (event of displayedEvents(); track event.id) {
            <mat-card class="event-card" [routerLink]="['/events', event.id]">
              <mat-card-content>
                <div class="event-top">
                  <mat-chip class="type-chip">{{ event.type }}</mat-chip>
                  @if (event.online) {
                    <mat-chip class="online-chip">🌐 {{ 'En ligne' | t }}</mat-chip>
                  }
                  @if (distanceKm(event) !== null) {
                    <mat-chip class="dist-chip"><mat-icon>near_me</mat-icon> {{ distanceLabel(event) }}</mat-chip>
                  }
                </div>
                <h3 class="event-title">{{ event.title }}</h3>
                <div class="event-meta">
                  <span><mat-icon class="meta-icon">calendar_today</mat-icon> {{ event.startDate | date:'d MMM yyyy, HH:mm' }}</span>
                  <span><mat-icon class="meta-icon">location_on</mat-icon> {{ event.locationCity }}</span>
                </div>
                <div class="event-org">
                  <mat-icon class="meta-icon">business</mat-icon> {{ event.organizationName }}
                </div>
                <div class="event-spots">
                  @if (event.maxParticipants) {
                    <div class="progress-container">
                      <div class="progress-bar" [style.width.%]="(event.registeredCount / event.maxParticipants) * 100"></div>
                    </div>
                    <span class="spots-text">
                      {{ event.registeredCount }}/{{ event.maxParticipants }} {{ 'inscrits' | t }}
                      @if (event.registeredCount >= event.maxParticipants) {
                        <span class="full-badge">{{ 'Complet' | t }}</span>
                      } @else if (event.registeredCount >= event.maxParticipants * 0.8) {
                        <span class="almost-full-badge">{{ 'Presque complet' | t }}</span>
                      }
                    </span>
                  } @else {
                    <span class="spots-text">{{ event.registeredCount }} {{ 'inscrits' | t }}</span>
                  }
                </div>
              </mat-card-content>
            </mat-card>
          }
        </div>
        <mat-paginator [length]="totalElements()" [pageSize]="10" [pageIndex]="currentPage()"
                       (page)="onPage($event)" />
      }
    </div>
  `,
  styles: [`
    .page-container { max-width: 1200px; margin: 0 auto; padding: 32px 24px; }
    .page-header { margin-bottom: 32px; }
    .page-header h1 { font-size: 2rem; font-weight: 600; margin: 0; }
    .subtitle { color: #666; margin-top: 4px; font-size: 1.05rem; }
    .filters { display: flex; gap: 16px; margin-bottom: 24px; flex-wrap: wrap; align-items: center; }
    .search-field { flex: 1; min-width: 200px; }
    .near-btn.active { background: var(--brand-primary-100); color: var(--brand-primary-dark); border-color: var(--brand-primary); }
    .geo-error { color: #c62828; font-size: 0.85rem; margin: -12px 0 16px; }
    .dist-chip { font-size: 11px; background: var(--brand-primary-100) !important; color: var(--brand-primary-dark) !important; }
    .dist-chip mat-icon { font-size: 14px; width: 14px; height: 14px; vertical-align: middle; }
    .loading { display: flex; justify-content: center; padding: 80px 0; }
    .empty-state { text-align: center; padding: 80px 24px; }
    .empty-icon { font-size: 64px; width: 64px; height: 64px; color: #ccc; }
    .empty-state h3 { margin-top: 16px; font-size: 1.3rem; }
    .empty-state p { color: #666; }
    .event-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(340px, 1fr)); gap: 24px; }
    .event-card { cursor: pointer; transition: box-shadow 0.2s, transform 0.2s; border-radius: 12px; }
    .event-card:hover { box-shadow: 0 8px 24px rgba(0,0,0,0.12); transform: translateY(-2px); }
    .event-top { display: flex; gap: 8px; margin-bottom: 12px; }
    .type-chip { font-size: 11px; }
    .online-chip { font-size: 11px; }
    .event-title { font-size: 1.15rem; font-weight: 600; margin: 0 0 12px; }
    .event-meta { display: flex; flex-direction: column; gap: 6px; color: #666; font-size: 0.85rem; margin-bottom: 8px; }
    .event-meta span { display: flex; align-items: center; gap: 6px; }
    .event-org { display: flex; align-items: center; gap: 6px; color: #888; font-size: 0.85rem; margin-bottom: 12px; }
    .meta-icon { font-size: 16px; width: 16px; height: 16px; }
    .event-spots { margin-top: 8px; }
    .progress-container { height: 6px; background: #eee; border-radius: 3px; overflow: hidden; margin-bottom: 6px; }
    .progress-bar { height: 100%; background: #4caf50; border-radius: 3px; transition: width 0.3s; }
    .spots-text { font-size: 0.8rem; color: #888; }
    .full-badge { background: #f44336; color: white; padding: 2px 8px; border-radius: 10px; font-size: 11px; margin-left: 8px; }
    .almost-full-badge { background: var(--brand-accent); color: white; padding: 2px 8px; border-radius: 10px; font-size: 11px; margin-left: 8px; }
  `],
})
export class EventListComponent implements OnInit {
  events = signal<EventSummary[]>([]);
  loading = signal(true);
  totalElements = signal(0);
  currentPage = signal(0);

  cityFilter = '';
  typeFilter: string | null = null;
  onlineOnly = false;

  // « Près de chez moi » — géolocalisation navigateur + tri par distance (côté client)
  userLoc = signal<{ lat: number; lng: number } | null>(null);
  locating = signal(false);
  geoError = signal('');
  private platformId = inject(PLATFORM_ID);

  /** Événements affichés : triés par distance croissante quand la position est connue. */
  displayedEvents = computed(() => {
    const list = this.events();
    if (!this.userLoc()) return list;
    return [...list].sort((a, b) => {
      const da = this.distanceKm(a), db = this.distanceKm(b);
      if (da === null) return 1;
      if (db === null) return -1;
      return da - db;
    });
  });

  readonly eventTypes = EVENT_TYPES;

  constructor(private eventService: EventService) {}

  ngOnInit() { this.loadEvents(); }

  toggleNearMe() {
    if (this.userLoc()) { this.userLoc.set(null); return; }
    if (!isPlatformBrowser(this.platformId) || !navigator.geolocation) {
      this.geoError.set('La géolocalisation n\'est pas disponible sur cet appareil.');
      return;
    }
    this.locating.set(true);
    this.geoError.set('');
    navigator.geolocation.getCurrentPosition(
      pos => {
        this.userLoc.set({ lat: pos.coords.latitude, lng: pos.coords.longitude });
        this.locating.set(false);
      },
      () => {
        this.geoError.set('Impossible d\'obtenir votre position. Autorisez la géolocalisation pour trier par distance.');
        this.locating.set(false);
      },
      { enableHighAccuracy: false, timeout: 8000, maximumAge: 300000 },
    );
  }

  /** Distance à vol d'oiseau (km) entre l'utilisateur et l'événement, ou null si non calculable. */
  distanceKm(event: EventSummary): number | null {
    const u = this.userLoc();
    if (!u || event.online || event.locationLat == null || event.locationLng == null) return null;
    return haversineKm(u.lat, u.lng, +event.locationLat, +event.locationLng);
  }

  distanceLabel(event: EventSummary): string {
    const d = this.distanceKm(event);
    if (d === null) return '';
    return d < 1 ? `${Math.round(d * 1000)} m` : `${d < 10 ? d.toFixed(1) : Math.round(d)} km`;
  }

  loadEvents() {
    this.loading.set(true);
    this.eventService.listEvents({
      page: this.currentPage(), size: 10,
      type: this.typeFilter || undefined,
      city: this.cityFilter || undefined,
      online: this.onlineOnly ? true : undefined,
    }).subscribe({
      next: res => {
        this.events.set(res.data.content);
        this.totalElements.set(res.data.totalElements);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  onPage(event: PageEvent) {
    this.currentPage.set(event.pageIndex);
    this.loadEvents();
  }
}

/** Distance à vol d'oiseau en km (formule de Haversine). */
function haversineKm(lat1: number, lng1: number, lat2: number, lng2: number): number {
  const R = 6371;
  const toRad = (d: number) => (d * Math.PI) / 180;
  const dLat = toRad(lat2 - lat1);
  const dLng = toRad(lng2 - lng1);
  const a = Math.sin(dLat / 2) ** 2
    + Math.cos(toRad(lat1)) * Math.cos(toRad(lat2)) * Math.sin(dLng / 2) ** 2;
  return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
}
