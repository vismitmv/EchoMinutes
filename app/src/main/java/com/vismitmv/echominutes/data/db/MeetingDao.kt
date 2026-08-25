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

    @Query("SELECT * FROM meetings WHERE syncStatus != 'SYNCED' ORDER BY createdAt ASC")
    suspend fun getUnsyncedMeetings(): List<MeetingEntity>

    @Query("UPDATE meetings SET syncStatus = :status, syncedAt = :syncedAt WHERE id = :id")
    suspend fun updateSyncStatus(id: Long, status: String, syncedAt: Long?)

    @Query("DELETE FROM meetings WHERE id = :id")
    suspend fun deleteById(id: Long)
}
