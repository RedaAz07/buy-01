import {
  HttpErrorResponse,
  HttpInterceptorFn
} from '@angular/common/http';

import { inject } from '@angular/core';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';

import { ErrorMessage } from '../services/error-message';

export const authInterceptor: HttpInterceptorFn = (req, next) => {

  const token = localStorage.getItem('jwt_token');

  const snackBar = inject(MatSnackBar);
  const router = inject(Router);
  const errorMessages = inject(ErrorMessage);

  let authReq = req;

  // Add JWT
  if (token) {
    authReq = req.clone({
      setHeaders: {
        Authorization: `Bearer ${token}`
      }
    });
  }

  return next(authReq).pipe(

    catchError((error: HttpErrorResponse) => {

      const backendMessage =
        errorMessages.getHttpErrorMessage(error);

      console.log(error);

      // Token expired / invalid
      if (error.status === 401 && token) {

        localStorage.removeItem('jwt_token');

        router.navigate(['/login']);

        return throwError(() => error);
      }

      // Forbidden
      if (error.status === 403) {

        snackBar.open(
          backendMessage,
          'Close',
          {
            duration: 5000,
            panelClass: ['error-snackbar'],
            horizontalPosition: 'end',
            verticalPosition: 'bottom'
          }
        );

        return throwError(() => error);
      }

      // Other errors
      snackBar.open(
        backendMessage,
        'Close',
        {
          duration: 5000,
          panelClass: ['error-snackbar'],
          horizontalPosition: 'end',
          verticalPosition: 'bottom'
        }
      );

      return throwError(() => error);
    })
  );
};
