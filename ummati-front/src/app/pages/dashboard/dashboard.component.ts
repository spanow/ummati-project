import { Component, signal, OnInit } from '@angular/core';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { RouterLink } from '@angular/router';
import { DatePipe } from '@angular/common';
import { DashboardService, VolunteerDashboard } from '../../core/services/dashboard.service';
import { TPipe } from '../../shared/pipes/t.pipe';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [MatCardModule, MatButtonModule, MatIconModule, MatChipsModule,
    MatProgressSpinnerModule, RouterLink, DatePipe, TPipe],
  template: `
    <div class="page-container">
      @if (loading()) {
        <div class="loading"><mat-spinner diameter="40" /></div>
      } @else if (dashboard()) {
        <header class="welcome">
          <h1>{{ 'Bonjour,' | t }} {{ dashboard()!.firstName }} 👋</h1>
          <p class="subtitle">{{ 'Votre espace bénévole' | t }}</p>
        </header>

        @if (!dashboard()!.onboardingDone) {
          <mat-card class="onboarding-banner">
            <mat-card-content>
              <mat-icon>emoji_objects</mat-icon>
              <div>
                <strong>{{ 'Complétez votre profil !' | t }}</strong>
                <p>{{ 'Ajoutez vos compétences pour recevoir des suggestions personnalisées.' | t }}</p>
              </div>
              <a mat-flat-button routerLink="/onboarding">{{ 'Compléter' | t }}</a>
            </mat-card-content>
          </mat-card>
        }

        <div class="stats-row">
          <mat-card class="stat-card">
            <mat-card-content>
              <mat-icon>event_available</mat-icon>
              <div class="stat-value">{{ dashboard()!.stats.eventsAttended }}</div>
              <div class="stat-label">{{ 'Événements participés' | t }}</div>
            </mat-card-content>
          </mat-card>
          <mat-card class="stat-card">
            <mat-card-content>
              <mat-icon>groups</mat-icon>
              <div class="stat-value">{{ dashboard()!.stats.organizationsJoined }}</div>
              <div class="stat-label">{{ 'Organisations' | t }}</div>
            </mat-card-content>
          </mat-card>
        </div>

        @if (dashboard()!.upcomingEvents.length > 0) {
          <section>
            <h2>{{ 'Mes prochains événements' | t }}</h2>
            <div class="card-row">
              @for (e of dashboard()!.upcomingEvents; track e.id) {
                <mat-card class="event-mini" [routerLink]="['/events', e.id]">
                  <mat-card-content>
                    <strong>{{ e.title }}</strong>
                    <span class="meta">{{ e.startDate | date:'d MMM, HH:mm' }} · {{ e.locationCity }}</span>
                  </mat-card-content>
                </mat-card>
              }
            </div>
          </section>
        }

        @if (dashboard()!.myOrganizations.length > 0) {
          <section>
            <h2>{{ 'Mes organisations' | t }}</h2>
            <div class="card-row">
              @for (o of dashboard()!.myOrganizations; track o.id) {
                <mat-card class="org-mini" [routerLink]="['/organizations', o.slug]">
                  <mat-card-content>
                    <strong>{{ o.name }}</strong>
                    <span class="meta">{{ o.city }} · {{ o.memberCount }} {{ 'membres' | t }}</span>
                  </mat-card-content>
                </mat-card>
              }
            </div>
          </section>
        }

        @if (dashboard()!.suggestedEvents.length > 0) {
          <section>
            <h2>{{ 'Événements suggérés' | t }}</h2>
            <div class="card-row">
              @for (e of dashboard()!.suggestedEvents; track e.id) {
                <mat-card class="event-mini" [routerLink]="['/events', e.id]">
                  <mat-card-content>
                    <mat-chip class="type-chip">{{ e.type }}</mat-chip>
                    <strong>{{ e.title }}</strong>
                    <span class="meta">{{ e.startDate | date:'d MMM' }} · {{ e.organizationName }}</span>
                  </mat-card-content>
                </mat-card>
              }
            </div>
          </section>
        }
      }
    </div>
  `,
  styles: [`
    .page-container { max-width: 1000px; margin: 0 auto; padding: 32px 24px; }
    .loading { display: flex; justify-content: center; padding: 80px; }
    .welcome h1 { font-size: 2rem; font-weight: 700; margin: 0; }
    .subtitle { color: #666; margin-top: 4px; font-size: 1.05rem; }
    .onboarding-banner { margin: 24px 0; border-radius: 12px; background: #fff3e0; }
    .onboarding-banner mat-card-content { display: flex; align-items: center; gap: 16px; }
    .onboarding-banner mat-icon { font-size: 32px; width: 32px; height: 32px; color: #e65100; }
    .onboarding-banner p { margin: 4px 0 0; color: #666; }
    .stats-row { display: flex; gap: 16px; margin: 24px 0; }
    .stat-card { flex: 1; border-radius: 12px; text-align: center; }
    .stat-card mat-icon { color: var(--brand-primary); font-size: 28px; width: 28px; height: 28px; }
    .stat-value { font-size: 2rem; font-weight: 700; margin: 4px 0; }
    .stat-label { color: #888; font-size: 0.85rem; }
    section { margin-top: 32px; }
    section h2 { font-size: 1.3rem; font-weight: 600; margin-bottom: 16px; }
    .card-row { display: grid; grid-template-columns: repeat(auto-fill, minmax(240px, 1fr)); gap: 12px; }
    .event-mini, .org-mini { border-radius: 10px; cursor: pointer; transition: box-shadow 0.2s; }
    .event-mini:hover, .org-mini:hover { box-shadow: 0 4px 16px rgba(0,0,0,0.1); }
    .meta { display: block; color: #888; font-size: 0.8rem; margin-top: 4px; }
    .type-chip { font-size: 10px; margin-bottom: 6px; }
  `],
})
export class DashboardComponent implements OnInit {
  dashboard = signal<VolunteerDashboard | null>(null);
  loading = signal(true);

  constructor(private dashboardService: DashboardService) {}

  ngOnInit() {
    this.dashboardService.getVolunteerDashboard().subscribe({
      next: res => { this.dashboard.set(res.data); this.loading.set(false); },
      error: () => this.loading.set(false),
    });
  }
}

