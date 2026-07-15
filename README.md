# PUNDAR — Build Together. Grow Together.

<p align="center">
  <img src="app/src/main/res/drawable/logo.png" width="120" alt="PUNDAR Logo"/>
</p>

<p align="center">
  <strong>A next-generation Filipino fintech super-app powered by Stellar blockchain and Soroban smart contracts.</strong><br/>
  Pay · Save · Grow · Circle (Paluwagan)
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android-3DDC84?style=flat-square&logo=android"/>
  <img src="https://img.shields.io/badge/Kotlin-2.0.21-7F52FF?style=flat-square&logo=kotlin"/>
  <img src="https://img.shields.io/badge/Stellar-Soroban-000000?style=flat-square&logo=stellar"/>
  <img src="https://img.shields.io/badge/Firebase-Firestore-FFCA28?style=flat-square&logo=firebase"/>
  <img src="https://img.shields.io/badge/Jetpack_Compose-BOM_2024.09-4285F4?style=flat-square&logo=jetpackcompose"/>
  <img src="https://img.shields.io/badge/minSdk-26_(Android_8)-brightgreen?style=flat-square"/>
</p>

---

## 🌟 Contract Address

> **Soroban Smart Contract — Paluwagan Circle Escrow**
>
> ```
> PUNDAR_CIRCLE_CONTRACT_ADDRESS = <YOUR_CONTRACT_ADDRESS_HERE>
> ```
>
> Network: **Stellar Testnet / Futurenet**
> Contract: `circle_contract/src/lib.rs`

---

## 📖 Project Description

**PUNDAR** is a Filipino-first fintech super-app that combines everyday mobile payments with community-based savings (Paluwagan), micro-investing, and load purchasing — all secured by the **Stellar blockchain** and **Soroban smart contracts**.

The name *Pundar* (Filipino: "to save/invest") reflects our mission: making financial tools that are trustless, transparent, and built for the Filipino community.

### The Problem We're Solving

1. **Trust in Paluwagan** — Traditional Filipino group savings (Paluwagan) rely on personal trust. Members have no recourse if the organizer disappears with the pot.
2. **Financial exclusion** — Millions of Filipinos are unbanked or underserved by traditional financial institutions.
3. **Fragmented apps** — Users juggle separate apps for payments, savings, load, and investments.

### Our Solution

PUNDAR puts all of these into a single, beautiful app:
- 💳 **Wallet** — Send, receive, cash in via QR
- 🧾 **Pay (Group Bills)** — Split bills, track settlements, instant QR payments
- 🌀 **Circle (Paluwagan)** — Blockchain-secured rotating savings with Soroban escrow
- 📈 **Grow** — Philippine stock market micro-investing
- 📱 **Load** — Buy mobile load for Smart, Globe, TNT, TM, and DITO with prefix validation

---

## 🏗️ Architecture

```
┌─────────────────────────────────────────────────────────┐
│                     Android App                          │
│   Kotlin + Jetpack Compose (Material 3)                  │
│                                                          │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐   │
│  │   Home   │ │   Pay    │ │  Circle  │ │   Grow   │   │
│  │  Wallet  │ │  Bills   │ │Paluwagan │ │ Investing│   │
│  └──────────┘ └──────────┘ └──────────┘ └──────────┘   │
│                                                          │
│  ┌──────────────────────────────────────────────────┐   │
│  │               Data Layer                         │   │
│  │  AuthRepository · CircleRepository · PayRepo     │   │
│  │  GrowRepository · NotificationRepository         │   │
│  └──────────────────────────────────────────────────┘   │
└───────────────┬──────────────────────┬──────────────────┘
                │                      │
    ┌───────────▼──────────┐  ┌────────▼────────────────┐
    │  Google Firebase      │  │  Stellar Network        │
    │  Firestore (DB)       │  │  • XLM Wallet           │
    │  Security Rules       │  │  • Soroban Contract     │
    └───────────────────────┘  │  • Escrow Automation    │
                               └─────────────────────────┘
```

---

## 🔗 Stellar & Soroban Integration

### Soroban Smart Contract — `PundarCircleContract`

The heart of PUNDAR's Paluwagan feature is a **Soroban smart contract** written in Rust that:

