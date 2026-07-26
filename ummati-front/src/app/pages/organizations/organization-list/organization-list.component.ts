import { Component, signal, OnInit } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { OrganizationService, OrganizationSummary } from '../../../core/services/organization.service';
import { TPipe } from '../../../shared/pipes/t.pipe';
import { CardSkeletonComponent } from '../../../shared/components/skeleton/skeleton.component';

/** Teintes de bandeau retenues — famille froide + chaudes franches, pas de jaune-vert. */
const ORG_COVER_HUES = [172, 196, 214, 232, 258, 286, 320, 344, 12, 26];

@Component({
  selector: 'app-organization-list',
  standalone: true,
  imports: [MatButtonModule, MatIconModule, MatFormFieldModule, MatInputModule,
    MatSelectModule, MatPaginatorModule, RouterLink, FormsModule, TPipe, CardSkeletonComponent],
  template: `
    <div class="page">
      <header class="page-head">
        <div>
          <h1 class="page-title">{{ 'Organisations' | t }}</h1>
          <p class="page-sub">{{ 'Trouvez une association qui vous correspond' | t }}</p>
        </div>
        <a mat-flat-button routerLink="/organizations/new" class="create-btn no-underline">
          <mat-icon>add</mat-icon> {{ 'Créer une ONG' | t }}
        </a>
      </header>

      <div class="filters-row">
        <mat-form-field appearance="outline" class="search-field" subscriptSizing="dynamic">
          <mat-label>{{ 'Rechercher' | t }}</mat-label>
          <input matInput [(ngModel)]="searchQuery" (keyup.enter)="loadOrganizations()"
                 [placeholder]="'Nom ou description...' | t" />
          <mat-icon matSuffix>search</mat-icon>
        </mat-form-field>
        <mat-form-field appearance="outline" subscriptSizing="dynamic">
          <mat-label>{{ 'Domaine' | t }}</mat-label>
          <mat-select [(ngModel)]="selectedDomain" (selectionChange)="loadOrganizations()">
            <mat-option [value]="null">{{ 'Tous' | t }}</mat-option>
            @for (d of domains; track d.value) {
              <mat-option [value]="d.value">{{ d.label | t }}</mat-option>
            }
          </mat-select>
        </mat-form-field>
      </div>

      @if (loading()) {
        <div class="org-grid" aria-busy="true">
          @for (i of skeletonSlots; track i) { <app-card-skeleton /> }
        </div>
      } @else if (organizations().length === 0) {
        <div class="empty-state">
          <div class="empty-badge"><mat-icon>diversity_3</mat-icon></div>
          <h3>{{ 'Aucune organisation trouvée' | t }}</h3>
          <p>{{ 'Essayez d\\'ajuster vos filtres ou créez la vôtre !' | t }}</p>
          <a mat-flat-button routerLink="/organizations/new" class="no-underline">{{ 'Créer une ONG' | t }}</a>
        </div>
      } @else {
        <div class="org-grid">
          @for (org of organizations(); track org.id) {
            <a class="org-card hover-lift no-underline" [routerLink]="['/organizations', org.slug]">
              <!-- Bandeau : couleur dérivée du nom, stable d'une visite à l'autre -->
              <div class="org-cover" [style.--cover-hue]="hueFor(org.name)">
                <span class="org-cover-pattern pattern-stars" aria-hidden="true"></span>
                <span class="badge badge-neutral domain-badge">{{ org.domain }}</span>
              </div>

              <div class="org-body">
                @if (org.logoUrl) {
                  <img [src]="org.logoUrl" [alt]="org.name" class="org-logo" loading="lazy" />
                } @else {
                  <span class="org-logo org-logo-fallback" aria-hidden="true">{{ initials(org.name) }}</span>
                }

                <h3 class="org-name">{{ org.name }}</h3>
                <div class="org-meta">
                  <span><mat-icon>location_on</mat-icon>{{ org.city }}</span>
                  <span><mat-icon>group</mat-icon><b class="tnum">{{ org.memberCount }}</b> {{ 'membres' | t }}</span>
                </div>
                <p class="org-excerpt">{{ org.descriptionExcerpt }}</p>
              </div>
            </a>
          }
        </div>
        <mat-paginator [length]="totalElements()" [pageSize]="12" [pageIndex]="currentPage()"
                       (page)="onPage($event)" />
      }
    </div>
  `,
  styles: [`
    .create-btn { height: 46px; }
    .search-field { flex: 1; min-width: 240px; }

    .empty-state { text-align: center; padding: var(--space-9) var(--space-5); }
    .empty-badge {
      width: 76px; height: 76px; margin: 0 auto var(--space-4); border-radius: 50%;
      background: var(--brand-surface-2); border: 1px solid var(--brand-border);
      display: flex; align-items: center; justify-content: center;
    }
    .empty-badge mat-icon { font-size: 34px; width: 34px; height: 34px; color: var(--brand-text-faint); }
    .empty-state h3 { font-size: 1.25rem; margin: 0 0 var(--space-2); }
    .empty-state p { color: var(--brand-text-soft); margin: 0 0 var(--space-5); }

    .org-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(310px, 1fr)); gap: var(--space-4); }

    .org-card {
      display: flex; flex-direction: column; overflow: hidden; color: inherit;
      background: var(--brand-surface); border: 1px solid var(--brand-border);
      border-radius: var(--radius-card); box-shadow: var(--brand-shadow-xs);
    }

    .org-cover {
      position: relative; height: 84px;
      background: linear-gradient(
        135deg,
        hsl(var(--cover-hue) 42% 42%) 0%,
        hsl(calc(var(--cover-hue) + 28) 48% 52%) 100%
      );
    }
    .org-cover-pattern { position: absolute; inset: 0; opacity: 0.16; filter: invert(1) brightness(3); }
    .domain-badge {
      position: absolute; top: var(--space-3); inset-inline-end: var(--space-3);
      background: color-mix(in oklab, var(--brand-surface) 88%, transparent) !important;
      backdrop-filter: blur(6px);
    }

    .org-body { padding: 0 var(--space-5) var(--space-5); display: flex; flex-direction: column; flex: 1; }
    /*
      Logo à cheval sur le bandeau. position/z-index sont indispensables :
      .org-cover est positionné, donc il se peindrait AU-DESSUS d'un logo
      resté statique et le rognerait.
    */
    .org-logo {
      position: relative; z-index: 1;
      width: 58px; height: 58px; border-radius: var(--radius-md); margin-top: -29px;
      object-fit: cover; background: var(--brand-surface);
      border: 3px solid var(--brand-surface); box-shadow: var(--brand-shadow-sm);
      box-sizing: border-box;
    }
    .org-logo-fallback {
      display: flex; align-items: center; justify-content: center;
      background: var(--brand-primary-100); color: var(--brand-primary-dark);
      font-weight: 800; font-size: 1.15rem; letter-spacing: -0.02em;
    }

    .org-name {
      font-size: 1.12rem; font-weight: 700; margin: var(--space-3) 0 var(--space-2);
      color: var(--brand-ink); line-height: 1.35;
    }
    .org-meta {
      display: flex; gap: var(--space-4); flex-wrap: wrap;
      color: var(--brand-text-soft); font-size: 0.85rem; margin-bottom: var(--space-3);
    }
    .org-meta span { display: flex; align-items: center; gap: 5px; }
    .org-meta mat-icon { font-size: 16px; width: 16px; height: 16px; color: var(--brand-text-faint); }
    .org-meta b { color: var(--brand-ink); font-weight: 700; }
    .org-excerpt {
      color: var(--brand-text-soft); font-size: 0.92rem; line-height: 1.6; margin: 0;
      display: -webkit-box; -webkit-line-clamp: 3; -webkit-box-orient: vertical; overflow: hidden;
    }
  `],
})
export class OrganizationListComponent implements OnInit {
  organizations = signal<OrganizationSummary[]>([]);
  loading = signal(true);
  totalElements = signal(0);
  currentPage = signal(0);
  searchQuery = '';
  selectedDomain: string | null = null;

