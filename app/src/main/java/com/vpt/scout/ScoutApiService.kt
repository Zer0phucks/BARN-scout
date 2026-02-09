package com.vpt.scout

import android.content.Context
import com.google.gson.GsonBuilder
import com.google.gson.annotations.SerializedName
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import java.util.concurrent.TimeUnit

// ============================================================================
// Data Classes for API Responses
// ============================================================================

/**
 * Property data from /api/properties endpoint.
 */
data class Property(
    val apn: String,
    val address: String?,
    val city: String?,
    val latitude: Double?,
    val longitude: Double?,
    @SerializedName("has_vpt") val hasVpt: Boolean = false,
    @SerializedName("condition_score") val conditionScore: Float?,
    @SerializedName("is_scouted") val isScouted: Boolean = false,
    @SerializedName("streetview_image_path") val streetviewImagePath: String?
)

/**
 * Paginated properties response.
 */
data class PropertiesResponse(
    val properties: List<Property>,
    val total: Int,
    val page: Int,
    @SerializedName("per_page") val perPage: Int,
    @SerializedName("total_pages") val totalPages: Int
)

/**
 * Next property response from /api/scout/next.
 */
data class NextPropertyResponse(
    val property: Property?,
    val remaining: Int
)

/**
 * List (collection) from /api/lists.
 */
data class PropertyList(
    val id: Long,
    val name: String,
    val description: String?,
    @SerializedName("property_count") val propertyCount: Int = 0,
    @SerializedName("created_at") val createdAt: String? = null
)

/**
 * List with properties from /api/lists/{id}.
 */
data class ListWithProperties(
    val id: Long,
    val name: String,
    val description: String?,
    @SerializedName("created_at") val createdAt: String?,
    val properties: List<Property>
)

/**
 * Request to create a new list.
 */
data class CreateListRequest(
    val name: String,
    val description: String? = null
)

/**
 * Request to add properties to a list.
 */
data class AddPropertiesRequest(
    val apns: List<String>
)

/**
 * Scout result submission request.
 */
data class ScoutResultRequest(
    val apn: String,
    @SerializedName("follow_up") val followUp: Boolean,
    val flyered: Boolean,
    val notes: String?,
    val latitude: Double?,
    val longitude: Double?
)

/**
 * Scout result from /api/scout/results.
 */
data class ScoutResult(
    val id: Long,
    val apn: String,
    @SerializedName("follow_up") val followUp: Boolean,
    val flyered: Boolean,
    val notes: String?,
    @SerializedName("scouted_at") val scoutedAt: String?,
    val latitude: Double?,
    val longitude: Double?
)

/**
 * Scout statistics from /api/scout/results/stats.
 */
data class ScoutStats(
    @SerializedName("total_visits") val totalVisits: Int,
    @SerializedName("follow_ups") val followUps: Int,
    val flyered: Int,
    @SerializedName("unique_properties") val uniqueProperties: Int
)

/**
 * Generic API response wrapper.
 */
data class ApiResponse<T>(
    val success: Boolean,
    val data: T? = null,
    val error: String? = null
)

/**
 * Route response from /api/lists/{id}/route.
 */
data class RouteResponse(
    val url: String,
    @SerializedName("property_count") val propertyCount: Int,
    val optimized: Boolean
)

/**
 * GeoJSON response for map markers.
 */
data class GeoJsonResponse(
    val type: String,
    val features: List<Feature>
)

data class Feature(
    val type: String,
    val properties: FeatureProperties,
    val geometry: Geometry?
)

data class FeatureProperties(
    val apn: String,
    val address: String?,
    @SerializedName("has_vpt") val hasVpt: Int?,
    @SerializedName("condition_score") val conditionScore: Float?,
    val city: String?,
    @SerializedName("streetview_image_path") val streetviewImagePath: String?
)

data class Geometry(
    val type: String,
    val coordinates: List<Double>?
)

// ============================================================================
// API Interface
// ============================================================================

/**
 * Retrofit API interface for Flask backend.
 */
interface ScoutApiService {
    
    // -------------------------------------------------------------------------
    // Properties Endpoints
    // -------------------------------------------------------------------------
    
