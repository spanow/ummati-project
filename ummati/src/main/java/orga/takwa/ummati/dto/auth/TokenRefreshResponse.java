package orga.takwa.ummati.dto.auth;

public record TokenRefreshResponse(
        String accessToken,
        long expiresIn,
        String tokenType
) {}

