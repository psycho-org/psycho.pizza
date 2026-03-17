package pizza.psycho.sos.identity.challenge.support

import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.PostgreSQLContainer

abstract class PostgresTestContainerSupport {
    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    @BeforeEach
    fun resetDatabaseState() {
        val tableNames =
            jdbcTemplate.queryForList(
                """
                select quote_ident(tablename)
                from pg_tables
                where schemaname = 'public'
                  and tablename not in ('flyway_schema_history', 'mail_templates')
                order by tablename
                """.trimIndent(),
                String::class.java,
            )

        if (tableNames.isNotEmpty()) {
            jdbcTemplate.execute("TRUNCATE TABLE ${tableNames.joinToString(", ")} RESTART IDENTITY CASCADE")
        }
    }

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
