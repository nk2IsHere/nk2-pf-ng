package eu.nk2.portfolio.impl.api

import eu.nk2.portfolio.PortfolioConfigurationProperties
import eu.nk2.portfolio.impl.api.filter.AuthenticationFilterConfiguration
import eu.nk2.portfolio.impl.api.filter.AuthorizationFilterDependencies
import eu.nk2.portfolio.impl.api.filter.authorizationFilter
import eu.nk2.portfolio.impl.data.*
import eu.nk2.portfolio.impl.data.resource.ResourceQueryServiceDependencies
import eu.nk2.portfolio.impl.data.resource.resourceQueryServiceQuery
import eu.nk2.portfolio.util.context.toApiResponse
import eu.nk2.portfolio.util.control.*
import kotlinx.coroutines.flow.toList
import org.springframework.http.HttpMethod
import org.springframework.web.reactive.function.server.bodyToFlux
import org.springframework.web.reactive.function.server.bodyToMono
import org.springframework.web.reactive.function.server.coRouter

interface RestApiV1ResourceDependencies: ResourceServiceDependencies, ResourceQueryServiceDependencies, AuthorizationFilterDependencies

data class RestApiV1ResourceQueryRequest(
    val variables: Map<String, String>,
    val resourceIds: List<ResourceId>
)

context(PortfolioConfigurationProperties, RestApiV1ResourceDependencies)
fun restApiV1ResourceRouter() =
    coRouter {
        filter(authorizationFilter(listOf(
            AuthenticationFilterConfiguration(
                "/api/v1/resource",
                methodMode = AuthenticationFilterConfiguration.SomeMethods(listOf(HttpMethod.PUT))
            )
        )))

        GET("/api/v1/resource/{containerId}") {
            val containerId = it.pathVariable("containerId")
            val resources = async { resourceServiceFindAllByContainerId(containerId) }

            resources
                .await()
                .toApiResponse()
        }

        PUT("/api/v1/resource") {
            val resourcesRequest = fluxTry { it.bodyToFlux<Resource>() }
            val savedResources = async {
                resourcesRequest
                    .flatMap { resourceServiceSaveAll(it.toList()) }
            }

            savedResources
                .await()
                .toApiResponse()
        }

        POST("/api/v1/resource/query") {
            val resourcesQueryRequest = monoTryOption { it.bodyToMono<RestApiV1ResourceQueryRequest>() }
            val resourcesQueryResponse = async {
                resourcesQueryRequest
                    .popLeft()
                    .flatMapRight { resourceQueryServiceQuery(it.resourceIds, it.variables) }
            }

            resourcesQueryResponse
                .await()
                .toApiResponse()
        }
    }
