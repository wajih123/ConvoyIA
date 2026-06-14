import { ArrowRight, Globe, Shield, Sparkles } from "lucide-react";

const badges = [
  { icon: <Shield size={13} />,   label: "Production-ready" },
  { icon: <Globe size={13} />,    label: "12+ markets"      },
  { icon: <Sparkles size={13} />, label: "6 AI agents"      },
];

const metrics = [
  { value: "6",      label: "AI Agents"      },
  { value: "12+",    label: "Markets"        },
  { value: "<72ms",  label: "Avg. latency"   },
  { value: "99.9%",  label: "Uptime SLA"     },
];

export default function Hero() {
  return (
    <section className="relative pt-32 pb-24 overflow-hidden">
      {/* Background glows */}
      <div className="absolute inset-0 pointer-events-none">
        <div className="absolute top-0 left-1/2 -translate-x-1/2 w-[900px] h-[600px] bg-brand-600/10 rounded-full blur-3xl" />
        <div className="absolute top-0 left-1/2 -translate-x-1/2 w-[500px] h-[300px] bg-brand-500/8 rounded-full blur-2xl" />
      </div>

      <div className="relative max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 text-center">
        {/* Badges */}
        <div className="flex flex-wrap justify-center gap-2 mb-8">
          {badges.map((b) => (
            <span
              key={b.label}
              className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-medium bg-brand-900/60 border border-brand-700/50 text-brand-300"
            >
              {b.icon}{b.label}
            </span>
          ))}
        </div>

        {/* Headline */}
        <h1 className="text-5xl sm:text-6xl lg:text-7xl font-extrabold tracking-tight leading-[1.08] mb-6">
          The AI Platform That{" "}
          <br className="hidden sm:block" />
          <span className="text-gradient">Powers Vehicle</span>
          <br className="hidden sm:block" />
          Conveyance Marketplaces
        </h1>

        <p className="max-w-2xl mx-auto text-lg sm:text-xl text-white/60 mb-10 leading-relaxed">
          ConvoyIA delivers 6 intelligent agents — dispatch, verification, pricing,
          inspection, tracking, and billing — as white-label APIs your platform
          plugs into in days, not months.
        </p>

        {/* CTAs */}
        <div className="flex flex-col sm:flex-row items-center justify-center gap-4">
          <a href="#pricing" className="btn-primary text-base px-8 py-4">
            Start Building <ArrowRight size={18} />
          </a>
          <a href="#agents" className="btn-outline text-base px-8 py-4">
            Explore Agents
          </a>
        </div>

        {/* Social proof */}
        <p className="mt-10 text-sm text-white/40">
          Powering{" "}
          <span className="text-white/70 font-semibold">Goweyy</span>
          {" "}— live in production, Nice, France · Expanding to 12 markets
        </p>

        {/* Metric cards */}
        <div className="mt-16 grid grid-cols-2 sm:grid-cols-4 gap-4 max-w-3xl mx-auto">
          {metrics.map((m) => (
            <div key={m.label} className="card-glass p-4 text-center hover:border-white/20 transition-colors">
              <div className="text-2xl font-extrabold text-gradient">{m.value}</div>
              <div className="text-xs text-white/50 mt-1">{m.label}</div>
            </div>
          ))}
        </div>
      </div>
    </section>
  );
}
