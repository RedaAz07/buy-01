import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { Component, inject } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Productdto } from '../../core/models/post';

@Component({
    selector: 'app-boutton',
    imports: [CommonModule, ReactiveFormsModule, MatIconModule, MatFormFieldModule, MatInputModule],
    templateUrl: './boutton.html',
    styleUrl: './boutton.css'
})
export class Boutton {
    showCard = false;
    productForm: FormGroup;
    selectedFile: File | null = null;
    previewUrl: string | null = null;

    fb = inject(FormBuilder);
    http = inject(HttpClient);
    snackbar = inject(MatSnackBar);

    constructor() {
        this.productForm = this.fb.group({
            name: ['', [Validators.required, Validators.maxLength(120)]],
            description: ['', [Validators.required, Validators.maxLength(5000)]],
            price: [0, [Validators.required, Validators.min(0.01)]],
            quantity: [0, [Validators.required, Validators.min(0)]],
            imageUrl: [''],
        });
    }
    /*
    control.touched=> enter the inpute
    control.dirty=>chenge the value of input
     */
    hasError(controlName: string, errorName: string): boolean {
        const control = this.productForm.get(controlName);
        return !!control && control.hasError(errorName) && (control.touched || control.dirty);
    }

    onSubmit() {
        if (this.productForm.invalid) {
            this.productForm.markAllAsTouched();
            return;
        }

        const value = this.productForm.value;
        const payload = {
            name: value.name,
            description: value.description,
            price: value.price,
            quantity: value.quantity,

        };

        this.http.post<Productdto>('http://localhost:8080/api/products', payload).subscribe({
            next: (product) => {
                if (this.selectedFile) {
                    const formData = new FormData();
                    formData.append('media', this.selectedFile, this.selectedFile.name);

                    this.http
                        .post(
                            `http://localhost:8080/api/media/images?productId=${product.id}&type=PRODUCT_IMAGE`,
                            formData
                        )
                        .subscribe({
                            next: () => {
                                this.snackbar.open('Product created successfully!', 'Close', { duration: 3000 });
                            },
                            error: () => {
                                this.snackbar.open('Product created, but image upload failed.', 'Close', { duration: 3000 });
                            },
                        });
                } else {
                    this.snackbar.open('Product created successfully!', 'Close', { duration: 3000 });
                }

                this.showCard = false;
                this.productForm.reset({
                    name: '',
                    description: '',
                    price: 0,
                    quantity: 0,
                    imageUrl: '',
                });
                this.selectedFile = null;
                this.previewUrl = null;
            },
            error: (err) => {
                const msg = err?.error?.message || 'Failed to create product. Please try again.';
                this.snackbar.open(msg, 'Close', { duration: 3000 });
            },
        });
    }
    onFileSelected(event: Event) {
        const input = event.target as HTMLInputElement;

        if (input.files && input.files.length > 0) {
            this.selectedFile = input.files[0];
            const reader = new FileReader();
            reader.onload = () => {
                this.previewUrl = reader.result as string;
            };
            reader.readAsDataURL(this.selectedFile);
        } else {
            this.selectedFile = null;
            this.previewUrl = null;
        }
    }

    removeFile() {
        this.selectedFile = null;
        this.previewUrl = null;
        this.productForm.patchValue({ imageUrl: '' });
    }
}
