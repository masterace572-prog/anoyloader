'use client';

import React, { useEffect, useState } from 'react';
import { api, clearAdminToken } from '@/lib/api';
import { FeedbackProvider } from './feedback';
import { LicensesView } from './LicensesView';
import { LoginView } from './LoginView';
import { AppView, Shell } from './Shell';
import { SystemView } from './SystemView';
import { UpdatesView } from './UpdatesView';
import { Spinner } from './ui';

function Dashboard() {
  const [view, setView] = useState<AppView>('licenses');
  const [live, setLive] = useState(false);

  return (
    <Shell
      view={view}
      onView={setView}
      live={live}
      onLogout={() => {
        clearAdminToken();
        window.location.reload();
      }}
    >
      {view === 'licenses' ? <LicensesView onLiveChange={setLive} /> : null}
      {view === 'updates' ? <UpdatesView /> : null}
      {view === 'system' ? <SystemView /> : null}
    </Shell>
  );
}

export default function App() {
  const [checking, setChecking] = useState(true);
  const [authenticated, setAuthenticated] = useState(false);
  const [pinConfigured, setPinConfigured] = useState(true);

  useEffect(() => {
    (async () => {
      try {
        const data = await api<{ authenticated?: boolean; configured?: boolean }>('/api/admin/auth', {
          method: 'GET',
        });
        setPinConfigured(data?.configured !== false);
        if (data?.authenticated) setAuthenticated(true);
        else clearAdminToken();
      } catch {
        clearAdminToken();
      } finally {
        setChecking(false);
      }
    })();
  }, []);

  if (checking) {
    return (
      <div className="flex min-h-screen items-center justify-center text-sm text-muted">
        <Spinner />
      </div>
    );
  }

  if (!authenticated) {
    return <LoginView configured={pinConfigured} onAuthenticated={() => setAuthenticated(true)} />;
  }

  return (
    <FeedbackProvider>
      <Dashboard />
    </FeedbackProvider>
  );
}
