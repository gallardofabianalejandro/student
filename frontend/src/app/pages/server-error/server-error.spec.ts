import { Location } from '@angular/common';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { ServerError } from './server-error';

describe('ServerError', () => {
  function render() {
    TestBed.configureTestingModule({ imports: [ServerError], providers: [provideRouter([])] });
    const fixture = TestBed.createComponent(ServerError);
    fixture.detectChanges();
    return fixture.nativeElement as HTMLElement;
  }

  it('render_whenCreated_thenShowsTheCodeAndAHeading', () => {
    const page = render();

    expect(page.textContent).toContain('500');
    expect(page.querySelector('h1')?.textContent).toContain('Algo salió mal');
  });

  it('retry_whenClicked_thenGoesBackToThePreviousPage', () => {
    const page = render();
    const back = vi.spyOn(TestBed.inject(Location), 'back').mockImplementation(() => undefined);

    const retry = Array.from(page.querySelectorAll('button')).find((b) => b.textContent?.includes('Reintentar'));
    retry?.click();

    expect(back).toHaveBeenCalledOnce();
  });

  it('render_whenCreated_thenOffersAWayBackHome', () => {
    expect(render().querySelector('a[href="/"]')?.textContent).toContain('Volver al inicio');
  });
});
