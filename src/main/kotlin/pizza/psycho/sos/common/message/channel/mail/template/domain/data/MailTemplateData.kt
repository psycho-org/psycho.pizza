package pizza.psycho.sos.common.message.channel.mail.template.domain.data

import pizza.psycho.sos.common.message.domain.MessageType

/**
 * 메일 템플릿 입력 데이터의 공통 계약.
 *
 * 사용법:
 * 1) `MessageType`에 타입 추가
 * 2) 이 패키지에 DTO 파일을 새로 추가
 * 3) DB `mail_templates`에 해당 타입 템플릿 레코드 추가
 *
 * DTO는 템플릿에서 사용하는 변수명을 `variables()`로 제공해야 한다.
 */
sealed interface MailTemplateData {
    val mailType: MessageType

    fun variables(): Map<String, String?>
}
