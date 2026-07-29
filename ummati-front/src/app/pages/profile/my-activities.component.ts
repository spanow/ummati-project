import { Component, signal, OnInit } from '@angular/core';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTabsModule } from '@angular/material/tabs';
import { MatChipsModule } from '@angular/material/chips';
import { MatMenuModule } from '@angular/material/menu';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { RouterLink } from '@angular/router';
import { DatePipe } from '@angular/common';
import { MembershipService, MembershipResponse } from '../../core/services/membership.service';
import { EventService, EventSummary, SignupResponse } from '../../core/services/event.service';
import { RetentionService } from '../../core/services/retention.service';
import { TPipe } from '../../shared/pipes/t.pipe';
import { LabelPipe } from '../../shared/pipes/label.pipe';

@Component({
  selector: 'app-my-activities',
  standalone: true,
  imports: [LabelPipe, MatCardModule, MatButtonModule, MatIconModule, MatTabsModule, MatChipsModule,
    MatMenuModule, MatPaginatorModule, MatProgressSpinnerModule, MatSnackBarModule, RouterLink, DatePipe, TPipe],
  template: `
    <div class="page page-narrow">
      <h1>{{ 'Mes activités' | t }}</h1>

      <mat-tab-group>
        <!-- Mes organisations -->
        <mat-tab [label]="('Mes organisations' | t) + ' (' + membershipTotal() + ')'">
          @if (membershipsLoading()) {
            <div class="state-center"><mat-spinner diameter="30" /></div>
          } @else if (memberships().length === 0) {
            <div class="empty">
              <mat-icon>group_off</mat-icon>
              <p>{{ 'Vous n\\'êtes membre d\\'aucune organisation.' | t }}</p>
              <a mat-flat-button routerLink="/organizations">{{ 'Explorer les ONG' | t }}</a>
            </div>
          } @else {
            <div class="card-list">
              @for (m of memberships(); track m.id) {
                <mat-card class="activity-card hover-lift" [routerLink]="['/organizations', m.organizationSlug]">
                  <mat-card-content>
                    <div class="card-row">
                      <mat-icon>business</mat-icon>
                      <div class="info">
                        <strong>{{ m.organizationName }}</strong>
                        <span class="meta">{{ 'Membre depuis' | t }} {{ m.joinedAt | date:'d MMM yyyy' }}</span>
                      </div>
                      <mat-chip [class]="'role-' + m.role.toLowerCase()">{{ m.role }}</mat-chip>
                      <button mat-icon-button [matMenuTriggerFor]="membershipMenu"
                              (click)="$event.stopPropagation()" aria-label="Options">
                        <mat-icon>more_vert</mat-icon>
                      </button>
                      <mat-menu #membershipMenu="matMenu">
                        <button mat-menu-item class="danger-item" (click)="leaveOrg(m)">
                          <mat-icon>logout</mat-icon> {{ 'Quitter l\\'organisation' | t }}
                        </button>
                      </mat-menu>
                    </div>
                  </mat-card-content>
                </mat-card>
              }
            </div>
            <mat-paginator [length]="membershipTotal()" [pageSize]="10" (page)="onMembershipPage($event)" />
          }
        </mat-tab>

        <!-- Mes inscriptions -->
        <mat-tab [label]="('Mes inscriptions' | t) + ' (' + signupTotal() + ')'">
          @if (signupsLoading()) {
            <div class="state-center"><mat-spinner diameter="30" /></div>
          } @else if (signups().length === 0) {
            <div class="empty">
              <mat-icon>event_busy</mat-icon>
              <p>{{ 'Vous n\\'avez aucune inscription à un événement.' | t }}</p>
              <a mat-flat-button routerLink="/events">{{ 'Voir les événements' | t }}</a>
            </div>
          } @else {
            <div class="card-list">
              @for (s of signups(); track s.id) {
                <mat-card class="activity-card hover-lift" [routerLink]="['/events', s.eventId]">
                  <mat-card-content>
                    <div class="card-row">
                      <mat-icon>event</mat-icon>
                      <div class="info">
                        <strong>{{ s.eventTitle }}</strong>
                        <span class="meta">{{ 'Inscrit le' | t }} {{ s.registeredAt | date:'d MMM yyyy' }}</span>
                      </div>
                      <mat-chip [class]="'status-' + s.status.toLowerCase()">{{ s.status | label: 'signupStatus' }}</mat-chip>
                    </div>
                  </mat-card-content>
                </mat-card>
              }
            </div>
            <mat-paginator [length]="signupTotal()" [pageSize]="10" (page)="onSignupPage($event)" />
          }
        </mat-tab>

        <mat-tab [label]="('Mises de côté' | t) + ' (' + favoriteTotal() + ')'">
          @if (favorites().length === 0) {
            <div class="empty-tab">
              <mat-icon>favorite_border</mat-icon>
              <p>{{ 'Vous n\\'avez mis aucune mission de côté.' | t }}</p>
              <p class="empty-hint">{{ 'Le cœur sur une mission la garde ici, et nous vous rappelons avant qu\\'elle n\\'ait lieu.' | t }}</p>
              <a mat-flat-button routerLink="/events">{{ 'Voir les événements' | t }}</a>
            </div>
          } @else {
            <div class="activity-list">
              @for (e of favorites(); track e.id) {
                <mat-card class="activity-card">
                  <mat-card-content>
                    <div class="activity-row">
                      <div class="activity-info">
                        <a class="no-underline" [routerLink]="['/events', e.id]"><strong>{{ e.title }}</strong></a>
                        <span class="date">
                          {{ e.startDate | date:'EEE d MMM yyyy, HH:mm' }} ·
                          {{ e.online ? ('À distance' | t) : e.locationCity }}
                        </span>
                      </div>
                      <mat-chip>{{ e.type | label: 'eventType' }}</mat-chip>
                    </div>
                  </mat-card-content>
                </mat-card>
              }
            </div>
            <mat-paginator [length]="favoriteTotal()" [pageSize]="10" (page)="onFavoritePage($event)" />
          }
        </mat-tab>
      </mat-tab-group>
    </div>
  `,
  styles: [`
    h1 { font-size: 1.8rem; font-weight: 800; margin-bottom: 24px; letter-spacing: -0.02em; }
    .empty { text-align: center; padding: 48px 24px; color: var(--brand-text-soft); }
    .empty mat-icon { font-size: 48px; width: 48px; height: 48px; color: var(--brand-text-faint); display: block; margin: 0 auto 12px; }
    .card-list { display: flex; flex-direction: column; gap: 10px; padding: 16px 0; }
    .activity-card { cursor: pointer; }
    .card-row { display: flex; align-items: center; gap: 16px; }
    .card-row mat-icon { color: var(--brand-primary); }
    .info { flex: 1; }
    .info strong { display: block; color: var(--brand-ink); }
    .meta { color: var(--brand-text-soft); font-size: 0.8rem; }
    .role-admin { background: var(--brand-primary-100) !important; color: var(--brand-primary-dark) !important; }
    .role-member { background: var(--brand-success-soft) !important; color: var(--brand-success) !important; }
    .status-registered { background: var(--brand-success-soft) !important; color: var(--brand-success) !important; }
    .status-waitlisted { background: var(--brand-accent-soft) !important; color: var(--brand-warn) !important; }
    .status-attended { background: var(--brand-primary-100) !important; color: var(--brand-primary-dark) !important; }
    .status-cancelled { background: var(--brand-danger-soft) !important; color: var(--brand-danger) !important; }
    .danger-item { color: var(--brand-danger); }
  `],
})
export class MyActivitiesComponent implements OnInit {
  memberships = signal<MembershipResponse[]>([]);
  membershipsLoading = signal(true);
  membershipTotal = signal(0);
  signups = signal<SignupResponse[]>([]);
  signupsLoading = signal(true);
  signupTotal = signal(0);

