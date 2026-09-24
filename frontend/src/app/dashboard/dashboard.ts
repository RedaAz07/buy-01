import { Component, ElementRef, inject, OnInit, signal, ViewChild } from '@angular/core';
import { FormBuilder, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { UpdateRequest, UserProfileDTO } from '../core/models/user';
import { Auth } from '../core/services/auth';
import { Media } from '../core/services/media';
import { User } from '../core/services/user';
import { PageProductDTO, Productdto } from '../core/models/post';
import { Product } from '../core/services/product';
import { Boutton } from '../components/boutton/boutton';
import { OwnerActions } from '../components/owner-actions/owner-actions';



interface Order {
  id: string;
  product: string;
  buyer: string;
  date: string;
  amount: number;
  status: 'delivered' | 'shipped' | 'pending';
}

interface WeekSale {
  day: string;
  value: number;
}

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [FormsModule, ReactiveFormsModule, Boutton, OwnerActions],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.css',
})
export class Dashboard implements OnInit {
  private observer!: IntersectionObserver;
  currentPage = 0;
  isLoading = false;

  @ViewChild('scrollAnchor') set setupScrollAnchor(element: ElementRef) {
    if (element) {
      if (this.observer) {
        this.observer.disconnect();
      }
      this.observer = new IntersectionObserver(([entry]) => {
        if (entry.isIntersecting && !this.isLoading) {
          this.loadMoreProducts();
        }
      }, { root: null, rootMargin: '0px', threshold: 0.1 });
      this.observer.observe(element.nativeElement);
    }
  }

  private product = inject(Product);
  private auth = inject(Auth);
  private userService = inject(User);

  private media = inject(Media)

  sellerProducts = signal<Productdto[]>([]);
  user = signal<UserProfileDTO | null>(null);
  activeTab = signal<'products' | 'settings'>(
    'settings'
  );

  toastMessage = signal('');
  showToastMessage = signal(false);
  showSaveHint = signal(false);
  private fb = inject(FormBuilder);

  settingsForm = this.fb.group({
    name: [
      '',
      [
        Validators.required,
        Validators.minLength(3),
        Validators.maxLength(50),
      ],
    ],

    email: [
      '',
      [
        Validators.required,
        Validators.email,
      ],
    ],
  });
  // ===================== DATA =====================



  // ===================== TAB TITLES =====================

  tabTitles = {

    products: {
      title: 'My Products',
      subtitle: 'Manage your listings, stock, and pricing.',
    },
    settings: {
      title: 'Settings',
      subtitle: 'Update your shop profile and photo.',
    },
  };

  // ===================== GETTERS =====================

  loadMoreProducts(): void {
    if (this.isLoading) {
      return
    }
    this.isLoading = true;
    this.product.getMyproduct(this.currentPage, 10).subscribe({
      next: (newProducts: PageProductDTO) => {
        this.sellerProducts.update((curr) => [...curr, ...newProducts.content])
        this.currentPage++;
        this.isLoading = false;
      },
      error: (err) => {
        this.isLoading = false;
      }

    })
  }



  ngOnInit(): void {
    this.auth.currentUser$.subscribe(user => {

      this.user.set(user);

      if (user) {
        this.settingsForm.patchValue({
          name: user.name,
          email: user.email,
        });

        this.settingsForm.markAsPristine();
      }

    });

    this.loadMoreProducts();


  }

  saveSettings(): void {

    if (this.settingsForm.invalid) {
      this.settingsForm.markAllAsTouched();
      return;
    }

    const { name, email } = this.settingsForm.getRawValue();
    const data: UpdateRequest = { name, email };
    this.userService.updateProfile(data).subscribe({
      next: updatedUser => {

        this.user.update(user => {
          if (!user) {
            return null;
          }

          return {
            ...user,
            name: updatedUser.name,
            email: updatedUser.email
          };
        });

        localStorage.removeItem("jwt_token")
        localStorage.setItem("jwt_token", updatedUser.jwt);

        this.settingsForm.patchValue({
          name: updatedUser.name,
          email: updatedUser.email,
        });

        this.settingsForm.markAsPristine();

        this.showSaveHint.set(true);

        setTimeout(() => {
          this.showSaveHint.set(false);
        }, 2200);

        this.showToast('Profile updated successfully');
      },

      error: () => {
        this.showToast('Failed to update profile');
      }
    });
  }
  get currentTabTitle() {
    return this.tabTitles[this.activeTab()].title;
  }

  get currentTabSubtitle() {
    return this.tabTitles[this.activeTab()].subtitle;
  }

  get initials(): string {
    return this.user()?.name || "USER"
  }



  // ===================== TAB SWITCHING =====================

  switchTab(
    tab: 'settings' | 'products'
  ): void {
    if (this.user()?.role === "ROLE_CIENT" && tab === "products") {
      this.activeTab.set("settings");
      return
    }

    this.activeTab.set(tab);
  }

  // ===================== PRODUCTS =====================

  addProduct(): void {
    this.switchTab('products');
  }

  editProduct(product: Productdto): void {
    this.showToast(`Editing "${product.name}"`);
  }



  // ===================== AVATAR =====================

  openAvatarPicker(input: HTMLInputElement): void {
    input.click();
  }
  onAvatarSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];

    if (!file) {
      return;
    }

    if (!file.type.startsWith('image/')) {
      this.showToast('Please choose an image file');
      input.value = '';
      return;
    }
    if (file.size > 2 * 1024 * 1024) {
      this.showToast('Image size cannot be bigger than 2MB');
      input.value = '';
      return;
    }

    // Call media-service here
    this.media.setAvatar([file]).subscribe({
      next: avatarUrl => {

        this.user.update(user => {
          if (!user) {
            return null;
          }

          return {
            ...user,
            avatar: avatarUrl[0],
          };
        });


        this.showToast('Profile photo updated');
      },

      error: () => {
        this.showToast('Failed to update profile photo');
      }
    });

    input.value = '';
  }
 



  // ===================== TOAST =====================

  private toastTimer?: ReturnType<typeof setTimeout>;

  showToast(message: string): void {
    if (this.toastTimer) {
      clearTimeout(this.toastTimer);
    }

    this.toastMessage.set(message);
    this.showToastMessage.set(true);

    this.toastTimer = setTimeout(() => {
      this.showToastMessage.set(false);
    }, 2200);
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
  logout() {
    this.auth.logout();
  }
  onProductUpdated(updatedProduct: Productdto) {
    this.sellerProducts.update((products) =>
      products.map((product) => product.id === updatedProduct.id ? updatedProduct : product)
    );
  }

  onProductDeleted(id: String) {
    this.sellerProducts.update((products) =>
      products.filter((product) => product.id !== id)
    );
  }
  onProductCreated(product: Productdto) {

    this.sellerProducts.set([...this.sellerProducts(), product]);
  }
}
