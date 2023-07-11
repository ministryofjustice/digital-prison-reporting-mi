package uk.gov.justice.digital.hmpps.digitalprisonreportingmi.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.constraints.Min
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.digitalprisonreportingmi.model.Count
import uk.gov.justice.digital.hmpps.digitalprisonreportingmi.model.ExternalMovement
import uk.gov.justice.digital.hmpps.digitalprisonreportingmi.model.ExternalMovementFilter
import uk.gov.justice.digital.hmpps.digitalprisonreportingmi.model.ExternalMovementFilter.DIRECTION
import uk.gov.justice.digital.hmpps.digitalprisonreportingmi.service.ExternalMovementService

@Validated
@RestController
@Tag(name = "External Movements API")
class ExternalMovementsController(val externalMovementService: ExternalMovementService) {

  @GetMapping("/external-movements/count")
  @Operation(
    description = "Gets a count of external movements (mocked)",
    security = [ SecurityRequirement(name = "bearer-jwt") ],
  )
  fun stubbedCount(
    @RequestParam direction: String?,
  ): Count {
    return externalMovementService.count(createFilterMap(direction))
  }

  @GetMapping("/external-movements")
  @Operation(
    description = "Gets a list of external movements (mocked)",
    security = [ SecurityRequirement(name = "bearer-jwt") ],
  )
  fun stubbedExternalMovements(
    @RequestParam("selectedPage", defaultValue = "1")
    @Min(1)
    selectedPage: Long,
    @RequestParam("pageSize", defaultValue = "10")
    @Min(1)
    pageSize: Long,
    @RequestParam("sortColumn", defaultValue = "date") sortColumn: String,
    @RequestParam("sortedAsc", defaultValue = "false") sortedAsc: Boolean,
    @RequestParam direction: String?,
  ): List<ExternalMovement> {
    return externalMovementService.list(
      selectedPage = selectedPage,
      pageSize = pageSize,
      sortColumn = sortColumn,
      sortedAsc = sortedAsc,
      filters = createFilterMap(direction),
    )
  }

  private fun createFilterMap(direction: String?): Map<ExternalMovementFilter, String> =
    buildMap {
      if (!direction.isNullOrBlank()) {
        put(DIRECTION, direction)
      }
    }
}
