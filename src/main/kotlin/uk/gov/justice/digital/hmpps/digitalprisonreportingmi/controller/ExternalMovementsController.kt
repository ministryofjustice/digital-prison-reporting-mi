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
import uk.gov.justice.digital.hmpps.digitalprisonreportingmi.model.ExternalMovementFilter.END_DATE
import uk.gov.justice.digital.hmpps.digitalprisonreportingmi.model.ExternalMovementFilter.START_DATE
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
    @RequestParam startDate: String?,
    @RequestParam endDate: String?,
  ): Count {
    return externalMovementService.count(createFilterMap(direction, startDate, endDate))
  }

  @GetMapping("/external-movements")
  @Operation(
    description = "Gets a list of external movements (mocked)",
    security = [ SecurityRequirement(name = "bearer-jwt") ],
  )
  fun stubbedExternalMovements(
    @RequestParam(defaultValue = "1")
    @Min(1)
    selectedPage: Long,
    @RequestParam(defaultValue = "10")
    @Min(1)
    pageSize: Long,
    @RequestParam(defaultValue = "date") sortColumn: String,
    @RequestParam(defaultValue = "false") sortedAsc: Boolean,
    @RequestParam direction: String?,
    @RequestParam startDate: String?,
    @RequestParam endDate: String?,
  ): List<ExternalMovement> {
    return externalMovementService.list(
      selectedPage = selectedPage,
      pageSize = pageSize,
      sortColumn = sortColumn,
      sortedAsc = sortedAsc,
      filters = createFilterMap(direction, startDate, endDate),
    )
  }

  private fun createFilterMap(direction: String?, startDate: String?, endDate: String?): Map<ExternalMovementFilter, String> =
    buildMap {
      direction?.ifEmpty { null }?.let { put(DIRECTION, it) }
      startDate?.ifEmpty { null }?.let { put(START_DATE, it) }
      endDate?.ifEmpty { null }?.let { put(END_DATE, it) }
    }
}
