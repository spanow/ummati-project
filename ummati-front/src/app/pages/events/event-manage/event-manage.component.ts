import { Component, signal, OnInit } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTabsModule } from '@angular/material/tabs';
import { MatTableModule } from '@angular/material/table';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatChipsModule } from '@angular/material/chips';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { EventService, EventSummary, SignupResponse } from '../../../core/services/event.service';
import { OrganizationService } from '../../../core/services/organization.service';

@Component({
  selector: 'app-event-manage',
  standalone: true,
  imports: [MatCardModule, MatButtonModule, MatIconModule, MatTabsModule, MatTableModule,
    MatCheckboxModule, MatChipsModule, MatPaginatorModule, MatProgressSpinnerModule,
    MatSnackBarModule, MatDialogModule, RouterLink, DatePipe, FormsModule],
  template: `
    <div class="page-container">
      <header class="page-header">
        <h1>Gestion des événements</h1>
        <a mat-flat-button [routerLink]="['/organizations', orgId, 'events', 'new']">
          <mat-icon>add</mat-icon> Nouvel événement
        </a>
      </header>

      @if (loading()) {
        <div class="loading"><mat-spinner diameter="40" /></div>
      } @else {
        <mat-tab-group>
          <mat-tab label="Tous les événements">
            @if (events().length === 0) {
              <div class="empty">
                <p>Aucun événement pour cette organisation.</p>
              </div>
            } @else {
              @for (event of events(); track event.id) {
                <mat-card class="event-manage-card">
                  <mat-card-content>
                    <div class="em-row">
                      <div class="em-info">
                        <h3>{{ event.title }}</h3>
                        <div class="em-meta">
                          <mat-chip [class]="'status-' + event.status.toLowerCase()">{{ event.status }}</mat-chip>
                          <span>{{ event.startDate | date:'d MMM yyyy, HH:mm' }}</span>
                          <span>{{ event.registeredCount }}@if(event.maxParticipants){/{{ event.maxParticipants }}} inscrits</span>
                        </div>
                      </div>
                      <div class="em-actions">
                        @if (event.status === 'DRAFT') {
                          <button mat-stroked-button (click)="publish(event)">
                            <mat-icon>publish</mat-icon> Publier
                          </button>
                          <a mat-stroked-button [routerLink]="['/events', event.id, 'edit']">
                            <mat-icon>edit</mat-icon> Modifier
                          </a>
                        }
                        @if (event.status === 'PUBLISHED') {
                          <button mat-stroked-button color="warn" (click)="cancelEvent(event)">
                            <mat-icon>cancel</mat-icon> Annuler
                          </button>
                          <button mat-stroked-button (click)="viewSignups(event)">
                            <mat-icon>people</mat-icon> Inscrits
                          </button>
                        }
                        @if (event.status === 'COMPLETED') {
                          <button mat-stroked-button (click)="viewSignups(event)">
                            <mat-icon>people</mat-icon> Participants
                          </button>
                        }
                      </div>
                    </div>
                  </mat-card-content>
                </mat-card>
              }
            }
          </mat-tab>

          <mat-tab label="Inscrits" [disabled]="!selectedEvent()">
            @if (selectedEvent()) {
              <div class="signups-header">
                <h2>Inscrits — {{ selectedEvent()!.title }}</h2>
                <div class="signups-actions">
                  <button mat-stroked-button (click)="exportCsv()">
                    <mat-icon>download</mat-icon> Export CSV
                  </button>
                  @if (selectedEvent()!.status !== 'DRAFT') {
                    <button mat-flat-button (click)="markSelectedAttended()" [disabled]="selectedUserIds.length === 0">
                      <mat-icon>check_circle</mat-icon> Marquer présents
                    </button>
                  }
                </div>
              </div>
              @if (signupsLoading()) {
                <div class="loading"><mat-spinner diameter="30" /></div>
              } @else {
                <table class="signups-table">
                  <thead>
                    <tr>
                      <th><input type="checkbox" (change)="toggleAll($event)" /></th>
                      <th>Nom</th>
                      <th>Email</th>
                      <th>Statut</th>
                      <th>Inscrit le</th>
                    </tr>
                  </thead>
                  <tbody>
                    @for (s of signups(); track s.id) {
                      <tr>
                        <td><input type="checkbox" [checked]="selectedUserIds.includes(s.userId)"
                                   (change)="toggleUser(s.userId)" /></td>
                        <td>{{ s.userFirstName }} {{ s.userLastName }}</td>
                        <td>{{ s.userEmail }}</td>
                        <td><mat-chip [class]="'signup-' + s.status.toLowerCase()">{{ s.status }}</mat-chip></td>
                        <td>{{ s.registeredAt | date:'d MMM yyyy' }}</td>
                      </tr>
                    }
                  </tbody>
                </table>
                <mat-paginator [length]="signupTotal()" [pageSize]="20" (page)="onSignupPage($event)" />
              }
            }
          </mat-tab>
        </mat-tab-group>
      }
    </div>
  `,
  styles: [`
    .page-container { max-width: 1100px; margin: 0 auto; padding: 32px 24px; }
    .page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 24px; }
    .page-header h1 { font-size: 1.8rem; font-weight: 600; margin: 0; }
    .loading { display: flex; justify-content: center; padding: 40px; }
    .empty { text-align: center; padding: 40px; color: #888; }
    .event-manage-card { margin: 12px 0; border-radius: 10px; }
    .em-row { display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 12px; }
    .em-info h3 { margin: 0 0 8px; font-weight: 600; }
    .em-meta { display: flex; gap: 12px; align-items: center; font-size: 0.85rem; color: #666; }
    .em-actions { display: flex; gap: 8px; }
    .status-draft { background: #fff3e0 !important; color: #e65100 !important; }
    .status-published { background: #e8f5e9 !important; color: #2e7d32 !important; }
    .status-cancelled { background: #ffebee !important; color: #c62828 !important; }
    .status-completed { background: #e3f2fd !important; color: #1565c0 !important; }
    .signup-registered { background: #e8f5e9 !important; color: #2e7d32 !important; }
    .signup-waitlisted { background: #fff3e0 !important; color: #e65100 !important; }
    .signup-attended { background: #e3f2fd !important; color: #1565c0 !important; }
    .signup-cancelled { background: #ffebee !important; color: #c62828 !important; }
    .signups-header { display: flex; justify-content: space-between; align-items: center; margin: 16px 0; flex-wrap: wrap; gap: 12px; }
    .signups-header h2 { font-size: 1.2rem; font-weight: 600; margin: 0; }
    .signups-actions { display: flex; gap: 8px; }
    .signups-table { width: 100%; border-collapse: collapse; }
    .signups-table th, .signups-table td { padding: 10px 12px; text-align: left; border-bottom: 1px solid #eee; }
    .signups-table th { font-size: 0.8rem; color: #888; text-transform: uppercase; }
  `],
})
export class EventManageComponent implements OnInit {
  orgId = '';
  events = signal<EventSummary[]>([]);
  loading = signal(true);
  selectedEvent = signal<EventSummary | null>(null);
  signups = signal<SignupResponse[]>([]);
  signupsLoading = signal(false);
  signupTotal = signal(0);
  selectedUserIds: string[] = [];
  cancelReason = '';

