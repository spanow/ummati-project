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
import { TPipe } from '../../../shared/pipes/t.pipe';
import { LocationPickerComponent } from '../../../shared/components/location-picker/location-picker.component';
import { GeoResult } from '../../../core/services/geocoding.service';

@Component({
  selector: 'app-organization-create',
  standalone: true,
  imports: [ReactiveFormsModule, MatCardModule, MatFormFieldModule, MatInputModule,
    MatSelectModule, MatButtonModule, MatIconModule, MatProgressSpinnerModule, MatStepperModule, TPipe,
    LocationPickerComponent],
  template: `
    <div class="page page-narrow">
      <h1>{{ 'Créer une organisation' | t }}</h1>
      <p class="subtitle">{{ 'Enregistrez votre association sur Ummati' | t }}</p>

      @if (errorMessage()) { <div class="error-banner">{{ errorMessage() }}</div> }

      <mat-card class="form-card">
        <form [formGroup]="form" (ngSubmit)="onSubmit()">
          <h3>{{ 'Informations générales' | t }}</h3>
          <mat-form-field appearance="outline" class="full-width">
            <mat-label>{{ 'Nom de l\\'organisation' | t }}</mat-label>
            <input matInput formControlName="name" />
          </mat-form-field>
          <mat-form-field appearance="outline" class="full-width">
            <mat-label>{{ 'Description' | t }}</mat-label>
            <textarea matInput formControlName="description" rows="4"></textarea>
          </mat-form-field>
          <mat-form-field appearance="outline" class="full-width">
            <mat-label>{{ 'Mission (optionnel)' | t }}</mat-label>
            <textarea matInput formControlName="mission" rows="3"></textarea>
          </mat-form-field>
          <mat-form-field appearance="outline" class="full-width">
            <mat-label>{{ 'Domaine' | t }}</mat-label>
            <mat-select formControlName="domain">
              @for (d of domains; track d.value) {
                <mat-option [value]="d.value">{{ d.label | t }}</mat-option>
              }
            </mat-select>
          </mat-form-field>

          <h3>{{ 'Localisation' | t }}</h3>
          <div class="row">
            <mat-form-field appearance="outline" class="flex-2">
              <mat-label>{{ 'Ville' | t }}</mat-label>
              <input matInput formControlName="addressCity" />
            </mat-form-field>
            <mat-form-field appearance="outline" class="flex-1">
              <mat-label>{{ 'Code postal' | t }}</mat-label>
              <input matInput formControlName="addressZip" />
            </mat-form-field>
          </div>

          <div class="map-block">
            <label class="map-label">{{ 'Localiser le bureau sur la carte' | t }}</label>
            <app-location-picker
              [lat]="form.get('addressLat')?.value"
              [lng]="form.get('addressLng')?.value"
              (coordsChange)="onCoords($event)"
              (addressResolved)="onAddressResolved($event)" />
          </div>

          <h3>{{ 'Contact' | t }}</h3>
          <mat-form-field appearance="outline" class="full-width">
            <mat-label>{{ 'Email de contact' | t }}</mat-label>
            <input matInput formControlName="email" type="email" />
          </mat-form-field>
          <div class="row">
            <mat-form-field appearance="outline" class="flex-1">
              <mat-label>{{ 'Téléphone (optionnel)' | t }}</mat-label>
              <input matInput formControlName="phone" />
            </mat-form-field>
            <mat-form-field appearance="outline" class="flex-1">
              <mat-label>{{ 'Site web (optionnel)' | t }}</mat-label>
              <input matInput formControlName="website" />
            </mat-form-field>
          </div>

          <button mat-flat-button type="submit" class="full-width submit-btn" [disabled]="loading()">
            @if (loading()) { <mat-spinner diameter="20" /> } @else { {{ 'Soumettre l\\'organisation' | t }} }
          </button>
        </form>
      </mat-card>
    </div>
  `,
  styles: [`
    h1 { font-size: 1.8rem; font-weight: 800; margin: 0; letter-spacing: -0.02em; }
    .subtitle { color: var(--brand-text-soft); margin: 4px 0 24px; }
    h3 { font-size: 1rem; font-weight: 700; margin: 24px 0 12px; color: var(--brand-ink); }
    .form-card { padding: 32px; }
    .full-width { width: 100%; }
    .row { display: flex; gap: 16px; flex-wrap: wrap; }
    .flex-1 { flex: 1; min-width: 140px; }
    .flex-2 { flex: 2; min-width: 180px; }
    .map-block { margin: 8px 0 8px; }
    .map-label { display: block; font-size: 0.9rem; font-weight: 700; color: var(--brand-ink); margin-bottom: 8px; }
    .submit-btn { height: 48px; font-size: 16px; margin-top: 16px; border-radius: var(--radius-md); }
    .error-banner { background: var(--brand-danger-soft); color: var(--brand-danger); padding: 12px 14px; border-radius: var(--radius-sm); margin-bottom: 16px; font-size: 0.9rem; }
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
      addressLat: [null],
      addressLng: [null],
      email: ['', [Validators.required, Validators.email]],
      phone: [''],
      website: [''],
    });
  }

  onCoords(c: { lat: number; lng: number }) {
    this.form.patchValue({ addressLat: c.lat, addressLng: c.lng });
  }

  onAddressResolved(r: GeoResult) {
    const patch: any = {};
    if (r.city && !this.form.value.addressCity) patch.addressCity = r.city;
    if (r.zip && !this.form.value.addressZip) patch.addressZip = r.zip;
    if (r.street && !this.form.value.addressStreet) patch.addressStreet = r.street;
    if (Object.keys(patch).length) this.form.patchValue(patch);
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

