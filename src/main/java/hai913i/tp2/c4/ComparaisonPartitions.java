package hai913i.tp2.c4;

import hai913i.tp2.regroupement.Groupe;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Bonus C4 : compare deux partitions des mêmes classes.
 *
 * Usages : les modules d'une coupe contre les paquetages réels, ou les modules obtenus
 * sur deux jeux de faits du même projet (deux extracteurs).
 *
 * Deux mesures :
 *  - comptage des couples : pour chaque couple de classes, sont-elles ensemble dans A,
 *    dans B, dans les deux, dans aucune ? L'indice de Rand est la part des couples sur
 *    lesquels A et B sont d'accord (1 = partitions identiques) ;
 *  - pureté de A selon B : pour chaque groupe de A, la part de ses classes qui tombent
 *    dans son groupe dominant de B, moyennée sur toutes les classes. Avec B = les
 *    paquetages, elle dit si un module vient d'un seul paquetage ; la pureté inverse dit
 *    si un paquetage tient dans un seul module.
 *
 * Les classes présentes d'un seul côté sont écartées du calcul et signalées.
 */
public final class ComparaisonPartitions {

    private ComparaisonPartitions() {}

    /** Le résultat d'une comparaison. */
    public record Resultat(int classes, Set<String> seulementA, Set<String> seulementB,
                           long ensembleDansLesDeux, long ensembleDansASeul,
                           long ensembleDansBSeul, long separesDansLesDeux,
                           double purete, double pureteInverse) {

        /** Part des couples sur lesquels A et B sont d'accord. */
        public double rand() {
            long tous = ensembleDansLesDeux + ensembleDansASeul + ensembleDansBSeul + separesDansLesDeux;
            return tous == 0 ? 1.0 : (double) (ensembleDansLesDeux + separesDansLesDeux) / tous;
        }

        /** Vrai si les deux partitions sont identiques sur les classes communes. */
        public boolean identiques() {
            return ensembleDansASeul == 0 && ensembleDansBSeul == 0;
        }
    }

    /** Les modules d'une coupe vus comme une partition : classe → "M1", "M2"… */
    public static Map<String, String> partition(List<Groupe> modules) {
        Map<String, String> p = new LinkedHashMap<>();
        for (int k = 0; k < modules.size(); k++) {
            for (String classe : modules.get(k).classes) {
                p.put(classe, "M" + (k + 1));
            }
        }
        return p;
    }

    /**
     * @param a classe → groupe dans la première partition
     * @param b classe → groupe dans la seconde
     */
    public static Resultat comparer(Map<String, String> a, Map<String, String> b) {
        Set<String> communes = new TreeSet<>(a.keySet());
        communes.retainAll(b.keySet());
        Set<String> seulA = new TreeSet<>(a.keySet());
        seulA.removeAll(b.keySet());
        Set<String> seulB = new TreeSet<>(b.keySet());
        seulB.removeAll(a.keySet());
        List<String> c = new ArrayList<>(communes);

        long deux = 0;
        long aSeul = 0;
        long bSeul = 0;
        long aucun = 0;
        for (int i = 0; i < c.size(); i++) {
            for (int j = i + 1; j < c.size(); j++) {
                boolean ensembleA = a.get(c.get(i)).equals(a.get(c.get(j)));
                boolean ensembleB = b.get(c.get(i)).equals(b.get(c.get(j)));
                if (ensembleA && ensembleB) {
                    deux++;
                } else if (ensembleA) {
                    aSeul++;
                } else if (ensembleB) {
                    bSeul++;
                } else {
                    aucun++;
                }
            }
        }
        return new Resultat(c.size(), seulA, seulB, deux, aSeul, bSeul, aucun,
                purete(c, a, b), purete(c, b, a));
    }

    /** Pureté de x selon y : part des classes qui tombent dans le groupe dominant de leur groupe. */
    static double purete(List<String> classes, Map<String, String> x, Map<String, String> y) {
        if (classes.isEmpty()) {
            return 1.0;
        }
        Map<String, Map<String, Integer>> comptes = new TreeMap<>();
        for (String classe : classes) {
            comptes.computeIfAbsent(x.get(classe), k -> new TreeMap<>()).merge(y.get(classe), 1, Integer::sum);
        }
        int dominantes = 0;
        for (Map<String, Integer> parGroupe : comptes.values()) {
            dominantes += parGroupe.values().stream().mapToInt(Integer::intValue).max().orElse(0);
        }
        return (double) dominantes / classes.size();
    }

    /**
     * Une ligne par module de plus d'une classe : sa taille, son paquetage dominant, et
     * les classes qui viennent d'ailleurs.
     */
    public static List<String> detailParModule(List<Groupe> modules, Map<String, String> paquetages) {
        List<String> lignes = new ArrayList<>();
        for (int k = 0; k < modules.size(); k++) {
            Groupe m = modules.get(k);
            if (m.classes.size() < 2) {
                continue;
            }
            Map<String, List<String>> parPaquetage = new TreeMap<>();
            for (String classe : m.classes) {
                parPaquetage.computeIfAbsent(paquetages.get(classe), x -> new ArrayList<>())
                        .add(Groupe.nomCourt(classe));
            }
            String dominant = null;
            for (Map.Entry<String, List<String>> e : parPaquetage.entrySet()) {
                if (dominant == null || e.getValue().size() > parPaquetage.get(dominant).size()) {
                    dominant = e.getKey();
                }
            }
            StringBuilder l = new StringBuilder(String.format(Locale.ROOT,
                    "  M%-3d %2d classes  dominant %s (%d/%d)", k + 1, m.classes.size(),
                    dominant, parPaquetage.get(dominant).size(), m.classes.size()));
            if (parPaquetage.size() > 1) {
                Map<String, List<String>> autres = new TreeMap<>(parPaquetage);
                autres.remove(dominant);
                l.append("  autres : ").append(autres);
            }
            lignes.add(l.toString());
        }
        return lignes;
    }

    /** Résumé lisible d'une comparaison. */
    public static List<String> resume(Resultat r, String nomA, String nomB) {
        List<String> l = new ArrayList<>();
        l.add("Classes communes          : " + r.classes());
        if (!r.seulementA().isEmpty()) {
            l.add("Seulement dans " + nomA + " : " + r.seulementA());
        }
        if (!r.seulementB().isEmpty()) {
            l.add("Seulement dans " + nomB + " : " + r.seulementB());
        }
        l.add("Couples ensemble des deux côtés : " + r.ensembleDansLesDeux());
        l.add("Ensemble seulement dans " + nomA + " : " + r.ensembleDansASeul());
        l.add("Ensemble seulement dans " + nomB + " : " + r.ensembleDansBSeul());
        l.add("Séparés des deux côtés         : " + r.separesDansLesDeux());
        l.add(String.format(Locale.ROOT, "Indice de Rand                 : %.4f%s", r.rand(),
                r.identiques() ? "   (partitions identiques)" : ""));
        l.add(String.format(Locale.ROOT, "Pureté de %s selon %s : %.4f", nomA, nomB, r.purete()));
        l.add(String.format(Locale.ROOT, "Pureté de %s selon %s : %.4f", nomB, nomA, r.pureteInverse()));
        return l;
    }
}
