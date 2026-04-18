package orga.takwa.ummati.dto.auth;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        long expiresIn,
        String tokenType,
        UserSummary user
) {
    public record UserSummary(
            String id,
            String email,
            String firstName,
            String lastName,
            String role,
            boolean onboardingDone
    ) {}
}

