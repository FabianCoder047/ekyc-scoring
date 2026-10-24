import { HttpClient } from '@angular/common/http';
import { computed, inject, Injectable, signal } from '@angular/core';
import { Router } from '@angular/router';
import { Observable, shareReplay, tap, throwError } from 'rxjs';
import { Role, Session, Utilisateur } from './models';

const CLE_SESSION = 'kyc.session';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);

  readonly session = signal<Session | null>(this.lireSession());
  readonly utilisateur = computed<Utilisateur | null>(() => this.session()?.utilisateur ?? null);
  readonly connecte = computed(() => this.session() !== null);

  private refreshEnCours?: Observable<Session>;
  private minuterie?: ReturnType<typeof setTimeout>;

  constructor() {
    this.planifierRenouvellement();
  }

  login(email: string, motDePasse: string): Observable<Session> {
    return this.http
      .post<Session>('/api/auth/login', { email, password: motDePasse })
      .pipe(tap((session) => this.enregistrer(session)));
  }

  rafraichir(): Observable<Session> {
    if (!this.refreshEnCours) {
      const refreshToken = this.session()?.refreshToken;
      if (!refreshToken) {
        return throwError(() => new Error('Session expirée'));
      }
      this.refreshEnCours = this.http
        .post<Session>('/api/auth/refresh', { refreshToken })
        .pipe(
          tap((session) => this.enregistrer(session)),
          shareReplay(1),
        );
      this.refreshEnCours.subscribe({
        error: () => {
          this.refreshEnCours = undefined;
          this.deconnecter();
        },
        complete: () => (this.refreshEnCours = undefined),
      });
    }
    return this.refreshEnCours;
  }

  seDeconnecter(): void {
    const session = this.session();
    if (!session) {
      this.deconnecter();
      return;
    }
    this.http
      .post<void>('/api/auth/logout', { refreshToken: session.refreshToken })
      .subscribe({ complete: () => this.deconnecter(), error: () => this.deconnecter() });
  }

  deconnecter(rediriger = true): void {
    this.effacer();
    if (rediriger && !this.router.url.startsWith('/login')) {
      void this.router.navigate(['/login']);
    }
  }

  aUnRole(...roles: Role[]): boolean {
    const utilisateur = this.utilisateur();
    return utilisateur !== null && roles.includes(utilisateur.role);
  }

  private enregistrer(session: Session): void {
    this.session.set(session);
    sessionStorage.setItem(CLE_SESSION, JSON.stringify(session));
    this.planifierRenouvellement();
  }

  private effacer(): void {
    this.session.set(null);
    sessionStorage.removeItem(CLE_SESSION);
    if (this.minuterie) {
      clearTimeout(this.minuterie);
      this.minuterie = undefined;
    }
  }

  private lireSession(): Session | null {
    const brut = sessionStorage.getItem(CLE_SESSION);
    if (!brut) {
      return null;
    }
    try {
      return JSON.parse(brut) as Session;
    } catch {
      sessionStorage.removeItem(CLE_SESSION);
      return null;
    }
  }

  private planifierRenouvellement(): void {
    if (this.minuterie) {
      clearTimeout(this.minuterie);
    }
    const session = this.session();
    if (!session) {
      return;
    }
    const delai = Math.max(30, session.expiresIn * 0.8) * 1000;
    this.minuterie = setTimeout(() => {
      this.rafraichir().subscribe({ error: () => this.deconnecter() });
    }, delai);
  }
}

export function accueilRole(role: Role): string {
  switch (role) {
    case 'AGENT_ACCUEIL':
      return '/agent';
    case 'ANALYSTE_KYC':
      return '/analyste';
    case 'SUPERVISEUR':
      return '/superviseur';
    case 'ADMIN':
      return '/admin/utilisateurs';
  }
}
