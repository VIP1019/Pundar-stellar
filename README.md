# PUNDAR

### Progressive Unified Network for Digital Asset Resilience

> **“Every Peso Keeps Building.”**

**Official Entry — APAC Stellar Hackathon**
**Submitted by the University of Camarines Norte**

## Weekly Sprint Deliverable: PUNDAR Pay (Week 1)

### 🎯 Scope & Objectives
- Implemented end-to-end PUNDAR Pay shared-expense settlement on Stellar Testnet.
- Restricted split-bill transactions, invitations, and transfers strictly to verified registered accounts.
- Implemented real Stellar Testnet transaction signing via 4-digit MPIN authorization.
- Added GCash-style receipt generation with verifiable Stellar Expert links upon payment confirmation.

### 📦 Key Code Changes & Deliverables
- **`NewGroupBillScreen.kt`**: Multi-step split bill flow with verified participant discovery, testnet payment execution, and receipt generation.
- **`AuthRepository.kt`**: Added `findRegisteredUser` for strict verified phone / Stellar public key lookups.
- **`StellarWalletManager.kt`**: Stellar Testnet transaction creation, signing, and friendbot funding.
- **`SendMoneyScreen.kt` & `QrSendConfirmScreen.kt`**: User verification & QR payment rail via Stellar Testnet.
- **`PundarBottomBar.kt`**: Balanced navigation UI with profile section.
