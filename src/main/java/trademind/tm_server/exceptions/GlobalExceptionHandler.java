package trademind.tm_server.exceptions;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import trademind.tm_server.base.ErrorResponse;

import java.util.ArrayList;
import java.util.List;

/**
 * Global exception handler for the application using Spring's @RestControllerAdvice.
 * Provides centralized exception handling across all @RequestMapping methods.
 * Converts exceptions into standardized ErrorResponse objects with appropriate HTTP status codes.
 * All exceptions are logged for debugging and monitoring purposes.
 *
 * @see ErrorResponse
 * @see RestControllerAdvice
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Handles ResourceNotFoundException thrown when a requested resource cannot be found.
     * Returns HTTP 404 Not Found status.
     *
     * @param ex      The ResourceNotFoundException that was thrown
     * @param request The HTTP request that caused the exception
     * @return ResponseEntity containing ErrorResponse with 404 status and error details
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFoundException(
            ResourceNotFoundException ex,
            HttpServletRequest request) {

        logger.error("Resource not found: {}", ex.getMessage());

        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.NOT_FOUND.value(),
                "Not Found",
                ex.getMessage(),
                request.getRequestURI()
        );

        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
    }

    /**
     * Handles BadRequestException thrown when a request contains invalid data or violates business rules.
     * Returns HTTP 400 Bad Request status.
     *
     * @param ex      The BadRequestException that was thrown
     * @param request The HTTP request that caused the exception
     * @return ResponseEntity containing ErrorResponse with 400 status and error details
     */
    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ErrorResponse> handleBadRequestException(
            BadRequestException ex,
            HttpServletRequest request) {

        logger.error("Bad request: {}", ex.getMessage());

        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Bad Request",
                ex.getMessage(),
                request.getRequestURI()
        );

        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handles ConflictException thrown when an operation conflicts with the current state of the resource.
     * Common scenarios include duplicate entries or concurrent modification conflicts.
     * Returns HTTP 409 Conflict status.
     *
     * @param ex      The ConflictException that was thrown
     * @param request The HTTP request that caused the exception
     * @return ResponseEntity containing ErrorResponse with 409 status and error details
     */
    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ErrorResponse> handleConflictException(
            ConflictException ex,
            HttpServletRequest request) {

        logger.error("Conflict: {}", ex.getMessage());

        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.CONFLICT.value(),
                "Conflict",
                ex.getMessage(),
                request.getRequestURI()
        );

        return new ResponseEntity<>(errorResponse, HttpStatus.CONFLICT);
    }

    /**
     * Handles UnauthorizedException thrown when authentication is required but not provided or invalid.
     * Returns HTTP 401 Unauthorized status.
     *
     * @param ex      The UnauthorizedException that was thrown
     * @param request The HTTP request that caused the exception
     * @return ResponseEntity containing ErrorResponse with 401 status and error details
     */
    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorizedException(
            UnauthorizedException ex,
            HttpServletRequest request) {

        logger.error("Unauthorized: {}", ex.getMessage());

        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.UNAUTHORIZED.value(),
                "Unauthorized",
                ex.getMessage(),
                request.getRequestURI()
        );

        return new ResponseEntity<>(errorResponse, HttpStatus.UNAUTHORIZED);
    }

    /**
     * Handles ForbiddenException thrown when an authenticated user lacks permission for the requested resource.
     * Returns HTTP 403 Forbidden status.
     *
     * @param ex      The ForbiddenException that was thrown
     * @param request The HTTP request that caused the exception
     * @return ResponseEntity containing ErrorResponse with 403 status and error details
     */
    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ErrorResponse> handleForbiddenException(
            ForbiddenException ex,
            HttpServletRequest request) {

        logger.error("Forbidden: {}", ex.getMessage());

        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.FORBIDDEN.value(),
                "Forbidden",
                ex.getMessage(),
                request.getRequestURI()
        );

        return new ResponseEntity<>(errorResponse, HttpStatus.FORBIDDEN);
    }

    /**
     * Handles MethodArgumentNotValidException thrown when @Valid annotation fails on request body validation.
     * Extracts all field-level validation errors and returns them in a structured format.
     * Returns HTTP 400 Bad Request status.
     *
     * @param ex      The MethodArgumentNotValidException containing validation errors
     * @param request The HTTP request that caused the exception
     * @return ResponseEntity containing ErrorResponse with 400 status and detailed validation error list
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {

        logger.error("Validation error: {}", ex.getMessage());

        List<ErrorResponse.ValidationError> validationErrors = new ArrayList<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            validationErrors.add(new ErrorResponse.ValidationError(
                    error.getField(),
                    error.getDefaultMessage()
            ));
        }

        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Validation Failed",
                "Input validation failed",
                request.getRequestURI()
        );
        errorResponse.setValidationErrors(validationErrors);

        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handles MethodArgumentTypeMismatchException thrown when a request parameter cannot be converted to the expected type.
     * For example, passing a string where an integer is expected.
     * Returns HTTP 400 Bad Request status with a descriptive message about the type mismatch.
     *
     * @param ex      The MethodArgumentTypeMismatchException that was thrown
     * @param request The HTTP request that caused the exception
     * @return ResponseEntity containing ErrorResponse with 400 status and type mismatch details
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatchException(
            MethodArgumentTypeMismatchException ex,
            HttpServletRequest request) {

        logger.error("Type mismatch: {}", ex.getMessage());

        String message = String.format("Parameter '%s' should be of type %s",
                ex.getName(),
                ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown");

        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Bad Request",
                message,
                request.getRequestURI()
        );

        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handles NoHandlerFoundException thrown when no handler is found for the requested endpoint.
     * This requires spring.mvc.throw-exception-if-no-handler-found=true in application properties.
     * Returns HTTP 404 Not Found status.
     *
     * @param ex      The NoHandlerFoundException that was thrown
     * @param request The HTTP request that caused the exception
     * @return ResponseEntity containing ErrorResponse with 404 status
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoHandlerFoundException(
            NoHandlerFoundException ex,
            HttpServletRequest request) {

        logger.error("No handler found: {}", ex.getMessage());

        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.NOT_FOUND.value(),
                "Not Found",
                "The requested endpoint does not exist",
                request.getRequestURI()
        );

        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
    }

    /**
     * Handles all uncaught exceptions that don't have a specific handler.
     * This is the fallback handler to ensure all exceptions are caught and logged.
     * Returns HTTP 500 Internal Server Error status with a generic message to avoid exposing internal details.
     *
     * @param ex      The Exception that was thrown
     * @param request The HTTP request that caused the exception
     * @return ResponseEntity containing ErrorResponse with 500 status and generic error message
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGlobalException(
            Exception ex,
            HttpServletRequest request) {

        logger.error("Unexpected error occurred", ex);

        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Internal Server Error",
                "An unexpected error occurred. Please try again later.",
                request.getRequestURI()
        );

        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}