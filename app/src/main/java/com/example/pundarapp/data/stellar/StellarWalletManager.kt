package com.example.pundarapp.data.stellar

import android.util.Base64
import android.util.Log
import com.example.pundarapp.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.stellar.sdk.*
import org.stellar.sdk.operations.*
import org.stellar.sdk.responses.*
import org.stellar.sdk.responses.sorobanrpc.*
import org.stellar.sdk.scval.Scv
import org.stellar.sdk.xdr.SCVal
import org.stellar.sdk.exception.BadRequestException
import java.math.BigDecimal
import java.math.BigInteger
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
    private const val SOROBAN_URL = "https://soroban-testnet.stellar.org"
    private const val FRIEND_BOT_URL = "https://friendbot.stellar.org/?addr="
    private const val ITERATIONS = 10000
    private const val KEY_LENGTH = 256
    private const val IV_SIZE = 12
    private const val TAG_SIZE = 128

    const val USDC_ASSET_CODE = "USDC"
    const val USDC_ISSUER = "GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"

    const val CIRCLE_CONTRACT_ID = "CDH26EOC6AZFP2Z5LAUHH3PGCNIXBMTV3KXWBORA5X6IZ6MH3KDUY3AX"
    const val NATIVE_SAC_ID = "CDLZFC3SYJYDZT7K67VZ75HPJVIEUVNIXF47ZG2FB2RMQQVU2HHGCYSC"

    private val sorobanServer = SorobanServer(SOROBAN_URL)

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

    suspend fun getUsdcBalance(publicKey: String): Double = withContext(Dispatchers.IO) {
        try {
            val server = Server(HORIZON_URL)
            val account = server.accounts().account(publicKey)
            val balance = account.balances.find { 
                it.assetCode == USDC_ASSET_CODE && it.assetIssuer == USDC_ISSUER 
            }?.balance ?: "0.0"
            balance.toDouble()
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching USDC balance", e)
            0.0
        }
    }

    suspend fun hasUsdcTrustline(publicKey: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val server = Server(HORIZON_URL)
            val account = server.accounts().account(publicKey)
            account.balances.any { 
                it.assetCode == USDC_ASSET_CODE && it.assetIssuer == USDC_ISSUER 
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking trustline", e)
            false
        }
    }

    suspend fun establishUsdcTrustline(
        publicKey: String, 
        encryptedSeed: String, 
        mpin: String
    ): Result<String> = withContext(Dispatchers.IO) {
        var decryptedSeed: CharArray? = null
        try {
            decryptedSeed = decryptSecretSeed(encryptedSeed, mpin)
            val keyPair = KeyPair.fromSecretSeed(decryptedSeed)
            val server = Server(HORIZON_URL)
            val account = server.accounts().account(publicKey)

            val usdcAsset = AssetTypeCreditAlphaNum4(USDC_ASSET_CODE, USDC_ISSUER)
            val transaction = TransactionBuilder(account, Network.TESTNET)
                .addOperation(
                    ChangeTrustOperation.builder()
                        .asset(ChangeTrustAsset(usdcAsset))
                        .limit(BigDecimal("922337203685.4775807"))
                        .build()
                )
                .setTimeout(180L)
                .setBaseFee(Transaction.MIN_BASE_FEE)
                .build()

            transaction.sign(keyPair)
            val response = server.submitTransaction(transaction)

            if (response.successful == true) {
                Result.success(response.hash)
            } else {
                Result.failure(Exception("Failed to establish USDC trustline"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error establishing trustline", e)
            Result.failure(e)
        } finally {
            decryptedSeed?.let { clearCharArray(it) }
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

    suspend fun sendPathPayment(
        senderPublicKey: String,
        senderEncryptedSeed: String,
        senderMpin: String,
        recipientPublicKey: String,
        sendAssetCode: String, // "XLM" or "USDC"
        destAssetCode: String, // "XLM" or "USDC"
        sendAmount: String,
        memo: String
    ): Result<String> = withContext(Dispatchers.IO) {
        if (sendAssetCode == destAssetCode) {
            return@withContext sendPayment(
                senderPublicKey, senderEncryptedSeed, senderMpin, 
                recipientPublicKey, sendAmount, memo
            )
        }

        var decryptedSeed: CharArray? = null
        try {
            decryptedSeed = decryptSecretSeed(senderEncryptedSeed, senderMpin)
            val sourceKeyPair = KeyPair.fromSecretSeed(decryptedSeed)
            val server = Server(HORIZON_URL)
            val sourceAccount = server.accounts().account(senderPublicKey)

            val sAsset = if (sendAssetCode == "XLM") AssetTypeNative() 
                         else AssetTypeCreditAlphaNum4(USDC_ASSET_CODE, USDC_ISSUER)
            val dAsset = if (destAssetCode == "XLM") AssetTypeNative() 
                         else AssetTypeCreditAlphaNum4(USDC_ASSET_CODE, USDC_ISSUER)

            val truncatedMemo = if (memo.length > 28) memo.substring(0, 28) else memo

            // We use Strict Send: send exact amount of sendAsset, receive whatever destAsset the path gives (min 0)
            val op = PathPaymentStrictSendOperation.builder()
                .sendAsset(sAsset)
                .sendAmount(BigDecimal(sendAmount))
                .destination(recipientPublicKey)
                .destAsset(dAsset)
                .destMin(BigDecimal("0"))
                .build()

            val transaction = TransactionBuilder(sourceAccount, Network.TESTNET)
                .addOperation(op)
                .addMemo(Memo.text(truncatedMemo))
                .setTimeout(180L)
                .setBaseFee(Transaction.MIN_BASE_FEE)
                .build()

            transaction.sign(sourceKeyPair)
            val response = server.submitTransaction(transaction)

            if (response.successful == true) {
                Result.success(response.hash)
            } else {
                Result.failure(Exception("Path payment failed. Possibly no liquidity path found."))
            }
        } catch (e: BadRequestException) {
            val problem = e.problem
            val detail = problem?.detail ?: "Bad Request"
            val resultCodes = problem?.extras?.resultCodes
            val txCode = resultCodes?.transactionResultCode ?: ""
            val opCodes = resultCodes?.operationsResultCodes?.joinToString(",") ?: ""
            val fullError = "Horizon Error: $detail (TX: $txCode, Ops: $opCodes)"
            Log.e(TAG, "Path payment failed: $fullError")
            Result.failure(Exception(fullError))
        } catch (e: Exception) {
            Log.e(TAG, "Path payment failed", e)
            Result.failure(e)
        } finally {
            decryptedSeed?.let { clearCharArray(it) }
        }
    }

    // --- SOROBAN CIRCLE METHODS ---

    suspend fun createCircle(
        adminPublicKey: String,
        adminEncryptedSeed: String,
        adminMpin: String,
        members: List<String>,
        contributionAmount: Double,
        payoutOrder: List<String>,
        tokenAddress: String = NATIVE_SAC_ID,
        cycleDurationSeconds: Long = 604800 // 7 days
    ): Result<Long> = withContext(Dispatchers.IO) {
        Log.d(TAG, "createCircle started for admin $adminPublicKey")
        var decryptedSeed: CharArray? = null
        try {
            decryptedSeed = decryptSecretSeed(adminEncryptedSeed, adminMpin)
            val adminKeyPair = KeyPair.fromSecretSeed(decryptedSeed)
            
            val amountStroops = (contributionAmount * 10_000_000).toLong().toBigInteger()

            val args = listOf(
                Scv.toVec(members.map { Scv.toAddress(it) }),
                Scv.toInt128(amountStroops),
                Scv.toVec(payoutOrder.map { Scv.toAddress(it) }),
                Scv.toAddress(tokenAddress),
                Scv.toUint64(BigInteger.valueOf(cycleDurationSeconds))
            )

            Log.d(TAG, "Invoking create_circle contract function...")
            val result = invokeContract(adminPublicKey, adminKeyPair, "create_circle", args)
            if (result.isSuccess) {
                val scVal = result.getOrNull()
                val circleId = scVal?.u64?.uint64?.number?.toLong() ?: 0L
                Log.d(TAG, "create_circle success, circleId: $circleId")
                Result.success(circleId)
            } else {
                val error = result.exceptionOrNull()
                Log.e(TAG, "create_circle contract invocation failed: ${error?.message}")
                Result.failure(error ?: Exception("Unknown error in createCircle"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "createCircle failed with exception", e)
            Result.failure(e)
        } finally {
            decryptedSeed?.let { clearCharArray(it) }
        }
    }

    suspend fun contributeToCircle(
        memberPublicKey: String,
        memberEncryptedSeed: String,
        memberMpin: String,
        circleId: Long,
        amount: Double
    ): Result<String> = withContext(Dispatchers.IO) {
        var decryptedSeed: CharArray? = null
        try {
            decryptedSeed = decryptSecretSeed(memberEncryptedSeed, memberMpin)
            val memberKeyPair = KeyPair.fromSecretSeed(decryptedSeed)
            
            val amountStroops = (amount * 10_000_000).toLong().toBigInteger()

            val args = listOf(
                Scv.toUint64(BigInteger.valueOf(circleId)),
                Scv.toAddress(memberPublicKey),
                Scv.toInt128(amountStroops)
            )

            val result = invokeContract(memberPublicKey, memberKeyPair, "contribute", args)
            if (result.isSuccess) {
                Result.success("Contribution successful")
            } else {
                Result.failure(result.exceptionOrNull()!!)
            }
        } catch (e: Exception) {
            Log.e(TAG, "contribute failed", e)
            Result.failure(e)
        } finally {
            decryptedSeed?.let { clearCharArray(it) }
        }
    }

    suspend fun checkAndPayout(
        callerPublicKey: String,
        callerEncryptedSeed: String,
        callerMpin: String,
        circleId: Long
    ): Result<String> = withContext(Dispatchers.IO) {
        var decryptedSeed: CharArray? = null
        try {
            decryptedSeed = decryptSecretSeed(callerEncryptedSeed, callerMpin)
            val callerKeyPair = KeyPair.fromSecretSeed(decryptedSeed)

            val args = listOf(Scv.toUint64(BigInteger.valueOf(circleId)))
            val result = invokeContract(callerPublicKey, callerKeyPair, "check_and_payout", args)
            if (result.isSuccess) {
                Result.success("Payout successful")
            } else {
                Result.failure(result.exceptionOrNull()!!)
            }
        } catch (e: Exception) {
            Log.e(TAG, "checkAndPayout failed", e)
            Result.failure(e)
        } finally {
            decryptedSeed?.let { clearCharArray(it) }
        }
    }

    suspend fun checkAndDissolve(
        callerPublicKey: String,
        callerEncryptedSeed: String,
        callerMpin: String,
        circleId: Long
    ): Result<String> = withContext(Dispatchers.IO) {
        var decryptedSeed: CharArray? = null
        try {
            decryptedSeed = decryptSecretSeed(callerEncryptedSeed, callerMpin)
            val callerKeyPair = KeyPair.fromSecretSeed(decryptedSeed)

            val args = listOf(Scv.toUint64(BigInteger.valueOf(circleId)))
            val result = invokeContract(callerPublicKey, callerKeyPair, "check_and_dissolve", args)
            if (result.isSuccess) {
                Result.success("Circle dissolved and members refunded")
            } else {
                Result.failure(result.exceptionOrNull()!!)
            }
        } catch (e: Exception) {
            Log.e(TAG, "checkAndDissolve failed", e)
            Result.failure(e)
        } finally {
            decryptedSeed?.let { clearCharArray(it) }
        }
    }

    suspend fun getCircleState(circleId: Long): Result<CircleStateData> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "getCircleState for circle $circleId")
            val server = Server(HORIZON_URL)
            // Use a funded public address for simulation
            val sourceAccount = server.accounts().account("GCID46I7QIVBZJKEOMKD5LPBIO6UO5RPAJCUN52L546WNS3HAJD434XQ")

            val invokeOp = InvokeHostFunctionOperation.invokeContractFunctionOperationBuilder(
                CIRCLE_CONTRACT_ID,
                "get_circle_state",
                listOf(Scv.toUint64(BigInteger.valueOf(circleId)))
            ).build()

            val transaction = TransactionBuilder(sourceAccount, Network.TESTNET)
                    .addOperation(invokeOp)
                    .setTimeout(180L)
                    .setBaseFee(Transaction.MIN_BASE_FEE)
                    .build()
            
            Log.d(TAG, "Simulating get_circle_state...")
            val simulateResponse = withTimeout(20000) {
                sorobanServer.simulateTransaction(transaction)
            }
            
            if (simulateResponse.error != null) {
                Log.e(TAG, "Simulation error in getCircleState: ${simulateResponse.error}")
                return@withContext Result.failure(Exception("Simulation error: ${simulateResponse.error}"))
            }

            val scVal = simulateResponse.results[0].parseXdr()
            val entries = scVal.map.scMap
            
            var currentCycle = 0
            var nextPayoutIndex = 0
            var pooledAmount = "0"
            var cycleDeadline = 0L
            var dissolved = false
            var contributionsMap = emptyMap<String, Boolean>()

            for (i in entries.indices) {
                val entry = entries[i]
                val key = Scv.fromSymbol(entry.key)
                when (key) {
                    "current_cycle" -> currentCycle = Scv.fromUint32(entry.`val`).toInt()
                    "next_payout_index" -> nextPayoutIndex = Scv.fromUint32(entry.`val`).toInt()
                    "pooled_amount" -> pooledAmount = Scv.fromInt128(entry.`val`).toString()
                    "cycle_deadline" -> cycleDeadline = Scv.fromUint64(entry.`val`).toLong()
                    "dissolved" -> dissolved = Scv.fromBoolean(entry.`val`)
                    "contributions" -> {
                        val cMapEntries = entry.`val`.map.scMap
                        val tempMap = mutableMapOf<String, Boolean>()
                        for (j in cMapEntries.indices) {
                            val cEntry = cMapEntries[j]
                            tempMap[Scv.fromAddress(cEntry.key).toString()] = Scv.fromBoolean(cEntry.`val`)
                        }
                        contributionsMap = tempMap
                    }
                }
            }

            Result.success(CircleStateData(
                currentCycle = currentCycle,
                nextPayoutIndex = nextPayoutIndex,
                pooledAmount = pooledAmount.toBigDecimal().movePointLeft(7).toDouble(),
                cycleDeadline = cycleDeadline,
                dissolved = dissolved,
                contributions = contributionsMap
            ))
        } catch (e: Exception) {
            Log.e(TAG, "getCircleState failed", e)
            Result.failure(e)
        }
    }

    private suspend fun invokeContract(
        sourcePublicKey: String,
        sourceKeyPair: KeyPair,
        functionName: String,
        args: List<SCVal>
    ): Result<SCVal?> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "invokeContract: $functionName")
            val server = Server(HORIZON_URL)
            val sourceAccount = server.accounts().account(sourcePublicKey)

            val invokeOp = InvokeHostFunctionOperation.invokeContractFunctionOperationBuilder(
                CIRCLE_CONTRACT_ID,
                functionName,
                args
            ).build()

            // Preflight (Simulation) with 30s timeout
            Log.d(TAG, "Simulating transaction...")
            val simulateResponse = withTimeout(30000) {
                val transactionPreflight = TransactionBuilder(sourceAccount, Network.TESTNET)
                        .addOperation(invokeOp)
                        .setTimeout(180L)
                        .setBaseFee(Transaction.MIN_BASE_FEE)
                        .build()
                sorobanServer.simulateTransaction(transactionPreflight)
            }
            
            if (simulateResponse.error != null) {
                Log.e(TAG, "Simulation error: ${simulateResponse.error}")
                return@withContext Result.failure(Exception("Simulation error: ${simulateResponse.error}"))
            }

            val transaction = TransactionBuilder(sourceAccount, Network.TESTNET)
                .addOperation(invokeOp)
                .setTimeout(180L)
                .setBaseFee(Transaction.MIN_BASE_FEE + (simulateResponse.minResourceFee ?: 0L))
                .setSorobanData(simulateResponse.transactionData)
                .build()

            transaction.sign(sourceKeyPair)
            
            Log.d(TAG, "Sending transaction...")
            val sendResponse = withTimeout(30000) {
                sorobanServer.sendTransaction(transaction)
            }
            
            if (sendResponse.status == SendTransactionResponse.SendTransactionStatus.ERROR) {
                Log.e(TAG, "Send error: ${sendResponse.errorResultXdr}")
                return@withContext Result.failure(Exception("Send error: ${sendResponse.errorResultXdr}"))
            }

            Log.d(TAG, "Transaction sent. Hash: ${sendResponse.hash}. Polling for result...")
            // Poll for result with 60s total timeout
            val resultScVal = withTimeout(60000) {
                var getResponse = sorobanServer.getTransaction(sendResponse.hash)
                var attempts = 0
                while (getResponse.status == GetTransactionResponse.GetTransactionStatus.NOT_FOUND) {
                    if (attempts >= 30) break // 60 seconds max
                    Log.d(TAG, "Transaction not found yet, polling... (attempt $attempts)")
                    delay(2000)
                    getResponse = sorobanServer.getTransaction(sendResponse.hash)
                    attempts++
                }

                if (getResponse.status == GetTransactionResponse.GetTransactionStatus.SUCCESS) {
                    Log.d(TAG, "Transaction successful!")
                    val meta = getResponse.parseResultMetaXdr()
                    meta.v3.sorobanMeta.returnValue
                } else {
                    Log.e(TAG, "Transaction failed with status: ${getResponse.status}")
                    throw Exception("Transaction failed with status: ${getResponse.status}")
                }
            }
            Result.success(resultScVal)
        } catch (e: Exception) {
            Log.e(TAG, "invokeContract failed with exception", e)
            Result.failure(e)
        }
    }

    fun clearCharArray(array: CharArray) {
        array.fill('0')
    }
}

data class CircleStateData(
    val currentCycle: Int,
    val nextPayoutIndex: Int,
    val pooledAmount: Double,
    val cycleDeadline: Long,
    val dissolved: Boolean,
    val contributions: Map<String, Boolean>
)
