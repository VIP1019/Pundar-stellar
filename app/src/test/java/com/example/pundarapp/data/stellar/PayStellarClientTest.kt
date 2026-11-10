package com.example.pundarapp.data.stellar

import com.soneso.stellar.sdk.horizon.exceptions.BadRequestException
import com.soneso.stellar.sdk.horizon.exceptions.BadResponseException
import com.soneso.stellar.sdk.horizon.exceptions.ConnectionErrorException
import com.soneso.stellar.sdk.horizon.exceptions.NetworkException
import com.soneso.stellar.sdk.horizon.exceptions.RequestTimeoutException
import com.soneso.stellar.sdk.horizon.exceptions.TooManyRequestsException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PayStellarClientTest {

    @Test
    fun isMissingAccountError_onlyMatchesBadRequest404() {
        // Horizon 404 is thrown as BadRequestException(code = 404, body = ...) by RequestBuilder.executeGetRequest
        assertTrue(PayStellarClient.isMissingAccountError(BadRequestException(code = 404, body = """{"status":404}""")))

        // Other 4xx BadRequestExceptions must NOT trigger Friendbot
        assertFalse(PayStellarClient.isMissingAccountError(BadRequestException(code = 400, body = """{"status":400}""")))
        assertFalse(PayStellarClient.isMissingAccountError(BadRequestException(code = null, body = null)))
        assertFalse(PayStellarClient.isMissingAccountError(TooManyRequestsException(code = 429, body = "")))

        // Timeouts, offline/connection errors, 5xx server errors, and cancellations must NOT trigger Friendbot
        assertFalse(PayStellarClient.isMissingAccountError(RequestTimeoutException(code = 504, body = "")))
        assertFalse(PayStellarClient.isMissingAccountError(ConnectionErrorException( RuntimeException("Offline"))))
        assertFalse(PayStellarClient.isMissingAccountError(BadResponseException(code = 500, body = "")))
        assertFalse(PayStellarClient.isMissingAccountError(BadResponseException(code = 503, body = "")))
        assertFalse(PayStellarClient.isMissingAccountError(NetworkException(code = 502, body = "")))
        assertFalse(PayStellarClient.isMissingAccountError(CancellationException("cancelled")))
        assertFalse(PayStellarClient.isMissingAccountError(null))
    }

    @Test
    fun recoverMissingAccountBalance_triggersFriendbotOnlyOn404() = runBlocking {
        var fundCalls = 0
        var balanceFetches = 0

        val recovered = PayStellarClient.recoverMissingAccountBalance(
            publicKey = "GTEST404",
            balanceResult = Result.failure(BadRequestException(code = 404, body = "not_found")),
            fundAccount = {
                fundCalls++
                Result.success(true)
            },
            fetchBalance = {
                balanceFetches++
                Result.success(10_000L * Stroops.PER_XLM)
            }
        )

        assertEquals(1, fundCalls)
        assertEquals(1, balanceFetches)
        assertEquals(10_000L * Stroops.PER_XLM, recovered.getOrThrow())
    }

    @Test
    fun recoverMissingAccountBalance_doesNotFundOnTimeoutOfflineOrServerError() = runBlocking {
        val nonMissingErrors = listOf(
            RequestTimeoutException(code = 504, body = "timeout"),
            ConnectionErrorException(RuntimeException("Unable to resolve host")),
            BadResponseException(code = 502, body = "Bad Gateway"),
            BadRequestException(code = 400, body = "bad_request"),
            TooManyRequestsException(code = 429, body = "rate_limit")
        )

        for (err in nonMissingErrors) {
            var fundCalls = 0
            val result = PayStellarClient.recoverMissingAccountBalance(
                publicKey = "GTESTERR",
                balanceResult = Result.failure(err),
                fundAccount = {
                    fundCalls++
                    Result.success(true)
                },
                fetchBalance = { Result.success(100L) }
            )
            assertEquals("Should not fund on ${err::class.simpleName}", 0, fundCalls)
            assertTrue(result.isFailure)
            assertEquals(err, result.exceptionOrNull())
        }
    }

    @Test(expected = CancellationException::class)
    fun recoverMissingAccountBalance_rethrowsCancellation() = runBlocking {
        PayStellarClient.recoverMissingAccountBalance(
            publicKey = "GTESTCANCEL",
            balanceResult = Result.failure(CancellationException("Job was cancelled")),
            fundAccount = { Result.success(true) },
            fetchBalance = { Result.success(0L) }
        )
        Unit
    }
}
