package eu.nk2.portfolio.util.context

import org.springframework.context.support.AbstractApplicationContext
import org.springframework.context.support.BeanDefinitionDsl
import org.springframework.core.io.ResourceLoader
import org.springframework.fu.kofu.AbstractDsl
import kotlin.reflect.full.memberProperties

fun AbstractDsl.resolveContext(): AbstractApplicationContext {
    // HACK: https://github.com/spring-projects-experimental/spring-fu/issues/374
    return this::class
        .memberProperties
        .find { it.name == "context" }
        ?.call(this)
        as? AbstractApplicationContext
        ?: throw IllegalStateException("Is this AbstractDsl? Seems that context does not exist or was never properly initialized.")
}

fun BeanDefinitionDsl.resolveResourceLoader(): ResourceLoader {
    return this::class
        .memberProperties
        .find { it.name == "context" }
        ?.call(this)
        as? ResourceLoader
        ?: throw IllegalStateException("Is this BeanDefinitionDsl? Seems that context does not exist or was never properly initialized.")
}
