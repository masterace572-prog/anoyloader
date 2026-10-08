import Link from "next/link";
import { Brand } from "./Brand";
import { ThemeToggle } from "./ThemeToggle";
export function PublicNav() {
  return (
    <header className="sticky top-0 z-30 border-b border-line bg-background/95 backdrop-blur-xl">
      <nav
        aria-label="Main navigation"
        className="mx-auto flex max-w-6xl flex-wrap items-center justify-between gap-4 px-5 py-4 sm:px-8"
      >
        <Brand compact />
        <div className="flex items-center gap-2 sm:gap-5">
          <Link
            href="/docs"
            className="hidden text-sm text-muted hover:text-ink sm:block"
          >
            API docs
          </Link>
          <ThemeToggle />
          <Link
            href="/login"
            className="px-2 py-2 text-sm font-medium hover:text-accent"
          >
            Sign in
          </Link>
          <Link
            href="/signup"
            className="hidden rounded-xl bg-accent px-4 py-3 text-sm font-semibold text-onAccent hover:brightness-110 sm:block"
          >
            Get started
          </Link>
        </div>
      </nav>
    </header>
  );
}
