import { Component, inject, signal } from '@angular/core';
import { forkJoin } from 'rxjs';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatDialog } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTableModule } from '@angular/material/table';
import { AdminApiService, DemandeUtilisateur } from '../../core/admin-api.service';
import { AuthService } from '../../core/auth.service';
import { NotificationService } from '../../core/notification.service';
import { LIBELLES_ROLE, formatDate } from '../../core/libelles';
import { Agence, Role, UtilisateurAdmin } from '../../core/models';
import { ConfirmationDialog } from '../../shared/confirmation.dialog';
import {
  UtilisateurDialog,
  UtilisateurDialogData,
} from './utilisateur-dialog';

@Component({
  selector: 'app-utilisateurs-page',
  imports: [
    MatButtonModule,
    MatCardModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatTableModule,
  ],
  templateUrl: './utilisateurs.page.html',
  styleUrl: './utilisateurs.page.scss',
})
export class UtilisateursPage {
  private readonly api = inject(AdminApiService);
  private readonly auth = inject(AuthService);
  private readonly dialog = inject(MatDialog);
  private readonly notifications = inject(NotificationService);

  protected readonly utilisateurs = signal<UtilisateurAdmin[]>([]);
  protected readonly agences = signal<Agence[]>([]);
  protected readonly chargement = signal(true);
  protected readonly action = signal<number | null>(null);

  protected readonly colonnes = [
    'nom',
    'email',
    'role',
    'agence',
    'actif',
    'creeLe',
    'actions',
  ];
  protected readonly libellesRole = LIBELLES_ROLE;
  protected readonly formatDate = formatDate;

  constructor() {
    this.charger();
  }

  protected charger(): void {
    this.chargement.set(true);
    forkJoin({
      utilisateurs: this.api.utilisateurs(),
      agences: this.api.agences(),
    }).subscribe({
      next: ({ utilisateurs, agences }) => {
        this.utilisateurs.set(utilisateurs);
        this.agences.set(agences);
        this.chargement.set(false);
      },
      error: (erreur) => {
        this.chargement.set(false);
        this.notifications.erreur(erreur);
      },
    });
  }

  protected creer(): void {
    this.dialog
      .open<UtilisateurDialog, UtilisateurDialogData, DemandeUtilisateur>(UtilisateurDialog, {
        data: { agences: this.agences() },
        width: '620px',
        maxWidth: '94vw',
        disableClose: true,
      })
      .afterClosed()
      .subscribe((demande) => {
        if (!demande) {
          return;
        }
        this.api.creerUtilisateur(demande).subscribe({
          next: () => {
            this.notifications.succes('Utilisateur créé.');
            this.charger();
          },
          error: (erreur) => this.notifications.erreur(erreur),
        });
      });
  }

  protected modifier(utilisateur: UtilisateurAdmin): void {
    this.dialog
      .open<UtilisateurDialog, UtilisateurDialogData, DemandeUtilisateur>(UtilisateurDialog, {
        data: { utilisateur, agences: this.agences() },
        width: '620px',
        maxWidth: '94vw',
        disableClose: true,
      })
      .afterClosed()
      .subscribe((demande) => {
        if (!demande) {
          return;
        }
        this.api.modifierUtilisateur(utilisateur.id, demande).subscribe({
          next: () => {
            this.notifications.succes('Utilisateur mis à jour.');
            this.charger();
          },
          error: (erreur) => this.notifications.erreur(erreur),
        });
      });
  }

  protected desactiver(utilisateur: UtilisateurAdmin): void {
    this.dialog
      .open<ConfirmationDialog, unknown, boolean>(ConfirmationDialog, {
        data: {
          titre: 'Désactiver le compte',
          message: `Le compte de ${utilisateur.prenom} ${utilisateur.nom} ne pourra plus se connecter. Les dossiers et traces d’audit sont conservés.`,
          libelleConfirmer: 'Désactiver',
          danger: true,
        },
        width: '460px',
      })
      .afterClosed()
      .subscribe((confirme) => {
        if (!confirme) {
          return;
        }
        this.action.set(utilisateur.id);
        this.api.desactiverUtilisateur(utilisateur.id).subscribe({
          next: () => {
            this.action.set(null);
            this.notifications.succes('Compte désactivé.');
            this.charger();
          },
          error: (erreur) => {
            this.action.set(null);
            this.notifications.erreur(erreur);
          },
        });
      });
  }

  protected estMonCompte(utilisateur: UtilisateurAdmin): boolean {
    return utilisateur.id === this.auth.utilisateur()?.id;
  }

  protected libelleRole(role: Role): string {
    return LIBELLES_ROLE[role];
  }
}
