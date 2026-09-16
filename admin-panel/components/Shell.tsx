'use client';

import React from 'react';
import { FileArchive, KeyRound, LogOut, Settings2, Swords } from 'lucide-react';
import { cn } from '@/lib/cn';

export type AppView = 'licenses' | 'games' | 'updates' | 'system';

const NAV: { id: AppView; label: string; icon: React.ComponentType<{ className?: string }> }[] = [
  { id: 'licenses', label: 'Licenses', icon: KeyRound },
  { id: 'games', label: 'Games', icon: Swords },
  { id: 'updates', label: 'Updates', icon: FileArchive },
  { id: 'system', label: 'System', icon: Settings2 },
];

export function Shell({
  view,
  onView,
  live,
  onLogout,
  children,
}: {
  view: AppView;
  onView: (v: AppView) => void;
  live: boolean;
  onLogout: () => void;
  children: React.ReactNode;
}) {
  return (
    <div className="min-h-screen bg-background">
      <aside className="fixed inset-y-0 left-0 z-20 hidden w-60 border-r border-line bg-surface lg:flex lg:flex-col">
        <div className="flex h-16 items-center px-5">
          <div>
            <div className="font-serif text-lg text-ink">Anoy</div>
            <div className="text-xs text-muted">Control</div>
          </div>
        </div>
        <nav className="flex-1 space-y-1 px-3 py-2">
          {NAV.map((item) => {
            const Icon = item.icon;
            const active = view === item.id;
            return (
              <button
                key={item.id}
                type="button"
                onClick={() => onView(item.id)}
                className={cn(
                  'flex w-full items-center gap-3 rounded-lg px-3 py-2.5 text-sm transition-colors',
                  active ? 'bg-subtle text-ink' : 'text-muted hover:bg-subtle hover:text-ink'
                )}
              >
                <Icon className="h-4 w-4" />
                {item.label}
              </button>
            );
          })}
        </nav>
        <div className="border-t border-line p-4">
          <div className="mb-3 flex items-center justify-between text-xs">
            <span className="text-muted">Database</span>
            <span className={live ? 'text-success' : 'text-warning'}>{live ? 'Live' : 'Offline'}</span>
          </div>
          <button
            type="button"
            onClick={onLogout}
            className="flex w-full items-center gap-2 rounded-lg px-3 py-2 text-sm text-muted hover:bg-subtle hover:text-ink"
          >
            <LogOut className="h-4 w-4" />
            Sign out
          </button>
        </div>
      </aside>

      <header className="sticky top-0 z-20 border-b border-line bg-surface lg:hidden">
        <div className="flex items-center justify-between px-4 py-3">
          <div>
            <div className="font-serif text-base text-ink">Anoy</div>
            <div className="text-xs text-muted">{live ? 'Live' : 'Offline'}</div>
          </div>
          <button type="button" onClick={onLogout} className="rounded-lg p-2 text-muted hover:bg-subtle hover:text-ink">
            <LogOut className="h-4 w-4" />
          </button>
        </div>
        <div className="flex gap-1 overflow-x-auto no-scrollbar px-3 pb-2">
          {NAV.map((item) => (
            <button
              key={item.id}
              type="button"
              onClick={() => onView(item.id)}
              className={cn(
                'shrink-0 rounded-lg px-3 py-1.5 text-sm',
                view === item.id ? 'bg-subtle text-ink' : 'text-muted'
              )}
            >
              {item.label}
            </button>
          ))}
        </div>
      </header>

      <main className="min-h-screen lg:pl-60">
        <div className="mx-auto w-full max-w-6xl px-4 py-6 sm:px-8 sm:py-8">{children}</div>
      </main>
    </div>
  );
}
