package com.vismitmv.echominutes.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "meetings")
data class MeetingEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val audioFilePath: String,
    val transcript: String = "",
    val summary: String = "",
    val durationSeconds: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val syncStatus: String = "PENDING", // PENDING, SYNCED, FAILED, DISABLED
    val syncedAt: Long? = null
)
