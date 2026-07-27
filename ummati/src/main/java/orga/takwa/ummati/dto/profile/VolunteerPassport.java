package orga.takwa.ummati.dto.profile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Passeport bénévole : la vitrine d'un bénévole (missions, heures, causes, compétences).
 *
 * <p>Deux vues du même enregistrement :
 * <ul>
 *   <li><strong>publique</strong> — {@code lastName} réduit à l'initiale, pas d'email ni de
 *       téléphone, ville seule : accessible uniquement si le bénévole a activé l'option ;</li>
 *   <li><strong>privée</strong> — le bénévole consultant son propre passeport, nom complet.</li>
 * </ul>
 */
public record VolunteerPassport(
        UUID userId,
        String firstName,
        /** Nom complet en vue privée, initiale suivie d'un point en vue publique. */
        String lastName,
        String photoUrl,
        String bio,
        String city,
        LocalDateTime memberSince,
        int missionsCompleted,
        double hoursTotal,
        int organizationCount,
        List<SkillDto> skills,
        List<CauseDto> causes,
        List<MissionDto> recentMissions,
        /** Vrai si le passeport est visible publiquement. */
        boolean profilePublic
) {
    public record SkillDto(UUID id, String name, String category) {}

    /** Domaine d'engagement, avec le nombre de missions accomplies dans ce domaine. */
    public record CauseDto(String domain, int missionCount) {}

    public record MissionDto(
            UUID eventId,
            String title,
            String organizationName,
            String organizationSlug,
            LocalDateTime date
    ) {}
}
