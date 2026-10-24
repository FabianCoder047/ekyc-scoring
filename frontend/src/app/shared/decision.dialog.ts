import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatRadioModule } from '@angular/material/radio';
import { DossierDetail } from '../core/models';

export interface DecisionSaisie {
  decision: 'VALIDE' | 'REJETE';
  motif?: string;
}

@Component({
  selector: 'app-decision-dialog',
  imports: [
    MatDialogModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatRadioModule,
    ReactiveFormsModule,
  ],
  template: `
    <h2 mat-dialog-title>Décision finale</h2>
    <mat-dialog-content>
      <p class="rappel">
        Dossier <strong>#{{ data.id }}</strong> — {{ data.clientPrenom }} {{ data.clientNom }}
      </p>

      <form [formGroup]="form">
        <mat-radio-group formControlName="decision" class="choix" aria-label="Décision">
          <mat-radio-button value="VALIDE">Valider le dossier</mat-radio-button>
          <mat-radio-button value="REJETE">Rejeter le dossier</mat-radio-button>
        </mat-radio-group>

        <mat-form-field appearance="outline" class="pleine-largeur">
          <mat-label>Motif {{ form.controls.decision.value === 'REJETE' ? '(obligatoire)' : '(facultatif)' }}</mat-label>
          <textarea
            matInput
            formControlName="motif"
            rows="3"
            maxlength="500"
            placeholder="Précisez la raison de la décision…"
          ></textarea>
          <mat-hint align="end">{{ form.controls.motif.value.length }}/500</mat-hint>
        </mat-form-field>
      </form>

      @if (erreur()) {
        <p class="erreur">{{ erreur() }}</p>
      }

      <p class="quatre-yeux">
        Principe des quatre yeux : le superviseur qui décide ne doit être ni l’agent ayant soumis
        le dossier, ni l’analyste l’ayant pris en charge.
      </p>
    </mat-dialog-content>

    <mat-dialog-actions align="end">
      <button mat-button type="button" (click)="annuler()">Annuler</button>
      <button mat-flat-button type="button" color="primary" (click)="valider()">
        Confirmer la décision
      </button>
    </mat-dialog-actions>
  `,
  styles: [
    `
      .rappel {
        margin: 0 0 16px;
        color: #3c4b5e;
      }

      .choix {
        display: flex;
        flex-direction: column;
        gap: 8px;
        margin-bottom: 16px;
      }

      .pleine-largeur {
        width: 100%;
      }

      .erreur {
        color: #a11d2b;
        font-size: 13px;
        margin: 0 0 8px;
      }

      .quatre-yeux {
        margin: 4px 0 0;
        padding: 10px 12px;
        border-radius: 8px;
        background: #fff4d6;
        color: #6b4a00;
        font-size: 12px;
        line-height: 1.45;
      }
    `,
  ],
})
export class DecisionDialog {
  protected readonly data = inject<DossierDetail>(MAT_DIALOG_DATA);
  private readonly dialogRef = inject(MatDialogRef<DecisionDialog, DecisionSaisie>);
  private readonly fb = inject(FormBuilder);

  protected readonly erreur = signal<string | null>(null);

  protected readonly form = this.fb.nonNullable.group({
    decision: this.fb.nonNullable.control<'VALIDE' | 'REJETE'>('VALIDE', Validators.required),
    motif: this.fb.nonNullable.control('', Validators.maxLength(500)),
  });

  protected annuler(): void {
    this.dialogRef.close();
  }

  protected valider(): void {
    const { decision, motif } = this.form.getRawValue();
    const motifNettoye = motif.trim();
    if (decision === 'REJETE' && !motifNettoye) {
      this.erreur.set('Un motif est obligatoire pour rejeter un dossier.');
      return;
    }
    this.dialogRef.close({
      decision,
      motif: motifNettoye || undefined,
    });
  }
}
