package pizza.psycho.sos.common.support.transaction.config

import org.springframework.beans.factory.InitializingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import pizza.psycho.sos.common.support.transaction.helper.Tx
import pizza.psycho.sos.common.support.transaction.runner.TransactionRunner

@Configuration
class TransactionConfig {
    @Bean("txInitBean")
    fun txInitialize(txRunner: TransactionRunner): InitializingBean = InitializingBean { Tx.initialize(txRunner) }
}
