import {expect, test} from '@playwright/test';

const password = 'clave-e2e-12345';

test('items_whenNotLoggedIn_thenRedirectsToLogin', async ({page}) => {
    await page.goto('/items');

    await expect(page).toHaveURL(/\/login/);
});

test('flujo_whenRegisterLoginAndCrud_thenEveryStepWorks', async ({page}) => {
    const stamp = Date.now();
    const user = `e2e-${stamp}`;
    const name = `item-${stamp}`;

    await test.step('registro', async () => {
        await page.goto('/register');
        await page.getByLabel('Usuario').fill(user);
        await page.getByLabel('Contraseña').fill(password);
        await page.getByRole('button', {name: 'Registrarse'}).click();
        await expect(page).toHaveURL(/\/login/);
    });

    await test.step('login', async () => {
        await page.getByLabel('Usuario').fill(user);
        await page.getByLabel('Contraseña').fill(password);
        await page.getByRole('button', {name: 'Ingresar'}).click();
        await expect(page).toHaveURL(/\/dashboard/);
        await expect(page.getByText('Sesión iniciada como')).toBeVisible();
    });

    await test.step('ir a Items desde la barra', async () => {
        await page.getByRole('link', {name: 'Items', exact: true}).click();
        await expect(page).toHaveURL(/\/items/);
        await expect(page.getByRole('heading', {name: 'Items', exact: true})).toBeVisible();
    });

    const createItem = async (itemName: string) => {
        await page.getByLabel('Nombre').fill(itemName);
        await page.getByLabel('Cantidad').fill('3');
        await page.getByLabel('Precio').fill('9.5');
        await page.getByRole('button', {name: 'Crear'}).click();
    };

    await test.step('crear un item', async () => {
        await createItem(name);
        await expect(page.getByRole('row', {name: new RegExp(name)})).toBeVisible();
    });

    await test.step('un nombre repetido se rechaza', async () => {
        await createItem(name);
        await expect(page.getByRole('alert')).toContainText('Ya existe un item con ese nombre');
    });

    const edited = `${name}-editado`;

    await test.step('editar el item', async () => {
        await page.getByRole('row', {name: new RegExp(name)}).getByRole('button', {name: 'Editar'}).click();
        await page.getByLabel('Nombre').fill(edited);
        await page.getByRole('button', {name: 'Guardar cambios'}).click();
        await expect(page.getByRole('row', {name: new RegExp(edited)})).toBeVisible();
    });

    await test.step('eliminar el item con confirmacion', async () => {
        const row = page.getByRole('row', {name: new RegExp(edited)});
        await row.getByRole('button', {name: 'Eliminar'}).click();
        await row.getByRole('button', {name: 'Sí'}).click();
        await expect(page.getByRole('row', {name: new RegExp(edited)})).toHaveCount(0);
    });
});
