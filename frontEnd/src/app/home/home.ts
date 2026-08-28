import { Component, inject, signal } from '@angular/core';
import { Navbar } from '../components/navbar/navbar';
import { HttpClient } from '@angular/common/http';
import { Product } from '../core/models/post';
import { Post } from '../components/post/post';

@Component({
  selector: 'app-home',
  imports: [Navbar, Post],
  templateUrl: './home.html',
  styleUrl: './home.css',
})
export class Home {
  http = inject(HttpClient);

  products = signal<Array<Product>>([]);
  ngOnInit() {
    this.http
      .get<Product[]>("http://localhost:8080/api/products")
      .subscribe(products => {
        this.products.set(products);
        console.log(products);

      });
  }
}