package pizza.psycho.sos.analysis.application.service

import org.springframework.stereotype.Service
import pizza.psycho.sos.analysis.application.service.dto.SprintAnalysisInput
import pizza.psycho.sos.analysis.domain.exception.AnalysisErrorCode
import pizza.psycho.sos.analysis.domain.vo.AnalysisEventSubtype
import pizza.psycho.sos.common.handler.DomainException
import pizza.psycho.sos.project.common.domain.model.vo.WorkspaceId
import pizza.psycho.sos.project.project.application.port.out.ProjectPort
import pizza.psycho.sos.project.project.application.port.out.dto.ProjectSnapshot
import pizza.psycho.sos.project.sprint.application.port.out.SprintPort
import pizza.psycho.sos.project.task.application.port.out.TaskPort
import pizza.psycho.sos.project.task.application.port.out.dto.TaskSnapshot
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

@Service
class SprintAnalysisMetricService(
    private val sprintService: SprintPort,
    private val taskService: TaskPort,
    private val projectService: ProjectPort,
    private val analysisMetricCountService: AnalysisMetricCountService,
) {
    /*
     * 큐에 보낼 input payload 생성
     * - LLM이 바로 읽을 수 있는 정리된 입력값 생성
     */
    fun buildInput(
        workspaceId: UUID,
        sprintId: UUID,
    ): SprintAnalysisInput {
        val sprint =
            sprintService.findByIdWithProjects(
                sprintId = sprintId,
                workspaceId = WorkspaceId(workspaceId),
            ) ?: throw DomainException(AnalysisErrorCode.SPRINT_NOT_FOUND)

        val projectsInSprint =
            projectService.findByIdIn(
                projectIds = sprint.projectIds,
                workspaceId = WorkspaceId(workspaceId),
            )

        val tasksInProject =
            taskService.findByIdIn(
                ids = projectsInSprint.flatMap(ProjectSnapshot::taskIds).distinct(),
                workspaceId = WorkspaceId(workspaceId),
            )

        val snapshot = calculateSnapshot(tasksInProject)

        val eventCounts =
            analysisMetricCountService.getCounts(
                workspaceId = workspaceId,
                sprintId = sprintId,
            )

        return SprintAnalysisInput(
            schemaVersion = "0.1.0",
            context =
                SprintAnalysisInput.Context(
                    workspaceId = workspaceId,
                    sprint =
                        SprintAnalysisInput.Context.Sprint(
                            id = sprint.sprintId,
                            name = sprint.name,
                            periodDays = calculatePeriodDays(sprint.startDate, sprint.endDate),
                            totalTasksCount = tasksInProject.size,
                        ),
                ),
            summary =
                SprintAnalysisInput.Summary(
                    statusSnapshot =
                        SprintAnalysisInput.Summary.StatusSnapshot(
                            todoCount = snapshot.todoCount,
                            inProgressCount = snapshot.inProgressCount,
                            doneCount = snapshot.doneCount,
                            canceledCount = snapshot.canceledCount,
                        ),
                ),
            metrics =
                SprintAnalysisInput.Metrics(
                    completion =
                        SprintAnalysisInput.Metrics.Completion(
                            unassignedTasksCount = snapshot.unassignedTasksCount,
                        ),
                    stability =
                        SprintAnalysisInput.Metrics.Stability(
                            sprintGoalChangeCount = eventCounts[AnalysisEventSubtype.GOAL_UPDATED] ?: 0,
                            sprintPeriodChangeCount = eventCounts[AnalysisEventSubtype.PERIOD_UPDATED] ?: 0,
                        ),
                    flow =
                        SprintAnalysisInput.Metrics.Flow(
                            reworkEventsCount = eventCounts[AnalysisEventSubtype.STATUS_REGRESSION_FROM_DONE] ?: 0,
                            todoToDoneDirectCount = eventCounts[AnalysisEventSubtype.TODO_TO_DONE_DIRECT] ?: 0,
                            scopeChurnEventsCount = eventCounts[AnalysisEventSubtype.SCOPE_CHURN] ?: 0,
                            canceledTasksCount = eventCounts[AnalysisEventSubtype.STATUS_CHANGED_TO_CANCELED] ?: 0,
                        ),
                ),
        )
    }

    private fun calculateSnapshot(tasks: List<TaskSnapshot>): SnapshotCounts =
        SnapshotCounts(
            todoCount = tasks.count { it.status.name == "TODO" },
            inProgressCount = tasks.count { it.status.name == "IN_PROGRESS" },
            doneCount = tasks.count { it.status.name == "DONE" },
            canceledCount = tasks.count { it.status.name == "CANCELED" },
            unassignedTasksCount = tasks.count { it.assigneeId == null },
        )

    private fun calculatePeriodDays(
        startDate: Instant,
        endDate: Instant,
    ): Int = ChronoUnit.DAYS.between(startDate, endDate).toInt() + 1

    private data class SnapshotCounts(
        val todoCount: Int,
        val inProgressCount: Int,
        val doneCount: Int,
        val canceledCount: Int,
        val unassignedTasksCount: Int,
    )
}
