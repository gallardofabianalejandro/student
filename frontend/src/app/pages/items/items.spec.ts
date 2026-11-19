import { TestBed } from '@angular/core/testing';
import { HttpErrorResponse } from '@angular/common/http';
import { of, throwError } from 'rxjs';
import { Item, ItemPage } from '../../models/item.model';
import { ItemService } from '../../services/item.service';
import { Items } from './items';

describe('Items', () => {
  const caja: Item = { id: 'id-1', name: 'caja', description: 'chica', quantity: 3, price: 9.5 };
  const pageOf = (content: Item[], page = 0, last = true): ItemPage => ({
    content,
    page,
    size: 10,
    totalElements: content.length,
    totalPages: last ? page + 1 : page + 2,
    first: page === 0,
    last,
  });
  const service = { list: vi.fn(), create: vi.fn(), update: vi.fn(), remove: vi.fn() };

  beforeEach(() => {
    Object.values(service).forEach((fn) => fn.mockReset());
    service.list.mockReturnValue(of(pageOf([caja])));
    TestBed.configureTestingModule({ imports: [Items], providers: [{ provide: ItemService, useValue: service }] });
  });

  function create() {
    const fixture = TestBed.createComponent(Items);
    fixture.detectChanges();
    return fixture;
  }

  const text = (fixture: { nativeElement: unknown }) => (fixture.nativeElement as HTMLElement).textContent ?? '';
  const alert = (fixture: { nativeElement: unknown }) =>
    (fixture.nativeElement as HTMLElement).querySelector('[role="alert"]')?.textContent?.trim() ?? '';

  it('init_whenLoaded_thenShowsTheItemsOfTheFirstPage', () => {
    const fixture = create();

    expect(service.list).toHaveBeenCalledWith(0, 10);
    expect(text(fixture)).toContain('caja');
  });

  it('init_whenNoItems_thenShowsEmptyMessage', () => {
    service.list.mockReturnValue(of(pageOf([])));

    expect(text(create())).toContain('No hay items');
  });

  it('init_whenListFails_thenShowsAnError', () => {
    service.list.mockReturnValue(throwError(() => new HttpErrorResponse({ status: 500 })));

    expect(alert(create())).toContain('No se pudo');
  });

  it('submit_whenNewItem_thenCreatesItAndReloads', () => {
    service.create.mockReturnValue(of(caja));
    const fixture = create();
    fixture.componentInstance['form'].setValue({ name: 'nuevo', description: '', quantity: 2, price: 1.5 });

    fixture.componentInstance.submit();

    expect(service.create).toHaveBeenCalledWith({ name: 'nuevo', description: '', quantity: 2, price: 1.5 });
    expect(service.list).toHaveBeenCalledTimes(2);
  });

  it('submit_whenEditing_thenUpdatesTheSameItem', () => {
    service.update.mockReturnValue(of(caja));
    const fixture = create();
    fixture.componentInstance.edit(caja);
    fixture.componentInstance['form'].patchValue({ name: 'caja grande' });

    fixture.componentInstance.submit();

    expect(service.update).toHaveBeenCalledWith('id-1', expect.objectContaining({ name: 'caja grande' }));
    expect(service.create).not.toHaveBeenCalled();
  });

  it('submit_whenFormInvalid_thenDoesNotCallTheServer', () => {
    const fixture = create();

    fixture.componentInstance.submit();

    expect(service.create).not.toHaveBeenCalled();
  });

  it.each([
    [409, 'Ya existe'],
    [400, 'Revisá los datos'],
    [422, 'Revisá los datos'],
    [404, 'ya no existe'],
    [500, 'No se pudo'],
  ])('submit_whenServerAnswers%i_thenShowsMatchingMessage', (status, expected) => {
    service.create.mockReturnValue(throwError(() => new HttpErrorResponse({ status })));
    const fixture = create();
    fixture.componentInstance['form'].setValue({ name: 'x', description: '', quantity: 1, price: 1 });

    fixture.componentInstance.submit();
    fixture.detectChanges();

    expect(alert(fixture)).toContain(expected);
  });

  it('remove_whenNotConfirmed_thenDoesNotDelete', () => {
    const fixture = create();

    fixture.componentInstance.askRemove(caja);

    expect(service.remove).not.toHaveBeenCalled();
  });

  it('remove_whenConfirmed_thenDeletesAndReloads', () => {
    service.remove.mockReturnValue(of(undefined));
    const fixture = create();

    fixture.componentInstance.askRemove(caja);
    fixture.componentInstance.confirmRemove();

    expect(service.remove).toHaveBeenCalledWith('id-1');
    expect(service.list).toHaveBeenCalledTimes(2);
  });

  it('next_whenNotLastPage_thenLoadsTheFollowingPage', () => {
    service.list.mockReturnValue(of(pageOf([caja], 0, false)));
    const fixture = create();

    fixture.componentInstance.next();

    expect(service.list).toHaveBeenLastCalledWith(1, 10);
  });

  it('previous_whenFirstPage_thenDoesNothing', () => {
    const fixture = create();

    fixture.componentInstance.previous();

    expect(service.list).toHaveBeenCalledTimes(1);
  });
});
