package com.example.kofre.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "recurring_transactions",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["categoryId"])
    ]
)
data class RecurringTransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val amountInCents: Long,
    val categoryId: Long,
    val type: String,
    val paymentMethod: String,
    val frequency: String,
    val startDate: Long,
    val endDate: Long? = null,
    val totalOccurrences: Int? = null,
    val generatedCount: Int = 0,
    val lastGeneratedDate: Long? = null,
    val isActive: Boolean = true,
    val isEssential: Boolean = false,
    val notes: String? = null
)
