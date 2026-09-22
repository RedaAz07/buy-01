import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { describe, it, expect, beforeEach, afterEach } from 'vitest';

import { Product } from './product';
import { environment } from '../../../environments/environment';
import { PageProductDTO, Productdto } from '../models/post';

describe('Product Service', () => {
  let service: Product;
  let httpMock: HttpTestingController;

  const mockProduct1 = { id: '1', name: 'Product 1', price: 100 , description:"product1", imageUrls : [] , quantity: 10 , sellerId : "11112"} as Productdto;
  const mockProduct2 ={ id: '2', name: 'Product 2', price: 100 , description:"product2", imageUrls : [] , quantity: 10 , sellerId : "11112"}  as Productdto;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        Product,
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    });

    service = TestBed.inject(Product);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  describe('getMyproduct()', () => {
    it('should send GET request with page and size parameters and append results to products$', () => {
      const mockPageResponse: PageProductDTO = {
        content: [mockProduct1],
        last : false,
      } as PageProductDTO;

      service.getMyproduct(0, 10).subscribe((res) => {
        expect(res).toEqual(mockPageResponse);
      });

      const req = httpMock.expectOne(
        (request) =>
          request.url === `${environment.apiUrl}/api/products/my` &&
          request.params.get('page') === '0' &&
          request.params.get('size') === '10'
      );
      expect(req.request.method).toBe('GET');

      req.flush(mockPageResponse);

      service.products$.subscribe((products) => {
        expect(products).toEqual([mockProduct1]);
      });
    });

    it('should append newly fetched page items to existing items in products$', () => {
      const page1Response = { content: [mockProduct1] } as PageProductDTO;
      const page2Response = { content: [mockProduct2] } as PageProductDTO;

      service.getMyproduct(0, 5).subscribe();
      httpMock.expectOne(`${environment.apiUrl}/api/products/my?page=0&size=5`).flush(page1Response);

      service.getMyproduct(1, 5).subscribe();
      httpMock.expectOne(`${environment.apiUrl}/api/products/my?page=1&size=5`).flush(page2Response);

      service.products$.subscribe((products) => {
        expect(products).toEqual([mockProduct1, mockProduct2]);
      });
    });
  });

  describe('createProduct()', () => {
    it('should send POST request to create a new product', () => {
      const payload = { name: 'New Product', price: 150 };

      service.createProduct(payload).subscribe((res) => {
        expect(res).toEqual(mockProduct1);
      });

      const req = httpMock.expectOne(`${environment.apiUrl}/api/products`);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual(payload);

      req.flush(mockProduct1);
    });
  });

  describe('notifyProductCreated()', () => {
    it('should emit product through productCreated$ observable stream', () => {
      let emittedProduct: Productdto | undefined;

      service.productCreated$.subscribe((product) => {
        emittedProduct = product;
      });

      service.notifyProductCreated(mockProduct1);

      expect(emittedProduct).toEqual(mockProduct1);
    });
  });
});
