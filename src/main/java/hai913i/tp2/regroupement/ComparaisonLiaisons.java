package hai913i.tp2.regroupement;

import java.util.HashSet;
import java.util.List;

/** Bonus C1 */
public class ComparaisonLiaisons {

    /**
     * Numéro (à partir de 1) de la première étape où les deux regroupements
     * ne fusionnent pas les mêmes classes. Renvoie -1 si les deux arbres sont identiques.
     * On compare les classes du groupe créé, pas son identifiant.
     */
    public static int premiereDivergence(List<Groupe> fusionsA, List<Groupe> fusionsB) {
        int nb = Math.min(fusionsA.size(), fusionsB.size());
        for (int i = 0; i < nb; i++) {
            if (!new HashSet<>(fusionsA.get(i).classes).equals(new HashSet<>(fusionsB.get(i).classes))) {
                return i + 1;
            }
        }
        return -1;
    }

    /** Bonus C2 : vrai si un module de la liste contient exactement les mêmes classes que "module". */
    public static boolean moduleIdentiqueDans(Groupe module, List<Groupe> modules) {
        for (Groupe autre : modules) {
            if (new HashSet<>(autre.classes).equals(new HashSet<>(module.classes))) {
                return true;
            }
        }
        return false;
    }
}