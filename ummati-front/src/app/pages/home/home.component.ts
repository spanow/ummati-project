import { Component, inject, OnInit, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatCardModule } from '@angular/material/card';
import { RouterLink } from '@angular/router';
import { CommonModule } from '@angular/common';
import { AdminApiService } from '../../core/services/dashboard.service';

interface PlatformStat { icon: string; value: string; label: string; }
interface Feature { icon: string; title: string; description: string; }

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [CommonModule, MatButtonModule, MatIconModule, MatCardModule, RouterLink],
  template: `
    <!-- Hero Section -->
    <section class="hero" aria-label="Bienvenue sur Ummati">
      <div class="hero-content">
        <div class="hero-badge">
          <mat-icon aria-hidden="true">volunteer_activism</mat-icon>
          Plateforme de bénévolat
        </div>
        <h1>Rejoignez une communauté<br><span class="gradient-text">engagée & solidaire</span></h1>
        <p class="hero-desc">
          Ummati connecte les bénévoles passionnés avec des associations qui ont besoin de vous.
          Trouvez votre mission, développez vos compétences, faites la différence.
        </p>
        <div class="hero-actions">
          <a mat-flat-button routerLink="/register" color="primary" class="cta-primary" aria-label="Créer un compte bénévole">
            <mat-icon>person_add</mat-icon>
            Devenir bénévole
          </a>
          <a mat-stroked-button routerLink="/organizations" class="cta-secondary" aria-label="Découvrir les associations">
            <mat-icon>search</mat-icon>
            Découvrir les ONG
          </a>
        </div>
        <div class="hero-tags">
          @for (tag of tags; track tag) {
            <span class="tag">{{ tag }}</span>
          }
        </div>
      </div>
      <div class="hero-visual" aria-hidden="true">
        <div class="blob blob-1"></div>
        <div class="blob blob-2"></div>
        <div class="hero-illustration">
          <mat-icon class="big-icon">volunteer_activism</mat-icon>
        </div>
      </div>
    </section>

    <!-- Stats Section -->
    <section class="stats-section" aria-label="Statistiques de la plateforme">
      <div class="stats-container">
        @for (stat of stats(); track stat.label) {
          <div class="stat-card">
            <mat-icon aria-hidden="true">{{ stat.icon }}</mat-icon>
            <span class="stat-value">{{ stat.value }}</span>
            <span class="stat-label">{{ stat.label }}</span>
          </div>
        }
      </div>
    </section>

    <!-- Features Section -->
    <section class="features-section" aria-labelledby="features-title">
      <div class="section-header">
        <h2 id="features-title">Pourquoi choisir Ummati ?</h2>
        <p>Une plateforme pensée pour faciliter l'engagement bénévole et la gestion des associations</p>
      </div>
      <div class="features-grid">
        @for (feature of features; track feature.title) {
          <article class="feature-card" tabindex="0">
            <div class="feature-icon">
              <mat-icon aria-hidden="true">{{ feature.icon }}</mat-icon>
            </div>
            <h3>{{ feature.title }}</h3>
            <p>{{ feature.description }}</p>
          </article>
        }
      </div>
    </section>

    <!-- How It Works -->
    <section class="how-section" aria-labelledby="how-title">
      <div class="section-header">
        <h2 id="how-title">Comment ça marche ?</h2>
        <p>Rejoindre Ummati en 3 étapes simples</p>
      </div>
      <div class="steps">
        @for (step of steps; track step.number) {
          <div class="step">
            <div class="step-number" [attr.aria-label]="'Étape ' + step.number">{{ step.number }}</div>
            <mat-icon aria-hidden="true">{{ step.icon }}</mat-icon>
            <h3>{{ step.title }}</h3>
            <p>{{ step.description }}</p>
          </div>
        }
      </div>
    </section>

    <!-- CTA Section -->
    <section class="cta-section" aria-labelledby="cta-title">
      <div class="cta-content">
        <h2 id="cta-title">Prêt(e) à faire la différence ?</h2>
        <p>Rejoignez des milliers de bénévoles déjà engagés sur Ummati</p>
        <div class="cta-buttons">
          <a mat-flat-button routerLink="/register" color="primary" class="cta-large">Créer mon compte — c'est gratuit</a>
          <a mat-button routerLink="/events" class="cta-events">
            Explorer les événements <mat-icon>arrow_forward</mat-icon>
          </a>
        </div>
      </div>
    </section>

    <!-- Footer minimal -->
    <footer class="home-footer" role="contentinfo">
      <p>© 2026 Ummati — Bénévolat &amp; Gestion d'ONG</p>
      <nav aria-label="Liens du pied de page">
        <a routerLink="/organizations">Organisations</a>
        <a routerLink="/events">Événements</a>
        <a routerLink="/login">Connexion</a>
      </nav>
    </footer>
  `,
  styles: [`
    :host { display: block; }
    .hero {
      min-height: 88vh; display: flex; align-items: center; justify-content: space-between;
      padding: 80px 10% 60px; gap: 48px; position: relative; overflow: hidden;
      background: linear-gradient(135deg, #f8f9ff 0%, #ffffff 60%, #f0f4ff 100%);
    }
    .hero-content { flex: 1; max-width: 580px; z-index: 1; }
    .hero-badge {
      display: inline-flex; align-items: center; gap: 6px;
      background: rgba(63,81,181,0.1); color: #3f51b5;
      padding: 6px 14px; border-radius: 20px; font-size: 0.85rem; font-weight: 600; margin-bottom: 24px;
    }
    .hero-badge mat-icon { font-size: 18px !important; width: 18px !important; height: 18px !important; }
    h1 { font-size: clamp(2rem, 5vw, 3.2rem); font-weight: 800; line-height: 1.15; color: #1a1a2e; margin: 0 0 20px; }
    .gradient-text {
      background: linear-gradient(135deg, #3f51b5, #7c4dff);
      -webkit-background-clip: text; -webkit-text-fill-color: transparent; background-clip: text;
    }
    .hero-desc { font-size: 1.1rem; color: #555; line-height: 1.7; margin-bottom: 36px; }
    .hero-actions { display: flex; gap: 16px; flex-wrap: wrap; margin-bottom: 32px; }
    .cta-primary, .cta-secondary { padding: 12px 28px !important; font-size: 1rem !important; border-radius: 12px !important; }
    .hero-tags { display: flex; gap: 8px; flex-wrap: wrap; }
    .tag { background: #f0f0f0; color: #555; padding: 4px 12px; border-radius: 20px; font-size: 0.8rem; }
    .hero-visual { flex: 0 0 400px; position: relative; display: flex; align-items: center; justify-content: center; height: 400px; }
    .blob { position: absolute; border-radius: 50%; filter: blur(60px); opacity: 0.4; }
    .blob-1 { width: 300px; height: 300px; background: #7c4dff; top: 0; right: 0; }
    .blob-2 { width: 200px; height: 200px; background: #3f51b5; bottom: 0; left: 0; }
    .hero-illustration {
      z-index: 1; background: white; border-radius: 50%; width: 200px; height: 200px;
      display: flex; align-items: center; justify-content: center;
      box-shadow: 0 20px 60px rgba(63,81,181,0.2);
    }
    .big-icon { font-size: 96px !important; width: 96px !important; height: 96px !important; color: #3f51b5; }
    /* Stats */
    .stats-section { background: #3f51b5; padding: 48px 10%; }
    .stats-container { display: grid; grid-template-columns: repeat(auto-fit, minmax(180px, 1fr)); gap: 32px; text-align: center; }
    .stat-card { display: flex; flex-direction: column; align-items: center; gap: 8px; color: white; }
    .stat-card mat-icon { font-size: 32px !important; width: 32px !important; height: 32px !important; opacity: 0.9; }
    .stat-value { font-size: 2.4rem; font-weight: 800; }
    .stat-label { font-size: 0.9rem; opacity: 0.85; }
    /* Features */
    .features-section { padding: 80px 10%; background: #fafafa; }
    .section-header { text-align: center; margin-bottom: 56px; }
    .section-header h2 { font-size: 2rem; font-weight: 700; color: #1a1a2e; margin-bottom: 12px; }
    .section-header p { color: #666; font-size: 1.05rem; }
    .features-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(260px, 1fr)); gap: 24px; }
    .feature-card {
      background: white; border-radius: 16px; padding: 32px;
      box-shadow: 0 2px 16px rgba(0,0,0,0.06); transition: transform 0.2s, box-shadow 0.2s; outline-offset: 4px;
    }
    .feature-card:hover, .feature-card:focus {
      transform: translateY(-4px); box-shadow: 0 8px 32px rgba(63,81,181,0.12); outline: 2px solid #3f51b5;
    }
    .feature-icon {
      width: 56px; height: 56px; background: linear-gradient(135deg, #e8eaf6, #c5cae9);
      border-radius: 14px; display: flex; align-items: center; justify-content: center; margin-bottom: 20px;
    }
    .feature-icon mat-icon { color: #3f51b5; font-size: 28px !important; width: 28px !important; height: 28px !important; }
    .feature-card h3 { font-size: 1.1rem; font-weight: 600; margin: 0 0 10px; color: #1a1a2e; }
    .feature-card p { color: #666; line-height: 1.6; margin: 0; font-size: 0.95rem; }
    /* How it works */
    .how-section { padding: 80px 10%; }
    .steps { display: grid; grid-template-columns: repeat(auto-fit, minmax(220px, 1fr)); gap: 32px; }
    .step { text-align: center; padding: 32px 20px; }
    .step-number {
      width: 48px; height: 48px; background: #3f51b5; color: white; border-radius: 50%;
      display: flex; align-items: center; justify-content: center; font-size: 1.3rem; font-weight: 700; margin: 0 auto 16px;
    }
    .step mat-icon { font-size: 36px !important; width: 36px !important; height: 36px !important; color: #7c4dff; display: block; margin: 0 auto 12px; }
    .step h3 { font-size: 1.05rem; font-weight: 600; color: #1a1a2e; margin: 0 0 8px; }
    .step p { color: #666; font-size: 0.9rem; line-height: 1.6; margin: 0; }
    /* CTA */
    .cta-section { background: linear-gradient(135deg, #3f51b5, #7c4dff); padding: 80px 10%; text-align: center; }
    .cta-content h2 { color: white; font-size: 2rem; font-weight: 700; margin-bottom: 16px; }
    .cta-content p { color: rgba(255,255,255,0.85); font-size: 1.05rem; margin-bottom: 36px; }
    .cta-buttons { display: flex; gap: 16px; justify-content: center; flex-wrap: wrap; }
    .cta-large { background: white !important; color: #3f51b5 !important; padding: 14px 32px !important; font-size: 1rem !important; border-radius: 12px !important; font-weight: 600 !important; }
    .cta-events { color: white !important; padding: 14px 24px !important; font-size: 1rem !important; }
    /* Footer */
    .home-footer {
      background: #1a1a2e; color: rgba(255,255,255,0.7);
      padding: 32px 10%; display: flex; align-items: center; justify-content: space-between; flex-wrap: wrap; gap: 16px; font-size: 0.9rem;
    }
    .home-footer nav { display: flex; gap: 24px; }
    .home-footer a { color: rgba(255,255,255,0.7); text-decoration: none; }
    .home-footer a:hover { color: white; }
    .home-footer a:focus { outline: 2px solid white; border-radius: 2px; }
    @media (max-width: 768px) {
      .hero { flex-direction: column; padding: 60px 5% 40px; text-align: center; }
      .hero-actions { justify-content: center; }
      .hero-tags { justify-content: center; }
      .hero-visual { display: none; }
      .home-footer { flex-direction: column; text-align: center; }
    }
  `],
})
export class HomeComponent implements OnInit {
  private adminService = inject(AdminApiService);
  stats = signal<PlatformStat[]>([
    { icon: 'people', value: '500+', label: 'Bénévoles inscrits' },
    { icon: 'business', value: '50+', label: 'Associations actives' },
    { icon: 'event', value: '200+', label: 'Événements organisés' },
    { icon: 'volunteer_activism', value: '1 000+', label: 'Heures de bénévolat' },
  ]);
  tags = ['Solidarité', 'Bénévolat', 'ONG', 'Communauté', 'Engagement', 'Social'];
  features: Feature[] = [
    { icon: 'search', title: 'Trouvez votre association', description: 'Explorez des dizaines d\'associations par domaine, ville ou compétences requises.' },
    { icon: 'event_available', title: 'Inscrivez-vous aux événements', description: 'Participez à des missions de bénévolat près de chez vous ou en ligne.' },
    { icon: 'psychology', title: 'Développez vos compétences', description: 'Chaque mission est une opportunité d\'apprendre et de partager vos talents.' },
    { icon: 'notifications_active', title: 'Restez informé(e)', description: 'Recevez des notifications pour les nouvelles missions correspondant à vos intérêts.' },
    { icon: 'groups', title: 'Gérez votre équipe', description: 'Pour les associations : gérez membres, événements et présences en un seul endroit.' },
    { icon: 'verified', title: 'Plateforme de confiance', description: 'Chaque association est vérifiée par notre équipe avant publication.' },
  ];
  steps = [
    { number: '1', icon: 'person_add', title: 'Créez votre compte', description: 'Inscription gratuite en quelques secondes. Renseignez vos compétences et votre ville.' },
    { number: '2', icon: 'search', title: 'Explorez et rejoignez', description: 'Parcourez les associations et les événements. Rejoignez ceux qui vous correspondent.' },
    { number: '3', icon: 'favorite', title: 'Agissez & impactez', description: 'Participez aux missions, collectez des retours et construisez votre profil bénévole.' },
  ];

  ngOnInit() {
    this.adminService.getStats().subscribe({
      next: (res) => {
        if (res?.data) {
          this.stats.set([
            { icon: 'people', value: this.fmt(res.data.totalUsers), label: 'Bénévoles inscrits' },
            { icon: 'business', value: this.fmt(res.data.totalOrganizations), label: 'Associations actives' },
            { icon: 'event', value: this.fmt(res.data.totalEvents), label: 'Événements organisés' },
            { icon: 'volunteer_activism', value: '1 000+', label: 'Heures de bénévolat' },
          ]);
        }
      },
      error: () => {} // garde les valeurs par défaut
    });
  }
  private fmt(n: number): string {
    return n >= 1000 ? (n / 1000).toFixed(1) + 'k' : n.toString();
  }
}
