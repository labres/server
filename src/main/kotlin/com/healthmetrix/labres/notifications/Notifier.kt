package com.healthmetrix.labres.notifications

import com.healthmetrix.labres.logger

interface Notifier<T : Notification> {
    fun send(notification: T): Boolean
}

class LoggingNotifier<T : Notification> : Notifier<T> {
    override fun send(notification: T): Boolean {
        val target = when (notification) {
            is Notification.HttpNotification -> notification.url
            is Notification.FcmNotification -> notification.token
            else -> "not_supported"
        }

        logger.info("Send notification to $target")
        return true
    }
}
