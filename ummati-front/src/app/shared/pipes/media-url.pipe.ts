import { Pipe, PipeTransform } from '@angular/core';
import { environment } from '../../../environments/environment';

/**
 * Résout l'adresse d'un média servi par le backend :
 *
 *   <img [src]="org.logoUrl | mediaUrl" />
 *
 * Le backend renvoie des chemins absolus de site (« /uploads/images/… »). Sur le web
 * ils se résolvent contre le domaine courant et le pipe ne fait rien. Dans l'app
 * installée, la page est servie depuis capacitor://localhost : sans réécriture, ces
 * chemins pointeraient vers le conteneur natif et toutes les images seraient
 * cassées.
 *
 * <p>Seuls les chemins commençant par « / » sont réécrits. Les aperçus locaux
 * (blob:, data:) et les URL déjà absolues traversent inchangés — plusieurs écrans
 * alimentent la même liaison tantôt avec un fichier choisi par l'utilisateur, tantôt
 * avec l'image déjà enregistrée.
 */
@Pipe({ name: 'mediaUrl', standalone: true })
export class MediaUrlPipe implements PipeTransform {
  transform(value: string | null | undefined): string | null {
    if (!value) return null;
    if (!value.startsWith('/')) return value;
    return environment.mediaBaseUrl + value;
  }
}
