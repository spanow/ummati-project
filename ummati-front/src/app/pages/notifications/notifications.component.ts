import { Component, signal, OnInit } from '@angular/core';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatBadgeModule } from '@angular/material/badge';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { Router } from '@angular/router';
import { DatePipe } from '@angular/common';
import { NotificationService, NotificationItem } from '../../core/services/notification.service';
import { TPipe } from '../../shared/pipes/t.pipe';

@Component({
  selector: 'app-notifications',
  standalone: true,
  imports: [MatCardModule, MatButtonModule, MatIconModule, MatBadgeModule,
    MatPaginatorModule, MatProgressSpinnerModule, DatePipe, TPipe],
  template: `
    <div class="page page-narrow">
      <header class="page-header">
        <h1>{{ 'Notifications' | t }}</h1>
        <button mat-stroked-button (click)="markAllRead()" [disabled]="unreadCount() === 0">
          <mat-icon>done_all</mat-icon> {{ 'Tout marquer comme lu' | t }}
        </button>
      </header>

      @if (loading()) {
        <div class="state-center"><mat-spinner diameter="40" /></div>
      } @else if (notifications().length === 0) {
        <div class="empty-state">
          <mat-icon class="empty-icon">notifications_off</mat-icon>
          <h3>{{ 'Aucune notification' | t }}</h3>
          <p>{{ 'Vous êtes à jour !' | t }}</p>
        </div>
      } @else {
        <div class="notif-list">
          @for (n of notifications(); track n.id) {
            <mat-card class="notif-card" [class.unread]="!n.read" (click)="onClickNotif(n)">
              <mat-card-content>
                <div class="notif-row">
                  <mat-icon class="notif-icon" [class.unread-icon]="!n.read">
                    {{ getIcon(n.type) }}
                  </mat-icon>
                  <div class="notif-body">
                    <strong>{{ n.title }}</strong>
                    <p>{{ n.message }}</p>
                    <span class="notif-date">{{ n.createdAt | date:'d MMM yyyy, HH:mm' }}</span>
                  </div>
                  @if (!n.read) {
                    <div class="unread-dot"></div>
                  }
                </div>
              </mat-card-content>
            </mat-card>
          }
        </div>
        <mat-paginator [length]="totalElements()" [pageSize]="20" [pageIndex]="currentPage()" (page)="onPage($event)" />
      }
    </div>
  `,
  styles: [`
    .page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 24px; gap: 16px; flex-wrap: wrap; }
    .page-header h1 { font-size: 1.8rem; font-weight: 800; margin: 0; letter-spacing: -0.02em; }
    .empty-state { text-align: center; padding: 80px 24px; }
    .empty-icon { font-size: 64px; width: 64px; height: 64px; color: var(--brand-text-faint); }
    .notif-list { display: flex; flex-direction: column; gap: 8px; }
    .notif-card { border-radius: 12px; cursor: pointer; transition: background 0.2s; }
    .notif-card:hover { background: var(--brand-surface-2); }
    .notif-card.unread { border-left: 3px solid var(--brand-primary); }
    .notif-row { display: flex; gap: 16px; align-items: flex-start; }
    .notif-icon { color: var(--brand-text-soft); margin-top: 2px; }
    .unread-icon { color: var(--brand-primary); }
    .notif-body { flex: 1; }
    .notif-body strong { display: block; margin-bottom: 4px; color: var(--brand-ink); }
    .notif-body p { margin: 0; color: var(--brand-text); font-size: 0.9rem; }
    .notif-date { font-size: 0.8rem; color: var(--brand-text-faint); }
    .unread-dot { width: 10px; height: 10px; border-radius: 50%; background: var(--brand-primary); margin-top: 6px; flex-shrink: 0; }
  `],
})
export class NotificationsComponent implements OnInit {
  notifications = signal<NotificationItem[]>([]);
  loading = signal(true);
  totalElements = signal(0);
  currentPage = signal(0);
  unreadCount = signal(0);

  constructor(private notifService: NotificationService, private router: Router) {}

  ngOnInit() { this.load(); this.loadUnread(); }

  load() {
    this.loading.set(true);
    this.notifService.list(this.currentPage()).subscribe({
      next: res => {
        this.notifications.set(res.data.content);
        this.totalElements.set(res.data.totalElements);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  loadUnread() {
    this.notifService.unreadCount().subscribe(res => this.unreadCount.set(res.data.count));
  }

  onClickNotif(n: NotificationItem) {
    const navigate = () => { if (n.link) this.router.navigateByUrl(n.link); };
    if (!n.read) {
      this.notifService.markAsRead(n.id).subscribe(() => {
        this.notifications.update(list => list.map(item => item.id === n.id ? { ...item, read: true } : item));
        this.unreadCount.update(c => Math.max(0, c - 1));
        navigate();
      });
    } else {
      navigate();
    }
  }

  markAllRead() {
    this.notifService.markAllAsRead().subscribe(() => {
      this.notifications.update(list => list.map(n => ({ ...n, read: true })));
      this.unreadCount.set(0);
    });
  }

  onPage(event: PageEvent) { this.currentPage.set(event.pageIndex); this.load(); }

  getIcon(type: string): string {
    const map: Record<string, string> = {
      EVENT_PUBLISHED: 'event', EVENT_CANCELLED: 'event_busy',
      SIGNUP_CONFIRMED: 'check_circle', SIGNUP_WAITLISTED: 'hourglass_top', SIGNUP_PROMOTED: 'celebration',
      MEMBERSHIP_REQUESTED: 'person_add', MEMBERSHIP_ACCEPTED: 'how_to_reg', MEMBERSHIP_REJECTED: 'person_off',
      ONG_SUBMITTED: 'business', ONG_VALIDATED: 'verified', ONG_REJECTED: 'block',
      FEEDBACK_REQUESTED: 'rate_review', WELCOME: 'waving_hand',
    };
    return map[type] || 'notifications';
  }
}

