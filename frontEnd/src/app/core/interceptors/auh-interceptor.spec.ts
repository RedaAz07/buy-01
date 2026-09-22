import { TestBed } from '@angular/core/testing';
import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { Router } from '@angular/router';
import { MatSnackBar } from '@angular/material/snack-bar';
import { authInterceptor } from './auth-interceptor';
import { ErrorMessage } from '../services/error-message';
import { describe, it, expect, beforeEach, vi, afterEach } from 'vitest';

describe('authInterceptor', () => {
  let http: HttpClient;
  let httpTestingController: HttpTestingController;

  let mockRouter: { navigate: ReturnType<typeof vi.fn> };
  let mockSnackBar: { open: ReturnType<typeof vi.fn> };
  let mockErrorMessage: { getHttpErrorMessage: ReturnType<typeof vi.fn> };

  let getItemSpy: ReturnType<typeof vi.spyOn>;
  let removeItemSpy: ReturnType<typeof vi.spyOn>;

  beforeEach(() => {
    mockRouter = { navigate: vi.fn() };
    mockSnackBar = { open: vi.fn() };
    mockErrorMessage = { getHttpErrorMessage: vi.fn().mockReturnValue('Mocked Error Message') };

    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),

        { provide: Router, useValue: mockRouter },
        { provide: MatSnackBar, useValue: mockSnackBar },
        { provide: ErrorMessage, useValue: mockErrorMessage }
      ]
    });

    http = TestBed.inject(HttpClient);
    httpTestingController = TestBed.inject(HttpTestingController);

    getItemSpy = vi.spyOn(Storage.prototype, 'getItem');
    removeItemSpy = vi.spyOn(Storage.prototype, 'removeItem');
  });

  afterEach(() => {
    httpTestingController.verify();
    vi.clearAllMocks();
  });

  describe('Request Modification', () => {
    it('should add Authorization header if token exists', () => {
      getItemSpy.mockReturnValue('fake-token');

      http.get('/api/data').subscribe();

      const req = httpTestingController.expectOne('/api/data');

      expect(req.request.headers.get('Authorization')).toBe('Bearer fake-token');
      req.flush(null);
    });

    it('should NOT add Authorization header if no token exists', () => {
      getItemSpy.mockReturnValue(null);

      http.get('/api/data').subscribe();

      const req = httpTestingController.expectOne('/api/data');
      expect(req.request.headers.has('Authorization')).toBe(false);
      req.flush(null);
    });
  });

  describe('Error Handling', () => {
    it('should remove token and navigate to login on 401 with existing token', () => {
      getItemSpy.mockReturnValue('fake-token');

      http.get('/api/data').subscribe({
        next: () => expect.fail('Should have failed with 401 error'),
        error: (err) => {
          expect(err.status).toBe(401);
        }
      });

      const req = httpTestingController.expectOne('/api/data');
      req.flush('Unauthorized', { status: 401, statusText: 'Unauthorized' });

      expect(removeItemSpy).toHaveBeenCalledWith('jwt_token');
      expect(mockRouter.navigate).toHaveBeenCalledWith(['/login']);
      expect(mockErrorMessage.getHttpErrorMessage).toHaveBeenCalled();
    });



    it('should show snackbar for other generic errors (e.g., 500)', () => {
      http.get('/api/data').subscribe({
        next: () => expect.fail('Should have failed with 500 error'),
        error: (err) => {
          expect(err.status).toBe(500);
        }
      });

      const req = httpTestingController.expectOne('/api/data');
      req.flush('Server Error', { status: 500, statusText: 'Server Error' });
      expect(mockSnackBar.open).toHaveBeenCalledWith(
        'Mocked Error Message',
        'Close',
        expect.objectContaining({ panelClass: ['error-snackbar'] })
      );
    });
  });
});
