import { Routes } from '@angular/router';
import { accueilGuard, authGuard, roleGuard } from './core/auth.guard';

export const routes: Routes = [
  {
    path: 'login',
    loadComponent: () => import('./features/login/login.page').then((m) => m.LoginPage),
    title: 'Connexion · e-KYC',
  },
  {
    path: 'agent',
    canActivate: [authGuard, roleGuard(['AGENT_ACCUEIL'])],
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'dossiers' },
      {
        path: 'dossiers',
        loadComponent: () =>
          import('./features/dossiers/dossier-list.page').then((m) => m.DossierListPage),
        data: { mode: 'agent' },
        title: 'Mes dossiers · e-KYC',
      },
      {
        path: 'dossiers/nouveau',
        loadComponent: () =>
          import('./features/dossiers/dossier-form.page').then((m) => m.DossierFormPage),
        title: 'Nouveau dossier · e-KYC',
      },
      {
        path: 'dossiers/:id/modifier',
        loadComponent: () =>
          import('./features/dossiers/dossier-form.page').then((m) => m.DossierFormPage),
        title: 'Modifier le dossier · e-KYC',
      },
    ],
  },
  {
    path: 'analyste',
    canActivate: [authGuard, roleGuard(['ANALYSTE_KYC'])],
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'file' },
      {
        path: 'file',
        loadComponent: () =>
          import('./features/dossiers/dossier-list.page').then((m) => m.DossierListPage),
        data: { mode: 'file' },
        title: 'File d’attente · e-KYC',
      },
      {
        path: 'dossiers',
        loadComponent: () =>
          import('./features/dossiers/dossier-list.page').then((m) => m.DossierListPage),
        data: { mode: 'tous' },
        title: 'Dossiers · e-KYC',
      },
    ],
  },
  {
    path: 'superviseur',
    canActivate: [authGuard, roleGuard(['SUPERVISEUR'])],
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'decisions' },
      {
        path: 'decisions',
        loadComponent: () =>
          import('./features/dossiers/dossier-list.page').then((m) => m.DossierListPage),
        data: { mode: 'decisions' },
        title: 'Décisions à rendre · e-KYC',
      },
      {
        path: 'dossiers',
        loadComponent: () =>
          import('./features/dossiers/dossier-list.page').then((m) => m.DossierListPage),
        data: { mode: 'tous' },
        title: 'Dossiers · e-KYC',
      },
      {
        path: 'audit',
        loadComponent: () => import('./features/audit/audit.page').then((m) => m.AuditPage),
        title: 'Journal d’audit · e-KYC',
      },
    ],
  },
  {
    path: 'admin',
    canActivate: [authGuard, roleGuard(['ADMIN'])],
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'utilisateurs' },
      {
        path: 'utilisateurs',
        loadComponent: () =>
          import('./features/admin/utilisateurs.page').then((m) => m.UtilisateursPage),
        title: 'Utilisateurs · e-KYC',
      },
      {
        path: 'criteres',
        loadComponent: () =>
          import('./features/admin/criteres.page').then((m) => m.CriteresPage),
        title: 'Critères de scoring · e-KYC',
      },
    ],
  },
  {
    path: 'dossiers/:id',
    loadComponent: () =>
      import('./features/dossiers/dossier-detail.page').then((m) => m.DossierDetailPage),
    canActivate: [
      authGuard,
      roleGuard(['AGENT_ACCUEIL', 'ANALYSTE_KYC', 'SUPERVISEUR']),
    ],
    title: 'Dossier KYC · e-KYC',
  },
  {
    path: '',
    pathMatch: 'full',
    canActivate: [accueilGuard],
    loadComponent: () => import('./features/login/login.page').then((m) => m.LoginPage),
  },
  { path: '**', redirectTo: '' },
];
