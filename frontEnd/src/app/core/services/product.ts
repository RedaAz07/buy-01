import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { Productdto } from '../models/post';
import { HttpClient } from '@angular/common/http';

@Injectable({
  providedIn: 'root',
})
export class Product {
  private http = inject(HttpClient);
  private apiUrl = "http://localhost:8080/api/products/my"
  getMyproduct(): Observable<Productdto[]> {
    return this.http.get<Productdto[]>(this.apiUrl)
  }


}
