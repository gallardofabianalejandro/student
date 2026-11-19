import {defineConfig, devices} from '@playwright/test';
import {readFileSync} from 'node:fs';
import {join} from 'node:path';

// Ambientes con nombre en environments.json. Se elige con E2E_ENV (por defecto "local"):
//   task e2e                      -> local
//   task e2e E2E_ENV=render       -> render
// BASE_URL, si esta definida, manda sobre el archivo (lo usa el workflow de GitHub).
const environments: Record<string, string> = JSON.parse(
    readFileSync(join(__dirname, 'environments.json'), 'utf-8'),
);
const envName = process.env.E2E_ENV || 'local';
const baseURL = process.env.BASE_URL || environments[envName];

if (!baseURL) {
    throw new Error(
        `El ambiente '${envName}' no esta en e2e/environments.json. Opciones: ${Object.keys(environments).join(', ')}`,
    );
}

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
        baseURL,
        trace: 'retain-on-failure',
        screenshot: 'only-on-failure',
    },
    projects: [{name: 'chromium', use: {...devices['Desktop Chrome']}}],
});
