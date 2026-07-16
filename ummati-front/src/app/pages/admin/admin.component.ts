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

@Component({
  selector: 'app-admin',
  standalone: true,
  imports: [MatCardModule, MatButtonModule, MatIconModule, MatTabsModule, MatFormFieldModule,
    MatInputModule, MatSelectModule, MatChipsModule, MatPaginatorModule, MatProgressSpinnerModule,
    MatSnackBarModule, FormsModule, DatePipe, RouterLink],
  template: `
    <div class="page-container">
      <h1>Administration</h1>

      @if (stats()) {
        <div class="stats-row">
          <mat-card class="stat">
            <mat-card-content><div class="val">{{ stats()!.totalUsers }}</div><div class="lbl">Utilisateurs</div></mat-card-content>
          </mat-card>
          <mat-card class="stat">
            <mat-card-content><div class="val">{{ stats()!.totalOrganizations }}</div><div class="lbl">Organisations</div></mat-card-content>
          </mat-card>
          <mat-card class="stat">
            <mat-card-content><div class="val">{{ stats()!.totalEvents }}</div><div class="lbl">Événements</div></mat-card-content>
          </mat-card>
          <mat-card class="stat highlight">
            <mat-card-content><div class="val">{{ stats()!.pendingOrganizations }}</div><div class="lbl">ONG en attente</div></mat-card-content>
          </mat-card>
          <mat-card class="stat">
            <mat-card-content><div class="val">{{ stats()!.registrationsThisWeek }}</div><div class="lbl">Inscriptions (7j)</div></mat-card-content>
          </mat-card>
        </div>
      }

      <mat-tab-group>
        <mat-tab label="Utilisateurs">
          <div class="tab-content">
            <div class="filters">
              <mat-form-field appearance="outline" class="search-field">
                <mat-label>Rechercher</mat-label>
                <input matInput [(ngModel)]="userSearch" (keyup.enter)="loadUsers()" placeholder="Nom, email..." />
              </mat-form-field>
            </div>
            @if (usersLoading()) {
              <div class="loading"><mat-spinner diameter="30" /></div>
            } @else {
              <table class="data-table">
                <thead><tr><th>Nom</th><th>Email</th><th>Rôle</th><th>Statut</th><th>Inscrit le</th><th>Actions</th></tr></thead>
                <tbody>
                  @for (u of users(); track u.id) {
                    <tr>
                      <td>{{ u.firstName }} {{ u.lastName }}</td>
                      <td>{{ u.email }}</td>
                      <td><mat-chip>{{ u.role }}</mat-chip></td>
                      <td>
                        <mat-chip [class]="u.enabled ? 'active' : 'disabled'">{{ u.enabled ? 'Actif' : 'Désactivé' }}</mat-chip>
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

        <mat-tab label="Organisations">
          <div class="tab-content">
            <div class="filters">
              <mat-form-field appearance="outline">
                <mat-label>Statut</mat-label>
                <mat-select [(ngModel)]="orgStatus" (selectionChange)="loadOrgs()">
                  <mat-option value="">Tous</mat-option>
                  <mat-option value="PENDING">En attente</mat-option>
                  <mat-option value="ACTIVE">Actives</mat-option>
                  <mat-option value="REJECTED">Rejetées</mat-option>
                  <mat-option value="SUSPENDED">Suspendues</mat-option>
                </mat-select>
              </mat-form-field>
            </div>
            @if (orgsLoading()) {
              <div class="loading"><mat-spinner diameter="30" /></div>
            } @else {
              <table class="data-table">
                <thead><tr><th>Nom</th><th>Domaine</th><th>Ville</th><th>Statut</th><th>Membres</th><th>Actions</th></tr></thead>
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
                           [title]="'Voir/valider ' + o.name">
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
            Signalements
            @if (reportsTotal() > 0 && reportStatus === 'PENDING') {
              <span class="tab-badge">{{ reportsTotal() }}</span>
            }
          </ng-template>
          <div class="tab-content">
            <div class="filters">
              <mat-form-field appearance="outline">
                <mat-label>Statut</mat-label>
                <mat-select [(ngModel)]="reportStatus" (selectionChange)="loadReports()">
                  <mat-option value="">Tous</mat-option>
                  <mat-option value="PENDING">En attente</mat-option>
                  <mat-option value="REVIEWED">Examinés</mat-option>
                  <mat-option value="DISMISSED">Rejetés</mat-option>
                  <mat-option value="ACTION_TAKEN">Action prise</mat-option>
                </mat-select>
              </mat-form-field>
            </div>
            @if (reportsLoading()) {
              <div class="loading"><mat-spinner diameter="30" /></div>
            } @else if (reports().length === 0) {
              <p class="empty-hint">Aucun signalement.</p>
            } @else {
              <div class="report-list">
                @for (r of reports(); track r.id) {
                  <mat-card class="report-card">
                    <mat-card-content>
                      <div class="report-header">
                        <mat-chip>{{ r.targetType }}</mat-chip>
                        <mat-chip>{{ reasonLabels[r.reason] }}</mat-chip>
                        <mat-chip [class]="'status-' + r.status.toLowerCase()">{{ r.status }}</mat-chip>
                      </div>
                      <p class="report-target"><strong>Cible :</strong> {{ r.targetLabel }}</p>
                      <p class="report-reporter">Signalé par {{ r.reporterName }} le {{ r.createdAt | date:'d MMM yyyy, HH:mm' }}</p>
                      @if (r.description) {
                        <p class="report-description">{{ r.description }}</p>
                      }
                      @if (r.status === 'PENDING') {
                        <div class="report-actions">
                          <button mat-stroked-button (click)="resolveReport(r, 'DISMISSED')">Rejeter</button>
                          <button mat-flat-button color="warn" (click)="resolveReport(r, 'ACTION_TAKEN')">Action prise</button>
                          <button mat-button (click)="resolveReport(r, 'REVIEWED')">Marquer examiné</button>
                        </div>
                      } @else {
                        <p class="report-resolution">
                          Traité par {{ r.reviewedByName }} le {{ r.reviewedAt | date:'d MMM yyyy, HH:mm' }}
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
    .page-container { max-width: 1200px; margin: 0 auto; padding: 32px 24px; }
    h1 { font-size: 2rem; font-weight: 600; margin-bottom: 24px; }
    .stats-row { display: flex; gap: 12px; margin-bottom: 32px; flex-wrap: wrap; }
    .stat { flex: 1; min-width: 140px; border-radius: 10px; text-align: center; }
    .stat .val { font-size: 1.8rem; font-weight: 700; }
    .stat .lbl { color: #888; font-size: 0.8rem; }
    .stat.highlight { border: 2px solid #ff9800; }
    .tab-content { padding: 16px 0; }
    .filters { margin-bottom: 16px; }
    .search-field { width: 300px; }
    .loading { display: flex; justify-content: center; padding: 40px; }
    .data-table { width: 100%; border-collapse: collapse; }
    .data-table th, .data-table td { padding: 10px 12px; text-align: left; border-bottom: 1px solid #eee; }
    .data-table th { font-size: 0.8rem; color: #888; text-transform: uppercase; }
    .active { background: #e8f5e9 !important; color: #2e7d32 !important; }
    .disabled { background: #ffebee !important; color: #c62828 !important; }
    .tab-badge { background: #f44336; color: white; border-radius: 10px; padding: 1px 7px; font-size: 11px; margin-left: 6px; }
    .empty-hint { color: #888; font-style: italic; padding: 40px 0; text-align: center; }
    .report-list { display: flex; flex-direction: column; gap: 12px; }
    .report-card { border-radius: 10px; }
    .report-header { display: flex; gap: 8px; margin-bottom: 8px; }
    .status-pending { background: #fff3e0 !important; color: #e65100 !important; }
    .status-reviewed { background: #e3f2fd !important; color: #1565c0 !important; }
    .status-dismissed { background: #f5f5f5 !important; color: #757575 !important; }
    .status-action_taken { background: #ffebee !important; color: #c62828 !important; }
    .report-target { margin: 4px 0; }
    .report-reporter { color: #888; font-size: 0.85rem; margin: 4px 0; }
    .report-description { background: #fafafa; border-radius: 6px; padding: 10px 12px; margin: 8px 0; color: #444; }
    .report-actions { display: flex; gap: 8px; margin-top: 12px; }
    .report-resolution { color: #666; font-size: 0.85rem; margin: 8px 0 0; font-style: italic; }
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

