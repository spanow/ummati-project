import { Component, signal, OnInit } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDividerModule } from '@angular/material/divider';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { DatePipe, DecimalPipe } from '@angular/common';
import { EventService, EventDetail, FeedbackResponse } from '../../../core/services/event.service';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-event-detail',
  standalone: true,
  imports: [MatCardModule, MatButtonModule, MatIconModule, MatChipsModule, MatProgressBarModule,
    MatProgressSpinnerModule, MatDividerModule, MatSnackBarModule, RouterLink, DatePipe, DecimalPipe],
  template: `
    <div class="page-container">
      @if (loading()) {
        <div class="loading"><mat-spinner diameter="40" /></div>
      } @else if (event()) {
        <div class="event-header">
          <a mat-button [routerLink]="['/organizations', event()!.organizationSlug]" class="org-link">
            <mat-icon>business</mat-icon> {{ event()!.organizationName }}
          </a>
          <h1>{{ event()!.title }}</h1>
          <div class="event-badges">
            <mat-chip>{{ event()!.type }}</mat-chip>
            @if (event()!.online) { <mat-chip>🌐 En ligne</mat-chip> }
            <mat-chip [class]="'status-' + event()!.status.toLowerCase()">{{ event()!.status }}</mat-chip>
          </div>
        </div>

        <div class="event-content">
          <div class="main-col">
            <mat-card>
              <mat-card-content>
                <h3>Description</h3>
                <p class="description">{{ event()!.description }}</p>
                @if (event()!.objectives) {
                  <h3>Objectifs</h3>
                  <p>{{ event()!.objectives }}</p>
                }
              </mat-card-content>
            </mat-card>

            @if (event()!.requiredSkills.length > 0) {
              <mat-card>
                <mat-card-content>
                  <h3>Compétences recherchées</h3>
                  <div class="skills-list">
                    @for (skill of event()!.requiredSkills; track skill.id) {
                      <mat-chip>{{ skill.name }}</mat-chip>
                    }
                  </div>
                </mat-card-content>
              </mat-card>
            }

            @if (feedbacks().length > 0) {
              <mat-card>
                <mat-card-content>
                  <h3>Feedbacks
                    @if (avgRating()) {
                      <span class="avg-rating">⭐ {{ avgRating()! | number:'1.1-1' }}/5</span>
                    }
                  </h3>
                  @for (fb of feedbacks(); track fb.id) {
                    <div class="feedback-item">
                      <div class="fb-header">
                        <span class="fb-stars">{{ '⭐'.repeat(fb.rating) }}</span>
                        <span class="fb-author">{{ fb.anonymous ? 'Anonyme' : fb.userFirstName + ' ' + fb.userLastName }}</span>
                        <span class="fb-date">{{ fb.createdAt | date:'d MMM yyyy' }}</span>
                      </div>
                      @if (fb.comment) { <p class="fb-comment">{{ fb.comment }}</p> }
                    </div>
                  }
                </mat-card-content>
              </mat-card>
            }
          </div>

          <div class="side-col">
            <mat-card class="info-card">
              <mat-card-content>
                <div class="info-row">
                  <mat-icon>calendar_today</mat-icon>
                  <div>
                    <strong>Date</strong>
                    <p>{{ event()!.startDate | date:'EEEE d MMMM yyyy' }}</p>
                    <p>{{ event()!.startDate | date:'HH:mm' }} — {{ event()!.endDate | date:'HH:mm' }}</p>
                  </div>
                </div>
                <mat-divider />
                <div class="info-row">
                  <mat-icon>location_on</mat-icon>
                  <div>
                    <strong>Lieu</strong>
                    @if (event()!.online) {
                      <p>En ligne</p>
                    } @else {
                      <p>{{ event()!.locationName || event()!.locationAddress }}</p>
                      <p>{{ event()!.locationCity }} {{ event()!.locationZip }}</p>
                    }
                  </div>
                </div>
                @if (event()!.maxParticipants) {
                  <mat-divider />
                  <div class="info-row">
                    <mat-icon>people</mat-icon>
                    <div>
                      <strong>Places</strong>
                      <p>{{ event()!.registeredCount }}/{{ event()!.maxParticipants }} inscrits</p>
                      <mat-progress-bar mode="determinate"
                        [value]="(event()!.registeredCount / event()!.maxParticipants!) * 100" />
                      @if (event()!.availableSpots === 0) {
                        <p class="waitlist-info">Liste d'attente : {{ event()!.waitlistedCount }} personnes</p>
                      }
                    </div>
                  </div>
                }
                @if (event()!.minAge) {
                  <mat-divider />
                  <div class="info-row">
                    <mat-icon>cake</mat-icon>
                    <div><strong>Âge minimum</strong><p>{{ event()!.minAge }} ans</p></div>
                  </div>
                }
                @if (event()!.registrationDeadline) {
                  <mat-divider />
                  <div class="info-row">
                    <mat-icon>timer</mat-icon>
                    <div><strong>Date limite</strong><p>{{ event()!.registrationDeadline | date:'d MMM yyyy, HH:mm' }}</p></div>
                  </div>
                }
              </mat-card-content>
            </mat-card>

            @if (event()!.status === 'PUBLISHED') {
              <button mat-flat-button class="signup-btn" (click)="toggleSignup()" [disabled]="signingUp()">
                @if (isSignedUp()) {
                  <mat-icon>close</mat-icon> Se désinscrire
                } @else {
                  <mat-icon>how_to_reg</mat-icon> S'inscrire
                }
              </button>
            }

            @if (event()!.status === 'CANCELLED') {
              <mat-card class="cancel-card">
                <mat-card-content>
                  <h4>⚠️ Événement annulé</h4>
                  <p>{{ event()!.cancellationReason }}</p>
                </mat-card-content>
              </mat-card>
            }
          </div>
        </div>
      }
    </div>
  `,
  styles: [`
    .page-container { max-width: 1200px; margin: 0 auto; padding: 32px 24px; }
    .loading { display: flex; justify-content: center; padding: 80px 0; }
    .event-header { margin-bottom: 32px; }
    .org-link { color: #1976d2; margin-bottom: 8px; }
    .event-header h1 { font-size: 2rem; font-weight: 700; margin: 8px 0 16px; }
    .event-badges { display: flex; gap: 8px; flex-wrap: wrap; }
    .status-published { background: #e8f5e9 !important; color: #2e7d32 !important; }
    .status-cancelled { background: #ffebee !important; color: #c62828 !important; }
    .status-completed { background: #e3f2fd !important; color: #1565c0 !important; }
    .status-draft { background: #fff3e0 !important; color: #e65100 !important; }
    .event-content { display: grid; grid-template-columns: 1fr 360px; gap: 32px; align-items: start; }
    @media (max-width: 768px) { .event-content { grid-template-columns: 1fr; } }
    .main-col { display: flex; flex-direction: column; gap: 24px; }
    .main-col mat-card { border-radius: 12px; }
    .description { white-space: pre-line; line-height: 1.7; color: #444; }
    .skills-list { display: flex; gap: 8px; flex-wrap: wrap; }
    .side-col { display: flex; flex-direction: column; gap: 16px; }
    .info-card { border-radius: 12px; }
    .info-row { display: flex; gap: 16px; padding: 12px 0; }
    .info-row mat-icon { color: #1976d2; margin-top: 2px; }
    .info-row strong { display: block; font-size: 0.85rem; color: #666; text-transform: uppercase; letter-spacing: 0.5px; }
    .info-row p { margin: 4px 0 0; font-size: 0.95rem; }
    .waitlist-info { color: #ff9800; font-size: 0.85rem; margin-top: 4px; }
    .signup-btn { width: 100%; height: 48px; font-size: 1rem; }
    .cancel-card { border-radius: 12px; background: #fff3e0; }
    .cancel-card h4 { margin: 0 0 8px; }
    .avg-rating { font-size: 0.9rem; font-weight: 400; color: #ff9800; margin-left: 8px; }
    .feedback-item { padding: 12px 0; border-bottom: 1px solid #eee; }
    .feedback-item:last-child { border-bottom: none; }
    .fb-header { display: flex; gap: 12px; align-items: center; font-size: 0.85rem; }
    .fb-author { font-weight: 500; }
    .fb-date { color: #999; margin-left: auto; }
    .fb-comment { margin: 8px 0 0; color: #555; line-height: 1.5; }
  `],
})
export class EventDetailComponent implements OnInit {
  event = signal<EventDetail | null>(null);
  loading = signal(true);
  isSignedUp = signal(false);
  signingUp = signal(false);
  feedbacks = signal<FeedbackResponse[]>([]);
  avgRating = signal<number | null>(null);

