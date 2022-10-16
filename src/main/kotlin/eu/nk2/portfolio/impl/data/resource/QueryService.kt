package eu.nk2.portfolio.impl.data.resource

import eu.nk2.portfolio.impl.data.DataDependencies
import eu.nk2.portfolio.impl.data.Resource
import eu.nk2.portfolio.impl.data.ResourceId
import eu.nk2.portfolio.impl.data.ResourceServiceDependencies
import eu.nk2.portfolio.util.control.*
import eu.nk2.portfolio.util.misc.deferred
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.merge

typealias ResourceQueryServiceChain = suspend (ids: List<ResourceId>) -> Try<Flow<Resource>>

data class ResourceQueryServiceLoader(
    val priority: Byte = Byte.MAX_VALUE,
    val predicate: (id: ResourceId) -> Boolean,
    val mapper: suspend (chain: ResourceQueryServiceChain, id: ResourceId) -> Try<Flow<Resource>>
)

interface ResourceQueryServiceDependencies: ResourceServiceDependencies, DataDependencies {
    val resourceQueryServiceLoaders: List<ResourceQueryServiceLoader>
}

fun hydrateResourceIdWithVariables(id: ResourceId, variables: Map<String, String>) =
    variables
        .entries
        .fold(id) { acc, (name, value) ->
            acc.copy(
                containerId = acc.containerId.replace("{${name}}", value),
                resourceId = acc.resourceId.replace("{${name}}", value)
            )
        }

context(ResourceQueryServiceDependencies)
suspend fun resourceQueryServiceQuery(
    ids: List<ResourceId>,
    variables: Map<String, String>
): Try<Flow<Resource>> =
    ids
        .map { hydrateResourceIdWithVariables(it, variables) }
        .map { id ->
            resourceQueryServiceLoaders
                .sortedByDescending { it.priority }
                .find { it.predicate(id) }
                .option
                .map { async { it.mapper({ resourceQueryServiceQuery(it, variables) }, id) } }
                .foldRight {
                    emptyFlow<Resource>()
                        .wrap
                        .deferred
                }
        }
        .awaitAll()
        .reduce { acc, entry ->
            acc.zip(entry) { it1, it2 ->
                merge(it1, it2)
            }
        }