| Feature | Description |
|---------|-------------|
| `create_circle` | Creates a new savings group with members, payout order, cycle duration, and token address |
| `contribute` | Members transfer tokens; contract verifies membership, amount, and prevents double-contribution |
| `check_and_payout` | When all members have contributed, automatically pays the next recipient in the rotation |
| `check_and_dissolve` | If a cycle deadline passes without full contribution, refunds all contributors |
| `get_circle_state` | Read-only state query for the mobile app |

**Security guarantees:**
- Funds are held **in-contract** — no single party controls the pot
- Payout order is set at creation and cannot be tampered with
- Automatic refund if cycle fails (deadline protection)
- Built with `soroban-sdk = "22.0.11"`

### Stellar Wallet Manager

Each PUNDAR user gets a **Stellar XLM wallet** generated and managed on-device:
- Keys are encrypted and stored locally with a pepper
- Real-time balance fetched from Stellar Horizon API
- Transaction signing for Soroban contract calls

---

## ✨ Features

### 💳 Home & Wallet
- Animated flippable wallet card (front: balance, back: virtual card)
- Hide/show balance toggle
- Activity feed (Pay, Circle, Grow events)
- Pull-to-refresh with live balance sync
- QR code send & receive

### 🧾 Pay — Group Bill Splitting
- Create group bills with multiple members
- Track pending, partial, and settled bills
- QR-based instant settlement
- Real-time Firestore sync
- Bill history with PUNDAR Score impact

### 🌀 Circle — Blockchain Paluwagan
- Create savings circles with configurable rules
- **Soroban smart contract escrow** — trustless and automated
- Admin panel: start/end cycle, manage members, set penalties
- 4 invitation methods: QR Code, Share Link, Username, Phone
- Join via deep-link invitation
- Member contribution tracking with status chips
- Pending join requests with approve/reject flow
- Cycle status lifecycle: Not Started → Active → Paused → Completed

### 📈 Grow — Micro-Investing
- Philippine Stock Exchange (PSE) holdings
- Portfolio overview with return metrics
- Stock detail screens with price history charts
- Invest / Withdraw dialogs
- Portfolio optimization suggestions
- Favorites system with Firebase persistence
- Auto-sweep round-up feature

### 📱 Load — Mobile Top-Up
- **5 networks**: Smart, TNT, Globe, TM, DITO
- Prefix-based network auto-detection and validation
- Named product catalog (GoSURF, GigaLife, DITO99, etc.)
- **Prepaid / Postpaid** architecture (Postpaid: coming soon)
- Balance check before purchase

---

## 🎨 Design System

PUNDAR uses a **custom futuristic fintech design system** built entirely with Jetpack Compose:

- **Color palette**: Deep navy (`#090F1F`) base with electric blue, neon green, and premium gold accents
- **AnimatedBackground**: Per-screen canvas animations — drifting radial orbs, rotating mesh lines, particle field, shimmer sweep
- **Flippable Wallet Card**: 3D flip animation with glowing shadow, pulsing orbs, NFC icon, verified badge
- **App Icon**: Adaptive icon with deep navy gradient, concentric ring reticle, P lettermark, dual arc sweeps in blue and gold
- **Typography**: Futuristic weight hierarchy from ExtraBold headers to Muted labels

---

## 🛠️ Tech Stack

| Category | Technology |
|----------|------------|
| **Language** | Kotlin 2.0.21 |
| **UI** | Jetpack Compose (BOM 2024.09.00) + Material 3 |
| **Navigation** | Navigation Compose 2.8.5 |
| **Blockchain** | Stellar SDK 4.0.0 + Soroban (Rust) |
| **Smart Contract** | Rust · soroban-sdk 22.0.11 |
| **Database** | Firebase Firestore |
| **Camera / QR** | CameraX 1.3.1 + ML Kit Barcode Scanning 17.2.0 |
| **QR Generation** | ZXing 3.5.3 |
| **Image Loading** | Coil Compose 2.7.0 |
| **Serialization** | kotlinx.serialization 1.6.3 |
| **Min SDK** | 26 (Android 8.0 Oreo) |
| **Target SDK** | 35 |

---

## 📁 Project Structure

