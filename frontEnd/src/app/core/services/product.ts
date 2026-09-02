import { inject, Injectable } from '@angular/core';
import { Observable, Subject } from 'rxjs';
import { Productdto } from '../models/post';
import { HttpClient } from '@angular/common/http';
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

  getMyproduct(): Observable<Productdto[]> {
    return this.http.get<Productdto[]>(this.apiUrl)
  }

  createProduct(payload: any): Observable<Productdto> {
    return this.http.post<Productdto>(`${environment.apiUrl}/api/products`, payload);
  }

  notifyProductCreated(product: Productdto): void {
    this.productCreatedSubject.next(product);
  }
}
