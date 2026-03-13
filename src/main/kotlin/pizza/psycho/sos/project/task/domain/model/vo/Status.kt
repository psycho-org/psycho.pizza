package pizza.psycho.sos.project.task.domain.model.vo

enum class Status {
    TODO,
    IN_PROGRESS,
    DONE,
    CANCELLED,
    ;

    fun isTransitionableTo(newStatus: Status): Boolean = this != newStatus
}
