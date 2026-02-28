package pizza.psycho.sos.common.support.transaction.runner

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import pizza.psycho.sos.common.support.transaction.annotation.ReadOnlyTransactional

@Component
class TransactionRunner {
    @Transactional
    fun <T> run(block: () -> T): T = block()

    @ReadOnlyTransactional
    fun <T> runReadOnly(block: () -> T): T = block()
}
