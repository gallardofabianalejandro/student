import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { TitleStrategy, provideRouter } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { PageTitleStrategy } from './page-title.strategy';

@Component({ template: '' })
class Blank {}

describe('PageTitleStrategy', () => {
  async function visit(url: string): Promise<void> {
    document.title = 'student';
    TestBed.configureTestingModule({
      providers: [
        provideRouter([
          { path: 'items', title: 'Items', component: Blank },
          { path: 'sin-titulo', component: Blank },
        ]),
        { provide: TitleStrategy, useClass: PageTitleStrategy },
      ],
    });
    const harness = await RouterTestingHarness.create();
    await harness.navigateByUrl(url);
  }

  it('updateTitle_whenRouteHasATitle_thenAppendsTheApplicationName', async () => {
    await visit('/items');

    expect(document.title).toBe('Items · student');
  });

  it('updateTitle_whenRouteHasNoTitle_thenKeepsTheApplicationName', async () => {
    await visit('/sin-titulo');

    expect(document.title).toBe('student');
  });
});
