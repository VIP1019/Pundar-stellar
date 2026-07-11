package com.example.pundarapp.data.stellar

import android.util.Base64
import android.util.Log
import com.example.pundarapp.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.stellar.sdk.AssetTypeNative
import org.stellar.sdk.KeyPair
import org.stellar.sdk.Memo
import org.stellar.sdk.Network
import org.stellar.sdk.Server
import org.stellar.sdk.Transaction
import org.stellar.sdk.TransactionBuilder
import org.stellar.sdk.operations.PaymentOperation
import org.stellar.sdk.responses.TransactionResponse
import java.math.BigDecimal
import java.net.URL
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object StellarWalletManager {
    private const val TAG = "StellarWalletManager"
    private const val HORIZON_URL = "https://horizon-testnet.stellar.org"
    private const val FRIEND_BOT_URL = "https://friendbot.stellar.org/?addr="
    private const val ITERATIONS = 10000
    private const val KEY_LENGTH = 256
    private const val IV_SIZE = 12
    private const val TAG_SIZE = 128

    fun generateKeyPair(): KeyPair {
        return KeyPair.random()
    }

    suspend fun fundTestnetAccount(publicKey: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Funding account: $publicKey")
            val response = URL("$FRIEND_BOT_URL$publicKey").readText()
            Log.d(TAG, "Friendbot response: $response")
            Result.success(true)
        } catch (e: Exception) {
            Log.e(TAG, "Friendbot funding failed", e)
            Result.failure(e)
        }
    }

    fun encryptSecretSeed(seed: CharArray, mpin: String): String {
        val salt = SecureRandom().generateSeed(16)
        val iv = ByteArray(IV_SIZE)
        SecureRandom().nextBytes(iv)

        val secretKey = deriveKey(mpin, salt)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val spec = GCMParameterSpec(TAG_SIZE, iv)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, spec)

        val seedBytes = seed.map { it.code.toByte() }.toByteArray()
        val encrypted = cipher.doFinal(seedBytes)
        
        // Zero out seedBytes immediately
        seedBytes.fill(0)

        val combined = ByteArray(salt.size + iv.size + encrypted.size)
        System.arraycopy(salt, 0, combined, 0, salt.size)
        System.arraycopy(iv, 0, combined, salt.size, iv.size)
        System.arraycopy(encrypted, 0, combined, salt.size + iv.size, encrypted.size)

        return Base64.encodeToString(combined, Base64.NO_WRAP)
    }

    fun decryptSecretSeed(encryptedBase64: String, mpin: String): CharArray {
        val combined = Base64.decode(encryptedBase64, Base64.NO_WRAP)
        val salt = combined.sliceArray(0 until 16)
        val iv = combined.sliceArray(16 until 16 + IV_SIZE)
        val encrypted = combined.sliceArray(16 + IV_SIZE until combined.size)

        val secretKey = deriveKey(mpin, salt)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val spec = GCMParameterSpec(TAG_SIZE, iv)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)

        val decryptedBytes = cipher.doFinal(encrypted)
        val result = CharArray(decryptedBytes.size) { decryptedBytes[it].toInt().toChar() }
        
        // Zero out decryptedBytes
        decryptedBytes.fill(0)

        return result
    }

    private fun deriveKey(mpin: String, salt: ByteArray): SecretKeySpec {
        val pepper = BuildConfig.STELLAR_PEPPER
        val password = (mpin + pepper).toCharArray()
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(password, salt, ITERATIONS, KEY_LENGTH)
        val tmp = factory.generateSecret(spec)
        
        // Zero out password
        password.fill('0')
        
        return SecretKeySpec(tmp.encoded, "AES")
    }

    suspend fun getXlmBalance(publicKey: String): Double = withContext(Dispatchers.IO) {
        try {
            val server = Server(HORIZON_URL)
            val account = server.accounts().account(publicKey)
            val balance = account.balances.find { it.assetType == "native" }?.balance ?: "0.0"
            balance.toDouble()
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching balance", e)
            0.0
        }
    }

    suspend fun sendPayment(
        senderPublicKey: String,
        senderEncryptedSeed: String,
        senderMpin: String,
        recipientPublicKey: String,
        amountXlm: String,
        memo: String
    ): Result<String> = withContext(Dispatchers.IO) {
        var decryptedSeed: CharArray? = null
        try {
            decryptedSeed = decryptSecretSeed(senderEncryptedSeed, senderMpin)
            val sourceKeyPair = KeyPair.fromSecretSeed(decryptedSeed)
            val server = Server(HORIZON_URL)

            val sourceAccount = server.accounts().account(senderPublicKey)
            
            val truncatedMemo = if (memo.length > 28) memo.substring(0, 28) else memo

            val transaction = TransactionBuilder(sourceAccount, Network.TESTNET)
                .addOperation(
                    PaymentOperation.builder()
                        .destination(recipientPublicKey)
                        .asset(AssetTypeNative())
                        .amount(BigDecimal(amountXlm))
                        .build()
                )
                .addMemo(Memo.text(truncatedMemo))
                .setTimeout(180L)
                .setBaseFee(Transaction.MIN_BASE_FEE)
                .build()

            transaction.sign(sourceKeyPair)

            val response: TransactionResponse = server.submitTransaction(transaction)

            if (response.successful == true) {
                Result.success(response.hash)
            } else {
                Result.failure(Exception("Transaction failed. Check account balance and recipient."))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Payment failed", e)
            Result.failure(e)
        } finally {
            decryptedSeed?.let { clearCharArray(it) }
        }
    }
    
    fun clearCharArray(array: CharArray) {
        array.fill('0')
    }
}
