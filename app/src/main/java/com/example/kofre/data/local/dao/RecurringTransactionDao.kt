package com.example.kofre.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.kofre.data.local.entity.RecurringTransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecurringTransactionDao {
    @Query("SELECT * FROM recurring_transactions")
    fun getAllRecurringTransactions(): Flow<List<RecurringTransactionEntity>>

    @Query("SELECT * FROM recurring_transactions WHERE isActive = 1")
    fun getActiveRecurringTransactions(): Flow<List<RecurringTransactionEntity>>

    @Query("SELECT * FROM recurring_transactions WHERE isActive = 1")
    suspend fun getActiveRecurringEntities(): List<RecurringTransactionEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(recurring: RecurringTransactionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(recurringList: List<RecurringTransactionEntity>): List<Long>

    @Update
    suspend fun update(recurring: RecurringTransactionEntity)

    @Query("DELETE FROM recurring_transactions WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM recurring_transactions")
    suspend fun deleteAllRecurringTransactions()
}
