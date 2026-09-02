import { Component, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Productdto } from '../core/models/post';
import { Post } from '../components/post/post';
import { Product } from '../core/services/product';

@Component({
  selector: 'app-home',
  imports: [ Post],
  templateUrl: './home.html',
  styleUrl: './home.css',
})
export class Home {
  http = inject(HttpClient);
  productService = inject(Product);

  products = signal<Array<Productdto>>([]);
  
  ngOnInit() {
    this.loadProducts();
    
    // Subscribe to product creation events
    this.productService.productCreated$.subscribe(() => {
      this.loadProducts();
    });
  }

  private loadProducts(): void {
    this.http
      .get<Productdto[]>("http://localhost:8080/api/products")
      .subscribe(products => {
        this.products.set(products);
        console.log(products);
      });
  }
}
