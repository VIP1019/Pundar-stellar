OFFICIAL ENTRY — APAC Stellar Hackathon
Submitted by The University of Camarines Norte

# PUNDAR

Progressive Unified Network for Digital Asset Resilience
" Every Peso Keeps Building."
---

## Overview

PUNDAR — Filipino for "to build" or "to establish" — is the next-generation financial ecosystem built to empower Filipinos through decentralized group savings, transparent bill management, and accessible investment tools.

Built on the Stellar Network, Pundar bridges traditional Filipino community finance — like the filipino finance culture "paluwagan" with modern blockchain infrastructure, giving every Filipino a fast, low-cost, and trustless way to save, spend, and grow their money together.

---

## Core Functionality: The Three Core Function

Pundar organizes financial life into three tightly integrated modules.

### PUNDAR CIRCLE — Decentralized Paluwagan
The center of Pundar. It digitizes the traditional Filipino "paluwagan"(Rotating Savings and Credit Association).

- Soroban Smart Contracts — Group funds are locked in secure Soroban escrows on the Stellar Network, so no single member can abscond with the pot.
- Automated Cycles — Handles invitations, join requests, and contribution tracking with cryptographic transparency.
- Pundar Score — A reputation system that rewards consistent contributors, making community trust measurable.

### PUNDAR PAY — Social Payments
Simplifies group financial obligations through intuitive social tools.

- Group Bill Splitting— Create and track shared expenses (e.g., "Cafe with friends").
- QR-Powered Transfers — Stellar-backed QR payloads for instant, low-cost peer-to-peer transfers and bill settlements.
- Real-Time Activity Feed — A centralized "Home" feed tracking every peso moved within the ecosystem.

### PUNDAR GROW — Inclusive Wealth Building
Lowers the barrier to entry for the Philippine stock market and fixed-income assets.

- Micro-Investments — Access to PH Equities and Fixed Income portfolios.
- Auto-Sweep & Round-Ups — Automatically invests spare change from daily transactions into a diversified portfolio.
- Interactive Analytics — Real-time performance tracking with custom-built Compose charts.

---

## Tech Stack & Architecture

| Layer | Technology |

| Language | 100% Kotlin |

| UI Framework | Jetpack Compose — declarative, reactive UI with a custom "Futuristic" design language |

| State Management | Centralized Compose Snapshot State via a singleton `AppState` for instantaneous UI updates app-wide |

| Backend | Firebase Firestore — real-time database for user profiles, social metadata, and notifications |

| Blockchain — Stellar SDK | Custodial wallet generation, XLM/USDC balance management, testnet funding |

| Blockchain — Soroban| Smart contracts powering decentralized escrow and paluwagan logic |

| Security | 4-digit MPIN for local sessions and transaction signing; AES encryption for stored Stellar secret seeds |

| Hardware Integration | ML Kit & CameraX for high-speed QR code scanning and generation |

---

## Getting Started (For Judges & Developers)

### Prerequisites
- Android Studio 
- Kotlin 1.9.0+
- A Firebase project with Firestore enabled
- Stellar Testnet access (Horizon)

### Installation

```bash
# 1. Clone the repo
git clone https://github.com/VIP1019/Pundar-stellar.git
cd Pundar-stellar
```
2. Firebase Setup — Add your `google-services.json` file to the `/app` directory.
3. Build the project:
   ```bash
   ./gradlew assembleDebug
   ```
4. Onboarding — Register with a phone number. Pundar automatically generates and funds a Stellar Testnet wallet for you instantly. No manual wallet setup required.

---

## Roadmap

- [/] Phase 1 — Core "Circle" logic with Soroban integration 
- [ ] Phase 2 — Fiat on/off-ramps via local Philippine payment gateways (GCash / Maya bridge)
- [ ] Phase 3 — AI-driven "Pundar Assistant" to optimize savings based on spending habits

---

## Team

**University of Camarines Norte**
| Name               | Contact                      |
|--------------------|------------------------------|
| Marc Lester Acunin | Coach                        |
| Prince Jheck Juan  | princejheckjuan023@gmail.com |
| Mark James De Rosa | markjamesderosa73@gmail.com  |
| Quintin Buena      | buenaquintin5@gmail.com      |
| Yasmine Abarca     | yasmineabarca495@gmail.com   |

---

## License

© 2026 Pundar — University of Camarines Norte. All Rights Reserved.

This project and its source code are proprietary and submitted 
exclusively for the APAC Stellar Hackathon. No part of this repository 
may be copied, modified, distributed, or used for commercial purposes 
without explicit written permission from the team.

---

Pundar: " Every Peso Keeps Building." 




