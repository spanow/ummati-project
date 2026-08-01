package orga.takwa.ummati.service;

import orga.takwa.ummati.dto.auth.DeviceContext;
import orga.takwa.ummati.entity.RefreshToken;
import orga.takwa.ummati.entity.User;
import orga.takwa.ummati.entity.enums.DevicePlatform;
import orga.takwa.ummati.exception.ForbiddenException;
import orga.takwa.ummati.repository.RefreshTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Sessions longues des apps installées.
 *
 * <p>L'enjeu couvert ici n'est pas la génération d'un jeton mais ce qui rend une
 * durée de 90 jours acceptable : la rotation, la détection de rejeu, et le fait que
 * le mécanisme se déclare étranger aux jetons web pour ne pas les casser.
 */
@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock private RefreshTokenRepository repository;

    private RefreshTokenService service;
    private User user;

    private static final DeviceContext IPHONE =
            new DeviceContext(DevicePlatform.IOS, "device-1", "iPhone de Manil", "1.0.0");

    @BeforeEach
    void setUp() {
        service = new RefreshTokenService(repository, 90);
        user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("benevole@ummati.org");
    }

    /** Reproduit ce que le repository ferait : rend la ligne telle qu'elle a été écrite. */
    private RefreshToken captureSaved() {
        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(repository, atLeastOnce()).save(captor.capture());
        return captor.getValue();
    }

    @Test
    @DisplayName("Le jeton émis n'est jamais stocké en clair")
    void issueStoresOnlyTheHash() {
        var issued = service.issue(user, IPHONE);

        RefreshToken saved = captureSaved();
        assertThat(saved.getTokenHash())
                .isNotEqualTo(issued.rawToken())
                .hasSize(64)
                .isEqualTo(RefreshTokenService.hash(issued.rawToken()));
    }

    @Test
    @DisplayName("Le contexte de l'appareil est conservé pour permettre la révocation ciblée")
    void issueKeepsDeviceContext() {
        service.issue(user, IPHONE);

        RefreshToken saved = captureSaved();
        assertThat(saved.getPlatform()).isEqualTo(DevicePlatform.IOS);
        assertThat(saved.getDeviceId()).isEqualTo("device-1");
        assertThat(saved.getDeviceName()).isEqualTo("iPhone de Manil");
        assertThat(saved.getExpiresAt()).isAfter(LocalDateTime.now().plusDays(89));
    }

    @Test
    @DisplayName("Deux émissions produisent des jetons distincts")
    void issuedTokensAreUnique() {
        String first = service.issue(user, IPHONE).rawToken();
        String second = service.issue(user, IPHONE).rawToken();

        assertThat(first).isNotEqualTo(second);
    }

    @Test
    @DisplayName("Un jeton inconnu n'est pas rejeté : le refresh JWT web doit pouvoir prendre le relais")
    void unknownTokenYieldsEmptyRatherThanError() {
        when(repository.findByTokenHash(any())).thenReturn(Optional.empty());

        assertThat(service.rotate("un-jwt-web-quelconque")).isEmpty();
    }

    @Test
    @DisplayName("La rotation consomme l'ancien jeton et en émet un nouveau dans la même famille")
    void rotateConsumesAndReissues() {
        RefreshToken existing = usableToken();
        when(repository.findByTokenHash(any())).thenReturn(Optional.of(existing));

        var result = service.rotate("peu-importe");

        assertThat(result).isPresent();
        assertThat(result.get().user()).isEqualTo(user);
        assertThat(existing.getUsedAt()).isNotNull();

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(repository, times(2)).save(captor.capture());
        RefreshToken reissued = captor.getAllValues().get(1);
        assertThat(reissued.getFamilyId()).isEqualTo(existing.getFamilyId());
        assertThat(reissued.getTokenHash()).isEqualTo(RefreshTokenService.hash(result.get().rawToken()));
    }

    @Test
    @DisplayName("Rejouer un jeton déjà consommé révoque toute la lignée")
    void replayRevokesTheWholeFamily() {
        RefreshToken used = usableToken();
        used.setUsedAt(LocalDateTime.now().minusMinutes(5));
        when(repository.findByTokenHash(any())).thenReturn(Optional.of(used));

        assertThatThrownBy(() -> service.rotate("jeton-vole"))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("révoquée");

        verify(repository).revokeFamily(eq(used.getFamilyId()), any());
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("Un jeton expiré est refusé sans être traité comme un vol")
    void expiredTokenIsRefused() {
        RefreshToken expired = usableToken();
        expired.setExpiresAt(LocalDateTime.now().minusDays(1));
        when(repository.findByTokenHash(any())).thenReturn(Optional.of(expired));

        assertThatThrownBy(() -> service.rotate("jeton-perime"))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("expirée");

        verify(repository, never()).revokeFamily(any(), any());
    }

    @Test
    @DisplayName("Un jeton révoqué reste refusé")
    void revokedTokenIsRefused() {
        RefreshToken revoked = usableToken();
        revoked.setRevokedAt(LocalDateTime.now().minusHours(1));
        when(repository.findByTokenHash(any())).thenReturn(Optional.of(revoked));

        assertThatThrownBy(() -> service.rotate("jeton-revoque"))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    @DisplayName("La déconnexion coupe la lignée entière, pas seulement le dernier maillon")
    void revokeCutsTheFamily() {
        RefreshToken existing = usableToken();
        when(repository.findByTokenHash(any())).thenReturn(Optional.of(existing));

        service.revoke("jeton-courant");

        verify(repository).revokeFamily(eq(existing.getFamilyId()), any());
    }

    @Test
    @DisplayName("Déconnecter avec un jeton inconnu ou vide ne lève pas d'erreur")
    void revokeIsSilentOnUnknownToken() {
        when(repository.findByTokenHash(any())).thenReturn(Optional.empty());

        assertThatCode(() -> service.revoke("inconnu")).doesNotThrowAnyException();
        assertThatCode(() -> service.revoke(null)).doesNotThrowAnyException();
        assertThatCode(() -> service.revoke("  ")).doesNotThrowAnyException();
    }

    private RefreshToken usableToken() {
        RefreshToken token = new RefreshToken();
        token.setId(UUID.randomUUID());
        token.setUser(user);
        token.setFamilyId(UUID.randomUUID());
        token.setPlatform(DevicePlatform.IOS);
        token.setDeviceId("device-1");
        token.setTokenHash(RefreshTokenService.hash("peu-importe"));
        token.setExpiresAt(LocalDateTime.now().plusDays(30));
        return token;
    }
}
