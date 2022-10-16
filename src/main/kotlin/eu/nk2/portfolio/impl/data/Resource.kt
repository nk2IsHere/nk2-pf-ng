package eu.nk2.portfolio.impl.data

import com.fasterxml.jackson.databind.JsonMappingException
import de.neuland.pug4j.parser.node.TextNode
import eu.nk2.kjackson.*
import eu.nk2.portfolio.impl.data.view.ViewModel
import eu.nk2.portfolio.util.annotation.NoArgsConstructor
import eu.nk2.portfolio.util.control.*
import eu.nk2.portfolio.util.misc.shouldNotHappen
import kotlinx.coroutines.flow.Flow
import org.springframework.core.convert.converter.Converter
import org.springframework.data.annotation.Transient
import org.springframework.data.convert.ReadingConverter
import org.springframework.data.convert.WritingConverter
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.mapping.MongoId
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query.query
import reactor.core.publisher.Flux
import org.springframework.data.mongodb.core.find
import org.springframework.data.mongodb.core.mapping.FieldType
import java.util.regex.Pattern

@NoArgsConstructor
data class ResourceId(
    val containerId: String,
    val resourceId: String,
    val tags: List<String> = listOf()
) {

    override fun toString(): String = (
        "${containerId}/${resourceId}"
            + (
                if(tags.isNotEmpty()) "#${tags.joinToString(",")}"
                else ""
            )
    )

    companion object {
        fun fromString(src: String): ResourceId {
            val matcher = Regex("^(.*)/(.+?(?=#|\$))#?(.*)\$")
                .matchEntire(src)
                ?: error("$src does not match ResourceId pattern.")

            val values = matcher.groupValues
                .drop(1)

            return when(values.size) {
                3 -> ResourceId(
                    containerId = values[0],
                    resourceId = values[1],
                    tags = (
                        if(values[2].isEmpty()) listOf()
                        else values[2].split(",")
                    )
                )
                else -> error("$src does not match ResourceId pattern.")
            }
        }

        val jsonSerializer = jsonSerializer<ResourceId> { src, _ ->
            src.toString()
                .toJson()
        }

        val jsonDeserializer = jsonDeserializer { src, context ->
            src.nullString
                ?.let { TryBlocking { fromString(it) } }
                ?.mapLeft {
                    throw JsonMappingException(
                        context.parser,
                        "$src is not a proper ResourceId"
                    )
                }
                ?.zip()
                ?: throw JsonMappingException(
                    context.parser,
                    "$src is not a string."
                )
        }
    }

    @WritingConverter
    class MongoSerializer: Converter<ResourceId, String> {
        override fun convert(src: ResourceId): String =
            src.toString()
    }

    @ReadingConverter
    class MongoDeserializer: Converter<String, ResourceId> {
        override fun convert(src: String): ResourceId =
            fromString(src)
    }
}

@NoArgsConstructor
open class ResourceContainer(
    @Transient open val id: String,
    @Transient open val resourceIds: List<ResourceId>
)

@Document(collection = "resource")
@NoArgsConstructor
abstract class Resource(
    @MongoId(FieldType.STRING) open val id: ResourceId,
    @Transient val type: String
): ViewModel {
    companion object {
        val jsonDeserializer = jsonDeserializer { src, context ->
            val type = src["type"].nullString
                ?: throw JsonMappingException(
                    context.parser,
                    "type is a required attribute of resource"
                )

            val typeClass = TryBlocking { Class.forName(type) }

            typeClass
                .map {
                    (context(src, it) as? Resource)
                        .option
                }
                .fold(
                    {
                        throw JsonMappingException(
                            context.parser,
                            "Resource of type $type is not registered as class."
                        )
                    },
                    {
                        throw JsonMappingException(
                            context.parser,
                            "Resource of type $type is not found as a child of class Resource."
                        )
                    },
                    { it }
                )
        }
    }
}

@NoArgsConstructor
data class LinkResource(
    @Transient override val id: ResourceId,
    val name: String,
    val value: String
): Resource(
    id = id,
    type = LinkResource::class.qualifiedName ?: shouldNotHappen()
) {
    override fun render(): Map<String, Any> =
        mapOf(
            "type" to "LinkResource",
            "name" to name,
            "value" to value
        )
}

@NoArgsConstructor
data class ImageResource(
    @Transient override val id: ResourceId,
    val path: String,
    val location: Location
): Resource(
    id = id,
    type = ImageResource::class.qualifiedName ?: shouldNotHappen()
) {
    enum class Location {
        ASSETS,
        EXTERNAL
    }

    override fun render(): Map<String, Any> =
        mapOf(
            "type" to "ImageResource",
            "path" to path,
            "location" to location
        )
}

@NoArgsConstructor
data class VideoResource(
    @Transient override val id: ResourceId,
    val path: String,
    val location: Location
): Resource(
    id = id,
    type = VideoResource::class.qualifiedName ?: shouldNotHappen()
) {
    enum class Location {
        ASSETS,
        EXTERNAL
    }

    override fun render(): Map<String, Any> =
        mapOf(
            "type" to "VideoResource",
            "path" to path,
            "location" to location
        )
}

@NoArgsConstructor
data class MetadataResource(
    @Transient override val id: ResourceId,
    val data: Map<String, Any>
): Resource(
    id = id,
    type = MetadataResource::class.qualifiedName ?: shouldNotHappen()
) {
    override fun render(): Map<String, Any> =
        (
            mapOf("type" to "MetadataResource")
                + data
        )
}

interface ResourceServiceDependencies: DataDependencies

context(ResourceServiceDependencies)
suspend fun resourceServiceFindAllByContainerId(containerId: String): Try<Flow<Resource>> =
    fluxTry {
        mongoTemplate
            .find(query(
                Criteria
                    .where("id")
                    .regex("^(${containerId})\\/.+\$")
            ))
    }

context(ResourceServiceDependencies)
    suspend fun resourceServiceFindAllByIdRegex(idRegex: Regex): Try<Flow<Resource>> =
    fluxTry {
        mongoTemplate
            .find(query(
                Criteria
                    .where("id")
                    .regex(idRegex.toPattern())
            ))
    }

context(ResourceServiceDependencies)
suspend fun resourceServiceSaveAll(resources: List<Resource>): Try<Flow<Resource>> =
    fluxTry {
        Flux
            .fromIterable(resources)
            .flatMap { mongoTemplate.save(it) }
    }
