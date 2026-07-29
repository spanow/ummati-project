package orga.takwa.ummati.repository;

import orga.takwa.ummati.entity.EventFavorite;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface EventFavoriteRepository extends JpaRepository<EventFavorite, UUID> {

    Optional<EventFavorite> findByUserIdAndEventId(UUID userId, UUID eventId);

    boolean existsByUserIdAndEventId(UUID userId, UUID eventId);

    void deleteByUserIdAndEventId(UUID userId, UUID eventId);

    Page<EventFavorite> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    long countByEventId(UUID eventId);

    /** Marquage en lot des favoris d'une liste de missions — évite un appel par carte. */
    @Query("SELECT f.event.id FROM EventFavorite f WHERE f.user.id = :userId AND f.event.id IN :eventIds")
    Set<UUID> findFavoritedEventIds(@Param("userId") UUID userId, @Param("eventIds") Collection<UUID> eventIds);

    /**
     * Favoris dont la clôture des inscriptions approche et pour lesquels le bénévole
     * n'a pas encore de place — cible de la relance avant fermeture.
     */
    @Query("""
            SELECT f FROM EventFavorite f
            JOIN FETCH f.event e
            JOIN FETCH f.user u
            WHERE e.status = 'PUBLISHED'
              AND e.startDate BETWEEN :from AND :to
              AND NOT EXISTS (
                SELECT 1 FROM EventSignup s
                WHERE s.event.id = e.id AND s.user.id = u.id
                  AND s.status IN ('REGISTERED', 'WAITLISTED', 'ATTENDED')
              )
            """)
    List<EventFavorite> findClosingSoonWithoutSignup(@Param("from") LocalDateTime from,
                                                     @Param("to") LocalDateTime to);
}
