import {
  NiveauRisque,
  PalierScoring,
  Role,
  StatutDossier,
  TypeDocument,
  TypePiece,
} from './models';

export const LIBELLES_STATUT: Record<StatutDossier, string> = {
  BROUILLON: 'Brouillon',
  SOUMIS: 'Soumis',
  EN_REVUE: 'En revue',
  VALIDE: 'Validé',
  REJETE: 'Rejeté',
};

export const LIBELLES_ROLE: Record<Role, string> = {
  AGENT_ACCUEIL: 'Agent d’accueil',
  ANALYSTE_KYC: 'Analyste KYC',
  SUPERVISEUR: 'Superviseur',
  ADMIN: 'Administrateur',
};

export const LIBELLES_TYPE_PIECE: Record<TypePiece, string> = {
  CNI: 'Carte nationale d’identité',
  PASSEPORT: 'Passeport',
  CARTE_CONSULAIRE: 'Carte consulaire',
};

export const LIBELLES_RISQUE: Record<NiveauRisque, string> = {
  FAIBLE: 'Risque faible',
  MOYEN: 'Risque moyen',
  ELEVE: 'Risque élevé',
};

export const LIBELLES_TYPE_DOCUMENT: Record<TypeDocument, string> = {
  RECTO_PIECE: 'Recto de la pièce',
  VERSO_PIECE: 'Verso de la pièce',
  JUSTIFICATIF_DOMICILE: 'Justificatif de domicile',
};

export const LIBELLES_PALIER: Record<PalierScoring, string> = {
  FAVORABLE: 'Favorable',
  A_ETUDIER: 'À étudier',
  DEFAVORABLE: 'Défavorable',
};

export const LIBELLES_CRITERE: Record<string, string> = {
  RATIO_ENDETTEMENT: 'Ratio d’endettement',
  ANCIENNETE_ACTIVITE: 'Ancienneté d’activité',
  REVENU_MENSUEL: 'Revenu mensuel',
  HISTORIQUE_REMBOURSEMENT: 'Historique de remboursement',
};

export const ACTIONS_AUDIT = [
  'LOGIN_SUCCES',
  'LOGIN_ECHEC',
  'DOSSIER_CREE',
  'DOSSIER_MODIFIE',
  'DOCUMENT_DEPOSE',
  'DOCUMENT_CONSULTE',
  'DOSSIER_SOUMIS',
  'DOSSIER_ASSIGNE',
  'SCORING_CALCULE',
  'DOSSIER_VALIDE',
  'DOSSIER_REJETE',
  'ACCES_REFUSE',
  'UTILISATEUR_CREE',
  'UTILISATEUR_MODIFIE',
  'CRITERE_CREE',
  'CRITERE_MODIFIE',
  'CRITERE_SUPPRIME',
  'TOKEN_REUTILISE',
];

export const ENTITES_AUDIT = [
  'DOSSIER_KYC',
  'DOCUMENT_KYC',
  'RESULTAT_SCORING',
  'UTILISATEUR',
  'CRITERE_SCORING',
  'CLIENT',
  'ROUTE',
  'AUTH',
];

export function classeStatut(statut: StatutDossier): string {
  return `badge badge-${statut.toLowerCase()}`;
}

export function classeRisque(niveau: NiveauRisque): string {
  return `badge badge-risque-${niveau.toLowerCase()}`;
}

export function formatDate(valeur?: string | null): string {
  if (!valeur) {
    return '—';
  }
  const date = new Date(valeur);
  return Number.isNaN(date.getTime())
    ? '—'
    : new Intl.DateTimeFormat('fr-FR', {
        day: '2-digit',
        month: '2-digit',
        year: 'numeric',
      }).format(date);
}

export function formatDateHeure(valeur?: string | null): string {
  if (!valeur) {
    return '—';
  }
  const date = new Date(valeur);
  return Number.isNaN(date.getTime())
    ? '—'
    : new Intl.DateTimeFormat('fr-FR', {
        day: '2-digit',
        month: '2-digit',
        year: 'numeric',
        hour: '2-digit',
        minute: '2-digit',
      }).format(date);
}

export function formatMontant(valeur?: number | null): string {
  if (valeur === null || valeur === undefined) {
    return '—';
  }
  return new Intl.NumberFormat('fr-FR', { maximumFractionDigits: 0 }).format(valeur) + ' FCFA';
}

export function formatOctets(octets: number): string {
  if (octets < 1024) {
    return `${octets} o`;
  }
  if (octets < 1024 * 1024) {
    return `${(octets / 1024).toFixed(1)} Ko`;
  }
  return `${(octets / (1024 * 1024)).toFixed(1)} Mo`;
}

export function initiales(nom: string, prenom: string): string {
  return `${prenom.charAt(0)}${nom.charAt(0)}`.toUpperCase();
}
