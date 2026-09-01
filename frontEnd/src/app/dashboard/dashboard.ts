import { TitleCasePipe } from '@angular/common';
import { Component, inject, OnInit, signal } from '@angular/core';
import { FormBuilder, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { UpdateRequest, UserProfileDTO } from '../core/models/user';
import { Auth } from '../core/services/auth';
import { Media } from '../core/services/media';
import { User } from '../core/services/user';
import { Productdto } from '../core/models/post';
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
  imports: [FormsModule, TitleCasePipe, ReactiveFormsModule, Boutton,OwnerActions],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.css',
})
export class Dashboard implements OnInit {
  private product = inject(Product);
  private auth = inject(Auth);
  private userService = inject(User);

  private media = inject(Media)

  sellerProducts = signal<Productdto[]>([]);
  user = signal<UserProfileDTO | null>(null);
  activeTab = signal<'overview' | 'products' | 'orders' | 'settings'>(
    'overview'
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


  orders: Order[] = [
    {
      id: '#3491',
      product: 'Canvas Tote Bag',
      buyer: 'Nora K.',
      date: 'Aug 26',
      amount: 38,
      status: 'delivered',
    },
    {
      id: '#3490',
      product: 'Ceramic Pour-Over Set',
      buyer: 'Liam P.',
      date: 'Aug 25',
      amount: 54,
      status: 'shipped',
    },
    {
      id: '#3488',
      product: 'Crossbody Bag',
      buyer: 'Sofia M.',
      date: 'Aug 24',
      amount: 64,
      status: 'pending',
    },
    {
      id: '#3485',
      product: 'Woven Belt',
      buyer: 'Yusuf A.',
      date: 'Aug 22',
      amount: 22,
      status: 'delivered',
    },
    {
      id: '#3481',
      product: 'Canvas Tote Bag',
      buyer: 'Elena V.',
      date: 'Aug 21',
      amount: 38,
      status: 'delivered',
    },
  ];

  weekSales: WeekSale[] = [
    { day: 'Mon', value: 40 },
    { day: 'Tue', value: 65 },
    { day: 'Wed', value: 50 },
    { day: 'Thu', value: 80 },
    { day: 'Fri', value: 95 },
    { day: 'Sat', value: 70 },
    { day: 'Sun', value: 55 },
  ];

  // ===================== TAB TITLES =====================

  tabTitles = {
    overview: {
      title: 'Overview',
      subtitle: "Welcome back — here's how your shop is doing.",
    },
    products: {
      title: 'My Products',
      subtitle: 'Manage your listings, stock, and pricing.',
    },
    orders: {
      title: 'Orders',
      subtitle: 'Track and fulfill orders from your buyers.',
    },
    settings: {
      title: 'Settings',
      subtitle: 'Update your shop profile and photo.',
    },
  };

  // ===================== GETTERS =====================
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
    this.product.getMyproduct().subscribe({
      next: Product => {
        this.sellerProducts.set(Product);
      }
    })


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

  get maxSales(): number {
    return Math.max(...this.weekSales.map((sale) => sale.value));
  }

  // ===================== TAB SWITCHING =====================

  switchTab(
    tab: 'overview' | 'products' | 'orders' | 'settings'
  ): void {
    if (tab === "overview" || tab === "orders") {
      this.showToast("This is currently static data. The service will be available soon.");
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

  deleteProduct(product: Productdto): void {


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

    // Call media-service here
    this.media.setAvatar([file]).subscribe({
      next: avatarUrl => {


        this.showToast('Profile photo updated');
      },

      error: () => {
        this.showToast('Failed to update profile photo');
      }
    });

    input.value = '';
  }
  removeAvatar(): void {
    const user = this.user();

    if (!user?.avatar) {
      return;
    }

    this.media.deleteAvatar(user.avatar).subscribe({
      next: () => {
        this.user.set({
          ...user,
          avatar: null
        });

        this.showToast('Profile photo removed');
      },

      error: (err) => {
        const errorMessage =
          err?.error?.message || 'Failed to delete this image';

        this.showToast(errorMessage);
      }
    });
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
}
