import type { Metadata, Viewport } from 'next';
import './globals.css';

export const metadata: Metadata = {
  title: 'ANOY PANEL | Admin Control Center',
  description: 'Enterprise License Key & Device Management Dashboard for Anoy Loader',
};

export const viewport: Viewport = {
  width: 'device-width',
  initialScale: 1,
  maximumScale: 1,
};

export default function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <html lang="en" className="dark">
      <body className="min-h-screen bg-background text-textPrimary antialiased selection:bg-primary selection:text-white">
        {children}
      </body>
    </html>
  );
}
