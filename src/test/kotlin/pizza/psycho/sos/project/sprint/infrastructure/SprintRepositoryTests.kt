package pizza.psycho.sos.project.sprint.infrastructure

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.data.jpa.repository.config.EnableJpaAuditing
import org.springframework.test.context.ActiveProfiles
import pizza.psycho.sos.project.common.domain.model.vo.WorkspaceId
import pizza.psycho.sos.project.sprint.domain.model.entity.Sprint
import pizza.psycho.sos.project.sprint.infrastructure.persistence.SprintJpaRepository
import java.time.Instant
import java.util.UUID

@DataJpaTest
@EnableJpaAuditing
@ActiveProfiles("test")
class SprintRepositoryTests {
    @Autowired
    private lateinit var sprintRepository: SprintJpaRepository

    private val workspaceId = WorkspaceId(UUID.fromString("00000000-0000-0000-0000-000000000001"))
    private val otherWorkspaceId = WorkspaceId(UUID.fromString("00000000-0000-0000-0000-000000000002"))
    private val startDate = Instant.parse("2026-01-01T00:00:00Z")
    private val endDate = Instant.parse("2026-01-15T00:00:00Z")

    @Test
    fun `findActiveSprintByIdOrNull returns active sprint`() {
        val sprint = sprintRepository.save(Sprint.create("Sprint A", workspaceId, "goal", startDate, endDate))

        val found = sprintRepository.findActiveSprintByIdOrNull(sprint.sprintId, workspaceId)

        assertNotNull(found)
        assertEquals("Sprint A", found!!.name)
    }

    @Test
    fun `findActiveSprintByIdOrNull returns null for deleted sprint`() {
        val sprint = sprintRepository.save(Sprint.create("Sprint Deleted", workspaceId, "goal", startDate, endDate))
        sprint.delete(UUID.fromString("00000000-0000-0000-0000-000000000999"))
        sprintRepository.save(sprint)

        val found = sprintRepository.findActiveSprintByIdOrNull(sprint.sprintId, workspaceId)

        assertNull(found)
    }

    @Test
    fun `findActiveSprintByIdOrNull returns null for different workspace`() {
        val sprint = sprintRepository.save(Sprint.create("Sprint Workspace", workspaceId, "goal", startDate, endDate))

        val found = sprintRepository.findActiveSprintByIdOrNull(sprint.sprintId, otherWorkspaceId)

        assertNull(found)
    }

    @Test
    fun `save persists sprint and assigns id`() {
        val sprint = sprintRepository.save(Sprint.create("New Sprint", workspaceId, "goal", startDate, endDate))

        assertNotNull(sprint.id)
        assertEquals("New Sprint", sprint.name)
    }

    @Test
    fun `deleteById soft deletes sprint and returns 1`() {
        val sprint = sprintRepository.save(Sprint.create("Delete Sprint", workspaceId, "goal", startDate, endDate))
        val deletedBy = UUID.fromString("00000000-0000-0000-0000-000000000999")

        val count = sprintRepository.deleteById(sprint.sprintId, deletedBy, workspaceId)

        assertEquals(1, count)
        assertNull(sprintRepository.findActiveSprintByIdOrNull(sprint.sprintId, workspaceId))
    }

    @Test
    fun `deleteById returns 0 for non-existent sprint`() {
        val count =
            sprintRepository.deleteById(
                UUID.fromString("00000000-0000-0000-0000-000000000000"),
                UUID.fromString("00000000-0000-0000-0000-000000000123"),
                workspaceId,
            )

        assertEquals(0, count)
    }
}
