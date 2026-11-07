package hai913i.tp2.c4;

import hai913i.tp2.input.FactsSource;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Bonus C4 : le paquetage réel de chaque classe, pour comparer les modules à
 * l'architecture déclarée par les développeurs.
 *
 * Le jeu canonique donne à une classe imbriquée sa classe englobante comme « paquetage »
 * (LoanService.Loan → library.service.LoanService). On remonte alors jusqu'au paquetage
 * de la classe englobante, pour que LoanService.Loan compte bien dans library.service.
 */
public final class Paquetages {

    private Paquetages() {}

    /** Classe → paquetage réel, dans l'ordre des classes du jeu de faits. */
    public static Map<String, String> reels(FactsSource facts) {
        Map<String, String> resultat = new LinkedHashMap<>();
        for (String classe : facts.classNames()) {
            String p = facts.packageOf(classe);
            // garde-fou : au plus 32 niveaux d'imbrication
            for (int i = 0; i < 32 && facts.classNames().contains(p); i++) {
                p = facts.packageOf(p);
            }
            resultat.put(classe, p);
        }
        return resultat;
    }
}
