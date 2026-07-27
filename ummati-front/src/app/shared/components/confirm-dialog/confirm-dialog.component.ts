import { Component, Inject } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { TPipe } from '../../pipes/t.pipe';

/** Résultat rendu par le dialogue. `undefined` si l'utilisateur ferme sans choisir. */
export type ConfirmDialogResult = 'confirm' | 'secondary' | undefined;

export interface ConfirmDialogData {
  title: string;
  message: string;
  /** Action principale (ex. « Publier quand même »). */
  confirmLabel: string;
  /** Action alternative mise en avant (ex. « Ajouter une image »). Optionnelle. */
  secondaryLabel?: string;
  cancelLabel?: string;
  icon?: string;
}

/**
 * Dialogue de confirmation générique, avec une action alternative facultative.
 *
 * Pensé pour les rappels non bloquants : l'action principale reste toujours
 * accessible, la suggestion est simplement mise en avant.
 */
@Component({
  selector: 'app-confirm-dialog',
  standalone: true,
  imports: [MatDialogModule, MatButtonModule, MatIconModule, TPipe],
  template: `
    <h2 mat-dialog-title>
      @if (data.icon) { <mat-icon class="title-icon">{{ data.icon }}</mat-icon> }
      {{ data.title | t }}
    </h2>
    <mat-dialog-content>
      <p class="message">{{ data.message | t }}</p>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button [mat-dialog-close]="undefined">
        {{ (data.cancelLabel || 'Annuler') | t }}
      </button>
      <button mat-button [mat-dialog-close]="'confirm'">{{ data.confirmLabel | t }}</button>
      @if (data.secondaryLabel) {
        <button mat-flat-button [mat-dialog-close]="'secondary'">{{ data.secondaryLabel | t }}</button>
      }
    </mat-dialog-actions>
  `,
  styles: [`
    h2 { display: flex; align-items: center; gap: 10px; }
    .title-icon { color: var(--brand-primary); }
    .message { color: var(--brand-text-soft); line-height: 1.6; margin: 0; }
    mat-dialog-content { max-width: 460px; }
    mat-dialog-actions { flex-wrap: wrap; gap: 8px; }
  `],
})
export class ConfirmDialogComponent {
  constructor(@Inject(MAT_DIALOG_DATA) public data: ConfirmDialogData) {}
}
