import { Component, signal, OnInit, computed } from '@angular/core';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatChipsModule } from '@angular/material/chips';
import { MatTabsModule } from '@angular/material/tabs';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatDividerModule } from '@angular/material/divider';
import { ProfileService, ProfileResponse } from '../../core/services/profile.service';

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [
    ReactiveFormsModule, MatCardModule, MatButtonModule, MatIconModule,
    MatFormFieldModule, MatInputModule, MatChipsModule, MatTabsModule,
    MatProgressSpinnerModule, MatSnackBarModule, MatDividerModule,
  ],
  template: `
    @if (loading()) {
      <div class="loading"><mat-spinner diameter="40" /></div>
    } @else if (profile()) {
      <div class="page-container">
        <!-- Profile header -->
        <div class="profile-hero">
          <div class="avatar-wrapper">
            @if (profile()!.photoUrl) {
              <img [src]="profile()!.photoUrl" class="avatar" />
            } @else {
              <div class="avatar-placeholder">
                {{ profile()!.firstName[0] }}{{ profile()!.lastName[0] }}
              </div>
            }
            <label class="avatar-upload" title="Changer la photo">
              <mat-icon>photo_camera</mat-icon>
              <input type="file" accept="image/jpeg,image/png" (change)="onPhotoChange($event)" hidden />
            </label>
          </div>
          <div class="hero-info">
            <h1>{{ profile()!.firstName }} {{ profile()!.lastName }}</h1>
            @if (profile()!.address.city) {
              <p class="location"><mat-icon>location_on</mat-icon> {{ profile()!.address.city }}</p>
            }
            @if (profile()!.bio) {
              <p class="bio">{{ profile()!.bio }}</p>
            }
          </div>
          <div class="stats-row">
            <div class="stat">
              <span class="stat-val">{{ profile()!.stats.organizationCount }}</span>
              <span class="stat-lbl">ONG</span>
            </div>
            <div class="stat">
              <span class="stat-val">{{ profile()!.stats.eventsAttended }}</span>
              <span class="stat-lbl">Événements</span>
            </div>
            <div class="stat">
              <span class="stat-val">{{ profile()!.stats.volunteerHours }}</span>
              <span class="stat-lbl">Heures</span>
            </div>
          </div>
        </div>

        <!-- Skills -->
        @if (profile()!.skills.length > 0) {
          <div class="section">
            <h3>Compétences</h3>
            <div class="chips-row">
              @for (skill of profile()!.skills; track skill.id) {
                <mat-chip>{{ skill.name }}</mat-chip>
              }
            </div>
          </div>
        }

        <mat-divider />

        <!-- Edit form -->
        <mat-tab-group class="edit-tabs">
          <mat-tab label="Informations">
            <form [formGroup]="infoForm" (ngSubmit)="saveInfo()" class="tab-form">
              <div class="form-row">
                <mat-form-field appearance="outline">
                  <mat-label>Prénom</mat-label>
                  <input matInput formControlName="firstName" />
                </mat-form-field>
                <mat-form-field appearance="outline">
                  <mat-label>Nom</mat-label>
                  <input matInput formControlName="lastName" />
                </mat-form-field>
              </div>
              <mat-form-field appearance="outline" class="full-width">
                <mat-label>Téléphone</mat-label>
                <input matInput formControlName="phone" />
              </mat-form-field>
              <mat-form-field appearance="outline" class="full-width">
                <mat-label>Bio</mat-label>
                <textarea matInput formControlName="bio" rows="3"></textarea>
              </mat-form-field>
              <div class="form-row">
                <mat-form-field appearance="outline" class="flex-2">
                  <mat-label>Ville</mat-label>
                  <input matInput formControlName="city" />
                </mat-form-field>
                <mat-form-field appearance="outline" class="flex-1">
                  <mat-label>Code postal</mat-label>
                  <input matInput formControlName="zip" />
                </mat-form-field>
              </div>
              @if (infoError()) { <div class="error-banner">{{ infoError() }}</div> }
              <button mat-flat-button type="submit" [disabled]="savingInfo()">
                @if (savingInfo()) { <mat-spinner diameter="18" /> } @else { Sauvegarder }
              </button>
            </form>
          </mat-tab>

          <mat-tab label="Mot de passe">
            <form [formGroup]="passwordForm" (ngSubmit)="changePassword()" class="tab-form">
              <mat-form-field appearance="outline" class="full-width">
                <mat-label>Mot de passe actuel</mat-label>
                <input matInput formControlName="currentPassword" type="password" />
              </mat-form-field>
              <mat-form-field appearance="outline" class="full-width">
                <mat-label>Nouveau mot de passe</mat-label>
                <input matInput formControlName="newPassword" type="password" />
                <mat-hint>Min. 8 caractères, 1 majuscule, 1 chiffre, 1 spécial</mat-hint>
              </mat-form-field>
              @if (pwdError()) { <div class="error-banner">{{ pwdError() }}</div> }
              @if (pwdSuccess()) { <div class="success-banner">{{ pwdSuccess() }}</div> }
              <button mat-flat-button type="submit" [disabled]="savingPwd()">
                @if (savingPwd()) { <mat-spinner diameter="18" /> } @else { Changer le mot de passe }
              </button>
            </form>
          </mat-tab>
        </mat-tab-group>
      </div>
    }
  `,
  styles: [`
    .loading { display: flex; justify-content: center; padding: 120px 0; }
    .page-container { max-width: 800px; margin: 0 auto; padding: 32px 24px; }
    .profile-hero { display: flex; align-items: flex-start; gap: 24px; margin-bottom: 32px; flex-wrap: wrap; }
    .avatar-wrapper { position: relative; flex-shrink: 0; }
    .avatar { width: 88px; height: 88px; border-radius: 50%; object-fit: cover; border: 3px solid white; box-shadow: 0 2px 12px rgba(0,0,0,0.15); }
    .avatar-placeholder { width: 88px; height: 88px; border-radius: 50%; background: linear-gradient(135deg, #1976d2, #42a5f5); color: white; display: flex; align-items: center; justify-content: center; font-size: 1.8rem; font-weight: 600; }
    .avatar-upload { position: absolute; bottom: 0; right: 0; width: 28px; height: 28px; background: white; border-radius: 50%; border: 2px solid #e0e0e0; display: flex; align-items: center; justify-content: center; cursor: pointer; box-shadow: 0 1px 4px rgba(0,0,0,0.15); }
    .avatar-upload mat-icon { font-size: 16px; width: 16px; height: 16px; color: #555; }
    .hero-info { flex: 1; }
    .hero-info h1 { font-size: 1.6rem; font-weight: 600; margin: 0 0 6px; }
    .location { display: flex; align-items: center; gap: 4px; color: #888; font-size: 0.9rem; margin: 0; }
    .location mat-icon { font-size: 16px; width: 16px; height: 16px; }
    .bio { color: #555; line-height: 1.6; margin: 8px 0 0; font-size: 0.95rem; }
    .stats-row { display: flex; gap: 24px; margin-left: auto; }
    .stat { display: flex; flex-direction: column; align-items: center; padding: 12px 20px; background: #f8f9fa; border-radius: 10px; }
    .stat-val { font-size: 1.5rem; font-weight: 700; color: #1976d2; }
    .stat-lbl { font-size: 0.75rem; color: #888; margin-top: 2px; }
    .section { margin: 24px 0; }
    .section h3 { font-size: 1rem; font-weight: 600; margin: 0 0 12px; }
    .chips-row { display: flex; flex-wrap: wrap; gap: 8px; }
    .edit-tabs { margin-top: 24px; }
    .tab-form { padding: 24px 0; max-width: 540px; display: flex; flex-direction: column; gap: 8px; }
    .form-row { display: flex; gap: 16px; }
    .form-row mat-form-field { flex: 1; }
    .flex-1 { flex: 1; }
    .flex-2 { flex: 2; }
    .full-width { width: 100%; }
    .error-banner { background: #fdecea; color: #d32f2f; padding: 10px 14px; border-radius: 6px; font-size: 0.9rem; }
    .success-banner { background: #e8f5e9; color: #2e7d32; padding: 10px 14px; border-radius: 6px; font-size: 0.9rem; }
  `],
})
export class ProfileComponent implements OnInit {
  profile = signal<ProfileResponse | null>(null);
  loading = signal(true);
  savingInfo = signal(false);
  savingPwd = signal(false);
  infoError = signal('');
  pwdError = signal('');
  pwdSuccess = signal('');

