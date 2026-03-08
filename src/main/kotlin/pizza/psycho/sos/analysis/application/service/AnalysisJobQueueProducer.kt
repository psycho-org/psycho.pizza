package pizza.psycho.sos.analysis.application.service

import java.util.UUID

interface AnalysisJobQueueProducer {
    fun enqueue(jobId: UUID)
}
