package com.healthmetrix.labres.notifications

import com.healthmetrix.labres.JacksonConfig
import com.healthmetrix.labres.secrets.AwsSecrets
import java.util.UUID
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

@SpringBootTest(
    classes = [
        NotifyOnStatusChangeUseCase::class,
        NotificationsConfig::class,
        AwsSecrets::class,
        JacksonConfig::class
    ],
    properties = [
        "labres.stage=dev",
        "logging.level.com.healthmetrix=DEBUG"
    ]
)
@ActiveProfiles("secrets,notify")
@Disabled
internal class ManualNotifierTest {

    @Value("\${notification.target}")
    private lateinit var target: String

    @Autowired
    private lateinit var notifyOnStatusChangeUseCase: NotifyOnStatusChangeUseCase

    /*
        To test sending notifications manually, you need the following prerequisites:
        - AWS credentials having the permission to check out the necessary secrets for FCM and basic auth locally set up
        - env NOTIFICATION_TARGET set to where ever you want to send a notification to
        - Comment out the @Disabled annotation on the test

        Then you can run this test and invoke actual notifications being sent
     */
    @Test
    fun `send test notification`() {
        notifyOnStatusChangeUseCase.invoke(UUID.randomUUID(), target)
    }
}
