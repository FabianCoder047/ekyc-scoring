import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { AuthService, accueilRole } from '../../core/auth.service';
import { messageErreur } from '../../core/notification.service';
import { LIBELLES_ROLE } from '../../core/libelles';
import { Role } from '../../core/models';

interface CompteDemo {
  role: Role;
  email: string;
  description: string;
}

const COMPTES_DEMO: CompteDemo[] = [
  {
    role: 'AGENT_ACCUEIL',
    email: 'agent@sfd.tg',
    description: 'Crée et soumet les dossiers clients',
  },
  {
    role: 'ANALYSTE_KYC',
    email: 'analyste@sfd.tg',
    description: 'Instruit les dossiers et calcule le scoring',
  },
  {
    role: 'SUPERVISEUR',
    email: 'superviseur@sfd.tg',
    description: 'Décide et consulte le journal d’audit',
  },
  {
    role: 'ADMIN',
    email: 'admin@sfd.tg',
    description: 'Gère les comptes et les critères de scoring',
  },
];

@Component({
  selector: 'app-login-page',
  imports: [
    ReactiveFormsModule,
    MatButtonModule,
    MatCardModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './login.page.html',
  styleUrl: './login.page.scss',
})
export class LoginPage {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly fb = inject(FormBuilder);

  protected readonly comptesDemo = COMPTES_DEMO;
  protected readonly libelleRole = (role: Role): string => LIBELLES_ROLE[role];
  protected readonly chargement = signal(false);
  protected readonly erreur = signal<string | null>(null);
  protected readonly motDePasseVisible = signal(false);

  protected readonly form = this.fb.nonNullable.group({
    email: this.fb.nonNullable.control('', [Validators.required, Validators.email]),
    motDePasse: this.fb.nonNullable.control('', Validators.required),
  });

  constructor() {
    const utilisateur = this.auth.utilisateur();
    if (utilisateur) {
      void this.router.navigateByUrl(accueilRole(utilisateur.role));
    }
  }

  protected utiliserCompte(compte: CompteDemo): void {
    this.form.setValue({ email: compte.email, motDePasse: 'Demo123456!' });
    this.erreur.set(null);
  }

  protected basculerMotDePasse(): void {
    this.motDePasseVisible.update((visible) => !visible);
  }

  protected connecter(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.chargement.set(true);
    this.erreur.set(null);

    const { email, motDePasse } = this.form.getRawValue();
    this.auth.login(email.trim(), motDePasse).subscribe({
      next: (session) => {
        this.chargement.set(false);
        const retour = this.route.snapshot.queryParamMap.get('retour');
        const destination =
          retour && retour.startsWith('/') && !retour.startsWith('//')
            ? retour
            : accueilRole(session.utilisateur.role);
        void this.router.navigateByUrl(destination);
      },
      error: (erreur) => {
        this.chargement.set(false);
        this.erreur.set(messageErreur(erreur));
      },
    });
  }
}
