package pizza.psycho.sos.project.task.domain.model

import org.springframework.test.context.ActiveProfiles
import pizza.psycho.sos.project.task.domain.event.TaskDueDateChangedEvent
import pizza.psycho.sos.project.task.domain.exception.InvalidDueDateException
import pizza.psycho.sos.project.task.domain.model.entity.Task
import pizza.psycho.sos.project.task.domain.model.vo.AssigneeId
import pizza.psycho.sos.project.task.domain.model.vo.Status
import pizza.psycho.sos.project.task.domain.model.vo.TaskDueDate
import java.time.Instant
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

@ActiveProfiles("test")
class TaskTests {
    private val workspaceId = UUID.randomUUID()
    private val assigneeId = UUID.randomUUID()

    private fun createTask(
        title: String = "Test Task",
        description: String = "Test Description",
        assigneeId: UUID? = this.assigneeId,
        workspaceId: UUID = this.workspaceId,
        dueDate: Instant? = null,
    ): Task =
        Task
            .create(
                title = title,
                description = description,
                assigneeId = assigneeId,
                workspaceId = workspaceId,
                dueDate = dueDate,
            ).apply {
                // 테스트에서는 영속된 엔티티 상황을 가정하고 도메인 이벤트 발행을 검증하기 위해
                // ID를 임의로 세팅한다.
                id = UUID.randomUUID()
            }

    @Test
    fun `create - 태스크 필드가 올바르게 설정된다`() {
        val task = createTask()

        assertEquals("Test Task", task.title)
        assertEquals("Test Description", task.description)
        assertEquals(Status.TODO, task.status)
        assertEquals(AssigneeId(assigneeId), task.assigneeId)
        assertEquals(workspaceId, task.workspaceId.value)
    }

    @Test
    fun `create - 기본 상태가 TODO로 설정된다`() {
        val task = createTask()

        assertEquals(Status.TODO, task.status)
    }

    @Test
    fun `create - 기본 마감일이 null로 설정된다`() {
        val task = createTask()

        assertNull(task.dueDate.value)
    }

    @Test
    fun `modify - 제목과 설명이 수정된다`() {
        val task = createTask()

        task.modify(title = "Updated Title", description = "Updated Description")

        assertEquals("Updated Title", task.title)
        assertEquals("Updated Description", task.description)
    }

    @Test
    fun `modify - null 전달 시 기존 값이 유지된다`() {
        val task = createTask()

        task.modify(title = null, description = null)

        assertEquals("Test Task", task.title)
        assertEquals("Test Description", task.description)
    }

    @Test
    fun `modify - 설명이 null이면 제목만 수정된다`() {
        val task = createTask()

        task.modify(title = "New Title")

        assertEquals("New Title", task.title)
        assertEquals("Test Description", task.description)
    }

    @Test
    fun `assign - 담당자가 배정된다`() {
        val task = createTask()
        val newAssigneeId = UUID.randomUUID()

        task.assign(newAssigneeId, assigneeId)

        assertEquals(AssigneeId(newAssigneeId), task.assigneeId)
    }

    @Test
    fun `unassign - 담당자가 해제된다`() {
        val task = createTask()

        task.unassign(assigneeId)

        assertEquals(AssigneeId.empty(), task.assigneeId)
        assertNull(task.assigneeId.value)
    }

    @Test
    fun `changeStatus - 상태가 변경된다`() {
        val task = createTask()

        task.changeStatus(Status.IN_PROGRESS, assigneeId)
        assertEquals(Status.IN_PROGRESS, task.status)

        task.changeStatus(Status.DONE, assigneeId)
        assertEquals(Status.DONE, task.status)

        task.changeStatus(Status.CANCELLED, assigneeId)
        assertEquals(Status.CANCELLED, task.status)
    }

    @Test
    fun `changeDueDate - 마감일이 설정된다`() {
        val task = createTask()
        val dueDate = Instant.now().plusSeconds(3600)

        task.changeDueDate(dueDate, assigneeId)

        assertEquals(dueDate, task.dueDate.value)
    }

    @Test
    fun `changeDueDate - 과거 마감일은 거부된다`() {
        val task = createTask()

        assertFailsWith<InvalidDueDateException> {
            task.changeDueDate(Instant.now().minusSeconds(3600), assigneeId)
        }
    }

    @Test
    fun `clearDueDate - 마감일이 초기화된다`() {
        val task = createTask()
        val dueDate = Instant.now().plusSeconds(3600)
        task.changeDueDate(dueDate, assigneeId)

        task.clearDueDate()

        assertNull(task.dueDate.value)
        val event = task.domainEvents().last() as TaskDueDateChangedEvent
        assertEquals(workspaceId, event.workspaceId)
        assertEquals(task.taskId, event.taskId)
        assertEquals(dueDate, event.fromDueDate)
        assertNull(event.toDueDate)
    }
}

@ActiveProfiles("test")
class AssigneeIdTests {
    @Test
    fun `empty - null 값을 가진 AssigneeId가 생성된다`() {
        val assigneeId = AssigneeId.empty()

        assertNull(assigneeId.value)
    }

    @Test
    fun `AssigneeId - UUID 값을 보유한다`() {
        val uuid = UUID.randomUUID()
        val assigneeId = AssigneeId(uuid)

        assertEquals(uuid, assigneeId.value)
    }
}

@ActiveProfiles("test")
class TaskDueDateTests {
    @Test
    fun `기본 TaskDueDate는 null 값을 가진다`() {
        val dueDate = TaskDueDate()

        assertNull(dueDate.value)
    }

    @Test
    fun `withValidation - 미래 날짜를 허용한다`() {
        val future = Instant.now().plusSeconds(3600)
        val dueDate = TaskDueDate.withValidation(future)

        assertEquals(future, dueDate.value)
    }

    @Test
    fun `withValidation - 과거 날짜는 거부된다`() {
        val past = Instant.now().minusSeconds(3600)

        assertFailsWith<InvalidDueDateException> {
            TaskDueDate.withValidation(past)
        }
    }

    @Test
    fun `withValidation - null이면 빈 TaskDueDate가 생성된다`() {
        val dueDate = TaskDueDate.withValidation(null)

        assertNull(dueDate.value)
    }
}
