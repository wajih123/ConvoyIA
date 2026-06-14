# ConvoyIA — AI Agent Platform for Vehicle Conveyance

## What is ConvoyIA?

ConvoyIA is a vertical AI platform that powers vehicle conveyance marketplaces
worldwide. It provides 6 intelligent agents that handle the full lifecycle of
a conveyance mission — from dispatch to billing.

## Live Proof of Concept

**Goweyy** (Nice, France) is the first production deployment of ConvoyIA.
Every mission on Goweyy validates ConvoyIA's agents in real conditions.

## Agents

| Agent | Role |
|-------|------|
| Dispatcher | Mission qualification, routing, orchestration |
| Verifier | Driver + vehicle compliance, document checks |
| Pricer | Intelligent pricing with full breakdown |
| Inspector | Vision AI for vehicle condition (pre/post mission) |
| Tracker | Real-time GPS, anomaly detection |
| Biller | Automated split billing, invoicing, Stripe Connect |

## White-Label SaaS

ConvoyIA is sold as a white-label SaaS to vehicle conveyance platforms worldwide.
Each tenant configures: currency, language, tax rules, insurance, background
check requirements, pricing formula, platform fee, and return trip partner.

## Markets Roadmap

| Phase | Markets |
|-------|---------|
| M0 (now) | France (Goweyy) |
| M6 | UK, Belgium, Netherlands |
| M12 | Germany, Spain, Italy |
| M18 | UAE, Saudi Arabia |
| M24 | Australia, Canada, USA |

## Tech Stack

Java 21 · Spring Boot 3.5 · PostgreSQL · Kafka · Redis
Ollama (Mistral 7B, Phi-3, LLaMA 3, Qwen-VL) · Stripe Connect
Keycloak · Hetzner Cloud · Kubernetes

## LLM Strategy

Open source first (Ollama, self-hosted):
- Phi-3 Mini — fast qualification
- Mistral 7B — routing + verification reasoning
- LLaMA 3 8B — anomaly detection
- Qwen-VL 7B — vehicle vision inspection

Claude Sonnet fallback only when confidence < 0.72.
Zero LLM vendor lock-in.

## Worldwide Dimensions

Every dimension that varies per country or client is configurable per tenant:

| Dimension | Goweyy (France) | ConvoyIA Worldwide |
|-----------|-----------------|-------------------|
| Language | French | Multi-langue i18n |
| Currency | EUR | Multi-devise ISO 4217 |
| Tax | TVA 20% | Configurable per market |
| Insurance | Hiscox France | Per local market |
| Driver licence | Category B France | Mapping per country |
| Background check | Casier B3 (90 days) | Configurable per tenant |
| Payment | Stripe EUR | Stripe multi-currency |
| Return trip | Bolt Business FR | Local partner per market |
| Platform fee | 25% Goweyy | Configurable per tenant |
| Address validation | French format | Google Places global |
| Timezone | Europe/Paris | Multi-timezone |

## Supported Markets (ConvoyMarket enum)

France · United Kingdom · Germany · Spain · Italy · Netherlands · Belgium ·
UAE · Saudi Arabia · Australia · United States · Canada
