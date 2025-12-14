package trademind.tm_server.base;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Standardized error response object returned by the API when errors occur.
 * Provides consistent error information across all endpoints including HTTP status,
 * error details, and optional field-level validation errors.
 *
 * <p>Used by {@link trader.beast.traderBeast.exceptions.GlobalExceptionHandler} to format all exceptions.
 *
 * <p>Fields with null values are excluded from JSON serialization via @JsonInclude.
 *
 * @see trader.beast.traderBeast.exceptions.GlobalExceptionHandler
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Standardized error response returned when an error occurs in the API")
public class ErrorResponse {

    @Schema(description = "Timestamp when the error occurred", example = "2024-01-15T10:30:45")
    private LocalDateTime timestamp;

    @Schema(description = "HTTP status code of the error", example = "404")
    private int status;

    @Schema(description = "HTTP status text or error category", example = "Not Found")
    private String error;

    @Schema(description = "Detailed error message explaining what went wrong", example = "Trade with id 123 not found")
    private String message;

    @Schema(description = "Request path that caused the error", example = "/api/trades/123")
    private String path;

    @Schema(description = "List of field-level validation errors (only present for validation failures)")
    private List<ValidationError> validationErrors;

    /**
     * Default constructor that automatically sets the timestamp to current time.
     */
    public ErrorResponse() {
        this.timestamp = LocalDateTime.now();
    }

    /**
     * Constructor for creating error responses with standard fields.
     * Timestamp is automatically set to current time.
     *
     * @param status  HTTP status code
     * @param error   HTTP status text or error category
     * @param message Detailed error message
     * @param path    Request path that caused the error
     */
    public ErrorResponse(int status, String error, String message, String path) {
        this();
        this.status = status;
        this.error = error;
        this.message = message;
        this.path = path;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public List<ValidationError> getValidationErrors() {
        return validationErrors;
    }

    public void setValidationErrors(List<ValidationError> validationErrors) {
        this.validationErrors = validationErrors;
    }

    /**
     * Represents a single field-level validation error.
     * Used to provide detailed information about which fields failed validation and why.
     */
    @Schema(description = "Field-level validation error details")
    public static class ValidationError {

        @Schema(description = "Name of the field that failed validation", example = "email")
        private String field;

        @Schema(description = "Validation error message for this field", example = "must be a well-formed email address")
        private String message;

        /**
         * Creates a validation error for a specific field.
         *
         * @param field   Name of the field that failed validation
         * @param message Validation error message
         */
        public ValidationError(String field, String message) {
            this.field = field;
            this.message = message;
        }

        public String getField() {
            return field;
        }

        public void setField(String field) {
            this.field = field;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }
}