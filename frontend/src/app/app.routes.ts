import { Routes } from '@angular/router';
import { authGuard } from './guards/auth.guard';

export const routes: Routes = [
  { path: '', title: 'Inicio', loadComponent: () => import('./pages/home/home').then((m) => m.Home) },
  { path: 'login', title: 'Ingresar', loadComponent: () => import('./pages/login/login').then((m) => m.Login) },
  {
    path: 'register',
    title: 'Crear cuenta',
    loadComponent: () => import('./pages/register/register').then((m) => m.Register),
  },
  {
    path: 'dashboard',
    title: 'Dashboard',
    canActivate: [authGuard],
    loadComponent: () => import('./pages/dashboard/dashboard').then((m) => m.Dashboard),
  },
  {
    path: 'items',
    title: 'Items',
    canActivate: [authGuard],
    loadComponent: () => import('./pages/items/items').then((m) => m.Items),
  },
  {
    path: 'forbidden',
    title: 'Sin permiso',
    loadComponent: () => import('./pages/forbidden/forbidden').then((m) => m.Forbidden),
  },
  {
    path: 'error',
    title: 'Error del servidor',
    loadComponent: () => import('./pages/server-error/server-error').then((m) => m.ServerError),
  },
  {
    path: '**',
    title: 'Página no encontrada',
    loadComponent: () => import('./pages/not-found/not-found').then((m) => m.NotFound),
  },
];
