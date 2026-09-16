export function getAdminToken(): string | null {
  if (typeof window === 'undefined') return null;
  return sessionStorage.getItem('admin_session_token');
}

export function setAdminToken(token: string) {
  sessionStorage.setItem('admin_session_token', token);
  sessionStorage.removeItem('admin_session_auth');
}

export function clearAdminToken() {
  sessionStorage.removeItem('admin_session_token');
  sessionStorage.removeItem('admin_session_auth');
}

export function adminHeaders(extra?: HeadersInit): HeadersInit {
  const token = getAdminToken();
  return {
    'Content-Type': 'application/json',
    ...(token ? { Authorization: `Bearer ${token}`, 'x-admin-token': token } : {}),
    ...(extra || {}),
  };
}

export class ApiError extends Error {
  status: number;
  constructor(message: string, status: number) {
    super(message);
    this.status = status;
  }
}

export async function api<T = any>(url: string, init?: RequestInit): Promise<T> {
  const res = await fetch(url, {
    cache: 'no-store',
    ...init,
    headers: adminHeaders(init?.headers),
  });
  const data = await res.json().catch(() => ({}));
  if (!res.ok) {
    throw new ApiError(data?.error || `Request failed (${res.status})`, res.status);
  }
  return data as T;
}
