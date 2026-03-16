package pizza.psycho.sos.analysis.domain.vo

enum class AnalysisEventSubtype {
    GOAL_UPDATED,
    PERIOD_UPDATED,
    STATUS_REGRESSION_FROM_DONE,
    TODO_TO_DONE_DIRECT,
    SCOPE_CHURN,
    STATUS_CHANGED_TO_CANCELED,
}
