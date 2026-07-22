import { Component, inject, OnInit, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';
import { MatBadgeModule } from '@angular/material/badge';
import { MatDividerModule } from '@angular/material/divider';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { CommonModule } from '@angular/common';
import { AuthService } from '../../../core/services/auth.service';
import { NotificationService } from '../../../core/services/notification.service';
import { I18nService } from '../../../core/services/i18n.service';
import { TPipe } from '../../pipes/t.pipe';

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [
    CommonModule, MatButtonModule, MatIconModule, MatMenuModule,
    MatBadgeModule, MatDividerModule, RouterLink, RouterLinkActive, TPipe,
  ],
  template: `
    <header class="nav-root" role="banner">
      <div class="nav-inner">
        <!-- Brand -->
        <a routerLink="/" class="brand" aria-label="Ummati">
          <span class="brand-mark"><mat-icon aria-hidden="true">volunteer_activism</mat-icon></span>
          <span class="brand-name">Ummati</span>
        </a>

        <!-- Desktop nav -->
        <nav class="desktop-nav" aria-label="Navigation principale">
          <a routerLink="/organizations" routerLinkActive="active-link" class="nav-link">{{ 'Organisations' | t }}</a>
          <a routerLink="/events" routerLinkActive="active-link" class="nav-link">{{ 'Événements' | t }}</a>
          @if (authService.isLoggedIn()) {
            <a routerLink="/dashboard" routerLinkActive="active-link" class="nav-link">{{ 'Dashboard' | t }}</a>
          }
          @if (authService.isAdmin()) {
            <a routerLink="/admin" routerLinkActive="active-link" class="nav-link admin-link">
              <mat-icon>admin_panel_settings</mat-icon> {{ 'Admin' | t }}
            </a>
          }
        </nav>

        <span class="spacer"></span>

        <!-- Language switcher -->
        <button mat-icon-button [matMenuTriggerFor]="langMenu" class="lang-btn"
                [attr.aria-label]="'Langue' | t">
          <mat-icon>language</mat-icon>
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

        <!-- Right actions -->
        @if (authService.isLoggedIn()) {
          <a mat-icon-button routerLink="/notifications" class="bell-btn"
             [attr.aria-label]="'Notifications' | t"
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
          <a mat-button routerLink="/login" class="login-link">{{ 'Connexion' | t }}</a>
          <a mat-flat-button routerLink="/register" class="register-btn">{{ 'Inscription' | t }}</a>
        }

        <!-- Mobile burger -->
        <button mat-icon-button class="mobile-menu-btn" (click)="mobileOpen.set(!mobileOpen())"
                [attr.aria-expanded]="mobileOpen()" aria-controls="mobile-menu"
                [attr.aria-label]="'Ouvrir le menu' | t">
          <mat-icon>{{ mobileOpen() ? 'close' : 'menu' }}</mat-icon>
        </button>
      </div>
    </header>

    <!-- Mobile drawer -->
    @if (mobileOpen()) {
      <nav id="mobile-menu" class="mobile-menu" aria-label="Navigation mobile" role="navigation">
        <a mat-button routerLink="/" (click)="mobileOpen.set(false)">
          <mat-icon>home</mat-icon> {{ 'Accueil' | t }}
        </a>
        <a mat-button routerLink="/organizations" (click)="mobileOpen.set(false)">
          <mat-icon>business</mat-icon> {{ 'Organisations' | t }}
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
              <mat-icon>admin_panel_settings</mat-icon> {{ 'Administration' | t }}
            </a>
          }
          <a mat-button routerLink="/notifications" (click)="mobileOpen.set(false)">
            <mat-icon>notifications</mat-icon> {{ 'Notifications' | t }}
            @if (unreadCount() > 0) { <span class="mobile-badge">{{ unreadCount() }}</span> }
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
        <div class="mobile-lang-row">
          <button mat-button (click)="i18n.setLang('fr')" [class.lang-active]="i18n.lang() === 'fr'">🇫🇷 Français</button>
          <button mat-button (click)="i18n.setLang('ar')" [class.lang-active]="i18n.lang() === 'ar'">🇩🇿 العربية</button>
        </div>
      </nav>
    }
  `,
  styles: [`
    .nav-root {
      position: sticky; top: 0; z-index: 100;
      background: rgba(255, 255, 255, 0.92);
      backdrop-filter: blur(10px);
      border-bottom: 1px solid var(--brand-border);
    }
    .nav-inner {
      max-width: var(--page-max); margin: 0 auto; height: 66px;
      display: flex; align-items: center; gap: 8px; padding: 0 24px;
    }
    .brand { display: flex; align-items: center; gap: 10px; text-decoration: none !important; }
    .brand-mark {
      width: 38px; height: 38px; border-radius: 12px;
      background: var(--brand-gradient); display: flex; align-items: center; justify-content: center;
      box-shadow: 0 4px 12px rgba(15, 118, 110, 0.28);
    }
    .brand-mark mat-icon { color: white; font-size: 22px; width: 22px; height: 22px; }
    .brand-name { font-size: 1.25rem; font-weight: 800; color: var(--brand-primary-dark); letter-spacing: -0.02em; }
    .spacer { flex: 1 1 auto; }
    .desktop-nav { display: flex; gap: 4px; margin: 0 12px; }
    .nav-link {
      display: inline-flex; align-items: center; gap: 6px;
      padding: 8px 16px; border-radius: 999px;
      color: var(--brand-text); font-weight: 600; font-size: 0.92rem;
      text-decoration: none !important; transition: background 0.15s, color 0.15s;
    }
    .nav-link:hover { background: var(--brand-primary-soft); color: var(--brand-primary-dark); }
    .nav-link.active-link { background: var(--brand-primary-100); color: var(--brand-primary-dark); }
    .nav-link mat-icon { font-size: 18px; width: 18px; height: 18px; }
    .lang-btn, .bell-btn { color: var(--brand-text); }
    .lang-flag { margin-inline-end: 8px; }
    .lang-check { margin-inline-start: 8px; color: var(--brand-primary); }
    .lang-active { font-weight: 700; }
    .user-btn { display: flex; align-items: center; gap: 8px; border-radius: 999px !important; padding: 4px 10px 4px 4px !important; }
    .user-avatar {
      width: 32px; height: 32px; border-radius: 50%;
      background: var(--brand-gradient); color: white;
      display: inline-flex; align-items: center; justify-content: center;
      font-weight: 700; font-size: 0.9rem;
    }
    .user-name { max-width: 120px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; color: var(--brand-ink); font-weight: 600; }
    .caret { color: var(--brand-text-soft); }
    .login-link { color: var(--brand-ink); font-weight: 600; }
    .register-btn { border-radius: 999px !important; font-weight: 700; }
    .mobile-menu-btn { display: none !important; color: var(--brand-ink); }

    .mobile-menu {
      position: fixed; top: 66px; inset-inline: 0; bottom: 0;
      background: white; z-index: 99; padding: 16px;
      display: flex; flex-direction: column; gap: 4px;
      box-shadow: 0 4px 20px rgba(0,0,0,0.15);
      overflow-y: auto;
    }
    .mobile-menu a, .mobile-menu button {
      width: 100%; justify-content: flex-start !important;
      padding: 12px 16px !important;
      border-radius: 10px !important;
    }
    .mobile-badge {
      background: #f44336; color: white;
      border-radius: 10px; padding: 1px 6px;
      font-size: 0.75rem; margin-inline-start: 8px;
    }
    .logout-btn { color: #f44336 !important; }
    .mobile-lang-row { display: flex; gap: 8px; padding-top: 8px; }
    .mobile-lang-row button { width: auto; }

    @media (max-width: 768px) {
      .nav-inner { padding: 0 12px; }
      .desktop-nav { display: none !important; }
      .user-btn, .register-btn, .login-link, .lang-btn { display: none !important; }
      .mobile-menu-btn { display: inline-flex !important; }
    }
  `],
})
export class NavbarComponent implements OnInit {
  authService = inject(AuthService);
  i18n = inject(I18nService);
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
