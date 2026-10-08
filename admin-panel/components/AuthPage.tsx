"use client";
import { LoginView } from "./LoginView";
export default function AuthPage({ signup = false }: { signup?: boolean }) {
  return (
    <LoginView
      signup={signup}
      onAuthenticated={() => window.location.assign("/dashboard")}
    />
  );
}
