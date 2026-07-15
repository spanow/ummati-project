package orga.takwa.ummati.service;

import orga.takwa.ummati.entity.AuditLog;
import orga.takwa.ummati.repository.AuditLogRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    public void log(UUID actorId, String action, String entityType, UUID entityId) {
        log(actorId, action, entityType, entityId, null);
    }

    public void log(UUID actorId, String action, String entityType, UUID entityId, String ipAddress) {
        AuditLog entry = new AuditLog();
        entry.setActorId(actorId);
        entry.setAction(action);
        entry.setEntityType(entityType);
        entry.setEntityId(entityId);
        entry.setIpAddress(ipAddress);
        auditLogRepository.save(entry);
    }
}
