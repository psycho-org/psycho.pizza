package pizza.psycho.sos.project.sprint.application.service

import org.springframework.stereotype.Service
import pizza.psycho.sos.common.event.DomainEventPublisher
import pizza.psycho.sos.common.patch.Patch
import pizza.psycho.sos.common.support.log.loggerDelegate
import pizza.psycho.sos.common.support.transaction.helper.Tx
import pizza.psycho.sos.project.common.domain.model.vo.WorkspaceId
import pizza.psycho.sos.project.project.application.port.out.ProjectPort
import pizza.psycho.sos.project.project.application.port.out.dto.ProjectSnapshot
import pizza.psycho.sos.project.project.application.port.out.dto.TaskAssignment
import pizza.psycho.sos.project.project.application.port.out.query.ProjectProgress
import pizza.psycho.sos.project.sprint.application.policy.SprintTaskPolicy
import pizza.psycho.sos.project.sprint.application.service.dto.SprintCommand
import pizza.psycho.sos.project.sprint.application.service.dto.SprintQuery
import pizza.psycho.sos.project.sprint.application.service.dto.SprintResult
import pizza.psycho.sos.project.sprint.domain.event.TaskRemovedFromSprintEvent
import pizza.psycho.sos.project.sprint.domain.model.entity.Sprint
import pizza.psycho.sos.project.sprint.domain.repository.SprintRepository
import pizza.psycho.sos.project.task.application.port.out.TaskPort
import pizza.psycho.sos.project.task.application.port.out.dto.SprintTaskMembershipSnapshot
import pizza.psycho.sos.project.task.application.port.out.dto.TaskSnapshot
import java.util.UUID

