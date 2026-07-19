package orga.takwa.ummati.service;

import orga.takwa.ummati.dto.stats.PublicStatsResponse;
import orga.takwa.ummati.entity.enums.OrganizationStatus;
import orga.takwa.ummati.entity.enums.SignupStatus;
import orga.takwa.ummati.repository.EventRepository;
import orga.takwa.ummati.repository.EventSignupRepository;
import orga.takwa.ummati.repository.OrganizationRepository;
import orga.takwa.ummati.repository.UserRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PublicStatsService {

    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final EventRepository eventRepository;
    private final EventSignupRepository eventSignupRepository;

    public PublicStatsService(UserRepository userRepository,
                              OrganizationRepository organizationRepository,
                              EventRepository eventRepository,
                              EventSignupRepository eventSignupRepository) {
        this.userRepository = userRepository;
        this.organizationRepository = organizationRepository;
        this.eventRepository = eventRepository;
        this.eventSignupRepository = eventSignupRepository;
    }

    @Transactional(readOnly = true)
    @Cacheable("public-stats")
    public PublicStatsResponse getStats() {
        return new PublicStatsResponse(
                userRepository.count(),
                organizationRepository.countByStatus(OrganizationStatus.ACTIVE),
                eventRepository.count(),
                eventSignupRepository.countByStatus(SignupStatus.ATTENDED));
    }
}
