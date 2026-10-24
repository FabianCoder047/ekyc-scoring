import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { Agence, Critere, Role, UtilisateurAdmin } from './models';

export interface DemandeUtilisateur {
  email?: string;
  motDePasse?: string;
  nom: string;
  prenom: string;
  role: Role;
  agenceId?: number | null;
  actif?: boolean;
  nouveauMotDePasse?: string | null;
}

export interface DemandeCritere {
  critere: string;
  borneMin?: number | null;
  borneMax?: number | null;
  points: number;
  libelle: string;
  actif: boolean;
}

@Injectable({ providedIn: 'root' })
export class AdminApiService {
  private readonly http = inject(HttpClient);

  utilisateurs(): Observable<UtilisateurAdmin[]> {
    return this.http.get<UtilisateurAdmin[]>('/api/admin/utilisateurs');
  }

  agences(): Observable<Agence[]> {
    return this.http.get<Agence[]>('/api/admin/agences');
  }

  creerUtilisateur(demande: DemandeUtilisateur): Observable<UtilisateurAdmin> {
    return this.http.post<UtilisateurAdmin>('/api/admin/utilisateurs', demande);
  }

  modifierUtilisateur(id: number, demande: DemandeUtilisateur): Observable<UtilisateurAdmin> {
    return this.http.put<UtilisateurAdmin>(`/api/admin/utilisateurs/${id}`, demande);
  }

  desactiverUtilisateur(id: number): Observable<void> {
    return this.http.delete<void>(`/api/admin/utilisateurs/${id}`);
  }

  criteres(): Observable<Critere[]> {
    return this.http.get<Critere[]>('/api/admin/criteres-scoring');
  }

  creerCritere(demande: DemandeCritere): Observable<Critere> {
    return this.http.post<Critere>('/api/admin/criteres-scoring', demande);
  }

  modifierCritere(id: number, demande: DemandeCritere): Observable<Critere> {
    return this.http.put<Critere>(`/api/admin/criteres-scoring/${id}`, demande);
  }

  supprimerCritere(id: number): Observable<void> {
    return this.http.delete<void>(`/api/admin/criteres-scoring/${id}`);
  }
}
