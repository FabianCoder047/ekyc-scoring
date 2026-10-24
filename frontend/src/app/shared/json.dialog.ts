import { Component, computed, inject } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';

export interface JsonDialogData {
  titre: string;
  contenu: unknown;
}

@Component({
  selector: 'app-json-dialog',
  imports: [MatDialogModule, MatButtonModule],
  template: `
    <h2 mat-dialog-title>{{ data.titre }}</h2>
    <mat-dialog-content>
      <pre class="pre-json">{{ texte() }}</pre>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button type="button" mat-dialog-close>Fermer</button>
    </mat-dialog-actions>
  `,
  styles: [
    `
      mat-dialog-content {
        min-width: min(680px, 78vw);
      }
    `,
  ],
})
export class JsonDialog {
  protected readonly data = inject<JsonDialogData>(MAT_DIALOG_DATA);

  protected readonly texte = computed(() => {
    if (this.data.contenu === null || this.data.contenu === undefined) {
      return '—';
    }
    try {
      return JSON.stringify(this.data.contenu, null, 2);
    } catch {
      return String(this.data.contenu);
    }
  });
}
