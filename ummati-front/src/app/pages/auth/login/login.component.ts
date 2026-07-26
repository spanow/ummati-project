import { Component, signal, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { Router, RouterLink, ActivatedRoute } from '@angular/router';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { AuthService } from '../../../core/services/auth.service';
import { TPipe } from '../../../shared/pipes/t.pipe';
import { AuthShellComponent } from '../../../shared/components/auth-shell/auth-shell.component';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [
    ReactiveFormsModule, RouterLink, MatFormFieldModule, MatInputModule,
    MatButtonModule, MatIconModule, MatProgressSpinnerModule, TPipe, AuthShellComponent,
  ],
  template: `
    <app-auth-shell title="Bon retour" subtitle="Connectez-vous pour retrouver vos missions.">
      @if (successMessage()) {
        <div class="banner banner-success" role="status">
          <mat-icon>check_circle</mat-icon>
          <span>{{ successMessage() }}</span>
        </div>
      }
      @if (errorMessage()) {
        <div class="banner banner-error" role="alert">
          <mat-icon>error</mat-icon>
          <span>{{ errorMessage() }}</span>
        </div>
      }

      <form [formGroup]="form" (ngSubmit)="onSubmit()">
        <mat-form-field appearance="outline" class="field-full">
          <mat-label>{{ 'Email' | t }}</mat-label>
          <input matInput formControlName="email" type="email" autocomplete="email" />
          <mat-icon matSuffix>mail</mat-icon>
        </mat-form-field>

        <mat-form-field appearance="outline" class="field-full">
          <mat-label>{{ 'Mot de passe' | t }}</mat-label>
          <input matInput formControlName="password" autocomplete="current-password"
                 [type]="hidePassword() ? 'password' : 'text'" />
          <button mat-icon-button matSuffix type="button" (click)="hidePassword.set(!hidePassword())"
                  [attr.aria-label]="(hidePassword() ? 'Afficher le mot de passe' : 'Masquer le mot de passe') | t">
            <mat-icon>{{ hidePassword() ? 'visibility_off' : 'visibility' }}</mat-icon>
          </button>
        </mat-form-field>

        <div class="forgot-link">
          <a routerLink="/forgot-password">{{ 'Mot de passe oublié ?' | t }}</a>
        </div>

        <button mat-flat-button type="submit" class="submit-btn" [disabled]="loading()">
          @if (loading()) { <mat-spinner diameter="20" /> } @else { {{ 'Se connecter' | t }} }
        </button>
      </form>

      <ng-container footer>
        {{ 'Pas encore de compte ?' | t }}
        <a routerLink="/register">{{ 'S\\'inscrire' | t }}</a>
      </ng-container>
    </app-auth-shell>
  `,
  styles: [`
    .forgot-link { text-align: end; margin: calc(-1 * var(--space-2)) 0 var(--space-4); }
    .forgot-link a { font-size: 0.88rem; }
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
