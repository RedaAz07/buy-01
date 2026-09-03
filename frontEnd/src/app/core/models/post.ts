export interface Productdto {
  id: String;
  name: String;
  description: String;
  price: number;
  quantity: number;
  sellerId: String;
  imageUrls: Array<String>;
}

export interface PageProductDTO {
  content: Productdto[];
  last: boolean;
}
