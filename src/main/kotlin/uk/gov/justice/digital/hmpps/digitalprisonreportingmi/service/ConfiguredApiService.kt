package uk.gov.justice.digital.hmpps.digitalprisonreportingmi.service

import jakarta.validation.ValidationException
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.digitalprisonreportingmi.data.ConfiguredApiRepositoryCustom
import uk.gov.justice.digital.hmpps.digitalprisonreportingmi.data.StubbedProductDefinitionRepository
import uk.gov.justice.digital.hmpps.digitalprisonreportingmi.data.model.DataSet
import uk.gov.justice.digital.hmpps.digitalprisonreportingmi.data.model.ParameterType
import uk.gov.justice.digital.hmpps.digitalprisonreportingmi.data.model.ReportField
import java.time.LocalDate
import java.time.format.DateTimeParseException

@Service
class ConfiguredApiService(
  val stubbedProductDefinitionRepository: StubbedProductDefinitionRepository,
  val configuredApiRepository: ConfiguredApiRepositoryCustom,
) {

  companion object {
    const val INVALID_REPORT_ID_MESSAGE = "Invalid report id provided."
    const val INVALID_FILTERS_MESSAGE = "Invalid filters provided."
    const val INVALID_STATIC_OPTIONS_MESSAGE = "Invalid static options provided."
    private const val schemaFieldPrefix = "\$ref:"
  }

  val startSuffix = ".start"
  val endSuffix = ".end"
  fun validateAndFetchData(
    reportId: String,
    dataSetId: String,
    reportVariantId: String,
    filters: Map<String, String>,
    selectedPage: Long,
    pageSize: Long,
    sortColumn: String?,
    sortedAsc: Boolean,
  ): List<Map<String, Any>> {
    val dataSet = stubbedProductDefinitionRepository.getDataSet(reportId, dataSetId)
    validateFilters(reportId, reportVariantId, filters, dataSet)
    val (rangeFilters, filtersExcludingRange) = filters.entries.partition { (k, _) -> k.endsWith(startSuffix) || k.endsWith(endSuffix) }
    val validatedSortColumn = validateSortColumnOrGetDefault(sortColumn, reportId, dataSet, reportVariantId)
    return configuredApiRepository
      .executeQuery(
        dataSet.query,
        rangeFilters.associate(transformMapEntryToPair()),
        filtersExcludingRange.associate(transformMapEntryToPair()),
        selectedPage,
        pageSize,
        validatedSortColumn,
        sortedAsc,
      )
  }

  fun calculateDefaultSortColumn(reportId: String, dataSetId: String, reportVariantId: String): String {
    return getReportVariantSpecFields(reportId, reportVariantId)
      ?.first { it.defaultSortColumn }
      ?.schemaField
      ?.removePrefix(schemaFieldPrefix)
      ?: throw ValidationException("Could not find default sort column for reportId: $reportId, dataSetId: $dataSetId, reportVariantId: $reportVariantId")
  }

  private fun validateSortColumnOrGetDefault(sortColumn: String?, reportId: String, dataSet: DataSet, reportVariantId: String): String {
    return sortColumn?.let {
      dataSet.schema.field.filter { schemaField -> schemaField.name == sortColumn }
        .ifEmpty { throw ValidationException("Invalid sortColumn provided: $sortColumn") }
        .first().name
    } ?: calculateDefaultSortColumn(reportId, dataSet.id, reportVariantId)
  }

  private fun validateFilters(reportId: String, reportVariantId: String, filters: Map<String, String>, dataSet: DataSet) {
    filters.ifEmpty { return }
    val reportFieldsWithFiltersOnly = getReportFieldsWithFiltersOnly(reportId, reportVariantId)
    validateFiltersMatchSchemaName(reportFieldsWithFiltersOnly, filters)
    validateFilterTypes(filters, dataSet)
    val filtersWithStaticOptionsOnly = getFiltersWithStaticOptionsOnly(filters, reportFieldsWithFiltersOnly)
    filtersWithStaticOptionsOnly.ifEmpty { return }
    validateStaticOptions(filtersWithStaticOptionsOnly, reportFieldsWithFiltersOnly)
  }

  private fun getReportFieldsWithFiltersOnly(reportId: String, reportVariantId: String) =
    getReportVariantSpecFields(reportId, reportVariantId)
      ?.filter { it.filter?.let { true } ?: false }.orEmpty()

  private fun getFiltersWithStaticOptionsOnly(filters: Map<String, String>, reportFieldsWithFiltersOnly: List<ReportField>) =
    filters.entries.filter { filterEntry ->
      reportFieldsWithFiltersOnly.any { reportField ->
        reportField.schemaField.removePrefix(schemaFieldPrefix) == filterEntry.key
      }
    }

  private fun validateFiltersMatchSchemaName(reportFieldsWithFilters: List<ReportField>?, filters: Map<String, String>) {
    (
      reportFieldsWithFilters
        ?.filter { truncateRangeFilters(filters).containsKey(it.schemaField.removePrefix(schemaFieldPrefix)) }
        ?.takeIf { it.size == truncateRangeFilters(filters).size }
        ?.ifEmpty { throw ValidationException(INVALID_FILTERS_MESSAGE) }
        ?: throw ValidationException(INVALID_FILTERS_MESSAGE)
      )
  }

  private fun validateStaticOptions(filtersWithStaticOptionsOnly: List<Map.Entry<String, String>>, reportFieldsWithFiltersOnly: List<ReportField>) {
    filtersWithStaticOptionsOnly.forEach { filterWithStaticOptionsOnlyEntry ->
      reportFieldsWithFiltersOnly.first { reportField ->
        filterWithStaticOptionsOnlyEntry.key == reportField.schemaField.removePrefix(schemaFieldPrefix)
      }
        .filter
        ?.staticOptions
        ?.filter { staticOption ->
          // Case Insensitive comparison for static option filter values. This is in line with the same lenience in the ConfiguredApiRepository.
          staticOption.name.lowercase() == filterWithStaticOptionsOnlyEntry.value.lowercase()
        }
        .orEmpty()
        .ifEmpty {
          throw ValidationException(INVALID_STATIC_OPTIONS_MESSAGE)
        }
    }
  }

  private fun validateFilterTypes(filters: Map<String, String>, dataSet: DataSet) {
    truncateRangeFilters(filters)
      .forEach { filter ->
        val schemaField = dataSet.schema.field.first { it.name == filter.key }
        if (schemaField.type == ParameterType.Long) {
          try {
            filter.value.toLong()
          } catch (e: NumberFormatException) {
            throw ValidationException("Invalid value ${filter.value} for filter ${filter.key}. Cannot be parsed as a number.")
          }
        } else if (schemaField.type == ParameterType.Date) {
          try {
            LocalDate.parse(filter.value)
          } catch (e: DateTimeParseException) {
            throw ValidationException("Invalid value ${filter.value} for filter ${filter.key}. Cannot be parsed as a date.")
          }
        }
      }
  }

  private fun getReportVariantSpecFields(reportId: String, reportVariantId: String) =
    stubbedProductDefinitionRepository.getProductDefinitions()
      .filter { it.id == reportId }
      .flatMap { it.report.filter { report -> report.id == reportVariantId } }
      .ifEmpty { throw ValidationException(INVALID_REPORT_ID_MESSAGE) }
      .first()
      .specification
      ?.field

  private fun truncateRangeFilters(filters: Map<String, String>): Map<String, String> {
    return filters.entries
      .associate { (k, v) -> truncateBasedOnSuffix(k, v) }
  }

  private fun truncateBasedOnSuffix(k: String, v: String): Pair<String, String> {
    return if (k.endsWith(startSuffix)) {
      k.substring(0, k.length - startSuffix.length) to v
    } else if (k.endsWith(endSuffix)) {
      k.substring(0, k.length - endSuffix.length) to v
    } else {
      k to v
    }
  }

  private fun transformMapEntryToPair(): (Map.Entry<String, String>) -> Pair<String, String> {
    return { (k, v) -> k to v }
  }
}
