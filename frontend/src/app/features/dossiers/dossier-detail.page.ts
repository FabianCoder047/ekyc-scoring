import {
  Component,
  ElementRef,
  computed,
  inject,
  signal,
  viewChild,
} from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatDialog } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';
import { AuthService } from '../../core/auth.service';
import { KycApiService } from '../../core/kyc-api.service';
import { NotificationService } from '../../core/notification.service';
import {
  LIBELLES_CRITERE,
  LIBELLES_PALIER,
  LIBELLES_RISQUE,
  LIBELLES_STATUT,
  LIBELLES_TYPE_DOCUMENT,
  LIBELLES_TYPE_PIECE,
  classeRisque,
  classeStatut,
  formatDate,
  formatDateHeure,
  formatMontant,
  formatOctets,
} from '../../core/libelles';
import {
  DocumentKyc,
  DossierDetail,
  Historique,
  PalierScoring,
  ResultatScoring,
  TypeDocument,
} from '../../core/models';
import { ConfirmationDialog } from '../../shared/confirmation.dialog';
import { DecisionDialog, DecisionSaisie } from '../../shared/decision.dialog';

@Component({
  selector: 'app-dossier-detail-page',
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
  templateUrl: './dossier-detail.page.html',
  styleUrl: './dossier-detail.page.scss',
})
export class DossierDetailPage {
  private readonly api = inject(KycApiService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly auth = inject(AuthService);
  private readonly notifications = inject(NotificationService);
  private readonly dialog = inject(MatDialog);
  private readonly fb = inject(FormBuilder);

  private readonly dossierId = Number(this.route.snapshot.paramMap.get('id'));

  protected readonly dossier = signal<DossierDetail | null>(null);
  protected readonly historique = signal<Historique | null>(null);
  protected readonly dernierScoring = signal<ResultatScoring | null>(null);
  protected readonly chargement = signal(true);
  protected readonly action = signal<string | null>(null);
  protected readonly fichier = signal<File | null>(null);

  protected readonly typesDocument = Object.entries(LIBELLES_TYPE_DOCUMENT) as [
    TypeDocument,
    string,
  ][];
  protected readonly libellesStatut = LIBELLES_STATUT;
  protected readonly libellesRisque = LIBELLES_RISQUE;
  protected readonly libellesTypePiece = LIBELLES_TYPE_PIECE;
  protected readonly libellesTypeDocument = LIBELLES_TYPE_DOCUMENT;
  protected readonly libellesPalier = LIBELLES_PALIER;
  protected readonly libellesCritere = LIBELLES_CRITERE;
  protected readonly classeStatut = classeStatut;
  protected readonly classeRisque = classeRisque;
  protected readonly formatDate = formatDate;
  protected readonly formatDateHeure = formatDateHeure;
  protected readonly formatMontant = formatMontant;
  protected readonly formatOctets = formatOctets;

  protected readonly libellesEtape: Record<string, string> = {
    PRISE_EN_CHARGE: 'Prise en charge',
    ANALYSE: 'Analyse KYC',
    VALIDATION: 'Décision finale',
  };

  protected readonly libellesDecision: Record<string, string> = {
    ASSIGNE: 'Assigné',
    RECOMMANDE: 'Recommandation',
    VALIDE: 'Validé',
    REJETE: 'Rejeté',
  };

  protected readonly formulaireDepot = this.fb.nonNullable.group({
    type: this.fb.nonNullable.control<TypeDocument>('RECTO_PIECE', Validators.required),
  });

  private readonly champFichier = viewChild<ElementRef<HTMLInputElement>>('champFichier');

  protected readonly estAgent = computed(() => this.auth.aUnRole('AGENT_ACCUEIL'));
  protected readonly estAnalyste = computed(() => this.auth.aUnRole('ANALYSTE_KYC'));
  protected readonly estSuperviseur = computed(() => this.auth.aUnRole('SUPERVISEUR'));

  protected readonly proprietaire = computed(() => {
    const dossier = this.dossier();
    const utilisateur = this.auth.utilisateur();
    return !!dossier && !!utilisateur && dossier.creePar?.id === utilisateur.id;
  });

  protected readonly peutModifier = computed(() => {
    const dossier = this.dossier();
    return (
      this.estAgent() &&
      this.proprietaire() &&
      !!dossier &&
      (dossier.statut === 'BROUILLON' || dossier.statut === 'REJETE')
    );
  });

  protected readonly peutSoumettre = computed(
    () => this.estAgent() && this.proprietaire() && this.dossier()?.statut === 'BROUILLON',
  );

  protected readonly peutPrendreEnCharge = computed(
    () => this.estAnalyste() && this.dossier()?.statut === 'SOUMIS',
  );

  protected readonly estAssigne = computed(() => {
    const dossier = this.dossier();
    const utilisateur = this.auth.utilisateur();
    return !!dossier?.assigneA && dossier.assigneA.id === utilisateur?.id;
  });

  protected readonly peutScorer = computed(
    () => this.estAnalyste() && this.dossier()?.statut === 'EN_REVUE' && this.estAssigne(),
  );

  protected readonly peutDecider = computed(
    () => this.estSuperviseur() && this.dossier()?.statut === 'EN_REVUE',
  );

  protected readonly peutTelecharger = computed(
    () => this.estSuperviseur() || (this.estAnalyste() && this.estAssigne()),
  );

  protected readonly peutDeposer = computed(
    () => this.estAgent() && this.proprietaire() && this.dossier()?.statut === 'BROUILLON',
  );

  protected readonly peutVoirHistorique = computed(
    () => this.estAnalyste() || this.estSuperviseur(),
  );

  protected readonly documentsActifs = computed(() =>
    (this.dossier()?.documents ?? []).filter((document) => !document.remplacePar),
  );

  protected readonly manqueRecto = computed(
    () => !this.documentsActifs().some((document) => document.type === 'RECTO_PIECE'),
  );

  protected readonly manqueVerso = computed(
    () => !this.documentsActifs().some((document) => document.type === 'VERSO_PIECE'),
  );

  constructor() {
    this.charger();
  }

  protected charger(): void {
    this.chargement.set(true);
    this.api.detail(this.dossierId).subscribe({
      next: (dossier) => {
        this.dossier.set(dossier);
        this.chargement.set(false);
        if (this.estAnalyste() || this.estSuperviseur()) {
          this.chargerHistorique();
        }
      },
      error: (erreur) => {
        this.chargement.set(false);
        this.notifications.erreur(erreur);
        void this.router.navigate(['/']);
      },
    });
  }

  protected soumettre(): void {
    this.action.set('soumission');
    this.api.soumettre(this.dossierId).subscribe({
      next: (dossier) => {
        this.action.set(null);
        this.dossier.set(dossier);
        this.notifications.succes('Dossier soumis à l’analyse KYC.');
      },
      error: (erreur) => {
        this.action.set(null);
        this.notifications.erreur(erreur);
      },
    });
  }

  protected prendreEnCharge(): void {
    this.action.set('prise');
    this.api.prendreEnCharge(this.dossierId).subscribe({
      next: (dossier) => {
        this.action.set(null);
        this.dossier.set(dossier);
        this.notifications.succes('Dossier pris en charge. Vous pouvez calculer le scoring.');
        this.chargerHistorique();
      },
      error: (erreur) => {
        this.action.set(null);
        this.notifications.erreur(erreur);
        this.charger();
      },
    });
  }

  protected calculerScoring(): void {
    this.action.set('scoring');
    this.api.scorer(this.dossierId).subscribe({
      next: (resultat) => {
        this.action.set(null);
        this.dernierScoring.set(resultat);
        this.notifications.succes(`Scoring calculé : ${resultat.score}/100.`);
        this.chargerHistorique();
        this.chargerDossierSeul();
      },
      error: (erreur) => {
        this.action.set(null);
        this.notifications.erreur(erreur);
      },
    });
  }

  protected ouvrirDecision(): void {
    const dossier = this.dossier();
    if (!dossier) {
      return;
    }
    this.dialog
      .open<DecisionDialog, DossierDetail, DecisionSaisie>(DecisionDialog, {
        data: dossier,
        width: '520px',
        disableClose: true,
      })
      .afterClosed()
      .subscribe((saisie) => {
        if (!saisie) {
          return;
        }
        this.action.set('decision');
        this.api.decider(this.dossierId, saisie.decision, saisie.motif).subscribe({
          next: () => {
            this.action.set(null);
            this.notifications.succes(
              saisie.decision === 'VALIDE' ? 'Dossier validé.' : 'Dossier rejeté.',
            );
            this.charger();
          },
          error: (erreur) => {
            this.action.set(null);
            this.notifications.erreur(erreur);
          },
        });
      });
  }

  protected surFichier(evenement: Event): void {
    const input = evenement.target as HTMLInputElement;
    this.fichier.set(input.files && input.files.length > 0 ? input.files[0] : null);
  }

  protected deposerDocument(): void {
    const fichier = this.fichier();
    if (!fichier) {
      this.notifications.info('Sélectionnez un fichier à déposer.');
      return;
    }
    this.action.set('depot');
    this.api.deposerDocument(this.dossierId, this.formulaireDepot.getRawValue().type, fichier).subscribe({
      next: () => {
        this.action.set(null);
        this.fichier.set(null);
        const champ = this.champFichier();
        if (champ) {
          champ.nativeElement.value = '';
        }
        this.notifications.succes('Document déposé et chiffré.');
        this.chargerDossierSeul();
      },
      error: (erreur) => {
        this.action.set(null);
        this.notifications.erreur(erreur);
      },
    });
  }

  protected telecharger(document: DocumentKyc): void {
    this.api.contenuDocument(this.dossierId, document.id).subscribe({
      next: (blob) => {
        const url = URL.createObjectURL(blob);
        window.open(url, '_blank', 'noopener');
        window.setTimeout(() => URL.revokeObjectURL(url), 60_000);
      },
      error: (erreur) => this.notifications.erreur(erreur),
    });
  }

  protected confirmerSoumission(): void {
    const dossier = this.dossier();
    if (!dossier) {
      return;
    }
    this.dialog
      .open<ConfirmationDialog, unknown, boolean>(ConfirmationDialog, {
        data: {
          titre: 'Soumettre le dossier',
          message:
            'Le dossier sera transmis à l’analyse KYC et ne sera plus modifiable par l’agence. Confirmez-vous la soumission ?',
          libelleConfirmer: 'Soumettre',
        },
        width: '460px',
      })
      .afterClosed()
      .subscribe((confirme) => {
        if (confirme) {
          this.soumettre();
        }
      });
  }

  protected classePalier(palier: string): string {
    return `score-valeur score-${palier.toLowerCase()}`;
  }

  protected classePalierBadge(palier: PalierScoring): string {
    switch (palier) {
      case 'FAVORABLE':
        return 'badge badge-valide';
      case 'A_ETUDIER':
        return 'badge badge-en_revue';
      case 'DEFAVORABLE':
        return 'badge badge-rejete';
      default:
        return 'badge badge-brouillon';
    }
  }

  protected classeDecision(decision: string): string {
    switch (decision) {
      case 'VALIDE':
        return 'badge badge-valide';
      case 'REJETE':
        return 'badge badge-rejete';
      case 'ASSIGNE':
        return 'badge badge-soumis';
      case 'RECOMMANDE':
        return 'badge badge-en_revue';
      default:
        return 'badge badge-brouillon';
    }
  }

  private chargerHistorique(): void {
    this.api.historique(this.dossierId).subscribe({
      next: (historique) => {
        this.historique.set(historique);
        this.dernierScoring.set(historique.scorings[0] ?? null);
      },
      error: () => {
        // L’historique est réservé aux analystes et superviseurs.
      },
    });
  }

  private chargerDossierSeul(): void {
    this.api.detail(this.dossierId).subscribe({
      next: (dossier) => this.dossier.set(dossier),
      error: (erreur) => this.notifications.erreur(erreur),
    });
  }
}
