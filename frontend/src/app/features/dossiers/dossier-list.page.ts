import { Component, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';
import { MatTableModule } from '@angular/material/table';
import { KycApiService } from '../../core/kyc-api.service';
import { NotificationService } from '../../core/notification.service';
import {
  LIBELLES_RISQUE,
  LIBELLES_STATUT,
  LIBELLES_TYPE_PIECE,
  classeRisque,
  classeStatut,
  formatDate,
} from '../../core/libelles';
import { DossierResume, NiveauRisque, StatutDossier, TypePiece } from '../../core/models';

type ModeListe = 'agent' | 'file' | 'decisions' | 'tous';

const TITRES: Record<ModeListe, { titre: string; sousTitre: string }> = {
  agent: {
    titre: 'Dossiers de mon agence',
    sousTitre: 'Créez, complétez et soumettez les dossiers de connaissance client.',
  },
  file: {
    titre: 'File d’attente KYC',
    sousTitre: 'Dossiers soumis par les agences et en attente de prise en charge.',
  },
  decisions: {
    titre: 'Décisions à rendre',
    sousTitre: 'Dossiers en revue, instruits et prêts pour la décision finale.',
  },
  tous: {
    titre: 'Tous les dossiers',
    sousTitre: 'Vue consolidée des dossiers accessibles avec votre rôle.',
  },
};

@Component({
  selector: 'app-dossier-list-page',
  imports: [
    ReactiveFormsModule,
    RouterLink,
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
  templateUrl: './dossier-list.page.html',
  styleUrl: './dossier-list.page.scss',
})
export class DossierListPage {
  private readonly api = inject(KycApiService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly notifications = inject(NotificationService);
  private readonly fb = inject(FormBuilder);

  protected readonly mode = (this.route.snapshot.data['mode'] as ModeListe) ?? 'tous';
  protected readonly entete = TITRES[this.mode];
  protected readonly colonnes = [
    'reference',
    'client',
    'agence',
    'statut',
    'risque',
    'piece',
    'date',
    'actions',
  ];

  protected readonly statuts = Object.entries(LIBELLES_STATUT) as [StatutDossier, string][];
  protected readonly libellesStatut = LIBELLES_STATUT;

  protected libelleStatut(statut: StatutDossier): string {
    return LIBELLES_STATUT[statut];
  }

  protected libelleRisque(niveau: NiveauRisque): string {
    return LIBELLES_RISQUE[niveau];
  }

  protected libelleTypePiece(type: TypePiece): string {
    return LIBELLES_TYPE_PIECE[type];
  }
  protected readonly libellesRisque = LIBELLES_RISQUE;
  protected readonly libellesTypePiece = LIBELLES_TYPE_PIECE;
  protected readonly classeStatut = classeStatut;
  protected readonly classeRisque = classeRisque;
  protected readonly formatDate = formatDate;

  protected readonly dossiers = signal<DossierResume[]>([]);
  protected readonly total = signal(0);
  protected readonly chargement = signal(true);
  protected readonly actionEnCours = signal<number | null>(null);
  protected readonly pageIndex = signal(0);
  protected readonly taille = signal(20);

  protected readonly filtre = this.fb.nonNullable.group({
    q: '',
    statut: this.statutInitial(),
  });

  protected readonly estAgent = computed(() => this.mode === 'agent');
  protected readonly peutCreer = computed(() => this.mode === 'agent');
  protected readonly afficherPriseEnCharge = computed(() => this.mode === 'file');
  protected readonly afficherDecision = computed(() => this.mode === 'decisions');

  constructor() {
    this.charger();
  }

  protected charger(): void {
    this.chargement.set(true);
    const { q, statut } = this.filtre.getRawValue();
    this.api.lister((statut || null) as StatutDossier | null, q, this.pageIndex(), this.taille()).subscribe({
      next: (page) => {
        this.dossiers.set(page.contenu);
        this.total.set(page.total);
        this.chargement.set(false);
      },
      error: (erreur) => {
        this.chargement.set(false);
        this.notifications.erreur(erreur);
      },
    });
  }

  protected rechercher(): void {
    this.pageIndex.set(0);
    this.charger();
  }

  protected changerStatut(): void {
    this.pageIndex.set(0);
    this.charger();
  }

  protected changerPage(evenement: PageEvent): void {
    this.pageIndex.set(evenement.pageIndex);
    this.taille.set(evenement.pageSize);
    this.charger();
  }

  protected prendreEnCharge(dossier: DossierResume): void {
    this.actionEnCours.set(dossier.id);
    this.api.prendreEnCharge(dossier.id).subscribe({
      next: () => {
        this.actionEnCours.set(null);
        this.notifications.succes(`Dossier #${dossier.id} pris en charge.`);
        void this.router.navigate(['/dossiers', dossier.id]);
      },
      error: (erreur) => {
        this.actionEnCours.set(null);
        this.notifications.erreur(erreur);
        this.charger();
      },
    });
  }

  private statutInitial(): string {
    if (this.mode === 'file') {
      return 'SOUMIS';
    }
    if (this.mode === 'decisions') {
      return 'EN_REVUE';
    }
    return '';
  }
}
