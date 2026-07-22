import { Component, inject, OnInit, signal, PLATFORM_ID } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatCardModule } from '@angular/material/card';
import { RouterLink } from '@angular/router';
import { CommonModule } from '@angular/common';
import { PublicService } from '../../core/services/public.service';
import { I18nService } from '../../core/services/i18n.service';
import { TPipe } from '../../shared/pipes/t.pipe';

interface PlatformStat { icon: string; value: string; label: string; }
interface StatTarget { icon: string; label: string; target: number; }
interface Feature { icon: string; title: string; description: string; }

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [CommonModule, MatButtonModule, MatIconModule, MatCardModule, RouterLink, TPipe],
  template: `
    <!-- Hero Section -->
    <section class="hero" aria-label="Bienvenue sur Ummati">
      <div class="hero-content">
        <div class="hero-badge">
          <mat-icon aria-hidden="true">volunteer_activism</mat-icon>
          {{ 'Plateforme de bénévolat' | t }}
        </div>
        <h1>{{ 'Rejoignez une communauté' | t }}<br><span class="gradient-text">{{ 'engagée & solidaire' | t }}</span></h1>
        <p class="hero-desc">
          {{ 'Ummati connecte les bénévoles passionnés avec des associations qui ont besoin de vous. Trouvez votre mission, développez vos compétences, faites la différence.' | t }}
        </p>
        <div class="hero-actions">
          <a mat-flat-button routerLink="/register" color="primary" class="cta-primary" aria-label="Créer un compte bénévole">
            <mat-icon>person_add</mat-icon>
            {{ 'Devenir bénévole' | t }}
          </a>
          <a mat-stroked-button routerLink="/organizations" class="cta-secondary" aria-label="Découvrir les associations">
            <mat-icon>search</mat-icon>
            {{ 'Découvrir les ONG' | t }}
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
        <h2 id="features-title">{{ 'Pourquoi choisir Ummati ?' | t }}</h2>
        <p>{{ 'Une plateforme pensée pour faciliter l\\'engagement bénévole et la gestion des associations' | t }}</p>
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
        <h2 id="how-title">{{ 'Comment ça marche ?' | t }}</h2>
        <p>{{ 'Rejoindre Ummati en 3 étapes simples' | t }}</p>
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
        <h2 id="cta-title">{{ 'Prêt(e) à faire la différence ?' | t }}</h2>
        <p>{{ 'Rejoignez des milliers de bénévoles déjà engagés sur Ummati' | t }}</p>
        <div class="cta-buttons">
          <a mat-flat-button routerLink="/register" color="primary" class="cta-large">{{ 'Créer mon compte — c\\'est gratuit' | t }}</a>
          <a mat-button routerLink="/events" class="cta-events">
            {{ 'Explorer les événements' | t }} <mat-icon>arrow_forward</mat-icon>
          </a>
        </div>
      </div>
    </section>

    <!-- Footer minimal -->
    <footer class="home-footer" role="contentinfo">
      <p>{{ '© 2026 Ummati — Bénévolat & Gestion d\\'ONG' | t }}</p>
      <nav aria-label="Liens du pied de page">
        <a routerLink="/organizations">{{ 'Organisations' | t }}</a>
        <a routerLink="/events">{{ 'Événements' | t }}</a>
        <a routerLink="/login">{{ 'Connexion' | t }}</a>
      </nav>
    </footer>
  `,
  styles: [`
    :host { display: block; }

    /* ===== Hero ===== */
    .hero {
      min-height: 84vh; display: flex; align-items: center; justify-content: space-between;
      max-width: var(--page-max); margin: 0 auto;
      padding: 72px 24px 64px; gap: 64px; position: relative; overflow: hidden;
    }
    .hero-content { flex: 1; max-width: 580px; z-index: 1; }
    /* Entrée en fondu, staggerée (respecte prefers-reduced-motion via styles.css) */
    .hero-content > * { animation: fade-up 0.6s cubic-bezier(0.22, 0.61, 0.36, 1) both; }
    .hero-content > *:nth-child(1) { animation-delay: 0.04s; }
    .hero-content > *:nth-child(2) { animation-delay: 0.10s; }
    .hero-content > *:nth-child(3) { animation-delay: 0.16s; }
    .hero-content > *:nth-child(4) { animation-delay: 0.22s; }
    .hero-content > *:nth-child(5) { animation-delay: 0.28s; }
    .hero-badge {
      display: inline-flex; align-items: center; gap: 8px;
      background: var(--brand-primary-soft); color: var(--brand-primary-dark);
      border: 1px solid var(--brand-primary-100);
      padding: 7px 16px; border-radius: var(--radius-pill); font-size: 0.85rem; font-weight: 600; margin-bottom: 28px;
    }
    .hero-badge mat-icon { font-size: 18px !important; width: 18px !important; height: 18px !important; }
    h1 { font-size: clamp(2.1rem, 5vw, 3.4rem); font-weight: 800; line-height: 1.12; color: var(--brand-ink); margin: 0 0 22px; letter-spacing: -0.03em; }
    .gradient-text {
      background: linear-gradient(120deg, var(--brand-primary), var(--brand-primary-light));
      -webkit-background-clip: text; -webkit-text-fill-color: transparent; background-clip: text;
    }
    .hero-desc { font-size: 1.12rem; color: var(--brand-text); line-height: 1.7; margin-bottom: 36px; }
    .hero-actions { display: flex; gap: 14px; flex-wrap: wrap; margin-bottom: 30px; }
    .cta-primary, .cta-secondary { padding: 12px 26px !important; font-size: 1rem !important; border-radius: var(--radius-md) !important; height: 50px; }
    .hero-tags { display: flex; gap: 8px; flex-wrap: wrap; }
    .tag { background: var(--brand-surface); border: 1px solid var(--brand-border); color: var(--brand-text-soft); padding: 5px 14px; border-radius: var(--radius-pill); font-size: 0.82rem; font-weight: 500; }

    .hero-visual { flex: 0 0 400px; position: relative; display: flex; align-items: center; justify-content: center; height: 400px; animation: fade-up 0.8s cubic-bezier(0.22,0.61,0.36,1) 0.15s both; }
    .blob { position: absolute; border-radius: 50%; filter: blur(72px); opacity: 0.32; }
    .blob-1 { width: 300px; height: 300px; background: var(--brand-primary-light); top: 10px; right: 10px; }
    .blob-2 { width: 220px; height: 220px; background: var(--brand-primary); bottom: 0; left: 10px; }
    .hero-illustration {
      z-index: 1; background: var(--brand-surface); border-radius: 50%; width: 208px; height: 208px;
      display: flex; align-items: center; justify-content: center;
      box-shadow: 0 24px 60px rgba(15, 118, 110, 0.18); border: 1px solid var(--brand-border);
    }
    .big-icon { font-size: 96px !important; width: 96px !important; height: 96px !important; color: var(--brand-primary); }

    /* ===== Stats ===== */
    .stats-section { background: var(--brand-gradient-deep); padding: 56px 24px; }
    .stats-container { max-width: var(--page-max); margin: 0 auto; display: grid; grid-template-columns: repeat(auto-fit, minmax(180px, 1fr)); gap: 32px; text-align: center; }
    .stat-card { display: flex; flex-direction: column; align-items: center; gap: 6px; color: white; }
    .stat-card mat-icon { font-size: 30px !important; width: 30px !important; height: 30px !important; opacity: 0.85; margin-bottom: 4px; }
    .stat-value { font-size: 2.6rem; font-weight: 800; letter-spacing: -0.02em; font-variant-numeric: tabular-nums; }
    .stat-label { font-size: 0.92rem; opacity: 0.82; }

    /* ===== Features ===== */
    .features-section { padding: 88px 24px; max-width: var(--page-max); margin: 0 auto; }
    .section-header { text-align: center; margin-bottom: 56px; }
    .section-header h2 { font-size: clamp(1.7rem, 3vw, 2.1rem); font-weight: 800; color: var(--brand-ink); margin: 0 0 12px; }
    .section-header p { color: var(--brand-text-soft); font-size: 1.08rem; max-width: 60ch; margin: 0 auto; }
    .features-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(280px, 1fr)); gap: 24px; }
    .feature-card {
      background: var(--brand-surface); border: 1px solid var(--brand-border); border-radius: var(--radius-lg); padding: 32px;
      box-shadow: var(--brand-shadow-sm); transition: transform 0.2s ease, box-shadow 0.2s ease, border-color 0.2s ease; outline-offset: 4px;
    }
    .feature-card:hover, .feature-card:focus-visible {
      transform: translateY(-4px); box-shadow: var(--brand-shadow-md); border-color: var(--brand-primary-100);
    }
    .feature-icon {
      width: 56px; height: 56px; background: var(--brand-primary-soft); border: 1px solid var(--brand-primary-100);
      border-radius: var(--radius-md); display: flex; align-items: center; justify-content: center; margin-bottom: 20px;
    }
    .feature-icon mat-icon { color: var(--brand-primary); font-size: 28px !important; width: 28px !important; height: 28px !important; }
    .feature-card h3 { font-size: 1.15rem; font-weight: 700; margin: 0 0 10px; color: var(--brand-ink); }
    .feature-card p { color: var(--brand-text-soft); line-height: 1.65; margin: 0; font-size: 0.95rem; }

    /* ===== How it works ===== */
    .how-section { padding: 24px 24px 88px; max-width: var(--page-max); margin: 0 auto; }
    .steps { display: grid; grid-template-columns: repeat(auto-fit, minmax(220px, 1fr)); gap: 32px; }
    .step { text-align: center; padding: 28px 20px; }
    .step-number {
      width: 48px; height: 48px; background: var(--brand-gradient); color: white; border-radius: 50%;
      display: flex; align-items: center; justify-content: center; font-size: 1.3rem; font-weight: 800; margin: 0 auto 16px;
      box-shadow: 0 8px 20px rgba(15, 118, 110, 0.22);
    }
    .step mat-icon { font-size: 34px !important; width: 34px !important; height: 34px !important; color: var(--brand-primary-light); display: block; margin: 0 auto 12px; }
    .step h3 { font-size: 1.08rem; font-weight: 700; color: var(--brand-ink); margin: 0 0 8px; }
    .step p { color: var(--brand-text-soft); font-size: 0.92rem; line-height: 1.65; margin: 0; }

    /* ===== CTA ===== */
    .cta-section { background: var(--brand-gradient); padding: 80px 24px; text-align: center; position: relative; overflow: hidden; }
    .cta-content { max-width: 640px; margin: 0 auto; position: relative; z-index: 1; }
    .cta-content h2 { color: white; font-size: clamp(1.7rem, 3vw, 2.1rem); font-weight: 800; margin: 0 0 14px; letter-spacing: -0.02em; }
    .cta-content p { color: rgba(255,255,255,0.9); font-size: 1.08rem; margin-bottom: 34px; }
    .cta-buttons { display: flex; gap: 14px; justify-content: center; flex-wrap: wrap; }
    .cta-large { background: white !important; color: var(--brand-primary-dark) !important; padding: 14px 30px !important; font-size: 1rem !important; border-radius: var(--radius-md) !important; font-weight: 700 !important; height: 52px; }
    .cta-events { color: white !important; padding: 14px 22px !important; font-size: 1rem !important; height: 52px; }

    /* ===== Footer ===== */
    .home-footer {
      background: var(--brand-ink); color: rgba(255,255,255,0.7);
      padding: 32px 24px; display: flex; align-items: center; justify-content: space-between; flex-wrap: wrap; gap: 16px; font-size: 0.9rem;
    }
    .home-footer p { margin: 0; }
    .home-footer nav { display: flex; gap: 24px; }
    .home-footer a { color: rgba(255,255,255,0.72); text-decoration: none; }
    .home-footer a:hover { color: white; }
    .home-footer a:focus-visible { outline: 2px solid white; border-radius: 2px; }

    @media (max-width: 768px) {
      .hero { flex-direction: column; min-height: auto; padding: 48px 20px 40px; text-align: center; gap: 40px; }
      .hero-actions { justify-content: center; }
      .hero-tags { justify-content: center; }
      .hero-visual { display: none; }
      .features-section, .how-section { padding-top: 56px; padding-bottom: 56px; }
      .home-footer { flex-direction: column; text-align: center; }
    }
  `],
})
export class HomeComponent implements OnInit {
  private publicService = inject(PublicService);
  private i18n = inject(I18nService);
  private platformId = inject(PLATFORM_ID);
  private tt = (s: string) => this.i18n.t(s);

