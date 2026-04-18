package orga.takwa.ummati.dto.profile;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record OnboardingRequest(
        @Size(max = 2000) String bio,
        @NotBlank String city,
        List<UUID> skillIds,
        List<String> preferredDomains
) {}

