package pizza.psycho.sos.project.project.domain.model.vo

data class Progress(
    val totalCount: Int,
    val completedCount: Int,
) {
    val value: Double
        get() = if (totalCount == 0) 0.0 else completedCount.toDouble() / totalCount
}
