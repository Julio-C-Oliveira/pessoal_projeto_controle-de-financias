package com.example.kofre.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["categoryId"]),
        Index(value = ["installmentGroupId"])
    ]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val amountInCents: Long,
    val timestamp: Long,
    val categoryId: Long,
    val type: String,
    val paymentMethod: String,
    val isEssential: Boolean = false,
    val installmentGroupId: String? = null,
    val installmentsCount: Int = 1,
    val currentInstallment: Int = 1,
    val notes: String? = null
)
