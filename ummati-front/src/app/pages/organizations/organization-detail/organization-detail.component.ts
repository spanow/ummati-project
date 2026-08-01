import { Component, signal, OnInit, inject } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatTabsModule } from '@angular/material/tabs';
import { MatMenuModule } from '@angular/material/menu';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { DatePipe, DecimalPipe } from '@angular/common';
import { OrganizationService, OrganizationDetail } from '../../../core/services/organization.service';
import { MembershipService } from '../../../core/services/membership.service';
import { AuthService } from '../../../core/services/auth.service';
import { OrgAnnouncementService, OrgAnnouncementResponse } from '../../../core/services/org-announcement.service';
import { EventService, EventSummary } from '../../../core/services/event.service';
import { ReportDialogComponent } from '../../../shared/components/report-dialog/report-dialog.component';
import { JoinDialogComponent } from '../join-dialog/join-dialog.component';
import { TPipe } from '../../../shared/pipes/t.pipe';
import { LocationPickerComponent } from '../../../shared/components/location-picker/location-picker.component';
import { LabelPipe } from '../../../shared/pipes/label.pipe';
import { RetentionService } from '../../../core/services/retention.service';
import { MediaUrlPipe } from '../../../shared/pipes/media-url.pipe';

@Component({
  selector: 'app-organization-detail',
  standalone: true,
  imports: [MediaUrlPipe, LabelPipe, MatCardModule, MatButtonModule, MatIconModule, MatChipsModule, MatTabsModule, MatMenuModule,
    MatProgressSpinnerModule, MatSnackBarModule, MatDialogModule, DecimalPipe, DatePipe, RouterLink, TPipe,
    LocationPickerComponent],
  template: `
    @if (loading()) {
      <div class="state-center"><mat-spinner diameter="40" /></div>
    } @else if (org()) {
      <div class="detail-page">
        <div class="banner" [style.background-image]="org()!.bannerUrl ? 'url(' + org()!.bannerUrl + ')' : ''">
          <div class="banner-overlay">
            <div class="org-identity">
              @if (org()!.logoUrl) {
                <img [src]="org()!.logoUrl | mediaUrl" class="logo" />
              } @else {
                <div class="logo-placeholder"><mat-icon>business</mat-icon></div>
              }
              <div>
                <h1>{{ org()!.name }}</h1>
                <div class="meta-row">
                  <mat-chip>{{ org()!.domain | label: 'domain' }}</mat-chip>
                  <span class="location"><mat-icon>location_on</mat-icon> {{ org()!.addressCity }}</span>
                </div>
              </div>
            </div>
            @if (isLoggedIn()) {
              <button mat-icon-button class="more-btn" [matMenuTriggerFor]="orgMenu" [attr.aria-label]="'Plus d\\'options' | t">
                <mat-icon>more_vert</mat-icon>
              </button>
              <mat-menu #orgMenu="matMenu">
                <button mat-menu-item (click)="reportOrg()">
                  <mat-icon>flag</mat-icon> {{ 'Signaler cette organisation' | t }}
                </button>
              </mat-menu>
            }
          </div>
        </div>

        <div class="content-container">
          <div class="stats-bar">
            <div class="stat">
              <span class="stat-value">{{ org()!.stats.memberCount }}</span>
              <span class="stat-label">{{ 'Membres' | t }}</span>
            </div>
            <div class="stat">
              <span class="stat-value">{{ org()!.stats.eventCount }}</span>
              <span class="stat-label">{{ 'Événements' | t }}</span>
            </div>
            <div class="stat">
              <span class="stat-value">{{ org()!.stats.averageRating ? (org()!.stats.averageRating | number:'1.1-1') : '—' }}</span>
              <span class="stat-label">{{ 'Note moyenne' | t }}</span>
            </div>
            @if (membershipRole() === 'ADMIN') {
              <div class="admin-actions">
                <a mat-flat-button color="primary"
                   [routerLink]="['/organizations', org()!.id, 'events', 'new']">
                  <mat-icon>add</mat-icon> {{ 'Créer un événement' | t }}
                </a>
                <a mat-stroked-button
                   [routerLink]="['/organizations', org()!.slug, 'manage']"
                   [queryParams]="{ orgId: org()!.id }">
                  <mat-icon>settings</mat-icon> {{ 'Gérer' | t }}
                </a>
              </div>
            } @else if (membershipStatus() === 'ACTIVE') {
              <button mat-stroked-button class="join-btn" disabled>{{ 'Membre' | t }}</button>
            } @else if (membershipStatus() === 'PENDING') {
              <button mat-stroked-button class="join-btn" disabled>{{ 'En attente' | t }}</button>
            } @else if (!isLoggedIn()) {
              <a mat-flat-button class="join-btn" routerLink="/login">{{ 'Rejoindre' | t }}</a>
            } @else {
              <button mat-flat-button class="join-btn" [disabled]="joining()" (click)="joinOrg()">
                {{ (joining() ? 'Envoi…' : 'Rejoindre') | t }}
              </button>
            }

            @if (isLoggedIn()) {
              <!-- Suivre n'est pas adhérer : aucun engagement, aucune validation.
                   C'est le geste pour « prévenez-moi quand ils publient ». -->
              <button mat-stroked-button class="follow-btn" [class.is-on]="following()"
                      [attr.aria-pressed]="following()" (click)="toggleFollow()">
                <mat-icon>{{ following() ? 'notifications_active' : 'notifications' }}</mat-icon>
                {{ (following() ? 'Suivi' : 'Suivre') | t }}
              </button>
            }
          </div>

          <mat-tab-group>
            <mat-tab [label]="'À propos' | t">
              <div class="tab-content">
                <h3>{{ 'Description' | t }}</h3>
                <p class="description">{{ org()!.description }}</p>
                @if (org()!.mission) {
                  <h3>{{ 'Mission' | t }}</h3>
                  <p>{{ org()!.mission }}</p>
                }
                <div class="contact-info">
                  @if (org()!.email) { <div><mat-icon>email</mat-icon> {{ org()!.email }}</div> }
                  @if (org()!.phone) { <div><mat-icon>phone</mat-icon> {{ org()!.phone }}</div> }
                  @if (org()!.website) { <div><mat-icon>language</mat-icon> <a [href]="org()!.website" target="_blank">{{ org()!.website }}</a></div> }
                  @if (org()!.addressCity) { <div><mat-icon>place</mat-icon> {{ org()!.addressStreet ? org()!.addressStreet + ', ' : '' }}{{ org()!.addressZip }} {{ org()!.addressCity }}</div> }
                </div>
                @if (org()!.addressLat && org()!.addressLng) {
                  <div class="org-map">
                    <app-location-picker [editable]="false"
                      [lat]="+org()!.addressLat!" [lng]="+org()!.addressLng!" />
                  </div>
                }
              </div>
            </mat-tab>

            <mat-tab>
              <ng-template matTabLabel>
                {{ 'Événements' | t }}
                @if (orgEvents().length > 0) {
                  <span class="tab-badge">{{ orgEvents().length }}</span>
                }
              </ng-template>
              <div class="tab-content">
                @if (membershipRole() === 'ADMIN') {
                  <div class="events-admin-bar">
                    <a mat-flat-button color="primary"
                       [routerLink]="['/organizations', org()!.id, 'events', 'new']">
                      <mat-icon>add</mat-icon> {{ 'Créer un événement' | t }}
                    </a>
                    <a mat-stroked-button [routerLink]="['/organizations', org()!.id, 'events', 'manage']">
                      <mat-icon>list</mat-icon> {{ 'Gérer les événements' | t }}
                    </a>
                  </div>
                }
                @if (loadingEvents()) {
                  <div class="loading-inline"><mat-spinner diameter="28" /></div>
                } @else if (orgEvents().length === 0) {
                  <p class="placeholder-text">{{ 'Aucun événement à venir pour le moment.' | t }}</p>
                } @else {
                  <div class="org-event-list">
                    @for (e of orgEvents(); track e.id) {
                      <a class="org-event-card" [routerLink]="['/events', e.id]">
                        <div class="event-date-block">
                          <span class="event-day">{{ e.startDate | date:'d' }}</span>
                          <span class="event-month">{{ e.startDate | date:'MMM' }}</span>
                        </div>
                        <div class="event-info">
                          <h4 class="event-title">{{ e.title }}</h4>
                          <div class="event-meta">
                            <span><mat-icon>schedule</mat-icon> {{ e.startDate | date:'HH:mm' }}</span>
                            @if (e.online) {
                              <span><mat-icon>videocam</mat-icon> {{ 'En ligne' | t }}</span>
                            } @else {
                              <span><mat-icon>location_on</mat-icon> {{ e.locationCity }}</span>
                            }
                            <mat-chip class="event-type-chip">{{ e.type | label: 'eventType' }}</mat-chip>
                          </div>
                        </div>
                        @if (e.maxParticipants) {
                          <div class="event-spots"
                               [class.almost-full]="e.registeredCount / e.maxParticipants >= 0.8">
                            {{ e.registeredCount }}/{{ e.maxParticipants }}
                            <span class="spots-label">{{ 'inscrits' | t }}</span>
                          </div>
                        }
                      </a>
                    }
                  </div>
                }
              </div>
            </mat-tab>

            <mat-tab>
              <ng-template matTabLabel>
                {{ 'Annonces' | t }}
                @if (announcements().length > 0) {
                  <span class="tab-badge">{{ announcements().length }}</span>
                }
              </ng-template>
              <div class="tab-content">
                @if (loadingAnnouncements()) {
                  <div class="loading-inline"><mat-spinner diameter="28" /></div>
                } @else if (announcements().length === 0) {
                  <p class="placeholder-text">{{ 'Aucune annonce pour le moment.' | t }}</p>
                } @else {
                  <div class="announcement-list">
                    @for (a of announcements(); track a.id) {
                      <div class="announcement-card" [class.pinned]="a.pinned">
                        @if (a.pinned) {
                          <span class="pin-badge"><mat-icon>push_pin</mat-icon> {{ 'Épinglé' | t }}</span>
                        }
                        <h4 class="announcement-title">{{ a.title }}</h4>
                        <p class="announcement-content">{{ a.content }}</p>
                        <div class="announcement-meta">
                          {{ a.authorFirstName }} {{ a.authorLastName }} · {{ a.createdAt | date:'dd/MM/yyyy HH:mm' }}
                        </div>
                      </div>
                    }
                  </div>
                }
              </div>
            </mat-tab>
          </mat-tab-group>
        </div>
      </div>
    }
  `,
  styles: [`
    .loading-inline { display: flex; justify-content: center; padding: 32px; }
    .detail-page { max-width: 960px; margin: 0 auto; }
    .banner { height: 200px; background: var(--brand-gradient); position: relative; background-size: cover; background-position: center; }
    .banner-overlay { position: absolute; inset: 0; background: rgba(15,23,42,0.28); display: flex; align-items: flex-end; padding: 24px 32px; }
    .more-btn { position: absolute; top: 16px; right: 16px; color: white; }
    .org-identity { display: flex; align-items: center; gap: 20px; color: white; }
    .logo { width: 72px; height: 72px; border-radius: 14px; border: 3px solid white; object-fit: cover; }
    .logo-placeholder { width: 72px; height: 72px; border-radius: 14px; border: 3px solid white; background: rgba(255,255,255,0.2); display: flex; align-items: center; justify-content: center; }
    .logo-placeholder mat-icon { font-size: 36px; width: 36px; height: 36px; color: white; }
    h1 { margin: 0 0 6px; font-size: 1.8rem; font-weight: 700; letter-spacing: -0.02em; }
    .meta-row { display: flex; align-items: center; gap: 12px; flex-wrap: wrap; }
    .location { display: flex; align-items: center; gap: 4px; font-size: 0.9rem; opacity: 0.92; }
    .content-container { padding: 0 32px 48px; }
    .stats-bar { display: flex; align-items: center; gap: 40px; padding: 24px 0; border-bottom: 1px solid var(--brand-border); margin-bottom: 24px; flex-wrap: wrap; }
    .stat { display: flex; flex-direction: column; align-items: center; }
    .stat-value { font-size: 1.5rem; font-weight: 800; color: var(--brand-primary); }
    .stat-label { font-size: 0.8rem; color: var(--brand-text-soft); margin-top: 2px; }
    .join-btn { margin-left: auto; height: 44px; padding: 0 32px; }
    .follow-btn { height: 44px; border-radius: var(--radius-md) !important; }
    .follow-btn.is-on {
      background: var(--brand-primary-soft); color: var(--brand-primary-dark);
      border-color: var(--brand-primary-100);
    }
    .follow-btn mat-icon { font-size: 18px; width: 18px; height: 18px; }
    .admin-actions { margin-left: auto; display: flex; gap: 8px; flex-wrap: wrap; }
    .events-admin-bar { display: flex; gap: 8px; margin-bottom: 24px; flex-wrap: wrap; }
    .tab-content { padding: 24px 0; }
    .tab-content h3 { font-size: 1.1rem; font-weight: 700; margin: 0 0 12px; }
    .description { line-height: 1.7; color: var(--brand-text); white-space: pre-line; }
    .contact-info { margin-top: 24px; display: flex; flex-direction: column; gap: 10px; }
    .contact-info div { display: flex; align-items: center; gap: 8px; color: var(--brand-text); }
    .contact-info a { color: var(--brand-primary); text-decoration: none; }
    .org-map { margin-top: 20px; }
    .placeholder-text { color: var(--brand-text-soft); font-style: italic; padding: 40px 0; text-align: center; }
    .tab-badge { background: var(--brand-primary); color: var(--brand-surface); border-radius: 10px; padding: 1px 7px; font-size: 11px; margin-inline-start: 6px; font-weight: 700; }
    .announcement-list { display: flex; flex-direction: column; gap: 16px; }
    .announcement-card { padding: 20px 24px; border-radius: var(--radius-md); border: 1px solid var(--brand-border); background: var(--brand-surface); }
    .announcement-card.pinned { border-left: 4px solid var(--brand-primary); background: var(--brand-primary-soft); }
    .pin-badge { display: inline-flex; align-items: center; gap: 4px; font-size: 0.75rem; color: var(--brand-primary); font-weight: 700; margin-bottom: 8px; }
    .pin-badge mat-icon { font-size: 14px; width: 14px; height: 14px; }
    .announcement-title { margin: 0 0 8px; font-size: 1rem; font-weight: 700; color: var(--brand-ink); }
    .announcement-content { margin: 0 0 12px; color: var(--brand-text); line-height: 1.6; white-space: pre-line; }
    .announcement-meta { font-size: 0.8rem; color: var(--brand-text-faint); }
    .org-event-list { display: flex; flex-direction: column; gap: 12px; }
    .org-event-card { display: flex; align-items: center; gap: 20px; padding: 16px 20px; border: 1px solid var(--brand-border); border-radius: var(--radius-md); background: var(--brand-surface); text-decoration: none; color: inherit; transition: box-shadow 0.18s ease, transform 0.18s ease, border-color 0.18s ease; }
    .org-event-card:hover { box-shadow: var(--brand-shadow-md); transform: translateY(-2px); border-color: var(--brand-primary-100); }
    .event-date-block { display: flex; flex-direction: column; align-items: center; justify-content: center; width: 56px; height: 56px; background: var(--brand-primary-100); border-radius: 10px; flex-shrink: 0; }
    .event-day { font-size: 1.3rem; font-weight: 800; color: var(--brand-primary-dark); line-height: 1.1; }
    .event-month { font-size: 0.7rem; text-transform: uppercase; color: var(--brand-primary); font-weight: 700; }
    .event-info { flex: 1; min-width: 0; }
    .event-title { margin: 0 0 6px; font-size: 1rem; font-weight: 700; color: var(--brand-ink); }
    .event-meta { display: flex; align-items: center; gap: 14px; flex-wrap: wrap; color: var(--brand-text-soft); font-size: 0.85rem; }
    .event-meta span { display: inline-flex; align-items: center; gap: 4px; }
    .event-meta mat-icon { font-size: 16px; width: 16px; height: 16px; }
    .event-type-chip { font-size: 0.7rem !important; min-height: 22px !important; }
    .event-spots { display: flex; flex-direction: column; align-items: center; font-weight: 800; color: var(--brand-success); flex-shrink: 0; }
    .event-spots.almost-full { color: var(--brand-warn); }
    .spots-label { font-size: 0.7rem; font-weight: 400; color: var(--brand-text-faint); }
  `],
})
export class OrganizationDetailComponent implements OnInit {
  org = signal<OrganizationDetail | null>(null);
  loading = signal(true);
  joining = signal(false);
  membershipStatus = signal<string | null>(null);
  membershipRole = signal<string | null>(null);
  announcements = signal<OrgAnnouncementResponse[]>([]);
  loadingAnnouncements = signal(false);
  orgEvents = signal<EventSummary[]>([]);
  loadingEvents = signal(false);

