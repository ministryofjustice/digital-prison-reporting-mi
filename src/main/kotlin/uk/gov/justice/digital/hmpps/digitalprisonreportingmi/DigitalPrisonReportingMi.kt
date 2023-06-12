package uk.gov.justice.digital.hmpps.digitalprisonreportingmi

import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication

@SpringBootApplication
class DigitalPrisonReportingMi

fun main(args: Array<String>) {
  runApplication<DigitalPrisonReportingMi>(*args)
}
