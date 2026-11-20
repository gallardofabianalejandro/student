import { AbstractControl } from '@angular/forms';

/** Mensaje del primer error de un campo, solo despues de que el usuario lo toco. Null si no hay nada que mostrar. */
export function fieldError(control: AbstractControl, messages: Record<string, string>): string | null {
  if (!control.touched || control.valid || !control.errors) {
    return null;
  }
  const code = Object.keys(control.errors)[0];
  return messages[code] ?? null;
}
