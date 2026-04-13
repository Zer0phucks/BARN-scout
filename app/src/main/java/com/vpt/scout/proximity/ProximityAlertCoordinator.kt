package com.vpt.scout.proximity

class ProximityAlertCoordinator {
    fun evaluate(
        nearest: AlertCandidate?,
        settings: ProximityAlertSettings,
        suppression: AlertSuppressionState
    ): EvaluationResult {
        if (!settings.enabled || nearest == null || nearest.isScouted) {
            return EvaluationResult.noAlert(
                suppression.copy(lastInsideThreshold = false)
            )
        }

        val insideThreshold = nearest.distanceFeet <= settings.thresholdFeet
        val alreadyAlerted = suppression.lastAlertedApn == nearest.apn && suppression.lastInsideThreshold

        return if (insideThreshold && !alreadyAlerted) {
            EvaluationResult.alert(
                alertApn = nearest.apn,
                nextSuppression = AlertSuppressionState(
                    lastAlertedApn = nearest.apn,
                    lastInsideThreshold = true
                )
            )
        } else {
            EvaluationResult.noAlert(
                suppression.copy(
                    lastAlertedApn = if (insideThreshold) nearest.apn else suppression.lastAlertedApn,
                    lastInsideThreshold = insideThreshold
                )
            )
        }
    }
}
