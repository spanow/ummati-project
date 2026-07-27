package orga.takwa.ummati.config.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * T-142 — Sécurité : Rate limiting simple par IP sur les endpoints sensibles.
 * Max 20 requêtes/minute sur /api/v1/auth/**
 */
@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    private static final int MAX_REQUESTS = 20;
    private static final long WINDOW_MS = 60_000L; // 1 minute

    /**
     * Plafond du nombre de clients suivis simultanément. Sans plafond, la table de
     * compteurs grossit indéfiniment : il suffisait d'envoyer des requêtes avec une IP
     * différente à chaque fois pour saturer la mémoire du serveur.
     */
    private static final int MAX_TRACKED_CLIENTS = 50_000;

    private final Map<String, RequestCount> counters = new ConcurrentHashMap<>();

    /**
     * N'activer que derrière un reverse proxy de confiance qui réécrit lui-même
     * l'en-tête. Sinon un client peut envoyer un X-Forwarded-For différent à chaque
     * requête et se voir attribuer un compteur neuf à chaque fois — le quota ne
     * s'applique alors plus du tout.
     */
    @Value("${app.security.trust-forwarded-for:false}")
    private boolean trustForwardedFor;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/v1/auth/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        long now = Instant.now().toEpochMilli();
        String ip = getClientIp(request);

        RequestCount count = counters.compute(ip, (key, current) -> {
            if (current == null || now - current.windowStart > WINDOW_MS) {
                return new RequestCount(now);
            }
            current.requests++;
            return current;
        });

        // Purge des fenêtres expirées, amortie sur les requêtes : le nettoyage n'a lieu
        // que lorsque la table dépasse le plafond, pas à chaque appel.
        if (counters.size() > MAX_TRACKED_CLIENTS) {
            evictExpired(now);
        }

        if (count.requests > MAX_REQUESTS) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write(
                    "{\"success\":false,\"message\":\"Trop de requêtes. Réessayez dans une minute.\",\"data\":null}");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private void evictExpired(long now) {
        Iterator<Map.Entry<String, RequestCount>> it = counters.entrySet().iterator();
        while (it.hasNext()) {
            if (now - it.next().getValue().windowStart > WINDOW_MS) {
                it.remove();
            }
        }
    }

    private String getClientIp(HttpServletRequest request) {
        if (trustForwardedFor) {
            String forwarded = request.getHeader("X-Forwarded-For");
            if (forwarded != null && !forwarded.isBlank()) {
                return forwarded.split(",")[0].trim();
            }
        }
        return request.getRemoteAddr();
    }

    private static class RequestCount {
        final long windowStart;
        /** Toujours lu et écrit sous le verrou de segment de {@code compute}. */
        int requests;

        RequestCount(long windowStart) {
            this.windowStart = windowStart;
            this.requests = 1;
        }
    }
}
