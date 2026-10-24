import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';
import { KycApiService } from '../../core/kyc-api.service';
import { NotificationService } from '../../core/notification.service';
import { LIBELLES_TYPE_PIECE } from '../../core/libelles';
import {
  Client,
  DossierCreation,
  DossierDetail,
  TypePiece,
} from '../../core/models';

@Component({
  selector: 'app-dossier-form-page',
  imports: [
    ReactiveFormsModule,
    RouterLink,
    MatButtonModule,
    MatCardModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatProgressSpinnerModule,
    MatSelectModule,
  ],
  templateUrl: './dossier-form.page.html',
  styleUrl: './dossier-form.page.scss',
})
export class DossierFormPage {
  private readonly api = inject(KycApiService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly notifications = inject(NotificationService);
  private readonly fb = inject(FormBuilder);

  private readonly dossierId = this.route.snapshot.paramMap.get('id');

  protected readonly modification = this.dossierId !== null;
  protected readonly typesPiece = Object.entries(LIBELLES_TYPE_PIECE) as [TypePiece, string][];
  protected readonly clients = signal<Client[]>([]);
  protected readonly dossier = signal<DossierDetail | null>(null);
  protected readonly chargement = signal(this.modification);
  protected readonly enregistrement = signal(false);

  protected readonly form = this.fb.group({
    clientId: this.fb.control<number | null>(null, Validators.required),
    typePiece: this.fb.nonNullable.control<TypePiece>('CNI', Validators.required),
    numeroPiece: this.fb.nonNullable.control('', [
      Validators.required,
      Validators.maxLength(40),
    ]),
    dateExpirationPiece: this.fb.nonNullable.control('', Validators.required),
    revenuMensuel: this.fb.control<number | null>(null, Validators.min(0)),
    chargesMensuelles: this.fb.control<number | null>(null, Validators.min(0)),
    ancienneteActiviteMois: this.fb.control<number | null>(null, Validators.min(0)),
    nbPersonnesCharge: this.fb.control<number | null>(null, Validators.min(0)),
  });

  constructor() {
    if (this.modification) {
      this.form.controls.clientId.disable();
      this.chargerDossier();
    } else {
      this.chargerClients();
    }
  }

  protected get estCorrection(): boolean {
    return this.dossier()?.statut === 'REJETE';
  }

  protected enregistrer(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.enregistrement.set(true);
    const valeurs = this.form.getRawValue();
    const base: Omit<DossierCreation, 'clientId'> = {
      typePiece: valeurs.typePiece,
      numeroPiece: valeurs.numeroPiece.trim(),
      dateExpirationPiece: valeurs.dateExpirationPiece,
      revenuMensuel: nombreOuNull(valeurs.revenuMensuel),
      chargesMensuelles: nombreOuNull(valeurs.chargesMensuelles),
      ancienneteActiviteMois: nombreOuNull(valeurs.ancienneteActiviteMois),
      nbPersonnesCharge: nombreOuNull(valeurs.nbPersonnesCharge),
    };

    if (this.dossierId) {
      this.api.modifier(Number(this.dossierId), base).subscribe({
        next: (dossier) => this.apresEnregistrement(dossier, true),
        error: (erreur) => this.echec(erreur),
      });
    } else {
      const demande: DossierCreation = { ...base, clientId: valeurs.clientId ?? undefined };
      this.api.creer(demande).subscribe({
        next: (dossier) => this.apresEnregistrement(dossier, false),
        error: (erreur) => this.echec(erreur),
      });
    }
  }

  private chargerClients(): void {
    this.api.clients().subscribe({
      next: (clients) => {
        this.clients.set(clients);
        this.chargement.set(false);
      },
      error: (erreur) => {
        this.chargement.set(false);
        this.notifications.erreur(erreur);
      },
    });
  }

  private chargerDossier(): void {
    this.api.detail(Number(this.dossierId)).subscribe({
      next: (dossier) => {
        if (dossier.statut !== 'BROUILLON' && dossier.statut !== 'REJETE') {
          this.notifications.info('Ce dossier n’est plus modifiable.');
          void this.router.navigate(['/dossiers', dossier.id]);
          return;
        }
        this.dossier.set(dossier);
        this.form.patchValue({
          clientId: dossier.clientId,
          typePiece: dossier.typePiece,
          numeroPiece: dossier.numeroPiece ?? '',
          dateExpirationPiece: dossier.dateExpirationPiece,
          revenuMensuel: dossier.revenuMensuel ?? null,
          chargesMensuelles: dossier.chargesMensuelles ?? null,
          ancienneteActiviteMois: dossier.ancienneteActiviteMois ?? null,
          nbPersonnesCharge: dossier.nbPersonnesCharge ?? null,
        });
        this.chargement.set(false);
      },
      error: (erreur) => {
        this.chargement.set(false);
        this.notifications.erreur(erreur);
        void this.router.navigate(['/agent/dossiers']);
      },
    });
  }

  private apresEnregistrement(dossier: DossierDetail, modification: boolean): void {
    this.enregistrement.set(false);
    if (this.estCorrection) {
      this.notifications.succes(
        `Nouvelle version v${dossier.version} créée. Déposez à nouveau les documents.`,
      );
    } else if (modification) {
      this.notifications.succes('Dossier mis à jour.');
    } else {
      this.notifications.succes('Dossier créé. Déposez le recto et le verso de la pièce.');
    }
    void this.router.navigate(['/dossiers', dossier.id]);
  }

  private echec(erreur: unknown): void {
    this.enregistrement.set(false);
    this.notifications.erreur(erreur);
  }
}

function nombreOuNull(valeur: unknown): number | null {
  if (valeur === null || valeur === undefined || valeur === '') {
    return null;
  }
  const nombre = Number(valeur);
  return Number.isFinite(nombre) ? nombre : null;
}
