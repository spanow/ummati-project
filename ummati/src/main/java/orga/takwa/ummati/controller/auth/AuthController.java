package orga.takwa.ummati.controller.auth;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import orga.takwa.ummati.dto.ApiResponse;
import orga.takwa.ummati.dto.auth.*;
import orga.takwa.ummati.service.AuthService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentification", description = "Inscription, connexion, tokens, reset mot de passe")
public class AuthController {

    private final AuthService authService;
    @org.springframework.beans.factory.annotation.Value("${app.base-url}")
    private String appBaseUrl;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<RegisterResponse>> register(@Valid @RequestBody RegisterRequest request) {
        RegisterResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Compte créé. Vérifiez votre email.", response));
    }

    /** Clic depuis l'email → GET → redirige vers le front */
    @GetMapping("/confirm-email")
    public ResponseEntity<Void> confirmEmailGet(@RequestParam String token) {
        try {
            authService.confirmEmail(token);
            HttpHeaders headers = new HttpHeaders();
            headers.add("Location", appBaseUrl + "/login?emailConfirmed=true");
            return ResponseEntity.status(HttpStatus.FOUND).headers(headers).build();
        } catch (RuntimeException e) {
            HttpHeaders headers = new HttpHeaders();
            headers.add("Location", appBaseUrl + "/login?emailError=true");
            return ResponseEntity.status(HttpStatus.FOUND).headers(headers).build();
        }
    }

    /** Appel API direct → POST (Swagger / tests) */
    @PostMapping("/confirm-email")
    public ResponseEntity<ApiResponse<Void>> confirmEmail(@RequestParam String token) {
        authService.confirmEmail(token);
        return ResponseEntity.ok(ApiResponse.ok("Email confirmé avec succès", null));
    }

    @PostMapping("/resend-confirmation")
    public ResponseEntity<ApiResponse<Void>> resendConfirmation(@RequestBody ResendConfirmationRequest request) {
        authService.resendConfirmation(request.email());
        return ResponseEntity.ok(ApiResponse.ok("Si ce compte existe, un email a été envoyé", null));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request,
                                                           HttpServletRequest httpRequest) {
        AuthResponse response = authService.login(request, httpRequest.getRemoteAddr());
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<TokenRefreshResponse>> refresh(
            @RequestHeader("Authorization") String authHeader) {
        String refreshToken = authHeader.replace("Bearer ", "");
        TokenRefreshResponse response = authService.refreshToken(refreshToken);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request.email());
        return ResponseEntity.ok(ApiResponse.ok("Si ce compte existe, un email a été envoyé", null));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok(ApiResponse.ok("Mot de passe réinitialisé", null));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        // Client-side token removal; server-side refresh token invalidation can be added later
        return ResponseEntity.noContent().build();
    }
}
