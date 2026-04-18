package orga.takwa.ummati.dto.profile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record ProfileResponse(
        UUID id, String email, String firstName, String lastName,
        String phone, LocalDate dateOfBirth, String photoUrl, String bio,
        AddressDto address, List<SkillDto> skills, StatsDto stats,
        boolean onboardingDone, boolean emailVerified, LocalDateTime createdAt
) {
    public record AddressDto(String street, String city, String zip, String country) {}
    public record SkillDto(UUID id, String name, String category) {}
    public record StatsDto(long organizationCount, long eventsAttended, double volunteerHours) {}
}

