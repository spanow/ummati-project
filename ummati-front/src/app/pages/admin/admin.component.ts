import { Component, signal, OnInit } from '@angular/core';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTabsModule } from '@angular/material/tabs';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatChipsModule } from '@angular/material/chips';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { FormsModule } from '@angular/forms';
import { DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { AdminService, AdminStats, AdminUserSummary } from '../../core/services/admin.service';
import { ReportService, ReportResponse, ReportStatus, REPORT_REASON_LABELS } from '../../core/services/report.service';
import { TPipe } from '../../shared/pipes/t.pipe';

@Component({
  selector: 'app-admin',
  standalone: true,
  imports: [MatCardModule, MatButtonModule, MatIconModule, MatTabsModule, MatFormFieldModule,
    MatInputModule, MatSelectModule, MatChipsModule, MatPaginatorModule, MatProgressSpinnerModule,
    MatSnackBarModule, FormsModule, DatePipe, RouterLink, TPipe],
  template: `
    <div class="page">
      <h1>{{ 'Administration' | t }}</h1>

      @if (stats()) {
        <div class="stats-row">
          <mat-card class="stat">
            <mat-card-content><div class="val">{{ stats()!.totalUsers }}</div><div class="lbl">{{ 'Utilisateurs' | t }}</div></mat-card-content>
          </mat-card>
          <mat-card class="stat">
            <mat-card-content><div class="val">{{ stats()!.totalOrganizations }}</div><div class="lbl">{{ 'Organisations' | t }}</div></mat-card-content>
          </mat-card>
          <mat-card class="stat">
            <mat-card-content><div class="val">{{ stats()!.totalEvents }}</div><div class="lbl">{{ 'Événements' | t }}</div></mat-card-content>
          </mat-card>
          <mat-card class="stat highlight">
            <mat-card-content><div class="val">{{ stats()!.pendingOrganizations }}</div><div class="lbl">{{ 'ONG en attente' | t }}</div></mat-card-content>
          </mat-card>
          <mat-card class="stat">
            <mat-card-content><div class="val">{{ stats()!.registrationsThisWeek }}</div><div class="lbl">{{ 'Inscriptions (7j)' | t }}</div></mat-card-content>
          </mat-card>
        </div>
      }

      <mat-tab-group>
        <mat-tab [label]="'Utilisateurs' | t">
          <div class="tab-content">
            <div class="filters">
              <mat-form-field appearance="outline" class="search-field">
                <mat-label>{{ 'Rechercher' | t }}</mat-label>
                <input matInput [(ngModel)]="userSearch" (keyup.enter)="loadUsers()" [placeholder]="'Nom, email...' | t" />
              </mat-form-field>
            </div>
            @if (usersLoading()) {
              <div class="state-center"><mat-spinner diameter="30" /></div>
            } @else {
              <table class="data-table">
                <thead><tr><th>{{ 'Nom' | t }}</th><th>{{ 'Email' | t }}</th><th>{{ 'Rôle' | t }}</th><th>{{ 'Statut' | t }}</th><th>{{ 'Inscrit le' | t }}</th><th>{{ 'Actions' | t }}</th></tr></thead>
                <tbody>
                  @for (u of users(); track u.id) {
                    <tr>
                      <td>{{ u.firstName }} {{ u.lastName }}</td>
                      <td>{{ u.email }}</td>
                      <td><mat-chip>{{ u.role }}</mat-chip></td>
                      <td>
                        <mat-chip [class]="u.enabled ? 'active' : 'disabled'">{{ (u.enabled ? 'Actif' : 'Désactivé') | t }}</mat-chip>
                      </td>
                      <td>{{ u.createdAt | date:'d MMM yyyy' }}</td>
                      <td>
                        <button mat-icon-button (click)="toggleUser(u)">
                          <mat-icon>{{ u.enabled ? 'block' : 'check_circle' }}</mat-icon>
                        </button>
                      </td>
                    </tr>
                  }
                </tbody>
              </table>
              <mat-paginator [length]="usersTotal()" [pageSize]="20" (page)="onUsersPage($event)" />
            }
          </div>
        </mat-tab>

        <mat-tab [label]="'Organisations' | t">
          <div class="tab-content">
            <div class="filters">
              <mat-form-field appearance="outline">
                <mat-label>{{ 'Statut' | t }}</mat-label>
                <mat-select [(ngModel)]="orgStatus" (selectionChange)="loadOrgs()">
                  <mat-option value="">{{ 'Tous' | t }}</mat-option>
                  <mat-option value="PENDING">{{ 'En attente' | t }}</mat-option>
                  <mat-option value="ACTIVE">{{ 'Actives' | t }}</mat-option>
                  <mat-option value="REJECTED">{{ 'Rejetées' | t }}</mat-option>
                  <mat-option value="SUSPENDED">{{ 'Suspendues' | t }}</mat-option>
                </mat-select>
              </mat-form-field>
            </div>
            @if (orgsLoading()) {
              <div class="state-center"><mat-spinner diameter="30" /></div>
            } @else {
              <table class="data-table">
                <thead><tr><th>{{ 'Nom' | t }}</th><th>{{ 'Domaine' | t }}</th><th>{{ 'Ville' | t }}</th><th>{{ 'Statut' | t }}</th><th>{{ 'Membres' | t }}</th><th>{{ 'Actions' | t }}</th></tr></thead>
                <tbody>
                  @for (o of orgs(); track o.id) {
                    <tr>
                      <td>{{ o.name }}</td>
                      <td>{{ o.domain }}</td>
                      <td>{{ o.city }}</td>
                      <td><mat-chip>{{ o.status || 'ACTIVE' }}</mat-chip></td>
                      <td>{{ o.memberCount }}</td>
                      <td>
                        <a mat-icon-button [routerLink]="['/admin/organizations', o.slug, 'validate']"
                           [title]="('Voir/valider' | t) + ' ' + o.name">
                          <mat-icon>visibility</mat-icon>
                        </a>
                      </td>
                    </tr>
                  }
                </tbody>
              </table>
              <mat-paginator [length]="orgsTotal()" [pageSize]="20" (page)="onOrgsPage($event)" />
            }
          </div>
        </mat-tab>

        <mat-tab>
          <ng-template matTabLabel>
            {{ 'Signalements' | t }}
            @if (reportsTotal() > 0 && reportStatus === 'PENDING') {
              <span class="tab-badge">{{ reportsTotal() }}</span>
            }
          </ng-template>
          <div class="tab-content">
            <div class="filters">
              <mat-form-field appearance="outline">
                <mat-label>{{ 'Statut' | t }}</mat-label>
                <mat-select [(ngModel)]="reportStatus" (selectionChange)="loadReports()">
                  <mat-option value="">{{ 'Tous' | t }}</mat-option>
                  <mat-option value="PENDING">{{ 'En attente' | t }}</mat-option>
                  <mat-option value="REVIEWED">{{ 'Examinés' | t }}</mat-option>
                  <mat-option value="DISMISSED">{{ 'Rejetés' | t }}</mat-option>
                  <mat-option value="ACTION_TAKEN">{{ 'Action prise' | t }}</mat-option>
                </mat-select>
              </mat-form-field>
            </div>
            @if (reportsLoading()) {
              <div class="state-center"><mat-spinner diameter="30" /></div>
            } @else if (reports().length === 0) {
              <p class="empty-hint">{{ 'Aucun signalement.' | t }}</p>
            } @else {
              <div class="report-list">
                @for (r of reports(); track r.id) {
                  <mat-card class="report-card">
                    <mat-card-content>
                      <div class="report-header">
                        <mat-chip>{{ r.targetType }}</mat-chip>
                        <mat-chip>{{ reasonLabels[r.reason] | t }}</mat-chip>
                        <mat-chip [class]="'status-' + r.status.toLowerCase()">{{ r.status }}</mat-chip>
                      </div>
                      <p class="report-target"><strong>{{ 'Cible :' | t }}</strong> {{ r.targetLabel }}</p>
                      <p class="report-reporter">{{ 'Signalé par' | t }} {{ r.reporterName }} {{ 'le' | t }} {{ r.createdAt | date:'d MMM yyyy, HH:mm' }}</p>
                      @if (r.description) {
                        <p class="report-description">{{ r.description }}</p>
                      }
                      @if (r.status === 'PENDING') {
                        <div class="report-actions">
                          <button mat-stroked-button (click)="resolveReport(r, 'DISMISSED')">{{ 'Rejeter' | t }}</button>
                          <button mat-flat-button color="warn" (click)="resolveReport(r, 'ACTION_TAKEN')">{{ 'Action prise' | t }}</button>
                          <button mat-button (click)="resolveReport(r, 'REVIEWED')">{{ 'Marquer examiné' | t }}</button>
                        </div>
                      } @else {
                        <p class="report-resolution">
                          {{ 'Traité par' | t }} {{ r.reviewedByName }} {{ 'le' | t }} {{ r.reviewedAt | date:'d MMM yyyy, HH:mm' }}
                          @if (r.resolutionNote) { — {{ r.resolutionNote }} }
                        </p>
                      }
                    </mat-card-content>
                  </mat-card>
                }
              </div>
              <mat-paginator [length]="reportsTotal()" [pageSize]="20" (page)="onReportsPage($event)" />
            }
          </div>
        </mat-tab>
      </mat-tab-group>
    </div>
  `,
  styles: [`
    h1 { font-size: 2rem; font-weight: 800; margin-bottom: 24px; letter-spacing: -0.02em; }
    .stats-row { display: flex; gap: 12px; margin-bottom: 32px; flex-wrap: wrap; }
    .stat { flex: 1; min-width: 140px; text-align: center; }
    .stat .val { font-size: 1.8rem; font-weight: 800; color: var(--brand-ink); }
    .stat .lbl { color: var(--brand-text-soft); font-size: 0.8rem; }
    .stat.highlight { border: 2px solid var(--brand-accent); }
    .tab-content { padding: 16px 0; }
    .filters { margin-bottom: 16px; }
    .search-field { width: 300px; max-width: 100%; }
    .data-table { width: 100%; border-collapse: collapse; }
    .data-table th, .data-table td { padding: 10px 12px; text-align: left; border-bottom: 1px solid var(--brand-border); }
    .data-table th { font-size: 0.8rem; color: var(--brand-text-soft); text-transform: uppercase; }
    .active { background: var(--brand-success-soft) !important; color: var(--brand-success) !important; }
    .disabled { background: var(--brand-danger-soft) !important; color: var(--brand-danger) !important; }
    .tab-badge { background: var(--brand-danger); color: white; border-radius: 10px; padding: 1px 7px; font-size: 11px; margin-left: 6px; }
    .empty-hint { color: var(--brand-text-soft); font-style: italic; padding: 40px 0; text-align: center; }
    .report-list { display: flex; flex-direction: column; gap: 12px; }
    .report-header { display: flex; gap: 8px; margin-bottom: 8px; flex-wrap: wrap; }
    .status-pending { background: var(--brand-accent-soft) !important; color: var(--brand-warn) !important; }
    .status-reviewed { background: var(--brand-primary-100) !important; color: var(--brand-primary-dark) !important; }
    .status-dismissed { background: var(--brand-surface-2) !important; color: var(--brand-text-soft) !important; }
    .status-action_taken { background: var(--brand-danger-soft) !important; color: var(--brand-danger) !important; }
    .report-target { margin: 4px 0; }
    .report-reporter { color: var(--brand-text-soft); font-size: 0.85rem; margin: 4px 0; }
    .report-description { background: var(--brand-surface-2); border-radius: var(--radius-xs); padding: 10px 12px; margin: 8px 0; color: var(--brand-text); }
    .report-actions { display: flex; gap: 8px; margin-top: 12px; flex-wrap: wrap; }
    .report-resolution { color: var(--brand-text-soft); font-size: 0.85rem; margin: 8px 0 0; font-style: italic; }
  `],
})
export class AdminComponent implements OnInit {
  stats = signal<AdminStats | null>(null);
  users = signal<AdminUserSummary[]>([]);
  usersLoading = signal(false);
  usersTotal = signal(0);
  userSearch = '';
  orgs = signal<any[]>([]);
  orgsLoading = signal(false);
  orgsTotal = signal(0);
  orgStatus = 'PENDING';

  reports = signal<ReportResponse[]>([]);
  reportsLoading = signal(false);
  reportsTotal = signal(0);
  reportStatus: ReportStatus | '' = 'PENDING';
  reasonLabels = REPORT_REASON_LABELS;

  constructor(
    private adminService: AdminService,
    private reportService: ReportService,
    private snackBar: MatSnackBar,
  ) {}

  ngOnInit() {
    this.adminService.getStats().subscribe(res => this.stats.set(res.data));
    this.loadUsers();
    this.loadOrgs();
    this.loadReports();
  }

  loadUsers(page = 0) {
    this.usersLoading.set(true);
    this.adminService.listUsers(this.userSearch || '', undefined, page).subscribe({
      next: res => { this.users.set(res.data.content); this.usersTotal.set(res.data.totalElements); this.usersLoading.set(false); },
      error: () => this.usersLoading.set(false),
    });
  }

  loadOrgs(page = 0) {
    this.orgsLoading.set(true);
    this.adminService.listOrganizations(this.orgStatus || undefined, page).subscribe({
      next: res => { this.orgs.set(res.data.content); this.orgsTotal.set(res.data.totalElements); this.orgsLoading.set(false); },
      error: () => this.orgsLoading.set(false),
    });
  }

  toggleUser(user: AdminUserSummary) {
    this.adminService.changeUserStatus(user.id, !user.enabled).subscribe({
      next: res => {
        this.snackBar.open(res.data.enabled ? 'Utilisateur activé' : 'Utilisateur désactivé', 'OK', { duration: 3000 });
        this.loadUsers();
      },
    });
  }

  onUsersPage(e: PageEvent) { this.loadUsers(e.pageIndex); }
  onOrgsPage(e: PageEvent) { this.loadOrgs(e.pageIndex); }
  onReportsPage(e: PageEvent) { this.loadReports(e.pageIndex); }

  loadReports(page = 0) {
    this.reportsLoading.set(true);
    this.reportService.list(this.reportStatus || undefined, page).subscribe({
      next: res => { this.reports.set(res.data.content); this.reportsTotal.set(res.data.totalElements); this.reportsLoading.set(false); },
      error: () => this.reportsLoading.set(false),
    });
  }

  resolveReport(report: ReportResponse, status: 'REVIEWED' | 'DISMISSED' | 'ACTION_TAKEN') {
    this.reportService.resolve(report.id, { status }).subscribe({
      next: () => {
        this.snackBar.open('Signalement mis à jour', 'OK', { duration: 3000 });
        this.loadReports();
      },
      error: () => this.snackBar.open('Erreur lors de la mise à jour', 'OK', { duration: 3000 }),
    });
  }
}

