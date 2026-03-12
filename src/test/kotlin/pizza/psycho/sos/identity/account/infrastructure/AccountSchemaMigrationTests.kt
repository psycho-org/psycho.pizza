package pizza.psycho.sos.identity.account.infrastructure

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.data.jpa.repository.config.EnableJpaAuditing
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.ActiveProfiles

@DataJpaTest
@EnableJpaAuditing
@ActiveProfiles("test")
class AccountSchemaMigrationTests {
    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    @Test
    fun `flyway drops display_name column from accounts`() {
        val count =
            jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM INFORMATION_SCHEMA.COLUMNS
                WHERE TABLE_SCHEMA = 'PUBLIC'
                  AND TABLE_NAME = 'ACCOUNTS'
                  AND COLUMN_NAME = 'DISPLAY_NAME'
                """.trimIndent(),
                Int::class.java,
            )

        assertEquals(0, count)
    }
}
