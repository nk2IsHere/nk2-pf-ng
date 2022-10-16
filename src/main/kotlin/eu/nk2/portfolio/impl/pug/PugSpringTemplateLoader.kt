package eu.nk2.portfolio.impl.pug

import de.neuland.pug4j.template.TemplateLoader
import org.apache.commons.io.FilenameUtils
import org.springframework.core.io.Resource
import org.springframework.core.io.ResourceLoader
import java.io.InputStreamReader
import java.io.Reader


internal class PugSpringTemplateLoader(
    private val resourceLoader: ResourceLoader,
    private val templateLoaderPath: String,
    private val templateExtension: String,
    private val basePath: String = ""
): TemplateLoader {

    private fun getResourceNameWithExtension(name: String): String {
        val resourceName = FilenameUtils.separatorsToUnix(name)
        val resourceWithPath = FilenameUtils.normalize(
            if(templateLoaderPath.endsWith("/")) templateLoaderPath else "${templateLoaderPath}/"
                + if(basePath.endsWith("/")) basePath else "${basePath}/"
                + if(name.startsWith("/")) resourceName.drop(1) else resourceName,
            true
        )

        return if(FilenameUtils.getExtension(resourceWithPath) == "") "${resourceWithPath}.${extension}"
        else resourceWithPath
    }

    override fun getLastModified(name: String): Long =
        resourceLoader
            .getResource(getResourceNameWithExtension(name))
            .lastModified()

    override fun getReader(name: String): Reader =
        InputStreamReader(
            resourceLoader
                .getResource(getResourceNameWithExtension(name))
                .inputStream
        )

    override fun getExtension(): String =
        if(templateExtension.startsWith(".")) templateExtension.drop(1)
        else templateExtension

    override fun getBase(): String =
        basePath
}
