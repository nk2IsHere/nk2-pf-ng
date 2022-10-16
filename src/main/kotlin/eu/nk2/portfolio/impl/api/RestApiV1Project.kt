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
import org.springframework.web.reactive.function.server.bodyToFlux
import org.springframework.web.reactive.function.server.bodyToMono
import org.springframework.web.reactive.function.server.coRouter
import org.springframework.web.reactive.function.server.queryParamOrNull

interface RestApiV1ProjectDependencies: ProjectServiceDependencies, AuthorizationFilterDependencies

context(PortfolioConfigurationProperties, RestApiV1ProjectDependencies)
fun restApiV1ProjectRouter() =
    coRouter {
        filter(authorizationFilter(listOf(
            AuthenticationFilterConfiguration(
                "/api/v1/project",
                methodMode = AuthenticationFilterConfiguration.SomeMethods(listOf(HttpMethod.PUT))
            )
        )))

        GET("/api/v1/project") {
            val pageNumber = it
                .queryParamOrNull("page")
                .option
                .flatMap { it.toIntOrNull().option }

            val projects = async {
                projectServiceFindAll(
                    pageNumber
                        .fold(
                            { Pageable.unpaged() },
                            { Pageable.ofSize(webDefaultPageSize).withPage(it) }
                        )
                )
            }

            projects
                .await()
                .toApiResponse()
        }

        GET("/api/v1/project/{id}") {
            val id = it.pathVariable("id")
            val project = async { projectServiceFindById(id) }

            project
                .await()
                .toApiResponse()
        }

        PUT("/api/v1/project") {
            val projectsRequest = fluxTry { it.bodyToFlux<Project>() }
            val savedProjects = async {
                projectsRequest
                    .flatMap { projectServiceSaveAll(it.toList()) }
            }

            savedProjects
                .await()
                .toApiResponse()
        }
    }
