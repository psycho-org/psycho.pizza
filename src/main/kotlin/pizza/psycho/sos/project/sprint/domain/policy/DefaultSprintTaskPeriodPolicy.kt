package pizza.psycho.sos.project.sprint.domain.policy

import org.springframework.stereotype.Component
import pizza.psycho.sos.project.sprint.domain.model.vo.Period
import java.time.Instant

/**
 * 기본 Sprint-Task 기간 정책 구현.
 *
 * 현재는 실제 검증 로직을 가지고 있지 않으며, 추후 SprintErrorCode 와 함께
 * 도메인 예외를 던지는 방식으로 확장할 수 있다.
 */
@Component
class DefaultSprintTaskPeriodPolicy : SprintTaskPeriodPolicy {
    override fun isTaskDueDateWithinSprint(
        sprintPeriod: Period,
        taskDueDate: Instant?,
    ): Boolean {
        if (taskDueDate == null) {
            // dueDate 가 없으면 기간 검증 대상이 아니다
            return true
        }

        val start = sprintPeriod.startDate
        val end = sprintPeriod.endDate

        return !taskDueDate.isBefore(start) && !taskDueDate.isAfter(end)
    }
}
