import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

/** Barre de progression avec label */
@Component({
  selector: 'app-progress-bar',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="progress-wrapper">
      @if (label) { <span class="progress-label" [attr.id]="labelId">{{ label }}</span> }
      <div class="progress-track" role="progressbar"
           [attr.aria-valuenow]="value" [attr.aria-valuemin]="0" [attr.aria-valuemax]="max"
           [attr.aria-labelledby]="labelId" [attr.aria-label]="label ?? 'Progression'">
        <div class="progress-fill"
             [style.width]="percentage + '%'"
             [class.danger]="percentage >= 90"
             [class.warning]="percentage >= 70 && percentage < 90">
        </div>
      </div>
      @if (showValue) {
        <span class="progress-value" aria-hidden="true">{{ value }}/{{ max }}</span>
      }
    </div>
  `,
  styles: [`
    .progress-wrapper { display: flex; align-items: center; gap: 10px; }
    .progress-label { font-size: 0.85rem; color: #666; white-space: nowrap; }
    .progress-track { flex: 1; height: 8px; background: #e0e0e0; border-radius: 4px; overflow: hidden; }
    .progress-fill {
      height: 100%; border-radius: 4px; transition: width 0.3s ease;
      background: linear-gradient(90deg, #3f51b5, #7c4dff);
    }
    .progress-fill.warning { background: linear-gradient(90deg, #ff9800, #ff5722); }
    .progress-fill.danger { background: linear-gradient(90deg, #f44336, #d32f2f); }
    .progress-value { font-size: 0.8rem; color: #888; white-space: nowrap; }
  `]
})
export class ProgressBarComponent {
  @Input() value = 0;
  @Input() max = 100;
  @Input() label?: string;
  @Input() showValue = false;
  readonly labelId = `progress-${Math.random().toString(36).slice(2)}`;

  get percentage(): number {
    if (this.max === 0) return 0;
    return Math.min(100, Math.round((this.value / this.max) * 100));
  }
}