  constructor(
    private route: ActivatedRoute,
    private eventService: EventService,
    private snackBar: MatSnackBar,
  ) {}

  ngOnInit() {
    this.orgId = this.route.snapshot.paramMap.get('orgId') || '';
    this.loadEvents();
  }

  loadEvents() {
    this.loading.set(true);
    this.eventService.listOrgEvents(this.orgId).subscribe({
      next: res => { this.events.set(res.data.content); this.loading.set(false); },
      error: () => this.loading.set(false),
    });
  }

  publish(event: EventSummary) {
    this.eventService.changeStatus(event.id, { status: 'PUBLISH' }).subscribe({
      next: () => { this.snackBar.open('Événement publié !', 'OK', { duration: 3000 }); this.loadEvents(); },
      error: err => this.snackBar.open(err.error?.message || 'Erreur', 'OK', { duration: 3000 }),
    });
  }

  cancelEvent(event: EventSummary) {
    const reason = prompt('Motif d\'annulation (min 10 caractères) :');
    if (!reason) return;
    this.eventService.changeStatus(event.id, { status: 'CANCEL', reason }).subscribe({
      next: () => { this.snackBar.open('Événement annulé', 'OK', { duration: 3000 }); this.loadEvents(); },
      error: err => this.snackBar.open(err.error?.message || 'Erreur', 'OK', { duration: 3000 }),
    });
  }

  viewSignups(event: EventSummary) {
    this.selectedEvent.set(event);
    this.loadSignups(0);
  }

  loadSignups(page: number) {
    this.signupsLoading.set(true);
    this.eventService.listSignups(this.selectedEvent()!.id, page).subscribe({
      next: res => {
        this.signups.set(res.data.content);
        this.signupTotal.set(res.data.totalElements);
        this.signupsLoading.set(false);
      },
      error: () => this.signupsLoading.set(false),
    });
  }

  onSignupPage(event: PageEvent) { this.loadSignups(event.pageIndex); }

  toggleUser(userId: string) {
    const idx = this.selectedUserIds.indexOf(userId);
    if (idx >= 0) this.selectedUserIds.splice(idx, 1);
    else this.selectedUserIds.push(userId);
  }

  toggleAll(event: any) {
    if (event.target.checked) {
      this.selectedUserIds = this.signups().map(s => s.userId);
    } else {
      this.selectedUserIds = [];
    }
  }

  markSelectedAttended() {
    this.eventService.markAttendance(this.selectedEvent()!.id, this.selectedUserIds).subscribe({
      next: () => {
        this.snackBar.open('Présences marquées', 'OK', { duration: 3000 });
        this.selectedUserIds = [];
        this.loadSignups(0);
      },
      error: err => this.snackBar.open(err.error?.message || 'Erreur', 'OK', { duration: 3000 }),
    });
  }

  exportCsv() {
    this.eventService.exportSignupsCsv(this.selectedEvent()!.id).subscribe(blob => {
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = 'inscrits.csv';
      a.click();
      window.URL.revokeObjectURL(url);
    });
  }
}

