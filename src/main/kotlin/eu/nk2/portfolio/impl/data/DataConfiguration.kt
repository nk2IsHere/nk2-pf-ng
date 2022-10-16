package eu.nk2.portfolio.impl.data

import eu.nk2.portfolio.PortfolioConfigurationProperties
import eu.nk2.portfolio.impl.data.resource.*
import org.springframework.context.support.BeanDefinitionDsl
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.convert.MongoCustomConversions
import org.springframework.fu.kofu.configuration
import org.springframework.fu.kofu.mongo.reactiveMongodb

interface DataDependencies {
    val mongoTemplate: ReactiveMongoTemplate
}

private fun BeanDefinitionDsl.resourceQueryServiceLoadersBean() =
    bean {
        val regularResourceQueryServiceLoaderDependencies = object: RegularResourceQueryServiceLoaderDependencies {
            override val mongoTemplate: ReactiveMongoTemplate = ref()
        }

        val projectResourceQueryServiceLoaderDependencies = object: ProjectResourceQueryServiceLoaderDependencies {
            override val mongoTemplate: ReactiveMongoTemplate = ref()
        }

        val projectResourceQueryQueryServiceLoaderDependencies = object: ProjectResourceQueryQueryServiceLoaderDependencies {
            override val mongoTemplate: ReactiveMongoTemplate = ref()
        }

        listOf(
            with(regularResourceQueryServiceLoaderDependencies) { regularResourceQueryServiceLoader() },
            with(projectResourceQueryServiceLoaderDependencies) { projectResourceQueryServiceLoader() },
            with(projectResourceQueryQueryServiceLoaderDependencies) { projectResourceQueryQueryServiceLoader() }
        )
    }

private fun BeanDefinitionDsl.mongoCustomConversionsBean() =
    bean(isPrimary = true) {
        MongoCustomConversions(listOf(
            ResourceId.MongoSerializer(),
            ResourceId.MongoDeserializer()
        ))
    }

context(PortfolioConfigurationProperties)
fun dataConfig() =
    configuration {
        beans {
            mongoCustomConversionsBean()
            resourceQueryServiceLoadersBean()
        }

        reactiveMongodb {
            uri = mongoUri
        }
    }
