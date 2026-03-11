package pizza.psycho.sos.identity.challenge.application.port

import pizza.psycho.sos.identity.account.domain.vo.Email
import pizza.psycho.sos.identity.challenge.domain.vo.OperationType

interface VerificationDelivery {
    fun sendOtp(
        email: Email,
        otp: String,
        operationType: OperationType,
    )
}
