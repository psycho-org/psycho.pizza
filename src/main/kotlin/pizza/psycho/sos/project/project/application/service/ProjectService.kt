package pizza.psycho.sos.project.project.application.service

import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.stereotype.Service
import pizza.psycho.sos.common.event.DomainEventPublisher
import pizza.psycho.sos.common.support.log.loggerDelegate
import pizza.psycho.sos.common.support.transaction.helper.Tx
import pizza.psycho.sos.project.common.domain.model.vo.WorkspaceId
import pizza.psycho.sos.project.project.application.port.out.ProjectRepository
import pizza.psycho.sos.project.project.application.port.out.ProjectSprintParticipationQuery
import pizza.psycho.sos.project.project.application.port.out.dto.TaskAssignment
import pizza.psycho.sos.project.project.application.port.out.query.ProjectProgress
import pizza.psycho.sos.project.project.application.service.dto.ProjectCommand
import pizza.psycho.sos.project.project.application.service.dto.ProjectQuery
import pizza.psycho.sos.project.project.application.service.dto.ProjectResult
import pizza.psycho.sos.project.project.domain.model.entity.Project
import pizza.psycho.sos.project.sprint.application.policy.SprintTaskPolicy
import pizza.psycho.sos.project.sprint.domain.event.TaskRemovedFromSprintEvent
import pizza.psycho.sos.project.task.application.port.out.TaskPort
import pizza.psycho.sos.project.task.application.port.out.dto.SprintTaskMembershipSnapshot
import pizza.psycho.sos.project.task.application.port.out.dto.TaskSnapshot
import java.util.UUID

