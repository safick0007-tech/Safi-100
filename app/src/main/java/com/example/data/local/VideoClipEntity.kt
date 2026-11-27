package com.example.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Entity(
    tableName = "video_clips",
    indices = [Index("createdAt"), Index("parentClipId")]
)
data class VideoClipEntity(
    @PrimaryKey val id: String,
    val prompt: String,
    val mode: String,
    val model: String,
    val ratio: String,
    val segmentDurationSec: Int,
    val totalDurationSec: Int,
    val videoPath: String,
    val thumbPath: String,
    val lastFramePath: String,
    val parentClipId: String?,
    val extendStep: Int,
    val createdAt: Long
)

@Dao
interface VideoClipDao {
    @Query("SELECT * FROM video_clips ORDER BY createdAt DESC")
    fun observeAllClips(): Flow<List<VideoClipEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertClip(entity: VideoClipEntity)

    @Query("DELETE FROM video_clips WHERE id = :id")
    suspend fun deleteClip(id: String)
}
