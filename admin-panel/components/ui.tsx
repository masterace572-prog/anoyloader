'use client';

import React from 'react';
import { Loader2, X } from 'lucide-react';
import { cn } from '@/lib/cn';

export function Button({
  children,
  className,
  variant = 'primary',
  size = 'md',
  loading = false,
  disabled,
  type = 'button',
  ...props
}: React.ButtonHTMLAttributes<HTMLButtonElement> & {
  variant?: 'primary' | 'secondary' | 'ghost' | 'danger';
  size?: 'sm' | 'md';
  loading?: boolean;
}) {
  const styles = {
    primary: 'bg-accent text-onAccent hover:opacity-90',
    secondary: 'border border-line bg-elevated text-ink hover:bg-subtle',
    ghost: 'text-muted hover:text-ink hover:bg-subtle',
    danger: 'text-danger hover:bg-subtle',
  }[variant];
  const sizing = size === 'sm' ? 'h-9 px-3 text-sm' : 'h-11 px-4 text-sm';

  return (
    <button
      type={type}
      disabled={disabled || loading}
      className={cn(
        'inline-flex items-center justify-center gap-2 rounded-[10px] font-medium transition-colors disabled:cursor-not-allowed disabled:opacity-40',
        styles,
        sizing,
        className
      )}
      {...props}
    >
      {loading ? <Loader2 className="h-4 w-4 animate-spin" /> : null}
      {children}
    </button>
  );
}

export function Input({
  className,
  ...props
}: React.InputHTMLAttributes<HTMLInputElement>) {
  return (
    <input
      className={cn(
        'h-11 w-full rounded-lg border border-line bg-subtle px-3.5 text-sm text-ink placeholder:text-faint outline-none transition-colors focus:border-ink',
        className
      )}
      {...props}
    />
  );
}

export function Textarea({
  className,
  ...props
}: React.TextareaHTMLAttributes<HTMLTextAreaElement>) {
  return (
    <textarea
      className={cn(
        'w-full rounded-lg border border-line bg-subtle px-3.5 py-2.5 text-sm text-ink placeholder:text-faint outline-none transition-colors focus:border-ink',
        className
      )}
      {...props}
    />
  );
}

export function Field({
  label,
  hint,
  error,
  children,
}: {
  label: string;
  hint?: string;
  error?: string;
  children: React.ReactNode;
}) {
  return (
    <label className="block space-y-1.5">
      <span className="text-sm text-muted">{label}</span>
      {children}
      {error ? <span className="text-xs text-danger">{error}</span> : null}
      {!error && hint ? <span className="text-xs text-faint">{hint}</span> : null}
    </label>
  );
}

export function Card({
  className,
  children,
}: {
  className?: string;
  children: React.ReactNode;
}) {
  return (
    <div className={cn('rounded-xl border border-line bg-surface', className)}>{children}</div>
  );
}

export function StatusText({
  status,
}: {
  status: 'ACTIVE' | 'UNUSED' | 'EXPIRED' | 'BANNED' | string;
}) {
  const color =
    status === 'ACTIVE'
      ? 'text-success'
      : status === 'UNUSED'
        ? 'text-warning'
        : status === 'BANNED'
          ? 'text-danger'
          : 'text-muted';
  const label =
    status === 'ACTIVE'
      ? 'Active'
      : status === 'UNUSED'
        ? 'Unused'
        : status === 'EXPIRED'
          ? 'Expired'
          : status === 'BANNED'
            ? 'Banned'
            : status;
  return (
    <span className={cn('inline-flex items-center gap-2 text-sm', color)}>
      <span className="block h-3.5 w-0.5 rounded-full bg-current" />
      {label}
    </span>
  );
}

