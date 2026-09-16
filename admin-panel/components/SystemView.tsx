'use client';

import React, { useEffect, useState } from 'react';
import { api } from '@/lib/api';
import { useFeedback } from './feedback';
import { Button, Card, ErrorBanner, Field, Input, Textarea, Toggle } from './ui';

export function SystemView() {
  const { notify } = useFeedback();
  const [error, setError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);
  const [maintenanceMode, setMaintenanceMode] = useState(false);
  const [maintenanceMessage, setMaintenanceMessage] = useState(
    'The app is temporarily unavailable. Please try again soon.'
  );
  const [maintenanceEnd, setMaintenanceEnd] = useState('');

  const load = async () => {
    setError(null);
    try {
      const data = await fetch('/api/client/config', { cache: 'no-store' }).then((r) => r.json());
      setMaintenanceMode(!!data.maintenance_mode);
      if (data.maintenance_message) setMaintenanceMessage(data.maintenance_message);
      setMaintenanceEnd(data.maintenance_estimated_end || '');
    } catch (err: any) {
      setError(err?.message || 'Could not load settings.');
    }
  };

  useEffect(() => {
    load();
  }, []);

  const save = async (e: React.FormEvent) => {
    e.preventDefault();
    setSaving(true);
    try {
      await api('/api/client/config', {
        method: 'POST',
        body: JSON.stringify({
          maintenance_mode: maintenanceMode,
          maintenance_message: maintenanceMessage,
          maintenance_estimated_end: maintenanceEnd,
          announcement_active: false,
          announcement_title: '',
          announcement_message: '',
          announcement_type: 'info',
          announcement_link: '',
        }),
      });
      notify('Saved.');
    } catch (err: any) {
      notify(err?.message || 'Save failed.', 'err');
    } finally {
      setSaving(false);
    }
  };

  return (
    <form onSubmit={save} className="mx-auto max-w-xl space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl text-ink">Maintenance</h1>
        <Button type="submit" loading={saving}>
          Save
        </Button>
      </div>

      {error ? <ErrorBanner message={error} onRetry={load} /> : null}

      <Card className="space-y-4 p-5">
        <Toggle
          checked={maintenanceMode}
          onChange={setMaintenanceMode}
          label="Pause the app"
          description="Users cannot sign in until this is off."
        />
        <Field label="Message">
          <Textarea rows={3} value={maintenanceMessage} onChange={(e) => setMaintenanceMessage(e.target.value)} />
        </Field>
        <Field label="Until">
          <Input value={maintenanceEnd} onChange={(e) => setMaintenanceEnd(e.target.value)} placeholder="Optional" />
        </Field>
      </Card>
    </form>
  );
}
