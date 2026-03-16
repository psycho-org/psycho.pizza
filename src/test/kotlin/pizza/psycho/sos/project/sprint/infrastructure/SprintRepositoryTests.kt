package pizza.psycho.sos.project.sprint.infrastructure
import jakarta.persistence.EntityManager
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.data.jpa.repository.config.EnableJpaAuditing
import org.springframework.test.context.ActiveProfiles
import pizza.psycho.sos.project.common.domain.model.vo.WorkspaceId
import pizza.psycho.sos.project.project.domain.model.entity.Project
import pizza.psycho.sos.project.sprint.domain.model.entity.Sprint
import pizza.psycho.sos.project.sprint.infrastructure.persistence.SprintJpaRepository
import pizza.psycho.sos.project.task.domain.model.entity.Task
import java.time.Instant
import java.util.UUID

@DataJpaTest
@EnableJpaAuditing
@ActiveProfiles("test")
class SprintRepositoryTests {
    @Autowired
    private lateinit var sprintRepository: SprintJpaRepository

    @Autowired
    private lateinit var entityManager: EntityManager

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

    @Test
    fun `deleted project does not count as active sprint task membership`() {
        val deletedBy = UUID.randomUUID()
        val project = Project.create("Deleted Project", workspaceId)
        val task = Task.create("Task", "Description", null, workspaceId.value)
        entityManager.persist(task)
        entityManager.persist(project)
        entityManager.flush()
        val taskId = task.taskId
        project.addTask(taskId)
        entityManager.flush()

        val sprint =
            Sprint.create("Sprint With Deleted Project", workspaceId, "goal", startDate, endDate).apply {
                addProject(project.projectId)
            }
        sprintRepository.saveAndFlush(sprint)

        project.delete(deletedBy)
        entityManager.flush()
        entityManager.clear()

        assertFalse(sprintRepository.existsActiveSprintByTaskIdAndSprintId(taskId, sprint.sprintId, workspaceId))
        assertFalse(sprintRepository.existsActiveSprintByTaskId(taskId, workspaceId))
        assertTrue(sprintRepository.findActiveSprintIdsByTaskId(taskId, workspaceId).isEmpty())
    }

    @Test
    fun `deleted task does not count as active sprint task membership`() {
        val deletedBy = UUID.randomUUID()
        val task = Task.create("Deleted Task", "Description", null, workspaceId.value)
        val project = Project.create("Project", workspaceId)
        entityManager.persist(task)
        entityManager.persist(project)
        entityManager.flush()
        val taskId = task.taskId
        project.addTask(taskId)
        entityManager.flush()

        val sprint =
            Sprint.create("Sprint With Deleted Task", workspaceId, "goal", startDate, endDate).apply {
                addProject(project.projectId)
            }
        sprintRepository.saveAndFlush(sprint)

        task.delete(deletedBy)
        entityManager.flush()
        entityManager.clear()

        assertFalse(sprintRepository.existsActiveSprintByTaskIdAndSprintId(taskId, sprint.sprintId, workspaceId))
        assertFalse(sprintRepository.existsActiveSprintByTaskId(taskId, workspaceId))
        assertTrue(sprintRepository.findActiveSprintIdsByTaskId(taskId, workspaceId).isEmpty())
    }

    @Test
    fun `deleted project is excluded from active sprint project membership queries`() {
        val deletedBy = UUID.randomUUID()
        val project = Project.create("Deleted Project", workspaceId)
        entityManager.persist(project)
        entityManager.flush()

        val sprint =
            Sprint.create("Sprint With Deleted Project", workspaceId, "goal", startDate, endDate).apply {
                addProject(project.projectId)
            }
        sprintRepository.saveAndFlush(sprint)

        assertTrue(sprintRepository.existsActiveSprintByProjectId(project.projectId, workspaceId))
        assertEquals(listOf(sprint.sprintId), sprintRepository.findActiveSprintIdsByProjectId(project.projectId, workspaceId))

        project.delete(deletedBy)
        entityManager.flush()
        entityManager.clear()

        assertFalse(sprintRepository.existsActiveSprintByProjectId(project.projectId, workspaceId))
        assertTrue(sprintRepository.findActiveSprintIdsByProjectId(project.projectId, workspaceId).isEmpty())
        assertTrue(sprintRepository.findActiveSprintsByProjectId(project.projectId, workspaceId).isEmpty())
    }

