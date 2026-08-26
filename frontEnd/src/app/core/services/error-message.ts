import { HttpErrorResponse } from '@angular/common/http';
import { Injectable } from '@angular/core';
type FieldErrors = Record<string, string>;

@Injectable({
  providedIn: 'root',
})
export class ErrorMessage {
  getHttpErrorMessage(error: unknown, fallback = 'Something went wrong. Please try again.'): string {
    if (!(error instanceof HttpErrorResponse)) {
      return fallback;
    }

    if (error.status === 0) {
      return 'Network error. Please check your connection and try again.';
    }

    const payload = error.error;

    if (typeof payload === 'string' && payload.trim()) {
      return payload;
    }

    if (payload && typeof payload === 'object') {
      const data = payload as {
        message?: unknown;
        reason?: unknown;
        error?: unknown;
        fieldErrors?: unknown;
      };

      const message =
        typeof data.message === 'string' && data.message.trim() ? data.message.trim() : null;
      const reason = typeof data.reason === 'string' && data.reason.trim() ? data.reason.trim() : null;
      const errorLabel =
        typeof data.error === 'string' && data.error.trim() ? data.error.trim() : null;
      const fieldErrors = this.parseFieldErrors(data.fieldErrors);

      if (message && fieldErrors) {
        return `${message} (${fieldErrors})`;
      }
      if (message) {
        return message;
      }
      if (fieldErrors) {
        return fieldErrors;
      }
      if (reason) {
        return reason;
      }
      if (errorLabel) {
        return errorLabel;
      }
    }

    return error.message || fallback;
  }

  private parseFieldErrors(value: unknown): string | null {
    if (!value || typeof value !== 'object') {
      return null;
    }

    const entries = Object.entries(value as FieldErrors)
      .filter(([field, msg]) => Boolean(field) && typeof msg === 'string' && Boolean(msg.trim()))
      .map(([field, msg]) => `${field}: ${msg}`);

    return entries.length ? entries.join(' | ') : null;
  }
}
