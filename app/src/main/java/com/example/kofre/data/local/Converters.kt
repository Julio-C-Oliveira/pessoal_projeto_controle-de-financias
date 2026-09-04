package com.example.kofre.data.local

import androidx.room.TypeConverter
import com.example.kofre.data.local.enums.CategoryType
import com.example.kofre.data.local.enums.InvestmentHorizon
import com.example.kofre.data.local.enums.InvestmentType
import com.example.kofre.data.local.enums.PaymentMethod
import com.example.kofre.data.local.enums.TransactionType

class Converters {

    @TypeConverter
    fun fromInvestmentHorizon(value: InvestmentHorizon?): String? = value?.name

    @TypeConverter
    fun toInvestmentHorizon(value: String?): InvestmentHorizon? =
        value?.let { runCatching { InvestmentHorizon.valueOf(it) }.getOrNull() }

    @TypeConverter
    fun fromCategoryType(value: CategoryType?): String? = value?.name

    @TypeConverter
    fun toCategoryType(value: String?): CategoryType? =
        value?.let { runCatching { CategoryType.valueOf(it) }.getOrNull() }

    @TypeConverter
    fun fromTransactionType(value: TransactionType?): String? = value?.name

    @TypeConverter
    fun toTransactionType(value: String?): TransactionType? =
        value?.let { runCatching { TransactionType.valueOf(it) }.getOrNull() }

    @TypeConverter
    fun fromPaymentMethod(value: PaymentMethod?): String? = value?.name

    @TypeConverter
    fun toPaymentMethod(value: String?): PaymentMethod? =
        value?.let { runCatching { PaymentMethod.valueOf(it) }.getOrNull() }

    @TypeConverter
    fun fromInvestmentType(value: InvestmentType?): String? = value?.name

    @TypeConverter
    fun toInvestmentType(value: String?): InvestmentType? =
        value?.let { runCatching { InvestmentType.valueOf(it) }.getOrNull() }
}
