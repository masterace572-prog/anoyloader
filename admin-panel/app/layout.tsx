import type { Metadata, Viewport } from "next";
import "./globals.css";

export const metadata: Metadata = {
  title: "Anoy Control",
  description: "License, update, and system administration for Anoy Loader",
};

export const viewport: Viewport = {
  width: "device-width",
  initialScale: 1,
};

export default function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <html lang="en" suppressHydrationWarning>
      <head>
        <script
          dangerouslySetInnerHTML={{
            __html:
              "try{var t=localStorage.getItem('anoy-theme');document.documentElement.dataset.theme=t==='light'?'light':'dark'}catch(e){}",
          }}
        />
      </head>
      <body className="min-h-screen bg-background font-sans text-ink antialiased">
        {children}
      </body>
    </html>
  );
}
