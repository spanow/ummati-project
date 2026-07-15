import { Component, signal, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTabsModule } from '@angular/material/tabs';
import { MatTableModule } from '@angular/material/table';
import { MatChipsModule } from '@angular/material/chips';
import { MatMenuModule } from '@angular/material/menu';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatDialogModule, MatDialog } from '@angular/material/dialog';
import { MatBadgeModule } from '@angular/material/badge';
import { DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { MembershipService, MembershipResponse } from '../../../core/services/membership.service';

@Component({
  selector: 'app-organization-manage',
  standalone: true,
  imports: [
    MatCardModule, MatButtonModule, MatIconModule, MatTabsModule, MatTableModule,
    MatChipsModule, MatMenuModule, MatProgressSpinnerModule, MatSnackBarModule,
    MatDialogModule, MatBadgeModule, DatePipe, RouterLink,
  ],
  template: `
    <div class="page-container">
      <header class="page-header">
        <div>
          <h1>Gestion de l'organisation</h1>
          <p class="subtitle">{{ orgSlug }}</p>
        </div>
        <div class="header-actions">
          <a mat-flat-button color="primary"
             [routerLink]="['/organizations', orgId, 'events', 'new']">
            <mat-icon>add</mat-icon> Créer un événement
          </a>
          <a mat-stroked-button [routerLink]="['/organizations', orgId, 'events', 'manage']">
            <mat-icon>event</mat-icon> Événements
          </a>
        </div>
      </header>

      <mat-tab-group>
        <!-- Pending tab -->
        <mat-tab>
          <ng-template matTabLabel>
            <mat-icon>pending</mat-icon>
            En attente
            @if (pendingMembers().length > 0) {
              <span class="badge">{{ pendingMembers().length }}</span>
            }
          </ng-template>

          @if (loading()) {
            <div class="loading"><mat-spinner diameter="36" /></div>
          } @else if (pendingMembers().length === 0) {
            <div class="empty-state">
              <mat-icon>check_circle</mat-icon>
              <p>Aucune demande en attente</p>
            </div>
          } @else {
            <div class="member-list">
              @for (m of pendingMembers(); track m.id) {
                <div class="member-row">
                  <div class="member-avatar">
                    @if (m.photoUrl) {
                      <img [src]="m.photoUrl" class="avatar-img" />
                    } @else {
                      <div class="avatar-placeholder">{{ m.firstName[0] }}{{ m.lastName[0] }}</div>
                    }
                  </div>
                  <div class="member-info">
                    <strong>{{ m.firstName }} {{ m.lastName }}</strong>
                    <span class="motivation">{{ m.motivation || 'Aucun message de motivation' }}</span>
                    <span class="date">Demande reçue le {{ m.createdAt | date:'dd/MM/yyyy' }}</span>
                  </div>
                  <div class="member-actions">
                    <button mat-flat-button color="primary" (click)="approve(m)">
                      <mat-icon>check</mat-icon> Accepter
                    </button>
                    <button mat-stroked-button (click)="reject(m)">
                      <mat-icon>close</mat-icon> Refuser
                    </button>
                  </div>
                </div>
              }
            </div>
          }
        </mat-tab>

        <!-- Active members tab -->
        <mat-tab>
          <ng-template matTabLabel>
            <mat-icon>group</mat-icon>
            Membres actifs ({{ activeMembers().length }})
          </ng-template>

          @if (loading()) {
            <div class="loading"><mat-spinner diameter="36" /></div>
          } @else if (activeMembers().length === 0) {
            <div class="empty-state">
              <mat-icon>group_off</mat-icon>
              <p>Aucun membre actif</p>
            </div>
          } @else {
            <div class="member-list">
              @for (m of activeMembers(); track m.id) {
                <div class="member-row">
                  <div class="member-avatar">
                    @if (m.photoUrl) {
                      <img [src]="m.photoUrl" class="avatar-img" />
                    } @else {
                      <div class="avatar-placeholder">{{ m.firstName[0] }}{{ m.lastName[0] }}</div>
                    }
                  </div>
                  <div class="member-info">
                    <strong>{{ m.firstName }} {{ m.lastName }}</strong>
                    <div class="role-row">
                      <mat-chip [class]="'role-' + m.role.toLowerCase()">{{ roleLabel(m.role) }}</mat-chip>
                      <span class="date">Membre depuis {{ m.joinedAt | date:'dd/MM/yyyy' }}</span>
                    </div>
                  </div>
                  <button mat-icon-button [matMenuTriggerFor]="memberMenu">
                    <mat-icon>more_vert</mat-icon>
                  </button>
                  <mat-menu #memberMenu="matMenu">
                    @if (m.role !== 'ADMIN') {
                      <button mat-menu-item (click)="changeRole(m, 'ADMIN')">
                        <mat-icon>admin_panel_settings</mat-icon> Promouvoir admin
                      </button>
                    }
                    @if (m.role === 'ADMIN') {
                      <button mat-menu-item (click)="changeRole(m, 'MEMBER')">
                        <mat-icon>person</mat-icon> Rétrograder membre
                      </button>
                    }
                    <button mat-menu-item class="danger-item" (click)="exclude(m)">
                      <mat-icon>person_remove</mat-icon> Exclure
                    </button>
                  </mat-menu>
                </div>
              }
            </div>
          }
        </mat-tab>
      </mat-tab-group>
    </div>
  `,
  styles: [`
    .page-container { max-width: 900px; margin: 0 auto; padding: 32px 24px; }
    .page-header { margin-bottom: 24px; display: flex; justify-content: space-between; align-items: center; }
    .page-header h1 { font-size: 1.8rem; font-weight: 600; margin: 0; }
    .header-actions { display: flex; gap: 8px; }
    .subtitle { color: #666; margin-top: 4px; }
    .loading { display: flex; justify-content: center; padding: 48px; }
    .empty-state { text-align: center; padding: 48px 24px; color: #999; }
    .empty-state mat-icon { font-size: 48px; width: 48px; height: 48px; display: block; margin: 0 auto 12px; }
    .member-list { padding: 16px 0; display: flex; flex-direction: column; gap: 12px; }
    .member-row { display: flex; align-items: center; gap: 16px; padding: 16px 20px;
      background: #fafafa; border-radius: 10px; border: 1px solid #f0f0f0;
      transition: box-shadow 0.2s; }
    .member-row:hover { box-shadow: 0 2px 12px rgba(0,0,0,0.06); }
    .member-avatar { flex-shrink: 0; }
    .avatar-img { width: 44px; height: 44px; border-radius: 50%; object-fit: cover; }
    .avatar-placeholder { width: 44px; height: 44px; border-radius: 50%;
      background: #e3f2fd; color: #1976d2; display: flex; align-items: center;
      justify-content: center; font-weight: 600; font-size: 0.9rem; }
    .member-info { flex: 1; display: flex; flex-direction: column; gap: 4px; }
    .member-info strong { font-size: 0.95rem; }
    .motivation { font-size: 0.85rem; color: #666; font-style: italic; }
    .date { font-size: 0.8rem; color: #aaa; }
    .role-row { display: flex; align-items: center; gap: 12px; }
    .member-actions { display: flex; gap: 8px; flex-shrink: 0; }
    .badge { background: #e53935; color: white; border-radius: 10px; padding: 1px 7px;
      font-size: 11px; margin-left: 6px; }
    .role-admin { --mdc-chip-label-text-color: #1565c0; background: #e3f2fd; }
    .role-member { --mdc-chip-label-text-color: #2e7d32; background: #e8f5e9; }
    .role-accountant { --mdc-chip-label-text-color: #f57f17; background: #fff8e1; }
    .danger-item { color: #d32f2f; }
  `],
})
export class OrganizationManageComponent implements OnInit {
  orgSlug = '';
  orgId = '';
  pendingMembers = signal<MembershipResponse[]>([]);
  activeMembers = signal<MembershipResponse[]>([]);
  loading = signal(true);

  constructor(
    private route: ActivatedRoute,
    private membershipService: MembershipService,
    private snackBar: MatSnackBar,
  ) {}

  ngOnInit() {
    this.orgSlug = this.route.snapshot.paramMap.get('slug') ?? '';
    this.orgId = this.route.snapshot.queryParamMap.get('orgId') ?? '';
    if (this.orgId) this.loadMembers();
  }

  loadMembers() {
    this.loading.set(true);
    this.membershipService.listMembers(this.orgId, undefined, 0, 100).subscribe({
      next: res => {
        const all = res.data.content;
        this.pendingMembers.set(all.filter(m => m.status === 'PENDING'));
        this.activeMembers.set(all.filter(m => m.status === 'ACTIVE'));
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  approve(m: MembershipResponse) {
    this.membershipService.approve(m.id).subscribe({
      next: () => { this.snackBar.open(`${m.firstName} accepté(e) !`, '', { duration: 3000 }); this.loadMembers(); },
      error: err => this.snackBar.open(err.error?.message ?? 'Erreur', '', { duration: 4000 }),
    });
  }

  reject(m: MembershipResponse) {
    this.membershipService.reject(m.id).subscribe({
      next: () => { this.snackBar.open(`Demande refusée.`, '', { duration: 3000 }); this.loadMembers(); },
      error: err => this.snackBar.open(err.error?.message ?? 'Erreur', '', { duration: 4000 }),
    });
  }

  changeRole(m: MembershipResponse, role: string) {
    this.membershipService.changeRole(m.id, role).subscribe({
      next: () => { this.snackBar.open('Rôle mis à jour.', '', { duration: 3000 }); this.loadMembers(); },
      error: err => this.snackBar.open(err.error?.message ?? 'Erreur', '', { duration: 4000 }),
    });
  }

  exclude(m: MembershipResponse) {
    if (!confirm(`Exclure ${m.firstName} ${m.lastName} ?`)) return;
    this.membershipService.remove(m.id).subscribe({
      next: () => { this.snackBar.open('Membre exclu.', '', { duration: 3000 }); this.loadMembers(); },
      error: err => this.snackBar.open(err.error?.message ?? 'Erreur', '', { duration: 4000 }),
    });
  }

  roleLabel(role: string): string {
    return { ADMIN: 'Admin', MEMBER: 'Membre', ACCOUNTANT: 'Comptable' }[role] ?? role;
  }
}