@Service
class SprintService(
    private val sprintRepository: SprintRepository,
    private val projectPort: ProjectPort,
    private val taskPort: TaskPort,
    private val domainEventPublisher: DomainEventPublisher,
    private val sprintTaskPolicy: SprintTaskPolicy,
) {
    private val log by loggerDelegate()

    /*
     * 정책
     * 1. sprint에 속하지 않는 task는 backlog로 칭함
     * 2. sprint에서 task를 제거하여 backlog로 전환할 때는 to do로 전환한 후 이벤트를 발송합니다.
     * 3. sprint에 연결된 project에 task를 추가할 때 dueDate가 추가되어 있는 경우 sprint의 기간 이내에 있어야 합니다.
     * 4. sprint에 연결된 project에 존재하는 task에서 dueDate를 수정하는 경우 sprint의 기간 이내에 있어야 합니다.
     * 5. sprint의 기간을 변경하는 경우 sprint 내의 project에서 task의 목록을 반환할 때, sprint의 기간 이내에 존재하는지 dto로 반환합니다.
     */

    fun getSprint(command: SprintQuery.Find): SprintResult =
        Tx.readable {
            log.debug("getSprint: sprintId={}, workspaceId={}", command.sprintId, command.workspaceId)

            val sprint =
                sprintRepository.findActiveSprintByIdOrNull(command.sprintId, command.workspaceId)
                    ?: run {
                        log.warn("getSprint: sprint not found. sprintId=${command.sprintId}")
                        return@readable SprintResult.Failure.IdNotFound
                    }

            sprint
                .toResult()
                .also { log.info("getSprint success: sprintId=${command.sprintId}") }
        }

    fun getSprints(command: SprintQuery.FindAll): SprintResult =
        Tx.readable {
            log.debug("getSprints: workspaceId={}, pageable={}", command.workspaceId, command.pageable)
            val page = sprintRepository.findActiveSprints(command.workspaceId, command.pageable)
            SprintResult
                .SprintPage(page.map { it.toResult() })
                .also { log.info("getSprints success: workspaceId={}", command.workspaceId) }
        }

    fun getProjectsInSprint(command: SprintQuery.FindProjectsInSprint): SprintResult =
        Tx.readable {
            log.debug("getProjectsInSprint: sprintId={}, workspaceId={}", command.sprintId, command.workspaceId)

            val sprint =
                findActiveSprint(command.sprintId, command.workspaceId)
                    ?: run {
                        log.warn("getProjectsInSprint: sprint not found. sprintId={}", command.sprintId)
                        return@readable SprintResult.Failure.IdNotFound
                    }

            val projectIds = sprint.projectIds()
            if (projectIds.isEmpty()) {
                return@readable SprintResult.ProjectList(emptyList())
            }

            SprintResult
                .ProjectList(fetchProjectsWithProgress(projectIds, command.workspaceId))
                .also { log.info("getProjectsInSprint success: sprintId=${command.sprintId}") }
        }

    fun getTasksInSprint(command: SprintQuery.FindTasksInSprint): SprintResult =
        Tx.readable {
            log.debug("getTasksInSprint: sprintId={}, workspaceId={}", command.sprintId, command.workspaceId)

            val sprint =
                findActiveSprint(command.sprintId, command.workspaceId)
                    ?: run {
                        log.warn("getTasksInSprint: sprint not found. sprintId={}", command.sprintId)
                        return@readable SprintResult.Failure.IdNotFound
                    }

            val projects = loadProjectSnapshots(sprint.projectIds(), command.workspaceId)
            if (projects.isEmpty()) {
                return@readable SprintResult.TaskList(emptyList())
            }

            val projectInfoByTaskId = mutableMapOf<UUID, Pair<UUID, String>>()
            val orderedTaskIds = mutableListOf<UUID>()

            projects.forEach { project ->
                project.taskIds.forEach { taskId ->
                    if (taskId !in projectInfoByTaskId) {
                        orderedTaskIds += taskId
                        projectInfoByTaskId[taskId] = project.projectId to project.name
                    }
                }
            }

            val tasksById = loadTaskSnapshots(orderedTaskIds, command.workspaceId).associateBy(TaskSnapshot::id)
            val tasks =
                orderedTaskIds
                    .mapNotNull { taskId ->
                        val task = tasksById[taskId] ?: return@mapNotNull null
                        val (projectId, projectName) = projectInfoByTaskId.getValue(taskId)
                        task.toResult(projectId, projectName)
                    }.filter { it.status == command.status }

            SprintResult
                .TaskList(tasks)
                .also { log.info("getTasksInSprint success: sprintId={}, status={}", command.sprintId, command.status) }
        }

    fun createProject(command: SprintCommand.CreateProject): SprintResult =
        Tx.writable {
            log.debug("createProject: sprintId={}, workspaceId={}", command.sprintId, command.workspaceId)

            val sprint =
                sprintRepository.findActiveSprintByIdOrNull(command.sprintId, command.workspaceId)
                    ?: run {
                        log.warn("createProject: sprint not found. sprintId={}", command.sprintId)
                        return@writable SprintResult.Failure.IdNotFound
                    }

            val project = projectPort.createProject(workspaceId = command.workspaceId, name = command.name)

            sprint.addProject(project.projectId)
            log.info("createProject success: sprintId={}, projectId={}", command.sprintId, project.projectId)
            SprintResult.ProjectCreated(project.toResult())
        }

    fun create(command: SprintCommand.Create): SprintResult =
        Tx.writable {
            val saved =
                sprintRepository.save(
                    Sprint.create(
                        name = command.name,
                        workspaceId = command.workspaceId,
                        goal = command.goal,
                        startDate = command.startDate,
                        endDate = command.endDate,
                    ),
                )
            saved
                .toResult()
                .also { log.info("create success: sprintId=${saved.sprintId}") }
        }

    fun remove(command: SprintCommand.Remove): SprintResult =
        Tx.writable {
            val sprint =
                findActiveSprint(command.sprintId, command.workspaceId)
                    ?: run {
                        log.warn("remove: sprint not found. sprintId={}", command.sprintId)
                        return@writable SprintResult.Failure.IdNotFound
                    }

            val projectIds = sprint.projectIds()
            val projectSnapshots = loadProjectSnapshots(projectIds, command.workspaceId)
            val candidateTaskIds = projectSnapshots.flatMap(ProjectSnapshot::taskIds).distinct()
            val removedProjectIds = projectSnapshots.mapTo(mutableSetOf()) { it.projectId }
            val assignments = projectPort.findActiveProjectIdsByTaskIds(candidateTaskIds, command.workspaceId)
            val deletableTaskIds = deletableTaskIds(candidateTaskIds, removedProjectIds, assignments)
            val remainingProjectIds =
                assignments
                    .map(TaskAssignment::projectId)
                    .filterNot(removedProjectIds::contains)
                    .toSet()
            val sprintIdsByProjectId = sprintRepository.findActiveSprintIdsByProjectIds(remainingProjectIds, command.workspaceId)
            val taskIdsMovingToBacklog =
                sprintTaskPolicy.tasksMovingToBacklog(
                    candidateTaskIds = candidateTaskIds,
                    deletableTaskIds = deletableTaskIds,
                    assignments = assignments,
                    removedProjectIds = removedProjectIds,
                    sprintIdsByProjectId = sprintIdsByProjectId,
                )
            if (taskIdsMovingToBacklog.isNotEmpty()) {
                taskPort.moveSprintTasksToBacklog(
                    taskIdsMovingToBacklog,
                    command.deletedBy,
                    command.workspaceId,
                    SprintTaskMembershipSnapshot.of(taskIdsMovingToBacklog),
                )
            }
            publishTaskRemovedFromSprintEvents(
                sprintId = command.sprintId,
                taskIds = candidateTaskIds,
                workspaceId = command.workspaceId,
                actorId = command.deletedBy,
            )
            val deletedTaskCount =
                deleteTasks(
                    deletableTaskIds,
                    command.deletedBy,
                    command.workspaceId,
                    command.reason,
                )
            val deletedProjectCount =
                deleteProjects(
                    projectSnapshots,
                    command.deletedBy,
                    command.workspaceId,
                    command.reason,
                )
            sprint.delete(command.deletedBy, command.reason)
            val deletedSprintCount = 1

            log.info(
                "remove success: sprintId={}, projects={}, tasks={}",
                command.sprintId,
                deletedProjectCount,
                deletedTaskCount,
            )

            domainEventPublisher.publishAndClear(sprint)

            SprintResult.Remove(
                sprintCount = deletedSprintCount,
                projectCount = deletedProjectCount,
                taskCount = deletedTaskCount,
            )
        }

    fun modify(command: SprintCommand.Update): SprintResult =
        Tx.writable {
            val sprint =
                sprintRepository.findActiveSprintByIdOrNull(command.sprintId, command.workspaceId)
                    ?: return@writable SprintResult.Failure.IdNotFound

            validateProjectIds(command)?.let { return@writable it }
            validateRemoveProjectIds(sprint, command)?.let { return@writable it }
            applyUpdates(sprint, command)

            log.info("update success: sprintId=${command.sprintId}")
            domainEventPublisher.publishAndClear(sprint)
            SprintResult.Success
        }

    private fun validateProjectIds(command: SprintCommand.Update): SprintResult.Failure? =
        with(command) {
            if (addProjectIds.size != addProjectIds.distinct().size) {
                log.warn("update: addProjectIds contain duplicates. addProjectIds={}", addProjectIds)
                return@with SprintResult.Failure.InvalidRequest
            }

            val overlap = addProjectIds.intersect(removeProjectIds.toSet())
            if (overlap.isNotEmpty()) {
                log.warn("update: addProjectIds and removeProjectIds overlap. overlap={}", overlap)
                return@with SprintResult.Failure.InvalidRequest
            }

            if (addProjectIds.isNotEmpty()) {
                val existing = projectPort.findByIdIn(addProjectIds, workspaceId)
                if (existing.size != addProjectIds.size) {
                    log.warn("update: some projectIds not found. addProjectIds={}", addProjectIds)
                    return@with SprintResult.Failure.ProjectNotFound
                }

                val conflictingAssignments =
                    sprintRepository
                        .findActiveSprintIdsByProjectIds(addProjectIds, workspaceId)
                        .filterValues { sprintIds -> sprintIds.any { it != sprintId } }
                if (conflictingAssignments.isNotEmpty()) {
                    log.warn(
                        "update: projects already assigned to another sprint. sprintId={}, conflicts={}",
                        sprintId,
                        conflictingAssignments,
                    )
                    return@with SprintResult.Failure.InvalidRequest
                }
            }

            null
        }

    private fun validateRemoveProjectIds(
        sprint: Sprint,
        command: SprintCommand.Update,
    ): SprintResult.Failure? {
        if (command.removeProjectIds.isEmpty()) {
            return null
        }

        val currentProjects = sprint.projectIds().toSet()
        val invalidIds = command.removeProjectIds.filterNot { currentProjects.contains(it) }

        if (invalidIds.isNotEmpty()) {
            log.warn("update: removeProjectIds contain projects not in sprint. invalid={}", invalidIds)
            return SprintResult.Failure.ProjectNotFound
        }

        return null
    }

    private fun applyUpdates(
        sprint: Sprint,
        command: SprintCommand.Update,
    ) = with(command) {
        val existingProjects = loadProjectSnapshots(sprint.projectIds(), workspaceId)
        name?.let { sprint.modify(it) }
        when (goal) {
            is Patch.Value -> sprint.changeGoal(goal.value, by)
            Patch.Clear -> sprint.changeGoal(null, by)
            Patch.Unchanged -> Unit
        }
        if (startDate != null || endDate != null) {
            sprint.changePeriod(startDate, endDate, by)
        }

        if (addProjectIds.isNotEmpty()) {
            val addedProjects = loadProjectSnapshots(addProjectIds, workspaceId)
            val taskIdsEnteringSprint = sprintTaskPolicy.tasksEnteringSprint(existingProjects, addedProjects)
            val tasksEnteringSprint = loadTaskSnapshots(taskIdsEnteringSprint, workspaceId)
            sprintTaskPolicy.validateTasksWithinSprintPeriod(sprint, tasksEnteringSprint)
            sprint.addProjects(addProjectIds, taskIdsEnteringSprint, by)
            log.info("update: projects added. sprintId=$sprintId, projectIds=$addProjectIds")
        }

        if (removeProjectIds.isNotEmpty()) {
            val removedProjects = existingProjects.filter { removeProjectIds.contains(it.projectId) }
            val addedProjects =
                if (addProjectIds.isEmpty()) {
                    emptyList()
                } else {
                    loadProjectSnapshots(addProjectIds, workspaceId)
                }
            val remainingProjects = existingProjects.filterNot { removeProjectIds.contains(it.projectId) } + addedProjects
            val taskIdsMovingToBacklog = sprintTaskPolicy.tasksMovingToBacklog(removedProjects, remainingProjects)

            if (taskIdsMovingToBacklog.isNotEmpty()) {
                taskPort.moveSprintTasksToBacklog(
                    taskIdsMovingToBacklog,
                    by,
                    workspaceId,
                    SprintTaskMembershipSnapshot.of(taskIdsMovingToBacklog),
                )
            } else {
                log.debug(
                    "update: no tasks move to backlog when removing projects. sprintId={}, removeProjectIds={}",
                    sprint.sprintId,
                    removeProjectIds,
                )
            }

            sprint.removeProjects(removeProjectIds, taskIdsMovingToBacklog, by)
            log.info("update: projects removed. sprintId=$sprintId, projectIds=$removeProjectIds")
        }
    }

    // ----------------------------------------------------------------------------------------------

    private fun findActiveSprint(
        sprintId: UUID,
        workspaceId: WorkspaceId,
    ): Sprint? = sprintRepository.findActiveSprintByIdOrNull(sprintId, workspaceId)

    private fun fetchProjectsWithProgress(
        projectIds: List<UUID>,
        workspaceId: WorkspaceId,
    ): List<SprintResult.Project> {
        val projects = projectPort.findByIdIn(projectIds, workspaceId)
        if (projects.isEmpty()) {
            return emptyList()
        }

        val progressMap =
            projectPort
                .findProgressesByProjectId(projectIds, workspaceId)
                .associateBy(ProjectProgress::projectId)
        return projects.map { it.toResult(progressMap[it.projectId]) }
    }

    private fun loadProjectSnapshots(
        projectIds: List<UUID>,
        workspaceId: WorkspaceId,
    ): List<ProjectSnapshot> =
        if (projectIds.isEmpty()) {
            emptyList()
        } else {
            projectPort.findByIdIn(projectIds, workspaceId)
        }

    private fun loadTaskSnapshots(
        taskIds: Collection<UUID>,
        workspaceId: WorkspaceId,
    ): List<TaskSnapshot> =
        if (taskIds.isEmpty()) {
            emptyList()
        } else {
            taskPort.findByIdIn(taskIds.toList(), workspaceId)
        }

    private fun deleteProjects(
        projects: List<ProjectSnapshot>,
        deletedBy: UUID,
        workspaceId: WorkspaceId,
        reason: String,
    ): Int =
        if (projects.isEmpty()) {
            0
        } else {
            projectPort
                .deleteByIdIn(
                    projectIds = projects.map(ProjectSnapshot::projectId),
                    deletedBy = deletedBy,
                    workspaceId = workspaceId,
                    reason = reason,
                ).also { deletedEvents ->
                    deletedEvents.forEach(domainEventPublisher::publish)
                }.size
        }

    private fun deleteTasks(
        deletableTaskIds: Collection<UUID>,
        deletedBy: UUID,
        workspaceId: WorkspaceId,
        reason: String,
    ): Int =
        if (deletableTaskIds.isEmpty()) {
            0
        } else {
            taskPort.deleteByIdIn(deletableTaskIds, deletedBy, workspaceId, reason)
        }

    private fun publishTaskRemovedFromSprintEvents(
        sprintId: UUID,
        taskIds: Collection<UUID>,
        workspaceId: WorkspaceId,
        actorId: UUID,
    ) {
        val sprint =
            sprintRepository.findActiveSprintByIdOrNull(sprintId, workspaceId)
                ?: return
        taskIds
            .distinct()
            .forEach { taskId ->
                domainEventPublisher.publish(
                    TaskRemovedFromSprintEvent(
                        workspaceId = workspaceId.value,
                        sprintId = sprintId,
                        taskId = taskId,
                        actorId = actorId,
                        sprintStartDate = sprint.period.startDate,
                        sprintEndDate = sprint.period.endDate,
                        eventId = UUID.randomUUID(),
                    ),
                )
            }
    }

    private fun deletableTaskIds(
        candidateTaskIds: Collection<UUID>,
        removedProjectIds: Set<UUID>,
        assignments: List<TaskAssignment>,
    ): List<UUID> {
        if (candidateTaskIds.isEmpty()) {
            return emptyList()
        }

        val assignmentsByTaskId = assignments.groupBy(TaskAssignment::taskId) { it.projectId }

        return candidateTaskIds.filter { taskId ->
            assignmentsByTaskId[taskId].orEmpty().all(removedProjectIds::contains)
        }
    }

    private fun Sprint.toResult(): SprintResult.SprintInfo =
        SprintResult.SprintInfo(
            workspaceId = workspaceId,
            sprintId = sprintId,
            name = name,
            goal = goal,
            startDate = period.startDate,
            endDate = period.endDate,
        )

    private fun ProjectSnapshot.toResult(progress: ProjectProgress? = null): SprintResult.Project =
        SprintResult.Project(
            projectId = projectId,
            name = name,
            progress =
                SprintResult.Progress(
                    totalCount = progress?.totalCount?.toInt() ?: 0,
                    completedCount = progress?.completedCount?.toInt() ?: 0,
                    progress = progress?.value ?: 0.0,
                ),
        )

    private fun TaskSnapshot.toResult(
        projectId: UUID,
        projectName: String,
    ): SprintResult.Task =
        SprintResult.Task(
            id = id,
            title = title,
            status = status,
            priority = priority,
            projectId = projectId,
            projectName = projectName,
            assigneeId = assigneeId,
            dueDate = dueDate,
        )
}
