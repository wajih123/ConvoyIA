import { ArrowRight, Mail } from "lucide-react";

export default function CTA() {
  return (
    <section className="py-24 border-t border-white/5">
      <div className="max-w-3xl mx-auto px-4 sm:px-6 lg:px-8 text-center">
        {/* Glow */}
        <div className="absolute left-1/2 -translate-x-1/2 w-[600px] h-[300px] bg-brand-600/15 rounded-full blur-3xl pointer-events-none" />

        <h2 className="relative text-4xl sm:text-5xl font-extrabold tracking-tight mb-5">
          Ready to launch your{" "}
          <span className="text-gradient">conveyance marketplace</span>?
        </h2>
        <p className="text-white/55 text-lg mb-10 max-w-xl mx-auto leading-relaxed">
          Start with a free 14-day sandbox trial. No credit card required.
          One API call to create your first mission.
        </p>

        <div className="flex flex-col sm:flex-row items-center justify-center gap-4">
          <a href="#pricing" className="btn-primary text-base px-8 py-4">
            Start Free Trial <ArrowRight size={18} />
          </a>
          <a href="mailto:hello@convoyia.io" className="btn-outline text-base px-8 py-4">
            <Mail size={18} /> Contact Sales
          </a>
        </div>

        <p className="mt-8 text-xs text-white/30">
          hello@convoyia.io · hello@goweyy.com
        </p>
      </div>
    </section>
  );
}