  stats = signal<PlatformStat[]>([
    { icon: 'people', value: '—', label: this.tt('Bénévoles inscrits') },
    { icon: 'business', value: '—', label: this.tt('Associations actives') },
    { icon: 'event', value: '—', label: this.tt('Événements organisés') },
    { icon: 'volunteer_activism', value: '—', label: this.tt('Participations validées') },
  ]);
  tags = ['Solidarité', 'Bénévolat', 'ONG', 'Communauté', 'Engagement', 'Social'].map(this.tt);
  features: Feature[] = [
    { icon: 'search', title: 'Trouvez votre association', description: 'Explorez des dizaines d\'associations par domaine, ville ou compétences requises.' },
    { icon: 'event_available', title: 'Inscrivez-vous aux événements', description: 'Participez à des missions de bénévolat près de chez vous ou en ligne.' },
    { icon: 'psychology', title: 'Développez vos compétences', description: 'Chaque mission est une opportunité d\'apprendre et de partager vos talents.' },
    { icon: 'notifications_active', title: 'Restez informé(e)', description: 'Recevez des notifications pour les nouvelles missions correspondant à vos intérêts.' },
    { icon: 'groups', title: 'Gérez votre équipe', description: 'Pour les associations : gérez membres, événements et présences en un seul endroit.' },
    { icon: 'verified', title: 'Plateforme de confiance', description: 'Chaque association est vérifiée par notre équipe avant publication.' },
  ].map(f => ({ ...f, title: this.tt(f.title), description: this.tt(f.description) }));
  steps = [
    { number: '1', icon: 'person_add', title: 'Créez votre compte', description: 'Inscription gratuite en quelques secondes. Renseignez vos compétences et votre ville.' },
    { number: '2', icon: 'search', title: 'Explorez et rejoignez', description: 'Parcourez les associations et les événements. Rejoignez ceux qui vous correspondent.' },
    { number: '3', icon: 'favorite', title: 'Agissez & impactez', description: 'Participez aux missions, collectez des retours et construisez votre profil bénévole.' },
  ].map(s => ({ ...s, title: this.tt(s.title), description: this.tt(s.description) }));

