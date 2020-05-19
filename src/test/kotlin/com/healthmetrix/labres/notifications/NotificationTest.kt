package com.healthmetrix.labres.notifications

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class NotificationTest {

    @Test
    fun `it should return HttpNotification for http URL`() {
        val target = "http://something"

        val result = Notification.from(target)

        assertThat(result).isInstanceOf(Notification.HttpNotification::class.java)
    }

    @Test
    fun `it should set the target as url on the HttpNotification`() {
        val target = "http://something"

        val result = Notification.from(target) as Notification.HttpNotification

        assertThat(result.url).isEqualTo(target)
    }

    @Test
    fun `it should return HttpNotification for https URL`() {
        val target = "https://something"

        val result = Notification.from(target)

        assertThat(result).isInstanceOf(Notification.HttpNotification::class.java)
    }

    @Test
    fun `it should return FcmNotification for http URL`() {
        val target = "fcm://labres@something"

        val result = Notification.from(target)

        assertThat(result).isInstanceOf(Notification.FcmNotification::class.java)
    }

    @Test
    fun `it should set the token on the FcmNotification`() {
        val token = "something"
        val target = "fcm://labres@$token"

        val result = Notification.from(target) as Notification.FcmNotification

        assertThat(result.token).isEqualTo(token)
    }

    @Test
    fun `it should return null for any other target`() {
        val target = "something"

        val result = Notification.from(target)

        assertThat(result).isNull()
    }
}
