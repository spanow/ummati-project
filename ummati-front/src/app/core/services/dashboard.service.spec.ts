import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { DashboardService } from './dashboard.service';
import { AdminService } from './admin.service';

describe('DashboardService', () => {
  let service: DashboardService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), DashboardService],
    });
    service = TestBed.inject(DashboardService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('should be created', () => expect(service).toBeTruthy());

  it('getVolunteerDashboard() should call GET /dashboard/volunteer', () => {
    service.getVolunteerDashboard().subscribe();
    const req = httpMock.expectOne(r => r.url.endsWith('/dashboard/volunteer'));
    expect(req.request.method).toBe('GET');
    req.flush({ success: true, data: {} });
  });

  it('getOrgAdminDashboard() should call GET /dashboard/org-admin/:id', () => {
    service.getOrgAdminDashboard('org-1').subscribe();
    const req = httpMock.expectOne(r => r.url.endsWith('/dashboard/org-admin/org-1'));
    expect(req.request.method).toBe('GET');
    req.flush({ success: true, data: {} });
  });
});

describe('AdminService', () => {
  let service: AdminService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), AdminService],
    });
    service = TestBed.inject(AdminService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('should be created', () => expect(service).toBeTruthy());

  it('getStats() should call GET /admin/stats', () => {
    service.getStats().subscribe();
    const req = httpMock.expectOne(r => r.url.endsWith('/admin/stats'));
    expect(req.request.method).toBe('GET');
    req.flush({ success: true, data: {} });
  });

  it('listUsers() should call GET /admin/users with search param', () => {
    service.listUsers('test', undefined, 0).subscribe();
    const req = httpMock.expectOne(r => r.url.includes('/admin/users'));
    expect(req.request.params.get('search')).toBe('test');
    req.flush({ success: true, data: { content: [], totalElements: 0 } });
  });

  it('changeUserStatus() should call PATCH /admin/users/:id/status', () => {
    service.changeUserStatus('user-1', false).subscribe();
    const req = httpMock.expectOne(r => r.url.includes('/admin/users/user-1/status'));
    expect(req.request.method).toBe('PATCH');
    expect(req.request.body.enabled).toBe(false);
    req.flush({ success: true, data: {} });
  });

  it('listOrganizations() should filter by status', () => {
    service.listOrganizations('PENDING', 0).subscribe();
    const req = httpMock.expectOne(r => r.url.includes('/admin/organizations'));
    expect(req.request.params.get('status')).toBe('PENDING');
    req.flush({ success: true, data: { content: [], totalElements: 0 } });
  });
});
