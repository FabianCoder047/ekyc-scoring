export type Role = 'AGENT_ACCUEIL' | 'ANALYSTE_KYC' | 'SUPERVISEUR' | 'ADMIN';

export type StatutDossier = 'BROUILLON' | 'SOUMIS' | 'EN_REVUE' | 'VALIDE' | 'REJETE';

export type TypeDocument = 'RECTO_PIECE' | 'VERSO_PIECE' | 'JUSTIFICATIF_DOMICILE';

export type TypePiece = 'CNI' | 'PASSEPORT' | 'CARTE_CONSULAIRE';

export type NiveauRisque = 'FAIBLE' | 'MOYEN' | 'ELEVE';

export type PalierScoring = 'FAVORABLE' | 'A_ETUDIER' | 'DEFAVORABLE';

export interface Utilisateur {
  id: number;
  email: string;
  nom: string;
  prenom: string;
  role: Role;
  agenceId?: number;
  agenceNom?: string;
}

export interface Session {
  accessToken: string;
  refreshToken: string;
  expiresIn: number;
  utilisateur: Utilisateur;
}

export interface Client {
  id: number;
  nom: string;
  prenom: string;
  telephone?: string;
}

export interface UtilisateurRef {
  id: number;
  nom: string;
  prenom: string;
}

export interface DocumentKyc {
  id: number;
  type: TypeDocument;
  nomOriginal: string;
  mimeType: string;
  tailleOctets: number;
  deposeLe: string;
  remplacePar?: number;
}

export interface DossierResume {
  id: number;
  clientId: number;
  clientNom: string;
  clientPrenom: string;
  agenceId: number;
  agenceNom: string;
  version: number;
  statut: StatutDossier;
  typePiece: TypePiece;
  numeroPieceMasque: string;
  dateExpirationPiece: string;
  niveauRisque?: NiveauRisque | null;
  creeLe: string;
  soumisLe?: string;
}

export interface DossierDetail {
  id: number;
  clientId: number;
  clientNom: string;
  clientPrenom: string;
  agenceId: number;
  agenceNom: string;
  version: number;
  statut: StatutDossier;
  typePiece: TypePiece;
  numeroPiece?: string;
  numeroPieceMasque: string;
  dateExpirationPiece: string;
  revenuMensuel?: number;
  chargesMensuelles?: number;
  ancienneteActiviteMois?: number;
  nbPersonnesCharge?: number;
  niveauRisque?: NiveauRisque | null;
  creePar: UtilisateurRef;
  soumisPar?: UtilisateurRef;
  soumisLe?: string;
  assigneA?: UtilisateurRef;
  decidePar?: UtilisateurRef;
  decideLe?: string;
  motifRejet?: string;
  creeLe: string;
  documents: DocumentKyc[];
}

export interface DetailCritere {
  critere: string;
  valeur?: number;
  points: number;
  libelle: string;
  borneMin?: number;
  borneMax?: number;
}

export interface ResultatScoring {
  id: number;
  score: number;
  palier: PalierScoring;
  detail: DetailCritere[];
  calculePar: UtilisateurRef;
  calculeLe: string;
}

export interface DecisionKyc {
  id: number;
  etape: string;
  decision: string;
  commentaire?: string;
  decideur: UtilisateurRef;
  creeLe: string;
}

export interface Historique {
  dossierId: number;
  decisions: DecisionKyc[];
  scorings: ResultatScoring[];
}

export interface PageResponse<T> {
  contenu: T[];
  page: number;
  taille: number;
  total: number;
}

export interface AuditEntree {
  id: number;
  horodatage: string;
  utilisateurId?: number;
  role?: string;
  action: string;
  entiteType: string;
  entiteId?: number;
  avant?: unknown;
  apres?: unknown;
  adresseIp?: string;
  userAgent?: string;
  hashPrecedent: string;
  hash: string;
}

export interface VerificationAudit {
  valide: boolean;
  premiereLigneInvalide?: number;
  lignesVerifiees: number;
}

export interface UtilisateurAdmin {
  id: number;
  email: string;
  nom: string;
  prenom: string;
  role: Role;
  agenceId?: number;
  agenceNom?: string;
  actif: boolean;
  creeLe: string;
}

export interface Agence {
  id: number;
  code: string;
  nom: string;
}

export interface Critere {
  id: number;
  critere: string;
  borneMin?: number;
  borneMax?: number;
  points: number;
  libelle: string;
  actif: boolean;
}

export interface DossierCreation {
  clientId?: number;
  typePiece: TypePiece;
  numeroPiece: string;
  dateExpirationPiece: string;
  revenuMensuel?: number | null;
  chargesMensuelles?: number | null;
  ancienneteActiviteMois?: number | null;
  nbPersonnesCharge?: number | null;
}

export interface Probleme {
  code?: string;
  detail?: string;
  status?: number;
  erreurs?: { champ: string; message: string }[];
}
