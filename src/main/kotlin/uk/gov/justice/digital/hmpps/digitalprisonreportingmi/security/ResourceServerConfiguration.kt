package uk.gov.justice.digital.hmpps.digitalprisonreportingmi.security

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.invoke
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
@Profile("!test")
@Configuration
class ResourceServerConfiguration {

  @Value("\${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}")
  private lateinit var jwkUri: String

  @Bean
  @Throws(Exception::class)
  fun filterChain(http: HttpSecurity): SecurityFilterChain? {
    http {
      headers { frameOptions { sameOrigin = true } }
      sessionManagement { sessionCreationPolicy = SessionCreationPolicy.STATELESS }
      // Can't have CSRF protection as requires session
      csrf { disable() }
      authorizeHttpRequests {
        listOf(
          "/health/**",
          "/info",
        ).forEach { authorize(it, permitAll) }
        authorize(anyRequest, authenticated)
      }
      oauth2ResourceServer { jwt {
        jwtAuthenticationConverter = AuthAwareTokenConverter()
        jwkSetUri = jwkUri
      } }
    }
    return http.build()
  }
}
