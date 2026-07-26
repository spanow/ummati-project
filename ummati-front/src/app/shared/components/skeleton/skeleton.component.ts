import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

/** Bloc de chargement animé (balayage défini dans styles.css : .skeleton-block). */
@Component({
  selector: 'app-skeleton',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="skeleton-block"
         [class.sk-text]="type === 'text'"
         [class.sk-circle]="type === 'circle'"
         [class.sk-rect]="type === 'rect'"
         [style.width]="width" [style.height]="height"
         role="progressbar" aria-label="Chargement en cours..." aria-busy="true">
    </div>
  `,
  styles: [`
    .sk-text { height: 14px; width: 100%; border-radius: 6px; }
    .sk-circle { border-radius: 50%; }
    .sk-rect { border-radius: var(--radius-sm); }
  `]
})
export class SkeletonComponent {
  @Input() type: 'text' | 'circle' | 'rect' = 'text';
  @Input() width = '100%';
  @Input() height = '14px';
}

/**
 * Squelette d'une carte de liste — calqué sur la vraie carte (bloc visuel,
 * titre, deux lignes de méta) pour que le passage au contenu ne fasse pas
 * sauter la mise en page.
 */
@Component({
  selector: 'app-card-skeleton',
  standalone: true,
  imports: [SkeletonComponent],
  template: `
    <div class="card-skeleton" role="status" aria-label="Chargement de la carte...">
      <div class="cs-head">
        <app-skeleton type="rect" width="54px" height="54px" />
        <app-skeleton type="rect" width="86px" height="24px" />
      </div>
      <app-skeleton type="text" width="76%" height="19px" />
      <app-skeleton type="text" width="54%" />
      <app-skeleton type="text" width="42%" />
      <app-skeleton type="rect" width="100%" height="6px" />
    </div>
  `,
  styles: [`
    .card-skeleton {
      background: var(--brand-surface);
      border: 1px solid var(--brand-border);
      border-radius: var(--radius-card);
      box-shadow: var(--brand-shadow-xs);
      padding: var(--space-5);
      display: flex; flex-direction: column; gap: var(--space-3);
    }
    .cs-head { display: flex; align-items: center; justify-content: space-between; margin-bottom: var(--space-1); }
  `]
})
export class CardSkeletonComponent {}
