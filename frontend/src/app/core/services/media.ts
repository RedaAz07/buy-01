import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Router } from '@angular/router';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

@Injectable({
  providedIn: 'root',
})
export class Media {
  private apiUrl = `${environment.apiUrl}/api/media/images`;
  private http = inject(HttpClient);
  private router = inject(Router);
  setAvatar(
    files: File[],
    type: string = 'AVATAR'
  ): Observable<any> {

    const formData = new FormData();

    files.forEach(file => {
      formData.append('media', file, file.name);
    });

    formData.append('type', type);

    return this.http.post<any>(
      this.apiUrl,
      formData
    );
  }



  deleteAvatar(id: string): Observable<any> {
    return this.http.delete<any>(
      `${this.apiUrl}/${id}`);
  }
}
