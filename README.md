# PUNDAR

### Progressive Unified Network for Digital Asset Resilience

> **“Every Peso Keeps Building.”**

**Official Entry — APAC Stellar Hackathon**
**Submitted by the University of Camarines Norte**

---

## Overview

**PUNDAR** — derived from the Filipino word *“pundar,”* meaning to build, establish, or accumulate — is a Stellar-based financial platform designed to bring **shared payments, community savings, and financial-growth tracking** into one integrated ecosystem.

PUNDAR combines familiar Filipino financial practices, particularly the **Paluwagan** community savings model, with Stellar blockchain infrastructure to create a transparent, low-cost, and user-friendly financial experience.

The platform consists of three integrated modules:

* **PUNDAR Pay** — Shared expenses and social payments
* **PUNDAR Circle** — Blockchain-powered community savings
* **PUNDAR Grow** — Spare-change accumulation and portfolio tracking

PUNDAR is currently deployed and validated as a **functional Stellar Testnet MVP**. Its architecture is designed to support transition to **Stellar Mainnet** through the appropriate Mainnet contracts, assets, wallets, and operational configuration once the required security, operational, and compliance requirements are satisfied.

---

# Core Functionality

PUNDAR organizes everyday financial activities into three connected modules.

## PUNDAR Pay — Shared Social Payments

PUNDAR Pay simplifies shared expenses by allowing users to create and manage group financial obligations within one platform.

### Features

* **Shared Expense Creation** — Create expenses and identify participating users.
* **Automatic Share Calculation** — Calculate each participant's required contribution based on the configured expense.
* **Non-Custodial Wallet Authorization** — Users authorize their own applicable Stellar transactions through the supported wallet flow.
* **Stellar Testnet Settlement** — Payment transactions are submitted and settled on the Stellar Testnet.
* **Transaction Verification** — Completed settlements can be independently verified using Stellar transaction records.
* **Transaction History** — Users can review completed payment activity within the application.
* **QR-Based Payment Workflow** — QR functionality supports payment-related user flows where applicable.

PUNDAR Pay does not take custody of users' private keys or directly control users' blockchain funds.

---

## PUNDAR Circle — Blockchain-Powered Paluwagan

PUNDAR Circle digitizes the traditional Filipino **Paluwagan**, or rotating community savings model, using **Soroban smart contracts on Stellar**.

Users can create or join a savings circle, define contribution requirements, register members, and follow a fixed rotating payout order.

### Features

* **Circle Creation** — Create a digital savings circle with defined contribution parameters.
* **Member Registration** — Define the authorized participants of the Circle.
* **Fixed Contributions** — Establish the contribution amount and schedule for the Circle.
* **Fixed Rotating Payout Order** — Members receive payouts according to the predefined rotation.
* **Soroban Smart Contracts** — Contract logic maintains Circle state and enforces predefined rules.
* **Contribution Validation** — Contributions are checked against the Circle's requirements.
* **Payout Eligibility** — The smart contract verifies whether the current recipient satisfies the predefined payout conditions.
* **Contract-Enforced Payout Flow** — Valid payout transactions are submitted only when the required Circle conditions are satisfied.
* **On-Chain Verification** — Contributions, contract interactions, and applicable payout transactions can be independently verified on Stellar Testnet.

### MVP Scope

The current Circle implementation focuses on a **fixed contribution amount and fixed rotating payout order**.

Advanced mechanisms such as:

* lotteries,
* slashing,
* bonded collateral,
* penalty-based economics,
* alternative payout models, and
* complex credit-risk mechanisms

are outside the current MVP scope.

For Testnet validation, Circle contributions and payouts use a designated/project-issued Testnet asset. The asset code and issuer public address are documented separately without exposing private keys.

---

## PUNDAR Grow — Spare-Change Financial Growth

PUNDAR Grow is designed to make financial-growth tracking more accessible by helping users understand how small amounts accumulated from everyday transactions can build over time.

### Features

* **Spare-Change Round-Up** — Calculate the applicable round-up amount from eligible transactions.
* **Accumulated Spare Change** — Track the total amount accumulated through round-ups.
* **Portfolio Allocation** — Demonstrate simulated or Testnet-only allocation of accumulated amounts.
* **Portfolio Visualization** — Display tracked portfolio information through the application.
* **Activity History** — Review round-up and portfolio-related activity.
* **Future RWA Readiness** — The architecture can support future compliant tokenized real-world asset integrations.

### MVP Limitation

During the current MVP, PUNDAR Grow:

* does **not** execute real investments;
* does **not** custody investment funds;
* does **not** provide investment advice;
* does **not** offer live tokenized real-world assets;
* does **not** represent simulated/Testnet balances as real-world investment holdings.

The current implementation focuses on demonstrating the **round-up, accumulation, tracking, and portfolio visualization workflow**.

