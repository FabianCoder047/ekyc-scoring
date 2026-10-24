import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { AuditEntree, PageResponse, VerificationAudit } from './models';

export interface FiltresAudit {
  entite?: string;
  action?: string;
  du?: string;
  au?: string;
  page: number;
  taille: number;
}

@Injectable({ providedIn: 'root' })
export class AuditApiService {
  private readonly http = inject(HttpClient);

  lister(filtres: FiltresAudit): Observable<PageResponse<AuditEntree>> {
    let params = new HttpParams().set('page', filtres.page).set('taille', filtres.taille);
    if (filtres.entite) {
      params = params.set('entite', filtres.entite);
    }
    if (filtres.action) {
      params = params.set('action', filtres.action);
    }
    if (filtres.du) {
      params = params.set('du', filtres.du);
    }
    if (filtres.au) {
      params = params.set('au', filtres.au);
    }
    return this.http.get<PageResponse<AuditEntree>>('/api/audit', { params });
  }

  verifier(): Observable<VerificationAudit> {
    return this.http.get<VerificationAudit>('/api/audit/verification');
  }
}
