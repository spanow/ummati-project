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
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatMenuModule } from '@angular/material/menu';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { FormsModule } from '@angular/forms';
import { DatePipe, DecimalPipe } from '@angular/common';
import { EventService, EventDetail, FeedbackResponse } from '../../../core/services/event.service';
import { EventAnnouncementService, AnnouncementResponse } from '../../../core/services/event-announcement.service';
import { EventCommentService, CommentResponse } from '../../../core/services/event-comment.service';
import { AuthService } from '../../../core/services/auth.service';
import { ReportDialogComponent } from '../../../shared/components/report-dialog/report-dialog.component';
import { StarRatingComponent } from '../../../shared/components/star-rating/star-rating.component';
import { TPipe } from '../../../shared/pipes/t.pipe';
import { LocationPickerComponent } from '../../../shared/components/location-picker/location-picker.component';

@Component({
  selector: 'app-event-detail',
  standalone: true,
  imports: [MatCardModule, MatButtonModule, MatIconModule, MatChipsModule, MatProgressBarModule,
    MatProgressSpinnerModule, MatDividerModule, MatSnackBarModule, MatFormFieldModule, MatInputModule,
    MatMenuModule, MatDialogModule, MatCheckboxModule, FormsModule, RouterLink, DatePipe, DecimalPipe,
    StarRatingComponent, TPipe, LocationPickerComponent],
  template: `
    <div class="page">
      @if (loading()) {
        <div class="state-center"><mat-spinner diameter="40" /></div>
      } @else if (event()) {
        <div class="event-header">
          <div class="header-top">
            <a mat-button [routerLink]="['/organizations', event()!.organizationSlug]" class="org-link">
              <mat-icon>business</mat-icon> {{ event()!.organizationName }}
            </a>
            @if (isLoggedIn()) {
              <button mat-icon-button [matMenuTriggerFor]="eventMenu" [attr.aria-label]="'Plus d\\'options' | t">
                <mat-icon>more_vert</mat-icon>
              </button>
              <mat-menu #eventMenu="matMenu">
                <button mat-menu-item (click)="reportEvent()">
                  <mat-icon>flag</mat-icon> {{ 'Signaler cet événement' | t }}
                </button>
              </mat-menu>
            }
          </div>
          <h1>{{ event()!.title }}</h1>
          <div class="event-badges">
            <mat-chip>{{ event()!.type }}</mat-chip>
            @if (event()!.online) { <mat-chip>🌐 {{ 'En ligne' | t }}</mat-chip> }
            <mat-chip [class]="'status-' + event()!.status.toLowerCase()">{{ event()!.status }}</mat-chip>
          </div>
          <div class="event-quick-actions">
            <button mat-stroked-button type="button" (click)="addToCalendar()">
              <mat-icon>calendar_add_on</mat-icon> {{ 'Ajouter à mon agenda' | t }}
            </button>
            <button mat-stroked-button type="button" (click)="shareEvent()">
              <mat-icon>share</mat-icon> {{ 'Partager' | t }}
            </button>
          </div>
        </div>

        <div class="event-content">
          <div class="main-col">
            <mat-card>
              <mat-card-content>
                <h3>{{ 'Description' | t }}</h3>
                <p class="description">{{ event()!.description }}</p>
                @if (event()!.objectives) {
                  <h3>{{ 'Objectifs' | t }}</h3>
                  <p>{{ event()!.objectives }}</p>
                }
              </mat-card-content>
            </mat-card>

            @if (event()!.requiredSkills.length > 0) {
              <mat-card>
                <mat-card-content>
                  <h3>{{ 'Compétences recherchées' | t }}</h3>
                  <div class="skills-list">
                    @for (skill of event()!.requiredSkills; track skill.id) {
                      <mat-chip>{{ skill.name }}</mat-chip>
                    }
                  </div>
                </mat-card-content>
              </mat-card>
            }

            @if (announcements().length > 0) {
              <mat-card class="announcements-card">
                <mat-card-content>
                  <h3>📢 {{ 'Annonces de l\\'organisateur' | t }}</h3>
                  @for (ann of announcements(); track ann.id) {
                    <div class="announcement-item" [class.pinned]="ann.pinned">
                      @if (ann.pinned) { <span class="pin-badge">📌 {{ 'Épinglée' | t }}</span> }
                      <p class="ann-content">{{ ann.content }}</p>
                      <span class="ann-meta">{{ ann.authorFirstName }} {{ ann.authorLastName }} · {{ ann.createdAt | date:'d MMM yyyy, HH:mm' }}</span>
                    </div>
                  }
                </mat-card-content>
              </mat-card>
            }

            <mat-card class="comments-card">
              <mat-card-content>
                <h3>💬 {{ 'Discussion' | t }} ({{ totalComments() }})</h3>

                @if (isLoggedIn() && isParticipant()) {
                  <div class="comment-form">
                    <mat-form-field appearance="outline" class="comment-input">
                      <mat-label>{{ 'Votre commentaire' | t }}</mat-label>
                      <textarea matInput [(ngModel)]="newComment" rows="2" maxlength="500"
                        [placeholder]="'Partagez vos questions ou infos pratiques...' | t"></textarea>
                    </mat-form-field>
                    <button mat-flat-button color="primary" (click)="postComment()"
                        [disabled]="newComment.trim().length < 2 || postingComment()">
                      {{ 'Publier' | t }}
                    </button>
                  </div>
                } @else if (isLoggedIn() && !isParticipant()) {
                  <p class="comment-hint">{{ 'Inscrivez-vous à l\\'événement pour commenter.' | t }}</p>
                } @else {
                  <p class="comment-hint"><a routerLink="/login">{{ 'Connectez-vous' | t }}</a> {{ 'et inscrivez-vous pour commenter.' | t }}</p>
                }

                @for (c of comments(); track c.id) {
                  <div class="comment-item">
                    <div class="comment-header">
                      <span class="comment-author">{{ c.authorFirstName }} {{ c.authorLastName }}</span>
                      <span class="comment-date">{{ c.createdAt | date:'d MMM yyyy, HH:mm' }}</span>
                      @if (canDeleteComment(c)) {
                        <button mat-icon-button class="delete-btn" (click)="deleteComment(c.id)" [title]="'Supprimer' | t">
                          <mat-icon>delete_outline</mat-icon>
                        </button>
                      }
                    </div>
                    <p class="comment-content">{{ c.content }}</p>
                  </div>
                }

                @if (comments().length === 0 && !loadingComments()) {
                  <p class="no-comments">{{ 'Soyez le premier à commenter !' | t }}</p>
                }
              </mat-card-content>
            </mat-card>

            @if (canGiveFeedback()) {
              <mat-card class="feedback-form-card">
                <mat-card-content>
                  <h3>⭐ {{ 'Donner mon avis' | t }}</h3>
                  <p class="feedback-hint">{{ 'Vous avez participé à cet événement — partagez votre expérience !' | t }}</p>
                  <div class="feedback-form">
                    <app-star-rating [value]="feedbackRating" (valueChange)="feedbackRating = $event" />
                    <mat-form-field appearance="outline" class="full-width">
                      <mat-label>{{ 'Commentaire (optionnel)' | t }}</mat-label>
                      <textarea matInput [(ngModel)]="feedbackComment" rows="3" maxlength="1000"
                                [placeholder]="'Qu\\'avez-vous pensé de cet événement ?' | t"></textarea>
                    </mat-form-field>
                    <div class="feedback-actions">
                      <mat-checkbox [(ngModel)]="feedbackAnonymous">{{ 'Publier anonymement' | t }}</mat-checkbox>
                      <button mat-flat-button color="primary" (click)="submitFeedback()"
                              [disabled]="feedbackRating === 0 || submittingFeedback()">
                        {{ (submittingFeedback() ? 'Envoi…' : 'Publier mon avis') | t }}
                      </button>
                    </div>
                  </div>
                </mat-card-content>
              </mat-card>
            }

            @if (feedbacks().length > 0) {
              <mat-card>
                <mat-card-content>
                  <h3>{{ 'Feedbacks' | t }}
                    @if (avgRating()) {
                      <span class="avg-rating">⭐ {{ avgRating()! | number:'1.1-1' }}/5</span>
                    }
                  </h3>
                  @for (fb of feedbacks(); track fb.id) {
                    <div class="feedback-item">
                      <div class="fb-header">
                        <span class="fb-stars">{{ '⭐'.repeat(fb.rating) }}</span>
                        <span class="fb-author">{{ fb.anonymous ? ('Anonyme' | t) : fb.userFirstName + ' ' + fb.userLastName }}</span>
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
                @if (!isMulti()) {
                  <div class="info-row">
                    <mat-icon>calendar_today</mat-icon>
                    <div>
                      <strong>{{ 'Date' | t }}</strong>
                      <p>{{ event()!.startDate | date:'EEEE d MMMM yyyy' }}</p>
                      <p>{{ event()!.startDate | date:'HH:mm' }} — {{ event()!.endDate | date:'HH:mm' }}</p>
                    </div>
                  </div>
                  <mat-divider />
                }
                <div class="info-row">
                  <mat-icon>location_on</mat-icon>
                  <div>
                    <strong>{{ 'Lieu' | t }}</strong>
                    @if (event()!.online) {
                      <p>{{ 'En ligne' | t }}</p>
                    } @else {
                      <p>{{ event()!.locationName || event()!.locationAddress }}</p>
                      <p>{{ event()!.locationCity }} {{ event()!.locationZip }}</p>
                    }
                  </div>
                </div>
                @if (!isMulti() && event()!.maxParticipants) {
                  <mat-divider />
                  <div class="info-row">
                    <mat-icon>people</mat-icon>
                    <div>
                      <strong>{{ 'Places' | t }}</strong>
                      <p>{{ event()!.registeredCount }}/{{ event()!.maxParticipants }} {{ 'inscrits' | t }}</p>
                      <mat-progress-bar mode="determinate"
                        [value]="(event()!.registeredCount / event()!.maxParticipants!) * 100" />
                      @if (event()!.availableSpots === 0) {
                        <p class="waitlist-info">{{ 'Liste d\\'attente :' | t }} {{ event()!.waitlistedCount }} {{ 'personnes' | t }}</p>
                      }
                    </div>
                  </div>
                }
                @if (event()!.minAge) {
                  <mat-divider />
                  <div class="info-row">
                    <mat-icon>cake</mat-icon>
                    <div><strong>{{ 'Âge minimum' | t }}</strong><p>{{ event()!.minAge }} {{ 'ans' | t }}</p></div>
                  </div>
                }
                @if (event()!.registrationDeadline) {
                  <mat-divider />
                  <div class="info-row">
                    <mat-icon>timer</mat-icon>
                    <div><strong>{{ 'Date limite' | t }}</strong><p>{{ event()!.registrationDeadline | date:'d MMM yyyy, HH:mm' }}</p></div>
                  </div>
                }
              </mat-card-content>
            </mat-card>

            @if (isMulti()) {
              <mat-card class="occurrences-card">
                <mat-card-content>
                  <h3>{{ 'Créneaux' | t }} ({{ event()!.occurrences.length }})</h3>
                  @for (occ of event()!.occurrences; track occ.id) {
                    <div class="occ-item" [class.occ-cancelled]="occ.status === 'CANCELLED'">
                      <div class="occ-info">
                        @if (occ.label) { <span class="occ-label">{{ occ.label }}</span> }
                        <span class="occ-date">{{ occ.startDate | date:'EEE d MMM yyyy' }}</span>
                        <span class="occ-time">{{ occ.startDate | date:'HH:mm' }} — {{ occ.endDate | date:'HH:mm' }}</span>
                        @if (occ.maxParticipants) {
                          <span class="occ-places">{{ occ.registeredCount }}/{{ occ.maxParticipants }} {{ 'inscrits' | t }}</span>
                        }
                      </div>
                      @if (occ.status === 'CANCELLED') {
                        <span class="occ-badge cancelled">{{ 'Annulé' | t }}</span>
                      } @else if (occ.status === 'COMPLETED') {
                        <span class="occ-badge">{{ 'Terminé' | t }}</span>
                      } @else if (isLoggedIn()) {
                        @if (occ.currentUserSignupStatus === 'REGISTERED' || occ.currentUserSignupStatus === 'WAITLISTED') {
                          <button mat-stroked-button (click)="cancelOccurrence(occ.id)" [disabled]="signingUp()">
                            <mat-icon>close</mat-icon>
                            {{ (occ.currentUserSignupStatus === 'WAITLISTED' ? 'Quitter' : 'Se désinscrire') | t }}
                          </button>
                        } @else {
                          <button mat-flat-button color="primary" (click)="signupOccurrence(occ.id)"
                                  [disabled]="signingUp() || occ.status !== 'PUBLISHED'">
                            <mat-icon>how_to_reg</mat-icon> {{ 'S\\'inscrire' | t }}
                          </button>
                        }
                      }
                    </div>
                  }
                </mat-card-content>
              </mat-card>
            }

            @if (!event()!.online && event()!.locationLat && event()!.locationLng) {
              <app-location-picker [editable]="false"
                [lat]="+event()!.locationLat" [lng]="+event()!.locationLng" />
            }

            @if (!isMulti() && event()!.status === 'PUBLISHED') {
              <button mat-flat-button class="signup-btn" (click)="toggleSignup()" [disabled]="signingUp()">
                <mat-icon>{{ isSignedUp() ? 'close' : 'how_to_reg' }}</mat-icon>
                {{ (isSignedUp() ? 'Se désinscrire' : 'S\\'inscrire') | t }}
              </button>
            }

            @if (event()!.status === 'CANCELLED') {
              <mat-card class="cancel-card">
                <mat-card-content>
                  <h4>⚠️ {{ 'Événement annulé' | t }}</h4>
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
    .event-header { margin-bottom: 32px; }
    .header-top { display: flex; align-items: center; justify-content: space-between; }
    .org-link { color: var(--brand-primary); margin-bottom: 8px; }
    .event-header h1 { font-size: 2rem; font-weight: 800; margin: 8px 0 16px; letter-spacing: -0.02em; }
    .event-badges { display: flex; gap: 8px; flex-wrap: wrap; }
    .event-quick-actions { display: flex; gap: 10px; margin-top: 16px; flex-wrap: wrap; }
    .event-quick-actions mat-icon { font-size: 18px; width: 18px; height: 18px; }
    .status-published { background: var(--brand-success-soft) !important; color: var(--brand-success) !important; }
    .status-cancelled { background: var(--brand-danger-soft) !important; color: var(--brand-danger) !important; }
    .status-completed { background: var(--brand-primary-100) !important; color: var(--brand-primary-dark) !important; }
    .status-draft { background: var(--brand-accent-soft) !important; color: var(--brand-warn) !important; }
    .event-content { display: grid; grid-template-columns: 1fr 360px; gap: 32px; align-items: start; }
    @media (max-width: 768px) { .event-content { grid-template-columns: 1fr; } }
    .main-col { display: flex; flex-direction: column; gap: 24px; }
    .description { white-space: pre-line; line-height: 1.7; color: var(--brand-text); }
    .skills-list { display: flex; gap: 8px; flex-wrap: wrap; }
    .side-col { display: flex; flex-direction: column; gap: 16px; }
    .info-row { display: flex; gap: 16px; padding: 12px 0; }
    .info-row mat-icon { color: var(--brand-primary); margin-top: 2px; }
    .info-row strong { display: block; font-size: 0.85rem; color: var(--brand-text-soft); text-transform: uppercase; letter-spacing: 0.5px; }
    .info-row p { margin: 4px 0 0; font-size: 0.95rem; }
    .waitlist-info { color: var(--brand-accent); font-size: 0.85rem; margin-top: 4px; }
    .signup-btn { width: 100%; height: 48px; font-size: 1rem; }
    .cancel-card { background: var(--brand-accent-soft); }
    .cancel-card h4 { margin: 0 0 8px; }
    .avg-rating { font-size: 0.9rem; font-weight: 400; color: var(--brand-accent); margin-left: 8px; }
    .feedback-item { padding: 12px 0; border-bottom: 1px solid var(--brand-border); }
    .feedback-item:last-child { border-bottom: none; }
    .fb-header { display: flex; gap: 12px; align-items: center; font-size: 0.85rem; }
    .fb-author { font-weight: 500; }
    .fb-date { color: var(--brand-text-faint); margin-left: auto; }
    .fb-comment { margin: 8px 0 0; color: var(--brand-text); line-height: 1.5; }
    .feedback-form-card { border-left: 4px solid var(--brand-accent); }
    .feedback-hint { color: var(--brand-text-soft); font-size: 0.9rem; margin: 0 0 16px; }
    .feedback-form { display: flex; flex-direction: column; gap: 12px; }
    .feedback-actions { display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 12px; }
    .full-width { width: 100%; }
    .announcements-card { border-left: 4px solid var(--brand-primary); }
    .announcement-item { padding: 12px 0; border-bottom: 1px solid var(--brand-border); }
    .announcement-item:last-child { border-bottom: none; }
    .announcement-item.pinned { background: var(--brand-primary-soft); border-radius: var(--radius-sm); padding: 12px; margin-bottom: 8px; }
    .pin-badge { font-size: 0.75rem; color: var(--brand-primary); font-weight: 700; display: block; margin-bottom: 4px; }
    .ann-content { margin: 4px 0; white-space: pre-line; line-height: 1.6; }
    .ann-meta { font-size: 0.8rem; color: var(--brand-text-faint); }
    .comment-form { display: flex; gap: 12px; align-items: flex-start; margin-bottom: 20px; }
    .comment-input { flex: 1; }
    .comment-hint { font-size: 0.9rem; color: var(--brand-text-soft); margin-bottom: 16px; }
    .comment-hint a { color: var(--brand-primary); }
    .comment-item { padding: 12px 0; border-bottom: 1px solid var(--brand-border); }
    .comment-item:last-child { border-bottom: none; }
    .comment-header { display: flex; align-items: center; gap: 8px; margin-bottom: 4px; }
    .comment-author { font-weight: 700; font-size: 0.9rem; color: var(--brand-ink); }
    .comment-date { color: var(--brand-text-faint); font-size: 0.8rem; }
    .delete-btn { margin-left: auto; color: var(--brand-text-faint); width: 32px; height: 32px; line-height: 32px; }
    .delete-btn mat-icon { font-size: 18px; }
    .comment-content { margin: 0; line-height: 1.6; white-space: pre-line; color: var(--brand-text); }
    .no-comments { color: var(--brand-text-faint); font-style: italic; text-align: center; padding: 24px 0; }
    .occ-item { display: flex; justify-content: space-between; align-items: center; gap: 12px; padding: 12px 0; border-bottom: 1px solid var(--brand-border); }
    .occ-item:last-child { border-bottom: none; }
    .occ-item.occ-cancelled { opacity: 0.55; }
    .occ-info { display: flex; flex-direction: column; gap: 2px; }
    .occ-label { font-weight: 700; font-size: 0.9rem; }
    .occ-date { font-weight: 500; font-size: 0.9rem; text-transform: capitalize; }
    .occ-time { font-size: 0.82rem; color: var(--brand-text-soft); }
    .occ-places { font-size: 0.8rem; color: var(--brand-primary); }
    .occ-badge { font-size: 0.78rem; padding: 3px 8px; border-radius: 6px; background: var(--brand-surface-2); color: var(--brand-text-soft); white-space: nowrap; border: 1px solid var(--brand-border); }
    .occ-badge.cancelled { background: var(--brand-danger-soft); color: var(--brand-danger); border-color: transparent; }
    .occ-item button mat-icon { font-size: 18px; width: 18px; height: 18px; }
  `],
})
export class EventDetailComponent implements OnInit {
  event = signal<EventDetail | null>(null);
  loading = signal(true);
  isSignedUp = signal(false);
  signingUp = signal(false);
  feedbacks = signal<FeedbackResponse[]>([]);
  avgRating = signal<number | null>(null);
  announcements = signal<AnnouncementResponse[]>([]);
  comments = signal<CommentResponse[]>([]);
  totalComments = signal(0);
  loadingComments = signal(false);
  isParticipant = signal(false);
  newComment = '';
  postingComment = signal(false);
  mySignupStatus = signal<string | null>(null);
  feedbackRating = 0;
  feedbackComment = '';
  feedbackAnonymous = false;
  submittingFeedback = signal(false);
  feedbackSubmitted = signal(false);