Future integrations with compliant financial and digital-asset providers may be explored separately.

---

# Unified PUNDAR Experience

PUNDAR connects the three modules into one financial workflow:

```text
                    PUNDAR
                       |
        +--------------+--------------+
        |              |              |
        v              v              v
   PUNDAR Pay     PUNDAR Circle   PUNDAR Grow
        |              |              |
 Shared Expenses   Community      Spare-Change
 & Payments         Savings        Tracking
        |              |              |
        +--------------+--------------+
                       |
                       v
              Stellar Ecosystem
                       |
              +--------+--------+
              |                 |
              v                 v
          Stellar          Soroban
          Network         Smart Contracts
```

Users can move between shared payments, community savings, and financial-growth tracking without needing to manage blockchain infrastructure directly.

Where blockchain transactions are required, users authorize their own transactions through the supported non-custodial wallet flow.

---

# Stellar Ecosystem Integration

Stellar is a core component of PUNDAR's architecture rather than simply an external payment option.

## Stellar Network

Stellar provides the blockchain settlement layer for applicable PUNDAR transactions.

PUNDAR uses Stellar for:

* payment settlement;
* Testnet transaction verification;
* wallet-based authorization;
* asset transfers;
* transaction history and verification.

## Soroban Smart Contracts

Soroban powers the programmable logic behind **PUNDAR Circle**.

The smart contracts are responsible for maintaining Circle state and enforcing predefined rules related to:

* membership;
* contribution requirements;
* payout eligibility;
* payout order; and
* Circle completion.

## Stellar Wallets

PUNDAR follows a **non-custodial transaction model**.

Users authorize applicable blockchain transactions through their supported wallet flow. PUNDAR does not claim custody of users' private keys or present the MVP as a custodial financial service.

## Testnet and Mainnet

The current implementation is configured for **Stellar Testnet** for development, validation, and demonstration.

The system architecture is designed to support transition to **Stellar Mainnet** by deploying the appropriate Mainnet contracts and configuring the corresponding Mainnet assets and wallet/network settings.

Mainnet deployment is subject to appropriate security, operational, and compliance requirements.

---

# Architecture

```text
+-------------------------------------------------------+
|                    PUNDAR Mobile App                  |
|                                                       |
|  PUNDAR Pay | PUNDAR Circle | PUNDAR Grow            |
+---------------------------+---------------------------+
                            |
                            v
+-------------------------------------------------------+
|                 Application Layer                    |
|                                                       |
|  User Interface | Business Logic | Local State       |
+---------------------------+---------------------------+
                            |
              +-------------+-------------+
              |                           |
              v                           v
+-------------------------+   +-------------------------+
|     Off-Chain Layer     |   |    Stellar Layer        |
|                         |   |                         |
| Firebase / App Data     |   | Stellar Transactions    |
| User Metadata           |   | Wallet Authorization    |
| Notifications           |   | Testnet Assets         |
| UI Preferences          |   | Soroban Contracts      |
+-------------------------+   +-------------------------+
```

The architecture separates application-level information from blockchain transactions.

### Off-Chain Data

Examples include:

* user profiles;
* display information;
* notifications;
* application preferences;
* interface metadata; and
* other non-financial application data.

### On-Chain Data

Applicable blockchain activity includes:

* Stellar payment settlements;
* Circle contributions;
* Soroban contract interactions;
* payout transactions; and
* relevant asset transfers.

This separation allows PUNDAR to provide a user-friendly application while retaining independent verification for applicable blockchain activity.

---

# Tech Stack

| Layer                          | Technology                        |
| ------------------------------ | --------------------------------- |
| **Language**                   | Kotlin                            |
| **UI Framework**               | Jetpack Compose                   |
| **State Management**           | Centralized application state     |
| **Backend / Application Data** | Firebase Firestore                |
| **Blockchain**                 | Stellar Network                   |
| **Smart Contracts**            | Soroban                           |
| **Wallet Architecture**        | Non-custodial Stellar wallet flow |
| **QR Functionality**           | ML Kit / CameraX                  |
| **Development Environment**    | Android Studio                    |
| **Version Control**            | Git / GitHub                      |

---

# Getting Started

## Prerequisites

* Android Studio
* Kotlin-compatible Android development environment
* Firebase project with Firestore enabled
* Stellar Testnet access
* Git

## Installation

### 1. Clone the Repository

```bash
git clone https://github.com/VIP1019/Pundar-stellar.git
cd Pundar-stellar
```

### 2. Configure Firebase

Add the appropriate:

```text
google-services.json
```

file to the:

```text
/app
```

directory.

Do not commit private credentials, secrets, API keys, or production configuration files to the repository.

### 3. Build the Project

```bash
./gradlew assembleDebug
```

