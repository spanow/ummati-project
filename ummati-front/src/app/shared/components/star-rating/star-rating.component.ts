import { Component, EventEmitter, Input, Output } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';

@Component({
  selector: 'app-star-rating',
  standalone: true,
  imports: [MatIconModule],
  template: `
    <div class="stars">
      @for (star of [1,2,3,4,5]; track star) {
        <mat-icon class="star" [class.filled]="star <= (hovered || value)"
                  (mouseenter)="hovered = readonly ? 0 : star"
                  (mouseleave)="hovered = 0"
                  (click)="!readonly && select(star)">
          {{ star <= (hovered || value) ? 'star' : 'star_border' }}
        </mat-icon>
      }
    </div>
  `,
  styles: [`
    .stars { display: inline-flex; gap: 2px; }
    .star { cursor: pointer; color: var(--brand-text-faint); font-size: 28px; width: 28px; height: 28px; transition: color 0.15s; }
    .star.filled { color: var(--brand-accent); }
    :host-context([readonly]) .star { cursor: default; }
  `],
})
export class StarRatingComponent {
  @Input() value = 0;
  @Input() readonly = false;
  @Output() valueChange = new EventEmitter<number>();
  hovered = 0;

  select(star: number) {
    this.value = star;
    this.valueChange.emit(star);
  }
}

