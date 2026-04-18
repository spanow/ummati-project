import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { NotificationService } from './notification.service';

describe('NotificationService', () => {
  let service: NotificationService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), NotificationService],
    });
    service = TestBed.inject(NotificationService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('list() should call GET /notifications with pagination params', () => {
    service.list(0, 20).subscribe();
    const req = httpMock.expectOne(r => r.url.includes('/notifications') && !r.url.includes('unread'));
    expect(req.request.method).toBe('GET');
    expect(req.request.params.get('page')).toBe('0');
    expect(req.request.params.get('size')).toBe('20');
    req.flush({ success: true, data: { content: [], totalElements: 0 } });
  });

  it('unreadCount() should call GET /notifications/unread-count', () => {
    service.unreadCount().subscribe();
    const req = httpMock.expectOne(r => r.url.includes('unread-count'));
    expect(req.request.method).toBe('GET');
    req.flush({ success: true, data: { count: 3 } });
  });

  it('markAsRead() should call PATCH /notifications/:id/read', () => {
    service.markAsRead('notif-1').subscribe();
    const req = httpMock.expectOne(r => r.url.endsWith('/notif-1/read'));
    expect(req.request.method).toBe('PATCH');
    req.flush(null);
  });

  it('markAllAsRead() should call PATCH /notifications/read-all', () => {
    service.markAllAsRead().subscribe();
    const req = httpMock.expectOne(r => r.url.endsWith('/read-all'));
    expect(req.request.method).toBe('PATCH');
    req.flush(null);
  });
});

