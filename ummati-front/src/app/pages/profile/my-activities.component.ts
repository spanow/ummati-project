import { Component, signal, OnInit } from '@angular/core';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTabsModule } from '@angular/material/tabs';
import { MatChipsModule } from '@angular/material/chips';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { RouterLink } from '@angular/router';
import { DatePipe } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';

interface Membership {
  id: string; organizationId: string; organizationName?: string;
  role: string; status: string; joinedAt: string;
}

interface Signup {
  id: string; eventId: string; eventTitle?: string;
  status: string; registeredAt: string;
}

@Component({
  selector: 'app-my-activities',
  standalone: true,
  imports: [MatCardModule, MatButtonModule, MatIconModule, MatTabsModule, MatChipsModule,
    MatPaginatorModule, MatProgressSpinnerModule, RouterLink, DatePipe],
  template: `
    <div class="page-container">
      <h1>Mes activités</h1>

      <mat-tab-group>
        <!-- Mes organisations -->
        <mat-tab label="Mes organisations ({{ membershipTotal() }})">
          @if (membershipsLoading()) {
            <div class="loading"><mat-spinner diameter="30" /></div>
          } @else if (memberships().length === 0) {
            <div class="empty">
              <mat-icon>groups_off</mat-icon>
              <p>Vous n'êtes membre d'aucune organisation.</p>
              <a mat-flat-button routerLink="/organizations">Explorer les ONG</a>
            </div>
          } @else {
            <div class="card-list">
              @for (m of memberships(); track m.id) {
                <mat-card class="activity-card">
                  <mat-card-content>
                    <div class="card-row">
                      <mat-icon>business</mat-icon>
                      <div class="info">
                        <strong>{{ m.organizationId }}</strong>
                        <span class="meta">Membre depuis {{ m.joinedAt | date:'d MMM yyyy' }}</span>
                      </div>
                      <mat-chip [class]="'role-' + m.role.toLowerCase()">{{ m.role }}</mat-chip>
                    </div>
                  </mat-card-content>
                </mat-card>
              }
            </div>
            <mat-paginator [length]="membershipTotal()" [pageSize]="10" (page)="onMembershipPage($event)" />
          }
        </mat-tab>

        <!-- Mes inscriptions -->
        <mat-tab label="Mes inscriptions ({{ signupTotal() }})">
          @if (signupsLoading()) {
            <div class="loading"><mat-spinner diameter="30" /></div>
          } @else if (signups().length === 0) {
            <div class="empty">
              <mat-icon>event_busy</mat-icon>
              <p>Vous n'avez aucune inscription à un événement.</p>
              <a mat-flat-button routerLink="/events">Voir les événements</a>
            </div>
          } @else {
            <div class="card-list">
              @for (s of signups(); track s.id) {
                <mat-card class="activity-card" [routerLink]="['/events', s.eventId]">
                  <mat-card-content>
                    <div class="card-row">
                      <mat-icon>event</mat-icon>
                      <div class="info">
                        <strong>{{ s.eventId }}</strong>
                        <span class="meta">Inscrit le {{ s.registeredAt | date:'d MMM yyyy' }}</span>
                      </div>
                      <mat-chip [class]="'status-' + s.status.toLowerCase()">{{ s.status }}</mat-chip>
                    </div>
                  </mat-card-content>
                </mat-card>
              }
            </div>
            <mat-paginator [length]="signupTotal()" [pageSize]="10" (page)="onSignupPage($event)" />
          }
        </mat-tab>
      </mat-tab-group>
    </div>
  `,
  styles: [`
    .page-container { max-width: 900px; margin: 0 auto; padding: 32px 24px; }
    h1 { font-size: 1.8rem; font-weight: 600; margin-bottom: 24px; }
    .loading { display: flex; justify-content: center; padding: 40px; }
    .empty { text-align: center; padding: 48px 24px; color: #888; }
    .empty mat-icon { font-size: 48px; width: 48px; height: 48px; color: #ccc; display: block; margin: 0 auto 12px; }
    .card-list { display: flex; flex-direction: column; gap: 10px; padding: 16px 0; }
    .activity-card { border-radius: 10px; cursor: pointer; }
    .activity-card:hover { box-shadow: 0 4px 16px rgba(0,0,0,0.1); }
    .card-row { display: flex; align-items: center; gap: 16px; }
    .card-row mat-icon { color: #1976d2; }
    .info { flex: 1; }
    .info strong { display: block; }
    .meta { color: #888; font-size: 0.8rem; }
    .role-admin { background: #e3f2fd !important; color: #1565c0 !important; }
    .role-member { background: #e8f5e9 !important; color: #2e7d32 !important; }
    .status-registered { background: #e8f5e9 !important; color: #2e7d32 !important; }
    .status-waitlisted { background: #fff3e0 !important; color: #e65100 !important; }
    .status-attended { background: #e3f2fd !important; color: #1565c0 !important; }
    .status-cancelled { background: #ffebee !important; color: #c62828 !important; }
  `],
})
export class MyActivitiesComponent implements OnInit {
  memberships = signal<Membership[]>([]);
  membershipsLoading = signal(true);
  membershipTotal = signal(0);
  signups = signal<Signup[]>([]);
  signupsLoading = signal(true);
  signupTotal = signal(0);

  constructor(private http: HttpClient) {}

  ngOnInit() { this.loadMemberships(); this.loadSignups(); }

  loadMemberships(page = 0) {
    this.membershipsLoading.set(true);
    this.http.get<any>(`${environment.apiUrl}/profile/memberships`, { params: { page, size: 10 } }).subscribe({
      next: res => {
        this.memberships.set(res.data.content);
        this.membershipTotal.set(res.data.totalElements);
        this.membershipsLoading.set(false);
      },
      error: () => this.membershipsLoading.set(false),
    });
  }

  loadSignups(page = 0) {
    this.signupsLoading.set(true);
    this.http.get<any>(`${environment.apiUrl}/profile/signups`, { params: { page, size: 10 } }).subscribe({
      next: res => {
        this.signups.set(res.data.content);
        this.signupTotal.set(res.data.totalElements);
        this.signupsLoading.set(false);
      },
      error: () => this.signupsLoading.set(false),
    });
  }

  onMembershipPage(e: PageEvent) { this.loadMemberships(e.pageIndex); }
  onSignupPage(e: PageEvent) { this.loadSignups(e.pageIndex); }
}

