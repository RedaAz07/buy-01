import { Routes } from '@angular/router';
import { Login } from './auth/login/login';
import { Register } from './auth/register/register';
import { guestGuard } from './core/guards/guest-guard';
import { Home } from './home/home';
import { authGuard } from './core/guards/auth-guard';
import { Product } from './product/product';

export const routes: Routes = [
  { path: "", redirectTo: "home", pathMatch: "full" },
  { path: 'login', component: Login, canActivate: [guestGuard] },
  { path: 'register', component: Register, canActivate: [guestGuard] },
  { path: 'home', component: Home, canActivate: [authGuard] },
  { path: 'product/:id', component: Product, canActivate: [authGuard] },
  { path: '**', redirectTo: 'home' },
];
