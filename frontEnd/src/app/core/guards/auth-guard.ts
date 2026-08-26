import { inject } from '@angular/core';
import { MatSnackBar } from '@angular/material/snack-bar';
import { CanActivateFn, Router } from '@angular/router';

export const authGuard: CanActivateFn = (route, state) => {
  const snackBar = inject(MatSnackBar);
  const router = inject(Router);
  const token = localStorage.getItem("jwt_token");
  if (token) {
    return true
  } else {
    snackBar.open('You must be logged in to view this page!', 'Close', { duration: 3000 });
    router.navigate(['/login']);
    return false;
  }

};
