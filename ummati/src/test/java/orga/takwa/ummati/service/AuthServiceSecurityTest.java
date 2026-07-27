package orga.takwa.ummati.service;

import orga.takwa.ummati.config.security.JwtTokenProvider;
import orga.takwa.ummati.dto.auth.LoginRequest;
import orga.takwa.ummati.dto.auth.ResetPasswordRequest;
import orga.takwa.ummati.entity.User;
import orga.takwa.ummati.entity.VerificationToken;
import orga.takwa.ummati.entity.enums.TokenType;
import orga.takwa.ummati.exception.BusinessRuleException;
import orga.takwa.ummati.exception.ForbiddenException;
import orga.takwa.ummati.repository.UserRepository;
import orga.takwa.ummati.repository.VerificationTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceSecurityTest {

    @Mock private UserRepository userRepository;
    @Mock private VerificationTokenRepository tokenRepository;
    @Mock private NotificationService notificationService;
    @Mock private AuditService auditService;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtTokenProvider jwtTokenProvider;
    @Mock private EmailService emailService;

    @InjectMocks private AuthService authService;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("amina@test.com");
        user.setFirstName("Amina");
        user.setPasswordHash("$2a$12$hash");
        user.setEnabled(true);
        user.setEmailVerified(true);
    }

    // --- Énumération de comptes ---

    @Test
    void login_shouldNotRevealThatAnUnverifiedAccountExists_whenPasswordIsWrong() {
        user.setEmailVerified(false);
        when(userRepository.findByEmail("amina@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        // Le message doit être le même que pour un email inconnu : sinon on apprend que
        // l'adresse est inscrite sans connaître le mot de passe.
        assertThatThrownBy(() -> authService.login(new LoginRequest("amina@test.com", "faux"), "1.2.3.4"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Email ou mot de passe incorrect");
    }

    @Test
    void login_shouldNotRevealThatADisabledAccountExists_whenPasswordIsWrong() {
        user.setEnabled(false);
        when(userRepository.findByEmail("amina@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest("amina@test.com", "faux"), "1.2.3.4"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Email ou mot de passe incorrect");
    }

    @Test
    void login_shouldGiveTheSameMessage_whenEmailIsUnknown() {
        when(userRepository.findByEmail("inconnu@test.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginRequest("inconnu@test.com", "faux"), "1.2.3.4"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Email ou mot de passe incorrect");
    }

    @Test
    void login_shouldStillExplainTheAccountState_onceThePasswordIsProven() {
        user.setEmailVerified(false);
        when(userRepository.findByEmail("amina@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);

        assertThatThrownBy(() -> authService.login(new LoginRequest("amina@test.com", "bon"), "1.2.3.4"))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("Email non vérifié");
    }

    // --- Blocage temporaire ---

    @Test
    void resetPassword_shouldClearTheTemporaryLockout() {
        // Le blocage vient de tentatives échouées ; la réinitialisation par email prouve
        // l'identité, le laisser en place enfermerait l'utilisateur 30 minutes de plus.
        user.setFailedAttempts(5);
        user.setLockedUntil(LocalDateTime.now().plusMinutes(25));

        VerificationToken vt = new VerificationToken();
        vt.setUser(user);
        vt.setToken("tok");
        vt.setType(TokenType.PASSWORD_RESET);
        vt.setExpiresAt(LocalDateTime.now().plusHours(1));

        when(tokenRepository.findByToken("tok")).thenReturn(Optional.of(vt));
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$12$newhash");
        lenient().when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        authService.resetPassword(new ResetPasswordRequest("tok", "NouveauMdp123!"));

        assertThat(user.getFailedAttempts()).isZero();
        assertThat(user.getLockedUntil()).isNull();
        assertThat(user.getPasswordHash()).isEqualTo("$2a$12$newhash");
    }
}
