import { Component, inject, signal } from '@angular/core';
import { ReactiveFormsModule, FormBuilder, Validators, AbstractControl, ValidationErrors } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDividerModule } from '@angular/material/divider';
import { MatDialogModule, MatDialog } from '@angular/material/dialog';
import { MatTabsModule } from '@angular/material/tabs';
import { Router } from '@angular/router';
import { ProfileService } from '../../core/services/profile.service';
import { AuthService } from '../../core/services/auth.service';

function passwordMatch(control: AbstractControl): ValidationErrors | null {
  const pwd = control.get('newPassword')?.value;
  const confirm = control.get('confirmPassword')?.value;
  return pwd && confirm && pwd !== confirm ? { passwordMismatch: true } : null;
}

@Component({
  selector: 'app-settings',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    MatCardModule, MatButtonModule, MatIconModule, MatInputModule,
    MatFormFieldModule, MatSnackBarModule, MatProgressSpinnerModule,
    MatDividerModule, MatDialogModule, MatTabsModule
  ],
  template: `
    <div class="settings-container" role="main">
      <div class="page-header">
        <h1>Paramètres du compte</h1>
        <p>Gérez votre sécurité et vos préférences</p>
      </div>

      <mat-tab-group animationDuration="200ms" aria-label="Sections des paramètres">

        <!-- Tab Mot de passe -->
        <mat-tab label="Mot de passe">
          <ng-template matTabContent>
            <mat-card class="settings-card">
              <mat-card-header>
                <mat-icon mat-card-avatar aria-hidden="true">lock</mat-icon>
                <mat-card-title>Changer le mot de passe</mat-card-title>
                <mat-card-subtitle>Votre mot de passe doit comporter au moins 8 caractères</mat-card-subtitle>
              </mat-card-header>
              <mat-card-content>
                <form [formGroup]="passwordForm" (ngSubmit)="changePassword()" aria-label="Formulaire changement de mot de passe">
                  <mat-form-field appearance="outline" class="full-width">
                    <mat-label>Mot de passe actuel</mat-label>
                    <input matInput [type]="showCurrentPwd() ? 'text' : 'password'"
                           formControlName="currentPassword" autocomplete="current-password"
                           aria-required="true">
                    <button mat-icon-button matSuffix type="button" (click)="showCurrentPwd.set(!showCurrentPwd())"
                            [attr.aria-label]="showCurrentPwd() ? 'Masquer' : 'Afficher'">
                      <mat-icon>{{ showCurrentPwd() ? 'visibility_off' : 'visibility' }}</mat-icon>
                    </button>
                    @if (passwordForm.get('currentPassword')?.hasError('required') && passwordForm.get('currentPassword')?.touched) {
                      <mat-error role="alert">Le mot de passe actuel est requis</mat-error>
                    }
                  </mat-form-field>

                  <mat-form-field appearance="outline" class="full-width">
                    <mat-label>Nouveau mot de passe</mat-label>
                    <input matInput [type]="showNewPwd() ? 'text' : 'password'"
                           formControlName="newPassword" autocomplete="new-password"
                           aria-required="true">
                    <button mat-icon-button matSuffix type="button" (click)="showNewPwd.set(!showNewPwd())"
                            [attr.aria-label]="showNewPwd() ? 'Masquer' : 'Afficher'">
                      <mat-icon>{{ showNewPwd() ? 'visibility_off' : 'visibility' }}</mat-icon>
                    </button>
                    @if (passwordForm.get('newPassword')?.hasError('minlength') && passwordForm.get('newPassword')?.touched) {
                      <mat-error role="alert">Minimum 8 caractères</mat-error>
                    }
                  </mat-form-field>

                  <mat-form-field appearance="outline" class="full-width">
                    <mat-label>Confirmer le nouveau mot de passe</mat-label>
                    <input matInput [type]="showNewPwd() ? 'text' : 'password'"
                           formControlName="confirmPassword" autocomplete="new-password"
                           aria-required="true">
                    @if (passwordForm.hasError('passwordMismatch') && passwordForm.get('confirmPassword')?.touched) {
                      <mat-error role="alert">Les mots de passe ne correspondent pas</mat-error>
                    }
                  </mat-form-field>

                  <button mat-flat-button color="primary" type="submit"
                          [disabled]="passwordForm.invalid || pwdLoading()"
                          aria-label="Mettre à jour le mot de passe">
                    @if (pwdLoading()) {
                      <mat-spinner diameter="20"></mat-spinner>
                    } @else {
                      <ng-container>
                        <mat-icon>save</mat-icon> Mettre à jour
                      </ng-container>
                    }
                  </button>
                </form>
              </mat-card-content>
            </mat-card>
          </ng-template>
        </mat-tab>

        <!-- Tab Suppression compte -->
        <mat-tab label="Supprimer le compte">
          <ng-template matTabContent>
            <mat-card class="settings-card danger-card">
              <mat-card-header>
                <mat-icon mat-card-avatar color="warn" aria-hidden="true">warning</mat-icon>
                <mat-card-title>Supprimer mon compte</mat-card-title>
                <mat-card-subtitle>Cette action est irréversible. Toutes vos données seront anonymisées (RGPD).</mat-card-subtitle>
              </mat-card-header>
              <mat-card-content>
                <div class="danger-info" role="note">
                  <mat-icon aria-hidden="true">info</mat-icon>
                  <p>La suppression de votre compte entraîne :</p>
                  <ul>
                    <li>L'anonymisation de votre profil</li>
                    <li>La perte de vos inscriptions aux événements</li>
                    <li>La sortie de toutes vos associations</li>
                    <li>La suppression définitive après 30 jours</li>
                  </ul>
                </div>

                @if (!showDeleteForm()) {
                  <button mat-stroked-button color="warn"
                          (click)="showDeleteForm.set(true)"
                          aria-label="Afficher le formulaire de suppression">
                    <mat-icon>delete_forever</mat-icon>
                    Supprimer mon compte
                  </button>
                } @else {
                  <form [formGroup]="deleteForm" (ngSubmit)="deleteAccount()" aria-label="Formulaire suppression du compte">
                    <p class="confirm-label"><strong>Entrez votre mot de passe pour confirmer :</strong></p>
                    <mat-form-field appearance="outline" class="full-width">
                      <mat-label>Mot de passe</mat-label>
                      <input matInput type="password" formControlName="password"
                             autocomplete="current-password" aria-required="true">
                      @if (deleteForm.get('password')?.hasError('required') && deleteForm.get('password')?.touched) {
                        <mat-error role="alert">Le mot de passe est requis</mat-error>
                      }
                    </mat-form-field>
                    <div class="delete-actions">
                      <button mat-button type="button" (click)="showDeleteForm.set(false)" aria-label="Annuler">
                        Annuler
                      </button>
                      <button mat-flat-button color="warn" type="submit"
                              [disabled]="deleteForm.invalid || deleteLoading()"
                              aria-label="Confirmer la suppression du compte">
                      @if (deleteLoading()) {
                        <mat-spinner diameter="20"></mat-spinner>
                      } @else {
                        Confirmer la suppression
                      }
                      </button>
                    </div>
                  </form>
                }
              </mat-card-content>
            </mat-card>

            <!-- Export RGPD -->
            <mat-card class="settings-card">
              <mat-card-header>
                <mat-icon mat-card-avatar aria-hidden="true">download</mat-icon>
                <mat-card-title>Exporter mes données (RGPD)</mat-card-title>
                <mat-card-subtitle>Téléchargez toutes vos données personnelles au format JSON</mat-card-subtitle>
              </mat-card-header>
              <mat-card-content>
                <button mat-stroked-button (click)="exportData()" aria-label="Télécharger mes données">
                  <mat-icon>download</mat-icon>
                  Télécharger mes données
                </button>
              </mat-card-content>
            </mat-card>
          </ng-template>
        </mat-tab>

      </mat-tab-group>
    </div>
  `,
  styles: [`
    .settings-container { max-width: 700px; margin: 40px auto; padding: 0 20px; }
    .page-header { margin-bottom: 32px; }
    .page-header h1 { font-size: 1.8rem; font-weight: 700; margin: 0 0 8px; color: #1a1a2e; }
    .page-header p { color: #666; margin: 0; }
    .settings-card { margin-top: 24px; border-radius: 12px !important; }
    .full-width { width: 100%; margin-bottom: 16px; display: block; }
    form { padding-top: 16px; }
    .danger-card { border-left: 4px solid #f44336; }
    .danger-info {
      background: #fff3e0; border-radius: 8px; padding: 16px; margin-bottom: 24px;
      display: flex; flex-direction: column; gap: 8px;
    }
    .danger-info mat-icon { color: #f57c00; }
    .danger-info ul { margin: 0; padding-left: 20px; }
    .danger-info li { margin-bottom: 4px; font-size: 0.9rem; }
    .confirm-label { margin-bottom: 12px; }
    .delete-actions { display: flex; gap: 12px; margin-top: 8px; }
    button[disabled] { opacity: 0.6; }
    mat-spinner { display: inline-block; margin-right: 8px; }
  `]
})
export class SettingsComponent {
  private fb = inject(FormBuilder);
  private profileService = inject(ProfileService);
  private authService = inject(AuthService);
  private snackBar = inject(MatSnackBar);
  private router = inject(Router);

