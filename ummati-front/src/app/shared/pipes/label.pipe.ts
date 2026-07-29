import { Pipe, PipeTransform, inject } from '@angular/core';
import { I18nService } from '../../core/services/i18n.service';
import { LabelSet, enumLabel } from '../../core/constants/labels';

/**
 * Affiche le libellé lisible d'une énumération backend :
 *
 *   {{ event.type | label: 'eventType' }}      → « Maraude »
 *   {{ org.domain | label: 'domain' }}         → « Santé »
 *   {{ signup.status | label: 'signupStatus' }} → « Liste d'attente »
 *
 * Le libellé passe par la traduction, donc il suit la langue active.
 */
@Pipe({ name: 'label', standalone: true })
export class LabelPipe implements PipeTransform {
  private i18n = inject(I18nService);

  transform(value: string | null | undefined, set: LabelSet): string {
    const label = enumLabel(value, set);
    return label ? this.i18n.t(label) : '';
  }
}