  ngOnInit() {
    this.publicService.getStats().subscribe({
      next: (res) => {
        if (res?.data) {
          const targets: StatTarget[] = [
            { icon: 'people', label: this.tt('Bénévoles inscrits'), target: res.data.totalVolunteers },
            { icon: 'business', label: this.tt('Associations actives'), target: res.data.totalOrganizations },
            { icon: 'event', label: this.tt('Événements organisés'), target: res.data.totalEvents },
            { icon: 'volunteer_activism', label: this.tt('Participations validées'), target: res.data.totalParticipations },
          ];
          this.revealStats(targets);
        }
      },
      error: () => {} // garde les tirets si l'API est indisponible
    });
  }

  /** Affiche les stats — avec compteur animé côté navigateur, sauf si mouvement réduit. */
  private revealStats(targets: StatTarget[]) {
    const setFinal = () =>
      this.stats.set(targets.map(t => ({ icon: t.icon, value: this.fmt(t.target), label: t.label })));

    const reducedMotion = isPlatformBrowser(this.platformId)
      && typeof matchMedia !== 'undefined'
      && matchMedia('(prefers-reduced-motion: reduce)').matches;

    if (!isPlatformBrowser(this.platformId) || reducedMotion) {
      setFinal();
      return;
    }

    const duration = 1100;
    const start = performance.now();
    const tick = (now: number) => {
      const p = Math.min(1, (now - start) / duration);
      const eased = 1 - Math.pow(1 - p, 3); // easeOutCubic
      this.stats.set(targets.map(t => ({
        icon: t.icon,
        value: this.fmt(Math.round(t.target * eased)),
        label: t.label,
      })));
      if (p < 1) requestAnimationFrame(tick);
    };
    requestAnimationFrame(tick);
  }

  private fmt(n: number): string {
    return n >= 1000 ? (n / 1000).toFixed(1) + 'k' : n.toString();
  }
}
