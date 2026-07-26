import { Component, Input } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';
import { RouterLink } from '@angular/router';
import { TPipe } from '../../pipes/t.pipe';

/**
 * Écran scindé partagé par les pages d'authentification
 * (connexion, inscription, mot de passe oublié, réinitialisation).
 *
 *   <app-auth-shell title="Connexion" subtitle="…">
 *     …formulaire…
 *     <ng-container footer>Pas de compte ? …</ng-container>
 *   </app-auth-shell>
 *
 * Le panneau de gauche (marque + arguments) disparaît sous 900px :
 * sur mobile, seul le formulaire compte.
 */
@Component({
  selector: 'app-auth-shell',
  standalone: true,
  imports: [MatIconModule, RouterLink, TPipe],
  template: `
    <div class="auth-shell">
      <!-- Panneau de marque -->
      <aside class="auth-aside" aria-hidden="true">
        <span class="aside-veil pattern-stars"></span>
        <div class="aside-content">
          <a routerLink="/" class="aside-brand no-underline">
            <span class="brand-mark">
              <svg viewBox="0 0 32 32" width="18" height="18" fill="none" stroke="currentColor" stroke-width="1.8">
                <rect x="6" y="6" width="20" height="20" rx="2" />
                <rect x="6" y="6" width="20" height="20" rx="2" transform="rotate(45 16 16)" />
              </svg>
            </span>
            Ummati
          </a>

          <p class="aside-quote">
            {{ 'Le temps que vous donnez ne se rattrape pas. Il se transmet.' | t }}
          </p>

          <ul class="aside-points">
            @for (point of points; track point.label) {
              <li>
                <mat-icon>{{ point.icon }}</mat-icon>
                <span>{{ point.label | t }}</span>
              </li>
            }
          </ul>
        </div>
      </aside>

      <!-- Formulaire -->
      <main class="auth-main">
        <div class="auth-box">
          <h1 class="auth-title">{{ title | t }}</h1>
          @if (subtitle) { <p class="auth-subtitle">{{ subtitle | t }}</p> }

          <ng-content />

          <div class="auth-footer">
            <ng-content select="[footer]" />
          </div>
        </div>
      </main>
    </div>
  `,
  styles: [`
    .auth-shell {
      display: grid; grid-template-columns: minmax(0, 0.95fr) minmax(0, 1.05fr);
      min-height: calc(100vh - var(--nav-h));
    }

    /* ---- Panneau de marque ---- */
    .auth-aside {
      position: relative; overflow: hidden;
      background: var(--brand-gradient-deep);
      display: flex; align-items: center; padding: var(--space-8) var(--space-7);
    }
    .aside-veil { position: absolute; inset: 0; opacity: 0.14; filter: invert(1) brightness(3); }
    .aside-content { position: relative; z-index: 1; max-width: 420px; color: #fff; }

    .aside-brand {
      display: inline-flex; align-items: center; gap: var(--space-2);
      color: #fff !important; font-size: 1.3rem; font-weight: 700;
      margin-bottom: var(--space-8); text-decoration: none !important;
    }
    .brand-mark {
      width: 34px; height: 34px; border-radius: 10px; color: #fff;
      background: rgba(255, 255, 255, 0.16); border: 1px solid rgba(255, 255, 255, 0.24);
      display: inline-flex; align-items: center; justify-content: center;
    }

    .aside-quote {
      --font-current: var(--font-display);
      font-size: clamp(1.5rem, 1rem + 1.6vw, 2.15rem);
      font-variation-settings: 'SOFT' 60, 'WONK' 1;
      font-weight: 500; line-height: 1.25; letter-spacing: -0.015em;
      margin: 0 0 var(--space-7);
    }

    .aside-points { list-style: none; margin: 0; padding: 0; display: grid; gap: var(--space-4); }
    .aside-points li { display: flex; align-items: center; gap: var(--space-3); font-size: 0.95rem; }
    .aside-points mat-icon {
      flex: 0 0 auto; width: 34px; height: 34px; font-size: 19px;
      border-radius: 50%; background: rgba(255, 255, 255, 0.14);
      display: flex; align-items: center; justify-content: center;
    }

    /* ---- Colonne formulaire ---- */
    .auth-main {
      display: flex; align-items: center; justify-content: center;
      padding: var(--space-8) var(--space-5);
    }
    .auth-box { width: 100%; max-width: 420px; animation: fade-up 0.5s var(--ease-out) both; }
    .auth-title {
      font-size: clamp(1.8rem, 1.4rem + 1.4vw, 2.3rem);
      font-weight: 600; margin: 0 0 var(--space-2); color: var(--brand-ink);
    }
    .auth-subtitle { color: var(--brand-text-soft); margin: 0 0 var(--space-6); font-size: 1rem; }
    .auth-footer {
      margin-top: var(--space-6); padding-top: var(--space-5);
      border-top: 1px solid var(--brand-border);
      font-size: 0.92rem; color: var(--brand-text-soft); text-align: center;
    }

    @media (max-width: 900px) {
      .auth-shell { grid-template-columns: 1fr; }
      .auth-aside { display: none; }
      .auth-main { padding: var(--space-7) var(--space-4); align-items: flex-start; }
    }
  `],
})
export class AuthShellComponent {
  @Input({ required: true }) title = '';
  @Input() subtitle = '';

  readonly points = [
    { icon: 'search', label: 'Des missions près de chez vous' },
    { icon: 'schedule', label: 'Vos heures suivies et validées' },
    { icon: 'workspace_premium', label: 'Votre attestation en un clic' },
  ];
}