  favorites = signal<EventSummary[]>([]);
  favoriteTotal = signal(0);

  constructor(
    private membershipService: MembershipService,
    private eventService: EventService,
    private retention: RetentionService,
    private snackBar: MatSnackBar,
  ) {}

  ngOnInit() { this.loadMemberships(); this.loadSignups(); this.loadFavorites(0); }

  loadFavorites(page: number) {
    this.retention.listFavorites(page, 10).subscribe({
      next: res => {
        this.favorites.set(res.data.content);
        this.favoriteTotal.set(res.data.totalElements);
      },
      error: () => {},
    });
  }

  onFavoritePage(event: PageEvent) { this.loadFavorites(event.pageIndex); }

  leaveOrg(m: MembershipResponse) {
    if (!confirm(`Quitter l'organisation « ${m.organizationName} » ?`)) return;
    this.membershipService.remove(m.id).subscribe({
      next: () => {
        this.snackBar.open(`Vous avez quitté ${m.organizationName}.`, 'OK', { duration: 4000 });
        this.loadMemberships();
      },
      error: err => {
        const msg = err?.error?.message ?? 'Impossible de quitter cette organisation.';
        this.snackBar.open(msg, 'OK', { duration: 6000 });
      },
    });
  }

  loadMemberships(page = 0) {
    this.membershipsLoading.set(true);
    this.membershipService.listByUser('ACTIVE', page).subscribe({
      next: res => {
        this.memberships.set(res.data.content);
        this.membershipTotal.set(res.data.totalElements);
        this.membershipsLoading.set(false);
      },
      error: () => this.membershipsLoading.set(false),
    });
  }

  loadSignups(page = 0) {
    this.signupsLoading.set(true);
    this.eventService.listUserSignups(page).subscribe({
      next: res => {
        this.signups.set(res.data.content);
        this.signupTotal.set(res.data.totalElements);
        this.signupsLoading.set(false);
      },
      error: () => this.signupsLoading.set(false),
    });
  }

  onMembershipPage(e: PageEvent) { this.loadMemberships(e.pageIndex); }
  onSignupPage(e: PageEvent) { this.loadSignups(e.pageIndex); }
}

