import { Routes } from '@angular/router';
import { Login } from './auth/login/login';
import { Register } from './auth/register/register';
import { guestGuard } from './core/guards/guest-guard';
import { Home } from './home/home';
import { authGuard } from './core/guards/auth-guard';
import { Product } from './product/product';
import { Dashboard } from './dashboard/dashboard';
import { roleGuard } from './core/guards/role-guard';
import { MainLayout } from './core/layouts/main-layout/main-layout';

export const routes: Routes = [
  { path: 'login', component: Login, canActivate: [guestGuard] },
  { path: 'register', component: Register, canActivate: [guestGuard] },
  {
    path: "", component: MainLayout,
    children: [
      { path: '', redirectTo: 'home', pathMatch: 'full' },
      { path: 'home', component: Home, canActivate: [authGuard] },
      { path: 'product/:id', component: Product, canActivate: [authGuard] },
    ]
  },
  { path: 'dashboard', component: Dashboard, canActivate: [roleGuard] },

  { path: '**', redirectTo: 'home' },
];
