package com.vpt.scout.proximity

data class ProximityAlertSettings(
    val enabled: Boolean = false,
    val thresholdFeet: Int = 500
)

data class AlertSuppressionState(
    val lastAlertedApn: String? = null,
    val lastInsideThreshold: Boolean = false
)

data class AlertCandidate(
    val apn: String,
    val distanceFeet: Float,
    val isScouted: Boolean
)

data class EvaluationResult(
    val shouldNotify: Boolean,
    val alertApn: String? = null,
    val nextSuppression: AlertSuppressionState = AlertSuppressionState()
) {
    companion object {
        fun alert(
            alertApn: String,
            nextSuppression: AlertSuppressionState
        ) = EvaluationResult(
            shouldNotify = true,
            alertApn = alertApn,
            nextSuppression = nextSuppression
        )

        fun noAlert(nextSuppression: AlertSuppressionState) = EvaluationResult(
            shouldNotify = false,
            nextSuppression = nextSuppression
        )
    }
}
