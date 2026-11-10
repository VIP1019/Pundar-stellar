package com.example.pundarapp.data.stellar

import android.util.Log
import com.soneso.stellar.sdk.AbstractTransaction
import com.soneso.stellar.sdk.AssetTypeNative
import com.soneso.stellar.sdk.FriendBot
import com.soneso.stellar.sdk.KeyPair
import com.soneso.stellar.sdk.MemoText
import com.soneso.stellar.sdk.Network
import com.soneso.stellar.sdk.PaymentOperation
import com.soneso.stellar.sdk.TransactionBuilder
import com.soneso.stellar.sdk.horizon.HorizonServer
import com.soneso.stellar.sdk.horizon.exceptions.BadRequestException
import com.soneso.stellar.sdk.horizon.exceptions.ConnectionErrorException
import com.soneso.stellar.sdk.horizon.exceptions.NetworkException
import com.soneso.stellar.sdk.horizon.exceptions.RequestTimeoutException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/** Outcome of a payment attempt. */
sealed class PaymentResult {
    data class Success(val hash: String) : PaymentResult()
    data class Failure(val code: String, val message: String) : PaymentResult()
    /** Submitted but outcome unknown (timeout/connection). Resolve by hash on Horizon. */
    data class Unknown(val hash: String?) : PaymentResult()
}

/**
 * Pay-side Stellar Testnet client on the Soneso KMP SDK (`com.soneso.stellar.sdk.*`).
 */
object PayStellarClient {
    private const val TAG = "PayStellarClient"
    const val HORIZON_URL = "https://horizon-testnet.stellar.org"
    private const val FRIENDBOT_ATTEMPTS = 3

    private val horizon by lazy { HorizonServer(HORIZON_URL) }

    suspend fun generateKeyPair(): KeyPair = KeyPair.random()

    /**
     * Returns true ONLY when Horizon confirms the account does not exist on-chain (HTTP 404).
     * In Soneso SDK (`RequestBuilder.executeGetRequest`), HTTP 404 throws `BadRequestException(code = 404, body = ...)`
     * which is a subclass of `NetworkException(code = 404)`.
     * Timeouts (`RequestTimeoutException`), connectivity/cancellation (`ConnectionErrorException` / `CancellationException`),
     * and server errors (`BadResponseException` 5xx) all return false.
     */
    fun isMissingAccountError(t: Throwable?): Boolean =
        t is BadRequestException && t.code == 404

    /** Funds via FriendBot with retries. Succeeds if the account ends up existing on-chain. */
    suspend fun fundTestnetAccount(publicKey: String): Result<Boolean> = withContext(Dispatchers.IO) {
        var lastError: Throwable? = null
        repeat(FRIENDBOT_ATTEMPTS) { attempt ->
            try {
                if (FriendBot.fundTestnetAccount(publicKey)) return@withContext Result.success(true)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                lastError = e
            }
            // Friendbot rejects already-funded accounts; treat an existing account as success.
            try {
                horizon.loadAccount(publicKey)
                return@withContext Result.success(true)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
            }
            if (attempt < FRIENDBOT_ATTEMPTS - 1) delay(2000L * (attempt + 1))
        }
        Log.e(TAG, "Friendbot funding failed", lastError)
        Result.failure(lastError ?: Exception("Friendbot funding failed"))
    }

    /** Native XLM balance in stroops. Failure if the account is missing or Horizon is unreachable. */
    suspend fun getXlmBalance(publicKey: String): Result<Long> = withContext(Dispatchers.IO) {
        try {
            val account = horizon.accounts().account(publicKey)
            val native = account.balances.firstOrNull { it.assetType == "native" }?.balance
                ?: return@withContext Result.success(0L)
            Result.success(Stroops.fromXlmString(native) ?: 0L)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching balance", e)
            Result.failure(e)
        }
    }

    /**
     * Recovers an account via Friendbot ONLY when [balanceResult] failed with a confirmed
     * missing-account response from Horizon (`BadRequestException` with HTTP status `404`).
     * Timeouts, offline errors, 5xx responses, and cancellations never trigger Friendbot.
     */
    suspend fun recoverMissingAccountBalance(
        publicKey: String,
        balanceResult: Result<Long>,
        fundAccount: suspend (String) -> Result<Boolean> = ::fundTestnetAccount,
        fetchBalance: suspend (String) -> Result<Long> = ::getXlmBalance
    ): Result<Long> {
        if (balanceResult.isSuccess) return balanceResult
        val error = balanceResult.exceptionOrNull()
        if (error is CancellationException) throw error
        if (!isMissingAccountError(error)) {
            return balanceResult
        }
        runCatching { Log.d(TAG, "Account $publicKey confirmed missing (HTTP 404). Attempting Friendbot recovery...") }
        val funded = fundAccount(publicKey)
        return if (funded.isSuccess) {
            fetchBalance(publicKey)
        } else {
            Result.failure(funded.exceptionOrNull() ?: error!!)
        }
    }

