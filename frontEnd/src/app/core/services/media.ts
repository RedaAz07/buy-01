import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Router } from '@angular/router';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root',
})
export class Media {
  private apiUrl = 'http://localhost:8080/media/images';
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
}
