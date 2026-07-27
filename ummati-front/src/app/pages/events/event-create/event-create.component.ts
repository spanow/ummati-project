import { Component, signal, OnInit, OnDestroy } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';
import { MatChipsModule } from '@angular/material/chips';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { EventService } from '../../../core/services/event.service';
import { SkillService } from '../../../core/services/skill.service';
import { ImageService } from '../../../core/services/image.service';
import { EVENT_TYPES } from '../../../core/constants/event-types';
import { TPipe } from '../../../shared/pipes/t.pipe';
import { LocationPickerComponent } from '../../../shared/components/location-picker/location-picker.component';
import { GeoResult } from '../../../core/services/geocoding.service';

@Component({
  selector: 'app-event-create',
  standalone: true,
  imports: [ReactiveFormsModule, MatCardModule, MatButtonModule, MatIconModule, MatFormFieldModule,
    MatInputModule, MatSelectModule, MatCheckboxModule, MatDatepickerModule, MatNativeDateModule,
    MatChipsModule, MatSnackBarModule, MatProgressSpinnerModule, RouterLink, TPipe, LocationPickerComponent],
  template: `
    <div class="page page-narrow">
      <h1>{{ (isEdit ? 'Modifier un événement' : 'Créer un événement') | t }}</h1>
      <mat-card class="form-card">
        <mat-card-content>
          <form [formGroup]="form" (ngSubmit)="onSubmit()">
            <!-- Le visuel est le premier facteur de clic sur une mission : il se choisit
                 ici, au moment de la création, pas dans un écran de gestion séparé. -->
            <div class="cover-field">
              <span class="cover-label">{{ 'Image de couverture' | t }}</span>
              <p class="cover-hint">{{ 'Elle s\\'affiche sur les cartes et en tête de votre mission. JPG, PNG ou WebP, 5 Mo maximum.' | t }}</p>
              <div class="cover-row">
                <div class="cover-preview" [class.is-empty]="!coverPreview()">
                  @if (coverPreview()) {
                    <img [src]="coverPreview()" alt="" />
                  } @else {
                    <mat-icon>add_photo_alternate</mat-icon>
                  }
                </div>
                <div class="cover-actions">
                  <button mat-stroked-button type="button" (click)="coverInput.click()">
                    <mat-icon>upload</mat-icon>
                    {{ (coverPreview() ? 'Remplacer' : 'Choisir une image') | t }}
                  </button>
                  @if (coverPreview()) {
                    <button mat-button type="button" (click)="clearCover()">
                      <mat-icon>close</mat-icon> {{ 'Retirer' | t }}
                    </button>
                  }
                  <input #coverInput type="file" hidden [accept]="acceptedTypes"
                         (change)="onCoverSelected($event)" />
                </div>
              </div>
            </div>

            <mat-form-field appearance="outline" class="full-width">
              <mat-label>{{ 'Titre' | t }}</mat-label>
              <input matInput formControlName="title" />
            </mat-form-field>

            <mat-form-field appearance="outline" class="full-width">
              <mat-label>{{ 'Description' | t }}</mat-label>
              <textarea matInput formControlName="description" rows="5"></textarea>
            </mat-form-field>

            <mat-form-field appearance="outline" class="full-width">
              <mat-label>{{ 'Objectifs (optionnel)' | t }}</mat-label>
              <textarea matInput formControlName="objectives" rows="3"></textarea>
            </mat-form-field>

            <div class="row">
              <mat-form-field appearance="outline">
                <mat-label>{{ 'Type' | t }}</mat-label>
                <mat-select formControlName="type">
                  @for (t of eventTypes; track t.value) {
                    <mat-option [value]="t.value">{{ t.label | t }}</mat-option>
                  }
                </mat-select>
              </mat-form-field>

              <mat-form-field appearance="outline">
                <mat-label>{{ 'Ville' | t }}</mat-label>
                <input matInput formControlName="locationCity" />
              </mat-form-field>
            </div>

            <div class="row">
              <mat-form-field appearance="outline">
                <mat-label>{{ 'Nom du lieu' | t }}</mat-label>
                <input matInput formControlName="locationName" />
              </mat-form-field>
              <mat-form-field appearance="outline">
                <mat-label>{{ 'Adresse' | t }}</mat-label>
                <input matInput formControlName="locationAddress" />
              </mat-form-field>
            </div>

            @if (!form.get('online')?.value) {
              <div class="map-block">
                <label class="map-label">{{ 'Localiser sur la carte' | t }}</label>
                <app-location-picker
                  [lat]="form.get('locationLat')?.value"
                  [lng]="form.get('locationLng')?.value"
                  (coordsChange)="onCoords($event)"
                  (addressResolved)="onAddressResolved($event)" />
              </div>
            }

            <mat-checkbox formControlName="online" class="online-check">{{ 'Événement en ligne' | t }}</mat-checkbox>
            @if (form.get('online')?.value) {
              <mat-form-field appearance="outline" class="full-width">
                <mat-label>{{ 'Lien en ligne' | t }}</mat-label>
                <input matInput formControlName="onlineLink" />
              </mat-form-field>
            }

            <div class="row">
              <mat-form-field appearance="outline">
                <mat-label>{{ 'Date de début' | t }}</mat-label>
                <input matInput type="datetime-local" formControlName="startDate" />
              </mat-form-field>
              <mat-form-field appearance="outline">
                <mat-label>{{ 'Date de fin' | t }}</mat-label>
                <input matInput type="datetime-local" formControlName="endDate" />
              </mat-form-field>
            </div>

            <div class="row">
              <mat-form-field appearance="outline">
                <mat-label>{{ 'Date limite d\\'inscription' | t }}</mat-label>
                <input matInput type="datetime-local" formControlName="registrationDeadline" />
              </mat-form-field>
              <mat-form-field appearance="outline">
                <mat-label>{{ 'Max participants' | t }}</mat-label>
                <input matInput type="number" formControlName="maxParticipants" />
              </mat-form-field>
            </div>

            <mat-form-field appearance="outline">
              <mat-label>{{ 'Âge minimum' | t }}</mat-label>
              <input matInput type="number" formControlName="minAge" />
            </mat-form-field>

            @if (!isEdit) {
              <div class="recurrence-block">
                <mat-checkbox formControlName="recurrenceEnabled" class="online-check">
                  {{ 'Événement récurrent (plusieurs créneaux)' | t }}
                </mat-checkbox>
                @if (form.get('recurrenceEnabled')?.value) {
                  <div class="row">
                    <mat-form-field appearance="outline">
                      <mat-label>{{ 'Fréquence' | t }}</mat-label>
                      <mat-select formControlName="recurrenceFrequency">
                        <mat-option value="WEEKLY">{{ 'Chaque semaine' | t }}</mat-option>
                        <mat-option value="MONTHLY">{{ 'Chaque mois' | t }}</mat-option>
                      </mat-select>
                    </mat-form-field>
                    <mat-form-field appearance="outline">
                      <mat-label>{{ 'Tous les' | t }}</mat-label>
                      <input matInput type="number" min="1" formControlName="recurrenceInterval" />
                    </mat-form-field>
                    <mat-form-field appearance="outline">
                      <mat-label>{{ 'Jusqu\\'au' | t }}</mat-label>
                      <input matInput type="date" formControlName="recurrenceUntil" />
                    </mat-form-field>
                  </div>
                  <p class="recurrence-hint">
                    {{ 'Un créneau est généré à partir de la date de début, répété selon la règle jusqu\\'à la date de fin (100 créneaux max).' | t }}
                  </p>
                }
              </div>
            }

            @if (skills().length > 0) {
              <mat-form-field appearance="outline" class="full-width">
                <mat-label>{{ 'Compétences requises' | t }}</mat-label>
                <mat-select formControlName="requiredSkillIds" multiple>
                  @for (skill of skills(); track skill.id) {
                    <mat-option [value]="skill.id">{{ skill.name }}</mat-option>
                  }
                </mat-select>
              </mat-form-field>
            }

            @if (errorMessage()) {
              <p class="error">{{ errorMessage() }}</p>
            }

            <div class="actions">
              <button mat-flat-button type="submit" [disabled]="submitting()">
                @if (submitting()) { <mat-spinner diameter="20" /> }
                {{ (isEdit ? 'Enregistrer' : 'Créer l\\'événement') | t }}
              </button>
              <button mat-button type="button" routerLink="..">{{ 'Annuler' | t }}</button>
            </div>
          </form>
        </mat-card-content>
      </mat-card>
    </div>
  `,
  styles: [`
    h1 { font-size: 1.8rem; font-weight: 800; margin-bottom: 24px; letter-spacing: -0.02em; }
    .full-width { width: 100%; }
    .cover-field { margin-bottom: 24px; }
    .cover-label { display: block; font-size: 0.9rem; font-weight: 700; color: var(--brand-ink); }
    .cover-hint { font-size: 0.85rem; color: var(--brand-text-soft); margin: 4px 0 12px; }
    .cover-row { display: flex; gap: 16px; align-items: flex-start; flex-wrap: wrap; }
    .cover-preview {
      width: 220px; aspect-ratio: 16 / 9; flex: 0 0 auto; overflow: hidden;
      border-radius: var(--radius-sm); background: var(--brand-surface-2);
      border: 1px dashed var(--brand-border-strong);
      display: flex; align-items: center; justify-content: center;
    }
    .cover-preview img { width: 100%; height: 100%; object-fit: cover; }
    .cover-preview.is-empty mat-icon {
      font-size: 32px; width: 32px; height: 32px; color: var(--brand-text-faint);
    }
    .cover-actions { display: flex; flex-direction: column; gap: 8px; }
    .row { display: flex; gap: 16px; flex-wrap: wrap; }
    .row mat-form-field { flex: 1; min-width: 160px; }
    .online-check { margin-bottom: 16px; display: block; }
    .map-block { margin-bottom: 16px; }
    .map-label { display: block; font-size: 0.9rem; font-weight: 700; color: var(--brand-ink); margin-bottom: 8px; }
    .recurrence-block { margin-bottom: 16px; padding: 8px 0; }
    .recurrence-hint { font-size: 0.85rem; color: var(--brand-text-soft); margin: 4px 0 0; }
    .error { color: var(--brand-danger); font-size: 0.9rem; margin-bottom: 16px; }
    .actions { display: flex; gap: 12px; margin-top: 16px; flex-wrap: wrap; }
    .actions button[type="submit"] { min-width: 180px; height: 44px; }
  `],
})
export class EventCreateComponent implements OnInit, OnDestroy {
  form!: FormGroup;
  isEdit = false;
  submitting = signal(false);
  errorMessage = signal('');
  skills = signal<{ id: string; name: string }[]>([]);

