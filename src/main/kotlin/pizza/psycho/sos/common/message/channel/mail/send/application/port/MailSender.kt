package pizza.psycho.sos.common.message.channel.mail.send.application.port

import pizza.psycho.sos.common.message.channel.MessageChannelSender
import pizza.psycho.sos.common.message.channel.mail.send.application.model.MailSendRequest

interface MailSender : MessageChannelSender<MailSendRequest>
