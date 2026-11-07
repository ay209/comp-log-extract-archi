package hai913i.tp2.mq;

import hai913i.tp2.couplage.Couplage;
import hai913i.tp2.regroupement.Coupe;
import hai913i.tp2.regroupement.Groupe;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

/**
 * Choix automatique de CP par le MQ.
 *
 * La coupe ne change que lorsque CP franchit le couplage interne moyen C̄(G) d'un nœud de
 * l'arbre (section 7.4 du rapport). On énumère donc toutes les coupes possibles, une par
 * intervalle [a ; b[ de CP, au lieu d'essayer des valeurs au hasard. Pour chacune : nombre
 * de modules, contrainte M/2, MQ. On retient, parmi les coupes qui respectent M/2, celle
 * de plus grand MQ, et on propose le milieu de son intervalle comme valeur de CP.
 */
public final class ChoixDuSeuil {

    private static final double TOLERANCE = 1e-9;

    private ChoixDuSeuil() {}

    /** Une coupe possible : CP dans [debut ; fin[ (fin infinie pour la dernière). */
    public record Palier(double debut, double fin, List<Groupe> modules, int maximum,
                         QualiteModularisation.Resultat mq) {
        public boolean respectee() {
            return modules.size() <= maximum;
        }

        /** Un CP représentatif : le milieu de l'intervalle (le début pour le dernier). */
        public double milieu() {
            return Double.isInfinite(fin) ? debut : (debut + fin) / 2;
        }
    }

    /** Toutes les coupes possibles, de CP = 0 à l'infini, paliers identiques fusionnés. */
    public static List<Palier> paliers(Groupe racine, Couplage couplage,
                                       Set<QualiteModularisation.Dependance> mdg, int nbClasses) {
        TreeSet<Double> seuils = new TreeSet<>();
        seuils.add(0.0);
        collecter(racine, couplage, seuils);
        List<Double> bornes = new ArrayList<>();
        for (double s : seuils) {                 // valeurs à moins de TOLERANCE confondues
            if (bornes.isEmpty() || s - bornes.get(bornes.size() - 1) > TOLERANCE) {
                bornes.add(s);
            }
        }

        List<Palier> paliers = new ArrayList<>();
        for (int i = 0; i < bornes.size(); i++) {
            double debut = bornes.get(i);
            double fin = i + 1 < bornes.size() ? bornes.get(i + 1) : Double.POSITIVE_INFINITY;
            List<Groupe> modules = Coupe.couper(racine, couplage, debut);
            Palier precedent = paliers.isEmpty() ? null : paliers.get(paliers.size() - 1);
            if (precedent != null && memePartition(precedent.modules(), modules)) {
                paliers.set(paliers.size() - 1, new Palier(precedent.debut(), fin, precedent.modules(),
                        precedent.maximum(), precedent.mq()));
            } else {
                paliers.add(new Palier(debut, fin, modules, nbClasses / 2,
                        QualiteModularisation.calculer(modules, mdg)));
            }
        }
        return paliers;
    }

    /** La coupe de meilleur MQ parmi celles qui respectent M/2 (à égalité : la première). */
    public static Palier meilleur(List<Palier> paliers) {
        return meilleur(paliers, true);
    }

    /** La coupe de meilleur MQ, sous la contrainte M/2 ou non (à égalité : la première). */
    public static Palier meilleur(List<Palier> paliers, boolean sousContrainte) {
        Palier meilleur = null;
        for (Palier p : paliers) {
            boolean admise = p.respectee() || !sousContrainte;
            if (admise && (meilleur == null || p.mq().mq() > meilleur.mq().mq() + TOLERANCE)) {
                meilleur = p;
            }
        }
        return meilleur;
    }

    /** Le palier qui contient une valeur de CP donnée. */
    public static Palier palierDe(List<Palier> paliers, double cp) {
        for (Palier p : paliers) {
            if (cp >= p.debut() - TOLERANCE && cp < p.fin() - TOLERANCE) {
                return p;
            }
        }
        return paliers.get(paliers.size() - 1);
    }

    private static void collecter(Groupe g, Couplage couplage, Set<Double> seuils) {
        if (g.estFeuille()) {
            return;
        }
        seuils.add(Coupe.couplageInterneMoyen(g, couplage));
        collecter(g.gauche, couplage, seuils);
        collecter(g.droit, couplage, seuils);
    }

    private static boolean memePartition(List<Groupe> a, List<Groupe> b) {
        return ensembles(a).equals(ensembles(b));
    }

    private static Set<Set<String>> ensembles(List<Groupe> modules) {
        Set<Set<String>> p = new HashSet<>();
        for (Groupe g : modules) {
            p.add(new HashSet<>(g.classes));
        }
        return p;
    }

    /** Le tableau des paliers, prêt à afficher. */
    public static List<String> tableau(List<Palier> paliers, Palier retenu, Palier courant) {
        List<String> out = new ArrayList<>();
        out.add("  CP dans               modules  verdict      interne  externe      MQ");
        for (Palier p : paliers) {
            String fin = Double.isInfinite(p.fin()) ? "   +inf" : String.format(Locale.ROOT, "%.5f", p.fin());
            String marque = (p == retenu ? "  <- meilleur MQ" : "") + (p == courant ? "  <- CP courant" : "");
            out.add(String.format(Locale.ROOT, "  [%.5f ; %s[  %6d  %-9s  %8.4f  %7.4f  %7.4f%s",
                    p.debut(), fin, p.modules().size(), p.respectee() ? "respectée" : "VIOLÉE",
                    p.mq().interne(), p.mq().externe(), p.mq().mq(), marque));
        }
        return out;
    }
}
