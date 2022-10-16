package eu.nk2.portfolio.util.context

import eu.nk2.portfolio.util.control.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.toList
import org.springframework.http.HttpStatus
import org.springframework.web.reactive.function.server.RouterFunction
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.bodyValueAndAwait
import org.springframework.web.reactive.function.server.buildAndAwait
import org.springframework.web.server.ResponseStatusException

typealias FilterFunction = suspend (ServerRequest, suspend (ServerRequest) -> ServerResponse) -> ServerResponse

data class WebFluxExceptionApiResponse(
    val message: String?,
    val stacktrace: String
)

@JvmName("toApiResponseTryOption")
suspend inline fun <T: Any> Try<Option<T>>.toApiResponse() =
    this
        .fold(
            {
                ServerResponse
                    .status(
                        if(it is ResponseStatusException) it.status
                        else HttpStatus.INTERNAL_SERVER_ERROR
                    )
                    .bodyValueAndAwait(WebFluxExceptionApiResponse(
                        it.message,
                        it.stackTraceToString()
                    ))
            },
            {
                ServerResponse
                    .notFound()
                    .buildAndAwait()
            },
            {
                ServerResponse
                    .ok()
                    .bodyValueAndAwait(it)
            }
        )

@JvmName("toApiResponseOptionTry")
suspend inline fun <T: Any> Option<Try<T>>.toApiResponse() =
    this
        .popLeft()
        .toApiResponse()

@JvmName("toApiResponseTryFlow")
suspend inline fun <T: Any> Try<Flow<T>>.toApiResponse() =
    this
        .fold(
            {
                ServerResponse
                    .status(
                        if(it is ResponseStatusException) it.status
                        else HttpStatus.INTERNAL_SERVER_ERROR
                    )
                    .bodyValueAndAwait(WebFluxExceptionApiResponse(
                        it.message,
                        it.stackTraceToString()
                    ))
            },
            {
                ServerResponse
                    .ok()
                    .bodyValueAndAwait(it.toList())
            }
        )

@JvmName("toApiResponseTryOptionFlow")
suspend inline fun <T: Any> Try<Option<Flow<T>>>.toApiResponse() =
    this
        .fold(
            {
                ServerResponse
                    .status(
                        if(it is ResponseStatusException) it.status
                        else HttpStatus.INTERNAL_SERVER_ERROR
                    )
                    .bodyValueAndAwait(WebFluxExceptionApiResponse(
                        it.message,
                        it.stackTraceToString()
                    ))
            },
            {
                ServerResponse
                    .ok()
                    .bodyValueAndAwait(listOf<T>())
            },
            {
                ServerResponse
                    .ok()
                    .bodyValueAndAwait(it.toList())
            }
        )

@JvmName("toApiResponseOptionTryFlow")
suspend inline fun <T: Any> Option<Try<Flow<T>>>.toApiResponse() =
    this
        .popLeft()
        .toApiResponse()