    /**
     * Builds, signs and submits one native XLM payment.
     * [secret] is wiped before returning. [memo] is optional and must be <= 28 UTF-8 bytes.
     */
    suspend fun sendPayment(
        secret: CharArray,
        destination: String,
        amountStroops: Long,
        memo: String
    ): PaymentResult = withContext(Dispatchers.IO) {
        if (amountStroops <= 0) {
            clearCharArray(secret)
            return@withContext PaymentResult.Failure("invalid_amount", "Amount must be greater than zero.")
        }
        if (memo.encodeToByteArray().size > 28) {
            clearCharArray(secret)
            return@withContext PaymentResult.Failure("memo_too_long", "Memo is longer than 28 bytes.")
        }

        // Stage 1: nothing has been sent yet, so any failure here is a plain Failure.
        val envelope: String
        val hash: String
        try {
            val keyPair = KeyPair.fromSecretSeed(secret)
            val source = horizon.loadAccount(keyPair.getAccountId())
            val builder = TransactionBuilder(source, Network.TESTNET)
                .setBaseFee(AbstractTransaction.MIN_BASE_FEE)
                .addOperation(
                    PaymentOperation(
                        destination = destination,
                        asset = AssetTypeNative,
                        amount = Stroops.toXlmString(amountStroops)
                    )
                )
                .setTimeout(180L)
            if (memo.isNotEmpty()) builder.addMemo(MemoText(memo))
            val tx = builder.build()
            tx.sign(keyPair)
            envelope = tx.toEnvelopeXdrBase64()
            hash = tx.hashHex()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Payment build failed", e)
            return@withContext mapError(e, hash = null, submitted = false)
        } finally {
            clearCharArray(secret)
        }

        // Stage 2: submit. Timeouts here leave the outcome unknown.
        try {
            val response = horizon.submitTransaction(envelope)
            if (response.successful) PaymentResult.Success(response.hash)
            else PaymentResult.Failure("tx_failed", "The transaction was rejected by the network.")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Payment submit failed", e)
            mapError(e, hash = hash, submitted = true)
        }
    }

    fun clearCharArray(array: CharArray) {
        array.fill('\u0000')
    }

    internal fun mapError(e: Exception, hash: String?, submitted: Boolean): PaymentResult {
        if (e is RequestTimeoutException || e is ConnectionErrorException) {
            return if (submitted) PaymentResult.Unknown(hash)
            else PaymentResult.Failure("network_error", "Network problem. Please try again.")
        }
        if (e is BadRequestException) {
            if (e.code == 404) {
                return PaymentResult.Failure("account_not_found", "Account not found on Stellar Testnet.")
            }
            val (txCode, opCode) = parseResultCodes(e.body)
            val code = opCode ?: txCode ?: "bad_request"
            return PaymentResult.Failure(code, messageFor(code))
        }
        if (e is NetworkException && e.code == 404) {
            return PaymentResult.Failure("account_not_found", "Account not found on Stellar Testnet.")
        }
        return PaymentResult.Failure("error", e.message ?: "Payment failed.")
    }

    private fun parseResultCodes(body: String?): Pair<String?, String?> {
        if (body.isNullOrBlank()) return null to null
        return try {
            val codes = Json.parseToJsonElement(body).jsonObject["extras"]
                ?.jsonObject?.get("result_codes")?.jsonObject
            val tx = codes?.get("transaction")?.jsonPrimitive?.contentOrNull
            val op = codes?.opCode()
            tx to op
        } catch (e: Exception) {
            null to null
        }
    }

    // First non-success operation code, if any.
    private fun JsonObject.opCode(): String? =
        this["operations"]?.jsonArray
            ?.mapNotNull { it.jsonPrimitive.contentOrNull }
            ?.firstOrNull { it != "op_success" }

    private fun messageFor(code: String): String = when (code) {
        "tx_bad_seq" -> "Your account changed while sending. Please try again."
        "op_underfunded", "tx_insufficient_balance" -> "Insufficient XLM balance."
        "op_no_destination" -> "The recipient account does not exist on Stellar Testnet."
        "op_low_reserve" -> "Not enough XLM left to keep the minimum account reserve."
        "tx_insufficient_fee" -> "Network fee too low. Please try again."
        "tx_too_late" -> "The transaction expired. Please try again."
        else -> "Payment failed ($code)."
    }
}
