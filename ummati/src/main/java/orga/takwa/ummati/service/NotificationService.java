package orga.takwa.ummati.service;

import orga.takwa.ummati.dto.notification.NotificationResponse;
import orga.takwa.ummati.entity.Notification;
import orga.takwa.ummati.entity.User;
import orga.takwa.ummati.entity.enums.NotificationType;
import orga.takwa.ummati.exception.ForbiddenException;
import orga.takwa.ummati.exception.ResourceNotFoundException;
import orga.takwa.ummati.repository.NotificationRepository;
import orga.takwa.ummati.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    public NotificationService(NotificationRepository notificationRepository,
                               UserRepository userRepository, EmailService emailService) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
        this.emailService = emailService;
    }

    // Save notification to DB only (no email) — used internally by domain services
    public void saveNotification(User user, NotificationType type, String title, String message, String link) {
        Notification notif = new Notification();
        notif.setUser(user);
        notif.setType(type);
        notif.setTitle(title);
        notif.setMessage(message);
        notif.setLink(link);
        notificationRepository.save(notif);
    }

    // T-100: Create notification in-app + async email
    public void notify(User user, NotificationType type, String title, String message, String link) {
        Notification notif = new Notification();
        notif.setUser(user);
        notif.setType(type);
        notif.setTitle(title);
        notif.setMessage(message);
        notif.setLink(link);
        notificationRepository.save(notif);

        // Async email for non-GENERAL types
        if (type != NotificationType.GENERAL) {
            emailService.sendNotificationEmail(user.getEmail(), user.getFirstName(), title, message, link);
        }
    }

    // T-101: List notifications
    @Transactional(readOnly = true)
    public Page<NotificationResponse> listNotifications(UUID userId, Pageable pageable) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(this::toResponse);
    }

    // T-101: Unread count
    @Transactional(readOnly = true)
    public long getUnreadCount(UUID userId) {
        return notificationRepository.countByUserIdAndReadFalse(userId);
    }

    // T-101: Mark one as read
    @Transactional
    public void markAsRead(UUID userId, UUID notificationId) {
        Notification notif = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification non trouvée"));
        if (!notif.getUser().getId().equals(userId)) {
            throw new ForbiddenException("Cette notification ne vous appartient pas");
        }
        notif.setRead(true);
        notificationRepository.save(notif);
    }

    // T-101: Mark all as read
    @Transactional
    public void markAllAsRead(UUID userId) {
        notificationRepository.markAllAsRead(userId);
    }

    private NotificationResponse toResponse(Notification n) {
        return new NotificationResponse(n.getId(), n.getType().name(), n.getTitle(),
                n.getMessage(), n.isRead(), n.getLink(), n.getCreatedAt());
    }
}

