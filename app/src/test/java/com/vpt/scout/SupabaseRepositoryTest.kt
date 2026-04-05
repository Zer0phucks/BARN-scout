package com.vpt.scout

import kotlinx.coroutines.runBlocking
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
}

private class FakeScannerDataService(
    private val submitScoutResultError: Throwable? = null
) : ScannerDataService {
    override suspend fun getProperties(
        filters: PropertyFilters,
        page: Int,
        perPage: Int
    ): PropertiesResponse = PropertiesResponse(emptyList(), 0, page, perPage, 1)

    override suspend fun getNextProperty(
        latitude: Double,
        longitude: Double,
        city: String?,
        vptOnly: Boolean,
        listId: Long?,
        conditionMin: Float?,
        conditionMax: Float?
    ): NextPropertyResponse = NextPropertyResponse(null, 0)

    override suspend fun getLists(): List<PropertyList> = emptyList()

    override suspend fun createList(request: CreateListRequest): PropertyList {
        error("Not needed in this test")
    }

    override suspend fun getList(listId: Long): ListWithProperties {
        error("Not needed in this test")
    }

    override suspend fun deleteList(listId: Long) {
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

    override suspend fun getScoutStats(): ScoutStats = ScoutStats(0, 0, 0, 0)
}
