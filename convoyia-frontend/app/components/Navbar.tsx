"use client";
import { useState } from "react";
import { Menu, X, Zap } from "lucide-react";

const links = [
  { label: "Platform", href: "#platform" },
  { label: "Agents", href: "#agents" },
  { label: "APIs", href: "#apis" },
  { label: "Pricing", href: "#pricing" },
  { label: "Markets", href: "#markets" },
];

export default function Navbar() {
  const [open, setOpen] = useState(false);

  return (
    <header className="fixed top-0 inset-x-0 z-50 border-b border-white/5 bg-[#0a0a14]/80 backdrop-blur-xl">
      <nav className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 h-16 flex items-center justify-between">
        {/* Logo */}
        <a href="#" className="flex items-center gap-2 font-extrabold text-xl tracking-tight">
          <span className="p-1.5 rounded-lg bg-brand-600">
            <Zap size={16} className="text-white" />
          </span>
          <span className="text-gradient">ConvoyIA</span>
        </a>

        {/* Desktop links */}
        <ul className="hidden md:flex items-center gap-8">
          {links.map((l) => (
            <li key={l.href}>
              <a
                href={l.href}
                className="text-sm text-white/60 hover:text-white transition-colors"
              >
                {l.label}
              </a>
            </li>
          ))}
        </ul>

        {/* CTA */}
        <div className="hidden md:flex items-center gap-3">
          <a href="#pricing" className="text-sm text-white/60 hover:text-white transition-colors">
            Sign in
          </a>
          <a href="#pricing" className="btn-primary text-sm py-2 px-4">
            Get API Access
          </a>
        </div>

        {/* Mobile toggle */}
        <button
          className="md:hidden text-white/70 hover:text-white"
          onClick={() => setOpen((v) => !v)}
          aria-label="Toggle menu"
        >
          {open ? <X size={22} /> : <Menu size={22} />}
        </button>
      </nav>

      {/* Mobile menu */}
      {open && (
        <div className="md:hidden bg-[#0d0d20] border-t border-white/10 px-4 py-4 space-y-3">
          {links.map((l) => (
            <a
              key={l.href}
              href={l.href}
              className="block text-sm text-white/70 hover:text-white py-1"
              onClick={() => setOpen(false)}
            >
              {l.label}
            </a>
          ))}
          <a href="#pricing" className="btn-primary w-full justify-center mt-2 text-sm">
            Get API Access
          </a>
        </div>
      )}
    </header>
  );
}
