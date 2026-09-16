'use client';

import React, { useEffect, useMemo, useState } from 'react';
import { Ban, Check, Copy, Plus, RefreshCw, Trash2, Unlock } from 'lucide-react';
import { api } from '@/lib/api';
import { DURATION_OPTIONS, DurationOption, KeyStatus, LicenseKey } from '@/lib/types';
import { useFeedback } from './feedback';
import {
  Button,
  Card,
  EmptyState,
  ErrorBanner,
  Field,
  Input,
  Modal,
  Segmented,
  Spinner,
  StatusText,
} from './ui';

function formatDate(iso: string | null) {
  if (!iso) return 'Lifetime';
  try {
    return new Date(iso).toLocaleString('en-US', {
      month: 'short',
      day: 'numeric',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    });
  } catch {
    return iso;
  }
}

function expiryLabel(k: LicenseKey) {
  if (k.expires_at) return formatDate(k.expires_at);
  if (k.duration_seconds === 0 || k.duration_label === 'Lifetime') return 'Lifetime';
  if (k.status === 'UNUSED') return `Starts on first use (${k.duration_label})`;
  return '—';
}

export function LicensesView({ onLiveChange }: { onLiveChange: (live: boolean) => void }) {
  const { notify, confirm } = useFeedback();
  const [keys, setKeys] = useState<LicenseKey[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [search, setSearch] = useState('');
  const [status, setStatus] = useState<'ALL' | KeyStatus>('ALL');
  const [copied, setCopied] = useState<string | null>(null);
  const [createOpen, setCreateOpen] = useState(false);
  const [extendTarget, setExtendTarget] = useState<LicenseKey | null>(null);
  const [bulkKeys, setBulkKeys] = useState<string[] | null>(null);
  const [busyId, setBusyId] = useState<string | null>(null);

  const load = async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await api<{ success: boolean; live?: boolean; keys: LicenseKey[] }>('/api/admin/keys');
      setKeys(data.keys || []);
      onLiveChange(!!data.live);
    } catch (err: any) {
      setError(err?.message || 'Could not load licenses.');
      onLiveChange(false);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const stats = useMemo(() => {
    return {
      total: keys.length,
      active: keys.filter((k) => k.status === 'ACTIVE').length,
      unused: keys.filter((k) => k.status === 'UNUSED').length,
      expired: keys.filter((k) => k.status === 'EXPIRED').length,
      banned: keys.filter((k) => k.status === 'BANNED').length,
    };
  }, [keys]);

  const filtered = useMemo(() => {
    const q = search.trim().toLowerCase();
    return keys.filter((k) => {
      const matchesStatus = status === 'ALL' || k.status === status;
      const matchesSearch =
        !q ||
        k.key.toLowerCase().includes(q) ||
        (k.notes || '').toLowerCase().includes(q) ||
        (k.last_ip || '').includes(q) ||
        k.hwid_list.some((h) => h.toLowerCase().includes(q));
      return matchesStatus && matchesSearch;
    });
  }, [keys, search, status]);

  const copy = async (text: string) => {
    await navigator.clipboard.writeText(text);
    setCopied(text);
    setTimeout(() => setCopied(null), 1600);
  };

  const mutate = async (action: string, payload: Record<string, unknown>, id?: string) => {
    setBusyId(id || 'global');
    try {
      const data = await api<{ success: boolean; key?: LicenseKey; keys?: LicenseKey[]; generated?: string[]; id?: string }>(
        '/api/admin/keys',
        { method: 'POST', body: JSON.stringify({ action, ...payload }) }
      );
      if (action === 'create' && data.key) setKeys((prev) => [data.key!, ...prev]);
      if (action === 'bulk' && data.keys) {
        setKeys((prev) => [...data.keys!, ...prev]);
        setBulkKeys(data.generated || data.keys.map((k) => k.key));
      }
      if (data.key && action !== 'create') {
        setKeys((prev) => prev.map((k) => (k.id === data.key!.id ? data.key! : k)));
      }
      if (action === 'delete' && payload.id) {
        setKeys((prev) => prev.filter((k) => k.id !== payload.id));
      }
      return data;
    } catch (err: any) {
      notify(err?.message || 'Action failed.', 'err');
      throw err;
    } finally {
      setBusyId(null);
    }
  };

  return (
    <div className="space-y-6">
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <h1 className="text-2xl text-ink">Keys</h1>
        <div className="flex gap-2">
          <Button variant="secondary" onClick={load} disabled={loading}>
            <RefreshCw className={`h-4 w-4 ${loading ? 'animate-spin' : ''}`} />
          </Button>
          <Button onClick={() => setCreateOpen(true)}>
            <Plus className="h-4 w-4" />
            New
          </Button>
        </div>
      </div>

      <div className="grid grid-cols-2 gap-3 lg:grid-cols-5">
        {[
          ['Total', stats.total],
          ['Active', stats.active],
          ['Unused', stats.unused],
          ['Expired', stats.expired],
          ['Banned', stats.banned],
        ].map(([label, value]) => (
          <Card key={String(label)} className="p-4">
            <div className="text-xs text-muted">{label}</div>
            <div className="mt-1 font-mono text-2xl text-ink">{value}</div>
          </Card>
        ))}
      </div>

      <Card className="p-4">
        <div className="flex flex-col gap-3 lg:flex-row lg:items-center">
          <Input
            placeholder="Search key, notes, IP, or HWID"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            className="lg:max-w-sm"
          />
          <div className="overflow-x-auto no-scrollbar">
            <Segmented
              value={status}
              onChange={setStatus}
              options={(['ALL', 'ACTIVE', 'UNUSED', 'EXPIRED', 'BANNED'] as const).map((s) => ({
                value: s,
                label: s === 'ALL' ? 'All' : s.charAt(0) + s.slice(1).toLowerCase(),
              }))}
            />
          </div>
        </div>
      </Card>

      {error ? <ErrorBanner message={error} onRetry={load} /> : null}

      <Card className="overflow-hidden">
        {loading ? (
          <div className="flex items-center justify-center gap-2 py-16 text-sm text-muted">
            <Spinner /> Loading licenses
          </div>
        ) : filtered.length === 0 ? (
          <EmptyState
            title="No licenses"
            message={search || status !== 'ALL' ? 'Nothing matches the current filters.' : 'Generate a key to get started.'}
            action={
              !search && status === 'ALL' ? (
                <Button onClick={() => setCreateOpen(true)}>New key</Button>
              ) : undefined
            }
          />
        ) : (
          <>
            <div className="divide-y divide-line md:hidden">
              {filtered.map((k) => (
                <KeyCard
                  key={k.id}
                  item={k}
                  copied={copied}
                  busy={busyId === k.id}
                  onCopy={copy}
                  onExtend={() => setExtendTarget(k)}
                  onReset={async () => {
                    const ok = await confirm({
                      title: 'Reset devices?',
                      message: `Clear HWID bindings for ${k.key}.`,
                      confirmLabel: 'Reset',
                    });
                    if (ok) {
                      await mutate('reset_hwid', { id: k.id }, k.id);
                      notify('Device bindings cleared.');
                    }
                  }}
                  onBan={async () => {
                    await mutate('toggle_ban', { id: k.id }, k.id);
                    notify(k.status === 'BANNED' ? 'Key restored.' : 'Key banned.');
                  }}
                  onDelete={async () => {
                    const ok = await confirm({
                      title: 'Delete key?',
                      message: `${k.key} will be permanently removed.`,
                      confirmLabel: 'Delete',
                      danger: true,
                    });
                    if (ok) {
                      await mutate('delete', { id: k.id }, k.id);
                      notify('Key deleted.');
                    }
                  }}
                />
              ))}
            </div>
            <div className="hidden overflow-x-auto md:block">
              <table className="w-full text-left text-sm">
                <thead className="border-b border-line bg-subtle text-xs text-muted">
                  <tr>
                    <th className="px-5 py-3 font-medium">Key</th>
                    <th className="px-4 py-3 font-medium">Duration</th>
                    <th className="px-4 py-3 font-medium">Devices</th>
                    <th className="px-4 py-3 font-medium">Status</th>
                    <th className="px-4 py-3 font-medium">Expires</th>
                    <th className="px-4 py-3 font-medium">Last IP</th>
                    <th className="px-5 py-3 text-right font-medium">Actions</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-line">
                  {filtered.map((k) => (
                    <tr key={k.id} className="hover:bg-subtle/60">
                      <td className="px-5 py-3">
                        <div className="flex items-center gap-2">
                          <span className="font-mono text-ink">{k.key}</span>
                          <button type="button" onClick={() => copy(k.key)} className="text-faint hover:text-ink">
                            {copied === k.key ? <Check className="h-3.5 w-3.5" /> : <Copy className="h-3.5 w-3.5" />}
                          </button>
                        </div>
                        {k.notes ? <div className="mt-0.5 truncate text-xs text-muted">{k.notes}</div> : null}
                      </td>
                      <td className="px-4 py-3 text-muted">{k.duration_label}</td>
                      <td className="px-4 py-3 font-mono text-muted">
                        {k.hwid_list.length}/{k.max_devices}
                      </td>
                      <td className="px-4 py-3">
                        <StatusText status={k.status} />
                      </td>
                      <td className="px-4 py-3 text-muted">{expiryLabel(k)}</td>
                      <td className="px-4 py-3 font-mono text-xs text-muted">{k.last_ip || '—'}</td>
                      <td className="px-5 py-3">
                        <div className="flex justify-end gap-1.5">
                          <Button size="sm" variant="secondary" onClick={() => setExtendTarget(k)}>
                            Extend
                          </Button>
                          <Button
                            size="sm"
                            variant="secondary"
                            disabled={busyId === k.id}
                            onClick={async () => {
                              const ok = await confirm({
                                title: 'Reset devices?',
                                message: `Clear HWID bindings for ${k.key}.`,
                                confirmLabel: 'Reset',
                              });
                              if (ok) {
                                await mutate('reset_hwid', { id: k.id }, k.id);
                                notify('Device bindings cleared.');
                              }
                            }}
                          >
                            Reset
                          </Button>
                          <Button
                            size="sm"
                            variant="ghost"
                            disabled={busyId === k.id}
                            onClick={async () => {
                              await mutate('toggle_ban', { id: k.id }, k.id);
                              notify(k.status === 'BANNED' ? 'Key restored.' : 'Key banned.');
                            }}
                          >
                            {k.status === 'BANNED' ? <Unlock className="h-4 w-4" /> : <Ban className="h-4 w-4" />}
                          </Button>
                          <Button
                            size="sm"
                            variant="danger"
                            disabled={busyId === k.id}
                            onClick={async () => {
                              const ok = await confirm({
                                title: 'Delete key?',
                                message: `${k.key} will be permanently removed.`,
                                confirmLabel: 'Delete',
                                danger: true,
                              });
                              if (ok) {
                                await mutate('delete', { id: k.id }, k.id);
                                notify('Key deleted.');
                              }
                            }}
                          >
                            <Trash2 className="h-4 w-4" />
                          </Button>
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </>
        )}
      </Card>

      <CreateKeyModal
        open={createOpen}
        onClose={() => setCreateOpen(false)}
        onCreate={async (payload) => {
          await mutate(payload.mode, payload.body);
          setCreateOpen(false);
          notify(payload.mode === 'bulk' ? 'Keys generated.' : 'Key created.');
        }}
      />

      <ExtendModal
        target={extendTarget}
        onClose={() => setExtendTarget(null)}
        onConfirm={async (seconds) => {
          if (!extendTarget) return;
          await mutate('extend', { id: extendTarget.id, seconds }, extendTarget.id);
          setExtendTarget(null);
          notify('Key extended.');
        }}
      />

      <Modal
        open={!!bulkKeys}
        onClose={() => setBulkKeys(null)}
        title="Keys generated"
        description="Copy and store these keys securely."
      >
        <pre className="max-h-64 overflow-auto rounded-lg border border-line bg-subtle p-3 font-mono text-xs text-ink">
          {(bulkKeys || []).join('\n')}
        </pre>
        <div className="mt-4 flex gap-2">
          <Button
            className="flex-1"
            onClick={() => bulkKeys && copy(bulkKeys.join('\n'))}
          >
            Copy all
          </Button>
          <Button variant="secondary" className="flex-1" onClick={() => setBulkKeys(null)}>
            Close
          </Button>
        </div>
      </Modal>
    </div>
  );
}

function KeyCard({
  item,
  copied,
  busy,
  onCopy,
  onExtend,
  onReset,
  onBan,
  onDelete,
}: {
  item: LicenseKey;
  copied: string | null;
  busy: boolean;
  onCopy: (v: string) => void;
  onExtend: () => void;
  onReset: () => void;
  onBan: () => void;
  onDelete: () => void;
}) {
  return (
    <div className="space-y-3 p-4">
      <div className="flex items-start justify-between gap-3">
        <div className="min-w-0">
          <div className="flex items-center gap-2">
            <span className="break-all font-mono text-sm text-ink">{item.key}</span>
            <button type="button" onClick={() => onCopy(item.key)} className="text-faint hover:text-ink">
              {copied === item.key ? <Check className="h-3.5 w-3.5" /> : <Copy className="h-3.5 w-3.5" />}
            </button>
          </div>
          {item.notes ? <p className="mt-1 truncate text-xs text-muted">{item.notes}</p> : null}
        </div>
        <StatusText status={item.status} />
      </div>
      <div className="grid grid-cols-2 gap-2 rounded-lg border border-line bg-subtle p-3 text-xs text-muted">
        <div>Duration · {item.duration_label}</div>
        <div className="font-mono">
          Devices · {item.hwid_list.length}/{item.max_devices}
        </div>
        <div className="col-span-2">Expires · {expiryLabel(item)}</div>
      </div>
      <div className="flex gap-1.5">
        <Button size="sm" variant="secondary" className="flex-1" onClick={onExtend}>
          Extend
        </Button>
        <Button size="sm" variant="secondary" className="flex-1" disabled={busy} onClick={onReset}>
          Reset
        </Button>
        <Button size="sm" variant="ghost" disabled={busy} onClick={onBan}>
          {item.status === 'BANNED' ? <Unlock className="h-4 w-4" /> : <Ban className="h-4 w-4" />}
        </Button>
        <Button size="sm" variant="danger" disabled={busy} onClick={onDelete}>
          <Trash2 className="h-4 w-4" />
        </Button>
      </div>
    </div>
  );
}

function CreateKeyModal({
  open,
  onClose,
  onCreate,
}: {
  open: boolean;
  onClose: () => void;
  onCreate: (payload: { mode: 'create' | 'bulk'; body: Record<string, unknown> }) => Promise<void>;
}) {
  const [mode, setMode] = useState<'single' | 'bulk'>('single');
  const [customKey, setCustomKey] = useState('');
  const [bulkCount, setBulkCount] = useState(5);
  const [duration, setDuration] = useState<DurationOption>(DURATION_OPTIONS[5]);
  const [maxDevices, setMaxDevices] = useState(1);
  const [notes, setNotes] = useState('');
  const [timing, setTiming] = useState<'ON_FIRST_USE' | 'IMMEDIATE'>('ON_FIRST_USE');
  const [saving, setSaving] = useState(false);
  const [formError, setFormError] = useState<string | null>(null);

  const submit = async (e: React.FormEvent) => {
    e.preventDefault();
    setFormError(null);
    if (mode === 'single' && customKey.trim() && !/^[A-Za-z0-9][A-Za-z0-9\-_]{2,63}$/.test(customKey.trim())) {
      setFormError('Custom key must be 3–64 characters: letters, numbers, hyphen, underscore.');
      return;
    }
    setSaving(true);
    try {
      await onCreate({
        mode: mode === 'single' ? 'create' : 'bulk',
        body: {
          key: customKey,
          count: bulkCount,
          duration_label: duration.label,
          duration_seconds: duration.seconds,
          max_devices: maxDevices,
          notes,
          activation_timing: timing,
        },
      });
      setCustomKey('');
      setNotes('');
    } catch (err: any) {
      setFormError(err?.message || 'Could not create key.');
    } finally {
      setSaving(false);
    }
  };

  return (
    <Modal
      open={open}
      onClose={onClose}
      title="New license"
      description="Configure duration and device limit."
    >
      <form onSubmit={submit} className="space-y-4">
        <Segmented
          value={mode}
          onChange={setMode}
          options={[
            { value: 'single', label: 'Single' },
            { value: 'bulk', label: 'Bulk' },
          ]}
        />
        {mode === 'single' ? (
          <Field label="Custom key" hint="Leave empty to auto-generate.">
            <Input
              value={customKey}
              onChange={(e) => setCustomKey(e.target.value)}
              placeholder="ANOY-XXXX-XXXX-XXXX"
              className="font-mono"
            />
          </Field>
        ) : (
          <Field label="Number of keys">
            <Input
              type="number"
              min={1}
              max={50}
              value={bulkCount}
              onChange={(e) => setBulkCount(Math.min(50, Math.max(1, parseInt(e.target.value, 10) || 1)))}
            />
          </Field>
        )}

        <div>
          <div className="mb-1.5 text-sm text-muted">Duration</div>
          <div className="grid grid-cols-3 gap-1.5 sm:grid-cols-4">
            {DURATION_OPTIONS.map((opt) => (
              <button
                key={opt.label}
                type="button"
                onClick={() => setDuration(opt)}
                className={`rounded-lg border px-2 py-2 text-sm ${
                  duration.label === opt.label
                    ? 'border-ink bg-elevated text-ink'
                    : 'border-line bg-subtle text-muted hover:text-ink'
                }`}
              >
                {opt.label}
              </button>
            ))}
          </div>
        </div>

        {duration.seconds > 0 ? (
          <div>
            <div className="mb-1.5 text-sm text-muted">Timer starts</div>
            <div className="grid grid-cols-2 gap-2">
              {(
                [
                  ['ON_FIRST_USE', 'On first login', 'Countdown begins when the user authenticates.'],
                  ['IMMEDIATE', 'Immediately', 'Countdown starts now.'],
                ] as const
              ).map(([value, title, hint]) => (
                <button
                  key={value}
                  type="button"
                  onClick={() => setTiming(value)}
                  className={`rounded-lg border p-3 text-left ${
                    timing === value ? 'border-ink bg-elevated' : 'border-line bg-subtle'
                  }`}
                >
                  <div className="text-sm text-ink">{title}</div>
                  <div className="mt-0.5 text-xs text-muted">{hint}</div>
                </button>
              ))}
            </div>
          </div>
        ) : null}

        <Field label="Max devices">
          <Input
            type="number"
            min={1}
            max={10}
            value={maxDevices}
            onChange={(e) => setMaxDevices(Math.min(10, Math.max(1, parseInt(e.target.value, 10) || 1)))}
          />
        </Field>
        <Field label="Notes">
          <Input value={notes} onChange={(e) => setNotes(e.target.value)} placeholder="Optional internal note" />
        </Field>
        {formError ? <p className="text-sm text-danger">{formError}</p> : null}
        <div className="flex gap-2 pt-2">
          <Button type="button" variant="secondary" className="flex-1" onClick={onClose}>
            Cancel
          </Button>
          <Button type="submit" className="flex-1" loading={saving}>
            {mode === 'single' ? 'Generate key' : `Generate ${bulkCount}`}
          </Button>
        </div>
      </form>
    </Modal>
  );
}

function ExtendModal({
  target,
  onClose,
  onConfirm,
}: {
  target: LicenseKey | null;
  onClose: () => void;
  onConfirm: (seconds: number) => Promise<void>;
}) {
  const [seconds, setSeconds] = useState(86400);
  const [saving, setSaving] = useState(false);
  const options = [
    { label: '+1 day', secs: 86400 },
    { label: '+3 days', secs: 86400 * 3 },
    { label: '+7 days', secs: 86400 * 7 },
    { label: '+30 days', secs: 86400 * 30 },
  ];

  return (
    <Modal
      open={!!target}
      onClose={onClose}
      title="Extend key"
      description={target ? target.key : undefined}
    >
      <div className="grid grid-cols-2 gap-2">
        {options.map((opt) => (
          <button
            key={opt.label}
            type="button"
            onClick={() => setSeconds(opt.secs)}
            className={`rounded-lg border py-2 text-sm ${
              seconds === opt.secs ? 'border-ink bg-elevated text-ink' : 'border-line bg-subtle text-muted'
            }`}
          >
            {opt.label}
          </button>
        ))}
      </div>
      <div className="mt-4 flex gap-2">
        <Button variant="secondary" className="flex-1" onClick={onClose}>
          Cancel
        </Button>
        <Button
          className="flex-1"
          loading={saving}
          onClick={async () => {
            setSaving(true);
            try {
              await onConfirm(seconds);
            } finally {
              setSaving(false);
            }
          }}
        >
          Confirm
        </Button>
      </div>
    </Modal>
  );
}
