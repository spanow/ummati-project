import { Component, inject, signal, OnInit } from '@angular/core';
import { FormsModule, ReactiveFormsModule, FormBuilder, Validators, AbstractControl, ValidationErrors } from '@angular/forms';
import { RetentionService, MissionAlert, NotificationPreferences }
  from '../../core/services/retention.service';
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
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { Router } from '@angular/router';
import { ProfileService } from '../../core/services/profile.service';
import { AuthService } from '../../core/services/auth.service';
import { PushNotificationService } from '../../core/services/push-notification.service';
import { TPipe } from '../../shared/pipes/t.pipe';

function passwordMatch(control: AbstractControl): ValidationErrors | null {
  const pwd = control.get('newPassword')?.value;
  const confirm = control.get('confirmPassword')?.value;
  return pwd && confirm && pwd !== confirm ? { passwordMismatch: true } : null;
}

@Component({
  selector: 'app-settings',
  standalone: true,
  imports: [
    ReactiveFormsModule, FormsModule,
    MatCardModule, MatButtonModule, MatIconModule, MatInputModule,
    MatFormFieldModule, MatSnackBarModule, MatProgressSpinnerModule,
    MatDividerModule, MatDialogModule, MatTabsModule, MatSlideToggleModule, TPipe
  ],
  template: `
    <div class="page page-narrow" role="main">
      <div class="page-header">
        <h1>{{ 'Paramètres du compte' | t }}</h1>
        <p>{{ 'Gérez votre sécurité et vos préférences' | t }}</p>
      </div>

      <mat-tab-group animationDuration="200ms" aria-label="Sections des paramètres">

        <!-- Tab Mot de passe -->
        <mat-tab [label]="'Mot de passe' | t">
          <ng-template matTabContent>
            <mat-card class="settings-card">
              <mat-card-header>
                <mat-icon mat-card-avatar aria-hidden="true">lock</mat-icon>
                <mat-card-title>{{ 'Changer le mot de passe' | t }}</mat-card-title>
                <mat-card-subtitle>{{ 'Votre mot de passe doit comporter au moins 8 caractères' | t }}</mat-card-subtitle>
              </mat-card-header>
              <mat-card-content>
                <form [formGroup]="passwordForm" (ngSubmit)="changePassword()" aria-label="Formulaire changement de mot de passe">
                  <mat-form-field appearance="outline" class="full-width">
                    <mat-label>{{ 'Mot de passe actuel' | t }}</mat-label>
                    <input matInput [type]="showCurrentPwd() ? 'text' : 'password'"
                           formControlName="currentPassword" autocomplete="current-password"
                           aria-required="true">
                    <button mat-icon-button matSuffix type="button" (click)="showCurrentPwd.set(!showCurrentPwd())"
                            [attr.aria-label]="(showCurrentPwd() ? 'Masquer' : 'Afficher') | t">
                      <mat-icon>{{ showCurrentPwd() ? 'visibility_off' : 'visibility' }}</mat-icon>
                    </button>
                    @if (passwordForm.get('currentPassword')?.hasError('required') && passwordForm.get('currentPassword')?.touched) {
                      <mat-error role="alert">{{ 'Le mot de passe actuel est requis' | t }}</mat-error>
                    }
                  </mat-form-field>

                  <mat-form-field appearance="outline" class="full-width">
                    <mat-label>{{ 'Nouveau mot de passe' | t }}</mat-label>
                    <input matInput [type]="showNewPwd() ? 'text' : 'password'"
                           formControlName="newPassword" autocomplete="new-password"
                           aria-required="true">
                    <button mat-icon-button matSuffix type="button" (click)="showNewPwd.set(!showNewPwd())"
                            [attr.aria-label]="(showNewPwd() ? 'Masquer' : 'Afficher') | t">
                      <mat-icon>{{ showNewPwd() ? 'visibility_off' : 'visibility' }}</mat-icon>
                    </button>
                    @if (passwordForm.get('newPassword')?.hasError('minlength') && passwordForm.get('newPassword')?.touched) {
                      <mat-error role="alert">{{ 'Minimum 8 caractères' | t }}</mat-error>
                    }
                  </mat-form-field>

                  <mat-form-field appearance="outline" class="full-width">
                    <mat-label>{{ 'Confirmer le nouveau mot de passe' | t }}</mat-label>
                    <input matInput [type]="showNewPwd() ? 'text' : 'password'"
                           formControlName="confirmPassword" autocomplete="new-password"
                           aria-required="true">
                    @if (passwordForm.hasError('passwordMismatch') && passwordForm.get('confirmPassword')?.touched) {
                      <mat-error role="alert">{{ 'Les mots de passe ne correspondent pas' | t }}</mat-error>
                    }
                  </mat-form-field>

                  <button mat-flat-button color="primary" type="submit"
                          [disabled]="passwordForm.invalid || pwdLoading()"
                          aria-label="Mettre à jour le mot de passe">
                    @if (pwdLoading()) {
                      <mat-spinner diameter="20"></mat-spinner>
                    } @else {
                      <ng-container>
                        <mat-icon>save</mat-icon> {{ 'Mettre à jour' | t }}
                      </ng-container>
                    }
                  </button>
                </form>
              </mat-card-content>
            </mat-card>
          </ng-template>
        </mat-tab>

        <!-- Tab Notifications -->
        <mat-tab [label]="'Notifications' | t">
          <ng-template matTabContent>
            <mat-card class="settings-card">
              <mat-card-header>
                <mat-icon mat-card-avatar aria-hidden="true">notifications</mat-icon>
                <mat-card-title>{{ 'Notifications push' | t }}</mat-card-title>
                <mat-card-subtitle>{{ 'Recevez une alerte dans votre navigateur pour les événements importants' | t }}</mat-card-subtitle>
              </mat-card-header>
              <mat-card-content>
                @if (pushUnsupported()) {
                  <p class="push-hint">{{ 'Votre navigateur ne supporte pas les notifications push.' | t }}</p>
                } @else {
                  <div class="push-toggle-row">
                    <mat-slide-toggle [checked]="pushEnabled()" [disabled]="pushLoading()" (change)="togglePush($event.checked)">
                      {{ 'Activer les notifications push' | t }}
                    </mat-slide-toggle>
                  </div>
                  <p class="push-hint">{{ 'Vous serez notifié pour les nouvelles annonces, inscriptions et mises à jour de vos événements.' | t }}</p>
                }
              </mat-card-content>
            </mat-card>

            <mat-card class="settings-card">
              <mat-card-header>
                <mat-icon mat-card-avatar aria-hidden="true">mail</mat-icon>
                <mat-card-title>{{ 'Emails' | t }}</mat-card-title>
                <mat-card-subtitle>{{ 'Choisissez ce que vous souhaitez recevoir' | t }}</mat-card-subtitle>
              </mat-card-header>
              <mat-card-content>
                @if (prefs(); as p) {
                  <div class="pref-row">
                    <mat-slide-toggle [checked]="p.emailNewMissions"
                                      (change)="updatePref('NEW_MISSIONS', $event.checked)">
                      {{ 'Nouvelles missions' | t }}
                    </mat-slide-toggle>
                    <span class="pref-hint">{{ 'Alertes, associations suivies, missions mises de côté' | t }}</span>
                  </div>
                  <div class="pref-row">
                    <mat-slide-toggle [checked]="p.emailReminders"
                                      (change)="updatePref('REMINDERS', $event.checked)">
                      {{ 'Rappels' | t }}
                    </mat-slide-toggle>
                    <span class="pref-hint">{{ 'Avant vos missions, et demande d\\'avis après' | t }}</span>
                  </div>
                  <div class="pref-row">
                    <mat-slide-toggle [checked]="p.emailMemberships"
                                      (change)="updatePref('MEMBERSHIPS', $event.checked)">
                      {{ 'Adhésions' | t }}
                    </mat-slide-toggle>
                    <span class="pref-hint">{{ 'Suites données à vos demandes d\\'adhésion' | t }}</span>
                  </div>
                  <div class="pref-row">
                    <mat-slide-toggle [checked]="p.emailAnnouncements"
                                      (change)="updatePref('ANNOUNCEMENTS', $event.checked)">
                      {{ 'Annonces' | t }}
                    </mat-slide-toggle>
                    <span class="pref-hint">{{ 'Messages publiés par les associations' | t }}</span>
                  </div>

                  <p class="push-hint transactional-note">
                    <mat-icon>lock</mat-icon>
                    {{ 'Les emails liés à vos inscriptions — confirmation, annulation d\\'une mission — vous parviennent toujours.' | t }}
                  </p>
                }
              </mat-card-content>
            </mat-card>
          </ng-template>
        </mat-tab>

        <mat-tab [label]="'Alertes' | t">
          <ng-template matTabContent>
            <mat-card class="settings-card">
              <mat-card-header>
                <mat-icon mat-card-avatar aria-hidden="true">notifications_active</mat-icon>
                <mat-card-title>{{ 'Mes alertes missions' | t }}</mat-card-title>
                <mat-card-subtitle>{{ 'Soyez prévenu quand une mission correspond à ce que vous cherchez' | t }}</mat-card-subtitle>
              </mat-card-header>
              <mat-card-content>
                @if (alerts().length === 0) {
                  <p class="push-hint">{{ 'Aucune alerte pour le moment. Créez-en une pour ne plus surveiller la liste vous-même.' | t }}</p>
                } @else {
                  <ul class="alert-list">
                    @for (a of alerts(); track a.id) {
                      <li class="alert-item">
                        <div class="alert-info">
                          <strong>{{ a.label }}</strong>
                          <span class="pref-hint">
                            {{ describeAlert(a) }} ·
                            {{ (a.frequency === 'DAILY' ? 'Chaque jour' : 'Chaque semaine') | t }}
                          </span>
                        </div>
                        <mat-slide-toggle [checked]="a.enabled" (change)="toggleAlert(a, $event.checked)"
                                          [attr.aria-label]="'Activer cette alerte' | t" />
                        <button mat-icon-button (click)="deleteAlert(a)" [attr.aria-label]="'Supprimer' | t">
                          <mat-icon>delete</mat-icon>
                        </button>
                      </li>
                    }
                  </ul>
                }

                <div class="alert-create">
                  <mat-form-field appearance="outline">
                    <mat-label>{{ 'Nom de l\\'alerte' | t }}</mat-label>
                    <input matInput [(ngModel)]="newAlertLabel" maxlength="120"
                           [placeholder]="'Environnement près de Lyon' | t" />
                  </mat-form-field>
                  <mat-form-field appearance="outline">
                    <mat-label>{{ 'Ville' | t }}</mat-label>
                    <input matInput [(ngModel)]="newAlertCity" placeholder="Lyon" />
                  </mat-form-field>
                  <button mat-flat-button [disabled]="newAlertLabel.trim().length < 2 || creatingAlert()"
                          (click)="createAlert()">
                    <mat-icon>add</mat-icon> {{ 'Créer' | t }}
                  </button>
                </div>
              </mat-card-content>
            </mat-card>
          </ng-template>
        </mat-tab>

        <!-- Tab Suppression compte -->
        <mat-tab [label]="'Supprimer le compte' | t">
          <ng-template matTabContent>
            <mat-card class="settings-card danger-card">
              <mat-card-header>
                <mat-icon mat-card-avatar color="warn" aria-hidden="true">warning</mat-icon>
                <mat-card-title>{{ 'Supprimer mon compte' | t }}</mat-card-title>
                <mat-card-subtitle>{{ 'Cette action est irréversible. Toutes vos données seront anonymisées (RGPD).' | t }}</mat-card-subtitle>
              </mat-card-header>
              <mat-card-content>
                <div class="danger-info" role="note">
                  <mat-icon aria-hidden="true">info</mat-icon>
                  <p>{{ 'La suppression de votre compte entraîne :' | t }}</p>
                  <ul>
                    <li>{{ 'L\\'anonymisation de votre profil' | t }}</li>
                    <li>{{ 'La perte de vos inscriptions aux événements' | t }}</li>
                    <li>{{ 'La sortie de toutes vos associations' | t }}</li>
                    <li>{{ 'La suppression définitive après 30 jours' | t }}</li>
                  </ul>
                </div>

                @if (!showDeleteForm()) {
                  <button mat-stroked-button color="warn"
                          (click)="showDeleteForm.set(true)"
                          aria-label="Afficher le formulaire de suppression">
                    <mat-icon>delete_forever</mat-icon>
                    {{ 'Supprimer mon compte' | t }}
                  </button>
                } @else {
                  <form [formGroup]="deleteForm" (ngSubmit)="deleteAccount()" aria-label="Formulaire suppression du compte">
                    <p class="confirm-label"><strong>{{ 'Entrez votre mot de passe pour confirmer :' | t }}</strong></p>
                    <mat-form-field appearance="outline" class="full-width">
                      <mat-label>{{ 'Mot de passe' | t }}</mat-label>
                      <input matInput type="password" formControlName="password"
                             autocomplete="current-password" aria-required="true">
                      @if (deleteForm.get('password')?.hasError('required') && deleteForm.get('password')?.touched) {
                        <mat-error role="alert">{{ 'Le mot de passe est requis' | t }}</mat-error>
                      }
                    </mat-form-field>
                    <div class="delete-actions">
                      <button mat-button type="button" (click)="showDeleteForm.set(false)" aria-label="Annuler">
                        {{ 'Annuler' | t }}
                      </button>
                      <button mat-flat-button color="warn" type="submit"
                              [disabled]="deleteForm.invalid || deleteLoading()"
                              aria-label="Confirmer la suppression du compte">
                      @if (deleteLoading()) {
                        <mat-spinner diameter="20"></mat-spinner>
                      } @else {
                        {{ 'Confirmer la suppression' | t }}
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
                <mat-card-title>{{ 'Exporter mes données (RGPD)' | t }}</mat-card-title>
                <mat-card-subtitle>{{ 'Téléchargez toutes vos données personnelles au format JSON' | t }}</mat-card-subtitle>
              </mat-card-header>
              <mat-card-content>
                <button mat-stroked-button (click)="exportData()" aria-label="Télécharger mes données">
                  <mat-icon>download</mat-icon>
                  {{ 'Télécharger mes données' | t }}
                </button>
              </mat-card-content>
            </mat-card>
          </ng-template>
        </mat-tab>

      </mat-tab-group>
    </div>
  `,
  styles: [`
    .page-header { margin-bottom: 32px; }
    .page-header h1 { font-size: 1.8rem; font-weight: 800; margin: 0 0 8px; color: var(--brand-ink); letter-spacing: -0.02em; }
    .page-header p { color: var(--brand-text-soft); margin: 0; }
    .settings-card { margin-top: 24px; }
    .full-width { width: 100%; margin-bottom: 16px; display: block; }
    form { padding-top: 16px; }
    .danger-card { border-left: 4px solid var(--brand-danger); }
    .danger-info {
      background: var(--brand-accent-soft); border-radius: var(--radius-sm); padding: 16px; margin-bottom: 24px;
      display: flex; flex-direction: column; gap: 8px;
    }
    .danger-info mat-icon { color: var(--brand-warn); }
    .danger-info ul { margin: 0; padding-left: 20px; }
    .danger-info li { margin-bottom: 4px; font-size: 0.9rem; }
    .confirm-label { margin-bottom: 12px; }
    .delete-actions { display: flex; gap: 12px; margin-top: 8px; }
    button[disabled] { opacity: 0.6; }
    mat-spinner { display: inline-block; margin-right: 8px; }
    .push-toggle-row { margin-bottom: 12px; }
    .push-hint { color: var(--brand-text-soft); font-size: 0.9rem; margin: 0; }

    .pref-row { display: flex; flex-direction: column; gap: 2px; padding: 12px 0; }
    .pref-row + .pref-row { border-top: 1px solid var(--brand-border); }
    .pref-hint { color: var(--brand-text-soft); font-size: 0.83rem; }
    .transactional-note {
      display: flex; align-items: center; gap: 8px; margin-top: 16px;
      padding: 10px 14px; border-radius: var(--radius-sm); background: var(--brand-surface-2);
    }
    .transactional-note mat-icon { font-size: 18px; width: 18px; height: 18px; flex: 0 0 auto; }

    .alert-list { list-style: none; margin: 0 0 20px; padding: 0; }
    .alert-item {
      display: flex; align-items: center; gap: 12px; padding: 12px 0;
      border-bottom: 1px solid var(--brand-border);
    }
    .alert-info { flex: 1; display: flex; flex-direction: column; gap: 2px; }
    .alert-create { display: flex; gap: 12px; align-items: flex-start; flex-wrap: wrap; }
    .alert-create mat-form-field { flex: 1; min-width: 180px; }
    .alert-create button { height: 54px; border-radius: var(--radius-md) !important; }
  `]
})
export class SettingsComponent implements OnInit {
  private fb = inject(FormBuilder);
  private profileService = inject(ProfileService);
  private authService = inject(AuthService);
  private snackBar = inject(MatSnackBar);
  private router = inject(Router);
  private pushNotificationService = inject(PushNotificationService);

