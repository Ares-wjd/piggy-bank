package io.github.areswjd.piggybank.data.local

import androidx.room.TypeConverter
import io.github.areswjd.piggybank.model.TransactionType
import java.time.LocalDate

class Converters {
    @TypeConverter
    fun localDateToEpochDay(date: LocalDate): Long = date.toEpochDay()

    @TypeConverter
    fun epochDayToLocalDate(epochDay: Long): LocalDate = LocalDate.ofEpochDay(epochDay)

    @TypeConverter
    fun transactionTypeToString(type: TransactionType): String = type.name

    @TypeConverter
    fun stringToTransactionType(value: String): TransactionType = TransactionType.valueOf(value)
}
