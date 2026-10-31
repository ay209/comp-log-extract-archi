package hai913i.tp2;

import hai913i.tp2.couplage.Couple;
import hai913i.tp2.couplage.Couplage;
import hai913i.tp2.graphe.Arete;
import hai913i.tp2.graphe.GrapheCouplage;
import hai913i.tp2.input.CsvFactsSource;
import hai913i.tp2.input.FactsSource;
import hai913i.tp2.regroupement.Groupe;
import hai913i.tp2.regroupement.Regroupement;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Locale;

/**
 * Point d'entrée du TP 2.
 *
 * Usage :
 *   java -jar target/hai913i-tp2.jar <dossier-des-faits> [--cp <valeur>]
 *
 * A0 : charge les faits et affiche les 3 compteurs.
 * A1 : calcule le couplage entre classes.
 */
public final class Main {

    private static final double CP_DEFAUT = 0.025;

    private Main() {}

    public static void main(String[] args) {
        if (args.length == 0) {
            System.err.println("Usage : java -jar hai913i-tp2.jar <dossier-des-faits> [--cp <valeur>]");
            System.exit(1);
            return;
        }

        // lecture des arguments
        Path dir = Path.of(args[0]);
        double cp = CP_DEFAUT;
        for (int i = 1; i < args.length; i++) {
            if ("--cp".equals(args[i])) {
                if (i + 1 >= args.length) {
                    System.err.println("--cp attend une valeur.");
                    System.exit(1);
                    return;
                }
                try {
                    cp = Double.parseDouble(args[++i]);
                } catch (NumberFormatException e) {
                    System.err.println("Valeur de --cp invalide : " + args[i]);
                    System.exit(1);
                    return;
                }
                if (!Double.isFinite(cp) || cp < 0) {
                    System.err.println("--cp doit être un nombre positif ou nul : " + cp);
                    System.exit(1);
                    return;
                }
            } else {
                System.err.println("Argument inconnu : " + args[i]);
                System.exit(1);
                return;
            }
        }

        // ---------- A0 : chargement des faits derrière FactsSource ----------
        FactsSource facts;
        try {
            String prefix = Facts.discoverPrefix(dir);
            facts = new CsvFactsSource(dir, prefix);
        } catch (IOException e) {
            System.err.println("Erreur de lecture : " + e.getMessage());
            System.exit(2);
            return;
        }

        System.out.println("Dossier des faits : " + dir.toAbsolutePath());
        System.out.println("Jeu de données    : " + facts.name());
        System.out.println("Classes           : " + facts.classNames().size());
        System.out.println("Méthodes          : " + facts.methodCount());
        System.out.println("Arcs du graphe    : " + facts.arcs().size());
        System.out.println("Seuil CP retenu   : " + cp);

        // ---------- A1 : couplage entre classes ----------
        System.out.println("===============A1=========================");
        Couplage couplage = new Couplage(facts);

        System.out.println();
        System.out.println("Arcs internes écartés : " + (facts.arcs().size() - couplage.nbArcsInter()));
        System.out.println("Arcs inter-classes    : " + couplage.nbArcsInter());
        System.out.println("Couples non nuls      : " + couplage.nbCouplesNonNuls());
        System.out.println(String.format(Locale.ROOT, "Somme des couplages   : %.4f", couplage.somme()));

        // vérification : symétrie et diagonale nulle
        boolean symetrique = true;
        boolean diagonaleNulle = true;
        for (String a : facts.classNames()) {
            if (couplage.valeur(a, a) != 0.0) {
                diagonaleNulle = false;
            }
            for (String b : facts.classNames()) {
                if (couplage.valeur(a, b) != couplage.valeur(b, a)) {
                    symetrique = false;
                }
            }
        }
        System.out.println("Symétrique : " + symetrique);
        System.out.println("Diagonale nulle : " + diagonaleNulle);
        System.out.println();

        // couples par couplage décroissant
        for (Couple c : couplage.couplesTries()) {
            double valeur = couplage.valeur(c.classeA, c.classeB);
            System.out.println(String.format(Locale.ROOT, "%s - %s  r=%d  C=%.4f",
                    c.classeA, c.classeB, c.nbArcs, valeur));
        }
        // ---------- A2 : graphe de couplage pondéré ----------
        System.out.println("===============A2=========================");
        GrapheCouplage graphe = new GrapheCouplage(couplage, facts.classNames());
        System.out.println("Nœuds  : " + graphe.nbNoeuds());
        System.out.println("Arêtes : " + graphe.nbAretes());

        System.out.println("Les 5 couples les plus couplés :");
        for (int i = 0; i < 5 && i < graphe.nbAretes(); i++) {
            Arete a = graphe.aretes().get(i);
            System.out.println(String.format(Locale.ROOT, "  %s - %s  C=%.4f", a.classeA, a.classeB, a.poids));
        }
        System.out.println("Classes isolées : " + graphe.classesIsolees());

        // export, puis rechargement pour vérifier qu'on retrouve les mêmes valeurs
        try {
            Path csv = Path.of("sorties", "graphe.csv");
            graphe.exporterCsv(csv);
            graphe.exporterDot(Path.of("sorties", "graphe.dot"));

            GrapheCouplage recharge = GrapheCouplage.charger(csv);
            boolean identique = recharge.nbNoeuds() == graphe.nbNoeuds()
                    && recharge.nbAretes() == graphe.nbAretes();
            for (Arete a : graphe.aretes()) {
                if (recharge.poids(a.classeA, a.classeB) != a.poids) {
                    identique = false;
                }
            }
            System.out.println("Exports écrits dans : " + Path.of("sorties").toAbsolutePath());
            System.out.println("Rechargement identique : " + identique);
        } catch (IOException e) {
            System.err.println("Erreur d'export : " + e.getMessage());
            System.exit(2);
            return;
        }
        // ---------- B1 : clustering hiérarchique ascendant ----------------
        System.out.println("===============B1=========================");
        Regroupement regroupement = new Regroupement(couplage, facts.classNames());

        int etape = 1;
        boolean decroissant = true;          // vrai tant que les similarités ne remontent jamais
        double precedente = Double.MAX_VALUE;
        for (Groupe fusion : regroupement.fusions()) {
            System.out.println(String.format(Locale.ROOT, "Étape %2d : %s + %s  S=%.4f  -> groupe %d",
                    etape, fusion.gauche.libelle(), fusion.droit.libelle(), fusion.similarite, fusion.id));
            if (fusion.similarite > precedente + 1e-9) {
                decroissant = false;
            }
            precedente = fusion.similarite;
            etape++;
        }
        System.out.println("Nombre de fusions : " + regroupement.fusions().size());
        System.out.println("Similarités qui ne remontent jamais : " + decroissant);
        System.out.println("Classes dans la racine : " + regroupement.racine().classes.size());
    }



}