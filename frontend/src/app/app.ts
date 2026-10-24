import { Component, computed, inject } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { AuthService } from './core/auth.service';
import { LIBELLES_ROLE, initiales } from './core/libelles';
import { Role } from './core/models';

interface LienNavigation {
  libelle: string;
  url: string;
}

const LIENS_PAR_ROLE: Record<Role, LienNavigation[]> = {
  AGENT_ACCUEIL: [
    { libelle: 'Dossiers', url: '/agent/dossiers' },
    { libelle: 'Nouveau dossier', url: '/agent/dossiers/nouveau' },
  ],
  ANALYSTE_KYC: [
    { libelle: 'File d’attente', url: '/analyste/file' },
    { libelle: 'Tous les dossiers', url: '/analyste/dossiers' },
  ],
  SUPERVISEUR: [
    { libelle: 'Décisions', url: '/superviseur/decisions' },
    { libelle: 'Dossiers', url: '/superviseur/dossiers' },
    { libelle: 'Audit', url: '/superviseur/audit' },
  ],
  ADMIN: [
    { libelle: 'Utilisateurs', url: '/admin/utilisateurs' },
    { libelle: 'Critères de scoring', url: '/admin/criteres' },
  ],
};

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, RouterLink, RouterLinkActive, MatIconModule, MatButtonModule],
  templateUrl: './app.html',
  styleUrl: './app.scss',
})
export class App {
  protected readonly auth = inject(AuthService);

  protected readonly liens = computed<LienNavigation[]>(() => {
    const role = this.auth.utilisateur()?.role;
    return role ? LIENS_PAR_ROLE[role] : [];
  });

  protected readonly libelleRole = (role: Role): string => LIBELLES_ROLE[role];
  protected readonly initiales = initiales;

  protected deconnexion(): void {
    this.auth.seDeconnecter();
  }
}
