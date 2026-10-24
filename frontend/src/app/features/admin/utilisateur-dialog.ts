import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { DemandeUtilisateur } from '../../core/admin-api.service';
import { LIBELLES_ROLE } from '../../core/libelles';
import { Agence, Role, UtilisateurAdmin } from '../../core/models';

export interface UtilisateurDialogData {
  utilisateur?: UtilisateurAdmin;
  agences: Agence[];
}

@Component({
  selector: 'app-utilisateur-dialog',
  imports: [
    ReactiveFormsModule,
    MatDialogModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatSlideToggleModule,
  ],
  templateUrl: './utilisateur-dialog.html',
  styleUrl: './utilisateur-dialog.scss',
})
export class UtilisateurDialog {
  protected readonly data = inject<UtilisateurDialogData>(MAT_DIALOG_DATA);
  private readonly dialogRef = inject(MatDialogRef<UtilisateurDialog, DemandeUtilisateur>);
  private readonly fb = inject(FormBuilder);

  protected readonly creation = !this.data.utilisateur;
  protected readonly roles = Object.entries(LIBELLES_ROLE) as [Role, string][];
  protected readonly erreur = signal<string | null>(null);

  protected readonly form = this.fb.nonNullable.group({
    email: this.fb.nonNullable.control(
      this.data.utilisateur?.email ?? '',
      this.creation ? [Validators.required, Validators.email] : [],
    ),
    motDePasse: this.fb.nonNullable.control(
      '',
      this.creation ? [Validators.required, Validators.minLength(10)] : [],
    ),
    nom: this.fb.nonNullable.control(this.data.utilisateur?.nom ?? '', Validators.required),
    prenom: this.fb.nonNullable.control(this.data.utilisateur?.prenom ?? '', Validators.required),
    role: this.fb.nonNullable.control<Role>(this.data.utilisateur?.role ?? 'AGENT_ACCUEIL', Validators.required),
    agenceId: this.fb.control<number | null>(this.data.utilisateur?.agenceId ?? null),
    actif: this.fb.nonNullable.control(this.data.utilisateur?.actif ?? true),
    nouveauMotDePasse: this.fb.nonNullable.control('', Validators.minLength(10)),
  });

  protected annuler(): void {
    this.dialogRef.close();
  }

  protected valider(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const valeurs = this.form.getRawValue();
    if (valeurs.role === 'AGENT_ACCUEIL' && valeurs.agenceId === null) {
      this.erreur.set('Une agence est obligatoire pour un agent d’accueil.');
      return;
    }

    if (this.creation) {
      this.dialogRef.close({
        email: valeurs.email.trim(),
        motDePasse: valeurs.motDePasse,
        nom: valeurs.nom.trim(),
        prenom: valeurs.prenom.trim(),
        role: valeurs.role,
        agenceId: valeurs.agenceId,
      });
      return;
    }

    this.dialogRef.close({
      nom: valeurs.nom.trim(),
      prenom: valeurs.prenom.trim(),
      role: valeurs.role,
      agenceId: valeurs.agenceId,
      actif: valeurs.actif,
      nouveauMotDePasse: valeurs.nouveauMotDePasse.trim() || null,
    });
  }
}
