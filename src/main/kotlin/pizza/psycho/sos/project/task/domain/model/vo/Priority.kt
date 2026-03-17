package pizza.psycho.sos.project.task.domain.model.vo

enum class Priority {
    LOW,
    MEDIUM,
    HIGH,
    ;

    fun isTransitionableTo(newPriority: Priority): Boolean = this != newPriority
}
