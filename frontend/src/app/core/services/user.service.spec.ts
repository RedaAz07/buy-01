import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { describe, it, expect, beforeEach, afterEach } from 'vitest';

import { User } from './user';
import { environment } from '../../../environments/environment';
import { UpdateRequest, UpdateResponse } from '../models/user';

describe('User Service', () => {
  let service: User;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        User,
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    });

    service = TestBed.inject(User);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  describe('updateProfile()', () => {
    it('should send a PUT request to update profile data', () => {
      const updateData: UpdateRequest = { name: 'Reda Updated', email: 'reda@test.com' };
      const mockResponse: UpdateResponse = { name: 'Reda Updated', email: 'reda@test.com', jwt: 'new-token' } as UpdateResponse;

      service.updateProfile(updateData).subscribe((response) => {
        expect(response).toEqual(mockResponse);
      });

      const req = httpMock.expectOne(`${environment.apiUrl}/api/users/me`);
      expect(req.request.method).toBe('PUT');
      expect(req.request.body).toEqual(updateData);

      req.flush(mockResponse);
    });
  });
});
