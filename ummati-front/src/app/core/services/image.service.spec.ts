import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { ImageService } from './image.service';

describe('ImageService', () => {
  let service: ImageService;
  let httpMock: HttpTestingController;

  const jpeg = () => new File([new Uint8Array([0xff, 0xd8, 0xff])], 'photo.jpg', { type: 'image/jpeg' });

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), ImageService],
    });
    service = TestBed.inject(ImageService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  describe('validate', () => {
    it('should accept a JPEG under the size limit', () => {
      expect(ImageService.validate(jpeg())).toBeNull();
    });

    it('should reject an unsupported type', () => {
      const gif = new File(['x'], 'anim.gif', { type: 'image/gif' });
      expect(ImageService.validate(gif)).toContain('JPG, PNG et WebP');
    });

    it('should reject a file over 5 MB', () => {
      const big = new File([new Uint8Array(5 * 1024 * 1024 + 1)], 'big.jpg', { type: 'image/jpeg' });
      expect(ImageService.validate(big)).toContain('5 Mo');
    });
  });

  it('uploadOrgLogo should POST multipart to /organizations/:id/logo', () => {
    service.uploadOrgLogo('org-1', jpeg()).subscribe();
    const req = httpMock.expectOne(r => r.url.endsWith('/organizations/org-1/logo'));
    expect(req.request.method).toBe('POST');
    expect(req.request.body instanceof FormData).toBe(true);
    expect((req.request.body as FormData).get('file')).toBeTruthy();
    req.flush({ success: true, data: { logoUrl: '/uploads/images/organizations/org-1/a.jpg' } });
  });

  it('uploadEventCover should POST to /events/:id/cover', () => {
    service.uploadEventCover('evt-1', jpeg()).subscribe();
    const req = httpMock.expectOne(r => r.url.endsWith('/events/evt-1/cover'));
    expect(req.request.method).toBe('POST');
    req.flush({ success: true, data: { coverUrl: '/uploads/images/events/evt-1/a.jpg' } });
  });

  it('addEventPhoto should include the caption when provided', () => {
    service.addEventPhoto('evt-1', jpeg(), 'Distribution du samedi').subscribe();
    const req = httpMock.expectOne(r => r.url.endsWith('/events/evt-1/photos'));
    expect((req.request.body as FormData).get('caption')).toBe('Distribution du samedi');
    req.flush({ success: true, data: {} });
  });

  it('addEventPhoto should omit the caption when absent', () => {
    service.addEventPhoto('evt-1', jpeg()).subscribe();
    const req = httpMock.expectOne(r => r.url.endsWith('/events/evt-1/photos'));
    expect((req.request.body as FormData).has('caption')).toBe(false);
    req.flush({ success: true, data: {} });
  });

  it('listEventPhotos should GET the public gallery', () => {
    service.listEventPhotos('evt-1').subscribe();
    const req = httpMock.expectOne(r => r.url.endsWith('/events/evt-1/photos'));
    expect(req.request.method).toBe('GET');
    req.flush({ success: true, data: [] });
  });

  it('deleteEventPhoto should DELETE /events/photos/:photoId', () => {
    service.deleteEventPhoto('photo-9').subscribe();
    const req = httpMock.expectOne(r => r.url.endsWith('/events/photos/photo-9'));
    expect(req.request.method).toBe('DELETE');
    req.flush(null);
  });
});
