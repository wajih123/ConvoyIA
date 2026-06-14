import type { Metadata } from "next";
import localFont from "next/font/local";
import "./globals.css";

const inter = localFont({
  src: "./fonts/GeistVF.woff",
  variable: "--font-inter",
});

export const metadata: Metadata = {
  title: "ConvoyIA — AI Agent Platform for Vehicle Conveyance",
  description:
    "ConvoyIA powers vehicle conveyance marketplaces with 6 intelligent AI agents: dispatch, verification, pricing, inspection, tracking, and billing.",
  keywords: ["vehicle conveyance", "AI platform", "SaaS", "fleet management", "white-label"],
  openGraph: {
    title: "ConvoyIA",
    description: "The AI platform that powers vehicle conveyance marketplaces worldwide.",
    type: "website",
  },
};

export default function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <html lang="en" className="scroll-smooth">
      <body className={`${inter.variable} antialiased`}>{children}</body>
    </html>
  );
}
