package com.example.pundarapp.data.remote

import com.example.pundarapp.ui.data.BillStatus
import com.example.pundarapp.ui.data.GroupBill
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.serialization.Serializable

@Serializable
data class SupabaseBill(
    val id: String,
    val name: String,
    val total_amount: Double,
    val member_count: Int,
    val status: String,
    val date: String,
    val your_share: Double
)

object PayRepository {
    private val client = Supabase.client.postgrest

    suspend fun getBills(): List<GroupBill> {
        return try {
            val supabaseBills = client["group_bills"].select().decodeList<SupabaseBill>()
            supabaseBills.map { 
                GroupBill(
                    id = it.id,
                    name = it.name,
                    totalAmount = it.total_amount,
                    memberCount = it.member_count,
                    status = BillStatus.valueOf(it.status),
                    date = it.date,
                    yourShare = it.your_share,
                    members = emptyList() // Fetching members separately could be added later
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun createBill(bill: GroupBill): Boolean {
        return try {
            val supabaseBill = SupabaseBill(
                id = bill.id,
                name = bill.name,
                total_amount = bill.totalAmount,
                member_count = bill.memberCount,
                status = bill.status.name,
                date = bill.date,
                your_share = bill.yourShare
            )
            client["group_bills"].insert(supabaseBill)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
