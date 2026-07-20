import { Component, signal } from '@angular/core';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { AuthService } from '../../../core/services/auth.service';
import { TPipe } from '../../../shared/pipes/t.pipe';

@Component({
  selector: 'app-forgot-password',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink, MatCardModule, MatFormFieldModule, MatInputModule, MatButtonModule, MatProgressSpinnerModule, TPipe],
  template: `
    <div class="auth-container">
      <mat-card class="auth-card">
        <mat-card-header>
          <mat-card-title>{{ 'Mot de passe oublié' | t }}</mat-card-title>
          <mat-card-subtitle>{{ 'Entrez votre email pour recevoir un lien de réinitialisation' | t }}</mat-card-subtitle>
        </mat-card-header>
        <mat-card-content>
          @if (sent()) {
            <div class="success-banner">{{ 'Si ce compte existe, un email de réinitialisation a été envoyé.' | t }}</div>
          }
          <form [formGroup]="form" (ngSubmit)="onSubmit()">
            <mat-form-field appearance="outline" class="full-width">
              <mat-label>{{ 'Email' | t }}</mat-label>
              <input matInput formControlName="email" type="email" />
            </mat-form-field>
            <button mat-flat-button color="primary" type="submit" class="full-width" [disabled]="loading()">
              @if (loading()) { <mat-spinner diameter="20" /> } @else { {{ 'Envoyer le lien' | t }} }
            </button>
          </form>
        </mat-card-content>
        <mat-card-actions align="end">
          <a routerLink="/login">{{ 'Retour à la connexion' | t }}</a>
        </mat-card-actions>
      </mat-card>
    </div>
  `,
  styles: [`
    .auth-container { display: flex; justify-content: center; padding: 48px 16px; }
    .auth-card { max-width: 440px; width: 100%; }
    .full-width { width: 100%; }
    .success-banner { background: #e8f5e9; color: #2e7d32; padding: 12px; border-radius: 4px; margin-bottom: 16px; }
  `],
})
export class ForgotPasswordComponent {
  form: FormGroup;
  loading = signal(false);
  sent = signal(false);

  constructor(private fb: FormBuilder, private authService: AuthService) {
    this.form = this.fb.group({ email: ['', [Validators.required, Validators.email]] });
  }

  onSubmit() {
    if (this.form.invalid) return;
    this.loading.set(true);
    this.authService.forgotPassword(this.form.value.email).subscribe({
      next: () => { this.loading.set(false); this.sent.set(true); },
      error: () => { this.loading.set(false); this.sent.set(true); } // Always show success
    });
  }
}

