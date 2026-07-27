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
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { EventService, EventPhoto, EventSummary, SignupResponse } from '../../../core/services/event.service';
import { EventAnnouncementService, AnnouncementResponse } from '../../../core/services/event-announcement.service';
import { ImageService } from '../../../core/services/image.service';
import { OrganizationService } from '../../../core/services/organization.service';
import { TPipe } from '../../../shared/pipes/t.pipe';
import { OrgDocumentsComponent } from '../../organizations/org-documents/org-documents.component';

@Component({
  selector: 'app-event-manage',
  standalone: true,
  imports: [MatCardModule, MatButtonModule, MatIconModule, MatTabsModule, MatTableModule,
    MatCheckboxModule, MatChipsModule, MatPaginatorModule, MatProgressSpinnerModule,
    MatSnackBarModule, MatDialogModule, MatFormFieldModule, MatInputModule, MatSlideToggleModule,
    RouterLink, DatePipe, FormsModule, TPipe, OrgDocumentsComponent],
  template: `
    <div class="page">
      <header class="page-header">
        <h1>{{ 'Gestion des événements' | t }}</h1>
        <a mat-flat-button [routerLink]="['/organizations', orgId, 'events', 'new']">
          <mat-icon>add</mat-icon> {{ 'Nouvel événement' | t }}
        </a>
      </header>

      @if (loading()) {
        <div class="state-center"><mat-spinner diameter="40" /></div>
      } @else {
        <mat-tab-group [selectedIndex]="tabIndex()" (selectedIndexChange)="tabIndex.set($event)">
          <mat-tab [label]="'Tous les événements' | t">
            @if (events().length === 0) {
              <div class="empty">
                <p>{{ 'Aucun événement pour cette organisation.' | t }}</p>
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
                          <span>{{ event.registeredCount }}@if(event.maxParticipants){/{{ event.maxParticipants }}} {{ 'inscrits' | t }}</span>
                        </div>
                      </div>
                      <div class="em-actions">
                        @if (event.status === 'DRAFT') {
                          <button mat-stroked-button (click)="publish(event)">
                            <mat-icon>publish</mat-icon> {{ 'Publier' | t }}
                          </button>
                          <a mat-stroked-button [routerLink]="['/events', event.id, 'edit']">
                            <mat-icon>edit</mat-icon> {{ 'Modifier' | t }}
                          </a>
                        }
                        @if (event.status === 'PUBLISHED') {
                          <button mat-stroked-button color="warn" (click)="cancelEvent(event)">
                            <mat-icon>cancel</mat-icon> {{ 'Annuler' | t }}
                          </button>
                          <button mat-stroked-button (click)="viewSignups(event)">
                            <mat-icon>people</mat-icon> {{ 'Inscrits' | t }}
                          </button>
                        }
                        @if (event.status === 'COMPLETED') {
                          <button mat-stroked-button (click)="viewSignups(event)">
                            <mat-icon>people</mat-icon> {{ 'Participants' | t }}
                          </button>
                        }
                        <button mat-stroked-button (click)="openVisuals(event)">
                          <mat-icon>image</mat-icon> {{ 'Visuels' | t }}
                        </button>
                        <button mat-stroked-button (click)="openDocuments(event)">
                          <mat-icon>folder</mat-icon> {{ 'Documents' | t }}
                        </button>
                      </div>
                    </div>
                  </mat-card-content>
                </mat-card>
              }
            }
          </mat-tab>

          <mat-tab [label]="'📢 ' + ('Annonces' | t)" [disabled]="!selectedEvent()">
            @if (selectedEvent()) {
              <div class="ann-section">
                <h2>{{ 'Annonces' | t }} — {{ selectedEvent()!.title }}</h2>
                @if (selectedEvent()!.status === 'PUBLISHED') {
                  <div class="ann-form">
                    <mat-form-field appearance="outline" class="ann-input">
                      <mat-label>{{ 'Nouvelle annonce' | t }}</mat-label>
                      <textarea matInput [(ngModel)]="newAnnContent" rows="3" maxlength="1000"
                        [placeholder]="'Ex: Rendez-vous à 18h30 devant la gare — 3 voitures disponibles' | t"></textarea>
                    </mat-form-field>
                    <div class="ann-form-actions">
                      <label class="pin-toggle">
                        <input type="checkbox" [(ngModel)]="newAnnPinned" /> {{ 'Épingler' | t }}
                      </label>
                      <button mat-flat-button color="primary" (click)="postAnnouncement()"
                          [disabled]="newAnnContent.trim().length < 5">
                        <mat-icon>campaign</mat-icon> {{ 'Publier l\\'annonce' | t }}
                      </button>
                    </div>
                  </div>
                }
                <div class="ann-list">
                  @for (ann of announcements(); track ann.id) {
                    <div class="ann-item" [class.pinned]="ann.pinned">
                      <div class="ann-item-header">
                        @if (ann.pinned) { <span class="pin-tag">📌 {{ 'Épinglée' | t }}</span> }
                        <span class="ann-date">{{ ann.createdAt | date:'d MMM yyyy, HH:mm' }}</span>
                        <button mat-icon-button color="warn" (click)="deleteAnnouncement(ann.id)" [title]="'Supprimer' | t">
                          <mat-icon>delete</mat-icon>
                        </button>
                      </div>
                      <p class="ann-content">{{ ann.content }}</p>
                    </div>
                  } @empty {
                    <p class="empty-ann">{{ 'Aucune annonce pour cet événement.' | t }}</p>
                  }
                </div>
              </div>
            }
          </mat-tab>

          <mat-tab [label]="'Inscrits' | t" [disabled]="!selectedEvent()">
            @if (selectedEvent()) {
              <div class="signups-header">
                <h2>{{ 'Inscrits' | t }} — {{ selectedEvent()!.title }}</h2>
                <div class="signups-actions">
                  <button mat-stroked-button (click)="exportCsv()">
                    <mat-icon>download</mat-icon> {{ 'Export CSV' | t }}
                  </button>
                  @if (selectedEvent()!.status !== 'DRAFT') {
                    <button mat-flat-button (click)="markSelectedAttended()" [disabled]="selectedUserIds.length === 0">
                      <mat-icon>check_circle</mat-icon> {{ 'Marquer présents' | t }}
                    </button>
                  }
                </div>
              </div>
              @if (signupsLoading()) {
                <div class="state-center"><mat-spinner diameter="30" /></div>
              } @else {
                <table class="signups-table">
                  <thead>
                    <tr>
                      <th><input type="checkbox" (change)="toggleAll($event)" /></th>
                      <th>{{ 'Nom' | t }}</th>
                      <th>{{ 'Email' | t }}</th>
                      <th>{{ 'Statut' | t }}</th>
                      <th>{{ 'Inscrit le' | t }}</th>
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

          <mat-tab [label]="'Visuels' | t" [disabled]="!selectedEvent()">
            @if (selectedEvent()) {
              <div class="tab-section-docs">
                <p class="docs-intro">
                  <mat-icon>info</mat-icon>
                  {{ 'Une mission avec un visuel attire nettement plus de bénévoles. JPG, PNG ou WebP, 5 Mo maximum.' | t }}
                </p>

                <section class="visual-block">
                  <h3>{{ 'Image de couverture' | t }}</h3>
                  <p class="visual-help">{{ 'Affichée en tête de la mission et sur les cartes de la liste. Format paysage recommandé.' | t }}</p>
                  <div class="cover-editor">
                    <div class="cover-preview" [class.is-empty]="!coverUrl()">
                      @if (coverUrl()) {
                        <img [src]="coverUrl()" alt="" />
                      } @else {
                        <mat-icon>image</mat-icon>
                      }
                    </div>
                    <div class="visual-actions">
                      <button mat-flat-button type="button" [disabled]="uploadingCover()"
                              (click)="coverInput.click()">
                        @if (uploadingCover()) { <mat-spinner diameter="18" /> }
                        @else { <mat-icon>upload</mat-icon> }
                        {{ (coverUrl() ? 'Remplacer' : 'Ajouter une couverture') | t }}
                      </button>
                      @if (coverUrl()) {
                        <button mat-stroked-button type="button" (click)="removeCover()">
                          <mat-icon>delete</mat-icon> {{ 'Retirer' | t }}
                        </button>
                      }
                      <input #coverInput type="file" hidden [accept]="acceptedTypes"
                             (change)="onCoverSelected($event)" />
                    </div>
                  </div>
                </section>

                <section class="visual-block">
                  <h3>{{ 'Galerie' | t }}</h3>
                  <p class="visual-help">
                    {{ 'Ajoutez des photos après la mission : c\\'est la meilleure preuve d\\'impact et le meilleur argument pour la prochaine édition.' | t }}
                    <strong>{{ 'Assurez-vous d\\'avoir l\\'accord des personnes reconnaissables.' | t }}</strong>
                  </p>
                  <button mat-flat-button type="button" [disabled]="uploadingPhoto()"
                          (click)="photoInput.click()">
                    @if (uploadingPhoto()) { <mat-spinner diameter="18" /> }
                    @else { <mat-icon>add_photo_alternate</mat-icon> }
                    {{ 'Ajouter une photo' | t }}
                  </button>
                  <input #photoInput type="file" hidden [accept]="acceptedTypes"
                         (change)="onPhotoSelected($event)" />

                  @if (photos().length > 0) {
                    <div class="photo-grid">
                      @for (photo of photos(); track photo.id) {
                        <figure class="photo-item">
                          <img [src]="photo.url" [alt]="photo.caption || ''" loading="lazy" />
                          <button mat-icon-button class="photo-delete" type="button"
                                  [attr.aria-label]="'Supprimer cette photo' | t"
                                  (click)="removePhoto(photo.id)">
                            <mat-icon>delete</mat-icon>
                          </button>
                        </figure>
                      }
                    </div>
                  } @else {
                    <p class="empty-ann">{{ 'Aucune photo pour le moment.' | t }}</p>
                  }
                </section>
              </div>
            }
          </mat-tab>

          <mat-tab [label]="'Documents' | t" [disabled]="!selectedEvent()">
            @if (selectedEvent()) {
              <div class="tab-section-docs">
                <p class="docs-intro">
                  <mat-icon>info</mat-icon>
                  {{ 'Ajoutez ici les pièces justificatives de l\\'événement. Elles pourront être examinées par un administrateur de la plateforme.' | t }}
                </p>
                <app-org-documents [eventId]="selectedEvent()!.id" [canUpload]="true" />
              </div>
            }
          </mat-tab>
        </mat-tab-group>
      }
    </div>
  `,
  styles: [`
    .page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 24px; gap: 16px; flex-wrap: wrap; }
    .page-header h1 { font-size: 1.8rem; font-weight: 800; margin: 0; letter-spacing: -0.02em; }
    .empty { text-align: center; padding: 40px; color: var(--brand-text-soft); }
    .event-manage-card { margin: 12px 0; }
    .em-row { display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 12px; }
    .em-info h3 { margin: 0 0 8px; font-weight: 700; color: var(--brand-ink); }
    .em-meta { display: flex; gap: 12px; align-items: center; font-size: 0.85rem; color: var(--brand-text-soft); flex-wrap: wrap; }
    .em-actions { display: flex; gap: 8px; flex-wrap: wrap; }
    .status-draft { background: var(--brand-accent-soft) !important; color: var(--brand-warn) !important; }
    .status-published { background: var(--brand-success-soft) !important; color: var(--brand-success) !important; }
    .status-cancelled { background: var(--brand-danger-soft) !important; color: var(--brand-danger) !important; }
    .status-completed { background: var(--brand-primary-100) !important; color: var(--brand-primary-dark) !important; }
    .signup-registered { background: var(--brand-success-soft) !important; color: var(--brand-success) !important; }
    .signup-waitlisted { background: var(--brand-accent-soft) !important; color: var(--brand-warn) !important; }
    .signup-attended { background: var(--brand-primary-100) !important; color: var(--brand-primary-dark) !important; }
    .signup-cancelled { background: var(--brand-danger-soft) !important; color: var(--brand-danger) !important; }
    .signups-header { display: flex; justify-content: space-between; align-items: center; margin: 16px 0; flex-wrap: wrap; gap: 12px; }
    .signups-header h2 { font-size: 1.2rem; font-weight: 700; margin: 0; }
    .signups-actions { display: flex; gap: 8px; flex-wrap: wrap; }
    .signups-table { width: 100%; border-collapse: collapse; }
    .signups-table th, .signups-table td { padding: 10px 12px; text-align: left; border-bottom: 1px solid var(--brand-border); }
    .signups-table th { font-size: 0.8rem; color: var(--brand-text-soft); text-transform: uppercase; }
    .ann-section { padding: 16px 0; }
    .ann-section h2 { font-size: 1.2rem; font-weight: 700; margin: 0 0 16px; }
    .ann-form { background: var(--brand-surface-2); border: 1px solid var(--brand-border); border-radius: var(--radius-sm); padding: 16px; margin-bottom: 24px; }
    .ann-input { width: 100%; }
    .ann-form-actions { display: flex; align-items: center; gap: 16px; margin-top: 8px; flex-wrap: wrap; }
    .pin-toggle { display: flex; align-items: center; gap: 6px; font-size: 0.9rem; cursor: pointer; }
    .ann-list { display: flex; flex-direction: column; gap: 12px; }
    .ann-item { border: 1px solid var(--brand-border); border-radius: var(--radius-sm); padding: 12px 16px; }
    .ann-item.pinned { border-color: var(--brand-primary); background: var(--brand-primary-soft); }
    .ann-item-header { display: flex; align-items: center; gap: 8px; margin-bottom: 8px; }
    .pin-tag { font-size: 0.8rem; font-weight: 700; color: var(--brand-primary); }
    .ann-date { font-size: 0.8rem; color: var(--brand-text-faint); margin-left: auto; }
    .ann-content { margin: 0; white-space: pre-line; line-height: 1.6; }
    .empty-ann { color: var(--brand-text-faint); text-align: center; padding: 24px; }
    .tab-section-docs { padding: 20px 0; }
    .docs-intro { display: flex; align-items: center; gap: 8px; color: var(--brand-text-soft); font-size: 0.9rem;
      background: var(--brand-primary-soft); border-radius: var(--radius-sm); padding: 10px 14px; margin: 0 0 16px; }
    .docs-intro mat-icon { color: var(--brand-primary); font-size: 20px; width: 20px; height: 20px; }

    .visual-block { margin-bottom: 32px; }
    .visual-block h3 { font-size: 1.05rem; font-weight: 700; margin: 0 0 4px; }
    .visual-help { color: var(--brand-text-soft); font-size: 0.88rem; margin: 0 0 14px; line-height: 1.55; }
    .cover-editor { display: flex; gap: 20px; align-items: flex-start; flex-wrap: wrap; }
    .cover-preview {
      width: 280px; aspect-ratio: 16 / 9; border-radius: var(--radius-sm); overflow: hidden;
      background: var(--brand-surface-2); border: 1px solid var(--brand-border);
      display: flex; align-items: center; justify-content: center; flex: 0 0 auto;
    }
    .cover-preview img { width: 100%; height: 100%; object-fit: cover; }
    .cover-preview.is-empty mat-icon {
      font-size: 40px; width: 40px; height: 40px; color: var(--brand-text-faint);
    }
    .visual-actions { display: flex; flex-direction: column; gap: 10px; }
    .photo-grid {
      display: grid; grid-template-columns: repeat(auto-fill, minmax(150px, 1fr));
      gap: 12px; margin-top: 16px;
    }
    .photo-item { position: relative; margin: 0; }
    .photo-item img {
      width: 100%; aspect-ratio: 4 / 3; object-fit: cover; display: block;
      border-radius: var(--radius-sm); background: var(--brand-surface-2);
    }
    .photo-delete {
      position: absolute; inset-block-start: 6px; inset-inline-end: 6px;
      background: var(--brand-surface); box-shadow: var(--brand-shadow-xs);
    }
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
  announcements = signal<AnnouncementResponse[]>([]);
  newAnnContent = '';
  newAnnPinned = false;
  tabIndex = signal(0);

  photos = signal<EventPhoto[]>([]);
  coverUrl = signal<string | null>(null);
  uploadingCover = signal(false);
  uploadingPhoto = signal(false);
  readonly acceptedTypes = ImageService.ACCEPTED_TYPES;

  constructor(
    private route: ActivatedRoute,
    private eventService: EventService,
    private announcementService: EventAnnouncementService,
    private imageService: ImageService,
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
    this.loadAnnouncements(event.id);
    this.tabIndex.set(2); // onglet « Inscrits »
  }

  openVisuals(event: EventSummary) {
    this.selectedEvent.set(event);
    this.coverUrl.set(event.coverUrl);
    this.loadPhotos(event.id);
    this.tabIndex.set(3); // onglet « Visuels »
  }

  openDocuments(event: EventSummary) {
    this.selectedEvent.set(event);
    this.tabIndex.set(4); // onglet « Documents »
  }

  // --- Visuels ---

  loadPhotos(eventId: string) {
    this.imageService.listEventPhotos(eventId).subscribe({
      next: res => this.photos.set(res.data),
      error: () => this.photos.set([]),
    });
  }

  onCoverSelected(event: Event) {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    // On vide l'input tout de suite : sans ça, resélectionner le même fichier
    // après une erreur ne déclencherait aucun événement « change ».
    input.value = '';
    const eventId = this.selectedEvent()?.id;
    if (!file || !eventId) return;

    const error = ImageService.validate(file);
    if (error) { this.snackBar.open(error, 'OK', { duration: 4000 }); return; }

    this.uploadingCover.set(true);
    this.imageService.uploadEventCover(eventId, file).subscribe({
      next: res => {
        this.coverUrl.set(res.data.coverUrl);
        this.uploadingCover.set(false);
        this.snackBar.open('Couverture mise à jour', 'OK', { duration: 3000 });
        this.loadEvents();
      },
      error: err => {
        this.uploadingCover.set(false);
        this.snackBar.open(err.error?.message || 'Erreur', 'OK', { duration: 4000 });
      },
    });
  }

  removeCover() {
    const eventId = this.selectedEvent()?.id;
    if (!eventId) return;
    this.imageService.deleteEventCover(eventId).subscribe({
      next: () => {
        this.coverUrl.set(null);
        this.snackBar.open('Couverture retirée', 'OK', { duration: 3000 });
        this.loadEvents();
      },
      error: err => this.snackBar.open(err.error?.message || 'Erreur', 'OK', { duration: 3000 }),
    });
  }

  onPhotoSelected(event: Event) {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    input.value = '';
    const eventId = this.selectedEvent()?.id;
    if (!file || !eventId) return;

    const error = ImageService.validate(file);
    if (error) { this.snackBar.open(error, 'OK', { duration: 4000 }); return; }

    this.uploadingPhoto.set(true);
    this.imageService.addEventPhoto(eventId, file).subscribe({
      next: res => {
        this.photos.update(list => [...list, res.data]);
        this.uploadingPhoto.set(false);
      },
      error: err => {
        this.uploadingPhoto.set(false);
        this.snackBar.open(err.error?.message || 'Erreur', 'OK', { duration: 4000 });
      },
    });
  }

  removePhoto(photoId: string) {
    this.imageService.deleteEventPhoto(photoId).subscribe({
      next: () => this.photos.update(list => list.filter(p => p.id !== photoId)),
      error: err => this.snackBar.open(err.error?.message || 'Erreur', 'OK', { duration: 3000 }),
    });
  }

  loadAnnouncements(eventId: string) {
    this.announcementService.list(eventId).subscribe({
      next: res => this.announcements.set(res.data),
      error: () => {},
    });
  }

  postAnnouncement() {
    const eventId = this.selectedEvent()?.id;
    if (!eventId || this.newAnnContent.trim().length < 5) return;
    this.announcementService.create(eventId, { content: this.newAnnContent.trim(), pinned: this.newAnnPinned })
      .subscribe({
        next: res => {
          this.announcements.update(list => {
            const updated = [...list, res.data];
            return updated.sort((a, b) => (b.pinned ? 1 : 0) - (a.pinned ? 1 : 0));
          });
          this.newAnnContent = '';
          this.newAnnPinned = false;
          this.snackBar.open('Annonce publiée — inscrits notifiés !', 'OK', { duration: 3000 });
        },
        error: err => this.snackBar.open(err.error?.message || 'Erreur', 'OK', { duration: 3000 }),
      });
  }

  deleteAnnouncement(annId: string) {
    const eventId = this.selectedEvent()?.id;
    if (!eventId) return;
    this.announcementService.delete(eventId, annId).subscribe({
      next: () => {
        this.announcements.update(list => list.filter(a => a.id !== annId));
        this.snackBar.open('Annonce supprimée', 'OK', { duration: 2000 });
      },
      error: () => this.snackBar.open('Erreur lors de la suppression', 'OK', { duration: 3000 }),
    });
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

