import { HttpErrorResponse } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Probleme } from './models';

@Injectable({ providedIn: 'root' })
export class NotificationService {
  private readonly snackBar = inject(MatSnackBar);

  succes(message: string): void {
    this.snackBar.open(message, 'Fermer', { duration: 4000, panelClass: 'snack-succes' });
  }

  erreur(erreur: unknown): void {
    this.snackBar.open(messageErreur(erreur), 'Fermer', {
      duration: 6000,
      panelClass: 'snack-erreur',
    });
  }

  info(message: string): void {
    this.snackBar.open(message, 'Fermer', { duration: 4000 });
  }

  erreurTexte(message: string): void {
    this.snackBar.open(message, 'Fermer', {
      duration: 6000,
      panelClass: 'snack-erreur',
    });
  }
}

export function messageErreur(erreur: unknown): string {
  if (erreur instanceof HttpErrorResponse) {
    if (erreur.status === 0) {
      return 'Le serveur est injoignable. Vérifiez votre connexion.';
    }
    const probleme = erreur.error as Probleme | null;
    if (probleme?.detail) {
      return probleme.detail;
    }
    if (probleme?.erreurs?.length) {
      return probleme.erreurs.map((e) => e.message).join(' · ');
    }
    if (erreur.status === 403) {
      return 'Accès refusé.';
    }
    if (erreur.status === 429) {
      return 'Trop de tentatives. Réessayez plus tard.';
    }
    return `Erreur ${erreur.status}.`;
  }
  return 'Une erreur inattendue est survenue.';
}
