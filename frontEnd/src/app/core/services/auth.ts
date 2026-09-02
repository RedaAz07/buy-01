import { inject, Injectable } from '@angular/core';
import { BehaviorSubject, Observable, tap } from 'rxjs';
import { UserProfileDTO } from '../models/user';
import { Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';
import { AuthResponseDTO, LoginRequestDTO, RegisterRequestDTO } from '../models/auth';

@Injectable({
  providedIn: 'root',
})
export class Auth {
  private apiUrl = `${environment.apiUrl}/api/auth`;
  private loggedInSubject = new BehaviorSubject<boolean>(false);
  public isLoggedIn$ = this.loggedInSubject.asObservable();
  private currentUserSubject = new BehaviorSubject<UserProfileDTO | null>(null);
  public currentUser$ = this.currentUserSubject.asObservable();
  constructor(
    private http: HttpClient,
    private router: Router,
  ) {
    const token = localStorage.getItem('jwt_token');
    if (token) {
      this.loggedInSubject.next(true);
      this.loadCurrentUser().subscribe({
        error: (e) => {
          console.log(e);

         // localStorage.removeItem('jwt_token');
          this.loggedInSubject.next(false);
        },
      });
    }
  }

  login(credentials: LoginRequestDTO): Observable<AuthResponseDTO> {
    return this.http.post<AuthResponseDTO>(`${this.apiUrl}/login`, credentials).pipe(
      tap((response : AuthResponseDTO ) => {
        localStorage.setItem('jwt_token', response.jwt);
        this.loggedInSubject.next(true);
        this.loadCurrentUser().subscribe({
          error: () => {
            localStorage.removeItem('jwt_token');
            this.loggedInSubject.next(false);
          },
        });
      }),
    );
  }

  logout(): void {
    localStorage.removeItem('jwt_token');
    this.loggedInSubject.next(false);
    this.currentUserSubject.next(null);
    this.router.navigate(['/login']);
  }

  register(userData: RegisterRequestDTO): Observable<AuthResponseDTO> {
    return this.http.post<AuthResponseDTO>(`${this.apiUrl}/register`, userData);
  }

  loadCurrentUser(): Observable<UserProfileDTO> {
    return this.http.get<UserProfileDTO>(`${environment.apiUrl}/api/users/me`).pipe(
      tap((user) => {
        
        this.currentUserSubject.next(user);
      }),
    );
  }


}
