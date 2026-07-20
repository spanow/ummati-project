import { Component, signal, OnInit } from '@angular/core';
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
import { EVENT_TYPES } from '../../../core/constants/event-types';
import { TPipe } from '../../../shared/pipes/t.pipe';

@Component({
  selector: 'app-event-create',
  standalone: true,
  imports: [ReactiveFormsModule, MatCardModule, MatButtonModule, MatIconModule, MatFormFieldModule,
    MatInputModule, MatSelectModule, MatCheckboxModule, MatDatepickerModule, MatNativeDateModule,
    MatChipsModule, MatSnackBarModule, MatProgressSpinnerModule, RouterLink, TPipe],
  template: `
    <div class="page-container">
      <h1>{{ (isEdit ? 'Modifier un événement' : 'Créer un événement') | t }}</h1>
      <mat-card class="form-card">
        <mat-card-content>
          <form [formGroup]="form" (ngSubmit)="onSubmit()">
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
    .page-container { max-width: 800px; margin: 0 auto; padding: 32px 24px; }
    h1 { font-size: 1.8rem; font-weight: 600; margin-bottom: 24px; }
    .form-card { border-radius: 12px; }
    .full-width { width: 100%; }
    .row { display: flex; gap: 16px; }
    .row mat-form-field { flex: 1; }
    .online-check { margin-bottom: 16px; display: block; }
    .error { color: #d32f2f; font-size: 0.9rem; margin-bottom: 16px; }
    .actions { display: flex; gap: 12px; margin-top: 16px; }
    .actions button[type="submit"] { min-width: 180px; height: 44px; }
  `],
})
export class EventCreateComponent implements OnInit {
  form!: FormGroup;
  isEdit = false;
  submitting = signal(false);
  errorMessage = signal('');
  skills = signal<{ id: string; name: string }[]>([]);

  private orgId = '';
  private eventId = '';

  readonly eventTypes = EVENT_TYPES;

  constructor(
    private fb: FormBuilder,
    private route: ActivatedRoute,
    private router: Router,
    private eventService: EventService,
    private skillService: SkillService,
    private snackBar: MatSnackBar,
  ) {}

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
      online: [false],
      onlineLink: [''],
      startDate: ['', Validators.required],
      endDate: ['', Validators.required],
      registrationDeadline: [''],
      maxParticipants: [null],
      minAge: [null],
      requiredSkillIds: [[]],
    });

    this.loadSkills();

    if (this.isEdit) {
      this.eventService.getEvent(this.eventId).subscribe(res => {
        const e = res.data;
        this.orgId = e.organizationId;
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

  onSubmit() {
    if (this.form.invalid) return;
    this.submitting.set(true);
    this.errorMessage.set('');

    const data = { ...this.form.value };
    if (!data.registrationDeadline) delete data.registrationDeadline;
    if (!data.maxParticipants) delete data.maxParticipants;
    if (!data.minAge) delete data.minAge;

    const obs = this.isEdit
      ? this.eventService.updateEvent(this.eventId, data)
      : this.eventService.createEvent(this.orgId, data);

    obs.subscribe({
      next: res => {
        this.submitting.set(false);
        this.snackBar.open(this.isEdit ? 'Événement modifié' : 'Événement créé', 'OK', { duration: 3000 });
        this.router.navigate(['/events', res.data.id]);
      },
      error: err => {
        this.submitting.set(false);
        this.errorMessage.set(err.error?.message || 'Erreur lors de la sauvegarde');
      },
    });
  }
}


