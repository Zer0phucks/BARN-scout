package com.vpt.scout

import com.vpt.scout.data.local.PropertyDao
import com.vpt.scout.data.local.PropertyEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class SupabaseRepositoryTest {

    @Test
    fun `submitScoutResult fails when supabase write fails`() = runBlocking {
        val repository = ScoutRepository(
            service = FakeScannerDataService(
                submitScoutResultError = IOException("offline")
            )
        )

        val result = runCatching {
            repository.submitScoutResult(
                apn = "001-100-100",
                followUp = true,
                flyered = false,
                notes = "Need follow up",
                latitude = 37.8,
                longitude = -122.2
            )
        }

        assertTrue(result.isFailure)
    }

    @Test
    fun `loadAllPropertyApns collects apns across all pages`() = runBlocking {
        val service = FakeScannerDataService(
            pagedProperties = listOf(
                PropertiesResponse(
                    properties = listOf(testProperty(apn = "001"), testProperty(apn = "002")),
                    total = 3,
                    page = 1,
                    perPage = 500,
                    totalPages = 2
                ),
                PropertiesResponse(
                    properties = listOf(testProperty(apn = "003")),
                    total = 3,
                    page = 2,
                    perPage = 500,
                    totalPages = 2
                )
            )
        )
        val repository = PropertyRepository(
            propertyDao = FakePropertyDao(),
            service = service
        )

        val apns = repository.loadAllPropertyApns(city = "OAKLAND", vptOnly = true)

        assertEquals(setOf("001", "002", "003"), apns)
    }

    @Test
    fun `refreshMarkers replaces cache with mappable properties only`() = runBlocking {
        val propertyDao = FakePropertyDao()
        val service = FakeScannerDataService(
            pagedProperties = listOf(
                PropertiesResponse(
                    properties = listOf(
                        testProperty(apn = "001", latitude = 37.8, longitude = -122.2),
                        testProperty(apn = "002", latitude = null, longitude = -122.3)
                    ),
                    total = 2,
                    page = 1,
                    perPage = 200,
                    totalPages = 1
                )
            )
        )
        val repository = PropertyRepository(propertyDao = propertyDao, service = service)

        repository.refreshMarkers()

        val cached = repository.cachedMarkers.first()
        assertEquals(listOf("001"), cached.map { it.apn })
        assertEquals(1, propertyDao.deleteAllCalls)
    }

    @Test
    fun `refreshMarkers streams map pages into cache using larger page size`() = runBlocking {
        val propertyDao = FakePropertyDao()
        val service = FakeScannerDataService(
            pagedProperties = listOf(
                PropertiesResponse(
                    properties = listOf(testProperty(apn = "001", latitude = 37.8, longitude = -122.2)),
                    total = 2,
                    page = 1,
                    perPage = 1000,
                    totalPages = 2
                ),
                PropertiesResponse(
                    properties = listOf(testProperty(apn = "002", latitude = 37.9, longitude = -122.3)),
                    total = 2,
                    page = 2,
                    perPage = 1000,
                    totalPages = 2
                )
            )
        )
        val repository = PropertyRepository(propertyDao = propertyDao, service = service)

        repository.refreshMarkers()

        assertEquals(listOf(1 to 1000, 2 to 1000), service.propertyRequests)
        assertEquals(2, propertyDao.insertAllCalls)
        assertEquals(listOf("001", "002"), repository.cachedMarkers.first().map { it.apn }.sorted())
    }

    @Test
    fun `mapPropertiesResponseToEntities skips properties without coordinates`() {
        val entities = mapPropertiesResponseToEntities(
            PropertiesResponse(
                properties = listOf(
                    testProperty(apn = "001", latitude = 37.8, longitude = -122.2),
                    testProperty(apn = "002", latitude = null, longitude = -122.3),
                    testProperty(apn = "003", latitude = 37.9, longitude = null)
                ),
                total = 3,
                page = 1,
                perPage = 50,
                totalPages = 1
            )
        )

        assertEquals(1, entities.size)
        assertEquals("001", entities.single().apn)
    }

    @Test
    fun `listRepository createList refreshes lists state`() = runBlocking {
        val service = FakeScannerDataService(
            initialLists = mutableListOf(
                PropertyList(id = 1, name = "Existing", description = null, propertyCount = 1)
            )
        )
        val repository = ListRepository(service)

        val created = repository.createList("New targets", "follow up")

        val state = repository.listsState.value
        assertEquals("New targets", created.name)
        assertTrue(state is ListsState.Success)
        val lists = (state as ListsState.Success).lists
        assertEquals(listOf("Existing", "New targets"), lists.map { it.name })
    }

    @Test
    fun `scoutRepository getStats delegates to service`() = runBlocking {
        val repository = ScoutRepository(
            service = FakeScannerDataService(
                scoutStats = ScoutStats(
                    totalVisits = 8,
                    followUps = 3,
                    flyered = 2,
                    uniqueProperties = 6
                )
            )
        )

        val stats = repository.getStats()

        assertEquals(8, stats.totalVisits)
        assertEquals(3, stats.followUps)
        assertEquals(2, stats.flyered)
        assertEquals(6, stats.uniqueProperties)
    }
}

