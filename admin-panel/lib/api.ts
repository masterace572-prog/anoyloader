import { supabase } from "./supabase";
export async function clearAdminToken() {
  if (typeof window !== "undefined") {
    sessionStorage.removeItem("admin_session_token");
    sessionStorage.removeItem("admin_session_auth");
  }
  await supabase?.auth.signOut();
}
export class ApiError extends Error {
  constructor(
    message: string,
    public status: number,
  ) {
    super(message);
  }
}
export async function api<T = any>(
  url: string,
  init?: RequestInit,
): Promise<T> {
  const session = await supabase?.auth.getSession();
  const headers = new Headers(init?.headers);
  headers.set("Content-Type", "application/json");
  const token = session?.data.session?.access_token;
  if (token) headers.set("Authorization", `Bearer ${token}`);
  const res = await fetch(url, { cache: "no-store", ...init, headers });
  const data = await res.json().catch(() => ({}));
  if (!res.ok)
    throw new ApiError(
      data?.error || `Request failed (${res.status})`,
      res.status,
    );
  return data as T;
}
