package orga.takwa.ummati.repository;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Vérifie le calcul de distance servant à enrichir les résultats de la recherche
 * « autour de moi ». Le filtrage et le tri, eux, sont exécutés en base.
 */
class EventSpecificationDistanceTest {

    private static final double PARIS_LAT = 48.8566;
    private static final double PARIS_LNG = 2.3522;
    private static final double LYON_LAT = 45.7640;
    private static final double LYON_LNG = 4.8357;

    @Test
    void distanceKm_shouldBeZero_forTheSamePoint() {
        assertThat(EventSpecification.distanceKm(PARIS_LAT, PARIS_LNG, PARIS_LAT, PARIS_LNG))
                .isCloseTo(0.0, org.assertj.core.data.Offset.offset(0.001));
    }

    @Test
    void distanceKm_shouldMatchKnownDistance_parisLyon() {
        // ~392 km à vol d'oiseau
        double distance = EventSpecification.distanceKm(PARIS_LAT, PARIS_LNG, LYON_LAT, LYON_LNG);

        assertThat(distance).isCloseTo(392.0, org.assertj.core.data.Offset.offset(5.0));
    }

    @Test
    void distanceKm_shouldBeSymmetric() {
        double aToB = EventSpecification.distanceKm(PARIS_LAT, PARIS_LNG, LYON_LAT, LYON_LNG);
        double bToA = EventSpecification.distanceKm(LYON_LAT, LYON_LNG, PARIS_LAT, PARIS_LNG);

        assertThat(aToB).isCloseTo(bToA, org.assertj.core.data.Offset.offset(0.0001));
    }

    @Test
    void distanceKm_shouldHandleAntimeridianCrossing() {
        // De part et d'autre du 180e méridien : ~1 degré de longitude près de l'équateur
        double distance = EventSpecification.distanceKm(0.0, 179.5, 0.0, -179.5);

        assertThat(distance).isCloseTo(111.0, org.assertj.core.data.Offset.offset(2.0));
    }

    @Test
    void distanceKm_shouldStayStable_forVeryClosepoints() {
        // ~111 m d'écart en latitude : la formule de Haversine reste précise à courte
        // distance, contrairement à une formule basée sur acos().
        double distance = EventSpecification.distanceKm(48.8566, 2.3522, 48.8576, 2.3522);

        assertThat(distance).isCloseTo(0.111, org.assertj.core.data.Offset.offset(0.005));
    }
}
