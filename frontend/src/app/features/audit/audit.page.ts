import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatDialog } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';
import { MatTableModule } from '@angular/material/table';
import { AuditApiService } from '../../core/audit-api.service';
import { NotificationService } from '../../core/notification.service';
import { ACTIONS_AUDIT, ENTITES_AUDIT, formatDateHeure } from '../../core/libelles';
import { AuditEntree, VerificationAudit } from '../../core/models';
import { JsonDialog } from '../../shared/json.dialog';

@Component({
  selector: 'app-audit-page',
  imports: [
    ReactiveFormsModule,
    MatButtonModule,
    MatCardModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatPaginatorModule,
    MatProgressSpinnerModule,
    MatSelectModule,
    MatTableModule,
  ],
  templateUrl: './audit.page.html',
  styleUrl: './audit.page.scss',
})
export class AuditPage {
  private readonly api = inject(AuditApiService);
  private readonly dialog = inject(MatDialog);
  private readonly notifications = inject(NotificationService);
  private readonly fb = inject(FormBuilder);

  protected readonly actions = ACTIONS_AUDIT;
  protected readonly entites = ENTITES_AUDIT;
  protected readonly formatDateHeure = formatDateHeure;
  protected readonly colonnes = [
    'horodatage',
    'utilisateur',
    'action',
    'entite',
    'adresse',
    'hash',
    'details',
  ];

  protected readonly entrees = signal<AuditEntree[]>([]);
  protected readonly total = signal(0);
  protected readonly chargement = signal(true);
  protected readonly verification = signal<VerificationAudit | null>(null);
  protected readonly verificationEnCours = signal(false);
  protected readonly pageIndex = signal(0);
  protected readonly taille = signal(25);

  protected readonly filtre = this.fb.nonNullable.group({
    entite: '',
    action: '',
    du: '',
    au: '',
  });

  constructor() {
    this.charger();
  }

  protected charger(): void {
    this.chargement.set(true);
    const { entite, action, du, au } = this.filtre.getRawValue();
    this.api
      .lister({
        entite: entite || undefined,
        action: action || undefined,
        du: instantIso(du),
        au: instantIso(au),
        page: this.pageIndex(),
        taille: this.taille(),
      })
      .subscribe({
        next: (page) => {
          this.entrees.set(page.contenu);
          this.total.set(page.total);
          this.chargement.set(false);
        },
        error: (erreur) => {
          this.chargement.set(false);
          this.notifications.erreur(erreur);
        },
      });
  }

  protected filtrer(): void {
    this.pageIndex.set(0);
    this.charger();
  }

  protected reinitialiser(): void {
    this.filtre.reset({ entite: '', action: '', du: '', au: '' });
    this.pageIndex.set(0);
    this.charger();
  }

  protected changerPage(evenement: PageEvent): void {
    this.pageIndex.set(evenement.pageIndex);
    this.taille.set(evenement.pageSize);
    this.charger();
  }

  protected verifier(): void {
    this.verificationEnCours.set(true);
    this.api.verifier().subscribe({
      next: (resultat) => {
        this.verification.set(resultat);
        this.verificationEnCours.set(false);
        if (resultat.valide) {
          this.notifications.succes(
            `Chaîne d’audit intègre : ${resultat.lignesVerifiees} ligne(s) vérifiée(s).`,
          );
        } else {
          this.notifications.erreurTexte(
            `Chaîne invalide à la ligne ${resultat.premiereLigneInvalide ?? '?'}.`,
          );
        }
      },
      error: (erreur) => {
        this.verificationEnCours.set(false);
        this.notifications.erreur(erreur);
      },
    });
  }

  protected details(entree: AuditEntree): void {
    this.dialog.open(JsonDialog, {
      data: {
        titre: `Audit #${entree.id} — ${entree.action}`,
        contenu: {
          avant: entree.avant ?? null,
          apres: entree.apres ?? null,
          userAgent: entree.userAgent ?? null,
          hashPrecedent: entree.hashPrecedent,
          hash: entree.hash,
        },
      },
      width: '720px',
      maxWidth: '92vw',
    });
  }

  protected aDesDetails(entree: AuditEntree): boolean {
    return entree.avant !== null || entree.apres !== null;
  }

  protected hashCourt(hash: string): string {
    return hash ? `${hash.slice(0, 12)}…` : '—';
  }
}

function instantIso(valeur: string): string | undefined {
  if (!valeur) {
    return undefined;
  }
  const date = new Date(valeur);
  return Number.isNaN(date.getTime()) ? undefined : date.toISOString();
}
