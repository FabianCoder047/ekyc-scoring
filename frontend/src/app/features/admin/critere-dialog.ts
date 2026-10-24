import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { DemandeCritere } from '../../core/admin-api.service';
import { LIBELLES_CRITERE } from '../../core/libelles';
import { Critere } from '../../core/models';

export interface CritereDialogData {
  critere?: Critere;
}

@Component({
  selector: 'app-critere-dialog',
  imports: [
    ReactiveFormsModule,
    MatDialogModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatSlideToggleModule,
  ],
  templateUrl: './critere-dialog.html',
  styleUrl: './critere-dialog.scss',
})
export class CritereDialog {
  protected readonly data = inject<CritereDialogData>(MAT_DIALOG_DATA);
  private readonly dialogRef = inject(MatDialogRef<CritereDialog, DemandeCritere>);
  private readonly fb = inject(FormBuilder);

  protected readonly creation = !this.data.critere;
  protected readonly criteres = Object.entries(LIBELLES_CRITERE) as [string, string][];
  protected readonly erreur = signal<string | null>(null);

  protected readonly form = this.fb.nonNullable.group({
    critere: this.fb.nonNullable.control(this.data.critere?.critere ?? 'RATIO_ENDETTEMENT', Validators.required),
    borneMin: this.fb.control<number | null>(this.data.critere?.borneMin ?? null),
    borneMax: this.fb.control<number | null>(this.data.critere?.borneMax ?? null),
    points: this.fb.nonNullable.control(this.data.critere?.points ?? 0, [
      Validators.required,
      Validators.min(0),
      Validators.max(25),
    ]),
    libelle: this.fb.nonNullable.control(this.data.critere?.libelle ?? '', [
      Validators.required,
      Validators.maxLength(120),
    ]),
    actif: this.fb.nonNullable.control(this.data.critere?.actif ?? true),
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
    const min = nombreOuNull(valeurs.borneMin);
    const max = nombreOuNull(valeurs.borneMax);
    if (min !== null && max !== null && min > max) {
      this.erreur.set('La borne minimale doit être inférieure ou égale à la borne maximale.');
      return;
    }
    this.dialogRef.close({
      critere: valeurs.critere,
      borneMin: min,
      borneMax: max,
      points: Number(valeurs.points),
      libelle: valeurs.libelle.trim(),
      actif: valeurs.actif,
    });
  }
}

function nombreOuNull(valeur: unknown): number | null {
  if (valeur === null || valeur === undefined || valeur === '') {
    return null;
  }
  const nombre = Number(valeur);
  return Number.isFinite(nombre) ? nombre : null;
}
