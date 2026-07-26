import { Component, inject, OnInit, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';
import { MatBadgeModule } from '@angular/material/badge';
import { MatDividerModule } from '@angular/material/divider';
import { MatTooltipModule } from '@angular/material/tooltip';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { CommonModule } from '@angular/common';
import { AuthService } from '../../../core/services/auth.service';
import { NotificationService } from '../../../core/services/notification.service';
import { I18nService } from '../../../core/services/i18n.service';
import { ThemeService } from '../../../core/services/theme.service';
import { TPipe } from '../../pipes/t.pipe';

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [
    CommonModule, MatButtonModule, MatIconModule, MatMenuModule,
    MatBadgeModule, MatDividerModule, MatTooltipModule, RouterLink, RouterLinkActive, TPipe,
  ],
  template: `
    <header class="nav-root" role="banner">
      <div class="nav-inner">
        <!-- Marque -->
        <a routerLink="/" class="brand no-underline" aria-label="Ummati">
          <span class="brand-mark" aria-hidden="true">
            <svg viewBox="0 0 32 32" width="20" height="20" fill="none" stroke="currentColor" stroke-width="1.8">
              <rect x="6" y="6" width="20" height="20" rx="2" />
              <rect x="6" y="6" width="20" height="20" rx="2" transform="rotate(45 16 16)" />
            </svg>
          </span>
          <span class="brand-name">Ummati</span>
        </a>

        <!-- Navigation bureau -->
        <nav class="desktop-nav" aria-label="Navigation principale">
          <a routerLink="/organizations" routerLinkActive="active-link" class="nav-link no-underline">{{ 'Organisations' | t }}</a>
          <a routerLink="/events" routerLinkActive="active-link" class="nav-link no-underline">{{ 'Événements' | t }}</a>
          @if (authService.isLoggedIn()) {
            <a routerLink="/dashboard" routerLinkActive="active-link" class="nav-link no-underline">{{ 'Dashboard' | t }}</a>
          }
          @if (authService.isAdmin()) {
            <a routerLink="/admin" routerLinkActive="active-link" class="nav-link no-underline">
              <mat-icon>shield_person</mat-icon> {{ 'Admin' | t }}
            </a>
          }
        </nav>

        <span class="spacer"></span>

        <!-- Thème clair / sombre -->
        <button mat-icon-button class="chrome-btn" (click)="theme.toggle()"
                [matTooltip]="(theme.resolved() === 'dark' ? 'Passer en clair' : 'Passer en sombre') | t"
                [attr.aria-label]="(theme.resolved() === 'dark' ? 'Passer en clair' : 'Passer en sombre') | t">
          <mat-icon class="theme-icon">{{ theme.resolved() === 'dark' ? 'light_mode' : 'dark_mode' }}</mat-icon>
        </button>

        <!-- Langue -->
        <button mat-icon-button [matMenuTriggerFor]="langMenu" class="chrome-btn"
                [matTooltip]="'Langue' | t" [attr.aria-label]="'Langue' | t">
          <mat-icon>translate</mat-icon>
        </button>
        <mat-menu #langMenu="matMenu">
          <button mat-menu-item (click)="i18n.setLang('fr')" [class.lang-active]="i18n.lang() === 'fr'">
            <span class="lang-flag">🇫🇷</span> Français
            @if (i18n.lang() === 'fr') { <mat-icon class="lang-check">check</mat-icon> }
          </button>
          <button mat-menu-item (click)="i18n.setLang('ar')" [class.lang-active]="i18n.lang() === 'ar'">
            <span class="lang-flag">🇩🇿</span> العربية
            @if (i18n.lang() === 'ar') { <mat-icon class="lang-check">check</mat-icon> }
          </button>
        </mat-menu>

        <!-- Actions à droite -->
        @if (authService.isLoggedIn()) {
          <a mat-icon-button routerLink="/notifications" class="chrome-btn"
             [attr.aria-label]="'Notifications' | t" [matTooltip]="'Notifications' | t"
             [matBadge]="unreadCount() > 0 ? unreadCount() : null"
             matBadgeColor="warn" matBadgeSize="small">
            <mat-icon>notifications</mat-icon>
          </a>
          <button mat-button [matMenuTriggerFor]="userMenu" class="user-btn" aria-haspopup="true">
            <span class="user-avatar">{{ (authService.user()?.firstName || '?')[0] }}</span>
            <span class="user-name">{{ authService.user()?.firstName }}</span>
            <mat-icon class="caret">expand_more</mat-icon>
          </button>
          <mat-menu #userMenu="matMenu">
            <button mat-menu-item routerLink="/profile">
              <mat-icon>person</mat-icon> {{ 'Mon profil' | t }}
            </button>
            <button mat-menu-item routerLink="/my-activities">
              <mat-icon>list_alt</mat-icon> {{ 'Mes activités' | t }}
            </button>
            <button mat-menu-item routerLink="/settings">
              <mat-icon>settings</mat-icon> {{ 'Paramètres' | t }}
            </button>
            <mat-divider></mat-divider>
            <button mat-menu-item (click)="authService.logout()">
              <mat-icon>logout</mat-icon> {{ 'Déconnexion' | t }}
            </button>
          </mat-menu>
        } @else {
          <a mat-button routerLink="/login" class="login-link no-underline">{{ 'Connexion' | t }}</a>
          <a mat-flat-button routerLink="/register" class="register-btn no-underline">{{ 'Inscription' | t }}</a>
        }

        <!-- Menu mobile -->
        <button mat-icon-button class="mobile-menu-btn" (click)="mobileOpen.set(!mobileOpen())"
                [attr.aria-expanded]="mobileOpen()" aria-controls="mobile-menu"
                [attr.aria-label]="'Ouvrir le menu' | t">
          <mat-icon>{{ mobileOpen() ? 'close' : 'menu' }}</mat-icon>
        </button>
      </div>
    </header>

    <!-- Tiroir mobile -->
    @if (mobileOpen()) {
      <nav id="mobile-menu" class="mobile-menu" aria-label="Navigation mobile" role="navigation">
        <a mat-button routerLink="/" (click)="mobileOpen.set(false)">
          <mat-icon>home</mat-icon> {{ 'Accueil' | t }}
        </a>
        <a mat-button routerLink="/organizations" (click)="mobileOpen.set(false)">
          <mat-icon>diversity_3</mat-icon> {{ 'Organisations' | t }}
        </a>
        <a mat-button routerLink="/events" (click)="mobileOpen.set(false)">
          <mat-icon>event</mat-icon> {{ 'Événements' | t }}
        </a>
        @if (authService.isLoggedIn()) {
          <a mat-button routerLink="/dashboard" (click)="mobileOpen.set(false)">
            <mat-icon>dashboard</mat-icon> {{ 'Dashboard' | t }}
          </a>
          @if (authService.isAdmin()) {
            <a mat-button routerLink="/admin" (click)="mobileOpen.set(false)">
              <mat-icon>shield_person</mat-icon> {{ 'Administration' | t }}
            </a>
          }
          <a mat-button routerLink="/notifications" (click)="mobileOpen.set(false)">
            <mat-icon>notifications</mat-icon> {{ 'Notifications' | t }}
            @if (unreadCount() > 0) { <span class="badge badge-danger mobile-badge">{{ unreadCount() }}</span> }
          </a>
          <a mat-button routerLink="/profile" (click)="mobileOpen.set(false)">
            <mat-icon>person</mat-icon> {{ 'Profil' | t }}
          </a>
          <a mat-button routerLink="/settings" (click)="mobileOpen.set(false)">
            <mat-icon>settings</mat-icon> {{ 'Paramètres' | t }}
          </a>
          <button mat-button class="logout-btn" (click)="authService.logout(); mobileOpen.set(false)">
            <mat-icon>logout</mat-icon> {{ 'Déconnexion' | t }}
          </button>
        } @else {
          <a mat-button routerLink="/login" (click)="mobileOpen.set(false)">{{ 'Connexion' | t }}</a>
          <a mat-flat-button routerLink="/register" (click)="mobileOpen.set(false)">{{ 'Inscription' | t }}</a>
        }
        <mat-divider></mat-divider>
        <div class="mobile-pref-row">
          <button mat-button (click)="i18n.setLang('fr')" [class.lang-active]="i18n.lang() === 'fr'">🇫🇷 Français</button>
          <button mat-button (click)="i18n.setLang('ar')" [class.lang-active]="i18n.lang() === 'ar'">🇩🇿 العربية</button>
          <button mat-button (click)="theme.toggle()">
            <mat-icon>{{ theme.resolved() === 'dark' ? 'light_mode' : 'dark_mode' }}</mat-icon>
            {{ (theme.resolved() === 'dark' ? 'Clair' : 'Sombre') | t }}
          </button>
        </div>
      </nav>
    }
  `,
  styles: [`
    .nav-root {
      position: sticky; top: 0; z-index: 100;
      background: color-mix(in oklab, var(--brand-bg) 82%, transparent);
      backdrop-filter: blur(14px) saturate(1.4);
      -webkit-backdrop-filter: blur(14px) saturate(1.4);
      border-bottom: 1px solid var(--brand-border);
    }
    .nav-inner {
      max-width: var(--page-max); margin: 0 auto; height: var(--nav-h);
      display: flex; align-items: center; gap: var(--space-1); padding: 0 var(--space-5);
    }

    .brand { display: flex; align-items: center; gap: 10px; text-decoration: none !important; }
    .brand-mark {
      width: 38px; height: 38px; border-radius: 11px; color: #fff;
      background: var(--brand-gradient); display: flex; align-items: center; justify-content: center;
      box-shadow: 0 4px 14px color-mix(in oklab, var(--brand-primary) 40%, transparent);
      transition: transform 0.4s var(--ease-spring);
    }
    .brand:hover .brand-mark { transform: rotate(45deg); }
    .brand-name {
      font-size: 1.4rem; font-weight: 600; color: var(--brand-ink);
      letter-spacing: -0.02em; line-height: 1;
    }

    .spacer { flex: 1 1 auto; }
    .desktop-nav { display: flex; gap: 2px; margin-inline-start: var(--space-5); }
    .nav-link {
      position: relative;
      display: inline-flex; align-items: center; gap: 6px;
      padding: 9px 14px; border-radius: var(--radius-sm);
      color: var(--brand-text); font-weight: 600; font-size: 0.94rem;
      text-decoration: none !important; transition: color 0.16s, background 0.16s;
    }
    .nav-link:hover { background: var(--brand-surface-2); color: var(--brand-ink); }
    .nav-link.active-link { color: var(--brand-primary-dark); }
    /* Soulignement de l'onglet actif : un trait, pas une pastille */
    .nav-link.active-link::after {
      content: ''; position: absolute; inset-inline: 14px; bottom: 2px; height: 2px;
      border-radius: 2px; background: var(--brand-primary);
    }
    .nav-link mat-icon { font-size: 18px; width: 18px; height: 18px; }

    .chrome-btn { color: var(--brand-text-soft); }
    .chrome-btn:hover { color: var(--brand-ink); }
    .theme-icon { transition: transform 0.5s var(--ease-spring); }
    .chrome-btn:hover .theme-icon { transform: rotate(30deg); }

    .lang-flag { margin-inline-end: 8px; }
    .lang-check { margin-inline-start: 8px; color: var(--brand-primary); }
    .lang-active { font-weight: 700; }

    .user-btn {
      display: flex; align-items: center; gap: 8px;
      border-radius: var(--radius-pill) !important;
      padding: 4px 12px 4px 4px !important;
      border: 1px solid var(--brand-border);
    }
    .user-btn:hover { background: var(--brand-surface-2); }
    .user-avatar {
      width: 30px; height: 30px; border-radius: 50%;
      background: var(--brand-gradient); color: #fff;
      display: inline-flex; align-items: center; justify-content: center;
      font-weight: 700; font-size: 0.88rem; text-transform: uppercase;
    }
    .user-name {
      max-width: 120px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
      color: var(--brand-ink); font-weight: 600;
    }
    .caret { color: var(--brand-text-faint); font-size: 20px; width: 20px; height: 20px; }
    .login-link { color: var(--brand-ink); font-weight: 650; }
    .register-btn { border-radius: var(--radius-pill) !important; font-weight: 700; }
    .mobile-menu-btn { display: none !important; color: var(--brand-ink); }

    .mobile-menu {
      position: fixed; top: var(--nav-h); inset-inline: 0; bottom: 0;
      background: var(--brand-bg); z-index: 99; padding: var(--space-4);
      display: flex; flex-direction: column; gap: var(--space-1);
      overflow-y: auto;
      animation: fade-up 0.22s var(--ease-out) both;
    }
    .mobile-menu a, .mobile-menu button {
      width: 100%; justify-content: flex-start !important;
      padding: 12px 16px !important; border-radius: var(--radius-sm) !important;
    }
    .mobile-badge { margin-inline-start: auto; }
    .logout-btn { color: var(--brand-danger) !important; }
    .mobile-pref-row { display: flex; gap: var(--space-2); padding-top: var(--space-2); flex-wrap: wrap; }
    .mobile-pref-row button { width: auto; }

    @media (max-width: 900px) {
      .nav-inner { padding: 0 var(--space-3); }
      .desktop-nav, .user-btn, .register-btn, .login-link { display: none !important; }
      .mobile-menu-btn { display: inline-flex !important; }
    }
  `],
})
export class NavbarComponent implements OnInit {
  authService = inject(AuthService);
  i18n = inject(I18nService);
  theme = inject(ThemeService);
  private notificationService = inject(NotificationService);
  unreadCount = signal(0);
  mobileOpen = signal(false);

  ngOnInit() {
    if (this.authService.isLoggedIn()) {
      this.loadUnreadCount();
    }
  }

  private loadUnreadCount() {
    this.notificationService.unreadCount().subscribe({
      next: (res) => this.unreadCount.set(res?.data?.count ?? 0),
      error: () => {}
    });
  }
}
