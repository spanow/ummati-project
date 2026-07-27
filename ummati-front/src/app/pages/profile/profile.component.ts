import { Component, signal, OnInit, computed } from '@angular/core';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
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
import { SkillService, Skill } from '../../core/services/skill.service';
import { TPipe } from '../../shared/pipes/t.pipe';

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [
    ReactiveFormsModule, MatCardModule, MatButtonModule, MatIconModule,
    MatFormFieldModule, MatInputModule, MatChipsModule, MatTabsModule,
    MatProgressSpinnerModule, MatSnackBarModule, MatDividerModule, RouterLink, TPipe,
  ],
  template: `
    @if (loading()) {
      <div class="state-center"><mat-spinner diameter="40" /></div>
    } @else if (profile()) {
      <div class="page page-narrow">
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
            <label class="avatar-upload" [title]="'Changer la photo' | t">
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
            <a mat-stroked-button class="passport-link" [routerLink]="['/volunteers', profile()!.id]">
              <mat-icon>badge</mat-icon>
              {{ 'Mon passeport bénévole' | t }}
              @if (!profile()!.profilePublic) {
                <span class="passport-private">{{ 'privé' | t }}</span>
              }
            </a>
          </div>
          <div class="stats-row">
            <div class="stat">
              <span class="stat-val">{{ profile()!.stats.organizationCount }}</span>
              <span class="stat-lbl">{{ 'ONG' | t }}</span>
            </div>
            <div class="stat">
              <span class="stat-val">{{ profile()!.stats.eventsAttended }}</span>
              <span class="stat-lbl">{{ 'Événements' | t }}</span>
            </div>
            <div class="stat">
              <span class="stat-val">{{ profile()!.stats.volunteerHours }}</span>
              <span class="stat-lbl">{{ 'Heures' | t }}</span>
            </div>
          </div>
        </div>

        <!-- Attestation de bénévolat -->
        @if (profile()!.stats.eventsAttended > 0) {
          <div class="attestation-cta">
            <div class="att-text">
              <strong>{{ 'Attestation de bénévolat' | t }}</strong>
              <p>{{ 'Téléchargez le justificatif de vos heures validées (PDF).' | t }}</p>
            </div>
            <button mat-flat-button (click)="downloadAttestation()" [disabled]="downloadingAtt()">
              @if (downloadingAtt()) { <mat-spinner diameter="18" /> } @else { <mat-icon>picture_as_pdf</mat-icon> }
              {{ 'Télécharger' | t }}
            </button>
          </div>
        }

        <!-- Skills -->
        @if (profile()!.skills.length > 0) {
          <div class="section">
            <h3>{{ 'Compétences' | t }}</h3>
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
          <mat-tab [label]="'Informations' | t">
            <form [formGroup]="infoForm" (ngSubmit)="saveInfo()" class="tab-form">
              <div class="form-row">
                <mat-form-field appearance="outline">
                  <mat-label>{{ 'Prénom' | t }}</mat-label>
                  <input matInput formControlName="firstName" />
                </mat-form-field>
                <mat-form-field appearance="outline">
                  <mat-label>{{ 'Nom' | t }}</mat-label>
                  <input matInput formControlName="lastName" />
                </mat-form-field>
              </div>
              <mat-form-field appearance="outline" class="full-width">
                <mat-label>{{ 'Téléphone' | t }}</mat-label>
                <input matInput formControlName="phone" />
              </mat-form-field>
              <mat-form-field appearance="outline" class="full-width">
                <mat-label>{{ 'Bio' | t }}</mat-label>
                <textarea matInput formControlName="bio" rows="3"></textarea>
              </mat-form-field>
              <div class="form-row">
                <mat-form-field appearance="outline" class="flex-2">
                  <mat-label>{{ 'Ville' | t }}</mat-label>
                  <input matInput formControlName="city" />
                </mat-form-field>
                <mat-form-field appearance="outline" class="flex-1">
                  <mat-label>{{ 'Code postal' | t }}</mat-label>
                  <input matInput formControlName="zip" />
                </mat-form-field>
              </div>

              <div class="skills-edit">
                <h4>{{ 'Mes compétences' | t }}</h4>
                <p class="skills-hint">{{ 'Sélectionnez les compétences que vous souhaitez mettre à disposition des associations.' | t }}</p>
                <div class="skills-grid">
                  @for (skill of allSkills(); track skill.id) {
                    <mat-chip-option [selected]="selectedSkillIds.has(skill.id)" (click)="toggleSkill(skill.id)">
                      {{ skill.name }}
                    </mat-chip-option>
                  }
                </div>
              </div>

              @if (infoError()) { <div class="error-banner">{{ infoError() }}</div> }
              <button mat-flat-button type="submit" [disabled]="savingInfo()">
                @if (savingInfo()) { <mat-spinner diameter="18" /> } @else { {{ 'Sauvegarder' | t }} }
              </button>
            </form>
          </mat-tab>

          <mat-tab [label]="'Mot de passe' | t">
            <form [formGroup]="passwordForm" (ngSubmit)="changePassword()" class="tab-form">
              <mat-form-field appearance="outline" class="full-width">
                <mat-label>{{ 'Mot de passe actuel' | t }}</mat-label>
                <input matInput formControlName="currentPassword" type="password" />
              </mat-form-field>
              <mat-form-field appearance="outline" class="full-width">
                <mat-label>{{ 'Nouveau mot de passe' | t }}</mat-label>
                <input matInput formControlName="newPassword" type="password" />
                <mat-hint>{{ 'Min. 8 caractères, 1 majuscule, 1 chiffre, 1 spécial' | t }}</mat-hint>
              </mat-form-field>
              @if (pwdError()) { <div class="error-banner">{{ pwdError() }}</div> }
              @if (pwdSuccess()) { <div class="success-banner">{{ pwdSuccess() }}</div> }
              <button mat-flat-button type="submit" [disabled]="savingPwd()">
                @if (savingPwd()) { <mat-spinner diameter="18" /> } @else { {{ 'Changer le mot de passe' | t }} }
              </button>
            </form>
          </mat-tab>
        </mat-tab-group>
      </div>
    }
  `,
  styles: [`
    .profile-hero { display: flex; align-items: flex-start; gap: 24px; margin-bottom: 32px; flex-wrap: wrap; }
    .avatar-wrapper { position: relative; flex-shrink: 0; }
    .avatar { width: 88px; height: 88px; border-radius: 50%; object-fit: cover; border: 3px solid var(--brand-surface); box-shadow: var(--brand-shadow-md); }
    .avatar-placeholder { width: 88px; height: 88px; border-radius: 50%; background: var(--brand-gradient); color: white; display: flex; align-items: center; justify-content: center; font-size: 1.8rem; font-weight: 700; }
    .avatar-upload { position: absolute; bottom: 0; right: 0; width: 28px; height: 28px; background: var(--brand-surface); border-radius: 50%; border: 2px solid var(--brand-border-strong); display: flex; align-items: center; justify-content: center; cursor: pointer; box-shadow: var(--brand-shadow-sm); }
    .avatar-upload mat-icon { font-size: 16px; width: 16px; height: 16px; color: var(--brand-text); }
    .hero-info { flex: 1; }
    .hero-info h1 { font-size: 1.6rem; font-weight: 800; margin: 0 0 6px; letter-spacing: -0.02em; }
    .location { display: flex; align-items: center; gap: 4px; color: var(--brand-text-soft); font-size: 0.9rem; margin: 0; }
    .location mat-icon { font-size: 16px; width: 16px; height: 16px; }
    .bio { color: var(--brand-text); line-height: 1.6; margin: 8px 0 0; font-size: 0.95rem; }
    .passport-link { margin-top: 14px; }
    .passport-private {
      margin-inline-start: 8px; padding: 1px 8px; border-radius: 10px;
      background: var(--brand-surface-3); color: var(--brand-text-soft);
      font-size: 0.72rem; font-weight: 700; text-transform: uppercase; letter-spacing: 0.04em;
    }
    .stats-row { display: flex; gap: 16px; margin-left: auto; flex-wrap: wrap; }
    .stat { display: flex; flex-direction: column; align-items: center; padding: 12px 20px; background: var(--brand-surface-2); border: 1px solid var(--brand-border); border-radius: var(--radius-sm); }
    .stat-val { font-size: 1.5rem; font-weight: 800; color: var(--brand-primary); }
    .stat-lbl { font-size: 0.75rem; color: var(--brand-text-soft); margin-top: 2px; }
    .section { margin: 24px 0; }
    .section h3 { font-size: 1rem; font-weight: 700; margin: 0 0 12px; }
    .chips-row { display: flex; flex-wrap: wrap; gap: 8px; }
    .edit-tabs { margin-top: 24px; }
    .tab-form { padding: 24px 0; max-width: 540px; display: flex; flex-direction: column; gap: 8px; }
    .form-row { display: flex; gap: 16px; }
    .form-row mat-form-field { flex: 1; }
    .flex-1 { flex: 1; }
    .flex-2 { flex: 2; }
    .full-width { width: 100%; }
    .error-banner { background: var(--brand-danger-soft); color: var(--brand-danger); padding: 10px 14px; border-radius: var(--radius-sm); font-size: 0.9rem; }
    .success-banner { background: var(--brand-success-soft); color: var(--brand-success); padding: 10px 14px; border-radius: var(--radius-sm); font-size: 0.9rem; }
    .skills-edit { margin: 8px 0 16px; }
    .skills-edit h4 { font-size: 0.95rem; font-weight: 700; margin: 0 0 4px; }
    .skills-hint { color: var(--brand-text-soft); font-size: 0.85rem; margin: 0 0 12px; }
    .skills-grid { display: flex; flex-wrap: wrap; gap: 8px; }
    .attestation-cta { display: flex; align-items: center; justify-content: space-between; gap: 16px;
      background: var(--brand-primary-soft, #f0f7f3); border: 1px solid var(--brand-primary-100, #cfe6da);
      border-radius: 10px; padding: 14px 18px; margin: 8px 0 24px; flex-wrap: wrap; }
    .att-text strong { font-size: 0.95rem; }
    .att-text p { margin: 2px 0 0; font-size: 0.85rem; color: #666; }
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
  allSkills = signal<Skill[]>([]);
  selectedSkillIds = new Set<string>();
  downloadingAtt = signal(false);

  infoForm: FormGroup;
  passwordForm: FormGroup;

  constructor(
    private fb: FormBuilder,
    private profileService: ProfileService,
    private skillService: SkillService,
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
      next: res => {
        this.profile.set(res.data);
        this.infoForm.patchValue({
          firstName: res.data.firstName,
          lastName: res.data.lastName,
          phone: res.data.phone ?? '',
          bio: res.data.bio ?? '',
          city: res.data.address.city ?? '',
          zip: res.data.address.zip ?? '',
        });
        this.selectedSkillIds = new Set(res.data.skills.map(s => s.id));
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
    this.skillService.getAll().subscribe({
      next: res => this.allSkills.set(res.data ?? []),
      error: () => {},
    });
  }

  toggleSkill(id: string) {
    if (this.selectedSkillIds.has(id)) this.selectedSkillIds.delete(id);
    else this.selectedSkillIds.add(id);
  }

  saveInfo() {
    if (this.infoForm.invalid) return;
    this.savingInfo.set(true);
    this.infoError.set('');
    const v = this.infoForm.value;
    this.profileService.update({
      firstName: v.firstName, lastName: v.lastName, phone: v.phone, bio: v.bio,
      address: { city: v.city, zip: v.zip },
      skillIds: Array.from(this.selectedSkillIds),
    }).subscribe({
      next: res => {
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
      next: res => {
        this.profile.update(p => p ? { ...p, photoUrl: res.data.photoUrl } : p);
        this.snackBar.open('Photo mise à jour !', '', { duration: 3000 });
      },
      error: (err: any) => this.snackBar.open(err.error?.message ?? 'Erreur upload', '', { duration: 4000 }),
    });
  }

  downloadAttestation() {
    this.downloadingAtt.set(true);
    this.profileService.downloadAttestation().subscribe({
      next: blob => {
        this.downloadingAtt.set(false);
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = 'attestation-benevolat.pdf';
        a.click();
        window.URL.revokeObjectURL(url);
      },
      error: () => {
        this.downloadingAtt.set(false);
        this.snackBar.open('Erreur lors de la génération de l\'attestation', 'OK', { duration: 3000 });
      },
    });
  }
}






