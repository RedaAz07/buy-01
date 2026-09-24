import { inject, Injectable } from '@angular/core';
import { UpdateRequest, UpdateResponse } from '../models/user';
import { Observable } from 'rxjs';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';
import { Router } from '@angular/router';

@Injectable({
  providedIn: 'root',
})
export class User {
  private apiUrl = `${environment.apiUrl}/api/users/me`;
  private http = inject(HttpClient);
  private router = inject(Router);
  updateProfile(userData: UpdateRequest): Observable<UpdateResponse> {
    return this.http.put<UpdateResponse>(`${this.apiUrl}`, userData);
  }
}
