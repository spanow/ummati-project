import { Component, signal, inject, OnInit, PLATFORM_ID } from '@angular/core';
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

      <mat-form-field appearance="outline" class="q-field" subscriptSizing="dynamic">
        <mat-label>{{ 'Rechercher une mission' | t }}</mat-label>
        <input matInput [(ngModel)]="searchQuery" (keyup.enter)="resetAndLoad()"
               [placeholder]="'maraude, cours de soutien, collecte...' | t" />
        <mat-icon matPrefix>search</mat-icon>
        @if (searchQuery) {
          <button matSuffix mat-icon-button type="button" [attr.aria-label]="'Effacer' | t"
                  (click)="searchQuery = ''; resetAndLoad()">
            <mat-icon>close</mat-icon>
          </button>
        }
      </mat-form-field>

      <div class="filters-row">
        <mat-form-field appearance="outline" class="search-field" subscriptSizing="dynamic">
          <mat-label>{{ 'Ville' | t }}</mat-label>
          <input matInput [(ngModel)]="cityFilter" (keyup.enter)="resetAndLoad()"
                 placeholder="Lyon, Paris..." />
          <mat-icon matSuffix>location_on</mat-icon>
        </mat-form-field>
        <mat-form-field appearance="outline" subscriptSizing="dynamic">
          <mat-label>{{ 'Type' | t }}</mat-label>
          <mat-select [(ngModel)]="typeFilter" (selectionChange)="resetAndLoad()">
            <mat-option [value]="null">{{ 'Tous' | t }}</mat-option>
            @for (t of eventTypes; track t.value) {
              <mat-option [value]="t.value">{{ t.label }}</mat-option>
            }
          </mat-select>
        </mat-form-field>
        @if (userLoc()) {
          <mat-form-field appearance="outline" subscriptSizing="dynamic" class="radius-field">
            <mat-label>{{ 'Rayon' | t }}</mat-label>
            <mat-select [(ngModel)]="radiusKm" (selectionChange)="resetAndLoad()">
              @for (r of radiusOptions; track r) {
                <mat-option [value]="r">{{ r }} km</mat-option>
              }
            </mat-select>
          </mat-form-field>
        }
        <mat-checkbox [(ngModel)]="onlineOnly" (change)="resetAndLoad()">{{ 'En ligne uniquement' | t }}</mat-checkbox>
        <button mat-stroked-button type="button" class="near-btn" [class.active]="userLoc()"
                [disabled]="locating()" (click)="toggleNearMe()">
          @if (locating()) { <mat-spinner diameter="18" /> } @else { <mat-icon>near_me</mat-icon> }
          {{ (userLoc() ? 'Autour de moi' : 'Près de chez moi') | t }}
        </button>
      </div>
      @if (geoError()) { <p class="geo-error">{{ geoError() }}</p> }
      @if (userLoc() && !onlineOnly) {
        <p class="geo-hint">
          <mat-icon>info</mat-icon>
          {{ 'Missions à moins de' | t }} {{ radiusKm }} km, de la plus proche à la plus lointaine.
          {{ 'Les missions en ligne sont exclues de ce périmètre.' | t }}
        </p>
      }

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
          @for (event of events(); track event.id) {
            <a class="event-card hover-lift no-underline" [routerLink]="['/events', event.id]">
              <!-- Visuel d'annonce : premier élément de décision pour le bénévole.
                   Sans couverture, un aplat dégradé conserve le rythme de la grille. -->
              <div class="ev-cover" [class.is-placeholder]="!event.coverUrl">
                @if (event.coverUrl) {
                  <img [src]="event.coverUrl" alt="" loading="lazy" decoding="async" />
                } @else {
                  <mat-icon aria-hidden="true">volunteer_activism</mat-icon>
                }
                <time class="ev-date" [attr.datetime]="event.startDate">
                  <b>{{ event.startDate | date:'d' }}</b>
                  <span>{{ event.startDate | date:'MMM' }}</span>
                </time>
              </div>

              <div class="ev-head">
                <div class="ev-tags">
                  <span class="badge badge-neutral">{{ event.type }}</span>
                  @if (event.online) { <span class="badge badge-info">{{ 'En ligne' | t }}</span> }
                  @if (event.distanceKm !== null) {
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
    .q-field { width: 100%; margin-bottom: var(--space-3); }
    .search-field { flex: 1; min-width: 200px; }
    .radius-field { width: 120px; }
    .near-btn { height: 54px; border-radius: var(--radius-md) !important; }
    .near-btn.active {
      background: var(--brand-primary-100); color: var(--brand-primary-dark);
      border-color: var(--brand-primary);
    }
    .geo-error { color: var(--brand-danger); font-size: 0.85rem; margin: -12px 0 16px; }
    .geo-hint {
      display: flex; align-items: center; gap: 8px;
      color: var(--brand-text-soft); font-size: 0.85rem; margin: -8px 0 16px;
    }
    .geo-hint mat-icon { font-size: 16px; width: 16px; height: 16px; flex: 0 0 auto; }

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
      display: flex; flex-direction: column; overflow: hidden;
      background: var(--brand-surface); border: 1px solid var(--brand-border);
      border-radius: var(--radius-card); box-shadow: var(--brand-shadow-xs);
      padding: 0 var(--space-5) var(--space-5); color: inherit;
    }

    .ev-cover {
      position: relative; margin: 0 calc(-1 * var(--space-5)) var(--space-4);
      aspect-ratio: 16 / 9; background: var(--brand-surface-2); overflow: hidden;
    }
    .ev-cover img { width: 100%; height: 100%; object-fit: cover; display: block; }
    .ev-cover.is-placeholder {
      display: flex; align-items: center; justify-content: center;
      background: var(--brand-primary-soft);
    }
    .ev-cover.is-placeholder mat-icon {
      font-size: 40px; width: 40px; height: 40px; color: var(--brand-primary-100);
    }

    .ev-head { display: flex; align-items: flex-start; gap: var(--space-3); margin-bottom: var(--space-3); }
    /* Bloc date façon page de calendrier, posé sur le visuel : repère immédiat */
    .ev-date {
      position: absolute; inset-block-start: var(--space-3); inset-inline-start: var(--space-3);
      width: 54px; height: 54px; border-radius: var(--radius-sm);
      background: var(--brand-surface); border: 1px solid var(--brand-primary-100);
      color: var(--brand-primary-dark); box-shadow: var(--brand-shadow-xs);
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

  searchQuery = '';
  cityFilter = '';
  typeFilter: string | null = null;
  onlineOnly = false;
  radiusKm = 25;

  readonly radiusOptions = [5, 10, 25, 50, 100];

  /** Nombre de squelettes affichés pendant le chargement (= taille de page). */
  readonly skeletonSlots = Array.from({ length: 6 }, (_, i) => i);

  // « Autour de moi » — géolocalisation navigateur, puis filtrage et tri côté serveur :
  // trier la seule page courante ne remonterait jamais une mission proche située page 2.
  userLoc = signal<{ lat: number; lng: number } | null>(null);
  locating = signal(false);
  geoError = signal('');
  private platformId = inject(PLATFORM_ID);

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
    if (this.userLoc()) {
      this.userLoc.set(null);
      this.resetAndLoad();
      return;
    }
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
        this.resetAndLoad();
      },
      () => {
        this.geoError.set('Impossible d\'obtenir votre position. Autorisez la géolocalisation pour trier par distance.');
        this.locating.set(false);
      },
      { enableHighAccuracy: false, timeout: 8000, maximumAge: 300000 },
    );
  }

  /** Distance renvoyée par le serveur, mise en forme. */
  distanceLabel(event: EventSummary): string {
    const d = event.distanceKm;
    if (d === null) return '';
    return d < 1 ? `${Math.round(d * 1000)} m` : `${d < 10 ? d.toFixed(1) : Math.round(d)} km`;
  }

  /** Tout changement de critère renvoie à la première page : sinon on pagine un résultat qui n'existe plus. */
  resetAndLoad() {
    this.currentPage.set(0);
    this.loadEvents();
  }

  loadEvents() {
    this.loading.set(true);
    const loc = this.userLoc();
    this.eventService.listEvents({
      page: this.currentPage(), size: 10,
      type: this.typeFilter || undefined,
      city: this.cityFilter || undefined,
      online: this.onlineOnly ? true : undefined,
      q: this.searchQuery.trim() || undefined,
      // Le périmètre ne s'applique qu'aux missions physiques : le combiner avec
      // « en ligne uniquement » ne renverrait jamais rien.
      lat: loc?.lat,
      lng: loc?.lng,
      radiusKm: loc && !this.onlineOnly ? this.radiusKm : undefined,
      sort: loc ? 'distance' : undefined,
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
