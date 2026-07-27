import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { EventService } from './event.service';

describe('EventService', () => {
  let service: EventService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), EventService],
    });
    service = TestBed.inject(EventService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('listEvents should call GET /events with params', () => {
    service.listEvents({ page: 0, size: 10, type: 'FORMATION', city: 'Paris' }).subscribe();
    const req = httpMock.expectOne(r => r.url.includes('/events') && r.method === 'GET');
    expect(req.request.params.get('type')).toBe('FORMATION');
    expect(req.request.params.get('city')).toBe('Paris');
    expect(req.request.params.get('page')).toBe('0');
    req.flush({ success: true, data: { content: [], totalElements: 0 } });
  });

  it('listEvents should not set undefined params', () => {
    service.listEvents({ page: 0, size: 10 }).subscribe();
    const req = httpMock.expectOne(r => r.url.includes('/events'));
    expect(req.request.params.has('type')).toBe(false);
    expect(req.request.params.has('city')).toBe(false);
    req.flush({ success: true, data: { content: [], totalElements: 0 } });
  });

  it('listEvents should pass the full-text query', () => {
    service.listEvents({ q: 'maraude' }).subscribe();
    const req = httpMock.expectOne(r => r.url.includes('/events'));
    expect(req.request.params.get('q')).toBe('maraude');
    req.flush({ success: true, data: { content: [], totalElements: 0 } });
  });

  it('listEvents should pass geolocation params for a "near me" search', () => {
    service.listEvents({ lat: 48.8566, lng: 2.3522, radiusKm: 25, sort: 'distance' }).subscribe();
    const req = httpMock.expectOne(r => r.url.includes('/events'));
    expect(req.request.params.get('lat')).toBe('48.8566');
    expect(req.request.params.get('lng')).toBe('2.3522');
    expect(req.request.params.get('radiusKm')).toBe('25');
    expect(req.request.params.get('sort')).toBe('distance');
    req.flush({ success: true, data: { content: [], totalElements: 0 } });
  });

  it('listEvents should keep a zero longitude, not drop it as falsy', () => {
    // lng = 0 (méridien de Greenwich) est une valeur légitime : un test de vérité
    // simple l'écarterait et décalerait silencieusement la recherche.
    service.listEvents({ lat: 51.4778, lng: 0 }).subscribe();
    const req = httpMock.expectOne(r => r.url.includes('/events'));
    expect(req.request.params.get('lng')).toBe('0');
    req.flush({ success: true, data: { content: [], totalElements: 0 } });
  });

  it('getEvent should call GET /events/:id', () => {
    service.getEvent('abc-123').subscribe();
    const req = httpMock.expectOne(r => r.url.endsWith('/events/abc-123'));
    expect(req.request.method).toBe('GET');
    req.flush({ success: true, data: {} });
  });

  it('createEvent should call POST /organizations/:orgId/events', () => {
    const body = { title: 'Test', description: 'Desc', type: 'MARAUDE' };
    service.createEvent('org-1', body).subscribe();
    const req = httpMock.expectOne(r => r.url.includes('/organizations/org-1/events'));
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(body);
    req.flush({ success: true, data: {} });
  });

  it('signup should call POST /events/:id/signups', () => {
    service.signup('evt-1').subscribe();
    const req = httpMock.expectOne(r => r.url.endsWith('/events/evt-1/signups'));
    expect(req.request.method).toBe('POST');
    req.flush({ success: true, data: { status: 'REGISTERED' } });
  });

  it('cancelSignup should call DELETE /events/:id/signups', () => {
    service.cancelSignup('evt-1').subscribe();
    const req = httpMock.expectOne(r => r.url.endsWith('/events/evt-1/signups'));
    expect(req.request.method).toBe('DELETE');
    req.flush(null);
  });

  it('changeStatus should call PATCH /events/:id/status', () => {
    service.changeStatus('evt-1', { status: 'PUBLISH' }).subscribe();
    const req = httpMock.expectOne(r => r.url.endsWith('/events/evt-1/status'));
    expect(req.request.method).toBe('PATCH');
    expect(req.request.body.status).toBe('PUBLISH');
    req.flush({ success: true, data: {} });
  });

  it('markAttendance should send userIds', () => {
    service.markAttendance('evt-1', ['u1', 'u2']).subscribe();
    const req = httpMock.expectOne(r => r.url.includes('/attendance'));
    expect(req.request.body.userIds).toEqual(['u1', 'u2']);
    req.flush(null);
  });

  it('createFeedback should call POST /events/:id/feedbacks', () => {
    const fb = { rating: 4, comment: 'Nice', anonymous: false };
    service.createFeedback('evt-1', fb).subscribe();
    const req = httpMock.expectOne(r => r.url.endsWith('/events/evt-1/feedbacks'));
    expect(req.request.method).toBe('POST');
    expect(req.request.body.rating).toBe(4);
    req.flush({ success: true, data: {} });
  });

  it('signupToOccurrence should call POST /events/:id/occurrences/:occId/signups', () => {
    service.signupToOccurrence('evt-1', 'occ-9').subscribe();
    const req = httpMock.expectOne(r => r.url.endsWith('/events/evt-1/occurrences/occ-9/signups'));
    expect(req.request.method).toBe('POST');
    req.flush({ success: true, data: { status: 'REGISTERED' } });
  });

  it('cancelOccurrenceSignup should call DELETE /events/:id/occurrences/:occId/signups', () => {
    service.cancelOccurrenceSignup('evt-1', 'occ-9').subscribe();
    const req = httpMock.expectOne(r => r.url.endsWith('/events/evt-1/occurrences/occ-9/signups'));
    expect(req.request.method).toBe('DELETE');
    req.flush(null);
  });

  it('changeOccurrenceStatus should call PATCH /events/:id/occurrences/:occId/status', () => {
    service.changeOccurrenceStatus('evt-1', 'occ-9', { status: 'CANCEL', reason: 'Créneau annulé' }).subscribe();
    const req = httpMock.expectOne(r => r.url.endsWith('/events/evt-1/occurrences/occ-9/status'));
    expect(req.request.method).toBe('PATCH');
    expect(req.request.body.status).toBe('CANCEL');
    req.flush({ success: true, data: {} });
  });
});

