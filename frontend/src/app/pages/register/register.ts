import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { fieldError } from '../../forms/field-error';
import { AuthService } from '../../services/auth.service';

const FIELD_MESSAGES = {
  username: {
    required: 'El usuario es obligatorio.',
    maxlength: 'El usuario admite hasta 50 caracteres.',
  },
  password: {
    required: 'La contraseña es obligatoria.',
    minlength: 'La contraseña debe tener al menos 8 caracteres.',
    maxlength: 'La contraseña admite hasta 72 caracteres.',
  },
} as const;

@Component({
  selector: 'app-register',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './register.html',
})
export class Register {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  protected readonly form = inject(NonNullableFormBuilder).group({
    username: ['', [Validators.required, Validators.maxLength(50)]],
    password: ['', [Validators.required, Validators.minLength(8), Validators.maxLength(72)]],
  });
  protected readonly error = signal<string | null>(null);
  protected readonly loading = signal(false);

  protected fieldError(field: 'username' | 'password'): string | null {
    return fieldError(this.form.controls[field], FIELD_MESSAGES[field]);
  }

  submit(): void {
    if (this.form.invalid || this.loading()) {
      return;
    }
    this.loading.set(true);
    this.error.set(null);
    const { username, password } = this.form.getRawValue();
    this.auth.register(username, password).subscribe({
      next: () => this.router.navigate(['/login']),
      error: (e: HttpErrorResponse) => {
        this.loading.set(false);
        this.error.set(this.messageFor(e.status));
      },
    });
  }

  private messageFor(status: number): string {
    switch (status) {
      case 404:
        return 'El registro de usuarios no está habilitado.';
      case 409:
        return 'Ese usuario ya existe.';
      case 400:
      case 422:
        return 'Revisá los datos: la contraseña necesita al menos 8 caracteres.';
      default:
        return 'No se pudo completar el registro. Intentá de nuevo.';
    }
  }
}
