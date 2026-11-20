import { Location } from '@angular/common';
import { Component, inject } from '@angular/core';
import { Router, RouterLink } from '@angular/router';

@Component({
  selector: 'app-server-error',
  imports: [RouterLink],
  templateUrl: './server-error.html',
})
export class ServerError {
  private readonly location = inject(Location);
  private readonly router = inject(Router);

  /** Vuelve a cargar la pantalla que fallo; si no se sabe cual fue, retrocede en el historial. */
  retry(): void {
    const from = (this.location.getState() as { from?: unknown } | null)?.from;
    if (typeof from === 'string' && from.startsWith('/') && !from.startsWith('//') && !from.startsWith('/\\')) {
      this.router.navigateByUrl(from);
    } else {
      this.location.back();
    }
  }
}
