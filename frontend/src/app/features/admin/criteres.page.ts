import { Component, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatDialog } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTableModule } from '@angular/material/table';
import { AdminApiService, DemandeCritere } from '../../core/admin-api.service';
import { NotificationService } from '../../core/notification.service';
import { LIBELLES_CRITERE } from '../../core/libelles';
import { Critere } from '../../core/models';
import { ConfirmationDialog } from '../../shared/confirmation.dialog';
import { CritereDialog, CritereDialogData } from './critere-dialog';

@Component({
  selector: 'app-criteres-page',
  imports: [
    MatButtonModule,
    MatCardModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatTableModule,
  ],
  templateUrl: './criteres.page.html',
  styleUrl: './criteres.page.scss',
})
export class CriteresPage {
  private readonly api = inject(AdminApiService);
  private readonly dialog = inject(MatDialog);
  private readonly notifications = inject(NotificationService);

  protected readonly criteres = signal<Critere[]>([]);
  protected readonly chargement = signal(true);
  protected readonly action = signal<number | null>(null);

  protected readonly colonnes = ['critere', 'libelle', 'bornes', 'points', 'actif', 'actions'];
  protected readonly libellesCritere = LIBELLES_CRITERE;

  constructor() {
    this.charger();
  }

  protected charger(): void {
    this.chargement.set(true);
    this.api.criteres().subscribe({
      next: (criteres) => {
        this.criteres.set(criteres);
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
      .open<CritereDialog, CritereDialogData, DemandeCritere>(CritereDialog, {
        data: {},
        width: '620px',
        maxWidth: '94vw',
        disableClose: true,
      })
      .afterClosed()
      .subscribe((demande) => {
        if (!demande) {
          return;
        }
        this.api.creerCritere(demande).subscribe({
          next: () => {
            this.notifications.succes('Tranche de scoring créée.');
            this.charger();
          },
          error: (erreur) => this.notifications.erreur(erreur),
        });
      });
  }

  protected modifier(critere: Critere): void {
    this.dialog
      .open<CritereDialog, CritereDialogData, DemandeCritere>(CritereDialog, {
        data: { critere },
        width: '620px',
        maxWidth: '94vw',
        disableClose: true,
      })
      .afterClosed()
      .subscribe((demande) => {
        if (!demande) {
          return;
        }
        this.api.modifierCritere(critere.id, demande).subscribe({
          next: () => {
            this.notifications.succes('Tranche de scoring mise à jour.');
            this.charger();
          },
          error: (erreur) => this.notifications.erreur(erreur),
        });
      });
  }

  protected supprimer(critere: Critere): void {
    this.dialog
      .open<ConfirmationDialog, unknown, boolean>(ConfirmationDialog, {
        data: {
          titre: 'Supprimer la tranche',
          message: `La tranche « ${critere.libelle} » (${critere.points} points) sera supprimée. Les scorings déjà calculés ne sont pas modifiés.`,
          libelleConfirmer: 'Supprimer',
          danger: true,
        },
        width: '470px',
      })
      .afterClosed()
      .subscribe((confirme) => {
        if (!confirme) {
          return;
        }
        this.action.set(critere.id);
        this.api.supprimerCritere(critere.id).subscribe({
          next: () => {
            this.action.set(null);
            this.notifications.succes('Tranche supprimée.');
            this.charger();
          },
          error: (erreur) => {
            this.action.set(null);
            this.notifications.erreur(erreur);
          },
        });
      });
  }

  protected libelleCritere(critere: string): string {
    return LIBELLES_CRITERE[critere] ?? critere;
  }

  protected borne(valeur: number | null | undefined): string {
    if (valeur === null || valeur === undefined) {
      return '—';
    }
    return new Intl.NumberFormat('fr-FR', { maximumFractionDigits: 4 }).format(valeur);
  }
}
