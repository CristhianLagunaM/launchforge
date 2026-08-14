import { computed, inject } from '@angular/core';
import { Router } from '@angular/router';
import { patchState, signalStore, withComputed, withHooks, withMethods, withState } from '@ngrx/signals';
import { firstValueFrom } from 'rxjs';
import { AuthApiService } from './auth-api.service';
import { AuthResponse, AuthUser, LoginRequest, ProblemDetails, RegisterRequest } from './auth.models';
import { clearSession, loadSession, saveSession } from './jwt-session.util';

type AuthStatus = 'idle' | 'loading' | 'authenticated' | 'error';

interface AuthState {
  accessToken: string | null;
  issuedAt: string | null;
  expiresAt: string | null;
  user: AuthUser | null;
  status: AuthStatus;
  error: string | null;
}

const initialState: AuthState = {
  accessToken: null,
  issuedAt: null,
  expiresAt: null,
  user: null,
  status: 'idle',
  error: null
};

export const AuthStore = signalStore(
  { providedIn: 'root' },
  withState(initialState),
  withComputed((store) => ({
    isAuthenticated: computed(() => !!store.accessToken() && !!store.user()),
    roles: computed(() => store.user()?.roles ?? []),
    displayName: computed(() => {
      const user = store.user();
      return user ? `${user.firstName} ${user.lastName}` : '';
    }),
    isAdmin: computed(() => (store.user()?.roles ?? []).includes('ADMIN'))
  })),
  withMethods((store, authApi = inject(AuthApiService), router = inject(Router)) => ({
    async login(payload: LoginRequest): Promise<void> {
      patchState(store, { status: 'loading', error: null });
      try {
        const response = await firstValueFrom(authApi.login(payload));
        persistAuthenticatedSession(store, response);
        await router.navigate(['/app']);
      } catch (error) {
        patchState(store, { status: 'error', error: extractProblemDetail(error, 'No fue posible iniciar sesión.') });
      }
    },
    async register(payload: RegisterRequest): Promise<void> {
      patchState(store, { status: 'loading', error: null });
      try {
        const response = await firstValueFrom(authApi.register(payload));
        persistAuthenticatedSession(store, response);
        await router.navigate(['/app']);
      } catch (error) {
        patchState(store, { status: 'error', error: extractProblemDetail(error, 'No fue posible crear la cuenta.') });
      }
    },
    clearError(): void {
      patchState(store, { error: null, status: store.accessToken() ? 'authenticated' : 'idle' });
    },
    logout(): void {
      clearSession();
      patchState(store, initialState);
      void router.navigate(['/login']);
    },
    handleUnauthorized(): void {
      clearSession();
      patchState(store, { ...initialState, error: 'La sesión expiró o el token ya no es válido.' });
      void router.navigate(['/login']);
    }
  })),
  withHooks({
    onInit(store) {
      const session = loadSession();
      if (!session) {
        return;
      }

      patchState(store, {
        accessToken: session.accessToken,
        issuedAt: session.issuedAt,
        expiresAt: session.expiresAt,
        user: session.user,
        status: 'authenticated',
        error: null
      });
    }
  })
);

function persistAuthenticatedSession(store: any, response: AuthResponse): void {
  saveSession(response);
  patchState(store, {
    accessToken: response.accessToken,
    issuedAt: response.issuedAt,
    expiresAt: response.expiresAt,
    user: response.user,
    status: 'authenticated',
    error: null
  });
}

function extractProblemDetail(error: unknown, fallback: string): string {
  const maybeProblem = (error as { error?: ProblemDetails })?.error;
  return maybeProblem?.detail ?? maybeProblem?.title ?? fallback;
}
