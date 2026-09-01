import { Component, Input, Output, EventEmitter, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Productdto } from '../../core/models/post';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { ActivatedRoute, Router } from '@angular/router';

@Component({
    selector: 'app-owner-actions',
    standalone: true,
    imports: [CommonModule, ReactiveFormsModule],
    templateUrl: './owner-actions.html',
    styleUrl: './owner-actions.css',
})
export class OwnerActions {
    @Input() product!: Productdto;
    fb = inject(FormBuilder);
    productForm: FormGroup;
    http = inject(HttpClient);
    route = inject(ActivatedRoute);
    router = inject(Router);
    @Input() id!: String;
    show = false;
    constructor() {
        this.productForm = this.fb.group({
            name: ['', [Validators.required, Validators.maxLength(120)]],
            description: ['', [Validators.required, Validators.maxLength(5000)]],
            price: [0, [Validators.required, Validators.min(0.01)]],
            quantity: [0, [Validators.required, Validators.min(0)]],
        });
    }

    ngOnInit() {
       console.log(this.product);
       
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
        this.http.put(`http://localhost:8080/api/products/${this.product.id}`, payload).subscribe({
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
        this.http.delete(`http://localhost:8080/api/products/${this.product.id}`).subscribe({
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
