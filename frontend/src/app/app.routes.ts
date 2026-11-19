import { Routes } from '@angular/router';
import { authGuard } from './guards/auth.guard';

export const routes: Routes = [
  { path: '', loadComponent: () => import('./pages/home/home').then((m) => m.Home) },
  { path: 'login', loadComponent: () => import('./pages/login/login').then((m) => m.Login) },
  { path: 'register', loadComponent: () => import('./pages/register/register').then((m) => m.Register) },
  {
    path: 'dashboard',
    canActivate: [authGuard],
    loadComponent: () => import('./pages/dashboard/dashboard').then((m) => m.Dashboard),
  },
  {
    path: 'items',
    canActivate: [authGuard],
    loadComponent: () => import('./pages/items/items').then((m) => m.Items),
  },
  { path: 'error', loadComponent: () => import('./pages/server-error/server-error').then((m) => m.ServerError) },
  { path: '**', loadComponent: () => import('./pages/not-found/not-found').then((m) => m.NotFound) },
];
