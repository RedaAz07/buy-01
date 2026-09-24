import { inject, Injectable } from '@angular/core';
import { BehaviorSubject, Observable, Subject, tap } from 'rxjs';
import { PageProductDTO, Productdto } from '../models/post';
import { HttpClient, HttpParams } from '@angular/common/http';
import { environment } from '../../../environments/environment';

@Injectable({
  providedIn: 'root',
})
export class Product {
  private http = inject(HttpClient);
  private apiUrl = `${environment.apiUrl}/api/products/my`

  // Subject to notify when products change
  private productCreatedSubject = new Subject<Productdto>();
  public productCreated$ = this.productCreatedSubject.asObservable();
  // pagination
  private productsSubject = new BehaviorSubject<Productdto[]>([]);
  public products$ = this.productsSubject.asObservable();

  getMyproduct(pageNumber: number, pageSize: number = 5): Observable<PageProductDTO> {
    let params = new HttpParams()
      .set('page', pageNumber.toString())
      .set('size', pageSize.toString());
    return this.http.get<PageProductDTO>(this.apiUrl, { params }).pipe(
      tap((response) => {
        const currentProducts = this.productsSubject.value;
        const combinedList = [...currentProducts, ...response.content];
        this.productsSubject.next(combinedList)
      })
    )
  }

  createProduct(payload: any): Observable<Productdto> {
    return this.http.post<Productdto>(`${environment.apiUrl}/api/products`, payload);
  }

  notifyProductCreated(product: Productdto): void {
    this.productCreatedSubject.next(product);
  }
}
