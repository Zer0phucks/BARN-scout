package com.vpt.scout.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PropertyDao {
    
    @Query("SELECT * FROM properties ORDER BY city, address")
    fun getAllProperties(): Flow<List<PropertyEntity>>
    
    @Query("SELECT * FROM properties WHERE apn = :apn")
    suspend fun getPropertyByApn(apn: String): PropertyEntity?
    
    @Query("SELECT * FROM properties WHERE hasVpt = 1 ORDER BY city, address")
    fun getVptProperties(): Flow<List<PropertyEntity>>
    
    @Query("SELECT * FROM properties WHERE city = :city ORDER BY address")
    fun getPropertiesByCity(city: String): Flow<List<PropertyEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(properties: List<PropertyEntity>)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(property: PropertyEntity)
    
    @Query("DELETE FROM properties")
    suspend fun deleteAll()
    
    @Query("SELECT COUNT(*) FROM properties")
    suspend fun getCount(): Int
}

@Dao
interface CollectionDao {
    
    @Query("SELECT * FROM collections ORDER BY updatedAt DESC")
    fun getAllCollections(): Flow<List<CollectionEntity>>
    
    @Query("SELECT * FROM collections WHERE id = :id")
    suspend fun getCollectionById(id: Long): CollectionEntity?
    
    @Query("""
        SELECT p.* FROM properties p
        INNER JOIN collection_properties cp ON p.apn = cp.apn
        WHERE cp.collectionId = :collectionId
        ORDER BY cp.sortOrder
    """)
    fun getPropertiesInCollection(collectionId: Long): Flow<List<PropertyEntity>>
    
    @Query("SELECT COUNT(*) FROM collection_properties WHERE collectionId = :collectionId")
    suspend fun getPropertyCount(collectionId: Long): Int
    
    @Insert
    suspend fun insertCollection(collection: CollectionEntity): Long
    
    @Update
    suspend fun updateCollection(collection: CollectionEntity)
    
    @Delete
    suspend fun deleteCollection(collection: CollectionEntity)
    
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addPropertyToCollection(item: CollectionPropertyEntity)
    
    @Query("DELETE FROM collection_properties WHERE collectionId = :collectionId AND apn = :apn")
    suspend fun removePropertyFromCollection(collectionId: Long, apn: String)
    
    @Query("SELECT EXISTS(SELECT 1 FROM collection_properties WHERE collectionId = :collectionId AND apn = :apn)")
    suspend fun isPropertyInCollection(collectionId: Long, apn: String): Boolean
}

@Dao
interface ScoutResultDao {
    
    @Query("SELECT * FROM scout_results ORDER BY scoutedAt DESC")
    fun getAllResults(): Flow<List<ScoutResultEntity>>

    @Query("SELECT * FROM scout_results WHERE collectionId = :collectionId ORDER BY scoutedAt DESC")
    fun getResultsForCollection(collectionId: Long): Flow<List<ScoutResultEntity>>
    
    @Query("SELECT * FROM scout_results WHERE apn = :apn ORDER BY scoutedAt DESC LIMIT 1")
    suspend fun getLatestResultForProperty(apn: String): ScoutResultEntity?
    
    @Query("SELECT apn FROM scout_results WHERE collectionId = :collectionId")
    suspend fun getScoutedApnsInCollection(collectionId: Long): List<String>
    
    @Query("SELECT * FROM scout_results WHERE synced = 0")
    suspend fun getUnsyncedResults(): List<ScoutResultEntity>
    
    @Insert
    suspend fun insert(result: ScoutResultEntity): Long
    
    @Query("UPDATE scout_results SET synced = 1 WHERE id = :id")
    suspend fun markAsSynced(id: Long)
    
    @Query("SELECT COUNT(*) FROM scout_results WHERE collectionId = :collectionId")
    suspend fun getScoutedCountForCollection(collectionId: Long): Int
}
