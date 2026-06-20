package com.mobilemuuzaji.app

import com.mobilemuuzaji.app.network.models.GroupedSaleItem
import com.mobilemuuzaji.app.network.models.SalesItem
import java.text.SimpleDateFormat
import java.util.*

object SalesFilterHelper {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())

    fun filter(items: List<SalesItem>, state: SalesFilterState): List<SalesItem> {
        var result = items

        // Apply date filter
        result = when (state.dateFilter) {
            "today" -> {
                val startOfDay = getStartOfDay(0)
                result.filter { parseDate(it.date) >= startOfDay }
            }
            "week" -> {
                val startOfWeek = getStartOfDay(7)
                result.filter { parseDate(it.date) >= startOfWeek }
            }
            "month" -> {
                val startOfMonth = getStartOfDay(30)
                result.filter { parseDate(it.date) >= startOfMonth }
            }
            "custom" -> {
                val start = state.customStart ?: 0L
                val end   = state.customEnd   ?: Long.MAX_VALUE
                result.filter {
                    val date = parseDate(it.date)
                    date in start..end
                }
            }
            else -> result   // "all" or null — no date filter
        }

        // Apply sort
        result = when (state.sortBy) {
            "alphabetical" -> result.sortedBy { it.item_name.lowercase() }
            "earnings"     -> result.sortedByDescending { it.profit }
            "date"         -> result.sortedByDescending { parseDate(it.date) }
            else           -> result.sortedByDescending { parseDate(it.date) }  // default: newest first
        }

        return result
    }

    fun group(items: List<SalesItem>): List<GroupedSaleItem> {
        return items
            .groupBy { it.item_name }
            .map { (name, sales) ->
                GroupedSaleItem(
                    item_name      = name,
                    total_quantity = sales.sumOf { it.item_quantity },
                    total_gross    = sales.sumOf { it.gross_income },
                    total_profit   = sales.sumOf { it.profit },
                    total_vat      = sales.sumOf { it.vat_amount ?: 0 },
                    sale_count     = sales.size
                )
            }
            .sortedByDescending { it.total_profit }  // grouped view always sorts by total earnings
    }

    private fun parseDate(dateStr: String): Long {
        return try {
            dateFormat.parse(dateStr)?.time ?: 0L
        } catch (e: Exception) {
            0L
        }
    }

    private fun getStartOfDay(daysAgo: Int): Long {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -daysAgo)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }
}