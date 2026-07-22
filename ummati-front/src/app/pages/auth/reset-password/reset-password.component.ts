import { Component, signal, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { AuthService } from '../../../core/services/auth.service';
import { TPipe } from '../../../shared/pipes/t.pipe';

@Component({
  selector: 'app-reset-password',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink, MatCardModule, MatFormFieldModule, MatInputModule, MatButtonModule, MatIconModule, MatProgressSpinnerModule, TPipe],
  template: `
    <div class="auth-container">
      <mat-card class="auth-card">
        <mat-card-header>
          <mat-card-title>{{ 'Nouveau mot de passe' | t }}</mat-card-title>
        </mat-card-header>
        <mat-card-content>
          @if (successMessage()) {
            <div class="success-banner">{{ successMessage() }} <a routerLink="/login">{{ 'Se connecter' | t }}</a></div>
          }
          @if (errorMessage()) {
            <div class="error-banner">{{ errorMessage() }}</div>
          }
          <form [formGroup]="form" (ngSubmit)="onSubmit()">
            <mat-form-field appearance="outline" class="full-width">
              <mat-label>{{ 'Nouveau mot de passe' | t }}</mat-label>
              <input matInput formControlName="newPassword" [type]="hidePassword() ? 'password' : 'text'" />
              <mat-hint>{{ 'Min. 8 caractères, 1 majuscule, 1 chiffre, 1 spécial' | t }}</mat-hint>
              <button mat-icon-button matSuffix type="button" (click)="hidePassword.set(!hidePassword())">
                <mat-icon>{{ hidePassword() ? 'visibility_off' : 'visibility' }}</mat-icon>
              </button>
            </mat-form-field>
            <button mat-flat-button color="primary" type="submit" class="full-width" [disabled]="loading()" style="margin-top:16px">
              @if (loading()) { <mat-spinner diameter="20" /> } @else { {{ 'Réinitialiser' | t }} }
            </button>
          </form>
        </mat-card-content>
      </mat-card>
    </div>
  `,
  styles: [`
    .auth-container { display: flex; justify-content: center; align-items: flex-start; padding: 56px 16px; }
    .auth-card { max-width: 440px; width: 100%; }
    .full-width { width: 100%; }
    .error-banner { background: var(--brand-danger-soft); color: var(--brand-danger); padding: 12px 14px; border-radius: var(--radius-sm); margin-bottom: 16px; font-size: 0.9rem; }
    .success-banner { background: var(--brand-success-soft); color: var(--brand-success); padding: 12px 14px; border-radius: var(--radius-sm); margin-bottom: 16px; font-size: 0.9rem; }
  `],
})
export class ResetPasswordComponent implements OnInit {
  form: FormGroup;
  loading = signal(false);
  errorMessage = signal('');
  successMessage = signal('');
  hidePassword = signal(true);
  private token = '';

  constructor(private fb: FormBuilder, private authService: AuthService, private route: ActivatedRoute, private router: Router) {
    this.form = this.fb.group({
      newPassword: ['', [Validators.required, Validators.minLength(8),
        Validators.pattern(/^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&#])/)]],
    });
  }

  ngOnInit() {
    this.token = this.route.snapshot.queryParamMap.get('token') || '';
    if (!this.token) {
      this.errorMessage.set('Token manquant. Demandez un nouveau lien.');
    }
  }

  onSubmit() {
    if (this.form.invalid || !this.token) return;
    this.loading.set(true);
    this.authService.resetPassword(this.token, this.form.value.newPassword).subscribe({
      next: () => {
        this.loading.set(false);
        this.successMessage.set('Mot de passe réinitialisé avec succès !');
      },
      error: (err) => {
        this.loading.set(false);
        this.errorMessage.set(err.error?.message || 'Erreur lors de la réinitialisation');
      }
    });
  }
}

