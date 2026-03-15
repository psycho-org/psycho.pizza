package pizza.psycho.sos.project.sprint.application.event.handler

import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener
import pizza.psycho.sos.common.event.DomainEventPublisher
import pizza.psycho.sos.common.support.log.loggerDelegate
import pizza.psycho.sos.project.common.domain.model.vo.WorkspaceId
import pizza.psycho.sos.project.project.domain.event.ProjectDomainEvent
import pizza.psycho.sos.project.project.domain.event.TaskAddedToProjectEvent
import pizza.psycho.sos.project.project.domain.event.TaskProjectChangedEvent
import pizza.psycho.sos.project.project.domain.event.TaskRemovedFromProjectEvent
import pizza.psycho.sos.project.sprint.domain.event.TaskAddedToSprintEvent
import pizza.psycho.sos.project.sprint.domain.event.TaskRemovedFromSprintEvent
import pizza.psycho.sos.project.sprint.domain.repository.SprintRepository
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * 프로젝트에 Task가 추가/제거될 때, 해당 프로젝트가 속한 Sprint들에 대해
 * TaskAddedToSprintEvent / TaskRemovedFromSprintEvent 를 발생시키는 애플리케이션 핸들러.
 *
 * - Project 도메인은 Sprint 존재를 모르고, 단순히 TaskAddedToProjectEvent / TaskRemovedFromProjectEvent 만 발생시킨다.
 * - Sprint 쪽에서 Project 이벤트를 구독해서, 실제 Sprint 관점의 Task 추가/제거 이벤트를 생성한다.
 */
@Component
class SprintTaskInProjectDomainEventHandler(
    private val sprintRepository: SprintRepository,
    private val eventPublisher: DomainEventPublisher,
) {
    private val log by loggerDelegate()
    private val suppressedMoveKeys = ConcurrentHashMap.newKeySet<TaskSprintKey>()
    private val emittedTaskAddedKeys = ConcurrentHashMap.newKeySet<TaskSprintKey>()

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handle(event: ProjectDomainEvent) {
        when (event) {
            is TaskAddedToProjectEvent -> handleTaskAddedToProject(event)
            is TaskRemovedFromProjectEvent -> handleTaskRemovedFromProject(event)
            is TaskProjectChangedEvent -> Unit
        }
    }

    private fun handleTaskAddedToProject(event: TaskAddedToProjectEvent) {
        val workspaceId = WorkspaceId(event.workspaceId)

        val sprintIds = sprintRepository.findActiveSprintIdsByProjectId(event.projectId, workspaceId)
        if (sprintIds.isEmpty()) {
            log.debug("Skip TaskAddedToSprintEvent: project not in any active sprint. event={}", event)
            return
        }

        sprintIds.forEach { sprintId ->
            val key = TaskSprintKey(event.workspaceId, sprintId, event.taskId)
            if (suppressedMoveKeys.remove(key)) {
                log.debug(
                    "Skip TaskAddedToSprintEvent (move within sprint). taskId={}, sprintId={}, projectId={}",
                    event.taskId,
                    sprintId,
                    event.projectId,
                )
                return@forEach
            }
            val isNewEmission = emittedTaskAddedKeys.add(key)
            if (!isNewEmission) {
                log.debug(
                    "Skip TaskAddedToSprintEvent (already emitted). taskId={}, sprintId={}, projectId={}",
                    event.taskId,
                    sprintId,
                    event.projectId,
                )
                return@forEach
            }
            eventPublisher.publish(
                TaskAddedToSprintEvent(
                    workspaceId = event.workspaceId,
                    sprintId = sprintId,
                    taskId = event.taskId,
                    actorId = event.actorId,
                    eventId = UUID.randomUUID(),
                ),
            )
        }

        log.info(
            "TaskAddedToSprintEvent published for taskId={}, projectId={}, sprintIds={}, workspaceId={}",
            event.taskId,
            event.projectId,
            sprintIds,
            event.workspaceId,
        )
    }

    private fun handleTaskRemovedFromProject(event: TaskRemovedFromProjectEvent) {
        val workspaceId = WorkspaceId(event.workspaceId)
        val sprintIds = sprintRepository.findActiveSprintIdsByProjectId(event.projectId, workspaceId)

        if (sprintIds.isEmpty()) {
            clearEmittedTaskAddedKeys(event.workspaceId, event.taskId)
            log.debug("Skip TaskRemovedFromSprintEvent: project not in any active sprint. event={}", event)
            return
        }

        sprintIds.forEach { sprintId ->
            val key = TaskSprintKey(event.workspaceId, sprintId, event.taskId)
            val stillInSprint =
                sprintRepository.existsActiveSprintByTaskIdAndSprintId(
                    taskId = event.taskId,
                    sprintId = sprintId,
                    workspaceId = workspaceId,
                )
            if (stillInSprint) {
                suppressedMoveKeys.add(key)
                log.debug(
                    "Skip TaskRemovedFromSprintEvent (task still in sprint). taskId={}, sprintId={}, projectId={}",
                    event.taskId,
                    sprintId,
                    event.projectId,
                )
                return@forEach
            } else {
                suppressedMoveKeys.remove(key)
            }
            eventPublisher.publish(
                TaskRemovedFromSprintEvent(
                    workspaceId = event.workspaceId,
                    sprintId = sprintId,
                    taskId = event.taskId,
                    actorId = event.actorId,
                    eventId = UUID.randomUUID(),
                ),
            )
            emittedTaskAddedKeys.remove(key)
        }

        log.info(
            "TaskRemovedFromSprintEvent published for taskId={}, projectId={}, sprintIds={}, workspaceId={}",
            event.taskId,
            event.projectId,
            sprintIds,
            event.workspaceId,
        )
    }

    private data class TaskSprintKey(
        val workspaceId: UUID,
        val sprintId: UUID,
        val taskId: UUID,
    )

    private fun clearEmittedTaskAddedKeys(
        workspaceId: UUID,
        taskId: UUID,
    ) {
        emittedTaskAddedKeys.removeIf { it.workspaceId == workspaceId && it.taskId == taskId }
    }
}
