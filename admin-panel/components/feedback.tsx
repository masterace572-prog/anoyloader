'use client';

import React, { createContext, useCallback, useContext, useMemo, useState } from 'react';
import { Button, Modal } from './ui';

type Toast = { id: number; message: string; tone: 'ok' | 'err' };

type ConfirmOpts = {
  title: string;
  message: string;
  confirmLabel?: string;
  danger?: boolean;
};

type Feedback = {
  notify: (message: string, tone?: 'ok' | 'err') => void;
  confirm: (opts: ConfirmOpts) => Promise<boolean>;
};

const FeedbackContext = createContext<Feedback | null>(null);

export function useFeedback() {
  const ctx = useContext(FeedbackContext);
  if (!ctx) throw new Error('useFeedback must be used within FeedbackProvider');
  return ctx;
}

export function FeedbackProvider({ children }: { children: React.ReactNode }) {
  const [toasts, setToasts] = useState<Toast[]>([]);
  const [confirmState, setConfirmState] = useState<(ConfirmOpts & { resolve: (v: boolean) => void }) | null>(
    null
  );

  const notify = useCallback((message: string, tone: 'ok' | 'err' = 'ok') => {
    const id = Date.now() + Math.random();
    setToasts((prev) => [...prev.slice(-3), { id, message, tone }]);
    setTimeout(() => setToasts((prev) => prev.filter((t) => t.id !== id)), 3200);
  }, []);

  const confirm = useCallback((opts: ConfirmOpts) => {
    return new Promise<boolean>((resolve) => {
      setConfirmState({ ...opts, resolve });
    });
  }, []);

  const value = useMemo(() => ({ notify, confirm }), [notify, confirm]);

  return (
    <FeedbackContext.Provider value={value}>
      {children}
      <div className="pointer-events-none fixed bottom-4 right-4 z-[70] flex w-[min(100%-2rem,360px)] flex-col gap-2">
        {toasts.map((t) => (
          <div
            key={t.id}
            className="pointer-events-auto rounded-xl border border-line bg-elevated px-4 py-3 text-sm text-ink"
          >
            <span className={t.tone === 'err' ? 'text-danger' : 'text-ink'}>{t.message}</span>
          </div>
        ))}
      </div>
      <Modal
        open={!!confirmState}
        onClose={() => {
          confirmState?.resolve(false);
          setConfirmState(null);
        }}
        title={confirmState?.title || ''}
        description={confirmState?.message}
      >
        <div className="flex justify-end gap-2">
          <Button
            variant="secondary"
            onClick={() => {
              confirmState?.resolve(false);
              setConfirmState(null);
            }}
          >
            Cancel
          </Button>
          <Button
            variant={confirmState?.danger ? 'danger' : 'primary'}
            onClick={() => {
              confirmState?.resolve(true);
              setConfirmState(null);
            }}
          >
            {confirmState?.confirmLabel || 'Confirm'}
          </Button>
        </div>
      </Modal>
    </FeedbackContext.Provider>
  );
}
