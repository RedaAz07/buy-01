import { NgFor, NgIf } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { Productdto } from '../core/models/post';
import { Navbar } from '../components/navbar/navbar';

@Component({
  selector: 'app-product',
  imports: [NgIf, NgFor,Navbar],
  templateUrl: './product.html',
  styleUrl: './product.css',
})
export class Product {
  products = signal<Productdto | null>(null);
  http = inject(HttpClient)
  route = inject(ActivatedRoute)
  ngOnInit() {
    const id = this.route.snapshot.paramMap.get('id');
    this.http.get<Productdto>(`http://localhost:8080/api/products/${id}`).subscribe(
      p => {
        this.products.set(p);
        console.log(p);

      }
    )
  }

}
