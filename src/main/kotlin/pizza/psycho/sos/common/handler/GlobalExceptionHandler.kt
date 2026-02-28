package pizza.psycho.sos.common.handler

import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.ConstraintViolationException
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.security.access.AccessDeniedException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.server.ResponseStatusException
import java.time.Instant

@RestControllerAdvice
class GlobalExceptionHandler {
    @ExceptionHandler(ResponseStatusException::class)
    fun handleResponseStatus(
        ex: ResponseStatusException,
        request: HttpServletRequest,
    ) = ex.statusCode.toResponse(
        message = ex.reason ?: ex.statusCode.reasonPhraseOrFallback(),
        path = request.requestURI,
    )

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleMethodArgumentNotValid(
        ex: MethodArgumentNotValidException,
        request: HttpServletRequest,
    ) = HttpStatus.BAD_REQUEST.toResponse(
        message = "Validation Failed",
        path = request.requestURI,
        details =
            ex.bindingResult.fieldErrors.groupBy(
                keySelector = { it.field },
                valueTransform = { it.defaultMessage ?: "Invalid value" },
            ),
    )

    @ExceptionHandler(ConstraintViolationException::class)
    fun handleConstraintViolation(
        ex: ConstraintViolationException,
        request: HttpServletRequest,
    ) = HttpStatus.BAD_REQUEST.toResponse(
        message = "Validation Failed",
        path = request.requestURI,
        details =
            ex.constraintViolations.groupBy(
                keySelector = { it.propertyPath.toString() },
                valueTransform = { it.message },
            ),
    )

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleHttpMessageNotReadable(
        ex: HttpMessageNotReadableException,
        request: HttpServletRequest,
    ) = HttpStatus.BAD_REQUEST.toResponse(
        message = "Malformed JSON request",
        path = request.requestURI,
    )

    @ExceptionHandler(AccessDeniedException::class)
    fun handleAccessDenied(
        ex: AccessDeniedException,
        request: HttpServletRequest,
    ) = HttpStatus.FORBIDDEN.toResponse(
        message = "Forbidden",
        path = request.requestURI,
    )

    @ExceptionHandler(Exception::class)
    fun handleException(
        ex: Exception,
        request: HttpServletRequest,
    ): ResponseEntity<ErrorResponse> {
        logger.error("Unhandled exception at path={}", request.requestURI, ex)
        return HttpStatus.INTERNAL_SERVER_ERROR.toResponse(
            message = "Internal Server Error",
            path = request.requestURI,
        )
    }

    // EXTENSIONS -----------------------------------------------------------------------------------------------------

    private fun HttpStatus.toResponse(
        code: String? = null,
        message: String,
        path: String,
        details: Map<String, List<String>>? = null,
    ): ResponseEntity<ErrorResponse> =
        ResponseEntity.status(this).body(
            ErrorResponse(
                timestamp = Instant.now(),
                status = value(),
                error = reasonPhrase,
                code = code,
                message = message,
                path = path,
                details = details,
            ),
        )

    private fun HttpStatusCode.toResponse(
        code: String? = null,
        message: String,
        path: String,
        details: Map<String, List<String>>? = null,
    ): ResponseEntity<ErrorResponse> =
        ResponseEntity.status(this).body(
            ErrorResponse(
                timestamp = Instant.now(),
                status = value(),
                error = reasonPhraseOrFallback(),
                code = code,
                message = message,
                path = path,
                details = details,
            ),
        )

    private fun HttpStatusCode.reasonPhraseOrFallback(): String = HttpStatus.resolve(value())?.reasonPhrase ?: "HTTP ${value()}"

    companion object {
        private val logger = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)
    }
}
