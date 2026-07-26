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
import { RevealDirective } from '../../shared/directives/reveal.directive';

interface PlatformStat { icon: string; value: string; label: string; }
interface StatTarget { icon: string; label: string; target: number; }
interface Feature { icon: string; title: string; description: string; }

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [CommonModule, MatButtonModule, MatIconModule, MatCardModule, RouterLink, TPipe, RevealDirective],
  template: `
    <!-- ===== Hero ===== -->
    <section class="hero" aria-label="Bienvenue sur Ummati">
      <div class="hero-veil pattern-stars" aria-hidden="true"></div>

      <div class="hero-inner">
        <div class="hero-content">
          <span class="kicker">{{ 'Plateforme de bénévolat' | t }}</span>
          <h1>
            {{ 'Le temps que vous donnez' | t }}
            <span class="underlined">{{ 'change une vie' | t }}</span>
          </h1>
          <p class="hero-desc">
            {{ 'Ummati relie les bénévoles aux associations qui ont besoin d\\'eux. Trouvez une mission près de chez vous, suivez vos heures, obtenez votre attestation.' | t }}
          </p>

          <div class="hero-actions">
            <a mat-flat-button routerLink="/register" class="cta-primary no-underline"
               aria-label="Créer un compte bénévole">
              {{ 'Devenir bénévole' | t }}
              <mat-icon>arrow_forward</mat-icon>
            </a>
            <a mat-stroked-button routerLink="/events" class="cta-secondary no-underline"
               aria-label="Parcourir les missions">
              <mat-icon>search</mat-icon>
              {{ 'Voir les missions' | t }}
            </a>
          </div>

          <p class="hero-note">
            <mat-icon aria-hidden="true">verified_user</mat-icon>
            {{ 'Gratuit pour les bénévoles · Associations vérifiées une à une' | t }}
          </p>
        </div>

        <!-- Aperçu produit : plus parlant qu'une illustration abstraite -->
        <div class="hero-preview" aria-hidden="true">
          <div class="preview-card preview-main">
            <div class="pc-top">
              <span class="pc-date"><b>14</b><span>MARS</span></span>
              <span class="badge badge-brand">{{ 'Distribution' | t }}</span>
            </div>
            <h4>{{ 'Maraude solidaire — centre-ville' | t }}</h4>
            <p class="pc-meta"><mat-icon>location_on</mat-icon> Lyon 3ᵉ · 14h00</p>
            <div class="pc-progress"><span style="width: 72%"></span></div>
            <p class="pc-spots">18 / 25 {{ 'inscrits' | t }}</p>
          </div>

          <div class="preview-card preview-org">
            <span class="po-avatar">SA</span>
            <div>
              <strong>{{ 'Solidarité Active' | t }}</strong>
              <span class="po-sub">{{ '124 bénévoles' | t }}</span>
            </div>
            <mat-icon class="po-check">verified</mat-icon>
          </div>

          <div class="preview-card preview-hours">
            <mat-icon>schedule</mat-icon>
            <div>
              <strong class="tnum">36 h</strong>
              <span class="po-sub">{{ 'validées cette année' | t }}</span>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- ===== Chiffres ===== -->
    <section class="stats-section" aria-label="Statistiques de la plateforme">
      <div class="stats-container">
        @for (stat of stats(); track stat.label) {
          <div class="stat-card">
            <mat-icon aria-hidden="true">{{ stat.icon }}</mat-icon>
            <span class="stat-value tnum">{{ stat.value }}</span>
            <span class="stat-label">{{ stat.label }}</span>
          </div>
        }
      </div>
    </section>

    <!-- ===== Fonctionnalités ===== -->
    <section class="features-section" aria-labelledby="features-title">
      <div class="section-header" appReveal>
        <span class="kicker">{{ 'Ce que vous y gagnez' | t }}</span>
        <h2 id="features-title">{{ 'Pensé pour celles et ceux qui donnent de leur temps' | t }}</h2>
        <p>{{ 'Côté bénévole comme côté association, tout est au même endroit.' | t }}</p>
      </div>
      <div class="features-grid">
        @for (feature of features; track feature.title; let i = $index) {
          <article class="feature-card" tabindex="0" appReveal [revealDelay]="i * 70">
            <div class="feature-icon">
              <mat-icon aria-hidden="true">{{ feature.icon }}</mat-icon>
            </div>
            <h3>{{ feature.title }}</h3>
            <p>{{ feature.description }}</p>
          </article>
        }
      </div>
    </section>

    <!-- ===== Comment ça marche ===== -->
    <section class="how-section" aria-labelledby="how-title">
      <div class="section-header" appReveal>
        <span class="kicker">{{ 'En pratique' | t }}</span>
        <h2 id="how-title">{{ 'Trois étapes, dix minutes' | t }}</h2>
      </div>
      <ol class="steps">
        @for (step of steps; track step.number; let i = $index) {
          <li class="step" appReveal [revealDelay]="i * 100">
            <div class="step-number" aria-hidden="true">{{ step.number }}</div>
            <h3>{{ step.title }}</h3>
            <p>{{ step.description }}</p>
          </li>
        }
      </ol>
    </section>

    <!-- ===== Appel à l'action ===== -->
    <section class="cta-section" aria-labelledby="cta-title">
      <div class="cta-veil pattern-stars" aria-hidden="true"></div>
      <div class="cta-content" appReveal>
        <h2 id="cta-title">{{ 'Votre première mission vous attend' | t }}</h2>
        <p>{{ 'Créez votre compte, choisissez une cause, donnez le temps que vous pouvez.' | t }}</p>
        <div class="cta-buttons">
          <a mat-flat-button routerLink="/register" class="cta-large no-underline">{{ 'Créer mon compte — c\\'est gratuit' | t }}</a>
          <a mat-button routerLink="/organizations" class="cta-events no-underline">
            {{ 'Découvrir les associations' | t }} <mat-icon>arrow_forward</mat-icon>
          </a>
        </div>
      </div>
    </section>

    <!-- ===== Pied de page ===== -->
    <footer class="home-footer" role="contentinfo">
      <div class="footer-inner">
        <div class="footer-brand">
          <span class="brand-mark" aria-hidden="true">
            <svg viewBox="0 0 32 32" width="18" height="18" fill="none" stroke="currentColor" stroke-width="1.8">
              <rect x="6" y="6" width="20" height="20" rx="2" />
              <rect x="6" y="6" width="20" height="20" rx="2" transform="rotate(45 16 16)" />
            </svg>
          </span>
          <span>Ummati</span>
        </div>
        <nav aria-label="Liens du pied de page">
          <a routerLink="/organizations" class="no-underline">{{ 'Organisations' | t }}</a>
          <a routerLink="/events" class="no-underline">{{ 'Événements' | t }}</a>
          <a routerLink="/login" class="no-underline">{{ 'Connexion' | t }}</a>
        </nav>
        <p class="footer-legal">{{ '© 2026 Ummati — Bénévolat & Gestion d\\'ONG' | t }}</p>
      </div>
    </footer>
  `,
  styles: [`
    :host { display: block; }

    /* ===== Hero ===== */
    .hero { position: relative; overflow: hidden; padding: var(--space-9) 0 var(--space-8); }
    .hero-veil {
      position: absolute; inset: -10% -10% auto -10%; height: 130%;
      -webkit-mask-image: radial-gradient(70% 60% at 70% 25%, #000 0%, transparent 72%);
      mask-image: radial-gradient(70% 60% at 70% 25%, #000 0%, transparent 72%);
      pointer-events: none;
    }
    .hero-inner {
      position: relative; z-index: 1;
      max-width: var(--page-max); margin: 0 auto; padding-inline: var(--space-5);
      display: grid; grid-template-columns: minmax(0, 1.05fr) minmax(0, 0.95fr);
      align-items: center; gap: var(--space-8);
    }
    .hero-content > * { animation: fade-up 0.65s var(--ease-out) both; }
    .hero-content > *:nth-child(1) { animation-delay: 0.02s; }
    .hero-content > *:nth-child(2) { animation-delay: 0.09s; }
    .hero-content > *:nth-child(3) { animation-delay: 0.16s; }
    .hero-content > *:nth-child(4) { animation-delay: 0.23s; }
    .hero-content > *:nth-child(5) { animation-delay: 0.3s; }

    h1 {
      font-size: clamp(2.6rem, 1.4rem + 4.4vw, 4.4rem);
      font-weight: 600; line-height: 1.04; margin: 0 0 var(--space-4);
      color: var(--brand-ink);
    }
    /* Trait de surlignage tracé à la main, plutôt qu'un dégradé de texte */
    .underlined {
      display: inline-block; position: relative; color: var(--brand-primary-dark);
    }
    .underlined::after {
      content: ''; position: absolute; inset-inline: -2px; bottom: 0.06em; height: 0.28em;
      background: color-mix(in oklab, var(--brand-accent) 42%, transparent);
      border-radius: 0.14em; z-index: -1;
    }
    .hero-desc {
      font-size: clamp(1.05rem, 0.98rem + 0.3vw, 1.2rem);
      color: var(--brand-text); line-height: 1.68; margin: 0 0 var(--space-6); max-width: 54ch;
    }
    .hero-actions { display: flex; gap: var(--space-3); flex-wrap: wrap; margin-bottom: var(--space-5); }
    .cta-primary, .cta-secondary { height: 54px; padding-inline: var(--space-6) !important; font-size: 1rem !important; }
    .hero-note {
      display: flex; align-items: center; gap: var(--space-2);
      color: var(--brand-text-soft); font-size: 0.9rem; margin: 0;
    }
    .hero-note mat-icon { font-size: 18px; width: 18px; height: 18px; color: var(--brand-success); }

    /* ---- Aperçu produit ---- */
    .hero-preview {
      position: relative; height: 420px;
      animation: fade-up 0.9s var(--ease-out) 0.2s both;
    }
    .preview-card {
      position: absolute; background: var(--brand-surface);
      border: 1px solid var(--brand-border); border-radius: var(--radius-lg);
      box-shadow: var(--brand-shadow-lg);
    }
    .preview-main { inset-inline-start: 8%; top: 40px; width: min(340px, 88%); padding: var(--space-5); }
    .pc-top { display: flex; align-items: center; justify-content: space-between; margin-bottom: var(--space-4); }
    .pc-date {
      display: flex; flex-direction: column; align-items: center; justify-content: center;
      width: 52px; height: 52px; border-radius: var(--radius-sm);
      background: var(--brand-primary-soft); border: 1px solid var(--brand-primary-100);
      color: var(--brand-primary-dark); line-height: 1.1;
    }
    .pc-date b { font-size: 1.25rem; font-weight: 800; }
    .pc-date span { font-size: 0.6rem; font-weight: 700; letter-spacing: 0.08em; }
    .preview-main h4 { margin: 0 0 var(--space-2); font-size: 1.05rem; font-weight: 700; color: var(--brand-ink); }
    .pc-meta {
      display: flex; align-items: center; gap: 6px; margin: 0 0 var(--space-4);
      color: var(--brand-text-soft); font-size: 0.86rem;
    }
    .pc-meta mat-icon { font-size: 16px; width: 16px; height: 16px; }
    .pc-progress { height: 6px; border-radius: 3px; background: var(--brand-surface-3); overflow: hidden; }
    .pc-progress span { display: block; height: 100%; border-radius: 3px; background: var(--brand-gradient); }
    .pc-spots { margin: 8px 0 0; font-size: 0.8rem; color: var(--brand-text-soft); }

    .preview-org {
      inset-inline-end: 0; top: 0; display: flex; align-items: center; gap: var(--space-3);
      padding: var(--space-3) var(--space-4);
    }
    .po-avatar {
      width: 38px; height: 38px; border-radius: 50%; background: var(--brand-gradient-warm);
      color: #fff; display: inline-flex; align-items: center; justify-content: center;
      font-weight: 700; font-size: 0.82rem;
    }
    .preview-org strong, .preview-hours strong { display: block; font-size: 0.92rem; color: var(--brand-ink); }
    .po-sub { font-size: 0.78rem; color: var(--brand-text-soft); }
    .po-check { color: var(--brand-primary); font-size: 20px; width: 20px; height: 20px; }

    .preview-hours {
      inset-inline-end: 6%; bottom: 24px; display: flex; align-items: center; gap: var(--space-3);
      padding: var(--space-3) var(--space-4);
    }
    .preview-hours > mat-icon { color: var(--brand-accent); }
    .preview-hours strong { font-size: 1.15rem; font-weight: 800; }

    /* ===== Chiffres ===== */
    .stats-section {
      background: var(--brand-gradient-deep); padding: var(--space-7) var(--space-5);
      position: relative; overflow: hidden;
    }
    .stats-container {
      max-width: var(--page-max); margin: 0 auto; display: grid;
      grid-template-columns: repeat(auto-fit, minmax(170px, 1fr)); gap: var(--space-6); text-align: center;
    }
    .stat-card { display: flex; flex-direction: column; align-items: center; gap: 4px; color: #fff; }
    .stat-card mat-icon { font-size: 26px !important; width: 26px !important; height: 26px !important; opacity: 0.7; }
    .stat-value { font-size: clamp(2rem, 1.4rem + 1.8vw, 2.8rem); font-weight: 800; letter-spacing: -0.03em; line-height: 1.1; }
    .stat-label { font-size: 0.88rem; opacity: 0.78; }

    /* ===== Sections ===== */
    .features-section, .how-section {
      padding: var(--space-9) var(--space-5); max-width: var(--page-max); margin: 0 auto;
    }
    .how-section { padding-top: 0; }
    .section-header { text-align: center; margin-bottom: var(--space-7); }
    .section-header h2 {
      font-size: clamp(1.9rem, 1.3rem + 2vw, 2.6rem); font-weight: 600;
      color: var(--brand-ink); margin: 0 0 var(--space-3);
    }
    .section-header p { color: var(--brand-text-soft); font-size: 1.05rem; max-width: 58ch; margin: 0 auto; }

    .features-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(290px, 1fr)); gap: var(--space-4); }
    .feature-card {
      background: var(--brand-surface); border: 1px solid var(--brand-border);
      border-radius: var(--radius-card); padding: var(--space-6);
      box-shadow: var(--brand-shadow-xs);
      transition: transform 0.22s var(--ease-out), box-shadow 0.22s var(--ease-out), border-color 0.22s var(--ease-out);
      outline-offset: 4px;
    }
    .feature-card:hover, .feature-card:focus-visible {
      transform: translateY(-4px); box-shadow: var(--brand-shadow-md);
      border-color: color-mix(in oklab, var(--brand-primary) 35%, var(--brand-border));
    }
    .feature-icon {
      width: 50px; height: 50px; background: var(--brand-primary-soft);
      border: 1px solid var(--brand-primary-100); border-radius: var(--radius-md);
      display: flex; align-items: center; justify-content: center; margin-bottom: var(--space-4);
      transition: transform 0.3s var(--ease-spring);
    }
    .feature-card:hover .feature-icon { transform: scale(1.08) rotate(-4deg); }
    .feature-icon mat-icon { color: var(--brand-primary); font-size: 26px !important; width: 26px !important; height: 26px !important; }
    .feature-card h3 { font-size: 1.1rem; font-weight: 700; margin: 0 0 var(--space-2); color: var(--brand-ink); }
    .feature-card p { color: var(--brand-text-soft); line-height: 1.65; margin: 0; font-size: 0.94rem; }

    /* ===== Étapes : un fil qui relie les numéros ===== */
    .steps {
      list-style: none; margin: 0; padding: 0;
      display: grid; grid-template-columns: repeat(auto-fit, minmax(240px, 1fr)); gap: var(--space-6);
      position: relative;
    }
    .step { text-align: center; position: relative; padding: 0 var(--space-3); }
    .step:not(:last-child)::before {
      content: ''; position: absolute; top: 26px; inset-inline-start: calc(50% + 34px); inset-inline-end: calc(-50% + 34px);
      border-top: 2px dashed var(--brand-border-strong);
    }
    .step-number {
      width: 52px; height: 52px; background: var(--brand-surface); color: var(--brand-primary-dark);
      border: 2px solid var(--brand-primary-100); border-radius: 50%;
      display: flex; align-items: center; justify-content: center;
      font-size: 1.2rem; font-weight: 800; margin: 0 auto var(--space-4);
      position: relative; z-index: 1;
    }
    .step h3 { font-size: 1.06rem; font-weight: 700; color: var(--brand-ink); margin: 0 0 var(--space-2); }
    .step p { color: var(--brand-text-soft); font-size: 0.92rem; line-height: 1.65; margin: 0; }

    /* ===== CTA ===== */
    .cta-section {
      background: var(--brand-gradient); padding: var(--space-9) var(--space-5);
      text-align: center; position: relative; overflow: hidden;
    }
    .cta-veil { position: absolute; inset: 0; opacity: 0.12; filter: invert(1) brightness(3); }
    .cta-content { max-width: 620px; margin: 0 auto; position: relative; z-index: 1; }
    .cta-content h2 {
      color: #fff; font-size: clamp(1.9rem, 1.3rem + 2vw, 2.6rem);
      font-weight: 600; margin: 0 0 var(--space-3);
    }
    .cta-content p { color: rgba(255, 255, 255, 0.88); font-size: 1.06rem; margin-bottom: var(--space-6); }
    .cta-buttons { display: flex; gap: var(--space-3); justify-content: center; flex-wrap: wrap; }
    .cta-large {
      background: #fff !important; color: var(--brand-primary-dark) !important;
      height: 54px; padding-inline: var(--space-6) !important;
      font-size: 1rem !important; font-weight: 700 !important;
    }
    :root[data-theme='dark'] .cta-large { color: #06231f !important; }
    .cta-events { color: #fff !important; height: 54px; padding-inline: var(--space-4) !important; font-size: 1rem !important; }

    /* ===== Pied de page ===== */
    .home-footer { background: var(--brand-surface-2); border-top: 1px solid var(--brand-border); padding: var(--space-6) var(--space-5); }
    .footer-inner {
      max-width: var(--page-max); margin: 0 auto;
      display: flex; align-items: center; justify-content: space-between; flex-wrap: wrap; gap: var(--space-4);
    }
    .footer-brand { display: flex; align-items: center; gap: var(--space-2); font-weight: 700; color: var(--brand-ink); }
    .footer-brand .brand-mark {
      width: 30px; height: 30px; border-radius: 9px; color: #fff; background: var(--brand-gradient);
      display: inline-flex; align-items: center; justify-content: center;
    }
    .home-footer nav { display: flex; gap: var(--space-5); flex-wrap: wrap; }
    .home-footer a { color: var(--brand-text-soft); font-size: 0.92rem; font-weight: 600; }
    .home-footer a:hover { color: var(--brand-primary-dark); }
    .footer-legal { margin: 0; color: var(--brand-text-faint); font-size: 0.85rem; }

    @media (max-width: 980px) {
      .hero-inner { grid-template-columns: 1fr; gap: var(--space-6); }
      .hero-preview { display: none; }
      .hero { padding-top: var(--space-7); }
      .step:not(:last-child)::before { display: none; }
    }
    @media (max-width: 768px) {
      .features-section, .how-section { padding: var(--space-7) var(--space-4); }
      .footer-inner { flex-direction: column; text-align: center; }
    }
  `],
})
export class HomeComponent implements OnInit {
  private publicService = inject(PublicService);
  private i18n = inject(I18nService);
  private platformId = inject(PLATFORM_ID);
  private tt = (s: string) => this.i18n.t(s);

