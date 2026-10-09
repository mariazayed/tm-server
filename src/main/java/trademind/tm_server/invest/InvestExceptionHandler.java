package trademind.tm_server.invest;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import trademind.tm_server.auth.RequestTooLargeException;
import trademind.tm_server.base.ErrorResponse;
import trademind.tm_server.invest.ai.AiInvalidReplyException;
import trademind.tm_server.invest.ai.AiLimitExceededException;
import trademind.tm_server.invest.ai.AiUnavailableException;
import trademind.tm_server.invest.ai.AiUpstreamException;

/**
 * An unreadable request body (bad JSON, unknown enum value, wrong type) is the caller's mistake, so
 * answer 400. The shared handler would report 500; this advice is scoped to the invest controllers
 * so the existing trade-journal endpoints keep their behaviour.
 */
@RestControllerAdvice(basePackages = "trademind.tm_server.invest")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class InvestExceptionHandler {

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> unreadableBody(HttpMessageNotReadableException ex, HttpServletRequest request) {
        for (Throwable t = ex; t != null; t = t.getCause() == t ? null : t.getCause()) {
            if (t instanceof RequestTooLargeException) {
                return ResponseEntity.status(HttpStatus.CONTENT_TOO_LARGE).body(new ErrorResponse(
                        HttpStatus.CONTENT_TOO_LARGE.value(), "Payload Too Large", "Request body is too large",
                        request.getRequestURI()));
            }
        }
        // The parser message can echo input and internals, so keep the response generic.
        ErrorResponse body = new ErrorResponse(HttpStatus.BAD_REQUEST.value(), "Bad Request",
                "Request body is missing or not valid JSON for this endpoint", request.getRequestURI());
        return ResponseEntity.badRequest().body(body);
    }

    /** AI is off or not configured: the message names the setting and is safe to show. */
    @ExceptionHandler(AiUnavailableException.class)
    public ResponseEntity<ErrorResponse> aiUnavailable(AiUnavailableException ex, HttpServletRequest request) {
        return error(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage(), request);
    }

    @ExceptionHandler(AiLimitExceededException.class)
    public ResponseEntity<ErrorResponse> aiLimit(AiLimitExceededException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header(HttpHeaders.RETRY_AFTER, String.valueOf(ex.retryAfterSeconds()))
                .body(body(HttpStatus.TOO_MANY_REQUESTS, ex.getMessage(), request));
    }

    /** A failed model call. The provider's error is never passed on: it can echo what was sent. */
    @ExceptionHandler(AiUpstreamException.class)
    public ResponseEntity<ErrorResponse> aiUpstream(AiUpstreamException ex, HttpServletRequest request) {
        return error(HttpStatus.BAD_GATEWAY, "The AI service did not answer. Try again in a moment.", request);
    }

    /** Two unusable replies in a row; the reply itself is never shown. */
    @ExceptionHandler(AiInvalidReplyException.class)
    public ResponseEntity<ErrorResponse> aiInvalidReply(AiInvalidReplyException ex, HttpServletRequest request) {
        return error(HttpStatus.BAD_GATEWAY, "The AI answered in a form that could not be used. Try again.", request);
    }

    private static ResponseEntity<ErrorResponse> error(HttpStatus status, String message, HttpServletRequest request) {
        return ResponseEntity.status(status).body(body(status, message, request));
    }

    private static ErrorResponse body(HttpStatus status, String message, HttpServletRequest request) {
        return new ErrorResponse(status.value(), status.getReasonPhrase(), message, request.getRequestURI());
    }
}
