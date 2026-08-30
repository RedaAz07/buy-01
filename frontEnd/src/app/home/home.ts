import { Component, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Productdto } from '../core/models/post';
import { Post } from '../components/post/post';

@Component({
  selector: 'app-home',
  imports: [ Post],
  templateUrl: './home.html',
  styleUrl: './home.css',
})
export class Home {
  http = inject(HttpClient);

  products = signal<Array<Productdto>>([]);
  ngOnInit() {
    this.http
      .get<Productdto[]>("http://localhost:8080/api/products")
      .subscribe(products => {
        this.products.set(products);
        console.log(products);

      });
  }
}