export function Segmented<T extends string>({
  value,
  onChange,
  options,
}: {
  value: T;
  onChange: (v: T) => void;
  options: { value: T; label: string }[];
}) {
  return (
    <div className="inline-flex rounded-lg border border-line bg-subtle p-1">
      {options.map((opt) => (
        <button
          key={opt.value}
          type="button"
          onClick={() => onChange(opt.value)}
          className={cn(
            'rounded-md px-3 py-1.5 text-sm transition-colors',
            value === opt.value ? 'bg-elevated text-ink' : 'text-muted hover:text-ink'
          )}
        >
          {opt.label}
        </button>
      ))}
    </div>
  );
}

export function Toggle({
  checked,
  onChange,
  label,
  description,
}: {
  checked: boolean;
  onChange: (v: boolean) => void;
  label: string;
  description?: string;
}) {
  return (
    <button
      type="button"
      onClick={() => onChange(!checked)}
      className="flex w-full items-center justify-between gap-4 rounded-lg border border-line bg-subtle px-4 py-3 text-left"
    >
      <span>
        <span className="block text-sm text-ink">{label}</span>
        {description ? <span className="mt-0.5 block text-xs text-muted">{description}</span> : null}
      </span>
      <span
        className={cn(
          'relative h-6 w-10 shrink-0 rounded-full transition-colors',
          checked ? 'bg-accent' : 'bg-line'
        )}
      >
        <span
          className={cn(
            'absolute top-0.5 h-5 w-5 rounded-full transition-all',
            checked ? 'left-[18px] bg-onAccent' : 'left-[2px] bg-muted'
          )}
        />
      </span>
    </button>
  );
}

export function Modal({
  open,
  onClose,
  title,
  description,
  children,
  wide,
}: {
  open: boolean;
  onClose: () => void;
  title: string;
  description?: string;
  children: React.ReactNode;
  wide?: boolean;
}) {
  if (!open) return null;
  return (
    <div className="fixed inset-0 z-50 flex items-end justify-center p-0 sm:items-center sm:p-6">
      <button
        type="button"
        aria-label="Close"
        className="absolute inset-0 bg-black/64"
        onClick={onClose}
      />
      <div
        className={cn(
          'relative z-10 max-h-[92vh] w-full overflow-y-auto rounded-t-2xl border border-line bg-surface p-5 sm:rounded-2xl sm:p-6',
          wide ? 'sm:max-w-2xl' : 'sm:max-w-lg'
        )}
      >
        <div className="mb-5 flex items-start justify-between gap-4">
          <div>
            <h2 className="font-serif text-xl text-ink">{title}</h2>
            {description ? <p className="mt-1 text-sm text-muted">{description}</p> : null}
          </div>
          <button
            type="button"
            onClick={onClose}
            className="rounded-lg p-1.5 text-muted hover:bg-subtle hover:text-ink"
          >
            <X className="h-4 w-4" />
          </button>
        </div>
        {children}
      </div>
    </div>
  );
}

export function EmptyState({
  title,
  message,
  action,
}: {
  title: string;
  message: string;
  action?: React.ReactNode;
}) {
  return (
    <div className="flex flex-col items-center px-6 py-16 text-center">
      <h3 className="font-serif text-xl text-ink">{title}</h3>
      <p className="mt-2 max-w-sm text-sm text-muted">{message}</p>
      {action ? <div className="mt-5">{action}</div> : null}
    </div>
  );
}

export function Spinner({ className }: { className?: string }) {
  return <Loader2 className={cn('h-4 w-4 animate-spin text-muted', className)} />;
}

export function ErrorBanner({
  message,
  onRetry,
}: {
  message: string;
  onRetry?: () => void;
}) {
  return (
    <div className="flex items-start justify-between gap-3 rounded-xl border border-line bg-subtle px-4 py-3">
      <p className="text-sm text-danger">{message}</p>
      {onRetry ? (
        <button type="button" onClick={onRetry} className="shrink-0 text-sm text-ink hover:underline">
          Retry
        </button>
      ) : null}
    </div>
  );
}
