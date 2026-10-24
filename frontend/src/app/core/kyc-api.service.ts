import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import {
  Client,
  DossierCreation,
  DossierDetail,
  DossierResume,
  Historique,
  PageResponse,
  ResultatScoring,
  StatutDossier,
  TypeDocument,
} from './models';

@Injectable({ providedIn: 'root' })
export class KycApiService {
  private readonly http = inject(HttpClient);

  clients(): Observable<Client[]> {
    return this.http.get<Client[]>('/api/kyc/clients');
  }

  lister(statut: StatutDossier | null, q: string, page = 0, taille = 50): Observable<PageResponse<DossierResume>> {
    let params = new HttpParams().set('page', page).set('taille', taille);
    if (statut) {
      params = params.set('statut', statut);
    }
    if (q.trim()) {
      params = params.set('q', q.trim());
    }
    return this.http.get<PageResponse<DossierResume>>('/api/kyc/dossiers', { params });
  }

  detail(id: number): Observable<DossierDetail> {
    return this.http.get<DossierDetail>(`/api/kyc/dossiers/${id}`);
  }

  creer(demande: DossierCreation): Observable<DossierDetail> {
    return this.http.post<DossierDetail>('/api/kyc/dossiers', demande);
  }

  modifier(id: number, demande: Omit<DossierCreation, 'clientId'>): Observable<DossierDetail> {
    return this.http.put<DossierDetail>(`/api/kyc/dossiers/${id}`, demande);
  }

  soumettre(id: number): Observable<DossierDetail> {
    return this.http.post<DossierDetail>(`/api/kyc/dossiers/${id}/soumission`, {});
  }

  prendreEnCharge(id: number): Observable<DossierDetail> {
    return this.http.post<DossierDetail>(`/api/kyc/dossiers/${id}/prise-en-charge`, {});
  }

  scorer(id: number): Observable<ResultatScoring> {
    return this.http.post<ResultatScoring>(`/api/kyc/dossiers/${id}/scoring`, {});
  }

  decider(id: number, decision: 'VALIDE' | 'REJETE', motif?: string): Observable<DossierDetail> {
    return this.http.post<DossierDetail>(`/api/kyc/dossiers/${id}/decision`, { decision, motif });
  }

  historique(id: number): Observable<Historique> {
    return this.http.get<Historique>(`/api/kyc/dossiers/${id}/historique`);
  }

  deposerDocument(id: number, type: TypeDocument, fichier: File): Observable<unknown> {
    const donnees = new FormData();
    donnees.append('type', type);
    donnees.append('fichier', fichier);
    return this.http.post(`/api/kyc/dossiers/${id}/documents`, donnees);
  }

  contenuDocument(dossierId: number, documentId: number): Observable<Blob> {
    return this.http.get(`/api/kyc/dossiers/${dossierId}/documents/${documentId}/contenu`, {
      responseType: 'blob',
    });
  }
}
