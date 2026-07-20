import { Pipe, PipeTransform, inject } from '@angular/core';
import { I18nService } from '../../core/services/i18n.service';

/**
 * Pipe de traduction : {{ 'Texte français' | t }}.
 * Pure — le changement de langue recharge la page (voir I18nService).
 */
@Pipe({ name: 't', standalone: true })
export class TPipe implements PipeTransform {
  private i18n = inject(I18nService);

  transform(key: string): string {
    return this.i18n.t(key);
  }
}
