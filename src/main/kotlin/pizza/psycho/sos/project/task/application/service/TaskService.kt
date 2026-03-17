package pizza.psycho.sos.project.task.application.service

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import pizza.psycho.sos.common.event.DomainEventPublisher
import pizza.psycho.sos.common.patch.Patch
import pizza.psycho.sos.common.support.log.loggerDelegate
import pizza.psycho.sos.common.support.transaction.helper.Tx
import pizza.psycho.sos.project.common.domain.model.vo.WorkspaceId
import pizza.psycho.sos.project.project.application.port.out.ProjectPort
import pizza.psycho.sos.project.project.application.port.out.ProjectSprintParticipationQuery
import pizza.psycho.sos.project.project.application.port.out.dto.ProjectSnapshot
import pizza.psycho.sos.project.project.application.port.out.dto.TaskAssignment
import pizza.psycho.sos.project.sprint.application.policy.SprintTaskPolicy
import pizza.psycho.sos.project.sprint.domain.model.vo.Period
import pizza.psycho.sos.project.sprint.domain.policy.SprintTaskPeriodPolicy
import pizza.psycho.sos.project.task.application.event.handler.TaskEventSprintMembershipRegistry
import pizza.psycho.sos.project.task.application.port.out.TaskSprintParticipationQuery
import pizza.psycho.sos.project.task.application.port.out.dto.SprintTaskMembershipSnapshot
import pizza.psycho.sos.project.task.application.port.out.dto.TaskSprintSummary
import pizza.psycho.sos.project.task.application.service.dto.TaskCommand
import pizza.psycho.sos.project.task.application.service.dto.TaskQuery
import pizza.psycho.sos.project.task.application.service.dto.TaskResult
import pizza.psycho.sos.project.task.application.service.dto.TaskResult.Assignee
import pizza.psycho.sos.project.task.application.service.dto.TaskResult.TaskInformation
import pizza.psycho.sos.project.task.domain.event.TaskDomainEvent
import pizza.psycho.sos.project.task.domain.model.entity.Task
import pizza.psycho.sos.project.task.domain.model.entity.TaskUpdateSpec
import pizza.psycho.sos.project.task.domain.model.vo.Status
import pizza.psycho.sos.project.task.domain.repository.TaskRepository
import java.util.UUID