  stats = signal<PlatformStat[]>([
    { icon: 'diversity_3', value: '—', label: this.tt('Bénévoles inscrits') },
    { icon: 'apartment', value: '—', label: this.tt('Associations actives') },
    { icon: 'event', value: '—', label: this.tt('Événements organisés') },
    { icon: 'volunteer_activism', value: '—', label: this.tt('Participations validées') },
  ]);
  features: Feature[] = [
    { icon: 'search', title: 'Trouvez votre association', description: 'Explorez des dizaines d\'associations par domaine, ville ou compétences requises.' },
    { icon: 'event_available', title: 'Inscrivez-vous aux événements', description: 'Participez à des missions de bénévolat près de chez vous ou en ligne.' },
    { icon: 'schedule', title: 'Suivez vos heures', description: 'Vos heures validées sont comptabilisées et votre attestation se génère toute seule.' },
    { icon: 'notifications_active', title: 'Restez informé(e)', description: 'Recevez des notifications pour les nouvelles missions correspondant à vos intérêts.' },
    { icon: 'groups', title: 'Gérez votre équipe', description: 'Pour les associations : gérez membres, événements et présences en un seul endroit.' },
    { icon: 'verified', title: 'Plateforme de confiance', description: 'Chaque association est vérifiée par notre équipe avant publication.' },
  ].map(f => ({ ...f, title: this.tt(f.title), description: this.tt(f.description) }));
  steps = [
    { number: '1', title: 'Créez votre compte', description: 'Inscription gratuite en quelques secondes. Renseignez vos compétences et votre ville.' },
    { number: '2', title: 'Explorez et rejoignez', description: 'Parcourez les associations et les événements. Rejoignez ceux qui vous correspondent.' },
    { number: '3', title: 'Agissez & impactez', description: 'Participez aux missions, collectez des retours et construisez votre profil bénévole.' },
  ].map(s => ({ ...s, title: this.tt(s.title), description: this.tt(s.description) }));

  ngOnInit() {
    this.publicService.getStats().subscribe({
      next: (res) => {
        if (res?.data) {
          const targets: StatTarget[] = [
            { icon: 'diversity_3', label: this.tt('Bénévoles inscrits'), target: res.data.totalVolunteers },
            { icon: 'apartment', label: this.tt('Associations actives'), target: res.data.totalOrganizations },
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
