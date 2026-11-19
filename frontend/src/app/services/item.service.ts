import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { Item, ItemPage, ItemRequest } from '../models/item.model';

/** CRUD de ejemplo (SampleItem). Rutas relativas: el token lo agrega el interceptor de autenticacion. */
@Injectable({ providedIn: 'root' })
export class ItemService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/v1/sample-items';

  list(page: number, size: number): Observable<ItemPage> {
    return this.http.get<ItemPage>(`${this.baseUrl}?page=${page}&size=${size}`);
  }

  create(item: ItemRequest): Observable<Item> {
    return this.http.post<Item>(this.baseUrl, item);
  }

  update(id: string, item: ItemRequest): Observable<Item> {
    return this.http.put<Item>(`${this.baseUrl}/${id}`, item);
  }

  remove(id: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}
