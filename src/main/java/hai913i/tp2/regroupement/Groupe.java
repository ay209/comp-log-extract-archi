package hai913i.tp2.regroupement;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Un groupe de classes.
 *  une feuille (une seule classe),
 *  le résultat de la fusion de deux groupes.
 */
public class Groupe {
    public int id;                                   // numéro du groupe
    public List<String> classes = new ArrayList<>(); // toutes les classes du groupe
    public Groupe gauche;                            // premier fils
    public Groupe droit;                             // second fils
    public double similarite;                        // similarité au moment de la fusion

    /** Feuille : un groupe qui contient une seule classe. */
    public Groupe(int id, String classe) {
        this.id = id;
        this.classes.add(classe);
    }

    /** Nœud interne : fusion de deux groupes. */
    public Groupe(int id, Groupe gauche, Groupe droit, double similarite) {
        this.id = id;
        this.gauche = gauche;
        this.droit = droit;
        this.similarite = similarite;
        // les classes du nouveau groupe
        this.classes.addAll(gauche.classes);
        this.classes.addAll(droit.classes);
    }

    public boolean estFeuille() {
        return gauche == null;
    }

    /** Noms courts non ambigus du jeu de données courant (voir fixerNoms). */
    private static Map<String, String> nomsCourts = Map.of();

    /**
     * Fixe les noms courts pour un jeu de classes : chaque classe reçoit le plus court
     * suffixe de son nom qualifié qui ne désigne qu'elle. Sans homonyme (projet de
     * validation), c'est le nom simple, comme avant. Avec les trois TestRunner de JUnit
     * (bonus C4), on obtient textui.TestRunner, awtui.TestRunner et swingui.TestRunner.
     * Appelé une fois, juste après le chargement des faits.
     */
    public static void fixerNoms(List<String> classes) {
        Map<String, String> noms = new HashMap<>();
        for (String q : classes) {
            String[] parties = q.split("\\.");
            String nom = q;
            for (int n = 1; n <= parties.length; n++) {
                String suffixe = String.join(".",
                        List.of(parties).subList(parties.length - n, parties.length));
                boolean unique = true;
                for (String autre : classes) {
                    if (!autre.equals(q) && (autre.equals(suffixe) || autre.endsWith("." + suffixe))) {
                        unique = false;
                        break;
                    }
                }
                if (unique) {
                    nom = suffixe;
                    break;
                }
            }
            noms.put(q, nom);
        }
        nomsCourts = Map.copyOf(noms);
    }

    /** Nom court d'une classe : le nom simple, allongé s'il est ambigu (voir fixerNoms). */
    public static String nomCourt(String classe) {
        String connu = nomsCourts.get(classe);
        if (connu != null) {
            return connu;
        }
        return classe.substring(classe.lastIndexOf('.') + 1);
    }

    /** Affichage lisible du groupe  */
    public String libelle() {
        StringBuilder texte = new StringBuilder("{");
        for (int i = 0; i < classes.size(); i++) {
            if (i > 0) {
                texte.append(", ");
            }
            texte.append(nomCourt(classes.get(i)));
        }
        return texte.append("}").toString();
    }
}