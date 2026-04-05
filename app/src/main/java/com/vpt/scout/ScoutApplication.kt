package com.vpt.scout

import android.app.Application

class ScoutApplication : Application() {
    
    lateinit var container: AppContainer
    
    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}

/**
 * Simple dependency container for the app.
 * Provides singleton instances of repositories and database.
 */
class AppContainer(private val context: Application) {
    val authManager: SupabaseAuthManager by lazy {
        SupabaseAuthManager(context)
    }
    
    val database: ScoutDatabase by lazy {
        ScoutDatabase.getDatabase(context)
    }
    
    val scannerService: ScannerDataService by lazy {
        SupabaseScannerService(
            baseUrl = authManager.projectUrl,
            anonKey = authManager.anonKey,
            accessTokenProvider = { authManager.getAccessToken() },
            authManager = authManager
        )
    }
    
    val propertyRepository: PropertyRepository by lazy {
        PropertyRepository(database.propertyDao(), scannerService)
    }
    
    val listRepository: ListRepository by lazy {
        ListRepository(scannerService)
    }
    
    val scoutRepository: ScoutRepository by lazy {
        ScoutRepository(scannerService)
    }
    
    // Legacy - for backward compatibility
    val collectionRepository: CollectionRepository by lazy {
        CollectionRepository(database.collectionDao(), database.scoutResultDao(), scannerService)
    }
}
