package hai913i.tp2;

import hai913i.tp2.couplage.Couple;
import hai913i.tp2.couplage.Couplage;
import hai913i.tp2.input.CsvFactsSource;
import hai913i.tp2.input.FactsSource;
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
    }
}