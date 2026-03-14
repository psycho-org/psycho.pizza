package pizza.psycho.sos.analysis.application.port

import pizza.psycho.sos.analysis.application.port.dto.AnalysisJobQueueItem

interface AnalysisJobQueueProducer {
    fun enqueue(item: AnalysisJobQueueItem)
}
