import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { provideRouter, Router } from '@angular/router'; // 1. Import provideRouter
import { MatSnackBar } from '@angular/material/snack-bar';
import { of, throwError } from 'rxjs';
import { describe, it, expect, beforeEach, vi } from 'vitest';

import { Login } from './login';
import { Auth } from '../../core/services/auth';

describe('Login Component', () => {
  let component: Login;
  let fixture: ComponentFixture<Login>;
  let router: Router;

  let mockAuthService: { login: ReturnType<typeof vi.fn> };
  let mockSnackBar: { open: ReturnType<typeof vi.fn> };

  beforeEach(async () => {
    mockAuthService = { login: vi.fn() };
    mockSnackBar = { open: vi.fn() };

    await TestBed.configureTestingModule({
      imports: [Login],
      providers: [
        provideNoopAnimations(),
        provideRouter([]),
        { provide: Auth, useValue: mockAuthService },
        { provide: MatSnackBar, useValue: mockSnackBar },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(Login);
    component = fixture.componentInstance;

    router = TestBed.inject(Router);
    vi.spyOn(router, 'navigate').mockImplementation(() => Promise.resolve(true));

    fixture.detectChanges();
  });

  it('should create the login component', () => {
    expect(component).toBeTruthy();
  });

  describe('Form Initialization & Validation', () => {
    it('should initialize with empty inputs and an invalid form', () => {
      expect(component.loginForm.valid).toBe(false);
      expect(component.loginForm.get('name')?.value).toBe('');
      expect(component.loginForm.get('password')?.value).toBe('');
    });

    it('should invalidate password if shorter than 6 characters', () => {
      const passwordControl = component.loginForm.get('password');

      passwordControl?.setValue('12345');
      expect(passwordControl?.valid).toBe(false);

      passwordControl?.setValue('123456');
      expect(passwordControl?.valid).toBe(true);
    });

    it('should mark form as valid when name and password meet constraints', () => {
      component.loginForm.patchValue({
        name: 'testUser',
        password: 'password123',
      });
      expect(component.loginForm.valid).toBe(true);
    });
  });

  describe('onSubmit()', () => {
    it('should stop execution and not call Auth service if form is invalid', () => {
      component.onSubmit();
      expect(mockAuthService.login).not.toHaveBeenCalled();
    });

    describe('When login succeeds', () => {
      let setItemSpy: ReturnType<typeof vi.spyOn>;

      beforeEach(() => {
        component.loginForm.patchValue({ name: 'testUser', password: 'password123' });

        mockAuthService.login.mockReturnValue(of({ jwt: 'fake-jwt-token' }));

        setItemSpy = vi.spyOn(Storage.prototype, 'setItem');
      });

      it('should store JWT token, navigate to home, and open success snackbar', () => {
        component.onSubmit();

        expect(mockAuthService.login).toHaveBeenCalledWith({
          name: 'testUser',
          password: 'password123',
        });

        expect(setItemSpy).toHaveBeenCalledWith('jwt_token', 'fake-jwt-token');
        expect(router.navigate).toHaveBeenCalledWith(['/home']);
        expect(mockSnackBar.open).toHaveBeenCalledWith('Login successful!', 'Close', { duration: 3000 });
      });
    });

    describe('When login fails', () => {
      beforeEach(() => {
        component.loginForm.patchValue({ name: 'testUser', password: 'password123' });
      });

      it('should display the specific error message provided by backend API', () => {
        const mockApiError = { error: { message: 'Invalid username or password' } };
        mockAuthService.login.mockReturnValue(throwError(() => mockApiError));

        component.onSubmit();

        expect(mockSnackBar.open).toHaveBeenCalledWith('Invalid username or password', 'Close', { duration: 3000 });
        expect(router.navigate).not.toHaveBeenCalled();
      });

      it('should display fallback error message if backend payload contains no message', () => {
        mockAuthService.login.mockReturnValue(throwError(() => new Error('Server unreachable')));

        component.onSubmit();

        expect(mockSnackBar.open).toHaveBeenCalledWith('Login failed. Please try again.', 'Close', { duration: 3000 });
      });
    });
  });
});
