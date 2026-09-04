package com.example.kofre.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.kofre.data.local.entity.InvestmentContributionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface InvestmentContributionDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertContribution(contribution: InvestmentContributionEntity): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertContributions(contributions: List<InvestmentContributionEntity>): List<Long>

    @Query("SELECT * FROM investment_contributions WHERE investmentId = :investmentId ORDER BY timestamp DESC")
    fun getContributionsByInvestmentId(investmentId: Long): Flow<List<InvestmentContributionEntity>>

    @Query("SELECT * FROM investment_contributions ORDER BY timestamp DESC")
    fun getAllContributions(): Flow<List<InvestmentContributionEntity>>

    @Query("DELETE FROM investment_contributions")
    suspend fun deleteAllContributions()
}
