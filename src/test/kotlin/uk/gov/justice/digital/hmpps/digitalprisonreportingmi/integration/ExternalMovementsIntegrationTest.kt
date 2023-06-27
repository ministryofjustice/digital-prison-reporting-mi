package uk.gov.justice.digital.hmpps.digitalprisonreportingmi.integration

import org.junit.jupiter.api.Test
import org.springframework.web.util.UriBuilder

class ExternalMovementsIntegrationTest : IntegrationTestBase() {

  @Test
  fun `External movements count returns stubbed value`() {
    webTestClient.get()
      .uri("/external-movements/count")
      .headers(setAuthorisation(roles = listOf(authorisedRole)))
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("count").isEqualTo("500")
  }

  @Test
  fun `External movements returns stubbed value`() {
    webTestClient.get()
      .uri { uriBuilder: UriBuilder ->
        uriBuilder
          .path("/external-movements")
          .queryParam("selectedPage", 1)
          .queryParam("pageSize", 3)
          .queryParam("sortColumn", "date")
          .queryParam("sortedAsc", false)
          .build()
      }
      .headers(setAuthorisation(roles = listOf(authorisedRole)))
      .exchange()
      .expectStatus()
      .isOk()
      .expectBody()
      .json(
        """[
        {"prisonNumber":"Q966ABC","date":"2023-05-20","time":"14:00:00","from":"Isle of Wight","to":"Northumberland","direction":"In","type":"Transfer","reason":"Transfer In from Other Establishment"},
        {"prisonNumber":"Z966YYY","date":"2023-05-01","time":"15:19:00","from":"Cardiff","to":"Maidstone","direction":"Out","type":"Transfer","reason":"Transfer Out to Other Establishment"},
        {"prisonNumber":"A966ZZZ","date":"2023-04-30","time":"13:19:00","from":"Wakefield","to":"Dartmoor","direction":"In","type":"Transfer","reason":"Transfer In from Other Establishment"}
      ]       
      """,
      )
  }

  @Test
  fun `External movements call without query params defaults to preset query params`() {
    webTestClient.get()
      .uri { uriBuilder: UriBuilder ->
        uriBuilder
          .path("/external-movements")
          .build()
      }
      .headers(setAuthorisation(roles = listOf(authorisedRole)))
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .json(
        """
      [
        {"prisonNumber":"Q966ABC","date":"2023-05-20","time":"14:00:00","from":"Isle of Wight","to":"Northumberland","direction":"In","type":"Transfer","reason":"Transfer In from Other Establishment"},
        {"prisonNumber":"Z966YYY","date":"2023-05-01","time":"15:19:00","from":"Cardiff","to":"Maidstone","direction":"Out","type":"Transfer","reason":"Transfer Out to Other Establishment"},
        {"prisonNumber":"A966ZZZ","date":"2023-04-30","time":"13:19:00","from":"Wakefield","to":"Dartmoor","direction":"In","type":"Transfer","reason":"Transfer In from Other Establishment"},
        {"prisonNumber":"Q9660WX","date": "2023-04-25","time":"12:19:00","from":"Elmley","to":"Pentonville","direction":"In","type":"Transfer","reason":"Transfer In from Other Establishment"},
        {"prisonNumber":"N9980PJ","date": "2023-01-31","time":"03:01:00","from":"Ranby","to":"Kirkham","direction":"In","type":"Admission","reason":"Unconvicted Remand"}
      ]
      """,
      )
  }

  @Test
  fun `External movements returns 400 for invalid selectedPage query param`() {
    requestWithQueryAndAssert400("selectedPage", 0)
  }

  @Test
  fun `External movements returns 400 for invalid pageSize query param`() {
    requestWithQueryAndAssert400("pageSize", 0)
  }

  @Test
  fun `External movements returns 400 for invalid (wrong type) pageSize query param`() {
    requestWithQueryAndAssert400("pageSize", "a")
  }

  @Test
  fun `External movements returns 400 for invalid sortColumn query param`() {
    requestWithQueryAndAssert400("sortColumn", "nonExistentColumn")
  }

  @Test
  fun `External movements returns 400 for invalid sortedAsc query param`() {
    requestWithQueryAndAssert400("sortedAsc", "abc")
  }
  private fun requestWithQueryAndAssert400(paramName: String, paramValue: Any) {
    webTestClient.get()
      .uri { uriBuilder: UriBuilder ->
        uriBuilder
          .path("/external-movements")
          .queryParam(paramName, paramValue)
          .build()
      }
      .headers(setAuthorisation(roles = listOf(authorisedRole)))
      .exchange()
      .expectStatus()
      .isBadRequest
  }
}
