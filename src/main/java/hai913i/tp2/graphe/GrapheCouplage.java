package hai913i.tp2.graphe;

import hai913i.tp2.couplage.Couple;
import hai913i.tp2.couplage.Couplage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Graphe pondéré du couplage.
 * Un nœud = une classe.
 * Une arête = deux classes qui ont un couplage non nul, avec le couplage comme poids.
 */
public class GrapheCouplage {

    // toutes les classes du projet (même celles sans arête)
    private List<String> noeuds = new ArrayList<>();

    // les arêtes (couples de classes avec leur poids)
    private List<Arete> aretes = new ArrayList<>();

    /** Graphe vide : sert quand on recharge un fichier. */
    public GrapheCouplage() {
    }

    /** Construit le graphe à partir du couplage calculé en A1. */
    public GrapheCouplage(Couplage couplage, List<String> classes) {
        // on ajoute toutes les classes, y compris les isolées (Dvd, LoanException)
        noeuds.addAll(classes);

        // une arête par couple de couplage non nul
        for (Couple c : couplage.couplesTries()) {
            aretes.add(new Arete(c.classeA, c.classeB, couplage.valeur(c.classeA, c.classeB)));
        }
    }

    /** Nombre de nœuds (de classes). */
    public int nbNoeuds() {
        return noeuds.size();
    }

    /** Nombre d'arêtes. */
    public int nbAretes() {
        return aretes.size();
    }

    /** Les arêtes, déjà rangées de la plus lourde à la plus légère. */
    public List<Arete> aretes() {
        return aretes;
    }

    /** Poids de l'arête entre a et b, dans un sens ou dans l'autre. Vaut 0 si elle n'existe pas. */
    public double poids(String a, String b) {
        for (Arete arete : aretes) {
            // l'arête peut être écrite A-B ou B-A, on teste les deux
            boolean sensDirect = arete.classeA.equals(a) && arete.classeB.equals(b);
            boolean sensInverse = arete.classeA.equals(b) && arete.classeB.equals(a);
            if (sensDirect || sensInverse) {
                return arete.poids;
            }
        }
        // couple absent : pas d'erreur, juste 0
        return 0.0;
    }

    /** Les classes qui n'apparaissent dans aucune arête. */
    public List<String> classesIsolees() {
        List<String> isolees = new ArrayList<>();
        for (String noeud : noeuds) {
            // on cherche si ce nœud touche au moins une arête
            boolean relie = false;
            for (Arete arete : aretes) {
                if (arete.classeA.equals(noeud) || arete.classeB.equals(noeud)) {
                    relie = true;
                }
            }
            // aucune arête trouvée : la classe est isolée
            if (!relie) {
                isolees.add(noeud);
            }
        }
        return isolees;
    }

    /**
     * Écrit le graphe dans un seul fichier CSV :
     */
    public void exporterCsv(Path fichier) throws IOException {
        List<String> lignes = new ArrayList<>();

        // d'abord les nœuds
        for (String noeud : noeuds) {
            lignes.add("N;" + noeud);
        }

        // puis les arêtes
        for (Arete arete : aretes) {
            // le double est écrit avec toute sa précision : on le relit à l'identique
            lignes.add("A;" + arete.classeA + ";" + arete.classeB + ";" + arete.poids);
        }

        // on crée le dossier (sorties/) s'il n'existe pas
        if (fichier.getParent() != null) {
            Files.createDirectories(fichier.getParent());
        }
        Files.write(fichier, lignes);
    }

    /** Relit le fichier écrit par exporterCsv et reconstruit le graphe. */
    public static GrapheCouplage charger(Path fichier) throws IOException {
        GrapheCouplage graphe = new GrapheCouplage();

        for (String ligne : Files.readAllLines(fichier)) {
            // on découpe la ligne sur les points-virgules avec split
            String[] morceaux = ligne.split(";");

            if (morceaux[0].equals("N")) {
                // ligne "N;classe" : on ajoute un nœud
                graphe.noeuds.add(morceaux[1]);
            } else if (morceaux[0].equals("A")) {
                // ligne "A;classeA;classeB;poids" : on ajoute une arête
                graphe.aretes.add(new Arete(morceaux[1], morceaux[2], Double.parseDouble(morceaux[3])));
            }
        }
        return graphe;
    }

    /** Écrit le graphe au format DOT, que Graphviz sait transformer en image. */
    public void exporterDot(Path fichier) throws IOException {
        List<String> lignes = new ArrayList<>();
        lignes.add("graph couplage ");

        // tous les nœuds, pour que les classes isolées apparaissent aussi
        for (String noeud : noeuds) {
            lignes.add("  \"" + noeud + "\";");
        }

        // les arêtes, avec leur poids affiché
        for (Arete arete : aretes) {
            lignes.add(String.format(Locale.ROOT, "  \"%s\" -- \"%s\" [label=\"%.4f\"];",
                    arete.classeA, arete.classeB, arete.poids));
        }

        lignes.add("}");

        // on crée le dossier (sorties/) s'il n'existe pas
        if (fichier.getParent() != null) {
            Files.createDirectories(fichier.getParent());
        }
        Files.write(fichier, lignes);
    }
}