import { Component, signal } from '@angular/core';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatStepperModule } from '@angular/material/stepper';
import { OrganizationService } from '../../../core/services/organization.service';

@Component({
  selector: 'app-organization-create',
  standalone: true,
  imports: [ReactiveFormsModule, MatCardModule, MatFormFieldModule, MatInputModule,
    MatSelectModule, MatButtonModule, MatIconModule, MatProgressSpinnerModule, MatStepperModule],
  template: `
    <div class="page-container">
      <h1>Créer une organisation</h1>
      <p class="subtitle">Enregistrez votre association sur Ummati</p>

      @if (errorMessage()) { <div class="error-banner">{{ errorMessage() }}</div> }

      <mat-card class="form-card">
        <form [formGroup]="form" (ngSubmit)="onSubmit()">
          <h3>Informations générales</h3>
          <mat-form-field appearance="outline" class="full-width">
            <mat-label>Nom de l'organisation</mat-label>
            <input matInput formControlName="name" />
          </mat-form-field>
          <mat-form-field appearance="outline" class="full-width">
            <mat-label>Description</mat-label>
            <textarea matInput formControlName="description" rows="4"></textarea>
          </mat-form-field>
          <mat-form-field appearance="outline" class="full-width">
            <mat-label>Mission (optionnel)</mat-label>
            <textarea matInput formControlName="mission" rows="3"></textarea>
          </mat-form-field>
          <mat-form-field appearance="outline" class="full-width">
            <mat-label>Domaine</mat-label>
            <mat-select formControlName="domain">
              @for (d of domains; track d.value) {
                <mat-option [value]="d.value">{{ d.label }}</mat-option>
              }
            </mat-select>
          </mat-form-field>

          <h3>Localisation</h3>
          <div class="row">
            <mat-form-field appearance="outline" class="flex-2">
              <mat-label>Ville</mat-label>
              <input matInput formControlName="addressCity" />
            </mat-form-field>
            <mat-form-field appearance="outline" class="flex-1">
              <mat-label>Code postal</mat-label>
              <input matInput formControlName="addressZip" />
            </mat-form-field>
          </div>

          <h3>Contact</h3>
          <mat-form-field appearance="outline" class="full-width">
            <mat-label>Email de contact</mat-label>
            <input matInput formControlName="email" type="email" />
          </mat-form-field>
          <div class="row">
            <mat-form-field appearance="outline" class="flex-1">
              <mat-label>Téléphone (optionnel)</mat-label>
              <input matInput formControlName="phone" />
            </mat-form-field>
            <mat-form-field appearance="outline" class="flex-1">
              <mat-label>Site web (optionnel)</mat-label>
              <input matInput formControlName="website" />
            </mat-form-field>
          </div>

          <button mat-flat-button type="submit" class="full-width submit-btn" [disabled]="loading()">
            @if (loading()) { <mat-spinner diameter="20" /> } @else { Soumettre l'organisation }
          </button>
        </form>
      </mat-card>
    </div>
  `,
  styles: [`
    .page-container { max-width: 680px; margin: 0 auto; padding: 32px 24px; }
    h1 { font-size: 1.8rem; font-weight: 600; margin: 0; }
    .subtitle { color: #666; margin: 4px 0 24px; }
    h3 { font-size: 1rem; font-weight: 600; margin: 24px 0 12px; color: #333; }
    .form-card { padding: 32px; border-radius: 12px; }
    .full-width { width: 100%; }
    .row { display: flex; gap: 16px; }
    .flex-1 { flex: 1; }
    .flex-2 { flex: 2; }
    .submit-btn { height: 48px; font-size: 16px; margin-top: 16px; }
    .error-banner { background: #fdecea; color: #d32f2f; padding: 12px; border-radius: 8px; margin-bottom: 16px; }
  `],
})
export class OrganizationCreateComponent {
  form: FormGroup;
  loading = signal(false);
  errorMessage = signal('');

  domains = [
    { value: 'EDUCATION', label: 'Éducation' }, { value: 'SANTE', label: 'Santé' },
    { value: 'ENVIRONNEMENT', label: 'Environnement' }, { value: 'SOCIAL', label: 'Social' },
    { value: 'CULTURE', label: 'Culture' }, { value: 'SPORT', label: 'Sport' },
    { value: 'HUMANITAIRE', label: 'Humanitaire' }, { value: 'DROITS_HUMAINS', label: 'Droits humains' },
    { value: 'AIDE_URGENCE', label: "Aide d'urgence" }, { value: 'AUTRE', label: 'Autre' },
  ];

  constructor(private fb: FormBuilder, private orgService: OrganizationService, private router: Router) {
    this.form = this.fb.group({
      name: ['', Validators.required],
      description: ['', Validators.required],
      mission: [''],
      domain: ['', Validators.required],
      addressCity: ['', Validators.required],
      addressZip: ['', Validators.required],
      email: ['', [Validators.required, Validators.email]],
      phone: [''],
      website: [''],
    });
  }

  onSubmit() {
    if (this.form.invalid) return;
    this.loading.set(true);
    this.orgService.create(this.form.value).subscribe({
      next: res => { this.loading.set(false); this.router.navigate(['/organizations', res.data.slug]); },
      error: err => { this.loading.set(false); this.errorMessage.set(err.error?.message || 'Erreur'); },
    });
  }
}

