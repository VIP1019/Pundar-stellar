package com.example.pundarapp

import com.example.pundarapp.data.qr.QrPayload
import com.example.pundarapp.data.qr.isExpired
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class QrPayloadTest {

    @Test
    fun roundTripReceiveMoneyPayload() {
        val original = QrPayload(
            type = QrPayload.TYPE_RECEIVE_MONEY,
            userId = "09171234567",
            username = "09171234567",
            displayName = "Yasmine Abarca",
            walletId = "GCID46I7QIVBZJKEOMKD5LPBIO6UO5RPAJCUN52L546WNS3HAJD434XQ",
            amount = 150.50,
            timestamp = 1700000000000L,
            securityToken = "09171234567"
        )
        val encoded = QrPayload.encode(original)
        val decoded = QrPayload.decode(encoded)

        assertNotNull(decoded)
        assertEquals(QrPayload.TYPE_RECEIVE_MONEY, decoded!!.type)
        assertEquals("09171234567", decoded.userId)
        assertEquals("Yasmine Abarca", decoded.displayName)
        assertEquals(150.50, decoded.amount!!, 0.001)
        assertFalse(decoded.isExpired())
    }

    @Test
    fun decodesSep0007StellarUri() {
        val pk = "GCID46I7QIVBZJKEOMKD5LPBIO6UO5RPAJCUN52L546WNS3HAJD434XQ"
        val uri = "web+stellar:pay?destination=$pk&amount=42.25&memo=Lunch%20Split"
        val decoded = QrPayload.decode(uri)

        assertNotNull(decoded)
        assertEquals(QrPayload.TYPE_STELLAR_ADDRESS, decoded!!.type)
        assertEquals(pk, decoded.userId)
        assertEquals(pk, decoded.walletId)
        assertEquals(42.25, decoded.amount!!, 0.001)
        assertEquals("Lunch Split", decoded.billRef)
        assertFalse(decoded.isExpired())
    }

    @Test
    fun decodesRawStellarPublicKey() {
        val pk = "GCID46I7QIVBZJKEOMKD5LPBIO6UO5RPAJCUN52L546WNS3HAJD434XQ"
        val decoded = QrPayload.decode("  $pk  ")

        assertNotNull(decoded)
        assertEquals(QrPayload.TYPE_STELLAR_ADDRESS, decoded!!.type)
        assertEquals(pk, decoded.userId)
        assertEquals(pk, decoded.walletId)
        assertNull(decoded.amount)
        assertFalse(decoded.isExpired())
    }

    @Test
    fun decodesPundarDeepLinks() {
        val payLink = "pundar://pay?to=09171234567&amount=250.0&name=Yasmine"
        val decodedPay = QrPayload.decode(payLink)
        assertNotNull(decodedPay)
        assertEquals(QrPayload.TYPE_RECEIVE_MONEY, decodedPay!!.type)
        assertEquals("09171234567", decodedPay.userId)
        assertEquals(250.0, decodedPay.amount!!, 0.001)

        val joinLink = "https://pundar.app/bill/join?billId=BILL-555&title=Team%20Lunch&amount=120.0"
        val decodedJoin = QrPayload.decode(joinLink)
        assertNotNull(decodedJoin)
        assertEquals(QrPayload.TYPE_GROUP_BILL_JOIN, decodedJoin!!.type)
        assertEquals("BILL-555", decodedJoin.userId)
        assertEquals("Team Lunch", decodedJoin.billRef)
        assertEquals(120.0, decodedJoin.amount!!, 0.001)
    }

    @Test
    fun decodesGroupBillRefs() {
        val decoded = QrPayload.decode("BILL-ABC987")
        assertNotNull(decoded)
        assertEquals(QrPayload.TYPE_GROUP_BILL_JOIN, decoded!!.type)
        assertEquals("BILL-ABC987", decoded.userId)
    }

    @Test
    fun decodesPhoneNumbers() {
        val decoded = QrPayload.decode("09171234567")
        assertNotNull(decoded)
        assertEquals(QrPayload.TYPE_RECEIVE_MONEY, decoded!!.type)
        assertEquals("09171234567", decoded.userId)

        val decodedIntl = QrPayload.decode("+639171234567")
        assertNotNull(decodedIntl)
        assertEquals(QrPayload.TYPE_RECEIVE_MONEY, decodedIntl!!.type)
        assertEquals("09171234567", decodedIntl.userId)
    }

    @Test
    fun decodesFederatedAddress() {
        val decoded = QrPayload.decode("yasmine*stellar.org")
        assertNotNull(decoded)
        assertEquals(QrPayload.TYPE_STELLAR_ADDRESS, decoded!!.type)
        assertEquals("yasmine*stellar.org", decoded.userId)
    }

    @Test
    fun billPaymentExpirationCheck() {
        val expiredBill = QrPayload(
            type = QrPayload.TYPE_BILL_PAYMENT,
            userId = "09171234567",
            amount = 500.0,
            billRef = "Dinner",
            transactionId = "BILL-1234",
            timestamp = System.currentTimeMillis() - 25 * 60 * 60 * 1000L,
            securityToken = "token-123"
        )
        assertTrue(expiredBill.isExpired())

        val activeBill = expiredBill.copy(timestamp = System.currentTimeMillis())
        assertFalse(activeBill.isExpired())
    }

    @Test
    fun blankQrStringsReturnNull() {
        assertNull(QrPayload.decode(""))
        assertNull(QrPayload.decode("   "))
    }
}
