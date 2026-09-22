import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter, Router } from '@angular/router';
import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';

import { Auth } from './auth';
import { environment } from '../../../environments/environment';
import { UserProfileDTO } from '../models/user';
import { AuthResponseDTO } from '../models/auth';
import { TestBed } from '@angular/core/testing';

describe('Auth Service', () => {
  let service: Auth;
  let httpMock: HttpTestingController;
  let router: Router;

  let getItemSpy: ReturnType<typeof vi.spyOn>;
  let setItemSpy: ReturnType<typeof vi.spyOn>;
  let removeItemSpy: ReturnType<typeof vi.spyOn>;

  const mockUser: UserProfileDTO = {
    id: '123',
    name: 'Reda',
    email: 'reda@test.com',
    role: 'ROLE_SELLER',
    avatar: 'avatar-url'
  } as UserProfileDTO;

  beforeEach(() => {
    getItemSpy = vi.spyOn(Storage.prototype, 'getItem');
    setItemSpy = vi.spyOn(Storage.prototype, 'setItem');
    removeItemSpy = vi.spyOn(Storage.prototype, 'removeItem');

    TestBed.configureTestingModule({
      providers: [
        Auth,
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
      ],
    });

    router = TestBed.inject(Router);
    vi.spyOn(router, 'navigate').mockImplementation(() => Promise.resolve(true));
  });

  afterEach(() => {
    httpMock?.verify();
    vi.clearAllMocks();
  });

  describe('Constructor Initialization', () => {
    it('should NOT attempt to fetch user if no token exists in localStorage', () => {
      getItemSpy.mockReturnValue(null);

      service = TestBed.inject(Auth);
      httpMock = TestBed.inject(HttpTestingController);

      service.isLoggedIn$.subscribe((isLogged) => {
        expect(isLogged).toBe(false);
      });

      httpMock.expectNone(`${environment.apiUrl}/api/users/me`);
    });

    it('should auto-load current user if token exists in localStorage', () => {
      getItemSpy.mockReturnValue('existing-jwt-token');

      service = TestBed.inject(Auth);
      httpMock = TestBed.inject(HttpTestingController);

      const req = httpMock.expectOne(`${environment.apiUrl}/api/users/me`);
      expect(req.request.method).toBe('GET');
      req.flush(mockUser);

      service.isLoggedIn$.subscribe((isLogged) => {
        expect(isLogged).toBe(true);
      });

      service.currentUser$.subscribe((user) => {
        expect(user).toEqual(mockUser);
      });
    });
  });

  describe('login()', () => {
    beforeEach(() => {
      getItemSpy.mockReturnValue(null);
      service = TestBed.inject(Auth);
      httpMock = TestBed.inject(HttpTestingController);
    });

    it('should post login credentials, store JWT, and load current user on success', () => {
      const loginPayload = { name: 'Reda', password: 'password123' };
      const authResponse: AuthResponseDTO = { jwt: 'new-jwt-token' };

      service.login(loginPayload).subscribe((res) => {
        expect(res).toEqual(authResponse);
      });

      const loginReq = httpMock.expectOne(`${environment.apiUrl}/api/auth/login`);
      expect(loginReq.request.method).toBe('POST');
      expect(loginReq.request.body).toEqual(loginPayload);
      loginReq.flush(authResponse);

      const userReq = httpMock.expectOne(`${environment.apiUrl}/api/users/me`);
      expect(userReq.request.method).toBe('GET');
      userReq.flush(mockUser);

      expect(setItemSpy).toHaveBeenCalledWith('jwt_token', 'new-jwt-token');
      service.currentUser$.subscribe((user) => {
        expect(user).toEqual(mockUser);
      });
    });
  });

  describe('logout()', () => {
    beforeEach(() => {
      getItemSpy.mockReturnValue(null);
      service = TestBed.inject(Auth);
      httpMock = TestBed.inject(HttpTestingController);
    });

    it('should remove token, clear state, and navigate to /login', () => {
      service.logout();

      expect(removeItemSpy).toHaveBeenCalledWith('jwt_token');

      service.isLoggedIn$.subscribe((isLogged) => {
        expect(isLogged).toBe(false);
      });

      service.currentUser$.subscribe((user) => {
        expect(user).toBeNull();
      });

      expect(router.navigate).toHaveBeenCalledWith(['/login']);
    });
  });

  describe('loadCurrentUser()', () => {
    beforeEach(() => {
      getItemSpy.mockReturnValue(null);
      service = TestBed.inject(Auth);
      httpMock = TestBed.inject(HttpTestingController);
    });

    it('should fetch user from API and update currentUserSubject', () => {
      service.loadCurrentUser().subscribe((user) => {
        expect(user).toEqual(mockUser);
      });

      const req = httpMock.expectOne(`${environment.apiUrl}/api/users/me`);
      expect(req.request.method).toBe('GET');
      req.flush(mockUser);

      service.currentUser$.subscribe((user) => {
        expect(user).toEqual(mockUser);
      });
    });
  });
});