```
PundarApp/
├── app/src/main/java/com/example/pundarapp/
│   ├── data/
│   │   ├── qr/              # QR payload model + generator + scanner
│   │   ├── remote/          # Repositories (Auth, Pay, Circle, Grow, etc.)
│   │   └── stellar/         # StellarWalletManager
│   └── ui/
│       ├── components/      # Shared Composables
│       │   ├── AnimatedBackground.kt   ← Per-screen canvas animations
│       │   ├── FlippableWalletCard.kt  ← 3D animated wallet
│       │   ├── PinInputComponent.kt    ← Secure PIN keypad
│       │   └── ...
│       ├── data/            # AppState + Data models
│       ├── navigation/      # PundarNavigation + Routes
│       ├── screens/
│       │   ├── auth/        # Login, Register
│       │   ├── home/        # Home, Send, Receive, BuyLoad, CashIn, Scan
│       │   ├── pay/         # PayScreen, BillDetail, InstantSettle, BillQR
│       │   ├── circle/      # CircleScreen, Detail, Admin, InviteMethods, Create
│       │   └── grow/        # GrowScreen, StockDetail
│       ├── theme/           # Color.kt, Type.kt, Theme.kt
│       └── utils/           # AnimationUtils, DesignTokens
├── circle_contract/         # Soroban Smart Contract (Rust)
│   └── src/
│       ├── lib.rs           # PundarCircleContract
│       └── test.rs          # Contract unit tests
├── firestore.rules          # Firestore security rules
└── supabase_schema.sql      # (Future) Supabase migration schema
```

---

## 🚀 Getting Started

### Prerequisites
- Android Studio Meerkat or newer
- JDK 11+
- Android device or emulator with API 26+
- (For contract) Rust + `soroban-cli`

### Clone & Run

```bash
git clone https://github.com/<your-username>/PundarApp.git
cd PundarApp
```

1. Open in **Android Studio**
2. Add your `google-services.json` to `app/`
3. Add `stellar.pepper=<your_pepper>` to `local.properties`
4. Click **Run ▶**

### Build the Soroban Contract

```bash
cd circle_contract
cargo build --target wasm32-unknown-unknown --release
```

Deploy to Stellar testnet:

```bash
soroban contract deploy \
  --wasm target/wasm32-unknown-unknown/release/circle_contract.wasm \
  --source <your_keypair> \
  --network testnet
```

> ⚠️ Update the contract address in this README and in `StellarWalletManager.kt` after deployment.

---

## 🧪 Smart Contract Tests

```bash
cd circle_contract
cargo test
```

The test suite in `src/test.rs` covers:
- Circle creation
- Member contribution flow
- Full-cycle payout triggering
- Deadline-based dissolution and refund

---

## 🔐 Security

- **PIN-protected** transactions (4-digit MPIN, verified against Firestore)
- **Biometric** authentication hook (future-ready)
- **Firestore Security Rules** enforce data shape validation, prevent balance manipulation, and restrict mpin field overwrites
- **Soroban contract** enforces contribution amounts, membership, and payout order — no admin override possible
- **QR anti-replay tokens** prevent double-spend attacks

---

## 🗺️ Roadmap

- [ ] Deploy contract to Stellar Mainnet
- [ ] Postpaid load support
- [ ] Real SMS invite via Twilio
- [ ] Push notifications (Firebase Cloud Messaging)
- [ ] Biometric authentication (BiometricPrompt)
- [ ] Real PSE market data feed
- [ ] KYC / eKYC integration
- [ ] Penalty management for missed contributions
- [ ] GCash / Maya cash-in bridge

---

## 🏆 Hackathon Track

**Track:** Stellar / Soroban DeFi & Web3 Financial Applications

**Why Stellar?**
- Fast finality (5 seconds) — critical for Paluwagan payouts
- Low fees — accessible to unbanked Filipinos
- Soroban's Rust-based smart contracts give us the security guarantees needed for group savings
- XLM as a settlement layer bridges fiat (PHP) and decentralized finance

---

## 👥 Team

| Role | Contribution |
|------|-------------|
| Mobile Developer | Jetpack Compose UI, Navigation, Repositories |
| Blockchain Developer | Soroban contract, Stellar wallet integration |
| Product Designer | Design system, UX flow, animated components |

---

## 📄 License

```
MIT License — © 2026 PUNDAR Team
```

---

<p align="center">
  <strong>PUNDAR — Build Together. Grow Together. 🇵🇭</strong>
</p>
