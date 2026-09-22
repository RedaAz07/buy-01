import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Navbar } from './navbar';
import { Auth } from '../../services/auth';
import { provideRouter, Router } from '@angular/router';
import { describe, it, expect, beforeEach, vi } from 'vitest';
import { BehaviorSubject } from 'rxjs';

describe('Navbar', () => {
  let component: Navbar;
  let fixture: ComponentFixture<Navbar>;
  let router: Router;

  let mockAuthService: {
    currentUser$: BehaviorSubject<any>;
    logout: ReturnType<typeof vi.fn>;
  };

  beforeEach(async () => {
    mockAuthService = {
      currentUser$: new BehaviorSubject<any>(null),
      logout: vi.fn()
    };

    await TestBed.configureTestingModule({
      imports: [Navbar],
      providers: [
        provideRouter([]),
        { provide: Auth, useValue: mockAuthService },
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(Navbar);
    component = fixture.componentInstance;

    router = TestBed.inject(Router);
    vi.spyOn(router, 'navigate').mockImplementation(() => Promise.resolve(true));

    fixture.detectChanges();
  });

  it('should create the navbar', () => {
    expect(component).toBeTruthy();
  });

  describe('ngOnInit (User State)', () => {
    it('should update the "user" signal when currentUser$ emits a new value', () => {
      const mockUser = { id: 1, name: 'Reda', role: 'ROLE_SELLER' };

      mockAuthService.currentUser$.next(mockUser);

      expect(component.user()).toEqual(mockUser);
    });
  });

  describe('Component Methods', () => {
    it('should call authService.logout() when logout is clicked', () => {
      component.logout();
      expect(mockAuthService.logout).toHaveBeenCalled();
    });

    it('should navigate to dashboard when route() is called', () => {
      component.route();
      expect(router.navigate).toHaveBeenCalledWith(['/dashboard']);
    });
  });
});
