"use client";
import { useEffect, useState } from "react";
import { Moon, Sun } from "lucide-react";
export function ThemeToggle() {
  const [light, setLight] = useState(false);
  useEffect(() => {
    setLight(document.documentElement.dataset.theme === "light");
  }, []);
  return (
    <button
      type="button"
      aria-label={light ? "Switch to dark theme" : "Switch to light theme"}
      title={light ? "Dark theme" : "Light theme"}
      onClick={() => {
        const next = !light;
        setLight(next);
        document.documentElement.dataset.theme = next ? "light" : "dark";
        try {
          localStorage.setItem("anoy-theme", next ? "light" : "dark");
        } catch {
          /* Storage may be unavailable. */
        }
      }}
      className="inline-flex h-11 w-11 items-center justify-center rounded-xl border border-line bg-surface text-muted transition-colors hover:text-ink"
    >
      {light ? <Moon size={18} /> : <Sun size={18} />}
    </button>
  );
}
