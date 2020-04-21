package com.healthmetrix.labres.lab

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ExtractObxResultUseCaseTest {
    private val underTest = ExtractObxResultUseCase()
    private val obxSegment =
        "OBX|3|ST|21300^2019-nCoronav.-RNA Sonst (PCR)|0061749799|%s|||N|||S|||20200406101220|Extern|||||||||Extern"

    @Test
    fun `it returns POSITIVE when result is positive`() {
        val obx = obxSegment.format("Positiv")
        assertThat(underTest(obx, "labId")?.result).isEqualTo(Result.POSITIVE)
    }

    @Test
    fun `it returns NEGATIVE when result is negative`() {
        val obx = obxSegment.format("Nicht nachweisbar")
        assertThat(underTest(obx, "labId")?.result).isEqualTo(Result.NEGATIVE)
    }

    @Test
    fun `it returns WEAK_POSITIVE when result is weak positive`() {
        val obx = obxSegment.format("Schwach positiv")
        assertThat(underTest(obx, "labId")?.result).isEqualTo(Result.WEAK_POSITIVE)
    }

    @Test
    fun `it returns INVALID when result is invalid`() {
        val obx = obxSegment.format("Prozessfehler")
        assertThat(underTest(obx, "labId")?.result).isEqualTo(Result.INVALID)
    }

    @Test
    fun `it returns IN_PROGRESS when result is InArbeit`() {
        val obx = obxSegment.format("InArbeit")
        assertThat(underTest(obx, "labId")?.result).isEqualTo(Result.IN_PROGRESS)
    }

    @Test
    fun `it returns null when the result is not accounted for`() {
        val obx = obxSegment.format("Not a value")
        assertThat(underTest(obx, "labId")?.result).isNull()
    }
}
