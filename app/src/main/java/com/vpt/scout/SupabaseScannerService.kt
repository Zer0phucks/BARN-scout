package com.vpt.scout

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Authenticator
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

interface ScannerDataService {
    suspend fun getProperties(filters: PropertyFilters, page: Int = 1, perPage: Int = 50): PropertiesResponse
    suspend fun getNextProperty(
        latitude: Double,
        longitude: Double,
        city: String? = null,
        vptOnly: Boolean = false,
        listId: Long? = null,
        conditionMin: Float? = null,
        conditionMax: Float? = null
    ): NextPropertyResponse
    suspend fun getLists(): List<PropertyList>
    suspend fun createList(request: CreateListRequest): PropertyList
    suspend fun getList(listId: Long): ListWithProperties
    suspend fun deleteList(listId: Long)
    suspend fun addPropertiesToList(listId: Long, request: AddPropertiesRequest)
    suspend fun removePropertyFromList(listId: Long, apn: String)
    suspend fun getListRoute(listId: Long): RouteResponse
    suspend fun submitScoutResult(request: ScoutResultRequest)
    suspend fun getScoutResults(collectionId: Long? = null): List<ScoutResult>
    suspend fun getScoutStats(): ScoutStats
}

class SupabaseScannerService(
    private val baseUrl: String,
    private val anonKey: String,
    private val accessTokenProvider: () -> String?,
    private val authManager: SupabaseAuthManager? = null,
    private val client: OkHttpClient = buildClient(accessTokenProvider, authManager)
) : ScannerDataService {

    override suspend fun getNextProperty(
        latitude: Double,
        longitude: Double,
        city: String?,
        vptOnly: Boolean,
        listId: Long?,
        conditionMin: Float?,
        conditionMax: Float?
    ): NextPropertyResponse = withContext(Dispatchers.IO) {
        val body = JsonObject().apply {
            addProperty("p_lat", latitude)
            addProperty("p_lng", longitude)
            addProperty("p_city", city?.trim()?.uppercase().orEmpty())
            addProperty("p_list_id", listId)
            addProperty("p_q", "")
            addProperty("p_vpt", if (vptOnly) 1 else -1)
            if (conditionMin != null) {
                addProperty("p_condition_min", conditionMin)
            }
            if (conditionMax != null) {
                addProperty("p_condition_max", conditionMax)
            }
        }

        val request = Request.Builder()
            .url("${baseUrl.trimEnd('/')}/rest/v1/rpc/android_get_next_scoutable_property")
            .addHeader("apikey", anonKey)
            .addHeader("Authorization", "Bearer ${accessTokenProvider().orEmpty()}")
            .addHeader("Content-Type", "application/json")
            .post(body.toString().toRequestBody(JSON_MEDIA_TYPE))
            .build()

        client.newCall(request).execute().use { response ->
            response.requireSuccess()
            val rawBody = response.body?.string().orEmpty()
            if (rawBody.isBlank()) {
                return@use NextPropertyResponse(property = null, remaining = 0)
            }
            val payload = extractRpcPayload(rawBody, "android_get_next_scoutable_property")
            val propertyJson = payload.get("property")
            val property = if (propertyJson == null || propertyJson.isJsonNull) {
                null
            } else {
                propertyJson.asJsonObject.toProperty()
            }
            NextPropertyResponse(
                property = property,
                remaining = payload.get("remaining")?.asInt ?: 0
            )
        }
    }

    override suspend fun getProperties(
        filters: PropertyFilters,
        page: Int,
        perPage: Int
    ): PropertiesResponse = withContext(Dispatchers.IO) {
        val requiresClientPostFilter = filters.scouted != null || filters.listId != null
        if (requiresClientPostFilter) {
            val allProperties = fetchAllProperties(filters)
            val scoutedApns = getScoutedApns()
            val listApns = filters.listId?.let { getListApns(it).toSet() }

            val filtered = allProperties
                .map { property ->
                    property.copy(isScouted = property.apn in scoutedApns)
                }
                .filter { property ->
                    val matchesScouted = when (filters.scouted) {
                        true -> property.isScouted
                        false -> !property.isScouted
                        null -> true
                    }
                    val matchesList = listApns?.contains(property.apn) ?: true
                    matchesScouted && matchesList
                }

            val total = filtered.size
            val safePerPage = perPage.coerceAtLeast(1)
            val offset = ((page - 1).coerceAtLeast(0)) * safePerPage
            val pagedProperties = filtered.drop(offset).take(safePerPage)

            PropertiesResponse(
                properties = pagedProperties,
                total = total,
                page = page,
                perPage = perPage,
                totalPages = ((total + safePerPage - 1) / safePerPage).coerceAtLeast(1)
            )
        } else {
            val response = executePropertiesRpc(filters, page, perPage)
            val scoutedApns = getScoutedApns(response.properties.map { it.apn })
            response.copy(
                properties = response.properties.map { property ->
                    property.copy(isScouted = property.apn in scoutedApns)
                }
            )
        }
    }

    override suspend fun getLists(): List<PropertyList> = withContext(Dispatchers.IO) {
        val listRows = executeJsonArrayRequest(
            Request.Builder()
                .url("${baseUrl.trimEnd('/')}/rest/v1/lists?select=id,name,description,created_at&order=name.asc")
                .applySupabaseHeaders()
                .get()
                .build()
        )

        buildList {
            for (index in 0 until listRows.size()) {
                val row = listRows.get(index).asJsonObject
                val listId = row.get("id").asLong
                val count = getListPropertyCount(listId)
                add(
                    PropertyList(
                        id = listId,
                        name = row.get("name")?.asString.orEmpty(),
                        description = row.get("description")?.takeUnless { it.isJsonNull }?.asString,
                        propertyCount = count,
                        createdAt = row.get("created_at")?.takeUnless { it.isJsonNull }?.asString
                    )
                )
            }
        }
    }

    override suspend fun createList(request: CreateListRequest): PropertyList = withContext(Dispatchers.IO) {
        val body = JsonObject().apply {
            addProperty("name", request.name)
            if (!request.description.isNullOrBlank()) {
                addProperty("description", request.description)
            }
        }
        val row = executeJsonObjectRequest(
            Request.Builder()
                .url("${baseUrl.trimEnd('/')}/rest/v1/lists")
                .applySupabaseHeaders(returnRepresentation = true)
                .post(body.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()
        )
        PropertyList(
            id = row.get("id").asLong,
            name = row.get("name").asString,
            description = row.get("description")?.takeUnless { it.isJsonNull }?.asString,
            propertyCount = 0,
            createdAt = row.get("created_at")?.takeUnless { it.isJsonNull }?.asString
        )
    }

    override suspend fun getList(listId: Long): ListWithProperties = withContext(Dispatchers.IO) {
        val listRow = executeJsonObjectRequest(
            Request.Builder()
                .url("${baseUrl.trimEnd('/')}/rest/v1/lists?select=id,name,description,created_at&id=eq.$listId")
                .applySupabaseHeaders()
                .get()
                .build()
        )

        val apnRows = executeJsonArrayRequest(
            Request.Builder()
                .url("${baseUrl.trimEnd('/')}/rest/v1/list_properties?select=apn,sort_order&list_id=eq.$listId&order=sort_order.asc")
                .applySupabaseHeaders()
                .get()
                .build()
        )

        val apns = buildList {
            for (index in 0 until apnRows.size()) {
                add(apnRows.get(index).asJsonObject.get("apn").asString)
            }
        }

        val properties = if (apns.isEmpty()) {
            emptyList()
        } else {
            val inClause = apns.joinToString(",") { "\"$it\"" }
            val propertyRows = executeJsonArrayRequest(
                Request.Builder()
                    .url("${baseUrl.trimEnd('/')}/rest/v1/bills?select=apn,location_of_property,city,has_vpt,condition_score,streetview_image_path,power_status,deceased_count&apn=in.($inClause)")
                    .applySupabaseHeaders()
                    .get()
                    .build()
            )
            val parcelRows = executeJsonArrayRequest(
                Request.Builder()
                    .url("${baseUrl.trimEnd('/')}/rest/v1/parcels?select=APN,row_json&APN=in.($inClause)")
                    .applySupabaseHeaders()
                    .get()
                    .build()
            )
            val scoutedApns = getScoutedApns(apns)

            val billMap = mutableMapOf<String, JsonObject>()
            for (index in 0 until propertyRows.size()) {
                val row = propertyRows.get(index).asJsonObject
                billMap[row.get("apn").asString] = row
            }
            val parcelMap = mutableMapOf<String, com.google.gson.JsonElement?>()
            for (index in 0 until parcelRows.size()) {
                val row = parcelRows.get(index).asJsonObject
                parcelMap[row.get("APN").asString] = row.get("row_json")
            }

            apns.mapNotNull { apn ->
                val bill = billMap[apn] ?: return@mapNotNull null
                // Extract mailing address and last sale date from row_json
                val rowJsonElement = parcelMap[apn]
                var mailingAddress: String? = null
                var lastSaleDate: String? = null
                var isOutOfState = false
                if (rowJsonElement != null && !rowJsonElement.isJsonNull) {
                    val rowObj = when {
                        rowJsonElement.isJsonObject -> rowJsonElement.asJsonObject
                        rowJsonElement.isJsonPrimitive && rowJsonElement.asJsonPrimitive.isString -> {
                            try { JsonParser().parse(rowJsonElement.asString).asJsonObject } catch (_: Exception) { null }
                        }
                        else -> null
                    }
                    if (rowObj != null) {
                        mailingAddress = rowObj.get("MailingAddress")?.takeUnless { it.isJsonNull }?.asString
                        lastSaleDate = rowObj.get("LatestDocumentDate")?.takeUnless { it.isJsonNull }?.asString
                            ?.split(" ")?.firstOrNull()
                        if (!mailingAddress.isNullOrBlank()) {
                            val upper = mailingAddress.uppercase()
                            isOutOfState = !upper.endsWith(" CA") && " CA " !in upper
                        }
                    }
                }
                val row = JsonObject().apply {
                    addProperty("apn", apn)
                    add("location_of_property", bill.get("location_of_property"))
                    add("city", bill.get("city"))
                    add("has_vpt", bill.get("has_vpt"))
                    add("condition_score", bill.get("condition_score"))
                    add("streetview_image_path", bill.get("streetview_image_path"))
                    add("deceased_count", bill.get("deceased_count"))
                    parcelMap[apn]?.let { add("row_json", it) }
                }
                val powerStatus = bill.get("power_status")?.takeUnless { it.isJsonNull }?.asString
                val deceasedCount = bill.get("deceased_count")?.takeUnless { it.isJsonNull }?.asInt
                row.toProperty().copy(
                    isScouted = apn in scoutedApns,
                    powerStatus = powerStatus,
                    mailingAddress = mailingAddress,
                    lastSaleDate = lastSaleDate,
                    isOutOfState = isOutOfState,
                    deceasedCount = deceasedCount
                )
            }
        }

        ListWithProperties(
            id = listRow.get("id").asLong,
            name = listRow.get("name").asString,
            description = listRow.get("description")?.takeUnless { it.isJsonNull }?.asString,
            createdAt = listRow.get("created_at")?.takeUnless { it.isJsonNull }?.asString,
            properties = properties
        )
    }

    override suspend fun deleteList(listId: Long) = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("${baseUrl.trimEnd('/')}/rest/v1/lists?id=eq.$listId")
            .applySupabaseHeaders()
            .delete()
            .build()

        client.newCall(request).execute().use { response ->
            response.requireSuccess()
        }
    }

    override suspend fun addPropertiesToList(listId: Long, request: AddPropertiesRequest) = withContext(Dispatchers.IO) {
        if (request.apns.isEmpty()) {
            return@withContext
        }
        val body = JsonArray().apply {
            request.apns.forEachIndexed { index, apn ->
                add(JsonObject().apply {
                    addProperty("list_id", listId)
                    addProperty("apn", apn)
                    addProperty("sort_order", index)
                })
            }
        }

        client.newCall(
            Request.Builder()
                .url("${baseUrl.trimEnd('/')}/rest/v1/list_properties")
                .applySupabaseHeaders()
                .post(body.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()
        ).execute().use { response ->
            response.requireSuccess()
        }
    }

    override suspend fun removePropertyFromList(listId: Long, apn: String) = withContext(Dispatchers.IO) {
        val encodedApn = apn.encodeUrlComponent()
        client.newCall(
            Request.Builder()
                .url("${baseUrl.trimEnd('/')}/rest/v1/list_properties?list_id=eq.$listId&apn=eq.$encodedApn")
                .applySupabaseHeaders()
                .delete()
                .build()
        ).execute().use { response ->
            response.requireSuccess()
        }
    }

    override suspend fun getListRoute(listId: Long): RouteResponse = withContext(Dispatchers.IO) {
        val list = getList(listId)
        val destinations = list.properties
            .mapNotNull { it.address }
            .filter { it.isNotBlank() }
        val url = if (destinations.isEmpty()) {
            "https://www.google.com/maps"
        } else {
            val joined = destinations.joinToString("/")
            "https://www.google.com/maps/dir/${joined.encodeRoutePath()}"
        }
        RouteResponse(
            url = url,
            propertyCount = destinations.size,
            optimized = false
        )
    }

    override suspend fun submitScoutResult(request: ScoutResultRequest) = withContext(Dispatchers.IO) {
        val body = JsonObject().apply {
            addProperty("apn", request.apn)
            addProperty("follow_up", request.followUp)
            addProperty("flyered", request.flyered)
            if (!request.notes.isNullOrBlank()) {
                addProperty("notes", request.notes)
            } else {
                add("notes", null)
            }
            request.latitude?.let { addProperty("latitude", it) }
            request.longitude?.let { addProperty("longitude", it) }
        }
        client.newCall(
            Request.Builder()
                .url("${baseUrl.trimEnd('/')}/rest/v1/scout_results")
                .applySupabaseHeaders()
                .post(body.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()
        ).execute().use { response ->
            response.requireSuccess()
        }
    }

    override suspend fun getScoutResults(collectionId: Long?): List<ScoutResult> = withContext(Dispatchers.IO) {
        val url = buildString {
            append("${baseUrl.trimEnd('/')}/rest/v1/scout_results?select=id,apn,collection_id,follow_up,flyered,notes,scouted_at,latitude,longitude&order=scouted_at.desc")
            if (collectionId != null) {
                append("&collection_id=eq.$collectionId")
            }
        }
        val rows = executeJsonArrayRequest(
            Request.Builder()
                .url(url)
                .applySupabaseHeaders()
                .get()
                .build()
        )
        buildList {
            for (index in 0 until rows.size()) {
                val row = rows.get(index).asJsonObject
                add(
                    ScoutResult(
                        id = row.get("id").asLong,
                        apn = row.get("apn").asString,
                        followUp = row.get("follow_up").asBoolean,
                        flyered = row.get("flyered").asBoolean,
                        notes = row.get("notes")?.takeUnless { it.isJsonNull }?.asString,
                        scoutedAt = row.get("scouted_at")?.takeUnless { it.isJsonNull }?.asString,
                        latitude = row.get("latitude")?.takeUnless { it.isJsonNull }?.asDouble,
                        longitude = row.get("longitude")?.takeUnless { it.isJsonNull }?.asDouble
                    )
                )
            }
        }
    }

    override suspend fun getScoutStats(): ScoutStats = withContext(Dispatchers.IO) {
        val results = getScoutResults(collectionId = null)
        val uniqueProperties = results.map { it.apn }.toSet().size
        ScoutStats(
            totalVisits = results.size,
            followUps = results.count { it.followUp },
            flyered = results.count { it.flyered },
            uniqueProperties = uniqueProperties
        )
    }

    private fun executePropertiesRpc(
        filters: PropertyFilters,
        page: Int,
        perPage: Int
    ): PropertiesResponse {
        val body = JsonObject().apply {
            addProperty("p_q", filters.query.orEmpty())
            addProperty("p_zip", "")
            addProperty("p_power", "")
            addProperty("p_fav", -1)
            addProperty("p_city", filters.city?.trim()?.uppercase().orEmpty())
            addProperty("p_vpt", if (filters.vptOnly) 1 else -1)
            addProperty("p_delinquent", -1)
            addProperty("p_condition", "")
            addProperty("p_outofstate", -1)
            addProperty("p_sort", "location_of_property")
            addProperty("p_order", "asc")
            addProperty("p_limit", perPage)
            addProperty("p_offset", (page - 1) * perPage)
            addProperty("p_research", "")
            addProperty("p_owner_name", "")
        }

        val request = Request.Builder()
            .url("${baseUrl.trimEnd('/')}/rest/v1/rpc/get_bills_filtered")
            .addHeader("apikey", anonKey)
            .addHeader("Authorization", "Bearer ${accessTokenProvider().orEmpty()}")
            .addHeader("Content-Type", "application/json")
            .post(body.toString().toRequestBody(JSON_MEDIA_TYPE))
            .build()

        return client.newCall(request).execute().use { response ->
            response.requireSuccess()
            val rawBody = response.body?.string().orEmpty()
            val payload = extractRpcPayload(rawBody, "get_bills_filtered")
            val rows = payload.getAsJsonArray("rows") ?: JsonArray()
            val properties = buildList {
                for (index in 0 until rows.size()) {
                    add(rows.get(index).asJsonObject.toProperty())
                }
            }
            PropertiesResponse(
                properties = properties,
                total = payload.get("total")?.asInt ?: properties.size,
                page = page,
                perPage = perPage,
                totalPages = if (perPage > 0) {
                    val total = payload.get("total")?.asInt ?: properties.size
                    ((total + perPage - 1) / perPage).coerceAtLeast(1)
                } else {
                    1
                }
            )
        }
    }

    private fun extractRpcPayload(rawBody: String, rpcName: String): JsonObject {
        if (rawBody.isBlank()) {
            return JsonObject()
        }

        val parsed = JsonParser().parse(rawBody)
        val candidate = when {
            parsed.isJsonObject -> parsed.asJsonObject
            parsed.isJsonArray -> {
                val array = parsed.asJsonArray
                if (array.size() == 0 || !array[0].isJsonObject) {
                    return JsonObject()
                }
                array[0].asJsonObject
            }
            else -> return JsonObject()
        }

        val wrapped = candidate.get(rpcName)
        return if (wrapped != null && wrapped.isJsonObject) {
            wrapped.asJsonObject
        } else {
            candidate
        }
    }

    private fun fetchAllProperties(filters: PropertyFilters): List<Property> {
        val collected = mutableListOf<Property>()
        var page = 1
        var totalPages: Int
        do {
            val response = executePropertiesRpc(
                filters = filters.copy(scouted = null, listId = null),
                page = page,
                perPage = 200
            )
            collected += response.properties
            totalPages = response.totalPages
            page += 1
        } while (page <= totalPages)
        return collected
    }

    private fun JsonObject.toProperty(): Property {
        val hasVptElement = get("has_vpt")
        val hasVpt = when {
            hasVptElement == null || hasVptElement.isJsonNull -> false
            hasVptElement.asJsonPrimitive.isBoolean -> hasVptElement.asBoolean
            else -> hasVptElement.asInt == 1
        }
        val derivedCoordinates = deriveCoordinates(get("row_json"))
        val latitude = get("latitude")?.takeUnless { it.isJsonNull }?.asDouble ?: derivedCoordinates?.first
        val longitude = get("longitude")?.takeUnless { it.isJsonNull }?.asDouble ?: derivedCoordinates?.second
        val locationOfProperty = get("location_of_property")?.takeUnless { it.isJsonNull }?.asString
        val address = get("address")?.takeUnless { it.isJsonNull }?.asString
        val city = get("city")?.takeUnless { it.isJsonNull }?.asString
        val streetviewImagePath = get("streetview_image_path")?.takeUnless { it.isJsonNull }?.asString
        val deceasedCount = get("deceased_count")?.takeUnless { it.isJsonNull }?.asInt

        return Property(
            apn = get("apn")?.asString.orEmpty(),
            address = (
                locationOfProperty
                    ?: address
                )?.takeIf { it.isNotBlank() },
            city = city?.takeIf { it.isNotBlank() },
            latitude = latitude,
            longitude = longitude,
            hasVpt = hasVpt,
            conditionScore = if (has("condition_score") && !get("condition_score").isJsonNull) {
                get("condition_score").asFloat
            } else {
                null
            },
            isScouted = false,
            streetviewImagePath = streetviewImagePath?.takeIf { it.isNotBlank() },
            deceasedCount = deceasedCount
        )
    }

    private fun getListApns(listId: Long): List<String> {
        val apnRows = executeJsonArrayRequest(
            Request.Builder()
                .url("${baseUrl.trimEnd('/')}/rest/v1/list_properties?select=apn&list_id=eq.$listId&order=sort_order.asc")
                .applySupabaseHeaders()
                .get()
                .build()
        )
        return buildList {
            for (index in 0 until apnRows.size()) {
                add(apnRows.get(index).asJsonObject.get("apn").asString)
            }
        }
    }

    private fun getScoutedApns(apns: Collection<String>? = null): Set<String> {
        if (apns != null && apns.isEmpty()) {
            return emptySet()
        }

        val url = buildString {
            append("${baseUrl.trimEnd('/')}/rest/v1/scout_results?select=apn")
            if (apns != null) {
                val inClause = apns.joinToString(",") { "\"$it\"" }
                append("&apn=in.($inClause)")
            }
        }
        val rows = executeJsonArrayRequest(
            Request.Builder()
                .url(url)
                .applySupabaseHeaders()
                .get()
                .build()
        )
        return buildSet {
            for (index in 0 until rows.size()) {
                add(rows.get(index).asJsonObject.get("apn").asString)
            }
        }
    }

    private fun getListPropertyCount(listId: Long): Int {
        return executeJsonArrayRequest(
            Request.Builder()
                .url("${baseUrl.trimEnd('/')}/rest/v1/list_properties?select=apn&list_id=eq.$listId")
                .applySupabaseHeaders()
                .get()
                .build()
        ).size()
    }

    private fun deriveCoordinates(rowJsonElement: com.google.gson.JsonElement?): Pair<Double, Double>? {
        if (rowJsonElement == null || rowJsonElement.isJsonNull) {
            return null
        }
        val rowObject = when {
            rowJsonElement.isJsonObject -> rowJsonElement.asJsonObject
            rowJsonElement.isJsonPrimitive && rowJsonElement.asJsonPrimitive.isString -> {
                JsonParser().parse(rowJsonElement.asString).asJsonObject
            }
            else -> return null
        }

        val x = rowObject.get("CENTROID_X")?.takeUnless { it.isJsonNull }?.asDouble ?: return null
        val y = rowObject.get("CENTROID_Y")?.takeUnless { it.isJsonNull }?.asDouble ?: return null
        if (x == 0.0 || y == 0.0) {
            return null
        }
        val lng = (x / 20037508.34) * 180
        var lat = (y / 20037508.34) * 180
        lat = 180 / Math.PI * (2 * kotlin.math.atan(kotlin.math.exp(lat * Math.PI / 180)) - Math.PI / 2)
        return lat to lng
    }

    private fun executeJsonObjectRequest(request: Request): JsonObject {
        return client.newCall(request).execute().use { response ->
            response.requireSuccess()
            val rawBody = response.body?.string().orEmpty()
            val root = if (rawBody.isBlank()) JsonArray() else JsonParser().parse(rawBody)
            when {
                root.isJsonArray && root.asJsonArray.size() > 0 -> root.asJsonArray.get(0).asJsonObject
                root.isJsonObject -> root.asJsonObject
                else -> JsonObject()
            }
        }
    }

    private fun executeJsonArrayRequest(request: Request): JsonArray {
        return client.newCall(request).execute().use { response ->
            response.requireSuccess()
            val rawBody = response.body?.string().orEmpty()
            if (rawBody.isBlank()) {
                JsonArray()
            } else {
                JsonParser().parse(rawBody).asJsonArray
            }
        }
    }

    private fun Request.Builder.applySupabaseHeaders(
        returnRepresentation: Boolean = false
    ): Request.Builder {
        addHeader("apikey", anonKey)
        addHeader("Authorization", "Bearer ${accessTokenProvider().orEmpty()}")
        addHeader("Content-Type", "application/json")
        if (returnRepresentation) {
            addHeader("Prefer", "return=representation")
        }
        return this
    }

    companion object {
        private fun buildClient(
            accessTokenProvider: () -> String?,
            authManager: SupabaseAuthManager?
        ): OkHttpClient {
            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            }

            val tokenAuthenticator = Authenticator { _, response ->
                if (response.request.header("X-Retry-After-Refresh") != null) {
                    return@Authenticator null
                }
                val refreshed = authManager?.refreshAccessTokenSync() ?: false
                if (!refreshed) {
                    return@Authenticator null
                }
                val newToken = accessTokenProvider().orEmpty()
                if (newToken.isBlank()) {
                    return@Authenticator null
                }
                response.request.newBuilder()
                    .removeHeader("Authorization")
                    .addHeader("Authorization", "Bearer $newToken")
                    .addHeader("X-Retry-After-Refresh", "true")
                    .build()
            }

            return OkHttpClient.Builder()
                .addInterceptor(Interceptor { chain -> chain.proceed(chain.request()) })
                .addInterceptor(logging)
                .authenticator(tokenAuthenticator)
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .build()
        }
    }
}

private fun Response.requireSuccess() {
    if (isSuccessful) return
    val bodyText = body?.string().orEmpty()
    throw IllegalStateException("Supabase scanner request failed (${code}): $bodyText")
}

private fun String.encodeUrlComponent(): String = URLEncoder.encode(this, Charsets.UTF_8.name())

private fun String.encodeRoutePath(): String =
    split("/")
        .joinToString("/") { it.encodeUrlComponent() }
