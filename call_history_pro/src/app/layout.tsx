import type { Metadata, Viewport } from "next";
import { Inter } from 'next/font/google'
import "./globals.css";
import { Toaster } from "@/components/ui/toaster";
import { cn } from "@/lib/utils";

const inter = Inter({
  subsets: ['latin'],
  display: 'swap',
  variable: '--font-inter',
})

export const metadata: Metadata = {
  title: "CallSync Notes",
  description: "Sync and display call history with notes.",
  manifest: "/manifest.json",
};

export const viewport: Viewport = {
  themeColor: "hsl(var(--primary))",
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="en" suppressHydrationWarning className={cn(inter.variable)}>
      <head>
        <link rel="apple-touch-icon" href="/icon-192x192.png"></link>
      </head>
      <body className={cn("font-body antialiased", "bg-gray-100 dark:bg-gray-900")}>
        <main className="relative mx-auto h-dvh max-w-md overflow-hidden border-x bg-background shadow-2xl">
          {children}
        </main>
        <Toaster />
      </body>
    </html>
  );
}
