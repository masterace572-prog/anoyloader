'use client';

import React, { useState } from 'react';
import { api, setAdminToken } from '@/lib/api';
import { Button, Field, Input } from './ui';

export function LoginView({
  onAuthenticated,
  configured = true,
}: {
  onAuthenticated: () => void;
  configured?: boolean;
}) {
  const [pin, setPin] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  if (!configured) {
    return (
      <div className="flex min-h-screen items-center justify-center px-4 py-10">
        <div className="w-full max-w-[420px] rounded-2xl border border-line bg-surface p-6 text-center">
          <p className="text-xs text-muted">Anoy Control</p>
          <h1 className="mt-2 font-serif text-3xl text-ink">Setup required</h1>
          <p className="mt-3 text-sm text-muted">
            Set <span className="font-mono text-ink">ADMIN_PIN</span> in the server environment, then redeploy. The
            dashboard will not accept logins until that secret exists.
          </p>
        </div>
      </div>
    );
  }

  const submit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    const value = pin.trim();
    if (!value) {
      setError('Enter your administrator PIN.');
      return;
    }
    setSubmitting(true);
    try {
      const data = await api<{ success: boolean; token?: string; error?: string }>('/api/admin/auth', {
        method: 'POST',
        body: JSON.stringify({ pin: value }),
      });
      if (!data.token) throw new Error(data.error || 'Could not issue a session.');
      setAdminToken(data.token);
      onAuthenticated();
    } catch (err: any) {
      setError(err?.message || 'Could not reach the auth server.');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="flex min-h-screen items-center justify-center px-4 py-10">
      <div className="w-full max-w-[400px]">
        <div className="mb-8 text-center">
          <p className="text-xs text-muted">Anoy Control</p>
          <h1 className="mt-2 font-serif text-3xl text-ink">Sign in</h1>
          <p className="mt-2 text-sm text-muted">Enter the administrator PIN to manage licenses and updates.</p>
        </div>

        <form onSubmit={submit} className="space-y-4 rounded-2xl border border-line bg-surface p-6">
          <Field label="PIN" error={error || undefined}>
            <Input
              type="password"
              autoFocus
              autoComplete="current-password"
              maxLength={64}
              value={pin}
              onChange={(e) => setPin(e.target.value)}
              placeholder="Administrator PIN"
              className="text-center font-mono tracking-[0.2em]"
            />
          </Field>
          <Button type="submit" className="w-full" loading={submitting}>
            {submitting ? 'Verifying' : 'Continue'}
          </Button>
        </form>
      </div>
    </div>
  );
}
