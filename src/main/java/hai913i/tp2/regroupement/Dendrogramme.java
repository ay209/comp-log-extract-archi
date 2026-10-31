package hai913i.tp2.regroupement;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Outils pour compter, vérifier et exporter le dendrogramme (l'arbre de regroupement). */
public class Dendrogramme {

    /** Nombre de feuilles de l'arbre (une feuille = une classe). */
    public static int compterFeuilles(Groupe groupe) {
        if (groupe.estFeuille()) {
            return 1;
        }
        return compterFeuilles(groupe.gauche) + compterFeuilles(groupe.droit);
    }

    /** Nombre de nœuds internes de l'arbre (un nœud interne = une fusion). */
    public static int compterInternes(Groupe groupe) {
        if (groupe.estFeuille()) {
            return 0;
        }
        return 1 + compterInternes(groupe.gauche) + compterInternes(groupe.droit);
    }

    /**
     * Vérifie l'arbre :
     * - la racine contient les M classes, chacune une seule fois ;
     * - pour chaque nœud interne, les classes sont exactement l'union disjointe de celles des deux fils.
     */
    public static boolean estValide(Groupe racine, int nbClasses) {
        Set<String> distinctes = new HashSet<>(racine.classes);
        if (racine.classes.size() != nbClasses || distinctes.size() != nbClasses) {
            return false;
        }
        return noeudValide(racine);
    }

    private static boolean noeudValide(Groupe groupe) {
        if (groupe.estFeuille()) {
            return true;
        }
        Set<String> union = new HashSet<>(groupe.gauche.classes);
        union.addAll(groupe.droit.classes);

        // pas de classe en commun entre les deux fils (la taille de l'union = somme des tailles)
        boolean disjoints = union.size() == groupe.gauche.classes.size() + groupe.droit.classes.size();
        // et le nœud contient exactement cette union
        boolean memesClasses = union.equals(new HashSet<>(groupe.classes))
                && groupe.classes.size() == union.size();

        return disjoints && memesClasses && noeudValide(groupe.gauche) && noeudValide(groupe.droit);
    }

    /** l'arbre indenté : un nœud interne par fusion, une feuille par classe. */
    public static void exporterTexte(Path fichier, Groupe racine) throws IOException {
        List<String> lignes = new ArrayList<>();
        ecrireNoeud(racine, "", lignes);
        ecrireFichier(fichier, lignes);
    }

    private static void ecrireNoeud(Groupe groupe, String indentation, List<String> lignes) {
        if (groupe.estFeuille()) {
            lignes.add(indentation + "- " + groupe.classes.get(0) + "  (groupe " + groupe.id + ")");
            return;
        }
        lignes.add(String.format(Locale.ROOT, "%s+ groupe %d  S=%.4f  (%d classes)",
                indentation, groupe.id, groupe.similarite, groupe.classes.size()));

        ecrireNoeud(groupe.gauche, indentation + "    ", lignes);
        ecrireNoeud(groupe.droit, indentation + "    ", lignes);
    }

    /**  la liste des fusions dans l'ordre : étape, groupe créé, fils, similarité, membres. */
    public static void exporterFusions(Path fichier, List<Groupe> fusions) throws IOException {
        List<String> lignes = new ArrayList<>();
        lignes.add("etape;groupe;fils_gauche;fils_droit;similarite;membres");
        int etape = 1;
        for (Groupe fusion : fusions) {
            lignes.add(String.format(Locale.ROOT, "%d;%d;%d;%d;%.4f;%s",
                    etape, fusion.id, fusion.gauche.id, fusion.droit.id,
                    fusion.similarite, fusion.libelle()));
            etape++;
        }
        ecrireFichier(fichier, lignes);
    }

    /** l'arbre au format DOT (Graphviz) */
    public static void exporterDot(Path fichier, Groupe racine) throws IOException {
        List<String> lignes = new ArrayList<>();
        lignes.add("digraph dendrogramme {");
        lignes.add("  edge [arrowhead=none];");
        ecrireNoeudDot(racine, lignes);
        lignes.add("}");
        ecrireFichier(fichier, lignes);
    }

    private static void ecrireNoeudDot(Groupe groupe, List<String> lignes) {
        if (groupe.estFeuille()) {
            lignes.add("  n" + groupe.id + " [shape=ellipse, label=\""
                    + Groupe.nomCourt(groupe.classes.get(0)) + "\"];");
            return;
        }

        lignes.add(String.format(Locale.ROOT, "  n%d [shape=box, label=\"groupe %d\\nS=%.4f\"];",
                groupe.id, groupe.id, groupe.similarite));
        lignes.add("  n" + groupe.id + " -> n" + groupe.gauche.id + ";");
        lignes.add("  n" + groupe.id + " -> n" + groupe.droit.id + ";");
        ecrireNoeudDot(groupe.gauche, lignes);
        ecrireNoeudDot(groupe.droit, lignes);
    }

    /**  les lignes dans un fichier, en créant le dossier s'il n'existe pas. */
    private static void ecrireFichier(Path fichier, List<String> lignes) throws IOException {
        if (fichier.getParent() != null) {
            Files.createDirectories(fichier.getParent());
        }
        Files.write(fichier, lignes);
    }
}