import { Component, Input } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import {
  HttpTestingController,
  provideHttpClientTesting,
} from '@angular/common/http/testing';
import { Subject } from 'rxjs';
import { Home } from './home';
import { Productdto } from '../core/models/post';
import { Product } from '../core/services/product';
import { environment } from '../../environments/environment';

@Component({
  selector: 'app-post',
  template: '',
})
class MockPost {
  @Input() product!: Productdto;
}

const products: Productdto[] = [
  {
    id: 'product-1',
    name: 'Vintage chair',
    description: 'A comfortable chair',
    price: 75,
    quantity: 1,
    sellerId: 'seller-1',
    imageUrls: [],
  },
];

describe('Home', () => {
  let fixture: ComponentFixture<Home>;
  let component: Home;
  let httpTesting: HttpTestingController;
  let productCreated$: Subject<Productdto>;

  beforeEach(async () => {
    productCreated$ = new Subject<Productdto>();

    await TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: Product, useValue: { productCreated$: productCreated$.asObservable() } },
      ],
    })
      .overrideComponent(Home, { set: { imports: [MockPost] } })
      .compileComponents();

    fixture = TestBed.createComponent(Home);
    component = fixture.componentInstance;
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTesting.verify();
  });

  it('creates and loads products on init', () => {
    fixture.detectChanges();
    const request = httpTesting.expectOne(`${environment.apiUrl}/api/products`);
    expect(request.request.method).toBe('GET');

    request.flush(products);
    fixture.detectChanges();

    expect(component).toBeTruthy();
    expect(component.products()).toEqual(products);
    expect(fixture.nativeElement.querySelectorAll('app-post').length).toBe(1);
  });

  it('reloads products when a product is created', () => {
    fixture.detectChanges();
    httpTesting.expectOne(`${environment.apiUrl}/api/products`).flush(products);

    productCreated$.next(products[0]);
    const request = httpTesting.expectOne(`${environment.apiUrl}/api/products`);
    request.flush([...products, { ...products[0], id: 'product-2' }]);

    expect(component.products()).toHaveLength(2);
  });
});
