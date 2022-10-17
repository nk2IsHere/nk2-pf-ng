package eu.nk2.portfolio.impl.data

import eu.nk2.portfolio.util.annotation.NoArgsConstructor
import eu.nk2.portfolio.util.control.*
import eu.nk2.portfolio.util.misc.shouldNotHappen
import kotlinx.coroutines.flow.Flow
import org.springframework.data.annotation.Transient
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.findById
import org.springframework.data.mongodb.core.find
import org.springframework.data.mongodb.core.aggregate
import org.springframework.data.mongodb.core.aggregation.Aggregation
import org.springframework.data.mongodb.core.aggregation.LimitOperation
import org.springframework.data.mongodb.core.aggregation.SkipOperation
import org.springframework.data.mongodb.core.aggregation.SortOperation
import org.springframework.data.mongodb.core.findAllAndRemove
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.mapping.MongoId
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Query.query
import org.springframework.data.mongodb.core.query.inValues
import reactor.core.publisher.Flux

@Document(collection = "project")
@NoArgsConstructor
data class Project(
    @MongoId override val id: String,
    val name: String,
    val year: Int,
    val categories: List<String> = listOf(),
    override val resourceIds: List<ResourceId> = listOf()
): ResourceContainer(
    id = id,
    resourceIds = resourceIds
)

@NoArgsConstructor
data class ProjectResource(
    @Transient override val id: ResourceId,
    val name: String,
    val year: Int,
    val categories: List<String> = listOf()
): Resource(
    id = id,
    type = ProjectResource::class.qualifiedName ?: shouldNotHappen()
) {
    override fun render(): Map<String, Any> =
        mapOf(
            "type" to "ProjectResource",
            "id" to id.resourceId,
            "name" to name,
            "year" to year,
            "categories" to categories
        )
}

fun Project.toResource() =
    ProjectResource(
        ResourceId("eu.nk2.pf.project", id),
        name,
        year,
        categories
    )

@NoArgsConstructor
data class ProjectMetadataResource(
    @Transient override val id: ResourceId,
    val client: String,
    val technologies: List<String>,
    val shortDescription: String,
    val description: List<String>
): Resource(
    id = id,
    type = ProjectMetadataResource::class.qualifiedName ?: shouldNotHappen()
) {
    override fun render(): Map<String, Any> =
        mapOf(
            "type" to "ProjectMetadataResource",
            "client" to client,
            "technologies" to technologies,
            "shortDescription" to shortDescription,
            "description" to description
        )
}


@NoArgsConstructor
data class ProjectGpdrMetadataResource(
    @Transient override val id: ResourceId,
    val language: String,
    val name: String,
    val using: String,
    val email: String
): Resource(
    id = id,
    type = ProjectGpdrMetadataResource::class.qualifiedName ?: shouldNotHappen()
) {
    override fun render(): Map<String, Any> =
        mapOf(
            "type" to "ProjectGpdrMetadataResource",
            "language" to language,
            "name" to name,
            "using" to using,
            "email" to email
        )
}

interface ProjectServiceDependencies: DataDependencies

context(ProjectServiceDependencies)
suspend fun projectServiceFindById(id: String): Try<Option<Project>> =
    monoTryOption {
        mongoTemplate.findById(id)
    }

context(ProjectServiceDependencies)
suspend fun projectServiceFindAll(pageable: Pageable): Try<Flow<Project>> =
    fluxTry {
        mongoTemplate
            .find(
                Query()
                    .with(pageable)
                    .with(Sort.by(Sort.Direction.DESC, "year", "name"))
            )
    }

context(ProjectServiceDependencies)
suspend fun projectServiceFindAllByIdRegex(idRegex: Regex): Try<Flow<Project>> =
    fluxTry {
        mongoTemplate
            .find(query(
                Criteria
                    .where("id")
                    .regex(idRegex.toPattern())
            ))
    }

context(ProjectServiceDependencies)
suspend fun projectServiceSaveAll(projects: List<Project>): Try<Flow<Project>> =
    fluxTry {
        Flux
            .fromIterable(projects)
            .flatMap { mongoTemplate.save(it) }
    }

context(ProjectServiceDependencies)
suspend fun projectServiceDeleteByIds(ids: List<String>): Try<Flow<Project>> =
    fluxTry {
        mongoTemplate
            .findAllAndRemove(query(
                Criteria
                    .where("id")
                    .inValues(ids)
            ))
    }
