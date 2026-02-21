package com.vpt.scout

import com.vpt.scout.data.local.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import java.time.Instant

/**
 * Repository for managing properties with Flask API as source of truth.
 */
class PropertyRepository(
    private val propertyDao: PropertyDao,
    private val apiService: ScoutApiService
) {
    
    private val _propertiesState = MutableStateFlow<PropertiesState>(PropertiesState.Loading)
    val propertiesState: StateFlow<PropertiesState> = _propertiesState
    
    /**
     * Load properties with filters from Flask API.
     */
    suspend fun loadProperties(
        page: Int = 1,
        perPage: Int = 50,
        city: String? = null,
        query: String? = null,
        vptOnly: Boolean = false,
        scouted: Boolean? = null,
        listId: Long? = null
    ): PropertiesResponse {
        val response = apiService.getProperties(
            page = page,
            perPage = perPage,
            city = city,
            query = query,
            vptOnly = if (vptOnly) "1" else null,
            scouted = when (scouted) {
                true -> "true"
                false -> "false"
                null -> null
            },
            listId = listId
        )
        _propertiesState.value = PropertiesState.Success(response)
        return response
    }
    
    /**
     * Get next property for scout mode.
     */
    suspend fun getNextProperty(
        latitude: Double,
        longitude: Double,
        city: String? = null,
        vptOnly: Boolean = false,
        listId: Long? = null
    ): NextPropertyResponse {
        return apiService.getNextProperty(
            latitude = latitude,
            longitude = longitude,
            city = city,
            vptOnly = if (vptOnly) "1" else null,
            listId = listId
        )
    }
    
    /**
     * Refresh map markers from API and cache locally.
     */
    suspend fun refreshMarkers() {
        try {
            val response = apiService.getMarkers()
            val entities = response.features.mapNotNull { feature ->
                val coords = feature.geometry?.coordinates
                if (coords != null && coords.size >= 2) {
                    PropertyEntity(
                        apn = feature.properties.apn,
                        address = feature.properties.address ?: "Unknown",
                        longitude = coords[0],
                        latitude = coords[1],
                        hasVpt = (feature.properties.hasVpt ?: 0) == 1,
                        conditionScore = feature.properties.conditionScore,
                        city = feature.properties.city,
                        streetViewImagePath = feature.properties.streetviewImagePath,
                        updatedAt = Instant.now()
                    )
                } else null
            }
            propertyDao.insertAll(entities)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    /**
     * Get cached markers as Flow for map display.
     */
    val cachedMarkers: Flow<List<PropertyEntity>> = propertyDao.getAllProperties()
    
    /**
     * Get VPT-only properties from cache.
     */
    val cachedVptProperties: Flow<List<PropertyEntity>> = propertyDao.getVptProperties()
    
    /**
     * Load all APNs matching the given filters (for select all functionality).
     * Fetches all pages and collects all APNs.
     */
    suspend fun loadAllPropertyApns(
        city: String? = null,
        query: String? = null,
        vptOnly: Boolean = false,
        scouted: Boolean? = null,
        listId: Long? = null
    ): Set<String> {
        val allApns = mutableSetOf<String>()
        var page = 1
        var totalPages: Int
        
        do {
            val response = loadProperties(
                page = page,
                perPage = 500, // Fetch in larger batches for efficiency
                city = city,
                query = query,
                vptOnly = vptOnly,
                scouted = scouted,
                listId = listId
            )
            allApns.addAll(response.properties.map { it.apn })
            totalPages = response.totalPages
            page++
        } while (page <= totalPages)
        
        return allApns
    }
}

/**
 * State for properties loading.
 */
sealed class PropertiesState {
    object Loading : PropertiesState()
    data class Success(val response: PropertiesResponse) : PropertiesState()
    data class Error(val message: String) : PropertiesState()
}

/**
 * Repository for managing lists (collections) via Flask API.
 */
class ListRepository(
    private val apiService: ScoutApiService
) {
    
    private val _listsState = MutableStateFlow<ListsState>(ListsState.Loading)
    val listsState: StateFlow<ListsState> = _listsState
    
    /**
     * Refresh lists from Flask API.
     */
    suspend fun refreshLists(): List<PropertyList> {
        val lists = apiService.getLists()
        _listsState.value = ListsState.Success(lists)
        return lists
    }
    
    /**
     * Create a new list.
     */
    suspend fun createList(name: String, description: String? = null): PropertyList {
        val newList = apiService.createList(CreateListRequest(name, description))
        refreshLists()
        return newList
    }
    
    /**
     * Delete a list.
     */
    suspend fun deleteList(listId: Long) {
        apiService.deleteList(listId)
        refreshLists()
    }
    
    /**
     * Get list with properties.
     */
    suspend fun getList(listId: Long): ListWithProperties {
        return apiService.getList(listId)
    }
    
    /**
     * Add properties to a list.
     */
    suspend fun addPropertiesToList(listId: Long, apns: List<String>) {
        apiService.addPropertiesToList(listId, AddPropertiesRequest(apns))
    }
    
    /**
     * Remove property from a list.
     */
    suspend fun removePropertyFromList(listId: Long, apn: String) {
        apiService.removePropertyFromList(listId, apn)
    }
    
    /**
     * Get route URL for a list.
     */
    suspend fun getRouteUrl(listId: Long): RouteResponse {
        return apiService.getListRoute(listId)
    }
}

/**
 * State for lists loading.
 */
sealed class ListsState {
    object Loading : ListsState()
    data class Success(val lists: List<PropertyList>) : ListsState()
    data class Error(val message: String) : ListsState()
}

/**
 * Repository for scout mode operations.
 */
class ScoutRepository(
    private val scoutResultDao: ScoutResultDao,
    private val apiService: ScoutApiService
) {
    
    /**
     * Submit a scout result (saves locally and syncs to server).
     */
    suspend fun submitScoutResult(
        apn: String,
        followUp: Boolean,
        flyered: Boolean,
        notes: String?,
        latitude: Double?,
        longitude: Double?
    ): Long {
        // Save locally
        val entity = ScoutResultEntity(
            apn = apn,
            collectionId = null,
            followUp = followUp,
            flyered = flyered,
            notes = notes,
            scoutedAt = Instant.now(),
            latitude = latitude,
            longitude = longitude,
            synced = false
        )
        val id = scoutResultDao.insert(entity)
        
        // Sync to server
        try {
            apiService.submitScoutResult(
                ScoutResultRequest(
                    apn = apn,
                    followUp = followUp,
                    flyered = flyered,
                    notes = notes,
                    latitude = latitude,
                    longitude = longitude
                )
            )
            scoutResultDao.markAsSynced(id)
        } catch (e: Exception) {
            e.printStackTrace()
            // Will sync later
        }
        
        return id
    }
    
    /**
     * Sync pending results to server.
     */
    suspend fun syncPendingResults() {
        val unsynced = scoutResultDao.getUnsyncedResults()
        for (result in unsynced) {
            try {
                apiService.submitScoutResult(
                    ScoutResultRequest(
                        apn = result.apn,
                        followUp = result.followUp,
                        flyered = result.flyered,
                        notes = result.notes,
                        latitude = result.latitude,
                        longitude = result.longitude
                    )
                )
                scoutResultDao.markAsSynced(result.id)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    /**
     * Get scout statistics from server.
     */
    suspend fun getStats(): ScoutStats {
        return apiService.getScoutStats()
    }
    
    /**
     * Get local scout results as Flow.
     */
    fun getLocalResults(): Flow<List<ScoutResultEntity>> {
        return scoutResultDao.getAllResults()
    }
}

// Keep old CollectionRepository for backward compatibility during migration
// This can be removed once fully migrated to ListRepository
class CollectionRepository(
    private val collectionDao: CollectionDao,
    private val scoutResultDao: ScoutResultDao,
    private val apiService: ScoutApiService
) {
    // Delegate to ListRepository for API operations
    private val listRepo = ListRepository(apiService)
    
    val allCollections: Flow<List<PropertyList>> = flow {
        emit(listRepo.refreshLists())
    }
    
    suspend fun createCollection(name: String, description: String? = null): Long {
        val list = listRepo.createList(name, description)
        return list.id
    }
    
    suspend fun deleteCollection(collectionId: Long) {
        listRepo.deleteList(collectionId)
    }
    
    suspend fun getPropertiesInCollection(collectionId: Long): List<Property> {
        val list = listRepo.getList(collectionId)
        return list.properties
    }
    
    suspend fun addPropertyToCollection(collectionId: Long, apn: String) {
        listRepo.addPropertiesToList(collectionId, listOf(apn))
    }
    
    suspend fun removePropertyFromCollection(collectionId: Long, apn: String) {
        listRepo.removePropertyFromList(collectionId, apn)
    }
    
    // Scout results - delegate to ScoutRepository
    private val scoutRepo = ScoutRepository(scoutResultDao, apiService)
    
    suspend fun saveScoutResult(
        apn: String,
        followUp: Boolean,
        flyered: Boolean,
        notes: String?,
        latitude: Double?,
        longitude: Double?
    ): Long {
        return scoutRepo.submitScoutResult(apn, followUp, flyered, notes, latitude, longitude)
    }
    
    suspend fun syncPendingResults() {
        scoutRepo.syncPendingResults()
    }
}
