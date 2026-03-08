package pizza.psycho.sos.analysis.application.service

import java.util.UUID

interface AnalysisJobQueueConsumer {
    fun take(): UUID
}
