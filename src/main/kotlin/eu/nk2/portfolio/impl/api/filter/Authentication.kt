package eu.nk2.portfolio.impl.api.filter

import eu.nk2.portfolio.PortfolioConfigurationProperties
import eu.nk2.portfolio.impl.data.TokenServiceDependencies
import eu.nk2.portfolio.impl.data.TokenValidationResult
import eu.nk2.portfolio.impl.data.tokenServiceValidateToken
import eu.nk2.portfolio.util.context.FilterFunction
import eu.nk2.portfolio.util.control.fold
import eu.nk2.portfolio.util.control.map
import eu.nk2.portfolio.util.control.option
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.util.PathMatcher
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.bodyValueAndAwait

data class AuthenticationFilterConfiguration(
    val pathPattern: String,
    val methodMode: HttpMethodMode,
) {
    sealed class HttpMethodMode
    object AllMethods: HttpMethodMode()
    data class SomeMethods(val methods: List<HttpMethod>): HttpMethodMode()
}


interface AuthorizationFilterDependencies: TokenServiceDependencies {
    val pathMatcher: PathMatcher
}

context(PortfolioConfigurationProperties, AuthorizationFilterDependencies)
fun authorizationFilter(configurations: List<AuthenticationFilterConfiguration>): FilterFunction =
    { request, next ->
        val matchedConfiguration = configurations
            .firstOrNull { pathMatcher.match(it.pathPattern, request.path()) }

        val hasMatchByPath = matchedConfiguration != null

        val hasMatchByMethod = request.method() != HttpMethod.OPTIONS
            && when(val methodMode = matchedConfiguration?.methodMode) {
                is AuthenticationFilterConfiguration.SomeMethods -> request.method() in methodMode.methods
                is AuthenticationFilterConfiguration.AllMethods -> true
                null -> false
            }

        when {
            hasMatchByPath && hasMatchByMethod -> {
                val token = request.headers()
                    .header("Authorization")
                    .firstOrNull()
                    .option

                token
                    .map { it.removePrefix("Bearer ") }
                    .map { tokenServiceValidateToken(it) }
                    .fold(
                        {
                            ServerResponse
                                .status(HttpStatus.UNAUTHORIZED)
                                .bodyValueAndAwait(TokenValidationResult.TOKEN_INVALID)
                        },
                        {
                            ServerResponse
                                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .bodyValueAndAwait(it)
                        },
                        {
                            when(it) {
                                TokenValidationResult.TOKEN_VALID -> next(request)
                                else -> ServerResponse
                                    .status(HttpStatus.UNAUTHORIZED)
                                    .bodyValueAndAwait(it)
                            }
                        }
                    )
            }
            else -> next(request)
        }
    }
