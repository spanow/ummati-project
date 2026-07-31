import { TestBed } from '@angular/core/testing';
import { TokenStorageService } from './token-storage.service';
import { PlatformService } from './platform.service';

/**
 * Le stockage est le point où web et natif divergent. Ces cas vérifient surtout que
 * le chemin web n'a pas bougé : c'est lui qui porte les sessions existantes.
 */
describe('TokenStorageService', () => {
  function serviceOn(isNative: boolean): TokenStorageService {
    TestBed.resetTestingModule();
    TestBed.configureTestingModule({
      providers: [
        TokenStorageService,
        { provide: PlatformService, useValue: { isNative, platform: isNative ? 'IOS' : 'WEB' } },
      ],
    });
    return TestBed.inject(TokenStorageService);
  }

  beforeEach(() => localStorage.clear());

  describe('sur le web', () => {
    it('écrit et relit par localStorage, comme avant', () => {
      const storage = serviceOn(false);

      storage.write('accessToken', 'abc');

      expect(localStorage.getItem('accessToken')).toBe('abc');
      expect(storage.read('accessToken')).toBe('abc');
    });

    it('supprime réellement la clé', () => {
      const storage = serviceOn(false);
      storage.write('refreshToken', 'xyz');

      storage.remove('refreshToken');

      expect(localStorage.getItem('refreshToken')).toBeNull();
      expect(storage.read('refreshToken')).toBeNull();
    });

    it('rend null pour une clé absente', () => {
      expect(serviceOn(false).read('inconnu')).toBeNull();
    });
  });

  describe('en natif', () => {
    /**
     * Le cache mémoire est ce qui permet de garder une API synchrone au-dessus d'un
     * stockage natif asynchrone, sans rendre asynchrone tout l'intercepteur HTTP.
     */
    it('sert les lectures depuis le cache mémoire', () => {
      const storage = serviceOn(true);

      storage.write('accessToken', 'natif-123');

      expect(storage.read('accessToken')).toBe('natif-123');
    });

    it("n'écrit pas dans localStorage, hors de portée du JavaScript de la webview", () => {
      const storage = serviceOn(true);

      storage.write('refreshToken', 'secret-90-jours');

      expect(localStorage.getItem('refreshToken')).toBeNull();
    });

    it('oublie la valeur après suppression', () => {
      const storage = serviceOn(true);
      storage.write('user', '{"id":"1"}');

      storage.remove('user');

      expect(storage.read('user')).toBeNull();
    });
  });
});
