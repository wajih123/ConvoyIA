import { Zap } from "lucide-react";

const cols = [
  {
    title: "Platform",
    links: [
      { label: "What is ConvoyIA", href: "#platform" },
      { label: "AI Agents", href: "#agents" },
      { label: "API Catalog", href: "#apis" },
      { label: "Tech Stack", href: "#tech" },
    ],
  },
  {
    title: "Business",
    links: [
      { label: "Pricing", href: "#pricing" },
      { label: "Markets", href: "#markets" },
      { label: "White-Label SaaS", href: "#platform" },
      { label: "Contact Sales", href: "mailto:hello@convoyia.io" },
    ],
  },
  {
    title: "Resources",
    links: [
      { label: "API Reference", href: "#apis" },
      { label: "Status", href: "#" },
      { label: "Changelog", href: "#" },
      { label: "GitHub", href: "#" },
    ],
  },
  {
    title: "Legal",
    links: [
      { label: "Privacy Policy", href: "#" },
      { label: "Terms of Service", href: "#" },
      { label: "DPA", href: "#" },
      { label: "Security", href: "#" },
    ],
  },
];

export default function Footer() {
  return (
    <footer className="border-t border-white/5 pt-14 pb-8 mt-8">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-5 gap-8 mb-12">
          {/* Brand */}
          <div className="col-span-2 sm:col-span-3 lg:col-span-1">
            <a href="#" className="flex items-center gap-2 font-extrabold text-xl mb-3">
              <span className="p-1.5 rounded-lg bg-brand-600">
                <Zap size={16} className="text-white" />
              </span>
              <span className="text-gradient">ConvoyIA</span>
            </a>
            <p className="text-xs text-white/40 leading-relaxed max-w-xs">
              AI platform powering vehicle conveyance marketplaces worldwide.
              Open-source LLMs. Zero vendor lock-in.
            </p>
          </div>

          {cols.map((c) => (
            <div key={c.title}>
              <div className="text-xs font-bold uppercase tracking-widest text-white/35 mb-4">
                {c.title}
              </div>
              <ul className="space-y-2.5">
                {c.links.map((l) => (
                  <li key={l.label}>
                    <a
                      href={l.href}
                      className="text-sm text-white/50 hover:text-white transition-colors"
                    >
                      {l.label}
                    </a>
                  </li>
                ))}
              </ul>
            </div>
          ))}
        </div>

        <div className="border-t border-white/5 pt-6 flex flex-col sm:flex-row items-center justify-between gap-3">
          <p className="text-xs text-white/30">
            © {new Date().getFullYear()} ConvoyIA. All rights reserved.
          </p>
          <p className="text-xs text-white/25">
            Powering{" "}
            <span className="text-white/45">Goweyy</span> — Nice, France · Built with ❤️ on Hetzner + K8s
          </p>
        </div>
      </div>
    </footer>
  );
}
