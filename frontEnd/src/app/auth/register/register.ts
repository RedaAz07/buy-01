import { Component, inject } from '@angular/core';
import {
  AbstractControl,
  FormBuilder,
  FormGroup,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Router, RouterModule } from '@angular/router';
import { Auth } from '../../core/services/auth';
import { MatSelectModule } from '@angular/material/select';

@Component({
  selector: 'app-register',
  imports: [ReactiveFormsModule, MatIconModule, MatFormFieldModule, MatInputModule, RouterModule, MatSelectModule],
  templateUrl: './register.html',
  styleUrl: './register.css',
})
export class Register {
  registerForm: FormGroup;
  errorMessage: string = '';
  showPassword: boolean = false;
  snackbar = inject(MatSnackBar);
  fb = inject(FormBuilder);
  authService = inject(Auth);
  router = inject(Router);

  constructor() {
    this.registerForm = this.fb.group({
      name: [
        '',
        [
          Validators.required,
          Validators.pattern('^[a-zA-Z0-9]+$'),
          Validators.minLength(3),
          Validators.maxLength(15),
        ],
      ],

      email: ['', [Validators.required, Validators.email]],

      password: ['', [Validators.required, Validators.minLength(8), Validators.maxLength(20)]],

      role: ['', [Validators.required, Validators.pattern('ROLE_CLIENT|ROLE_SELLER')]],
    });
  }
  get name(): AbstractControl | null {
    return this.registerForm.get('name');
  }
  get email(): AbstractControl | null {
    return this.registerForm.get('email');
  }
  get password(): AbstractControl | null {
    return this.registerForm.get('password');
  }

  get role(): AbstractControl | null {
    return this.registerForm.get('role');
  }

  hasError(controlName: string, errorName: string): boolean {
    const control = this.registerForm.get(controlName);
    return !!control && control.hasError(errorName) && (control.touched || control.dirty);
  }

  onSubmit() {
    if (this.registerForm.invalid) {
      this.registerForm.markAllAsTouched();
      return;
    }

    const userData = this.registerForm.value;

    this.authService.register(userData).subscribe({
      next: (response) => {
        localStorage.setItem('jwt_token', response.jwt);
        this.router.navigate(['/home']);
        this.snackbar.open('Register successful!', 'Close', { duration: 3000 });

      },
      error: (err) => {
        const ErrorMessage = err?.error?.message || 'Registration failed. Please try again.';
        this.snackbar.open(ErrorMessage, 'Close', { duration: 3000 });
      },
    });
  }
}
