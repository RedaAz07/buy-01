import { inject } from '@angular/core';
import { MatSnackBar } from '@angular/material/snack-bar';
import { CanActivateFn, Router } from '@angular/router';

export const guestGuard: CanActivateFn = (route, state) => {
  const router = inject(Router);
  const token = localStorage.getItem('jwt_token');
  const snackBar = inject(MatSnackBar);
  if (token) {
    snackBar.open('You are already logged in!', 'Close', { duration: 3000 });
    router.navigate(['/home']);
    return false;
  }
  return true;
};
