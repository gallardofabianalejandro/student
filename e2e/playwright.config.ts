import {defineConfig, devices} from '@playwright/test';

// BASE_URL: la instancia a probar. Por defecto el backend local que sirve el front empaquetado.
export default defineConfig({
    testDir: './tests',
    // Un servicio gratis de Render tarda en despertar: margenes amplios.
    timeout: 120_000,
    expect: {timeout: 30_000},
    fullyParallel: false,
    workers: 1,
    retries: 0,
    reporter: [['list'], ['html', {open: 'never'}]],
    use: {
        baseURL: process.env.BASE_URL ?? 'http://localhost:8080',
        trace: 'retain-on-failure',
        screenshot: 'only-on-failure',
    },
    projects: [{name: 'chromium', use: {...devices['Desktop Chrome']}}],
});
