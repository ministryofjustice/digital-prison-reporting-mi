package uk.gov.justice.digital.hmpps.digitalprisonreportingmi.integration

import org.junit.jupiter.api.Test

class ExternalMovementsIntegrationTest : IntegrationTestBase() {

  @Test
  fun `External movements count returns stubbed value`() {
    webTestClient.get()
      .uri("/external-movements/count")
      .headers(setAuthorisation(roles = listOf(USER_AUTHORITY)))
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("count").isEqualTo("501")
  }
}
