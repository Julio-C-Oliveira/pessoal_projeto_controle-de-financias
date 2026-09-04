package com.example.kofre.ui.util

import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object Formatters {

    private val ptBrLocale = Locale("pt", "BR")
    private val currencyFormat = NumberFormat.getCurrencyInstance(ptBrLocale)
    private val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    private val monthYearFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", ptBrLocale)

    fun formatCentsToCurrency(amountInCents: Long): String {
        val amount = amountInCents / 100.0
        return currencyFormat.format(amount)
    }

    fun formatDate(timestamp: Long): String {
        return Instant.ofEpochMilli(timestamp)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
            .format(dateFormatter)
    }

    fun formatMonthYear(timestamp: Long): String {
        val formatted = Instant.ofEpochMilli(timestamp)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
            .format(monthYearFormatter)
        return formatted.replaceFirstChar { if (it.isLowerCase()) it.titlecase(ptBrLocale) else it.toString() }
    }
}
