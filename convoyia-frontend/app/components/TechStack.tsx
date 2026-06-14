import { Server } from "lucide-react";

const stacks = [
  {
    category: "Backend",
    items: [
      { name: "Java 21", desc: "Virtual threads, records, pattern matching" },
      { name: "Spring Boot 3.5", desc: "Reactive WebFlux + Spring AI" },
      { name: "PostgreSQL", desc: "Primary datastore per agent" },
      { name: "Kafka", desc: "Inter-agent event streaming" },
      { name: "Redis", desc: "GPS trajectory cache & rate limiting" },
    ],
  },
  {
    category: "AI / LLM",
    items: [
      { name: "Phi-3 Mini", desc: "Fast mission qualification" },
      { name: "Mistral 7B", desc: "Routing + verification reasoning" },
      { name: "LLaMA 3 8B", desc: "Anomaly detection in tracking" },
      { name: "Qwen-VL 7B", desc: "Vehicle vision inspection" },
      { name: "Claude Sonnet", desc: "Fallback when confidence < 0.72" },
    ],
  },
  {
    category: "Infrastructure",
    items: [
      { name: "Kubernetes", desc: "Agent orchestration on Hetzner Cloud" },
      { name: "Keycloak", desc: "OAuth2 / OIDC for tenant SSO" },
      { name: "Stripe Connect", desc: "Payments, payouts, invoicing" },
      { name: "Ollama", desc: "Self-hosted LLM inference — no vendor lock-in" },
      { name: "Docker", desc: "Containerised microservices" },
    ],
  },
];

export default function TechStack() {
  return (
    <section id="tech" className="py-24 border-t border-white/5">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="max-w-2xl mx-auto text-center mb-16">
          <p className="section-label">
            <Server size={14} /> Tech Stack
          </p>
          <h2 className="text-4xl sm:text-5xl font-extrabold tracking-tight mb-4">
            Enterprise-grade,{" "}
            <span className="text-gradient">open source first</span>
          </h2>
          <p className="text-white/55 text-lg">
            ConvoyIA runs 100% on open-source LLMs by default. No LLM API bill.
            No usage data leaving your servers. Claude is a fallback, never the default.
          </p>
        </div>

        <div className="grid lg:grid-cols-3 gap-6">
          {stacks.map((s) => (
            <div key={s.category} className="card-glass p-6">
              <h3 className="font-bold text-xs uppercase tracking-widest text-white/45 mb-5">
                {s.category}
              </h3>
              <ul className="space-y-4">
                {s.items.map((item) => (
                  <li key={item.name} className="flex items-start gap-3">
                    <span className="mt-0.5 w-2 h-2 rounded-full bg-brand-500 shrink-0" />
                    <div>
                      <div className="font-semibold text-sm">{item.name}</div>
                      <div className="text-xs text-white/45 mt-0.5">{item.desc}</div>
                    </div>
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
