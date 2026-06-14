import { useState, useEffect } from "react";
import { Menu, X, Zap } from "lucide-react";

const links = [
  { label: "Platform", href: "#platform" },
  { label: "Agents",   href: "#agents"   },
  { label: "APIs",     href: "#apis"     },
  { label: "Pricing",  href: "#pricing"  },
  { label: "Markets",  href: "#markets"  },
];

export default function Navbar() {
  const [open, setOpen]       = useState(false);
  const [scrolled, setScrolled] = useState(false);

  useEffect(() => {
    const handler = () => setScrolled(window.scrollY > 20);
    window.addEventListener("scroll", handler);
    return () => window.removeEventListener("scroll", handler);
  }, []);

  return (
    <header
      className={`fixed top-0 inset-x-0 z-50 transition-all duration-300 ${
        scrolled
          ? "border-b border-white/8 bg-[#0a0a14]/90 backdrop-blur-xl shadow-lg shadow-black/20"
          : "bg-transparent"
      }`}
    >
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
              <a href={l.href} className="text-sm text-white/60 hover:text-white transition-colors">
                {l.label}
              </a>
            </li>
          ))}
        </ul>

        {/* Desktop CTA */}
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
          className="md:hidden text-white/70 hover:text-white transition-colors"
          onClick={() => setOpen((v) => !v)}
          aria-label="Toggle menu"
        >
          {open ? <X size={22} /> : <Menu size={22} />}
        </button>
      </nav>

      {/* Mobile drawer */}
      <div
        className={`md:hidden overflow-hidden transition-all duration-300 ${
          open ? "max-h-96 opacity-100" : "max-h-0 opacity-0"
        } bg-[#0d0d20] border-t border-white/10`}
      >
        <div className="px-4 py-4 space-y-3">
          {links.map((l) => (
            <a
              key={l.href}
              href={l.href}
              className="block text-sm text-white/70 hover:text-white py-1 transition-colors"
              onClick={() => setOpen(false)}
            >
              {l.label}
            </a>
          ))}
          <a
            href="#pricing"
            className="btn-primary w-full justify-center mt-2 text-sm"
            onClick={() => setOpen(false)}
          >
            Get API Access
          </a>
        </div>
      </div>
    </header>
  );
}
