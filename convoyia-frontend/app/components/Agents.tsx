import {
  Navigation2,
  ShieldCheck,
  TrendingUp,
  Camera,
  MapPin,
  Receipt,
} from "lucide-react";

const agents = [
  {
    id: "dispatcher",
    icon: <Navigation2 size={22} />,
    color: "from-brand-600 to-brand-400",
    name: "Dispatcher",
    tagline: "Mission qualification & orchestration",
    desc: "Qualifies incoming requests, computes optimal routing, assigns the best available driver, and orchestrates all downstream agents through a Kafka event bus.",
    features: ["AI-based driver scoring", "Multi-stop routing", "Real-time re-routing", "SLA monitoring"],
  },
  {
    id: "verifier",
    icon: <ShieldCheck size={22} />,
    color: "from-violet-600 to-violet-400",
    name: "Verifier",
    tagline: "Compliance & document checks",
    desc: "Automatically checks driver licence categories, background certificates, insurance documents, and vehicle papers — with configurable rules per market.",
    features: ["Licence category mapping", "Background check API", "Document OCR + expiry", "KYC workflow"],
  },
  {
    id: "pricer",
    icon: <TrendingUp size={22} />,
    color: "from-emerald-600 to-emerald-400",
    name: "Pricer",
    tagline: "Intelligent dynamic pricing",
    desc: "Computes a full price breakdown — base rate, distance, duration, tolls, return trip, platform fee, insurance, VAT — with configurable formulas per tenant.",
    features: ["Formula-based pricing", "Real-time toll data", "Platform fee split", "Multi-currency"],
  },
  {
    id: "inspector",
    icon: <Camera size={22} />,
    color: "from-orange-600 to-amber-400",
    name: "Inspector",
    tagline: "Vision AI for vehicle condition",
    desc: "Qwen-VL 7B analyses pre- and post-mission photos to detect scratches, dents, and damage — generating structured condition reports with confidence scores.",
    features: ["Pre/post comparison", "Damage localisation", "Confidence scoring", "Report generation"],
  },
  {
    id: "tracker",
    icon: <MapPin size={22} />,
    color: "from-sky-600 to-cyan-400",
    name: "Tracker",
    tagline: "Real-time GPS & anomaly detection",
    desc: "Receives GPS pings over WebSocket, stores trajectory in Redis, and uses LLaMA 3 8B to detect anomalies — route deviation, idling, speed events.",
    features: ["Live GPS stream", "Route deviation alerts", "Idle detection", "ETA prediction"],
  },
  {
    id: "biller",
    icon: <Receipt size={22} />,
    color: "from-pink-600 to-rose-400",
    name: "Biller",
    tagline: "Automated billing & Stripe Connect",
    desc: "Triggers on mission completion, charges the client via Stripe, splits payouts to driver and platform with configurable rates, and generates PDF invoices.",
    features: ["Stripe Connect", "Automatic split", "PDF invoicing", "Multi-currency payouts"],
  },
];

export default function Agents() {
  return (
    <section id="agents" className="py-24 border-t border-white/5">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="max-w-2xl mx-auto text-center mb-16">
          <p className="section-label">
            <Navigation2 size={14} /> AI Agents
          </p>
          <h2 className="text-4xl sm:text-5xl font-extrabold tracking-tight mb-4">
            Six agents.{" "}
            <span className="text-gradient">One complete platform.</span>
          </h2>
          <p className="text-white/55 text-lg">
            Each agent is a standalone microservice with its own REST API,
            Kafka topics, and LLM model. Use all six or compose only the ones
            you need.
          </p>
        </div>

        <div className="grid sm:grid-cols-2 lg:grid-cols-3 gap-6">
          {agents.map((a) => (
            <div
              key={a.id}
              className="card-glass p-6 flex flex-col hover:border-white/20 transition-colors group"
            >
              <div
                className={`w-11 h-11 rounded-xl bg-gradient-to-br ${a.color} flex items-center justify-center text-white mb-5 shadow-lg`}
              >
                {a.icon}
              </div>
              <div className="font-bold text-lg mb-0.5">{a.name}</div>
              <div className="text-xs text-white/45 mb-3">{a.tagline}</div>
              <p className="text-sm text-white/60 leading-relaxed mb-5 flex-1">{a.desc}</p>
              <ul className="space-y-1.5">
                {a.features.map((f) => (
                  <li key={f} className="flex items-center gap-2 text-xs text-white/55">
                    <span className="w-1.5 h-1.5 rounded-full bg-brand-400 shrink-0" />
                    {f}
                  </li>
                ))}
              </ul>
            </div>
          ))}
        </div>
      </div>
    </section>
  );
}
