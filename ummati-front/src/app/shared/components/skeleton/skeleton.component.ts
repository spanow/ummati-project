import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

/** Skeleton loader animé pour les états de chargement */
@Component({
  selector: 'app-skeleton',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="skeleton"
         [class.skeleton-text]="type === 'text'"
         [class.skeleton-circle]="type === 'circle'"
         [class.skeleton-rect]="type === 'rect'"
         [style.width]="width" [style.height]="height"
         role="progressbar" aria-label="Chargement en cours..." aria-busy="true">
    </div>
  `,
  styles: [`
    .skeleton {
      background: linear-gradient(90deg, #f0f0f0 25%, #e0e0e0 50%, #f0f0f0 75%);
      background-size: 200% 100%;
      animation: shimmer 1.5s infinite;
      border-radius: 4px;
    }
    .skeleton-text { height: 16px; width: 100%; border-radius: 4px; }
    .skeleton-circle { border-radius: 50%; }
    .skeleton-rect { border-radius: 8px; }
    @keyframes shimmer {
      0% { background-position: 200% 0; }
      100% { background-position: -200% 0; }
    }
  `]
})
export class SkeletonComponent {
  @Input() type: 'text' | 'circle' | 'rect' = 'text';
  @Input() width = '100%';
  @Input() height = '16px';
}

/** Squelette d'une carte */
@Component({
  selector: 'app-card-skeleton',
  standalone: true,
  imports: [SkeletonComponent],
  template: `
    <div class="card-skeleton" role="status" aria-label="Chargement de la carte...">
      <app-skeleton type="rect" height="180px"></app-skeleton>
      <div class="card-body">
        <app-skeleton type="text" width="60%" height="20px"></app-skeleton>
        <app-skeleton type="text" width="80%"></app-skeleton>
        <app-skeleton type="text" width="40%"></app-skeleton>
      </div>
    </div>
  `,
  styles: [`
    .card-skeleton { background: white; border-radius: 12px; overflow: hidden; box-shadow: 0 2px 8px rgba(0,0,0,0.08); }
    .card-body { padding: 16px; display: flex; flex-direction: column; gap: 10px; }
  `]
})
export class CardSkeletonComponent {}

