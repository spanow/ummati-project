import { Component, signal, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatStepperModule } from '@angular/material/stepper';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';
import { AuthService } from '../../core/services/auth.service';

interface Skill { id: string; name: string; category: string; }

@Component({
  selector: 'app-onboarding',
  standalone: true,
  imports: [ReactiveFormsModule, MatCardModule, MatFormFieldModule, MatInputModule,
    MatButtonModule, MatIconModule, MatChipsModule, MatStepperModule, MatProgressSpinnerModule],
  template: `
    <div class="onboarding-container">
      <div class="onboarding-header">
        <mat-icon class="welcome-icon">waving_hand</mat-icon>
        <h1>Bienvenue sur Ummati !</h1>
        <p>Complétez votre profil en quelques étapes</p>
      </div>

      <mat-stepper linear #stepper class="onboarding-stepper">
        <mat-step [stepControl]="step1">
          <ng-template matStepLabel>À propos de vous</ng-template>
          <form [formGroup]="step1">
            <mat-form-field appearance="outline" class="full-width">
              <mat-label>Bio (optionnel)</mat-label>
              <textarea matInput formControlName="bio" rows="3" placeholder="Présentez-vous en quelques mots..."></textarea>
            </mat-form-field>
            <mat-form-field appearance="outline" class="full-width">
              <mat-label>Ville</mat-label>
              <input matInput formControlName="city" placeholder="Votre ville" />
            </mat-form-field>
            <div class="step-actions">
              <button mat-flat-button matStepperNext>Suivant</button>
            </div>
          </form>
        </mat-step>

        <mat-step>
          <ng-template matStepLabel>Compétences</ng-template>
          <p class="step-hint">Sélectionnez vos compétences pour être mieux orienté :</p>
          <div class="skills-grid">
            @for (skill of skills(); track skill.id) {
              <mat-chip-option [selected]="selectedSkillIds.has(skill.id)" (click)="toggleSkill(skill.id)">
                {{ skill.name }}
              </mat-chip-option>
            }
          </div>
          <div class="step-actions">
            <button mat-stroked-button matStepperPrevious>Retour</button>
            <button mat-flat-button matStepperNext>Suivant</button>
          </div>
        </mat-step>

        <mat-step>
          <ng-template matStepLabel>Terminé !</ng-template>
          <div class="finish-step">
            <mat-icon class="finish-icon">celebration</mat-icon>
            <h3>Votre profil est prêt !</h3>
            <p>Vous pouvez maintenant découvrir les organisations et événements.</p>
            <button mat-flat-button (click)="complete()" [disabled]="loading()" class="finish-btn">
              @if (loading()) { <mat-spinner diameter="20" /> } @else { Commencer }
            </button>
          </div>
        </mat-step>
      </mat-stepper>
    </div>
  `,
  styles: [`
    .onboarding-container { max-width: 600px; margin: 0 auto; padding: 48px 24px; }
    .onboarding-header { text-align: center; margin-bottom: 40px; }
    .welcome-icon { font-size: 48px; width: 48px; height: 48px; color: #f9a825; }
    h1 { font-size: 1.8rem; font-weight: 600; margin: 12px 0 4px; }
    .onboarding-header p { color: #666; font-size: 1.05rem; }
    .full-width { width: 100%; }
    .step-hint { color: #666; margin-bottom: 16px; }
    .skills-grid { display: flex; flex-wrap: wrap; gap: 8px; margin-bottom: 24px; }
    .step-actions { display: flex; justify-content: flex-end; gap: 12px; margin-top: 24px; }
    .finish-step { text-align: center; padding: 32px 0; }
    .finish-icon { font-size: 56px; width: 56px; height: 56px; color: #43a047; }
    .finish-step h3 { font-size: 1.3rem; margin: 16px 0 8px; }
    .finish-step p { color: #666; margin-bottom: 24px; }
    .finish-btn { height: 48px; padding: 0 40px; font-size: 16px; }
  `],
})
export class OnboardingComponent implements OnInit {
  step1: FormGroup;
  skills = signal<Skill[]>([]);
  selectedSkillIds = new Set<string>();
  loading = signal(false);

  constructor(private fb: FormBuilder, private http: HttpClient, private router: Router, private authService: AuthService) {
    this.step1 = this.fb.group({
      bio: [''],
      city: ['', Validators.required],
    });
  }

  ngOnInit() {
    this.http.get<any>(`${environment.apiUrl}/skills`).subscribe({
      next: res => this.skills.set(res.data || []),
    });
  }

  toggleSkill(id: string) {
    if (this.selectedSkillIds.has(id)) this.selectedSkillIds.delete(id);
    else this.selectedSkillIds.add(id);
  }

  complete() {
    this.loading.set(true);
    const body = {
      bio: this.step1.value.bio,
      city: this.step1.value.city,
      skillIds: Array.from(this.selectedSkillIds),
      preferredDomains: [],
    };
    this.http.post(`${environment.apiUrl}/profile/onboarding`, body).subscribe({
      next: () => {
        this.loading.set(false);
        this.authService.updateOnboardingDone();
        this.router.navigate(['/dashboard']);
      },
      error: () => { this.loading.set(false); this.router.navigate(['/']); },
    });
  }
}

