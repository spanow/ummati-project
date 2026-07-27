package orga.takwa.ummati.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Encodage des cellules de l'export CSV des inscrits.
 *
 * <p>Les prénoms, noms et emails sont saisis par les utilisateurs : ils ne peuvent pas
 * être concaténés tels quels dans un fichier téléchargé puis ouvert dans un tableur.
 */
class EventServiceCsvExportTest {

    @Test
    void shouldQuoteEveryCell() {
        assertThat(EventService.csvCell("Amina")).isEqualTo("\"Amina\"");
    }

    @Test
    void shouldKeepColumnsAligned_whenValueContainsAComma() {
        // Sans guillemets, « Dupont, Jean » décalait toutes les colonnes suivantes.
        assertThat(EventService.csvCell("Dupont, Jean")).isEqualTo("\"Dupont, Jean\"");
    }

    @Test
    void shouldDoubleInnerQuotes() {
        assertThat(EventService.csvCell("Jean \"Jojo\" Dupont"))
                .isEqualTo("\"Jean \"\"Jojo\"\" Dupont\"");
    }

    @Test
    void shouldKeepNewlinesInsideTheQuotedCell() {
        assertThat(EventService.csvCell("ligne1\nligne2")).isEqualTo("\"ligne1\nligne2\"");
    }

    @Test
    void shouldNeutralizeFormulas() {
        // Excel et LibreOffice exécutent une cellule commençant par = + - @.
        assertThat(EventService.csvCell("=1+1")).isEqualTo("\"'=1+1\"");
        assertThat(EventService.csvCell("+33612345678")).isEqualTo("\"'+33612345678\"");
        assertThat(EventService.csvCell("-2")).isEqualTo("\"'-2\"");
        assertThat(EventService.csvCell("@SUM(A1)")).isEqualTo("\"'@SUM(A1)\"");
    }

    @Test
    void shouldNeutralizeTheClassicRemoteCommandPayload() {
        String payload = "=cmd|'/c calc'!A1";

        String cell = EventService.csvCell(payload);

        assertThat(cell).startsWith("\"'=");
    }

    @Test
    void shouldHandleNullAndEmpty() {
        assertThat(EventService.csvCell(null)).isEqualTo("\"\"");
        assertThat(EventService.csvCell("")).isEqualTo("\"\"");
    }
}
