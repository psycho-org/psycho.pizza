package pizza.psycho.sos.common.support.transaction.runner

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import pizza.psycho.sos.common.support.transaction.annotation.ReadOnlyTransactional

@Component
open class TransactionRunner {
    @Transactional
    open fun <T> run(block: () -> T): T = block()

    @ReadOnlyTransactional
    open fun <T> runReadOnly(block: () -> T): T = block()
}
