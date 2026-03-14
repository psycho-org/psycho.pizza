package pizza.psycho.sos.project.sprint.application.service

import org.springframework.stereotype.Service
import pizza.psycho.sos.common.patch.Patch
import pizza.psycho.sos.common.support.log.loggerDelegate
import pizza.psycho.sos.common.support.transaction.helper.Tx
import pizza.psycho.sos.project.common.domain.model.vo.WorkspaceId
import pizza.psycho.sos.project.project.application.port.out.ProjectPort
import pizza.psycho.sos.project.project.application.port.out.dto.ProjectSnapshot
import pizza.psycho.sos.project.project.application.port.out.query.ProjectProgress
import pizza.psycho.sos.project.sprint.application.service.dto.SprintCommand
import pizza.psycho.sos.project.sprint.application.service.dto.SprintQuery
import pizza.psycho.sos.project.sprint.application.service.dto.SprintResult
import pizza.psycho.sos.project.sprint.domain.model.entity.Sprint
import pizza.psycho.sos.project.sprint.domain.repository.SprintRepository
import pizza.psycho.sos.project.task.application.port.out.TaskPort
import java.util.UUID

@Service
class SprintService(
    private val sprintRepository: SprintRepository,
    private val projectPort: ProjectPort,
    private val taskPort: TaskPort,
) {
    private val log by loggerDelegate()

    /*
     * todo
     *      1. sprint의 기간 변경 시 하위 task들의 기간변경 (검증 후 초과 task 목록 반환 방식 검토)
     *          - sprint 범위 밖 task일 경우
     *          - sprintA에서 sprintB로 옮겼을 때 기준 기간이 달라진다면..?
     *          - sprint 기간 변경 시 하위 task들 기간 그대로 대신 task 기간을 변경하거나 task를 추가할 시 검증
     *          - ui에 줄 때 sprint 기간 내에 있는지 dto에 담아서 보내줄 것
     *      2. sprint 내의 프로젝트에 task의 기간 변경 또는 task 추가 시 sprint와의 기간 검증
     *          - 변경은 task에 넣고 검증을 sprint에?
     *
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
                        log.warn("remove: sprint not found. sprintId=${command.sprintId}")
                        return@writable SprintResult.Failure.IdNotFound
                    }

            val projectIds = sprint.projectIds()
            val deletedProjectCount = deleteProjects(projectIds, command.deletedBy, command.workspaceId)
            val deletedSprintCount = deleteSprint(command.sprintId, command.deletedBy, command.workspaceId)

            log.info(
                "remove success: sprintId={}, deletedProjects={}, deletedSprint={}",
                command.sprintId,
                deletedProjectCount,
                deletedSprintCount,
            )

            SprintResult.Remove(deletedSprintCount)
        }

    fun removeWithTasks(command: SprintCommand.RemoveWithTasks): SprintResult =
        Tx.writable {
            val sprint =
                findActiveSprint(command.sprintId, command.workspaceId)
                    ?: run {
                        log.warn("removeWithTasks: sprint not found. sprintId=${command.sprintId}")
                        return@writable SprintResult.Failure.IdNotFound
                    }

            val projectIds = sprint.projectIds()
            val projectSnapshots = loadProjectSnapshots(projectIds, command.workspaceId)
            val deletedTaskCount = deleteTasks(projectSnapshots, command.deletedBy, command.workspaceId)
            val deletedProjectCount = deleteProjects(projectIds, command.deletedBy, command.workspaceId)
            val deletedSprintCount = deleteSprint(command.sprintId, command.deletedBy, command.workspaceId)

            log.info(
                "removeWithTasks success: sprintId={}, projects={}, tasks={}",
                command.sprintId,
                deletedProjectCount,
                deletedTaskCount,
            )

            SprintResult.RemoveWithTasks(
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
            SprintResult.Success
        }

    private fun validateProjectIds(command: SprintCommand.Update): SprintResult.Failure? =
        with(command) {
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
            sprint.addProjects(addProjectIds)
            log.info("update: projects added. sprintId=$sprintId, projectIds=$addProjectIds")
        }

        if (removeProjectIds.isNotEmpty()) {
            // 스프린트에서 프로젝트를 분리할 때, 해당 프로젝트 내 Task 들의 상태를 TO DO로 리셋
            val projectSnapshots = loadProjectSnapshots(removeProjectIds, workspaceId)
            val removingTaskIds = projectSnapshots.flatMap { it.taskIds }

            val remainingProjectIds = sprint.projectIds().filterNot { removeProjectIds.contains(it) }
            val remainingTaskIds =
                loadProjectSnapshots(remainingProjectIds, workspaceId)
                    .flatMap { it.taskIds }
                    .toSet()

            val taskIdsToReset =
                removingTaskIds
                    .filterNot { remainingTaskIds.contains(it) }
                    .distinct()

            if (taskIdsToReset.isNotEmpty()) {
                taskPort.resetStatusToTodo(taskIdsToReset, by, workspaceId, emitEvent = true)
            } else {
                log.debug(
                    "update: no tasks need reset when removing projects. sprintId={}, removeProjectIds={}",
                    sprint.sprintId,
                    removeProjectIds,
                )
            }

            sprint.removeProjects(removeProjectIds)
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

    private fun deleteProjects(
        projectIds: List<UUID>,
        deletedBy: UUID,
        workspaceId: WorkspaceId,
    ): Int =
        if (projectIds.isEmpty()) {
            0
        } else {
            projectPort.deleteByIdIn(projectIds, deletedBy, workspaceId)
        }

    private fun deleteTasks(
        projectSnapshots: List<ProjectSnapshot>,
        deletedBy: UUID,
        workspaceId: WorkspaceId,
    ): Int {
        val taskIds = projectSnapshots.flatMap { it.taskIds }.distinct()
        return if (taskIds.isEmpty()) {
            0
        } else {
            taskPort.deleteByIdIn(taskIds, deletedBy, workspaceId)
        }
    }

    private fun deleteSprint(
        sprintId: UUID,
        deletedBy: UUID,
        workspaceId: WorkspaceId,
    ): Int = sprintRepository.deleteById(sprintId, deletedBy, workspaceId)

    private fun Sprint.toResult(): SprintResult =
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
}