  get isLoggedIn() { return this.authService.isLoggedIn; }

  private eventId = '';
  private currentUserId = signal<string | null>(null);

  constructor(
    private route: ActivatedRoute,
    private eventService: EventService,
    private announcementService: EventAnnouncementService,
    private commentService: EventCommentService,
    private authService: AuthService,
    private snackBar: MatSnackBar,
    private dialog: MatDialog,
  ) {}

  ngOnInit() {
    this.eventId = this.route.snapshot.paramMap.get('id')!;
    const user = this.authService.user();
    this.currentUserId.set(user?.id ?? null);
    this.loadEvent();
    this.loadFeedbacks();
    this.loadAnnouncements();
    this.loadComments();
    if (this.authService.isLoggedIn()) {
      this.eventService.getMySignup(this.eventId).subscribe({
        next: res => {
          const active = ['REGISTERED', 'WAITLISTED', 'ATTENDED'];
          const isActive = active.includes(res.data.status);
          this.isSignedUp.set(res.data.status === 'REGISTERED' || res.data.status === 'WAITLISTED');
          this.isParticipant.set(isActive);
          this.mySignupStatus.set(res.data.status);
        },
        error: () => {}
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

  loadAnnouncements() {
    this.announcementService.list(this.eventId).subscribe({
      next: res => this.announcements.set(res.data),
      error: () => {},
    });
  }

  loadComments() {
    this.loadingComments.set(true);
    this.commentService.list(this.eventId).subscribe({
      next: res => {
        this.comments.set(res.data.content);
        this.totalComments.set(res.data.totalElements);
        this.loadingComments.set(false);
      },
      error: () => this.loadingComments.set(false),
    });
  }

  postComment() {
    const content = this.newComment.trim();
    if (content.length < 2) return;
    this.postingComment.set(true);
    this.commentService.create(this.eventId, content).subscribe({
      next: res => {
        this.comments.update(list => [...list, res.data]);
        this.totalComments.update(n => n + 1);
        this.newComment = '';
        this.postingComment.set(false);
      },
      error: err => {
        this.postingComment.set(false);
        this.snackBar.open(err?.error?.message || 'Erreur lors de la publication', 'OK', { duration: 3000 });
      },
    });
  }

  deleteComment(commentId: string) {
    this.commentService.delete(this.eventId, commentId).subscribe({
      next: () => {
        this.comments.update(list => list.filter(c => c.id !== commentId));
        this.totalComments.update(n => n - 1);
      },
      error: () => this.snackBar.open('Impossible de supprimer ce commentaire', 'OK', { duration: 3000 }),
    });
  }

  canDeleteComment(comment: CommentResponse): boolean {
    const uid = this.currentUserId();
    if (!uid) return false;
    return comment.authorId === uid;
  }

  loadFeedbacks() {
    this.eventService.listFeedbacks(this.eventId).subscribe({
      next: res => {
        this.feedbacks.set(res.data.feedbacks.content);
        this.avgRating.set(res.data.averageRating);
      },
    });
  }

  canGiveFeedback(): boolean {
    return this.event()?.status === 'COMPLETED'
      && this.mySignupStatus() === 'ATTENDED'
      && !this.feedbackSubmitted();
  }

  submitFeedback() {
    if (this.feedbackRating === 0) return;
    this.submittingFeedback.set(true);
    this.eventService.createFeedback(this.eventId, {
      rating: this.feedbackRating,
      comment: this.feedbackComment.trim() || undefined,
      anonymous: this.feedbackAnonymous,
    }).subscribe({
      next: () => {
        this.submittingFeedback.set(false);
        this.feedbackSubmitted.set(true);
        this.snackBar.open('Merci pour votre avis !', 'OK', { duration: 3000 });
        this.loadFeedbacks();
      },
      error: err => {
        this.submittingFeedback.set(false);
        const msg = err?.error?.message || 'Erreur lors de l\'envoi de votre avis';
        this.snackBar.open(msg, 'OK', { duration: 4000 });
        // Feedback déjà donné : on masque le formulaire
        if (err?.status === 409 || msg.toLowerCase().includes('déjà')) {
          this.feedbackSubmitted.set(true);
        }
      },
    });
  }

  reportEvent() {
    const e = this.event()!;
    this.dialog.open(ReportDialogComponent, {
      data: { targetType: 'EVENT', targetId: this.eventId, targetLabel: e.title },
    });
  }

  /** Télécharge un fichier .ics standard (compatible Google/Apple/Outlook). */
  addToCalendar() {
    const e = this.event()!;
    const fmt = (iso: string) => new Date(iso).toISOString().replace(/[-:]/g, '').replace(/\.\d{3}/, '');
    const location = e.online
      ? 'En ligne'
      : [e.locationName, e.locationAddress, e.locationCity, e.locationZip].filter(Boolean).join(', ');
    const esc = (s: string) => (s ?? '').replace(/([,;\\])/g, '\\$1').replace(/\n/g, '\\n');
    const url = window.location.href;
    const ics = [
      'BEGIN:VCALENDAR', 'VERSION:2.0', 'PRODID:-//Ummati//Events//FR', 'CALSCALE:GREGORIAN',
      'BEGIN:VEVENT',
      `UID:${this.eventId}@ummati`,
      `DTSTAMP:${fmt(new Date().toISOString())}`,
      `DTSTART:${fmt(e.startDate)}`,
      `DTEND:${fmt(e.endDate)}`,
      `SUMMARY:${esc(e.title)}`,
      `DESCRIPTION:${esc(e.description)}`,
      `LOCATION:${esc(location)}`,
      `URL:${url}`,
      'END:VEVENT', 'END:VCALENDAR',
    ].join('\r\n');

    const blob = new Blob([ics], { type: 'text/calendar;charset=utf-8' });
    const href = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = href;
    a.download = `${e.title.replace(/[^\p{L}\p{N}]+/gu, '_').slice(0, 40) || 'evenement'}.ics`;
    a.click();
    URL.revokeObjectURL(href);
  }

  /** Partage natif (Web Share API) avec repli sur copie du lien. */
  async shareEvent() {
    const e = this.event()!;
    const shareData = { title: e.title, text: e.title, url: window.location.href };
    try {
      if (navigator.share) {
        await navigator.share(shareData);
        return;
      }
    } catch { return; /* annulé par l'utilisateur */ }
    try {
      await navigator.clipboard.writeText(window.location.href);
      this.snackBar.open('Lien copié dans le presse-papiers', 'OK', { duration: 3000 });
    } catch {
      this.snackBar.open('Impossible de partager sur cet appareil', 'OK', { duration: 3000 });
    }
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

  /** Vrai si l'événement a plusieurs créneaux (inscription au niveau créneau). */
  isMulti(): boolean {
    return (this.event()?.occurrences?.length ?? 0) > 1;
  }

  signupOccurrence(occurrenceId: string) {
    this.signingUp.set(true);
    this.eventService.signupToOccurrence(this.eventId, occurrenceId).subscribe({
      next: res => {
        this.signingUp.set(false);
        const msg = res.data.status === 'WAITLISTED'
          ? 'Vous êtes sur la liste d\'attente' : 'Inscription confirmée !';
        this.snackBar.open(msg, 'OK', { duration: 3000 });
        this.loadEvent();
      },
      error: err => {
        this.signingUp.set(false);
        this.snackBar.open(err?.error?.message || 'Erreur lors de l\'inscription', 'OK', { duration: 4000 });
      },
    });
  }

  cancelOccurrence(occurrenceId: string) {
    this.signingUp.set(true);
    this.eventService.cancelOccurrenceSignup(this.eventId, occurrenceId).subscribe({
      next: () => {
        this.signingUp.set(false);
        this.snackBar.open('Désinscription effectuée', 'OK', { duration: 3000 });
        this.loadEvent();
      },
      error: err => {
        this.signingUp.set(false);
        this.snackBar.open(err?.error?.message || 'Erreur lors de la désinscription', 'OK', { duration: 4000 });
      },
    });
  }
}



