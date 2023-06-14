package uk.gov.justice.digital.hmpps.digitalprisonreportingmi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.digitalprisonreportingmi.data.EstablishmentRepositoryRepository

@Service
data class EstablishmentsService(val establishmentRepository: EstablishmentRepositoryRepository) {

  fun establishmentsCount(): Long {
    return establishmentRepository.count()
  }


}