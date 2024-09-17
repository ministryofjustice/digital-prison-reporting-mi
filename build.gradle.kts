plugins {
  id("uk.gov.justice.hmpps.gradle-spring-boot") version "6.0.4"
  kotlin("jvm") version "2.0.20"
  kotlin("plugin.spring") version "2.0.0"
  kotlin("plugin.jpa") version "2.0.20"
  id("jacoco")
  id("org.barfuin.gradle.jacocolog") version "3.1.0"
}

configurations {
  testImplementation { exclude(group = "org.junit.vintage") }
}

dependencies {
  implementation("org.springframework.boot:spring-boot-starter-webflux")
  implementation("org.springframework.boot:spring-boot-starter-data-jpa")
  implementation("com.amazon.redshift:redshift-jdbc4-no-awssdk:1.2.45.1069")
  implementation("uk.gov.justice.service.hmpps:hmpps-digital-prison-reporting-lib:6.1.1")

  // Security
  implementation("org.springframework.boot:spring-boot-starter-security")
  implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
  implementation("org.springframework.boot:spring-boot-starter-oauth2-client")

  // Swagger
  implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.6.0")

  implementation("software.amazon.awssdk:redshiftdata:2.28.1")
  implementation("software.amazon.awssdk:athena:2.28.0")
  implementation("software.amazon.awssdk:sts:2.28.2")

  // Testing
  testImplementation("com.h2database:h2")
  testImplementation("io.jsonwebtoken:jjwt:0.12.5")
  testImplementation("javax.xml.bind:jaxb-api:2.4.0-b180830.0359")
  testImplementation("com.marcinziolo:kotlin-wiremock:2.1.1")
}

java {
  toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

repositories {
  mavenLocal()
  mavenCentral()
  maven("https://s3.amazonaws.com/redshift-maven-repository/release")
}

kotlin {
  jvmToolchain(21)
}

tasks.test {
  finalizedBy(tasks.jacocoTestReport)
}

tasks.jacocoTestReport {
  dependsOn(tasks.test)
}
