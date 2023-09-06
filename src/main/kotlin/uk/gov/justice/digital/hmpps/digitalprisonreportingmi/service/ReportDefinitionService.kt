package uk.gov.justice.digital.hmpps.digitalprisonreportingmi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.digitalprisonreportingmi.controller.model.FieldDefinition
import uk.gov.justice.digital.hmpps.digitalprisonreportingmi.controller.model.FieldType
import uk.gov.justice.digital.hmpps.digitalprisonreportingmi.controller.model.FilterDefinition
import uk.gov.justice.digital.hmpps.digitalprisonreportingmi.controller.model.FilterOption
import uk.gov.justice.digital.hmpps.digitalprisonreportingmi.controller.model.FilterType
import uk.gov.justice.digital.hmpps.digitalprisonreportingmi.controller.model.RenderMethod
import uk.gov.justice.digital.hmpps.digitalprisonreportingmi.controller.model.ReportDefinition
import uk.gov.justice.digital.hmpps.digitalprisonreportingmi.controller.model.Specification
import uk.gov.justice.digital.hmpps.digitalprisonreportingmi.controller.model.VariantDefinition
import uk.gov.justice.digital.hmpps.digitalprisonreportingmi.controller.model.WordWrap
import uk.gov.justice.digital.hmpps.digitalprisonreportingmi.data.ProductDefinitionRepository
import uk.gov.justice.digital.hmpps.digitalprisonreportingmi.data.model.DataSet
import uk.gov.justice.digital.hmpps.digitalprisonreportingmi.data.model.ProductDefinition
import uk.gov.justice.digital.hmpps.digitalprisonreportingmi.data.model.Report
import uk.gov.justice.digital.hmpps.digitalprisonreportingmi.data.model.ReportField
import uk.gov.justice.digital.hmpps.digitalprisonreportingmi.data.model.SchemaField
import uk.gov.justice.digital.hmpps.digitalprisonreportingmi.service.DateDifferenceMagnitude.Days
import uk.gov.justice.digital.hmpps.digitalprisonreportingmi.service.DateDifferenceMagnitude.Months
import uk.gov.justice.digital.hmpps.digitalprisonreportingmi.service.DateDifferenceMagnitude.Weeks
import uk.gov.justice.digital.hmpps.digitalprisonreportingmi.service.DateDifferenceMagnitude.Years
import java.time.LocalDate
import java.time.format.DateTimeFormatter.ISO_LOCAL_DATE

val todayRegex: Regex = Regex("today\\(\\)")
val dateRegex: Regex = Regex("today\\((-?\\d+), ?(.[a-z]+)\\)")

@Service
class ReportDefinitionService(val productDefinitionRepository: ProductDefinitionRepository) {

  fun getListForUser(renderMethod: RenderMethod?): List<ReportDefinition> {
    return productDefinitionRepository.getProductDefinitions()
      .map { map(it, renderMethod) }
      .filter { it.variants.isNotEmpty() }
  }

  private fun map(definition: ProductDefinition, renderMethod: RenderMethod?): ReportDefinition = ReportDefinition(
    id = definition.id,
    name = definition.name,
    description = definition.description,
    variants = definition.report
      .filter { renderMethod == null || it.render.toString() == renderMethod.toString() }
      .map { map(it, definition.dataSet) },
  )

  private fun map(definition: Report, dataSets: List<DataSet>): VariantDefinition {
    val dataSetRef = definition.dataset.removePrefix("\$ref:")
    val dataSet = dataSets.find { it.id == dataSetRef }
      ?: throw IllegalArgumentException("Could not find matching DataSet '$dataSetRef'")

    return VariantDefinition(
      id = definition.id,
      name = definition.name,
      description = definition.description,
      specification = map(definition.specification, dataSet.schema.field),
      resourceName = dataSet.id,
    )
  }

  private fun map(
    specification: uk.gov.justice.digital.hmpps.digitalprisonreportingmi.data.model.Specification?,
    schemaFields: List<SchemaField>,

  ): Specification? {
    if (specification == null) {
      return null
    }

    return Specification(
      template = specification.template,
      fields = specification.field.map { map(it, schemaFields) },
    )
  }

  private fun map(field: ReportField, schemaFields: List<SchemaField>): FieldDefinition {
    val schemaFieldRef = field.schemaField.removePrefix("\$ref:")
    val schemaField = schemaFields.find { it.name == schemaFieldRef }
      ?: throw IllegalArgumentException("Could not find matching Schema Field '$schemaFieldRef'")

    return FieldDefinition(
      name = schemaField.name,
      displayName = field.displayName,
      wordWrap = field.wordWrap?.toString()?.let(WordWrap::valueOf),
      filter = field.filter?.let(this::map),
      sortable = field.sortable,
      defaultSortColumn = field.defaultSortColumn,
      type = schemaField.type.toString().let(FieldType::valueOf),
    )
  }

  private fun map(definition: uk.gov.justice.digital.hmpps.digitalprisonreportingmi.data.model.FilterDefinition): FilterDefinition = FilterDefinition(
    type = FilterType.valueOf(definition.type.toString()),
    staticOptions = definition.staticOptions?.map(this::map),
    defaultValue = replaceTokens(definition.defaultValue),
  )

  private fun replaceTokens(defaultValue: String?): String? {
    if (defaultValue == null) {
      return null
    }

    val today = LocalDate.now().format(ISO_LOCAL_DATE)
    var result = defaultValue.replace(todayRegex, today)

    if (result.matches(dateRegex)) {
      dateRegex.findAll(result)
        .forEach {
          val offset = it.groupValues[0].toLong()

          val resultDate = when (DateDifferenceMagnitude.valueOf(it.groupValues[1])) {
            Days -> LocalDate.now().plusDays(offset)
            Weeks -> LocalDate.now().plusWeeks(offset)
            Months -> LocalDate.now().plusMonths(offset)
            Years -> LocalDate.now().plusYears(offset)
          }

          result = result.replace(it.value, resultDate.format(ISO_LOCAL_DATE))
        }
    }

    return result
  }

  private fun map(definition: uk.gov.justice.digital.hmpps.digitalprisonreportingmi.data.model.FilterOption): FilterOption = FilterOption(
    name = definition.name,
    displayName = definition.displayName,
  )
}

enum class DateDifferenceMagnitude {
  Days,
  Weeks,
  Months,
  Years,
}
