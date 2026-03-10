package pizza.psycho.sos.analysis.application.port

import java.util.UUID

interface AnalysisJobQueueConsumer {
    fun take(): UUID
}
