import { Component, Input } from '@angular/core';
import { TestBed, ComponentFixture } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ActivatedRoute, Router } from '@angular/router';
import { BehaviorSubject } from 'rxjs';
import { ReactiveFormsModule } from '@angular/forms';
import { Product } from './product';
import { Productdto } from '../core/models/post';
import { UserProfileDTO } from '../core/models/user';
import { Auth } from '../core/services/auth';
import { environment } from '../../environments/environment';

@Component({
  selector: 'app-owner-actions',
  standalone: true,
  template: '',
})
class MockOwnerActions {
  @Input() product!: Productdto;
  @Input() id!: string;
}

const mockProduct: Productdto = {
  id: 'abc123',
  name: 'Wireless Mouse',
  description: 'A great mouse',
  price: 29.99,
  quantity: 5,
  sellerId: 'seller-1',
  imageUrls: ['img1.jpg', 'img2.jpg', 'img3.jpg'],
};

const currentUser: UserProfileDTO = {
  id: 'seller-1',
  name: 'Alice',
  email: 'alice@example.com',
  role: 'SELLER',
  avatar: null,
};

describe('ProductComponent', () => {
  let fixture: ComponentFixture<Product>;
  let component: Product;
  let httpTesting: HttpTestingController;
  let navigateCalls: any[];
  let currentUserSubject: BehaviorSubject<UserProfileDTO | null>;

  const paramMap = {
    get: (key: string) => (key === 'id' ? 'abc123' : null),
  };

  beforeEach(async () => {
    navigateCalls = [];
    currentUserSubject = new BehaviorSubject<UserProfileDTO | null>(null);

    await TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap } } },
        { provide: Router, useValue: { navigate: (args: any[]) => navigateCalls.push(args) } },
        { provide: Auth, useValue: { currentUser$: currentUserSubject.asObservable() } },
      ],
    })
      .overrideComponent(Product, {
        set: { imports: [ReactiveFormsModule, MockOwnerActions] },
      })
      .compileComponents();

    fixture = TestBed.createComponent(Product);
    component = fixture.componentInstance;
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTesting.verify();
  });

  function flushProduct(product: Productdto = mockProduct) {
    fixture.detectChanges();
    const req = httpTesting.expectOne(`${environment.apiUrl}/api/products/abc123`);
    req.flush(product);
    fixture.detectChanges();
  }

  it('should create the component', () => {
    expect(component).toBeTruthy();
  });

  it('should load the product and populate the form on init', () => {
    flushProduct();

    expect(component.products()).toEqual(mockProduct);
    expect(component.productForm.get('name')?.value).toBe('Wireless Mouse');
    expect(component.productForm.get('description')?.value).toBe('A great mouse');
    expect(component.productForm.get('price')?.value).toBe(29.99);
    expect(component.productForm.get('quantity')?.value).toBe(5);
  });

  it('should render the product details in the template', () => {
    flushProduct();

    const el = fixture.nativeElement as HTMLElement;
    expect(el.querySelector('.name')?.textContent).toContain('Wireless Mouse');
    expect(el.querySelector('.description')?.textContent).toContain('A great mouse');
    expect(el.querySelector('.price')?.textContent).toContain('29.99');
    expect(el.querySelector('.stock')?.textContent).toContain('5 in stock');
  });

  it('should navigate to /home when the product is not found (404)', () => {
    fixture.detectChanges();
    const req = httpTesting.expectOne(`${environment.apiUrl}/api/products/abc123`);
    req.flush({ status: 404, statusText: 'Not Found' }, { status: 404, statusText: 'Not Found' });

    expect(navigateCalls).toEqual([['/home']]);
  });

  it('should switch the displayed image when a thumbnail is clicked', () => {
    flushProduct();

    const mainImage = fixture.nativeElement.querySelector('.main-image') as HTMLImageElement;
    expect(mainImage.src).toContain('img1.jpg');

    const thumbs = fixture.nativeElement.querySelectorAll('.thumb');
    expect(thumbs.length).toBe(3);

    (thumbs[2] as HTMLElement).click();
    fixture.detectChanges();

    const updated = fixture.nativeElement.querySelector('.main-image') as HTMLImageElement;
    expect(updated.src).toContain('img3.jpg');
  });

  it('should show out of stock and disable add to cart when quantity is zero', () => {
    flushProduct({ ...mockProduct, quantity: 0 });
    currentUserSubject.next({ ...currentUser, id: 'buyer-1' });
    fixture.detectChanges();

    const el = fixture.nativeElement as HTMLElement;
    expect(el.querySelector('.stock')?.textContent).toContain('Out of stock');
    expect((el.querySelector('.btn-primary') as HTMLButtonElement).disabled).toBe(true);
  });

  it('should show the coming soon message when add to cart is clicked and hide it on second click', () => {
    flushProduct();
    currentUserSubject.next({ ...currentUser, id: 'buyer-1' });
    fixture.detectChanges();

    let el = fixture.nativeElement as HTMLElement;
    expect(el.querySelector('.coming-soon-message')).toBeFalsy();

    (el.querySelector('.btn-primary') as HTMLButtonElement).click();
    fixture.detectChanges();

    el = fixture.nativeElement as HTMLElement;
    expect(el.querySelector('.coming-soon-message')).toBeTruthy();

    (el.querySelector('.btn-primary') as HTMLButtonElement).click();
    fixture.detectChanges();

    el = fixture.nativeElement as HTMLElement;
    expect(el.querySelector('.coming-soon-message')).toBeFalsy();
  });

  it('should render owner actions when the current user is the seller', () => {
    flushProduct();
    currentUserSubject.next(currentUser);
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('app-owner-actions')).toBeTruthy();
    expect(fixture.nativeElement.querySelector('.actions')).toBeNull();
  });

  it('should not render owner actions or buy actions when no user is logged in', () => {
    flushProduct();
    currentUserSubject.next(null);
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('app-owner-actions')).toBeNull();
    expect(fixture.nativeElement.querySelector('.actions')).toBeNull();
  });

  it('should hide owner actions but show add to cart for a non-seller user', () => {
    flushProduct();
    currentUserSubject.next({ ...currentUser, id: 'buyer-1' });
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('app-owner-actions')).toBeNull();
    expect(fixture.nativeElement.querySelector('.actions')).toBeTruthy();
  });

  describe('lotNo', () => {
    it('should return 000 for empty ids', () => {
      expect(component.lotNo('')).toBe('000');
    });

    it('should return 000 when no hex digits remain', () => {
      expect(component.lotNo('XYZ')).toBe('000');
    });

    it('should compute a serial from the last hex digits', () => {
      expect(component.lotNo('ABC-123')).toBe('099');
      expect(component.lotNo('abc')).toBe('748');
    });
  });
});