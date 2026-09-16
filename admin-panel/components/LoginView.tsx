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
      <div className="flex min-h-screen items-center justify-center px-4">
        <div className="w-full max-w-sm rounded-2xl border border-line bg-surface p-6 text-center">
          <h1 className="text-xl text-ink">Setup required</h1>
          <p className="mt-2 text-sm text-muted">Set ADMIN_PIN on the server, then redeploy.</p>
        </div>
      </div>
    );
  }

  const submit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    const value = pin.trim();
    if (!value) {
      setError('PIN required');
      return;
    }
    setSubmitting(true);
    try {
      const data = await api<{ success: boolean; token?: string; error?: string }>('/api/admin/auth', {
        method: 'POST',
        body: JSON.stringify({ pin: value }),
      });
      if (!data.token) throw new Error(data.error || 'Could not sign in.');
      setAdminToken(data.token);
      onAuthenticated();
    } catch (err: any) {
      setError(err?.message || 'Could not sign in.');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="flex min-h-screen items-center justify-center px-4">
      <form onSubmit={submit} className="w-full max-w-sm space-y-4 rounded-2xl border border-line bg-surface p-6">
        <h1 className="text-center text-xl text-ink">Anoy</h1>
        <Field label="PIN" error={error || undefined}>
          <Input
            type="password"
            autoFocus
            autoComplete="current-password"
            maxLength={64}
            value={pin}
            onChange={(e) => setPin(e.target.value)}
            className="text-center font-mono tracking-[0.18em]"
          />
        </Field>
        <Button type="submit" className="w-full" loading={submitting}>
          Continue
        </Button>
      </form>
    </div>
  );
}
