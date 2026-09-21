import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Register } from './register';
import { provideRouter, Router } from '@angular/router';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { Auth } from '../../core/services/auth';
import { MatSnackBar } from '@angular/material/snack-bar';
import { of, throwError } from 'rxjs';

describe('register', () => {
  let component: Register;
  let fixture: ComponentFixture<Register>;
  let router: Router;

  let mockAuthService: { register: ReturnType<typeof vi.fn> };
  let mockSnackBar: { open: ReturnType<typeof vi.fn> };


  beforeEach(async () => {
    mockAuthService = { register: vi.fn() };
    mockSnackBar = { open: vi.fn() };

    await TestBed.configureTestingModule({
      imports: [Register],
      providers: [
        provideNoopAnimations(),
        provideRouter([]),
        { provide: Auth, useValue: mockAuthService },
        { provide: MatSnackBar, useValue: mockSnackBar },

      ],
    }).compileComponents();

    fixture = TestBed.createComponent(Register);
    component = fixture.componentInstance;
    router = TestBed.inject(Router);
    vi.spyOn(router, 'navigate').mockImplementation(() => Promise.resolve(true));

    fixture.detectChanges();
  });

  it('should create register component', () => {
    expect(component).toBeTruthy();
  });

  describe('Form Initialization & Validation', () => {
    it('should initialize with empty inputs and an invalid form', () => {
      expect(component.registerForm.valid).toBe(false);
      expect(component.registerForm.get('name')?.value).toBe('');
      expect(component.registerForm.get('email')?.value).toBe('');
      expect(component.registerForm.get('role')?.value).toBe('');
      expect(component.registerForm.get('password')?.value).toBe('');
    });

    it('should invalidate password if shorter than 6 characters', () => {
      const passwordControl = component.registerForm.get('password');

      passwordControl?.setValue('12345');
      expect(passwordControl?.valid).toBe(false);

      passwordControl?.setValue('123456');
      expect(passwordControl?.valid).toBe(true);
    });
    it('should invalidate name  if shorter than 6 characters  and has exlude the [a-zA-Z0-9]', () => {
      const NameControl = component.registerForm.get('name');
      NameControl?.setValue('azertyuiopqsdfgh');
      expect(NameControl?.valid).toBe(false);
      NameControl?.setValue('te');
      expect(NameControl?.valid).toBe(false);
      NameControl?.setValue('tes t2');
      expect(NameControl?.valid).toBe(false);
      NameControl?.setValue('test');
      expect(NameControl?.valid).toBe(true);
    });
    it('should validate  email', () => {
      const emailControle = component.registerForm.get('email');
      emailControle?.setValue('reda@gmail.com');
      expect(emailControle?.valid).toBe(true);
      emailControle?.setValue('reda @gmail.com');
      expect(emailControle?.valid).toBe(false);
      emailControle?.setValue('reda.com');
      expect(emailControle?.valid).toBe(false);

    });

    it('should validate  role', () => {
      const roleControle = component.registerForm.get('role');
      roleControle?.setValue('ROLE_SELLER');
      expect(roleControle?.valid).toBe(true);
      roleControle?.setValue('ROLE_CLIENT');
      expect(roleControle?.valid).toBe(true);
      roleControle?.setValue('random');
      expect(roleControle?.valid).toBe(false);

    });

    it('should mark form as valid when  constraints', () => {
      component.registerForm.patchValue({
        name: 'testUser',
        password: 'password123',
        email: "test@gmail.com",
        role: "ROLE_SELLER"
      });
      expect(component.registerForm.valid).toBe(true);
    });
  });



  describe('onSubmit()', () => {
    it('should stop execution and not call Auth service if form is invalid', () => {
      component.onSubmit();
      expect(mockAuthService.register).not.toHaveBeenCalled();
    });

    describe('When register succeeds', () => {
      let setItemSpy: ReturnType<typeof vi.spyOn>;

      beforeEach(() => {
        component.registerForm.patchValue({
          name: 'testUser',
          password: 'password123',
          email: "test@gmail.com",
          role: "ROLE_SELLER"
        });

        mockAuthService.register.mockReturnValue(of({ jwt: 'fake-jwt-token' }));

        setItemSpy = vi.spyOn(Storage.prototype, 'setItem');
      });

      it('should store JWT token, navigate to home, and open success snackbar', () => {
        component.onSubmit();

        expect(mockAuthService.register).toHaveBeenCalledWith({
          name: 'testUser',
          password: 'password123',
          email: "test@gmail.com",
          role: "ROLE_SELLER"
        });

        expect(setItemSpy).toHaveBeenCalledWith('jwt_token', 'fake-jwt-token');
        expect(router.navigate).toHaveBeenCalledWith(['/home']);
        expect(mockSnackBar.open).toHaveBeenCalledWith('Register successful!', 'Close', { duration: 3000 });
      });
    });

    describe('When login fails', () => {
      beforeEach(() => {
        component.registerForm.patchValue({
          name: 'testUser',
          password: 'password123',
          email: "test@gmail.com",
          role: "ROLE_SELLER"
        });
      });

      it('should display the specific error message provided by backend API', () => {
        const mockApiError = { error: { message: 'name already exists' } };
        mockAuthService.register.mockReturnValue(throwError(() => mockApiError));
        component.onSubmit();

        expect(mockSnackBar.open).toHaveBeenCalledWith('name already exists', 'Close', { duration: 3000 });
        expect(router.navigate).not.toHaveBeenCalled();
      });

      it('should display fallback error message if backend payload contains no message', () => {
        mockAuthService.register.mockReturnValue(throwError(() => new Error('Server unreachable')));

        component.onSubmit();

        expect(mockSnackBar.open).toHaveBeenCalledWith('Registration failed. Please try again.', 'Close', { duration: 3000 });
      });
    });
  });
});
