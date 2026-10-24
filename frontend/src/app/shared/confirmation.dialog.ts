import { Component, inject } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';

export interface ConfirmationData {
  titre: string;
  message: string;
  libelleConfirmer?: string;
  danger?: boolean;
}

@Component({
  selector: 'app-confirmation-dialog',
  imports: [MatDialogModule, MatButtonModule],
  template: `
    <h2 mat-dialog-title>{{ data.titre }}</h2>
    <mat-dialog-content>
      <p class="message">{{ data.message }}</p>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button type="button" [mat-dialog-close]="false">Annuler</button>
      <button
        mat-flat-button
        type="button"
        [color]="data.danger ? 'warn' : 'primary'"
        [mat-dialog-close]="true"
      >
        {{ data.libelleConfirmer ?? 'Confirmer' }}
      </button>
    </mat-dialog-actions>
  `,
  styles: [
    `
      .message {
        margin: 0;
        line-height: 1.5;
        color: #3c4b5e;
      }
    `,
  ],
})
export class ConfirmationDialog {
  protected readonly data = inject<ConfirmationData>(MAT_DIALOG_DATA);
  protected readonly dialogRef = inject(MatDialogRef<ConfirmationDialog, boolean>);
}
