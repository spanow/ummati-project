package orga.takwa.ummati.service;

import orga.takwa.ummati.entity.User;
import orga.takwa.ummati.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * T-141 — AccountPurgeJob
 * Purge des comptes anonymisés (RGPD) de plus de 30 jours — chaque jour à 04:00 UTC.
 */
@Component
public class AccountPurgeJob {

    private static final Logger log = LoggerFactory.getLogger(AccountPurgeJob.class);

    private final UserRepository userRepository;

    public AccountPurgeJob(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Scheduled(cron = "0 0 4 * * *", zone = "UTC")
    @Transactional
    public void purgeAnonymizedAccounts() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(30);
        List<User> toDelete = userRepository.findAnonymizedAccountsBefore(cutoff);

        if (toDelete.isEmpty()) {
            log.debug("AccountPurgeJob: aucun compte anonymisé à purger.");
            return;
        }

        userRepository.deleteAll(toDelete);
        log.info("AccountPurgeJob: {} compte(s) anonymisé(s) purgé(s).", toDelete.size());
    }
}