  /** URL affichée : celle déjà stockée en édition, ou un aperçu local du fichier choisi. */
  coverPreview = signal<string | null>(null);
  readonly acceptedTypes = ImageService.ACCEPTED_TYPES;

  /** Fichier choisi mais pas encore envoyé : l'upload exige un eventId, qui n'existe
   *  qu'après la création. Il part donc juste après l'enregistrement. */
  private pendingCover: File | null = null;
  /** En édition, l'utilisateur a retiré la couverture existante. */
  private coverCleared = false;
  /** Object URL de l'aperçu local, à révoquer pour ne pas fuir de mémoire. */
  private previewObjectUrl: string | null = null;

  private orgId = '';
  private eventId = '';

  readonly eventTypes = EVENT_TYPES;

  constructor(
    private fb: FormBuilder,
    private route: ActivatedRoute,
    private router: Router,
    private eventService: EventService,
    private skillService: SkillService,
    private imageService: ImageService,
    private snackBar: MatSnackBar,
  ) {}

  ngOnDestroy() {
    this.revokePreview();
  }

  ngOnInit() {
    this.orgId = this.route.snapshot.paramMap.get('orgId') || '';
    this.eventId = this.route.snapshot.paramMap.get('eventId') || '';
    this.isEdit = !!this.eventId;

    this.form = this.fb.group({
      title: ['', Validators.required],
      description: ['', Validators.required],
      objectives: [''],
      type: ['', Validators.required],
      locationCity: ['', Validators.required],
      locationName: [''],
      locationAddress: [''],
      locationLat: [null],
      locationLng: [null],
      online: [false],
      onlineLink: [''],
      startDate: ['', Validators.required],
      endDate: ['', Validators.required],
      registrationDeadline: [''],
      maxParticipants: [null],
      minAge: [null],
      recurrenceEnabled: [false],
      recurrenceFrequency: ['WEEKLY'],
      recurrenceInterval: [1],
      recurrenceUntil: [''],
      requiredSkillIds: [[]],
    });

    this.loadSkills();

    if (this.isEdit) {
      this.eventService.getEvent(this.eventId).subscribe(res => {
        const e = res.data;
        this.orgId = e.organizationId;
        this.coverPreview.set(e.coverUrl);
        this.form.patchValue({
          ...e,
          startDate: e.startDate?.slice(0, 16),
          endDate: e.endDate?.slice(0, 16),
          registrationDeadline: e.registrationDeadline?.slice(0, 16),
          requiredSkillIds: e.requiredSkills.map(s => s.id),
        });
      });
    }
  }

