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

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [
    ReactiveFormsModule, RouterLink,
    MatCardModule, MatFormFieldModule, MatInputModule,
    MatButtonModule, MatIconModule, MatProgressSpinnerModule,
  ],
  template: `
    <div class="auth-container">
      <mat-card class="auth-card">
        <mat-card-header>
          <mat-card-title>Inscription</mat-card-title>
          <mat-card-subtitle>Rejoignez la communauté Ummati</mat-card-subtitle>
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
                <mat-label>Prénom</mat-label>
                <input matInput formControlName="firstName" />
              </mat-form-field>
              <mat-form-field appearance="outline">
                <mat-label>Nom</mat-label>
                <input matInput formControlName="lastName" />
              </mat-form-field>
            </div>
            <mat-form-field appearance="outline" class="full-width">
              <mat-label>Email</mat-label>
              <input matInput formControlName="email" type="email" />
              <mat-icon matSuffix>email</mat-icon>
            </mat-form-field>
            <mat-form-field appearance="outline" class="full-width">
              <mat-label>Mot de passe</mat-label>
              <input matInput formControlName="password" [type]="hidePassword() ? 'password' : 'text'" />
              <mat-hint>Min. 8 caractères, 1 majuscule, 1 chiffre, 1 spécial</mat-hint>
              <button mat-icon-button matSuffix type="button" (click)="hidePassword.set(!hidePassword())">
                <mat-icon>{{ hidePassword() ? 'visibility_off' : 'visibility' }}</mat-icon>
              </button>
            </mat-form-field>
            <button mat-flat-button color="primary" type="submit" class="full-width submit-btn"
                    [disabled]="loading()">
              @if (loading()) { <mat-spinner diameter="20" /> } @else { Créer mon compte }
            </button>
          </form>
        </mat-card-content>
        <mat-card-actions align="end">
          <span>Déjà inscrit ? <a routerLink="/login">Se connecter</a></span>
        </mat-card-actions>
      </mat-card>
    </div>
  `,
  styles: [`
    .auth-container { display: flex; justify-content: center; padding: 48px 16px; }
    .auth-card { max-width: 480px; width: 100%; }
    .full-width { width: 100%; }
    .name-row { display: flex; gap: 16px; }
    .name-row mat-form-field { flex: 1; }
    .submit-btn { height: 48px; font-size: 16px; margin-top: 16px; }
    .error-banner { background: #fdecea; color: #d32f2f; padding: 12px; border-radius: 4px; margin-bottom: 16px; }
    .success-banner { background: #e8f5e9; color: #2e7d32; padding: 12px; border-radius: 4px; margin-bottom: 16px; }
    mat-card-actions span { font-size: 14px; }
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
