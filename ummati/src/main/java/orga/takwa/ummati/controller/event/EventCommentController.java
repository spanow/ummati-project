package orga.takwa.ummati.controller.event;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import orga.takwa.ummati.config.security.CurrentUser;
import orga.takwa.ummati.dto.ApiResponse;
import orga.takwa.ummati.dto.PageResponse;
import orga.takwa.ummati.dto.event.CommentResponse;
import orga.takwa.ummati.dto.event.CreateCommentRequest;
import orga.takwa.ummati.service.EventCommentService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/events/{eventId}/comments")
@Tag(name = "Commentaires", description = "Commentaires participants sur les événements")
public class EventCommentController {

    private final EventCommentService commentService;

    public EventCommentController(EventCommentService commentService) {
        this.commentService = commentService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<CommentResponse>>> list(
            @PathVariable UUID eventId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.ok(PageResponse.from(
                commentService.list(eventId, PageRequest.of(page, size, Sort.by("createdAt").ascending())))));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CommentResponse>> create(
            @CurrentUser UUID userId, @PathVariable UUID eventId,
            @Valid @RequestBody CreateCommentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(commentService.create(userId, eventId, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@CurrentUser UUID userId,
                                       @PathVariable UUID eventId, @PathVariable UUID id) {
        commentService.delete(userId, id);
        return ResponseEntity.noContent().build();
    }
}
