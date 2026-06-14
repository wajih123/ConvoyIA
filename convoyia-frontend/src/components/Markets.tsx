import { Globe } from "lucide-react";

const phases = [
  { phase: "M0 — Live now",    markets: ["🇫🇷 France (Goweyy)"],                          status: "live"    },
  { phase: "M6 — Q1 2027",     markets: ["🇬🇧 United Kingdom", "🇧🇪 Belgium", "🇳🇱 Netherlands"], status: "soon"    },
  { phase: "M12 — Q3 2027",    markets: ["🇩🇪 Germany", "🇪🇸 Spain", "🇮🇹 Italy"],         status: "planned" },
  { phase: "M18 — Q1 2028",    markets: ["🇦🇪 UAE", "🇸🇦 Saudi Arabia"],                   status: "planned" },
  { phase: "M24 — Q3 2028",    markets: ["🇦🇺 Australia", "🇨🇦 Canada", "🇺🇸 United States"], status: "planned" },
];

const dimensions = [
  ["Language",          "French",           "Multi-langue i18n"          ],
  ["Currency",          "EUR",              "ISO 4217 multi-devise"      ],
  ["Tax",               "TVA 20%",          "Configurable per market"    ],
  ["Insurance",         "Hiscox France",    "Local partner per market"   ],
  ["Driver licence",    "Category B (FR)",  "Mapping per country"        ],
  ["Background check",  "Casier B3",        "Configurable per tenant"    ],
  ["Payment",           "Stripe EUR",       "Stripe multi-currency"      ],
  ["Return trip",       "Bolt Business FR", "Local partner per market"   ],
  ["Platform fee",      "25% (Goweyy)",     "Configurable per tenant"    ],
  ["Address validation","French format",    "Google Places global"       ],
  ["Timezone",          "Europe/Paris",     "Multi-timezone"             ],
];

const statusStyle: Record<string, string> = {
  live:    "bg-emerald-500/20 text-emerald-400 border-emerald-500/30",
  soon:    "bg-brand-500/20 text-brand-400 border-brand-500/30",
  planned: "bg-white/8 text-white/40 border-white/10",
};

const statusLabel: Record<string, string> = {
  live: "Live", soon: "Coming soon", planned: "Roadmap",
};

export default function Markets() {
  return (
    <section id="markets" className="py-24 border-t border-white/5">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="max-w-2xl mx-auto text-center mb-16">
          <p className="section-label"><Globe size={14} /> Global Expansion</p>
          <h2 className="text-4xl sm:text-5xl font-extrabold tracking-tight mb-4">
            Built for the{" "}
            <span className="text-gradient">whole world</span>
          </h2>
          <p className="text-white/55 text-lg">
            Every dimension that varies per country is a tenant config. Launch
            in a new market by updating JSON, not rewriting code.
          </p>
        </div>

        <div className="grid sm:grid-cols-2 lg:grid-cols-3 gap-4 mb-14">
          {phases.map((p) => (
            <div key={p.phase} className="card-glass p-5 hover:border-white/20 transition-colors">
              <div className="flex items-center justify-between mb-3">
                <span className="font-semibold text-sm">{p.phase}</span>
                <span className={`text-[11px] font-bold px-2 py-0.5 rounded-full border ${statusStyle[p.status]}`}>
                  {statusLabel[p.status]}
                </span>
              </div>
              <ul className="space-y-1">
                {p.markets.map((m) => <li key={m} className="text-sm text-white/65">{m}</li>)}
              </ul>
            </div>
          ))}
        </div>

        <h3 className="font-bold text-white/50 text-xs uppercase tracking-widest mb-5">
          Configurable Dimensions per Tenant
        </h3>
        <div className="card-glass overflow-hidden">
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-white/10">
                {["Dimension", "Goweyy (France)", "ConvoyIA Worldwide"].map((h) => (
                  <th key={h} className="text-left px-5 py-3 text-white/40 font-semibold text-xs uppercase tracking-wider">{h}</th>
                ))}
              </tr>
            </thead>
            <tbody>
              {dimensions.map(([dim, fr, global], i) => (
                <tr key={dim} className={i % 2 === 0 ? "bg-white/[0.02]" : ""}>
                  <td className="px-5 py-3 font-medium text-white/80">{dim}</td>
                  <td className="px-5 py-3 text-white/55">{fr}</td>
                  <td className="px-5 py-3 text-brand-400 text-xs">{global}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </section>
  );
}
