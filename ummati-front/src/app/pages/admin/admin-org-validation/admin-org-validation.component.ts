import { Component, inject, OnInit, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDividerModule } from '@angular/material/divider';
import { MatChipsModule } from '@angular/material/chips';
import { MatDialogModule, MatDialog } from '@angular/material/dialog';
import { OrganizationService, OrganizationDetail } from '../../../core/services/organization.service';
import { AdminService } from '../../../core/services/admin.service';
import { OrgDocumentsComponent } from '../../organizations/org-documents/org-documents.component';
import { TPipe } from '../../../shared/pipes/t.pipe';

@Component({
  selector: 'app-admin-org-validation',
  standalone: true,
  imports: [
    DatePipe, RouterLink, ReactiveFormsModule,
    MatCardModule, MatButtonModule, MatIconModule, MatInputModule,
    MatFormFieldModule, MatSnackBarModule, MatProgressSpinnerModule,
    MatDividerModule, MatChipsModule, MatDialogModule,
    OrgDocumentsComponent, TPipe
  ],
  template: `
    <div class="validation-container" role="main">

      <!-- Back button -->
      <a mat-button routerLink="/admin" class="back-btn" [attr.aria-label]="'Retour à l\\'administration' | t">
        <mat-icon>arrow_back</mat-icon> {{ 'Retour' | t }}
      </a>

      @if (loading()) {
        <div class="loading-center" aria-live="polite" aria-busy="true">
          <mat-spinner aria-label="Chargement de l'organisation..."></mat-spinner>
        </div>
      } @else if (org()) {
        <div class="page-header">
          <div class="org-info">
            @if (org()!.logoUrl) {
              <img [src]="org()!.logoUrl" [alt]="org()!.name + ' logo'" class="org-logo">
            } @else {
              <div class="org-logo-placeholder" aria-hidden="true">
                <mat-icon>business</mat-icon>
              </div>
            }
            <div>
              <h1>{{ org()!.name }}</h1>
              <div class="meta">
                <span class="status-badge" [class]="'status-' + org()!.status.toLowerCase()">
                  {{ statusLabel(org()!.status) | t }}
                </span>
                <span>{{ org()!.domain }}</span>
                @if (org()!.addressCity) { <span>📍 {{ org()!.addressCity }}</span> }
              </div>
            </div>
          </div>
        </div>

        <div class="content-grid">
          <!-- Left: Org details -->
          <div class="left-col">
            <!-- Description -->
            <mat-card class="detail-card">
              <mat-card-header>
                <mat-icon mat-card-avatar aria-hidden="true">info</mat-icon>
                <mat-card-title>{{ 'Informations' | t }}</mat-card-title>
              </mat-card-header>
              <mat-card-content>
                <div class="info-grid">
                  @if (org()!.email) {
                    <div class="info-item">
                      <mat-icon aria-hidden="true">email</mat-icon>
                      <a [href]="'mailto:' + org()!.email">{{ org()!.email }}</a>
                    </div>
                  }
                  @if (org()!.phone) {
                    <div class="info-item">
                      <mat-icon aria-hidden="true">phone</mat-icon>
                      <span>{{ org()!.phone }}</span>
                    </div>
                  }
                  @if (org()!.website) {
                    <div class="info-item">
                      <mat-icon aria-hidden="true">language</mat-icon>
                      <a [href]="org()!.website" target="_blank" rel="noopener">{{ org()!.website }}</a>
                    </div>
                  }
                  <div class="info-item">
                    <mat-icon aria-hidden="true">calendar_today</mat-icon>
                    <span>{{ 'Créée le' | t }} {{ org()!.createdAt | date:'dd/MM/yyyy' }}</span>
                  </div>
                </div>

                @if (org()!.description) {
                  <mat-divider class="my-divider"></mat-divider>
                  <h3>{{ 'Description' | t }}</h3>
                  <p class="description-text">{{ org()!.description }}</p>
                }
                @if (org()!.mission) {
                  <h3>{{ 'Mission' | t }}</h3>
                  <p class="description-text">{{ org()!.mission }}</p>
                }
              </mat-card-content>
            </mat-card>

            <!-- Documents -->
            <app-org-documents [orgId]="org()!.id" [canUpload]="false"></app-org-documents>
          </div>

          <!-- Right: Validation actions -->
          @if (org()!.status === 'PENDING') {
            <div class="right-col">
              <mat-card class="action-card approve-card">
                <mat-card-header>
                  <mat-icon mat-card-avatar style="color: var(--brand-success)" aria-hidden="true">check_circle</mat-icon>
                  <mat-card-title>{{ 'Approuver l\\'organisation' | t }}</mat-card-title>
                  <mat-card-subtitle>{{ 'L\\'organisation sera publiée et visible de tous' | t }}</mat-card-subtitle>
                </mat-card-header>
                <mat-card-content>
                  <button mat-flat-button color="primary" (click)="approve()"
                          [disabled]="actionLoading()"
                          class="full-width-btn"
                          [attr.aria-label]="'Approuver cette organisation' | t">
                    @if (actionLoading()) { <mat-spinner diameter="20"></mat-spinner> }
                    @else { <ng-container><mat-icon>check</mat-icon> {{ 'Approuver' | t }}</ng-container> }
                  </button>
                </mat-card-content>
              </mat-card>

              <mat-card class="action-card reject-card">
                <mat-card-header>
                  <mat-icon mat-card-avatar color="warn" aria-hidden="true">cancel</mat-icon>
                  <mat-card-title>{{ 'Rejeter l\\'organisation' | t }}</mat-card-title>
                  <mat-card-subtitle>{{ 'Un motif d\\'au moins 20 caractères est requis' | t }}</mat-card-subtitle>
                </mat-card-header>
                <mat-card-content>
                  <form [formGroup]="rejectForm" (ngSubmit)="reject()" aria-label="Formulaire de rejet">
                    <mat-form-field appearance="outline" class="full-width">
                      <mat-label>{{ 'Motif du rejet' | t }}</mat-label>
                      <textarea matInput formControlName="reason" rows="4"
                                [placeholder]="'Expliquez pourquoi cette organisation est rejetée...' | t"
                                aria-required="true"></textarea>
                      @if (rejectForm.get('reason')?.hasError('minlength') && rejectForm.get('reason')?.touched) {
                        <mat-error role="alert">{{ 'Le motif doit contenir au moins 20 caractères' | t }}</mat-error>
                      }
                      <mat-hint>{{ rejectForm.get('reason')?.value?.length ?? 0 }} / 20 min</mat-hint>
                    </mat-form-field>
                    <button mat-flat-button color="warn" type="submit"
                            [disabled]="rejectForm.invalid || actionLoading()"
                            class="full-width-btn"
                            [attr.aria-label]="'Rejeter cette organisation' | t">
                      @if (actionLoading()) { <mat-spinner diameter="20"></mat-spinner> }
                      @else { <ng-container><mat-icon>close</mat-icon> {{ 'Rejeter' | t }}</ng-container> }
                    </button>
                  </form>
                </mat-card-content>
              </mat-card>
            </div>
          } @else {
            <div class="right-col">
              <mat-card class="action-card">
                <mat-card-content>
                  <div class="status-info">
                    <mat-icon [class]="'status-icon-' + org()!.status.toLowerCase()" aria-hidden="true">
                      {{ org()!.status === 'ACTIVE' ? 'check_circle' : org()!.status === 'REJECTED' ? 'cancel' : 'block' }}
                    </mat-icon>
                    <p>{{ 'Cette organisation est' | t }} <strong>{{ statusLabel(org()!.status) | t }}</strong>.</p>
                    @if (org()!.rejectionReason) {
                      <div class="rejection-reason" role="note">
                        <strong>{{ 'Motif :' | t }}</strong> {{ org()!.rejectionReason }}
                      </div>
                    }
                  </div>
                </mat-card-content>
              </mat-card>
            </div>
          }
        </div>
      } @else {
        <div class="error-state" role="alert">
          <mat-icon>error_outline</mat-icon>
          <p>{{ 'Organisation introuvable.' | t }}</p>
          <a mat-button routerLink="/admin">{{ 'Retour à l\\'administration' | t }}</a>
        </div>
      }
    </div>
  `,
  styles: [`
    .validation-container { max-width: 1100px; margin: 0 auto; padding: 24px 20px; }
    .back-btn { margin-bottom: 16px; }
    .loading-center { display: flex; justify-content: center; padding: 80px; }
    .page-header { margin-bottom: 32px; }
    .org-info { display: flex; align-items: center; gap: 20px; }
    .org-logo { width: 72px; height: 72px; border-radius: 12px; object-fit: cover; }
    .org-logo-placeholder {
      width: 72px; height: 72px; border-radius: 12px;
      background: var(--brand-primary-100); display: flex; align-items: center; justify-content: center;
    }
    .org-logo-placeholder mat-icon { font-size: 36px !important; width: 36px !important; height: 36px !important; color: var(--brand-primary); }
    h1 { font-size: 1.8rem; font-weight: 700; margin: 0 0 8px; color: var(--brand-ink); }
    .meta { display: flex; gap: 12px; align-items: center; flex-wrap: wrap; font-size: 0.9rem; color: var(--brand-text-soft); }
    .status-badge {
      padding: 4px 12px; border-radius: 20px; font-size: 0.8rem; font-weight: 600;
    }
    .status-pending { background: var(--brand-accent-soft); color: var(--brand-warn); }
    .status-active { background: var(--brand-success-soft); color: var(--brand-success); }
    .status-rejected { background: var(--brand-danger-soft); color: var(--brand-danger); }
    .status-suspended { background: #fce4ec; color: #880e4f; }
    .content-grid { display: grid; grid-template-columns: 1fr 340px; gap: 24px; }
    .left-col, .right-col { display: flex; flex-direction: column; gap: 24px; }
    .approve-card { border-left: 4px solid var(--brand-success); }
    .reject-card { border-left: 4px solid var(--brand-danger); }
    .info-grid { display: flex; flex-direction: column; gap: 12px; }
    .info-item { display: flex; align-items: center; gap: 10px; }
    .info-item mat-icon { color: var(--brand-text-soft); font-size: 18px !important; width: 18px !important; height: 18px !important; }
    .info-item a { color: var(--brand-primary); text-decoration: none; }
    .my-divider { margin: 20px 0 !important; }
    h3 { font-size: 1rem; font-weight: 700; color: var(--brand-ink); margin: 0 0 8px; }
    .description-text { color: var(--brand-text); line-height: 1.7; margin: 0 0 16px; white-space: pre-wrap; }
    .full-width { width: 100%; display: block; }
    .full-width-btn { width: 100%; margin-top: 8px; }
    .status-info { display: flex; flex-direction: column; align-items: center; gap: 12px; padding: 24px; text-align: center; }
    .status-icon-active { color: var(--brand-success); font-size: 48px !important; width: 48px !important; height: 48px !important; }
    .status-icon-rejected { color: var(--brand-danger); font-size: 48px !important; width: 48px !important; height: 48px !important; }
    .status-icon-suspended { color: var(--brand-accent); font-size: 48px !important; width: 48px !important; height: 48px !important; }
    .rejection-reason {
      background: var(--brand-danger-soft); border-radius: var(--radius-sm); padding: 12px 16px; font-size: 0.9rem; color: var(--brand-text); text-align: left;
    }
    mat-spinner { display: inline-block; }
    .error-state { text-align: center; padding: 80px; }
    .error-state mat-icon { font-size: 48px !important; width: 48px !important; height: 48px !important; color: var(--brand-danger); }
    @media (max-width: 768px) { .content-grid { grid-template-columns: 1fr; } }
  `]
})
export class AdminOrgValidationComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private orgService = inject(OrganizationService);
  private snackBar = inject(MatSnackBar);
  private fb = inject(FormBuilder);

  org = signal<OrganizationDetail | null>(null);
  loading = signal(true);
  actionLoading = signal(false);

  rejectForm = this.fb.group({
    reason: ['', [Validators.required, Validators.minLength(20)]]
  });

  ngOnInit() {
    const slug = this.route.snapshot.paramMap.get('slug') ?? '';
    this.orgService.getBySlug(slug).subscribe({
      next: (res) => { this.org.set(res.data); this.loading.set(false); },
      error: () => this.loading.set(false)
    });
  }

  approve() {
    if (!this.org()) return;
    this.actionLoading.set(true);
    this.orgService.changeStatus(this.org()!.id, 'ACTIVE', '').subscribe({
      next: () => {
        this.snackBar.open('Organisation approuvée avec succès !', 'OK', { duration: 4000 });
        this.org.update(o => o ? { ...o, status: 'ACTIVE' } : o);
        this.actionLoading.set(false);
      },
      error: (err) => {
        this.snackBar.open(err.error?.message ?? 'Erreur', 'OK', { duration: 5000 });
        this.actionLoading.set(false);
      }
    });
  }

  reject() {
    if (this.rejectForm.invalid || !this.org()) return;
    this.actionLoading.set(true);
    const reason = this.rejectForm.value.reason!;
    this.orgService.changeStatus(this.org()!.id, 'REJECTED', reason).subscribe({
      next: () => {
        this.snackBar.open('Organisation rejetée.', 'OK', { duration: 4000 });
        this.org.update(o => o ? { ...o, status: 'REJECTED', rejectionReason: reason } : o);
        this.actionLoading.set(false);
      },
      error: (err) => {
        this.snackBar.open(err.error?.message ?? 'Erreur', 'OK', { duration: 5000 });
        this.actionLoading.set(false);
      }
    });
  }

  statusLabel(status: string): string {
    const labels: Record<string, string> = {
      PENDING: 'En attente', ACTIVE: 'Active', REJECTED: 'Rejetée',
      SUSPENDED: 'Suspendue', ARCHIVED: 'Archivée'
    };
    return labels[status] ?? status;
  }
}


