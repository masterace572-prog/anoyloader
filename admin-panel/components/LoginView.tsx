"use client";
import React, { useState } from "react";
import Link from "next/link";
import { supabase } from "@/lib/supabase";
import { Button, Field, Input } from "./ui";
export function LoginView({
  onAuthenticated,
  configured = true,
  signup = false,
}: {
  onAuthenticated: () => void;
  configured?: boolean;
  signup?: boolean;
}) {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [message, setMessage] = useState("");
  const [busy, setBusy] = useState(false);
  async function submit(e: React.FormEvent) {
    e.preventDefault();
    setBusy(true);
    setMessage("");
    try {
      if (!supabase || !configured)
        throw new Error(
          "Account backend is not configured. Contact the administrator.",
        );
      const result = signup
        ? await supabase.auth.signUp({
            email,
            password,
            options: { emailRedirectTo: `${window.location.origin}/dashboard` },
          })
        : await supabase.auth.signInWithPassword({ email, password });
      if (result.error) throw result.error;
      if (signup)
        setMessage(
          "Account created. Confirm your email if requested, then ask an administrator to approve your account.",
        );
      else onAuthenticated();
    } catch (err: any) {
      setMessage(err.message || "Authentication failed.");
    } finally {
      setBusy(false);
    }
  }
  return (
    <div className="flex min-h-screen items-center justify-center px-4">
      <form
        onSubmit={submit}
        className="w-full max-w-md space-y-5 rounded-2xl border border-line bg-surface p-8"
      >
        <Link href="/" className="text-sm text-muted">
          ← Anoy home
        </Link>
        <h1 className="text-2xl">
          {signup ? "Create your account" : "Welcome back"}
        </h1>
        <p className="text-sm text-muted">
          {signup
            ? "Management access is available to approved administrators only."
            : "Sign in to your Anoy control workspace."}
        </p>
        <Field label="Email">
          <Input
            required
            type="email"
            autoComplete="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
          />
        </Field>
        <Field label="Password">
          <Input
            required
            type="password"
            minLength={signup ? 12 : 1}
            autoComplete={signup ? "new-password" : "current-password"}
            value={password}
            onChange={(e) => setPassword(e.target.value)}
          />
        </Field>
        {message && (
          <p role="status" className="text-sm text-muted">
            {message}
          </p>
        )}
        <Button type="submit" loading={busy} className="w-full">
          {signup ? "Create account" : "Sign in"}
        </Button>
        <p className="text-sm text-muted">
          {signup ? "Already registered?" : "New to Anoy?"}{" "}
          <Link
            className="text-ink underline"
            href={signup ? "/login" : "/signup"}
          >
            {signup ? "Sign in" : "Create an account"}
          </Link>
        </p>
      </form>
    </div>
  );
}
