import { Component, signal, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTabsModule } from '@angular/material/tabs';
import { MatTableModule } from '@angular/material/table';
import { MatChipsModule } from '@angular/material/chips';
import { MatMenuModule } from '@angular/material/menu';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatDialogModule, MatDialog } from '@angular/material/dialog';
import { MatBadgeModule } from '@angular/material/badge';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatSelectModule } from '@angular/material/select';
import { DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { MembershipService, MembershipResponse, MembershipQuestion, MembershipAnswer } from '../../../core/services/membership.service';
import { OrgAnnouncementService, OrgAnnouncementResponse } from '../../../core/services/org-announcement.service';
import { OrganizationService, OrganizationDetail } from '../../../core/services/organization.service';
import { OrgDocumentsComponent } from '../org-documents/org-documents.component';
import { TPipe } from '../../../shared/pipes/t.pipe';
import { LocationPickerComponent } from '../../../shared/components/location-picker/location-picker.component';
import { GeoResult } from '../../../core/services/geocoding.service';

@Component({
  selector: 'app-organization-manage',
  standalone: true,
  imports: [
    MatCardModule, MatButtonModule, MatIconModule, MatTabsModule, MatTableModule,
    MatChipsModule, MatMenuModule, MatProgressSpinnerModule, MatSnackBarModule,
    MatDialogModule, MatBadgeModule, MatFormFieldModule, MatInputModule, MatCheckboxModule,
    MatSelectModule, DatePipe, RouterLink, FormsModule, OrgDocumentsComponent, TPipe,
    LocationPickerComponent,
  ],
  template: `
    <div class="page-container">
      <header class="page-header">
        <div>
          <h1>{{ 'Gestion de l\\'organisation' | t }}</h1>
          <p class="subtitle">{{ orgSlug }}</p>
        </div>
        <div class="header-actions">
          <a mat-flat-button color="primary"
             [routerLink]="['/organizations', orgId, 'events', 'new']">
            <mat-icon>add</mat-icon> {{ 'Créer un événement' | t }}
          </a>
          <a mat-stroked-button [routerLink]="['/organizations', orgId, 'events', 'manage']">
            <mat-icon>event</mat-icon> {{ 'Événements' | t }}
          </a>
        </div>
      </header>

      <mat-tab-group>
        <!-- Pending tab -->
        <mat-tab>
          <ng-template matTabLabel>
            <mat-icon>pending</mat-icon>
            {{ 'En attente' | t }}
            @if (pendingMembers().length > 0) {
              <span class="badge">{{ pendingMembers().length }}</span>
            }
          </ng-template>

          @if (loading()) {
            <div class="loading"><mat-spinner diameter="36" /></div>
          } @else if (pendingMembers().length === 0) {
            <div class="empty-state">
              <mat-icon>check_circle</mat-icon>
              <p>{{ 'Aucune demande en attente' | t }}</p>
            </div>
          } @else {
            <div class="member-list">
              @for (m of pendingMembers(); track m.id) {
                <div class="member-block">
                  <div class="member-row">
                    <div class="member-avatar">
                      @if (m.photoUrl) {
                        <img [src]="m.photoUrl" class="avatar-img" />
                      } @else {
                        <div class="avatar-placeholder">{{ m.firstName[0] }}{{ m.lastName[0] }}</div>
                      }
                    </div>
                    <div class="member-info">
                      <strong>{{ m.firstName }} {{ m.lastName }}</strong>
                      <span class="motivation">{{ m.motivation || ('Aucun message de motivation' | t) }}</span>
                      <span class="date">{{ 'Demande reçue le' | t }} {{ m.createdAt | date:'dd/MM/yyyy' }}</span>
                    </div>
                    <div class="member-actions">
                      <button mat-button (click)="toggleAnswers(m)">
                        <mat-icon>{{ expandedMember() === m.id ? 'expand_less' : 'quiz' }}</mat-icon>
                        {{ 'Réponses' | t }}
                      </button>
                      <button mat-flat-button color="primary" (click)="approve(m)">
                        <mat-icon>check</mat-icon> {{ 'Accepter' | t }}
                      </button>
                      <button mat-stroked-button (click)="reject(m)">
                        <mat-icon>close</mat-icon> {{ 'Refuser' | t }}
                      </button>
                    </div>
                  </div>
                  @if (expandedMember() === m.id) {
                    <div class="answers-panel">
                      @if (answersByMembership()[m.id]?.length) {
                        @for (a of answersByMembership()[m.id]; track a.questionId) {
                          <div class="answer-item">
                            <span class="a-q">{{ a.questionLabel }}</span>
                            <span class="a-v">{{ a.value }}</span>
                          </div>
                        }
                      } @else {
                        <p class="no-answers">{{ 'Aucune réponse au questionnaire.' | t }}</p>
                      }
                    </div>
                  }
                </div>
              }
            </div>
          }
        </mat-tab>

        <!-- Active members tab -->
        <mat-tab>
          <ng-template matTabLabel>
            <mat-icon>group</mat-icon>
            {{ 'Membres actifs' | t }} ({{ activeMembers().length }})
          </ng-template>

          @if (loading()) {
            <div class="loading"><mat-spinner diameter="36" /></div>
          } @else if (activeMembers().length === 0) {
            <div class="empty-state">
              <mat-icon>group_off</mat-icon>
              <p>{{ 'Aucun membre actif' | t }}</p>
            </div>
          } @else {
            <div class="member-list">
              @for (m of activeMembers(); track m.id) {
                <div class="member-row">
                  <div class="member-avatar">
                    @if (m.photoUrl) {
                      <img [src]="m.photoUrl" class="avatar-img" />
                    } @else {
                      <div class="avatar-placeholder">{{ m.firstName[0] }}{{ m.lastName[0] }}</div>
                    }
                  </div>
                  <div class="member-info">
                    <strong>{{ m.firstName }} {{ m.lastName }}</strong>
                    <div class="role-row">
                      <mat-chip [class]="'role-' + m.role.toLowerCase()">{{ roleLabel(m.role) | t }}</mat-chip>
                      <span class="date">{{ 'Membre depuis' | t }} {{ m.joinedAt | date:'dd/MM/yyyy' }}</span>
                    </div>
                  </div>
                  <button mat-icon-button [matMenuTriggerFor]="memberMenu">
                    <mat-icon>more_vert</mat-icon>
                  </button>
                  <mat-menu #memberMenu="matMenu">
                    @if (m.role !== 'ADMIN') {
                      <button mat-menu-item (click)="changeRole(m, 'ADMIN')">
                        <mat-icon>admin_panel_settings</mat-icon> {{ 'Promouvoir admin' | t }}
                      </button>
                    }
                    @if (m.role === 'ADMIN') {
                      <button mat-menu-item (click)="changeRole(m, 'MEMBER')">
                        <mat-icon>person</mat-icon> {{ 'Rétrograder membre' | t }}
                      </button>
                    }
                    <button mat-menu-item class="danger-item" (click)="exclude(m)">
                      <mat-icon>person_remove</mat-icon> {{ 'Exclure' | t }}
                    </button>
                  </mat-menu>
                </div>
              }
            </div>
          }
        </mat-tab>

        <!-- Membership questionnaire tab -->
        <mat-tab>
          <ng-template matTabLabel>
            <mat-icon>quiz</mat-icon>
            {{ 'Questionnaire' | t }}
          </ng-template>

          <div class="tab-section">
            <div class="announce-form">
              <h3>{{ 'Nouvelle question d\\'adhésion' | t }}</h3>
              <mat-form-field appearance="outline" class="full-width">
                <mat-label>{{ 'Question' | t }}</mat-label>
                <input matInput [(ngModel)]="newQ.label" name="qlabel" maxlength="500"
                       [placeholder]="'Ex : Pourquoi souhaitez-vous nous rejoindre ?' | t" />
              </mat-form-field>
              <div class="form-row">
                <mat-form-field appearance="outline" class="flex-1">
                  <mat-label>{{ 'Type' | t }}</mat-label>
                  <mat-select [(ngModel)]="newQ.type" name="qtype">
                    <mat-option value="TEXT">{{ 'Texte libre' | t }}</mat-option>
                    <mat-option value="BOOLEAN">{{ 'Oui / Non' | t }}</mat-option>
                    <mat-option value="SINGLE_CHOICE">{{ 'Choix unique' | t }}</mat-option>
                  </mat-select>
                </mat-form-field>
                <mat-checkbox [(ngModel)]="newQ.required" name="qreq" class="req-check">{{ 'Obligatoire' | t }}</mat-checkbox>
              </div>
              @if (newQ.type === 'SINGLE_CHOICE') {
                <mat-form-field appearance="outline" class="full-width">
                  <mat-label>{{ 'Options (une par ligne)' | t }}</mat-label>
                  <textarea matInput [(ngModel)]="newQ.options" name="qopts" rows="3"></textarea>
                </mat-form-field>
              }
              <button mat-flat-button color="primary" (click)="addQuestion()" [disabled]="!newQ.label.trim()">
                <mat-icon>add</mat-icon> {{ 'Ajouter la question' | t }}
              </button>
            </div>

            @if (questions().length === 0) {
              <div class="empty-state">
                <mat-icon>quiz</mat-icon>
                <p>{{ 'Aucune question — les candidats ne renseignent que la motivation.' | t }}</p>
              </div>
            } @else {
              <div class="q-list">
                @for (q of questions(); track q.id) {
                  <div class="q-row">
                    <div class="q-main">
                      <strong>{{ q.label }}</strong>
                      <div class="q-meta">
                        <mat-chip>{{ qTypeLabel(q.type) | t }}</mat-chip>
                        @if (q.required) { <span class="q-req">{{ 'Obligatoire' | t }}</span> }
                        @if (q.options.length) { <span class="q-opts">{{ q.options.join(' · ') }}</span> }
                      </div>
                    </div>
                    <button mat-icon-button color="warn" (click)="deleteQuestion(q)" [title]="'Supprimer' | t">
                      <mat-icon>delete</mat-icon>
                    </button>
                  </div>
                }
              </div>
            }
          </div>
        </mat-tab>

        <!-- Announcements tab -->
        <mat-tab>
          <ng-template matTabLabel>
            <mat-icon>campaign</mat-icon>
            {{ 'Annonces' | t }} ({{ orgAnnouncements().length }})
          </ng-template>

          <div class="tab-section">
            <div class="announce-form">
              <h3>{{ 'Nouvelle annonce' | t }}</h3>
              <mat-form-field appearance="outline" class="full-width">
                <mat-label>{{ 'Titre' | t }}</mat-label>
                <input matInput [(ngModel)]="newTitle" [placeholder]="'Réunion mensuelle…' | t" maxlength="200" />
              </mat-form-field>
              <mat-form-field appearance="outline" class="full-width">
                <mat-label>{{ 'Contenu' | t }}</mat-label>
                <textarea matInput [(ngModel)]="newContent" rows="4"
                          [placeholder]="'Message pour tous les membres…' | t" maxlength="2000"></textarea>
              </mat-form-field>
              <div class="announce-footer">
                <mat-checkbox [(ngModel)]="newPinned">{{ 'Épingler cette annonce' | t }}</mat-checkbox>
                <button mat-flat-button color="primary" [disabled]="posting() || !newTitle.trim() || !newContent.trim()"
                        (click)="postAnnouncement()">
                  <mat-icon>send</mat-icon> {{ (posting() ? 'Envoi…' : 'Publier') | t }}
                </button>
              </div>
            </div>

            @if (loadingAnnouncements()) {
              <div class="loading"><mat-spinner diameter="28" /></div>
            } @else if (orgAnnouncements().length === 0) {
              <div class="empty-state">
                <mat-icon>campaign</mat-icon>
                <p>{{ 'Aucune annonce publiée' | t }}</p>
              </div>
            } @else {
              <div class="announce-list">
                @for (a of orgAnnouncements(); track a.id) {
                  <div class="announce-card" [class.pinned]="a.pinned">
                    @if (a.pinned) {
                      <span class="pin-badge"><mat-icon>push_pin</mat-icon> {{ 'Épinglé' | t }}</span>
                    }
                    <div class="announce-header">
                      <strong>{{ a.title }}</strong>
                      <button mat-icon-button class="delete-btn" (click)="deleteAnnouncement(a)">
                        <mat-icon>delete</mat-icon>
                      </button>
                    </div>
                    <p class="announce-body">{{ a.content }}</p>
                    <span class="announce-date">{{ a.createdAt | date:'dd/MM/yyyy HH:mm' }}</span>
                  </div>
                }
              </div>
            }
          </div>
        </mat-tab>

        <!-- Informations tab -->
        <mat-tab>
          <ng-template matTabLabel>
            <mat-icon>edit</mat-icon>
            {{ 'Informations' | t }}
          </ng-template>

          <div class="tab-section">
            @if (!org()) {
              <div class="loading"><mat-spinner diameter="28" /></div>
            } @else {
              <form class="org-edit-form" (ngSubmit)="saveOrgInfo()">
                <mat-form-field appearance="outline" class="full-width">
                  <mat-label>{{ 'Description' | t }}</mat-label>
                  <textarea matInput [(ngModel)]="editModel.description" name="description" rows="4" maxlength="5000"></textarea>
                </mat-form-field>
                <mat-form-field appearance="outline" class="full-width">
                  <mat-label>{{ 'Mission' | t }}</mat-label>
                  <textarea matInput [(ngModel)]="editModel.mission" name="mission" rows="3" maxlength="5000"></textarea>
                </mat-form-field>
                <mat-form-field appearance="outline">
                  <mat-label>{{ 'Domaine' | t }}</mat-label>
                  <mat-select [(ngModel)]="editModel.domain" name="domain">
                    @for (d of domains; track d) {
                      <mat-option [value]="d">{{ d }}</mat-option>
                    }
                  </mat-select>
                </mat-form-field>
                <div class="form-row">
                  <mat-form-field appearance="outline" class="flex-2">
                    <mat-label>{{ 'Adresse' | t }}</mat-label>
                    <input matInput [(ngModel)]="editModel.addressStreet" name="addressStreet" />
                  </mat-form-field>
                </div>
                <div class="form-row">
                  <mat-form-field appearance="outline" class="flex-2">
                    <mat-label>{{ 'Ville' | t }}</mat-label>
                    <input matInput [(ngModel)]="editModel.addressCity" name="addressCity" />
                  </mat-form-field>
                  <mat-form-field appearance="outline" class="flex-1">
                    <mat-label>{{ 'Code postal' | t }}</mat-label>
                    <input matInput [(ngModel)]="editModel.addressZip" name="addressZip" />
                  </mat-form-field>
                </div>
                <div class="map-block">
                  <label class="map-label">{{ 'Localiser le bureau sur la carte' | t }}</label>
                  <app-location-picker
                    [lat]="editModel.addressLat" [lng]="editModel.addressLng"
                    (coordsChange)="onCoords($event)"
                    (addressResolved)="onAddressResolved($event)" />
                </div>
                <div class="form-row">
                  <mat-form-field appearance="outline" class="flex-1">
                    <mat-label>{{ 'Email de contact' | t }}</mat-label>
                    <input matInput type="email" [(ngModel)]="editModel.email" name="email" />
                  </mat-form-field>
                  <mat-form-field appearance="outline" class="flex-1">
                    <mat-label>{{ 'Téléphone' | t }}</mat-label>
                    <input matInput [(ngModel)]="editModel.phone" name="phone" />
                  </mat-form-field>
                </div>
                <mat-form-field appearance="outline" class="full-width">
                  <mat-label>{{ 'Site web' | t }}</mat-label>
                  <input matInput [(ngModel)]="editModel.website" name="website" placeholder="https://…" />
                </mat-form-field>
                <button mat-flat-button color="primary" type="submit" [disabled]="savingInfo()">
                  {{ (savingInfo() ? 'Sauvegarde…' : 'Sauvegarder les modifications') | t }}
                </button>
              </form>
            }
          </div>
        </mat-tab>

        <!-- Documents tab -->
        <mat-tab>
          <ng-template matTabLabel>
            <mat-icon>folder</mat-icon>
            {{ 'Documents' | t }}
          </ng-template>

          <div class="tab-section">
            @if (orgId) {
              <app-org-documents [orgId]="orgId" [canUpload]="true" />
            }
          </div>
        </mat-tab>
      </mat-tab-group>
    </div>
  `,
  styles: [`
    .page-container { max-width: 900px; margin: 0 auto; padding: 32px 24px; }
    .page-header { margin-bottom: 24px; display: flex; justify-content: space-between; align-items: center; }
    .page-header h1 { font-size: 1.8rem; font-weight: 600; margin: 0; }
    .header-actions { display: flex; gap: 8px; }
    .subtitle { color: #666; margin-top: 4px; }
    .loading { display: flex; justify-content: center; padding: 48px; }
    .empty-state { text-align: center; padding: 48px 24px; color: #999; }
    .empty-state mat-icon { font-size: 48px; width: 48px; height: 48px; display: block; margin: 0 auto 12px; }
    .member-list { padding: 16px 0; display: flex; flex-direction: column; gap: 12px; }
    .member-row { display: flex; align-items: center; gap: 16px; padding: 16px 20px;
      background: #fafafa; border-radius: 10px; border: 1px solid #f0f0f0;
      transition: box-shadow 0.2s; }
    .member-row:hover { box-shadow: 0 2px 12px rgba(0,0,0,0.06); }
    .member-avatar { flex-shrink: 0; }
    .avatar-img { width: 44px; height: 44px; border-radius: 50%; object-fit: cover; }
    .avatar-placeholder { width: 44px; height: 44px; border-radius: 50%;
      background: var(--brand-primary-100); color: var(--brand-primary); display: flex; align-items: center;
      justify-content: center; font-weight: 600; font-size: 0.9rem; }
    .member-info { flex: 1; display: flex; flex-direction: column; gap: 4px; }
    .member-info strong { font-size: 0.95rem; }
    .motivation { font-size: 0.85rem; color: #666; font-style: italic; }
    .date { font-size: 0.8rem; color: #aaa; }
    .role-row { display: flex; align-items: center; gap: 12px; }
    .member-actions { display: flex; gap: 8px; flex-shrink: 0; }
    .badge { background: #e53935; color: white; border-radius: 10px; padding: 1px 7px;
      font-size: 11px; margin-left: 6px; }
    .role-admin { --mdc-chip-label-text-color: var(--brand-primary-dark); background: var(--brand-primary-100); }
    .role-member { --mdc-chip-label-text-color: #2e7d32; background: #e8f5e9; }
    .role-accountant { --mdc-chip-label-text-color: #f57f17; background: #fff8e1; }
    .danger-item { color: #d32f2f; }
    .tab-section { padding: 24px 0; }
    .announce-form { background: #fafafa; border: 1px solid #eeeeee; border-radius: 12px;
      padding: 20px 24px; margin-bottom: 28px; }
    .announce-form h3 { margin: 0 0 16px; font-size: 1rem; font-weight: 600; color: #333; }
    .full-width { width: 100%; }
    .announce-footer { display: flex; align-items: center; justify-content: space-between; margin-top: 8px; }
    .announce-list { display: flex; flex-direction: column; gap: 14px; }
    .announce-card { padding: 18px 20px; border-radius: 10px; border: 1px solid #e0e0e0; background: white; }
    .announce-card.pinned { border-left: 4px solid var(--brand-primary); background: var(--brand-primary-soft); }
    .pin-badge { display: inline-flex; align-items: center; gap: 4px; font-size: 0.75rem; color: var(--brand-primary); font-weight: 600; margin-bottom: 6px; }
    .pin-badge mat-icon { font-size: 14px; width: 14px; height: 14px; }
    .announce-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 8px; }
    .announce-header strong { font-size: 0.95rem; }
    .delete-btn { color: #d32f2f; }
    .announce-body { margin: 0 0 10px; color: #444; line-height: 1.6; white-space: pre-line; font-size: 0.9rem; }
    .announce-date { font-size: 0.78rem; color: #aaa; }
    .org-edit-form { display: flex; flex-direction: column; gap: 4px; max-width: 640px; padding: 16px 0; }
    .map-block { margin: 8px 0; }
    .map-label { display: block; font-size: 0.9rem; font-weight: 600; color: #333; margin-bottom: 8px; }
    .form-row { display: flex; gap: 16px; }
    .flex-1 { flex: 1; }
    .flex-2 { flex: 2; }
    .full-width { width: 100%; }
    .org-edit-form button { align-self: flex-start; margin-top: 8px; }
    .member-block { display: flex; flex-direction: column; }
    .answers-panel { background: white; border: 1px solid #f0f0f0; border-top: none;
      border-radius: 0 0 10px 10px; padding: 10px 20px 14px; margin: -6px 0 0; }
    .answer-item { display: flex; flex-direction: column; padding: 6px 0; border-bottom: 1px solid #f5f5f5; }
    .answer-item:last-child { border-bottom: none; }
    .a-q { font-size: 0.78rem; color: #888; }
    .a-v { font-size: 0.9rem; color: #333; }
    .no-answers { color: #aaa; font-style: italic; margin: 4px 0; font-size: 0.85rem; }
    .req-check { align-self: center; }
    .q-list { display: flex; flex-direction: column; gap: 10px; }
    .q-row { display: flex; align-items: center; justify-content: space-between; gap: 12px;
      padding: 14px 18px; background: #fafafa; border: 1px solid #f0f0f0; border-radius: 10px; }
    .q-main strong { font-size: 0.95rem; }
    .q-meta { display: flex; align-items: center; gap: 10px; margin-top: 6px; font-size: 0.8rem; color: #888; flex-wrap: wrap; }
    .q-req { color: #d32f2f; font-weight: 600; }
  `],
})
export class OrganizationManageComponent implements OnInit {
  orgSlug = '';
  orgId = '';
  pendingMembers = signal<MembershipResponse[]>([]);
  activeMembers = signal<MembershipResponse[]>([]);
  loading = signal(true);
  orgAnnouncements = signal<OrgAnnouncementResponse[]>([]);
  loadingAnnouncements = signal(false);
  posting = signal(false);
  newTitle = '';
  newContent = '';
  newPinned = false;
  org = signal<OrganizationDetail | null>(null);
  savingInfo = signal(false);
  questions = signal<MembershipQuestion[]>([]);
  answersByMembership = signal<Record<string, MembershipAnswer[]>>({});
  expandedMember = signal<string | null>(null);
  newQ = { label: '', type: 'TEXT', options: '', required: false };
  domains = ['EDUCATION', 'SANTE', 'ENVIRONNEMENT', 'SOCIAL', 'CULTURE', 'SPORT',
    'HUMANITAIRE', 'DROITS_HUMAINS', 'AIDE_URGENCE', 'AUTRE'];
  editModel: {
    description: string; mission: string; domain: string; addressStreet: string;
    addressCity: string; addressZip: string;
    addressLat: number | null; addressLng: number | null;
    email: string; phone: string; website: string;
  } = {
    description: '', mission: '', domain: '', addressStreet: '',
    addressCity: '', addressZip: '', addressLat: null, addressLng: null,
    email: '', phone: '', website: '',
  };

  constructor(
    private route: ActivatedRoute,
    private membershipService: MembershipService,
    private announcementService: OrgAnnouncementService,
    private orgService: OrganizationService,
    private snackBar: MatSnackBar,
  ) {}

  ngOnInit() {
    this.orgSlug = this.route.snapshot.paramMap.get('slug') ?? '';
    this.orgId = this.route.snapshot.queryParamMap.get('orgId') ?? '';
    this.orgService.getBySlug(this.orgSlug).subscribe({
      next: res => {
        this.org.set(res.data);
        this.editModel = {
          description: res.data.description ?? '',
          mission: res.data.mission ?? '',
          domain: res.data.domain ?? '',
          addressStreet: res.data.addressStreet ?? '',
          addressCity: res.data.addressCity ?? '',
          addressZip: res.data.addressZip ?? '',
          addressLat: res.data.addressLat ?? null,
          addressLng: res.data.addressLng ?? null,
          email: res.data.email ?? '',
          phone: res.data.phone ?? '',
          website: res.data.website ?? '',
        };
        // Fallback si le queryParam orgId est absent (accès direct par URL)
        if (!this.orgId) {
          this.orgId = res.data.id;
          this.loadMembers();
          this.loadAnnouncements();
          this.loadQuestions();
        }
      },
      error: () => {},
    });
    if (this.orgId) {
      this.loadMembers();
      this.loadAnnouncements();
      this.loadQuestions();
    }
  }

  onCoords(c: { lat: number; lng: number }) {
    this.editModel.addressLat = c.lat;
    this.editModel.addressLng = c.lng;
  }

  onAddressResolved(r: GeoResult) {
    if (r.city && !this.editModel.addressCity) this.editModel.addressCity = r.city;
    if (r.zip && !this.editModel.addressZip) this.editModel.addressZip = r.zip;
    if (r.street && !this.editModel.addressStreet) this.editModel.addressStreet = r.street;
  }

  saveOrgInfo() {
    this.savingInfo.set(true);
    this.orgService.update(this.orgId, this.editModel).subscribe({
      next: res => {
        this.org.set(res.data);
        this.savingInfo.set(false);
        this.snackBar.open('Informations de l\'organisation mises à jour !', '', { duration: 3000 });
      },
      error: err => {
        this.savingInfo.set(false);
        this.snackBar.open(err.error?.message ?? 'Erreur lors de la sauvegarde', '', { duration: 4000 });
      },
    });
  }

  loadMembers() {
    this.loading.set(true);
    this.membershipService.listMembers(this.orgId, undefined, 0, 100).subscribe({
      next: res => {
        const all = res.data.content;
        this.pendingMembers.set(all.filter(m => m.status === 'PENDING'));
        this.activeMembers.set(all.filter(m => m.status === 'ACTIVE'));
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  loadAnnouncements() {
    this.loadingAnnouncements.set(true);
    this.announcementService.list(this.orgId).subscribe({
      next: res => {
        this.orgAnnouncements.set(res.data);
        this.loadingAnnouncements.set(false);
      },
      error: () => this.loadingAnnouncements.set(false),
    });
  }

  postAnnouncement() {
    if (!this.newTitle.trim() || !this.newContent.trim()) return;
    this.posting.set(true);
    this.announcementService.create(this.orgId, {
      title: this.newTitle.trim(),
      content: this.newContent.trim(),
      pinned: this.newPinned,
    }).subscribe({
      next: () => {
        this.snackBar.open('Annonce publiée !', '', { duration: 3000 });
        this.newTitle = '';
        this.newContent = '';
        this.newPinned = false;
        this.posting.set(false);
        this.loadAnnouncements();
      },
      error: err => {
        this.posting.set(false);
        this.snackBar.open(err.error?.message ?? 'Erreur lors de la publication', '', { duration: 4000 });
      },
    });
  }

  deleteAnnouncement(a: OrgAnnouncementResponse) {
    if (!confirm(`Supprimer l'annonce "${a.title}" ?`)) return;
    this.announcementService.delete(this.orgId, a.id).subscribe({
      next: () => {
        this.snackBar.open('Annonce supprimée.', '', { duration: 3000 });
        this.orgAnnouncements.update(list => list.filter(x => x.id !== a.id));
      },
      error: err => this.snackBar.open(err.error?.message ?? 'Erreur', '', { duration: 4000 }),
    });
  }

  approve(m: MembershipResponse) {
    this.membershipService.approve(m.id).subscribe({
      next: () => { this.snackBar.open(`${m.firstName} accepté(e) !`, '', { duration: 3000 }); this.loadMembers(); },
      error: err => this.snackBar.open(err.error?.message ?? 'Erreur', '', { duration: 4000 }),
    });
  }

  reject(m: MembershipResponse) {
    this.membershipService.reject(m.id).subscribe({
      next: () => { this.snackBar.open(`Demande refusée.`, '', { duration: 3000 }); this.loadMembers(); },
      error: err => this.snackBar.open(err.error?.message ?? 'Erreur', '', { duration: 4000 }),
    });
  }

  changeRole(m: MembershipResponse, role: string) {
    this.membershipService.changeRole(m.id, role).subscribe({
      next: () => { this.snackBar.open('Rôle mis à jour.', '', { duration: 3000 }); this.loadMembers(); },
      error: err => this.snackBar.open(err.error?.message ?? 'Erreur', '', { duration: 4000 }),
    });
  }

  exclude(m: MembershipResponse) {
    if (!confirm(`Exclure ${m.firstName} ${m.lastName} ?`)) return;
    this.membershipService.remove(m.id).subscribe({
      next: () => { this.snackBar.open('Membre exclu.', '', { duration: 3000 }); this.loadMembers(); },
      error: err => this.snackBar.open(err.error?.message ?? 'Erreur', '', { duration: 4000 }),
    });
  }

  roleLabel(role: string): string {
    return { ADMIN: 'Admin', MEMBER: 'Membre', ACCOUNTANT: 'Comptable' }[role] ?? role;
  }

  // --- Questionnaire d'adhésion ---

  loadQuestions() {
    this.membershipService.listQuestions(this.orgId).subscribe({
      next: res => this.questions.set(res.data ?? []),
      error: () => {},
    });
  }

  addQuestion() {
    const label = this.newQ.label.trim();
    if (!label) return;
    const options = this.newQ.type === 'SINGLE_CHOICE'
      ? this.newQ.options.split('\n').map(s => s.trim()).filter(Boolean)
      : undefined;
    this.membershipService.createQuestion(this.orgId, {
      label, type: this.newQ.type, options, required: this.newQ.required,
    }).subscribe({
      next: res => {
        this.questions.update(list => [...list, res.data]);
        this.newQ = { label: '', type: 'TEXT', options: '', required: false };
        this.snackBar.open('Question ajoutée.', '', { duration: 2500 });
      },
      error: err => this.snackBar.open(err.error?.message ?? 'Erreur', '', { duration: 4000 }),
    });
  }

  deleteQuestion(q: MembershipQuestion) {
    if (!confirm(`Supprimer la question "${q.label}" ?`)) return;
    this.membershipService.deleteQuestion(q.id).subscribe({
      next: () => this.questions.update(list => list.filter(x => x.id !== q.id)),
      error: err => this.snackBar.open(err.error?.message ?? 'Erreur', '', { duration: 4000 }),
    });
  }

  toggleAnswers(m: MembershipResponse) {
    if (this.expandedMember() === m.id) { this.expandedMember.set(null); return; }
    this.expandedMember.set(m.id);
    if (!this.answersByMembership()[m.id]) {
      this.membershipService.getAnswers(m.id).subscribe({
        next: res => this.answersByMembership.update(a => ({ ...a, [m.id]: res.data ?? [] })),
        error: () => {},
      });
    }
  }

  qTypeLabel(type: string): string {
    return { TEXT: 'Texte libre', BOOLEAN: 'Oui / Non', SINGLE_CHOICE: 'Choix unique' }[type] ?? type;
  }
}