@Service
class TaskService(
    private val taskRepository: TaskRepository,
    private val domainEventPublisher: DomainEventPublisher,
    private val projectPort: ProjectPort,
    private val projectSprintParticipationQuery: ProjectSprintParticipationQuery,
    private val sprintTaskPolicy: SprintTaskPolicy,
    private val sprintParticipationQuery: TaskSprintParticipationQuery,
    private val sprintMembershipRegistry: TaskEventSprintMembershipRegistry,
    private val taskSprintParticipationQuery: TaskSprintParticipationQuery,
    private val sprintTaskPeriodPolicy: SprintTaskPeriodPolicy,
) {
    private val log by loggerDelegate()

    fun create(command: TaskCommand.AddTask): TaskResult =
        Tx.writable {
            saveTask(command).toResult()
        }

    fun getAll(command: TaskQuery.FindTasks): TaskResult.TaskList =
        taskRepository.findAllActiveTasks(WorkspaceId(command.workspaceId), command.pageable).let {
            TaskResult.TaskList(it.toResult())
        }

    fun getAssignedTasks(command: TaskQuery.FindAssignedTasks): TaskResult.AssignedTaskGroups =
        Tx.readable {
            val workspaceId = WorkspaceId(command.workspaceId)
            val page =
                taskRepository.findAllActiveTasksByAssigneeId(
                    workspaceId = workspaceId,
                    assigneeId = command.assigneeId,
                    pageable = command.pageable,
                )
            val taskPage = page.map { task -> task.toAssignedResult(emptyList(), emptyList()) }
            if (page.isEmpty) {
                return@readable TaskResult.AssignedTaskGroups(
                    page = taskPage,
                    sprintGroups = emptyList(),
                )
            }

            val taskIds = page.content.map(Task::taskId)
            val assignments = projectPort.findActiveProjectIdsByTaskIds(taskIds, workspaceId)
            val projectIds = assignments.map(TaskAssignment::projectId).distinct()
            val projectIdsByTaskId =
                assignments
                    .groupBy(keySelector = { it.taskId }, valueTransform = { it.projectId })
                    .mapValues { (_, ids) -> ids.distinct() }
            val projectsById =
                projectPort
                    .findByIdIn(projectIds, workspaceId)
                    .associateBy(ProjectSnapshot::projectId)
            val sprintsByTaskId = taskSprintParticipationQuery.findActiveSprintsByTaskIds(taskIds, command.workspaceId)
            val sprintById = sprintsByTaskId.values.flatten().associateBy(TaskSprintSummary::id)
            val sprintIdsByProjectId =
                projectSprintParticipationQuery.findActiveSprintIdsByProjectIds(
                    projectIds = projectIds,
                    workspaceId = command.workspaceId,
                )

            val enrichedPage =
                page.map { task ->
                    task.toAssignedResult(
                        projects =
                            projectIdsByTaskId[task.taskId]
                                .orEmpty()
                                .mapNotNull(projectsById::get),
                        sprints = sprintsByTaskId[task.taskId].orEmpty(),
                    )
                }

            TaskResult.AssignedTaskGroups(
                page = enrichedPage,
                sprintGroups =
                    buildAssignedSprintGroups(
                        tasks = enrichedPage.content,
                        projectIdsByTaskId = projectIdsByTaskId,
                        projectsById = projectsById,
                        sprintIdsByProjectId = sprintIdsByProjectId,
                        sprintById = sprintById,
                    ),
            )
        }

    fun getBacklog(command: TaskQuery.FindBacklogTasks): TaskResult.TaskList =
        taskRepository.findAllActiveBacklogTasks(WorkspaceId(command.workspaceId), command.pageable).let {
            TaskResult.TaskList(it.toResult())
        }

    fun getInformation(command: TaskQuery.FindTask): TaskResult =
        taskRepository
            .findActiveTaskByIdOrNull(id = command.id, workspaceId = WorkspaceId(command.workspaceId))
            ?.toResult()
            ?: TaskResult.Failure.IdNotFound

    fun remove(command: TaskCommand.RemoveTask): TaskResult =
        Tx.writable {
            val deletedCount =
                taskRepository
                    .findActiveTaskByIdOrNull(command.id, WorkspaceId(command.workspaceId))
                    ?.also {
                        val wasInActiveSprint =
                            sprintParticipationQuery.existsActiveSprintByTaskId(it.taskId, command.workspaceId)
                        it.delete(command.deletedBy, command.reason)
                        markSprintMembership(it, wasInActiveSprint)
                        domainEventPublisher.publishAndClear(it)
                    }?.let { 1 }
                    ?: 0

            TaskResult.Remove(deletedCount)
        }

    fun saveTask(command: TaskCommand.AddTask): Task = taskRepository.save(command.toDomain())

    fun findTasksByIdIn(
        ids: Collection<UUID>,
        workspaceId: WorkspaceId,
    ): List<Task> = taskRepository.findAllByIdIn(ids, workspaceId)

    fun findTasksByIdIn(
        ids: Collection<UUID>,
        workspaceId: WorkspaceId,
        pageable: Pageable,
    ): Page<Task> = taskRepository.findAllByIdIn(ids, workspaceId, pageable)

    fun deleteTaskById(
        id: UUID,
        deletedBy: UUID,
        workspaceId: WorkspaceId,
    ): Int =
        Tx.writable {
            taskRepository
                .findActiveTaskByIdOrNull(id, workspaceId)
                ?.also {
                    val wasInActiveSprint = sprintParticipationQuery.existsActiveSprintByTaskId(it.taskId, workspaceId.value)
                    it.delete(deletedBy)
                    markSprintMembership(it, wasInActiveSprint)
                    domainEventPublisher.publishAndClear(it)
                }?.let { 1 }
                ?: 0
        }

    fun deleteTasksByIdIn(
        ids: Collection<UUID>,
        deletedBy: UUID,
        workspaceId: WorkspaceId,
        reason: String? = null,
    ): Int =
        Tx.writable {
            if (ids.isEmpty()) return@writable 0
            val sprintTaskIds = sprintParticipationQuery.findTaskIdsInActiveSprints(ids, workspaceId.value)
            val tasks =
                taskRepository
                    .findAllByIdIn(ids, workspaceId)
                    .onEach {
                        it.delete(deletedBy, reason)
                        markSprintMembership(it, sprintTaskIds.contains(it.taskId))
                    }

            domainEventPublisher.publishAndClearAll(tasks)
            return@writable tasks.size
        }

    /**
     * 주어진 Task 들을 backlog 로 되돌린다.
     * backlog 전환 시 상태는 TO DO 로 보정된다.
     */
    fun moveToBacklog(
        ids: Collection<UUID>,
        workspaceId: WorkspaceId,
        actorId: UUID?,
    ) {
        val membershipSnapshot =
            SprintTaskMembershipSnapshot.of(
                sprintParticipationQuery.findTaskIdsInActiveSprints(ids, workspaceId.value),
            )
        moveSprintTasksToBacklog(ids, workspaceId, actorId, membershipSnapshot)
    }

    fun moveSprintTasksToBacklog(
        ids: Collection<UUID>,
        workspaceId: WorkspaceId,
        actorId: UUID?,
        membershipSnapshot: SprintTaskMembershipSnapshot,
    ) = Tx.writable {
        if (ids.isEmpty()) return@writable

        val tasks = taskRepository.findAllByIdIn(ids, workspaceId)
        if (tasks.isEmpty()) return@writable

        tasks.forEach { task ->
            if (task.status != Status.TODO) {
                task.changeStatus(
                    status = Status.TODO,
                    by = actorId,
                )
                markSprintMembership(task, membershipSnapshot.contains(task.taskId))
            }
        }

        domainEventPublisher.publishAndClearAll(tasks)
    }

    fun update(command: TaskCommand.UpdateTask): TaskResult =
        Tx.writable {
            val workspaceId = WorkspaceId(command.workspaceId)

            val task =
                taskRepository.findActiveTaskByIdOrNull(command.id, workspaceId)
                    ?: return@writable TaskResult.Failure.IdNotFound

            val spec = command.toUpdateSpec()
            val sprintPeriods =
                taskSprintParticipationQuery.findActiveSprintPeriodsByTaskId(
                    taskId = task.taskId,
                    workspaceId = task.workspaceId.value,
                )

            if (sprintPeriods.isNotEmpty()) {
                when (val dueDatePatch = spec.dueDate) {
                    is Patch.Value -> {
                        val dueDate = dueDatePatch.value
                        val violates =
                            sprintPeriods.any { snapshot ->
                                !sprintTaskPeriodPolicy.isTaskDueDateWithinSprint(
                                    Period(snapshot.startDate, snapshot.endDate),
                                    dueDate,
                                )
                            }
                        if (violates) {
                            log.warn(
                                "update task dueDate outside sprint period. taskId={}, dueDate={}, sprintIds={}",
                                task.taskId,
                                dueDate,
                                sprintPeriods.map { it.sprintId },
                            )
                            return@writable TaskResult.Failure.InvalidRequest
                        }
                    }

                    else -> Unit
                }
            }
            sprintTaskPolicy.validateTaskDueDateChange(task.taskId, spec.dueDate, workspaceId)
            task.apply(spec)

            domainEventPublisher.publishAndClear(task)

            task.toResult()
        }

    // -----------------------------------------------------------------------------

    // todo 유저 로직 추가 시 수정
    private fun Page<Task>.toResult(): Page<TaskResult.TaskListInfo> =
        map {
            TaskResult.TaskListInfo(
                id = it.taskId,
                title = it.title,
                assignee =
                    it.assigneeId.value?.let { id ->
                        Assignee(
                            id = id,
                            name = "",
                            email = "",
                        )
                    },
                status = it.status,
                dueDate = it.dueDate.value,
            )
        }

    private fun Task.toResult(): TaskResult =
        TaskInformation(
            id = taskId,
            title = title,
            description = description,
            status = status,
            priority = priority,
            assignee =
                assigneeId.value?.let { id ->
                    Assignee(
                        id = id,
                        name = "",
                        email = "",
                    )
                },
            dueDate = dueDate.value,
            workspaceId = workspaceId.value,
        )

    private fun Task.toAssignedResult(
        projects: List<ProjectSnapshot>,
        sprints: List<TaskSprintSummary>,
    ): TaskResult.AssignedTaskListInfo =
        TaskResult.AssignedTaskListInfo(
            id = taskId,
            title = title,
            status = status,
            assignee =
                assigneeId.value?.let { id ->
                    Assignee(
                        id = id,
                        name = "",
                        email = "",
                    )
                },
            dueDate = dueDate.value,
            projects =
                projects.map { project ->
                    TaskResult.Project(
                        id = project.projectId,
                        name = project.name,
                    )
                },
            sprints =
                sprints.map { sprint ->
                    TaskResult.Sprint(
                        id = sprint.id,
                        name = sprint.name,
                        startDate = sprint.startDate,
                        endDate = sprint.endDate,
                    )
                },
        )

    private fun buildAssignedSprintGroups(
        tasks: List<TaskResult.AssignedTaskListInfo>,
        projectIdsByTaskId: Map<UUID, List<UUID>>,
        projectsById: Map<UUID, ProjectSnapshot>,
        sprintIdsByProjectId: Map<UUID, Set<UUID>>,
        sprintById: Map<UUID, TaskSprintSummary>,
    ): List<TaskResult.AssignedSprintGroup> {
        val grouped = linkedMapOf<UUID?, LinkedHashMap<UUID?, MutableList<TaskResult.AssignedTask>>>()

        tasks.forEach { task ->
            val taskInfo = task.toAssignedTask()
            val projectIds = projectIdsByTaskId[task.id].orEmpty()
            if (projectIds.isEmpty()) {
                grouped.addTask(sprintId = null, projectId = null, task = taskInfo)
                return@forEach
            }

            projectIds.forEach { projectId ->
                val sprintIds =
                    sprintIdsByProjectId[projectId]
                        .orEmpty()
                        .filter(sprintById::containsKey)
                        .sortedBy { sprintId -> sprintById.getValue(sprintId).startDate }

                if (sprintIds.isEmpty()) {
                    grouped.addTask(sprintId = null, projectId = projectId, task = taskInfo)
                } else {
                    sprintIds.forEach { sprintId ->
                        grouped.addTask(sprintId = sprintId, projectId = projectId, task = taskInfo)
                    }
                }
            }
        }

        return grouped.map { (sprintId, projectGroups) ->
            val projects =
                projectGroups.map { (projectId, projectTasks) ->
                    TaskResult.AssignedProjectGroup(
                        project =
                            projectId?.let(projectsById::get)?.let { project ->
                                TaskResult.Project(
                                    id = project.projectId,
                                    name = project.name,
                                )
                            },
                        taskCount = projectTasks.size,
                        tasks = projectTasks.toList(),
                    )
                }

            TaskResult.AssignedSprintGroup(
                sprint =
                    sprintId?.let(sprintById::get)?.let { sprint ->
                        TaskResult.Sprint(
                            id = sprint.id,
                            name = sprint.name,
                            startDate = sprint.startDate,
                            endDate = sprint.endDate,
                        )
                    },
                uniqueTaskCount =
                    projects
                        .flatMap(TaskResult.AssignedProjectGroup::tasks)
                        .map(TaskResult.AssignedTask::id)
                        .distinct()
                        .size,
                projects = projects,
            )
        }
    }

    private fun LinkedHashMap<UUID?, LinkedHashMap<UUID?, MutableList<TaskResult.AssignedTask>>>.addTask(
        sprintId: UUID?,
        projectId: UUID?,
        task: TaskResult.AssignedTask,
    ) {
        val projects = getOrPut(sprintId) { linkedMapOf() }
        val tasks = projects.getOrPut(projectId) { mutableListOf() }
        if (tasks.none { it.id == task.id }) {
            tasks += task
        }
    }

    private fun TaskResult.AssignedTaskListInfo.toAssignedTask(): TaskResult.AssignedTask =
        TaskResult.AssignedTask(
            id = id,
            title = title,
            status = status,
            assignee = assignee,
            dueDate = dueDate,
        )

    private fun TaskCommand.AddTask.toDomain(): Task =
        Task.create(
            title = title,
            description = description,
            assigneeId = assigneeId,
            workspaceId = workspaceId,
            dueDate = dueDate,
        )

    private fun TaskCommand.UpdateTask.toUpdateSpec(): TaskUpdateSpec =
        TaskUpdateSpec(
            title = title,
            description = description,
            status = status,
            assigneeId = assigneeId,
            dueDate = dueDate,
            priority = priority,
            actorId = actorId,
        )

    private fun markSprintMembership(
        task: Task,
        wasInActiveSprint: Boolean,
    ) {
        task.domainEvents().filterIsInstance<TaskDomainEvent>().forEach { event ->
            sprintMembershipRegistry.register(event.eventId, wasInActiveSprint)
        }
    }
}
