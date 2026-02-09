package com.vpt.scout.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.TypeConverter
import java.time.Instant

/**
 * Property entity cached from the Flask API.
 */
@Entity(tableName = "properties")
data class PropertyEntity(
    @PrimaryKey
    val apn: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val hasVpt: Boolean = false,
    val conditionScore: Float? = null,
    val city: String? = null,
    val streetViewImagePath: String? = null,
    val updatedAt: Instant = Instant.now()
)

/**
 * Scouting collection (like a playlist of properties).
 */
@Entity(tableName = "collections")
data class CollectionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val description: String? = null,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now()
)

/**
 * Junction table linking properties to collections.
 */
@Entity(
    tableName = "collection_properties",
    primaryKeys = ["collectionId", "apn"],
    foreignKeys = [
        ForeignKey(
            entity = CollectionEntity::class,
            parentColumns = ["id"],
            childColumns = ["collectionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("collectionId"), Index("apn")]
)
data class CollectionPropertyEntity(
    val collectionId: Long,
    val apn: String,
    val sortOrder: Int = 0,
    val addedAt: Instant = Instant.now()
)

/**
 * Scout visit result captured in the field.
 */
@Entity(tableName = "scout_results")
data class ScoutResultEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val apn: String,
    val collectionId: Long? = null,
    val followUp: Boolean,
    val flyered: Boolean,
    val notes: String? = null,
    val scoutedAt: Instant = Instant.now(),
    val latitude: Double? = null,
    val longitude: Double? = null,
    val synced: Boolean = false
)

/**
 * Type converters for Room.
 */
class Converters {
    @TypeConverter
    fun fromInstant(value: Instant?): Long? = value?.toEpochMilli()
    
    @TypeConverter
    fun toInstant(value: Long?): Instant? = value?.let { Instant.ofEpochMilli(it) }
}
