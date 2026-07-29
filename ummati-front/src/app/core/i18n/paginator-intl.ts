import { Injectable, inject } from '@angular/core';
import { MatPaginatorIntl } from '@angular/material/paginator';
import { I18nService } from '../services/i18n.service';

/**
 * Étiquettes du paginateur Material.
 *
 * Par défaut Angular Material affiche « Items per page », « 1 – 6 of 6 » et
 * « Next page » : de l'anglais brut au milieu d'une interface française, sur
 * toutes les listes de l'application. Les chaînes passent par le service de
 * traduction, donc elles suivent aussi le basculement en arabe.
 */
@Injectable()
export class UmmatiPaginatorIntl extends MatPaginatorIntl {
  private i18n = inject(I18nService);

  constructor() {
    super();
    this.itemsPerPageLabel = this.i18n.t('Par page');
    this.nextPageLabel = this.i18n.t('Page suivante');
    this.previousPageLabel = this.i18n.t('Page précédente');
    this.firstPageLabel = this.i18n.t('Première page');
    this.lastPageLabel = this.i18n.t('Dernière page');
  }

  override getRangeLabel = (page: number, pageSize: number, length: number): string => {
    if (length === 0 || pageSize === 0) {
      return this.i18n.t('Aucun résultat');
    }
    const total = Math.max(length, 0);
    const start = page * pageSize;
    // La dernière page est rarement pleine : on borne la fin au total réel.
    const end = Math.min(start + pageSize, total);
    return `${start + 1} – ${end} ${this.i18n.t('sur')} ${total}`;
  };
}
