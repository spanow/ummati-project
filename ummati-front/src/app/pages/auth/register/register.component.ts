import { Component, computed, signal } from '@angular/core';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { toSignal } from '@angular/core/rxjs-interop';
import { Router, RouterLink } from '@angular/router';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { AuthService } from '../../../core/services/auth.service';
import { TPipe } from '../../../shared/pipes/t.pipe';
import { AuthShellComponent } from '../../../shared/components/auth-shell/auth-shell.component';

/** Règles de mot de passe — doivent rester alignées sur le validateur du formulaire. */
const PASSWORD_RULES: { label: string; test: (v: string) => boolean }[] = [
  { label: '8 caractères minimum', test: v => v.length >= 8 },
  { label: 'Une majuscule et une minuscule', test: v => /[a-z]/.test(v) && /[A-Z]/.test(v) },
  { label: 'Un chiffre', test: v => /\d/.test(v) },
  { label: 'Un caractère spécial', test: v => /[@$!%*?&#]/.test(v) },
];

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [
    ReactiveFormsModule, RouterLink, MatFormFieldModule, MatInputModule,
    MatButtonModule, MatIconModule, MatProgressSpinnerModule, TPipe, AuthShellComponent,
  ],
  template: `
    <app-auth-shell title="Créer un compte" subtitle="Quelques secondes, et vous pouvez déjà vous inscrire à une mission.">
      @if (successMessage()) {
        <div class="banner banner-success" role="status">
          <mat-icon>mark_email_read</mat-icon>
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
        <div class="name-row">
          <mat-form-field appearance="outline">
            <mat-label>{{ 'Prénom' | t }}</mat-label>
            <input matInput formControlName="firstName" autocomplete="given-name" />
          </mat-form-field>
          <mat-form-field appearance="outline">
            <mat-label>{{ 'Nom' | t }}</mat-label>
            <input matInput formControlName="lastName" autocomplete="family-name" />
          </mat-form-field>
        </div>

        <mat-form-field appearance="outline" class="field-full">
          <mat-label>{{ 'Email' | t }}</mat-label>
          <input matInput formControlName="email" type="email" autocomplete="email" />
          <mat-icon matSuffix>mail</mat-icon>
        </mat-form-field>

        <mat-form-field appearance="outline" class="field-full" subscriptSizing="dynamic">
          <mat-label>{{ 'Mot de passe' | t }}</mat-label>
          <input matInput formControlName="password" autocomplete="new-password"
                 [type]="hidePassword() ? 'password' : 'text'" />
          <button mat-icon-button matSuffix type="button" (click)="hidePassword.set(!hidePassword())"
                  [attr.aria-label]="(hidePassword() ? 'Afficher le mot de passe' : 'Masquer le mot de passe') | t">
            <mat-icon>{{ hidePassword() ? 'visibility_off' : 'visibility' }}</mat-icon>
          </button>
        </mat-form-field>

        <!-- Critères cochés en direct : plus utile qu'un simple message d'erreur après coup -->
        <ul class="pwd-rules" aria-live="polite">
          @for (rule of ruleStates(); track rule.label) {
            <li [class.ok]="rule.ok">
              <mat-icon>{{ rule.ok ? 'check_circle' : 'radio_button_unchecked' }}</mat-icon>
              {{ rule.label | t }}
            </li>
          }
        </ul>

        <button mat-flat-button type="submit" class="submit-btn" [disabled]="loading()">
          @if (loading()) { <mat-spinner diameter="20" /> } @else { {{ 'Créer mon compte' | t }} }
        </button>
      </form>

      <ng-container footer>
        {{ 'Déjà inscrit ?' | t }}
        <a routerLink="/login">{{ 'Se connecter' | t }}</a>
      </ng-container>
    </app-auth-shell>
  `,
  styles: [`
    .name-row { display: flex; gap: var(--space-4); }
    .name-row mat-form-field { flex: 1; }

    .pwd-rules {
      list-style: none; margin: var(--space-3) 0 var(--space-5); padding: 0;
      display: grid; gap: 6px;
    }
    .pwd-rules li {
      display: flex; align-items: center; gap: 8px;
      font-size: 0.84rem; color: var(--brand-text-faint);
      transition: color 0.2s var(--ease-out);
    }
    .pwd-rules li.ok { color: var(--brand-success); }
    .pwd-rules mat-icon { font-size: 16px; width: 16px; height: 16px; }

    @media (max-width: 480px) { .name-row { flex-direction: column; gap: 0; } }
  `],
})
export class RegisterComponent {
  form: FormGroup;
  loading = signal(false);
  errorMessage = signal('');
  successMessage = signal('');
  hidePassword = signal(true);

  /** Valeur courante du champ mot de passe, exposée en signal. */
  private passwordValue;
  /** État de chaque critère, recalculé à la frappe. */
  ruleStates;

  constructor(private fb: FormBuilder, private authService: AuthService, private router: Router) {
    this.form = this.fb.group({
      firstName: ['', Validators.required],
      lastName: ['', Validators.required],
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required, Validators.minLength(8),
        Validators.pattern(/^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&#])/)]],
    });

    this.passwordValue = toSignal(this.form.controls['password'].valueChanges, { initialValue: '' });
    this.ruleStates = computed(() => {
      const v = this.passwordValue() ?? '';
      return PASSWORD_RULES.map(r => ({ label: r.label, ok: r.test(v) }));
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
