import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideRouter } from '@angular/router';
import { AuthService } from './auth.service';

describe('AuthService', () => {
  let service: AuthService;
  let httpMock: HttpTestingController;

  // logout() redirige vers /login : sans cette route déclarée, la navigation échoue
  // en arrière-plan et Vitest la remonte en erreur non capturée.
  const routes = [{ path: 'login', children: [] }];

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter(routes), AuthService],
    });
    service = TestBed.inject(AuthService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
    localStorage.clear();
  });

  describe('refreshToken', () => {
    it('should issue a single HTTP call when several requests refresh at once', () => {
      localStorage.setItem('refreshToken', 'rt-1');
      const received: string[] = [];

      // Trois requêtes reçoivent un 401 en même temps et demandent un refresh.
      service.refreshToken().subscribe(r => received.push(r.data.accessToken));
      service.refreshToken().subscribe(r => received.push(r.data.accessToken));
      service.refreshToken().subscribe(r => received.push(r.data.accessToken));

      // Sans partage, trois appels partaient et le rate limiter /auth/** finissait
      // par répondre 429, ce que l'intercepteur traduisait en déconnexion.
      const req = httpMock.expectOne(r => r.url.endsWith('/auth/refresh'));
      req.flush({ success: true, data: { accessToken: 'at-2', expiresIn: 900 } });

      expect(received).toEqual(['at-2', 'at-2', 'at-2']);
      expect(localStorage.getItem('accessToken')).toBe('at-2');
    });

    it('should allow a new refresh once the previous one has finished', () => {
      localStorage.setItem('refreshToken', 'rt-1');

      service.refreshToken().subscribe();
      httpMock.expectOne(r => r.url.endsWith('/auth/refresh'))
        .flush({ success: true, data: { accessToken: 'at-2', expiresIn: 900 } });

      service.refreshToken().subscribe();
      const second = httpMock.expectOne(r => r.url.endsWith('/auth/refresh'));
      second.flush({ success: true, data: { accessToken: 'at-3', expiresIn: 900 } });

      expect(localStorage.getItem('accessToken')).toBe('at-3');
    });

    it('should retry after a failed refresh instead of staying locked', () => {
      localStorage.setItem('refreshToken', 'rt-1');

      service.refreshToken().subscribe({ error: () => {} });
      httpMock.expectOne(r => r.url.endsWith('/auth/refresh'))
        .flush({ message: 'nope' }, { status: 401, statusText: 'Unauthorized' });

      // Le verrou doit avoir été relâché : un nouvel essai part bien.
      service.refreshToken().subscribe({ error: () => {} });
      httpMock.expectOne(r => r.url.endsWith('/auth/refresh'))
        .flush({ message: 'nope' }, { status: 401, statusText: 'Unauthorized' });
    });

    it('should send the refresh token as bearer', () => {
      localStorage.setItem('refreshToken', 'rt-42');

      service.refreshToken().subscribe();

      const req = httpMock.expectOne(r => r.url.endsWith('/auth/refresh'));
      expect(req.request.headers.get('Authorization')).toBe('Bearer rt-42');
      req.flush({ success: true, data: { accessToken: 'at', expiresIn: 900 } });
    });

    /**
     * Les sessions natives tournent : le serveur consomme le jeton présenté et en
     * renvoie un autre. Ne pas l'enregistrer ferait rejouer un jeton mort au refresh
     * suivant — le backend y voit une session volée, révoque la lignée entière et
     * déconnecte l'utilisateur sans raison visible.
     */
    it('should store the rotated refresh token when the server returns one', () => {
      localStorage.setItem('refreshToken', 'rt-1');

      service.refreshToken().subscribe();

      httpMock.expectOne(r => r.url.endsWith('/auth/refresh')).flush({
        success: true,
        data: { accessToken: 'at-2', expiresIn: 900, refreshToken: 'rt-2' },
      });

      expect(localStorage.getItem('refreshToken')).toBe('rt-2');
    });

    /** Le web n'a pas de rotation : son jeton doit rester valable jusqu'à son terme. */
    it('should keep the existing refresh token when the server rotates nothing', () => {
      localStorage.setItem('refreshToken', 'rt-1');

      service.refreshToken().subscribe();

      httpMock.expectOne(r => r.url.endsWith('/auth/refresh')).flush({
        success: true,
        data: { accessToken: 'at-2', expiresIn: 900, refreshToken: null },
      });

      expect(localStorage.getItem('refreshToken')).toBe('rt-1');
    });
  });

  describe('logout', () => {
    it('should hand the refresh token to the server so the session is revoked', () => {
      localStorage.setItem('refreshToken', 'rt-7');

      service.logout();

      const req = httpMock.expectOne(r => r.url.endsWith('/auth/logout'));
      expect(req.request.headers.get('X-Refresh-Token')).toBe('rt-7');
      req.flush(null);

      expect(localStorage.getItem('refreshToken')).toBeNull();
      expect(service.isLoggedIn()).toBe(false);
    });
  });

  describe('session state', () => {
    /** Recrée un TestBed pour que le constructeur relise le localStorage courant. */
    function serviceWithStoredUser(raw: string): AuthService {
      TestBed.resetTestingModule();
      localStorage.setItem('user', raw);
      TestBed.configureTestingModule({
        providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter(routes), AuthService],
      });
      return TestBed.inject(AuthService);
    }

    it('should expose the stored user on startup', () => {
      const fresh = serviceWithStoredUser(JSON.stringify({
        id: 'u1', email: 'a@b.c', firstName: 'Amina', lastName: 'B',
        role: 'VOLUNTEER', onboardingDone: true,
      }));

      expect(fresh.user()?.firstName).toBe('Amina');
      expect(fresh.isLoggedIn()).toBe(true);
    });

    it('should ignore a corrupted stored user instead of crashing', () => {
      const fresh = serviceWithStoredUser('{not json');

      expect(fresh.user()).toBeNull();
      expect(fresh.isLoggedIn()).toBe(false);
    });
  });
});
