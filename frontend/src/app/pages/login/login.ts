import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { fieldError } from '../../forms/field-error';
import { AuthService } from '../../services/auth.service';

const FIELD_MESSAGES = {
  username: { required: 'Ingresá tu usuario.' },
  password: { required: 'Ingresá tu contraseña.' },
} as const;

@Component({
  selector: 'app-login',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './login.html',
})
export class Login {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  protected readonly form = inject(NonNullableFormBuilder).group({
    username: ['', Validators.required],
    password: ['', Validators.required],
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
    this.auth.login(username, password).subscribe({
      next: () => this.router.navigateByUrl(this.safeReturnUrl()),
      error: (e: HttpErrorResponse) => {
        this.loading.set(false);
        this.error.set(
          e.status === 401 ? 'Usuario o contraseña incorrectos.' : 'No se pudo iniciar sesión. Intentá de nuevo.',
        );
      },
    });
  }

  /** Solo rutas internas: un returnUrl absoluto o "//host" seria una redireccion abierta. */
  private safeReturnUrl(): string {
    const target = this.route.snapshot.queryParamMap.get('returnUrl');
    return target && target.startsWith('/') && !target.startsWith('//') ? target : '/dashboard';
  }
}
