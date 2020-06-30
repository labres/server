package com.healthmetrix.labres.notifications

import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyAll
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID

internal class NotifyOnStatusChangeUseCaseTest {

    private val fcmNotifier: Notifier<Notification.FcmNotification> = mockk()
    private val httpNotifier: Notifier<Notification.HttpNotification> = mockk()

    private val underTest = NotifyOnStatusChangeUseCase(fcmNotifier, httpNotifier)

    @BeforeEach
    internal fun setUp() {
        clearMocks(fcmNotifier, httpNotifier)
        every { httpNotifier.send(any()) } returns true
        every { fcmNotifier.send(any()) } returns true
    }

    @Test
    fun `it should send http notification`() {
        underTest(UUID.randomUUID(), listOf("https://callme.test"))

        verify(exactly = 1) { httpNotifier.send(any()) }
    }

    @Test
    fun `it should send http notifications to multiple targets`() {
        val notificationUrls = listOf("https://callme.test", "https://alsome.test")

        underTest(UUID.randomUUID(), notificationUrls)

        verify(exactly = 2) { httpNotifier.send(match { notificationUrls.contains(it.url) }) }
    }

    @Test
    fun `it should send fcm notification`() {
        underTest(UUID.randomUUID(), listOf("fcm://labres@test_token"))

        verify(exactly = 1) { fcmNotifier.send(any()) }
    }

    @Test
    fun `it should send fcm notifications to multiple targets`() {
        val notificationTokens = listOf("test_token", "test_token2")

        underTest(UUID.randomUUID(), notificationTokens.map(this::addFcmPrefix))

        verify(exactly = 2) { fcmNotifier.send(match { notificationTokens.contains(it.token) }) }
    }

    @Test
    fun `it should send http and fcm notifications to multiple targets`() {
        val httpUrl = "https://callme.test"
        val fcmToken1 = "test_token"
        val fcmToken2 = "test_token2"
        val notificationTokens = listOf(addFcmPrefix(fcmToken1), httpUrl, addFcmPrefix(fcmToken2))

        underTest(UUID.randomUUID(), notificationTokens)

        verifyAll {
            fcmNotifier.send(match { it.token == fcmToken1 })
            fcmNotifier.send(match { it.token == fcmToken2 })
            httpNotifier.send(match { it.url == httpUrl })
        }
    }

    @Test
    fun `it should return true when http notification was sent successfully`() {
        val result = underTest(UUID.randomUUID(), listOf("https://callme.test"))

        assertThat(result).isTrue()
    }

    @Test
    fun `it should return true when fcm notification was sent successfully`() {
        val result = underTest(UUID.randomUUID(), listOf("fcm://labres@test_token"))

        assertThat(result).isTrue()
    }

    @Test
    fun `it should return false when http notification was not sent successfully`() {
        every { httpNotifier.send(any()) } returns false

        val result = underTest(UUID.randomUUID(), listOf("https://callme.test"))

        assertThat(result).isFalse()
    }

    @Test
    fun `it should return false when fcm notification was not sent successfully`() {
        every { fcmNotifier.send(any()) } returns false

        val result = underTest(UUID.randomUUID(), listOf("fcm://labres@test_token"))

        assertThat(result).isFalse()
    }

    @Test
    fun `it should return false if no target was provided`() {
        val result = underTest(UUID.randomUUID(), emptyList())

        assertThat(result).isFalse()
    }

    @Test
    fun `it should not send any notification if no target was provided`() {
        underTest(UUID.randomUUID(), emptyList())

        verify(exactly = 0) {
            fcmNotifier.send(any())
            httpNotifier.send(any())
        }
    }

    @Test
    fun `it should return false if notification type could not be determined for the target`() {
        val result = underTest(UUID.randomUUID(), listOf("wrong"))

        assertThat(result).isFalse()
    }

    @Test
    fun `it should not send any notification if notification type could not be determined for the target`() {
        underTest(UUID.randomUUID(), listOf("wrong"))

        verify(exactly = 0) {
            fcmNotifier.send(any())
            httpNotifier.send(any())
        }
    }

    @Test
    fun `it should return true when all http and fcm notifications can be sent successfully`() {
        val httpUrl = "https://callme.test"
        val fcmToken1 = "test_token"
        val fcmToken2 = "test_token2"
        val notificationTokens = listOf(addFcmPrefix(fcmToken1), httpUrl, addFcmPrefix(fcmToken2))

        val res = underTest(UUID.randomUUID(), notificationTokens)

        assertThat(res).isTrue()
    }

    @Test
    fun `it should return false when any http and fcm notifications fails to be sent successfully`() {
        val httpUrl = "https://callme.test"
        val fcmToken1 = "test_token"
        val fcmToken2 = "test_token2"
        val notificationTokens = listOf(addFcmPrefix(fcmToken1), httpUrl, addFcmPrefix(fcmToken2))

        every { fcmNotifier.send(match { it.token == fcmToken2 }) } returns false

        val res = underTest(UUID.randomUUID(), notificationTokens)

        assertThat(res).isFalse()
    }

    private fun addFcmPrefix(token: String) = "fcm://labres@$token"
}
