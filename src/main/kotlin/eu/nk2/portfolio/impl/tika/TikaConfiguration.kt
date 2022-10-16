package eu.nk2.portfolio.impl.tika

import org.apache.tika.Tika
import org.springframework.context.support.BeanDefinitionDsl
import org.springframework.fu.kofu.configuration

private fun BeanDefinitionDsl.tikaBean() =
    bean {
        Tika()
    }

fun tikaConfig() =
    configuration {
        beans {
            tikaBean()
        }
    }
