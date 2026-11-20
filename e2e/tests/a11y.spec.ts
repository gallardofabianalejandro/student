import AxeBuilder from '@axe-core/playwright';
import {expect, Page, test} from '@playwright/test';
import {sampleItems, signInWithoutBackend} from './support';

// Contraste de color, nombres accesibles y estructura (WCAG 2.1 A y AA) en cada pantalla y en los dos temas.
// Si alguien cambia un color y deja de leerse, falla aca.
const themes = ['light', 'dark'] as const;

const screens = [
    {name: 'inicio', url: '/', signedIn: false},
    {name: 'login', url: '/login', signedIn: false},
    {name: 'registro', url: '/register', signedIn: false},
    {name: '404', url: '/ruta-que-no-existe', signedIn: false},
    {name: '500', url: '/error', signedIn: false},
    {name: '403', url: '/forbidden', signedIn: true},
    {name: 'dashboard', url: '/dashboard', signedIn: true},
    {name: 'items', url: '/items', signedIn: true},
];

async function violations(page: Page): Promise<string[]> {
    const results = await new AxeBuilder({page}).withTags(['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa']).analyze();
    return results.violations.map((v) => `${v.id} (${v.impact}): ${v.help} -> ${v.nodes.map((n) => n.target.join(' ')).join(' | ')}`);
}

async function open(page: Page, theme: string, url: string, signedIn: boolean) {
    await page.addInitScript((t) => localStorage.setItem('theme', t), theme);
    if (signedIn) {
        await signInWithoutBackend(page, sampleItems);
    } else {
        await page.route('**/api/auth/refresh', (route) => route.fulfill({status: 401, json: {}}));
    }
    await page.goto(url);
    await page.waitForLoadState('networkidle');
    await expect.poll(() => page.evaluate(() => document.documentElement.dataset['theme'])).toBe(theme);
}

for (const theme of themes) {
    for (const screen of screens) {
        test(`accesibilidad_${screen.name}_${theme}_thenHasNoViolations`, async ({page}) => {
            await open(page, theme, screen.url, screen.signedIn);

            expect(await violations(page)).toEqual([]);
        });
    }

    test(`accesibilidad_loginConError_${theme}_thenHasNoViolations`, async ({page}) => {
        await page.route('**/api/auth/login', (route) => route.fulfill({status: 401, json: {}}));
        await open(page, theme, '/login', false);
        await page.getByLabel('Usuario').fill('ana');
        await page.getByLabel('Contraseña').fill('clave-incorrecta');
        await page.getByRole('button', {name: 'Ingresar'}).click();
        await expect(page.getByRole('alert')).toBeVisible();

        expect(await violations(page)).toEqual([]);
    });

    test(`accesibilidad_itemsConErroresYAviso_${theme}_thenHasNoViolations`, async ({page}) => {
        await open(page, theme, '/items', true);
        await page.getByLabel('Nombre').focus();
        await page.getByLabel('Nombre').blur();
        await page.getByLabel('Cantidad').fill('-1');
        await page.getByLabel('Precio').focus();
        await expect(page.getByText('El nombre es obligatorio.')).toBeVisible();
        await expect(page.getByText('La cantidad no puede ser negativa.')).toBeVisible();

        expect(await violations(page)).toEqual([]);
    });

    test(`accesibilidad_itemsConfirmandoBorrado_${theme}_thenHasNoViolations`, async ({page}) => {
        await open(page, theme, '/items', true);
        await page.getByRole('row', {name: /Teclado/}).getByRole('button', {name: 'Eliminar'}).click();
        await expect(page.getByText('¿Eliminar?')).toBeVisible();

        expect(await violations(page)).toEqual([]);
    });
}
