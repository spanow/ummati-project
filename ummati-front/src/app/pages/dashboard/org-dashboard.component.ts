import { Component, signal, OnInit } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatChipsModule } from '@angular/material/chips';
import { DatePipe, DecimalPipe } from '@angular/common';
import { DashboardService, OrgAdminDashboard } from '../../core/services/dashboard.service';

@Component({
  selector: 'app-org-dashboard',
  standalone: true,
  imports: [MatCardModule, MatButtonModule, MatIconModule, MatProgressSpinnerModule, MatChipsModule, RouterLink, DatePipe, DecimalPipe],
  template: `
    <div class="page-container">
      @if (loading()) {
        <div class="loading"><mat-spinner diameter="40" /></div>
      } @else if (dashboard()) {
        <header class="page-header">
          <div>
            <h1>{{ dashboard()!.organizationName }}</h1>
            <p class="subtitle">Tableau de bord administrateur</p>
          </div>
          <a mat-stroked-button [routerLink]="['/organizations', orgId, 'events', 'manage']">
            <mat-icon>event</mat-icon> Gérer les événements
          </a>
        </header>

        <div class="stats-row">
          <mat-card class="stat-card">
            <mat-card-content>
              <mat-icon>people</mat-icon>
              <div class="val">{{ dashboard()!.activeMembers }}</div>
              <div class="lbl">Membres actifs</div>
            </mat-card-content>
          </mat-card>
          <mat-card class="stat-card" [class.alert]="dashboard()!.pendingRequests > 0">
            <mat-card-content>
              <mat-icon>person_add</mat-icon>
              <div class="val">{{ dashboard()!.pendingRequests }}</div>
              <div class="lbl">Demandes en attente</div>
            </mat-card-content>
          </mat-card>
          <mat-card class="stat-card">
            <mat-card-content>
              <mat-icon>event</mat-icon>
              <div class="val">{{ dashboard()!.eventsThisMonth }}</div>
              <div class="lbl">Événements ce mois</div>
            </mat-card-content>
          </mat-card>
          <mat-card class="stat-card">
            <mat-card-content>
              <mat-icon>bar_chart</mat-icon>
              <div class="val">{{ dashboard()!.totalEvents }}</div>
              <div class="lbl">Total événements</div>
            </mat-card-content>
          </mat-card>
          @if (dashboard()!.averageFeedbackRating) {
            <mat-card class="stat-card">
              <mat-card-content>
                <mat-icon>star</mat-icon>
                <div class="val">{{ dashboard()!.averageFeedbackRating! | number:'1.1-1' }}/5</div>
                <div class="lbl">Note moyenne</div>
              </mat-card-content>
            </mat-card>
          }
        </div>

        @if (dashboard()!.pendingRequests > 0) {
          <mat-card class="alert-banner">
            <mat-card-content>
              <mat-icon>notifications_active</mat-icon>
              <span>{{ dashboard()!.pendingRequests }} demande(s) d'adhésion en attente</span>
              <a mat-stroked-button [routerLink]="['/organizations', orgId, 'manage']">Gérer</a>
            </mat-card-content>
          </mat-card>
        }

        @if (dashboard()!.recentMembers.length > 0) {
          <section>
            <h2>Derniers membres</h2>
            <div class="members-list">
              @for (m of dashboard()!.recentMembers; track m.joinedAt) {
                <div class="member-row">
                  <mat-icon>person</mat-icon>
                  <span>{{ m.firstName }} {{ m.lastName }}</span>
                  <mat-chip>{{ m.role }}</mat-chip>
                  <span class="date">{{ m.joinedAt | date:'d MMM yyyy' }}</span>
                </div>
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
    .page-header { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 32px; }
    .page-header h1 { font-size: 1.8rem; font-weight: 700; margin: 0; }
    .subtitle { color: #666; margin-top: 4px; }
    .stats-row { display: flex; gap: 12px; flex-wrap: wrap; margin-bottom: 24px; }
    .stat-card { flex: 1; min-width: 140px; border-radius: 10px; text-align: center; }
    .stat-card mat-icon { color: #1976d2; margin-bottom: 8px; }
    .stat-card .val { font-size: 1.8rem; font-weight: 700; }
    .stat-card .lbl { color: #888; font-size: 0.8rem; }
    .stat-card.alert { border: 2px solid #ff9800; }
    .alert-banner { border-radius: 10px; background: #fff3e0; margin-bottom: 24px; }
    .alert-banner mat-card-content { display: flex; align-items: center; gap: 12px; }
    .alert-banner mat-icon { color: #e65100; }
    .alert-banner span { flex: 1; }
    section h2 { font-size: 1.2rem; font-weight: 600; margin-bottom: 12px; }
    .members-list { display: flex; flex-direction: column; gap: 8px; }
    .member-row { display: flex; align-items: center; gap: 12px; padding: 10px; background: #fafafa; border-radius: 8px; }
    .member-row mat-icon { color: #888; }
    .member-row span:first-of-type { flex: 1; font-weight: 500; }
    .date { color: #999; font-size: 0.8rem; }
  `],
})
export class OrgDashboardComponent implements OnInit {
  dashboard = signal<OrgAdminDashboard | null>(null);
  loading = signal(true);
  orgId = '';

  constructor(private route: ActivatedRoute, private dashboardService: DashboardService) {}

  ngOnInit() {
    this.orgId = this.route.snapshot.paramMap.get('orgId') || this.route.snapshot.paramMap.get('slug') || '';
    this.dashboardService.getOrgAdminDashboard(this.orgId).subscribe({
      next: res => { this.dashboard.set(res.data); this.loading.set(false); },
      error: () => this.loading.set(false),
    });
  }
}



