import { BrainCircuit, Layers, Lock, RefreshCw } from "lucide-react";

const pillars = [
  {
    icon: <BrainCircuit size={24} className="text-brand-400" />,
    title: "AI-Native Architecture",
    desc: "Every workflow is driven by open-source LLMs (Mistral 7B, Phi-3, LLaMA 3, Qwen-VL) running on your own infra — with Claude Sonnet as a fallback. Zero vendor lock-in.",
  },
  {
    icon: <Layers size={24} className="text-accent-400" />,
    title: "Full Mission Lifecycle",
    desc: "From the moment a driver accepts a mission to the automated invoice, ConvoyIA orchestrates every step through interconnected agents over Kafka event streams.",
  },
  {
    icon: <Lock size={24} className="text-brand-300" />,
    title: "Compliance First",
    desc: "Automated KYC, licence checks, background verifications, and document expiry — configurable per market. Casier B3 in France, DBS in the UK, and more.",
  },
  {
    icon: <RefreshCw size={24} className="text-accent-500" />,
    title: "White-Label SaaS",
    desc: "Each tenant gets its own currency, language, tax rules, pricing formula, platform fee, and branded UI. Go live in a new market without touching the core.",
  },
];

export default function Platform() {
  return (
    <section id="platform" className="py-24 border-t border-white/5">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="max-w-2xl mx-auto text-center mb-16">
          <p className="section-label">
            <BrainCircuit size={14} /> What is ConvoyIA
          </p>
          <h2 className="text-4xl sm:text-5xl font-extrabold tracking-tight mb-4">
            Infrastructure for{" "}
            <span className="text-gradient">vehicle conveyance</span> platforms
          </h2>
          <p className="text-white/55 text-lg leading-relaxed">
            ConvoyIA is a vertical AI platform that lets any vehicle marketplace
            launch, operate, and scale driver-based conveyance missions with
            production-grade AI — deployed on Kubernetes in hours.
          </p>
        </div>

        <div className="grid sm:grid-cols-2 gap-6">
          {pillars.map((p) => (
            <div key={p.title} className="card-glass p-7 hover:border-brand-600/40 transition-colors">
              <div className="mb-4">{p.icon}</div>
              <h3 className="font-bold text-lg mb-2">{p.title}</h3>
              <p className="text-white/55 text-sm leading-relaxed">{p.desc}</p>
            </div>
          ))}
        </div>
      </div>
    </section>
  );
}
