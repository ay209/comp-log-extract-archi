package hai913i.tp2;

import hai913i.tp2.input.CsvFactsSource;
import hai913i.tp2.input.FactsSource;
import java.io.IOException;
import java.nio.file.Path;

/**
 * Les deux jeux de faits utilisés par les tests des étapes A1 à C3.
 *
 * <ul>
 *   <li>{@code facts-mini} (src/test/resources) : 3 classes, 5 méthodes, 4 arcs ; toutes les
 *       valeurs attendues se calculent à la main (examples/README.txt du sujet).</li>
 *   <li>{@code facts-validation-jdt} (c4/faits/validation-jdt) : le projet de validation,
 *       reconstruit par l'extracteur JDT du TP 1. Le jeu canonique du sujet n'est pas dans le
 *       dépôt ; ce jeu-là a les mêmes compteurs (15/59/44) et donne exactement la même
 *       partition (indice de Rand 1, bonus C4), donc les valeurs attendues du sujet.</li>
 * </ul>
 */
public final class JeuxDeTest {

    /** Tolérance des comparaisons de double (celle du sujet). */
    public static final double EPS = 1e-9;

    private JeuxDeTest() {}

    public static FactsSource mini() throws IOException {
        return new CsvFactsSource(Path.of("src", "test", "resources"), "facts-mini");
    }

    public static Path dossierValidation() {
        return Path.of("c4", "faits", "validation-jdt");
    }

    public static FactsSource validation() throws IOException {
        return new CsvFactsSource(dossierValidation(), "facts-validation-jdt");
    }

    /** Nom qualifié d'une classe du projet de validation à partir de son nom court. */
    public static String v(String nomCourt) {
        return switch (nomCourt) {
            case "LibraryApp" -> "library.app.LibraryApp";
            case "Book", "Category", "Dvd", "Item", "Loanable", "Member" -> "library.model." + nomCourt;
            case "Catalog", "ConsoleNotifier", "LoanException", "LoanService", "Notifier" ->
                    "library.service." + nomCourt;
            case "Loan" -> "library.service.LoanService.Loan";
            case "Statistics", "TextUtils" -> "library.util." + nomCourt;
            default -> throw new IllegalArgumentException("classe inconnue : " + nomCourt);
        };
    }
}
