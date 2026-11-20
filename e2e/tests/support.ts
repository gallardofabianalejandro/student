import {Page} from '@playwright/test';

export interface SampleItem {
    id: string;
    name: string;
    description: string;
    quantity: number;
    price: number;
}

export const sampleItems: SampleItem[] = [
    {id: '1', name: 'Teclado mecánico', description: 'Switches rojos, layout español', quantity: 1234, price: 68900},
    {id: '2', name: 'Monitor 27" 4K', description: 'Panel IPS, 60 Hz', quantity: 8, price: 412500.5},
    {id: '3', name: 'Mouse inalámbrico', description: 'Sensor óptico', quantity: 51, price: 24300},
];

/**
 * Sesion simulada en el navegador, para los tests que verifican como reacciona la pantalla (errores de la API,
 * diseno, accesibilidad) sin depender del backend ni de la base. El login real lo cubre items.spec.ts.
 *
 * items: un numero responde ese codigo HTTP al listar; una lista responde esa pagina; null no toca el listado.
 */
export async function signInWithoutBackend(page: Page, items: number | SampleItem[] | null) {
    await page.route('**/api/auth/refresh', (route) => route.fulfill({json: {accessToken: 'e2e'}}));
    await page.route('**/api/auth/me', (route) => route.fulfill({json: {username: 'ana', roles: ['USER']}}));
    if (typeof items === 'number') {
        await page.route('**/api/v1/sample-items*', (route) => route.fulfill({status: items, json: {}}));
    } else if (items !== null) {
        await page.route('**/api/v1/sample-items*', (route) =>
            route.fulfill({
                json: {content: items, page: 0, size: 10, totalElements: items.length, totalPages: 1, first: true, last: true},
            }),
        );
    }
}
