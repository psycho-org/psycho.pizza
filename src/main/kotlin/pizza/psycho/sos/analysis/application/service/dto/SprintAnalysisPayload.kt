package pizza.psycho.sos.analysis.application.service.dto

import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.annotation.JsonNaming
import java.time.Instant
import java.util.UUID

/**
 * 릴레이 서버(LLM 분석 워커)로 전달되는 분석 데이터의 최상위 Root Payload
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
data class SprintAnalysisPayload(
    val schemaVersion: String = "1.0.0", // 데이터 포맷 호환성을 위한 스키마 버전
    val context: AnalysisContextDto, // 어떤 워크스페이스/스프린트인지에 대한 기본 메타데이터
    val summary: AnalysisSummaryDto, // 스프린트의 전체적인 상태 요약 (완료율 등)
    val penaltyScores: PenaltyScoresDto, // 4가지 주요 지표별 산정된 감점 결과
    val metrics: SprintMetricsDto, // 감점의 근거가 되는 구체적인 정량적 메트릭(수치/비율)
    val anomalies: List<AnomalyDto>, // LLM이 리포트에 인용할 구체적인 이상 징후 사례 목록
)

/**
 * 분석 대상의 식별 컨텍스트
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
data class AnalysisContextDto(
    val workspaceId: UUID, // 데이터가 속한 워크스페이스 ID (격리용)
    val sprint: SprintContextDto, // 대상 스프린트 정보
)

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
data class SprintContextDto(
    val id: UUID,
    val name: String, // 스프린트 이름 (예: "Sprint 14")
    val periodDays: Int, // 스프린트 진행 기간 (일)
    val activeMembersCount: Int, // 스프린트 내 활동 중인 팀원 수 (WIP 한도 계산 시 사용)
    val totalTasksCount: Int, // 스프린트 내 전체 태스크 수
    val reportExcluded: Boolean, // 프로젝트나 태스크가 아예 없어 분석 대상에서 제외되는지 여부
    val reportExcludedReason: String? = null, // 제외된 경우 그 이유
)

/**
 * 전체 진행 상태 요약
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
data class AnalysisSummaryDto(
    val statusSnapshot: StatusSnapshotDto, // 현재 시점의 상태별 개수 스냅샷
    val csgOverview: CsgOverviewDto, // CSG(Completed State Group) 관점의 요약
)

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
data class StatusSnapshotDto(
    val todoCount: Int,
    val inProgressCount: Int,
    val doneCount: Int,
    val canceledCount: Int,
)

/**
 * CSG (Completed State Group): Task가 완료되었거나(Done) 취소된(Canceled) 종결 상태 그룹
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
data class CsgOverviewDto(
    val totalCsgCount: Int, // Done + Canceled 합계
    val nonCsgCount: Int, // Todo + InProgress 합계 (미완료 잔여 작업)
    val csgRatio: Double, // 전체 태스크 대비 종결(CSG) 비율
)

/**
 * 4가지 평가 영역별 감점 점수표
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
data class PenaltyScoresDto(
    val completion: ScoreDetailDto, // 완결력 감점 (마무리 깔끔도)
    val stability: ScoreDetailDto, // 계획 안정성 감점 (목표/일정 변경 여부)
    val flow: ScoreDetailDto, // 흐름 감점 (정체, 리워크 여부)
    val ownership: ScoreDetailDto, // 책임/리소스 감점 (업무 편중도)
)

/**
 * 점수 세부 정보 (캡(Cap) 적용 로직 포함)
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
data class ScoreDetailDto(
    val rawScore: Int, // 발생한 이벤트별 감점 합계 (단순 합산)
    val appliedScore: Int, // 상한선(maxCap)을 적용한 실제 최종 감점 (min(rawScore, maxCap))
    val maxCap: Int, // 해당 영역이 최대로 깎일 수 있는 상한 점수
)

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
data class SprintMetricsDto(
    val completion: CompletionMetricsDto,
    val stability: StabilityMetricsDto,
    val flow: FlowMetricsDto,
    val ownership: OwnershipMetricsDto,
)

/**
 * 비율 계산을 위한 공통 DTO (LLM의 수학적 환각 방지용)
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
data class RatioMetricDto(
    val count: Int, // 분자 (해당 이벤트 발생 건수)
    val baseCount: Int, // 분모 (기준이 되는 전체 건수)
    val ratio: Double, // 계산된 실제 비율 (count / baseCount)
)

/**
 * 완결력 (Completion) 메트릭
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
data class CompletionMetricsDto(
    val unassignedTasksCount: Int, // 담당자(Assignee)가 지정되지 않은 태스크 수
    val nonCsgTasksExist: Boolean, // 스프린트 종료 시점 기준 미완료(Non-CSG) 태스크 존재 여부
    val nonCsgTasksCount: Int, // 미완료 태스크 개수
    val canceledToDoneInCsg: RatioMetricDto, // 종결(CSG) 그룹 내에서 취소(Canceled)된 비율
    val emptyProjectExists: Boolean, // 태스크가 하나도 없는 빈 프로젝트가 존재하는지
    val emptyProjectsCount: Int, // 빈 프로젝트 개수
)

/**
 * 계획 안정성 (Stability) 메트릭
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
data class StabilityMetricsDto(
    val outOfCommitDoneTasks: RatioMetricDto, // 초기 계획(Commit) 외에 나중에 추가되어 Done 처리된 비율
    val lastMinuteDoneTasks: RatioMetricDto, // 스프린트 종료 직전(D-2 내)에 한꺼번에 Done 처리된 몰아치기 비율
    val tasksWithoutDueDate: RatioMetricDto, // 마감일(DueDate)이 아예 설정되지 않은 태스크 비율
    val deletedTasksDuringSprint: RatioMetricDto, // 스프린트 진행 도중 삭제된 태스크 비율
    val lateDoneTasksCount: Int, // 마감일(DueDate)을 넘겨서 뒤늦게 Done 처리된 개수
    val dueDateExtensionCounts: DueDateExtensionCountsDto, // 마감일 연장 횟수별 통계
    val sprintGoalChangeCount: Int, // 스프린트 목표(Goal)가 수정된 횟수
    val sprintPeriodChangeCount: Int, // 스프린트 기간(Period)이 연장/단축된 횟수
    val taskProjectChangeCount: Int, // 태스크의 소속 프로젝트가 변경된 횟수
)

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
data class DueDateExtensionCountsDto(
    val extended1TimeCount: Int, // 1회 연장 건수
    val extended2TimesCount: Int, // 2회 연장 건수
    val extended3OrMoreTimesCount: Int, // 3회 이상 상습 연장 건수
)

/**
 * 흐름 (Flow) 메트릭
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
data class FlowMetricsDto(
    val reworkEventsCount: Int, // 리워크(상태 회귀: Done -> InProgress 등) 발생 건수 합계
    val todoToDoneDirectCount: Int, // InProgress를 거치지 않고 Todo에서 Done으로 직행한 건수
    val longPendingBottleneckTasksCount: Int, // 병목(특정 상태, 주로 InProgress에 장기간 방치됨) 태스크 수
    val canceledTasksCount: Int, // 진행 중 취소된 태스크 수
    val doneToInProgressCount: Int, // Done 상태였다가 작업(InProgress)으로 되돌아간 건수
    val inProgressToTodoCount: Int, // 작업을 하다가 다시 할일(Todo) 상태로 되돌아간 건수
    val scopeChurnEventsCount: Int, // 스프린트 도중 태스크 추가/삭제가 빈번하게 일어난 이벤트 수(Scope Churn)
)

/**
 * 책임/리소스 (Ownership) 메트릭
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
data class OwnershipMetricsDto(
    val frequentAssigneeChangeTasksCount: Int, // 담당자(Assignee)가 핑퐁 치듯 너무 자주 바뀐 태스크 수
    val workloadConcentration: WorkloadConcentrationDto, // 특정 인원에게 업무가 몰렸는지 여부
    val maxWipRatio: Double, // 인원수 대비 최대 WIP(Work In Progress) 도달 비율
    val wipLimitExceededDays: Int, // WIP 한도 초과 상태가 지속된 일수
)

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
data class WorkloadConcentrationDto(
    val isConcentrated: Boolean, // 업무 편중이 심각한 수준인지에 대한 논리적 판단
    val topAssigneeTaskShare: Double, // 작업을 가장 많이 맡은 1명이 차지하는 비중 (예: 0.42 = 42%)
    val top2AssigneesTaskShare: Double, // 작업을 가장 많이 맡은 상위 2명이 차지하는 비중
)

/**
 * LLM이 인과관계를 설명하고 리포트를 쓸 때 인용할 구체적인 '이상 징후(사례)'
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
data class AnomalyDto(
    val entityType: String, // 이슈 발생 대상 타입 (예: "TASK", "SPRINT", "PROJECT")
    val entityId: UUID, // 이슈가 발생한 대상의 고유 ID
    val projectId: UUID? = null, // (Task인 경우) 소속된 프로젝트 ID (맥락 파악용)
    val projectName: String? = null, // (Task인 경우) 소속된 프로젝트명
    val issueType: String, // 이슈의 성격 (예: "REWORK_AND_LATE", "BOTTLENECK", "SPRINT_GOAL_CHANGED")
    val occurredAt: Instant, // 이슈가 발생한(또는 감지된) 정확한 시간
    val dayOffset: Int, // 스프린트 시작일을 기준으로 며칠 차에 발생했는지 (시계열 흐름 파악용)
    val durationDays: Int? = null, // 병목(BOTTLENECK)처럼 '방치된 기간'이 중요할 때 일 단위로 명시
    val evidenceTags: List<String>, // LLM이 문장을 만들 때 사용할 팩트 키워드 모음
)
