'use client';

import React, { useEffect, useState } from 'react';
import { api } from '@/lib/api';
import { useFeedback } from './feedback';
import { Button, Card, ErrorBanner, Field, Input, Textarea, Toggle } from './ui';

export function UpdatesView() {
  const { notify } = useFeedback();
  const [error, setError] = useState<string | null>(null);
  const [libVersion, setLibVersion] = useState('');
  const [libUrl, setLibUrl] = useState('');
  const [libActive, setLibActive] = useState({ version: '', url: '', updated: '' });
  const [apkName, setApkName] = useState('');
  const [apkCode, setApkCode] = useState('');
  const [apkUrl, setApkUrl] = useState('');
  const [apkChangelog, setApkChangelog] = useState('');
  const [apkMandatory, setApkMandatory] = useState(false);
  const [apkActive, setApkActive] = useState({
    version_name: '',
    version_code: 0,
    download_url: '',
    changelog: '',
    is_mandatory: false,
  });
  const [savingLib, setSavingLib] = useState(false);
  const [savingApk, setSavingApk] = useState(false);

  const load = async () => {
    setError(null);
    try {
      const [lib, apk] = await Promise.all([
        fetch('/api/client/lib-update', { cache: 'no-store' }).then((r) => r.json()),
        fetch('/api/client/app-update', { cache: 'no-store' }).then((r) => r.json()),
      ]);
      setLibActive({
        version: lib.version || '',
        url: lib.download_url || '',
        updated: lib.updated_at || '',
      });
      setApkActive({
        version_name: apk.version_name || '',
        version_code: apk.version_code || 0,
        download_url: apk.download_url || '',
        changelog: apk.changelog || '',
        is_mandatory: !!apk.is_mandatory,
      });
    } catch (err: any) {
      setError(err?.message || 'Could not load update channels.');
    }
  };

  useEffect(() => {
    load();
  }, []);

  return (
    <div className="space-y-6">
      <div>
        <h1 className="font-serif text-3xl text-ink">Updates</h1>
        <p className="mt-1 text-sm text-muted">Publish native library packages and in-app APK updates.</p>
      </div>

      {error ? <ErrorBanner message={error} onRetry={load} /> : null}

      <div className="grid gap-6 lg:grid-cols-2">
        <Card className="p-5">
          <h2 className="text-base text-ink">Native library</h2>
          <p className="mt-1 text-sm text-muted">Clients download and unpack this ZIP into the sandbox.</p>
          <div className="mt-4 rounded-lg border border-line bg-subtle p-3 text-sm">
            <div className="flex justify-between text-muted">
              <span>Active version</span>
              <span className="font-mono text-ink">v{libActive.version || '—'}</span>
            </div>
            <p className="mt-2 break-all font-mono text-xs text-muted">{libActive.url || 'No URL published'}</p>
            {libActive.updated ? (
              <p className="mt-2 text-xs text-faint">{new Date(libActive.updated).toLocaleString()}</p>
            ) : null}
          </div>
          <form
            className="mt-4 space-y-4"
            onSubmit={async (e) => {
              e.preventDefault();
              if (!libVersion.trim()) {
                notify('Version is required.', 'err');
                return;
              }
              if (!/^https?:\/\//i.test(libUrl.trim())) {
                notify('Enter a valid download URL.', 'err');
                return;
              }
              setSavingLib(true);
              try {
                await api('/api/client/lib-update', {
                  method: 'POST',
                  body: JSON.stringify({ version: libVersion.trim(), download_url: libUrl.trim() }),
                });
                setLibVersion('');
                setLibUrl('');
                notify('Library update published.');
                await load();
              } catch (err: any) {
                notify(err?.message || 'Publish failed.', 'err');
              } finally {
                setSavingLib(false);
              }
            }}
          >
            <Field label="New version">
              <Input
                className="font-mono"
                value={libVersion}
                onChange={(e) => setLibVersion(e.target.value)}
                placeholder={libActive.version ? `Current ${libActive.version}` : '1.1'}
              />
            </Field>
            <Field label="Direct ZIP URL">
              <Input
                type="url"
                className="font-mono"
                value={libUrl}
                onChange={(e) => setLibUrl(e.target.value)}
                placeholder="https://…"
              />
            </Field>
            <Button type="submit" className="w-full" loading={savingLib}>
              Publish library
            </Button>
          </form>
        </Card>

        <Card className="p-5">
          <h2 className="text-base text-ink">Loader APK</h2>
          <p className="mt-1 text-sm text-muted">Optional or mandatory in-app update for the loader itself.</p>
          <div className="mt-4 rounded-lg border border-line bg-subtle p-3 text-sm">
            <div className="flex justify-between text-muted">
              <span>Active version</span>
              <span className="font-mono text-ink">
                {apkActive.version_name || '—'} ({apkActive.version_code || 0})
              </span>
            </div>
            <div className="mt-1 text-xs text-muted">{apkActive.is_mandatory ? 'Mandatory' : 'Optional'}</div>
            <p className="mt-2 break-all font-mono text-xs text-muted">
              {apkActive.download_url || 'No URL published'}
            </p>
          </div>
          <form
            className="mt-4 space-y-4"
            onSubmit={async (e) => {
              e.preventDefault();
              const versionName = apkName.trim() || apkActive.version_name;
              const versionCode = parseInt(apkCode, 10) || apkActive.version_code;
              const url = apkUrl.trim() || apkActive.download_url;
              if (!url || !/^https?:\/\//i.test(url)) {
                notify('A valid APK download URL is required.', 'err');
                return;
              }
              if (!versionName) {
                notify('Version name is required.', 'err');
                return;
              }
              setSavingApk(true);
              try {
                await api('/api/client/app-update', {
                  method: 'POST',
                  body: JSON.stringify({
                    version_name: versionName,
                    version_code: versionCode,
                    download_url: url,
                    changelog: apkChangelog.trim() || apkActive.changelog,
                    is_mandatory: apkMandatory,
                  }),
                });
                notify('APK update published.');
                await load();
              } catch (err: any) {
                notify(err?.message || 'Publish failed.', 'err');
              } finally {
                setSavingApk(false);
              }
            }}
          >
            <div className="grid grid-cols-2 gap-3">
              <Field label="Version name">
                <Input
                  className="font-mono"
                  value={apkName}
                  onChange={(e) => setApkName(e.target.value)}
                  placeholder={apkActive.version_name || '1.4.0'}
                />
              </Field>
              <Field label="Version code">
                <Input
                  type="number"
                  className="font-mono"
                  value={apkCode}
                  onChange={(e) => setApkCode(e.target.value)}
                  placeholder={String(apkActive.version_code || '')}
                />
              </Field>
            </div>
            <Field label="Direct APK URL">
              <Input
                type="url"
                className="font-mono"
                value={apkUrl}
                onChange={(e) => setApkUrl(e.target.value)}
                placeholder="https://…/loader.apk"
              />
            </Field>
            <Field label="Changelog">
              <Textarea
                rows={3}
                value={apkChangelog}
                onChange={(e) => setApkChangelog(e.target.value)}
                placeholder="What changed in this build"
              />
            </Field>
            <Toggle
              checked={apkMandatory}
              onChange={setApkMandatory}
              label="Mandatory update"
              description="Older loader versions cannot continue until they update."
            />
            <Button type="submit" className="w-full" loading={savingApk}>
              Publish APK
            </Button>
          </form>
        </Card>
      </div>
    </div>
  );
}
