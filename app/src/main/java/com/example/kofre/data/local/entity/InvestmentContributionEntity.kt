package com.example.kofre.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "investment_contributions",
    foreignKeys = [
        ForeignKey(
            entity = InvestmentEntity::class,
            parentColumns = ["id"],
            childColumns = ["investmentId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("investmentId")]
)
data class InvestmentContributionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val investmentId: Long,
    val amountInCents: Long,
    val timestamp: Long,
    val notes: String? = null
)