### 4. Configure Stellar Testnet

Configure the application to use the appropriate Stellar Testnet network and wallet configuration for development and testing.

For Testnet testing, use Testnet assets and wallets only.

---

# Security and Trust Model

PUNDAR follows a **non-custodial architecture** for applicable blockchain transactions.

Users are responsible for authorizing their own blockchain transactions through their supported wallet flow.

The MVP does not claim to be:

* security-audited;
* production financial infrastructure;
* a custodial wallet service;
* a real-money investment platform; or
* a regulated financial service.

Smart-contract correctness, wallet security, Testnet limitations, and other known technical risks remain part of the project's documented security considerations.

---

# Current Status

PUNDAR is currently at the **functional MVP stage**.

### Current Progress

* [x] Core PUNDAR platform developed
* [x] PUNDAR Pay workflow implemented
* [x] PUNDAR Circle workflow implemented
* [x] Soroban integration developed for Circle logic
* [x] PUNDAR Grow workflow implemented
* [x] Stellar Testnet configuration
* [x] Non-custodial wallet transaction flow
* [x] Integrated platform experience
* [x] Testing and validation workflow
* [ ] Stellar Mainnet deployment
* [ ] External cash-in/cash-out integrations
* [ ] Production financial-service integrations
* [ ] Future compliant RWA integrations

The Mainnet transition is planned as a deployment/configuration step after the necessary security, operational, and compliance requirements are satisfied.

---

# Roadmap

## Phase 1 — Stellar Testnet MVP

* Complete core PUNDAR Pay functionality
* Complete PUNDAR Circle Soroban workflows
* Complete PUNDAR Grow round-up and portfolio tracking
* Validate end-to-end Stellar Testnet transactions
* Strengthen testing and documentation

## Phase 2 — Stellar Mainnet Transition

* Deploy appropriate Soroban contracts to Mainnet
* Configure Mainnet assets
* Configure Mainnet wallet/network settings
* Conduct additional security and operational testing
* Prepare the application for real-world Mainnet usage

## Phase 3 — External Financial Integrations

Explore integrations with compliant external financial and digital-asset providers through APIs.

Potential future use cases include:

* cash-in;
* cash-out;
* asset conversion;
* external payment integrations; and
* connections between supported Philippine financial services and the Stellar ecosystem.

Potential providers such as **PDAX, GCash, or Maya** are considered future integration possibilities and are **not represented as currently integrated unless an official technical integration has been completed**.

## Phase 4 — Future Financial Growth Ecosystem

Explore compliant expansion of PUNDAR Grow toward:

* tokenized real-world assets;
* additional portfolio products;
* improved financial analytics;
* personalized financial tools; and
* AI-assisted savings guidance.

These capabilities are subject to technical feasibility, provider availability, security requirements, and applicable regulatory/compliance requirements.

---

# Team

## University of Camarines Norte

| Name                   | Role / Contact                                                                                              |
| ---------------------- | ----------------------------------------------------------------------------------------------------------- |
| **Marc Lester Acunin** | Coach                                                                                                       |
| **Prince Jheck Juan**  | Lead Developer / Full-Stack Developer — [princejheckjuan023@gmail.com](mailto:princejheckjuan023@gmail.com) |
| **Mark James De Rosa** | Team Member — [markjamesderosa73@gmail.com](mailto:markjamesderosa73@gmail.com)                             |
| **Quintin Buena**      | Team Member — [buenaquintin5@gmail.com](mailto:buenaquintin5@gmail.com)                                     |
| **Yasmine Abarca**     | Team Member — [yasmineabarca495@gmail.com](mailto:yasmineabarca495@gmail.com)                               |

The team combines experience in full-stack development, mobile application development, system analysis, software engineering, and blockchain ecosystem development.

PUNDAR has also been developed and presented through the **APAC Stellar Hackathon**, where the team advanced to the international grand finale as a finalist.

---

# Hackathon Submission

**Event:** APAC Stellar Hackathon
**Project:** PUNDAR
**Category:** Stellar-based financial application
**Current Network:** Stellar Testnet
**Smart Contract Platform:** Soroban
**Repository:** https://github.com/VIP1019/Pundar-stellar

PUNDAR's current MVP focuses on demonstrating practical Stellar utility through shared payments, community savings, and financial-growth tracking.

---

# License

The PUNDAR source code and associated project materials are not licensed for public copying, modification, redistribution, or commercial use without permission from the PUNDAR team.

For requests regarding the use, reproduction, modification, distribution, collaboration, or commercial use of this project, please contact the PUNDAR team directly for permission.

**Contact:** [princejheckjuan023@gmail.com](mailto:princejheckjuan023@gmail.com)

---

## PUNDAR

### **“Every Peso Keeps Building.”**

**Build together. Save together. Grow together.**
