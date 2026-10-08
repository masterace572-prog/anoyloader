"use client";
import React from "react";
import Link from "next/link";
import {
  BookOpen,
  Code2,
  FileArchive,
  KeyRound,
  LogOut,
  Settings2,
  ShieldCheck,
  ChevronRight,
} from "lucide-react";
import { cn } from "@/lib/cn";
import { Brand } from "./Brand";
import { ThemeToggle } from "./ThemeToggle";
export type AppView = "licenses" | "updates" | "system" | "api";
const NAV = [
  {
    id: "licenses",
    label: "Licenses",
    icon: KeyRound,
    detail: "Keys and device access",
  },
  {
    id: "updates",
    label: "Updates",
    icon: FileArchive,
    detail: "App and library releases",
  },
  {
    id: "system",
    label: "Maintenance",
    icon: Settings2,
    detail: "Availability and messages",
  },
  {
    id: "api",
    label: "API access",
    icon: Code2,
    detail: "Credentials and usage",
  },
] as const;
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
  const current = NAV.find((n) => n.id === view)!;
  return (
    <div className="min-h-screen bg-background">
      <a
        href="#workspace"
        className="sr-only focus:not-sr-only focus:fixed focus:left-4 focus:top-4 focus:z-50 focus:rounded-lg focus:bg-accent focus:p-3 focus:text-onAccent"
      >
        Skip to workspace
      </a>
      <aside className="fixed inset-y-0 left-0 z-30 hidden w-64 flex-col border-r border-line bg-surface lg:flex">
        <div className="px-6 py-8">
          <Brand />
        </div>
        <p className="px-6 pb-3 text-[10px] font-semibold uppercase tracking-[0.16em] text-faint">
          Workspace
        </p>
        <nav
          aria-label="Workspace navigation"
          className="flex-1 space-y-2 px-3"
        >
          {NAV.map((item) => (
            <button
              key={item.id}
              type="button"
              aria-current={view === item.id ? "page" : undefined}
              onClick={() => onView(item.id)}
              className={cn(
                "group flex w-full items-center gap-3 rounded-xl px-3 py-3 text-left transition-colors",
                view === item.id
                  ? "bg-accent/10 text-accent"
                  : "text-muted hover:bg-subtle hover:text-ink",
              )}
            >
              <item.icon size={20} />
              <span>
                <span className="block text-sm font-semibold">
                  {item.label}
                </span>
                <span className="mt-1 block text-[11px] text-muted">
                  {item.detail}
                </span>
              </span>
              {view === item.id && <ChevronRight className="ml-auto h-4 w-4" />}
            </button>
          ))}
        </nav>
        <div className="mx-4 mb-4 rounded-xl border border-line bg-background p-4">
          <div className="flex items-center gap-2 text-sm font-medium">
            <ShieldCheck size={17} className="text-accent" />
            Approved workspace
          </div>
          <p className="mt-2 text-xs leading-relaxed text-muted">
            Access is checked on every management request.
          </p>
        </div>
        <div className="space-y-1 border-t border-line p-3">
          <Link
            href="/docs"
            className="flex items-center gap-3 rounded-xl p-3 text-sm text-muted hover:bg-subtle hover:text-ink"
          >
            <BookOpen size={18} />
            API documentation
          </Link>
          <button
            onClick={onLogout}
            className="flex w-full items-center gap-3 rounded-xl p-3 text-sm text-muted hover:bg-subtle hover:text-ink"
          >
            <LogOut size={18} />
            Sign out
          </button>
        </div>
      </aside>
      <div className="lg:pl-64">
        <header className="sticky top-0 z-20 border-b border-line bg-background/95 backdrop-blur-xl">
          <div className="flex min-h-20 items-center justify-between gap-3 px-4 sm:px-8">
            <div className="lg:hidden">
              <Brand compact />
            </div>
            <div className="hidden lg:block">
              <p className="text-xs text-muted">
                Workspace / <span className="text-ink">{current.label}</span>
              </p>
            </div>
            <div className="flex items-center gap-3">
              <span className="hidden items-center gap-2 rounded-full border border-line bg-surface px-3 py-1.5 text-xs text-muted sm:flex">
                <span
                  className={cn(
                    "h-1.5 w-1.5 rounded-full",
                    live ? "bg-success" : "bg-faint",
                  )}
                />
                {live ? "License data connected" : "Anoy Control"}
              </span>
              <ThemeToggle />
              <button
                aria-label="Sign out"
                onClick={onLogout}
                className="flex h-11 w-11 items-center justify-center rounded-xl border border-line text-muted lg:hidden"
              >
                <LogOut size={18} />
              </button>
            </div>
          </div>
          <nav
            aria-label="Mobile workspace navigation"
            className="no-scrollbar flex gap-2 overflow-x-auto px-4 pb-3 lg:hidden"
          >
            {NAV.map((item) => (
              <button
                key={item.id}
                aria-current={view === item.id ? "page" : undefined}
                onClick={() => onView(item.id)}
                className={cn(
                  "flex shrink-0 items-center gap-2 rounded-xl px-4 py-3 text-xs font-medium",
                  view === item.id
                    ? "bg-accent/10 text-accent"
                    : "text-muted hover:bg-subtle",
                )}
              >
                <item.icon size={16} />
                {item.label}
              </button>
            ))}
          </nav>
        </header>
        <main
          id="workspace"
          tabIndex={-1}
          className="mx-auto w-full max-w-7xl px-4 py-7 sm:px-8 sm:py-10"
        >
          <div key={view} className="fade-in">
            {children}
          </div>
        </main>
        <footer className="px-8 pb-8 text-xs text-faint">
          Anoy Control <span aria-hidden="true">·</span>{" "}
          <Link href="/docs" className="hover:text-ink">
            Documentation
          </Link>
        </footer>
      </div>
    </div>
  );
}
