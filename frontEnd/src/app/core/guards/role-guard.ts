import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { jwtDecode } from 'jwt-decode';

interface JwtPayload {
  sub: string;
  role: string;
  exp: number;
  iat: number;
}

export const roleGuard: CanActivateFn = (route, state) => {
  const router = inject(Router);

  const token = localStorage.getItem('jwt_token');

  if (!token) {
    return router.createUrlTree(['/login']);
  }

  try {
    const decodedToken = jwtDecode<JwtPayload>(token);

    console.log('Role:', decodedToken.role);

    if (decodedToken.role === 'ROLE_SELLER') {
      return true;
    }

    return router.createUrlTree(['/home']);

  } catch (error) {
    console.error('Invalid JWT:', error);

    localStorage.removeItem('jwt_token');

    return router.createUrlTree(['/login']);
  }
};