  private eventId = '';

  constructor(
    private route: ActivatedRoute,
    private eventService: EventService,
    private authService: AuthService,
    private snackBar: MatSnackBar,
  ) {}

  ngOnInit() {
    this.eventId = this.route.snapshot.paramMap.get('id')!;
    this.loadEvent();
    this.loadFeedbacks();
    if (this.authService.isLoggedIn()) {
      this.eventService.getMySignup(this.eventId).subscribe({
        next: res => {
          const active = ['REGISTERED', 'WAITLISTED'];
          this.isSignedUp.set(active.includes(res.data.status));
        },
        error: () => {} // 404 = pas inscrit
      });
    }
  }

  loadEvent() {
    this.loading.set(true);
    this.eventService.getEvent(this.eventId).subscribe({
      next: res => {
        this.event.set(res.data);
        const s = res.data.currentUserSignupStatus;
        this.isSignedUp.set(s === 'REGISTERED' || s === 'WAITLISTED');
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  loadFeedbacks() {
    this.eventService.listFeedbacks(this.eventId).subscribe({
      next: res => {
        this.feedbacks.set(res.data.feedbacks.content);
        this.avgRating.set(res.data.averageRating);
      },
    });
  }

  toggleSignup() {
    this.signingUp.set(true);
    if (this.isSignedUp()) {
      this.eventService.cancelSignup(this.eventId).subscribe({
        next: () => {
          this.isSignedUp.set(false);
          this.signingUp.set(false);
          this.snackBar.open('Désinscription effectuée', 'OK', { duration: 3000 });
          this.loadEvent();
        },
        error: (err) => {
          this.signingUp.set(false);
          const msg = err?.error?.message || 'Erreur lors de la désinscription';
          this.snackBar.open(msg, 'OK', { duration: 4000 });
        },
      });
    } else {
      this.eventService.signup(this.eventId).subscribe({
        next: res => {
          this.isSignedUp.set(true);
          this.signingUp.set(false);
          const msg = res.data.status === 'WAITLISTED'
            ? 'Vous êtes sur la liste d\'attente' : 'Inscription confirmée !';
          this.snackBar.open(msg, 'OK', { duration: 3000 });
          this.loadEvent();
        },
        error: (err) => {
          this.signingUp.set(false);
          const msg = err?.error?.message || 'Erreur lors de l\'inscription';
          this.snackBar.open(msg, 'OK', { duration: 4000 });
        },
      });
    }
  }
}