@Service
class ProjectService(
    private val projectRepository: ProjectRepository,
    private val eventPublisher: DomainEventPublisher,
    private val taskPort: TaskPort,
    private val sprintTaskPolicy: SprintTaskPolicy,
    private val sprintParticipationQuery: ProjectSprintParticipationQuery,
) {
    private val log by loggerDelegate()

    /*
     * todo: 워크스페이스의 모든 프로젝트를 찾는 로직 추가
     */

    fun getProject(command: ProjectQuery.Find): ProjectResult =
        Tx.readable {
            log.debug("getProject: projectId={}, workspaceId={}", command.projectId, command.workspaceId)

            val project =
                projectRepository.findActiveProjectByIdOrNull(command.projectId, command.workspaceId)
                    ?: run {
                        log.warn("getProject: project not found. projectId=${command.projectId}")
                        return@readable ProjectResult.Failure.IdNotFound
                    }

            val progress =
                projectRepository.findProgressByProjectId(command.projectId, command.workspaceId)
                    ?: ProjectProgress(project.projectId, 0L, 0L)

            project
                .toResult(progress)
                .also { log.info("getProject success: projectId=${command.projectId}") }
        }

    fun getTasksInProject(command: ProjectQuery.FindTasksInProject): ProjectResult =
        Tx.readable {
            log.debug("getTasksInProject: projectId={}, workspaceId={}", command.projectId, command.workspaceId)

            val project =
                projectRepository.findActiveProjectByIdOrNull(command.projectId, command.workspaceId)
                    ?: run {
                        log.warn("getTasksInProject: project not found. projectId={}", command.projectId)
                        return@readable ProjectResult.Failure.IdNotFound
                    }

            val taskIdsPage =
                projectRepository.findActiveTaskIdsByProjectId(
                    projectId = project.projectId,
                    workspaceId = command.workspaceId,
                    pageable = command.pageable,
                )
            if (taskIdsPage.isEmpty) {
                return@readable ProjectResult.TaskList(PageImpl(emptyList(), command.pageable, 0))
            }

            val tasksById =
                taskPort
                    .findByIdIn(
                        ids = taskIdsPage.content,
                        workspaceId = command.workspaceId,
                    ).associateBy(TaskSnapshot::id)

            ProjectResult
                .TaskList(
                    PageImpl(
                        taskIdsPage.content.mapNotNull(tasksById::get).map { it.toResult() },
                        command.pageable,
                        taskIdsPage.totalElements,
                    ),
                ).also { log.info("getTasksInProject success: projectId=${command.projectId}") }
        }

    fun create(command: ProjectCommand.Create): ProjectResult =
        Tx.writable {
            val project = Project.create(workspaceId = command.workspaceId, name = command.name)
            val saved = projectRepository.save(project)
            saved.toResult().also { log.info("create success: projectId=${saved.projectId}") }
        }

    fun createTask(command: ProjectCommand.CreateTask): ProjectResult =
        Tx.writable {
            val project =
                projectRepository.findActiveProjectByIdOrNull(command.projectId, command.workspaceId)
                    ?: run {
                        log.warn("createTask: project not found. projectId={}", command.projectId)
                        return@writable ProjectResult.Failure.IdNotFound
                    }

            sprintTaskPolicy.validateTaskDueDateForProject(command.projectId, command.dueDate, command.workspaceId)
            val task =
                taskPort.createTask(
                    workspaceId = command.workspaceId.value,
                    title = command.title,
                    description = command.description,
                    assigneeId = command.assigneeId,
                    dueDate = command.dueDate,
                )
            project.addTask(task.id, command.createdBy)
            eventPublisher.publishAndClear(project)
            log.info("createTask success: projectId=${command.projectId}, taskId=${task.id}")
            task.toResult()
        }

    fun remove(command: ProjectCommand.Remove): ProjectResult =
        Tx.writable {
            val project =
                projectRepository.findActiveProjectByIdOrNull(command.projectId, command.workspaceId)
                    ?: run {
                        log.warn("remove: project not found. projectId={}", command.projectId)
                        return@writable ProjectResult.Failure.IdNotFound
                    }
            val activeTaskIds = projectRepository.findActiveTaskIdsByProjectId(project.projectId, command.workspaceId)
            val assignments = assignmentsByTaskIds(activeTaskIds, command.workspaceId)
            val deletableTaskIds = deletableTaskIds(activeTaskIds, setOf(project.projectId), assignments)
            val removedSprintIds =
                sprintParticipationQuery.findActiveSprintIdsByProjectId(project.projectId, command.workspaceId.value)
            val (sprintIdsByProjectId, taskIdsMovingToBacklog) =
                if (removedSprintIds.isEmpty()) {
                    emptyMap<UUID, Set<UUID>>() to emptySet()
                } else {
                    val remainingProjectIds =
                        assignments
                            .map(TaskAssignment::projectId)
                            .filterNot { it == project.projectId }
                            .toSet()
                    val sprintIdsByProjectId =
                        sprintParticipationQuery.findActiveSprintIdsByProjectIds(remainingProjectIds, command.workspaceId.value)
                    sprintIdsByProjectId to
                        sprintTaskPolicy.tasksMovingToBacklog(
                            candidateTaskIds = activeTaskIds,
                            deletableTaskIds = deletableTaskIds,
                            assignments = assignments,
                            removedProjectIds = setOf(project.projectId),
                            sprintIdsByProjectId = sprintIdsByProjectId,
                        )
                }
            if (taskIdsMovingToBacklog.isNotEmpty()) {
                taskPort.moveSprintTasksToBacklog(
                    taskIdsMovingToBacklog,
                    command.deletedBy,
                    command.workspaceId,
                    SprintTaskMembershipSnapshot.of(taskIdsMovingToBacklog),
                )
            }
            publishTaskRemovedFromSprintEvents(
                taskIds = activeTaskIds,
                assignments = assignments,
                workspaceId = command.workspaceId,
                removedProjectId = project.projectId,
                removedSprintIds = removedSprintIds,
                sprintIdsByProjectId = sprintIdsByProjectId,
                actorId = command.deletedBy,
            )
            val deletedTaskCount =
                if (deletableTaskIds.isEmpty()) {
                    0
                } else {
                    taskPort
                        .deleteByIdIn(deletableTaskIds, command.deletedBy, command.workspaceId)
                        .also {
                            log.info(
                                "remove: tasks soft-deleted. count={}, projectId={}",
                                it,
                                command.projectId,
                            )
                        }
                }

            project.delete(command.deletedBy)
            log.info("remove success: projectId={}, deletedTasks={}", command.projectId, deletedTaskCount)
            ProjectResult.Remove(projectCount = 1, taskCount = deletedTaskCount)
        }

    fun modify(command: ProjectCommand.Update): ProjectResult =
        Tx.writable {
            val project =
                projectRepository.findActiveProjectByIdOrNull(command.projectId, command.workspaceId)
                    ?: return@writable ProjectResult.Failure.IdNotFound

            validateTaskIds(command)?.let { return@writable it }
            moveRemovedTasksToBacklog(project, command)
            applyUpdates(project, command)
            eventPublisher.publishAndClear(project)

            log.info("update success: projectId=${command.projectId}")
            ProjectResult.Success
        }

    /**
     * 프로젝트 간 Task 이동
     */
    fun moveTask(command: ProjectCommand.MoveTask): ProjectResult =
        Tx.writable {
            log.debug(
                "moveTask: fromProjectId={}, toProjectId={}, taskId={}, workspaceId={}",
                command.fromProjectId,
                command.toProjectId,
                command.taskId,
                command.workspaceId,
            )

            val fromProject =
                projectRepository.findActiveProjectByIdOrNull(command.fromProjectId, command.workspaceId)
                    ?: run {
                        log.warn("moveTask: fromProject not found. projectId=${command.fromProjectId}")
                        return@writable ProjectResult.Failure.IdNotFound
                    }

            val toProject =
                projectRepository.findActiveProjectByIdOrNull(command.toProjectId, command.workspaceId)
                    ?: run {
                        log.warn("moveTask: toProject not found. projectId=${command.toProjectId}")
                        return@writable ProjectResult.Failure.IdNotFound
                    }

            val taskSnapshot =
                taskPort.findByIdIn(listOf(command.taskId), command.workspaceId).singleOrNull()
                    ?: return@writable ProjectResult.Failure.TaskNotFound
            sprintTaskPolicy.validateTaskAssignmentsToProject(command.toProjectId, listOf(taskSnapshot), command.workspaceId)
            moveTaskToBacklogIfLeavingLastSprint(fromProject, toProject, command)

            fromProject.moveTaskTo(command.taskId, toProject, command.movedBy)
            eventPublisher.publishAndClear(fromProject)
            eventPublisher.publishAndClear(toProject)

            log.info(
                "moveTask success: taskId={}, fromProjectId={}, toProjectId={}",
                command.taskId,
                command.fromProjectId,
                command.toProjectId,
            )

            ProjectResult.Success
        }

    private fun validateTaskIds(command: ProjectCommand.Update): ProjectResult.Failure? =
        with(command) {
            val overlap = addTaskIds.intersect(removeTaskIds.toSet())
            if (overlap.isNotEmpty()) {
                log.warn("update: addTaskIds and removeTaskIds overlap. overlap={}", overlap)
                return@with ProjectResult.Failure.InvalidRequest
            }

            if (addTaskIds.isNotEmpty()) {
                val existingTasks = taskPort.findByIdIn(addTaskIds, workspaceId)
                if (existingTasks.size != addTaskIds.size) {
                    log.warn("update: some taskIds not found. addTaskIds={}", addTaskIds)
                    return@with ProjectResult.Failure.TaskNotFound
                }
                sprintTaskPolicy.validateTaskAssignmentsToProject(projectId, existingTasks, workspaceId)

                val assignments =
                    projectRepository
                        .findActiveProjectIdsByTaskIds(addTaskIds, workspaceId)
                        .filter { it.projectId != projectId }

                if (assignments.isNotEmpty()) {
                    log.warn(
                        "update: tasks already assigned to other projects. projectId={}, conflicts={}",
                        projectId,
                        assignments,
                    )
                    return@with ProjectResult.Failure.TaskAlreadyAssigned
                }
            }

            null
        }

    private fun applyUpdates(
        project: Project,
        command: ProjectCommand.Update,
    ) = with(command) {
        name?.let { project.modify(it) }

        if (addTaskIds.isNotEmpty()) {
            project.addTasks(addTaskIds, updatedBy)
            log.info("update: tasks added. projectId=$projectId, taskIds=$addTaskIds")
        }

        if (removeTaskIds.isNotEmpty()) {
            project.removeTasks(removeTaskIds, updatedBy)
            log.info("update: tasks removed. projectId=$projectId, taskIds=$removeTaskIds")
        }
    }

    private fun moveRemovedTasksToBacklog(
        project: Project,
        command: ProjectCommand.Update,
    ) {
        if (command.removeTaskIds.isEmpty()) {
            return
        }

        val activeTaskIds =
            projectRepository
                .findActiveTaskIdsByProjectId(project.projectId, command.workspaceId)
                .filter(command.removeTaskIds.toSet()::contains)
        if (activeTaskIds.isEmpty()) {
            return
        }

        moveTasksToBacklogIfLeavingLastSprint(
            candidateTaskIds = activeTaskIds,
            workspaceId = command.workspaceId,
            actorId = command.updatedBy,
            removedProjectIds = setOf(project.projectId),
        )
    }

    private fun moveTaskToBacklogIfLeavingLastSprint(
        fromProject: Project,
        toProject: Project,
        command: ProjectCommand.MoveTask,
    ) {
        moveTasksToBacklogIfLeavingLastSprint(
            candidateTaskIds = listOf(command.taskId),
            workspaceId = command.workspaceId,
            actorId = command.movedBy,
            removedProjectIds = setOf(fromProject.projectId),
            addedAssignments = listOf(TaskAssignment(command.taskId, toProject.projectId)),
        )
    }

    private fun moveTasksToBacklogIfLeavingLastSprint(
        candidateTaskIds: Collection<UUID>,
        workspaceId: WorkspaceId,
        actorId: UUID?,
        removedProjectIds: Set<UUID>,
        addedAssignments: Collection<TaskAssignment> = emptyList(),
    ) {
        if (candidateTaskIds.isEmpty()) {
            return
        }

        val removedSprintIds =
            sprintParticipationQuery
                .findActiveSprintIdsByProjectIds(removedProjectIds, workspaceId.value)
                .values
                .flatten()
                .toSet()
        if (removedSprintIds.isEmpty()) {
            return
        }

        val assignments =
            (assignmentsByTaskIds(candidateTaskIds, workspaceId) + addedAssignments)
                .distinctBy { it.taskId to it.projectId }
        val remainingProjectIds =
            assignments
                .map(TaskAssignment::projectId)
                .filterNot(removedProjectIds::contains)
                .toSet()
        val sprintIdsByProjectId =
            sprintParticipationQuery.findActiveSprintIdsByProjectIds(remainingProjectIds, workspaceId.value)
        val taskIdsMovingToBacklog =
            sprintTaskPolicy.tasksMovingToBacklog(
                candidateTaskIds = candidateTaskIds,
                deletableTaskIds = emptyList(),
                assignments = assignments,
                removedProjectIds = removedProjectIds,
                sprintIdsByProjectId = sprintIdsByProjectId,
            )

        if (taskIdsMovingToBacklog.isNotEmpty()) {
            taskPort.moveSprintTasksToBacklog(
                taskIdsMovingToBacklog,
                actorId,
                workspaceId,
                SprintTaskMembershipSnapshot.of(taskIdsMovingToBacklog),
            )
        }
    }

    // ----------------------------------------------------------------------------------------------

    private fun Page<TaskSnapshot>.toResult(): Page<ProjectResult.Task> = map { it.toResult() }

    private fun publishTaskRemovedFromSprintEvents(
        taskIds: Collection<UUID>,
        assignments: List<TaskAssignment>,
        workspaceId: WorkspaceId,
        removedProjectId: UUID,
        removedSprintIds: Collection<UUID>,
        sprintIdsByProjectId: Map<UUID, Set<UUID>>,
        actorId: UUID,
    ) {
        if (taskIds.isEmpty()) {
            return
        }

        if (removedSprintIds.isEmpty()) {
            return
        }

        val assignmentsByTaskId =
            assignments.groupBy(TaskAssignment::taskId) { it.projectId }

        taskIds.distinct().forEach { taskId ->
            val remainingSprintIds =
                assignmentsByTaskId[taskId]
                    .orEmpty()
                    .asSequence()
                    .filterNot { it == removedProjectId }
                    .flatMap { assignedProjectId ->
                        sprintIdsByProjectId[assignedProjectId].orEmpty().asSequence()
                    }.toSet()

            removedSprintIds
                .asSequence()
                .filterNot(remainingSprintIds::contains)
                .forEach { sprintId ->
                    eventPublisher.publish(
                        TaskRemovedFromSprintEvent(
                            workspaceId = workspaceId.value,
                            sprintId = sprintId,
                            taskId = taskId,
                            actorId = actorId,
                            eventId = UUID.randomUUID(),
                        ),
                    )
                }
        }
    }

    private fun assignmentsByTaskIds(
        taskIds: Collection<UUID>,
        workspaceId: WorkspaceId,
    ): List<TaskAssignment> = projectRepository.findActiveProjectIdsByTaskIds(taskIds, workspaceId)

    private fun deletableTaskIds(
        candidateTaskIds: Collection<UUID>,
        removedProjectIds: Set<UUID>,
        assignments: List<TaskAssignment>,
    ): List<UUID> {
        if (candidateTaskIds.isEmpty()) {
            return emptyList()
        }

        val assignmentsByTaskId =
            assignments.groupBy(TaskAssignment::taskId) { it.projectId }

        return candidateTaskIds.filter { taskId ->
            assignmentsByTaskId[taskId].orEmpty().all(removedProjectIds::contains)
        }
    }

    private fun TaskSnapshot.toResult(): ProjectResult.Task =
        ProjectResult.Task(
            id = id,
            title = title,
            status = status,
            assignee =
                assigneeId?.let { id ->
                    ProjectResult.Assignee(id = id, name = "", email = "")
                },
            dueDate = dueDate,
        )

    private fun Project.toResult(progress: ProjectProgress = ProjectProgress(projectId, 0L, 0L)): ProjectResult =
        ProjectResult.ProjectInfo(
            workspaceId = workspaceId,
            projectId = projectId,
            name = name,
            progress = progress.toResult(),
        )

    private fun ProjectProgress.toResult() =
        ProjectResult.Progress(
            totalCount = totalCount.toInt(),
            completedCount = completedCount.toInt(),
            progress = value,
        )
}
