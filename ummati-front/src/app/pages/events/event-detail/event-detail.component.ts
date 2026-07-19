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

@Component({
  selector: 'app-event-detail',
  standalone: true,
  imports: [MatCardModule, MatButtonModule, MatIconModule, MatChipsModule, MatProgressBarModule,
    MatProgressSpinnerModule, MatDividerModule, MatSnackBarModule, MatFormFieldModule, MatInputModule,
    MatMenuModule, MatDialogModule, MatCheckboxModule, FormsModule, RouterLink, DatePipe, DecimalPipe,
    StarRatingComponent],
  template: `
    <div class="page-container">
      @if (loading()) {
        <div class="loading"><mat-spinner diameter="40" /></div>
      } @else if (event()) {
        <div class="event-header">
          <div class="header-top">
            <a mat-button [routerLink]="['/organizations', event()!.organizationSlug]" class="org-link">
              <mat-icon>business</mat-icon> {{ event()!.organizationName }}
            </a>
            @if (isLoggedIn()) {
              <button mat-icon-button [matMenuTriggerFor]="eventMenu" aria-label="Plus d'options">
                <mat-icon>more_vert</mat-icon>
              </button>
              <mat-menu #eventMenu="matMenu">
                <button mat-menu-item (click)="reportEvent()">
                  <mat-icon>flag</mat-icon> Signaler cet événement
                </button>
              </mat-menu>
            }
          </div>
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

            @if (announcements().length > 0) {
              <mat-card class="announcements-card">
                <mat-card-content>
                  <h3>📢 Annonces de l'organisateur</h3>
                  @for (ann of announcements(); track ann.id) {
                    <div class="announcement-item" [class.pinned]="ann.pinned">
                      @if (ann.pinned) { <span class="pin-badge">📌 Épinglée</span> }
                      <p class="ann-content">{{ ann.content }}</p>
                      <span class="ann-meta">{{ ann.authorFirstName }} {{ ann.authorLastName }} · {{ ann.createdAt | date:'d MMM yyyy, HH:mm' }}</span>
                    </div>
                  }
                </mat-card-content>
              </mat-card>
            }

            <mat-card class="comments-card">
              <mat-card-content>
                <h3>💬 Discussion ({{ totalComments() }})</h3>

                @if (isLoggedIn() && isParticipant()) {
                  <div class="comment-form">
                    <mat-form-field appearance="outline" class="comment-input">
                      <mat-label>Votre commentaire</mat-label>
                      <textarea matInput [(ngModel)]="newComment" rows="2" maxlength="500"
                        placeholder="Partagez vos questions ou infos pratiques..."></textarea>
                    </mat-form-field>
                    <button mat-flat-button color="primary" (click)="postComment()"
                        [disabled]="newComment.trim().length < 2 || postingComment()">
                      Publier
                    </button>
                  </div>
                } @else if (isLoggedIn() && !isParticipant()) {
                  <p class="comment-hint">Inscrivez-vous à l'événement pour commenter.</p>
                } @else {
                  <p class="comment-hint"><a routerLink="/login">Connectez-vous</a> et inscrivez-vous pour commenter.</p>
                }

                @for (c of comments(); track c.id) {
                  <div class="comment-item">
                    <div class="comment-header">
                      <span class="comment-author">{{ c.authorFirstName }} {{ c.authorLastName }}</span>
                      <span class="comment-date">{{ c.createdAt | date:'d MMM yyyy, HH:mm' }}</span>
                      @if (canDeleteComment(c)) {
                        <button mat-icon-button class="delete-btn" (click)="deleteComment(c.id)" title="Supprimer">
                          <mat-icon>delete_outline</mat-icon>
                        </button>
                      }
                    </div>
                    <p class="comment-content">{{ c.content }}</p>
                  </div>
                }

                @if (comments().length === 0 && !loadingComments()) {
                  <p class="no-comments">Soyez le premier à commenter !</p>
                }
              </mat-card-content>
            </mat-card>

            @if (canGiveFeedback()) {
              <mat-card class="feedback-form-card">
                <mat-card-content>
                  <h3>⭐ Donner mon avis</h3>
                  <p class="feedback-hint">Vous avez participé à cet événement — partagez votre expérience !</p>
                  <div class="feedback-form">
                    <app-star-rating [value]="feedbackRating" (valueChange)="feedbackRating = $event" />
                    <mat-form-field appearance="outline" class="full-width">
                      <mat-label>Commentaire (optionnel)</mat-label>
                      <textarea matInput [(ngModel)]="feedbackComment" rows="3" maxlength="1000"
                                placeholder="Qu'avez-vous pensé de cet événement ?"></textarea>
                    </mat-form-field>
                    <div class="feedback-actions">
                      <mat-checkbox [(ngModel)]="feedbackAnonymous">Publier anonymement</mat-checkbox>
                      <button mat-flat-button color="primary" (click)="submitFeedback()"
                              [disabled]="feedbackRating === 0 || submittingFeedback()">
                        {{ submittingFeedback() ? 'Envoi…' : 'Publier mon avis' }}
                      </button>
                    </div>
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
    .header-top { display: flex; align-items: center; justify-content: space-between; }
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
    .feedback-form-card { border-left: 4px solid #ffc107; }
    .feedback-hint { color: #666; font-size: 0.9rem; margin: 0 0 16px; }
    .feedback-form { display: flex; flex-direction: column; gap: 12px; }
    .feedback-actions { display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 12px; }
    .full-width { width: 100%; }
    .announcements-card { border-left: 4px solid #1976d2; }
    .announcement-item { padding: 12px 0; border-bottom: 1px solid #eee; }
    .announcement-item:last-child { border-bottom: none; }
    .announcement-item.pinned { background: #f3f8ff; border-radius: 8px; padding: 12px; margin-bottom: 8px; }
    .pin-badge { font-size: 0.75rem; color: #1976d2; font-weight: 600; display: block; margin-bottom: 4px; }
    .ann-content { margin: 4px 0; white-space: pre-line; line-height: 1.6; }
    .ann-meta { font-size: 0.8rem; color: #999; }
    .comments-card { }
    .comment-form { display: flex; gap: 12px; align-items: flex-start; margin-bottom: 20px; }
    .comment-input { flex: 1; }
    .comment-hint { font-size: 0.9rem; color: #888; margin-bottom: 16px; }
    .comment-hint a { color: #1976d2; }
    .comment-item { padding: 12px 0; border-bottom: 1px solid #eee; }
    .comment-item:last-child { border-bottom: none; }
    .comment-header { display: flex; align-items: center; gap: 8px; margin-bottom: 4px; }
    .comment-author { font-weight: 600; font-size: 0.9rem; }
    .comment-date { color: #999; font-size: 0.8rem; }
    .delete-btn { margin-left: auto; color: #bbb; width: 32px; height: 32px; line-height: 32px; }
    .delete-btn mat-icon { font-size: 18px; }
    .comment-content { margin: 0; line-height: 1.6; white-space: pre-line; color: #444; }
    .no-comments { color: #aaa; font-style: italic; text-align: center; padding: 24px 0; }
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



