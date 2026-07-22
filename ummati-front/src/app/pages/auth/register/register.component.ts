import { Component, signal } from '@angular/core';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { AuthService } from '../../../core/services/auth.service';
import { TPipe } from '../../../shared/pipes/t.pipe';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [
    ReactiveFormsModule, RouterLink,
    MatCardModule, MatFormFieldModule, MatInputModule,
    MatButtonModule, MatIconModule, MatProgressSpinnerModule, TPipe,
  ],
  template: `
    <div class="auth-container">
      <mat-card class="auth-card">
        <mat-card-header>
          <mat-card-title>{{ 'Inscription' | t }}</mat-card-title>
          <mat-card-subtitle>{{ 'Rejoignez la communauté Ummati' | t }}</mat-card-subtitle>
        </mat-card-header>
        <mat-card-content>
          @if (successMessage()) {
            <div class="success-banner">{{ successMessage() }}</div>
          }
          @if (errorMessage()) {
            <div class="error-banner">{{ errorMessage() }}</div>
          }
          <form [formGroup]="form" (ngSubmit)="onSubmit()">
            <div class="name-row">
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
              <mat-label>{{ 'Email' | t }}</mat-label>
              <input matInput formControlName="email" type="email" />
              <mat-icon matSuffix>email</mat-icon>
            </mat-form-field>
            <mat-form-field appearance="outline" class="full-width">
              <mat-label>{{ 'Mot de passe' | t }}</mat-label>
              <input matInput formControlName="password" [type]="hidePassword() ? 'password' : 'text'" />
              <mat-hint>{{ 'Min. 8 caractères, 1 majuscule, 1 chiffre, 1 spécial' | t }}</mat-hint>
              <button mat-icon-button matSuffix type="button" (click)="hidePassword.set(!hidePassword())">
                <mat-icon>{{ hidePassword() ? 'visibility_off' : 'visibility' }}</mat-icon>
              </button>
            </mat-form-field>
            <button mat-flat-button color="primary" type="submit" class="full-width submit-btn"
                    [disabled]="loading()">
              @if (loading()) { <mat-spinner diameter="20" /> } @else { {{ 'Créer mon compte' | t }} }
            </button>
          </form>
        </mat-card-content>
        <mat-card-actions align="end">
          <span>{{ 'Déjà inscrit ?' | t }} <a routerLink="/login">{{ 'Se connecter' | t }}</a></span>
        </mat-card-actions>
      </mat-card>
    </div>
  `,
  styles: [`
    .auth-container { display: flex; justify-content: center; align-items: flex-start; padding: 56px 16px; }
    .auth-card { max-width: 480px; width: 100%; }
    .full-width { width: 100%; }
    .name-row { display: flex; gap: 16px; }
    .name-row mat-form-field { flex: 1; }
    .submit-btn { height: 48px; font-size: 16px; margin-top: 16px; border-radius: var(--radius-md); }
    .error-banner { background: var(--brand-danger-soft); color: var(--brand-danger); padding: 12px 14px; border-radius: var(--radius-sm); margin-bottom: 16px; font-size: 0.9rem; }
    .success-banner { background: var(--brand-success-soft); color: var(--brand-success); padding: 12px 14px; border-radius: var(--radius-sm); margin-bottom: 16px; font-size: 0.9rem; }
    mat-card-actions span { font-size: 14px; }
    @media (max-width: 480px) { .name-row { flex-direction: column; gap: 0; } }
  `],
})
export class RegisterComponent {
  form: FormGroup;
  loading = signal(false);
  errorMessage = signal('');
  successMessage = signal('');
  hidePassword = signal(true);

  constructor(private fb: FormBuilder, private authService: AuthService, private router: Router) {
    this.form = this.fb.group({
      firstName: ['', Validators.required],
      lastName: ['', Validators.required],
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required, Validators.minLength(8),
        Validators.pattern(/^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&#])/)]],
    });
  }

  onSubmit() {
    if (this.form.invalid) return;
    this.loading.set(true);
    this.errorMessage.set('');
    this.successMessage.set('');

    this.authService.register(this.form.value).subscribe({
      next: (res) => {
        this.loading.set(false);
        this.successMessage.set(res.message || 'Compte créé ! Vérifiez votre email.');
        this.form.reset();
      },
      error: (err) => {
        this.loading.set(false);
        this.errorMessage.set(err.error?.message || "Erreur lors de l'inscription");
      }
    });
  }
}
