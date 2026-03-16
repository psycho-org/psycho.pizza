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
import pizza.psycho.sos.project.project.domain.event.TasksAddedToProjectEvent
import pizza.psycho.sos.project.project.domain.event.TasksRemovedFromProjectEvent
import pizza.psycho.sos.project.sprint.domain.event.TaskAddedToSprintEvent
import pizza.psycho.sos.project.sprint.domain.event.TaskRemovedFromSprintEvent
import pizza.psycho.sos.project.sprint.domain.repository.SprintRepository
import java.time.Duration
import java.time.Instant
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
    private val emittedTaskAddedKeys = ConcurrentHashMap<TaskSprintKey, Instant>()

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handle(event: ProjectDomainEvent) {
        when (event) {
            is TaskAddedToProjectEvent ->
                handleTaskAddedToProject(
                    workspaceId = event.workspaceId,
                    projectId = event.projectId,
                    taskIds = listOf(event.taskId),
                    actorId = event.actorId,
                )

            is TasksAddedToProjectEvent ->
                handleTaskAddedToProject(
                    workspaceId = event.workspaceId,
                    projectId = event.projectId,
                    taskIds = event.taskIds,
                    actorId = event.actorId,
                )

            is TaskRemovedFromProjectEvent ->
                handleTaskRemovedFromProject(
                    workspaceId = event.workspaceId,
                    projectId = event.projectId,
                    taskIds = listOf(event.taskId),
                    actorId = event.actorId,
                )

            is TasksRemovedFromProjectEvent ->
                handleTaskRemovedFromProject(
                    workspaceId = event.workspaceId,
                    projectId = event.projectId,
                    taskIds = event.taskIds,
                    actorId = event.actorId,
                )

            is TaskProjectChangedEvent -> Unit
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    fun handle(event: TaskRemovedFromSprintEvent) {
        val key = TaskSprintKey(event.workspaceId, event.sprintId, event.taskId)
        suppressedMoveKeys.remove(key)
        emittedTaskAddedKeys.remove(key)
    }

    private fun handleTaskAddedToProject(
        workspaceId: UUID,
        projectId: UUID,
        taskIds: Collection<UUID>,
        actorId: UUID?,
    ) {
        pruneEmittedKeys()
        val workspace = WorkspaceId(workspaceId)

        val sprintIds = sprintRepository.findActiveSprintIdsByProjectId(projectId, workspace)
        if (sprintIds.isEmpty()) {
            log.debug("Skip TaskAddedToSprintEvent: project not in any active sprint. projectId={}", projectId)
            return
        }

        taskIds.distinct().forEach { taskId ->
            sprintIds.forEach sprintLoop@{ sprintId ->
                val key = TaskSprintKey(workspaceId, sprintId, taskId)
                if (suppressedMoveKeys.remove(key)) {
                    log.debug(
                        "Skip TaskAddedToSprintEvent (move within sprint). taskId={}, sprintId={}, projectId={}",
                        taskId,
                        sprintId,
                        projectId,
                    )
                    return@sprintLoop
                }
                val isNewEmission = markEmissionIfNew(key)
                if (!isNewEmission) {
                    log.debug(
                        "Skip TaskAddedToSprintEvent (already emitted). taskId={}, sprintId={}, projectId={}",
                        taskId,
                        sprintId,
                        projectId,
                    )
                    return@sprintLoop
                }
                eventPublisher.publish(
                    TaskAddedToSprintEvent(
                        workspaceId = workspaceId,
                        sprintId = sprintId,
                        taskId = taskId,
                        actorId = actorId,
                        eventId = UUID.randomUUID(),
                    ),
                )
            }
        }

        log.info(
            "TaskAddedToSprintEvent published for taskIds={}, projectId={}, sprintIds={}, workspaceId={}",
            taskIds,
            projectId,
            sprintIds,
            workspaceId,
        )
    }

    private fun handleTaskRemovedFromProject(
        workspaceId: UUID,
        projectId: UUID,
        taskIds: Collection<UUID>,
        actorId: UUID?,
    ) {
        val workspace = WorkspaceId(workspaceId)
        val sprintIds = sprintRepository.findActiveSprintIdsByProjectId(projectId, workspace)

        if (sprintIds.isEmpty()) {
            taskIds.forEach { taskId -> clearEmittedTaskAddedKeys(workspaceId, taskId) }
            log.debug("Skip TaskRemovedFromSprintEvent: project not in any active sprint. projectId={}", projectId)
            return
        }

        val remainingSprintIdsByTaskId = sprintRepository.findActiveSprintIdsByTaskIds(taskIds, workspace)

        taskIds.distinct().forEach { taskId ->
            val remainingSprintIds = remainingSprintIdsByTaskId[taskId].orEmpty()
            sprintIds.forEach sprintLoop@{ sprintId ->
                val key = TaskSprintKey(workspaceId, sprintId, taskId)
                val stillInSprint = remainingSprintIds.contains(sprintId)
                if (stillInSprint) {
                    suppressedMoveKeys.add(key)
                    log.debug(
                        "Skip TaskRemovedFromSprintEvent (task still in sprint). taskId={}, sprintId={}, projectId={}",
                        taskId,
                        sprintId,
                        projectId,
                    )
                    return@sprintLoop
                } else {
                    suppressedMoveKeys.remove(key)
                }
                eventPublisher.publish(
                    TaskRemovedFromSprintEvent(
                        workspaceId = workspaceId,
                        sprintId = sprintId,
                        taskId = taskId,
                        actorId = actorId,
                        eventId = UUID.randomUUID(),
                    ),
                )
            }
        }

        log.info(
            "TaskRemovedFromSprintEvent published for taskIds={}, projectId={}, sprintIds={}, workspaceId={}",
            taskIds,
            projectId,
            sprintIds,
            workspaceId,
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
        emittedTaskAddedKeys.keys.removeIf { it.workspaceId == workspaceId && it.taskId == taskId }
    }

    private fun markEmissionIfNew(key: TaskSprintKey): Boolean {
        val now = Instant.now()
        val previous = emittedTaskAddedKeys.putIfAbsent(key, now)
        return if (previous == null) {
            true
        } else if (previous.isBefore(now.minus(EMITTED_KEY_TTL))) {
            emittedTaskAddedKeys.replace(key, previous, now)
        } else {
            false
        }
    }

    private fun pruneEmittedKeys() {
        if (emittedTaskAddedKeys.size <= MAX_EMITTED_KEY_SIZE) {
            emittedTaskAddedKeys.entries.removeIf { (_, emittedAt) ->
                emittedAt.isBefore(Instant.now().minus(EMITTED_KEY_TTL))
            }
            return
        }
        val threshold = Instant.now().minus(EMITTED_KEY_TTL)
        emittedTaskAddedKeys.entries.removeIf { (_, emittedAt) -> emittedAt.isBefore(threshold) }
        if (emittedTaskAddedKeys.size > MAX_EMITTED_KEY_SIZE) {
            emittedTaskAddedKeys.clear()
            log.warn("Cleared emittedTaskAddedKeys due to size overflow; dedupe state reset.")
        }
    }

    companion object {
        private val EMITTED_KEY_TTL: Duration = Duration.ofHours(12)
        private const val MAX_EMITTED_KEY_SIZE: Int = 100_000
    }
}
