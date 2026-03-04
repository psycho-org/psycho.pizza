package pizza.psycho.sos.common.exception

import org.springframework.http.HttpStatus

interface BaseErrorCode {
    val status: HttpStatus
    val code: String
    val message: String
}
