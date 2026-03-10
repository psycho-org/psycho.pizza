package pizza.psycho.sos.analysis.application.port

import java.util.UUID

interface AnalysisJobQueueProducer {
    fun enqueue(jobId: UUID)
}
