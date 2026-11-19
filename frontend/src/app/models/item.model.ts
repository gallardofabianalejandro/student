export interface Item {
  id: string;
  name: string;
  description: string | null;
  quantity: number;
  price: number;
}

export interface ItemRequest {
  name: string;
  description: string;
  quantity: number;
  price: number;
}

export interface ItemPage {
  content: Item[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}
