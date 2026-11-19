import { Location } from '@angular/common';
import { Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-server-error',
  imports: [RouterLink],
  templateUrl: './server-error.html',
})
export class ServerError {
  private readonly location = inject(Location);

  retry(): void {
    this.location.back();
  }
}
