package com.healthmetrix.labres.lab

class ExtractObxResultUseCaseTest {
    private val underTest = ExtractObxResultUseCase()
    private val labId = "labId"
    private val issuerId = null
    private val obxSegment =
        "OBX|3|ST|21300^2019-nCoronav.-RNA Sonst (PCR)|0061749799|%s|||N|||S|||20200406101220|Extern|||||||||Extern"

    // TODO: find out how to correctly parse the relating order number from OBX
    // @Test
    // fun `it returns POSITIVE when result is positive`() {
    //     val obx = obxSegment.format("Positiv")
    //     assertThat(underTest(obx, labId, issuerId)?.result).isEqualTo(Result.POSITIVE)
    // }
    //
    // @Test
    // fun `it also works for preissued order numbers`() {
    //     val obx = obxSegment.format("Positiv")
    //     assertThat(underTest(obx, labId, "issuerId")?.result).isEqualTo(Result.POSITIVE)
    // }
    //
    // @Test
    // fun `it returns NEGATIVE when result is negative`() {
    //     val obx = obxSegment.format("Nicht nachweisbar")
    //     assertThat(underTest(obx, labId, issuerId)?.result).isEqualTo(Result.NEGATIVE)
    // }
    //
    // @Test
    // fun `it returns WEAK_POSITIVE when result is weak positive`() {
    //     val obx = obxSegment.format("Schwach positiv")
    //     assertThat(underTest(obx, labId, issuerId)?.result).isEqualTo(Result.WEAK_POSITIVE)
    // }
    //
    // @Test
    // fun `it returns INVALID when result is invalid`() {
    //     val obx = obxSegment.format("Prozessfehler")
    //     assertThat(underTest(obx, labId, issuerId)?.result).isEqualTo(Result.INVALID)
    // }
    //
    // @Test
    // fun `it returns IN_PROGRESS when result is InArbeit`() {
    //     val obx = obxSegment.format("InArbeit")
    //     assertThat(underTest(obx, labId, issuerId)?.result).isEqualTo(Result.IN_PROGRESS)
    // }
    //
    // @Test
    // fun `it returns null when the result is not accounted for`() {
    //     val obx = obxSegment.format("Not a value")
    //     assertThat(underTest(obx, labId, issuerId)?.result).isNull()
    // }
}
