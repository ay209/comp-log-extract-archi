package hai913i.tp2.regroupement;

import hai913i.tp2.couplage.Couplage;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Coupe du dendrogramme : on en tire les modules selon le seuil CP. */
public class Coupe {

    // deux double à moins de 1e-9 d'écart sont considérés égaux
    private static final double TOLERANCE = 1e-9;

    /**
     * Couplage interne moyen C(G) d'un groupe :
     * la moyenne du couplage sur tous les couples de classes du groupe.
     * Un groupe de n classes a n(n-1)/2 couples.
     * (Ce n'est PAS la similarité de la fusion : celle-ci se calcule entre les deux fils.)
     */
    public static double couplageInterneMoyen(Groupe groupe, Couplage couplage) {
        int n = groupe.classes.size();
        if (n < 2) {
            return 0.0; // une seule classe(pas de couple)
        }
        double somme = 0;
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                somme += couplage.valeur(groupe.classes.get(i), groupe.classes.get(j));
            }
        }
        return somme / (n * (n - 1 ) / 2);
    }

    /** Coupe l'arbre depuis la racine et renvoie la liste des modules. */
    public static List<Groupe> couper(Groupe racine, Couplage couplage, double cp) {
        List<Groupe> modules = new ArrayList<>();
        visiter(racine, couplage, cp, modules);
        return modules;
    }

    private static void visiter(Groupe groupe, Couplage couplage, double cp, List<Groupe> modules) {
        // une feuille est toujours un module (d'une seule classe)
        if (groupe.estFeuille()) {
            modules.add(groupe);
            return;
        }
        // C(G) strictement supérieur à CP : le nœud devient un module, on ne descend pas plus bas
        if (couplageInterneMoyen(groupe, couplage) > cp + TOLERANCE) {
            modules.add(groupe);
            return;
        }
        // sinon on descend dans les deux fils
        visiter(groupe.gauche, couplage, cp, modules);
        visiter(groupe.droit, couplage, cp, modules);
    }

    /** Vérifie que les modules forment une partition : aucune classe oubliée, aucune en double. */
    public static boolean partitionValide(List<Groupe> modules, int nbClasses) {
        List<String> toutes = new ArrayList<>();
        for (Groupe module : modules) {
            toutes.addAll(module.classes);
        }
        Set<String> distinctes = new HashSet<>(toutes);
        return toutes.size() == nbClasses && distinctes.size() == nbClasses;
    }

    /** Vérifie que tout module de 2 classes ou plus a C(G) strictement supérieur à CP. */
    public static boolean seuilRespecte(List<Groupe> modules, Couplage couplage, double cp) {
        for (Groupe module : modules) {
            if (module.classes.size() >= 2
                    && couplageInterneMoyen(module, couplage) <= cp + TOLERANCE) {
                return false;
            }
        }
        return true;
    }
}