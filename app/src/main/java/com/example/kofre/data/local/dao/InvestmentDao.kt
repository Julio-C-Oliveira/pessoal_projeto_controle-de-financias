package com.example.kofre.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.kofre.data.local.entity.InvestmentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface InvestmentDao {
    @Query("SELECT * FROM investments")
    fun getAllInvestments(): Flow<List<InvestmentEntity>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertInvestment(investment: InvestmentEntity): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertInvestments(investments: List<InvestmentEntity>): List<Long>

    @Query("SELECT * FROM investments WHERE id = :id")
    suspend fun getInvestmentById(id: Long): InvestmentEntity?

    @Query("UPDATE investments SET currentBalanceInCents = :newBalanceInCents WHERE id = :id")
    suspend fun updateBalance(id: Long, newBalanceInCents: Long)

    @Query("DELETE FROM investments")
    suspend fun deleteAllInvestments()
}
