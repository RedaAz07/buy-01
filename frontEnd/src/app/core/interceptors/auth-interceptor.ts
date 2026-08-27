import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Router } from '@angular/router';
import { ErrorMessage } from '../services/error-message';
import { catchError, throwError } from 'rxjs';

export const authInterceptor: HttpInterceptorFn = (req, next) => {

  const token = localStorage.getItem("jwt_token");
  const snackBar = inject(MatSnackBar);
  const router = inject(Router);
  const errorMessages = inject(ErrorMessage);
  let authreq = req;
  if (token) {
    authreq.clone({
      setHeaders: {
        Authorization: `Bearer ${token}`,
      }
    })
  }
  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      const backendMessage = errorMessages.getHttpErrorMessage(error);

      if (error.status === 401 && token) {
        localStorage.removeItem('jwt_token');
        router.navigate(['/login']);
        return throwError(() => error);
      }
      if (error.status === 403) {
        snackBar.open(backendMessage, 'Close', {
          duration: 5000,
          panelClass: ['error-snackbar'],
          horizontalPosition: 'end',
          verticalPosition: 'bottom',
        });
        return throwError(() => error);
      }

      // snackBar.open(backendMessage, 'Close', {
      //   duration: 5000,
      //   panelClass: ['error-snackbar'],
      //   horizontalPosition: 'end',
      //   verticalPosition: 'bottom',
      // });

      return throwError(() => error);
    })
  );
};
