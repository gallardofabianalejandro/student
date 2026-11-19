import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { NotFound } from './not-found';

describe('NotFound', () => {
  function render(): HTMLElement {
    TestBed.configureTestingModule({ imports: [NotFound], providers: [provideRouter([])] });
    const fixture = TestBed.createComponent(NotFound);
    fixture.detectChanges();
    return fixture.nativeElement as HTMLElement;
  }

  it('render_whenCreated_thenShowsTheCodeAndAHeading', () => {
    const page = render();

    expect(page.textContent).toContain('404');
    expect(page.querySelector('h1')?.textContent).toContain('No encontramos esa página');
  });

  it('render_whenCreated_thenOffersAWayBackHome', () => {
    const link = render().querySelector('a[href="/"]');

    expect(link?.textContent).toContain('Volver al inicio');
  });
});
