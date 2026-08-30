import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { Component, inject } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Productdto } from '../../core/models/post';
import { Router } from '@angular/router';

@Component({
  selector: 'app-boutton',
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatIconModule,
    MatFormFieldModule,
    MatInputModule
  ],
  templateUrl: './boutton.html',
  styleUrl: './boutton.css'
})
export class Boutton {
  showCard = false;

  productForm: FormGroup;

  // Multiple files
  selectedFiles: File[] = [];

  // Multiple previews
  previewUrls: string[] = [];

  fb = inject(FormBuilder);
  http = inject(HttpClient);
  snackbar = inject(MatSnackBar);
  router = inject(Router);

  constructor() {
    this.productForm = this.fb.group({
      name: ['', [Validators.required, Validators.maxLength(120)]],
      description: ['', [Validators.required, Validators.maxLength(5000)]],
      price: [0, [Validators.required, Validators.min(0.01)]],
      quantity: [0, [Validators.required, Validators.min(0)]],
      imageUrl: [''],
    });
  }

  hasError(controlName: string, errorName: string): boolean {
    const control = this.productForm.get(controlName);

    return !!control &&
      control.hasError(errorName) &&
      (control.touched || control.dirty);
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

    // Create product first
    this.http.post<Productdto>(
      'http://localhost:8080/api/products',
      payload
    ).subscribe({
      next: (product) => {

        // Upload images if selected
        if (this.selectedFiles.length > 0) {

          const formData = new FormData();

          // Append ALL files using the same key
          this.selectedFiles.forEach((file) => {
            formData.append('media', file, file.name);
          });

          formData.append('type', 'PRODUCT_IMAGE');
          console.log(formData.get("media"),"---------------------------->");


          this.http
            .post(
              `http://localhost:8080/api/media/images?productId=${product.id}`,
              formData
            )
            .subscribe({
              next: () => {
                this.snackbar.open(
                  'Product created successfully!',
                  'Close',
                  { duration: 3000 }
                );
              },

              error: () => {
                this.snackbar.open(
                  'Product created, but image upload failed.',
                  'Close',
                  { duration: 3000 }
                );
              },
            });

        } else {
          this.snackbar.open(
            'Product created successfully!',
            'Close',
            { duration: 3000 }
          );
        }

        // Reset form
        this.showCard = false;

        this.productForm.reset({
          name: '',
          description: '',
          price: 0,
          quantity: 0,
          imageUrl: '',
        });
        this.selectedFiles.forEach((_, index) => {
          URL.revokeObjectURL(this.previewUrls[index]);
        });
        this.selectedFiles = [];
        this.previewUrls = [];
      },

      error: (err) => {
        const msg =
          err?.error?.message ||
          'Failed to create product. Please try again.';

        this.snackbar.open(msg, 'Close', {
          duration: 3000
        });
      },
    });
  }

  onFilesSelected(event: Event) {
    const input = event.target as HTMLInputElement;

    if (!input.files || input.files.length === 0) {
      this.selectedFiles = [];
      this.previewUrls = [];
      return;
    }

    this.selectedFiles = [...this.selectedFiles , ...Array.from(input.files)]

    this.previewUrls = this.selectedFiles.map((file) =>
      URL.createObjectURL(file)
    );
  }

  removeFile(index: number) {
    URL.revokeObjectURL(this.previewUrls[index]);

    this.selectedFiles.splice(index, 1);
    this.previewUrls.splice(index, 1);
  }
}
