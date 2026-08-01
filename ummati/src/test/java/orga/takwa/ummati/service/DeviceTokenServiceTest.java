package orga.takwa.ummati.service;

import orga.takwa.ummati.dto.push.RegisterDeviceRequest;
import orga.takwa.ummati.entity.DeviceToken;
import orga.takwa.ummati.entity.User;
import orga.takwa.ummati.entity.enums.DevicePlatform;
import orga.takwa.ummati.repository.DeviceTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Registre des appareils joignables par notification native.
 *
 * <p>Le cas qui compte est celui du téléphone partagé ou revendu : un jeton FCM
 * appartient à une installation, pas à un compte.
 */
@ExtendWith(MockitoExtension.class)
class DeviceTokenServiceTest {

    @Mock private DeviceTokenRepository repository;

    private DeviceTokenService service;
    private User alice;

    private static final String FCM_TOKEN = "fcm-token-abc123";

    @BeforeEach
    void setUp() {
        service = new DeviceTokenService(repository);
        alice = userWithId();
    }

    @Test
    @DisplayName("Un nouvel appareil est enregistré avec son contexte")
    void registersNewDevice() {
        when(repository.findByToken(FCM_TOKEN)).thenReturn(Optional.empty());

        service.register(alice, new RegisterDeviceRequest(
                FCM_TOKEN, DevicePlatform.ANDROID, "device-1", "Pixel 8", "1.0.0"));

        DeviceToken saved = captureSaved();
        assertThat(saved.getUser()).isEqualTo(alice);
        assertThat(saved.getToken()).isEqualTo(FCM_TOKEN);
        assertThat(saved.getPlatform()).isEqualTo(DevicePlatform.ANDROID);
        assertThat(saved.getDeviceName()).isEqualTo("Pixel 8");
        assertThat(saved.getLastSeenAt()).isNotNull();
    }

    @Test
    @DisplayName("Réenregistrer le même jeton met à jour la ligne au lieu d'en créer une seconde")
    void reRegisteringUpdatesInPlace() {
        DeviceToken existing = existingDevice(alice);
        when(repository.findByToken(FCM_TOKEN)).thenReturn(Optional.of(existing));

        service.register(alice, new RegisterDeviceRequest(
                FCM_TOKEN, DevicePlatform.ANDROID, "device-1", "Pixel 8", "1.1.0"));

        DeviceToken saved = captureSaved();
        assertThat(saved.getId()).isEqualTo(existing.getId());
        assertThat(saved.getAppVersion()).isEqualTo("1.1.0");
    }

    /**
     * Le scénario à ne pas rater : sans réattribution, le nouveau porteur du téléphone
     * recevrait les notifications du compte précédent.
     */
    @Test
    @DisplayName("Un jeton repris par un autre compte change de propriétaire")
    void reassignsTokenToNewOwner() {
        DeviceToken existing = existingDevice(alice);
        User bob = userWithId();
        when(repository.findByToken(FCM_TOKEN)).thenReturn(Optional.of(existing));

        service.register(bob, new RegisterDeviceRequest(
                FCM_TOKEN, DevicePlatform.ANDROID, "device-1", "Pixel 8", "1.0.0"));

        DeviceToken saved = captureSaved();
        assertThat(saved.getId()).isEqualTo(existing.getId());
        assertThat(saved.getUser()).isEqualTo(bob);
        verify(repository, times(1)).save(any());
    }

    @Test
    @DisplayName("Le désenregistrement cible le couple utilisateur + jeton")
    void unregisterIsScopedToUser() {
        UUID userId = alice.getId();

        service.unregister(userId, FCM_TOKEN);

        verify(repository).deleteByUserIdAndToken(userId, FCM_TOKEN);
    }

    private DeviceToken captureSaved() {
        ArgumentCaptor<DeviceToken> captor = ArgumentCaptor.forClass(DeviceToken.class);
        verify(repository).save(captor.capture());
        return captor.getValue();
    }

    private static User userWithId() {
        User user = new User();
        user.setId(UUID.randomUUID());
        return user;
    }

    private static DeviceToken existingDevice(User owner) {
        DeviceToken device = new DeviceToken();
        device.setId(UUID.randomUUID());
        device.setUser(owner);
        device.setToken(FCM_TOKEN);
        device.setPlatform(DevicePlatform.ANDROID);
        return device;
    }
}
