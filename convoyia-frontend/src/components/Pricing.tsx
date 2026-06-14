import { useState } from "react";
import { CheckCircle2, Zap } from "lucide-react";

const plans = [
  {
    name: "Starter",
    badge: null,
    monthly: 499,
    annual: 415,
    desc: "For early-stage marketplaces validating product-market fit in a single market.",
    features: [
      "Up to 500 missions / month",
      "3 agents (Dispatcher, Pricer, Biller)",
      "1 tenant · 1 market",
      "REST APIs + Webhook events",
      "Sandbox environment",
      "Email support",
    ],
    cta: "Start Free Trial",
    highlight: false,
  },
  {
    name: "Growth",
    badge: "Most Popular",
    monthly: 1499,
    annual: 1249,
    desc: "All 6 agents unlocked. Built for scaling platforms across multiple markets.",
    features: [
      "Up to 5,000 missions / month",
      "All 6 agents included",
      "Up to 5 tenants · markets",
      "Vision AI inspection (Qwen-VL)",
      "Real-time GPS tracking",
      "Priority support (SLA 4 h)",
      "Dedicated Slack channel",
      "Custom pricing formula",
    ],
    cta: "Get Started",
    highlight: true,
  },
  {
    name: "Enterprise",
    badge: null,
    monthly: null,
    annual: null,
    desc: "Unlimited scale, SLA guarantees, and on-premise deployment for large operators.",
    features: [
      "Unlimited missions",
      "All 6 agents",
      "Unlimited tenants & markets",
      "Private cloud / on-premise",
      "Custom LLM fine-tuning",
      "99.9% uptime SLA",
      "Dedicated success engineer",
      "White-glove onboarding",
    ],
    cta: "Contact Sales",
    highlight: false,
  },
];

const addons = [
  { name: "Extra missions",              price: "€0.08 / mission over quota" },
  { name: "Additional tenant / market",  price: "€299 / month"               },
  { name: "Vision inspection add-on",    price: "€0.15 / photo analysed"     },
  { name: "White-label branding kit",    price: "€99 one-time"               },
];

export default function Pricing() {
  const [annual, setAnnual] = useState(false);

  return (
    <section id="pricing" className="py-24 border-t border-white/5">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="max-w-2xl mx-auto text-center mb-12">
          <p className="section-label"><Zap size={14} /> Pricing</p>
          <h2 className="text-4xl sm:text-5xl font-extrabold tracking-tight mb-4">
            Simple, transparent{" "}
            <span className="text-gradient">pricing</span>
          </h2>
          <p className="text-white/55 text-lg mb-8">
            Flat monthly subscriptions. No per-seat fees. Scale with mission volume only.
          </p>

          {/* Billing toggle */}
          <div className="inline-flex items-center gap-3 bg-white/5 border border-white/10 rounded-xl p-1">
            <button
              onClick={() => setAnnual(false)}
              className={`px-4 py-1.5 rounded-lg text-sm font-medium transition-all ${!annual ? "bg-brand-600 text-white shadow" : "text-white/50 hover:text-white"}`}
            >
              Monthly
            </button>
            <button
              onClick={() => setAnnual(true)}
              className={`px-4 py-1.5 rounded-lg text-sm font-medium transition-all flex items-center gap-1.5 ${annual ? "bg-brand-600 text-white shadow" : "text-white/50 hover:text-white"}`}
            >
              Annual
              <span className="text-[10px] bg-accent-500/30 text-accent-400 px-1.5 py-0.5 rounded-full font-bold">-2 mo</span>
            </button>
          </div>
        </div>

        <div className="grid lg:grid-cols-3 gap-6 mb-12">
          {plans.map((p) => (
            <div
              key={p.name}
              className={`relative card-glass flex flex-col p-8 transition-all duration-200 ${
                p.highlight
                  ? "border-brand-500/60 ring-1 ring-brand-500/30 shadow-lg shadow-brand-900/30"
                  : "hover:border-white/20"
              }`}
            >
              {p.badge && (
                <div className="absolute -top-3 left-1/2 -translate-x-1/2">
                  <span className="px-3 py-1 rounded-full text-xs font-bold bg-brand-600 text-white shadow">
                    {p.badge}
                  </span>
                </div>
              )}

              <div className="font-bold text-sm text-white/50 mb-2">{p.name}</div>
              <div className="flex items-end gap-1 mb-3">
                {p.monthly ? (
                  <>
                    <span className="text-4xl font-extrabold">
                      €{annual ? p.annual : p.monthly}
                    </span>
                    <span className="text-white/40 mb-1">/mo</span>
                  </>
                ) : (
                  <span className="text-4xl font-extrabold">Custom</span>
                )}
              </div>
              {annual && p.monthly && (
                <p className="text-xs text-accent-400 -mt-2 mb-3">
                  Billed annually — save €{(p.monthly - p.annual!) * 12}/yr
                </p>
              )}

              <p className="text-sm text-white/55 mb-6 leading-relaxed">{p.desc}</p>

              <a
                href="#"
                className={`text-center py-3 rounded-xl font-semibold text-sm transition-all mb-7 ${
                  p.highlight
                    ? "bg-brand-600 hover:bg-brand-500 text-white shadow-lg shadow-brand-900/40"
                    : "border border-white/15 hover:border-brand-500/50 text-white/80 hover:text-white"
                }`}
              >
                {p.cta}
              </a>

              <ul className="space-y-2.5 flex-1">
                {p.features.map((f) => (
                  <li key={f} className="flex items-start gap-2.5 text-sm text-white/65">
                    <CheckCircle2
                      size={15}
                      className={`shrink-0 mt-0.5 ${p.highlight ? "text-brand-400" : "text-white/30"}`}
                    />
                    {f}
                  </li>
                ))}
              </ul>
            </div>
          ))}
        </div>

        {/* Add-ons */}
        <div className="card-glass p-6">
          <h3 className="font-bold text-xs text-white/50 uppercase tracking-widest mb-5">
            Add-ons &amp; Usage Extras
          </h3>
          <div className="grid sm:grid-cols-2 lg:grid-cols-4 gap-4">
            {addons.map((a) => (
              <div key={a.name} className="bg-white/4 rounded-xl p-4">
                <div className="font-semibold text-sm mb-1">{a.name}</div>
                <div className="text-xs text-brand-400 font-mono">{a.price}</div>
              </div>
            ))}
          </div>
        </div>
        <p className="text-center text-xs text-white/30 mt-5">
          All prices in EUR excl. VAT. Stripe processing fees apply separately.
        </p>
      </div>
    </section>
  );
}
