import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { Productdto } from '../core/models/post';
import { Auth } from '../core/services/auth';
import { toSignal } from '@angular/core/rxjs-interop';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';

@Component({
  selector: 'app-product',
  imports: [ReactiveFormsModule],
  templateUrl: './product.html',
  styleUrl: './product.css',
})
export class Product {
  products = signal<Productdto | null>(null);
  http = inject(HttpClient);
  route = inject(ActivatedRoute);
  router = inject(Router);
  id = signal<String | null>(null);
  show = false;
  private userService = inject(Auth);
  fb = inject(FormBuilder);
  productForm: FormGroup;

  constructor() {
    this.productForm = this.fb.group({
      name: ['', [Validators.required, Validators.maxLength(120)]],
      description: ['', [Validators.required, Validators.maxLength(5000)]],
      price: [0, [Validators.required, Validators.min(0.01)]],
      quantity: [0, [Validators.required, Validators.min(0)]],
    });
  }

  lotNo(id: String | string): string {
    const clean = String(id)
      .toLowerCase()
      .replace(/[^0-9a-f]/g, '');
    if (!clean) {
      return '000';
    }
    const n = parseInt(clean.slice(-6), 16);
    return String(isNaN(n) ? 1 : n % 1000).padStart(3, '0');
  }

  user = toSignal(this.userService.currentUser$, {
    initialValue: null,
  });
  ngOnInit() {
    this.id.set(this.route.snapshot.paramMap.get('id'));
    this.http.get<Productdto>(`http://localhost:8080/api/products/${this.id()}`).subscribe({
      next: (p) => {
        this.products.set(p);
        console.log(p);
        this.productForm.patchValue({
          name: p.name,
          description: p.description,
          price: p.price,
          quantity: p.quantity,
        });
      },
      error: (e: HttpErrorResponse) => {
        if (e.status == 404) {
          this.router.navigate(['/home']);
        }
      },
    });
  }
  updetProduct() {
    if (this.productForm.invalid) {
      this.productForm.markAllAsTouched();
      return;
    }

    const value = this.productForm.value;
    console.log(value);
    const payload = {
      name: value.name,
      description: value.description,
      price: value.price,
      quantity: value.quantity,
    };
    this.http.put(`http://localhost:8080/api/products/${this.id()}`, payload).subscribe({
      next: (value) => {
        console.log(value);
        this.show = false
      },
      error(err) {
        console.log(err);
      },
    });
  }
  deleteProduct() {
    this.http.delete(`http://localhost:8080/api/products/${this.id()}`).subscribe({
      next: (p) => {
        console.log(p);
        this.router.navigate(['/home']);
      },
      error: (e: HttpErrorResponse) => {
        if (e.status == 404) {
          this.router.navigate(['/home']);
        }
      },
    });
  }
}
