import { Component, Input, inject, input, effect } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Productdto } from '../../core/models/post';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { ActivatedRoute, Router } from '@angular/router';
import { MatIcon } from '@angular/material/icon';
import { MatSnackBar } from '@angular/material/snack-bar';

@Component({
  selector: 'app-owner-actions',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, MatIcon],
  templateUrl: './owner-actions.html',
  styleUrl: './owner-actions.css',
})
export class OwnerActions {
  product = input.required<Productdto>();

  // CHANGED: This is now an array of strings based on your console.log
  existingImages: string[] = [];

  selectedFiles: File[] = [];
  previewUrls: string[] = [];

  snackbar = inject(MatSnackBar);
  fb = inject(FormBuilder);
  productForm: FormGroup;
  http = inject(HttpClient);
  route = inject(ActivatedRoute);
  router = inject(Router);

  @Input() id!: String;
  show = false;

  constructor() {
    this.productForm = this.fb.group({
      name: ['', [Validators.maxLength(120)]],
      description: ['', [Validators.maxLength(5000)]],
      price: [0, [Validators.min(0.01)]],
      quantity: [0, [Validators.min(0)]],
    });

    effect(() => {
      const currentProduct = this.product();
      if (currentProduct) {
        this.productForm.patchValue({
          name: currentProduct.name,
          description: currentProduct.description,
          price: currentProduct.price,
          quantity: currentProduct.quantity,
        });

        // CHANGED: Use imageUrls exactly as it appears in the console
        this.existingImages = (currentProduct as any).imageUrls || [];
      }
    });
  }

  updetProduct() {
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

    this.http.put(`http://localhost:8080/api/products/${this.product().id}`, payload).subscribe({
      next: () => {
        if (this.selectedFiles.length > 0) {
          this.uploadNewImages(this.product().id);
        } else {
          this.snackbar.open('Product updated successfully!', 'Close', { duration: 3000 });
          this.show = false;
        }
      },
      error: (err) => {
        console.error(err);
        this.snackbar.open('Failed to update product details.', 'Close', { duration: 3000 });
      },
    });
  }

  private uploadNewImages(productId: String) {
    const formData = new FormData();

    this.selectedFiles.forEach((file) => {
      formData.append('media', file, file.name);
    });

    formData.append('type', 'PRODUCT_IMAGE');

    this.http
      .post(`http://localhost:8080/api/media/images?productId=${productId}`, formData)
      .subscribe({
        next: () => {
          this.snackbar.open('Product and new images updated successfully!', 'Close', { duration: 3000 });
          this.selectedFiles = [];
          this.previewUrls = [];
          this.show = false;
        },
        error: (err) => {
          console.error(err);
          this.snackbar.open('Product updated, but image upload failed.', 'Close', { duration: 3000 });
        },
      });
  }


  removeExistingImage(imageUrl: string, index: number) {
    this.http.delete(`http://localhost:8080/api/media?url=${encodeURIComponent(imageUrl)}`).subscribe({
      next: () => {
        this.existingImages.splice(index, 1);
        this.snackbar.open('Image deleted successfully', 'Close', { duration: 2000 });
      },
      error: (err) => {
        console.error(err);
        this.snackbar.open('Failed to delete image', 'Close', { duration: 3000 });
      },
    });
  }

  deleteProduct() {
    this.http.delete(`http://localhost:8080/api/products/${this.product().id}`).subscribe({
      next: () => {
        this.router.navigate(['/home']);
      },
      error: (e: HttpErrorResponse) => {
        if (e.status == 404) {
          this.router.navigate(['/home']);
        }
      },
    });
  }

  onFilesSelected(event: Event) {
    const input = event.target as HTMLInputElement;

    if (!input.files || input.files.length === 0) {
      return;
    }

    const newFiles = Array.from(input.files);
    this.selectedFiles = [...this.selectedFiles, ...newFiles];

    const newUrls = newFiles.map((file) => URL.createObjectURL(file));
    this.previewUrls = [...this.previewUrls, ...newUrls];
  }

  removeNewFile(index: number) {
    URL.revokeObjectURL(this.previewUrls[index]);
    this.selectedFiles.splice(index, 1);
    this.previewUrls.splice(index, 1);
  }
}
