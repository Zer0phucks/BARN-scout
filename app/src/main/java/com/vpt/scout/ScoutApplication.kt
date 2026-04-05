package com.vpt.scout

import android.app.Application
import com.vpt.scout.proximity.ProximityAlertCoordinator
import com.vpt.scout.proximity.ProximityAlertPreferences
import com.vpt.scout.proximity.ProximityAlertRepository
import com.vpt.scout.proximity.ProximityNotificationManager

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

    val proximityAlertPreferences: ProximityAlertPreferences by lazy {
        ProximityAlertPreferences(context)
    }

    val proximityAlertCoordinator: ProximityAlertCoordinator by lazy {
        ProximityAlertCoordinator()
    }

    val proximityAlertRepository: ProximityAlertRepository by lazy {
        ProximityAlertRepository(propertyRepository)
    }

    val proximityNotificationManager: ProximityNotificationManager by lazy {
        ProximityNotificationManager(context)
    }
    
    // Legacy - for backward compatibility
    val collectionRepository: CollectionRepository by lazy {
        CollectionRepository(database.collectionDao(), database.scoutResultDao(), scannerService)
    }
}
