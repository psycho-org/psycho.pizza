package pizza.psycho.sos.project.project.application.service

import org.springframework.data.domain.Page
import org.springframework.stereotype.Service
import pizza.psycho.sos.common.support.log.loggerDelegate
import pizza.psycho.sos.common.support.transaction.helper.Tx
import pizza.psycho.sos.project.project.application.port.out.ProjectRepository
import pizza.psycho.sos.project.project.application.port.out.query.ProjectProgress
import pizza.psycho.sos.project.project.application.service.dto.ProjectCommand
import pizza.psycho.sos.project.project.application.service.dto.ProjectResult
import pizza.psycho.sos.project.project.domain.model.entity.Project
import pizza.psycho.sos.project.task.application.port.out.TaskPort
import pizza.psycho.sos.project.task.application.port.out.dto.TaskSnapshot

@Service
class ProjectService(
    private val projectRepository: ProjectRepository,
    private val taskPort: TaskPort,
) {
    private val log by loggerDelegate()

    fun getProject(command: ProjectCommand.Get): ProjectResult =
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

    fun getTasksInProject(command: ProjectCommand.GetTasks): ProjectResult =
        Tx.readable {
            log.debug("getTasksInProject: projectId={}, workspaceId={}", command.projectId, command.workspaceId)

            val project =
                projectRepository.findActiveProjectByIdOrNull(command.projectId, command.workspaceId)
                    ?: run {
                        log.warn("getTasksInProject: project not found. projectId={}", command.projectId)
                        return@readable ProjectResult.Failure.IdNotFound
                    }

            taskPort
                .findByIdIn(
                    ids = project.taskIds(),
                    workspaceId = command.workspaceId,
                    pageable = command.pageable,
                ).let { ProjectResult.TaskList(it.toResult()) }
                .also { log.info("getTasksInProject success: projectId=${command.projectId}") }
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

            val task =
                taskPort.createTask(
                    workspaceId = command.workspaceId.value,
                    title = command.title,
                    description = command.description,
                    assigneeId = command.assigneeId,
                    dueDate = command.dueDate,
                )
            project.addTask(task.id)
            log.info("createTask success: projectId=${command.projectId}, taskId=${task.id}")
            task.toResult()
        }

    fun remove(command: ProjectCommand.Remove): ProjectResult =
        Tx.writable {
            projectRepository
                .deleteById(command.projectId, command.deletedBy, command.workspaceId)
                .let { ProjectResult.Remove(it) }
                .also { log.info("remove success: projectId=${command.projectId}") }
        }

    fun removeWithTasks(command: ProjectCommand.RemoveWithTasks): ProjectResult =
        Tx.writable {
            val project =
                projectRepository.findActiveProjectByIdOrNull(command.projectId, command.workspaceId)
                    ?: run {
                        log.warn("removeWithTasks: project not found. projectId=${command.projectId}")
                        return@writable ProjectResult.Failure.IdNotFound
                    }

            val taskIds = project.taskIds()
            val deletedTaskCount =
                if (taskIds.isEmpty()) {
                    0
                } else {
                    taskPort
                        .deleteByIdIn(taskIds, command.deletedBy, command.workspaceId)
                        .also {
                            log.info(
                                "removeWithTasks: tasks soft-deleted. count={}, projectId={}",
                                it,
                                command.projectId,
                            )
                        }
                }

            project.delete(command.deletedBy)
            log.info("removeWithTasks success: projectId=${command.projectId}, deletedTasks=$deletedTaskCount")
            ProjectResult.RemoveWithTasks(projectCount = 1, taskCount = deletedTaskCount)
        }

    fun modify(command: ProjectCommand.Update): ProjectResult =
        Tx.writable {
            val project =
                projectRepository.findActiveProjectByIdOrNull(command.projectId, command.workspaceId)
                    ?: return@writable ProjectResult.Failure.IdNotFound

            validateTaskIds(command)?.let { return@writable it }
            applyUpdates(project, command)

            log.info("update success: projectId=${command.projectId}")
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
            }

            null
        }

    private fun applyUpdates(
        project: Project,
        command: ProjectCommand.Update,
    ) = with(command) {
        name?.let { project.modify(it) }

        if (addTaskIds.isNotEmpty()) {
            project.addTasks(addTaskIds)
            log.info("update: tasks added. projectId=$projectId, taskIds=$addTaskIds")
        }

        if (removeTaskIds.isNotEmpty()) {
            project.removeTasks(removeTaskIds)
            log.info("update: tasks removed. projectId=$projectId, taskIds=$removeTaskIds")
        }
    }

    // ----------------------------------------------------------------------------------------------

    private fun Page<TaskSnapshot>.toResult(): Page<ProjectResult.Task> = map { it.toResult() }

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
