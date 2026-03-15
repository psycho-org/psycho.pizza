package pizza.psycho.sos.project.sprint.domain.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import pizza.psycho.sos.common.handler.DomainException
import pizza.psycho.sos.project.common.domain.model.vo.WorkspaceId
import pizza.psycho.sos.project.sprint.domain.event.SprintPeriodChangedEvent
import pizza.psycho.sos.project.sprint.domain.event.TaskAddedToSprintEvent
import pizza.psycho.sos.project.sprint.domain.event.TaskRemovedFromSprintEvent
import pizza.psycho.sos.project.sprint.domain.model.entity.Sprint
import java.time.Instant
import java.util.UUID

class SprintTests {
    private val workspaceId = WorkspaceId(UUID.randomUUID())
    private val startDate = Instant.parse("2026-01-01T00:00:00Z")
    private val endDate = Instant.parse("2026-01-15T00:00:00Z")

    private fun createSprint(
        name: String = "스프린트 A",
        workspaceId: WorkspaceId = this.workspaceId,
        goal: String? = "목표 A",
        start: Instant = startDate,
        end: Instant = endDate,
    ): Sprint =
        Sprint.create(name, workspaceId, goal, start, end).apply {
            // 도메인 이벤트 발행 등에서 sprintId 사용 가능하도록 테스트용 ID 세팅
            id = UUID.randomUUID()
        }

    @Test
    fun `스프린트 생성 시 필드가 정상적으로 설정된다`() {
        val sprint = createSprint()

        assertEquals("스프린트 A", sprint.name)
        assertEquals(workspaceId, sprint.workspaceId)
        assertEquals(startDate, sprint.period.startDate)
        assertEquals(endDate, sprint.period.endDate)
    }

    @Test
    fun `modify 호출 시 이름이 변경된다`() {
        val sprint = createSprint()

        sprint.modify("스프린트 B")

        assertEquals("스프린트 B", sprint.name)
    }

    @Test
    fun `goal 이 공백이면 생성에 실패한다`() {
        assertThrows(DomainException::class.java) {
            createSprint(goal = "   ")
        }
    }

    @Test
    fun `빈 문자열로 modify 호출 시 DomainException이 발생한다`() {
        val sprint = createSprint()

        assertThrows(DomainException::class.java) {
            sprint.modify("")
        }
    }

    @Test
    fun `addProject는 동일한 프로젝트를 중복으로 추가하지 않는다`() {
        val sprint = createSprint()
        val projectId = UUID.randomUUID()

        sprint.addProject(projectId)
        sprint.addProject(projectId)

        assertEquals(1, sprint.projectIds().size)
        assertTrue(sprint.hasProject(projectId))
    }

    @Test
    fun `removeProject 호출 시 해당 프로젝트 연결이 제거된다`() {
        val sprint = createSprint()
        val projectId1 = UUID.randomUUID()
        val projectId2 = UUID.randomUUID()

        sprint.addProjects(listOf(projectId1, projectId2))

        sprint.removeProject(projectId1)

        assertFalse(sprint.hasProject(projectId1))
        assertTrue(sprint.hasProject(projectId2))
    }

    @Test
    fun `changePeriod 호출 시 기간이 업데이트된다`() {
        val sprint = createSprint()
        val newStart = Instant.parse("2026-01-05T00:00:00Z")
        val newEnd = Instant.parse("2026-01-20T00:00:00Z")
        val updatedBy = UUID.randomUUID()

        sprint.changePeriod(startDate = newStart, endDate = newEnd, updatedBy)

        assertEquals(newStart, sprint.period.startDate)
        assertEquals(newEnd, sprint.period.endDate)
    }

    @Test
    fun `changePeriod 호출 시 기간이 동일하면 변경 이벤트를 발행하지 않는다`() {
        val sprint = createSprint()
        val updatedBy = UUID.randomUUID()

        sprint.changePeriod(
            startDate = startDate,
            endDate = endDate,
            by = updatedBy,
        )

        assertTrue(sprint.domainEvents().none { it is SprintPeriodChangedEvent })
    }

    @Test
    fun `프로젝트 추가 시 새로 sprint 에 들어온 task 이벤트를 등록한다`() {
        val sprint = createSprint()
        val actorId = UUID.randomUUID()
        val projectId = UUID.randomUUID()
        val taskId = UUID.randomUUID()

        sprint.addProjects(listOf(projectId), listOf(taskId), actorId)

        assertTrue(sprint.domainEvents().any { it is TaskAddedToSprintEvent })
    }

    @Test
    fun `프로젝트 제거 시 backlog 로 이동한 task 이벤트를 등록한다`() {
        val sprint = createSprint()
        val actorId = UUID.randomUUID()
        val projectId = UUID.randomUUID()
        val taskId = UUID.randomUUID()
        sprint.addProject(projectId)

        sprint.removeProjects(listOf(projectId), listOf(taskId), actorId)

        assertTrue(sprint.domainEvents().any { it is TaskRemovedFromSprintEvent })
    }
}
