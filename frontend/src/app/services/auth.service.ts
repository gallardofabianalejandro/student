import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Observable, catchError, finalize, firstValueFrom, map, of, shareReplay, switchMap, tap } from 'rxjs';
import { TokenResponse, User } from '../models/auth.model';

/**
 * Unico punto que conoce el mecanismo de autenticacion (login propio con JWT). Cambiar a Keycloak/OIDC
 * implica reescribir este servicio y los interceptores, no las paginas.
 *
 * El access token vive solo en memoria: no se guarda en localStorage ni sessionStorage (un XSS lo leeria).
 * La sesion sobrevive a un reload gracias al refresh token, que viaja en una cookie HttpOnly.
 */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly accessToken = signal<string | null>(null);
  private readonly currentUser = signal<User | null>(null);
  private refreshInFlight$: Observable<string> | null = null;

  readonly user = this.currentUser.asReadonly();
  readonly isAuthenticated = computed(() => this.currentUser() !== null);

  getAccessToken(): string | null {
    return this.accessToken();
  }

  login(username: string, password: string): Observable<User> {
    return this.http.post<TokenResponse>('/api/auth/login', { username, password }).pipe(
      tap((response) => this.accessToken.set(response.accessToken)),
      switchMap(() => this.loadUser()),
    );
  }

  register(username: string, password: string): Observable<User> {
    return this.http.post<User>('/api/auth/register', { username, password });
  }

  /** Una sola renovacion a la vez: varias requests con 401 simultaneas comparten el mismo refresh. */
  refreshAccessToken(): Observable<string> {
    if (!this.refreshInFlight$) {
      this.refreshInFlight$ = this.http
        .post<TokenResponse>(
          '/api/auth/refresh',
          {},
          { withCredentials: true, headers: { 'X-Requested-With': 'XMLHttpRequest' } },
        )
        .pipe(
          map((response) => response.accessToken),
          tap((token) => this.accessToken.set(token)),
          finalize(() => (this.refreshInFlight$ = null)),
          shareReplay(1),
        );
    }
    return this.refreshInFlight$;
  }

  /** Al arrancar: si hay cookie de refresh valida, recupera token y usuario. Nunca falla. */
  restoreSession(): Promise<void> {
    return firstValueFrom(
      this.refreshAccessToken().pipe(
        switchMap(() => this.loadUser()),
        map(() => undefined),
        catchError(() => of(undefined)),
      ),
    );
  }

  logout(): Observable<unknown> {
    return this.http.post('/api/auth/logout', {}, { withCredentials: true }).pipe(
      catchError(() => of(undefined)),
      finalize(() => this.clearSession()),
    );
  }

  /** Limpia el estado local sin llamar al servidor (sesion ya invalida). */
  clearSession(): void {
    this.accessToken.set(null);
    this.currentUser.set(null);
  }

  private loadUser(): Observable<User> {
    return this.http.get<User>('/api/auth/me').pipe(tap((user) => this.currentUser.set(user)));
  }
}
