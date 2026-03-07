package pizza.psycho.sos.analysis.application.service

import java.util.UUID

interface AnalysisJobQueue {
    fun enqueue(jobId: UUID)
}
