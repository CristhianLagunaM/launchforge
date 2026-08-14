import { AuthResponse, AuthUser } from './auth.models';

const STORAGE_KEY = 'launchforge.auth.session';

export interface StoredSession {
  accessToken: string;
  expiresAt: string;
  issuedAt: string;
  user: AuthUser;
}

export function saveSession(response: AuthResponse): void {
  const session: StoredSession = {
    accessToken: response.accessToken,
    issuedAt: response.issuedAt,
    expiresAt: response.expiresAt,
    user: response.user
  };
  localStorage.setItem(STORAGE_KEY, JSON.stringify(session));
}

export function clearSession(): void {
  localStorage.removeItem(STORAGE_KEY);
}

export function loadSession(): StoredSession | null {
  const raw = localStorage.getItem(STORAGE_KEY);
  if (!raw) {
    return null;
  }

  try {
    const session = JSON.parse(raw) as StoredSession;
    if (!session.accessToken || isExpired(session.expiresAt)) {
      clearSession();
      return null;
    }
    return session;
  } catch {
    clearSession();
    return null;
  }
}

export function isExpired(expiresAt: string): boolean {
  return new Date(expiresAt).getTime() <= Date.now();
}
