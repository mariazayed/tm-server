package trademind.tm_server.invest;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import trademind.tm_server.auth.RequestTooLargeException;
import trademind.tm_server.base.ErrorResponse;

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
}
