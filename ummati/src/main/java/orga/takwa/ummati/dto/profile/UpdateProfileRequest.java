package orga.takwa.ummati.dto.profile;

import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record UpdateProfileRequest(
        @Size(max = 100) String firstName,
        @Size(max = 100) String lastName,
        @Size(max = 20) String phone,
        @Past LocalDate dateOfBirth,
        @Size(max = 2000) String bio,
        AddressDto address,
        List<UUID> skillIds,
        List<String> newSkills
) {
    public record AddressDto(String street, String city, String zip, String country) {}
}

