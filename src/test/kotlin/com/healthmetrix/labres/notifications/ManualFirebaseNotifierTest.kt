package com.healthmetrix.labres.notifications

import com.healthmetrix.labres.JacksonConfig
import com.healthmetrix.labres.secrets.AwsSecrets
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

@SpringBootTest(
    classes = [NotificationsConfig::class, AwsSecrets::class, JacksonConfig::class],
    properties = [
        "labres.stage=dev",
        "logging.level.com.healthmetrix=DEBUG"
    ]
)
@ActiveProfiles("secrets,notify")
@Disabled
internal class ManualFirebaseNotifierTest {

    @Value("\${fcm.token}")
    private lateinit var testToken: String

    @Autowired
    private lateinit var firebaseNotifier: Notifier<Notification.FcmNotification>

    @Test
    fun `send test notification`() {
        val notification = Notification.FcmNotification(testToken)
        firebaseNotifier.send(notification)
    }
}
