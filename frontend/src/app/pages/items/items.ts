import { CurrencyPipe, DecimalPipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, ElementRef, OnInit, inject, signal, viewChild } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { fieldError } from '../../forms/field-error';
import { Item, ItemPage } from '../../models/item.model';
import { ItemService } from '../../services/item.service';

const PAGE_SIZE = 10;

const FIELD_MESSAGES = {
  name: {
    required: 'El nombre es obligatorio.',
    maxlength: 'El nombre admite hasta 100 caracteres.',
  },
  description: {
    maxlength: 'La descripción admite hasta 500 caracteres.',
  },
  quantity: {
    required: 'Ingresá una cantidad.',
    min: 'La cantidad no puede ser negativa.',
  },
  price: {
    required: 'Ingresá un precio.',
    min: 'El precio no puede ser negativo.',
  },
} as const;

type Field = keyof typeof FIELD_MESSAGES;

@Component({
  selector: 'app-items',
  imports: [ReactiveFormsModule, CurrencyPipe, DecimalPipe],
  templateUrl: './items.html',
})
export class Items implements OnInit {
  private readonly items = inject(ItemService);
  private readonly nameInput = viewChild<ElementRef<HTMLInputElement>>('nameInput');

  protected readonly currency = 'ARS';
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
  protected readonly notice = signal<string | null>(null);
  protected readonly loading = signal(false);

  ngOnInit(): void {
    this.load(0);
  }

  protected fieldError(field: Field): string | null {
    return fieldError(this.form.controls[field], FIELD_MESSAGES[field]);
  }

  submit(): void {
    if (this.form.invalid || this.loading()) {
      return;
    }
    this.loading.set(true);
    this.error.set(null);
    this.notice.set(null);
    const body = this.form.getRawValue();
    const id = this.editingId();
    const request = id ? this.items.update(id, body) : this.items.create(body);
    request.subscribe({
      next: () => {
        this.resetForm();
        this.notice.set(id ? 'Cambios guardados.' : 'Item creado.');
        this.load(this.current()?.page ?? 0);
      },
      error: (e: HttpErrorResponse) => this.fail(e),
    });
  }

  edit(item: Item): void {
    this.editingId.set(item.id);
    this.notice.set(null);
    this.form.setValue({
      name: item.name,
      description: item.description ?? '',
      quantity: item.quantity,
      price: item.price,
    });
    // En pantallas chicas el formulario queda arriba y fuera de vista: el foco lo trae.
    this.nameInput()?.nativeElement.focus();
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
    this.error.set(null);
    this.items.remove(item.id).subscribe({
      next: () => {
        this.pendingRemoval.set(null);
        this.notice.set('Item eliminado.');
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
    this.notice.set(null);
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
