package pizza.psycho.sos.project.sprint.domain.policy

import pizza.psycho.sos.project.sprint.domain.model.vo.Period
import java.time.Instant

/**
 * Sprint 관점에서 Task 기간(예: dueDate)이 Sprint 기간 안에 들어오는지 판단하기 위한 정책 인터페이스.
 *
 * 예외를 던지지 않고 boolean 값으로만 결과를 반환한다.
 */
interface SprintTaskPeriodPolicy {
    /**
     * 단일 Task 의 마감일이 Sprint 기간 안에 있는지 여부를 반환한다.
     *
     * @param sprintPeriod Sprint 의 기간
     * @param taskDueDate Task 의 마감일 (null 이면 검증하지 않고 true 를 반환)
     */
    fun isTaskDueDateWithinSprint(
        sprintPeriod: Period,
        taskDueDate: Instant?,
    ): Boolean
}
