package orga.takwa.ummati.repository;

import orga.takwa.ummati.entity.EventSignup;
import orga.takwa.ummati.entity.enums.SignupStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EventSignupRepository extends JpaRepository<EventSignup, UUID> {

    // --- Niveau occurrence (créneau réservé) : unicité, capacité, waitlist FIFO, présence ---
    Optional<EventSignup> findByOccurrenceIdAndUserId(UUID occurrenceId, UUID userId);
    long countByOccurrenceIdAndStatus(UUID occurrenceId, SignupStatus status);
    Page<EventSignup> findByOccurrenceId(UUID occurrenceId, Pageable pageable);
    List<EventSignup> findByOccurrenceIdAndStatusIn(UUID occurrenceId, Collection<SignupStatus> statuses);
    Optional<EventSignup> findFirstByOccurrenceIdAndStatusOrderByRegisteredAtAsc(UUID occurrenceId, SignupStatus status);

    // --- Niveau série (événement) : agrégats et listes tous créneaux confondus ---
    // NB : un bénévole peut avoir plusieurs inscriptions par événement (une par occurrence) → List.
    List<EventSignup> findByEventIdAndUserId(UUID eventId, UUID userId);
    boolean existsByEventIdAndUserId(UUID eventId, UUID userId);
    boolean existsByEventIdAndUserIdAndStatus(UUID eventId, UUID userId, SignupStatus status);
    boolean existsByEventIdAndUserIdAndStatusIn(UUID eventId, UUID userId, Collection<SignupStatus> statuses);
    long countByEventIdAndStatus(UUID eventId, SignupStatus status);
    Page<EventSignup> findByEventId(UUID eventId, Pageable pageable);
    List<EventSignup> findByEventIdAndStatusIn(UUID eventId, Collection<SignupStatus> statuses);

    // --- Niveau utilisateur : stats / mes inscriptions ---
    Page<EventSignup> findByUserId(UUID userId, Pageable pageable);
    long countByUserIdAndStatus(UUID userId, SignupStatus status);
    long countByUserId(UUID userId);
    long countByStatus(SignupStatus status);

    // Somme des heures certifiées (présences validées) d'un bénévole — null si aucune.
    @Query("SELECT SUM(s.hoursValidated) FROM EventSignup s WHERE s.user.id = :userId AND s.status = 'ATTENDED'")
    BigDecimal sumValidatedHoursByUserId(@Param("userId") UUID userId);

    // Présences validées d'un bénévole, avec créneau + événement + ONG chargés — pour l'attestation.
    @Query("""
            SELECT s FROM EventSignup s
            JOIN FETCH s.occurrence o
            JOIN FETCH s.event e
            JOIN FETCH e.organization
            WHERE s.user.id = :userId AND s.status = 'ATTENDED' AND s.hoursValidated IS NOT NULL
            ORDER BY o.startDate ASC
            """)
    List<EventSignup> findAttendedWithHoursByUserId(@Param("userId") UUID userId);

    // Rappels J-1 : inscriptions REGISTERED dont le CRÉNEAU (occurrence) démarre demain.
    @Query("""
            SELECT s FROM EventSignup s
            WHERE s.status = 'REGISTERED'
              AND s.occurrence.status = 'PUBLISHED'
              AND s.occurrence.startDate >= :from
              AND s.occurrence.startDate < :to
            """)
    List<EventSignup> findRegisteredSignupsForOccurrencesBetween(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);
}
