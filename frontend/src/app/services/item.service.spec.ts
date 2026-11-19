import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ItemService } from './item.service';

describe('ItemService', () => {
  let service: ItemService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    service = TestBed.inject(ItemService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('list_whenCalled_thenAsksTheRequestedPageAndSize', () => {
    service.list(2, 5).subscribe();

    const req = http.expectOne('/api/v1/sample-items?page=2&size=5');
    expect(req.request.method).toBe('GET');
    req.flush({ content: [], page: 2, size: 5, totalElements: 0, totalPages: 0, first: false, last: true });
  });

  it('create_whenCalled_thenPostsTheBody', () => {
    const body = { name: 'caja', description: 'x', quantity: 3, price: 9.5 };

    service.create(body).subscribe();

    const req = http.expectOne('/api/v1/sample-items');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(body);
    req.flush({ id: 'abc', ...body });
  });

  it('update_whenCalled_thenPutsToTheItemUrl', () => {
    const body = { name: 'caja', description: '', quantity: 1, price: 1 };

    service.update('abc', body).subscribe();

    const req = http.expectOne('/api/v1/sample-items/abc');
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual(body);
    req.flush({ id: 'abc', ...body });
  });

  it('remove_whenCalled_thenDeletesTheItemUrl', () => {
    service.remove('abc').subscribe();

    const req = http.expectOne('/api/v1/sample-items/abc');
    expect(req.request.method).toBe('DELETE');
    req.flush(null, { status: 204, statusText: 'No Content' });
  });
});
