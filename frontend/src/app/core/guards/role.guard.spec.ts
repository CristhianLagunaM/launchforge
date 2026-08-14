import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { AuthStore } from '../auth/auth.store';
import { roleGuard } from './role.guard';

describe('roleGuard', () => {
  beforeEach(() => {
    localStorage.setItem(
      'launchforge.auth.session',
      JSON.stringify({
        accessToken: 'jwt-token',
        issuedAt: '2026-08-14T15:00:00Z',
        expiresAt: '2099-08-14T16:00:00Z',
        user: {
          id: '11111111-1111-1111-1111-111111111112',
          email: 'customer@launchforge.dev',
          firstName: 'Customer',
          lastName: 'LaunchForge',
          roles: ['CUSTOMER']
        }
      })
    );

    TestBed.configureTestingModule({
      providers: [provideRouter([])]
    });
  });

  it('redirects to forbidden when the authenticated user lacks the role', () => {
    const store = TestBed.inject(AuthStore);
    const router = TestBed.inject(Router);

    const result = TestBed.runInInjectionContext(() =>
      roleGuard({ data: { roles: ['ADMIN'] } } as never, {} as never)
    );

    expect(store.isAuthenticated()).toBe(true);
    expect(result).toEqual(router.createUrlTree(['/app/forbidden']));
  });
});
