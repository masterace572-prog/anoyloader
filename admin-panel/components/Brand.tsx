import { Layers3 } from "lucide-react";
import Link from "next/link";

export function Brand({ compact = false }: { compact?: boolean }) {
  return (
    <Link
      href="/"
      aria-label="Anoy home"
      className="inline-flex shrink-0 items-center gap-3"
    >
      <span className="flex h-10 w-10 items-center justify-center rounded-xl bg-accent text-onAccent shadow-sm">
        <Layers3 size={21} strokeWidth={1.8} />
      </span>
      <span className="text-xl font-semibold tracking-tight">
        anoy<span className="text-accent">.</span>
        {!compact && (
          <span className="mt-0.5 block text-[10px] font-medium uppercase tracking-[0.18em] text-muted">
            Control workspace
          </span>
        )}
      </span>
    </Link>
  );
}
