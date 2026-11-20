import {expect, test} from '@playwright/test';
import {signInWithoutBackend} from './support';

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

test('items_whenLoadFailsWith500_thenShowsErrorPageAndBackDoesNotTrapTheUser', async ({page}) => {
    await signInWithoutBackend(page, 500);
    await page.goto('/dashboard');

    await page.getByRole('link', {name: 'Items', exact: true}).click();
    await expect(page).toHaveURL(/\/error$/);
    await expect(page.getByRole('heading', {name: /Algo salió mal/})).toBeVisible();

    await page.goBack();
    await expect(page).toHaveURL(/\/dashboard/);
});

test('items_whenApiRecoversAfterAnError_thenRetryReloadsTheList', async ({page}) => {
    await signInWithoutBackend(page, null);
    let failing = true;
    await page.route('**/api/v1/sample-items*', (route) =>
        failing
            ? route.fulfill({status: 503, json: {}})
            : route.fulfill({json: {content: [], page: 0, size: 10, totalElements: 0, totalPages: 1, first: true, last: true}}),
    );
    await page.goto('/dashboard');
    await page.getByRole('link', {name: 'Items', exact: true}).click();
    await expect(page).toHaveURL(/\/error$/);

    failing = false;
    await page.getByRole('button', {name: 'Reintentar'}).click();

    await expect(page).toHaveURL(/\/items$/);
    await expect(page.getByText('No hay items todavía')).toBeVisible();
});

test('items_whenUserHasNoPermission_thenShowsForbiddenPage', async ({page}) => {
    await signInWithoutBackend(page, 403);
    await page.goto('/dashboard');

    await page.getByRole('link', {name: 'Items', exact: true}).click();

    await expect(page).toHaveURL(/\/forbidden$/);
    await expect(page.getByRole('heading', {name: /No tenés permiso/})).toBeVisible();
});
