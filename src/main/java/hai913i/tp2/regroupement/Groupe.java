package hai913i.tp2.regroupement;

import java.util.ArrayList;
import java.util.List;

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

    /** Nom court d'une classe  */
    public static String nomCourt(String classe) {
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