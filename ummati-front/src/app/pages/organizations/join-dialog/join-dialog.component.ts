import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatRadioModule } from '@angular/material/radio';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MembershipService, MembershipQuestion } from '../../../core/services/membership.service';
import { TPipe } from '../../../shared/pipes/t.pipe';

@Component({
  selector: 'app-join-dialog',
  standalone: true,
  imports: [FormsModule, MatDialogModule, MatButtonModule, MatFormFieldModule, MatInputModule,
    MatRadioModule, MatProgressSpinnerModule, TPipe],
  template: `
    <h2 mat-dialog-title>{{ 'Rejoindre' | t }} {{ data.orgName }}</h2>
    <mat-dialog-content>
      @if (loading()) {
        <div class="loading"><mat-spinner diameter="28" /></div>
      } @else {
        <mat-form-field appearance="outline" class="full">
          <mat-label>{{ 'Message / motivation (optionnel)' | t }}</mat-label>
          <textarea matInput [(ngModel)]="motivation" rows="3" maxlength="1000"></textarea>
        </mat-form-field>

        @for (q of questions(); track q.id) {
          <div class="q">
            <label class="q-label">{{ q.label }}@if (q.required) { <span class="req">*</span> }</label>
            @switch (q.type) {
              @case ('TEXT') {
                <mat-form-field appearance="outline" class="full">
                  <textarea matInput [(ngModel)]="answers[q.id]" rows="2" maxlength="1000"></textarea>
                </mat-form-field>
              }
              @case ('BOOLEAN') {
                <mat-radio-group [(ngModel)]="answers[q.id]" class="radio-row">
                  <mat-radio-button value="Oui">{{ 'Oui' | t }}</mat-radio-button>
                  <mat-radio-button value="Non">{{ 'Non' | t }}</mat-radio-button>
                </mat-radio-group>
              }
              @case ('SINGLE_CHOICE') {
                <mat-radio-group [(ngModel)]="answers[q.id]" class="radio-col">
                  @for (opt of q.options; track opt) {
                    <mat-radio-button [value]="opt">{{ opt }}</mat-radio-button>
                  }
                </mat-radio-group>
              }
            }
          </div>
        }

        @if (error()) { <p class="err">{{ error() }}</p> }
      }
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button mat-dialog-close>{{ 'Annuler' | t }}</button>
      <button mat-flat-button color="primary" (click)="submit()" [disabled]="submitting() || loading()">
        {{ (submitting() ? 'Envoi…' : 'Envoyer ma demande') | t }}
      </button>
    </mat-dialog-actions>
  `,
  styles: [`
    .loading { display: flex; justify-content: center; padding: 24px; }
    .full { width: 100%; }
    .q { margin-bottom: 12px; }
    .q-label { display: block; font-weight: 500; font-size: 0.9rem; margin-bottom: 6px; }
    .req { color: #d32f2f; }
    .radio-row { display: flex; gap: 20px; }
    .radio-col { display: flex; flex-direction: column; gap: 4px; }
    .err { color: #d32f2f; font-size: 0.9rem; margin: 4px 0 0; }
    mat-dialog-content { min-width: 380px; max-width: 520px; }
  `],
})
export class JoinDialogComponent {
  data = inject(MAT_DIALOG_DATA) as { orgId: string; orgName: string };
  private membershipService = inject(MembershipService);
  private dialogRef = inject(MatDialogRef<JoinDialogComponent>);

  loading = signal(true);
  submitting = signal(false);
  error = signal('');
  questions = signal<MembershipQuestion[]>([]);
  motivation = '';
  answers: Record<string, string> = {};

  constructor() {
    this.membershipService.listQuestions(this.data.orgId).subscribe({
      next: res => { this.questions.set(res.data ?? []); this.loading.set(false); },
      error: () => this.loading.set(false),
    });
  }

  submit() {
    for (const q of this.questions()) {
      if (q.required && !(this.answers[q.id] ?? '').trim()) {
        this.error.set('Merci de répondre à : ' + q.label);
        return;
      }
    }
    const answers = Object.entries(this.answers)
      .filter(([, v]) => v != null && String(v).trim().length > 0)
      .map(([questionId, value]) => ({ questionId, value: String(value).trim() }));

    this.submitting.set(true);
    this.error.set('');
    this.membershipService.requestMembership(this.data.orgId, this.motivation.trim() || undefined, answers).subscribe({
      next: res => { this.submitting.set(false); this.dialogRef.close(res.data); },
      error: err => { this.submitting.set(false); this.error.set(err?.error?.message ?? 'Une erreur est survenue.'); },
    });
  }
}
