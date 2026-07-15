package orga.takwa.ummati.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ResendConfirmationRequest(@NotBlank @Email String email) {}
