import { Component, signal, OnInit, inject, afterNextRender } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatTabsModule } from '@angular/material/tabs';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { DatePipe, DecimalPipe } from '@angular/common';
import { OrganizationService, OrganizationDetail } from '../../../core/services/organization.service';
import { MembershipService } from '../../../core/services/membership.service';
import { AuthService } from '../../../core/services/auth.service';
import { OrgAnnouncementService, OrgAnnouncementResponse } from '../../../core/services/org-announcement.service';

@Component({
  selector: 'app-organization-detail',
  standalone: true,
  imports: [MatCardModule, MatButtonModule, MatIconModule, MatChipsModule, MatTabsModule, MatProgressSpinnerModule, MatSnackBarModule, DecimalPipe, DatePipe, RouterLink],
  template: `
    @if (loading()) {
      <div class="loading"><mat-spinner diameter="40" /></div>
    } @else if (org()) {
      <div class="detail-page">
        <div class="banner" [style.background-image]="org()!.bannerUrl ? 'url(' + org()!.bannerUrl + ')' : ''">
          <div class="banner-overlay">
            <div class="org-identity">
              @if (org()!.logoUrl) {
                <img [src]="org()!.logoUrl" class="logo" />
              } @else {
                <div class="logo-placeholder"><mat-icon>business</mat-icon></div>
              }
              <div>
                <h1>{{ org()!.name }}</h1>
                <div class="meta-row">
                  <mat-chip>{{ org()!.domain }}</mat-chip>
                  <span class="location"><mat-icon>location_on</mat-icon> {{ org()!.addressCity }}</span>
                </div>
              </div>
            </div>
          </div>
        </div>

        <div class="content-container">
          <div class="stats-bar">
            <div class="stat">
              <span class="stat-value">{{ org()!.stats.memberCount }}</span>
              <span class="stat-label">Membres</span>
            </div>
            <div class="stat">
              <span class="stat-value">{{ org()!.stats.eventCount }}</span>
              <span class="stat-label">Événements</span>
            </div>
            <div class="stat">
              <span class="stat-value">{{ org()!.stats.averageRating ? (org()!.stats.averageRating | number:'1.1-1') : '—' }}</span>
              <span class="stat-label">Note moyenne</span>
            </div>
            @if (membershipRole() === 'ADMIN') {
              <div class="admin-actions">
                <a mat-flat-button color="primary"
                   [routerLink]="['/organizations', org()!.id, 'events', 'new']">
                  <mat-icon>add</mat-icon> Créer un événement
                </a>
                <a mat-stroked-button
                   [routerLink]="['/organizations', org()!.slug, 'manage']"
                   [queryParams]="{ orgId: org()!.id }">
                  <mat-icon>settings</mat-icon> Gérer
                </a>
              </div>
            } @else if (membershipStatus() === 'ACTIVE') {
              <button mat-stroked-button class="join-btn" disabled>Membre</button>
            } @else if (membershipStatus() === 'PENDING') {
              <button mat-stroked-button class="join-btn" disabled>En attente</button>
            } @else if (!isLoggedIn()) {
              <a mat-flat-button class="join-btn" routerLink="/login">Rejoindre</a>
            } @else {
              <button mat-flat-button class="join-btn" [disabled]="joining()" (click)="joinOrg()">
                {{ joining() ? 'Envoi…' : 'Rejoindre' }}
              </button>
            }
          </div>

          <mat-tab-group>
            <mat-tab label="À propos">
              <div class="tab-content">
                <h3>Description</h3>
                <p class="description">{{ org()!.description }}</p>
                @if (org()!.mission) {
                  <h3>Mission</h3>
                  <p>{{ org()!.mission }}</p>
                }
                <div class="contact-info">
                  @if (org()!.email) { <div><mat-icon>email</mat-icon> {{ org()!.email }}</div> }
                  @if (org()!.phone) { <div><mat-icon>phone</mat-icon> {{ org()!.phone }}</div> }
                  @if (org()!.website) { <div><mat-icon>language</mat-icon> <a [href]="org()!.website" target="_blank">{{ org()!.website }}</a></div> }
                </div>
              </div>
            </mat-tab>

            <mat-tab label="Événements">
              <div class="tab-content">
                @if (membershipRole() === 'ADMIN') {
                  <div class="events-admin-bar">
                    <a mat-flat-button color="primary"
                       [routerLink]="['/organizations', org()!.id, 'events', 'new']">
                      <mat-icon>add</mat-icon> Créer un événement
                    </a>
                    <a mat-stroked-button [routerLink]="['/organizations', org()!.id, 'events', 'manage']">
                      <mat-icon>list</mat-icon> Gérer les événements
                    </a>
                  </div>
                }
                <p class="placeholder-text">Les événements publiés apparaîtront ici.</p>
              </div>
            </mat-tab>

            <mat-tab>
              <ng-template matTabLabel>
                Annonces
                @if (announcements().length > 0) {
                  <span class="tab-badge">{{ announcements().length }}</span>
                }
              </ng-template>
              <div class="tab-content">
                @if (loadingAnnouncements()) {
                  <div class="loading-inline"><mat-spinner diameter="28" /></div>
                } @else if (announcements().length === 0) {
                  <p class="placeholder-text">Aucune annonce pour le moment.</p>
                } @else {
                  <div class="announcement-list">
                    @for (a of announcements(); track a.id) {
                      <div class="announcement-card" [class.pinned]="a.pinned">
                        @if (a.pinned) {
                          <span class="pin-badge"><mat-icon>push_pin</mat-icon> Épinglé</span>
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
    .loading { display: flex; justify-content: center; padding: 120px 0; }
    .loading-inline { display: flex; justify-content: center; padding: 32px; }
    .detail-page { max-width: 960px; margin: 0 auto; }
    .banner { height: 200px; background: linear-gradient(135deg, #1976d2 0%, #42a5f5 100%); position: relative; }
    .banner-overlay { position: absolute; inset: 0; background: rgba(0,0,0,0.2); display: flex; align-items: flex-end; padding: 24px 32px; }
    .org-identity { display: flex; align-items: center; gap: 20px; color: white; }
    .logo { width: 72px; height: 72px; border-radius: 14px; border: 3px solid white; object-fit: cover; }
    .logo-placeholder { width: 72px; height: 72px; border-radius: 14px; border: 3px solid white; background: rgba(255,255,255,0.2); display: flex; align-items: center; justify-content: center; }
    .logo-placeholder mat-icon { font-size: 36px; width: 36px; height: 36px; color: white; }
    h1 { margin: 0 0 6px; font-size: 1.8rem; font-weight: 600; }
    .meta-row { display: flex; align-items: center; gap: 12px; }
    .location { display: flex; align-items: center; gap: 4px; font-size: 0.9rem; opacity: 0.9; }
    .content-container { padding: 0 32px 48px; }
    .stats-bar { display: flex; align-items: center; gap: 40px; padding: 24px 0; border-bottom: 1px solid #eee; margin-bottom: 24px; }
    .stat { display: flex; flex-direction: column; align-items: center; }
    .stat-value { font-size: 1.5rem; font-weight: 700; color: #1976d2; }
    .stat-label { font-size: 0.8rem; color: #888; margin-top: 2px; }
    .join-btn { margin-left: auto; height: 44px; padding: 0 32px; }
    .admin-actions { margin-left: auto; display: flex; gap: 8px; }
    .events-admin-bar { display: flex; gap: 8px; margin-bottom: 24px; }
    .tab-content { padding: 24px 0; }
    .tab-content h3 { font-size: 1.1rem; font-weight: 600; margin: 0 0 12px; }
    .description { line-height: 1.7; color: #444; white-space: pre-line; }
    .contact-info { margin-top: 24px; display: flex; flex-direction: column; gap: 10px; }
    .contact-info div { display: flex; align-items: center; gap: 8px; color: #555; }
    .contact-info a { color: #1976d2; text-decoration: none; }
    .placeholder-text { color: #888; font-style: italic; padding: 40px 0; text-align: center; }
    .tab-badge { background: #1976d2; color: white; border-radius: 10px; padding: 1px 7px; font-size: 11px; margin-left: 6px; }
    .announcement-list { display: flex; flex-direction: column; gap: 16px; }
    .announcement-card { padding: 20px 24px; border-radius: 10px; border: 1px solid #e0e0e0; background: white; }
    .announcement-card.pinned { border-left: 4px solid #1976d2; background: #f5f9ff; }
    .pin-badge { display: inline-flex; align-items: center; gap: 4px; font-size: 0.75rem; color: #1976d2; font-weight: 600; margin-bottom: 8px; }
    .pin-badge mat-icon { font-size: 14px; width: 14px; height: 14px; }
    .announcement-title { margin: 0 0 8px; font-size: 1rem; font-weight: 600; color: #222; }
    .announcement-content { margin: 0 0 12px; color: #444; line-height: 1.6; white-space: pre-line; }
    .announcement-meta { font-size: 0.8rem; color: #999; }
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

  private route = inject(ActivatedRoute);
  private orgService = inject(OrganizationService);
  private membershipService = inject(MembershipService);
  private announcementService = inject(OrgAnnouncementService);
  private snackBar = inject(MatSnackBar);
  private authService = inject(AuthService);
  protected isLoggedIn = this.authService.isLoggedIn;

  constructor() {
    // afterNextRender must be in an injection context (constructor/field initializer).
    // It fires only in the browser after hydration — the SSR path where isLoggedIn()
    // was false and the inline check was skipped.
    afterNextRender(() => {
      const o = this.org();
      if (o && this.isLoggedIn() && this.membershipRole() === null && this.membershipStatus() === null) {
        this.checkMembership(o.id);
      }
    });
  }

  ngOnInit() {
    const slug = this.route.snapshot.paramMap.get('slug')!;
    this.orgService.getBySlug(slug).subscribe({
      next: res => {
        this.org.set(res.data);
        this.loading.set(false);
        this.loadAnnouncements(res.data.id);
        // Pure client-side path (no SSR): check membership inline once org is loaded.
        if (this.isLoggedIn()) {
          this.checkMembership(res.data.id);
        }
      },
      error: () => this.loading.set(false),
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

  joinOrg() {
    const orgId = this.org()!.id;
    this.joining.set(true);
    this.membershipService.requestMembership(orgId).subscribe({
      next: res => {
        this.joining.set(false);
        this.membershipStatus.set(res.data.status);
        this.snackBar.open('Demande d\'adhésion envoyée, en attente de validation.', 'Fermer', { duration: 4000 });
      },
      error: err => {
        this.joining.set(false);
        const msg = err?.error?.message ?? 'Une erreur est survenue. Veuillez réessayer.';
        this.snackBar.open(msg, 'Fermer', { duration: 4000 });
      },
    });
  }
}