private class FakeScannerDataService(
    private val submitScoutResultError: Throwable? = null,
    private val pagedProperties: List<PropertiesResponse> = listOf(
        PropertiesResponse(emptyList(), 0, 1, 50, 1)
    ),
    initialLists: MutableList<PropertyList> = mutableListOf(),
    private val scoutStats: ScoutStats = ScoutStats(0, 0, 0, 0)
) : ScannerDataService {
    private val lists = initialLists
    private var nextListId = (lists.maxOfOrNull { it.id } ?: 0L) + 1L
    val propertyRequests = mutableListOf<Pair<Int, Int>>()

    override suspend fun getProperties(
        filters: PropertyFilters,
        page: Int,
        perPage: Int
    ): PropertiesResponse {
        propertyRequests += page to perPage
        return pagedProperties.getOrElse(page - 1) {
        PropertiesResponse(emptyList(), 0, page, perPage, page)
    }
    }

    override suspend fun getNextProperty(
        latitude: Double,
        longitude: Double,
        city: String?,
        vptOnly: Boolean,
        listId: Long?,
        conditionMin: Float?,
        conditionMax: Float?
    ): NextPropertyResponse = NextPropertyResponse(null, 0)

    override suspend fun getLists(): List<PropertyList> = lists.toList()

    override suspend fun createList(request: CreateListRequest): PropertyList {
        val list = PropertyList(
            id = nextListId++,
            name = request.name,
            description = request.description,
            propertyCount = 0,
            createdAt = null
        )
        lists += list
        return list
    }

    override suspend fun getList(listId: Long): ListWithProperties {
        val list = lists.first { it.id == listId }
        return ListWithProperties(
            id = list.id,
            name = list.name,
            description = list.description,
            createdAt = list.createdAt,
            properties = emptyList()
        )
    }

    override suspend fun deleteList(listId: Long) {
        lists.removeAll { it.id == listId }
    }

    override suspend fun addPropertiesToList(listId: Long, request: AddPropertiesRequest) {
    }

    override suspend fun removePropertyFromList(listId: Long, apn: String) {
    }

    override suspend fun getListRoute(listId: Long): RouteResponse {
        error("Not needed in this test")
    }

    override suspend fun submitScoutResult(request: ScoutResultRequest) {
        submitScoutResultError?.let { throw it }
    }

    override suspend fun getScoutResults(collectionId: Long?): List<ScoutResult> = emptyList()

    override suspend fun getScoutStats(): ScoutStats = scoutStats
}

private class FakePropertyDao : PropertyDao {
    private val flow = MutableStateFlow<List<PropertyEntity>>(emptyList())
    var deleteAllCalls: Int = 0
    var insertAllCalls: Int = 0

    override fun getAllProperties(): Flow<List<PropertyEntity>> = flow

    override suspend fun getPropertyByApn(apn: String): PropertyEntity? =
        flow.value.firstOrNull { it.apn == apn }

    override fun getVptProperties(): Flow<List<PropertyEntity>> =
        MutableStateFlow(flow.value.filter { it.hasVpt })

    override fun getPropertiesByCity(city: String): Flow<List<PropertyEntity>> =
        MutableStateFlow(flow.value.filter { it.city == city })

    override suspend fun insertAll(properties: List<PropertyEntity>) {
        insertAllCalls += 1
        val merged = flow.value.associateBy { it.apn }.toMutableMap()
        properties.forEach { property -> merged[property.apn] = property }
        flow.value = merged.values.toList()
    }

    override suspend fun insert(property: PropertyEntity) {
        flow.value = flow.value.filterNot { it.apn == property.apn } + property
    }

    override suspend fun deleteAll() {
        deleteAllCalls += 1
        flow.value = emptyList()
    }

    override suspend fun getCount(): Int = flow.value.size
}

private fun testProperty(
    apn: String,
    latitude: Double? = 37.8,
    longitude: Double? = -122.2
): Property = Property(
    apn = apn,
    address = "123 Test St",
    city = "OAKLAND",
    latitude = latitude,
    longitude = longitude,
    hasVpt = true,
    conditionScore = 5.0f,
    isScouted = false,
    streetviewImagePath = null
)
