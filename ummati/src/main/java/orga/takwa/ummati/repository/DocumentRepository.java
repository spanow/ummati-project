package orga.takwa.ummati.repository;

import orga.takwa.ummati.entity.Document;
import orga.takwa.ummati.entity.enums.DocumentOwnerType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DocumentRepository extends JpaRepository<Document, UUID> {
    List<Document> findByOwnerTypeAndOwnerId(DocumentOwnerType ownerType, UUID ownerId);
}

