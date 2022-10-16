import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
	kotlin("jvm") version "1.7.20"
	kotlin("plugin.noarg") version "1.7.20"
	kotlin("plugin.spring") version "1.7.20"
	id("io.spring.dependency-management") version "1.0.14.RELEASE"
	id("org.springframework.boot") version "2.7.4"
}

group = "eu.nk2.portfolio"
version = "0.0.1"
java.sourceCompatibility = JavaVersion.VERSION_17

repositories {
	mavenLocal()
	mavenCentral()
	maven("https://repo.spring.io/milestone")
	maven("https://repo.spring.io/snapshot")
	maven("https://repo.spring.io/release")
	maven("https://repo.nk2.eu/artifactory/gradle-release")
}

dependencies {
	implementation("org.jetbrains.kotlin:kotlin-reflect")
	implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core")
	implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")
	implementation("io.projectreactor.kotlin:reactor-kotlin-extensions")

	implementation("org.springframework.fu:spring-fu-kofu:0.5.2-SNAPSHOT")
	implementation("org.springframework.boot:spring-boot-starter-webflux")
	implementation("org.springframework.boot:spring-boot-starter-data-mongodb-reactive")
	implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
	implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")

	implementation("de.neuland-bfi:pug4j:2.0.5")
	implementation("org.apache.tika:tika-core:2.5.0")
	implementation("eu.nk2:kjackson:v0.2.5")

	testImplementation("org.springframework.boot:spring-boot-starter-test")
}

tasks.withType<KotlinCompile> {
	kotlinOptions {
		jvmTarget = "17"
		freeCompilerArgs = listOf("-java-parameters", "-Xjsr305=strict", "-Xjvm-default=enable", "-Xcontext-receivers")
	}
}

noArg {
	annotation("eu.nk2.portfolio.util.annotation.NoArgsConstructor")
}

tasks.withType<Test> {
	useJUnitPlatform()
}