  showCurrentPwd = signal(false);
  showNewPwd = signal(false);
  pwdLoading = signal(false);
  deleteLoading = signal(false);
  showDeleteForm = signal(false);
  pushEnabled = signal(false);
  pushLoading = signal(false);
  pushUnsupported = signal(false);

  private retention = inject(RetentionService);

  prefs = signal<NotificationPreferences | null>(null);
  alerts = signal<MissionAlert[]>([]);
  creatingAlert = signal(false);
  newAlertLabel = '';
  newAlertCity = '';

  // --- Préférences d'email ---

  private loadRetention() {
    this.retention.getPreferences().subscribe({
      next: res => this.prefs.set(res.data),
      error: () => {},
    });
    this.retention.listAlerts().subscribe({
      next: res => this.alerts.set(res.data),
      error: () => {},
    });
  }

  updatePref(category: string, value: boolean) {
    this.retention.updatePreference(category, value).subscribe({
      next: res => this.prefs.set(res.data),
      error: err => this.snackBar.open(err.error?.message || 'Erreur', 'OK', { duration: 3000 }),
    });
  }

  // --- Alertes ---

  /** Résumé du périmètre d'une alerte, pour l'afficher en une ligne. */
  describeAlert(alert: MissionAlert): string {
    if (alert.radiusKm && alert.lat != null) return `${alert.radiusKm} km autour de moi`;
    if (alert.city) return alert.city;
    return 'Partout';
  }