    /**
     * Get paginated list of properties with optional filters.
     */
    @GET("api/properties")
    suspend fun getProperties(
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 50,
        @Query("city") city: String? = null,
        @Query("q") query: String? = null,
        @Query("vpt") vptOnly: String? = null,  // "1" for VPT only
        @Query("scouted") scouted: String? = null,  // "true", "false", or null for all
        @Query("list_id") listId: Long? = null
    ): PropertiesResponse
    
    /**
     * Get map markers in GeoJSON format.
     */
    @GET("api/markers")
    suspend fun getMarkers(): GeoJsonResponse
    
    // -------------------------------------------------------------------------
    // Lists Endpoints
    // -------------------------------------------------------------------------
    
    /**
     * Get all lists.
     */
    @GET("api/lists")
    suspend fun getLists(): List<PropertyList>
    
    /**
     * Create a new list.
     */
    @POST("api/lists")
    suspend fun createList(@Body request: CreateListRequest): PropertyList
    
    /**
     * Get a list with its properties.
     */
    @GET("api/lists/{id}")
    suspend fun getList(@Path("id") listId: Long): ListWithProperties
    
    /**
     * Delete a list.
     */
    @DELETE("api/lists/{id}")
    suspend fun deleteList(@Path("id") listId: Long): ApiResponse<Unit>
    
    /**
     * Add properties to a list.
     */
    @POST("api/lists/{id}/add-properties")
    suspend fun addPropertiesToList(
        @Path("id") listId: Long,
        @Body request: AddPropertiesRequest
    ): ApiResponse<Unit>
    
    /**
     * Remove a property from a list.
     */
    @DELETE("api/lists/{id}/remove-property/{apn}")
    suspend fun removePropertyFromList(
        @Path("id") listId: Long,
        @Path("apn") apn: String
    ): ApiResponse<Unit>
    
    /**
     * Get optimized route URL for a list.
     */
    @GET("api/lists/{id}/route")
    suspend fun getListRoute(@Path("id") listId: Long): RouteResponse
    
    // -------------------------------------------------------------------------
    // Scout Mode Endpoints
    // -------------------------------------------------------------------------
    
    /**
     * Get the nearest unscouted property matching filters.
     */
    @GET("api/scout/next")
    suspend fun getNextProperty(
        @Query("lat") latitude: Double,
        @Query("lng") longitude: Double,
        @Query("city") city: String? = null,
        @Query("vpt") vptOnly: String? = null,
        @Query("list_id") listId: Long? = null,
        @Query("condition_min") conditionMin: Float? = null,
        @Query("condition_max") conditionMax: Float? = null
    ): NextPropertyResponse
    
    /**
     * Submit a scout result.
     */
    @POST("api/scout/results")
    suspend fun submitScoutResult(@Body request: ScoutResultRequest): ApiResponse<Unit>
    
    /**
     * Get scout results, optionally filtered by collection.
     */
    @GET("api/scout/results")
    suspend fun getScoutResults(
        @Query("collection_id") collectionId: Long? = null
    ): List<ScoutResult>
    
    /**
     * Get scouting statistics.
     */
    @GET("api/scout/results/stats")
    suspend fun getScoutStats(): ScoutStats
    
    // -------------------------------------------------------------------------
    // Factory
    // -------------------------------------------------------------------------
    
    companion object {
        fun create(context: Context): ScoutApiService {
            val baseUrl = context.getString(R.string.api_base_url)
            val apiKey = context.getString(R.string.api_key)
            
            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }
            
            // Add API key to all requests
            val authInterceptor = okhttp3.Interceptor { chain ->
                val request = chain.request().newBuilder()
                    .addHeader("X-API-Key", apiKey)
                    .build()
                chain.proceed(request)
            }
            
            val client = OkHttpClient.Builder()
                .addInterceptor(authInterceptor)
                .addInterceptor(logging)
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .build()
            
            val gson = GsonBuilder()
                .setLenient()
                .create()
            
            return Retrofit.Builder()
                .baseUrl("$baseUrl/")
                .client(client)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build()
                .create(ScoutApiService::class.java)
        }
    }
}
