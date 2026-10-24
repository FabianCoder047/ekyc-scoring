import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { accueilRole, AuthService } from './auth.service';
import { Role } from './models';

export const authGuard: CanActivateFn = (_route, etat) => {
  const auth = inject(AuthService);
  const router = inject(Router);
  if (auth.connecte()) {
    return true;
  }
  return router.createUrlTree(['/login'], { queryParams: { retour: etat.url } });
};

export const roleGuard = (roles: Role[]): CanActivateFn => () => {
  const auth = inject(AuthService);
  const router = inject(Router);
  const utilisateur = auth.utilisateur();
  if (!utilisateur) {
    return router.createUrlTree(['/login']);
  }
  return roles.includes(utilisateur.role) ? true : router.createUrlTree([accueilRole(utilisateur.role)]);
};

export const accueilGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);
  const utilisateur = auth.utilisateur();
  return router.createUrlTree([utilisateur ? accueilRole(utilisateur.role) : '/login']);
};
