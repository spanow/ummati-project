import { MediaUrlPipe } from './media-url.pipe';
import { environment } from '../../../environments/environment';

/**
 * Ce pipe traverse toutes les images de l'application. Ce qui est vérifié ici est
 * autant son utilité sur mobile que son innocuité sur le web : avec un mediaBaseUrl
 * vide, il doit rendre exactement ce qu'on lui donne.
 */
describe('MediaUrlPipe', () => {
  let pipe: MediaUrlPipe;
  const original = environment.mediaBaseUrl;

  beforeEach(() => {
    pipe = new MediaUrlPipe();
    environment.mediaBaseUrl = original;
  });

  afterEach(() => {
    environment.mediaBaseUrl = original;
  });

  it('laisse les chemins intacts sur le web', () => {
    environment.mediaBaseUrl = '';
    expect(pipe.transform('/uploads/images/org/logo.jpg')).toBe('/uploads/images/org/logo.jpg');
  });

  it("préfixe les chemins du backend dans l'app installée", () => {
    environment.mediaBaseUrl = 'https://ummati.example.org';
    expect(pipe.transform('/uploads/images/org/logo.jpg'))
      .toBe('https://ummati.example.org/uploads/images/org/logo.jpg');
  });

  /** Plusieurs écrans alimentent la même liaison avec un fichier choisi localement. */
  it('ne touche pas aux aperçus locaux', () => {
    environment.mediaBaseUrl = 'https://ummati.example.org';
    expect(pipe.transform('blob:http://localhost/abc')).toBe('blob:http://localhost/abc');
    expect(pipe.transform('data:image/png;base64,AAAA')).toBe('data:image/png;base64,AAAA');
  });

  it('ne double pas les URL déjà absolues', () => {
    environment.mediaBaseUrl = 'https://ummati.example.org';
    expect(pipe.transform('https://cdn.example.org/a.jpg')).toBe('https://cdn.example.org/a.jpg');
  });

  it('rend null pour une valeur absente', () => {
    expect(pipe.transform(null)).toBeNull();
    expect(pipe.transform(undefined)).toBeNull();
    expect(pipe.transform('')).toBeNull();
  });
});
