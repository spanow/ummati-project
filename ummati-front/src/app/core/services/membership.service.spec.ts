import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { MembershipService } from './membership.service';

describe('MembershipService', () => {
  let service: MembershipService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), MembershipService],
    });
    service = TestBed.inject(MembershipService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('getMyMembership() should call GET /organizations/:orgId/memberships/me', () => {
    const orgId = 'org-uuid-123';
    service.getMyMembership(orgId).subscribe(res => {
      expect(res.data.status).toBe('ACTIVE');
      expect(res.data.role).toBe('ADMIN');
    });

    const req = httpMock.expectOne(r => r.url.endsWith(`/organizations/${orgId}/memberships/me`));
    expect(req.request.method).toBe('GET');
    req.flush({ success: true, data: { id: 'mem-1', status: 'ACTIVE', role: 'ADMIN' } });
  });

  it('requestMembership() should call POST /organizations/:orgId/memberships with motivation', () => {
    const orgId = 'org-uuid-123';
    service.requestMembership(orgId, 'Je souhaite contribuer').subscribe();

    const req = httpMock.expectOne(r =>
      r.url.endsWith(`/organizations/${orgId}/memberships`) && r.method === 'POST'
    );
    expect(req.request.body).toEqual({ motivation: 'Je souhaite contribuer' });
    req.flush({ success: true, data: { id: 'mem-1', status: 'PENDING' } });
  });

  it('requestMembership() without motivation should still POST', () => {
    const orgId = 'org-uuid-123';
    service.requestMembership(orgId).subscribe();

    const req = httpMock.expectOne(r =>
      r.url.endsWith(`/organizations/${orgId}/memberships`) && r.method === 'POST'
    );
    expect(req.request.body).toEqual({ motivation: undefined });
    req.flush({ success: true, data: { id: 'mem-1', status: 'PENDING' } });
  });

  it('approve() should call PATCH /memberships/:id with action APPROVE', () => {
    service.approve('mem-1').subscribe();

    const req = httpMock.expectOne(r => r.url.endsWith('/memberships/mem-1'));
    expect(req.request.method).toBe('PATCH');
    expect(req.request.body).toEqual({ action: 'APPROVE' });
    req.flush({ success: true, data: { id: 'mem-1', status: 'ACTIVE' } });
  });

  it('reject() should call PATCH /memberships/:id with action REJECT', () => {
    service.reject('mem-1').subscribe();

    const req = httpMock.expectOne(r => r.url.endsWith('/memberships/mem-1'));
    expect(req.request.method).toBe('PATCH');
    expect(req.request.body).toEqual({ action: 'REJECT' });
    req.flush({ success: true, data: { id: 'mem-1', status: 'REJECTED' } });
  });
});