    @Test
    fun `findActiveSprintsByProjectId returns active sprints for project`() {
        val project = Project.create("Project", workspaceId)
        entityManager.persist(project)
        entityManager.flush()

        val sprintA =
            Sprint.create("Sprint A", workspaceId, "goal", startDate, endDate).apply {
                addProject(project.projectId)
            }
        val sprintB =
            Sprint.create("Sprint B", workspaceId, "goal", startDate, endDate).apply {
                addProject(project.projectId)
            }
        sprintRepository.saveAndFlush(sprintA)
        sprintRepository.saveAndFlush(sprintB)
        entityManager.clear()

        val found = sprintRepository.findActiveSprintsByProjectId(project.projectId, workspaceId)

        assertEquals(setOf(sprintA.sprintId, sprintB.sprintId), found.map { it.sprintId }.toSet())
    }

    @Test
    fun `findActiveSprintIdsByProjectIds returns sprint ids grouped by project`() {
        val projectA = Project.create("Project A", workspaceId)
        val projectB = Project.create("Project B", workspaceId)
        entityManager.persist(projectA)
        entityManager.persist(projectB)
        entityManager.flush()

        val sprintA =
            Sprint.create("Sprint A", workspaceId, "goal", startDate, endDate).apply {
                addProject(projectA.projectId)
            }
        val sprintB =
            Sprint.create("Sprint B", workspaceId, "goal", startDate, endDate).apply {
                addProjects(listOf(projectA.projectId, projectB.projectId))
            }
        sprintRepository.saveAndFlush(sprintA)
        sprintRepository.saveAndFlush(sprintB)
        entityManager.clear()

        val found =
            sprintRepository.findActiveSprintIdsByProjectIds(
                listOf(projectA.projectId, projectB.projectId),
                workspaceId,
            )

        assertEquals(setOf(sprintA.sprintId, sprintB.sprintId), found[projectA.projectId])
        assertEquals(setOf(sprintB.sprintId), found[projectB.projectId])
    }

    @Test
    fun `findActiveSprintIdsByTaskId returns active sprint ids for task`() {
        val projectA = Project.create("Project A", workspaceId)
        val projectB = Project.create("Project B", workspaceId)
        val task = Task.create("Task", "Description", null, workspaceId.value)
        entityManager.persist(task)
        entityManager.persist(projectA)
        entityManager.persist(projectB)
        entityManager.flush()
        val taskId = task.taskId
        projectA.addTask(taskId)
        projectB.addTask(taskId)
        entityManager.flush()

        val sprintA =
            Sprint.create("Sprint A", workspaceId, "goal", startDate, endDate).apply {
                addProject(projectA.projectId)
            }
        val sprintB =
            Sprint.create("Sprint B", workspaceId, "goal", startDate, endDate).apply {
                addProject(projectB.projectId)
            }
        sprintRepository.saveAndFlush(sprintA)
        sprintRepository.saveAndFlush(sprintB)
        entityManager.clear()

        val found = sprintRepository.findActiveSprintIdsByTaskId(taskId, workspaceId)

        assertEquals(setOf(sprintA.sprintId, sprintB.sprintId), found.toSet())
    }

    @Test
    fun `findActiveSprintIdsByTaskIds returns sprint ids grouped by task`() {
        val taskA = Task.create("Task A", "Description", null, workspaceId.value)
        val taskB = Task.create("Task B", "Description", null, workspaceId.value)
        val project = Project.create("Project", workspaceId)
        entityManager.persist(taskA)
        entityManager.persist(taskB)
        entityManager.persist(project)
        entityManager.flush()
        val taskIdA = taskA.taskId
        val taskIdB = taskB.taskId
        project.addTasks(listOf(taskIdA, taskIdB))
        entityManager.flush()

        val sprintA =
            Sprint.create("Sprint A", workspaceId, "goal", startDate, endDate).apply {
                addProject(project.projectId)
            }
        val sprintB =
            Sprint.create("Sprint B", workspaceId, "goal", startDate, endDate).apply {
                addProject(project.projectId)
            }
        sprintRepository.saveAndFlush(sprintA)
        sprintRepository.saveAndFlush(sprintB)
        entityManager.clear()

        val found = sprintRepository.findActiveSprintIdsByTaskIds(listOf(taskIdA, taskIdB), workspaceId)

        assertEquals(setOf(sprintA.sprintId, sprintB.sprintId), found[taskIdA])
        assertEquals(setOf(sprintA.sprintId, sprintB.sprintId), found[taskIdB])
    }
}
