package com.vpt.scout

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.asCoroutineDispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Collections
import java.util.concurrent.Executors

class SupabaseScannerServiceTest {

    private lateinit var server: MockWebServer

    @Before
    fun setUp() {
        server = MockWebServer()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `getProperties posts scanner filters to get_bills_filtered rpc`() = runBlocking {
        server.enqueueJson("""{"rows":[],"total":0}""")
        server.enqueueJson("""[]""")

        val service = SupabaseScannerService(
            baseUrl = server.url("/").toString(),
            anonKey = "anon-key",
            accessTokenProvider = { "jwt-token" },
            authManager = null
        )

        val response = service.getProperties(
            filters = PropertyFilters(
                city = "OAKLAND",
                query = "elm",
                vptOnly = true
            ),
            page = 1,
            perPage = 50
        )

        val request = server.takeRequest()
        assertEquals("/rest/v1/rpc/get_bills_filtered", request.path)
        assertEquals("Bearer jwt-token", request.getHeader("Authorization"))
        assertEquals("anon-key", request.getHeader("apikey"))
        assertTrue(request.body.readUtf8().contains("\"p_city\":\"OAKLAND\""))
        assertEquals(0, response.total)
        assertTrue(response.properties.isEmpty())
    }

    @Test
    fun `getNextProperty posts location filters to the next scoutable property rpc`() = runBlocking {
        server.enqueueJson(
            """
            {
              "property": {
                "apn": "001-100-100",
                "address": "123 Test St",
                "city": "OAKLAND",
                "latitude": 37.8,
                "longitude": -122.2,
                "has_vpt": true,
                "condition_score": 5.5,
                "streetview_image_path": "streetview/test.jpg"
              },
              "remaining": 4
            }
            """.trimIndent()
        )

        val service = SupabaseScannerService(
            baseUrl = server.url("/").toString(),
            anonKey = "anon-key",
            accessTokenProvider = { "jwt-token" },
            authManager = null
        )

        val response = service.getNextProperty(
            latitude = 37.8,
            longitude = -122.2,
            city = "OAKLAND",
            vptOnly = true,
            listId = 7L
        )

        val request = server.takeRequest()
        assertEquals("/rest/v1/rpc/android_get_next_scoutable_property", request.path)
        assertTrue(request.body.readUtf8().contains("\"p_lat\":37.8"))
        assertEquals("001-100-100", response.property?.apn)
        assertEquals(4, response.remaining)
    }

    @Test
    fun `getProperties derives map coordinates from row_json`() = runBlocking {
        server.enqueueJson(
            """
            {
              "rows": [
                {
                  "apn": "001-100-100",
                  "location_of_property": "123 Test St",
                  "city": "OAKLAND",
                  "has_vpt": 1,
                  "condition_score": 4.0,
                  "row_json": {
                    "CENTROID_X": "-13603237.85",
                    "CENTROID_Y": "4547675.35"
                  }
                }
              ],
              "total": 1
            }
            """.trimIndent()
        )
        server.enqueueJson("""[]""")

        val service = SupabaseScannerService(
            baseUrl = server.url("/").toString(),
            anonKey = "anon-key",
            accessTokenProvider = { "jwt-token" },
            authManager = null
        )

        val response = service.getProperties(
            filters = PropertyFilters(city = "OAKLAND"),
            page = 1,
            perPage = 50
        )

        assertEquals(1, response.properties.size)
        assertTrue(response.properties.first().latitude != null)
        assertTrue(response.properties.first().longitude != null)
    }

    @Test
    fun `getProperties tolerates explicit json nulls in property rows`() = runBlocking {
        server.enqueueJson(
            """
            {
              "rows": [
                {
                  "apn": "001-100-100",
                  "location_of_property": null,
                  "address": "123 Test St",
                  "city": null,
                  "has_vpt": 1,
                  "condition_score": null,
                  "streetview_image_path": null,
                  "row_json": null
                }
              ],
              "total": 1
            }
            """.trimIndent()
        )
        server.enqueueJson("""[]""")

        val service = SupabaseScannerService(
            baseUrl = server.url("/").toString(),
            anonKey = "anon-key",
            accessTokenProvider = { "jwt-token" },
            authManager = null
        )

        val response = service.getProperties(
            filters = PropertyFilters(city = "OAKLAND"),
            page = 1,
            perPage = 50
        )

        assertEquals(1, response.total)
        assertEquals("001-100-100", response.properties.single().apn)
        assertEquals("123 Test St", response.properties.single().address)
        assertEquals(null, response.properties.single().city)
    }

    @Test
    fun `getProperties parses wrapped postgrest rpc payload`() = runBlocking {
        server.enqueueJson(
            """
            [
              {
                "get_bills_filtered": {
                  "rows": [
                    {
                      "apn": "001-100-100",
                      "location_of_property": "123 Test St",
                      "city": "OAKLAND",
                      "has_vpt": 1
                    }
                  ],
                  "total": 1
                }
              }
            ]
            """.trimIndent()
        )
        server.enqueueJson("""[]""")

        val service = SupabaseScannerService(
            baseUrl = server.url("/").toString(),
            anonKey = "anon-key",
            accessTokenProvider = { "jwt-token" },
            authManager = null
        )

        val response = service.getProperties(
            filters = PropertyFilters(city = "OAKLAND"),
            page = 1,
            perPage = 50
        )

        assertEquals(1, response.total)
        assertEquals("001-100-100", response.properties.single().apn)
    }

    @Test
    fun `getProperties sends -1 for p_vpt when not filtering to vpt only`() = runBlocking {
        server.enqueueJson("""{"rows":[],"total":0}""")
        server.enqueueJson("""[]""")

        val service = SupabaseScannerService(
            baseUrl = server.url("/").toString(),
            anonKey = "anon-key",
            accessTokenProvider = { "jwt-token" },
            authManager = null
        )

        service.getProperties(
            filters = PropertyFilters(city = "OAKLAND", vptOnly = false),
            page = 1,
            perPage = 50
        )

        val request = server.takeRequest()
        assertTrue(request.body.readUtf8().contains("\"p_vpt\":-1"))
    }

    @Test
    fun `getProperties sends no-filter sentinel values for optional integer filters`() = runBlocking {
        server.enqueueJson("""{"rows":[],"total":0}""")
        server.enqueueJson("""[]""")

        val service = SupabaseScannerService(
            baseUrl = server.url("/").toString(),
            anonKey = "anon-key",
            accessTokenProvider = { "jwt-token" },
            authManager = null
        )

        service.getProperties(
            filters = PropertyFilters(city = "OAKLAND", vptOnly = false),
            page = 1,
            perPage = 50
        )

        val requestBody = server.takeRequest().body.readUtf8()
        assertTrue(requestBody.contains("\"p_fav\":-1"))
        assertTrue(requestBody.contains("\"p_delinquent\":-1"))
        assertTrue(requestBody.contains("\"p_outofstate\":-1"))
    }

    @Test
    fun `getProperties applies list and unscouted filters client side`() = runBlocking {
        server.enqueueJson(
            """
            {
              "rows": [
                {"apn": "001-001", "location_of_property": "101 First St", "city": "OAKLAND", "has_vpt": 1},
                {"apn": "002-002", "location_of_property": "202 Second St", "city": "OAKLAND", "has_vpt": 0}
              ],
              "total": 2
            }
            """.trimIndent()
        )
        server.enqueueJson("""[{"apn":"001-001"}]""")
        server.enqueueJson("""[{"apn":"001-001"},{"apn":"002-002"}]""")

        val service = SupabaseScannerService(
            baseUrl = server.url("/").toString(),
            anonKey = "anon-key",
            accessTokenProvider = { "jwt-token" },
            authManager = null
        )

        val response = service.getProperties(
            filters = PropertyFilters(city = "OAKLAND", scouted = false, listId = 7L),
            page = 1,
            perPage = 50
        )

        assertEquals(1, response.total)
        assertEquals(listOf("002-002"), response.properties.map { it.apn })
        assertTrue(!response.properties.first().isScouted)
    }

    @Test
    fun `getList hydrates parcel coordinates and scout state in list order`() = runBlocking {
        server.enqueueJson("""[{"id":7,"name":"East Bay","description":"targets","created_at":"2026-04-05T00:00:00Z"}]""")
        server.enqueueJson("""[{"apn":"002-002","sort_order":1},{"apn":"001-001","sort_order":2}]""")
        server.enqueueJson(
            """
            [
              {"apn":"001-001","location_of_property":"101 First St","city":"OAKLAND","has_vpt":1,"condition_score":4.5,"streetview_image_path":"streetview/1.jpg"},
              {"apn":"002-002","location_of_property":"202 Second St","city":"OAKLAND","has_vpt":0,"condition_score":6.0,"streetview_image_path":"streetview/2.jpg"}
            ]
            """.trimIndent()
        )
        server.enqueueJson(
            """
            [
              {"APN":"001-001","row_json":{"CENTROID_X":"-13603237.85","CENTROID_Y":"4547675.35"}},
              {"APN":"002-002","row_json":{"CENTROID_X":"-13603200.00","CENTROID_Y":"4547600.00"}}
            ]
            """.trimIndent()
        )
        server.enqueueJson("""[{"apn":"002-002"}]""")

        val service = SupabaseScannerService(
            baseUrl = server.url("/").toString(),
            anonKey = "anon-key",
            accessTokenProvider = { "jwt-token" },
            authManager = null
        )

        val list = service.getList(7L)

        assertEquals(listOf("002-002", "001-001"), list.properties.map { it.apn })
        assertTrue(list.properties.first().latitude != null)
        assertTrue(list.properties.first().longitude != null)
        assertTrue(list.properties.first().isScouted)
        assertTrue(!list.properties.last().isScouted)
    }

    @Test
    fun `getProperties executes blocking http work off the caller thread`() = runBlocking {
        val requestThreads = Collections.synchronizedList(mutableListOf<String>())
        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                requestThreads += Thread.currentThread().name
                val body = when (chain.request().url.encodedPath) {
                    "/rest/v1/rpc/get_bills_filtered" -> """{"rows":[],"total":0}"""
                    else -> "[]"
                }
                Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body(body.toResponseBody("application/json".toMediaType()))
                    .build()
            }
            .build()

        val service = SupabaseScannerService(
            baseUrl = "https://example.supabase.co",
            anonKey = "anon-key",
            accessTokenProvider = { "jwt-token" },
            authManager = null,
            client = client
        )

        Executors.newSingleThreadExecutor { runnable ->
            Thread(runnable, "caller-thread")
        }.asCoroutineDispatcher().use { dispatcher ->
            withContext(dispatcher) {
                service.getProperties(
                    filters = PropertyFilters(city = "OAKLAND"),
                    page = 1,
                    perPage = 50
                )
            }
        }

        assertTrue(requestThreads.isNotEmpty())
        assertTrue(requestThreads.none { it.contains("caller-thread") })
    }

    private fun MockWebServer.enqueueJson(body: String) {
        enqueue(
            MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "application/json")
                .setBody(body)
        )
    }
}
