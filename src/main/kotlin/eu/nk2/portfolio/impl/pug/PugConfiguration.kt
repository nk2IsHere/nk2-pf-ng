package eu.nk2.portfolio.impl.pug

import de.neuland.pug4j.PugConfiguration
import eu.nk2.portfolio.util.annotation.NoArgsConstructor
import eu.nk2.portfolio.util.context.resolveResourceLoader
import eu.nk2.portfolio.util.control.Try
import eu.nk2.portfolio.util.control.async
import eu.nk2.portfolio.util.control.foldRight
import eu.nk2.portfolio.util.control.tryAsync
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.reactor.awaitSingleOrNull
import kotlinx.coroutines.reactor.mono
import kotlinx.coroutines.withContext
import org.springframework.context.support.BeanDefinitionDsl
import org.springframework.core.io.buffer.DataBufferUtils
import org.springframework.fu.kofu.configuration
import org.springframework.web.reactive.result.view.View
import org.springframework.web.reactive.result.view.ViewResolver
import reactor.core.publisher.Mono

@NoArgsConstructor
data class PugConfigurationProperties(
    val templateLoaderFolderResource: String = "classpath:/templates",
    val templateExtension: String = ".pug",
    val viewRenderExceptions: Boolean = false,
    val viewCaching: Boolean = true,
    val errorViewName: String = "cascade/error.pug"
)

private const val DATA_BUFFER_DEFAULT_SIZE: Int = 16 * 1024 * 1024

context(PugConfigurationProperties)
private fun resolvePugView(name: String, pugConfiguration: PugConfiguration): View =
    View { model, _, exchange -> mono {
        val renderedTemplate = withContext(Dispatchers.IO) {
            Try {
                pugConfiguration
                    .renderTemplate(
                        pugConfiguration.getTemplate(name),
                        model
                    )
                    .byteInputStream()
            }
            .foldRight {
                pugConfiguration
                    .renderTemplate(
                        pugConfiguration.getTemplate(errorViewName),
                        mapOf(
                            "errorMessage" to it.message,
                            "stackTrace" to it.stackTraceToString()
                        )
                    )
                    .byteInputStream()
            }
        }


        exchange
            .response
            .writeWith(DataBufferUtils.readInputStream(
                { renderedTemplate },
                exchange.response.bufferFactory(),
                DATA_BUFFER_DEFAULT_SIZE
            ))
            .awaitSingleOrNull()
    } }


context(PugConfigurationProperties)
private fun BeanDefinitionDsl.pugConfigurationBean() =
    bean {
        PugConfiguration()
            .apply {
                isCaching = viewCaching
                templateLoader = ref()
            }
    }

context(PugConfigurationProperties)
private fun BeanDefinitionDsl.pugTemplateResolverBean() =
    bean {
        ViewResolver { name, _ ->
            Mono.fromCallable { resolvePugView(name, ref()) }
        }
    }

context(PugConfigurationProperties)
private fun BeanDefinitionDsl.pugTemplateLoaderBean() =
    bean {
        PugSpringTemplateLoader(
            resourceLoader = resolveResourceLoader(),
            templateLoaderPath = templateLoaderFolderResource,
            templateExtension = templateExtension
        )
    }

fun pugConfig() =
    configuration {
        with(configurationProperties<PugConfigurationProperties>(prefix = "pug")) {
            beans {
                pugTemplateLoaderBean()
                pugTemplateResolverBean()
                pugConfigurationBean()
            }
        }
    }