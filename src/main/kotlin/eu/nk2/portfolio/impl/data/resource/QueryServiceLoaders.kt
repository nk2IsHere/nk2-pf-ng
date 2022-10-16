package eu.nk2.portfolio.impl.data.resource

import eu.nk2.portfolio.impl.data.*
import eu.nk2.portfolio.util.control.*
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map

private fun buildResourceQueryRegexByResourceId(id: ResourceId): Regex {
    val containerIdPart = id.containerId.replace("*", ".*")
    val resourceIdPart = id.resourceId.replace("*", ".*")

    return Regex("^(${containerIdPart})/(${resourceIdPart})#?.*\$")
}

private fun resourceHasProperTags(id: ResourceId, resource: Resource): Boolean =
    resource.id
        .tags
        .containsAll(id.tags)

interface RegularResourceQueryServiceLoaderDependencies: ResourceServiceDependencies

context(RegularResourceQueryServiceLoaderDependencies)
fun regularResourceQueryServiceLoader() =
    ResourceQueryServiceLoader(
        priority = 1,
        predicate = { true },
        mapper = { _, id ->
            resourceServiceFindAllByIdRegex(buildResourceQueryRegexByResourceId(id))
                .map {
                    it.filter { resourceHasProperTags(id, it) }
                }
        }
    )

private fun buildProjectQueryRegexById(id: String): Regex =
    Regex("^${id.replace("*", "(.*)")}\$")

interface ProjectResourceQueryServiceLoaderDependencies: ResourceServiceDependencies, ProjectServiceDependencies

context(ProjectResourceQueryServiceLoaderDependencies)
fun projectResourceQueryServiceLoader() =
    ResourceQueryServiceLoader(
        predicate = { it.containerId == "eu.nk2.pf.project" },
        mapper = { _, id ->
            projectServiceFindAllByIdRegex(buildProjectQueryRegexById(id.resourceId))
                .map {
                    it.map { it.toResource() }
                }
        }
    )


interface ProjectResourceQueryQueryServiceLoaderDependencies: ResourceServiceDependencies, ProjectServiceDependencies

context(ProjectResourceQueryQueryServiceLoaderDependencies)
fun projectResourceQueryQueryServiceLoader() =
    ResourceQueryServiceLoader(
        predicate = { it.containerId == "eu.nk2.pf.project.resources" },
        mapper = { chain, id ->
            projectServiceFindById(id.resourceId)
                .flatMapTry { chain(it.resourceIds) }
                .map {
                    it.foldRight { emptyFlow() }
                }
                .map {
                    it.filter { resourceHasProperTags(id, it) }
                }
        }
    )
