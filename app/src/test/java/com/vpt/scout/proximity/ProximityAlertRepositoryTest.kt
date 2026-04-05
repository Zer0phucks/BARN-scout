package com.vpt.scout.proximity

import com.vpt.scout.AddPropertiesRequest
import com.vpt.scout.CreateListRequest
import com.vpt.scout.ListWithProperties
import com.vpt.scout.NextPropertyResponse
import com.vpt.scout.PropertiesResponse
import com.vpt.scout.Property
import com.vpt.scout.PropertyFilters
import com.vpt.scout.PropertyList
import com.vpt.scout.PropertyRepository
import com.vpt.scout.RouteResponse
import com.vpt.scout.ScannerDataService
import com.vpt.scout.ScoutResult
import com.vpt.scout.ScoutResultRequest
import com.vpt.scout.ScoutStats
import com.vpt.scout.data.local.PropertyDao
import com.vpt.scout.data.local.PropertyEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProximityAlertRepositoryTest {

    @Test
    fun `getNearestUnscoutedProperty uses next-property rpc with global filters`() = runBlocking {
        val service = FakeProximityScannerDataService(
            nextProperty = testProperty(apn = "001-100-100")
        )
        val propertyRepository = PropertyRepository(FakePropertyDao(), service)
        val repository = ProximityAlertRepository(propertyRepository)

        val property = repository.getNearestUnscoutedProperty(37.8, -122.2)

        assertEquals("001-100-100", property?.apn)
        assertEquals(37.8, service.lastGetNextLatitude)
        assertEquals(-122.2, service.lastGetNextLongitude)
        assertEquals(null, service.lastGetNextCity)
        assertEquals(false, service.lastGetNextVptOnly)
        assertEquals(null, service.lastGetNextListId)
    }

    @Test
    fun `getPropertyForAlert returns exact alerted property by apn`() = runBlocking {
        val service = FakeProximityScannerDataService(
            propertiesByQuery = mapOf(
                "001-100-100" to listOf(testProperty(apn = "001-100-100"))
            )
        )
        val propertyRepository = PropertyRepository(FakePropertyDao(), service)
        val repository = ProximityAlertRepository(propertyRepository)

        val property = repository.getPropertyForAlert("001-100-100")

        assertEquals("001-100-100", property?.apn)
        assertEquals("001-100-100", service.lastPropertiesQuery)
    }

    @Test
    fun `returns null when nearest property is already scouted`() = runBlocking {
        val service = FakeProximityScannerDataService(
            nextProperty = testProperty(apn = "001-100-100", isScouted = true)
        )
        val propertyRepository = PropertyRepository(FakePropertyDao(), service)
        val repository = ProximityAlertRepository(propertyRepository)

        val property = repository.getNearestUnscoutedProperty(37.8, -122.2)

        assertNull(property)
    }
}

private class FakeProximityScannerDataService(
    private val nextProperty: Property? = null,
    private val propertiesByQuery: Map<String, List<Property>> = emptyMap()
) : ScannerDataService {
    var lastGetNextLatitude: Double? = null
    var lastGetNextLongitude: Double? = null
    var lastGetNextCity: String? = null
    var lastGetNextVptOnly: Boolean? = null
    var lastGetNextListId: Long? = null
    var lastPropertiesQuery: String? = null

    override suspend fun getProperties(
        filters: PropertyFilters,
        page: Int,
        perPage: Int
    ): PropertiesResponse {
        lastPropertiesQuery = filters.query
        val properties = propertiesByQuery[filters.query].orEmpty()
        return PropertiesResponse(
            properties = properties,
            total = properties.size,
            page = page,
            perPage = perPage,
            totalPages = 1
        )
    }

    override suspend fun getNextProperty(
        latitude: Double,
        longitude: Double,
        city: String?,
        vptOnly: Boolean,
        listId: Long?,
        conditionMin: Float?,
        conditionMax: Float?
    ): NextPropertyResponse {
        lastGetNextLatitude = latitude
        lastGetNextLongitude = longitude
        lastGetNextCity = city
        lastGetNextVptOnly = vptOnly
        lastGetNextListId = listId
        return NextPropertyResponse(nextProperty, remaining = if (nextProperty == null) 0 else 1)
    }

    override suspend fun getLists(): List<PropertyList> = emptyList()

    override suspend fun createList(request: CreateListRequest): PropertyList {
        error("Not needed in this test")
    }

    override suspend fun getList(listId: Long): ListWithProperties {
        error("Not needed in this test")
    }

    override suspend fun deleteList(listId: Long) {}

    override suspend fun addPropertiesToList(listId: Long, request: AddPropertiesRequest) {}

    override suspend fun removePropertyFromList(listId: Long, apn: String) {}

    override suspend fun getListRoute(listId: Long): RouteResponse {
        error("Not needed in this test")
    }

    override suspend fun submitScoutResult(request: ScoutResultRequest) {}

    override suspend fun getScoutResults(collectionId: Long?): List<ScoutResult> = emptyList()

    override suspend fun getScoutStats(): ScoutStats = ScoutStats(0, 0, 0, 0)
}

private class FakePropertyDao : PropertyDao {
    private val flow = MutableStateFlow<List<PropertyEntity>>(emptyList())

    override fun getAllProperties(): Flow<List<PropertyEntity>> = flow

    override suspend fun getPropertyByApn(apn: String): PropertyEntity? = null

    override fun getVptProperties(): Flow<List<PropertyEntity>> = flow

    override fun getPropertiesByCity(city: String): Flow<List<PropertyEntity>> = flow

    override suspend fun insertAll(properties: List<PropertyEntity>) {
        flow.value = properties
    }

    override suspend fun insert(property: PropertyEntity) {
        flow.value = flow.value + property
    }

    override suspend fun deleteAll() {
        flow.value = emptyList()
    }

    override suspend fun getCount(): Int = flow.value.size
}

private fun testProperty(
    apn: String,
    isScouted: Boolean = false
): Property = Property(
    apn = apn,
    address = "123 Test St",
    city = "OAKLAND",
    latitude = 37.8,
    longitude = -122.2,
    hasVpt = true,
    conditionScore = 5.0f,
    isScouted = isScouted,
    streetviewImagePath = null
)
