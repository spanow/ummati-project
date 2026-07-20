import { Component, inject, Input, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatTableModule } from '@angular/material/table';
import { MatTooltipModule } from '@angular/material/tooltip';
import { DocumentItem, DocumentService } from '../../../core/services/document.service';
import { EmptyStateComponent } from '../../../shared/components/empty-state/empty-state.component';
import { environment } from '../../../../environments/environment';
import { TPipe } from '../../../shared/pipes/t.pipe';

@Component({
  selector: 'app-org-documents',
  standalone: true,
  imports: [
    CommonModule, MatCardModule, MatButtonModule, MatIconModule,
    MatProgressSpinnerModule, MatSnackBarModule, MatTableModule,
    MatTooltipModule, EmptyStateComponent, TPipe
  ],
  template: `
    <mat-card class="docs-card">
      <mat-card-header>
        <mat-icon mat-card-avatar aria-hidden="true">folder</mat-icon>
        <mat-card-title>{{ 'Documents' | t }}</mat-card-title>
        <mat-card-subtitle>{{ (eventId ? 'Documents de l\\'événement (PDF, JPG, PNG, DOCX — max 10 Mo)' : 'Fichiers partagés de l\\'organisation (PDF, JPG, PNG, DOCX — max 10 Mo)') | t }}</mat-card-subtitle>
        <span class="header-spacer"></span>
        @if (canUpload) {
          <button mat-flat-button color="primary" (click)="fileInput.click()"
                  [disabled]="uploading()" [attr.aria-label]="'Ajouter un document' | t">
            @if (uploading()) { <mat-spinner diameter="18"></mat-spinner> }
            @else { <mat-icon>upload</mat-icon> }
            {{ 'Ajouter' | t }}
          </button>
          <input #fileInput type="file" hidden accept=".pdf,.jpg,.jpeg,.png,.docx"
                 (change)="onFileSelected($event)" aria-label="Sélectionner un fichier">
        }
      </mat-card-header>

      <mat-card-content>
        @if (loading()) {
          <div class="loading-center" aria-live="polite" aria-busy="true">
            <mat-spinner diameter="40" aria-label="Chargement des documents..."></mat-spinner>
          </div>
        } @else if (documents().length === 0) {
          <app-empty-state
            icon="description"
            [title]="'Aucun document' | t"
            [description]="(canUpload ? 'Ajoutez des documents pour les partager avec les membres.' : 'Aucun document disponible pour cette organisation.') | t"
            [ctaLabel]="canUpload ? ('Ajouter un document' | t) : undefined"
            [ctaIcon]="'upload'"
            [ctaClick]="canUpload ? onAddClick : undefined">
          </app-empty-state>
        } @else {
          <table mat-table [dataSource]="documents()" aria-label="Liste des documents" class="docs-table">
            <!-- Nom -->
            <ng-container matColumnDef="name">
              <th mat-header-cell *matHeaderCellDef scope="col">{{ 'Nom' | t }}</th>
              <td mat-cell *matCellDef="let doc">
                <div class="doc-name">
                  <mat-icon aria-hidden="true">{{ getFileIcon(doc.fileType) }}</mat-icon>
                  <span>{{ doc.name }}</span>
                </div>
              </td>
            </ng-container>
            <!-- Type -->
            <ng-container matColumnDef="type">
              <th mat-header-cell *matHeaderCellDef scope="col">{{ 'Type' | t }}</th>
              <td mat-cell *matCellDef="let doc">
                <span class="type-badge">{{ getTypeLabel(doc.fileType) }}</span>
              </td>
            </ng-container>
            <!-- Taille -->
            <ng-container matColumnDef="size">
              <th mat-header-cell *matHeaderCellDef scope="col">{{ 'Taille' | t }}</th>
              <td mat-cell *matCellDef="let doc">{{ formatSize(doc.fileSize) }}</td>
            </ng-container>
            <!-- Date -->
            <ng-container matColumnDef="date">
              <th mat-header-cell *matHeaderCellDef scope="col">{{ 'Ajouté le' | t }}</th>
              <td mat-cell *matCellDef="let doc">{{ doc.createdAt | date:'dd/MM/yyyy' }}</td>
            </ng-container>
            <!-- Actions -->
            <ng-container matColumnDef="actions">
              <th mat-header-cell *matHeaderCellDef scope="col">{{ 'Actions' | t }}</th>
              <td mat-cell *matCellDef="let doc">
                <a mat-icon-button [href]="getDownloadUrl(doc.id)" target="_blank"
                   [matTooltip]="('Télécharger' | t) + ' ' + doc.name" [attr.aria-label]="('Télécharger' | t) + ' ' + doc.name">
                  <mat-icon>download</mat-icon>
                </a>
                @if (canUpload) {
                  <button mat-icon-button color="warn" (click)="deleteDoc(doc)"
                          [matTooltip]="('Supprimer' | t) + ' ' + doc.name" [attr.aria-label]="('Supprimer' | t) + ' ' + doc.name">
                    <mat-icon>delete</mat-icon>
                  </button>
                }
              </td>
            </ng-container>

            <tr mat-header-row *matHeaderRowDef="columns"></tr>
            <tr mat-row *matRowDef="let row; columns: columns;"></tr>
          </table>
        }
      </mat-card-content>
    </mat-card>
  `,
  styles: [`
    .docs-card { border-radius: 12px !important; }
    mat-card-header { align-items: center; flex-wrap: wrap; gap: 8px; }
    .header-spacer { flex: 1; }
    .loading-center { display: flex; justify-content: center; padding: 40px; }
    .docs-table { width: 100%; }
    .doc-name { display: flex; align-items: center; gap: 8px; }
    .doc-name mat-icon { color: var(--brand-primary); font-size: 20px !important; width: 20px !important; height: 20px !important; }
    .type-badge {
      background: var(--brand-primary-100); color: var(--brand-primary); padding: 2px 8px;
      border-radius: 10px; font-size: 0.75rem; font-weight: 600;
    }
    mat-spinner { display: inline-block; }
  `]
})
export class OrgDocumentsComponent implements OnInit {
  /** Propriétaire : organisation (orgId) OU événement (eventId). eventId a la priorité. */
  @Input() orgId?: string;
  @Input() eventId?: string;
  @Input() canUpload = false;

