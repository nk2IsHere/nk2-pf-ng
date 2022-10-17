package eu.nk2.portfolio.impl.data

import eu.nk2.portfolio.util.annotation.NoArgsConstructor
import eu.nk2.portfolio.util.control.*
import eu.nk2.portfolio.util.misc.shouldNotHappen
import kotlinx.coroutines.flow.Flow
import org.springframework.data.annotation.Transient
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.find
import org.springframework.data.mongodb.core.findById
import org.springframework.data.mongodb.core.findAllAndRemove
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.mapping.MongoId
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Query.query
import org.springframework.data.mongodb.core.query.inValues
import reactor.core.publisher.Flux

@Document(collection = "page")
@NoArgsConstructor
data class Page(
    @MongoId val path: String,
    override val resourceIds: List<ResourceId>
): ResourceContainer(
    id = path,
    resourceIds = resourceIds
)

data class PagePathProjection(
    @MongoId val path: String
)

@NoArgsConstructor
data class PageTemplateResource(
    @Transient override val id: ResourceId,
    val path: String
): Resource(
    id = id,
    type = PageTemplateResource::class.qualifiedName ?: shouldNotHappen()
) {
    override fun render(): Map<String, Any> =
        mapOf(
            "type" to "PageTemplateResource",
            "path" to path
        )
}

interface PageServiceDependencies: DataDependencies

context(PageServiceDependencies)
suspend fun pageServiceFindAllPaths(): Try<Flow<PagePathProjection>> =
    fluxTry {
        mongoTemplate
            .find(
                Query().apply {
                    fields().include("id")
                },
                "page"
            )
    }

context(PageServiceDependencies)
suspend fun pageServiceFindByPath(path: String): Try<Option<Page>> =
    monoTryOption {
        mongoTemplate.findById(path)
    }

context(PageServiceDependencies)
suspend fun pageServiceFindAll(pageable: Pageable): Try<Flow<Page>> =
    fluxTry {
        mongoTemplate
            .find(
                Query()
                    .with(pageable)
                    .with(Sort.by(Sort.Direction.ASC, "path"))
            )
    }

context(PageServiceDependencies)
suspend fun pageServiceSaveAll(pages: List<Page>): Try<Flow<Page>> =
    fluxTry {
        Flux
            .fromIterable(pages)
            .flatMap { mongoTemplate.save(it) }
    }

context(PageServiceDependencies)
suspend fun pageServiceDeleteByPaths(paths: List<String>): Try<Flow<Page>> =
    fluxTry {
        mongoTemplate
            .findAllAndRemove(query(
                Criteria
                    .where("path")
                    .inValues(paths)
            ))
    }
