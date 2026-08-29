import { NgFor, NgIf } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { Productdto } from '../core/models/post';
import { Navbar } from '../components/navbar/navbar';
import { Auth } from '../core/services/auth';
import { toSignal } from '@angular/core/rxjs-interop';

@Component({
  selector: 'app-product',
  imports: [NgIf, NgFor, Navbar],
  templateUrl: './product.html',
  styleUrl: './product.css',
})
export class Product {
  products = signal<Productdto | null>(null);
  http = inject(HttpClient)
  route = inject(ActivatedRoute)
  id = signal<String | null>(null)
  private userService = inject(Auth);

  user = toSignal(this.userService.currentUser$, {
    initialValue: null
  });
  ngOnInit() {

    this.id.set(this.route.snapshot.paramMap.get('id'));
    this.http.get<Productdto>(`http://localhost:8080/api/products/${this.id()}`).subscribe(
      p => {
        this.products.set(p);

      }
    )
  }
  updetProduct() {

  }
  deleteProduct() {
    this.http.delete(`http://localhost:8080/api/products/${this.id()}`).subscribe(
      (e) => {
        console.log(e);
      }
    );
  }

}