  private authService = inject(AuthService);
  protected isLoggedIn = this.authService.isLoggedIn;

  constructor(
    private route: ActivatedRoute,
    private orgService: OrganizationService,
    private membershipService: MembershipService,
    private announcementService: OrgAnnouncementService,
    private eventService: EventService,
    private snackBar: MatSnackBar,
    private dialog: MatDialog,
    private retention: RetentionService,
  ) {}

  following = signal(false);

  /**
   * Bascule l'abonnement. L'état change d'abord et revient en arrière si l'appel
   * échoue : un bouton qui attend le serveur donne l'impression de ne pas répondre.
   */
  toggleFollow() {
    const next = !this.following();
    this.following.set(next);
    const orgId = this.org()?.id;
    if (!orgId) return;

    this.retention.toggleFollow(orgId, next).subscribe({
      next: () => this.snackBar.open(
        next ? 'Vous serez prévenu de ses prochaines missions' : 'Vous ne suivez plus cette association',
        'OK', { duration: 3000 }),
      error: () => this.following.set(!next),
    });
  }

  ngOnInit() {
    const slug = this.route.snapshot.paramMap.get('slug')!;
    this.orgService.getBySlug(slug).subscribe({
      next: res => {
        this.org.set(res.data);
        this.loading.set(false);
        this.loadAnnouncements(res.data.id);
        this.loadEvents(res.data.id);
        this.checkMembership(res.data.id);
        if (this.authService.isLoggedIn()) {
          this.retention.followState(res.data.id).subscribe({
            next: state => this.following.set(state.data.following),
            error: () => {},
          });
        }
      },
      error: () => this.loading.set(false),
    });
  }

