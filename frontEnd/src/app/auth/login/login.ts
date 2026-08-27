import { Component, inject } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Router, RouterModule } from '@angular/router';
import { Auth } from '../../core/services/auth';

@Component({
  selector: 'app-login',
  imports: [ReactiveFormsModule, MatIconModule, RouterModule],
  templateUrl: './login.html',
  styleUrl: './login.css',
})
export class Login {
  loginForm: FormGroup;

  errorMessage: string = '';

  showPassword: boolean = false;

  snackbar = inject(MatSnackBar);
  fb = inject(FormBuilder);
  authService = inject(Auth);
  router = inject(Router);
  constructor() {
    this.loginForm = this.fb.group({
      username: ['', [Validators.required]],
      password: ['', [Validators.required, Validators.minLength(6)]],
    });
  }

  onSubmit() {
    if (this.loginForm.invalid) {
      return;
    }

    const credentials = this.loginForm.value;

    this.authService.login(credentials).subscribe({
      next: (response) => {
        localStorage.setItem('jwt_token', response.jwt);

        this.router.navigate(['/home']);
        this.snackbar.open('Login successful!', 'Close', { duration: 3000 });
      },
      error: () => {
        const ErrorMessage = 'Login failed. Please try again.';
        this.snackbar.open(ErrorMessage, 'Close', { duration: 3000 });
      },
    });
  }
}