  loadSkills() {
    this.skillService.getAll().subscribe(res => {
      this.skills.set(res.data.map((s: any) => ({ id: s.id, name: s.name })));
    });
  }

  onCoverSelected(event: Event) {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    // On vide l'input tout de suite : sans ça, resélectionner le même fichier
    // après une erreur ne déclencherait aucun événement « change ».
    input.value = '';
    if (!file) return;

    const error = ImageService.validate(file);
    if (error) { this.snackBar.open(error, 'OK', { duration: 4000 }); return; }

    this.revokePreview();
    this.pendingCover = file;
    this.coverCleared = false;
    this.previewObjectUrl = URL.createObjectURL(file);
    this.coverPreview.set(this.previewObjectUrl);
  }

  clearCover() {
    this.revokePreview();
    this.pendingCover = null;
    // En édition seulement : marque la suppression de l'image déjà stockée.
    this.coverCleared = this.isEdit;
    this.coverPreview.set(null);
  }

  private revokePreview() {
    if (this.previewObjectUrl) {
      URL.revokeObjectURL(this.previewObjectUrl);
      this.previewObjectUrl = null;
    }
  }

  onCoords(c: { lat: number; lng: number }) {
    this.form.patchValue({ locationLat: c.lat, locationLng: c.lng });
  }

