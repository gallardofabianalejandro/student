import { TestBed } from '@angular/core/testing';
import { HttpErrorResponse } from '@angular/common/http';
import { provideRouter } from '@angular/router';
import { throwError } from 'rxjs';
import { AuthService } from '../../services/auth.service';
import { Register } from './register';

describe('Register', () => {
  const register = vi.fn();

  beforeEach(() => {
    register.mockReset();
    TestBed.configureTestingModule({
      imports: [Register],
      providers: [provideRouter([]), { provide: AuthService, useValue: { register } }],
    });
  });

  function submitFailingWith(status: number): string {
    register.mockReturnValue(throwError(() => new HttpErrorResponse({ status })));
    const fixture = TestBed.createComponent(Register);
    fixture.detectChanges();
    fixture.componentInstance['form'].setValue({ username: 'ana', password: 's3cret-pass' });

    fixture.componentInstance.submit();
    fixture.detectChanges();

    return (fixture.nativeElement as HTMLElement).querySelector('[role="alert"]')?.textContent?.trim() ?? '';
  }

  function textAfterTouching(values: { username: string; password: string }): string {
    const fixture = TestBed.createComponent(Register);
    fixture.detectChanges();
    fixture.componentInstance['form'].setValue(values);
    fixture.componentInstance['form'].markAllAsTouched();
    fixture.detectChanges();
    return (fixture.nativeElement as HTMLElement).textContent ?? '';
  }

  it('fieldError_whenPasswordIsShort_thenSaysTheMinimumLength', () => {
    expect(textAfterTouching({ username: 'ana', password: 'corta' })).toContain('al menos 8 caracteres');
  });

  it('fieldError_whenUsernameIsEmpty_thenAsksForIt', () => {
    expect(textAfterTouching({ username: '', password: 's3cret-pass' })).toContain('El usuario es obligatorio');
  });

  it('submit_whenRegistrationDisabled_thenExplainsItIsNotEnabled', () => {
    expect(submitFailingWith(404)).toContain('no está habilitado');
  });

  it('submit_whenUsernameTaken_thenSaysUserExists', () => {
    expect(submitFailingWith(409)).toContain('ya existe');
  });

  it.each([400, 422])('submit_whenServerRejectsValidationWith%i_thenAsksToReviewData', (status) => {
    expect(submitFailingWith(status)).toContain('Revisá los datos');
  });

  it('submit_whenUnexpectedError_thenShowsGenericMessage', () => {
    expect(submitFailingWith(500)).toContain('No se pudo completar');
  });

  it('submit_whenFormInvalid_thenDoesNotCallServer', () => {
    const fixture = TestBed.createComponent(Register);
    fixture.detectChanges();

    fixture.componentInstance.submit();

    expect(register).not.toHaveBeenCalled();
  });
});
