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
        background: "#070A10",
        surface: "#0E1522",
        surfaceHover: "#152033",
        surfaceBorder: "#243247",
        primary: "#FF6A1A",
        primaryHover: "#E5560A",
        secondary: "#8AF7FF",
        accent: "#FFB35C",
        textPrimary: "#F3F7FA",
        textSecondary: "#A5B4C4",
        textMuted: "#62758A",
      },
    },
  },
  plugins: [],
};
export default config;
