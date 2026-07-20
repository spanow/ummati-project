import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';

/** Avatar utilisateur avec initiales ou photo */
@Component({
  selector: 'app-avatar',
  standalone: true,
  imports: [CommonModule, MatIconModule],
  template: `
    @if (photoUrl) {
      <img [src]="photoUrl" [alt]="name || 'Avatar'" class="avatar-img"
           [style.width]="size + 'px'" [style.height]="size + 'px'"
           loading="lazy" (error)="photoUrl = null">
    } @else {
      <div class="avatar-initials" [style.width]="size + 'px'" [style.height]="size + 'px'"
           [style.font-size]="(size / 2.5) + 'px'" [style.background]="color"
           [attr.aria-label]="name || 'Utilisateur'">
        {{ initials }}
      </div>
    }
  `,
  styles: [`
    :host { display: inline-flex; }
    .avatar-img { border-radius: 50%; object-fit: cover; }
    .avatar-initials {
      border-radius: 50%; display: flex; align-items: center; justify-content: center;
      font-weight: 600; color: white; user-select: none;
    }
  `]
})
export class AvatarComponent {
  @Input() name: string | null = null;
  @Input() photoUrl: string | null = null;
  @Input() size = 40;
  @Input() color = 'var(--brand-primary)';

  get initials(): string {
    if (!this.name) return '?';
    return this.name.split(' ').slice(0, 2).map(w => w[0]?.toUpperCase() ?? '').join('');
  }
}

