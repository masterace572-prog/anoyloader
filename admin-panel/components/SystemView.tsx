'use client';

import React, { useEffect, useState } from 'react';
import { api } from '@/lib/api';
import { useFeedback } from './feedback';
import { Button, Card, ErrorBanner, Field, Input, Segmented, Textarea, Toggle } from './ui';

type AnnouncementType = 'info' | 'warning' | 'critical';

export function SystemView() {
  const { notify } = useFeedback();
  const [error, setError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);
  const [maintenanceMode, setMaintenanceMode] = useState(false);
  const [maintenanceMessage, setMaintenanceMessage] = useState(
    'Server is currently undergoing scheduled maintenance. Please check back soon.'
  );
  const [maintenanceEnd, setMaintenanceEnd] = useState('Soon');
  const [announcementActive, setAnnouncementActive] = useState(false);
  const [announcementTitle, setAnnouncementTitle] = useState('Notice');
  const [announcementMessage, setAnnouncementMessage] = useState('');
  const [announcementType, setAnnouncementType] = useState<AnnouncementType>('info');
  const [announcementLink, setAnnouncementLink] = useState('');

  const load = async () => {
    setError(null);
    try {
      const data = await fetch('/api/client/config', { cache: 'no-store' }).then((r) => r.json());
      setMaintenanceMode(!!data.maintenance_mode);
      setMaintenanceMessage(data.maintenance_message || maintenanceMessage);
      setMaintenanceEnd(data.maintenance_estimated_end || 'Soon');
      setAnnouncementActive(!!data.announcement_active);
      setAnnouncementTitle(data.announcement_title || 'Notice');
      setAnnouncementMessage(data.announcement_message || '');
      setAnnouncementType((data.announcement_type as AnnouncementType) || 'info');
      setAnnouncementLink(data.announcement_link || '');
    } catch (err: any) {
      setError(err?.message || 'Could not load system configuration.');
    }
  };

  useEffect(() => {
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
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
          announcement_active: announcementActive,
          announcement_title: announcementTitle,
          announcement_message: announcementMessage,
          announcement_type: announcementType,
          announcement_link: announcementLink,
        }),
      });
      notify('System configuration saved.');
    } catch (err: any) {
      notify(err?.message || 'Save failed.', 'err');
    } finally {
      setSaving(false);
    }
  };

  return (
    <form onSubmit={save} className="space-y-6">
      <div className="flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <h1 className="font-serif text-3xl text-ink">System</h1>
          <p className="mt-1 text-sm text-muted">Maintenance gate and launch announcements.</p>
        </div>
        <Button type="submit" loading={saving}>
          Save changes
        </Button>
      </div>

      {error ? <ErrorBanner message={error} onRetry={load} /> : null}

      <div className="grid gap-6 lg:grid-cols-2">
        <Card className="p-5 space-y-4">
          <h2 className="text-base text-ink">Maintenance</h2>
          <Toggle
            checked={maintenanceMode}
            onChange={setMaintenanceMode}
            label="Maintenance gate"
            description="Clients see a blocking notice and cannot authenticate."
          />
          <Field label="Message">
            <Textarea rows={4} value={maintenanceMessage} onChange={(e) => setMaintenanceMessage(e.target.value)} />
          </Field>
          <Field label="Estimated completion">
            <Input value={maintenanceEnd} onChange={(e) => setMaintenanceEnd(e.target.value)} />
          </Field>
        </Card>

        <Card className="p-5 space-y-4">
          <h2 className="text-base text-ink">Announcement</h2>
          <Toggle
            checked={announcementActive}
            onChange={setAnnouncementActive}
            label="Show on launch"
            description="Displays a modal the next time the app starts."
          />
          <Field label="Title">
            <Input value={announcementTitle} onChange={(e) => setAnnouncementTitle(e.target.value)} />
          </Field>
          <Field label="Message">
            <Textarea rows={4} value={announcementMessage} onChange={(e) => setAnnouncementMessage(e.target.value)} />
          </Field>
          <div>
            <div className="mb-1.5 text-sm text-muted">Severity</div>
            <Segmented
              value={announcementType}
              onChange={setAnnouncementType}
              options={[
                { value: 'info', label: 'Info' },
                { value: 'warning', label: 'Warning' },
                { value: 'critical', label: 'Critical' },
              ]}
            />
          </div>
          <Field label="Link" hint="Optional Telegram or website URL.">
            <Input
              value={announcementLink}
              onChange={(e) => setAnnouncementLink(e.target.value)}
              placeholder="https://t.me/…"
              className="font-mono"
            />
          </Field>
        </Card>
      </div>
    </form>
  );
}
