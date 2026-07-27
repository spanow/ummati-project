import { Component, signal, OnInit, inject, PLATFORM_ID } from '@angular/core';
import { isPlatformBrowser, DatePipe } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { FormsModule } from '@angular/forms';
import { ProfileService, VolunteerPassport } from '../../core/services/profile.service';
import { AuthService } from '../../core/services/auth.service';
import { AvatarComponent } from '../../shared/components/avatar/avatar.component';
import { TPipe } from '../../shared/pipes/t.pipe';

/** Libellés lisibles des domaines d'engagement renvoyés par l'API. */
const DOMAIN_LABELS: Record<string, string> = {
  EDUCATION: 'Éducation',
  SANTE: 'Santé',
  ENVIRONNEMENT: 'Environnement',
  SOCIAL: 'Social',
  CULTURE: 'Culture',
  SPORT: 'Sport',
  HUMANITAIRE: 'Humanitaire',
  DROITS_HUMAINS: 'Droits humains',
  AIDE_URGENCE: 'Aide d\'urgence',
  AUTRE: 'Autre',
};

/**
 * Passeport bénévole — la vitrine d'un bénévole.
 *
 * La même route sert le passeport public d'un tiers et le sien : sur son propre
 * profil on interroge l'endpoint privé, qui répond même quand la publication est
 * désactivée, et on expose le réglage de visibilité.
 */
