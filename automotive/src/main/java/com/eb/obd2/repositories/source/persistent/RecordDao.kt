package com.eb.obd2.repositories.source.persistent

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import java.time.LocalDateTime

@Dao
interface RecordDao {
    @Insert
    suspend fun insert(record: RecordEntity) : Long

    @Transaction
    @Query("""
        SELECT * FROM record r
        LEFT JOIN speed s ON r.recordId = s.recordId
        WHERE r.time BETWEEN :start AND :end
    """)
    suspend fun getRecordsInRange(start: LocalDateTime, end: LocalDateTime): List<DynamicRecord>
}