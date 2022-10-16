package eu.nk2.portfolio.impl.api

import eu.nk2.portfolio.PortfolioConfigurationProperties
import eu.nk2.portfolio.util.control.*
import org.apache.tika.Tika
import org.springframework.core.io.Resource
import org.springframework.core.io.ResourceLoader
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.bodyValueAndAwait
import org.springframework.web.reactive.function.server.buildAndAwait
import org.springframework.web.reactive.function.server.coRouter
import kotlin.io.path.Path

interface PublicApiAssetsDependencies {
    val resourceLoader: ResourceLoader
    val mediaTypeDetector: Tika
}

private suspend fun resourceToServerResponse(mediaTypeDetector: Tika, resource: Resource): ServerResponse {
    val pathContentType = async { mediaTypeDetector.detect(resource.file) }

    return ServerResponse
        .ok()
        .contentType(MediaType.parseMediaType(pathContentType.await()))
        .bodyValueAndAwait(resource)
}

context(PortfolioConfigurationProperties, PublicApiAssetsDependencies)
fun publicApiAssetsRouter() =
    coRouter {
        GET("/assets/**") {
            val resource = tryAsync {
                resourceLoader
                    .getResource(
                        Path(publicApiAssetFolderResource)
                            .resolve(
                                it.path()
                                    .removePrefix("/assets/")
                            )
                            .toString()
                    )
            }

            resource
                .await()
                .map {
                    if(it.isReadable && it.isFile) it.something
                    else it.nothing
                }
                .fold(
                    {
                        ServerResponse
                            .badRequest()
                            .buildAndAwait()
                    },
                    {
                        ServerResponse
                            .notFound()
                            .buildAndAwait()
                    },
                    {
                        resourceToServerResponse(
                            mediaTypeDetector,
                            it
                        )
                    }
                )
        }
    }
