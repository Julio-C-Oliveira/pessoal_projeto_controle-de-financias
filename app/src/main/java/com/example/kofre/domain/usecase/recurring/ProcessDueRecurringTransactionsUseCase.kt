package com.example.kofre.domain.usecase.recurring

import com.example.kofre.data.local.enums.PaymentMethod
import com.example.kofre.data.local.enums.RecurrenceFrequency
import com.example.kofre.data.local.enums.TransactionType
import com.example.kofre.domain.model.Transaction
import com.example.kofre.domain.repository.FinanceRepository
import java.time.Instant
import java.time.ZoneId

interface ProcessDueRecurringTransactionsUseCase {
    suspend operator fun invoke(currentTimestamp: Long): Result<Int>
}

class ProcessDueRecurringTransactionsUseCaseImpl(
    private val repository: FinanceRepository
) : ProcessDueRecurringTransactionsUseCase {

    override suspend operator fun invoke(currentTimestamp: Long): Result<Int> {
        return try {
            val activeRules = repository.getActiveRecurringEntities()
            var totalGenerated = 0

            for (rule in activeRules) {
                var lastGen = rule.lastGeneratedDate
                var currentGeneratedCount = rule.generatedCount
                var candidate = getNextCandidateTimestamp(
                    rule.startDate,
                    lastGen,
                    RecurrenceFrequency.valueOf(rule.frequency)
                )
                var generatedForRule = 0

                while (candidate <= currentTimestamp &&
                    (rule.endDate == null || candidate <= rule.endDate) &&
                    (rule.totalOccurrences == null || currentGeneratedCount < rule.totalOccurrences)
                ) {
                    val tx = Transaction(
                        amountInCents = rule.amountInCents,
                        timestamp = candidate,
                        categoryId = rule.categoryId,
                        type = TransactionType.valueOf(rule.type),
                        paymentMethod = PaymentMethod.valueOf(rule.paymentMethod),
                        isEssential = rule.isEssential,
                        recurringTransactionId = rule.id,
                        notes = rule.notes
                    )
                    repository.insertTransaction(tx)

                    lastGen = candidate
                    currentGeneratedCount++
                    generatedForRule++
                    totalGenerated++

                    candidate = getNextCandidateTimestamp(
                        rule.startDate,
                        lastGen,
                        RecurrenceFrequency.valueOf(rule.frequency)
                    )
                }

                if (generatedForRule > 0 && lastGen != null) {
                    repository.updateRecurringTransaction(
                        rule.copy(
                            lastGeneratedDate = lastGen,
                            generatedCount = currentGeneratedCount
                        )
                    )
                }
            }

            Result.success(totalGenerated)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun getNextCandidateTimestamp(
        startDate: Long,
        lastGeneratedDate: Long?,
        frequency: RecurrenceFrequency
    ): Long {
        val zoneId = ZoneId.systemDefault()
        if (lastGeneratedDate == null) {
            return startDate
        }
        val startLocalDate = Instant.ofEpochMilli(startDate).atZone(zoneId).toLocalDate()
        val lastLocalDate = Instant.ofEpochMilli(lastGeneratedDate).atZone(zoneId).toLocalDate()

        return when (frequency) {
            RecurrenceFrequency.WEEKLY -> {
                lastLocalDate.plusWeeks(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
            }
            RecurrenceFrequency.YEARLY -> {
                lastLocalDate.plusYears(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
            }
            RecurrenceFrequency.MONTHLY -> {
                calculateNextMonthlyCandidate(startLocalDate, lastLocalDate, zoneId)
            }
        }
    }

    private fun calculateNextMonthlyCandidate(
        startLocalDate: java.time.LocalDate,
        lastLocalDate: java.time.LocalDate,
        zoneId: ZoneId
    ): Long {
        var monthOffset = 1L
        while (true) {
            val candidate = startLocalDate.plusMonths(monthOffset)
            if (candidate.isAfter(lastLocalDate)) {
                return candidate.atStartOfDay(zoneId).toInstant().toEpochMilli()
            }
            monthOffset++
        }
    }
}
