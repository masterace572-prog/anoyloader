"use client";
import React, { useState } from "react";
import Link from "next/link";
import {
  ArrowRight,
  CheckCircle2,
  Eye,
  EyeOff,
  KeyRound,
  ShieldCheck,
  AlertCircle,
} from "lucide-react";
import { supabase } from "@/lib/supabase";
import { Button, Field, Input } from "./ui";
import { Brand } from "./Brand";
import { ThemeToggle } from "./ThemeToggle";
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
  const [visible, setVisible] = useState(false);
  const [message, setMessage] = useState("");
  const [success, setSuccess] = useState(false);
  const [busy, setBusy] = useState(false);
  async function submit(e: React.FormEvent) {
    e.preventDefault();
    setBusy(true);
    setMessage("");
    setSuccess(false);
    try {
      if (!supabase || !configured)
        throw new Error(
          "Account backend is not configured. Contact the administrator.",
        );
      const result = signup
        ? await supabase.auth.signUp({
            email: email.trim(),
            password,
            options: { emailRedirectTo: `${window.location.origin}/dashboard` },
          })
        : await supabase.auth.signInWithPassword({
            email: email.trim(),
            password,
          });
      if (result.error) throw result.error;
      if (signup) {
        setSuccess(true);
        setMessage(
          "Account created. Check your inbox to confirm your email, then ask an administrator to approve access.",
        );
      } else onAuthenticated();
    } catch (err: any) {
      setMessage(err.message || "Authentication failed. Please try again.");
    } finally {
      setBusy(false);
    }
  }
  return (
    <div className="min-h-screen">
      <header className="mx-auto flex max-w-6xl items-center justify-between px-6 py-6">
        <Brand compact />
        <ThemeToggle />
      </header>
      <main className="mx-auto grid max-w-6xl gap-12 px-5 pb-16 pt-8 sm:px-8 lg:grid-cols-2 lg:items-center lg:py-16">
        <section className="hidden lg:block">
          <p className="mb-5 text-xs font-semibold uppercase tracking-[0.2em] text-accent">
            Your workspace, connected
          </p>
          <h2 className="max-w-lg text-5xl font-semibold leading-[1.12]">
            Less busywork.
            <br />
            <span className="text-accent">More control.</span>
          </h2>
          <p className="mt-6 max-w-sm leading-relaxed text-muted">
            One place to manage access, ship updates, and build integrations
            that work for you.
          </p>
          <div className="mt-10 space-y-5">
            {[
              [
                KeyRound,
                "Every license, organized",
                "Create keys and manage subscriptions with confidence.",
              ],
              [
                ShieldCheck,
                "Access on your terms",
                "Approved accounts and revocable API credentials.",
              ],
            ].map(([Icon, title, detail]: any) => (
              <div key={title} className="flex gap-4">
                <span className="flex h-11 w-11 shrink-0 items-center justify-center rounded-xl border border-line bg-surface text-accent">
                  <Icon size={20} />
                </span>
                <div>
                  <h3 className="text-sm font-semibold">{title}</h3>
                  <p className="mt-1 text-sm text-muted">{detail}</p>
                </div>
              </div>
            ))}
          </div>
        </section>
        <section className="mx-auto w-full max-w-md rounded-3xl border border-line bg-surface p-6 shadow-xl sm:p-9">
          <div className="mb-6 flex h-12 w-12 items-center justify-center rounded-2xl bg-accent/10 text-accent">
            <ShieldCheck size={24} />
          </div>
          <h1 className="text-3xl font-semibold">
            {signup ? "Create an account" : "Welcome back"}
          </h1>
          <p className="mt-3 text-sm leading-relaxed text-muted">
            {signup
              ? "Set up your account to request access to Anoy Control."
              : "Sign in to continue to your control workspace."}
          </p>
          <form onSubmit={submit} className="mt-7 space-y-5">
            <Field label="Email address">
              <Input
                required
                disabled={busy}
                type="email"
                autoComplete="email"
                placeholder="you@example.com"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
              />
            </Field>
            <Field
              label="Password"
              hint={signup ? "Use at least 12 characters." : undefined}
            >
              <div className="relative">
                <Input
                  required
                  disabled={busy}
                  type={visible ? "text" : "password"}
                  minLength={signup ? 12 : 1}
                  autoComplete={signup ? "new-password" : "current-password"}
                  placeholder={
                    signup ? "Create a strong password" : "Enter your password"
                  }
                  className="pr-12"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                />
                <button
                  type="button"
                  aria-label={visible ? "Hide password" : "Show password"}
                  aria-pressed={visible}
                  onClick={() => setVisible(!visible)}
                  className="absolute right-0 top-0 flex h-11 w-11 items-center justify-center text-muted hover:text-ink"
                >
                  {visible ? <EyeOff size={18} /> : <Eye size={18} />}
                </button>
              </div>
            </Field>
            {message && (
              <div
                role={success ? "status" : "alert"}
                className={`flex items-start gap-2.5 rounded-xl border p-3 text-sm leading-relaxed ${success ? "border-success/25 bg-success/10 text-success" : "border-danger/25 bg-danger/10 text-danger"}`}
              >
                {success ? (
                  <CheckCircle2 size={18} className="mt-0.5 shrink-0" />
                ) : (
                  <AlertCircle size={18} className="mt-0.5 shrink-0" />
                )}
                <span>{message}</span>
              </div>
            )}
            <Button
              type="submit"
              loading={busy}
              disabled={signup && success}
              className="w-full"
            >
              {busy
                ? signup
                  ? "Creating account…"
                  : "Signing in…"
                : signup
                  ? "Create account"
                  : "Sign in"}{" "}
              {!busy && <ArrowRight size={17} />}
            </Button>
          </form>
          <p className="mt-6 text-center text-sm text-muted">
            {signup ? "Already have an account?" : "New to Anoy?"}{" "}
            <Link
              href={signup ? "/login" : "/signup"}
              className="font-medium text-accent hover:underline"
            >
              {signup ? "Sign in" : "Create an account"}
            </Link>
          </p>
          <div className="mt-6 border-t border-line pt-5 text-xs leading-relaxed text-muted">
            <ShieldCheck size={14} className="mr-1 inline text-accent" />
            Administrator approval is required for management access. App
            license keys are separate from this account.
          </div>
        </section>
      </main>
      <footer className="pb-8 text-center text-xs text-muted">
        <Link href="/" className="hover:text-ink">
          ← Back to home
        </Link>
        <span className="px-3">·</span>
        <Link href="/docs" className="hover:text-ink">
          API documentation
        </Link>
      </footer>
    </div>
  );
}
