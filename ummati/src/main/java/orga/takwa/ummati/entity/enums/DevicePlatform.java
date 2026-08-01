package orga.takwa.ummati.entity.enums;

/**
 * Support d'exécution du client.
 *
 * <p>Sert à deux décisions distinctes : le canal de push à emprunter (FCM pour les
 * apps natives, Web Push pour le navigateur) et la durée de session à accorder —
 * une app installée n'a pas les mêmes contraintes qu'un navigateur partagé.
 */
public enum DevicePlatform {
    IOS,
    ANDROID,
    WEB;

    /** Vrai pour les clients installés, qui obtiennent une session longue. */
    public boolean isNative() {
        return this == IOS || this == ANDROID;
    }
}
