import { Component, inject, OnInit, signal } from '@angular/core';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';
import { MatBadgeModule } from '@angular/material/badge';
import { MatSidenavModule } from '@angular/material/sidenav';
import { MatListModule } from '@angular/material/list';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { CommonModule } from '@angular/common';
import { AuthService } from '../../../core/services/auth.service';
import { NotificationService } from '../../../core/services/notification.service';

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [
    CommonModule, MatToolbarModule, MatButtonModule, MatIconModule,
    MatMenuModule, MatBadgeModule, MatSidenavModule, MatListModule,
    RouterLink, RouterLinkActive
  ],
  template: `
    <mat-toolbar color="primary" role="banner">
      <!-- Logo -->
      <a mat-button routerLink="/" class="brand" aria-label="Ummati - Accueil">
        <mat-icon aria-hidden="true">volunteer_activism</mat-icon>
        <span>Ummati</span>
      </a>

      <!-- Desktop nav -->
      <nav class="desktop-nav" aria-label="Navigation principale">
        <a mat-button routerLink="/organizations" routerLinkActive="active-link" aria-label="Organisations">Organisations</a>
        <a mat-button routerLink="/events" routerLinkActive="active-link" aria-label="Événements">Événements</a>
        @if (authService.isLoggedIn()) {
          <a mat-button routerLink="/dashboard" routerLinkActive="active-link" aria-label="Tableau de bord">Dashboard</a>
        }
        @if (authService.isAdmin()) {
          <a mat-button routerLink="/admin" routerLinkActive="active-link" aria-label="Administration">
            <mat-icon>admin_panel_settings</mat-icon> Admin
          </a>
        }
      </nav>

      <span class="spacer"></span>

      <!-- Right actions -->
      @if (authService.isLoggedIn()) {
        <!-- Notification bell -->
        <a mat-icon-button routerLink="/notifications"
           [attr.aria-label]="unreadCount() > 0 ? unreadCount() + ' notifications non lues' : 'Notifications'"
           [matBadge]="unreadCount() > 0 ? unreadCount() : null"
           matBadgeColor="warn" matBadgeSize="small">
          <mat-icon>notifications</mat-icon>
        </a>
        <!-- User menu -->
        <button mat-button [matMenuTriggerFor]="userMenu" class="user-btn"
                aria-label="Menu utilisateur" aria-haspopup="true">
          <mat-icon>account_circle</mat-icon>
          <span class="user-name">{{ authService.user()?.firstName }}</span>
          <mat-icon>arrow_drop_down</mat-icon>
        </button>
        <mat-menu #userMenu="matMenu">
          <button mat-menu-item routerLink="/profile">
            <mat-icon>person</mat-icon> Mon profil
          </button>
          <button mat-menu-item routerLink="/my-activities">
            <mat-icon>list_alt</mat-icon> Mes activités
          </button>
          <button mat-menu-item routerLink="/settings">
            <mat-icon>settings</mat-icon> Paramètres
          </button>
          <mat-divider></mat-divider>
          <button mat-menu-item (click)="authService.logout()">
            <mat-icon>logout</mat-icon> Déconnexion
          </button>
        </mat-menu>
      } @else {
        <a mat-button routerLink="/login" aria-label="Se connecter">Connexion</a>
        <a mat-flat-button routerLink="/register" class="register-btn" aria-label="S'inscrire">Inscription</a>
      }

      <!-- Mobile burger button -->
      <button mat-icon-button class="mobile-menu-btn" (click)="mobileOpen.set(!mobileOpen())"
              [attr.aria-expanded]="mobileOpen()" aria-controls="mobile-menu" aria-label="Ouvrir le menu">
        <mat-icon>{{ mobileOpen() ? 'close' : 'menu' }}</mat-icon>
      </button>
    </mat-toolbar>

    <!-- Mobile drawer -->
    @if (mobileOpen()) {
      <nav id="mobile-menu" class="mobile-menu" aria-label="Navigation mobile" role="navigation">
        <a mat-button routerLink="/" (click)="mobileOpen.set(false)">
          <mat-icon>home</mat-icon> Accueil
        </a>
        <a mat-button routerLink="/organizations" (click)="mobileOpen.set(false)">
          <mat-icon>business</mat-icon> Organisations
        </a>
        <a mat-button routerLink="/events" (click)="mobileOpen.set(false)">
          <mat-icon>event</mat-icon> Événements
        </a>
        @if (authService.isLoggedIn()) {
          <a mat-button routerLink="/dashboard" (click)="mobileOpen.set(false)">
            <mat-icon>dashboard</mat-icon> Dashboard
          </a>
          @if (authService.isAdmin()) {
            <a mat-button routerLink="/admin" (click)="mobileOpen.set(false)">
              <mat-icon>admin_panel_settings</mat-icon> Administration
            </a>
          }
          <a mat-button routerLink="/notifications" (click)="mobileOpen.set(false)">
            <mat-icon>notifications</mat-icon> Notifications
            @if (unreadCount() > 0) { <span class="mobile-badge">{{ unreadCount() }}</span> }
          </a>
          <a mat-button routerLink="/profile" (click)="mobileOpen.set(false)">
            <mat-icon>person</mat-icon> Profil
          </a>
          <a mat-button routerLink="/settings" (click)="mobileOpen.set(false)">
            <mat-icon>settings</mat-icon> Paramètres
          </a>
          <button mat-button class="logout-btn" (click)="authService.logout(); mobileOpen.set(false)">
            <mat-icon>logout</mat-icon> Déconnexion
          </button>
        } @else {
          <a mat-button routerLink="/login" (click)="mobileOpen.set(false)">Connexion</a>
          <a mat-flat-button color="primary" routerLink="/register" (click)="mobileOpen.set(false)">Inscription</a>
        }
      </nav>
    }
  `,
  styles: [`
    mat-toolbar { position: sticky; top: 0; z-index: 100; box-shadow: 0 2px 8px rgba(0,0,0,0.12); }
    .brand {
      font-size: 1.2rem; font-weight: 700; display: flex; align-items: center; gap: 8px;
    }
    .spacer { flex: 1 1 auto; }
    .desktop-nav { display: flex; gap: 4px; }
    .active-link { font-weight: 700; background: rgba(255,255,255,0.15); border-radius: 6px; }
    .user-btn { display: flex; align-items: center; gap: 4px; }
    .user-name { max-width: 120px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
    .register-btn { border-radius: 20px !important; margin-left: 8px; }
    .mobile-menu-btn { display: none !important; }

    /* Mobile menu */
    .mobile-menu {
      position: fixed; top: 64px; left: 0; right: 0; bottom: 0;
      background: white; z-index: 99; padding: 16px;
      display: flex; flex-direction: column; gap: 4px;
      box-shadow: 0 4px 20px rgba(0,0,0,0.15);
      overflow-y: auto;
    }
    .mobile-menu a, .mobile-menu button {
      width: 100%; justify-content: flex-start !important;
      text-align: left; padding: 12px 16px !important;
      border-radius: 8px !important;
    }
    .mobile-badge {
      background: #f44336; color: white;
      border-radius: 10px; padding: 1px 6px;
      font-size: 0.75rem; margin-left: 8px;
    }
    .logout-btn { color: #f44336 !important; }

    @media (max-width: 768px) {
      .desktop-nav { display: none !important; }
      .user-btn, .register-btn { display: none !important; }
      a[routerLink="/login"] { display: none !important; }
      :host-context(mat-toolbar) a[mat-flat-button]:not(.brand) { display: none; }
      .mobile-menu-btn { display: inline-flex !important; }
    }
  `],
})
export class NavbarComponent implements OnInit {
  authService = inject(AuthService);
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
