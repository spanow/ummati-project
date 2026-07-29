package orga.takwa.ummati.service;

import orga.takwa.ummati.dto.event.EventSummary;
import orga.takwa.ummati.entity.Event;
import orga.takwa.ummati.entity.EventFavorite;
import orga.takwa.ummati.entity.Organization;
import orga.takwa.ummati.entity.OrganizationFollow;
import orga.takwa.ummati.entity.User;
import orga.takwa.ummati.dto.organization.OrganizationSummary;
import orga.takwa.ummati.exception.ResourceNotFoundException;
import orga.takwa.ummati.repository.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Set;
import java.util.UUID;

/**
 * Missions mises de côté et associations suivies.
 *
 * <p>Les deux gestes servent la même fin : donner une raison de revenir. Mettre en
 * favori n'engage à rien mais signale une intention, ce qui permet de relancer avant
 * la clôture ; suivre une ONG transforme sa prochaine publication en notification.
 */
@Service
public class FavoriteService {

    private final EventFavoriteRepository favoriteRepository;
    private final OrganizationFollowRepository followRepository;
    private final EventRepository eventRepository;
    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    private final EventService eventService;
    private final OrganizationService organizationService;

    public FavoriteService(EventFavoriteRepository favoriteRepository,
                           OrganizationFollowRepository followRepository,
                           EventRepository eventRepository,
                           OrganizationRepository organizationRepository,
                           UserRepository userRepository,
                           EventService eventService,
                           OrganizationService organizationService) {
        this.favoriteRepository = favoriteRepository;
        this.followRepository = followRepository;
        this.eventRepository = eventRepository;
        this.organizationRepository = organizationRepository;
        this.userRepository = userRepository;
        this.eventService = eventService;
        this.organizationService = organizationService;
    }

    // --- Missions mises de côté ---

    /** Ajoute la mission aux favoris. Idempotent : ré-appeler ne crée pas de doublon. */
    @Transactional
    public void addFavorite(UUID userId, UUID eventId) {
        if (favoriteRepository.existsByUserIdAndEventId(userId, eventId)) {
            return;
        }
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Mission non trouvée"));
        User user = userRepository.getReferenceById(userId);

        EventFavorite favorite = new EventFavorite();
        favorite.setUser(user);
        favorite.setEvent(event);
        favoriteRepository.save(favorite);
    }

    /** Retire la mission des favoris. Idempotent également. */
    @Transactional
    public void removeFavorite(UUID userId, UUID eventId) {
        favoriteRepository.deleteByUserIdAndEventId(userId, eventId);
    }

    @Transactional(readOnly = true)
    public boolean isFavorite(UUID userId, UUID eventId) {
        return favoriteRepository.existsByUserIdAndEventId(userId, eventId);
    }

    /**
     * Identifiants des missions mises en favori parmi celles fournies.
     *
     * <p>Une seule requête pour toute une page de résultats : marquer les cœurs
     * carte par carte ferait dix appels pour dix missions.
     */
    @Transactional(readOnly = true)
    public Set<UUID> favoritedAmong(UUID userId, Collection<UUID> eventIds) {
        if (userId == null || eventIds == null || eventIds.isEmpty()) {
            return Set.of();
        }
        return favoriteRepository.findFavoritedEventIds(userId, eventIds);
    }

    @Transactional(readOnly = true)
    public Page<EventSummary> listFavorites(UUID userId, Pageable pageable) {
        return favoriteRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(f -> eventService.toSummary(f.getEvent()));
    }

    // --- Associations suivies ---

    @Transactional
    public void followOrganization(UUID userId, UUID orgId) {
        if (followRepository.existsByUserIdAndOrganizationId(userId, orgId)) {
            return;
        }
        Organization org = organizationRepository.findById(orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Organisation non trouvée"));
        User user = userRepository.getReferenceById(userId);

        OrganizationFollow follow = new OrganizationFollow();
        follow.setUser(user);
        follow.setOrganization(org);
        followRepository.save(follow);
    }

    @Transactional
    public void unfollowOrganization(UUID userId, UUID orgId) {
        followRepository.deleteByUserIdAndOrganizationId(userId, orgId);
    }

    @Transactional(readOnly = true)
    public boolean isFollowing(UUID userId, UUID orgId) {
        return followRepository.existsByUserIdAndOrganizationId(userId, orgId);
    }

    @Transactional(readOnly = true)
    public long followerCount(UUID orgId) {
        return followRepository.countByOrganizationId(orgId);
    }

    @Transactional(readOnly = true)
    public Page<OrganizationSummary> listFollowed(UUID userId, Pageable pageable) {
        return followRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(f -> organizationService.toSummary(f.getOrganization()));
    }
}
