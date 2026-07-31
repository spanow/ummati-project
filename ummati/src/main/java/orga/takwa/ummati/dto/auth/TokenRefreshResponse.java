package orga.takwa.ummati.dto.auth;

/**
 * @param refreshToken jeton de remplacement, renseigné uniquement pour les sessions
 *                     natives à rotation. {@code null} pour le refresh JWT sans état
 *                     du web, qui reste valable jusqu'à son terme — le client
 *                     historique ignore simplement ce champ.
 */
public record TokenRefreshResponse(
        String accessToken,
        long expiresIn,
        String tokenType,
        String refreshToken
) {

    /** Réponse du régime web historique, sans rotation. */
    public static TokenRefreshResponse stateless(String accessToken, long expiresIn) {
        return new TokenRefreshResponse(accessToken, expiresIn, "Bearer", null);
    }
}

