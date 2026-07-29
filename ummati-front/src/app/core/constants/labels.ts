import { EVENT_TYPES } from './event-types';

/**
 * Libellés lisibles des énumérations du backend.
 *
 * Les valeurs remontent de l'API en majuscules techniques (`MISSION_TERRAIN`,
 * `SANTE`, `PUBLISHED`). Affichées telles quelles, elles donnent une interface qui
 * a l'air inachevée — et « MISSION_TERRAIN » avec son tiret bas ne veut rien dire
 * pour un bénévole. Toutes les correspondances vivent ici pour qu'un nouveau statut
 * n'oblige pas à parcourir dix composants.
 *
 * Les chaînes passent ensuite par le pipe `t` pour la traduction arabe.
 */

type LabelMap = Readonly<Record<string, string>>;

const fromPairs = (pairs: readonly { value: string; label: string }[]): LabelMap =>
  Object.fromEntries(pairs.map(p => [p.value, p.label]));

/** Types de mission — source unique partagée avec les listes déroulantes. */
export const EVENT_TYPE_LABELS: LabelMap = fromPairs(EVENT_TYPES);

/** Domaines d'action des associations. */
export const ORGANIZATION_DOMAIN_LABELS: LabelMap = {
  EDUCATION: 'Éducation',
  SANTE: 'Santé',
  ENVIRONNEMENT: 'Environnement',
  SOCIAL: 'Social',
  CULTURE: 'Culture',
  SPORT: 'Sport',
  HUMANITAIRE: 'Humanitaire',
  DROITS_HUMAINS: 'Droits humains',
  AIDE_URGENCE: "Aide d'urgence",
  AUTRE: 'Autre',
};

/** Cycle de vie d'une mission. */
export const EVENT_STATUS_LABELS: LabelMap = {
  DRAFT: 'Brouillon',
  PUBLISHED: 'Publiée',
  CANCELLED: 'Annulée',
  COMPLETED: 'Terminée',
};

/** Cycle de vie d'un créneau. */
export const OCCURRENCE_STATUS_LABELS: LabelMap = {
  DRAFT: 'Brouillon',
  PUBLISHED: 'Ouvert',
  CANCELLED: 'Annulé',
  COMPLETED: 'Terminé',
};

/** Inscription d'un bénévole à un créneau. */
export const SIGNUP_STATUS_LABELS: LabelMap = {
  REGISTERED: 'Inscrit',
  WAITLISTED: "Liste d'attente",
  CANCELLED: 'Annulée',
  ATTENDED: 'Participation validée',
  NO_SHOW: 'Absence',
};

/** Adhésion à une association. */
export const MEMBERSHIP_STATUS_LABELS: LabelMap = {
  PENDING: 'En attente',
  ACTIVE: 'Membre actif',
  REJECTED: 'Refusée',
  LEFT: 'Départ',
};

export const MEMBERSHIP_ROLE_LABELS: LabelMap = {
  ADMIN: 'Administrateur',
  MEMBER: 'Membre',
  ACCOUNTANT: 'Trésorier',
};

/** Statut d'une association sur la plateforme. */
export const ORGANIZATION_STATUS_LABELS: LabelMap = {
  PENDING: 'En attente de validation',
  ACTIVE: 'Validée',
  REJECTED: 'Refusée',
  SUSPENDED: 'Suspendue',
  ARCHIVED: 'Archivée',
};

/** Traitement d'un signalement. */
export const REPORT_STATUS_LABELS: LabelMap = {
  PENDING: 'À traiter',
  REVIEWED: 'Examiné',
  RESOLVED: 'Résolu',
  DISMISSED: 'Classé sans suite',
};

/** Familles disponibles pour le pipe `label`. */
export const LABEL_SETS = {
  eventType: EVENT_TYPE_LABELS,
  domain: ORGANIZATION_DOMAIN_LABELS,
  eventStatus: EVENT_STATUS_LABELS,
  occurrenceStatus: OCCURRENCE_STATUS_LABELS,
  signupStatus: SIGNUP_STATUS_LABELS,
  membershipStatus: MEMBERSHIP_STATUS_LABELS,
  membershipRole: MEMBERSHIP_ROLE_LABELS,
  organizationStatus: ORGANIZATION_STATUS_LABELS,
  reportStatus: REPORT_STATUS_LABELS,
} as const;

export type LabelSet = keyof typeof LABEL_SETS;

/**
 * Libellé d'une valeur d'énumération.
 *
 * Repli volontairement lisible plutôt que la valeur brute : une énumération ajoutée
 * côté backend et pas encore déclarée ici s'affichera « Mission terrain » et non
 * « MISSION_TERRAIN ».
 */
export function enumLabel(value: string | null | undefined, set: LabelSet): string {
  if (!value) return '';
  const known = LABEL_SETS[set][value];
  if (known) return known;
  const words = value.replace(/_/g, ' ').toLowerCase();
  return words.charAt(0).toUpperCase() + words.slice(1);
}
