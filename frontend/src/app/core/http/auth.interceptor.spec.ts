import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { AuthStore } from '../auth/auth.store';
import { authInterceptor } from './auth.interceptor';

describe('authInterceptor', () => {
  beforeEach(() => {
    localStorage.setItem(
      'launchforge.auth.session',
      JSON.stringify({
        accessToken: 'jwt-token',
        issuedAt: '2026-08-14T15:00:00Z',
        expiresAt: '2099-08-14T16:00:00Z',
        user: {
          id: '11111111-1111-1111-1111-111111111111',
          email: 'admin@launchforge.dev',
          firstName: 'Admin',
          lastName: 'LaunchForge',
          roles: ['ADMIN']
        }
      })
    );

    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting()
      ]
    });
  });

  it('adds bearer token to outgoing requests', () => {
    const httpClient = TestBed.inject(HttpClient);
    const httpTestingController = TestBed.inject(HttpTestingController);
    TestBed.inject(AuthStore);

    httpClient.get('/api/v1/admin/ping').subscribe();

    const request = httpTestingController.expectOne('/api/v1/admin/ping');
    expect(request.request.headers.get('Authorization')).toBe('Bearer jwt-token');
    request.flush({ status: 'admin-ok' });
    httpTestingController.verify();
  });
});