  createAlert() {
    const label = this.newAlertLabel.trim();
    if (label.length < 2) return;

    this.creatingAlert.set(true);
    this.retention.createAlert({
      label,
      city: this.newAlertCity.trim() || null,
      frequency: 'WEEKLY',
      enabled: true,
    }).subscribe({
      next: res => {
        this.alerts.update(list => [res.data, ...list]);
        this.newAlertLabel = '';
        this.newAlertCity = '';
        this.creatingAlert.set(false);
        this.snackBar.open('Alerte créée — vous serez prévenu chaque semaine', 'OK', { duration: 4000 });
      },
      error: err => {
        this.creatingAlert.set(false);
        this.snackBar.open(err.error?.message || 'Erreur', 'OK', { duration: 4000 });
      },
    });
  }

  toggleAlert(alert: MissionAlert, enabled: boolean) {
    this.retention.updateAlert(alert.id, { ...alert, enabled }).subscribe({
      next: res => this.alerts.update(list => list.map(a => a.id === alert.id ? res.data : a)),
      error: err => this.snackBar.open(err.error?.message || 'Erreur', 'OK', { duration: 3000 }),
    });
  }

  deleteAlert(alert: MissionAlert) {
    this.retention.deleteAlert(alert.id).subscribe({
      next: () => this.alerts.update(list => list.filter(a => a.id !== alert.id)),
      error: err => this.snackBar.open(err.error?.message || 'Erreur', 'OK', { duration: 3000 }),
    });
  }

