package pizza.psycho.sos.config

import com.fasterxml.jackson.databind.Module
import com.fasterxml.jackson.databind.module.SimpleModule
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import pizza.psycho.sos.common.patch.Patch
import pizza.psycho.sos.common.patch.jackson.PatchDeserializer

@Configuration
class JacksonPatchConfig {
    @Bean
    fun patchModule(): Module =
        SimpleModule("PatchModule")
            .addDeserializer(Patch::class.java, PatchDeserializer())
}
