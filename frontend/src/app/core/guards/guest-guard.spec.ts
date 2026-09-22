import { TestBed } from '@angular/core/testing';
import { Router, ActivatedRouteSnapshot, RouterStateSnapshot } from '@angular/router';
import { MatSnackBar } from '@angular/material/snack-bar';
import { describe, it, expect, beforeEach, vi, afterEach } from 'vitest';
import { guestGuard } from './guest-guard';

describe('guestGuard', () => {
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

  it('should allow navigation and return false if token exists', () => {
    getItemSpy.mockReturnValue(null);

    const result = TestBed.runInInjectionContext(() =>
      guestGuard({} as ActivatedRouteSnapshot, {} as RouterStateSnapshot)
    );


    expect(result).toBe(true);
    expect(mockRouter.navigate).not.toHaveBeenCalled();
    expect(mockSnackBar.open).not.toHaveBeenCalled();
  });

  it('should deny navigation, show snackbar, and redirect to login if there is a token', () => {
    getItemSpy.mockReturnValue("valid_token");

    const result = TestBed.runInInjectionContext(() =>
      guestGuard({} as ActivatedRouteSnapshot, {} as RouterStateSnapshot)
    );

    // Assert
    expect(result).toBe(false);
    expect(mockSnackBar.open).toHaveBeenCalledWith(
      'You are already logged in!',
      'Close',
      { duration: 3000 }
    );
    expect(mockRouter.navigate).toHaveBeenCalledWith(['/home']);
  });
});
