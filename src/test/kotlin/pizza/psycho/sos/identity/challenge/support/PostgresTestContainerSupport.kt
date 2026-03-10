package pizza.psycho.sos.identity.challenge.support

import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.PostgreSQLContainer

abstract class PostgresTestContainerSupport {
    companion object {
        private val postgres =
            PostgreSQLContainer("postgres:16-alpine")
                .apply {
                    withDatabaseName("psycho_test")
                    withUsername("postgres")
                    withPassword("postgres")
                    start()
                }

        @JvmStatic
        @DynamicPropertySource
        fun registerDataSourceProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url", postgres::getJdbcUrl)
            registry.add("spring.datasource.username", postgres::getUsername)
            registry.add("spring.datasource.password", postgres::getPassword)
            registry.add("spring.datasource.driver-class-name") { "org.postgresql.Driver" }
        }
    }
}
