package com.example.pundarapp.data.stellar

import org.stellar.sdk.Asset
import org.stellar.sdk.KeyPair

object MockRwaIssuer {
    // Deterministic keypair for our Mock Issuer on the Testnet
    // Using a fixed 32-byte array to generate the same KeyPair every time.
    val ISSUER_KEYPAIR: KeyPair by lazy {
        val seedBytes = ByteArray(32) { 42.toByte() }
        KeyPair.fromSecretSeed(seedBytes)
    }

    const val ASSET_CODE = "PSP500"
    
    val ASSET: Asset by lazy {
        Asset.createNonNativeAsset(ASSET_CODE, ISSUER_KEYPAIR.accountId)
    }
}
