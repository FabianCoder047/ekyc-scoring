import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, switchMap, throwError } from 'rxjs';
import { AuthService } from './auth.service';

export const authInterceptor: HttpInterceptorFn = (requete, suivant) => {
  const auth = inject(AuthService);
  const token = auth.session()?.accessToken;

  const avecJeton = token && !estRoutePublique(requete.url)
    ? requete.clone({ setHeaders: { Authorization: `Bearer ${token}` } })
    : requete;

  return suivant(avecJeton).pipe(
    catchError((erreur: unknown) => {
      if (
        erreur instanceof HttpErrorResponse &&
        erreur.status === 401 &&
        !estRoutePublique(requete.url)
      ) {
        return auth.rafraichir().pipe(
          switchMap((session) =>
            suivant(
              requete.clone({
                setHeaders: { Authorization: `Bearer ${session.accessToken}` },
              }),
            ),
          ),
          catchError((erreurRafraichissement) => {
            auth.deconnecter();
            return throwError(() => erreurRafraichissement);
          }),
        );
      }
      return throwError(() => erreur);
    }),
  );
};

function estRoutePublique(url: string): boolean {
  return url.includes('/auth/login') || url.includes('/auth/refresh');
}
