package pizza.psycho.sos.analysis.application.port

import pizza.psycho.sos.analysis.application.port.dto.AnalysisJobQueueItem

interface AnalysisJobQueueConsumer {
    fun take(): AnalysisJobQueueItem
}