  readonly skeletonSlots = Array.from({ length: 6 }, (_, i) => i);

  domains = [
    { value: 'EDUCATION', label: 'Éducation' }, { value: 'SANTE', label: 'Santé' },
    { value: 'ENVIRONNEMENT', label: 'Environnement' }, { value: 'SOCIAL', label: 'Social' },
    { value: 'CULTURE', label: 'Culture' }, { value: 'SPORT', label: 'Sport' },
    { value: 'HUMANITAIRE', label: 'Humanitaire' }, { value: 'DROITS_HUMAINS', label: 'Droits humains' },
    { value: 'AIDE_URGENCE', label: "Aide d'urgence" }, { value: 'AUTRE', label: 'Autre' },
  ];

  constructor(private orgService: OrganizationService) {}

  ngOnInit() { this.loadOrganizations(); }

  /** Initiales affichées quand l'ONG n'a pas encore de logo. */
  initials(name: string): string {
    return (name || '?')
      .split(/\s+/)
      .filter(Boolean)
      .slice(0, 2)
      .map(w => w[0])
      .join('')
      .toUpperCase();
  }

  /**
   * Teinte du bandeau dérivée du nom : chaque ONG garde la même couleur
   * d'une page à l'autre, sans rien stocker côté serveur.
   *
   * On tire dans une palette fermée plutôt que sur les 360°, sinon les
   * teintes jaune-vert (~60-100°) sortent ternes et jurent avec le teal.
   */
  hueFor(name: string): number {
    let hash = 0;
    for (let i = 0; i < (name?.length ?? 0); i++) {
      hash = (hash * 31 + name.charCodeAt(i)) % 997;
    }
    return ORG_COVER_HUES[hash % ORG_COVER_HUES.length];
  }

  loadOrganizations() {
    this.loading.set(true);
    this.orgService.list({
      page: this.currentPage(), size: 12,
      domain: this.selectedDomain || undefined,
      search: this.searchQuery || undefined,
    }).subscribe({
      next: res => {
        this.organizations.set(res.data.content);
        this.totalElements.set(res.data.totalElements);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  onPage(event: PageEvent) {
    this.currentPage.set(event.pageIndex);
    this.loadOrganizations();
  }
}
