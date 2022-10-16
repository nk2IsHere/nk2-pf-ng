package eu.nk2.portfolio.impl.api

import eu.nk2.portfolio.PortfolioConfigurationProperties
import eu.nk2.portfolio.impl.api.filter.AuthenticationFilterConfiguration
import eu.nk2.portfolio.impl.api.filter.AuthorizationFilterDependencies
import eu.nk2.portfolio.impl.api.filter.authorizationFilter
import eu.nk2.portfolio.impl.data.Token
import eu.nk2.portfolio.impl.data.TokenServiceDependencies
import eu.nk2.portfolio.impl.data.tokenServiceGenerateToken
import eu.nk2.portfolio.util.control.*
import org.springframework.http.HttpMethod
import org.springframework.web.reactive.function.server.*

internal data class RestApiV1TokenPostRequest(
    val password: String
)

internal data class RestApiV1TokenPostResponse(
    val value: Token? = null,
    val error: String? = null
)

interface RestApiV1TokenDependencies: TokenServiceDependencies, AuthorizationFilterDependencies

context(PortfolioConfigurationProperties, RestApiV1TokenDependencies)
fun restApiV1TokenRouter() =
    coRouter {
        filter(authorizationFilter(listOf(
            AuthenticationFilterConfiguration(
                "/api/v1/token",
                methodMode = AuthenticationFilterConfiguration.SomeMethods(methods = listOf(HttpMethod.GET))
            )
        )))

        POST("/api/v1/token") {
            val request = monoOption {
                it.bodyToMono<RestApiV1TokenPostRequest>()
            }

            val token = async {
                request.flatMap { tokenServiceGenerateToken(it.password) }
            }

            token
                .await()
                .fold(
                    {
                        ServerResponse
                            .badRequest()
                            .bodyValueAndAwait(RestApiV1TokenPostResponse(
                                error = "Password mismatch or no body provided"
                            ))
                    },
                    {
                        ServerResponse
                            .ok()
                            .bodyValueAndAwait(RestApiV1TokenPostResponse(
                                value = it
                            ))
                    }
                )
        }

        GET("/api/v1/token") {
            // Validation provided by authorization filter
            ServerResponse
                .ok()
                .buildAndAwait()
        }
    }