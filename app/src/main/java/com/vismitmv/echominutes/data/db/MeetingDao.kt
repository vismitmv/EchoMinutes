package com.vismitmv.echominutes.data.db

import androidx.room.*

@Dao
interface MeetingDao {
    @Insert
    suspend fun insert(meeting: MeetingEntity): Long

    @Update
    suspend fun update(meeting: MeetingEntity)

    @Query("SELECT * FROM meetings ORDER BY createdAt DESC")
    suspend fun getAllMeetings(): List<MeetingEntity>

    @Query("SELECT * FROM meetings WHERE id = :id")
    suspend fun getById(id: Long): MeetingEntity?

    @Query("DELETE FROM meetings WHERE id = :id")
    suspend fun deleteById(id: Long)
}
