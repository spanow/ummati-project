import { Component, signal, OnInit } from '@angular/core';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatChipsModule } from '@angular/material/chips';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { OrganizationService, OrganizationSummary } from '../../../core/services/organization.service';
import { TPipe } from '../../../shared/pipes/t.pipe';

@Component({
  selector: 'app-organization-list',
  standalone: true,
  imports: [MatCardModule, MatButtonModule, MatIconModule, MatFormFieldModule, MatInputModule,
    MatSelectModule, MatChipsModule, MatPaginatorModule, MatProgressSpinnerModule, RouterLink, FormsModule, TPipe],
  template: `
    <div class="page">
      <header class="page-head">
        <div>
          <h1 class="page-title">{{ 'Organisations' | t }}</h1>
          <p class="page-sub">{{ 'Trouvez une association qui vous correspond' | t }}</p>
        </div>
        <a mat-flat-button routerLink="/organizations/new" class="create-btn">
          <mat-icon>add</mat-icon> {{ 'Créer une ONG' | t }}
        </a>
      </header>

      <div class="filters-row">
        <mat-form-field appearance="outline" class="search-field">
          <mat-label>{{ 'Rechercher' | t }}</mat-label>
          <input matInput [(ngModel)]="searchQuery" (keyup.enter)="loadOrganizations()"
                 [placeholder]="'Nom ou description...' | t" />
          <mat-icon matSuffix>search</mat-icon>
        </mat-form-field>
        <mat-form-field appearance="outline">
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
        <div class="state-center"><mat-spinner diameter="40" /></div>
      } @else if (organizations().length === 0) {
        <div class="empty-state">
          <mat-icon class="empty-icon">groups</mat-icon>
          <h3>{{ 'Aucune organisation trouvée' | t }}</h3>
          <p>{{ 'Essayez d\\'ajuster vos filtres ou créez la vôtre !' | t }}</p>
          <a mat-flat-button routerLink="/organizations/new">{{ 'Créer une ONG' | t }}</a>
        </div>
      } @else {
        <div class="org-grid">
          @for (org of organizations(); track org.id) {
            <mat-card class="org-card hover-lift" [routerLink]="['/organizations', org.slug]">
              <div class="card-header">
                @if (org.logoUrl) {
                  <img [src]="org.logoUrl" [alt]="org.name" class="org-logo" />
                } @else {
                  <div class="org-logo-placeholder">
                    <mat-icon>business</mat-icon>
                  </div>
                }
                <mat-chip class="domain-chip">{{ org.domain }}</mat-chip>
              </div>
              <mat-card-content>
                <h3 class="org-name">{{ org.name }}</h3>
                <div class="org-meta">
                  <span><mat-icon class="meta-icon">location_on</mat-icon> {{ org.city }}</span>
                  <span><mat-icon class="meta-icon">people</mat-icon> {{ org.memberCount }} {{ 'membres' | t }}</span>
                </div>
                <p class="org-excerpt">{{ org.descriptionExcerpt }}</p>
              </mat-card-content>
            </mat-card>
          }
        </div>
        <mat-paginator [length]="totalElements()" [pageSize]="12" [pageIndex]="currentPage()"
                       (page)="onPage($event)" />
      }
    </div>
  `,
  styles: [`
    .create-btn { height: 44px; }
    .search-field { flex: 1; min-width: 240px; }
    .empty-state { text-align: center; padding: 80px 24px; }
    .empty-icon { font-size: 64px; width: 64px; height: 64px; color: var(--brand-text-faint); }
    .empty-state h3 { margin-top: 16px; font-size: 1.3rem; }
    .empty-state p { color: var(--brand-text-soft); margin-bottom: 24px; }
    .org-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(320px, 1fr)); gap: 24px; }
    .org-card { cursor: pointer; overflow: hidden; }
    .card-header { display: flex; align-items: center; gap: 12px; padding: 20px 20px 0; }
    .org-logo { width: 48px; height: 48px; border-radius: 10px; object-fit: cover; }
    .org-logo-placeholder { width: 48px; height: 48px; border-radius: 10px; background: var(--brand-primary-100); display: flex; align-items: center; justify-content: center; }
    .org-logo-placeholder mat-icon { color: var(--brand-primary); }
    .domain-chip { margin-left: auto; font-size: 12px; }
    .org-name { font-size: 1.15rem; font-weight: 700; margin: 16px 0 8px; color: var(--brand-ink); }
    .org-meta { display: flex; gap: 16px; color: var(--brand-text-faint); font-size: 0.85rem; margin-bottom: 12px; }
    .org-meta span { display: flex; align-items: center; gap: 4px; }
    .meta-icon { font-size: 16px; width: 16px; height: 16px; }
    .org-excerpt { color: var(--brand-text-soft); font-size: 0.9rem; line-height: 1.55; }
  `],
})
export class OrganizationListComponent implements OnInit {
  organizations = signal<OrganizationSummary[]>([]);
  loading = signal(true);
  totalElements = signal(0);
  currentPage = signal(0);
  searchQuery = '';
  selectedDomain: string | null = null;

  domains = [
    { value: 'EDUCATION', label: 'Éducation' }, { value: 'SANTE', label: 'Santé' },
    { value: 'ENVIRONNEMENT', label: 'Environnement' }, { value: 'SOCIAL', label: 'Social' },
    { value: 'CULTURE', label: 'Culture' }, { value: 'SPORT', label: 'Sport' },
    { value: 'HUMANITAIRE', label: 'Humanitaire' }, { value: 'DROITS_HUMAINS', label: 'Droits humains' },
    { value: 'AIDE_URGENCE', label: "Aide d'urgence" }, { value: 'AUTRE', label: 'Autre' },
  ];

  constructor(private orgService: OrganizationService) {}

  ngOnInit() { this.loadOrganizations(); }

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
