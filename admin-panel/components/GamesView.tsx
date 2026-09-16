'use client';

import React, { useEffect, useState } from 'react';
import { Plus, Trash2 } from 'lucide-react';
import { api } from '@/lib/api';
import { GameVersion, ManagedGame } from '@/lib/types';
import { useFeedback } from './feedback';
import { Button, Card, EmptyState, ErrorBanner, Field, Input, Modal, Spinner, Toggle } from './ui';

export function GamesView() {
  const { notify, confirm } = useFeedback();
  const [games, setGames] = useState<ManagedGame[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [editing, setEditing] = useState<Partial<ManagedGame> | null>(null);
  const [versionFor, setVersionFor] = useState<ManagedGame | null>(null);
  const [versionDraft, setVersionDraft] = useState<Partial<GameVersion>>({});
  const [saving, setSaving] = useState(false);

  const load = async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await api<{ success: boolean; games: ManagedGame[] }>('/api/admin/games');
      setGames(data.games || []);
    } catch (err: any) {
      setError(err?.message || 'Could not load games.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    load();
  }, []);

  const post = async (body: Record<string, unknown>) => {
    setSaving(true);
    try {
      await api('/api/admin/games', { method: 'POST', body: JSON.stringify(body) });
      await load();
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="space-y-6">
      <div className="flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <h1 className="font-serif text-3xl text-ink">Games</h1>
          <p className="mt-1 text-sm text-muted">Target applications and version catalog.</p>
        </div>
        <Button
          onClick={() =>
            setEditing({
              id: '',
              title: '',
              package_name: '',
              lib_name: 'libbgmi.so',
              icon_type: 'bgmi',
              is_enabled: true,
              status_text: 'Ready',
              sort_order: games.length,
              versions: [],
            })
          }
        >
          <Plus className="h-4 w-4" />
          Add game
        </Button>
      </div>

      {error ? <ErrorBanner message={error} onRetry={load} /> : null}

      {loading ? (
        <div className="flex items-center justify-center gap-2 py-16 text-sm text-muted">
          <Spinner /> Loading games
        </div>
      ) : games.length === 0 ? (
        <Card>
          <EmptyState title="No games" message="Add a target application to publish versions to the loader." />
        </Card>
      ) : (
        <div className="space-y-4">
          {games.map((game) => (
            <Card key={game.id} className="p-5">
              <div className="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
                <div>
                  <div className="flex items-center gap-2">
                    <h2 className="text-base text-ink">{game.title}</h2>
                    <span className="text-xs text-muted">{game.is_enabled ? 'Enabled' : 'Disabled'}</span>
                  </div>
                  <p className="mt-1 font-mono text-xs text-muted">{game.package_name}</p>
                  <p className="mt-1 text-xs text-faint">
                    {game.lib_name} · {game.status_text}
                  </p>
                </div>
                <div className="flex flex-wrap gap-2">
                  <Button size="sm" variant="secondary" onClick={() => setEditing(game)}>
                    Edit
                  </Button>
                  <Button
                    size="sm"
                    variant="secondary"
                    onClick={() => {
                      setVersionFor(game);
                      setVersionDraft({
                        game_id: game.id,
                        version_name: '',
                        version_code: 0,
                        tag: 'LATEST',
                        status_text: 'Ready',
                        lib_name: game.lib_name,
                        is_default: game.versions.length === 0,
                        is_active: true,
                      });
                    }}
                  >
                    Add version
                  </Button>
                  <Button
                    size="sm"
                    variant="danger"
                    onClick={async () => {
                      const ok = await confirm({
                        title: 'Delete game?',
                        message: `${game.title} and its versions will be removed.`,
                        confirmLabel: 'Delete',
                        danger: true,
                      });
                      if (!ok) return;
                      try {
                        await post({ action: 'delete_game', id: game.id });
                        notify('Game deleted.');
                      } catch (err: any) {
                        notify(err?.message || 'Delete failed.', 'err');
                      }
                    }}
                  >
                    <Trash2 className="h-4 w-4" />
                  </Button>
                </div>
              </div>

              <div className="mt-4 divide-y divide-line overflow-hidden rounded-lg border border-line">
                {game.versions.length === 0 ? (
                  <p className="px-4 py-6 text-center text-sm text-muted">No versions yet.</p>
                ) : (
                  game.versions.map((v) => (
                    <div key={v.id} className="flex items-center justify-between gap-3 px-4 py-3">
                      <div>
                        <div className="text-sm text-ink">
                          {v.version_name}
                          <span className="ml-2 text-xs text-muted">{v.tag}</span>
                        </div>
                        <div className="text-xs text-muted">
                          Code {v.version_code} · {v.status_text}
                          {v.is_default ? ' · Default' : ''}
                        </div>
                      </div>
                      <Button
                        size="sm"
                        variant="danger"
                        onClick={async () => {
                          const ok = await confirm({
                            title: 'Delete version?',
                            message: `${v.version_name} will be removed.`,
                            confirmLabel: 'Delete',
                            danger: true,
                          });
                          if (!ok) return;
                          try {
                            await post({ action: 'delete_version', id: v.id });
                            notify('Version deleted.');
                          } catch (err: any) {
                            notify(err?.message || 'Delete failed.', 'err');
                          }
                        }}
                      >
                        Remove
                      </Button>
                    </div>
                  ))
                )}
              </div>
            </Card>
          ))}
        </div>
      )}

      <Modal
        open={!!editing}
        onClose={() => setEditing(null)}
        title={editing?.id && games.some((g) => g.id === editing.id) ? 'Edit game' : 'Add game'}
      >
        {editing ? (
          <form
            className="space-y-4"
            onSubmit={async (e) => {
              e.preventDefault();
              if (!editing.id || !editing.title || !editing.package_name) {
                notify('ID, title, and package name are required.', 'err');
                return;
              }
              try {
                await post({ action: 'upsert_game', game: editing });
                setEditing(null);
                notify('Game saved.');
              } catch (err: any) {
                notify(err?.message || 'Save failed.', 'err');
              }
            }}
          >
            <Field label="ID">
              <Input
                className="font-mono"
                value={editing.id || ''}
                onChange={(e) => setEditing({ ...editing, id: e.target.value })}
                placeholder="bgmi"
              />
            </Field>
            <Field label="Title">
              <Input
                value={editing.title || ''}
                onChange={(e) => setEditing({ ...editing, title: e.target.value })}
                placeholder="BGMI"
              />
            </Field>
            <Field label="Package name">
              <Input
                className="font-mono"
                value={editing.package_name || ''}
                onChange={(e) => setEditing({ ...editing, package_name: e.target.value })}
                placeholder="com.pubg.imobile"
              />
            </Field>
            <Field label="Library file">
              <Input
                className="font-mono"
                value={editing.lib_name || ''}
                onChange={(e) => setEditing({ ...editing, lib_name: e.target.value })}
                placeholder="libbgmi.so"
              />
            </Field>
            <Field label="Status text">
              <Input
                value={editing.status_text || ''}
                onChange={(e) => setEditing({ ...editing, status_text: e.target.value })}
              />
            </Field>
            <Toggle
              checked={!!editing.is_enabled}
              onChange={(v) => setEditing({ ...editing, is_enabled: v })}
              label="Enabled"
              description="Disabled games appear as unavailable in the loader."
            />
            <div className="flex gap-2 pt-2">
              <Button type="button" variant="secondary" className="flex-1" onClick={() => setEditing(null)}>
                Cancel
              </Button>
              <Button type="submit" className="flex-1" loading={saving}>
                Save
              </Button>
            </div>
          </form>
        ) : null}
      </Modal>

      <Modal
        open={!!versionFor}
        onClose={() => setVersionFor(null)}
        title="Add version"
        description={versionFor?.title}
      >
        <form
          className="space-y-4"
          onSubmit={async (e) => {
            e.preventDefault();
            if (!versionDraft.version_name) {
              notify('Version name is required.', 'err');
              return;
            }
            try {
              await post({ action: 'upsert_version', version: { ...versionDraft, game_id: versionFor?.id } });
              setVersionFor(null);
              notify('Version saved.');
            } catch (err: any) {
              notify(err?.message || 'Save failed.', 'err');
            }
          }}
        >
          <div className="grid grid-cols-2 gap-3">
            <Field label="Version name">
              <Input
                value={versionDraft.version_name || ''}
                onChange={(e) => setVersionDraft({ ...versionDraft, version_name: e.target.value })}
                placeholder="4.6.0"
              />
            </Field>
            <Field label="Version code">
              <Input
                type="number"
                value={versionDraft.version_code ?? ''}
                onChange={(e) => setVersionDraft({ ...versionDraft, version_code: parseInt(e.target.value, 10) || 0 })}
              />
            </Field>
          </div>
          <Field label="Tag">
            <Input
              value={versionDraft.tag || ''}
              onChange={(e) => setVersionDraft({ ...versionDraft, tag: e.target.value })}
              placeholder="LATEST"
            />
          </Field>
          <Field label="Library file">
            <Input
              className="font-mono"
              value={versionDraft.lib_name || ''}
              onChange={(e) => setVersionDraft({ ...versionDraft, lib_name: e.target.value })}
            />
          </Field>
          <Toggle
            checked={!!versionDraft.is_default}
            onChange={(v) => setVersionDraft({ ...versionDraft, is_default: v })}
            label="Default version"
          />
          <div className="flex gap-2 pt-2">
            <Button type="button" variant="secondary" className="flex-1" onClick={() => setVersionFor(null)}>
              Cancel
            </Button>
            <Button type="submit" className="flex-1" loading={saving}>
              Save version
            </Button>
          </div>
        </form>
      </Modal>
    </div>
  );
}
