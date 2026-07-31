/**
 * Environnement des applications installées (iOS / Android).
 *
 * <p>La différence essentielle avec le web tient en un point : l'app n'est pas servie
 * par le backend. La page vient du conteneur natif (capacitor://localhost sur iOS,
 * http://localhost sur Android), donc toute adresse relative pointerait vers ce
 * conteneur et non vers l'API. Les URL doivent être absolues.
 *
 * ⚠️ À renseigner avant toute compilation destinée aux stores : l'app embarque cette
 * valeur, elle ne peut pas être changée après coup sans republier une version.
 * Voir MOBILE.md.
 */
const API_HOST = 'https://ummati.example.org';

export const environment = {
  production: true,
  apiUrl: `${API_HOST}/api/v1`,
  mediaBaseUrl: API_HOST,
  appName: 'Ummati',
  native: true,
};
