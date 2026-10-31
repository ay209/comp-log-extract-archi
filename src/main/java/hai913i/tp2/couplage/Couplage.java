package hai913i.tp2.couplage;

import hai913i.tp2.input.FactsSource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Couplage {

    private Map<String, Couple> couples = new HashMap<>();
    private int nbArcsInter = 0;

    public Couplage(FactsSource faits) {
        for (FactsSource.Arc arc : faits.arcs()) {
            if (arc.isIntraClass()) {
                continue; //  on écarte l'arc interne
            }
            nbArcsInter++;

            // on range toujours les deux noms dans l'ordre alphabétique
            // pour que A-B et B-A donnent le même couple
            String a = arc.callerClass();
            String b = arc.calleeClass();
            if (a.compareTo(b) > 0) {
                String temp = a;
                a = b;
                b = temp;
            }

            String cle = a + "|" + b;
            Couple couple = couples.get(cle);
            if (couple == null) {
                couple = new Couple(a, b);
                couples.put(cle, couple);
            }
            couple.nbArcs++;
        }
    }

    public int nbArcsInter() {
        return nbArcsInter;
    }

    public int nbCouplesNonNuls() {
        return couples.size();
    }

    /** Couplage C(a, b) = r(a, b) / |Einter|. Vaut 0 si le couple est absent. */
    public double valeur(String classeA, String classeB) {
        if (classeA.equals(classeB)) {
            return 0.0;
        }
        if (classeA.compareTo(classeB) > 0) {
            String temp = classeA;
            classeA = classeB;
            classeB = temp;
        }
        Couple couple = couples.get(classeA + "|" + classeB);
        if (couple == null) {
            return 0.0;
        }
        return (double) couple.nbArcs / nbArcsInter;
    }

    /** Somme de tous les couplages : doit etre 1. */
    public double somme() {
        double total = 0;
        for (Couple couple : couples.values()) {
            total += (double) couple.nbArcs / nbArcsInter;
        }
        return total;
    }

    /** Les couples, du plus couplé au moins couplé  */
    public List<Couple> couplesTries() {
        List<Couple> liste = new ArrayList<>(couples.values());
        liste.sort((p, q) -> {
            if (p.nbArcs != q.nbArcs) {
                return q.nbArcs - p.nbArcs;
            }
            int diff = p.classeA.compareTo(q.classeA);
            if (diff != 0) {
                return diff;
            }
            return p.classeB.compareTo(q.classeB);
        });
        return liste;
    }
}