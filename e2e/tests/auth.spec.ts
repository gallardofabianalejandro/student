import {expect, test} from '@playwright/test';

// Contra el backend real: un 401 de login no debe disparar la renovacion de token (el interceptor la reservaria
// para las demas llamadas) ni llevar a una pagina de error: se muestra en el formulario y nada mas.
test('login_whenPasswordIsWrong_thenShowsTheErrorInTheFormWithoutRefreshingOrLeaving', async ({page}) => {
    const refreshCalls: string[] = [];
    page.on('request', (request) => {
        if (request.url().includes('/api/auth/refresh')) {
            refreshCalls.push(request.url());
        }
    });
    await page.goto('/login');
    await page.waitForLoadState('networkidle');
    const refreshesAtStartup = refreshCalls.length;

    await page.getByLabel('Usuario').fill(`nadie-${Date.now()}`);
    await page.getByLabel('Contraseña').fill('clave-incorrecta-12345');
    await page.getByRole('button', {name: 'Ingresar'}).click();

    await expect(page.getByRole('alert')).toContainText('Usuario o contraseña incorrectos');
    await expect(page).toHaveURL(/\/login/);
    expect(refreshCalls.length).toBe(refreshesAtStartup);
});
