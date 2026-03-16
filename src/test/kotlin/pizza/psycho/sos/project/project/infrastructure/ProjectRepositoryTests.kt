package pizza.psycho.sos.project.project.infrastructure

import org.junit.jupiter.api.Assertions.assertEquals
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
import pizza.psycho.sos.project.project.infrastructure.persistence.ProjectJpaRepository
import pizza.psycho.sos.project.task.domain.model.entity.Task
import pizza.psycho.sos.project.task.domain.model.vo.Status
import pizza.psycho.sos.project.task.infrastructure.persistence.repository.TaskJpaRepository
import java.util.UUID

@DataJpaTest
@EnableJpaAuditing
@ActiveProfiles("test")
class ProjectRepositoryTests {
    @Autowired
    private lateinit var projectRepository: ProjectJpaRepository

    @Autowired
    private lateinit var taskRepository: TaskJpaRepository

    private val workspaceId = WorkspaceId(UUID.fromString("00000000-0000-0000-0000-000000000001"))

    // ------------------------------------------------------------------------------------------------
    // findActiveProjectByIdOrNull
    // ------------------------------------------------------------------------------------------------

    @Test
    fun `findActiveProjectByIdOrNull returns active project`() {
        val project =
            projectRepository.save(
                Project.create(name = "Test Project", workspaceId = workspaceId),
            )

        val found = projectRepository.findActiveProjectByIdOrNull(project.projectId, workspaceId)

        assertNotNull(found)
        assertEquals("Test Project", found!!.name)
    }

    @Test
    fun `findActiveProjectByIdOrNull returns null for soft deleted project`() {
        val project =
            projectRepository.save(
                Project.create(name = "Deleted Project", workspaceId = workspaceId),
            )
        project.delete(UUID.fromString("00000000-0000-0000-0000-000000000999"))
        projectRepository.save(project)

        val found = projectRepository.findActiveProjectByIdOrNull(project.projectId, workspaceId)

        assertNull(found)
    }

    @Test
    fun `findActiveProjectByIdOrNull returns null for non-existent id`() {
        val found =
            projectRepository.findActiveProjectByIdOrNull(
                UUID.fromString("00000000-0000-0000-0000-000000000000"),
                workspaceId,
            )

        assertNull(found)
    }

    @Test
    fun `findActiveProjectByIdOrNull returns null for different workspaceId`() {
        val project =
            projectRepository.save(
                Project.create(name = "Other Workspace Project", workspaceId = workspaceId),
            )
        val otherWorkspaceId = WorkspaceId(UUID.fromString("00000000-0000-0000-0000-000000000002"))

        val found = projectRepository.findActiveProjectByIdOrNull(project.projectId, otherWorkspaceId)

        assertNull(found)
    }

    // ------------------------------------------------------------------------------------------------
    // save
    // ------------------------------------------------------------------------------------------------

    @Test
    fun `save persists project and returns entity with id`() {
        val project =
            projectRepository.save(
                Project.create(name = "New Project", workspaceId = workspaceId),
            )

        assertNotNull(project.id)
        assertEquals("New Project", project.name)
        assertEquals(workspaceId, project.workspaceId)
    }

    // ------------------------------------------------------------------------------------------------
    // deleteById (soft delete)
    // ------------------------------------------------------------------------------------------------

    @Test
    fun `deleteById soft deletes active project and returns 1`() {
        val project =
            projectRepository.save(
                Project.create(name = "To Delete", workspaceId = workspaceId),
            )
        val deletedBy = UUID.fromString("00000000-0000-0000-0000-000000000999")

        val count = projectRepository.deleteById(project.projectId, deletedBy, workspaceId)

        assertEquals(1, count)
        val found = projectRepository.findActiveProjectByIdOrNull(project.projectId, workspaceId)
        assertNull(found)
    }

    @Test
    fun `deleteById returns 0 for non-existent project`() {
        val count =
            projectRepository.deleteById(
                UUID.fromString("00000000-0000-0000-0000-000000000000"),
                UUID.fromString("00000000-0000-0000-0000-000000000999"),
                workspaceId,
            )

        assertEquals(0, count)
    }

    @Test
    fun `deleteById returns 0 for already deleted project`() {
        val project =
            projectRepository.save(
                Project.create(name = "Already Deleted", workspaceId = workspaceId),
            )
        project.delete(UUID.fromString("00000000-0000-0000-0000-000000000999"))
        projectRepository.save(project)

        val count =
            projectRepository.deleteById(
                project.projectId,
                UUID.fromString("00000000-0000-0000-0000-000000000999"),
                workspaceId,
            )

        assertEquals(0, count)
    }

    // ------------------------------------------------------------------------------------------------
    // findProgressByProjectId
    // ------------------------------------------------------------------------------------------------

    @Test
    fun `findProgressByProjectId returns progress with task counts`() {
        val task1 =
            taskRepository.save(
                Task.create(title = "Task 1", description = "desc", workspaceId = workspaceId.value),
            )
        val task2 =
            taskRepository.save(
                Task.create(title = "Task 2", description = "desc", workspaceId = workspaceId.value),
            )
        task2.changeStatus(Status.DONE)
        taskRepository.save(task2)

        val project =
            projectRepository.save(
                Project.create(name = "Progress Project", workspaceId = workspaceId),
            )
        project.addTasks(listOf(task1.taskId, task2.taskId))
        projectRepository.save(project)

        val progress = projectRepository.findProgressByProjectId(project.projectId, workspaceId)

        assertNotNull(progress)
        assertEquals(2L, progress!!.totalCount)
        assertEquals(1L, progress.completedCount)
    }

    @Test
    fun `findProgressByProjectId returns null when project has no tasks`() {
        val project =
            projectRepository.save(
                Project.create(name = "Empty Project", workspaceId = workspaceId),
            )

        val progress = projectRepository.findProgressByProjectId(project.projectId, workspaceId)

        assertNull(progress)
    }

    // ------------------------------------------------------------------------------------------------
    // findProgressesByProjectId (batch)
    // ------------------------------------------------------------------------------------------------

    @Test
    fun `findProgressesByProjectId returns progresses for multiple projects`() {
        val task =
            taskRepository.save(
                Task.create(title = "Shared Task", description = "desc", workspaceId = workspaceId.value),
            )

        val project1 =
            projectRepository.save(
                Project.create(name = "Project A", workspaceId = workspaceId),
            )
        project1.addTask(task.taskId)
        projectRepository.save(project1)

        val project2 = projectRepository.save(Project.create(name = "Project B", workspaceId = workspaceId))
        project2.addTask(task.taskId)
        projectRepository.save(project2)

        val progresses =
            projectRepository.findProgressesByProjectId(
                listOf(project1.projectId, project2.projectId),
                workspaceId,
            )

        assertEquals(2, progresses.size)
    }

    @Test
    fun `findProgressesByProjectId returns empty list when no projects match`() {
        val progresses =
            projectRepository.findProgressesByProjectId(
                listOf(UUID.fromString("00000000-0000-0000-0000-000000000000")),
                workspaceId,
            )

        assertTrue(progresses.isEmpty())
    }
}
