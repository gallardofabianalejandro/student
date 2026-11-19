import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, inject, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Item, ItemPage } from '../../models/item.model';
import { ItemService } from '../../services/item.service';

const PAGE_SIZE = 10;

@Component({
  selector: 'app-items',
  imports: [ReactiveFormsModule],
  templateUrl: './items.html',
})
export class Items implements OnInit {
  private readonly items = inject(ItemService);

  protected readonly form = inject(NonNullableFormBuilder).group({
    name: ['', [Validators.required, Validators.maxLength(100)]],
    description: ['', [Validators.maxLength(500)]],
    quantity: [0, [Validators.required, Validators.min(0)]],
    price: [0, [Validators.required, Validators.min(0)]],
  });
  protected readonly current = signal<ItemPage | null>(null);
  protected readonly editingId = signal<string | null>(null);
  protected readonly pendingRemoval = signal<Item | null>(null);
  protected readonly error = signal<string | null>(null);
  protected readonly loading = signal(false);

  ngOnInit(): void {
    this.load(0);
  }

  submit(): void {
    if (this.form.invalid || this.loading()) {
      return;
    }
    this.loading.set(true);
    this.error.set(null);
    const body = this.form.getRawValue();
    const id = this.editingId();
    const request = id ? this.items.update(id, body) : this.items.create(body);
    request.subscribe({
      next: () => {
        this.resetForm();
        this.load(this.current()?.page ?? 0);
      },
      error: (e: HttpErrorResponse) => this.fail(e),
    });
  }

  edit(item: Item): void {
    this.editingId.set(item.id);
    this.form.setValue({
      name: item.name,
      description: item.description ?? '',
      quantity: item.quantity,
      price: item.price,
    });
  }

  cancelEdit(): void {
    this.resetForm();
  }

  askRemove(item: Item): void {
    this.pendingRemoval.set(item);
  }

  cancelRemove(): void {
    this.pendingRemoval.set(null);
  }

  confirmRemove(): void {
    const item = this.pendingRemoval();
    if (!item) {
      return;
    }
    this.items.remove(item.id).subscribe({
      next: () => {
        this.pendingRemoval.set(null);
        this.load(this.current()?.page ?? 0);
      },
      error: (e: HttpErrorResponse) => {
        this.pendingRemoval.set(null);
        this.fail(e);
      },
    });
  }

  next(): void {
    const page = this.current();
    if (page && !page.last) {
      this.load(page.page + 1);
    }
  }

  previous(): void {
    const page = this.current();
    if (page && !page.first) {
      this.load(page.page - 1);
    }
  }

  private load(page: number): void {
    this.loading.set(true);
    this.items.list(page, PAGE_SIZE).subscribe({
      next: (result) => {
        this.current.set(result);
        this.loading.set(false);
      },
      error: (e: HttpErrorResponse) => this.fail(e),
    });
  }

  private resetForm(): void {
    this.editingId.set(null);
    this.form.reset({ name: '', description: '', quantity: 0, price: 0 });
  }

  private fail(e: HttpErrorResponse): void {
    this.loading.set(false);
    this.error.set(this.messageFor(e.status));
  }

  private messageFor(status: number): string {
    switch (status) {
      case 409:
        return 'Ya existe un item con ese nombre.';
      case 400:
      case 422:
        return 'Revisá los datos: nombre obligatorio, cantidad y precio no negativos.';
      case 404:
        return 'El item ya no existe.';
      default:
        return 'No se pudo completar la operación. Intentá de nuevo.';
    }
  }
}
