package pizza.psycho.sos.common.message.channel

import pizza.psycho.sos.common.message.domain.MessageChannel

interface MessageChannelSender<T> {
    val channel: MessageChannel

    fun send(request: T)
}