@Component({
  selector: 'app-volunteer-passport',
  standalone: true,
  imports: [MatCardModule, MatButtonModule, MatIconModule, MatChipsModule,
    MatProgressSpinnerModule, MatSlideToggleModule, MatSnackBarModule, FormsModule,
    RouterLink, DatePipe, AvatarComponent, TPipe],
  template: `
    <div class="page page-narrow">
      @if (loading()) {
        <div class="state-center"><mat-spinner diameter="40" /></div>
      } @else if (notFound()) {
        <div class="empty-state">
          <div class="empty-badge"><mat-icon>badge</mat-icon></div>
          <h3>{{ 'Passeport indisponible' | t }}</h3>
          <p>{{ 'Ce bénévole n\\'a pas rendu son passeport public.' | t }}</p>
          <a mat-flat-button routerLink="/events">{{ 'Découvrir les missions' | t }}</a>
        </div>
      } @else if (passport(); as p) {

        <header class="passport-head">
          <app-avatar [name]="p.firstName + ' ' + p.lastName" [photoUrl]="p.photoUrl" [size]="88" />
          <div class="head-text">
            <h1>{{ p.firstName }} {{ p.lastName }}</h1>
            <p class="head-meta">
              @if (p.city) { <span><mat-icon>place</mat-icon>{{ p.city }}</span> }
              <span><mat-icon>schedule</mat-icon>{{ 'Bénévole depuis' | t }} {{ p.memberSince | date:'MMMM yyyy' }}</span>
            </p>
            @if (p.bio) { <p class="head-bio">{{ p.bio }}</p> }
          </div>
        </header>

        <!-- Les trois chiffres qui résument l'engagement -->
        <div class="stat-row">
          <div class="stat">
            <b>{{ p.missionsCompleted }}</b>
            <span>{{ (p.missionsCompleted > 1 ? 'missions accomplies' : 'mission accomplie') | t }}</span>
          </div>
          <div class="stat">
            <b>{{ p.hoursTotal }}</b>
            <span>{{ (p.hoursTotal > 1 ? 'heures données' : 'heure donnée') | t }}</span>
          </div>
          <div class="stat">
            <b>{{ p.organizationCount }}</b>
            <span>{{ (p.organizationCount > 1 ? 'associations soutenues' : 'association soutenue') | t }}</span>
          </div>
        </div>

        @if (p.causes.length > 0) {
          <mat-card class="passport-card">
            <mat-card-content>
              <h2>{{ 'Ses causes' | t }}</h2>
              <div class="chip-row">
                @for (cause of p.causes; track cause.domain) {
                  <mat-chip>{{ domainLabel(cause.domain) }} · {{ cause.missionCount }}</mat-chip>
                }
              </div>
            </mat-card-content>
          </mat-card>
        }

        @if (p.skills.length > 0) {
          <mat-card class="passport-card">
            <mat-card-content>
              <h2>{{ 'Ses compétences' | t }}</h2>
              <div class="chip-row">
                @for (skill of p.skills; track skill.id) {
                  <mat-chip>{{ skill.name }}</mat-chip>
                }
              </div>
            </mat-card-content>
          </mat-card>
        }

        @if (p.recentMissions.length > 0) {
          <mat-card class="passport-card">
            <mat-card-content>
              <h2>{{ 'Ses dernières missions' | t }}</h2>
              <ul class="mission-list">
                @for (mission of p.recentMissions; track mission.eventId) {
                  <li>
                    <a class="mission-title" [routerLink]="['/events', mission.eventId]">{{ mission.title }}</a>
                    <span class="mission-meta">
                      <a [routerLink]="['/organizations', mission.organizationSlug]">{{ mission.organizationName }}</a>
                      · {{ mission.date | date:'d MMM yyyy' }}
                    </span>
                  </li>
                }
              </ul>
            </mat-card-content>
          </mat-card>
        }

        @if (p.missionsCompleted === 0) {
          <mat-card class="passport-card">
            <mat-card-content class="no-mission">
              <mat-icon>volunteer_activism</mat-icon>
              <p>{{ isOwnPassport() ? ('Votre passeport se remplira dès votre première mission.' | t)
                                     : ('Ce bénévole n\\'a pas encore accompli de mission.' | t) }}</p>
              @if (isOwnPassport()) {
                <a mat-flat-button routerLink="/events">{{ 'Trouver une mission' | t }}</a>
              }
            </mat-card-content>
          </mat-card>
        }

        @if (isOwnPassport()) {
          <mat-card class="passport-card visibility-card">
            <mat-card-content>
              <h2>{{ 'Visibilité' | t }}</h2>
              <mat-slide-toggle [checked]="p.profilePublic" [disabled]="savingVisibility()"
                                (change)="toggleVisibility($event.checked)">
                {{ 'Rendre mon passeport public' | t }}
              </mat-slide-toggle>
              <p class="visibility-help">
                {{ 'Une fois public, votre passeport est consultable par toute personne disposant du lien. Seuls votre prénom et l\\'initiale de votre nom sont affichés — jamais votre email, votre téléphone ni votre adresse.' | t }}
              </p>
              @if (p.profilePublic) {
                <div class="share-row">
                  <code class="share-link">{{ shareUrl() }}</code>
                  <button mat-stroked-button type="button" (click)="copyLink()">
                    <mat-icon>content_copy</mat-icon> {{ 'Copier le lien' | t }}
                  </button>
                </div>
              }
            </mat-card-content>
          </mat-card>
        }
      }
    </div>
  `,
  styles: [`
    .state-center { display: flex; justify-content: center; padding: 64px 0; }

    .empty-state { text-align: center; padding: var(--space-9) var(--space-5); }
    .empty-badge {
      width: 76px; height: 76px; margin: 0 auto var(--space-4); border-radius: 50%;
      background: var(--brand-surface-2); border: 1px solid var(--brand-border);
      display: flex; align-items: center; justify-content: center;
    }
    .empty-badge mat-icon { font-size: 34px; width: 34px; height: 34px; color: var(--brand-text-faint); }
    .empty-state h3 { font-size: 1.25rem; margin: 0 0 var(--space-2); }
    .empty-state p { color: var(--brand-text-soft); margin: 0 0 var(--space-5); }

    .passport-head { display: flex; gap: 24px; align-items: flex-start; margin-bottom: 28px; flex-wrap: wrap; }
    .head-text { flex: 1; min-width: 220px; }
    .head-text h1 { font-size: 1.9rem; font-weight: 800; margin: 0 0 8px; letter-spacing: -0.02em; }
    .head-meta {
      display: flex; gap: 16px; flex-wrap: wrap; margin: 0 0 12px;
      color: var(--brand-text-soft); font-size: 0.88rem;
    }
    .head-meta span { display: flex; align-items: center; gap: 6px; }
    .head-meta mat-icon { font-size: 16px; width: 16px; height: 16px; }
    .head-bio { margin: 0; line-height: 1.6; color: var(--brand-text-soft); }

    .stat-row {
      display: grid; grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
      gap: 12px; margin-bottom: 24px;
    }
    .stat {
      background: var(--brand-primary-soft); border: 1px solid var(--brand-primary-100);
      border-radius: var(--radius-card); padding: 20px; text-align: center;
    }
    .stat b {
      display: block; font-size: 2rem; font-weight: 800; line-height: 1.1;
      color: var(--brand-primary-dark); font-variant-numeric: tabular-nums;
    }
    .stat span { font-size: 0.82rem; color: var(--brand-text-soft); }

    .passport-card { margin-bottom: 16px; }
    .passport-card h2 { font-size: 1.05rem; font-weight: 700; margin: 0 0 14px; }
    .chip-row { display: flex; gap: 8px; flex-wrap: wrap; }

    .mission-list { list-style: none; margin: 0; padding: 0; display: flex; flex-direction: column; gap: 14px; }
    .mission-list li { display: flex; flex-direction: column; gap: 3px; }
    .mission-title { font-weight: 600; color: var(--brand-ink); }
    .mission-meta { font-size: 0.83rem; color: var(--brand-text-soft); }

    .no-mission { text-align: center; padding: 12px 0; }
    .no-mission mat-icon {
      font-size: 40px; width: 40px; height: 40px; color: var(--brand-text-faint); margin-bottom: 8px;
    }
    .no-mission p { color: var(--brand-text-soft); margin: 0 0 16px; }

    .visibility-help { color: var(--brand-text-soft); font-size: 0.85rem; line-height: 1.6; margin: 12px 0 0; }
    .share-row { display: flex; gap: 10px; align-items: center; margin-top: 14px; flex-wrap: wrap; }
    .share-link {
      flex: 1; min-width: 200px; overflow-x: auto; white-space: nowrap;
      background: var(--brand-surface-2); border: 1px solid var(--brand-border);
      border-radius: var(--radius-sm); padding: 8px 12px; font-size: 0.82rem;
    }
  `],
})
export class VolunteerPassportComponent implements OnInit {
  passport = signal<VolunteerPassport | null>(null);
  loading = signal(true);
  notFound = signal(false);
  savingVisibility = signal(false);
  isOwnPassport = signal(false);