  private docService = inject(DocumentService);
  private snackBar = inject(MatSnackBar);

  documents = signal<DocumentItem[]>([]);
  loading = signal(true);
  uploading = signal(false);

  columns = ['name', 'type', 'size', 'date', 'actions'];

  onAddClick = () => {};

  ngOnInit() { this.loadDocs(); }

  loadDocs() {
    this.loading.set(true);
    const src$ = this.eventId
      ? this.docService.listByEvent(this.eventId)
      : this.docService.listByOrg(this.orgId!);
    src$.subscribe({
      next: (res) => { this.documents.set(res.data ?? []); this.loading.set(false); },
      error: () => this.loading.set(false)
    });
  }

  onFileSelected(event: Event) {
    const file = (event.target as HTMLInputElement).files?.[0];
    if (!file) return;
    if (file.size > 10 * 1024 * 1024) {
      this.snackBar.open('Le fichier ne doit pas dépasser 10 Mo', 'OK', { duration: 4000 });
      return;
    }
    this.uploading.set(true);
    const up$ = this.eventId
      ? this.docService.uploadToEvent(this.eventId, file)
      : this.docService.upload(this.orgId!, file);
    up$.subscribe({
      next: (res) => {
        this.documents.update(docs => [res.data, ...docs]);
        this.snackBar.open('Document ajouté avec succès', 'OK', { duration: 3000 });
        this.uploading.set(false);
      },
      error: (err) => {
        this.snackBar.open(err.error?.message ?? 'Erreur lors de l\'upload', 'OK', { duration: 5000 });
        this.uploading.set(false);
      }
    });
  }

  deleteDoc(doc: DocumentItem) {
    if (!confirm(`Supprimer le document "${doc.name}" ?`)) return;
    this.docService.delete(doc.id).subscribe({
      next: () => {
        this.documents.update(docs => docs.filter(d => d.id !== doc.id));
        this.snackBar.open('Document supprimé', 'OK', { duration: 3000 });
      },
      error: () => this.snackBar.open('Erreur lors de la suppression', 'OK', { duration: 4000 })
    });
  }

  getDownloadUrl(docId: string) { return this.docService.downloadUrl(docId); }

  getFileIcon(type: string): string {
    if (type?.includes('pdf')) return 'picture_as_pdf';
    if (type?.includes('image')) return 'image';
    if (type?.includes('word') || type?.includes('docx')) return 'description';
    return 'attach_file';
  }

  getTypeLabel(type: string): string {
    if (type?.includes('pdf')) return 'PDF';
    if (type?.includes('jpeg') || type?.includes('jpg')) return 'JPG';
    if (type?.includes('png')) return 'PNG';
    if (type?.includes('word') || type?.includes('docx')) return 'DOCX';
    return type?.split('/').pop()?.toUpperCase() ?? 'Fichier';
  }

  formatSize(bytes: number): string {
    if (bytes < 1024) return bytes + ' o';
    if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' Ko';
    return (bytes / (1024 * 1024)).toFixed(1) + ' Mo';
  }
}

