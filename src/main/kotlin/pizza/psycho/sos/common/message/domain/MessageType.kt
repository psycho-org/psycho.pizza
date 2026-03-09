package pizza.psycho.sos.common.message.domain

enum class MessageType(
    val supportedChannels: Set<MessageChannel>,
) {
    WORKSPACE_INVITE(setOf(MessageChannel.EMAIL)),
}
