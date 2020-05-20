package com.healthmetrix.labres.notifications

private const val HTTP_SCHEME = "http://"
private const val HTTPS_SCHEME = "https://"
private const val LAB_RES_FCM_SCHEME = "fcm://labres@"

sealed class Notification {
    data class HttpNotification(val url: String) : Notification()
    data class FcmNotification(val token: String) : Notification()

    companion object {
        fun from(notificationUrl: String) = when {
            notificationUrl.isHttp() -> HttpNotification(notificationUrl)
            notificationUrl.isLabResFcmUrl() -> FcmNotification(notificationUrl.asFcmToken())
            else -> null
        }

        private fun String.isHttp() =
            this.startsWith(HTTP_SCHEME, true) || this.startsWith(HTTPS_SCHEME, true)

        private fun String.isLabResFcmUrl() = this.startsWith(LAB_RES_FCM_SCHEME)

        private fun String.asFcmToken() = this.removePrefix(LAB_RES_FCM_SCHEME)
    }
}
