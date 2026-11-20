import { Location } from '@angular/common';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { Forbidden } from './forbidden';

describe('Forbidden', () => {
  function render(): HTMLElement {
    TestBed.configureTestingModule({ imports: [Forbidden], providers: [provideRouter([])] });
    const fixture = TestBed.createComponent(Forbidden);
    fixture.detectChanges();
    return fixture.nativeElement as HTMLElement;
  }

  it('render_whenCreated_thenExplainsThereIsNoPermission', () => {
    const page = render();

    expect(page.textContent).toContain('403');
    expect(page.querySelector('h1')?.textContent).toContain('No tenés permiso');
  });

  it('back_whenClicked_thenGoesToThePreviousPage', () => {
    const page = render();
    const back = vi.spyOn(TestBed.inject(Location), 'back').mockImplementation(() => undefined);

    Array.from(page.querySelectorAll('button')).find((b) => b.textContent?.includes('Volver atrás'))?.click();

    expect(back).toHaveBeenCalledOnce();
  });

  it('render_whenCreated_thenOffersAWayBackHome', () => {
    expect(render().querySelector('a[href="/"]')?.textContent).toContain('Volver al inicio');
  });
});
