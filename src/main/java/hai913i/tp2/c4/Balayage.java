package hai913i.tp2.c4;

import hai913i.tp2.couplage.Couplage;
import hai913i.tp2.regroupement.Coupe;
import hai913i.tp2.regroupement.Groupe;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Bonus C4 : coupe le même arbre pour toute une gamme de seuils CP.
 *
 * Le seuil ne se transpose pas d'un projet à l'autre : tous les couplages sont des
 * multiples de 1/|E_inter|, qui vaut 1/37 sur la validation mais 1/192 sur JUnit. Il faut
 * donc refaire le choix de CP pour chaque projet ; le balayage le rend visible d'un coup.
 * La gamme couvre les deux échelles.
 */
public final class Balayage {

    /** Les seuils balayés : fins pour un grand projet, plus larges pour la validation. */
    public static final List<Double> SEUILS = List.of(
            0.001, 0.002, 0.003, 0.004, 0.005, 0.006, 0.008,
            0.010, 0.015, 0.020, 0.025, 0.030, 0.035, 0.040, 0.045, 0.060);

    private Balayage() {}

    /** Une ligne du tableau. */
    public record Ligne(double cp, int modules, int maximum, String tailles) {
        public boolean respectee() {
            return modules <= maximum;
        }
    }

    /** Coupe l'arbre pour chaque seuil de SEUILS. */
    public static List<Ligne> balayer(Groupe racine, Couplage couplage, int nbClasses) {
        List<Ligne> lignes = new ArrayList<>();
        for (double cp : SEUILS) {
            List<Groupe> modules = Coupe.couper(racine, couplage, cp);
            List<Integer> tailles = new ArrayList<>();
            for (Groupe m : modules) {
                tailles.add(m.classes.size());
            }
            tailles.sort(Comparator.reverseOrder());
            lignes.add(new Ligne(cp, modules.size(), nbClasses / 2, resumerTailles(tailles)));
        }
        return lignes;
    }

    /** « 14+5+4+3+2×8+1×14 » : les tailles décroissantes, répétitions regroupées. */
    static String resumerTailles(List<Integer> tailles) {
        StringBuilder sb = new StringBuilder();
        int i = 0;
        while (i < tailles.size()) {
            int t = tailles.get(i);
            int n = 0;
            while (i < tailles.size() && tailles.get(i) == t) {
                n++;
                i++;
            }
            if (sb.length() > 0) {
                sb.append('+');
            }
            sb.append(t);
            if (n > 1) {
                sb.append('×').append(n);
            }
        }
        return sb.toString();
    }

    /** Le tableau, prêt à afficher. */
    public static List<String> tableau(List<Ligne> lignes) {
        List<String> out = new ArrayList<>();
        out.add("   CP     modules  max  verdict    tailles");
        for (Ligne l : lignes) {
            out.add(String.format(Locale.ROOT, "  %.3f  %6d  %4d  %-9s  %s", l.cp(), l.modules(),
                    l.maximum(), l.respectee() ? "respectée" : "VIOLÉE", l.tailles()));
        }
        return out;
    }
}
