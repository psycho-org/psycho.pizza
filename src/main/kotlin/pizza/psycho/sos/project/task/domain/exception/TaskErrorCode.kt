package pizza.psycho.sos.project.task.domain.exception

import org.springframework.http.HttpStatus
import pizza.psycho.sos.common.exception.BaseErrorCode

enum class TaskErrorCode(
    override val status: HttpStatus,
    override val message: String,
) : BaseErrorCode {
    TASK_NOT_FOUND(HttpStatus.NOT_FOUND, "Task not found"),
    TASK_ID_NULL(HttpStatus.BAD_REQUEST, "Task ID must not be null"),
    TITLE_NOT_VALID(HttpStatus.BAD_REQUEST, "Title not valid"),
    DESCRIPTION_NOT_VALID(HttpStatus.BAD_REQUEST, "Description not valid"),
    TASK_INFO_NOT_FOUND(HttpStatus.BAD_REQUEST, "Task information not found"),
    INVALID_TRANSITION(HttpStatus.BAD_REQUEST, "Invalid transition"),
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "Invalid request"),
    ;

    override val code: String = name
}
