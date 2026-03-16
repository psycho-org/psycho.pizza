package pizza.psycho.sos.project.sprint.application.policy

import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import pizza.psycho.sos.common.entity.BaseEntity
import pizza.psycho.sos.common.handler.DomainException
import pizza.psycho.sos.common.patch.Patch
import pizza.psycho.sos.project.common.domain.model.vo.WorkspaceId
import pizza.psycho.sos.project.project.application.port.out.ProjectRepository
import pizza.psycho.sos.project.project.application.port.out.dto.ProjectSnapshot
import pizza.psycho.sos.project.project.application.port.out.dto.TaskAssignment
import pizza.psycho.sos.project.sprint.domain.model.entity.Sprint
import pizza.psycho.sos.project.sprint.domain.repository.SprintRepository
import java.time.Instant
import java.util.UUID

class SprintTaskPolicyTests {
    private val sprintRepository = mockk<SprintRepository>()
    private val projectRepository = mockk<ProjectRepository>()
    private val policy = SprintTaskPolicy(sprintRepository, projectRepository)

    private val workspaceId = WorkspaceId(UUID.randomUUID())
    private val sprintId = UUID.randomUUID()
    private val projectId = UUID.randomUUID()
    private val startDate = Instant.parse("2026-01-01T00:00:00Z")
    private val endDate = Instant.parse("2026-01-15T00:00:00Z")

    @Test
    fun `sprint 기간 밖 dueDate 는 프로젝트 할당 시 예외가 발생한다`() {
        val sprint = Sprint.create("Sprint", workspaceId, "Goal", startDate, endDate).withId(sprintId)
        every { sprintRepository.findActiveSprintsByProjectId(projectId, workspaceId) } returns listOf(sprint)

        assertThrows(DomainException::class.java) {
            policy.validateTaskDueDateForProject(
                projectId = projectId,
                dueDate = endDate.plusSeconds(3600),
                workspaceId = workspaceId,
            )
        }
    }

    @Test
    fun `task dueDate 변경 시 연결된 sprint 기준으로 검증한다`() {
        val taskId = UUID.randomUUID()
        val sprint = Sprint.create("Sprint", workspaceId, "Goal", startDate, endDate).withId(sprintId)
        every { projectRepository.findActiveProjectIdsByTaskIds(listOf(taskId), workspaceId) } returns
            listOf(TaskAssignment(taskId, projectId))
        every { sprintRepository.findActiveSprintsByProjectIds(listOf(projectId), workspaceId) } returns listOf(sprint)

        assertThrows(DomainException::class.java) {
            policy.validateTaskDueDateChange(
                taskId = taskId,
                dueDate = Patch.Value(endDate.plusSeconds(3600)),
                workspaceId = workspaceId,
            )
        }
    }

    @Test
    fun `backlog 전환 대상은 남아있는 프로젝트에 없는 task 만 계산한다`() {
        val sharedTaskId = UUID.randomUUID()
        val backlogTaskId = UUID.randomUUID()

        val removedProject =
            ProjectSnapshot(
                projectId = UUID.randomUUID(),
                workspaceId = workspaceId,
                name = "Removed",
                taskIds = listOf(sharedTaskId, backlogTaskId),
            )
        val remainingProject =
            ProjectSnapshot(
                projectId = UUID.randomUUID(),
                workspaceId = workspaceId,
                name = "Remaining",
                taskIds = listOf(sharedTaskId),
            )

        val result = policy.tasksMovingToBacklog(listOf(removedProject), listOf(remainingProject))

        assertEquals(setOf(backlogTaskId), result)
    }

    @Test
    fun `새로 sprint 에 들어오는 task 는 기존 sprint task 를 제외하고 계산한다`() {
        val existingTaskId = UUID.randomUUID()
        val addedTaskId = UUID.randomUUID()

        val existingProject =
            ProjectSnapshot(
                projectId = UUID.randomUUID(),
                workspaceId = workspaceId,
                name = "Existing",
                taskIds = listOf(existingTaskId),
            )
        val addedProject =
            ProjectSnapshot(
                projectId = UUID.randomUUID(),
                workspaceId = workspaceId,
                name = "Added",
                taskIds = listOf(existingTaskId, addedTaskId),
            )

        val result = policy.tasksEnteringSprint(listOf(existingProject), listOf(addedProject))

        assertEquals(setOf(addedTaskId), result)
    }
}

private fun Sprint.withId(id: UUID): Sprint =
    apply {
        val field = BaseEntity::class.java.getDeclaredField("id")
        field.isAccessible = true
        field.set(this, id)
    }
