import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { authGuard } from './auth.guard';

describe('authGuard', () => {
  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({ providers: [provideRouter([])] });
  });

  it('redirects an anonymous user to login and preserves the destination', () => {
    const router = TestBed.inject(Router);
    const result = TestBed.runInInjectionContext(() => authGuard({} as never, { url: '/orders' } as never));
    expect(result).toEqual(router.createUrlTree(['/login'], { queryParams: { redirectUrl: '/orders' } }));
  });
});