  showCurrentPwd = signal(false);
  showNewPwd = signal(false);
  pwdLoading = signal(false);
  deleteLoading = signal(false);
  showDeleteForm = signal(false);

  passwordForm = this.fb.group({
    currentPassword: ['', Validators.required],
    newPassword: ['', [Validators.required, Validators.minLength(8)]],
    confirmPassword: ['', Validators.required],
  }, { validators: passwordMatch });

  deleteForm = this.fb.group({
    password: ['', Validators.required],
  });

  changePassword() {
    if (this.passwordForm.invalid) return;
    this.pwdLoading.set(true);
    const { currentPassword, newPassword } = this.passwordForm.value;
    this.profileService.changePassword(currentPassword!, newPassword!).subscribe({
      next: () => {
        this.snackBar.open('Mot de passe mis à jour avec succès', 'OK', { duration: 4000 });
        this.passwordForm.reset();
        this.pwdLoading.set(false);
      },
      error: (err) => {
        const msg = err.error?.message ?? 'Erreur lors de la mise à jour';
        this.snackBar.open(msg, 'OK', { duration: 5000 });
        this.pwdLoading.set(false);
      }
    });
  }

  deleteAccount() {
    if (this.deleteForm.invalid) return;
    this.deleteLoading.set(true);
    this.profileService.delete(this.deleteForm.value.password!).subscribe({
      next: () => {
        this.authService.logout();
        this.snackBar.open('Compte supprimé. Au revoir !', 'OK', { duration: 5000 });
      },
      error: (err) => {
        const msg = err.error?.message ?? 'Erreur lors de la suppression';
        this.snackBar.open(msg, 'OK', { duration: 5000 });
        this.deleteLoading.set(false);
      }
    });
  }

  exportData() {
    window.open('/api/v1/profile/export', '_blank');
  }
}