  onAddressResolved(r: GeoResult) {
    // Autofill des champs texte laissés vides (sans écraser la saisie manuelle)
    const patch: any = {};
    if (r.city && !this.form.value.locationCity) patch.locationCity = r.city;
    if (r.street && !this.form.value.locationAddress) patch.locationAddress = r.street;
    if (Object.keys(patch).length) this.form.patchValue(patch);
  }

  onSubmit() {
    if (this.form.invalid) return;
    this.submitting.set(true);
    this.errorMessage.set('');

    const data = { ...this.form.value };
    if (!data.registrationDeadline) delete data.registrationDeadline;
    if (!data.maxParticipants) delete data.maxParticipants;
    if (!data.minAge) delete data.minAge;

    // Récurrence : transformer les champs de formulaire en objet `recurrence` (création seulement).
    const recEnabled = data.recurrenceEnabled;
    const recFreq = data.recurrenceFrequency;
    const recInterval = data.recurrenceInterval;
    const recUntil = data.recurrenceUntil;
    delete data.recurrenceEnabled;
    delete data.recurrenceFrequency;
    delete data.recurrenceInterval;
    delete data.recurrenceUntil;
    if (!this.isEdit && recEnabled && recUntil) {
      data.recurrence = { frequency: recFreq, interval: recInterval || 1, until: recUntil };
    }

    const obs = this.isEdit
      ? this.eventService.updateEvent(this.eventId, data)
      : this.eventService.createEvent(this.orgId, data);

    obs.subscribe({
      next: res => this.saveCoverThenLeave(res.data.id),
      error: err => {
        this.submitting.set(false);
        this.errorMessage.set(err.error?.message || 'Erreur lors de la sauvegarde');
      },
    });
  }

  /**
   * Applique le changement de couverture puis quitte le formulaire.
   *
   * L'événement est déjà enregistré à ce stade : un échec sur l'image ne doit pas
   * faire croire que la mission est perdue. On avertit et on continue — la couverture
   * reste modifiable depuis l'onglet « Visuels ».
   */
  private saveCoverThenLeave(eventId: string) {
    const done = (warning?: string) => {
      this.submitting.set(false);
      this.snackBar.open(
        warning ?? (this.isEdit ? 'Événement modifié' : 'Événement créé'),
        'OK', { duration: warning ? 5000 : 3000 });
      this.router.navigate(['/events', eventId]);
    };

    if (this.pendingCover) {
      this.imageService.uploadEventCover(eventId, this.pendingCover).subscribe({
        next: () => done(),
        error: () => done('Événement enregistré, mais l\'image n\'a pas pu être envoyée. Réessayez depuis l\'onglet « Visuels ».'),
      });
      return;
    }

    if (this.coverCleared) {
      this.imageService.deleteEventCover(eventId).subscribe({
        next: () => done(),
        error: () => done('Événement enregistré, mais l\'image n\'a pas pu être retirée.'),
      });
      return;
    }

    done();
  }
}


