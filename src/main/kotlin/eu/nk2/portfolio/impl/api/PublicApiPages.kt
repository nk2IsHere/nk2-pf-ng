package eu.nk2.portfolio.impl.api

import eu.nk2.portfolio.PortfolioConfigurationProperties
import eu.nk2.portfolio.impl.data.*
import eu.nk2.portfolio.impl.data.resource.ResourceQueryServiceDependencies
import eu.nk2.portfolio.impl.data.resource.resourceQueryServiceQuery
import eu.nk2.portfolio.impl.data.view.ViewModel
import eu.nk2.portfolio.util.context.WebFluxExceptionApiResponse
import eu.nk2.portfolio.util.control.*
import eu.nk2.portfolio.util.misc.shouldNotHappen
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.toList
import org.slf4j.LoggerFactory
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.util.PathMatcher
import org.springframework.web.reactive.function.server.*
import org.springframework.web.server.ResponseStatusException

interface PublicApiPagesDependencies: ResourceQueryServiceDependencies, PageServiceDependencies, ProjectServiceDependencies {
    val pathMatcher: PathMatcher
}

data class PublicApiPagesPageViewModel(
    val method: HttpMethod,
    val resources: List<Resource>
): ViewModel {
    private val logger = LoggerFactory.getLogger(PublicApiPagesPageViewModel::class.java)

    override fun render(): Map<String, Any> =
        mapOf(
            "context" to this,
            "method" to method.toString(),
            "resources" to resources.map { it.render() }
        )

    // TODO: move to vm functions beans and allow context functions extension
    // this is VERY ugly.

    private fun listResourcesByIdAndTypeInternal(id: ResourceId, type: String): List<Resource> {
        val foundResources = resources
            .filter {
                (it.id.containerId == id.containerId || id.containerId == "*")
                    && (it.id.resourceId == id.resourceId || id.resourceId == "*")
                    && it.id.tags.containsAll(id.tags)
                    && (it.type.contains(type)  || type == "*") // TODO: types are not defined in short form while searching
            }

        return foundResources
    }

    fun getResourceById(id: String): Map<String, Any>? {
        val parsedId = ResourceId.fromString(id)
        val foundResources = listResourcesByIdAndTypeInternal(parsedId, "*")

        logger.info("[getResourceById] $id -> $foundResources")
        return foundResources
            .firstOrNull()
            ?.render()
    }

    fun getResourceByIdAndType(id: String, type: String): Map<String, Any>? {
        val parsedId = ResourceId.fromString(id)
        val foundResources = listResourcesByIdAndTypeInternal(parsedId, type)

        logger.info("[getResourceByIdAndType] $id, $type -> $foundResources")
        return foundResources
            .firstOrNull()
            ?.render()
    }

    fun listResourcesById(id: String): List<Map<String, Any>> {
        val parsedId = ResourceId.fromString(id)

        return listResourcesByIdAndTypeInternal(parsedId, "*")
            .apply { logger.info("[listResourcesById] $id -> $this") }
            .map { it.render() }
    }


    fun listResourcesByIdAndType(id: String, type: String): List<Map<String, Any>> {
        val parsedId = ResourceId.fromString(id)

        return listResourcesByIdAndTypeInternal(parsedId, type)
            .apply { logger.info("[listResourcesByIdAndType] $id, $type -> $this") }
            .map { it.render() }
    }

    // TODO: move to another function module in pug
    fun replaceInStrings(values: List<String>, from: String, to: String) =
        values.map { it.replace(from, to) }

    fun joinStrings(values: List<String>, separator: String) =
        values.joinToString(separator)

    fun sortBy(values: List<Map<String, Any>>, key: String) =
        values.sortedBy { it[key] as Comparable<Any> }

    fun sortedByDescending(values: List<Map<String, Any>>, key: String) =
        values.sortedByDescending { it[key] as Comparable<Any> }
}

context(PortfolioConfigurationProperties, PublicApiPagesDependencies)
fun publicApiPagesRouter() =
    coRouter {
        GET("/**") {
            val requestMethod = it.method() ?: shouldNotHappen()
            val requestPath = it.path()

            val pageAndResourcesQuery = async {
                val matchingPathAndVariables = pageServiceFindAllPaths()
                    .map {
                        it.filter { pathMatcher.match(it.path, requestPath) }
                            .firstOrNull()
                            .option
                            .map { it.path then pathMatcher.extractUriTemplateVariables(it.path, requestPath) }
                    }

                val matchingPage = matchingPathAndVariables
                    .flatMapTryOption { (path, _) -> pageServiceFindByPath(path) }

                val resourcesQuery = matchingPathAndVariables
                    .zipWithTryOption(matchingPage)
                    .flatMapTry { (pathToVariables, page) ->
                        resourceQueryServiceQuery(
                            page.resourceIds,
                            pathToVariables.t2
                        )
                    }

                val templateResource = resourcesQuery
                    .flatMapOption {
                        it.firstOrNull { it is PageTemplateResource }
                            .option
                            .cast<Resource, PageTemplateResource>()
                    }

                zipTryOption(
                    matchingPage,
                    resourcesQuery,
                    templateResource
                )
            }

            pageAndResourcesQuery
                .await()
                .fold(
                    {
                        ServerResponse
                            .status(
                                if(it is ResponseStatusException) it.status
                                else HttpStatus.INTERNAL_SERVER_ERROR
                            )
                            .renderAndAwait(
                                "cascade/error.pug",
                                mapOf(
                                    "errorMessage" to it.message,
                                    "stackTrace" to it.stackTraceToString()
                                )
                            )
                    },
                    {
                        ServerResponse
                            .status(HttpStatus.NOT_FOUND)
                            .renderAndAwait(
                                "cascade/not_found.pug",
                                mapOf<String, Any>()
                            )
                    },
                    { (_, resources, template) ->
                        ServerResponse
                            .ok()
                            .renderAndAwait(
                                template.path,
                                PublicApiPagesPageViewModel(requestMethod, resources.toList())
                                    .render()
                            )
                    }
                )
        }
    }