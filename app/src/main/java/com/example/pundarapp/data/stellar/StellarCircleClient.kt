package com.example.pundarapp.data.stellar

// Circle (Soroban) operations — still on Lightsail SDK until Circle phase.
// All functions moved unchanged from StellarWalletManager.

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.stellar.sdk.*
import org.stellar.sdk.operations.*
import org.stellar.sdk.responses.sorobanrpc.*
import org.stellar.sdk.scval.Scv
import org.stellar.sdk.xdr.SCVal
import java.math.BigInteger

object StellarCircleClient {
    private const val TAG = "StellarCircleClient"
    private const val HORIZON_URL = "https://horizon-testnet.stellar.org"
    private const val SOROBAN_URL = "https://soroban-testnet.stellar.org:443"

    const val CIRCLE_CONTRACT_ID = "CBGZJWLQHJSRWFS2HMBD5SRWY5BAIB76PS7S5MGFJXNN34NF4QDSE3Y3"

    private val sorobanServer = SorobanServer(SOROBAN_URL)

    suspend fun createCircle(
        adminPublicKey: String,
        adminEncryptedSeed: String,
        adminMpin: String,
        members: List<String>,
        contributionAmount: Double,
        payoutOrder: List<String>,
        tokenAddress: String = StellarWalletManager.NATIVE_SAC_ID,
        cycleDurationSeconds: Long = 604800
    ): Result<Long> = withContext(Dispatchers.IO) {
        Log.d(TAG, "createCircle started for admin $adminPublicKey")
        var decryptedSeed: CharArray? = null
        try {
            decryptedSeed = StellarWalletManager.decryptSecretSeed(adminEncryptedSeed, adminMpin)
            val adminKeyPair = KeyPair.fromSecretSeed(decryptedSeed)
            val amountStroops = (contributionAmount * 10_000_000).toLong().toBigInteger()
            val args = listOf(
                Scv.toVec(members.map { Scv.toAddress(it) }),
                Scv.toInt128(amountStroops),
                Scv.toVec(payoutOrder.map { Scv.toAddress(it) }),
                Scv.toAddress(tokenAddress),
                Scv.toUint64(BigInteger.valueOf(cycleDurationSeconds))
            )
            try {
                val server = Server(HORIZON_URL)
                server.accounts().account(adminPublicKey)
            } catch (e: Exception) {
                // TODO(Circle): fund via Friendbot if needed
            }
            val result = invokeContract(adminPublicKey, adminKeyPair, "create_circle", args)
            if (result.isSuccess) {
                val circleId = result.getOrNull()?.u64?.uint64?.number?.toLong() ?: 0L
                Result.success(circleId)
            } else {
                Result.failure(result.exceptionOrNull() ?: Exception("createCircle unknown error"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "createCircle failed", e)
            Result.failure(e)
        } finally {
            decryptedSeed?.let { StellarWalletManager.clearCharArray(it) }
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
            decryptedSeed = StellarWalletManager.decryptSecretSeed(memberEncryptedSeed, memberMpin)
            val memberKeyPair = KeyPair.fromSecretSeed(decryptedSeed)
            val amountStroops = (amount * 10_000_000).toLong().toBigInteger()
            val args = listOf(
                Scv.toUint64(BigInteger.valueOf(circleId)),
                Scv.toAddress(memberPublicKey),
                Scv.toInt128(amountStroops)
            )
            val result = invokeContract(memberPublicKey, memberKeyPair, "contribute", args)
            if (result.isSuccess) Result.success("Contribution successful")
            else Result.failure(result.exceptionOrNull()!!)
        } catch (e: Exception) {
            Log.e(TAG, "contribute failed", e)
            Result.failure(e)
        } finally {
            decryptedSeed?.let { StellarWalletManager.clearCharArray(it) }
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
            decryptedSeed = StellarWalletManager.decryptSecretSeed(callerEncryptedSeed, callerMpin)
            val callerKeyPair = KeyPair.fromSecretSeed(decryptedSeed)
            val args = listOf(Scv.toUint64(BigInteger.valueOf(circleId)))
            val result = invokeContract(callerPublicKey, callerKeyPair, "check_and_payout", args)
            if (result.isSuccess) Result.success("Payout successful")
            else Result.failure(result.exceptionOrNull()!!)
        } catch (e: Exception) {
            Log.e(TAG, "checkAndPayout failed", e)
            Result.failure(e)
        } finally {
            decryptedSeed?.let { StellarWalletManager.clearCharArray(it) }
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
            decryptedSeed = StellarWalletManager.decryptSecretSeed(callerEncryptedSeed, callerMpin)
            val callerKeyPair = KeyPair.fromSecretSeed(decryptedSeed)
            val args = listOf(Scv.toUint64(BigInteger.valueOf(circleId)))
            val result = invokeContract(callerPublicKey, callerKeyPair, "check_and_dissolve", args)
            if (result.isSuccess) Result.success("Circle dissolved and members refunded")
            else Result.failure(result.exceptionOrNull()!!)
        } catch (e: Exception) {
            Log.e(TAG, "checkAndDissolve failed", e)
            Result.failure(e)
        } finally {
            decryptedSeed?.let { StellarWalletManager.clearCharArray(it) }
        }
    }

    suspend fun getCircleState(circleId: Long): Result<CircleStateData> = withContext(Dispatchers.IO) {
        try {
            val server = Server(HORIZON_URL)
            val sourceAccount = server.accounts().account("GCID46I7QIVBZJKEOMKD5LPBIO6UO5RPAJCUN52L546WNS3HAJD434XQ")
            val invokeOp = InvokeHostFunctionOperation.invokeContractFunctionOperationBuilder(
                CIRCLE_CONTRACT_ID, "get_circle_state",
                listOf(Scv.toUint64(BigInteger.valueOf(circleId)))
            ).build()
            val transaction = TransactionBuilder(sourceAccount, Network.TESTNET)
                .addOperation(invokeOp).setTimeout(180L).setBaseFee(Transaction.MIN_BASE_FEE).build()
            val simulateResponse = withTimeout(20000) { sorobanServer.simulateTransaction(transaction) }
            if (simulateResponse.error != null) {
                return@withContext Result.failure(Exception("Simulation error: ${simulateResponse.error}"))
            }
            val scVal = simulateResponse.results[0].parseXdr()
            val entries = scVal.map.scMap
            var currentCycle = 0; var nextPayoutIndex = 0; var pooledAmount = "0"
            var cycleDeadline = 0L; var dissolved = false
            var contributionsMap = emptyMap<String, Boolean>()
            for (i in entries.indices) {
                val entry = entries[i]
                when (Scv.fromSymbol(entry.key)) {
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
                currentCycle, nextPayoutIndex,
                pooledAmount.toBigDecimal().movePointLeft(7).toDouble(),
                cycleDeadline, dissolved, contributionsMap
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
            val server = Server(HORIZON_URL)
            val sourceAccount = server.accounts().account(sourcePublicKey)
            val invokeOp = InvokeHostFunctionOperation.invokeContractFunctionOperationBuilder(
                CIRCLE_CONTRACT_ID, functionName, args
            ).build()
            val simulateResponse = withTimeout(30000) {
                val preflight = TransactionBuilder(sourceAccount, Network.TESTNET)
                    .addOperation(invokeOp).setTimeout(180L).setBaseFee(Transaction.MIN_BASE_FEE).build()
                sorobanServer.simulateTransaction(preflight)
            }
            if (simulateResponse.error != null) {
                return@withContext Result.failure(Exception("Simulation error: ${simulateResponse.error}"))
            }
            val transaction = TransactionBuilder(sourceAccount, Network.TESTNET)
                .addOperation(invokeOp).setTimeout(180L)
                .setBaseFee(Transaction.MIN_BASE_FEE + (simulateResponse.minResourceFee ?: 0L))
                .setSorobanData(simulateResponse.transactionData).build()
            transaction.sign(sourceKeyPair)
            val sendResponse = withTimeout(30000) { sorobanServer.sendTransaction(transaction) }
            if (sendResponse.status == SendTransactionResponse.SendTransactionStatus.ERROR) {
                return@withContext Result.failure(Exception("Send error: ${sendResponse.errorResultXdr}"))
            }
            val resultScVal = withTimeout(60000) {
                var getResponse = sorobanServer.getTransaction(sendResponse.hash)
                var attempts = 0
                while (getResponse.status == GetTransactionResponse.GetTransactionStatus.NOT_FOUND) {
                    if (attempts >= 30) break
                    delay(2000)
                    getResponse = sorobanServer.getTransaction(sendResponse.hash)
                    attempts++
                }
                if (getResponse.status == GetTransactionResponse.GetTransactionStatus.SUCCESS) {
                    getResponse.parseResultMetaXdr().v3.sorobanMeta.returnValue
                } else {
                    throw Exception("Transaction failed: ${getResponse.status}")
                }
            }
            Result.success(resultScVal)
        } catch (e: Exception) {
            Log.e(TAG, "invokeContract failed", e)
            Result.failure(e)
        }
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

