import { Component, signal, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { Router, RouterLink, ActivatedRoute } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-login',
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
          <mat-card-title>Connexion</mat-card-title>
          <mat-card-subtitle>Accédez à votre espace Ummati</mat-card-subtitle>
        </mat-card-header>
        <mat-card-content>
          @if (successMessage()) {
            <div class="success-banner">
              <mat-icon>check_circle</mat-icon>
              {{ successMessage() }}
            </div>
          }
          @if (errorMessage()) {
            <div class="error-banner">{{ errorMessage() }}</div>
          }
          <form [formGroup]="form" (ngSubmit)="onSubmit()">
            <mat-form-field appearance="outline" class="full-width">
              <mat-label>Email</mat-label>
              <input matInput formControlName="email" type="email" />
              <mat-icon matSuffix>email</mat-icon>
            </mat-form-field>
            <mat-form-field appearance="outline" class="full-width">
              <mat-label>Mot de passe</mat-label>
              <input matInput formControlName="password" [type]="hidePassword() ? 'password' : 'text'" />
              <button mat-icon-button matSuffix type="button" (click)="hidePassword.set(!hidePassword())">
                <mat-icon>{{ hidePassword() ? 'visibility_off' : 'visibility' }}</mat-icon>
              </button>
            </mat-form-field>
            <div class="forgot-link">
              <a routerLink="/forgot-password">Mot de passe oublié ?</a>
            </div>
            <button mat-flat-button color="primary" type="submit" class="full-width submit-btn"
                    [disabled]="loading()">
              @if (loading()) { <mat-spinner diameter="20" /> } @else { Se connecter }
            </button>
          </form>
        </mat-card-content>
        <mat-card-actions align="end">
          <span>Pas encore de compte ? <a routerLink="/register">S'inscrire</a></span>
        </mat-card-actions>
      </mat-card>
    </div>
  `,
  styles: [`
    .auth-container { display: flex; justify-content: center; padding: 48px 16px; }
    .auth-card { max-width: 440px; width: 100%; }
    .full-width { width: 100%; }
    .submit-btn { height: 48px; font-size: 16px; margin-top: 8px; }
    .forgot-link { text-align: right; margin: -8px 0 16px; }
    .forgot-link a { font-size: 14px; }
    .error-banner { background: #fdecea; color: #d32f2f; padding: 12px; border-radius: 4px; margin-bottom: 16px; }
    .success-banner { background: #e8f5e9; color: #2e7d32; padding: 12px; border-radius: 4px; margin-bottom: 16px; display: flex; align-items: center; gap: 8px; }
    mat-card-actions span { font-size: 14px; }
  `],
})
export class LoginComponent implements OnInit {
  form: FormGroup;
  loading = signal(false);
  errorMessage = signal('');
  successMessage = signal('');
  hidePassword = signal(true);

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private router: Router,
    private route: ActivatedRoute,
  ) {
    this.form = this.fb.group({
      email: ['', [Validators.required, Validators.email]],
      password: ['', Validators.required],
    });
  }

  ngOnInit() {
    this.route.queryParams.subscribe(params => {
      if (params['emailConfirmed'] === 'true') {
        this.successMessage.set('Email confirmé avec succès ! Vous pouvez maintenant vous connecter.');
      } else if (params['emailError'] === 'true') {
        this.errorMessage.set('Le lien de confirmation est invalide ou expiré. Veuillez en demander un nouveau.');
      }
    });
  }

  onSubmit() {
    if (this.form.invalid) return;
    this.loading.set(true);
    this.errorMessage.set('');
    const { email, password } = this.form.value;

    this.authService.login(email, password).subscribe({
      next: (res) => {
        this.loading.set(false);
        const user = res.data?.user;
        if (user && !user.onboardingDone) {
          this.router.navigate(['/onboarding']);
        } else {
          this.router.navigate(['/']);
        }
      },
      error: (err) => {
        this.loading.set(false);
        this.errorMessage.set(err.error?.message || 'Erreur de connexion');
      }
    });
  }
}
