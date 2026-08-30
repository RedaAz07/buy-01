import { NgFor, NgIf } from '@angular/common';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
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
  router = inject(Router)
  id = signal<String | null>(null)
  show = false
  private userService = inject(Auth);
  lotNo(id: String | string): string {
    const clean = String(id).toLowerCase().replace(/[^0-9a-f]/g, '');
    if (!clean) {
      return '000';
    }
    const n = parseInt(clean.slice(-6), 16);
    return String(isNaN(n) ? 1 : n % 1000).padStart(3, '0');
  }

  user = toSignal(this.userService.currentUser$, {
    initialValue: null
  });
  ngOnInit() {

    this.id.set(this.route.snapshot.paramMap.get('id'));
    this.http.get<Productdto>(`http://localhost:8080/api/products/${this.id()}`).subscribe(
      {
        next: (p) => {
          this.products.set(p);
          console.log(p);
        },
        error: (e: HttpErrorResponse) => {
          if (e.status == 404) {
            this.router.navigate(['/home']);
          }
        }
      }
    )
  }
  updetProduct() {
    // this.http.put(`http://localhost:8080/api/products/${this.id()}`, pylode).subscribe({
    //   next(value) {

    //   },
    //   error(err) {

    //   },
    // })

  }
  deleteProduct() {
    this.http.delete(`http://localhost:8080/api/products/${this.id()}`).subscribe(
      {
        next: (p) => {
          console.log(p);
          this.router.navigate(['/home']);
        },
        error: (e: HttpErrorResponse) => {
          if (e.status == 404) {
            this.router.navigate(['/home']);
          }
        }
      }
    );
  }

}
