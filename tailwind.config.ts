import type { Config } from "tailwindcss";

const config: Config = {
  content: [
    "./pages/**/*.{js,ts,jsx,tsx,mdx}",
    "./components/**/*.{js,ts,jsx,tsx,mdx}",
    "./app/**/*.{js,ts,jsx,tsx,mdx}",
  ],
  theme: {
    extend: {
      colors: {
        background: "#090D16",
        surface: "#111827",
        surfaceHover: "#172033",
        surfaceBorder: "#1E293B",
        surfaceBorderLight: "#334155",
        primary: "#3B82F6",
        primaryHover: "#2563EB",
        accent: "#4F46E5",
        success: "#10B981",
        warning: "#F59E0B",
        danger: "#EF4444",
        textPrimary: "#F8FAFC",
        textSecondary: "#94A3B8",
        textMuted: "#64748B",
      },
    },
  },
  plugins: [],
};
export default config;
