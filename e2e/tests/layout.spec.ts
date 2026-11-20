import {expect, test} from '@playwright/test';
import {sampleItems, signInWithoutBackend} from './support';

// El diseno de las tarjetas es CSS puro: solo un navegador real puede comprobarlo.
test.describe('en pantalla de celular', () => {
    test.use({viewport: {width: 390, height: 800}});

    test('items_whenPhoneWidth_thenRowsAreCardsWithoutHorizontalScroll', async ({page}) => {
        await signInWithoutBackend(page, sampleItems);
        await page.goto('/items');
        await expect(page.getByRole('row', {name: /Teclado/})).toBeVisible();

        const overflows = await page.evaluate(() => document.documentElement.scrollWidth > document.documentElement.clientWidth);
        expect(overflows).toBe(false);

        const edit = await page
            .getByRole('row', {name: /Teclado/})
            .getByRole('button', {name: 'Editar'})
            .boundingBox();
        expect(edit).not.toBeNull();
        expect(edit!.x).toBeGreaterThanOrEqual(0);
        expect(edit!.x + edit!.width).toBeLessThanOrEqual(390);
    });

    test('items_whenPhoneWidthAndEditing_thenNameFieldIsFocusedAndInView', async ({page}) => {
        await signInWithoutBackend(page, sampleItems);
        await page.goto('/items');

        await page.getByRole('row', {name: /Monitor/}).getByRole('button', {name: 'Editar'}).click();

        await expect(page.getByLabel('Nombre')).toBeFocused();
        await expect(page.getByLabel('Nombre')).toHaveValue('Monitor 27" 4K');
        await expect(page.getByLabel('Nombre')).toBeInViewport();
    });
});
