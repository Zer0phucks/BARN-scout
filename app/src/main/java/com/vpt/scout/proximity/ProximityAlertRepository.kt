package com.vpt.scout.proximity

import com.vpt.scout.Property
import com.vpt.scout.PropertyRepository

class ProximityAlertRepository(
    private val propertyRepository: PropertyRepository
) {
    suspend fun getNearestUnscoutedProperty(
        latitude: Double,
        longitude: Double
    ): Property? {
        return propertyRepository.getNextProperty(
            latitude = latitude,
            longitude = longitude,
            city = null,
            vptOnly = false,
            listId = null
        ).property?.takeUnless { it.isScouted }
    }

    suspend fun getPropertyForAlert(apn: String): Property? {
        return propertyRepository.getPropertyByApn(apn)
    }
}
