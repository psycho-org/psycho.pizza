package pizza.psycho.sos.project.sprint.domain.exception

import org.springframework.http.HttpStatus
import pizza.psycho.sos.common.exception.BaseErrorCode

enum class SprintErrorCode(
    override val status: HttpStatus,
    override val message: String,
) : BaseErrorCode {
    SPRINT_ID_NOT_FOUND(HttpStatus.NOT_FOUND, "sprint_id not found"),
    SPRINT_NAME_NOT_VALID(HttpStatus.BAD_REQUEST, "sprint name not valid"),
    INVALID_DATE_RANGE(HttpStatus.BAD_REQUEST, "Invalid dateRange"),
    PROJECT_NOT_FOUND(HttpStatus.BAD_REQUEST, "project not found"),
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "invalid request"),
    TASK_DUE_DATE_OUTSIDE_SPRINT_PERIOD(HttpStatus.BAD_REQUEST, "task due date must be within sprint period"),
    GOAL_NOT_EMPTY_OR_BLANK(HttpStatus.BAD_REQUEST, "goal not empty or blank"),
    GOAL_LENGTH_TOO_LONG(HttpStatus.BAD_REQUEST, "goal length too long"),
    ;

    override val code: String = name
}