  infoForm: FormGroup;
  passwordForm: FormGroup;

  constructor(
    private fb: FormBuilder,
    private profileService: ProfileService,
    private snackBar: MatSnackBar,
  ) {
    this.infoForm = this.fb.group({
      firstName: ['', Validators.required],
      lastName: ['', Validators.required],
      phone: [''],
      bio: [''],
      city: [''],
      zip: [''],
    });
    this.passwordForm = this.fb.group({
      currentPassword: ['', Validators.required],
      newPassword: ['', [Validators.required, Validators.minLength(8),
        Validators.pattern(/^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&#])/)]],
    });
  }

  ngOnInit() {
    this.profileService.get().subscribe({
      next: (res: any) => {
        this.profile.set(res.data);
        this.infoForm.patchValue({
          firstName: res.data.firstName,
          lastName: res.data.lastName,
          phone: res.data.phone ?? '',
          bio: res.data.bio ?? '',
          city: res.data.address.city ?? '',
          zip: res.data.address.zip ?? '',
        });
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  saveInfo() {
    if (this.infoForm.invalid) return;
    this.savingInfo.set(true);
    this.infoError.set('');
    const v = this.infoForm.value;
    this.profileService.update({
      firstName: v.firstName, lastName: v.lastName, phone: v.phone, bio: v.bio,
      address: { city: v.city, zip: v.zip },
    }).subscribe({
      next: (res: any) => {
        this.profile.set(res.data);
        this.savingInfo.set(false);
        this.snackBar.open('Profil mis à jour !', '', { duration: 3000 });
      },
      error: (err: any) => {
        this.savingInfo.set(false);
        this.infoError.set(err.error?.message ?? 'Erreur lors de la sauvegarde');
      },
    });
  }

  changePassword() {
    if (this.passwordForm.invalid) return;
    this.savingPwd.set(true);
    this.pwdError.set('');
    this.pwdSuccess.set('');
    const { currentPassword, newPassword } = this.passwordForm.value;
    this.profileService.changePassword(currentPassword, newPassword).subscribe({
      next: () => {
        this.savingPwd.set(false);
        this.pwdSuccess.set('Mot de passe modifié avec succès !');
        this.passwordForm.reset();
      },
      error: (err: any) => {
        this.savingPwd.set(false);
        this.pwdError.set(err.error?.message ?? 'Erreur');
      },
    });
  }

  onPhotoChange(event: Event) {
    const file = (event.target as HTMLInputElement).files?.[0];
    if (!file) return;
    this.profileService.uploadPhoto(file).subscribe({
      next: (res: any) => {
        this.profile.update(p => p ? { ...p, photoUrl: res.data.photoUrl } : p);
        this.snackBar.open('Photo mise à jour !', '', { duration: 3000 });
      },
      error: (err: any) => this.snackBar.open(err.error?.message ?? 'Erreur upload', '', { duration: 4000 }),
    });
  }
}






