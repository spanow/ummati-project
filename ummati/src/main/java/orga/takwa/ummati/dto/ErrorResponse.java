package orga.takwa.ummati.dto;

import java.time.LocalDateTime;
import java.util.List;

public record ErrorResponse(
        int status,
        String error,
        String message,
        LocalDateTime timestamp,
        List<FieldError> details
) {
    public ErrorResponse(int status, String error, String message) {
        this(status, error, message, LocalDateTime.now(), null);
    }

    public record FieldError(String field, String message) {}
}

