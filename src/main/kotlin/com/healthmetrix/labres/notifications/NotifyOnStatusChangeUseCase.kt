package com.healthmetrix.labres.notifications

import com.healthmetrix.labres.logger
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class NotifyOnStatusChangeUseCase(
    private val fcmNotifier: Notifier<Notification.FcmNotification>,
    private val httpNotifier: Notifier<Notification.HttpNotification>
) {
    operator fun invoke(orderId: UUID, target: String?): Boolean {
        if (target == null) {
            logger.warn("No notification url for $orderId")
            return false
        }

        logger.debug("Sending notification for id $orderId to $target")

        return when (val notification = Notification.from(target)) {
            is Notification.HttpNotification -> httpNotifier.send(notification)
            is Notification.FcmNotification -> fcmNotifier.send(notification)
            null -> {
                logger.info("Notification type for id $orderId not supported: $target")
                false
            }
        }
    }
}
