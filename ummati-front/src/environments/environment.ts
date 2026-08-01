export const environment = {
  production: false,
  apiUrl: '/api/v1',
  // Vide sur le web : les chemins de médias renvoyés par le backend se résolvent
  // contre l'origine courante. Seule l'app installée doit les préfixer, la page y
  // étant servie depuis capacitor://localhost (cf. MediaUrlPipe).
  mediaBaseUrl: '',
  appName: 'Ummati',
  native: false,
};

