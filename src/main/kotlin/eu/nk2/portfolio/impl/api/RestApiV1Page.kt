package eu.nk2.portfolio.impl.api

import eu.nk2.portfolio.PortfolioConfigurationProperties
import eu.nk2.portfolio.impl.api.filter.AuthenticationFilterConfiguration
import eu.nk2.portfolio.impl.api.filter.AuthorizationFilterDependencies
import eu.nk2.portfolio.impl.api.filter.authorizationFilter
import eu.nk2.portfolio.impl.data.*
import eu.nk2.portfolio.util.context.toApiResponse
import eu.nk2.portfolio.util.control.*
import kotlinx.coroutines.flow.toList
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpMethod
import org.springframework.web.reactive.function.server.*

interface RestApiV1PageDependencies: PageServiceDependencies, AuthorizationFilterDependencies

context(PortfolioConfigurationProperties, RestApiV1PageDependencies)
fun restApiV1PageRouter() =
    coRouter {
        filter(authorizationFilter(listOf(
            AuthenticationFilterConfiguration(
                "/api/v1/page",
                methodMode = AuthenticationFilterConfiguration.SomeMethods(listOf(HttpMethod.PUT, HttpMethod.DELETE))
            )
        )))

        GET("/api/v1/page") {
            val pageNumber = it
                .queryParamOrNull("page")
                .option
                .flatMap { it.toIntOrNull().option }

            val pages = async {
                pageServiceFindAll(
                    pageNumber
                        .fold(
                            { Pageable.unpaged() },
                            { Pageable.ofSize(webDefaultPageSize).withPage(it) }
                        )
                )
            }

            pages
                .await()
                .toApiResponse()
        }

        PUT("/api/v1/page") {
            val pagesRequest = fluxTry { it.bodyToFlux<Page>() }
            val savedPages = async {
                pagesRequest.flatMap { pageServiceSaveAll(it.toList()) }
            }

            savedPages
                .await()
                .toApiResponse()
        }

        DELETE("/api/v1/page") {
            val pagesPathsRequest = fluxTry { it.bodyToFlux<String>() }
            val deletedPages = async {
                pagesPathsRequest.flatMap { pageServiceDeleteByPaths(it.toList()) }
            }

            deletedPages
                .await()
                .toApiResponse()
        }
    }
