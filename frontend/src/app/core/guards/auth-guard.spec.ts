import { TestBed } from '@angular/core/testing';
import { Router, ActivatedRouteSnapshot, RouterStateSnapshot } from '@angular/router';
import { MatSnackBar } from '@angular/material/snack-bar';
import { describe, it, expect, beforeEach, vi, afterEach } from 'vitest';
import { authGuard } from './auth-guard';

describe('authGuard', () => {
  let mockRouter: { navigate: ReturnType<typeof vi.fn> };
  let mockSnackBar: { open: ReturnType<typeof vi.fn> };
  let getItemSpy: ReturnType<typeof vi.spyOn>;

  beforeEach(() => {
    mockRouter = { navigate: vi.fn() };
    mockSnackBar = { open: vi.fn() };

    TestBed.configureTestingModule({
      providers: [
        { provide: Router, useValue: mockRouter },
        { provide: MatSnackBar, useValue: mockSnackBar }
      ]
    });

    getItemSpy = vi.spyOn(Storage.prototype, 'getItem');
  });

  afterEach(() => {
    vi.clearAllMocks();
  });

  it('should allow navigation and return true if token exists', () => {
    getItemSpy.mockReturnValue('valid-token');

    const result = TestBed.runInInjectionContext(() =>
      authGuard({} as ActivatedRouteSnapshot, {} as RouterStateSnapshot)
    );

    // Assert
    expect(result).toBe(true);
    expect(mockRouter.navigate).not.toHaveBeenCalled();
    expect(mockSnackBar.open).not.toHaveBeenCalled();
  });

  it('should deny navigation, show snackbar, and redirect to login if no token', () => {
    // Arrange: Simulate a logged-out user
    getItemSpy.mockReturnValue(null);

    // Act
    const result = TestBed.runInInjectionContext(() =>
      authGuard({} as ActivatedRouteSnapshot, {} as RouterStateSnapshot)
    );

    // Assert
    expect(result).toBe(false);
    expect(mockSnackBar.open).toHaveBeenCalledWith(
      'You must be logged in to view this page!',
      'Close',
      { duration: 3000 }
    );
    expect(mockRouter.navigate).toHaveBeenCalledWith(['/login']);
  });
});
