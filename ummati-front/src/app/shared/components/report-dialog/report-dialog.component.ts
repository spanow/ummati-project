import { Component, Inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { ReportService, ReportReason, ReportTargetType, REPORT_REASON_LABELS } from '../../../core/services/report.service';

export interface ReportDialogData {
  targetType: ReportTargetType;
  targetId: string;
  targetLabel: string;
}

@Component({
  selector: 'app-report-dialog',
  standalone: true,
  imports: [FormsModule, MatDialogModule, MatButtonModule, MatFormFieldModule,
    MatSelectModule, MatInputModule, MatProgressSpinnerModule, MatSnackBarModule],
  template: `
    <h2 mat-dialog-title>Signaler « {{ data.targetLabel }} »</h2>
    <mat-dialog-content>
      <p class="hint">Votre signalement sera examiné par un administrateur de la plateforme.</p>

      <mat-form-field appearance="outline" class="full-width">
        <mat-label>Motif</mat-label>
        <mat-select [(ngModel)]="reason">
          @for (r of reasons; track r) {
            <mat-option [value]="r">{{ reasonLabels[r] }}</mat-option>
          }
        </mat-select>
      </mat-form-field>

      <mat-form-field appearance="outline" class="full-width">
        <mat-label>Description (optionnel)</mat-label>
        <textarea matInput rows="3" [(ngModel)]="description" maxlength="1000"
                  placeholder="Précisez le contexte du signalement..."></textarea>
      </mat-form-field>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button [mat-dialog-close]="false" [disabled]="submitting()">Annuler</button>
      <button mat-flat-button color="warn" [disabled]="submitting()" (click)="submit()">
        {{ submitting() ? 'Envoi…' : 'Signaler' }}
      </button>
    </mat-dialog-actions>
  `,
  styles: [`
    .full-width { width: 100%; margin-bottom: 8px; }
    .hint { color: #666; font-size: 0.9rem; margin: 0 0 16px; }
    mat-dialog-content { min-width: 380px; }
  `],
})
export class ReportDialogComponent {
  reasons: ReportReason[] = ['SPAM', 'INAPPROPRIATE_CONTENT', 'FRAUD', 'HARASSMENT', 'OTHER'];
  reasonLabels = REPORT_REASON_LABELS;
  reason: ReportReason = 'SPAM';
  description = '';
  submitting = signal(false);

  constructor(
    private dialogRef: MatDialogRef<ReportDialogComponent>,
    private reportService: ReportService,
    private snackBar: MatSnackBar,
    @Inject(MAT_DIALOG_DATA) public data: ReportDialogData,
  ) {}

  submit() {
    this.submitting.set(true);
    this.reportService.create({
      targetType: this.data.targetType,
      targetId: this.data.targetId,
      reason: this.reason,
      description: this.description.trim() || undefined,
    }).subscribe({
      next: () => {
        this.submitting.set(false);
        this.snackBar.open('Signalement envoyé, merci pour votre vigilance.', 'Fermer', { duration: 4000 });
        this.dialogRef.close(true);
      },
      error: (err) => {
        this.submitting.set(false);
        const msg = err?.error?.message ?? 'Une erreur est survenue. Veuillez réessayer.';
        this.snackBar.open(msg, 'Fermer', { duration: 4000 });
      },
    });
  }
}
