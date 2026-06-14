import { useState } from "react";
import { Code2, Copy, Check } from "lucide-react";

const endpoints = [
  { method: "POST", path: "/v1/dispatch/missions",               color: "bg-emerald-500/20 text-emerald-400 border-emerald-500/30", desc: "Qualify and create a new conveyance mission. Returns mission ID, assigned driver, route, and estimated price." },
  { method: "POST", path: "/v1/verify/drivers/{driverId}",       color: "bg-emerald-500/20 text-emerald-400 border-emerald-500/30", desc: "Run full compliance verification on a driver — licence, background check, documents." },
  { method: "POST", path: "/v1/price/estimate",                  color: "bg-emerald-500/20 text-emerald-400 border-emerald-500/30", desc: "Get a full price breakdown including base rate, distance, tolls, fees, insurance, and VAT." },
  { method: "POST", path: "/v1/inspect/missions/{id}/photos",    color: "bg-emerald-500/20 text-emerald-400 border-emerald-500/30", desc: "Submit pre- or post-mission photos. Returns AI condition report with damage locations and confidence scores." },
  { method: "GET",  path: "/v1/track/missions/{id}/live",        color: "bg-sky-500/20 text-sky-400 border-sky-500/30",             desc: "WebSocket stream for real-time GPS position, ETA, and anomaly alerts for an active mission." },
  { method: "POST", path: "/v1/billing/missions/{id}/finalize",  color: "bg-emerald-500/20 text-emerald-400 border-emerald-500/30", desc: "Trigger billing for a completed mission — Stripe charge, payout split, PDF invoice." },
  { method: "GET",  path: "/v1/tenants/{tenantId}/config",       color: "bg-sky-500/20 text-sky-400 border-sky-500/30",             desc: "Retrieve full tenant configuration — currency, tax, pricing formula, platform fee, branding." },
  { method: "PUT",  path: "/v1/tenants/{tenantId}/config",       color: "bg-amber-500/20 text-amber-400 border-amber-500/30",       desc: "Update tenant configuration. Supports partial updates. Changes propagate to all agents in real time." },
];

const sampleCode = `// Create a conveyance mission
const res = await fetch(
  'https://api.convoyia.io/v1/dispatch/missions',
  {
    method: 'POST',
    headers: {
      'Authorization': '******',
      'Content-Type': 'application/json',
      'X-Tenant-ID': 'goweyy-fr',
    },
    body: JSON.stringify({
      pickup: {
        address: '1 Promenade des Anglais, Nice',
        lat: 43.6950, lng: 7.2730,
      },
      dropoff: {
        address: '15 Rue de la Paix, Paris',
        lat: 48.8698, lng: 2.3309,
      },
      vehicle: { plate: 'AA-123-BB', type: 'SEDAN' },
      scheduledAt: '2026-07-01T09:00:00Z',
    }),
  }
);

const mission = await res.json();
// {
//   missionId: 'msn_01jx...',
//   driverId: 'drv_...',
//   estimatedPrice: {
//     total: 420.00, currency: 'EUR',
//     breakdown: { base: 80, distance: 290, tolls: 24, fee: 26 }
//   }
// }`;

export default function APIs() {
  const [copied, setCopied] = useState(false);

  const handleCopy = () => {
    navigator.clipboard.writeText(sampleCode);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  return (
    <section id="apis" className="py-24 border-t border-white/5">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="max-w-2xl mx-auto text-center mb-16">
          <p className="section-label"><Code2 size={14} /> API Catalog</p>
          <h2 className="text-4xl sm:text-5xl font-extrabold tracking-tight mb-4">
            REST APIs for every{" "}
            <span className="text-gradient">mission stage</span>
          </h2>
          <p className="text-white/55 text-lg">
            All agents are exposed as versioned REST endpoints behind a single
            API Gateway. Authenticate once, call any agent.
          </p>
        </div>

        <div className="grid lg:grid-cols-2 gap-8 items-start">
          {/* Endpoint list */}
          <div className="space-y-3">
            {endpoints.map((e) => (
              <div key={e.path} className="card-glass p-4 flex gap-4 hover:border-white/20 transition-colors">
                <span className={`shrink-0 mt-0.5 inline-flex items-center px-2 py-0.5 rounded text-[11px] font-bold border ${e.color}`}>
                  {e.method}
                </span>
                <div>
                  <code className="text-sm font-mono text-white/90 block mb-1">{e.path}</code>
                  <p className="text-xs text-white/50 leading-relaxed">{e.desc}</p>
                </div>
              </div>
            ))}
          </div>

          {/* Code sample (sticky) */}
          <div className="lg:sticky lg:top-24">
            <div className="card-glass overflow-hidden">
              <div className="flex items-center justify-between px-5 py-3 border-b border-white/10">
                <span className="flex items-center gap-2 text-xs text-white/50">
                  <Code2 size={13} /> JavaScript / TypeScript — quick start
                </span>
                <button
                  onClick={handleCopy}
                  className="text-white/40 hover:text-white/70 transition-colors flex items-center gap-1 text-xs"
                  aria-label="Copy code"
                >
                  {copied ? <><Check size={13} className="text-accent-400" /> Copied</> : <><Copy size={13} /> Copy</>}
                </button>
              </div>
              <pre className="p-5 text-xs font-mono text-white/75 overflow-x-auto leading-relaxed">
                <code>{sampleCode}</code>
              </pre>
            </div>

            <div className="mt-4 card-glass p-4 flex gap-3 text-sm">
              <span className="text-brand-400 mt-0.5">🔑</span>
              <div>
                <div className="font-semibold text-white/90 mb-1">API Key authentication</div>
                <p className="text-white/50 text-xs leading-relaxed">
                  Pass your key as{" "}
                  <code className="bg-white/10 px-1.5 py-0.5 rounded text-brand-300">{"Authorization: ******"}</code>
                  {" "}and your tenant as{" "}
                  <code className="bg-white/10 px-1.5 py-0.5 rounded text-brand-300">X-Tenant-ID</code>.
                  Keys are scoped per environment (sandbox / production).
                </p>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>
  );
}
