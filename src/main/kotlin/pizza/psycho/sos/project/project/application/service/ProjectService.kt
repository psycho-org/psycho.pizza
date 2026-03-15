package pizza.psycho.sos.project.project.application.service

import org.springframework.data.domain.Page
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
import pizza.psycho.sos.project.sprint.domain.model.vo.Period
import pizza.psycho.sos.project.sprint.domain.policy.SprintTaskPeriodPolicy
import pizza.psycho.sos.project.task.application.port.out.TaskPort
import pizza.psycho.sos.project.task.application.port.out.dto.TaskSnapshot
import java.util.UUID

@Service
class ProjectService(
    private val projectRepository: ProjectRepository,
    private val eventPublisher: DomainEventPublisher,
    private val taskPort: TaskPort,
    private val projectSprintParticipationQuery: ProjectSprintParticipationQuery,
    private val sprintTaskPeriodPolicy: SprintTaskPeriodPolicy,
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

            val sprintPeriods =
                projectSprintParticipationQuery.findActiveSprintPeriodsByProjectId(
                    projectId = command.projectId,
                    workspaceId = command.workspaceId.value,
                )

            taskPort
                .findByIdIn(
                    ids = project.taskIds(),
                    workspaceId = command.workspaceId,
                    pageable = command.pageable,
                ).let { page ->
                    val mapped =
                        page.map { snapshot ->
                            val within =
                                if (sprintPeriods.isEmpty()) {
                                    null
                                } else {
                                    val dueDate = snapshot.dueDate
                                    sprintPeriods.any { snapshotPeriod ->
                                        sprintTaskPeriodPolicy.isTaskDueDateWithinSprint(
                                            Period(snapshotPeriod.startDate, snapshotPeriod.endDate),
                                            dueDate,
                                        )
                                    }
                                }
                            snapshot.toResult(isWithinSprintPeriod = within)
                        }
                    ProjectResult.TaskList(mapped)
                }.also { log.info("getTasksInProject success: projectId=${command.projectId}") }
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
            if (command.dueDate != null) {
                val sprintPeriods =
                    projectSprintParticipationQuery.findActiveSprintPeriodsByProjectId(
                        projectId = command.projectId,
                        workspaceId = command.workspaceId.value,
                    )

                val violatesSprint =
                    sprintPeriods.any { snapshot ->
                        !sprintTaskPeriodPolicy.isTaskDueDateWithinSprint(
                            Period(snapshot.startDate, snapshot.endDate),
                            command.dueDate,
                        )
                    }

                if (violatesSprint) {
                    log.warn(
                        "createTask: dueDate outside sprint period. projectId={}, dueDate={}, sprintIds={}",
                        command.projectId,
                        command.dueDate,
                        sprintPeriods.map { it.sprintId },
                    )
                    return@writable ProjectResult.Failure.InvalidRequest
                }
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

            val taskIds = project.taskIds()
            val deletableTaskIds = deletableTaskIds(taskIds, setOf(project.projectId), command.workspaceId)
            publishTaskRemovedFromSprintEvents(
                projectId = project.projectId,
                taskIds = taskIds,
                workspaceId = command.workspaceId,
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

    // ----------------------------------------------------------------------------------------------

    private fun Page<TaskSnapshot>.toResult(): Page<ProjectResult.Task> = map { it.toResult() }

    private fun publishTaskRemovedFromSprintEvents(
        projectId: UUID,
        taskIds: Collection<UUID>,
        workspaceId: WorkspaceId,
        actorId: UUID,
    ) {
        if (taskIds.isEmpty()) {
            return
        }

        val removedSprintIds = sprintParticipationQuery.findActiveSprintIdsByProjectId(projectId, workspaceId.value)
        if (removedSprintIds.isEmpty()) {
            return
        }

        val assignmentsByTaskId =
            projectRepository
                .findActiveProjectIdsByTaskIds(taskIds, workspaceId)
                .groupBy(TaskAssignment::taskId) { it.projectId }
        val sprintIdsByProjectId = mutableMapOf(projectId to removedSprintIds)

        taskIds.distinct().forEach { taskId ->
            val remainingSprintIds =
                assignmentsByTaskId[taskId]
                    .orEmpty()
                    .asSequence()
                    .filterNot { it == projectId }
                    .flatMap { assignedProjectId ->
                        sprintIdsByProjectId
                            .getOrPut(assignedProjectId) {
                                sprintParticipationQuery.findActiveSprintIdsByProjectId(
                                    assignedProjectId,
                                    workspaceId.value,
                                )
                            }.asSequence()
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

    private fun deletableTaskIds(
        candidateTaskIds: Collection<UUID>,
        removedProjectIds: Set<UUID>,
        workspaceId: WorkspaceId,
    ): List<UUID> {
        if (candidateTaskIds.isEmpty()) {
            return emptyList()
        }

        val assignmentsByTaskId =
            projectRepository
                .findActiveProjectIdsByTaskIds(candidateTaskIds, workspaceId)
                .groupBy(TaskAssignment::taskId) { it.projectId }

        return candidateTaskIds.filter { taskId ->
            assignmentsByTaskId[taskId].orEmpty().all(removedProjectIds::contains)
        }
    }

    private fun TaskSnapshot.toResult(isWithinSprintPeriod: Boolean? = null): ProjectResult.Task =
        ProjectResult.Task(
            id = id,
            title = title,
            status = status,
            assignee =
                assigneeId?.let { id ->
                    ProjectResult.Assignee(id = id, name = "", email = "")
                },
            dueDate = dueDate,
            isWithinSprintPeriod = isWithinSprintPeriod,
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
