import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { Dashboard } from './dashboard';
import { Auth } from '../core/services/auth';
import { Product } from '../core/services/product';
import { User } from '../core/services/user';
import { Media } from '../core/services/media';
import { FormBuilder } from '@angular/forms';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { BehaviorSubject, of, throwError } from 'rxjs';
import { describe, it, expect, beforeEach, vi, afterEach } from 'vitest';

describe('Dashboard Component', () => {
  let component: Dashboard;
  let fixture: ComponentFixture<Dashboard>;

  // Service Mocks
  let mockAuth: { currentUser$: BehaviorSubject<any>, logout: ReturnType<typeof vi.fn> };
  let mockProduct: { getMyproduct: ReturnType<typeof vi.fn> };
  let mockUser: { updateProfile: ReturnType<typeof vi.fn> };
  let mockMedia: { setAvatar: ReturnType<typeof vi.fn> };

  // Spies
  let setItemSpy: ReturnType<typeof vi.spyOn>;
  let removeItemSpy: ReturnType<typeof vi.spyOn>;

  beforeEach(async () => {
    const mockIntersectionObserver = vi.fn();
    mockIntersectionObserver.mockReturnValue({
      observe: () => null,
      unobserve: () => null,
      disconnect: () => null
    });
    window.IntersectionObserver = mockIntersectionObserver;

    // 2. Initialize Service Mocks
    mockAuth = {
      currentUser$: new BehaviorSubject(null),
      logout: vi.fn()
    };
    mockProduct = {
      getMyproduct: vi.fn().mockReturnValue(of({ content: [] }))
    };
    mockUser = {
      updateProfile: vi.fn()
    };
    mockMedia = {
      setAvatar: vi.fn()
    };

    await TestBed.configureTestingModule({
      imports: [Dashboard],
      providers: [
        FormBuilder,
        provideNoopAnimations(),
        { provide: Auth, useValue: mockAuth },
        { provide: Product, useValue: mockProduct },
        { provide: User, useValue: mockUser },
        { provide: Media, useValue: mockMedia },
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(Dashboard);
    component = fixture.componentInstance;

    setItemSpy = vi.spyOn(Storage.prototype, 'setItem');
    removeItemSpy = vi.spyOn(Storage.prototype, 'removeItem');
  });

  afterEach(() => {
    vi.clearAllMocks();
  });

  describe('Initialization & User Subscription', () => {
    it('should patch form and NOT load products if user is ROLE_CLIENT', () => {
      const clientUser = { name: 'John', email: 'john@test.com', role: 'ROLE_CLIENT' };
      mockAuth.currentUser$.next(clientUser);

      fixture.detectChanges();

      expect(component.user()).toEqual(clientUser);
      expect(component.settingsForm.value).toEqual({ name: 'John', email: 'john@test.com' });
      expect(mockProduct.getMyproduct).not.toHaveBeenCalled();
    });

    it('should patch form and load products if user is ROLE_SELLER', () => {
      const sellerUser = { name: 'Seller', email: 'seller@test.com', role: 'ROLE_SELLER' };
      mockAuth.currentUser$.next(sellerUser);

      fixture.detectChanges();

      expect(mockProduct.getMyproduct).toHaveBeenCalledWith(0, 10);
      expect(component.currentPage).toBe(1);
    });
  });

  describe('Tab Switching', () => {
    beforeEach(() => {
      fixture.detectChanges();
    });

    it('should prevent ROLE_CLIENT from accessing products tab', () => {
      component.user.set({ role: 'ROLE_CLIENT' } as any);

      component.switchTab('products');

      expect(component.activeTab()).toBe('settings');
    });

    it('should allow ROLE_SELLER to access products tab', () => {
      component.user.set({ role: 'ROLE_SELLER' } as any);

      component.switchTab('products');

      expect(component.activeTab()).toBe('products');
    });
  });

  describe('Settings Form & Update Profile', () => {
    beforeEach(() => {
      fixture.detectChanges();
      component.user.set({ name: 'Old', email: 'old@test.com' } as any);
    });

    it('should not call update API if form is invalid', () => {
      component.settingsForm.patchValue({ name: 'ab' });
      component.saveSettings();
      expect(mockUser.updateProfile).not.toHaveBeenCalled();
    });

    it('should call update API, update tokens, and show toast on success', () => {
      vi.useFakeTimers();

      component.settingsForm.patchValue({ name: 'NewName', email: 'new@test.com' });

      const updatedResponse = { name: 'NewName', email: 'new@test.com', jwt: 'new-token' };
      mockUser.updateProfile.mockReturnValue(of(updatedResponse));

      component.saveSettings();

      expect(mockUser.updateProfile).toHaveBeenCalledWith({ name: 'NewName', email: 'new@test.com' });
      expect(removeItemSpy).toHaveBeenCalledWith('jwt_token');
      expect(setItemSpy).toHaveBeenCalledWith('jwt_token', 'new-token');
      expect(component.user()?.name).toBe('NewName');

      expect(component.toastMessage()).toBe('Profile updated successfully');
      expect(component.showToastMessage()).toBe(true);

      vi.advanceTimersByTime(2200);

      expect(component.showToastMessage()).toBe(false);

      vi.useRealTimers();
    });
  });

  describe('Avatar Upload', () => {
    let mockInput: HTMLInputElement;
    let mockEvent: any;

    beforeEach(() => {
      fixture.detectChanges();
      mockInput = document.createElement('input');
      mockEvent = { target: mockInput };
      component.user.set({ avatar: 'old-url' } as any);
    });

    it('should reject non-image files', () => {
      const file = new File(['content'], 'test.txt', { type: 'text/plain' });
      Object.defineProperty(mockInput, 'files', { value: [file] });

      component.onAvatarSelected(mockEvent);

      expect(mockMedia.setAvatar).not.toHaveBeenCalled();
      expect(component.toastMessage()).toBe('Please choose an image file');
    });

    it('should reject files larger than 2MB', () => {
      // 3MB file
      const file = new File([''], 'big.jpg', { type: 'image/jpeg' });
      Object.defineProperty(file, 'size', { value: 3 * 1024 * 1024 });
      Object.defineProperty(mockInput, 'files', { value: [file] });

      component.onAvatarSelected(mockEvent);

      expect(mockMedia.setAvatar).not.toHaveBeenCalled();
      expect(component.toastMessage()).toBe('Image size cannot be bigger than 2MB');
    });

    it('should upload valid image and update user signal', () => {
      const file = new File([''], 'valid.png', { type: 'image/png' });
      Object.defineProperty(file, 'size', { value: 1024 }); // 1KB
      Object.defineProperty(mockInput, 'files', { value: [file] });

      mockMedia.setAvatar.mockReturnValue(of(['new-avatar-url.png']));

      component.onAvatarSelected(mockEvent);

      expect(mockMedia.setAvatar).toHaveBeenCalledWith([file]);
      expect(component.user()?.avatar).toBe('new-avatar-url.png');
      expect(component.toastMessage()).toBe('Profile photo updated');
    });
  });

  describe('Product List Management', () => {
    beforeEach(() => {
      fixture.detectChanges();
      component.sellerProducts.set([
        { id: '1', name: 'Product A' } as any,
        { id: '2', name: 'Product B' } as any
      ]);
    });

    it('should add a new product', () => {
      component.onProductCreated({ id: '3', name: 'Product C' } as any);
      expect(component.sellerProducts().length).toBe(3);
    });

    it('should update an existing product', () => {
      component.onProductUpdated({ id: '1', name: 'Product A Updated' } as any);
      expect(component.sellerProducts()[0].name).toBe('Product A Updated');
    });

    it('should delete a product', () => {
      component.onProductDeleted('2');
      expect(component.sellerProducts().length).toBe(1);
      expect(component.sellerProducts()[0].id).toBe('1');
    });
  });
});
