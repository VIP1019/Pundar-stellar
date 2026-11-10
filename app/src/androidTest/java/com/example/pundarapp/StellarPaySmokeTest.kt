package com.example.pundarapp

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.pundarapp.data.stellar.PayStellarClient
import com.example.pundarapp.data.stellar.PaymentResult
import com.example.pundarapp.data.stellar.Stroops
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

// Live Testnet smoke test: needs network and a device/emulator.
@RunWith(AndroidJUnit4::class)
class StellarPaySmokeTest {
    @Test fun fundTwoAccountsAndSendOneXlm() = runBlocking {
        val a = PayStellarClient.generateKeyPair()
        val b = PayStellarClient.generateKeyPair()
        assertTrue(PayStellarClient.fundTestnetAccount(a.getAccountId()).isSuccess)
        assertTrue(PayStellarClient.fundTestnetAccount(b.getAccountId()).isSuccess)

        val before = PayStellarClient.getXlmBalance(b.getAccountId()).getOrThrow()
        val result = PayStellarClient.sendPayment(
            secret = a.getSecretSeed()!!,
            destination = b.getAccountId(),
            amountStroops = Stroops.PER_XLM,
            memo = "PAY:TEST0001"
        )
        assertTrue("Expected Success but was $result", result is PaymentResult.Success)
        println("Payment hash: ${(result as PaymentResult.Success).hash}")

        val after = PayStellarClient.getXlmBalance(b.getAccountId()).getOrThrow()
        assertTrue(after - before == Stroops.PER_XLM)
    }
}
