package com.example.kofre.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "investments")
data class InvestmentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val type: String,
    val horizon: String,
    val currentBalanceInCents: Long = 0L
)
