package pizza.psycho.sos.project.sprint.application.service

import org.springframework.stereotype.Service
import pizza.psycho.sos.common.support.log.loggerDelegate
import pizza.psycho.sos.common.support.transaction.helper.Tx
import pizza.psycho.sos.project.common.domain.model.vo.WorkspaceId
import pizza.psycho.sos.project.project.application.port.out.ProjectPort
import pizza.psycho.sos.project.project.application.port.out.dto.ProjectSnapshot
import pizza.psycho.sos.project.project.application.port.out.query.ProjectProgress
import pizza.psycho.sos.project.sprint.application.service.dto.SprintCommand
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
     */

    fun getSprint(command: SprintCommand.Get): SprintResult =
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

    fun getProjectsInSprint(command: SprintCommand.GetProjects): SprintResult =
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
            val sprint =
                Sprint.create(
                    name = command.name,
                    workspaceId = command.workspaceId,
                    startDate = command.startDate,
                    endDate = command.endDate,
                )
            val saved = sprintRepository.save(sprint)
            saved
                .toResult()
                .also { log.info("create success: sprintId=${saved.sprintId}") }
        }

    fun remove(command: SprintCommand.Remove): SprintResult =
        Tx.writable {
            sprintRepository
                .deleteById(command.sprintId, command.deletedBy, command.workspaceId)
                .let { count -> SprintResult.Remove(count) }
                .also { log.info("remove success: sprintId=${command.sprintId}") }
        }

    fun removeWithProjects(command: SprintCommand.RemoveWithProjects): SprintResult =
        Tx.writable {
            val sprint =
                findActiveSprint(command.sprintId, command.workspaceId)
                    ?: run {
                        log.warn("removeWithProjects: sprint not found. sprintId=${command.sprintId}")
                        return@writable SprintResult.Failure.IdNotFound
                    }

            val projectIds = sprint.projectIds()
            val projectSnapshots = loadProjectSnapshots(projectIds, command.workspaceId)
            val (deletedProjectCount, deletedTaskCount) =
                deleteProjectsAndTasks(
                    projectIds = projectIds,
                    projectSnapshots = projectSnapshots,
                    deletedBy = command.deletedBy,
                    workspaceId = command.workspaceId,
                )

            if (deletedProjectCount > 0 || deletedTaskCount > 0) {
                log.info(
                    "removeWithProjects: projects={}, tasks={}, sprintId={}",
                    deletedProjectCount,
                    deletedTaskCount,
                    command.sprintId,
                )
            }

            sprint.delete(command.deletedBy)
            log.info("removeWithProjects success: sprintId=${command.sprintId}")
            SprintResult.RemoveWithProjects(
                sprintCount = 1,
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

    private fun applyUpdates(
        sprint: Sprint,
        command: SprintCommand.Update,
    ) = with(command) {
        name?.let { sprint.modify(it) }
        sprint.changePeriod(startDate, endDate)

        if (addProjectIds.isNotEmpty()) {
            sprint.addProjects(addProjectIds)
            log.info("update: projects added. sprintId=$sprintId, projectIds=$addProjectIds")
        }

        if (removeProjectIds.isNotEmpty()) {
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

    private fun deleteProjectsAndTasks(
        projectIds: List<UUID>,
        projectSnapshots: List<ProjectSnapshot>,
        deletedBy: UUID,
        workspaceId: WorkspaceId,
    ): Pair<Int, Int> {
        val taskIds = projectSnapshots.flatMap { it.taskIds }.distinct()
        val deletedTaskCount =
            if (taskIds.isEmpty()) {
                0
            } else {
                taskPort.deleteByIdIn(taskIds, deletedBy, workspaceId)
            }

        val deletedProjectCount =
            if (projectIds.isEmpty()) {
                0
            } else {
                projectPort.deleteByIdIn(projectIds, deletedBy, workspaceId)
            }

        return deletedProjectCount to deletedTaskCount
    }

    private fun Sprint.toResult(): SprintResult =
        SprintResult.SprintInfo(
            workspaceId = workspaceId,
            sprintId = sprintId,
            name = name,
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
