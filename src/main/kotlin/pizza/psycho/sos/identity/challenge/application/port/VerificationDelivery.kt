package pizza.psycho.sos.identity.challenge.application.port

import pizza.psycho.sos.identity.challenge.domain.vo.OperationType

interface VerificationDelivery {
    fun sendOtp(
        email: String,
        otp: String,
        operationType: OperationType,
    )
}
