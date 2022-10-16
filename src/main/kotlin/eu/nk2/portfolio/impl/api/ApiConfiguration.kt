package eu.nk2.portfolio.impl.api

import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.module.kotlin.addDeserializer
import com.fasterxml.jackson.module.kotlin.addSerializer
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import eu.nk2.portfolio.PortfolioConfigurationProperties
import eu.nk2.portfolio.impl.data.Resource
import eu.nk2.portfolio.impl.data.ResourceId
import eu.nk2.portfolio.impl.data.resource.ResourceQueryServiceLoader
import eu.nk2.portfolio.util.context.resolveResourceLoader
import org.apache.tika.Tika
import org.springframework.context.support.BeanDefinitionDsl
import org.springframework.core.io.ResourceLoader
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.fu.kofu.configuration
import org.springframework.fu.kofu.webflux.webFlux
import org.springframework.util.AntPathMatcher
import org.springframework.util.PathMatcher

context(PortfolioConfigurationProperties)
private fun BeanDefinitionDsl.routerBean() =
    bean {
        val restApiV1ProjectDependencies = object: RestApiV1ProjectDependencies {
            override val mongoTemplate: ReactiveMongoTemplate = ref()
            override val pathMatcher: PathMatcher = ref()
        }

        val restApiV1TokenDependencies = object: RestApiV1TokenDependencies {
            override val mongoTemplate: ReactiveMongoTemplate = ref()
            override val pathMatcher: PathMatcher = ref()
        }

        val restApiV1PageDependencies = object: RestApiV1PageDependencies {
            override val mongoTemplate: ReactiveMongoTemplate = ref()
            override val pathMatcher: PathMatcher = ref()
        }

        val restApiV1ResourceDependencies = object: RestApiV1ResourceDependencies {
            override val mongoTemplate: ReactiveMongoTemplate = ref()
            override val pathMatcher: PathMatcher = ref()
            override val resourceQueryServiceLoaders: List<ResourceQueryServiceLoader> = ref()
        }

        val publicApiAssetsDependencies = object: PublicApiAssetsDependencies {
            override val resourceLoader: ResourceLoader = resolveResourceLoader()
            override val mediaTypeDetector: Tika = ref()
        }

        val publicApiPagesDependencies = object: PublicApiPagesDependencies {
            override val pathMatcher: PathMatcher = ref()
            override val resourceQueryServiceLoaders: List<ResourceQueryServiceLoader> = ref()
            override val mongoTemplate: ReactiveMongoTemplate = ref()

        }

        with(restApiV1ProjectDependencies) { restApiV1ProjectRouter() }
            .andOther(with(restApiV1TokenDependencies) { restApiV1TokenRouter() })
            .andOther(with(restApiV1PageDependencies) { restApiV1PageRouter() })
            .andOther(with(restApiV1ResourceDependencies) { restApiV1ResourceRouter() })
            .andOther(with(publicApiAssetsDependencies) { publicApiAssetsRouter() })
            .andOther(with(publicApiPagesDependencies) { publicApiPagesRouter() })
    }

private fun BeanDefinitionDsl.objectMapperBean() =
    bean(isPrimary = true) {
        jacksonObjectMapper()
            .registerModules(
                SimpleModule()
                    .addDeserializer(Resource::class, Resource.jsonDeserializer)
                    .addSerializer(ResourceId::class, ResourceId.jsonSerializer)
                    .addDeserializer(ResourceId::class, ResourceId.jsonDeserializer)
            )
    }

private fun BeanDefinitionDsl.pathMatcherBean() =
    bean { AntPathMatcher() }

context(PortfolioConfigurationProperties)
fun apiConfig() =
    configuration {
        beans {
            pathMatcherBean()
            objectMapperBean()
            routerBean()
        }

        webFlux {
            port = webPort

            codecs {
                string()
                resource()
                multipart()
                jackson()
            }
        }
    }
