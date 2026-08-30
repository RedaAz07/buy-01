import { TitleCasePipe } from '@angular/common';
import { Component, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';

interface Product {
  id: number;
  name: string;
  emoji: string;
  price: number;
  stock: number;
  status: 'active' | 'low';
}

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
  imports: [FormsModule,TitleCasePipe],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.css',
})
export class Dashboard {
  // ===================== STATE =====================

  activeTab = signal<'overview' | 'products' | 'orders' | 'settings'>(
    'overview'
  );

  toastMessage = signal('');
  showToastMessage = signal(false);
  showSaveHint = signal(false);

  avatarUrl = signal<string | null>(null);

  fullName = 'Amélie Rousseau';
  shopName = 'Rousseau & Co.';
  email = 'amelie@rousseauco.com';
  phone = '+212 6 00 00 00 00';
  bio =
    'Independent maker of everyday goods — bags, home pieces, and small-batch ceramics.';

  // ===================== DATA =====================

  sellerProducts: Product[] = [
    {
      id: 1,
      name: 'Canvas Tote Bag',
      emoji: '👜',
      price: 38,
      stock: 24,
      status: 'active',
    },
    {
      id: 2,
      name: 'Ceramic Pour-Over Set',
      emoji: '☕',
      price: 54,
      stock: 3,
      status: 'low',
    },
    {
      id: 3,
      name: 'Woven Belt',
      emoji: '🧵',
      price: 22,
      stock: 40,
      status: 'active',
    },
    {
      id: 4,
      name: 'Linen Throw Pillow',
      emoji: '🛋️',
      price: 29,
      stock: 0,
      status: 'low',
    },
    {
      id: 5,
      name: 'Crossbody Bag',
      emoji: '👝',
      price: 64,
      stock: 18,
      status: 'active',
    },
  ];

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

  get currentTabTitle() {
    return this.tabTitles[this.activeTab()].title;
  }

  get currentTabSubtitle() {
    return this.tabTitles[this.activeTab()].subtitle;
  }

  get initials(): string {
    return this.fullName
      .trim()
      .split(/\s+/)
      .map((word) => word[0])
      .join('')
      .slice(0, 2)
      .toUpperCase();
  }

  get maxSales(): number {
    return Math.max(...this.weekSales.map((sale) => sale.value));
  }

  // ===================== TAB SWITCHING =====================

  switchTab(
    tab: 'overview' | 'products' | 'orders' | 'settings'
  ): void {
    this.activeTab.set(tab);
  }

  // ===================== PRODUCTS =====================

  addProduct(): void {
    this.switchTab('products');
  }

  editProduct(product: Product): void {
    this.showToast(`Editing "${product.name}"`);
  }

  deleteProduct(product: Product): void {
    this.sellerProducts = this.sellerProducts.filter(
      (item) => item.id !== product.id
    );

    this.showToast(`"${product.name}" removed from your shop`);
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

    const reader = new FileReader();

    reader.onload = () => {
      this.avatarUrl.set(reader.result as string);
      this.showToast('Profile photo updated');
    };

    reader.readAsDataURL(file);

    input.value = '';
  }

  removeAvatar(): void {
    this.avatarUrl.set(null);
    this.showToast('Profile photo removed');
  }

  // ===================== SETTINGS =====================

  saveSettings(): void {
    const fullName = this.fullName.trim();
    const shopName = this.shopName.trim();

    if (!fullName || !shopName) {
      this.showToast("Name and shop name can't be empty");
      return;
    }

    this.fullName = fullName;
    this.shopName = shopName;

    this.showSaveHint.set(true);

    setTimeout(() => {
      this.showSaveHint.set(false);
    }, 2500);

    this.showToast('Profile updated');
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
}
