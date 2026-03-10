package pizza.psycho.sos.project.project.domain.model

import org.springframework.test.context.ActiveProfiles
import pizza.psycho.sos.common.handler.DomainException
import pizza.psycho.sos.project.common.domain.model.vo.WorkspaceId
import pizza.psycho.sos.project.project.domain.model.entity.Project
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@ActiveProfiles("test")
class ProjectTests {
    private val workspaceId = WorkspaceId(UUID.randomUUID())

    private fun createProject(
        name: String = "Test Project",
        workspaceId: WorkspaceId = this.workspaceId,
    ): Project = Project.create(name = name, workspaceId = workspaceId)

    // ------------------------------------------------------------------------------------------------
    // create
    // ------------------------------------------------------------------------------------------------

    @Test
    fun `create - 프로젝트 필드가 올바르게 설정된다`() {
        val project = createProject()

        assertEquals("Test Project", project.name)
        assertEquals(workspaceId, project.workspaceId)
    }

    @Test
    fun `create - 빈 이름으로 생성 시 예외가 발생한다`() {
        assertFailsWith<DomainException> {
            createProject(name = "")
        }
    }

    @Test
    fun `create - 공백만 있는 이름으로 생성 시 예외가 발생한다`() {
        assertFailsWith<DomainException> {
            createProject(name = "   ")
        }
    }

    @Test
    fun `create - 초기 태스크 목록이 비어있다`() {
        val project = createProject()

        assertTrue(project.taskIds().isEmpty())
    }

    // ------------------------------------------------------------------------------------------------
    // modify
    // ------------------------------------------------------------------------------------------------

    @Test
    fun `modify - 이름이 수정된다`() {
        val project = createProject()

        project.modify("Updated Name")

        assertEquals("Updated Name", project.name)
    }

    @Test
    fun `modify - 빈 이름으로 수정 시 예외가 발생한다`() {
        val project = createProject()

        assertFailsWith<DomainException> {
            project.modify("")
        }
    }

    @Test
    fun `modify - 공백만 있는 이름으로 수정 시 예외가 발생한다`() {
        val project = createProject()

        assertFailsWith<DomainException> {
            project.modify("   ")
        }
    }

    // ------------------------------------------------------------------------------------------------
    // addTask / addTasks
    // ------------------------------------------------------------------------------------------------

    @Test
    fun `addTask - 태스크가 추가된다`() {
        val project = createProject()
        val taskId = UUID.randomUUID()

        project.addTask(taskId)

        assertTrue(project.hasTask(taskId))
        assertEquals(1, project.taskIds().size)
    }

    @Test
    fun `addTask - 동일한 태스크를 중복 추가해도 하나만 존재한다`() {
        val project = createProject()
        val taskId = UUID.randomUUID()

        project.addTask(taskId)
        project.addTask(taskId)

        assertEquals(1, project.taskIds().size)
    }

    @Test
    fun `addTasks - 여러 태스크가 한꺼번에 추가된다`() {
        val project = createProject()
        val taskId1 = UUID.randomUUID()
        val taskId2 = UUID.randomUUID()

        project.addTasks(listOf(taskId1, taskId2))

        assertEquals(2, project.taskIds().size)
        assertTrue(project.hasTask(taskId1))
        assertTrue(project.hasTask(taskId2))
    }

    // ------------------------------------------------------------------------------------------------
    // removeTask / removeTasks
    // ------------------------------------------------------------------------------------------------

    @Test
    fun `removeTask - 태스크가 제거된다`() {
        val project = createProject()
        val taskId = UUID.randomUUID()
        project.addTask(taskId)

        project.removeTask(taskId)

        assertFalse(project.hasTask(taskId))
        assertTrue(project.taskIds().isEmpty())
    }

    @Test
    fun `removeTask - 존재하지 않는 태스크 제거 시 예외 없이 무시된다`() {
        val project = createProject()

        project.removeTask(UUID.randomUUID())

        assertTrue(project.taskIds().isEmpty())
    }

    @Test
    fun `removeTasks - 여러 태스크가 한꺼번에 제거된다`() {
        val project = createProject()
        val taskId1 = UUID.randomUUID()
        val taskId2 = UUID.randomUUID()
        val taskId3 = UUID.randomUUID()
        project.addTasks(listOf(taskId1, taskId2, taskId3))

        project.removeTasks(listOf(taskId1, taskId3))

        assertEquals(1, project.taskIds().size)
        assertFalse(project.hasTask(taskId1))
        assertTrue(project.hasTask(taskId2))
        assertFalse(project.hasTask(taskId3))
    }

    // ------------------------------------------------------------------------------------------------
    // hasTask
    // ------------------------------------------------------------------------------------------------

    @Test
    fun `hasTask - 존재하는 태스크에 대해 true를 반환한다`() {
        val project = createProject()
        val taskId = UUID.randomUUID()
        project.addTask(taskId)

        assertTrue(project.hasTask(taskId))
    }

    @Test
    fun `hasTask - 존재하지 않는 태스크에 대해 false를 반환한다`() {
        val project = createProject()

        assertFalse(project.hasTask(UUID.randomUUID()))
    }

    // ------------------------------------------------------------------------------------------------
    // taskIds
    // ------------------------------------------------------------------------------------------------

    @Test
    fun `taskIds - 추가된 태스크 ID 목록을 반환한다`() {
        val project = createProject()
        val taskId1 = UUID.randomUUID()
        val taskId2 = UUID.randomUUID()
        project.addTasks(listOf(taskId1, taskId2))

        val ids = project.taskIds()

        assertEquals(2, ids.size)
        assertTrue(ids.containsAll(listOf(taskId1, taskId2)))
    }
}
