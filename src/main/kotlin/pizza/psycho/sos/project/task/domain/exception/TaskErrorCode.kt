package pizza.psycho.sos.project.task.domain.exception

import org.springframework.http.HttpStatus
import pizza.psycho.sos.common.exception.BaseErrorCode

enum class TaskErrorCode(
    override val status: HttpStatus,
    override val message: String,
) : BaseErrorCode {
    TASK_ID_NOT_FOUND(HttpStatus.BAD_REQUEST, "Task ID not found"),
    TASK_NOT_FOUND(HttpStatus.NOT_FOUND, "Task not found"),
    TASK_ID_NULL(HttpStatus.BAD_REQUEST, "Task ID not null"),
    TITLE_NOT_VALID(HttpStatus.BAD_REQUEST, "Title not valid"),
    DESCRIPTION_NOT_VALID(HttpStatus.BAD_REQUEST, "Description not valid"),
    TASK_INFORMATION_VALIDATION(HttpStatus.BAD_REQUEST, "Task Information validation"),
    ;

    override val code: String = name
}
