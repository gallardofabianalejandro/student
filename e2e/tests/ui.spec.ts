import {expect, test} from '@playwright/test';

test('rutaDesconocida_whenVisited_thenShowsNotFoundPageWithWayHome', async ({page}) => {
    await page.goto('/esta-ruta-no-existe');

    await expect(page.getByRole('heading', {name: 'No encontramos esa página'})).toBeVisible();
    await page.getByRole('link', {name: 'Volver al inicio'}).click();
    await expect(page).toHaveURL(/\/$/);
    await expect(page.getByRole('heading', {name: /Bienvenido/})).toBeVisible();
});

test('tema_whenToggled_thenChangesAndSurvivesReload', async ({page}) => {
    await page.emulateMedia({colorScheme: 'light'});
    await page.goto('/');
    const theme = () => page.evaluate(() => document.documentElement.dataset['theme']);
    await expect.poll(theme).toBe('light');

    await page.getByRole('button', {name: 'Cambiar tema'}).click();
    await expect.poll(theme).toBe('dark');

    await page.reload();
    await expect.poll(theme).toBe('dark');
});
