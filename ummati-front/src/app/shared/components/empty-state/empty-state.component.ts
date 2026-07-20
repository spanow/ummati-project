import { Component, Input } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { RouterLink } from '@angular/router';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-empty-state',
  standalone: true,
  imports: [CommonModule, MatButtonModule, MatIconModule, RouterLink],
  template: `
    <div class="empty-state" [attr.aria-label]="title" role="status">
      <div class="empty-icon">
        <mat-icon aria-hidden="true">{{ icon }}</mat-icon>
      </div>
      <h3>{{ title }}</h3>
      @if (description) {
        <p>{{ description }}</p>
      }
      @if (ctaLabel && ctaLink) {
        <a mat-flat-button color="primary" [routerLink]="ctaLink" class="cta-btn">
          @if (ctaIcon) { <mat-icon>{{ ctaIcon }}</mat-icon> }
          {{ ctaLabel }}
        </a>
      }
      @if (ctaLabel && ctaClick) {
        <button mat-flat-button color="primary" (click)="ctaClick()" class="cta-btn">
          @if (ctaIcon) { <mat-icon>{{ ctaIcon }}</mat-icon> }
          {{ ctaLabel }}
        </button>
      }
    </div>
  `,
  styles: [`
    .empty-state {
      display: flex; flex-direction: column; align-items: center; justify-content: center;
      padding: 64px 24px; text-align: center; gap: 16px;
    }
    .empty-icon {
      width: 80px; height: 80px; border-radius: 50%;
      background: linear-gradient(135deg, var(--brand-primary-100), var(--brand-primary-100));
      display: flex; align-items: center; justify-content: center; margin-bottom: 8px;
    }
    .empty-icon mat-icon { font-size: 40px !important; width: 40px !important; height: 40px !important; color: var(--brand-primary); }
    h3 { font-size: 1.2rem; font-weight: 600; color: var(--brand-ink); margin: 0; }
    p { color: #666; max-width: 360px; line-height: 1.6; margin: 0; }
    .cta-btn { border-radius: 20px !important; margin-top: 8px; }
  `]
})
export class EmptyStateComponent {
  @Input() icon = 'inbox';
  @Input() title = 'Aucun résultat';
  @Input() description?: string;
  @Input() ctaLabel?: string;
  @Input() ctaLink?: string;
  @Input() ctaIcon?: string;
  @Input() ctaClick?: () => void;
}

