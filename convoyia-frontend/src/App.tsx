import Navbar    from "./components/Navbar";
import Hero      from "./components/Hero";
import Platform  from "./components/Platform";
import Agents    from "./components/Agents";
import APIs      from "./components/APIs";
import Pricing   from "./components/Pricing";
import Markets   from "./components/Markets";
import TechStack from "./components/TechStack";
import CTA       from "./components/CTA";
import Footer    from "./components/Footer";

export default function App() {
  return (
    <div className="min-h-screen bg-[#0a0a14]">
      <Navbar />
      <main>
        <Hero />
        <Platform />
        <Agents />
        <APIs />
        <Pricing />
        <Markets />
        <TechStack />
        <CTA />
      </main>
      <Footer />
    </div>
  );
}
