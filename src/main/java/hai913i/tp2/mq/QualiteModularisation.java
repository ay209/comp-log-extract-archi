package hai913i.tp2.mq;

import hai913i.tp2.input.FactsSource;
import hai913i.tp2.regroupement.Groupe;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Qualité de modularisation MQ de Mancoridis et al. (IWPC 1998).
 *
 * Le graphe de dépendances entre modules (MDG) a un nœud par classe et un arc orienté
 * A → B si une méthode de A appelle une méthode de B (au moins une fois ; les arcs internes
 * à une classe sont écartés, comme en A1). Pour une partition en k modules :
 *
 *   A_i  = μ_i / N_i²            connectivité interne du module i
 *                                (μ_i : arcs entre classes du module, N_i : nombre de classes)
 *   E_ij = ε_ij / (2 N_i N_j)    connectivité entre les modules i et j
 *                                (ε_ij : arcs entre i et j, dans les deux sens)
 *
 *   MQ = (1/k) Σ A_i  −  (1 / (k(k−1)/2)) Σ_{i<j} E_ij      si k > 1
 *   MQ = A_1                                                 si k = 1
 *
 * MQ est compris entre −1 et 1 : il récompense les arcs à l'intérieur des modules et
 * pénalise ceux qui les relient. Contrairement à la coupe, il ne dépend d'aucun seuil.
 */
public final class QualiteModularisation {

    private QualiteModularisation() {}

    /** Un arc orienté du MDG : la classe appelante dépend de la classe appelée. */
    public record Dependance(String de, String vers) {}

    /** Le détail du calcul. */
    public record Resultat(int modules, double interne, double externe, double mq) {}

    /** Le MDG : les arcs orientés entre classes distinctes, sans doublon. */
    public static Set<Dependance> graphe(FactsSource faits) {
        Set<Dependance> mdg = new LinkedHashSet<>();
        for (FactsSource.Arc arc : faits.arcs()) {
            if (!arc.isIntraClass()) {
                mdg.add(new Dependance(arc.callerClass(), arc.calleeClass()));
            }
        }
        return mdg;
    }

    /** MQ d'une partition donnée par ses modules (groupes issus de la coupe). */
    public static Resultat calculer(List<Groupe> modules, Set<Dependance> mdg) {
        List<List<String>> classes = new ArrayList<>();
        for (Groupe g : modules) {
            classes.add(g.classes);
        }
        return calculerClasses(classes, mdg);
    }

    /** MQ d'une partition donnée par les listes de classes de ses modules. */
    public static Resultat calculerClasses(List<? extends List<String>> modules, Set<Dependance> mdg) {
        int k = modules.size();
        Map<String, Integer> moduleDe = new HashMap<>();
        for (int i = 0; i < k; i++) {
            for (String c : modules.get(i)) {
                moduleDe.put(c, i);
            }
        }
        int[] mu = new int[k];             // arcs internes à chaque module
        int[][] eps = new int[k][k];       // arcs entre deux modules (i < j), deux sens confondus
        for (Dependance d : mdg) {
            Integer i = moduleDe.get(d.de());
            Integer j = moduleDe.get(d.vers());
            if (i == null || j == null) {
                continue;                  // classe hors partition : ignorée
            }
            if (i.equals(j)) {
                mu[i]++;
            } else {
                eps[Math.min(i, j)][Math.max(i, j)]++;
            }
        }
        double sommeA = 0;
        for (int i = 0; i < k; i++) {
            double n = modules.get(i).size();
            sommeA += mu[i] / (n * n);
        }
        double interne = sommeA / k;
        if (k == 1) {
            return new Resultat(1, interne, 0.0, interne);
        }
        double sommeE = 0;
        for (int i = 0; i < k; i++) {
            for (int j = i + 1; j < k; j++) {
                double ni = modules.get(i).size();
                double nj = modules.get(j).size();
                sommeE += eps[i][j] / (2 * ni * nj);
            }
        }
        double externe = sommeE / (k * (k - 1) / 2.0);
        return new Resultat(k, interne, externe, interne - externe);
    }
}
