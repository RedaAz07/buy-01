import { Component, Input, Output, EventEmitter, inject, signal, effect, input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Productdto } from '../../core/models/post';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { ActivatedRoute, Router } from '@angular/router';
import { MatIcon } from '@angular/material/icon';
import { MatSnackBar } from '@angular/material/snack-bar';
import { environment } from '../../../environments/environment';

@Component({
  selector: 'app-owner-actions',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, MatIcon],
  templateUrl: './owner-actions.html',
  styleUrl: './owner-actions.css',
})
export class OwnerActions {
  product = input.required<Productdto>();
  @Output() productUpdated = new EventEmitter<Productdto>();
  @Output() productDelete = new EventEmitter<String>();

  // State managed via Signals
  existingImages = signal<String[]>([]);
  selectedFiles = signal<File[]>([]);
  previewUrls = signal<string[]>([]);

  snackbar = inject(MatSnackBar);
  fb = inject(FormBuilder);
  productForm: FormGroup;
  http = inject(HttpClient);
  route = inject(ActivatedRoute);
  router = inject(Router);

  @Input() id!: String;
  show = signal<boolean>(false);
  updated = false;
  deleted = false;

  // Per-action loading states
  updateLoading = signal<boolean>(false);
  deleteLoading = signal<boolean>(false);
  imageDeleteLoading = signal<boolean>(false);

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

        // Set signal value safely
        this.existingImages.set(currentProduct.imageUrls || []);
      }
    });
  }

  updetProduct() {
    if (this.updated) {
      return
    }
    this.updated = true;
    this.updateLoading.set(true);
    if (this.productForm.invalid) {
      this.productForm.markAllAsTouched();
      this.updateLoading.set(false);
      this.updated = false;
      return;
    }

    const value = this.productForm.value;
    const payload = {
      name: value.name,
      description: value.description,
      price: value.price,
      quantity: value.quantity,
    };

    this.http.put<Productdto>(`${environment.apiUrl}/api/products/${this.product().id}`, payload).subscribe({
      next: (p: Productdto) => {

        if (this.selectedFiles().length > 0) {
          this.uploadNewImages(this.product().id);
        } else {
          this.snackbar.open('Product updated successfully!', 'Close', { duration: 3000 });
          this.show.set(false)
          this.productUpdated.emit(p);
          this.updateLoading.set(false);
        }
        this.updated = false
      },
      error: (err) => {
        console.error(err);
        this.snackbar.open('Failed to update product details.', 'Close', { duration: 3000 });
        this.updated = false;
        this.updateLoading.set(false);
        if (err.state = 404) {
          this.router.navigate(['/home']);
        }
      },
    });
  }

  private uploadNewImages(productId: String) {
    const formData = new FormData();

    this.selectedFiles().forEach((file) => {
      formData.append('media', file, file.name);
    });

    formData.append('type', 'PRODUCT_IMAGE');

    this.http
      .post<string[]>(`${environment.apiUrl}/api/media/images?productId=${productId}`, formData)
      .subscribe({
        next: (urls: string[]) => {

          this.snackbar.open('Product and new images updated successfully!', 'Close', { duration: 3000 });
          this.selectedFiles.set([]);
          this.previewUrls.set([]);
          this.show.set(false) // Close popup
          const updatedProduct: Productdto = {
            ...this.product(),
            imageUrls: [
              ...(this.product().imageUrls ?? []),
              ...urls
            ]
          };
          this.productUpdated.emit(updatedProduct);
          this.updateLoading.set(false);
        },
        error: (err) => {
          console.error(err);
          this.snackbar.open('Product updated, but image upload failed.', 'Close', { duration: 3000 });
          this.updateLoading.set(false);
        },
      });
  }

  removeExistingImage(imageUrl: string) {
    if (this.deleted) {
      return
    }
    this.deleted = true;
    this.imageDeleteLoading.set(true);
    this.http.delete<{ "image": string }>(`${environment.apiUrl}/api/media/images?url=${encodeURIComponent(imageUrl)}`).subscribe({
      next: (v: { "image": string }) => {

        // Update signal immutably using .update()
        this.existingImages.update(images => images.filter(url => url !== imageUrl));
        this.snackbar.open('Image deleted successfully', 'Close', { duration: 2000 });
        const updatedProduct: Productdto = {
          ...this.product(),
          imageUrls: [
            ...(this.product().imageUrls.filter(u => u != v.image) ?? []),

          ]
        };
        this.productUpdated.emit(updatedProduct);
        this.deleted = false;
        this.imageDeleteLoading.set(false);
        this.show.set(false)
      },
      error: (err) => {
        console.error(err);
        this.snackbar.open('Failed to delete image', 'Close', { duration: 3000 });
        this.deleted = false;
        this.imageDeleteLoading.set(false);
      },
    });
  }

  deleteProduct() {
    if (this.deleteLoading()) {
      return;
    }
    this.deleteLoading.set(true);
    this.http.delete(`${environment.apiUrl}/api/products/${this.product().id}`).subscribe({
      next: () => {
        this.deleteLoading.set(false);
        if (window.location.href == `http://localhost:4200/product/${this.product().id}`) {
          this.router.navigate(['/home']);

        }

        const id = this.product().id;
        this.productDelete.emit(id);



      },
      error: (e: HttpErrorResponse) => {
        this.deleteLoading.set(false);
        if (e.status == 404) {
          this.router.navigate(['/home']);
        }
      },
    });
  }

  onFilesSelected(event: Event) {
    const input = event.target as HTMLInputElement;
    if (!input.files || input.files.length === 0) return;

    const newFiles = Array.from(input.files);
    const newUrls = newFiles.map((file) => URL.createObjectURL(file));

    // Append cleanly to signal state
    this.selectedFiles.update(files => [...files, ...newFiles]);
    this.previewUrls.update(urls => [...urls, ...newUrls]);

    // Reset native input value so selecting the same file triggers change again if needed
    input.value = '';
  }

  removeNewFile(index: number) {
    const currentUrls = this.previewUrls();
    URL.revokeObjectURL(currentUrls[index]);

    this.selectedFiles.update(files => files.filter((_, i) => i !== index));
    this.previewUrls.update(urls => urls.filter((_, i) => i !== index));
  }
}
