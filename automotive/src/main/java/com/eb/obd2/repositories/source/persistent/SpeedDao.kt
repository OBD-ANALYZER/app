package com.eb.obd2.repositories.source.persistent

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface SpeedDao {
    @Insert
    suspend fun insertSpeed(speed: SpeedEntity): Long

    @Transaction
    @Query("""
        SELECT * FROM speed s
        INNER JOIN record r ON s.recordId = r.recordId
        ORDER BY r.time DESC
        LIMIT 1
    """)
    suspend fun getLatestSpeedRecord(): SpeedRecord?
}