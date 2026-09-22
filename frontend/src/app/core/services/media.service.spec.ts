import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { describe, it, expect, beforeEach, afterEach } from 'vitest';

import { Media } from './media';
import { environment } from '../../../environments/environment';

describe('Media Service', () => {
  let service: Media;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        Media,
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    });

    service = TestBed.inject(Media);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  describe('setAvatar()', () => {
    it('should send a POST request with FormData containing files and default type', () => {
      const mockFile = new File(['content'], 'avatar.png', { type: 'image/png' });
      const mockResponse = ['https://cloudinary.com/avatar.png'];

      service.setAvatar([mockFile]).subscribe((res) => {
        expect(res).toEqual(mockResponse);
      });

      const req = httpMock.expectOne(`${environment.apiUrl}/api/media/images`);
      expect(req.request.method).toBe('POST');

      // Verify body is FormData and check fields
      const formData = req.request.body as FormData;
      expect(formData.get('type')).toBe('AVATAR');
      expect(formData.get('media')).toBeTruthy();

      req.flush(mockResponse);
    });

    it('should allow custom media type parameter', () => {
      const mockFile = new File(['content'], 'banner.png', { type: 'image/png' });

      service.setAvatar([mockFile], 'BANNER').subscribe();

      const req = httpMock.expectOne(`${environment.apiUrl}/api/media/images`);
      const formData = req.request.body as FormData;
      expect(formData.get('type')).toBe('BANNER');

      req.flush([]);
    });
  });

  describe('deleteAvatar()', () => {
    it('should send a DELETE request with image ID in path', () => {
      const imageId = 'img_12345';

      service.deleteAvatar(imageId).subscribe();

      const req = httpMock.expectOne(`${environment.apiUrl}/api/media/images/${imageId}`);
      expect(req.request.method).toBe('DELETE');

      req.flush(null);
    });
  });
});
