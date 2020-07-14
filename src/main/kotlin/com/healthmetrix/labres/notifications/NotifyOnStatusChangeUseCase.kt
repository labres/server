package com.healthmetrix.labres.notifications

import com.healthmetrix.labres.logger
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class NotifyOnStatusChangeUseCase(
    private val fcmNotifier: Notifier<Notification.FcmNotification>,
    private val httpNotifier: Notifier<Notification.HttpNotification>,
    private val metrics: NotificationMetrics
) {
    operator fun invoke(orderId: UUID, targets: List<String>): Boolean {
        if (targets.isEmpty()) {
            logger.warn("No notification url for $orderId")
            metrics.countTargetEmpty()
            return false
        }
        logger.debug("Sending notification for id $orderId to $targets")

        return targets
            .distinct()
            .map { sendNotification(orderId, it) }
            .reduce(Boolean::and)
    }

    private fun sendNotification(orderId: UUID, target: String) =
        when (val notification = Notification.from(target)) {
            is Notification.HttpNotification -> httpNotifier.send(notification)
            is Notification.FcmNotification -> fcmNotifier.send(notification)
            null -> {
                logger.warn("Notification type for id $orderId not supported: $target")
                metrics.countTargetNotSupported()
                false
            }
        }
}
