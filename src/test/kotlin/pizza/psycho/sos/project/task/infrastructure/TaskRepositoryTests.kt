package pizza.psycho.sos.project.task.infrastructure

import jakarta.persistence.EntityManager
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.data.domain.PageRequest
import org.springframework.data.jpa.repository.config.EnableJpaAuditing
import org.springframework.test.context.ActiveProfiles
import pizza.psycho.sos.project.common.domain.model.vo.WorkspaceId
import pizza.psycho.sos.project.project.domain.model.entity.Project
import pizza.psycho.sos.project.sprint.domain.model.entity.Sprint
import pizza.psycho.sos.project.sprint.infrastructure.persistence.repository.SprintJpaRepository
import pizza.psycho.sos.project.task.domain.model.entity.Task
import pizza.psycho.sos.project.task.infrastructure.persistence.repository.TaskJpaRepository
import java.time.Instant
import java.util.UUID

@DataJpaTest
@EnableJpaAuditing
@ActiveProfiles("test")
class TaskRepositoryTests {
    @Autowired
    private lateinit var taskRepository: TaskJpaRepository

    @Autowired
    private lateinit var sprintRepository: SprintJpaRepository

    @Autowired
    private lateinit var entityManager: EntityManager

    private val workspaceId = WorkspaceId(UUID.fromString("00000000-0000-0000-0000-000000000001"))
    private val startDate = Instant.parse("2026-01-01T00:00:00Z")
    private val endDate = Instant.parse("2026-01-15T00:00:00Z")

    @Test
    fun `findAllActiveBacklogTasks returns only tasks outside active sprints`() {
        val backlogTask = Task.create("Backlog Task", "desc", null, workspaceId.value)
        val activeSprintTask = Task.create("Sprint Task", "desc", null, workspaceId.value)
        val deletedSprintTask = Task.create("Deleted Sprint Task", "desc", null, workspaceId.value)

        val activeProject = Project.create("Active Project", workspaceId)
        val deletedSprintProject = Project.create("Deleted Sprint Project", workspaceId)

        entityManager.persist(backlogTask)
        entityManager.persist(activeSprintTask)
        entityManager.persist(deletedSprintTask)
        entityManager.persist(activeProject)
        entityManager.persist(deletedSprintProject)
        entityManager.flush()

        activeProject.addTask(activeSprintTask.taskId)
        deletedSprintProject.addTask(deletedSprintTask.taskId)
        entityManager.flush()

        val activeSprint =
            Sprint.create("Active Sprint", workspaceId, "goal", startDate, endDate).apply {
                addProject(activeProject.projectId)
            }
        val removedSprint =
            Sprint.create("Removed Sprint", workspaceId, "goal", startDate, endDate).apply {
                addProject(deletedSprintProject.projectId)
            }

        sprintRepository.saveAndFlush(activeSprint)
        sprintRepository.saveAndFlush(removedSprint)

        removedSprint.delete(UUID.randomUUID())
        sprintRepository.saveAndFlush(removedSprint)
        entityManager.clear()

        val found =
            taskRepository.findAllActiveBacklogTasks(
                workspaceId = workspaceId,
                pageable = PageRequest.of(0, 10),
            )

        val foundIds = found.content.map { it.taskId }.toSet()
        assertEquals(setOf(backlogTask.taskId, deletedSprintTask.taskId), foundIds)
        assertTrue(activeSprintTask.taskId !in foundIds)
    }
}
