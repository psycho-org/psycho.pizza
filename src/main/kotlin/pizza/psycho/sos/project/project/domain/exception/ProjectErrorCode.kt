package pizza.psycho.sos.project.project.domain.exception

import org.springframework.http.HttpStatus
import pizza.psycho.sos.common.exception.BaseErrorCode

enum class ProjectErrorCode(
    override val status: HttpStatus,
    override val message: String,
) : BaseErrorCode {
    PROJECT_ID_NULL(HttpStatus.BAD_REQUEST, "project_id is null."),
    PROJECT_NOT_FOUND(HttpStatus.NOT_FOUND, "Project not found"),
    PROJECT_NAME_NULL(HttpStatus.BAD_REQUEST, "Project name is null."),
    TASK_NOT_FOUND(HttpStatus.NOT_FOUND, "Task not found"),
    SAME_PROJECT(HttpStatus.BAD_REQUEST, "Same project"),
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "Invalid request"),
    ;

    override val code: String = name
}
