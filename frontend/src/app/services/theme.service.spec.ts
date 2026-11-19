import { TestBed } from '@angular/core/testing';
import { ThemeService } from './theme.service';

describe('ThemeService', () => {
  function systemPrefers(dark: boolean): void {
    vi.stubGlobal(
      'matchMedia',
      vi.fn().mockReturnValue({ matches: dark, addEventListener: vi.fn(), removeEventListener: vi.fn() }),
    );
  }

  function create(): ThemeService {
    TestBed.resetTestingModule();
    return TestBed.inject(ThemeService);
  }

  beforeEach(() => {
    localStorage.clear();
    document.documentElement.removeAttribute('data-theme');
    systemPrefers(false);
  });

  afterEach(() => {
    vi.unstubAllGlobals();
    vi.restoreAllMocks();
  });

  it('theme_whenNothingStoredAndSystemIsLight_thenLight', () => {
    expect(create().theme()).toBe('light');
  });

  it('theme_whenNothingStoredAndSystemIsDark_thenDark', () => {
    systemPrefers(true);

    expect(create().theme()).toBe('dark');
  });

  it('theme_whenStoredChoiceDiffersFromSystem_thenStoredChoiceWins', () => {
    systemPrefers(true);
    localStorage.setItem('theme', 'light');

    expect(create().theme()).toBe('light');
  });

  it('theme_whenStoredValueIsInvalid_thenFallsBackToSystem', () => {
    localStorage.setItem('theme', 'azul');

    expect(create().theme()).toBe('light');
  });

  it('theme_whenCreated_thenSetsDataThemeOnTheDocument', () => {
    systemPrefers(true);

    create();

    expect(document.documentElement.getAttribute('data-theme')).toBe('dark');
  });

  it('toggle_whenLight_thenDarkAndPersistsAndUpdatesTheDocument', () => {
    const service = create();

    service.toggle();

    expect(service.theme()).toBe('dark');
    expect(localStorage.getItem('theme')).toBe('dark');
    expect(document.documentElement.getAttribute('data-theme')).toBe('dark');
  });

  it('toggle_whenDark_thenLight', () => {
    localStorage.setItem('theme', 'dark');
    const service = create();

    service.toggle();

    expect(service.theme()).toBe('light');
    expect(document.documentElement.getAttribute('data-theme')).toBe('light');
  });

  it('toggle_whenStorageFails_thenStillSwitchesTheTheme', () => {
    const service = create();
    vi.spyOn(Storage.prototype, 'setItem').mockImplementation(() => {
      throw new Error('storage bloqueado');
    });

    service.toggle();

    expect(service.theme()).toBe('dark');
  });

  it('theme_whenMatchMediaIsMissing_thenLight', () => {
    vi.stubGlobal('matchMedia', undefined);

    expect(create().theme()).toBe('light');
  });
});