  loadEvents(orgId: string) {
    this.loadingEvents.set(true);
    this.eventService.listEvents({ orgId, size: 20 }).subscribe({
      next: res => {
        this.orgEvents.set(res.data.content);
        this.loadingEvents.set(false);
      },
      error: () => this.loadingEvents.set(false),
    });
  }

  private checkMembership(orgId: string) {
    this.membershipService.getMyMembership(orgId).subscribe({
      next: m => {
        this.membershipStatus.set(m.data.status);
        this.membershipRole.set(m.data.role);
      },
      error: () => {},
    });
  }

  loadAnnouncements(orgId: string) {
    this.loadingAnnouncements.set(true);
    this.announcementService.list(orgId).subscribe({
      next: res => {
        this.announcements.set(res.data);
        this.loadingAnnouncements.set(false);
      },
      error: () => this.loadingAnnouncements.set(false),
    });
  }

  reportOrg() {
    const o = this.org()!;
    this.dialog.open(ReportDialogComponent, {
      data: { targetType: 'ORGANIZATION', targetId: o.id, targetLabel: o.name },
    });
  }

  joinOrg() {
    const org = this.org()!;
    const ref = this.dialog.open(JoinDialogComponent, {
      data: { orgId: org.id, orgName: org.name },
      width: '520px',
    });
    ref.afterClosed().subscribe((result: any) => {
      if (result) {
        this.membershipStatus.set(result.status);
        this.snackBar.open('Demande d\'adhésion envoyée, en attente de validation.', 'Fermer', { duration: 4000 });
      }
    });
  }
}
