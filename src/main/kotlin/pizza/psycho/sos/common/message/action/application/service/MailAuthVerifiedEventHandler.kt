package pizza.psycho.sos.common.message.action.application.service

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener
import pizza.psycho.sos.common.message.token.application.event.MailAuthVerifiedEvent
import pizza.psycho.sos.common.message.token.domain.repository.MailAuthTokenRepository

@Component
class MailAuthVerifiedEventHandler(
    private val mailActionService: MailActionService,
    private val mailAuthTokenRepository: MailAuthTokenRepository,
) {
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun onVerified(event: MailAuthVerifiedEvent) {
        val token = mailAuthTokenRepository.findByToken(event.token)
        if (token == null) {
            logger.warn("Mail auth token not found for verified event. token={}", event.token)
            return
        }
        try {
            mailActionService.handle(event.actionRequest)
            token.markActionSuccess()
            mailAuthTokenRepository.save(token)
        } catch (ex: Exception) {
            token.markActionFailed(ex.message)
            mailAuthTokenRepository.save(token)
            logger.error("Mail action failed. token={} actionType={}", event.token, event.actionRequest.actionType, ex)
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(MailAuthVerifiedEventHandler::class.java)
    }
}
