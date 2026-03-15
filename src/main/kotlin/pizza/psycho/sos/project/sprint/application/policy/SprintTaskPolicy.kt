package pizza.psycho.sos.project.sprint.application.policy

import org.springframework.stereotype.Component
import pizza.psycho.sos.common.handler.DomainException
import pizza.psycho.sos.common.patch.Patch
import pizza.psycho.sos.project.common.domain.model.vo.WorkspaceId
import pizza.psycho.sos.project.project.application.port.out.ProjectRepository
import pizza.psycho.sos.project.project.application.port.out.dto.ProjectSnapshot
import pizza.psycho.sos.project.sprint.domain.exception.SprintErrorCode
import pizza.psycho.sos.project.sprint.domain.model.entity.Sprint
import pizza.psycho.sos.project.sprint.domain.repository.SprintRepository
import pizza.psycho.sos.project.task.application.port.out.dto.TaskSnapshot
import java.time.Instant
import java.util.UUID

@Component
class SprintTaskPolicy(
    private val sprintRepository: SprintRepository,
    private val projectRepository: ProjectRepository,
) {
    fun validateTaskAssignmentsToProject(
        projectId: UUID,
        tasks: Collection<TaskSnapshot>,
        workspaceId: WorkspaceId,
    ) {
        val activeSprints = activeSprintsForProject(projectId, workspaceId)
        tasks.forEach { task ->
            validateTaskDueDateWithinSprints(activeSprints, task.dueDate)
        }
    }

    fun validateTaskDueDateForProject(
        projectId: UUID,
        dueDate: Instant?,
        workspaceId: WorkspaceId,
    ) {
        validateTaskDueDateWithinSprints(activeSprintsForProject(projectId, workspaceId), dueDate)
    }

    fun validateTaskDueDateChange(
        taskId: UUID,
        dueDate: Patch<Instant>,
        workspaceId: WorkspaceId,
    ) {
        val changedDueDate = (dueDate as? Patch.Value)?.value ?: return
        val assignments = projectRepository.findActiveProjectIdsByTaskIds(listOf(taskId), workspaceId)
        assignments
            .map { it.projectId }
            .distinct()
            .forEach { projectId ->
                validateTaskDueDateForProject(projectId, changedDueDate, workspaceId)
            }
    }

    fun validateTasksWithinSprintPeriod(
        sprint: Sprint,
        tasks: Collection<TaskSnapshot>,
    ) {
        tasks.forEach { task ->
            val dueDate = task.dueDate ?: return@forEach
            if (!sprint.period.contains(dueDate)) {
                throw DomainException(
                    SprintErrorCode.TASK_DUE_DATE_OUTSIDE_SPRINT_PERIOD,
                    "task dueDate must be within sprint period. sprintId=${sprint.sprintId}, taskId=${task.id}",
                )
            }
        }
    }

    fun tasksEnteringSprint(
        existingProjects: Collection<ProjectSnapshot>,
        addedProjects: Collection<ProjectSnapshot>,
    ): Set<UUID> {
        val existingTaskIds = existingProjects.flatMapTo(mutableSetOf()) { it.taskIds }
        return addedProjects
            .flatMap { it.taskIds }
            .filterNot(existingTaskIds::contains)
            .toSet()
    }

    fun tasksMovingToBacklog(
        removedProjects: Collection<ProjectSnapshot>,
        remainingProjects: Collection<ProjectSnapshot>,
    ): Set<UUID> {
        val remainingTaskIds = remainingProjects.flatMapTo(mutableSetOf()) { it.taskIds }
        return removedProjects
            .flatMap { it.taskIds }
            .filterNot(remainingTaskIds::contains)
            .toSet()
    }

    private fun activeSprintsForProject(
        projectId: UUID,
        workspaceId: WorkspaceId,
    ): List<Sprint> = sprintRepository.findActiveSprintsByProjectId(projectId, workspaceId)

    private fun validateTaskDueDateWithinSprints(
        sprints: Collection<Sprint>,
        dueDate: Instant?,
    ) {
        if (dueDate == null) {
            return
        }

        sprints.forEach { sprint ->
            if (!sprint.period.contains(dueDate)) {
                throw DomainException(
                    SprintErrorCode.TASK_DUE_DATE_OUTSIDE_SPRINT_PERIOD,
                    "task dueDate must be within sprint period. sprintId=${sprint.sprintId}",
                )
            }
        }
    }
}
