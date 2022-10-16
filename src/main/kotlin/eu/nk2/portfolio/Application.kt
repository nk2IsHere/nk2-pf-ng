package eu.nk2.portfolio

import eu.nk2.portfolio.impl.api.apiConfig
import eu.nk2.portfolio.impl.data.dataConfig
import eu.nk2.portfolio.impl.pug.pugConfig
import eu.nk2.portfolio.impl.tika.tikaConfig
import eu.nk2.portfolio.util.annotation.NoArgsConstructor
import org.springframework.boot.logging.LogLevel
import org.springframework.fu.kofu.reactiveWebApplication

@NoArgsConstructor
data class PortfolioConfigurationProperties(
	val webPort: Int = 3030,
	val webDefaultPageSize: Int = 25,
	val mongoUri: String = "mongodb://localhost:27017/portfolio",
	val publicApiAssetFolderResource: String = "classpath:/assets",
	val tokenPassword: String,
	val tokenExpiryMilliseconds: Long = 604_800_000L, // 1 week
	val tokenLength: Int = 25,
)

fun app() =
	reactiveWebApplication {
		with(configurationProperties<PortfolioConfigurationProperties>(prefix = "portfolio")) {
			enable(dataConfig())
			enable(pugConfig())
			enable(tikaConfig())
			enable(apiConfig())
		}
	}

fun main() {
	app()
		.apply {
			// Mapping mongo document to object fails because parameter names in constructor are erased in Kotlin
			// And kotlin-specific parameter discovery routine is not triggered if this specific graalvm property is set
			// This property is for some apparent reason set on init of kofu application (even if native image is not used)
			// Hours spent on debugging: 4
			System.clearProperty("org.graalvm.nativeimage.imagecode")
		}
		.run()
}