  private platformId = inject(PLATFORM_ID);

  constructor(
    private route: ActivatedRoute,
    private profileService: ProfileService,
    private authService: AuthService,
    private snackBar: MatSnackBar,
  ) {}

  ngOnInit() {
    const userId = this.route.snapshot.paramMap.get('id')!;
    this.isOwnPassport.set(this.authService.user()?.id === userId);

    // Sur son propre passeport, l'endpoint privé répond même sans publication.
    const request$ = this.isOwnPassport()
      ? this.profileService.getMyPassport()
      : this.profileService.getPublicPassport(userId);

    request$.subscribe({
      next: res => { this.passport.set(res.data); this.loading.set(false); },
      error: () => { this.notFound.set(true); this.loading.set(false); },
    });
  }

  domainLabel(domain: string): string {
    return DOMAIN_LABELS[domain] ?? domain;
  }

  shareUrl(): string {
    const id = this.passport()?.userId ?? '';
    if (!isPlatformBrowser(this.platformId)) return `/volunteers/${id}`;
    return `${window.location.origin}/volunteers/${id}`;
  }

  toggleVisibility(isPublic: boolean) {
    this.savingVisibility.set(true);
    this.profileService.setPassportVisibility(isPublic).subscribe({
      next: res => {
        this.passport.set(res.data);
        this.savingVisibility.set(false);
        this.snackBar.open(
          isPublic ? 'Votre passeport est désormais public' : 'Votre passeport est redevenu privé',
          'OK', { duration: 3000 });
      },
      error: err => {
        this.savingVisibility.set(false);
        this.snackBar.open(err.error?.message || 'Erreur', 'OK', { duration: 3000 });
      },
    });
  }

  copyLink() {
    if (!isPlatformBrowser(this.platformId)) return;
    navigator.clipboard.writeText(this.shareUrl()).then(
      () => this.snackBar.open('Lien copié', 'OK', { duration: 2000 }),
      () => this.snackBar.open('Impossible de copier le lien', 'OK', { duration: 3000 }),
    );
  }
}