  passwordForm = this.fb.group({
    currentPassword: ['', Validators.required],
    newPassword: ['', [Validators.required, Validators.minLength(8)]],
    confirmPassword: ['', Validators.required],
  }, { validators: passwordMatch });

  deleteForm = this.fb.group({
    password: ['', Validators.required],
  });

  ngOnInit() {
    // Chargé avant la sortie anticipée ci-dessous : les préférences d'email et les
    // alertes n'ont rien à voir avec la prise en charge du push par le navigateur.
    this.loadRetention();

    if (!this.pushNotificationService.isSupported()) {
      this.pushUnsupported.set(true);
      return;
    }
    this.pushNotificationService.isSubscribed().then(subscribed => this.pushEnabled.set(subscribed));
  }

  async togglePush(enable: boolean) {
    this.pushLoading.set(true);
    try {
      if (enable) {
        await this.pushNotificationService.subscribe();
        this.pushEnabled.set(true);
        this.snackBar.open('Notifications push activées', 'OK', { duration: 3000 });
      } else {
        await this.pushNotificationService.unsubscribe();
        this.pushEnabled.set(false);
        this.snackBar.open('Notifications push désactivées', 'OK', { duration: 3000 });
      }
    } catch (err: any) {
      this.pushEnabled.set(await this.pushNotificationService.isSubscribed());
      this.snackBar.open(err?.message ?? 'Une erreur est survenue', 'OK', { duration: 4000 });
    } finally {
      this.pushLoading.set(false);
    }
  }

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




