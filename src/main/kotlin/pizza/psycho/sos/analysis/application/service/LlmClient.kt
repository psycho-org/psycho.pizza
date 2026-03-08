package pizza.psycho.sos.analysis.application.service

interface LlmClient {
    fun analyze(prompt: String): String
}
