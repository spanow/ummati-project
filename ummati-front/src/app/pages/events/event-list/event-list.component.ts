import { Component, signal, computed, inject, OnInit, PLATFORM_ID } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { DatePipe } from '@angular/common';
import { EventService, EventSummary } from '../../../core/services/event.service';
import { EVENT_TYPES } from '../../../core/constants/event-types';
import { TPipe } from '../../../shared/pipes/t.pipe';
import { CardSkeletonComponent } from '../../../shared/components/skeleton/skeleton.component';

@Component({
  selector: 'app-event-list',
  standalone: true,
  imports: [MatButtonModule, MatIconModule, MatFormFieldModule, MatInputModule,
    MatSelectModule, MatPaginatorModule, MatProgressSpinnerModule, MatCheckboxModule,
    RouterLink, FormsModule, DatePipe, TPipe, CardSkeletonComponent],
  template: `
    <div class="page">
      <header class="page-head">
        <div>
          <h1 class="page-title">{{ 'Événements' | t }}</h1>
          <p class="page-sub">{{ 'Trouvez une mission de bénévolat près de chez vous' | t }}</p>
        </div>
      </header>

      <div class="filters-row">
        <mat-form-field appearance="outline" class="search-field" subscriptSizing="dynamic">
          <mat-label>{{ 'Ville' | t }}</mat-label>
          <input matInput [(ngModel)]="cityFilter" (keyup.enter)="loadEvents()"
                 placeholder="Lyon, Paris..." />
          <mat-icon matSuffix>location_on</mat-icon>
        </mat-form-field>
        <mat-form-field appearance="outline" subscriptSizing="dynamic">
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
          @if (locating()) { <mat-spinner diameter="18" /> } @else { <mat-icon>near_me</mat-icon> }
          {{ (userLoc() ? 'Trié par distance' : 'Près de chez moi') | t }}
        </button>
      </div>
      @if (geoError()) { <p class="geo-error">{{ geoError() }}</p> }

      @if (loading()) {
        <div class="event-grid" aria-busy="true">
          @for (i of skeletonSlots; track i) { <app-card-skeleton /> }
        </div>
      } @else if (events().length === 0) {
        <div class="empty-state">
          <div class="empty-badge"><mat-icon>event_busy</mat-icon></div>
          <h3>{{ 'Aucun événement trouvé' | t }}</h3>
          <p>{{ 'Essayez d\\'ajuster vos filtres ou revenez plus tard !' | t }}</p>
        </div>
      } @else {
        <div class="event-grid">
          @for (event of displayedEvents(); track event.id) {
            <a class="event-card hover-lift no-underline" [routerLink]="['/events', event.id]">
              <div class="ev-head">
                <time class="ev-date" [attr.datetime]="event.startDate">
                  <b>{{ event.startDate | date:'d' }}</b>
                  <span>{{ event.startDate | date:'MMM' }}</span>
                </time>
                <div class="ev-tags">
                  <span class="badge badge-neutral">{{ event.type }}</span>
                  @if (event.online) { <span class="badge badge-info">{{ 'En ligne' | t }}</span> }
                  @if (distanceKm(event) !== null) {
                    <span class="badge badge-brand"><mat-icon>near_me</mat-icon>{{ distanceLabel(event) }}</span>
                  }
                </div>
              </div>

              <h3 class="ev-title">{{ event.title }}</h3>

              <div class="ev-meta">
                <span><mat-icon>schedule</mat-icon>{{ event.startDate | date:'EEE d MMM, HH:mm' }}</span>
                <span><mat-icon>location_on</mat-icon>{{ event.online ? ('À distance' | t) : event.locationCity }}</span>
                <span><mat-icon>apartment</mat-icon>{{ event.organizationName }}</span>
              </div>

              <div class="ev-foot">
                @if (event.maxParticipants) {
                  <div class="ev-progress" role="progressbar"
                       [attr.aria-valuenow]="event.registeredCount" aria-valuemin="0"
                       [attr.aria-valuemax]="event.maxParticipants">
                    <span [class.is-full]="isFull(event)" [class.is-almost]="isAlmostFull(event)"
                          [style.width.%]="fillPercent(event)"></span>
                  </div>
                  <div class="ev-spots">
                    <span class="tnum">{{ event.registeredCount }}/{{ event.maxParticipants }} {{ 'inscrits' | t }}</span>
                    @if (isFull(event)) {
                      <span class="badge badge-danger">{{ 'Complet' | t }}</span>
                    } @else if (isAlmostFull(event)) {
                      <span class="badge badge-warn">{{ 'Presque complet' | t }}</span>
                    } @else {
                      <span class="ev-left tnum">{{ event.maxParticipants - event.registeredCount }} {{ 'places' | t }}</span>
                    }
                  </div>
                } @else {
                  <div class="ev-spots"><span class="tnum">{{ event.registeredCount }} {{ 'inscrits' | t }}</span></div>
                }
              </div>
            </a>
          }
        </div>
        <mat-paginator [length]="totalElements()" [pageSize]="10" [pageIndex]="currentPage()"
                       (page)="onPage($event)" />
      }
    </div>
  `,
  styles: [`
    .search-field { flex: 1; min-width: 200px; }
    .near-btn { height: 54px; border-radius: var(--radius-md) !important; }
    .near-btn.active {
      background: var(--brand-primary-100); color: var(--brand-primary-dark);
      border-color: var(--brand-primary);
    }
    .geo-error { color: var(--brand-danger); font-size: 0.85rem; margin: -12px 0 16px; }

    .empty-state { text-align: center; padding: var(--space-9) var(--space-5); }
    .empty-badge {
      width: 76px; height: 76px; margin: 0 auto var(--space-4); border-radius: 50%;
      background: var(--brand-surface-2); border: 1px solid var(--brand-border);
      display: flex; align-items: center; justify-content: center;
    }
    .empty-badge mat-icon { font-size: 34px; width: 34px; height: 34px; color: var(--brand-text-faint); }
    .empty-state h3 { font-size: 1.25rem; margin: 0 0 var(--space-2); }
    .empty-state p { color: var(--brand-text-soft); margin: 0; }

    .event-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(330px, 1fr)); gap: var(--space-4); }

    /* Carte = lien : toute la surface est cliquable et focalisable au clavier */
    .event-card {
      display: flex; flex-direction: column;
      background: var(--brand-surface); border: 1px solid var(--brand-border);
      border-radius: var(--radius-card); box-shadow: var(--brand-shadow-xs);
      padding: var(--space-5); color: inherit;
    }

    .ev-head { display: flex; align-items: flex-start; gap: var(--space-3); margin-bottom: var(--space-4); }
    /* Bloc date façon page de calendrier : repère visuel immédiat */
    .ev-date {
      flex: 0 0 auto; width: 54px; height: 54px; border-radius: var(--radius-sm);
      background: var(--brand-primary-soft); border: 1px solid var(--brand-primary-100);
      color: var(--brand-primary-dark);
      display: flex; flex-direction: column; align-items: center; justify-content: center; line-height: 1.05;
    }
    .ev-date b { font-size: 1.3rem; font-weight: 800; }
    .ev-date span { font-size: 0.6rem; font-weight: 700; letter-spacing: 0.08em; text-transform: uppercase; }
    .ev-tags { display: flex; gap: 6px; flex-wrap: wrap; justify-content: flex-end; margin-inline-start: auto; }
    .ev-tags .badge mat-icon { font-size: 13px; width: 13px; height: 13px; }

    .ev-title {
      font-size: 1.12rem; font-weight: 700; line-height: 1.35; margin: 0 0 var(--space-3);
      color: var(--brand-ink);
      display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden;
    }

    .ev-meta { display: flex; flex-direction: column; gap: 6px; color: var(--brand-text-soft); font-size: 0.86rem; }
    .ev-meta span { display: flex; align-items: center; gap: 7px; }
    .ev-meta mat-icon { font-size: 16px; width: 16px; height: 16px; color: var(--brand-text-faint); flex: 0 0 auto; }

    .ev-foot { margin-top: auto; padding-top: var(--space-4); }
    .ev-progress { height: 6px; background: var(--brand-surface-3); border-radius: 3px; overflow: hidden; }
    .ev-progress span {
      display: block; height: 100%; border-radius: 3px;
      background: var(--brand-gradient); transition: width 0.4s var(--ease-out);
    }
    .ev-progress span.is-almost { background: var(--brand-warn); }
    .ev-progress span.is-full { background: var(--brand-danger); }
    .ev-spots {
      display: flex; align-items: center; justify-content: space-between; gap: var(--space-2);
      margin-top: 8px; font-size: 0.82rem; color: var(--brand-text-soft);
    }
    .ev-left { color: var(--brand-success); font-weight: 700; }
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

  /** Nombre de squelettes affichés pendant le chargement (= taille de page). */
  readonly skeletonSlots = Array.from({ length: 6 }, (_, i) => i);

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

  isFull(e: EventSummary): boolean {
    return !!e.maxParticipants && e.registeredCount >= e.maxParticipants;
  }
  isAlmostFull(e: EventSummary): boolean {
    return !!e.maxParticipants && !this.isFull(e) && e.registeredCount >= e.maxParticipants * 0.8;
  }
  fillPercent(e: EventSummary): number {
    if (!e.maxParticipants) return 0;
    return Math.min(100, (e.registeredCount / e.maxParticipants) * 100);
  }

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
