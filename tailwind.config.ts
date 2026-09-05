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
        background: "#080808",
        surface: "#121212",
        surfaceHover: "#1A1A1A",
        surfaceBorder: "#242424",
        surfaceBorderLight: "#333333",
        primary: "#FFFFFF",
        primaryHover: "#E5E5E5",
        accent: "#FFFFFF",
        success: "#22C55E",
        warning: "#F59E0B",
        danger: "#EF4444",
        textPrimary: "#FFFFFF",
        textSecondary: "#A1A1AA",
        textMuted: "#71717A",
      },
    },
  },
  plugins: [],
};
export default config;
